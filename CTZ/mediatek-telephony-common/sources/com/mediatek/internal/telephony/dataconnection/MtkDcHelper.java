package com.mediatek.internal.telephony.dataconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyDevController;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MtkGsmCdmaCallTracker;
import com.mediatek.internal.telephony.MtkHardwareConfig;
import com.mediatek.internal.telephony.MtkPhoneSwitcher;
import com.mediatek.internal.telephony.MtkSubscriptionController;
import com.mediatek.internal.telephony.imsphone.MtkImsPhoneCallTracker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MtkDcHelper extends Handler {
    private static final int DATA_CONFIG_MULTI_PS = 1;
    private static final boolean DBG = true;
    private static final int EVENT_DEFAULT_SUBSCRIPTION_CHANGED = 40;
    private static final int EVENT_ID_INTVL = 10;
    private static final int EVENT_NO_CS_CALL_AFTER_SRVCC = 40;
    private static final int EVENT_RIL_CONNECTED = 10;
    private static final int EVENT_SUBSCRIPTION_CHANGED = 0;
    private static final int EVENT_VOICE_CALL_ENDED = 30;
    private static final int EVENT_VOICE_CALL_STARTED = 20;
    private static final String INVALID_ICCID = "N/A";
    private static final String LOG_TAG = "DcHelper";
    public static final boolean MTK_SRLTE_SUPPORT;
    public static final boolean MTK_SVLTE_SUPPORT;
    private static String[] PROPERTY_ICCID_SIM = null;
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE;
    private static final String[] PROPERTY_RIL_TEST_SIM;
    private static final String PROP_DATA_CONFIG = "ro.vendor.mtk_data_config";
    private static final String PROP_MTK_CDMA_LTE_MODE = "ro.boot.opt_c2k_lte_mode";
    private static final String RIL_CDMA_DUALACT_SUPPORT = "vendor.ril.cdma.3g.dualact";
    private static final Map<Operator, List> mOperatorMap;
    private Context mContext;
    private int mDefaultDataSubscription;
    protected int mPhoneNum;
    protected Phone[] mPhones;
    private static final boolean VDBG = SystemProperties.get("ro.build.type").equals("eng");
    private static MtkDcHelper sMtkDcHelper = null;
    private TelephonyDevController mTelDevController = TelephonyDevController.getInstance();
    private boolean mHasFetchMpsAttachSupport = false;
    private boolean mMpsAttachSupport = false;
    private int mDefaultDataSlotId = -2;
    private int mCallingPhoneId = -1;
    private int mPrevCallingPhoneId = -1;
    private Call.SrvccState mSrvccState = Call.SrvccState.NONE;
    private Handler mRspHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            int i;
            int i2 = message.what % 10;
            int i3 = message.what - i2;
            if (i3 == 10) {
                MtkDcHelper.logd("EVENT_PHONE" + i2 + "_EVENT_RIL_CONNECTED");
                MtkDcHelper.this.onCheckIfRetriggerDataAllowed(i2);
                return;
            }
            if (i3 != 20) {
                if (i3 != 30) {
                    if (i3 == 40) {
                        MtkDcHelper.logd("Got 'no CS calls after SRVCC' notification, tunnel it to VOICE_CALL_END");
                        MtkDcHelper.this.mSrvccState = Call.SrvccState.NONE;
                    } else {
                        MtkDcHelper.logd("Unhandled message with number: " + message.what);
                        return;
                    }
                }
                Call.SrvccState srvccState = MtkDcHelper.this.mSrvccState;
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult == null || asyncResult.result == null) {
                    MtkDcHelper.this.mSrvccState = Call.SrvccState.NONE;
                } else {
                    MtkDcHelper.this.mSrvccState = (Call.SrvccState) asyncResult.result;
                }
                MtkDcHelper.logd("mSrvccState = " + MtkDcHelper.this.mSrvccState);
                if (!MtkDcHelper.this.isInSRVCC() || srvccState == Call.SrvccState.NONE) {
                    MtkDcHelper.logd("Voice Call Ended, mCallingPhoneId = " + MtkDcHelper.this.mCallingPhoneId);
                    if (MtkDcHelper.MTK_SVLTE_SUPPORT && MtkDcHelper.this.mPrevCallingPhoneId != -1) {
                        i = i2 == MtkDcHelper.this.mCallingPhoneId ? MtkDcHelper.this.mPrevCallingPhoneId : MtkDcHelper.this.mCallingPhoneId;
                        MtkDcHelper.this.mCallingPhoneId = i;
                        MtkDcHelper.this.mPrevCallingPhoneId = -1;
                        MtkDcHelper.logd("SVLTE Voice Call2 Ended, mCallingPhoneId = " + MtkDcHelper.this.mCallingPhoneId);
                    } else {
                        i = -1;
                    }
                    MtkDcHelper.this.onVoiceCallEnded();
                    MtkDcHelper.this.mCallingPhoneId = -1;
                    if (MtkDcHelper.MTK_SVLTE_SUPPORT && i != -1) {
                        MtkDcHelper.this.mCallingPhoneId = i;
                        MtkDcHelper.logd("SVLTE Voice Call Ended, restore first mCallingPhoneId = " + MtkDcHelper.this.mCallingPhoneId);
                        return;
                    }
                    return;
                }
                return;
            }
            if (!MtkDcHelper.this.isInSRVCC()) {
                if (MtkDcHelper.MTK_SVLTE_SUPPORT && MtkDcHelper.this.mCallingPhoneId != -1) {
                    MtkDcHelper.this.mPrevCallingPhoneId = MtkDcHelper.this.mCallingPhoneId;
                    MtkDcHelper.logd("SVLTE Voice Call2 Started, save first mPrevCallingPhoneId = " + MtkDcHelper.this.mPrevCallingPhoneId);
                }
                MtkDcHelper.this.mCallingPhoneId = i2;
                MtkDcHelper.logd("Voice Call Started, mCallingPhoneId = " + MtkDcHelper.this.mCallingPhoneId);
                MtkDcHelper.this.onVoiceCallStarted();
            }
        }
    };
    private final SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            MtkDcHelper.this.obtainMessage(0).sendToTarget();
        }
    };
    private final BroadcastReceiver mDefaultDataChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MtkDcHelper.this.obtainMessage(40).sendToTarget();
        }
    };
    private final BroadcastReceiver mPhoneSwitchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.RADIO_TECHNOLOGY".equals(intent.getAction())) {
                int intExtra = intent.getIntExtra("phone", -1);
                MtkDcHelper.logd("mPhoneSwitchReceiver: phoneId = " + intExtra);
                if (MtkDcHelper.isCdma3GDualModeCard(intExtra) || MtkDcHelper.isCdma3GCard(intExtra)) {
                    MtkDcHelper.this.mPhones[intExtra].mDcTracker.updateRecords();
                }
            }
        }
    };

    public enum Operator {
        OP156
    }

    static {
        MTK_SVLTE_SUPPORT = SystemProperties.getInt(PROP_MTK_CDMA_LTE_MODE, 0) == 1;
        MTK_SRLTE_SUPPORT = SystemProperties.getInt(PROP_MTK_CDMA_LTE_MODE, 0) == 2;
        PROPERTY_ICCID_SIM = new String[]{"vendor.ril.iccid.sim1", "vendor.ril.iccid.sim2", "vendor.ril.iccid.sim3", "vendor.ril.iccid.sim4"};
        PROPERTY_RIL_TEST_SIM = new String[]{"vendor.gsm.sim.ril.testsim", "vendor.gsm.sim.ril.testsim.2", "vendor.gsm.sim.ril.testsim.3", "vendor.gsm.sim.ril.testsim.4"};
        PROPERTY_RIL_FULL_UICC_TYPE = new String[]{"vendor.gsm.ril.fulluicctype", "vendor.gsm.ril.fulluicctype.2", "vendor.gsm.ril.fulluicctype.3", "vendor.gsm.ril.fulluicctype.4"};
        mOperatorMap = new HashMap<Operator, List>() {
            {
                put(Operator.OP156, Arrays.asList("23802", "23877"));
            }
        };
    }

    protected MtkDcHelper(Context context, Phone[] phoneArr) {
        this.mContext = context;
        this.mPhones = phoneArr;
        this.mPhoneNum = phoneArr.length;
        registerEvents();
    }

    public void dispose() {
        logd("MtkDcHelper.dispose");
        unregisterEvents();
    }

    public static MtkDcHelper makeMtkDcHelper(Context context, Phone[] phoneArr) {
        if (context == null || phoneArr == null) {
            throw new RuntimeException("param is null");
        }
        if (sMtkDcHelper == null) {
            logd("makeMtkDcHelper: phones.length=" + phoneArr.length);
            sMtkDcHelper = new MtkDcHelper(context, phoneArr);
        }
        logd("makesMtkDcHelper: X sMtkDcHelper =" + sMtkDcHelper);
        return sMtkDcHelper;
    }

    public static MtkDcHelper getInstance() {
        if (sMtkDcHelper == null) {
            throw new RuntimeException("Should not be called before makesMtkDcHelper");
        }
        return sMtkDcHelper;
    }

    @Override
    public void handleMessage(Message message) {
        logd("msg id = " + message.what);
        int i = message.what;
        if (i == 0 || i == 40) {
            logd("EVENT_SUBSCRIPTION_CHANGED");
            onSubscriptionChanged();
        } else {
            logd("Unhandled msg: " + message.what);
        }
    }

    private void onSubscriptionChanged() {
        int defaultDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
        if (defaultDataSubId != this.mDefaultDataSubscription) {
            this.mDefaultDataSubscription = defaultDataSubId;
        }
        int slotIndex = MtkSubscriptionController.getMtkInstance().getSlotIndex(defaultDataSubId);
        if (slotIndex != this.mDefaultDataSlotId) {
            this.mDefaultDataSlotId = slotIndex;
            syncDefaultDataSlotId(slotIndex);
        }
    }

    private void onCheckIfRetriggerDataAllowed(int i) {
        logd("onCheckIfRetriggerDataAllowed: retriggerDataAllowed: mPhone[" + i + "]");
        if (MtkPhoneSwitcher.getInstance() != null) {
            MtkPhoneSwitcher.getInstance().resendDataAllowed(i);
        }
    }

    public boolean isOperatorMccMnc(Operator operator, int i) {
        String simOperatorNumericForPhone = TelephonyManager.getDefault().getSimOperatorNumericForPhone(i);
        boolean zContains = mOperatorMap.get(operator).contains(simOperatorNumericForPhone);
        logd("isOperatorMccMnc: mccmnc=" + simOperatorNumericForPhone + ", bMatched=" + zContains);
        return zContains;
    }

    private void registerEvents() {
        logd("registerEvents");
        SubscriptionManager.from(this.mContext).addOnSubscriptionsChangedListener(this.mSubscriptionsChangedListener);
        for (int i = 0; i < this.mPhoneNum; i++) {
            this.mPhones[i].mCi.registerForRilConnected(this.mRspHandler, 10 + i, (Object) null);
            this.mPhones[i].getCallTracker().registerForVoiceCallStarted(this.mRspHandler, 20 + i, (Object) null);
            this.mPhones[i].getCallTracker().registerForVoiceCallEnded(this.mRspHandler, 30 + i, (Object) null);
        }
        this.mContext.registerReceiver(this.mDefaultDataChangedReceiver, new IntentFilter("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED"));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.RADIO_TECHNOLOGY");
        this.mContext.registerReceiver(this.mPhoneSwitchReceiver, intentFilter);
        logd("registered phone change event.");
    }

    public void registerImsEvents(int i) {
        if (!SubscriptionManager.isValidPhoneId(i)) {
            logd("registerImsEvents, invalid phoneId");
            return;
        }
        logd("registerImsEvents, phoneId = " + i);
        Phone imsPhone = this.mPhones[i].getImsPhone();
        if (imsPhone != null) {
            MtkImsPhoneCallTracker callTracker = imsPhone.getCallTracker();
            callTracker.registerForVoiceCallStarted(this.mRspHandler, 20 + i, null);
            callTracker.registerForVoiceCallEnded(this.mRspHandler, 30 + i, null);
            callTracker.registerForCallsDisconnectedDuringSrvcc(this.mRspHandler, 40 + i, null);
            return;
        }
        logd("Not register IMS phone calling state yet.");
    }

    private void unregisterEvents() {
        logd("unregisterEvents");
        SubscriptionManager.from(this.mContext).removeOnSubscriptionsChangedListener(this.mSubscriptionsChangedListener);
        for (int i = 0; i < this.mPhoneNum; i++) {
            this.mPhones[i].getCallTracker().unregisterForVoiceCallStarted(this.mRspHandler);
            this.mPhones[i].getCallTracker().unregisterForVoiceCallEnded(this.mRspHandler);
        }
    }

    public void unregisterImsEvents(int i) {
        if (!SubscriptionManager.isValidPhoneId(i)) {
            logd("unregisterImsEvents, invalid phoneId");
            return;
        }
        logd("unregisterImsEvents, phoneId = " + i);
        Phone imsPhone = this.mPhones[i].getImsPhone();
        if (imsPhone != null) {
            MtkImsPhoneCallTracker callTracker = imsPhone.getCallTracker();
            callTracker.unregisterForVoiceCallStarted(this.mRspHandler);
            callTracker.unregisterForVoiceCallEnded(this.mRspHandler);
            callTracker.unregisterForCallsDisconnectedDuringSrvcc(this.mRspHandler);
            return;
        }
        logd("Not unregister IMS phone calling state yet.");
    }

    private void onVoiceCallStarted() {
        for (int i = 0; i < this.mPhoneNum; i++) {
            logd("onVoiceCallStarted: mPhone[ " + i + "]");
            ((MtkDcTracker) this.mPhones[i].mDcTracker).onVoiceCallStartedEx();
        }
    }

    private void onVoiceCallEnded() {
        for (int i = 0; i < this.mPhoneNum; i++) {
            logd("onVoiceCallEnded: mPhone[ " + i + "]");
            ((MtkDcTracker) this.mPhones[i].mDcTracker).onVoiceCallEndedEx();
        }
    }

    public boolean isDataSupportConcurrent(int i) {
        boolean zIsInEmergencyCall;
        PhoneConstants.State state;
        boolean z;
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 < this.mPhoneNum; i2++) {
            if (this.mPhones[i2].getState() != PhoneConstants.State.IDLE) {
                arrayList.add(Integer.valueOf(i2));
            }
        }
        if (arrayList.size() == 0) {
            logd("isDataSupportConcurrent: no calling phone!");
            return true;
        }
        if (MTK_SVLTE_SUPPORT && arrayList.size() > 1) {
            logd("isDataSupportConcurrent: SVLTE support and more than 1 calling phone!");
            return false;
        }
        if (i == ((Integer) arrayList.get(0)).intValue()) {
            MtkGsmCdmaCallTracker callTracker = this.mPhones[i].getCallTracker();
            Phone imsPhone = this.mPhones[i].getImsPhone();
            if (imsPhone != null) {
                zIsInEmergencyCall = imsPhone.isInEmergencyCall();
            } else {
                zIsInEmergencyCall = false;
            }
            PhoneConstants.State state2 = PhoneConstants.State.IDLE;
            if (callTracker != null) {
                z = callTracker.getHandoverConnectionSize() != 0;
                state = callTracker.getState();
            } else {
                state = state2;
                z = false;
            }
            boolean zIsConcurrentVoiceAndDataAllowed = (zIsInEmergencyCall || z || state != PhoneConstants.State.IDLE) ? this.mPhones[i].getServiceStateTracker().isConcurrentVoiceAndDataAllowed() : true;
            logd("isDataSupportConcurrent: (voice/data on the same phone) isConcurrent = " + zIsConcurrentVoiceAndDataAllowed + ", phoneId = " + i + ", callingPhoneId = " + arrayList.get(0) + ", inPsEcc = " + zIsInEmergencyCall + ", inSrvcc = " + z + ", csCallState = " + state);
            return zIsConcurrentVoiceAndDataAllowed;
        }
        if (MTK_SRLTE_SUPPORT) {
            logd("isDataSupportConcurrent: support SRLTE ");
            return false;
        }
        if (MTK_SVLTE_SUPPORT) {
            int phoneType = this.mPhones[((Integer) arrayList.get(0)).intValue()].getPhoneType();
            if (phoneType == 2) {
                return true;
            }
            int rilDataRadioTechnology = this.mPhones[i].getServiceState().getRilDataRadioTechnology();
            logd("isDataSupportConcurrent: support SVLTE RilRat = " + rilDataRadioTechnology + "calling phoneType: " + phoneType);
            return ServiceState.isCdma(rilDataRadioTechnology);
        }
        logd("isDataSupportConcurrent: not SRLTE or SVLTE ");
        return false;
    }

    public boolean isAllCallingStateIdle() {
        PhoneConstants.State[] stateArr = new PhoneConstants.State[this.mPhoneNum];
        int i = 0;
        boolean z = false;
        while (i < this.mPhoneNum) {
            stateArr[i] = this.mPhones[i].getState();
            if (stateArr[i] != null && stateArr[i] == PhoneConstants.State.IDLE) {
                i++;
                z = true;
            } else {
                z = false;
                break;
            }
        }
        if (!z && VDBG) {
            for (int i2 = 0; i2 < this.mPhoneNum; i2++) {
                logd("isAllCallingStateIdle: state[" + i2 + "]=" + stateArr[i2] + " allCallingState = " + z);
            }
        }
        return z;
    }

    public boolean isWifiCallingEnabled() {
        RemoteException e;
        boolean zIsWifiCallingEnabled;
        int i = this.mCallingPhoneId;
        int i2 = this.mPrevCallingPhoneId;
        IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        boolean z = false;
        if (iMtkTelephonyExAsInterface == null) {
            return false;
        }
        try {
            if (SubscriptionManager.isValidPhoneId(i)) {
                zIsWifiCallingEnabled = iMtkTelephonyExAsInterface.isWifiCallingEnabled(this.mPhones[i].getSubId());
            } else {
                zIsWifiCallingEnabled = false;
            }
            try {
                return (MTK_SVLTE_SUPPORT && !zIsWifiCallingEnabled && SubscriptionManager.isValidPhoneId(i2)) ? iMtkTelephonyExAsInterface.isWifiCallingEnabled(this.mPhones[i2].getSubId()) : zIsWifiCallingEnabled;
            } catch (RemoteException e2) {
                e = e2;
                z = zIsWifiCallingEnabled;
                e.printStackTrace();
                return z;
            }
        } catch (RemoteException e3) {
            e = e3;
        }
    }

    public static boolean isImsOrEmergencyApn(String[] strArr) {
        if (strArr == null) {
            loge("isImsOrEmergencyApn: apnTypes is null");
            return false;
        }
        if (strArr.length == 0) {
            return false;
        }
        for (String str : strArr) {
            if (!"ims".equals(str) && !"emergency".equals(str)) {
                return false;
            }
        }
        return true;
    }

    public boolean isDataAllowedForConcurrent(int i) {
        if (!SubscriptionManager.isValidPhoneId(i)) {
            logd("isDataAllowedForConcurrent: invalid calling phone id");
            return false;
        }
        if (isAllCallingStateIdle() || isDataSupportConcurrent(i)) {
            return true;
        }
        return isWifiCallingEnabled() && !this.mPhones[i].isInEmergencyCall();
    }

    public static boolean hasVsimApn(String[] strArr) {
        if (strArr == null) {
            loge("hasVsimApn: apnTypes is null");
            return false;
        }
        if (strArr.length == 0) {
            return false;
        }
        for (String str : strArr) {
            if (TextUtils.equals("vsim", str)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSimInserted(int i) {
        logd("isSimInserted:phoneId =" + i);
        String str = SystemProperties.get(PROPERTY_ICCID_SIM[i], "");
        return (TextUtils.isEmpty(str) || "N/A".equals(str)) ? false : true;
    }

    public boolean isTestIccCard(int i) {
        String str = SystemProperties.get(PROPERTY_RIL_TEST_SIM[i], "");
        if (VDBG) {
            logd("isTestIccCard: phoneId id = " + i + ", iccType = " + str);
        }
        return str != null && str.equals("1");
    }

    public boolean isMultiPsAttachSupport() {
        if (!this.mHasFetchMpsAttachSupport) {
            if ((SystemProperties.getInt(PROP_DATA_CONFIG, 0) & 1) == 1) {
                this.mMpsAttachSupport = true;
            }
            this.mHasFetchMpsAttachSupport = true;
        }
        return this.mMpsAttachSupport;
    }

    public synchronized void syncDefaultDataSlotId(int i) {
        if (i <= -1) {
            i = -1;
        }
        logd("syncDefaultDataSlotId: slot ID: " + i);
        ((MtkDcTracker) this.mPhones[0].mDcTracker).syncDefaultDataSlotId(i);
    }

    public boolean hasMdAutoSetupImsCapability() {
        if (this.mTelDevController != null && this.mTelDevController.getModem(0) != null && ((MtkHardwareConfig) this.mTelDevController.getModem(0)).hasMdAutoSetupImsCapability()) {
            logd("hasMdAutoSetupImsCapability: true");
            return true;
        }
        logd("hasMdAutoSetupImsCapability: false");
        return false;
    }

    private boolean isInSRVCC() {
        return this.mSrvccState == Call.SrvccState.COMPLETED;
    }

    public static boolean isCdmaDualActivationSupport() {
        return SystemProperties.get(RIL_CDMA_DUALACT_SUPPORT).equals("1");
    }

    public static boolean isCdma3GDualModeCard(int i) {
        if (i < 0 || i >= TelephonyManager.getDefault().getPhoneCount()) {
            logd("isCdma3GDualModeCard invalid phoneId = " + i);
            return false;
        }
        String[] strArrSplit = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[i]).split(",");
        if (strArrSplit != null && strArrSplit.length <= 0) {
            return false;
        }
        List listAsList = Arrays.asList(strArrSplit);
        return listAsList.contains("RUIM") && listAsList.contains("SIM");
    }

    public static boolean isCdma3GCard(int i) {
        if (i < 0 || i >= TelephonyManager.getDefault().getPhoneCount()) {
            logd("isCdma3GCard invalid phoneId = " + i);
            return false;
        }
        String[] strArrSplit = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[i]).split(",");
        if (strArrSplit != null && strArrSplit.length <= 0) {
            return false;
        }
        List listAsList = Arrays.asList(strArrSplit);
        return ((!listAsList.contains("RUIM") && !listAsList.contains("CSIM")) || listAsList.contains("SIM") || listAsList.contains("USIM")) ? false : true;
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
