package io.playground.scraper.page.common;

import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import io.appium.java_client.pagefactory.Widget;
import io.appium.java_client.pagefactory.utils.WebDriverUnpackUtility;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.time.Duration;

public abstract class BaseWidget extends Widget {

    protected int timeoutInMs = BasePage.DEFAULT_TIMEOUT_IN_MS;
    protected WebDriver driver;

    protected BaseWidget(WebElement element) {
        super(element);
        this.driver = WebDriverUnpackUtility.unpackWebDriverFromSearchContext(element);
    }

    // constructor for custom initialization
    public BaseWidget(WebElement element, int timeoutInMs) {
        this(element);
        this.timeoutInMs = timeoutInMs;
        PageFactory.initElements(new AppiumFieldDecorator(element, Duration.ofMillis(timeoutInMs)), this);
    }
}
