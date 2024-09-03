package io.playground.common;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.testng.xml.XmlSuite;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
@Builder
@Setter(AccessLevel.NONE)
public class TestRunnerOption {
    
    @Builder.Default
    private XmlSuite.ParallelMode parallelMode = XmlSuite.ParallelMode.NONE;

    private boolean dataProviderParallel;

    @Builder.Default
    private List<String> testFilter = new ArrayList<>();

    @Builder.Default
    private int threadCount = Thread.activeCount() > 2 ? Thread.activeCount() - 2 : Thread.activeCount();
    
    public XmlSuite.ParallelMode getParallelMode() {
        String systemProperty = System.getProperty("tests.parallel");
        if (systemProperty != null) {
            return XmlSuite.ParallelMode.getValidParallel(systemProperty);
        } else {
            return parallelMode;
        }
    }

    public boolean isDataProviderParallel() {
        String systemProperty = System.getProperty("tests.dataParallel");
        if (systemProperty != null) {
            return Boolean.parseBoolean(systemProperty);
        } else {
            return dataProviderParallel;
        }
    }
    
    public List<String> getTestFilter() {
        String systemProperty = System.getProperty("tests");
        if (systemProperty != null) {
            String[] systemFilterArray = System.getProperty("tests").split(",");
            return List.of(systemFilterArray);
        } else {
            return List.copyOf(testFilter);
        }
    }
}
