package io.playground.scraper;

import io.playground.UCWebDriver;
import io.playground.scraper.constant.Constant;
import io.playground.scraper.util.DevToolsClient;
import io.playground.scraper.util.SleepUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

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

        SleepUtil.sleep(1000);

        driver.quit();
    }

    @Test
    public void test2() {
        WebDriver driver = new UCWebDriver();
        String url = "https://hmaker.github.io/selenium-detector/";
        driver.get(url);
        SleepUtil.sleep(2000);
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
    public void test3() {
        String url = "https://hmaker.github.io/selenium-detector/";
        DevToolsClient client = new DevToolsClient(UCWebDriver.getDevToolFirstTabUrl("127.0.0.1:9222"));
        client.enablePage();
        if (client.isOpen()) {
            client.navigate(url);
        }
        
        int rootNodeId = client.getRootNodeId();
        log.info("rootNodeId: {}", rootNodeId);
        
        String currentFrameId = client.getCurrentFrameId();
        log.info("currentFrameId: {}", currentFrameId);

        int executionContextId = client.createIsolatedWorld(currentFrameId).executionContextId();
        log.info("executionContextId: {}", executionContextId);
        
        String rootObjectId = client.resolveNode(rootNodeId, executionContextId).object().objectId();
        log.info("rootObjectId: {}", rootObjectId);

        String elementsObjectId = client.callFunctionOn("obj.querySelectorAll(arguments[0])", executionContextId, rootObjectId, Map.of("value", "#chromedriver-token")).result().objectId();
        log.info("elementsObjectId: {}", elementsObjectId);
        
        client.close();
    }

    @Test
    public void test() throws Exception {
        Runtime.getRuntime().addShutdownHook(new Thread(UCDriver::stopBinary));
        driver = new UCDriver();

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
