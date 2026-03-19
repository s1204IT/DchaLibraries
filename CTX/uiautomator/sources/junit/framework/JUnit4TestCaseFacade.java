package junit.framework;

import org.junit.runner.Describable;
import org.junit.runner.Description;

public class JUnit4TestCaseFacade implements Test, Describable {
    private final Description fDescription;

    JUnit4TestCaseFacade(Description description) {
        this.fDescription = description;
    }

    public String toString() {
        return getDescription().toString();
    }

    @Override
    public int countTestCases() {
        return 1;
    }

    @Override
    public void run(TestResult testResult) {
        throw new RuntimeException("This test stub created only for informational purposes.");
    }

    @Override
    public Description getDescription() {
        return this.fDescription;
    }
}
