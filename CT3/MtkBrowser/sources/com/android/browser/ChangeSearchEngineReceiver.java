package com.android.browser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.mediatek.common.search.SearchEngine;
import com.mediatek.search.SearchEngineManager;

public class ChangeSearchEngineReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = p.edit();
        SearchEngineManager searchEngineManager = (SearchEngineManager) context.getSystemService("search_engine");
        String searchEngineFavicon = "";
        String action = intent.getAction();
        if ("com.android.browser.SEARCH_ENGINE_CHANGED".equals(action)) {
            if (intent.getExtras() == null) {
                return;
            }
            String searchEngineName = intent.getExtras().getString("search_engine");
            SearchEngine searchEngineInfo = searchEngineManager.getByName(searchEngineName);
            if (searchEngineInfo != null) {
                searchEngineFavicon = searchEngineInfo.getFaviconUri();
            }
            editor.putString("search_engine", searchEngineName);
            editor.putString("search_engine_favicon", searchEngineFavicon);
            editor.commit();
            Log.d("@M_browser/ChangeSearchEngineReceiver", "ChangeSearchEngineReceiver (browser): search_engine---" + intent.getExtras().getString("search_engine"));
            return;
        }
        if (!"com.mediatek.search.SEARCH_ENGINE_CHANGED".equals(action)) {
            return;
        }
        Log.d("@M_browser/ChangeSearchEngineReceiver", "ChangeSearchEngineReceiver (search): search_engine---" + BrowserSettings.getInstance().getSearchEngineName());
    }
}
