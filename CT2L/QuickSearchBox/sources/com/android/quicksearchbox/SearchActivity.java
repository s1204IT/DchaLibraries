package com.android.quicksearchbox;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import com.android.quicksearchbox.ui.SearchActivityView;
import com.android.quicksearchbox.ui.SuggestionClickListener;
import com.android.quicksearchbox.ui.SuggestionsAdapter;
import com.google.common.base.CharMatcher;
import java.io.File;

public class SearchActivity extends Activity {
    private Bundle mAppSearchData;
    private OnDestroyListener mDestroyListener;
    private int mOnCreateLatency;
    private LatencyTracker mOnCreateTracker;
    private SearchActivityView mSearchActivityView;
    private Source mSource;
    private LatencyTracker mStartLatencyTracker;
    private boolean mStarting;
    private boolean mTookAction;
    private boolean mTraceStartUp;
    private final Handler mHandler = new Handler();
    private final Runnable mUpdateSuggestionsTask = new Runnable() {
        @Override
        public void run() {
            SearchActivity.this.updateSuggestions();
        }
    };
    private final Runnable mShowInputMethodTask = new Runnable() {
        @Override
        public void run() {
            SearchActivity.this.mSearchActivityView.showInputMethodForQuery();
        }
    };

    public interface OnDestroyListener {
        void onDestroyed();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.mTraceStartUp = getIntent().hasExtra("trace_start_up");
        if (this.mTraceStartUp) {
            String traceFile = new File(getDir("traces", 0), "qsb-start.trace").getAbsolutePath();
            Log.i("QSB.SearchActivity", "Writing start-up trace to " + traceFile);
            Debug.startMethodTracing(traceFile);
        }
        recordStartTime();
        super.onCreate(savedInstanceState);
        QsbApplication.get(this).getSearchBaseUrlHelper();
        this.mSource = QsbApplication.get(this).getGoogleSource();
        this.mSearchActivityView = setupContentView();
        if (getConfig().showScrollingResults()) {
            this.mSearchActivityView.setMaxPromotedResults(getConfig().getMaxPromotedResults());
        } else {
            this.mSearchActivityView.limitResultsToViewHeight();
        }
        this.mSearchActivityView.setSearchClickListener(new SearchActivityView.SearchClickListener() {
            @Override
            public boolean onSearchClicked(int method) {
                return SearchActivity.this.onSearchClicked(method);
            }
        });
        this.mSearchActivityView.setQueryListener(new SearchActivityView.QueryListener() {
            @Override
            public void onQueryChanged() {
                SearchActivity.this.updateSuggestionsBuffered();
            }
        });
        this.mSearchActivityView.setSuggestionClickListener(new ClickHandler());
        this.mSearchActivityView.setVoiceSearchButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SearchActivity.this.onVoiceSearchClicked();
            }
        });
        View.OnClickListener finishOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SearchActivity.this.finish();
            }
        };
        this.mSearchActivityView.setExitClickListener(finishOnClick);
        Intent intent = getIntent();
        setupFromIntent(intent);
        restoreInstanceState(savedInstanceState);
        this.mSearchActivityView.start();
        recordOnCreateDone();
    }

    protected SearchActivityView setupContentView() {
        setContentView(R.layout.search_activity);
        return (SearchActivityView) findViewById(R.id.search_activity_view);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        recordStartTime();
        setIntent(intent);
        setupFromIntent(intent);
    }

    private void recordStartTime() {
        this.mStartLatencyTracker = new LatencyTracker();
        this.mOnCreateTracker = new LatencyTracker();
        this.mStarting = true;
        this.mTookAction = false;
    }

    private void recordOnCreateDone() {
        this.mOnCreateLatency = this.mOnCreateTracker.getLatency();
    }

    protected void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String query = savedInstanceState.getString("query");
            setQuery(query, false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("query", getQuery());
    }

    private void setupFromIntent(Intent intent) {
        getCorpusNameFromUri(intent.getData());
        String query = intent.getStringExtra("query");
        Bundle appSearchData = intent.getBundleExtra("app_data");
        boolean selectAll = intent.getBooleanExtra("select_query", false);
        setQuery(query, selectAll);
        this.mAppSearchData = appSearchData;
    }

    private String getCorpusNameFromUri(Uri uri) {
        if (uri != null && "qsb.corpus".equals(uri.getScheme())) {
            return uri.getAuthority();
        }
        return null;
    }

    private QsbApplication getQsbApplication() {
        return QsbApplication.get(this);
    }

    private Config getConfig() {
        return getQsbApplication().getConfig();
    }

    private SuggestionsProvider getSuggestionsProvider() {
        return getQsbApplication().getSuggestionsProvider();
    }

    private Logger getLogger() {
        return getQsbApplication().getLogger();
    }

    public void setOnDestroyListener(OnDestroyListener l) {
        this.mDestroyListener = l;
    }

    @Override
    protected void onDestroy() {
        this.mSearchActivityView.destroy();
        super.onDestroy();
        if (this.mDestroyListener != null) {
            this.mDestroyListener.onDestroyed();
        }
    }

    @Override
    protected void onStop() {
        if (!this.mTookAction) {
            getLogger().logExit(getCurrentSuggestions(), getQuery().length());
        }
        this.mSearchActivityView.clearSuggestions();
        this.mSearchActivityView.onStop();
        super.onStop();
    }

    @Override
    protected void onPause() {
        this.mSearchActivityView.onPause();
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSuggestionsBuffered();
        this.mSearchActivityView.onResume();
        if (this.mTraceStartUp) {
            Debug.stopMethodTracing();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        createMenuItems(menu, true);
        return true;
    }

    public void createMenuItems(Menu menu, boolean showDisabled) {
        getQsbApplication().getHelp().addHelpMenuItem(menu, "search");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            this.mHandler.postDelayed(this.mShowInputMethodTask, 0L);
        }
    }

    protected String getQuery() {
        return this.mSearchActivityView.getQuery();
    }

    protected void setQuery(String query, boolean selectAll) {
        this.mSearchActivityView.setQuery(query, selectAll);
    }

    protected boolean onSearchClicked(int method) {
        String query = CharMatcher.WHITESPACE.trimAndCollapseFrom(getQuery(), ' ');
        if (TextUtils.getTrimmedLength(query) == 0) {
            return false;
        }
        this.mTookAction = true;
        getLogger().logSearch(method, query.length());
        startSearch(this.mSource, query);
        return true;
    }

    protected void startSearch(Source searchSource, String query) {
        Intent intent = searchSource.createSearchIntent(query, this.mAppSearchData);
        launchIntent(intent);
    }

    protected void onVoiceSearchClicked() {
        this.mTookAction = true;
        getLogger().logVoiceSearch();
        Intent intent = this.mSource.createVoiceSearchIntent(this.mAppSearchData);
        launchIntent(intent);
    }

    protected SuggestionCursor getCurrentSuggestions() {
        Suggestions suggestions = this.mSearchActivityView.getSuggestions();
        if (suggestions == null) {
            return null;
        }
        return suggestions.getResult();
    }

    protected SuggestionPosition getCurrentSuggestions(SuggestionsAdapter<?> adapter, long id) {
        SuggestionPosition pos = adapter.getSuggestion(id);
        if (pos == null) {
            return null;
        }
        SuggestionCursor suggestions = pos.getCursor();
        int position = pos.getPosition();
        if (suggestions == null) {
            return null;
        }
        int count = suggestions.getCount();
        if (position < 0 || position >= count) {
            Log.w("QSB.SearchActivity", "Invalid suggestion position " + position + ", count = " + count);
            return null;
        }
        suggestions.moveTo(position);
        return pos;
    }

    protected void launchIntent(Intent intent) {
        if (intent != null && BenesseExtension.getDchaState() == 0) {
            try {
                startActivity(intent);
            } catch (RuntimeException ex) {
                Log.e("QSB.SearchActivity", "Failed to start " + intent.toUri(0), ex);
            }
        }
    }

    private boolean launchSuggestion(SuggestionsAdapter<?> adapter, long id) {
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion == null) {
            return false;
        }
        this.mTookAction = true;
        getLogger().logSuggestionClick(id, suggestion.getCursor(), 0);
        launchSuggestion(suggestion.getCursor(), suggestion.getPosition());
        return true;
    }

    protected void launchSuggestion(SuggestionCursor suggestions, int position) {
        suggestions.moveTo(position);
        Intent intent = SuggestionUtils.getSuggestionIntent(suggestions, this.mAppSearchData);
        launchIntent(intent);
    }

    protected void refineSuggestion(SuggestionsAdapter<?> adapter, long id) {
        SuggestionPosition suggestion = getCurrentSuggestions(adapter, id);
        if (suggestion != null) {
            String query = suggestion.getSuggestionQuery();
            if (!TextUtils.isEmpty(query)) {
                getLogger().logSuggestionClick(id, suggestion.getCursor(), 1);
                String queryWithSpace = query + ' ';
                setQuery(queryWithSpace, false);
                updateSuggestions();
                this.mSearchActivityView.focusQueryTextView();
            }
        }
    }

    private void updateSuggestionsBuffered() {
        this.mHandler.removeCallbacks(this.mUpdateSuggestionsTask);
        long delay = getConfig().getTypingUpdateSuggestionsDelayMillis();
        this.mHandler.postDelayed(this.mUpdateSuggestionsTask, delay);
    }

    private void gotSuggestions(Suggestions suggestions) {
        if (this.mStarting) {
            this.mStarting = false;
            String source = getIntent().getStringExtra("source");
            int latency = this.mStartLatencyTracker.getLatency();
            getLogger().logStart(this.mOnCreateLatency, latency, source);
            getQsbApplication().onStartupComplete();
        }
    }

    public void updateSuggestions() {
        String query = CharMatcher.WHITESPACE.trimLeadingFrom(getQuery());
        updateSuggestions(query, this.mSource);
    }

    protected void updateSuggestions(String query, Source source) {
        Suggestions suggestions = getSuggestionsProvider().getSuggestions(query, source);
        gotSuggestions(suggestions);
        showSuggestions(suggestions);
    }

    protected void showSuggestions(Suggestions suggestions) {
        this.mSearchActivityView.setSuggestions(suggestions);
    }

    private class ClickHandler implements SuggestionClickListener {
        private ClickHandler() {
        }

        @Override
        public void onSuggestionClicked(SuggestionsAdapter<?> adapter, long id) {
            SearchActivity.this.launchSuggestion(adapter, id);
        }

        @Override
        public void onSuggestionQueryRefineClicked(SuggestionsAdapter<?> adapter, long id) {
            SearchActivity.this.refineSuggestion(adapter, id);
        }
    }
}
