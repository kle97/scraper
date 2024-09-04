package io.playground.scraper.page.detector;

import io.playground.scraper.page.common.BasePage;
import io.playground.scraper.util.Reporter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class SeleniumDetectorPage extends BasePage {

    private String windowToken;
    private String windowAsyncToken;

    @FindBy(css = ".test-status")
    private WebElement status;

    @FindBy(css = "#chromedriver-token")
    private WebElement tokenField;

    @FindBy(css = "#chromedriver-asynctoken")
    private WebElement asyncTokenField;

    @FindBy(css = "#chromedriver-test")
    private WebElement testButton;

    @FindBy(css = ".test-result")
    private WebElement testResult;

    public SeleniumDetectorPage(WebDriver driver) {
        super(driver);
    }

    public String getTestStatus() {
        return status.getText();
    }

    public void takeTestStatusScreenshot() {
        Reporter.addScreenshot(status, "TestStatus");
    }

    public void enterWindowToken(String windowToken) {
        tokenField.sendKeys(windowToken);
    }

    public String getTokenFieldValue() {
        return tokenField.getText();
    }

    public void enterWindowAsyncToken(String windowAsyncToken) {
        asyncTokenField.sendKeys(windowAsyncToken);
    }

    public String getAsyncTokenFieldValue() {
        return asyncTokenField.getText();
    }

    public void clickTestButton() {
        testButton.click();
    }

    public void clickTestResult() {
        testResult.click();
    }

    public String getWindowToken() {
        if (windowToken == null) {
            windowToken = (String) driver().executeScript("return window.token");
        }
        return windowToken;
    }

    public String getWindowAsyncToken() {
        if (windowAsyncToken == null) {
            windowAsyncToken = (String) driver().executeAsyncScript("window.getAsyncToken().then(arguments[0])");
        }
        return windowAsyncToken;
    }
}
