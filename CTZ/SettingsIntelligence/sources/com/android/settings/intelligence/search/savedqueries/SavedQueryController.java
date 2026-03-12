package com.android.settings.intelligence.search.savedqueries;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.android.settings.intelligence.R;
import com.android.settings.intelligence.overlay.FeatureFactory;
import com.android.settings.intelligence.search.SearchFeatureProvider;
import com.android.settings.intelligence.search.SearchResultsAdapter;
import java.util.List;

public class SavedQueryController implements LoaderManager.LoaderCallbacks, MenuItem.OnMenuItemClickListener {
    private final Context mContext;
    private final LoaderManager mLoaderManager;
    private final SearchResultsAdapter mResultAdapter;
    private final SearchFeatureProvider mSearchFeatureProvider;

    public SavedQueryController(Context context, LoaderManager loaderManager, SearchResultsAdapter searchResultsAdapter) {
        this.mContext = context;
        this.mLoaderManager = loaderManager;
        this.mResultAdapter = searchResultsAdapter;
        this.mSearchFeatureProvider = FeatureFactory.get(context).searchFeatureProvider();
    }

    @Override
    public Loader onCreateLoader(int i, Bundle bundle) {
        switch (i) {
            case 2:
                return new SavedQueryRecorder(this.mContext, bundle.getString("remove_query"));
            case 3:
                return new SavedQueryRemover(this.mContext);
            case 4:
                return this.mSearchFeatureProvider.getSavedQueryLoader(this.mContext);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object obj) {
        switch (loader.getId()) {
            case 3:
                this.mLoaderManager.restartLoader(4, null, this);
                break;
            case 4:
                this.mResultAdapter.displaySavedQuery((List) obj);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (menuItem.getItemId() != 1000) {
            return false;
        }
        removeQueries();
        return true;
    }

    public void buildMenuItem(Menu menu) {
        menu.add(0, 1000, 0, R.string.search_clear_history).setOnMenuItemClickListener(this);
    }

    public void saveQuery(String str) {
        Bundle bundle = new Bundle();
        bundle.putString("remove_query", str);
        this.mLoaderManager.restartLoader(2, bundle, this);
    }

    public void removeQueries() {
        this.mLoaderManager.restartLoader(3, new Bundle(), this);
    }

    public void loadSavedQueries() {
        this.mLoaderManager.restartLoader(4, null, this);
    }
}
