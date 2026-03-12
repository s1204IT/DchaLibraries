package com.android.settings.intelligence.search;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.EventLog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toolbar;
import com.android.settings.intelligence.R;
import com.android.settings.intelligence.instrumentation.MetricsFeatureProvider;
import com.android.settings.intelligence.overlay.FeatureFactory;
import com.android.settings.intelligence.search.indexing.IndexingCallback;
import com.android.settings.intelligence.search.savedqueries.SavedQueryController;
import com.android.settings.intelligence.search.savedqueries.SavedQueryViewHolder;
import java.util.List;

public class SearchFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<? extends SearchResult>>, SearchView.OnQueryTextListener, IndexingCallback {
    private long mEnterQueryTimestampMs;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    LinearLayout mNoResultsView;
    String mQuery;
    RecyclerView mResultsRecyclerView;
    SavedQueryController mSavedQueryController;
    SearchResultsAdapter mSearchAdapter;
    SearchFeatureProvider mSearchFeatureProvider;
    SearchView mSearchView;
    boolean mShowingSavedQuery;
    private boolean mNeverEnteredQuery = true;
    final RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int i, int i2) {
            if (i2 != 0) {
                SearchFragment.this.hideKeyboard();
            }
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.mSearchFeatureProvider = FeatureFactory.get(context).searchFeatureProvider();
        this.mMetricsFeatureProvider = FeatureFactory.get(context).metricsFeatureProvider(context);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        System.currentTimeMillis();
        setHasOptionsMenu(true);
        LoaderManager loaderManager = getLoaderManager();
        this.mSearchAdapter = new SearchResultsAdapter(this);
        this.mSavedQueryController = new SavedQueryController(getContext(), loaderManager, this.mSearchAdapter);
        this.mSearchFeatureProvider.initFeedbackButton();
        if (bundle != null) {
            this.mQuery = bundle.getString("state_query");
            this.mNeverEnteredQuery = bundle.getBoolean("state_never_entered_query");
            this.mShowingSavedQuery = bundle.getBoolean("state_showing_saved_query");
        } else {
            this.mShowingSavedQuery = true;
        }
        this.mSearchFeatureProvider.updateIndexAsync(getContext(), this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        this.mSavedQueryController.buildMenuItem(menu);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        Activity activity = getActivity();
        View viewInflate = layoutInflater.inflate(R.layout.search_panel, viewGroup, false);
        this.mResultsRecyclerView = (RecyclerView) viewInflate.findViewById(R.id.list_results);
        this.mResultsRecyclerView.setAdapter(this.mSearchAdapter);
        this.mResultsRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        this.mResultsRecyclerView.addOnScrollListener(this.mScrollListener);
        this.mNoResultsView = (LinearLayout) viewInflate.findViewById(R.id.no_results_layout);
        Toolbar toolbar = (Toolbar) viewInflate.findViewById(R.id.search_toolbar);
        activity.setActionBar(toolbar);
        activity.getActionBar().setDisplayHomeAsUpEnabled(true);
        this.mSearchView = (SearchView) toolbar.findViewById(R.id.search_view);
        this.mSearchView.setQuery(this.mQuery, false);
        this.mSearchView.setOnQueryTextListener(this);
        this.mSearchView.requestFocus();
        return viewInflate;
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mMetricsFeatureProvider.logEvent(4);
    }

    @Override
    public void onResume() {
        super.onResume();
        Context applicationContext = getContext().getApplicationContext();
        if (this.mSearchFeatureProvider.isSmartSearchRankingEnabled(applicationContext)) {
            this.mSearchFeatureProvider.searchRankingWarmup(applicationContext);
        }
        requery();
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mMetricsFeatureProvider.logEvent(5);
        Activity activity = getActivity();
        if (activity != null && activity.isFinishing() && this.mNeverEnteredQuery) {
            this.mMetricsFeatureProvider.logEvent(12);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString("state_query", this.mQuery);
        bundle.putBoolean("state_never_entered_query", this.mNeverEnteredQuery);
        bundle.putBoolean("state_showing_saved_query", this.mShowingSavedQuery);
    }

    @Override
    public boolean onQueryTextChange(String str) {
        if (TextUtils.equals(str, this.mQuery)) {
            return true;
        }
        this.mEnterQueryTimestampMs = System.currentTimeMillis();
        boolean zIsEmpty = TextUtils.isEmpty(str);
        if (this.mQuery != null && this.mNoResultsView.getVisibility() == 0 && str.length() < this.mQuery.length()) {
            this.mNoResultsView.setVisibility(8);
        }
        this.mNeverEnteredQuery = false;
        this.mQuery = str;
        if (!this.mSearchFeatureProvider.isIndexingComplete(getActivity())) {
            return true;
        }
        if (zIsEmpty) {
            getLoaderManager().destroyLoader(1);
            this.mShowingSavedQuery = true;
            this.mSavedQueryController.loadSavedQueries();
            this.mSearchFeatureProvider.hideFeedbackButton(getView());
        } else {
            this.mMetricsFeatureProvider.logEvent(6);
            restartLoaders();
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String str) {
        this.mSavedQueryController.saveQuery(this.mQuery);
        hideKeyboard();
        return true;
    }

    @Override
    public Loader<List<? extends SearchResult>> onCreateLoader(int i, Bundle bundle) {
        Activity activity = getActivity();
        if (i == 1) {
            return this.mSearchFeatureProvider.getSearchResultLoader(activity, this.mQuery);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<? extends SearchResult>> loader, List<? extends SearchResult> list) {
        this.mSearchAdapter.postSearchResults(list);
    }

    @Override
    public void onLoaderReset(Loader<List<? extends SearchResult>> loader) {
    }

    @Override
    public void onIndexingFinished() {
        if (getActivity() == null) {
            return;
        }
        if (this.mShowingSavedQuery) {
            this.mSavedQueryController.loadSavedQueries();
        } else {
            getLoaderManager().initLoader(1, null, this);
        }
        requery();
    }

    public void onSearchResultClicked(SearchViewHolder searchViewHolder, SearchResult searchResult) {
        logSearchResultClicked(searchViewHolder, searchResult);
        this.mSearchFeatureProvider.searchResultClicked(getContext(), this.mQuery, searchResult);
        this.mSavedQueryController.saveQuery(this.mQuery);
    }

    public void onSearchResultsDisplayed(int i) {
        long jCurrentTimeMillis = this.mEnterQueryTimestampMs > 0 ? System.currentTimeMillis() - this.mEnterQueryTimestampMs : 0L;
        if (i == 0) {
            this.mNoResultsView.setVisibility(0);
            this.mMetricsFeatureProvider.logEvent(10, jCurrentTimeMillis);
            EventLog.writeEvent(90204, 1, Integer.valueOf((int) jCurrentTimeMillis));
        } else {
            this.mNoResultsView.setVisibility(8);
            this.mResultsRecyclerView.scrollToPosition(0);
            this.mMetricsFeatureProvider.logEvent(11, jCurrentTimeMillis);
        }
        this.mSearchFeatureProvider.showFeedbackButton(this, getView());
    }

    public void onSavedQueryClicked(SavedQueryViewHolder savedQueryViewHolder, CharSequence charSequence) {
        String string = charSequence.toString();
        this.mMetricsFeatureProvider.logEvent(savedQueryViewHolder.getClickActionMetricName());
        this.mSearchView.setQuery(string, false);
        onQueryTextChange(string);
    }

    private void restartLoaders() {
        this.mShowingSavedQuery = false;
        getLoaderManager().restartLoader(1, null, this);
    }

    private void requery() {
        if (TextUtils.isEmpty(this.mQuery)) {
            return;
        }
        String str = this.mQuery;
        this.mQuery = "";
        onQueryTextChange(str);
    }

    public void hideKeyboard() {
        Activity activity = getActivity();
        if (activity != null) {
            ((InputMethodManager) activity.getSystemService("input_method")).hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
        if (this.mResultsRecyclerView != null) {
            this.mResultsRecyclerView.requestFocus();
        }
    }

    private void logSearchResultClicked(SearchViewHolder searchViewHolder, SearchResult searchResult) {
        this.mMetricsFeatureProvider.logSearchResultClick(searchResult, this.mQuery, searchViewHolder.getClickActionMetricName(), this.mSearchAdapter.getItemCount(), searchViewHolder.getAdapterPosition());
    }
}
