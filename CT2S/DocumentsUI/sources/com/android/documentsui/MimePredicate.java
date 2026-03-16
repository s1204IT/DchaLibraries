package com.android.documentsui;

import com.android.documentsui.model.DocumentInfo;
import com.android.internal.util.Predicate;

public class MimePredicate implements Predicate<DocumentInfo> {
    public static final String[] VISUAL_MIMES = {"image/*", "video/*"};
    private final String[] mFilters;

    public boolean apply(DocumentInfo doc) {
        return doc.isDirectory() || mimeMatches(this.mFilters, doc.mimeType);
    }

    public static boolean mimeMatches(String[] filters, String[] tests) {
        if (tests == null) {
            return false;
        }
        for (String test : tests) {
            if (mimeMatches(filters, test)) {
                return true;
            }
        }
        return false;
    }

    public static boolean mimeMatches(String[] filters, String test) {
        if (filters == null) {
            return true;
        }
        for (String filter : filters) {
            if (mimeMatches(filter, test)) {
                return true;
            }
        }
        return false;
    }

    public static boolean mimeMatches(String filter, String test) {
        if (test == null) {
            return false;
        }
        if (filter == null || "*/*".equals(filter)) {
            return true;
        }
        if (filter.equals(test)) {
            return true;
        }
        if (filter.endsWith("/*")) {
            return filter.regionMatches(0, test, 0, filter.indexOf(47));
        }
        return false;
    }
}
