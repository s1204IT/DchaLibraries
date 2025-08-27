package com.android.browser.search;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.search.SearchEngineManager;

/* loaded from: classes.dex */
public class SearchEngines {
    public static SearchEngine getDefaultSearchEngine(Context context) {
        return DefaultSearchEngine.create(context);
    }

    public static SearchEngine get(Context context, String str) {
        com.mediatek.common.search.SearchEngine searchEngineInfo;
        SearchEngine defaultSearchEngine = getDefaultSearchEngine(context);
        return (TextUtils.isEmpty(str) || (defaultSearchEngine != null && str.equals(defaultSearchEngine.getName())) || (searchEngineInfo = getSearchEngineInfo(context, str)) == null) ? defaultSearchEngine : new OpenSearchSearchEngine(context, searchEngineInfo);
    }

    public static com.mediatek.common.search.SearchEngine getSearchEngineInfo(Context context, String str) {
        try {
            return ((SearchEngineManager) context.getSystemService("search_engine_service")).getByName(str);
        } catch (IllegalArgumentException e) {
            Log.e("SearchEngines", "Cannot load search engine " + str, e);
            return null;
        }
    }
}
