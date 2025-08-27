package com.android.quicksearchbox;

import android.os.Handler;
import android.util.Log;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.NoOpConsumer;

/* loaded from: classes.dex */
public class SuggestionsProviderImpl implements SuggestionsProvider {
    private final Config mConfig;
    private final Logger mLogger;
    private final Handler mPublishThread;
    private final NamedTaskExecutor mQueryExecutor;

    public SuggestionsProviderImpl(Config config, NamedTaskExecutor namedTaskExecutor, Handler handler, Logger logger) {
        this.mConfig = config;
        this.mQueryExecutor = namedTaskExecutor;
        this.mPublishThread = handler;
        this.mLogger = logger;
    }

    @Override // com.android.quicksearchbox.SuggestionsProvider
    public void close() {
    }

    @Override // com.android.quicksearchbox.SuggestionsProvider
    public Suggestions getSuggestions(String str, Source source) {
        Consumer noOpConsumer;
        Suggestions suggestions = new Suggestions(str, source);
        Log.i("QSB.SuggestionsProviderImpl", "chars:" + str.length() + ",source:" + source);
        if (shouldDisplayResults(str)) {
            noOpConsumer = new SuggestionCursorReceiver(suggestions);
        } else {
            noOpConsumer = new NoOpConsumer();
            suggestions.done();
        }
        QueryTask.startQuery(str, this.mConfig.getMaxResultsPerSource(), source, this.mQueryExecutor, this.mPublishThread, noOpConsumer);
        return suggestions;
    }

    private boolean shouldDisplayResults(String str) {
        if (str.length() == 0 && !this.mConfig.showSuggestionsForZeroQuery()) {
            return false;
        }
        return true;
    }

    private class SuggestionCursorReceiver implements Consumer<SourceResult> {
        private final Suggestions mSuggestions;

        public SuggestionCursorReceiver(Suggestions suggestions) {
            this.mSuggestions = suggestions;
        }

        /* JADX DEBUG: Method merged with bridge method: consume(Ljava/lang/Object;)Z */
        @Override // com.android.quicksearchbox.util.Consumer
        public boolean consume(SourceResult sourceResult) {
            this.mSuggestions.addResults(sourceResult);
            if (sourceResult != null && SuggestionsProviderImpl.this.mLogger != null) {
                SuggestionsProviderImpl.this.mLogger.logLatency(sourceResult);
                return true;
            }
            return true;
        }
    }
}
