package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.AdnRecordCache;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class IccPhoneBookInterfaceManager {
    protected static final boolean ALLOW_SIM_OP_IN_UI_THREAD = false;
    protected static final boolean DBG = true;
    protected static final int EVENT_GET_EMAIL_SIZE_DONE = 7;
    protected static final int EVENT_GET_GROUP_DONE = 4;
    protected static final int EVENT_GET_SIZE_DONE = 1;
    protected static final int EVENT_LOAD_DONE = 2;
    protected static final int EVENT_UPDATE_DONE = 3;
    protected static final int EVENT_UPDATE_GROUP_DONE = 5;
    protected static final int EVENT_UPDATE_GROUP_TAG_DONE = 6;
    protected String[] groupString;
    protected Map<Integer, String> groups;
    protected AdnRecordCache mAdnCache;
    protected int[] mEmailRecordSize;
    protected PhoneBase mPhone;
    protected int[] mRecordSize;
    protected List<AdnRecord> mRecords;
    protected boolean mSuccess;
    private UiccCardApplication mCurrentApp = null;
    protected final Object mLock = new Object();
    protected final Object mLock2 = new Object();
    private boolean mIs3gCard = false;
    protected Handler mBaseHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    synchronized (IccPhoneBookInterfaceManager.this.mLock) {
                        if (ar.exception == null) {
                            IccPhoneBookInterfaceManager.this.mRecordSize = (int[]) ar.result;
                            IccPhoneBookInterfaceManager.this.logd("GET_RECORD_SIZE Size " + IccPhoneBookInterfaceManager.this.mRecordSize[0] + " total " + IccPhoneBookInterfaceManager.this.mRecordSize[1] + " #record " + IccPhoneBookInterfaceManager.this.mRecordSize[2]);
                        }
                        notifyPending(ar);
                        break;
                    }
                    return;
                case 2:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    synchronized (IccPhoneBookInterfaceManager.this.mLock) {
                        if (ar2.exception == null) {
                            IccPhoneBookInterfaceManager.this.mRecords = (List) ar2.result;
                        } else {
                            IccPhoneBookInterfaceManager.this.logd("Cannot load ADN records: " + ar2.exception);
                            if (IccPhoneBookInterfaceManager.this.mRecords != null) {
                                IccPhoneBookInterfaceManager.this.mRecords.clear();
                            }
                        }
                        notifyPending(ar2);
                        break;
                    }
                    return;
                case 3:
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    synchronized (IccPhoneBookInterfaceManager.this.mLock) {
                        if (ar3.exception == null) {
                            IccPhoneBookInterfaceManager.this.mSuccess = true;
                        } else {
                            IccPhoneBookInterfaceManager.this.logd("Cannot upadte ADN records: " + ar3.exception);
                        }
                        notifyPending(ar3);
                        break;
                    }
                    return;
                case 4:
                    AsyncResult ar4 = (AsyncResult) msg.obj;
                    synchronized (IccPhoneBookInterfaceManager.this.mLock) {
                        if (ar4.exception == null) {
                            IccPhoneBookInterfaceManager.this.groups = (Map) ar4.result;
                            int size = IccPhoneBookInterfaceManager.this.groups.size();
                            if (size > 0) {
                                IccPhoneBookInterfaceManager.this.groupString = new String[size];
                                for (int i = 0; i < size; i++) {
                                    IccPhoneBookInterfaceManager.this.groupString[i] = IccPhoneBookInterfaceManager.this.groups.get(Integer.valueOf(i + 1));
                                }
                            }
                        } else {
                            IccPhoneBookInterfaceManager.this.logd("Cannot load USIM groups");
                            if (IccPhoneBookInterfaceManager.this.groups != null) {
                                IccPhoneBookInterfaceManager.this.groups.clear();
                                IccPhoneBookInterfaceManager.this.groupString = null;
                            }
                        }
                        notifyPending(ar4);
                        break;
                    }
                    return;
                case 5:
                default:
                    return;
                case 6:
                    AsyncResult ar5 = (AsyncResult) msg.obj;
                    synchronized (IccPhoneBookInterfaceManager.this.mLock) {
                        IccPhoneBookInterfaceManager.this.mSuccess = ar5.exception == null;
                        notifyPending(ar5);
                        break;
                    }
                    return;
                case 7:
                    AsyncResult ar6 = (AsyncResult) msg.obj;
                    synchronized (IccPhoneBookInterfaceManager.this.mLock) {
                        if (ar6.exception == null) {
                            IccPhoneBookInterfaceManager.this.mEmailRecordSize = (int[]) ar6.result;
                            IccPhoneBookInterfaceManager.this.logd("GET_EMAIL_RECORD_SIZE Size " + IccPhoneBookInterfaceManager.this.mEmailRecordSize[0] + " total " + IccPhoneBookInterfaceManager.this.mEmailRecordSize[1] + " #record " + IccPhoneBookInterfaceManager.this.mEmailRecordSize[2]);
                        }
                        notifyPending(ar6);
                        break;
                    }
                    return;
            }
        }

        private void notifyPending(AsyncResult ar) {
            if (ar.userObj != null) {
                AtomicBoolean status = (AtomicBoolean) ar.userObj;
                status.set(true);
            }
            IccPhoneBookInterfaceManager.this.mLock.notifyAll();
        }
    };

    public abstract int[] getAdnRecordsSize(int i);

    public abstract int getTotalAdnRecordsSize();

    protected abstract void logd(String str);

    protected abstract void loge(String str);

    public IccPhoneBookInterfaceManager(PhoneBase phone) {
        this.mPhone = phone;
        IccRecords r = phone.mIccRecords.get();
        if (r != null) {
            this.mAdnCache = r.getAdnCache();
        }
    }

    public void dispose() {
    }

    public void updateIccRecords(IccRecords iccRecords) {
        if (iccRecords != null) {
            this.mAdnCache = iccRecords.getAdnCache();
        } else {
            this.mAdnCache = null;
        }
    }

    public boolean updateAdnRecordsInEfBySearch(int efid, String oldTag, String oldPhoneNumber, String newTag, String newPhoneNumber, String pin2) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        logd("updateAdnRecordsInEfBySearch: efid=" + efid + " (" + oldTag + "," + oldPhoneNumber + ")==> (" + newTag + "," + newPhoneNumber + ") pin2=" + pin2);
        int efid2 = updateEfForIccType(efid);
        synchronized (this.mLock2) {
            synchronized (this.mLock) {
                checkThread();
                this.mSuccess = false;
                AtomicBoolean status = new AtomicBoolean(false);
                Message response = this.mBaseHandler.obtainMessage(3, status);
                AdnRecord oldAdn = new AdnRecord(oldTag, oldPhoneNumber);
                AdnRecord newAdn = new AdnRecord(newTag, newPhoneNumber);
                if (this.mAdnCache != null) {
                    this.mAdnCache.updateAdnBySearch(efid2, oldAdn, newAdn, pin2, response);
                    waitForResult(status);
                } else {
                    loge("Failure while trying to update by search due to uninitialised adncache");
                }
            }
        }
        return this.mSuccess;
    }

    public boolean updateAdnRecordsInEfBySearch2(int efid, String oldTag, String oldPhoneNumber, String[] oldEmails, String oldPhoneNumber2, String oldSne, byte[] oldGrps, String newTag, String newPhoneNumber, String[] newEmails, String newPhoneNumber2, String newSne, byte[] newGrps, String pin2) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        logd("updateAdnRecordsInEfBySearch2: efid=" + efid + " (" + oldTag + "," + oldPhoneNumber + "," + oldEmails + "," + oldPhoneNumber2 + ")==> (" + newTag + "," + newPhoneNumber + "," + newEmails + "," + newPhoneNumber2 + ",) pin2=" + pin2);
        int efid2 = updateEfForIccType(efid);
        synchronized (this.mLock2) {
            synchronized (this.mLock) {
                checkThread();
                this.mSuccess = false;
                AtomicBoolean status = new AtomicBoolean(false);
                Message response = this.mBaseHandler.obtainMessage(3, status);
                AdnRecord oldAdn = new AdnRecord(oldTag, oldPhoneNumber, oldEmails, new String[]{oldPhoneNumber2}, new String[]{oldSne}, oldGrps);
                AdnRecord newAdn = new AdnRecord(newTag, newPhoneNumber, newEmails, new String[]{newPhoneNumber2}, new String[]{newSne}, newGrps);
                if (this.mAdnCache != null) {
                    this.mAdnCache.updateAdnBySearch2(efid2, oldAdn, newAdn, pin2, response);
                    waitForResult(status);
                } else {
                    loge("Failure while trying to update by search due to uninitialised adncache");
                }
            }
        }
        return this.mSuccess;
    }

    public boolean updateAdnRecordsGrpInEfBySearch(int efid, String name, String number, String email, String number2, byte[] grps, byte addgrp, String pin2) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        logd("updateAdnRecordsGrpInEfBySearch: efid=" + efid + " (" + name + "," + number + "," + email + "," + number2 + ")+ grp (+" + ((int) addgrp) + ")");
        synchronized (this.mLock2) {
            synchronized (this.mLock) {
                checkThread();
                this.mSuccess = false;
                AtomicBoolean status = new AtomicBoolean(false);
                Message response = this.mBaseHandler.obtainMessage(5, status);
                byte[] newGrps = new byte[10];
                AdnRecord oldAdn = new AdnRecord(name, number, new String[]{email}, new String[]{number2}, grps);
                if (this.mAdnCache != null) {
                    this.mAdnCache.updateAdnGrpBySearch(efid, oldAdn, pin2, newGrps, response);
                    waitForResult(status);
                } else {
                    loge("Failure while trying to update by search due to uninitialised adncache");
                }
            }
        }
        return this.mSuccess;
    }

    public boolean updateAdnRecordsGrpTagInEfByIndex(int efid, String grpId, String grpTag, String pin2) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        logd("updateAdnRecordsGrpTagInEfByIndex: efid=" + efid + " (" + grpId + "," + grpTag + ")");
        synchronized (this.mLock) {
            checkThread();
            this.mSuccess = false;
            AtomicBoolean status = new AtomicBoolean(false);
            Message response = this.mBaseHandler.obtainMessage(6, status);
            if (this.mAdnCache != null) {
                this.mAdnCache.updateAdnGrpTagByIndex(efid, grpId, grpTag, pin2, response);
                waitForResult(status);
            } else {
                loge("Failure while trying to update by search due to uninitialised adncache");
            }
        }
        return this.mSuccess;
    }

    public boolean updateAdnRecordsInEfByIndex(int efid, String newTag, String newPhoneNumber, int index, String pin2) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        logd("updateAdnRecordsInEfByIndex: efid=" + efid + " Index=" + index + " ==> (" + newTag + "," + newPhoneNumber + ") pin2=" + pin2);
        synchronized (this.mLock2) {
            synchronized (this.mLock) {
                checkThread();
                this.mSuccess = false;
                AtomicBoolean status = new AtomicBoolean(false);
                Message response = this.mBaseHandler.obtainMessage(3, status);
                AdnRecord newAdn = new AdnRecord(newTag, newPhoneNumber);
                if (this.mAdnCache != null) {
                    this.mAdnCache.updateAdnByIndex(efid, newAdn, index, pin2, response);
                    waitForResult(status);
                } else {
                    loge("Failure while trying to update by index due to uninitialised adncache");
                }
            }
        }
        return this.mSuccess;
    }

    public List<AdnRecord> getAdnRecordsInEf(int efid) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        int efid2 = updateEfForIccType(efid);
        logd("getAdnRecordsInEF: efid=" + efid2);
        synchronized (this.mLock2) {
            synchronized (this.mLock) {
                checkThread();
                AtomicBoolean status = new AtomicBoolean(false);
                Message response = this.mBaseHandler.obtainMessage(2, status);
                if (this.mAdnCache != null) {
                    this.mAdnCache.requestLoadAllAdnLike(efid2, this.mAdnCache.extensionEfForEf(efid2), response);
                    waitForResult(status);
                    if (this.mAdnCache != null) {
                        this.mAdnCache.setSimContactsLoaded(true, efid2);
                    }
                } else {
                    loge("Failure while trying to load from SIM due to uninitialised adncache");
                }
            }
        }
        return this.mRecords;
    }

    public String[] getGrpRecords(int efid) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        if (this.mPhone.getCurrentUiccAppType() != IccCardApplicationStatus.AppType.APPTYPE_USIM) {
            logd("Not USIM card, no EFgrp");
            return null;
        }
        logd("getGrpRecords: efid=" + efid);
        synchronized (this.mLock2) {
            synchronized (this.mLock) {
                checkThread();
                AtomicBoolean status = new AtomicBoolean(false);
                Message response = this.mBaseHandler.obtainMessage(4, status);
                this.mAdnCache.requestLoadUsimGroups(efid, response);
                waitForResult(status);
            }
        }
        return this.groupString;
    }

    protected void checkThread() {
        if (this.mBaseHandler.getLooper().equals(Looper.myLooper())) {
            loge("query() called on the main UI thread!");
            throw new IllegalStateException("You cannot call query on this provder from the main UI thread.");
        }
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

    protected int updateEfForIccType(int efid) {
        if (efid == 28474 && this.mPhone.getCurrentUiccAppType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
            return IccConstants.EF_PBR;
        }
        return efid;
    }

    public int getPBInitCount() {
        AdnRecordCache adnRecordCache = this.mAdnCache;
        int i = adnRecordCache.PBInitCount;
        adnRecordCache.PBInitCount = i + 1;
        return i;
    }

    public int getEmailFieldSize() {
        return this.mAdnCache.getEmailFieldSize();
    }

    public boolean isSneFieldEnable() {
        return this.mAdnCache.isSneFieldEnable();
    }

    public boolean isSimContactsLoaded() {
        if (this.mAdnCache == null) {
            return false;
        }
        return this.mAdnCache.isSimContactsLoaded();
    }

    public int[] getContactItemMaxLength() throws Throwable {
        AtomicBoolean status;
        int[] maxLength = new int[4];
        for (int i = 0; i < maxLength.length; i++) {
            maxLength[i] = 0;
        }
        int efid = updateEfForIccType(28474);
        int adnEfid = efid;
        int emailEfid = -1;
        if (efid == 20272) {
            adnEfid = this.mAdnCache.getUsimOneAdnEfid();
            emailEfid = this.mAdnCache.getUsimOneEmailEfid();
            if (adnEfid != -1 && emailEfid != -1) {
                synchronized (this.mLock) {
                    checkThread();
                    this.mRecordSize = new int[3];
                    for (int i2 = 0; i2 < this.mRecordSize.length; i2++) {
                        this.mRecordSize[i2] = 0;
                    }
                    AtomicBoolean status2 = new AtomicBoolean(false);
                    Message response = this.mBaseHandler.obtainMessage(1, status2);
                    IccFileHandler fh = this.mPhone.getIccFileHandler();
                    if (fh != null) {
                        fh.getEFLinearRecordSize(adnEfid, response);
                        waitForResult(status2);
                    }
                }
                if (this.mRecordSize[0] != 0) {
                    if (efid == 28474) {
                        maxLength[0] = this.mRecordSize[0] - 14;
                        maxLength[1] = 20;
                        maxLength[2] = 0;
                        maxLength[3] = 0;
                    } else {
                        synchronized (this.mLock) {
                            try {
                                checkThread();
                                this.mEmailRecordSize = new int[3];
                                for (int i3 = 0; i3 < this.mEmailRecordSize.length; i3++) {
                                    this.mEmailRecordSize[i3] = 0;
                                }
                                status = new AtomicBoolean(false);
                            } catch (Throwable th) {
                                th = th;
                            }
                            try {
                                Message response2 = this.mBaseHandler.obtainMessage(7, status);
                                IccFileHandler fh2 = this.mPhone.getIccFileHandler();
                                if (fh2 != null) {
                                    fh2.getEFLinearRecordSize(emailEfid, response2);
                                    waitForResult(status);
                                }
                                if (this.mEmailRecordSize[0] != 0) {
                                    maxLength[0] = this.mRecordSize[0] - 14;
                                    maxLength[1] = 20;
                                    maxLength[2] = this.mEmailRecordSize[0] - 2;
                                    maxLength[3] = 20;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                throw th;
                            }
                        }
                    }
                }
            }
        }
        return maxLength;
    }
}
