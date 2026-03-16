package com.android.browser.search;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class DefaultSearchEngine implements SearchEngine {
    private final CharSequence mLabel;
    private final SearchableInfo mSearchable;

    private DefaultSearchEngine(Context context, SearchableInfo searchable) {
        this.mSearchable = searchable;
        this.mLabel = loadLabel(context, this.mSearchable.getSearchActivity());
    }

    public static DefaultSearchEngine create(Context context) {
        SearchableInfo searchable;
        SearchManager searchManager = (SearchManager) context.getSystemService("search");
        ComponentName name = searchManager.getWebSearchActivity();
        if (name == null || (searchable = searchManager.getSearchableInfo(name)) == null) {
            return null;
        }
        return new DefaultSearchEngine(context, searchable);
    }

    private CharSequence loadLabel(Context context, ComponentName activityName) {
        PackageManager pm = context.getPackageManager();
        try {
            ActivityInfo ai = pm.getActivityInfo(activityName, 0);
            return ai.loadLabel(pm);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("DefaultSearchEngine", "Web search activity not found: " + activityName);
            return null;
        }
    }

    @Override
    public String getName() {
        String packageName = this.mSearchable.getSearchActivity().getPackageName();
        if ("com.google.android.googlequicksearchbox".equals(packageName) || "com.android.quicksearchbox".equals(packageName)) {
            return "google";
        }
        return packageName;
    }

    @Override
    public CharSequence getLabel() {
        return this.mLabel;
    }

    @Override
    public void startSearch(Context context, String query, Bundle appData, String extraData) {
        try {
            Intent intent = new Intent("android.intent.action.WEB_SEARCH");
            intent.setComponent(this.mSearchable.getSearchActivity());
            intent.addCategory("android.intent.category.DEFAULT");
            intent.putExtra("query", query);
            if (appData != null) {
                intent.putExtra("app_data", appData);
            }
            if (extraData != null) {
                intent.putExtra("intent_extra_data_key", extraData);
            }
            intent.putExtra("com.android.browser.application_id", context.getPackageName());
            Intent viewIntent = new Intent("android.intent.action.VIEW");
            viewIntent.addFlags(268435456);
            viewIntent.setPackage(context.getPackageName());
            PendingIntent pending = PendingIntent.getActivity(context, 0, viewIntent, 1073741824);
            intent.putExtra("web_search_pendingintent", pending);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e("DefaultSearchEngine", "Web search activity not found: " + this.mSearchable.getSearchActivity());
        }
    }

    @Override
    public Cursor getSuggestions(Context context, String query) {
        SearchManager searchManager = (SearchManager) context.getSystemService("search");
        return searchManager.getSuggestions(this.mSearchable, query);
    }

    @Override
    public boolean supportsSuggestions() {
        return !TextUtils.isEmpty(this.mSearchable.getSuggestAuthority());
    }

    @Override
    public void close() {
    }

    public String toString() {
        return "ActivitySearchEngine{" + this.mSearchable + "}";
    }

    @Override
    public boolean wantsEmptyQuery() {
        return false;
    }
}
