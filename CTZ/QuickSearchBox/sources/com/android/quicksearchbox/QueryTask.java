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

    public QueryTask(String str, int i, SuggestionCursorProvider<C> suggestionCursorProvider, Handler handler, Consumer<C> consumer) {
        this.mQuery = str;
        this.mQueryLimit = i;
        this.mProvider = suggestionCursorProvider;
        this.mHandler = handler;
        this.mConsumer = consumer;
    }

    public static <C extends SuggestionCursor> void startQuery(String str, int i, SuggestionCursorProvider<C> suggestionCursorProvider, NamedTaskExecutor namedTaskExecutor, Handler handler, Consumer<C> consumer) {
        namedTaskExecutor.execute(new QueryTask(str, i, suggestionCursorProvider, handler, consumer));
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
}
