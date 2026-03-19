package org.hamcrest;

public abstract class CustomMatcher<T> extends BaseMatcher<T> {
    private final String fixedDescription;

    public CustomMatcher(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Description should be non null!");
        }
        this.fixedDescription = str;
    }

    @Override
    public final void describeTo(Description description) {
        description.appendText(this.fixedDescription);
    }
}
