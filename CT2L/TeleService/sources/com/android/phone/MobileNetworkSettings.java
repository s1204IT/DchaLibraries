package com.android.phone;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MobileNetworkSettings extends PreferenceActivity implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener, Preference.OnPreferenceChangeListener {
    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;
    private List<SubscriptionInfo> mActiveSubInfos;
    private SwitchPreference mButton4glte;
    private SwitchPreference mButtonDataRoam;
    private ListPreference mButtonEnabledNetworks;
    private ListPreference mButtonPreferredNetworkMode;
    CdmaOptions mCdmaOptions;
    private Preference mClickedPreference;
    GsmUmtsOptions mGsmUmtsOptions;
    private MyHandler mHandler;
    private boolean mIsGlobalCdma;
    private Preference mLteDataServicePref;
    private boolean mOkClicked;
    private Phone mPhone;
    private boolean mShow4GForLTE;
    private SubscriptionManager mSubscriptionManager;
    private TabHost mTabHost;
    private UserManager mUm;
    private boolean mUnavailable;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            MobileNetworkSettings.log("PhoneStateListener.onCallStateChanged: state=" + state);
            Preference pref = MobileNetworkSettings.this.getPreferenceScreen().findPreference("enhanced_4g_lte");
            if (pref != null) {
                pref.setEnabled(state == 0 && ImsManager.isNonTtyOrTtyOnVolteEnabled(MobileNetworkSettings.this.getApplicationContext()));
            }
        }
    };
    private final BroadcastReceiver mPhoneChangeReceiver = new PhoneChangeReceiver();
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            MobileNetworkSettings.log("onSubscriptionsChanged:");
            MobileNetworkSettings.this.initializeSubscriptions();
        }
    };
    private TabHost.OnTabChangeListener mTabListener = new TabHost.OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            MobileNetworkSettings.log("onTabChanged:");
            MobileNetworkSettings.this.updatePhone(Integer.parseInt(tabId));
            MobileNetworkSettings.this.updateBody();
        }
    };
    private TabHost.TabContentFactory mEmptyTabContent = new TabHost.TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(MobileNetworkSettings.this.mTabHost.getContext());
        }
    };

    private enum TabState {
        NO_TABS,
        UPDATE,
        DO_NOTHING
    }

    private class PhoneChangeReceiver extends BroadcastReceiver {
        private PhoneChangeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            MobileNetworkSettings.log("onReceive:");
            MobileNetworkSettings.this.mGsmUmtsOptions = null;
            MobileNetworkSettings.this.mCdmaOptions = null;
            MobileNetworkSettings.this.updateBody();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            this.mPhone.setDataRoamingEnabled(true);
            this.mOkClicked = true;
        } else {
            this.mButtonDataRoam.setChecked(false);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        this.mButtonDataRoam.setChecked(this.mOkClicked);
    }

    private int getPreferredNetworkMode() {
        return PhoneFactory.calculatePreferredNetworkType(this.mPhone.getContext(), this.mPhone.getSubId());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        int phoneSubId = this.mPhone.getSubId();
        if (preference.getKey().equals("enhanced_4g_lte")) {
            return true;
        }
        if (this.mGsmUmtsOptions != null && this.mGsmUmtsOptions.preferenceTreeClick(preference)) {
            return true;
        }
        if (this.mCdmaOptions != null && this.mCdmaOptions.preferenceTreeClick(preference)) {
            if (!Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"))) {
                return true;
            }
            this.mClickedPreference = preference;
            startActivityForResult(new Intent("android.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS", (Uri) null), 17);
            return true;
        }
        if (preference == this.mButtonPreferredNetworkMode) {
            int settingsNetworkMode = PhoneFactory.calculatePreferredNetworkType(this.mPhone.getContext(), phoneSubId);
            this.mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            return true;
        }
        if (preference == this.mLteDataServicePref) {
            String tmpl = Settings.Global.getString(getContentResolver(), "setup_prepaid_data_service_url");
            if (!TextUtils.isEmpty(tmpl)) {
                TelephonyManager tm = (TelephonyManager) getSystemService("phone");
                String imsi = tm.getSubscriberId();
                if (imsi == null) {
                    imsi = "";
                }
                String url = TextUtils.isEmpty(tmpl) ? null : TextUtils.expandTemplate(tmpl, imsi).toString();
                Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(url));
                startActivity(intent);
                return true;
            }
            Log.e("NetworkSettings", "Missing SETUP_PREPAID_DATA_SERVICE_URL");
            return true;
        }
        if (preference == this.mButtonEnabledNetworks) {
            int settingsNetworkMode2 = getPreferredNetworkMode();
            UpdateEnabledNetworksValueAndSummary(settingsNetworkMode2);
            return true;
        }
        if (preference == this.mButtonDataRoam) {
            return true;
        }
        preferenceScreen.setEnabled(false);
        return false;
    }

    private void initializeSubscriptions() {
        String tabName;
        int currentTab = 0;
        log("initializeSubscriptions:+");
        List<SubscriptionInfo> sil = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        TabState state = isUpdateTabsNeeded(sil);
        this.mActiveSubInfos.clear();
        if (sil != null) {
            this.mActiveSubInfos.addAll(sil);
            if (sil.size() == 1) {
                currentTab = sil.get(0).getSimSlotIndex();
            }
        }
        switch (state) {
            case UPDATE:
                log("initializeSubscriptions: UPDATE");
                currentTab = this.mTabHost != null ? this.mTabHost.getCurrentTab() : 0;
                setContentView(R.layout.network_settings);
                this.mTabHost = (TabHost) findViewById(android.R.id.tabhost);
                this.mTabHost.setup();
                Iterator<SubscriptionInfo> siIterator = this.mActiveSubInfos.listIterator();
                SubscriptionInfo si = siIterator.hasNext() ? siIterator.next() : null;
                for (int simSlotIndex = 0; simSlotIndex < this.mActiveSubInfos.size(); simSlotIndex++) {
                    if (si != null && si.getSimSlotIndex() == simSlotIndex) {
                        tabName = String.valueOf(si.getDisplayName());
                        si = siIterator.hasNext() ? siIterator.next() : null;
                    } else {
                        tabName = getResources().getString(R.string.unknown);
                    }
                    log("initializeSubscriptions: tab=" + simSlotIndex + " name=" + tabName);
                    this.mTabHost.addTab(buildTabSpec(String.valueOf(simSlotIndex), tabName));
                }
                this.mTabHost.setOnTabChangedListener(this.mTabListener);
                this.mTabHost.setCurrentTab(currentTab);
                break;
            case NO_TABS:
                log("initializeSubscriptions: NO_TABS");
                if (this.mTabHost != null) {
                    this.mTabHost.clearAllTabs();
                    this.mTabHost = null;
                }
                setContentView(R.layout.network_settings);
                break;
            case DO_NOTHING:
                log("initializeSubscriptions: DO_NOTHING");
                if (this.mTabHost != null) {
                    currentTab = this.mTabHost.getCurrentTab();
                }
                break;
        }
        updatePhone(currentTab);
        updateBody();
        log("initializeSubscriptions:-");
    }

    private TabState isUpdateTabsNeeded(List<SubscriptionInfo> newSil) {
        TabState state = TabState.DO_NOTHING;
        if (newSil == null) {
            if (this.mActiveSubInfos.size() >= 2) {
                log("isUpdateTabsNeeded: NO_TABS, size unknown and was tabbed");
                state = TabState.NO_TABS;
            }
        } else if (newSil.size() < 2 && this.mActiveSubInfos.size() >= 2) {
            log("isUpdateTabsNeeded: NO_TABS, size went to small");
            state = TabState.NO_TABS;
        } else if (newSil.size() >= 2 && this.mActiveSubInfos.size() < 2) {
            log("isUpdateTabsNeeded: UPDATE, size changed");
            state = TabState.UPDATE;
        } else if (newSil.size() >= 2) {
            Iterator<SubscriptionInfo> siIterator = this.mActiveSubInfos.iterator();
            Iterator<SubscriptionInfo> it = newSil.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                SubscriptionInfo newSi = it.next();
                SubscriptionInfo curSi = siIterator.next();
                if (!newSi.getDisplayName().equals(curSi.getDisplayName())) {
                    log("isUpdateTabsNeeded: UPDATE, new name=" + ((Object) newSi.getDisplayName()));
                    state = TabState.UPDATE;
                    break;
                }
            }
        }
        log("isUpdateTabsNeeded:- " + state + " newSil.size()=" + (newSil != null ? newSil.size() : 0) + " mActiveSubInfos.size()=" + this.mActiveSubInfos.size());
        return state;
    }

    private void updatePhone(int slotId) {
        SubscriptionInfo sir = findRecordBySlotId(slotId);
        if (sir != null) {
            this.mPhone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(sir.getSubscriptionId()));
        }
        if (this.mPhone == null) {
            this.mPhone = PhoneGlobals.getPhone();
        }
        log("updatePhone:- slotId=" + slotId + " sir=" + sir);
    }

    private TabHost.TabSpec buildTabSpec(String tag, String title) {
        return this.mTabHost.newTabSpec(tag).setIndicator(title).setContent(this.mEmptyTabContent);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        log("onCreate:+");
        setTheme(R.style.Theme_Material_Settings);
        super.onCreate(icicle);
        this.mHandler = new MyHandler();
        this.mUm = (UserManager) getSystemService("user");
        this.mSubscriptionManager = SubscriptionManager.from(this);
        if (this.mUm.hasUserRestriction("no_config_mobile_networks")) {
            this.mUnavailable = true;
            setContentView(R.layout.telephony_disallowed_preference_screen);
            return;
        }
        addPreferencesFromResource(R.xml.network_setting);
        this.mButton4glte = (SwitchPreference) findPreference("enhanced_4g_lte");
        this.mButton4glte.setOnPreferenceChangeListener(this);
        try {
            Context con = createPackageContext("com.android.systemui", 0);
            int id = con.getResources().getIdentifier("config_show4GForLTE", "bool", "com.android.systemui");
            this.mShow4GForLTE = con.getResources().getBoolean(id);
        } catch (PackageManager.NameNotFoundException e) {
            loge("NameNotFoundException for show4GFotLTE");
            this.mShow4GForLTE = false;
        }
        PreferenceScreen prefSet = getPreferenceScreen();
        this.mButtonDataRoam = (SwitchPreference) prefSet.findPreference("button_roaming_key");
        this.mButtonPreferredNetworkMode = (ListPreference) prefSet.findPreference("preferred_network_mode_key");
        this.mButtonEnabledNetworks = (ListPreference) prefSet.findPreference("enabled_networks_key");
        this.mButtonDataRoam.setOnPreferenceChangeListener(this);
        this.mLteDataServicePref = prefSet.findPreference("cdma_lte_data_service_key");
        int max = this.mSubscriptionManager.getActiveSubscriptionInfoCountMax();
        this.mActiveSubInfos = new ArrayList(max);
        initializeSubscriptions();
        IntentFilter intentFilter = new IntentFilter("android.intent.action.RADIO_TECHNOLOGY");
        registerReceiver(this.mPhoneChangeReceiver, intentFilter);
        log("onCreate:-");
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume:+");
        if (this.mUnavailable) {
            log("onResume:- ignore mUnavailable == false");
            return;
        }
        getPreferenceScreen().setEnabled(true);
        this.mButtonDataRoam.setChecked(this.mPhone.getDataRoamingEnabled());
        if (getPreferenceScreen().findPreference("preferred_network_mode_key") != null) {
            this.mPhone.getPreferredNetworkType(this.mHandler.obtainMessage(0));
        }
        if (getPreferenceScreen().findPreference("enabled_networks_key") != null) {
            this.mPhone.getPreferredNetworkType(this.mHandler.obtainMessage(0));
        }
        if (ImsManager.isVolteEnabledByPlatform(this) && ImsManager.isVolteProvisionedOnDevice(this)) {
            TelephonyManager tm = (TelephonyManager) getSystemService("phone");
            tm.listen(this.mPhoneStateListener, 32);
        }
        this.mButton4glte.setChecked(ImsManager.isEnhanced4gLteModeSettingEnabledByUser(this) && ImsManager.isNonTtyOrTtyOnVolteEnabled(this));
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
        log("onResume:-");
    }

    private boolean isMasterSim() {
        int masterSim = Dsds.isSim2Master() ? PhoneConstants.SimId.SIM2.ordinal() : PhoneConstants.SimId.SIM1.ordinal();
        return masterSim == this.mPhone.getPhoneId();
    }

    private void updateBody() {
        Preference pref;
        getApplicationContext();
        PreferenceScreen prefSet = getPreferenceScreen();
        boolean isLteOnCdma = this.mPhone.getLteOnCdmaMode() == 1;
        int phoneSubId = this.mPhone.getSubId();
        log("updateBody: isLteOnCdma=" + isLteOnCdma + " phoneSubId=" + phoneSubId);
        if (prefSet != null) {
            prefSet.removeAll();
            prefSet.addPreference(this.mButtonDataRoam);
            prefSet.addPreference(this.mButtonPreferredNetworkMode);
            prefSet.addPreference(this.mButtonEnabledNetworks);
            prefSet.addPreference(this.mButton4glte);
        }
        int settingsNetworkMode = getPreferredNetworkMode();
        this.mIsGlobalCdma = isLteOnCdma && getResources().getBoolean(R.bool.config_show_cdma);
        int shouldHideCarrierSettings = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "hide_carrier_network_settings", 0);
        if (shouldHideCarrierSettings == 1) {
            prefSet.removePreference(this.mButtonPreferredNetworkMode);
            prefSet.removePreference(this.mButtonEnabledNetworks);
            prefSet.removePreference(this.mLteDataServicePref);
        } else if (getResources().getBoolean(R.bool.world_phone)) {
            prefSet.removePreference(this.mButtonEnabledNetworks);
            this.mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);
            this.mCdmaOptions = new CdmaOptions(this, prefSet, this.mPhone);
            this.mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, phoneSubId);
        } else {
            prefSet.removePreference(this.mButtonPreferredNetworkMode);
            int phoneType = this.mPhone.getPhoneType();
            if (phoneType == 2) {
                int lteForced = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "lte_service_forced" + this.mPhone.getSubId(), 0);
                if (isLteOnCdma) {
                    if (lteForced == 0) {
                        this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_choices);
                        this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_values);
                    } else {
                        switch (settingsNetworkMode) {
                            case 4:
                            case 5:
                            case 6:
                                this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_no_lte_choices);
                                this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_no_lte_values);
                                break;
                            case 7:
                            case 8:
                            case 10:
                            case 11:
                                this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_only_lte_choices);
                                this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_only_lte_values);
                                break;
                            case 9:
                            default:
                                this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_choices);
                                this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_values);
                                break;
                        }
                    }
                }
                this.mCdmaOptions = new CdmaOptions(this, prefSet, this.mPhone);
                if (isWorldMode()) {
                    this.mGsmUmtsOptions = null;
                }
            } else if (phoneType == 1) {
                if (!getResources().getBoolean(R.bool.config_prefer_2g) && !getResources().getBoolean(R.bool.config_enabled_lte)) {
                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_except_gsm_lte_choices);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_except_gsm_lte_values);
                } else if (!getResources().getBoolean(R.bool.config_prefer_2g)) {
                    int select = this.mShow4GForLTE ? R.array.enabled_networks_except_gsm_4g_choices : R.array.enabled_networks_except_gsm_choices;
                    this.mButtonEnabledNetworks.setEntries(select);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_except_gsm_values);
                } else if (!getResources().getBoolean(R.bool.config_enabled_lte)) {
                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_except_lte_choices);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_except_lte_values);
                } else if (this.mIsGlobalCdma) {
                    this.mButtonEnabledNetworks.setEntries(R.array.enabled_networks_cdma_choices);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_cdma_values);
                } else {
                    int select2 = this.mShow4GForLTE ? R.array.enabled_networks_4g_choices : R.array.enabled_networks_choices;
                    this.mButtonEnabledNetworks.setEntries(select2);
                    this.mButtonEnabledNetworks.setEntryValues(R.array.enabled_networks_values);
                }
                this.mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, phoneSubId);
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
            if (isWorldMode()) {
                this.mButtonEnabledNetworks.setEntries(R.array.preferred_network_mode_choices_world_mode);
                this.mButtonEnabledNetworks.setEntryValues(R.array.preferred_network_mode_values_world_mode);
            }
            this.mButtonEnabledNetworks.setOnPreferenceChangeListener(this);
            log("settingsNetworkMode: " + settingsNetworkMode);
        }
        boolean missingDataServiceUrl = TextUtils.isEmpty(Settings.Global.getString(getContentResolver(), "setup_prepaid_data_service_url"));
        if (!isLteOnCdma || missingDataServiceUrl) {
            prefSet.removePreference(this.mLteDataServicePref);
        } else {
            Log.d("NetworkSettings", "keep ltePref");
        }
        if ((!ImsManager.isVolteEnabledByPlatform(this) || !ImsManager.isVolteProvisionedOnDevice(this)) && (pref = prefSet.findPreference("enhanced_4g_lte")) != null) {
            prefSet.removePreference(pref);
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        boolean isSecondaryUser = UserHandle.myUserId() != 0;
        boolean isCellBroadcastAppLinkEnabled = getResources().getBoolean(android.R.^attr-private.hideWheelUntilFocused);
        if (isSecondaryUser || !isCellBroadcastAppLinkEnabled || this.mUm.hasUserRestriction("no_config_cell_broadcasts")) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference("cell_broadcast_settings");
            if (ps != null) {
                root.removePreference(ps);
            }
        }
        this.mButtonDataRoam.setChecked(this.mPhone.getDataRoamingEnabled());
        this.mButtonEnabledNetworks.setValue(Integer.toString(settingsNetworkMode));
        this.mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
        UpdatePreferredNetworkModeSummary(settingsNetworkMode);
        UpdateEnabledNetworksValueAndSummary(settingsNetworkMode);
        this.mPhone.setPreferredNetworkType(settingsNetworkMode, this.mHandler.obtainMessage(1));
        boolean hasActiveSubscriptions = this.mActiveSubInfos.size() > 0;
        this.mButtonDataRoam.setEnabled(hasActiveSubscriptions);
        this.mButtonPreferredNetworkMode.setEnabled(hasActiveSubscriptions);
        this.mButtonEnabledNetworks.setEnabled(hasActiveSubscriptions);
        this.mButton4glte.setEnabled(hasActiveSubscriptions);
        this.mLteDataServicePref.setEnabled(hasActiveSubscriptions);
        getPreferenceScreen();
        Preference ps2 = findPreference("cell_broadcast_settings");
        if (ps2 != null) {
            ps2.setEnabled(hasActiveSubscriptions);
        }
        Preference ps3 = findPreference("button_apn_key");
        if (ps3 != null) {
            ps3.setEnabled(hasActiveSubscriptions);
        }
        Preference ps4 = findPreference("button_carrier_sel_key");
        if (ps4 != null) {
            ps4.setEnabled(hasActiveSubscriptions);
        }
        Preference ps5 = findPreference("carrier_settings_key");
        if (ps5 != null) {
            ps5.setEnabled(hasActiveSubscriptions);
        }
        Preference ps6 = findPreference("cdma_system_select_key");
        if (ps6 != null) {
            ps6.setEnabled(hasActiveSubscriptions);
        }
        if (this.mButtonPreferredNetworkMode != null) {
            this.mButtonPreferredNetworkMode.setEnabled(isMasterSim());
            this.mPhone.getPreferredNetworkType(this.mHandler.obtainMessage(0));
        }
        if (this.mButtonEnabledNetworks != null) {
            this.mButtonEnabledNetworks.setEnabled(isMasterSim());
            this.mPhone.getPreferredNetworkType(this.mHandler.obtainMessage(0));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause:+");
        if (ImsManager.isVolteEnabledByPlatform(this) && ImsManager.isVolteProvisionedOnDevice(this)) {
            TelephonyManager tm = (TelephonyManager) getSystemService("phone");
            tm.listen(this.mPhoneStateListener, 0);
        }
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
        log("onPause:-");
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        int modemNetworkMode;
        int modemNetworkMode2;
        int phoneSubId = this.mPhone.getSubId();
        int phoneId = this.mPhone.getPhoneId();
        if (preference == this.mButtonPreferredNetworkMode) {
            this.mButtonPreferredNetworkMode.setValue((String) objValue);
            int buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
            int settingsNetworkMode = PhoneFactory.calculatePreferredNetworkType(this.mPhone.getContext(), phoneSubId);
            if (buttonNetworkMode != settingsNetworkMode) {
                switch (buttonNetworkMode) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 10:
                    case 11:
                    case 12:
                        modemNetworkMode2 = buttonNetworkMode;
                        PhoneFactory.saveNetworkMode(this.mPhone.getContext(), phoneId, buttonNetworkMode);
                        this.mPhone.setPreferredNetworkType(modemNetworkMode2, this.mHandler.obtainMessage(1));
                        updateBody();
                        break;
                    case 9:
                        modemNetworkMode2 = 21;
                        buttonNetworkMode = 21;
                        PhoneFactory.saveNetworkMode(this.mPhone.getContext(), phoneId, buttonNetworkMode);
                        this.mPhone.setPreferredNetworkType(modemNetworkMode2, this.mHandler.obtainMessage(1));
                        updateBody();
                        break;
                    default:
                        loge("Invalid Network Mode (" + buttonNetworkMode + ") chosen. Ignore.");
                        break;
                }
            } else {
                updateBody();
            }
        } else if (preference == this.mButtonEnabledNetworks) {
            this.mButtonEnabledNetworks.setValue((String) objValue);
            int buttonNetworkMode2 = Integer.valueOf((String) objValue).intValue();
            log("buttonNetworkMode: " + buttonNetworkMode2);
            int settingsNetworkMode2 = PhoneFactory.calculatePreferredNetworkType(this.mPhone.getContext(), phoneSubId);
            if (buttonNetworkMode2 != settingsNetworkMode2) {
                switch (buttonNetworkMode2) {
                    case 0:
                    case 1:
                    case 4:
                    case 5:
                    case 8:
                    case 10:
                        modemNetworkMode = buttonNetworkMode2;
                        UpdateEnabledNetworksValueAndSummary(buttonNetworkMode2);
                        PhoneFactory.saveNetworkMode(this.mPhone.getContext(), phoneId, buttonNetworkMode2);
                        this.mPhone.setPreferredNetworkType(modemNetworkMode, this.mHandler.obtainMessage(1));
                        updateBody();
                        break;
                    case 2:
                    case 3:
                    case 6:
                    case 7:
                    default:
                        loge("Invalid Network Mode (" + buttonNetworkMode2 + ") chosen. Ignore.");
                        break;
                    case 9:
                        modemNetworkMode = 21;
                        buttonNetworkMode2 = 21;
                        UpdateEnabledNetworksValueAndSummary(buttonNetworkMode2);
                        PhoneFactory.saveNetworkMode(this.mPhone.getContext(), phoneId, buttonNetworkMode2);
                        this.mPhone.setPreferredNetworkType(modemNetworkMode, this.mHandler.obtainMessage(1));
                        updateBody();
                        break;
                }
            }
        } else {
            if (preference == this.mButton4glte) {
                SwitchPreference ltePref = (SwitchPreference) preference;
                ltePref.setChecked(ltePref.isChecked() ? false : true);
                ImsManager.setEnhanced4gLteModeSetting(this, ltePref.isChecked());
            } else if (preference == this.mButtonDataRoam) {
                log("onPreferenceTreeClick: preference == mButtonDataRoam.");
                if (!this.mButtonDataRoam.isChecked()) {
                    this.mOkClicked = false;
                    new AlertDialog.Builder(this).setMessage(getResources().getString(R.string.roaming_warning)).setTitle(android.R.string.dialog_alert_title).setIconAttribute(android.R.attr.alertDialogIcon).setPositiveButton(android.R.string.yes, this).setNegativeButton(android.R.string.no, this).show().setOnDismissListener(this);
                } else {
                    this.mPhone.setDataRoamingEnabled(false);
                }
            }
            updateBody();
        }
        return true;
    }

    private boolean isVendorNetworkType(int networkType) {
        if (networkType < 13 || networkType > 21) {
            return false;
        }
        return true;
    }

    private class MyHandler extends Handler {
        private MyHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    handleGetPreferredNetworkTypeResponse(msg);
                    break;
                case 1:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
            }
        }

        private void handleGetPreferredNetworkTypeResponse(Message msg) {
            int phoneSubId = MobileNetworkSettings.this.mPhone.getSubId();
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                int modemNetworkMode = ((int[]) ar.result)[0];
                MobileNetworkSettings.log("handleGetPreferredNetworkTypeResponse: modemNetworkMode = " + modemNetworkMode);
                int settingsNetworkMode = PhoneFactory.calculatePreferredNetworkType(MobileNetworkSettings.this.mPhone.getContext(), phoneSubId);
                MobileNetworkSettings.log("handleGetPreferredNetworkTypeReponse: settingsNetworkMode = " + settingsNetworkMode);
                if (modemNetworkMode == 0 || modemNetworkMode == 1 || modemNetworkMode == 2 || modemNetworkMode == 3 || modemNetworkMode == 4 || modemNetworkMode == 5 || modemNetworkMode == 6 || modemNetworkMode == 7 || modemNetworkMode == 8 || modemNetworkMode == 9 || modemNetworkMode == 10 || modemNetworkMode == 11 || modemNetworkMode == 12 || MobileNetworkSettings.this.isVendorNetworkType(modemNetworkMode)) {
                    MobileNetworkSettings.log("handleGetPreferredNetworkTypeResponse: if 1: modemNetworkMode = " + modemNetworkMode);
                    if (modemNetworkMode != settingsNetworkMode) {
                        MobileNetworkSettings.log("handleGetPreferredNetworkTypeResponse: if 2: modemNetworkMode != settingsNetworkMode");
                        MobileNetworkSettings.log("handleGetPreferredNetworkTypeResponse: if 2: settingsNetworkMode = " + modemNetworkMode);
                    }
                    MobileNetworkSettings.this.UpdatePreferredNetworkModeSummary(modemNetworkMode);
                    MobileNetworkSettings.this.UpdateEnabledNetworksValueAndSummary(modemNetworkMode);
                    if (!MobileNetworkSettings.this.isVendorNetworkType(modemNetworkMode)) {
                        MobileNetworkSettings.this.mButtonPreferredNetworkMode.setValue(Integer.toString(modemNetworkMode));
                        return;
                    }
                    return;
                }
                MobileNetworkSettings.log("handleGetPreferredNetworkTypeResponse: else: reset to default");
                resetNetworkModeToDefault();
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            int phoneId = MobileNetworkSettings.this.mPhone.getPhoneId();
            if (ar.exception == null) {
                int networkMode = Integer.valueOf(MobileNetworkSettings.this.mButtonPreferredNetworkMode.getValue()).intValue();
                PhoneFactory.saveNetworkMode(MobileNetworkSettings.this.mPhone.getContext(), phoneId, networkMode);
                int networkMode2 = Integer.valueOf(MobileNetworkSettings.this.mButtonEnabledNetworks.getValue()).intValue();
                PhoneFactory.saveNetworkMode(MobileNetworkSettings.this.mPhone.getContext(), phoneId, networkMode2);
                return;
            }
            MobileNetworkSettings.this.mPhone.getPreferredNetworkType(obtainMessage(0));
        }

        private void resetNetworkModeToDefault() {
            int phoneId = MobileNetworkSettings.this.mPhone.getPhoneId();
            MobileNetworkSettings.this.mButtonPreferredNetworkMode.setValue(Integer.toString(MobileNetworkSettings.preferredNetworkMode));
            MobileNetworkSettings.this.mButtonEnabledNetworks.setValue(Integer.toString(MobileNetworkSettings.preferredNetworkMode));
            PhoneFactory.saveNetworkMode(MobileNetworkSettings.this.mPhone.getContext(), phoneId, MobileNetworkSettings.preferredNetworkMode);
            MobileNetworkSettings.this.mPhone.setPreferredNetworkType(MobileNetworkSettings.preferredNetworkMode, obtainMessage(1));
        }
    }

    private void UpdatePreferredNetworkModeSummary(int NetworkMode) {
        switch (NetworkMode) {
            case 0:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_wcdma_perf_summary);
                break;
            case 1:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_gsm_only_summary);
                break;
            case 2:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_wcdma_only_summary);
                break;
            case 3:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_gsm_wcdma_summary);
                break;
            case 4:
                switch (this.mPhone.getLteOnCdmaMode()) {
                    case 1:
                        this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_cdma_summary);
                        break;
                    default:
                        this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_cdma_evdo_summary);
                        break;
                }
                break;
            case 5:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_cdma_only_summary);
                break;
            case 6:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_evdo_only_summary);
                break;
            case 7:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_cdma_evdo_gsm_wcdma_summary);
                break;
            case 8:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_cdma_evdo_summary);
                break;
            case 9:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_gsm_wcdma_summary);
                break;
            case 10:
                if (this.mPhone.getPhoneType() == 2) {
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_global_summary);
                } else {
                    this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_summary);
                }
                break;
            case 11:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_summary);
                break;
            case 12:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_lte_wcdma_summary);
                break;
            default:
                this.mButtonPreferredNetworkMode.setSummary(R.string.preferred_network_mode_global_summary);
                break;
        }
    }

    private void UpdateEnabledNetworksValueAndSummary(int NetworkMode) {
        int i = R.string.network_lte;
        switch (NetworkMode) {
            case 0:
            case 2:
            case 3:
                if (!this.mIsGlobalCdma) {
                    this.mButtonEnabledNetworks.setValue(Integer.toString(0));
                    this.mButtonEnabledNetworks.setSummary(R.string.network_3G);
                    return;
                } else {
                    this.mButtonEnabledNetworks.setValue(Integer.toString(10));
                    this.mButtonEnabledNetworks.setSummary(R.string.network_global);
                    return;
                }
            case 1:
                if (!this.mIsGlobalCdma) {
                    this.mButtonEnabledNetworks.setValue(Integer.toString(1));
                    this.mButtonEnabledNetworks.setSummary(R.string.network_2G);
                    return;
                } else {
                    this.mButtonEnabledNetworks.setValue(Integer.toString(10));
                    this.mButtonEnabledNetworks.setSummary(R.string.network_global);
                    return;
                }
            case 4:
            case 6:
            case 7:
                this.mButtonEnabledNetworks.setValue(Integer.toString(4));
                this.mButtonEnabledNetworks.setSummary(R.string.network_3G);
                return;
            case 5:
                this.mButtonEnabledNetworks.setValue(Integer.toString(5));
                this.mButtonEnabledNetworks.setSummary(R.string.network_1x);
                return;
            case 8:
                if (isWorldMode()) {
                    this.mButtonEnabledNetworks.setSummary(R.string.preferred_network_mode_lte_cdma_summary);
                    controlCdmaOptions(true);
                    controlGsmOptions(false);
                    return;
                } else {
                    this.mButtonEnabledNetworks.setValue(Integer.toString(8));
                    this.mButtonEnabledNetworks.setSummary(R.string.network_lte);
                    return;
                }
            case 9:
                if (isWorldMode()) {
                    this.mButtonEnabledNetworks.setSummary(R.string.preferred_network_mode_lte_gsm_umts_summary);
                    controlCdmaOptions(false);
                    controlGsmOptions(true);
                    return;
                }
                break;
            case 10:
                if (isWorldMode()) {
                    controlCdmaOptions(true);
                    controlGsmOptions(false);
                }
                this.mButtonEnabledNetworks.setValue(Integer.toString(10));
                if (this.mPhone.getPhoneType() == 2) {
                    this.mButtonEnabledNetworks.setSummary(R.string.network_global);
                    return;
                }
                ListPreference listPreference = this.mButtonEnabledNetworks;
                if (this.mShow4GForLTE) {
                    i = R.string.network_4G;
                }
                listPreference.setSummary(i);
                return;
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 19:
            case 20:
            case 21:
                break;
            case 18:
            default:
                String errMsg = "Invalid Network Mode (" + NetworkMode + "). Ignore.";
                loge(errMsg);
                this.mButtonEnabledNetworks.setSummary(errMsg);
                return;
        }
        if (!this.mIsGlobalCdma) {
            this.mButtonEnabledNetworks.setValue(Integer.toString(9));
            ListPreference listPreference2 = this.mButtonEnabledNetworks;
            if (this.mShow4GForLTE) {
                i = R.string.network_4G;
            }
            listPreference2.setSummary(i);
            return;
        }
        this.mButtonEnabledNetworks.setValue(Integer.toString(10));
        this.mButtonEnabledNetworks.setSummary(R.string.network_global);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 17:
                Boolean isChoiceYes = Boolean.valueOf(data.getBooleanExtra("exit_ecm_result", false));
                if (isChoiceYes.booleanValue()) {
                    this.mCdmaOptions.showDialog(this.mClickedPreference);
                }
                break;
        }
    }

    private static void log(String msg) {
        Log.d("NetworkSettings", msg);
    }

    private static void loge(String msg) {
        Log.e("NetworkSettings", msg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        finish();
        return true;
    }

    private boolean isWorldMode() {
        String[] configArray;
        boolean worldModeOn = false;
        TelephonyManager tm = (TelephonyManager) getSystemService("phone");
        String configString = getResources().getString(R.string.config_world_mode);
        if (!TextUtils.isEmpty(configString) && (configArray = configString.split(";")) != null && ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true")) || (configArray.length == 2 && !TextUtils.isEmpty(configArray[1]) && tm != null && configArray[1].equalsIgnoreCase(tm.getGroupIdLevel1())))) {
            worldModeOn = true;
        }
        log("isWorldMode=" + worldModeOn);
        return worldModeOn;
    }

    private void controlGsmOptions(boolean enable) {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            if (this.mGsmUmtsOptions == null) {
                this.mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet, this.mPhone.getSubId());
            }
            PreferenceScreen apnExpand = (PreferenceScreen) prefSet.findPreference("button_apn_key");
            PreferenceScreen operatorSelectionExpand = (PreferenceScreen) prefSet.findPreference("button_carrier_sel_key");
            PreferenceScreen carrierSettings = (PreferenceScreen) prefSet.findPreference("carrier_settings_key");
            if (apnExpand != null) {
                apnExpand.setEnabled(isWorldMode() || enable);
            }
            if (operatorSelectionExpand != null) {
                operatorSelectionExpand.setEnabled(enable);
            }
            if (carrierSettings != null) {
                prefSet.removePreference(carrierSettings);
            }
        }
    }

    private void controlCdmaOptions(boolean enable) {
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            if (enable && this.mCdmaOptions == null) {
                this.mCdmaOptions = new CdmaOptions(this, prefSet, this.mPhone);
            }
            CdmaSystemSelectListPreference systemSelect = (CdmaSystemSelectListPreference) prefSet.findPreference("cdma_system_select_key");
            if (systemSelect != null) {
                systemSelect.setEnabled(enable);
            }
        }
    }

    public SubscriptionInfo findRecordBySlotId(int slotId) {
        if (this.mActiveSubInfos != null) {
            int subInfoLength = this.mActiveSubInfos.size();
            for (int i = 0; i < subInfoLength; i++) {
                SubscriptionInfo sir = this.mActiveSubInfos.get(i);
                if (sir.getSimSlotIndex() == slotId) {
                    return sir;
                }
            }
        }
        return null;
    }
}
