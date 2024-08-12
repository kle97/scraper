package io.playground.scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.playground.scraper.util.DownloadUtil;
import io.playground.scraper.util.Patcher;
import io.playground.scraper.util.PortUtil;
import io.playground.scraper.util.SleepUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.net.PortProber;
import org.openqa.selenium.remote.service.DriverFinder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UCDriver extends ChromeDriver {
    
    public static final String DEFAULT_WINDOWS_BINARY_PATH = "C:\\Progra~1\\Google\\Chrome\\Application\\chrome.exe";
    public static final int DEFAULT_PORT = 9222;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static int port = DEFAULT_PORT;
    
    private static Process process;

    public UCDriver() {
        super(patchedChromeOptions());
    }
    
    private static ChromeDriverService patchedDriverService() {
        String patchedDriverPathName = Patcher.patchChromeDriver(DownloadUtil.downloadLatestChromeDriver());
        port = PortUtil.isPortFree(DEFAULT_PORT) ? DEFAULT_PORT : PortProber.findFreePort();
//        startBinary(chromeOptionArguments());
        return new ChromeDriverService
                .Builder()
                .usingDriverExecutable(Paths.get(patchedDriverPathName).toFile())
                .usingPort(port)
                .withBuildCheckDisabled(true)
                .build();
    }
    
    private static ChromeOptions patchedChromeOptions() {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");
        
        List<String> arguments = chromeOptionArguments();
        chromeOptions.addArguments(arguments);
        for (Map.Entry<String, Object> entry : chromeOptions.asMap().entrySet()) {
            log.info("{}: {}", entry.getKey(), entry.getValue());
        }
        
        return chromeOptions;
    }
    
    private static Process startBinary(List<String> arguments) {
        List<String> command = new ArrayList<>();
        command.add("cmd");
        command.add("/c");
        command.add(getChromeLocation().getAbsolutePath());
        command.addAll(arguments);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.inheritIO().command(command);

        try {
            process = processBuilder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stopBinary(process)));
            return process;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void stopBinary(Process process) {
        process.children().forEach(ProcessHandle::destroy);
        process.destroy();
        try {
            process.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            process.destroyForcibly();
        }
    }
    
    private static List<String> chromeOptionArguments() {
        String patchedDriverPathName = Patcher.patchChromeDriver(DownloadUtil.downloadLatestChromeDriver());
        System.setProperty("webdriver.chrome.driver", patchedDriverPathName);


        List<String> arguments = new ArrayList<>();
//        arguments.add("--disable-blink-features=AutomationControlled");
//        arguments.add("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36");

//        arguments.add("--start-maximized");
//        arguments.add("--headless=new");

//        arguments.add("--no-default-browser-check");
//        arguments.add("--no-first-run");
//        arguments.add("--no-service-autorun");
//        arguments.add("--password-store=basic");

//        arguments.add("--window-size=1920,1080");
//        arguments.add("--disable-dev-shm-usage");
//        arguments.add("--disable-application-cache");
//        arguments.add("--safebrowsing-disable-download-protection");
//        arguments.add("--disable-search-engine-choice-screen");
//        arguments.add("--disable-browser-side-navigation");
//        arguments.add("--disable-save-password-bubble");
//        arguments.add("--disable-single-click-autofill");
//        arguments.add("--allow-file-access-from-files");
//        arguments.add("--disable-prompt-on-repost");
//        arguments.add("--dns-prefetch-disable");
//        arguments.add("--disable-translate");
//        arguments.add("--disable-renderer-backgrounding");
//        arguments.add("--disable-backgrounding-occluded-windows");
//        arguments.add("--disable-client-phishing-detection");
//        arguments.add("--disable-oopr-debug-crash-dump");
//        arguments.add("--disable-top-sites");
//        arguments.add("--ash-no-nudges");
//        arguments.add("--no-crash-upload");
//        arguments.add("--deny-permission-prompts");
//        arguments.add("--simulate-outdated-no-au=\"Tue, 31 Dec 2099 23:59:59 GMT\"");
//        arguments.add("--disable-ipc-flooding-protection");
//        arguments.add("--disable-password-protection");
//        arguments.add("--disable-domain-reliability");
//        arguments.add("--disable-component-update");
//        arguments.add("--disable-breakpad");
//        arguments.add("--disable-features=OptimizationHints,OptimizationHintsFetching,Translate," +
//                              "OptimizationTargetPrediction,OptimizationGuideModelDownloading,DownloadBubble," +
//                              "DownloadBubbleV2,InsecureDownloadWarnings,InterestFeedContentSuggestions," +
//                              "PrivacySandboxSettings4,SidePanelPinning,UserAgentClientHint");
//        arguments.add("--disable-popup-blocking");
//        arguments.add("--homepage=chrome://new-tab-page/");

//        arguments.add("--remote-debugging-host=127.0.0.1");
//        arguments.add("--remote-debugging-port=" + port);
        
        return arguments;
    }

    @Override
    public void quit() {
        super.quit();
//        stopBinary(process);
    }

    public static File getChromeLocation() {
        File file = new File(DEFAULT_WINDOWS_BINARY_PATH);
        if (file.exists()) {
            return file;
        }
        ChromeOptions options = new ChromeOptions();
        options.setBrowserVersion("stable");
        DriverFinder finder = new DriverFinder(ChromeDriverService.createDefaultService(), options);
        return new File(finder.getBrowserPath());
    }
    
    private static void createTempProfile() {
        Map<String, Object> preferences = Map.ofEntries(
                Map.entry("download", Map.of("default_directory", "C:\\Downloads", "directory_upgrade", true, "prompt_for_download", false)),
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
                Map.entry("profile", Map.of("password_manager_enabled", false, 
                                            "default_content_setting_values", Map.of("notifications", 2, "automatic_downloads", 1), 
                                            "default_content_settings", Map.of("popups", 0), 
                                            "managed_default_content_settings", Map.of("popups", 0))),
        );
        Map<String, Object> prefMap = new HashMap<>(preferences);
        prefMap.put("exit_type", null);
        
        
    }
}
