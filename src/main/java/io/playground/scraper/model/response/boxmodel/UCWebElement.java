package io.playground.scraper.model.response.boxmodel;

import io.playground.UCWebDriver;
import io.playground.scraper.util.DevToolsClient;
import org.openqa.selenium.remote.RemoteWebElement;

public class UCWebElement extends RemoteWebElement {

    private final UCWebDriver parent;
    private final DevToolsClient client;

    public UCWebElement(UCWebDriver parent, String id) {
        this.parent = parent;
        this.client = parent.getClient();
        super.setParent(parent);
        super.setFileDetector(parent.getFileDetector());
        super.setId(id);
    }

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        click();
        String allKeysToSend = String.join("", keysToSend);
        for (int i = 0; i < allKeysToSend.length(); i++) {
            client.sendKey(allKeysToSend.charAt(i));
        }
    }

    @Override
    public void click() {
        BoxModel boxModel = client.getBoxModel(getId());
        if (boxModel != null) {
            Point center = boxModel.model().getCenter();
            moveMouseTo(center);
            client.clickMouse(center);
        }
    }

    private void moveMouseTo(Point point) {
        Point top = new Point(point.x(), 0);
        client.moveMouse(top.getRandom(10));
        client.moveMouse(top.getRandom(10));
        client.moveMouse(top.getRandom(10));
        client.moveMouse(top.getRandom(10));
        client.moveMouse(top.getRandom(10));
        client.moveMouse(top);
        for (Point p : point.getSpreadPoints(10)) {
            parent.getClient().moveMouse(p);
        }
    }
}
