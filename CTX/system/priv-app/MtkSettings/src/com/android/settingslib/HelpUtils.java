package com.android.settingslib;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.android.internal.logging.MetricsLogger;
import java.net.URISyntaxException;
import java.util.Locale;
/* loaded from: classes.dex */
public class HelpUtils {
    private static final String TAG = HelpUtils.class.getSimpleName();
    private static String sCachedVersionCode = null;

    private HelpUtils() {
    }

    public static boolean prepareHelpMenuItem(Activity activity, Menu menu, String str, String str2) {
        MenuItem add = menu.add(0, 101, 0, R.string.help_feedback_label);
        add.setIcon(R.drawable.ic_help_actionbar);
        return prepareHelpMenuItem(activity, add, str, str2);
    }

    public static boolean prepareHelpMenuItem(Activity activity, Menu menu, int i, String str) {
        MenuItem add = menu.add(0, 101, 0, R.string.help_feedback_label);
        add.setIcon(R.drawable.ic_help_actionbar);
        return prepareHelpMenuItem(activity, add, activity.getString(i), str);
    }

    public static boolean prepareHelpMenuItem(final Activity activity, MenuItem menuItem, String str, String str2) {
        if (Settings.Global.getInt(activity.getContentResolver(), "device_provisioned", 0) == 0) {
            return false;
        }
        if (TextUtils.isEmpty(str)) {
            menuItem.setVisible(false);
            return false;
        }
        final Intent helpIntent = getHelpIntent(activity, str, str2);
        if (helpIntent != null) {
            menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() { // from class: com.android.settingslib.HelpUtils.1
                @Override // android.view.MenuItem.OnMenuItemClickListener
                public boolean onMenuItemClick(MenuItem menuItem2) {
                    MetricsLogger.action(activity, 513, helpIntent.getStringExtra("EXTRA_CONTEXT"));
                    try {
                        activity.startActivityForResult(helpIntent, 0);
                        return true;
                    } catch (ActivityNotFoundException e) {
                        String str3 = HelpUtils.TAG;
                        Log.e(str3, "No activity found for intent: " + helpIntent);
                        return true;
                    }
                }
            });
            menuItem.setShowAsAction(2);
            menuItem.setVisible(true);
            return true;
        }
        menuItem.setVisible(false);
        return false;
    }

    public static Intent getHelpIntent(Context context, String str, String str2) {
        if (Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 0) {
            return null;
        }
        try {
            Intent parseUri = Intent.parseUri(str, 3);
            addIntentParameters(context, parseUri, str2, true);
            if (parseUri.resolveActivity(context.getPackageManager()) != null) {
                return parseUri;
            }
            if (parseUri.hasExtra("EXTRA_BACKUP_URI")) {
                return getHelpIntent(context, parseUri.getStringExtra("EXTRA_BACKUP_URI"), str2);
            }
            return null;
        } catch (URISyntaxException e) {
            Intent intent = new Intent("android.intent.action.VIEW", uriWithAddedParameters(context, Uri.parse(str)));
            intent.setFlags(276824064);
            return intent;
        }
    }

    public static void addIntentParameters(Context context, Intent intent, String str, boolean z) {
        if (!intent.hasExtra("EXTRA_CONTEXT")) {
            intent.putExtra("EXTRA_CONTEXT", str);
        }
        Resources resources = context.getResources();
        boolean z2 = resources.getBoolean(17957013);
        if (z && z2) {
            String[] strArr = {resources.getString(17039686)};
            String[] strArr2 = {resources.getString(17039687)};
            String string = resources.getString(17039684);
            String string2 = resources.getString(17039685);
            String string3 = resources.getString(17039673);
            String string4 = resources.getString(17039674);
            intent.putExtra(string, strArr);
            intent.putExtra(string2, strArr2);
            intent.putExtra(string3, strArr);
            intent.putExtra(string4, strArr2);
        }
        intent.putExtra("EXTRA_THEME", 0);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(new int[]{16843827});
        intent.putExtra("EXTRA_PRIMARY_COLOR", obtainStyledAttributes.getColor(0, 0));
        obtainStyledAttributes.recycle();
    }

    private static Uri uriWithAddedParameters(Context context, Uri uri) {
        Uri.Builder buildUpon = uri.buildUpon();
        buildUpon.appendQueryParameter("hl", Locale.getDefault().toString());
        if (sCachedVersionCode == null) {
            try {
                sCachedVersionCode = Long.toString(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).getLongVersionCode());
                buildUpon.appendQueryParameter("version", sCachedVersionCode);
            } catch (PackageManager.NameNotFoundException e) {
                Log.wtf(TAG, "Invalid package name for context", e);
            }
        } else {
            buildUpon.appendQueryParameter("version", sCachedVersionCode);
        }
        return buildUpon.build();
    }
}
