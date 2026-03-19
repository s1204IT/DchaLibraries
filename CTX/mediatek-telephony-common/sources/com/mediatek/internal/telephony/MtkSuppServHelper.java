package com.mediatek.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsReasonInfo;
import com.android.ims.ImsConnectionStateListener;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.imsphone.MtkImsPhone;
import com.mediatek.internal.telephony.uicc.MtkSIMRecords;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MtkSuppServHelper extends Handler {
    private static final String ACTION_SYSTEM_UPDATE_SUCCESSFUL = "com.mediatek.systemupdate.UPDATE_SUCCESSFUL";
    private static final boolean CFU_QUERY_WHEN_IMS_REGISTERED_DEFAULT = false;
    private static final String CFU_SETTING_ALWAYS_NOT_QUERY = "1";
    private static final String CFU_SETTING_ALWAYS_QUERY = "2";
    private static final String CFU_SETTING_DEFAULT = "0";
    private static final String CFU_SETTING_QUERY_IF_EFCFIS_INVALID = "3";
    private static final boolean DBG = true;
    private static final int EFCFIS_STATUS_INVALID = 3;
    private static final int EFCFIS_STATUS_NOT_READY = 0;
    private static final int EFCFIS_STATUS_VALID = 2;
    private static final int EVENT_CALL_FORWARDING_STATUS_FROM_MD = 6;
    private static final int EVENT_CARRIER_CONFIG_LOADED = 15;
    private static final int EVENT_CFU_STATUS_FROM_MD = 8;
    public static final int EVENT_CLEAN_CFU_STATUS = 16;
    private static final int EVENT_DATA_CONNECTION_ATTACHED = 2;
    private static final int EVENT_DATA_CONNECTION_DETACHED = 3;
    private static final int EVENT_GET_CALL_FORWARD_BY_GSM_DONE = 4;
    private static final int EVENT_GET_CALL_FORWARD_BY_IMS_DONE = 5;
    private static final int EVENT_GET_CALL_FORWARD_TIME_SLOT_BY_GSM_DONE = 10;
    private static final int EVENT_GET_CALL_FORWARD_TIME_SLOT_BY_IMS_DONE = 11;
    private static final int EVENT_ICCRECORDS_READY = 1;
    private static final int EVENT_ICC_CHANGED = 13;
    private static final int EVENT_QUERY_CFU_OVER_CS = 7;
    private static final int EVENT_QUERY_CFU_OVER_CS_AFTER_DATA_NOT_ATTACHED = 14;
    private static final int EVENT_REGISTERED_TO_NETWORK = 0;
    private static final int EVENT_SIM_RECORDS_LOADED = 12;
    private static final int EVENT_SS_RESET = 9;
    private static final String IMS_NOT_QUERY_YET = "1";
    private static final String IMS_NO_NEED_QUERY = "0";
    private static final String IMS_QUERY_DONE = "2";
    private static final String LOG_TAG = "SuppServHelper";
    private static final int QUERY_OVER_GSM = 0;
    private static final int QUERY_OVER_GSM_OVER_UT = 1;
    private static final int QUERY_OVER_IMS = 2;
    private static final boolean SDBG;
    private static final String SIM_CHANGED = "1";
    private static final String SIM_NO_CHANGED = "0";
    private static final int TASK_CLEAN_CFU_STATUS = 4;
    private static final int TASK_QUERY_CFU = 0;
    private static final int TASK_QUERY_CFU_OVER_GSM = 1;
    private static final int TASK_QUERY_CFU_OVER_IMS = 2;
    private static final int TASK_TIME_SLOT_FAILED = 3;
    private static final int TIMER_FOR_RETRY_QUERY_CFU = 20000;
    private static final int TIMER_FOR_SKIP_WAITING_CFU_STATUS_FROM_MD = 20000;
    private static final int TIMER_FOR_WAIT_DATA_ATTACHED = 20000;
    private static final boolean VDBG = SystemProperties.get("ro.build.type").equals("eng");
    private static final boolean WITHOUT_TIME_SLOT = false;
    private static final boolean WITH_TIME_SLOT = true;
    private Context mContext;
    private ImsManager mImsManager;
    private MtkGsmCdmaPhone mPhone;
    private UiccController mUiccController = null;
    private final AtomicReference<IccRecords> mIccRecords = new AtomicReference<>();
    private AtomicBoolean mAttached = new AtomicBoolean(false);
    private MtkSuppServHelper mMtkSuppServHelper = null;
    private SuppServTaskDriven mSuppServTaskDriven = null;
    private boolean mNeedGetCFU = true;
    private boolean mNeedGetCFUOverIms = false;
    private boolean mSimRecordsLoaded = false;
    private boolean mCarrierConfigLoaded = false;
    private int mCFUStatusFromMD = -1;
    private boolean mSkipCFUStatusFromMD = false;
    private int mNeeedSyncForOTA = -1;
    private boolean mAlwaysQueryDone = false;
    private boolean mNotifyCFUIfEFCFISisInvalidDone = false;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED")) {
                MtkSuppServHelper.this.handleSubinfoUpdate();
                return;
            }
            if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE")) {
                MtkSuppServHelper.this.setNeedGetCFU(true);
                MtkSuppServHelper.this.mSuppServTaskDriven.appendTask(MtkSuppServHelper.this.new Task(0, false, "Radio capability done"));
                return;
            }
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                boolean booleanExtra = intent.getBooleanExtra("state", false);
                MtkSuppServHelper.this.logd("ACTION_AIRPLANE_MODE_CHANGED, bAirplaneModeOn = " + booleanExtra);
                if (booleanExtra) {
                    MtkSuppServHelper.this.setNeedGetCFU(true);
                    if (MtkSuppServHelper.this.isResetCSFBStatusAfterFlightMode()) {
                        MtkSuppServHelper.this.mPhone.setCsFallbackStatus(0);
                        return;
                    }
                    return;
                }
                return;
            }
            if (action.equals("android.intent.action.ACTION_SUPPLEMENTARY_SERVICE_UT_TEST")) {
                if (!MtkSuppServHelper.this.isSupportSuppServUTTest()) {
                    return;
                }
                MtkSuppServHelper.this.makeMtkSuppServUtTest(intent).run();
                return;
            }
            if (!action.equals("android.telephony.action.SIM_APPLICATION_STATE_CHANGED")) {
                if (action.equals(MtkSuppServHelper.ACTION_SYSTEM_UPDATE_SUCCESSFUL)) {
                    MtkSuppServHelper.this.logd("ACTION_SYSTEM_UPDATE_SUCCESSFUL, sync CFU info.");
                    MtkSuppServHelper.this.setNeedSyncSysPropToSIMforOTA(true);
                    if (MtkSuppServHelper.this.syncSysPropToSIMforOTA()) {
                        MtkSuppServHelper.this.setNeedSyncSysPropToSIMforOTA(false);
                        MtkSuppServHelper.this.mNeeedSyncForOTA = 0;
                        return;
                    }
                    return;
                }
                return;
            }
            int intExtra = intent.getIntExtra("android.telephony.extra.SIM_STATE", 0);
            int intExtra2 = intent.getIntExtra("subscription", -1);
            MtkSuppServHelper.this.logd("ACTION_SIM_APPLICATION_STATE_CHANGED: " + intExtra + ", subId: " + intExtra2 + ", CallForwardingFromSimRecords: " + MtkSuppServHelper.this.getCallForwardingFromSimRecords());
            if (10 == intExtra && intExtra2 == MtkSuppServHelper.this.mPhone.getSubId() && MtkSuppServHelper.this.getCallForwardingFromSimRecords() == 1) {
                MtkSuppServHelper.this.logd("ACTION_SIM_APPLICATION_STATE_CHANGED, refresh CFU info.");
                MtkSuppServHelper.this.mPhone.notifyCallForwardingIndicator();
            }
        }
    };
    private ImsConnectionStateListener mImsConnectionStateListener = new ImsConnectionStateListener() {
        public void onImsConnected(int i) {
            MtkSuppServHelper.this.logd("onImsConnected imsRadioTech=" + i);
            if (!MtkSuppServHelper.this.isMDSupportIMSSuppServ()) {
                if (MtkSuppServHelper.this.mPhone.isOpTbcwWithCS()) {
                    MtkSuppServHelper.this.mPhone.setTbcwMode(3);
                    MtkSuppServHelper.this.mPhone.setTbcwToEnabledOnIfDisabled();
                } else {
                    MtkSuppServHelper.this.mPhone.setTbcwMode(1);
                    MtkSuppServHelper.this.mPhone.setTbcwToEnabledOnIfDisabled();
                }
            }
            if (MtkSuppServHelper.this.getCfuSetting().equals("1")) {
                MtkSuppServHelper.this.logd("onImsConnected, no need to query CFU over IMS due to ALWAYS_NOT_QUERY");
                return;
            }
            if (!MtkSuppServHelper.this.isNotMachineTest()) {
                MtkSuppServHelper.this.logd("onImsConnected, no need to query CFU over IMS due to machine test");
                return;
            }
            MtkSuppServHelper.this.setNeedGetCFU(true);
            if (MtkSuppServHelper.this.getIMSQueryStatus()) {
                MtkSuppServHelper.this.mSuppServTaskDriven.appendTask(MtkSuppServHelper.this.new Task(2, "IMS state in service"));
            }
        }

        public void onImsDisconnected(ImsReasonInfo imsReasonInfo) {
        }

        public void onImsProgressing(int i) {
        }

        public void onImsResumed() {
        }

        public void onImsSuspended() {
        }
    };

    static {
        SDBG = !SystemProperties.get("ro.build.type").equals(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER);
    }

    private class Task {
        private boolean mExtraBool;
        private int mExtraInt;
        private String mExtraMsg;
        private int mTaskId;

        public Task(int i, boolean z, String str) {
            this.mTaskId = -1;
            this.mExtraBool = false;
            this.mExtraInt = -1;
            this.mExtraMsg = "";
            this.mTaskId = i;
            this.mExtraBool = z;
            this.mExtraMsg = str;
        }

        public Task(int i, String str) {
            this.mTaskId = -1;
            this.mExtraBool = false;
            this.mExtraInt = -1;
            this.mExtraMsg = "";
            this.mTaskId = i;
            this.mExtraMsg = str;
        }

        public int getTaskId() {
            return this.mTaskId;
        }

        public int getExtraInt() {
            return this.mExtraInt;
        }

        public boolean getExtraBoolean() {
            return this.mExtraBool;
        }

        public String getExtraMsg() {
            return this.mExtraMsg;
        }

        public String toString() {
            return "Task ID: " + this.mTaskId + ", ExtraBool: " + this.mExtraBool + ", ExtraInt: " + this.mExtraInt + ", ExtraMsg: " + this.mExtraMsg;
        }
    }

    private class SuppServTaskDriven extends Handler {
        private static final int EVENT_DONE = 0;
        private static final int EVENT_EXEC_NEXT = 1;
        private static final int STATE_DOING = 1;
        private static final int STATE_DONE = 2;
        private static final int STATE_NO_PENDING = 0;
        private ArrayList<Task> mPendingTask;
        private int mState;
        private Object mStateLock;
        private Object mTaskLock;

        public SuppServTaskDriven() {
            this.mPendingTask = new ArrayList<>();
            this.mTaskLock = new Object();
            this.mStateLock = new Object();
            this.mState = 0;
        }

        public SuppServTaskDriven(Looper looper) {
            super(looper);
            this.mPendingTask = new ArrayList<>();
            this.mTaskLock = new Object();
            this.mStateLock = new Object();
            this.mState = 0;
        }

        public void appendTask(Task task) {
            synchronized (this.mTaskLock) {
                this.mPendingTask.add(task);
            }
            obtainMessage(1).sendToTarget();
        }

        private int getState() {
            int i;
            synchronized (this.mStateLock) {
                i = this.mState;
            }
            return i;
        }

        private void setState(int i) {
            synchronized (this.mStateLock) {
                this.mState = i;
            }
        }

        private Task getCurrentPendingTask() {
            synchronized (this.mTaskLock) {
                if (this.mPendingTask.size() == 0) {
                    return null;
                }
                return this.mPendingTask.get(0);
            }
        }

        private void removePendingTask(int i) {
            synchronized (this.mTaskLock) {
                if (this.mPendingTask.size() > 0) {
                    this.mPendingTask.remove(i);
                    MtkSuppServHelper.this.logd("removePendingTask remain mPendingTask: " + this.mPendingTask.size());
                }
            }
        }

        public void clearPendingTask() {
            synchronized (this.mTaskLock) {
                this.mPendingTask.clear();
            }
        }

        public void exec() {
            Task currentPendingTask = getCurrentPendingTask();
            if (currentPendingTask == null) {
                setState(0);
            }
            if (getState() == 1) {
                return;
            }
            setState(1);
            int taskId = currentPendingTask.getTaskId();
            MtkSuppServHelper.this.logd(currentPendingTask.toString());
            switch (taskId) {
                case 0:
                    MtkSuppServHelper.this.startHandleCFUQueryProcess(currentPendingTask.getExtraBoolean(), currentPendingTask.getExtraMsg());
                    break;
                case 1:
                    MtkSuppServHelper.this.queryCallForwardStatusOverGSM();
                    break;
                case 2:
                    MtkSuppServHelper.this.queryCallForwardStatusOverIMS();
                    break;
                case 3:
                    MtkSuppServHelper.this.startCFUQuery(true);
                    break;
                case 4:
                    MtkSuppServHelper.this.cleanCFUStatus();
                    break;
                default:
                    MtkSuppServHelper.this.taskDone();
                    break;
            }
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    removePendingTask(0);
                    setState(2);
                    break;
                case 1:
                    break;
                default:
                    return;
            }
            exec();
        }

        private String stateToString(int i) {
            switch (i) {
                case 0:
                    return "STATE_NO_PENDING";
                case 1:
                    return "STATE_DOING";
                case 2:
                    return "STATE_DONE";
                default:
                    return "UNKNOWN_STATE";
            }
        }

        private String eventToString(int i) {
            switch (i) {
                case 0:
                    return "EVENT_DONE";
                case 1:
                    return "EVENT_EXEC_NEXT";
                default:
                    return "UNKNOWN_EVENT";
            }
        }
    }

    public MtkSuppServHelper(Context context, Phone phone) {
        this.mPhone = null;
        this.mImsManager = null;
        this.mContext = context;
        this.mPhone = (MtkGsmCdmaPhone) phone;
        this.mImsManager = ImsManager.getInstance(context, this.mPhone.getPhoneId());
        registerEvent();
        registerBroadcastReceiver();
        logd("MtkSuppServHelper init done.");
    }

    public void init(Looper looper) {
        this.mSuppServTaskDriven = new SuppServTaskDriven(looper);
    }

    private boolean checkInitCriteria(StringBuilder sb) {
        String cfuSetting = getCfuSetting();
        if (!getNeedGetCFU()) {
            sb.append("No need to get CFU. (flag is false), ");
            return false;
        }
        if (!isSubInfoReady()) {
            sb.append("SubInfo not ready, ");
            return false;
        }
        if (!isIccCardMncMccAvailable(this.mPhone.getPhoneId())) {
            sb.append("MCC MNC not ready, ");
            return false;
        }
        if (!isIccRecordsAvailable()) {
            sb.append("Icc record available, ");
            return false;
        }
        if (!isVoiceInService()) {
            sb.append("Network is not registered, ");
            return false;
        }
        if (!getSimRecordsLoaded()) {
            sb.append("Sim not loaded, ");
            return false;
        }
        if (cfuSetting.equals(CFU_SETTING_QUERY_IF_EFCFIS_INVALID)) {
            int iCheckEfCfis = checkEfCfis();
            logd("efcfisStatus: " + iCheckEfCfis);
            if (iCheckEfCfis == 2 || iCheckEfCfis == 0) {
                sb.append("EfCfis in SIM is valid, no need to check or SIMRecords not ready.");
                return false;
            }
        }
        sb.append("All Criteria ready.");
        return true;
    }

    private void onUpdateIcc() {
        if (this.mUiccController == null) {
            return;
        }
        IccRecords uiccRecords = getUiccRecords(1);
        if (uiccRecords == null && this.mPhone.getPhoneType() == 2) {
            uiccRecords = getUiccRecords(2);
        }
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords != uiccRecords) {
            setSimRecordsLoaded(false);
            this.mNotifyCFUIfEFCFISisInvalidDone = false;
            if (iccRecords != null) {
                logi("Removing stale icc objects.");
                iccRecords.unregisterForRecordsLoaded(this);
                this.mIccRecords.set(null);
            }
            if (uiccRecords != null) {
                if (SubscriptionManager.isValidSubscriptionId(this.mPhone.getSubId())) {
                    logi("New records found.");
                    this.mIccRecords.set(uiccRecords);
                    uiccRecords.registerForRecordsLoaded(this, 12, (Object) null);
                    return;
                }
                return;
            }
            logd("onUpdateIcc: Sim not ready.");
        }
    }

    private IccRecords getUiccRecords(int i) {
        return this.mUiccController.getIccRecords(this.mPhone.getPhoneId(), i);
    }

    private boolean getSimRecordsLoaded() {
        logi("mSimRecordsLoaded: " + this.mSimRecordsLoaded);
        return this.mSimRecordsLoaded;
    }

    private void setSimRecordsLoaded(boolean z) {
        logi("Set mSimRecordsLoaded: " + z);
        this.mSimRecordsLoaded = z;
    }

    private boolean getCarrierConfigLoaded() {
        logi("mCarrierConfigLoaded: " + this.mCarrierConfigLoaded);
        return this.mCarrierConfigLoaded;
    }

    private void setCarrierConfigLoaded(boolean z) {
        logi("Set mCarrierConfigLoaded: " + z);
        this.mCarrierConfigLoaded = z;
    }

    private void handleSubinfoUpdate() {
        if (!isSubInfoReady()) {
            return;
        }
        handleSuppServInit();
        if (!isIccRecordsAvailable()) {
        }
    }

    private String getCfuSetting() {
        String cFUQueryDefault = getCFUQueryDefault();
        if (!TelephonyManager.from(this.mContext).isVoiceCapable()) {
            return SystemProperties.get("persist.vendor.radio.cfu.querytype", "1");
        }
        return SystemProperties.get("persist.vendor.radio.cfu.querytype", cFUQueryDefault);
    }

    private String getCFUQueryDefault() {
        return "0";
    }

    private boolean isNotMachineTest() {
        boolean z;
        String str = "0";
        if (this.mPhone.getPhoneId() == 0) {
            str = SystemProperties.get("vendor.gsm.sim.ril.testsim", "0");
        } else if (this.mPhone.getPhoneId() == 1) {
            str = SystemProperties.get("vendor.gsm.sim.ril.testsim.2", "0");
        }
        String operatorNumeric = this.mPhone.getServiceState().getOperatorNumeric();
        if (operatorNumeric == null || !operatorNumeric.equals("46602")) {
            z = false;
        } else {
            z = true;
        }
        logd("isTestSIM : " + str + " isRRMEnv : " + z);
        return str.equals("0") && !z;
    }

    private void startHandleCFUQueryProcess(boolean z, String str) {
        StringBuilder sb = new StringBuilder();
        boolean zCheckInitCriteria = checkInitCriteria(sb);
        String cfuSetting = getCfuSetting();
        logd("startHandleCFUQueryProcess(), forceQuery: " + z + ", CFU_KEY = " + cfuSetting + ", reason: " + str + ", checkCriteria: " + zCheckInitCriteria + ", criteriaFailReason: " + sb.toString());
        if (!zCheckInitCriteria) {
            taskDone();
            return;
        }
        if (isNotMachineTest()) {
            if (cfuSetting.equals(CFU_SETTING_QUERY_IF_EFCFIS_INVALID) && getSIMChangedRecordFromSystemProp()) {
                SystemProperties.set("persist.vendor.radio.cfu.change." + this.mPhone.getPhoneId(), "0");
                startCFUQuery();
            } else if (cfuSetting.equals(MtkGsmCdmaPhone.ACT_TYPE_UTRAN) && !this.mAlwaysQueryDone) {
                logd("Always query done: " + this.mAlwaysQueryDone);
                startCFUQuery();
                this.mAlwaysQueryDone = true;
            } else {
                taskDone();
            }
        } else {
            taskDone();
        }
        setNeedGetCFU(false);
    }

    public void setAlwaysQueryDoneFlag(boolean z) {
        logd("setAlwaysQueryDoneFlag: flag = " + z);
        this.mAlwaysQueryDone = z;
    }

    private void setNeedGetCFU(boolean z) {
        logd("setNeedGetCFU: " + z);
        this.mNeedGetCFU = z;
    }

    private boolean getNeedGetCFU() {
        return this.mNeedGetCFU;
    }

    private void taskDone() {
        this.mSuppServTaskDriven.obtainMessage(0).sendToTarget();
    }

    private boolean isIccCardMncMccAvailable(int i) {
        IccRecords iccRecords = UiccController.getInstance().getIccRecords(i, 1);
        if (iccRecords != null && iccRecords.getOperatorNumeric() != null) {
            return true;
        }
        return false;
    }

    private boolean isReceiveCFUStatusFromMD() {
        return (this.mSkipCFUStatusFromMD || this.mCFUStatusFromMD == -1) ? true : true;
    }

    private boolean isIccRecordsAvailable() {
        if (this.mPhone.getIccRecords() != null) {
            return true;
        }
        return false;
    }

    private boolean isVoiceInService() {
        if (this.mPhone.mSST != null && this.mPhone.mSST.mSS != null && this.mPhone.mSST.mSS.getState() == 0) {
            return true;
        }
        return false;
    }

    private boolean isSubInfoReady() {
        SubscriptionInfo activeSubscriptionInfo;
        SubscriptionManager subscriptionManagerFrom = SubscriptionManager.from(this.mContext);
        if (subscriptionManagerFrom != null) {
            activeSubscriptionInfo = subscriptionManagerFrom.getActiveSubscriptionInfo(this.mPhone.getSubId());
        } else {
            activeSubscriptionInfo = null;
        }
        if (activeSubscriptionInfo != null && activeSubscriptionInfo.getIccId() != null) {
            return true;
        }
        return false;
    }

    private void handleSuppServInit() {
        SubscriptionInfo activeSubscriptionInfo;
        String str = "persist.vendor.radio.cfu.iccid." + this.mPhone.getPhoneId();
        String str2 = SystemProperties.get(str, "");
        String cfuSetting = getCfuSetting();
        SubscriptionManager subscriptionManagerFrom = SubscriptionManager.from(this.mContext);
        if (subscriptionManagerFrom != null) {
            activeSubscriptionInfo = subscriptionManagerFrom.getActiveSubscriptionInfo(this.mPhone.getSubId());
        } else {
            activeSubscriptionInfo = null;
        }
        if (activeSubscriptionInfo == null) {
            this.mNeeedSyncForOTA = -1;
        }
        boolean z = false;
        if (activeSubscriptionInfo != null && !activeSubscriptionInfo.getIccId().equals(str2)) {
            logw("mySubId " + this.mPhone.getSubId() + " mySettingName " + Rlog.pii(SDBG, str) + " old iccid : " + Rlog.pii(SDBG, str2) + " new iccid : " + Rlog.pii(SDBG, activeSubscriptionInfo.getIccId()));
            SystemProperties.set(str, activeSubscriptionInfo.getIccId());
            StringBuilder sb = new StringBuilder();
            sb.append("persist.vendor.radio.cfu.change.");
            sb.append(this.mPhone.getPhoneId());
            SystemProperties.set(sb.toString(), "1");
            if (isNeedSyncSysPropToSIMforOTA()) {
                setNeedSyncSysPropToSIMforOTA(false);
                this.mNeeedSyncForOTA = 0;
            }
            handleSuppServIfSimChanged();
            return;
        }
        if (activeSubscriptionInfo != null && activeSubscriptionInfo.getIccId().equals(str2)) {
            this.mNeeedSyncForOTA = 1;
            if (isNeedSyncSysPropToSIMforOTA()) {
                logd("ICC are the sames and trigger CFU status sync for OTA.");
                if (syncSysPropToSIMforOTA()) {
                    setNeedSyncSysPropToSIMforOTA(false);
                    this.mNeeedSyncForOTA = 0;
                    return;
                }
                return;
            }
            int iCheckEfCfis = checkEfCfis();
            logd("Notify CFU ICON, efcfis status: " + iCheckEfCfis);
            if (getSimRecordsLoaded() && iCheckEfCfis == 3 && !this.mNotifyCFUIfEFCFISisInvalidDone) {
                if (getCFUStatusFromLocal() == 1) {
                    z = true;
                }
                logd("Notify CFU icon: " + z);
                this.mPhone.setVoiceCallForwardingFlag(1, z, "");
                this.mNotifyCFUIfEFCFISisInvalidDone = true;
                return;
            }
            return;
        }
        if (cfuSetting.equals(MtkGsmCdmaPhone.ACT_TYPE_UTRAN)) {
            this.mSuppServTaskDriven.appendTask(new Task(0, false, "Always query CFU"));
        }
    }

    private void handleSuppServIfSimChanged() {
        if (getSIMChangedRecordFromSystemProp()) {
            reset();
            this.mPhone.setCsFallbackStatus(0);
            if (!isMDSupportIMSSuppServ()) {
                this.mPhone.setTbcwMode(0);
                this.mPhone.setSSPropertyThroughHidl(this.mPhone.getPhoneId(), "persist.vendor.radio.terminal-based.cw", "disabled_tbcw");
            }
            this.mAlwaysQueryDone = false;
            this.mPhone.saveTimeSlot(null);
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);
            if (defaultSharedPreferences.getInt("clir_key" + this.mPhone.getPhoneId(), -1) != -1) {
                SharedPreferences.Editor editorEdit = defaultSharedPreferences.edit();
                editorEdit.remove("clir_key" + this.mPhone.getPhoneId());
                if (!editorEdit.commit()) {
                    loge("failed to commit the removal of CLIR preference");
                }
            }
            TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "persist.vendor.radio.cfu.mode", "disabled_cfu_mode");
            if (needQueryCFUOverIms()) {
                TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "persist.vendor.radio.cfu_over_ims", "1");
            } else {
                TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "persist.vendor.radio.cfu_over_ims", "0");
            }
            this.mSuppServTaskDriven.appendTask(new Task(0, false, "Sim Changed"));
        }
    }

    private boolean getIMSQueryStatus() {
        String systemProperty = "0";
        if (needQueryCFUOverIms()) {
            systemProperty = this.mPhone.getSystemProperty("persist.vendor.radio.cfu_over_ims", "0");
        }
        return (MtkGsmCdmaPhone.ACT_TYPE_UTRAN.equals(systemProperty) || "0".equals(systemProperty) || !"1".equals(systemProperty)) ? false : true;
    }

    private boolean getSIMChangedRecordFromSystemProp() {
        String str = SystemProperties.get("persist.vendor.radio.cfu.change." + this.mPhone.getPhoneId(), "0");
        logd("getSIMChangedRecordFromSystemProp: " + str);
        if (str.equals("1")) {
            return true;
        }
        return false;
    }

    private int getCallForwardingFromSimRecords() {
        IccRecords iccRecords = this.mPhone.getIccRecords();
        if (iccRecords != null) {
            return iccRecords.getVoiceCallForwardingFlag();
        }
        return -1;
    }

    private void startCFUQuery() {
        startCFUQuery(false);
    }

    private void startCFUQuery(boolean z) {
        if (isIMSRegistered()) {
            this.mPhone.getImsPhone();
            if (isSupportCFUTimeSlot() && !z) {
                getCallForwardingOption(2, true);
                return;
            } else {
                getCallForwardingOption(2, false);
                return;
            }
        }
        boolean zIsDataEnabled = this.mPhone.isDataEnabled();
        if (this.mPhone.getCsFallbackStatus() == 0 && this.mPhone.isGsmUtSupport() && zIsDataEnabled) {
            logd("startCFUQuery, get data attached state : " + this.mAttached.get());
            if (!this.mAttached.get()) {
                sendMessageDelayed(obtainMessage(14), 20000L);
                taskDone();
                return;
            } else if (isSupportCFUTimeSlot() && !z) {
                getCallForwardingOption(1, true);
                return;
            } else {
                getCallForwardingOption(1, false);
                return;
            }
        }
        if (this.mPhone.isDuringVoLteCall() || this.mPhone.isDuringImsEccCall()) {
            logi("No need query CFU in CS domain due to during volte call and ims ecc call!");
            taskDone();
        } else if (isIMSRegistered() && isNoNeedToCSFBWhenIMSRegistered()) {
            taskDone();
        } else if (isNotSupportUtToCS()) {
            taskDone();
        } else {
            getCallForwardingOption(0, false);
        }
    }

    private boolean isIMSRegistered() {
        Phone imsPhone = this.mPhone.getImsPhone();
        if (this.mPhone.getCsFallbackStatus() == 0 && imsPhone != null && imsPhone.getServiceState().getState() == 0) {
            return true;
        }
        return false;
    }

    private void registerEvent() {
        this.mPhone.getServiceStateTracker().registerForDataConnectionAttached(this, 2, (Object) null);
        this.mPhone.getServiceStateTracker().registerForDataConnectionDetached(this, 3, (Object) null);
        this.mPhone.getServiceStateTracker().registerForNetworkAttached(this, 0, (Object) null);
        this.mPhone.registerForSimRecordsLoaded(this, 12, null);
        if (this.mImsManager != null) {
            try {
                this.mImsManager.addRegistrationListener(1, this.mImsConnectionStateListener);
            } catch (ImsException e) {
                logd("ImsManager addRegistrationListener failed, " + e.toString());
            }
        }
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 13, (Object) null);
        this.mPhone.mCi.registerForCallForwardingInfo(this, 8, null);
    }

    private void unRegisterEvent() {
        this.mPhone.getServiceStateTracker().unregisterForDataConnectionAttached(this);
        this.mPhone.getServiceStateTracker().unregisterForDataConnectionDetached(this);
        this.mPhone.getServiceStateTracker().unregisterForNetworkAttached(this);
        this.mPhone.unregisterForSimRecordsLoaded(this);
        if (this.mImsManager != null) {
            try {
                this.mImsManager.removeRegistrationListener(this.mImsConnectionStateListener);
            } catch (ImsException e) {
                logd("ImsManager removeRegistrationListener failed, " + e.toString());
            }
        }
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.unregisterForIccChanged(this);
        this.mPhone.mCi.unregisterForCallForwardingInfo(this);
    }

    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.ACTION_SUPPLEMENTARY_SERVICE_UT_TEST");
        intentFilter.addAction("android.telephony.action.SIM_APPLICATION_STATE_CHANGED");
        intentFilter.addAction(ACTION_SYSTEM_UPDATE_SUCCESSFUL);
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
    }

    private void unRegisterBroadReceiver() {
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
    }

    public void dispose() {
        unRegisterEvent();
        unRegisterBroadReceiver();
    }

    @Override
    public void handleMessage(Message message) {
        logd("handleMessage: " + toEventString(message.what) + "(" + message.what + ")");
        AsyncResult asyncResult = (AsyncResult) message.obj;
        switch (message.what) {
            case 0:
            case 1:
                this.mSuppServTaskDriven.appendTask(new Task(0, false, toReasonString(message.what)));
                break;
            case 2:
                this.mAttached.set(true);
                if (hasMessages(14)) {
                    logd("remove EVENT_QUERY_CFU_OVER_CS_AFTER_DATA_NOT_ATTACHED, and then start CFU query again");
                    removeMessages(14);
                    startCFUQuery();
                } else {
                    this.mSuppServTaskDriven.appendTask(new Task(0, false, toReasonString(message.what)));
                }
                break;
            case 3:
                this.mAttached.set(false);
                break;
            case 4:
                if (asyncResult.exception == null) {
                    Message cFCallbackMessage = this.mPhone.getCFCallbackMessage();
                    AsyncResult.forMessage(cFCallbackMessage, asyncResult.result, asyncResult.exception);
                    cFCallbackMessage.sendToTarget();
                }
                taskDone();
                break;
            case 5:
                if (isMDSupportIMSSuppServ()) {
                    Message cFCallbackMessage2 = this.mPhone.getCFCallbackMessage();
                    AsyncResult.forMessage(cFCallbackMessage2, asyncResult.result, asyncResult.exception);
                    cFCallbackMessage2.sendToTarget();
                    this.mPhone.setSystemProperty("persist.vendor.radio.cfu_over_ims", MtkGsmCdmaPhone.ACT_TYPE_UTRAN);
                    taskDone();
                } else {
                    if (asyncResult.exception != null && (asyncResult.exception instanceof CommandException)) {
                        activeSubscriptionInfo = (CommandException) asyncResult.exception;
                        logd("cmdException error:" + activeSubscriptionInfo.getCommandError());
                    }
                    if (activeSubscriptionInfo != null && (activeSubscriptionInfo.getCommandError() == CommandException.Error.OPERATION_NOT_ALLOWED || (activeSubscriptionInfo != null && activeSubscriptionInfo.getCommandError() == CommandException.Error.NO_NETWORK_FOUND))) {
                        if (!isNotSupportUtToCS() && !isNoNeedToCSFBWhenIMSRegistered()) {
                            this.mSuppServTaskDriven.appendTask(new Task(1, false, toReasonString(message.what)));
                        }
                    } else if (activeSubscriptionInfo == null) {
                        Message cFCallbackMessage3 = this.mPhone.getCFCallbackMessage();
                        AsyncResult.forMessage(cFCallbackMessage3, asyncResult.result, asyncResult.exception);
                        cFCallbackMessage3.sendToTarget();
                    }
                    this.mPhone.setSystemProperty("persist.vendor.radio.cfu_over_ims", MtkGsmCdmaPhone.ACT_TYPE_UTRAN);
                    taskDone();
                }
                break;
            case 6:
            case 9:
            default:
                logd("Unhandled msg: " + message.what);
                break;
            case 7:
                if (!isNotSupportUtToCS()) {
                    this.mSuppServTaskDriven.appendTask(new Task(1, "Query Cfu over CS"));
                    break;
                }
                break;
            case 8:
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                if (asyncResult2 != null && asyncResult2.exception == null && asyncResult2.result != null) {
                    int[] iArr = (int[]) asyncResult2.result;
                    logd("handle EVENT_CFU_STATUS_FROM_MD:" + iArr[0]);
                    this.mCFUStatusFromMD = iArr[0];
                    break;
                }
                break;
            case 10:
                if (asyncResult.exception != null && (asyncResult.exception instanceof CommandException)) {
                    activeSubscriptionInfo = (CommandException) asyncResult.exception;
                    logd("cmdException error:" + activeSubscriptionInfo.getCommandError());
                }
                if (message.arg1 == 1 && activeSubscriptionInfo != null && activeSubscriptionInfo.getCommandError() == CommandException.Error.REQUEST_NOT_SUPPORTED) {
                    this.mSuppServTaskDriven.appendTask(new Task(3, false, toReasonString(message.what)));
                } else if (message.arg1 != 1 || activeSubscriptionInfo == null) {
                    Message cFTimeSlotCallbackMessage = this.mPhone.getCFTimeSlotCallbackMessage();
                    AsyncResult.forMessage(cFTimeSlotCallbackMessage, asyncResult.result, asyncResult.exception);
                    cFTimeSlotCallbackMessage.sendToTarget();
                }
                taskDone();
                break;
            case 11:
                if (asyncResult.exception != null && (asyncResult.exception instanceof CommandException)) {
                    activeSubscriptionInfo = (CommandException) asyncResult.exception;
                    logd("cmdException error:" + activeSubscriptionInfo.getCommandError());
                }
                if (message.arg1 == 1 && activeSubscriptionInfo != null && activeSubscriptionInfo.getCommandError() == CommandException.Error.REQUEST_NOT_SUPPORTED) {
                    this.mSuppServTaskDriven.appendTask(new Task(3, false, toReasonString(message.what)));
                } else if (message.arg1 != 1 || activeSubscriptionInfo == null) {
                    Message cFTimeSlotCallbackMessage2 = this.mPhone.getCFTimeSlotCallbackMessage();
                    AsyncResult.forMessage(cFTimeSlotCallbackMessage2, asyncResult.result, asyncResult.exception);
                    cFTimeSlotCallbackMessage2.sendToTarget();
                }
                this.mPhone.setSystemProperty("persist.vendor.radio.cfu_over_ims", MtkGsmCdmaPhone.ACT_TYPE_UTRAN);
                taskDone();
                break;
            case 12:
                setSimRecordsLoaded(true);
                if (isNeedSyncSysPropToSIMforOTA() && syncSysPropToSIMforOTA()) {
                    setNeedSyncSysPropToSIMforOTA(false);
                    this.mNeeedSyncForOTA = 0;
                }
                notifyCdmaCallForwardingIndicator();
                String str = SystemProperties.get("persist.vendor.radio.cfu.iccid." + this.mPhone.getPhoneId(), "");
                SubscriptionManager subscriptionManagerFrom = SubscriptionManager.from(this.mContext);
                activeSubscriptionInfo = subscriptionManagerFrom != null ? subscriptionManagerFrom.getActiveSubscriptionInfo(this.mPhone.getSubId()) : null;
                if (activeSubscriptionInfo != null && activeSubscriptionInfo.getIccId().equals(str)) {
                    int iCheckEfCfis = checkEfCfis();
                    logd("Sim loaded, notify cfu icon, efcfis status: " + iCheckEfCfis);
                    if (iCheckEfCfis == 3 && !this.mNotifyCFUIfEFCFISisInvalidDone) {
                        boolean z = getCFUStatusFromLocal() == 1;
                        logd("Notify CFU icon: " + z);
                        this.mPhone.setVoiceCallForwardingFlag(1, z, "");
                        this.mNotifyCFUIfEFCFISisInvalidDone = true;
                    }
                }
                this.mSuppServTaskDriven.appendTask(new Task(0, false, toReasonString(message.what)));
                break;
            case 13:
                onUpdateIcc();
                break;
            case 14:
                logd("Receive the event for query CFU over CS after data not attached");
                if (!isNotSupportUtToCS()) {
                    this.mSuppServTaskDriven.appendTask(new Task(1, "Query Cfu over CS"));
                    break;
                }
                break;
            case 15:
                setCarrierConfigLoaded(true);
                this.mSuppServTaskDriven.appendTask(new Task(0, false, toReasonString(message.what)));
                break;
            case 16:
                logd("Receive EVENT_CLEAN_CFU_STATUS, SIM has disposed");
                this.mSuppServTaskDriven.appendTask(new Task(4, toReasonString(message.what)));
                break;
        }
    }

    private void queryCallForwardStatusOverGSM() {
        getCallForwardingOption(0, false);
    }

    private void queryCallForwardStatusOverIMS() {
        if (isIMSRegistered()) {
            if (isSupportCFUTimeSlot()) {
                getCallForwardingOption(2, true);
                return;
            } else {
                getCallForwardingOption(2, false);
                return;
            }
        }
        taskDone();
    }

    private int checkEfCfis() {
        MtkSIMRecords iccRecords = this.mPhone.getIccRecords();
        if (iccRecords != null && (iccRecords instanceof MtkSIMRecords)) {
            if (iccRecords.checkEfCfis()) {
                return 2;
            }
            return 3;
        }
        return 0;
    }

    private void setNeedSyncSysPropToSIMforOTA(boolean z) {
        TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "persist.vendor.radio.cfu.sync_for_ota", z ? "1" : "0");
    }

    private boolean isNeedSyncSysPropToSIMforOTA() {
        if (TelephonyManager.getTelephonyProperty(this.mPhone.getPhoneId(), "persist.vendor.radio.cfu.sync_for_ota", "0").equals("1")) {
            return true;
        }
        return false;
    }

    private boolean syncSysPropToSIMforOTA() {
        if (this.mNeeedSyncForOTA == 0) {
            logd("syncSysPropToSIMforOTA: No need to sync (sim change): " + this.mNeeedSyncForOTA);
            return true;
        }
        if (this.mNeeedSyncForOTA == -1) {
            logd("syncSysPropToSIMforOTA: No need to sync (unknown): " + this.mNeeedSyncForOTA);
            return false;
        }
        if (!getSimRecordsLoaded()) {
            logd("syncSysPropToSIMforOTA: SIM not loaded.");
            return false;
        }
        if (this.mCFUStatusFromMD == -1) {
            logd("syncSysPropToSIMforOTA: ECFU not receive yet.");
            return false;
        }
        int iCheckEfCfis = checkEfCfis();
        logd("syncSysPropToSIMforOTA: checkEfCfis = " + iCheckEfCfis);
        if (iCheckEfCfis == 0) {
            return false;
        }
        if (iCheckEfCfis != 2) {
            return iCheckEfCfis == 3 ? true : true;
        }
        int cFUStatusFromLocal = getCFUStatusFromLocal();
        if (cFUStatusFromLocal == 1) {
            logd("syncSysPropToSIMforOTA: true from system property.");
            this.mPhone.setVoiceCallForwardingFlag(1, true, "");
        } else if (cFUStatusFromLocal == 0) {
            logd("syncSysPropToSIMforOTA: false from system property. (do nothing.)");
        } else if (this.mCFUStatusFromMD == 1) {
            logd("syncSysPropToSIMforOTA: from MD.");
            this.mPhone.setVoiceCallForwardingFlag(1, true, "");
        }
        return true;
    }

    private boolean needQueryCFUOverIms() {
        return false;
    }

    public void setIccRecordsReady() {
        obtainMessage(1).sendToTarget();
    }

    private void reset() {
        this.mNeedGetCFU = true;
        this.mSuppServTaskDriven.clearPendingTask();
    }

    private void getCallForwardingOption(int i, boolean z) {
        MtkSuppServQueueHelper suppServQueueHelper = MtkSuppServManager.getSuppServQueueHelper();
        if (suppServQueueHelper == null) {
            switch (i) {
                case 0:
                    queryCallForwardingOption(i, z, obtainMessage(4));
                    break;
                case 1:
                    if (z) {
                        if (isMDSupportIMSSuppServ()) {
                            queryCallForwardingOption(i, z, obtainMessage(10));
                        } else {
                            queryCallForwardingOption(i, z, obtainMessage(10, 1, 0, null));
                        }
                    } else if (isMDSupportIMSSuppServ()) {
                        queryCallForwardingOption(i, z, obtainMessage(4));
                    } else {
                        queryCallForwardingOption(i, z, obtainMessage(4, null));
                    }
                    break;
                case 2:
                    if (z) {
                        queryCallForwardingOption(i, z, obtainMessage(11, 1, 0, null));
                    } else {
                        queryCallForwardingOption(i, z, obtainMessage(5, null));
                    }
                    break;
            }
        }
        switch (i) {
            case 0:
                suppServQueueHelper.getCallForwardingOption(i, z ? 1 : 0, obtainMessage(4), this.mPhone.getPhoneId());
                break;
            case 1:
                if (z) {
                    if (isMDSupportIMSSuppServ()) {
                        suppServQueueHelper.getCallForwardingOption(i, z ? 1 : 0, obtainMessage(10), this.mPhone.getPhoneId());
                    } else {
                        suppServQueueHelper.getCallForwardingOption(i, z ? 1 : 0, obtainMessage(10, 1, 0, null), this.mPhone.getPhoneId());
                    }
                } else if (isMDSupportIMSSuppServ()) {
                    suppServQueueHelper.getCallForwardingOption(i, z ? 1 : 0, obtainMessage(4), this.mPhone.getPhoneId());
                } else {
                    suppServQueueHelper.getCallForwardingOption(i, z ? 1 : 0, obtainMessage(4, null), this.mPhone.getPhoneId());
                }
                break;
            case 2:
                if (z) {
                    suppServQueueHelper.getCallForwardingOption(i, z ? 1 : 0, obtainMessage(11, 1, 0, null), this.mPhone.getPhoneId());
                } else {
                    suppServQueueHelper.getCallForwardingOption(i, z ? 1 : 0, obtainMessage(5, null), this.mPhone.getPhoneId());
                }
                break;
        }
    }

    public void queryCallForwardingOption(int i, boolean z, Message message) {
        logd("queryCallForwardingOption, reason: " + i + ", withTimeSlot: " + z);
        switch (i) {
            case 0:
                if (isVoiceInService()) {
                    this.mPhone.mCi.queryCallForwardStatus(0, 1, (String) null, message);
                } else {
                    AsyncResult.forMessage(message, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                    message.sendToTarget();
                    taskDone();
                }
                break;
            case 1:
                if (z) {
                    if (isMDSupportIMSSuppServ()) {
                        this.mPhone.mMtkCi.queryCallForwardInTimeSlotStatus(0, 1, message);
                    } else {
                        this.mPhone.getMtkSSRequestDecisionMaker().queryCallForwardInTimeSlotStatus(0, 1, message);
                    }
                } else if (isMDSupportIMSSuppServ()) {
                    this.mPhone.mCi.queryCallForwardStatus(0, 1, (String) null, message);
                } else {
                    this.mPhone.getMtkSSRequestDecisionMaker().queryCallForwardStatus(0, 1, null, message);
                }
                break;
            case 2:
                MtkImsPhone imsPhone = this.mPhone.getImsPhone();
                if (z) {
                    imsPhone.getCallForwardInTimeSlot(0, message);
                } else {
                    imsPhone.getCallForwardingOption(0, message);
                }
                break;
        }
    }

    public void notifyCarrierConfigLoaded() {
        obtainMessage(15).sendToTarget();
    }

    private boolean isSupportSuppServUTTest() {
        return SystemProperties.get("persist.vendor.ims_support").equals("1") && SystemProperties.get("persist.vendor.volte_support").equals("1") && this.mPhone.getPhoneId() == 0;
    }

    private boolean isMDSupportIMSSuppServ() {
        if (SystemProperties.get("ro.vendor.md_auto_setup_ims").equals("1")) {
            return true;
        }
        return false;
    }

    private boolean isNotSupportUtToCS() {
        return this.mPhone.isNotSupportUtToCSforCFUQuery();
    }

    private boolean isNoNeedToCSFBWhenIMSRegistered() {
        return this.mPhone.isNoNeedToCSFBWhenIMSRegistered();
    }

    private boolean isSupportCFUTimeSlot() {
        return this.mPhone.isSupportCFUTimeSlot();
    }

    private boolean isResetCSFBStatusAfterFlightMode() {
        return this.mPhone.isResetCSFBStatusAfterFlightMode();
    }

    private MtkSuppServUtTest makeMtkSuppServUtTest(Intent intent) {
        return new MtkSuppServUtTest(this.mContext, intent, this.mPhone);
    }

    private int getCFUStatusFromLocal() {
        String telephonyProperty = TelephonyManager.getTelephonyProperty(this.mPhone.getPhoneId(), "persist.vendor.radio.cfu.mode", "disabled_cfu_mode");
        logd("getCFUStatusFromLocal: " + telephonyProperty);
        if ("enabled_cfu_mode_on".equals(telephonyProperty)) {
            return 1;
        }
        if ("enabled_cfu_mode_off".equals(telephonyProperty)) {
            return 0;
        }
        return -1;
    }

    private void notifyCdmaCallForwardingIndicator() {
        if (this.mPhone.isGsmSsPrefer() && this.mPhone.getPhoneType() == 2) {
            this.mPhone.notifyCallForwardingIndicator();
        }
    }

    public String getXCAPErrorMessageFromSysProp(CommandException.Error error) {
        String str = "vendor.gsm.radio.ss.errormsg." + this.mPhone.getPhoneId();
        String str2 = "";
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append(".");
        int i = 0;
        sb.append(0);
        String str3 = SystemProperties.get(sb.toString(), "");
        while (!str3.equals("")) {
            str2 = str2 + str3;
            i++;
            str3 = SystemProperties.get(str + "." + i, "");
        }
        logd("fullErrorMsg: " + str2);
        if (AnonymousClass3.$SwitchMap$com$android$internal$telephony$CommandException$Error[error.ordinal()] != 1 || !str2.startsWith("409")) {
            return null;
        }
        String strSubstring = str2.substring("409".length() + 1);
        logd("errorMsg: " + strSubstring);
        return strSubstring;
    }

    static class AnonymousClass3 {
        static final int[] $SwitchMap$com$android$internal$telephony$CommandException$Error = new int[CommandException.Error.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.OEM_ERROR_1.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
        }
    }

    private void cleanCFUStatus() {
        if (this.mPhone != null) {
            setAlwaysQueryDoneFlag(false);
            this.mPhone.cleanCallForwardingIndicatorFromSharedPref();
            this.mPhone.notifyCallForwardingIndicatorWithoutCheckSimState();
        }
        taskDone();
    }

    private String toReasonString(int i) {
        if (i != 12) {
            switch (i) {
                case 0:
                    return "CS in service";
                case 1:
                    return "ICCRecords ready";
                case 2:
                    return "Data Attached";
                default:
                    switch (i) {
                        case 15:
                            return "Carrier config loaded";
                        case 16:
                            return "Clean CFU status";
                        default:
                            return "Unknown reason, should not be here.";
                    }
            }
        }
        return "SIM records loaded";
    }

    private String toEventString(int i) {
        switch (i) {
            case 0:
                return "EVENT_REGISTERED_TO_NETWORK";
            case 1:
                return "EVENT_ICCRECORDS_READY";
            case 2:
                return "EVENT_DATA_CONNECTION_ATTACHED";
            case 3:
                return "EVENT_DATA_CONNECTION_DETACHED";
            case 4:
                return "EVENT_GET_CALL_FORWARD_BY_GSM_DONE";
            case 5:
                return "EVENT_GET_CALL_FORWARD_BY_IMS_DONE";
            case 6:
                return "EVENT_CALL_FORWARDING_STATUS_FROM_MD";
            case 7:
                return "EVENT_QUERY_CFU_OVER_CS";
            case 8:
                return "EVENT_CFU_STATUS_FROM_MD";
            case 9:
                return "EVENT_SS_RESET";
            case 10:
                return "EVENT_GET_CALL_FORWARD_TIME_SLOT_BY_GSM_DONE";
            case 11:
                return "EVENT_GET_CALL_FORWARD_TIME_SLOT_BY_IMS_DONE";
            case 12:
                return "EVENT_SIM_RECORDS_LOADED";
            case 13:
                return "EVENT_ICC_CHANGED";
            case 14:
                return "EVENT_QUERY_CFU_OVER_CS_AFTER_DATA_NOT_ATTACHED";
            case 15:
                return "EVENT_CARRIER_CONFIG_LOADED";
            case 16:
                return "EVENT_CLEAN_CFU_STATUS";
            default:
                return "UNKNOWN_EVENT_ID";
        }
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logw(String str) {
        Rlog.w(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logi(String str) {
        Rlog.i(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logd(String str) {
        Rlog.d(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logv(String str) {
        Rlog.v(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }
}
