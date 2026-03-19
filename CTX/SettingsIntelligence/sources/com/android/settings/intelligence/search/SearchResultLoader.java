package com.android.settings.intelligence.search;

import android.content.Context;
import com.android.settings.intelligence.utils.AsyncLoader;
import java.util.List;

public class SearchResultLoader extends AsyncLoader<List<? extends SearchResult>> {
    private final String mQuery;

    public SearchResultLoader(Context context, String str) {
        super(context);
        this.mQuery = str;
    }

    @Override
    public List<? extends SearchResult> loadInBackground() {
        return SearchResultAggregator.getInstance().fetchResults(getContext(), this.mQuery);
    }

    @Override
    protected void onDiscardResult(List<? extends SearchResult> list) {
    }
}
