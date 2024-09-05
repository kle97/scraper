package io.playground.scraper.core;

import com.fasterxml.jackson.core.type.TypeReference;
import io.playground.scraper.constant.Constant;
import io.playground.scraper.core.side.UCNavigation;
import io.playground.scraper.core.side.UCOptions;
import io.playground.scraper.core.side.UCTargetLocator;
import io.playground.scraper.model.chromedevtools.BrowserInfo;
import io.playground.scraper.model.chromedevtools.PageInfo;
import io.playground.scraper.model.response.ResolvedNode;
import io.playground.scraper.model.response.ScriptNode;
import io.playground.scraper.model.response.html.OuterHtml;
import io.playground.scraper.model.response.screenshot.ScreenshotData;
import io.playground.scraper.model.response.screenshot.ViewPort;
import io.playground.scraper.model.response.target.TargetInfo;
import io.playground.scraper.model.response.target.TargetInfoProp;
import io.playground.scraper.model.response.target.TargetInfos;
import io.playground.scraper.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.federatedcredentialmanagement.FederatedCredentialManagementDialog;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.print.PrintOptions;
import org.openqa.selenium.remote.*;
import org.openqa.selenium.remote.service.DriverFinder;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.logging.Level;

@Slf4j
public class UCDriver extends RemoteWebDriver {
    
    public static final String ELEMENT_NOT_FOUND = "ELEMENT_NOT_FOUND";
    
    public static final int DEFAULT_EVENT_TIMEOUT_IN_MS = 3000;
    public static final String DEFAULT_WINDOWS_BINARY_PATH = "C:\\Progra~1\\Google\\Chrome\\Application\\chrome.exe";
    public static final String DEFAULT_NEW_TAB_URL = "chrome://newtab/";
    public static final int DEFAULT_PORT = 9222;
    
    private static String currentChromeVersion = "";
    private static String currentChromeLocation = "";

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ConditionFactory await = Awaitility.waitAtMost(Duration.ofMillis(DEFAULT_EVENT_TIMEOUT_IN_MS))
                                                            .pollDelay(Duration.ZERO)
                                                            .ignoreExceptions();
    
    private final String host;
    
    private final int port;
    
    private final String debuggerUrl;
    
    private final List<String> chromeOptionArguments;
    private final Process process;
    
    private final DevToolsClient client;
    
    private Path tempProfileFolderPath;

    private Capabilities capabilities;

    public UCDriver() {
        this(false);
    }
    
