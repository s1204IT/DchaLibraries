package com.android.uiautomator.testrunner;

import com.android.uiautomator.testrunner.TestCaseCollector;
import java.lang.reflect.Method;

public class UiAutomatorTestCaseFilter implements TestCaseCollector.TestCaseFilter {
    @Override
    public boolean accept(Method method) {
        return method.getParameterTypes().length == 0 && method.getName().startsWith("test") && method.getReturnType().getSimpleName().equals("void");
    }

    @Override
    public boolean accept(Class<?> clazz) {
        return UiAutomatorTestCase.class.isAssignableFrom(clazz);
    }
}
