package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.backup.IBackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import java.util.ArrayList;
import java.util.List;

public class PrivacySettings extends SettingsPreferenceFragment implements DialogInterface.OnClickListener, Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new PrivacySearchIndexProvider();
    private SwitchPreference mAutoRestore;
    private SwitchPreference mBackup;
    private IBackupManager mBackupManager;
    private PreferenceScreen mConfigure;
    private Dialog mConfirmDialog;
    private int mDialogType;
    private boolean mEnabled;
    private Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (!(preference instanceof SwitchPreference)) {
                return true;
            }
            boolean nextValue = ((Boolean) newValue).booleanValue();
            boolean result = false;
            if (preference != PrivacySettings.this.mBackup) {
                if (preference == PrivacySettings.this.mAutoRestore) {
                    try {
                        PrivacySettings.this.mBackupManager.setAutoRestore(nextValue);
                        result = true;
                    } catch (RemoteException e) {
                        PrivacySettings.this.mAutoRestore.setChecked(nextValue ? false : true);
                    }
                }
            } else if (!nextValue) {
                PrivacySettings.this.showEraseBackupDialog();
            } else {
                PrivacySettings.this.setBackupEnabled(true);
                result = true;
            }
            return result;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mEnabled = Process.myUserHandle().isOwner();
        if (this.mEnabled) {
            addPreferencesFromResource(R.xml.privacy_settings);
            PreferenceScreen screen = getPreferenceScreen();
            this.mBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
            this.mBackup = (SwitchPreference) screen.findPreference("backup_data");
            this.mBackup.setOnPreferenceChangeListener(this.preferenceChangeListener);
            this.mAutoRestore = (SwitchPreference) screen.findPreference("auto_restore");
            this.mAutoRestore.setOnPreferenceChangeListener(this.preferenceChangeListener);
            this.mConfigure = (PreferenceScreen) screen.findPreference("configure_account");
            ArrayList<String> keysToRemove = getNonVisibleKeys(getActivity());
            int screenPreferenceCount = screen.getPreferenceCount();
            for (int i = screenPreferenceCount - 1; i >= 0; i--) {
                Preference preference = screen.getPreference(i);
                if (keysToRemove.contains(preference.getKey())) {
                    screen.removePreference(preference);
                }
            }
            PreferenceCategory backupCategory = (PreferenceCategory) findPreference("backup_category");
            if (backupCategory != null) {
                int backupCategoryPreferenceCount = backupCategory.getPreferenceCount();
                for (int i2 = backupCategoryPreferenceCount - 1; i2 >= 0; i2--) {
                    Preference preference2 = backupCategory.getPreference(i2);
                    if (keysToRemove.contains(preference2.getKey())) {
                        backupCategory.removePreference(preference2);
                    }
                }
            }
            updateToggles();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mEnabled) {
            updateToggles();
        }
    }

    @Override
    public void onStop() {
        if (this.mConfirmDialog != null && this.mConfirmDialog.isShowing()) {
            this.mConfirmDialog.dismiss();
        }
        this.mConfirmDialog = null;
        this.mDialogType = 0;
        super.onStop();
    }

    private void showEraseBackupDialog() {
        this.mDialogType = 2;
        CharSequence msg = getResources().getText(R.string.backup_erase_dialog_message);
        this.mConfirmDialog = new AlertDialog.Builder(getActivity()).setMessage(msg).setTitle(R.string.backup_erase_dialog_title).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).show();
    }

    private void updateToggles() {
        ContentResolver res = getContentResolver();
        boolean backupEnabled = false;
        Intent configIntent = null;
        String configSummary = null;
        try {
            backupEnabled = this.mBackupManager.isBackupEnabled();
            String transport = this.mBackupManager.getCurrentTransport();
            configIntent = this.mBackupManager.getConfigurationIntent(transport);
            configSummary = this.mBackupManager.getDestinationString(transport);
        } catch (RemoteException e) {
            this.mBackup.setEnabled(false);
        }
        this.mBackup.setChecked(backupEnabled);
        this.mAutoRestore.setChecked(Settings.Secure.getInt(res, "backup_auto_restore", 1) == 1);
        this.mAutoRestore.setEnabled(backupEnabled);
        boolean configureEnabled = configIntent != null && backupEnabled;
        this.mConfigure.setEnabled(configureEnabled);
        this.mConfigure.setIntent(configIntent);
        setConfigureSummary(configSummary);
    }

    private void setConfigureSummary(String summary) {
        if (summary != null) {
            this.mConfigure.setSummary(summary);
        } else {
            this.mConfigure.setSummary(R.string.backup_configure_account_default_summary);
        }
    }

    private void updateConfigureSummary() {
        try {
            String transport = this.mBackupManager.getCurrentTransport();
            String summary = this.mBackupManager.getDestinationString(transport);
            setConfigureSummary(summary);
        } catch (RemoteException e) {
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (this.mDialogType == 2) {
            if (which == -1) {
                setBackupEnabled(false);
            } else if (which == -2) {
                setBackupEnabled(true);
            }
            updateConfigureSummary();
        }
        this.mDialogType = 0;
    }

    private void setBackupEnabled(boolean enable) {
        if (this.mBackupManager != null) {
            try {
                this.mBackupManager.setBackupEnabled(enable);
            } catch (RemoteException e) {
                this.mBackup.setChecked(!enable);
                this.mAutoRestore.setEnabled(enable ? false : true);
                return;
            }
        }
        this.mBackup.setChecked(enable);
        this.mAutoRestore.setEnabled(enable);
        this.mConfigure.setEnabled(enable);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_backup_reset;
    }

    private static class PrivacySearchIndexProvider extends BaseSearchIndexProvider {
        boolean mIsPrimary;

        public PrivacySearchIndexProvider() {
            this.mIsPrimary = UserHandle.myUserId() == 0;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            List<SearchIndexableResource> result = new ArrayList<>();
            if (this.mIsPrimary) {
                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.privacy_settings;
                result.add(sir);
            }
            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            return PrivacySettings.getNonVisibleKeys(context);
        }
    }

    private static ArrayList<String> getNonVisibleKeys(Context context) {
        ArrayList<String> nonVisibleKeys = new ArrayList<>();
        IBackupManager backupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        boolean isServiceActive = false;
        try {
            isServiceActive = backupManager.isBackupServiceActive(UserHandle.myUserId());
        } catch (RemoteException e) {
            Log.w("PrivacySettings", "Failed querying backup manager service activity status. Assuming it is inactive.");
        }
        if (isServiceActive) {
            nonVisibleKeys.add("backup_inactive");
        } else {
            nonVisibleKeys.add("auto_restore");
            nonVisibleKeys.add("configure_account");
            nonVisibleKeys.add("backup_data");
        }
        if (UserManager.get(context).hasUserRestriction("no_factory_reset")) {
            nonVisibleKeys.add("personal_data_category");
        }
        if (context.getPackageManager().resolveContentProvider("com.google.settings", 0) == null) {
            nonVisibleKeys.add("backup_category");
        }
        return nonVisibleKeys;
    }
}
