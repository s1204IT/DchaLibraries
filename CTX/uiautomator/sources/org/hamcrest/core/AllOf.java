package org.hamcrest.core;

import java.util.Arrays;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;

public class AllOf<T> extends DiagnosingMatcher<T> {
    private final Iterable<Matcher<? super T>> matchers;

    public AllOf(Iterable<Matcher<? super T>> iterable) {
        this.matchers = iterable;
    }

    @Override
    public boolean matches(Object obj, Description description) {
        for (Matcher<? super T> matcher : this.matchers) {
            if (!matcher.matches(obj)) {
                description.appendDescriptionOf(matcher).appendText(" ");
                matcher.describeMismatch(obj, description);
                return false;
            }
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendList("(", " and ", ")", this.matchers);
    }

    public static <T> Matcher<T> allOf(Iterable<Matcher<? super T>> iterable) {
        return new AllOf(iterable);
    }

    @SafeVarargs
    public static <T> Matcher<T> allOf(Matcher<? super T>... matcherArr) {
        return allOf(Arrays.asList(matcherArr));
    }
}
