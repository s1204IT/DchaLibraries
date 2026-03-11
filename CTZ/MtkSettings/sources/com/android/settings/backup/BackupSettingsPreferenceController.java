package com.android.settings.backup;

import android.content.Context;
import android.content.Intent;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class BackupSettingsPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private Intent mBackupSettingsIntent;
    private String mBackupSettingsSummary;
    private String mBackupSettingsTitle;
    private Intent mManufacturerIntent;
    private String mManufacturerLabel;

    public BackupSettingsPreferenceController(Context context) {
        super(context);
        BackupSettingsHelper backupSettingsHelper = new BackupSettingsHelper(context);
        this.mBackupSettingsIntent = backupSettingsHelper.getIntentForBackupSettings();
        this.mBackupSettingsTitle = backupSettingsHelper.getLabelForBackupSettings();
        this.mBackupSettingsSummary = backupSettingsHelper.getSummaryForBackupSettings();
        this.mManufacturerIntent = backupSettingsHelper.getIntentProvidedByManufacturer();
        this.mManufacturerLabel = backupSettingsHelper.getLabelProvidedByManufacturer();
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        Preference preferenceFindPreference = preferenceScreen.findPreference("backup_settings");
        Preference preferenceFindPreference2 = preferenceScreen.findPreference("manufacturer_backup");
        preferenceFindPreference.setIntent(this.mBackupSettingsIntent);
        preferenceFindPreference.setTitle(this.mBackupSettingsTitle);
        preferenceFindPreference.setSummary(this.mBackupSettingsSummary);
        preferenceFindPreference2.setIntent(this.mManufacturerIntent);
        preferenceFindPreference2.setTitle(this.mManufacturerLabel);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }
}
