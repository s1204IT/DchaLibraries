package com.mediatek.search;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import com.mediatek.common.MPlugin;
import com.mediatek.common.regionalphone.RegionalPhone;
import com.mediatek.common.search.IRegionalPhoneSearchEngineExt;
import com.mediatek.common.search.SearchEngine;
import com.mediatek.internal.R;
import com.mediatek.search.ISearchEngineManagerService;
import java.util.ArrayList;
import java.util.List;

public class SearchEngineManagerService extends ISearchEngineManagerService.Stub {
    private static final String TAG = "SearchEngineManagerService";
    private final Context mContext;
    private SearchEngine mDefaultSearchEngine;
    private ContentObserver mSearchEngineObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            SearchEngineManagerService.this.initSearchEngines();
        }
    };
    private List<SearchEngine> mSearchEngines;

    public SearchEngineManagerService(Context context) {
        this.mContext = context;
        this.mContext.registerReceiver(new BootCompletedReceiver(this, null), new IntentFilter("android.intent.action.BOOT_COMPLETED"));
        this.mContext.getContentResolver().registerContentObserver(RegionalPhone.SEARCHENGINE_URI, true, this.mSearchEngineObserver);
    }

    private final class BootCompletedReceiver extends BroadcastReceiver {
        BootCompletedReceiver(SearchEngineManagerService this$0, BootCompletedReceiver bootCompletedReceiver) {
            this();
        }

        private BootCompletedReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            new Thread() {
                @Override
                public void run() {
                    Process.setThreadPriority(10);
                    SearchEngineManagerService.this.mContext.unregisterReceiver(BootCompletedReceiver.this);
                    SearchEngineManagerService.this.initSearchEngines();
                    SearchEngineManagerService.this.mContext.registerReceiver(new LocaleChangeReceiver(SearchEngineManagerService.this, null), new IntentFilter("android.intent.action.LOCALE_CHANGED"));
                }
            }.start();
        }
    }

    public synchronized List<SearchEngine> getAvailables() {
        Log.i(TAG, "get avilable search engines");
        if (this.mSearchEngines == null) {
            initSearchEngines();
        }
        return this.mSearchEngines;
    }

    private void initSearchEngines() throws IllegalArgumentException {
        IRegionalPhoneSearchEngineExt regionalPhoneSearchEngineExt = (IRegionalPhoneSearchEngineExt) MPlugin.createInstance(IRegionalPhoneSearchEngineExt.class.getName(), this.mContext);
        if (regionalPhoneSearchEngineExt != null) {
            this.mSearchEngines = regionalPhoneSearchEngineExt.initSearchEngineInfosFromRpm(this.mContext);
            if (this.mSearchEngines != null) {
                this.mDefaultSearchEngine = this.mSearchEngines.get(0);
                Log.d(TAG, "RegionalPhone Search engine init");
                return;
            }
        }
        this.mSearchEngines = new ArrayList();
        Resources res = this.mContext.getResources();
        String[] searchEngines = res.getStringArray(R.array.new_search_engines);
        if (searchEngines == null || 1 >= searchEngines.length) {
            throw new IllegalArgumentException("No data found for ");
        }
        String sp = searchEngines[0];
        for (int i = 1; i < searchEngines.length; i++) {
            String configInfo = searchEngines[i];
            SearchEngine info = SearchEngine.parseFrom(configInfo, sp);
            this.mSearchEngines.add(info);
        }
        if (this.mDefaultSearchEngine != null) {
            this.mDefaultSearchEngine = getBestMatch(this.mDefaultSearchEngine.getName(), this.mDefaultSearchEngine.getFaviconUri());
        }
        if (this.mDefaultSearchEngine == null) {
            this.mDefaultSearchEngine = this.mSearchEngines.get(0);
        }
        broadcastSearchEngineChangedInternal(this.mContext);
    }

    private final class LocaleChangeReceiver extends BroadcastReceiver {
        LocaleChangeReceiver(SearchEngineManagerService this$0, LocaleChangeReceiver localeChangeReceiver) {
            this();
        }

        private LocaleChangeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            SearchEngineManagerService.this.initSearchEngines();
        }
    }

    private void broadcastSearchEngineChangedInternal(Context context) {
        Intent intent = new Intent("com.mediatek.search.SEARCH_ENGINE_CHANGED");
        context.sendBroadcast(intent);
        Log.d(TAG, "broadcast serach engine changed");
    }

    public SearchEngine getBestMatch(String name, String favicon) {
        SearchEngine engine = getByName(name);
        return engine != null ? engine : getByFavicon(favicon);
    }

    private SearchEngine getByFavicon(String favicon) {
        List<SearchEngine> engines = getAvailables();
        for (SearchEngine engine : engines) {
            if (favicon.equals(engine.getFaviconUri())) {
                return engine;
            }
        }
        return null;
    }

    private SearchEngine getByName(String name) {
        List<SearchEngine> engines = getAvailables();
        for (SearchEngine engine : engines) {
            if (name.equals(engine.getName())) {
                return engine;
            }
        }
        return null;
    }

    public SearchEngine getSearchEngine(int field, String value) {
        switch (field) {
            case -1:
                return getByName(value);
            case 0:
            case 1:
            default:
                return null;
            case 2:
                return getByFavicon(value);
        }
    }

    public SearchEngine getDefault() {
        return this.mDefaultSearchEngine;
    }

    public boolean setDefault(SearchEngine engine) {
        List<SearchEngine> engines = getAvailables();
        for (SearchEngine eng : engines) {
            if (eng.getName().equals(engine.getName())) {
                this.mDefaultSearchEngine = engine;
                return true;
            }
        }
        return false;
    }
}
