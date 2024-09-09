package io.playground.scraper.core;

import com.fasterxml.jackson.core.type.TypeReference;
import io.playground.scraper.constant.Constant;
import io.playground.scraper.model.chromedevtools.BrowserInfo;
import io.playground.scraper.model.chromedevtools.PageInfo;
import io.playground.scraper.model.geolocation.IPApiResponse;
import io.playground.scraper.util.*;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.cef.OS;
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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Builder(toBuilder = true)
public class UCDriverOptions {

    public static final String DEFAULT_NEW_TAB_URL = "chrome://newtab/";
    public static final String DEFAULT_PROXY_SERVER = "direct://";
    public static final int DEFAULT_PORT = 9222;
    private static final int DEFAULT_EVENT_TIMEOUT_IN_MS = 3000;

    private static final Random random = new Random();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ConditionFactory await = Awaitility.waitAtMost(Duration.ofMillis(DEFAULT_EVENT_TIMEOUT_IN_MS))
                                                            .pollDelay(Duration.ZERO)
                                                            .ignoreExceptions();
    @Builder.Default
    private String host = "127.0.0.1";

    @Builder.Default
    private int port = PortUtil.isPortFree(DEFAULT_PORT) ? DEFAULT_PORT : PortProber.findFreePort();;

    @Builder.Default
    private List<String> optionArguments = new ArrayList<>();

    @Getter
    @Builder.Default
    private boolean logCDPEvents = false;

    @Getter
    @Builder.Default
    private boolean fakeUserAgent = false;

    @Getter
    @Builder.Default
    private String proxyServer = DEFAULT_PROXY_SERVER;

    @Getter
    private String proxyIP;

    @Getter
    private String proxyPort;
    
    @Getter
    @Builder.Default
    private String proxyUsername = "";

    @Getter
    @Builder.Default
    private String proxyPassword = "";
    
    private Process process;
    
    private Path tempProfileFolderPath;
    
    public static UCDriverOptionsBuilder withProxyServer(String proxyServerFilePath) {
        File file = new File(proxyServerFilePath);
        if (file.exists()) {
            try {
                List<String> proxies = Files.readAllLines(file.toPath());
                int randomIndex = random.nextInt(proxies.size());
                return builder().build().toBuilder().proxyServer(proxies.get(randomIndex));
            } catch (Exception e) {
                log.warn("Couldn't find any proxy server info from '{}'!", proxyServerFilePath);
            }
        }
        return builder();
    }

    public String getDebuggerUrl() {
        return host + ":" + port;
    }
    
