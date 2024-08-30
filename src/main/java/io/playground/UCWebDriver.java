package io.playground;

import com.fasterxml.jackson.core.type.TypeReference;
import io.playground.scraper.constant.Constant;
import io.playground.scraper.model.chromedevtools.BrowserInfo;
import io.playground.scraper.model.chromedevtools.PageInfo;
import io.playground.scraper.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.remote.service.DriverFinder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UCWebDriver implements WebDriver {

    public static final int DEFAULT_TIMEOUT_IN_MS = 30000;
    public static final String DEFAULT_WINDOWS_BINARY_PATH = "C:\\Progra~1\\Google\\Chrome\\Application\\chrome.exe";
    public static final String DEFAULT_NEW_TAB_URL = "chrome://newtab/";
    public static final int DEFAULT_PORT = 9222;
    
    private static String currentChromeVersion = "";
    private static String currentChromeLocation = "";

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ConditionFactory await = Awaitility.waitAtMost(Duration.ofMillis(DEFAULT_TIMEOUT_IN_MS))
                                                            .pollDelay(Duration.ZERO)
                                                            .ignoreExceptions();
    
    private final String host;
    
    private final int port;
    
    private final String debuggerUrl;
    
    private final List<String> chromeOptionArguments;
    private final Process process;
    private final DevToolsClient client;
    
    public UCWebDriver() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::quit));
        this.host = "127.0.0.1";
        this.port = PortUtil.isPortFree(DEFAULT_PORT) ? DEFAULT_PORT : PortProber.findFreePort();
        this.debuggerUrl = this.host + ":" + this.port;
        this.chromeOptionArguments = chromeOptionArguments();
        this.process = startBinary(this.chromeOptionArguments);
        this.chromeOptionArguments.removeLast();
        this.client = new DevToolsClient(getDevToolNewTabUrl());
        this.client.enablePage();
    }
    
    @Override
    public void get(String url) {
        if (client != null && client.isOpen()) {
            client.navigate(url);
        }
    }

    @Override
    public String getCurrentUrl() {
        return "";
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public List<WebElement> findElements(By by) {
        String value = "";
        boolean useXpath = false;
        if (by instanceof By.ById) {
            useXpath = true;
            value = "//*[@id=\"" + by + "\"]";
        } else if (by instanceof By.ByClassName) {
            useXpath = true;
            value = "//*[@class=\"" + by + "\"]";
        } else if (by instanceof By.ByName) {
            useXpath = true;
            value = "//*[@name=\"" + by + "\"]";
        }
        
//        if (by instanceof By.ByTagName) {
//            return 
//        } else if () {
//            
//        } else if () {
//            
//        }

        return List.of();
    }

    @Override
    public WebElement findElement(By by) {
        return null;
    }

    @Override
    public String getPageSource() {
        return "";
    }

    @Override
    public void close() {

    }

    @Override
    public void quit() {
        if (client != null && client.isOpen()) {
            client.close();
        }
        stopBinary();
    }

    @Override
    public Set<String> getWindowHandles() {
        return Set.of();
    }

    @Override
    public String getWindowHandle() {
        return "";
    }

    @Override
    public TargetLocator switchTo() {
        return null;
    }

    @Override
    public Navigation navigate() {
        return null;
    }

    @Override
    public Options manage() {
        return null;
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
//            log.info(response.body());
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
            Path tempFolderPath = Files.createTempDirectory("UCWebDriver-profile-");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Files.walk(tempFolderPath)
                         .sorted(Comparator.reverseOrder())
                         .map(Path::toFile)
                         .forEach(File::delete);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }));
            String defaultFolder = tempFolderPath.toAbsolutePath() + FileSystems.getDefault().getSeparator() + "Default";
            Files.createDirectories(Path.of(defaultFolder));
            String preferencesFile = defaultFolder + FileSystems.getDefault().getSeparator() + "Preferences";
            Path preferencesPath = Path.of(preferencesFile);
            Files.write(preferencesPath, List.of(JacksonUtil.writeValueAsString(preferences)), StandardCharsets.UTF_8);
            return tempFolderPath.toAbsolutePath().toString();
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
}
