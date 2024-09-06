package io.playground.scraper.util;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.model.Media;
import com.aventstack.extentreports.observer.ExtentObserver;
import com.aventstack.extentreports.observer.entity.ObservedEntity;
import com.aventstack.extentreports.reporter.AbstractFileReporter;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import io.playground.scraper.constant.AnsiColor;
import io.playground.scraper.constant.Constant;
import io.playground.scraper.core.UCDriver;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WrapsDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class Reporter {
    private static final List<String> reporterFilePaths = new ArrayList<>();
    private static final ExtentReports extentReports = new ExtentReports();

    private static final ThreadLocal<ExtentTest> currentNode = new ThreadLocal<>();
    private static final Map<String, ExtentTest> testMap = new HashMap<>();

    public static void startReport() {
        String timeStamp = DateTimeFormatter.ofPattern("MM-dd-yyyy-HH-mm-ss")
                                            .withZone(ZoneId.systemDefault())
                                            .format(Instant.now());
        String filePath = "playground-test-suite" + "-" + timeStamp + ".html";
        String reportFilePath = Constant.REPORT_FOLDER_PATH + filePath;
        try {
            Files.createDirectories(Paths.get(Constant.REPORT_FOLDER_PATH));
            Files.createDirectories(Paths.get(Constant.SCREENSHOT_FOLDER_PATH));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        startReport(reportFilePath);
    }

    public static void startReport(String reportFilePath) {
        ExtentSparkReporter spark = new ExtentSparkReporter(reportFilePath);
        spark.config().thumbnailForBase64(true);
        spark.config().setCss(".col-md-3 > img { max-width: 180px; max-height: 320px; } .col-md-3 > .title { max-width: 180px; }");
        spark.config().setJs("""
                             for (let element of document.getElementsByClassName('card')) {
                                if (element.getElementsByClassName('collapse').length > 0) {
                                    element.getElementsByClassName('node').item(0).classList.add('collapsed')
                                }
                             }""");
        attachReporter(spark);
    }

    @SafeVarargs
    public static <T extends ObservedEntity> void attachReporter(ExtentObserver<T>... observers) {
        extentReports.attachReporter(observers);
        for (ExtentObserver<T> observer : observers) {
            if (observer instanceof AbstractFileReporter) {
                String path = ((AbstractFileReporter) observer).getFile().toURI().toString()
                                                               .replace("file:/", "file:///");
                reporterFilePaths.add(path);
            }
        }
    }

    public static void addSystemInfo(String key, String value) {
        extentReports.setSystemInfo(key, value);
    }

    public static void flush() {
        extentReports.flush();
        for (String reportFilePath : reporterFilePaths) {
            log.info(reportFilePath);
        }
    }

    public static ExtentTest addReport(String reportName) {
        ExtentTest test;
        if (testMap.containsKey(reportName)) {
            test = testMap.get(reportName);
        } else {
            test = extentReports.createTest(reportName);
            testMap.put(reportName, test);
        }
        return test;
    }

    public static ExtentTest appendReport(String reportName) {
        ExtentTest test = getCurrentTest();
        ExtentTest node = test.createNode(reportName);
        currentNode.set(node);
        return node;
    }

    public static ExtentTest appendReport(String testName, String reportName) {
        ExtentTest node = addReport(testName).createNode(reportName);
        currentNode.set(node);
        return node;
    }

    public static ExtentTest getCurrentNode() {
        return currentNode.get() != null ? currentNode.get() : appendReport("Default Node " + UUID.randomUUID());
    }

    public static ExtentTest getCurrentTest() {
        ITestResult currentTestResult = org.testng.Reporter.getCurrentTestResult();
        if (currentTestResult != null) {
            return addReport(currentTestResult.getInstance().getClass().getSimpleName());
        } else {
            return addReport("Default Test " + UUID.randomUUID());
        }
    }

    public static ExtentTest addScreenshotFromBase64String(String base64String) {
        return addScreenshotFromBase64String(base64String, null);
    }

    public static ExtentTest addScreenshotFromBase64String(String base64String, String title) {
        Media media = MediaEntityBuilder.createScreenCaptureFromBase64String("data:image/png;base64," + base64String, title).build();
        return logWithoutLogger(Status.INFO, null, null, media);
    }

    public static <T extends TakesScreenshot> ExtentTest addBase64Screenshot(T element, String title) {
        try {
            return addScreenshotFromBase64String(element.getScreenshotAs(OutputType.BASE64), title);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static <T extends SearchContext> ExtentTest addScreenshot(T element, String title) {
        if (element instanceof WrapsDriver wrapsDriver && wrapsDriver.getWrappedDriver() instanceof UCDriver) {
            return addScreenshot(DriverUtil.getUCElement(element).saveScreenshot(title).toAbsolutePath().toString(), title);
        }
        return null;
    }

    public static ExtentTest addScreenshot(UCDriver driver, String title) {
        return addScreenshot(driver.saveScreenshot(title).toAbsolutePath().toString(), null);
    }

    public static ExtentTest addScreenshot(String screenshotPath) {
        return addScreenshot(screenshotPath, null);
    }

    public static ExtentTest addScreenshot(String screenshotPath, String title) {
        Media media = MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath, title).build();
        return logWithoutLogger(Status.INFO, title, null, media);
    }

    public static ExtentTest info(String format, Object... args) {
        return info(MessageFormatter.arrayFormat(format, args).getMessage());
    }

    public static ExtentTest info(String message) {
        return logWithLogger(Status.INFO, message, null, null);
    }

    public static ExtentTest pass(String message) {
        return logWithLogger(Status.PASS, message, null, null);
    }

    public static ExtentTest fail(String message) {
        return logWithLogger(Status.FAIL, message, null, null);
    }

    public static ExtentTest fail(String message, Throwable throwable) {
        return logWithLogger(Status.FAIL, message, throwable, null);
    }

    public static ExtentTest fail(String message, String screenshotPath) {
        return fail(message, null, screenshotPath);
    }

    public static ExtentTest fail(String message, Throwable throwable, String screenshotPath) {
        Media media = MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build();
        return logWithLogger(Status.FAIL, message, throwable, media);
    }

    public static ExtentTest logWithLogger(Status status, String message, Throwable throwable, Media media) {
        if (status.equals(Status.INFO)) {
            getCurrentLogger().info(message);
        } else if (status.equals(Status.PASS)) {
            getCurrentLogger().info("    " + AnsiColor.GREEN_BOLD + "PASS" + AnsiColor.RESET + "    " + message);
        } else if (status.equals(Status.FAIL)) {
            getCurrentLogger().info("    " + AnsiColor.RED_BOLD + "FAIL" + AnsiColor.RESET + "    " + message);
            org.testng.Reporter.getCurrentTestResult().setStatus(ITestResult.SUCCESS_PERCENTAGE_FAILURE);
        }
        
        return logWithoutLogger(status, message, throwable, media);
    }

    public static ExtentTest logWithoutLogger(Status status, String message, Throwable throwable, Media media) {
        ExtentTest currentReport = getCurrentNode();
        if (currentReport != null) {
            currentReport.log(status, message, throwable, media);
        }
        return currentReport;
    }

    private static Logger getCurrentLogger() {
        ITestResult currentTestResult = org.testng.Reporter.getCurrentTestResult();
        Logger logger;
        if (currentTestResult != null) {
            logger = LoggerFactory.getLogger(currentTestResult.getInstance().getClass());
        } else {
            logger = log;
        }
        return logger;
    }
}
