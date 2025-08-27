package com.android.launcher3.shortcuts;

import android.annotation.TargetApi;
import android.util.ArrayMap;
import android.util.LruCache;
import java.util.Iterator;
import java.util.List;

@TargetApi(24)
/* loaded from: classes.dex */
public class ShortcutCache {
    private static final int CACHE_SIZE = 30;
    private final LruCache<ShortcutKey, ShortcutInfoCompat> mCachedShortcuts = new LruCache<>(30);
    private final ArrayMap<ShortcutKey, ShortcutInfoCompat> mPinnedShortcuts = new ArrayMap<>();

    public void removeShortcuts(List<ShortcutInfoCompat> list) {
        Iterator<ShortcutInfoCompat> it = list.iterator();
        while (it.hasNext()) {
            ShortcutKey shortcutKeyFromInfo = ShortcutKey.fromInfo(it.next());
            this.mCachedShortcuts.remove(shortcutKeyFromInfo);
            this.mPinnedShortcuts.remove(shortcutKeyFromInfo);
        }
    }

    public ShortcutInfoCompat get(ShortcutKey shortcutKey) {
        if (this.mPinnedShortcuts.containsKey(shortcutKey)) {
            return this.mPinnedShortcuts.get(shortcutKey);
        }
        return this.mCachedShortcuts.get(shortcutKey);
    }

    public void put(ShortcutKey shortcutKey, ShortcutInfoCompat shortcutInfoCompat) {
        if (shortcutInfoCompat.isPinned()) {
            this.mPinnedShortcuts.put(shortcutKey, shortcutInfoCompat);
        } else {
            this.mCachedShortcuts.put(shortcutKey, shortcutInfoCompat);
        }
    }
}