    public UCDriver(boolean logEvents) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::quit));
        this.host = "127.0.0.1";
        this.port = PortUtil.isPortFree(DEFAULT_PORT) ? DEFAULT_PORT : PortProber.findFreePort();
        this.debuggerUrl = this.host + ":" + this.port;
        this.chromeOptionArguments = chromeOptionArguments();
        this.process = startBinary(this.chromeOptionArguments);
        this.chromeOptionArguments.removeLast();
        this.client = new DevToolsClient(getDevToolNewTabUrl(), logEvents);
        this.client.enablePage();
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
        stopBinary();
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
    
    
    

    private Process startBinary(List<String> arguments) {
        List<String> command = new ArrayList<>();
        if (OSUtil.isWindows()) {
            command.add("cmd");
            command.add("/c");
        } else {
            command.add("sh");
            command.add("-c");
        }
        command.add(getChromeLocation().getAbsolutePath());
        command.addAll(arguments);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.inheritIO().command(command);

        try {
            log.info(String.join(" ", command));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stopBinary(process)));
            Process process = processBuilder.start();
            await.until(this::isDevToolsReachable);
            return process;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void stopBinary() {
        stopBinary(process);
    }

    private void stopBinary(Process process) {
        if (process == null) {
            return;
        }
        
        try {
            process.children().forEach(ProcessHandle::destroy);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        try {
            process.destroy();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        
        try {
            process.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            process.destroyForcibly();
        }
    }

    public String getDevToolBrowserUrl() {
        try {
            HttpRequest request = HttpRequest.newBuilder(new URI("http://" + debuggerUrl + "/json/version")).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return JacksonUtil.readValue(response.body(), BrowserInfo.class).webSocketDebuggerUrl();
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDevToolNewTabUrl() {
        return getDevToolPageUrl(DEFAULT_NEW_TAB_URL, debuggerUrl);
    }

    public static String getDevToolPageUrl(String url, String debuggerUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(new URI("http://" + debuggerUrl + "/json/list")).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            PageInfo pageInfo = JacksonUtil.readValue(response.body(), new TypeReference<List<PageInfo>>(){})
                                           .stream()
                                           .filter(info -> info.url().toLowerCase().contains(url.toLowerCase()))
                                           .findFirst()
                                           .orElse(null);
            return pageInfo != null ? pageInfo.webSocketDebuggerUrl() : "";
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getDevToolFirstTabUrl(String debuggerUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(new URI("http://" + debuggerUrl + "/json/list")).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            PageInfo pageInfo = JacksonUtil.readValue(response.body(), new TypeReference<List<PageInfo>>(){})
                                           .stream()
                                           .filter(info -> info.type().trim().equalsIgnoreCase("page"))
                                           .findFirst()
                                           .orElse(null);
            return pageInfo != null ? pageInfo.webSocketDebuggerUrl() : "";
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public boolean isDevToolsReachable() {
        try {
            HttpRequest request = HttpRequest.newBuilder(new URI("http://" + debuggerUrl)).build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public static File getChromeLocation() {
//        File file = new File(DEFAULT_WINDOWS_BINARY_PATH);
//        if (file.exists()) {
//            return file;
//        }
        ChromeOptions options = new ChromeOptions();
        options.setBrowserVersion("stable");
        DriverFinder finder = new DriverFinder(ChromeDriverService.createDefaultService(), options);
        String driverPath = finder.getDriverPath();
        String[] tokens = driverPath.split(StringEscapeUtils.escapeJava(FileSystems.getDefault().getSeparator()));
        for (String token : tokens) {
            if (token.matches("\\d\\.\\d")) {
                currentChromeVersion = token;
                break;
            }
        }
        currentChromeLocation = finder.getBrowserPath().replace("Program Files", "Progra~1");
        return new File(currentChromeLocation);
    }

    public static String getChromeVersion() {
        ChromeOptions options = new ChromeOptions();
        options.setBrowserVersion("stable");
        DriverFinder finder = new DriverFinder(ChromeDriverService.createDefaultService(), options);
        String driverPath = finder.getDriverPath();
        String[] tokens = driverPath.split(FileSystems.getDefault().getSeparator());
        for (String token : tokens) {
            if (token.matches("\\d\\.\\d")) {
                return token;
            }
        }
        return "";
    }

    private List<String> chromeOptionArguments() {
        List<String> arguments = new ArrayList<>();
//        arguments.add("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36");

        arguments.add("--no-default-browser-check");
        arguments.add("--no-first-run");
        arguments.add("--no-service-autorun");
        arguments.add("--password-store=basic");

        arguments.add("--window-size=1920,1080");
        arguments.add("--disable-dev-shm-usage");
        arguments.add("--disable-application-cache");
        arguments.add("--safebrowsing-disable-download-protection");
        arguments.add("--disable-search-engine-choice-screen");
        arguments.add("--disable-browser-side-navigation");
        arguments.add("--disable-save-password-bubble");
        arguments.add("--disable-single-click-autofill");
        arguments.add("--allow-file-access-from-files");
        arguments.add("--disable-prompt-on-repost");
        arguments.add("--dns-prefetch-disable");
        arguments.add("--disable-translate");
        arguments.add("--disable-renderer-backgrounding");
        arguments.add("--disable-backgrounding-occluded-windows");
        arguments.add("--disable-client-phishing-detection");
        arguments.add("--disable-oopr-debug-crash-dump");
        arguments.add("--disable-top-sites");
        arguments.add("--ash-no-nudges");
        arguments.add("--no-crash-upload");
        arguments.add("--deny-permission-prompts");
//        arguments.add("--simulate-outdated-no-au=\"Tue, 31 Dec 2099 23:59:59 GMT\"");
        arguments.add("--disable-ipc-flooding-protection");
        arguments.add("--disable-password-protection");
        arguments.add("--disable-domain-reliability");
        arguments.add("--disable-component-update");
        arguments.add("--disable-breakpad");
        arguments.add("--disable-features=OptimizationHints,OptimizationHintsFetching,Translate," +
                              "OptimizationTargetPrediction,OptimizationGuideModelDownloading,DownloadBubble," +
                              "DownloadBubbleV2,InsecureDownloadWarnings,InterestFeedContentSuggestions," +
                              "PrivacySandboxSettings4,SidePanelPinning,UserAgentClientHint");
        arguments.add("--disable-popup-blocking");
        arguments.add("--homepage=about:blank");

        arguments.add("--lang=en-US");
        arguments.add("--log-level=0");
        
        arguments.add("--remote-debugging-host=" + host);
        arguments.add("--remote-debugging-port=" + port);
        arguments.add("--user-data-dir=" + createTempProfile());
        
        arguments.add("--remote-allow-origins=http://" + debuggerUrl);

        return arguments;
    }

    private String createTempProfile() {
        Map<String, Object> profileMap = new HashMap<>();
        profileMap.put("password_manager_enabled", false);
        profileMap.put("default_content_setting_values", Map.of("notifications", 2, "automatic_downloads", 1));
        profileMap.put("default_content_settings", Map.of("popups", 0));
        profileMap.put("managed_default_content_settings", Map.of("popups", 0));
        profileMap.put("exit_type", null);

        Map<String, Object> preferences = Map.ofEntries(
                Map.entry("download", Map.of("default_directory", "D:\\Downloads", 
                                             "directory_upgrade", true, 
                                             "prompt_for_download", false)),
                Map.entry("credentials_enable_service", false),
                Map.entry("local_discovery", Map.of("notifications_enabled", false)),
                Map.entry("safebrowsing", Map.of("enabled", false, "disable_download_protection", true)),
                Map.entry("omnibox-max-zero-suggest-matches", 0),
                Map.entry("omnibox-use-existing-autocomplete-client", 0),
                Map.entry("omnibox-trending-zero-prefix-suggestions-on-ntp", 0),
                Map.entry("omnibox-local-history-zero-suggest-beyond-ntp", 0),
                Map.entry("omnibox-on-focus-suggestions-contextual-web", 0),
                Map.entry("omnibox-on-focus-suggestions-srp", 0),
                Map.entry("omnibox-zero-suggest-prefetching", 0),
                Map.entry("omnibox-zero-suggest-prefetching-on-srp", 0),
                Map.entry("omnibox-zero-suggest-prefetching-on-web", 0),
                Map.entry("omnibox-zero-suggest-in-memory-caching", 0),
                Map.entry("content_settings", Map.of("exceptions", Map.of("automatic_download", Map.of("*", Map.of("setting", 1))))),
                Map.entry("default_content_setting_values", Map.of("notifications", 0)),
                Map.entry("default_content_settings", Map.of("popups", 0)),
                Map.entry("managed_default_content_settings", Map.of("popups", 0)),
                Map.entry("profile", profileMap)
                                                       );
        try {
            Files.createDirectories(Path.of(Constant.TEMP_PROFILE_FOLDER_PATH));
            tempProfileFolderPath = Files.createDirectories(Path.of(Constant.TEMP_PROFILE_FOLDER_PATH + "temp-profile-" + UUID.randomUUID()));
            Files.walkFileTree(tempProfileFolderPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    file.toFile().deleteOnExit();
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    dir.toFile().deleteOnExit();
                    return FileVisitResult.CONTINUE;
                }
            });
            Runtime.getRuntime().addShutdownHook(new Thread(this::deleteTempProfile));
            String defaultFolder = tempProfileFolderPath.toAbsolutePath() + FileSystems.getDefault().getSeparator() + "Default";
            Files.createDirectories(Path.of(defaultFolder));
            String preferencesFile = defaultFolder + FileSystems.getDefault().getSeparator() + "Preferences";
            Path preferencesPath = Path.of(preferencesFile);
            Files.write(preferencesPath, List.of(JacksonUtil.writeValueAsString(preferences)), StandardCharsets.UTF_8);
            return tempProfileFolderPath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void deleteTempProfile() {
        try {
            if (tempProfileFolderPath != null) {
                Files.walkFileTree(tempProfileFolderPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                        if (e == null) {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }
                        throw e;
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createInfoFile() {
        try {
            boolean outOfSync = true;
            Files.createDirectories(Path.of(Constant.EXTERNAL_RESOURCES_PATH));
            File versionFile = new File(Constant.EXTERNAL_RESOURCES_PATH + "version.txt");
            if (currentChromeVersion == null || currentChromeVersion.isEmpty()) {
                currentChromeVersion = getChromeVersion();
            }

            if (versionFile.exists()) {
                List<String> lines = Files.readAllLines(versionFile.toPath());
                if (!lines.isEmpty() && lines.getFirst().trim().equals(currentChromeVersion)) {
                    outOfSync = false;
                }
            }

            if (outOfSync) {
                Files.write(versionFile.toPath(), List.of(currentChromeVersion), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ChromeDriverService patchedDriverService() {
        String patchedDriverPathName = Patcher.patchChromeDriver(DownloadUtil.downloadLatestChromeDriver());
        return new ChromeDriverService
                .Builder()
                .usingDriverExecutable(Paths.get(patchedDriverPathName).toFile())
                .withBuildCheckDisabled(true)
                .build();
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
