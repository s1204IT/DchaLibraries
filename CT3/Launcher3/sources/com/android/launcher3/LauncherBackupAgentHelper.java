package com.android.launcher3;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.android.launcher3.model.GridSizeMigrationTask;
import java.io.IOException;

public class LauncherBackupAgentHelper extends BackupAgentHelper {
    private LauncherBackupHelper mHelper;

    public static void dataChanged(Context context) {
        dataChanged(context, 0L);
    }

    public static void dataChanged(Context context, long throttleMs) {
        SharedPreferences prefs = Utilities.getPrefs(context);
        long now = System.currentTimeMillis();
        long lastTime = prefs.getLong("backup_manager_last_notified", 0L);
        if (now >= lastTime && now < lastTime + throttleMs) {
            return;
        }
        BackupManager.dataChanged(context.getPackageName());
        prefs.edit().putLong("backup_manager_last_notified", now).apply();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mHelper = new LauncherBackupHelper(this);
        addHelper("L", this.mHelper);
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        boolean hasData;
        if (!Utilities.ATLEAST_LOLLIPOP) {
            Log.i("LauncherBAHelper", "You shall not pass!!!");
            Log.d("LauncherBAHelper", "Restore is only supported on devices running Lollipop and above.");
            return;
        }
        LauncherAppState.getLauncherProvider().createEmptyDB();
        try {
            super.onRestore(data, appVersionCode, newState);
            Cursor c = getContentResolver().query(LauncherSettings$Favorites.CONTENT_URI, null, null, null, null);
            hasData = c.moveToNext();
            c.close();
        } catch (Exception e) {
            Log.e("LauncherBAHelper", "Restore failed", e);
            hasData = false;
        }
        if (hasData && this.mHelper.restoreSuccessful) {
            LauncherAppState.getLauncherProvider().clearFlagEmptyDbCreated();
            LauncherClings.markFirstRunClingDismissed(this);
            if (this.mHelper.restoredBackupVersion <= 3) {
                LauncherAppState.getLauncherProvider().updateFolderItemsRank();
            }
            if (GridSizeMigrationTask.ENABLED && this.mHelper.shouldAttemptWorkspaceMigration()) {
                GridSizeMigrationTask.markForMigration(getApplicationContext(), this.mHelper.widgetSizes, this.mHelper.migrationCompatibleProfileData);
            }
            LauncherAppState.getLauncherProvider().convertShortcutsToLauncherActivities();
            return;
        }
        LauncherAppState.getLauncherProvider().createEmptyDB();
    }
}
