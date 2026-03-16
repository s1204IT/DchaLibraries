package com.android.server.pm;

import com.android.server.IntentResolver;
import java.util.List;

class CrossProfileIntentResolver extends IntentResolver<CrossProfileIntentFilter, CrossProfileIntentFilter> {
    CrossProfileIntentResolver() {
    }

    @Override
    protected CrossProfileIntentFilter[] newArray(int size) {
        return new CrossProfileIntentFilter[size];
    }

    @Override
    protected boolean isPackageForFilter(String packageName, CrossProfileIntentFilter filter) {
        return false;
    }

    @Override
    protected void sortResults(List<CrossProfileIntentFilter> results) {
    }
}
