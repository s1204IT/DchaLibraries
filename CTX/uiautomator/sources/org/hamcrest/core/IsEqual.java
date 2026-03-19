package org.hamcrest.core;

import java.lang.reflect.Array;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class IsEqual<T> extends BaseMatcher<T> {
    private final Object expectedValue;

    public IsEqual(T t) {
        this.expectedValue = t;
    }

    @Override
    public boolean matches(Object obj) {
        return areEqual(obj, this.expectedValue);
    }

    @Override
    public void describeTo(Description description) {
        description.appendValue(this.expectedValue);
    }

    private static boolean areEqual(Object obj, Object obj2) {
        if (obj == null) {
            return obj2 == null;
        }
        if (obj2 == null || !isArray(obj)) {
            return obj.equals(obj2);
        }
        return isArray(obj2) && areArraysEqual(obj, obj2);
    }

    private static boolean areArraysEqual(Object obj, Object obj2) {
        return areArrayLengthsEqual(obj, obj2) && areArrayElementsEqual(obj, obj2);
    }

    private static boolean areArrayLengthsEqual(Object obj, Object obj2) {
        return Array.getLength(obj) == Array.getLength(obj2);
    }

    private static boolean areArrayElementsEqual(Object obj, Object obj2) {
        for (int i = 0; i < Array.getLength(obj); i++) {
            if (!areEqual(Array.get(obj, i), Array.get(obj2, i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isArray(Object obj) {
        return obj.getClass().isArray();
    }

    public static <T> Matcher<T> equalTo(T t) {
        return new IsEqual(t);
    }

    public static Matcher<Object> equalToObject(Object obj) {
        return new IsEqual(obj);
    }
}
