package com.android.launcher3.popup;

import android.content.ComponentName;
import android.os.Handler;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.launcher3.notification.NotificationInfo;
import com.android.launcher3.notification.NotificationKeyData;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.DeepShortcutView;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.util.PackageUserKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class PopupPopulator {
    public static final int MAX_SHORTCUTS = 4;
    public static final int MAX_SHORTCUTS_IF_NOTIFICATIONS = 2;

    @VisibleForTesting
    static final int NUM_DYNAMIC = 2;
    private static final Comparator<ShortcutInfoCompat> SHORTCUT_RANK_COMPARATOR = new Comparator<ShortcutInfoCompat>() {
        @Override
        public int compare(ShortcutInfoCompat shortcutInfoCompat, ShortcutInfoCompat shortcutInfoCompat2) {
            if (shortcutInfoCompat.isDeclaredInManifest() && !shortcutInfoCompat2.isDeclaredInManifest()) {
                return -1;
            }
            if (!shortcutInfoCompat.isDeclaredInManifest() && shortcutInfoCompat2.isDeclaredInManifest()) {
                return 1;
            }
            return Integer.compare(shortcutInfoCompat.getRank(), shortcutInfoCompat2.getRank());
        }
    };

    public static List<ShortcutInfoCompat> sortAndFilterShortcuts(List<ShortcutInfoCompat> list, @Nullable String str) {
        if (str != null) {
            Iterator<ShortcutInfoCompat> it = list.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                if (it.next().getId().equals(str)) {
                    it.remove();
                    break;
                }
            }
        }
        Collections.sort(list, SHORTCUT_RANK_COMPARATOR);
        if (list.size() <= 4) {
            return list;
        }
        ArrayList arrayList = new ArrayList(4);
        int size = list.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            ShortcutInfoCompat shortcutInfoCompat = list.get(i2);
            int size2 = arrayList.size();
            if (size2 < 4) {
                arrayList.add(shortcutInfoCompat);
                if (shortcutInfoCompat.isDynamic()) {
                    i++;
                }
            } else if (shortcutInfoCompat.isDynamic() && i < 2) {
                i++;
                arrayList.remove(size2 - i);
                arrayList.add(shortcutInfoCompat);
            }
        }
        return arrayList;
    }

    public static Runnable createUpdateRunnable(final Launcher launcher, final ItemInfo itemInfo, final Handler handler, final PopupContainerWithArrow popupContainerWithArrow, final List<String> list, final List<DeepShortcutView> list2, final List<NotificationKeyData> list3) {
        final ComponentName targetComponent = itemInfo.getTargetComponent();
        final UserHandle userHandle = itemInfo.user;
        return new Runnable() {
            @Override
            public final void run() {
                PopupPopulator.lambda$createUpdateRunnable$3(list3, launcher, handler, popupContainerWithArrow, targetComponent, list, userHandle, list2, itemInfo);
            }
        };
    }

    static void lambda$createUpdateRunnable$3(List list, final Launcher launcher, Handler handler, final PopupContainerWithArrow popupContainerWithArrow, ComponentName componentName, List list2, UserHandle userHandle, List list3, final ItemInfo itemInfo) {
        if (!list.isEmpty()) {
            List<StatusBarNotification> statusBarNotificationsForKeys = launcher.getPopupDataProvider().getStatusBarNotificationsForKeys(list);
            final ArrayList arrayList = new ArrayList(statusBarNotificationsForKeys.size());
            for (int i = 0; i < statusBarNotificationsForKeys.size(); i++) {
                arrayList.add(new NotificationInfo(launcher, statusBarNotificationsForKeys.get(i)));
            }
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    popupContainerWithArrow.applyNotificationInfos(arrayList);
                }
            });
        }
        List<ShortcutInfoCompat> listSortAndFilterShortcuts = sortAndFilterShortcuts(DeepShortcutManager.getInstance(launcher).queryForShortcutsContainer(componentName, list2, userHandle), list.isEmpty() ? null : ((NotificationKeyData) list.get(0)).shortcutId);
        for (int i2 = 0; i2 < listSortAndFilterShortcuts.size() && i2 < list3.size(); i2++) {
            final ShortcutInfoCompat shortcutInfoCompat = listSortAndFilterShortcuts.get(i2);
            final ShortcutInfo shortcutInfo = new ShortcutInfo(shortcutInfoCompat, launcher);
            LauncherIcons launcherIconsObtain = LauncherIcons.obtain(launcher);
            launcherIconsObtain.createShortcutIcon(shortcutInfoCompat, false).applyTo(shortcutInfo);
            launcherIconsObtain.recycle();
            shortcutInfo.rank = i2;
            final DeepShortcutView deepShortcutView = (DeepShortcutView) list3.get(i2);
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    deepShortcutView.applyShortcutInfo(shortcutInfo, shortcutInfoCompat, popupContainerWithArrow);
                }
            });
        }
        handler.post(new Runnable() {
            @Override
            public final void run() {
                launcher.refreshAndBindWidgetsForPackageUser(PackageUserKey.fromItemInfo(itemInfo));
            }
        });
    }
}
