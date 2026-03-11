package com.android.launcher3.model;

import android.content.Context;
import android.os.UserHandle;
import com.android.launcher3.AllAppsList;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.shortcuts.ShortcutKey;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.Provider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ShortcutsChangedTask extends BaseModelUpdateTask {
    private final String mPackageName;
    private final List<ShortcutInfoCompat> mShortcuts;
    private final boolean mUpdateIdMap;
    private final UserHandle mUser;

    public ShortcutsChangedTask(String str, List<ShortcutInfoCompat> list, UserHandle userHandle, boolean z) {
        this.mPackageName = str;
        this.mShortcuts = list;
        this.mUser = userHandle;
        this.mUpdateIdMap = z;
    }

    @Override
    public void execute(LauncherAppState launcherAppState, BgDataModel bgDataModel, AllAppsList allAppsList) {
        Context context = launcherAppState.getContext();
        DeepShortcutManager deepShortcutManager = DeepShortcutManager.getInstance(context);
        deepShortcutManager.onShortcutsChanged(this.mShortcuts);
        HashSet hashSet = new HashSet();
        MultiHashMap multiHashMap = new MultiHashMap();
        HashSet hashSet2 = new HashSet();
        for (ItemInfo itemInfo : bgDataModel.itemsIdMap) {
            if (itemInfo.itemType == 6) {
                ShortcutInfo shortcutInfo = (ShortcutInfo) itemInfo;
                if (shortcutInfo.getIntent().getPackage().equals(this.mPackageName) && shortcutInfo.user.equals(this.mUser)) {
                    multiHashMap.addToList(ShortcutKey.fromItemInfo(shortcutInfo), shortcutInfo);
                    hashSet2.add(shortcutInfo.getDeepShortcutId());
                }
            }
        }
        ArrayList<ShortcutInfo> arrayList = new ArrayList<>();
        if (!multiHashMap.isEmpty()) {
            for (ShortcutInfoCompat shortcutInfoCompat : deepShortcutManager.queryForFullDetails(this.mPackageName, new ArrayList(hashSet2), this.mUser)) {
                ShortcutKey shortcutKeyFromInfo = ShortcutKey.fromInfo(shortcutInfoCompat);
                List<ShortcutInfo> listRemove = multiHashMap.remove(shortcutKeyFromInfo);
                if (!shortcutInfoCompat.isPinned()) {
                    hashSet.add(shortcutKeyFromInfo);
                } else {
                    for (ShortcutInfo shortcutInfo2 : listRemove) {
                        shortcutInfo2.updateFromDeepShortcutInfo(shortcutInfoCompat, context);
                        LauncherIcons launcherIconsObtain = LauncherIcons.obtain(context);
                        launcherIconsObtain.createShortcutIcon(shortcutInfoCompat, true, Provider.of(shortcutInfo2.iconBitmap)).applyTo(shortcutInfo2);
                        launcherIconsObtain.recycle();
                        arrayList.add(shortcutInfo2);
                    }
                }
            }
        }
        hashSet.addAll(multiHashMap.keySet());
        bindUpdatedShortcuts(arrayList, this.mUser);
        if (!multiHashMap.isEmpty()) {
            deleteAndBindComponentsRemoved(ItemInfoMatcher.ofShortcutKeys(hashSet));
        }
        if (this.mUpdateIdMap) {
            bgDataModel.updateDeepShortcutMap(this.mPackageName, this.mUser, this.mShortcuts);
            bindDeepShortcuts(bgDataModel);
        }
    }
}
