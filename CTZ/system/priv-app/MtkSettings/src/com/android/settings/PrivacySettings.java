package com.android.settings;

import android.app.backup.IBackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/* loaded from: classes.dex */
public class PrivacySettings extends SettingsPreferenceFragment {
    static final String AUTO_RESTORE = "auto_restore";
    static final String BACKUP_DATA = "backup_data";
    static final String CONFIGURE_ACCOUNT = "configure_account";
    static final String DATA_MANAGEMENT = "data_management";
    private SwitchPreference mAutoRestore;
    private Preference mBackup;
    private IBackupManager mBackupManager;
    private Preference mConfigure;
    private boolean mEnabled;
    private Preference mManageData;
    private Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() { // from class: com.android.settings.PrivacySettings.1
        @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
        public boolean onPreferenceChange(Preference preference, Object obj) {
            if (!(preference instanceof SwitchPreference)) {
                return true;
            }
            boolean zBooleanValue = ((Boolean) obj).booleanValue();
            if (preference == PrivacySettings.this.mAutoRestore) {
                try {
                    PrivacySettings.this.mBackupManager.setAutoRestore(zBooleanValue);
                    return true;
                } catch (RemoteException e) {
                    PrivacySettings.this.mAutoRestore.setChecked(!zBooleanValue);
                    return false;
                }
            }
            return false;
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 81;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mEnabled = UserManager.get(getActivity()).isAdminUser();
        if (!this.mEnabled) {
            return;
        }
        addPreferencesFromResource(R.xml.privacy_settings);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        this.mBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        setPreferenceReferences(preferenceScreen);
        HashSet hashSet = new HashSet();
        getNonVisibleKeys(getActivity(), hashSet);
        for (int preferenceCount = preferenceScreen.getPreferenceCount() - 1; preferenceCount >= 0; preferenceCount--) {
            Preference preference = preferenceScreen.getPreference(preferenceCount);
            if (hashSet.contains(preference.getKey())) {
                preferenceScreen.removePreference(preference);
            }
        }
        updateToggles();
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        if (this.mEnabled) {
            updateToggles();
        }
    }

    void setPreferenceReferences(PreferenceScreen preferenceScreen) {
        this.mBackup = preferenceScreen.findPreference(BACKUP_DATA);
        this.mAutoRestore = (SwitchPreference) preferenceScreen.findPreference(AUTO_RESTORE);
        this.mAutoRestore.setOnPreferenceChangeListener(this.preferenceChangeListener);
        this.mConfigure = preferenceScreen.findPreference(CONFIGURE_ACCOUNT);
        this.mManageData = preferenceScreen.findPreference(DATA_MANAGEMENT);
    }

    /* JADX WARN: Removed duplicated region for block: B:30:0x0073  */
    /* JADX WARN: Removed duplicated region for block: B:31:0x0075  */
    /* JADX WARN: Removed duplicated region for block: B:34:0x0080 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:36:0x0084  */
    /* JADX WARN: Removed duplicated region for block: B:39:0x0095 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:43:0x009c  */
    /* JADX WARN: Removed duplicated region for block: B:46:0x00a9  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void updateToggles() {
        String dataManagementLabel;
        Intent intentValidatedActivityIntent;
        String destinationString;
        Intent intentValidatedActivityIntent2;
        boolean zIsBackupEnabled;
        String currentTransport;
        int i;
        ContentResolver contentResolver = getContentResolver();
        boolean z = false;
        try {
            zIsBackupEnabled = this.mBackupManager.isBackupEnabled();
            try {
                currentTransport = this.mBackupManager.getCurrentTransport();
                intentValidatedActivityIntent = validatedActivityIntent(this.mBackupManager.getConfigurationIntent(currentTransport), "config");
            } catch (RemoteException e) {
                dataManagementLabel = null;
                intentValidatedActivityIntent = null;
                destinationString = null;
            }
        } catch (RemoteException e2) {
            dataManagementLabel = null;
            intentValidatedActivityIntent = null;
            destinationString = null;
            intentValidatedActivityIntent2 = null;
            zIsBackupEnabled = false;
        }
        try {
            destinationString = this.mBackupManager.getDestinationString(currentTransport);
            try {
                intentValidatedActivityIntent2 = validatedActivityIntent(this.mBackupManager.getDataManagementIntent(currentTransport), "management");
                try {
                    dataManagementLabel = this.mBackupManager.getDataManagementLabel(currentTransport);
                    try {
                        Preference preference = this.mBackup;
                        if (zIsBackupEnabled) {
                            i = R.string.accessibility_feature_state_on;
                        } else {
                            i = R.string.accessibility_feature_state_off;
                        }
                        preference.setSummary(i);
                    } catch (RemoteException e3) {
                        this.mBackup.setEnabled(false);
                        this.mAutoRestore.setChecked(Settings.Secure.getInt(contentResolver, "backup_auto_restore", 1) == 1);
                        this.mAutoRestore.setEnabled(zIsBackupEnabled);
                        this.mConfigure.setEnabled(intentValidatedActivityIntent != null && zIsBackupEnabled);
                        this.mConfigure.setIntent(intentValidatedActivityIntent);
                        setConfigureSummary(destinationString);
                        if (intentValidatedActivityIntent2 != null) {
                        }
                        if (z) {
                        }
                    }
                } catch (RemoteException e4) {
                    dataManagementLabel = null;
                }
            } catch (RemoteException e5) {
                dataManagementLabel = null;
                intentValidatedActivityIntent2 = null;
            }
        } catch (RemoteException e6) {
            dataManagementLabel = null;
            destinationString = null;
            intentValidatedActivityIntent2 = destinationString;
            this.mBackup.setEnabled(false);
            this.mAutoRestore.setChecked(Settings.Secure.getInt(contentResolver, "backup_auto_restore", 1) == 1);
            this.mAutoRestore.setEnabled(zIsBackupEnabled);
            this.mConfigure.setEnabled(intentValidatedActivityIntent != null && zIsBackupEnabled);
            this.mConfigure.setIntent(intentValidatedActivityIntent);
            setConfigureSummary(destinationString);
            if (intentValidatedActivityIntent2 != null) {
                z = true;
            }
            if (z) {
            }
        }
        this.mAutoRestore.setChecked(Settings.Secure.getInt(contentResolver, "backup_auto_restore", 1) == 1);
        this.mAutoRestore.setEnabled(zIsBackupEnabled);
        this.mConfigure.setEnabled(intentValidatedActivityIntent != null && zIsBackupEnabled);
        this.mConfigure.setIntent(intentValidatedActivityIntent);
        setConfigureSummary(destinationString);
        if (intentValidatedActivityIntent2 != null && zIsBackupEnabled) {
            z = true;
        }
        if (z) {
            this.mManageData.setIntent(intentValidatedActivityIntent2);
            if (dataManagementLabel != null) {
                this.mManageData.setTitle(dataManagementLabel);
                return;
            }
            return;
        }
        getPreferenceScreen().removePreference(this.mManageData);
    }

    private Intent validatedActivityIntent(Intent intent, String str) {
        if (intent != null) {
            List<ResolveInfo> listQueryIntentActivities = getPackageManager().queryIntentActivities(intent, 0);
            if (listQueryIntentActivities == null || listQueryIntentActivities.isEmpty()) {
                Log.e("PrivacySettings", "Backup " + str + " intent " + ((Object) null) + " fails to resolve; ignoring");
                return null;
            }
            return intent;
        }
        return intent;
    }

    private void setConfigureSummary(String str) {
        if (str != null) {
            this.mConfigure.setSummary(str);
        } else {
            this.mConfigure.setSummary(R.string.backup_configure_account_default_summary);
        }
    }

    @Override // com.android.settings.support.actionbar.HelpResourceProvider
    public int getHelpResource() {
        return R.string.help_url_backup_reset;
    }

    private static void getNonVisibleKeys(Context context, Collection<String> collection) {
        boolean zIsBackupServiceActive;
        try {
            zIsBackupServiceActive = IBackupManager.Stub.asInterface(ServiceManager.getService("backup")).isBackupServiceActive(UserHandle.myUserId());
        } catch (RemoteException e) {
            Log.w("PrivacySettings", "Failed querying backup manager service activity status. Assuming it is inactive.");
            zIsBackupServiceActive = false;
        }
        boolean z = context.getPackageManager().resolveContentProvider("com.google.settings", 0) == null;
        if (z || zIsBackupServiceActive) {
            collection.add("backup_inactive");
        }
        if (z || !zIsBackupServiceActive) {
            collection.add(BACKUP_DATA);
            collection.add(AUTO_RESTORE);
            collection.add(CONFIGURE_ACCOUNT);
        }
    }
}
