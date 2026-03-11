package com.android.launcher3.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.os.UserHandle;
import android.util.ArrayMap;
import com.android.launcher3.AllAppsList;
import com.android.launcher3.AppInfo;
import com.android.launcher3.IconCache;
import com.android.launcher3.InstallShortcutReceiver;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.SessionCommitReceiver;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.graphics.BitmapInfo;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.launcher3.util.FlagOp;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.LongArrayMap;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.util.PackageUserKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

public class PackageUpdatedTask extends BaseModelUpdateTask {
    private static final boolean DEBUG = false;
    public static final int OP_ADD = 1;
    public static final int OP_NONE = 0;
    public static final int OP_REMOVE = 3;
    public static final int OP_SUSPEND = 5;
    public static final int OP_UNAVAILABLE = 4;
    public static final int OP_UNSUSPEND = 6;
    public static final int OP_UPDATE = 2;
    public static final int OP_USER_AVAILABILITY_CHANGE = 7;
    private static final String TAG = "PackageUpdatedTask";
    private final int mOp;
    private final String[] mPackages;
    private final UserHandle mUser;

    public PackageUpdatedTask(int i, UserHandle userHandle, String... strArr) {
        this.mOp = i;
        this.mUser = userHandle;
        this.mPackages = strArr;
    }

