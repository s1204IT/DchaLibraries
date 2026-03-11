package com.android.launcher3;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import com.android.launcher3.LauncherProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AutoInstallsLayout {
    private static final String HOTSEAT_CONTAINER_NAME = LauncherSettings$Favorites.containerToString(-101);
    final AppWidgetHost mAppWidgetHost;
    protected final LayoutParserCallback mCallback;
    private final int mColumnCount;
    final Context mContext;
    protected SQLiteDatabase mDb;
    private final int mHotseatAllAppsRank;
    protected final int mLayoutId;
    protected final PackageManager mPackageManager;
    protected final String mRootTag;
    private final int mRowCount;
    protected final Resources mSourceRes;
    private final long[] mTemp = new long[2];
    final ContentValues mValues = new ContentValues();

    public interface LayoutParserCallback {
        long generateNewItemId();

        long insertAndCheck(SQLiteDatabase sQLiteDatabase, ContentValues contentValues);
    }

    protected interface TagParser {
        long parseAndAdd(XmlResourceParser xmlResourceParser) throws XmlPullParserException, IOException;
    }

    static AutoInstallsLayout get(Context context, AppWidgetHost appWidgetHost, LayoutParserCallback callback) {
        Pair<String, Resources> customizationApkInfo = Utilities.findSystemApk("android.autoinstalls.config.action.PLAY_AUTO_INSTALL", context.getPackageManager());
        if (customizationApkInfo == null) {
            return null;
        }
        return get(context, (String) customizationApkInfo.first, (Resources) customizationApkInfo.second, appWidgetHost, callback);
    }

    static AutoInstallsLayout get(Context context, String pkg, Resources targetRes, AppWidgetHost appWidgetHost, LayoutParserCallback callback) {
        InvariantDeviceProfile grid = LauncherAppState.getInstance().getInvariantDeviceProfile();
        String layoutName = String.format(Locale.ENGLISH, "default_layout_%dx%d_h%s", Integer.valueOf(grid.numColumns), Integer.valueOf(grid.numRows), Integer.valueOf(grid.numHotseatIcons));
        int layoutId = targetRes.getIdentifier(layoutName, "xml", pkg);
        if (layoutId == 0) {
            Log.d("AutoInstalls", "Formatted layout: " + layoutName + " not found. Trying layout without hosteat");
            layoutName = String.format(Locale.ENGLISH, "default_layout_%dx%d", Integer.valueOf(grid.numColumns), Integer.valueOf(grid.numRows));
            layoutId = targetRes.getIdentifier(layoutName, "xml", pkg);
        }
        if (layoutId == 0) {
            Log.d("AutoInstalls", "Formatted layout: " + layoutName + " not found. Trying the default layout");
            layoutId = targetRes.getIdentifier("default_layout", "xml", pkg);
        }
        if (layoutId == 0) {
            Log.e("AutoInstalls", "Layout definition not found in package: " + pkg);
            return null;
        }
        return new AutoInstallsLayout(context, appWidgetHost, callback, targetRes, layoutId, "workspace");
    }

    public AutoInstallsLayout(Context context, AppWidgetHost appWidgetHost, LayoutParserCallback callback, Resources res, int layoutId, String rootTag) {
        this.mContext = context;
        this.mAppWidgetHost = appWidgetHost;
        this.mCallback = callback;
        this.mPackageManager = context.getPackageManager();
        this.mRootTag = rootTag;
        this.mSourceRes = res;
        this.mLayoutId = layoutId;
        InvariantDeviceProfile idp = LauncherAppState.getInstance().getInvariantDeviceProfile();
        this.mHotseatAllAppsRank = idp.hotseatAllAppsRank;
        this.mRowCount = idp.numRows;
        this.mColumnCount = idp.numColumns;
    }

    public int loadLayout(SQLiteDatabase db, ArrayList<Long> screenIds) {
        this.mDb = db;
        try {
            return parseLayout(this.mLayoutId, screenIds);
        } catch (Exception e) {
            Log.w("AutoInstalls", "Got exception parsing layout.", e);
            return -1;
        }
    }

    protected int parseLayout(int layoutId, ArrayList<Long> screenIds) throws XmlPullParserException, IOException {
        XmlResourceParser parser = this.mSourceRes.getXml(layoutId);
        beginDocument(parser, this.mRootTag);
        int depth = parser.getDepth();
        HashMap<String, TagParser> tagParserMap = getLayoutElementsMap();
        int count = 0;
        while (true) {
            int type = parser.next();
            if ((type == 3 && parser.getDepth() <= depth) || type == 1) {
                break;
            }
            if (type == 2) {
                count += parseAndAddNode(parser, tagParserMap, screenIds);
            }
        }
        return count;
    }

    protected void parseContainerAndScreen(XmlResourceParser parser, long[] out) {
        if (HOTSEAT_CONTAINER_NAME.equals(getAttributeValue(parser, "container"))) {
            out[0] = -101;
            long rank = Long.parseLong(getAttributeValue(parser, "rank"));
            if (rank >= this.mHotseatAllAppsRank) {
                rank++;
            }
            out[1] = rank;
            return;
        }
        out[0] = -100;
        out[1] = Long.parseLong(getAttributeValue(parser, "screen"));
    }

    protected int parseAndAddNode(XmlResourceParser parser, HashMap<String, TagParser> tagParserMap, ArrayList<Long> screenIds) throws XmlPullParserException, IOException {
        if ("include".equals(parser.getName())) {
            int resId = getAttributeResourceValue(parser, "workspace", 0);
            if (resId != 0) {
                return parseLayout(resId, screenIds);
            }
            return 0;
        }
        this.mValues.clear();
        parseContainerAndScreen(parser, this.mTemp);
        long container = this.mTemp[0];
        long screenId = this.mTemp[1];
        this.mValues.put("container", Long.valueOf(container));
        this.mValues.put("screen", Long.valueOf(screenId));
        this.mValues.put("cellX", convertToDistanceFromEnd(getAttributeValue(parser, "x"), this.mColumnCount));
        this.mValues.put("cellY", convertToDistanceFromEnd(getAttributeValue(parser, "y"), this.mRowCount));
        TagParser tagParser = tagParserMap.get(parser.getName());
        if (tagParser == null) {
            return 0;
        }
        long newElementId = tagParser.parseAndAdd(parser);
        if (newElementId >= 0) {
            if (!screenIds.contains(Long.valueOf(screenId)) && container == -100) {
                screenIds.add(Long.valueOf(screenId));
                return 1;
            }
            return 1;
        }
        return 0;
    }

    protected long addShortcut(String title, Intent intent, int type) {
        long id = this.mCallback.generateNewItemId();
        this.mValues.put("intent", intent.toUri(0));
        this.mValues.put("title", title);
        this.mValues.put("itemType", Integer.valueOf(type));
        this.mValues.put("spanX", (Integer) 1);
        this.mValues.put("spanY", (Integer) 1);
        this.mValues.put("_id", Long.valueOf(id));
        if (this.mCallback.insertAndCheck(this.mDb, this.mValues) < 0) {
            return -1L;
        }
        return id;
    }

    protected HashMap<String, TagParser> getFolderElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap<>();
        parsers.put("appicon", new AppShortcutParser());
        parsers.put("autoinstall", new AutoInstallParser());
        parsers.put("shortcut", new ShortcutParser(this.mSourceRes));
        return parsers;
    }

    protected HashMap<String, TagParser> getLayoutElementsMap() {
        HashMap<String, TagParser> parsers = new HashMap<>();
        parsers.put("appicon", new AppShortcutParser());
        parsers.put("autoinstall", new AutoInstallParser());
        parsers.put("folder", new FolderParser(this));
        parsers.put("appwidget", new AppWidgetParser());
        parsers.put("shortcut", new ShortcutParser(this.mSourceRes));
        return parsers;
    }

    protected class AppShortcutParser implements TagParser {
        protected AppShortcutParser() {
        }

        @Override
        public long parseAndAdd(XmlResourceParser parser) {
            ComponentName cn;
            ActivityInfo info;
            String packageName = AutoInstallsLayout.getAttributeValue(parser, "packageName");
            String className = AutoInstallsLayout.getAttributeValue(parser, "className");
            if (!TextUtils.isEmpty(packageName)) {
                try {
                    if (!TextUtils.isEmpty(className)) {
                        try {
                            cn = new ComponentName(packageName, className);
                            info = AutoInstallsLayout.this.mPackageManager.getActivityInfo(cn, 0);
                        } catch (PackageManager.NameNotFoundException e) {
                            String[] packages = AutoInstallsLayout.this.mPackageManager.currentToCanonicalPackageNames(new String[]{packageName});
                            cn = new ComponentName(packages[0], className);
                            info = AutoInstallsLayout.this.mPackageManager.getActivityInfo(cn, 0);
                        }
                        Intent intent = new Intent("android.intent.action.MAIN", (Uri) null).addCategory("android.intent.category.LAUNCHER").setComponent(cn).setFlags(270532608);
                        return AutoInstallsLayout.this.addShortcut(info.loadLabel(AutoInstallsLayout.this.mPackageManager).toString(), intent, 0);
                    }
                } catch (PackageManager.NameNotFoundException e2) {
                    Log.e("AutoInstalls", "Unable to add favorite: " + packageName + "/" + className, e2);
                    return -1L;
                }
            }
            return invalidPackageOrClass(parser);
        }

        protected long invalidPackageOrClass(XmlResourceParser parser) {
            Log.w("AutoInstalls", "Skipping invalid <favorite> with no component");
            return -1L;
        }
    }

    protected class AutoInstallParser implements TagParser {
        protected AutoInstallParser() {
        }

        @Override
        public long parseAndAdd(XmlResourceParser parser) {
            String packageName = AutoInstallsLayout.getAttributeValue(parser, "packageName");
            String className = AutoInstallsLayout.getAttributeValue(parser, "className");
            if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(className)) {
                return -1L;
            }
            AutoInstallsLayout.this.mValues.put("restored", (Integer) 2);
            Intent intent = new Intent("android.intent.action.MAIN", (Uri) null).addCategory("android.intent.category.LAUNCHER").setComponent(new ComponentName(packageName, className)).setFlags(270532608);
            return AutoInstallsLayout.this.addShortcut(AutoInstallsLayout.this.mContext.getString(R.string.package_state_unknown), intent, 0);
        }
    }

    protected class ShortcutParser implements TagParser {
        private final Resources mIconRes;

        public ShortcutParser(Resources iconRes) {
            this.mIconRes = iconRes;
        }

        @Override
        public long parseAndAdd(XmlResourceParser parser) {
            Intent intent;
            Drawable icon;
            int titleResId = AutoInstallsLayout.getAttributeResourceValue(parser, "title", 0);
            int iconId = AutoInstallsLayout.getAttributeResourceValue(parser, "icon", 0);
            if (titleResId == 0 || iconId == 0) {
                return -1L;
            }
            int dcha_state = BenesseExtension.getDchaState();
            if (dcha_state == 0) {
                intent = parseIntent(parser);
            } else {
                intent = null;
            }
            if (intent == null || (icon = this.mIconRes.getDrawable(iconId)) == null) {
                return -1L;
            }
            ItemInfo.writeBitmap(AutoInstallsLayout.this.mValues, Utilities.createIconBitmap(icon, AutoInstallsLayout.this.mContext));
            AutoInstallsLayout.this.mValues.put("iconType", (Integer) 0);
            AutoInstallsLayout.this.mValues.put("iconPackage", this.mIconRes.getResourcePackageName(iconId));
            AutoInstallsLayout.this.mValues.put("iconResource", this.mIconRes.getResourceName(iconId));
            intent.setFlags(270532608);
            return AutoInstallsLayout.this.addShortcut(AutoInstallsLayout.this.mSourceRes.getString(titleResId), intent, 1);
        }

        protected Intent parseIntent(XmlResourceParser parser) {
            String url = AutoInstallsLayout.getAttributeValue(parser, "url");
            if (TextUtils.isEmpty(url) || !Patterns.WEB_URL.matcher(url).matches()) {
                return null;
            }
            return new Intent("android.intent.action.VIEW", (Uri) null).setData(Uri.parse(url));
        }
    }

    protected class AppWidgetParser implements TagParser {
        protected AppWidgetParser() {
        }

        @Override
        public long parseAndAdd(XmlResourceParser parser) throws XmlPullParserException, IOException {
            int appWidgetId;
            String packageName = AutoInstallsLayout.getAttributeValue(parser, "packageName");
            String className = AutoInstallsLayout.getAttributeValue(parser, "className");
            if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(className)) {
                return -1L;
            }
            ComponentName cn = new ComponentName(packageName, className);
            try {
                AutoInstallsLayout.this.mPackageManager.getReceiverInfo(cn, 0);
            } catch (Exception e) {
                String[] packages = AutoInstallsLayout.this.mPackageManager.currentToCanonicalPackageNames(new String[]{packageName});
                cn = new ComponentName(packages[0], className);
                try {
                    AutoInstallsLayout.this.mPackageManager.getReceiverInfo(cn, 0);
                } catch (Exception e2) {
                    return -1L;
                }
            }
            AutoInstallsLayout.this.mValues.put("spanX", AutoInstallsLayout.getAttributeValue(parser, "spanX"));
            AutoInstallsLayout.this.mValues.put("spanY", AutoInstallsLayout.getAttributeValue(parser, "spanY"));
            Bundle extras = new Bundle();
            int widgetDepth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type != 3 || parser.getDepth() > widgetDepth) {
                    if (type == 2) {
                        if ("extra".equals(parser.getName())) {
                            String key = AutoInstallsLayout.getAttributeValue(parser, "key");
                            String value = AutoInstallsLayout.getAttributeValue(parser, "value");
                            if (key == null || value == null) {
                                break;
                            }
                            extras.putString(key, value);
                        } else {
                            throw new RuntimeException("Widgets can contain only extras");
                        }
                    }
                } else {
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(AutoInstallsLayout.this.mContext);
                    long insertedId = -1;
                    try {
                        appWidgetId = AutoInstallsLayout.this.mAppWidgetHost.allocateAppWidgetId();
                    } catch (RuntimeException e3) {
                    }
                    if (!appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn)) {
                        return -1L;
                    }
                    AutoInstallsLayout.this.mValues.put("itemType", (Integer) 4);
                    AutoInstallsLayout.this.mValues.put("appWidgetId", Integer.valueOf(appWidgetId));
                    AutoInstallsLayout.this.mValues.put("appWidgetProvider", cn.flattenToString());
                    AutoInstallsLayout.this.mValues.put("_id", Long.valueOf(AutoInstallsLayout.this.mCallback.generateNewItemId()));
                    insertedId = AutoInstallsLayout.this.mCallback.insertAndCheck(AutoInstallsLayout.this.mDb, AutoInstallsLayout.this.mValues);
                    if (insertedId < 0) {
                        AutoInstallsLayout.this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                        return insertedId;
                    }
                    if (!extras.isEmpty()) {
                        Intent intent = new Intent("com.android.launcher.action.APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE");
                        intent.setComponent(cn);
                        intent.putExtras(extras);
                        intent.putExtra("appWidgetId", appWidgetId);
                        AutoInstallsLayout.this.mContext.sendBroadcast(intent);
                    }
                    return insertedId;
                }
            }
            throw new RuntimeException("Widget extras must have a key and value");
        }
    }

    protected class FolderParser implements TagParser {
        private final HashMap<String, TagParser> mFolderElements;

        public FolderParser(AutoInstallsLayout this$0) {
            this(this$0.getFolderElementsMap());
        }

        public FolderParser(HashMap<String, TagParser> elements) {
            this.mFolderElements = elements;
        }

        @Override
        public long parseAndAdd(XmlResourceParser parser) throws XmlPullParserException, IOException {
            String title;
            int titleResId = AutoInstallsLayout.getAttributeResourceValue(parser, "title", 0);
            if (titleResId != 0) {
                title = AutoInstallsLayout.this.mSourceRes.getString(titleResId);
            } else {
                title = AutoInstallsLayout.this.mContext.getResources().getString(R.string.folder_name);
            }
            AutoInstallsLayout.this.mValues.put("title", title);
            AutoInstallsLayout.this.mValues.put("itemType", (Integer) 2);
            AutoInstallsLayout.this.mValues.put("spanX", (Integer) 1);
            AutoInstallsLayout.this.mValues.put("spanY", (Integer) 1);
            AutoInstallsLayout.this.mValues.put("_id", Long.valueOf(AutoInstallsLayout.this.mCallback.generateNewItemId()));
            long folderId = AutoInstallsLayout.this.mCallback.insertAndCheck(AutoInstallsLayout.this.mDb, AutoInstallsLayout.this.mValues);
            if (folderId < 0) {
                return -1L;
            }
            ContentValues myValues = new ContentValues(AutoInstallsLayout.this.mValues);
            ArrayList<Long> folderItems = new ArrayList<>();
            int folderDepth = parser.getDepth();
            int rank = 0;
            while (true) {
                int type = parser.next();
                if (type != 3 || parser.getDepth() > folderDepth) {
                    if (type == 2) {
                        AutoInstallsLayout.this.mValues.clear();
                        AutoInstallsLayout.this.mValues.put("container", Long.valueOf(folderId));
                        AutoInstallsLayout.this.mValues.put("rank", Integer.valueOf(rank));
                        TagParser tagParser = this.mFolderElements.get(parser.getName());
                        if (tagParser != null) {
                            long id = tagParser.parseAndAdd(parser);
                            if (id >= 0) {
                                folderItems.add(Long.valueOf(id));
                                rank++;
                            }
                        } else {
                            throw new RuntimeException("Invalid folder item " + parser.getName());
                        }
                    }
                } else {
                    if (folderItems.size() >= 2) {
                        return folderId;
                    }
                    Uri uri = LauncherSettings$Favorites.getContentUri(folderId);
                    LauncherProvider.SqlArguments args = new LauncherProvider.SqlArguments(uri, null, null);
                    AutoInstallsLayout.this.mDb.delete(args.table, args.where, args.args);
                    if (folderItems.size() != 1) {
                        return -1L;
                    }
                    ContentValues childValues = new ContentValues();
                    AutoInstallsLayout.copyInteger(myValues, childValues, "container");
                    AutoInstallsLayout.copyInteger(myValues, childValues, "screen");
                    AutoInstallsLayout.copyInteger(myValues, childValues, "cellX");
                    AutoInstallsLayout.copyInteger(myValues, childValues, "cellY");
                    long addedId = folderItems.get(0).longValue();
                    AutoInstallsLayout.this.mDb.update("favorites", childValues, "_id=" + addedId, null);
                    return addedId;
                }
            }
        }
    }

    protected static final void beginDocument(XmlPullParser parser, String firstElementName) throws XmlPullParserException, IOException {
        int type;
        do {
            type = parser.next();
            if (type == 2) {
                break;
            }
        } while (type != 1);
        if (type != 2) {
            throw new XmlPullParserException("No start tag found");
        }
        if (parser.getName().equals(firstElementName)) {
        } else {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() + ", expected " + firstElementName);
        }
    }

    private static String convertToDistanceFromEnd(String value, int endValue) {
        int x;
        if (!TextUtils.isEmpty(value) && (x = Integer.parseInt(value)) < 0) {
            return Integer.toString(endValue + x);
        }
        return value;
    }

    protected static String getAttributeValue(XmlResourceParser parser, String attribute) {
        String value = parser.getAttributeValue("http://schemas.android.com/apk/res-auto/com.android.launcher3", attribute);
        if (value == null) {
            return parser.getAttributeValue(null, attribute);
        }
        return value;
    }

    protected static int getAttributeResourceValue(XmlResourceParser parser, String attribute, int defaultValue) {
        int value = parser.getAttributeResourceValue("http://schemas.android.com/apk/res-auto/com.android.launcher3", attribute, defaultValue);
        if (value == defaultValue) {
            return parser.getAttributeResourceValue(null, attribute, defaultValue);
        }
        return value;
    }

    static void copyInteger(ContentValues from, ContentValues to, String key) {
        to.put(key, from.getAsInteger(key));
    }
}
