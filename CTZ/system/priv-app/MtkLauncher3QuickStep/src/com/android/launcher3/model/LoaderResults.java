package com.android.launcher3.model;

import android.os.Looper;
import android.util.Log;
import com.android.launcher3.AllAppsList;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.MainThreadExecutor;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.LooperIdleLock;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.ViewOnDrawExecutor;
import com.android.launcher3.widget.WidgetListRowEntry;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Executor;

/* loaded from: classes.dex */
public class LoaderResults {
    private static final long INVALID_SCREEN_ID = -1;
    private static final int ITEMS_CHUNK = 6;
    private static final String TAG = "LoaderResults";
    private final LauncherAppState mApp;
    private final AllAppsList mBgAllAppsList;
    private final BgDataModel mBgDataModel;
    private final WeakReference<LauncherModel.Callbacks> mCallbacks;
    private final int mPageToBindFirst;
    private final Executor mUiExecutor = new MainThreadExecutor();

    public LoaderResults(LauncherAppState launcherAppState, BgDataModel bgDataModel, AllAppsList allAppsList, int i, WeakReference<LauncherModel.Callbacks> weakReference) {
        this.mApp = launcherAppState;
        this.mBgDataModel = bgDataModel;
        this.mBgAllAppsList = allAppsList;
        this.mPageToBindFirst = i;
        this.mCallbacks = weakReference == null ? new WeakReference<>(null) : weakReference;
    }

    public void bindWorkspace() {
        LauncherModel.Callbacks callbacks = this.mCallbacks.get();
        if (callbacks == null) {
            Log.w(TAG, "LoaderTask running with no launcher");
            return;
        }
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        final ArrayList arrayList3 = new ArrayList();
        synchronized (this.mBgDataModel) {
            arrayList.addAll(this.mBgDataModel.workspaceItems);
            arrayList2.addAll(this.mBgDataModel.appWidgets);
            arrayList3.addAll(this.mBgDataModel.workspaceScreens);
            this.mBgDataModel.lastBindId++;
        }
        final int currentWorkspaceScreen = this.mPageToBindFirst != -1001 ? this.mPageToBindFirst : callbacks.getCurrentWorkspaceScreen();
        if (currentWorkspaceScreen >= arrayList3.size()) {
            currentWorkspaceScreen = -1001;
        }
        final boolean z = currentWorkspaceScreen >= 0;
        long jLongValue = z ? ((Long) arrayList3.get(currentWorkspaceScreen)).longValue() : -1L;
        ArrayList<ItemInfo> arrayList4 = new ArrayList<>();
        ArrayList<ItemInfo> arrayList5 = new ArrayList<>();
        ArrayList<LauncherAppWidgetInfo> arrayList6 = new ArrayList<>();
        ArrayList<LauncherAppWidgetInfo> arrayList7 = new ArrayList<>();
        filterCurrentWorkspaceItems(jLongValue, arrayList, arrayList4, arrayList5);
        filterCurrentWorkspaceItems(jLongValue, arrayList2, arrayList6, arrayList7);
        sortWorkspaceItemsSpatially(arrayList4);
        sortWorkspaceItemsSpatially(arrayList5);
        this.mUiExecutor.execute(new Runnable() { // from class: com.android.launcher3.model.LoaderResults.1
            @Override // java.lang.Runnable
            public void run() {
                LauncherModel.Callbacks callbacks2 = (LauncherModel.Callbacks) LoaderResults.this.mCallbacks.get();
                if (callbacks2 != null) {
                    callbacks2.clearPendingBinds();
                    callbacks2.startBinding();
                }
            }
        });
        this.mUiExecutor.execute(new Runnable() { // from class: com.android.launcher3.model.LoaderResults.2
            @Override // java.lang.Runnable
            public void run() {
                LauncherModel.Callbacks callbacks2 = (LauncherModel.Callbacks) LoaderResults.this.mCallbacks.get();
                if (callbacks2 != null) {
                    callbacks2.bindScreens(arrayList3);
                }
            }
        });
        Executor executor = this.mUiExecutor;
        bindWorkspaceItems(arrayList4, arrayList6, executor);
        final Executor viewOnDrawExecutor = z ? new ViewOnDrawExecutor() : executor;
        executor.execute(new Runnable() { // from class: com.android.launcher3.model.LoaderResults.3
            @Override // java.lang.Runnable
            public void run() {
                LauncherModel.Callbacks callbacks2 = (LauncherModel.Callbacks) LoaderResults.this.mCallbacks.get();
                if (callbacks2 != null) {
                    callbacks2.finishFirstPageBind(z ? (ViewOnDrawExecutor) viewOnDrawExecutor : null);
                }
            }
        });
        bindWorkspaceItems(arrayList5, arrayList7, viewOnDrawExecutor);
        viewOnDrawExecutor.execute(new Runnable() { // from class: com.android.launcher3.model.LoaderResults.4
            @Override // java.lang.Runnable
            public void run() {
                LauncherModel.Callbacks callbacks2 = (LauncherModel.Callbacks) LoaderResults.this.mCallbacks.get();
                if (callbacks2 != null) {
                    callbacks2.finishBindingItems();
                }
            }
        });
        if (z) {
            this.mUiExecutor.execute(new Runnable() { // from class: com.android.launcher3.model.LoaderResults.5
                @Override // java.lang.Runnable
                public void run() {
                    LauncherModel.Callbacks callbacks2 = (LauncherModel.Callbacks) LoaderResults.this.mCallbacks.get();
                    if (callbacks2 != null) {
                        if (currentWorkspaceScreen != -1001) {
                            callbacks2.onPageBoundSynchronously(currentWorkspaceScreen);
                        }
                        callbacks2.executeOnNextDraw((ViewOnDrawExecutor) viewOnDrawExecutor);
                    }
                }
            });
        }
    }

