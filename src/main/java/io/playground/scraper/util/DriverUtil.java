package io.playground.scraper.util;

import io.playground.scraper.core.DevToolsClient;
import io.playground.scraper.core.UCDriver;
import io.playground.scraper.core.UCElement;
import io.playground.scraper.model.response.boxmodel.Rect;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebElement;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

@Slf4j
public class DriverUtil {
    
    private DriverUtil() {}
    
    public static <T extends SearchContext> UCElement getUCElement(T element) {
        if (element instanceof WrapsDriver wrapsDriver && wrapsDriver.getWrappedDriver() instanceof UCDriver) {
            if (element instanceof RemoteWebElement) {
                return (UCElement) element;
            } else if (element instanceof WrapsElement wrapsElement) {
                return (UCElement) wrapsElement.getWrappedElement();
            }
        }
        throw new RuntimeException("The target element is not of type " + UCElement.class + "!");
    }

    public static Path saveScreenshot(WebDriver driver, String title) {
        if (driver instanceof UCDriver ucDriver) {
            return ucDriver.saveScreenshot(title);
        }
        return null;
    }
    
    public static <T extends SearchContext> Path saveScreenshot(T element, String title) {
        if (element instanceof WrapsDriver wrapsDriver && wrapsDriver.getWrappedDriver() instanceof UCDriver) {
            return getUCElement(element).saveScreenshot(title);
        }
        return null;
    }

    public static boolean waitForLoadingToFinish(WebDriver driver) {
        return waitForLoadingToFinish(driver, 4000);
    }
    
    public static boolean waitForLoadingToFinish(WebDriver driver, int timeoutInMs) {
        try {
            if (driver instanceof UCDriver ucDriver) {
                return Awaitility.waitAtMost(Duration.ofMillis(timeoutInMs))
                                 .pollDelay(Duration.ZERO)
                                 .ignoreExceptions()
                                 .until(() -> ucDriver.getClient().waitForLoadingEventToStop(), result -> result);
            }
        } catch (Exception ignored){
        }
        return false;
    }
    
    public static <T extends SearchContext> void scrollIntoViewIfNeeded(T element) {
        if (element instanceof WrapsDriver wrapsDriver && wrapsDriver.getWrappedDriver() instanceof UCDriver ucDriver) {
            ucDriver.getClient().scrollIntoViewIfNeeded(((RemoteWebElement) element).getId());
        }
    }

    public static <T extends SearchContext> void scrollIntoView(T element) {
        try {
            if (element instanceof WrapsDriver wrapsDriver) {
                WebElement container = wrapsDriver.getWrappedDriver().findElement(By.tagName("html"));
                scrollIntoView(container, element);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static <T extends SearchContext> Rect getRect(T element) {
        try {
            if (element instanceof WrapsDriver wrapsDriver && wrapsDriver.getWrappedDriver() instanceof UCDriver ucDriver) {
                DevToolsClient client = ucDriver.getClient();
                Rect elementRect = client.getRect(((RemoteWebElement) element).getId());
                if (elementRect != null) {
                    return elementRect;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Rect.defaultRect();
    }

    public static <T extends SearchContext> void scrollIntoView(T container, T element) {
        try {
            if (container instanceof WrapsDriver && element instanceof WrapsDriver wrapsDriver 
                    && wrapsDriver.getWrappedDriver() instanceof UCDriver ucDriver) {
                DevToolsClient client = ucDriver.getClient();
                Rect elementRect = getRect(element);
                if (elementRect.isNotNull()) {
                    String script = """
                                    let elementRect = arguments[0];
                                    obj.scrollTo(elementRect.x, elementRect.y);
                                    """;
                    client.executeScript(script, ((RemoteWebElement) container).getId(), Map.of("value", elementRect));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static <T extends SearchContext> void scrollIntoView2(T container, T element) {
        double scrollPercentage = 0.99;

        try {
            if (container instanceof WrapsDriver && element instanceof WrapsDriver wrapsDriver
                    && wrapsDriver.getWrappedDriver() instanceof UCDriver ucDriver) {
                DevToolsClient client = ucDriver.getClient();
                UCElement ucContainer = getUCElement(container);
                UCElement ucElement = getUCElement(element);

                int containerScrollLeft = Integer.parseInt(ucContainer.getDomProperty("scrollLeft"));
                int containerScrollTop = Integer.parseInt(ucContainer.getDomProperty("scrollTop"));
                int containerClientLeft = Integer.parseInt(ucContainer.getDomProperty("clientLeft"));
                int containerClientTop = Integer.parseInt(ucContainer.getDomProperty("clientTop"));
                int containerClientWidth = Integer.parseInt(ucContainer.getDomProperty("clientWidth"));
                int containerClientHeight = Integer.parseInt(ucContainer.getDomProperty("clientHeight"));
                int containerCenterX = containerClientLeft + containerClientWidth / 2;
                int containerCenterY = containerClientTop + containerClientHeight / 2;

                int elementOffsetLeft = Integer.parseInt(ucElement.getDomProperty("offsetLeft"));
                int elementOffsetTop = Integer.parseInt(ucElement.getDomProperty("offsetTop"));
                int elementOffsetWidth = Integer.parseInt(ucElement.getDomProperty("offsetWidth"));
                int elementOffsetHeight = Integer.parseInt(ucElement.getDomProperty("offsetHeight"));
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
