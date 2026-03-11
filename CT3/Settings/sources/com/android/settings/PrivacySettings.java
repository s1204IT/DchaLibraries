package com.android.settings;

import android.app.Activity;
import android.app.backup.IBackupManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.RestrictedLockUtils;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PrivacySettings extends SettingsPreferenceFragment implements Indexable {
    private SwitchPreference mAutoRestore;
    private PreferenceScreen mBackup;
    private IBackupManager mBackupManager;
    private PreferenceScreen mConfigure;
    private boolean mEnabled;
    private ISettingsMiscExt mExt;
    private PreferenceScreen mManageData;
    private Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (!(preference instanceof SwitchPreference)) {
                return true;
            }
            boolean nextValue = ((Boolean) newValue).booleanValue();
            if (preference != PrivacySettings.this.mAutoRestore) {
                return false;
            }
            try {
                PrivacySettings.this.mBackupManager.setAutoRestore(nextValue);
                return true;
            } catch (RemoteException e) {
                PrivacySettings.this.mAutoRestore.setChecked(nextValue ? false : true);
                return false;
            }
        }
    };
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new PrivacySearchIndexProvider();

    @Override
    protected int getMetricsCategory() {
        return 81;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mEnabled = UserManager.get(getActivity()).isAdminUser();
        if (!this.mEnabled) {
            return;
        }
        addPreferencesFromResource(R.xml.privacy_settings);
        PreferenceScreen screen = getPreferenceScreen();
        this.mBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        this.mBackup = (PreferenceScreen) screen.findPreference("backup_data");
        this.mAutoRestore = (SwitchPreference) screen.findPreference("auto_restore");
        this.mAutoRestore.setOnPreferenceChangeListener(this.preferenceChangeListener);
        this.mConfigure = (PreferenceScreen) screen.findPreference("configure_account");
        this.mManageData = (PreferenceScreen) screen.findPreference("data_management");
        this.mExt = UtilsExt.getMiscPlugin(getActivity());
        this.mExt.setFactoryResetTitle(getActivity());
        Set<String> keysToRemove = new HashSet<>();
        getNonVisibleKeys(getActivity(), keysToRemove);
        int screenPreferenceCount = screen.getPreferenceCount();
        for (int i = screenPreferenceCount - 1; i >= 0; i--) {
            Preference preference = screen.getPreference(i);
            if (keysToRemove.contains(preference.getKey())) {
                screen.removePreference(preference);
            }
        }
        updateToggles();
        if (FeatureOption.MTK_DRM_APP) {
            return;
        }
        screen.removePreference(findPreference("drm_settings"));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!this.mEnabled) {
            return;
        }
        updateToggles();
    }

    private void updateToggles() {
        int i;
        ContentResolver res = getContentResolver();
        boolean backupEnabled = false;
        Intent configIntent = null;
        String configSummary = null;
        Intent manageIntent = null;
        String manageLabel = null;
        try {
            backupEnabled = this.mBackupManager.isBackupEnabled();
            String transport = this.mBackupManager.getCurrentTransport();
            configIntent = validatedActivityIntent(this.mBackupManager.getConfigurationIntent(transport), "config");
            configSummary = this.mBackupManager.getDestinationString(transport);
            manageIntent = validatedActivityIntent(this.mBackupManager.getDataManagementIntent(transport), "management");
            manageLabel = this.mBackupManager.getDataManagementLabel(transport);
            PreferenceScreen preferenceScreen = this.mBackup;
            if (backupEnabled) {
                i = R.string.accessibility_feature_state_on;
            } else {
                i = R.string.accessibility_feature_state_off;
            }
            preferenceScreen.setSummary(i);
        } catch (RemoteException e) {
            this.mBackup.setEnabled(false);
        }
        this.mAutoRestore.setChecked(Settings.Secure.getInt(res, "backup_auto_restore", 1) == 1);
        this.mAutoRestore.setEnabled(backupEnabled);
        this.mConfigure.setEnabled(configIntent != null ? backupEnabled : false);
        this.mConfigure.setIntent(configIntent);
        setConfigureSummary(configSummary);
        boolean manageEnabled = manageIntent != null ? backupEnabled : false;
        if (manageEnabled) {
            this.mManageData.setIntent(manageIntent);
            if (manageLabel == null) {
                return;
            }
            this.mManageData.setTitle(manageLabel);
            return;
        }
        getPreferenceScreen().removePreference(this.mManageData);
    }

    private Intent validatedActivityIntent(Intent intent, String logLabel) {
        if (intent != null) {
            PackageManager pm = getPackageManager();
            List<ResolveInfo> resolved = pm.queryIntentActivities(intent, 0);
            if (resolved == null || resolved.isEmpty()) {
                Log.e("PrivacySettings", "Backup " + logLabel + " intent " + ((Object) null) + " fails to resolve; ignoring");
                return null;
            }
            return intent;
        }
        return intent;
    }

    private void setConfigureSummary(String summary) {
        if (summary != null) {
            this.mConfigure.setSummary(summary);
        } else {
            this.mConfigure.setSummary(R.string.backup_configure_account_default_summary);
        }
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_backup_reset;
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (!listening) {
                return;
            }
            IBackupManager backupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
            try {
                boolean backupEnabled = backupManager.isBackupEnabled();
                if (backupEnabled) {
                    String transport = backupManager.getCurrentTransport();
                    String configSummary = backupManager.getDestinationString(transport);
                    if (configSummary != null) {
                        this.mSummaryLoader.setSummary(this, configSummary);
                    } else {
                        this.mSummaryLoader.setSummary(this, this.mContext.getString(R.string.backup_configure_account_default_summary));
                    }
                } else {
                    this.mSummaryLoader.setSummary(this, this.mContext.getString(R.string.backup_disabled));
                }
            } catch (RemoteException e) {
            }
        }
    }

    private static class PrivacySearchIndexProvider extends BaseSearchIndexProvider {
        boolean mIsPrimary;

        public PrivacySearchIndexProvider() {
            this.mIsPrimary = UserHandle.myUserId() == 0;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            List<SearchIndexableResource> result = new ArrayList<>();
            if (!this.mIsPrimary) {
                return result;
            }
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.privacy_settings;
            result.add(sir);
            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonVisibleKeys = new ArrayList<>();
            PrivacySettings.getNonVisibleKeys(context, nonVisibleKeys);
            return nonVisibleKeys;
        }
    }

    public static void getNonVisibleKeys(Context context, Collection<String> nonVisibleKeys) {
        IBackupManager backupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        boolean isServiceActive = false;
        try {
            isServiceActive = backupManager.isBackupServiceActive(UserHandle.myUserId());
        } catch (RemoteException e) {
            Log.w("PrivacySettings", "Failed querying backup manager service activity status. Assuming it is inactive.");
        }
        boolean vendorSpecific = context.getPackageManager().resolveContentProvider("com.google.settings", 0) == null;
        if (vendorSpecific || isServiceActive) {
            nonVisibleKeys.add("backup_inactive");
        }
        if (vendorSpecific || !isServiceActive) {
            nonVisibleKeys.add("backup_data");
            nonVisibleKeys.add("auto_restore");
            nonVisibleKeys.add("configure_account");
        }
        if (RestrictedLockUtils.hasBaseUserRestriction(context, "no_factory_reset", UserHandle.myUserId())) {
            nonVisibleKeys.add("factory_reset");
        }
        if (!RestrictedLockUtils.hasBaseUserRestriction(context, "no_network_reset", UserHandle.myUserId())) {
            return;
        }
        nonVisibleKeys.add("network_reset");
    }
}
