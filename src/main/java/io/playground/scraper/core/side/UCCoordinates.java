package io.playground.scraper.core.side;

import io.playground.scraper.core.UCElement;
import io.playground.scraper.util.DriverUtil;
import org.openqa.selenium.Point;
import org.openqa.selenium.interactions.Coordinates;

public class UCCoordinates implements Coordinates {

    private final UCElement element;

    public UCCoordinates(UCElement element) {
        this.element = element;
    }
    
    @Override
    public Point onScreen() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Point inViewPort() {
        DriverUtil.scrollIntoView(element);
        return element.getLocation();
    }

    @Override
    public Point onPage() {
        return element.getLocation();
    }

    @Override
    public Object getAuxiliary() {
        return element.getId();
    }
}
