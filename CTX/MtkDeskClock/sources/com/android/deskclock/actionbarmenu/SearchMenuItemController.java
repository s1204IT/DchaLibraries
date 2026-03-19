package com.android.deskclock.actionbarmenu;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.android.deskclock.R;
import com.google.android.flexbox.BuildConfig;

public final class SearchMenuItemController implements MenuItemController {
    private static final String KEY_SEARCH_MODE = "search_mode";
    private static final String KEY_SEARCH_QUERY = "search_query";
    private static final int SEARCH_MENU_RES_ID = 2131361951;
    private final Context mContext;
    private String mQuery;
    private final SearchView.OnQueryTextListener mQueryListener;
    private boolean mSearchMode;
    private final SearchModeChangeListener mSearchModeChangeListener = new SearchModeChangeListener();

    public SearchMenuItemController(Context context, SearchView.OnQueryTextListener onQueryTextListener, Bundle bundle) {
        this.mQuery = BuildConfig.FLAVOR;
        this.mContext = context;
        this.mQueryListener = onQueryTextListener;
        if (bundle != null) {
            this.mSearchMode = bundle.getBoolean(KEY_SEARCH_MODE, false);
            this.mQuery = bundle.getString(KEY_SEARCH_QUERY, BuildConfig.FLAVOR);
        }
    }

    public void saveInstance(Bundle bundle) {
        bundle.putString(KEY_SEARCH_QUERY, this.mQuery);
        bundle.putBoolean(KEY_SEARCH_MODE, this.mSearchMode);
    }

    @Override
    public int getId() {
        return R.id.menu_item_search;
    }

    @Override
    public void onCreateOptionsItem(Menu menu) {
        SearchView searchView = new SearchView(this.mContext);
        searchView.setImeOptions(268435456);
        searchView.setInputType(8193);
        searchView.setQuery(this.mQuery, false);
        searchView.setOnCloseListener(this.mSearchModeChangeListener);
        searchView.setOnSearchClickListener(this.mSearchModeChangeListener);
        searchView.setOnQueryTextListener(this.mQueryListener);
        menu.add(0, R.id.menu_item_search, 1, android.R.string.search_go).setActionView(searchView).setShowAsAction(1);
        if (this.mSearchMode) {
            searchView.requestFocus();
            searchView.setIconified(false);
        }
    }

    @Override
    public void onPrepareOptionsItem(MenuItem menuItem) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return false;
    }

    public String getQueryText() {
        return this.mQuery;
    }

    public void setQueryText(String str) {
        this.mQuery = str;
    }

    private final class SearchModeChangeListener implements View.OnClickListener, SearchView.OnCloseListener {
        private SearchModeChangeListener() {
        }

        @Override
        public void onClick(View view) {
            SearchMenuItemController.this.mSearchMode = true;
        }

        @Override
        public boolean onClose() {
            SearchMenuItemController.this.mSearchMode = false;
            return false;
        }
    }
}
