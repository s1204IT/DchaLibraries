package com.android.settings.intelligence.search;

import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;
import com.android.settings.intelligence.overlay.FeatureFactory;
import com.android.settings.intelligence.search.query.SearchQueryTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SearchResultAggregator {
    private static SearchResultAggregator sResultAggregator;

    private SearchResultAggregator() {
    }

    public static SearchResultAggregator getInstance() {
        if (sResultAggregator == null) {
            sResultAggregator = new SearchResultAggregator();
        }
        return sResultAggregator;
    }

    public synchronized List<? extends SearchResult> fetchResults(Context context, String str) {
        ArrayMap arrayMap;
        SearchFeatureProvider searchFeatureProvider = FeatureFactory.get(context).searchFeatureProvider();
        ExecutorService executorService = searchFeatureProvider.getExecutorService();
        List<SearchQueryTask> searchQueryTasks = searchFeatureProvider.getSearchQueryTasks(context, str);
        Iterator<SearchQueryTask> it = searchQueryTasks.iterator();
        while (it.hasNext()) {
            executorService.execute(it.next());
        }
        arrayMap = new ArrayMap();
        System.currentTimeMillis();
        for (SearchQueryTask searchQueryTask : searchQueryTasks) {
            int taskId = searchQueryTask.getTaskId();
            try {
                arrayMap.put(Integer.valueOf(taskId), searchQueryTask.get(300L, TimeUnit.MILLISECONDS));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                Log.d("SearchResultAggregator", "Could not retrieve result in time: " + taskId, e);
                arrayMap.put(Integer.valueOf(taskId), Collections.EMPTY_LIST);
            }
        }
        System.currentTimeMillis();
        return mergeSearchResults(arrayMap);
    }

    private List<? extends SearchResult> mergeSearchResults(Map<Integer, List<? extends SearchResult>> map) {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(map.remove(13));
        PriorityQueue priorityQueue = new PriorityQueue();
        Iterator<List<? extends SearchResult>> it = map.values().iterator();
        while (it.hasNext()) {
            priorityQueue.addAll(it.next());
        }
        while (!priorityQueue.isEmpty()) {
            arrayList.add((SearchResult) priorityQueue.poll());
        }
        return arrayList;
    }
}
