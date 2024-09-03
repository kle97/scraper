package io.playground.common;

import io.playground.scraper.util.Reporter;
import lombok.extern.slf4j.Slf4j;
import org.testng.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.IDataProviderAnnotation;
import org.testng.xml.XmlSuite;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
public abstract class BaseTP extends BaseTest {
    @BeforeClass
    public void beforeBaseTPClass() {
        Reporter.startReport();
    }

    @AfterClass
    public void afterBaseTPClass() {
        Reporter.flush();
        try {
            softly().assertAll();
        } catch (Throwable ignored) {
            throw new RuntimeException("There are soft assertion failures!");
        }
    }

    protected void runTests(Object... tests) {
        runTests(TestRunnerOption.builder().build(), tests);
    }
    
    protected void runTests(TestRunnerOption option, Object... tests) {
        ITestResult testResult = org.testng.Reporter.getCurrentTestResult();
        
        TestNG testNG = new TestNG(false);
        FactoryTest.setTestClasses(tests);
        testNG.setTestClasses(new Class[] { FactoryTest.class });
        testNG.setVerbose(0);
        testNG.setParallel(option.getParallelMode());
        testNG.setGroupByInstances(!option.getParallelMode().equals(XmlSuite.ParallelMode.METHODS));
        testNG.addListener(new TestListener(testResult, option));
        testNG.shouldUseGlobalThreadPool(true);
        testNG.shareThreadPoolForDataProviders(true);
        testNG.setThreadCount(option.getThreadCount());
        testNG.run();

        org.testng.Reporter.setCurrentTestResult(testResult);
    }

    protected Object[][] streamTo2DArray(Stream<?> stream) {
        return stream.map(o -> new Object[] { o }).toArray(Object[][]::new);
    }

    protected Object[][] listTo2DArray(List<?> list) {
        return list.stream().map(o -> new Object[] { o }).toArray(Object[][]::new);
    }

    private record TestListener(ITestResult testResult, TestRunnerOption option) 
            implements ITestListener, IMethodInterceptor, IAnnotationTransformer, IConfigurationListener {
        @Override
        public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
            if (testResult != null && testResult.getStatus() != ITestResult.FAILURE) {
                testResult.setStatus(ITestResult.SUCCESS_PERCENTAGE_FAILURE);
            }

            if (testResult != null && result.getThrowable() != null) {
                onTestFailure(result);
            }
        }

        @Override
        public void onTestFailure(ITestResult result) {
            if (result.getThrowable() != null) {
                log.error(result.getThrowable().getMessage(), result.getThrowable());
//                testResult.setThrowable(new NoStackTraceException("There was an exception earlier!"));
                testResult.setStatus(ITestResult.FAILURE);
            }
        }

        @Override
        public void onConfigurationFailure(ITestResult result) {
            onTestFailure(result);
        }

        @Override
        public void transform(IDataProviderAnnotation annotation, Method method) {
            annotation.setParallel(option.isDataProviderParallel());
        }

        @Override
        public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
            if (!option.getTestFilter().isEmpty()) {
                List<IMethodInstance> filteredMethods = new ArrayList<>();
                for (IMethodInstance methodInstance : methods) {
                    if (option.getTestFilter().stream().anyMatch(pattern -> matches(pattern, methodInstance))) {
                        filteredMethods.add(methodInstance);
                    }
                }
                methods = filteredMethods;
                methods.sort(Comparator.comparing(m -> m.getInstance().toString()));
            }
            
            return methods;
        }

        private boolean matches(String pattern, IMethodInstance methodInstance) {
            String methodName = methodInstance.getMethod().getQualifiedName();
            String className = methodInstance.getMethod().getRealClass().getName();
            Pattern compiledPattern = Pattern.compile(pattern.replaceAll("\\*", ".*"));
            return compiledPattern.matcher(methodName).matches() || compiledPattern.matcher(className).matches();
        }
    }
}
