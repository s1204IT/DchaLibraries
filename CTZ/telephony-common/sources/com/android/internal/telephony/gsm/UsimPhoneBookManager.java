package com.android.internal.telephony.gsm;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.AdnRecordCache;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import java.util.ArrayList;

public class UsimPhoneBookManager extends Handler implements IccConstants {
    private static final boolean DBG = true;
    protected static final int EVENT_EMAIL_LOAD_DONE = 4;
    protected static final int EVENT_IAP_LOAD_DONE = 3;
    protected static final int EVENT_PBR_LOAD_DONE = 1;
    protected static final int EVENT_USIM_ADN_LOAD_DONE = 2;
    protected static final byte INVALID_BYTE = -1;
    protected static final int INVALID_SFI = -1;
    private static final String LOG_TAG = "UsimPhoneBookManager";
    protected static final int USIM_EFAAS_TAG = 199;
    protected static final int USIM_EFADN_TAG = 192;
    protected static final int USIM_EFANR_TAG = 196;
    protected static final int USIM_EFCCP1_TAG = 203;
    protected static final int USIM_EFEMAIL_TAG = 202;
    protected static final int USIM_EFEXT1_TAG = 194;
    protected static final int USIM_EFGRP_TAG = 198;
    protected static final int USIM_EFGSD_TAG = 200;
    protected static final int USIM_EFIAP_TAG = 193;
    protected static final int USIM_EFPBC_TAG = 197;
    protected static final int USIM_EFSNE_TAG = 195;
    protected static final int USIM_EFUID_TAG = 201;
    protected static final int USIM_TYPE1_TAG = 168;
    protected static final int USIM_TYPE2_TAG = 169;
    protected static final int USIM_TYPE3_TAG = 170;
    protected AdnRecordCache mAdnCache;
    protected ArrayList<byte[]> mEmailFileRecord;
    protected IccFileHandler mFh;
    protected ArrayList<byte[]> mIapFileRecord;
    protected Object mLock = new Object();
    protected boolean mRefreshCache = false;
    protected ArrayList<AdnRecord> mPhoneBookRecords = new ArrayList<>();
    protected ArrayList<PbrRecord> mPbrRecords = null;
    protected Boolean mIsPbrPresent = true;
    protected SparseArray<ArrayList<String>> mEmailsForAdnRec = new SparseArray<>();
    protected SparseIntArray mSfiEfidTable = new SparseIntArray();

    private class File {
        private final int mEfid;
        private final int mIndex;
        private final int mParentTag;
        private final int mSfi;

        File(int i, int i2, int i3, int i4) {
            this.mParentTag = i;
            this.mEfid = i2;
            this.mSfi = i3;
            this.mIndex = i4;
        }

        public int getParentTag() {
            return this.mParentTag;
        }

        public int getEfid() {
            return this.mEfid;
        }

        public int getSfi() {
            return this.mSfi;
        }

        public int getIndex() {
            return this.mIndex;
        }
    }

    public UsimPhoneBookManager(IccFileHandler iccFileHandler, AdnRecordCache adnRecordCache) {
        this.mFh = iccFileHandler;
        this.mAdnCache = adnRecordCache;
    }

    public void reset() {
        this.mPhoneBookRecords.clear();
        this.mIapFileRecord = null;
        this.mEmailFileRecord = null;
        this.mPbrRecords = null;
        this.mIsPbrPresent = true;
        this.mRefreshCache = false;
        this.mEmailsForAdnRec.clear();
        this.mSfiEfidTable.clear();
    }

