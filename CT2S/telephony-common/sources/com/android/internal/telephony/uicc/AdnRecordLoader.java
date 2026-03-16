package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.GsmAlphabet;
import java.util.ArrayList;
import java.util.Iterator;

public class AdnRecordLoader extends Handler {
    static final int EVENT_ADN_LOAD_ALL_DONE = 3;
    static final int EVENT_ADN_LOAD_DONE = 1;
    static final int EVENT_EF_LINEAR_RECORD_SIZE_DONE = 4;
    static final int EVENT_EF_LINEAR_RECORD_SIZE_DONE_ANR = 7;
    static final int EVENT_EF_LINEAR_RECORD_SIZE_DONE_EMAIL = 6;
    static final int EVENT_EF_LINEAR_RECORD_SIZE_DONE_GRP = 8;
    static final int EVENT_EF_LINEAR_RECORD_SIZE_DONE_GSD = 10;
    static final int EVENT_EF_LINEAR_RECORD_SIZE_DONE_SNE = 9;
    static final int EVENT_EXT1_RECORD_LOAD_ALL_DONE = 11;
    static final int EVENT_EXT2_RECORD_LOAD_ALL_DONE = 13;
    static final int EVENT_EXT_RECORD_LOAD_DONE = 2;
    static final int EVENT_UPDATE_ADN_RECORD_DONE = 15;
    static final int EVENT_UPDATE_ANR_RECORD_DONE = 16;
    static final int EVENT_UPDATE_EXT1_RECORD_DONE = 12;
    static final int EVENT_UPDATE_EXT2_RECORD_DONE = 14;
    static final int EVENT_UPDATE_RECORD_DONE = 5;
    static final String LOG_TAG = "AdnRecordLoader";
    private static final int MODE_ADN = 1;
    private static final int MODE_ANR = 2;
    private static final int MODE_FDN = 3;
    static final boolean VDBG = false;
    private int accessPort;
    int adnIndex;
    int extIndex;
    byte[] extbyte;
    private AdnRecordCache mAdnCache;
    ArrayList<AdnRecord> mAdns;
    byte[] mData;
    int mEf;
    int mExtensionEF;
    private IccFileHandler mFh;
    int mFirstIndex;
    int mPendingExtLoads;
    String mPin2;
    int mRecordNumber;
    Object mResult;
    int mUsed;
    Message mUserResponse;
    String pendingExtString;

    AdnRecordLoader(IccFileHandler fh) {
        super(Looper.getMainLooper());
        this.extIndex = 0;
        this.accessPort = 0;
        this.mFh = fh;
    }

    public AdnRecordLoader(IccFileHandler fh, AdnRecordCache cache) {
        this(fh);
        this.mAdnCache = cache;
    }

    public void loadFromEF(int ef, int extensionEF, int recordNumber, Message response) {
        this.mEf = ef;
        this.mExtensionEF = extensionEF;
        this.mRecordNumber = recordNumber;
        this.mUserResponse = response;
        this.mFh.loadEFLinearFixed(ef, recordNumber, obtainMessage(1));
    }

    public void loadAllFromEF(int ef, int extensionEF, Message response) {
        this.mEf = ef;
        this.mExtensionEF = extensionEF;
        this.mUserResponse = response;
        if (extensionEF > 0) {
            if (extensionEF == 28490 || (this.mAdnCache != null && extensionEF == this.mAdnCache.mUSIMExt1)) {
                this.mFh.loadEFLinearFixedAll(extensionEF, obtainMessage(11));
                return;
            } else if (extensionEF == 28491) {
                this.mFh.loadEFLinearFixedAll(extensionEF, obtainMessage(13));
                return;
            } else {
                Rlog.w(LOG_TAG, "don't support this extensionEF: " + extensionEF + ", load EF: " + ef + " directy");
                this.mFh.loadEFLinearFixedAll(ef, obtainMessage(3));
                return;
            }
        }
        Rlog.d(LOG_TAG, "no extensionEF");
        this.mFh.loadEFLinearFixedAll(ef, obtainMessage(3));
    }

    public void loadRecordsFromEF(int ef, int extensionEF, int firstIndex, int numOfRecords, Message response) {
        this.mEf = ef;
        this.mExtensionEF = extensionEF;
        this.mUserResponse = response;
        this.mFirstIndex = firstIndex;
        this.mUsed = numOfRecords;
        if (extensionEF > 0) {
            if (extensionEF == 28490 || (this.mAdnCache != null && extensionEF == this.mAdnCache.mUSIMExt1)) {
                this.mFh.loadEFLinearFixedAll(extensionEF, obtainMessage(11));
                return;
            } else if (extensionEF == 28491) {
                this.mFh.loadEFLinearFixedAll(extensionEF, firstIndex, numOfRecords, obtainMessage(13));
                return;
            } else {
                Rlog.w(LOG_TAG, "don't support this extensionEF: " + extensionEF + ", load EF: " + ef + " directy");
                this.mFh.loadEFLinearFixedAll(ef, firstIndex, numOfRecords, obtainMessage(3));
                return;
            }
        }
        Rlog.d(LOG_TAG, "no extensionEF");
        this.mFh.loadEFLinearFixedAll(ef, firstIndex, numOfRecords, obtainMessage(3));
    }

    public void updateEF(AdnRecord adn, int ef, int extensionEF, int recordNumber, String pin2, Message response) {
        this.mEf = ef;
        this.mExtensionEF = extensionEF;
        this.mRecordNumber = recordNumber;
        this.mUserResponse = response;
        this.mPin2 = pin2;
        this.mFh.getEFLinearRecordSize(this.mEf, obtainMessage(4, adn));
    }

