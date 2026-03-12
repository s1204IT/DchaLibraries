package com.android.settings.intelligence.search.query;

import android.content.Context;
import com.android.settings.intelligence.overlay.FeatureFactory;
import com.android.settings.intelligence.search.SearchResult;
import com.android.settings.intelligence.search.sitemap.SiteMapManager;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class SearchQueryTask extends FutureTask<List<? extends SearchResult>> {
    private final int mId;

    public SearchQueryTask(QueryWorker queryWorker) {
        super(queryWorker);
        this.mId = queryWorker.getQueryWorkerId();
    }

    public int getTaskId() {
        return this.mId;
    }

    public static abstract class QueryWorker implements Callable<List<? extends SearchResult>> {
        protected final Context mContext;
        protected final String mQuery;
        protected final SiteMapManager mSiteMapManager;

        protected abstract int getQueryWorkerId();

        protected abstract List<? extends SearchResult> query();

        public QueryWorker(Context context, SiteMapManager siteMapManager, String str) {
            this.mContext = context;
            this.mSiteMapManager = siteMapManager;
            this.mQuery = str;
        }

        @Override
        public List<? extends SearchResult> call() throws Exception {
            long jCurrentTimeMillis = System.currentTimeMillis();
            try {
                return query();
            } finally {
                FeatureFactory.get(this.mContext).metricsFeatureProvider(this.mContext).logEvent(getQueryWorkerId(), System.currentTimeMillis() - jCurrentTimeMillis);
            }
        }
    }
}
