package com.android.providers.telephony;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.SmsApplication;

public class ProviderUtil {
    public static boolean isAccessRestricted(Context context, String str, int i) {
        return (i == 1000 || i == 1001 || SmsApplication.isDefaultSmsApplication(context, str)) ? false : true;
    }

    public static boolean shouldSetCreator(ContentValues contentValues, int i) {
        return ((i == 1000 || i == 1001) && (contentValues.containsKey("creator") || contentValues.containsKey("creator"))) ? false : true;
    }

    public static boolean shouldRemoveCreator(ContentValues contentValues, int i) {
        return (i == 1000 || i == 1001 || (!contentValues.containsKey("creator") && !contentValues.containsKey("creator"))) ? false : true;
    }

    public static void notifyIfNotDefaultSmsApp(Uri uri, String str, Context context) {
        if (TextUtils.equals(str, Telephony.Sms.getDefaultSmsPackage(context))) {
            if (Log.isLoggable("SmsProvider", 2)) {
                Log.d("SmsProvider", "notifyIfNotDefaultSmsApp - called from default sms app");
                return;
            }
            return;
        }
        ComponentName defaultExternalTelephonyProviderChangedApplication = SmsApplication.getDefaultExternalTelephonyProviderChangedApplication(context, true);
        if (defaultExternalTelephonyProviderChangedApplication == null) {
            return;
        }
        Intent intent = new Intent("android.provider.action.EXTERNAL_PROVIDER_CHANGE");
        intent.setFlags(536870912);
        intent.setComponent(defaultExternalTelephonyProviderChangedApplication);
        if (uri != null) {
            intent.setData(uri);
        }
        if (Log.isLoggable("SmsProvider", 2)) {
            Log.d("SmsProvider", "notifyIfNotDefaultSmsApp - called from " + str + ", notifying");
        }
        intent.setFlags(1);
        context.sendBroadcast(intent);
    }

    public static Context getCredentialEncryptedContext(Context context) {
        if (context.isCredentialProtectedStorage()) {
            return context;
        }
        return context.createCredentialProtectedStorageContext();
    }

    public static Context getDeviceEncryptedContext(Context context) {
        if (context.isDeviceProtectedStorage()) {
            return context;
        }
        return context.createDeviceProtectedStorageContext();
    }
}
