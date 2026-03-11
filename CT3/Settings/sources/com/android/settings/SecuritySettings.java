package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableData;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.security.KeyStore;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.RecyclerView;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.GearPreference;
import com.android.settings.TrustAgentUtils;
import com.android.settings.fingerprint.FingerprintSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.RestrictedSwitchPreference;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.IDataProtectionExt;
import com.mediatek.settings.ext.IMdmPermissionControlExt;
import com.mediatek.settings.ext.IPermissionControlExt;
import com.mediatek.settings.ext.IPplSettingsEntryExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import java.util.ArrayList;
import java.util.List;

public class SecuritySettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, DialogInterface.OnClickListener, Indexable, GearPreference.OnGearClickListener {
    private static IPermissionControlExt mPermCtrlExt;
    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private String mCurrentDevicePassword;
    private String mCurrentProfilePassword;
    private DevicePolicyManager mDPM;
    private IDataProtectionExt mDataProectExt;
    private ISettingsMiscExt mExt;
    private boolean mIsAdmin;
    private KeyStore mKeyStore;
    private LockPatternUtils mLockPatternUtils;
    private ManagedLockPasswordProvider mManagedPasswordProvider;
    private IMdmPermissionControlExt mMdmPermCtrlExt;
    private IPplSettingsEntryExt mPplExt;
    private int mProfileChallengeUserId;
    private RestrictedPreference mResetCredentials;
    private Handler mScrollHandler = new Handler();
    private Runnable mScrollRunner = new Runnable() {
        @Override
        public void run() {
            RecyclerView listView = SecuritySettings.this.getListView();
            listView.smoothScrollToPosition(SecuritySettings.this.mUnknownSourcesPosition - 1);
        }
    };
    private boolean mScrollToUnknownSources;
    private SwitchPreference mShowPassword;
    private SubscriptionManager mSubscriptionManager;
    private RestrictedSwitchPreference mToggleAppInstallation;
    private Intent mTrustAgentClickIntent;
    private UserManager mUm;
    private SwitchPreference mUnifyProfile;
    private int mUnknownSourcesPosition;
    private SwitchPreference mVisiblePatternProfile;
    private DialogInterface mWarnInstallApps;
    private static final Intent TRUST_AGENT_INTENT = new Intent("android.service.trust.TrustAgentService");
    private static final String[] SWITCH_PREFERENCE_KEYS = {"show_password", "toggle_install_applications", "unification", "visiblepattern_profile"};
    private static final int MY_USER_ID = UserHandle.myUserId();
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new SecuritySearchIndexProvider(null);

    @Override
    protected int getMetricsCategory() {
        return 87;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mSubscriptionManager = SubscriptionManager.from(getActivity());
        this.mLockPatternUtils = new LockPatternUtils(getActivity());
        this.mManagedPasswordProvider = ManagedLockPasswordProvider.get(getActivity(), MY_USER_ID);
        this.mDPM = (DevicePolicyManager) getSystemService("device_policy");
        this.mUm = UserManager.get(getActivity());
        this.mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        if (savedInstanceState != null && savedInstanceState.containsKey("trust_agent_click_intent")) {
            this.mTrustAgentClickIntent = (Intent) savedInstanceState.getParcelable("trust_agent_click_intent");
        }
        setWhetherNeedScroll();
        initPlugin();
    }

