package io.playground.scraper.page.common;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import io.appium.java_client.proxy.Helpers;
import io.appium.java_client.proxy.MethodCallListener;
import io.playground.scraper.core.UCDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public abstract class BasePage {

    public static final int DEFAULT_TIMEOUT_IN_MS = 2000;
    private final int timeoutInMs;
    private final WebDriver driver;

    public BasePage(WebDriver driver) {
        this(driver, DEFAULT_TIMEOUT_IN_MS);
    }

    public BasePage(WebDriver driver, int timeoutInMs) {
        this.driver = driver;
        this.timeoutInMs = timeoutInMs;
        PageFactory.initElements(new AppiumFieldDecorator(driver, Duration.ofMillis(timeoutInMs)), this);
    }

    public BasePage(WebDriver driver, int timeoutInMs, MethodCallListener methodCallListener) {
        if (methodCallListener != null && driver instanceof AppiumDriver) {
            try {
                AppiumDriver appiumDriver = (AppiumDriver) driver;
                String remoteSessionAddress = appiumDriver.getRemoteAddress() + "/session/" + appiumDriver.getSessionId();
                URL remoteSessionURL = new URL(remoteSessionAddress);
                if (driver instanceof IOSDriver) {
                    this.driver = Helpers.createProxy(IOSDriver.class,
                                                      new Object[]{remoteSessionURL},
                                                      new Class<?>[]{URL.class},
                                                      methodCallListener);
                } else {
                    String automationName = (String) appiumDriver.getCapabilities().getCapability("automationName");
                    this.driver = Helpers.createProxy(AndroidDriver.class,
                                                      new Object[]{remoteSessionURL, automationName},
                                                      new Class<?>[]{URL.class, String.class},
                                                      methodCallListener);
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.driver = driver;
        }

        this.timeoutInMs = timeoutInMs;
        PageFactory.initElements(new AppiumFieldDecorator(this.driver, Duration.ofMillis(timeoutInMs)), this);
    }
    
    public UCDriver driver() {
        return (UCDriver) this.driver;
    }
}
