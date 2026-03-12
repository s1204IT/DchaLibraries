package com.android.settings.intelligence.search.indexing;

import android.provider.SearchIndexableData;
import android.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PreIndexData {
    private final List<SearchIndexableData> mDataToUpdate = new ArrayList();
    private final Map<String, Set<String>> mNonIndexableKeys = new HashMap();
    private final List<Pair<String, String>> mSiteMapPairs = new ArrayList();

    public Map<String, Set<String>> getNonIndexableKeys() {
        return this.mNonIndexableKeys;
    }

    public List<SearchIndexableData> getDataToUpdate() {
        return this.mDataToUpdate;
    }

    public List<Pair<String, String>> getSiteMapPairs() {
        return this.mSiteMapPairs;
    }

    public void addNonIndexableKeysForAuthority(String str, Set<String> set) {
        this.mNonIndexableKeys.put(str, set);
    }

    public void addDataToUpdate(List<? extends SearchIndexableData> list) {
        this.mDataToUpdate.addAll(list);
    }

    public void addSiteMapPairs(List<Pair<String, String>> list) {
        if (list == null) {
            return;
        }
        this.mSiteMapPairs.addAll(list);
    }
}
