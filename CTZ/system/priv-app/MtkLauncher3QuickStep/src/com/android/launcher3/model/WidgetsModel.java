package com.android.launcher3.model;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import com.android.launcher3.AppFilter;
import com.android.launcher3.IconCache;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.AlphabeticIndexCompat;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.ShortcutConfigActivityInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.Preconditions;
import com.android.launcher3.widget.WidgetItemComparator;
import com.android.launcher3.widget.WidgetListRowEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/* loaded from: classes.dex */
public class WidgetsModel {
    private static final boolean DEBUG = false;
    private static final String TAG = "WidgetsModel";
    private AppFilter mAppFilter;
    private final MultiHashMap<PackageItemInfo, WidgetItem> mWidgetsList = new MultiHashMap<>();

    public synchronized ArrayList<WidgetListRowEntry> getWidgetsList(Context context) {
        ArrayList<WidgetListRowEntry> arrayList;
        arrayList = new ArrayList<>();
        AlphabeticIndexCompat alphabeticIndexCompat = new AlphabeticIndexCompat(context);
        WidgetItemComparator widgetItemComparator = new WidgetItemComparator();
        for (Map.Entry<PackageItemInfo, WidgetItem> entry : this.mWidgetsList.entrySet()) {
            WidgetListRowEntry widgetListRowEntry = new WidgetListRowEntry(entry.getKey(), (ArrayList) entry.getValue());
            widgetListRowEntry.titleSectionName = alphabeticIndexCompat.computeSectionName(widgetListRowEntry.pkgItem.title);
            Collections.sort(widgetListRowEntry.widgets, widgetItemComparator);
            arrayList.add(widgetListRowEntry);
        }
        return arrayList;
    }

    public void update(LauncherAppState launcherAppState, @Nullable PackageUserKey packageUserKey) {
        Preconditions.assertWorkerThread();
        Context context = launcherAppState.getContext();
        ArrayList<? extends ComponentKey> arrayList = new ArrayList<>();
        try {
            PackageManager packageManager = context.getPackageManager();
            InvariantDeviceProfile invariantDeviceProfile = launcherAppState.getInvariantDeviceProfile();
            Iterator<AppWidgetProviderInfo> it = AppWidgetManagerCompat.getInstance(context).getAllProviders(packageUserKey).iterator();
            while (it.hasNext()) {
                arrayList.add(new WidgetItem(LauncherAppWidgetProviderInfo.fromProviderInfo(context, it.next()), packageManager, invariantDeviceProfile));
            }
            Iterator<ShortcutConfigActivityInfo> it2 = LauncherAppsCompat.getInstance(context).getCustomShortcutActivityList(packageUserKey).iterator();
            while (it2.hasNext()) {
                arrayList.add(new WidgetItem(it2.next()));
            }
            setWidgetsAndShortcuts(arrayList, launcherAppState, packageUserKey);
        } catch (Exception e) {
            if (!Utilities.isBinderSizeError(e)) {
                throw e;
            }
        }
        launcherAppState.getWidgetCache().removeObsoletePreviews(arrayList, packageUserKey);
    }

    private synchronized void setWidgetsAndShortcuts(ArrayList<WidgetItem> arrayList, LauncherAppState launcherAppState, @Nullable PackageUserKey packageUserKey) {
        HashMap map = new HashMap();
        if (packageUserKey == null) {
            this.mWidgetsList.clear();
        } else {
            PackageItemInfo packageItemInfo = null;
            Iterator<PackageItemInfo> it = this.mWidgetsList.keySet().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                PackageItemInfo next = it.next();
                if (next.packageName.equals(packageUserKey.mPackageName)) {
                    packageItemInfo = next;
                    break;
                }
            }
            if (packageItemInfo != null) {
                map.put(packageItemInfo.packageName, packageItemInfo);
                Iterator it2 = ((ArrayList) this.mWidgetsList.get(packageItemInfo)).iterator();
                while (it2.hasNext()) {
                    WidgetItem widgetItem = (WidgetItem) it2.next();
                    if (widgetItem.componentName.getPackageName().equals(packageUserKey.mPackageName) && widgetItem.user.equals(packageUserKey.mUser)) {
                        it2.remove();
                    }
                }
            }
        }
        InvariantDeviceProfile invariantDeviceProfile = launcherAppState.getInvariantDeviceProfile();
        UserHandle userHandleMyUserHandle = Process.myUserHandle();
        Iterator<WidgetItem> it3 = arrayList.iterator();
        while (it3.hasNext()) {
            WidgetItem next2 = it3.next();
            if (next2.widgetInfo != null) {
                if ((next2.widgetInfo.getWidgetFeatures() & 2) == 0) {
                    int iMin = Math.min(next2.widgetInfo.spanX, next2.widgetInfo.minSpanX);
                    int iMin2 = Math.min(next2.widgetInfo.spanY, next2.widgetInfo.minSpanY);
                    if (iMin <= invariantDeviceProfile.numColumns && iMin2 <= invariantDeviceProfile.numRows) {
                    }
                }
            }
            if (this.mAppFilter == null) {
                this.mAppFilter = AppFilter.newInstance(launcherAppState.getContext());
            }
            if (this.mAppFilter.shouldShowApp(next2.componentName)) {
                String packageName = next2.componentName.getPackageName();
                if (!packageName.startsWith("com.android.deskclock") && !packageName.startsWith("com.android.email") && !packageName.startsWith("com.android.calendar") && !packageName.startsWith("com.android.gallery3d") && !packageName.startsWith("com.android.browser") && !packageName.startsWith("com.android.music") && !packageName.startsWith("com.android.quicksearchbox") && !packageName.startsWith("com.android.settings") && !packageName.startsWith("com.android.contacts")) {
                    PackageItemInfo packageItemInfo2 = (PackageItemInfo) map.get(packageName);
                    if (packageItemInfo2 == null) {
                        packageItemInfo2 = new PackageItemInfo(packageName);
                        packageItemInfo2.user = next2.user;
                        map.put(packageName, packageItemInfo2);
                    } else if (!userHandleMyUserHandle.equals(packageItemInfo2.user)) {
                        packageItemInfo2.user = next2.user;
                    }
                    this.mWidgetsList.addToList(packageItemInfo2, next2);
                }
            }
        }
        IconCache iconCache = launcherAppState.getIconCache();
        Iterator it4 = map.values().iterator();
        while (it4.hasNext()) {
            iconCache.getTitleAndIconForApp((PackageItemInfo) it4.next(), true);
        }
    }
}
