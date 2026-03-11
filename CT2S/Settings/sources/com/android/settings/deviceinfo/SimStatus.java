package com.android.settings.deviceinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
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
import android.view.View;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.R;
import com.android.settings.Utils;
import java.util.ArrayList;
import java.util.List;

public class SimStatus extends PreferenceActivity {
    private String mDefaultText;
    private ListView mListView;
    private PhoneStateListener mPhoneStateListener;
    private Resources mRes;
    private List<SubscriptionInfo> mSelectableSubInfos;
    private boolean mShowLatestAreaInfo;
    private Preference mSignalStrength;
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
            if ("android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED".equals(action) && (extras = intent.getExtras()) != null && (cbMessage = (CellBroadcastMessage) extras.get("message")) != null && cbMessage.getServiceCategory() == 50 && SimStatus.this.mSir.getSubscriptionId() == cbMessage.getSubId()) {
                String latestAreaInfo = cbMessage.getMessageBody();
                SimStatus.this.updateAreaInfo(latestAreaInfo);
            }
        }
    };
    private TabHost.OnTabChangeListener mTabListener = new TabHost.OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            int slotId = Integer.parseInt(tabId);
            SimStatus.this.mSir = (SubscriptionInfo) SimStatus.this.mSelectableSubInfos.get(slotId);
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
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mSelectableSubInfos = new ArrayList();
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        addPreferencesFromResource(R.xml.device_info_sim_status);
        this.mRes = getResources();
        this.mDefaultText = this.mRes.getString(R.string.device_info_default);
        this.mSignalStrength = findPreference("signal_strength");
        for (int i = 0; i < this.mTelephonyManager.getSimCount(); i++) {
            SubscriptionInfo sir = Utils.findRecordBySlotId(this, i);
            if (sir != null) {
                this.mSelectableSubInfos.add(sir);
            }
        }
        this.mSir = this.mSelectableSubInfos.size() > 0 ? this.mSelectableSubInfos.get(0) : null;
        if (this.mSelectableSubInfos.size() > 1) {
            setContentView(R.layout.sim_information);
            this.mTabHost = (TabHost) findViewById(android.R.id.tabhost);
            this.mTabWidget = (TabWidget) findViewById(android.R.id.tabs);
            this.mListView = (ListView) findViewById(android.R.id.list);
            this.mTabHost.setup();
            this.mTabHost.setOnTabChangedListener(this.mTabListener);
            this.mTabHost.clearAllTabs();
            for (int i2 = 0; i2 < this.mSelectableSubInfos.size(); i2++) {
                this.mTabHost.addTab(buildTabSpec(String.valueOf(i2), String.valueOf(this.mSelectableSubInfos.get(i2).getDisplayName())));
            }
        }
        updatePhoneInfos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mPhone != null) {
            updatePreference();
            updateSignalStrength(this.mPhone.getSignalStrength());
            updateServiceState(this.mPhone.getServiceState());
            updateDataState();
            this.mTelephonyManager.listen(this.mPhoneStateListener, 321);
            if (this.mShowLatestAreaInfo) {
                registerReceiver(this.mAreaInfoReceiver, new IntentFilter("android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED"), "android.permission.RECEIVE_EMERGENCY_BROADCAST", null);
                Intent getLatestIntent = new Intent("android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO");
                sendBroadcastAsUser(getLatestIntent, UserHandle.ALL, "android.permission.RECEIVE_EMERGENCY_BROADCAST");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mPhone != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
        if (this.mShowLatestAreaInfo) {
            unregisterReceiver(this.mAreaInfoReceiver);
        }
    }

    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void setSummaryText(String key, String text) {
        if (TextUtils.isEmpty(text)) {
            text = this.mDefaultText;
        }
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setSummary(text);
        }
    }

    public void updateNetworkType() {
        String networktype = null;
        this.mSir.getSubscriptionId();
        int actualDataNetworkType = this.mTelephonyManager.getDataNetworkType(this.mSir.getSubscriptionId());
        int actualVoiceNetworkType = this.mTelephonyManager.getVoiceNetworkType(this.mSir.getSubscriptionId());
        if (actualDataNetworkType != 0) {
            TelephonyManager telephonyManager = this.mTelephonyManager;
            networktype = TelephonyManager.getNetworkTypeName(actualDataNetworkType);
        } else if (actualVoiceNetworkType != 0) {
            TelephonyManager telephonyManager2 = this.mTelephonyManager;
            networktype = TelephonyManager.getNetworkTypeName(actualVoiceNetworkType);
        }
        setSummaryText("network_type", networktype);
    }

    public void updateDataState() {
        int state = DefaultPhoneNotifier.convertDataState(this.mPhone.getDataConnectionState());
        String display = this.mRes.getString(R.string.radioInfo_unknown);
        switch (state) {
            case 0:
                display = this.mRes.getString(R.string.radioInfo_data_disconnected);
                break;
            case 1:
                display = this.mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case 2:
                display = this.mRes.getString(R.string.radioInfo_data_connected);
                break;
            case 3:
                display = this.mRes.getString(R.string.radioInfo_data_suspended);
                break;
        }
        setSummaryText("data_state", display);
    }

    public void updateServiceState(ServiceState serviceState) {
        int state = serviceState.getState();
        String display = this.mRes.getString(R.string.radioInfo_unknown);
        switch (state) {
            case 0:
                display = this.mRes.getString(R.string.radioInfo_service_in);
                break;
            case 1:
            case 2:
                display = this.mRes.getString(R.string.radioInfo_service_out);
                break;
            case 3:
                display = this.mRes.getString(R.string.radioInfo_service_off);
                break;
        }
        setSummaryText("service_state", display);
        if (serviceState.getRoaming()) {
            setSummaryText("roaming_state", this.mRes.getString(R.string.radioInfo_roaming_in));
        } else {
            setSummaryText("roaming_state", this.mRes.getString(R.string.radioInfo_roaming_not));
        }
        setSummaryText("operator_name", serviceState.getOperatorAlphaLong());
    }

    public void updateAreaInfo(String areaInfo) {
        if (areaInfo != null) {
            setSummaryText("latest_area_info", areaInfo);
        }
    }

    void updateSignalStrength(SignalStrength signalStrength) {
        if (this.mSignalStrength != null) {
            int state = this.mPhone.getServiceState().getState();
            Resources r = getResources();
            if (1 == state || 3 == state) {
                this.mSignalStrength.setSummary("0");
            }
            int signalDbm = signalStrength.getDbm();
            int signalAsu = signalStrength.getAsuLevel();
            if (-1 == signalDbm) {
                signalDbm = 0;
            }
            if (-1 == signalAsu) {
                signalAsu = 0;
            }
            this.mSignalStrength.setSummary(r.getString(R.string.sim_signal_strength, Integer.valueOf(signalDbm), Integer.valueOf(signalAsu)));
        }
    }

    public void updatePreference() {
        if (this.mPhone.getPhoneType() != 2 && "br".equals(this.mTelephonyManager.getSimCountryIso(this.mSir.getSubscriptionId()))) {
            this.mShowLatestAreaInfo = true;
        }
        String rawNumber = this.mTelephonyManager.getLine1NumberForSubscriber(this.mSir.getSubscriptionId());
        String formattedNumber = null;
        if (!TextUtils.isEmpty(rawNumber)) {
            formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
        }
        setSummaryText("number", formattedNumber);
        String imei = this.mPhone.getPhoneType() == 2 ? this.mPhone.getImei() : this.mPhone.getDeviceId();
        setSummaryText("imei", imei);
        setSummaryText("imei_sv", this.mTelephonyManager.getDeviceSoftwareVersion());
        if (!this.mShowLatestAreaInfo) {
            removePreferenceFromScreen("latest_area_info");
        }
    }

    public void updatePhoneInfos() {
        if (this.mSir != null) {
            Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(this.mSir.getSubscriptionId()));
            if (UserHandle.myUserId() == 0 && SubscriptionManager.isValidSubscriptionId(this.mSir.getSubscriptionId())) {
                if (phone == null) {
                    Log.e("SimStatus", "Unable to locate a phone object for the given Subscription ID.");
                } else {
                    this.mPhone = phone;
                    this.mPhoneStateListener = new PhoneStateListener(this.mSir.getSubscriptionId()) {
                        @Override
                        public void onDataConnectionStateChanged(int state) {
                            SimStatus.this.updateDataState();
                            SimStatus.this.updateNetworkType();
                        }

                        @Override
                        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                            SimStatus.this.updateSignalStrength(signalStrength);
                        }

                        @Override
                        public void onServiceStateChanged(ServiceState serviceState) {
                            SimStatus.this.updateServiceState(serviceState);
                        }
                    };
                }
            }
        }
    }

    private TabHost.TabSpec buildTabSpec(String tag, String title) {
        return this.mTabHost.newTabSpec(tag).setIndicator(title).setContent(this.mEmptyTabContent);
    }
}
