package org.hamcrest.core;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class Every<T> extends TypeSafeDiagnosingMatcher<Iterable<? extends T>> {
    private final Matcher<? super T> matcher;

    public Every(Matcher<? super T> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matchesSafely(Iterable<? extends T> iterable, Description description) {
        for (T t : iterable) {
            if (!this.matcher.matches(t)) {
                description.appendText("an item ");
                this.matcher.describeMismatch(t, description);
                return false;
            }
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("every item is ").appendDescriptionOf(this.matcher);
    }

    public static <U> Matcher<Iterable<? extends U>> everyItem(Matcher<U> matcher) {
        return new Every(matcher);
    }
}
