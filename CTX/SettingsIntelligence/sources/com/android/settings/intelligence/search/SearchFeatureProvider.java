package com.android.settings.intelligence.search;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import com.android.settings.intelligence.search.indexing.IndexingCallback;
import com.android.settings.intelligence.search.query.SearchQueryTask;
import com.android.settings.intelligence.search.savedqueries.SavedQueryLoader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

public interface SearchFeatureProvider {
    ExecutorService getExecutorService();

    FutureTask<List<Pair<String, Float>>> getRankerTask(Context context, String str);

    SavedQueryLoader getSavedQueryLoader(Context context);

    List<SearchQueryTask> getSearchQueryTasks(Context context, String str);

    SearchResultLoader getSearchResultLoader(Context context, String str);

    void hideFeedbackButton(View view);

    void initFeedbackButton();

    boolean isIndexingComplete(Context context);

    boolean isSmartSearchRankingEnabled(Context context);

    void searchRankingWarmup(Context context);

    void searchResultClicked(Context context, String str, SearchResult searchResult);

    void showFeedbackButton(SearchFragment searchFragment, View view);

    long smartSearchRankingTimeoutMs(Context context);

    void updateIndexAsync(Context context, IndexingCallback indexingCallback);
}
