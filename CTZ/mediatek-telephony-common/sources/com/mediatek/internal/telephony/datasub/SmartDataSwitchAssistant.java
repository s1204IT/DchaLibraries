package com.mediatek.internal.telephony.datasub;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.ims.ImsException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SettingsObserver;
import com.mediatek.ims.internal.MtkImsManagerEx;
import com.mediatek.internal.telephony.OpTelephonyCustomizationFactoryBase;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.dataconnection.IDataConnectionExt;

public class SmartDataSwitchAssistant extends Handler {
    private static final boolean DBG = true;
    private static final int EVENT_CALL_ENDED = 20;
    private static final int EVENT_CALL_STARTED = 10;
    private static final int EVENT_ID_INTVL = 10;
    private static final int EVENT_SRVCC_STATE_CHANGED = 30;
    private static final int EVENT_TEMPORARY_DATA_SERVICE_SETTINGS = 40;
    private static final String LOG_TAG = "SmartDataSwitch";
    private static final String PROPERTY_DEFAULT_DATA_SELECTED = "persist.vendor.radio.default.data.selected";
    private static final String TEMP_DATA_SERVICE = "data_service_enabled";
    private static String mOperatorSpec;
    private static SmartDataSwitchAssistant sSmartDataSwitchAssistant = null;
    protected boolean isTemporaryDataServiceOn;
    private ConnectivityManager mConnectivityManager;
    private Context mContext;
    private IDataConnectionExt mDataConnectionExt;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    protected int mPhoneNum;
    protected Phone[] mPhones;
    protected ContentResolver mResolver;
    protected final SettingsObserver mSettingsObserver;
    private OpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory;
    protected int mInCallPhoneId = -1;
    protected int mDefaultDataPhoneId = -1;
    protected boolean isResetTdsSettingsByFwk = true;
    private TelephonyManager mTelephonyManager = null;
    protected final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int imsCallPhoneId;
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                int intExtra = intent.getIntExtra("subscription", -1);
                SmartDataSwitchAssistant.logd("onReceive: DEFAULT_DATA_SUBSCRIPTION_CHANGED defaultDataSubId=" + intExtra);
                SmartDataSwitchAssistant.this.updateDefaultDataPhoneId(intExtra, "DataSubChanged");
                return;
            }
            if (action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED")) {
                int intExtra2 = intent.getIntExtra("simDetectStatus", 4);
                if ((intExtra2 == 2 || intExtra2 == 1) && SmartDataSwitchAssistant.this.isResetTdsSettingsByFwk) {
                    SmartDataSwitchAssistant.logd("onSubInfoRecordUPdate: Detecct Status:" + intExtra2);
                    SmartDataSwitchAssistant.this.resetTdsSettingsByFwk();
                    return;
                }
                return;
            }
            if (!action.equals("android.intent.action.PRECISE_CALL_STATE")) {
                if (action.equals("android.intent.action.PHONE_STATE")) {
                    String stringExtra = intent.getStringExtra("state");
                    SmartDataSwitchAssistant.logd("onPhoneStateChanged: callState:" + stringExtra);
                    if (SmartDataSwitchAssistant.this.isDuringImsCall() && stringExtra.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        SmartDataSwitchAssistant.this.obtainMessage(20 + SmartDataSwitchAssistant.this.mInCallPhoneId).sendToTarget();
                        return;
                    }
                    return;
                }
                return;
            }
            int intExtra3 = intent.getIntExtra("ringing_state", -1);
            int intExtra4 = intent.getIntExtra("foreground_state", -1);
            SmartDataSwitchAssistant.logd("onPreciseCallStateChanged: ringingCallState:" + intExtra3 + " foregroundCallState:" + intExtra4 + " backgroundCallState:" + intent.getIntExtra("background_state", -1));
            if (intExtra4 == 1 && !SmartDataSwitchAssistant.this.isDuringImsCall() && (imsCallPhoneId = SmartDataSwitchAssistant.this.getImsCallPhoneId()) != -1) {
                SmartDataSwitchAssistant.this.obtainMessage(10 + imsCallPhoneId).sendToTarget();
            }
        }
    };
    private final SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            SmartDataSwitchAssistant.this.updateDefaultDataPhoneId(SubscriptionManager.getDefaultDataSubscriptionId(), "SubscriptionsChanged");
        }
    };

    protected void regSettingsObserver() {
        this.mSettingsObserver.unobserve();
        this.mSettingsObserver.observe(Settings.Global.getUriFor(TEMP_DATA_SERVICE), 40);
    }

    public static SmartDataSwitchAssistant makeSmartDataSwitchAssistant(Context context, Phone[] phoneArr) {
        if (context == null || phoneArr == null) {
            throw new RuntimeException("param is null");
        }
        if (sSmartDataSwitchAssistant == null) {
            sSmartDataSwitchAssistant = new SmartDataSwitchAssistant(context, phoneArr);
        }
        logd("makeSmartDataSwitchAssistant: X sSmartDataSwitchAssistant =" + sSmartDataSwitchAssistant);
        return sSmartDataSwitchAssistant;
    }

    private SmartDataSwitchAssistant(Context context, Phone[] phoneArr) {
        this.isTemporaryDataServiceOn = false;
        this.mContext = null;
        this.mTelephonyCustomizationFactory = null;
        this.mDataConnectionExt = null;
        logd("SmartDataSwitchAssistant is created");
        this.mPhones = phoneArr;
        this.mPhoneNum = phoneArr.length;
        this.mContext = context;
        this.mResolver = this.mContext.getContentResolver();
        mOperatorSpec = SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, "OM");
        this.mSettingsObserver = new SettingsObserver(this.mContext, this);
        if (isSmartDataSwitchSupport()) {
            registerEvents();
        }
        try {
            this.mTelephonyCustomizationFactory = OpTelephonyCustomizationUtils.getOpFactory(this.mContext);
            this.mDataConnectionExt = this.mTelephonyCustomizationFactory.makeDataConnectionExt(this.mContext);
        } catch (Exception e) {
            loge("mDataConnectionExt init fail");
            e.printStackTrace();
        }
        if (Settings.Global.getInt(this.mResolver, TEMP_DATA_SERVICE, 0) != 0) {
            this.isTemporaryDataServiceOn = true;
        } else {
            this.isTemporaryDataServiceOn = false;
        }
        logd("SmartDataSwitchAssistant: isTemporaryDataServiceOn=" + this.isTemporaryDataServiceOn);
    }

    public void dispose() {
        logd("SmartDataSwitchAssistant.dispose");
        if (isSmartDataSwitchSupport()) {
            unregisterEvents();
        }
    }

    @Override
    public void handleMessage(Message message) {
        int i = message.what % 10;
        int i2 = message.what - i;
        if (i2 == 10) {
            if (getAllImsCallCount() == 1) {
                logd("Ims Call Started, phoneId=" + i);
                onCallStarted(i);
                return;
            }
            return;
        }
        if (i2 == 20) {
            logd("Ims Call Ended, phoneId=" + i);
            onCallEnded();
            return;
        }
        if (i2 == 30) {
            if (this.mInCallPhoneId != -1) {
                logd("SRVCC, phoneId=" + i);
                onCallEnded();
                return;
            }
            return;
        }
        if (i2 == 40) {
            boolean z = this.isTemporaryDataServiceOn;
            boolean z2 = Settings.Global.getInt(this.mResolver, TEMP_DATA_SERVICE, 0) != 0;
            if (z != z2) {
                this.isTemporaryDataServiceOn = z2;
                logd("TemporaryDataSetting changed newSettings=" + z2);
                onTemporaryDataSettingsChanged();
                return;
            }
            return;
        }
        logd("Unhandled message with number: " + message.what);
    }

    private void regSrvccEvent() {
        for (int i = 0; i < this.mPhoneNum; i++) {
            this.mPhones[i].registerForHandoverStateChanged(this, 30 + i, (Object) null);
        }
    }

    private void unregSrvccEvent() {
        for (int i = 0; i < this.mPhoneNum; i++) {
            this.mPhones[i].unregisterForHandoverStateChanged(this);
        }
    }

    private void registerEvents() {
        logd("registerEvents");
        regSettingsObserver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        intentFilter.addAction("android.intent.action.PRECISE_CALL_STATE");
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        if (this.isResetTdsSettingsByFwk) {
            intentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        }
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        SubscriptionManager.from(this.mContext).addOnSubscriptionsChangedListener(this.mSubscriptionsChangedListener);
    }

    private void unregisterEvents() {
        logd("unregisterEvents");
        getTelephonyManager();
        SubscriptionManager.from(this.mContext).removeOnSubscriptionsChangedListener(this.mSubscriptionsChangedListener);
    }

    public void updateDefaultDataPhoneId(int i, String str) {
        if (SubscriptionManager.isValidSubscriptionId(i)) {
            int phoneId = SubscriptionManager.getPhoneId(i);
            if (this.mDefaultDataPhoneId == -1) {
                this.mDefaultDataPhoneId = phoneId;
                logd("first time to update mDefaultDataPhoneId=" + this.mDefaultDataPhoneId + " reason:" + str);
                if (this.isResetTdsSettingsByFwk && !isDefaultDataSelectedBeforeReboot()) {
                    resetTdsSettingsByFwk();
                }
            } else if (phoneId != this.mDefaultDataPhoneId) {
                this.mDefaultDataPhoneId = phoneId;
                logd("updateDefaultDataPhoneId() mDefaultDataPhoneId=" + this.mDefaultDataPhoneId + " reason:" + str);
                if (this.isResetTdsSettingsByFwk) {
                    resetTdsSettingsByFwk();
                }
            }
            setDefaultDataSelectedProperty(1);
            return;
        }
        setDefaultDataSelectedProperty(0);
    }

    public void onCallStarted(int i) {
        this.mInCallPhoneId = i;
        regSrvccEvent();
        if (this.isTemporaryDataServiceOn) {
            if (this.mDefaultDataPhoneId != this.mInCallPhoneId) {
                if (isSmartDataSwtichAllowed()) {
                    newNetworkRequest(this.mPhones[i].getSubId());
                    return;
                }
                return;
            }
            logd("onCallStarted: don'w switch for Data SIM");
            return;
        }
        logd("onCallStarted: isTemporaryDataServiceOn = false");
    }

    public void onCallEnded() {
        unregSrvccEvent();
        releaseNetworkRequest();
        this.mInCallPhoneId = -1;
    }

    public void onTemporaryDataSettingsChanged() {
        logd("onTemporaryDataSettingsChanged() newSettings=" + this.isTemporaryDataServiceOn);
        if (this.mInCallPhoneId != -1) {
            if (this.isTemporaryDataServiceOn && this.mDefaultDataPhoneId != this.mInCallPhoneId) {
                if (isSmartDataSwtichAllowed()) {
                    newNetworkRequest(this.mPhones[this.mInCallPhoneId].getSubId());
                    return;
                }
                return;
            }
            releaseNetworkRequest();
            return;
        }
        logd("onTemporaryDataSettingsChanged: no active IMS call in non-data sim ");
    }

    private void newNetworkRequest(int i) {
        ConnectivityManager connectivityManager = getConnectivityManager();
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                SmartDataSwitchAssistant.logd("mNetworkCallback.onAvailable");
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                SmartDataSwitchAssistant.logd("mNetworkCallback.onLost");
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                SmartDataSwitchAssistant.logd("mNetworkCallback.onUnavailable");
            }
        };
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(0);
        builder.addCapability(28);
        builder.setNetworkSpecifier(String.valueOf(i));
        NetworkRequest networkRequestBuild = builder.build();
        logd("networkRequest:" + networkRequestBuild);
        connectivityManager.registerNetworkCallback(networkRequestBuild, this.mNetworkCallback);
        connectivityManager.requestNetwork(networkRequestBuild, this.mNetworkCallback);
    }

    private void releaseNetworkRequest() {
        if (this.mNetworkCallback != null) {
            logd("releaseNetworkRequest()");
            getConnectivityManager().unregisterNetworkCallback(this.mNetworkCallback);
            this.mNetworkCallback = null;
        }
    }

    private int getAllImsCallCount() {
        int i = 0;
        for (int i2 = 0; i2 < this.mPhoneNum; i2++) {
            try {
                int currentCallCount = MtkImsManagerEx.getInstance().getCurrentCallCount(i2);
                i += currentCallCount;
                logd("ImsManager.getCurrentCallCount() phone_" + i2 + ": " + currentCallCount);
            } catch (ImsException e) {
                loge("getAllImsCallCount: " + e);
            }
        }
        return i;
    }

    private int getImsCallPhoneId() {
        int i;
        int i2 = 0;
        while (true) {
            i = -1;
            try {
                if (i2 < this.mPhoneNum) {
                    if (MtkImsManagerEx.getInstance().getCurrentCallCount(i2) > 0 && this.mPhones[i2].isDuringImsCall()) {
                        break;
                    }
                    i2++;
                } else {
                    break;
                }
            } catch (ImsException e) {
                loge("getImsCallPhoneId: " + e);
            }
        }
        logd("get ImsCallPhoneId = " + i);
        return i;
    }

    private boolean isWifcCalling(int i) {
        return this.mPhones[i].isWifiCallingEnabled();
    }

    private boolean isDuringImsCall() {
        if (this.mInCallPhoneId != -1) {
            logd("isDuringImsCall() mInCallPhoneId=" + this.mInCallPhoneId + " return true");
            return true;
        }
        return false;
    }

    private void resetTdsSettingsByFwk() {
        Settings.Global.putInt(this.mResolver, TEMP_DATA_SERVICE, 0);
        logd("reset settings of Tempoary Data Service!");
    }

    private boolean isSmartDataSwtichAllowed() {
        boolean zIsSmartDataSwtichAllowed;
        if (this.mDataConnectionExt != null) {
            try {
                zIsSmartDataSwtichAllowed = this.mDataConnectionExt.isSmartDataSwtichAllowed();
            } catch (Exception e) {
                loge("Fail to create or use plug-in");
                e.printStackTrace();
                zIsSmartDataSwtichAllowed = false;
            }
        } else {
            zIsSmartDataSwtichAllowed = false;
        }
        logd("isSmartDataSwtichAllowed() return: " + zIsSmartDataSwtichAllowed);
        return zIsSmartDataSwtichAllowed;
    }

    private ConnectivityManager getConnectivityManager() {
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        return this.mConnectivityManager;
    }

    private TelephonyManager getTelephonyManager() {
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        return this.mTelephonyManager;
    }

    private void setDefaultDataSelectedProperty(int i) {
        if (!SystemProperties.get(PROPERTY_DEFAULT_DATA_SELECTED).equals(String.valueOf(i))) {
            SystemProperties.set(PROPERTY_DEFAULT_DATA_SELECTED, String.valueOf(i));
            logd("setDefaultDataSelectedProperty() selected=" + String.valueOf(i));
        }
    }

    private boolean isDefaultDataSelectedBeforeReboot() {
        String str = SystemProperties.get(PROPERTY_DEFAULT_DATA_SELECTED);
        logd("isDefaultDataSelectedBeforeReboot() property=" + str);
        return str.equals("1");
    }

    private boolean isSmartDataSwitchSupport() {
        return SystemProperties.get("persist.vendor.radio.smart.data.switch").equals("1");
    }

    protected static void logv(String str) {
        Rlog.v(LOG_TAG, str);
    }

    protected static void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }

    protected static void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    protected static void logi(String str) {
        Rlog.i(LOG_TAG, str);
    }
}
