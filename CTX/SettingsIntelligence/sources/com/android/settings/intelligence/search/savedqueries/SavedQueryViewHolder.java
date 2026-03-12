package com.android.settings.intelligence.search.savedqueries;

import android.R;
import android.view.View;
import android.widget.TextView;
import com.android.settings.intelligence.search.SearchFragment;
import com.android.settings.intelligence.search.SearchResult;
import com.android.settings.intelligence.search.SearchViewHolder;

public class SavedQueryViewHolder extends SearchViewHolder {
    public final TextView titleView;

    public SavedQueryViewHolder(View view) {
        super(view);
        this.titleView = (TextView) view.findViewById(R.id.title);
    }

    @Override
    public int getClickActionMetricName() {
        return 8;
    }

    @Override
    public void onBind(final SearchFragment searchFragment, final SearchResult searchResult) {
        this.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchFragment.onSavedQueryClicked(SavedQueryViewHolder.this, searchResult.title);
            }
        });
        this.titleView.setText(searchResult.title);
    }
}
