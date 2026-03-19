package com.mediatek.internal.telephony.uicc;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.MtkIccCardConstants;
import com.mediatek.internal.telephony.MtkSubscriptionController;
import com.mediatek.internal.telephony.MtkSubscriptionInfoUpdater;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import com.mediatek.telephony.internal.telephony.vsim.ExternalSimManager;

public class MtkUiccController extends UiccController {
    protected static final String COMMON_SLOT_PROPERTY = "ro.vendor.mtk_sim_hot_swap_common_slot";
    protected static final String DECRYPT_STATE = "trigger_restart_framework";
    protected static final int EVENT_BASE_ID = 100;
    protected static final int EVENT_COMMON_SLOT_NO_CHANGED = 111;
    protected static final int EVENT_GET_ICC_STATUS_DONE_FOR_SIM_MISSING = 105;
    protected static final int EVENT_GET_ICC_STATUS_DONE_FOR_SIM_RECOVERY = 106;
    protected static final int EVENT_INVALID_SIM_DETECTED = 109;
    protected static final int EVENT_REPOLL_SML_STATE = 110;
    protected static final int EVENT_SIM_MISSING = 103;
    protected static final int EVENT_SIM_PLUG_IN = 108;
    protected static final int EVENT_SIM_PLUG_OUT = 107;
    protected static final int EVENT_SIM_RECOVERY = 104;
    protected static final int EVENT_SML_SLOT_LOCK_INFO_CHANGED = 112;
    protected static final int EVENT_SUPPLY_DEVICE_LOCK_DONE = 113;
    protected static final int EVENT_VIRTUAL_SIM_OFF = 102;
    protected static final int EVENT_VIRTUAL_SIM_ON = 101;
    private static final String LOG_TAG_EX = "MtkUiccCtrl";
    private static final int SML_FEATURE_NEED_BROADCAST_INTENT = 1;
    private static final int SML_FEATURE_NO_NEED_BROADCAST_INTENT = 0;
    private int[] UICCCONTROLLER_STRING_NOTIFICATION_VIRTUAL_SIM_ON;
    private BroadcastReceiver mMdStateReceiver;

