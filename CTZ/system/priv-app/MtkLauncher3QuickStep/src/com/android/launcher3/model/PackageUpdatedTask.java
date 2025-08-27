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

/* loaded from: classes.dex */
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

    /* JADX WARN: Removed duplicated region for block: B:109:0x0281 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:110:0x0283 A[Catch: all -> 0x03ce, TryCatch #0 {, blocks: (B:55:0x015f, B:56:0x0165, B:58:0x016b, B:60:0x017b, B:62:0x0187, B:64:0x018f, B:66:0x0199, B:68:0x01a8, B:70:0x01af, B:72:0x01b5, B:74:0x01bb, B:76:0x01cb, B:80:0x01f5, B:83:0x01fd, B:85:0x0205, B:87:0x0211, B:89:0x0222, B:92:0x0232, B:93:0x023b, B:95:0x0241, B:97:0x024d, B:99:0x0255, B:101:0x0259, B:103:0x0263, B:112:0x0288, B:110:0x0283, B:116:0x0297, B:119:0x02a1, B:121:0x02ad, B:123:0x02b4, B:125:0x02c0, B:129:0x02f0), top: B:173:0x015f }] */
    /* JADX WARN: Removed duplicated region for block: B:112:0x0288 A[Catch: all -> 0x03ce, TryCatch #0 {, blocks: (B:55:0x015f, B:56:0x0165, B:58:0x016b, B:60:0x017b, B:62:0x0187, B:64:0x018f, B:66:0x0199, B:68:0x01a8, B:70:0x01af, B:72:0x01b5, B:74:0x01bb, B:76:0x01cb, B:80:0x01f5, B:83:0x01fd, B:85:0x0205, B:87:0x0211, B:89:0x0222, B:92:0x0232, B:93:0x023b, B:95:0x0241, B:97:0x024d, B:99:0x0255, B:101:0x0259, B:103:0x0263, B:112:0x0288, B:110:0x0283, B:116:0x0297, B:119:0x02a1, B:121:0x02ad, B:123:0x02b4, B:125:0x02c0, B:129:0x02f0), top: B:173:0x015f }] */
    /* JADX WARN: Removed duplicated region for block: B:126:0x02d9 A[PHI: r19 r21 r22 r23
  0x02d9: PHI (r19v4 int) = (r19v2 int), (r19v2 int), (r19v2 int), (r19v2 int), (r19v2 int), (r19v5 int) binds: [B:117:0x029d, B:118:0x029f, B:120:0x02ab, B:122:0x02b2, B:124:0x02be, B:113:0x028f] A[DONT_GENERATE, DONT_INLINE]
  0x02d9: PHI (r21v4 java.lang.String[]) = 
  (r21v2 java.lang.String[])
  (r21v2 java.lang.String[])
  (r21v2 java.lang.String[])
  (r21v2 java.lang.String[])
  (r21v2 java.lang.String[])
  (r21v8 java.lang.String[])
 binds: [B:117:0x029d, B:118:0x029f, B:120:0x02ab, B:122:0x02b2, B:124:0x02be, B:113:0x028f] A[DONT_GENERATE, DONT_INLINE]
  0x02d9: PHI (r22v2 com.android.launcher3.util.ItemInfoMatcher) = 
  (r22v0 com.android.launcher3.util.ItemInfoMatcher)
  (r22v0 com.android.launcher3.util.ItemInfoMatcher)
  (r22v0 com.android.launcher3.util.ItemInfoMatcher)
  (r22v0 com.android.launcher3.util.ItemInfoMatcher)
  (r22v0 com.android.launcher3.util.ItemInfoMatcher)
  (r22v5 com.android.launcher3.util.ItemInfoMatcher)
 binds: [B:117:0x029d, B:118:0x029f, B:120:0x02ab, B:122:0x02b2, B:124:0x02be, B:113:0x028f] A[DONT_GENERATE, DONT_INLINE]
  0x02d9: PHI (r23v1 java.util.ArrayList) = 
  (r23v0 java.util.ArrayList)
  (r23v0 java.util.ArrayList)
  (r23v0 java.util.ArrayList)
  (r23v0 java.util.ArrayList)
  (r23v0 java.util.ArrayList)
  (r23v4 java.util.ArrayList)
 binds: [B:117:0x029d, B:118:0x029f, B:120:0x02ab, B:122:0x02b2, B:124:0x02be, B:113:0x028f] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Removed duplicated region for block: B:158:0x0379  */
    /* JADX WARN: Removed duplicated region for block: B:161:0x03a4  */
    /* JADX WARN: Removed duplicated region for block: B:69:0x01ae  */
    @Override // com.android.launcher3.model.BaseModelUpdateTask
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
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
        final ArrayList arrayList6 = new ArrayList();
        arrayList6.addAll(allAppsList.added);
        allAppsList.added.clear();
        arrayList6.addAll(allAppsList.modified);
        allAppsList.modified.clear();
        ArrayList arrayList7 = new ArrayList(allAppsList.removed);
        allAppsList.removed.clear();
        ArrayMap arrayMap = new ArrayMap();
        if (!arrayList6.isEmpty()) {
            scheduleCallbackTask(new LauncherModel.CallbackTask() { // from class: com.android.launcher3.model.PackageUpdatedTask.1
                @Override // com.android.launcher3.LauncherModel.CallbackTask
                public void execute(LauncherModel.Callbacks callbacks) {
                    callbacks.bindAppsAddedOrUpdated(arrayList6);
                }
            });
            Iterator it = arrayList6.iterator();
            while (it.hasNext()) {
                AppInfo appInfo = (AppInfo) it.next();
                arrayMap.put(appInfo.componentName, appInfo);
            }
        }
        LongArrayMap longArrayMap = new LongArrayMap();
        if (this.mOp == 1 || flagOpRemoveFlag != FlagOp.NO_OP) {
            ArrayList<ShortcutInfo> arrayList8 = new ArrayList<>();
            ArrayList arrayList9 = new ArrayList();
            boolean z4 = this.mOp == 1 || this.mOp == 2;
            synchronized (bgDataModel) {
                Iterator<ItemInfo> it2 = bgDataModel.itemsIdMap.iterator();
                while (it2.hasNext()) {
                    Iterator<ItemInfo> it3 = it2;
                    ItemInfo next = it2.next();
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
                                        it2 = it3;
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
                                                    it2 = it3;
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
                                        it2 = it3;
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
                                    it2 = it3;
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
                        it2 = it3;
                        arrayList7 = arrayList10;
                        length = i3;
                        strArr4 = strArr2;
                        itemInfoMatcherOfPackages = itemInfoMatcher;
                    } else {
                        arrayList4 = arrayList3;
                        arrayList9 = arrayList4;
                        it2 = it3;
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
                scheduleCallbackTask(new LauncherModel.CallbackTask() { // from class: com.android.launcher3.model.PackageUpdatedTask.2
                    @Override // com.android.launcher3.LauncherModel.CallbackTask
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
                        Iterator it4 = arrayList5.iterator();
                        while (it4.hasNext()) {
                            hashSet3.add(((AppInfo) it4.next()).componentName);
                        }
                    }
                }
            }
            if (hashSet2.isEmpty() || !hashSet3.isEmpty()) {
                deleteAndBindComponentsRemoved(ItemInfoMatcher.ofPackages(hashSet2, this.mUser).or(ItemInfoMatcher.ofComponents(hashSet3, this.mUser)).and(ItemInfoMatcher.ofItemIds(longArrayMap, true)));
                InstallShortcutReceiver.removeFromInstallQueue(context, hashSet2, this.mUser);
            }
            if (!arrayList5.isEmpty()) {
                scheduleCallbackTask(new LauncherModel.CallbackTask() { // from class: com.android.launcher3.model.PackageUpdatedTask.3
                    @Override // com.android.launcher3.LauncherModel.CallbackTask
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
