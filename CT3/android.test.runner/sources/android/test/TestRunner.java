package android.test;

import android.content.Context;
import android.os.Debug;
import android.os.SystemClock;
import android.test.PerformanceTestCase;
import android.util.Log;
import com.google.android.collect.Lists;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

@Deprecated
public class TestRunner implements PerformanceTestCase.Intermediates {
    public static final int CLEARSCREEN = 0;
    public static final int PERFORMANCE = 1;
    public static final int PROFILING = 2;
    public static final int REGRESSION = 0;
    private static final String TAG = "TestHarness";
    private static Class mJUnitClass;
    private static Class mRunnableClass;
    private String mClassName;
    private Context mContext;
    private long mEndTime;
    private int mFailed;
    private int mInternalIterations;
    private int mPassed;
    private long mStartTime;
    private int mMode = 0;
    private List<Listener> mListeners = Lists.newArrayList();
    List<IntermediateTime> mIntermediates = null;

    public interface Listener {
        void failed(String str, Throwable th);

        void finished(String str);

        void passed(String str);

        void performance(String str, long j, int i, List<IntermediateTime> list);

        void started(String str);
    }

    static {
        try {
            mRunnableClass = Class.forName("java.lang.Runnable", false, null);
            mJUnitClass = Class.forName("junit.framework.TestCase", false, null);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("shouldn't happen", ex);
        }
    }

    public class JunitTestSuite extends TestSuite implements TestListener {
        boolean mError = false;

        public JunitTestSuite() {
        }

        public void run(TestResult result) {
            result.addListener(this);
            super.run(result);
            result.removeListener(this);
        }

        public void startTest(Test test) {
            TestRunner.this.started(test.toString());
        }

        public void endTest(Test test) {
            TestRunner.this.finished(test.toString());
            if (this.mError) {
                return;
            }
            TestRunner.this.passed(test.toString());
        }

        public void addError(Test test, Throwable t) {
            this.mError = true;
            TestRunner.this.failed(test.toString(), t);
        }

        public void addFailure(Test test, junit.framework.AssertionFailedError t) {
            this.mError = true;
            TestRunner.this.failed(test.toString(), t);
        }
    }

    public static class IntermediateTime {
        public String name;
        public long timeInNS;

        public IntermediateTime(String name, long timeInNS) {
            this.name = name;
            this.timeInNS = timeInNS;
        }
    }

    public TestRunner(Context context) {
        this.mContext = context;
    }

    public void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    public void startProfiling() {
        File file = new File("/tmp/trace");
        file.mkdir();
        String base = "/tmp/trace/" + this.mClassName + ".dmtrace";
        Debug.startMethodTracing(base, 8388608);
    }

    public void finishProfiling() {
        Debug.stopMethodTracing();
    }

    private void started(String className) {
        int count = this.mListeners.size();
        for (int i = 0; i < count; i++) {
            this.mListeners.get(i).started(className);
        }
    }

    private void finished(String className) {
        int count = this.mListeners.size();
        for (int i = 0; i < count; i++) {
            this.mListeners.get(i).finished(className);
        }
    }

    private void performance(String className, long itemTimeNS, int iterations, List<IntermediateTime> intermediates) {
        int count = this.mListeners.size();
        for (int i = 0; i < count; i++) {
            this.mListeners.get(i).performance(className, itemTimeNS, iterations, intermediates);
        }
    }

    public void passed(String className) {
        this.mPassed++;
        int count = this.mListeners.size();
        for (int i = 0; i < count; i++) {
            this.mListeners.get(i).passed(className);
        }
    }

    public void failed(String className, Throwable exception) {
        this.mFailed++;
        int count = this.mListeners.size();
        for (int i = 0; i < count; i++) {
            this.mListeners.get(i).failed(className, exception);
        }
    }

    public int passedCount() {
        return this.mPassed;
    }

    public int failedCount() {
        return this.mFailed;
    }

    public void run(String[] classes) {
        for (String cl : classes) {
            run(cl);
        }
    }

    public void setInternalIterations(int count) {
        this.mInternalIterations = count;
    }

    public void startTiming(boolean realTime) {
        if (realTime) {
            this.mStartTime = System.currentTimeMillis();
        } else {
            this.mStartTime = SystemClock.currentThreadTimeMillis();
        }
    }

