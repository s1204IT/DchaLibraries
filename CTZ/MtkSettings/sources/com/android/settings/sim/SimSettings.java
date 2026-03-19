package com.android.settings.sim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.settings.sim.RadioPowerController;
import com.mediatek.settings.sim.RadioPowerPreference;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.TelephonyUtils;
import java.util.ArrayList;
import java.util.List;

public class SimSettings extends RestrictedSettingsFragment implements Indexable {
    private static final boolean ENG_LOAD;
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER;
    private List<SubscriptionInfo> mAvailableSubInfos;
    private Context mContext;
    private boolean mIsAirplaneModeOn;
    private int mNumSlots;
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener;
    private RadioPowerController mRadioController;
    private BroadcastReceiver mReceiver;
    private List<SubscriptionInfo> mSelectableSubInfos;
    private PreferenceScreen mSimCards;
    private SimHotSwapHandler mSimHotSwapHandler;
    private ISimManagementExt mSimManagementExt;
    private List<SubscriptionInfo> mSubInfoList;
    private SubscriptionManager mSubscriptionManager;

    static {
        ENG_LOAD = SystemProperties.get("ro.build.type").equals("eng") || Log.isLoggable("SimSettings", 3);
        SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
                ArrayList arrayList = new ArrayList();
                if (Utils.showSimCardTile(context)) {
                    SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
                    searchIndexableResource.xmlResId = R.xml.sim_settings;
                    arrayList.add(searchIndexableResource);
                }
                return arrayList;
            }
        };
    }

    public SimSettings() {
        super("no_config_sim");
        this.mAvailableSubInfos = null;
        this.mSubInfoList = null;
        this.mSelectableSubInfos = null;
        this.mSimCards = null;
        this.mIsAirplaneModeOn = false;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("SimSettings", "onReceive, action=" + action);
                if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                    SimSettings.this.handleAirplaneModeChange(intent);
                    return;
                }
                if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                    SimSettings.this.updateCellularDataValues();
                    return;
                }
                if (action.equals("android.telecom.action.PHONE_ACCOUNT_REGISTERED") || action.equals("android.telecom.action.PHONE_ACCOUNT_UNREGISTERED")) {
                    SimSettings.this.updateCallValues();
                    return;
                }
                if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE") || action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED")) {
                    SimSettings.this.updateActivitesCategory();
                    return;
                }
                if (action.equals("android.intent.action.PHONE_STATE")) {
                    SimSettings.this.updateActivitesCategory();
                } else if (action.equals("com.mediatek.intent.action.RADIO_STATE_CHANGED")) {
                    if (SimSettings.this.mRadioController.isRadioSwitchComplete(intent.getIntExtra("subId", -1))) {
                        SimSettings.this.handleRadioPowerSwitchComplete();
                    }
                }
            }
        };
        this.mOnSubscriptionsChangeListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                SimSettings.this.log("onSubscriptionsChanged:");
                SimSettings.this.updateSubscriptions();
            }
        };
    }

    @Override
    public int getMetricsCategory() {
        return 88;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mContext = getActivity();
        this.mSubscriptionManager = SubscriptionManager.from(getActivity());
        TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService("phone");
        addPreferencesFromResource(R.xml.sim_settings);
        this.mNumSlots = telephonyManager.getSimCount();
        this.mSimCards = (PreferenceScreen) findPreference("sim_cards");
        this.mAvailableSubInfos = new ArrayList(this.mNumSlots);
        this.mSelectableSubInfos = new ArrayList();
        SimSelectNotification.cancelNotification(getActivity());
        this.mRadioController = RadioPowerController.getInstance(getContext());
        initForSimStateChange();
        this.mSimManagementExt = UtilsExt.getSimManagementExt(getActivity());
        this.mSimManagementExt.onCreate();
        this.mSimManagementExt.initPlugin(this);
        logInEng("PrimarySim add option");
        this.mSimManagementExt.initPrimarySim(this);
    }

    private void updateSubscriptions() {
        this.mSubInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        for (int i = 0; i < this.mNumSlots; i++) {
            Preference preferenceFindPreference = this.mSimCards.findPreference("sim" + i);
            if (preferenceFindPreference instanceof SimPreference) {
                this.mSimCards.removePreference(preferenceFindPreference);
            }
        }
        this.mAvailableSubInfos.clear();
        this.mSelectableSubInfos.clear();
        for (int i2 = 0; i2 < this.mNumSlots; i2++) {
            SubscriptionInfo activeSubscriptionInfoForSimSlotIndex = this.mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(i2);
            SimPreference simPreference = new SimPreference(getPrefContext(), activeSubscriptionInfoForSimSlotIndex, i2);
            simPreference.setOrder(i2 - this.mNumSlots);
            if (activeSubscriptionInfoForSimSlotIndex == null) {
                simPreference.bindRadioPowerState(-1, false, false, this.mIsAirplaneModeOn);
            } else {
                int subscriptionId = activeSubscriptionInfoForSimSlotIndex.getSubscriptionId();
                boolean zIsRadioOn = TelephonyUtils.isRadioOn(subscriptionId, this.mContext);
                simPreference.bindRadioPowerState(subscriptionId, !this.mIsAirplaneModeOn && this.mRadioController.isRadioSwitchComplete(subscriptionId, zIsRadioOn), zIsRadioOn, this.mIsAirplaneModeOn);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("addPreference slot=");
            sb.append(i2);
            sb.append(", subInfo=");
            sb.append(activeSubscriptionInfoForSimSlotIndex == null ? "null" : activeSubscriptionInfoForSimSlotIndex);
            logInEng(sb.toString());
            this.mSimCards.addPreference(simPreference);
            this.mAvailableSubInfos.add(activeSubscriptionInfoForSimSlotIndex);
            if (activeSubscriptionInfoForSimSlotIndex != null) {
                this.mSelectableSubInfos.add(activeSubscriptionInfoForSimSlotIndex);
            }
        }
        updateActivitesCategory();
    }

    private void updateSimSlotValues() {
        int preferenceCount = this.mSimCards.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = this.mSimCards.getPreference(i);
            if (preference instanceof SimPreference) {
                ((SimPreference) preference).update();
            }
        }
    }

    private void updateActivitesCategory() {
        updateCellularDataValues();
        updateCallValues();
        updateSmsValues();
        this.mSimManagementExt.subChangeUpdatePrimarySIM();
    }

    private void updateSmsValues() {
        Preference preferenceFindPreference = findPreference("sim_sms");
        if (preferenceFindPreference == null) {
            return;
        }
        SubscriptionInfo defaultSmsSubscriptionInfo = this.mSubscriptionManager.getDefaultSmsSubscriptionInfo();
        preferenceFindPreference.setTitle(R.string.sms_messages_title);
        log("[updateSmsValues] mSubInfoList=" + this.mSubInfoList);
        if (defaultSmsSubscriptionInfo != null) {
            preferenceFindPreference.setSummary(defaultSmsSubscriptionInfo.getDisplayName());
            preferenceFindPreference.setEnabled(this.mSelectableSubInfos.size() > 1);
        } else if (defaultSmsSubscriptionInfo == null) {
            preferenceFindPreference.setSummary(R.string.sim_calls_ask_first_prefs_title);
            preferenceFindPreference.setEnabled(this.mSelectableSubInfos.size() >= 1);
            this.mSimManagementExt.updateDefaultSmsSummary(preferenceFindPreference);
        }
        this.mSimManagementExt.configSimPreferenceScreen(preferenceFindPreference, "sim_sms", this.mSelectableSubInfos.size());
        this.mSimManagementExt.setPrefSummary(preferenceFindPreference, "sim_sms");
    }

    private void updateCellularDataValues() {
        Preference preferenceFindPreference = findPreference("sim_cellular_data");
        if (preferenceFindPreference == null) {
            return;
        }
        SubscriptionInfo defaultDataSubscriptionInfo = this.mSubscriptionManager.getDefaultDataSubscriptionInfo();
        preferenceFindPreference.setTitle(R.string.cellular_data_title);
        log("[updateCellularDataValues] mSubInfoList=" + this.mSubInfoList);
        log("default subInfo=" + defaultDataSubscriptionInfo);
        SubscriptionInfo defaultSubId = this.mSimManagementExt.setDefaultSubId(getActivity(), defaultDataSubscriptionInfo, "sim_cellular_data");
        log("updated subInfo=" + defaultSubId);
        boolean z = this.mSelectableSubInfos.size() > 1;
        if (defaultSubId != null) {
            preferenceFindPreference.setSummary(defaultSubId.getDisplayName());
        } else if (defaultSubId == null) {
            preferenceFindPreference.setSummary(R.string.sim_selection_required_pref);
            z = this.mSelectableSubInfos.size() >= 1;
        }
        preferenceFindPreference.setEnabled(shouldEnableSimPref(z));
        this.mSimManagementExt.configSimPreferenceScreen(preferenceFindPreference, "sim_cellular_data", -1);
    }

    private void updateCallValues() {
        String string;
        Preference preferenceFindPreference = findPreference("sim_calls");
        if (preferenceFindPreference == null) {
            return;
        }
        TelecomManager telecomManagerFrom = TelecomManager.from(this.mContext);
        PhoneAccountHandle userSelectedOutgoingPhoneAccount = telecomManagerFrom.getUserSelectedOutgoingPhoneAccount();
        List<PhoneAccountHandle> callCapablePhoneAccounts = telecomManagerFrom.getCallCapablePhoneAccounts();
        preferenceFindPreference.setTitle(R.string.calls_title);
        PhoneAccountHandle defaultCallValue = this.mSimManagementExt.setDefaultCallValue(userSelectedOutgoingPhoneAccount);
        log("updateCallValues, PhoneAccountSize=" + callCapablePhoneAccounts.size() + ", phoneAccount=" + defaultCallValue);
        PhoneAccount phoneAccount = defaultCallValue == null ? null : telecomManagerFrom.getPhoneAccount(defaultCallValue);
        if (phoneAccount == null) {
            string = this.mContext.getResources().getString(R.string.sim_calls_ask_first_prefs_title);
        } else {
            string = (String) phoneAccount.getLabel();
        }
        preferenceFindPreference.setSummary(string);
        preferenceFindPreference.setEnabled(callCapablePhoneAccounts.size() > 1);
        this.mSimManagementExt.configSimPreferenceScreen(preferenceFindPreference, "sim_calls", callCapablePhoneAccounts.size());
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
        updateSubscriptions();
        removeItemsForTablet();
        this.mSimManagementExt.onResume(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
        this.mSimManagementExt.onPause();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        Context context = this.mContext;
        Intent intent = new Intent(context, (Class<?>) SimDialogActivity.class);
        intent.addFlags(268435456);
        if (preference instanceof SimPreference) {
            Intent intent2 = new Intent(context, (Class<?>) SimPreferenceDialog.class);
            intent2.putExtra("slot_id", ((SimPreference) preference).getSlotId());
            startActivity(intent2);
        } else if (findPreference("sim_cellular_data") == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 0);
            context.startActivity(intent);
        } else if (findPreference("sim_calls") == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 1);
            context.startActivity(intent);
        } else if (findPreference("sim_sms") == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 2);
            context.startActivity(intent);
        } else {
            if (findPreference("primary_SIM_key") == preference) {
                log("host onPreferenceTreeClick 1");
                this.mSimManagementExt.onPreferenceClick(context);
                return true;
            }
            this.mSimManagementExt.handleEvent(this, context, preference);
        }
        return true;
    }

    private class SimPreference extends RadioPowerPreference {
        Context mContext;
        private int mSlotId;
        private SubscriptionInfo mSubInfoRecord;

        public SimPreference(Context context, SubscriptionInfo subscriptionInfo, int i) {
            super(context);
            this.mContext = context;
            this.mSubInfoRecord = subscriptionInfo;
            this.mSlotId = i;
            setKey("sim" + this.mSlotId);
            update();
        }

        public void update() {
            Resources resources = this.mContext.getResources();
            setTitle(String.format(this.mContext.getResources().getString(R.string.sim_editor_title), Integer.valueOf(this.mSlotId + 1)));
            if (this.mSubInfoRecord != null) {
                String phoneNumber = SimSettings.this.getPhoneNumber(this.mSubInfoRecord);
                SimSettings.this.logInEng("slot=" + this.mSlotId + ", phoneNum=" + phoneNumber);
                if (TextUtils.isEmpty(phoneNumber)) {
                    setSummary(this.mSubInfoRecord.getDisplayName());
                } else {
                    setSummary(((Object) this.mSubInfoRecord.getDisplayName()) + " - " + ((Object) PhoneNumberUtils.createTtsSpannable(phoneNumber)));
                    setEnabled(true);
                }
                setIcon(new BitmapDrawable(resources, this.mSubInfoRecord.createIconBitmap(this.mContext)));
                int subscriptionId = this.mSubInfoRecord.getSubscriptionId();
                boolean zIsRadioOn = TelephonyUtils.isRadioOn(subscriptionId, getContext());
                boolean zIsRadioSwitchComplete = SimSettings.this.mRadioController.isRadioSwitchComplete(subscriptionId, zIsRadioOn);
                setRadioEnabled(!SimSettings.this.mIsAirplaneModeOn && zIsRadioSwitchComplete);
                if (zIsRadioSwitchComplete) {
                    setRadioOn(!SimSettings.this.mIsAirplaneModeOn && zIsRadioOn);
                    return;
                }
                return;
            }
            setSummary(R.string.sim_slot_empty);
            setFragment(null);
            setEnabled(false);
        }

        private int getSlotId() {
            return this.mSlotId;
        }
    }

    private String getPhoneNumber(SubscriptionInfo subscriptionInfo) {
        return ((TelephonyManager) this.mContext.getSystemService("phone")).getLine1Number(subscriptionInfo.getSubscriptionId());
    }

    private void log(String str) {
        Log.d("SimSettings", str);
    }

    private void initForSimStateChange() {
        this.mSimHotSwapHandler = new SimHotSwapHandler(getActivity().getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                if (SimSettings.this.getActivity() != null) {
                    SimSettings.this.log("onSimHotSwap, finish Activity.");
                    SimSettings.this.getActivity().finish();
                }
            }
        });
        this.mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getActivity().getApplicationContext());
        logInEng("initForSimStateChange, airplaneMode=" + this.mIsAirplaneModeOn);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
        intentFilter.addAction("com.mediatek.intent.action.RADIO_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction("android.telecom.action.PHONE_ACCOUNT_REGISTERED");
        intentFilter.addAction("android.telecom.action.PHONE_ACCOUNT_UNREGISTERED");
        getActivity().registerReceiver(this.mReceiver, intentFilter);
    }

    private void handleRadioPowerSwitchComplete() {
        logInEng("handleRadioPowerSwitchComplete");
        updateSimSlotValues();
        updateActivitesCategory();
    }

    private void handleAirplaneModeChange(Intent intent) {
        this.mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
        Log.d("SimSettings", "airplaneMode=" + this.mIsAirplaneModeOn);
        updateSimSlotValues();
        updateActivitesCategory();
        removeItemsForTablet();
        this.mSimManagementExt.updatePrefState();
    }

    private void removeItemsForTablet() {
        if (FeatureOption.MTK_PRODUCT_IS_TABLET) {
            Preference preferenceFindPreference = findPreference("sim_calls");
            Preference preferenceFindPreference2 = findPreference("sim_sms");
            Preference preferenceFindPreference3 = findPreference("sim_cellular_data");
            PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("sim_activities");
            TelephonyManager telephonyManagerFrom = TelephonyManager.from(getActivity());
            if (!telephonyManagerFrom.isSmsCapable() && preferenceFindPreference2 != null) {
                preferenceCategory.removePreference(preferenceFindPreference2);
            }
            if (!telephonyManagerFrom.isMultiSimEnabled() && preferenceFindPreference3 != null && preferenceFindPreference2 != null) {
                preferenceCategory.removePreference(preferenceFindPreference3);
                preferenceCategory.removePreference(preferenceFindPreference2);
            }
            if (!telephonyManagerFrom.isVoiceCapable() && preferenceFindPreference != null) {
                preferenceCategory.removePreference(preferenceFindPreference);
            }
        }
    }

    @Override
    public void onDestroy() {
        logInEng("onDestroy()");
        getActivity().unregisterReceiver(this.mReceiver);
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        this.mSimManagementExt.onDestroy();
        super.onDestroy();
    }

    private boolean shouldEnableSimPref(boolean z) {
        int i;
        String str = SystemProperties.get("ril.cdma.inecmmode", "false");
        boolean z2 = str != null && str.contains("true");
        boolean zIsCapabilitySwitching = TelephonyUtils.isCapabilitySwitching();
        boolean zIsInCall = TelecomManager.from(this.mContext).isInCall();
        if (SystemProperties.getInt("ro.vendor.mtk_non_dsda_rsim_support", 0) == 1) {
            i = SystemProperties.getInt("vendor.gsm.prefered.rsim.slot", -1);
        } else {
            i = -1;
        }
        log("defaultState=" + z + ", capSwitching=" + zIsCapabilitySwitching + ", airplaneModeOn=" + this.mIsAirplaneModeOn + ", inCall=" + zIsInCall + ", ecbMode=" + str + ", rsimPhoneId=" + i);
        return (!z || zIsCapabilitySwitching || this.mIsAirplaneModeOn || zIsInCall || z2 || i != -1) ? false : true;
    }

    private void logInEng(String str) {
        if (ENG_LOAD) {
            Log.d("SimSettings", str);
        }
    }
}
