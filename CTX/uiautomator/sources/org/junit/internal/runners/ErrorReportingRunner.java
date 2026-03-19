package org.junit.internal.runners;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

public class ErrorReportingRunner extends Runner {
    private final List<Throwable> causes;
    private final Class<?> testClass;

    public ErrorReportingRunner(Class<?> cls, Throwable th) {
        if (cls == null) {
            throw new NullPointerException("Test class cannot be null");
        }
        this.testClass = cls;
        this.causes = getCauses(th);
    }

    @Override
    public Description getDescription() {
        Description descriptionCreateSuiteDescription = Description.createSuiteDescription(this.testClass);
        Iterator<Throwable> it = this.causes.iterator();
        while (it.hasNext()) {
            descriptionCreateSuiteDescription.addChild(describeCause(it.next()));
        }
        return descriptionCreateSuiteDescription;
    }

    @Override
    public void run(RunNotifier runNotifier) {
        Iterator<Throwable> it = this.causes.iterator();
        while (it.hasNext()) {
            runCause(it.next(), runNotifier);
        }
    }

    private List<Throwable> getCauses(Throwable th) {
        if (th instanceof InvocationTargetException) {
            return getCauses(th.getCause());
        }
        return th instanceof org.junit.runners.model.InitializationError ? th.getCauses() : th instanceof InitializationError ? th.getCauses() : Arrays.asList(th);
    }

    private Description describeCause(Throwable th) {
        return Description.createTestDescription(this.testClass, "initializationError");
    }

    private void runCause(Throwable th, RunNotifier runNotifier) {
        Description descriptionDescribeCause = describeCause(th);
        runNotifier.fireTestStarted(descriptionDescribeCause);
        runNotifier.fireTestFailure(new Failure(descriptionDescribeCause, th));
        runNotifier.fireTestFinished(descriptionDescribeCause);
    }
}
