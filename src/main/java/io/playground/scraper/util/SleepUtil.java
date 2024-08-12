package io.playground.scraper.util;

public class SleepUtil {
    
    private SleepUtil() {}
    
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ignored) {
        }
    }
}
