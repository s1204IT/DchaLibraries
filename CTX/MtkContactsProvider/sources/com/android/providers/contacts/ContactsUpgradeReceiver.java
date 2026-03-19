package com.android.providers.contacts;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import libcore.icu.ICU;

public class ContactsUpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            long jCurrentTimeMillis = System.currentTimeMillis();
            SharedPreferences sharedPreferences = context.getSharedPreferences("ContactsUpgradeReceiver", 0);
            int i = sharedPreferences.getInt("db_version", 0);
            String icuVersion = ICU.getIcuVersion();
            String osVersionString = getOsVersionString();
            String string = sharedPreferences.getString("icu_version", "");
            String string2 = sharedPreferences.getString("os_version", "");
            if (i != 1300 || !string.equals(icuVersion) || !string2.equals(osVersionString)) {
                SharedPreferences.Editor editorEdit = sharedPreferences.edit();
                editorEdit.putInt("db_version", 1300);
                editorEdit.putString("icu_version", icuVersion);
                editorEdit.putString("os_version", osVersionString);
                editorEdit.commit();
                ContactsDatabaseHelper contactsDatabaseHelper = ContactsDatabaseHelper.getInstance(context);
                ProfileDatabaseHelper profileDatabaseHelper = ProfileDatabaseHelper.getInstance(context);
                CallLogDatabaseHelper callLogDatabaseHelper = CallLogDatabaseHelper.getInstance(context);
                Log.i("ContactsUpgradeReceiver", "Creating or opening contacts database");
                contactsDatabaseHelper.getWritableDatabase();
                contactsDatabaseHelper.forceDirectoryRescan();
                profileDatabaseHelper.getWritableDatabase();
                callLogDatabaseHelper.getWritableDatabase();
                ContactsProvider2.updateLocaleOffline(context, contactsDatabaseHelper, profileDatabaseHelper);
                EventLogTags.writeContactsUpgradeReceiver(System.currentTimeMillis() - jCurrentTimeMillis);
            }
        } catch (Throwable th) {
            Log.e("ContactsUpgradeReceiver", "Error during upgrade attempt. Disabling receiver.", th);
            context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, getClass()), 2, 1);
        }
    }

    private static String getOsVersionString() {
        return Build.ID;
    }
}