    public static <T extends ItemInfo> void filterCurrentWorkspaceItems(long j, ArrayList<T> arrayList, ArrayList<T> arrayList2, ArrayList<T> arrayList3) {
        Iterator<T> it = arrayList.iterator();
        while (it.hasNext()) {
            if (it.next() == null) {
                it.remove();
            }
        }
        HashSet hashSet = new HashSet();
        Collections.sort(arrayList, new Comparator<ItemInfo>() { // from class: com.android.launcher3.model.LoaderResults.6
            /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
            @Override // java.util.Comparator
            public int compare(ItemInfo itemInfo, ItemInfo itemInfo2) {
                return Utilities.longCompare(itemInfo.container, itemInfo2.container);
            }
        });
        Iterator<T> it2 = arrayList.iterator();
        while (it2.hasNext()) {
            T next = it2.next();
            if (next.container == -100) {
                if (next.screenId == j) {
                    arrayList2.add(next);
                    hashSet.add(Long.valueOf(next.id));
                } else {
                    arrayList3.add(next);
                }
            } else if (next.container == -101) {
                arrayList2.add(next);
                hashSet.add(Long.valueOf(next.id));
            } else if (hashSet.contains(Long.valueOf(next.container))) {
                arrayList2.add(next);
                hashSet.add(Long.valueOf(next.id));
            } else {
                arrayList3.add(next);
            }
        }
    }

    private void sortWorkspaceItemsSpatially(ArrayList<ItemInfo> arrayList) {
        InvariantDeviceProfile invariantDeviceProfile = this.mApp.getInvariantDeviceProfile();
        final int i = invariantDeviceProfile.numColumns;
        final int i2 = invariantDeviceProfile.numColumns * invariantDeviceProfile.numRows;
        Collections.sort(arrayList, new Comparator<ItemInfo>() { // from class: com.android.launcher3.model.LoaderResults.7
            /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
            @Override // java.util.Comparator
            public int compare(ItemInfo itemInfo, ItemInfo itemInfo2) {
                if (itemInfo.container == itemInfo2.container) {
                    switch ((int) itemInfo.container) {
                        case LauncherSettings.Favorites.CONTAINER_HOTSEAT /* -101 */:
                            return Utilities.longCompare(itemInfo.screenId, itemInfo2.screenId);
                        case -100:
                            return Utilities.longCompare((itemInfo.screenId * i2) + (itemInfo.cellY * i) + itemInfo.cellX, (itemInfo2.screenId * i2) + (itemInfo2.cellY * i) + itemInfo2.cellX);
                        default:
                            return 0;
                    }
                }
                return Utilities.longCompare(itemInfo.container, itemInfo2.container);
            }
        });
    }

