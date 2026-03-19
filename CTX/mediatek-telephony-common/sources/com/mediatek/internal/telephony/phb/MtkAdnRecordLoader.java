package com.mediatek.internal.telephony.phb;

import android.hardware.radio.V1_0.LastCallFailCause;
import android.hardware.radio.V1_0.RadioError;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.uicc.AdnRecordLoader;
import com.android.internal.telephony.uicc.IccException;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.IccUtils;
import com.mediatek.internal.telephony.MtkPhoneNumberUtils;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class MtkAdnRecordLoader extends AdnRecordLoader {
    private static int ADN_FILE_SIZE = LastCallFailCause.RADIO_INTERNAL_ERROR;
    static final int EVENT_PHB_LOAD_ALL_DONE = 104;
    static final int EVENT_PHB_LOAD_DONE = 103;
    static final int EVENT_PHB_QUERY_STAUTS = 105;
    static final int EVENT_UPDATE_PHB_RECORD_DONE = 101;
    static final int EVENT_VERIFY_PIN2 = 102;
    static final String LOG_TAG = "MtkRecordLoader";
    private ArrayList<MtkAdnRecord> mAdns;

    MtkAdnRecordLoader(IccFileHandler iccFileHandler) {
        super(iccFileHandler);
    }

    public void loadFromEF(int i, int i2, int i3, Message message) {
        this.mEf = i;
        this.mExtensionEF = i2;
        this.mRecordNumber = i3;
        this.mUserResponse = message;
        int phbStorageType = getPhbStorageType(i);
        if (phbStorageType != -1) {
            this.mFh.mCi.readPhbEntry(phbStorageType, i3, i3, obtainMessage(EVENT_PHB_LOAD_DONE));
        } else {
            this.mFh.loadEFLinearFixed(i, getEFPath(i), i3, obtainMessage(1));
        }
    }

    public void loadAllFromEF(int i, int i2, Message message) {
        this.mEf = i;
        this.mExtensionEF = i2;
        this.mUserResponse = message;
        Rlog.i(LOG_TAG, "Usim :loadEFLinearFixedAll");
        int phbStorageType = getPhbStorageType(i);
        if (phbStorageType != -1) {
            this.mFh.mCi.queryPhbStorageInfo(phbStorageType, obtainMessage(105));
        } else {
            this.mFh.loadEFLinearFixedAll(i, getEFPath(i), obtainMessage(3));
        }
    }

    public void updateEF(MtkAdnRecord mtkAdnRecord, int i, int i2, int i3, String str, Message message) {
        this.mEf = i;
        this.mExtensionEF = i2;
        this.mRecordNumber = i3;
        this.mUserResponse = message;
        this.mPin2 = str;
        int phbStorageType = getPhbStorageType(i);
        if (phbStorageType != -1) {
            updatePhb(mtkAdnRecord, phbStorageType);
        } else {
            this.mFh.getEFLinearRecordSize(i, getEFPath(i), obtainMessage(4, mtkAdnRecord));
        }
    }

    public void handleMessage(Message message) {
        try {
            int i = message.what;
            int i2 = 0;
            switch (i) {
                case 1:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    byte[] bArr = (byte[]) asyncResult.result;
                    if (asyncResult.exception != null) {
                        throw new RuntimeException("load failed", asyncResult.exception);
                    }
                    MtkAdnRecord mtkAdnRecord = new MtkAdnRecord(this.mEf, this.mRecordNumber, bArr);
                    this.mResult = mtkAdnRecord;
                    if (mtkAdnRecord.hasExtendedRecord()) {
                        this.mPendingExtLoads = 1;
                        this.mFh.loadEFLinearFixed(this.mExtensionEF, mtkAdnRecord.mExtRecord, obtainMessage(2, mtkAdnRecord));
                    }
                    break;
                    break;
                case 2:
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    byte[] bArr2 = (byte[]) asyncResult2.result;
                    MtkAdnRecord mtkAdnRecord2 = (MtkAdnRecord) asyncResult2.userObj;
                    if (asyncResult2.exception == null) {
                        Rlog.d(LOG_TAG, "ADN extension EF: 0x" + Integer.toHexString(this.mExtensionEF) + ":" + mtkAdnRecord2.mExtRecord + "\n" + IccUtils.bytesToHexString(bArr2));
                        mtkAdnRecord2.appendExtRecord(bArr2);
                    } else {
                        Rlog.e(LOG_TAG, "Failed to read ext record. Clear the number now.");
                        mtkAdnRecord2.setNumber("");
                    }
                    this.mPendingExtLoads--;
                    break;
                case 3:
                    AsyncResult asyncResult3 = (AsyncResult) message.obj;
                    ArrayList arrayList = (ArrayList) asyncResult3.result;
                    if (asyncResult3.exception != null) {
                        throw new RuntimeException("load failed", asyncResult3.exception);
                    }
                    this.mAdns = new ArrayList<>(arrayList.size());
                    this.mResult = this.mAdns;
                    this.mPendingExtLoads = 0;
                    int size = arrayList.size();
                    while (i2 < size) {
                        int i3 = 1 + i2;
                        MtkAdnRecord mtkAdnRecord3 = new MtkAdnRecord(this.mEf, i3, (byte[]) arrayList.get(i2));
                        this.mAdns.add(mtkAdnRecord3);
                        if (mtkAdnRecord3.hasExtendedRecord()) {
                            this.mPendingExtLoads++;
                            this.mFh.loadEFLinearFixed(this.mExtensionEF, mtkAdnRecord3.mExtRecord, obtainMessage(2, mtkAdnRecord3));
                        }
                        i2 = i3;
                    }
                    break;
                    break;
                case 4:
                    AsyncResult asyncResult4 = (AsyncResult) message.obj;
                    MtkAdnRecord mtkAdnRecord4 = (MtkAdnRecord) asyncResult4.userObj;
                    if (asyncResult4.exception != null) {
                        throw new RuntimeException("get EF record size failed", asyncResult4.exception);
                    }
                    int[] iArr = (int[]) asyncResult4.result;
                    int i4 = this.mRecordNumber;
                    if (!CsimPhbUtil.hasModemPhbEnhanceCapability(this.mFh)) {
                        i4 = ((i4 - 1) % ADN_FILE_SIZE) + 1;
                    }
                    int i5 = i4;
                    Rlog.d(LOG_TAG, "[AdnRecordLoader] recordIndex :" + i5);
                    if (iArr.length != 3 || i5 > iArr[2]) {
                        throw new RuntimeException("get wrong EF record size format", asyncResult4.exception);
                    }
                    Rlog.d(LOG_TAG, "[AdnRecordLoader] EVENT_EF_LINEAR_RECORD_SIZE_DONE safe ");
                    Rlog.d(LOG_TAG, "in EVENT_EF_LINEAR_RECORD_SIZE_DONE,call adn.buildAdnString");
                    byte[] bArrBuildAdnString = mtkAdnRecord4.buildAdnString(iArr[0]);
                    if (bArrBuildAdnString == null) {
                        Rlog.d(LOG_TAG, "data is null");
                        int errorNumber = mtkAdnRecord4.getErrorNumber();
                        if (errorNumber == -1) {
                            throw new RuntimeException("data is null and DIAL_STRING_TOO_LONG", CommandException.fromRilErrno(RadioError.OEM_ERROR_1));
                        }
                        if (errorNumber == -2) {
                            throw new RuntimeException("data is null and TEXT_STRING_TOO_LONG", CommandException.fromRilErrno(RadioError.OEM_ERROR_2));
                        }
                        if (errorNumber == -15) {
                            throw new RuntimeException("wrong ADN format", asyncResult4.exception);
                        }
                        this.mPendingExtLoads = 0;
                        this.mResult = null;
                    } else {
                        this.mFh.updateEFLinearFixed(this.mEf, getEFPath(this.mEf), i5, bArrBuildAdnString, this.mPin2, obtainMessage(5));
                        this.mPendingExtLoads = 1;
                    }
                    break;
                    break;
                case 5:
                    AsyncResult asyncResult5 = (AsyncResult) message.obj;
                    IccIoResult iccIoResult = (IccIoResult) asyncResult5.result;
                    if (asyncResult5.exception != null) {
                        throw new RuntimeException("update EF adn record failed", asyncResult5.exception);
                    }
                    IccException exception = iccIoResult.getException();
                    if (exception != null) {
                        throw new RuntimeException("update EF adn record failed for sw", exception);
                    }
                    this.mPendingExtLoads = 0;
                    this.mResult = null;
                    break;
                    break;
                default:
                    switch (i) {
                        case 101:
                            AsyncResult asyncResult6 = (AsyncResult) message.obj;
                            if (asyncResult6.exception != null) {
                                throw new RuntimeException("update PHB EF record failed", asyncResult6.exception);
                            }
                            this.mPendingExtLoads = 0;
                            this.mResult = null;
                            break;
                            break;
                        case 102:
                            AsyncResult asyncResult7 = (AsyncResult) message.obj;
                            MtkAdnRecord mtkAdnRecord5 = (MtkAdnRecord) asyncResult7.userObj;
                            if (asyncResult7.exception != null) {
                                throw new RuntimeException("PHB Verify PIN2 error", asyncResult7.exception);
                            }
                            writeEntryToModem(mtkAdnRecord5, getPhbStorageType(this.mEf));
                            this.mPendingExtLoads = 1;
                            break;
                            break;
                        case EVENT_PHB_LOAD_DONE:
                            AsyncResult asyncResult8 = (AsyncResult) message.obj;
                            PhbEntry[] phbEntryArr = (PhbEntry[]) asyncResult8.result;
                            if (asyncResult8.exception != null) {
                                throw new RuntimeException("PHB Read an entry Error", asyncResult8.exception);
                            }
                            this.mResult = getAdnRecordFromPhbEntry(phbEntryArr[0]);
                            this.mPendingExtLoads = 0;
                            break;
                            break;
                        case 104:
                            AsyncResult asyncResult9 = (AsyncResult) message.obj;
                            int[] iArr2 = (int[]) asyncResult9.userObj;
                            PhbEntry[] phbEntryArr2 = (PhbEntry[]) asyncResult9.result;
                            if (asyncResult9.exception != null) {
                                throw new RuntimeException("PHB Read Entries Error", asyncResult9.exception);
                            }
                            for (PhbEntry phbEntry : phbEntryArr2) {
                                MtkAdnRecord adnRecordFromPhbEntry = getAdnRecordFromPhbEntry(phbEntry);
                                if (adnRecordFromPhbEntry != null) {
                                    this.mAdns.set(adnRecordFromPhbEntry.mRecordNumber - 1, adnRecordFromPhbEntry);
                                    iArr2[1] = iArr2[1] - 1;
                                    Rlog.d(LOG_TAG, "Read entries: " + adnRecordFromPhbEntry);
                                } else {
                                    throw new RuntimeException("getAdnRecordFromPhbEntry return null", CommandException.fromRilErrno(2));
                                }
                            }
                            iArr2[0] = iArr2[0] + 10;
                            if (iArr2[1] < 0) {
                                throw new RuntimeException("the read entries is not sync with query status: " + iArr2[1], CommandException.fromRilErrno(2));
                            }
                            if (iArr2[1] == 0 || iArr2[0] >= iArr2[2]) {
                                this.mResult = this.mAdns;
                                this.mPendingExtLoads = 0;
                            } else {
                                readEntryFromModem(getPhbStorageType(this.mEf), iArr2);
                            }
                            break;
                            break;
                        case 105:
                            AsyncResult asyncResult10 = (AsyncResult) message.obj;
                            int[] iArr3 = (int[]) asyncResult10.result;
                            if (asyncResult10.exception != null) {
                                throw new RuntimeException("PHB Query Info Error", asyncResult10.exception);
                            }
                            int phbStorageType = getPhbStorageType(this.mEf);
                            int[] iArr4 = {1, iArr3[0], iArr3[1]};
                            this.mAdns = new ArrayList<>(iArr4[2]);
                            while (i2 < iArr4[2]) {
                                int i6 = i2 + 1;
                                this.mAdns.add(i2, new MtkAdnRecord(this.mEf, i6, "", ""));
                                i2 = i6;
                            }
                            readEntryFromModem(phbStorageType, iArr4);
                            this.mPendingExtLoads = 1;
                            break;
                            break;
                    }
                    break;
            }
            if (this.mUserResponse != null && this.mPendingExtLoads == 0 && this.mUserResponse.getTarget() != null) {
                AsyncResult.forMessage(this.mUserResponse).result = this.mResult;
                this.mUserResponse.sendToTarget();
                this.mUserResponse = null;
            }
        } catch (RuntimeException e) {
            if (this.mUserResponse != null && this.mUserResponse.getTarget() != null) {
                Rlog.w(LOG_TAG, "handleMessage RuntimeException: " + e.getMessage());
                Rlog.w(LOG_TAG, "handleMessage RuntimeException: " + e.getCause());
                if (e.getCause() == null) {
                    Rlog.d(LOG_TAG, "handleMessage Null RuntimeException");
                    AsyncResult.forMessage(this.mUserResponse).exception = new CommandException(CommandException.Error.GENERIC_FAILURE);
                } else {
                    AsyncResult.forMessage(this.mUserResponse).exception = e.getCause();
                }
                this.mUserResponse.sendToTarget();
                this.mUserResponse = null;
            }
        }
    }

    private void updatePhb(MtkAdnRecord mtkAdnRecord, int i) {
        if (this.mPin2 != null) {
            this.mFh.mCi.supplyIccPin2(this.mPin2, obtainMessage(102, mtkAdnRecord));
        } else {
            writeEntryToModem(mtkAdnRecord, i);
        }
    }

    private boolean canUseGsm7Bit(String str) {
        return GsmAlphabet.countGsmSeptets(str, true) != null;
    }

    private String encodeATUCS(String str) {
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

    private int getPhbStorageType(int i) {
        switch (i) {
            case 28474:
                return 0;
            case 28475:
                return 1;
            default:
                return -1;
        }
    }

    private void writeEntryToModem(MtkAdnRecord mtkAdnRecord, int i) {
        int i2;
        String number = mtkAdnRecord.getNumber();
        String alphaTag = mtkAdnRecord.getAlphaTag();
        if (number.indexOf(43) != -1) {
            if (number.indexOf(43) != number.lastIndexOf(43)) {
                Rlog.w(LOG_TAG, "There are multiple '+' in the number: " + number);
            }
            i2 = 145;
            number = number.replace("+", "");
        } else {
            i2 = 129;
        }
        String strReplace = number.replace('N', '?').replace(',', 'p').replace(';', 'w');
        String strEncodeATUCS = encodeATUCS(alphaTag);
        PhbEntry phbEntry = new PhbEntry();
        if (!strReplace.equals("") || !strEncodeATUCS.equals("") || i2 != 129) {
            phbEntry.type = i;
            phbEntry.index = this.mRecordNumber;
            phbEntry.number = strReplace;
            phbEntry.ton = i2;
            phbEntry.alphaId = strEncodeATUCS;
        } else {
            phbEntry.type = i;
            phbEntry.index = this.mRecordNumber;
            phbEntry.number = null;
            phbEntry.ton = i2;
            phbEntry.alphaId = null;
        }
        this.mFh.mCi.writePhbEntry(phbEntry, obtainMessage(101));
    }

    private void readEntryFromModem(int i, int[] iArr) {
        if (iArr.length != 3) {
            Rlog.e(LOG_TAG, "readEntryToModem, invalid paramters:" + iArr.length);
            return;
        }
        int i2 = (iArr[0] + 10) - 1;
        if (i2 > iArr[2]) {
            i2 = iArr[2];
        }
        this.mFh.mCi.readPhbEntry(i, iArr[0], i2, obtainMessage(104, iArr));
    }

    private MtkAdnRecord getAdnRecordFromPhbEntry(PhbEntry phbEntry) {
        String strPrependPlusToNumber;
        Rlog.d(LOG_TAG, "Parse Adn entry :" + phbEntry);
        byte[] bArrHexStringToBytes = IccUtils.hexStringToBytes(phbEntry.alphaId);
        if (bArrHexStringToBytes == null) {
            Rlog.e(LOG_TAG, "entry.alphaId is null");
            return null;
        }
        try {
            String str = new String(bArrHexStringToBytes, 0, phbEntry.alphaId.length() / 2, "utf-16be");
            if (phbEntry.ton == 145) {
                strPrependPlusToNumber = MtkPhoneNumberUtils.prependPlusToNumber(phbEntry.number);
            } else {
                strPrependPlusToNumber = phbEntry.number;
            }
            return new MtkAdnRecord(this.mEf, phbEntry.index, str, strPrependPlusToNumber.replace('?', 'N').replace('p', ',').replace('w', ';'));
        } catch (UnsupportedEncodingException e) {
            Rlog.e(LOG_TAG, "implausible UnsupportedEncodingException", e);
            return null;
        }
    }
}
