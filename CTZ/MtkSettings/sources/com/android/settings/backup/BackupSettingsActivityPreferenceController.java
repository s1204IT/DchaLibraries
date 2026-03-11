package com.android.settings.backup;

import android.app.backup.BackupManager;
import android.content.Context;
import android.os.UserManager;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class BackupSettingsActivityPreferenceController extends BasePreferenceController {
    private static final String KEY_BACKUP_SETTINGS = "backup_settings";
    private static final String TAG = "BackupSettingActivityPC";
    private final BackupManager mBackupManager;
    private final UserManager mUm;

    public BackupSettingsActivityPreferenceController(Context context) {
        super(context, KEY_BACKUP_SETTINGS);
        this.mUm = (UserManager) context.getSystemService("user");
        this.mBackupManager = new BackupManager(context);
    }

    @Override
    public int getAvailabilityStatus() {
        if (this.mUm.isAdminUser()) {
            return 0;
        }
        return 2;
    }

    @Override
    public CharSequence getSummary() {
        if (this.mBackupManager.isBackupEnabled()) {
            return this.mContext.getText(R.string.accessibility_feature_state_on);
        }
        return this.mContext.getText(R.string.accessibility_feature_state_off);
    }
}
