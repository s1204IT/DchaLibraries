package com.mediatek.services.telephony;

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
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;
import com.android.phone.settings.SettingsConstants;
import com.android.services.telephony.Log;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkProxyController;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.internal.telephony.ratconfiguration.RatConfiguration;

public class SwitchPhoneHelper {
    private static final boolean MTK_CT_VOLTE_SUPPORT = SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get("persist.vendor.mtk_ct_volte_support", SettingsConstants.DUAL_VAL_OFF));
    private static final int PROJECT_SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    private Callback mCallback;
    private final Context mContext;
    private EmergencyNumberUtils mEccNumberUtils;
    private String mNumber;
    private Phone mPhone;
    private SimStateReceiver mSimStateReceiver;
    private SimSwitchReceiver mSimSwitchReceiver;
    private SwitchPhoneReceiver mSwitchPhoneReceiver;
    private IMtkTelephonyEx mTelEx;
    private TelephonyManager mTm;
    private boolean mRegisterSwitchPhoneReceiver = false;
    private boolean mRegisterSimStateReceiver = false;
    private boolean mRegisterSimSwitchReceiver = false;
    private boolean mSkipFirstIntent = false;
    private int mTargetPhoneType = 0;
    private int mFailRetryCount = 0;
    private int[] mRadioTechCollected = new int[PROJECT_SIM_NUM];
    private int[] mSimState = new int[PROJECT_SIM_NUM];
    private MtkTelephonyConnectionServiceUtil mMtkTelephonyConnectionServiceUtil = MtkTelephonyConnectionServiceUtil.getInstance();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    SwitchPhoneHelper.this.startSwitchPhoneInternal();
                    break;
                case 2:
                    SwitchPhoneHelper.this.logd("Receives MSG_SWITCH_PHONE_TIMEOUT");
                    SwitchPhoneHelper.this.finish();
                    break;
                case 3:
                    SwitchPhoneHelper.this.logd("Receives MSG_MODE_SWITCH_RESULT");
                    if (((AsyncResult) message.obj).exception == null) {
                        SwitchPhoneHelper.this.logd("Start switching phone");
                        SwitchPhoneHelper.this.startSwitchPhoneTimer();
                        if (!SwitchPhoneHelper.this.mRegisterSwitchPhoneReceiver) {
                            IntentFilter intentFilter = new IntentFilter("android.intent.action.RADIO_TECHNOLOGY");
                            intentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
                            SwitchPhoneHelper.this.mSwitchPhoneReceiver = new SwitchPhoneReceiver();
                            SwitchPhoneHelper.this.mContext.registerReceiver(SwitchPhoneHelper.this.mSwitchPhoneReceiver, intentFilter);
                            SwitchPhoneHelper.this.mRegisterSwitchPhoneReceiver = true;
                        }
                    } else {
                        SwitchPhoneHelper.access$308(SwitchPhoneHelper.this);
                        SwitchPhoneHelper.this.logd("Fail to switch now, failCount=" + SwitchPhoneHelper.this.mFailRetryCount + ", maxCount=10");
                        if (SwitchPhoneHelper.this.mFailRetryCount < 10) {
                            SwitchPhoneHelper.this.mHandler.sendEmptyMessageDelayed(1, 2000L);
                        } else {
                            SwitchPhoneHelper.this.finish();
                        }
                    }
                    break;
                case 4:
                    SwitchPhoneHelper.this.logd("Receives MSG_WAIT_FOR_INTENT_TIMEOUT");
                    if (SwitchPhoneHelper.this.needToSwitchPhone()) {
                        SwitchPhoneHelper.this.startSwitchPhone(SwitchPhoneHelper.this.mCallback);
                    } else {
                        SwitchPhoneHelper.this.finish();
                    }
                    break;
                case 5:
                    SwitchPhoneHelper.this.logd("Start turn off VoLTE!");
                    SwitchPhoneHelper.this.exitCtLteOnlyMode();
                    SwitchPhoneHelper.this.startTurnOffVolteTimer();
                    break;
                case 6:
                    SwitchPhoneHelper.this.logd("MSG_TURN_OFF_VOLTE_TIMEOUT");
                    SwitchPhoneHelper.this.finish();
                    break;
                default:
                    SwitchPhoneHelper.this.logd("Receives unexpected message=" + message.what);
                    break;
            }
        }
    };

    public interface Callback {
        void onComplete(boolean z);
    }

    static int access$308(SwitchPhoneHelper switchPhoneHelper) {
        int i = switchPhoneHelper.mFailRetryCount;
        switchPhoneHelper.mFailRetryCount = i + 1;
        return i;
    }

    public SwitchPhoneHelper(Context context, String str) {
        logd("SwitchPhoneHelper constructor");
        this.mContext = context;
        this.mNumber = str;
        this.mEccNumberUtils = new EmergencyNumberUtils(str);
        this.mTm = TelephonyManager.getDefault();
        this.mTelEx = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
    }

    public boolean needToPrepareForDial() {
        if (!RatConfiguration.isC2kSupported() || "2".equals(SystemProperties.get("persist.vendor.radio.ct.ir.engmode"))) {
            return false;
        }
        if (needToWaitSimState()) {
            return true;
        }
        if (!(ProxyController.getInstance() instanceof MtkProxyController) || !ProxyController.getInstance().isCapabilitySwitching()) {
            return needToSwitchPhone();
        }
        logd("needToPrepareForDial, capability switching");
        return true;
    }

    public void prepareForDial(Callback callback) {
        if (needToWaitSimState()) {
            startExitAirplaneModeAndWaitSimState(callback);
            return;
        }
        if ((ProxyController.getInstance() instanceof MtkProxyController) && ProxyController.getInstance().isCapabilitySwitching()) {
            waitForCapabilitySwitchFinish(callback);
        } else if (needToSwitchPhone()) {
            startSwitchPhone(callback);
        }
    }

    private boolean needToWaitSimState() {
        int i = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
        boolean zIsUnderCryptKeeper = RadioManager.isUnderCryptKeeper();
        for (int i2 = 0; i2 < PROJECT_SIM_NUM; i2++) {
            if (!RadioManager.isModemPowerOff(i2)) {
                this.mSimState[i2] = TelephonyManager.getDefault().getSimState(i2);
            } else {
                this.mSimState[i2] = 0;
            }
        }
        boolean zIsAllSimReady = isAllSimReady();
        logd("needToWaitSimState, airplaneMode = " + i + ", cryptKeeper =" + zIsUnderCryptKeeper + ", allSimReady = " + zIsAllSimReady);
        return (i <= 0 || zIsUnderCryptKeeper || zIsAllSimReady) ? false : true;
    }

    private void startSwitchPhone(Callback callback) {
        this.mPhone = null;
        unregisterReceiver();
        this.mCallback = callback;
        int mainCapabilityPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        logd("startSwitchPhone, mainPhoneId:" + mainCapabilityPhoneId);
        int i = 0;
        if (this.mMtkTelephonyConnectionServiceUtil.hasPerformEccRetry()) {
            int eccPhoneType = this.mMtkTelephonyConnectionServiceUtil.getEccPhoneType();
            logd("startSwitchPhone, previousPhoneType:" + eccPhoneType);
            if (eccPhoneType == 2) {
                this.mTargetPhoneType = 1;
                if (!this.mTm.hasIccCard(mainCapabilityPhoneId)) {
                    this.mPhone = PhoneFactory.getPhone(mainCapabilityPhoneId);
                } else {
                    logd("main phone has card, can't switch!");
                }
            } else {
                this.mTargetPhoneType = 2;
                int i2 = SystemProperties.getInt("persist.vendor.radio.cdma_slot", -1);
                logd("startSwitchPhone, cdmaSlot:" + i2);
                if (i2 != -1) {
                    int i3 = i2 - 1;
                    if (!this.mTm.hasIccCard(i3)) {
                        this.mPhone = PhoneFactory.getPhone(i3);
                        if (MTK_CT_VOLTE_SUPPORT && (this.mEccNumberUtils.isGsmAlwaysNumber() || this.mEccNumberUtils.isGsmOnlyNumber() || this.mEccNumberUtils.isGsmPreferredNumber())) {
                            this.mTargetPhoneType = 1;
                        }
                    } else {
                        if (isInCtLteOnlyMode()) {
                            this.mPhone = PhoneFactory.getPhone(i3);
                            this.mMtkTelephonyConnectionServiceUtil.setEccRetryPhoneId(this.mPhone.getPhoneId());
                            logd("startSwitchPhone, turn off VoLTE for phone" + this.mPhone.getPhoneId() + ", phoneType:" + this.mPhone.getPhoneType() + ", mTargetPhoneType:" + this.mTargetPhoneType);
                            this.mHandler.obtainMessage(5).sendToTarget();
                            return;
                        }
                        while (i < PROJECT_SIM_NUM) {
                            if (!this.mTm.hasIccCard(i)) {
                                this.mPhone = PhoneFactory.getPhone(i);
                            }
                            i++;
                        }
                    }
                } else {
                    while (i < PROJECT_SIM_NUM) {
                        if (!this.mTm.hasIccCard(i)) {
                            this.mPhone = PhoneFactory.getPhone(i);
                        }
                        i++;
                    }
                }
            }
            if (this.mPhone != null) {
                this.mMtkTelephonyConnectionServiceUtil.setEccRetryPhoneId(this.mPhone.getPhoneId());
            }
        } else {
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) > 0) {
                Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
                Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
                intent.putExtra("state", false);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            }
            if (this.mEccNumberUtils.isCdmaAlwaysNumber() || this.mEccNumberUtils.isCdmaPreferredNumber()) {
                int i4 = SystemProperties.getInt("persist.vendor.radio.cdma_slot", -1);
                logd("startSwitchPhone, cdmaSlot:" + i4);
                if (i4 != -1) {
                    int i5 = i4 - 1;
                    if (!this.mTm.hasIccCard(i5)) {
                        this.mPhone = PhoneFactory.getPhone(i5);
                    } else {
                        while (i < PROJECT_SIM_NUM) {
                            if (!this.mTm.hasIccCard(i)) {
                                this.mPhone = PhoneFactory.getPhone(i);
                            }
                            i++;
                        }
                    }
                    this.mTargetPhoneType = 2;
                }
            } else if (this.mEccNumberUtils.isGsmAlwaysNumber() || this.mEccNumberUtils.isGsmOnlyNumber() || this.mEccNumberUtils.isGsmPreferredNumber()) {
                if (!this.mTm.hasIccCard(mainCapabilityPhoneId)) {
                    this.mPhone = PhoneFactory.getPhone(mainCapabilityPhoneId);
                } else {
                    logd("main phone has card, can't switch!");
                }
                this.mTargetPhoneType = 1;
            }
        }
        if (this.mPhone == null) {
            logd("startSwitchPhone, no suitable phone selected to switch!");
            finish();
            return;
        }
        logd("startSwitchPhone with phone" + this.mPhone.getPhoneId() + ", phoneType:" + this.mPhone.getPhoneType() + ", mTargetPhoneType:" + this.mTargetPhoneType);
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    private void startSwitchPhoneInternal() {
        if (!this.mTm.hasIccCard(this.mPhone.getPhoneId())) {
            if (this.mTargetPhoneType == 1) {
                this.mPhone.exitEmergencyCallbackMode();
            }
            if (this.mPhone instanceof MtkGsmCdmaPhone) {
                this.mPhone.triggerModeSwitchByEcc(this.mTargetPhoneType == 2 ? 4 : 1, this.mHandler.obtainMessage(3));
                return;
            }
            return;
        }
        logd("startSwitchPhoneInternal, no need to switch phone!");
        finish();
    }

    private void finish() {
        onComplete(true);
        cleanup();
    }

    private void startSwitchPhoneTimer() {
        cancelSwitchPhoneTimer();
        this.mHandler.sendEmptyMessageDelayed(2, 30000L);
    }

    private void cancelSwitchPhoneTimer() {
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(1);
    }

    public void onDestroy() {
        logd("onDestroy");
        cleanup();
    }

    private void startExitAirplaneModeAndWaitSimState(Callback callback) {
        logd("startExitAirplaneModeAndWaitSimState");
        cleanup();
        this.mCallback = callback;
        IntentFilter intentFilter = new IntentFilter("android.telephony.action.SIM_CARD_STATE_CHANGED");
        this.mSimStateReceiver = new SimStateReceiver();
        this.mContext.registerReceiver(this.mSimStateReceiver, intentFilter);
        this.mRegisterSimStateReceiver = true;
        Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
        Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
        intent.putExtra("state", false);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        this.mHandler.sendEmptyMessageDelayed(4, 30000L);
    }

    private boolean isAllSimReady() {
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (this.mSimState[i] == 0) {
                return false;
            }
        }
        return true;
    }

    private boolean isRoaming() {
        int iccAppFamily;
        int i = SystemProperties.getInt("persist.vendor.radio.cdma_slot", -1);
        if (this.mTelEx != null) {
            try {
                iccAppFamily = this.mTelEx.getIccAppFamily(i - 1);
            } catch (RemoteException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Cannot get appFamily of cdma slot");
                sb.append(i - 1);
                logd(sb.toString());
                iccAppFamily = 0;
            }
        } else {
            iccAppFamily = 0;
        }
        int i2 = i - 1;
        Phone phone = PhoneFactory.getPhone(i2);
        StringBuilder sb2 = new StringBuilder();
        sb2.append("isRoaming, cdmaSlot:");
        sb2.append(i);
        sb2.append(", appFamily:");
        sb2.append(iccAppFamily);
        sb2.append(", phonetype:");
        sb2.append(phone != null ? phone.getPhoneType() : 0);
        logd(sb2.toString());
        if (i == -1 || iccAppFamily == 0 || iccAppFamily == 1 || phone == null || phone.getPhoneType() != 1) {
            return false;
        }
        logd("Card" + i2 + " is roaming");
        return true;
    }

    private boolean hasSuitableCdmaPhone() {
        for (Phone phone : PhoneFactory.getPhones()) {
            if (phone.getPhoneType() == 2) {
                logd("hasSuitableCdmaPhone, phone" + phone.getPhoneId());
                return true;
            }
        }
        return false;
    }

    private boolean hasSuitableGsmPhone() {
        boolean z;
        int i = 0;
        while (true) {
            if (i < PROJECT_SIM_NUM) {
                if (!this.mTm.hasIccCard(i)) {
                    i++;
                } else {
                    z = false;
                    break;
                }
            } else {
                z = true;
                break;
            }
        }
        Phone phone = PhoneFactory.getPhone(RadioCapabilitySwitchUtil.getMainCapabilityPhoneId());
        StringBuilder sb = new StringBuilder();
        sb.append("hasSuitableGsmPhone, noSimInserted:");
        sb.append(z);
        sb.append(", mainPhone type:");
        sb.append(phone != null ? phone.getPhoneType() : 0);
        logd(sb.toString());
        return !z || phone == null || phone.getPhoneType() == 1;
    }

    private boolean hasInServiceGsmPhone() {
        for (Phone phone : PhoneFactory.getPhones()) {
            if (phone.getPhoneType() == 1 && phone.getServiceState().getState() == 0) {
                logd("Phone" + phone.getPhoneId() + " in service");
                return true;
            }
        }
        return false;
    }

    private boolean needToSwitchPhone() {
        if (!RatConfiguration.isC2kSupported()) {
            return false;
        }
        if (this.mMtkTelephonyConnectionServiceUtil.hasPerformEccRetry()) {
            int eccPhoneType = this.mMtkTelephonyConnectionServiceUtil.getEccPhoneType();
            logd("needToSwitchPhone, previousPhoneType:" + eccPhoneType);
            if (!this.mEccNumberUtils.isGsmAlwaysNumber() && !this.mEccNumberUtils.isGsmOnlyNumber() && eccPhoneType == 1 && !hasSuitableCdmaPhone() && (!isRoaming() || isInCtLteOnlyMode())) {
                logd("Need to switch to CDMAPhone");
                return true;
            }
            if (!this.mEccNumberUtils.isCdmaAlwaysNumber() && ((eccPhoneType == 2 && !hasSuitableGsmPhone()) || (eccPhoneType == 1 && isInCtLteOnlyMode()))) {
                logd("Need to switch to GSMPhone");
                return true;
            }
            logd("No need to switch phone");
            return false;
        }
        if ((this.mEccNumberUtils.isGsmAlwaysNumber() || this.mEccNumberUtils.isGsmOnlyNumber() || this.mEccNumberUtils.isGsmPreferredNumber()) && !hasSuitableGsmPhone()) {
            logd("Need to switch to GSMPhone");
            return true;
        }
        if ((this.mEccNumberUtils.isCdmaAlwaysNumber() || (this.mEccNumberUtils.isCdmaPreferredNumber() && !hasInServiceGsmPhone())) && !hasSuitableCdmaPhone() && !isRoaming() && SystemProperties.getInt("vendor.gsm.gcf.testmode", 0) != 2) {
            logd("Need to switch to CDMAPhone");
            return true;
        }
        logd("No need to switch phone");
        return false;
    }

    private void waitForCapabilitySwitchFinish(Callback callback) {
        IntentFilter intentFilter;
        cleanup();
        this.mCallback = callback;
        if (!isAllCdmaCard()) {
            intentFilter = new IntentFilter("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
            intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
        } else {
            for (int i = 0; i < PROJECT_SIM_NUM; i++) {
                this.mRadioTechCollected[i] = 0;
            }
            intentFilter = new IntentFilter("android.intent.action.RADIO_TECHNOLOGY");
        }
        this.mSimSwitchReceiver = new SimSwitchReceiver();
        this.mContext.registerReceiver(this.mSimSwitchReceiver, intentFilter);
        this.mRegisterSimSwitchReceiver = true;
        this.mHandler.sendEmptyMessageDelayed(4, 30000L);
    }

    private void onComplete(boolean z) {
        if (this.mCallback != null) {
            Callback callback = this.mCallback;
            this.mCallback = null;
            callback.onComplete(z);
        }
    }

    private void unregisterReceiver() {
        logd("unregisterReceiver, mRegisterSwitchPhoneReceiver:" + this.mRegisterSwitchPhoneReceiver + ", mRegisterSimStateReceiver:" + this.mRegisterSimStateReceiver + ", mRegisterSimSwitchReceiver:" + this.mRegisterSimSwitchReceiver);
        if (this.mRegisterSwitchPhoneReceiver) {
            try {
                this.mContext.unregisterReceiver(this.mSwitchPhoneReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            this.mRegisterSwitchPhoneReceiver = false;
        }
        if (this.mRegisterSimStateReceiver) {
            try {
                this.mContext.unregisterReceiver(this.mSimStateReceiver);
            } catch (IllegalArgumentException e2) {
                e2.printStackTrace();
            }
            this.mRegisterSimStateReceiver = false;
        }
        if (this.mRegisterSimSwitchReceiver) {
            try {
                this.mContext.unregisterReceiver(this.mSimSwitchReceiver);
            } catch (IllegalArgumentException e3) {
                e3.printStackTrace();
            }
            this.mRegisterSimSwitchReceiver = false;
        }
    }

    private void cleanup() {
        logd("cleanup");
        unregisterReceiver();
        onComplete(false);
        this.mHandler.removeCallbacksAndMessages(null);
        this.mPhone = null;
    }

    private boolean isAllCdmaCard() {
        int i;
        int iccAppFamily;
        if (this.mTelEx != null) {
            for (0; i < PROJECT_SIM_NUM; i + 1) {
                try {
                    iccAppFamily = this.mTelEx.getIccAppFamily(i);
                } catch (RemoteException e) {
                    logd("Cannot get appFamily of slot" + i);
                }
                i = (iccAppFamily == 0 || iccAppFamily == 1) ? 0 : i + 1;
                logd("appFamily of slot" + i + " is " + iccAppFamily);
                return false;
            }
        }
        return true;
    }

    private boolean isAllPhoneReady() {
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (this.mRadioTechCollected[i] != 1) {
                return false;
            }
        }
        return true;
    }

    private void logd(String str) {
        Log.i(this, str, new Object[0]);
    }

    private class SwitchPhoneReceiver extends BroadcastReceiver {
        private SwitchPhoneReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            SwitchPhoneHelper.this.logd("Received:" + action);
            if (SwitchPhoneHelper.this.mPhone != null) {
                SwitchPhoneHelper.this.logd("Service state:" + SwitchPhoneHelper.this.mPhone.getServiceState().getState() + ", phoneType:" + SwitchPhoneHelper.this.mPhone.getPhoneType() + ", mTargetPhoneType:" + SwitchPhoneHelper.this.mTargetPhoneType + ", phoneId:" + SwitchPhoneHelper.this.mPhone.getPhoneId() + ", hasIccCard:" + SwitchPhoneHelper.this.mTm.hasIccCard(SwitchPhoneHelper.this.mPhone.getPhoneId()));
                if ("android.intent.action.RADIO_TECHNOLOGY".equals(action)) {
                    if (SwitchPhoneHelper.this.mPhone.getPhoneType() == SwitchPhoneHelper.this.mTargetPhoneType) {
                        SwitchPhoneHelper.this.logd("Switch to target phone!");
                        SwitchPhoneHelper.this.cancelSwitchPhoneTimer();
                        SwitchPhoneHelper.this.finish();
                        return;
                    }
                    return;
                }
                if ("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED".equals(action) && SwitchPhoneHelper.this.mTm.hasIccCard(SwitchPhoneHelper.this.mPhone.getPhoneId())) {
                    SwitchPhoneHelper.this.logd("No need to switch phone anymore!");
                    SwitchPhoneHelper.this.cancelSwitchPhoneTimer();
                    SwitchPhoneHelper.this.finish();
                }
            }
        }
    }

    private class SimStateReceiver extends BroadcastReceiver {
        private SimStateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int intExtra = intent.getIntExtra("phone", -1);
            int intExtra2 = intent.getIntExtra("android.telephony.extra.SIM_STATE", 0);
            SwitchPhoneHelper.this.logd("Received ACTION_SIM_CARD_STATE_CHANGED, slotId:" + intExtra + ", simState:" + intExtra2);
            if (intExtra != -1) {
                SwitchPhoneHelper.this.mSimState[intExtra] = intExtra2;
            }
            if (SwitchPhoneHelper.this.isAllSimReady()) {
                SwitchPhoneHelper.this.mHandler.removeMessages(4);
                if (SwitchPhoneHelper.this.needToSwitchPhone()) {
                    SwitchPhoneHelper.this.startSwitchPhone(SwitchPhoneHelper.this.mCallback);
                } else {
                    SwitchPhoneHelper.this.finish();
                }
            }
        }
    }

    private class SimSwitchReceiver extends BroadcastReceiver {
        private SimSwitchReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.RADIO_TECHNOLOGY".equals(action)) {
                int intExtra = intent.getIntExtra("phone", -1);
                SwitchPhoneHelper.this.logd("Received ACTION_RADIO_TECHNOLOGY_CHANGED, slotId:" + intExtra + ", mSkipFirstIntent:" + SwitchPhoneHelper.this.mSkipFirstIntent);
                if (!SwitchPhoneHelper.this.mSkipFirstIntent) {
                    SwitchPhoneHelper.this.mSkipFirstIntent = true;
                    return;
                }
                if (intExtra != -1) {
                    SwitchPhoneHelper.this.mRadioTechCollected[intExtra] = 1;
                }
                if (SwitchPhoneHelper.this.isAllPhoneReady()) {
                    SwitchPhoneHelper.this.mHandler.removeMessages(4);
                    if (SwitchPhoneHelper.this.needToSwitchPhone()) {
                        SwitchPhoneHelper.this.startSwitchPhone(SwitchPhoneHelper.this.mCallback);
                        return;
                    } else {
                        SwitchPhoneHelper.this.finish();
                        return;
                    }
                }
                return;
            }
            SwitchPhoneHelper.this.logd("Received " + action);
            SwitchPhoneHelper.this.mHandler.removeMessages(4);
            if (SwitchPhoneHelper.this.needToSwitchPhone()) {
                SwitchPhoneHelper.this.startSwitchPhone(SwitchPhoneHelper.this.mCallback);
            } else {
                SwitchPhoneHelper.this.finish();
            }
        }
    }

    private boolean isInCtLteOnlyMode() {
        int iccAppFamily;
        boolean zIsVolteEnabledByPlatform;
        boolean zIsEnhanced4gLteModeSettingEnabledByUser;
        if (!MTK_CT_VOLTE_SUPPORT) {
            return false;
        }
        int mainCapabilityPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
        int i = SystemProperties.getInt("persist.vendor.radio.cdma_slot", -1);
        try {
            iccAppFamily = this.mTelEx.getIccAppFamily(i - 1);
        } catch (RemoteException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Cannot get appFamily of cdma slot");
            sb.append(i - 1);
            logd(sb.toString());
            iccAppFamily = 0;
        }
        Phone phone = PhoneFactory.getPhone(i - 1);
        boolean z = true;
        if (i == -1 || ((iccAppFamily != 0 && iccAppFamily < 2) || phone == null || phone.getPhoneType() != 1)) {
            zIsVolteEnabledByPlatform = false;
            z = false;
            zIsEnhanced4gLteModeSettingEnabledByUser = false;
        } else {
            zIsVolteEnabledByPlatform = ImsManager.getInstance(phone.getContext(), phone.getPhoneId()).isVolteEnabledByPlatform();
            zIsEnhanced4gLteModeSettingEnabledByUser = MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(phone.getContext(), phone.getPhoneId());
            if (!zIsVolteEnabledByPlatform || !zIsEnhanced4gLteModeSettingEnabledByUser) {
                z = false;
            }
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("isInCtLteOnlyMode, mainPhoneId=");
        sb2.append(mainCapabilityPhoneId);
        sb2.append(", cdmaSlot=");
        sb2.append(i);
        sb2.append(", appFamily=");
        sb2.append(iccAppFamily);
        sb2.append(", phoneType=");
        sb2.append(phone != null ? phone.getPhoneType() : 0);
        sb2.append(", volteSupport=");
        sb2.append(zIsVolteEnabledByPlatform);
        sb2.append(", volteSetting=");
        sb2.append(zIsEnhanced4gLteModeSettingEnabledByUser);
        sb2.append(", ctLteOnlyMode=");
        sb2.append(z);
        logd(sb2.toString());
        return z;
    }

    private void exitCtLteOnlyMode() {
        int i = SystemProperties.getInt("persist.vendor.radio.cdma_slot", -1);
        Phone phone = PhoneFactory.getPhone(i - 1);
        if (i != -1 && phone != null) {
            MtkImsManager.setEnhanced4gLteModeSetting(phone.getContext(), false, phone.getPhoneId());
        }
        logd("exitCtLteOnlyMode, cdmaSlot=" + i);
    }

    private void startTurnOffVolteTimer() {
        cancelTurnOffVolteTimer();
        this.mHandler.sendEmptyMessageDelayed(6, 5000L);
    }

    private void cancelTurnOffVolteTimer() {
        this.mHandler.removeMessages(6);
        this.mHandler.removeMessages(5);
    }
}
