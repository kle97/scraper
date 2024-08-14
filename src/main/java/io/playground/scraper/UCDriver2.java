package io.playground.scraper;

import org.openqa.selenium.Beta;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.chromium.ChromiumDriverCommandExecutor;
import org.openqa.selenium.internal.Require;
import org.openqa.selenium.remote.CommandInfo;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebDriverBuilder;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.remote.service.DriverFinder;
import org.openqa.selenium.remote.service.DriverService;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UCDriver2 extends UCDriver3 {

    /**
     * Creates a new ChromeDriver using the {@link ChromeDriverService#createDefaultService default}
     * server configuration.
     *
     * @see #UCDriver2(ChromeDriverService, ChromeOptions)
     */
    public UCDriver2() {
        this(ChromeDriverService.createDefaultService(), new ChromeOptions());
    }

    /**
     * Creates a new ChromeDriver instance. The {@code service} will be started along with the driver,
     * and shutdown upon calling {@link #quit()}.
     *
     * @param service The service to use.
     * @see RemoteWebDriver#RemoteWebDriver(org.openqa.selenium.remote.CommandExecutor, Capabilities)
     */
    public UCDriver2(ChromeDriverService service) {
        this(service, new ChromeOptions());
    }

    /**
     * Creates a new ChromeDriver instance with the specified options.
     *
     * @param options The options to use.
     * @see #UCDriver2(ChromeDriverService, ChromeOptions)
     */
    public UCDriver2(ChromeOptions options) {
        this(ChromeDriverService.createDefaultService(), options);
    }

    /**
     * Creates a new ChromeDriver instance with the specified options. The {@code service} will be
     * started along with the driver, and shutdown upon calling {@link #quit()}.
     *
     * @param service The service to use.
     * @param options The options required from ChromeDriver.
     */
    public UCDriver2(ChromeDriverService service, ChromeOptions options) {
        this(service, options, ClientConfig.defaultConfig());
    }

    public UCDriver2(
            ChromeDriverService service, ChromeOptions options, ClientConfig clientConfig) {
        super(generateExecutor(service, options, clientConfig), options, ChromeOptions.CAPABILITY);
        casting = new AddHasCasting().getImplementation(getCapabilities(), getExecuteMethod());
        cdp = new AddHasCdp().getImplementation(getCapabilities(), getExecuteMethod());
    }

    private static ChromeDriverCommandExecutor generateExecutor(
            ChromeDriverService service, ChromeOptions options, ClientConfig clientConfig) {
        Require.nonNull("Driver service", service);
        Require.nonNull("Driver options", options);
        Require.nonNull("Driver clientConfig", clientConfig);
        DriverFinder finder = new DriverFinder(service, options);
        service.setExecutable(finder.getDriverPath());
        if (finder.hasBrowserPath()) {
            options.setBinary(finder.getBrowserPath());
            options.setCapability("browserVersion", (Object) null);
        }
        return new ChromeDriverCommandExecutor(service, clientConfig);
    }

    @Beta
    public static RemoteWebDriverBuilder builder() {
        return RemoteWebDriver.builder().oneOf(new ChromeOptions());
    }

    private static class ChromeDriverCommandExecutor extends ChromiumDriverCommandExecutor {
        public ChromeDriverCommandExecutor(DriverService service, ClientConfig clientConfig) {
            super(service, getExtraCommands(), clientConfig);
        }

        private static Map<String, CommandInfo> getExtraCommands() {
            return Stream.of(
                                 new AddHasCasting().getAdditionalCommands(), new AddHasCdp().getAdditionalCommands())
                         .flatMap((m) -> m.entrySet().stream())
                         .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }
}
