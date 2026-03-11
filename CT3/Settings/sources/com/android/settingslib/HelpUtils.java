package com.android.settingslib;

import android.R;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import java.net.URISyntaxException;
import java.util.Locale;

public class HelpUtils {
    private static final String TAG = HelpUtils.class.getSimpleName();
    private static String sCachedVersionCode = null;

    private HelpUtils() {
    }

    public static boolean prepareHelpMenuItem(Activity activity, Menu menu, String helpUri, String backupContext) {
        MenuItem helpItem = menu.add(0, 101, 0, R$string.help_feedback_label);
        return prepareHelpMenuItem(activity, helpItem, helpUri, backupContext);
    }

    public static boolean prepareHelpMenuItem(Activity activity, Menu menu, int helpUriResource, String backupContext) {
        MenuItem helpItem = menu.add(0, 101, 0, R$string.help_feedback_label);
        return prepareHelpMenuItem(activity, helpItem, activity.getString(helpUriResource), backupContext);
    }

    public static boolean prepareHelpMenuItem(final Activity activity, MenuItem helpMenuItem, String helpUriString, String backupContext) {
        if (Settings.Global.getInt(activity.getContentResolver(), "device_provisioned", 0) == 0) {
            return false;
        }
        if (TextUtils.isEmpty(helpUriString)) {
            helpMenuItem.setVisible(false);
            return false;
        }
        final Intent intent = getHelpIntent(activity, helpUriString, backupContext);
        if (intent != null) {
            helpMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    try {
                        activity.startActivityForResult(intent, 0);
                        return true;
                    } catch (ActivityNotFoundException e) {
                        Log.e(HelpUtils.TAG, "No activity found for intent: " + intent);
                        return true;
                    }
                }
            });
            helpMenuItem.setShowAsAction(0);
            helpMenuItem.setVisible(true);
            return true;
        }
        helpMenuItem.setVisible(false);
        return false;
    }

    public static Intent getHelpIntent(Context context, String helpUriString, String backupContext) {
        if (Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 0) {
            return null;
        }
        try {
            Intent intent = Intent.parseUri(helpUriString, 3);
            addIntentParameters(context, intent, backupContext);
            ComponentName component = intent.resolveActivity(context.getPackageManager());
            if (component != null) {
                return intent;
            }
            if (intent.hasExtra("EXTRA_BACKUP_URI")) {
                return getHelpIntent(context, intent.getStringExtra("EXTRA_BACKUP_URI"), backupContext);
            }
            return null;
        } catch (URISyntaxException e) {
            Uri fullUri = uriWithAddedParameters(context, Uri.parse(helpUriString));
            Intent intent2 = new Intent("android.intent.action.VIEW", fullUri);
            intent2.setFlags(276824064);
            return intent2;
        }
    }

    private static void addIntentParameters(Context context, Intent intent, String backupContext) {
        if (!intent.hasExtra("EXTRA_CONTEXT")) {
            intent.putExtra("EXTRA_CONTEXT", backupContext);
        }
        intent.putExtra("EXTRA_THEME", 1);
        Resources.Theme theme = context.getTheme();
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        intent.putExtra("EXTRA_PRIMARY_COLOR", context.getColor(typedValue.resourceId));
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