    public MtkUiccController(Context context, CommandsInterface[] commandsInterfaceArr) {
        super(context, commandsInterfaceArr);
        this.UICCCONTROLLER_STRING_NOTIFICATION_VIRTUAL_SIM_ON = new int[]{134545514, 134545515, 134545516, 134545517};
        Rlog.d(LOG_TAG_EX, "Creating MtkUiccController");
        for (int i = 0; i < this.mCis.length; i++) {
            Integer num = new Integer(i);
            this.mCis[i].unregisterForAvailable(this);
            this.mCis[i].unregisterForOn(this);
            if (SystemProperties.get("ro.crypto.state").equals("unencrypted") || SystemProperties.get("ro.crypto.state").equals("unsupported") || SystemProperties.get("ro.crypto.type").equals("file") || DECRYPT_STATE.equals(SystemProperties.get("vold.decrypt")) || !StorageManager.inCryptKeeperBounce()) {
                this.mCis[i].registerForAvailable(this, 6, num);
            } else {
                this.mCis[i].registerForOn(this, 5, num);
            }
            this.mCis[i].registerForVirtualSimOn(this, 101, num);
            this.mCis[i].registerForVirtualSimOff(this, 102, num);
            this.mCis[i].registerForSimMissing(this, EVENT_SIM_MISSING, num);
            this.mCis[i].registerForSimRecovery(this, 104, num);
            this.mCis[i].registerForSimPlugOut(this, EVENT_SIM_PLUG_OUT, num);
            this.mCis[i].registerForSimPlugIn(this, EVENT_SIM_PLUG_IN, num);
            this.mCis[i].registerForCommonSlotNoChanged(this, 111, num);
            this.mCis[i].registerForSmlSlotLockInfoChanged(this, 112, num);
        }
        if (SystemProperties.getInt("ro.vendor.mtk_external_sim_support", 0) == 1) {
            ExternalSimManager.make(context, commandsInterfaceArr);
        }
        this.mMdStateReceiver = new ModemStateChangedReceiver(this, null);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RadioManager.ACTION_MODEM_POWER_NO_CHANGE);
        this.mContext.registerReceiver(this.mMdStateReceiver, intentFilter);
    }

    public UiccCardApplication getUiccCardApplication(int i) {
        return getUiccCardApplication(SubscriptionController.getInstance().getPhoneId(SubscriptionController.getInstance().getDefaultSubId()), i);
    }

    public int getIccApplicationChannel(int i, int i2) {
        return 0;
    }

    public void handleMessage(Message message) {
        synchronized (mLock) {
            Integer ciIndex = getCiIndex(message);
            if (ciIndex.intValue() >= 0 && ciIndex.intValue() < this.mCis.length) {
                if (message.obj != null && (message.obj instanceof AsyncResult)) {
                }
                int i = message.what;
                if (i == 1) {
                    mtkLog("Received EVENT_ICC_STATUS_CHANGED, calling getIccCardStatus,index: " + ciIndex);
                    if (ignoreGetSimStatus()) {
                        mtkLog("FlightMode ON, Modem OFF: ignore get sim status");
                    } else {
                        this.mCis[ciIndex.intValue()].getIccCardStatus(obtainMessage(3, ciIndex));
                    }
                } else {
                    switch (i) {
                        case 5:
                        case 6:
                            if (ignoreGetSimStatus()) {
                                mtkLog("FlightMode ON, Modem OFF: ignore get sim status, index: " + ciIndex);
                            } else {
                                super.handleMessage(message);
                            }
                            break;
                        default:
                            switch (i) {
                                case 101:
                                    mtkLog("handleMessage (EVENT_VIRTUAL_SIM_ON)");
                                    setNotificationVirtual(ciIndex.intValue(), 101);
                                    SharedPreferences.Editor editorEdit = this.mContext.getSharedPreferences("AutoAnswer", 0).edit();
                                    editorEdit.putBoolean("flag", true);
                                    editorEdit.commit();
                                    break;
                                case 102:
                                    mtkLog("handleMessage (EVENT_VIRTUAL_SIM_OFF)");
                                    removeNotificationVirtual(ciIndex.intValue(), 101);
                                    SharedPreferences.Editor editorEdit2 = this.mContext.getSharedPreferences("AutoAnswer", 0).edit();
                                    editorEdit2.putBoolean("flag", false);
                                    editorEdit2.commit();
                                    break;
                                case EVENT_SIM_MISSING:
                                    mtkLog("handleMessage (EVENT_SIM_MISSING)");
                                    this.mCis[ciIndex.intValue()].getIccCardStatus(obtainMessage(105, ciIndex));
                                    break;
                                case 104:
                                    mtkLog("handleMessage (EVENT_SIM_RECOVERY)");
                                    this.mCis[ciIndex.intValue()].getIccCardStatus(obtainMessage(EVENT_GET_ICC_STATUS_DONE_FOR_SIM_RECOVERY, ciIndex));
                                    Intent intent = new Intent();
                                    intent.setAction("com.mediatek.phone.ACTION_SIM_RECOVERY_DONE");
                                    this.mContext.sendBroadcast(intent);
                                    break;
                                case 105:
                                    mtkLog("Received EVENT_GET_ICC_STATUS_DONE_FOR_SIM_MISSING");
                                    onGetIccCardStatusDone((AsyncResult) message.obj, ciIndex);
                                    break;
                                case EVENT_GET_ICC_STATUS_DONE_FOR_SIM_RECOVERY:
                                    mtkLog("Received EVENT_GET_ICC_STATUS_DONE_FOR_SIM_RECOVERY");
                                    onGetIccCardStatusDone((AsyncResult) message.obj, ciIndex);
                                    break;
                                default:
                                    switch (i) {
                                        case 110:
                                            mtkLog("Received EVENT_REPOLL_SML_STATE");
                                            AsyncResult asyncResult = (AsyncResult) message.obj;
                                            boolean z = message.arg1 == 1;
                                            onGetIccCardStatusDone(asyncResult, ciIndex);
                                            if (z) {
                                                UiccCardApplication uiccCardApplication = getUiccCardApplication(ciIndex.intValue(), 1);
                                                if (uiccCardApplication == null) {
                                                    mtkLog("UiccCardApplication = null");
                                                } else if (uiccCardApplication.getState() == IccCardApplicationStatus.AppState.APPSTATE_SUBSCRIPTION_PERSO) {
                                                    Intent intent2 = new Intent();
                                                    mtkLog("Broadcast ACTION_UNLOCK_SIM_LOCK");
                                                    intent2.setAction("com.mediatek.phone.ACTION_UNLOCK_SIM_LOCK");
                                                    intent2.putExtra("ss", "LOCKED");
                                                    intent2.putExtra(DataSubConstants.EXTRA_MOBILE_DATA_ENABLE_REASON, parsePersoType(uiccCardApplication.getPersoSubState()));
                                                    SubscriptionManager.putPhoneIdAndSubIdExtra(intent2, ciIndex.intValue());
                                                    this.mContext.sendBroadcast(intent2);
                                                }
                                            }
                                            break;
                                        case 111:
                                            mtkLog("handleMessage (EVENT_COMMON_SLOT_NO_CHANGED)");
                                            Intent intent3 = new Intent("com.mediatek.phone.ACTION_COMMON_SLOT_NO_CHANGED");
                                            int iIntValue = ciIndex.intValue();
                                            SubscriptionManager.putPhoneIdAndSubIdExtra(intent3, iIntValue);
                                            mtkLog("Broadcasting intent ACTION_COMMON_SLOT_NO_CHANGED for mSlotId : " + iIntValue);
                                            this.mContext.sendBroadcast(intent3);
                                            break;
                                        case 112:
                                            mtkLog("handleMessage (EVENT_SML_SLOT_LOCK_INFO_CHANGED)");
                                            onSmlSlotLoclInfoChaned((AsyncResult) message.obj, ciIndex);
                                            MtkSubscriptionInfoUpdater mtkSubscriptionInfoUpdater = (MtkSubscriptionInfoUpdater) PhoneFactory.getSubscriptionInfoUpdater();
                                            if (mtkSubscriptionInfoUpdater != null) {
                                                mtkSubscriptionInfoUpdater.updateNewSmlInfo(mtkSubscriptionInfoUpdater.getOldDetectedType(), MtkSubscriptionController.getMtkInstance().getActiveSubCountForSml(), MtkTelephonyManagerEx.getDefault().checkValidCard(0), MtkTelephonyManagerEx.getDefault().checkValidCard(1));
                                                mtkSubscriptionInfoUpdater.resetSimMountChangeState();
                                            } else {
                                                Rlog.e(LOG_TAG_EX, "MtkSubscriptionInfoUpdater.getInstance() is null");
                                            }
                                            break;
                                        case 113:
                                            mtkLog("handleMessage (EVENT_SUPPLY_DEVICE_LOCK_DONE)");
                                            int unlockDeviceResult = -1;
                                            AsyncResult asyncResult2 = (AsyncResult) message.obj;
                                            if (asyncResult2.exception != null && asyncResult2.result != null) {
                                                unlockDeviceResult = parseUnlockDeviceResult(asyncResult2);
                                            }
                                            Message message2 = (Message) asyncResult2.userObj;
                                            AsyncResult.forMessage(message2).exception = asyncResult2.exception;
                                            message2.arg1 = unlockDeviceResult;
                                            message2.sendToTarget();
                                            break;
                                        default:
                                            super.handleMessage(message);
                                            break;
                                    }
                                    break;
                            }
                            break;
                    }
                }
                return;
            }
            Rlog.e(LOG_TAG_EX, "Invalid index : " + ciIndex + " received with event " + message.what);
        }
    }

    private void onSmlSlotLoclInfoChaned(AsyncResult asyncResult, Integer num) {
        if (asyncResult.exception != null || asyncResult.result == null) {
            Rlog.e(LOG_TAG_EX, "onSmlSlotLoclInfoChaned exception");
            return;
        }
        int[] iArr = (int[]) asyncResult.result;
        if (iArr.length != 4) {
            Rlog.e(LOG_TAG_EX, "onSmlSlotLoclInfoChaned exception");
            return;
        }
        mtkLog("onSmlSlotLoclInfoChaned, infomation:,lock policy:" + iArr[0] + ",lock state:" + iArr[1] + ",service capability:" + iArr[2] + ",sim valid:" + iArr[3]);
        Intent intent = new Intent("com.mediatek.phone.ACTION_SIM_SLOT_LOCK_POLICY_INFORMATION");
        int iIntValue = num.intValue();
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, iIntValue);
        intent.putExtra("slot", iIntValue);
        intent.putExtra("DEVICE_LOCK_POLICY", iArr[0]);
        intent.putExtra("DEVICE_LOCK_STATE", iArr[1]);
        intent.putExtra("SIM_SERVICE_CAPABILITY", iArr[2]);
        intent.putExtra("SIM_VALID", iArr[3]);
        mtkLog("Broadcasting intent ACTION_SIM_SLOT_LOCK_POLICY_INFORMATION for mSlotId : " + iIntValue);
        this.mContext.sendBroadcast(intent);
    }

    public void supplyDeviceNetworkDepersonalization(String str, Message message) {
        this.mCis[0].supplyDeviceNetworkDepersonalization(str, obtainMessage(113, message));
    }

    private int parseUnlockDeviceResult(AsyncResult asyncResult) {
        int[] iArr = (int[]) asyncResult.result;
        int i = -1;
        if (iArr == null) {
            return -1;
        }
        if (iArr.length > 0) {
            i = iArr[0];
        }
        mtkLog("parseUnlockDeviceResult: attemptsRemaining=" + i);
        return i;
    }

    private void setNotificationVirtual(int i, int i2) {
        String string;
        mtkLog("setNotificationVirtual(): notifyType = " + i2);
        Notification notification = new Notification();
        notification.when = System.currentTimeMillis();
        notification.flags = 16;
        notification.icon = R.drawable.stat_sys_warning;
        notification.contentIntent = PendingIntent.getActivity(this.mContext, 0, new Intent(), 134217728);
        if (TelephonyManager.getDefault().getSimCount() > 1) {
            string = Resources.getSystem().getText(this.UICCCONTROLLER_STRING_NOTIFICATION_VIRTUAL_SIM_ON[i]).toString();
        } else {
            string = Resources.getSystem().getText(134545518).toString();
        }
        String string2 = this.mContext.getText(134545518).toString();
        notification.tickerText = this.mContext.getText(134545518).toString();
        notification.setLatestEventInfo(this.mContext, string, string2, notification.contentIntent);
        ((NotificationManager) this.mContext.getSystemService("notification")).notify(i2 + i, notification);
    }

    private void removeNotificationVirtual(int i, int i2) {
        ((NotificationManager) this.mContext.getSystemService("notification")).cancel(i2 + i);
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState = new int[IccCardApplicationStatus.PersoSubState.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK_SUBSET.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_CORPORATE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_SERVICE_PROVIDER.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_SIM.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    private String parsePersoType(IccCardApplicationStatus.PersoSubState persoSubState) {
        mtkLog("parsePersoType, state = " + persoSubState);
        switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$PersoSubState[persoSubState.ordinal()]) {
            case 1:
                return "NETWORK";
            case 2:
                return "NETWORK_SUBSET";
            case 3:
                return "CORPORATE";
            case 4:
                return "SERVICE_PROVIDER";
            case 5:
                return "SIM";
            default:
                return "UNKNOWN";
        }
    }

    public void repollIccStateForModemSmlChangeFeatrue(int i, boolean z) {
        mtkLog("repollIccStateForModemSmlChangeFeatrue, needIntent = " + z);
        this.mCis[i].getIccCardStatus(obtainMessage(110, !z ? 0 : 1, 0, Integer.valueOf(i)));
    }

    public boolean ignoreGetSimStatus() {
        int i = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
        mtkLog("ignoreGetSimStatus(): airplaneMode - " + i);
        if (!RadioManager.isFlightModePowerOffModemEnabled() || i != 1) {
            return false;
        }
        mtkLog("ignoreGetSimStatus(): return true");
        return true;
    }

    protected void mtkLog(String str) {
        Rlog.d(LOG_TAG_EX, str);
    }

    public boolean isAllRadioAvailable() {
        boolean z = true;
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            if (CommandsInterface.RadioState.RADIO_UNAVAILABLE == this.mCis[i].getRadioState()) {
                z = false;
            }
        }
        mtkLog("isAllRadioAvailable = " + z);
        return z;
    }

    public void resetRadioForVsim() {
        mtkLog("resetRadioForVsim...resetRadio");
        this.mCis[RadioCapabilitySwitchUtil.getMainCapabilityPhoneId()].resetRadio((Message) null);
    }

    public static MtkIccCardConstants.VsimType getVsimCardType(int i) {
        int i2 = SystemProperties.getInt("vendor.gsm.prefered.rsim.slot", -1);
        int i3 = SystemProperties.getInt("vendor.gsm.prefered.aka.sim.slot", -1);
        TelephonyManager.getDefault();
        String telephonyProperty = TelephonyManager.getTelephonyProperty(i, "vendor.gsm.external.sim.inserted", "0");
        boolean z = (telephonyProperty == null || telephonyProperty.length() <= 0 || "0".equals(telephonyProperty)) ? false : true;
        if (i == i2 && z) {
            return MtkIccCardConstants.VsimType.REMOTE_SIM;
        }
        if (i == i3) {
            if (z) {
                return MtkIccCardConstants.VsimType.SOFT_AKA_SIM;
            }
            return MtkIccCardConstants.VsimType.PHYSICAL_AKA_SIM;
        }
        if (i2 == -1 && i3 == -1 && z) {
            return MtkIccCardConstants.VsimType.LOCAL_SIM;
        }
        return MtkIccCardConstants.VsimType.PHYSICAL_SIM;
    }

    private class ModemStateChangedReceiver extends BroadcastReceiver {
        private ModemStateChangedReceiver() {
        }

        ModemStateChangedReceiver(MtkUiccController mtkUiccController, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(RadioManager.ACTION_MODEM_POWER_NO_CHANGE)) {
                for (int i = 0; i < MtkUiccController.this.mCis.length; i++) {
                    MtkUiccController.this.sendMessage(MtkUiccController.this.obtainMessage(1, new Integer(i)));
                    MtkUiccController.this.mtkLog("Trigger GET_SIM_STATUS due to modem state changed for slot " + i);
                }
            }
        }
    }
}
