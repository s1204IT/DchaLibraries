package com.android.providers.contacts.util;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

public class SelectionBuilder {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private final List<String> mWhereClauses = new ArrayList();

    public SelectionBuilder(String str) {
        addClause(str);
    }

    public SelectionBuilder addClause(String str) {
        if (!TextUtils.isEmpty(str)) {
            this.mWhereClauses.add(str);
        }
        return this;
    }

    public String build() {
        if (this.mWhereClauses.size() == 0) {
            return null;
        }
        return DbQueryUtils.concatenateClauses((String[]) this.mWhereClauses.toArray(EMPTY_STRING_ARRAY));
    }
}
