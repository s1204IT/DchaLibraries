package com.android.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import java.util.Locale;

public class HelpUtils {
    private static final String TAG = HelpUtils.class.getName();
    private static String sCachedVersionCode = null;

    private HelpUtils() {
    }

    public static boolean prepareHelpMenuItem(Context context, MenuItem helpMenuItem, String helpUrlString) {
        if (TextUtils.isEmpty(helpUrlString)) {
            helpMenuItem.setVisible(false);
            return false;
        }
        Uri fullUri = uriWithAddedParameters(context, Uri.parse(helpUrlString));
        Intent intent = new Intent("android.intent.action.VIEW", fullUri);
        intent.setFlags(276824064);
        ComponentName component = intent.resolveActivity(context.getPackageManager());
        if (component != null) {
            helpMenuItem.setIntent(intent);
            helpMenuItem.setShowAsAction(0);
            helpMenuItem.setVisible(true);
            return true;
        }
        helpMenuItem.setVisible(false);
        return false;
    }

    public static Uri uriWithAddedParameters(Context context, Uri baseUri) {
        Uri.Builder builder = baseUri.buildUpon();
        builder.appendQueryParameter("hl", Locale.getDefault().toString());
        if (sCachedVersionCode == null) {
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                sCachedVersionCode = Integer.toString(info.versionCode);
                builder.appendQueryParameter("version", sCachedVersionCode);
            } catch (PackageManager.NameNotFoundException e) {
                Log.wtf(TAG, "Invalid package name for context", e);
            }
        } else {
            builder.appendQueryParameter("version", sCachedVersionCode);
        }
        return builder.build();
    }
}