    public String getTimeZoneOfIpAddress(String ipAddress) {
        String url = "http://ip-api.com/json/" + ipAddress;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return JacksonUtil.readValue(response.body(), IPApiResponse.class).timezone();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void startBinary() {
        startBinary(getOptionArguments());
    }

    public void startBinary(List<String> arguments) {
        if (process != null && process.isAlive()) {
            stopBinary(process);
        }
        List<String> command = new ArrayList<>();
        if (OSUtil.isWindows()) {
            command.add("cmd");
            command.add("/c");
        } else {
            command.add("sh");
            command.add("-c");
        }
        command.add(getChromeLocation().toString());
        command.addAll(arguments);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.inheritIO().command(command);

        try {
            log.info(String.join(" ", command));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stopBinary(process)));
            process = processBuilder.start();
            await.until(this::isDevToolsReachable);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopBinary() {
        stopBinary(process);
    }

    public void stopBinary(Process process) {
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
            HttpRequest request = HttpRequest.newBuilder(new URI("http://" + getDebuggerUrl() + "/json/version")).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info(response.body());
            return JacksonUtil.readValue(response.body(), BrowserInfo.class).webSocketDebuggerUrl();
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDevToolNewTabUrl() {
        return getDevToolPageUrl(DEFAULT_NEW_TAB_URL, getDebuggerUrl());
    }

    public String getDevToolPageUrl(String url, String debuggerUrl) {
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

    public String getDevToolFirstTabUrl(String debuggerUrl) {
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
            HttpRequest request = HttpRequest.newBuilder(new URI("http://" + getDebuggerUrl())).build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public Path getChromeLocation() {
        ChromeOptions options = new ChromeOptions();
        options.setBrowserVersion("stable");
        DriverFinder finder = new DriverFinder(ChromeDriverService.createDefaultService(), options);
        return Path.of(finder.getBrowserPath().replace("Program Files", "Progra~1")).toAbsolutePath();
    }

    public String getChromeVersion() {
        ChromeOptions options = new ChromeOptions();
        options.setBrowserVersion("stable");
        DriverFinder finder = new DriverFinder(ChromeDriverService.createDefaultService(), options);
        String driverPath = finder.getDriverPath();
        String[] tokens = driverPath.split(StringEscapeUtils.escapeJava(FileSystems.getDefault().getSeparator()));
        for (String token : tokens) {
            if (token.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                return token;
            }
        }
        return "";
    }

    public String getChromeMajorVersion() {
        String[] tokens = getChromeVersion().split("\\.");
        return tokens[0];
    }
    
    public String getRandomUserAgent() {
        String chromeVersion = getChromeMajorVersion() + "-0";
        File userAgentFile = new File(Constant.FAKE_USER_AGENT_FOLDER_PATH + chromeVersion + ".txt");
        List<String> userAgents;
        try {
            if (!userAgentFile.exists()) {
                String url = "https://user-agents.net/download";
                String params = "browser=chrome&browser_type=browser&download=txt&version=" + chromeVersion;
                HttpRequest request = HttpRequest.newBuilder()
                                                 .uri(URI.create(url))
                                                 .header("Content-Type", "application/x-www-form-urlencoded")
                                                 .POST(HttpRequest.BodyPublishers.ofString(params))
                                                 .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                String[] tokens = response.body().split("\n");
                Set<String> set = new HashSet<>();
                for (String token : tokens) {
                    token = token.replaceAll("\\[.*]", "").trim();
                    set.add(token);
                }
                Files.createDirectories(Path.of(Constant.FAKE_USER_AGENT_FOLDER_PATH));
                Files.write(userAgentFile.toPath(), set, StandardOpenOption.CREATE, 
                            StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                userAgents = new ArrayList<>(set);
            } else {
                userAgents = Files.readAllLines(userAgentFile.toPath());
            }
            
            if (OS.isWindows()) {
                userAgents = userAgents.stream().filter(ua -> ua.contains("Windows NT 10.0;")).toList();
            } else if (OS.isLinux()) {
                userAgents = userAgents.stream().filter(ua -> ua.contains("Linux ")).toList();
            } else if (OS.isMacintosh()) {
                userAgents = userAgents.stream().filter(ua -> ua.contains("Macintosh")).toList();
            }
            int randomIndex = random.nextInt(userAgents.size());
            return userAgents.get(randomIndex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getOptionArguments() {
        if (optionArguments == null || optionArguments.isEmpty()) {
            List<String> arguments = new ArrayList<>();
            if (isFakeUserAgent()) {
                arguments.add("--user-agent=" + getRandomUserAgent());
            }
            
            if (proxyServer != null && !proxyServer.isEmpty()) {
                try {
                    if (proxyServer.contains("@")) {
                        int index1 = proxyServer.indexOf("://") + 3;
                        int index2 = proxyServer.indexOf("@");
                        String credentials = proxyServer.substring(index1, index2);
                        String[] tokens = credentials.split(":");
                        if (tokens.length == 2) {
                            proxyUsername = tokens[0];
                            proxyPassword = tokens[1];
                            tokens = proxyServer.substring(index2 + 1).split(":");
                            if (tokens.length == 2) {
                                proxyIP = tokens[0];
                                proxyPort = tokens[1];
                            }
                            
                            proxyServer = proxyServer.substring(0, index1) + proxyServer.substring(index2 + 1);
                        }
                    }
                } catch (Exception ignored) {
                }
                arguments.add("--proxy-server=" + proxyServer);
                
                if (!proxyServer.contains(DEFAULT_PROXY_SERVER)) {
//                    arguments.add("--load-extension=" + Path.of(Constant.PREVENT_WEB_RTC_LEAK_EXTENSION_PATH).toAbsolutePath());
                    arguments.add("--force-webrtc-ip-handling-policy");
                    arguments.add("--webrtc-ip-handling-policy=disable_non_proxied_udp");
                }
            }
            
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

            arguments.add("--remote-allow-origins=http://" + getDebuggerUrl());
            
            optionArguments = arguments;
        }
        return optionArguments;
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
            if (tempProfileFolderPath == null) {
                Files.createDirectories(Path.of(Constant.TEMP_PROFILE_FOLDER_PATH));
                tempProfileFolderPath = Files.createDirectories(Path.of(Constant.TEMP_PROFILE_FOLDER_PATH + "temp-profile-" + UUID.randomUUID()));
                setTempProfileForDeletion();
                Runtime.getRuntime().addShutdownHook(new Thread(this::deleteTempProfile));
                String defaultFolder = tempProfileFolderPath.toAbsolutePath() + FileSystems.getDefault().getSeparator() + "Default";
                Files.createDirectories(Path.of(defaultFolder));
                String preferencesFile = defaultFolder + FileSystems.getDefault().getSeparator() + "Preferences";
                Path preferencesPath = Path.of(preferencesFile);
                Files.write(preferencesPath, List.of(JacksonUtil.writeValueAsString(preferences)), StandardCharsets.UTF_8);
            }
            return tempProfileFolderPath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setTempProfileForDeletion() {
        try {
            Files.walkFileTree(tempProfileFolderPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        file.toFile().deleteOnExit();
                    } catch (Exception ignored) {
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    try {
                        dir.toFile().deleteOnExit();
                    } catch (Exception ignored) {
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
//            log.error(e.getMessage(), e);
        }
    }

    public void deleteTempProfile() {
        try {
            if (tempProfileFolderPath != null) {
                Files.walkFileTree(tempProfileFolderPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        try {
                            Files.delete(file);
                        } catch (Exception ignored) {
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                        if (e == null) {
                            try {
                                Files.delete(dir);
                            } catch (Exception ignored) {
                            }
                            return FileVisitResult.CONTINUE;
                        }
                        throw e;
                    }
                });
            }
        } catch (IOException e) {
//            log.error(e.getMessage(), e);
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
