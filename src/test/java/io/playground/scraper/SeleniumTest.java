package io.playground.scraper;

import io.playground.UCWebDriver;
import io.playground.scraper.constant.Constant;
import io.playground.scraper.util.DevToolsClient;
import io.playground.scraper.util.SleepUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@Slf4j
public class SeleniumTest {
    

    @Test
    public void test2() {
        UCWebDriver driver = new UCWebDriver();
        String url = "https://hmaker.github.io/selenium-detector/";
        driver.get(url);

        WebElement element1 = driver.findElement(By.cssSelector("#chromedriver-token"));
        element1.sendKeys((String) driver.executeScript("return window.token"));

        WebElement element2 = driver.findElement(By.cssSelector("#chromedriver-asynctoken"));
        element2.sendKeys((String) driver.executeAsyncScript("window.getAsyncToken().then(arguments[0])"));

        WebElement element3 = driver.findElement(By.cssSelector("#chromedriver-test"));
        element3.click();

        WebElement element4 = driver.findElement(By.cssSelector(".test-result"));
        element4.click();

        SleepUtil.sleep(5000);
        driver.quit();
    }

    @Test
    public void test4() {
        WebDriver driver = new UCWebDriver();
        String url = "https://g2.com";
        driver.get(url);
        SleepUtil.sleep(5000);
        driver.quit();
    }

    @Test
    public void test() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(UCDriver::stopBinary));
        UCDriver driver = new UCDriver();

        driver.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
        String url = "https://hmaker.github.io/selenium-detector/";
//        SleepUtil.sleep(4000);
        DevToolsClient browserClient = new DevToolsClient(UCDriver.getDevToolBrowserUrl());
//        driver.get(url);
//        SleepUtil.sleep(4000);
//        browserClient.getBrowserVersion();
//        DevToolsWebSocketClient pageClient = new DevToolsWebSocketClient(driver.getDevToolPageUrl(url));
        DevToolsClient pageClient = new DevToolsClient(UCDriver.getDevToolPageUrl("chrome://newtab/"));
        SleepUtil.sleep(100);
//        pageClient.sendMessage("{\"id\": 0, \"method\": \"Emulation.setFocusEmulationEnabled\", \"params\": {\"enabled\": true}}");
        SleepUtil.sleep(100);
        pageClient.send("{\"id\": 1, \"method\": \"Page.enable\"}");
        SleepUtil.sleep(100);
        browserClient.send("{\"id\": 2, \"method\": \"Page.getFrameTree\"}");
        SleepUtil.sleep(5000);
        pageClient.send("{\"id\": 3, \"method\": \"Page.navigate\", \"params\": {\"url\": \"https://hmaker.github.io/selenium-detector/\", \"transitionType\": \"link\"}}");
        SleepUtil.sleep(100);
//        SleepUtil.sleep(7000);
//        pageClient.querySelector("body");
//        pageClient.getDocument();
        SleepUtil.sleep(500);
        pageClient.close();
//        browserClient.close();

//        driver.findElement(By.cssSelector("#chromedriver-token"))
//              .sendKeys((CharSequence) driver.executeScript("return window.token"));
//        driver.findElement(By.cssSelector("#chromedriver-asynctoken"))
//              .sendKeys((CharSequence) driver.executeAsyncScript("window.getAsyncToken().then(arguments[0])"));
//        driver.findElement(By.cssSelector("#chromedriver-test")).click();
//        driver.findElement(By.cssSelector(".test-result")).click();
//
//        Files.createDirectories(Path.of(Constant.SCREENSHOT_FOLDER_PATH));
//        File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
//        File destination = new File("screenshots/screenshot-selenium.png");
//        Files.copy(source.toPath(), destination.getAbsoluteFile().toPath(), StandardCopyOption.REPLACE_EXISTING);

        SleepUtil.sleep(3000);

        driver.quit();
    }
}
