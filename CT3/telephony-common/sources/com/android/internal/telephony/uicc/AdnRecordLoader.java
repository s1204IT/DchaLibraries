package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.GsmAlphabet;
import com.mediatek.internal.telephony.uicc.PhbEntry;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class AdnRecordLoader extends Handler {
    static final int EVENT_ADN_LOAD_ALL_DONE = 3;
    static final int EVENT_ADN_LOAD_DONE = 1;
    static final int EVENT_EF_LINEAR_RECORD_SIZE_DONE = 4;
    static final int EVENT_EXT_RECORD_LOAD_DONE = 2;
    static final int EVENT_PHB_LOAD_ALL_DONE = 104;
    static final int EVENT_PHB_LOAD_DONE = 103;
    static final int EVENT_PHB_QUERY_STAUTS = 105;
    static final int EVENT_UPDATE_PHB_RECORD_DONE = 101;
    static final int EVENT_UPDATE_RECORD_DONE = 5;
    static final int EVENT_VERIFY_PIN2 = 102;
    static final String LOG_TAG = "AdnRecordLoader";
    static final boolean VDBG = false;
    int current_read;
    ArrayList<AdnRecord> mAdns;
    int mEf;
    int mExtensionEF;
    private IccFileHandler mFh;
    int mPendingExtLoads;
    String mPin2;
    int mRecordNumber;
    Object mResult;
    Message mUserResponse;
    int total;
    int used;

    AdnRecordLoader(IccFileHandler fh) {
        super(Looper.getMainLooper());
        this.mFh = fh;
    }

    private String getEFPath(int efid) {
        if (efid == 28474) {
            return "3F007F10";
        }
        return null;
    }

    public void loadFromEF(int ef, int extensionEF, int recordNumber, Message response) {
        this.mEf = ef;
        this.mExtensionEF = extensionEF;
        this.mRecordNumber = recordNumber;
        this.mUserResponse = response;
        int type = getPhbStorageType(ef);
        if (type != -1) {
            this.mFh.mCi.ReadPhbEntry(type, recordNumber, recordNumber, obtainMessage(EVENT_PHB_LOAD_DONE));
        } else {
            this.mFh.loadEFLinearFixed(ef, getEFPath(ef), recordNumber, obtainMessage(1));
        }
    }

    public void loadAllFromEF(int ef, int extensionEF, Message response) {
        this.mEf = ef;
        this.mExtensionEF = extensionEF;
        this.mUserResponse = response;
        Rlog.i(LOG_TAG, "Usim :loadEFLinearFixedAll");
        int type = getPhbStorageType(ef);
        if (type != -1) {
            this.mFh.mCi.queryPhbStorageInfo(type, obtainMessage(105));
        } else {
            this.mFh.loadEFLinearFixedAll(ef, getEFPath(ef), obtainMessage(3));
        }
    }

    public void updateEF(AdnRecord adn, int ef, int extensionEF, int recordNumber, String pin2, Message response) {
        this.mEf = ef;
        this.mExtensionEF = extensionEF;
        this.mRecordNumber = recordNumber;
        this.mUserResponse = response;
        this.mPin2 = pin2;
        int type = getPhbStorageType(ef);
        if (type != -1) {
            updatePhb(adn, type);
        } else {
            this.mFh.getEFLinearRecordSize(ef, getEFPath(ef), obtainMessage(4, adn));
        }
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
                    break;
                    break;
                case 2:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    byte[] data2 = (byte[]) ar2.result;
                    AdnRecord adn2 = (AdnRecord) ar2.userObj;
                    if (ar2.exception == null) {
                        Rlog.d(LOG_TAG, "ADN extension EF: 0x" + Integer.toHexString(this.mExtensionEF) + ":" + adn2.mExtRecord + "\n" + IccUtils.bytesToHexString(data2));
                        adn2.appendExtRecord(data2);
                    } else {
                        Rlog.e(LOG_TAG, "Failed to read ext record. Clear the number now.");
                        adn2.setNumber(UsimPBMemInfo.STRING_NOT_SET);
                    }
                    this.mPendingExtLoads--;
                    break;
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
                        if (adn3.hasExtendedRecord()) {
                            this.mPendingExtLoads++;
                            this.mFh.loadEFLinearFixed(this.mExtensionEF, adn3.mExtRecord, obtainMessage(2, adn3));
                        }
                    }
                    break;
                    break;
                case 4:
                    AsyncResult ar4 = (AsyncResult) msg.obj;
                    AdnRecord adn4 = (AdnRecord) ar4.userObj;
                    if (ar4.exception != null) {
                        throw new RuntimeException("get EF record size failed", ar4.exception);
                    }
                    int[] recordSize = (int[]) ar4.result;
                    int recordIndex = this.mRecordNumber;
                    if (this.mFh instanceof CsimFileHandler) {
                        recordIndex = ((recordIndex - 1) % 250) + 1;
                    }
                    Rlog.d(LOG_TAG, "[AdnRecordLoader] recordIndex :" + recordIndex);
                    if (recordSize.length != 3 || recordIndex > recordSize[2]) {
                        throw new RuntimeException("get wrong EF record size format", ar4.exception);
                    }
                    Rlog.d(LOG_TAG, "[AdnRecordLoader] EVENT_EF_LINEAR_RECORD_SIZE_DONE safe ");
                    Rlog.d(LOG_TAG, "in EVENT_EF_LINEAR_RECORD_SIZE_DONE,call adn.buildAdnString");
                    byte[] data3 = adn4.buildAdnString(recordSize[0]);
                    if (data3 != null) {
                        this.mFh.updateEFLinearFixed(this.mEf, getEFPath(this.mEf), recordIndex, data3, this.mPin2, obtainMessage(5));
                        this.mPendingExtLoads = 1;
                    } else {
                        Rlog.d(LOG_TAG, "data is null");
                        int errorNum = adn4.getErrorNumber();
                        if (errorNum == -1) {
                            throw new RuntimeException("data is null and DIAL_STRING_TOO_LONG", CommandException.fromRilErrno(1001));
                        }
                        if (errorNum == -2) {
                            throw new RuntimeException("data is null and TEXT_STRING_TOO_LONG", CommandException.fromRilErrno(1002));
                        }
                        if (errorNum == -15) {
                            throw new RuntimeException("wrong ADN format", ar4.exception);
                        }
                        this.mPendingExtLoads = 0;
                        this.mResult = null;
                    }
                    break;
                    break;
                case 5:
                    AsyncResult ar5 = (AsyncResult) msg.obj;
                    IccIoResult result = (IccIoResult) ar5.result;
                    if (ar5.exception != null) {
                        throw new RuntimeException("update EF adn record failed", ar5.exception);
                    }
                    IccException iccException = result.getException();
                    if (iccException != null) {
                        throw new RuntimeException("update EF adn record failed for sw", iccException);
                    }
                    this.mPendingExtLoads = 0;
                    this.mResult = null;
                    break;
                    break;
                case 101:
                    AsyncResult ar6 = (AsyncResult) msg.obj;
                    if (ar6.exception != null) {
                        throw new RuntimeException("update PHB EF record failed", ar6.exception);
                    }
                    this.mPendingExtLoads = 0;
                    this.mResult = null;
                    break;
                    break;
                case 102:
                    AsyncResult ar7 = (AsyncResult) msg.obj;
                    AdnRecord adn5 = (AdnRecord) ar7.userObj;
                    if (ar7.exception != null) {
                        throw new RuntimeException("PHB Verify PIN2 error", ar7.exception);
                    }
                    writeEntryToModem(adn5, getPhbStorageType(this.mEf));
                    this.mPendingExtLoads = 1;
                    break;
                    break;
                case EVENT_PHB_LOAD_DONE:
                    AsyncResult ar8 = (AsyncResult) msg.obj;
                    PhbEntry[] entries = (PhbEntry[]) ar8.result;
                    if (ar8.exception != null) {
                        throw new RuntimeException("PHB Read an entry Error", ar8.exception);
                    }
                    this.mResult = getAdnRecordFromPhbEntry(entries[0]);
                    this.mPendingExtLoads = 0;
                    break;
                    break;
                case 104:
                    AsyncResult ar9 = (AsyncResult) msg.obj;
                    int[] readInfo = (int[]) ar9.userObj;
                    PhbEntry[] entries2 = (PhbEntry[]) ar9.result;
                    if (ar9.exception != null) {
                        throw new RuntimeException("PHB Read Entries Error", ar9.exception);
                    }
                    for (PhbEntry phbEntry : entries2) {
                        AdnRecord adn6 = getAdnRecordFromPhbEntry(phbEntry);
                        if (adn6 == null) {
                            throw new RuntimeException("getAdnRecordFromPhbEntry return null", CommandException.fromRilErrno(2));
                        }
                        this.mAdns.set(adn6.mRecordNumber - 1, adn6);
                        readInfo[1] = readInfo[1] - 1;
                        Rlog.d(LOG_TAG, "Read entries: " + adn6);
                    }
                    readInfo[0] = readInfo[0] + 10;
                    if (readInfo[1] < 0) {
                        throw new RuntimeException("the read entries is not sync with query status: " + readInfo[1], CommandException.fromRilErrno(2));
                    }
                    if (readInfo[1] == 0 || readInfo[0] >= readInfo[2]) {
                        this.mResult = this.mAdns;
                        this.mPendingExtLoads = 0;
                    } else {
                        int type = getPhbStorageType(this.mEf);
                        readEntryFromModem(type, readInfo);
                    }
                    break;
                    break;
                case 105:
                    AsyncResult ar10 = (AsyncResult) msg.obj;
                    int[] info = (int[]) ar10.result;
                    if (ar10.exception != null) {
                        throw new RuntimeException("PHB Query Info Error", ar10.exception);
                    }
                    int type2 = getPhbStorageType(this.mEf);
                    int[] readInfo2 = {1, info[0], info[1]};
                    this.mAdns = new ArrayList<>(readInfo2[2]);
                    for (int i2 = 0; i2 < readInfo2[2]; i2++) {
                        this.mAdns.add(i2, new AdnRecord(this.mEf, i2 + 1, UsimPBMemInfo.STRING_NOT_SET, UsimPBMemInfo.STRING_NOT_SET));
                    }
                    readEntryFromModem(type2, readInfo2);
                    this.mPendingExtLoads = 1;
                    break;
                    break;
            }
            if (this.mUserResponse == null || this.mPendingExtLoads != 0 || this.mUserResponse.getTarget() == null) {
                return;
            }
            AsyncResult.forMessage(this.mUserResponse).result = this.mResult;
            this.mUserResponse.sendToTarget();
            this.mUserResponse = null;
        } catch (RuntimeException exc) {
            if (this.mUserResponse == null || this.mUserResponse.getTarget() == null) {
                return;
            }
            Rlog.w(LOG_TAG, "handleMessage RuntimeException: " + exc.getMessage());
            Rlog.w(LOG_TAG, "handleMessage RuntimeException: " + exc.getCause());
            if (exc.getCause() == null) {
                Rlog.d(LOG_TAG, "handleMessage Null RuntimeException");
                AsyncResult.forMessage(this.mUserResponse).exception = new CommandException(CommandException.Error.GENERIC_FAILURE);
            } else {
                AsyncResult.forMessage(this.mUserResponse).exception = exc.getCause();
            }
            this.mUserResponse.sendToTarget();
            this.mUserResponse = null;
        }
    }

    private void updatePhb(AdnRecord adn, int type) {
        if (this.mPin2 != null) {
            this.mFh.mCi.supplyIccPin2(this.mPin2, obtainMessage(102, adn));
        } else {
            writeEntryToModem(adn, type);
        }
    }

    private boolean canUseGsm7Bit(String alphaId) {
        return GsmAlphabet.countGsmSeptets(alphaId, true) != null;
    }

    private String encodeATUCS(String input) {
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

    private int getPhbStorageType(int ef) {
        switch (ef) {
            case 28474:
                return 0;
            case IccConstants.EF_FDN:
                return 1;
            default:
                return -1;
        }
    }

    private void writeEntryToModem(AdnRecord adn, int type) {
        boolean z = false;
        int ton = 129;
        String number = adn.getNumber();
        String alphaId = adn.getAlphaTag();
        if (number.indexOf(43) != -1) {
            if (number.indexOf(43) != number.lastIndexOf(43)) {
                Rlog.w(LOG_TAG, "There are multiple '+' in the number: " + number);
            }
            ton = 145;
            number = number.replace("+", UsimPBMemInfo.STRING_NOT_SET);
        }
        String number2 = number.replace('N', '?').replace(',', 'p').replace(';', 'w');
        String alphaId2 = encodeATUCS(alphaId);
        PhbEntry entry = new PhbEntry();
        if (number2.equals(UsimPBMemInfo.STRING_NOT_SET) && alphaId2.equals(UsimPBMemInfo.STRING_NOT_SET) && ton == 129) {
            z = true;
        }
        if (!z) {
            entry.type = type;
            entry.index = this.mRecordNumber;
            entry.number = number2;
            entry.ton = ton;
            entry.alphaId = alphaId2;
        } else {
            entry.type = type;
            entry.index = this.mRecordNumber;
            entry.number = null;
            entry.ton = ton;
            entry.alphaId = null;
        }
        this.mFh.mCi.writePhbEntry(entry, obtainMessage(101));
    }

    private void readEntryFromModem(int type, int[] readInfo) {
        if (readInfo.length != 3) {
            Rlog.e(LOG_TAG, "readEntryToModem, invalid paramters:" + readInfo.length);
            return;
        }
        int eIndex = (readInfo[0] + 10) - 1;
        if (eIndex > readInfo[2]) {
            eIndex = readInfo[2];
        }
        this.mFh.mCi.ReadPhbEntry(type, readInfo[0], eIndex, obtainMessage(104, readInfo));
    }

    private AdnRecord getAdnRecordFromPhbEntry(PhbEntry entry) {
        String number;
        Rlog.d(LOG_TAG, "Parse Adn entry :" + entry);
        byte[] ba = IccUtils.hexStringToBytes(entry.alphaId);
        if (ba == null) {
            Rlog.e(LOG_TAG, "entry.alphaId is null");
            return null;
        }
        try {
            String alphaId = new String(ba, 0, entry.alphaId.length() / 2, "utf-16be");
            if (entry.ton == 145) {
                number = PhoneNumberUtils.prependPlusToNumber(entry.number);
            } else {
                number = entry.number;
            }
            return new AdnRecord(this.mEf, entry.index, alphaId, number.replace('?', 'N').replace('p', ',').replace('w', ';'));
        } catch (UnsupportedEncodingException ex) {
            Rlog.e(LOG_TAG, "implausible UnsupportedEncodingException", ex);
            return null;
        }
    }
}