    @Override
    public void execute(LauncherAppState launcherAppState, BgDataModel bgDataModel, AllAppsList allAppsList) {
        String[] strArr;
        int i;
        final ArrayList arrayList;
        ArrayList arrayList2;
        int i2;
        String[] strArr2;
        int i3;
        ItemInfoMatcher itemInfoMatcher;
        ArrayList arrayList3;
        ArrayList arrayList4;
        boolean z;
        ComponentName targetComponent;
        boolean z2;
        boolean z3;
        String[] strArr3;
        int i4;
        final ArrayList arrayList5;
        Context context = launcherAppState.getContext();
        IconCache iconCache = launcherAppState.getIconCache();
        String[] strArr4 = this.mPackages;
        int length = strArr4.length;
        FlagOp flagOpRemoveFlag = FlagOp.NO_OP;
        HashSet hashSet = new HashSet(Arrays.asList(strArr4));
        ItemInfoMatcher itemInfoMatcherOfPackages = ItemInfoMatcher.ofPackages(hashSet, this.mUser);
        switch (this.mOp) {
            case 1:
                for (int i5 = 0; i5 < length; i5++) {
                    iconCache.updateIconsForPkg(strArr4[i5], this.mUser);
                    allAppsList.addPackage(context, strArr4[i5], this.mUser);
                    if (!Utilities.ATLEAST_OREO && !Process.myUserHandle().equals(this.mUser)) {
                        SessionCommitReceiver.queueAppIconAddition(context, strArr4[i5], this.mUser);
                    }
                }
                flagOpRemoveFlag = FlagOp.removeFlag(2);
                break;
            case 2:
                for (int i6 = 0; i6 < length; i6++) {
                    iconCache.updateIconsForPkg(strArr4[i6], this.mUser);
                    allAppsList.updatePackage(context, strArr4[i6], this.mUser);
                    launcherAppState.getWidgetCache().removePackage(strArr4[i6], this.mUser);
                }
                flagOpRemoveFlag = FlagOp.removeFlag(2);
                break;
            case 3:
                for (String str : strArr4) {
                    iconCache.removeIconsForPkg(str, this.mUser);
                }
            case 4:
                for (int i7 = 0; i7 < length; i7++) {
                    allAppsList.removePackage(strArr4[i7], this.mUser);
                    launcherAppState.getWidgetCache().removePackage(strArr4[i7], this.mUser);
                }
                flagOpRemoveFlag = FlagOp.addFlag(2);
                break;
            case 5:
            case 6:
                if (this.mOp == 5) {
                    flagOpRemoveFlag = FlagOp.addFlag(4);
                } else {
                    flagOpRemoveFlag = FlagOp.removeFlag(4);
                }
                allAppsList.updateDisabledFlags(itemInfoMatcherOfPackages, flagOpRemoveFlag);
                break;
            case 7:
                if (UserManagerCompat.getInstance(context).isQuietModeEnabled(this.mUser)) {
                    flagOpRemoveFlag = FlagOp.addFlag(8);
                } else {
                    flagOpRemoveFlag = FlagOp.removeFlag(8);
                }
                itemInfoMatcherOfPackages = ItemInfoMatcher.ofUser(this.mUser);
                allAppsList.updateDisabledFlags(itemInfoMatcherOfPackages, flagOpRemoveFlag);
                break;
        }
        final ArrayList<AppInfo> arrayList6 = new ArrayList();
        arrayList6.addAll(allAppsList.added);
        allAppsList.added.clear();
        arrayList6.addAll(allAppsList.modified);
        allAppsList.modified.clear();
        ArrayList arrayList7 = new ArrayList(allAppsList.removed);
        allAppsList.removed.clear();
        ArrayMap arrayMap = new ArrayMap();
        if (!arrayList6.isEmpty()) {
            scheduleCallbackTask(new LauncherModel.CallbackTask() {
                @Override
                public void execute(LauncherModel.Callbacks callbacks) {
                    callbacks.bindAppsAddedOrUpdated(arrayList6);
                }
            });
            for (AppInfo appInfo : arrayList6) {
                arrayMap.put(appInfo.componentName, appInfo);
            }
        }
        LongArrayMap longArrayMap = new LongArrayMap();
        if (this.mOp == 1 || flagOpRemoveFlag != FlagOp.NO_OP) {
            ArrayList<ShortcutInfo> arrayList8 = new ArrayList<>();
            ArrayList arrayList9 = new ArrayList();
            boolean z4 = this.mOp == 1 || this.mOp == 2;
            synchronized (bgDataModel) {
                Iterator<ItemInfo> it = bgDataModel.itemsIdMap.iterator();
                while (it.hasNext()) {
                    Iterator<ItemInfo> it2 = it;
                    ItemInfo next = it.next();
                    ArrayList arrayList10 = arrayList7;
                    if (next instanceof ShortcutInfo) {
                        i3 = length;
                        if (this.mUser.equals(next.user)) {
                            ShortcutInfo shortcutInfo = (ShortcutInfo) next;
                            if (shortcutInfo.iconResource != null && hashSet.contains(shortcutInfo.iconResource.packageName)) {
                                LauncherIcons launcherIconsObtain = LauncherIcons.obtain(context);
                                BitmapInfo bitmapInfoCreateIconBitmap = launcherIconsObtain.createIconBitmap(shortcutInfo.iconResource);
                                launcherIconsObtain.recycle();
                                if (bitmapInfoCreateIconBitmap != null) {
                                    bitmapInfoCreateIconBitmap.applyTo(shortcutInfo);
                                    z = true;
                                }
                                targetComponent = shortcutInfo.getTargetComponent();
                                if (targetComponent == null) {
                                }
                                strArr2 = strArr4;
                                itemInfoMatcher = itemInfoMatcherOfPackages;
                                arrayList3 = arrayList9;
                                z2 = z;
                                if (z2) {
                                }
                            } else {
                                z = false;
                                targetComponent = shortcutInfo.getTargetComponent();
                                if (targetComponent == null && itemInfoMatcherOfPackages.matches(shortcutInfo, targetComponent)) {
                                    AppInfo appInfo2 = (AppInfo) arrayMap.get(targetComponent);
                                    boolean z5 = z;
                                    if (shortcutInfo.hasStatusFlag(16)) {
                                        strArr2 = strArr4;
                                        itemInfoMatcher = itemInfoMatcherOfPackages;
                                        arrayList3 = arrayList9;
                                        longArrayMap.put(shortcutInfo.id, false);
                                        if (this.mOp == 3) {
                                        }
                                        it = it2;
                                        arrayList7 = arrayList10;
                                        length = i3;
                                        strArr4 = strArr2;
                                        itemInfoMatcherOfPackages = itemInfoMatcher;
                                        arrayList9 = arrayList3;
                                    } else {
                                        strArr2 = strArr4;
                                        itemInfoMatcher = itemInfoMatcherOfPackages;
                                        arrayList3 = arrayList9;
                                    }
                                    if (shortcutInfo.isPromise() && z4) {
                                        if (shortcutInfo.hasStatusFlag(2)) {
                                            if (!LauncherAppsCompat.getInstance(context).isActivityEnabledForProfile(targetComponent, this.mUser)) {
                                                Intent appLaunchIntent = new PackageManagerHelper(context).getAppLaunchIntent(targetComponent.getPackageName(), this.mUser);
                                                if (appLaunchIntent != null) {
                                                    appInfo2 = (AppInfo) arrayMap.get(appLaunchIntent.getComponent());
                                                }
                                                if (appLaunchIntent != null && appInfo2 != null) {
                                                    shortcutInfo.intent = appLaunchIntent;
                                                    shortcutInfo.status = 0;
                                                    z5 = true;
                                                } else if (shortcutInfo.hasPromiseIconUi()) {
                                                    longArrayMap.put(shortcutInfo.id, true);
                                                    it = it2;
                                                    arrayList7 = arrayList10;
                                                    length = i3;
                                                    strArr4 = strArr2;
                                                    itemInfoMatcherOfPackages = itemInfoMatcher;
                                                    arrayList9 = arrayList3;
                                                }
                                            }
                                        } else {
                                            shortcutInfo.status = 0;
                                            z5 = true;
                                        }
                                    }
                                    if (z4 && shortcutInfo.itemType == 0) {
                                        iconCache.getTitleAndIcon(shortcutInfo, shortcutInfo.usingLowResIcon);
                                        z2 = true;
                                    } else {
                                        z2 = z5;
                                    }
                                    int i8 = shortcutInfo.runtimeStatusFlags;
                                    shortcutInfo.runtimeStatusFlags = flagOpRemoveFlag.apply(shortcutInfo.runtimeStatusFlags);
                                    z3 = shortcutInfo.runtimeStatusFlags != i8;
                                    if (z2) {
                                        arrayList8.add(shortcutInfo);
                                        if (z2) {
                                        }
                                        arrayList4 = arrayList3;
                                        arrayList9 = arrayList4;
                                        it = it2;
                                        arrayList7 = arrayList10;
                                        length = i3;
                                        strArr4 = strArr2;
                                        itemInfoMatcherOfPackages = itemInfoMatcher;
                                    }
                                } else {
                                    strArr2 = strArr4;
                                    itemInfoMatcher = itemInfoMatcherOfPackages;
                                    arrayList3 = arrayList9;
                                    z2 = z;
                                    if (z2 || z3) {
                                        arrayList8.add(shortcutInfo);
                                    }
                                    if (z2) {
                                        getModelWriter().updateItemInDatabase(shortcutInfo);
                                    }
                                    arrayList4 = arrayList3;
                                    arrayList9 = arrayList4;
                                    it = it2;
                                    arrayList7 = arrayList10;
                                    length = i3;
                                    strArr4 = strArr2;
                                    itemInfoMatcherOfPackages = itemInfoMatcher;
                                }
                            }
                        } else {
                            strArr2 = strArr4;
                        }
                    } else {
                        strArr2 = strArr4;
                        i3 = length;
                    }
                    itemInfoMatcher = itemInfoMatcherOfPackages;
                    arrayList3 = arrayList9;
                    if ((next instanceof LauncherAppWidgetInfo) && z4) {
                        LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) next;
                        if (this.mUser.equals(launcherAppWidgetInfo.user) && launcherAppWidgetInfo.hasRestoreFlag(2) && hashSet.contains(launcherAppWidgetInfo.providerName.getPackageName())) {
                            launcherAppWidgetInfo.restoreStatus &= -11;
                            launcherAppWidgetInfo.restoreStatus |= 4;
                            arrayList4 = arrayList3;
                            arrayList4.add(launcherAppWidgetInfo);
                            getModelWriter().updateItemInDatabase(launcherAppWidgetInfo);
                        }
                        arrayList9 = arrayList4;
                        it = it2;
                        arrayList7 = arrayList10;
                        length = i3;
                        strArr4 = strArr2;
                        itemInfoMatcherOfPackages = itemInfoMatcher;
                    } else {
                        arrayList4 = arrayList3;
                        arrayList9 = arrayList4;
                        it = it2;
                        arrayList7 = arrayList10;
                        length = i3;
                        strArr4 = strArr2;
                        itemInfoMatcherOfPackages = itemInfoMatcher;
                    }
                }
                strArr = strArr4;
                i = length;
                arrayList = arrayList9;
                arrayList2 = arrayList7;
            }
            bindUpdatedShortcuts(arrayList8, this.mUser);
            if (!longArrayMap.isEmpty()) {
                i2 = 0;
                deleteAndBindComponentsRemoved(ItemInfoMatcher.ofItemIds(longArrayMap, false));
            } else {
                i2 = 0;
            }
            if (!arrayList.isEmpty()) {
                scheduleCallbackTask(new LauncherModel.CallbackTask() {
                    @Override
                    public void execute(LauncherModel.Callbacks callbacks) {
                        callbacks.bindWidgetsRestored(arrayList);
                    }
                });
            }
        } else {
            strArr = strArr4;
            i = length;
            arrayList2 = arrayList7;
            i2 = 0;
        }
        HashSet hashSet2 = new HashSet();
        HashSet hashSet3 = new HashSet();
        if (this.mOp == 3) {
            strArr3 = strArr;
            Collections.addAll(hashSet2, strArr3);
        } else {
            strArr3 = strArr;
            if (this.mOp == 2) {
                LauncherAppsCompat launcherAppsCompat = LauncherAppsCompat.getInstance(context);
                int i9 = i2;
                while (true) {
                    i4 = i;
                    if (i9 < i4) {
                        if (!launcherAppsCompat.isPackageEnabledForProfile(strArr3[i9], this.mUser)) {
                            hashSet2.add(strArr3[i9]);
                        }
                        i9++;
                        i = i4;
                    } else {
                        arrayList5 = arrayList2;
                        Iterator it3 = arrayList5.iterator();
                        while (it3.hasNext()) {
                            hashSet3.add(((AppInfo) it3.next()).componentName);
                        }
                    }
                }
            }
            if (hashSet2.isEmpty() || !hashSet3.isEmpty()) {
                deleteAndBindComponentsRemoved(ItemInfoMatcher.ofPackages(hashSet2, this.mUser).or(ItemInfoMatcher.ofComponents(hashSet3, this.mUser)).and(ItemInfoMatcher.ofItemIds(longArrayMap, true)));
                InstallShortcutReceiver.removeFromInstallQueue(context, hashSet2, this.mUser);
            }
            if (!arrayList5.isEmpty()) {
                scheduleCallbackTask(new LauncherModel.CallbackTask() {
                    @Override
                    public void execute(LauncherModel.Callbacks callbacks) {
                        callbacks.bindAppInfosRemoved(arrayList5);
                    }
                });
            }
            if (!Utilities.ATLEAST_OREO && this.mOp == 1) {
                while (i2 < i4) {
                    bgDataModel.widgetsModel.update(launcherAppState, new PackageUserKey(strArr3[i2], this.mUser));
                    i2++;
                }
                bindUpdatedWidgets(bgDataModel);
                return;
            }
        }
        arrayList5 = arrayList2;
        i4 = i;
        if (hashSet2.isEmpty()) {
            deleteAndBindComponentsRemoved(ItemInfoMatcher.ofPackages(hashSet2, this.mUser).or(ItemInfoMatcher.ofComponents(hashSet3, this.mUser)).and(ItemInfoMatcher.ofItemIds(longArrayMap, true)));
            InstallShortcutReceiver.removeFromInstallQueue(context, hashSet2, this.mUser);
        }
        if (!arrayList5.isEmpty()) {
        }
        if (!Utilities.ATLEAST_OREO) {
        }
    }
}
