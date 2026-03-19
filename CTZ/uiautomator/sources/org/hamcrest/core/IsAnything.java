package org.hamcrest.core;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class IsAnything<T> extends BaseMatcher<T> {
    private final String message;

    public IsAnything() {
        this("ANYTHING");
    }

    public IsAnything(String str) {
        this.message = str;
    }

    @Override
    public boolean matches(Object obj) {
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(this.message);
    }

    public static Matcher<Object> anything() {
        return new IsAnything();
    }

    public static Matcher<Object> anything(String str) {
        return new IsAnything(str);
    }
}
