package com.android.quicksearchbox;

import android.os.Handler;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.Consumers;
import com.android.quicksearchbox.util.NamedTask;
import com.android.quicksearchbox.util.NamedTaskExecutor;

public class QueryTask<C extends SuggestionCursor> implements NamedTask {
    private final Consumer<C> mConsumer;
    private final Handler mHandler;
    private final SuggestionCursorProvider<C> mProvider;
    private final String mQuery;
    private final int mQueryLimit;

    public QueryTask(String query, int queryLimit, SuggestionCursorProvider<C> provider, Handler handler, Consumer<C> consumer) {
        this.mQuery = query;
        this.mQueryLimit = queryLimit;
        this.mProvider = provider;
        this.mHandler = handler;
        this.mConsumer = consumer;
    }

    @Override
    public String getName() {
        return this.mProvider.getName();
    }

    @Override
    public void run() {
        Consumers.consumeCloseableAsync(this.mHandler, this.mConsumer, this.mProvider.getSuggestions(this.mQuery, this.mQueryLimit));
    }

    public String toString() {
        return this.mProvider + "[" + this.mQuery + "]";
    }

    public static <C extends SuggestionCursor> void startQuery(String query, int maxResults, SuggestionCursorProvider<C> provider, NamedTaskExecutor executor, Handler handler, Consumer<C> consumer) {
        QueryTask<C> task = new QueryTask<>(query, maxResults, provider, handler, consumer);
        executor.execute(task);
    }
}
