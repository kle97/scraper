package io.playground.scraper.util;

import io.playground.scraper.core.UCDriver;
import io.playground.scraper.core.UCElement;
import org.awaitility.Awaitility;
import org.openqa.selenium.WebElement;

import java.time.Duration;

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
}
