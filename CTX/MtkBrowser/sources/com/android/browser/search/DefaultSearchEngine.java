package com.android.browser.search;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class DefaultSearchEngine implements SearchEngine {
    private final CharSequence mLabel;
    private final SearchableInfo mSearchable;

    private DefaultSearchEngine(Context context, SearchableInfo searchableInfo) {
        this.mSearchable = searchableInfo;
        this.mLabel = loadLabel(context, this.mSearchable.getSearchActivity());
    }

    public static DefaultSearchEngine create(Context context) {
        SearchableInfo searchableInfo;
        SearchManager searchManager = (SearchManager) context.getSystemService("search");
        ComponentName webSearchActivity = searchManager.getWebSearchActivity();
        if (webSearchActivity != null && (searchableInfo = searchManager.getSearchableInfo(webSearchActivity)) != null) {
            return new DefaultSearchEngine(context, searchableInfo);
        }
        return null;
    }

    private CharSequence loadLabel(Context context, ComponentName componentName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            return packageManager.getActivityInfo(componentName, 0).loadLabel(packageManager);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("DefaultSearchEngine", "Web search activity not found: " + componentName);
            return null;
        }
    }

    @Override
    public String getName() {
        String packageName = this.mSearchable.getSearchActivity().getPackageName();
        return ("com.google.android.googlequicksearchbox".equals(packageName) || "com.android.quicksearchbox".equals(packageName)) ? "google" : packageName;
    }

    @Override
    public Cursor getSuggestions(Context context, String str) {
        return ((SearchManager) context.getSystemService("search")).getSuggestions(this.mSearchable, str);
    }

    @Override
    public void startSearch(Context context, String str, Bundle bundle, String str2) {
        try {
            Intent intent = new Intent("android.intent.action.WEB_SEARCH");
            intent.setComponent(this.mSearchable.getSearchActivity());
            intent.addCategory("android.intent.category.DEFAULT");
            intent.putExtra("query", str);
            if (bundle != null) {
                intent.putExtra("app_data", bundle);
            }
            if (str2 != null) {
                intent.putExtra("intent_extra_data_key", str2);
            }
            intent.putExtra("com.android.browser.application_id", context.getPackageName());
            Intent intent2 = new Intent("android.intent.action.VIEW");
            intent2.addFlags(268435456);
            intent2.setPackage(context.getPackageName());
            intent.putExtra("web_search_pendingintent", PendingIntent.getActivity(context, 0, intent2, 1073741824));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e("DefaultSearchEngine", "Web search activity not found: " + this.mSearchable.getSearchActivity());
        }
    }

    @Override
    public boolean supportsSuggestions() {
        return !TextUtils.isEmpty(this.mSearchable.getSuggestAuthority());
    }

    public String toString() {
        return "ActivitySearchEngine{" + this.mSearchable + "}";
    }

    @Override
    public boolean wantsEmptyQuery() {
        return false;
    }
}
