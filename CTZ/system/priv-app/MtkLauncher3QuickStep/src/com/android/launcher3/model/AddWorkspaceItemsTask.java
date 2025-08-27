package com.android.launcher3.model;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.LongSparseArray;
import android.util.Pair;
import com.android.launcher3.AllAppsList;
import com.android.launcher3.AppInfo;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.GridOccupancy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class AddWorkspaceItemsTask extends BaseModelUpdateTask {
    private final List<Pair<ItemInfo, Object>> mItemList;

    public AddWorkspaceItemsTask(List<Pair<ItemInfo, Object>> list) {
        this.mItemList = list;
    }

    @Override // com.android.launcher3.model.BaseModelUpdateTask
    public void execute(LauncherAppState launcherAppState, BgDataModel bgDataModel, AllAppsList allAppsList) {
        if (this.mItemList.isEmpty()) {
            return;
        }
        Context context = launcherAppState.getContext();
        final ArrayList arrayList = new ArrayList();
        final ArrayList<Long> arrayList2 = new ArrayList<>();
        ArrayList<Long> arrayListLoadWorkspaceScreensDb = LauncherModel.loadWorkspaceScreensDb(context);
        synchronized (bgDataModel) {
            ArrayList<ItemInfo> arrayList3 = new ArrayList();
            Iterator<Pair<ItemInfo, Object>> it = this.mItemList.iterator();
            while (it.hasNext()) {
                ItemInfo itemInfoMakeShortcut = (ItemInfo) it.next().first;
                if ((itemInfoMakeShortcut.itemType != 0 && itemInfoMakeShortcut.itemType != 1) || !shortcutExists(bgDataModel, itemInfoMakeShortcut.getIntent(), itemInfoMakeShortcut.user)) {
                    if (itemInfoMakeShortcut.itemType == 0 && (itemInfoMakeShortcut instanceof AppInfo)) {
                        itemInfoMakeShortcut = ((AppInfo) itemInfoMakeShortcut).makeShortcut();
                    }
                    if (itemInfoMakeShortcut != null) {
                        arrayList3.add(itemInfoMakeShortcut);
                    }
                }
            }
            for (ItemInfo itemInfoMakeShortcut2 : arrayList3) {
                Pair<Long, int[]> pairFindSpaceForItem = findSpaceForItem(launcherAppState, bgDataModel, arrayListLoadWorkspaceScreensDb, arrayList2, itemInfoMakeShortcut2.spanX, itemInfoMakeShortcut2.spanY);
                long jLongValue = ((Long) pairFindSpaceForItem.first).longValue();
                int[] iArr = (int[]) pairFindSpaceForItem.second;
                if (!(itemInfoMakeShortcut2 instanceof ShortcutInfo) && !(itemInfoMakeShortcut2 instanceof FolderInfo) && !(itemInfoMakeShortcut2 instanceof LauncherAppWidgetInfo)) {
                    if (itemInfoMakeShortcut2 instanceof AppInfo) {
                        itemInfoMakeShortcut2 = ((AppInfo) itemInfoMakeShortcut2).makeShortcut();
                    } else {
                        throw new RuntimeException("Unexpected info type");
                    }
                }
                getModelWriter().addItemToDatabase(itemInfoMakeShortcut2, -100L, jLongValue, iArr[0], iArr[1]);
                arrayList.add(itemInfoMakeShortcut2);
            }
        }
        updateScreens(context, arrayListLoadWorkspaceScreensDb);
        if (!arrayList.isEmpty()) {
            scheduleCallbackTask(new LauncherModel.CallbackTask() { // from class: com.android.launcher3.model.AddWorkspaceItemsTask.1
                @Override // com.android.launcher3.LauncherModel.CallbackTask
                public void execute(LauncherModel.Callbacks callbacks) {
                    ArrayList<ItemInfo> arrayList4 = new ArrayList<>();
                    ArrayList<ItemInfo> arrayList5 = new ArrayList<>();
                    if (!arrayList.isEmpty()) {
                        long j = ((ItemInfo) arrayList.get(arrayList.size() - 1)).screenId;
                        Iterator it2 = arrayList.iterator();
                        while (it2.hasNext()) {
                            ItemInfo itemInfo = (ItemInfo) it2.next();
                            if (itemInfo.screenId == j) {
                                arrayList4.add(itemInfo);
                            } else {
                                arrayList5.add(itemInfo);
                            }
                        }
                    }
                    callbacks.bindAppsAdded(arrayList2, arrayList5, arrayList4);
                }
            });
        }
    }

    protected void updateScreens(Context context, ArrayList<Long> arrayList) {
        LauncherModel.updateWorkspaceScreenOrder(context, arrayList);
    }

    protected boolean shortcutExists(BgDataModel bgDataModel, Intent intent, UserHandle userHandle) {
        String uri;
        String uri2;
        String packageName;
        if (intent == null) {
            return true;
        }
        if (intent.getComponent() != null) {
            packageName = intent.getComponent().getPackageName();
            if (intent.getPackage() != null) {
                uri = intent.toUri(0);
                uri2 = new Intent(intent).setPackage(null).toUri(0);
            } else {
                uri = new Intent(intent).setPackage(packageName).toUri(0);
                uri2 = intent.toUri(0);
            }
        } else {
            uri = intent.toUri(0);
            uri2 = intent.toUri(0);
            packageName = null;
        }
        boolean zIsLauncherAppTarget = Utilities.isLauncherAppTarget(intent);
        synchronized (bgDataModel) {
            Iterator<ItemInfo> it = bgDataModel.itemsIdMap.iterator();
            while (it.hasNext()) {
                ItemInfo next = it.next();
                if (next instanceof ShortcutInfo) {
                    ShortcutInfo shortcutInfo = (ShortcutInfo) next;
                    if (next.getIntent() != null && shortcutInfo.user.equals(userHandle)) {
                        Intent intent2 = new Intent(next.getIntent());
                        intent2.setSourceBounds(intent.getSourceBounds());
                        String uri3 = intent2.toUri(0);
                        if (!uri.equals(uri3) && !uri2.equals(uri3)) {
                            if (zIsLauncherAppTarget && shortcutInfo.isPromise() && shortcutInfo.hasStatusFlag(2) && shortcutInfo.getTargetComponent() != null && packageName != null && packageName.equals(shortcutInfo.getTargetComponent().getPackageName())) {
                                return true;
                            }
                        }
                        return true;
                    }
                }
            }
            return false;
        }
    }

    protected Pair<Long, int[]> findSpaceForItem(LauncherAppState launcherAppState, BgDataModel bgDataModel, ArrayList<Long> arrayList, ArrayList<Long> arrayList2, int i, int i2) {
        long jLongValue;
        LongSparseArray longSparseArray = new LongSparseArray();
        synchronized (bgDataModel) {
            Iterator<ItemInfo> it = bgDataModel.itemsIdMap.iterator();
            while (it.hasNext()) {
                ItemInfo next = it.next();
                if (next.container == -100) {
                    ArrayList arrayList3 = (ArrayList) longSparseArray.get(next.screenId);
                    if (arrayList3 == null) {
                        arrayList3 = new ArrayList();
                        longSparseArray.put(next.screenId, arrayList3);
                    }
                    arrayList3.add(next);
                }
            }
        }
        long jLongValue2 = 0;
        int[] iArr = new int[2];
        boolean zFindNextAvailableIconSpaceInScreen = false;
        int size = arrayList.size();
        int i3 = !arrayList.isEmpty() ? 1 : 0;
        if (i3 < size) {
            jLongValue2 = arrayList.get(i3).longValue();
            zFindNextAvailableIconSpaceInScreen = findNextAvailableIconSpaceInScreen(launcherAppState, (ArrayList) longSparseArray.get(jLongValue2), iArr, i, i2);
        }
        long j = jLongValue2;
        boolean z = zFindNextAvailableIconSpaceInScreen;
        if (!z) {
            long j2 = j;
            int i4 = 1;
            while (true) {
                if (i4 < size) {
                    jLongValue = arrayList.get(i4).longValue();
                    if (!findNextAvailableIconSpaceInScreen(launcherAppState, (ArrayList) longSparseArray.get(jLongValue), iArr, i, i2)) {
                        i4++;
                        j2 = jLongValue;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    jLongValue = j2;
                    break;
                }
            }
        } else {
            jLongValue = j;
        }
        if (!z) {
            jLongValue = LauncherSettings.Settings.call(launcherAppState.getContext().getContentResolver(), LauncherSettings.Settings.METHOD_NEW_SCREEN_ID).getLong(LauncherSettings.Settings.EXTRA_VALUE);
            arrayList.add(Long.valueOf(jLongValue));
            arrayList2.add(Long.valueOf(jLongValue));
            if (!findNextAvailableIconSpaceInScreen(launcherAppState, (ArrayList) longSparseArray.get(jLongValue), iArr, i, i2)) {
                throw new RuntimeException("Can't find space to add the item");
            }
        }
        return Pair.create(Long.valueOf(jLongValue), iArr);
    }

    private boolean findNextAvailableIconSpaceInScreen(LauncherAppState launcherAppState, ArrayList<ItemInfo> arrayList, int[] iArr, int i, int i2) {
        InvariantDeviceProfile invariantDeviceProfile = launcherAppState.getInvariantDeviceProfile();
        GridOccupancy gridOccupancy = new GridOccupancy(invariantDeviceProfile.numColumns, invariantDeviceProfile.numRows);
        if (arrayList != null) {
            Iterator<ItemInfo> it = arrayList.iterator();
            while (it.hasNext()) {
                gridOccupancy.markCells(it.next(), true);
            }
        }
        return gridOccupancy.findVacantCell(iArr, i, i2);
    }
}
