package com.android.settingslib.drawer;

import android.R;
import android.app.ActivityManager;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.widget.RemoteViews;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TileUtils {
    private static final Comparator<DashboardCategory> CATEGORY_COMPARATOR = new Comparator<DashboardCategory>() {
        @Override
        public int compare(DashboardCategory dashboardCategory, DashboardCategory dashboardCategory2) {
            return dashboardCategory2.priority - dashboardCategory.priority;
        }
    };

    public static List<DashboardCategory> getCategories(Context context, Map<Pair<String, String>, Tile> map, boolean z, String str, String str2) {
        System.currentTimeMillis();
        boolean z2 = Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) != 0;
        ArrayList<Tile> arrayList = new ArrayList();
        for (UserHandle userHandle : ((UserManager) context.getSystemService("user")).getUserProfiles()) {
            if (userHandle.getIdentifier() == ActivityManager.getCurrentUser()) {
                getTilesForAction(context, userHandle, "com.android.settings.action.SETTINGS", map, null, arrayList, true, str2);
                getTilesForAction(context, userHandle, "com.android.settings.OPERATOR_APPLICATION_SETTING", map, "com.android.settings.category.wireless", arrayList, false, true, str2);
                getTilesForAction(context, userHandle, "com.android.settings.MANUFACTURER_APPLICATION_SETTING", map, "com.android.settings.category.device", arrayList, false, true, str2);
            }
            if (z2) {
                getTilesForAction(context, userHandle, "com.android.settings.action.EXTRA_SETTINGS", map, null, arrayList, false, str2);
                if (!z) {
                    getTilesForAction(context, userHandle, "com.android.settings.action.IA_SETTINGS", map, null, arrayList, false, str2);
                    if (str != null) {
                        getTilesForAction(context, userHandle, str, map, null, arrayList, false, str2);
                    }
                }
            }
        }
        HashMap map2 = new HashMap();
        for (Tile tile : arrayList) {
            DashboardCategory dashboardCategoryCreateCategory = (DashboardCategory) map2.get(tile.category);
            if (dashboardCategoryCreateCategory == null) {
                dashboardCategoryCreateCategory = createCategory(context, tile.category, z);
                if (dashboardCategoryCreateCategory == null) {
                    Log.w("TileUtils", "Couldn't find category " + tile.category);
                } else {
                    map2.put(dashboardCategoryCreateCategory.key, dashboardCategoryCreateCategory);
                }
            }
            dashboardCategoryCreateCategory.addTile(tile);
        }
        ArrayList arrayList2 = new ArrayList(map2.values());
        Iterator it = arrayList2.iterator();
        while (it.hasNext()) {
            ((DashboardCategory) it.next()).sortTiles();
        }
        Collections.sort(arrayList2, CATEGORY_COMPARATOR);
        return arrayList2;
    }

    private static DashboardCategory createCategory(Context context, String str, boolean z) {
        DashboardCategory dashboardCategory = new DashboardCategory();
        dashboardCategory.key = str;
        if (!z) {
            return dashboardCategory;
        }
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(new Intent(str), 0);
        if (listQueryIntentActivities.size() == 0) {
            return null;
        }
        for (ResolveInfo resolveInfo : listQueryIntentActivities) {
            if (resolveInfo.system) {
                dashboardCategory.title = resolveInfo.activityInfo.loadLabel(packageManager);
                dashboardCategory.priority = "com.android.settings".equals(resolveInfo.activityInfo.applicationInfo.packageName) ? resolveInfo.priority : 0;
            }
        }
        return dashboardCategory;
    }

    private static void getTilesForAction(Context context, UserHandle userHandle, String str, Map<Pair<String, String>, Tile> map, String str2, ArrayList<Tile> arrayList, boolean z, String str3) {
        getTilesForAction(context, userHandle, str, map, str2, arrayList, z, z, str3);
    }

    private static void getTilesForAction(Context context, UserHandle userHandle, String str, Map<Pair<String, String>, Tile> map, String str2, ArrayList<Tile> arrayList, boolean z, boolean z2, String str3) {
        Intent intent = new Intent(str);
        if (z) {
            intent.setPackage(str3);
        }
        getTilesForIntent(context, userHandle, intent, map, str2, arrayList, z2, true, true);
    }

    public static void getTilesForIntent(Context context, UserHandle userHandle, Intent intent, Map<Pair<String, String>, Tile> map, String str, List<Tile> list, boolean z, boolean z2, boolean z3) {
        getTilesForIntent(context, userHandle, intent, map, str, list, z, z2, z3, false);
    }

    public static void getTilesForIntent(Context context, UserHandle userHandle, Intent intent, Map<Pair<String, String>, Tile> map, String str, List<Tile> list, boolean z, boolean z2, boolean z3, boolean z4) {
        PackageManager packageManager;
        Intent intent2 = intent;
        PackageManager packageManager2 = context.getPackageManager();
        List<ResolveInfo> listQueryIntentActivitiesAsUser = packageManager2.queryIntentActivitiesAsUser(intent2, 128, userHandle.getIdentifier());
        HashMap map2 = new HashMap();
        for (ResolveInfo resolveInfo : listQueryIntentActivitiesAsUser) {
            if (resolveInfo.system) {
                ActivityInfo activityInfo = resolveInfo.activityInfo;
                Bundle bundle = activityInfo.metaData;
                if (z2 && ((bundle == null || !bundle.containsKey("com.android.settings.category")) && str == null)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Found ");
                    sb.append(resolveInfo.activityInfo.name);
                    sb.append(" for intent ");
                    sb.append(intent2);
                    sb.append(" missing metadata ");
                    sb.append(bundle == null ? "" : "com.android.settings.category");
                    Log.w("TileUtils", sb.toString());
                } else {
                    String string = bundle.getString("com.android.settings.category");
                    Pair<String, String> pair = new Pair<>(activityInfo.packageName, activityInfo.name);
                    Tile tile = map.get(pair);
                    if (tile == null) {
                        Tile tile2 = new Tile();
                        tile2.intent = new Intent().setClassName(activityInfo.packageName, activityInfo.name);
                        tile2.category = string;
                        tile2.priority = z ? resolveInfo.priority : 0;
                        tile2.metaData = activityInfo.metaData;
                        packageManager = packageManager2;
                        updateTileData(context, tile2, activityInfo, activityInfo.applicationInfo, packageManager2, map2, z3);
                        map.put(pair, tile2);
                        tile = tile2;
                    } else {
                        packageManager = packageManager2;
                        if (z4) {
                            updateSummaryAndTitle(context, map2, tile);
                        }
                    }
                    if (!tile.userHandle.contains(userHandle)) {
                        tile.userHandle.add(userHandle);
                    }
                    if (!list.contains(tile)) {
                        list.add(tile);
                    }
                    packageManager2 = packageManager;
                    intent2 = intent;
                }
            }
        }
    }

    private static boolean updateTileData(Context context, Tile tile, ActivityInfo activityInfo, ApplicationInfo applicationInfo, PackageManager packageManager, Map<String, IContentProvider> map, boolean z) {
        boolean z2;
        String str;
        String string;
        String string2;
        String string3;
        if (!applicationInfo.isSystemApp()) {
            return false;
        }
        String string4 = null;
        try {
            Resources resourcesForApplication = packageManager.getResourcesForApplication(applicationInfo.packageName);
            Bundle bundle = activityInfo.metaData;
            if (z) {
                boolean z3 = !context.getPackageName().equals(applicationInfo.packageName);
                boolean z4 = z3;
                if (resourcesForApplication != null && bundle != null) {
                    try {
                        i = bundle.containsKey("com.android.settings.icon") ? bundle.getInt("com.android.settings.icon") : 0;
                        if (bundle.containsKey("com.android.settings.icon_tintable")) {
                            if (z3) {
                                Log.w("TileUtils", "Ignoring icon tintable for " + activityInfo);
                                z2 = z4;
                                if (bundle.containsKey("com.android.settings.title")) {
                                }
                                if (bundle.containsKey("com.android.settings.summary")) {
                                }
                                if (bundle.containsKey("com.android.settings.keyhint")) {
                                }
                                if (bundle.containsKey("com.android.settings.custom_view")) {
                                }
                                str = string4;
                                string4 = string2;
                            } else {
                                z2 = bundle.getBoolean("com.android.settings.icon_tintable");
                                if (bundle.containsKey("com.android.settings.title")) {
                                }
                                if (bundle.containsKey("com.android.settings.summary")) {
                                }
                                if (bundle.containsKey("com.android.settings.keyhint")) {
                                }
                                if (bundle.containsKey("com.android.settings.custom_view")) {
                                }
                                str = string4;
                                string4 = string2;
                            }
                        } else {
                            z2 = z4;
                            try {
                                if (bundle.containsKey("com.android.settings.title")) {
                                    if (bundle.get("com.android.settings.title") instanceof Integer) {
                                        string2 = resourcesForApplication.getString(bundle.getInt("com.android.settings.title"));
                                    } else {
                                        string2 = bundle.getString("com.android.settings.title");
                                    }
                                } else {
                                    string2 = null;
                                }
                                try {
                                    if (bundle.containsKey("com.android.settings.summary")) {
                                        if (bundle.get("com.android.settings.summary") instanceof Integer) {
                                            string = resourcesForApplication.getString(bundle.getInt("com.android.settings.summary"));
                                        } else {
                                            string = bundle.getString("com.android.settings.summary");
                                        }
                                    } else {
                                        string = null;
                                    }
                                    try {
                                        if (bundle.containsKey("com.android.settings.keyhint")) {
                                            if (bundle.get("com.android.settings.keyhint") instanceof Integer) {
                                                string3 = resourcesForApplication.getString(bundle.getInt("com.android.settings.keyhint"));
                                            } else {
                                                string3 = bundle.getString("com.android.settings.keyhint");
                                            }
                                            string4 = string3;
                                        }
                                        if (bundle.containsKey("com.android.settings.custom_view")) {
                                            tile.remoteViews = new RemoteViews(applicationInfo.packageName, bundle.getInt("com.android.settings.custom_view"));
                                            updateSummaryAndTitle(context, map, tile);
                                        }
                                        str = string4;
                                        string4 = string2;
                                    } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                                        str = string4;
                                        string4 = string2;
                                    }
                                } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e2) {
                                    str = null;
                                    string = null;
                                }
                            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e3) {
                                str = null;
                                string = null;
                            }
                        }
                    } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e4) {
                        str = null;
                        string = null;
                        z2 = z4;
                    }
                } else {
                    str = null;
                    string = null;
                    z2 = z4;
                }
            }
        } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e5) {
            z2 = false;
        }
        if (TextUtils.isEmpty(string4)) {
            string4 = activityInfo.loadLabel(packageManager).toString();
        }
        if (i == 0 && !tile.metaData.containsKey("com.android.settings.icon_uri")) {
            i = activityInfo.icon;
        }
        if (i != 0) {
            tile.icon = Icon.createWithResource(activityInfo.packageName, i);
        }
        tile.title = string4;
        tile.summary = string;
        tile.intent = new Intent().setClassName(activityInfo.packageName, activityInfo.name);
        tile.key = str;
        tile.isIconTintable = z2;
        return true;
    }

    private static void updateSummaryAndTitle(Context context, Map<String, IContentProvider> map, Tile tile) {
        if (tile == null || tile.metaData == null || !tile.metaData.containsKey("com.android.settings.summary_uri")) {
            return;
        }
        Bundle bundleFromUri = getBundleFromUri(context, tile.metaData.getString("com.android.settings.summary_uri"), map);
        String string = getString(bundleFromUri, "com.android.settings.summary");
        String string2 = getString(bundleFromUri, "com.android.settings.title");
        if (string != null) {
            tile.remoteViews.setTextViewText(R.id.summary, string);
        }
        if (string2 != null) {
            tile.remoteViews.setTextViewText(R.id.title, string2);
        }
    }

    private static Bundle getBundleFromUri(Context context, String str, Map<String, IContentProvider> map) {
        IContentProvider providerFromUri;
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        Uri uri = Uri.parse(str);
        String methodFromUri = getMethodFromUri(uri);
        if (TextUtils.isEmpty(methodFromUri) || (providerFromUri = getProviderFromUri(context, uri, map)) == null) {
            return null;
        }
        try {
            return providerFromUri.call(context.getPackageName(), methodFromUri, str, (Bundle) null);
        } catch (RemoteException e) {
            return null;
        }
    }

    private static String getString(Bundle bundle, String str) {
        if (bundle == null) {
            return null;
        }
        return bundle.getString(str);
    }

    private static IContentProvider getProviderFromUri(Context context, Uri uri, Map<String, IContentProvider> map) {
        if (uri == null) {
            return null;
        }
        String authority = uri.getAuthority();
        if (TextUtils.isEmpty(authority)) {
            return null;
        }
        if (!map.containsKey(authority)) {
            map.put(authority, context.getContentResolver().acquireUnstableProvider(uri));
        }
        return map.get(authority);
    }

    static String getMethodFromUri(Uri uri) {
        List<String> pathSegments;
        if (uri == null || (pathSegments = uri.getPathSegments()) == null || pathSegments.isEmpty()) {
            return null;
        }
        return pathSegments.get(0);
    }
}
