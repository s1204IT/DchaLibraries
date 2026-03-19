package org.hamcrest.core;

import org.hamcrest.Matcher;

public class StringContains extends SubstringMatcher {
    public StringContains(boolean z, String str) {
        super("containing", z, str);
    }

    @Override
    protected boolean evalSubstringOf(String str) {
        return converted(str).contains(converted(this.substring));
    }

    public static Matcher<String> containsString(String str) {
        return new StringContains(false, str);
    }

    public static Matcher<String> containsStringIgnoringCase(String str) {
        return new StringContains(true, str);
    }
}
