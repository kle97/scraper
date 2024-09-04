package io.playground.scraper.util;

import io.playground.scraper.core.DevToolsClient;
import io.playground.scraper.core.UCDriver;
import io.playground.scraper.core.UCElement;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;

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
    
    public static void scrollIntoViewIfNeeded(WebElement element) {
        if (element instanceof UCElement ucElement) {
            ucElement.getClient().scrollIntoViewIfNeeded(ucElement.getId());
        }
    }

    public static void scrollIntoView(WebElement element) {
        try {
            WebElement container = ((WrapsDriver) element).getWrappedDriver().findElement(By.tagName("html"));
            scrollIntoView(container, element);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public static void scrollIntoView(WebElement container, WebElement element) {
        double scrollPercentage = 0.99;
        Rectangle containerRect = container.getRect();

        try {
            if (container instanceof UCElement && containerRect.height > 0 && containerRect.width > 0) {
                DevToolsClient client = ((UCElement) container).getClient();

                int containerScrollLeft = Integer.parseInt(container.getDomProperty("scrollLeft"));
                int containerScrollTop = Integer.parseInt(container.getDomProperty("scrollTop"));
                int containerClientLeft = Integer.parseInt(container.getDomProperty("clientLeft"));
                int containerClientTop = Integer.parseInt(container.getDomProperty("clientTop"));
                int containerClientWidth = Integer.parseInt(container.getDomProperty("clientWidth"));
                int containerClientHeight = Integer.parseInt(container.getDomProperty("clientHeight"));
                int containerCenterX = containerClientLeft + containerClientWidth / 2;
                int containerCenterY = containerClientTop + containerClientHeight / 2;

                int elementOffsetLeft = Integer.parseInt(element.getDomProperty("offsetLeft"));
                int elementOffsetTop = Integer.parseInt(element.getDomProperty("offsetTop"));
                int elementOffsetWidth = Integer.parseInt(element.getDomProperty("offsetWidth"));
                int elementOffsetHeight = Integer.parseInt(element.getDomProperty("offsetHeight"));
                int elementCenterX = elementOffsetLeft + elementOffsetWidth / 2;
                int elementCenterY = elementOffsetTop + elementOffsetHeight / 2;

//            log.info("containerScrollLeft: {}", containerScrollLeft);
//            log.info("containerScrollTop: {}", containerScrollTop);
//            log.info("containerClientLeft: {}", containerClientLeft);
//            log.info("containerClientTop: {}", containerClientTop);
//            log.info("containerClientWidth: {}", containerClientWidth);
//            log.info("containerClientHeight: {}", containerClientHeight);
//            log.info("elementOffsetLeft: {}", elementOffsetLeft);
//            log.info("elementOffsetTop: {}", elementOffsetTop);
//            log.info("elementOffsetWidth: {}", elementOffsetWidth);
//            log.info("elementOffsetHeight: {}", elementOffsetHeight);

                if (elementCenterX < containerScrollLeft) {
                    int x = containerClientLeft + 1;
                    int y = containerCenterY;
                    int xDistance = (int) (scrollPercentage * containerClientWidth);
                    int count = (containerScrollLeft - elementCenterX) / xDistance;
                    client.scrollGesture(x, y, xDistance, null, null, count);
                    return;
                }

                if (elementCenterX > containerScrollLeft + containerClientWidth) {
                    int x = containerClientLeft + containerClientWidth - 1;
                    int y = containerCenterY;
                    int xDistance = - (int) (scrollPercentage * containerClientWidth);
                    int count = (elementCenterX - containerScrollLeft) / Math.abs(xDistance);
                    client.scrollGesture(x, y, xDistance, null, null, count);
                    return;
                }

                if (elementCenterY < containerScrollTop) {
                    int x = containerCenterX;
                    int y = containerClientTop + 1;
                    int yDistance = (int) (scrollPercentage * containerClientHeight);
                    int count = (containerScrollTop - elementCenterY) / yDistance;
                    client.scrollGesture(x, y, null, yDistance, null, count);
                    return;
                }

                if (elementCenterY > containerScrollTop + containerClientHeight) {
                    int x = containerCenterX;
                    int y = containerClientTop + containerClientHeight - 1;
                    int yDistance = - (int) (scrollPercentage * containerClientHeight);
                    int count = (elementCenterY - containerScrollTop) / Math.abs(yDistance);
                    client.scrollGesture(x, y, null, yDistance, null, count);
                }
            }
        } catch (Exception e){
            log.error(e.getMessage());
        }
    }
}
