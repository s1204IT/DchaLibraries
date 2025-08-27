package com.android.launcher3.model;

import android.content.Context;
import android.os.UserHandle;
import com.android.launcher3.AllAppsList;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.shortcuts.ShortcutKey;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class UserLockStateChangedTask extends BaseModelUpdateTask {
    private final UserHandle mUser;

    public UserLockStateChangedTask(UserHandle userHandle) {
        this.mUser = userHandle;
    }

    @Override // com.android.launcher3.model.BaseModelUpdateTask
    public void execute(LauncherAppState launcherAppState, BgDataModel bgDataModel, AllAppsList allAppsList) {
        Context context = launcherAppState.getContext();
        boolean zIsUserUnlocked = UserManagerCompat.getInstance(context).isUserUnlocked(this.mUser);
        DeepShortcutManager deepShortcutManager = DeepShortcutManager.getInstance(context);
        HashMap map = new HashMap();
        if (zIsUserUnlocked) {
            List<ShortcutInfoCompat> listQueryForPinnedShortcuts = deepShortcutManager.queryForPinnedShortcuts(null, this.mUser);
            if (deepShortcutManager.wasLastCallSuccess()) {
                for (ShortcutInfoCompat shortcutInfoCompat : listQueryForPinnedShortcuts) {
                    map.put(ShortcutKey.fromInfo(shortcutInfoCompat), shortcutInfoCompat);
                }
            } else {
                zIsUserUnlocked = false;
            }
        }
        ArrayList<ShortcutInfo> arrayList = new ArrayList<>();
        HashSet hashSet = new HashSet();
        Iterator<ItemInfo> it = bgDataModel.itemsIdMap.iterator();
        while (it.hasNext()) {
            ItemInfo next = it.next();
            if (next.itemType == 6 && this.mUser.equals(next.user)) {
                ShortcutInfo shortcutInfo = (ShortcutInfo) next;
                if (zIsUserUnlocked) {
                    ShortcutKey shortcutKeyFromItemInfo = ShortcutKey.fromItemInfo(shortcutInfo);
                    ShortcutInfoCompat shortcutInfoCompat2 = (ShortcutInfoCompat) map.get(shortcutKeyFromItemInfo);
                    if (shortcutInfoCompat2 == null) {
                        hashSet.add(shortcutKeyFromItemInfo);
                    } else {
                        shortcutInfo.runtimeStatusFlags &= -33;
                        shortcutInfo.updateFromDeepShortcutInfo(shortcutInfoCompat2, context);
                        LauncherIcons launcherIconsObtain = LauncherIcons.obtain(context);
                        launcherIconsObtain.createShortcutIcon(shortcutInfoCompat2, true, Provider.of(shortcutInfo.iconBitmap)).applyTo(shortcutInfo);
                        launcherIconsObtain.recycle();
                    }
                } else {
                    shortcutInfo.runtimeStatusFlags |= 32;
                }
                arrayList.add(shortcutInfo);
            }
        }
        bindUpdatedShortcuts(arrayList, this.mUser);
        if (!hashSet.isEmpty()) {
            deleteAndBindComponentsRemoved(ItemInfoMatcher.ofShortcutKeys(hashSet));
        }
        Iterator<ComponentKey> it2 = bgDataModel.deepShortcutMap.keySet().iterator();
        while (it2.hasNext()) {
            if (it2.next().user.equals(this.mUser)) {
                it2.remove();
            }
        }
        if (zIsUserUnlocked) {
            bgDataModel.updateDeepShortcutMap(null, this.mUser, deepShortcutManager.queryForAllShortcuts(this.mUser));
        }
        bindDeepShortcuts(bgDataModel);
    }
}
