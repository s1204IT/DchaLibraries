package com.android.packageinstaller;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;

public class PackageUtil {
    private static final String LOG_TAG = PackageUtil.class.getSimpleName();

    public static PackageParser.Package getPackageInfo(Context context, File file) {
        PackageParser packageParser = new PackageParser();
        packageParser.setCallback(new PackageParser.CallbackImpl(context.getPackageManager()));
        try {
            return packageParser.parsePackage(file, 0);
        } catch (PackageParser.PackageParserException e) {
            return null;
        }
    }

    public static View initSnippet(View view, CharSequence charSequence, Drawable drawable) {
        ((ImageView) view.findViewById(R.id.app_icon)).setImageDrawable(drawable);
        ((TextView) view.findViewById(R.id.app_name)).setText(charSequence);
        return view;
    }

    public static View initSnippetForInstalledApp(Context context, ApplicationInfo applicationInfo, View view) {
        return initSnippetForInstalledApp(context, applicationInfo, view, null);
    }

    public static View initSnippetForInstalledApp(Context context, ApplicationInfo applicationInfo, View view, UserHandle userHandle) {
        PackageManager packageManager = context.getPackageManager();
        Drawable drawableLoadIcon = applicationInfo.loadIcon(packageManager);
        if (userHandle != null) {
            drawableLoadIcon = context.getPackageManager().getUserBadgedIcon(drawableLoadIcon, userHandle);
        }
        return initSnippet(view, applicationInfo.loadLabel(packageManager), drawableLoadIcon);
    }

    public static View initSnippetForNewApp(Activity activity, AppSnippet appSnippet, int i) {
        View viewFindViewById = activity.findViewById(i);
        if (appSnippet.icon != null) {
            ((ImageView) viewFindViewById.findViewById(R.id.app_icon)).setImageDrawable(appSnippet.icon);
        }
        ((TextView) viewFindViewById.findViewById(R.id.app_name)).setText(appSnippet.label);
        return viewFindViewById;
    }

    public static class AppSnippet {
        public Drawable icon;
        public CharSequence label;

        public AppSnippet(CharSequence charSequence, Drawable drawable) {
            this.label = charSequence;
            this.icon = drawable;
        }
    }

    public static AppSnippet getAppSnippet(Activity activity, ApplicationInfo applicationInfo, File file) {
        CharSequence text;
        Drawable defaultActivityIcon;
        String absolutePath = file.getAbsolutePath();
        Resources resources = activity.getResources();
        AssetManager assetManager = new AssetManager();
        assetManager.addAssetPath(absolutePath);
        Resources resources2 = new Resources(assetManager, resources.getDisplayMetrics(), resources.getConfiguration());
        Drawable drawable = null;
        if (applicationInfo.labelRes != 0) {
            try {
                text = resources2.getText(applicationInfo.labelRes);
            } catch (Resources.NotFoundException e) {
                text = null;
            }
        } else {
            text = null;
        }
        if (text == null) {
            text = applicationInfo.nonLocalizedLabel != null ? applicationInfo.nonLocalizedLabel : applicationInfo.packageName;
        }
        try {
            if (applicationInfo.icon != 0) {
                try {
                    drawable = resources2.getDrawable(applicationInfo.icon);
                } catch (Resources.NotFoundException e2) {
                }
            }
            if (drawable == null) {
                defaultActivityIcon = activity.getPackageManager().getDefaultActivityIcon();
            } else {
                defaultActivityIcon = drawable;
            }
        } catch (OutOfMemoryError e3) {
            Log.i(LOG_TAG, "Could not load app icon", e3);
            defaultActivityIcon = drawable;
        }
        return new AppSnippet(text, defaultActivityIcon);
    }

    static int getMaxTargetSdkVersionForUid(Context context, int i) {
        PackageManager packageManager = context.getPackageManager();
        String[] packagesForUid = packageManager.getPackagesForUid(i);
        if (packagesForUid == null) {
            return -1;
        }
        int iMax = -1;
        for (String str : packagesForUid) {
            try {
                iMax = Math.max(iMax, packageManager.getApplicationInfo(str, 0).targetSdkVersion);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return iMax;
    }
}
