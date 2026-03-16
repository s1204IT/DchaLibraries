package com.android.quicksearchbox;

import android.os.Handler;
import android.util.Log;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.NoOpConsumer;

public class SuggestionsProviderImpl implements SuggestionsProvider {
    private final Config mConfig;
    private final Logger mLogger;
    private final Handler mPublishThread;
    private final NamedTaskExecutor mQueryExecutor;

    public SuggestionsProviderImpl(Config config, NamedTaskExecutor queryExecutor, Handler publishThread, Logger logger) {
        this.mConfig = config;
        this.mQueryExecutor = queryExecutor;
        this.mPublishThread = publishThread;
        this.mLogger = logger;
    }

    @Override
    public void close() {
    }

    @Override
    public Suggestions getSuggestions(String query, Source sourceToQuery) {
        Consumer<SourceResult> receiver;
        Suggestions suggestions = new Suggestions(query, sourceToQuery);
        Log.i("QSB.SuggestionsProviderImpl", "chars:" + query.length() + ",source:" + sourceToQuery);
        if (shouldDisplayResults(query)) {
            receiver = new SuggestionCursorReceiver(suggestions);
        } else {
            receiver = new NoOpConsumer<>();
            suggestions.done();
        }
        int maxResults = this.mConfig.getMaxResultsPerSource();
        QueryTask.startQuery(query, maxResults, sourceToQuery, this.mQueryExecutor, this.mPublishThread, receiver);
        return suggestions;
    }

    private boolean shouldDisplayResults(String query) {
        return query.length() != 0 || this.mConfig.showSuggestionsForZeroQuery();
    }

    private class SuggestionCursorReceiver implements Consumer<SourceResult> {
        private final Suggestions mSuggestions;

        public SuggestionCursorReceiver(Suggestions suggestions) {
            this.mSuggestions = suggestions;
        }

        @Override
        public boolean consume(SourceResult cursor) {
            this.mSuggestions.addResults(cursor);
            if (cursor != null && SuggestionsProviderImpl.this.mLogger != null) {
                SuggestionsProviderImpl.this.mLogger.logLatency(cursor);
                return true;
            }
            return true;
        }
    }
}
