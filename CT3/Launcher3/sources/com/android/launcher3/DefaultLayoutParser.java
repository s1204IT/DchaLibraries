package com.android.launcher3;

import android.appwidget.AppWidgetHost;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.AutoInstallsLayout;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class DefaultLayoutParser extends AutoInstallsLayout {
    public DefaultLayoutParser(Context context, AppWidgetHost appWidgetHost, AutoInstallsLayout.LayoutParserCallback callback, Resources sourceRes, int layoutId) {
        super(context, appWidgetHost, callback, sourceRes, layoutId, "favorites");
    }

    public DefaultLayoutParser(Context context, AppWidgetHost appWidgetHost, AutoInstallsLayout.LayoutParserCallback callback, Resources sourceRes, int layoutId, String rootTag) {
        super(context, appWidgetHost, callback, sourceRes, layoutId, rootTag);
    }

    @Override
    protected HashMap<String, AutoInstallsLayout.TagParser> getFolderElementsMap() {
        return getFolderElementsMap(this.mSourceRes);
    }

    HashMap<String, AutoInstallsLayout.TagParser> getFolderElementsMap(Resources res) {
        HashMap<String, AutoInstallsLayout.TagParser> parsers = new HashMap<>();
        parsers.put("favorite", new AppShortcutWithUriParser());
        parsers.put("shortcut", new UriShortcutParser(res));
        return parsers;
    }

    @Override
    protected HashMap<String, AutoInstallsLayout.TagParser> getLayoutElementsMap() {
        HashMap<String, AutoInstallsLayout.TagParser> parsers = new HashMap<>();
        parsers.put("favorite", new AppShortcutWithUriParser());
        parsers.put("appwidget", new AutoInstallsLayout.AppWidgetParser());
        parsers.put("shortcut", new UriShortcutParser(this.mSourceRes));
        parsers.put("resolve", new ResolveParser());
        parsers.put("folder", new MyFolderParser());
        parsers.put("partner-folder", new PartnerFolderParser());
        return parsers;
    }

    @Override
    protected void parseContainerAndScreen(XmlResourceParser parser, long[] out) {
        out[0] = -100;
        String strContainer = getAttributeValue(parser, "container");
        if (strContainer != null) {
            out[0] = Long.valueOf(strContainer).longValue();
        }
        out[1] = Long.parseLong(getAttributeValue(parser, "screen"));
    }

    class AppShortcutWithUriParser extends AutoInstallsLayout.AppShortcutParser {
        AppShortcutWithUriParser() {
            super();
        }

        @Override
        protected long invalidPackageOrClass(XmlResourceParser parser) {
            String uri = DefaultLayoutParser.getAttributeValue(parser, "uri");
            if (TextUtils.isEmpty(uri)) {
                Log.e("DefaultLayoutParser", "Skipping invalid <favorite> with no component or uri");
                return -1L;
            }
            try {
                Intent metaIntent = Intent.parseUri(uri, 0);
                ResolveInfo resolved = DefaultLayoutParser.this.mPackageManager.resolveActivity(metaIntent, 65536);
                List<ResolveInfo> appList = DefaultLayoutParser.this.mPackageManager.queryIntentActivities(metaIntent, 65536);
                if (wouldLaunchResolverActivity(resolved, appList)) {
                    ResolveInfo systemApp = getSingleSystemActivity(appList);
                    if (systemApp == null) {
                        Log.w("DefaultLayoutParser", "No preference or single system activity found for " + metaIntent.toString());
                        return -1L;
                    }
                    resolved = systemApp;
                }
                ActivityInfo info = resolved.activityInfo;
                Intent intent = DefaultLayoutParser.this.mPackageManager.getLaunchIntentForPackage(info.packageName);
                if (intent == null) {
                    return -1L;
                }
                if ("com.android.gallery3d".equals(info.packageName)) {
                    intent.setPackage(null);
                }
                intent.setFlags(270532608);
                return DefaultLayoutParser.this.addShortcut(info.loadLabel(DefaultLayoutParser.this.mPackageManager).toString(), intent, 0);
            } catch (URISyntaxException e) {
                Log.e("DefaultLayoutParser", "Unable to add meta-favorite: " + uri, e);
                return -1L;
            }
        }

        private ResolveInfo getSingleSystemActivity(List<ResolveInfo> appList) {
            ResolveInfo systemResolve = null;
            int N = appList.size();
            for (int i = 0; i < N; i++) {
                try {
                    ApplicationInfo info = DefaultLayoutParser.this.mPackageManager.getApplicationInfo(appList.get(i).activityInfo.packageName, 0);
                    if ((info.flags & 1) != 0) {
                        if (systemResolve != null) {
                            return null;
                        }
                        systemResolve = appList.get(i);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w("DefaultLayoutParser", "Unable to get info about resolve results", e);
                    return null;
                }
            }
            return systemResolve;
        }

        private boolean wouldLaunchResolverActivity(ResolveInfo resolved, List<ResolveInfo> appList) {
            for (int i = 0; i < appList.size(); i++) {
                ResolveInfo tmp = appList.get(i);
                if (tmp.activityInfo.name.equals(resolved.activityInfo.name) && tmp.activityInfo.packageName.equals(resolved.activityInfo.packageName)) {
                    return false;
                }
            }
            return true;
        }
    }

    private class UriShortcutParser extends AutoInstallsLayout.ShortcutParser {
        public UriShortcutParser(Resources iconRes) {
            super(iconRes);
        }

        @Override
        protected Intent parseIntent(XmlResourceParser parser) {
            String uri = null;
            try {
                uri = DefaultLayoutParser.getAttributeValue(parser, "uri");
                return Intent.parseUri(uri, 0);
            } catch (URISyntaxException e) {
                Log.w("DefaultLayoutParser", "Shortcut has malformed uri: " + uri);
                return null;
            }
        }
    }

    protected class ResolveParser implements AutoInstallsLayout.TagParser {
        private final AppShortcutWithUriParser mChildParser;

        protected ResolveParser() {
            this.mChildParser = DefaultLayoutParser.this.new AppShortcutWithUriParser();
        }

        @Override
        public long parseAndAdd(XmlResourceParser parser) throws XmlPullParserException, IOException {
            int groupDepth = parser.getDepth();
            long addedId = -1;
            while (true) {
                int type = parser.next();
                if (type != 3 || parser.getDepth() > groupDepth) {
                    if (type == 2 && addedId <= -1) {
                        String fallback_item_name = parser.getName();
                        if ("favorite".equals(fallback_item_name)) {
                            addedId = this.mChildParser.parseAndAdd(parser);
                        } else {
                            Log.e("DefaultLayoutParser", "Fallback groups can contain only favorites, found " + fallback_item_name);
                        }
                    }
                } else {
                    return addedId;
                }
            }
        }
    }

    class PartnerFolderParser implements AutoInstallsLayout.TagParser {
        PartnerFolderParser() {
        }

        @Override
        public long parseAndAdd(XmlResourceParser parser) throws XmlPullParserException, IOException {
            Resources partnerRes;
            int resId;
            Partner partner = Partner.get(DefaultLayoutParser.this.mPackageManager);
            if (partner != null && (resId = (partnerRes = partner.getResources()).getIdentifier("partner_folder", "xml", partner.getPackageName())) != 0) {
                XmlResourceParser partnerParser = partnerRes.getXml(resId);
                DefaultLayoutParser.beginDocument(partnerParser, "folder");
                AutoInstallsLayout.FolderParser folderParser = new AutoInstallsLayout.FolderParser(DefaultLayoutParser.this.getFolderElementsMap(partnerRes));
                return folderParser.parseAndAdd(partnerParser);
            }
            return -1L;
        }
    }

    class MyFolderParser extends AutoInstallsLayout.FolderParser {
        MyFolderParser() {
            super(DefaultLayoutParser.this);
        }

        @Override
        public long parseAndAdd(XmlResourceParser parser) throws XmlPullParserException, IOException {
            int resId = DefaultLayoutParser.getAttributeResourceValue(parser, "folderItems", 0);
            if (resId != 0) {
                parser = DefaultLayoutParser.this.mSourceRes.getXml(resId);
                DefaultLayoutParser.beginDocument(parser, "folder");
            }
            return super.parseAndAdd(parser);
        }
    }
}
