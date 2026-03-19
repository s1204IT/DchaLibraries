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
import java.util.Iterator;
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
    private static final String HANDLER_THREAD_NAME = "UiAutomatorHandlerThread";
    private static final String LOGTAG = UiAutomatorTestRunner.class.getSimpleName();
    private boolean mDebug;
    private HandlerThread mHandlerThread;
    private boolean mMonkey;
    private UiDevice mUiDevice;
    private Bundle mParams = null;
    private List<String> mTestClasses = null;
    private final FakeInstrumentationWatcher mWatcher = new FakeInstrumentationWatcher();
    private final IAutomationSupport mAutomationSupport = new IAutomationSupport() {
        @Override
        public void sendStatus(int i, Bundle bundle) {
            UiAutomatorTestRunner.this.mWatcher.instrumentationStatus(null, i, bundle);
        }
    };
    private final List<TestListener> mTestListeners = new ArrayList();

    private interface ResultReporter extends TestListener {
        void print(TestResult testResult, long j, Bundle bundle);

        void printUnexpectedError(Throwable th);
    }

    public void run(List<String> list, Bundle bundle, boolean z, boolean z2) {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable th) {
                Log.e(UiAutomatorTestRunner.LOGTAG, "uncaught exception", th);
                Bundle bundle2 = new Bundle();
                bundle2.putString("shortMsg", th.getClass().getName());
                bundle2.putString("longMsg", th.getMessage());
                UiAutomatorTestRunner.this.mWatcher.instrumentationFinished(null, UiAutomatorTestRunner.EXIT_OK, bundle2);
                System.exit(UiAutomatorTestRunner.EXIT_EXCEPTION);
            }
        });
        this.mTestClasses = list;
        this.mParams = bundle;
        this.mDebug = z;
        this.mMonkey = z2;
        start();
        System.exit(EXIT_OK);
    }

    protected void start() {
        ResultReporter watcherResultPrinter;
        TestCaseCollector testCaseCollector = getTestCaseCollector(getClass().getClassLoader());
        try {
            testCaseCollector.addTestClasses(this.mTestClasses);
            if (this.mDebug) {
                Debug.waitForDebugger();
            }
            this.mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
            this.mHandlerThread.setDaemon(true);
            this.mHandlerThread.start();
            UiAutomationShellWrapper uiAutomationShellWrapper = new UiAutomationShellWrapper();
            uiAutomationShellWrapper.connect();
            long jUptimeMillis = SystemClock.uptimeMillis();
            TestResult testResult = new TestResult();
            String string = this.mParams.getString("outputFormat");
            List<TestCase> testCases = testCaseCollector.getTestCases();
            Bundle bundle = new Bundle();
            if ("simple".equals(string)) {
                watcherResultPrinter = new SimpleResultPrinter(System.out, true);
            } else {
                watcherResultPrinter = new WatcherResultPrinter(testCases.size());
            }
            try {
                try {
                    uiAutomationShellWrapper.setRunAsMonkey(this.mMonkey);
                    this.mUiDevice = UiDevice.getInstance();
                    this.mUiDevice.initialize(new ShellUiAutomatorBridge(uiAutomationShellWrapper.getUiAutomation()));
                    String string2 = this.mParams.getString("traceOutputMode");
                    if (string2 != null) {
                        Tracer.Mode mode = (Tracer.Mode) Tracer.Mode.valueOf(Tracer.Mode.class, string2);
                        if (mode == Tracer.Mode.FILE || mode == Tracer.Mode.ALL) {
                            String string3 = this.mParams.getString("traceLogFilename");
                            if (string3 == null) {
                                throw new RuntimeException("Name of log file not specified. Please specify it using traceLogFilename parameter");
                            }
                            Tracer.getInstance().setOutputFilename(string3);
                        }
                        Tracer.getInstance().setOutputMode(mode);
                    }
                    testResult.addListener(watcherResultPrinter);
                    Iterator<TestListener> it = this.mTestListeners.iterator();
                    while (it.hasNext()) {
                        testResult.addListener(it.next());
                    }
                    for (TestCase testCase : testCases) {
                        prepareTestCase(testCase);
                        testCase.run(testResult);
                    }
                } catch (Throwable th) {
                    watcherResultPrinter.printUnexpectedError(th);
                    bundle.putString("shortMsg", th.getMessage());
                }
            } finally {
                watcherResultPrinter.print(testResult, SystemClock.uptimeMillis() - jUptimeMillis, bundle);
                uiAutomationShellWrapper.disconnect();
                uiAutomationShellWrapper.setRunAsMonkey(false);
                this.mHandlerThread.quit();
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private class FakeInstrumentationWatcher implements IInstrumentationWatcher {
        private final boolean mRawMode;

        private FakeInstrumentationWatcher() {
            this.mRawMode = true;
        }

        public IBinder asBinder() {
            throw new UnsupportedOperationException("I'm just a fake!");
        }

        public void instrumentationStatus(ComponentName componentName, int i, Bundle bundle) {
            synchronized (this) {
                if (bundle != null) {
                    try {
                        for (String str : bundle.keySet()) {
                            System.out.println("INSTRUMENTATION_STATUS: " + str + "=" + bundle.get(str));
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                System.out.println("INSTRUMENTATION_STATUS_CODE: " + i);
                notifyAll();
            }
        }

        public void instrumentationFinished(ComponentName componentName, int i, Bundle bundle) {
            synchronized (this) {
                if (bundle != null) {
                    try {
                        for (String str : bundle.keySet()) {
                            System.out.println("INSTRUMENTATION_RESULT: " + str + "=" + bundle.get(str));
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                System.out.println("INSTRUMENTATION_CODE: " + i);
                notifyAll();
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

        public WatcherResultPrinter(int i) {
            this.mResultTemplate.putString("id", REPORT_VALUE_ID);
            this.mResultTemplate.putInt(REPORT_KEY_NUM_TOTAL, i);
            this.mStream = new ByteArrayOutputStream();
            this.mWriter = new PrintStream(this.mStream);
            this.mPrinter = UiAutomatorTestRunner.this.new SimpleResultPrinter(this.mWriter, false);
        }

        @Override
        public void startTest(Test test) {
            String name = test.getClass().getName();
            String name2 = ((TestCase) test).getName();
            this.mTestResult = new Bundle(this.mResultTemplate);
            this.mTestResult.putString(REPORT_KEY_NAME_CLASS, name);
            this.mTestResult.putString(REPORT_KEY_NAME_TEST, name2);
            Bundle bundle = this.mTestResult;
            int i = this.mTestNum + 1;
            this.mTestNum = i;
            bundle.putInt(REPORT_KEY_NUM_CURRENT, i);
            if (name != null && !name.equals(this.mTestClass)) {
                this.mTestResult.putString("stream", String.format("\n%s:", name));
                this.mTestClass = name;
            } else {
                this.mTestResult.putString("stream", "");
            }
            try {
                Method method = test.getClass().getMethod(name2, new Class[UiAutomatorTestRunner.EXIT_OK]);
                if (method.isAnnotationPresent(RepetitiveTest.class)) {
                    this.mTestResult.putInt(REPORT_KEY_NUM_ITERATIONS, method.getAnnotation(RepetitiveTest.class).numIterations());
                }
            } catch (NoSuchMethodException e) {
            }
            UiAutomatorTestRunner.this.mAutomationSupport.sendStatus(1, this.mTestResult);
            this.mTestResultCode = UiAutomatorTestRunner.EXIT_OK;
            this.mPrinter.startTest(test);
        }

        @Override
        public void addError(Test test, Throwable th) {
            this.mTestResult.putString(REPORT_KEY_STACK, BaseTestRunner.getFilteredTrace(th));
            this.mTestResultCode = REPORT_VALUE_RESULT_ERROR;
            this.mTestResult.putString("stream", String.format("\nError in %s:\n%s", ((TestCase) test).getName(), BaseTestRunner.getFilteredTrace(th)));
            this.mPrinter.addError(test, th);
        }

        @Override
        public void addFailure(Test test, AssertionFailedError assertionFailedError) {
            this.mTestResult.putString(REPORT_KEY_STACK, BaseTestRunner.getFilteredTrace(assertionFailedError));
            this.mTestResultCode = REPORT_VALUE_RESULT_FAILURE;
            this.mTestResult.putString("stream", String.format("\nFailure in %s:\n%s", ((TestCase) test).getName(), BaseTestRunner.getFilteredTrace(assertionFailedError)));
            this.mPrinter.addFailure(test, assertionFailedError);
        }

        @Override
        public void endTest(Test test) {
            if (this.mTestResultCode == 0) {
                this.mTestResult.putString("stream", ".");
            }
            UiAutomatorTestRunner.this.mAutomationSupport.sendStatus(this.mTestResultCode, this.mTestResult);
            this.mPrinter.endTest(test);
        }

        @Override
        public void print(TestResult testResult, long j, Bundle bundle) {
            this.mPrinter.print(testResult, j, bundle);
            bundle.putString("stream", String.format("\nTest results for %s=%s", getClass().getSimpleName(), this.mStream.toString()));
            this.mWriter.close();
            UiAutomatorTestRunner.this.mAutomationSupport.sendStatus(REPORT_VALUE_RESULT_ERROR, bundle);
        }

        @Override
        public void printUnexpectedError(Throwable th) {
            this.mWriter.println(String.format("Test run aborted due to unexpected exception: %s", th.getMessage()));
            th.printStackTrace(this.mWriter);
        }
    }

    private class SimpleResultPrinter extends ResultPrinter implements ResultReporter {
        private final boolean mFullOutput;

        public SimpleResultPrinter(PrintStream printStream, boolean z) {
            super(printStream);
            this.mFullOutput = z;
        }

        @Override
        public void print(TestResult testResult, long j, Bundle bundle) {
            printHeader(j);
            if (this.mFullOutput) {
                printErrors(testResult);
                printFailures(testResult);
            }
            printFooter(testResult);
        }

        @Override
        public void printUnexpectedError(Throwable th) {
            if (this.mFullOutput) {
                getWriter().printf("Test run aborted due to unexpected exeption: %s", th.getMessage());
                th.printStackTrace(getWriter());
            }
        }
    }

    protected TestCaseCollector getTestCaseCollector(ClassLoader classLoader) {
        return new TestCaseCollector(classLoader, getTestCaseFilter());
    }

    public UiAutomatorTestCaseFilter getTestCaseFilter() {
        return new UiAutomatorTestCaseFilter();
    }

    protected void addTestListener(TestListener testListener) {
        if (!this.mTestListeners.contains(testListener)) {
            this.mTestListeners.add(testListener);
        }
    }

    protected void removeTestListener(TestListener testListener) {
        this.mTestListeners.remove(testListener);
    }

    protected void prepareTestCase(TestCase testCase) {
        UiAutomatorTestCase uiAutomatorTestCase = (UiAutomatorTestCase) testCase;
        uiAutomatorTestCase.setAutomationSupport(this.mAutomationSupport);
        uiAutomatorTestCase.setUiDevice(this.mUiDevice);
        uiAutomatorTestCase.setParams(this.mParams);
    }
}