    public void addIntermediate(String name) {
        addIntermediate(name, (System.currentTimeMillis() - this.mStartTime) * 1000000);
    }

    public void addIntermediate(String name, long timeInNS) {
        this.mIntermediates.add(new IntermediateTime(name, timeInNS));
    }

    public void finishTiming(boolean realTime) {
        if (realTime) {
            this.mEndTime = System.currentTimeMillis();
        } else {
            this.mEndTime = SystemClock.currentThreadTimeMillis();
        }
    }

    public void setPerformanceMode(int mode) {
        this.mMode = mode;
    }

    private void missingTest(String className, Throwable e) {
        started(className);
        finished(className);
        failed(className, e);
    }

    public void run(String className) {
        try {
            this.mClassName = className;
            Class<?> clsLoadClass = this.mContext.getClassLoader().loadClass(className);
            Method method = getChildrenMethod(clsLoadClass);
            if (method != null) {
                String[] children = getChildren(method);
                run(children);
                return;
            }
            if (mRunnableClass.isAssignableFrom(clsLoadClass)) {
                Runnable test = (Runnable) clsLoadClass.newInstance();
                TestCase testcase = null;
                if (test instanceof TestCase) {
                    testcase = (TestCase) test;
                }
                Throwable e = null;
                boolean didSetup = false;
                started(className);
                if (testcase != null) {
                    try {
                        testcase.setUp(this.mContext);
                        didSetup = true;
                        if (this.mMode != 1) {
                            runInPerformanceMode(test, className, false, className);
                        } else if (this.mMode == 2) {
                            startProfiling();
                            test.run();
                            finishProfiling();
                        } else {
                            test.run();
                        }
                    } catch (Throwable ex) {
                        e = ex;
                    }
                } else if (this.mMode != 1) {
                }
                if (testcase != null && didSetup) {
                    try {
                        testcase.tearDown();
                    } catch (Throwable ex2) {
                        e = ex2;
                    }
                }
                finished(className);
                if (e == null) {
                    passed(className);
                    return;
                } else {
                    failed(className, e);
                    return;
                }
            }
            if (mJUnitClass.isAssignableFrom(clsLoadClass)) {
                Throwable e2 = null;
                JunitTestSuite suite = new JunitTestSuite();
                Method[] methods = getAllTestMethods(clsLoadClass);
                for (Method m : methods) {
                    AndroidTestCase androidTestCase = (junit.framework.TestCase) clsLoadClass.newInstance();
                    androidTestCase.setName(m.getName());
                    if (androidTestCase instanceof AndroidTestCase) {
                        AndroidTestCase testcase2 = androidTestCase;
                        try {
                            testcase2.setContext(this.mContext);
                            testcase2.setTestContext(this.mContext);
                        } catch (Exception ex3) {
                            Log.i(TAG, ex3.toString());
                        }
                    }
                    suite.addTest(androidTestCase);
                }
                if (this.mMode == 1) {
                    int testCount = suite.testCount();
                    for (int j = 0; j < testCount; j++) {
                        Object test2 = suite.testAt(j);
                        started(test2.toString());
                        try {
                            runInPerformanceMode(test2, className, true, test2.toString());
                        } catch (Throwable ex4) {
                            e2 = ex4;
                        }
                        finished(test2.toString());
                        if (e2 == null) {
                            passed(test2.toString());
                        } else {
                            failed(test2.toString(), e2);
                        }
                    }
                    return;
                }
                if (this.mMode == 2) {
                    startProfiling();
                    junit.textui.TestRunner.run((Test) suite);
                    finishProfiling();
                    return;
                }
                junit.textui.TestRunner.run((Test) suite);
                return;
            }
            System.out.println("Test wasn't Runnable and didn't have a children method: " + className);
        } catch (ClassNotFoundException e3) {
            Log.e("ClassNotFoundException for " + className, e3.toString());
            if (isJunitTest(className)) {
                runSingleJunitTest(className);
            } else {
                missingTest(className, e3);
            }
        } catch (IllegalAccessException e4) {
            System.out.println("IllegalAccessException for " + className);
            missingTest(className, e4);
        } catch (InstantiationException e5) {
            System.out.println("InstantiationException for " + className);
            missingTest(className, e5);
        }
    }

