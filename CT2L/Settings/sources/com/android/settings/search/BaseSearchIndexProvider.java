package com.android.settings.search;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.search.Indexable;
import java.util.Collections;
import java.util.List;

public class BaseSearchIndexProvider implements Indexable.SearchIndexProvider {
    private static final List<String> EMPTY_LIST = Collections.emptyList();

    @Override
    public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
        return null;
    }

    @Override
    public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
        return null;
    }

    @Override
    public List<String> getNonIndexableKeys(Context context) {
        return EMPTY_LIST;
    }
}
