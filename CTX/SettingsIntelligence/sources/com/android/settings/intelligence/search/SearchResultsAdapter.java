package com.android.settings.intelligence.search;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.settings.intelligence.R;
import com.android.settings.intelligence.search.savedqueries.SavedQueryViewHolder;
import java.util.ArrayList;
import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchViewHolder> {
    private final SearchFragment mFragment;
    private final List<SearchResult> mSearchResults = new ArrayList();

    public SearchResultsAdapter(SearchFragment searchFragment) {
        this.mFragment = searchFragment;
        setHasStableIds(true);
    }

    @Override
    public SearchViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater layoutInflaterFrom = LayoutInflater.from(viewGroup.getContext());
        if (i == 0) {
            return new IntentSearchViewHolder(layoutInflaterFrom.inflate(R.layout.search_intent_item, viewGroup, false));
        }
        switch (i) {
            case 2:
                return new IntentSearchViewHolder(layoutInflaterFrom.inflate(R.layout.search_intent_item, viewGroup, false));
            case 3:
                return new IntentSearchViewHolder(layoutInflaterFrom.inflate(R.layout.search_intent_item, viewGroup, false));
            case 4:
                return new SavedQueryViewHolder(layoutInflaterFrom.inflate(R.layout.search_saved_query_item, viewGroup, false));
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(SearchViewHolder searchViewHolder, int i) {
        searchViewHolder.onBind(this.mFragment, this.mSearchResults.get(i));
    }

    @Override
    public long getItemId(int i) {
        return this.mSearchResults.get(i).hashCode();
    }

    @Override
    public int getItemViewType(int i) {
        return this.mSearchResults.get(i).viewType;
    }

    @Override
    public int getItemCount() {
        return this.mSearchResults.size();
    }

    public void displaySavedQuery(List<? extends SearchResult> list) {
        clearResults();
        this.mSearchResults.addAll(list);
        notifyDataSetChanged();
    }

    public void clearResults() {
        this.mSearchResults.clear();
        notifyDataSetChanged();
    }

    public void postSearchResults(List<? extends SearchResult> list) {
        DiffUtil.DiffResult diffResultCalculateDiff = DiffUtil.calculateDiff(new SearchResultDiffCallback(this.mSearchResults, list));
        this.mSearchResults.clear();
        this.mSearchResults.addAll(list);
        diffResultCalculateDiff.dispatchUpdatesTo(this);
        this.mFragment.onSearchResultsDisplayed(this.mSearchResults.size());
    }
}
