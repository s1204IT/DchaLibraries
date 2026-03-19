package com.mediatek.internal.telephony.phb;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.uicc.MtkRuimRecords;
import com.mediatek.internal.telephony.uicc.MtkSIMRecords;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MtkIccPhoneBookInterfaceManager extends IccPhoneBookInterfaceManager {
    protected static final boolean DBG = !SystemProperties.get("ro.build.type").equals(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER);
    static final String LOG_TAG = "MtkIccPhoneBookIM";
    private int mErrorCause;
    private IccRecords mIccRecords;
    protected Handler mMtkBaseHandler;
    private List<MtkAdnRecord> mRecords;
    private int mSlotId;

    public MtkIccPhoneBookInterfaceManager(Phone phone) {
        super(phone);
        this.mSlotId = -1;
        this.mMtkBaseHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        MtkIccPhoneBookInterfaceManager.this.mBaseHandler.handleMessage(message);
                        return;
                    case 2:
                        AsyncResult asyncResult = (AsyncResult) message.obj;
                        synchronized (MtkIccPhoneBookInterfaceManager.this.mLock) {
                            if (asyncResult.exception == null) {
                                MtkIccPhoneBookInterfaceManager.this.mRecords = (List) asyncResult.result;
                            } else {
                                if (MtkIccPhoneBookInterfaceManager.DBG) {
                                    MtkIccPhoneBookInterfaceManager.this.logd("Cannot load ADN records");
                                }
                                MtkIccPhoneBookInterfaceManager.this.mRecords = null;
                            }
                            notifyPending(asyncResult);
                            break;
                        }
                        return;
                    case 3:
                        MtkIccPhoneBookInterfaceManager.this.logd("EVENT_UPDATE_DONE");
                        AsyncResult asyncResult2 = (AsyncResult) message.obj;
                        synchronized (MtkIccPhoneBookInterfaceManager.this.mLock) {
                            MtkIccPhoneBookInterfaceManager.this.mSuccess = asyncResult2.exception == null;
                            MtkIccPhoneBookInterfaceManager.this.logd("EVENT_UPDATE_DONE mSuccess:" + MtkIccPhoneBookInterfaceManager.this.mSuccess);
                            if (MtkIccPhoneBookInterfaceManager.this.mSuccess) {
                                MtkIccPhoneBookInterfaceManager.this.mErrorCause = 1;
                            } else if (asyncResult2.exception instanceof CommandException) {
                                MtkIccPhoneBookInterfaceManager.this.mErrorCause = MtkIccPhoneBookInterfaceManager.this.getErrorCauseFromException(asyncResult2.exception);
                            } else {
                                MtkIccPhoneBookInterfaceManager.this.loge("Error : Unknow exception instance");
                                MtkIccPhoneBookInterfaceManager.this.mErrorCause = -10;
                            }
                            MtkIccPhoneBookInterfaceManager.this.logi("update done result: " + MtkIccPhoneBookInterfaceManager.this.mErrorCause);
                            notifyPending(asyncResult2);
                            break;
                        }
                        return;
                    default:
                        return;
                }
            }

            private void notifyPending(AsyncResult asyncResult) {
                if (asyncResult.userObj == null) {
                    return;
                }
                try {
                    ((AtomicBoolean) asyncResult.userObj).set(true);
                    MtkIccPhoneBookInterfaceManager.this.mLock.notifyAll();
                } catch (ClassCastException e) {
                    MtkIccPhoneBookInterfaceManager.this.loge("notifyPending " + e.getMessage());
                }
            }
        };
    }

    public void updateIccRecords(IccRecords iccRecords) {
        this.mIccRecords = iccRecords;
        if (iccRecords != null) {
            this.mAdnCache = iccRecords.getAdnCache();
            if (this.mAdnCache != null && (this.mAdnCache instanceof MtkAdnRecordCache)) {
                this.mSlotId = ((MtkAdnRecordCache) this.mAdnCache).getSlotId();
            } else {
                this.mSlotId = -1;
            }
            logi("[updateIccRecords] Set mAdnCache value");
            return;
        }
        this.mAdnCache = null;
        logi("[updateIccRecords] Set mAdnCache value to null");
        this.mSlotId = -1;
    }

    protected void logd(String str) {
        Rlog.d(LOG_TAG, "[IccPbInterfaceManager] " + str + "(slot " + this.mSlotId + ")");
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, "[IccPbInterfaceManager] " + str + "(slot " + this.mSlotId + ")");
    }

    protected void logi(String str) {
        Rlog.i(LOG_TAG, "[IccPbInterfaceManager] " + str + "(slot " + this.mSlotId + ")");
    }

    public boolean updateAdnRecordsInEfBySearch(int i, String str, String str2, String str3, String str4, String str5) {
        return updateAdnRecordsInEfBySearchWithError(i, str, str2, str3, str4, str5) == 1;
    }

    public synchronized int updateAdnRecordsInEfBySearchWithError(int i, String str, String str2, String str3, String str4, String str5) {
        int iUpdateAdnBySearch = -1;
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            loge("updateAdnRecordsInEfBySearchWithError mAdnCache is null");
            return 0;
        }
        if (DBG) {
            logd("updateAdnRecordsInEfBySearch: efid=0x" + Integer.toHexString(i).toUpperCase() + " (" + str + "," + str2 + ")==> (" + str3 + "," + str4 + ") pin2=" + str5);
        }
        int iUpdateEfForIccType = updateEfForIccType(i);
        synchronized (this.mLock) {
            checkThread();
            this.mSuccess = false;
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            Message messageObtainMessage = this.mMtkBaseHandler.obtainMessage(3, atomicBoolean);
            MtkAdnRecord mtkAdnRecord = new MtkAdnRecord(str, str2);
            if (str4 == null) {
                str4 = "";
            }
            MtkAdnRecord mtkAdnRecord2 = new MtkAdnRecord(str3, str4);
            if (this.mAdnCache != null) {
                iUpdateAdnBySearch = ((MtkAdnRecordCache) this.mAdnCache).updateAdnBySearch(iUpdateEfForIccType, mtkAdnRecord, mtkAdnRecord2, str5, messageObtainMessage, null);
                waitForResult(atomicBoolean);
            } else {
                loge("Failure while trying to update by search due to uninitialised adncache");
            }
        }
        if (this.mErrorCause == 1) {
            logi("updateAdnRecordsInEfBySearchWithError success index is " + iUpdateAdnBySearch);
            return iUpdateAdnBySearch;
        }
        return this.mErrorCause;
    }

    public synchronized int updateUsimPBRecordsInEfBySearchWithError(int i, String str, String str2, String str3, String str4, String[] strArr, String str5, String str6, String str7, String str8, String[] strArr2) {
        int i2;
        String str9;
        String str10;
        String str11;
        Object obj;
        String str12;
        int iUpdateAdnBySearch;
        String str13 = str2;
        synchronized (this) {
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
                throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
            }
            if (this.mAdnCache == null) {
                loge("updateUsimPBRecordsInEfBySearchWithError mAdnCache is null");
                return 0;
            }
            if (DBG) {
                StringBuilder sb = new StringBuilder();
                sb.append("updateUsimPBRecordsInEfBySearchWithError: efid=");
                i2 = i;
                sb.append(i2);
                sb.append(" (");
                sb.append(str);
                sb.append(",");
                sb.append(str13);
                sb.append("oldAnr");
                sb.append(str3);
                sb.append(" oldGrpIds ");
                sb.append(str4);
                sb.append(")==>(");
                str9 = str5;
                sb.append(str9);
                sb.append(",");
                sb.append(str6);
                sb.append(") newAnr= ");
                str10 = str7;
                sb.append(str10);
                sb.append(" newGrpIds = ");
                str11 = str8;
                sb.append(str11);
                sb.append(" newEmails = ");
                sb.append(strArr2 == null ? "null" : strArr2[0]);
                logd(sb.toString());
            } else {
                i2 = i;
                str9 = str5;
                str10 = str7;
                str11 = str8;
            }
            Object obj2 = this.mLock;
            try {
                synchronized (obj2) {
                    try {
                        checkThread();
                        this.mSuccess = false;
                        Message messageObtainMessage = this.mMtkBaseHandler.obtainMessage(3, atomicBoolean);
                        MtkAdnRecord mtkAdnRecord = new MtkAdnRecord(str, str13);
                        if (str6 == null) {
                            str12 = "";
                        } else {
                            str12 = str6;
                        }
                        iUpdateAdnBySearch = ((MtkAdnRecordCache) this.mAdnCache).updateAdnBySearch(i2, mtkAdnRecord, new MtkAdnRecord(0, 0, str9, str12, str10, strArr2, str11), null, messageObtainMessage, null);
                        waitForResult(atomicBoolean);
                    } catch (Throwable th) {
                        th = th;
                        obj = obj2;
                        throw th;
                    }
                }
                if (this.mErrorCause == 1) {
                    logi("updateUsimPBRecordsInEfBySearchWithError success index is " + iUpdateAdnBySearch);
                    return iUpdateAdnBySearch;
                }
                return this.mErrorCause;
            } catch (Throwable th2) {
                th = th2;
                obj = str13;
            }
        }
    }

    public synchronized int updateUsimPBRecordsBySearchWithError(int i, MtkAdnRecord mtkAdnRecord, MtkAdnRecord mtkAdnRecord2) {
        int iUpdateAdnBySearch;
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            loge("updateUsimPBRecordsBySearchWithError mAdnCache is null");
            return 0;
        }
        if (DBG) {
            logd("updateUsimPBRecordsBySearchWithError: efid=" + i + " (" + mtkAdnRecord + ")==>(" + mtkAdnRecord2 + ")");
        }
        synchronized (this.mLock) {
            checkThread();
            this.mSuccess = false;
            Message messageObtainMessage = this.mMtkBaseHandler.obtainMessage(3, atomicBoolean);
            if (mtkAdnRecord2.getNumber() == null) {
                mtkAdnRecord2.setNumber("");
            }
            iUpdateAdnBySearch = ((MtkAdnRecordCache) this.mAdnCache).updateAdnBySearch(i, mtkAdnRecord, mtkAdnRecord2, null, messageObtainMessage, null);
            waitForResult(atomicBoolean);
        }
        if (this.mErrorCause == 1) {
            logi("updateUsimPBRecordsBySearchWithError success index is " + iUpdateAdnBySearch);
            return iUpdateAdnBySearch;
        }
        return this.mErrorCause;
    }

    public boolean updateAdnRecordsInEfByIndex(int i, String str, String str2, int i2, String str3) {
        return updateAdnRecordsInEfByIndexWithError(i, str, str2, i2, str3) == 1;
    }

    public synchronized int updateAdnRecordsInEfByIndexWithError(int i, String str, String str2, int i2, String str3) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            loge("updateAdnRecordsInEfByIndex mAdnCache is null");
            return 0;
        }
        if (DBG) {
            logd("updateAdnRecordsInEfByIndex: efid=0x" + Integer.toHexString(i).toUpperCase() + " Index=" + i2 + " ==> (" + str + "," + str2 + ") pin2=" + str3);
        }
        synchronized (this.mLock) {
            checkThread();
            this.mSuccess = false;
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            Message messageObtainMessage = this.mMtkBaseHandler.obtainMessage(3, atomicBoolean);
            if (str2 == null) {
                str2 = "";
            }
            MtkAdnRecord mtkAdnRecord = new MtkAdnRecord(str, str2);
            if (this.mAdnCache != null) {
                ((MtkAdnRecordCache) this.mAdnCache).updateAdnByIndex(i, mtkAdnRecord, i2, str3, messageObtainMessage);
                waitForResult(atomicBoolean);
            } else {
                loge("Failure while trying to update by index due to uninitialised adncache");
            }
        }
        return this.mErrorCause;
    }

    public synchronized int updateUsimPBRecordsInEfByIndexWithError(int i, String str, String str2, String str3, String str4, String[] strArr, int i2) {
        int i3;
        String str5;
        String str6;
        String str7;
        int i4;
        String str8 = str2;
        synchronized (this) {
            if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
                throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
            }
            if (this.mAdnCache == null) {
                loge("updateUsimPBRecordsInEfByIndexWithError mAdnCache is null");
                return 0;
            }
            if (DBG) {
                StringBuilder sb = new StringBuilder();
                sb.append("updateUsimPBRecordsInEfByIndexWithError: efid=");
                i3 = i;
                sb.append(i3);
                sb.append(" Index=");
                i4 = i2;
                sb.append(i4);
                sb.append(" ==> (");
                str5 = str;
                sb.append(str5);
                sb.append(",");
                sb.append(str8);
                sb.append(") newAnr= ");
                str6 = str3;
                sb.append(str6);
                sb.append(" newGrpIds = ");
                str7 = str4;
                sb.append(str7);
                sb.append(" newEmails = ");
                sb.append(strArr == null ? "null" : strArr[0]);
                logd(sb.toString());
            } else {
                i3 = i;
                str5 = str;
                str6 = str3;
                str7 = str4;
                i4 = i2;
            }
            synchronized (this.mLock) {
                checkThread();
                this.mSuccess = false;
                AtomicBoolean atomicBoolean = new AtomicBoolean(false);
                Message messageObtainMessage = this.mMtkBaseHandler.obtainMessage(3, atomicBoolean);
                if (str8 == null) {
                    str8 = "";
                }
                ((MtkAdnRecordCache) this.mAdnCache).updateAdnByIndex(i3, new MtkAdnRecord(i3, i4, str5, str8, str6, strArr, str7), i4, null, messageObtainMessage);
                waitForResult(atomicBoolean);
            }
            return this.mErrorCause;
        }
    }

    public synchronized int updateUsimPBRecordsByIndexWithError(int i, MtkAdnRecord mtkAdnRecord, int i2) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            loge("updateUsimPBRecordsByIndexWithError mAdnCache is null");
            return 0;
        }
        if (DBG) {
            logd("updateUsimPBRecordsByIndexWithError: efid=" + i + " Index=" + i2 + " ==> " + mtkAdnRecord);
        }
        synchronized (this.mLock) {
            checkThread();
            this.mSuccess = false;
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            ((MtkAdnRecordCache) this.mAdnCache).updateAdnByIndex(i, mtkAdnRecord, i2, null, this.mMtkBaseHandler.obtainMessage(3, atomicBoolean));
            waitForResult(atomicBoolean);
        }
        return this.mErrorCause;
    }

    private String getAdnEFPath(int i) {
        if (i == 28474) {
            return "3F007F10";
        }
        return null;
    }

    public int[] getAdnRecordsSize(int i) {
        if (DBG) {
            logd("getAdnRecordsSize: efid=" + i);
        }
        synchronized (this.mLock) {
            checkThread();
            this.mRecordSize = new int[3];
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            Message messageObtainMessage = this.mMtkBaseHandler.obtainMessage(1, atomicBoolean);
            IccFileHandler iccFileHandler = this.mPhone.getIccFileHandler();
            if (iccFileHandler != null) {
                if (getAdnEFPath(i) != null) {
                    iccFileHandler.getEFLinearRecordSize(i, getAdnEFPath(i), messageObtainMessage);
                } else {
                    iccFileHandler.getEFLinearRecordSize(i, messageObtainMessage);
                }
                waitForResult(atomicBoolean);
            }
        }
        return this.mRecordSize;
    }

    public synchronized List<MtkAdnRecord> getAdnRecordsInEf(int i, Object obj) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        int iUpdateEfForIccType = updateEfForIccType(i);
        if (DBG) {
            logd("getAdnRecordsInEF: efid=0x" + Integer.toHexString(iUpdateEfForIccType).toUpperCase());
        }
        if (this.mAdnCache == null) {
            loge("getAdnRecordsInEF mAdnCache is null");
            return null;
        }
        synchronized (this.mLock) {
            checkThread();
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            Message messageObtainMessage = this.mMtkBaseHandler.obtainMessage(2, atomicBoolean);
            if (this.mAdnCache != null) {
                this.mAdnCache.requestLoadAllAdnLike(iUpdateEfForIccType, this.mAdnCache.extensionEfForEf(iUpdateEfForIccType), messageObtainMessage);
                waitForResult(atomicBoolean);
            } else {
                loge("Failure while trying to load from SIM due to uninitialised adncache");
            }
        }
        return this.mRecords;
    }

    protected void checkThread() {
        if (this.mMtkBaseHandler.getLooper().equals(Looper.myLooper())) {
            loge("query() called on the main UI thread!");
            throw new IllegalStateException("You cannot call query on this provder from the main UI thread.");
        }
    }

    private int getErrorCauseFromException(CommandException commandException) {
        if (commandException == null) {
            return 1;
        }
        switch (AnonymousClass2.$SwitchMap$com$android$internal$telephony$CommandException$Error[commandException.getCommandError().ordinal()]) {
            case 1:
                return -10;
            case 2:
                return -1;
            case 3:
            case 4:
                return -5;
            case 5:
                return -2;
            case 6:
                return -3;
            case 7:
                return -4;
            case 8:
                return -6;
            case 9:
                return -14;
            case 10:
            case 11:
                return -11;
            case 12:
                return -12;
            case 13:
                return -13;
            case 14:
                return -16;
            case 15:
                return -17;
            default:
                return 0;
        }
    }

    static class AnonymousClass2 {
        static final int[] $SwitchMap$com$android$internal$telephony$CommandException$Error = new int[CommandException.Error.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.GENERIC_FAILURE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.OEM_ERROR_1.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.SIM_PUK2.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.PASSWORD_INCORRECT.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.OEM_ERROR_2.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.OEM_ERROR_3.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.OEM_ERROR_4.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.OEM_ERROR_5.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.OEM_ERROR_6.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.RADIO_NOT_AVAILABLE.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.OEM_ERROR_7.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.OEM_ERROR_8.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.OEM_ERROR_9.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.OEM_ERROR_10.ordinal()] = 14;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandException$Error[CommandException.Error.OEM_ERROR_11.ordinal()] = 15;
            } catch (NoSuchFieldError e15) {
            }
        }
    }

    public void onPhbReady() {
        if (this.mAdnCache != null) {
            this.mAdnCache.requestLoadAllAdnLike(28474, this.mAdnCache.extensionEfForEf(28474), (Message) null);
        }
    }

    public boolean isPhbReady() {
        if (this.mAdnCache == null || !SubscriptionManager.isValidSlotIndex(this.mSlotId) || this.mIccRecords == null) {
            return false;
        }
        if (this.mIccRecords instanceof MtkSIMRecords) {
            return this.mIccRecords.isPhbReady();
        }
        if (this.mIccRecords instanceof MtkRuimRecords) {
            return this.mIccRecords.isPhbReady();
        }
        return false;
    }

    public List<UsimGroup> getUsimGroups() {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return null;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).getUsimGroups();
    }

    public String getUsimGroupById(int i) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return null;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).getUsimGroupById(i);
    }

    public boolean removeUsimGroupById(int i) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return false;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).removeUsimGroupById(i);
    }

    public int insertUsimGroup(String str) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return -1;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).insertUsimGroup(str);
    }

    public int updateUsimGroup(int i, String str) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return -1;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).updateUsimGroup(i, str);
    }

    public boolean addContactToGroup(int i, int i2) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return false;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).addContactToGroup(i, i2);
    }

    public boolean removeContactFromGroup(int i, int i2) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return false;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).removeContactFromGroup(i, i2);
    }

    public boolean updateContactToGroups(int i, int[] iArr) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return false;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).updateContactToGroups(i, iArr);
    }

    public boolean moveContactFromGroupsToGroups(int i, int[] iArr, int[] iArr2) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return false;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).moveContactFromGroupsToGroups(i, iArr, iArr2);
    }

    public int hasExistGroup(String str) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return -1;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).hasExistGroup(str);
    }

    public int getUsimGrpMaxNameLen() {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return -1;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).getUsimGrpMaxNameLen();
    }

    public int getUsimGrpMaxCount() {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return -1;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).getUsimGrpMaxCount();
    }

    public List<AlphaTag> getUsimAasList() {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return null;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).getUsimAasList();
    }

    public String getUsimAasById(int i) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return null;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).getUsimAasById(i);
    }

    public boolean removeUsimAasById(int i, int i2) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return false;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).removeUsimAasById(i, i2);
    }

    public int insertUsimAas(String str) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return -1;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).insertUsimAas(str);
    }

    public boolean updateUsimAas(int i, int i2, String str) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return false;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).updateUsimAas(i, i2, str);
    }

    public boolean updateAdnAas(int i, int i2) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return false;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).updateAdnAas(i, i2);
    }

    public int getAnrCount() {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return 0;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).getAnrCount();
    }

    public int getEmailCount() {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return 0;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).getEmailCount();
    }

    public int getUsimAasMaxCount() {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return -1;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).getUsimAasMaxCount();
    }

    public int getUsimAasMaxNameLen() {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return -1;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).getUsimAasMaxNameLen();
    }

    public boolean hasSne() {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return false;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).hasSne();
    }

    public int getSneRecordLen() {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return -1;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).getSneRecordLen();
    }

    public boolean isAdnAccessible() {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return false;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).isAdnAccessible();
    }

    public synchronized UsimPBMemInfo[] getPhonebookMemStorageExt() {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        return this.mAdnCache == null ? null : ((MtkAdnRecordCache) this.mAdnCache).getPhonebookMemStorageExt();
    }

    public int getUpbDone() {
        if (this.mAdnCache == null) {
            return -1;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).getUpbDone();
    }

    public int[] getAdnRecordsCapacity() {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mAdnCache == null) {
            return null;
        }
        return ((MtkAdnRecordCache) this.mAdnCache).getAdnRecordsCapacity();
    }
}
