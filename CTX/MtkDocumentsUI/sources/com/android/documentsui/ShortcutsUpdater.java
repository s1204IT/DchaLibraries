package com.android.documentsui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.util.Log;
import com.android.documentsui.base.Features;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.SharedMinimal;
import com.android.documentsui.files.FilesActivity;
import com.android.documentsui.prefs.ScopedPreferences;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class ShortcutsUpdater {
    private final Context mContext;
    private final Features mFeatures;
    private final ScopedPreferences mPrefs;

    public ShortcutsUpdater(Context context, ScopedPreferences scopedPreferences) {
        this.mContext = context;
        this.mPrefs = scopedPreferences;
        this.mFeatures = Features.create(this.mContext);
    }

    public void update(Collection<RootInfo> collection) {
        if (this.mFeatures != null && !this.mFeatures.isLauncherEnabled()) {
            if (SharedMinimal.DEBUG) {
                Log.d("ShortcutsUpdater", "Launcher is disabled");
                return;
            }
            return;
        }
        ShortcutManager shortcutManager = (ShortcutManager) this.mContext.getSystemService(ShortcutManager.class);
        Map<String, ShortcutInfo> pinnedShortcuts = getPinnedShortcuts(shortcutManager);
        List<ShortcutInfo> deviceShortcuts = getDeviceShortcuts(collection);
        ArrayList arrayList = new ArrayList();
        Iterator<ShortcutInfo> it = deviceShortcuts.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getId());
        }
        shortcutManager.setDynamicShortcuts(deviceShortcuts.subList(0, getNumDynSlots(shortcutManager, deviceShortcuts.size())));
        ArrayList arrayList2 = new ArrayList();
        for (String str : pinnedShortcuts.keySet()) {
            if (!arrayList.contains(str)) {
                arrayList2.add(str);
            }
        }
        shortcutManager.enableShortcuts(arrayList);
        shortcutManager.disableShortcuts(arrayList2);
    }

    private List<ShortcutInfo> getDeviceShortcuts(Collection<RootInfo> collection) {
        ArrayList arrayList = new ArrayList();
        for (RootInfo rootInfo : collection) {
            rootInfo.getUri().toString();
            if (rootInfo.isAdvanced() && rootInfo.authority.equals("com.android.externalstorage.documents")) {
                if (this.mPrefs.getShowDeviceRoot()) {
                    arrayList.add(0, createShortcut(rootInfo, R.drawable.ic_advanced_shortcut));
                }
            } else if (rootInfo.isAdvanced()) {
                arrayList.add(0, createShortcut(rootInfo, R.drawable.ic_folder_shortcut));
            }
        }
        return arrayList;
    }

    private Map<String, ShortcutInfo> getPinnedShortcuts(ShortcutManager shortcutManager) {
        HashMap map = new HashMap();
        for (ShortcutInfo shortcutInfo : shortcutManager.getDynamicShortcuts()) {
            map.put(shortcutInfo.getId(), shortcutInfo);
        }
        return map;
    }

    private int getNumDynSlots(ShortcutManager shortcutManager, int i) {
        int maxShortcutCountForActivity = shortcutManager.getMaxShortcutCountForActivity() - shortcutManager.getManifestShortcuts().size();
        return i >= maxShortcutCountForActivity ? maxShortcutCountForActivity : i;
    }

    private ShortcutInfo createShortcut(RootInfo rootInfo, int i) {
        Intent intent = new Intent(this.mContext, (Class<?>) FilesActivity.class);
        intent.setAction("android.intent.action.VIEW");
        intent.setData(rootInfo.getUri());
        return new ShortcutInfo.Builder(this.mContext, rootInfo.getUri().toString()).setShortLabel(rootInfo.title).setIcon(Icon.createWithResource(this.mContext, i)).setIntent(intent).build();
    }
}
