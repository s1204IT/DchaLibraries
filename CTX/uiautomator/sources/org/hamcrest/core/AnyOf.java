package org.hamcrest.core;

import java.util.Arrays;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class AnyOf<T> extends ShortcutCombination<T> {
    @Override
    public void describeTo(Description description, String str) {
        super.describeTo(description, str);
    }

    public AnyOf(Iterable<Matcher<? super T>> iterable) {
        super(iterable);
    }

    @Override
    public boolean matches(Object obj) {
        return matches(obj, true);
    }

    @Override
    public void describeTo(Description description) {
        describeTo(description, "or");
    }

    public static <T> AnyOf<T> anyOf(Iterable<Matcher<? super T>> iterable) {
        return new AnyOf<>(iterable);
    }

    @SafeVarargs
    public static <T> AnyOf<T> anyOf(Matcher<? super T>... matcherArr) {
        return anyOf(Arrays.asList(matcherArr));
    }
}
