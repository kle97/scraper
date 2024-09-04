package io.playground.scraper.page.g2;

import io.playground.scraper.page.common.BasePage;
import io.playground.scraper.util.DriverUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class G2HomePage extends BasePage {

    @FindBy(xpath = "//*[text()='Software Reviews']")
    private WebElement softwareReviewsButton;

    public G2HomePage(WebDriver driver) {
        super(driver);
    }

    public String getSoftwareReviewsButtonLabel() {
        return softwareReviewsButton.getText();
    }

    public void clickSoftwareReviewsButton() {
        softwareReviewsButton.click();
    }

    public void scrollToSoftwareReviewsButton() {
        DriverUtil.scrollIntoView(softwareReviewsButton);
        DriverUtil.scrollIntoView(driver(), softwareReviewsButton);
    }
}
