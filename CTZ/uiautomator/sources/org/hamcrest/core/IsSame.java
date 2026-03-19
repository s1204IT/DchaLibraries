package org.hamcrest.core;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class IsSame<T> extends BaseMatcher<T> {
    private final T object;

    public IsSame(T t) {
        this.object = t;
    }

    @Override
    public boolean matches(Object obj) {
        return obj == this.object;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("sameInstance(").appendValue(this.object).appendText(")");
    }

    public static <T> Matcher<T> sameInstance(T t) {
        return new IsSame(t);
    }

    public static <T> Matcher<T> theInstance(T t) {
        return new IsSame(t);
    }
}
