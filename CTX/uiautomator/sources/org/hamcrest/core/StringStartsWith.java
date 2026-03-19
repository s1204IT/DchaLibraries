package org.hamcrest.core;

import org.hamcrest.Matcher;

public class StringStartsWith extends SubstringMatcher {
    public StringStartsWith(boolean z, String str) {
        super("starting with", z, str);
    }

    @Override
    protected boolean evalSubstringOf(String str) {
        return converted(str).startsWith(converted(this.substring));
    }

    public static Matcher<String> startsWith(String str) {
        return new StringStartsWith(false, str);
    }

    public static Matcher<String> startsWithIgnoringCase(String str) {
        return new StringStartsWith(true, str);
    }
}
