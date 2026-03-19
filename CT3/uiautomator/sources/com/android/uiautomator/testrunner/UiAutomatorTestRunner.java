package com.android.uiautomator.testrunner;

import android.app.IInstrumentationWatcher;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Debug;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.test.RepetitiveTest;
import android.util.Log;
import com.android.uiautomator.core.ShellUiAutomatorBridge;
import com.android.uiautomator.core.Tracer;
import com.android.uiautomator.core.UiAutomationShellWrapper;
import com.android.uiautomator.core.UiDevice;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.Thread;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.runner.BaseTestRunner;
import junit.textui.ResultPrinter;

public class UiAutomatorTestRunner {
    private static final int EXIT_EXCEPTION = -1;
    private static final int EXIT_OK = 0;
    private static final String HANDLER_THREAD_NAME = "UiAutomatorTestRunner-UiAutomatorHandlerThread";
    private static final String LOGTAG = UiAutomatorTestRunner.class.getSimpleName();
    private boolean mDebug;
    private HandlerThread mHandlerThread;
    private boolean mMonkey;
    private UiDevice mUiDevice;
    private Bundle mParams = null;
    private List<String> mTestClasses = null;
    private final FakeInstrumentationWatcher mWatcher = new FakeInstrumentationWatcher(this, null);
    private final IAutomationSupport mAutomationSupport = new IAutomationSupport() {
        @Override
        public void sendStatus(int resultCode, Bundle status) {
            UiAutomatorTestRunner.this.mWatcher.instrumentationStatus(null, resultCode, status);
        }
    };
    private final List<TestListener> mTestListeners = new ArrayList();

    private interface ResultReporter extends TestListener {
        void print(TestResult testResult, long j, Bundle bundle);

        void printUnexpectedError(Throwable th);
    }