    private void bindWorkspaceItems(final ArrayList<ItemInfo> arrayList, ArrayList<LauncherAppWidgetInfo> arrayList2, Executor executor) {
        final int i;
        int size = arrayList.size();
        final int i2 = 0;
        while (i2 < size) {
            int i3 = i2 + 6;
            if (i3 > size) {
                i = size - i2;
            } else {
                i = 6;
            }
            executor.execute(new Runnable() { // from class: com.android.launcher3.model.LoaderResults.8
                @Override // java.lang.Runnable
                public void run() {
                    LauncherModel.Callbacks callbacks = (LauncherModel.Callbacks) LoaderResults.this.mCallbacks.get();
                    if (callbacks != null) {
                        callbacks.bindItems(arrayList.subList(i2, i2 + i), false);
                    }
                }
            });
            i2 = i3;
        }
        int size2 = arrayList2.size();
        for (int i4 = 0; i4 < size2; i4++) {
            final LauncherAppWidgetInfo launcherAppWidgetInfo = arrayList2.get(i4);
            executor.execute(new Runnable() { // from class: com.android.launcher3.model.LoaderResults.9
                @Override // java.lang.Runnable
                public void run() {
                    LauncherModel.Callbacks callbacks = (LauncherModel.Callbacks) LoaderResults.this.mCallbacks.get();
                    if (callbacks != null) {
                        callbacks.bindItems(Collections.singletonList(launcherAppWidgetInfo), false);
                    }
                }
            });
        }
    }

    public void bindDeepShortcuts() {
        final MultiHashMap<ComponentKey, String> multiHashMapClone;
        synchronized (this.mBgDataModel) {
            multiHashMapClone = this.mBgDataModel.deepShortcutMap.clone();
        }
        this.mUiExecutor.execute(new Runnable() { // from class: com.android.launcher3.model.LoaderResults.10
            @Override // java.lang.Runnable
            public void run() {
                LauncherModel.Callbacks callbacks = (LauncherModel.Callbacks) LoaderResults.this.mCallbacks.get();
                if (callbacks != null) {
                    callbacks.bindDeepShortcutMap(multiHashMapClone);
                }
            }
        });
    }

    public void bindAllApps() {
        final ArrayList arrayList = (ArrayList) this.mBgAllAppsList.data.clone();
        this.mUiExecutor.execute(new Runnable() { // from class: com.android.launcher3.model.LoaderResults.11
            @Override // java.lang.Runnable
            public void run() {
                LauncherModel.Callbacks callbacks = (LauncherModel.Callbacks) LoaderResults.this.mCallbacks.get();
                if (callbacks != null) {
                    callbacks.bindAllApplications(arrayList);
                }
            }
        });
    }

    public void bindWidgets() {
        final ArrayList<WidgetListRowEntry> widgetsList = this.mBgDataModel.widgetsModel.getWidgetsList(this.mApp.getContext());
        this.mUiExecutor.execute(new Runnable() { // from class: com.android.launcher3.model.LoaderResults.12
            @Override // java.lang.Runnable
            public void run() {
                LauncherModel.Callbacks callbacks = (LauncherModel.Callbacks) LoaderResults.this.mCallbacks.get();
                if (callbacks != null) {
                    callbacks.bindAllWidgets(widgetsList);
                }
            }
        });
    }

    public LooperIdleLock newIdleLock(Object obj) {
        LooperIdleLock looperIdleLock = new LooperIdleLock(obj, Looper.getMainLooper());
        if (this.mCallbacks.get() == null) {
            looperIdleLock.queueIdle();
        }
        return looperIdleLock;
    }
}
