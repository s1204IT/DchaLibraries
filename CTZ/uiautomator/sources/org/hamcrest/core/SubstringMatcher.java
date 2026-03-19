package org.hamcrest.core;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public abstract class SubstringMatcher extends TypeSafeMatcher<String> {
    private final boolean ignoringCase;
    private final String relationship;
    protected final String substring;

    protected abstract boolean evalSubstringOf(String str);

    protected SubstringMatcher(String str, boolean z, String str2) {
        this.relationship = str;
        this.ignoringCase = z;
        this.substring = str2;
    }

    @Override
    public boolean matchesSafely(String str) {
        if (this.ignoringCase) {
            str = str.toLowerCase();
        }
        return evalSubstringOf(str);
    }

    @Override
    public void describeMismatchSafely(String str, Description description) {
        description.appendText("was \"").appendText(str).appendText("\"");
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a string ").appendText(this.relationship).appendText(" ").appendValue(this.substring);
        if (this.ignoringCase) {
            description.appendText(" ignoring case");
        }
    }

    protected String converted(String str) {
        return this.ignoringCase ? str.toLowerCase() : str;
    }
}
