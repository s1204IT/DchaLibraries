package com.android.providers.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Slog;
import com.android.providers.media.MediaProvider;

public class MediaUpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MediaUpgradeReceiver", 0);
        int i = sharedPreferences.getInt("db_version", 0);
        int databaseVersion = MediaProvider.getDatabaseVersion(context);
        if (i == databaseVersion) {
            return;
        }
        sharedPreferences.edit().putInt("db_version", databaseVersion).commit();
        try {
            String[] list = context.getDatabasePath("foo").getParentFile().list();
            if (list == null) {
                return;
            }
            for (String str : list) {
                if (MediaProvider.isMediaDatabaseName(str)) {
                    long jCurrentTimeMillis = System.currentTimeMillis();
                    Slog.i("MediaUpgradeReceiver", "---> Start upgrade of media database " + str);
                    try {
                        SQLiteDatabase writableDatabase = new MediaProvider.DatabaseHelper(context, str, MediaProvider.isInternalMediaDatabaseName(str), false, null).getWritableDatabase();
                        if (writableDatabase != null) {
                            writableDatabase.close();
                        }
                    } catch (Throwable th) {
                        Log.wtf("MediaUpgradeReceiver", "Error during upgrade of media db " + str, th);
                    }
                    Slog.i("MediaUpgradeReceiver", "<--- Finished upgrade of media database " + str + " in " + (System.currentTimeMillis() - jCurrentTimeMillis) + "ms");
                }
            }
        } catch (Throwable th2) {
            Log.wtf("MediaUpgradeReceiver", "Error during upgrade attempt.", th2);
        }
    }
}
