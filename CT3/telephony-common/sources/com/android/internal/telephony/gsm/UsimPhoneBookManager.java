package com.android.internal.telephony.gsm;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.AdnRecordCache;
import com.android.internal.telephony.uicc.CsimFileHandler;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccException;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.mediatek.internal.telephony.uicc.AlphaTag;
import com.mediatek.internal.telephony.uicc.CsimPhbStorageInfo;
import com.mediatek.internal.telephony.uicc.EFResponseData;
import com.mediatek.internal.telephony.uicc.PhbEntry;
import com.mediatek.internal.telephony.uicc.UsimGroup;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UsimPhoneBookManager extends Handler implements IccConstants {
    private static final boolean DBG;
    private static final int EVENT_AAS_LOAD_DONE = 5;
    private static final int EVENT_AAS_LOAD_DONE_OPTMZ = 28;
    private static final int EVENT_AAS_UPDATE_DONE = 10;
    private static final int EVENT_ANR_RECORD_LOAD_DONE = 16;
    private static final int EVENT_ANR_RECORD_LOAD_OPTMZ_DONE = 23;
    private static final int EVENT_ANR_UPDATE_DONE = 9;
    private static final int EVENT_EMAIL_LOAD_DONE = 4;
    private static final int EVENT_EMAIL_RECORD_LOAD_DONE = 15;
    private static final int EVENT_EMAIL_RECORD_LOAD_OPTMZ_DONE = 22;
    private static final int EVENT_EMAIL_UPDATE_DONE = 8;
    private static final int EVENT_EXT1_LOAD_DONE = 1001;
    private static final int EVENT_GAS_LOAD_DONE = 6;
    private static final int EVENT_GAS_UPDATE_DONE = 13;
    private static final int EVENT_GET_RECORDS_SIZE_DONE = 1000;
    private static final int EVENT_GRP_RECORD_LOAD_DONE = 17;
    private static final int EVENT_GRP_UPDATE_DONE = 12;
    private static final int EVENT_IAP_LOAD_DONE = 3;
    private static final int EVENT_IAP_RECORD_LOAD_DONE = 14;
    private static final int EVENT_IAP_UPDATE_DONE = 7;
    private static final int EVENT_PBR_LOAD_DONE = 1;
    private static final int EVENT_QUERY_ANR_AVAILABLE_OPTMZ_DONE = 26;
    private static final int EVENT_QUERY_EMAIL_AVAILABLE_OPTMZ_DONE = 25;
    private static final int EVENT_QUERY_PHB_ADN_INFO = 21;
    private static final int EVENT_QUERY_SNE_AVAILABLE_OPTMZ_DONE = 27;
    private static final int EVENT_SELECT_EF_FILE_DONE = 20;
    private static final int EVENT_SNE_RECORD_LOAD_DONE = 18;
    private static final int EVENT_SNE_RECORD_LOAD_OPTMZ_DONE = 24;
    private static final int EVENT_SNE_UPDATE_DONE = 11;
    private static final int EVENT_UPB_CAPABILITY_QUERY_DONE = 19;
    private static final int EVENT_USIM_ADN_LOAD_DONE = 2;
    private static final byte INVALID_BYTE = -1;
    private static final int INVALID_SFI = -1;
    private static final String LOG_TAG = "UsimPhoneBookManager";
    private static final String PROP_FORCE_DEBUG_KEY = "persist.log.tag.tel_dbg";
    private static final int UPB_EF_AAS = 3;
    private static final int UPB_EF_ANR = 0;
    private static final int UPB_EF_EMAIL = 1;
    private static final int UPB_EF_GAS = 4;
    private static final int UPB_EF_GRP = 5;
    private static final int UPB_EF_SNE = 2;
    private static final int USIM_EFAAS_TAG = 199;
    private static final int USIM_EFADN_TAG = 192;
    private static final int USIM_EFANR_TAG = 196;
    private static final int USIM_EFCCP1_TAG = 203;
    private static final int USIM_EFEMAIL_TAG = 202;
    private static final int USIM_EFEXT1_TAG = 194;
    private static final int USIM_EFGRP_TAG = 198;
    private static final int USIM_EFGSD_TAG = 200;
    private static final int USIM_EFIAP_TAG = 193;
    private static final int USIM_EFPBC_TAG = 197;
    private static final int USIM_EFSNE_TAG = 195;
    private static final int USIM_EFUID_TAG = 201;
    public static final int USIM_ERROR_CAPACITY_FULL = -30;
    public static final int USIM_ERROR_GROUP_COUNT = -20;
    public static final int USIM_ERROR_NAME_LEN = -10;
    public static final int USIM_ERROR_OTHERS = -50;
    public static final int USIM_ERROR_STRING_TOOLONG = -40;
    public static final int USIM_MAX_ANR_COUNT = 3;
    private static final int USIM_TYPE1_TAG = 168;
    private static final int USIM_TYPE2_CONDITIONAL_LENGTH = 2;
    private static final int USIM_TYPE2_TAG = 169;
    private static final int USIM_TYPE3_TAG = 170;
    protected EFResponseData efData;
    private ArrayList<String> mAasForAnr;
    private AdnRecordCache mAdnCache;
    private int mAdnFileSize;
    private int[] mAdnRecordSize;
    private ArrayList<int[]> mAnrInfo;
    private int mAnrRecordSize;
    private CommandsInterface mCi;
    private UiccCardApplication mCurrentApp;
    private ArrayList<byte[]> mEmailFileRecord;
    private int mEmailFileSize;
    private int[] mEmailInfo;
    private int[] mEmailRecTable;
    private int mEmailRecordSize;
    private SparseArray<ArrayList<String>> mEmailsForAdnRec;
    private ArrayList<ArrayList<byte[]>> mExt1FileList;
    private IccFileHandler mFh;
    private ArrayList<UsimGroup> mGasForGrp;
    private final Object mGasLock;
    private ArrayList<ArrayList<byte[]>> mIapFileList;
    private ArrayList<byte[]> mIapFileRecord;
    private Boolean mIsPbrPresent;
    private final Object mLock;
    private AtomicBoolean mNeedNotify;
    private ArrayList<PbrRecord> mPbrRecords;
    private ArrayList<AdnRecord> mPhoneBookRecords;
    private AtomicInteger mReadingAnrNum;
    private AtomicInteger mReadingEmailNum;
    private AtomicInteger mReadingGrpNum;
    private AtomicInteger mReadingIapNum;
    private AtomicInteger mReadingSneNum;
    private SparseArray<int[]> mRecordSize;
    private boolean mRefreshCache;
    private int mResult;
    private SparseIntArray mSfiEfidTable;
    private int mSliceCount;
    private int mSlotId;
    private int[] mSneInfo;
    private final Object mUPBCapabilityLock;
    private int[] mUpbCap;

    static {
        DBG = SystemProperties.getInt(PROP_FORCE_DEBUG_KEY, 0) == 1;
    }

    private class File {
        public int mAnrIndex;
        private final int mEfid;
        private final int mIndex;
        private final int mParentTag;
        public int mPbrRecord;
        private final int mSfi;
        public int mTag;

        File(int parentTag, int efid, int sfi, int index) {
            this.mParentTag = parentTag;
            this.mEfid = efid;
            this.mSfi = sfi;
            this.mIndex = index;
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

        public String toString() {
            return "mParentTag:" + Integer.toHexString(this.mParentTag).toUpperCase() + ",mEfid:" + Integer.toHexString(this.mEfid).toUpperCase() + ",mSfi:" + Integer.toHexString(this.mSfi).toUpperCase() + ",mIndex:" + this.mIndex + ",mPbrRecord:" + this.mPbrRecord + ",mAnrIndex" + this.mAnrIndex + ",mTag:" + Integer.toHexString(this.mTag).toUpperCase();
        }
    }

    public UsimPhoneBookManager(IccFileHandler fh, AdnRecordCache cache) {
        this.mSlotId = -1;
        this.mLock = new Object();
        this.mGasLock = new Object();
        this.mUPBCapabilityLock = new Object();
        this.mEmailRecordSize = -1;
        this.mEmailFileSize = 100;
        this.mAdnFileSize = 250;
        this.mAnrRecordSize = 0;
        this.mSliceCount = 0;
        this.mIapFileList = null;
        this.mRefreshCache = false;
        this.mEmailRecTable = new int[400];
        this.mUpbCap = new int[8];
        this.mResult = -1;
        this.mReadingAnrNum = new AtomicInteger(0);
        this.mReadingEmailNum = new AtomicInteger(0);
        this.mReadingGrpNum = new AtomicInteger(0);
        this.mReadingSneNum = new AtomicInteger(0);
        this.mReadingIapNum = new AtomicInteger(0);
        this.mNeedNotify = new AtomicBoolean(false);
        this.efData = null;
        this.mFh = fh;
        this.mPhoneBookRecords = new ArrayList<>();
        this.mGasForGrp = new ArrayList<>();
        this.mIapFileList = new ArrayList<>();
        this.mPbrRecords = null;
        this.mIsPbrPresent = true;
        this.mAdnCache = cache;
        this.mCi = null;
        this.mCurrentApp = null;
        this.mEmailsForAdnRec = new SparseArray<>();
        this.mSfiEfidTable = new SparseIntArray();
        logi("constructor finished. ");
    }

    public UsimPhoneBookManager(IccFileHandler fh, AdnRecordCache cache, CommandsInterface ci, UiccCardApplication app) {
        this.mSlotId = -1;
        this.mLock = new Object();
        this.mGasLock = new Object();
        this.mUPBCapabilityLock = new Object();
        this.mEmailRecordSize = -1;
        this.mEmailFileSize = 100;
        this.mAdnFileSize = 250;
        this.mAnrRecordSize = 0;
        this.mSliceCount = 0;
        this.mIapFileList = null;
        this.mRefreshCache = false;
        this.mEmailRecTable = new int[400];
        this.mUpbCap = new int[8];
        this.mResult = -1;
        this.mReadingAnrNum = new AtomicInteger(0);
        this.mReadingEmailNum = new AtomicInteger(0);
        this.mReadingGrpNum = new AtomicInteger(0);
        this.mReadingSneNum = new AtomicInteger(0);
        this.mReadingIapNum = new AtomicInteger(0);
        this.mNeedNotify = new AtomicBoolean(false);
        this.efData = null;
        this.mFh = fh;
        this.mPhoneBookRecords = new ArrayList<>();
        this.mGasForGrp = new ArrayList<>();
        this.mIapFileList = new ArrayList<>();
        this.mPbrRecords = null;
        this.mIsPbrPresent = true;
        this.mAdnCache = cache;
        this.mCi = ci;
        this.mCurrentApp = app;
        this.mSlotId = app != null ? app.getSlotId() : -1;
        this.mEmailsForAdnRec = new SparseArray<>();
        this.mSfiEfidTable = new SparseIntArray();
        logi("constructor finished. ");
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
        this.mGasForGrp.clear();
        this.mIapFileList = null;
        this.mAasForAnr = null;
        this.mExt1FileList = null;
        this.mSliceCount = 0;
        this.mEmailRecTable = new int[400];
        this.mEmailInfo = null;
        this.mSneInfo = null;
        this.mAnrInfo = null;
        for (int i = 0; i < 8; i++) {
            this.mUpbCap[i] = 0;
        }
        logi("reset finished. ");
    }

    public ArrayList<AdnRecord> loadEfFilesFromUsim() {
        int[] size;
        int[] size2;
        synchronized (this.mLock) {
            long prevTime = System.currentTimeMillis();
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
            if (this.mPbrRecords == null || this.mPbrRecords.size() == 0) {
                readPbrFileAndWait(false);
            }
            if (this.mPbrRecords == null || this.mPbrRecords.size() == 0) {
                readPbrFileAndWait(true);
            }
            if (this.mPbrRecords == null || this.mPbrRecords.size() == 0) {
                readAdnFileAndWait(0);
                return this.mAdnCache.getRecordsIfLoaded(28474);
            }
            if (this.mPbrRecords.get(0).mFileIds.get(USIM_EFEMAIL_TAG) != null && (size2 = readEFLinearRecordSize(((File) this.mPbrRecords.get(0).mFileIds.get(USIM_EFEMAIL_TAG)).getEfid())) != null && size2.length == 3) {
                this.mEmailFileSize = size2[2];
                this.mEmailRecordSize = size2[0];
            }
            int adnEf = ((File) this.mPbrRecords.get(0).mFileIds.get(192)).getEfid();
            if (adnEf > 0 && (size = readEFLinearRecordSize(adnEf)) != null && size.length == 3) {
                this.mAdnFileSize = size[2];
            }
            readAnrRecordSize();
            if (this.mPbrRecords.get(0).mFileIds.get(195) != null) {
                readEFLinearRecordSize(((File) this.mPbrRecords.get(0).mFileIds.get(195)).getEfid());
            }
            int numRecs = this.mPbrRecords.size();
            if (this.mFh instanceof CsimFileHandler) {
                for (int i = 0; i < numRecs; i++) {
                    readAdnFileAndWaitForUICC(i);
                }
            } else {
                readAdnFileAndWait(0);
            }
            if (this.mPhoneBookRecords.isEmpty()) {
                return this.mPhoneBookRecords;
            }
            if (this.mFh instanceof CsimFileHandler) {
                for (int i2 = 0; i2 < numRecs; i2++) {
                    readAASFileAndWait(i2);
                    readSneFileAndWait(i2);
                    readAnrFileAndWait(i2);
                    readEmailFileAndWait(i2);
                }
            } else {
                readAasFileAndWaitOptmz();
                readSneFileAndWaitOptmz();
                readAnrFileAndWaitOptmz();
                readEmailFileAndWaitOptmz();
            }
            readGrpIdsAndWait();
            long endTime = System.currentTimeMillis();
            log("loadEfFilesFromUsim Time: " + (endTime - prevTime) + " AppType: " + this.mCurrentApp.getType());
            return this.mPhoneBookRecords;
        }
    }

    private void refreshCache() {
        if (this.mPbrRecords == null) {
            return;
        }
        this.mPhoneBookRecords.clear();
        int numRecs = this.mPbrRecords.size();
        for (int i = 0; i < numRecs; i++) {
            readAdnFileAndWait(i);
        }
    }

    public void invalidateCache() {
        this.mRefreshCache = true;
    }

    private void readPbrFileAndWait(boolean is7FFF) {
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_PBR, obtainMessage(1), is7FFF);
        try {
            this.mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readPbrFileAndWait");
        }
    }

    private void readEmailFileAndWait(int recId) {
        SparseArray<File> files;
        File emailFile;
        logi("readEmailFileAndWait " + recId);
        if (this.mPbrRecords == null || (files = this.mPbrRecords.get(recId).mFileIds) == null || (emailFile = files.get(USIM_EFEMAIL_TAG)) == null) {
            return;
        }
        emailFile.getEfid();
        if (emailFile.getParentTag() == 168) {
            readType1Ef(emailFile, 0);
        } else {
            if (emailFile.getParentTag() != 169) {
                return;
            }
            readType2Ef(emailFile);
        }
    }

    private void readIapFileAndWait(int pbrIndex, int efid, boolean forceRefresh) {
        int[] size;
        logi("readIapFileAndWait pbrIndex :" + pbrIndex + ",efid:" + efid + ",forceRefresh:" + forceRefresh);
        if (efid <= 0) {
            return;
        }
        if (this.mIapFileList == null) {
            logi("readIapFileAndWait IapFileList is null !!!! recreate it !");
            this.mIapFileList = new ArrayList<>();
        }
        if (this.mRecordSize != null && this.mRecordSize.get(efid) != null) {
            int[] size2 = this.mRecordSize.get(efid);
            size = size2;
        } else {
            size = readEFLinearRecordSize(efid);
        }
        if (size == null || size.length != 3) {
            Rlog.e(LOG_TAG, "readIapFileAndWait: read record size error.");
            this.mIapFileList.add(pbrIndex, new ArrayList<>());
            return;
        }
        if (this.mIapFileList.size() <= pbrIndex) {
            log("Create IAP first!");
            ArrayList<byte[]> iapList = new ArrayList<>();
            for (int i = 0; i < this.mAdnFileSize; i++) {
                byte[] value = new byte[size[0]];
                for (byte b : value) {
                }
                iapList.add(value);
            }
            this.mIapFileList.add(pbrIndex, iapList);
        } else {
            log("This IAP has been loaded!");
            if (!forceRefresh) {
                return;
            }
        }
        int numAdnRecs = this.mPhoneBookRecords.size();
        int nOffset = pbrIndex * this.mAdnFileSize;
        int nMax = nOffset + this.mAdnFileSize;
        if (numAdnRecs < nMax) {
            nMax = numAdnRecs;
        }
        log("readIapFileAndWait nOffset " + nOffset + ", nMax " + nMax);
        int totalReadingNum = 0;
        for (int i2 = nOffset; i2 < nMax; i2++) {
            try {
                AdnRecord rec = this.mPhoneBookRecords.get(i2);
                if (rec.getAlphaTag().length() > 0 || rec.getNumber().length() > 0) {
                    this.mReadingIapNum.addAndGet(1);
                    int[] data = {pbrIndex, i2 - nOffset};
                    this.mFh.readEFLinearFixed(efid, (i2 + 1) - nOffset, size[0], obtainMessage(14, data));
                    totalReadingNum++;
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "readIapFileAndWait: mPhoneBookRecords IndexOutOfBoundsException numAdnRecs is " + numAdnRecs + "index is " + i2);
            }
        }
        if (this.mReadingIapNum.get() == 0) {
            this.mNeedNotify.set(false);
            return;
        }
        this.mNeedNotify.set(true);
        logi("readIapFileAndWait before mLock.wait " + this.mNeedNotify.get() + " total:" + totalReadingNum);
        synchronized (this.mLock) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e2) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readIapFileAndWait");
            }
        }
        logi("readIapFileAndWait after mLock.wait");
    }

    private void readAASFileAndWait(int recId) {
        SparseArray<File> files;
        File aasFile;
        logi("readAASFileAndWait " + recId);
        if (this.mPbrRecords == null || (files = this.mPbrRecords.get(recId).mFileIds) == null || (aasFile = files.get(USIM_EFAAS_TAG)) == null) {
            return;
        }
        int aasEfid = aasFile.getEfid();
        log("readAASFileAndWait-get AAS EFID " + aasEfid);
        if (this.mAasForAnr != null) {
            logi("AAS has been loaded for Pbr number " + recId);
        }
        if (this.mFh != null) {
            Message msg = obtainMessage(5);
            msg.arg1 = recId;
            this.mFh.loadEFLinearFixedAll(aasEfid, msg);
            try {
                this.mLock.wait();
                return;
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readAASFileAndWait");
                return;
            }
        }
        Rlog.e(LOG_TAG, "readAASFileAndWait-IccFileHandler is null");
    }

    private void readSneFileAndWait(int recId) {
        SparseArray<File> files;
        File sneFile;
        logi("readSneFileAndWait " + recId);
        if (this.mPbrRecords == null || (files = this.mPbrRecords.get(recId).mFileIds) == null || (sneFile = files.get(195)) == null) {
            return;
        }
        int sneEfid = sneFile.getEfid();
        log("readSneFileAndWait: EFSNE id is " + sneEfid);
        if (sneFile.getParentTag() == 169) {
            readType2Ef(sneFile);
        } else {
            if (sneFile.getParentTag() != 168) {
                return;
            }
            readType1Ef(sneFile, 0);
        }
    }

    private void readAnrFileAndWait(int recId) {
        logi("readAnrFileAndWait: recId is " + recId);
        if (this.mPbrRecords == null) {
            return;
        }
        SparseArray<File> files = this.mPbrRecords.get(recId).mFileIds;
        if (files == null) {
            log("readAnrFileAndWait: No anr tag in pbr record " + recId);
            return;
        }
        for (int index = 0; index < this.mPbrRecords.get(recId).mAnrIndex; index++) {
            File anrFile = files.get((index * 256) + 196);
            if (anrFile != null) {
                if (anrFile.getParentTag() == 169) {
                    anrFile.mAnrIndex = index;
                    readType2Ef(anrFile);
                    return;
                } else {
                    if (anrFile.getParentTag() != 168) {
                        return;
                    }
                    anrFile.mAnrIndex = index;
                    readType1Ef(anrFile, index);
                    return;
                }
            }
        }
    }

    private void readGrpIdsAndWait() {
        logi("readGrpIdsAndWait begin");
        int totalReadingNum = 0;
        int numAdnRecs = this.mPhoneBookRecords.size();
        for (int i = 0; i < numAdnRecs; i++) {
            try {
                AdnRecord rec = this.mPhoneBookRecords.get(i);
                if (rec.getAlphaTag().length() > 0 || rec.getNumber().length() > 0) {
                    this.mReadingGrpNum.incrementAndGet();
                    int adnIndex = rec.getRecId();
                    int[] data = {i, adnIndex};
                    this.mCi.readUPBGrpEntry(adnIndex, obtainMessage(17, data));
                    totalReadingNum++;
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "readGrpIdsAndWait: mPhoneBookRecords IndexOutOfBoundsException numAdnRecs is " + numAdnRecs + "index is " + i);
            }
        }
        if (this.mReadingGrpNum.get() == 0) {
            this.mNeedNotify.set(false);
            return;
        }
        this.mNeedNotify.set(true);
        logi("readGrpIdsAndWait before mLock.wait " + this.mNeedNotify.get() + " total:" + totalReadingNum);
        try {
            this.mLock.wait();
        } catch (InterruptedException e2) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readGrpIdsAndWait");
        }
        logi("readGrpIdsAndWait after mLock.wait");
    }

    private void readAdnFileAndWait(int recId) {
        logi("readAdnFileAndWait: recId is " + recId + UsimPBMemInfo.STRING_NOT_SET);
        int previousSize = this.mPhoneBookRecords.size();
        this.mAdnCache.requestLoadAllAdnLike(28474, this.mAdnCache.extensionEfForEf(28474), obtainMessage(2));
        try {
            this.mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readAdnFileAndWait");
        }
        if (this.mPbrRecords == null || this.mPbrRecords.size() <= recId) {
            return;
        }
        this.mPbrRecords.get(recId).mMasterFileRecordNum = this.mPhoneBookRecords.size() - previousSize;
    }

    private void createPbrFile(ArrayList<byte[]> records) {
        int sfi;
        if (records == null) {
            this.mPbrRecords = null;
            this.mIsPbrPresent = false;
            return;
        }
        this.mPbrRecords = new ArrayList<>();
        this.mSliceCount = 0;
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i)[0] != -1) {
                this.mPbrRecords.add(new PbrRecord(records.get(i)));
            }
        }
        for (PbrRecord record : this.mPbrRecords) {
            File file = (File) record.mFileIds.get(192);
            if (file != null && (sfi = file.getSfi()) != -1) {
                this.mSfiEfidTable.put(sfi, ((File) record.mFileIds.get(192)).getEfid());
            }
        }
    }

    private void readAasFileAndWaitOptmz() {
        SparseArray<File> files;
        logi("readAasFileAndWaitOptmz begin");
        if (this.mAasForAnr != null && this.mAasForAnr.size() != 0) {
            return;
        }
        if (this.mUpbCap[3] <= 0) {
            if (this.mPbrRecords == null || (files = this.mPbrRecords.get(0).mFileIds) == null) {
                return;
            }
            File aasFile = files.get(USIM_EFAAS_TAG);
            if (aasFile == null) {
                return;
            }
        }
        this.mCi.readUPBAasList(1, this.mUpbCap[3], obtainMessage(28));
        try {
            this.mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readAasFileAndWaitOptmz");
        }
    }

    private void readEmailFileAndWaitOptmz() {
        SparseArray<File> files;
        if (this.mPbrRecords == null || (files = this.mPbrRecords.get(0).mFileIds) == null) {
            return;
        }
        File emailFile = files.get(USIM_EFEMAIL_TAG);
        if (emailFile == null) {
            return;
        }
        int totalReadingNum = 0;
        int numAdnRecs = this.mPhoneBookRecords.size();
        for (int i = 0; i < numAdnRecs; i++) {
            try {
                AdnRecord rec = this.mPhoneBookRecords.get(i);
                if (rec.getAlphaTag().length() > 0 || rec.getNumber().length() > 0) {
                    int[] data = {0, i};
                    this.mReadingEmailNum.incrementAndGet();
                    this.mCi.readUPBEmailEntry(i + 1, 1, obtainMessage(22, data));
                    totalReadingNum++;
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "readEmailFileAndWaitOptmz: mPhoneBookRecords IndexOutOfBoundsnumAdnRecs is " + numAdnRecs + "index is " + i);
            }
        }
        if (this.mReadingEmailNum.get() == 0) {
            this.mNeedNotify.set(false);
            return;
        }
        this.mNeedNotify.set(true);
        logi("readEmailFileAndWaitOptmz before mLock.wait " + this.mNeedNotify.get() + " total:" + totalReadingNum);
        synchronized (this.mLock) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e2) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readEmailFileAndWaitOptmz");
            }
        }
        logi("readEmailFileAndWaitOptmz after mLock.wait " + this.mNeedNotify.get());
    }

    private void readAnrFileAndWaitOptmz() {
        SparseArray<File> files;
        if (this.mPbrRecords == null || (files = this.mPbrRecords.get(0).mFileIds) == null) {
            return;
        }
        File anrFile = files.get(196);
        if (anrFile == null) {
            return;
        }
        int totalReadingNum = 0;
        int numAdnRecs = this.mPhoneBookRecords.size();
        for (int i = 0; i < numAdnRecs; i++) {
            try {
                AdnRecord rec = this.mPhoneBookRecords.get(i);
                if (rec.getAlphaTag().length() > 0 || rec.getNumber().length() > 0) {
                    int[] data = {0, i, 0};
                    this.mReadingAnrNum.addAndGet(1);
                    this.mCi.readUPBAnrEntry(i + 1, 1, obtainMessage(23, data));
                    totalReadingNum++;
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "readAnrFileAndWaitOptmz: mPhoneBookRecords IndexOutOfBoundsnumAdnRecs is " + numAdnRecs + "index is " + i);
            }
        }
        if (this.mReadingAnrNum.get() == 0) {
            this.mNeedNotify.set(false);
            return;
        }
        this.mNeedNotify.set(true);
        logi("readAnrFileAndWaitOptmz before mLock.wait " + this.mNeedNotify.get() + " total:" + totalReadingNum);
        synchronized (this.mLock) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e2) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readAnrFileAndWaitOptmz");
            }
        }
        logi("readAnrFileAndWaitOptmz after mLock.wait " + this.mNeedNotify.get());
    }

    private void readSneFileAndWaitOptmz() {
        SparseArray<File> files;
        if (this.mPbrRecords == null || (files = this.mPbrRecords.get(0).mFileIds) == null) {
            return;
        }
        File sneFile = files.get(195);
        if (sneFile == null) {
            return;
        }
        int totalReadingNum = 0;
        int numAdnRecs = this.mPhoneBookRecords.size();
        for (int i = 0; i < numAdnRecs; i++) {
            try {
                AdnRecord rec = this.mPhoneBookRecords.get(i);
                if (rec.getAlphaTag().length() > 0 || rec.getNumber().length() > 0) {
                    int[] data = {0, i};
                    this.mReadingSneNum.incrementAndGet();
                    this.mCi.readUPBSneEntry(i + 1, 1, obtainMessage(24, data));
                    totalReadingNum++;
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "readSneFileAndWaitOptmz: mPhoneBookRecords IndexOutOfBoundsnumAdnRecs is " + numAdnRecs + "index is " + i);
            }
        }
        if (this.mReadingSneNum.get() == 0) {
            this.mNeedNotify.set(false);
            return;
        }
        this.mNeedNotify.set(true);
        logi("readSneFileAndWaitOptmz before mLock.wait " + this.mNeedNotify.get() + " total:" + totalReadingNum);
        synchronized (this.mLock) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e2) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readSneFileAndWaitOptmz");
            }
        }
        logi("readSneFileAndWaitOptmz after mLock.wait " + this.mNeedNotify.get());
    }

    private void updatePhoneAdnRecordWithEmailByIndexOptmz(int emailIndex, int adnIndex, String email) {
        log("updatePhoneAdnRecordWithEmailByIndex emailIndex = " + emailIndex + ",adnIndex = " + adnIndex + ", email = " + email);
        if (email == null || email == null) {
            return;
        }
        try {
            if (email.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                return;
            }
            AdnRecord rec = this.mPhoneBookRecords.get(adnIndex);
            rec.setEmails(new String[]{email});
        } catch (IndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "[JE]updatePhoneAdnRecordWithEmailByIndex " + e.getMessage());
        }
    }

    private void updatePhoneAdnRecordWithAnrByIndexOptmz(int recId, int adnIndex, int anrIndex, PhbEntry anrData) {
        String anr;
        log("updatePhoneAdnRecordWithAnrByIndexOptmz the " + adnIndex + "th anr record is " + anrData);
        if (anrData == null || anrData.number == null || anrData.number.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            return;
        }
        if (anrData.ton == 145) {
            anr = PhoneNumberUtils.prependPlusToNumber(anrData.number);
        } else {
            anr = anrData.number;
        }
        String anr2 = anr.replace('?', 'N').replace('p', ',').replace('w', ';');
        int anrAas = anrData.index;
        if (anr2 == null || anr2.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            return;
        }
        String aas = null;
        if (anrAas > 0 && anrAas != 255 && this.mAasForAnr != null && this.mAasForAnr != null && anrAas <= this.mAasForAnr.size()) {
            aas = this.mAasForAnr.get(anrAas - 1);
        }
        log(" updatePhoneAdnRecordWithAnrByIndex " + adnIndex + " th anr is " + anr2 + " the anrIndex is " + anrIndex);
        try {
            AdnRecord rec = this.mPhoneBookRecords.get(adnIndex);
            rec.setAnr(anr2, anrIndex);
            if (aas != null && aas.length() > 0) {
                rec.setAasIndex(anrAas);
            }
            this.mPhoneBookRecords.set(adnIndex, rec);
        } catch (IndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "updatePhoneAdnRecordWithAnrByIndex: mPhoneBookRecords IndexOutOfBoundsException size: " + this.mPhoneBookRecords.size() + "index: " + adnIndex);
        }
    }

    private String[] buildAnrRecordOptmz(String number, int aas) {
        int ton = 129;
        if (number.indexOf(43) != -1) {
            if (number.indexOf(43) != number.lastIndexOf(43)) {
                Rlog.w(LOG_TAG, "There are multiple '+' in the number: " + number);
            }
            ton = 145;
            number = number.replace("+", UsimPBMemInfo.STRING_NOT_SET);
        }
        String[] res = {number.replace('N', '?').replace(',', 'p').replace(';', 'w'), Integer.toString(ton), Integer.toString(aas)};
        return res;
    }

    private void updatePhoneAdnRecordWithSneByIndexOptmz(int adnIndex, String sne) {
        if (sne == null) {
            return;
        }
        log("updatePhoneAdnRecordWithSneByIndex index " + adnIndex + " recData file is " + sne);
        if (sne == null || sne.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            return;
        }
        try {
            AdnRecord rec = this.mPhoneBookRecords.get(adnIndex);
            rec.setSne(sne);
        } catch (IndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "updatePhoneAdnRecordWithSneByIndex: mPhoneBookRecords IndexOutOfBoundsException size() is " + this.mPhoneBookRecords.size() + "index is " + adnIndex);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        Object obj;
        String[] aasList;
        ArrayList<byte[]> record;
        ArrayList<byte[]> aasFileRecords;
        String[] gasList;
        switch (msg.what) {
            case 1:
                logi("handleMessage: EVENT_PBR_LOAD_DONE");
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    createPbrFile((ArrayList) ar.result);
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 2:
                logi("Loading USIM ADN records done");
                AsyncResult ar2 = (AsyncResult) msg.obj;
                if (ar2.exception != null || this.mPhoneBookRecords == null) {
                    Rlog.w(LOG_TAG, "Loading USIM ADN records fail.");
                } else if ((this.mFh instanceof CsimFileHandler) && this.mPhoneBookRecords.size() > 0 && ar2.result != null) {
                    ArrayList<AdnRecord> adnList = changeAdnRecordNumber(this.mPhoneBookRecords.size(), (ArrayList) ar2.result);
                    this.mPhoneBookRecords.addAll(adnList);
                    CsimPhbStorageInfo.checkPhbStorage(adnList);
                } else if (ar2.result != null) {
                    this.mPhoneBookRecords.addAll((ArrayList) ar2.result);
                    if (this.mFh instanceof CsimFileHandler) {
                        CsimPhbStorageInfo.checkPhbStorage((ArrayList) ar2.result);
                    }
                    log("Loading USIM ADN records " + this.mPhoneBookRecords.size());
                } else {
                    log("Loading USIM ADN records ar.result:" + ar2.result);
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 3:
                logi("Loading USIM IAP records done");
                AsyncResult ar3 = (AsyncResult) msg.obj;
                if (ar3.exception == null) {
                    this.mIapFileRecord = (ArrayList) ar3.result;
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 4:
                logi("Loading USIM Email records done");
                AsyncResult ar4 = (AsyncResult) msg.obj;
                if (ar4.exception == null) {
                    this.mEmailFileRecord = (ArrayList) ar4.result;
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 5:
                AsyncResult ar5 = (AsyncResult) msg.obj;
                int pbrIndexAAS = msg.arg1;
                logi("EVENT_AAS_LOAD_DONE done pbr " + pbrIndexAAS);
                if (ar5.exception == null && (aasFileRecords = (ArrayList) ar5.result) != null) {
                    int size = aasFileRecords.size();
                    ArrayList<String> list = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        byte[] aas = aasFileRecords.get(i);
                        if (aas == null) {
                            list.add(null);
                        } else {
                            String aasAlphaTag = IccUtils.adnStringFieldToString(aas, 0, aas.length);
                            log("AAS[" + i + "]=" + aasAlphaTag + ",byte=" + IccUtils.bytesToHexString(aas));
                            list.add(aasAlphaTag);
                        }
                    }
                    this.mAasForAnr = list;
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 6:
                logi("Load UPB GAS done");
                AsyncResult ar6 = (AsyncResult) msg.obj;
                if (ar6.exception == null && (gasList = (String[]) ar6.result) != null && gasList.length > 0) {
                    this.mGasForGrp = new ArrayList<>();
                    for (int i2 = 0; i2 < gasList.length; i2++) {
                        String gas = decodeGas(gasList[i2]);
                        UsimGroup uGasEntry = new UsimGroup(i2 + 1, gas);
                        this.mGasForGrp.add(uGasEntry);
                        log("Load UPB GAS done i is " + i2 + ", gas is " + gas);
                    }
                }
                obj = this.mGasLock;
                synchronized (obj) {
                    this.mGasLock.notify();
                    break;
                }
                break;
            case 7:
                logi("Updating USIM IAP records done");
                if (((AsyncResult) msg.obj).exception == null) {
                    log("Updating USIM IAP records successfully!");
                    return;
                }
                return;
            case 8:
                logi("Updating USIM Email records done");
                AsyncResult ar7 = (AsyncResult) msg.obj;
                if (ar7.exception == null) {
                    log("Updating USIM Email records successfully!");
                } else {
                    Rlog.e(LOG_TAG, "EVENT_EMAIL_UPDATE_DONE exception", ar7.exception);
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 9:
                logi("Updating USIM ANR records done");
                AsyncResult ar8 = (AsyncResult) msg.obj;
                IccIoResult res = (IccIoResult) ar8.result;
                if (ar8.exception != null) {
                    Rlog.e(LOG_TAG, "EVENT_ANR_UPDATE_DONE exception", ar8.exception);
                }
                if (res != null) {
                    IccException exception = res.getException();
                    if (exception == null) {
                        log("Updating USIM ANR records successfully!");
                    }
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 10:
                logi("EVENT_AAS_UPDATE_DONE done.");
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 11:
                logi("update UPB SNE done");
                AsyncResult ar9 = (AsyncResult) msg.obj;
                if (ar9.exception != null) {
                    Rlog.e(LOG_TAG, "EVENT_SNE_UPDATE_DONE exception", ar9.exception);
                    CommandException e = (CommandException) ar9.exception;
                    if (e.getCommandError() == CommandException.Error.TEXT_STRING_TOO_LONG) {
                        this.mResult = -40;
                    } else if (e.getCommandError() == CommandException.Error.SIM_MEM_FULL) {
                        this.mResult = -30;
                    } else {
                        this.mResult = -50;
                    }
                } else {
                    this.mResult = 0;
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 12:
                logi("update UPB GRP done");
                if (((AsyncResult) msg.obj).exception == null) {
                    this.mResult = 0;
                } else {
                    this.mResult = -1;
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 13:
                logi("update UPB GAS done");
                AsyncResult ar10 = (AsyncResult) msg.obj;
                if (ar10.exception == null) {
                    this.mResult = 0;
                } else {
                    CommandException e2 = (CommandException) ar10.exception;
                    if (e2.getCommandError() == CommandException.Error.TEXT_STRING_TOO_LONG) {
                        this.mResult = -10;
                    } else if (e2.getCommandError() == CommandException.Error.SIM_MEM_FULL) {
                        this.mResult = -20;
                    } else {
                        this.mResult = -1;
                    }
                }
                logi("update UPB GAS done mResult is " + this.mResult);
                obj = this.mGasLock;
                synchronized (obj) {
                    this.mGasLock.notify();
                    break;
                }
                break;
            case 14:
                AsyncResult ar11 = (AsyncResult) msg.obj;
                int[] userData = (int[]) ar11.userObj;
                IccIoResult re = (IccIoResult) ar11.result;
                if (re != null && this.mIapFileList != null) {
                    IccException iccException = re.getException();
                    if (iccException == null) {
                        log("Loading USIM Iap record done result is " + IccUtils.bytesToHexString(re.payload));
                        try {
                            ArrayList<byte[]> iapList = this.mIapFileList.get(userData[0]);
                            if (iapList.size() > 0) {
                                iapList.set(userData[1], re.payload);
                            } else {
                                Rlog.w(LOG_TAG, "Warning: IAP size is 0");
                            }
                        } catch (IndexOutOfBoundsException e3) {
                            Rlog.e(LOG_TAG, "Index out of bounds.");
                        }
                    }
                    break;
                }
                this.mReadingIapNum.decrementAndGet();
                log("haman, mReadingIapNum when load done after minus: " + this.mReadingIapNum.get() + ",mNeedNotify " + this.mNeedNotify.get() + ", Iap pbr:" + userData[0] + ", adn i:" + userData[1]);
                if (this.mReadingIapNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                        }
                    }
                    logi("EVENT_IAP_RECORD_LOAD_DONE end mLock.notify");
                    return;
                }
                return;
            case 15:
                AsyncResult ar12 = (AsyncResult) msg.obj;
                int[] userData2 = (int[]) ar12.userObj;
                IccIoResult em = (IccIoResult) ar12.result;
                log("Loading USIM email record done email index:" + userData2[0] + ", adn i:" + userData2[1]);
                if (em != null) {
                    IccException iccException2 = em.getException();
                    if (iccException2 == null) {
                        log("Loading USIM Email record done result is " + IccUtils.bytesToHexString(em.payload));
                        updatePhoneAdnRecordWithEmailByIndex(userData2[0], userData2[1], em.payload);
                    }
                }
                this.mReadingEmailNum.decrementAndGet();
                log("haman, mReadingEmailNum when load done after minus: " + this.mReadingEmailNum.get() + ", mNeedNotify:" + this.mNeedNotify.get());
                if (this.mReadingEmailNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                        }
                    }
                    logi("EVENT_EMAIL_RECORD_LOAD_DONE end mLock.notify");
                    return;
                }
                return;
            case 16:
                AsyncResult ar13 = (AsyncResult) msg.obj;
                int[] userData3 = (int[]) ar13.userObj;
                IccIoResult result = (IccIoResult) ar13.result;
                if (result != null) {
                    IccException iccException3 = result.getException();
                    if (iccException3 == null) {
                        updatePhoneAdnRecordWithAnrByIndex(userData3[0], userData3[1], userData3[2], result.payload);
                    }
                }
                this.mReadingAnrNum.decrementAndGet();
                log("haman, mReadingAnrNum when load done after minus: " + this.mReadingAnrNum.get() + ", mNeedNotify:" + this.mNeedNotify.get());
                if (this.mReadingAnrNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                        }
                    }
                    logi("EVENT_ANR_RECORD_LOAD_DONE end mLock.notify");
                    return;
                }
                return;
            case 17:
                AsyncResult ar14 = (AsyncResult) msg.obj;
                int[] userData4 = (int[]) ar14.userObj;
                if (ar14.result != null) {
                    int[] grpIds = (int[]) ar14.result;
                    if (grpIds.length > 0) {
                        updatePhoneAdnRecordWithGrpByIndex(userData4[0], userData4[1], grpIds);
                    }
                }
                this.mReadingGrpNum.decrementAndGet();
                log("haman, mReadingGrpNum when load done after minus: " + this.mReadingGrpNum.get() + ",mNeedNotify:" + this.mNeedNotify.get());
                if (this.mReadingGrpNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                        }
                    }
                    logi("EVENT_GRP_RECORD_LOAD_DONE end mLock.notify");
                    return;
                }
                return;
            case 18:
                logi("Loading USIM SNE record done");
                AsyncResult ar15 = (AsyncResult) msg.obj;
                int[] userData5 = (int[]) ar15.userObj;
                IccIoResult r = (IccIoResult) ar15.result;
                if (r != null) {
                    IccException iccException4 = r.getException();
                    if (iccException4 == null) {
                        log("Loading USIM SNE record done result is " + IccUtils.bytesToHexString(r.payload));
                        updatePhoneAdnRecordWithSneByIndex(userData5[0], userData5[1], r.payload);
                    }
                }
                this.mReadingSneNum.decrementAndGet();
                log("haman, mReadingSneNum when load done after minus: " + this.mReadingSneNum.get() + ",mNeedNotify:" + this.mNeedNotify.get());
                if (this.mReadingSneNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                        }
                    }
                    logi("EVENT_SNE_RECORD_LOAD_DONE end mLock.notify");
                    return;
                }
                return;
            case 19:
                logi("Query UPB capability done");
                AsyncResult ar16 = (AsyncResult) msg.obj;
                if (ar16.exception == null) {
                    this.mUpbCap = (int[]) ar16.result;
                }
                obj = this.mUPBCapabilityLock;
                synchronized (obj) {
                    this.mUPBCapabilityLock.notify();
                    break;
                }
                break;
            case 20:
                AsyncResult ar17 = (AsyncResult) msg.obj;
                if (ar17.exception == null) {
                    this.efData = (EFResponseData) ar17.result;
                } else {
                    Rlog.w(LOG_TAG, "Select EF file fail" + ar17.exception);
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 21:
                logi("EVENT_QUERY_PHB_ADN_INFO");
                AsyncResult ar18 = (AsyncResult) msg.obj;
                if (ar18.exception == null) {
                    int[] info = (int[]) ar18.result;
                    if (info != null) {
                        this.mAdnRecordSize = new int[4];
                        this.mAdnRecordSize[0] = info[0];
                        this.mAdnRecordSize[1] = info[1];
                        this.mAdnRecordSize[2] = info[2];
                        this.mAdnRecordSize[3] = info[3];
                        log("recordSize[0]=" + this.mAdnRecordSize[0] + ",recordSize[1]=" + this.mAdnRecordSize[1] + ",recordSize[2]=" + this.mAdnRecordSize[2] + ",recordSize[3]=" + this.mAdnRecordSize[3]);
                    } else {
                        this.mAdnRecordSize = new int[4];
                        this.mAdnRecordSize[0] = 0;
                        this.mAdnRecordSize[1] = 0;
                        this.mAdnRecordSize[2] = 0;
                        this.mAdnRecordSize[3] = 0;
                    }
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 22:
                AsyncResult ar19 = (AsyncResult) msg.obj;
                int[] userData6 = (int[]) ar19.userObj;
                String emailResult = (String) ar19.result;
                if (emailResult != null && ar19.exception == null) {
                    log("Loading USIM Email record done result is " + emailResult);
                    updatePhoneAdnRecordWithEmailByIndexOptmz(userData6[0], userData6[1], emailResult);
                }
                this.mReadingEmailNum.decrementAndGet();
                log("haman, mReadingEmailNum when load done after minus: " + this.mReadingEmailNum.get() + ", mNeedNotify:" + this.mNeedNotify.get() + ", email index:" + userData6[0] + ", adn i:" + userData6[1]);
                if (this.mReadingEmailNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                        }
                    }
                    logi("EVENT_EMAIL_RECORD_LOAD_OPTMZ_DONE end mLock.notify");
                    return;
                }
                return;
            case 23:
                AsyncResult ar20 = (AsyncResult) msg.obj;
                int[] userData7 = (int[]) ar20.userObj;
                PhbEntry[] anrResult = (PhbEntry[]) ar20.result;
                if (anrResult != null && ar20.exception == null) {
                    log("Loading USIM Anr record done result is " + anrResult[0]);
                    updatePhoneAdnRecordWithAnrByIndexOptmz(userData7[0], userData7[1], userData7[2], anrResult[0]);
                }
                this.mReadingAnrNum.decrementAndGet();
                log("haman, mReadingAnrNum when load done after minus: " + this.mReadingAnrNum.get() + ", mNeedNotify:" + this.mNeedNotify.get() + ", anr index:" + userData7[2] + ", adn i:" + userData7[1]);
                if (this.mReadingAnrNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                        }
                    }
                    logi("EVENT_ANR_RECORD_LOAD_OPTMZ_DONE end mLock.notify");
                    return;
                }
                return;
            case 24:
                AsyncResult ar21 = (AsyncResult) msg.obj;
                int[] userData8 = (int[]) ar21.userObj;
                String sneResult = (String) ar21.result;
                if (sneResult != null && ar21.exception == null) {
                    String sneResult2 = decodeGas(sneResult);
                    log("Loading USIM Sne record done result is " + sneResult2);
                    updatePhoneAdnRecordWithSneByIndexOptmz(userData8[1], sneResult2);
                }
                this.mReadingSneNum.decrementAndGet();
                log("haman, mReadingSneNum when load done after minus: " + this.mReadingSneNum.get() + ", mNeedNotify:" + this.mNeedNotify.get() + ", sne index:" + userData8[0] + ", adn i:" + userData8[1]);
                if (this.mReadingSneNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                        }
                    }
                    logi("EVENT_SNE_RECORD_LOAD_OPTMZ_DONE end mLock.notify");
                    return;
                }
                return;
            case 25:
                AsyncResult ar22 = (AsyncResult) msg.obj;
                if (ar22.exception == null) {
                    this.mEmailInfo = (int[]) ar22.result;
                    if (this.mEmailInfo == null) {
                        log("mEmailInfo Null!");
                    } else {
                        logi("mEmailInfo = " + this.mEmailInfo[0] + " " + this.mEmailInfo[1] + " " + this.mEmailInfo[2]);
                    }
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 26:
                AsyncResult ar23 = (AsyncResult) msg.obj;
                int[] tmpAnrInfo = (int[]) ar23.result;
                if (ar23.exception == null) {
                    if (tmpAnrInfo == null) {
                        log("tmpAnrInfo Null!");
                    } else {
                        logi("tmpAnrInfo = " + tmpAnrInfo[0] + " " + tmpAnrInfo[1] + " " + tmpAnrInfo[2]);
                        if (this.mAnrInfo == null) {
                            this.mAnrInfo = new ArrayList<>();
                        }
                        this.mAnrInfo.add(tmpAnrInfo);
                    }
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 27:
                AsyncResult ar24 = (AsyncResult) msg.obj;
                if (ar24.exception == null) {
                    this.mSneInfo = (int[]) ar24.result;
                    if (this.mSneInfo == null) {
                        log("mSneInfo Null!");
                    } else {
                        logi("mSneInfo = " + this.mSneInfo[0] + " " + this.mSneInfo[1] + " " + this.mSneInfo[2]);
                    }
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 28:
                logi("Load UPB AAS done");
                AsyncResult ar25 = (AsyncResult) msg.obj;
                if (ar25.exception == null && (aasList = (String[]) ar25.result) != null && aasList.length > 0) {
                    this.mAasForAnr = new ArrayList<>();
                    for (int i3 = 0; i3 < aasList.length; i3++) {
                        String aas2 = decodeGas(aasList[i3]);
                        this.mAasForAnr.add(aas2);
                        log("Load UPB AAS done i is " + i3 + ", aas is " + aas2);
                    }
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 1000:
                AsyncResult ar26 = (AsyncResult) msg.obj;
                int efid = msg.arg1;
                if (ar26.exception == null) {
                    int[] recordSize = (int[]) ar26.result;
                    if (recordSize.length == 3) {
                        if (this.mRecordSize == null) {
                            this.mRecordSize = new SparseArray<>();
                        }
                        this.mRecordSize.put(efid, recordSize);
                    } else {
                        Rlog.e(LOG_TAG, "get wrong record size format" + ar26.exception);
                    }
                } else {
                    Rlog.e(LOG_TAG, "get EF record size failed" + ar26.exception);
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            case 1001:
                AsyncResult ar27 = (AsyncResult) msg.obj;
                int pbrIndexExt1 = msg.arg1;
                logi("EVENT_EXT1_LOAD_DONE done pbr " + pbrIndexExt1);
                if (ar27.exception == null && (record = (ArrayList) ar27.result) != null) {
                    log("EVENT_EXT1_LOAD_DONE done size " + record.size());
                    if (this.mExt1FileList == null) {
                        this.mExt1FileList = new ArrayList<>();
                    }
                    this.mExt1FileList.add(record);
                }
                obj = this.mLock;
                synchronized (obj) {
                    this.mLock.notify();
                    break;
                }
                break;
            default:
                Rlog.e(LOG_TAG, "UnRecognized Message : " + msg.what);
                return;
        }
    }

    private class PbrRecord {
        private int mAnrIndex = 0;
        private SparseArray<File> mFileIds = new SparseArray<>();
        private int mMasterFileRecordNum;

        PbrRecord(byte[] record) {
            UsimPhoneBookManager.this.logi("PBR rec: " + IccUtils.bytesToHexString(record));
            SimTlv recTlv = new SimTlv(record, 0, record.length);
            parseTag(recTlv);
        }

        void parseTag(SimTlv tlv) {
            do {
                int tag = tlv.getTag();
                switch (tag) {
                    case 168:
                    case 169:
                    case 170:
                        byte[] data = tlv.getData();
                        SimTlv tlvEfSfi = new SimTlv(data, 0, data.length);
                        parseEfAndSFI(tlvEfSfi, tag);
                        break;
                }
            } while (tlv.nextObject());
            UsimPhoneBookManager.this.mSliceCount++;
        }

        void parseEfAndSFI(SimTlv tlv, int parentTag) {
            int tagNumberWithinParentTag = 0;
            do {
                int tag = tlv.getTag();
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
                    case 201:
                    case UsimPhoneBookManager.USIM_EFEMAIL_TAG:
                    case UsimPhoneBookManager.USIM_EFCCP1_TAG:
                        int sfi = -1;
                        byte[] data = tlv.getData();
                        if (data.length < 2 || data.length > 3) {
                            Rlog.w(UsimPhoneBookManager.LOG_TAG, "Invalid TLV length: " + data.length);
                        } else {
                            if (data.length == 3) {
                                sfi = data[2] & 255;
                            }
                            int efid = ((data[0] & 255) << 8) | (data[1] & 255);
                            if (tag == 196) {
                                tag += this.mAnrIndex * 256;
                                this.mAnrIndex++;
                            }
                            File object = UsimPhoneBookManager.this.new File(parentTag, efid, sfi, tagNumberWithinParentTag);
                            object.mTag = tag;
                            object.mPbrRecord = UsimPhoneBookManager.this.mSliceCount;
                            UsimPhoneBookManager.this.logi("pbr " + object);
                            this.mFileIds.put(tag, object);
                        }
                        break;
                }
                tagNumberWithinParentTag++;
            } while (tlv.nextObject());
        }
    }

    private void queryUpbCapablityAndWait() {
        logi("queryUpbCapablityAndWait begin");
        synchronized (this.mUPBCapabilityLock) {
            for (int i = 0; i < 8; i++) {
                this.mUpbCap[i] = 0;
            }
            if (checkIsPhbReady()) {
                this.mCi.queryUPBCapability(obtainMessage(19));
                try {
                    this.mUPBCapabilityLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in queryUpbCapablityAndWait");
                }
            }
        }
        logi("queryUpbCapablityAndWait done:N_Anr is " + this.mUpbCap[0] + ", N_Email is " + this.mUpbCap[1] + ",N_Sne is " + this.mUpbCap[2] + ",N_Aas is " + this.mUpbCap[3] + ", L_Aas is " + this.mUpbCap[4] + ",N_Gas is " + this.mUpbCap[5] + ",L_Gas is " + this.mUpbCap[6] + ", N_Grp is " + this.mUpbCap[7]);
    }

    private void readGasListAndWait() {
        logi("readGasListAndWait begin");
        synchronized (this.mGasLock) {
            if (this.mUpbCap[5] <= 0) {
                log("readGasListAndWait no need to read. return");
                return;
            }
            this.mCi.readUPBGasList(1, this.mUpbCap[5], obtainMessage(6));
            try {
                this.mGasLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readGasListAndWait");
            }
        }
    }

    private void updatePhoneAdnRecordWithAnrByIndex(int recId, int adnIndex, int anrIndex, byte[] anrRecData) {
        String anr;
        ArrayList<String> aasList;
        log("updatePhoneAdnRecordWithAnrByIndex the " + adnIndex + "th anr record is " + IccUtils.bytesToHexString(anrRecData));
        int anrRecLength = anrRecData[1];
        int anrAas = anrRecData[0];
        if (anrRecLength <= 0 || anrRecLength > 11 || (anr = PhoneNumberUtils.calledPartyBCDToString(anrRecData, 2, anrRecData[1])) == null || anr.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            return;
        }
        String aas = null;
        if (anrAas > 0 && anrAas != 255 && this.mAasForAnr != null && (aasList = this.mAasForAnr) != null && anrAas <= aasList.size()) {
            aas = aasList.get(anrAas - 1);
        }
        logi(" updatePhoneAdnRecordWithAnrByIndex " + adnIndex + " th anr is " + anr + " the anrIndex is " + anrIndex);
        try {
            AdnRecord rec = this.mPhoneBookRecords.get(adnIndex);
            rec.setAnr(anr, anrIndex);
            if (aas != null && aas.length() > 0) {
                rec.setAasIndex(anrAas);
            }
            this.mPhoneBookRecords.set(adnIndex, rec);
        } catch (IndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "updatePhoneAdnRecordWithAnrByIndex: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + adnIndex);
        }
    }

    public ArrayList<UsimGroup> getUsimGroups() {
        logi("getUsimGroups ");
        synchronized (this.mGasLock) {
            if (!this.mGasForGrp.isEmpty()) {
                return this.mGasForGrp;
            }
            queryUpbCapablityAndWait();
            readGasListAndWait();
            return this.mGasForGrp;
        }
    }

    public String getUsimGroupById(int nGasId) {
        UsimGroup uGas;
        String grpName = null;
        logi("getUsimGroupById nGasId is " + nGasId);
        if (this.mGasForGrp != null && nGasId <= this.mGasForGrp.size() && (uGas = this.mGasForGrp.get(nGasId - 1)) != null) {
            grpName = uGas.getAlphaTag();
            log("getUsimGroupById index is " + uGas.getRecordIndex() + ", name is " + grpName);
        }
        logi("getUsimGroupById grpName is " + grpName);
        return grpName;
    }

    public synchronized boolean removeUsimGroupById(int nGasId) {
        boolean ret;
        ret = false;
        logi("removeUsimGroupById nGasId is " + nGasId);
        synchronized (this.mGasLock) {
            if (this.mGasForGrp == null || nGasId > this.mGasForGrp.size()) {
                Rlog.e(LOG_TAG, "removeUsimGroupById fail ");
            } else {
                UsimGroup uGas = this.mGasForGrp.get(nGasId - 1);
                if (uGas != null) {
                    log(" removeUsimGroupById index is " + uGas.getRecordIndex());
                }
                if (uGas != null && uGas.getAlphaTag() != null) {
                    this.mCi.deleteUPBEntry(4, 0, nGasId, obtainMessage(13));
                    try {
                        this.mGasLock.wait();
                    } catch (InterruptedException e) {
                        Rlog.e(LOG_TAG, "Interrupted Exception in removeUsimGroupById");
                    }
                    if (this.mResult == 0) {
                        ret = true;
                        uGas.setAlphaTag(null);
                        this.mGasForGrp.set(nGasId - 1, uGas);
                    }
                } else {
                    Rlog.w(LOG_TAG, "removeUsimGroupById fail: this gas doesn't exist ");
                }
            }
        }
        logi("removeUsimGroupById result is " + ret);
        return ret;
    }

    private String decodeGas(String srcGas) {
        log("[decodeGas] gas string is " + (srcGas == null ? "null" : srcGas));
        if (srcGas == null || srcGas.length() % 2 != 0) {
            return null;
        }
        try {
            byte[] ba = IccUtils.hexStringToBytes(srcGas);
            if (ba == null) {
                Rlog.w(LOG_TAG, "gas string is null");
                return null;
            }
            String retGas = new String(ba, 0, srcGas.length() / 2, "utf-16be");
            return retGas;
        } catch (UnsupportedEncodingException ex) {
            Rlog.e(LOG_TAG, "[decodeGas] implausible UnsupportedEncodingException", ex);
            return null;
        } catch (RuntimeException ex2) {
            Rlog.e(LOG_TAG, "[decodeGas] RuntimeException", ex2);
            return null;
        }
    }

    private String encodeToUcs2(String input) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            String hexInt = Integer.toHexString(input.charAt(i));
            for (int j = 0; j < 4 - hexInt.length(); j++) {
                output.append("0");
            }
            output.append(hexInt);
        }
        return output.toString();
    }

    public synchronized int insertUsimGroup(String grpName) {
        int index = -1;
        logi("insertUsimGroup grpName is " + grpName);
        synchronized (this.mGasLock) {
            if (this.mGasForGrp == null || this.mGasForGrp.size() == 0) {
                Rlog.w(LOG_TAG, "insertUsimGroup fail ");
            } else {
                UsimGroup gasEntry = null;
                int i = 0;
                while (true) {
                    if (i < this.mGasForGrp.size()) {
                        gasEntry = this.mGasForGrp.get(i);
                        if (gasEntry == null || gasEntry.getAlphaTag() != null) {
                            i++;
                        } else {
                            index = gasEntry.getRecordIndex();
                            log("insertUsimGroup index is " + index);
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (index < 0) {
                    Rlog.w(LOG_TAG, "insertUsimGroup fail: gas file is full.");
                    return -20;
                }
                String temp = encodeToUcs2(grpName);
                this.mCi.editUPBEntry(4, 0, index, temp, null, obtainMessage(13));
                try {
                    this.mGasLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in insertUsimGroup");
                }
                if (this.mResult < 0) {
                    Rlog.e(LOG_TAG, "result is negative. insertUsimGroup");
                    return this.mResult;
                }
                gasEntry.setAlphaTag(grpName);
                this.mGasForGrp.set(i, gasEntry);
            }
            return index;
        }
    }

    public synchronized int updateUsimGroup(int nGasId, String grpName) {
        int ret;
        logi("updateUsimGroup nGasId is " + nGasId);
        synchronized (this.mGasLock) {
            this.mResult = -1;
            if (this.mGasForGrp == null || nGasId > this.mGasForGrp.size()) {
                Rlog.w(LOG_TAG, "updateUsimGroup fail ");
            } else if (grpName != null) {
                String temp = encodeToUcs2(grpName);
                this.mCi.editUPBEntry(4, 0, nGasId, temp, null, obtainMessage(13));
                try {
                    this.mGasLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in updateUsimGroup");
                }
            }
            if (this.mResult == 0) {
                ret = nGasId;
                UsimGroup uGasEntry = this.mGasForGrp.get(nGasId - 1);
                if (uGasEntry != null) {
                    log("updateUsimGroup index is " + uGasEntry.getRecordIndex());
                    uGasEntry.setAlphaTag(grpName);
                } else {
                    Rlog.w(LOG_TAG, "updateUsimGroup the entry doesn't exist ");
                }
            } else {
                ret = this.mResult;
            }
        }
        return ret;
    }

    public boolean addContactToGroup(int adnIndex, int grpIndex) {
        boolean ret = false;
        logi("addContactToGroup adnIndex is " + adnIndex + " to grp " + grpIndex);
        if (this.mPhoneBookRecords == null || adnIndex <= 0 || adnIndex > this.mPhoneBookRecords.size()) {
            Rlog.e(LOG_TAG, "addContactToGroup no records or invalid index.");
            return false;
        }
        synchronized (this.mLock) {
            try {
                AdnRecord rec = this.mPhoneBookRecords.get(adnIndex - 1);
                if (rec != null) {
                    log(" addContactToGroup the adn index is " + rec.getRecId() + " old grpList is " + rec.getGrpIds());
                    String grpList = rec.getGrpIds();
                    boolean bExist = false;
                    int nOrder = -1;
                    int grpCount = this.mUpbCap[7];
                    int grpMaxCount = this.mUpbCap[7] > this.mUpbCap[5] ? this.mUpbCap[5] : this.mUpbCap[7];
                    int[] grpIdArray = new int[grpCount];
                    for (int i = 0; i < grpCount; i++) {
                        grpIdArray[i] = 0;
                    }
                    if (grpList != null) {
                        String[] grpIds = rec.getGrpIds().split(",");
                        int i2 = 0;
                        while (true) {
                            if (i2 >= grpMaxCount) {
                                break;
                            }
                            grpIdArray[i2] = Integer.parseInt(grpIds[i2]);
                            if (grpIndex == grpIdArray[i2]) {
                                bExist = true;
                                log(" addContactToGroup the adn is already in the group. i is " + i2);
                                break;
                            }
                            if (nOrder < 0 && (grpIdArray[i2] == 0 || grpIdArray[i2] == 255)) {
                                nOrder = i2;
                                log(" addContactToGroup found an unsed position in the group list. i is " + i2);
                            }
                            i2++;
                        }
                    } else {
                        nOrder = 0;
                    }
                    if (!bExist && nOrder >= 0) {
                        grpIdArray[nOrder] = grpIndex;
                        this.mCi.writeUPBGrpEntry(adnIndex, grpIdArray, obtainMessage(12));
                        try {
                            this.mLock.wait();
                        } catch (InterruptedException e) {
                            Rlog.e(LOG_TAG, "Interrupted Exception in addContactToGroup");
                        }
                        if (this.mResult == 0) {
                            ret = true;
                            updatePhoneAdnRecordWithGrpByIndex(adnIndex - 1, adnIndex, grpIdArray);
                            logi(" addContactToGroup the adn index is " + rec.getRecId());
                            this.mResult = -1;
                        }
                    }
                }
            } catch (IndexOutOfBoundsException e2) {
                Rlog.e(LOG_TAG, "addContactToGroup: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + (adnIndex - 1));
                return false;
            }
        }
        return ret;
    }

    public synchronized boolean removeContactFromGroup(int adnIndex, int grpIndex) {
        boolean ret = false;
        logi("removeContactFromGroup adnIndex is " + adnIndex + " to grp " + grpIndex);
        if (this.mPhoneBookRecords == null || adnIndex <= 0 || adnIndex > this.mPhoneBookRecords.size()) {
            Rlog.e(LOG_TAG, "removeContactFromGroup no records or invalid index.");
            return false;
        }
        synchronized (this.mLock) {
            try {
                AdnRecord rec = this.mPhoneBookRecords.get(adnIndex - 1);
                if (rec != null) {
                    String grpList = rec.getGrpIds();
                    if (grpList == null) {
                        Rlog.e(LOG_TAG, " the adn is not in any group. ");
                        return false;
                    }
                    String[] grpIds = grpList.split(",");
                    boolean bExist = false;
                    int nOrder = -1;
                    int[] grpIdArray = new int[grpIds.length];
                    for (int i = 0; i < grpIds.length; i++) {
                        grpIdArray[i] = Integer.parseInt(grpIds[i]);
                        if (grpIndex == grpIdArray[i]) {
                            bExist = true;
                            nOrder = i;
                            log(" removeContactFromGroup the adn is in the group. i is " + i);
                        }
                    }
                    if (!bExist || nOrder < 0) {
                        Rlog.e(LOG_TAG, " removeContactFromGroup the adn is not in the group. ");
                    } else {
                        grpIdArray[nOrder] = 0;
                        this.mCi.writeUPBGrpEntry(adnIndex, grpIdArray, obtainMessage(12));
                        try {
                            this.mLock.wait();
                        } catch (InterruptedException e) {
                            Rlog.e(LOG_TAG, "Interrupted Exception in removeContactFromGroup");
                        }
                        if (this.mResult == 0) {
                            ret = true;
                            updatePhoneAdnRecordWithGrpByIndex(adnIndex - 1, adnIndex, grpIdArray);
                            this.mResult = -1;
                        }
                    }
                }
                return ret;
            } catch (IndexOutOfBoundsException e2) {
                Rlog.e(LOG_TAG, "removeContactFromGroup: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + (adnIndex - 1));
                return false;
            }
        }
    }

    public boolean updateContactToGroups(int adnIndex, int[] grpIdList) {
        boolean ret = false;
        if (this.mPhoneBookRecords == null || adnIndex <= 0 || adnIndex > this.mPhoneBookRecords.size() || grpIdList == null) {
            Rlog.e(LOG_TAG, "updateContactToGroups no records or invalid index.");
            return false;
        }
        logi("updateContactToGroups grpIdList is " + adnIndex + " to grp list count " + grpIdList.length);
        synchronized (this.mLock) {
            AdnRecord rec = this.mPhoneBookRecords.get(adnIndex - 1);
            if (rec != null) {
                log(" updateContactToGroups the adn index is " + rec.getRecId() + " old grpList is " + rec.getGrpIds());
                int grpCount = this.mUpbCap[7];
                if (grpIdList.length > grpCount) {
                    Rlog.e(LOG_TAG, "updateContactToGroups length of grpIdList > grpCount.");
                    return false;
                }
                int[] grpIdArray = new int[grpCount];
                int i = 0;
                while (i < grpCount) {
                    grpIdArray[i] = i < grpIdList.length ? grpIdList[i] : 0;
                    log("updateContactToGroups i:" + i + ",grpIdArray[" + i + "]:" + grpIdArray[i]);
                    i++;
                }
                this.mCi.writeUPBGrpEntry(adnIndex, grpIdArray, obtainMessage(12));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in updateContactToGroups");
                }
                if (this.mResult == 0) {
                    ret = true;
                    updatePhoneAdnRecordWithGrpByIndex(adnIndex - 1, adnIndex, grpIdArray);
                    logi(" updateContactToGroups the adn index is " + rec.getRecId());
                    this.mResult = -1;
                }
            }
            return ret;
        }
    }

    public boolean moveContactFromGroupsToGroups(int adnIndex, int[] fromGrpIdList, int[] toGrpIdList) {
        boolean ret = false;
        if (this.mPhoneBookRecords == null || adnIndex <= 0 || adnIndex > this.mPhoneBookRecords.size()) {
            Rlog.e(LOG_TAG, "moveContactFromGroupsToGroups no records or invalid index.");
            return false;
        }
        synchronized (this.mLock) {
            AdnRecord rec = this.mPhoneBookRecords.get(adnIndex - 1);
            if (rec != null) {
                int grpCount = this.mUpbCap[7];
                int grpMaxCount = this.mUpbCap[7] > this.mUpbCap[5] ? this.mUpbCap[5] : this.mUpbCap[7];
                String grpIds = rec.getGrpIds();
                logi(" moveContactFromGroupsToGroups the adn index is " + rec.getRecId() + " original grpIds is " + grpIds + ", fromGrpIdList: " + (fromGrpIdList == null ? "null" : fromGrpIdList) + ", toGrpIdList: " + (toGrpIdList == null ? "null" : toGrpIdList));
                int[] grpIdIntArray = new int[grpCount];
                for (int i = 0; i < grpCount; i++) {
                    grpIdIntArray[i] = 0;
                }
                if (grpIds != null) {
                    String[] grpIdStrArray = grpIds.split(",");
                    for (int i2 = 0; i2 < grpMaxCount; i2++) {
                        grpIdIntArray[i2] = Integer.parseInt(grpIdStrArray[i2]);
                    }
                }
                if (fromGrpIdList != null) {
                    for (int i3 : fromGrpIdList) {
                        for (int j = 0; j < grpMaxCount; j++) {
                            if (grpIdIntArray[j] == i3) {
                                grpIdIntArray[j] = 0;
                            }
                        }
                    }
                }
                if (toGrpIdList != null) {
                    for (int i4 = 0; i4 < toGrpIdList.length; i4++) {
                        boolean bEmpty = false;
                        boolean bExist = false;
                        int k = 0;
                        while (true) {
                            if (k >= grpMaxCount) {
                                break;
                            }
                            if (grpIdIntArray[k] == toGrpIdList[i4]) {
                                bExist = true;
                                break;
                            }
                            k++;
                        }
                        if (bExist) {
                            Rlog.w(LOG_TAG, "moveContactFromGroupsToGroups the adn isalready in the group.");
                        } else {
                            for (int j2 = 0; j2 < grpMaxCount; j2++) {
                                if (grpIdIntArray[j2] == 0 || grpIdIntArray[j2] == 255) {
                                    bEmpty = true;
                                    grpIdIntArray[j2] = toGrpIdList[i4];
                                    break;
                                }
                            }
                            if (!bEmpty) {
                                Rlog.e(LOG_TAG, "moveContactFromGroupsToGroups no empty to add.");
                                return false;
                            }
                        }
                    }
                }
                this.mCi.writeUPBGrpEntry(adnIndex, grpIdIntArray, obtainMessage(12));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in moveContactFromGroupsToGroups");
                }
                if (this.mResult == 0) {
                    ret = true;
                    updatePhoneAdnRecordWithGrpByIndex(adnIndex - 1, adnIndex, grpIdIntArray);
                    logi("moveContactFromGroupsToGroups the adn index is " + rec.getRecId());
                    this.mResult = -1;
                }
            }
            return ret;
        }
    }

    public boolean removeContactGroup(int adnIndex) {
        boolean ret = false;
        logi("removeContactsGroup adnIndex is " + adnIndex);
        if (this.mPhoneBookRecords == null || this.mPhoneBookRecords.isEmpty()) {
            return false;
        }
        synchronized (this.mLock) {
            try {
                AdnRecord rec = this.mPhoneBookRecords.get(adnIndex - 1);
                if (rec == null) {
                    return false;
                }
                log("removeContactsGroup rec is " + rec);
                String grpList = rec.getGrpIds();
                if (grpList == null) {
                    return false;
                }
                String[] grpIds = grpList.split(",");
                boolean hasGroup = false;
                int i = 0;
                while (true) {
                    if (i >= grpIds.length) {
                        break;
                    }
                    int value = Integer.parseInt(grpIds[i]);
                    if (value > 0 && value < 255) {
                        hasGroup = true;
                        break;
                    }
                    i++;
                }
                if (hasGroup) {
                    this.mCi.writeUPBGrpEntry(adnIndex, new int[0], obtainMessage(12));
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                        Rlog.e(LOG_TAG, "Interrupted Exception in removeContactGroup");
                    }
                    if (this.mResult == 0) {
                        ret = true;
                        int[] grpIdArray = new int[grpIds.length];
                        for (int i2 = 0; i2 < grpIds.length; i2++) {
                            grpIdArray[i2] = 0;
                        }
                        updatePhoneAdnRecordWithGrpByIndex(adnIndex - 1, adnIndex, grpIdArray);
                        logi(" removeContactGroup the adn index is " + rec.getRecId());
                        this.mResult = -1;
                    }
                }
                return ret;
            } catch (IndexOutOfBoundsException e2) {
                Rlog.e(LOG_TAG, "removeContactGroup: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + (adnIndex - 1));
                return false;
            }
        }
    }

    public int hasExistGroup(String grpName) {
        int grpId = -1;
        logi("hasExistGroup grpName is " + grpName);
        if (grpName == null) {
            return -1;
        }
        if (this.mGasForGrp != null && this.mGasForGrp.size() > 0) {
            int i = 0;
            while (true) {
                if (i < this.mGasForGrp.size()) {
                    UsimGroup uGas = this.mGasForGrp.get(i);
                    if (uGas == null || !grpName.equals(uGas.getAlphaTag())) {
                        i++;
                    } else {
                        log("getUsimGroupById index is " + uGas.getRecordIndex() + ", name is " + grpName);
                        grpId = uGas.getRecordIndex();
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        logi("hasExistGroup grpId is " + grpId);
        return grpId;
    }

    public int getUsimGrpMaxNameLen() {
        int ret;
        logi("getUsimGrpMaxNameLen begin");
        synchronized (this.mUPBCapabilityLock) {
            if (checkIsPhbReady()) {
                if (this.mUpbCap[6] <= 0) {
                    queryUpbCapablityAndWait();
                }
                ret = this.mUpbCap[6];
            } else {
                ret = 0;
            }
            logi("getUsimGrpMaxNameLen done: L_Gas is " + ret);
        }
        return ret;
    }

    public int getUsimGrpMaxCount() {
        int ret;
        logi("getUsimGrpMaxCount begin");
        synchronized (this.mUPBCapabilityLock) {
            if (checkIsPhbReady()) {
                if (this.mUpbCap[5] <= 0) {
                    queryUpbCapablityAndWait();
                }
                ret = this.mUpbCap[5];
            } else {
                ret = 0;
            }
            logi("getUsimGrpMaxCount done: N_Gas is " + ret);
        }
        return ret;
    }

    private void log(String msg) {
        if (DBG) {
            Rlog.d(LOG_TAG, msg + "(slot " + this.mSlotId + ")");
        }
    }

    private void logi(String msg) {
        Rlog.i(LOG_TAG, msg + "(slot " + this.mSlotId + ")");
    }

    public boolean isAnrCapacityFree(String anr, int adnIndex, int anrIndex) {
        if (anr == null || anr.equals(UsimPBMemInfo.STRING_NOT_SET) || anrIndex < 0) {
            return true;
        }
        if (this.mFh instanceof CsimFileHandler) {
            int pbrRecNum = (adnIndex - 1) / this.mAdnFileSize;
            int anrRecNum = (adnIndex - 1) % this.mAdnFileSize;
            try {
                log("isAnrCapacityFree anr: " + anr);
                if (this.mRecordSize == null || this.mRecordSize.size() == 0) {
                    log("isAnrCapacityFree: mAnrFileSize is empty");
                    return false;
                }
                File anrFile = (File) this.mPbrRecords.get(pbrRecNum).mFileIds.get((anrIndex * 256) + 196);
                if (anrFile == null) {
                    return false;
                }
                int anrFileId = anrFile.getEfid();
                int[] sizeInfo = this.mRecordSize.get(anrFileId);
                int size = sizeInfo[2];
                log("isAnrCapacityFree size: " + size);
                if (size < anrRecNum + 1) {
                    log("isAnrCapacityFree: anrRecNum out of size: " + anrRecNum);
                    return false;
                }
                return true;
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "isAnrCapacityFree Index out of bounds.");
                return false;
            } catch (NullPointerException e2) {
                Rlog.e(LOG_TAG, "isAnrCapacityFree exception:" + e2.toString());
                return false;
            }
        }
        synchronized (this.mLock) {
            if (this.mAnrInfo == null || anrIndex >= this.mAnrInfo.size()) {
                this.mCi.queryUPBAvailable(0, anrIndex + 1, obtainMessage(26));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e3) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in isAnrCapacityFree");
                }
            }
        }
        if (this.mAnrInfo != null && this.mAnrInfo.get(anrIndex) != null && this.mAnrInfo.get(anrIndex)[1] > 0) {
            return true;
        }
        return false;
    }

    public void updateAnrByAdnIndex(String anr, int adnIndex, int anrIndex) {
        int pbrRecNum = (adnIndex - 1) / this.mAdnFileSize;
        int anrRecNum = (adnIndex - 1) % this.mAdnFileSize;
        if (this.mPbrRecords == null) {
            return;
        }
        SparseArray<File> fileIds = this.mPbrRecords.get(pbrRecNum).mFileIds;
        if (fileIds == null) {
            log("updateAnrByAdnIndex: No anr tag in pbr record 0");
            return;
        }
        if (this.mPhoneBookRecords == null || this.mPhoneBookRecords.isEmpty()) {
            Rlog.w(LOG_TAG, "updateAnrByAdnIndex: mPhoneBookRecords is empty");
            return;
        }
        File anrFile = fileIds.get((anrIndex * 256) + 196);
        if (anrFile == null) {
            log("updateAnrByAdnIndex no efFile anrIndex: " + anrIndex);
            return;
        }
        log("updateAnrByAdnIndex effile " + anrFile);
        if (this.mFh instanceof CsimFileHandler) {
            int efid = anrFile.getEfid();
            log("updateAnrByAdnIndex recId: " + pbrRecNum + " EF_ANR id is " + Integer.toHexString(efid).toUpperCase());
            if (anrFile.getParentTag() == 169) {
                updateType2Anr(anr, adnIndex, anrFile);
                return;
            }
            try {
                AdnRecord rec = this.mPhoneBookRecords.get(adnIndex - 1);
                int aas = rec.getAasIndex();
                byte[] data = buildAnrRecord(anr, this.mAnrRecordSize, aas);
                if (data != null) {
                    this.mFh.updateEFLinearFixed(efid, anrRecNum + 1, data, null, obtainMessage(9));
                    return;
                }
                return;
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "updateAnrByAdnIndex: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + (adnIndex - 1));
                return;
            }
        }
        try {
            AdnRecord rec2 = this.mPhoneBookRecords.get(adnIndex - 1);
            int aas2 = rec2.getAasIndex();
            Message msg = obtainMessage(9);
            synchronized (this.mLock) {
                if (anr != null) {
                    if (anr.length() == 0) {
                        this.mCi.deleteUPBEntry(0, anrIndex + 1, adnIndex, msg);
                    } else {
                        String[] param = buildAnrRecordOptmz(anr, aas2);
                        this.mCi.editUPBEntry(0, anrIndex + 1, adnIndex, param[0], param[1], param[2], msg);
                    }
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e2) {
                        Rlog.e(LOG_TAG, "Interrupted Exception in updateAnrByAdnIndexOptmz");
                    }
                }
            }
        } catch (IndexOutOfBoundsException e3) {
            Rlog.e(LOG_TAG, "updateAnrByAdnIndexOptmz: mPhoneBookRecords IndexOutOfBoundsException size() is " + this.mPhoneBookRecords.size() + "index is " + (adnIndex - 1));
        }
    }

    private int getEmailRecNum(String[] emails, int pbrRecNum, int nIapRecNum, byte[] bArr, int tagNum) {
        boolean hasEmail = false;
        int emailType2Index = ((File) this.mPbrRecords.get(pbrRecNum).mFileIds.get(USIM_EFEMAIL_TAG)).getIndex();
        if (emails == null) {
            if (bArr[emailType2Index] != 255 && bArr[emailType2Index] > 0) {
                this.mEmailRecTable[bArr[emailType2Index] - 1] = 0;
            }
            return -1;
        }
        int i = 0;
        while (true) {
            if (i < emails.length) {
                if (emails[i] == null || emails[i].equals(UsimPBMemInfo.STRING_NOT_SET)) {
                    i++;
                } else {
                    hasEmail = true;
                    break;
                }
            } else {
                break;
            }
        }
        if (!hasEmail) {
            if (bArr[emailType2Index] != 255 && bArr[emailType2Index] > 0) {
                this.mEmailRecTable[bArr[emailType2Index] - 1] = 0;
            }
            return -1;
        }
        int i2 = bArr[tagNum];
        log("getEmailRecNum recNum:" + i2);
        if (i2 > this.mEmailFileSize || i2 >= 255 || i2 <= 0) {
            int nOffset = pbrRecNum * this.mEmailFileSize;
            int i3 = nOffset;
            while (true) {
                if (i3 >= this.mEmailFileSize + nOffset) {
                    break;
                }
                log("updateEmailsByAdnIndex: mEmailRecTable[" + i3 + "] is " + this.mEmailRecTable[i3]);
                if (this.mEmailRecTable[i3] != 0) {
                    i3++;
                } else {
                    i2 = (i3 + 1) - nOffset;
                    this.mEmailRecTable[i3] = nIapRecNum;
                    break;
                }
            }
        }
        if (i2 > this.mEmailFileSize) {
            i2 = 255;
        }
        if (i2 == -1) {
            return -2;
        }
        return i2;
    }

    public boolean checkEmailCapacityFree(int adnIndex, String[] emails) {
        boolean hasEmail = false;
        if (emails == null) {
            return true;
        }
        int i = 0;
        while (true) {
            if (i >= emails.length) {
                break;
            }
            if (emails[i] == null || emails[i].equals(UsimPBMemInfo.STRING_NOT_SET)) {
                i++;
            } else {
                hasEmail = true;
                break;
            }
        }
        if (!hasEmail) {
            return true;
        }
        if (this.mFh instanceof CsimFileHandler) {
            int pbrRecNum = (adnIndex - 1) / this.mAdnFileSize;
            int nOffset = pbrRecNum * this.mEmailFileSize;
            for (int i2 = nOffset; i2 < this.mEmailFileSize + nOffset; i2++) {
                if (this.mEmailRecTable[i2] == 0) {
                    return true;
                }
            }
            return false;
        }
        synchronized (this.mLock) {
            if (this.mEmailInfo == null) {
                this.mCi.queryUPBAvailable(1, 1, obtainMessage(25));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in checkEmailCapacityFreeOptmz");
                }
            }
        }
        return this.mEmailInfo != null && this.mEmailInfo[1] > 0;
    }

    public boolean checkSneCapacityFree(int adnIndex, String sne) {
        if (sne == null || sne.equals(UsimPBMemInfo.STRING_NOT_SET) || (this.mFh instanceof CsimFileHandler)) {
            return true;
        }
        synchronized (this.mLock) {
            if (this.mSneInfo == null) {
                this.mCi.queryUPBAvailable(2, 1, obtainMessage(27));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in checkSneCapacityFree");
                }
            }
        }
        return this.mSneInfo != null && this.mSneInfo[1] > 0;
    }

    public boolean checkEmailLength(String[] emails) {
        SparseArray<File> files;
        File emailFile;
        if (emails == null || emails[0] == null || this.mPbrRecords == null || (files = this.mPbrRecords.get(0).mFileIds) == null || (emailFile = files.get(USIM_EFEMAIL_TAG)) == null) {
            return true;
        }
        boolean emailType2 = emailFile.getParentTag() == 169;
        int maxDataLength = (this.mEmailRecordSize == -1 || !emailType2) ? this.mEmailRecordSize : this.mEmailRecordSize - 2;
        byte[] eMailData = GsmAlphabet.stringToGsm8BitPacked(emails[0]);
        logi("checkEmailLength eMailData.length=" + eMailData.length + ", maxDataLength=" + maxDataLength);
        return maxDataLength == -1 || eMailData.length <= maxDataLength;
    }

    public int updateEmailsByAdnIndex(String[] emails, int adnIndex) {
        SparseArray<File> files;
        int pbrRecNum = (adnIndex - 1) / this.mAdnFileSize;
        int adnRecNum = (adnIndex - 1) % this.mAdnFileSize;
        if (this.mPbrRecords == null || (files = this.mPbrRecords.get(pbrRecNum).mFileIds) == null || files.size() == 0 || this.mPhoneBookRecords == null || this.mPhoneBookRecords.isEmpty()) {
            return 0;
        }
        File efFile = files.get(USIM_EFEMAIL_TAG);
        if (efFile == null) {
            log("updateEmailsByAdnIndex: No email tag in pbr record 0");
            return 0;
        }
        int efid = efFile.getEfid();
        boolean emailType2 = efFile.getParentTag() == 169;
        efFile.getIndex();
        logi("updateEmailsByAdnIndex: pbrrecNum is " + pbrRecNum + " EF_EMAIL id is " + Integer.toHexString(efid).toUpperCase());
        if (this.mFh instanceof CsimFileHandler) {
            if (emailType2 && this.mIapFileList != null) {
                return updateType2Email(emails, adnIndex, efFile);
            }
            log("updateEmailsByAdnIndex file: " + efFile);
            String str = (emails == null || emails.length <= 0) ? null : emails[0];
            if (this.mEmailRecordSize <= 0) {
                return -50;
            }
            byte[] data = buildEmailRecord(str, adnIndex, this.mEmailRecordSize, emailType2);
            log("updateEmailsByAdnIndex build type1 email record:" + IccUtils.bytesToHexString(data));
            if (data == null) {
                return -40;
            }
            this.mFh.updateEFLinearFixed(efid, adnRecNum + 1, data, null, obtainMessage(8));
            return 0;
        }
        Message msg = obtainMessage(8);
        synchronized (this.mLock) {
            if (emails != null) {
                if (emails.length == 0) {
                    this.mCi.deleteUPBEntry(1, 1, adnIndex, msg);
                } else {
                    String temp = encodeToUcs2(emails[0]);
                    this.mCi.editUPBEntry(1, 1, adnIndex, temp, null, msg);
                }
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in updateEmailsByAdnIndex");
                }
            }
        }
        return 0;
    }

    private int updateType2Email(String[] emails, int adnIndex, File emailFile) {
        int pbrRecNum = (adnIndex - 1) / this.mAdnFileSize;
        int adnRecNum = (adnIndex - 1) % this.mAdnFileSize;
        int emailType2Index = emailFile.getIndex();
        emailFile.getEfid();
        try {
            ArrayList<byte[]> iapFile = this.mIapFileList.get(pbrRecNum);
            if (iapFile.size() > 0) {
                byte[] iapRec = iapFile.get(adnRecNum);
                int recNum = getEmailRecNum(emails, pbrRecNum, adnRecNum + 1, iapRec, emailType2Index);
                log("updateEmailsByAdnIndex: Email recNum is " + recNum);
                if (-2 == recNum) {
                    return -30;
                }
                log("updateEmailsByAdnIndex: found Email recNum is " + recNum);
                iapRec[emailType2Index] = (byte) recNum;
                SparseArray<File> files = this.mPbrRecords.get(pbrRecNum).mFileIds;
                if (files.get(193) != null) {
                    int efid = files.get(193).getEfid();
                    this.mFh.updateEFLinearFixed(efid, adnRecNum + 1, iapRec, null, obtainMessage(7));
                    if (recNum != 255 && recNum != -1) {
                        String eMailAd = null;
                        if (emails != null) {
                            try {
                                eMailAd = emails[0];
                            } catch (IndexOutOfBoundsException e) {
                                Rlog.e(LOG_TAG, "Error: updateEmailsByAdnIndex no email address, continuing");
                            }
                            if (this.mEmailRecordSize <= 0) {
                                return -50;
                            }
                            byte[] eMailRecData = buildEmailRecord(eMailAd, adnIndex, this.mEmailRecordSize, true);
                            if (eMailRecData == null) {
                                return -40;
                            }
                            int efid2 = files.get(USIM_EFEMAIL_TAG).getEfid();
                            this.mFh.updateEFLinearFixed(efid2, recNum, eMailRecData, null, obtainMessage(8));
                            return 0;
                        }
                        return 0;
                    }
                    return 0;
                }
                Rlog.e(LOG_TAG, "updateEmailsByAdnIndex Error: No IAP file!");
                return -50;
            }
            Rlog.w(LOG_TAG, "Warning: IAP size is 0");
            return -50;
        } catch (IndexOutOfBoundsException e2) {
            Rlog.e(LOG_TAG, "Index out of bounds.");
            return -50;
        }
    }

    private byte[] buildAnrRecord(String anr, int recordSize, int aas) {
        log("buildAnrRecord anr:" + anr + ",recordSize:" + recordSize + ",aas:" + aas);
        if (recordSize <= 0) {
            readAnrRecordSize();
        }
        log("buildAnrRecord recordSize:" + recordSize);
        byte[] anrString = new byte[recordSize];
        for (int i = 0; i < recordSize; i++) {
            anrString[i] = -1;
        }
        String updatedAnr = PhoneNumberUtils.convertPreDial(anr);
        if (TextUtils.isEmpty(updatedAnr)) {
            Rlog.w(LOG_TAG, "[buildAnrRecord] Empty dialing number");
            return anrString;
        }
        if (updatedAnr.length() > 20) {
            Rlog.w(LOG_TAG, "[buildAnrRecord] Max length of dialing number is 20");
            return null;
        }
        byte[] bcdNumber = PhoneNumberUtils.numberToCalledPartyBCD(updatedAnr);
        if (bcdNumber != null) {
            anrString[0] = (byte) aas;
            System.arraycopy(bcdNumber, 0, anrString, 2, bcdNumber.length);
            anrString[1] = (byte) bcdNumber.length;
        }
        return anrString;
    }

    private byte[] buildEmailRecord(String strEmail, int adnIndex, int recordSize, boolean emailType2) {
        byte[] eMailRecData = new byte[recordSize];
        for (int i = 0; i < recordSize; i++) {
            eMailRecData[i] = -1;
        }
        if (strEmail != null && !strEmail.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            byte[] eMailData = GsmAlphabet.stringToGsm8BitPacked(strEmail);
            int maxDataLength = (this.mEmailRecordSize != -1 && emailType2) ? eMailRecData.length - 2 : eMailRecData.length;
            log("buildEmailRecord eMailData.length=" + eMailData.length + ", maxDataLength=" + maxDataLength);
            if (eMailData.length > maxDataLength) {
                return null;
            }
            System.arraycopy(eMailData, 0, eMailRecData, 0, eMailData.length);
            log("buildEmailRecord eMailData=" + IccUtils.bytesToHexString(eMailData) + ", eMailRecData=" + IccUtils.bytesToHexString(eMailRecData));
            if (emailType2 && this.mPbrRecords != null) {
                int pbrIndex = (adnIndex - 1) / this.mAdnFileSize;
                int adnRecId = (adnIndex % this.mAdnFileSize) & 255;
                SparseArray<File> files = this.mPbrRecords.get(pbrIndex).mFileIds;
                File adnFile = files.get(192);
                eMailRecData[recordSize - 2] = (byte) adnFile.getSfi();
                eMailRecData[recordSize - 1] = (byte) adnRecId;
                log("buildEmailRecord x+1=" + adnFile.getSfi() + ", x+2=" + adnRecId);
            }
        }
        return eMailRecData;
    }

    public void updateUsimPhonebookRecordsList(int index, AdnRecord newAdn) {
        logi("updateUsimPhonebookRecordsList update the " + index + "th record.");
        if (index >= this.mPhoneBookRecords.size()) {
            return;
        }
        AdnRecord oldAdn = this.mPhoneBookRecords.get(index);
        if (oldAdn != null && oldAdn.getGrpIds() != null) {
            newAdn.setGrpIds(oldAdn.getGrpIds());
        }
        this.mPhoneBookRecords.set(index, newAdn);
    }

    private void updatePhoneAdnRecordWithGrpByIndex(int recIndex, int adnIndex, int[] grpIds) {
        int grpSize;
        log("updatePhoneAdnRecordWithGrpByIndex the " + recIndex + "th grp ");
        if (recIndex > this.mPhoneBookRecords.size() || (grpSize = grpIds.length) <= 0) {
            return;
        }
        try {
            AdnRecord rec = this.mPhoneBookRecords.get(recIndex);
            log("updatePhoneAdnRecordWithGrpByIndex the adnIndex is " + adnIndex + "; the original index is " + rec.getRecId());
            StringBuilder grpIdsSb = new StringBuilder();
            for (int i = 0; i < grpSize - 1; i++) {
                grpIdsSb.append(grpIds[i]);
                grpIdsSb.append(",");
            }
            grpIdsSb.append(grpIds[grpSize - 1]);
            rec.setGrpIds(grpIdsSb.toString());
            log("updatePhoneAdnRecordWithGrpByIndex grpIds is " + grpIdsSb.toString());
            this.mPhoneBookRecords.set(recIndex, rec);
            log("updatePhoneAdnRecordWithGrpByIndex the rec:" + rec);
        } catch (IndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "updatePhoneAdnRecordWithGrpByIndex: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + recIndex);
        }
    }

    private void readType1Ef(File file, int anrIndex) {
        int[] size;
        log("readType1Ef:" + file);
        if (file.getParentTag() != 168) {
            return;
        }
        int pbrIndex = file.mPbrRecord;
        int numAdnRecs = this.mPhoneBookRecords.size();
        int nOffset = pbrIndex * this.mAdnFileSize;
        int nMax = nOffset + this.mAdnFileSize;
        if (numAdnRecs < nMax) {
            nMax = numAdnRecs;
        }
        if (this.mRecordSize != null && this.mRecordSize.get(file.getEfid()) != null) {
            int[] size2 = this.mRecordSize.get(file.getEfid());
            size = size2;
        } else {
            size = readEFLinearRecordSize(file.getEfid());
        }
        if (size == null || size.length != 3) {
            Rlog.e(LOG_TAG, "readType1Ef: read record size error.");
            return;
        }
        int recordSize = size[0];
        int tag = file.mTag % 256;
        int i = file.mTag / 256;
        log("readType1Ef: RecordSize = " + recordSize);
        if (tag == USIM_EFEMAIL_TAG) {
            for (int i2 = nOffset; i2 < this.mEmailFileSize + nOffset; i2++) {
                try {
                    this.mEmailRecTable[i2] = 0;
                } catch (ArrayIndexOutOfBoundsException e) {
                    Rlog.e(LOG_TAG, "init RecTable error " + e.getMessage());
                }
            }
        }
        if (recordSize == 0) {
            Rlog.w(LOG_TAG, "readType1Ef: recordSize is 0. ");
            return;
        }
        int totalReadingNum = 0;
        for (int i3 = nOffset; i3 < nMax; i3++) {
            try {
                AdnRecord rec = this.mPhoneBookRecords.get(i3);
                if (rec.getAlphaTag().length() > 0 || rec.getNumber().length() > 0) {
                    int[] data = {file.mPbrRecord, i3, anrIndex};
                    int loadWhat = 0;
                    switch (tag) {
                        case 195:
                            loadWhat = 18;
                            this.mReadingSneNum.incrementAndGet();
                            break;
                        case 196:
                            loadWhat = 16;
                            this.mReadingAnrNum.addAndGet(1);
                            break;
                        case USIM_EFEMAIL_TAG:
                            data[0] = ((i3 + 1) - nOffset) + (this.mEmailFileSize * nOffset);
                            loadWhat = 15;
                            this.mReadingEmailNum.incrementAndGet();
                            break;
                        default:
                            Rlog.e(LOG_TAG, "not support tag " + file.mTag);
                            break;
                    }
                    this.mFh.readEFLinearFixed(file.getEfid(), (i3 + 1) - nOffset, recordSize, obtainMessage(loadWhat, data));
                    totalReadingNum++;
                }
            } catch (IndexOutOfBoundsException e2) {
                Rlog.e(LOG_TAG, "readType1Ef: mPhoneBookRecords IndexOutOfBoundsException numAdnRecs is " + numAdnRecs + "index is " + i3);
            }
        }
        switch (tag) {
            case 195:
                if (this.mReadingSneNum.get() == 0) {
                    this.mNeedNotify.set(false);
                    return;
                }
                this.mNeedNotify.set(true);
                break;
            case 196:
                if (this.mReadingAnrNum.get() == 0) {
                    this.mNeedNotify.set(false);
                    return;
                }
                this.mNeedNotify.set(true);
                break;
            case USIM_EFEMAIL_TAG:
                if (this.mReadingEmailNum.get() == 0) {
                    this.mNeedNotify.set(false);
                    return;
                }
                this.mNeedNotify.set(true);
                break;
            default:
                Rlog.e(LOG_TAG, "not support tag " + Integer.toHexString(file.mTag).toUpperCase());
                break;
        }
        logi("readType1Ef before mLock.wait " + this.mNeedNotify.get() + " total:" + totalReadingNum);
        synchronized (this.mLock) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e3) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readType1Ef");
            }
        }
        logi("readType1Ef after mLock.wait " + this.mNeedNotify.get());
    }

    private void readType2Ef(File file) {
        int[] size;
        log("readType2Ef:" + file);
        if (file.getParentTag() != 169) {
            return;
        }
        int recId = file.mPbrRecord;
        SparseArray<File> files = this.mPbrRecords.get(file.mPbrRecord).mFileIds;
        if (files == null) {
            Rlog.e(LOG_TAG, "Error: no fileIds");
            return;
        }
        File iapFile = files.get(193);
        if (iapFile == null) {
            Rlog.e(LOG_TAG, "Can't locate EF_IAP in EF_PBR.");
            return;
        }
        readIapFileAndWait(recId, iapFile.getEfid(), false);
        if (this.mIapFileList == null || this.mIapFileList.size() <= recId || this.mIapFileList.get(recId).size() == 0) {
            Rlog.e(LOG_TAG, "Error: IAP file is empty");
            return;
        }
        int numAdnRecs = this.mPhoneBookRecords.size();
        int nOffset = recId * this.mAdnFileSize;
        int nMax = nOffset + this.mAdnFileSize;
        if (numAdnRecs < nMax) {
            nMax = numAdnRecs;
        }
        switch (file.mTag) {
            case 195:
            case 196:
                break;
            case USIM_EFEMAIL_TAG:
                for (int i = nOffset; i < this.mEmailFileSize + nOffset; i++) {
                    try {
                        this.mEmailRecTable[i] = 0;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        Rlog.e(LOG_TAG, "init RecTable error " + e.getMessage());
                    }
                    break;
                }
                break;
            default:
                Rlog.e(LOG_TAG, "no implement type2 EF " + file.mTag);
                return;
        }
        int efid = file.getEfid();
        if (this.mRecordSize != null && this.mRecordSize.get(efid) != null) {
            int[] size2 = this.mRecordSize.get(efid);
            size = size2;
        } else {
            size = readEFLinearRecordSize(efid);
        }
        if (size == null || size.length != 3) {
            Rlog.e(LOG_TAG, "readType2: read record size error.");
            return;
        }
        log("readType2: RecordSize = " + size[0]);
        ArrayList<byte[]> iapList = this.mIapFileList.get(recId);
        if (iapList.size() == 0) {
            Rlog.e(LOG_TAG, "Warning: IAP size is 0");
            return;
        }
        int Type2Index = file.getIndex();
        int totalReadingNum = 0;
        for (int i2 = nOffset; i2 < nMax; i2++) {
            try {
                AdnRecord arec = this.mPhoneBookRecords.get(i2);
                if (arec.getAlphaTag().length() > 0 || arec.getNumber().length() > 0) {
                    byte[] iapRecord = iapList.get(i2 - nOffset);
                    int index = iapRecord[Type2Index] & 255;
                    if (index > 0 && index < 255) {
                        log("Type2 iap[" + (i2 - nOffset) + "]=" + index);
                        int[] data = {recId, i2, 0};
                        int loadWhat = 0;
                        switch (file.mTag) {
                            case 195:
                                loadWhat = 18;
                                this.mReadingSneNum.incrementAndGet();
                                break;
                            case 196:
                                loadWhat = 16;
                                data[2] = file.mAnrIndex;
                                this.mReadingAnrNum.addAndGet(1);
                                break;
                            case USIM_EFEMAIL_TAG:
                                data[0] = ((i2 + 1) - nOffset) + (this.mEmailFileSize * nOffset);
                                loadWhat = 15;
                                this.mReadingEmailNum.incrementAndGet();
                                break;
                            default:
                                Rlog.e(LOG_TAG, "not support tag " + file.mTag);
                                break;
                        }
                        this.mFh.readEFLinearFixed(efid, index, size[0], obtainMessage(loadWhat, data));
                        totalReadingNum++;
                    }
                }
            } catch (IndexOutOfBoundsException e2) {
                Rlog.e(LOG_TAG, "readType2Ef: mPhoneBookRecords IndexOutOfBoundsException numAdnRecs is " + numAdnRecs + "index is " + i2);
            }
        }
        switch (file.mTag) {
            case 195:
                if (this.mReadingSneNum.get() == 0) {
                    this.mNeedNotify.set(false);
                    return;
                }
                this.mNeedNotify.set(true);
                break;
            case 196:
                if (this.mReadingAnrNum.get() == 0) {
                    this.mNeedNotify.set(false);
                    return;
                }
                this.mNeedNotify.set(true);
                break;
            case USIM_EFEMAIL_TAG:
                if (this.mReadingEmailNum.get() == 0) {
                    this.mNeedNotify.set(false);
                    return;
                }
                this.mNeedNotify.set(true);
                break;
            default:
                Rlog.e(LOG_TAG, "not support tag " + file.mTag);
                break;
        }
        logi("readType2Ef before mLock.wait " + this.mNeedNotify.get() + " total:" + totalReadingNum);
        synchronized (this.mLock) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e3) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readType2Ef");
            }
        }
        logi("readType2Ef after mLock.wait " + this.mNeedNotify.get());
    }

    private void updatePhoneAdnRecordWithEmailByIndex(int emailIndex, int adnIndex, byte[] emailRecData) {
        log("updatePhoneAdnRecordWithEmailByIndex emailIndex = " + emailIndex + ",adnIndex = " + adnIndex);
        if (emailRecData == null || this.mPbrRecords == null) {
            return;
        }
        boolean emailType2 = ((File) this.mPbrRecords.get(0).mFileIds.get(USIM_EFEMAIL_TAG)).getParentTag() == 169;
        log("updatePhoneAdnRecordWithEmailByIndex: Type2: " + emailType2 + " emailData: " + IccUtils.bytesToHexString(emailRecData));
        int length = emailRecData.length;
        if (emailType2 && emailRecData.length >= 2) {
            length = emailRecData.length - 2;
        }
        log("updatePhoneAdnRecordWithEmailByIndex length = " + length);
        byte[] validEMailData = new byte[length];
        for (int i = 0; i < length; i++) {
            validEMailData[i] = -1;
        }
        System.arraycopy(emailRecData, 0, validEMailData, 0, length);
        log("validEMailData=" + IccUtils.bytesToHexString(validEMailData) + ", validEmailLen=" + length);
        try {
            String email = IccUtils.adnStringFieldToString(validEMailData, 0, length);
            log("updatePhoneAdnRecordWithEmailByIndex index " + adnIndex + " emailRecData record is " + email);
            if (email != null && !email.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                AdnRecord rec = this.mPhoneBookRecords.get(adnIndex);
                rec.setEmails(new String[]{email});
            }
            this.mEmailRecTable[emailIndex - 1] = adnIndex + 1;
        } catch (IndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "[JE]updatePhoneAdnRecordWithEmailByIndex " + e.getMessage());
        }
    }

    private void updateType2Anr(String anr, int adnIndex, File file) {
        SparseArray<File> files;
        int tem;
        int tem2;
        logi("updateType2Ef anr:" + anr + ",adnIndex:" + adnIndex + ",file:" + file);
        int pbrRecNum = (adnIndex - 1) / this.mAdnFileSize;
        int iapRecNum = (adnIndex - 1) % this.mAdnFileSize;
        log("updateType2Ef pbrRecNum:" + pbrRecNum + ",iapRecNum:" + iapRecNum);
        if (this.mIapFileList == null || file == null || this.mPbrRecords == null || (files = this.mPbrRecords.get(file.mPbrRecord).mFileIds) == null) {
            return;
        }
        try {
            ArrayList<byte[]> list = this.mIapFileList.get(file.mPbrRecord);
            if (list == null) {
                return;
            }
            if (list.size() == 0) {
                Rlog.e(LOG_TAG, "Warning: IAP size is 0");
                return;
            }
            byte[] iap = list.get(iapRecNum);
            if (iap == null) {
                return;
            }
            int index = iap[file.getIndex()] & 255;
            log("updateType2Ef orignal index :" + index);
            if (anr == null || anr.length() == 0) {
                if (index > 0) {
                    iap[file.getIndex()] = -1;
                    if (files.get(193) != null) {
                        this.mFh.updateEFLinearFixed(files.get(193).getEfid(), iapRecNum + 1, iap, null, obtainMessage(7));
                        return;
                    } else {
                        Rlog.e(LOG_TAG, "updateType2Anr Error: No IAP file!");
                        return;
                    }
                }
                return;
            }
            int recNum = 0;
            int[] tmpSize = this.mRecordSize.get(file.getEfid());
            int size = tmpSize[2];
            log("updateType2Anr size :" + size);
            if (index <= 0 || index > size) {
                int[] indexArray = new int[size + 1];
                for (int i = 1; i <= size; i++) {
                    indexArray[i] = 0;
                }
                for (int i2 = 0; i2 < list.size(); i2++) {
                    byte[] value = list.get(i2);
                    if (value != null && (tem2 = value[file.getIndex()] & 255) > 0 && tem2 < 255 && tem2 <= size) {
                        indexArray[tem2] = 1;
                    }
                }
                boolean sharedAnr = false;
                File file2 = null;
                int i3 = 0;
                while (true) {
                    if (i3 >= this.mPbrRecords.size()) {
                        break;
                    }
                    if (i3 == file.mPbrRecord || (file2 = (File) this.mPbrRecords.get(i3).mFileIds.get((adnIndex * 256) + 196)) == null) {
                        i3++;
                    } else if (file2.getEfid() == file.getEfid()) {
                        sharedAnr = true;
                    }
                }
                if (sharedAnr) {
                    try {
                        ArrayList<byte[]> relatedList = this.mIapFileList.get(file2.mPbrRecord);
                        if (relatedList != null && relatedList.size() > 0) {
                            for (int i4 = 0; i4 < relatedList.size(); i4++) {
                                byte[] value2 = relatedList.get(i4);
                                if (value2 != null && (tem = value2[file2.getIndex()] & 255) > 0 && tem < 255 && tem <= size) {
                                    indexArray[tem] = 1;
                                }
                            }
                        }
                    } catch (IndexOutOfBoundsException e) {
                        Rlog.e(LOG_TAG, "Index out of bounds.");
                        return;
                    }
                }
                int i5 = 1;
                while (true) {
                    if (i5 > size) {
                        break;
                    }
                    if (indexArray[i5] == 0) {
                        recNum = i5;
                        break;
                    }
                    i5++;
                }
            } else {
                recNum = index;
            }
            log("updateType2Anr final index :" + recNum);
            if (recNum == 0) {
                return;
            }
            AdnRecord rec = null;
            try {
                rec = this.mPhoneBookRecords.get(adnIndex - 1);
            } catch (IndexOutOfBoundsException e2) {
                Rlog.e(LOG_TAG, "updateType2Anr: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + (adnIndex - 1));
            }
            if (rec == null) {
                return;
            }
            int aas = rec.getAasIndex();
            byte[] data = buildAnrRecord(anr, this.mAnrRecordSize, aas);
            int fileId = file.getEfid();
            if (data != null) {
                this.mFh.updateEFLinearFixed(fileId, recNum, data, null, obtainMessage(9));
                if (recNum != index) {
                    iap[file.getIndex()] = (byte) recNum;
                    if (files.get(193) != null) {
                        this.mFh.updateEFLinearFixed(files.get(193).getEfid(), iapRecNum + 1, iap, null, obtainMessage(7));
                    } else {
                        Rlog.e(LOG_TAG, "updateType2Anr Error: No IAP file!");
                    }
                }
            }
        } catch (IndexOutOfBoundsException e3) {
            Rlog.e(LOG_TAG, "Index out of bounds.");
        }
    }

    private void readAnrRecordSize() {
        SparseArray<File> fileIds;
        log("readAnrRecordSize");
        if (this.mPbrRecords == null || (fileIds = this.mPbrRecords.get(0).mFileIds) == null) {
            return;
        }
        File anrFile = fileIds.get(196);
        if (fileIds.size() == 0 || anrFile == null) {
            log("readAnrRecordSize: No anr tag in pbr file ");
            return;
        }
        int efid = anrFile.getEfid();
        int[] size = readEFLinearRecordSize(efid);
        if (size == null || size.length != 3) {
            Rlog.e(LOG_TAG, "readAnrRecordSize: read record size error.");
        } else {
            this.mAnrRecordSize = size[0];
        }
    }

    private boolean loadAasFiles() {
        synchronized (this.mLock) {
            if (this.mAasForAnr == null || this.mAasForAnr.size() == 0) {
                if (!this.mIsPbrPresent.booleanValue()) {
                    Rlog.e(LOG_TAG, "No PBR files");
                    return false;
                }
                loadPBRFiles();
                if (this.mPbrRecords == null) {
                    return false;
                }
                int numRecs = this.mPbrRecords.size();
                if (this.mAasForAnr == null) {
                    this.mAasForAnr = new ArrayList<>();
                }
                this.mAasForAnr.clear();
                if (this.mFh instanceof CsimFileHandler) {
                    for (int i = 0; i < numRecs; i++) {
                        readAASFileAndWait(i);
                    }
                } else {
                    readAasFileAndWaitOptmz();
                }
            }
            return true;
        }
    }

    public ArrayList<AlphaTag> getUsimAasList() {
        ArrayList<String> allAas;
        logi("getUsimAasList start");
        ArrayList<AlphaTag> results = new ArrayList<>();
        if (!loadAasFiles() || (allAas = this.mAasForAnr) == null) {
            return results;
        }
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < allAas.size(); j++) {
                String value = allAas.get(j);
                logi("aasIndex:" + (j + 1) + ",pbrIndex:" + i + ",value:" + value);
                AlphaTag tag = new AlphaTag(j + 1, value, i);
                results.add(tag);
            }
        }
        return results;
    }

    public String getUsimAasById(int index, int pbrIndex) {
        ArrayList<String> map;
        logi("getUsimAasById by id " + index + ",pbrIndex " + pbrIndex);
        if (loadAasFiles() && (map = this.mAasForAnr) != null) {
            return map.get(index - 1);
        }
        return null;
    }

    public boolean removeUsimAasById(int index, int pbrIndex) {
        logi("removeUsimAasById by id " + index + ",pbrIndex " + pbrIndex);
        if (!loadAasFiles()) {
            return false;
        }
        SparseArray<File> files = this.mPbrRecords.get(pbrIndex).mFileIds;
        if (files == null || files.get(USIM_EFAAS_TAG) == null) {
            Rlog.e(LOG_TAG, "removeUsimAasById-PBR have no AAS EF file");
            return false;
        }
        int efid = files.get(USIM_EFAAS_TAG).getEfid();
        log("removeUsimAasById result,efid:" + efid);
        if (this.mFh != null) {
            Message msg = obtainMessage(10);
            int len = getUsimAasMaxNameLen();
            byte[] aasString = new byte[len];
            for (int i = 0; i < len; i++) {
                aasString[i] = -1;
            }
            synchronized (this.mLock) {
                this.mCi.deleteUPBEntry(3, 1, index, msg);
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in removesimAasById");
                }
            }
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar == null || ar.exception == null) {
                ArrayList<String> list = this.mAasForAnr;
                if (list != null) {
                    log("remove aas done " + list.get(index - 1));
                    list.set(index - 1, null);
                    return true;
                }
                return true;
            }
            Rlog.e(LOG_TAG, "removeUsimAasById exception " + ar.exception);
            return false;
        }
        Rlog.e(LOG_TAG, "removeUsimAasById-IccFileHandler is null");
        return false;
    }

    public int insertUsimAas(String aasName) {
        logi("insertUsimAas " + aasName);
        if (aasName == null || aasName.length() == 0) {
            return 0;
        }
        if (!loadAasFiles()) {
            return -1;
        }
        int limit = getUsimAasMaxNameLen();
        int len = aasName.length();
        if (len > limit) {
            return 0;
        }
        synchronized (this.mLock) {
            int aasIndex = 0;
            boolean found = false;
            ArrayList<String> allAas = this.mAasForAnr;
            for (int j = 0; j < allAas.size(); j++) {
                String value = allAas.get(j);
                if (value == null || value.length() == 0) {
                    found = true;
                    aasIndex = j + 1;
                    break;
                }
            }
            log("insertUsimAas aasIndex:" + aasIndex + ",found:" + found);
            if (!found) {
                return -2;
            }
            String temp = encodeToUcs2(aasName);
            Message msg = obtainMessage(10);
            this.mCi.editUPBEntry(3, 0, aasIndex, temp, null, msg);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "Interrupted Exception in insertUsimAas");
            }
            AsyncResult ar = (AsyncResult) msg.obj;
            log("insertUsimAas UPB_EF_AAS: ar " + ar);
            if (ar == null || ar.exception == null) {
                ArrayList<String> list = this.mAasForAnr;
                if (list != null) {
                    list.set(aasIndex - 1, aasName);
                    logi("insertUsimAas update mAasForAnr done");
                }
                return aasIndex;
            }
            Rlog.e(LOG_TAG, "insertUsimAas exception " + ar.exception);
            return -1;
        }
    }

    public boolean updateUsimAas(int index, int pbrIndex, String aasName) {
        logi("updateUsimAas index " + index + ",pbrIndex " + pbrIndex + ",aasName " + aasName);
        if (!loadAasFiles()) {
            return false;
        }
        ArrayList<String> map = this.mAasForAnr;
        if (index <= 0 || index > map.size()) {
            Rlog.e(LOG_TAG, "updateUsimAas not found aas index " + index);
            return false;
        }
        String aas = map.get(index - 1);
        log("updateUsimAas old aas " + aas);
        if (aasName == null || aasName.length() == 0) {
            return removeUsimAasById(index, pbrIndex);
        }
        int limit = getUsimAasMaxNameLen();
        int len = aasName.length();
        log("updateUsimAas aas limit " + limit);
        if (len > limit) {
            return false;
        }
        log("updateUsimAas offset 0");
        int aasIndex = index + 0;
        String temp = encodeToUcs2(aasName);
        Message msg = obtainMessage(10);
        synchronized (this.mLock) {
            this.mCi.editUPBEntry(3, 0, aasIndex, temp, null, msg);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "Interrupted Exception in updateUsimAas");
            }
        }
        AsyncResult ar = (AsyncResult) msg.obj;
        if (ar == null || ar.exception == null) {
            ArrayList<String> list = this.mAasForAnr;
            if (list != null) {
                list.set(index - 1, aasName);
                logi("updateUsimAas update mAasForAnr done");
                return true;
            }
            return true;
        }
        Rlog.e(LOG_TAG, "updateUsimAas exception " + ar.exception);
        return false;
    }

    public boolean updateAdnAas(int adnIndex, int aasIndex) {
        int i = (adnIndex - 1) / this.mAdnFileSize;
        int i2 = (adnIndex - 1) % this.mAdnFileSize;
        try {
            AdnRecord rec = this.mPhoneBookRecords.get(adnIndex - 1);
            rec.setAasIndex(aasIndex);
            for (int i3 = 0; i3 < 3; i3++) {
                String anr = rec.getAdditionalNumber(i3);
                updateAnrByAdnIndex(anr, adnIndex, i3);
            }
            return true;
        } catch (IndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "updateADNAAS: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + (adnIndex - 1));
            return false;
        }
    }

    public int getUsimAasMaxNameLen() {
        logi("getUsimAasMaxNameLen begin");
        synchronized (this.mUPBCapabilityLock) {
            if (this.mUpbCap[4] <= 0 && checkIsPhbReady()) {
                this.mCi.queryUPBCapability(obtainMessage(19));
                try {
                    this.mUPBCapabilityLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in getUsimAasMaxNameLen");
                }
            }
        }
        logi("getUsimAasMaxNameLen done: L_AAS is " + this.mUpbCap[4]);
        return this.mUpbCap[4];
    }

    public int getUsimAasMaxCount() {
        logi("getUsimAasMaxCount begin");
        synchronized (this.mUPBCapabilityLock) {
            if (this.mUpbCap[3] <= 0 && checkIsPhbReady()) {
                this.mCi.queryUPBCapability(obtainMessage(19));
                try {
                    this.mUPBCapabilityLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in getUsimAasMaxCount");
                }
            }
        }
        logi("getUsimAasMaxCount done: N_AAS is " + this.mUpbCap[3]);
        return this.mUpbCap[3];
    }

    public void loadPBRFiles() {
        if (!this.mIsPbrPresent.booleanValue()) {
            return;
        }
        synchronized (this.mLock) {
            if (this.mPbrRecords == null) {
                readPbrFileAndWait(false);
            }
            if (this.mPbrRecords == null) {
                readPbrFileAndWait(true);
            }
        }
    }

    public int getAnrCount() {
        logi("getAnrCount begin");
        synchronized (this.mUPBCapabilityLock) {
            if (this.mUpbCap[0] <= 0 && checkIsPhbReady()) {
                this.mCi.queryUPBCapability(obtainMessage(19));
                try {
                    this.mUPBCapabilityLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in getAnrCount");
                }
            }
        }
        if (this.mAnrRecordSize == 0) {
            return 0;
        }
        logi("getAnrCount done: N_ANR is " + this.mUpbCap[0]);
        return this.mUpbCap[0] > 0 ? 1 : 0;
    }

    public int getEmailCount() {
        logi("getEmailCount begin");
        synchronized (this.mUPBCapabilityLock) {
            if (this.mUpbCap[1] <= 0 && checkIsPhbReady()) {
                this.mCi.queryUPBCapability(obtainMessage(19));
                try {
                    this.mUPBCapabilityLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in getEmailCount");
                }
            }
        }
        if (this.mEmailRecordSize <= 0) {
            return 0;
        }
        logi("getEmailCount done: N_EMAIL is " + this.mUpbCap[1]);
        return this.mUpbCap[1] > 0 ? 1 : 0;
    }

    public boolean hasSne() {
        synchronized (this.mLock) {
            loadPBRFiles();
            if (this.mPbrRecords == null) {
                Rlog.e(LOG_TAG, "hasSne No PBR files");
                return false;
            }
            SparseArray<File> files = this.mPbrRecords.get(0).mFileIds;
            if (files != null && files.get(195) != null) {
                logi("hasSne:  true");
                return true;
            }
            logi("hasSne:  false");
            return false;
        }
    }

    public int getSneRecordLen() {
        SparseArray<File> files;
        File sneFile;
        int[] size;
        if (!hasSne() || (files = this.mPbrRecords.get(0).mFileIds) == null || (sneFile = files.get(195)) == null) {
            return -1;
        }
        int efid = sneFile.getEfid();
        boolean sneType2 = sneFile.getParentTag() == 169;
        logi("getSneRecordLen: EFSNE id is " + efid);
        if (this.mRecordSize != null && this.mRecordSize.get(efid) != null) {
            int[] size2 = this.mRecordSize.get(efid);
            size = size2;
        } else {
            size = readEFLinearRecordSize(efid);
        }
        if (size == null) {
            return 0;
        }
        if (sneType2) {
            int resultSize = size[0] - 2;
            return resultSize;
        }
        int resultSize2 = size[0];
        return resultSize2;
    }

    private void updatePhoneAdnRecordWithSneByIndex(int recNum, int adnIndex, byte[] recData) {
        if (recData == null) {
            return;
        }
        String sne = IccUtils.adnStringFieldToString(recData, 0, recData.length);
        log("updatePhoneAdnRecordWithSneByIndex index " + adnIndex + " recData file is " + sne);
        if (sne == null || sne.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            return;
        }
        try {
            AdnRecord rec = this.mPhoneBookRecords.get(adnIndex);
            rec.setSne(sne);
        } catch (IndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "updatePhoneAdnRecordWithSneByIndex: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + adnIndex);
        }
    }

    public int updateSneByAdnIndex(String sne, int adnIndex) {
        logi("updateSneByAdnIndex sne is " + sne + ",adnIndex " + adnIndex);
        int pbrRecNum = (adnIndex - 1) / this.mAdnFileSize;
        int i = (adnIndex - 1) % this.mAdnFileSize;
        if (this.mPbrRecords == null) {
            return -1;
        }
        Message msg = obtainMessage(11);
        SparseArray<File> files = this.mPbrRecords.get(pbrRecNum).mFileIds;
        if (files == null || files.get(195) == null) {
            log("updateSneByAdnIndex: No SNE tag in pbr file 0");
            return -1;
        }
        if (this.mPhoneBookRecords == null || this.mPhoneBookRecords.isEmpty()) {
            return -1;
        }
        File sneFile = files.get(195);
        int efid = sneFile.getEfid();
        log("updateSneByAdnIndex: EF_SNE id is " + Integer.toHexString(efid).toUpperCase());
        log("updateSneByAdnIndex: efIndex is 1");
        synchronized (this.mLock) {
            if (sne != null) {
                if (sne.length() == 0) {
                    this.mCi.deleteUPBEntry(2, 1, adnIndex, msg);
                } else {
                    String temp = encodeToUcs2(sne);
                    this.mCi.editUPBEntry(2, 1, adnIndex, temp, null, msg);
                }
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in updateSneByAdnIndex");
                }
            }
        }
        return this.mResult;
    }

    private int[] getAdnStorageInfo() {
        log("getAdnStorageInfo ");
        if (this.mCi != null) {
            this.mCi.queryPhbStorageInfo(0, obtainMessage(21));
            synchronized (this.mLock) {
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in getAdnStorageInfo");
                }
            }
            return this.mAdnRecordSize;
        }
        Rlog.w(LOG_TAG, "GetAdnStorageInfo: filehandle is null.");
        return null;
    }

    public UsimPBMemInfo[] getPhonebookMemStorageExt() {
        ArrayList<byte[]> ext1;
        boolean is3G = this.mCurrentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM;
        log("getPhonebookMemStorageExt isUsim " + is3G);
        if (!is3G) {
            return getPhonebookMemStorageExt2G();
        }
        if (this.mPbrRecords == null) {
            loadPBRFiles();
        }
        if (this.mPbrRecords == null) {
            return null;
        }
        log("getPhonebookMemStorageExt slice " + this.mPbrRecords.size());
        UsimPBMemInfo[] response = new UsimPBMemInfo[this.mPbrRecords.size()];
        for (int i = 0; i < this.mPbrRecords.size(); i++) {
            response[i] = new UsimPBMemInfo();
        }
        if (this.mPhoneBookRecords.isEmpty()) {
            Rlog.w(LOG_TAG, "mPhoneBookRecords has not been loaded.");
            return response;
        }
        for (int pbrIndex = 0; pbrIndex < this.mPbrRecords.size(); pbrIndex++) {
            SparseArray<File> files = this.mPbrRecords.get(pbrIndex).mFileIds;
            int numAdnRecs = this.mPhoneBookRecords.size();
            int nOffset = pbrIndex * this.mAdnFileSize;
            int nMax = nOffset + this.mAdnFileSize;
            if (numAdnRecs < nMax) {
                nMax = numAdnRecs;
            }
            File adnFile = files.get(192);
            if (adnFile != null) {
                int[] size = readEFLinearRecordSize(adnFile.getEfid());
                if (size != null) {
                    response[pbrIndex].setAdnLength(size[0]);
                    response[pbrIndex].setAdnTotal(size[2]);
                }
                response[pbrIndex].setAdnType(adnFile.getParentTag());
                response[pbrIndex].setSliceIndex(pbrIndex + 1);
                int used = 0;
                AdnRecord rec = null;
                for (int j = nOffset; j < nMax; j++) {
                    try {
                        rec = this.mPhoneBookRecords.get(j);
                    } catch (IndexOutOfBoundsException e) {
                        Rlog.e(LOG_TAG, "getPhonebookMemStorageExt: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + j);
                    }
                    if (rec != null && ((rec.getAlphaTag() != null && rec.getAlphaTag().length() > 0) || (rec.getNumber() != null && rec.getNumber().length() > 0))) {
                        log("Adn: " + rec.toString());
                        used++;
                        rec = null;
                    }
                }
                log("adn used " + used);
                response[pbrIndex].setAdnUsed(used);
            }
            File anrFile = files.get(196);
            if (anrFile != null) {
                int[] size2 = readEFLinearRecordSize(anrFile.getEfid());
                if (size2 != null) {
                    response[pbrIndex].setAnrLength(size2[0]);
                    response[pbrIndex].setAnrTotal(size2[2]);
                }
                response[pbrIndex].setAnrType(anrFile.getParentTag());
                int used2 = 0;
                AdnRecord rec2 = null;
                for (int i2 = nOffset; i2 < this.mPhoneBookRecords.size() + nOffset; i2++) {
                    try {
                        rec2 = this.mPhoneBookRecords.get(i2);
                    } catch (IndexOutOfBoundsException e2) {
                        Rlog.e(LOG_TAG, "getPhonebookMemStorageExt: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + i2);
                    }
                    if (rec2 == null) {
                        log("null anr rec ");
                    } else {
                        String anrStr = rec2.getAdditionalNumber();
                        if (anrStr != null && anrStr.length() > 0) {
                            log("anrStr: " + anrStr);
                            used2++;
                        }
                    }
                }
                log("anr used: " + used2);
                response[pbrIndex].setAnrUsed(used2);
            }
            File emailFile = files.get(USIM_EFEMAIL_TAG);
            if (emailFile != null) {
                int[] size3 = readEFLinearRecordSize(emailFile.getEfid());
                if (size3 != null) {
                    response[pbrIndex].setEmailLength(size3[0]);
                    response[pbrIndex].setEmailTotal(size3[2]);
                }
                response[pbrIndex].setEmailType(emailFile.getParentTag());
                log("numAdnRecs:" + numAdnRecs);
                int used3 = 0;
                for (int i3 = nOffset; i3 < this.mEmailFileSize + nOffset; i3++) {
                    try {
                        if (this.mEmailRecTable[i3] > 0) {
                            used3++;
                        }
                    } catch (ArrayIndexOutOfBoundsException e3) {
                        Rlog.e(LOG_TAG, "get mEmailRecTable error " + e3.getMessage());
                    }
                }
                log("emailList used:" + used3);
                response[pbrIndex].setEmailUsed(used3);
            }
            File ext1File = files.get(194);
            if (ext1File != null) {
                int[] size4 = readEFLinearRecordSize(ext1File.getEfid());
                if (size4 != null) {
                    response[pbrIndex].setExt1Length(size4[0]);
                    response[pbrIndex].setExt1Total(size4[2]);
                }
                response[pbrIndex].setExt1Type(ext1File.getParentTag());
                synchronized (this.mLock) {
                    readExt1FileAndWait(pbrIndex);
                }
                int used4 = 0;
                if (this.mExt1FileList != null && pbrIndex < this.mExt1FileList.size() && (ext1 = this.mExt1FileList.get(pbrIndex)) != null) {
                    int len = ext1.size();
                    for (int i4 = 0; i4 < len; i4++) {
                        byte[] arr = ext1.get(i4);
                        log("ext1[" + i4 + "]=" + IccUtils.bytesToHexString(arr));
                        if (arr != null && arr.length > 0 && (arr[0] == 1 || arr[0] == 2)) {
                            used4++;
                        }
                    }
                }
                response[pbrIndex].setExt1Used(used4);
            }
            File gasFile = files.get(200);
            if (gasFile != null) {
                int[] size5 = readEFLinearRecordSize(gasFile.getEfid());
                if (size5 != null) {
                    response[pbrIndex].setGasLength(size5[0]);
                    response[pbrIndex].setGasTotal(size5[2]);
                }
                response[pbrIndex].setGasType(gasFile.getParentTag());
            }
            File aasFile = files.get(USIM_EFAAS_TAG);
            if (aasFile != null) {
                int[] size6 = readEFLinearRecordSize(aasFile.getEfid());
                if (size6 != null) {
                    response[pbrIndex].setAasLength(size6[0]);
                    response[pbrIndex].setAasTotal(size6[2]);
                }
                response[pbrIndex].setAasType(aasFile.getParentTag());
            }
            File sneFile = files.get(195);
            if (sneFile != null) {
                int[] size7 = readEFLinearRecordSize(sneFile.getEfid());
                if (size7 != null) {
                    response[pbrIndex].setSneLength(size7[0]);
                    response[pbrIndex].setSneTotal(size7[0]);
                }
                response[pbrIndex].setSneType(sneFile.getParentTag());
            }
            File ccpFile = files.get(USIM_EFCCP1_TAG);
            if (ccpFile != null) {
                int[] size8 = readEFLinearRecordSize(ccpFile.getEfid());
                if (size8 != null) {
                    response[pbrIndex].setCcpLength(size8[0]);
                    response[pbrIndex].setCcpTotal(size8[0]);
                }
                response[pbrIndex].setCcpType(ccpFile.getParentTag());
            }
        }
        for (int i5 = 0; i5 < this.mPbrRecords.size(); i5++) {
            log("getPhonebookMemStorageExt[" + i5 + "]:" + response[i5]);
        }
        return response;
    }

    public UsimPBMemInfo[] getPhonebookMemStorageExt2G() {
        ArrayList<byte[]> ext1;
        UsimPBMemInfo[] response = {new UsimPBMemInfo()};
        int[] size = readEFLinearRecordSize(28474);
        if (size != null) {
            response[0].setAdnLength(size[0]);
            if (isAdnAccessible()) {
                response[0].setAdnTotal(size[2]);
            } else {
                response[0].setAdnTotal(0);
            }
        }
        response[0].setAdnType(168);
        response[0].setSliceIndex(1);
        int[] size2 = readEFLinearRecordSize(IccConstants.EF_EXT1);
        if (size2 != null) {
            response[0].setExt1Length(size2[0]);
            response[0].setExt1Total(size2[2]);
        }
        response[0].setExt1Type(170);
        synchronized (this.mLock) {
            if (this.mFh != null) {
                Message msg = obtainMessage(1001);
                msg.arg1 = 0;
                this.mFh.loadEFLinearFixedAll(IccConstants.EF_EXT1, msg);
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in readExt1FileAndWait");
                }
                int used = 0;
                if (this.mExt1FileList != null && this.mExt1FileList.size() > 0 && (ext1 = this.mExt1FileList.get(0)) != null) {
                    int len = ext1.size();
                    for (int i = 0; i < len; i++) {
                        byte[] arr = ext1.get(i);
                        log("ext1[" + i + "]=" + IccUtils.bytesToHexString(arr));
                        if (arr != null && arr.length > 0 && (arr[0] == 1 || arr[0] == 2)) {
                            used++;
                        }
                    }
                }
                response[0].setExt1Used(used);
                log("getPhonebookMemStorageExt2G:" + response[0]);
                return response;
            }
            Rlog.e(LOG_TAG, "readExt1FileAndWait-IccFileHandler is null");
            return response;
        }
    }

    public int[] readEFLinearRecordSize(int fileId) {
        int[] iArr;
        log("readEFLinearRecordSize fileid " + Integer.toHexString(fileId).toUpperCase());
        Message msg = obtainMessage(1000);
        msg.arg1 = fileId;
        synchronized (this.mLock) {
            if (this.mFh != null) {
                this.mFh.getEFLinearRecordSize(fileId, msg);
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in readEFLinearRecordSize");
                }
            } else {
                Rlog.e(LOG_TAG, "readEFLinearRecordSize-IccFileHandler is null");
            }
            iArr = this.mRecordSize != null ? this.mRecordSize.get(fileId) : null;
            if (iArr != null) {
                logi("readEFLinearRecordSize fileid:" + Integer.toHexString(fileId).toUpperCase() + ",len:" + iArr[0] + ",total:" + iArr[1] + ",count:" + iArr[2]);
            }
        }
        return iArr;
    }

    private void readExt1FileAndWait(int recId) {
        logi("readExt1FileAndWait " + recId);
        if (this.mPbrRecords == null || this.mPbrRecords.get(recId) == null) {
            return;
        }
        SparseArray<File> files = this.mPbrRecords.get(recId).mFileIds;
        if (files == null || files.get(194) == null) {
            Rlog.e(LOG_TAG, "readExt1FileAndWait-PBR have no Ext1 record");
            return;
        }
        int efid = files.get(194).getEfid();
        log("readExt1FileAndWait-get EXT1 EFID " + efid);
        if (this.mExt1FileList != null && recId < this.mExt1FileList.size()) {
            log("EXT1 has been loaded for Pbr number " + recId);
            return;
        }
        if (this.mFh != null) {
            Message msg = obtainMessage(1001);
            msg.arg1 = recId;
            this.mFh.loadEFLinearFixedAll(efid, msg);
            try {
                this.mLock.wait();
                return;
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readExt1FileAndWait");
                return;
            }
        }
        Rlog.e(LOG_TAG, "readExt1FileAndWait-IccFileHandler is null");
    }

    private boolean checkIsPhbReady() {
        boolean isSimLocked;
        String strPhbReady;
        String strCurSimState = UsimPBMemInfo.STRING_NOT_SET;
        int slotId = this.mCurrentApp.getSlotId();
        if (!SubscriptionManager.isValidSlotId(slotId)) {
            log("[isPhbReady] InvalidSlotId slotId: " + slotId);
            return false;
        }
        int[] subId = SubscriptionManager.getSubId(slotId);
        int phoneId = SubscriptionManager.getPhoneId(subId[0]);
        String strAllSimState = SystemProperties.get("gsm.sim.state");
        if (strAllSimState != null && strAllSimState.length() > 0) {
            String[] values = strAllSimState.split(",");
            if (phoneId >= 0 && phoneId < values.length && values[phoneId] != null) {
                strCurSimState = values[phoneId];
            }
        }
        if (strCurSimState.equals("NETWORK_LOCKED")) {
            isSimLocked = true;
        } else {
            isSimLocked = strCurSimState.equals("PIN_REQUIRED");
        }
        if (1 == slotId) {
            strPhbReady = SystemProperties.get("gsm.sim.ril.phbready.2", "false");
        } else if (2 == slotId) {
            strPhbReady = SystemProperties.get("gsm.sim.ril.phbready.3", "false");
        } else if (3 == slotId) {
            strPhbReady = SystemProperties.get("gsm.sim.ril.phbready.4", "false");
        } else {
            strPhbReady = SystemProperties.get("gsm.sim.ril.phbready", "false");
        }
        logi("[isPhbReady] subId[0]:" + subId[0] + ", slotId: " + slotId + ", isPhbReady: " + strPhbReady + ",strSimState: " + strAllSimState);
        return strPhbReady.equals("true") && !isSimLocked;
    }

    public boolean isAdnAccessible() {
        if (this.mFh != null && this.mCurrentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_SIM) {
            synchronized (this.mLock) {
                Message response = obtainMessage(20);
                this.mFh.selectEFFile(28474, response);
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in isAdnAccessible");
                }
            }
            if (this.efData != null) {
                int fs = this.efData.getFileStatus();
                return (fs & 5) > 0;
            }
        }
        return true;
    }

    public boolean isUsimPhbEfAndNeedReset(int fileId) {
        logi("isUsimPhbEfAndNeedReset, fileId: " + Integer.toHexString(fileId).toUpperCase());
        if (this.mPbrRecords == null) {
            Rlog.e(LOG_TAG, "isUsimPhbEfAndNeedReset, No PBR files");
            return false;
        }
        int numRecs = this.mPbrRecords.size();
        for (int i = 0; i < numRecs; i++) {
            SparseArray<File> files = this.mPbrRecords.get(i).mFileIds;
            for (int j = 192; j <= USIM_EFCCP1_TAG; j++) {
                if (j == 197 || j == 201 || j == USIM_EFCCP1_TAG) {
                    logi("isUsimPhbEfAndNeedReset, not reset EF: " + j);
                } else if (files.get(j) != null && fileId == files.get(j).getEfid()) {
                    logi("isUsimPhbEfAndNeedReset, return true with EF: " + j);
                    return true;
                }
            }
        }
        log("isUsimPhbEfAndNeedReset, return false.");
        return false;
    }

    private void readAdnFileAndWaitForUICC(int recId) {
        SparseArray<File> files;
        logi("readAdnFileAndWaitForUICC " + recId);
        if (this.mPbrRecords == null || (files = this.mPbrRecords.get(recId).mFileIds) == null || files.size() == 0) {
            return;
        }
        if (files.get(192) == null) {
            Rlog.e(LOG_TAG, "readAdnFileAndWaitForUICC: No ADN tag in pbr record " + recId);
            return;
        }
        int efid = files.get(192).getEfid();
        log("readAdnFileAndWaitForUICC: EFADN id is " + efid);
        log("UiccPhoneBookManager readAdnFileAndWaitForUICC: recId is " + recId + UsimPBMemInfo.STRING_NOT_SET);
        this.mAdnCache.requestLoadAllAdnLike(efid, this.mAdnCache.extensionEfForEf(28474), obtainMessage(2));
        try {
            this.mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readAdnFileAndWait");
        }
        int previousSize = this.mPhoneBookRecords.size();
        if (this.mPbrRecords == null || this.mPbrRecords.size() <= recId) {
            return;
        }
        this.mPbrRecords.get(recId).mMasterFileRecordNum = this.mPhoneBookRecords.size() - previousSize;
    }

    public ArrayList<AdnRecord> getAdnListFromUsim() {
        return this.mPhoneBookRecords;
    }

    private ArrayList<AdnRecord> changeAdnRecordNumber(int baseNumber, ArrayList<AdnRecord> adnList) {
        int size = adnList.size();
        for (int i = 0; i < size; i++) {
            AdnRecord adnRecord = adnList.get(i);
            if (adnRecord != null) {
                adnRecord.setRecordIndex(adnRecord.getRecId() + baseNumber);
            }
        }
        return adnList;
    }
}
