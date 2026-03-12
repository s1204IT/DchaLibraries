package com.android.settings.intelligence.search;

import android.R;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.intelligence.overlay.FeatureFactory;

public abstract class SearchViewHolder extends RecyclerView.ViewHolder {
    private final String DYNAMIC_PLACEHOLDER;
    public final TextView breadcrumbView;
    public final ImageView iconView;
    private final String mPlaceholderSummary;
    protected final SearchFeatureProvider mSearchFeatureProvider;
    public final TextView summaryView;
    public final TextView titleView;

    public abstract int getClickActionMetricName();

    public SearchViewHolder(View view) {
        super(view);
        this.DYNAMIC_PLACEHOLDER = "%s";
        this.mSearchFeatureProvider = FeatureFactory.get(view.getContext().getApplicationContext()).searchFeatureProvider();
        this.titleView = (TextView) view.findViewById(R.id.title);
        this.summaryView = (TextView) view.findViewById(R.id.summary);
        this.iconView = (ImageView) view.findViewById(R.id.icon);
        this.breadcrumbView = (TextView) view.findViewById(com.android.settings.intelligence.R.id.breadcrumb);
        this.mPlaceholderSummary = view.getContext().getString(com.android.settings.intelligence.R.string.summary_placeholder);
    }

    public void onBind(SearchFragment searchFragment, SearchResult searchResult) {
        this.titleView.setText(searchResult.title);
        if (TextUtils.isEmpty(searchResult.summary) || TextUtils.equals(searchResult.summary, this.mPlaceholderSummary) || TextUtils.equals(searchResult.summary, "%s")) {
            this.summaryView.setVisibility(8);
        } else {
            this.summaryView.setText(searchResult.summary);
            this.summaryView.setVisibility(0);
        }
        if (searchResult instanceof AppSearchResult) {
            this.iconView.setImageDrawable(((AppSearchResult) searchResult).info.loadIcon(searchFragment.getActivity().getPackageManager()));
        } else {
            this.iconView.setImageDrawable(searchResult.icon);
        }
        bindBreadcrumbView(searchResult);
    }

    private void bindBreadcrumbView(SearchResult searchResult) {
        if (searchResult.breadcrumbs == null || searchResult.breadcrumbs.isEmpty()) {
            this.breadcrumbView.setVisibility(8);
            return;
        }
        Context context = this.breadcrumbView.getContext();
        String str = searchResult.breadcrumbs.get(0);
        int size = searchResult.breadcrumbs.size();
        String string = str;
        for (int i = 1; i < size; i++) {
            string = context.getString(com.android.settings.intelligence.R.string.search_breadcrumb_connector, string, searchResult.breadcrumbs.get(i));
        }
        this.breadcrumbView.setText(string);
        this.breadcrumbView.setVisibility(0);
    }
}
