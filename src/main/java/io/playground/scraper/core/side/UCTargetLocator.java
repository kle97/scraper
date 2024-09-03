package io.playground.scraper.core.side;

import io.playground.scraper.core.UCDriver;
import io.playground.scraper.core.UCElement;
import io.playground.scraper.model.response.frame.Frame;
import io.playground.scraper.model.response.frame.FrameProp;
import io.playground.scraper.model.response.frame.FrameTree;
import io.playground.scraper.model.response.target.TargetInfoProp;
import io.playground.scraper.model.response.target.TargetInfos;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UCTargetLocator implements WebDriver.TargetLocator {
    
    private final UCDriver driver;
    
    public UCTargetLocator(UCDriver driver) {
        this.driver = driver;
    }
    
    public List<TargetInfoProp> getTargets() {
        List<TargetInfoProp> targets = new ArrayList<>();
        TargetInfos targetInfos = driver.getClient().getTargets();
        if (targetInfos != null) {
            targets.addAll(targetInfos.targetInfos());
        }
        return targets;
    }
    
    public List<Frame> getFrames() {
        List<Frame> frames = new ArrayList<>();
        FrameTree frameTree = driver.getClient().getFrameTree();
        if (frameTree != null) {
            frames.add(frameTree.frameTree().frame());
            for (FrameProp frameProp : frameTree.frameTree().childFrames()) {
                Frame frame = frameProp.frame();
                frames.add(frame);
            }
        }
        return frames;
    }
    
    @Override
    public WebDriver frame(int index) {
        List<Frame> frames = getFrames();
        if (!frames.isEmpty()) {
            driver.getClient().activateTarget(frames.getFirst().id());
        }
        return driver;
    }

    @Override
    public WebDriver frame(String nameOrId) {
        List<Frame> frames = getFrames();
        if (!frames.isEmpty()) {
            for (Frame frame : frames) {
                if (frame.id().equals(nameOrId) || frame.url().equals(nameOrId)) {
                    driver.getClient().activateTarget(frame.id());
                    break;
                }
            }
        }
        return driver;
    }

    @Override
    public WebDriver frame(WebElement frameElement) {
        try {
            if (frameElement instanceof UCElement element) {
                WebDriver webDriver = element.getWrappedDriver();
                if (webDriver instanceof UCDriver ucDriver) {
                    String frameId = ucDriver.getClient().getCurrentFrameId();
                    driver.getClient().activateTarget(frameId);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public WebDriver parentFrame() {
        List<Frame> frames = getFrames();
        if (!frames.isEmpty()) {
            for (Frame frame : frames) {
                if (frame.parentId() == null) {
                    driver.getClient().activateTarget(frame.id());
                    break;
                }
            }
        }
        return driver;
    }

    @Override
    public WebDriver window(String nameOrHandle) {
        List<TargetInfoProp> targets = getTargets();
        if (!targets.isEmpty()) {
            for (TargetInfoProp target : targets) {
                if (target.title().equals(nameOrHandle)) {
                    driver.getClient().activateTarget(target.targetId());
                    break;
                }
            }
        }
        return driver;
    }

    @Override
    public WebDriver newWindow(WindowType typeHint) {
        String targetId = driver.getClient().createTarget("", typeHint.toString().equals("window"));
        driver.getClient().attachToTarget(targetId);
        return driver;
    }

    @Override
    public WebDriver defaultContent() {
        return parentFrame();
    }

    @Override
    public WebElement activeElement() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Alert alert() {
        return new UCAlert(driver);
    }
}
