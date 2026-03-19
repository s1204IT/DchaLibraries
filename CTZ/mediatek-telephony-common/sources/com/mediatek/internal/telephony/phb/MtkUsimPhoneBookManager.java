package com.mediatek.internal.telephony.phb;

import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.gsm.SimTlv;
import com.android.internal.telephony.gsm.UsimPhoneBookManager;
import com.android.internal.telephony.uicc.AdnRecordCache;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;
import com.mediatek.internal.telephony.MtkRIL;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.EFResponseData;
import com.mediatek.telephony.internal.telephony.vsim.ExternalSimConstants;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MtkUsimPhoneBookManager extends UsimPhoneBookManager {
    private static final boolean DBG;
    private static final int EVENT_AAS_LOAD_DONE = 5;
    private static final int EVENT_AAS_LOAD_DONE_OPTMZ = 28;
    private static final int EVENT_AAS_UPDATE_DONE = 10;
    private static final int EVENT_ANR_RECORD_LOAD_DONE = 16;
    private static final int EVENT_ANR_RECORD_LOAD_OPTMZ_DONE = 23;
    private static final int EVENT_ANR_UPDATE_DONE = 9;
    private static final int EVENT_EMAIL_RECORD_LOAD_DONE = 15;
    private static final int EVENT_EMAIL_RECORD_LOAD_OPTMZ_DONE = 22;
    private static final int EVENT_EMAIL_UPDATE_DONE = 8;
    private static final int EVENT_EXT1_LOAD_DONE = 1001;
    private static final int EVENT_GAS_LOAD_DONE = 6;
    private static final int EVENT_GAS_UPDATE_DONE = 13;
    private static final int EVENT_GET_RECORDS_SIZE_DONE = 1000;
    private static final int EVENT_GRP_RECORD_LOAD_DONE = 17;
    private static final int EVENT_GRP_UPDATE_DONE = 12;
    private static final int EVENT_IAP_RECORD_LOAD_DONE = 14;
    private static final int EVENT_IAP_UPDATE_DONE = 7;
    private static final int EVENT_QUERY_ANR_AVAILABLE_OPTMZ_DONE = 26;
    private static final int EVENT_QUERY_EMAIL_AVAILABLE_OPTMZ_DONE = 25;
    private static final int EVENT_QUERY_PHB_ADN_INFO = 21;
    private static final int EVENT_QUERY_SNE_AVAILABLE_OPTMZ_DONE = 27;
    private static final int EVENT_SELECT_EF_FILE_DONE = 20;
    private static final int EVENT_SNE_RECORD_LOAD_DONE = 18;
    private static final int EVENT_SNE_RECORD_LOAD_OPTMZ_DONE = 24;
    private static final int EVENT_SNE_UPDATE_DONE = 11;
    private static final int EVENT_UPB_CAPABILITY_QUERY_DONE = 19;
    private static final String LOG_TAG = "MtkUsimPhoneBookManager";
    private static final int PBR_NOT_NEED_NOTIFY = -1;
    private static final String PROP_FORCE_DEBUG_KEY = "persist.vendor.log.tel_dbg";
    private static final int UPB_EF_AAS = 3;
    private static final int UPB_EF_ANR = 0;
    private static final int UPB_EF_EMAIL = 1;
    private static final int UPB_EF_GAS = 4;
    private static final int UPB_EF_GRP = 5;
    private static final int UPB_EF_SNE = 2;
    private static final int USIM_DEFAULT_MAX_ADN_FILE_SIZE = 250;
    private static final int USIM_DEFAULT_MAX_EMAIL_FILE_SIZE = 100;
    public static final int USIM_ERROR_CAPACITY_FULL = -30;
    public static final int USIM_ERROR_GROUP_COUNT = -20;
    public static final int USIM_ERROR_NAME_LEN = -10;
    public static final int USIM_ERROR_OTHERS = -50;
    public static final int USIM_ERROR_STRING_TOOLONG = -40;
    private static final int USIM_MAX_AAS_ENTRIES_COUNT = 5;
    public static final int USIM_MAX_ANR_COUNT = 3;
    private static final int USIM_TYPE2_CONDITIONAL_LENGTH = 2;
    private ArrayList<String> mAasForAnr;
    private MtkAdnRecordCache mAdnCache;
    private int mAdnFileSize;
    private int[] mAdnRecordSize;
    private ArrayList<int[]> mAnrInfo;
    private int mAnrRecordSize;
    private MtkRIL mCi;
    private UiccCardApplication mCurrentApp;
    protected EFResponseData mEfData;
    private int mEmailFileSize;
    private int[] mEmailInfo;
    private int[] mEmailRecTable;
    private int mEmailRecordSize;
    private ArrayList<ArrayList<byte[]>> mExt1FileList;
    private ArrayList<UsimGroup> mGasForGrp;
    private final Object mGasLock;
    private ArrayList<ArrayList<byte[]>> mIapFileList;
    private boolean mIsReset;
    private AtomicBoolean mNeedNotify;
    private int mPbrNeedNotify;
    private ArrayList<PbrRecord> mPbrRecords;
    private ArrayList<MtkAdnRecord> mPhoneBookRecords;
    private AtomicInteger mReadingAnrNum;
    private AtomicInteger mReadingEmailNum;
    private AtomicInteger mReadingGrpNum;
    private AtomicInteger mReadingIapNum;
    private AtomicInteger mReadingSneNum;
    private SparseArray<int[]> mRecordSize;
    private boolean mRefreshAdnInfo;
    private boolean mRefreshAnrInfo;
    private boolean mRefreshEmailInfo;
    private int mResult;
    private int mSliceCount;
    private int mSlotId;
    private int[] mSneInfo;
    private final Object mUPBCapabilityLock;
    private int[] mUpbCap;
    private int mUpbDone;

    static int access$408(MtkUsimPhoneBookManager mtkUsimPhoneBookManager) {
        int i = mtkUsimPhoneBookManager.mSliceCount;
        mtkUsimPhoneBookManager.mSliceCount = i + 1;
        return i;
    }

    static {
        boolean z = false;
        if (SystemProperties.getInt(PROP_FORCE_DEBUG_KEY, 0) == 1 && !SystemProperties.get("ro.build.type").equals(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER)) {
            z = true;
        }
        DBG = z;
    }

    private class File {
        public int mAnrIndex;
        private final int mEfid;
        private final int mIndex;
        private final int mParentTag;
        public int mPbrRecord;
        private final int mSfi;
        public int mTag;

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

        public String toString() {
            return "mParentTag:" + Integer.toHexString(this.mParentTag).toUpperCase() + ",mEfid:" + Integer.toHexString(this.mEfid).toUpperCase() + ",mSfi:" + Integer.toHexString(this.mSfi).toUpperCase() + ",mIndex:" + this.mIndex + ",mPbrRecord:" + this.mPbrRecord + ",mAnrIndex" + this.mAnrIndex + ",mTag:" + Integer.toHexString(this.mTag).toUpperCase();
        }
    }

    public MtkUsimPhoneBookManager(IccFileHandler iccFileHandler, AdnRecordCache adnRecordCache) {
        super(iccFileHandler, adnRecordCache);
        this.mSlotId = -1;
        this.mGasLock = new Object();
        this.mUPBCapabilityLock = new Object();
        this.mEmailRecordSize = -1;
        this.mEmailFileSize = 100;
        this.mAdnFileSize = 250;
        this.mAnrRecordSize = -1;
        this.mSliceCount = 0;
        this.mUpbDone = -1;
        this.mIsReset = false;
        this.mPbrNeedNotify = -1;
        this.mIapFileList = null;
        this.mRefreshEmailInfo = false;
        this.mRefreshAnrInfo = false;
        this.mRefreshAdnInfo = false;
        this.mEmailRecTable = new int[400];
        this.mUpbCap = new int[8];
        this.mResult = -1;
        this.mReadingAnrNum = new AtomicInteger(0);
        this.mReadingEmailNum = new AtomicInteger(0);
        this.mReadingGrpNum = new AtomicInteger(0);
        this.mReadingSneNum = new AtomicInteger(0);
        this.mReadingIapNum = new AtomicInteger(0);
        this.mNeedNotify = new AtomicBoolean(false);
        this.mEfData = null;
    }

    public MtkUsimPhoneBookManager(IccFileHandler iccFileHandler, AdnRecordCache adnRecordCache, CommandsInterface commandsInterface, UiccCardApplication uiccCardApplication) {
        super(iccFileHandler, adnRecordCache);
        this.mSlotId = -1;
        this.mGasLock = new Object();
        this.mUPBCapabilityLock = new Object();
        this.mEmailRecordSize = -1;
        this.mEmailFileSize = 100;
        this.mAdnFileSize = 250;
        this.mAnrRecordSize = -1;
        this.mSliceCount = 0;
        this.mUpbDone = -1;
        this.mIsReset = false;
        this.mPbrNeedNotify = -1;
        this.mIapFileList = null;
        this.mRefreshEmailInfo = false;
        this.mRefreshAnrInfo = false;
        this.mRefreshAdnInfo = false;
        this.mEmailRecTable = new int[400];
        this.mUpbCap = new int[8];
        this.mResult = -1;
        this.mReadingAnrNum = new AtomicInteger(0);
        this.mReadingEmailNum = new AtomicInteger(0);
        this.mReadingGrpNum = new AtomicInteger(0);
        this.mReadingSneNum = new AtomicInteger(0);
        this.mReadingIapNum = new AtomicInteger(0);
        this.mNeedNotify = new AtomicBoolean(false);
        this.mEfData = null;
        this.mFh = iccFileHandler;
        this.mPhoneBookRecords = new ArrayList<>();
        this.mGasForGrp = new ArrayList<>();
        this.mIapFileList = new ArrayList<>();
        this.mPbrRecords = null;
        this.mIsPbrPresent = true;
        this.mAdnCache = (MtkAdnRecordCache) adnRecordCache;
        this.mCi = (MtkRIL) commandsInterface;
        this.mCurrentApp = uiccCardApplication;
        this.mSlotId = uiccCardApplication == null ? -1 : uiccCardApplication.getPhoneId();
        this.mEmailsForAdnRec = new SparseArray();
        this.mSfiEfidTable = new SparseIntArray();
        for (int i = 0; i < 8; i++) {
            this.mUpbCap[i] = -1;
        }
        logi("constructor finished. ");
    }

    public void reset() {
        this.mIsReset = true;
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
            this.mUpbCap[i] = -1;
        }
        this.mEmailRecordSize = -1;
        this.mAnrRecordSize = -1;
        this.mUpbDone = -1;
        this.mAdnRecordSize = null;
        this.mRefreshEmailInfo = false;
        this.mRefreshAnrInfo = false;
        this.mRefreshAdnInfo = false;
        synchronized (this.mLock) {
            this.mLock.notifyAll();
        }
        this.mPbrNeedNotify = -1;
        logi("reset finished. ");
    }

    public ArrayList<MtkAdnRecord> loadEfFilesFromUsim(Object obj) {
        int[] eFLinearRecordSize;
        synchronized (this.mLock) {
            this.mIsReset = false;
            long jCurrentTimeMillis = System.currentTimeMillis();
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
                this.mPbrNeedNotify++;
                readPbrFileAndWait();
            }
            if (this.mPbrRecords != null && this.mPbrRecords.size() != 0) {
                logi("loadEfFilesFromUsim mPbrNeedNotify:" + this.mPbrNeedNotify);
                if (this.mEmailRecordSize < 0) {
                    readEmailRecordSize();
                }
                if (this.mAnrRecordSize < 0) {
                    readAnrRecordSize();
                }
                int efid = ((File) this.mPbrRecords.get(0).mFileIds.get(192)).getEfid();
                if (efid > 0 && (eFLinearRecordSize = readEFLinearRecordSize(efid)) != null && eFLinearRecordSize.length == 3) {
                    this.mAdnFileSize = eFLinearRecordSize[2];
                }
                if (this.mPbrRecords.get(0).mFileIds.get(195) != null) {
                    readEFLinearRecordSize(((File) this.mPbrRecords.get(0).mFileIds.get(195)).getEfid());
                }
                int size = this.mPbrRecords.size();
                if (!CsimPhbUtil.hasModemPhbEnhanceCapability(this.mFh)) {
                    for (int i = 0; i < size; i++) {
                        readAASFileAndWait(i);
                        readAdnFileAndWaitForUICC(i);
                    }
                } else {
                    readAasFileAndWaitOptmz();
                    readAdnFileAndWait(0);
                }
                if (this.mPhoneBookRecords.isEmpty()) {
                    logi("loadEfFilesFromUsim mPhoneBookRecords Empty");
                    return this.mPhoneBookRecords;
                }
                if (!CsimPhbUtil.hasModemPhbEnhanceCapability(this.mFh)) {
                    for (int i2 = 0; i2 < size; i2++) {
                        readSneFileAndWait(i2);
                        readAnrFileAndWait(i2);
                        readEmailFileAndWait(i2);
                    }
                } else {
                    logi("loadEfFilesFromUsim Speed up read begin");
                    readSneFileAndWaitOptmz();
                    readAnrFileAndWaitOptmz();
                    readEmailFileAndWaitOptmz();
                    logi("loadEfFilesFromUsim Speed up read end");
                }
                readGrpIdsAndWait();
                if (this.mPbrRecords != null) {
                    this.mUpbDone = 1;
                    logi("loadEfFilesFromUsim Time: " + (System.currentTimeMillis() - jCurrentTimeMillis) + " AppType: " + this.mCurrentApp.getType());
                } else {
                    logi("loadEfFilesFromUsim end");
                }
                return this.mPhoneBookRecords;
            }
            if (checkIsPhbReady() && !this.mIsReset) {
                readAdnFileAndWait(0);
                this.mEmailRecordSize = 0;
                this.mAnrRecordSize = 0;
                this.mUpbDone = 1;
                this.mIsPbrPresent = false;
                logi("loadEfFilesFromUsim getRecordIfLoaded EF_ADN");
                return this.mAdnCache.getRecordsIfLoaded(28474, null);
            }
            logi("loadEfFilesFromUsim phb not ready and Reset");
            return null;
        }
    }

    private void readEmailFileAndWait(int i) {
        SparseArray sparseArray;
        File file;
        logi("readEmailFileAndWait " + i);
        if (this.mPbrRecords != null && (sparseArray = this.mPbrRecords.get(i).mFileIds) != null && (file = (File) sparseArray.get(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP)) != null) {
            file.getEfid();
            if (file.getParentTag() == 168) {
                readType1Ef(file, 0);
            } else if (file.getParentTag() == 169) {
                readType2Ef(file);
            }
        }
    }

    private void readIapFileAndWait(int i, int i2, boolean z) {
        int[] eFLinearRecordSize;
        logi("readIapFileAndWait pbrIndex :" + i + ",efid:" + i2 + ",forceRefresh:" + z);
        if (i2 <= 0) {
            return;
        }
        if (this.mIapFileList == null) {
            logi("readIapFileAndWait IapFileList is null !!!! recreate it !");
            this.mIapFileList = new ArrayList<>();
        }
        if (this.mRecordSize != null && this.mRecordSize.get(i2) != null) {
            eFLinearRecordSize = this.mRecordSize.get(i2);
        } else {
            eFLinearRecordSize = readEFLinearRecordSize(i2);
        }
        if (eFLinearRecordSize == null || eFLinearRecordSize.length != 3) {
            Rlog.e(LOG_TAG, "readIapFileAndWait: read record size error.");
            this.mIapFileList.add(i, new ArrayList<>());
            return;
        }
        if (this.mIapFileList.size() <= i) {
            log("Create IAP first!");
            ArrayList<byte[]> arrayList = new ArrayList<>();
            for (int i3 = 0; i3 < this.mAdnFileSize; i3++) {
                byte[] bArr = new byte[eFLinearRecordSize[0]];
                for (byte b : bArr) {
                }
                arrayList.add(bArr);
            }
            this.mIapFileList.add(i, arrayList);
        } else {
            log("This IAP has been loaded!");
            if (!z) {
                return;
            }
        }
        int size = this.mPhoneBookRecords.size();
        int i4 = this.mAdnFileSize * i;
        int i5 = this.mAdnFileSize + i4;
        if (size < i5) {
            i5 = size;
        }
        log("readIapFileAndWait nOffset " + i4 + ", nMax " + i5);
        int i6 = 0;
        for (int i7 = i4; i7 < i5; i7++) {
            try {
                MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i7);
                if (mtkAdnRecord.getAlphaTag().length() > 0 || mtkAdnRecord.getNumber().length() > 0) {
                    this.mReadingIapNum.addAndGet(1);
                    this.mFh.readEFLinearFixed(i2, (i7 + 1) - i4, eFLinearRecordSize[0], obtainMessage(14, new int[]{i, i7 - i4}));
                    i6++;
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "readIapFileAndWait: mPhoneBookRecords IndexOutOfBoundsException numAdnRecs is " + size + "index is " + i7);
            }
        }
        if (this.mReadingIapNum.get() == 0) {
            this.mNeedNotify.set(false);
            return;
        }
        this.mNeedNotify.set(true);
        logi("readIapFileAndWait before mLock.wait " + this.mNeedNotify.get() + " total:" + i6);
        synchronized (this.mLock) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e2) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readIapFileAndWait");
            }
        }
        logi("readIapFileAndWait after mLock.wait after mLock.wait:" + this.mNeedNotify.get());
        if (true == this.mNeedNotify.get()) {
            this.mNeedNotify.set(false);
        }
    }

    private void readAASFileAndWait(int i) {
        SparseArray sparseArray;
        File file;
        logi("readAASFileAndWait " + i);
        if (this.mPbrRecords == null || (sparseArray = this.mPbrRecords.get(i).mFileIds) == null || (file = (File) sparseArray.get(199)) == null) {
            return;
        }
        int efid = file.getEfid();
        log("readAASFileAndWait-get AAS EFID " + efid);
        if (this.mAasForAnr != null) {
            logi("AAS has been loaded for Pbr number " + i);
        }
        if (this.mFh != null) {
            Message messageObtainMessage = obtainMessage(5);
            messageObtainMessage.arg1 = i;
            this.mFh.loadEFLinearFixedAll(efid, messageObtainMessage);
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

    private void readSneFileAndWait(int i) {
        SparseArray sparseArray;
        File file;
        logi("readSneFileAndWait " + i);
        if (this.mPbrRecords == null || (sparseArray = this.mPbrRecords.get(i).mFileIds) == null || (file = (File) sparseArray.get(195)) == null) {
            return;
        }
        log("readSneFileAndWait: EFSNE id is " + file.getEfid());
        if (file.getParentTag() == 169) {
            readType2Ef(file);
        } else if (file.getParentTag() == 168) {
            readType1Ef(file, 0);
        }
    }

    private void readAnrFileAndWait(int i) {
        logi("readAnrFileAndWait: recId is " + i);
        if (this.mPbrRecords == null) {
            return;
        }
        SparseArray sparseArray = this.mPbrRecords.get(i).mFileIds;
        if (sparseArray == null) {
            log("readAnrFileAndWait: No anr tag in pbr record " + i);
            return;
        }
        for (int i2 = 0; i2 < this.mPbrRecords.get(i).mAnrIndex; i2++) {
            File file = (File) sparseArray.get(196 + (i2 * 256));
            if (file != null) {
                if (file.getParentTag() == 169) {
                    file.mAnrIndex = i2;
                    readType2Ef(file);
                    return;
                } else {
                    if (file.getParentTag() == 168) {
                        file.mAnrIndex = i2;
                        readType1Ef(file, i2);
                        return;
                    }
                    return;
                }
            }
        }
    }

    private void readGrpIdsAndWait() {
        SparseArray sparseArray;
        logi("readGrpIdsAndWait begin");
        if (this.mPbrRecords == null || (sparseArray = this.mPbrRecords.get(0).mFileIds) == null || ((File) sparseArray.get(198)) == null) {
            return;
        }
        int size = this.mPhoneBookRecords.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            try {
                MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i2);
                if (mtkAdnRecord.getAlphaTag().length() > 0 || mtkAdnRecord.getNumber().length() > 0) {
                    this.mReadingGrpNum.incrementAndGet();
                    int recId = mtkAdnRecord.getRecId();
                    this.mCi.readUPBGrpEntry(recId, obtainMessage(17, new int[]{i2, recId}));
                    i++;
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "readGrpIdsAndWait: mPhoneBookRecords IndexOutOfBoundsException numAdnRecs is " + size + "index is " + i2);
            }
        }
        if (this.mReadingGrpNum.get() == 0) {
            this.mNeedNotify.set(false);
            return;
        }
        this.mNeedNotify.set(true);
        logi("readGrpIdsAndWait before mLock.wait " + this.mNeedNotify.get() + " total:" + i);
        try {
            this.mLock.wait();
        } catch (InterruptedException e2) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readGrpIdsAndWait");
        }
        logi("readGrpIdsAndWait after mLock.wait after mLock.wait " + this.mNeedNotify.get());
        if (true == this.mNeedNotify.get()) {
            this.mNeedNotify.set(false);
        }
    }

    private void readAdnFileAndWait(int i) {
        logi("readAdnFileAndWait begin: recId is " + i + "");
        int size = this.mPhoneBookRecords.size();
        this.mAdnCache.requestLoadAllAdnLike(28474, this.mAdnCache.extensionEfForEf(28474), obtainMessage(2));
        try {
            this.mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readAdnFileAndWait");
        }
        if (this.mPbrRecords != null && this.mPbrRecords.size() > i) {
            this.mPbrRecords.get(i).mMasterFileRecordNum = this.mPhoneBookRecords.size() - size;
        }
        logi("readAdnFileAndWait end: recId is " + i + "");
    }

    private void createPbrFile(ArrayList<byte[]> arrayList) {
        int sfi;
        if (arrayList == null) {
            this.mPbrRecords = null;
            this.mIsPbrPresent = false;
            return;
        }
        this.mPbrRecords = new ArrayList<>();
        this.mSliceCount = 0;
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

    private void readAasFileAndWaitOptmz() {
        int i;
        File file;
        logi("readAasFileAndWaitOptmz begin");
        if (this.mAasForAnr == null || this.mAasForAnr.size() == 0) {
            if (this.mUpbCap[3] < 0) {
                if (this.mPbrRecords == null) {
                    return;
                }
                i = 0;
                SparseArray sparseArray = this.mPbrRecords.get(0).mFileIds;
                if (sparseArray == null || (file = (File) sparseArray.get(199)) == null) {
                    return;
                }
                int[] eFLinearRecordSize = readEFLinearRecordSize(file.getEfid());
                if (eFLinearRecordSize != null && eFLinearRecordSize.length == 3) {
                    i = eFLinearRecordSize[2];
                }
            } else {
                i = this.mUpbCap[3];
            }
            int i2 = 5;
            if (i <= 5) {
                i2 = i;
            }
            this.mCi.readUPBAasList(1, i2, obtainMessage(28));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readAasFileAndWaitOptmz");
            }
        }
        logi("readAasFileAndWaitOptmz end");
    }

    private void readEmailFileAndWaitOptmz() {
        SparseArray sparseArray;
        if (this.mPbrRecords == null || (sparseArray = this.mPbrRecords.get(0).mFileIds) == null || ((File) sparseArray.get(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP)) == null) {
            return;
        }
        int size = this.mPhoneBookRecords.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            try {
                MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i2);
                if (mtkAdnRecord.getAlphaTag().length() > 0 || mtkAdnRecord.getNumber().length() > 0) {
                    this.mReadingEmailNum.incrementAndGet();
                    this.mCi.readUPBEmailEntry(i2 + 1, 1, obtainMessage(22, new int[]{0, i2}));
                    i++;
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "readEmailFileAndWaitOptmz: mPhoneBookRecords IndexOutOfBoundsnumAdnRecs is " + size + "index is " + i2);
            }
        }
        if (this.mReadingEmailNum.get() == 0) {
            this.mNeedNotify.set(false);
            return;
        }
        this.mNeedNotify.set(true);
        logi("readEmailFileAndWaitOptmz before mLock.wait " + this.mNeedNotify.get() + " total:" + i);
        synchronized (this.mLock) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e2) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readEmailFileAndWaitOptmz");
            }
        }
        logi("readEmailFileAndWaitOptmz after mLock.wait: " + this.mNeedNotify.get());
        if (true == this.mNeedNotify.get()) {
            this.mNeedNotify.set(false);
        }
    }

    private void readAnrFileAndWaitOptmz() {
        SparseArray sparseArray;
        if (this.mPbrRecords == null || (sparseArray = this.mPbrRecords.get(0).mFileIds) == null || ((File) sparseArray.get(196)) == null) {
            return;
        }
        int size = this.mPhoneBookRecords.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            try {
                MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i2);
                if (mtkAdnRecord.getAlphaTag().length() > 0 || mtkAdnRecord.getNumber().length() > 0) {
                    this.mReadingAnrNum.addAndGet(1);
                    this.mCi.readUPBAnrEntry(i2 + 1, 1, obtainMessage(23, new int[]{0, i2, 0}));
                    i++;
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "readAnrFileAndWaitOptmz: mPhoneBookRecords IndexOutOfBoundsnumAdnRecs is " + size + "index is " + i2);
            }
        }
        if (this.mReadingAnrNum.get() == 0) {
            this.mNeedNotify.set(false);
            return;
        }
        this.mNeedNotify.set(true);
        logi("readAnrFileAndWaitOptmz before mLock.wait " + this.mNeedNotify.get() + " total:" + i);
        synchronized (this.mLock) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e2) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readAnrFileAndWaitOptmz");
            }
        }
        logi("readAnrFileAndWaitOptmz after mLock.wait:" + this.mNeedNotify.get());
        if (true == this.mNeedNotify.get()) {
            this.mNeedNotify.set(false);
        }
    }

    private void readSneFileAndWaitOptmz() {
        SparseArray sparseArray;
        if (this.mPbrRecords == null || (sparseArray = this.mPbrRecords.get(0).mFileIds) == null || ((File) sparseArray.get(195)) == null) {
            return;
        }
        int size = this.mPhoneBookRecords.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            try {
                MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i2);
                if (mtkAdnRecord.getAlphaTag().length() > 0 || mtkAdnRecord.getNumber().length() > 0) {
                    this.mReadingSneNum.incrementAndGet();
                    this.mCi.readUPBSneEntry(i2 + 1, 1, obtainMessage(24, new int[]{0, i2}));
                    i++;
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "readSneFileAndWaitOptmz: mPhoneBookRecords IndexOutOfBoundsnumAdnRecs is " + size + "index is " + i2);
            }
        }
        if (this.mReadingSneNum.get() == 0) {
            this.mNeedNotify.set(false);
            return;
        }
        this.mNeedNotify.set(true);
        logi("readSneFileAndWaitOptmz before mLock.wait " + this.mNeedNotify.get() + " total:" + i);
        synchronized (this.mLock) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e2) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readSneFileAndWaitOptmz");
            }
        }
        logi("readSneFileAndWaitOptmz after mLock.wait: " + this.mNeedNotify.get());
        if (true == this.mNeedNotify.get()) {
            this.mNeedNotify.set(false);
        }
    }

    private void updatePhoneAdnRecordWithEmailByIndexOptmz(int i, int i2, String str) {
        log("updatePhoneAdnRecordWithEmailByIndex emailIndex = " + i + ",adnIndex = " + i2 + ", email = " + str);
        if (str != null && str != null) {
            try {
                if (!str.equals("")) {
                    this.mPhoneBookRecords.get(i2).setEmails(new String[]{str});
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "[JE]updatePhoneAdnRecordWithEmailByIndex " + e.getMessage());
            }
        }
    }

    private void updatePhoneAdnRecordWithAnrByIndexOptmz(int i, int i2, int i3, PhbEntry phbEntry) {
        String strPrependPlusToNumber;
        log("updatePhoneAdnRecordWithAnrByIndexOptmz the " + i2 + "th anr record is " + phbEntry);
        if (phbEntry != null && phbEntry.number != null && !phbEntry.number.equals("")) {
            if (phbEntry.ton == 145) {
                strPrependPlusToNumber = MtkPhoneNumberUtils.prependPlusToNumber(phbEntry.number);
            } else {
                strPrependPlusToNumber = phbEntry.number;
            }
            String strReplace = strPrependPlusToNumber.replace('?', 'N').replace('p', ',').replace('w', ';');
            int i4 = phbEntry.index;
            if (strReplace != null && !strReplace.equals("")) {
                String str = null;
                if (i4 > 0 && i4 != 255 && this.mAasForAnr != null && i4 <= this.mAasForAnr.size()) {
                    str = this.mAasForAnr.get(i4 - 1);
                }
                log(" updatePhoneAdnRecordWithAnrByIndex " + i2 + " th anr is " + strReplace + " the anrIndex is " + i3);
                try {
                    MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i2);
                    mtkAdnRecord.setAnr(strReplace, i3);
                    if (str != null && str.length() > 0) {
                        mtkAdnRecord.setAasIndex(i4);
                    }
                    this.mPhoneBookRecords.set(i2, mtkAdnRecord);
                } catch (IndexOutOfBoundsException e) {
                    Rlog.e(LOG_TAG, "updatePhoneAdnRecordWithAnrByIndex: mPhoneBookRecords IndexOutOfBoundsException size: " + this.mPhoneBookRecords.size() + "index: " + i2);
                }
            }
        }
    }

    private String[] buildAnrRecordOptmz(String str, int i) {
        int i2;
        if (str.indexOf(43) != -1) {
            if (str.indexOf(43) != str.lastIndexOf(43)) {
                Rlog.w(LOG_TAG, "There are multiple '+' in the number: " + str);
            }
            i2 = 145;
            str = str.replace("+", "");
        } else {
            i2 = 129;
        }
        return new String[]{str.replace('N', '?').replace(',', 'p').replace(';', 'w'), Integer.toString(i2), Integer.toString(i)};
    }

    private void updatePhoneAdnRecordWithSneByIndexOptmz(int i, String str) {
        if (str == null) {
            return;
        }
        log("updatePhoneAdnRecordWithSneByIndex index " + i + " recData file is " + str);
        if (str != null && !str.equals("")) {
            try {
                this.mPhoneBookRecords.get(i).setSne(str);
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "updatePhoneAdnRecordWithSneByIndex: mPhoneBookRecords IndexOutOfBoundsException size() is " + this.mPhoneBookRecords.size() + "index is " + i);
            }
        }
    }

    public void handleMessage(Message message) {
        ArrayList arrayList;
        String[] strArr;
        String[] strArr2;
        ArrayList<byte[]> arrayList2;
        int i = message.what;
        int i2 = 0;
        switch (i) {
            case 1:
                logi("handleMessage: EVENT_PBR_LOAD_DONE:" + this.mPbrNeedNotify);
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.exception == null) {
                    createPbrFile((ArrayList) asyncResult.result);
                }
                if (this.mPbrNeedNotify != -1) {
                    synchronized (this.mLock) {
                        this.mLock.notify();
                        break;
                    }
                    this.mPbrNeedNotify--;
                    return;
                }
                return;
            case 2:
                logi("Loading USIM ADN records done");
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                if (asyncResult2.exception == null && this.mPhoneBookRecords != null) {
                    if (!CsimPhbUtil.hasModemPhbEnhanceCapability(this.mFh) && this.mPhoneBookRecords.size() > 0 && asyncResult2.result != null) {
                        ArrayList<MtkAdnRecord> arrayListChangeAdnRecordNumber = changeAdnRecordNumber(this.mPhoneBookRecords.size(), (ArrayList) asyncResult2.result);
                        this.mPhoneBookRecords.addAll(arrayListChangeAdnRecordNumber);
                        CsimPhbUtil.initPhbStorage(arrayListChangeAdnRecordNumber);
                    } else if (asyncResult2.result != null) {
                        this.mPhoneBookRecords.addAll((ArrayList) asyncResult2.result);
                        if (!CsimPhbUtil.hasModemPhbEnhanceCapability(this.mFh)) {
                            CsimPhbUtil.initPhbStorage((ArrayList) asyncResult2.result);
                        }
                        log("Loading USIM ADN records " + this.mPhoneBookRecords.size());
                    } else {
                        log("Loading USIM ADN records ar.result:" + asyncResult2.result);
                    }
                } else {
                    Rlog.w(LOG_TAG, "Loading USIM ADN records fail.");
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 3:
                logi("Loading USIM IAP records done");
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
                logi("Loading USIM Email records done");
                AsyncResult asyncResult4 = (AsyncResult) message.obj;
                if (asyncResult4.exception == null) {
                    this.mEmailFileRecord = (ArrayList) asyncResult4.result;
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 5:
                AsyncResult asyncResult5 = (AsyncResult) message.obj;
                logi("EVENT_AAS_LOAD_DONE done pbr " + message.arg1);
                if (asyncResult5.exception == null && (arrayList = (ArrayList) asyncResult5.result) != null) {
                    int size = arrayList.size();
                    ArrayList<String> arrayList3 = new ArrayList<>();
                    for (int i3 = 0; i3 < size; i3++) {
                        byte[] bArr = (byte[]) arrayList.get(i3);
                        if (bArr == null) {
                            arrayList3.add(null);
                        } else {
                            String strAdnStringFieldToString = IccUtils.adnStringFieldToString(bArr, 0, bArr.length);
                            log("AAS[" + i3 + "]=" + strAdnStringFieldToString + ",byte=" + IccUtils.bytesToHexString(bArr));
                            arrayList3.add(strAdnStringFieldToString);
                        }
                    }
                    this.mAasForAnr = arrayList3;
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 6:
                logi("Load UPB GAS done");
                AsyncResult asyncResult6 = (AsyncResult) message.obj;
                if (asyncResult6.exception == null && (strArr = (String[]) asyncResult6.result) != null && strArr.length > 0) {
                    this.mGasForGrp = new ArrayList<>();
                    while (i2 < strArr.length) {
                        String strDecodeGas = decodeGas(strArr[i2]);
                        int i4 = i2 + 1;
                        this.mGasForGrp.add(new UsimGroup(i4, strDecodeGas));
                        log("Load UPB GAS done i is " + i2 + ", gas is " + strDecodeGas);
                        i2 = i4;
                    }
                }
                synchronized (this.mGasLock) {
                    this.mGasLock.notify();
                    break;
                }
                return;
            case 7:
                logi("Updating USIM IAP records done");
                if (((AsyncResult) message.obj).exception == null) {
                    log("Updating USIM IAP records successfully!");
                    return;
                }
                return;
            case 8:
                logi("Updating USIM Email records done");
                AsyncResult asyncResult7 = (AsyncResult) message.obj;
                if (asyncResult7.exception == null) {
                    log("Updating USIM Email records successfully!");
                    this.mRefreshEmailInfo = true;
                } else {
                    Rlog.e(LOG_TAG, "EVENT_EMAIL_UPDATE_DONE exception", asyncResult7.exception);
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 9:
                logi("Updating USIM ANR records done");
                AsyncResult asyncResult8 = (AsyncResult) message.obj;
                IccIoResult iccIoResult = (IccIoResult) asyncResult8.result;
                if (asyncResult8.exception != null) {
                    Rlog.e(LOG_TAG, "EVENT_ANR_UPDATE_DONE exception", asyncResult8.exception);
                } else if (iccIoResult != null) {
                    if (iccIoResult.getException() == null) {
                        log("Updating USIM ANR records successfully!");
                        this.mRefreshAnrInfo = true;
                    }
                } else {
                    this.mRefreshAnrInfo = true;
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 10:
                logi("EVENT_AAS_UPDATE_DONE done.");
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 11:
                logi("update UPB SNE done");
                AsyncResult asyncResult9 = (AsyncResult) message.obj;
                if (asyncResult9.exception != null) {
                    Rlog.e(LOG_TAG, "EVENT_SNE_UPDATE_DONE exception", asyncResult9.exception);
                    CommandException commandException = asyncResult9.exception;
                    if (commandException.getCommandError() == CommandException.Error.OEM_ERROR_2) {
                        this.mResult = -40;
                    } else if (commandException.getCommandError() == CommandException.Error.OEM_ERROR_3) {
                        this.mResult = -30;
                    } else {
                        this.mResult = -50;
                    }
                } else {
                    this.mResult = 0;
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 12:
                logi("update UPB GRP done");
                if (((AsyncResult) message.obj).exception == null) {
                    this.mResult = 0;
                } else {
                    this.mResult = -1;
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 13:
                logi("update UPB GAS done");
                AsyncResult asyncResult10 = (AsyncResult) message.obj;
                if (asyncResult10.exception == null) {
                    this.mResult = 0;
                } else {
                    CommandException commandException2 = asyncResult10.exception;
                    if (commandException2.getCommandError() == CommandException.Error.OEM_ERROR_2) {
                        this.mResult = -10;
                    } else if (commandException2.getCommandError() == CommandException.Error.OEM_ERROR_3) {
                        this.mResult = -20;
                    } else {
                        this.mResult = -1;
                    }
                }
                logi("update UPB GAS done mResult is " + this.mResult);
                synchronized (this.mGasLock) {
                    this.mGasLock.notify();
                    break;
                }
                return;
            case 14:
                AsyncResult asyncResult11 = (AsyncResult) message.obj;
                int[] iArr = (int[]) asyncResult11.userObj;
                IccIoResult iccIoResult2 = (IccIoResult) asyncResult11.result;
                boolean z = this.mNeedNotify.get();
                if (iccIoResult2 != null && this.mIapFileList != null && iccIoResult2.getException() == null) {
                    log("Loading USIM Iap record done result is " + IccUtils.bytesToHexString(iccIoResult2.payload));
                    try {
                        ArrayList<byte[]> arrayList4 = this.mIapFileList.get(iArr[0]);
                        if (arrayList4.size() > 0) {
                            arrayList4.set(iArr[1], iccIoResult2.payload);
                        } else {
                            Rlog.w(LOG_TAG, "Warning: IAP size is 0");
                        }
                    } catch (IndexOutOfBoundsException e) {
                        Rlog.e(LOG_TAG, "Index out of bounds.");
                    }
                    break;
                }
                this.mReadingIapNum.decrementAndGet();
                log("haman, mReadingIapNum when load done after minus: " + this.mReadingIapNum.get() + ",mNeedNotify " + this.mNeedNotify.get() + ", Iap pbr:" + iArr[0] + ", adn i:" + iArr[1]);
                if (this.mReadingIapNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                            break;
                        }
                    }
                    logi("EVENT_IAP_RECORD_LOAD_DONE end mLock.notify:" + z);
                    return;
                }
                return;
            case 15:
                AsyncResult asyncResult12 = (AsyncResult) message.obj;
                int[] iArr2 = (int[]) asyncResult12.userObj;
                IccIoResult iccIoResult3 = (IccIoResult) asyncResult12.result;
                log("Loading USIM email record done email index:" + iArr2[0] + ", adn i:" + iArr2[1]);
                if (iccIoResult3 != null && iccIoResult3.getException() == null) {
                    log("Loading USIM Email record done result is " + IccUtils.bytesToHexString(iccIoResult3.payload));
                    updatePhoneAdnRecordWithEmailByIndex(iArr2[0], iArr2[1], iccIoResult3.payload);
                }
                this.mReadingEmailNum.decrementAndGet();
                log("haman, mReadingEmailNum when load done after minus: " + this.mReadingEmailNum.get() + ", mNeedNotify:" + this.mNeedNotify.get());
                if (this.mReadingEmailNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                            break;
                        }
                    }
                    logi("EVENT_EMAIL_RECORD_LOAD_DONE end mLock.notify");
                    return;
                }
                return;
            case 16:
                AsyncResult asyncResult13 = (AsyncResult) message.obj;
                int[] iArr3 = (int[]) asyncResult13.userObj;
                IccIoResult iccIoResult4 = (IccIoResult) asyncResult13.result;
                if (iccIoResult4 != null && iccIoResult4.getException() == null) {
                    updatePhoneAdnRecordWithAnrByIndex(iArr3[0], iArr3[1], iArr3[2], iccIoResult4.payload);
                }
                this.mReadingAnrNum.decrementAndGet();
                log("haman, mReadingAnrNum when load done after minus: " + this.mReadingAnrNum.get() + ", mNeedNotify:" + this.mNeedNotify.get());
                if (this.mReadingAnrNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                            break;
                        }
                    }
                    logi("EVENT_ANR_RECORD_LOAD_DONE end mLock.notify");
                    return;
                }
                return;
            case 17:
                AsyncResult asyncResult14 = (AsyncResult) message.obj;
                int[] iArr4 = (int[]) asyncResult14.userObj;
                boolean z2 = this.mNeedNotify.get();
                if (asyncResult14.result != null) {
                    int[] iArr5 = (int[]) asyncResult14.result;
                    if (iArr5.length > 0) {
                        updatePhoneAdnRecordWithGrpByIndex(iArr4[0], iArr4[1], iArr5);
                    }
                }
                this.mReadingGrpNum.decrementAndGet();
                log("haman, mReadingGrpNum when load done after minus: " + this.mReadingGrpNum.get() + ",mNeedNotify:" + z2);
                if (this.mReadingGrpNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                            break;
                        }
                    }
                    logi("EVENT_GRP_RECORD_LOAD_DONE end mLock.notify:" + z2);
                    return;
                }
                return;
            case 18:
                logi("Loading USIM SNE record done");
                AsyncResult asyncResult15 = (AsyncResult) message.obj;
                int[] iArr6 = (int[]) asyncResult15.userObj;
                IccIoResult iccIoResult5 = (IccIoResult) asyncResult15.result;
                if (iccIoResult5 != null && iccIoResult5.getException() == null) {
                    log("Loading USIM SNE record done result is " + IccUtils.bytesToHexString(iccIoResult5.payload));
                    updatePhoneAdnRecordWithSneByIndex(iArr6[0], iArr6[1], iccIoResult5.payload);
                }
                this.mReadingSneNum.decrementAndGet();
                log("haman, mReadingSneNum when load done after minus: " + this.mReadingSneNum.get() + ",mNeedNotify:" + this.mNeedNotify.get());
                if (this.mReadingSneNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                            break;
                        }
                    }
                    logi("EVENT_SNE_RECORD_LOAD_DONE end mLock.notify");
                    return;
                }
                return;
            case 19:
                logi("Query UPB capability done");
                AsyncResult asyncResult16 = (AsyncResult) message.obj;
                if (asyncResult16.exception == null) {
                    this.mUpbCap = (int[]) asyncResult16.result;
                }
                synchronized (this.mUPBCapabilityLock) {
                    this.mUPBCapabilityLock.notify();
                    break;
                }
                return;
            case 20:
                AsyncResult asyncResult17 = (AsyncResult) message.obj;
                if (asyncResult17.exception == null) {
                    this.mEfData = (EFResponseData) asyncResult17.result;
                } else {
                    Rlog.w(LOG_TAG, "Select EF file fail" + asyncResult17.exception);
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 21:
                logi("EVENT_QUERY_PHB_ADN_INFO");
                AsyncResult asyncResult18 = (AsyncResult) message.obj;
                if (asyncResult18.exception == null) {
                    int[] iArr7 = (int[]) asyncResult18.result;
                    if (iArr7 != null && iArr7.length == 4) {
                        this.mAdnRecordSize = new int[4];
                        this.mAdnRecordSize[0] = iArr7[0];
                        this.mAdnRecordSize[1] = iArr7[1];
                        this.mAdnRecordSize[2] = iArr7[2];
                        this.mAdnRecordSize[3] = iArr7[3];
                        log("recordSize[0]=" + this.mAdnRecordSize[0] + ",recordSize[1]=" + this.mAdnRecordSize[1] + ",recordSize[2]=" + this.mAdnRecordSize[2] + ",recordSize[3]=" + this.mAdnRecordSize[3]);
                    } else {
                        this.mAdnRecordSize = new int[4];
                        this.mAdnRecordSize[0] = 0;
                        this.mAdnRecordSize[1] = 0;
                        this.mAdnRecordSize[2] = 0;
                        this.mAdnRecordSize[3] = 0;
                    }
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 22:
                AsyncResult asyncResult19 = (AsyncResult) message.obj;
                int[] iArr8 = (int[]) asyncResult19.userObj;
                String str = (String) asyncResult19.result;
                boolean z3 = this.mNeedNotify.get();
                if (str != null && asyncResult19.exception == null) {
                    log("Loading USIM Email record done result is " + str);
                    updatePhoneAdnRecordWithEmailByIndexOptmz(iArr8[0], iArr8[1], str);
                }
                this.mReadingEmailNum.decrementAndGet();
                log("haman, mReadingEmailNum when load done after minus: " + this.mReadingEmailNum.get() + ", mNeedNotify:" + this.mNeedNotify.get() + ", email index:" + iArr8[0] + ", adn i:" + iArr8[1]);
                if (this.mReadingEmailNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                            break;
                        }
                    }
                    logi("EVENT_EMAIL_RECORD_LOAD_OPTMZ_DONE end mLock.notify:" + z3);
                    return;
                }
                return;
            case 23:
                AsyncResult asyncResult20 = (AsyncResult) message.obj;
                int[] iArr9 = (int[]) asyncResult20.userObj;
                PhbEntry[] phbEntryArr = (PhbEntry[]) asyncResult20.result;
                boolean z4 = this.mNeedNotify.get();
                if (phbEntryArr != null && asyncResult20.exception == null) {
                    log("Loading USIM Anr record done result is " + phbEntryArr[0]);
                    updatePhoneAdnRecordWithAnrByIndexOptmz(iArr9[0], iArr9[1], iArr9[2], phbEntryArr[0]);
                }
                this.mReadingAnrNum.decrementAndGet();
                log("haman, mReadingAnrNum when load done after minus: " + this.mReadingAnrNum.get() + ", mNeedNotify:" + this.mNeedNotify.get() + ", anr index:" + iArr9[2] + ", adn i:" + iArr9[1]);
                if (this.mReadingAnrNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                            break;
                        }
                    }
                    logi("EVENT_ANR_RECORD_LOAD_OPTMZ_DONE end mLock.notify:" + z4);
                    return;
                }
                return;
            case 24:
                AsyncResult asyncResult21 = (AsyncResult) message.obj;
                int[] iArr10 = (int[]) asyncResult21.userObj;
                String str2 = (String) asyncResult21.result;
                boolean z5 = this.mNeedNotify.get();
                if (str2 != null && asyncResult21.exception == null) {
                    String strDecodeGas2 = decodeGas(str2);
                    log("Loading USIM Sne record done result is " + strDecodeGas2);
                    updatePhoneAdnRecordWithSneByIndexOptmz(iArr10[1], strDecodeGas2);
                }
                this.mReadingSneNum.decrementAndGet();
                log("haman, mReadingSneNum when load done after minus: " + this.mReadingSneNum.get() + ", mNeedNotify:" + this.mNeedNotify.get() + ", sne index:" + iArr10[0] + ", adn i:" + iArr10[1]);
                if (this.mReadingSneNum.get() == 0) {
                    if (this.mNeedNotify.get()) {
                        this.mNeedNotify.set(false);
                        synchronized (this.mLock) {
                            this.mLock.notify();
                            break;
                        }
                    }
                    logi("EVENT_SNE_RECORD_LOAD_OPTMZ_DONE end mLock.notify:" + z5);
                    return;
                }
                return;
            case 25:
                AsyncResult asyncResult22 = (AsyncResult) message.obj;
                if (asyncResult22.exception == null) {
                    this.mEmailInfo = (int[]) asyncResult22.result;
                    if (this.mEmailInfo == null) {
                        log("mEmailInfo Null!");
                    } else {
                        logi("mEmailInfo = " + this.mEmailInfo[0] + " " + this.mEmailInfo[1] + " " + this.mEmailInfo[2]);
                    }
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 26:
                AsyncResult asyncResult23 = (AsyncResult) message.obj;
                int[] iArr11 = (int[]) asyncResult23.result;
                if (asyncResult23.exception == null) {
                    if (iArr11 == null) {
                        log("tmpAnrInfo Null!");
                    } else {
                        logi("tmpAnrInfo = " + iArr11[0] + " " + iArr11[1] + " " + iArr11[2]);
                        if (this.mAnrInfo == null) {
                            this.mAnrInfo = new ArrayList<>();
                        } else if (this.mAnrInfo.size() > 0) {
                            this.mAnrInfo.clear();
                        }
                        this.mAnrInfo.add(iArr11);
                    }
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 27:
                AsyncResult asyncResult24 = (AsyncResult) message.obj;
                if (asyncResult24.exception == null) {
                    this.mSneInfo = (int[]) asyncResult24.result;
                    if (this.mSneInfo == null) {
                        log("mSneInfo Null!");
                    } else {
                        logi("mSneInfo = " + this.mSneInfo[0] + " " + this.mSneInfo[1] + " " + this.mSneInfo[2]);
                    }
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 28:
                logi("Load UPB AAS done");
                AsyncResult asyncResult25 = (AsyncResult) message.obj;
                if (asyncResult25.exception == null && (strArr2 = (String[]) asyncResult25.result) != null && strArr2.length > 0) {
                    this.mAasForAnr = new ArrayList<>();
                    while (i2 < strArr2.length) {
                        String strDecodeGas3 = decodeGas(strArr2[i2]);
                        this.mAasForAnr.add(strDecodeGas3);
                        log("Load UPB AAS done i is " + i2 + ", aas is " + strDecodeGas3);
                        i2++;
                    }
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            default:
                switch (i) {
                    case 1000:
                        logi("EVENT_GET_RECORDS_SIZE_DONE done.");
                        AsyncResult asyncResult26 = (AsyncResult) message.obj;
                        int i5 = message.arg1;
                        if (asyncResult26.exception == null) {
                            int[] iArr12 = (int[]) asyncResult26.result;
                            if (iArr12.length == 3) {
                                if (this.mRecordSize == null) {
                                    this.mRecordSize = new SparseArray<>();
                                }
                                this.mRecordSize.put(i5, iArr12);
                            } else {
                                Rlog.e(LOG_TAG, "get wrong record size format" + asyncResult26.exception);
                            }
                        } else {
                            Rlog.e(LOG_TAG, "get EF record size failed" + asyncResult26.exception);
                        }
                        synchronized (this.mLock) {
                            this.mLock.notify();
                            break;
                        }
                        return;
                    case 1001:
                        AsyncResult asyncResult27 = (AsyncResult) message.obj;
                        logi("EVENT_EXT1_LOAD_DONE done pbr " + message.arg1);
                        if (asyncResult27.exception == null && (arrayList2 = (ArrayList) asyncResult27.result) != null) {
                            log("EVENT_EXT1_LOAD_DONE done size " + arrayList2.size());
                            if (this.mExt1FileList == null) {
                                this.mExt1FileList = new ArrayList<>();
                            }
                            this.mExt1FileList.add(arrayList2);
                        }
                        synchronized (this.mLock) {
                            this.mLock.notify();
                            break;
                        }
                        return;
                    default:
                        Rlog.e(LOG_TAG, "UnRecognized Message : " + message.what);
                        return;
                }
        }
    }

    private class PbrRecord {
        private int mAnrIndex = 0;
        private SparseArray<File> mFileIds = new SparseArray<>();
        private int mMasterFileRecordNum;

        PbrRecord(byte[] bArr) {
            MtkUsimPhoneBookManager.this.logi("PBR rec: " + IccUtils.bytesToHexString(bArr));
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
            MtkUsimPhoneBookManager.access$408(MtkUsimPhoneBookManager.this);
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
                    case 198:
                    case 199:
                    case 200:
                    case 201:
                    case ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP:
                    case ExternalSimConstants.EVENT_TYPE_RSIM_AUTH_DONE:
                        int i3 = -1;
                        byte[] data = simTlv.getData();
                        if (data.length < 2 || data.length > 3) {
                            Rlog.w(MtkUsimPhoneBookManager.LOG_TAG, "Invalid TLV length: " + data.length);
                        } else {
                            if (data.length == 3) {
                                i3 = data[2] & PplMessageManager.Type.INVALID;
                            }
                            int i4 = i3;
                            int i5 = ((data[0] & PplMessageManager.Type.INVALID) << 8) | (data[1] & PplMessageManager.Type.INVALID);
                            if (tag == 196) {
                                tag += 256 * this.mAnrIndex;
                                this.mAnrIndex++;
                            }
                            int i6 = tag;
                            File file = MtkUsimPhoneBookManager.this.new File(i, i5, i4, i2);
                            file.mTag = i6;
                            file.mPbrRecord = MtkUsimPhoneBookManager.this.mSliceCount;
                            MtkUsimPhoneBookManager.this.logi("pbr " + file);
                            this.mFileIds.put(i6, file);
                        }
                        break;
                }
                i2++;
            } while (simTlv.nextObject());
        }
    }

    private void queryUpbCapablityAndWait() {
        logi("queryUpbCapablityAndWait begin");
        synchronized (this.mUPBCapabilityLock) {
            for (int i = 0; i < 8; i++) {
                try {
                    this.mUpbCap[i] = -1;
                } catch (Throwable th) {
                    throw th;
                }
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
            logi("readGasListAndWait end");
        }
    }

    private void updatePhoneAdnRecordWithAnrByIndex(int i, int i2, int i3, byte[] bArr) {
        String strCalledPartyBCDToString;
        ArrayList<String> arrayList;
        log("updatePhoneAdnRecordWithAnrByIndex the " + i2 + "th anr record is " + IccUtils.bytesToHexString(bArr));
        byte b = bArr[1];
        byte b2 = bArr[0];
        if (b > 0 && b <= 11 && (strCalledPartyBCDToString = MtkPhoneNumberUtils.calledPartyBCDToString(bArr, 2, bArr[1])) != null && !strCalledPartyBCDToString.equals("")) {
            String str = null;
            if (b2 > 0 && b2 != 255 && this.mAasForAnr != null && (arrayList = this.mAasForAnr) != null && b2 <= arrayList.size()) {
                str = arrayList.get(b2 - 1);
            }
            logi(" updatePhoneAdnRecordWithAnrByIndex " + i2 + " th anr is " + strCalledPartyBCDToString + " the anrIndex is " + i3);
            try {
                MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i2);
                mtkAdnRecord.setAnr(strCalledPartyBCDToString, i3);
                if (str != null && str.length() > 0) {
                    mtkAdnRecord.setAasIndex(b2);
                }
                this.mPhoneBookRecords.set(i2, mtkAdnRecord);
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "updatePhoneAdnRecordWithAnrByIndex: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + i2);
            }
        }
    }

    public ArrayList<UsimGroup> getUsimGroups() {
        logi("getUsimGroups begin");
        synchronized (this.mGasLock) {
            if (!this.mGasForGrp.isEmpty()) {
                return this.mGasForGrp;
            }
            queryUpbCapablityAndWait();
            readGasListAndWait();
            logi("getUsimGroups end");
            return this.mGasForGrp;
        }
    }

    public String getUsimGroupById(int i) {
        String alphaTag;
        UsimGroup usimGroup;
        logi("getUsimGroupById nGasId is " + i);
        if (this.mGasForGrp != null && i <= this.mGasForGrp.size() && (usimGroup = this.mGasForGrp.get(i - 1)) != null) {
            alphaTag = usimGroup.getAlphaTag();
            log("getUsimGroupById index is " + usimGroup.getRecordIndex() + ", name is " + alphaTag);
        } else {
            alphaTag = null;
        }
        logi("getUsimGroupById grpName is " + alphaTag);
        return alphaTag;
    }

    public synchronized boolean removeUsimGroupById(int i) {
        boolean z;
        logi("removeUsimGroupById nGasId is " + i);
        synchronized (this.mGasLock) {
            z = false;
            if (this.mGasForGrp == null || i > this.mGasForGrp.size()) {
                Rlog.e(LOG_TAG, "removeUsimGroupById fail ");
            } else {
                int i2 = i - 1;
                UsimGroup usimGroup = this.mGasForGrp.get(i2);
                if (usimGroup != null) {
                    log(" removeUsimGroupById index is " + usimGroup.getRecordIndex());
                }
                if (usimGroup != null && usimGroup.getAlphaTag() != null) {
                    this.mCi.deleteUPBEntry(4, 0, i, obtainMessage(13));
                    try {
                        this.mGasLock.wait();
                    } catch (InterruptedException e) {
                        Rlog.e(LOG_TAG, "Interrupted Exception in removeUsimGroupById");
                    }
                    if (this.mResult == 0) {
                        usimGroup.setAlphaTag(null);
                        this.mGasForGrp.set(i2, usimGroup);
                        z = true;
                    }
                } else {
                    Rlog.w(LOG_TAG, "removeUsimGroupById fail: this gas doesn't exist ");
                }
            }
        }
        logi("removeUsimGroupById result is " + z);
        return z;
    }

    private String decodeGas(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("[decodeGas] gas string is ");
        sb.append(str == null ? "null" : str);
        log(sb.toString());
        if (str == null || TextUtils.isEmpty(str) || str.length() % 2 != 0) {
            return null;
        }
        try {
            byte[] bArrHexStringToBytes = IccUtils.hexStringToBytes(str);
            if (bArrHexStringToBytes == null) {
                Rlog.w(LOG_TAG, "gas string is null");
                return null;
            }
            return new String(bArrHexStringToBytes, 0, str.length() / 2, "utf-16be");
        } catch (UnsupportedEncodingException e) {
            Rlog.e(LOG_TAG, "[decodeGas] implausible UnsupportedEncodingException", e);
            return null;
        } catch (RuntimeException e2) {
            Rlog.e(LOG_TAG, "[decodeGas] RuntimeException", e2);
            return null;
        }
    }

    private String encodeToUcs2(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            String hexString = Integer.toHexString(str.charAt(i));
            for (int i2 = 0; i2 < 4 - hexString.length(); i2++) {
                sb.append("0");
            }
            sb.append(hexString);
        }
        return sb.toString();
    }

    public synchronized int insertUsimGroup(String str) {
        int recordIndex = -1;
        logi("insertUsimGroup grpName is " + str);
        synchronized (this.mGasLock) {
            if (this.mGasForGrp == null || this.mGasForGrp.size() == 0) {
                Rlog.w(LOG_TAG, "insertUsimGroup fail ");
            } else {
                UsimGroup usimGroup = null;
                int i = 0;
                while (true) {
                    if (i < this.mGasForGrp.size()) {
                        usimGroup = this.mGasForGrp.get(i);
                        if (usimGroup == null || usimGroup.getAlphaTag() != null) {
                            i++;
                        } else {
                            recordIndex = usimGroup.getRecordIndex();
                            log("insertUsimGroup index is " + recordIndex);
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (recordIndex < 0) {
                    Rlog.w(LOG_TAG, "insertUsimGroup fail: gas file is full.");
                    return -20;
                }
                this.mCi.editUPBEntry(4, 0, recordIndex, encodeToUcs2(str), null, obtainMessage(13));
                try {
                    this.mGasLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in insertUsimGroup");
                }
                if (this.mResult < 0) {
                    Rlog.e(LOG_TAG, "result is negative. insertUsimGroup");
                    return this.mResult;
                }
                usimGroup.setAlphaTag(str);
                this.mGasForGrp.set(i, usimGroup);
            }
            return recordIndex;
        }
    }

    public synchronized int updateUsimGroup(int i, String str) {
        logi("updateUsimGroup nGasId is " + i);
        synchronized (this.mGasLock) {
            this.mResult = -1;
            if (this.mGasForGrp == null || i > this.mGasForGrp.size()) {
                Rlog.w(LOG_TAG, "updateUsimGroup fail ");
            } else if (str != null) {
                this.mCi.editUPBEntry(4, 0, i, encodeToUcs2(str), null, obtainMessage(13));
                try {
                    this.mGasLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in updateUsimGroup");
                }
            }
            if (this.mResult == 0) {
                UsimGroup usimGroup = this.mGasForGrp.get(i - 1);
                if (usimGroup != null) {
                    log("updateUsimGroup index is " + usimGroup.getRecordIndex());
                    usimGroup.setAlphaTag(str);
                } else {
                    Rlog.w(LOG_TAG, "updateUsimGroup the entry doesn't exist ");
                }
            } else {
                i = this.mResult;
            }
        }
        return i;
    }

    public boolean addContactToGroup(int i, int i2) {
        boolean z;
        int i3;
        logi("addContactToGroup begin adnIndex is " + i + " to grp " + i2);
        boolean z2 = false;
        if (this.mPhoneBookRecords == null || i <= 0 || i > this.mPhoneBookRecords.size()) {
            Rlog.e(LOG_TAG, "addContactToGroup no records or invalid index.");
            return false;
        }
        synchronized (this.mLock) {
            try {
                int i4 = i - 1;
                MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i4);
                if (mtkAdnRecord != null) {
                    log(" addContactToGroup the adn index is " + mtkAdnRecord.getRecId() + " old grpList is " + mtkAdnRecord.getGrpIds());
                    String grpIds = mtkAdnRecord.getGrpIds();
                    int i5 = this.mUpbCap[7];
                    int i6 = this.mUpbCap[7] > this.mUpbCap[5] ? this.mUpbCap[5] : this.mUpbCap[7];
                    int[] iArr = new int[i5];
                    for (int i7 = 0; i7 < i5; i7++) {
                        iArr[i7] = 0;
                    }
                    if (grpIds != null) {
                        String[] strArrSplit = mtkAdnRecord.getGrpIds().split(",");
                        int i8 = 0;
                        i3 = -1;
                        while (true) {
                            if (i8 < i6) {
                                iArr[i8] = Integer.parseInt(strArrSplit[i8]);
                                if (i2 == iArr[i8]) {
                                    log(" addContactToGroup the adn is already in the group. i is " + i8);
                                    z = true;
                                    break;
                                }
                                if (i3 < 0 && (iArr[i8] == 0 || iArr[i8] == 255)) {
                                    log(" addContactToGroup found an unsed position in the group list. i is " + i8);
                                    i3 = i8;
                                }
                                i8++;
                            } else {
                                z = false;
                                break;
                            }
                        }
                    } else {
                        z = false;
                        i3 = 0;
                    }
                    if (!z && i3 >= 0) {
                        iArr[i3] = i2;
                        this.mCi.writeUPBGrpEntry(i, iArr, obtainMessage(12));
                        try {
                            this.mLock.wait();
                        } catch (InterruptedException e) {
                            Rlog.e(LOG_TAG, "Interrupted Exception in addContactToGroup");
                        }
                        if (this.mResult == 0) {
                            updatePhoneAdnRecordWithGrpByIndex(i4, i, iArr);
                            logi(" addContactToGroup the adn index is " + mtkAdnRecord.getRecId());
                            this.mResult = -1;
                            z2 = true;
                        }
                    }
                }
            } catch (IndexOutOfBoundsException e2) {
                Rlog.e(LOG_TAG, "addContactToGroup: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + (i - 1));
                return false;
            }
        }
        logi("addContactToGroup end adnIndex is " + i + " to grp " + i2);
        return z2;
    }

    public synchronized boolean removeContactFromGroup(int i, int i2) {
        logi("removeContactFromGroup begin adnIndex is " + i + " to grp " + i2);
        boolean z = false;
        if (this.mPhoneBookRecords != null && i > 0 && i <= this.mPhoneBookRecords.size()) {
            synchronized (this.mLock) {
                try {
                    int i3 = i - 1;
                    MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i3);
                    if (mtkAdnRecord != null) {
                        String grpIds = mtkAdnRecord.getGrpIds();
                        if (grpIds == null) {
                            Rlog.e(LOG_TAG, " the adn is not in any group. ");
                            return false;
                        }
                        String[] strArrSplit = grpIds.split(",");
                        int[] iArr = new int[strArrSplit.length];
                        boolean z2 = false;
                        int i4 = -1;
                        for (int i5 = 0; i5 < strArrSplit.length; i5++) {
                            iArr[i5] = Integer.parseInt(strArrSplit[i5]);
                            if (i2 == iArr[i5]) {
                                log(" removeContactFromGroup the adn is in the group. i is " + i5);
                                z2 = true;
                                i4 = i5;
                            }
                        }
                        if (!z2 || i4 < 0) {
                            Rlog.e(LOG_TAG, " removeContactFromGroup the adn is not in the group. ");
                        } else {
                            iArr[i4] = 0;
                            this.mCi.writeUPBGrpEntry(i, iArr, obtainMessage(12));
                            try {
                                this.mLock.wait();
                            } catch (InterruptedException e) {
                                Rlog.e(LOG_TAG, "Interrupted Exception in removeContactFromGroup");
                            }
                            if (this.mResult == 0) {
                                updatePhoneAdnRecordWithGrpByIndex(i3, i, iArr);
                                this.mResult = -1;
                                z = true;
                            }
                        }
                    }
                    logi("removeContactFromGroup end adnIndex is " + i + " to grp " + i2);
                    return z;
                } catch (IndexOutOfBoundsException e2) {
                    Rlog.e(LOG_TAG, "removeContactFromGroup: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + (i - 1));
                    return false;
                }
            }
        }
        Rlog.e(LOG_TAG, "removeContactFromGroup no records or invalid index.");
        return false;
    }

    public boolean updateContactToGroups(int i, int[] iArr) {
        boolean z = false;
        if (this.mPhoneBookRecords == null || i <= 0 || i > this.mPhoneBookRecords.size() || iArr == null) {
            Rlog.e(LOG_TAG, "updateContactToGroups no records or invalid index.");
            return false;
        }
        logi("updateContactToGroups begin grpIdList is " + i + " to grp list count " + iArr.length);
        synchronized (this.mLock) {
            int i2 = i - 1;
            MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i2);
            if (mtkAdnRecord != null) {
                log(" updateContactToGroups the adn index is " + mtkAdnRecord.getRecId() + " old grpList is " + mtkAdnRecord.getGrpIds());
                int i3 = this.mUpbCap[7];
                if (iArr.length > i3) {
                    Rlog.e(LOG_TAG, "updateContactToGroups length of grpIdList > grpCount.");
                    return false;
                }
                int[] iArr2 = new int[i3];
                int i4 = 0;
                while (i4 < i3) {
                    iArr2[i4] = i4 < iArr.length ? iArr[i4] : 0;
                    log("updateContactToGroups i:" + i4 + ",grpIdArray[" + i4 + "]:" + iArr2[i4]);
                    i4++;
                }
                this.mCi.writeUPBGrpEntry(i, iArr2, obtainMessage(12));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in updateContactToGroups");
                }
                if (this.mResult == 0) {
                    updatePhoneAdnRecordWithGrpByIndex(i2, i, iArr2);
                    logi(" updateContactToGroups the adn index is " + mtkAdnRecord.getRecId());
                    this.mResult = -1;
                    z = true;
                }
            }
            logi("updateContactToGroups end grpIdList is " + i + " to grp list count " + iArr.length);
            return z;
        }
    }

    public boolean moveContactFromGroupsToGroups(int i, int[] iArr, int[] iArr2) {
        boolean z;
        boolean z2;
        boolean z3 = false;
        if (this.mPhoneBookRecords == null || i <= 0 || i > this.mPhoneBookRecords.size()) {
            Rlog.e(LOG_TAG, "moveContactFromGroupsToGroups no records or invalid index.");
            return false;
        }
        synchronized (this.mLock) {
            int i2 = i - 1;
            MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i2);
            if (mtkAdnRecord != null) {
                int i3 = this.mUpbCap[7];
                int i4 = this.mUpbCap[7] > this.mUpbCap[5] ? this.mUpbCap[5] : this.mUpbCap[7];
                String grpIds = mtkAdnRecord.getGrpIds();
                StringBuilder sb = new StringBuilder();
                sb.append(" moveContactFromGroupsToGroups the adn index is ");
                sb.append(mtkAdnRecord.getRecId());
                sb.append(" original grpIds is ");
                sb.append(grpIds);
                sb.append(", fromGrpIdList: ");
                sb.append(iArr == null ? "null" : iArr);
                sb.append(", toGrpIdList: ");
                sb.append(iArr2 == null ? "null" : iArr2);
                logi(sb.toString());
                int[] iArr3 = new int[i3];
                for (int i5 = 0; i5 < i3; i5++) {
                    iArr3[i5] = 0;
                }
                if (grpIds != null) {
                    String[] strArrSplit = grpIds.split(",");
                    for (int i6 = 0; i6 < i4; i6++) {
                        iArr3[i6] = Integer.parseInt(strArrSplit[i6]);
                    }
                }
                if (iArr != null) {
                    for (int i7 : iArr) {
                        for (int i8 = 0; i8 < i4; i8++) {
                            if (iArr3[i8] == i7) {
                                iArr3[i8] = 0;
                            }
                        }
                    }
                }
                if (iArr2 != null) {
                    for (int i9 = 0; i9 < iArr2.length; i9++) {
                        int i10 = 0;
                        while (true) {
                            if (i10 < i4) {
                                if (iArr3[i10] != iArr2[i9]) {
                                    i10++;
                                } else {
                                    z = true;
                                    break;
                                }
                            } else {
                                z = false;
                                break;
                            }
                        }
                        if (z) {
                            Rlog.w(LOG_TAG, "moveContactFromGroupsToGroups the adn isalready in the group.");
                        } else {
                            for (int i11 = 0; i11 < i4; i11++) {
                                if (iArr3[i11] != 0 && iArr3[i11] != 255) {
                                }
                                iArr3[i11] = iArr2[i9];
                                z2 = true;
                                break;
                            }
                            z2 = false;
                            if (!z2) {
                                Rlog.e(LOG_TAG, "moveContactFromGroupsToGroups no empty to add.");
                                return false;
                            }
                        }
                    }
                }
                this.mCi.writeUPBGrpEntry(i, iArr3, obtainMessage(12));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in moveContactFromGroupsToGroups");
                }
                if (this.mResult == 0) {
                    updatePhoneAdnRecordWithGrpByIndex(i2, i, iArr3);
                    logi("moveContactFromGroupsToGroups the adn index is " + mtkAdnRecord.getRecId());
                    this.mResult = -1;
                    z3 = true;
                }
            }
            return z3;
        }
    }

    public boolean removeContactGroup(int i) {
        boolean z;
        logi("removeContactsGroup adnIndex is " + i);
        boolean z2 = false;
        if (this.mPhoneBookRecords == null || this.mPhoneBookRecords.isEmpty()) {
            return false;
        }
        synchronized (this.mLock) {
            try {
                try {
                    int i2 = i - 1;
                    MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i2);
                    if (mtkAdnRecord == null) {
                        return false;
                    }
                    log("removeContactsGroup rec is " + mtkAdnRecord);
                    String grpIds = mtkAdnRecord.getGrpIds();
                    if (grpIds == null) {
                        return false;
                    }
                    String[] strArrSplit = grpIds.split(",");
                    int i3 = 0;
                    while (true) {
                        if (i3 < strArrSplit.length) {
                            int i4 = Integer.parseInt(strArrSplit[i3]);
                            if (i4 <= 0 || i4 >= 255) {
                                i3++;
                            } else {
                                z = true;
                                break;
                            }
                        } else {
                            z = false;
                            break;
                        }
                    }
                    if (z) {
                        this.mCi.writeUPBGrpEntry(i, new int[0], obtainMessage(12));
                        try {
                            this.mLock.wait();
                        } catch (InterruptedException e) {
                            Rlog.e(LOG_TAG, "Interrupted Exception in removeContactGroup");
                        }
                        if (this.mResult == 0) {
                            int[] iArr = new int[strArrSplit.length];
                            for (int i5 = 0; i5 < strArrSplit.length; i5++) {
                                iArr[i5] = 0;
                            }
                            updatePhoneAdnRecordWithGrpByIndex(i2, i, iArr);
                            logi(" removeContactGroup the adn index is " + mtkAdnRecord.getRecId());
                            this.mResult = -1;
                            z2 = true;
                        }
                    }
                    return z2;
                } catch (IndexOutOfBoundsException e2) {
                    Rlog.e(LOG_TAG, "removeContactGroup: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + (i - 1));
                    return false;
                }
            } finally {
            }
        }
    }

    public int hasExistGroup(String str) {
        logi("hasExistGroup grpName is " + str);
        int recordIndex = -1;
        if (str == null) {
            return -1;
        }
        if (this.mGasForGrp != null && this.mGasForGrp.size() > 0) {
            int i = 0;
            while (true) {
                if (i < this.mGasForGrp.size()) {
                    UsimGroup usimGroup = this.mGasForGrp.get(i);
                    if (usimGroup == null || !str.equals(usimGroup.getAlphaTag())) {
                        i++;
                    } else {
                        log("getUsimGroupById index is " + usimGroup.getRecordIndex() + ", name is " + str);
                        recordIndex = usimGroup.getRecordIndex();
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        logi("hasExistGroup grpId is " + recordIndex);
        return recordIndex;
    }

    public int getUsimGrpMaxNameLen() {
        int i;
        logi("getUsimGrpMaxNameLen begin");
        synchronized (this.mUPBCapabilityLock) {
            if (checkIsPhbReady()) {
                if (this.mUpbCap[6] < 0) {
                    queryUpbCapablityAndWait();
                }
                i = this.mUpbCap[6];
            } else {
                i = -1;
            }
            logi("getUsimGrpMaxNameLen done: L_Gas is " + i);
        }
        return i;
    }

    public int getUsimGrpMaxCount() {
        int i;
        logi("getUsimGrpMaxCount begin");
        synchronized (this.mUPBCapabilityLock) {
            if (checkIsPhbReady()) {
                if (this.mUpbCap[5] < 0) {
                    queryUpbCapablityAndWait();
                }
                i = this.mUpbCap[5];
            } else {
                i = -1;
            }
            logi("getUsimGrpMaxCount done: N_Gas is " + i);
        }
        return i;
    }

    private void log(String str) {
        if (DBG) {
            Rlog.d(LOG_TAG, str + "(slot " + this.mSlotId + ")");
        }
    }

    private void logi(String str) {
        Rlog.i(LOG_TAG, str + "(slot " + this.mSlotId + ")");
    }

    public boolean isAnrCapacityFree(String str, int i, int i2, MtkAdnRecord mtkAdnRecord) {
        String additionalNumber;
        if (mtkAdnRecord != null) {
            additionalNumber = mtkAdnRecord.getAdditionalNumber(i2);
        } else {
            additionalNumber = null;
        }
        if (str == null || str.equals("") || i2 < 0 || getUsimEfType(196) == 168 || !(additionalNumber == null || additionalNumber.equals(""))) {
            return true;
        }
        if (!CsimPhbUtil.hasModemPhbEnhanceCapability(this.mFh)) {
            int i3 = i - 1;
            int i4 = i3 / this.mAdnFileSize;
            int i5 = i3 % this.mAdnFileSize;
            try {
                log("isAnrCapacityFree anr: " + str);
                if (this.mRecordSize != null && this.mRecordSize.size() != 0) {
                    File file = (File) this.mPbrRecords.get(i4).mFileIds.get(196 + (i2 * 256));
                    if (file == null) {
                        return false;
                    }
                    int i6 = this.mRecordSize.get(file.getEfid())[2];
                    log("isAnrCapacityFree size: " + i6);
                    if (i6 >= i5 + 1) {
                        return true;
                    }
                    log("isAnrCapacityFree: anrRecNum out of size: " + i5);
                    return false;
                }
                log("isAnrCapacityFree: mAnrFileSize is empty");
                return false;
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "isAnrCapacityFree Index out of bounds.");
                return false;
            } catch (NullPointerException e2) {
                Rlog.e(LOG_TAG, "isAnrCapacityFree exception:" + e2.toString());
                return false;
            }
        }
        synchronized (this.mLock) {
            if (this.mAnrInfo == null || i2 >= this.mAnrInfo.size()) {
                this.mCi.queryUPBAvailable(0, i2 + 1, obtainMessage(26));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e3) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in isAnrCapacityFree");
                }
            }
        }
        return (this.mAnrInfo == null || this.mAnrInfo.get(i2) == null || this.mAnrInfo.get(i2)[1] <= 0) ? false : true;
    }

    public void updateAnrByAdnIndex(String str, int i, int i2, MtkAdnRecord mtkAdnRecord) {
        int i3 = i - 1;
        int i4 = i3 / this.mAdnFileSize;
        int i5 = i3 % this.mAdnFileSize;
        if (this.mPbrRecords != null) {
            SparseArray sparseArray = this.mPbrRecords.get(i4).mFileIds;
            if (sparseArray == null) {
                log("updateAnrByAdnIndex: No anr tag in pbr record 0");
                return;
            }
            if (this.mPhoneBookRecords == null || this.mPhoneBookRecords.isEmpty()) {
                Rlog.w(LOG_TAG, "updateAnrByAdnIndex: mPhoneBookRecords is empty");
                return;
            }
            File file = (File) sparseArray.get(196 + (i2 * 256));
            if (file == null) {
                log("updateAnrByAdnIndex no efFile anrIndex: " + i2);
                return;
            }
            logi("updateAnrByAdnIndex begin effile " + file);
            String additionalNumber = null;
            if (mtkAdnRecord != null) {
                additionalNumber = mtkAdnRecord.getAdditionalNumber(i2);
                mtkAdnRecord.getAasIndex();
            }
            if (!CsimPhbUtil.hasModemPhbEnhanceCapability(this.mFh)) {
                int efid = file.getEfid();
                log("updateAnrByAdnIndex recId: " + i4 + " EF_ANR id is " + Integer.toHexString(efid).toUpperCase());
                if (file.getParentTag() == 169) {
                    updateType2Anr(str, i, file);
                    return;
                }
                try {
                    byte[] bArrBuildAnrRecord = buildAnrRecord(str, this.mAnrRecordSize, this.mPhoneBookRecords.get(i3).getAasIndex());
                    if (bArrBuildAnrRecord != null) {
                        this.mFh.updateEFLinearFixed(efid, i5 + 1, bArrBuildAnrRecord, (String) null, obtainMessage(9));
                    }
                } catch (IndexOutOfBoundsException e) {
                    Rlog.e(LOG_TAG, "updateAnrByAdnIndex: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + i3);
                    return;
                }
            } else {
                try {
                    int aasIndex = this.mPhoneBookRecords.get(i3).getAasIndex();
                    Message messageObtainMessage = obtainMessage(9);
                    synchronized (this.mLock) {
                        if (str == null) {
                            if (additionalNumber != null) {
                                this.mCi.deleteUPBEntry(0, i2 + 1, i, messageObtainMessage);
                                this.mLock.wait();
                            }
                            return;
                        }
                        try {
                            if (str.length() == 0) {
                                if (additionalNumber != null && additionalNumber.length() != 0) {
                                    this.mCi.deleteUPBEntry(0, i2 + 1, i, messageObtainMessage);
                                    try {
                                        this.mLock.wait();
                                    } catch (InterruptedException e2) {
                                        Rlog.e(LOG_TAG, "Interrupted Exception in updateAnrByAdnIndexOptmz");
                                    }
                                }
                                return;
                            }
                            String[] strArrBuildAnrRecordOptmz = buildAnrRecordOptmz(str, aasIndex);
                            this.mCi.editUPBEntry(0, i2 + 1, i, strArrBuildAnrRecordOptmz[0], strArrBuildAnrRecordOptmz[1], strArrBuildAnrRecordOptmz[2], messageObtainMessage);
                            this.mLock.wait();
                        } finally {
                        }
                    }
                } catch (IndexOutOfBoundsException e3) {
                    Rlog.e(LOG_TAG, "updateAnrByAdnIndexOptmz: mPhoneBookRecords IndexOutOfBoundsException size() is " + this.mPhoneBookRecords.size() + "index is " + i3);
                    return;
                }
            }
            logi("updateAnrByAdnIndex end effile " + file);
        }
    }

    private int getEmailRecNum(String[] strArr, int i, int i2, byte[] bArr, int i3) {
        boolean z;
        int i4 = bArr[i3] & PplMessageManager.Type.INVALID;
        log("getEmailRecNum recNum:" + i4);
        if (strArr == null) {
            if (i4 < 255 && i4 > 0) {
                this.mEmailRecTable[i4 - 1] = 0;
            }
            return -1;
        }
        int i5 = 0;
        while (true) {
            if (i5 < strArr.length) {
                if (strArr[i5] == null || strArr[i5].equals("")) {
                    i5++;
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (!z) {
            if (i4 < 255 && i4 > 0) {
                this.mEmailRecTable[i4 - 1] = 0;
            }
            return -1;
        }
        if (i4 > this.mEmailFileSize || i4 >= 255 || i4 <= 0) {
            int i6 = i * this.mEmailFileSize;
            int i7 = i6;
            while (true) {
                if (i7 >= this.mEmailFileSize + i6) {
                    break;
                }
                log("updateEmailsByAdnIndex: mEmailRecTable[" + i7 + "] is " + this.mEmailRecTable[i7]);
                if (this.mEmailRecTable[i7] != 0) {
                    i7++;
                } else {
                    i4 = (i7 + 1) - i6;
                    this.mEmailRecTable[i7] = i2;
                    break;
                }
            }
        }
        if (i4 > this.mEmailFileSize) {
            i4 = 255;
        }
        if (i4 == -1) {
            return -2;
        }
        return i4;
    }

    public boolean checkEmailCapacityFree(int i, String[] strArr, MtkAdnRecord mtkAdnRecord) {
        boolean z;
        if (strArr == null || getUsimEfType(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP) == 168 || (mtkAdnRecord != null && mtkAdnRecord.getEmails() != null)) {
            return true;
        }
        int i2 = 0;
        while (true) {
            if (i2 < strArr.length) {
                if (strArr[i2] == null || strArr[i2].equals("")) {
                    i2++;
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (!z) {
            return true;
        }
        if (!CsimPhbUtil.hasModemPhbEnhanceCapability(this.mFh)) {
            int i3 = ((i - 1) / this.mAdnFileSize) * this.mEmailFileSize;
            for (int i4 = i3; i4 < this.mEmailFileSize + i3; i4++) {
                if (this.mEmailRecTable[i4] == 0) {
                    return true;
                }
            }
            return false;
        }
        synchronized (this.mLock) {
            if (this.mEmailInfo == null || this.mEmailInfo.length != 3) {
                this.mCi.queryUPBAvailable(1, 1, obtainMessage(25));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in CheckEmailCapacityFree");
                }
                if (this.mUpbDone == -1) {
                    return true;
                }
                this.mEmailFileSize = countEmailFileSize();
            }
            int iCountEmailCapacity = countEmailCapacity(i);
            logi("CheckEmailCapacityFree: mEmailFileSize: " + this.mEmailFileSize + " used: " + iCountEmailCapacity + " adnIndex: " + i);
            if (iCountEmailCapacity < this.mEmailFileSize) {
                return true;
            }
            return false;
        }
    }

    private int countEmailFileSize() {
        int size = this.mPhoneBookRecords.size();
        int i = size / this.mAdnFileSize;
        if (size % this.mAdnFileSize > 0) {
            i++;
        }
        if (this.mEmailInfo != null && this.mEmailInfo.length == 3 && i > 0) {
            return this.mEmailInfo[0] / i;
        }
        return 100;
    }

    private int countEmailCapacity(int i) {
        String[] emails;
        if (this.mPbrRecords == null) {
            return -1;
        }
        int i2 = (i - 1) / this.mAdnFileSize;
        int i3 = this.mAdnFileSize * i2;
        int size = this.mPhoneBookRecords.size();
        int i4 = this.mAdnFileSize + i3;
        if (size >= i4) {
            size = i4;
        }
        if (((File) this.mPbrRecords.get(i2).mFileIds.get(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP)) == null) {
            return -1;
        }
        MtkAdnRecord mtkAdnRecord = null;
        int i5 = 0;
        while (i3 < size) {
            try {
                mtkAdnRecord = this.mPhoneBookRecords.get(i3);
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "countEmailCapacity: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + i3);
            }
            if (mtkAdnRecord != null && (emails = mtkAdnRecord.getEmails()) != null && emails.length > 0 && emails[0].length() > 0) {
                i5++;
            }
            i3++;
        }
        log("countEmailCapacity: email used: " + i5);
        return i5;
    }

    public boolean checkSneCapacityFree(int i, String str, MtkAdnRecord mtkAdnRecord) {
        String sne;
        if (mtkAdnRecord != null) {
            sne = mtkAdnRecord.getSne();
        } else {
            sne = null;
        }
        if (str == null || str.equals("") || getUsimEfType(195) == 168 || !((sne == null || sne.equals("")) && CsimPhbUtil.hasModemPhbEnhanceCapability(this.mFh))) {
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

    private int getUsimEfType(int i) {
        SparseArray sparseArray;
        File file;
        if (this.mPbrRecords == null || (sparseArray = this.mPbrRecords.get(0).mFileIds) == null || (file = (File) sparseArray.get(i)) == null) {
            return 0;
        }
        Rlog.d(LOG_TAG, "[getUsimEfType] efTag: " + i + ", type: " + file.getParentTag());
        return file.getParentTag();
    }

    public boolean checkEmailLength(String[] strArr) {
        SparseArray sparseArray;
        File file;
        if (strArr == null || strArr[0] == null || this.mPbrRecords == null || (sparseArray = this.mPbrRecords.get(0).mFileIds) == null || (file = (File) sparseArray.get(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP)) == null) {
            return true;
        }
        int i = (this.mEmailRecordSize == -1 || !(file.getParentTag() == 169)) ? this.mEmailRecordSize : this.mEmailRecordSize - 2;
        byte[] bArrStringToGsm8BitPacked = GsmAlphabet.stringToGsm8BitPacked(strArr[0]);
        logi("checkEmailLength eMailData.length=" + bArrStringToGsm8BitPacked.length + ", maxDataLength=" + i);
        return i == -1 || bArrStringToGsm8BitPacked.length <= i;
    }

    public int updateEmailsByAdnIndex(String[] strArr, int i, MtkAdnRecord mtkAdnRecord) {
        SparseArray sparseArray;
        String[] emails;
        String str;
        int i2 = i - 1;
        int i3 = i2 / this.mAdnFileSize;
        int i4 = i2 % this.mAdnFileSize;
        if (this.mPbrRecords == null || (sparseArray = this.mPbrRecords.get(i3).mFileIds) == null || sparseArray.size() == 0 || this.mPhoneBookRecords == null || this.mPhoneBookRecords.isEmpty()) {
            return 0;
        }
        File file = (File) sparseArray.get(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP);
        if (file == null) {
            log("updateEmailsByAdnIndex: No email tag in pbr record 0");
            return 0;
        }
        String str2 = null;
        if (mtkAdnRecord != null) {
            emails = mtkAdnRecord.getEmails();
        } else {
            emails = null;
        }
        if (emails != null && emails.length != 0 && !TextUtils.isEmpty(emails[0])) {
            str = emails[0];
        } else {
            str = null;
        }
        int efid = file.getEfid();
        boolean z = file.getParentTag() == 169;
        file.getIndex();
        logi("updateEmailsByAdnIndex: pbrrecNum is " + i3 + " EF_EMAIL id is " + Integer.toHexString(efid).toUpperCase());
        if (!CsimPhbUtil.hasModemPhbEnhanceCapability(this.mFh)) {
            if (z && this.mIapFileList != null) {
                return updateType2Email(strArr, i, file);
            }
            log("updateEmailsByAdnIndex file: " + file);
            if (strArr != null && strArr.length > 0) {
                str2 = strArr[0];
            }
            if (this.mEmailRecordSize <= 0) {
                return -50;
            }
            byte[] bArrBuildEmailRecord = buildEmailRecord(str2, i, this.mEmailRecordSize, z);
            log("updateEmailsByAdnIndex build type1 email record:" + IccUtils.bytesToHexString(bArrBuildEmailRecord));
            if (bArrBuildEmailRecord == null) {
                return -40;
            }
            this.mFh.updateEFLinearFixed(efid, i4 + 1, bArrBuildEmailRecord, (String) null, obtainMessage(8));
            return 0;
        }
        Message messageObtainMessage = obtainMessage(8);
        synchronized (this.mLock) {
            if (strArr != null) {
                try {
                    if (strArr.length == 0 || TextUtils.isEmpty(strArr[0])) {
                        if (str == null) {
                            return 0;
                        }
                        this.mCi.deleteUPBEntry(1, 1, i, messageObtainMessage);
                    } else {
                        if (strArr[0].equals(str)) {
                            return 0;
                        }
                        this.mCi.editUPBEntry(1, 1, i, encodeToUcs2(strArr[0]), null, messageObtainMessage);
                    }
                } finally {
                }
            }
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "Interrupted Exception in updateEmailsByAdnIndex");
            }
            return 0;
        }
    }

    private int updateType2Email(String[] strArr, int i, File file) {
        String str;
        int i2 = i - 1;
        int i3 = i2 / this.mAdnFileSize;
        int i4 = i2 % this.mAdnFileSize;
        int index = file.getIndex();
        file.getEfid();
        try {
            ArrayList<byte[]> arrayList = this.mIapFileList.get(i3);
            if (arrayList.size() > 0) {
                byte[] bArr = arrayList.get(i4);
                int i5 = i4 + 1;
                int emailRecNum = getEmailRecNum(strArr, i3, i5, bArr, index);
                log("updateEmailsByAdnIndex: Email recNum is " + emailRecNum);
                if (-2 == emailRecNum) {
                    return -30;
                }
                log("updateEmailsByAdnIndex: found Email recNum is " + emailRecNum);
                bArr[index] = (byte) emailRecNum;
                SparseArray sparseArray = this.mPbrRecords.get(i3).mFileIds;
                if (sparseArray.get(193) != null) {
                    this.mFh.updateEFLinearFixed(((File) sparseArray.get(193)).getEfid(), i5, bArr, (String) null, obtainMessage(7));
                    if (emailRecNum != 255 && emailRecNum != -1 && strArr != null) {
                        try {
                            str = strArr[0];
                        } catch (IndexOutOfBoundsException e) {
                            Rlog.e(LOG_TAG, "Error: updateEmailsByAdnIndex no email address, continuing");
                            str = null;
                        }
                        if (this.mEmailRecordSize <= 0) {
                            return -50;
                        }
                        byte[] bArrBuildEmailRecord = buildEmailRecord(str, i, this.mEmailRecordSize, true);
                        if (bArrBuildEmailRecord == null) {
                            return -40;
                        }
                        this.mFh.updateEFLinearFixed(((File) sparseArray.get(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP)).getEfid(), emailRecNum, bArrBuildEmailRecord, (String) null, obtainMessage(8));
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

    private byte[] buildAnrRecord(String str, int i, int i2) {
        log("buildAnrRecord anr:" + str + ",recordSize:" + i + ",aas:" + i2);
        if (i <= 0) {
            readAnrRecordSize();
        }
        log("buildAnrRecord recordSize:" + i);
        byte[] bArr = new byte[i];
        for (int i3 = 0; i3 < i; i3++) {
            bArr[i3] = -1;
        }
        String strConvertPreDial = MtkPhoneNumberUtils.convertPreDial(str);
        if (TextUtils.isEmpty(strConvertPreDial)) {
            Rlog.w(LOG_TAG, "[buildAnrRecord] Empty dialing number");
            return bArr;
        }
        if (strConvertPreDial.length() > 20) {
            Rlog.w(LOG_TAG, "[buildAnrRecord] Max length of dialing number is 20");
            return null;
        }
        byte[] bArrNumberToCalledPartyBCD = MtkPhoneNumberUtils.numberToCalledPartyBCD(strConvertPreDial);
        if (bArrNumberToCalledPartyBCD != null) {
            bArr[0] = (byte) i2;
            System.arraycopy(bArrNumberToCalledPartyBCD, 0, bArr, 2, bArrNumberToCalledPartyBCD.length);
            bArr[1] = (byte) bArrNumberToCalledPartyBCD.length;
        }
        return bArr;
    }

    private byte[] buildEmailRecord(String str, int i, int i2, boolean z) {
        byte[] bArr = new byte[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            bArr[i3] = -1;
        }
        if (str != null && !str.equals("")) {
            byte[] bArrStringToGsm8BitPacked = GsmAlphabet.stringToGsm8BitPacked(str);
            int length = (this.mEmailRecordSize != -1 && z) ? bArr.length - 2 : bArr.length;
            log("buildEmailRecord eMailData.length=" + bArrStringToGsm8BitPacked.length + ", maxDataLength=" + length);
            if (bArrStringToGsm8BitPacked.length <= length) {
                System.arraycopy(bArrStringToGsm8BitPacked, 0, bArr, 0, bArrStringToGsm8BitPacked.length);
                log("buildEmailRecord eMailData=" + IccUtils.bytesToHexString(bArrStringToGsm8BitPacked) + ", eMailRecData=" + IccUtils.bytesToHexString(bArr));
                if (z && this.mPbrRecords != null) {
                    int i4 = (i - 1) / this.mAdnFileSize;
                    int i5 = (i % this.mAdnFileSize) & 255;
                    File file = (File) this.mPbrRecords.get(i4).mFileIds.get(192);
                    bArr[i2 - 2] = (byte) file.getSfi();
                    bArr[i2 - 1] = (byte) i5;
                    log("buildEmailRecord x+1=" + file.getSfi() + ", x+2=" + i5);
                }
            } else {
                return null;
            }
        }
        return bArr;
    }

    public void updateUsimPhonebookRecordsList(int i, MtkAdnRecord mtkAdnRecord) {
        logi("updateUsimPhonebookRecordsList update the " + i + "th record.");
        if (i < this.mPhoneBookRecords.size()) {
            MtkAdnRecord mtkAdnRecord2 = this.mPhoneBookRecords.get(i);
            if (mtkAdnRecord2 != null && mtkAdnRecord2.getGrpIds() != null) {
                mtkAdnRecord.setGrpIds(mtkAdnRecord2.getGrpIds());
            }
            this.mPhoneBookRecords.set(i, mtkAdnRecord);
            this.mRefreshAdnInfo = true;
        }
    }

    private void updatePhoneAdnRecordWithGrpByIndex(int i, int i2, int[] iArr) {
        int length;
        log("updatePhoneAdnRecordWithGrpByIndex the " + i + "th grp ");
        if (i <= this.mPhoneBookRecords.size() && (length = iArr.length) > 0) {
            try {
                MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i);
                log("updatePhoneAdnRecordWithGrpByIndex the adnIndex is " + i2 + "; the original index is " + mtkAdnRecord.getRecId());
                StringBuilder sb = new StringBuilder();
                int i3 = 0;
                while (true) {
                    int i4 = length - 1;
                    if (i3 < i4) {
                        sb.append(iArr[i3]);
                        sb.append(",");
                        i3++;
                    } else {
                        sb.append(iArr[i4]);
                        mtkAdnRecord.setGrpIds(sb.toString());
                        log("updatePhoneAdnRecordWithGrpByIndex grpIds is " + sb.toString());
                        this.mPhoneBookRecords.set(i, mtkAdnRecord);
                        log("updatePhoneAdnRecordWithGrpByIndex the rec:" + mtkAdnRecord);
                        return;
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "updatePhoneAdnRecordWithGrpByIndex: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + i);
            }
        }
    }

    private void readType1Ef(File file, int i) {
        int[] eFLinearRecordSize;
        int i2;
        log("readType1Ef:" + file);
        if (file.getParentTag() != 168) {
            return;
        }
        int i3 = file.mPbrRecord;
        int size = this.mPhoneBookRecords.size();
        int i4 = this.mAdnFileSize * i3;
        int i5 = this.mAdnFileSize + i4;
        int i6 = size < i5 ? size : i5;
        if (this.mRecordSize != null && this.mRecordSize.get(file.getEfid()) != null) {
            eFLinearRecordSize = this.mRecordSize.get(file.getEfid());
        } else {
            eFLinearRecordSize = readEFLinearRecordSize(file.getEfid());
        }
        if (eFLinearRecordSize != null) {
            int i7 = 3;
            if (eFLinearRecordSize.length == 3) {
                int i8 = 0;
                int i9 = eFLinearRecordSize[0];
                int i10 = file.mTag % 256;
                int i11 = file.mTag / 256;
                log("readType1Ef: RecordSize = " + i9);
                if (i10 == 202) {
                    for (int i12 = i4; i12 < this.mEmailFileSize + i4; i12++) {
                        try {
                            this.mEmailRecTable[i12] = 0;
                        } catch (ArrayIndexOutOfBoundsException e) {
                            Rlog.e(LOG_TAG, "init RecTable error " + e.getMessage());
                        }
                    }
                }
                if (i9 == 0) {
                    Rlog.w(LOG_TAG, "readType1Ef: recordSize is 0. ");
                    return;
                }
                int i13 = i4;
                int i14 = 0;
                while (i13 < i6) {
                    try {
                        MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i13);
                        if (mtkAdnRecord.getAlphaTag().length() > 0 || mtkAdnRecord.getNumber().length() > 0) {
                            int[] iArr = new int[i7];
                            iArr[i8] = file.mPbrRecord;
                            iArr[1] = i13;
                            iArr[2] = i;
                            if (i10 == 202) {
                                iArr[i8] = ((i13 + 1) - i4) + (this.mEmailFileSize * i4);
                                i2 = 15;
                                this.mReadingEmailNum.incrementAndGet();
                            } else {
                                switch (i10) {
                                    case 195:
                                        i2 = 18;
                                        this.mReadingSneNum.incrementAndGet();
                                        break;
                                    case 196:
                                        i2 = 16;
                                        this.mReadingAnrNum.addAndGet(1);
                                        break;
                                    default:
                                        Rlog.e(LOG_TAG, "not support tag " + file.mTag);
                                        i2 = i8;
                                        break;
                                }
                            }
                            this.mFh.readEFLinearFixed(file.getEfid(), (i13 + 1) - i4, i9, obtainMessage(i2, iArr));
                            i14++;
                        }
                        i13++;
                        i8 = 0;
                        i7 = 3;
                    } catch (IndexOutOfBoundsException e2) {
                        Rlog.e(LOG_TAG, "readType1Ef: mPhoneBookRecords IndexOutOfBoundsException numAdnRecs is " + size + "index is " + i13);
                    }
                }
                if (i10 == 202) {
                    if (this.mReadingEmailNum.get() == 0) {
                        this.mNeedNotify.set(false);
                        return;
                    }
                    this.mNeedNotify.set(true);
                } else {
                    switch (i10) {
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
                        default:
                            Rlog.e(LOG_TAG, "not support tag " + Integer.toHexString(file.mTag).toUpperCase());
                            break;
                    }
                }
                logi("readType1Ef before mLock.wait " + this.mNeedNotify.get() + " total:" + i14);
                synchronized (this.mLock) {
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e3) {
                        Rlog.e(LOG_TAG, "Interrupted Exception in readType1Ef");
                    }
                }
                logi("readType1Ef after mLock.wait " + this.mNeedNotify.get());
                return;
            }
        }
        Rlog.e(LOG_TAG, "readType1Ef: read record size error.");
    }

    private void readType2Ef(File file) {
        int[] eFLinearRecordSize;
        int i;
        char c;
        int i2;
        int i3;
        int i4;
        log("readType2Ef:" + file);
        if (file.getParentTag() != 169) {
            return;
        }
        int i5 = file.mPbrRecord;
        SparseArray sparseArray = this.mPbrRecords.get(file.mPbrRecord).mFileIds;
        if (sparseArray == null) {
            Rlog.e(LOG_TAG, "Error: no fileIds");
            return;
        }
        File file2 = (File) sparseArray.get(193);
        if (file2 == null) {
            Rlog.e(LOG_TAG, "Can't locate EF_IAP in EF_PBR.");
            return;
        }
        readIapFileAndWait(i5, file2.getEfid(), false);
        if (this.mIapFileList == null || this.mIapFileList.size() <= i5 || this.mIapFileList.get(i5).size() == 0) {
            Rlog.e(LOG_TAG, "Error: IAP file is empty");
            return;
        }
        int size = this.mPhoneBookRecords.size();
        int i6 = i5 * this.mAdnFileSize;
        int i7 = this.mAdnFileSize + i6;
        int i8 = size < i7 ? size : i7;
        int i9 = file.mTag;
        if (i9 == 202) {
            for (int i10 = i6; i10 < this.mEmailFileSize + i6; i10++) {
                try {
                    this.mEmailRecTable[i10] = 0;
                } catch (ArrayIndexOutOfBoundsException e) {
                    Rlog.e(LOG_TAG, "init RecTable error " + e.getMessage());
                }
            }
        } else {
            switch (i9) {
                case 195:
                case 196:
                    break;
                default:
                    Rlog.e(LOG_TAG, "no implement type2 EF " + file.mTag);
                    return;
            }
        }
        int efid = file.getEfid();
        if (this.mRecordSize != null && this.mRecordSize.get(efid) != null) {
            eFLinearRecordSize = this.mRecordSize.get(efid);
        } else {
            eFLinearRecordSize = readEFLinearRecordSize(efid);
        }
        if (eFLinearRecordSize == null || eFLinearRecordSize.length != 3) {
            Rlog.e(LOG_TAG, "readType2: read record size error.");
            return;
        }
        log("readType2: RecordSize = " + eFLinearRecordSize[0]);
        ArrayList<byte[]> arrayList = this.mIapFileList.get(i5);
        if (arrayList.size() == 0) {
            Rlog.e(LOG_TAG, "Warning: IAP size is 0");
            return;
        }
        int index = file.getIndex();
        int i11 = 0;
        int i12 = i6;
        while (i12 < i8) {
            try {
                MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i12);
                if (mtkAdnRecord.getAlphaTag().length() > 0 || mtkAdnRecord.getNumber().length() > 0) {
                    int i13 = i12 - i6;
                    int i14 = arrayList.get(i13)[index] & PplMessageManager.Type.INVALID;
                    if (i14 <= 0 || i14 >= 255) {
                        i = i5;
                        i3 = i6;
                    } else {
                        log("Type2 iap[" + i13 + "]=" + i14);
                        int[] iArr = {i5, i12, 0};
                        int i15 = file.mTag;
                        if (i15 == 202) {
                            i = i5;
                            c = 0;
                            iArr[0] = ((i12 + 1) - i6) + (this.mEmailFileSize * i6);
                            i2 = 15;
                            this.mReadingEmailNum.incrementAndGet();
                        } else {
                            switch (i15) {
                                case 195:
                                    i = i5;
                                    i4 = 18;
                                    this.mReadingSneNum.incrementAndGet();
                                    i2 = i4;
                                    break;
                                case 196:
                                    i = i5;
                                    i4 = 16;
                                    iArr[2] = file.mAnrIndex;
                                    this.mReadingAnrNum.addAndGet(1);
                                    i2 = i4;
                                    break;
                                default:
                                    StringBuilder sb = new StringBuilder();
                                    i = i5;
                                    sb.append("not support tag ");
                                    sb.append(file.mTag);
                                    Rlog.e(LOG_TAG, sb.toString());
                                    i2 = 0;
                                    break;
                            }
                            c = 0;
                        }
                        i3 = i6;
                        this.mFh.readEFLinearFixed(efid, i14, eFLinearRecordSize[c], obtainMessage(i2, iArr));
                        i11++;
                    }
                }
                i12++;
                i5 = i;
                i6 = i3;
            } catch (IndexOutOfBoundsException e2) {
                Rlog.e(LOG_TAG, "readType2Ef: mPhoneBookRecords IndexOutOfBoundsException numAdnRecs is " + size + "index is " + i12);
            }
        }
        int i16 = file.mTag;
        if (i16 == 202) {
            if (this.mReadingEmailNum.get() == 0) {
                this.mNeedNotify.set(false);
                return;
            }
            this.mNeedNotify.set(true);
        } else {
            switch (i16) {
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
                default:
                    Rlog.e(LOG_TAG, "not support tag " + file.mTag);
                    break;
            }
        }
        logi("readType2Ef before mLock.wait " + this.mNeedNotify.get() + " total:" + i11);
        synchronized (this.mLock) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e3) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readType2Ef");
            }
        }
        logi("readType2Ef after mLock.wait " + this.mNeedNotify.get());
    }

    private void updatePhoneAdnRecordWithEmailByIndex(int i, int i2, byte[] bArr) {
        log("updatePhoneAdnRecordWithEmailByIndex emailIndex = " + i + ",adnIndex = " + i2);
        if (bArr == null || this.mPbrRecords == null) {
            return;
        }
        boolean z = ((File) this.mPbrRecords.get(0).mFileIds.get(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP)).getParentTag() == 169;
        log("updatePhoneAdnRecordWithEmailByIndex: Type2: " + z + " emailData: " + IccUtils.bytesToHexString(bArr));
        int length = bArr.length;
        if (z && bArr.length >= 2) {
            length = bArr.length - 2;
        }
        log("updatePhoneAdnRecordWithEmailByIndex length = " + length);
        byte[] bArr2 = new byte[length];
        for (int i3 = 0; i3 < length; i3++) {
            bArr2[i3] = -1;
        }
        System.arraycopy(bArr, 0, bArr2, 0, length);
        log("validEMailData=" + IccUtils.bytesToHexString(bArr2) + ", validEmailLen=" + length);
        try {
            String strAdnStringFieldToString = IccUtils.adnStringFieldToString(bArr2, 0, length);
            log("updatePhoneAdnRecordWithEmailByIndex index " + i2 + " emailRecData record is " + strAdnStringFieldToString);
            if (strAdnStringFieldToString != null && !strAdnStringFieldToString.equals("")) {
                this.mPhoneBookRecords.get(i2).setEmails(new String[]{strAdnStringFieldToString});
            }
            this.mEmailRecTable[i - 1] = i2 + 1;
        } catch (IndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "[JE]updatePhoneAdnRecordWithEmailByIndex " + e.getMessage());
        }
    }

    private void updateType2Anr(String str, int i, File file) {
        SparseArray sparseArray;
        boolean z;
        int i2;
        int i3;
        int i4;
        MtkAdnRecord mtkAdnRecord;
        logi("updateType2Ef anr:" + str + ",adnIndex:" + i + ",file:" + file);
        int i5 = i + (-1);
        int i6 = i5 / this.mAdnFileSize;
        int i7 = i5 % this.mAdnFileSize;
        log("updateType2Ef pbrRecNum:" + i6 + ",iapRecNum:" + i7);
        if (this.mIapFileList != null && file != null && this.mPbrRecords != null && (sparseArray = this.mPbrRecords.get(file.mPbrRecord).mFileIds) != null) {
            try {
                ArrayList<byte[]> arrayList = this.mIapFileList.get(file.mPbrRecord);
                if (arrayList == null) {
                    return;
                }
                if (arrayList.size() == 0) {
                    Rlog.e(LOG_TAG, "Warning: IAP size is 0");
                    return;
                }
                byte[] bArr = arrayList.get(i7);
                if (bArr == null) {
                    return;
                }
                int i8 = bArr[file.getIndex()] & 255;
                log("updateType2Ef orignal index :" + i8);
                if (str == null || str.length() == 0) {
                    if (i8 > 0) {
                        bArr[file.getIndex()] = -1;
                        if (sparseArray.get(193) != null) {
                            this.mFh.updateEFLinearFixed(((File) sparseArray.get(193)).getEfid(), i7 + 1, bArr, (String) null, obtainMessage(7));
                            return;
                        } else {
                            Rlog.e(LOG_TAG, "updateType2Anr Error: No IAP file!");
                            return;
                        }
                    }
                    return;
                }
                int i9 = this.mRecordSize.get(file.getEfid())[2];
                log("updateType2Anr size :" + i9);
                if (i8 <= 0 || i8 > i9) {
                    int[] iArr = new int[i9 + 1];
                    for (int i10 = 1; i10 <= i9; i10++) {
                        iArr[i10] = 0;
                    }
                    for (int i11 = 0; i11 < arrayList.size(); i11++) {
                        byte[] bArr2 = arrayList.get(i11);
                        if (bArr2 != null && (i4 = bArr2[file.getIndex()] & PplMessageManager.Type.INVALID) > 0 && i4 < 255 && i4 <= i9) {
                            iArr[i4] = 1;
                        }
                    }
                    int i12 = 0;
                    File file2 = null;
                    while (true) {
                        if (i12 >= this.mPbrRecords.size()) {
                            break;
                        }
                        if (i12 == file.mPbrRecord || (file2 = (File) this.mPbrRecords.get(i12).mFileIds.get(196 + (i * 256))) == null) {
                            i12++;
                        } else {
                            z = file2.getEfid() == file.getEfid();
                        }
                    }
                    if (z) {
                        try {
                            ArrayList<byte[]> arrayList2 = this.mIapFileList.get(file2.mPbrRecord);
                            if (arrayList2 != null && arrayList2.size() > 0) {
                                for (int i13 = 0; i13 < arrayList2.size(); i13++) {
                                    byte[] bArr3 = arrayList2.get(i13);
                                    if (bArr3 != null && (i2 = bArr3[file2.getIndex()] & PplMessageManager.Type.INVALID) > 0 && i2 < 255 && i2 <= i9) {
                                        iArr[i2] = 1;
                                    }
                                }
                            }
                        } catch (IndexOutOfBoundsException e) {
                            Rlog.e(LOG_TAG, "Index out of bounds.");
                            return;
                        }
                    }
                    int i14 = 1;
                    while (true) {
                        if (i14 <= i9) {
                            if (iArr[i14] != 0) {
                                i14++;
                            } else {
                                i3 = i14;
                                break;
                            }
                        } else {
                            i3 = 0;
                            break;
                        }
                    }
                } else {
                    i3 = i8;
                }
                log("updateType2Anr final index :" + i3);
                if (i3 == 0) {
                    return;
                }
                try {
                    mtkAdnRecord = this.mPhoneBookRecords.get(i5);
                } catch (IndexOutOfBoundsException e2) {
                    Rlog.e(LOG_TAG, "updateType2Anr: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + i5);
                    mtkAdnRecord = null;
                }
                if (mtkAdnRecord == null) {
                    return;
                }
                byte[] bArrBuildAnrRecord = buildAnrRecord(str, this.mAnrRecordSize, mtkAdnRecord.getAasIndex());
                int efid = file.getEfid();
                if (bArrBuildAnrRecord != null) {
                    this.mFh.updateEFLinearFixed(efid, i3, bArrBuildAnrRecord, (String) null, obtainMessage(9));
                    if (i3 != i8) {
                        bArr[file.getIndex()] = (byte) i3;
                        if (sparseArray.get(193) != null) {
                            this.mFh.updateEFLinearFixed(((File) sparseArray.get(193)).getEfid(), i7 + 1, bArr, (String) null, obtainMessage(7));
                        } else {
                            Rlog.e(LOG_TAG, "updateType2Anr Error: No IAP file!");
                        }
                    }
                }
            } catch (IndexOutOfBoundsException e3) {
                Rlog.e(LOG_TAG, "Index out of bounds.");
            }
        }
    }

    private void readAnrRecordSize() {
        logi("readAnrRecordSize");
        if (this.mPbrRecords != null) {
            SparseArray sparseArray = this.mPbrRecords.get(0).mFileIds;
            if (sparseArray == null) {
                Rlog.w(LOG_TAG, "readAnrRecordSize: fileIds null ");
                return;
            }
            File file = (File) sparseArray.get(196);
            if (sparseArray.size() == 0 || file == null) {
                this.mAnrRecordSize = 0;
                Rlog.w(LOG_TAG, "readAnrRecordSize: No anr tag in pbr file ");
                return;
            }
            int[] eFLinearRecordSize = readEFLinearRecordSize(file.getEfid());
            if (eFLinearRecordSize == null || eFLinearRecordSize.length != 3) {
                Rlog.e(LOG_TAG, "readAnrRecordSize: read record size error.");
                return;
            }
            this.mAnrRecordSize = eFLinearRecordSize[0];
            logi("readAnrRecordSize end size = " + this.mAnrRecordSize);
            return;
        }
        Rlog.w(LOG_TAG, "readAnrRecordSize: PBR null ");
    }

    private void readEmailRecordSize() {
        logi("readEmailRecordSize");
        if (this.mPbrRecords != null) {
            SparseArray sparseArray = this.mPbrRecords.get(0).mFileIds;
            if (sparseArray == null) {
                Rlog.w(LOG_TAG, "readEmailRecordSize: fileId null");
                return;
            }
            File file = (File) sparseArray.get(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP);
            if (sparseArray.size() == 0 || file == null) {
                this.mEmailRecordSize = 0;
                Rlog.w(LOG_TAG, "readEmailRecordSize: No email tag in pbr file ");
                return;
            }
            int[] eFLinearRecordSize = readEFLinearRecordSize(file.getEfid());
            if (eFLinearRecordSize == null || eFLinearRecordSize.length != 3) {
                Rlog.e(LOG_TAG, "readEmailRecordSize: read record size error.");
                return;
            }
            this.mEmailFileSize = eFLinearRecordSize[2];
            this.mEmailRecordSize = eFLinearRecordSize[0];
            logi("readEmailRecordSize Size:" + this.mEmailFileSize + "," + this.mEmailRecordSize);
            return;
        }
        Rlog.w(LOG_TAG, "readEmailRecordSize: PBR null");
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
                int size = this.mPbrRecords.size();
                if (this.mAasForAnr == null) {
                    this.mAasForAnr = new ArrayList<>();
                }
                this.mAasForAnr.clear();
                logi("loadAasFiles read num:" + size + ", " + this.mPbrNeedNotify);
                if (!CsimPhbUtil.hasModemPhbEnhanceCapability(this.mFh)) {
                    for (int i = 0; i < size; i++) {
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
        ArrayList<String> arrayList;
        logi("getUsimAasList start");
        ArrayList<AlphaTag> arrayList2 = new ArrayList<>();
        if (!loadAasFiles() || (arrayList = this.mAasForAnr) == null) {
            return arrayList2;
        }
        for (int i = 0; i < 1; i++) {
            int i2 = 0;
            while (i2 < arrayList.size()) {
                String str = arrayList.get(i2);
                StringBuilder sb = new StringBuilder();
                sb.append("aasIndex:");
                i2++;
                sb.append(i2);
                sb.append(",pbrIndex:");
                sb.append(i);
                sb.append(",value:");
                sb.append(str);
                logi(sb.toString());
                arrayList2.add(new AlphaTag(i2, str, i));
            }
        }
        return arrayList2;
    }

    public String getUsimAasById(int i, int i2) {
        logi("getUsimAasById by id " + i + ",pbrIndex " + i2);
        if (!loadAasFiles()) {
            return null;
        }
        ArrayList<String> arrayList = this.mAasForAnr;
        if (arrayList != null) {
            logi("getUsimAasById NonNULL by id " + i + ",pbrIndex " + i2);
            return arrayList.get(i - 1);
        }
        logi("getUsimAasById NULL by id " + i + ",pbrIndex " + i2);
        return null;
    }

    public boolean removeUsimAasById(int i, int i2) {
        logi("removeUsimAasById by id " + i + ",pbrIndex " + i2);
        if (!loadAasFiles()) {
            return false;
        }
        SparseArray sparseArray = this.mPbrRecords.get(i2).mFileIds;
        if (sparseArray == null || sparseArray.get(199) == null) {
            Rlog.e(LOG_TAG, "removeUsimAasById-PBR have no AAS EF file");
            return false;
        }
        log("removeUsimAasById result,efid:" + ((File) sparseArray.get(199)).getEfid());
        if (this.mFh != null) {
            Message messageObtainMessage = obtainMessage(10);
            int usimAasMaxNameLen = getUsimAasMaxNameLen();
            byte[] bArr = new byte[usimAasMaxNameLen];
            for (int i3 = 0; i3 < usimAasMaxNameLen; i3++) {
                bArr[i3] = -1;
            }
            synchronized (this.mLock) {
                this.mCi.deleteUPBEntry(3, 1, i, messageObtainMessage);
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in removesimAasById");
                }
            }
            AsyncResult asyncResult = (AsyncResult) messageObtainMessage.obj;
            if (asyncResult == null || asyncResult.exception == null) {
                ArrayList<String> arrayList = this.mAasForAnr;
                if (arrayList != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("remove aas done ");
                    int i4 = i - 1;
                    sb.append(arrayList.get(i4));
                    log(sb.toString());
                    arrayList.set(i4, null);
                }
                return true;
            }
            Rlog.e(LOG_TAG, "removeUsimAasById exception " + asyncResult.exception);
            return false;
        }
        Rlog.e(LOG_TAG, "removeUsimAasById-IccFileHandler is null");
        return false;
    }

    public int insertUsimAas(String str) {
        boolean z;
        logi("insertUsimAas begin" + str);
        int i = 0;
        if (str == null || str.length() == 0) {
            return 0;
        }
        if (!loadAasFiles()) {
            return -1;
        }
        if (str.length() > getUsimAasMaxNameLen()) {
            return 0;
        }
        synchronized (this.mLock) {
            ArrayList<String> arrayList = this.mAasForAnr;
            int i2 = 0;
            while (true) {
                z = true;
                if (i2 < arrayList.size()) {
                    String str2 = arrayList.get(i2);
                    if (str2 == null || str2.length() == 0) {
                        break;
                    }
                    i2++;
                } else {
                    z = false;
                    break;
                }
            }
            i = i2 + 1;
            log("insertUsimAas aasIndex:" + i + ",found:" + z);
            if (!z) {
                return -2;
            }
            String strEncodeToUcs2 = encodeToUcs2(str);
            Message messageObtainMessage = obtainMessage(10);
            this.mCi.editUPBEntry(3, 0, i, strEncodeToUcs2, null, messageObtainMessage);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "Interrupted Exception in insertUsimAas");
            }
            AsyncResult asyncResult = (AsyncResult) messageObtainMessage.obj;
            logi("insertUsimAas UPB_EF_AAS: ar " + asyncResult);
            if (asyncResult != null && asyncResult.exception != null) {
                Rlog.e(LOG_TAG, "insertUsimAas exception " + asyncResult.exception);
                return -1;
            }
            ArrayList<String> arrayList2 = this.mAasForAnr;
            if (arrayList2 != null) {
                arrayList2.set(i - 1, str);
                logi("insertUsimAas update mAasForAnr done");
            }
            return i;
        }
    }

    public boolean updateUsimAas(int i, int i2, String str) {
        logi("updateUsimAas index " + i + ",pbrIndex " + i2 + ",aasName " + str);
        if (!loadAasFiles()) {
            return false;
        }
        ArrayList<String> arrayList = this.mAasForAnr;
        if (i <= 0 || i > arrayList.size()) {
            Rlog.e(LOG_TAG, "updateUsimAas not found aas index " + i);
            return false;
        }
        int i3 = i - 1;
        log("updateUsimAas old aas " + arrayList.get(i3));
        if (str == null || str.length() == 0) {
            return removeUsimAasById(i, i2);
        }
        int usimAasMaxNameLen = getUsimAasMaxNameLen();
        int length = str.length();
        log("updateUsimAas aas limit " + usimAasMaxNameLen);
        if (length > usimAasMaxNameLen) {
            return false;
        }
        log("updateUsimAas offset 0");
        int i4 = i + 0;
        String strEncodeToUcs2 = encodeToUcs2(str);
        Message messageObtainMessage = obtainMessage(10);
        synchronized (this.mLock) {
            this.mCi.editUPBEntry(3, 0, i4, strEncodeToUcs2, null, messageObtainMessage);
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "Interrupted Exception in updateUsimAas");
            }
        }
        AsyncResult asyncResult = (AsyncResult) messageObtainMessage.obj;
        if (asyncResult == null || asyncResult.exception == null) {
            ArrayList<String> arrayList2 = this.mAasForAnr;
            if (arrayList2 != null) {
                arrayList2.set(i3, str);
                logi("updateUsimAas update mAasForAnr done");
                return true;
            }
            return true;
        }
        Rlog.e(LOG_TAG, "updateUsimAas exception " + asyncResult.exception);
        return false;
    }

    public boolean updateAdnAas(int i, int i2) {
        int i3 = i - 1;
        int i4 = i3 / this.mAdnFileSize;
        int i5 = i3 % this.mAdnFileSize;
        try {
            MtkAdnRecord mtkAdnRecord = this.mPhoneBookRecords.get(i3);
            mtkAdnRecord.setAasIndex(i2);
            for (int i6 = 0; i6 < 3; i6++) {
                updateAnrByAdnIndex(mtkAdnRecord.getAdditionalNumber(i6), i, i6, mtkAdnRecord);
            }
            return true;
        } catch (IndexOutOfBoundsException e) {
            Rlog.e(LOG_TAG, "updateADNAAS: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + i3);
            return false;
        }
    }

    public int getUsimAasMaxNameLen() {
        logi("getUsimAasMaxNameLen begin");
        synchronized (this.mUPBCapabilityLock) {
            if (this.mUpbCap[4] < 0 && checkIsPhbReady()) {
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
            if (this.mUpbCap[3] < 0 && checkIsPhbReady()) {
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
                this.mPbrNeedNotify++;
                readPbrFileAndWait();
            }
        }
    }

    public int getAnrCount() {
        logi("getAnrCount begin");
        synchronized (this.mUPBCapabilityLock) {
            if (this.mUpbCap[0] < 0 && checkIsPhbReady()) {
                this.mCi.queryUPBCapability(obtainMessage(19));
                try {
                    this.mUPBCapabilityLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in getAnrCount");
                }
            }
        }
        if (this.mAnrRecordSize <= 0) {
            logi("getAnrCount end mAnrRecordSize:" + this.mAnrRecordSize);
            return this.mAnrRecordSize;
        }
        logi("getAnrCount done: N_ANR is " + this.mUpbCap[0]);
        return this.mUpbCap[0] > 0 ? 1 : 0;
    }

    public int getEmailCount() {
        logi("getEmailCount begin");
        synchronized (this.mUPBCapabilityLock) {
            if (this.mUpbCap[1] < 0 && checkIsPhbReady()) {
                this.mCi.queryUPBCapability(obtainMessage(19));
                try {
                    this.mUPBCapabilityLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in getEmailCount");
                }
            }
        }
        if (this.mEmailRecordSize <= 0) {
            logi("getEmailCount end mEmailRecordSize:" + this.mEmailRecordSize);
            return this.mEmailRecordSize;
        }
        logi("getEmailCount done: N_EMAIL is " + this.mUpbCap[1]);
        return this.mUpbCap[1] > 0 ? 1 : 0;
    }

    public boolean hasSne() {
        log("hasSne begin");
        synchronized (this.mUPBCapabilityLock) {
            if (this.mUpbCap[2] < 0 && checkIsPhbReady()) {
                this.mCi.queryUPBCapability(obtainMessage(19));
                try {
                    this.mUPBCapabilityLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in hasSne");
                }
            }
        }
        log("hasSne done: N_Sne is " + this.mUpbCap[2]);
        return this.mUpbCap[2] > 0;
    }

    public int getSneRecordLen() {
        SparseArray sparseArray;
        int[] eFLinearRecordSize;
        if (!hasSne()) {
            return 0;
        }
        if (this.mPbrRecords == null || this.mPbrRecords.get(0) == null || (sparseArray = this.mPbrRecords.get(0).mFileIds) == null) {
            return -1;
        }
        File file = (File) sparseArray.get(195);
        if (file == null) {
            return 0;
        }
        int efid = file.getEfid();
        boolean z = file.getParentTag() == 169;
        logi("getSneRecordLen: EFSNE id is " + efid);
        if (this.mRecordSize != null && this.mRecordSize.get(efid) != null) {
            eFLinearRecordSize = this.mRecordSize.get(efid);
        } else {
            eFLinearRecordSize = readEFLinearRecordSize(efid);
        }
        if (eFLinearRecordSize == null) {
            return 0;
        }
        if (z) {
            return eFLinearRecordSize[0] - 2;
        }
        return eFLinearRecordSize[0];
    }

    public int getUpbDone() {
        return this.mUpbDone;
    }

    private void updatePhoneAdnRecordWithSneByIndex(int i, int i2, byte[] bArr) {
        if (bArr == null) {
            return;
        }
        String strAdnStringFieldToString = IccUtils.adnStringFieldToString(bArr, 0, bArr.length);
        log("updatePhoneAdnRecordWithSneByIndex index " + i2 + " recData file is " + strAdnStringFieldToString);
        if (strAdnStringFieldToString != null && !strAdnStringFieldToString.equals("")) {
            try {
                this.mPhoneBookRecords.get(i2).setSne(strAdnStringFieldToString);
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "updatePhoneAdnRecordWithSneByIndex: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + i2);
            }
        }
    }

    public int updateSneByAdnIndex(String str, int i, MtkAdnRecord mtkAdnRecord) {
        logi("updateSneByAdnIndex begin sne is " + str + ",adnIndex " + i);
        int i2 = i + (-1);
        int i3 = i2 / this.mAdnFileSize;
        int i4 = i2 % this.mAdnFileSize;
        if (this.mPbrRecords == null) {
            return -1;
        }
        Message messageObtainMessage = obtainMessage(11);
        SparseArray sparseArray = this.mPbrRecords.get(i3).mFileIds;
        if (sparseArray == null || sparseArray.get(195) == null) {
            log("updateSneByAdnIndex: No SNE tag in pbr file 0");
            return -1;
        }
        if (this.mPhoneBookRecords == null || this.mPhoneBookRecords.isEmpty()) {
            return -1;
        }
        String sne = null;
        if (mtkAdnRecord != null) {
            sne = mtkAdnRecord.getSne();
        }
        log("updateSneByAdnIndex: EF_SNE id is " + Integer.toHexString(((File) sparseArray.get(195)).getEfid()).toUpperCase());
        log("updateSneByAdnIndex: efIndex is 1");
        synchronized (this.mLock) {
            if (str == null) {
                if (sne != null) {
                    this.mCi.deleteUPBEntry(2, 1, i, messageObtainMessage);
                }
                return 0;
            }
            try {
                if (str.length() == 0) {
                    if (sne != null && sne.length() != 0) {
                        this.mCi.deleteUPBEntry(2, 1, i, messageObtainMessage);
                    }
                    return 0;
                }
                if (str.equals(sne)) {
                    return 0;
                }
                this.mCi.editUPBEntry(2, 1, i, encodeToUcs2(str), null, messageObtainMessage);
            } finally {
            }
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "Interrupted Exception in updateSneByAdnIndex");
            }
            logi("updateSneByAdnIndex end sne is " + str + ",adnIndex " + i);
            return this.mResult;
        }
    }

    public int[] getAdnRecordsCapacity() {
        int[] iArr = new int[6];
        if (this.mRefreshAdnInfo || this.mRefreshEmailInfo || this.mRefreshAnrInfo || this.mAdnRecordSize == null || this.mAdnRecordSize.length != 4) {
            getAdnStorageInfo();
            this.mRefreshAdnInfo = false;
        }
        if (this.mAdnRecordSize == null || this.mAdnRecordSize.length != 4) {
            return null;
        }
        iArr[0] = this.mAdnRecordSize[1];
        iArr[1] = this.mAdnRecordSize[0];
        if (this.mRefreshEmailInfo || this.mEmailInfo == null || this.mEmailInfo.length != 3) {
            this.mCi.queryUPBAvailable(1, 1, obtainMessage(25));
            synchronized (this.mLock) {
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in getAdnRecordsCapacity");
                }
            }
            this.mRefreshEmailInfo = false;
        }
        if (this.mEmailInfo == null || this.mEmailInfo.length != 3) {
            return null;
        }
        iArr[2] = this.mEmailInfo[0];
        iArr[3] = this.mEmailInfo[0] - this.mEmailInfo[1];
        if (this.mRefreshAnrInfo || this.mAnrInfo == null || this.mAnrInfo.get(0) == null || this.mAnrInfo.get(0).length != 3) {
            this.mCi.queryUPBAvailable(0, 1, obtainMessage(26));
            synchronized (this.mLock) {
                try {
                    this.mLock.wait();
                } catch (InterruptedException e2) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in getAdnRecordsCapacity");
                }
            }
            this.mRefreshAnrInfo = false;
        }
        if (this.mAnrInfo == null || this.mAnrInfo.get(0) == null || this.mAnrInfo.get(0).length != 3) {
            return null;
        }
        iArr[4] = this.mAnrInfo.get(0)[0];
        iArr[5] = this.mAnrInfo.get(0)[0] - this.mAnrInfo.get(0)[1];
        logi("getAdnRecordsCapacity: max adn=" + iArr[0] + ", used adn=" + iArr[1] + ", max email=" + iArr[2] + ", used email=" + iArr[3] + ", max anr=" + iArr[4] + ", used anr=" + iArr[5]);
        return iArr;
    }

    private int[] getAdnStorageInfo() {
        logi("getAdnStorageInfo");
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
        int i;
        ArrayList<byte[]> arrayList;
        boolean z = this.mCurrentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM;
        logi("getPhonebookMemStorageExt isUsim " + z);
        if (!z) {
            return getPhonebookMemStorageExt2G();
        }
        if (this.mPbrRecords == null) {
            loadPBRFiles();
        }
        if (this.mPbrRecords == null) {
            return null;
        }
        log("getPhonebookMemStorageExt slice " + this.mPbrRecords.size());
        UsimPBMemInfo[] usimPBMemInfoArr = new UsimPBMemInfo[this.mPbrRecords.size()];
        for (int i2 = 0; i2 < this.mPbrRecords.size(); i2++) {
            usimPBMemInfoArr[i2] = new UsimPBMemInfo();
        }
        if (this.mPhoneBookRecords.isEmpty()) {
            Rlog.w(LOG_TAG, "mPhoneBookRecords has not been loaded.");
            return usimPBMemInfoArr;
        }
        for (int i3 = 0; i3 < this.mPbrRecords.size(); i3++) {
            SparseArray sparseArray = this.mPbrRecords.get(i3).mFileIds;
            int size = this.mPhoneBookRecords.size();
            int i4 = this.mAdnFileSize * i3;
            int i5 = this.mAdnFileSize + i4;
            if (size >= i5) {
                size = i5;
            }
            File file = (File) sparseArray.get(192);
            if (file != null) {
                int[] eFLinearRecordSize = readEFLinearRecordSize(file.getEfid());
                if (eFLinearRecordSize != null) {
                    usimPBMemInfoArr[i3].setAdnLength(eFLinearRecordSize[0]);
                    usimPBMemInfoArr[i3].setAdnTotal(eFLinearRecordSize[2]);
                }
                usimPBMemInfoArr[i3].setAdnType(file.getParentTag());
                usimPBMemInfoArr[i3].setSliceIndex(i3 + 1);
                MtkAdnRecord mtkAdnRecord = null;
                int i6 = 0;
                for (int i7 = i4; i7 < size; i7++) {
                    try {
                        mtkAdnRecord = this.mPhoneBookRecords.get(i7);
                    } catch (IndexOutOfBoundsException e) {
                        Rlog.e(LOG_TAG, "getPhonebookMemStorageExt: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + i7);
                    }
                    if (mtkAdnRecord != null && ((mtkAdnRecord.getAlphaTag() != null && mtkAdnRecord.getAlphaTag().length() > 0) || (mtkAdnRecord.getNumber() != null && mtkAdnRecord.getNumber().length() > 0))) {
                        log("Adn: " + mtkAdnRecord.toString());
                        i6++;
                        mtkAdnRecord = null;
                    }
                }
                log("adn used " + i6);
                usimPBMemInfoArr[i3].setAdnUsed(i6);
            }
            File file2 = (File) sparseArray.get(196);
            if (file2 != null) {
                int[] eFLinearRecordSize2 = readEFLinearRecordSize(file2.getEfid());
                if (eFLinearRecordSize2 != null) {
                    usimPBMemInfoArr[i3].setAnrLength(eFLinearRecordSize2[0]);
                    usimPBMemInfoArr[i3].setAnrTotal(eFLinearRecordSize2[2]);
                }
                usimPBMemInfoArr[i3].setAnrType(file2.getParentTag());
                MtkAdnRecord mtkAdnRecord2 = null;
                int i8 = 0;
                for (int i9 = i4; i9 < size; i9++) {
                    try {
                        mtkAdnRecord2 = this.mPhoneBookRecords.get(i9);
                    } catch (IndexOutOfBoundsException e2) {
                        Rlog.e(LOG_TAG, "getPhonebookMemStorageExt: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + i9);
                    }
                    if (mtkAdnRecord2 == null) {
                        log("null anr rec ");
                    } else {
                        String additionalNumber = mtkAdnRecord2.getAdditionalNumber();
                        if (additionalNumber != null && additionalNumber.length() > 0) {
                            log("anrStr: " + additionalNumber);
                            i8++;
                        }
                    }
                }
                log("anr used: " + i8);
                usimPBMemInfoArr[i3].setAnrUsed(i8);
            }
            File file3 = (File) sparseArray.get(ExternalSimConstants.EVENT_TYPE_RECEIVE_RSIM_AUTH_RSP);
            if (file3 != null) {
                int[] eFLinearRecordSize3 = readEFLinearRecordSize(file3.getEfid());
                if (eFLinearRecordSize3 != null) {
                    usimPBMemInfoArr[i3].setEmailLength(eFLinearRecordSize3[0]);
                    usimPBMemInfoArr[i3].setEmailTotal(eFLinearRecordSize3[2]);
                }
                usimPBMemInfoArr[i3].setEmailType(file3.getParentTag());
                MtkAdnRecord mtkAdnRecord3 = null;
                int i10 = 0;
                while (i4 < size) {
                    try {
                        mtkAdnRecord3 = this.mPhoneBookRecords.get(i4);
                    } catch (IndexOutOfBoundsException e3) {
                        Rlog.e(LOG_TAG, "getPhonebookMemStorageExt: mPhoneBookRecords IndexOutOfBoundsException mPhoneBookRecords.size() is " + this.mPhoneBookRecords.size() + "index is " + i4);
                    }
                    if (mtkAdnRecord3 == null) {
                        log("null email rec ");
                    } else {
                        String[] emails = mtkAdnRecord3.getEmails();
                        if (emails != null && emails.length > 0 && emails[0].length() > 0) {
                            log("email: " + emails[0]);
                            i10++;
                        }
                    }
                    i4++;
                }
                log("email used: " + i10);
                usimPBMemInfoArr[i3].setEmailUsed(i10);
            }
            File file4 = (File) sparseArray.get(194);
            if (file4 != null) {
                int[] eFLinearRecordSize4 = readEFLinearRecordSize(file4.getEfid());
                if (eFLinearRecordSize4 != null) {
                    usimPBMemInfoArr[i3].setExt1Length(eFLinearRecordSize4[0]);
                    usimPBMemInfoArr[i3].setExt1Total(eFLinearRecordSize4[2]);
                }
                usimPBMemInfoArr[i3].setExt1Type(file4.getParentTag());
                synchronized (this.mLock) {
                    readExt1FileAndWait(i3);
                }
                if (this.mExt1FileList != null && i3 < this.mExt1FileList.size() && (arrayList = this.mExt1FileList.get(i3)) != null) {
                    int size2 = arrayList.size();
                    i = 0;
                    for (int i11 = 0; i11 < size2; i11++) {
                        byte[] bArr = arrayList.get(i11);
                        log("ext1[" + i11 + "]=" + IccUtils.bytesToHexString(bArr));
                        if (bArr != null && bArr.length > 0 && (bArr[0] == 1 || bArr[0] == 2)) {
                            i++;
                        }
                    }
                } else {
                    i = 0;
                }
                usimPBMemInfoArr[i3].setExt1Used(i);
            }
            File file5 = (File) sparseArray.get(200);
            if (file5 != null) {
                int[] eFLinearRecordSize5 = readEFLinearRecordSize(file5.getEfid());
                if (eFLinearRecordSize5 != null) {
                    usimPBMemInfoArr[i3].setGasLength(eFLinearRecordSize5[0]);
                    usimPBMemInfoArr[i3].setGasTotal(eFLinearRecordSize5[2]);
                }
                usimPBMemInfoArr[i3].setGasType(file5.getParentTag());
            }
            File file6 = (File) sparseArray.get(199);
            if (file6 != null) {
                int[] eFLinearRecordSize6 = readEFLinearRecordSize(file6.getEfid());
                if (eFLinearRecordSize6 != null) {
                    usimPBMemInfoArr[i3].setAasLength(eFLinearRecordSize6[0]);
                    usimPBMemInfoArr[i3].setAasTotal(eFLinearRecordSize6[2]);
                }
                usimPBMemInfoArr[i3].setAasType(file6.getParentTag());
            }
            File file7 = (File) sparseArray.get(195);
            if (file7 != null) {
                int[] eFLinearRecordSize7 = readEFLinearRecordSize(file7.getEfid());
                if (eFLinearRecordSize7 != null) {
                    usimPBMemInfoArr[i3].setSneLength(eFLinearRecordSize7[0]);
                    usimPBMemInfoArr[i3].setSneTotal(eFLinearRecordSize7[0]);
                }
                usimPBMemInfoArr[i3].setSneType(file7.getParentTag());
            }
            File file8 = (File) sparseArray.get(ExternalSimConstants.EVENT_TYPE_RSIM_AUTH_DONE);
            if (file8 != null) {
                int[] eFLinearRecordSize8 = readEFLinearRecordSize(file8.getEfid());
                if (eFLinearRecordSize8 != null) {
                    usimPBMemInfoArr[i3].setCcpLength(eFLinearRecordSize8[0]);
                    usimPBMemInfoArr[i3].setCcpTotal(eFLinearRecordSize8[0]);
                }
                usimPBMemInfoArr[i3].setCcpType(file8.getParentTag());
            }
        }
        for (int i12 = 0; i12 < this.mPbrRecords.size(); i12++) {
            log("getPhonebookMemStorageExt[" + i12 + "]:" + usimPBMemInfoArr[i12]);
        }
        return usimPBMemInfoArr;
    }

    public UsimPBMemInfo[] getPhonebookMemStorageExt2G() {
        int i;
        ArrayList<byte[]> arrayList;
        UsimPBMemInfo[] usimPBMemInfoArr = {new UsimPBMemInfo()};
        int[] eFLinearRecordSize = readEFLinearRecordSize(28474);
        if (eFLinearRecordSize != null) {
            usimPBMemInfoArr[0].setAdnLength(eFLinearRecordSize[0]);
            if (isAdnAccessible()) {
                usimPBMemInfoArr[0].setAdnTotal(eFLinearRecordSize[2]);
            } else {
                usimPBMemInfoArr[0].setAdnTotal(0);
            }
        }
        usimPBMemInfoArr[0].setAdnType(168);
        usimPBMemInfoArr[0].setSliceIndex(1);
        int[] eFLinearRecordSize2 = readEFLinearRecordSize(28490);
        if (eFLinearRecordSize2 != null) {
            usimPBMemInfoArr[0].setExt1Length(eFLinearRecordSize2[0]);
            usimPBMemInfoArr[0].setExt1Total(eFLinearRecordSize2[2]);
        }
        usimPBMemInfoArr[0].setExt1Type(170);
        synchronized (this.mLock) {
            if (this.mFh != null) {
                Message messageObtainMessage = obtainMessage(1001);
                messageObtainMessage.arg1 = 0;
                this.mFh.loadEFLinearFixedAll(28490, messageObtainMessage);
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in readExt1FileAndWait");
                }
                if (this.mExt1FileList != null && this.mExt1FileList.size() > 0 && (arrayList = this.mExt1FileList.get(0)) != null) {
                    int size = arrayList.size();
                    i = 0;
                    for (int i2 = 0; i2 < size; i2++) {
                        byte[] bArr = arrayList.get(i2);
                        log("ext1[" + i2 + "]=" + IccUtils.bytesToHexString(bArr));
                        if (bArr != null && bArr.length > 0 && (bArr[0] == 1 || bArr[0] == 2)) {
                            i++;
                        }
                    }
                } else {
                    i = 0;
                }
                usimPBMemInfoArr[0].setExt1Used(i);
                logi("getPhonebookMemStorageExt2G:" + usimPBMemInfoArr[0]);
                return usimPBMemInfoArr;
            }
            Rlog.e(LOG_TAG, "readExt1FileAndWait-IccFileHandler is null");
            return usimPBMemInfoArr;
        }
    }

    public int[] readEFLinearRecordSize(int i) {
        int[] iArr;
        logi("readEFLinearRecordSize fileid " + Integer.toHexString(i).toUpperCase());
        Message messageObtainMessage = obtainMessage(1000);
        messageObtainMessage.arg1 = i;
        synchronized (this.mLock) {
            if (this.mFh != null) {
                this.mFh.getEFLinearRecordSize(i, messageObtainMessage);
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in readEFLinearRecordSize");
                }
            } else {
                Rlog.e(LOG_TAG, "readEFLinearRecordSize-IccFileHandler is null");
            }
            iArr = this.mRecordSize != null ? this.mRecordSize.get(i) : null;
            if (iArr != null) {
                logi("readEFLinearRecordSize fileid:" + Integer.toHexString(i).toUpperCase() + ",len:" + iArr[0] + ",total:" + iArr[1] + ",count:" + iArr[2]);
            } else {
                logi("readEFLinearRecordSize fileid:" + Integer.toHexString(i).toUpperCase() + ",recordSize: null");
            }
        }
        return iArr;
    }

    private void readExt1FileAndWait(int i) {
        logi("readExt1FileAndWait " + i);
        if (this.mPbrRecords != null && this.mPbrRecords.get(i) != null) {
            SparseArray sparseArray = this.mPbrRecords.get(i).mFileIds;
            if (sparseArray == null || sparseArray.get(194) == null) {
                Rlog.e(LOG_TAG, "readExt1FileAndWait-PBR have no Ext1 record");
                return;
            }
            int efid = ((File) sparseArray.get(194)).getEfid();
            log("readExt1FileAndWait-get EXT1 EFID " + efid);
            if (this.mExt1FileList != null && i < this.mExt1FileList.size()) {
                log("EXT1 has been loaded for Pbr number " + i);
                return;
            }
            if (this.mFh != null) {
                Message messageObtainMessage = obtainMessage(1001);
                messageObtainMessage.arg1 = i;
                this.mFh.loadEFLinearFixedAll(efid, messageObtainMessage);
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
    }

    private boolean checkIsPhbReady() {
        String str = "";
        if (!SubscriptionManager.isValidSlotIndex(this.mSlotId)) {
            log("[isPhbReady] InvalidSlotId slotId: " + this.mSlotId);
            return false;
        }
        String str2 = SystemProperties.get("gsm.sim.state");
        if (str2 != null && str2.length() > 0) {
            String[] strArrSplit = str2.split(",");
            if (this.mSlotId >= 0 && this.mSlotId < strArrSplit.length && strArrSplit[this.mSlotId] != null) {
                str = strArrSplit[this.mSlotId];
            }
        }
        boolean z = str.equals("NETWORK_LOCKED") || str.equals("PIN_REQUIRED");
        String telephonyProperty = TelephonyManager.getTelephonyProperty(this.mSlotId, "vendor.gsm.sim.ril.phbready", "false");
        logi("[isPhbReady] isPhbReady: " + telephonyProperty + ",strSimState: " + str2);
        return telephonyProperty.equals("true") && !z;
    }

    public boolean isAdnAccessible() {
        if (this.mFh != null && this.mCurrentApp.getType() == IccCardApplicationStatus.AppType.APPTYPE_SIM) {
            synchronized (this.mLock) {
                this.mFh.selectEFFile(28474, obtainMessage(20));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in isAdnAccessible");
                }
            }
            return this.mEfData == null || (this.mEfData.getFileStatus() & 5) > 0;
        }
        return true;
    }

    public boolean isUsimPhbEfAndNeedReset(int i) {
        logi("isUsimPhbEfAndNeedReset, fileId: " + Integer.toHexString(i).toUpperCase());
        if (this.mPbrRecords == null) {
            Rlog.e(LOG_TAG, "isUsimPhbEfAndNeedReset, No PBR files");
            return false;
        }
        int size = this.mPbrRecords.size();
        for (int i2 = 0; i2 < size; i2++) {
            SparseArray sparseArray = this.mPbrRecords.get(i2).mFileIds;
            for (int i3 = 192; i3 <= 203; i3++) {
                if (i3 == 197 || i3 == 201 || i3 == 203) {
                    logi("isUsimPhbEfAndNeedReset, not reset EF: " + i3);
                } else if (sparseArray.get(i3) != null && i == ((File) sparseArray.get(i3)).getEfid()) {
                    logi("isUsimPhbEfAndNeedReset, return true with EF: " + i3);
                    return true;
                }
            }
        }
        log("isUsimPhbEfAndNeedReset, return false.");
        return false;
    }

    private void readAdnFileAndWaitForUICC(int i) {
        SparseArray sparseArray;
        logi("readAdnFileAndWaitForUICC begin" + i);
        if (this.mPbrRecords == null || (sparseArray = this.mPbrRecords.get(i).mFileIds) == null || sparseArray.size() == 0) {
            return;
        }
        if (sparseArray.get(192) == null) {
            Rlog.e(LOG_TAG, "readAdnFileAndWaitForUICC: No ADN tag in pbr record " + i);
            return;
        }
        int efid = ((File) sparseArray.get(192)).getEfid();
        log("readAdnFileAndWaitForUICC: EFADN id is " + efid);
        log("UiccPhoneBookManager readAdnFileAndWaitForUICC: recId is " + i + "");
        this.mAdnCache.requestLoadAllAdnLike(efid, this.mAdnCache.extensionEfForEf(28474), obtainMessage(2));
        try {
            this.mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readAdnFileAndWait");
        }
        int size = this.mPhoneBookRecords.size();
        if (this.mPbrRecords != null && this.mPbrRecords.size() > i) {
            this.mPbrRecords.get(i).mMasterFileRecordNum = this.mPhoneBookRecords.size() - size;
        }
        logi("readAdnFileAndWaitForUICC end" + i);
    }

    private ArrayList<MtkAdnRecord> changeAdnRecordNumber(int i, ArrayList<MtkAdnRecord> arrayList) {
        int size = arrayList.size();
        for (int i2 = 0; i2 < size; i2++) {
            MtkAdnRecord mtkAdnRecord = arrayList.get(i2);
            if (mtkAdnRecord != null) {
                mtkAdnRecord.setRecordIndex(mtkAdnRecord.getRecId() + i);
            }
        }
        return arrayList;
    }
}
