package com.android.launcher3.model;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.BenesseExtension;
import android.os.DeadObjectException;
import android.os.TransactionTooLargeException;
import android.util.Log;
import com.android.launcher3.AppFilter;
import com.android.launcher3.IconCache;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.AlphabeticIndexCompat;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.UserHandleCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class WidgetsModel {
    private final AppFilter mAppFilter;
    private final Comparator<ItemInfo> mAppNameComparator;
    private final AppWidgetManagerCompat mAppWidgetMgr;
    private final IconCache mIconCache;
    private final AlphabeticIndexCompat mIndexer;
    private final ArrayList<PackageItemInfo> mPackageItemInfos;
    private ArrayList<Object> mRawList;
    private final WidgetsAndShortcutNameComparator mWidgetAndShortcutNameComparator;
    private final HashMap<PackageItemInfo, ArrayList<Object>> mWidgetsList;

    public WidgetsModel(Context context, IconCache iconCache, AppFilter appFilter) {
        this.mAppWidgetMgr = AppWidgetManagerCompat.getInstance(context);
        this.mWidgetAndShortcutNameComparator = new WidgetsAndShortcutNameComparator(context);
        this.mAppNameComparator = new AppNameComparator(context).getAppInfoComparator();
        this.mIconCache = iconCache;
        this.mAppFilter = appFilter;
        this.mIndexer = new AlphabeticIndexCompat(context);
        this.mPackageItemInfos = new ArrayList<>();
        this.mWidgetsList = new HashMap<>();
        this.mRawList = new ArrayList<>();
    }

    private WidgetsModel(WidgetsModel model) {
        this.mAppWidgetMgr = model.mAppWidgetMgr;
        this.mPackageItemInfos = (ArrayList) model.mPackageItemInfos.clone();
        this.mWidgetsList = (HashMap) model.mWidgetsList.clone();
        this.mWidgetAndShortcutNameComparator = model.mWidgetAndShortcutNameComparator;
        this.mAppNameComparator = model.mAppNameComparator;
        this.mIconCache = model.mIconCache;
        this.mAppFilter = model.mAppFilter;
        this.mIndexer = model.mIndexer;
        this.mRawList = (ArrayList) model.mRawList.clone();
    }

    public int getPackageSize() {
        return this.mPackageItemInfos.size();
    }

    public PackageItemInfo getPackageItemInfo(int pos) {
        if (pos >= this.mPackageItemInfos.size() || pos < 0) {
            return null;
        }
        return this.mPackageItemInfos.get(pos);
    }

    public List<Object> getSortedWidgets(int pos) {
        return this.mWidgetsList.get(this.mPackageItemInfos.get(pos));
    }

    public ArrayList<Object> getRawList() {
        return this.mRawList;
    }

    public boolean isEmpty() {
        return this.mRawList.isEmpty();
    }

    public WidgetsModel updateAndClone(Context context) {
        Utilities.assertWorkerThread();
        try {
            ArrayList<Object> widgetsAndShortcuts = new ArrayList<>();
            for (AppWidgetProviderInfo widgetInfo : AppWidgetManagerCompat.getInstance(context).getAllProviders()) {
                widgetsAndShortcuts.add(LauncherAppWidgetProviderInfo.fromProviderInfo(context, widgetInfo));
            }
            if (BenesseExtension.getDchaState() == 0) {
                widgetsAndShortcuts.addAll(context.getPackageManager().queryIntentActivities(new Intent("android.intent.action.CREATE_SHORTCUT"), 0));
            }
            setWidgetsAndShortcuts(widgetsAndShortcuts);
        } catch (Exception e) {
            if (LauncherAppState.isDogfoodBuild() || (!(e.getCause() instanceof TransactionTooLargeException) && !(e.getCause() instanceof DeadObjectException))) {
                throw e;
            }
        }
        return m133clone();
    }

    private void setWidgetsAndShortcuts(ArrayList<Object> rawWidgetsShortcuts) {
        this.mRawList = rawWidgetsShortcuts;
        HashMap<String, PackageItemInfo> tmpPackageItemInfos = new HashMap<>();
        this.mWidgetsList.clear();
        this.mPackageItemInfos.clear();
        this.mWidgetAndShortcutNameComparator.reset();
        InvariantDeviceProfile idp = LauncherAppState.getInstance().getInvariantDeviceProfile();
        for (Object o : rawWidgetsShortcuts) {
            String packageName = "";
            UserHandleCompat userHandle = null;
            ComponentName componentName = null;
            if (o instanceof LauncherAppWidgetProviderInfo) {
                LauncherAppWidgetProviderInfo widgetInfo = (LauncherAppWidgetProviderInfo) o;
                int minSpanX = Math.min(widgetInfo.spanX, widgetInfo.minSpanX);
                int minSpanY = Math.min(widgetInfo.spanY, widgetInfo.minSpanY);
                if (minSpanX <= idp.numColumns && minSpanY <= idp.numRows) {
                    componentName = widgetInfo.provider;
                    packageName = widgetInfo.provider.getPackageName();
                    userHandle = this.mAppWidgetMgr.getUser(widgetInfo);
                }
            } else if (o instanceof ResolveInfo) {
                ResolveInfo resolveInfo = (ResolveInfo) o;
                componentName = new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                packageName = resolveInfo.activityInfo.packageName;
                userHandle = UserHandleCompat.myUserHandle();
            }
            if (!packageName.startsWith("com.android.deskclock") && !packageName.startsWith("com.android.email") && !packageName.startsWith("com.android.calendar") && !packageName.startsWith("com.android.gallery3d") && !packageName.startsWith("com.android.browser") && !packageName.startsWith("com.android.music") && !packageName.startsWith("com.android.quicksearchbox") && !packageName.startsWith("com.android.settings") && !packageName.startsWith("com.android.contacts")) {
                if (componentName == null || userHandle == null) {
                    Log.e("WidgetsModel", String.format("Widget cannot be set for %s.", o.getClass().toString()));
                } else if (this.mAppFilter == null || this.mAppFilter.shouldShowApp(componentName)) {
                    ArrayList<Object> widgetsShortcutsList = this.mWidgetsList.get(tmpPackageItemInfos.get(packageName));
                    if (widgetsShortcutsList != null) {
                        widgetsShortcutsList.add(o);
                    } else {
                        ArrayList<Object> widgetsShortcutsList2 = new ArrayList<>();
                        widgetsShortcutsList2.add(o);
                        PackageItemInfo pInfo = new PackageItemInfo(packageName);
                        this.mIconCache.getTitleAndIconForApp(packageName, userHandle, true, pInfo);
                        pInfo.titleSectionName = this.mIndexer.computeSectionName(pInfo.title);
                        this.mWidgetsList.put(pInfo, widgetsShortcutsList2);
                        tmpPackageItemInfos.put(packageName, pInfo);
                        this.mPackageItemInfos.add(pInfo);
                    }
                }
            }
        }
        Collections.sort(this.mPackageItemInfos, this.mAppNameComparator);
        for (PackageItemInfo p : this.mPackageItemInfos) {
            Collections.sort(this.mWidgetsList.get(p), this.mWidgetAndShortcutNameComparator);
        }
    }

    public WidgetsModel m133clone() {
        return new WidgetsModel(this);
    }
}
