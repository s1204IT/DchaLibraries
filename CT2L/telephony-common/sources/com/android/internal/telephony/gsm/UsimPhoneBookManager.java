package com.android.internal.telephony.gsm;

import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.AdnRecordCache;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class UsimPhoneBookManager extends Handler implements IccConstants {
    private static final boolean DBG = true;
    private static final boolean DBGV = false;
    private static final int EVENT_ANR_LOAD_DONE = 6;
    private static final int EVENT_EF_LINEAR_RECORD_SIZE_DONE = 14;
    private static final int EVENT_EMAIL_LOAD_DONE = 4;
    private static final int EVENT_IAP_LOAD_DONE = 3;
    private static final int EVENT_IAP_UPDATE_DONE = 5;
    private static final int EVENT_PBR_LOAD_DONE = 1;
    private static final int EVENT_SINGLE_ANR_LOAD_DONE = 12;
    private static final int EVENT_SINGLE_EMAIL_LOAD_DONE = 11;
    private static final int EVENT_SINGLE_SNE_LOAD_DONE = 13;
    private static final int EVENT_SNE_LOAD_DONE = 7;
    private static final int EVENT_USIM_ADN_LOAD_DONE = 2;
    private static final int EVENT_USIM_GRP_LOAD_DONE = 8;
    private static final int EVENT_USIM_GSD_LOAD_DONE = 9;
    private static final String LOG_TAG = "UsimPhoneBookManager";
    private static final int MAX_GROUP_SIZE_BYTES = 10;
    private static final int MAX_NUMBER_SIZE_BYTES = 11;
    public static final char PAUSE = ',';
    public static final String PBR_RECORD_INDEX = "pbrRecordIndex";
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
    public static final int USIM_TYPE1_TAG = 168;
    public static final int USIM_TYPE2_TAG = 169;
    public static final int USIM_TYPE3_TAG = 170;
    public static final char WILD = 'N';
    private AdnRecordCache mAdnCache;
    private ArrayList<byte[]> mEmailFileRecord;
    private Map<Integer, ArrayList<String>> mEmailsForAdnRec;
    private IccFileHandler mFh;
    private ArrayList<byte[]> mIapFileRecord;
    private int[] mRecordSize;
    private Object mLock = new Object();
    private boolean mEmailPresentInIap = false;
    private int mEmailTagNumberInIap = 0;
    private Map<Integer, ArrayList<byte[]>> mIapFileRecords = new HashMap();
    private Map<Integer, Integer> mAdnEfRecords = new HashMap();
    private Map<Integer, Map<Integer, ArrayList<String>>> mEmailFileType1Records = new HashMap();
    private Map<Integer, Map<Integer, ArrayList<String>>> mEmailFileType2Records = new HashMap();
    private Map<Integer, Map<Integer, ArrayList<String>>> mAnrFileType1Records = new HashMap();
    private Map<Integer, Map<Integer, ArrayList<String>>> mAnrFileType2Records = new HashMap();
    private Map<Integer, Map<Integer, ArrayList<String>>> mSneFileType1Records = new HashMap();
    private Map<Integer, Map<Integer, ArrayList<String>>> mSneFileType2Records = new HashMap();
    private Map<Integer, Map<Integer, ArrayList<byte[]>>> mGrpFileRecords = new HashMap();
    private Map<Integer, Map<Integer, ArrayList<String>>> mGsdFileRecords = new HashMap();
    private Map<Integer, String> mUsimGroupsMap = new HashMap();
    private Map<Integer, Map<Integer, ArrayList<Integer>>> mAnrFileType1RecordsExt = new HashMap();
    private Map<Integer, Map<Integer, ArrayList<Integer>>> mAnrFileType2RecordsExt = new HashMap();
    private Map<Integer, ArrayList<Integer>> mValidADNIndexes = new HashMap();
    private Map<Integer, String> mEmailsHash = new HashMap();
    private Map<Integer, String> mAnrsHash = new HashMap();
    private Map<Integer, Integer> mAnrs0ExtHash = new HashMap();
    private Map<Integer, String> mSnesHash = new HashMap();
    private boolean mRefreshCache = false;
    private boolean mUpdateIapSuccess = false;
    private boolean isSneEnable = false;
    public int extEf = 0;
    private ArrayList<AdnRecord> mPhoneBookRecords = new ArrayList<>();
    private PbrFile mPbrFile = null;
    private Boolean mIsPbrPresent = true;

    public UsimPhoneBookManager(IccFileHandler fh, AdnRecordCache cache) {
        this.mFh = fh;
        this.mAdnCache = cache;
    }

    public void reset() {
        this.mPhoneBookRecords.clear();
        this.mIapFileRecord = null;
        this.mEmailFileRecord = null;
        this.mEmailFileType1Records.clear();
        this.mEmailFileType2Records.clear();
        this.mAnrFileType1Records.clear();
        this.mAnrFileType2Records.clear();
        this.mSneFileType1Records.clear();
        this.mSneFileType2Records.clear();
        this.mGrpFileRecords.clear();
        this.mGsdFileRecords.clear();
        this.mUsimGroupsMap.clear();
        this.mAdnEfRecords.clear();
        this.mAnrFileType1RecordsExt.clear();
        this.mAnrFileType2RecordsExt.clear();
        this.extEf = 0;
        this.mPbrFile = null;
        this.mIsPbrPresent = true;
        this.mRefreshCache = false;
    }

    public boolean isSimRecordsEmpty() {
        return this.mPhoneBookRecords.isEmpty() || this.mPbrFile == null || !this.mIsPbrPresent.booleanValue();
    }

    public ArrayList<String> getEmailFileRecords(int usimTag, int recNum, int efid) {
        if (usimTag == 169) {
            if (this.mEmailFileType2Records.get(Integer.valueOf(recNum)) != null) {
                return this.mEmailFileType2Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(efid));
            }
            return null;
        }
        if (usimTag != 168 || this.mEmailFileType1Records.get(Integer.valueOf(recNum)) == null) {
            return null;
        }
        return this.mEmailFileType1Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(efid));
    }

    public int getPbrFileSize() {
        return this.mPbrFile.mFileIds.size();
    }

    public int getGsdEf(int grpId) {
        if (this.mGsdFileRecords.isEmpty()) {
            return -1;
        }
        Map<Integer, ArrayList<String>> gsdEfMap = this.mGsdFileRecords.get(0);
        return gsdEfMap.keySet().iterator().next().intValue();
    }

    public Set<Integer> getEmailType2Files(int recNum) {
        if (this.mEmailFileType2Records.get(Integer.valueOf(recNum)) != null) {
            return this.mEmailFileType2Records.get(Integer.valueOf(recNum)).keySet();
        }
        return null;
    }

    public boolean isSneFieldEnable() {
        Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds;
        if (this.mPbrFile == null || this.mPbrFile.mFileIds == null || this.mPbrFile == null || (fileIds = this.mPbrFile.mFileIds.get(0)) == null || fileIds.isEmpty()) {
            return false;
        }
        boolean hasRecordsInA9 = fileIds.containsKey(169) && fileIds.get(169).containsKey(195);
        boolean hasRecordsInA8 = fileIds.containsKey(168) && fileIds.get(168).containsKey(195);
        this.isSneEnable = hasRecordsInA9 || hasRecordsInA8;
        return this.isSneEnable;
    }

    public int getEmailNumInOneRecord(int recNum) {
        int type2EmailNumInOneRecord;
        int type1EmailNumInOneRecord;
        if (this.mEmailFileType2Records.get(Integer.valueOf(recNum)) == null) {
            type2EmailNumInOneRecord = 0;
        } else {
            type2EmailNumInOneRecord = this.mEmailFileType2Records.get(Integer.valueOf(recNum)).size();
        }
        if (this.mEmailFileType1Records.get(Integer.valueOf(recNum)) == null) {
            type1EmailNumInOneRecord = 0;
        } else {
            type1EmailNumInOneRecord = this.mEmailFileType1Records.get(Integer.valueOf(recNum)).size();
        }
        if (type2EmailNumInOneRecord == 0 && type1EmailNumInOneRecord == 0) {
            return 0;
        }
        return type2EmailNumInOneRecord + type1EmailNumInOneRecord;
    }

    public ArrayList<String> getAnrFileRecords(int usimTag, int recNum, int efid) {
        if (usimTag == 169) {
            if (this.mAnrFileType2Records.get(Integer.valueOf(recNum)) != null) {
                return this.mAnrFileType2Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(efid));
            }
            return null;
        }
        if (usimTag != 168 || this.mAnrFileType1Records.get(Integer.valueOf(recNum)) == null) {
            return null;
        }
        return this.mAnrFileType1Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(efid));
    }

    public ArrayList<String> getSneFileRecords(int usimTag, int recNum, int efid) {
        if (usimTag == 169) {
            if (this.mSneFileType2Records.get(Integer.valueOf(recNum)) != null) {
                return this.mSneFileType2Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(efid));
            }
            return null;
        }
        if (usimTag != 168 || this.mSneFileType1Records.get(Integer.valueOf(recNum)) == null) {
            return null;
        }
        return this.mSneFileType1Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(efid));
    }

    public Map<Integer, String> getUsimGroups() {
        synchronized (this.mLock) {
            if (!this.mIsPbrPresent.booleanValue()) {
                return null;
            }
            if (this.mPbrFile == null) {
                readPbrFileAndWait();
            }
            if (this.mPbrFile == null) {
                return null;
            }
            int numRecs = this.mPbrFile.mFileIds.size();
            for (int i = 0; i < numRecs; i++) {
                readGsdFilesAndWait(i);
                if (this.mPbrFile == null || this.mPbrFile.mFileIds == null) {
                    reset();
                    return null;
                }
            }
            if (this.mGsdFileRecords.isEmpty()) {
                return null;
            }
            return this.mUsimGroupsMap;
        }
    }

    public ArrayList<AdnRecord> getUsimPhoneBook() {
        return this.mPhoneBookRecords;
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
            if (this.mPbrFile == null) {
                readPbrFileAndWait();
            }
            if (this.mPbrFile == null) {
                return null;
            }
            int numRecs = this.mPbrFile.mFileIds.size();
            for (int i = 0; i < numRecs; i++) {
                readAdnFileAndWait(i);
                if (this.mPbrFile == null || this.mPbrFile.mFileIds == null) {
                    reset();
                    return null;
                }
                readEmailAnrSneFilesAndWait(i);
                if (this.mPbrFile == null || this.mPbrFile.mFileIds == null) {
                    reset();
                    return null;
                }
                readGroupFilesAndWait(i);
                if (this.mPbrFile == null || this.mPbrFile.mFileIds == null) {
                    reset();
                    return null;
                }
            }
            return this.mPhoneBookRecords;
        }
    }

    private void readGroupFilesAndWait(int recNum) {
        readGrpFilesAndWait(recNum);
        readGsdFilesAndWait(recNum);
        updatePhoneAdnRecordGrp(recNum);
    }

    private void readGsdFilesAndWait(int recNum) {
        Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds;
        if (this.mPbrFile != null && this.mPbrFile.mFileIds != null && (fileIds = this.mPbrFile.mFileIds.get(Integer.valueOf(recNum))) != null && !fileIds.isEmpty()) {
            boolean hasGroupFiles = fileIds.containsKey(168) && fileIds.get(168).containsKey(Integer.valueOf(USIM_EFGRP_TAG)) && fileIds.get(170).containsKey(200);
            if (hasGroupFiles) {
                Map<Integer, Integer> efRecords = (HashMap) fileIds.get(170).get(200);
                Iterator<Integer> it = efRecords.keySet().iterator();
                while (it.hasNext()) {
                    int key = it.next().intValue();
                    Message response = obtainMessage(9, recNum, efRecords.get(Integer.valueOf(key)).intValue(), null);
                    Bundle bundle = new Bundle();
                    bundle.putInt(PBR_RECORD_INDEX, recNum);
                    response.setData(bundle);
                    this.mFh.loadEFLinearFixedAll(efRecords.get(Integer.valueOf(key)).intValue(), response);
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                        Rlog.e(LOG_TAG, "Interrupted Exception in readGsdFilesAndWait");
                    }
                }
            }
        }
    }

    private void readGrpFilesAndWait(int recNum) {
        Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds;
        if (this.mPbrFile != null && this.mPbrFile.mFileIds != null && (fileIds = this.mPbrFile.mFileIds.get(Integer.valueOf(recNum))) != null && !fileIds.isEmpty()) {
            boolean hasGroupFiles = fileIds.containsKey(168) && fileIds.get(168).containsKey(Integer.valueOf(USIM_EFGRP_TAG));
            if (hasGroupFiles) {
                Map<Integer, Integer> efRecords = (HashMap) fileIds.get(168).get(Integer.valueOf(USIM_EFGRP_TAG));
                Iterator<Integer> it = efRecords.keySet().iterator();
                while (it.hasNext()) {
                    int key = it.next().intValue();
                    Message response = obtainMessage(8, recNum, efRecords.get(Integer.valueOf(key)).intValue(), null);
                    Bundle bundle = new Bundle();
                    bundle.putInt(PBR_RECORD_INDEX, recNum);
                    response.setData(bundle);
                    this.mFh.loadEFLinearFixedAll(efRecords.get(Integer.valueOf(key)).intValue(), response);
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                        Rlog.e(LOG_TAG, "Interrupted Exception in readGroupFilesAndWait");
                    }
                }
            }
        }
    }

    private void refreshCache() {
        if (this.mPbrFile != null) {
            this.mPhoneBookRecords.clear();
            int numRecs = this.mPbrFile.mFileIds.size();
            for (int i = 0; i < numRecs; i++) {
                readAdnFileAndWait(i);
            }
        }
    }

    public void invalidateCache() {
        this.mRefreshCache = true;
    }

    private void readPbrFileAndWait() {
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_PBR, obtainMessage(1));
        try {
            this.mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readAdnFileAndWait");
        }
    }

    private void readEmailAnrSneFilesAndWait(int recNum) {
        Rlog.d(LOG_TAG, "begin readEmailAnrSneFilesAndWait() recNum = " + recNum);
        readEmailFileAndWait(recNum);
        readAnrFileAndWait(recNum);
        readSneFileAndWait(recNum);
        updatePhoneAdnRecordEmail(recNum);
        updatePhoneAdnRecordAnr(recNum);
        updatePhoneAdnRecordSne(recNum);
    }

    private void readSneFileAndWait(int recNum) {
        Rlog.d(LOG_TAG, "begin readSneFileAndWait()");
        actuallyReadEmailOrAnrOrSneFile(recNum, 195);
    }

    private void updatePhoneAdnRecordSne(int recNum) {
        int type2SneNumInOneRecord;
        int type1SneNumInOneRecord;
        Rlog.d(LOG_TAG, "begin updatePhoneAdnRecordSne()");
        if (this.mPbrFile != null && this.mPbrFile.mFileIds != null) {
            Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds = this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
            if (this.mSneFileType2Records.get(Integer.valueOf(recNum)) == null) {
                type2SneNumInOneRecord = 0;
            } else {
                type2SneNumInOneRecord = this.mSneFileType2Records.get(Integer.valueOf(recNum)).size();
            }
            if (this.mSneFileType1Records.get(Integer.valueOf(recNum)) == null) {
                type1SneNumInOneRecord = 0;
            } else {
                type1SneNumInOneRecord = this.mSneFileType1Records.get(Integer.valueOf(recNum)).size();
            }
            if (type2SneNumInOneRecord != 0 || type1SneNumInOneRecord != 0) {
                int sneNumInOneRecord = type2SneNumInOneRecord + type1SneNumInOneRecord;
                Rlog.d(LOG_TAG, "fileIds.get(USIM_TYPE1_TAG).get(USIM_EFADN_TAG).get(0) = " + fileIds.get(168).get(192).get(0));
                Rlog.d(LOG_TAG, "mAdnEfRecords = " + this.mAdnEfRecords);
                int numAdnRecs = this.mAdnEfRecords.get(fileIds.get(168).get(192).get(0)).intValue();
                for (int i = 0; i < numAdnRecs; i++) {
                    Rlog.d(LOG_TAG, "readSneFileAndWait() i = " + i);
                    String[] snes = new String[sneNumInOneRecord];
                    int[] sneEfids = new int[sneNumInOneRecord];
                    int[] sneFileTypes = new int[sneNumInOneRecord];
                    int[] sneIndexes = new int[sneNumInOneRecord];
                    int j = 0;
                    if (type2SneNumInOneRecord > 0 && this.mIapFileRecords.get(Integer.valueOf(recNum)) != null) {
                        Rlog.d(LOG_TAG, " type2SneNumInOneRecord  ");
                        try {
                            byte[] record = this.mIapFileRecords.get(Integer.valueOf(recNum)).get(i);
                            byte[] record2 = record;
                            Iterator<Integer> it = this.mSneFileType2Records.get(Integer.valueOf(recNum)).keySet().iterator();
                            while (it.hasNext()) {
                                int key = it.next().intValue();
                                int sneOrder = this.mPbrFile.mFileType2Order.get(Integer.valueOf(recNum)).get(195).get(Integer.valueOf(key)).intValue();
                                int num = record2[sneOrder];
                                ArrayList<String> sneFile = this.mSneFileType2Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(key));
                                Rlog.d(LOG_TAG, "sne, num: " + num);
                                if (num > 0 && num <= sneFile.size()) {
                                    snes[j] = sneFile.get(num - 1);
                                    Rlog.d(LOG_TAG, "email " + snes[j]);
                                    sneIndexes[j] = num;
                                } else {
                                    snes[j] = "";
                                    sneIndexes[j] = -1;
                                }
                                sneEfids[j] = key;
                                sneFileTypes[j] = 169;
                                j++;
                            }
                        } catch (IndexOutOfBoundsException e) {
                            Rlog.e(LOG_TAG, "Error: Improper ICC card: No IAP record for ADN, continuing");
                            return;
                        }
                    }
                    if (type1SneNumInOneRecord > 0) {
                        Rlog.d(LOG_TAG, " type1SneNumInOneRecord  ");
                        Iterator<Integer> it2 = this.mSneFileType1Records.get(Integer.valueOf(recNum)).keySet().iterator();
                        while (it2.hasNext()) {
                            int key2 = it2.next().intValue();
                            ArrayList<String> sneFile2 = this.mSneFileType1Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(key2));
                            String sne = null;
                            if (i < sneFile2.size()) {
                                String sne2 = sneFile2.get(i);
                                sne = sne2;
                            }
                            if (sne == null || sne.equals("")) {
                                snes[j] = "";
                                sneIndexes[j] = -1;
                            } else {
                                snes[j] = sne;
                                sneIndexes[j] = i + 1;
                            }
                            sneEfids[j] = key2;
                            sneFileTypes[j] = 168;
                            j++;
                        }
                    }
                    int offset = 0;
                    for (int m = 0; recNum > 0 && m < recNum; m++) {
                        offset += this.mAdnEfRecords.get(this.mPbrFile.mFileIds.get(Integer.valueOf(m)).get(168).get(192).get(0)).intValue();
                    }
                    AdnRecord rec = this.mPhoneBookRecords.get(i + offset);
                    if (rec != null) {
                        rec.setSnes(snes);
                    } else {
                        rec = new AdnRecord("", "", snes);
                    }
                    rec.setSneEfids(sneEfids);
                    rec.setSneFileTypes(sneFileTypes);
                    rec.setSneIndexes(sneIndexes);
                    this.mPhoneBookRecords.set(i + offset, rec);
                }
            }
        }
    }

    public int getRecordOffset(int recNum) {
        this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
        int offset = 0;
        for (int m = 0; recNum > 0 && m < recNum; m++) {
            Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds = this.mPbrFile.mFileIds.get(Integer.valueOf(m));
            offset += this.mAdnEfRecords.get(fileIds.get(168).get(192).get(0)).intValue();
        }
        return offset;
    }

    public int getpbrRecordNum(int count) {
        int i = 1;
        while (count - getRecordOffset(i) >= 0) {
            i++;
        }
        return i - 1;
    }

    private void readEmailFileAndWait(int recNum) {
        Rlog.d(LOG_TAG, "begin readEmailFileAndWait()");
        actuallyReadEmailOrAnrOrSneFile(recNum, USIM_EFEMAIL_TAG);
    }

    private void readAnrFileAndWait(int recNum) {
        Rlog.d(LOG_TAG, "begin readAnrFileAndWait()");
        actuallyReadEmailOrAnrOrSneFile(recNum, 196);
    }

    private void updatePhoneAdnRecordEmail(int recNum) {
        int type2EmailNumInOneRecord;
        int type1EmailNumInOneRecord;
        Rlog.d(LOG_TAG, "begin updatePhoneAdnRecordEmail()");
        if (this.mPbrFile != null && this.mPbrFile.mFileIds != null) {
            Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds = this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
            if (this.mEmailFileType2Records.get(Integer.valueOf(recNum)) == null) {
                type2EmailNumInOneRecord = 0;
            } else {
                type2EmailNumInOneRecord = this.mEmailFileType2Records.get(Integer.valueOf(recNum)).size();
            }
            if (this.mEmailFileType1Records.get(Integer.valueOf(recNum)) == null) {
                type1EmailNumInOneRecord = 0;
            } else {
                type1EmailNumInOneRecord = this.mEmailFileType1Records.get(Integer.valueOf(recNum)).size();
            }
            if (type2EmailNumInOneRecord != 0 || type1EmailNumInOneRecord != 0) {
                int emailNumInOneRecord = type2EmailNumInOneRecord + type1EmailNumInOneRecord;
                Rlog.d(LOG_TAG, "fileIds.get(USIM_TYPE1_TAG).get(USIM_EFADN_TAG).get(0) = " + fileIds.get(168).get(192).get(0));
                Rlog.d(LOG_TAG, "mAdnEfRecords = " + this.mAdnEfRecords);
                int numAdnRecs = this.mAdnEfRecords.get(fileIds.get(168).get(192).get(0)).intValue();
                for (int i = 0; i < numAdnRecs; i++) {
                    Rlog.d(LOG_TAG, "readEmailFileAndWait() i = " + i);
                    String[] emails = new String[emailNumInOneRecord];
                    int[] emailEfids = new int[emailNumInOneRecord];
                    int[] emailFileTypes = new int[emailNumInOneRecord];
                    int[] emailIndexes = new int[emailNumInOneRecord];
                    int j = 0;
                    if (type2EmailNumInOneRecord > 0 && this.mIapFileRecords.get(Integer.valueOf(recNum)) != null) {
                        Rlog.d(LOG_TAG, " type2EmailNumInOneRecord  ");
                        try {
                            byte[] record = this.mIapFileRecords.get(Integer.valueOf(recNum)).get(i);
                            byte[] record2 = record;
                            Iterator<Integer> it = this.mEmailFileType2Records.get(Integer.valueOf(recNum)).keySet().iterator();
                            while (it.hasNext()) {
                                int key = it.next().intValue();
                                int emailOrder = this.mPbrFile.mFileType2Order.get(Integer.valueOf(recNum)).get(Integer.valueOf(USIM_EFEMAIL_TAG)).get(Integer.valueOf(key)).intValue();
                                int num = record2[emailOrder];
                                ArrayList<String> emailFile = this.mEmailFileType2Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(key));
                                Rlog.d(LOG_TAG, "email, num: " + num);
                                if (num > 0 && num <= emailFile.size()) {
                                    emails[j] = emailFile.get(num - 1);
                                    Rlog.d(LOG_TAG, "email " + emails[j]);
                                    emailIndexes[j] = num;
                                } else {
                                    emails[j] = "";
                                    emailIndexes[j] = -1;
                                }
                                emailEfids[j] = key;
                                emailFileTypes[j] = 169;
                                j++;
                            }
                        } catch (IndexOutOfBoundsException e) {
                            Rlog.e(LOG_TAG, "Error: Improper ICC card: No IAP record for ADN, continuing");
                            return;
                        }
                    }
                    if (type1EmailNumInOneRecord > 0) {
                        Rlog.d(LOG_TAG, " type1EmailNumInOneRecord  ");
                        Iterator<Integer> it2 = this.mEmailFileType1Records.get(Integer.valueOf(recNum)).keySet().iterator();
                        while (it2.hasNext()) {
                            int key2 = it2.next().intValue();
                            ArrayList<String> emailFile2 = this.mEmailFileType1Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(key2));
                            String email = null;
                            if (i < emailFile2.size()) {
                                String email2 = emailFile2.get(i);
                                email = email2;
                            }
                            if (email == null || email.equals("")) {
                                emails[j] = "";
                                emailIndexes[j] = -1;
                            } else {
                                emails[j] = email;
                                emailIndexes[j] = i + 1;
                            }
                            emailEfids[j] = key2;
                            emailFileTypes[j] = 168;
                            j++;
                        }
                    }
                    int offset = 0;
                    for (int m = 0; recNum > 0 && m < recNum; m++) {
                        offset += this.mAdnEfRecords.get(this.mPbrFile.mFileIds.get(Integer.valueOf(m)).get(168).get(192).get(0)).intValue();
                    }
                    AdnRecord rec = this.mPhoneBookRecords.get(i + offset);
                    if (rec != null) {
                        rec.setEmails(emails);
                    } else {
                        rec = new AdnRecord("", "", emails);
                    }
                    rec.setEmailEfids(emailEfids);
                    rec.setEmailFileTypes(emailFileTypes);
                    rec.setEmailIndexes(emailIndexes);
                    this.mPhoneBookRecords.set(i + offset, rec);
                }
            }
        }
    }

    private void updatePhoneAdnRecordAnr(int recNum) {
        int type2AnrNumInOneRecord;
        int type1AnrNumInOneRecord;
        if (this.mPbrFile != null && this.mPbrFile.mFileIds != null) {
            Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds = this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
            if (this.mAnrFileType2Records.get(Integer.valueOf(recNum)) == null) {
                type2AnrNumInOneRecord = 0;
            } else {
                type2AnrNumInOneRecord = this.mAnrFileType2Records.get(Integer.valueOf(recNum)).size();
            }
            if (this.mAnrFileType1Records.get(Integer.valueOf(recNum)) == null) {
                type1AnrNumInOneRecord = 0;
            } else {
                type1AnrNumInOneRecord = this.mAnrFileType1Records.get(Integer.valueOf(recNum)).size();
            }
            if (type2AnrNumInOneRecord != 0 || type1AnrNumInOneRecord != 0) {
                int anrNumInOneRecord = type2AnrNumInOneRecord + type1AnrNumInOneRecord;
                int numAdnRecs = this.mAdnEfRecords.get(fileIds.get(168).get(192).get(0)).intValue();
                for (int i = 0; i < numAdnRecs; i++) {
                    String[] anrs = new String[anrNumInOneRecord];
                    int[] anrsExt = new int[anrNumInOneRecord];
                    int[] anrEfids = new int[anrNumInOneRecord];
                    int[] anrFileTypes = new int[anrNumInOneRecord];
                    int[] anrIndexes = new int[anrNumInOneRecord];
                    int j = 0;
                    if (type2AnrNumInOneRecord > 0 && this.mIapFileRecords.get(Integer.valueOf(recNum)) != null) {
                        try {
                            byte[] record = this.mIapFileRecords.get(Integer.valueOf(recNum)).get(i);
                            byte[] record2 = record;
                            Iterator<Integer> it = this.mAnrFileType2Records.get(Integer.valueOf(recNum)).keySet().iterator();
                            while (it.hasNext()) {
                                int key = it.next().intValue();
                                int anrOrder = this.mPbrFile.mFileType2Order.get(Integer.valueOf(recNum)).get(196).get(Integer.valueOf(key)).intValue();
                                int num = record2[anrOrder];
                                ArrayList<String> anrFile = this.mAnrFileType2Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(key));
                                ArrayList<Integer> anrExtFile = this.mAnrFileType2RecordsExt.get(Integer.valueOf(recNum)).get(Integer.valueOf(key));
                                if (num > 0 && num <= anrFile.size()) {
                                    anrs[j] = anrFile.get(num - 1);
                                    anrsExt[j] = anrExtFile.get(num - 1).intValue();
                                    anrIndexes[j] = num;
                                } else {
                                    anrs[j] = "";
                                    anrsExt[j] = 255;
                                    anrIndexes[j] = -1;
                                }
                                anrEfids[j] = key;
                                anrFileTypes[j] = 169;
                                j++;
                            }
                        } catch (IndexOutOfBoundsException e) {
                            Rlog.e(LOG_TAG, "Error: Improper ICC card: No IAP record for ADN, continuing");
                            return;
                        }
                    }
                    if (type1AnrNumInOneRecord > 0) {
                        Iterator<Integer> it2 = this.mAnrFileType1Records.get(Integer.valueOf(recNum)).keySet().iterator();
                        while (it2.hasNext()) {
                            int key2 = it2.next().intValue();
                            ArrayList<String> anrFile2 = this.mAnrFileType1Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(key2));
                            ArrayList<Integer> anrExtFile2 = this.mAnrFileType1RecordsExt.get(Integer.valueOf(recNum)).get(Integer.valueOf(key2));
                            String anr = null;
                            if (i < anrFile2.size()) {
                                String anr2 = anrFile2.get(i);
                                anr = anr2;
                            }
                            if (anr == null || anr.equals("")) {
                                anrs[j] = "";
                                anrsExt[j] = 255;
                                anrIndexes[j] = -1;
                            } else {
                                anrs[j] = anr;
                                anrsExt[j] = anrExtFile2.get(i).intValue();
                                anrIndexes[j] = i + 1;
                            }
                            anrEfids[j] = key2;
                            anrFileTypes[j] = 168;
                            j++;
                        }
                    }
                    int offset = 0;
                    for (int m = 0; recNum > 0 && m < recNum; m++) {
                        Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds2 = this.mPbrFile.mFileIds.get(Integer.valueOf(m));
                        offset += this.mAdnEfRecords.get(fileIds2.get(168).get(192).get(0)).intValue();
                    }
                    AdnRecord rec = this.mPhoneBookRecords.get(i + offset);
                    if (rec != null) {
                        rec.setAnrs(anrs);
                        rec.setExtRecordAnr0(anrsExt[0]);
                    } else {
                        rec = new AdnRecord("", "", (String[]) null, anrs);
                    }
                    rec.setAnrEfids(anrEfids);
                    rec.setAnrFileTypes(anrFileTypes);
                    rec.setAnrIndexes(anrIndexes);
                    this.mPhoneBookRecords.set(i + offset, rec);
                }
            }
        }
    }

    private void updatePhoneAdnRecordGrp(int recNum) {
        int grpNumInOneRecord;
        if (this.mPbrFile != null && this.mPbrFile.mFileIds != null) {
            Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds = this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
            if (this.mGrpFileRecords.get(Integer.valueOf(recNum)) == null) {
                grpNumInOneRecord = 0;
            } else {
                grpNumInOneRecord = this.mGrpFileRecords.get(Integer.valueOf(recNum)).size();
            }
            if (grpNumInOneRecord != 0) {
                int numAdnRecs = this.mAdnEfRecords.get(fileIds.get(168).get(192).get(0)).intValue();
                for (int i = 0; i < numAdnRecs; i++) {
                    byte[] grps = new byte[10];
                    int grpEfid = -1;
                    Iterator<Integer> it = this.mGrpFileRecords.get(Integer.valueOf(recNum)).keySet().iterator();
                    if (it.hasNext()) {
                        int key = it.next().intValue();
                        grpEfid = key;
                        ArrayList<byte[]> grpFile = this.mGrpFileRecords.get(Integer.valueOf(recNum)).get(Integer.valueOf(key));
                        byte[] grps2 = grpFile.get(i);
                        grps = grps2;
                        if (!this.mGsdFileRecords.isEmpty()) {
                            for (int k = 0; k < grps.length; k++) {
                                int index = grps[k] != 0 ? grps[k] - 1 : -1;
                                if (index > -1) {
                                    Iterator<Integer> it2 = this.mGsdFileRecords.get(Integer.valueOf(recNum)).keySet().iterator();
                                    if (it2.hasNext()) {
                                        int key2 = it2.next().intValue();
                                        if (index < this.mGsdFileRecords.get(Integer.valueOf(recNum)).get(Integer.valueOf(key2)).size()) {
                                            this.mGsdFileRecords.get(Integer.valueOf(recNum)).get(Integer.valueOf(key2)).get(index);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    int offset = 0;
                    for (int m = 0; recNum > 0 && m < recNum; m++) {
                        Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds2 = this.mPbrFile.mFileIds.get(Integer.valueOf(m));
                        offset += this.mAdnEfRecords.get(fileIds2.get(168).get(192).get(0)).intValue();
                    }
                    AdnRecord rec = this.mPhoneBookRecords.get(i + offset);
                    if (rec != null) {
                        rec.setGrps(grps);
                        rec.setGrpEfid(grpEfid);
                    }
                    this.mPhoneBookRecords.set(i + offset, rec);
                }
            }
        }
    }

    private void actuallyReadEmailOrAnrOrSneFile(int recNum, int usimTag) {
        Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds;
        if (this.mPbrFile != null && this.mPbrFile.mFileIds != null && (fileIds = this.mPbrFile.mFileIds.get(Integer.valueOf(recNum))) != null && !fileIds.isEmpty()) {
            if (usimTag == USIM_EFEMAIL_TAG || usimTag == 196 || usimTag == 195) {
                boolean hasRecordsInA9 = fileIds.containsKey(169) && fileIds.get(169).containsKey(Integer.valueOf(usimTag));
                boolean hasRecordsInA8 = fileIds.containsKey(168) && fileIds.get(168).containsKey(Integer.valueOf(usimTag));
                Rlog.d(LOG_TAG, "in actuallyReadEmailOrAnrOrSneFile(), hasRecordsInA9: " + hasRecordsInA9);
                Rlog.d(LOG_TAG, "in actuallyReadEmailOrAnrOrSneFile(), hasRecordsInA8: " + hasRecordsInA8);
                if (hasRecordsInA9 || hasRecordsInA8) {
                    if (hasRecordsInA9) {
                        if (fileIds.get(168).containsKey(193)) {
                            int iapEfid = fileIds.get(168).get(193).get(0).intValue();
                            if (this.mIapFileRecords.get(Integer.valueOf(recNum)) == null) {
                                readIapFileAndWait(recNum, iapEfid);
                                if (this.mIapFileRecords.get(Integer.valueOf(recNum)) == null) {
                                    Rlog.e(LOG_TAG, "Error: IAP file is empty");
                                    return;
                                }
                            }
                            Map<Integer, Integer> efRecords = (HashMap) fileIds.get(169).get(Integer.valueOf(usimTag));
                            Iterator<Integer> it = efRecords.keySet().iterator();
                            while (it.hasNext()) {
                                int key = it.next().intValue();
                                Rlog.e(LOG_TAG, "fileIds.get(USIM_TYPE1_TAG).get(USIM_EFADN_TAG).get(0) = " + fileIds.get(168).get(192).get(0));
                                Rlog.e(LOG_TAG, "mAdnEfRecords = " + this.mAdnEfRecords);
                                this.mFh.getEFLinearRecordSize(efRecords.get(Integer.valueOf(key)).intValue(), obtainMessage(14));
                                try {
                                    this.mLock.wait();
                                } catch (InterruptedException e) {
                                    Rlog.e(LOG_TAG, "records in A9, Interrupted Exception in actuallyReadEmailOrAnrOrSneFile(), tag: " + usimTag);
                                }
                                int numAdnRecs = this.mAdnEfRecords.get(fileIds.get(168).get(192).get(0)).intValue();
                                for (int i = 0; i < numAdnRecs; i++) {
                                    try {
                                        byte[] record = this.mIapFileRecords.get(Integer.valueOf(recNum)).get(i);
                                        byte[] record2 = record;
                                        int order = this.mPbrFile.mFileType2Order.get(Integer.valueOf(recNum)).get(Integer.valueOf(usimTag)).get(efRecords.get(Integer.valueOf(key))).intValue();
                                        int num = record2[order];
                                        Rlog.e(LOG_TAG, "adn num: " + i + "tag: " + usimTag + "efid: " + efRecords.get(Integer.valueOf(key)) + "num is " + num);
                                        if (num > 0 && num != 255) {
                                            Message response = null;
                                            if (usimTag == USIM_EFEMAIL_TAG) {
                                                response = obtainMessage(11, 169, efRecords.get(Integer.valueOf(key)).intValue(), Integer.valueOf(num));
                                            } else if (usimTag == 196) {
                                                response = obtainMessage(12, 169, efRecords.get(Integer.valueOf(key)).intValue(), Integer.valueOf(num));
                                            } else if (usimTag == 195) {
                                                response = obtainMessage(13, 169, efRecords.get(Integer.valueOf(key)).intValue(), Integer.valueOf(num));
                                            }
                                            Rlog.e(LOG_TAG, "mRecordSize[0]= " + this.mRecordSize[0]);
                                            if (this.mRecordSize != null && this.mRecordSize[0] != 0) {
                                                this.mFh.loadEFLinearFixedWithRecordSize(efRecords.get(Integer.valueOf(key)).intValue(), num, this.mRecordSize[0], response);
                                            }
                                            try {
                                                this.mLock.wait();
                                            } catch (InterruptedException e2) {
                                                Rlog.e(LOG_TAG, "records in A9, Interrupted Exception in actuallyReadEmailOrAnrOrSneFile(), tag: " + usimTag);
                                            }
                                        }
                                    } catch (IndexOutOfBoundsException e3) {
                                        Rlog.e(LOG_TAG, "Error: Improper ICC card: No IAP record for ADN, continuing");
                                    }
                                }
                                if (usimTag == USIM_EFEMAIL_TAG) {
                                    ArrayList<String> emails = new ArrayList<>(numAdnRecs);
                                    for (int i2 = 0; i2 < numAdnRecs; i2++) {
                                        if (this.mEmailsHash.containsKey(Integer.valueOf(i2))) {
                                            emails.add(i2, this.mEmailsHash.get(Integer.valueOf(i2)));
                                        } else {
                                            emails.add(i2, "");
                                        }
                                    }
                                    this.mEmailsHash.clear();
                                    if (this.mEmailFileType2Records.get(Integer.valueOf(recNum)) == null) {
                                        Map<Integer, ArrayList<String>> records = new HashMap<>();
                                        records.put(efRecords.get(Integer.valueOf(key)), emails);
                                        this.mEmailFileType2Records.put(Integer.valueOf(recNum), records);
                                    } else {
                                        this.mEmailFileType2Records.get(Integer.valueOf(recNum)).put(efRecords.get(Integer.valueOf(key)), emails);
                                    }
                                    Rlog.d(LOG_TAG, "mEmailFileType2Records: " + this.mEmailFileType2Records);
                                } else if (usimTag == 196) {
                                    ArrayList<String> anrs = new ArrayList<>(numAdnRecs);
                                    ArrayList<Integer> anrs0Ext = new ArrayList<>(numAdnRecs);
                                    for (int i3 = 0; i3 < numAdnRecs; i3++) {
                                        if (this.mAnrsHash.containsKey(Integer.valueOf(i3))) {
                                            anrs.add(i3, this.mAnrsHash.get(Integer.valueOf(i3)));
                                        } else {
                                            anrs.add(i3, "");
                                        }
                                        if (this.mAnrs0ExtHash.containsKey(Integer.valueOf(i3))) {
                                            anrs0Ext.add(i3, this.mAnrs0ExtHash.get(Integer.valueOf(i3)));
                                        } else {
                                            anrs0Ext.add(i3, 255);
                                        }
                                    }
                                    this.mAnrsHash.clear();
                                    this.mAnrs0ExtHash.clear();
                                    if (this.mAnrFileType2Records.get(Integer.valueOf(recNum)) == null) {
                                        Map<Integer, ArrayList<String>> records2 = new HashMap<>();
                                        records2.put(efRecords.get(Integer.valueOf(key)), anrs);
                                        Map<Integer, ArrayList<Integer>> ext0 = new HashMap<>();
                                        ext0.put(efRecords.get(Integer.valueOf(key)), anrs0Ext);
                                        this.mAnrFileType2Records.put(Integer.valueOf(recNum), records2);
                                        this.mAnrFileType2RecordsExt.put(Integer.valueOf(recNum), ext0);
                                    } else {
                                        Map<Integer, ArrayList<String>> records3 = this.mAnrFileType2Records.get(Integer.valueOf(recNum));
                                        Map<Integer, ArrayList<Integer>> ext02 = this.mAnrFileType2RecordsExt.get(Integer.valueOf(recNum));
                                        records3.put(efRecords.get(Integer.valueOf(key)), anrs);
                                        ext02.put(efRecords.get(Integer.valueOf(key)), anrs0Ext);
                                    }
                                    Rlog.d(LOG_TAG, "mAnrFileType2Records: " + this.mAnrFileType2Records);
                                } else if (usimTag == 195) {
                                    ArrayList<String> snes = new ArrayList<>(numAdnRecs);
                                    for (int i4 = 0; i4 < numAdnRecs; i4++) {
                                        if (this.mSnesHash.containsKey(Integer.valueOf(i4))) {
                                            snes.add(i4, this.mSnesHash.get(Integer.valueOf(i4)));
                                        } else {
                                            snes.add(i4, "");
                                        }
                                    }
                                    this.mSnesHash.clear();
                                    if (this.mSneFileType2Records.get(Integer.valueOf(recNum)) == null) {
                                        Map<Integer, ArrayList<String>> records4 = new HashMap<>();
                                        records4.put(efRecords.get(Integer.valueOf(key)), snes);
                                        this.mSneFileType2Records.put(Integer.valueOf(recNum), records4);
                                    } else {
                                        this.mSneFileType2Records.get(Integer.valueOf(recNum)).put(efRecords.get(Integer.valueOf(key)), snes);
                                    }
                                    Rlog.d(LOG_TAG, "mSneFileType2Records: " + this.mSneFileType2Records);
                                }
                            }
                        } else {
                            Rlog.e(LOG_TAG, "Error: IAP file does not exist");
                            return;
                        }
                    }
                    if (hasRecordsInA8) {
                        Map<Integer, Integer> efRecords2 = (HashMap) fileIds.get(168).get(Integer.valueOf(usimTag));
                        Iterator<Integer> it2 = efRecords2.keySet().iterator();
                        while (it2.hasNext()) {
                            int key2 = it2.next().intValue();
                            Message response2 = null;
                            if (usimTag == USIM_EFEMAIL_TAG) {
                                response2 = obtainMessage(4, 168, efRecords2.get(Integer.valueOf(key2)).intValue(), null);
                            } else if (usimTag == 196) {
                                response2 = obtainMessage(6, 168, efRecords2.get(Integer.valueOf(key2)).intValue(), null);
                            } else if (usimTag == 195) {
                                response2 = obtainMessage(7, 168, efRecords2.get(Integer.valueOf(key2)).intValue(), null);
                            }
                            Bundle bundle = new Bundle();
                            bundle.putInt(PBR_RECORD_INDEX, recNum);
                            response2.setData(bundle);
                            Rlog.d(LOG_TAG, "mValidADNIndexes= " + this.mValidADNIndexes);
                            this.mFh.loadEFLinearFixedByIndexes(efRecords2.get(Integer.valueOf(key2)).intValue(), this.mValidADNIndexes.get(Integer.valueOf(recNum)), response2);
                            try {
                                this.mLock.wait();
                            } catch (InterruptedException e4) {
                                Rlog.e(LOG_TAG, "records in A8, Interrupted Exception in actuallyReadEmailFile(), tag: " + usimTag);
                            }
                            if (usimTag == USIM_EFEMAIL_TAG) {
                                if (this.mEmailFileType1Records.get(Integer.valueOf(recNum)) == null || this.mEmailFileType1Records.get(Integer.valueOf(recNum)).get(efRecords2.get(Integer.valueOf(key2))) == null) {
                                    Rlog.e(LOG_TAG, "records in A8, Error: Type 1 file is empty, tag: " + usimTag + "key: " + efRecords2.get(Integer.valueOf(key2)));
                                    return;
                                }
                            } else if (usimTag == 196) {
                                if (this.mAnrFileType1Records.get(Integer.valueOf(recNum)) == null || this.mAnrFileType1Records.get(Integer.valueOf(recNum)).get(efRecords2.get(Integer.valueOf(key2))) == null) {
                                    Rlog.e(LOG_TAG, "records in A8, Error: Type 1 file is empty, tag: " + usimTag + "key: " + efRecords2.get(Integer.valueOf(key2)));
                                    return;
                                }
                            } else if (usimTag == 195 && (this.mSneFileType1Records.get(Integer.valueOf(recNum)) == null || this.mSneFileType1Records.get(Integer.valueOf(recNum)).get(efRecords2.get(Integer.valueOf(key2))) == null)) {
                                Rlog.e(LOG_TAG, "records in A8, Error: Type 1 file is empty, tag: " + usimTag + "key: " + efRecords2.get(Integer.valueOf(key2)));
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void readIapFileAndWait(int recNum, int efid) {
        Rlog.d(LOG_TAG, "begin readIapFileAndWait(), efid: " + efid);
        this.mFh.loadEFLinearFixedByIndexes(efid, this.mValidADNIndexes.get(Integer.valueOf(recNum)), obtainMessage(3, recNum, -1));
        try {
            this.mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in readIapFileAndWait");
        }
    }

    private void updatePhoneAdnRecord() {
        if (this.mEmailFileRecord != null) {
            int numAdnRecs = this.mPhoneBookRecords.size();
            if (this.mIapFileRecord != null) {
                for (int i = 0; i < numAdnRecs; i++) {
                    try {
                        byte[] record = this.mIapFileRecord.get(i);
                        int recNum = record[this.mEmailTagNumberInIap];
                        if (recNum != -1) {
                            String[] emails = {readEmailRecord(recNum - 1)};
                            AdnRecord rec = this.mPhoneBookRecords.get(i);
                            if (rec != null) {
                                rec.setEmails(emails);
                            } else {
                                rec = new AdnRecord("", "", emails);
                            }
                            this.mPhoneBookRecords.set(i, rec);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        Rlog.e(LOG_TAG, "Error: Improper ICC card: No IAP record for ADN, continuing");
                    }
                }
            }
            int len = this.mPhoneBookRecords.size();
            if (this.mEmailsForAdnRec == null) {
                parseType1EmailFile(len);
            }
            for (int i2 = 0; i2 < numAdnRecs; i2++) {
                try {
                    ArrayList<String> emailList = this.mEmailsForAdnRec.get(Integer.valueOf(i2));
                    if (emailList != null) {
                        AdnRecord rec2 = this.mPhoneBookRecords.get(i2);
                        String[] emails2 = new String[emailList.size()];
                        System.arraycopy(emailList.toArray(), 0, emails2, 0, emailList.size());
                        rec2.setEmails(emails2);
                        this.mPhoneBookRecords.set(i2, rec2);
                    }
                } catch (IndexOutOfBoundsException e2) {
                    return;
                }
            }
        }
    }

    public void updateUsimAdn(int position, AdnRecord adn) {
        this.mPhoneBookRecords.set(position, adn);
    }

    public boolean updateUsimAdnEmail(AdnRecord adn, int recNum, int index) {
        Map<Integer, Integer> iapEfids;
        ArrayList<byte[]> iapFileRecord;
        Rlog.d(LOG_TAG, "begin updateUsimAdnEmail()");
        if (adn.emailFileTypes[0] == 169) {
            if (this.mEmailFileType2Records.get(Integer.valueOf(recNum)) != null && this.mEmailFileType2Records.get(Integer.valueOf(recNum)).containsKey(Integer.valueOf(adn.emailEfids[0]))) {
                ArrayList<String> emails = this.mEmailFileType2Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(adn.emailEfids[0]));
                emails.set(index - 1, adn.mEmails[0]);
                Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds = this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
                if (fileIds == null || fileIds.isEmpty() || (iapEfids = (HashMap) fileIds.get(168).get(193)) == null || iapEfids.isEmpty() || (iapFileRecord = this.mIapFileRecords.get(Integer.valueOf(recNum))) == null) {
                    return false;
                }
                try {
                    byte[] record = iapFileRecord.get(adn.mRecordNumber - 1);
                    int emailOrder = this.mPbrFile.mFileType2Order.get(Integer.valueOf(recNum)).get(Integer.valueOf(USIM_EFEMAIL_TAG)).get(Integer.valueOf(adn.emailEfids[0])).intValue();
                    if (adn.emailIndexes[0] == -1) {
                        record[emailOrder] = -1;
                    } else {
                        record[emailOrder] = (byte) (index & 255);
                    }
                    byte[] newRecord = new byte[record.length];
                    System.arraycopy(record, 0, newRecord, 0, newRecord.length);
                    this.mFh.updateEFLinearFixed(iapEfids.get(0).intValue(), adn.mRecordNumber, newRecord, null, obtainMessage(5));
                    return this.mUpdateIapSuccess;
                } catch (IndexOutOfBoundsException e) {
                    Rlog.e(LOG_TAG, "Error: updateUsimAdnEmail");
                    return false;
                }
            }
            return false;
        }
        if (this.mEmailFileType1Records.get(Integer.valueOf(recNum)) != null && this.mEmailFileType1Records.get(Integer.valueOf(recNum)).containsKey(Integer.valueOf(adn.emailEfids[0]))) {
            ArrayList<String> emails2 = this.mEmailFileType1Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(adn.emailEfids[0]));
            emails2.set(index - 1, adn.mEmails[0]);
            return true;
        }
        return false;
    }

    public boolean updateUsimAdnAnr(AdnRecord adn, int recNum, int index) {
        Map<Integer, Integer> iapEfids;
        ArrayList<byte[]> iapFileRecord;
        Rlog.d(LOG_TAG, "begin updateUsimAdnAnr()");
        if (adn.anrFileTypes[0] == 169) {
            if (this.mAnrFileType2Records.get(Integer.valueOf(recNum)) != null && this.mAnrFileType2Records.get(Integer.valueOf(recNum)).containsKey(Integer.valueOf(adn.anrEfids[0]))) {
                ArrayList<String> anrs = this.mAnrFileType2Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(adn.anrEfids[0]));
                ArrayList<Integer> anrsExt1 = this.mAnrFileType2RecordsExt.get(Integer.valueOf(recNum)).get(Integer.valueOf(adn.anrEfids[0]));
                anrs.set(index - 1, adn.anrs[0]);
                anrsExt1.set(index - 1, Integer.valueOf(adn.getExtRecordAnr0()));
                Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds = this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
                if (fileIds == null || fileIds.isEmpty() || (iapEfids = (HashMap) fileIds.get(168).get(193)) == null || iapEfids.isEmpty() || (iapFileRecord = this.mIapFileRecords.get(Integer.valueOf(recNum))) == null) {
                    return false;
                }
                try {
                    byte[] record = iapFileRecord.get(adn.mRecordNumber - 1);
                    int anrOrder = this.mPbrFile.mFileType2Order.get(Integer.valueOf(recNum)).get(196).get(Integer.valueOf(adn.anrEfids[0])).intValue();
                    if (adn.anrIndexes[0] == -1) {
                        record[anrOrder] = -1;
                    } else {
                        record[anrOrder] = (byte) (index & 255);
                    }
                    byte[] newRecord = new byte[record.length];
                    System.arraycopy(record, 0, newRecord, 0, newRecord.length);
                    this.mFh.updateEFLinearFixed(iapEfids.get(0).intValue(), adn.mRecordNumber, newRecord, null, obtainMessage(5));
                    return this.mUpdateIapSuccess;
                } catch (IndexOutOfBoundsException e) {
                    Rlog.e(LOG_TAG, "Error: updateUsimAdnAnr");
                    return false;
                }
            }
            return false;
        }
        if (this.mAnrFileType1Records.get(Integer.valueOf(recNum)) != null && this.mAnrFileType1Records.get(Integer.valueOf(recNum)).containsKey(Integer.valueOf(adn.anrEfids[0]))) {
            ArrayList<String> anrs2 = this.mAnrFileType1Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(adn.anrEfids[0]));
            ArrayList<Integer> anrsExt12 = this.mAnrFileType1RecordsExt.get(Integer.valueOf(recNum)).get(Integer.valueOf(adn.anrEfids[0]));
            anrs2.set(index - 1, adn.anrs[0]);
            anrsExt12.set(index - 1, Integer.valueOf(adn.getExtRecordAnr0()));
            return true;
        }
        return false;
    }

    public boolean updateUsimAdnSne(AdnRecord adn, int recNum, int index) {
        Map<Integer, Integer> iapEfids;
        ArrayList<byte[]> iapFileRecord;
        Rlog.d(LOG_TAG, "begin updateUsimAdnSne()");
        if (adn.sneFileTypes[0] == 169) {
            if (this.mSneFileType2Records.get(Integer.valueOf(recNum)) != null && this.mSneFileType2Records.get(Integer.valueOf(recNum)).containsKey(Integer.valueOf(adn.sneEfids[0]))) {
                ArrayList<String> snes = this.mSneFileType2Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(adn.sneEfids[0]));
                snes.set(index - 1, adn.snes[0]);
                Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds = this.mPbrFile.mFileIds.get(Integer.valueOf(recNum));
                if (fileIds == null || fileIds.isEmpty() || (iapEfids = (HashMap) fileIds.get(168).get(193)) == null || iapEfids.isEmpty() || (iapFileRecord = this.mIapFileRecords.get(Integer.valueOf(recNum))) == null) {
                    return false;
                }
                try {
                    byte[] record = iapFileRecord.get(adn.mRecordNumber - 1);
                    int sneOrder = this.mPbrFile.mFileType2Order.get(Integer.valueOf(recNum)).get(195).get(Integer.valueOf(adn.sneEfids[0])).intValue();
                    if (adn.sneIndexes[0] == -1) {
                        record[sneOrder] = -1;
                    } else {
                        record[sneOrder] = (byte) (index & 255);
                    }
                    byte[] newRecord = new byte[record.length];
                    System.arraycopy(record, 0, newRecord, 0, newRecord.length);
                    this.mFh.updateEFLinearFixed(iapEfids.get(0).intValue(), adn.mRecordNumber, newRecord, null, obtainMessage(5));
                    return this.mUpdateIapSuccess;
                } catch (IndexOutOfBoundsException e) {
                    Rlog.e(LOG_TAG, "Error: updateUsimAdnSne");
                    return false;
                }
            }
            return false;
        }
        if (this.mSneFileType1Records.get(Integer.valueOf(recNum)) != null && this.mSneFileType1Records.get(Integer.valueOf(recNum)).containsKey(Integer.valueOf(adn.sneEfids[0]))) {
            ArrayList<String> snes2 = this.mSneFileType1Records.get(Integer.valueOf(recNum)).get(Integer.valueOf(adn.sneEfids[0]));
            snes2.set(index - 1, adn.snes[0]);
            return true;
        }
        return false;
    }

    public boolean updateUsimAdnGrp(AdnRecord adn, int recNum, int index) {
        Rlog.d(LOG_TAG, "begin updateUsimAdnGrp()");
        if (this.mGrpFileRecords.get(Integer.valueOf(recNum)) == null || !this.mGrpFileRecords.get(Integer.valueOf(recNum)).containsKey(Integer.valueOf(adn.grpEfid))) {
            return false;
        }
        ArrayList<byte[]> grps = this.mGrpFileRecords.get(Integer.valueOf(recNum)).get(Integer.valueOf(adn.grpEfid));
        grps.set(index - 1, adn.grps);
        return true;
    }

    void parseType1EmailFile(int numRecs) {
        String email;
        this.mEmailsForAdnRec = new HashMap();
        for (int i = 0; i < numRecs; i++) {
            try {
                byte[] emailRec = this.mEmailFileRecord.get(i);
                int adnRecNum = emailRec[emailRec.length - 1];
                if (adnRecNum != -1 && (email = readEmailRecord(i)) != null && !email.equals("")) {
                    ArrayList<String> val = this.mEmailsForAdnRec.get(Integer.valueOf(adnRecNum - 1));
                    if (val == null) {
                        val = new ArrayList<>();
                    }
                    val.add(email);
                    this.mEmailsForAdnRec.put(Integer.valueOf(adnRecNum - 1), val);
                }
            } catch (IndexOutOfBoundsException e) {
                Rlog.e(LOG_TAG, "Error: Improper ICC card: No email record for ADN, continuing");
                return;
            }
        }
    }

    private String readEmailRecord(int recNum) {
        try {
            byte[] emailRec = this.mEmailFileRecord.get(recNum);
            return IccUtils.adnStringFieldToString(emailRec, 0, emailRec.length - 2);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private void readAdnFileAndWait(int recNum) {
        Map<Integer, Map<Integer, Map<Integer, Integer>>> fileIds;
        if (this.mPbrFile != null && this.mPbrFile.mFileIds != null && (fileIds = this.mPbrFile.mFileIds.get(Integer.valueOf(recNum))) != null && !fileIds.isEmpty()) {
            if (fileIds.containsKey(170) && fileIds.get(170).containsKey(194)) {
                this.extEf = fileIds.get(170).get(194).get(0).intValue();
                this.mAdnCache.mUSIMExt1 = this.extEf;
            }
            this.mAdnCache.requestLoadAllAdnLike(fileIds.get(168).get(192).get(0).intValue(), this.extEf, obtainMessage(2, fileIds.get(168).get(192).get(0).intValue(), recNum, null));
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                Rlog.e(LOG_TAG, "Interrupted Exception in readAdnFileAndWait");
            }
        }
    }

    private void createPbrFile(ArrayList<byte[]> records) {
        if (records == null) {
            this.mPbrFile = null;
            this.mIsPbrPresent = false;
        } else {
            this.mPbrFile = new PbrFile(records);
        }
    }

    private void printPbrFile(PbrFile pbrFile) {
        if (pbrFile != null && pbrFile.mFileIds != null) {
            HashMap<Integer, Map<Integer, Map<Integer, Map<Integer, Integer>>>> map = pbrFile.mFileIds;
            Iterator<Integer> it = map.keySet().iterator();
            while (it.hasNext()) {
                int pbrKey = it.next().intValue();
                log("" + pbrKey);
                HashMap subPbr = (HashMap) map.get(Integer.valueOf(pbrKey));
                Iterator subPbrIterator = subPbr.keySet().iterator();
                while (subPbrIterator.hasNext()) {
                    int subPbrKey = ((Integer) subPbrIterator.next()).intValue();
                    log("    ->" + subPbrKey);
                    HashMap AHashmap = (HashMap) subPbr.get(Integer.valueOf(subPbrKey));
                    Iterator AIterator = AHashmap.keySet().iterator();
                    while (AIterator.hasNext()) {
                        int AKey = ((Integer) AIterator.next()).intValue();
                        log("       ->" + AKey);
                        HashMap CHashmap = (HashMap) AHashmap.get(Integer.valueOf(AKey));
                        Iterator CIterator = CHashmap.keySet().iterator();
                        while (CIterator.hasNext()) {
                            int CKey = ((Integer) CIterator.next()).intValue();
                            log("           ->" + CKey);
                            log("           ->" + CHashmap.get(Integer.valueOf(CKey)));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        String anr;
        String anr2;
        switch (msg.what) {
            case 1:
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    createPbrFile((ArrayList) ar.result);
                    printPbrFile(this.mPbrFile);
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 2:
                log("Loading USIM ADN records done");
                AsyncResult ar2 = (AsyncResult) msg.obj;
                if (ar2.exception == null) {
                    this.mPhoneBookRecords.addAll((ArrayList) ar2.result);
                    int size = ((ArrayList) ar2.result).size();
                    if (!this.mAdnEfRecords.containsKey(Integer.valueOf(msg.arg1))) {
                        this.mAdnEfRecords.put(Integer.valueOf(msg.arg1), Integer.valueOf(size));
                    }
                    ArrayList<Integer> indexes = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        AdnRecord rec = (AdnRecord) ((ArrayList) ar2.result).get(i);
                        if (rec != null && !rec.isEmpty()) {
                            indexes.add(Integer.valueOf(i + 1));
                        }
                    }
                    if (!this.mValidADNIndexes.containsKey(Integer.valueOf(msg.arg2))) {
                        this.mValidADNIndexes.put(Integer.valueOf(msg.arg2), indexes);
                    }
                }
                Rlog.d(LOG_TAG, "mValidADNIndexes= " + this.mValidADNIndexes);
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 3:
                log("Loading USIM IAP records done");
                AsyncResult ar3 = (AsyncResult) msg.obj;
                int recNum = msg.arg1;
                if (ar3.exception == null) {
                    this.mIapFileRecords.put(Integer.valueOf(recNum), (ArrayList) ar3.result);
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 4:
                log("Loading USIM Email records done");
                AsyncResult ar4 = (AsyncResult) msg.obj;
                if (ar4.exception == null) {
                    ArrayList<byte[]> datas = (ArrayList) ar4.result;
                    ArrayList<String> emails = new ArrayList<>(datas.size());
                    int s = datas.size();
                    for (int i2 = 0; i2 < s; i2++) {
                        byte[] data = datas.get(i2);
                        emails.add(IccUtils.adnStringFieldToString(data, 0, data.length - 2));
                    }
                    int pbrRecordIndex = msg.getData().getInt(PBR_RECORD_INDEX, 0);
                    if (msg.arg1 == 169) {
                        if (this.mEmailFileType2Records.get(Integer.valueOf(pbrRecordIndex)) == null) {
                            Map<Integer, ArrayList<String>> records = new HashMap<>();
                            records.put(Integer.valueOf(msg.arg2), emails);
                            this.mEmailFileType2Records.put(Integer.valueOf(pbrRecordIndex), records);
                        } else {
                            this.mEmailFileType2Records.get(Integer.valueOf(pbrRecordIndex)).put(Integer.valueOf(msg.arg2), emails);
                        }
                        Rlog.d(LOG_TAG, "mEmailFileType2Records: " + this.mEmailFileType2Records);
                    } else if (msg.arg1 == 168) {
                        if (this.mEmailFileType1Records.get(Integer.valueOf(pbrRecordIndex)) == null) {
                            Map<Integer, ArrayList<String>> records2 = new HashMap<>();
                            records2.put(Integer.valueOf(msg.arg2), emails);
                            this.mEmailFileType1Records.put(Integer.valueOf(pbrRecordIndex), records2);
                        } else {
                            this.mEmailFileType1Records.get(Integer.valueOf(pbrRecordIndex)).put(Integer.valueOf(msg.arg2), emails);
                        }
                    }
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 5:
                log("update USIM IAP records done");
                if (((AsyncResult) msg.obj).exception == null) {
                    this.mUpdateIapSuccess = true;
                    return;
                } else {
                    this.mUpdateIapSuccess = false;
                    return;
                }
            case 6:
                log("Loading USIM ANR records done");
                AsyncResult ar5 = (AsyncResult) msg.obj;
                if (ar5.exception == null) {
                    ArrayList<byte[]> datas2 = (ArrayList) ar5.result;
                    ArrayList<String> anrs = new ArrayList<>(datas2.size());
                    ArrayList<Integer> anrs0Ext = new ArrayList<>(datas2.size());
                    int s2 = datas2.size();
                    for (int i3 = 0; i3 < s2; i3++) {
                        byte[] data2 = datas2.get(i3);
                        int numberLength = data2[1] & 255;
                        if (numberLength > 11) {
                            anr2 = "";
                        } else {
                            anr2 = PhoneNumberUtils.calledPartyBCDToString(data2, 2, numberLength);
                        }
                        int extRecord = data2[14] & 255;
                        if (hasExtendedRecord(extRecord) && !this.mAdnCache.mAdnExt1Map.isEmpty()) {
                            byte[] ext1Data = this.mAdnCache.mAdnExt1Map.get(Integer.valueOf(extRecord));
                            Rlog.d(LOG_TAG, "ANR extension EF: 0x" + Integer.toHexString(this.extEf) + ":" + extRecord + "\n" + IccUtils.bytesToHexString(ext1Data));
                            try {
                            } catch (RuntimeException ex) {
                                Rlog.w(LOG_TAG, "Error parsing anr ext record", ex);
                            }
                            if (ext1Data.length == 13 && (ext1Data[0] & 3) == 2 && (ext1Data[1] & 255) <= 10) {
                                anr2 = anr2 + PhoneNumberUtils.calledPartyBCDFragmentToString(ext1Data, 2, ext1Data[1] & 255);
                                anrs.add(anr2);
                                anrs0Ext.add(Integer.valueOf(extRecord));
                            }
                        } else {
                            anrs.add(anr2);
                            anrs0Ext.add(Integer.valueOf(extRecord));
                        }
                    }
                    int pbrRecordIndex2 = msg.getData().getInt(PBR_RECORD_INDEX, 0);
                    if (msg.arg1 == 169) {
                        if (this.mAnrFileType2Records.get(Integer.valueOf(pbrRecordIndex2)) == null) {
                            Map<Integer, ArrayList<String>> records3 = new HashMap<>();
                            records3.put(Integer.valueOf(msg.arg2), anrs);
                            Map<Integer, ArrayList<Integer>> ext0 = new HashMap<>();
                            ext0.put(Integer.valueOf(msg.arg2), anrs0Ext);
                            this.mAnrFileType2Records.put(Integer.valueOf(pbrRecordIndex2), records3);
                            this.mAnrFileType2RecordsExt.put(Integer.valueOf(pbrRecordIndex2), ext0);
                        } else {
                            Map<Integer, ArrayList<String>> records4 = this.mAnrFileType2Records.get(Integer.valueOf(pbrRecordIndex2));
                            Map<Integer, ArrayList<Integer>> ext02 = this.mAnrFileType2RecordsExt.get(Integer.valueOf(pbrRecordIndex2));
                            records4.put(Integer.valueOf(msg.arg2), anrs);
                            ext02.put(Integer.valueOf(msg.arg2), anrs0Ext);
                        }
                    } else if (msg.arg1 == 168) {
                        if (this.mAnrFileType1Records.get(Integer.valueOf(pbrRecordIndex2)) == null) {
                            Map<Integer, ArrayList<String>> records5 = new HashMap<>();
                            records5.put(Integer.valueOf(msg.arg2), anrs);
                            Map<Integer, ArrayList<Integer>> ext03 = new HashMap<>();
                            ext03.put(Integer.valueOf(msg.arg2), anrs0Ext);
                            this.mAnrFileType1Records.put(Integer.valueOf(pbrRecordIndex2), records5);
                            this.mAnrFileType1RecordsExt.put(Integer.valueOf(pbrRecordIndex2), ext03);
                        } else {
                            Map<Integer, ArrayList<String>> records6 = this.mAnrFileType1Records.get(Integer.valueOf(pbrRecordIndex2));
                            Map<Integer, ArrayList<Integer>> ext04 = this.mAnrFileType1RecordsExt.get(Integer.valueOf(pbrRecordIndex2));
                            records6.put(Integer.valueOf(msg.arg2), anrs);
                            ext04.put(Integer.valueOf(msg.arg2), anrs0Ext);
                        }
                        Rlog.d(LOG_TAG, "mAnrFileType1Records: " + this.mAnrFileType1Records);
                    }
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 7:
                log("Loading USIM Sne records done");
                AsyncResult ar6 = (AsyncResult) msg.obj;
                if (ar6.exception == null) {
                    ArrayList<byte[]> datas3 = (ArrayList) ar6.result;
                    ArrayList<String> snes = new ArrayList<>(datas3.size());
                    int pbrRecordIndex3 = msg.getData().getInt(PBR_RECORD_INDEX, 0);
                    if (msg.arg1 == 169) {
                        for (byte[] data3 : datas3) {
                            snes.add(IccUtils.adnStringFieldToString(data3, 0, data3.length - 2));
                        }
                        if (this.mSneFileType2Records.get(Integer.valueOf(pbrRecordIndex3)) == null) {
                            Map<Integer, ArrayList<String>> records7 = new HashMap<>();
                            records7.put(Integer.valueOf(msg.arg2), snes);
                            this.mSneFileType2Records.put(Integer.valueOf(pbrRecordIndex3), records7);
                        } else {
                            this.mSneFileType2Records.get(Integer.valueOf(pbrRecordIndex3)).put(Integer.valueOf(msg.arg2), snes);
                        }
                        Rlog.d(LOG_TAG, "mSneFileType2Records: " + this.mSneFileType2Records);
                    } else if (msg.arg1 == 168) {
                        for (byte[] data4 : datas3) {
                            Rlog.v(LOG_TAG, "sne data = " + data4);
                            Rlog.v(LOG_TAG, "sne data.length = " + data4.length);
                            snes.add(IccUtils.adnStringFieldToString(data4, 0, data4.length));
                        }
                        if (this.mSneFileType1Records.get(Integer.valueOf(pbrRecordIndex3)) == null) {
                            Map<Integer, ArrayList<String>> records8 = new HashMap<>();
                            records8.put(Integer.valueOf(msg.arg2), snes);
                            this.mSneFileType1Records.put(Integer.valueOf(pbrRecordIndex3), records8);
                        } else {
                            this.mSneFileType1Records.get(Integer.valueOf(pbrRecordIndex3)).put(Integer.valueOf(msg.arg2), snes);
                        }
                    }
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 8:
                log("Loading USIM GRP records done");
                AsyncResult ar7 = (AsyncResult) msg.obj;
                int recNum2 = msg.arg1;
                int efid = msg.arg2;
                Map<Integer, ArrayList<byte[]>> grpFileRecord = new HashMap<>();
                if (ar7.exception == null) {
                    grpFileRecord.put(Integer.valueOf(efid), (ArrayList) ar7.result);
                    this.mGrpFileRecords.put(Integer.valueOf(recNum2), grpFileRecord);
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 9:
                log("Loading USIM GSD records done");
                AsyncResult ar8 = (AsyncResult) msg.obj;
                int recNum3 = msg.arg1;
                int efid2 = msg.arg2;
                Map<Integer, ArrayList<String>> gsdFileRecord = new HashMap<>();
                if (ar8.exception == null) {
                    ArrayList<byte[]> gsdList = (ArrayList) ar8.result;
                    int i4 = 1;
                    ArrayList<String> gsdTags = new ArrayList<>();
                    for (byte[] gsdBytes : gsdList) {
                        String gsdTag = IccUtils.adnStringFieldToString(gsdBytes, 0, gsdBytes.length);
                        gsdTags.add(gsdTag);
                        this.mUsimGroupsMap.put(Integer.valueOf(i4), gsdTag);
                        i4++;
                    }
                    gsdFileRecord.put(Integer.valueOf(efid2), gsdTags);
                    this.mGsdFileRecords.put(Integer.valueOf(recNum3), gsdFileRecord);
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 10:
            default:
                return;
            case 11:
                Rlog.e(LOG_TAG, "load single email record done");
                AsyncResult ar9 = (AsyncResult) msg.obj;
                int numEmail = ((Integer) ar9.userObj).intValue();
                if (ar9.exception == null) {
                    byte[] data5 = (byte[]) ar9.result;
                    String email = IccUtils.adnStringFieldToString(data5, 0, data5.length - 2);
                    Rlog.e(LOG_TAG, "load single email is " + email + "at " + numEmail);
                    this.mEmailsHash.put(Integer.valueOf(numEmail - 1), email);
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 12:
                Rlog.e(LOG_TAG, "load single anr record done");
                AsyncResult ar10 = (AsyncResult) msg.obj;
                int numANR = ((Integer) ar10.userObj).intValue();
                if (ar10.exception == null) {
                    byte[] data6 = (byte[]) ar10.result;
                    int numberLength2 = data6[1] & 255;
                    if (numberLength2 > 11) {
                        anr = "";
                    } else {
                        anr = PhoneNumberUtils.calledPartyBCDToString(data6, 2, numberLength2);
                    }
                    Rlog.e(LOG_TAG, "load single anr is " + anr + "at " + numANR);
                    int extRecord2 = data6[14] & 255;
                    if (hasExtendedRecord(extRecord2) && !this.mAdnCache.mAdnExt1Map.isEmpty()) {
                        byte[] ext1Data2 = this.mAdnCache.mAdnExt1Map.get(Integer.valueOf(extRecord2));
                        Rlog.d(LOG_TAG, "ANR extension EF: 0x" + Integer.toHexString(this.extEf) + ":" + extRecord2 + "\n" + IccUtils.bytesToHexString(ext1Data2));
                        try {
                            if (ext1Data2.length == 13 && (ext1Data2[0] & 3) == 2 && (ext1Data2[1] & 255) <= 10) {
                                anr = anr + PhoneNumberUtils.calledPartyBCDFragmentToString(ext1Data2, 2, ext1Data2[1] & 255);
                            } else {
                                return;
                            }
                        } catch (RuntimeException ex2) {
                            Rlog.w(LOG_TAG, "Error parsing anr ext record", ex2);
                        }
                    }
                    Rlog.e(LOG_TAG, "load single anr with ext1 is " + anr + "at " + numANR);
                    Rlog.e(LOG_TAG, "load single anr with extRecord is " + extRecord2 + "at " + numANR);
                    this.mAnrsHash.put(Integer.valueOf(numANR - 1), anr);
                    this.mAnrs0ExtHash.put(Integer.valueOf(numANR - 1), Integer.valueOf(extRecord2));
                    break;
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 13:
                Rlog.e(LOG_TAG, "load single sne record done");
                AsyncResult ar11 = (AsyncResult) msg.obj;
                int numSne = ((Integer) ar11.userObj).intValue();
                if (ar11.exception == null) {
                    String sne = IccUtils.adnStringFieldToString((byte[]) ar11.result, 0, r7.length - 2);
                    Rlog.e(LOG_TAG, "load single sne is " + sne + "at " + numSne);
                    this.mSnesHash.put(Integer.valueOf(numSne - 1), sne);
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
            case 14:
                AsyncResult ar12 = (AsyncResult) msg.obj;
                if (ar12.exception != null) {
                    throw new RuntimeException("get EF record size failed", ar12.exception);
                }
                this.mRecordSize = (int[]) ar12.result;
                Rlog.e(LOG_TAG, "mRecordSize[0]= " + this.mRecordSize[0]);
                Rlog.e(LOG_TAG, "mRecordSize[1]= " + this.mRecordSize[1]);
                Rlog.e(LOG_TAG, "mRecordSize[2]= " + this.mRecordSize[2]);
                if (this.mRecordSize.length != 3) {
                    throw new RuntimeException("get wrong EF record size format", ar12.exception);
                }
                synchronized (this.mLock) {
                    this.mLock.notify();
                    break;
                }
                return;
        }
    }

    private boolean hasExtendedRecord(int extRecord) {
        return (extRecord == 0 || extRecord == 255) ? false : true;
    }

    private static char bcdToChar(byte b) {
        if (b < 10) {
            return (char) (b + 48);
        }
        switch (b) {
            case 10:
                return '*';
            case 11:
                return '#';
            case 12:
                return PAUSE;
            case 13:
                return WILD;
            default:
                return (char) 0;
        }
    }

    private class PbrFile {
        HashMap<Integer, Map<Integer, Map<Integer, Map<Integer, Integer>>>> mFileIds = new HashMap<>();
        Map<Integer, Map<Integer, Map<Integer, Integer>>> mFileType2Order = new HashMap();
        Map<Integer, Map<Integer, Integer>> mOrder;

        PbrFile(ArrayList<byte[]> records) {
            int recNum = 0;
            for (byte[] record : records) {
                SimTlv recTlv = new SimTlv(record, 0, record.length);
                parseTag(recTlv, recNum);
                recNum++;
            }
        }

        void parseTag(SimTlv tlv, int recNum) {
            Map<Integer, Map<Integer, Map<Integer, Integer>>> pbrRecord = new HashMap<>();
            this.mOrder = new HashMap();
            do {
                Map<Integer, Map<Integer, Integer>> fileAx = new HashMap<>();
                int tag = tlv.getTag();
                switch (tag) {
                    case 168:
                        byte[] data = tlv.getData();
                        SimTlv tlvEf = new SimTlv(data, 0, data.length);
                        parseEf(tlvEf, fileAx, tag);
                        pbrRecord.put(168, fileAx);
                        break;
                    case 169:
                        byte[] data2 = tlv.getData();
                        SimTlv tlvEf2 = new SimTlv(data2, 0, data2.length);
                        parseEf(tlvEf2, fileAx, tag);
                        pbrRecord.put(169, fileAx);
                        break;
                    case 170:
                        byte[] data3 = tlv.getData();
                        SimTlv tlvEf3 = new SimTlv(data3, 0, data3.length);
                        parseEf(tlvEf3, fileAx, tag);
                        pbrRecord.put(170, fileAx);
                        break;
                }
            } while (tlv.nextObject());
            this.mFileIds.put(Integer.valueOf(recNum), pbrRecord);
            this.mFileType2Order.put(Integer.valueOf(recNum), this.mOrder);
        }

        void parseEf(SimTlv tlv, Map<Integer, Map<Integer, Integer>> fileAx, int parentTag) {
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
                    case UsimPhoneBookManager.USIM_EFUID_TAG:
                    case UsimPhoneBookManager.USIM_EFEMAIL_TAG:
                    case UsimPhoneBookManager.USIM_EFCCP1_TAG:
                        byte[] data = tlv.getData();
                        int efid = ((data[0] & 255) << 8) | (data[1] & 255);
                        if (fileAx.containsKey(Integer.valueOf(tag))) {
                            Map<Integer, Integer> fileCx = (HashMap) fileAx.get(Integer.valueOf(tag));
                            fileCx.put(Integer.valueOf(fileCx.size()), Integer.valueOf(efid));
                            if (parentTag == 169) {
                                ((HashMap) this.mOrder.get(Integer.valueOf(tag))).put(Integer.valueOf(efid), Integer.valueOf(tagNumberWithinParentTag));
                            }
                        } else {
                            Map<Integer, Integer> fileCx2 = new HashMap<>();
                            fileCx2.put(0, Integer.valueOf(efid));
                            fileAx.put(Integer.valueOf(tag), fileCx2);
                            if (parentTag == 169) {
                                Map<Integer, Integer> fileCxPosition = new HashMap<>();
                                fileCxPosition.put(Integer.valueOf(efid), Integer.valueOf(tagNumberWithinParentTag));
                                this.mOrder.put(Integer.valueOf(tag), fileCxPosition);
                            }
                        }
                        break;
                }
                tagNumberWithinParentTag++;
            } while (tlv.nextObject());
        }
    }

    private void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }
}
