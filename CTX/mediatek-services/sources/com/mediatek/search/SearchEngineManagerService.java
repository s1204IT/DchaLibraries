package com.mediatek.search;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import com.mediatek.common.regionalphone.RegionalPhone;
import com.mediatek.common.search.SearchEngine;
import com.mediatek.search.ISearchEngineManagerService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SearchEngineManagerService extends ISearchEngineManagerService.Stub {
    private static final String TAG = "SearchEngineManagerService";
    private final Context mContext;
    private SearchEngine mDefaultSearchEngine;
    private ContentObserver mSearchEngineObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z) {
            SearchEngineManagerService.this.initSearchEngines();
            SearchEngineManagerService.this.broadcastSearchEngineChangedInternal(SearchEngineManagerService.this.mContext);
        }
    };
    private List<SearchEngine> mSearchEngines;

    public SearchEngineManagerService(Context context) {
        this.mContext = context;
        this.mContext.registerReceiver(new BootCompletedReceiver(), new IntentFilter("android.intent.action.BOOT_COMPLETED"));
        this.mContext.getContentResolver().registerContentObserver(RegionalPhone.SEARCHENGINE_URI, true, this.mSearchEngineObserver);
    }

    private final class BootCompletedReceiver extends BroadcastReceiver {
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
                    SearchEngineManagerService.this.mContext.registerReceiver(new LocaleChangeReceiver(), new IntentFilter("android.intent.action.LOCALE_CHANGED"));
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
        this.mSearchEngines = new ArrayList();
        String[] stringArray = this.mContext.getResources().getStringArray(134479879);
        if (stringArray != null) {
            if (1 < stringArray.length) {
                String str = stringArray[0];
                for (int i = 1; i < stringArray.length; i++) {
                    this.mSearchEngines.add(SearchEngine.parseFrom(stringArray[i], str));
                }
                if (this.mDefaultSearchEngine != null) {
                    this.mDefaultSearchEngine = getBestMatch(this.mDefaultSearchEngine.getName(), this.mDefaultSearchEngine.getFaviconUri());
                }
                if (this.mDefaultSearchEngine == null) {
                    this.mDefaultSearchEngine = this.mSearchEngines.get(0);
                    return;
                }
                return;
            }
        }
        throw new IllegalArgumentException("No data found for ");
    }

    private final class LocaleChangeReceiver extends BroadcastReceiver {
        private LocaleChangeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            SearchEngineManagerService.this.initSearchEngines();
            SearchEngineManagerService.this.broadcastSearchEngineChangedInternal(SearchEngineManagerService.this.mContext);
        }
    }

    private void broadcastSearchEngineChangedInternal(Context context) {
        context.sendBroadcast(new Intent("com.mediatek.search.SEARCH_ENGINE_CHANGED"));
        Log.d(TAG, "broadcast serach engine changed");
    }

    public SearchEngine getBestMatch(String str, String str2) {
        SearchEngine byName = getByName(str);
        return byName != null ? byName : getByFavicon(str2);
    }

    private SearchEngine getByFavicon(String str) {
        for (SearchEngine searchEngine : getAvailables()) {
            if (str.equals(searchEngine.getFaviconUri())) {
                return searchEngine;
            }
        }
        return null;
    }

    private SearchEngine getByName(String str) {
        for (SearchEngine searchEngine : getAvailables()) {
            if (str.equals(searchEngine.getName())) {
                return searchEngine;
            }
        }
        return null;
    }

    public SearchEngine getSearchEngine(int i, String str) {
        if (i == -1) {
            return getByName(str);
        }
        if (i == 2) {
            return getByFavicon(str);
        }
        return null;
    }

    public SearchEngine getDefault() {
        return this.mDefaultSearchEngine;
    }

    public boolean setDefault(SearchEngine searchEngine) {
        Iterator<SearchEngine> it = getAvailables().iterator();
        while (it.hasNext()) {
            if (it.next().getName().equals(searchEngine.getName())) {
                this.mDefaultSearchEngine = searchEngine;
                return true;
            }
        }
        return false;
    }
}
