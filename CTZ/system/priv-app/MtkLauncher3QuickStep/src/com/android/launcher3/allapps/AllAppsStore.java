package com.android.launcher3.allapps;

import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.PromiseAppInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.PackageUserKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/* loaded from: classes.dex */
public class AllAppsStore {
    private PackageUserKey mTempKey = new PackageUserKey(null, null);
    private final HashMap<ComponentKey, AppInfo> mComponentToAppMap = new HashMap<>();
    private final List<OnUpdateListener> mUpdateListeners = new ArrayList();
    private final ArrayList<ViewGroup> mIconContainers = new ArrayList<>();
    private boolean mDeferUpdates = false;
    private boolean mUpdatePending = false;

    public interface IconAction {
        void apply(BubbleTextView bubbleTextView);
    }

    public interface OnUpdateListener {
        void onAppsUpdated();
    }

    public Collection<AppInfo> getApps() {
        return this.mComponentToAppMap.values();
    }

    public void setApps(List<AppInfo> list) {
        this.mComponentToAppMap.clear();
        addOrUpdateApps(list);
    }

    public AppInfo getApp(ComponentKey componentKey) {
        return this.mComponentToAppMap.get(componentKey);
    }

    public void setDeferUpdates(boolean z) {
        if (this.mDeferUpdates != z) {
            this.mDeferUpdates = z;
            if (!this.mDeferUpdates && this.mUpdatePending) {
                notifyUpdate();
                this.mUpdatePending = false;
            }
        }
    }

    public void addOrUpdateApps(List<AppInfo> list) {
        for (AppInfo appInfo : list) {
            if (appInfo.componentName.getPackageName().startsWith("com.android.settings") || appInfo.componentName.getPackageName().startsWith("com.android.cts.verifier") || appInfo.componentName.getPackageName().startsWith("jp.co.benesse.dcha.gp.calibration")) {
                this.mComponentToAppMap.put(appInfo.toComponentKey(), appInfo);
            }
        }
        notifyUpdate();
    }

    public void removeApps(List<AppInfo> list) {
        Iterator<AppInfo> it = list.iterator();
        while (it.hasNext()) {
            this.mComponentToAppMap.remove(it.next().toComponentKey());
        }
        notifyUpdate();
    }

    private void notifyUpdate() {
        if (this.mDeferUpdates) {
            this.mUpdatePending = true;
            return;
        }
        int size = this.mUpdateListeners.size();
        for (int i = 0; i < size; i++) {
            this.mUpdateListeners.get(i).onAppsUpdated();
        }
    }

    public void addUpdateListener(OnUpdateListener onUpdateListener) {
        this.mUpdateListeners.add(onUpdateListener);
    }

    public void removeUpdateListener(OnUpdateListener onUpdateListener) {
        this.mUpdateListeners.remove(onUpdateListener);
    }

    public void registerIconContainer(ViewGroup viewGroup) {
        if (viewGroup != null) {
            this.mIconContainers.add(viewGroup);
        }
    }

    public void unregisterIconContainer(ViewGroup viewGroup) {
        this.mIconContainers.remove(viewGroup);
    }

    public void updateIconBadges(final Set<PackageUserKey> set) {
        updateAllIcons(new IconAction() { // from class: com.android.launcher3.allapps.-$$Lambda$AllAppsStore$6bVp2gMnIq4ZTAQOmyy7A3JXVgc
            @Override // com.android.launcher3.allapps.AllAppsStore.IconAction
            public final void apply(BubbleTextView bubbleTextView) {
                AllAppsStore.lambda$updateIconBadges$0(this.f$0, set, bubbleTextView);
            }
        });
    }

    public static /* synthetic */ void lambda$updateIconBadges$0(AllAppsStore allAppsStore, Set set, BubbleTextView bubbleTextView) {
        if (bubbleTextView.getTag() instanceof ItemInfo) {
            ItemInfo itemInfo = (ItemInfo) bubbleTextView.getTag();
            if (allAppsStore.mTempKey.updateFromItemInfo(itemInfo) && set.contains(allAppsStore.mTempKey)) {
                bubbleTextView.applyBadgeState(itemInfo, true);
            }
        }
    }

    public void updatePromiseAppProgress(final PromiseAppInfo promiseAppInfo) {
        updateAllIcons(new IconAction() { // from class: com.android.launcher3.allapps.-$$Lambda$AllAppsStore$XmF5pBEgXE-njZ0HPoPNR9vf60k
            @Override // com.android.launcher3.allapps.AllAppsStore.IconAction
            public final void apply(BubbleTextView bubbleTextView) {
                AllAppsStore.lambda$updatePromiseAppProgress$1(promiseAppInfo, bubbleTextView);
            }
        });
    }

    static /* synthetic */ void lambda$updatePromiseAppProgress$1(PromiseAppInfo promiseAppInfo, BubbleTextView bubbleTextView) {
        if (bubbleTextView.getTag() == promiseAppInfo) {
            bubbleTextView.applyProgressLevel(promiseAppInfo.level);
        }
    }

    private void updateAllIcons(IconAction iconAction) {
        for (int size = this.mIconContainers.size() - 1; size >= 0; size--) {
            ViewGroup viewGroup = this.mIconContainers.get(size);
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = viewGroup.getChildAt(i);
                if (childAt instanceof BubbleTextView) {
                    iconAction.apply((BubbleTextView) childAt);
                }
            }
        }
    }
}
