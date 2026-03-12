package com.android.settings.intelligence.search;

import android.content.pm.ApplicationInfo;
import com.android.settings.intelligence.search.SearchResult;

public class AppSearchResult extends SearchResult {
    public final ApplicationInfo info;

    public AppSearchResult(Builder builder) {
        super(builder);
        this.info = builder.mInfo;
    }

    public static class Builder extends SearchResult.Builder {
        protected ApplicationInfo mInfo;

        public SearchResult.Builder setAppInfo(ApplicationInfo applicationInfo) {
            this.mInfo = applicationInfo;
            return this;
        }

        @Override
        public AppSearchResult build() {
            return new AppSearchResult(this);
        }
    }
}
