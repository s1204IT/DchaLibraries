package com.android.settings.intelligence.search;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import com.android.settings.intelligence.search.indexing.DatabaseIndexingManager;
import com.android.settings.intelligence.search.indexing.IndexData;
import com.android.settings.intelligence.search.indexing.IndexingCallback;
import com.android.settings.intelligence.search.query.AccessibilityServiceResultTask;
import com.android.settings.intelligence.search.query.DatabaseResultTask;
import com.android.settings.intelligence.search.query.InputDeviceResultTask;
import com.android.settings.intelligence.search.query.InstalledAppResultTask;
import com.android.settings.intelligence.search.query.SearchQueryTask;
import com.android.settings.intelligence.search.savedqueries.SavedQueryLoader;
import com.android.settings.intelligence.search.sitemap.SiteMapManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class SearchFeatureProviderImpl implements SearchFeatureProvider {
    private DatabaseIndexingManager mDatabaseIndexingManager;
    private ExecutorService mExecutorService;
    private SiteMapManager mSiteMapManager;

    @Override
    public SearchResultLoader getSearchResultLoader(Context context, String str) {
        return new SearchResultLoader(context, cleanQuery(str));
    }

    @Override
    public List<SearchQueryTask> getSearchQueryTasks(Context context, String str) {
        ArrayList arrayList = new ArrayList();
        String strCleanQuery = cleanQuery(str);
        arrayList.add(DatabaseResultTask.newTask(context, getSiteMapManager(), strCleanQuery));
        arrayList.add(InstalledAppResultTask.newTask(context, getSiteMapManager(), strCleanQuery));
        arrayList.add(AccessibilityServiceResultTask.newTask(context, getSiteMapManager(), strCleanQuery));
        arrayList.add(InputDeviceResultTask.newTask(context, getSiteMapManager(), strCleanQuery));
        return arrayList;
    }

    @Override
    public SavedQueryLoader getSavedQueryLoader(Context context) {
        return new SavedQueryLoader(context);
    }

    public DatabaseIndexingManager getIndexingManager(Context context) {
        if (this.mDatabaseIndexingManager == null) {
            this.mDatabaseIndexingManager = new DatabaseIndexingManager(context.getApplicationContext());
        }
        return this.mDatabaseIndexingManager;
    }

    public SiteMapManager getSiteMapManager() {
        if (this.mSiteMapManager == null) {
            this.mSiteMapManager = new SiteMapManager();
        }
        return this.mSiteMapManager;
    }

    @Override
    public boolean isIndexingComplete(Context context) {
        return getIndexingManager(context).isIndexingComplete();
    }

    @Override
    public void initFeedbackButton() {
    }

    @Override
    public void showFeedbackButton(SearchFragment searchFragment, View view) {
    }

    @Override
    public void hideFeedbackButton(View view) {
    }

    @Override
    public void searchResultClicked(Context context, String str, SearchResult searchResult) {
    }

    @Override
    public boolean isSmartSearchRankingEnabled(Context context) {
        return false;
    }

    @Override
    public long smartSearchRankingTimeoutMs(Context context) {
        return 300L;
    }

    @Override
    public void searchRankingWarmup(Context context) {
    }

    @Override
    public FutureTask<List<Pair<String, Float>>> getRankerTask(Context context, String str) {
        return null;
    }

    @Override
    public void updateIndexAsync(Context context, IndexingCallback indexingCallback) {
        getIndexingManager(context).indexDatabase(indexingCallback);
    }

    @Override
    public ExecutorService getExecutorService() {
        if (this.mExecutorService == null) {
            this.mExecutorService = Executors.newCachedThreadPool();
        }
        return this.mExecutorService;
    }

    String cleanQuery(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        if (Locale.getDefault().equals(Locale.JAPAN)) {
            str = IndexData.normalizeJapaneseString(str);
        }
        return str.trim();
    }
}
