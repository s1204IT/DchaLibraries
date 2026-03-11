package com.android.launcher3.util;

import java.util.Set;

public abstract class StringFilter {
    StringFilter(StringFilter stringFilter) {
        this();
    }

    public abstract boolean matches(String str);

    private StringFilter() {
    }

    public static StringFilter matchesAll() {
        return new StringFilter() {
            @Override
            public boolean matches(String str) {
                return true;
            }
        };
    }

    public static StringFilter of(final Set<String> validEntries) {
        return new StringFilter() {
            {
                super(null);
            }

            @Override
            public boolean matches(String str) {
                return validEntries.contains(str);
            }
        };
    }
}
