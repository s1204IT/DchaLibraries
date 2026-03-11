package com.android.settings.dashboard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.search.Index;
import java.util.HashMap;

public class SearchResultsSummary extends InstrumentedFragment {
    private static char ELLIPSIS = 8230;
    private ViewGroup mLayoutResults;
    private ViewGroup mLayoutSuggestions;
    private String mQuery;
    private SearchResultsAdapter mResultsAdapter;
    private ListView mResultsListView;
    private SearchView mSearchView;
    private boolean mShowResults;
    private SuggestionsAdapter mSuggestionsAdapter;
    private ListView mSuggestionsListView;
    private UpdateSearchResultsTask mUpdateSearchResultsTask;
    private UpdateSuggestionsTask mUpdateSuggestionsTask;

    private class UpdateSearchResultsTask extends AsyncTask<String, Void, Cursor> {
        UpdateSearchResultsTask(SearchResultsSummary this$0, UpdateSearchResultsTask updateSearchResultsTask) {
            this();
        }

        private UpdateSearchResultsTask() {
        }

        @Override
        public Cursor doInBackground(String... params) {
            return Index.getInstance(SearchResultsSummary.this.getActivity()).search(params[0]);
        }

        @Override
        public void onPostExecute(Cursor cursor) {
            if (!isCancelled()) {
                MetricsLogger.action(SearchResultsSummary.this.getContext(), 226, cursor.getCount());
                SearchResultsSummary.this.setResultsCursor(cursor);
                SearchResultsSummary.this.setResultsVisibility(cursor.getCount() > 0);
            } else {
                if (cursor == null) {
                    return;
                }
                cursor.close();
            }
        }
    }

    private class UpdateSuggestionsTask extends AsyncTask<String, Void, Cursor> {
        UpdateSuggestionsTask(SearchResultsSummary this$0, UpdateSuggestionsTask updateSuggestionsTask) {
            this();
        }

        private UpdateSuggestionsTask() {
        }

        @Override
        public Cursor doInBackground(String... params) {
            return Index.getInstance(SearchResultsSummary.this.getActivity()).getSuggestions(params[0]);
        }

