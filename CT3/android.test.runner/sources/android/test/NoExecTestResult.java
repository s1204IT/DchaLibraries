package android.test;

import junit.framework.TestResult;

@Deprecated
class NoExecTestResult extends TestResult {
    NoExecTestResult() {
    }

    protected void run(junit.framework.TestCase test) {
        startTest(test);
        endTest(test);
    }
}
