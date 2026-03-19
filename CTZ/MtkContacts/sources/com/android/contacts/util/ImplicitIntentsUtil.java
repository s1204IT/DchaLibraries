package com.android.contacts.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.quickcontact.QuickContactActivity;
import java.util.List;

public class ImplicitIntentsUtil {
    public static void startActivityInAppIfPossible(Context context, Intent intent) {
        Intent intentInAppIfExists = getIntentInAppIfExists(context, intent);
        if (intentInAppIfExists != null) {
            context.startActivity(intentInAppIfExists);
        } else {
            context.startActivity(intent);
        }
    }

    public static void startActivityInApp(Context context, Intent intent) {
        intent.setPackage(context.getPackageName());
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.missing_app, 0).show();
        }
    }

    public static void startActivityOutsideApp(Context context, Intent intent) {
        context.startActivity(intent);
    }

    public static void startQuickContact(Activity activity, Uri uri, int i) {
        startActivityInApp(activity, composeQuickContactIntent(activity, uri, i));
    }

    public static Intent composeQuickContactIntent(Context context, Uri uri, int i) {
        return composeQuickContactIntent(context, uri, 4, i);
    }

    public static Intent composeQuickContactIntent(Context context, Uri uri, int i, int i2) {
        Intent intent = new Intent(context, (Class<?>) QuickContactActivity.class);
        intent.setAction("android.provider.action.QUICK_CONTACT");
        intent.setData(uri);
        intent.putExtra("android.provider.extra.MODE", i);
        intent.addFlags(67108864);
        intent.putExtra("previous_screen_type", i2);
        return intent;
    }

    public static Intent getIntentForAddingAccount() {
        Intent intent = new Intent("android.settings.SYNC_SETTINGS");
        intent.setFlags(524288);
        intent.putExtra("authorities", new String[]{"com.android.contacts"});
        return intent;
    }

    public static Intent getIntentForQuickContactLauncherShortcut(Context context, Uri uri) {
        Intent intentComposeQuickContactIntent = composeQuickContactIntent(context, uri, 3, 0);
        intentComposeQuickContactIntent.setPackage(context.getPackageName());
        intentComposeQuickContactIntent.addFlags(268533760);
        intentComposeQuickContactIntent.putExtra("com.android.launcher.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION", true);
        intentComposeQuickContactIntent.putExtra("android.provider.extra.EXCLUDE_MIMES", (String[]) null);
        return intentComposeQuickContactIntent;
    }

    private static Intent getIntentInAppIfExists(Context context, Intent intent) {
        try {
            Intent intent2 = new Intent(intent);
            intent2.setPackage(context.getPackageName());
            List<ResolveInfo> listQueryIntentActivities = context.getPackageManager().queryIntentActivities(intent2, 65536);
            if (listQueryIntentActivities == null || listQueryIntentActivities.size() == 0) {
                return null;
            }
            if (listQueryIntentActivities.get(0).activityInfo != null && !TextUtils.isEmpty(listQueryIntentActivities.get(0).activityInfo.name)) {
                intent2.setClassName(context.getPackageName(), listQueryIntentActivities.get(0).activityInfo.name);
            }
            return intent2;
        } catch (Exception e) {
            return null;
        }
    }
}
