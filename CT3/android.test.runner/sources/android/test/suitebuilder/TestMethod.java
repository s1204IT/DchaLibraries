package android.test.suitebuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import junit.framework.TestCase;

public class TestMethod {
    private final Class<? extends TestCase> enclosingClass;
    private final String enclosingClassname;
    private final String testMethodName;

    public TestMethod(Method method, Class<? extends TestCase> enclosingClass) {
        this(method.getName(), enclosingClass);
    }

    public TestMethod(String methodName, Class<? extends TestCase> enclosingClass) {
        this.enclosingClass = enclosingClass;
        this.enclosingClassname = enclosingClass.getName();
        this.testMethodName = methodName;
    }

    public TestMethod(TestCase testCase) {
        this(testCase.getName(), (Class<? extends TestCase>) testCase.getClass());
    }

    public String getName() {
        return this.testMethodName;
    }

    public String getEnclosingClassname() {
        return this.enclosingClassname;
    }

    public <T extends Annotation> T getAnnotation(Class<T> cls) {
        try {
            return (T) getEnclosingClass().getMethod(getName(), new Class[0]).getAnnotation(cls);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public Class<? extends TestCase> getEnclosingClass() {
        return this.enclosingClass;
    }

    public TestCase createTest() throws IllegalAccessException, InstantiationException, InvocationTargetException {
        return instantiateTest(this.enclosingClass, this.testMethodName);
    }

    private TestCase instantiateTest(Class testCaseClass, String testName) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Constructor<?>[] constructors = testCaseClass.getConstructors();
        if (constructors.length == 0) {
            return instantiateTest(testCaseClass.getSuperclass(), testName);
        }
        for (Constructor<?> constructor : constructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (noargsConstructor(parameterTypes)) {
                TestCase test = (TestCase) constructor.newInstance(new Object[0]);
                test.setName(testName);
                return test;
            }
            if (singleStringConstructor(parameterTypes)) {
                return (TestCase) constructor.newInstance(testName);
            }
        }
        throw new RuntimeException("Unable to locate a constructor for " + testCaseClass.getName());
    }

    private boolean singleStringConstructor(Class[] params) {
        if (params.length == 1) {
            return params[0].equals(String.class);
        }
        return false;
    }

    private boolean noargsConstructor(Class[] params) {
        return params.length == 0;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TestMethod that = (TestMethod) o;
        if (this.enclosingClassname == null ? that.enclosingClassname == null : this.enclosingClassname.equals(that.enclosingClassname)) {
            return this.testMethodName == null ? that.testMethodName == null : this.testMethodName.equals(that.testMethodName);
        }
        return false;
    }

    public int hashCode() {
        int result = this.enclosingClassname != null ? this.enclosingClassname.hashCode() : 0;
        return (result * 31) + (this.testMethodName != null ? this.testMethodName.hashCode() : 0);
    }

    public String toString() {
        return this.enclosingClassname + "." + this.testMethodName;
    }
}
