package com.mediatek.internal.telephony.dataconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.util.SparseArray;
import com.android.internal.telephony.Phone;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.ArrayList;

public class FdManager extends Handler {
    private static final int BASE = 0;
    protected static final boolean DBG = true;
    private static final String DEFAULT_FD_SCREEN_OFF_R8_TIMER = "50";
    private static final String DEFAULT_FD_SCREEN_OFF_TIMER = "50";
    private static final String DEFAULT_FD_SCREEN_ON_R8_TIMER = "150";
    private static final String DEFAULT_FD_SCREEN_ON_TIMER = "150";
    private static final int EVENT_FD_MODE_SET = 0;
    private static final int EVENT_RADIO_AVAILABLE = 1;
    protected static final String LOG_TAG = "FdManager";
    protected static final String PROPERTY_FD_ON_CHARGE = "fd.on.charge";
    protected static final String PROPERTY_FD_SCREEN_OFF_ONLY = "fd.screen.off.only";
    protected static final String PROPERTY_MTK_FD_SUPPORT = "ro.mtk_fd_support";
    protected static final String PROPERTY_RIL_FD_MODE = "ril.fd.mode";
    private static final String STR_PROPERTY_FD_SCREEN_OFF_R8_TIMER = "persist.radio.fd.off.r8.counter";
    private static final String STR_PROPERTY_FD_SCREEN_OFF_TIMER = "persist.radio.fd.off.counter";
    private static final String STR_PROPERTY_FD_SCREEN_ON_R8_TIMER = "persist.radio.fd.r8.counter";
    private static final String STR_PROPERTY_FD_SCREEN_ON_TIMER = "persist.radio.fd.counter";
    private static final String STR_SCREEN_OFF = "SCREEN_OFF";
    private static final String STR_SCREEN_ON = "SCREEN_ON";
    private static final SparseArray<FdManager> sFdManagers = new SparseArray<>();
    private static String[] sTimerValue = {"50", "150", "50", "150"};
    private Phone mPhone;
    private boolean mChargingMode = false;
    private boolean mIsTetheredMode = false;
    private int mEnableFdOnCharing = 0;
    private boolean mIsScreenOn = true;
    protected BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            FdManager.this.logd("onReceive: action=" + action);
            int fdMdEnableMode = Integer.parseInt(SystemProperties.get(FdManager.PROPERTY_RIL_FD_MODE, "0"));
            if (action.equals("android.intent.action.SCREEN_ON")) {
                FdManager.this.onScreenSwitch(true, fdMdEnableMode);
                return;
            }
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                FdManager.this.onScreenSwitch(false, fdMdEnableMode);
                return;
            }
            if (!action.equals("android.intent.action.BATTERY_CHANGED")) {
                if (action.equals("android.net.conn.TETHER_STATE_CHANGED") && FdManager.isFdSupport()) {
                    FdManager.this.logd("Received ConnectivityManager.ACTION_TETHER_STATE_CHANGED");
                    ArrayList<String> active = intent.getStringArrayListExtra("activeArray");
                    FdManager.this.mIsTetheredMode = active != null && active.size() > 0;
                    FdManager.this.logd("[TETHER_STATE_CHANGED]mIsTetheredMode = " + FdManager.this.mIsTetheredMode + "mChargingMode=" + FdManager.this.mChargingMode);
                    if (FdManager.this.checkNeedTurnOn()) {
                        FdManager.this.updateFdMdEnableStatus(true);
                        return;
                    } else {
                        FdManager.this.updateFdMdEnableStatus(false);
                        return;
                    }
                }
                return;
            }
            if (FdManager.isFdSupport()) {
                int status = intent.getIntExtra("status", 0);
                int plugged = intent.getIntExtra("plugged", 0);
                boolean previousChargingMode = FdManager.this.mChargingMode;
                String sPluggedStr = UsimPBMemInfo.STRING_NOT_SET;
                if (status == 2) {
                    FdManager.this.mChargingMode = true;
                } else {
                    FdManager.this.mChargingMode = false;
                }
                if (plugged == 1) {
                    sPluggedStr = "Plugged in AC";
                } else if (plugged == 2) {
                    sPluggedStr = "Plugged in USB";
                }
                if (plugged == 1 || plugged == 2) {
                    FdManager.this.mChargingMode = true;
                }
                int previousEnableFDOnCharging = FdManager.this.mEnableFdOnCharing;
                FdManager.this.mEnableFdOnCharing = Integer.parseInt(SystemProperties.get(FdManager.PROPERTY_FD_ON_CHARGE, "0"));
                if (previousChargingMode != FdManager.this.mChargingMode || previousEnableFDOnCharging != FdManager.this.mEnableFdOnCharing) {
                    FdManager.this.logd("fdMdEnableMode=" + fdMdEnableMode + ", charging state changed");
                    FdManager.this.logd("previousEnableFdOnCharging=" + previousEnableFDOnCharging + ", mEnableFdOnCharing=" + FdManager.this.mEnableFdOnCharing + ", when charging state is changed");
                    FdManager.this.logd("previousChargingMode=" + previousChargingMode + ", mChargingMode=" + FdManager.this.mChargingMode + ", status=" + status + "(" + sPluggedStr + ")");
                }
                if (fdMdEnableMode == 1 && FdManager.this.checkAllowFd(FdManager.this.mPhone)) {
                    if (previousChargingMode == FdManager.this.mChargingMode && previousEnableFDOnCharging == FdManager.this.mEnableFdOnCharing) {
                        return;
                    }
                    if (FdManager.this.checkNeedTurnOn()) {
                        FdManager.this.updateFdMdEnableStatus(true);
                    } else {
                        FdManager.this.updateFdMdEnableStatus(false);
                    }
                }
            }
        }
    };

    public enum FdModeType {
        DISABLE_MD_FD,
        ENABLE_MD_FD,
        SET_FD_INACTIVITY_TIMER,
        INFO_MD_SCREEN_STATUS;

        public static FdModeType[] valuesCustom() {
            return values();
        }
    }

    public enum FdTimerType {
        ScreenOffLegacyFd,
        ScreenOnLegacyFd,
        ScreenOffR8Fd,
        ScreenOnR8Fd,
        SupportedTimerTypes;

        public static FdTimerType[] valuesCustom() {
            return values();
        }
    }

    private void onScreenSwitch(boolean isScreenOn, int fdMdEnableMode) {
        this.mIsScreenOn = isScreenOn;
        String strOnOff = this.mIsScreenOn ? STR_SCREEN_ON : STR_SCREEN_OFF;
        int screenMode = isScreenOn ? 1 : 0;
        if (!isFdSupport()) {
            return;
        }
        logd("fdMdEnableMode=" + fdMdEnableMode + ", when switching to " + strOnOff);
        if (fdMdEnableMode == 1) {
            if (!checkAllowFd(this.mPhone)) {
                return;
            }
            this.mPhone.mCi.setFDMode(FdModeType.INFO_MD_SCREEN_STATUS.ordinal(), screenMode, -1, obtainMessage(0));
            if (!isFdScreenOffOnly()) {
                return;
            }
            if (isScreenOn) {
                logd("Because FD_SCREEN_OFF_ONLY, disable fd when screen on.");
                updateFdMdEnableStatus(false);
                return;
            } else {
                if (isScreenOn || !checkNeedTurnOn()) {
                    return;
                }
                logd("Because FD_SCREEN_OFF_ONLY, enable fd when screen off.");
                updateFdMdEnableStatus(true);
                return;
            }
        }
        logd("Not Support AP-trigger FD now");
    }

    public static FdManager getInstance(Phone phone) {
        if (isFdSupport() && phone != null) {
            if (getPhoneId(phone) < 0) {
                Rlog.e(LOG_TAG, "phoneId[" + getPhoneId(phone) + "]is invalid!");
                return null;
            }
            FdManager fdMgr = sFdManagers.get(getPhoneId(phone));
            if (fdMgr == null) {
                Rlog.d(LOG_TAG, "FDMagager for phoneId:" + getPhoneId(phone) + " doesn't exist, create it");
                FdManager fdMgr2 = new FdManager(phone);
                sFdManagers.put(getPhoneId(phone), fdMgr2);
                return fdMgr2;
            }
            return fdMgr;
        }
        Rlog.e(LOG_TAG, "FDMagager can't get phone to init!");
        return null;
    }

    private FdManager(Phone p) {
        this.mPhone = p;
        logd("initial FastDormancyManager");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.net.conn.TETHER_STATE_CHANGED");
        this.mPhone.getContext().registerReceiver(this.mIntentReceiver, filter, null, this.mPhone);
        this.mPhone.mCi.registerForAvailable(this, 1, null);
        initFdTimer();
    }

    public void dispose() {
        logd("FD.dispose");
        if (!isFdSupport()) {
            return;
        }
        this.mPhone.getContext().unregisterReceiver(this.mIntentReceiver);
        this.mPhone.mCi.unregisterForAvailable(this);
        sFdManagers.remove(getPhoneId(this.mPhone));
    }

    private void initFdTimer() {
        sTimerValue[FdTimerType.ScreenOffLegacyFd.ordinal()] = Integer.toString((int) Double.parseDouble(timerStr[0]));
        sTimerValue[FdTimerType.ScreenOnLegacyFd.ordinal()] = Integer.toString((int) Double.parseDouble(timerStr[1]));
        sTimerValue[FdTimerType.ScreenOffR8Fd.ordinal()] = Integer.toString((int) Double.parseDouble(timerStr[2]));
        String[] timerStr = {SystemProperties.get(STR_PROPERTY_FD_SCREEN_OFF_TIMER, "50"), SystemProperties.get(STR_PROPERTY_FD_SCREEN_ON_TIMER, "150"), SystemProperties.get(STR_PROPERTY_FD_SCREEN_OFF_R8_TIMER, "50"), SystemProperties.get(STR_PROPERTY_FD_SCREEN_ON_R8_TIMER, "150")};
        sTimerValue[FdTimerType.ScreenOnR8Fd.ordinal()] = Integer.toString((int) Double.parseDouble(timerStr[3]));
        logd("Default FD timers=" + sTimerValue[0] + "," + sTimerValue[1] + "," + sTimerValue[2] + "," + sTimerValue[3]);
    }

    public int getNumberOfSupportedTypes() {
        return FdTimerType.SupportedTimerTypes.ordinal();
    }

    public int setFdTimerValue(String[] newTimerValue, Message onComplete) {
        int fdMdEnableMode = Integer.parseInt(SystemProperties.get(PROPERTY_RIL_FD_MODE, "0"));
        if (isFdSupport() && fdMdEnableMode == 1 && checkAllowFd(this.mPhone)) {
            for (int i = 0; i < newTimerValue.length; i++) {
                sTimerValue[i] = newTimerValue[i];
            }
            SystemProperties.set(STR_PROPERTY_FD_SCREEN_ON_TIMER, sTimerValue[FdTimerType.ScreenOnLegacyFd.ordinal()]);
            SystemProperties.set(STR_PROPERTY_FD_SCREEN_ON_R8_TIMER, sTimerValue[FdTimerType.ScreenOnR8Fd.ordinal()]);
            SystemProperties.set(STR_PROPERTY_FD_SCREEN_OFF_TIMER, sTimerValue[FdTimerType.ScreenOffLegacyFd.ordinal()]);
            SystemProperties.set(STR_PROPERTY_FD_SCREEN_OFF_R8_TIMER, sTimerValue[FdTimerType.ScreenOffR8Fd.ordinal()]);
            this.mPhone.mCi.setFDMode(FdModeType.SET_FD_INACTIVITY_TIMER.ordinal(), FdTimerType.ScreenOffLegacyFd.ordinal(), Integer.parseInt(sTimerValue[FdTimerType.ScreenOffLegacyFd.ordinal()]), null);
            this.mPhone.mCi.setFDMode(FdModeType.SET_FD_INACTIVITY_TIMER.ordinal(), FdTimerType.ScreenOnLegacyFd.ordinal(), Integer.parseInt(sTimerValue[FdTimerType.ScreenOnLegacyFd.ordinal()]), null);
            this.mPhone.mCi.setFDMode(FdModeType.SET_FD_INACTIVITY_TIMER.ordinal(), FdTimerType.ScreenOffR8Fd.ordinal(), Integer.parseInt(sTimerValue[FdTimerType.ScreenOffR8Fd.ordinal()]), null);
            this.mPhone.mCi.setFDMode(FdModeType.SET_FD_INACTIVITY_TIMER.ordinal(), FdTimerType.ScreenOnR8Fd.ordinal(), Integer.parseInt(sTimerValue[FdTimerType.ScreenOnR8Fd.ordinal()]), onComplete);
            logd("Set Default FD timers=" + sTimerValue[0] + "," + sTimerValue[1] + "," + sTimerValue[2] + "," + sTimerValue[3]);
        }
        return 0;
    }

    public int setFdTimerValue(String[] newTimerValue, Message onComplete, Phone phone) {
        FdManager fdMgr = getInstance(phone);
        if (fdMgr != null) {
            fdMgr.setFdTimerValue(newTimerValue, onComplete);
            return 0;
        }
        logd("setFDTimerValue fail!");
        return 0;
    }

    public static String[] getFdTimerValue() {
        return sTimerValue;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    logd("SET_FD_MODE ERROR");
                }
                break;
            case 1:
                logd("EVENT_RADIO_AVAILABLE check screen on/off again");
                int fdMdEnableMode = Integer.parseInt(SystemProperties.get(PROPERTY_RIL_FD_MODE, "0"));
                if (this.mIsScreenOn) {
                    onScreenSwitch(true, fdMdEnableMode);
                } else {
                    onScreenSwitch(false, fdMdEnableMode);
                }
                break;
            default:
                Rlog.e(LOG_TAG, "Unidentified event msg=" + msg);
                break;
        }
    }

    private void updateFdMdEnableStatus(boolean enabled) {
        int fdMdEnableMode = Integer.parseInt(SystemProperties.get(PROPERTY_RIL_FD_MODE, "0"));
        logd("updateFdMdEnableStatus():enabled=" + enabled + ",fdMdEnableMode=" + fdMdEnableMode);
        if (fdMdEnableMode != 1 || !checkAllowFd(this.mPhone)) {
            return;
        }
        if (enabled) {
            this.mPhone.mCi.setFDMode(FdModeType.ENABLE_MD_FD.ordinal(), -1, -1, obtainMessage(0));
        } else {
            this.mPhone.mCi.setFDMode(FdModeType.DISABLE_MD_FD.ordinal(), -1, -1, obtainMessage(0));
        }
    }

    public void disableFdWhenTethering() {
        if (!isFdSupport()) {
            return;
        }
        ConnectivityManager connMgr = (ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity");
        if (connMgr != null && connMgr.getTetheredIfaces() != null) {
            this.mIsTetheredMode = connMgr.getTetheredIfaces().length > 0;
        }
        logd("mIsTetheredMode = " + this.mIsTetheredMode + "mChargingMode=" + this.mChargingMode);
        if (checkNeedTurnOn()) {
            updateFdMdEnableStatus(true);
        } else {
            updateFdMdEnableStatus(false);
        }
    }

    private boolean checkNeedTurnOn() {
        return ((isFdScreenOffOnly() ? this.mIsScreenOn : false) || (this.mChargingMode && this.mEnableFdOnCharing == 0) || this.mIsTetheredMode) ? false : true;
    }

    public static boolean isFdScreenOffOnly() {
        return SystemProperties.getInt(PROPERTY_FD_SCREEN_OFF_ONLY, 0) == 1;
    }

    public static boolean isFdSupport() {
        return SystemProperties.getInt(PROPERTY_MTK_FD_SUPPORT, 1) == 1;
    }

    private static int getPhoneId(Phone phone) {
        return phone.getPhoneId();
    }

    private boolean checkAllowFd(Phone phone) {
        if ((phone.getRadioAccessFamily() & 16384) == 16384 || (phone.getRadioAccessFamily() & 8) == 8) {
            return true;
        }
        return false;
    }

    protected void logd(String s) {
        Rlog.d(LOG_TAG, "[GDCT][phoneId" + getPhoneId(this.mPhone) + "]" + s);
    }
}
