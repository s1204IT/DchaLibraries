package com.android.browser.search;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import java.util.ArrayList;

class SearchEnginePreference extends ListPreference {
    public SearchEnginePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        ArrayList<CharSequence> entryValues = new ArrayList<>();
        ArrayList<CharSequence> entries = new ArrayList<>();
        SearchEngine defaultSearchEngine = SearchEngines.getDefaultSearchEngine(context);
        String defaultSearchEngineName = null;
        if (defaultSearchEngine != null) {
            defaultSearchEngineName = defaultSearchEngine.getName();
            int d = defaultSearchEngineName.indexOf(95);
            if (d > 0) {
                entryValues.add(defaultSearchEngineName.substring(0, d));
            } else {
                entryValues.add(defaultSearchEngineName);
            }
            entries.add(defaultSearchEngine.getLabel());
        }
        for (SearchEngineInfo searchEngineInfo : SearchEngines.getSearchEngineInfos(context)) {
            String name = searchEngineInfo.getName();
            if (!name.equals(defaultSearchEngineName)) {
                int d2 = name.indexOf(95);
                if (d2 > 0) {
                    entryValues.add(name.substring(0, d2));
                } else {
                    entryValues.add(name);
                }
                entries.add(searchEngineInfo.getLabel());
            }
        }
        setEntryValues((CharSequence[]) entryValues.toArray(new CharSequence[entryValues.size()]));
        setEntries((CharSequence[]) entries.toArray(new CharSequence[entries.size()]));
    }
}
