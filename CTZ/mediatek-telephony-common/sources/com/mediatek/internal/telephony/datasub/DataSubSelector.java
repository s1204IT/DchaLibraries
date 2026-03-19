package com.mediatek.internal.telephony.datasub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.mediatek.internal.telephony.MtkSubscriptionController;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.internal.telephony.OpTelephonyCustomizationFactoryBase;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.dataconnection.MtkDcHelper;
import java.util.List;

public class DataSubSelector {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "DSSelector";
    private static String mOperatorSpec;
    private CapabilitySwitch mCapabilitySwitch;
    private Context mContext;
    private IDataSubSelectorOPExt mDataSubSelectorOPExt;
    private int mPhoneNum;
    private Handler mProtocolHandler;
    private ISimSwitchForDSSExt mSimSwitchForDSSExt;
    private static final boolean USER_BUILD = TextUtils.equals(Build.TYPE, DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER);
    private static DataSubSelector sDataSubSelector = null;
    private OpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory = null;
    private boolean mIsWaitIccid = false;
    private boolean mIsNeedPreCheck = true;
    private boolean mIsNeedWaitAirplaneModeOff = false;
    private boolean mIsNeedWaitAirplaneModeOffRoaming = false;
    private boolean mAirplaneModeOn = false;
    private Intent mIntent = null;
    private boolean mIsInRoaming = false;
    protected final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            DataSubSelector.this.log("onReceive: action=" + action);
            if (action.equals("android.telephony.action.SIM_APPLICATION_STATE_CHANGED")) {
                DataSubSelector.this.handleSimStateChanged(intent);
                return;
            }
            if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                int intExtra = intent.getIntExtra("subscription", -1);
                DataSubSelector.this.log("nDefaultDataSubId: " + intExtra);
                DataSubSelector.this.handleDefaultDataChanged(intent);
                return;
            }
            if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                DataSubSelector.this.mAirplaneModeOn = intent.getBooleanExtra("state", false);
                DataSubSelector.this.log("ACTION_AIRPLANE_MODE_CHANGED, enabled = " + DataSubSelector.this.mAirplaneModeOn);
                if (!DataSubSelector.this.mAirplaneModeOn) {
                    if (DataSubSelector.this.mIsNeedWaitAirplaneModeOff) {
                        DataSubSelector.this.mIsNeedWaitAirplaneModeOff = false;
                        DataSubSelector.this.handleAirPlaneModeOff(intent);
                    }
                    if (DataSubSelector.this.mIsNeedWaitAirplaneModeOffRoaming) {
                        DataSubSelector.this.mIsNeedWaitAirplaneModeOffRoaming = false;
                        return;
                    }
                    return;
                }
                return;
            }
            if (action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED")) {
                DataSubSelector.this.handleSubinfoRecordUpdated(intent);
            } else if ("mediatek.intent.action.LOCATED_PLMN_CHANGED".equals(action)) {
                DataSubSelector.this.handlePlmnChanged(intent);
            }
        }
    };
    private UpdateNWTypeHandler mUpdateNWTypeHandler = new UpdateNWTypeHandler();

    public static DataSubSelector makeDataSubSelector(Context context, int i) {
        if (sDataSubSelector == null) {
            sDataSubSelector = new DataSubSelector(context, i);
        }
        return sDataSubSelector;
    }

    private DataSubSelector(Context context, int i) {
        this.mDataSubSelectorOPExt = null;
        this.mSimSwitchForDSSExt = null;
        this.mContext = null;
        this.mCapabilitySwitch = null;
        log("DataSubSelector is created");
        this.mPhoneNum = i;
        mOperatorSpec = SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, "OM");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.SIM_APPLICATION_STATE_CHANGED");
        intentFilter.addAction("mediatek.intent.action.LOCATED_PLMN_CHANGED");
        intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        intentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter);
        this.mContext = context;
        initOpDataSubSelector(context);
        if (this.mDataSubSelectorOPExt == null) {
            this.mDataSubSelectorOPExt = new DataSubSelectorOpExt(context);
        }
        initSimSwitchForDSS(context);
        if (this.mSimSwitchForDSSExt == null) {
            this.mSimSwitchForDSSExt = new SimSwitchForDSSExt(context);
        }
        this.mCapabilitySwitch = CapabilitySwitch.getInstance(context, this);
        this.mSimSwitchForDSSExt.init(this);
        this.mDataSubSelectorOPExt.init(this, this.mSimSwitchForDSSExt);
    }

    private void initOpDataSubSelector(Context context) {
        try {
            this.mTelephonyCustomizationFactory = OpTelephonyCustomizationUtils.getOpFactory(context);
            this.mDataSubSelectorOPExt = this.mTelephonyCustomizationFactory.makeDataSubSelectorOPExt(context);
        } catch (Exception e) {
            log("mDataSubSelectorOPExt init fail");
            e.printStackTrace();
        }
    }

    private void initSimSwitchForDSS(Context context) {
        try {
            this.mTelephonyCustomizationFactory = OpTelephonyCustomizationUtils.getOpFactory(context);
            this.mSimSwitchForDSSExt = this.mTelephonyCustomizationFactory.makeSimSwitchForDSSOPExt(context);
        } catch (Exception e) {
            log("mSimSwitchForDSSExt init fail");
            e.printStackTrace();
        }
    }

    private void handleDefaultDataChanged(Intent intent) {
        this.mDataSubSelectorOPExt.handleDefaultDataChanged(intent);
    }

    private void handleSubinfoRecordUpdated(Intent intent) {
        this.mDataSubSelectorOPExt.handleSubinfoRecordUpdated(intent);
    }

    private void handleSimStateChanged(Intent intent) {
        this.mDataSubSelectorOPExt.handleSimStateChanged(intent);
    }

    private void handleAirPlaneModeOff(Intent intent) {
        this.mDataSubSelectorOPExt.handleAirPlaneModeOff(intent);
    }

    private void handlePlmnChanged(Intent intent) {
        this.mDataSubSelectorOPExt.handlePlmnChanged(intent);
    }

    public boolean getAirPlaneModeOn() {
        return this.mAirplaneModeOn;
    }

    public boolean getIsWaitIccid() {
        return this.mIsWaitIccid;
    }

    public void setIsWaitIccid(boolean z) {
        this.mIsWaitIccid = z;
    }

    public boolean getIsNeedPreCheck() {
        return this.mIsNeedPreCheck;
    }

    public void setIsNeedPreCheck(boolean z) {
        this.mIsNeedPreCheck = z;
    }

    public void setDataEnabled(int i, boolean z) {
        log("setDataEnabled: phoneId=" + i + ", enable=" + z);
        TelephonyManager telephonyManager = TelephonyManager.getDefault();
        if (telephonyManager != null) {
            if (i == -1) {
                telephonyManager.setDataEnabled(z);
                return;
            }
            if (!z) {
                int subId = PhoneFactory.getPhone(i).getSubId();
                log("Set Sub" + subId + " to disable");
                telephonyManager.setDataEnabled(subId, z);
                return;
            }
            for (int i2 = 0; i2 < this.mPhoneNum; i2++) {
                int subId2 = PhoneFactory.getPhone(i2).getSubId();
                if (i2 != i) {
                    log("Set Sub" + subId2 + " to disable");
                    telephonyManager.setDataEnabled(subId2, false);
                } else {
                    log("Set Sub" + subId2 + " to enable");
                    telephonyManager.setDataEnabled(subId2, true);
                }
            }
        }
    }

    public void setDefaultData(int i) {
        SubscriptionController.getInstance();
        int subIdUsingPhoneId = MtkSubscriptionManager.getSubIdUsingPhoneId(i);
        int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        log("setDefaultDataSubId: " + subIdUsingPhoneId + ", current default sub:" + defaultDataSubscriptionId);
        if (subIdUsingPhoneId != defaultDataSubscriptionId && subIdUsingPhoneId >= -1) {
            MtkSubscriptionController.getMtkInstance().setDefaultDataSubIdWithoutCapabilitySwitch(subIdUsingPhoneId);
        } else {
            log("setDefaultDataSubId: default data unchanged");
        }
    }

    public void syncDefaultDataToMd(int i) {
        MtkDcHelper.getInstance().syncDefaultDataSlotId(MtkSubscriptionController.getMtkInstance().getSlotIndex(MtkSubscriptionManager.getSubIdUsingPhoneId(i)));
    }

    public int getPhoneNum() {
        return this.mPhoneNum;
    }

    public void updateNetworkMode(Context context, final int i) {
        this.mContext = context.getApplicationContext();
        final List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(this.mContext).getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList == null) {
            log("subInfoList null");
            return;
        }
        this.mProtocolHandler = new Handler();
        this.mProtocolHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (activeSubscriptionInfoList.size() == 1) {
                    DataSubSelector.this.updateNetworkModeUtil(i, 9);
                    return;
                }
                if (activeSubscriptionInfoList.size() > 1) {
                    for (int i2 = 0; i2 < activeSubscriptionInfoList.size(); i2++) {
                        int subscriptionId = ((SubscriptionInfo) activeSubscriptionInfoList.get(i2)).getSubscriptionId();
                        if (subscriptionId == i) {
                            DataSubSelector.this.updateNetworkModeUtil(subscriptionId, 9);
                        } else {
                            DataSubSelector.this.updateNetworkModeUtil(subscriptionId, 0);
                        }
                    }
                }
            }
        }, 5000L);
    }

    private void updateNetworkModeUtil(int i, int i2) {
        log("Updating network mode for subId " + i + "mode " + i2);
        PhoneFactory.getPhone(SubscriptionManager.getPhoneId(i)).setPreferredNetworkType(i2, this.mUpdateNWTypeHandler.obtainMessage(0, i, i2));
    }

    private class UpdateNWTypeHandler extends Handler {
        static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 0;

        private UpdateNWTypeHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                handleSetPreferredNetworkTypeResponse(message, message.arg1, message.arg2);
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message message, int i, int i2) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(i));
            if (asyncResult.exception != null) {
                DataSubSelector.this.log("handleSetPreferredNetworkTypeResponse:exception in setting network.");
                return;
            }
            DataSubSelector.this.log("handleSetPreferredNetwrokTypeResponse2: networkMode:" + i2);
            Settings.Global.putInt(phone.getContext().getContentResolver(), "preferred_network_mode" + i, i2);
        }
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }
}
