package org.hamcrest.core;

import org.hamcrest.Matcher;

public class StringEndsWith extends SubstringMatcher {
    public StringEndsWith(boolean z, String str) {
        super("ending with", z, str);
    }

    @Override
    protected boolean evalSubstringOf(String str) {
        return converted(str).endsWith(converted(this.substring));
    }

    public static Matcher<String> endsWith(String str) {
        return new StringEndsWith(false, str);
    }

    public static Matcher<String> endsWithIgnoringCase(String str) {
        return new StringEndsWith(true, str);
    }
}
