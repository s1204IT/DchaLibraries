package junit.runner;

import java.lang.reflect.Modifier;
import junit.framework.Test;
import junit.framework.TestSuite;

public class LoadingTestCollector extends ClassPathTestCollector {
    TestCaseClassLoader fLoader = new TestCaseClassLoader();

    @Override
    protected boolean isTestClass(String classFileName) {
        Class testClass;
        try {
            if (!classFileName.endsWith(".class") || (testClass = classFromFile(classFileName)) == null) {
                return false;
            }
            return isTestClass(testClass);
        } catch (ClassNotFoundException e) {
        } catch (NoClassDefFoundError e2) {
        }
        return false;
    }

    Class classFromFile(String classFileName) throws ClassNotFoundException {
        String className = classNameFromFile(classFileName);
        if (!this.fLoader.isExcluded(className)) {
            return this.fLoader.loadClass(className, false);
        }
        return null;
    }

    boolean isTestClass(Class testClass) {
        if (hasSuiteMethod(testClass)) {
            return true;
        }
        return Test.class.isAssignableFrom(testClass) && Modifier.isPublic(testClass.getModifiers()) && hasPublicConstructor(testClass);
    }

    boolean hasSuiteMethod(Class testClass) {
        try {
            testClass.getMethod(BaseTestRunner.SUITE_METHODNAME, new Class[0]);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    boolean hasPublicConstructor(Class testClass) {
        try {
            TestSuite.getTestConstructor(testClass);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
