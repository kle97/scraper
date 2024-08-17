package io.playground.scraper;

import io.playground.scraper.constant.Constant;
import io.playground.scraper.util.DevToolsWebSocketClient;
import io.playground.scraper.util.SleepUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
public class SeleniumTest {
    
    private UCDriver driver;

    @Test
    public void tempTest() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(UCDriver::stopBinary));
        driver = new UCDriver();
        
        log.info(driver.getCapabilities().toString());
        driver.get("https://xamvn.name");

        Files.createDirectories(Path.of(Constant.SCREENSHOT_FOLDER_PATH));
        File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File destination = new File("screenshots/screenshot-selenium.png");
        Files.copy(source.toPath(), destination.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);

        SleepUtil.sleep(3000);

        driver.quit();
    }

    @Test
    public void test() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(UCDriver::stopBinary));
        driver = new UCDriver();

//        driver.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        String url = "https://hmaker.github.io/selenium-detector/";
        SleepUtil.sleep(7000);
        DevToolsWebSocketClient browserClient = new DevToolsWebSocketClient(driver.getDevToolBrowserUrl());
        driver.get(url);
        browserClient.getBrowserVersion();
        DevToolsWebSocketClient pageClient = new DevToolsWebSocketClient(driver.getDevToolPageUrl(url));
//        SleepUtil.sleep(7000);
//        pageClient.querySelector("body");
        pageClient.getDocument();
        SleepUtil.sleep(500);
        pageClient.close();
        browserClient.close();

        driver.findElement(By.cssSelector("#chromedriver-token"))
              .sendKeys((CharSequence) driver.executeScript("return window.token"));
        driver.findElement(By.cssSelector("#chromedriver-asynctoken"))
              .sendKeys((CharSequence) driver.executeAsyncScript("window.getAsyncToken().then(arguments[0])"));
        driver.findElement(By.cssSelector("#chromedriver-test")).click();
        driver.findElement(By.cssSelector(".test-result")).click();

        Files.createDirectories(Path.of(Constant.SCREENSHOT_FOLDER_PATH));
        File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        File destination = new File("screenshots/screenshot-selenium.png");
        Files.copy(source.toPath(), destination.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);

        SleepUtil.sleep(3000);

        driver.quit();
    }
}
