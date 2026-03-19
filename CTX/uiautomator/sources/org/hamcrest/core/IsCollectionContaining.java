package org.hamcrest.core;

import java.util.ArrayList;
import java.util.Iterator;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class IsCollectionContaining<T> extends TypeSafeDiagnosingMatcher<Iterable<? super T>> {
    private final Matcher<? super T> elementMatcher;

    public IsCollectionContaining(Matcher<? super T> matcher) {
        this.elementMatcher = matcher;
    }

    @Override
    protected boolean matchesSafely(Iterable<? super T> iterable, Description description) {
        if (isEmpty(iterable)) {
            description.appendText("was empty");
            return false;
        }
        Iterator<? super T> it = iterable.iterator();
        while (it.hasNext()) {
            if (this.elementMatcher.matches(it.next())) {
                return true;
            }
        }
        description.appendText("mismatches were: [");
        boolean z = false;
        for (T t : iterable) {
            if (z) {
                description.appendText(", ");
            }
            this.elementMatcher.describeMismatch(t, description);
            z = true;
        }
        description.appendText("]");
        return false;
    }

    private boolean isEmpty(Iterable<? super T> iterable) {
        return !iterable.iterator().hasNext();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a collection containing ").appendDescriptionOf(this.elementMatcher);
    }

    public static <T> Matcher<Iterable<? super T>> hasItem(Matcher<? super T> matcher) {
        return new IsCollectionContaining(matcher);
    }

    public static <T> Matcher<Iterable<? super T>> hasItem(T t) {
        return new IsCollectionContaining(IsEqual.equalTo(t));
    }

    @SafeVarargs
    public static <T> Matcher<Iterable<T>> hasItems(Matcher<? super T>... matcherArr) {
        ArrayList arrayList = new ArrayList(matcherArr.length);
        for (Matcher<? super T> matcher : matcherArr) {
            arrayList.add(new IsCollectionContaining(matcher));
        }
        return AllOf.allOf(arrayList);
    }

    @SafeVarargs
    public static <T> Matcher<Iterable<T>> hasItems(T... tArr) {
        ArrayList arrayList = new ArrayList(tArr.length);
        for (T t : tArr) {
            arrayList.add(hasItem(t));
        }
        return AllOf.allOf(arrayList);
    }
}
