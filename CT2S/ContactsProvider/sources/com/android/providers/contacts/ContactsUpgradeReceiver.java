package com.android.providers.contacts;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Log;
import libcore.icu.ICU;

public class ContactsUpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            long startTime = System.currentTimeMillis();
            SharedPreferences prefs = context.getSharedPreferences("ContactsUpgradeReceiver", 0);
            int prefDbVersion = prefs.getInt("db_version", 0);
            String curIcuVersion = ICU.getIcuVersion();
            String prefIcuVersion = prefs.getString("icu_version", "");
            if (prefDbVersion != 911 || !prefIcuVersion.equals(curIcuVersion)) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("db_version", 911);
                editor.putString("icu_version", curIcuVersion);
                editor.commit();
                ContactsDatabaseHelper helper = ContactsDatabaseHelper.getInstance(context);
                ProfileDatabaseHelper profileHelper = ProfileDatabaseHelper.getInstance(context);
                Log.i("ContactsUpgradeReceiver", "Creating or opening contacts database");
                try {
                    ActivityManagerNative.getDefault().showBootMessage(context.getText(R.string.upgrade_msg), true);
                } catch (RemoteException e) {
                }
                helper.getWritableDatabase();
                profileHelper.getWritableDatabase();
                ContactsProvider2.updateLocaleOffline(context, helper, profileHelper);
                EventLogTags.writeContactsUpgradeReceiver(System.currentTimeMillis() - startTime);
            }
        } catch (Throwable t) {
            Log.wtf("ContactsUpgradeReceiver", "Error during upgrade attempt. Disabling receiver.", t);
            context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, getClass()), 2, 1);
        }
    }
}
