package com.android.settings.sim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
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
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.settings.FeatureOption;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimManagementExt;
import com.mediatek.settings.sim.RadioPowerController;
import com.mediatek.settings.sim.RadioPowerPreference;
import com.mediatek.settings.sim.SimHotSwapHandler;
import com.mediatek.settings.sim.TelephonyUtils;
import java.util.ArrayList;
import java.util.List;

public class SimSettings extends RestrictedSettingsFragment implements Indexable {
    private static final boolean ENG_LOAD;
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            ArrayList<SearchIndexableResource> result = new ArrayList<>();
            if (Utils.showSimCardTile(context)) {
                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.sim_settings;
                result.add(sir);
            }
            return result;
        }
    };
    private List<SubscriptionInfo> mAvailableSubInfos;
    private int[] mCallState;
    private Context mContext;
    private boolean mIsAirplaneModeOn;
    private ISettingsMiscExt mMiscExt;
    private int mNumSlots;
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener;
    private int mPhoneCount;
    private PhoneStateListener[] mPhoneStateListener;
    private RadioPowerController mRadioController;
    private BroadcastReceiver mReceiver;
    private List<SubscriptionInfo> mSelectableSubInfos;
    private PreferenceScreen mSimCards;
    private SimHotSwapHandler mSimHotSwapHandler;
    private ISimManagementExt mSimManagementExt;
    private List<SubscriptionInfo> mSubInfoList;
    private SubscriptionManager mSubscriptionManager;
    private ITelephonyEx mTelephonyEx;

    public SimSettings() {
        super("no_config_sim");
        this.mAvailableSubInfos = null;
        this.mSubInfoList = null;
        this.mSelectableSubInfos = null;
        this.mSimCards = null;
        this.mPhoneCount = TelephonyManager.getDefault().getPhoneCount();
        this.mCallState = new int[this.mPhoneCount];
        this.mPhoneStateListener = new PhoneStateListener[this.mPhoneCount];
        this.mOnSubscriptionsChangeListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                SimSettings.this.log("onSubscriptionsChanged:");
                SimSettings.this.updateSubscriptions();
            }
        };
        this.mIsAirplaneModeOn = false;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("SimSettings", "mReceiver action = " + action);
                if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                    SimSettings.this.handleAirplaneModeChange(intent);
                    return;
                }
                if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                    SimSettings.this.updateCellularDataValues();
                    return;
                }
                if (action.equals("android.telecom.action.DEFAULT_ACCOUNT_CHANGED") || action.equals("android.telecom.action.PHONE_ACCOUNT_CHANGED")) {
                    SimSettings.this.updateCallValues();
                    return;
                }
                if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE") || action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED")) {
                    SimSettings.this.updateActivitesCategory();
                    return;
                }
                if (action.equals("android.intent.action.PHONE_STATE")) {
                    SimSettings.this.updateActivitesCategory();
                } else {
                    if (!action.equals("android.intent.action.RADIO_STATE_CHANGED")) {
                        return;
                    }
                    int subId = intent.getIntExtra("subId", -1);
                    if (!SimSettings.this.mRadioController.isRadioSwitchComplete(subId)) {
                        return;
                    }
                    SimSettings.this.handleRadioPowerSwitchComplete();
                }
            }
        };
    }

    @Override
    protected int getMetricsCategory() {
        return 88;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mContext = getActivity();
        this.mSubscriptionManager = SubscriptionManager.from(getActivity());
        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService("phone");
        addPreferencesFromResource(R.xml.sim_settings);
        this.mNumSlots = tm.getSimCount();
        this.mSimCards = (PreferenceScreen) findPreference("sim_cards");
        this.mAvailableSubInfos = new ArrayList(this.mNumSlots);
        this.mSelectableSubInfos = new ArrayList();
        SimSelectNotification.cancelNotification(getActivity());
        initForSimStateChange();
        this.mSimManagementExt = UtilsExt.getSimManagmentExtPlugin(getActivity());
        this.mMiscExt = UtilsExt.getMiscPlugin(getActivity());
        this.mRadioController = RadioPowerController.getInstance(getContext());
    }

    public void updateSubscriptions() {
        this.mSubInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        for (int i = 0; i < this.mNumSlots; i++) {
            Preference pref = this.mSimCards.findPreference("sim" + i);
            if (pref instanceof SimPreference) {
                this.mSimCards.removePreference(pref);
            }
        }
        this.mAvailableSubInfos.clear();
        this.mSelectableSubInfos.clear();
        for (int i2 = 0; i2 < this.mNumSlots; i2++) {
            SubscriptionInfo sir = this.mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(i2);
            SimPreference simPreference = new SimPreference(getPrefContext(), sir, i2);
            simPreference.setOrder(i2 - this.mNumSlots);
            if (sir != null) {
                int subId = sir.getSubscriptionId();
                simPreference.bindRadioPowerState(subId, this.mRadioController.isRadioSwitchComplete(subId));
            } else {
                simPreference.bindRadioPowerState(-1, this.mRadioController.isRadioSwitchComplete(-1));
            }
            logInEng("addPreference slot " + i2);
            this.mSimCards.addPreference(simPreference);
            this.mAvailableSubInfos.add(sir);
            if (sir != null) {
                this.mSelectableSubInfos.add(sir);
            }
        }
        updateAllOptions();
    }

    private void updateAllOptions() {
        updateSimSlotValues();
        updateActivitesCategory();
    }

    private void updateSimSlotValues() {
        int prefSize = this.mSimCards.getPreferenceCount();
        for (int i = 0; i < prefSize; i++) {
            Preference pref = this.mSimCards.getPreference(i);
            if (pref instanceof SimPreference) {
                ((SimPreference) pref).update();
            }
        }
    }

    public void updateActivitesCategory() {
        updateCellularDataValues();
        updateCallValues();
        updateSmsValues();
    }

    private void updateSmsValues() {
        boolean enabled = true;
        Preference simPref = findPreference("sim_sms");
        if (simPref == null) {
            return;
        }
        SubscriptionInfo sir = this.mSubscriptionManager.getDefaultSmsSubscriptionInfo();
        simPref.setTitle(R.string.sms_messages_title);
        log("[updateSmsValues] mSubInfoList=" + this.mSubInfoList);
        SubscriptionInfo sir2 = this.mSimManagementExt.setDefaultSubId(getActivity(), sir, "sim_sms");
        if (sir2 != null) {
            simPref.setSummary(sir2.getDisplayName());
        } else if (sir2 == null) {
            simPref.setSummary(R.string.sim_calls_ask_first_prefs_title);
            this.mSimManagementExt.updateDefaultSmsSummary(simPref);
        }
        if (sir2 == null) {
            if (this.mSelectableSubInfos.size() < 1) {
                enabled = false;
            }
        } else if (this.mSelectableSubInfos.size() <= 1) {
            enabled = false;
        }
        simPref.setEnabled(enabled);
        this.mSimManagementExt.configSimPreferenceScreen(simPref, "sim_sms", this.mSelectableSubInfos.size());
        this.mSimManagementExt.setPrefSummary(simPref, "sim_sms");
    }

    public void updateCellularDataValues() {
        boolean defaultState = true;
        Preference simPref = findPreference("sim_cellular_data");
        if (simPref == null) {
            return;
        }
        SubscriptionInfo sir = this.mSubscriptionManager.getDefaultDataSubscriptionInfo();
        simPref.setTitle(R.string.cellular_data_title);
        log("[updateCellularDataValues] mSubInfoList=" + this.mSubInfoList);
        SubscriptionInfo sir2 = this.mSimManagementExt.setDefaultSubId(getActivity(), sir, "sim_cellular_data");
        if (sir2 != null) {
            simPref.setSummary(sir2.getDisplayName());
        } else if (sir2 == null) {
            simPref.setSummary(R.string.sim_selection_required_pref);
        }
        if (sir2 == null) {
            if (this.mSelectableSubInfos.size() < 1) {
                defaultState = false;
            }
        } else if (this.mSelectableSubInfos.size() <= 1) {
            defaultState = false;
        }
        simPref.setEnabled(shouldEnableSimPref(defaultState));
        this.mSimManagementExt.configSimPreferenceScreen(simPref, "sim_cellular_data", -1);
    }

    public void updateCallValues() {
        String string;
        Preference simPref = findPreference("sim_calls");
        if (simPref == null) {
            return;
        }
        TelecomManager telecomManager = TelecomManager.from(this.mContext);
        PhoneAccountHandle phoneAccount = telecomManager.getUserSelectedOutgoingPhoneAccount();
        List<PhoneAccountHandle> allPhoneAccounts = telecomManager.getCallCapablePhoneAccounts();
        PhoneAccountHandle phoneAccount2 = this.mSimManagementExt.setDefaultCallValue(phoneAccount);
        log("updateCallValues allPhoneAccounts size = " + allPhoneAccounts.size() + " phoneAccount =" + phoneAccount2);
        simPref.setTitle(R.string.calls_title);
        PhoneAccount defaultAccount = phoneAccount2 != null ? telecomManager.getPhoneAccount(phoneAccount2) : null;
        if (defaultAccount == null) {
            string = this.mContext.getResources().getString(R.string.sim_calls_ask_first_prefs_title);
        } else {
            string = (String) defaultAccount.getLabel();
        }
        simPref.setSummary(string);
        simPref.setEnabled(allPhoneAccounts.size() > 1);
        this.mSimManagementExt.configSimPreferenceScreen(simPref, "sim_calls", allPhoneAccounts.size());
        if (SystemProperties.get("ro.cmcc_light_cust_support").equals("1")) {
            log("Op01 open market:set call enable if size >= 1");
            simPref.setEnabled(allPhoneAccounts.size() >= 1);
        }
        this.mSimManagementExt.setPrefSummary(simPref, "sim_calls");
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
        updateSubscriptions();
        removeItemsForTablet();
        customizeSimDisplay();
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
        intent.addFlags(268468224);
        if (preference instanceof SimPreference) {
            Intent newIntent = new Intent(context, (Class<?>) SimPreferenceDialog.class);
            newIntent.putExtra("slot_id", ((SimPreference) preference).getSlotId());
            startActivity(newIntent);
        } else if (findPreference("sim_cellular_data") == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 0);
            context.startActivity(intent);
        } else if (findPreference("sim_calls") == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 1);
            context.startActivity(intent);
        } else if (findPreference("sim_sms") == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 2);
            context.startActivity(intent);
        }
        return true;
    }

    private class SimPreference extends RadioPowerPreference {
        Context mContext;
        private int mSlotId;
        private SubscriptionInfo mSubInfoRecord;

        public SimPreference(Context context, SubscriptionInfo subInfoRecord, int slotId) {
            super(context);
            this.mContext = context;
            this.mSubInfoRecord = subInfoRecord;
            this.mSlotId = slotId;
            setKey("sim" + this.mSlotId);
            update();
        }

        public void update() {
            Resources res = this.mContext.getResources();
            setTitle(String.format(this.mContext.getResources().getString(R.string.sim_editor_title), Integer.valueOf(this.mSlotId + 1)));
            customizePreferenceTitle();
            if (this.mSubInfoRecord != null) {
                String phoneNum = SimSettings.this.getPhoneNumber(this.mSubInfoRecord);
                SimSettings.this.logInEng("phoneNum = " + phoneNum);
                if (TextUtils.isEmpty(phoneNum)) {
                    setSummary(this.mSubInfoRecord.getDisplayName());
                } else {
                    setSummary(this.mSubInfoRecord.getDisplayName() + " - " + PhoneNumberUtils.createTtsSpannable(phoneNum));
                    setEnabled(true);
                }
                setIcon(new BitmapDrawable(res, this.mSubInfoRecord.createIconBitmap(this.mContext)));
                int subId = this.mSubInfoRecord.getSubscriptionId();
                setRadioEnabled(SimSettings.this.mIsAirplaneModeOn ? false : SimSettings.this.mRadioController.isRadioSwitchComplete(subId));
                if (!SimSettings.this.mRadioController.isRadioSwitchComplete(subId)) {
                    return;
                }
                setRadioOn(TelephonyUtils.isRadioOn(subId, getContext()));
                return;
            }
            setSummary(R.string.sim_slot_empty);
            setFragment(null);
            setEnabled(false);
        }

        public int getSlotId() {
            return this.mSlotId;
        }

        private void customizePreferenceTitle() {
            int subId = -1;
            if (this.mSubInfoRecord != null) {
                subId = this.mSubInfoRecord.getSubscriptionId();
            }
            setTitle(String.format(SimSettings.this.mMiscExt.customizeSimDisplayString(this.mContext.getResources().getString(R.string.sim_editor_title), subId), Integer.valueOf(this.mSlotId + 1)));
        }
    }

    public String getPhoneNumber(SubscriptionInfo info) {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        return tm.getLine1Number(info.getSubscriptionId());
    }

    public void log(String s) {
        Log.d("SimSettings", s);
    }

    static {
        ENG_LOAD = SystemProperties.get("ro.build.type").equals("eng") ? true : Log.isLoggable("SimSettings", 3);
    }

    private void initForSimStateChange() {
        this.mTelephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        this.mSimHotSwapHandler = new SimHotSwapHandler(getActivity().getApplicationContext());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                if (SimSettings.this.getActivity() == null) {
                    return;
                }
                SimSettings.this.log("onSimHotSwap, finish Activity~~");
                SimSettings.this.getActivity().finish();
            }
        });
        this.mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(getActivity().getApplicationContext());
        logInEng("init()... airplane mode is: " + this.mIsAirplaneModeOn);
        IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        intentFilter.addAction("android.telecom.action.DEFAULT_ACCOUNT_CHANGED");
        intentFilter.addAction("android.telecom.action.PHONE_ACCOUNT_CHANGED");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
        intentFilter.addAction("android.intent.action.RADIO_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        getActivity().registerReceiver(this.mReceiver, intentFilter);
    }

    public void handleRadioPowerSwitchComplete() {
        if (isResumed()) {
            updateSimSlotValues();
        }
        this.mSimManagementExt.showChangeDataConnDialog(this, isResumed());
    }

    public void handleAirplaneModeChange(Intent intent) {
        this.mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
        Log.d("SimSettings", "airplane mode is = " + this.mIsAirplaneModeOn);
        updateSimSlotValues();
        updateActivitesCategory();
        removeItemsForTablet();
    }

    private void removeItemsForTablet() {
        if (!FeatureOption.MTK_PRODUCT_IS_TABLET) {
            return;
        }
        Preference sim_call_Pref = findPreference("sim_calls");
        Preference sim_sms_Pref = findPreference("sim_sms");
        Preference sim_data_Pref = findPreference("sim_cellular_data");
        PreferenceCategory mPreferenceCategoryActivities = (PreferenceCategory) findPreference("sim_activities");
        TelephonyManager tm = TelephonyManager.from(getActivity());
        if (!tm.isSmsCapable() && sim_sms_Pref != null) {
            mPreferenceCategoryActivities.removePreference(sim_sms_Pref);
        }
        if (!tm.isMultiSimEnabled() && sim_data_Pref != null && sim_sms_Pref != null) {
            mPreferenceCategoryActivities.removePreference(sim_data_Pref);
            mPreferenceCategoryActivities.removePreference(sim_sms_Pref);
        }
        if (tm.isVoiceCapable() || sim_call_Pref == null) {
            return;
        }
        mPreferenceCategoryActivities.removePreference(sim_call_Pref);
    }

    @Override
    public void onDestroy() {
        logInEng("onDestroy()");
        getActivity().unregisterReceiver(this.mReceiver);
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
        super.onDestroy();
    }

    private void customizeSimDisplay() {
        if (this.mSimCards != null) {
            this.mSimCards.setTitle(this.mMiscExt.customizeSimDisplayString(getString(R.string.sim_settings_title), -1));
        }
        getActivity().setTitle(this.mMiscExt.customizeSimDisplayString(getString(R.string.sim_settings_title), -1));
    }

    private boolean shouldEnableSimPref(boolean defaultState) {
        String ecbMode = SystemProperties.get("ril.cdma.inecmmode", "false");
        boolean isInEcbMode = false;
        if (ecbMode != null && ecbMode.contains("true")) {
            isInEcbMode = true;
        }
        boolean capSwitching = TelephonyUtils.isCapabilitySwitching();
        boolean inCall = TelecomManager.from(this.mContext).isInCall();
        log("defaultState :" + defaultState + ", capSwitching :" + capSwitching + ", airplaneModeOn :" + this.mIsAirplaneModeOn + ", inCall :" + inCall + ", ecbMode: " + ecbMode);
        return (!defaultState || capSwitching || this.mIsAirplaneModeOn || inCall || isInEcbMode) ? false : true;
    }

    public void logInEng(String s) {
        if (!ENG_LOAD) {
            return;
        }
        Log.d("SimSettings", s);
    }
}
