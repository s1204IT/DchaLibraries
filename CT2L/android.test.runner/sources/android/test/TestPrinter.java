package android.test;

import android.test.TestRunner;
import android.util.Log;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestListener;

public class TestPrinter implements TestRunner.Listener, TestListener {
    private Set<String> mFailedTests = new HashSet();
    private boolean mOnlyFailures;
    private String mTag;

    public TestPrinter(String tag, boolean onlyFailures) {
        this.mTag = tag;
        this.mOnlyFailures = onlyFailures;
    }

    @Override
    public void started(String className) {
        if (!this.mOnlyFailures) {
            Log.i(this.mTag, "started: " + className);
        }
    }

    @Override
    public void finished(String className) {
        if (!this.mOnlyFailures) {
            Log.i(this.mTag, "finished: " + className);
        }
    }

    @Override
    public void performance(String className, long itemTimeNS, int iterations, List<TestRunner.IntermediateTime> intermediates) {
        Log.i(this.mTag, "perf: " + className + " = " + itemTimeNS + "ns/op (done " + iterations + " times)");
        if (intermediates != null && intermediates.size() > 0) {
            int N = intermediates.size();
            for (int i = 0; i < N; i++) {
                TestRunner.IntermediateTime time = intermediates.get(i);
                Log.i(this.mTag, "  intermediate: " + time.name + " = " + time.timeInNS + "ns");
            }
        }
    }

    @Override
    public void passed(String className) {
        if (!this.mOnlyFailures) {
            Log.i(this.mTag, "passed: " + className);
        }
    }

    @Override
    public void failed(String className, Throwable exception) {
        Log.i(this.mTag, "failed: " + className);
        Log.i(this.mTag, "----- begin exception -----");
        Log.i(this.mTag, "", exception);
        Log.i(this.mTag, "----- end exception -----");
    }

    private void failed(Test test, Throwable t) {
        this.mFailedTests.add(test.toString());
        failed(test.toString(), t);
    }

    public void addError(Test test, Throwable t) {
        failed(test, t);
    }

    public void addFailure(Test test, junit.framework.AssertionFailedError t) {
        failed(test, (Throwable) t);
    }

    public void endTest(Test test) {
        finished(test.toString());
        if (!this.mFailedTests.contains(test.toString())) {
            passed(test.toString());
        }
        this.mFailedTests.remove(test.toString());
    }

    public void startTest(Test test) {
        started(test.toString());
    }
}
