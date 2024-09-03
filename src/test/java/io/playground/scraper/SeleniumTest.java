package io.playground.scraper;

import io.playground.common.BaseTP;
import io.playground.scraper.core.UCDriver;
import io.playground.scraper.util.DriverUtil;
import io.playground.scraper.util.Reporter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

@Slf4j
public class SeleniumTest extends BaseTP {

    @Test
    public void testBet365() {
        UCDriver driver = new UCDriver();
        String url = "https://bet365.com";
        driver.get(url);
        WebElement element = driver.findElement(By.xpath("//*[contains(text(), \"UEFA Champions League\")]"));
        element.click();
        DriverUtil.waitForLoadingToFinish(driver);
        WebElement element2 = driver.findElement(By.xpath("//*[contains(text(), 'Upcoming Matches')]"));
        DriverUtil.scrollIntoView(element2);
        softly().as("Upcoming Matches title").assertThat(element2.getText()).isEqualTo("Upcoming Matches");
        Reporter.addScreenshot(driver, "screenshot-selenium");
        driver.sleep(5000);
        driver.quit();
    }

    @Test
    public void testSeleniumDetector() {
        UCDriver driver = new UCDriver();
        String url = "https://hmaker.github.io/selenium-detector/";
        driver.get(url);
        WebElement element0 = driver.findElement(By.cssSelector(".test-status"));
        softly().as("status1").assertThat(element0.getText()).isEqualTo("Passing...");
        Reporter.addScreenshot(element0, "screenshot-selenium-element1");

        WebElement element1 = driver.findElement(By.cssSelector("#chromedriver-token"));
        String windowToken = (String) driver.executeScript("return window.token");
        element1.sendKeys(windowToken);
        softly().as("windowToken").assertThat(element1.getText()).isEqualTo(windowToken);

        WebElement element2 = driver.findElement(By.cssSelector("#chromedriver-asynctoken"));
        String windowAsyncToken = (String) driver.executeAsyncScript("window.getAsyncToken().then(arguments[0])");
        element2.sendKeys(windowAsyncToken);
        softly().as("windowAsyncToken").assertThat(element2.getText()).isEqualTo(windowAsyncToken);
        
        WebElement element3 = driver.findElement(By.cssSelector("#chromedriver-test"));
        element3.click();

        WebElement element4 = driver.findElement(By.cssSelector(".test-result"));
        element4.click();

        softly().as("status2").assertThat(element0.getText()).isEqualTo("Passed!");
        Reporter.addScreenshot(element0, "screenshot-selenium-element2");

        Reporter.addScreenshot(driver, "screenshot-selenium");
        driver.sleep(5000);
        driver.quit();
    }

    @Test
    public void testG2() {
        UCDriver driver = new UCDriver();
        String url = "https://g2.com";
        driver.get(url);
        driver.sleep(5000);
        driver.quit();
    }
}
