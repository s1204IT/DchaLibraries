package junit.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public abstract class TestCase extends Assert implements Test {
    private String fName;

    public TestCase() {
        this.fName = null;
    }

    public TestCase(String str) {
        this.fName = str;
    }

    @Override
    public int countTestCases() {
        return 1;
    }

    protected TestResult createResult() {
        return new TestResult();
    }

    public TestResult run() {
        TestResult testResultCreateResult = createResult();
        run(testResultCreateResult);
        return testResultCreateResult;
    }

    @Override
    public void run(TestResult testResult) {
        testResult.run(this);
    }

    public void runBare() throws Throwable {
        setUp();
        try {
            runTest();
            try {
                tearDown();
                th = null;
            } catch (Throwable th) {
                th = th;
            }
        } catch (Throwable th2) {
            th = th2;
            try {
                tearDown();
            } catch (Throwable th3) {
            }
        }
        if (th != null) {
            throw th;
        }
    }

    protected void runTest() throws Throwable {
        assertNotNull("TestCase.fName cannot be null", this.fName);
        Method method = null;
        try {
            method = getClass().getMethod(this.fName, (Class[]) null);
        } catch (NoSuchMethodException e) {
            fail("Method \"" + this.fName + "\" not found");
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            fail("Method \"" + this.fName + "\" should be public");
        }
        try {
            method.invoke(this, new Object[0]);
        } catch (IllegalAccessException e2) {
            e2.fillInStackTrace();
            throw e2;
        } catch (InvocationTargetException e3) {
            e3.fillInStackTrace();
            throw e3.getTargetException();
        }
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public String toString() {
        return getName() + "(" + getClass().getName() + ")";
    }

    public String getName() {
        return this.fName;
    }

    public void setName(String str) {
        this.fName = str;
    }
}
