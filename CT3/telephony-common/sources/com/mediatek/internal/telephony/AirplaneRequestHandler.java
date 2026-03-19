package com.mediatek.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;
import java.util.concurrent.atomic.AtomicBoolean;

public class AirplaneRequestHandler extends Handler {

    private static final int[] f39x22a12f3f = null;
    private static final int EVENT_GSM_RADIO_CHANGE_FOR_AVALIABLE = 101;
    private static final int EVENT_GSM_RADIO_CHANGE_FOR_OFF = 100;
    private static final int EVENT_SET_MODEM_POWER_DONE = 104;
    private static final int EVENT_WAIT_RADIO_CHANGE_FOR_AVALIABLE = 102;
    private static final int EVENT_WAIT_RADIO_CHANGE_UNAVALIABLE_TO_AVALIABLE = 103;
    private static final String LOG_TAG = "AirplaneHandler";
    private static AtomicBoolean sInSwitching = new AtomicBoolean(false);
    private Context mContext;
    private boolean mForceSwitch;
    private ModemPowerMessage[] mModemPowerMessages;
    private boolean mNeedIgnoreMessageForChangeDone;
    private boolean mNeedIgnoreMessageForWait;
    private Boolean mPendingAirplaneModeRequest;
    private int mPhoneCount;
    private boolean mPowerModem = true;
    private boolean mIsRadioUnavailable = false;
    private boolean mHasRegisterWorldModeReceiver = false;
    private BroadcastReceiver mWorldModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!"android.intent.action.ACTION_WORLD_MODE_CHANGED".equals(action)) {
                return;
            }
            int wmState = intent.getIntExtra("worldModeState", -1);
            if (!AirplaneRequestHandler.this.mHasRegisterWorldModeReceiver || wmState != 1) {
                return;
            }
            AirplaneRequestHandler.this.unRegisterWorldModeReceiver();
            AirplaneRequestHandler.sInSwitching.set(false);
            AirplaneRequestHandler.this.checkPendingRequest();
        }
    };

    private static int[] m550xa4bc86e3() {
        if (f39x22a12f3f != null) {
            return f39x22a12f3f;
        }
        int[] iArr = new int[TelephonyManager.MultiSimVariants.values().length];
        try {
            iArr[TelephonyManager.MultiSimVariants.DSDA.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[TelephonyManager.MultiSimVariants.DSDS.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[TelephonyManager.MultiSimVariants.TSTS.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[TelephonyManager.MultiSimVariants.UNKNOWN.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        f39x22a12f3f = iArr;
        return iArr;
    }

    protected boolean allowSwitching() {
        if (sInSwitching.get() && !this.mForceSwitch) {
            return false;
        }
        return true;
    }

    protected void pendingAirplaneModeRequest(boolean enabled) {
        log("pendingAirplaneModeRequest, enabled = " + enabled);
        this.mPendingAirplaneModeRequest = new Boolean(enabled);
    }

    public AirplaneRequestHandler(Context context, int phoneCount) {
        this.mContext = context;
        this.mPhoneCount = phoneCount;
    }

    protected void monitorRadioChangeDone(boolean power) {
        this.mNeedIgnoreMessageForChangeDone = false;
        sInSwitching.set(true);
        for (int i = 0; i < this.mPhoneCount; i++) {
            Phone phone = PhoneFactory.getPhone(i);
            if (phone != null) {
                if (power) {
                    phone.mCi.registerForRadioStateChanged(this, 101, null);
                } else {
                    phone.mCi.registerForRadioStateChanged(this, 100, null);
                }
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        boolean isWifiOnly = isWifiOnly();
        switch (msg.what) {
            case 100:
                if (!this.mNeedIgnoreMessageForChangeDone) {
                    if (msg.what == 100) {
                        log("handle EVENT_GSM_RADIO_CHANGE_FOR_OFF");
                    }
                    int i = 0;
                    while (true) {
                        if (i < this.mPhoneCount) {
                            Phone phone = PhoneFactory.getPhone(i);
                            if (phone != null) {
                                if (isWifiOnly) {
                                    log("wifi-only, don't judge radio off");
                                } else if (isRadioOff(i)) {
                                    i++;
                                }
                            }
                        }
                    }
                }
                break;
            case 101:
                if (!this.mNeedIgnoreMessageForChangeDone) {
                    if (msg.what == 101) {
                        log("handle EVENT_GSM_RADIO_CHANGE_FOR_AVALIABLE");
                    }
                    int i2 = 0;
                    while (true) {
                        if (i2 < this.mPhoneCount) {
                            Phone phone2 = PhoneFactory.getPhone(i2);
                            if (phone2 != null) {
                                if (isWifiOnly) {
                                    log("wifi-only, don't judge radio avaliable");
                                } else if (isRadioAvaliable(i2)) {
                                    i2++;
                                }
                            }
                            break;
                        }
                    }
                    sInSwitching.set(false);
                    unMonitorAirplaneChangeDone(false);
                    checkPendingRequest();
                }
                break;
            case 102:
                if (!this.mNeedIgnoreMessageForWait) {
                    log("handle EVENT_WAIT_RADIO_CHANGE_FOR_AVALIABLE");
                    if (isRadioAvaliable()) {
                        unWaitRadioAvaliable();
                        sInSwitching.set(false);
                        checkPendingRequest();
                        break;
                    }
                }
                break;
            case EVENT_WAIT_RADIO_CHANGE_UNAVALIABLE_TO_AVALIABLE:
                log("handle EVENT_WAIT_RADIO_CHANGE_UNAVALIABLE_TO_AVALIABLE");
                if (!this.mNeedIgnoreMessageForChangeDone && this.mPowerModem) {
                    if (isWifiOnly) {
                        log("wifi-only, don't judge radio avaliable");
                    } else if (!isRadioOn()) {
                        if (!this.mIsRadioUnavailable) {
                            if (isRadioUnavailable()) {
                                log("All radio unavaliable");
                                this.mIsRadioUnavailable = true;
                            }
                            break;
                        } else if (!isRadioAvaliable()) {
                        }
                    }
                    sInSwitching.set(false);
                    unMonitorAirplaneChangeDone(false);
                    checkPendingRequest();
                    break;
                }
                break;
            case 104:
                log("[SMP]handle EVENT_SET_MODEM_POWER_DONE -> " + (this.mPowerModem ? "ON" : "OFF"));
                if (!this.mPowerModem) {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    ModemPowerMessage message = (ModemPowerMessage) ar.userObj;
                    log("[SMP]handleModemPowerMessage, message:" + message);
                    if (ar.exception == null) {
                        if (ar.result != null) {
                            log("[SMP]handleModemPowerMessage, result:" + ar.result);
                        }
                    } else {
                        log("[SMP]handleModemPowerMessage, Unhandle ar.exception:" + ar.exception);
                    }
                    message.isFinish = true;
                    if (isSetModemPowerFinish()) {
                        cleanModemPowerMessage();
                        sInSwitching.set(false);
                        unMonitorAirplaneChangeDone(true);
                        checkPendingRequest();
                        break;
                    }
                }
                break;
        }
    }

    private boolean isRadioAvaliable(int phoneId) {
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null) {
            return false;
        }
        log("phoneId = " + phoneId + ", RadioState=" + phone.mCi.getRadioState());
        return phone.mCi.getRadioState() != CommandsInterface.RadioState.RADIO_UNAVAILABLE;
    }

    private boolean isRadioOff(int phoneId) {
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null) {
            return false;
        }
        log("phoneId = " + phoneId + ", RadioState=" + phone.mCi.getRadioState());
        return phone.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF;
    }

    private boolean isRadioOn(int phoneId) {
        Phone phone = PhoneFactory.getPhone(phoneId);
        return phone != null && phone.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_ON;
    }

    private void checkPendingRequest() {
        log("checkPendingRequest, mPendingAirplaneModeRequest = " + this.mPendingAirplaneModeRequest);
        if (this.mPendingAirplaneModeRequest == null) {
            return;
        }
        Boolean pendingAirplaneModeRequest = this.mPendingAirplaneModeRequest;
        this.mPendingAirplaneModeRequest = null;
        RadioManager.getInstance().notifyAirplaneModeChange(pendingAirplaneModeRequest.booleanValue());
    }

    protected Boolean hasPendingAirplaneRequest() {
        log("hasPendingAirplaneRequest = " + this.mPendingAirplaneModeRequest);
        return this.mPendingAirplaneModeRequest;
    }

    protected void unMonitorAirplaneChangeDone(boolean airplaneMode) {
        this.mNeedIgnoreMessageForChangeDone = true;
        Intent intent = new Intent("com.mediatek.intent.action.AIRPLANE_CHANGE_DONE");
        intent.putExtra("airplaneMode", airplaneMode);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        for (int i = 0; i < this.mPhoneCount; i++) {
            Phone phone = PhoneFactory.getPhone(i);
            if (phone != null) {
                phone.mCi.unregisterForRadioStateChanged(this);
                log("unMonitorAirplaneChangeDone, for gsm phone,  phoneId = " + i);
            }
        }
    }

    public void setForceSwitch(boolean forceSwitch) {
        this.mForceSwitch = forceSwitch;
        log("setForceSwitch, forceSwitch =" + forceSwitch);
    }

    protected boolean waitForReady(boolean enabled) {
        return waitRadioAvaliable(enabled) || waitWorlModeSwitching(enabled);
    }

    private boolean waitRadioAvaliable(boolean enabled) {
        boolean wait = (!isCdmaLteDcSupport() || isWifiOnly() || isRadioAvaliable()) ? false : true;
        log("waitRadioAvaliable, enabled=" + enabled + ", wait=" + wait);
        if (wait) {
            pendingAirplaneModeRequest(enabled);
            this.mNeedIgnoreMessageForWait = false;
            sInSwitching.set(true);
            for (int i = 0; i < this.mPhoneCount; i++) {
                Phone phone = PhoneFactory.getPhone(i);
                if (phone != null) {
                    phone.mCi.registerForRadioStateChanged(this, 102, null);
                }
            }
        }
        return wait;
    }

    private void unWaitRadioAvaliable() {
        this.mNeedIgnoreMessageForWait = true;
        for (int i = 0; i < this.mPhoneCount; i++) {
            Phone phone = PhoneFactory.getPhone(i);
            if (phone != null) {
                phone.mCi.unregisterForRadioStateChanged(this);
                log("unWaitRadioAvaliable, for gsm phone,  phoneId = " + i);
            }
        }
    }

    private boolean isRadioOn() {
        for (int i = 0; i < this.mPhoneCount; i++) {
            if (!isRadioOn(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean isRadioAvaliable() {
        for (int i = 0; i < this.mPhoneCount; i++) {
            if (!isRadioAvaliable(i)) {
                log("isRadioAvaliable=false, phoneId = " + i);
                return false;
            }
        }
        return true;
    }

    private boolean isRadioUnavailable() {
        for (int i = 0; i < this.mPhoneCount; i++) {
            if (isRadioAvaliable(i)) {
                log("isRadioUnavailable=false, phoneId = " + i);
                return false;
            }
        }
        return true;
    }

    private boolean waitWorlModeSwitching(boolean enabled) {
        boolean zIsWorldPhoneSwitching = (!isCdmaLteDcSupport() || isWifiOnly()) ? false : WorldPhoneUtil.isWorldPhoneSwitching();
        log("waitWorlModeSwitching, enabled=" + enabled + ", wait=" + zIsWorldPhoneSwitching);
        if (zIsWorldPhoneSwitching) {
            pendingAirplaneModeRequest(enabled);
            sInSwitching.set(true);
            if (!this.mHasRegisterWorldModeReceiver) {
                registerWorldModeReceiver();
            }
        }
        return zIsWorldPhoneSwitching;
    }

    private void registerWorldModeReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_WORLD_MODE_CHANGED");
        this.mContext.registerReceiver(this.mWorldModeReceiver, filter);
        this.mHasRegisterWorldModeReceiver = true;
    }

    private void unRegisterWorldModeReceiver() {
        this.mContext.unregisterReceiver(this.mWorldModeReceiver);
        this.mHasRegisterWorldModeReceiver = false;
    }

    private boolean isWifiOnly() {
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        return !cm.isNetworkSupported(0);
    }

    private static final boolean isCdmaLteDcSupport() {
        if (SystemProperties.get("ro.boot.opt_c2k_lte_mode").equals("1") || SystemProperties.get("ro.boot.opt_c2k_lte_mode").equals(Phone.ACT_TYPE_UTRAN)) {
            return true;
        }
        return false;
    }

    private static void log(String s) {
        Rlog.d(LOG_TAG, "[RadioManager] " + s);
    }

    protected final Message[] monitorModemPowerChangeDone(boolean power, int phoneBitMap, int mainCapabilityPhoneId) {
        this.mPowerModem = power;
        log("[SMP]monitorModemPowerChangeDone, Power:" + power + ", PhoneBitMap:" + phoneBitMap + ", mainCapabilityPhoneId:" + mainCapabilityPhoneId + ", mPhoneCount:" + this.mPhoneCount);
        this.mNeedIgnoreMessageForChangeDone = false;
        this.mIsRadioUnavailable = false;
        sInSwitching.set(true);
        Message[] msgs = new Message[this.mPhoneCount];
        if (this.mPowerModem) {
            for (int i = 0; i < this.mPhoneCount; i++) {
                Phone phone = PhoneFactory.getPhone(i);
                if (phone != null) {
                    phone.mCi.registerForRadioStateChanged(this, EVENT_WAIT_RADIO_CHANGE_UNAVALIABLE_TO_AVALIABLE, null);
                }
            }
        } else {
            ModemPowerMessage[] messages = createMessage(power, phoneBitMap, mainCapabilityPhoneId, this.mPhoneCount);
            this.mModemPowerMessages = messages;
            for (int i2 = 0; i2 < messages.length; i2++) {
                if (messages[i2] != null) {
                    msgs[i2] = obtainMessage(104, messages[i2]);
                }
            }
        }
        return msgs;
    }

    private final boolean isSetModemPowerFinish() {
        if (this.mModemPowerMessages != null) {
            for (int i = 0; i < this.mModemPowerMessages.length; i++) {
                if (this.mModemPowerMessages[i] != null) {
                    log("[SMP]isSetModemPowerFinish [" + i + "]: " + this.mModemPowerMessages[i]);
                    if (!this.mModemPowerMessages[i].isFinish) {
                        return false;
                    }
                } else {
                    log("[SMP]isSetModemPowerFinish [" + i + "]: MPMsg is null");
                }
            }
            return true;
        }
        return true;
    }

    private final void cleanModemPowerMessage() {
        log("[SMP]cleanModemPowerMessage");
        if (this.mModemPowerMessages == null) {
            return;
        }
        for (int i = 0; i < this.mModemPowerMessages.length; i++) {
            this.mModemPowerMessages[i] = null;
        }
        this.mModemPowerMessages = null;
    }

    private static final ModemPowerMessage[] createMessage(boolean power, int phoneBitMap, int mainCapabilityPhoneId, int phoneCount) {
        TelephonyManager.MultiSimVariants config = TelephonyManager.getDefault().getMultiSimConfiguration();
        log("[SMP]createMessage, config:" + config);
        ModemPowerMessage[] msgs = new ModemPowerMessage[phoneCount];
        switch (m550xa4bc86e3()[config.ordinal()]) {
            case 1:
                for (int i = 0; i < phoneCount; i++) {
                    int phoneId = i;
                    if (((1 << i) & phoneBitMap) != 0) {
                        log("[SMP]createMessage, Power:" + power + ", phoneId:" + phoneId);
                        msgs[phoneId] = new ModemPowerMessage(phoneId);
                    }
                }
                break;
            case 2:
            case 3:
                msgs[mainCapabilityPhoneId] = new ModemPowerMessage(mainCapabilityPhoneId);
                break;
            default:
                int phoneId2 = PhoneFactory.getDefaultPhone().getPhoneId();
                msgs[phoneId2] = new ModemPowerMessage(phoneId2);
                break;
        }
        for (int i2 = 0; i2 < phoneCount; i2++) {
            if (msgs[i2] != null) {
                log("[SMP]createMessage, [" + i2 + "]: " + msgs[i2].toString());
            }
        }
        return msgs;
    }

    private static final class ModemPowerMessage {
        public boolean isFinish = false;
        private final int mPhoneId;

        public ModemPowerMessage(int phoneId) {
            this.mPhoneId = phoneId;
        }

        public String toString() {
            return "MPMsg [mPhoneId=" + this.mPhoneId + ", isFinish=" + this.isFinish + "]";
        }
    }
}
