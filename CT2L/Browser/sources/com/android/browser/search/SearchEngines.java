package com.android.browser.search;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import com.android.browser.R;
import java.util.ArrayList;
import java.util.List;

public class SearchEngines {
    public static SearchEngine getDefaultSearchEngine(Context context) {
        return DefaultSearchEngine.create(context);
    }

    public static List<SearchEngineInfo> getSearchEngineInfos(Context context) {
        ArrayList<SearchEngineInfo> searchEngineInfos = new ArrayList<>();
        Resources res = context.getResources();
        String[] searchEngines = res.getStringArray(R.array.search_engines);
        for (String name : searchEngines) {
            SearchEngineInfo info = new SearchEngineInfo(context, name);
            searchEngineInfos.add(info);
        }
        return searchEngineInfos;
    }

    public static SearchEngine get(Context context, String name) {
        SearchEngineInfo searchEngineInfo;
        SearchEngine defaultSearchEngine = getDefaultSearchEngine(context);
        if (TextUtils.isEmpty(name)) {
            return defaultSearchEngine;
        }
        return ((defaultSearchEngine == null || !name.equals(defaultSearchEngine.getName())) && (searchEngineInfo = getSearchEngineInfo(context, name)) != null) ? new OpenSearchSearchEngine(context, searchEngineInfo) : defaultSearchEngine;
    }

    public static SearchEngineInfo getSearchEngineInfo(Context context, String name) {
        try {
            return new SearchEngineInfo(context, name);
        } catch (IllegalArgumentException exception) {
            Log.e("SearchEngines", "Cannot load search engine " + name, exception);
            return null;
        }
    }
}
