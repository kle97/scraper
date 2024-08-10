package io.playground.scraper;

import io.playground.scraper.constant.Constant;
import io.playground.scraper.util.DownloadUtil;
import io.playground.scraper.util.Patcher;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.service.DriverFinder;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@Slf4j
public class SeleniumTest {
    
    @Test
    public void download() {
        DownloadUtil.downloadLatestChromeDriver();
//        String patchedChromeDriverPath = Patcher.patchChromeDriver(DownloadUtil.downloadLatestChromeDriver());
    }

    @Test
    public void test() throws Exception {
        ChromeOptions chromeOptions = new ChromeOptions();
        System.setProperty("webdriver.chrome.driver", Patcher.patchChromeDriver(DownloadUtil.downloadLatestChromeDriver()));

        chromeOptions.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        chromeOptions.setExperimentalOption("useAutomationExtension", false);
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");

        chromeOptions.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36");

//        chromeOptions.addArguments("--window-size=1920,1080");
//        chromeOptions.addArguments("--headless=new");

        chromeOptions.addArguments("--start-maximized");
//        chromeOptions.addArguments("--remote-debugging-pipe");
//        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--no-default-browser-check");
        chromeOptions.addArguments("--no-first-run");

        for (Map.Entry<String, Object> entry : chromeOptions.asMap().entrySet()) {
            log.info(entry.getKey() + ": " + entry.getValue());
        }

        ChromeDriver driver = new ChromeDriver(chromeOptions);

        driver.get("https://hmaker.github.io/selenium-detector/");
//        driver.manage().window().setSize(new Dimension(1920, 1080));
        driver.findElement(By.cssSelector("#chromedriver-token"))
              .sendKeys((CharSequence) driver.executeScript("return window.token"));
        driver.findElement(By.cssSelector("#chromedriver-asynctoken"))
              .sendKeys((CharSequence) driver.executeAsyncScript("window.getAsyncToken().then(arguments[0])"));
        driver.findElement(By.cssSelector("#chromedriver-test")).click();

        Files.createDirectories(Path.of(Constant.SCREENSHOT_FOLDER_PATH));
        File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File destination = new File("screenshots/screenshot-selenium.png");
        Files.copy(source.toPath(), destination.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);

        driver.quit();
    }

    private File getChromeLocation() {
        ChromeOptions options = new ChromeOptions();
        options.setBrowserVersion("stable");
        DriverFinder finder = new DriverFinder(ChromeDriverService.createDefaultService(), options);
        return new File(finder.getBrowserPath());
    }
}
