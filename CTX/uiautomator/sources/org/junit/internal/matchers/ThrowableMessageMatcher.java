package org.junit.internal.matchers;

import java.lang.Throwable;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;

public class ThrowableMessageMatcher<T extends Throwable> extends org.hamcrest.TypeSafeMatcher<T> {
    private final Matcher<String> matcher;

    public ThrowableMessageMatcher(Matcher<String> matcher) {
        this.matcher = matcher;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("exception with message ");
        description.appendDescriptionOf(this.matcher);
    }

    @Override
    protected boolean matchesSafely(T t) {
        return this.matcher.matches(t.getMessage());
    }

    @Override
    protected void describeMismatchSafely(T t, Description description) {
        description.appendText("message ");
        this.matcher.describeMismatch(t.getMessage(), description);
    }

    @Factory
    public static <T extends Throwable> Matcher<T> hasMessage(Matcher<String> matcher) {
        return new ThrowableMessageMatcher(matcher);
    }
}
