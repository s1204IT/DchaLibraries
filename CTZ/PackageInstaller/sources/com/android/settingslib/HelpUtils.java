package com.android.settingslib;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.BenesseExtension;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.android.internal.logging.MetricsLogger;
import java.net.URISyntaxException;
import java.util.Locale;

public class HelpUtils {
    private static final String TAG = HelpUtils.class.getSimpleName();
    private static String sCachedVersionCode = null;

    private HelpUtils() {
    }

    public static boolean prepareHelpMenuItem(Activity activity, Menu menu, int i, String str) {
        MenuItem menuItemAdd = menu.add(0, 101, 0, R.string.help_feedback_label);
        menuItemAdd.setIcon(R.drawable.ic_help_actionbar);
        return prepareHelpMenuItem(activity, menuItemAdd, activity.getString(i), str);
    }

    public static boolean prepareHelpMenuItem(final Activity activity, MenuItem menuItem, String str, String str2) {
        String str3;
        if (Settings.Global.getInt(activity.getContentResolver(), "device_provisioned", 0) == 0) {
            return false;
        }
        if (BenesseExtension.getDchaState() != 0) {
            str3 = "";
        } else {
            str3 = str;
        }
        if (TextUtils.isEmpty(str3)) {
            menuItem.setVisible(false);
            return false;
        }
        final Intent helpIntent = getHelpIntent(activity, str, str2);
        if (helpIntent != null) {
            menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem2) {
                    MetricsLogger.action(activity, 513, helpIntent.getStringExtra("EXTRA_CONTEXT"));
                    try {
                        activity.startActivityForResult(helpIntent, 0);
                        return true;
                    } catch (ActivityNotFoundException e) {
                        Log.e(HelpUtils.TAG, "No activity found for intent: " + helpIntent);
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
            Intent uri = Intent.parseUri(str, 3);
            addIntentParameters(context, uri, str2, true);
            if (uri.resolveActivity(context.getPackageManager()) != null) {
                return uri;
            }
            if (uri.hasExtra("EXTRA_BACKUP_URI")) {
                return getHelpIntent(context, uri.getStringExtra("EXTRA_BACKUP_URI"), str2);
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
        boolean z2 = resources.getBoolean(android.R.^attr-private.maxItems);
        if (z && z2) {
            String[] strArr = {resources.getString(android.R.string.alert_windows_notification_message)};
            String[] strArr2 = {resources.getString(android.R.string.alert_windows_notification_title)};
            String string = resources.getString(android.R.string.alert_windows_notification_channel_group_name);
            String string2 = resources.getString(android.R.string.alert_windows_notification_channel_name);
            String string3 = resources.getString(android.R.string.add_account_label);
            String string4 = resources.getString(android.R.string.aerr_application);
            intent.putExtra(string, strArr);
            intent.putExtra(string2, strArr2);
            intent.putExtra(string3, strArr);
            intent.putExtra(string4, strArr2);
        }
        intent.putExtra("EXTRA_THEME", 0);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{android.R.attr.colorPrimary});
        intent.putExtra("EXTRA_PRIMARY_COLOR", typedArrayObtainStyledAttributes.getColor(0, 0));
        typedArrayObtainStyledAttributes.recycle();
    }

    private static Uri uriWithAddedParameters(Context context, Uri uri) {
        Uri.Builder builderBuildUpon = uri.buildUpon();
        builderBuildUpon.appendQueryParameter("hl", Locale.getDefault().toString());
        if (sCachedVersionCode == null) {
            try {
                sCachedVersionCode = Long.toString(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).getLongVersionCode());
                builderBuildUpon.appendQueryParameter("version", sCachedVersionCode);
            } catch (PackageManager.NameNotFoundException e) {
                Log.wtf(TAG, "Invalid package name for context", e);
            }
        } else {
            builderBuildUpon.appendQueryParameter("version", sCachedVersionCode);
        }
        return builderBuildUpon.build();
    }
}
