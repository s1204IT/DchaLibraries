package com.android.settings.deviceinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.telephony.CarrierConfigManager;
import android.telephony.CellBroadcastMessage;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaSimStatus;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.sim.SimHotSwapHandler;
import java.util.List;

public class SimStatus extends SettingsPreferenceFragment {
    private CarrierConfigManager mCarrierConfigManager;
    private CdmaSimStatus mCdmaSimStatus;
    private String mDefaultText;
    private ListView mListView;
    private PhoneStateListener mPhoneStateListener;
    private Resources mRes;
    private List<SubscriptionInfo> mSelectableSubInfos;
    private boolean mShowICCID;
    private boolean mShowLatestAreaInfo;
    private Preference mSignalStrength;
    private SimHotSwapHandler mSimHotSwapHandler;
    private SubscriptionInfo mSir;
    private TabHost mTabHost;
    private TabWidget mTabWidget;
    private TelephonyManager mTelephonyManager;
    private Phone mPhone = null;
    private BroadcastReceiver mAreaInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras;
            CellBroadcastMessage cbMessage;
            String action = intent.getAction();
            if (!"android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED".equals(action) || (extras = intent.getExtras()) == null || (cbMessage = (CellBroadcastMessage) extras.get("message")) == null || cbMessage.getServiceCategory() != 50) {
                return;
            }
            String latestAreaInfo = cbMessage.getMessageBody();
            SimStatus.this.updateAreaInfo(latestAreaInfo);
        }
    };
    private TabHost.OnTabChangeListener mTabListener = new TabHost.OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            Log.d("SimStatus", "onTabChange, tabId = " + tabId);
            int slotId = Integer.parseInt(tabId);
            SimStatus.this.mSir = (SubscriptionInfo) SimStatus.this.mSelectableSubInfos.get(slotId);
            SimStatus.this.mCdmaSimStatus.setSubscriptionInfo(SimStatus.this.mSir);
            SimStatus.this.updatePhoneInfos();
            SimStatus.this.mTelephonyManager.listen(SimStatus.this.mPhoneStateListener, 321);
            SimStatus.this.updateDataState();
            SimStatus.this.updateNetworkType();
            SimStatus.this.updatePreference();
        }
    };
    private TabHost.TabContentFactory mEmptyTabContent = new TabHost.TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(SimStatus.this.mTabHost.getContext());
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        this.mCarrierConfigManager = (CarrierConfigManager) getSystemService("carrier_config");
        this.mSelectableSubInfos = SubscriptionManager.from(getContext()).getActiveSubscriptionInfoList();
        addPreferencesFromResource(R.xml.device_info_sim_status);
        this.mRes = getResources();
        this.mDefaultText = this.mRes.getString(R.string.device_info_default);
        this.mSignalStrength = findPreference("signal_strength");
        this.mCdmaSimStatus = new CdmaSimStatus(this, null);
        updatePhoneInfos();
        this.mSimHotSwapHandler = new SimHotSwapHandler(getActivity());
        this.mSimHotSwapHandler.registerOnSimHotSwap(new SimHotSwapHandler.OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                Log.d("SimStatus", "onSimHotSwap, finish Activity~~");
                SimStatus.this.finish();
            }
        });
        customizeTitle();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (this.mSelectableSubInfos == null) {
            this.mSir = null;
            this.mCdmaSimStatus.setSubscriptionInfo(this.mSir);
        } else {
            this.mSir = this.mSelectableSubInfos.size() > 0 ? this.mSelectableSubInfos.get(0) : null;
            this.mCdmaSimStatus.setSubscriptionInfo(this.mSir);
            if (this.mSelectableSubInfos.size() > 1) {
                View view = inflater.inflate(R.layout.icc_lock_tabs, container, false);
                ViewGroup prefs_container = (ViewGroup) view.findViewById(R.id.prefs_container);
                Utils.prepareCustomPreferencesList(container, view, prefs_container, false);
                View prefs = super.onCreateView(inflater, prefs_container, savedInstanceState);
                prefs_container.addView(prefs);
                this.mTabHost = (TabHost) view.findViewById(android.R.id.tabhost);
                this.mTabWidget = (TabWidget) view.findViewById(android.R.id.tabs);
                this.mListView = (ListView) view.findViewById(android.R.id.list);
                this.mTabHost.setup();
                this.mTabHost.setOnTabChangedListener(this.mTabListener);
                this.mTabHost.clearAllTabs();
                for (int i = 0; i < this.mSelectableSubInfos.size(); i++) {
                    this.mTabHost.addTab(buildTabSpec(String.valueOf(i), String.valueOf(this.mSelectableSubInfos.get(i).getDisplayName())));
                }
                return view;
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updatePhoneInfos();
    }

    @Override
    protected int getMetricsCategory() {
        return 43;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mPhone == null) {
            return;
        }
        updatePreference();
        updateSignalStrength(this.mPhone.getSignalStrength());
        updateServiceState(this.mPhone.getServiceState());
        updateDataState();
        this.mTelephonyManager.listen(this.mPhoneStateListener, 321);
        if (!this.mShowLatestAreaInfo) {
            return;
        }
        getContext().registerReceiver(this.mAreaInfoReceiver, new IntentFilter("android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED"), "android.permission.RECEIVE_EMERGENCY_BROADCAST", null);
        Intent getLatestIntent = new Intent("android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO");
        getContext().sendBroadcastAsUser(getLatestIntent, UserHandle.ALL, "android.permission.RECEIVE_EMERGENCY_BROADCAST");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mPhone != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
        if (!this.mShowLatestAreaInfo) {
            return;
        }
        getContext().unregisterReceiver(this.mAreaInfoReceiver);
    }

    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref == null) {
            return;
        }
        getPreferenceScreen().removePreference(pref);
    }

    private void setSummaryText(String key, String text) {
        if (TextUtils.isEmpty(text)) {
            text = this.mDefaultText;
        }
        Preference preference = findPreference(key);
        if (preference == null) {
            return;
        }
        preference.setSummary(text);
    }

    public void updateNetworkType() {
        String networktype = null;
        int subId = this.mSir.getSubscriptionId();
        int actualDataNetworkType = this.mTelephonyManager.getDataNetworkType(this.mSir.getSubscriptionId());
        int actualVoiceNetworkType = this.mTelephonyManager.getVoiceNetworkType(this.mSir.getSubscriptionId());
        Log.d("SimStatus", "updateNetworkType(), actualDataNetworkType = " + actualDataNetworkType + "actualVoiceNetworkType = " + actualVoiceNetworkType);
        if (actualDataNetworkType != 0) {
            TelephonyManager telephonyManager = this.mTelephonyManager;
            networktype = TelephonyManager.getNetworkTypeName(actualDataNetworkType);
        } else if (actualVoiceNetworkType != 0) {
            TelephonyManager telephonyManager2 = this.mTelephonyManager;
            networktype = TelephonyManager.getNetworkTypeName(actualVoiceNetworkType);
        }
        boolean show4GForLTE = false;
        ISettingsMiscExt ext = UtilsExt.getMiscPlugin(getContext());
        try {
            Context con = getActivity().createPackageContext("com.android.systemui", 0);
            int id = con.getResources().getIdentifier("config_show4GForLTE", "bool", "com.android.systemui");
            show4GForLTE = con.getResources().getBoolean(id);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("SimStatus", "NameNotFoundException for show4GFotLTE");
        }
        if (networktype != null && networktype.equals("LTE") && show4GForLTE) {
            networktype = "4G";
        }
        String networktype2 = ext.getNetworktypeString(networktype, subId);
        setSummaryText("network_type", networktype2);
        this.mCdmaSimStatus.updateNetworkType("network_type", networktype2);
    }

    public void updateDataState() {
        int state = DefaultPhoneNotifier.convertDataState(this.mPhone.getDataConnectionState());
        String display = this.mRes.getString(R.string.radioInfo_unknown);
        switch (state) {
            case DefaultWfcSettingsExt.RESUME:
                display = this.mRes.getString(R.string.radioInfo_data_disconnected);
                break;
            case DefaultWfcSettingsExt.PAUSE:
                display = this.mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case DefaultWfcSettingsExt.CREATE:
                display = this.mRes.getString(R.string.radioInfo_data_connected);
                break;
            case DefaultWfcSettingsExt.DESTROY:
                display = this.mRes.getString(R.string.radioInfo_data_suspended);
                break;
        }
        setSummaryText("data_state", display);
    }

    public void updateServiceState(ServiceState serviceState) {
        int state = serviceState.getState();
        String display = this.mRes.getString(R.string.radioInfo_unknown);
        Log.d("SimStatus", "updateServiceState : " + serviceState);
        switch (state) {
            case DefaultWfcSettingsExt.RESUME:
                display = this.mRes.getString(R.string.radioInfo_service_in);
                if (this.mPhone != null) {
                    updateSignalStrength(this.mPhone.getSignalStrength());
                }
                break;
            case DefaultWfcSettingsExt.PAUSE:
                this.mSignalStrength.setSummary("0");
            case DefaultWfcSettingsExt.CREATE:
                display = this.mRes.getString(R.string.radioInfo_service_out);
                break;
            case DefaultWfcSettingsExt.DESTROY:
                display = this.mRes.getString(R.string.radioInfo_service_off);
                this.mSignalStrength.setSummary("0");
                break;
        }
        setSummaryText("service_state", display);
        if (serviceState.getRoaming()) {
            setSummaryText("roaming_state", this.mRes.getString(R.string.radioInfo_roaming_in));
        } else {
            setSummaryText("roaming_state", this.mRes.getString(R.string.radioInfo_roaming_not));
        }
        setSummaryText("operator_name", serviceState.getOperatorAlphaLong());
        this.mCdmaSimStatus.setServiceState(serviceState);
    }

    public void updateAreaInfo(String areaInfo) {
        if (areaInfo == null) {
            return;
        }
        setSummaryText("latest_area_info", areaInfo);
    }

    void updateSignalStrength(SignalStrength signalStrength) {
        if (this.mSignalStrength == null) {
            return;
        }
        int state = this.mPhone.getServiceState().getState();
        if (1 == state || 3 == state) {
            this.mSignalStrength.setSummary("0");
            return;
        }
        int signalDbm = signalStrength.getDbm();
        int signalAsu = signalStrength.getAsuLevel();
        if (-1 == signalDbm) {
            signalDbm = 0;
        }
        if (-1 == signalAsu) {
            signalAsu = 0;
        }
        Log.d("SimStatus", "updateSignalStrength(), signalDbm = " + signalDbm + " signalAsu = " + signalAsu);
        this.mSignalStrength.setSummary(this.mRes.getString(R.string.sim_signal_strength, Integer.valueOf(signalDbm), Integer.valueOf(signalAsu)));
        this.mCdmaSimStatus.updateSignalStrength(signalStrength, this.mSignalStrength);
    }

    public void updatePreference() {
        int titleResId;
        if (this.mPhone.getPhoneType() != 2 && "br".equals(this.mTelephonyManager.getSimCountryIso(this.mSir.getSubscriptionId()))) {
            this.mShowLatestAreaInfo = true;
        }
        PersistableBundle carrierConfig = this.mCarrierConfigManager.getConfigForSubId(this.mSir.getSubscriptionId());
        this.mShowICCID = carrierConfig.getBoolean("show_iccid_in_sim_status_bool");
        String rawNumber = this.mTelephonyManager.getLine1Number(this.mSir.getSubscriptionId());
        String formattedNumber = null;
        if (!TextUtils.isEmpty(rawNumber)) {
            formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
        }
        setSummaryText("number", formattedNumber);
        String deviceId = this.mPhone.getPhoneType() != 2 ? this.mPhone.getImei() : this.mPhone.getMeid();
        setSummaryText("imei", deviceId);
        setSummaryText("imei_sv", this.mPhone.getDeviceSvn());
        if (!this.mShowICCID) {
            removePreferenceFromScreen("iccid");
        } else {
            String iccid = this.mTelephonyManager.getSimSerialNumber(this.mSir.getSubscriptionId());
            setSummaryText("iccid", iccid);
        }
        Preference preference = findPreference("imei");
        if (preference != null) {
            if (this.mPhone.getPhoneType() == 2) {
                titleResId = R.string.status_meid_number;
            } else {
                titleResId = R.string.status_imei;
            }
            preference.setTitle(titleResId);
        }
        if (!this.mShowLatestAreaInfo) {
            removePreferenceFromScreen("latest_area_info");
        }
        this.mCdmaSimStatus.updateCdmaPreference(this, this.mSir);
    }

    public void updatePhoneInfos() {
        if (this.mSir == null) {
            return;
        }
        Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(this.mSir.getSubscriptionId()));
        if (!UserManager.get(getContext()).isAdminUser() || !SubscriptionManager.isValidSubscriptionId(this.mSir.getSubscriptionId())) {
            return;
        }
        if (phone == null) {
            Log.e("SimStatus", "Unable to locate a phone object for the given Subscription ID.");
            return;
        }
        this.mPhone = phone;
        this.mCdmaSimStatus.setPhoneInfos(this.mPhone);
        if (this.mPhoneStateListener != null) {
            Log.d("SimStatus", "remove the phone state listener mPhoneStateListener = " + this.mPhoneStateListener);
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
        this.mPhoneStateListener = new PhoneStateListener(this.mSir.getSubscriptionId()) {
            @Override
            public void onDataConnectionStateChanged(int state) {
                if (SimStatus.this.getActivity() == null) {
                    Log.d("SimStatus", "DataConnectionStateChanged activity is null");
                    return;
                }
                Log.d("SimStatus", "onDataConnectionStateChanged sub = " + SimStatus.this.mSir + " state = " + state);
                SimStatus.this.updateDataState();
                SimStatus.this.updateNetworkType();
            }

            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                if (SimStatus.this.getActivity() == null) {
                    Log.d("SimStatus", "SignalStrengthsChanged activity is null");
                } else {
                    Log.d("SimStatus", "onSignalStrengthsChanged sub = " + SimStatus.this.mSir);
                    SimStatus.this.updateSignalStrength(signalStrength);
                }
            }

            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                if (SimStatus.this.getActivity() == null) {
                    Log.d("SimStatus", "ServiceStateChanged activity is null");
                } else {
                    Log.d("SimStatus", "onServiceStateChanged sub = " + SimStatus.this.mSir);
                    SimStatus.this.updateServiceState(serviceState);
                }
            }
        };
    }

    private TabHost.TabSpec buildTabSpec(String tag, String title) {
        return this.mTabHost.newTabSpec(tag).setIndicator(title).setContent(this.mEmptyTabContent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mSimHotSwapHandler.unregisterOnSimHotSwap();
    }

    private void customizeTitle() {
        String title = getActivity().getTitle().toString();
        Log.d("SimStatus", "title = " + title);
        if (!title.equals(getString(R.string.sim_status_title))) {
            return;
        }
        ISettingsMiscExt ext = UtilsExt.getMiscPlugin(getActivity());
        String title2 = ext.customizeSimDisplayString(getActivity().getTitle().toString(), -1);
        Log.d("SimStatus", "title = " + title2);
        getActivity().setTitle(title2);
    }
}
