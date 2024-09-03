package io.playground.scraper.core.side;

import io.playground.scraper.core.UCDriver;
import org.openqa.selenium.WebDriver;

import java.net.URL;

public class UCNavigation implements WebDriver.Navigation {
    
    private final UCDriver driver;
    
    public UCNavigation(UCDriver driver) {
        this.driver = driver;
    }
    
    @Override
    public void back() {
        driver.getClient().backPage();
    }

    @Override
    public void forward() {
        driver.getClient().forwardPage();
    }

    @Override
    public void to(String url) {
        driver.get(url);
    }

    @Override
    public void to(URL url) {
        driver.get(String.valueOf(url));
    }

    @Override
    public void refresh() {
        driver.getClient().reloadPage();
    }
}
