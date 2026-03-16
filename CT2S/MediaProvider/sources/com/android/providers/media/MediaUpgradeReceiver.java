package com.android.providers.media;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.providers.media.MediaProvider;
import java.io.File;

public class MediaUpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("MediaUpgradeReceiver", 0);
        int prefVersion = prefs.getInt("db_version", 0);
        int dbVersion = MediaProvider.getDatabaseVersion(context);
        if (prefVersion != dbVersion) {
            prefs.edit().putInt("db_version", dbVersion).commit();
            try {
                File dbDir = context.getDatabasePath("foo").getParentFile();
                String[] files = dbDir.list();
                if (files != null) {
                    for (String file : files) {
                        if (MediaProvider.isMediaDatabaseName(file)) {
                            try {
                                ActivityManagerNative.getDefault().showBootMessage(context.getText(R.string.upgrade_msg), true);
                            } catch (RemoteException e) {
                            }
                            long startTime = System.currentTimeMillis();
                            Slog.i("MediaUpgradeReceiver", "---> Start upgrade of media database " + file);
                            SQLiteDatabase db = null;
                            try {
                                try {
                                    MediaProvider.DatabaseHelper helper = new MediaProvider.DatabaseHelper(context, file, MediaProvider.isInternalMediaDatabaseName(file), false, null);
                                    SQLiteDatabase db2 = helper.getWritableDatabase();
                                    if (db2 != null) {
                                        db2.close();
                                    }
                                } catch (Throwable t) {
                                    Log.wtf("MediaUpgradeReceiver", "Error during upgrade of media db " + file, t);
                                    if (0 != 0) {
                                        db.close();
                                    }
                                }
                                Slog.i("MediaUpgradeReceiver", "<--- Finished upgrade of media database " + file + " in " + (System.currentTimeMillis() - startTime) + "ms");
                            } catch (Throwable th) {
                                if (0 != 0) {
                                    db.close();
                                }
                                throw th;
                            }
                        }
                    }
                }
            } catch (Throwable t2) {
                Log.wtf("MediaUpgradeReceiver", "Error during upgrade attempt.", t2);
            }
        }
    }
}
