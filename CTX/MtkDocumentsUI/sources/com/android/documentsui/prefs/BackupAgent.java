package com.android.documentsui.prefs;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import java.io.IOException;

public class BackupAgent extends BackupAgentHelper {
    private SharedPreferences mBackupPreferences;
    private PrefsBackupHelper mPrefsBackupHelper;

    @Override
    public void onCreate() {
        addHelper("DOCUMENTSUI_BACKUP_HELPER_KEY", new SharedPreferencesBackupHelper(this, "documentsui_backup_prefs"));
        this.mPrefsBackupHelper = new PrefsBackupHelper(this);
        this.mBackupPreferences = getSharedPreferences("documentsui_backup_prefs", 0);
    }

    @Override
    public void onBackup(ParcelFileDescriptor parcelFileDescriptor, BackupDataOutput backupDataOutput, ParcelFileDescriptor parcelFileDescriptor2) throws IOException {
        this.mPrefsBackupHelper.getBackupPreferences(this.mBackupPreferences);
        super.onBackup(parcelFileDescriptor, backupDataOutput, parcelFileDescriptor2);
        this.mBackupPreferences.edit().clear().apply();
    }

    @Override
    public void onRestore(BackupDataInput backupDataInput, int i, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        super.onRestore(backupDataInput, i, parcelFileDescriptor);
        this.mPrefsBackupHelper.putBackupPreferences(this.mBackupPreferences);
        this.mBackupPreferences.edit().clear().apply();
    }
}