    public void run(List<String> testClasses, Bundle params, boolean debug, boolean monkey) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.e(UiAutomatorTestRunner.LOGTAG, "uncaught exception", ex);
                Bundle results = new Bundle();
                results.putString("shortMsg", ex.getClass().getName());
                results.putString("longMsg", ex.getMessage());
                UiAutomatorTestRunner.this.mWatcher.instrumentationFinished(null, UiAutomatorTestRunner.EXIT_OK, results);
                System.exit(UiAutomatorTestRunner.EXIT_EXCEPTION);
            }
        });
        this.mTestClasses = testClasses;
        this.mParams = params;
        this.mDebug = debug;
        this.mMonkey = monkey;
        start();
        Log.i(LOGTAG, "calling System exit");
        System.exit(EXIT_OK);
    }

    protected void start() {
        ResultReporter resultPrinter;
        TestCaseCollector collector = getTestCaseCollector(getClass().getClassLoader());
        try {
            collector.addTestClasses(this.mTestClasses);
            if (this.mDebug) {
                Debug.waitForDebugger();
            }
            this.mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
            this.mHandlerThread.setDaemon(true);
            this.mHandlerThread.start();
            UiAutomationShellWrapper automationWrapper = new UiAutomationShellWrapper();
            automationWrapper.connect();
            long startTime = SystemClock.uptimeMillis();
            TestResult testRunResult = new TestResult();
            String outputFormat = this.mParams.getString("outputFormat");
            List<TestCase> testCases = collector.getTestCases();
            Bundle testRunOutput = new Bundle();
            if ("simple".equals(outputFormat)) {
                resultPrinter = new SimpleResultPrinter(System.out, true);
            } else {
                resultPrinter = new WatcherResultPrinter(testCases.size());
            }
            try {
                try {
                    automationWrapper.setRunAsMonkey(this.mMonkey);
                    this.mUiDevice = UiDevice.getInstance();
                    this.mUiDevice.initialize(new ShellUiAutomatorBridge(automationWrapper.getUiAutomation()));
                    String traceType = this.mParams.getString("traceOutputMode");
                    if (traceType != null) {
                        Tracer.Mode mode = (Tracer.Mode) Tracer.Mode.valueOf(Tracer.Mode.class, traceType);
                        if (mode == Tracer.Mode.FILE || mode == Tracer.Mode.ALL) {
                            String filename = this.mParams.getString("traceLogFilename");
                            if (filename == null) {
                                throw new RuntimeException("Name of log file not specified. Please specify it using traceLogFilename parameter");
                            }
                            Tracer.getInstance().setOutputFilename(filename);
                        }
                        Tracer.getInstance().setOutputMode(mode);
                    }
                    testRunResult.addListener(resultPrinter);
                    for (TestListener listener : this.mTestListeners) {
                        testRunResult.addListener(listener);
                    }
                    for (TestCase testCase : testCases) {
                        prepareTestCase(testCase);
                        testCase.run(testRunResult);
                    }
                    long runTime = SystemClock.uptimeMillis() - startTime;
                    resultPrinter.print(testRunResult, runTime, testRunOutput);
                    automationWrapper.disconnect();
                    automationWrapper.setRunAsMonkey(false);
                    boolean quit_result = this.mHandlerThread.quitSafely();
                    Log.i(LOGTAG, "all case run finished going to quit, HandlerThread quit result : " + quit_result);
                    try {
                        this.mHandlerThread.join();
                    } catch (InterruptedException e) {
                    }
                } catch (Throwable t) {
                    resultPrinter.printUnexpectedError(t);
                    testRunOutput.putString("shortMsg", t.getMessage());
                    long runTime2 = SystemClock.uptimeMillis() - startTime;
                    resultPrinter.print(testRunResult, runTime2, testRunOutput);
                    automationWrapper.disconnect();
                    automationWrapper.setRunAsMonkey(false);
                    boolean quit_result2 = this.mHandlerThread.quitSafely();
                    Log.i(LOGTAG, "all case run finished going to quit, HandlerThread quit result : " + quit_result2);
                    try {
                        this.mHandlerThread.join();
                    } catch (InterruptedException e2) {
                    }
                }
            } catch (Throwable th) {
                long runTime3 = SystemClock.uptimeMillis() - startTime;
                resultPrinter.print(testRunResult, runTime3, testRunOutput);
                automationWrapper.disconnect();
                automationWrapper.setRunAsMonkey(false);
                boolean quit_result3 = this.mHandlerThread.quitSafely();
                Log.i(LOGTAG, "all case run finished going to quit, HandlerThread quit result : " + quit_result3);
                try {
                    this.mHandlerThread.join();
                } catch (InterruptedException e3) {
                }
                throw th;
            }
        } catch (ClassNotFoundException e4) {
            throw new RuntimeException(e4.getMessage(), e4);
        }
    }

    private class FakeInstrumentationWatcher implements IInstrumentationWatcher {
        private final boolean mRawMode;

        FakeInstrumentationWatcher(UiAutomatorTestRunner this$0, FakeInstrumentationWatcher fakeInstrumentationWatcher) {
            this();
        }

        private FakeInstrumentationWatcher() {
            this.mRawMode = true;
        }

        public IBinder asBinder() {
            throw new UnsupportedOperationException("I'm just a fake!");
        }

        public void instrumentationStatus(ComponentName name, int resultCode, Bundle results) {
            synchronized (this) {
                if (results != null) {
                    for (String key : results.keySet()) {
                        System.out.println("INSTRUMENTATION_STATUS: " + key + "=" + results.get(key));
                    }
                    System.out.println("INSTRUMENTATION_STATUS_CODE: " + resultCode);
                    notifyAll();
                } else {
                    System.out.println("INSTRUMENTATION_STATUS_CODE: " + resultCode);
                    notifyAll();
                }
            }
        }

        public void instrumentationFinished(ComponentName name, int resultCode, Bundle results) {
            synchronized (this) {
                if (results != null) {
                    for (String key : results.keySet()) {
                        System.out.println("INSTRUMENTATION_RESULT: " + key + "=" + results.get(key));
                    }
                    System.out.println("INSTRUMENTATION_CODE: " + resultCode);
                    notifyAll();
                } else {
                    System.out.println("INSTRUMENTATION_CODE: " + resultCode);
                    notifyAll();
                }
            }
        }
    }

    private class WatcherResultPrinter implements ResultReporter {
        private static final String REPORT_KEY_NAME_CLASS = "class";
        private static final String REPORT_KEY_NAME_TEST = "test";
        private static final String REPORT_KEY_NUM_CURRENT = "current";
        private static final String REPORT_KEY_NUM_ITERATIONS = "numiterations";
        private static final String REPORT_KEY_NUM_TOTAL = "numtests";
        private static final String REPORT_KEY_STACK = "stack";
        private static final String REPORT_VALUE_ID = "UiAutomatorTestRunner";
        private static final int REPORT_VALUE_RESULT_ERROR = -1;
        private static final int REPORT_VALUE_RESULT_FAILURE = -2;
        private static final int REPORT_VALUE_RESULT_START = 1;
        private final SimpleResultPrinter mPrinter;
        private final ByteArrayOutputStream mStream;
        Bundle mTestResult;
        private final PrintStream mWriter;
        int mTestNum = UiAutomatorTestRunner.EXIT_OK;
        int mTestResultCode = UiAutomatorTestRunner.EXIT_OK;
        String mTestClass = null;
        private final Bundle mResultTemplate = new Bundle();

        public WatcherResultPrinter(int numTests) {
            this.mResultTemplate.putString("id", REPORT_VALUE_ID);
            this.mResultTemplate.putInt(REPORT_KEY_NUM_TOTAL, numTests);
            this.mStream = new ByteArrayOutputStream();
            this.mWriter = new PrintStream(this.mStream);
            this.mPrinter = UiAutomatorTestRunner.this.new SimpleResultPrinter(this.mWriter, false);
        }

        public void startTest(Test test) {
            String testClass = test.getClass().getName();
            String testName = ((TestCase) test).getName();
            this.mTestResult = new Bundle(this.mResultTemplate);
            this.mTestResult.putString(REPORT_KEY_NAME_CLASS, testClass);
            this.mTestResult.putString(REPORT_KEY_NAME_TEST, testName);
            Bundle bundle = this.mTestResult;
            int i = this.mTestNum + REPORT_VALUE_RESULT_START;
            this.mTestNum = i;
            bundle.putInt(REPORT_KEY_NUM_CURRENT, i);
            if (testClass != null && !testClass.equals(this.mTestClass)) {
                this.mTestResult.putString("stream", String.format("\n%s:", testClass));
                this.mTestClass = testClass;
            } else {
                this.mTestResult.putString("stream", "");
            }
            try {
                Method testMethod = test.getClass().getMethod(testName, new Class[UiAutomatorTestRunner.EXIT_OK]);
                if (testMethod.isAnnotationPresent(RepetitiveTest.class)) {
                    int numIterations = testMethod.getAnnotation(RepetitiveTest.class).numIterations();
                    this.mTestResult.putInt(REPORT_KEY_NUM_ITERATIONS, numIterations);
                }
            } catch (NoSuchMethodException e) {
            }
            UiAutomatorTestRunner.this.mAutomationSupport.sendStatus(REPORT_VALUE_RESULT_START, this.mTestResult);
            this.mTestResultCode = UiAutomatorTestRunner.EXIT_OK;
            this.mPrinter.startTest(test);
        }

        public void addError(Test test, Throwable t) {
            this.mTestResult.putString(REPORT_KEY_STACK, BaseTestRunner.getFilteredTrace(t));
            this.mTestResultCode = REPORT_VALUE_RESULT_ERROR;
            this.mTestResult.putString("stream", String.format("\nError in %s:\n%s", ((TestCase) test).getName(), BaseTestRunner.getFilteredTrace(t)));
            this.mPrinter.addError(test, t);
        }

        public void addFailure(Test test, AssertionFailedError t) {
            this.mTestResult.putString(REPORT_KEY_STACK, BaseTestRunner.getFilteredTrace(t));
            this.mTestResultCode = REPORT_VALUE_RESULT_FAILURE;
            this.mTestResult.putString("stream", String.format("\nFailure in %s:\n%s", ((TestCase) test).getName(), BaseTestRunner.getFilteredTrace(t)));
            this.mPrinter.addFailure(test, t);
        }

        public void endTest(Test test) {
            if (this.mTestResultCode == 0) {
                this.mTestResult.putString("stream", ".");
            }
            UiAutomatorTestRunner.this.mAutomationSupport.sendStatus(this.mTestResultCode, this.mTestResult);
            this.mPrinter.endTest(test);
        }

        @Override
        public void print(TestResult result, long runTime, Bundle testOutput) {
            this.mPrinter.print(result, runTime, testOutput);
            testOutput.putString("stream", String.format("\nTest results for %s=%s", getClass().getSimpleName(), this.mStream.toString()));
            this.mWriter.close();
            UiAutomatorTestRunner.this.mAutomationSupport.sendStatus(REPORT_VALUE_RESULT_ERROR, testOutput);
        }

        @Override
        public void printUnexpectedError(Throwable t) {
            this.mWriter.println(String.format("Test run aborted due to unexpected exception: %s", t.getMessage()));
            t.printStackTrace(this.mWriter);
        }
    }

    private class SimpleResultPrinter extends ResultPrinter implements ResultReporter {
        private final boolean mFullOutput;

        public SimpleResultPrinter(PrintStream writer, boolean fullOutput) {
            super(writer);
            this.mFullOutput = fullOutput;
        }

        @Override
        public void print(TestResult result, long runTime, Bundle testOutput) {
            printHeader(runTime);
            if (this.mFullOutput) {
                printErrors(result);
                printFailures(result);
            }
            printFooter(result);
        }

        @Override
        public void printUnexpectedError(Throwable t) {
            if (!this.mFullOutput) {
                return;
            }
            getWriter().printf("Test run aborted due to unexpected exeption: %s", t.getMessage());
            t.printStackTrace(getWriter());
        }
    }

    protected TestCaseCollector getTestCaseCollector(ClassLoader classLoader) {
        return new TestCaseCollector(classLoader, getTestCaseFilter());
    }

    public UiAutomatorTestCaseFilter getTestCaseFilter() {
        return new UiAutomatorTestCaseFilter();
    }

    protected void addTestListener(TestListener listener) {
        if (this.mTestListeners.contains(listener)) {
            return;
        }
        this.mTestListeners.add(listener);
    }

    protected void removeTestListener(TestListener listener) {
        this.mTestListeners.remove(listener);
    }

    protected void prepareTestCase(TestCase testCase) {
        ((UiAutomatorTestCase) testCase).setAutomationSupport(this.mAutomationSupport);
        ((UiAutomatorTestCase) testCase).setUiDevice(this.mUiDevice);
        ((UiAutomatorTestCase) testCase).setParams(this.mParams);
    }
}
