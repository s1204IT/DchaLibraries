package com.android.launcher2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import com.android.launcher.R;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

public class InstallShortcutReceiver extends BroadcastReceiver {
    private static Object sLock = new Object();
    private static boolean mUseInstallQueue = false;

    private static void addToStringSet(SharedPreferences sharedPrefs, SharedPreferences.Editor editor, String key, String value) {
        Set<String> strings;
        Set<String> strings2 = sharedPrefs.getStringSet(key, null);
        if (strings2 == null) {
            strings = new HashSet<>(0);
        } else {
            strings = new HashSet<>(strings2);
        }
        strings.add(value);
        editor.putStringSet(key, strings);
    }

    private static void addToInstallQueue(SharedPreferences sharedPrefs, PendingInstallShortcutInfo info) {
        synchronized (sLock) {
            try {
                JSONStringer json = new JSONStringer().object().key("intent.data").value(info.data.toUri(0)).key("intent.launch").value(info.launchIntent.toUri(0)).key("name").value(info.name);
                if (info.icon != null) {
                    byte[] iconByteArray = ItemInfo.flattenBitmap(info.icon);
                    json = json.key("icon").value(Base64.encodeToString(iconByteArray, 0, iconByteArray.length, 0));
                }
                if (info.iconResource != null) {
                    json = json.key("iconResource").value(info.iconResource.resourceName).key("iconResourcePackage").value(info.iconResource.packageName);
                }
                JSONStringer json2 = json.endObject();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                addToStringSet(sharedPrefs, editor, "apps_to_install", json2.toString());
                editor.commit();
            } catch (JSONException e) {
                Log.d("InstallShortcutReceiver", "Exception when adding shortcut: " + e);
            }
        }
    }

    private static ArrayList<PendingInstallShortcutInfo> getAndClearInstallQueue(SharedPreferences sharedPrefs) {
        ArrayList<PendingInstallShortcutInfo> infos;
        synchronized (sLock) {
            Set<String> strings = sharedPrefs.getStringSet("apps_to_install", null);
            if (strings == null) {
                infos = new ArrayList<>();
            } else {
                infos = new ArrayList<>();
                for (String json : strings) {
                    try {
                        JSONObject object = (JSONObject) new JSONTokener(json).nextValue();
                        Intent data = Intent.parseUri(object.getString("intent.data"), 0);
                        Intent launchIntent = Intent.parseUri(object.getString("intent.launch"), 0);
                        String name = object.getString("name");
                        String iconBase64 = object.optString("icon");
                        String iconResourceName = object.optString("iconResource");
                        String iconResourcePackageName = object.optString("iconResourcePackage");
                        if (iconBase64 != null && !iconBase64.isEmpty()) {
                            byte[] iconArray = Base64.decode(iconBase64, 0);
                            Bitmap b = BitmapFactory.decodeByteArray(iconArray, 0, iconArray.length);
                            data.putExtra("android.intent.extra.shortcut.ICON", b);
                        } else if (iconResourceName != null && !iconResourceName.isEmpty()) {
                            Intent.ShortcutIconResource iconResource = new Intent.ShortcutIconResource();
                            iconResource.resourceName = iconResourceName;
                            iconResource.packageName = iconResourcePackageName;
                            data.putExtra("android.intent.extra.shortcut.ICON_RESOURCE", iconResource);
                        }
                        data.putExtra("android.intent.extra.shortcut.INTENT", launchIntent);
                        PendingInstallShortcutInfo info = new PendingInstallShortcutInfo(data, name, launchIntent);
                        infos.add(info);
                    } catch (URISyntaxException e) {
                        Log.d("InstallShortcutReceiver", "Exception reading shortcut to add: " + e);
                    } catch (JSONException e2) {
                        Log.d("InstallShortcutReceiver", "Exception reading shortcut to add: " + e2);
                    }
                }
                sharedPrefs.edit().putStringSet("apps_to_install", new HashSet()).commit();
            }
        }
        return infos;
    }

    private static class PendingInstallShortcutInfo {
        Intent data;
        Bitmap icon;
        Intent.ShortcutIconResource iconResource;
        Intent launchIntent;
        String name;

        public PendingInstallShortcutInfo(Intent rawData, String shortcutName, Intent shortcutIntent) {
            this.data = rawData;
            this.name = shortcutName;
            this.launchIntent = shortcutIntent;
        }
    }

