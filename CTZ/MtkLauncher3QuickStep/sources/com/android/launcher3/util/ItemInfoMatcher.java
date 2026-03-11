package com.android.launcher3.util;

import android.content.ComponentName;
import android.os.UserHandle;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.shortcuts.ShortcutKey;
import java.util.HashSet;

public abstract class ItemInfoMatcher {
    public abstract boolean matches(ItemInfo itemInfo, ComponentName componentName);

    public final HashSet<ItemInfo> filterItemInfos(Iterable<ItemInfo> iterable) {
        LauncherAppWidgetInfo launcherAppWidgetInfo;
        ComponentName componentName;
        HashSet<ItemInfo> hashSet = new HashSet<>();
        for (ItemInfo itemInfo : iterable) {
            if (itemInfo instanceof ShortcutInfo) {
                ItemInfo itemInfo2 = (ShortcutInfo) itemInfo;
                ComponentName targetComponent = itemInfo2.getTargetComponent();
                if (targetComponent != null && matches(itemInfo2, targetComponent)) {
                    hashSet.add(itemInfo2);
                }
            } else if (itemInfo instanceof FolderInfo) {
                for (ItemInfo itemInfo3 : ((FolderInfo) itemInfo).contents) {
                    ComponentName targetComponent2 = itemInfo3.getTargetComponent();
                    if (targetComponent2 != null && matches(itemInfo3, targetComponent2)) {
                        hashSet.add(itemInfo3);
                    }
                }
            } else if ((itemInfo instanceof LauncherAppWidgetInfo) && (componentName = (launcherAppWidgetInfo = (LauncherAppWidgetInfo) itemInfo).providerName) != null && matches(launcherAppWidgetInfo, componentName)) {
                hashSet.add(launcherAppWidgetInfo);
            }
        }
        return hashSet;
    }

    public ItemInfoMatcher or(final ItemInfoMatcher itemInfoMatcher) {
        return new ItemInfoMatcher() {
            @Override
            public boolean matches(ItemInfo itemInfo, ComponentName componentName) {
                return this.matches(itemInfo, componentName) || itemInfoMatcher.matches(itemInfo, componentName);
            }
        };
    }

    public ItemInfoMatcher and(final ItemInfoMatcher itemInfoMatcher) {
        return new ItemInfoMatcher() {
            @Override
            public boolean matches(ItemInfo itemInfo, ComponentName componentName) {
                return this.matches(itemInfo, componentName) && itemInfoMatcher.matches(itemInfo, componentName);
            }
        };
    }

    public static ItemInfoMatcher not(ItemInfoMatcher itemInfoMatcher) {
        return new ItemInfoMatcher() {
            @Override
            public boolean matches(ItemInfo itemInfo, ComponentName componentName) {
                return !ItemInfoMatcher.this.matches(itemInfo, componentName);
            }
        };
    }

    public static ItemInfoMatcher ofUser(final UserHandle userHandle) {
        return new ItemInfoMatcher() {
            @Override
            public boolean matches(ItemInfo itemInfo, ComponentName componentName) {
                return itemInfo.user.equals(userHandle);
            }
        };
    }

    public static ItemInfoMatcher ofComponents(final HashSet<ComponentName> hashSet, final UserHandle userHandle) {
        return new ItemInfoMatcher() {
            @Override
            public boolean matches(ItemInfo itemInfo, ComponentName componentName) {
                return hashSet.contains(componentName) && itemInfo.user.equals(userHandle);
            }
        };
    }

    public static ItemInfoMatcher ofPackages(final HashSet<String> hashSet, final UserHandle userHandle) {
        return new ItemInfoMatcher() {
            @Override
            public boolean matches(ItemInfo itemInfo, ComponentName componentName) {
                return hashSet.contains(componentName.getPackageName()) && itemInfo.user.equals(userHandle);
            }
        };
    }

    public static ItemInfoMatcher ofShortcutKeys(final HashSet<ShortcutKey> hashSet) {
        return new ItemInfoMatcher() {
            @Override
            public boolean matches(ItemInfo itemInfo, ComponentName componentName) {
                return itemInfo.itemType == 6 && hashSet.contains(ShortcutKey.fromItemInfo(itemInfo));
            }
        };
    }

    public static ItemInfoMatcher ofItemIds(final LongArrayMap<Boolean> longArrayMap, final Boolean bool) {
        return new ItemInfoMatcher() {
            @Override
            public boolean matches(ItemInfo itemInfo, ComponentName componentName) {
                return ((Boolean) longArrayMap.get(itemInfo.id, bool)).booleanValue();
            }
        };
    }
}
