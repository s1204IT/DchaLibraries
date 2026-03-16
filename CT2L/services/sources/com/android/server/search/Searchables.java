package com.android.server.search;

import android.app.AppGlobals;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Searchables {
    private static final String LOG_TAG = "Searchables";
    private static final String MD_LABEL_DEFAULT_SEARCHABLE = "android.app.default_searchable";
    private static final String MD_SEARCHABLE_SYSTEM_SEARCH = "*";
    private Context mContext;
    private List<ResolveInfo> mGlobalSearchActivities;
    private int mUserId;
    public static String GOOGLE_SEARCH_COMPONENT_NAME = "com.android.googlesearch/.GoogleSearch";
    public static String ENHANCED_GOOGLE_SEARCH_COMPONENT_NAME = "com.google.android.providers.enhancedgooglesearch/.Launcher";
    private static final Comparator<ResolveInfo> GLOBAL_SEARCH_RANKER = new Comparator<ResolveInfo>() {
        @Override
        public int compare(ResolveInfo lhs, ResolveInfo rhs) {
            if (lhs != rhs) {
                boolean lhsSystem = Searchables.isSystemApp(lhs);
                boolean rhsSystem = Searchables.isSystemApp(rhs);
                if (lhsSystem && !rhsSystem) {
                    return -1;
                }
                if (rhsSystem && !lhsSystem) {
                    return 1;
                }
                return rhs.priority - lhs.priority;
            }
            return 0;
        }
    };
    private HashMap<ComponentName, SearchableInfo> mSearchablesMap = null;
    private ArrayList<SearchableInfo> mSearchablesList = null;
    private ArrayList<SearchableInfo> mSearchablesInGlobalSearchList = null;
    private ComponentName mCurrentGlobalSearchActivity = null;
    private ComponentName mWebSearchActivity = null;
    private final IPackageManager mPm = AppGlobals.getPackageManager();

    public Searchables(Context context, int userId) {
        this.mContext = context;
        this.mUserId = userId;
    }

    public SearchableInfo getSearchableInfo(ComponentName activity) {
        ComponentName referredActivity;
        Bundle md;
        synchronized (this) {
            SearchableInfo result = this.mSearchablesMap.get(activity);
            if (result != null) {
                return result;
            }
            try {
                ActivityInfo ai = this.mPm.getActivityInfo(activity, 128, this.mUserId);
                String refActivityName = null;
                Bundle md2 = ai.metaData;
                if (md2 != null) {
                    refActivityName = md2.getString(MD_LABEL_DEFAULT_SEARCHABLE);
                }
                if (refActivityName == null && (md = ai.applicationInfo.metaData) != null) {
                    refActivityName = md.getString(MD_LABEL_DEFAULT_SEARCHABLE);
                }
                if (refActivityName != null) {
                    if (refActivityName.equals(MD_SEARCHABLE_SYSTEM_SEARCH)) {
                        return null;
                    }
                    String pkg = activity.getPackageName();
                    if (refActivityName.charAt(0) == '.') {
                        referredActivity = new ComponentName(pkg, pkg + refActivityName);
                    } else {
                        referredActivity = new ComponentName(pkg, refActivityName);
                    }
                    synchronized (this) {
                        SearchableInfo result2 = this.mSearchablesMap.get(referredActivity);
                        if (result2 != null) {
                            this.mSearchablesMap.put(activity, result2);
                            return result2;
                        }
                    }
                }
                return null;
            } catch (RemoteException re) {
                Log.e(LOG_TAG, "Error getting activity info " + re);
                return null;
            }
        }
    }

    public void buildSearchableList() {
        SearchableInfo searchable;
        HashMap<ComponentName, SearchableInfo> newSearchablesMap = new HashMap<>();
        ArrayList<SearchableInfo> newSearchablesList = new ArrayList<>();
        ArrayList<SearchableInfo> newSearchablesInGlobalSearchList = new ArrayList<>();
        Intent intent = new Intent("android.intent.action.SEARCH");
        long ident = Binder.clearCallingIdentity();
        try {
            List<ResolveInfo> searchList = queryIntentActivities(intent, 128);
            Intent webSearchIntent = new Intent("android.intent.action.WEB_SEARCH");
            List<ResolveInfo> webSearchInfoList = queryIntentActivities(webSearchIntent, 128);
            if (searchList != null || webSearchInfoList != null) {
                int search_count = searchList == null ? 0 : searchList.size();
                int web_search_count = webSearchInfoList == null ? 0 : webSearchInfoList.size();
                int count = search_count + web_search_count;
                int ii = 0;
                while (ii < count) {
                    ResolveInfo info = ii < search_count ? searchList.get(ii) : webSearchInfoList.get(ii - search_count);
                    ActivityInfo ai = info.activityInfo;
                    if (newSearchablesMap.get(new ComponentName(ai.packageName, ai.name)) == null && (searchable = SearchableInfo.getActivityMetaData(this.mContext, ai, this.mUserId)) != null) {
                        newSearchablesList.add(searchable);
                        newSearchablesMap.put(searchable.getSearchActivity(), searchable);
                        if (searchable.shouldIncludeInGlobalSearch()) {
                            newSearchablesInGlobalSearchList.add(searchable);
                        }
                    }
                    ii++;
                }
            }
            List<ResolveInfo> newGlobalSearchActivities = findGlobalSearchActivities();
            ComponentName newGlobalSearchActivity = findGlobalSearchActivity(newGlobalSearchActivities);
            ComponentName newWebSearchActivity = findWebSearchActivity(newGlobalSearchActivity);
            synchronized (this) {
                this.mSearchablesMap = newSearchablesMap;
                this.mSearchablesList = newSearchablesList;
                this.mSearchablesInGlobalSearchList = newSearchablesInGlobalSearchList;
                this.mGlobalSearchActivities = newGlobalSearchActivities;
                this.mCurrentGlobalSearchActivity = newGlobalSearchActivity;
                this.mWebSearchActivity = newWebSearchActivity;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private List<ResolveInfo> findGlobalSearchActivities() {
        Intent intent = new Intent("android.search.action.GLOBAL_SEARCH");
        List<ResolveInfo> activities = queryIntentActivities(intent, 65536);
        if (activities != null && !activities.isEmpty()) {
            Collections.sort(activities, GLOBAL_SEARCH_RANKER);
        }
        return activities;
    }

    private ComponentName findGlobalSearchActivity(List<ResolveInfo> installed) {
        ComponentName globalSearchComponent;
        String searchProviderSetting = getGlobalSearchProviderSetting();
        return (TextUtils.isEmpty(searchProviderSetting) || (globalSearchComponent = ComponentName.unflattenFromString(searchProviderSetting)) == null || !isInstalled(globalSearchComponent)) ? getDefaultGlobalSearchProvider(installed) : globalSearchComponent;
    }

    private boolean isInstalled(ComponentName globalSearch) {
        Intent intent = new Intent("android.search.action.GLOBAL_SEARCH");
        intent.setComponent(globalSearch);
        List<ResolveInfo> activities = queryIntentActivities(intent, 65536);
        return (activities == null || activities.isEmpty()) ? false : true;
    }

    private static final boolean isSystemApp(ResolveInfo res) {
        return (res.activityInfo.applicationInfo.flags & 1) != 0;
    }

    private ComponentName getDefaultGlobalSearchProvider(List<ResolveInfo> providerList) {
        if (providerList != null && !providerList.isEmpty()) {
            ActivityInfo ai = providerList.get(0).activityInfo;
            return new ComponentName(ai.packageName, ai.name);
        }
        Log.w(LOG_TAG, "No global search activity found");
        return null;
    }

    private String getGlobalSearchProviderSetting() {
        return Settings.Secure.getString(this.mContext.getContentResolver(), "search_global_search_activity");
    }

    private ComponentName findWebSearchActivity(ComponentName globalSearchActivity) {
        if (globalSearchActivity == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.WEB_SEARCH");
        intent.setPackage(globalSearchActivity.getPackageName());
        List<ResolveInfo> activities = queryIntentActivities(intent, 65536);
        if (activities != null && !activities.isEmpty()) {
            ActivityInfo ai = activities.get(0).activityInfo;
            return new ComponentName(ai.packageName, ai.name);
        }
        Log.w(LOG_TAG, "No web search activity found");
        return null;
    }

    private List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        try {
            List<ResolveInfo> activities = this.mPm.queryIntentActivities(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), flags, this.mUserId);
            return activities;
        } catch (RemoteException e) {
            return null;
        }
    }

    public synchronized ArrayList<SearchableInfo> getSearchablesList() {
        ArrayList<SearchableInfo> result;
        result = new ArrayList<>(this.mSearchablesList);
        return result;
    }

    public synchronized ArrayList<SearchableInfo> getSearchablesInGlobalSearchList() {
        return new ArrayList<>(this.mSearchablesInGlobalSearchList);
    }

    public synchronized ArrayList<ResolveInfo> getGlobalSearchActivities() {
        return new ArrayList<>(this.mGlobalSearchActivities);
    }

    public synchronized ComponentName getGlobalSearchActivity() {
        return this.mCurrentGlobalSearchActivity;
    }

    public synchronized ComponentName getWebSearchActivity() {
        return this.mWebSearchActivity;
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Searchable authorities:");
        synchronized (this) {
            if (this.mSearchablesList != null) {
                for (SearchableInfo info : this.mSearchablesList) {
                    pw.print("  ");
                    pw.println(info.getSuggestAuthority());
                }
            }
        }
    }
}