    @Override
    public void onReceive(Context context, Intent data) {
        Intent intent;
        if ("com.android.launcher.action.INSTALL_SHORTCUT".equals(data.getAction()) && (intent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT")) != null) {
            String name = data.getStringExtra("android.intent.extra.shortcut.NAME");
            if (name == null) {
                try {
                    PackageManager pm = context.getPackageManager();
                    name = pm.getActivityInfo(intent.getComponent(), 0).loadLabel(pm).toString();
                } catch (PackageManager.NameNotFoundException e) {
                    return;
                }
            }
            Bitmap icon = (Bitmap) data.getParcelableExtra("android.intent.extra.shortcut.ICON");
            Intent.ShortcutIconResource iconResource = (Intent.ShortcutIconResource) data.getParcelableExtra("android.intent.extra.shortcut.ICON_RESOURCE");
            boolean launcherNotLoaded = LauncherModel.getCellCountX() <= 0 || LauncherModel.getCellCountY() <= 0;
            PendingInstallShortcutInfo info = new PendingInstallShortcutInfo(data, name, intent);
            info.icon = icon;
            info.iconResource = iconResource;
            if (mUseInstallQueue || launcherNotLoaded) {
                String spKey = LauncherApplication.getSharedPreferencesKey();
                SharedPreferences sp = context.getSharedPreferences(spKey, 0);
                addToInstallQueue(sp, info);
                return;
            }
            processInstallShortcut(context, info);
        }
    }

    static void enableInstallQueue() {
        mUseInstallQueue = true;
    }

    static void disableAndFlushInstallQueue(Context context) {
        mUseInstallQueue = false;
        flushInstallQueue(context);
    }

    static void flushInstallQueue(Context context) {
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = context.getSharedPreferences(spKey, 0);
        ArrayList<PendingInstallShortcutInfo> installQueue = getAndClearInstallQueue(sp);
        Iterator<PendingInstallShortcutInfo> iter = installQueue.iterator();
        while (iter.hasNext()) {
            processInstallShortcut(context, iter.next());
        }
    }

    private static void processInstallShortcut(Context context, PendingInstallShortcutInfo pendingInfo) {
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = context.getSharedPreferences(spKey, 0);
        Intent data = pendingInfo.data;
        Intent intent = pendingInfo.launchIntent;
        String name = pendingInfo.name;
        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        int[] result = {0};
        boolean found = false;
        synchronized (app) {
            app.getModel().flushWorkerThread();
            ArrayList<ItemInfo> items = LauncherModel.getItemsInLocalCoordinates(context);
            boolean exists = LauncherModel.shortcutExists(context, name, intent);
            for (int i = 0; i < 11 && !found; i++) {
                int si = ((i % 2 == 1 ? 1 : -1) * ((int) ((i / 2.0f) + 0.5f))) + 2;
                if (si >= 0 && si < 5) {
                    found = installShortcut(context, data, items, name, intent, si, exists, sp, result);
                }
            }
        }
        if (!found) {
            if (result[0] == -2) {
                Toast.makeText(context, context.getString(R.string.completely_out_of_space), 0).show();
            } else if (result[0] == -1) {
                Toast.makeText(context, context.getString(R.string.shortcut_duplicate, name), 0).show();
            }
        }
    }

    private static boolean installShortcut(Context context, Intent data, ArrayList<ItemInfo> items, String name, final Intent intent, final int screen, boolean shortcutExists, final SharedPreferences sharedPrefs, int[] result) {
        int[] tmpCoordinates = new int[2];
        if (findEmptyCell(context, items, tmpCoordinates, screen)) {
            if (intent != null) {
                if (intent.getAction() == null) {
                    intent.setAction("android.intent.action.VIEW");
                } else if (intent.getAction().equals("android.intent.action.MAIN") && intent.getCategories() != null && intent.getCategories().contains("android.intent.category.LAUNCHER")) {
                    intent.addFlags(270532608);
                }
                boolean duplicate = data.getBooleanExtra("duplicate", true);
                if (duplicate || !shortcutExists) {
                    new Thread("setNewAppsThread") {
                        @Override
                        public void run() {
                            synchronized (InstallShortcutReceiver.sLock) {
                                int newAppsScreen = sharedPrefs.getInt("apps.new.page", screen);
                                SharedPreferences.Editor editor = sharedPrefs.edit();
                                if (newAppsScreen == -1 || newAppsScreen == screen) {
                                    InstallShortcutReceiver.addToStringSet(sharedPrefs, editor, "apps.new.list", intent.toUri(0));
                                }
                                editor.putInt("apps.new.page", screen);
                                editor.commit();
                            }
                        }
                    }.start();
                    LauncherApplication app = (LauncherApplication) context.getApplicationContext();
                    ShortcutInfo info = app.getModel().addShortcut(context, data, -100L, screen, tmpCoordinates[0], tmpCoordinates[1], true);
                    if (info == null) {
                        return false;
                    }
                } else {
                    result[0] = -1;
                }
                return true;
            }
        } else {
            result[0] = -2;
        }
        return false;
    }

    private static boolean findEmptyCell(Context context, ArrayList<ItemInfo> items, int[] xy, int screen) {
        int xCount = LauncherModel.getCellCountX();
        int yCount = LauncherModel.getCellCountY();
        boolean[][] occupied = (boolean[][]) Array.newInstance((Class<?>) Boolean.TYPE, xCount, yCount);
        for (int i = 0; i < items.size(); i++) {
            ItemInfo item = items.get(i);
            if (item.container == -100 && item.screen == screen) {
                int cellX = item.cellX;
                int cellY = item.cellY;
                int spanX = item.spanX;
                int spanY = item.spanY;
                for (int x = cellX; x >= 0 && x < cellX + spanX && x < xCount; x++) {
                    for (int y = cellY; y >= 0 && y < cellY + spanY && y < yCount; y++) {
                        occupied[x][y] = true;
                    }
                }
            }
        }
        return CellLayout.findVacantCell(xy, 1, 1, xCount, yCount, occupied);
    }
}
