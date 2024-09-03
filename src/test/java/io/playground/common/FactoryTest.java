package io.playground.common;

import org.testng.annotations.Factory;

public class FactoryTest {

    private static final ThreadLocal<Object[]> testClasses = new ThreadLocal<>();

    public static void setTestClasses(Object[] classes) {
        testClasses.set(classes);
    }

    @Factory
    public Object[] factory() {
        Object[] classes = testClasses.get();
        testClasses.remove();
        return classes;
    }
}
