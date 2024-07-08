package io.playground.scraper;

import com.microsoft.playwright.*;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class PlaywrightStarter {

    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            List<BrowserType> browserTypes = List.of(playwright.chromium(), playwright.webkit(), playwright.firefox());
            for (BrowserType browserType : browserTypes) {
                try (Browser browser = browserType.launch()) {
                    BrowserContext context = browser.newContext();
                    Page page = context.newPage();
                    page.navigate("https://playwright.dev/");
                    page.screenshot(new Page.ScreenshotOptions()
                                            .setPath(Paths.get("screenshots/screenshot-" + browserType.name() + ".png")));
                }
            }
        }
    }
}
