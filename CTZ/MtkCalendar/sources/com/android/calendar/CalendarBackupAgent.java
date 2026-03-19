package com.android.calendar;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;
import java.io.IOException;

public class CalendarBackupAgent extends BackupAgentHelper {
    @Override
    public void onCreate() {
        addHelper("shared_pref", new SharedPreferencesBackupHelper(this, "com.android.calendar_preferences"));
    }

    @Override
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        getSharedPreferences("com.android.calendar_preferences_no_backup", 0).edit().putString("preferences_alerts_ringtone", "content://settings/system/notification_sound").commit();
        super.onRestore(backupDataInput, i, parcelFileDescriptor);
    }
}
