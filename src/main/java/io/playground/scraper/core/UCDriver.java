package io.playground.scraper.core;

import io.playground.scraper.constant.Constant;
import io.playground.scraper.core.side.UCNavigation;
import io.playground.scraper.core.side.UCOptions;
import io.playground.scraper.core.side.UCTargetLocator;
import io.playground.scraper.model.response.ResolvedNode;
import io.playground.scraper.model.response.ScriptNode;
import io.playground.scraper.model.response.html.OuterHtml;
import io.playground.scraper.model.response.screenshot.ScreenshotData;
import io.playground.scraper.model.response.screenshot.ViewPort;
import io.playground.scraper.model.response.target.TargetInfo;
import io.playground.scraper.model.response.target.TargetInfoProp;
import io.playground.scraper.model.response.target.TargetInfos;
import io.playground.scraper.util.SleepUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.*;
import org.openqa.selenium.federatedcredentialmanagement.FederatedCredentialManagementDialog;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.print.PrintOptions;
import org.openqa.selenium.remote.*;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Level;

@Slf4j
public class UCDriver extends RemoteWebDriver {
    
    private final UCDriverOptions ucDriverOptions;
    private final DevToolsClient client;
    private Capabilities capabilities;

    public UCDriver() {
        this(UCDriverOptions.builder().build());
    }
    
