package org.hamcrest;

public abstract class CustomTypeSafeMatcher<T> extends TypeSafeMatcher<T> {
    private final String fixedDescription;

    public CustomTypeSafeMatcher(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Description must be non null!");
        }
        this.fixedDescription = str;
    }

    @Override
    public final void describeTo(Description description) {
        description.appendText(this.fixedDescription);
    }
}
