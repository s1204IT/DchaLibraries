package com.android.settings.intelligence.search.query;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import com.android.settings.intelligence.R;
import com.android.settings.intelligence.search.AppSearchResult;
import com.android.settings.intelligence.search.ResultPayload;
import com.android.settings.intelligence.search.SearchResult;
import com.android.settings.intelligence.search.query.SearchQueryTask;
import com.android.settings.intelligence.search.sitemap.SiteMapManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstalledAppResultTask extends SearchQueryTask.QueryWorker {
    private final String INTENT_SCHEME;
    private List<String> mBreadcrumb;
    private final PackageManager mPackageManager;

    public static SearchQueryTask newTask(Context context, SiteMapManager siteMapManager, String str) {
        return new SearchQueryTask(new InstalledAppResultTask(context, siteMapManager, str));
    }

    public InstalledAppResultTask(Context context, SiteMapManager siteMapManager, String str) {
        super(context, siteMapManager, str);
        this.INTENT_SCHEME = "package";
        this.mPackageManager = context.getPackageManager();
    }

    @Override
    protected int getQueryWorkerId() {
        return 14;
    }

    @Override
    protected List<? extends SearchResult> query() {
        ArrayList arrayList = new ArrayList();
        for (ApplicationInfo applicationInfo : this.mPackageManager.getInstalledApplications(8421888)) {
            int wordDifference = SearchQueryUtils.getWordDifference(applicationInfo.loadLabel(this.mPackageManager).toString(), this.mQuery);
            if (wordDifference != -1) {
                Intent intentPutExtra = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS").setAction("android.settings.APPLICATION_DETAILS_SETTINGS").setData(Uri.fromParts("package", applicationInfo.packageName, null)).putExtra(":settings:source_metrics", 34);
                AppSearchResult.Builder builder = new AppSearchResult.Builder();
                builder.setAppInfo(applicationInfo).setDataKey(applicationInfo.packageName).setTitle(applicationInfo.loadLabel(this.mPackageManager)).setRank(getRank(wordDifference)).addBreadcrumbs(getBreadCrumb()).setPayload(new ResultPayload(intentPutExtra));
                arrayList.add(builder.build());
            }
        }
        Collections.sort(arrayList);
        return arrayList;
    }

    private List<String> getBreadCrumb() {
        if (this.mBreadcrumb == null || this.mBreadcrumb.isEmpty()) {
            this.mBreadcrumb = this.mSiteMapManager.buildBreadCrumb(this.mContext, "com.android.settings.applications.ManageApplications", this.mContext.getString(R.string.applications_settings));
        }
        return this.mBreadcrumb;
    }

    private int getRank(int i) {
        if (i < 6) {
            return 2;
        }
        return 3;
    }
}