    public UCDriver(UCDriverOptions ucDriverOptions) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::quit));
        ucDriverOptions = ucDriverOptions == null ? UCDriverOptions.builder().build() : ucDriverOptions;
        this.ucDriverOptions = ucDriverOptions ;
        ucDriverOptions.startBinary();
        this.client = new DevToolsClient(ucDriverOptions.getDevToolNewTabUrl(), ucDriverOptions);
        this.client.enablePage();
        if (ucDriverOptions.getProxyIP() != null) {
            this.client.emulationSetTimeZoneOverride(ucDriverOptions.getTimeZoneOfIpAddress(ucDriverOptions.getProxyIP()));
        }
    }
    
    public DevToolsClient getClient() {
        if (client != null && client.isOpen()) {
            return client;
        } else {
            throw new WebDriverException("DevToolsClient is not ready!");
        }
    }

    @Override
    public void get(String url) {
        if (!url.isEmpty() && url.charAt(url.length() - 1) != '/') {
            url += "/";
        }
        client.enableFetch(url);
        getClient().navigate(url);
    }

    @Override
    public String getCurrentUrl() {
        String frameId = getClient().getCurrentFrameId();
        TargetInfo targetInfo = getClient().getTargetInfo(frameId);
        if (targetInfo != null) {
            return targetInfo.targetInfo().url();
        }
        return "";
    }

    @Override
    public String getTitle() {
        String frameId = getClient().getCurrentFrameId();
        TargetInfo targetInfo = getClient().getTargetInfo(frameId);
        if (targetInfo != null) {
            return targetInfo.targetInfo().title();
        }
        return "";
    }

    public Path saveScreenshot(String title) {
        return saveScreenshot(title, null);
    }
    
    public Path saveScreenshot(String title, ViewPort viewPort) {
        try {
            Files.createDirectories(Path.of(Constant.SCREENSHOT_FOLDER_PATH));
            File destination = new File(Constant.SCREENSHOT_FOLDER_PATH + title + "-" + UUID.randomUUID() + ".png");
            return Files.write(destination.toPath(), getScreenshotAs(OutputType.BYTES, viewPort));
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> outputType) throws WebDriverException {
        return getScreenshotAs(outputType, null);
    }

    public <X> X getScreenshotAs(OutputType<X> outputType, ViewPort viewPort) throws WebDriverException {
        ScreenshotData result = getClient().getScreenshot(viewPort);
        if (result != null && result.data() instanceof String base64EncodedPng) {
            return outputType.convertFromBase64Png(base64EncodedPng);
        } else {
            throw new RuntimeException(
                    String.format(
                            "Unexpected result for %s command: %s",
                            DriverCommand.SCREENSHOT,
                            result == null ? "null" : result.getClass().getName() + " instance"));
        }
    }

    @Override
    public Object executeScript(ScriptKey key, Object... args) {
        return executeScript(key.getIdentifier(), args);
    }

    @Override
    public Object executeScript(String script, Object... args) {
        String globalThisId = getClient().getGlobalThisId();
        int executionContextId = ResolvedNode.getExecutionContextId(globalThisId);
        Map<String, Object> params = new HashMap<>();
        if (args != null && args.length > 1 && args.length % 2 == 0) {
            for (int i = 0; i < args.length; i+=2) {
                params.put((String) args[i], args[i + 1]);
            }
        }
        ScriptNode scriptNode = getClient().executeScript(script, executionContextId, globalThisId, params);
        if (scriptNode != null && scriptNode.result().value() != null) {
            return scriptNode.result().value();
        }

        return null;
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        int executionContextId = ResolvedNode.getExecutionContextId(getClient().getGlobalThisId());
        ScriptNode scriptNode = getClient().executeAsyncScript(script, executionContextId, null);
        if (scriptNode != null && scriptNode.result().value() != null) {
            return scriptNode.result().value();
        }

        return null;
    }

    @Override
    public List<WebElement> findElements(By by) {
        return findElements(by, null);
    }

    @Override
    public WebElement findElement(By by) {
        List<WebElement> elements = findElements(by, 0);
        if (!elements.isEmpty()) {
            return elements.getFirst();
        } else {
            throw new NoSuchElementException("No such element for locator: '" + by + "'!");
        }
    }

    public List<WebElement> findElements(By by, Integer targetIndex) {
        return findElements(by, getClient().getRootObjectId(), targetIndex);
    }
    
    public List<WebElement> findElements(By by, String objectId, Integer targetIndex) {
        List<WebElement> elements = new ArrayList<>();
        String script = "";
        String value = by.toString().split(": ")[1];
        boolean useXpath = false;
        if (by.toString() != null) {
            if (by.toString().contains("By.id")) {
                useXpath = true;
                value = "//*[@id=\"" + value + "\"]";
            } else if (by.toString().contains("By.className")) {
                useXpath = true;
                value = "//*[@class=\"" + value + "\"]";
            } else if (by.toString().contains("By.name")) {
                useXpath = true;
                value = "//*[@name=\"" + value + "\"]";
            }

            if (by.toString().contains("By.tagName")) {
                script = "return obj.getElementsByTagName(arguments[0])";
            } else if (by.toString().contains("By.cssSelector")) {
                script = "return obj.querySelectorAll(arguments[0])";
            } else if (by.toString().contains("By.xpath") || useXpath) {
                script = "return document.evaluate(arguments[0],obj,null,XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,null)";
            }
        }

        if (script.isEmpty()) {
            return elements;
        }

        ScriptNode scriptNode = getClient().executeScript(script, objectId, Map.of("value", value));
        if (scriptNode == null) {
            return elements;
        }
        String parentObjectId = scriptNode.result().objectId();
        if (scriptNode.result().className() != null && scriptNode.result().className().equals("XPathResult")) {
            ScriptNode checkType = getClient().executeScript("return obj.resultType == 7", parentObjectId,
                                                             "json");
            if (checkType != null && checkType.result().getValueAsBoolean()) {
                ScriptNode lengthNode = getClient().executeScript("return obj.snapshotLength", parentObjectId,
                                                                  "json");
                if (lengthNode != null && lengthNode.result().getValueAsInteger() > 0) {
                    int length = lengthNode.result().getValueAsInteger();
                    elements = findElements("return obj.snapshotItem(arguments[0])", parentObjectId, length, targetIndex);
                }
            }
        } else if (scriptNode.result().className() != null
                && (scriptNode.result().className().equals("NodeList") || scriptNode.result().className().equals("HTMLCollection"))) {
            int nodeListSize = scriptNode.result().getListSize();
            elements = findElements("return obj[arguments[0]]", parentObjectId, nodeListSize, targetIndex);
        }

        return elements;
    }
    
    private List<WebElement> findElements(String script, String parentObjectId, int length, Integer targetIndex) {
        List<WebElement> elements = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            ScriptNode scriptNode = getClient().executeScript(script, parentObjectId, Map.of("value", i));
            if (scriptNode != null) {
                String elementObjectId = scriptNode.result().objectId();
                UCElement element = new UCElement(this, elementObjectId);
                elements.add(element);
            }

            if (targetIndex != null && targetIndex == i) {
                return elements;
            }
        }
        return elements;
    }

    @Override
    public String getPageSource() {
        int rootNodeId = getClient().getRootNodeId();
        OuterHtml outerHtml = getClient().getOuterHtml(rootNodeId);
        if (outerHtml != null) {
            return outerHtml.outerHTML();
        }
        return "";
    }

    @Override
    public void close() {
        getClient().close();
    }

    @Override
    public void quit() {
        if (client != null && client.isOpen()) {
            client.quit();
        }
        ucDriverOptions.stopBinary();
        ucDriverOptions.deleteTempProfile();
    }

    @Override
    public Set<String> getWindowHandles() {
        Set<String> handles = new HashSet<>();
        TargetInfos targetInfos = getClient().getTargets();
        if (targetInfos != null && !targetInfos.targetInfos().isEmpty()) {
            for (TargetInfoProp targetInfo : targetInfos.targetInfos()) {
                if (targetInfo.type().equals("page")) {
                    handles.add(targetInfo.targetId());
                }
            }
        }
        return handles;
    }

    @Override
    public String getWindowHandle() {
        return getClient().getCurrentFrameId();
    }

    @Override
    public TargetLocator switchTo() {
        return new UCTargetLocator(this);
    }

    @Override
    public Navigation navigate() {
        return new UCNavigation(this);
    }

    @Override
    public Options manage() {
        return new UCOptions(this);
    }

    public void sleep(int sleepTimeInMs) {
        SleepUtil.sleep(sleepTimeInMs);
    }

    @Override
    public Capabilities getCapabilities() {
        if (capabilities == null) {
            capabilities = new ImmutableCapabilities();
        }
        return capabilities;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
    
    
    

    
    
    @Override
    public void perform(Collection<Sequence> actions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resetInputState() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualAuthenticator addVirtualAuthenticator(VirtualAuthenticatorOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeVirtualAuthenticator(VirtualAuthenticator authenticator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getDownloadableFiles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void downloadFile(String fileName, Path targetLocation) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteDownloadableFiles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDelayEnabled(boolean enabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resetCooldown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileDetector getFileDetector() {
        return super.getFileDetector();
    }

    @Override
    public FederatedCredentialManagementDialog getFederatedCredentialManagementDialog() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFileDetector(FileDetector detector) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLogLevel(Level level) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Script script() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Pdf print(PrintOptions printOptions) throws WebDriverException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<WebElement> findElements(SearchContext context, BiFunction<String, Object, CommandPayload> findCommand,
                                         By locator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CommandExecutor getCommandExecutor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setErrorHandler(ErrorHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ErrorHandler getErrorHandler() {
        throw new UnsupportedOperationException();
    }

    @Override
    public SessionId getSessionId() {
        throw new UnsupportedOperationException();
    }
}
