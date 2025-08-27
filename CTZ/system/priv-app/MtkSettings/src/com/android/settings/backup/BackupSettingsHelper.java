package com.android.settings.backup;

import android.app.backup.IBackupManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Settings;
import java.net.URISyntaxException;

/* loaded from: classes.dex */
public class BackupSettingsHelper {
    private IBackupManager mBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
    private Context mContext;

    public BackupSettingsHelper(Context context) {
        this.mContext = context;
    }

    public Intent getIntentForBackupSettings() {
        if (isIntentProvidedByTransport()) {
            return getIntentForBackupSettingsFromTransport();
        }
        Log.e("BackupSettingsHelper", "Backup transport has not provided an intent or the component for the intent is not found!");
        return getIntentForDefaultBackupSettings();
    }

    public String getLabelForBackupSettings() {
        String labelFromBackupTransport = getLabelFromBackupTransport();
        if (labelFromBackupTransport == null || labelFromBackupTransport.isEmpty()) {
            return this.mContext.getString(R.string.privacy_settings_title);
        }
        return labelFromBackupTransport;
    }

    public String getSummaryForBackupSettings() {
        String summaryFromBackupTransport = getSummaryFromBackupTransport();
        if (summaryFromBackupTransport == null) {
            return this.mContext.getString(R.string.backup_configure_account_default_summary);
        }
        return summaryFromBackupTransport;
    }

    public boolean isBackupProvidedByManufacturer() throws Resources.NotFoundException {
        if (Log.isLoggable("BackupSettingsHelper", 3)) {
            Log.d("BackupSettingsHelper", "Checking if intent provided by manufacturer");
        }
        String string = this.mContext.getResources().getString(R.string.config_backup_settings_intent);
        return (string == null || string.isEmpty()) ? false : true;
    }

    public String getLabelProvidedByManufacturer() {
        return this.mContext.getResources().getString(R.string.config_backup_settings_label);
    }

    public Intent getIntentProvidedByManufacturer() throws Resources.NotFoundException {
        if (Log.isLoggable("BackupSettingsHelper", 3)) {
            Log.d("BackupSettingsHelper", "Getting a backup settings intent provided by manufacturer");
        }
        String string = this.mContext.getResources().getString(R.string.config_backup_settings_intent);
        if (string != null && !string.isEmpty()) {
            try {
                return Intent.parseUri(string, 0);
            } catch (URISyntaxException e) {
                Log.e("BackupSettingsHelper", "Invalid intent provided by the manufacturer.", e);
                return null;
            }
        }
        return null;
    }

    Intent getIntentForBackupSettingsFromTransport() {
        Intent intentFromBackupTransport = getIntentFromBackupTransport();
        if (intentFromBackupTransport != null) {
            intentFromBackupTransport.putExtra("backup_services_available", isBackupServiceActive());
        }
        return intentFromBackupTransport;
    }

    private Intent getIntentForDefaultBackupSettings() {
        return new Intent(this.mContext, (Class<?>) Settings.PrivacySettingsActivity.class);
    }

    boolean isIntentProvidedByTransport() {
        Intent intentFromBackupTransport = getIntentFromBackupTransport();
        return (intentFromBackupTransport == null || intentFromBackupTransport.resolveActivity(this.mContext.getPackageManager()) == null) ? false : true;
    }

    private Intent getIntentFromBackupTransport() {
        try {
            Intent dataManagementIntent = this.mBackupManager.getDataManagementIntent(this.mBackupManager.getCurrentTransport());
            if (Log.isLoggable("BackupSettingsHelper", 3)) {
                if (dataManagementIntent != null) {
                    Log.d("BackupSettingsHelper", "Parsed intent from backup transport: " + dataManagementIntent.toString());
                } else {
                    Log.d("BackupSettingsHelper", "Received a null intent from backup transport");
                }
            }
            return dataManagementIntent;
        } catch (RemoteException e) {
            Log.e("BackupSettingsHelper", "Error getting data management intent", e);
            return null;
        }
    }

    private boolean isBackupServiceActive() {
        try {
            return this.mBackupManager.isBackupServiceActive(UserHandle.myUserId());
        } catch (Exception e) {
            return false;
        }
    }

    String getLabelFromBackupTransport() {
        try {
            String dataManagementLabel = this.mBackupManager.getDataManagementLabel(this.mBackupManager.getCurrentTransport());
            if (Log.isLoggable("BackupSettingsHelper", 3)) {
                Log.d("BackupSettingsHelper", "Received the backup settings label from backup transport: " + dataManagementLabel);
            }
            return dataManagementLabel;
        } catch (RemoteException e) {
            Log.e("BackupSettingsHelper", "Error getting data management label", e);
            return null;
        }
    }

    String getSummaryFromBackupTransport() {
        try {
            String destinationString = this.mBackupManager.getDestinationString(this.mBackupManager.getCurrentTransport());
            if (Log.isLoggable("BackupSettingsHelper", 3)) {
                Log.d("BackupSettingsHelper", "Received the backup settings summary from backup transport: " + destinationString);
            }
            return destinationString;
        } catch (RemoteException e) {
            Log.e("BackupSettingsHelper", "Error getting data management summary", e);
            return null;
        }
    }
}
