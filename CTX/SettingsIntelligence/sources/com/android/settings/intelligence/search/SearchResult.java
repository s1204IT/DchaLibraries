package com.android.settings.intelligence.search;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import java.util.List;

public class SearchResult implements Comparable<SearchResult> {
    public final List<String> breadcrumbs;
    public final String dataKey;
    public final Drawable icon;
    public final ResultPayload payload;
    public final int rank;
    public final CharSequence summary;
    public final CharSequence title;
    public final int viewType;

    protected SearchResult(Builder builder) {
        this.dataKey = builder.mDataKey;
        this.title = builder.mTitle;
        this.summary = builder.mSummary;
        this.breadcrumbs = builder.mBreadcrumbs;
        this.rank = builder.mRank;
        this.icon = builder.mIcon;
        this.payload = builder.mResultPayload;
        this.viewType = this.payload.getType();
    }

    @Override
    public int compareTo(SearchResult searchResult) {
        if (searchResult == null) {
            return -1;
        }
        return this.rank - searchResult.rank;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SearchResult)) {
            return false;
        }
        return TextUtils.equals(this.dataKey, ((SearchResult) obj).dataKey);
    }

    public int hashCode() {
        return this.dataKey.hashCode();
    }

    public static class Builder {
        private List<String> mBreadcrumbs;
        private String mDataKey;
        private Drawable mIcon;
        private int mRank = 42;
        private ResultPayload mResultPayload;
        private CharSequence mSummary;
        private CharSequence mTitle;

        public Builder setTitle(CharSequence charSequence) {
            this.mTitle = charSequence;
            return this;
        }

        public Builder setSummary(CharSequence charSequence) {
            this.mSummary = charSequence;
            return this;
        }

        public Builder addBreadcrumbs(List<String> list) {
            this.mBreadcrumbs = list;
            return this;
        }

        public Builder setRank(int i) {
            if (i >= 0 && i <= 9) {
                this.mRank = i;
            }
            return this;
        }

        public Builder setIcon(Drawable drawable) {
            this.mIcon = drawable;
            return this;
        }

        public Builder setPayload(ResultPayload resultPayload) {
            this.mResultPayload = resultPayload;
            return this;
        }

        public Builder setDataKey(String str) {
            this.mDataKey = str;
            return this;
        }

        public SearchResult build() {
            if (TextUtils.isEmpty(this.mTitle)) {
                throw new IllegalStateException("SearchResult missing title argument");
            }
            if (TextUtils.isEmpty(this.mDataKey)) {
                Log.v("SearchResult", "No data key on SearchResult with title: " + ((Object) this.mTitle));
                throw new IllegalStateException("SearchResult missing stableId argument");
            }
            if (this.mResultPayload == null) {
                throw new IllegalStateException("SearchResult missing Payload argument");
            }
            return new SearchResult(this);
        }
    }
}
