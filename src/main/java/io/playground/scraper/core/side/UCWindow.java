package io.playground.scraper.core.side;

import io.playground.scraper.core.UCDriver;
import io.playground.scraper.model.response.window.WindowBounds;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;

public class UCWindow implements WebDriver.Window {

    private final UCDriver driver;

    public UCWindow(UCDriver driver) {
        this.driver = driver;
    }
    
    @Override
    public Dimension getSize() {
        WindowBounds bounds = driver.getClient().getWindowBounds(driver.getClient().getCurrentFrameId());
        if (bounds != null) {
            return new Dimension(bounds.width(), bounds.height());
        }
        return new Dimension(0, 0);
    }

    @Override
    public void setSize(Dimension targetSize) {
        driver.getClient().setWindowBounds(driver.getClient().getCurrentFrameId(),
                                           new WindowBounds(null, null, targetSize.width, targetSize.height, null));
    }

    @Override
    public Point getPosition() {
        WindowBounds bounds = driver.getClient().getWindowBounds(driver.getClient().getCurrentFrameId());
        if (bounds != null) {
            return new Point(bounds.left(), bounds.top());
        }
        return new Point(0, 0);
    }

    @Override
    public void setPosition(Point targetPosition) {
        driver.getClient().setWindowBounds(driver.getClient().getCurrentFrameId(),
                                           new WindowBounds(targetPosition.getX(), targetPosition.getY(), null, null, null));
    }

    @Override
    public void maximize() {
        driver.getClient().setWindowBounds(driver.getClient().getCurrentFrameId(),
                                           new WindowBounds(null, null, null, null, "maximized"));
    }

    @Override
    public void minimize() {
        driver.getClient().setWindowBounds(driver.getClient().getCurrentFrameId(),
                                           new WindowBounds(null, null, null, null, "minimized"));
    }

    @Override
    public void fullscreen() {
        driver.getClient().setWindowBounds(driver.getClient().getCurrentFrameId(),
                                           new WindowBounds(null, null, null, null, "fullscreen"));
    }
}
