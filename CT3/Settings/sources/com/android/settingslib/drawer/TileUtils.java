package com.android.settingslib.drawer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.internal.util.ArrayUtils;
import com.mediatek.settingslib.UtilsExt;
import com.mediatek.settingslib.ext.IDrawerExt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TileUtils {
    private static IDrawerExt sDrawerExt;
    private static final String[] EXTRA_PACKAGE_WHITE_LIST = {"com.mediatek.duraspeed"};
    private static final Comparator<Tile> TILE_COMPARATOR = new Comparator<Tile>() {
        @Override
        public int compare(Tile lhs, Tile rhs) {
            return rhs.priority - lhs.priority;
        }
    };
    private static final Comparator<DashboardCategory> CATEGORY_COMPARATOR = new Comparator<DashboardCategory>() {
        @Override
        public int compare(DashboardCategory lhs, DashboardCategory rhs) {
            return rhs.priority - lhs.priority;
        }
    };

    public static List<DashboardCategory> getCategories(Context context, HashMap<Pair<String, String>, Tile> cache) {
        System.currentTimeMillis();
        boolean setup = Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) != 0;
        ArrayList<Tile> tiles = new ArrayList<>();
        UserManager userManager = UserManager.get(context);
        for (UserHandle user : userManager.getUserProfiles()) {
            if (user.getIdentifier() == ActivityManager.getCurrentUser()) {
                getTilesForAction(context, user, "com.android.settings.action.SETTINGS", cache, null, tiles, true);
                getTilesForAction(context, user, "com.android.settings.OPERATOR_APPLICATION_SETTING", cache, "com.android.settings.category.wireless", tiles, false);
                getTilesForAction(context, user, "com.android.settings.MANUFACTURER_APPLICATION_SETTING", cache, "com.android.settings.category.device", tiles, false);
            }
            if (setup) {
                getTilesForAction(context, user, "com.android.settings.action.EXTRA_SETTINGS", cache, null, tiles, false);
            }
        }
        HashMap<String, DashboardCategory> categoryMap = new HashMap<>();
        for (Tile tile : tiles) {
            DashboardCategory category = categoryMap.get(tile.category);
            if (category == null) {
                category = createCategory(context, tile.category);
                if (category == null) {
                    Log.w("TileUtils", "Couldn't find category " + tile.category);
                } else {
                    categoryMap.put(category.key, category);
                }
            }
            category.addTile(tile);
        }
        ArrayList<DashboardCategory> categories = new ArrayList<>(categoryMap.values());
        Iterator category$iterator = categories.iterator();
        while (category$iterator.hasNext()) {
            Collections.sort(((DashboardCategory) category$iterator.next()).tiles, TILE_COMPARATOR);
        }
        Collections.sort(categories, CATEGORY_COMPARATOR);
        return categories;
    }

    private static DashboardCategory createCategory(Context context, String categoryKey) {
        DashboardCategory category = new DashboardCategory();
        category.key = categoryKey;
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> results = pm.queryIntentActivities(new Intent(categoryKey), 0);
        if (results.size() == 0) {
            return null;
        }
        for (ResolveInfo resolved : results) {
            if (resolved.system || ArrayUtils.contains(EXTRA_PACKAGE_WHITE_LIST, resolved.activityInfo.packageName)) {
                category.title = resolved.activityInfo.loadLabel(pm);
                category.priority = "com.android.settings".equals(resolved.activityInfo.applicationInfo.packageName) ? resolved.priority : 0;
            }
        }
        return category;
    }

    private static void getTilesForAction(Context context, UserHandle user, String action, Map<Pair<String, String>, Tile> addedCache, String defaultCategory, ArrayList<Tile> outTiles, boolean requireSettings) {
        Intent intent = new Intent(action);
        if (requireSettings) {
            intent.setPackage("com.android.settings");
        }
        getTilesForIntent(context, user, intent, addedCache, defaultCategory, outTiles, requireSettings, true);
    }

    public static void getTilesForIntent(Context context, UserHandle user, Intent intent, Map<Pair<String, String>, Tile> addedCache, String defaultCategory, List<Tile> outTiles, boolean usePriority, boolean checkCategory) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> results = pm.queryIntentActivitiesAsUser(intent, 128, user.getIdentifier());
        for (ResolveInfo resolved : results) {
            if (resolved.system || ArrayUtils.contains(EXTRA_PACKAGE_WHITE_LIST, resolved.activityInfo.packageName)) {
                ActivityInfo activityInfo = resolved.activityInfo;
                Bundle metaData = activityInfo.metaData;
                if (checkCategory && ((metaData == null || !metaData.containsKey("com.android.settings.category")) && defaultCategory == null)) {
                    Log.w("TileUtils", "Found " + resolved.activityInfo.name + " for intent " + intent + " missing metadata " + (metaData == null ? "" : "com.android.settings.category"));
                } else {
                    String categoryKey = metaData.getString("com.android.settings.category");
                    if (categoryKey == null) {
                        categoryKey = defaultCategory;
                    }
                    Pair<String, String> key = new Pair<>(activityInfo.packageName, activityInfo.name);
                    Tile tile = addedCache.get(key);
                    if (tile == null) {
                        tile = new Tile();
                        tile.intent = new Intent().setClassName(activityInfo.packageName, activityInfo.name);
                        tile.category = categoryKey;
                        tile.priority = usePriority ? resolved.priority : 0;
                        tile.metaData = activityInfo.metaData;
                        updateTileData(context, tile, activityInfo, activityInfo.applicationInfo, pm);
                        if (sDrawerExt == null) {
                            sDrawerExt = UtilsExt.getDrawerPlugin(context);
                        }
                        if (activityInfo.name.endsWith("PrivacySettingsActivity")) {
                            sDrawerExt.setFactoryResetTitle(tile);
                        } else if (activityInfo.name.endsWith("SimSettingsActivity")) {
                            tile.title = sDrawerExt.customizeSimDisplayString(tile.title.toString(), -1);
                        }
                        addedCache.put(key, tile);
                    }
                    if (!tile.userHandle.contains(user)) {
                        tile.userHandle.add(user);
                    }
                    if (!outTiles.contains(tile)) {
                        outTiles.add(tile);
                    }
                }
            }
        }
    }

    private static boolean updateTileData(Context context, Tile tile, ActivityInfo activityInfo, ApplicationInfo applicationInfo, PackageManager pm) {
        if (!applicationInfo.isSystemApp() && !ArrayUtils.contains(EXTRA_PACKAGE_WHITE_LIST, activityInfo.packageName)) {
            return false;
        }
        int icon = 0;
        CharSequence title = null;
        String summary = null;
        try {
            Resources res = pm.getResourcesForApplication(applicationInfo.packageName);
            Bundle metaData = activityInfo.metaData;
            if (res != null && metaData != null) {
                if (metaData.containsKey("com.android.settings.icon")) {
                    icon = metaData.getInt("com.android.settings.icon");
                }
                if (metaData.containsKey("com.android.settings.title")) {
                    if (metaData.get("com.android.settings.title") instanceof Integer) {
                        title = res.getString(metaData.getInt("com.android.settings.title"));
                    } else {
                        title = metaData.getString("com.android.settings.title");
                    }
                }
                if (metaData.containsKey("com.android.settings.summary")) {
                    summary = metaData.get("com.android.settings.summary") instanceof Integer ? res.getString(metaData.getInt("com.android.settings.summary")) : metaData.getString("com.android.settings.summary");
                }
            }
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
        }
        if (TextUtils.isEmpty(title)) {
            title = activityInfo.loadLabel(pm).toString();
        }
        if (icon == 0) {
            icon = activityInfo.icon;
        }
        tile.icon = Icon.createWithResource(activityInfo.packageName, icon);
        tile.title = title;
        tile.summary = summary;
        tile.intent = new Intent().setClassName(activityInfo.packageName, activityInfo.name);
        return true;
    }
}
