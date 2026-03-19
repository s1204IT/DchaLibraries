package com.android.quicksearchbox;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
    public void onCreate(Bundle bundle) {
        this.mTraceStartUp = getIntent().hasExtra("trace_start_up");
        if (this.mTraceStartUp) {
            String absolutePath = new File(getDir("traces", 0), "qsb-start.trace").getAbsolutePath();
            Log.i("QSB.SearchActivity", "Writing start-up trace to " + absolutePath);
            Debug.startMethodTracing(absolutePath);
        }
        recordStartTime();
        super.onCreate(bundle);
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
            public boolean onSearchClicked(int i) {
                return SearchActivity.this.onSearchClicked(i);
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
        this.mSearchActivityView.setExitClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SearchActivity.this.finish();
            }
        });
        setupFromIntent(getIntent());
        restoreInstanceState(bundle);
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

    protected void restoreInstanceState(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        setQuery(bundle.getString("query"), false);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString("query", getQuery());
    }

    private void setupFromIntent(Intent intent) {
        getCorpusNameFromUri(intent.getData());
        String stringExtra = intent.getStringExtra("query");
        Bundle bundleExtra = intent.getBundleExtra("app_data");
        setQuery(stringExtra, intent.getBooleanExtra("select_query", false));
        this.mAppSearchData = bundleExtra;
    }

    private String getCorpusNameFromUri(Uri uri) {
        if (uri == null || !"qsb.corpus".equals(uri.getScheme())) {
            return null;
        }
        return uri.getAuthority();
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

    public void setOnDestroyListener(OnDestroyListener onDestroyListener) {
        this.mDestroyListener = onDestroyListener;
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

    public void createMenuItems(Menu menu, boolean z) {
        getQsbApplication().getHelp().addHelpMenuItem(menu, "search");
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (z) {
            this.mHandler.postDelayed(this.mShowInputMethodTask, 0L);
        }
    }

    protected String getQuery() {
        return this.mSearchActivityView.getQuery();
    }

    protected void setQuery(String str, boolean z) {
        this.mSearchActivityView.setQuery(str, z);
    }

    protected boolean onSearchClicked(int i) {
        String strTrimAndCollapseFrom = CharMatcher.WHITESPACE.trimAndCollapseFrom(getQuery(), ' ');
        if (TextUtils.getTrimmedLength(strTrimAndCollapseFrom) == 0) {
            return false;
        }
        this.mTookAction = true;
        getLogger().logSearch(i, strTrimAndCollapseFrom.length());
        startSearch(this.mSource, strTrimAndCollapseFrom);
        return true;
    }

    protected void startSearch(Source source, String str) {
        launchIntent(source.createSearchIntent(str, this.mAppSearchData));
    }

    protected void onVoiceSearchClicked() {
        this.mTookAction = true;
        getLogger().logVoiceSearch();
        launchIntent(this.mSource.createVoiceSearchIntent(this.mAppSearchData));
    }

    protected SuggestionCursor getCurrentSuggestions() {
        Suggestions suggestions = this.mSearchActivityView.getSuggestions();
        if (suggestions == null) {
            return null;
        }
        return suggestions.getResult();
    }

    protected SuggestionPosition getCurrentSuggestions(SuggestionsAdapter<?> suggestionsAdapter, long j) {
        SuggestionPosition suggestion = suggestionsAdapter.getSuggestion(j);
        if (suggestion == null) {
            return null;
        }
        SuggestionCursor cursor = suggestion.getCursor();
        int position = suggestion.getPosition();
        if (cursor == null) {
            return null;
        }
        int count = cursor.getCount();
        if (position < 0 || position >= count) {
            Log.w("QSB.SearchActivity", "Invalid suggestion position " + position + ", count = " + count);
            return null;
        }
        cursor.moveTo(position);
        return suggestion;
    }

    protected void launchIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        try {
            startActivity(intent);
        } catch (RuntimeException e) {
            Log.e("QSB.SearchActivity", "Failed to start " + intent.toUri(0), e);
        }
    }

    private boolean launchSuggestion(SuggestionsAdapter<?> suggestionsAdapter, long j) {
        SuggestionPosition currentSuggestions = getCurrentSuggestions(suggestionsAdapter, j);
        if (currentSuggestions == null) {
            return false;
        }
        this.mTookAction = true;
        getLogger().logSuggestionClick(j, currentSuggestions.getCursor(), 0);
        launchSuggestion(currentSuggestions.getCursor(), currentSuggestions.getPosition());
        return true;
    }

    protected void launchSuggestion(SuggestionCursor suggestionCursor, int i) {
        suggestionCursor.moveTo(i);
        launchIntent(SuggestionUtils.getSuggestionIntent(suggestionCursor, this.mAppSearchData));
    }

    protected void refineSuggestion(SuggestionsAdapter<?> suggestionsAdapter, long j) {
        SuggestionPosition currentSuggestions = getCurrentSuggestions(suggestionsAdapter, j);
        if (currentSuggestions == null) {
            return;
        }
        String suggestionQuery = currentSuggestions.getSuggestionQuery();
        if (TextUtils.isEmpty(suggestionQuery)) {
            return;
        }
        getLogger().logSuggestionClick(j, currentSuggestions.getCursor(), 1);
        setQuery(suggestionQuery + ' ', false);
        updateSuggestions();
        this.mSearchActivityView.focusQueryTextView();
    }

    private void updateSuggestionsBuffered() {
        this.mHandler.removeCallbacks(this.mUpdateSuggestionsTask);
        this.mHandler.postDelayed(this.mUpdateSuggestionsTask, getConfig().getTypingUpdateSuggestionsDelayMillis());
    }

    private void gotSuggestions(Suggestions suggestions) {
        if (this.mStarting) {
            this.mStarting = false;
            String stringExtra = getIntent().getStringExtra("source");
            getLogger().logStart(this.mOnCreateLatency, this.mStartLatencyTracker.getLatency(), stringExtra);
            getQsbApplication().onStartupComplete();
        }
    }

    public void updateSuggestions() {
        updateSuggestions(CharMatcher.WHITESPACE.trimLeadingFrom(getQuery()), this.mSource);
    }

    protected void updateSuggestions(String str, Source source) {
        Suggestions suggestions = getSuggestionsProvider().getSuggestions(str, source);
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
        public void onSuggestionClicked(SuggestionsAdapter<?> suggestionsAdapter, long j) {
            SearchActivity.this.launchSuggestion(suggestionsAdapter, j);
        }

        @Override
        public void onSuggestionQueryRefineClicked(SuggestionsAdapter<?> suggestionsAdapter, long j) {
            SearchActivity.this.refineSuggestion(suggestionsAdapter, j);
        }
    }
}
