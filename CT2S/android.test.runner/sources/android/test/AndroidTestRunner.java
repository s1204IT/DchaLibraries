package android.test;

import android.app.Instrumentation;
import android.content.Context;
import android.os.PerformanceCollector;
import com.google.android.collect.Lists;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.runner.BaseTestRunner;

public class AndroidTestRunner extends BaseTestRunner {
    private Context mContext;
    private Instrumentation mInstrumentation;
    private PerformanceCollector.PerformanceResultsWriter mPerfWriter;
    private List<junit.framework.TestCase> mTestCases;
    private String mTestClassName;
    private TestResult mTestResult;
    private boolean mSkipExecution = false;
    private List<TestListener> mTestListeners = Lists.newArrayList();

    public void setTestClassName(String testClassName, String testMethodName) {
        Class<? extends Test> clsLoadTestClass = loadTestClass(testClassName);
        if (shouldRunSingleTestMethod(testMethodName, clsLoadTestClass)) {
            junit.framework.TestCase testCase = buildSingleTestMethod(clsLoadTestClass, testMethodName);
            this.mTestCases = Lists.newArrayList(new junit.framework.TestCase[]{testCase});
            this.mTestClassName = clsLoadTestClass.getSimpleName();
            return;
        }
        setTest(getTest(clsLoadTestClass), clsLoadTestClass);
    }

    public void setTest(Test test) {
        setTest(test, test.getClass());
    }

    private void setTest(Test test, Class<? extends Test> testClass) {
        this.mTestCases = TestCaseUtil.getTests(test, true);
        if (TestSuite.class.isAssignableFrom(testClass)) {
            this.mTestClassName = TestCaseUtil.getTestName(test);
        } else {
            this.mTestClassName = testClass.getSimpleName();
        }
    }

    public void clearTestListeners() {
        this.mTestListeners.clear();
    }

    public void addTestListener(TestListener testListener) {
        if (testListener != null) {
            this.mTestListeners.add(testListener);
        }
    }

    private Class<? extends Test> loadTestClass(String testClassName) {
        try {
            return this.mContext.getClassLoader().loadClass(testClassName);
        } catch (ClassNotFoundException e) {
            runFailed("Could not find test class. Class: " + testClassName);
            return null;
        }
    }

    private junit.framework.TestCase buildSingleTestMethod(Class testClass, String testMethodName) {
        try {
            Constructor c = testClass.getConstructor(new Class[0]);
            return newSingleTestMethod(testClass, testMethodName, c, new Object[0]);
        } catch (NoSuchMethodException e) {
            try {
                Constructor c2 = testClass.getConstructor(String.class);
                return newSingleTestMethod(testClass, testMethodName, c2, testMethodName);
            } catch (NoSuchMethodException e2) {
                return null;
            }
        }
    }

    private junit.framework.TestCase newSingleTestMethod(Class testClass, String testMethodName, Constructor constructor, Object... args) {
        try {
            junit.framework.TestCase testCase = (junit.framework.TestCase) constructor.newInstance(args);
            testCase.setName(testMethodName);
            return testCase;
        } catch (IllegalAccessException e) {
            runFailed("Could not access test class. Class: " + testClass.getName());
            return null;
        } catch (IllegalArgumentException e2) {
            runFailed("Illegal argument passed to constructor. Class: " + testClass.getName());
            return null;
        } catch (InstantiationException e3) {
            runFailed("Could not instantiate test class. Class: " + testClass.getName());
            return null;
        } catch (InvocationTargetException e4) {
            runFailed("Constructor thew an exception. Class: " + testClass.getName());
            return null;
        }
    }

    private boolean shouldRunSingleTestMethod(String testMethodName, Class<? extends Test> testClass) {
        return testMethodName != null && junit.framework.TestCase.class.isAssignableFrom(testClass);
    }

    private Test getTest(Class clazz) {
        if (TestSuiteProvider.class.isAssignableFrom(clazz)) {
            try {
                TestSuiteProvider testSuiteProvider = (TestSuiteProvider) clazz.getConstructor(new Class[0]).newInstance(new Object[0]);
                return testSuiteProvider.getTestSuite();
            } catch (IllegalAccessException e) {
                runFailed("Illegal access of test suite provider. Class: " + clazz.getName());
            } catch (InstantiationException e2) {
                runFailed("Could not instantiate test suite provider. Class: " + clazz.getName());
            } catch (NoSuchMethodException e3) {
                runFailed("No such method on test suite provider. Class: " + clazz.getName());
            } catch (InvocationTargetException e4) {
                runFailed("Invocation exception test suite provider. Class: " + clazz.getName());
            }
        }
        return getTest(clazz.getName());
    }

    protected TestResult createTestResult() {
        return this.mSkipExecution ? new NoExecTestResult() : new TestResult();
    }

    void setSkipExecution(boolean skip) {
        this.mSkipExecution = skip;
    }

    public List<junit.framework.TestCase> getTestCases() {
        return this.mTestCases;
    }

    public String getTestClassName() {
        return this.mTestClassName;
    }

    public TestResult getTestResult() {
        return this.mTestResult;
    }

    public void runTest() {
        runTest(createTestResult());
    }

    public void runTest(TestResult testResult) {
        this.mTestResult = testResult;
        for (TestListener testListener : this.mTestListeners) {
            this.mTestResult.addListener(testListener);
        }
        Context testContext = this.mInstrumentation == null ? this.mContext : this.mInstrumentation.getContext();
        for (junit.framework.TestCase testCase : this.mTestCases) {
            setContextIfAndroidTestCase(testCase, this.mContext, testContext);
            setInstrumentationIfInstrumentationTestCase(testCase, this.mInstrumentation);
            setPerformanceWriterIfPerformanceCollectorTestCase(testCase, this.mPerfWriter);
            testCase.run(this.mTestResult);
        }
    }

    private void setContextIfAndroidTestCase(Test test, Context context, Context testContext) {
        if (AndroidTestCase.class.isAssignableFrom(test.getClass())) {
            ((AndroidTestCase) test).setContext(context);
            ((AndroidTestCase) test).setTestContext(testContext);
        }
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    private void setInstrumentationIfInstrumentationTestCase(Test test, Instrumentation instrumentation) {
        if (InstrumentationTestCase.class.isAssignableFrom(test.getClass())) {
            ((InstrumentationTestCase) test).injectInstrumentation(instrumentation);
        }
    }

    private void setPerformanceWriterIfPerformanceCollectorTestCase(Test test, PerformanceCollector.PerformanceResultsWriter writer) {
        if (PerformanceCollectorTestCase.class.isAssignableFrom(test.getClass())) {
            ((PerformanceCollectorTestCase) test).setPerformanceResultsWriter(writer);
        }
    }

    public void setInstrumentation(Instrumentation instrumentation) {
        this.mInstrumentation = instrumentation;
    }

    @Deprecated
    public void setInstrumentaiton(Instrumentation instrumentation) {
        setInstrumentation(instrumentation);
    }

    public void setPerformanceResultsWriter(PerformanceCollector.PerformanceResultsWriter writer) {
        this.mPerfWriter = writer;
    }

    @Override
    protected Class loadSuiteClass(String suiteClassName) throws ClassNotFoundException {
        return this.mContext.getClassLoader().loadClass(suiteClassName);
    }

    @Override
    public void testStarted(String testName) {
    }

    @Override
    public void testEnded(String testName) {
    }

    @Override
    public void testFailed(int status, Test test, Throwable t) {
    }

    @Override
    protected void runFailed(String message) {
        throw new RuntimeException(message);
    }
}
