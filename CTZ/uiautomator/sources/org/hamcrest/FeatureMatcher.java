package org.hamcrest;

import org.hamcrest.internal.ReflectiveTypeFinder;

public abstract class FeatureMatcher<T, U> extends TypeSafeDiagnosingMatcher<T> {
    private static final ReflectiveTypeFinder TYPE_FINDER = new ReflectiveTypeFinder("featureValueOf", 1, 0);
    private final String featureDescription;
    private final String featureName;
    private final Matcher<? super U> subMatcher;

    protected abstract U featureValueOf(T t);

    public FeatureMatcher(Matcher<? super U> matcher, String str, String str2) {
        super(TYPE_FINDER);
        this.subMatcher = matcher;
        this.featureDescription = str;
        this.featureName = str2;
    }

    @Override
    protected boolean matchesSafely(T t, Description description) {
        U uFeatureValueOf = featureValueOf(t);
        if (!this.subMatcher.matches(uFeatureValueOf)) {
            description.appendText(this.featureName).appendText(" ");
            this.subMatcher.describeMismatch(uFeatureValueOf, description);
            return false;
        }
        return true;
    }

    @Override
    public final void describeTo(Description description) {
        description.appendText(this.featureDescription).appendText(" ").appendDescriptionOf(this.subMatcher);
    }
}
