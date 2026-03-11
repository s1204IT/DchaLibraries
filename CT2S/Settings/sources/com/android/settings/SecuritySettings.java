package com.android.settings;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.security.KeyStore;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.TrustAgentUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.List;

public class SecuritySettings extends SettingsPreferenceFragment implements DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener, Indexable {
    private SwitchPreference mBiometricWeakLiveliness;
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private DevicePolicyManager mDPM;
    private boolean mIsPrimary;
    private KeyStore mKeyStore;
    private ListPreference mLockAfter;
    private LockPatternUtils mLockPatternUtils;
    private SwitchPreference mPowerButtonInstantlyLocks;
    private Preference mResetCredentials;
    private SwitchPreference mShowPassword;
    private SubscriptionManager mSubscriptionManager;
    private SwitchPreference mToggleAppInstallation;
    private Intent mTrustAgentClickIntent;
    private SwitchPreference mVisiblePattern;
    private DialogInterface mWarnInstallApps;
    private static final Intent TRUST_AGENT_INTENT = new Intent("android.service.trust.TrustAgentService");
    private static final String[] SWITCH_PREFERENCE_KEYS = {"lock_after_timeout", "lockenabled", "visiblepattern", "biometric_weak_liveliness", "power_button_instantly_locks", "show_password", "toggle_install_applications"};
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new SecuritySearchIndexProvider();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mSubscriptionManager = SubscriptionManager.from(getActivity());
        this.mLockPatternUtils = new LockPatternUtils(getActivity());
        this.mDPM = (DevicePolicyManager) getSystemService("device_policy");
        this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        if (savedInstanceState != null && savedInstanceState.containsKey("trust_agent_click_intent")) {
            this.mTrustAgentClickIntent = (Intent) savedInstanceState.getParcelable("trust_agent_click_intent");
        }
    }

    public static int getResIdForLockUnlockScreen(Context context, LockPatternUtils lockPatternUtils) {
        if (!lockPatternUtils.isSecure()) {
            UserManager mUm = (UserManager) context.getSystemService("user");
            List<UserInfo> users = mUm.getUsers(true);
            boolean singleUser = users.size() == 1;
            if (singleUser && lockPatternUtils.isLockScreenDisabled()) {
                return R.xml.security_settings_lockscreen;
            }
            return R.xml.security_settings_chooser;
        }
        if (lockPatternUtils.usingBiometricWeak() && lockPatternUtils.isBiometricWeakInstalled()) {
            return R.xml.security_settings_biometric_weak;
        }
        switch (lockPatternUtils.getKeyguardStoredPasswordQuality()) {
            case 65536:
                return R.xml.security_settings_pattern;
            case 131072:
            case 196608:
                return R.xml.security_settings_pin;
            case 262144:
            case 327680:
            case 393216:
                return R.xml.security_settings_password;
            default:
                return 0;
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        Preference manageAgents;
        Preference ownerInfoPref;
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.security_settings);
        PreferenceScreen root2 = getPreferenceScreen();
        int resid = getResIdForLockUnlockScreen(getActivity(), this.mLockPatternUtils);
        addPreferencesFromResource(resid);
        this.mIsPrimary = UserHandle.myUserId() == 0;
        if (!this.mIsPrimary && (ownerInfoPref = findPreference("owner_info_settings")) != null) {
            if (UserManager.get(getActivity()).isLinkedUser()) {
                ownerInfoPref.setTitle(R.string.profile_info_settings_title);
            } else {
                ownerInfoPref.setTitle(R.string.user_info_settings_title);
            }
        }
        if (this.mIsPrimary) {
            if (LockPatternUtils.isDeviceEncryptionEnabled()) {
                addPreferencesFromResource(R.xml.security_settings_encrypted);
            } else {
                addPreferencesFromResource(R.xml.security_settings_unencrypted);
            }
        }
        PreferenceGroup securityCategory = (PreferenceGroup) root2.findPreference("security_category");
        if (securityCategory != null) {
            boolean hasSecurity = this.mLockPatternUtils.isSecure();
            ArrayList<TrustAgentUtils.TrustAgentComponentInfo> agents = getActiveTrustAgents(getPackageManager(), this.mLockPatternUtils);
            for (int i = 0; i < agents.size(); i++) {
                TrustAgentUtils.TrustAgentComponentInfo agent = agents.get(i);
                Preference trustAgentPreference = new Preference(securityCategory.getContext());
                trustAgentPreference.setKey("trust_agent");
                trustAgentPreference.setTitle(agent.title);
                trustAgentPreference.setSummary(agent.summary);
                Intent intent = new Intent();
                intent.setComponent(agent.componentName);
                intent.setAction("android.intent.action.MAIN");
                trustAgentPreference.setIntent(intent);
                securityCategory.addPreference(trustAgentPreference);
                if (!hasSecurity) {
                    trustAgentPreference.setEnabled(false);
                    trustAgentPreference.setSummary(R.string.disabled_because_no_backup_security);
                }
            }
        }
        this.mLockAfter = (ListPreference) root2.findPreference("lock_after_timeout");
        if (this.mLockAfter != null) {
            setupLockAfterPreference();
            updateLockAfterPreferenceSummary();
        }
        this.mBiometricWeakLiveliness = (SwitchPreference) root2.findPreference("biometric_weak_liveliness");
        this.mVisiblePattern = (SwitchPreference) root2.findPreference("visiblepattern");
        this.mPowerButtonInstantlyLocks = (SwitchPreference) root2.findPreference("power_button_instantly_locks");
        Preference trustAgentPreference2 = root2.findPreference("trust_agent");
        if (this.mPowerButtonInstantlyLocks != null && trustAgentPreference2 != null && trustAgentPreference2.getTitle().length() > 0) {
            this.mPowerButtonInstantlyLocks.setSummary(getString(R.string.lockpattern_settings_power_button_instantly_locks_summary, new Object[]{trustAgentPreference2.getTitle()}));
        }
        if (resid == R.xml.security_settings_biometric_weak && this.mLockPatternUtils.getKeyguardStoredPasswordQuality() != 65536 && securityCategory != null && this.mVisiblePattern != null) {
            securityCategory.removePreference(root2.findPreference("visiblepattern"));
        }
        addPreferencesFromResource(R.xml.security_settings_misc);
        TelephonyManager.getDefault();
        if (!this.mIsPrimary || !isSimIccReady()) {
            root2.removePreference(root2.findPreference("sim_lock"));
        } else {
            root2.findPreference("sim_lock").setEnabled(isSimReady());
        }
        if (Settings.System.getInt(getContentResolver(), "lock_to_app_enabled", 0) != 0) {
            root2.findPreference("screen_pinning_settings").setSummary(getResources().getString(R.string.switch_on_text));
        }
        this.mShowPassword = (SwitchPreference) root2.findPreference("show_password");
        this.mResetCredentials = root2.findPreference("credentials_reset");
        UserManager um = (UserManager) getActivity().getSystemService("user");
        this.mKeyStore = KeyStore.getInstance();
        if (!um.hasUserRestriction("no_config_credentials")) {
            Preference credentialStorageType = root2.findPreference("credential_storage_type");
            int storageSummaryRes = this.mKeyStore.isHardwareBacked() ? R.string.credential_storage_type_hardware : R.string.credential_storage_type_software;
            credentialStorageType.setSummary(storageSummaryRes);
        } else {
            PreferenceGroup credentialsManager = (PreferenceGroup) root2.findPreference("credentials_management");
            credentialsManager.removePreference(root2.findPreference("credentials_reset"));
            credentialsManager.removePreference(root2.findPreference("credentials_install"));
            credentialsManager.removePreference(root2.findPreference("credential_storage_type"));
        }
        this.mToggleAppInstallation = (SwitchPreference) findPreference("toggle_install_applications");
        this.mToggleAppInstallation.setChecked(isNonMarketAppsAllowed());
        this.mToggleAppInstallation.setEnabled(!um.getUserInfo(UserHandle.myUserId()).isRestricted());
        if (um.hasUserRestriction("no_install_unknown_sources") || um.hasUserRestriction("no_install_apps")) {
            this.mToggleAppInstallation.setEnabled(false);
        }
        PreferenceGroup advancedCategory = (PreferenceGroup) root2.findPreference("advanced_security");
        if (advancedCategory != null && (manageAgents = advancedCategory.findPreference("manage_trust_agents")) != null && !this.mLockPatternUtils.isSecure()) {
            manageAgents.setEnabled(false);
            manageAgents.setSummary(R.string.disabled_because_no_backup_security);
        }
        Index.getInstance(getActivity()).updateFromClassNameResource(SecuritySettings.class.getName(), true, true);
        for (int i2 = 0; i2 < SWITCH_PREFERENCE_KEYS.length; i2++) {
            Preference pref = findPreference(SWITCH_PREFERENCE_KEYS[i2]);
            if (pref != null) {
                pref.setOnPreferenceChangeListener(this);
            }
        }
        return root2;
    }

    private boolean isSimIccReady() {
        TelephonyManager tm = TelephonyManager.getDefault();
        List<SubscriptionInfo> subInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (tm.hasIccCard(subInfo.getSimSlotIndex())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSimReady() {
        List<SubscriptionInfo> subInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                int simState = TelephonyManager.getDefault().getSimState(subInfo.getSimSlotIndex());
                if (simState != 1 && simState != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ArrayList<TrustAgentUtils.TrustAgentComponentInfo> getActiveTrustAgents(PackageManager pm, LockPatternUtils utils) {
        ArrayList<TrustAgentUtils.TrustAgentComponentInfo> result = new ArrayList<>();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(TRUST_AGENT_INTENT, 128);
        List<ComponentName> enabledTrustAgents = utils.getEnabledTrustAgents();
        if (enabledTrustAgents != null && !enabledTrustAgents.isEmpty()) {
            int i = 0;
            while (true) {
                if (i >= resolveInfos.size()) {
                    break;
                }
                ResolveInfo resolveInfo = resolveInfos.get(i);
                if (resolveInfo.serviceInfo != null && TrustAgentUtils.checkProvidePermission(resolveInfo, pm)) {
                    TrustAgentUtils.TrustAgentComponentInfo trustAgentComponentInfo = TrustAgentUtils.getSettingsComponent(pm, resolveInfo);
                    if (trustAgentComponentInfo.componentName != null && enabledTrustAgents.contains(TrustAgentUtils.getComponentName(resolveInfo)) && !TextUtils.isEmpty(trustAgentComponentInfo.title)) {
                        result.add(trustAgentComponentInfo);
                        break;
                    }
                }
                i++;
            }
        }
        return result;
    }

    private boolean isNonMarketAppsAllowed() {
        return Settings.Global.getInt(getContentResolver(), "install_non_market_apps", 0) > 0;
    }

    private void setNonMarketAppsAllowed(boolean enabled) {
        UserManager um = (UserManager) getActivity().getSystemService("user");
        if (!um.hasUserRestriction("no_install_unknown_sources")) {
            Settings.Global.putInt(getContentResolver(), "install_non_market_apps", enabled ? 1 : 0);
        }
    }

    private void warnAppInstallation() {
        this.mWarnInstallApps = new AlertDialog.Builder(getActivity()).setTitle(getResources().getString(R.string.error_title)).setIcon(android.R.drawable.ic_dialog_alert).setMessage(getResources().getString(R.string.install_all_warning)).setPositiveButton(android.R.string.yes, this).setNegativeButton(android.R.string.no, this).show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog == this.mWarnInstallApps) {
            boolean turnOn = which == -1;
            setNonMarketAppsAllowed(turnOn);
            if (this.mToggleAppInstallation != null) {
                this.mToggleAppInstallation.setChecked(turnOn);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mWarnInstallApps != null) {
            this.mWarnInstallApps.dismiss();
        }
    }

    private void setupLockAfterPreference() {
        long currentTimeout = Settings.Secure.getLong(getContentResolver(), "lock_screen_lock_after_timeout", 5000L);
        this.mLockAfter.setValue(String.valueOf(currentTimeout));
        this.mLockAfter.setOnPreferenceChangeListener(this);
        long adminTimeout = this.mDPM != null ? this.mDPM.getMaximumTimeToLock(null) : 0L;
        long displayTimeout = Math.max(0, Settings.System.getInt(getContentResolver(), "screen_off_timeout", 0));
        if (adminTimeout > 0) {
            disableUnusableTimeouts(Math.max(0L, adminTimeout - displayTimeout));
        }
    }

    private void updateLockAfterPreferenceSummary() {
        long currentTimeout = Settings.Secure.getLong(getContentResolver(), "lock_screen_lock_after_timeout", 5000L);
        CharSequence[] entries = this.mLockAfter.getEntries();
        CharSequence[] values = this.mLockAfter.getEntryValues();
        int best = 0;
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString()).longValue();
            if (currentTimeout >= timeout) {
                best = i;
            }
        }
        Preference preference = getPreferenceScreen().findPreference("trust_agent");
        if (preference != null && preference.getTitle().length() > 0) {
            this.mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary_with_exception, new Object[]{entries[best], preference.getTitle()}));
        } else {
            this.mLockAfter.setSummary(getString(R.string.lock_after_timeout_summary, new Object[]{entries[best]}));
        }
    }

    private void disableUnusableTimeouts(long maxTimeout) {
        CharSequence[] entries = this.mLockAfter.getEntries();
        CharSequence[] values = this.mLockAfter.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<>();
        ArrayList<CharSequence> revisedValues = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.valueOf(values[i].toString()).longValue();
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            this.mLockAfter.setEntries((CharSequence[]) revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            this.mLockAfter.setEntryValues((CharSequence[]) revisedValues.toArray(new CharSequence[revisedValues.size()]));
            int userPreference = Integer.valueOf(this.mLockAfter.getValue()).intValue();
            if (userPreference <= maxTimeout) {
                this.mLockAfter.setValue(String.valueOf(userPreference));
            }
        }
        this.mLockAfter.setEnabled(revisedEntries.size() > 0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mTrustAgentClickIntent != null) {
            outState.putParcelable("trust_agent_click_intent", this.mTrustAgentClickIntent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();
        LockPatternUtils lockPatternUtils = this.mChooseLockSettingsHelper.utils();
        if (this.mBiometricWeakLiveliness != null) {
            this.mBiometricWeakLiveliness.setChecked(lockPatternUtils.isBiometricWeakLivelinessEnabled());
        }
        if (this.mVisiblePattern != null) {
            this.mVisiblePattern.setChecked(lockPatternUtils.isVisiblePatternEnabled());
        }
        if (this.mPowerButtonInstantlyLocks != null) {
            this.mPowerButtonInstantlyLocks.setChecked(lockPatternUtils.getPowerButtonInstantlyLocks());
        }
        if (this.mShowPassword != null) {
            this.mShowPassword.setChecked(Settings.System.getInt(getContentResolver(), "show_password", 1) != 0);
        }
        if (this.mResetCredentials != null) {
            this.mResetCredentials.setEnabled(this.mKeyStore.isEmpty() ? false : true);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if ("unlock_set_or_change".equals(key)) {
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment", R.string.lock_settings_picker_title, 123, null);
        } else if ("biometric_weak_improve_matching".equals(key)) {
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
            if (!helper.launchConfirmationActivity(124, null, null)) {
                startBiometricWeakImprove();
            }
        } else if ("trust_agent".equals(key)) {
            ChooseLockSettingsHelper helper2 = new ChooseLockSettingsHelper(getActivity(), this);
            this.mTrustAgentClickIntent = preference.getIntent();
            if (!helper2.launchConfirmationActivity(126, null, null) && this.mTrustAgentClickIntent != null) {
                startActivity(this.mTrustAgentClickIntent);
                this.mTrustAgentClickIntent = null;
            }
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 124 && resultCode == -1) {
            startBiometricWeakImprove();
            return;
        }
        if (requestCode == 125 && resultCode == -1) {
            LockPatternUtils lockPatternUtils = this.mChooseLockSettingsHelper.utils();
            lockPatternUtils.setBiometricWeakLivelinessEnabled(false);
        } else {
            if (requestCode == 126 && resultCode == -1) {
                if (this.mTrustAgentClickIntent != null) {
                    startActivity(this.mTrustAgentClickIntent);
                    this.mTrustAgentClickIntent = null;
                    return;
                }
                return;
            }
            createPreferenceHierarchy();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String key = preference.getKey();
        LockPatternUtils lockPatternUtils = this.mChooseLockSettingsHelper.utils();
        if ("lock_after_timeout".equals(key)) {
            int timeout = Integer.parseInt((String) value);
            try {
                Settings.Secure.putInt(getContentResolver(), "lock_screen_lock_after_timeout", timeout);
            } catch (NumberFormatException e) {
                Log.e("SecuritySettings", "could not persist lockAfter timeout setting", e);
            }
            updateLockAfterPreferenceSummary();
            return true;
        }
        if ("lockenabled".equals(key)) {
            lockPatternUtils.setLockPatternEnabled(((Boolean) value).booleanValue());
            return true;
        }
        if ("visiblepattern".equals(key)) {
            lockPatternUtils.setVisiblePatternEnabled(((Boolean) value).booleanValue());
            return true;
        }
        if ("biometric_weak_liveliness".equals(key)) {
            if (((Boolean) value).booleanValue()) {
                lockPatternUtils.setBiometricWeakLivelinessEnabled(true);
                return true;
            }
            this.mBiometricWeakLiveliness.setChecked(true);
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
            if (helper.launchConfirmationActivity(125, null, null)) {
                return true;
            }
            lockPatternUtils.setBiometricWeakLivelinessEnabled(false);
            this.mBiometricWeakLiveliness.setChecked(false);
            return true;
        }
        if ("power_button_instantly_locks".equals(key)) {
            this.mLockPatternUtils.setPowerButtonInstantlyLocks(((Boolean) value).booleanValue());
            return true;
        }
        if ("show_password".equals(key)) {
            Settings.System.putInt(getContentResolver(), "show_password", ((Boolean) value).booleanValue() ? 1 : 0);
            return true;
        }
        if (!"toggle_install_applications".equals(key)) {
            return true;
        }
        if (((Boolean) value).booleanValue()) {
            this.mToggleAppInstallation.setChecked(false);
            warnAppInstallation();
            return false;
        }
        setNonMarketAppsAllowed(false);
        return true;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_security;
    }

    public void startBiometricWeakImprove() {
        Intent intent = new Intent();
        intent.setClassName("com.android.facelock", "com.android.facelock.AddToSetup");
        startActivity(intent);
    }

    private static class SecuritySearchIndexProvider extends BaseSearchIndexProvider {
        boolean mIsPrimary;

        public SecuritySearchIndexProvider() {
            this.mIsPrimary = UserHandle.myUserId() == 0;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            List<SearchIndexableResource> result = new ArrayList<>();
            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            int resId = SecuritySettings.getResIdForLockUnlockScreen(context, lockPatternUtils);
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = resId;
            result.add(sir);
            if (this.mIsPrimary) {
                DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
                switch (dpm.getStorageEncryptionStatus()) {
                    case 1:
                        resId = R.xml.security_settings_unencrypted;
                        break;
                    case 3:
                        resId = R.xml.security_settings_encrypted;
                        break;
                }
                SearchIndexableResource sir2 = new SearchIndexableResource(context);
                sir2.xmlResId = resId;
                result.add(sir2);
            }
            SearchIndexableResource sir3 = new SearchIndexableResource(context);
            sir3.xmlResId = R.xml.security_settings_misc;
            result.add(sir3);
            return result;
        }

        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList<>();
            Resources res = context.getResources();
            String screenTitle = res.getString(R.string.security_settings_title);
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = screenTitle;
            data.screenTitle = screenTitle;
            result.add(data);
            if (!this.mIsPrimary) {
                int resId = UserManager.get(context).isLinkedUser() ? R.string.profile_info_settings_title : R.string.user_info_settings_title;
                SearchIndexableRaw data2 = new SearchIndexableRaw(context);
                data2.title = res.getString(resId);
                data2.screenTitle = screenTitle;
                result.add(data2);
            }
            UserManager um = (UserManager) context.getSystemService("user");
            if (!um.hasUserRestriction("no_config_credentials")) {
                KeyStore keyStore = KeyStore.getInstance();
                int storageSummaryRes = keyStore.isHardwareBacked() ? R.string.credential_storage_type_hardware : R.string.credential_storage_type_software;
                SearchIndexableRaw data3 = new SearchIndexableRaw(context);
                data3.title = res.getString(storageSummaryRes);
                data3.screenTitle = screenTitle;
                result.add(data3);
            }
            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            if (lockPatternUtils.isSecure()) {
                ArrayList<TrustAgentUtils.TrustAgentComponentInfo> agents = SecuritySettings.getActiveTrustAgents(context.getPackageManager(), lockPatternUtils);
                for (int i = 0; i < agents.size(); i++) {
                    TrustAgentUtils.TrustAgentComponentInfo agent = agents.get(i);
                    SearchIndexableRaw data4 = new SearchIndexableRaw(context);
                    data4.title = agent.title;
                    data4.screenTitle = screenTitle;
                    result.add(data4);
                }
            }
            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> keys = new ArrayList<>();
            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            int resId = SecuritySettings.getResIdForLockUnlockScreen(context, lockPatternUtils);
            if (resId == R.xml.security_settings_biometric_weak && lockPatternUtils.getKeyguardStoredPasswordQuality() != 65536) {
                keys.add("visiblepattern");
            }
            TelephonyManager tm = TelephonyManager.getDefault();
            if (!this.mIsPrimary || !tm.hasIccCard()) {
                keys.add("sim_lock");
            }
            UserManager um = (UserManager) context.getSystemService("user");
            if (um.hasUserRestriction("no_config_credentials")) {
                keys.add("credentials_management");
            }
            if (!lockPatternUtils.isSecure()) {
                keys.add("trust_agent");
                keys.add("manage_trust_agents");
            }
            return keys;
        }
    }
}
