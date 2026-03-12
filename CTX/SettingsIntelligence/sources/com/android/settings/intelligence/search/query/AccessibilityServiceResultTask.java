package com.android.settings.intelligence.search.query;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.view.accessibility.AccessibilityManager;
import com.android.settings.intelligence.R;
import com.android.settings.intelligence.search.ResultPayload;
import com.android.settings.intelligence.search.SearchResult;
import com.android.settings.intelligence.search.indexing.DatabaseIndexingUtils;
import com.android.settings.intelligence.search.query.SearchQueryTask;
import com.android.settings.intelligence.search.sitemap.SiteMapManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccessibilityServiceResultTask extends SearchQueryTask.QueryWorker {
    private final AccessibilityManager mAccessibilityManager;
    private List<String> mBreadcrumb;
    private final PackageManager mPackageManager;

    public static SearchQueryTask newTask(Context context, SiteMapManager siteMapManager, String str) {
        return new SearchQueryTask(new AccessibilityServiceResultTask(context, siteMapManager, str));
    }

    public AccessibilityServiceResultTask(Context context, SiteMapManager siteMapManager, String str) {
        super(context, siteMapManager, str);
        this.mPackageManager = context.getPackageManager();
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
    }

    @Override
    protected List<? extends SearchResult> query() {
        ArrayList arrayList = new ArrayList();
        List<AccessibilityServiceInfo> installedAccessibilityServiceList = this.mAccessibilityManager.getInstalledAccessibilityServiceList();
        String string = this.mContext.getString(R.string.accessibility_settings);
        for (AccessibilityServiceInfo accessibilityServiceInfo : installedAccessibilityServiceList) {
            if (accessibilityServiceInfo != null) {
                ResolveInfo resolveInfo = accessibilityServiceInfo.getResolveInfo();
                if (accessibilityServiceInfo.getResolveInfo() != null) {
                    ServiceInfo serviceInfo = resolveInfo.serviceInfo;
                    CharSequence charSequenceLoadLabel = resolveInfo.loadLabel(this.mPackageManager);
                    int wordDifference = SearchQueryUtils.getWordDifference(charSequenceLoadLabel.toString(), this.mQuery);
                    if (wordDifference != -1) {
                        Drawable drawableLoadIcon = serviceInfo.loadIcon(this.mPackageManager);
                        String strFlattenToString = new ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToString();
                        arrayList.add(new SearchResult.Builder().setTitle(charSequenceLoadLabel).addBreadcrumbs(getBreadCrumb()).setPayload(new ResultPayload(DatabaseIndexingUtils.buildSearchTrampolineIntent(this.mContext, "com.android.settings.accessibility.AccessibilitySettings", strFlattenToString, string))).setRank(wordDifference).setIcon(drawableLoadIcon).setDataKey(strFlattenToString).build());
                    }
                }
            }
        }
        Collections.sort(arrayList);
        return arrayList;
    }

    @Override
    protected int getQueryWorkerId() {
        return 16;
    }

    private List<String> getBreadCrumb() {
        if (this.mBreadcrumb == null || this.mBreadcrumb.isEmpty()) {
            this.mBreadcrumb = this.mSiteMapManager.buildBreadCrumb(this.mContext, "com.android.settings.accessibility.AccessibilitySettings", this.mContext.getString(R.string.accessibility_settings));
        }
        return this.mBreadcrumb;
    }
}
