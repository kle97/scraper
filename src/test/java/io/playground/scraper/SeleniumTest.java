package io.playground.scraper;

import io.playground.common.BaseTP;
import io.playground.scraper.constant.Constant;
import io.playground.scraper.core.UCDriver;
import io.playground.scraper.core.UCDriverOptions;
import io.playground.scraper.page.Pages;
import io.playground.scraper.page.bet365.Bet365Page;
import io.playground.scraper.page.detector.SeleniumDetectorPage;
import io.playground.scraper.page.g2.G2HomePage;
import io.playground.scraper.util.Reporter;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Optional;
import org.testng.annotations.Test;

@Slf4j
public class SeleniumTest extends BaseTP {

    @Test
    public void testPixelScanWithProxy() {
        testPixelScan(UCDriverOptions.withProxyServer(Constant.PROVIDER_PROXY_SERVER_FILE_PATH)
                                     .fakeUserAgent(true)
                                     .build());
    }
    
    @Test
    public void testPixelScan(@Optional UCDriverOptions options) {
        UCDriver driver = new UCDriver(options);
        driver.get("https://ipfighter.com");
        driver.sleep(15000);
        Reporter.addBase64Screenshot(driver, "screenshot-selenium");
        driver.get("https://browserleaks.com/webrtc");
        driver.sleep(5000);
        driver.get("https://pixelscan.net");
        driver.sleep(10000);
        Reporter.addBase64Screenshot(driver, "screenshot-selenium");
        driver.quit();
    }

    @Test
    public void testBet365WithProxy() {
        testBet365(UCDriverOptions.withProxyServer(Constant.PROVIDER_PROXY_SERVER_FILE_PATH)
                                  .fakeUserAgent(true)
                                  .build());
    }
    
    @Test
    public void testBet365(@Optional UCDriverOptions options) {
        UCDriver driver = new UCDriver(options);
        String url = "https://bet365.com";
        driver.get(url);
        driver.sleep(3000);
        Bet365Page page = Pages.getBet365Page(driver);

        softly().as("UEFA Champions League")
                .assertThat(page.getUefaChampionsLeagueLabel())
                .isEqualTo("UEFA Champions League");
        
        page.clickUefaChampionsLeague();
        
        page.scrollToUpcomingMatches();
        
        softly().as("Upcoming Matches title")
                .assertThat(page.getUpcomingMatchesLabel())
                .isEqualTo("Upcoming Matches");
        
        Reporter.addBase64Screenshot(driver, "screenshot-selenium");
        driver.sleep(1000);
        driver.quit();
    }

    @Test
    public void testSeleniumDetectorWithProxy() {
        testSeleniumDetector(UCDriverOptions.withProxyServer(Constant.PROVIDER_PROXY_SERVER_FILE_PATH)
                                            .fakeUserAgent(true)
                                            .build());
    }

    @Test
    public void testSeleniumDetector(@Optional UCDriverOptions options) {
        UCDriver driver = new UCDriver(options);
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

        Reporter.addBase64Screenshot(driver, "screenshot-selenium");
        driver.quit();
    }

    @Test
    public void testG2WithProxy() {
        testG2(UCDriverOptions.withProxyServer(Constant.PROVIDER_PROXY_SERVER_FILE_PATH)
                              .fakeUserAgent(true)
                              .build());
    }

    @Test
    public void testG2(@Optional UCDriverOptions options) {
        UCDriver driver = new UCDriver(options);
        String url = "https://g2.com";
        driver.get(url);
        
        G2HomePage page = Pages.getG2HomePage(driver);
        
        page.scrollToSoftwareReviewsButton();
        softly().as("Software Reviews button label")
                .assertThat(page.getSoftwareReviewsButtonLabel())
                .isEqualTo("Software Reviews");
        Reporter.addBase64Screenshot(driver, "testG2");

        page.scrollToClaimG2ProfileButton();
        softly().as("Claim G2 Profile button label")
                .assertThat(page.getClaimG2ProfileButtonLabel())
                .isEqualTo("Claim Your G2 Profile");
        Reporter.addBase64Screenshot(driver, "testG2");

        page.scrollToWriteAReviewButton();
        softly().as("Write a Review button label")
                .assertThat(page.getWriteAReviewButtonLabel())
                .isEqualTo("Write a Review");
        Reporter.addBase64Screenshot(driver, "testG2");

        page.scrollToSoftwareReviewsButton();
        page.clickSoftwareReviewsButton();
        
        Reporter.addBase64Screenshot(driver, "testG2");
        driver.quit();
    }
}
