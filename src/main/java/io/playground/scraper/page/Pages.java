package io.playground.scraper.page;

import io.playground.scraper.page.bet365.Bet365Page;
import io.playground.scraper.page.detector.SeleniumDetectorPage;
import io.playground.scraper.page.g2.G2HomePage;
import org.openqa.selenium.WebDriver;

public class Pages {

    private Pages() {}

    public static SeleniumDetectorPage getSeleniumDetectorPage(WebDriver driver) {
        return new SeleniumDetectorPage(driver);
    }

    public static G2HomePage getG2HomePage(WebDriver driver) {
        return new G2HomePage(driver);
    }
    
    public static Bet365Page getBet365Page(WebDriver driver) {
        return new Bet365Page(driver);
    }
}
