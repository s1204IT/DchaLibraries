package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.util.Log;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.AdnRecordCache;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.mediatek.internal.telephony.uicc.AlphaTag;
import com.mediatek.internal.telephony.uicc.UsimGroup;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class IccPhoneBookInterfaceManager {

    private static final int[] f9xa0f04c1a = null;
    protected static final boolean ALLOW_SIM_OP_IN_UI_THREAD = false;
    protected static final int EVENT_GET_SIZE_DONE = 1;
    protected static final int EVENT_LOAD_DONE = 2;
    protected static final int EVENT_UPDATE_DONE = 3;
    protected AdnRecordCache mAdnCache;
    protected int mErrorCause;
    protected Phone mPhone;
    protected int[] mRecordSize;
    protected List<AdnRecord> mRecords;
    protected boolean mSuccess;
    static final String LOG_TAG = "IccPhoneBookIM";
    protected static final boolean DBG = Log.isLoggable(LOG_TAG, 3);
    private UiccCardApplication mCurrentApp = null;
    protected final Object mLock = new Object();
    private boolean mIs3gCard = ALLOW_SIM_OP_IN_UI_THREAD;
    protected int mSlotId = -1;
    protected Handler mBaseHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Object obj;
            switch (msg.what) {
                case 1:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    obj = IccPhoneBookInterfaceManager.this.mLock;
                    synchronized (obj) {
                        if (ar.exception == null) {
                            IccPhoneBookInterfaceManager.this.mRecordSize = (int[]) ar.result;
                            IccPhoneBookInterfaceManager.this.logd("GET_RECORD_SIZE Size " + IccPhoneBookInterfaceManager.this.mRecordSize[0] + " total " + IccPhoneBookInterfaceManager.this.mRecordSize[1] + " #record " + IccPhoneBookInterfaceManager.this.mRecordSize[2]);
                        }
                        notifyPending(ar);
                        break;
                    }
                    break;
                case 2:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    obj = IccPhoneBookInterfaceManager.this.mLock;
                    synchronized (obj) {
                        if (ar2.exception == null) {
                            IccPhoneBookInterfaceManager.this.mRecords = (List) ar2.result;
                        } else {
                            if (IccPhoneBookInterfaceManager.DBG) {
                                IccPhoneBookInterfaceManager.this.logd("Cannot load ADN records");
                            }
                            IccPhoneBookInterfaceManager.this.mRecords = null;
                        }
                        notifyPending(ar2);
                        break;
                    }
                    break;
                case 3:
                    IccPhoneBookInterfaceManager.this.logd("EVENT_UPDATE_DONE");
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    synchronized (IccPhoneBookInterfaceManager.this.mLock) {
                        IccPhoneBookInterfaceManager.this.mSuccess = ar3.exception == null;
                        IccPhoneBookInterfaceManager.this.logd("EVENT_UPDATE_DONE mSuccess:" + IccPhoneBookInterfaceManager.this.mSuccess);
                        if (IccPhoneBookInterfaceManager.this.mSuccess) {
                            IccPhoneBookInterfaceManager.this.mErrorCause = 1;
                        } else if (ar3.exception instanceof CommandException) {
                            IccPhoneBookInterfaceManager.this.mErrorCause = IccPhoneBookInterfaceManager.this.getErrorCauseFromException((CommandException) ar3.exception);
                        } else {
                            IccPhoneBookInterfaceManager.this.loge("Error : Unknow exception instance");
                            IccPhoneBookInterfaceManager.this.mErrorCause = -10;
                        }
                        IccPhoneBookInterfaceManager.this.logi("update done result: " + IccPhoneBookInterfaceManager.this.mErrorCause);
                        notifyPending(ar3);
                    }
                    return;
                default:
                    return;
            }
        }

        private void notifyPending(AsyncResult ar) {
            if (ar.userObj == null) {
                return;
            }
            try {
                AtomicBoolean status = (AtomicBoolean) ar.userObj;
                status.set(true);
                IccPhoneBookInterfaceManager.this.mLock.notifyAll();
            } catch (ClassCastException e) {
                IccPhoneBookInterfaceManager.this.loge("notifyPending " + e.getMessage());
            }
        }
    };

    private static int[] m24x9944abe() {
        if (f9xa0f04c1a != null) {
            return f9xa0f04c1a;
        }
        int[] iArr = new int[CommandException.Error.valuesCustom().length];
        try {
            iArr[CommandException.Error.ABORTED.ordinal()] = 15;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[CommandException.Error.ADDITIONAL_NUMBER_SAVE_FAILURE.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[CommandException.Error.ADDITIONAL_NUMBER_STRING_TOO_LONG.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[CommandException.Error.ADN_LIST_NOT_EXIST.ordinal()] = 3;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[CommandException.Error.CALL_BARRED.ordinal()] = 16;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[CommandException.Error.CC_CALL_HOLD_FAILED_CAUSED_BY_TERMINATED.ordinal()] = 17;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[CommandException.Error.DEVICE_IN_USE.ordinal()] = 18;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[CommandException.Error.DIAL_MODIFIED_TO_DIAL.ordinal()] = 19;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[CommandException.Error.DIAL_MODIFIED_TO_SS.ordinal()] = 20;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[CommandException.Error.DIAL_MODIFIED_TO_USSD.ordinal()] = 21;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[CommandException.Error.DIAL_STRING_TOO_LONG.ordinal()] = 4;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[CommandException.Error.EMAIL_NAME_TOOLONG.ordinal()] = 5;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[CommandException.Error.EMAIL_SIZE_LIMIT.ordinal()] = 6;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[CommandException.Error.EMPTY_RECORD.ordinal()] = 22;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[CommandException.Error.ENCODING_ERR.ordinal()] = 23;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[CommandException.Error.FDN_CHECK_FAILURE.ordinal()] = 24;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[CommandException.Error.GENERIC_FAILURE.ordinal()] = 7;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[CommandException.Error.ILLEGAL_SIM_OR_ME.ordinal()] = 25;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[CommandException.Error.INTERNAL_ERR.ordinal()] = 26;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[CommandException.Error.INVALID_ARGUMENTS.ordinal()] = 27;
        } catch (NoSuchFieldError e20) {
        }
        try {
            iArr[CommandException.Error.INVALID_CALL_ID.ordinal()] = 28;
        } catch (NoSuchFieldError e21) {
        }
        try {
            iArr[CommandException.Error.INVALID_MODEM_STATE.ordinal()] = 29;
        } catch (NoSuchFieldError e22) {
        }
        try {
            iArr[CommandException.Error.INVALID_PARAMETER.ordinal()] = 30;
        } catch (NoSuchFieldError e23) {
        }
        try {
            iArr[CommandException.Error.INVALID_RESPONSE.ordinal()] = 31;
        } catch (NoSuchFieldError e24) {
        }
        try {
            iArr[CommandException.Error.INVALID_SIM_STATE.ordinal()] = 32;
        } catch (NoSuchFieldError e25) {
        }
        try {
            iArr[CommandException.Error.INVALID_SMSC_ADDRESS.ordinal()] = 33;
        } catch (NoSuchFieldError e26) {
        }
        try {
            iArr[CommandException.Error.INVALID_SMS_FORMAT.ordinal()] = 34;
        } catch (NoSuchFieldError e27) {
        }
        try {
            iArr[CommandException.Error.INVALID_STATE.ordinal()] = 35;
        } catch (NoSuchFieldError e28) {
        }
        try {
            iArr[CommandException.Error.LCE_NOT_SUPPORTED.ordinal()] = 36;
        } catch (NoSuchFieldError e29) {
        }
        try {
            iArr[CommandException.Error.MISSING_RESOURCE.ordinal()] = 37;
        } catch (NoSuchFieldError e30) {
        }
        try {
            iArr[CommandException.Error.MODEM_ERR.ordinal()] = 38;
        } catch (NoSuchFieldError e31) {
        }
        try {
            iArr[CommandException.Error.MODE_NOT_SUPPORTED.ordinal()] = 39;
        } catch (NoSuchFieldError e32) {
        }
        try {
            iArr[CommandException.Error.NETWORK_ERR.ordinal()] = 40;
        } catch (NoSuchFieldError e33) {
        }
        try {
            iArr[CommandException.Error.NETWORK_NOT_READY.ordinal()] = 41;
        } catch (NoSuchFieldError e34) {
        }
        try {
            iArr[CommandException.Error.NETWORK_REJECT.ordinal()] = 42;
        } catch (NoSuchFieldError e35) {
        }
        try {
            iArr[CommandException.Error.NOT_PROVISIONED.ordinal()] = 43;
        } catch (NoSuchFieldError e36) {
        }
        try {
            iArr[CommandException.Error.NOT_READY.ordinal()] = 8;
        } catch (NoSuchFieldError e37) {
        }
        try {
            iArr[CommandException.Error.NO_MEMORY.ordinal()] = 44;
        } catch (NoSuchFieldError e38) {
        }
        try {
            iArr[CommandException.Error.NO_NETWORK_FOUND.ordinal()] = 45;
        } catch (NoSuchFieldError e39) {
        }
        try {
            iArr[CommandException.Error.NO_RESOURCES.ordinal()] = 46;
        } catch (NoSuchFieldError e40) {
        }
        try {
            iArr[CommandException.Error.NO_SMS_TO_ACK.ordinal()] = 47;
        } catch (NoSuchFieldError e41) {
        }
        try {
            iArr[CommandException.Error.NO_SUBSCRIPTION.ordinal()] = 48;
        } catch (NoSuchFieldError e42) {
        }
        try {
            iArr[CommandException.Error.NO_SUCH_ELEMENT.ordinal()] = 49;
        } catch (NoSuchFieldError e43) {
        }
        try {
            iArr[CommandException.Error.NO_SUCH_ENTRY.ordinal()] = 50;
        } catch (NoSuchFieldError e44) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_1.ordinal()] = 51;
        } catch (NoSuchFieldError e45) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_10.ordinal()] = 52;
        } catch (NoSuchFieldError e46) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_11.ordinal()] = 53;
        } catch (NoSuchFieldError e47) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_12.ordinal()] = 54;
        } catch (NoSuchFieldError e48) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_13.ordinal()] = 55;
        } catch (NoSuchFieldError e49) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_14.ordinal()] = 56;
        } catch (NoSuchFieldError e50) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_15.ordinal()] = 57;
        } catch (NoSuchFieldError e51) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_16.ordinal()] = 58;
        } catch (NoSuchFieldError e52) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_17.ordinal()] = 59;
        } catch (NoSuchFieldError e53) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_18.ordinal()] = 60;
        } catch (NoSuchFieldError e54) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_19.ordinal()] = 61;
        } catch (NoSuchFieldError e55) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_2.ordinal()] = 62;
        } catch (NoSuchFieldError e56) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_20.ordinal()] = 63;
        } catch (NoSuchFieldError e57) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_21.ordinal()] = 64;
        } catch (NoSuchFieldError e58) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_22.ordinal()] = 65;
        } catch (NoSuchFieldError e59) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_23.ordinal()] = 66;
        } catch (NoSuchFieldError e60) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_24.ordinal()] = 67;
        } catch (NoSuchFieldError e61) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_25.ordinal()] = 68;
        } catch (NoSuchFieldError e62) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_3.ordinal()] = 69;
        } catch (NoSuchFieldError e63) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_4.ordinal()] = 70;
        } catch (NoSuchFieldError e64) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_5.ordinal()] = 71;
        } catch (NoSuchFieldError e65) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_6.ordinal()] = 72;
        } catch (NoSuchFieldError e66) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_7.ordinal()] = 73;
        } catch (NoSuchFieldError e67) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_8.ordinal()] = 74;
        } catch (NoSuchFieldError e68) {
        }
        try {
            iArr[CommandException.Error.OEM_ERROR_9.ordinal()] = 75;
        } catch (NoSuchFieldError e69) {
        }
        try {
            iArr[CommandException.Error.OPERATION_NOT_ALLOWED.ordinal()] = 76;
        } catch (NoSuchFieldError e70) {
        }
        try {
            iArr[CommandException.Error.OP_NOT_ALLOWED_BEFORE_REG_NW.ordinal()] = 77;
        } catch (NoSuchFieldError e71) {
        }
        try {
            iArr[CommandException.Error.OP_NOT_ALLOWED_DURING_VOICE_CALL.ordinal()] = 78;
        } catch (NoSuchFieldError e72) {
        }
        try {
            iArr[CommandException.Error.PASSWORD_INCORRECT.ordinal()] = 9;
        } catch (NoSuchFieldError e73) {
        }
        try {
            iArr[CommandException.Error.RADIO_NOT_AVAILABLE.ordinal()] = 79;
        } catch (NoSuchFieldError e74) {
        }
        try {
            iArr[CommandException.Error.REQUEST_NOT_SUPPORTED.ordinal()] = 80;
        } catch (NoSuchFieldError e75) {
        }
        try {
            iArr[CommandException.Error.REQUEST_RATE_LIMITED.ordinal()] = 81;
        } catch (NoSuchFieldError e76) {
        }
        try {
            iArr[CommandException.Error.SIM_ABSENT.ordinal()] = 82;
        } catch (NoSuchFieldError e77) {
        }
        try {
            iArr[CommandException.Error.SIM_ALREADY_POWERED_OFF.ordinal()] = 83;
        } catch (NoSuchFieldError e78) {
        }
        try {
            iArr[CommandException.Error.SIM_ALREADY_POWERED_ON.ordinal()] = 84;
        } catch (NoSuchFieldError e79) {
        }
        try {
            iArr[CommandException.Error.SIM_BUSY.ordinal()] = 85;
        } catch (NoSuchFieldError e80) {
        }
        try {
            iArr[CommandException.Error.SIM_DATA_NOT_AVAILABLE.ordinal()] = 86;
        } catch (NoSuchFieldError e81) {
        }
        try {
            iArr[CommandException.Error.SIM_ERR.ordinal()] = 87;
        } catch (NoSuchFieldError e82) {
        }
        try {
            iArr[CommandException.Error.SIM_FULL.ordinal()] = 88;
        } catch (NoSuchFieldError e83) {
        }
        try {
            iArr[CommandException.Error.SIM_MEM_FULL.ordinal()] = 10;
        } catch (NoSuchFieldError e84) {
        }
        try {
            iArr[CommandException.Error.SIM_PIN2.ordinal()] = 89;
        } catch (NoSuchFieldError e85) {
        }
        try {
            iArr[CommandException.Error.SIM_PUK2.ordinal()] = 11;
        } catch (NoSuchFieldError e86) {
        }
        try {
            iArr[CommandException.Error.SIM_SAP_CONNECT_FAILURE.ordinal()] = 90;
        } catch (NoSuchFieldError e87) {
        }
        try {
            iArr[CommandException.Error.SIM_SAP_CONNECT_OK_CALL_ONGOING.ordinal()] = 91;
        } catch (NoSuchFieldError e88) {
        }
        try {
            iArr[CommandException.Error.SIM_SAP_MSG_SIZE_TOO_LARGE.ordinal()] = 92;
        } catch (NoSuchFieldError e89) {
        }
        try {
            iArr[CommandException.Error.SIM_SAP_MSG_SIZE_TOO_SMALL.ordinal()] = 93;
        } catch (NoSuchFieldError e90) {
        }
        try {
            iArr[CommandException.Error.SMS_FAIL_RETRY.ordinal()] = 94;
        } catch (NoSuchFieldError e91) {
        }
        try {
            iArr[CommandException.Error.SNE_NAME_TOOLONG.ordinal()] = 12;
        } catch (NoSuchFieldError e92) {
        }
        try {
            iArr[CommandException.Error.SNE_SIZE_LIMIT.ordinal()] = 13;
        } catch (NoSuchFieldError e93) {
        }
        try {
            iArr[CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED.ordinal()] = 95;
        } catch (NoSuchFieldError e94) {
        }
        try {
            iArr[CommandException.Error.SS_MODIFIED_TO_DIAL.ordinal()] = 96;
        } catch (NoSuchFieldError e95) {
        }
        try {
            iArr[CommandException.Error.SS_MODIFIED_TO_SS.ordinal()] = 97;
        } catch (NoSuchFieldError e96) {
        }
        try {
            iArr[CommandException.Error.SS_MODIFIED_TO_USSD.ordinal()] = 98;
        } catch (NoSuchFieldError e97) {
        }
        try {
            iArr[CommandException.Error.SUBSCRIPTION_NOT_AVAILABLE.ordinal()] = 99;
        } catch (NoSuchFieldError e98) {
        }
        try {
            iArr[CommandException.Error.SUBSCRIPTION_NOT_SUPPORTED.ordinal()] = 100;
        } catch (NoSuchFieldError e99) {
        }
        try {
            iArr[CommandException.Error.SYSTEM_ERR.ordinal()] = 101;
        } catch (NoSuchFieldError e100) {
        }
        try {
            iArr[CommandException.Error.TEXT_STRING_TOO_LONG.ordinal()] = 14;
        } catch (NoSuchFieldError e101) {
        }
        try {
            iArr[CommandException.Error.USSD_MODIFIED_TO_DIAL.ordinal()] = 102;
        } catch (NoSuchFieldError e102) {
        }
        try {
            iArr[CommandException.Error.USSD_MODIFIED_TO_SS.ordinal()] = 103;
        } catch (NoSuchFieldError e103) {
        }
        try {
            iArr[CommandException.Error.USSD_MODIFIED_TO_USSD.ordinal()] = 104;
        } catch (NoSuchFieldError e104) {
        }
        try {
            iArr[CommandException.Error.UT_UNKNOWN_HOST.ordinal()] = 105;
        } catch (NoSuchFieldError e105) {
        }
        try {
            iArr[CommandException.Error.UT_XCAP_403_FORBIDDEN.ordinal()] = 106;
        } catch (NoSuchFieldError e106) {
        }
        try {
            iArr[CommandException.Error.UT_XCAP_404_NOT_FOUND.ordinal()] = 107;
        } catch (NoSuchFieldError e107) {
        }
        try {
            iArr[CommandException.Error.UT_XCAP_409_CONFLICT.ordinal()] = 108;
        } catch (NoSuchFieldError e108) {
        }
        f9xa0f04c1a = iArr;
        return iArr;
    }

    public IccPhoneBookInterfaceManager(Phone phone) {
        this.mPhone = phone;
        IccRecords r = phone.getIccRecords();
        if (r == null) {
            return;
        }
        this.mAdnCache = r.getAdnCache();
    }

    public void dispose() {
    }

    public void updateIccRecords(IccRecords iccRecords) {
        if (iccRecords != null) {
            this.mAdnCache = iccRecords.getAdnCache();
            this.mSlotId = this.mAdnCache != null ? this.mAdnCache.getSlotId() : -1;
            logi("[updateIccRecords] Set mAdnCache value");
        } else {
            this.mAdnCache = null;
            logi("[updateIccRecords] Set mAdnCache value to null");
            this.mSlotId = -1;
        }
    }

    protected void logd(String msg) {
        Rlog.d(LOG_TAG, "[IccPbInterfaceManager] " + msg + "(slot " + this.mSlotId + ")");
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, "[IccPbInterfaceManager] " + msg + "(slot " + this.mSlotId + ")");
    }

    protected void logi(String msg) {
        Rlog.i(LOG_TAG, "[IccPbInterfaceManager] " + msg + "(slot " + this.mSlotId + ")");
    }

    public boolean updateAdnRecordsInEfBySearch(int efid, String oldTag, String oldPhoneNumber, String newTag, String newPhoneNumber, String pin2) {
        int result = updateAdnRecordsInEfBySearchWithError(efid, oldTag, oldPhoneNumber, newTag, newPhoneNumber, pin2);
        if (result == 1) {
            return true;
        }
        return ALLOW_SIM_OP_IN_UI_THREAD;
    }

    public synchronized int updateAdnRecordsInEfBySearchWithError(int efid, String oldTag, String oldPhoneNumber, String newTag, String newPhoneNumber, String pin2) {
        int index = -1;
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            loge("updateAdnRecordsInEfBySearchWithError mAdnCache is null");
            return 0;
        }
        if (DBG) {
            logd("updateAdnRecordsInEfBySearch: efid=0x" + Integer.toHexString(efid).toUpperCase() + " (" + oldTag + "," + oldPhoneNumber + ")==> (" + newTag + "," + newPhoneNumber + ") pin2=" + pin2);
        }
        int efid2 = updateEfForIccType(efid);
        synchronized (this.mLock) {
            checkThread();
            this.mSuccess = ALLOW_SIM_OP_IN_UI_THREAD;
            AtomicBoolean status = new AtomicBoolean(ALLOW_SIM_OP_IN_UI_THREAD);
            Message response = this.mBaseHandler.obtainMessage(3, status);
            AdnRecord oldAdn = new AdnRecord(oldTag, oldPhoneNumber);
            if (newPhoneNumber == null) {
                newPhoneNumber = UsimPBMemInfo.STRING_NOT_SET;
            }
            AdnRecord newAdn = new AdnRecord(newTag, newPhoneNumber);
            if (this.mAdnCache != null) {
                index = this.mAdnCache.updateAdnBySearch(efid2, oldAdn, newAdn, pin2, response);
                waitForResult(status);
            } else {
                loge("Failure while trying to update by search due to uninitialised adncache");
            }
        }
        if (this.mErrorCause == 1) {
            logi("updateAdnRecordsInEfBySearchWithError success index is " + index);
            return index;
        }
        return this.mErrorCause;
    }

    public synchronized int updateUsimPBRecordsInEfBySearchWithError(int efid, String oldTag, String oldPhoneNumber, String oldAnr, String oldGrpIds, String[] oldEmails, String newTag, String newPhoneNumber, String newAnr, String newGrpIds, String[] newEmails) {
        int index;
        AtomicBoolean status = new AtomicBoolean(ALLOW_SIM_OP_IN_UI_THREAD);
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            loge("updateUsimPBRecordsInEfBySearchWithError mAdnCache is null");
            return 0;
        }
        if (DBG) {
            logd("updateUsimPBRecordsInEfBySearchWithError: efid=" + efid + " (" + oldTag + "," + oldPhoneNumber + "oldAnr" + oldAnr + " oldGrpIds " + oldGrpIds + ")==>(" + newTag + "," + newPhoneNumber + ") newAnr= " + newAnr + " newGrpIds = " + newGrpIds + " newEmails = " + newEmails);
        }
        synchronized (this.mLock) {
            checkThread();
            this.mSuccess = ALLOW_SIM_OP_IN_UI_THREAD;
            Message response = this.mBaseHandler.obtainMessage(3, status);
            AdnRecord oldAdn = new AdnRecord(oldTag, oldPhoneNumber);
            if (newPhoneNumber == null) {
                newPhoneNumber = UsimPBMemInfo.STRING_NOT_SET;
            }
            AdnRecord newAdn = new AdnRecord(0, 0, newTag, newPhoneNumber, newAnr, newEmails, newGrpIds);
            index = this.mAdnCache.updateAdnBySearch(efid, oldAdn, newAdn, null, response);
            waitForResult(status);
        }
        if (this.mErrorCause == 1) {
            logi("updateUsimPBRecordsInEfBySearchWithError success index is " + index);
            return index;
        }
        return this.mErrorCause;
    }

    public synchronized int updateUsimPBRecordsBySearchWithError(int efid, AdnRecord oldAdn, AdnRecord newAdn) {
        int index;
        AtomicBoolean status = new AtomicBoolean(ALLOW_SIM_OP_IN_UI_THREAD);
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            loge("updateUsimPBRecordsBySearchWithError mAdnCache is null");
            return 0;
        }
        if (DBG) {
            logd("updateUsimPBRecordsBySearchWithError: efid=" + efid + " (" + oldAdn + ")==>(" + newAdn + ")");
        }
        synchronized (this.mLock) {
            checkThread();
            this.mSuccess = ALLOW_SIM_OP_IN_UI_THREAD;
            Message response = this.mBaseHandler.obtainMessage(3, status);
            if (newAdn.getNumber() == null) {
                newAdn.setNumber(UsimPBMemInfo.STRING_NOT_SET);
            }
            index = this.mAdnCache.updateAdnBySearch(efid, oldAdn, newAdn, null, response);
            waitForResult(status);
        }
        if (this.mErrorCause == 1) {
            logi("updateUsimPBRecordsBySearchWithError success index is " + index);
            return index;
        }
        return this.mErrorCause;
    }

    public boolean updateAdnRecordsInEfByIndex(int efid, String newTag, String newPhoneNumber, int index, String pin2) {
        int result = updateAdnRecordsInEfByIndexWithError(efid, newTag, newPhoneNumber, index, pin2);
        if (result == 1) {
            return true;
        }
        return ALLOW_SIM_OP_IN_UI_THREAD;
    }

    public synchronized int updateAdnRecordsInEfByIndexWithError(int efid, String newTag, String newPhoneNumber, int index, String pin2) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            loge("updateAdnRecordsInEfByIndex mAdnCache is null");
            return 0;
        }
        if (DBG) {
            logd("updateAdnRecordsInEfByIndex: efid=0x" + Integer.toHexString(efid).toUpperCase() + " Index=" + index + " ==> (" + newTag + "," + newPhoneNumber + ") pin2=" + pin2);
        }
        synchronized (this.mLock) {
            checkThread();
            this.mSuccess = ALLOW_SIM_OP_IN_UI_THREAD;
            AtomicBoolean status = new AtomicBoolean(ALLOW_SIM_OP_IN_UI_THREAD);
            Message response = this.mBaseHandler.obtainMessage(3, status);
            if (newPhoneNumber == null) {
                newPhoneNumber = UsimPBMemInfo.STRING_NOT_SET;
            }
            AdnRecord newAdn = new AdnRecord(newTag, newPhoneNumber);
            if (this.mAdnCache != null) {
                this.mAdnCache.updateAdnByIndex(efid, newAdn, index, pin2, response);
                waitForResult(status);
            } else {
                loge("Failure while trying to update by index due to uninitialised adncache");
            }
        }
        return this.mErrorCause;
    }

    public synchronized int updateUsimPBRecordsInEfByIndexWithError(int efid, String newTag, String newPhoneNumber, String newAnr, String newGrpIds, String[] newEmails, int index) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            loge("updateUsimPBRecordsInEfByIndexWithError mAdnCache is null");
            return 0;
        }
        if (DBG) {
            logd("updateUsimPBRecordsInEfByIndexWithError: efid=" + efid + " Index=" + index + " ==> (" + newTag + "," + newPhoneNumber + ") newAnr= " + newAnr + " newGrpIds = " + newGrpIds + " newEmails = " + newEmails);
        }
        synchronized (this.mLock) {
            checkThread();
            this.mSuccess = ALLOW_SIM_OP_IN_UI_THREAD;
            AtomicBoolean status = new AtomicBoolean(ALLOW_SIM_OP_IN_UI_THREAD);
            Message response = this.mBaseHandler.obtainMessage(3, status);
            if (newPhoneNumber == null) {
                newPhoneNumber = UsimPBMemInfo.STRING_NOT_SET;
            }
            AdnRecord newAdn = new AdnRecord(efid, index, newTag, newPhoneNumber, newAnr, newEmails, newGrpIds);
            this.mAdnCache.updateAdnByIndex(efid, newAdn, index, null, response);
            waitForResult(status);
        }
        return this.mErrorCause;
    }

    public synchronized int updateUsimPBRecordsByIndexWithError(int efid, AdnRecord record, int index) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            loge("updateUsimPBRecordsByIndexWithError mAdnCache is null");
            return 0;
        }
        if (DBG) {
            logd("updateUsimPBRecordsByIndexWithError: efid=" + efid + " Index=" + index + " ==> " + record);
        }
        synchronized (this.mLock) {
            checkThread();
            this.mSuccess = ALLOW_SIM_OP_IN_UI_THREAD;
            AtomicBoolean status = new AtomicBoolean(ALLOW_SIM_OP_IN_UI_THREAD);
            Message response = this.mBaseHandler.obtainMessage(3, status);
            this.mAdnCache.updateAdnByIndex(efid, record, index, null, response);
            waitForResult(status);
        }
        return this.mErrorCause;
    }

    private String getAdnEFPath(int efid) {
        if (efid == 28474) {
            return "3F007F10";
        }
        return null;
    }

    public int[] getAdnRecordsSize(int efid) {
        if (DBG) {
            logd("getAdnRecordsSize: efid=" + efid);
        }
        synchronized (this.mLock) {
            checkThread();
            this.mRecordSize = new int[3];
            AtomicBoolean status = new AtomicBoolean(ALLOW_SIM_OP_IN_UI_THREAD);
            Message response = this.mBaseHandler.obtainMessage(1, status);
            IccFileHandler fh = this.mPhone.getIccFileHandler();
            if (fh != null) {
                if (getAdnEFPath(efid) != null) {
                    fh.getEFLinearRecordSize(efid, getAdnEFPath(efid), response);
                } else {
                    fh.getEFLinearRecordSize(efid, response);
                }
                waitForResult(status);
            }
        }
        return this.mRecordSize;
    }

    public synchronized List<AdnRecord> getAdnRecordsInEf(int efid) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        int efid2 = updateEfForIccType(efid);
        if (DBG) {
            logd("getAdnRecordsInEF: efid=0x" + Integer.toHexString(efid2).toUpperCase());
        }
        if (this.mAdnCache == null) {
            loge("getAdnRecordsInEF mAdnCache is null");
            return null;
        }
        synchronized (this.mLock) {
            checkThread();
            AtomicBoolean status = new AtomicBoolean(ALLOW_SIM_OP_IN_UI_THREAD);
            Message response = this.mBaseHandler.obtainMessage(2, status);
            if (this.mAdnCache != null) {
                this.mAdnCache.requestLoadAllAdnLike(efid2, this.mAdnCache.extensionEfForEf(efid2), response);
                waitForResult(status);
            } else {
                loge("Failure while trying to load from SIM due to uninitialised adncache");
            }
        }
        return this.mRecords;
    }

    protected void checkThread() {
        if (!this.mBaseHandler.getLooper().equals(Looper.myLooper())) {
            return;
        }
        loge("query() called on the main UI thread!");
        throw new IllegalStateException("You cannot call query on this provder from the main UI thread.");
    }

    protected void waitForResult(AtomicBoolean status) {
        while (!status.get()) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                logd("interrupted while trying to update by search");
            }
        }
    }

    private int updateEfForIccType(int efid) {
        if (efid == 28474 && this.mPhone.getCurrentUiccAppType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
            return IccConstants.EF_PBR;
        }
        return efid;
    }

    private int getErrorCauseFromException(CommandException e) {
        if (e == null) {
            return 1;
        }
        switch (m24x9944abe()[e.getCommandError().ordinal()]) {
            case 1:
                return -14;
            case 2:
                return -6;
            case 3:
                return -11;
            case 4:
                return -1;
            case 5:
                return -13;
            case 6:
                return -12;
            case 7:
                return -10;
            case 8:
                return -4;
            case 9:
            case 11:
                return -5;
            case 10:
                return -3;
            case 12:
                return -17;
            case 13:
                return -16;
            case 14:
                return -2;
            default:
                return 0;
        }
    }

    public void onPhbReady() {
        if (this.mAdnCache == null) {
            return;
        }
        this.mAdnCache.requestLoadAllAdnLike(28474, this.mAdnCache.extensionEfForEf(28474), null);
    }

    public boolean isPhbReady() {
        String strPhbReady = "false";
        String strAllSimState = UsimPBMemInfo.STRING_NOT_SET;
        String strCurSimState = UsimPBMemInfo.STRING_NOT_SET;
        boolean isSimLocked = ALLOW_SIM_OP_IN_UI_THREAD;
        int subId = this.mPhone.getSubId();
        int phoneId = this.mPhone.getPhoneId();
        int slotId = SubscriptionManager.getSlotId(subId);
        if (SubscriptionManager.isValidSlotId(slotId)) {
            strAllSimState = SystemProperties.get("gsm.sim.state");
            if (strAllSimState != null && strAllSimState.length() > 0) {
                String[] values = strAllSimState.split(",");
                if (phoneId >= 0 && phoneId < values.length && values[phoneId] != null) {
                    strCurSimState = values[phoneId];
                }
            }
            isSimLocked = !strCurSimState.equals("NETWORK_LOCKED") ? strCurSimState.equals("PIN_REQUIRED") : true;
            strPhbReady = this.mPhone.getPhoneType() == 1 ? slotId == 0 ? SystemProperties.get("gsm.sim.ril.phbready", "false") : SystemProperties.get("gsm.sim.ril.phbready." + (slotId + 1), "false") : slotId == 0 ? SystemProperties.get("cdma.sim.ril.phbready", "false") : SystemProperties.get("cdma.sim.ril.phbready." + (slotId + 1), "false");
        }
        logi("[isPhbReady] subId:" + subId + ", slotId: " + slotId + ", isPhbReady: " + strPhbReady + ",strSimState: " + strAllSimState + ", phoneType: " + this.mPhone.getPhoneType());
        if (!strPhbReady.equals("true") || isSimLocked) {
            return ALLOW_SIM_OP_IN_UI_THREAD;
        }
        return true;
    }

    public List<UsimGroup> getUsimGroups() {
        if (this.mAdnCache == null) {
            return null;
        }
        return this.mAdnCache.getUsimGroups();
    }

    public String getUsimGroupById(int nGasId) {
        if (this.mAdnCache == null) {
            return null;
        }
        return this.mAdnCache.getUsimGroupById(nGasId);
    }

    public boolean removeUsimGroupById(int nGasId) {
        return this.mAdnCache == null ? ALLOW_SIM_OP_IN_UI_THREAD : this.mAdnCache.removeUsimGroupById(nGasId);
    }

    public int insertUsimGroup(String grpName) {
        if (this.mAdnCache == null) {
            return -1;
        }
        return this.mAdnCache.insertUsimGroup(grpName);
    }

    public int updateUsimGroup(int nGasId, String grpName) {
        if (this.mAdnCache == null) {
            return -1;
        }
        return this.mAdnCache.updateUsimGroup(nGasId, grpName);
    }

    public boolean addContactToGroup(int adnIndex, int grpIndex) {
        return this.mAdnCache == null ? ALLOW_SIM_OP_IN_UI_THREAD : this.mAdnCache.addContactToGroup(adnIndex, grpIndex);
    }

    public boolean removeContactFromGroup(int adnIndex, int grpIndex) {
        return this.mAdnCache == null ? ALLOW_SIM_OP_IN_UI_THREAD : this.mAdnCache.removeContactFromGroup(adnIndex, grpIndex);
    }

    public boolean updateContactToGroups(int adnIndex, int[] grpIdList) {
        return this.mAdnCache == null ? ALLOW_SIM_OP_IN_UI_THREAD : this.mAdnCache.updateContactToGroups(adnIndex, grpIdList);
    }

    public boolean moveContactFromGroupsToGroups(int adnIndex, int[] fromGrpIdList, int[] toGrpIdList) {
        return this.mAdnCache == null ? ALLOW_SIM_OP_IN_UI_THREAD : this.mAdnCache.moveContactFromGroupsToGroups(adnIndex, fromGrpIdList, toGrpIdList);
    }

    public int hasExistGroup(String grpName) {
        if (this.mAdnCache == null) {
            return -1;
        }
        return this.mAdnCache.hasExistGroup(grpName);
    }

    public int getUsimGrpMaxNameLen() {
        if (this.mAdnCache == null) {
            return -1;
        }
        return this.mAdnCache.getUsimGrpMaxNameLen();
    }

    public int getUsimGrpMaxCount() {
        if (this.mAdnCache == null) {
            return -1;
        }
        return this.mAdnCache.getUsimGrpMaxCount();
    }

    public List<AlphaTag> getUsimAasList() {
        if (this.mAdnCache == null) {
            return null;
        }
        return this.mAdnCache.getUsimAasList();
    }

    public String getUsimAasById(int index) {
        if (this.mAdnCache == null) {
            return null;
        }
        return this.mAdnCache.getUsimAasById(index);
    }

    public boolean removeUsimAasById(int index, int pbrIndex) {
        return this.mAdnCache == null ? ALLOW_SIM_OP_IN_UI_THREAD : this.mAdnCache.removeUsimAasById(index, pbrIndex);
    }

    public int insertUsimAas(String aasName) {
        if (this.mAdnCache == null) {
            return -1;
        }
        return this.mAdnCache.insertUsimAas(aasName);
    }

    public boolean updateUsimAas(int index, int pbrIndex, String aasName) {
        return this.mAdnCache == null ? ALLOW_SIM_OP_IN_UI_THREAD : this.mAdnCache.updateUsimAas(index, pbrIndex, aasName);
    }

    public boolean updateAdnAas(int adnIndex, int aasIndex) {
        return this.mAdnCache == null ? ALLOW_SIM_OP_IN_UI_THREAD : this.mAdnCache.updateAdnAas(adnIndex, aasIndex);
    }

    public int getAnrCount() {
        if (this.mAdnCache == null) {
            return 0;
        }
        return this.mAdnCache.getAnrCount();
    }

    public int getEmailCount() {
        if (this.mAdnCache == null) {
            return 0;
        }
        return this.mAdnCache.getEmailCount();
    }

    public int getUsimAasMaxCount() {
        if (this.mAdnCache == null) {
            return -1;
        }
        return this.mAdnCache.getUsimAasMaxCount();
    }

    public int getUsimAasMaxNameLen() {
        if (this.mAdnCache == null) {
            return -1;
        }
        return this.mAdnCache.getUsimAasMaxNameLen();
    }

    public boolean hasSne() {
        return this.mAdnCache == null ? ALLOW_SIM_OP_IN_UI_THREAD : this.mAdnCache.hasSne();
    }

    public int getSneRecordLen() {
        if (this.mAdnCache == null) {
            return -1;
        }
        return this.mAdnCache.getSneRecordLen();
    }

    public boolean isAdnAccessible() {
        return this.mAdnCache == null ? ALLOW_SIM_OP_IN_UI_THREAD : this.mAdnCache.isAdnAccessible();
    }

    public synchronized UsimPBMemInfo[] getPhonebookMemStorageExt() {
        UsimPBMemInfo[] phonebookMemStorageExt;
        synchronized (this) {
            phonebookMemStorageExt = this.mAdnCache != null ? this.mAdnCache.getPhonebookMemStorageExt() : null;
        }
        return phonebookMemStorageExt;
    }
}
