package io.playground.scraper.util;

import io.playground.scraper.core.DevToolsClient;
import io.playground.scraper.core.UCDriver;
import io.playground.scraper.core.UCElement;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.time.Duration;

@Slf4j
public class DriverUtil {
    
    private DriverUtil() {}
    
    public static void saveScreenshot(WebElement element, String title) {
        if (element instanceof UCElement ucElement) {
            ucElement.saveScreenshot(title);
        }
    }

    public static boolean waitForLoadingToFinish(UCDriver driver) {
        return waitForLoadingToFinish(driver, 4000);
    }
    
    public static boolean waitForLoadingToFinish(UCDriver driver, int timeoutInMs) {
        try {
            return Awaitility.waitAtMost(Duration.ofMillis(timeoutInMs))
                             .pollDelay(Duration.ZERO)
                             .ignoreExceptions()
                             .until(() -> driver.getClient().waitForLoadingEventToStop(), result -> result);
        } catch (Exception ignored){
        }
        return false;
    }
    
    public static void scrollIntoView(WebElement element) {
        if (element instanceof UCElement ucElement) {
            ucElement.getClient().scrollIntoViewIfNeeded(ucElement.getId());
        }
    }

    public static void scrollIntoView(WebElement container, WebElement element) {
        Rectangle containerRect = container.getRect();
        log.info("container.x: {}", containerRect.x);
        log.info("container.y: {}", containerRect.y);
        log.info("container.width: {}", container.getDomProperty("clientWidth"));
        log.info("container.height: {}", container.getDomProperty("clientHeight"));

        Rectangle elementRect = element.getRect();
        log.info("element.x: {}", elementRect.x);
        log.info("element.y: {}", elementRect.y);
        log.info("element.width: {}", elementRect.width);
        log.info("element.height: {}", elementRect.height);
    }

    public static void scrollIntoView(UCDriver driver, WebElement element) {
        DevToolsClient client = driver.getClient();
        UCElement root = new UCElement(driver, client.getRootObjectId());

        Rectangle containerRect = root.getRect();
        log.info("container.x: {}", containerRect.x);
        log.info("container.y: {}", containerRect.y);
        log.info("container.width: {}", containerRect.width);
        log.info("container.height: {}", containerRect.height);

        Rectangle elementRect = element.getRect();
        log.info("element.x: {}", elementRect.x);
        log.info("element.y: {}", elementRect.y);
        log.info("element.width: {}", elementRect.width);
        log.info("element.height: {}", elementRect.height);


    }
}
