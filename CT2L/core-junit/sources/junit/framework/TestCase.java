package junit.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public abstract class TestCase extends Assert implements Test {
    private String fName;

    public TestCase() {
        this.fName = null;
    }

    public TestCase(String name) {
        this.fName = name;
    }

    @Override
    public int countTestCases() {
        return 1;
    }

    protected TestResult createResult() {
        return new TestResult();
    }

    public TestResult run() {
        TestResult result = createResult();
        run(result);
        return result;
    }

    @Override
    public void run(TestResult result) {
        result.run(this);
    }

    public void runBare() throws Throwable {
        Throwable exception = null;
        setUp();
        try {
            runTest();
            try {
                tearDown();
            } catch (Throwable tearingDown) {
                if (0 == 0) {
                    exception = tearingDown;
                }
            }
        } catch (Throwable th) {
            try {
                tearDown();
            } catch (Throwable th2) {
                if (0 == 0) {
                }
            }
            throw th;
        }
        if (exception != null) {
            throw exception;
        }
    }

    protected void runTest() throws Throwable {
        assertNotNull("TestCase.fName cannot be null", this.fName);
        Method runMethod = null;
        try {
            runMethod = getClass().getMethod(this.fName, (Class[]) null);
        } catch (NoSuchMethodException e) {
            fail("Method \"" + this.fName + "\" not found");
        }
        if (!Modifier.isPublic(runMethod.getModifiers())) {
            fail("Method \"" + this.fName + "\" should be public");
        }
        try {
            runMethod.invoke(this, new Object[0]);
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

    public void setName(String name) {
        this.fName = name;
    }
}
