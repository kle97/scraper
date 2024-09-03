package io.playground.scraper.core.side;

import io.playground.scraper.core.UCDriver;
import org.openqa.selenium.Alert;

public class UCAlert implements Alert {
    
    private final UCDriver driver;
    
    public UCAlert(UCDriver driver) {
        this.driver = driver;
    }
    
    @Override
    public void dismiss() {
        driver.getClient().handleJavaScriptDialog(false); 
    }

    @Override
    public void accept() {
        driver.getClient().handleJavaScriptDialog(true);
    }

    @Override
    public String getText() {
        return driver.getClient().getLastDialogMessage();
    }

    @Override
    public void sendKeys(String keysToSend) {
        driver.getClient().handleJavaScriptDialog(true, keysToSend);
    }
}