    public void runInPerformanceMode(Object testCase, String className, boolean junitTest, String testNameInDb) throws Exception {
        long duration;
        boolean increaseIterations = true;
        int iterations = 1;
        this.mIntermediates = null;
        this.mInternalIterations = 1;
        Object perftest = this.mContext.getClassLoader().loadClass(className).newInstance();
        PerformanceTestCase perftestcase = null;
        if (perftest instanceof PerformanceTestCase) {
            perftestcase = (PerformanceTestCase) perftest;
            if (this.mMode == 0 && perftestcase.isPerformanceOnly()) {
                return;
            }
        }
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        if (perftestcase != null) {
            this.mIntermediates = new ArrayList();
            iterations = perftestcase.startPerformance(this);
            if (iterations > 0) {
                increaseIterations = false;
            } else {
                iterations = 1;
            }
        }
        Thread.sleep(1000L);
        while (true) {
            this.mEndTime = 0L;
            if (increaseIterations) {
                this.mStartTime = SystemClock.currentThreadTimeMillis();
            } else {
                this.mStartTime = 0L;
            }
            if (junitTest) {
                for (int i = 0; i < iterations; i++) {
                    junit.textui.TestRunner.run((Test) testCase);
                }
            } else {
                Runnable test = (Runnable) testCase;
                for (int i2 = 0; i2 < iterations; i2++) {
                    test.run();
                }
            }
            long endTime = this.mEndTime;
            if (endTime == 0) {
                endTime = SystemClock.currentThreadTimeMillis();
            }
            duration = endTime - this.mStartTime;
            if (!increaseIterations) {
                break;
            }
            if (duration <= 1) {
                iterations *= 1000;
            } else if (duration <= 10) {
                iterations *= 100;
            } else if (duration < 100) {
                iterations *= 10;
            } else if (duration >= 1000) {
                break;
            } else {
                iterations *= (int) ((1000 / duration) + 2);
            }
        }
        if (duration == 0) {
            return;
        }
        int iterations2 = iterations * this.mInternalIterations;
        performance(testNameInDb, (1000000 * duration) / ((long) iterations2), iterations2, this.mIntermediates);
    }

