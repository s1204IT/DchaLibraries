package org.hamcrest.core;

import java.util.regex.Pattern;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class DescribedAs<T> extends BaseMatcher<T> {
    private static final Pattern ARG_PATTERN = Pattern.compile("%([0-9]+)");
    private final String descriptionTemplate;
    private final Matcher<T> matcher;
    private final Object[] values;

    public DescribedAs(String str, Matcher<T> matcher, Object[] objArr) {
        this.descriptionTemplate = str;
        this.matcher = matcher;
        this.values = (Object[]) objArr.clone();
    }

    @Override
    public boolean matches(Object obj) {
        return this.matcher.matches(obj);
    }

    @Override
    public void describeTo(Description description) {
        java.util.regex.Matcher matcher = ARG_PATTERN.matcher(this.descriptionTemplate);
        int iEnd = 0;
        while (matcher.find()) {
            description.appendText(this.descriptionTemplate.substring(iEnd, matcher.start()));
            description.appendValue(this.values[Integer.parseInt(matcher.group(1))]);
            iEnd = matcher.end();
        }
        if (iEnd < this.descriptionTemplate.length()) {
            description.appendText(this.descriptionTemplate.substring(iEnd));
        }
    }

    @Override
    public void describeMismatch(Object obj, Description description) {
        this.matcher.describeMismatch(obj, description);
    }

    public static <T> Matcher<T> describedAs(String str, Matcher<T> matcher, Object... objArr) {
        return new DescribedAs(str, matcher, objArr);
    }
}
