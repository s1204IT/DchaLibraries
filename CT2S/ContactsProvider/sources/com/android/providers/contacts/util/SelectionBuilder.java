package com.android.providers.contacts.util;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

public class SelectionBuilder {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private final List<String> mWhereClauses = new ArrayList();

    public SelectionBuilder(String baseSelection) {
        addClause(baseSelection);
    }

    public SelectionBuilder addClause(String clause) {
        if (!TextUtils.isEmpty(clause)) {
            this.mWhereClauses.add(clause);
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
