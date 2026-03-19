package com.android.server.search;

import android.app.AppGlobals;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ResolveInfo;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.LocalServices;
import com.android.server.pm.DumpState;
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
        public int compare(ResolveInfo resolveInfo, ResolveInfo resolveInfo2) {
            if (resolveInfo != resolveInfo2) {
                boolean zIsSystemApp = Searchables.isSystemApp(resolveInfo);
                boolean zIsSystemApp2 = Searchables.isSystemApp(resolveInfo2);
                if (zIsSystemApp && !zIsSystemApp2) {
                    return -1;
                }
                if (zIsSystemApp2 && !zIsSystemApp) {
                    return 1;
                }
                return resolveInfo2.priority - resolveInfo.priority;
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

    public Searchables(Context context, int i) {
        this.mContext = context;
        this.mUserId = i;
    }

    public SearchableInfo getSearchableInfo(ComponentName componentName) {
        String string;
        ComponentName componentName2;
        SearchableInfo searchableInfo;
        Bundle bundle;
        synchronized (this) {
            SearchableInfo searchableInfo2 = this.mSearchablesMap.get(componentName);
            if (searchableInfo2 != null) {
                if (((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).canAccessComponent(Binder.getCallingUid(), searchableInfo2.getSearchActivity(), UserHandle.getCallingUserId())) {
                    return searchableInfo2;
                }
                return null;
            }
            try {
                ActivityInfo activityInfo = this.mPm.getActivityInfo(componentName, 128, this.mUserId);
                Bundle bundle2 = activityInfo.metaData;
                if (bundle2 != null) {
                    string = bundle2.getString(MD_LABEL_DEFAULT_SEARCHABLE);
                } else {
                    string = null;
                }
                if (string == null && (bundle = activityInfo.applicationInfo.metaData) != null) {
                    string = bundle.getString(MD_LABEL_DEFAULT_SEARCHABLE);
                }
                if (string == null || string.equals(MD_SEARCHABLE_SYSTEM_SEARCH)) {
                    return null;
                }
                String packageName = componentName.getPackageName();
                if (string.charAt(0) == '.') {
                    componentName2 = new ComponentName(packageName, packageName + string);
                } else {
                    componentName2 = new ComponentName(packageName, string);
                }
                synchronized (this) {
                    searchableInfo = this.mSearchablesMap.get(componentName2);
                    if (searchableInfo != null) {
                        this.mSearchablesMap.put(componentName, searchableInfo);
                    }
                }
                if (searchableInfo == null || !((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).canAccessComponent(Binder.getCallingUid(), searchableInfo.getSearchActivity(), UserHandle.getCallingUserId())) {
                    return null;
                }
                return searchableInfo;
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Error getting activity info " + e);
                return null;
            }
        }
    }

    public void updateSearchableList() {
        int size;
        int size2;
        ResolveInfo resolveInfo;
        SearchableInfo activityMetaData;
        HashMap<ComponentName, SearchableInfo> map = new HashMap<>();
        ArrayList<SearchableInfo> arrayList = new ArrayList<>();
        ArrayList<SearchableInfo> arrayList2 = new ArrayList<>();
        Intent intent = new Intent("android.intent.action.SEARCH");
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            List<ResolveInfo> listQueryIntentActivities = queryIntentActivities(intent, 268435584);
            List<ResolveInfo> listQueryIntentActivities2 = queryIntentActivities(new Intent("android.intent.action.WEB_SEARCH"), 268435584);
            if (BenesseExtension.getDchaState() != 0) {
                listQueryIntentActivities2 = null;
            }
            if (listQueryIntentActivities != null || listQueryIntentActivities2 != null) {
                if (listQueryIntentActivities != null) {
                    size = listQueryIntentActivities.size();
                } else {
                    size = 0;
                }
                if (listQueryIntentActivities2 != null) {
                    size2 = listQueryIntentActivities2.size();
                } else {
                    size2 = 0;
                }
                int i = size2 + size;
                for (int i2 = 0; i2 < i; i2++) {
                    if (i2 < size) {
                        resolveInfo = listQueryIntentActivities.get(i2);
                    } else {
                        resolveInfo = listQueryIntentActivities2.get(i2 - size);
                    }
                    ActivityInfo activityInfo = resolveInfo.activityInfo;
                    if (map.get(new ComponentName(activityInfo.packageName, activityInfo.name)) == null && (activityMetaData = SearchableInfo.getActivityMetaData(this.mContext, activityInfo, this.mUserId)) != null) {
                        arrayList.add(activityMetaData);
                        map.put(activityMetaData.getSearchActivity(), activityMetaData);
                        if (activityMetaData.shouldIncludeInGlobalSearch()) {
                            arrayList2.add(activityMetaData);
                        }
                    }
                }
            }
            List<ResolveInfo> listFindGlobalSearchActivities = findGlobalSearchActivities();
            ComponentName componentNameFindGlobalSearchActivity = findGlobalSearchActivity(listFindGlobalSearchActivities);
            ComponentName componentNameFindWebSearchActivity = findWebSearchActivity(componentNameFindGlobalSearchActivity);
            synchronized (this) {
                this.mSearchablesMap = map;
                this.mSearchablesList = arrayList;
                this.mSearchablesInGlobalSearchList = arrayList2;
                this.mGlobalSearchActivities = listFindGlobalSearchActivities;
                this.mCurrentGlobalSearchActivity = componentNameFindGlobalSearchActivity;
                this.mWebSearchActivity = componentNameFindWebSearchActivity;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private List<ResolveInfo> findGlobalSearchActivities() {
        if (BenesseExtension.getDchaState() != 0) {
            return null;
        }
        List<ResolveInfo> listQueryIntentActivities = queryIntentActivities(new Intent("android.search.action.GLOBAL_SEARCH"), 268500992);
        if (listQueryIntentActivities != null && !listQueryIntentActivities.isEmpty()) {
            Collections.sort(listQueryIntentActivities, GLOBAL_SEARCH_RANKER);
        }
        return listQueryIntentActivities;
    }

    private ComponentName findGlobalSearchActivity(List<ResolveInfo> list) {
        ComponentName componentNameUnflattenFromString;
        String globalSearchProviderSetting = getGlobalSearchProviderSetting();
        if (!TextUtils.isEmpty(globalSearchProviderSetting) && (componentNameUnflattenFromString = ComponentName.unflattenFromString(globalSearchProviderSetting)) != null && isInstalled(componentNameUnflattenFromString)) {
            return componentNameUnflattenFromString;
        }
        return getDefaultGlobalSearchProvider(list);
    }

    private boolean isInstalled(ComponentName componentName) {
        if (BenesseExtension.getDchaState() != 0) {
            return false;
        }
        Intent intent = new Intent("android.search.action.GLOBAL_SEARCH");
        intent.setComponent(componentName);
        List<ResolveInfo> listQueryIntentActivities = queryIntentActivities(intent, 65536);
        return (listQueryIntentActivities == null || listQueryIntentActivities.isEmpty()) ? false : true;
    }

    private static final boolean isSystemApp(ResolveInfo resolveInfo) {
        return (resolveInfo.activityInfo.applicationInfo.flags & 1) != 0;
    }

    private ComponentName getDefaultGlobalSearchProvider(List<ResolveInfo> list) {
        if (list != null && !list.isEmpty()) {
            ActivityInfo activityInfo = list.get(0).activityInfo;
            return new ComponentName(activityInfo.packageName, activityInfo.name);
        }
        Log.w(LOG_TAG, "No global search activity found");
        return null;
    }

    private String getGlobalSearchProviderSetting() {
        return Settings.Secure.getString(this.mContext.getContentResolver(), "search_global_search_activity");
    }

    private ComponentName findWebSearchActivity(ComponentName componentName) {
        if (BenesseExtension.getDchaState() != 0 || componentName == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.WEB_SEARCH");
        intent.setPackage(componentName.getPackageName());
        List<ResolveInfo> listQueryIntentActivities = queryIntentActivities(intent, 65536);
        if (listQueryIntentActivities != null && !listQueryIntentActivities.isEmpty()) {
            ActivityInfo activityInfo = listQueryIntentActivities.get(0).activityInfo;
            return new ComponentName(activityInfo.packageName, activityInfo.name);
        }
        Log.w(LOG_TAG, "No web search activity found");
        return null;
    }

    private List<ResolveInfo> queryIntentActivities(Intent intent, int i) {
        try {
            return this.mPm.queryIntentActivities(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), i | DumpState.DUMP_VOLUMES, this.mUserId).getList();
        } catch (RemoteException e) {
            return null;
        }
    }

    public synchronized ArrayList<SearchableInfo> getSearchablesList() {
        return createFilterdSearchableInfoList(this.mSearchablesList);
    }

    public synchronized ArrayList<SearchableInfo> getSearchablesInGlobalSearchList() {
        return createFilterdSearchableInfoList(this.mSearchablesInGlobalSearchList);
    }

    public synchronized ArrayList<ResolveInfo> getGlobalSearchActivities() {
        return createFilterdResolveInfoList(this.mGlobalSearchActivities);
    }

    private ArrayList<SearchableInfo> createFilterdSearchableInfoList(List<SearchableInfo> list) {
        if (list == null) {
            return null;
        }
        ArrayList<SearchableInfo> arrayList = new ArrayList<>(list.size());
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        for (SearchableInfo searchableInfo : list) {
            if (packageManagerInternal.canAccessComponent(callingUid, searchableInfo.getSearchActivity(), callingUserId)) {
                arrayList.add(searchableInfo);
            }
        }
        return arrayList;
    }

    private ArrayList<ResolveInfo> createFilterdResolveInfoList(List<ResolveInfo> list) {
        if (list == null) {
            return null;
        }
        ArrayList<ResolveInfo> arrayList = new ArrayList<>(list.size());
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        for (ResolveInfo resolveInfo : list) {
            if (packageManagerInternal.canAccessComponent(callingUid, resolveInfo.activityInfo.getComponentName(), callingUserId)) {
                arrayList.add(resolveInfo);
            }
        }
        return arrayList;
    }

    public synchronized ComponentName getGlobalSearchActivity() {
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        if (this.mCurrentGlobalSearchActivity != null && packageManagerInternal.canAccessComponent(callingUid, this.mCurrentGlobalSearchActivity, callingUserId)) {
            return this.mCurrentGlobalSearchActivity;
        }
        return null;
    }

    public synchronized ComponentName getWebSearchActivity() {
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        if (this.mWebSearchActivity != null && packageManagerInternal.canAccessComponent(callingUid, this.mWebSearchActivity, callingUserId)) {
            return this.mWebSearchActivity;
        }
        return null;
    }

    void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("Searchable authorities:");
        synchronized (this) {
            if (this.mSearchablesList != null) {
                for (SearchableInfo searchableInfo : this.mSearchablesList) {
                    printWriter.print("  ");
                    printWriter.println(searchableInfo.getSuggestAuthority());
                }
            }
        }
    }
}
