package io.playground.scraper.page.g2;

import io.playground.scraper.page.common.BasePage;
import io.playground.scraper.util.DriverUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class G2HomePage extends BasePage {

    @FindBy(xpath = "//*[text()='Write a Review']")
    private WebElement writeAReviewButton;

    @FindBy(xpath = "//*[text()='Claim Your G2 Profile']")
    private WebElement claimG2ProfileButton;

    @FindBy(xpath = "//*[text()='Software Reviews']")
    private WebElement softwareReviewsButton;
    
    public G2HomePage(WebDriver driver) {
        super(driver);
    }

    public String getSoftwareReviewsButtonLabel() {
        return softwareReviewsButton.getText();
    }

    public String getWriteAReviewButtonLabel() {
        return writeAReviewButton.getText();
    }

    public String getClaimG2ProfileButtonLabel() {
        return claimG2ProfileButton.getText();
    }

    public void clickSoftwareReviewsButton() {
        softwareReviewsButton.click();
        DriverUtil.waitForLoadingToFinish(driver());
    }

    public void scrollToSoftwareReviewsButton() {
        DriverUtil.scrollIntoView(softwareReviewsButton);
    }

    public void scrollToWriteAReviewButton() {
        DriverUtil.scrollIntoView(writeAReviewButton);
    }

    public void scrollToClaimG2ProfileButton() {
        DriverUtil.scrollIntoView(claimG2ProfileButton);
    }
}
