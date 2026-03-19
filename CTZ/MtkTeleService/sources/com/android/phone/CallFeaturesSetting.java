package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsReasonInfo;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.ims.ImsConnectionStateListener;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.phone.PhoneGlobals;
import com.android.phone.settings.PhoneAccountSettingsFragment;
import com.android.phone.settings.VoicemailSettingsActivity;
import com.android.phone.settings.fdn.FdnSetting;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.CallSettingUtils;
import com.mediatek.settings.TelephonyUtils;
import com.mediatek.settings.cdma.CdmaCLIRUtOptions;
import com.mediatek.settings.cdma.CdmaCallForwardOptions;
import com.mediatek.settings.cdma.CdmaCallWaitOptions;
import com.mediatek.settings.cdma.CdmaCallWaitingUtOptions;
import com.mediatek.settings.cdma.TelephonyUtilsEx;
import com.mediatek.settings.vtss.GsmUmtsVTCBOptions;
import com.mediatek.settings.vtss.GsmUmtsVTCFOptions;
import java.util.List;

public class CallFeaturesSetting extends PreferenceActivity implements Preference.OnPreferenceChangeListener, PhoneGlobals.SubInfoUpdateListener {
    private static final String ADDITIONAL_GSM_SETTINGS_KEY = "additional_gsm_call_settings_key";
    private static final String BUTTON_CDMA_OPTIONS = "button_cdma_more_expand_key";
    private static final String BUTTON_CP_KEY = "button_voice_privacy_key";
    private static final String BUTTON_FDN_KEY = "button_fdn_key";
    private static final String BUTTON_GSM_UMTS_OPTIONS = "button_gsm_more_expand_key";
    private static final String BUTTON_RETRY_KEY = "button_auto_retry_key";
    private static final String CALL_BARRING_KEY = "call_barring_key";
    private static final String CALL_FORWARDING_KEY = "call_forwarding_key";
    private static final boolean DBG = true;
    private static final String ENABLE_VIDEO_CALLING_KEY = "button_enable_video_calling";
    private static final String KEY_CALLER_ID = "button_caller_id";
    private static final String KEY_CALL_FORWARD = "button_cf_expand_key";
    private static final String KEY_CALL_WAIT = "button_cw_key";
    private static final String LOG_TAG = "CallFeaturesSetting";
    private static final String PHONE_ACCOUNT_SETTINGS_KEY = "phone_account_settings_preference_screen";
    private static final String VOICEMAIL_SETTING_SCREEN_PREF_KEY = "button_voicemail_category_key";
    private SwitchPreference mButtonAutoRetry;
    private Preference mButtonWifiCalling;
    private SwitchPreference mEnableVideoCalling;
    private ImsManager mImsMgr;
    private Phone mPhone;
    private SubscriptionInfoHelper mSubscriptionInfoHelper;
    private TelecomManager mTelecomManager;
    private PreferenceScreen mVoicemailSettingsScreen;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int i, String str) {
            CallFeaturesSetting.log("PhoneStateListener onCallStateChanged: state is " + i);
            if (CallFeaturesSetting.this.mEnableVideoCalling != null) {
                TelephonyManager telephonyManager = (TelephonyManager) CallFeaturesSetting.this.getSystemService("phone");
                CallFeaturesSetting.this.mEnableVideoCalling.setEnabled(telephonyManager.getCallState() == 0);
                CallFeaturesSetting.this.mButtonWifiCalling.setEnabled(telephonyManager.getCallState() == 0);
            }
        }
    };
    private int mImsRegState = 3;
    private ImsConnectionStateListener mImsConnectionStateListener = new ImsConnectionStateListener() {
        public void onImsConnected(int i) {
            CallFeaturesSetting.log("onImsConnected imsRadioTech=" + i);
            CallFeaturesSetting.this.mImsRegState = 0;
            CallFeaturesSetting.this.updateScreenStatusOnUI();
        }

        public void onImsDisconnected(ImsReasonInfo imsReasonInfo) {
            CallFeaturesSetting.log("onImsDisconnected imsReasonInfo=" + imsReasonInfo);
            CallFeaturesSetting.this.mImsRegState = 1;
            CallFeaturesSetting.this.updateScreenStatusOnUI();
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            CallFeaturesSetting.log("onReceive, action = " + action);
            if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                CallFeaturesSetting.this.updateScreenStatus();
            }
        }
    };

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (onPreferenceTreeClickMTK(preferenceScreen, preference)) {
            return DBG;
        }
        if (preference == this.mButtonAutoRetry) {
            Settings.Global.putInt(this.mPhone.getContext().getContentResolver(), "call_auto_retry", this.mButtonAutoRetry.isChecked() ? 1 : 0);
            return DBG;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        log("onPreferenceChange: \"" + preference + "\" changed to \"" + obj + "\"");
        if (preference == this.mEnableVideoCalling) {
            if (this.mImsMgr.isEnhanced4gLteModeSettingEnabledByUser()) {
                Boolean bool = (Boolean) obj;
                this.mImsMgr.setVtSetting(bool.booleanValue());
                ExtensionManager.getCallFeaturesSettingExt().videoPreferenceChange(bool.booleanValue());
                return DBG;
            }
            new AlertDialog.Builder(this).setMessage(getResources().getString(R.string.enable_video_calling_dialog_msg)).setNeutralButton(getResources().getString(R.string.enable_video_calling_dialog_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    CallFeaturesSetting.this.startActivity(new Intent(CallFeaturesSetting.this.mPhone.getContext(), (Class<?>) MobileNetworkSettings.class));
                }
            }).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).show();
            return false;
        }
        return DBG;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        log("onCreate: Intent is " + getIntent());
        if (!UserManager.get(this).isAdminUser()) {
            Toast.makeText(this, R.string.call_settings_admin_user_only, 0).show();
            finish();
            return;
        }
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.call_settings_with_label);
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
        this.mTelecomManager = TelecomManager.from(this);
        registerEventCallbacks();
        if (this.mPhone == null) {
            log("onCreate: mPhone is null, finish!!!");
            finish();
        }
    }

    private void updateImsManager(Phone phone) {
        log("updateImsManager :: phone.getContext()=" + phone.getContext() + " phone.getPhoneId()=" + phone.getPhoneId());
        this.mImsMgr = ImsManager.getInstance(phone.getContext(), phone.getPhoneId());
        if (this.mImsMgr == null) {
            log("updateImsManager :: Could not get ImsManager instance!");
            return;
        }
        log("updateImsManager :: mImsMgr=" + this.mImsMgr);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ((TelephonyManager) getSystemService("phone")).listen(this.mPhoneStateListener, 0);
    }

    @Override
    protected void onResume() {
        boolean zIsUtPreferByCdmaSimAndImsOn;
        boolean zIsVtEnabledByUser;
        super.onResume();
        updateImsManager(this.mPhone);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.removeAll();
        }
        addPreferencesFromResource(R.xml.call_feature_setting);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService("phone");
        telephonyManager.listen(this.mPhoneStateListener, 32);
        PreferenceScreen preferenceScreen2 = getPreferenceScreen();
        this.mVoicemailSettingsScreen = (PreferenceScreen) findPreference(VOICEMAIL_SETTING_SCREEN_PREF_KEY);
        int subId = this.mPhone.getSubId();
        Intent intent = new Intent(this, (Class<?>) VoicemailSettingsActivity.class);
        SubscriptionInfoHelper.addExtrasToIntent(intent, MtkSubscriptionManager.getSubInfo((String) null, subId));
        this.mVoicemailSettingsScreen.setIntent(intent);
        maybeHideVoicemailSettings();
        this.mButtonAutoRetry = (SwitchPreference) findPreference(BUTTON_RETRY_KEY);
        this.mEnableVideoCalling = (SwitchPreference) findPreference(ENABLE_VIDEO_CALLING_KEY);
        this.mButtonWifiCalling = findPreference(getResources().getString(R.string.wifi_calling_settings_key));
        PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(this.mPhone.getSubId());
        if (carrierConfigForSubId.getBoolean("auto_retry_enabled_bool")) {
            this.mButtonAutoRetry.setOnPreferenceChangeListener(this);
            this.mButtonAutoRetry.setChecked(Settings.Global.getInt(getContentResolver(), "call_auto_retry", 0) != 0);
        } else {
            preferenceScreen2.removePreference(this.mButtonAutoRetry);
            this.mButtonAutoRetry = null;
        }
        Preference preferenceFindPreference = preferenceScreen2.findPreference(BUTTON_CDMA_OPTIONS);
        Preference preferenceFindPreference2 = preferenceScreen2.findPreference(BUTTON_GSM_UMTS_OPTIONS);
        Preference preferenceFindPreference3 = preferenceScreen2.findPreference(BUTTON_FDN_KEY);
        Intent intent2 = new Intent(this, (Class<?>) FdnSetting.class);
        SubscriptionInfoHelper.addExtrasToIntent(intent2, MtkSubscriptionManager.getSubInfo((String) null, subId));
        preferenceFindPreference3.setIntent(intent2);
        if (carrierConfigForSubId.getBoolean("world_phone_bool")) {
            preferenceFindPreference.setIntent(this.mSubscriptionInfoHelper.getIntent(CdmaCallOptions.class));
            preferenceFindPreference2.setIntent(this.mSubscriptionInfoHelper.getIntent(GsmUmtsCallOptions.class));
        } else {
            preferenceScreen2.removePreference(preferenceFindPreference);
            preferenceScreen2.removePreference(preferenceFindPreference2);
            int phoneType = this.mPhone.getPhoneType();
            if (carrierConfigForSubId.getBoolean("hide_carrier_network_settings_bool")) {
                preferenceScreen2.removePreference(preferenceFindPreference3);
            } else {
                boolean zIsUtPreferOnlyByCdmaSim = CallSettingUtils.isUtPreferOnlyByCdmaSim(subId);
                if (!zIsUtPreferOnlyByCdmaSim) {
                    zIsUtPreferByCdmaSimAndImsOn = CallSettingUtils.isUtPreferByCdmaSimAndImsOn(this, subId, DBG, DBG);
                } else {
                    zIsUtPreferByCdmaSimAndImsOn = false;
                }
                boolean z = zIsUtPreferOnlyByCdmaSim || zIsUtPreferByCdmaSimAndImsOn;
                if (phoneType == 2 || zIsUtPreferOnlyByCdmaSim) {
                    preferenceScreen2.removePreference(preferenceFindPreference3);
                    if (!carrierConfigForSubId.getBoolean("voice_privacy_disable_ui_bool")) {
                        addPreferencesFromResource(R.xml.cdma_call_privacy);
                        configCdmaVoicePrivacy(preferenceScreen2, z);
                    }
                    addPreferencesFromResource(R.xml.mtk_cdma_call_options);
                    configCdmaCallerId(preferenceScreen2, zIsUtPreferByCdmaSimAndImsOn);
                    configCdmaCallWaiting(preferenceScreen2, carrierConfigForSubId);
                } else if (phoneType == 1) {
                    if (carrierConfigForSubId.getBoolean("additional_call_setting_bool")) {
                        addPreferencesFromResource(R.xml.gsm_umts_call_options);
                        GsmUmtsCallOptions.init(preferenceScreen2, this.mSubscriptionInfoHelper);
                    }
                } else {
                    throw new IllegalStateException("Unexpected phone type: " + phoneType);
                }
            }
        }
        boolean zIsVtEnabledByPlatform = this.mImsMgr.isVtEnabledByPlatform();
        boolean zIsVtProvisionedOnDevice = this.mImsMgr.isVtProvisionedOnDevice();
        boolean z2 = carrierConfigForSubId.getBoolean("ignore_data_enabled_changed_for_video_calls");
        boolean zIsDataEnabled = this.mPhone.mDcTracker.isDataEnabled();
        log("isVtEnabledByPlatform:" + zIsVtEnabledByPlatform + "\nisVtProvisionedOnDevice:" + zIsVtProvisionedOnDevice + "\nisIgnoreDataChanged:" + z2 + "\nisDataEnabled:" + zIsDataEnabled);
        if (zIsVtEnabledByPlatform && zIsVtProvisionedOnDevice && (z2 || zIsDataEnabled)) {
            if (MtkImsManager.isSupportMims() || TelephonyUtilsEx.isCapabilityPhone(this.mPhone)) {
                boolean zIsEnhanced4gLteModeSettingEnabledByUser = this.mImsMgr.isEnhanced4gLteModeSettingEnabledByUser();
                if (zIsEnhanced4gLteModeSettingEnabledByUser) {
                    zIsVtEnabledByUser = this.mImsMgr.isVtEnabledByUser();
                } else {
                    zIsVtEnabledByUser = false;
                }
                log("isEnhanced4gLteModeSettingEnabledByUser:" + zIsEnhanced4gLteModeSettingEnabledByUser + "\nisVtEnabledByUser:" + zIsVtEnabledByUser);
                if (CallSettingUtils.isCtVolteNone4gSim(subId) && !zIsEnhanced4gLteModeSettingEnabledByUser) {
                    preferenceScreen2.removePreference(this.mEnableVideoCalling);
                } else {
                    this.mEnableVideoCalling.setChecked(zIsVtEnabledByUser);
                    this.mEnableVideoCalling.setOnPreferenceChangeListener(this);
                }
            } else {
                preferenceScreen2.removePreference(this.mEnableVideoCalling);
            }
        } else {
            preferenceScreen2.removePreference(this.mEnableVideoCalling);
        }
        boolean z3 = carrierConfigForSubId.getBoolean("mtk_wfc_remove_preference_mode_bool");
        log("removeWfcPrefMode:" + z3);
        PhoneAccountHandle simCallManager = this.mTelecomManager.getSimCallManager();
        if (simCallManager != null) {
            Intent intentBuildPhoneAccountConfigureIntent = PhoneAccountSettingsFragment.buildPhoneAccountConfigureIntent(this, simCallManager);
            log("simCallManager is not null");
            if (intentBuildPhoneAccountConfigureIntent != null) {
                PackageManager packageManager = this.mPhone.getContext().getPackageManager();
                List<ResolveInfo> listQueryIntentActivities = packageManager.queryIntentActivities(intentBuildPhoneAccountConfigureIntent, 0);
                if (!listQueryIntentActivities.isEmpty()) {
                    log("set wfc by: " + ((Object) listQueryIntentActivities.get(0).loadLabel(packageManager)));
                    this.mButtonWifiCalling.setTitle(listQueryIntentActivities.get(0).loadLabel(packageManager));
                    this.mButtonWifiCalling.setSummary((CharSequence) null);
                    this.mButtonWifiCalling.setIntent(intentBuildPhoneAccountConfigureIntent);
                } else {
                    log("Remove WFC Preference since resolutions is empty");
                    preferenceScreen2.removePreference(this.mButtonWifiCalling);
                }
            } else {
                log("Remove WFC Preference since PhoneAccountConfigureIntent is null");
                preferenceScreen2.removePreference(this.mButtonWifiCalling);
            }
        } else if (!this.mImsMgr.isWfcEnabledByPlatform() || !this.mImsMgr.isWfcProvisionedOnDevice()) {
            log("Remove WFC Preference since wfc is not enabled on the device.");
            preferenceScreen2.removePreference(this.mButtonWifiCalling);
        } else if (z3) {
            this.mButtonWifiCalling.setSummary((CharSequence) null);
        } else {
            int i = android.R.string.notification_channel_network_available;
            if (this.mImsMgr.isWfcEnabledByUser()) {
                int wfcMode = this.mImsMgr.getWfcMode(telephonyManager.isNetworkRoaming());
                switch (wfcMode) {
                    case 0:
                        i = android.R.string.next_button_label;
                        break;
                    case 1:
                        i = android.R.string.news_notification_channel_label;
                        break;
                    case 2:
                        i = android.R.string.noApplications;
                        break;
                    default:
                        log("Unexpected WFC mode value: " + wfcMode);
                        break;
                }
            }
            this.mButtonWifiCalling.setSummary(i);
        }
        try {
            if (this.mImsMgr.getImsServiceState() != 2) {
                log("Feature state not ready so remove vt and wfc settings for  phone =" + this.mPhone.getPhoneId());
                preferenceScreen2.removePreference(this.mButtonWifiCalling);
                preferenceScreen2.removePreference(this.mEnableVideoCalling);
            }
        } catch (ImsException e) {
            log("Exception when trying to get ImsServiceStatus: " + e);
            preferenceScreen2.removePreference(this.mButtonWifiCalling);
            preferenceScreen2.removePreference(this.mEnableVideoCalling);
        }
        updateScreenStatus();
        ExtensionManager.getCallFeaturesSettingExt().initOtherCallFeaturesSetting(this);
        ExtensionManager.getCallFeaturesSettingExt().onCallFeatureSettingsEvent(0);
    }

    private void maybeHideVoicemailSettings() {
        String defaultDialerPackage = ((TelecomManager) getSystemService(TelecomManager.class)).getDefaultDialerPackage();
        if (defaultDialerPackage == null) {
            return;
        }
        try {
            Bundle bundle = getPackageManager().getApplicationInfo(defaultDialerPackage, 128).metaData;
            if (bundle == null) {
                return;
            }
            if (!bundle.getBoolean("android.telephony.HIDE_VOICEMAIL_SETTINGS_MENU", false)) {
                log("maybeHideVoicemailSettings(): not disabled by default dialer");
            } else {
                getPreferenceScreen().removePreference(this.mVoicemailSettingsScreen);
                log("maybeHideVoicemailSettings(): disabled by default dialer");
            }
        } catch (PackageManager.NameNotFoundException e) {
            log("maybeHideVoicemailSettings(): not controlled by default dialer");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        this.mSubscriptionInfoHelper = new SubscriptionInfoHelper(this, getIntent());
        this.mSubscriptionInfoHelper.setActionBarTitle(getActionBar(), getResources(), R.string.call_settings_with_label);
        this.mPhone = this.mSubscriptionInfoHelper.getPhone();
    }

    private static void log(String str) {
        Log.d(LOG_TAG, str);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            onBackPressed();
            return DBG;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static void goUpToTopLevelSetting(Activity activity, SubscriptionInfoHelper subscriptionInfoHelper) {
        Intent intent = subscriptionInfoHelper.getIntent(CallFeaturesSetting.class);
        intent.setAction("android.intent.action.MAIN");
        intent.addFlags(67108864);
        activity.startActivity(intent);
        activity.finish();
    }

    @Override
    protected void onDestroy() {
        if (UserManager.get(this).isAdminUser()) {
            ExtensionManager.getCallFeaturesSettingExt().onCallFeatureSettingsEvent(2);
            unregisterEventCallbacks();
        }
        super.onDestroy();
    }

    @Override
    public void handleSubInfoUpdate() {
        finish();
    }

    private void handlePreferenceClickForGsm(PreferenceScreen preferenceScreen, Preference preference, int i) {
        Intent intent;
        PersistableBundle carrierConfigForSubId = PhoneGlobals.getInstance().getCarrierConfigForSubId(i);
        if (preference == preferenceScreen.findPreference(CALL_FORWARDING_KEY)) {
            if (carrierConfigForSubId.getBoolean("mtk_support_vt_ss_bool")) {
                log("Support VT SS");
                intent = this.mSubscriptionInfoHelper.getIntent(GsmUmtsVTCFOptions.class);
            } else {
                log("Not Support VT SS");
                intent = this.mSubscriptionInfoHelper.getIntent(GsmUmtsCallForwardOptions.class);
            }
        } else if (preference == preferenceScreen.findPreference(CALL_BARRING_KEY)) {
            if (carrierConfigForSubId.getBoolean("mtk_support_vt_ss_bool")) {
                log("Support VT SS");
                intent = this.mSubscriptionInfoHelper.getIntent(GsmUmtsVTCBOptions.class);
            } else {
                log("Not Support VT SS");
                intent = this.mSubscriptionInfoHelper.getIntent(GsmUmtsCallBarringOptions.class);
            }
        } else {
            intent = this.mSubscriptionInfoHelper.getIntent(GsmUmtsAdditionalCallOptions.class);
        }
        SubscriptionInfoHelper.addExtrasToIntent(intent, MtkSubscriptionManager.getSubInfo((String) null, i));
        startActivity(intent);
    }

    private void handlePreferenceClickForCdma(PreferenceScreen preferenceScreen, Preference preference, int i, boolean z, boolean z2) {
        Intent intent;
        if (preference == preferenceScreen.findPreference(KEY_CALL_FORWARD)) {
            if (z2) {
                intent = this.mSubscriptionInfoHelper.getIntent(GsmUmtsCallForwardOptions.class);
            } else {
                intent = this.mSubscriptionInfoHelper.getIntent(CdmaCallForwardOptions.class);
            }
            SubscriptionInfoHelper.addExtrasToIntent(intent, MtkSubscriptionManager.getSubInfo((String) null, i));
            startActivity(intent);
            return;
        }
        if (preference == preferenceScreen.findPreference(KEY_CALLER_ID)) {
            Intent intent2 = this.mSubscriptionInfoHelper.getIntent(CdmaCLIRUtOptions.class);
            SubscriptionInfoHelper.addExtrasToIntent(intent2, MtkSubscriptionManager.getSubInfo((String) null, i));
            startActivity(intent2);
        } else if (z2 && z) {
            startActivity(this.mSubscriptionInfoHelper.getIntent(CdmaCallWaitingUtOptions.class));
        } else {
            showDialog(TimeConsumingPreferenceActivity.STK_CC_SS_TO_DIAL_VIDEO_ERROR);
        }
    }

    private boolean onPreferenceTreeClickMTK(PreferenceScreen preferenceScreen, Preference preference) {
        log("onPreferenceTreeClickMTK: " + preference.getKey());
        int subId = this.mPhone.getSubId();
        if (preference == preferenceScreen.findPreference(CALL_FORWARDING_KEY) || preference == preferenceScreen.findPreference(ADDITIONAL_GSM_SETTINGS_KEY) || preference == preferenceScreen.findPreference(CALL_BARRING_KEY)) {
            CallSettingUtils.DialogType dialogTipsType = CallSettingUtils.getDialogTipsType(this, this.mPhone.getSubId());
            if (dialogTipsType != CallSettingUtils.DialogType.NONE) {
                CallSettingUtils.showDialogTips(this, subId, dialogTipsType, preference);
            } else {
                handlePreferenceClickForGsm(preferenceScreen, preference, subId);
            }
            return DBG;
        }
        if (preference == preferenceScreen.findPreference(KEY_CALL_FORWARD) || preference == preferenceScreen.findPreference(KEY_CALL_WAIT) || preference == preferenceScreen.findPreference(KEY_CALLER_ID)) {
            boolean zIsImsServiceAvailable = TelephonyUtils.isImsServiceAvailable(this, subId);
            boolean zIsUtPreferCdmaSim = CallSettingUtils.isUtPreferCdmaSim(subId);
            CallSettingUtils.DialogType dialogTipsType2 = CallSettingUtils.DialogType.NONE;
            if (zIsUtPreferCdmaSim) {
                dialogTipsType2 = CallSettingUtils.getDialogTipsType(this, this.mPhone.getSubId());
            }
            if (dialogTipsType2 != CallSettingUtils.DialogType.NONE) {
                if (!zIsImsServiceAvailable && MtkImsManager.isSupportMims() && preference == preferenceScreen.findPreference(KEY_CALL_WAIT)) {
                    showDialog(TimeConsumingPreferenceActivity.STK_CC_SS_TO_DIAL_VIDEO_ERROR);
                } else {
                    CallSettingUtils.showDialogTips(this, subId, dialogTipsType2, preference);
                }
            } else {
                handlePreferenceClickForCdma(preferenceScreen, preference, subId, zIsImsServiceAvailable, zIsUtPreferCdmaSim);
            }
            return DBG;
        }
        return false;
    }

    private void updateScreenStatusOnUI() {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                CallFeaturesSetting.log("updateScreenStatusOnUI");
                CallFeaturesSetting.this.updateScreenStatus();
            }
        });
    }

    private void updateScreenStatus() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        boolean zIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(PhoneGlobals.getInstance());
        if (preferenceScreen == null) {
            return;
        }
        int subId = this.mPhone.getSubId();
        boolean zIsValidSubscriptionId = SubscriptionManager.isValidSubscriptionId(subId);
        log("updateScreenStatus, hasSubId " + zIsValidSubscriptionId);
        boolean z = false;
        int i = 0;
        while (true) {
            int preferenceCount = preferenceScreen.getPreferenceCount();
            boolean z2 = DBG;
            if (i >= preferenceCount) {
                break;
            }
            Preference preference = preferenceScreen.getPreference(i);
            if (zIsAirplaneModeOn || !zIsValidSubscriptionId) {
                z2 = false;
            }
            preference.setEnabled(z2);
            i++;
        }
        if (zIsValidSubscriptionId) {
            if (this.mImsRegState == 0) {
                z = true;
            }
            Preference preferenceFindPreference = getPreferenceScreen().findPreference(CALL_FORWARDING_KEY);
            Preference preferenceFindPreference2 = getPreferenceScreen().findPreference(CALL_BARRING_KEY);
            Preference preferenceFindPreference3 = getPreferenceScreen().findPreference(ADDITIONAL_GSM_SETTINGS_KEY);
            if (preferenceFindPreference != null && z && (MtkImsManager.isSupportMims() || TelephonyUtilsEx.isCapabilityPhone(this.mPhone))) {
                log(" --- set SS item enabled when IMS is registered ---");
                preferenceFindPreference.setEnabled(DBG);
                preferenceFindPreference2.setEnabled(DBG);
                preferenceFindPreference3.setEnabled(DBG);
            }
            if (CallSettingUtils.isUtPreferByCdmaSimAndImsOn(this, subId, DBG, z)) {
                Preference preferenceFindPreference4 = getPreferenceScreen().findPreference(KEY_CALL_FORWARD);
                Preference preferenceFindPreference5 = getPreferenceScreen().findPreference(KEY_CALL_WAIT);
                Preference preferenceFindPreference6 = getPreferenceScreen().findPreference(KEY_CALLER_ID);
                log(" -- set CDMA SS item enabled when IMS is registered for SmartFren only --");
                if (preferenceFindPreference4 != null) {
                    preferenceFindPreference4.setEnabled(DBG);
                }
                if (preferenceFindPreference5 != null) {
                    preferenceFindPreference5.setEnabled(DBG);
                }
                if (preferenceFindPreference6 != null) {
                    preferenceFindPreference6.setEnabled(DBG);
                }
            }
        }
        updateOptionsByCallState(zIsValidSubscriptionId);
    }

    @Override
    protected Dialog onCreateDialog(int i) {
        if (i == 1000) {
            return new CdmaCallWaitOptions(this, this.mPhone).createDialog();
        }
        return null;
    }

    private void registerEventCallbacks() {
        PhoneGlobals.getInstance().addSubInfoUpdateListener(this);
        registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.AIRPLANE_MODE"));
        try {
            ImsManager imsManager = ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId());
            if (imsManager != null) {
                imsManager.addRegistrationListener(1, this.mImsConnectionStateListener);
            }
        } catch (ImsException e) {
            log("ImsException:" + e);
        }
    }

    private void unregisterEventCallbacks() {
        PhoneGlobals.getInstance().removeSubInfoUpdateListener(this);
        unregisterReceiver(this.mReceiver);
        try {
            ImsManager imsManager = ImsManager.getInstance(this.mPhone.getContext(), this.mPhone.getPhoneId());
            if (imsManager != null) {
                imsManager.removeRegistrationListener(this.mImsConnectionStateListener);
            }
        } catch (ImsException e) {
            log("ImsException:" + e);
        }
    }

    private void updateOptionsByCallState(boolean z) {
        if (this.mEnableVideoCalling != null) {
            boolean zIsInCall = TelephonyUtils.isInCall(this);
            boolean z2 = false;
            this.mEnableVideoCalling.setEnabled(z && !zIsInCall);
            Preference preference = this.mButtonWifiCalling;
            if (z && !zIsInCall) {
                z2 = true;
            }
            preference.setEnabled(z2);
        }
    }

    private void configCdmaVoicePrivacy(PreferenceScreen preferenceScreen, boolean z) {
        CdmaVoicePrivacySwitchPreference cdmaVoicePrivacySwitchPreference = (CdmaVoicePrivacySwitchPreference) findPreference(BUTTON_CP_KEY);
        if (cdmaVoicePrivacySwitchPreference != null) {
            if (z) {
                log("Voice privacy option removed");
                preferenceScreen.removePreference(cdmaVoicePrivacySwitchPreference);
            } else {
                cdmaVoicePrivacySwitchPreference.setPhone(this.mPhone);
            }
        }
    }

    private void configCdmaCallerId(PreferenceScreen preferenceScreen, boolean z) {
        if (!z) {
            preferenceScreen.removePreference(preferenceScreen.findPreference(KEY_CALLER_ID));
            log("No support by operator, so remove Caller ID pref for CDMA");
        }
    }

    private void configCdmaCallWaiting(PreferenceScreen preferenceScreen, PersistableBundle persistableBundle) {
        String string = persistableBundle.getString("carrier_name_string");
        if (string != null && string.equalsIgnoreCase("Sprint")) {
            preferenceScreen.removePreference(preferenceScreen.findPreference(KEY_CALL_WAIT));
            log("No support by operator, so remove call waiting pref for CDMA");
        }
    }

    public int getPhoneId() {
        return this.mPhone.getPhoneId();
    }
}