        @Override
        public void onPostExecute(Cursor cursor) {
            if (!isCancelled()) {
                SearchResultsSummary.this.setSuggestionsCursor(cursor);
                SearchResultsSummary.this.setSuggestionsVisibility(cursor.getCount() > 0);
            } else {
                if (cursor == null) {
                    return;
                }
                cursor.close();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mResultsAdapter = new SearchResultsAdapter(getActivity());
        this.mSuggestionsAdapter = new SuggestionsAdapter(getActivity());
        if (savedInstanceState == null) {
            return;
        }
        this.mShowResults = savedInstanceState.getBoolean(":settings:show_results");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(":settings:show_results", this.mShowResults);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        clearSuggestions();
        clearResults();
        this.mResultsListView = null;
        this.mResultsAdapter = null;
        this.mUpdateSearchResultsTask = null;
        this.mSuggestionsListView = null;
        this.mSuggestionsAdapter = null;
        this.mUpdateSuggestionsTask = null;
        this.mSearchView = null;
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_panel, container, false);
        this.mLayoutSuggestions = (ViewGroup) view.findViewById(R.id.layout_suggestions);
        this.mLayoutResults = (ViewGroup) view.findViewById(R.id.layout_results);
        this.mResultsListView = (ListView) view.findViewById(R.id.list_results);
        this.mResultsListView.setAdapter((ListAdapter) this.mResultsAdapter);
        this.mResultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view2, int position, long id) {
                int position2 = position - 1;
                if (position2 < 0) {
                    return;
                }
                Cursor cursor = SearchResultsSummary.this.mResultsAdapter.mCursor;
                cursor.moveToPosition(position2);
                String className = cursor.getString(6);
                String screenTitle = cursor.getString(7);
                String action = cursor.getString(9);
                String key = cursor.getString(13);
                SettingsActivity sa = (SettingsActivity) SearchResultsSummary.this.getActivity();
                sa.needToRevertToInitialFragment();
                if (TextUtils.isEmpty(action)) {
                    Bundle args = new Bundle();
                    args.putString(":settings:fragment_args_key", key);
                    Utils.startWithFragment(sa, className, args, null, 0, -1, screenTitle);
                } else {
                    Intent intent = new Intent(action);
                    String targetPackage = cursor.getString(10);
                    String targetClass = cursor.getString(11);
                    if (!TextUtils.isEmpty(targetPackage) && !TextUtils.isEmpty(targetClass)) {
                        ComponentName component = new ComponentName(targetPackage, targetClass);
                        intent.setComponent(component);
                    }
                    intent.putExtra(":settings:fragment_args_key", key);
                    sa.startActivity(intent);
                }
                SearchResultsSummary.this.saveQueryToDatabase();
            }
        });
        this.mResultsListView.addHeaderView(LayoutInflater.from(getActivity()).inflate(R.layout.search_panel_results_header, (ViewGroup) this.mResultsListView, false), null, false);
        this.mSuggestionsListView = (ListView) view.findViewById(R.id.list_suggestions);
        this.mSuggestionsListView.setAdapter((ListAdapter) this.mSuggestionsAdapter);
        this.mSuggestionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view2, int position, long id) {
                int position2 = position - 1;
                if (position2 < 0) {
                    return;
                }
                Cursor cursor = SearchResultsSummary.this.mSuggestionsAdapter.mCursor;
                cursor.moveToPosition(position2);
                SearchResultsSummary.this.mShowResults = true;
                SearchResultsSummary.this.mQuery = cursor.getString(0);
                SearchResultsSummary.this.mSearchView.setQuery(SearchResultsSummary.this.mQuery, false);
            }
        });
        this.mSuggestionsListView.addHeaderView(LayoutInflater.from(getActivity()).inflate(R.layout.search_panel_suggestions_header, (ViewGroup) this.mSuggestionsListView, false), null, false);
        return view;
    }

    @Override
    protected int getMetricsCategory() {
        return 34;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mShowResults) {
            return;
        }
        showSomeSuggestions();
    }

    public void setSearchView(SearchView searchView) {
        this.mSearchView = searchView;
    }

    public void setSuggestionsVisibility(boolean visible) {
        if (this.mLayoutSuggestions == null) {
            return;
        }
        this.mLayoutSuggestions.setVisibility(visible ? 0 : 8);
    }

    public void setResultsVisibility(boolean visible) {
        if (this.mLayoutResults == null) {
            return;
        }
        this.mLayoutResults.setVisibility(visible ? 0 : 8);
    }

    public void saveQueryToDatabase() {
        Index.getInstance(getActivity()).addSavedQuery(this.mQuery);
    }

    public boolean onQueryTextSubmit(String query) {
        this.mQuery = getFilteredQueryString(query);
        this.mShowResults = true;
        setSuggestionsVisibility(false);
        updateSearchResults();
        saveQueryToDatabase();
        return false;
    }

    public boolean onQueryTextChange(String query) {
        String newQuery = getFilteredQueryString(query);
        this.mQuery = newQuery;
        if (TextUtils.isEmpty(this.mQuery)) {
            this.mShowResults = false;
            setResultsVisibility(false);
            updateSuggestions();
        } else {
            this.mShowResults = true;
            setSuggestionsVisibility(false);
            updateSearchResults();
        }
        return true;
    }

    public void showSomeSuggestions() {
        setResultsVisibility(false);
        this.mQuery = "";
        updateSuggestions();
    }

    private void clearSuggestions() {
        if (this.mUpdateSuggestionsTask != null) {
            this.mUpdateSuggestionsTask.cancel(false);
            this.mUpdateSuggestionsTask = null;
        }
        setSuggestionsCursor(null);
    }

    public void setSuggestionsCursor(Cursor cursor) {
        Cursor oldCursor;
        if (this.mSuggestionsAdapter == null || (oldCursor = this.mSuggestionsAdapter.swapCursor(cursor)) == null) {
            return;
        }
        oldCursor.close();
    }

    private void clearResults() {
        if (this.mUpdateSearchResultsTask != null) {
            this.mUpdateSearchResultsTask.cancel(false);
            this.mUpdateSearchResultsTask = null;
        }
        setResultsCursor(null);
    }

    public void setResultsCursor(Cursor cursor) {
        Cursor oldCursor;
        if (this.mResultsAdapter == null || (oldCursor = this.mResultsAdapter.swapCursor(cursor)) == null) {
            return;
        }
        oldCursor.close();
    }

    private String getFilteredQueryString(CharSequence query) {
        if (query == null) {
            return null;
        }
        StringBuilder filtered = new StringBuilder();
        for (int n = 0; n < query.length(); n++) {
            char c = query.charAt(n);
            if (Character.isLetterOrDigit(c) || Character.isSpaceChar(c)) {
                filtered.append(c);
            }
        }
        return filtered.toString();
    }

    private void clearAllTasks() {
        if (this.mUpdateSearchResultsTask != null) {
            this.mUpdateSearchResultsTask.cancel(false);
            this.mUpdateSearchResultsTask = null;
        }
        if (this.mUpdateSuggestionsTask == null) {
            return;
        }
        this.mUpdateSuggestionsTask.cancel(false);
        this.mUpdateSuggestionsTask = null;
    }

    private void updateSuggestions() {
        UpdateSuggestionsTask updateSuggestionsTask = null;
        clearAllTasks();
        if (this.mQuery == null) {
            setSuggestionsCursor(null);
        } else {
            this.mUpdateSuggestionsTask = new UpdateSuggestionsTask(this, updateSuggestionsTask);
            this.mUpdateSuggestionsTask.execute(this.mQuery);
        }
    }

    private void updateSearchResults() {
        UpdateSearchResultsTask updateSearchResultsTask = null;
        clearAllTasks();
        if (TextUtils.isEmpty(this.mQuery)) {
            setResultsVisibility(false);
            setResultsCursor(null);
        } else {
            this.mUpdateSearchResultsTask = new UpdateSearchResultsTask(this, updateSearchResultsTask);
            this.mUpdateSearchResultsTask.execute(this.mQuery);
        }
    }

    private static class SuggestionItem {
        public String query;

        public SuggestionItem(String query) {
            this.query = query;
        }
    }

    private static class SuggestionsAdapter extends BaseAdapter {
        private Context mContext;
        private Cursor mCursor;
        private boolean mDataValid;
        private LayoutInflater mInflater;

        public SuggestionsAdapter(Context context) {
            this.mDataValid = false;
            this.mContext = context;
            this.mInflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
            this.mDataValid = false;
        }

        public Cursor swapCursor(Cursor newCursor) {
            if (newCursor == this.mCursor) {
                return null;
            }
            Cursor oldCursor = this.mCursor;
            this.mCursor = newCursor;
            if (newCursor != null) {
                this.mDataValid = true;
                notifyDataSetChanged();
            } else {
                this.mDataValid = false;
                notifyDataSetInvalidated();
            }
            return oldCursor;
        }

        @Override
        public int getCount() {
            if (!this.mDataValid || this.mCursor == null || this.mCursor.isClosed()) {
                return 0;
            }
            return this.mCursor.getCount();
        }

        @Override
        public Object getItem(int position) {
            if (this.mDataValid && this.mCursor.moveToPosition(position)) {
                String query = this.mCursor.getString(0);
                return new SuggestionItem(query);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0L;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (!this.mDataValid && convertView == null) {
                throw new IllegalStateException("this should only be called when the cursor is valid");
            }
            if (!this.mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
            if (convertView == null) {
                view = this.mInflater.inflate(R.layout.search_suggestion_item, parent, false);
            } else {
                view = convertView;
            }
            TextView query = (TextView) view.findViewById(R.id.title);
            SuggestionItem item = (SuggestionItem) getItem(position);
            query.setText(item.query);
            return view;
        }
    }

    private static class SearchResult {
        public Context context;
        public String entries;
        public int iconResId;
        public String key;
        public String summaryOff;
        public String summaryOn;
        public String title;

        public SearchResult(Context context, String title, String summaryOn, String summaryOff, String entries, int iconResId, String key) {
            this.context = context;
            this.title = title;
            this.summaryOn = summaryOn;
            this.summaryOff = summaryOff;
            this.entries = entries;
            this.iconResId = iconResId;
            this.key = key;
        }
    }

    private static class SearchResultsAdapter extends BaseAdapter {
        private Context mContext;
        private Cursor mCursor;
        private LayoutInflater mInflater;
        private HashMap<String, Context> mContextMap = new HashMap<>();
        private boolean mDataValid = false;

        public SearchResultsAdapter(Context context) {
            this.mContext = context;
            this.mInflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        }

        public Cursor swapCursor(Cursor newCursor) {
            if (newCursor == this.mCursor) {
                return null;
            }
            Cursor oldCursor = this.mCursor;
            this.mCursor = newCursor;
            if (newCursor != null) {
                this.mDataValid = true;
                notifyDataSetChanged();
            } else {
                this.mDataValid = false;
                notifyDataSetInvalidated();
            }
            return oldCursor;
        }

        @Override
        public int getCount() {
            if (!this.mDataValid || this.mCursor == null || this.mCursor.isClosed()) {
                return 0;
            }
            return this.mCursor.getCount();
        }

        @Override
        public Object getItem(int position) {
            Context packageContext;
            if (this.mDataValid && this.mCursor.moveToPosition(position)) {
                String title = this.mCursor.getString(1);
                String summaryOn = this.mCursor.getString(2);
                String summaryOff = this.mCursor.getString(3);
                String entries = this.mCursor.getString(4);
                String iconResStr = this.mCursor.getString(8);
                String className = this.mCursor.getString(6);
                String packageName = this.mCursor.getString(10);
                String key = this.mCursor.getString(13);
                if (TextUtils.isEmpty(className) && !TextUtils.isEmpty(packageName)) {
                    packageContext = this.mContextMap.get(packageName);
                    if (packageContext == null) {
                        try {
                            packageContext = this.mContext.createPackageContext(packageName, 0);
                            this.mContextMap.put(packageName, packageContext);
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.e("SearchResultsSummary", "Cannot create Context for package: " + packageName);
                            return null;
                        }
                    }
                } else {
                    packageContext = this.mContext;
                }
                int iconResId = TextUtils.isEmpty(iconResStr) ? R.drawable.empty_icon : Integer.parseInt(iconResStr);
                return new SearchResult(packageContext, title, summaryOn, summaryOff, entries, iconResId, key);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0L;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (!this.mDataValid && convertView == null) {
                throw new IllegalStateException("this should only be called when the cursor is valid");
            }
            if (!this.mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
            if (convertView == null) {
                view = this.mInflater.inflate(R.layout.search_result_item, parent, false);
            } else {
                view = convertView;
            }
            TextView textTitle = (TextView) view.findViewById(R.id.title);
            ImageView imageView = (ImageView) view.findViewById(R.id.icon);
            SearchResult result = (SearchResult) getItem(position);
            textTitle.setText(result.title);
            if (result.iconResId != R.drawable.empty_icon) {
                Context packageContext = result.context;
                try {
                    Drawable drawable = packageContext.getDrawable(result.iconResId);
                    imageView.setImageDrawable(drawable);
                } catch (Resources.NotFoundException e) {
                    Log.e("SearchResultsSummary", "Cannot load Drawable for " + result.title);
                }
            } else {
                imageView.setImageDrawable(null);
                imageView.setBackgroundResource(R.drawable.empty_icon);
            }
            return view;
        }
    }
}
