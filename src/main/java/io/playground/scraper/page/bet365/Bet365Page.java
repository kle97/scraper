package io.playground.scraper.page.bet365;

import io.playground.scraper.page.common.BasePage;
import io.playground.scraper.util.DriverUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class Bet365Page extends BasePage {
    
    @FindBy(xpath = "//*[contains(text(), \"UEFA Champions League\")]")
    private WebElement uefaChampionsLeague;

    @FindBy(xpath = "//div[contains(text(), 'Upcoming Matches')]")
    private WebElement upcomingMatches;

    @FindBy(css = ".wcl-PageContainer-scrollable")
    private WebElement pageContainer;

    public Bet365Page(WebDriver driver) {
        super(driver);
    }

    public String getUefaChampionsLeagueLabel() {
        return uefaChampionsLeague.getText();
    }

    public void clickUefaChampionsLeague() {
        uefaChampionsLeague.click();
        DriverUtil.waitForLoadingToFinish(driver());
    }

    public String getUpcomingMatchesLabel() {
        return upcomingMatches.getText();
    }

    public void scrollToUpcomingMatches() {
        DriverUtil.scrollIntoView(pageContainer, upcomingMatches);
    }
}
