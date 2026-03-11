package com.android.browser.search;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.search.SearchEngineManager;

public class SearchEngines {
    public static SearchEngine getDefaultSearchEngine(Context context) {
        return DefaultSearchEngine.create(context);
    }

    public static SearchEngine get(Context context, String name) {
        com.mediatek.common.search.SearchEngine searchEngineInfo;
        SearchEngine defaultSearchEngine = getDefaultSearchEngine(context);
        return (TextUtils.isEmpty(name) || (defaultSearchEngine != null && name.equals(defaultSearchEngine.getName())) || (searchEngineInfo = getSearchEngineInfo(context, name)) == null) ? defaultSearchEngine : new OpenSearchSearchEngine(context, searchEngineInfo);
    }

    public static com.mediatek.common.search.SearchEngine getSearchEngineInfo(Context context, String name) {
        try {
            SearchEngineManager searchEngineManger = (SearchEngineManager) context.getSystemService("search_engine");
            return searchEngineManger.getByName(name);
        } catch (IllegalArgumentException exception) {
            Log.e("SearchEngines", "Cannot load search engine " + name, exception);
            return null;
        }
    }
}