    public void runSingleJunitTest(String className) {
        int index = className.lastIndexOf(36);
        String testName = "";
        if (index >= 0) {
            className = className.substring(0, index);
            testName = className.substring(index + 1);
        }
        try {
            Class<?> clsLoadClass = this.mContext.getClassLoader().loadClass(className);
            if (!mJUnitClass.isAssignableFrom(clsLoadClass)) {
                return;
            }
            AndroidTestCase androidTestCase = (junit.framework.TestCase) clsLoadClass.newInstance();
            JunitTestSuite newSuite = new JunitTestSuite();
            androidTestCase.setName(testName);
            if (androidTestCase instanceof AndroidTestCase) {
                AndroidTestCase testcase = androidTestCase;
                try {
                    testcase.setContext(this.mContext);
                } catch (Exception ex) {
                    Log.w(TAG, "Exception encountered while trying to set the context.", ex);
                }
            }
            newSuite.addTest(androidTestCase);
            if (this.mMode == 1) {
                try {
                    started(androidTestCase.toString());
                    runInPerformanceMode(androidTestCase, className, true, androidTestCase.toString());
                    finished(androidTestCase.toString());
                    passed(androidTestCase.toString());
                    return;
                } catch (Throwable th) {
                    return;
                }
            }
            if (this.mMode == 2) {
                startProfiling();
                junit.textui.TestRunner.run((Test) newSuite);
                finishProfiling();
                return;
            }
            junit.textui.TestRunner.run((Test) newSuite);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "No test case to run", e);
        } catch (IllegalAccessException e2) {
            Log.e(TAG, "Illegal Access Exception", e2);
        } catch (InstantiationException e3) {
            Log.e(TAG, "Instantiation Exception", e3);
        }
    }

    public static Method getChildrenMethod(Class clazz) {
        try {
            return clazz.getMethod("children", (Class[]) null);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static Method getChildrenMethod(Context c, String className) {
        try {
            return getChildrenMethod(c.getClassLoader().loadClass(className));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static String[] getChildren(Context c, String className) {
        Method m = getChildrenMethod(c, className);
        String[] testChildren = getTestChildren(c, className);
        if ((testChildren == null) & (m == null)) {
            throw new RuntimeException("couldn't get children method for " + className);
        }
        if (m != null) {
            String[] children = getChildren(m);
            if (testChildren != null) {
                String[] allChildren = new String[testChildren.length + children.length];
                System.arraycopy(children, 0, allChildren, 0, children.length);
                System.arraycopy(testChildren, 0, allChildren, children.length, testChildren.length);
                return allChildren;
            }
            return children;
        }
        if (testChildren != null) {
            return testChildren;
        }
        return null;
    }

    public static String[] getChildren(Method m) {
        try {
            if (!Modifier.isStatic(m.getModifiers())) {
                throw new RuntimeException("children method is not static");
            }
            return (String[]) m.invoke(null, (Object[]) null);
        } catch (IllegalAccessException e) {
            return new String[0];
        } catch (InvocationTargetException e2) {
            return new String[0];
        }
    }

    public static String[] getTestChildren(Context c, String className) {
        try {
            Class<?> clsLoadClass = c.getClassLoader().loadClass(className);
            if (mJUnitClass.isAssignableFrom(clsLoadClass)) {
                return getTestChildren(clsLoadClass);
            }
            return null;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "No class found", e);
            return null;
        }
    }

    public static String[] getTestChildren(Class clazz) {
        Method[] methods = getAllTestMethods(clazz);
        String[] onScreenTestNames = new String[methods.length];
        int index = 0;
        for (Method m : methods) {
            onScreenTestNames[index] = clazz.getName() + "$" + m.getName();
            index++;
        }
        return onScreenTestNames;
    }

    public static Method[] getAllTestMethods(Class clazz) {
        Method[] allMethods = clazz.getDeclaredMethods();
        int numOfMethods = 0;
        for (Method method : allMethods) {
            boolean mTrue = isTestMethod(method);
            if (mTrue) {
                numOfMethods++;
            }
        }
        int index = 0;
        Method[] testMethods = new Method[numOfMethods];
        for (Method m : allMethods) {
            boolean mTrue2 = isTestMethod(m);
            if (mTrue2) {
                testMethods[index] = m;
                index++;
            }
        }
        return testMethods;
    }

    private static boolean isTestMethod(Method m) {
        return m.getName().startsWith(InstrumentationTestRunner.REPORT_KEY_NAME_TEST) && m.getReturnType() == Void.TYPE && m.getParameterTypes().length == 0;
    }

    public static int countJunitTests(Class clazz) {
        Method[] allTestMethods = getAllTestMethods(clazz);
        int numberofMethods = allTestMethods.length;
        return numberofMethods;
    }

    public static boolean isTestSuite(Context c, String className) {
        boolean childrenMethods = getChildrenMethod(c, className) != null;
        try {
            Class<?> clsLoadClass = c.getClassLoader().loadClass(className);
            if (mJUnitClass.isAssignableFrom(clsLoadClass)) {
                int numTests = countJunitTests(clsLoadClass);
                if (numTests > 0) {
                    return true;
                }
                return childrenMethods;
            }
            return childrenMethods;
        } catch (ClassNotFoundException e) {
            return childrenMethods;
        }
    }

    public boolean isJunitTest(String className) {
        int index = className.lastIndexOf(36);
        if (index >= 0) {
            className = className.substring(0, index);
        }
        return mJUnitClass.isAssignableFrom(this.mContext.getClassLoader().loadClass(className));
    }

    public static int countTests(Context c, String className) {
        try {
            Class<?> clsLoadClass = c.getClassLoader().loadClass(className);
            Method method = getChildrenMethod(clsLoadClass);
            if (method != null) {
                String[] children = getChildren(method);
                int rv = 0;
                for (String child : children) {
                    rv += countTests(c, child);
                }
                return rv;
            }
            if (mRunnableClass.isAssignableFrom(clsLoadClass)) {
                return 1;
            }
            if (mJUnitClass.isAssignableFrom(clsLoadClass)) {
                return countJunitTests(clsLoadClass);
            }
            return 0;
        } catch (ClassNotFoundException e) {
            return 1;
        }
    }

    public static String getTitle(String className) {
        int indexDot = className.lastIndexOf(46);
        int indexDollar = className.lastIndexOf(36);
        int index = indexDot > indexDollar ? indexDot : indexDollar;
        if (index >= 0) {
            return className.substring(index + 1);
        }
        return className;
    }
}
