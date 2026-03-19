package com.android.server.telecom;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.telecom.Log;

public class QuickResponseUtils {
    public static void maybeMigrateLegacyQuickResponses(Context context) {
        boolean z;
        Context contextCreatePackageContext;
        Log.d("QuickResponseUtils", "maybeMigrateLegacyQuickResponses() - Starting", new Object[0]);
        SharedPreferences sharedPreferences = context.getSharedPreferences("respond_via_sms_prefs", 0);
        Resources resources = context.getResources();
        if (sharedPreferences.contains("canned_response_pref_1") || sharedPreferences.contains("canned_response_pref_2") || sharedPreferences.contains("canned_response_pref_3") || sharedPreferences.contains("canned_response_pref_4")) {
            z = true;
        } else {
            z = false;
        }
        if (z) {
            Log.d("QuickResponseUtils", "maybeMigrateLegacyQuickResponses() - Telecom QuickResponses exist", new Object[0]);
            return;
        }
        Log.d("QuickResponseUtils", "maybeMigrateLegacyQuickResponses() - No local QuickResponses", new Object[0]);
        try {
            contextCreatePackageContext = context.createPackageContext("com.android.phone", 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("QuickResponseUtils", e, "maybeMigrateLegacyQuickResponses() - Can't find Telephony package.", new Object[0]);
            contextCreatePackageContext = null;
        }
        if (contextCreatePackageContext != null) {
            Log.d("QuickResponseUtils", "maybeMigrateLegacyQuickResponses() - Using Telephony QuickResponses.", new Object[0]);
            SharedPreferences sharedPreferences2 = contextCreatePackageContext.getSharedPreferences("respond_via_sms_prefs", 0);
            if (!sharedPreferences2.contains("canned_response_pref_1")) {
                return;
            }
            String string = sharedPreferences2.getString("canned_response_pref_1", resources.getString(R.string.respond_via_sms_canned_response_1));
            String string2 = sharedPreferences2.getString("canned_response_pref_2", resources.getString(R.string.respond_via_sms_canned_response_2));
            String string3 = sharedPreferences2.getString("canned_response_pref_3", resources.getString(R.string.respond_via_sms_canned_response_3));
            String string4 = sharedPreferences2.getString("canned_response_pref_4", resources.getString(R.string.respond_via_sms_canned_response_4));
            SharedPreferences.Editor editorEdit = sharedPreferences.edit();
            editorEdit.putString("canned_response_pref_1", string);
            editorEdit.putString("canned_response_pref_2", string2);
            editorEdit.putString("canned_response_pref_3", string3);
            editorEdit.putString("canned_response_pref_4", string4);
            editorEdit.commit();
        }
        Log.d("QuickResponseUtils", "maybeMigrateLegacyQuickResponses() - Done.", new Object[0]);
    }
}