    public void updateEmailEF(AdnRecord adn, int emailEF, int recordEmailIndex, int adnIndex, String pin2, Message response) {
        this.mEf = emailEF;
        this.mRecordNumber = recordEmailIndex;
        this.mUserResponse = response;
        this.adnIndex = adnIndex;
        this.mPin2 = pin2;
        this.mFh.getEFLinearRecordSize(this.mEf, obtainMessage(6, adn.mEmails[0]));
    }

    public void updateAnrEF(AdnRecord adn, int anrEF, int extensionEF, int recordAnrIndex, int adnIndex, String pin2, Message response) {
        this.mEf = anrEF;
        this.mExtensionEF = extensionEF;
        this.mRecordNumber = recordAnrIndex;
        this.mUserResponse = response;
        this.adnIndex = adnIndex;
        this.mPin2 = pin2;
        this.mFh.getEFLinearRecordSize(this.mEf, obtainMessage(7, adn));
    }

    public void updateSneEF(AdnRecord adn, int sneEF, int recordSneIndex, int adnIndex, String pin2, Message response) {
        this.mEf = sneEF;
        this.mRecordNumber = recordSneIndex;
        this.mUserResponse = response;
        this.adnIndex = adnIndex;
        this.mPin2 = pin2;
        this.mFh.getEFLinearRecordSize(this.mEf, obtainMessage(9, adn.snes[0]));
    }

    public void updateGrpEF(AdnRecord adn, int grpEF, int grpIndex, String pin2, Message response) {
        this.mEf = grpEF;
        this.mRecordNumber = grpIndex;
        this.mUserResponse = response;
        this.adnIndex = grpIndex;
        this.mPin2 = pin2;
        this.mFh.getEFLinearRecordSize(this.mEf, obtainMessage(8, adn.grps));
    }

