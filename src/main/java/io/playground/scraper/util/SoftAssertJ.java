package io.playground.scraper.util;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assert;
import org.assertj.core.api.AssertionErrorCollector;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.description.Description;
import org.assertj.core.description.LazyTextDescription;
import org.assertj.core.description.TextDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.synchronizedList;
import static java.util.Collections.unmodifiableList;

@Slf4j
public class SoftAssertJ extends SoftAssertions {

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final ThreadLocal<AssertionDescription> messages = new ThreadLocal<>();
    private static final ThreadLocal<Logger> logger = new ThreadLocal<>();
    private static final ThreadLocal<Object> savedActual = new ThreadLocal<>();
    private static SoftAssertJ softAssertJ;

    public static SoftAssertJ getInstance() {
        if (softAssertJ == null) {
            softAssertJ = new SoftAssertJ();
            softAssertJ.setDelegate(new AssertionErrorCollectorDelegate());
        }
        return softAssertJ;
    }

    private SoftAssertJ() {
    }

    public SoftAssertJ as(String description, Object... args) {
        messages.set(AssertionDescription.builder().formatter(description).args(args).build());
        return this;
    }

    public SoftAssertJ as(Description description) {
        messages.set(AssertionDescription.builder().description(description).build());
        return this;
    }

    public SoftAssertJ as(Supplier<String> description) {
        messages.set(AssertionDescription.builder().supplier(description).build());
        return this;
    }

    @Override
    public <SELF extends Assert<? extends SELF, ? extends ACTUAL>, ACTUAL> SELF proxy(Class<SELF> assertClass,
                                                                                      Class<ACTUAL> actualClass,
                                                                                      ACTUAL actual) {
        return proxy(assertClass, actualClass, actual, 3);
    }

    public <SELF extends Assert<? extends SELF, ? extends ACTUAL>, ACTUAL> SELF proxy(Class<SELF> assertClass,
                                                                                      Class<ACTUAL> actualClass,
                                                                                      ACTUAL actual, int walkStep) {
        SELF proxy = super.proxy(assertClass, actualClass, actual);
        savedActual.set(actual);

        try {
            STACK_WALKER.walk(s -> s.skip(walkStep).findFirst())
                        .ifPresent(stackFrame -> logger.set(LoggerFactory.getLogger(stackFrame.getClassName())));
        } catch (Exception ignore) {
        }

        if (messages.get() != null) {
            AssertionDescription description = messages.get();
            if (description.getFormatter() != null) {
                proxy.as(description.getFormatter(), description.getArgs());
            } else if (description.getDescription() != null) {
                proxy.as(description.getDescription());
            } else if (description.getSupplier() != null) {
                proxy.as(description.getSupplier());
            }
        }
        return proxy;
    }

    @Override
    public void succeeded() {
        String message = "";
        if (messages.get() != null) {
            AssertionDescription description = messages.get();
            if (description.getFormatter() != null) {
                message = new TextDescription(description.getFormatter(), description.getArgs()).value();
            } else if (description.getDescription() != null) {
                message = description.getDescription().value();
            } else if (description.getSupplier() != null) {
                message = new LazyTextDescription(description.getSupplier()).value();
            }
            messages.remove();
        }

        if (savedActual.get() != null) {
            String toString = savedActual.get().toString();
            logPass("[" + message + "] " + toString);
            savedActual.remove();
        }
    }

    @Override
    public void onAssertionErrorCollected(AssertionError e) {
        messages.remove();
        savedActual.remove();
        logFail(e.getMessage());
    }

    private void logPass(String message) {
        message = message.trim().replaceAll("\\s+", " ");
        Reporter.pass(message);
    }

    private void logFail(String message) {
        message = message.trim().replaceAll("\\s+", " ").replace("Expecting", "expected");
        Reporter.fail(message);
    }

    private Logger getLogger() {
        if (logger.get() != null) {
            return logger.get();
        } else {
            return log;
        }
    }

    @Builder
    @Getter
    static class AssertionDescription {
        private String formatter;
        private Object[] args;
        private Description description;
        private Supplier<String> supplier;
    }

    static class AssertionErrorCollectorDelegate implements AssertionErrorCollector {

        private volatile boolean wasSuccess = true;
        private List<AssertionError> collectedAssertionErrors = synchronizedList(new ArrayList<>());

        @Override
        public void collectAssertionError(AssertionError error) {
            collectedAssertionErrors.add(error);
            wasSuccess = false;
        }

        @Override
        public List<AssertionError> assertionErrorsCollected() {
            List<AssertionError> errors = unmodifiableList(collectedAssertionErrors);
            if (!errors.isEmpty()) {
                collectedAssertionErrors = synchronizedList(new ArrayList<>());
            }
            return errors;
        }

        @Override
        public void succeeded() {
            wasSuccess = true;
        }

        @Override
        public boolean wasSuccess() {
            return wasSuccess;
        }
    }
}
