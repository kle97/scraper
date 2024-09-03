package io.playground.common;

import io.playground.scraper.util.SoftAssertJ;

public class BaseTest {

    private final String toString = System.nanoTime() + "@" + getClass().getName();

    @Override
    public String toString() {
        return toString;
    }

    protected SoftAssertJ softly() {
        return SoftAssertJ.getInstance();
    }
}
