package io.playground.scraper.core;

import io.playground.scraper.core.side.UCCoordinates;
import io.playground.scraper.model.response.ObjectNode;
import io.playground.scraper.model.response.ScriptNode;
import io.playground.scraper.model.response.boxmodel.BoxModel;
import io.playground.scraper.model.response.boxmodel.Point;
import io.playground.scraper.model.response.css.CSSStyle;
import io.playground.scraper.model.response.screenshot.ViewPort;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Coordinates;
import org.openqa.selenium.remote.RemoteWebElement;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class UCElement extends RemoteWebElement {
    
    private int nodeId = -1;

    private final UCDriver driver;
    private final DevToolsClient client;

    public UCElement(UCDriver driver, String id) {
        this.driver = driver;
        this.client = driver.getClient();
        super.setParent(driver);
        super.setFileDetector(driver.getFileDetector());
        super.setId(id);
    }
    
    public DevToolsClient getClient() {
        return driver.getClient();
    }

    @Override
    public String getId() {
        return super.getId();
    }
    
    public int getNodeId() {
        if (nodeId < 1) {
            nodeId = client.requestNode(getId());
        }
        if (nodeId < 1) {
            throw new NoSuchElementException("Node id for element " + getId() + " not found!");
        } else {
            return nodeId;
        }
    }

    @Override
    public void submit() {
        String script = """
                var form = this;
                while (form.nodeName != 'FORM' && form.parentNode) {
                    form = form.parentNode;
                }
                if (!form) {
                    throw Error('Unable to find containing form element');
                }
                if (!form.ownerDocument) {
                    throw Error('Unable to find owning document');
                }
                var e = form.ownerDocument.createEvent('Event');
                e.initEvent('submit', true, true);
                if (form.dispatchEvent(e)) {
                    HTMLFormElement.prototype.submit.call(form)
                }
                """;
        try {
            client.executeScript(script, getId());
        } catch (Exception ignored) {
        }
    }

    @Override
    public void clear() {
        try {
            client.executeScript("obj.value = ''", getId());
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getDomProperty(String name) {
        try {
            ScriptNode scriptNode = client.executeScript("return obj[arguments[0]]", getId(), Map.of("value", name));
            if (scriptNode != null) {
                if (scriptNode.result().value() != null) {
                    return String.valueOf(scriptNode.result().value());
                }
            }
        } catch (Exception ignored) {
        }
        return UCDriver.ELEMENT_NOT_FOUND;
    }

    @Override
    public String getDomAttribute(String name) {
        try {
            Map<String, Object> attributes = client.getAttributes(getNodeId());
            if (attributes.containsKey(name)) {
                Object value = attributes.get(name);
                if (value != null) {
                    return String.valueOf(value);
                }
            }
        } catch (Exception ignored){
        }
        return UCDriver.ELEMENT_NOT_FOUND;
    }

    @Override
    public String getAttribute(String name) {
        return getDomAttribute(name);
    }

    @Override
    public String getAriaRole() {
        return "NOT_SUPPORTED";
    }

    @Override
    public String getAccessibleName() {
        return getDomProperty("id");
    }

    @Override
    public String getTagName() {
        return getDomProperty("tagName");
    }

    @Override
    public boolean isSelected() {
        return !Boolean.parseBoolean(getDomProperty("checked"));
    }

    @Override
    public boolean isEnabled() {
        return !Boolean.parseBoolean(getDomProperty("disabled"));
    }

    @Override
    public boolean isDisplayed() {
        Rectangle rect = getRect();
        return !Boolean.parseBoolean(getDomProperty("hidden")) && rect.width > 0 && rect.height > 0;
    }

    @Override
    public String getText() {
        String text = getDomProperty("value");
        if (text.equals(UCDriver.ELEMENT_NOT_FOUND)) {
            text = getDomProperty("outerText");
        }
        if (text.equals(UCDriver.ELEMENT_NOT_FOUND)) {
            text = getDomProperty("innerText");
        }
        return text;
    }

    @Override
    public String getCssValue(String propertyName) {
        try {
            for (CSSStyle cssStyle : client.getCSSStyle(getNodeId())) {
                if (cssStyle.name().equals(propertyName)) {
                    return cssStyle.value();
                }
            }
        } catch (Exception ignored) {
        }
        return UCDriver.ELEMENT_NOT_FOUND;
    }

    @Override
    public List<WebElement> findElements(By by) {
        return driver.findElements(by, getId(), null);
    }

    @Override
    public WebElement findElement(By by) {
        List<WebElement> elements = driver.findElements(by, getId(), 0);
        if (!elements.isEmpty()) {
            return elements.getFirst();
        } else {
            throw new NoSuchElementException("No such element for locator: '" + by + "'!");
        }
    }

    @Override
    public SearchContext getShadowRoot() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WebDriver getWrappedDriver() {
        return driver;
    }

    @Override
    public org.openqa.selenium.Point getLocation() {
        try {
            int x = Integer.parseInt(getDomProperty("clientLeft"));
            int y = Integer.parseInt(getDomProperty("clientTop"));
            return new org.openqa.selenium.Point(x, y);
        } catch (Exception ignored) {
        }
        return new org.openqa.selenium.Point(0, 0);
    }

    @Override
    public Dimension getSize() {
        BoxModel boxModel = client.getBoxModel(super.getId());
        if (boxModel != null) {
            return new Dimension((int) boxModel.model().width(), (int) boxModel.model().height());
        }
        return new Dimension(0, 0);
    }

    @Override
    public Rectangle getRect() {
        BoxModel boxModel = client.getBoxModel(super.getId());
        if (boxModel != null) {
            return new Rectangle((int) boxModel.model().getTopLeft().x(), 
                                 (int) boxModel.model().getTopLeft().y(), 
                                 (int) boxModel.model().width(), 
                                 (int) boxModel.model().height());
        }
        return new Rectangle(new org.openqa.selenium.Point(0, 0), new Dimension(0, 0));
    }

    @Override
    public Coordinates getCoordinates() {
        return new UCCoordinates(this);
    }
    
    public WebElement getParent() {
        Integer parentNodeId = client.getContainerForNode(getNodeId());
        if (parentNodeId != null) {
            ObjectNode parentNode = client.resolveNode(parentNodeId, client.getCurrentExecutionContextId());
            if (parentNode != null) {
                return new UCElement(driver, parentNode.object().objectId());
            }
        }
        throw new NoSuchElementException("Can't find parent of element with id " + getId() + "!");
    }

    public Path saveScreenshot(String title) {
        BoxModel boxModel = client.getBoxModel(super.getId());
        if (boxModel != null) {
            Point topLeft = boxModel.model().getTopLeft();
            ViewPort viewPort = new ViewPort(topLeft.x(), topLeft.y(),
                                             (int) boxModel.model().width(),
                                             (int) boxModel.model().height(),
                                             1.0);
            return driver.saveScreenshot(title, viewPort);
        }
        return driver.saveScreenshot(title);
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> outputType) throws WebDriverException {
        BoxModel boxModel = client.getBoxModel(super.getId());
        if (boxModel != null) {
            Point topLeft = boxModel.model().getTopLeft();
            ViewPort viewPort = new ViewPort(topLeft.x(), topLeft.y(), 
                                             (int) boxModel.model().width(),
                                             (int) boxModel.model().height(),
                                             1.0);
            return driver.getScreenshotAs(outputType, viewPort);
        }
        return driver.getScreenshotAs(outputType);
    }

    @Override
    public Map<String, Object> toJson() {
        return super.toJson();
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
            driver.getClient().moveMouse(p);
        }
    }
}
