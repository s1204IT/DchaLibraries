package org.hamcrest.core;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class Is<T> extends BaseMatcher<T> {
    private final Matcher<T> matcher;

    public Is(Matcher<T> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(Object obj) {
        return this.matcher.matches(obj);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("is ").appendDescriptionOf(this.matcher);
    }

    @Override
    public void describeMismatch(Object obj, Description description) {
        this.matcher.describeMismatch(obj, description);
    }

    public static <T> Matcher<T> is(Matcher<T> matcher) {
        return new Is(matcher);
    }

    public static <T> Matcher<T> is(T t) {
        return is(IsEqual.equalTo(t));
    }

    public static <T> Matcher<T> isA(Class<T> cls) {
        return is(IsInstanceOf.instanceOf(cls));
    }
}