    public ArrayList<AdnRecord> loadEfFilesFromUsim() {
        synchronized (this.mLock) {
            if (!this.mPhoneBookRecords.isEmpty()) {
                if (this.mRefreshCache) {
                    this.mRefreshCache = false;
                    refreshCache();
                }
                return this.mPhoneBookRecords;
            }
            if (!this.mIsPbrPresent.booleanValue()) {
                return null;
            }
            if (this.mPbrRecords == null) {
                readPbrFileAndWait();
            }
            if (this.mPbrRecords == null) {
                return null;
            }
            int size = this.mPbrRecords.size();
            log("loadEfFilesFromUsim: Loading adn and emails");
            for (int i = 0; i < size; i++) {
                readAdnFileAndWait(i);
                readEmailFileAndWait(i);
            }
            updatePhoneAdnRecord();
            return this.mPhoneBookRecords;
        }
    }

    protected void refreshCache() {
        if (this.mPbrRecords == null) {
            return;
        }
        this.mPhoneBookRecords.clear();
        int size = this.mPbrRecords.size();
        for (int i = 0; i < size; i++) {
            readAdnFileAndWait(i);
        }
    }

    public void invalidateCache() {
        this.mRefreshCache = true;
    }

    protected void readPbrFileAndWait() {
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_PBR, obtainMessage(1));
        try {
            this.mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readAdnFileAndWait");
        }
    }

    private void readEmailFileAndWait(int i) {
        File file;
        SparseArray sparseArray;
        File file2;
        SparseArray sparseArray2 = this.mPbrRecords.get(i).mFileIds;
        if (sparseArray2 != null && (file = (File) sparseArray2.get(USIM_EFEMAIL_TAG)) != null) {
            if (file.getParentTag() == 169) {
                if (sparseArray2.get(193) == null) {
                    Rlog.e(LOG_TAG, "Can't locate EF_IAP in EF_PBR.");
                    return;
                }
                log("EF_IAP exists. Loading EF_IAP to retrieve the index.");
                readIapFileAndWait(((File) sparseArray2.get(193)).getEfid());
                if (this.mIapFileRecord == null) {
                    Rlog.e(LOG_TAG, "Error: IAP file is empty");
                    return;
                }
                log("EF_EMAIL order in PBR record: " + file.getIndex());
            }
            int efid = file.getEfid();
            log("EF_EMAIL exists in PBR. efid = 0x" + Integer.toHexString(efid).toUpperCase());
            for (int i2 = 0; i2 < i; i2++) {
                if (this.mPbrRecords.get(i2) != null && (sparseArray = this.mPbrRecords.get(i2).mFileIds) != null && (file2 = (File) sparseArray.get(USIM_EFEMAIL_TAG)) != null && file2.getEfid() == efid) {
                    log("Skipped this EF_EMAIL which was loaded earlier");
                    return;
                }
            }
            this.mFh.loadEFLinearFixedAll(efid, obtainMessage(4));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readEmailFileAndWait");
            }
            if (this.mEmailFileRecord == null) {
                Rlog.e(LOG_TAG, "Error: Email file is empty");
            } else if (file.getParentTag() == 169 && this.mIapFileRecord != null) {
                buildType2EmailList(i);
            } else {
                buildType1EmailList(i);
            }
        }
    }

    private void buildType1EmailList(int i) {
        int efid;
        if (this.mPbrRecords.get(i) == null) {
            return;
        }
        int i2 = this.mPbrRecords.get(i).mMasterFileRecordNum;
        log("Building type 1 email list. recId = " + i + ", numRecs = " + i2);
        for (int i3 = 0; i3 < i2; i3++) {
            try {
                byte[] bArr = this.mEmailFileRecord.get(i3);
                byte b = bArr[bArr.length - 2];
                byte b2 = bArr[bArr.length - 1];
                String emailRecord = readEmailRecord(i3);
                if (emailRecord != null && !emailRecord.equals("")) {
                    if (b != -1 && this.mSfiEfidTable.get(b) != 0) {
                        efid = this.mSfiEfidTable.get(b);
                    } else {
                        File file = (File) this.mPbrRecords.get(i).mFileIds.get(192);
                        if (file != null) {
                            efid = file.getEfid();
                        }
                    }
                    int i4 = ((b2 + INVALID_BYTE) & 255) | ((efid & 65535) << 8);
                    ArrayList<String> arrayList = this.mEmailsForAdnRec.get(i4);
                    if (arrayList == null) {
                        arrayList = new ArrayList<>();
                    }
                    log("Adding email #" + i3 + " list to index 0x" + Integer.toHexString(i4).toUpperCase());
                    arrayList.add(emailRecord);
                    this.mEmailsForAdnRec.put(i4, arrayList);
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "Error: Improper ICC card: No email record for ADN, continuing");
                return;
            }
        }
    }

    private boolean buildType2EmailList(int i) {
        if (this.mPbrRecords.get(i) == null) {
            return false;
        }
        int i2 = this.mPbrRecords.get(i).mMasterFileRecordNum;
        log("Building type 2 email list. recId = " + i + ", numRecs = " + i2);
        File file = (File) this.mPbrRecords.get(i).mFileIds.get(192);
        if (file == null) {
            Rlog.e(LOG_TAG, "Error: Improper ICC card: EF_ADN does not exist in PBR files");
            return false;
        }
        int efid = file.getEfid();
        for (int i3 = 0; i3 < i2; i3++) {
            try {
                String emailRecord = readEmailRecord(this.mIapFileRecord.get(i3)[((File) this.mPbrRecords.get(i).mFileIds.get(USIM_EFEMAIL_TAG)).getIndex()] - 1);
                if (emailRecord != null && !emailRecord.equals("")) {
                    int i4 = ((65535 & efid) << 8) | (i3 & 255);
                    ArrayList<String> arrayList = this.mEmailsForAdnRec.get(i4);
                    if (arrayList == null) {
                        arrayList = new ArrayList<>();
                    }
                    arrayList.add(emailRecord);
                    log("Adding email list to index 0x" + Integer.toHexString(i4).toUpperCase());
                    this.mEmailsForAdnRec.put(i4, arrayList);
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "Error: Improper ICC card: Corrupted EF_IAP");
            }
        }
        return true;
    }

    private void readIapFileAndWait(int i) {
        this.mFh.loadEFLinearFixedAll(i, obtainMessage(3));
        try {
            this.mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readIapFileAndWait");
        }
    }

    private void updatePhoneAdnRecord() {
        int size = this.mPhoneBookRecords.size();
        for (int i = 0; i < size; i++) {
            AdnRecord adnRecord = this.mPhoneBookRecords.get(i);
            try {
                ArrayList<String> arrayList = this.mEmailsForAdnRec.get(((adnRecord.getEfid() & 65535) << 8) | ((adnRecord.getRecId() - 1) & 255));
                if (arrayList != null) {
                    String[] strArr = new String[arrayList.size()];
                    System.arraycopy(arrayList.toArray(), 0, strArr, 0, arrayList.size());
                    adnRecord.setEmails(strArr);
                    log("Adding email list to ADN (0x" + Integer.toHexString(this.mPhoneBookRecords.get(i).getEfid()).toUpperCase() + ") record #" + this.mPhoneBookRecords.get(i).getRecId());
                    this.mPhoneBookRecords.set(i, adnRecord);
                }
            } catch (IndexOutOfBoundsException e) {
            }
        }
    }

    private String readEmailRecord(int i) {
        try {
            return IccUtils.adnStringFieldToString(this.mEmailFileRecord.get(i), 0, r3.length - 2);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private void readAdnFileAndWait(int i) {
        SparseArray sparseArray = this.mPbrRecords.get(i).mFileIds;
        if (sparseArray == null || sparseArray.size() == 0) {
            return;
        }
        int efid = 0;
        if (sparseArray.get(194) != null) {
            efid = ((File) sparseArray.get(194)).getEfid();
        }
        if (sparseArray.get(192) == null) {
            return;
        }
        int size = this.mPhoneBookRecords.size();
        this.mAdnCache.requestLoadAllAdnLike(((File) sparseArray.get(192)).getEfid(), efid, obtainMessage(2));
        try {
            this.mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readAdnFileAndWait");
        }
        this.mPbrRecords.get(i).mMasterFileRecordNum = this.mPhoneBookRecords.size() - size;
    }

    private void createPbrFile(ArrayList<byte[]> arrayList) {
        int sfi;
        if (arrayList == null) {
            this.mPbrRecords = null;
            this.mIsPbrPresent = false;
            return;
        }
        this.mPbrRecords = new ArrayList<>();
        for (int i = 0; i < arrayList.size(); i++) {
            if (arrayList.get(i)[0] != -1) {
                this.mPbrRecords.add(new PbrRecord(arrayList.get(i)));
            }
        }
        for (PbrRecord pbrRecord : this.mPbrRecords) {
            File file = (File) pbrRecord.mFileIds.get(192);
            if (file != null && (sfi = file.getSfi()) != -1) {
                this.mSfiEfidTable.put(sfi, ((File) pbrRecord.mFileIds.get(192)).getEfid());
            }
        }
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 1:
                log("Loading PBR records done");
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.exception == null) {
                    createPbrFile((ArrayList) asyncResult.result);
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 2:
                log("Loading USIM ADN records done");
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                if (asyncResult2.exception == null) {
                    this.mPhoneBookRecords.addAll((ArrayList) asyncResult2.result);
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 3:
                log("Loading USIM IAP records done");
                AsyncResult asyncResult3 = (AsyncResult) message.obj;
                if (asyncResult3.exception == null) {
                    this.mIapFileRecord = (ArrayList) asyncResult3.result;
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 4:
                log("Loading USIM Email records done");
                AsyncResult asyncResult4 = (AsyncResult) message.obj;
                if (asyncResult4.exception == null) {
                    this.mEmailFileRecord = (ArrayList) asyncResult4.result;
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            default:
                return;
        }
    }

    private class PbrRecord {
        private SparseArray<File> mFileIds = new SparseArray<>();
        private int mMasterFileRecordNum;

        PbrRecord(byte[] bArr) {
            UsimPhoneBookManager.this.log("PBR rec: " + IccUtils.bytesToHexString(bArr));
            parseTag(new SimTlv(bArr, 0, bArr.length));
        }

        void parseTag(SimTlv simTlv) {
            do {
                int tag = simTlv.getTag();
                switch (tag) {
                    case 168:
                    case 169:
                    case 170:
                        byte[] data = simTlv.getData();
                        parseEfAndSFI(new SimTlv(data, 0, data.length), tag);
                        break;
                }
            } while (simTlv.nextObject());
        }

        void parseEfAndSFI(SimTlv simTlv, int i) {
            int i2 = 0;
            do {
                int tag = simTlv.getTag();
                switch (tag) {
                    case 192:
                    case 193:
                    case 194:
                    case 195:
                    case 196:
                    case 197:
                    case UsimPhoneBookManager.USIM_EFGRP_TAG:
                    case UsimPhoneBookManager.USIM_EFAAS_TAG:
                    case 200:
                    case UsimPhoneBookManager.USIM_EFUID_TAG:
                    case UsimPhoneBookManager.USIM_EFEMAIL_TAG:
                    case UsimPhoneBookManager.USIM_EFCCP1_TAG:
                        int i3 = -1;
                        byte[] data = simTlv.getData();
                        if (data.length < 2 || data.length > 3) {
                            UsimPhoneBookManager.this.log("Invalid TLV length: " + data.length);
                        } else {
                            if (data.length == 3) {
                                i3 = data[2] & UsimPhoneBookManager.INVALID_BYTE;
                            }
                            this.mFileIds.put(tag, UsimPhoneBookManager.this.new File(i, ((data[0] & UsimPhoneBookManager.INVALID_BYTE) << 8) | (data[1] & UsimPhoneBookManager.INVALID_BYTE), i3, i2));
                        }
                        break;
                }
                i2++;
            } while (simTlv.nextObject());
        }
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
    }
}
