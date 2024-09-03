package io.playground.common;

import lombok.extern.slf4j.Slf4j;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

@Slf4j
public class RetryAnalyzer implements IRetryAnalyzer {
    
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 2;
    
    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            log.info("Retrying test {} for the {} time!", result.getMethod().getMethodName(), ordinal(retryCount));
            return true;
        }
        return false;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public static int getCurrentRetryCount() {
        ITestResult currentTestResult = org.testng.Reporter.getCurrentTestResult();
        if (currentTestResult != null) {
            if (currentTestResult.getMethod().getRetryAnalyzer(currentTestResult) instanceof RetryAnalyzer retryAnalyzer) {
                return retryAnalyzer.getRetryCount();
            }
        }
        return 0;
    }

    private static String ordinal(int i) {
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        return switch (i % 100) {
            case 11, 12, 13 -> i + "th";
            default -> i + suffixes[i % 10];
        };
    }
}
