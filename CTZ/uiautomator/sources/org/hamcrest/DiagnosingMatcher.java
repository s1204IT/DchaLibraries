package org.hamcrest;

public abstract class DiagnosingMatcher<T> extends BaseMatcher<T> {
    protected abstract boolean matches(Object obj, Description description);

    @Override
    public final boolean matches(Object obj) {
        return matches(obj, Description.NONE);
    }

    @Override
    public final void describeMismatch(Object obj, Description description) {
        matches(obj, description);
    }
}