    public void updateGsdEF(int gspEf, int grpId, String grpTag, String pin2, Message response) {
        this.mEf = gspEf;
        this.mRecordNumber = grpId;
        this.mUserResponse = response;
        this.mPin2 = pin2;
        this.mFh.getEFLinearRecordSize(this.mEf, obtainMessage(10, grpTag));
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            switch (msg.what) {
                case 1:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    byte[] data = (byte[]) ar.result;
                    if (ar.exception != null) {
                        throw new RuntimeException("load failed", ar.exception);
                    }
                    AdnRecord adn = new AdnRecord(this.mEf, this.mRecordNumber, data);
                    this.mResult = adn;
                    if (adn.hasExtendedRecord()) {
                        this.mPendingExtLoads = 1;
                        this.mFh.loadEFLinearFixed(this.mExtensionEF, adn.mExtRecord, obtainMessage(2, adn));
                    }
                    if (this.mUserResponse != null && this.mPendingExtLoads == 0) {
                        AsyncResult.forMessage(this.mUserResponse).result = this.mResult;
                        this.mUserResponse.sendToTarget();
                        this.mUserResponse = null;
                        this.extIndex = 0;
                        this.mData = null;
                        return;
                    }
                    return;
                case 2:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    byte[] data2 = (byte[]) ar2.result;
                    AdnRecord adn2 = (AdnRecord) ar2.userObj;
                    if (ar2.exception != null) {
                        throw new RuntimeException("load failed", ar2.exception);
                    }
                    Rlog.d(LOG_TAG, "ADN extension EF: 0x" + Integer.toHexString(this.mExtensionEF) + ":" + adn2.mExtRecord + "\n" + IccUtils.bytesToHexString(data2));
                    adn2.appendExtRecord(data2);
                    this.mPendingExtLoads--;
                    if (this.mUserResponse != null) {
                        return;
                    } else {
                        return;
                    }
                case 3:
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    ArrayList<byte[]> datas = (ArrayList) ar3.result;
                    if (ar3.exception != null) {
                        throw new RuntimeException("load failed", ar3.exception);
                    }
                    this.mAdns = new ArrayList<>(datas.size());
                    this.mResult = this.mAdns;
                    this.mPendingExtLoads = 0;
                    int s = datas.size();
                    for (int i = 0; i < s; i++) {
                        AdnRecord adn3 = new AdnRecord(this.mEf, i + 1, datas.get(i));
                        this.mAdns.add(adn3);
                        if ((this.mExtensionEF == 28490 || this.mExtensionEF == this.mAdnCache.mUSIMExt1) && this.mAdnCache != null && adn3.hasExtendedRecord() && !this.mAdnCache.mAdnExt1Map.isEmpty()) {
                            byte[] data3 = this.mAdnCache.mAdnExt1Map.get(Integer.valueOf(adn3.mExtRecord));
                            Rlog.d(LOG_TAG, "ADN extension EF: 0x" + Integer.toHexString(this.mExtensionEF) + ":" + adn3.mExtRecord + "\n" + IccUtils.bytesToHexString(data3));
                            adn3.appendExtRecord(data3);
                        } else if (this.mExtensionEF == 28491 && this.mAdnCache != null && adn3.hasExtendedRecord() && !this.mAdnCache.mAdnExt2Map.isEmpty()) {
                            byte[] data4 = this.mAdnCache.mAdnExt2Map.get(Integer.valueOf(adn3.mExtRecord));
                            Rlog.d(LOG_TAG, "FDN extension EF: 0x" + Integer.toHexString(this.mExtensionEF) + ":" + adn3.mExtRecord + "\n" + IccUtils.bytesToHexString(data4));
                            adn3.appendExtRecord(data4);
                        }
                    }
                    if (this.mUserResponse != null) {
                    }
                    break;
                case 4:
                    AsyncResult ar4 = (AsyncResult) msg.obj;
                    AdnRecord adn4 = (AdnRecord) ar4.userObj;
                    this.mResult = adn4;
                    if (ar4.exception != null) {
                        throw new RuntimeException("get EF record size failed", ar4.exception);
                    }
                    int[] recordSize = (int[]) ar4.result;
                    if (recordSize.length != 3 || this.mRecordNumber > recordSize[2]) {
                        throw new RuntimeException("get wrong EF record size format", ar4.exception);
                    }
                    if (adn4.mNumber.length() > 40) {
                        throw new RuntimeException("wrong ADN format, number length over 40", ar4.exception);
                    }
                    if (adn4.mNumber.length() > 20) {
                        if (this.mExtensionEF == 28490 || (this.mAdnCache != null && this.mExtensionEF == this.mAdnCache.mUSIMExt1)) {
                            if (this.mAdnCache != null && this.mAdnCache.mAdnExt1Map.isEmpty()) {
                                throw new RuntimeException("ADN number is over length", ar4.exception);
                            }
                        } else if (this.mExtensionEF == 28491 && this.mAdnCache != null && this.mAdnCache.mAdnExt2Map.isEmpty()) {
                            throw new RuntimeException("FDN number is over length", ar4.exception);
                        }
                        this.mPendingExtLoads = 2;
                        String temp = adn4.mNumber;
                        String temp2 = (String) temp.subSequence(0, 20);
                        adn4.mNumber = temp2;
                        this.pendingExtString = (String) temp.subSequence(20, temp.length());
                        Rlog.v(LOG_TAG, " [adn] Split long String to shorts");
                        Rlog.v(LOG_TAG, " EFadn String = " + temp2);
                        Rlog.v(LOG_TAG, " pendingExtString = " + this.pendingExtString);
                        adn4.mNumber = temp2;
                        if (this.mExtensionEF == 28490 || (this.mAdnCache != null && this.mExtensionEF == this.mAdnCache.mUSIMExt1)) {
                            findValidExtIndex(adn4, 1, ar4);
                        } else if (this.mExtensionEF == 28491) {
                            findValidExtIndex(adn4, 3, ar4);
                        }
                        adn4.mExtRecord = this.extIndex & 255;
                        byte[] data5 = adn4.buildAdnString(recordSize[0], adn4.mExtRecord);
                        if (this.mExtensionEF == 28491) {
                            this.mData = adn4.buildAdnString(recordSize[0], adn4.mExtRecord);
                        }
                        adn4.mNumber = temp;
                        this.mResult = adn4;
                        if (data5 == null) {
                            throw new RuntimeException("wrong ADN format", ar4.exception);
                        }
                        if (this.mExtensionEF == 28491) {
                            Rlog.d(LOG_TAG, "update FDN ext2 ");
                            try {
                                this.extbyte = new byte[13];
                                for (int i2 = 0; i2 < 13; i2++) {
                                    this.extbyte[i2] = -1;
                                }
                                byte[] bcdNumber = PhoneNumberUtils.numberToCalledPartyBCD(this.pendingExtString);
                                if (bcdNumber != null) {
                                    System.arraycopy(bcdNumber, 1, this.extbyte, 2, bcdNumber.length - 1);
                                    this.extbyte[0] = 2;
                                    this.extbyte[1] = (byte) ((bcdNumber.length - 1) & 255);
                                }
                                AdnRecord adn5 = (AdnRecord) this.mResult;
                                this.mFh.updateEFLinearFixed(this.mExtensionEF, this.extIndex, this.extbyte, this.mPin2, obtainMessage(14));
                                this.accessPort = 15;
                                this.mResult = adn5;
                            } catch (Exception e) {
                                e.printStackTrace();
                                throw new RuntimeException("update EF fdn ext2 record failed, full ext2", ar4.exception);
                            }
                        } else {
                            this.mFh.updateEFLinearFixed(this.mEf, this.mRecordNumber, data5, this.mPin2, obtainMessage(15));
                        }
                    } else {
                        if (adn4.hasExtendedRecord()) {
                            if ((this.mExtensionEF == 28490 || (this.mAdnCache != null && this.mExtensionEF == this.mAdnCache.mUSIMExt1)) && this.mAdnCache != null && this.mAdnCache.mAdnExt1Map.isEmpty()) {
                                throw new RuntimeException("pervious ADN number format is wrong", ar4.exception);
                            }
                            if (this.mExtensionEF == 28491 && this.mAdnCache != null && this.mAdnCache.mAdnExt2Map.isEmpty()) {
                                throw new RuntimeException("pervious FDN number format is wrong", ar4.exception);
                            }
                            Rlog.v(LOG_TAG, "<20 adn.extRecord1 = " + adn4.mExtRecord);
                            byte[] nullData = new byte[13];
                            for (int i3 = 0; i3 < 13; i3++) {
                                nullData[i3] = -1;
                            }
                            if (this.mExtensionEF == 28490 || (this.mAdnCache != null && this.mExtensionEF == this.mAdnCache.mUSIMExt1)) {
                                if (this.mAdnCache != null) {
                                    this.mAdnCache.mAdnExt1Map.put(Integer.valueOf(adn4.mExtRecord), nullData);
                                }
                            } else if (this.mAdnCache != null && this.mExtensionEF == 28491) {
                                this.mAdnCache.mAdnExt2Map.put(Integer.valueOf(adn4.mExtRecord), nullData);
                            }
                            this.extIndex = adn4.mExtRecord & 255;
                            adn4.mExtRecord = 255;
                            this.mPendingExtLoads = 2;
                            this.pendingExtString = "";
                            byte[] data6 = adn4.buildAdnString(recordSize[0]);
                            this.extbyte = new byte[13];
                            for (int i4 = 0; i4 < 13; i4++) {
                                this.extbyte[i4] = -1;
                            }
                            if (this.mExtensionEF == 28491) {
                                this.mData = adn4.buildAdnString(recordSize[0], adn4.mExtRecord);
                            }
                            this.mResult = adn4;
                            if (data6 == null) {
                                throw new RuntimeException("wrong ADN format", ar4.exception);
                            }
                            if (this.mExtensionEF == 28491) {
                                Rlog.d(LOG_TAG, "update FDN ext2 ");
                                try {
                                    AdnRecord adn6 = (AdnRecord) this.mResult;
                                    this.mFh.updateEFLinearFixed(this.mExtensionEF, this.extIndex, nullData, this.mPin2, obtainMessage(14));
                                    this.accessPort = 15;
                                    this.mResult = adn6;
                                } catch (Exception e2) {
                                    e2.printStackTrace();
                                    throw new RuntimeException("update EF fdn ext2 record failed, full ext2", ar4.exception);
                                }
                            } else {
                                this.mFh.updateEFLinearFixed(this.mEf, this.mRecordNumber, data6, this.mPin2, obtainMessage(15));
                            }
                        } else {
                            byte[] data7 = adn4.buildAdnString(recordSize[0]);
                            this.mPendingExtLoads = 1;
                            if (data7 == null) {
                                throw new RuntimeException("wrong ADN format", ar4.exception);
                            }
                            this.mFh.updateEFLinearFixed(this.mEf, this.mRecordNumber, data7, this.mPin2, obtainMessage(5));
                            this.mPendingExtLoads = 1;
                        }
                        break;
                    }
                    if (this.mUserResponse != null) {
                    }
                    break;
                case 5:
                    AsyncResult ar5 = (AsyncResult) msg.obj;
                    if (ar5.exception != null) {
                        throw new RuntimeException("update EF adn record failed", ar5.exception);
                    }
                    AdnRecord adn7 = (AdnRecord) this.mResult;
                    if (adn7 != null) {
                        if (this.accessPort == 16) {
                            if (this.mAdnCache != null && this.mAdnCache.mAdnExt1Map.containsKey(Integer.valueOf(adn7.extRecordAnr0))) {
                                this.mAdnCache.mAdnExt1Map.put(Integer.valueOf(adn7.extRecordAnr0), this.extbyte);
                            }
                        } else if (this.accessPort == 15) {
                            if (this.mExtensionEF == 28490 || (this.mAdnCache != null && this.mExtensionEF == this.mAdnCache.mUSIMExt1)) {
                                if (this.mAdnCache != null && this.mAdnCache.mAdnExt1Map.containsKey(Integer.valueOf(adn7.mExtRecord))) {
                                    this.mAdnCache.mAdnExt1Map.put(Integer.valueOf(adn7.mExtRecord), this.extbyte);
                                }
                            } else if (this.mAdnCache != null && this.mExtensionEF == 28491 && this.mAdnCache.mAdnExt2Map.containsKey(Integer.valueOf(adn7.mExtRecord))) {
                                this.mAdnCache.mAdnExt2Map.put(Integer.valueOf(adn7.mExtRecord), this.extbyte);
                            }
                        }
                    }
                    this.mPendingExtLoads = 0;
                    if (this.mResult == null) {
                        this.mResult = ar5.userObj;
                    }
                    if (this.mUserResponse != null) {
                    }
                    break;
                case 6:
                    Rlog.d(LOG_TAG, "EVENT_EF_LINEAR_RECORD_SIZE_DONE_EMAIL ");
                    AsyncResult ar6 = (AsyncResult) msg.obj;
                    String email = (String) ar6.userObj;
                    Rlog.d(LOG_TAG, "email: " + email);
                    if (ar6.exception != null) {
                        throw new RuntimeException("get Email EF record size failed", ar6.exception);
                    }
                    int[] recordSize2 = (int[]) ar6.result;
                    Rlog.d(LOG_TAG, " recordSize[0] = " + recordSize2[0]);
                    if (recordSize2.length != 3 || this.mRecordNumber > recordSize2[2]) {
                        throw new RuntimeException("get wrong EF record size format", ar6.exception);
                    }
                    try {
                        byte[] emailString = new byte[recordSize2[0]];
                        for (int i5 = 0; i5 < recordSize2[0]; i5++) {
                            emailString[i5] = -1;
                            break;
                        }
                        if (TextUtils.isEmpty(email)) {
                            Rlog.w(LOG_TAG, "[buildEmailString] Empty email");
                        } else {
                            if (email.length() > recordSize2[0] - 2) {
                                Rlog.w(LOG_TAG, "[buildEmailString] Max length of email is " + (recordSize2[0] - 2));
                                throw new RuntimeException("wrong ADN Email format", ar6.exception);
                            }
                            emailString[recordSize2[0] - 1] = (byte) this.adnIndex;
                            byte[] byteEmail = GsmAlphabet.stringToGsm8BitPacked(email);
                            System.arraycopy(byteEmail, 0, emailString, 0, byteEmail.length);
                        }
                        Rlog.d(LOG_TAG, "---> email efid: " + this.mEf + " recordNumber: " + this.mRecordNumber);
                        this.mFh.updateEFLinearFixed(this.mEf, this.mRecordNumber, emailString, this.mPin2, obtainMessage(5));
                        this.mPendingExtLoads = 1;
                        if (this.mUserResponse != null) {
                        }
                    } catch (Exception e3) {
                        e3.printStackTrace();
                        throw new RuntimeException("Update Email error", ar6.exception);
                    }
                    break;
                case 7:
                    Rlog.d(LOG_TAG, "EVENT_EF_LINEAR_RECORD_SIZE_DONE_ANR ");
                    AsyncResult ar7 = (AsyncResult) msg.obj;
                    AdnRecord adn8 = (AdnRecord) ar7.userObj;
                    this.mResult = adn8;
                    String anr = adn8.anrs[0];
                    Rlog.d(LOG_TAG, "anr: " + anr);
                    if (ar7.exception != null) {
                        Rlog.e(LOG_TAG, " DONE_ANR, ar.exception != null");
                        throw new RuntimeException("get Anr EF record size failed", ar7.exception);
                    }
                    int[] recordSize3 = (int[]) ar7.result;
                    Rlog.d(LOG_TAG, "recordSize[0] = " + recordSize3[0]);
                    if (recordSize3.length != 3 || this.mRecordNumber > recordSize3[2]) {
                        throw new RuntimeException("get wrong EF record size format", ar7.exception);
                    }
                    try {
                        if (anr == null) {
                            Rlog.d(LOG_TAG, "[buildanrBytes] Null anr");
                        } else {
                            if (anr.length() > 40) {
                                Rlog.d(LOG_TAG, "[buildanrBytes] Max length of anr is 40");
                                throw new RuntimeException("wrong ADN Anr format", ar7.exception);
                            }
                            if (anr.length() > 20) {
                                if (this.mAdnCache != null && this.mAdnCache.mAdnExt1Map.isEmpty()) {
                                    throw new RuntimeException("ANR number is over length", ar7.exception);
                                }
                                this.mPendingExtLoads = 2;
                                String temp22 = (String) anr.subSequence(0, 20);
                                this.pendingExtString = (String) anr.subSequence(20, anr.length());
                                Rlog.v(LOG_TAG, " [anr] Split long String to shorts");
                                Rlog.v(LOG_TAG, " EFanr String = " + temp22);
                                Rlog.v(LOG_TAG, " pendingExtString = " + this.pendingExtString);
                                findValidExtIndex(adn8, 2, ar7);
                                adn8.extRecordAnr0 = this.extIndex & 255;
                                byte[] data8 = adn8.buildAnrString(recordSize3[0], temp22, this.mRecordNumber, this.adnIndex, adn8.extRecordAnr0);
                                if (data8 == null) {
                                    throw new RuntimeException("wrong ADN anr format", ar7.exception);
                                }
                                this.mFh.updateEFLinearFixed(this.mEf, this.mRecordNumber, data8, this.mPin2, obtainMessage(16));
                            } else if (adn8.hasExtendedRecordAnr0()) {
                                Rlog.d(LOG_TAG, "<20 adn.extRecordAnr0 = " + adn8.extRecordAnr0);
                                if (this.mAdnCache != null && this.mAdnCache.mAdnExt1Map.isEmpty()) {
                                    throw new RuntimeException("pervious ANR number format is wrong", ar7.exception);
                                }
                                byte[] nullData2 = new byte[13];
                                for (int i6 = 0; i6 < 13; i6++) {
                                    nullData2[i6] = -1;
                                }
                                if (this.mAdnCache != null) {
                                    this.mAdnCache.mAdnExt1Map.put(Integer.valueOf(adn8.extRecordAnr0), nullData2);
                                }
                                this.extIndex = adn8.extRecordAnr0 & 255;
                                adn8.extRecordAnr0 = 255;
                                this.mPendingExtLoads = 2;
                                this.pendingExtString = "";
                                byte[] data9 = adn8.buildAnrString(recordSize3[0], anr, this.mRecordNumber, this.adnIndex);
                                if (data9 == null) {
                                    throw new RuntimeException("wrong ADN format", ar7.exception);
                                }
                                this.mFh.updateEFLinearFixed(this.mEf, this.mRecordNumber, data9, this.mPin2, obtainMessage(16));
                            } else {
                                byte[] data10 = adn8.buildAnrString(recordSize3[0], anr, this.mRecordNumber, this.adnIndex);
                                Rlog.d(LOG_TAG, "---> anr efid: " + this.mEf + "recordNumber: " + this.mRecordNumber);
                                if (data10 == null) {
                                    throw new RuntimeException("wrong ADN format", ar7.exception);
                                }
                                this.mFh.updateEFLinearFixed(this.mEf, this.mRecordNumber, data10, this.mPin2, obtainMessage(5));
                            }
                            if (this.mUserResponse != null) {
                            }
                        }
                        this.mPendingExtLoads = 1;
                        if (this.mUserResponse != null) {
                        }
                    } catch (Exception e4) {
                        e4.printStackTrace();
                        throw new RuntimeException("Update ANR error", ar7.exception);
                    }
                    break;
                case 8:
                    Rlog.d(LOG_TAG, "EVENT_EF_LINEAR_RECORD_SIZE_DONE_GRP ");
                    AsyncResult ar8 = (AsyncResult) msg.obj;
                    byte[] grps = (byte[]) ar8.userObj;
                    Rlog.d(LOG_TAG, "grps: " + grps);
                    if (ar8.exception != null) {
                        Rlog.e(LOG_TAG, " DONE_GRP, ar.exception != null");
                        throw new RuntimeException("get Grp EF record size failed", ar8.exception);
                    }
                    int[] recordSize4 = (int[]) ar8.result;
                    Rlog.d(LOG_TAG, "recordSize[0] = " + recordSize4[0]);
                    if (recordSize4.length != 3 || this.mRecordNumber > recordSize4[2]) {
                        throw new RuntimeException("get wrong EF record size format", ar8.exception);
                    }
                    try {
                        byte[] bcdGrp = new byte[10];
                        byte[] grpsString = new byte[recordSize4[0]];
                        for (int i7 = 0; i7 < recordSize4[0]; i7++) {
                            grpsString[i7] = 0;
                            break;
                        }
                        Rlog.d(LOG_TAG, "---> grps.length " + grps.length);
                        if (grps.length == 0) {
                            Rlog.d(LOG_TAG, "[buildgrpsString] Empty grps");
                        } else {
                            if (grps.length > 10) {
                                Rlog.d(LOG_TAG, "[buildgrpsString] Max length of anr is 10");
                                throw new RuntimeException("wrong ADN Grp format", ar8.exception);
                            }
                            bcdGrp = grps;
                        }
                        Rlog.d(LOG_TAG, "---> grp efid: " + this.mEf + "recordNumber: " + this.mRecordNumber);
                        this.mFh.updateEFLinearFixed(this.mEf, this.mRecordNumber, bcdGrp, this.mPin2, obtainMessage(5));
                        this.mPendingExtLoads = 1;
                        if (this.mUserResponse != null) {
                        }
                    } catch (Exception e5) {
                        e5.printStackTrace();
                        throw new RuntimeException("Update GRP error", ar8.exception);
                    }
                    break;
                case 9:
                    Rlog.d(LOG_TAG, "EVENT_EF_LINEAR_RECORD_SIZE_DONE_SNE ");
                    AsyncResult ar9 = (AsyncResult) msg.obj;
                    String sne = (String) ar9.userObj;
                    Rlog.d(LOG_TAG, "sne: " + sne);
                    if (ar9.exception != null) {
                        throw new RuntimeException("get Sne EF record size failed", ar9.exception);
                    }
                    int[] recordSize5 = (int[]) ar9.result;
                    Rlog.d(LOG_TAG, " recordSize[0] = " + recordSize5[0]);
                    if (recordSize5.length != 3 || this.mRecordNumber > recordSize5[2]) {
                        throw new RuntimeException("get wrong EF record size format", ar9.exception);
                    }
                    try {
                        byte[] sneString = new byte[recordSize5[0]];
                        for (int i8 = 0; i8 < recordSize5[0]; i8++) {
                            sneString[i8] = -1;
                            break;
                        }
                        if (TextUtils.isEmpty(sne)) {
                            Rlog.w(LOG_TAG, "[buildSneString] Empty sne");
                        } else {
                            if (sne.length() > recordSize5[0]) {
                                Rlog.w(LOG_TAG, "[buildEmailString] Max length of sne is " + recordSize5[0]);
                                throw new RuntimeException("wrong ADN Sne format", ar9.exception);
                            }
                            byte[] byteSne = GsmAlphabet.stringToGsm8BitPacked(sne);
                            System.arraycopy(byteSne, 0, sneString, 0, byteSne.length);
                        }
                        Rlog.d(LOG_TAG, "---> sne efid: " + this.mEf + "recordNumber: " + this.mRecordNumber);
                        this.mFh.updateEFLinearFixed(this.mEf, this.mRecordNumber, sneString, this.mPin2, obtainMessage(5));
                        this.mPendingExtLoads = 1;
                        if (this.mUserResponse != null) {
                        }
                    } catch (Exception e6) {
                        e6.printStackTrace();
                        throw new RuntimeException("Update SNE error", ar9.exception);
                    }
                    break;
                case 10:
                    Rlog.d(LOG_TAG, "EVENT_EF_LINEAR_RECORD_SIZE_DONE_GSD ");
                    AsyncResult ar10 = (AsyncResult) msg.obj;
                    String gsdTag = (String) ar10.userObj;
                    Rlog.d(LOG_TAG, "gsdTag: " + gsdTag);
                    if (ar10.exception != null) {
                        Rlog.e(LOG_TAG, " DONE_GSD, ar.exception != null");
                        throw new RuntimeException("get Gsd EF record size failed", ar10.exception);
                    }
                    int[] recordSize6 = (int[]) ar10.result;
                    Rlog.d(LOG_TAG, "recordSize[0] = " + recordSize6[0]);
                    if (recordSize6.length != 3 || this.mRecordNumber > recordSize6[2]) {
                        throw new RuntimeException("get wrong EF record size format", ar10.exception);
                    }
                    try {
                        byte[] gsdString = new byte[recordSize6[0]];
                        for (int i9 = 0; i9 < recordSize6[0]; i9++) {
                            gsdString[i9] = -1;
                            break;
                        }
                        if (TextUtils.isEmpty(gsdTag)) {
                            Rlog.w(LOG_TAG, "[buildGsdString] Empty gsdTag");
                        } else {
                            if (gsdTag.length() > recordSize6[0]) {
                                Rlog.w(LOG_TAG, "[buildGsdString] Max length of gsdTag is " + recordSize6[0]);
                                throw new RuntimeException("wrong ADN Gsd format", ar10.exception);
                            }
                            byte[] byteGsd = GsmAlphabet.stringToGsm8BitPacked(gsdTag);
                            System.arraycopy(byteGsd, 0, gsdString, 0, byteGsd.length);
                        }
                        Rlog.d(LOG_TAG, "---> Gsd efid: " + this.mEf + "recordNumber: " + this.mRecordNumber);
                        this.mFh.updateEFLinearFixed(this.mEf, this.mRecordNumber, gsdString, this.mPin2, obtainMessage(5));
                        this.mPendingExtLoads = 0;
                        if (this.mUserResponse != null) {
                        }
                    } catch (Exception e7) {
                        e7.printStackTrace();
                        throw new RuntimeException("Update GSD error", ar10.exception);
                    }
                    break;
                case 11:
                    AsyncResult ar11 = (AsyncResult) msg.obj;
                    this.mPendingExtLoads = 1;
                    ArrayList<byte[]> datas2 = (ArrayList) ar11.result;
                    if (ar11.exception != null) {
                        Rlog.w(LOG_TAG, "EVENT_EXT1_RECORD_LOAD_ALL_DONE ADN extension EF: 0x" + Integer.toHexString(this.mExtensionEF) + "\nhas exception, no EFext1");
                        if (this.mAdnCache != null) {
                            this.mAdnCache.mAdnExt1Map.clear();
                        }
                    } else {
                        int s2 = datas2.size();
                        for (int i10 = 0; i10 < s2; i10++) {
                            Rlog.d(LOG_TAG, "EVENT_EXT1_RECORD_LOAD_ALL_DONE ADN extension EF: 0x" + Integer.toHexString(this.mExtensionEF) + "\n" + IccUtils.bytesToHexString(datas2.get(i10)));
                            if (this.mAdnCache != null) {
                                this.mAdnCache.mAdnExt1Map.put(Integer.valueOf(i10 + 1), datas2.get(i10));
                            }
                        }
                    }
                    this.mFh.loadEFLinearFixedAll(this.mEf, this.mFirstIndex, this.mUsed, obtainMessage(3));
                    if (this.mUserResponse != null) {
                    }
                    break;
                case 12:
                default:
                    if (this.mUserResponse != null) {
                    }
                    break;
                case 13:
                    AsyncResult ar12 = (AsyncResult) msg.obj;
                    this.mPendingExtLoads = 1;
                    ArrayList<byte[]> datas3 = (ArrayList) ar12.result;
                    if (ar12.exception != null) {
                        Rlog.w(LOG_TAG, "EVENT_EXT2_RECORD_LOAD_ALL_DONE FDN extension EF: 0x" + Integer.toHexString(this.mExtensionEF) + "\nhas exception, no EFext2");
                        if (this.mAdnCache != null) {
                            this.mAdnCache.mAdnExt2Map.clear();
                        }
                    } else {
                        int s3 = datas3.size();
                        for (int i11 = 0; i11 < s3; i11++) {
                            Rlog.d(LOG_TAG, "EVENT_EXT2_RECORD_LOAD_ALL_DONE FDN extension EF: 0x" + Integer.toHexString(this.mExtensionEF) + "\n" + IccUtils.bytesToHexString(datas3.get(i11)));
                            if (this.mAdnCache != null) {
                                this.mAdnCache.mAdnExt2Map.put(Integer.valueOf(i11 + 1), datas3.get(i11));
                            }
                        }
                    }
                    this.mFh.loadEFLinearFixedAll(this.mEf, this.mFirstIndex, this.mUsed, obtainMessage(3));
                    if (this.mUserResponse != null) {
                    }
                    break;
                case 14:
                    Rlog.d(LOG_TAG, "EVENT_UPDATE_EXT2_RECORD_DONE ");
                    AsyncResult ar13 = (AsyncResult) msg.obj;
                    if (ar13.exception != null) {
                        throw new RuntimeException("update EF ext2 record failed", ar13.exception);
                    }
                    this.mPendingExtLoads = 1;
                    this.pendingExtString = "";
                    this.mFh.updateEFLinearFixed(this.mEf, this.mRecordNumber, this.mData, this.mPin2, obtainMessage(15));
                    if (this.mUserResponse != null) {
                    }
                    break;
                case 15:
                    AsyncResult ar14 = (AsyncResult) msg.obj;
                    if (ar14.exception != null) {
                        throw new RuntimeException("update EF adn record failed", ar14.exception);
                    }
                    Rlog.d(LOG_TAG, "EVENT_UPDATE_ADN_RECORD_DONE 1 ");
                    if (this.mPendingExtLoads == 2) {
                        Rlog.d(LOG_TAG, "EVENT_UPDATE_ADN_RECORD_DONE 2 ");
                        try {
                            this.extbyte = new byte[13];
                            for (int i12 = 0; i12 < 13; i12++) {
                                this.extbyte[i12] = -1;
                            }
                            byte[] bcdNumber2 = PhoneNumberUtils.numberToCalledPartyBCD(this.pendingExtString);
                            if (bcdNumber2 != null) {
                                System.arraycopy(bcdNumber2, 1, this.extbyte, 2, bcdNumber2.length - 1);
                                this.extbyte[0] = 2;
                                this.extbyte[1] = (byte) ((bcdNumber2.length - 1) & 255);
                            }
                            AdnRecord adn9 = (AdnRecord) this.mResult;
                            this.mFh.updateEFLinearFixed(this.mExtensionEF, this.extIndex, this.extbyte, this.mPin2, obtainMessage(5));
                            this.accessPort = 15;
                            this.mResult = adn9;
                            this.mPendingExtLoads = 1;
                            this.pendingExtString = "";
                        } catch (Exception e8) {
                            e8.printStackTrace();
                            throw new RuntimeException("update EF adn ext1 record failed, full ext1", ar14.exception);
                        }
                    } else {
                        obtainMessage(5, msg.obj).sendToTarget();
                    }
                    if (this.mUserResponse != null) {
                    }
                    break;
                    break;
                case 16:
                    AsyncResult ar15 = (AsyncResult) msg.obj;
                    if (ar15.exception != null) {
                        throw new RuntimeException("update EF adn record failed", ar15.exception);
                    }
                    Rlog.v(LOG_TAG, "EVENT_UPDATE_ANR_RECORD_DONE 1 ");
                    if (this.mPendingExtLoads == 2) {
                        Rlog.v(LOG_TAG, "EVENT_UPDATE_ANR_RECORD_DONE 2 ");
                        try {
                            this.extbyte = new byte[13];
                            for (int i13 = 0; i13 < 13; i13++) {
                                this.extbyte[i13] = -1;
                            }
                            byte[] bcdNumber3 = PhoneNumberUtils.numberToCalledPartyBCD(this.pendingExtString);
                            if (bcdNumber3 != null) {
                                System.arraycopy(bcdNumber3, 1, this.extbyte, 2, bcdNumber3.length - 1);
                                this.extbyte[0] = 2;
                                this.extbyte[1] = (byte) ((bcdNumber3.length - 1) & 255);
                            }
                            AdnRecord adn10 = (AdnRecord) this.mResult;
                            this.mFh.updateEFLinearFixed(this.mExtensionEF, this.extIndex, this.extbyte, this.mPin2, obtainMessage(5));
                            this.accessPort = 16;
                            this.mResult = adn10;
                            this.mPendingExtLoads = 1;
                            this.pendingExtString = "";
                        } catch (Exception e9) {
                            e9.printStackTrace();
                            throw new RuntimeException("update EF adn ext1 record failed, full ext1", ar15.exception);
                        }
                    } else {
                        obtainMessage(5, msg.obj).sendToTarget();
                    }
                    if (this.mUserResponse != null) {
                    }
                    break;
                    break;
            }
        } catch (RuntimeException exc) {
            if (this.mUserResponse != null) {
                AsyncResult.forMessage(this.mUserResponse).exception = exc;
                this.mUserResponse.sendToTarget();
                this.mUserResponse = null;
            }
        }
    }

    private void findValidExtIndex(AdnRecord adn, int mode, AsyncResult ar) throws RuntimeException {
        boolean isExist;
        if (this.mAdnCache == null) {
            throw new RuntimeException("update EF adn/fdn ext record failed, no ADN cache", ar.exception);
        }
        this.extIndex = 0;
        if ((this.mExtensionEF == 28490 || this.mExtensionEF == this.mAdnCache.mUSIMExt1) && this.mAdnCache.mAdnExt1Map.isEmpty()) {
            this.mResult = null;
            throw new RuntimeException("findValidExtIndex failed, no ext1 EF", ar.exception);
        }
        if (this.mExtensionEF == 28491 && this.mAdnCache.mAdnExt2Map.isEmpty()) {
            this.mResult = null;
            throw new RuntimeException("findValidExtIndex failed, no ext2 EF", ar.exception);
        }
        if (mode == 1 || mode == 3) {
            isExist = adn.hasExtendedRecord();
        } else {
            isExist = adn.hasExtendedRecordAnr0();
        }
        if (isExist) {
            Rlog.v(LOG_TAG, "findValidExtIndex 1 ");
            if (mode == 1 || mode == 3) {
                this.extIndex = adn.mExtRecord;
                return;
            } else {
                this.extIndex = adn.extRecordAnr0;
                return;
            }
        }
        Rlog.v(LOG_TAG, "findValidExtIndex 2 ");
        if (mode == 1 || mode == 2) {
            Iterator<Integer> it = this.mAdnCache.mAdnExt1Map.keySet().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                int i = it.next().intValue();
                if (this.mAdnCache.mAdnExt1Map.containsKey(Integer.valueOf(i)) && byteArrayEqualsEmpty(this.mAdnCache.mAdnExt1Map.get(Integer.valueOf(i)))) {
                    this.extIndex = i;
                    break;
                }
            }
        } else if (mode == 3) {
            Iterator<Integer> it2 = this.mAdnCache.mAdnExt2Map.keySet().iterator();
            while (true) {
                if (!it2.hasNext()) {
                    break;
                }
                int i2 = it2.next().intValue();
                if (this.mAdnCache.mAdnExt2Map.containsKey(Integer.valueOf(i2)) && byteArrayEqualsEmpty(this.mAdnCache.mAdnExt2Map.get(Integer.valueOf(i2)))) {
                    this.extIndex = i2;
                    break;
                }
            }
        }
        Rlog.d(LOG_TAG, "findValidExtIndex extIndex = " + this.extIndex);
        if (this.extIndex == 0 || (((mode == 1 || mode == 2) && this.extIndex > this.mAdnCache.mAdnExt1Map.size()) || (mode == 3 && this.extIndex > this.mAdnCache.mAdnExt2Map.size()))) {
            this.mResult = null;
            throw new RuntimeException("update EF adn/fdn ext record failed, full ext", ar.exception);
        }
    }

    private boolean byteArrayEqualsEmpty(byte[] bytes) {
        if (bytes.length != 13) {
            return false;
        }
        if (bytes[0] != 0 && bytes[0] != -1) {
            return false;
        }
        for (int i = 1; i < bytes.length; i++) {
            if (bytes[i] != -1) {
                return false;
            }
        }
        return true;
    }
}
