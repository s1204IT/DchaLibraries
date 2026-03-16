package com.android.packageinstaller;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;

public class PackageUtil {
    public static PackageParser.Package getPackageInfo(File sourceFile) {
        PackageParser parser = new PackageParser();
        try {
            PackageParser.Package pkg = parser.parseMonolithicPackage(sourceFile, 0);
            parser.collectManifestDigest(pkg);
            return pkg;
        } catch (PackageParser.PackageParserException e) {
            return null;
        }
    }

    public static View initSnippet(View snippetView, CharSequence label, Drawable icon) {
        ((ImageView) snippetView.findViewById(R.id.app_icon)).setImageDrawable(icon);
        ((TextView) snippetView.findViewById(R.id.app_name)).setText(label);
        return snippetView;
    }

    public static View initSnippetForInstalledApp(Activity pContext, ApplicationInfo appInfo, View snippetView) {
        return initSnippetForInstalledApp(pContext, appInfo, snippetView, null);
    }

    public static View initSnippetForInstalledApp(Activity pContext, ApplicationInfo appInfo, View snippetView, UserHandle user) {
        PackageManager pm = pContext.getPackageManager();
        Drawable icon = appInfo.loadIcon(pm);
        if (user != null) {
            icon = pContext.getPackageManager().getUserBadgedIcon(icon, user);
        }
        return initSnippet(snippetView, appInfo.loadLabel(pm), icon);
    }

    public static View initSnippetForNewApp(Activity pContext, AppSnippet as, int snippetId) {
        View appSnippet = pContext.findViewById(snippetId);
        ((ImageView) appSnippet.findViewById(R.id.app_icon)).setImageDrawable(as.icon);
        ((TextView) appSnippet.findViewById(R.id.app_name)).setText(as.label);
        return appSnippet;
    }

    public static class AppSnippet {
        Drawable icon;
        CharSequence label;

        public AppSnippet(CharSequence label, Drawable icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    public static AppSnippet getAppSnippet(Activity pContext, ApplicationInfo appInfo, File sourceFile) {
        String archiveFilePath = sourceFile.getAbsolutePath();
        Resources pRes = pContext.getResources();
        AssetManager assmgr = new AssetManager();
        assmgr.addAssetPath(archiveFilePath);
        Resources res = new Resources(assmgr, pRes.getDisplayMetrics(), pRes.getConfiguration());
        CharSequence label = null;
        if (appInfo.labelRes != 0) {
            try {
                label = res.getText(appInfo.labelRes);
            } catch (Resources.NotFoundException e) {
            }
        }
        if (label == null) {
            label = appInfo.nonLocalizedLabel != null ? appInfo.nonLocalizedLabel : appInfo.packageName;
        }
        Drawable icon = null;
        if (appInfo.icon != 0) {
            try {
                icon = res.getDrawable(appInfo.icon);
            } catch (Resources.NotFoundException e2) {
            }
        }
        if (icon == null) {
            icon = pContext.getPackageManager().getDefaultActivityIcon();
        }
        return new AppSnippet(label, icon);
    }
}
