package com.android.server.pm;

import com.android.server.IntentResolver;
import java.util.List;

class CrossProfileIntentResolver extends IntentResolver<CrossProfileIntentFilter, CrossProfileIntentFilter> {
    CrossProfileIntentResolver() {
    }

    @Override
    protected CrossProfileIntentFilter[] newArray(int i) {
        return new CrossProfileIntentFilter[i];
    }

    @Override
    protected boolean isPackageForFilter(String str, CrossProfileIntentFilter crossProfileIntentFilter) {
        return false;
    }

    @Override
    protected void sortResults(List<CrossProfileIntentFilter> list) {
    }
}
