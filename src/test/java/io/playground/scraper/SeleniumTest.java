package io.playground.scraper;

import io.playground.common.BaseTP;
import io.playground.scraper.core.UCDriver;
import io.playground.scraper.page.Pages;
import io.playground.scraper.page.detector.SeleniumDetectorPage;
import io.playground.scraper.page.g2.G2HomePage;
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

        SeleniumDetectorPage page = Pages.getSeleniumDetectorPage(driver);

        softly().as("status1").assertThat(page.getTestStatus()).isEqualTo("Passing...");
        page.takeTestStatusScreenshot();

        page.enterWindowToken(page.getWindowToken());
        softly().as("windowToken").assertThat(page.getTokenFieldValue()).isEqualTo(page.getWindowToken());

        page.enterWindowAsyncToken(page.getWindowAsyncToken());
        softly().as("windowAsyncToken").assertThat(page.getAsyncTokenFieldValue()).isEqualTo(page.getWindowAsyncToken());
        
        page.clickTestButton();
        page.clickTestResult();

        softly().as("status2").assertThat(page.getTestStatus()).isEqualTo("Passed!");
        page.takeTestStatusScreenshot();

        Reporter.addScreenshot(driver, "screenshot-selenium");
        driver.sleep(5000);
        driver.quit();
    }

    @Test
    public void testG2() {
        UCDriver driver = new UCDriver();
        String url = "https://g2.com";
        driver.get(url);
        G2HomePage page = Pages.getG2HomePage(driver);
        page.scrollToSoftwareReviewsButton();
        softly().as("Software Reviews button label")
                .assertThat(page.getSoftwareReviewsButtonLabel())
                .isEqualTo("Software Reviews");
        Reporter.addScreenshot(driver, "testG2");
        page.clickSoftwareReviewsButton();
        Reporter.addScreenshot(driver, "testG2");
        driver.quit();
    }
}
