package com.android.launcher3;

import android.appwidget.AppWidgetHost;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import com.android.launcher3.LauncherProvider;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.graphics.LauncherIcons;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AutoInstallsLayout {
    private static final String ACTION_APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE = "com.android.launcher.action.APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE";
    static final String ACTION_LAUNCHER_CUSTOMIZATION = "android.autoinstalls.config.action.PLAY_AUTO_INSTALL";
    private static final String ATTR_CLASS_NAME = "className";
    private static final String ATTR_CONTAINER = "container";
    private static final String ATTR_ICON = "icon";
    private static final String ATTR_KEY = "key";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_RANK = "rank";
    private static final String ATTR_SCREEN = "screen";
    private static final String ATTR_SPAN_X = "spanX";
    private static final String ATTR_SPAN_Y = "spanY";
    private static final String ATTR_TITLE = "title";
    private static final String ATTR_URL = "url";
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_WORKSPACE = "workspace";
    private static final String ATTR_X = "x";
    private static final String ATTR_Y = "y";
    private static final String FORMATTED_LAYOUT_RES = "default_layout_%dx%d";
    private static final String FORMATTED_LAYOUT_RES_WITH_HOSTEAT = "default_layout_%dx%d_h%s";
    private static final String HOTSEAT_CONTAINER_NAME = LauncherSettings.Favorites.containerToString(LauncherSettings.Favorites.CONTAINER_HOTSEAT);
    private static final String LAYOUT_RES = "default_layout";
    private static final boolean LOGD = false;
    private static final String TAG = "AutoInstalls";
    private static final String TAG_APPWIDGET = "appwidget";
    private static final String TAG_APP_ICON = "appicon";
    private static final String TAG_AUTO_INSTALL = "autoinstall";
    private static final String TAG_EXTRA = "extra";
    private static final String TAG_FOLDER = "folder";
    private static final String TAG_INCLUDE = "include";
    private static final String TAG_SHORTCUT = "shortcut";
    private static final String TAG_WORKSPACE = "workspace";
    final AppWidgetHost mAppWidgetHost;
    protected final LayoutParserCallback mCallback;
    private final int mColumnCount;
    final Context mContext;
    protected SQLiteDatabase mDb;
    private final InvariantDeviceProfile mIdp;
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

    public interface TagParser {
        long parseAndAdd(XmlResourceParser xmlResourceParser) throws XmlPullParserException, IOException;
    }

    static AutoInstallsLayout get(Context context, AppWidgetHost appWidgetHost, LayoutParserCallback layoutParserCallback) {
        Pair<String, Resources> pairFindSystemApk = Utilities.findSystemApk(ACTION_LAUNCHER_CUSTOMIZATION, context.getPackageManager());
        if (pairFindSystemApk == null) {
            return null;
        }
        return get(context, (String) pairFindSystemApk.first, (Resources) pairFindSystemApk.second, appWidgetHost, layoutParserCallback);
    }

    static AutoInstallsLayout get(Context context, String str, Resources resources, AppWidgetHost appWidgetHost, LayoutParserCallback layoutParserCallback) {
        InvariantDeviceProfile idp = LauncherAppState.getIDP(context);
        String str2 = String.format(Locale.ENGLISH, FORMATTED_LAYOUT_RES_WITH_HOSTEAT, Integer.valueOf(idp.numColumns), Integer.valueOf(idp.numRows), Integer.valueOf(idp.numHotseatIcons));
        int identifier = resources.getIdentifier(str2, "xml", str);
        if (identifier == 0) {
            Log.d(TAG, "Formatted layout: " + str2 + " not found. Trying layout without hosteat");
            str2 = String.format(Locale.ENGLISH, FORMATTED_LAYOUT_RES, Integer.valueOf(idp.numColumns), Integer.valueOf(idp.numRows));
            identifier = resources.getIdentifier(str2, "xml", str);
        }
        if (identifier == 0) {
            Log.d(TAG, "Formatted layout: " + str2 + " not found. Trying the default layout");
            identifier = resources.getIdentifier(LAYOUT_RES, "xml", str);
        }
        int i = identifier;
        if (i == 0) {
            Log.e(TAG, "Layout definition not found in package: " + str);
            return null;
        }
        return new AutoInstallsLayout(context, appWidgetHost, layoutParserCallback, resources, i, "workspace");
    }

    public AutoInstallsLayout(Context context, AppWidgetHost appWidgetHost, LayoutParserCallback layoutParserCallback, Resources resources, int i, String str) {
        this.mContext = context;
        this.mAppWidgetHost = appWidgetHost;
        this.mCallback = layoutParserCallback;
        this.mPackageManager = context.getPackageManager();
        this.mRootTag = str;
        this.mSourceRes = resources;
        this.mLayoutId = i;
        this.mIdp = LauncherAppState.getIDP(context);
        this.mRowCount = this.mIdp.numRows;
        this.mColumnCount = this.mIdp.numColumns;
    }

    public int loadLayout(SQLiteDatabase sQLiteDatabase, ArrayList<Long> arrayList) {
        this.mDb = sQLiteDatabase;
        try {
            return parseLayout(this.mLayoutId, arrayList);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing layout: " + e);
            return -1;
        }
    }

    protected int parseLayout(int i, ArrayList<Long> arrayList) throws XmlPullParserException, IOException {
        XmlResourceParser xml = this.mSourceRes.getXml(i);
        beginDocument(xml, this.mRootTag);
        int depth = xml.getDepth();
        ArrayMap<String, TagParser> layoutElementsMap = getLayoutElementsMap();
        int andAddNode = 0;
        while (true) {
            int next = xml.next();
            if ((next == 3 && xml.getDepth() <= depth) || next == 1) {
                break;
            }
            if (next == 2) {
                andAddNode += parseAndAddNode(xml, layoutElementsMap, arrayList);
            }
        }
        return andAddNode;
    }

    protected void parseContainerAndScreen(XmlResourceParser xmlResourceParser, long[] jArr) {
        if (HOTSEAT_CONTAINER_NAME.equals(getAttributeValue(xmlResourceParser, "container"))) {
            jArr[0] = -101;
            jArr[1] = Long.parseLong(getAttributeValue(xmlResourceParser, "rank"));
        } else {
            jArr[0] = -100;
            jArr[1] = Long.parseLong(getAttributeValue(xmlResourceParser, "screen"));
        }
    }

    protected int parseAndAddNode(XmlResourceParser xmlResourceParser, ArrayMap<String, TagParser> arrayMap, ArrayList<Long> arrayList) throws XmlPullParserException, IOException {
        if (TAG_INCLUDE.equals(xmlResourceParser.getName())) {
            int attributeResourceValue = getAttributeResourceValue(xmlResourceParser, "workspace", 0);
            if (attributeResourceValue != 0) {
                return parseLayout(attributeResourceValue, arrayList);
            }
            return 0;
        }
        this.mValues.clear();
        parseContainerAndScreen(xmlResourceParser, this.mTemp);
        long j = this.mTemp[0];
        long j2 = this.mTemp[1];
        this.mValues.put("container", Long.valueOf(j));
        this.mValues.put("screen", Long.valueOf(j2));
        this.mValues.put(LauncherSettings.Favorites.CELLX, convertToDistanceFromEnd(getAttributeValue(xmlResourceParser, ATTR_X), this.mColumnCount));
        this.mValues.put(LauncherSettings.Favorites.CELLY, convertToDistanceFromEnd(getAttributeValue(xmlResourceParser, ATTR_Y), this.mRowCount));
        TagParser tagParser = arrayMap.get(xmlResourceParser.getName());
        if (tagParser == null || tagParser.parseAndAdd(xmlResourceParser) < 0) {
            return 0;
        }
        if (!arrayList.contains(Long.valueOf(j2)) && j == -100) {
            arrayList.add(Long.valueOf(j2));
        }
        return 1;
    }

    protected long addShortcut(String str, Intent intent, int i) {
        long jGenerateNewItemId = this.mCallback.generateNewItemId();
        this.mValues.put(LauncherSettings.BaseLauncherColumns.INTENT, intent.toUri(0));
        this.mValues.put("title", str);
        this.mValues.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, Integer.valueOf(i));
        this.mValues.put("spanX", (Integer) 1);
        this.mValues.put("spanY", (Integer) 1);
        this.mValues.put("_id", Long.valueOf(jGenerateNewItemId));
        if (this.mCallback.insertAndCheck(this.mDb, this.mValues) < 0) {
            return -1L;
        }
        return jGenerateNewItemId;
    }

    protected ArrayMap<String, TagParser> getFolderElementsMap() {
        ArrayMap<String, TagParser> arrayMap = new ArrayMap<>();
        arrayMap.put(TAG_APP_ICON, new AppShortcutParser());
        arrayMap.put(TAG_AUTO_INSTALL, new AutoInstallParser());
        arrayMap.put(TAG_SHORTCUT, new ShortcutParser(this.mSourceRes));
        return arrayMap;
    }

    protected ArrayMap<String, TagParser> getLayoutElementsMap() {
        ArrayMap<String, TagParser> arrayMap = new ArrayMap<>();
        arrayMap.put(TAG_APP_ICON, new AppShortcutParser());
        arrayMap.put(TAG_AUTO_INSTALL, new AutoInstallParser());
        arrayMap.put(TAG_FOLDER, new FolderParser(this));
        arrayMap.put(TAG_APPWIDGET, new PendingWidgetParser());
        arrayMap.put(TAG_SHORTCUT, new ShortcutParser(this.mSourceRes));
        return arrayMap;
    }

    protected class AppShortcutParser implements TagParser {
        protected AppShortcutParser() {
        }

        @Override
        public long parseAndAdd(XmlResourceParser xmlResourceParser) {
            ActivityInfo activityInfo;
            ComponentName componentName;
            String attributeValue = AutoInstallsLayout.getAttributeValue(xmlResourceParser, AutoInstallsLayout.ATTR_PACKAGE_NAME);
            String attributeValue2 = AutoInstallsLayout.getAttributeValue(xmlResourceParser, AutoInstallsLayout.ATTR_CLASS_NAME);
            if (!TextUtils.isEmpty(attributeValue) && !TextUtils.isEmpty(attributeValue2)) {
                try {
                    try {
                        componentName = new ComponentName(attributeValue, attributeValue2);
                        activityInfo = AutoInstallsLayout.this.mPackageManager.getActivityInfo(componentName, 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        ComponentName componentName2 = new ComponentName(AutoInstallsLayout.this.mPackageManager.currentToCanonicalPackageNames(new String[]{attributeValue})[0], attributeValue2);
                        activityInfo = AutoInstallsLayout.this.mPackageManager.getActivityInfo(componentName2, 0);
                        componentName = componentName2;
                    }
                    return AutoInstallsLayout.this.addShortcut(activityInfo.loadLabel(AutoInstallsLayout.this.mPackageManager).toString(), new Intent("android.intent.action.MAIN", (Uri) null).addCategory("android.intent.category.LAUNCHER").setComponent(componentName).setFlags(270532608), 0);
                } catch (PackageManager.NameNotFoundException e2) {
                    Log.e(AutoInstallsLayout.TAG, "Favorite not found: " + attributeValue + "/" + attributeValue2);
                    return -1L;
                }
            }
            return invalidPackageOrClass(xmlResourceParser);
        }

        protected long invalidPackageOrClass(XmlResourceParser xmlResourceParser) {
            Log.w(AutoInstallsLayout.TAG, "Skipping invalid <favorite> with no component");
            return -1L;
        }
    }

    protected class AutoInstallParser implements TagParser {
        protected AutoInstallParser() {
        }

        @Override
        public long parseAndAdd(XmlResourceParser xmlResourceParser) {
            String attributeValue = AutoInstallsLayout.getAttributeValue(xmlResourceParser, AutoInstallsLayout.ATTR_PACKAGE_NAME);
            String attributeValue2 = AutoInstallsLayout.getAttributeValue(xmlResourceParser, AutoInstallsLayout.ATTR_CLASS_NAME);
            if (TextUtils.isEmpty(attributeValue) || TextUtils.isEmpty(attributeValue2)) {
                return -1L;
            }
            AutoInstallsLayout.this.mValues.put(LauncherSettings.Favorites.RESTORED, (Integer) 2);
            return AutoInstallsLayout.this.addShortcut(AutoInstallsLayout.this.mContext.getString(R.string.package_state_unknown), new Intent("android.intent.action.MAIN", (Uri) null).addCategory("android.intent.category.LAUNCHER").setComponent(new ComponentName(attributeValue, attributeValue2)).setFlags(270532608), 0);
        }
    }

    protected class ShortcutParser implements TagParser {
        private final Resources mIconRes;

        public ShortcutParser(Resources resources) {
            this.mIconRes = resources;
        }

        @Override
        public long parseAndAdd(XmlResourceParser xmlResourceParser) {
            Intent intent;
            Drawable drawable;
            int attributeResourceValue = AutoInstallsLayout.getAttributeResourceValue(xmlResourceParser, "title", 0);
            int attributeResourceValue2 = AutoInstallsLayout.getAttributeResourceValue(xmlResourceParser, "icon", 0);
            if (attributeResourceValue == 0 || attributeResourceValue2 == 0 || (intent = parseIntent(xmlResourceParser)) == null || (drawable = this.mIconRes.getDrawable(attributeResourceValue2)) == null) {
                return -1L;
            }
            LauncherIcons launcherIconsObtain = LauncherIcons.obtain(AutoInstallsLayout.this.mContext);
            AutoInstallsLayout.this.mValues.put("icon", Utilities.flattenBitmap(launcherIconsObtain.createBadgedIconBitmap(drawable, Process.myUserHandle(), Build.VERSION.SDK_INT).icon));
            launcherIconsObtain.recycle();
            AutoInstallsLayout.this.mValues.put(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE, this.mIconRes.getResourcePackageName(attributeResourceValue2));
            AutoInstallsLayout.this.mValues.put(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE, this.mIconRes.getResourceName(attributeResourceValue2));
            intent.setFlags(270532608);
            return AutoInstallsLayout.this.addShortcut(AutoInstallsLayout.this.mSourceRes.getString(attributeResourceValue), intent, 1);
        }

        protected Intent parseIntent(XmlResourceParser xmlResourceParser) {
            String attributeValue = AutoInstallsLayout.getAttributeValue(xmlResourceParser, AutoInstallsLayout.ATTR_URL);
            if (TextUtils.isEmpty(attributeValue) || !Patterns.WEB_URL.matcher(attributeValue).matches()) {
                return null;
            }
            return new Intent("android.intent.action.VIEW", (Uri) null).setData(Uri.parse(attributeValue));
        }
    }

    protected class PendingWidgetParser implements TagParser {
        protected PendingWidgetParser() {
        }

        @Override
        public long parseAndAdd(XmlResourceParser xmlResourceParser) throws XmlPullParserException, IOException {
            String attributeValue = AutoInstallsLayout.getAttributeValue(xmlResourceParser, AutoInstallsLayout.ATTR_PACKAGE_NAME);
            String attributeValue2 = AutoInstallsLayout.getAttributeValue(xmlResourceParser, AutoInstallsLayout.ATTR_CLASS_NAME);
            if (TextUtils.isEmpty(attributeValue) || TextUtils.isEmpty(attributeValue2)) {
                return -1L;
            }
            AutoInstallsLayout.this.mValues.put("spanX", AutoInstallsLayout.getAttributeValue(xmlResourceParser, "spanX"));
            AutoInstallsLayout.this.mValues.put("spanY", AutoInstallsLayout.getAttributeValue(xmlResourceParser, "spanY"));
            AutoInstallsLayout.this.mValues.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, (Integer) 4);
            Bundle bundle = new Bundle();
            int depth = xmlResourceParser.getDepth();
            while (true) {
                int next = xmlResourceParser.next();
                if (next != 3 || xmlResourceParser.getDepth() > depth) {
                    if (next == 2) {
                        if (AutoInstallsLayout.TAG_EXTRA.equals(xmlResourceParser.getName())) {
                            String attributeValue3 = AutoInstallsLayout.getAttributeValue(xmlResourceParser, AutoInstallsLayout.ATTR_KEY);
                            String attributeValue4 = AutoInstallsLayout.getAttributeValue(xmlResourceParser, "value");
                            if (attributeValue3 == null || attributeValue4 == null) {
                                break;
                            }
                            bundle.putString(attributeValue3, attributeValue4);
                        } else {
                            throw new RuntimeException("Widgets can contain only extras");
                        }
                    }
                } else {
                    return verifyAndInsert(new ComponentName(attributeValue, attributeValue2), bundle);
                }
            }
            throw new RuntimeException("Widget extras must have a key and value");
        }

        protected long verifyAndInsert(ComponentName componentName, Bundle bundle) {
            AutoInstallsLayout.this.mValues.put(LauncherSettings.Favorites.APPWIDGET_PROVIDER, componentName.flattenToString());
            AutoInstallsLayout.this.mValues.put(LauncherSettings.Favorites.RESTORED, (Integer) 35);
            AutoInstallsLayout.this.mValues.put("_id", Long.valueOf(AutoInstallsLayout.this.mCallback.generateNewItemId()));
            if (!bundle.isEmpty()) {
                AutoInstallsLayout.this.mValues.put(LauncherSettings.BaseLauncherColumns.INTENT, new Intent().putExtras(bundle).toUri(0));
            }
            long jInsertAndCheck = AutoInstallsLayout.this.mCallback.insertAndCheck(AutoInstallsLayout.this.mDb, AutoInstallsLayout.this.mValues);
            if (jInsertAndCheck < 0) {
                return -1L;
            }
            return jInsertAndCheck;
        }
    }

    protected class FolderParser implements TagParser {
        private final ArrayMap<String, TagParser> mFolderElements;

        public FolderParser(AutoInstallsLayout autoInstallsLayout) {
            this(autoInstallsLayout.getFolderElementsMap());
        }

        public FolderParser(ArrayMap<String, TagParser> arrayMap) {
            this.mFolderElements = arrayMap;
        }

        @Override
        public long parseAndAdd(XmlResourceParser xmlResourceParser) throws XmlPullParserException, IOException {
            String string;
            int attributeResourceValue = AutoInstallsLayout.getAttributeResourceValue(xmlResourceParser, "title", 0);
            if (attributeResourceValue != 0) {
                string = AutoInstallsLayout.this.mSourceRes.getString(attributeResourceValue);
            } else {
                string = AutoInstallsLayout.this.mContext.getResources().getString(R.string.folder_name);
            }
            AutoInstallsLayout.this.mValues.put("title", string);
            AutoInstallsLayout.this.mValues.put(LauncherSettings.BaseLauncherColumns.ITEM_TYPE, (Integer) 2);
            AutoInstallsLayout.this.mValues.put("spanX", (Integer) 1);
            AutoInstallsLayout.this.mValues.put("spanY", (Integer) 1);
            AutoInstallsLayout.this.mValues.put("_id", Long.valueOf(AutoInstallsLayout.this.mCallback.generateNewItemId()));
            long jInsertAndCheck = AutoInstallsLayout.this.mCallback.insertAndCheck(AutoInstallsLayout.this.mDb, AutoInstallsLayout.this.mValues);
            if (jInsertAndCheck < 0) {
                return -1L;
            }
            ContentValues contentValues = new ContentValues(AutoInstallsLayout.this.mValues);
            ArrayList arrayList = new ArrayList();
            int depth = xmlResourceParser.getDepth();
            int i = 0;
            while (true) {
                int next = xmlResourceParser.next();
                if (next != 3 || xmlResourceParser.getDepth() > depth) {
                    if (next == 2) {
                        AutoInstallsLayout.this.mValues.clear();
                        AutoInstallsLayout.this.mValues.put("container", Long.valueOf(jInsertAndCheck));
                        AutoInstallsLayout.this.mValues.put("rank", Integer.valueOf(i));
                        TagParser tagParser = this.mFolderElements.get(xmlResourceParser.getName());
                        if (tagParser != null) {
                            long andAdd = tagParser.parseAndAdd(xmlResourceParser);
                            if (andAdd >= 0) {
                                arrayList.add(Long.valueOf(andAdd));
                                i++;
                            }
                        } else {
                            throw new RuntimeException("Invalid folder item " + xmlResourceParser.getName());
                        }
                    }
                } else {
                    if (arrayList.size() >= 2) {
                        return jInsertAndCheck;
                    }
                    LauncherProvider.SqlArguments sqlArguments = new LauncherProvider.SqlArguments(LauncherSettings.Favorites.getContentUri(jInsertAndCheck), null, null);
                    AutoInstallsLayout.this.mDb.delete(sqlArguments.table, sqlArguments.where, sqlArguments.args);
                    if (arrayList.size() != 1) {
                        return -1L;
                    }
                    ContentValues contentValues2 = new ContentValues();
                    AutoInstallsLayout.copyInteger(contentValues, contentValues2, "container");
                    AutoInstallsLayout.copyInteger(contentValues, contentValues2, "screen");
                    AutoInstallsLayout.copyInteger(contentValues, contentValues2, LauncherSettings.Favorites.CELLX);
                    AutoInstallsLayout.copyInteger(contentValues, contentValues2, LauncherSettings.Favorites.CELLY);
                    long jLongValue = ((Long) arrayList.get(0)).longValue();
                    AutoInstallsLayout.this.mDb.update(LauncherSettings.Favorites.TABLE_NAME, contentValues2, "_id=" + jLongValue, null);
                    return jLongValue;
                }
            }
        }
    }

    protected static void beginDocument(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        int next;
        do {
            next = xmlPullParser.next();
            if (next == 2) {
                break;
            }
        } while (next != 1);
        if (next != 2) {
            throw new XmlPullParserException("No start tag found");
        }
        if (!xmlPullParser.getName().equals(str)) {
            throw new XmlPullParserException("Unexpected start tag: found " + xmlPullParser.getName() + ", expected " + str);
        }
    }

    private static String convertToDistanceFromEnd(String str, int i) {
        int i2;
        if (!TextUtils.isEmpty(str) && (i2 = Integer.parseInt(str)) < 0) {
            return Integer.toString(i + i2);
        }
        return str;
    }

    protected static String getAttributeValue(XmlResourceParser xmlResourceParser, String str) {
        String attributeValue = xmlResourceParser.getAttributeValue("http://schemas.android.com/apk/res-auto/com.android.launcher3", str);
        if (attributeValue == null) {
            return xmlResourceParser.getAttributeValue(null, str);
        }
        return attributeValue;
    }

    protected static int getAttributeResourceValue(XmlResourceParser xmlResourceParser, String str, int i) {
        int attributeResourceValue = xmlResourceParser.getAttributeResourceValue("http://schemas.android.com/apk/res-auto/com.android.launcher3", str, i);
        if (attributeResourceValue == i) {
            return xmlResourceParser.getAttributeResourceValue(null, str, i);
        }
        return attributeResourceValue;
    }

    static void copyInteger(ContentValues contentValues, ContentValues contentValues2, String str) {
        contentValues2.put(str, contentValues.getAsInteger(str));
    }
}
