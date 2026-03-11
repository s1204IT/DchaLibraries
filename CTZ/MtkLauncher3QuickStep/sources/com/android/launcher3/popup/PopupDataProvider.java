package com.android.launcher3.popup;

import android.content.ComponentName;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;
import com.android.launcher3.badge.BadgeInfo;
import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.notification.NotificationKeyData;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.widget.WidgetListRowEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PopupDataProvider implements NotificationListener.NotificationsChangedListener {
    private static final boolean LOGD = false;
    private static final SystemShortcut[] SYSTEM_SHORTCUTS = {new SystemShortcut.AppInfo(), new SystemShortcut.Widgets(), new SystemShortcut.Install()};
    private static final String TAG = "PopupDataProvider";
    private final Launcher mLauncher;
    private MultiHashMap<ComponentKey, String> mDeepShortcutMap = new MultiHashMap<>();
    private Map<PackageUserKey, BadgeInfo> mPackageUserToBadgeInfos = new HashMap();
    private ArrayList<WidgetListRowEntry> mAllWidgets = new ArrayList<>();

    public PopupDataProvider(Launcher launcher) {
        this.mLauncher = launcher;
    }

    @Override
    public void onNotificationPosted(PackageUserKey packageUserKey, NotificationKeyData notificationKeyData, boolean z) {
        boolean zAddOrUpdateNotificationKey;
        BadgeInfo badgeInfo = this.mPackageUserToBadgeInfos.get(packageUserKey);
        if (badgeInfo == null) {
            if (!z) {
                BadgeInfo badgeInfo2 = new BadgeInfo(packageUserKey);
                badgeInfo2.addOrUpdateNotificationKey(notificationKeyData);
                this.mPackageUserToBadgeInfos.put(packageUserKey, badgeInfo2);
                zAddOrUpdateNotificationKey = true;
            } else {
                zAddOrUpdateNotificationKey = false;
            }
        } else {
            if (z) {
                zAddOrUpdateNotificationKey = badgeInfo.removeNotificationKey(notificationKeyData);
            } else {
                zAddOrUpdateNotificationKey = badgeInfo.addOrUpdateNotificationKey(notificationKeyData);
            }
            if (badgeInfo.getNotificationKeys().size() == 0) {
                this.mPackageUserToBadgeInfos.remove(packageUserKey);
            }
        }
        if (zAddOrUpdateNotificationKey) {
            this.mLauncher.updateIconBadges(Utilities.singletonHashSet(packageUserKey));
        }
    }

    @Override
    public void onNotificationRemoved(PackageUserKey packageUserKey, NotificationKeyData notificationKeyData) {
        BadgeInfo badgeInfo = this.mPackageUserToBadgeInfos.get(packageUserKey);
        if (badgeInfo != null && badgeInfo.removeNotificationKey(notificationKeyData)) {
            if (badgeInfo.getNotificationKeys().size() == 0) {
                this.mPackageUserToBadgeInfos.remove(packageUserKey);
            }
            this.mLauncher.updateIconBadges(Utilities.singletonHashSet(packageUserKey));
            trimNotifications(this.mPackageUserToBadgeInfos);
        }
    }

    @Override
    public void onNotificationFullRefresh(List<StatusBarNotification> list) {
        if (list == null) {
            return;
        }
        HashMap map = new HashMap(this.mPackageUserToBadgeInfos);
        this.mPackageUserToBadgeInfos.clear();
        for (StatusBarNotification statusBarNotification : list) {
            PackageUserKey packageUserKeyFromNotification = PackageUserKey.fromNotification(statusBarNotification);
            BadgeInfo badgeInfo = this.mPackageUserToBadgeInfos.get(packageUserKeyFromNotification);
            if (badgeInfo == null) {
                badgeInfo = new BadgeInfo(packageUserKeyFromNotification);
                this.mPackageUserToBadgeInfos.put(packageUserKeyFromNotification, badgeInfo);
            }
            badgeInfo.addOrUpdateNotificationKey(NotificationKeyData.fromNotification(statusBarNotification));
        }
        for (PackageUserKey packageUserKey : this.mPackageUserToBadgeInfos.keySet()) {
            BadgeInfo badgeInfo2 = (BadgeInfo) map.get(packageUserKey);
            BadgeInfo badgeInfo3 = this.mPackageUserToBadgeInfos.get(packageUserKey);
            if (badgeInfo2 == null) {
                map.put(packageUserKey, badgeInfo3);
            } else if (!badgeInfo2.shouldBeInvalidated(badgeInfo3)) {
                map.remove(packageUserKey);
            }
        }
        if (!map.isEmpty()) {
            this.mLauncher.updateIconBadges(map.keySet());
        }
        trimNotifications(map);
    }

    private void trimNotifications(Map<PackageUserKey, BadgeInfo> map) {
        PopupContainerWithArrow open = PopupContainerWithArrow.getOpen(this.mLauncher);
        if (open != null) {
            open.trimNotifications(map);
        }
    }

    public void setDeepShortcutMap(MultiHashMap<ComponentKey, String> multiHashMap) {
        this.mDeepShortcutMap = multiHashMap;
    }

    public List<String> getShortcutIdsForItem(ItemInfo itemInfo) {
        if (!DeepShortcutManager.supportsShortcuts(itemInfo)) {
            return Collections.EMPTY_LIST;
        }
        ComponentName targetComponent = itemInfo.getTargetComponent();
        if (targetComponent == null) {
            return Collections.EMPTY_LIST;
        }
        List<String> list = (List) this.mDeepShortcutMap.get(new ComponentKey(targetComponent, itemInfo.user));
        return list == null ? Collections.EMPTY_LIST : list;
    }

    public BadgeInfo getBadgeInfoForItem(ItemInfo itemInfo) {
        if (!DeepShortcutManager.supportsShortcuts(itemInfo)) {
            return null;
        }
        return this.mPackageUserToBadgeInfos.get(PackageUserKey.fromItemInfo(itemInfo));
    }

    @NonNull
    public List<NotificationKeyData> getNotificationKeysForItem(ItemInfo itemInfo) {
        BadgeInfo badgeInfoForItem = getBadgeInfoForItem(itemInfo);
        return badgeInfoForItem == null ? Collections.EMPTY_LIST : badgeInfoForItem.getNotificationKeys();
    }

    @NonNull
    public List<StatusBarNotification> getStatusBarNotificationsForKeys(List<NotificationKeyData> list) {
        NotificationListener instanceIfConnected = NotificationListener.getInstanceIfConnected();
        return instanceIfConnected == null ? Collections.EMPTY_LIST : instanceIfConnected.getNotificationsForKeys(list);
    }

    @NonNull
    public List<SystemShortcut> getEnabledSystemShortcutsForItem(ItemInfo itemInfo) {
        ArrayList arrayList = new ArrayList();
        for (SystemShortcut systemShortcut : SYSTEM_SHORTCUTS) {
            if (systemShortcut.getOnClickListener(this.mLauncher, itemInfo) != null) {
                arrayList.add(systemShortcut);
            }
        }
        return arrayList;
    }

    public void cancelNotification(String str) {
        NotificationListener instanceIfConnected = NotificationListener.getInstanceIfConnected();
        if (instanceIfConnected == null) {
            return;
        }
        instanceIfConnected.cancelNotificationFromLauncher(str);
    }

    public void setAllWidgets(ArrayList<WidgetListRowEntry> arrayList) {
        this.mAllWidgets = arrayList;
    }

    public ArrayList<WidgetListRowEntry> getAllWidgets() {
        return this.mAllWidgets;
    }

    public List<WidgetItem> getWidgetsForPackageUser(PackageUserKey packageUserKey) {
        for (WidgetListRowEntry widgetListRowEntry : this.mAllWidgets) {
            if (widgetListRowEntry.pkgItem.packageName.equals(packageUserKey.mPackageName)) {
                ArrayList arrayList = new ArrayList(widgetListRowEntry.widgets);
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    if (!((WidgetItem) it.next()).user.equals(packageUserKey.mUser)) {
                        it.remove();
                    }
                }
                if (arrayList.isEmpty()) {
                    return null;
                }
                return arrayList;
            }
        }
        return null;
    }
}
