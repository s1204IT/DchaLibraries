package com.mediatek.internal.telephony.dataconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.AsyncResult;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.util.SparseArray;
import android.view.Display;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SubscriptionController;
import java.util.ArrayList;

public class FdManager extends Handler {
    private static final boolean DBG = true;
    private static final String DEFAULT_FD_SCREEN_OFF_R8_TIMER = "50";
    private static final String DEFAULT_FD_SCREEN_OFF_TIMER = "50";
    private static final String DEFAULT_FD_SCREEN_ON_R8_TIMER = "150";
    private static final String DEFAULT_FD_SCREEN_ON_TIMER = "150";
    private static final int EVENT_BASE = 0;
    private static final int EVENT_FD_MODE_SET = 0;
    private static final int EVENT_RADIO_ON = 1;
    private static final int FD_MODE = 1;
    private static final boolean IN_CHARGING = true;
    private static final String LOG_TAG = "FdManager";
    private static final boolean MTK_FD_SUPPORT;
    private static final boolean NOT_IN_CHARGING = false;
    private static final String PROPERTY_FD_ON_CHARGE = "persist.vendor.fd.on.charge";
    private static final String PROPERTY_FD_SCREEN_OFF_ONLY = "vendor.fd.screen.off.only";
    private static final String PROPERTY_RIL_FD_MODE = "vendor.ril.fd.mode";
    private static final String STR_PROPERTY_FD_SCREEN_OFF_R8_TIMER = "persist.vendor.radio.fd.off.r8.counter";
    private static final String STR_PROPERTY_FD_SCREEN_OFF_TIMER = "persist.vendor.radio.fd.off.counter";
    private static final String STR_PROPERTY_FD_SCREEN_ON_R8_TIMER = "persist.vendor.radio.fd.r8.counter";
    private static final String STR_PROPERTY_FD_SCREEN_ON_TIMER = "persist.vendor.radio.fd.counter";
    private static final SparseArray<FdManager> sFdManagers;
    private static String[] sTimerValue;
    private DisplayManager mDisplayManager;
    private boolean mIsCharging;
    private Phone mPhone;
    private boolean mIsTetheredMode = false;
    private int mEnableFdOnCharing = Integer.parseInt(SystemProperties.get(PROPERTY_FD_ON_CHARGE, "0"));
    private boolean mIsScreenOn = true;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte b;
            String action = intent.getAction();
            int iHashCode = action.hashCode();
            if (iHashCode != -1754841973) {
                if (iHashCode != -54942926) {
                    if (iHashCode != -25388475) {
                        b = (iHashCode == 948344062 && action.equals("android.os.action.CHARGING")) ? (byte) 0 : (byte) -1;
                    } else if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                        b = 3;
                    }
                } else if (action.equals("android.os.action.DISCHARGING")) {
                    b = 1;
                }
            } else if (action.equals("android.net.conn.TETHER_STATE_CHANGED")) {
                b = 2;
            }
            switch (b) {
                case 0:
                    FdManager.this.logd("mIntentReceiver: Received ACTION_CHARGING");
                    FdManager.this.onChargingModeSwitched(true);
                    break;
                case 1:
                    FdManager.this.logd("mIntentReceiver: Received ACTION_DISCHARGING");
                    FdManager.this.onChargingModeSwitched(false);
                    break;
                case 2:
                    ArrayList<String> stringArrayListExtra = intent.getStringArrayListExtra("tetherArray");
                    FdManager.this.mIsTetheredMode = stringArrayListExtra != null && stringArrayListExtra.size() > 0;
                    FdManager.this.logd("mIntentReceiver: Received ACTION_TETHER_STATE_CHANGED mIsTetheredMode = " + FdManager.this.mIsTetheredMode);
                    FdManager.this.onTetheringSwitched();
                    break;
                case 3:
                    FdManager.this.logd("mIntentReceiver: Received ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
                    FdManager.this.onDefaultDataSwitched();
                    break;
                default:
                    FdManager.this.logw("mIntentReceiver: weird, should never be here!");
                    break;
            }
        }
    };
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int i) {
        }

        @Override
        public void onDisplayRemoved(int i) {
        }

        @Override
        public void onDisplayChanged(int i) {
            FdManager.this.onScreenSwitched(FdManager.this.isScreenOn());
        }
    };

    private enum FdModeType {
        DISABLE_MD_FD,
        ENABLE_MD_FD,
        SET_FD_INACTIVITY_TIMER,
        INFO_MD_SCREEN_STATUS
    }

    private enum FdTimerType {
        SCREEN_OFF_LEGACY_FD,
        SCREEN_ON_LEGACY_FD,
        SCREEN_OFF_R8_FD,
        SCREEN_ON_R8_FD,
        SUPPORT_TIMER_TYPES
    }

    static {
        MTK_FD_SUPPORT = Integer.parseInt(SystemProperties.get("ro.vendor.mtk_fd_support", "0")) == 1;
        sFdManagers = new SparseArray<>();
        sTimerValue = new String[]{"50", "150", "50", "150"};
    }

    public static FdManager getInstance(Phone phone) {
        if (MTK_FD_SUPPORT && phone != null) {
            int phoneId = getPhoneId(phone);
            if (!SubscriptionManager.isValidPhoneId(phoneId)) {
                Rlog.e(LOG_TAG, "phoneId " + phoneId + " is invalid!");
                return null;
            }
            FdManager fdManager = sFdManagers.get(phoneId);
            if (fdManager == null) {
                Rlog.d(LOG_TAG, "FdManager " + phoneId + " doesn't exist, create one");
                FdManager fdManager2 = new FdManager(phone);
                sFdManagers.put(phoneId, fdManager2);
                return fdManager2;
            }
            return fdManager;
        }
        Rlog.e(LOG_TAG, "Fast dormancy feature is not enabled or FdManager initialize fail");
        return null;
    }

    private FdManager(Phone phone) {
        this.mIsCharging = false;
        this.mPhone = phone;
        this.mIsCharging = isDeviceCharging();
        logd("Initial FdManager: mIsCharging = " + this.mIsCharging);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.os.action.CHARGING");
        intentFilter.addAction("android.os.action.DISCHARGING");
        intentFilter.addAction("android.net.conn.TETHER_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        this.mPhone.getContext().registerReceiver(this.mIntentReceiver, intentFilter, null, this.mPhone);
        this.mPhone.mCi.registerForOn(this, 1, (Object) null);
        this.mDisplayManager = (DisplayManager) this.mPhone.getContext().getSystemService("display");
        this.mDisplayManager.registerDisplayListener(this.mDisplayListener, null);
        initFdTimer();
    }

    public void dispose() {
        logd("Dispose FdManager");
        if (MTK_FD_SUPPORT) {
            this.mPhone.getContext().unregisterReceiver(this.mIntentReceiver);
            this.mPhone.mCi.unregisterForOn(this);
            this.mDisplayManager.unregisterDisplayListener(this.mDisplayListener);
            sFdManagers.remove(getPhoneId(this.mPhone));
        }
    }

    private void initFdTimer() {
        sTimerValue[FdTimerType.SCREEN_OFF_LEGACY_FD.ordinal()] = Integer.toString((int) Double.parseDouble(strArr[0]));
        sTimerValue[FdTimerType.SCREEN_ON_LEGACY_FD.ordinal()] = Integer.toString((int) Double.parseDouble(strArr[1]));
        sTimerValue[FdTimerType.SCREEN_OFF_R8_FD.ordinal()] = Integer.toString((int) Double.parseDouble(strArr[2]));
        String[] strArr = {SystemProperties.get(STR_PROPERTY_FD_SCREEN_OFF_TIMER, "50"), SystemProperties.get(STR_PROPERTY_FD_SCREEN_ON_TIMER, "150"), SystemProperties.get(STR_PROPERTY_FD_SCREEN_OFF_R8_TIMER, "50"), SystemProperties.get(STR_PROPERTY_FD_SCREEN_ON_R8_TIMER, "150")};
        sTimerValue[FdTimerType.SCREEN_ON_R8_FD.ordinal()] = Integer.toString((int) Double.parseDouble(strArr[3]));
        logd("initFdTimer: timers = " + sTimerValue[0] + ", " + sTimerValue[1] + ", " + sTimerValue[2] + ", " + sTimerValue[3]);
    }

    public int getNumberOfSupportedTypes() {
        return FdTimerType.SUPPORT_TIMER_TYPES.ordinal();
    }

    public int setFdTimerValue(String[] strArr, Message message) {
        int i = Integer.parseInt(SystemProperties.get(PROPERTY_RIL_FD_MODE, "0"));
        if (MTK_FD_SUPPORT && i == 1 && isFdAllowed()) {
            for (int i2 = 0; i2 < strArr.length; i2++) {
                sTimerValue[i2] = strArr[i2];
            }
            this.mPhone.mCi.setFdMode(FdModeType.SET_FD_INACTIVITY_TIMER.ordinal(), FdTimerType.SCREEN_OFF_LEGACY_FD.ordinal(), Integer.parseInt(sTimerValue[FdTimerType.SCREEN_OFF_LEGACY_FD.ordinal()]), null);
            this.mPhone.mCi.setFdMode(FdModeType.SET_FD_INACTIVITY_TIMER.ordinal(), FdTimerType.SCREEN_ON_LEGACY_FD.ordinal(), Integer.parseInt(sTimerValue[FdTimerType.SCREEN_ON_LEGACY_FD.ordinal()]), null);
            this.mPhone.mCi.setFdMode(FdModeType.SET_FD_INACTIVITY_TIMER.ordinal(), FdTimerType.SCREEN_OFF_R8_FD.ordinal(), Integer.parseInt(sTimerValue[FdTimerType.SCREEN_OFF_R8_FD.ordinal()]), null);
            this.mPhone.mCi.setFdMode(FdModeType.SET_FD_INACTIVITY_TIMER.ordinal(), FdTimerType.SCREEN_ON_R8_FD.ordinal(), Integer.parseInt(sTimerValue[FdTimerType.SCREEN_ON_R8_FD.ordinal()]), message);
            logd("setFdTimerValue: sTimerValue = " + sTimerValue[0] + ", " + sTimerValue[1] + ", " + sTimerValue[2] + ", " + sTimerValue[3]);
        }
        return 0;
    }

    public int setFdTimerValue(String[] strArr, Message message, Phone phone) {
        FdManager fdManager = getInstance(phone);
        if (fdManager != null) {
            fdManager.setFdTimerValue(strArr, message);
            return 0;
        }
        logw("setFdTimerValue: fail!");
        return 0;
    }

    public static String[] getFdTimerValue() {
        return sTimerValue;
    }

    @Override
    public void handleMessage(Message message) {
        logd("handleMessage: msg.what = " + message.what);
        switch (message.what) {
            case 0:
                if (((AsyncResult) message.obj).exception != null) {
                    loge("handleMessage: RIL_REQUEST_SET_FD_MODE error!");
                }
                break;
            case 1:
                onRadioOn();
                break;
            default:
                logw("handleMessage: weird, should never be here!");
                break;
        }
    }

    private boolean isScreenOn() {
        Display[] displays = ((DisplayManager) this.mPhone.getContext().getSystemService("display")).getDisplays();
        if (displays != null) {
            for (Display display : displays) {
                if (display.getState() == 2) {
                    logd("isScreenOn: Screen " + Display.typeToString(display.getType()) + " on");
                    return true;
                }
            }
            logd("isScreenOn: Screens all off");
            return false;
        }
        logd("isScreenOn: No displays found");
        return false;
    }

    private boolean isDeviceCharging() {
        return ((BatteryManager) this.mPhone.getContext().getSystemService("batterymanager")).isCharging();
    }

    private boolean isDefaultDataSubId(int i) {
        int defaultDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
        logd("isDefaultDataSubId: subId = " + i + " dataSubId = " + defaultDataSubId);
        return SubscriptionManager.isUsableSubIdValue(i) && i == defaultDataSubId;
    }

    private boolean isRadioCapabilityValid(Phone phone) {
        return (phone.getRadioAccessFamily() & 16384) == 16384 || (phone.getRadioAccessFamily() & 8) == 8;
    }

    private boolean isFdAllowed() {
        return isRadioCapabilityValid(this.mPhone) && isDefaultDataSubId(this.mPhone.getSubId());
    }

    private boolean shouldEnableFd() {
        if (isFdEnabledOnlyWhenScreenOff() && this.mIsScreenOn) {
            return false;
        }
        if ((!this.mIsCharging || this.mEnableFdOnCharing != 0) && !this.mIsTetheredMode) {
            return true;
        }
        return false;
    }

    private void updateFdModeIfNeeded() {
        if (Integer.parseInt(SystemProperties.get(PROPERTY_RIL_FD_MODE, "0")) == 1 && isFdAllowed()) {
            if (shouldEnableFd()) {
                this.mPhone.mCi.setFdMode(FdModeType.ENABLE_MD_FD.ordinal(), -1, -1, obtainMessage(0));
            } else {
                this.mPhone.mCi.setFdMode(FdModeType.DISABLE_MD_FD.ordinal(), -1, -1, obtainMessage(0));
            }
        }
    }

    private void updateScreenStatusIfNeeded() {
        if (isFdAllowed()) {
            boolean z = this.mIsScreenOn;
            this.mPhone.mCi.setFdMode(FdModeType.INFO_MD_SCREEN_STATUS.ordinal(), z ? 1 : 0, -1, obtainMessage(0));
        }
    }

    private void onRadioOn() {
        logd("onRadioOn: update fd status when radio on");
        updateScreenStatusIfNeeded();
        updateFdModeIfNeeded();
    }

    private void onScreenSwitched(boolean z) {
        logd("onScreenSwitched: screenOn = " + z);
        this.mIsScreenOn = z;
        updateScreenStatusIfNeeded();
        if (isFdEnabledOnlyWhenScreenOff()) {
            updateFdModeIfNeeded();
        }
    }

    private void onChargingModeSwitched(boolean z) {
        boolean z2 = this.mIsCharging;
        this.mIsCharging = z;
        int i = this.mEnableFdOnCharing;
        this.mEnableFdOnCharing = Integer.parseInt(SystemProperties.get(PROPERTY_FD_ON_CHARGE, "0"));
        logd("onChargingModeSwitched: preCharging = " + z2 + " mIsCharging = " + this.mIsCharging + " preEnableFdonCharging = " + i + " mEnableFdOnCharing = " + this.mEnableFdOnCharing);
        if (z2 != this.mIsCharging || i != this.mEnableFdOnCharing) {
            updateFdModeIfNeeded();
        }
    }

    private void onTetheringSwitched() {
        updateFdModeIfNeeded();
    }

    private void onDefaultDataSwitched() {
        updateFdModeIfNeeded();
    }

    private static boolean isFdEnabledOnlyWhenScreenOff() {
        return SystemProperties.getInt(PROPERTY_FD_SCREEN_OFF_ONLY, 0) == 1;
    }

    private static int getPhoneId(Phone phone) {
        return phone.getPhoneId();
    }

    private void logd(String str) {
        Rlog.d(LOG_TAG, "[phoneId" + getPhoneId(this.mPhone) + "]" + str);
    }

    private void logw(String str) {
        Rlog.w(LOG_TAG, "[phoneId" + getPhoneId(this.mPhone) + "]" + str);
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, "[phoneId" + getPhoneId(this.mPhone) + "]" + str);
    }
}
