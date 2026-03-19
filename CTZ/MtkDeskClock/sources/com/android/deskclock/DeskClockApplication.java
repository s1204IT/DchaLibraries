package com.android.deskclock;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.UserManager;
import android.preference.PreferenceManager;
import com.android.deskclock.controller.Controller;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.LogEventTracker;
import com.android.deskclock.uidata.UiDataModel;

public class DeskClockApplication extends Application {
    private static final BroadcastReceiver mCompleteMigrationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!context.createDeviceProtectedStorageContext().moveSharedPreferencesFrom(context, PreferenceManager.getDefaultSharedPreferencesName(context))) {
                LogUtils.wtf("Failed to migrate database", new Object[0]);
            }
            LogUtils.v("[BroadcastReceiver]Migration completed successfully", new Object[0]);
            context.unregisterReceiver(DeskClockApplication.mCompleteMigrationReceiver);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Context applicationContext = getApplicationContext();
        SharedPreferences defaultSharedPreferences = getDefaultSharedPreferences(applicationContext);
        NotificationChannelManager.getInstance().firstInitIfNeeded(this);
        DataModel.getDataModel().init(applicationContext, defaultSharedPreferences);
        UiDataModel.getUiDataModel().init(applicationContext, defaultSharedPreferences);
        Controller.getController().setContext(applicationContext);
        Controller.getController().addEventTracker(new LogEventTracker(applicationContext));
    }

    @TargetApi(24)
    private static SharedPreferences getDefaultSharedPreferences(Context context) {
        if (Utils.isNOrLater()) {
            String defaultSharedPreferencesName = PreferenceManager.getDefaultSharedPreferencesName(context);
            Context contextCreateDeviceProtectedStorageContext = context.createDeviceProtectedStorageContext();
            if (UserManager.get(context).isUserUnlocked()) {
                if (!contextCreateDeviceProtectedStorageContext.moveSharedPreferencesFrom(context, defaultSharedPreferencesName)) {
                    LogUtils.wtf("Failed to migrate shared preferences", new Object[0]);
                }
                LogUtils.v("Migration completed successfully", new Object[0]);
            } else {
                LogUtils.v("[onCreate]User locked, register receiver for migration", new Object[0]);
                context.registerReceiver(mCompleteMigrationReceiver, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
            }
            context = contextCreateDeviceProtectedStorageContext;
        }
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
