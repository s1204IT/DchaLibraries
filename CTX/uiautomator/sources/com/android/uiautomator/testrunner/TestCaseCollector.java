package com.android.uiautomator.testrunner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;

public class TestCaseCollector {
    private ClassLoader mClassLoader;
    private TestCaseFilter mFilter;
    private List<TestCase> mTestCases = new ArrayList();

    public interface TestCaseFilter {
        boolean accept(Class<?> cls);

        boolean accept(Method method);
    }

    public TestCaseCollector(ClassLoader classLoader, TestCaseFilter testCaseFilter) {
        this.mClassLoader = classLoader;
        this.mFilter = testCaseFilter;
    }

    public void addTestClasses(List<String> list) throws ClassNotFoundException {
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            addTestClass(it.next());
        }
    }

    public void addTestClass(String str) throws ClassNotFoundException {
        String strSubstring;
        int iIndexOf = str.indexOf(35);
        if (iIndexOf != -1) {
            strSubstring = str.substring(iIndexOf + 1);
            str = str.substring(0, iIndexOf);
        } else {
            strSubstring = null;
        }
        addTestClass(str, strSubstring);
    }

    public void addTestClass(String str, String str2) throws ClassNotFoundException {
        Class<?> clsLoadClass = this.mClassLoader.loadClass(str);
        if (str2 != null) {
            addSingleTestMethod(clsLoadClass, str2);
            return;
        }
        for (Method method : clsLoadClass.getMethods()) {
            if (this.mFilter.accept(method)) {
                addSingleTestMethod(clsLoadClass, method.getName());
            }
        }
    }

    public List<TestCase> getTestCases() {
        return Collections.unmodifiableList(this.mTestCases);
    }

    protected void addSingleTestMethod(Class<?> cls, String str) {
        if (!this.mFilter.accept(cls)) {
            throw new RuntimeException("Test class must be derived from UiAutomatorTestCase");
        }
        try {
            TestCase testCase = (TestCase) cls.newInstance();
            testCase.setName(str);
            this.mTestCases.add(testCase);
        } catch (IllegalAccessException e) {
            this.mTestCases.add(error(cls, "IllegalAccessException: could not instantiate test class. Class: " + cls.getName()));
        } catch (InstantiationException e2) {
            this.mTestCases.add(error(cls, "InstantiationException: could not instantiate test class. Class: " + cls.getName()));
        }
    }

    private UiAutomatorTestCase error(Class<?> cls, final String str) {
        UiAutomatorTestCase uiAutomatorTestCase = new UiAutomatorTestCase() {
            @Override
            protected void runTest() {
                fail(str);
            }
        };
        uiAutomatorTestCase.setName(cls.getName());
        return uiAutomatorTestCase;
    }
}
