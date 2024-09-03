package io.playground.scraper.core.side;

import io.playground.scraper.core.UCDriver;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.TimeUnit;

public class UCTimeouts implements WebDriver.Timeouts {

    private final UCDriver driver;

    public UCTimeouts(UCDriver driver) {
        this.driver = driver;
    }
    
    @Override
    public WebDriver.Timeouts implicitlyWait(long time, TimeUnit unit) {
        return this;
    }

    @Override
    public WebDriver.Timeouts setScriptTimeout(long time, TimeUnit unit) {
        return this;
    }

    @Override
    public WebDriver.Timeouts pageLoadTimeout(long time, TimeUnit unit) {
        return this;
    }
}