    public static int getResIdForLockUnlockScreen(Context context, LockPatternUtils lockPatternUtils, ManagedLockPasswordProvider managedPasswordProvider, int userId) {
        boolean isMyUser = userId == MY_USER_ID;
        if (!lockPatternUtils.isSecure(userId)) {
            if (!isMyUser) {
                return R.xml.security_settings_lockscreen_profile;
            }
            if (lockPatternUtils.isLockScreenDisabled(userId)) {
                return R.xml.security_settings_lockscreen;
            }
            return R.xml.security_settings_chooser;
        }
        switch (lockPatternUtils.getKeyguardStoredPasswordQuality(userId)) {
            case 65536:
                return isMyUser ? R.xml.security_settings_pattern : R.xml.security_settings_pattern_profile;
            case 131072:
            case 196608:
                return isMyUser ? R.xml.security_settings_pin : R.xml.security_settings_pin_profile;
            case 262144:
            case 327680:
            case 393216:
                return isMyUser ? R.xml.security_settings_password : R.xml.security_settings_password_profile;
            case 524288:
                int resid = managedPasswordProvider.getResIdForLockUnlockScreen(!isMyUser);
                return resid;
            default:
                return 0;
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        Preference manageAgents;
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.security_settings);
        PreferenceScreen root2 = getPreferenceScreen();
        int resid = getResIdForLockUnlockScreen(getActivity(), this.mLockPatternUtils, this.mManagedPasswordProvider, MY_USER_ID);
        addPreferencesFromResource(resid);
        disableIfPasswordQualityManaged("unlock_set_or_change", MY_USER_ID);
        this.mProfileChallengeUserId = Utils.getManagedProfileId(this.mUm, MY_USER_ID);
        if (this.mProfileChallengeUserId != -10000 && this.mLockPatternUtils.isSeparateProfileChallengeAllowed(this.mProfileChallengeUserId)) {
            addPreferencesFromResource(R.xml.security_settings_profile);
            addPreferencesFromResource(R.xml.security_settings_unification);
            int profileResid = getResIdForLockUnlockScreen(getActivity(), this.mLockPatternUtils, this.mManagedPasswordProvider, this.mProfileChallengeUserId);
            addPreferencesFromResource(profileResid);
            maybeAddFingerprintPreference(root2, this.mProfileChallengeUserId);
            if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(this.mProfileChallengeUserId)) {
                Preference lockPreference = root2.findPreference("unlock_set_or_change_profile");
                String summary = getContext().getString(R.string.lock_settings_profile_unified_summary);
                lockPreference.setSummary(summary);
                lockPreference.setEnabled(false);
                disableIfPasswordQualityManaged("unlock_set_or_change", this.mProfileChallengeUserId);
            } else {
                disableIfPasswordQualityManaged("unlock_set_or_change_profile", this.mProfileChallengeUserId);
            }
        }
        Preference unlockSetOrChange = findPreference("unlock_set_or_change");
        if (unlockSetOrChange instanceof GearPreference) {
            ((GearPreference) unlockSetOrChange).setOnGearClickListener(this);
        }
        this.mIsAdmin = this.mUm.isAdminUser();
        boolean isEMMC = FeatureOption.MTK_EMMC_SUPPORT && !FeatureOption.MTK_CACHE_MERGE_SUPPORT;
        boolean isFTL = FeatureOption.MTK_NAND_FTL_SUPPORT;
        boolean isUFS = FeatureOption.MTK_UFS_BOOTING;
        boolean isMNTL = FeatureOption.MTK_MNTL_SUPPORT;
        if ((isFTL || isEMMC || isUFS || isMNTL) && this.mIsAdmin) {
            if (LockPatternUtils.isDeviceEncryptionEnabled()) {
                addPreferencesFromResource(R.xml.security_settings_encrypted);
            } else {
                addPreferencesFromResource(R.xml.security_settings_unencrypted);
            }
        }
        PreferenceGroup securityCategory = (PreferenceGroup) root2.findPreference("security_category");
        if (securityCategory != null) {
            maybeAddFingerprintPreference(securityCategory, UserHandle.myUserId());
            addTrustAgentSettings(securityCategory);
        }
        this.mVisiblePatternProfile = (SwitchPreference) root2.findPreference("visiblepattern_profile");
        this.mUnifyProfile = (SwitchPreference) root2.findPreference("unification");
        addPreferencesFromResource(R.xml.security_settings_misc);
        changeSimTitle();
        TelephonyManager.getDefault();
        CarrierConfigManager cfgMgr = (CarrierConfigManager) getActivity().getSystemService("carrier_config");
        PersistableBundle b = cfgMgr.getConfig();
        if (!this.mIsAdmin || !isSimIccReady() || b.getBoolean("hide_sim_lock_settings_bool")) {
            root2.removePreference(root2.findPreference("sim_lock"));
        } else {
            root2.findPreference("sim_lock").setEnabled(isSimReady());
        }
        if (Settings.System.getInt(getContentResolver(), "lock_to_app_enabled", 0) != 0) {
            root2.findPreference("screen_pinning_settings").setSummary(getResources().getString(R.string.switch_on_text));
        }
        this.mShowPassword = (SwitchPreference) root2.findPreference("show_password");
        this.mResetCredentials = (RestrictedPreference) root2.findPreference("credentials_reset");
        UserManager um = (UserManager) getActivity().getSystemService("user");
        this.mKeyStore = KeyStore.getInstance();
        if (!RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_config_credentials", MY_USER_ID)) {
            RestrictedPreference userCredentials = (RestrictedPreference) root2.findPreference("user_credentials");
            userCredentials.checkRestrictionAndSetDisabled("no_config_credentials");
            RestrictedPreference credentialStorageType = (RestrictedPreference) root2.findPreference("credential_storage_type");
            credentialStorageType.checkRestrictionAndSetDisabled("no_config_credentials");
            RestrictedPreference installCredentials = (RestrictedPreference) root2.findPreference("credentials_install");
            installCredentials.checkRestrictionAndSetDisabled("no_config_credentials");
            this.mResetCredentials.checkRestrictionAndSetDisabled("no_config_credentials");
            int storageSummaryRes = this.mKeyStore.isHardwareBacked() ? R.string.credential_storage_type_hardware : R.string.credential_storage_type_software;
            credentialStorageType.setSummary(storageSummaryRes);
        } else {
            PreferenceGroup credentialsManager = (PreferenceGroup) root2.findPreference("credentials_management");
            credentialsManager.removePreference(root2.findPreference("credentials_reset"));
            credentialsManager.removePreference(root2.findPreference("credentials_install"));
            credentialsManager.removePreference(root2.findPreference("credential_storage_type"));
            credentialsManager.removePreference(root2.findPreference("user_credentials"));
        }
        PreferenceGroup deviceAdminCategory = (PreferenceGroup) root2.findPreference("device_admin_category");
        this.mToggleAppInstallation = (RestrictedSwitchPreference) findPreference("toggle_install_applications");
        this.mToggleAppInstallation.setChecked(isNonMarketAppsAllowed());
        this.mToggleAppInstallation.setEnabled(!um.getUserInfo(MY_USER_ID).isRestricted());
        if (RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_install_unknown_sources", MY_USER_ID) || RestrictedLockUtils.hasBaseUserRestriction(getActivity(), "no_install_apps", MY_USER_ID)) {
            this.mToggleAppInstallation.setEnabled(false);
        }
        if (this.mToggleAppInstallation.isEnabled()) {
            this.mToggleAppInstallation.checkRestrictionAndSetDisabled("no_install_unknown_sources");
            if (!this.mToggleAppInstallation.isDisabledByAdmin()) {
                this.mToggleAppInstallation.checkRestrictionAndSetDisabled("no_install_apps");
            }
        }
        PreferenceGroup advancedCategory = (PreferenceGroup) root2.findPreference("advanced_security");
        if (advancedCategory != null && (manageAgents = advancedCategory.findPreference("manage_trust_agents")) != null && !this.mLockPatternUtils.isSecure(MY_USER_ID)) {
            manageAgents.setEnabled(false);
            manageAgents.setSummary(R.string.disabled_because_no_backup_security);
        }
        Index.getInstance(getActivity()).updateFromClassNameResource(SecuritySettings.class.getName(), true, true);
        for (int i = 0; i < SWITCH_PREFERENCE_KEYS.length; i++) {
            Preference pref = findPreference(SWITCH_PREFERENCE_KEYS[i]);
            if (pref != null) {
                pref.setOnPreferenceChangeListener(this);
            }
        }
        addPluginEntrance(deviceAdminCategory);
        return root2;
    }

    private void disableIfPasswordQualityManaged(String preferenceKey, int userId) {
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfPasswordQualityIsSet(getActivity(), userId);
        if (admin == null || this.mDPM.getPasswordQuality(admin.component, userId) != 524288) {
            return;
        }
        RestrictedPreference pref = (RestrictedPreference) getPreferenceScreen().findPreference(preferenceKey);
        pref.setDisabledByAdmin(admin);
    }

    private void maybeAddFingerprintPreference(PreferenceGroup securityCategory, int userId) {
        Preference fingerprintPreference = FingerprintSettings.getFingerprintPreferenceForUser(securityCategory.getContext(), userId);
        if (fingerprintPreference == null) {
            return;
        }
        securityCategory.addPreference(fingerprintPreference);
    }

    private void addTrustAgentSettings(PreferenceGroup securityCategory) {
        boolean hasSecurity = this.mLockPatternUtils.isSecure(MY_USER_ID);
        ArrayList<TrustAgentUtils.TrustAgentComponentInfo> agents = getActiveTrustAgents(getActivity(), this.mLockPatternUtils, this.mDPM);
        for (int i = 0; i < agents.size(); i++) {
            TrustAgentUtils.TrustAgentComponentInfo agent = agents.get(i);
            RestrictedPreference trustAgentPreference = new RestrictedPreference(securityCategory.getContext());
            trustAgentPreference.setKey("trust_agent");
            trustAgentPreference.setTitle(agent.title);
            trustAgentPreference.setSummary(agent.summary);
            Intent intent = new Intent();
            intent.setComponent(agent.componentName);
            intent.setAction("android.intent.action.MAIN");
            trustAgentPreference.setIntent(intent);
            securityCategory.addPreference(trustAgentPreference);
            trustAgentPreference.setDisabledByAdmin(agent.admin);
            if (!trustAgentPreference.isDisabledByAdmin() && !hasSecurity) {
                trustAgentPreference.setEnabled(false);
                trustAgentPreference.setSummary(R.string.disabled_because_no_backup_security);
            }
        }
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
            return false;
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

    public static ArrayList<TrustAgentUtils.TrustAgentComponentInfo> getActiveTrustAgents(Context context, LockPatternUtils utils, DevicePolicyManager dpm) {
        PackageManager pm = context.getPackageManager();
        ArrayList<TrustAgentUtils.TrustAgentComponentInfo> result = new ArrayList<>();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(TRUST_AGENT_INTENT, 128);
        List<ComponentName> enabledTrustAgents = utils.getEnabledTrustAgents(MY_USER_ID);
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(context, 16, UserHandle.myUserId());
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
                        if (admin != null && dpm.getTrustAgentConfiguration(null, TrustAgentUtils.getComponentName(resolveInfo)) == null) {
                            trustAgentComponentInfo.admin = admin;
                        }
                        result.add(trustAgentComponentInfo);
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
        if (um.hasUserRestriction("no_install_unknown_sources")) {
            return;
        }
        Settings.Global.putInt(getContentResolver(), "install_non_market_apps", enabled ? 1 : 0);
    }

    private void warnAppInstallation() {
        this.mWarnInstallApps = new AlertDialog.Builder(getActivity()).setTitle(getResources().getString(R.string.error_title)).setIcon(android.R.drawable.ic_dialog_alert).setMessage(getResources().getString(R.string.install_all_warning)).setPositiveButton(android.R.string.yes, this).setNegativeButton(android.R.string.no, this).show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog != this.mWarnInstallApps) {
            return;
        }
        boolean turnOn = which == -1;
        setNonMarketAppsAllowed(turnOn);
        if (this.mToggleAppInstallation == null) {
            return;
        }
        this.mToggleAppInstallation.setChecked(turnOn);
    }

    @Override
    public void onGearClick(GearPreference p) {
        if (!"unlock_set_or_change".equals(p.getKey())) {
            return;
        }
        startFragment(this, SecuritySubSettings.class.getName(), 0, 0, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mWarnInstallApps == null) {
            return;
        }
        this.mWarnInstallApps.dismiss();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mTrustAgentClickIntent == null) {
            return;
        }
        outState.putParcelable("trust_agent_click_intent", this.mTrustAgentClickIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();
        if (this.mVisiblePatternProfile != null) {
            this.mVisiblePatternProfile.setChecked(this.mLockPatternUtils.isVisiblePatternEnabled(this.mProfileChallengeUserId));
        }
        updateUnificationPreference();
        if (this.mShowPassword != null) {
            this.mShowPassword.setChecked(Settings.System.getInt(getContentResolver(), "show_password", 1) != 0);
        }
        if (this.mResetCredentials != null && !this.mResetCredentials.isDisabledByAdmin()) {
            this.mResetCredentials.setEnabled(this.mKeyStore.isEmpty() ? false : true);
        }
        ScrollToUnknownSources();
        mPermCtrlExt.enablerResume();
        this.mPplExt.enablerResume();
    }

    public void updateUnificationPreference() {
        if (this.mUnifyProfile == null) {
            return;
        }
        this.mUnifyProfile.setChecked(!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(this.mProfileChallengeUserId));
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        if ("unlock_set_or_change".equals(key)) {
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment", R.string.lock_settings_picker_title, 123, null);
            return true;
        }
        if ("unlock_set_or_change_profile".equals(key)) {
            if (Utils.startQuietModeDialogIfNecessary(getActivity(), this.mUm, this.mProfileChallengeUserId)) {
                return false;
            }
            Bundle extras = new Bundle();
            extras.putInt("android.intent.extra.USER_ID", this.mProfileChallengeUserId);
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment", R.string.lock_settings_picker_title_profile, 127, extras);
            return true;
        }
        if ("trust_agent".equals(key)) {
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
            this.mTrustAgentClickIntent = preference.getIntent();
            boolean confirmationLaunched = helper.launchConfirmationActivity(126, preference.getTitle());
            if (!confirmationLaunched && this.mTrustAgentClickIntent != null) {
                startActivity(this.mTrustAgentClickIntent);
                this.mTrustAgentClickIntent = null;
                return true;
            }
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 126 && resultCode == -1) {
            if (this.mTrustAgentClickIntent != null) {
                startActivity(this.mTrustAgentClickIntent);
                this.mTrustAgentClickIntent = null;
                return;
            }
            return;
        }
        if (requestCode == 128 && resultCode == -1) {
            this.mCurrentDevicePassword = data.getStringExtra("password");
            launchConfirmProfileLockForUnification();
        } else if (requestCode == 129 && resultCode == -1) {
            this.mCurrentProfilePassword = data.getStringExtra("password");
            unifyLocks();
        } else if (requestCode == 130 && resultCode == -1) {
            ununifyLocks();
        } else {
            createPreferenceHierarchy();
        }
    }

    public void launchConfirmDeviceLockForUnification() {
        String title = getActivity().getString(R.string.unlock_set_unlock_launch_picker_title);
        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
        if (helper.launchConfirmationActivity(128, title, true, MY_USER_ID)) {
            return;
        }
        launchConfirmProfileLockForUnification();
    }

    private void launchConfirmProfileLockForUnification() {
        String title = getActivity().getString(R.string.unlock_set_unlock_launch_picker_title_profile);
        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
        if (helper.launchConfirmationActivity(129, title, true, this.mProfileChallengeUserId)) {
            return;
        }
        unifyLocks();
        createPreferenceHierarchy();
    }

    private void unifyLocks() {
        int profileQuality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mProfileChallengeUserId);
        if (profileQuality == 65536) {
            this.mLockPatternUtils.saveLockPattern(LockPatternUtils.stringToPattern(this.mCurrentProfilePassword), this.mCurrentDevicePassword, MY_USER_ID);
        } else {
            this.mLockPatternUtils.saveLockPassword(this.mCurrentProfilePassword, this.mCurrentDevicePassword, profileQuality, MY_USER_ID);
        }
        this.mLockPatternUtils.setSeparateProfileChallengeEnabled(this.mProfileChallengeUserId, false, this.mCurrentProfilePassword);
        boolean profilePatternVisibility = this.mLockPatternUtils.isVisiblePatternEnabled(this.mProfileChallengeUserId);
        this.mLockPatternUtils.setVisiblePatternEnabled(profilePatternVisibility, MY_USER_ID);
        this.mCurrentDevicePassword = null;
        this.mCurrentProfilePassword = null;
    }

    public void unifyUncompliantLocks() {
        this.mLockPatternUtils.setSeparateProfileChallengeEnabled(this.mProfileChallengeUserId, false, this.mCurrentProfilePassword);
        startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment", R.string.lock_settings_picker_title, 123, null);
    }

    private void ununifyLocks() {
        Bundle extras = new Bundle();
        extras.putInt("android.intent.extra.USER_ID", this.mProfileChallengeUserId);
        startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment", R.string.lock_settings_picker_title_profile, 127, extras);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        String key = preference.getKey();
        LockPatternUtils lockPatternUtilsUtils = this.mChooseLockSettingsHelper.utils();
        if ("visiblepattern_profile".equals(key)) {
            if (Utils.startQuietModeDialogIfNecessary(getActivity(), this.mUm, this.mProfileChallengeUserId)) {
                return false;
            }
            lockPatternUtilsUtils.setVisiblePatternEnabled(((Boolean) obj).booleanValue(), this.mProfileChallengeUserId);
            return true;
        }
        if ("unification".equals(key)) {
            if (Utils.startQuietModeDialogIfNecessary(getActivity(), this.mUm, this.mProfileChallengeUserId)) {
                return false;
            }
            if (((Boolean) obj).booleanValue()) {
                UnificationConfirmationDialog.newIntance(this.mLockPatternUtils.getKeyguardStoredPasswordQuality(this.mProfileChallengeUserId) >= 65536 ? this.mLockPatternUtils.isSeparateProfileChallengeAllowedToUnify(this.mProfileChallengeUserId) : false).show(getChildFragmentManager(), "unification_dialog");
                return true;
            }
            if (new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(130, getActivity().getString(R.string.unlock_set_unlock_launch_picker_title), true, MY_USER_ID)) {
                return true;
            }
            ununifyLocks();
            return true;
        }
        if ("show_password".equals(key)) {
            Settings.System.putInt(getContentResolver(), "show_password", ((Boolean) obj).booleanValue() ? 1 : 0);
            lockPatternUtilsUtils.setVisiblePasswordEnabled(((Boolean) obj).booleanValue(), MY_USER_ID);
            return true;
        }
        if (!"toggle_install_applications".equals(key)) {
            return true;
        }
        if (((Boolean) obj).booleanValue()) {
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

    private static class SecuritySearchIndexProvider extends BaseSearchIndexProvider {
        SecuritySearchIndexProvider(SecuritySearchIndexProvider securitySearchIndexProvider) {
            this();
        }

        private SecuritySearchIndexProvider() {
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            List<SearchIndexableResource> index = new ArrayList<>();
            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            ManagedLockPasswordProvider managedPasswordProvider = ManagedLockPasswordProvider.get(context, SecuritySettings.MY_USER_ID);
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService("device_policy");
            UserManager um = UserManager.get(context);
            int profileUserId = Utils.getManagedProfileId(um, SecuritySettings.MY_USER_ID);
            if (!isPasswordManaged(SecuritySettings.MY_USER_ID, context, dpm) && (profileUserId == -10000 || lockPatternUtils.isSeparateProfileChallengeAllowed(profileUserId) || !isPasswordManaged(profileUserId, context, dpm))) {
                int resId = SecuritySettings.getResIdForLockUnlockScreen(context, lockPatternUtils, managedPasswordProvider, SecuritySettings.MY_USER_ID);
                index.add(getSearchResource(context, resId));
            }
            if (profileUserId != -10000 && lockPatternUtils.isSeparateProfileChallengeAllowed(profileUserId) && !isPasswordManaged(profileUserId, context, dpm)) {
                index.add(getSearchResource(context, SecuritySettings.getResIdForLockUnlockScreen(context, lockPatternUtils, managedPasswordProvider, profileUserId)));
            }
            if (um.isAdminUser()) {
                switch (dpm.getStorageEncryptionStatus()) {
                    case DefaultWfcSettingsExt.PAUSE:
                        index.add(getSearchResource(context, R.xml.security_settings_unencrypted));
                        break;
                    case DefaultWfcSettingsExt.DESTROY:
                        index.add(getSearchResource(context, R.xml.security_settings_encrypted));
                        break;
                }
            }
            SearchIndexableResource sir = getSearchResource(context, SecuritySubSettings.getResIdForLockUnlockSubScreen(context, lockPatternUtils, managedPasswordProvider));
            sir.className = SecuritySubSettings.class.getName();
            index.add(sir);
            index.add(getSearchResource(context, R.xml.security_settings_misc));
            return index;
        }

        private SearchIndexableResource getSearchResource(Context context, int xmlResId) {
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = xmlResId;
            return sir;
        }

        private boolean isPasswordManaged(int userId, Context context, DevicePolicyManager dpm) {
            RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfPasswordQualityIsSet(context, userId);
            return admin != null && dpm.getPasswordQuality(admin.component, userId) == 524288;
        }

        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            int storageSummaryRes;
            List<SearchIndexableRaw> result = new ArrayList<>();
            Resources res = context.getResources();
            String screenTitle = res.getString(R.string.security_settings_title);
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = screenTitle;
            data.screenTitle = screenTitle;
            result.add(data);
            UserManager um = UserManager.get(context);
            if (!um.isAdminUser()) {
                int resId = um.isLinkedUser() ? R.string.profile_info_settings_title : R.string.user_info_settings_title;
                SearchIndexableRaw data2 = new SearchIndexableRaw(context);
                data2.title = res.getString(resId);
                data2.screenTitle = screenTitle;
                result.add(data2);
            }
            FingerprintManager fpm = (FingerprintManager) context.getSystemService("fingerprint");
            if (fpm != null && fpm.isHardwareDetected()) {
                SearchIndexableRaw data3 = new SearchIndexableRaw(context);
                data3.title = res.getString(R.string.security_settings_fingerprint_preference_title);
                data3.screenTitle = screenTitle;
                result.add(data3);
                SearchIndexableRaw data4 = new SearchIndexableRaw(context);
                data4.title = res.getString(R.string.fingerprint_manage_category_title);
                data4.screenTitle = screenTitle;
                result.add(data4);
            }
            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            int profileUserId = Utils.getManagedProfileId(um, SecuritySettings.MY_USER_ID);
            if (profileUserId != -10000 && lockPatternUtils.isSeparateProfileChallengeAllowed(profileUserId) && lockPatternUtils.getKeyguardStoredPasswordQuality(profileUserId) >= 65536 && lockPatternUtils.isSeparateProfileChallengeAllowedToUnify(profileUserId)) {
                SearchIndexableRaw data5 = new SearchIndexableRaw(context);
                data5.title = res.getString(R.string.lock_settings_profile_unification_title);
                data5.screenTitle = screenTitle;
                result.add(data5);
            }
            if (!um.hasUserRestriction("no_config_credentials")) {
                KeyStore keyStore = KeyStore.getInstance();
                if (keyStore.isHardwareBacked()) {
                    storageSummaryRes = R.string.credential_storage_type_hardware;
                } else {
                    storageSummaryRes = R.string.credential_storage_type_software;
                }
                SearchIndexableRaw data6 = new SearchIndexableRaw(context);
                data6.title = res.getString(storageSummaryRes);
                data6.screenTitle = screenTitle;
                result.add(data6);
            }
            if (lockPatternUtils.isSecure(SecuritySettings.MY_USER_ID)) {
                ArrayList<TrustAgentUtils.TrustAgentComponentInfo> agents = SecuritySettings.getActiveTrustAgents(context, lockPatternUtils, (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class));
                for (int i = 0; i < agents.size(); i++) {
                    TrustAgentUtils.TrustAgentComponentInfo agent = agents.get(i);
                    SearchIndexableRaw data7 = new SearchIndexableRaw(context);
                    data7.title = agent.title;
                    data7.screenTitle = screenTitle;
                    result.add(data7);
                }
            }
            if (SecuritySettings.mPermCtrlExt == null) {
                Log.d("SecuritySettings", "mPermCtrlExt init firstly");
                IPermissionControlExt unused = SecuritySettings.mPermCtrlExt = UtilsExt.getPermControlExtPlugin(context);
            }
            List<SearchIndexableData> permList = SecuritySettings.mPermCtrlExt.getRawDataToIndex(enabled);
            Log.d("SecuritySettings", "permList = " + permList);
            if (permList != null) {
                for (SearchIndexableData permdata : permList) {
                    SearchIndexableRaw indexablePerm = new SearchIndexableRaw(context);
                    String orign = permdata.toString();
                    String title = orign.substring(orign.indexOf("title:") + "title:".length());
                    indexablePerm.title = title;
                    indexablePerm.intentAction = permdata.intentAction;
                    Log.d("SecuritySettings", "title: " + indexablePerm.title + " intentAction: " + indexablePerm.intentAction);
                    result.add(indexablePerm);
                }
            }
            return result;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> keys = new ArrayList<>();
            LockPatternUtils lockPatternUtils = new LockPatternUtils(context);
            UserManager um = UserManager.get(context);
            TelephonyManager tm = TelephonyManager.from(context);
            if (!um.isAdminUser() || !tm.hasIccCard()) {
                keys.add("sim_lock");
            }
            if (um.hasUserRestriction("no_config_credentials")) {
                keys.add("credentials_management");
            }
            if (!lockPatternUtils.isSecure(SecuritySettings.MY_USER_ID)) {
                keys.add("trust_agent");
                keys.add("manage_trust_agents");
            }
            return keys;
        }
    }

    public static class SecuritySubSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
        private static final String[] SWITCH_PREFERENCE_KEYS = {"lock_after_timeout", "visiblepattern", "power_button_instantly_locks"};
        private DevicePolicyManager mDPM;
        private TimeoutListPreference mLockAfter;
        private LockPatternUtils mLockPatternUtils;
        private RestrictedPreference mOwnerInfoPref;
        private SwitchPreference mPowerButtonInstantlyLocks;
        private SwitchPreference mVisiblePattern;

        @Override
        protected int getMetricsCategory() {
            return 87;
        }

        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            this.mLockPatternUtils = new LockPatternUtils(getContext());
            this.mDPM = (DevicePolicyManager) getContext().getSystemService(DevicePolicyManager.class);
            createPreferenceHierarchy();
        }

        @Override
        public void onResume() {
            super.onResume();
            createPreferenceHierarchy();
            if (this.mVisiblePattern != null) {
                this.mVisiblePattern.setChecked(this.mLockPatternUtils.isVisiblePatternEnabled(SecuritySettings.MY_USER_ID));
            }
            if (this.mPowerButtonInstantlyLocks != null) {
                this.mPowerButtonInstantlyLocks.setChecked(this.mLockPatternUtils.getPowerButtonInstantlyLocks(SecuritySettings.MY_USER_ID));
            }
            updateOwnerInfo();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            createPreferenceHierarchy();
        }

        private void createPreferenceHierarchy() {
            PreferenceScreen root = getPreferenceScreen();
            if (root != null) {
                root.removeAll();
            }
            int resid = getResIdForLockUnlockSubScreen(getActivity(), new LockPatternUtils(getContext()), ManagedLockPasswordProvider.get(getContext(), SecuritySettings.MY_USER_ID));
            addPreferencesFromResource(resid);
            this.mLockAfter = (TimeoutListPreference) findPreference("lock_after_timeout");
            if (this.mLockAfter != null) {
                setupLockAfterPreference();
                updateLockAfterPreferenceSummary();
            }
            this.mVisiblePattern = (SwitchPreference) findPreference("visiblepattern");
            this.mPowerButtonInstantlyLocks = (SwitchPreference) findPreference("power_button_instantly_locks");
            Preference trustAgentPreference = findPreference("trust_agent");
            if (this.mPowerButtonInstantlyLocks != null && trustAgentPreference != null && trustAgentPreference.getTitle().length() > 0) {
                this.mPowerButtonInstantlyLocks.setSummary(getString(R.string.lockpattern_settings_power_button_instantly_locks_summary, new Object[]{trustAgentPreference.getTitle()}));
            }
            this.mOwnerInfoPref = (RestrictedPreference) findPreference("owner_info_settings");
            if (this.mOwnerInfoPref != null) {
                if (this.mLockPatternUtils.isDeviceOwnerInfoEnabled()) {
                    RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.getDeviceOwner(getActivity());
                    this.mOwnerInfoPref.setDisabledByAdmin(admin);
                } else {
                    this.mOwnerInfoPref.setDisabledByAdmin(null);
                    this.mOwnerInfoPref.setEnabled(!this.mLockPatternUtils.isLockScreenDisabled(SecuritySettings.MY_USER_ID));
                    if (this.mOwnerInfoPref.isEnabled()) {
                        this.mOwnerInfoPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                OwnerInfoSettings.show(SecuritySubSettings.this);
                                return true;
                            }
                        });
                    }
                }
            }
            for (int i = 0; i < SWITCH_PREFERENCE_KEYS.length; i++) {
                Preference pref = findPreference(SWITCH_PREFERENCE_KEYS[i]);
                if (pref != null) {
                    pref.setOnPreferenceChangeListener(this);
                }
            }
        }

        private void setupLockAfterPreference() {
            long currentTimeout = Settings.Secure.getLong(getContentResolver(), "lock_screen_lock_after_timeout", 5000L);
            this.mLockAfter.setValue(String.valueOf(currentTimeout));
            this.mLockAfter.setOnPreferenceChangeListener(this);
            if (this.mDPM == null) {
                return;
            }
            RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfMaximumTimeToLockIsSet(getActivity());
            long adminTimeout = this.mDPM.getMaximumTimeToLockForUserAndProfiles(UserHandle.myUserId());
            long displayTimeout = Math.max(0, Settings.System.getInt(getContentResolver(), "screen_off_timeout", 0));
            long maxTimeout = Math.max(0L, adminTimeout - displayTimeout);
            this.mLockAfter.removeUnusableTimeouts(maxTimeout, admin);
        }

        private void updateLockAfterPreferenceSummary() {
            String summary;
            if (this.mLockAfter.isDisabledByAdmin()) {
                summary = getString(R.string.disabled_by_policy_title);
            } else {
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
                Preference preference = findPreference("trust_agent");
                if (preference == null || preference.getTitle().length() <= 0) {
                    summary = getString(R.string.lock_after_timeout_summary, new Object[]{entries[best]});
                } else if (Long.valueOf(values[best].toString()).longValue() == 0) {
                    summary = getString(R.string.lock_immediately_summary_with_exception, new Object[]{preference.getTitle()});
                } else {
                    summary = getString(R.string.lock_after_timeout_summary_with_exception, new Object[]{entries[best], preference.getTitle()});
                }
            }
            this.mLockAfter.setSummary(summary);
        }

        public void updateOwnerInfo() {
            String string;
            if (this.mOwnerInfoPref == null) {
                return;
            }
            if (this.mLockPatternUtils.isDeviceOwnerInfoEnabled()) {
                this.mOwnerInfoPref.setSummary(this.mLockPatternUtils.getDeviceOwnerInfo());
                return;
            }
            RestrictedPreference restrictedPreference = this.mOwnerInfoPref;
            if (this.mLockPatternUtils.isOwnerInfoEnabled(SecuritySettings.MY_USER_ID)) {
                string = this.mLockPatternUtils.getOwnerInfo(SecuritySettings.MY_USER_ID);
            } else {
                string = getString(R.string.owner_info_settings_summary);
            }
            restrictedPreference.setSummary(string);
        }

        public static int getResIdForLockUnlockSubScreen(Context context, LockPatternUtils lockPatternUtils, ManagedLockPasswordProvider managedPasswordProvider) {
            if (lockPatternUtils.isSecure(SecuritySettings.MY_USER_ID)) {
                switch (lockPatternUtils.getKeyguardStoredPasswordQuality(SecuritySettings.MY_USER_ID)) {
                    case 65536:
                        return R.xml.security_settings_pattern_sub;
                    case 131072:
                    case 196608:
                        return R.xml.security_settings_pin_sub;
                    case 262144:
                    case 327680:
                    case 393216:
                        return R.xml.security_settings_password_sub;
                    case 524288:
                        return managedPasswordProvider.getResIdForLockUnlockSubScreen();
                    default:
                        return 0;
                }
            }
            if (!lockPatternUtils.isLockScreenDisabled(SecuritySettings.MY_USER_ID)) {
                return R.xml.security_settings_slide_sub;
            }
            return 0;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String key = preference.getKey();
            if ("power_button_instantly_locks".equals(key)) {
                this.mLockPatternUtils.setPowerButtonInstantlyLocks(((Boolean) value).booleanValue(), SecuritySettings.MY_USER_ID);
                return true;
            }
            if ("lock_after_timeout".equals(key)) {
                int timeout = Integer.parseInt((String) value);
                try {
                    Settings.Secure.putInt(getContentResolver(), "lock_screen_lock_after_timeout", timeout);
                } catch (NumberFormatException e) {
                    Log.e("SecuritySettings", "could not persist lockAfter timeout setting", e);
                }
                setupLockAfterPreference();
                updateLockAfterPreferenceSummary();
                return true;
            }
            if ("visiblepattern".equals(key)) {
                this.mLockPatternUtils.setVisiblePatternEnabled(((Boolean) value).booleanValue(), SecuritySettings.MY_USER_ID);
                return true;
            }
            return true;
        }
    }

    public static class UnificationConfirmationDialog extends DialogFragment {
        public static UnificationConfirmationDialog newIntance(boolean compliant) {
            UnificationConfirmationDialog dialog = new UnificationConfirmationDialog();
            Bundle args = new Bundle();
            args.putBoolean("compliant", compliant);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public void show(FragmentManager manager, String tag) {
            if (manager.findFragmentByTag(tag) != null) {
                return;
            }
            super.show(manager, tag);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final SecuritySettings parentFragment = (SecuritySettings) getParentFragment();
            final boolean compliant = getArguments().getBoolean("compliant");
            return new AlertDialog.Builder(getActivity()).setTitle(R.string.lock_settings_profile_unification_dialog_title).setMessage(compliant ? R.string.lock_settings_profile_unification_dialog_body : R.string.lock_settings_profile_unification_dialog_uncompliant_body).setPositiveButton(compliant ? R.string.lock_settings_profile_unification_dialog_confirm : R.string.lock_settings_profile_unification_dialog_uncompliant_confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    if (compliant) {
                        parentFragment.launchConfirmDeviceLockForUnification();
                    } else {
                        parentFragment.unifyUncompliantLocks();
                    }
                }
            }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            ((SecuritySettings) getParentFragment()).updateUnificationPreference();
        }
    }

    private void setWhetherNeedScroll() {
        String action = getActivity().getIntent().getAction();
        if (!"android.settings.SECURITY_SETTINGS".equals(action)) {
            return;
        }
        this.mScrollToUnknownSources = true;
    }

    private void ScrollToUnknownSources() {
        if (!this.mScrollToUnknownSources) {
            return;
        }
        this.mScrollToUnknownSources = false;
        this.mUnknownSourcesPosition = 0;
        findPreferencePosition("toggle_install_applications", getPreferenceScreen());
        this.mScrollHandler.postDelayed(this.mScrollRunner, 500L);
    }

    private Preference findPreferencePosition(CharSequence key, PreferenceGroup root) {
        if (key.equals(root.getKey())) {
            return root;
        }
        this.mUnknownSourcesPosition++;
        int preferenceCount = root.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = root.getPreference(i);
            String curKey = preference.getKey();
            if (curKey != null && curKey.equals(key)) {
                return preference;
            }
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup group = (PreferenceGroup) preference;
                Preference returnedPreference = findPreferencePosition(key, group);
                if (returnedPreference != null) {
                    return returnedPreference;
                }
            } else {
                this.mUnknownSourcesPosition++;
            }
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.mScrollHandler.removeCallbacks(this.mScrollRunner);
    }

    private void initPlugin() {
        mPermCtrlExt = UtilsExt.getPermControlExtPlugin(getActivity());
        this.mPplExt = UtilsExt.getPrivacyProtectionLockExtPlugin(getActivity());
        this.mMdmPermCtrlExt = UtilsExt.getMdmPermControlExtPlugin(getActivity());
        this.mDataProectExt = UtilsExt.getDataProectExtPlugin(getActivity());
        this.mExt = UtilsExt.getMiscPlugin(getActivity());
    }

    private void addPluginEntrance(PreferenceGroup deviceAdminCategory) {
        mPermCtrlExt.addAutoBootPrf(deviceAdminCategory);
        mPermCtrlExt.addPermSwitchPrf(deviceAdminCategory);
        this.mDataProectExt.addDataPrf(deviceAdminCategory);
        this.mPplExt.addPplPrf(deviceAdminCategory);
        this.mMdmPermCtrlExt.addMdmPermCtrlPrf(deviceAdminCategory);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPermCtrlExt.enablerPause();
        this.mPplExt.enablerPause();
    }

    private void changeSimTitle() {
        findPreference("sim_lock").setTitle(this.mExt.customizeSimDisplayString(findPreference("sim_lock").getTitle().toString(), -1));
        findPreference("sim_lock_settings").setTitle(this.mExt.customizeSimDisplayString(findPreference("sim_lock_settings").getTitle().toString(), -1));
    }
}
