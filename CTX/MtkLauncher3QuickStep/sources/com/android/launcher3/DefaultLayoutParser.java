package com.android.launcher3;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.android.launcher3.AutoInstallsLayout;
import com.android.launcher3.LauncherSettings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class DefaultLayoutParser extends AutoInstallsLayout {
    private static final String ACTION_APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE = "com.android.launcher.action.APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE";
    private static final String ATTR_CONTAINER = "container";
    private static final String ATTR_FOLDER_ITEMS = "folderItems";
    private static final String ATTR_SCREEN = "screen";
    protected static final String ATTR_URI = "uri";
    private static final String TAG = "DefaultLayoutParser";
    private static final String TAG_APPWIDGET = "appwidget";
    protected static final String TAG_FAVORITE = "favorite";
    private static final String TAG_FAVORITES = "favorites";
    private static final String TAG_FOLDER = "folder";
    private static final String TAG_PARTNER_FOLDER = "partner-folder";
    protected static final String TAG_RESOLVE = "resolve";
    protected static final String TAG_SHORTCUT = "shortcut";

    public DefaultLayoutParser(Context context, AppWidgetHost appWidgetHost, AutoInstallsLayout.LayoutParserCallback layoutParserCallback, Resources resources, int i) {
        super(context, appWidgetHost, layoutParserCallback, resources, i, "favorites");
    }

    @Override
    protected ArrayMap<String, AutoInstallsLayout.TagParser> getFolderElementsMap() {
        return getFolderElementsMap(this.mSourceRes);
    }

    ArrayMap<String, AutoInstallsLayout.TagParser> getFolderElementsMap(Resources resources) {
        ArrayMap<String, AutoInstallsLayout.TagParser> arrayMap = new ArrayMap<>();
        arrayMap.put(TAG_FAVORITE, new AppShortcutWithUriParser());
        arrayMap.put(TAG_SHORTCUT, new UriShortcutParser(resources));
        return arrayMap;
    }

    @Override
    protected ArrayMap<String, AutoInstallsLayout.TagParser> getLayoutElementsMap() {
        ArrayMap<String, AutoInstallsLayout.TagParser> arrayMap = new ArrayMap<>();
        arrayMap.put(TAG_FAVORITE, new AppShortcutWithUriParser());
        arrayMap.put(TAG_APPWIDGET, new AppWidgetParser());
        arrayMap.put(TAG_SHORTCUT, new UriShortcutParser(this.mSourceRes));
        arrayMap.put(TAG_RESOLVE, new ResolveParser());
        arrayMap.put(TAG_FOLDER, new MyFolderParser());
        arrayMap.put(TAG_PARTNER_FOLDER, new PartnerFolderParser());
        return arrayMap;
    }

    @Override
    protected void parseContainerAndScreen(XmlResourceParser xmlResourceParser, long[] jArr) {
        jArr[0] = -100;
        String attributeValue = getAttributeValue(xmlResourceParser, "container");
        if (attributeValue != null) {
            jArr[0] = Long.valueOf(attributeValue).longValue();
        }
        jArr[1] = Long.parseLong(getAttributeValue(xmlResourceParser, "screen"));
    }

    public class AppShortcutWithUriParser extends AutoInstallsLayout.AppShortcutParser {
        public AppShortcutWithUriParser() {
            super();
        }

        @Override
        public long parseAndAdd(XmlResourceParser xmlResourceParser) {
            return super.parseAndAdd(xmlResourceParser);
        }

        @Override
        protected long invalidPackageOrClass(XmlResourceParser xmlResourceParser) {
            String attributeValue = AutoInstallsLayout.getAttributeValue(xmlResourceParser, DefaultLayoutParser.ATTR_URI);
            if (TextUtils.isEmpty(attributeValue)) {
                Log.e(DefaultLayoutParser.TAG, "Skipping invalid <favorite> with no component or uri");
                return -1L;
            }
            try {
                Intent uri = Intent.parseUri(attributeValue, 0);
                ResolveInfo resolveInfoResolveActivity = DefaultLayoutParser.this.mPackageManager.resolveActivity(uri, 65536);
                List<ResolveInfo> listQueryIntentActivities = DefaultLayoutParser.this.mPackageManager.queryIntentActivities(uri, 65536);
                if (wouldLaunchResolverActivity(resolveInfoResolveActivity, listQueryIntentActivities) && (resolveInfoResolveActivity = getSingleSystemActivity(listQueryIntentActivities)) == null) {
                    Log.w(DefaultLayoutParser.TAG, "No preference or single system activity found for " + uri.toString());
                    return -1L;
                }
                ActivityInfo activityInfo = resolveInfoResolveActivity.activityInfo;
                Intent launchIntentForPackage = DefaultLayoutParser.this.mPackageManager.getLaunchIntentForPackage(activityInfo.packageName);
                if (launchIntentForPackage == null) {
                    return -1L;
                }
                if ("com.android.gallery3d".equals(activityInfo.packageName) || "com.android.email".equals(activityInfo.packageName) || "com.android.mms".equals(activityInfo.packageName)) {
                    launchIntentForPackage.setPackage(null);
                }
                launchIntentForPackage.setFlags(270532608);
                return DefaultLayoutParser.this.addShortcut(activityInfo.loadLabel(DefaultLayoutParser.this.mPackageManager).toString(), launchIntentForPackage, 0);
            } catch (URISyntaxException e) {
                Log.e(DefaultLayoutParser.TAG, "Unable to add meta-favorite: " + attributeValue, e);
                return -1L;
            }
        }

        private ResolveInfo getSingleSystemActivity(List<ResolveInfo> list) {
            int size = list.size();
            ResolveInfo resolveInfo = null;
            for (int i = 0; i < size; i++) {
                try {
                    if ((DefaultLayoutParser.this.mPackageManager.getApplicationInfo(list.get(i).activityInfo.packageName, 0).flags & 1) != 0) {
                        if (resolveInfo != null) {
                            return null;
                        }
                        resolveInfo = list.get(i);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(DefaultLayoutParser.TAG, "Unable to get info about resolve results", e);
                    return null;
                }
            }
            return resolveInfo;
        }

        private boolean wouldLaunchResolverActivity(ResolveInfo resolveInfo, List<ResolveInfo> list) {
            for (int i = 0; i < list.size(); i++) {
                ResolveInfo resolveInfo2 = list.get(i);
                if (resolveInfo2.activityInfo.name.equals(resolveInfo.activityInfo.name) && resolveInfo2.activityInfo.packageName.equals(resolveInfo.activityInfo.packageName)) {
                    return false;
                }
            }
            return true;
        }
    }

    public class UriShortcutParser extends AutoInstallsLayout.ShortcutParser {
        @Override
        public long parseAndAdd(XmlResourceParser xmlResourceParser) {
            return super.parseAndAdd(xmlResourceParser);
        }

        public UriShortcutParser(Resources resources) {
            super(resources);
        }

        @Override
        protected Intent parseIntent(XmlResourceParser xmlResourceParser) {
            String attributeValue;
            try {
                attributeValue = AutoInstallsLayout.getAttributeValue(xmlResourceParser, DefaultLayoutParser.ATTR_URI);
            } catch (URISyntaxException e) {
                attributeValue = null;
            }
            try {
                return Intent.parseUri(attributeValue, 0);
            } catch (URISyntaxException e2) {
                Log.w(DefaultLayoutParser.TAG, "Shortcut has malformed uri: " + attributeValue);
                return null;
            }
        }
    }

    public class ResolveParser implements AutoInstallsLayout.TagParser {
        private final AppShortcutWithUriParser mChildParser;

        public ResolveParser() {
            this.mChildParser = DefaultLayoutParser.this.new AppShortcutWithUriParser();
        }

        @Override
        public long parseAndAdd(XmlResourceParser xmlResourceParser) throws XmlPullParserException, IOException {
            int depth = xmlResourceParser.getDepth();
            long andAdd = -1;
            while (true) {
                int next = xmlResourceParser.next();
                if (next != 3 || xmlResourceParser.getDepth() > depth) {
                    if (next == 2 && andAdd <= -1) {
                        String name = xmlResourceParser.getName();
                        if (DefaultLayoutParser.TAG_FAVORITE.equals(name)) {
                            andAdd = this.mChildParser.parseAndAdd(xmlResourceParser);
                        } else {
                            Log.e(DefaultLayoutParser.TAG, "Fallback groups can contain only favorites, found " + name);
                        }
                    }
                } else {
                    return andAdd;
                }
            }
        }
    }

    class PartnerFolderParser implements AutoInstallsLayout.TagParser {
        PartnerFolderParser() {
        }

        @Override
        public long parseAndAdd(XmlResourceParser xmlResourceParser) throws XmlPullParserException, IOException {
            Resources resources;
            int identifier;
            Partner partner = Partner.get(DefaultLayoutParser.this.mPackageManager);
            if (partner != null && (identifier = (resources = partner.getResources()).getIdentifier(Partner.RES_FOLDER, "xml", partner.getPackageName())) != 0) {
                XmlResourceParser xml = resources.getXml(identifier);
                AutoInstallsLayout.beginDocument(xml, DefaultLayoutParser.TAG_FOLDER);
                return new AutoInstallsLayout.FolderParser(DefaultLayoutParser.this.getFolderElementsMap(resources)).parseAndAdd(xml);
            }
            return -1L;
        }
    }

    class MyFolderParser extends AutoInstallsLayout.FolderParser {
        MyFolderParser() {
            super(DefaultLayoutParser.this);
        }

        @Override
        public long parseAndAdd(XmlResourceParser xmlResourceParser) throws XmlPullParserException, IOException {
            int attributeResourceValue = AutoInstallsLayout.getAttributeResourceValue(xmlResourceParser, DefaultLayoutParser.ATTR_FOLDER_ITEMS, 0);
            if (attributeResourceValue != 0) {
                xmlResourceParser = DefaultLayoutParser.this.mSourceRes.getXml(attributeResourceValue);
                AutoInstallsLayout.beginDocument(xmlResourceParser, DefaultLayoutParser.TAG_FOLDER);
            }
            return super.parseAndAdd(xmlResourceParser);
        }
    }

    protected class AppWidgetParser extends AutoInstallsLayout.PendingWidgetParser {
        protected AppWidgetParser() {
            super();
        }

        @Override
        protected long verifyAndInsert(ComponentName componentName, Bundle bundle) {
            long jInsertAndCheck;
            int iAllocateAppWidgetId;
            try {
                DefaultLayoutParser.this.mPackageManager.getReceiverInfo(componentName, 0);
            } catch (Exception e) {
                ComponentName componentName2 = new ComponentName(DefaultLayoutParser.this.mPackageManager.currentToCanonicalPackageNames(new String[]{componentName.getPackageName()})[0], componentName.getClassName());
                try {
                    DefaultLayoutParser.this.mPackageManager.getReceiverInfo(componentName2, 0);
                    componentName = componentName2;
                } catch (Exception e2) {
                    Log.d(DefaultLayoutParser.TAG, "Can't find widget provider: " + componentName2.getClassName());
                    return -1L;
                }
            }
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(DefaultLayoutParser.this.mContext);
            try {
                iAllocateAppWidgetId = DefaultLayoutParser.this.mAppWidgetHost.allocateAppWidgetId();
            } catch (RuntimeException e3) {
                e = e3;
                jInsertAndCheck = -1;
            }
            if (!appWidgetManager.bindAppWidgetIdIfAllowed(iAllocateAppWidgetId, componentName)) {
                Log.e(DefaultLayoutParser.TAG, "Unable to bind app widget id " + componentName);
                DefaultLayoutParser.this.mAppWidgetHost.deleteAppWidgetId(iAllocateAppWidgetId);
                return -1L;
            }
            DefaultLayoutParser.this.mValues.put(LauncherSettings.Favorites.APPWIDGET_ID, Integer.valueOf(iAllocateAppWidgetId));
            DefaultLayoutParser.this.mValues.put(LauncherSettings.Favorites.APPWIDGET_PROVIDER, componentName.flattenToString());
            DefaultLayoutParser.this.mValues.put("_id", Long.valueOf(DefaultLayoutParser.this.mCallback.generateNewItemId()));
            jInsertAndCheck = DefaultLayoutParser.this.mCallback.insertAndCheck(DefaultLayoutParser.this.mDb, DefaultLayoutParser.this.mValues);
            try {
            } catch (RuntimeException e4) {
                e = e4;
                Log.e(DefaultLayoutParser.TAG, "Problem allocating appWidgetId", e);
            }
            if (jInsertAndCheck < 0) {
                DefaultLayoutParser.this.mAppWidgetHost.deleteAppWidgetId(iAllocateAppWidgetId);
                return jInsertAndCheck;
            }
            if (!bundle.isEmpty()) {
                Intent intent = new Intent(DefaultLayoutParser.ACTION_APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE);
                intent.setComponent(componentName);
                intent.putExtras(bundle);
                intent.putExtra(LauncherSettings.Favorites.APPWIDGET_ID, iAllocateAppWidgetId);
                DefaultLayoutParser.this.mContext.sendBroadcast(intent);
            }
            return jInsertAndCheck;
        }
    }
}
