package com.android.launcher3.model;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.util.MultiHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* loaded from: classes.dex */
public class FirstScreenBroadcast {
    private static final String ACTION_FIRST_SCREEN_ACTIVE_INSTALLS = "com.android.launcher3.action.FIRST_SCREEN_ACTIVE_INSTALLS";
    private static final boolean DEBUG = false;
    private static final String FOLDER_ITEM_EXTRA = "folderItem";
    private static final String HOTSEAT_ITEM_EXTRA = "hotseatItem";
    private static final String TAG = "FirstScreenBroadcast";
    private static final String VERIFICATION_TOKEN_EXTRA = "verificationToken";
    private static final String WIDGET_ITEM_EXTRA = "widgetItem";
    private static final String WORKSPACE_ITEM_EXTRA = "workspaceItem";
    private final MultiHashMap<String, String> mPackagesForInstaller;

    public FirstScreenBroadcast(HashMap<String, PackageInstaller.SessionInfo> map) {
        this.mPackagesForInstaller = getPackagesForInstaller(map);
    }

    private MultiHashMap<String, String> getPackagesForInstaller(HashMap<String, PackageInstaller.SessionInfo> map) {
        MultiHashMap<String, String> multiHashMap = new MultiHashMap<>();
        for (Map.Entry<String, PackageInstaller.SessionInfo> entry : map.entrySet()) {
            multiHashMap.addToList(entry.getValue().getInstallerPackageName(), entry.getKey());
        }
        return multiHashMap;
    }

    public void sendBroadcasts(Context context, List<ItemInfo> list) {
        for (Map.Entry<String, String> entry : this.mPackagesForInstaller.entrySet()) {
            sendBroadcastToInstaller(context, entry.getKey(), (List) entry.getValue(), list);
        }
    }

    private void sendBroadcastToInstaller(Context context, String str, List<String> list, List<ItemInfo> list2) {
        HashSet hashSet = new HashSet();
        HashSet hashSet2 = new HashSet();
        HashSet hashSet3 = new HashSet();
        HashSet hashSet4 = new HashSet();
        for (ItemInfo itemInfo : list2) {
            if (itemInfo instanceof FolderInfo) {
                Iterator<ShortcutInfo> it = ((FolderInfo) itemInfo).contents.iterator();
                while (it.hasNext()) {
                    String packageName = getPackageName(it.next());
                    if (packageName != null && list.contains(packageName)) {
                        hashSet.add(packageName);
                    }
                }
            }
            String packageName2 = getPackageName(itemInfo);
            if (packageName2 != null && list.contains(packageName2)) {
                if (itemInfo instanceof LauncherAppWidgetInfo) {
                    hashSet4.add(packageName2);
                } else if (itemInfo.container == -101) {
                    hashSet3.add(packageName2);
                } else if (itemInfo.container == -100) {
                    hashSet2.add(packageName2);
                }
            }
        }
        context.sendBroadcast(new Intent(ACTION_FIRST_SCREEN_ACTIVE_INSTALLS).setPackage(str).putStringArrayListExtra(FOLDER_ITEM_EXTRA, new ArrayList<>(hashSet)).putStringArrayListExtra(WORKSPACE_ITEM_EXTRA, new ArrayList<>(hashSet2)).putStringArrayListExtra(HOTSEAT_ITEM_EXTRA, new ArrayList<>(hashSet3)).putStringArrayListExtra(WIDGET_ITEM_EXTRA, new ArrayList<>(hashSet4)).putExtra(VERIFICATION_TOKEN_EXTRA, PendingIntent.getActivity(context, 0, new Intent(), 1140850688)));
    }

    private static String getPackageName(ItemInfo itemInfo) {
        if (!(itemInfo instanceof LauncherAppWidgetInfo)) {
            if (itemInfo.getTargetComponent() != null) {
                return itemInfo.getTargetComponent().getPackageName();
            }
            return null;
        }
        LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) itemInfo;
        if (launcherAppWidgetInfo.providerName != null) {
            return launcherAppWidgetInfo.providerName.getPackageName();
        }
        return null;
    }

    private static void printList(String str, String str2, Set<String> set) {
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            Log.d(TAG, str + ":" + str2 + ":" + it.next());
        }
    }
}
