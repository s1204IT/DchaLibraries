package com.android.internal.telephony.uicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.GsmAlphabet;
import java.io.UnsupportedEncodingException;

public class AdnRecord implements Parcelable {
    static final int ADN_BCD_NUMBER_LENGTH = 0;
    static final int ADN_CAPABILITY_ID = 12;
    public static final int ADN_DIALING_NUMBER_END = 11;
    public static final int ADN_DIALING_NUMBER_START = 2;
    static final int ADN_EXTENSION_ID = 13;
    static final int ADN_TON_AND_NPI = 1;
    public static final int ANR_ADN_RECORD_IDENTIFIER = 16;
    public static final int ANR_ADN_SFI = 15;
    public static final int ANR_BCD_NUMBER_LENGTH = 1;
    public static final int ANR_CAPABILITY_ID = 13;
    public static final int ANR_DIALING_NUMBER_END = 12;
    public static final int ANR_DIALING_NUMBER_START = 3;
    public static final int ANR_EXTENSION_ID = 14;
    public static final int ANR_TON_AND_NPI = 2;
    public static final int EXT_RECORD_LENGTH_BYTES = 13;
    public static final int EXT_RECORD_TYPE_ADDITIONAL_DATA = 2;
    public static final int EXT_RECORD_TYPE_MASK = 3;
    public static final int FOOTER_SIZE_BYTES = 14;
    public static final int MAX_EXT_CALLED_PARTY_LENGTH = 10;
    public static final int MAX_GROUP_SIZE_BYTES = 10;
    static final int MAX_NUMBER_SIZE_BYTES = 11;
    public int[] anrEfids;
    public int[] anrFileTypes;
    public int[] anrIndexes;
    public String[] anrs;
    public int[] emailEfids;
    public int[] emailFileTypes;
    public int[] emailIndexes;
    int extRecordAnr0;
    public int grpEfid;
    public byte[] grps;
    String mAlphaTag;
    int mEfid;
    public String[] mEmails;
    int mExtRecord;
    String mNumber;
    public int mRecordNumber;
    public int[] sneEfids;
    public int[] sneFileTypes;
    public int[] sneIndexes;
    public String[] snes;
    static final String LOG_TAG = "AdnRecord";
    private static String TAG = LOG_TAG;
    public static final Parcelable.Creator<AdnRecord> CREATOR = new Parcelable.Creator<AdnRecord>() {
        @Override
        public AdnRecord createFromParcel(Parcel source) {
            int efid = source.readInt();
            int recordNumber = source.readInt();
            String alphaTag = source.readString();
            String number = source.readString();
            String[] emails = source.readStringArray();
            int[] emailEfids = source.createIntArray();
            int[] emailIndexes = source.createIntArray();
            int[] emailFileTypes = source.createIntArray();
            String[] anrs = source.readStringArray();
            int[] anrEfids = source.createIntArray();
            int[] anrIndexes = source.createIntArray();
            int[] anrFileTypes = source.createIntArray();
            String[] snes = source.readStringArray();
            int[] sneEfids = source.createIntArray();
            int[] sneIndexes = source.createIntArray();
            int[] sneFileTypes = source.createIntArray();
            byte[] grps = source.createByteArray();
            int grpEfid = source.readInt();
            return new AdnRecord(efid, recordNumber, alphaTag, number, emails, anrs, snes, emailEfids, anrEfids, sneEfids, emailIndexes, anrIndexes, sneIndexes, emailFileTypes, anrFileTypes, sneFileTypes, grps, grpEfid);
        }

        @Override
        public AdnRecord[] newArray(int size) {
            return new AdnRecord[size];
        }
    };

    public AdnRecord(byte[] record) {
        this(0, 0, record);
    }

    public AdnRecord(int efid, int recordNumber, byte[] record) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.mExtRecord = 255;
        this.extRecordAnr0 = 255;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        parseRecord(record);
    }

    public AdnRecord(String alphaTag, String number) {
        this(0, 0, alphaTag, number, (String[]) null);
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.mExtRecord = 255;
        this.extRecordAnr0 = 255;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = null;
        this.anrs = null;
    }

    public AdnRecord(String alphaTag, String number, String[] emails) {
        this(0, 0, alphaTag, number, emails);
    }

    public AdnRecord(String alphaTag, String number, String[] emails, String[] anrs) {
        this(0, 0, alphaTag, number, emails, anrs);
    }

    public AdnRecord(String alphaTag, String number, String[] emails, String[] anrs, byte[] grps) {
        this(0, 0, alphaTag, number, emails, anrs, grps);
    }

    public AdnRecord(String alphaTag, String number, String[] emails, String[] anrs, String[] snes, byte[] grps) {
        this(0, 0, alphaTag, number, emails, anrs, snes, grps);
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String[] emails) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.mExtRecord = 255;
        this.extRecordAnr0 = 255;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String[] emails, String[] anrs) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.mExtRecord = 255;
        this.extRecordAnr0 = 255;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
        this.anrs = anrs;
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String[] emails, String[] anrs, byte[] grps) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.mExtRecord = 255;
        this.extRecordAnr0 = 255;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
        this.anrs = anrs;
        this.grps = grps;
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String[] emails, String[] anrs, String[] snes, byte[] grps) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.mExtRecord = 255;
        this.extRecordAnr0 = 255;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
        this.anrs = anrs;
        this.snes = snes;
        this.grps = grps;
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String[] emails, int[] emailEfids, int[] emailIndexes, int[] emailFileTypes) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.mExtRecord = 255;
        this.extRecordAnr0 = 255;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
        this.emailEfids = emailEfids;
        this.emailIndexes = emailIndexes;
        this.emailFileTypes = emailFileTypes;
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String[] emails, String[] anrs, int[] emailEfids, int[] anrEfids, int[] emailIndexes, int[] anrIndexes, int[] emailFileTypes, int[] anrFileTypes) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.mExtRecord = 255;
        this.extRecordAnr0 = 255;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
        this.anrs = anrs;
        this.emailEfids = emailEfids;
        this.anrEfids = anrEfids;
        this.emailIndexes = emailIndexes;
        this.anrIndexes = anrIndexes;
        this.emailFileTypes = emailFileTypes;
        this.anrFileTypes = anrFileTypes;
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String[] emails, String[] anrs, int[] emailEfids, int[] anrEfids, int[] emailIndexes, int[] anrIndexes, int[] emailFileTypes, int[] anrFileTypes, byte[] grps, int grpEfid) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.mExtRecord = 255;
        this.extRecordAnr0 = 255;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
        this.anrs = anrs;
        this.emailEfids = emailEfids;
        this.anrEfids = anrEfids;
        this.emailIndexes = emailIndexes;
        this.anrIndexes = anrIndexes;
        this.emailFileTypes = emailFileTypes;
        this.anrFileTypes = anrFileTypes;
        this.grps = grps;
        this.grpEfid = grpEfid;
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String[] emails, String[] anrs, String[] snes, int[] emailEfids, int[] anrEfids, int[] sneEfids, int[] emailIndexes, int[] anrIndexes, int[] sneIndexes, int[] emailFileTypes, int[] anrFileTypes, int[] sneFileTypes, byte[] grps, int grpEfid) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.mExtRecord = 255;
        this.extRecordAnr0 = 255;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
        this.anrs = anrs;
        this.snes = snes;
        this.emailEfids = emailEfids;
        this.anrEfids = anrEfids;
        this.sneEfids = sneEfids;
        this.emailIndexes = emailIndexes;
        this.anrIndexes = anrIndexes;
        this.sneIndexes = sneIndexes;
        this.emailFileTypes = emailFileTypes;
        this.anrFileTypes = anrFileTypes;
        this.sneFileTypes = sneFileTypes;
        this.grps = grps;
        this.grpEfid = grpEfid;
    }

    public String getAlphaTag() {
        return this.mAlphaTag;
    }

    public String getNumber() {
        return this.mNumber;
    }

    public String[] getEmails() {
        return this.mEmails;
    }

    public String[] getAnrs() {
        return this.anrs;
    }

    public String[] getSnes() {
        return this.snes;
    }

    public int[] getEmailEfids() {
        return this.emailEfids;
    }

    public int[] getAnrEfids() {
        return this.anrEfids;
    }

    public int[] getSneEfids() {
        return this.sneEfids;
    }

    public byte[] getGrps() {
        return this.grps;
    }

    public int getGrpEfid() {
        return this.grpEfid;
    }

    public int getExtRecordAnr0() {
        return this.extRecordAnr0;
    }

    public void setEmails(String[] emails) {
        this.mEmails = emails;
    }

    public void setAnrs(String[] anrs) {
        this.anrs = anrs;
    }

    public void setSnes(String[] snes) {
        this.snes = snes;
    }

    public void setEmailEfids(int[] emailEfids) {
        this.emailEfids = emailEfids;
    }

    public void setAnrEfids(int[] anrEfids) {
        this.anrEfids = anrEfids;
    }

    public void setSneEfids(int[] sneEfids) {
        this.sneEfids = sneEfids;
    }

    public void setEmailIndexes(int[] emailIndexes) {
        this.emailIndexes = emailIndexes;
    }

    public void setAnrIndexes(int[] anrIndexes) {
        this.anrIndexes = anrIndexes;
    }

    public void setSneIndexes(int[] sneIndexes) {
        this.sneIndexes = sneIndexes;
    }

    public void setExtRecordAnr0(int extRecordAnr0) {
        this.extRecordAnr0 = extRecordAnr0;
    }

    public void setEmailFileTypes(int[] emailFileTypes) {
        this.emailFileTypes = emailFileTypes;
    }

    public void setAnrFileTypes(int[] anrFileTypes) {
        this.anrFileTypes = anrFileTypes;
    }

    public void setSneFileTypes(int[] sneFileTypes) {
        this.sneFileTypes = sneFileTypes;
    }

    public void setGrps(byte[] grps) {
        this.grps = grps;
    }

    public void setGrpEfid(int grpEfid) {
        this.grpEfid = grpEfid;
    }

    public String toString() {
        StringBuilder output = new StringBuilder("ADN Record: ");
        output.append("alphaTag = [" + this.mAlphaTag + "],");
        output.append("number = [" + this.mNumber + "],");
        if (this.mEmails != null) {
            String[] arr$ = this.mEmails;
            for (String email : arr$) {
                output.append("emails = [" + email + "],");
            }
        } else {
            output.append("emails is null,");
        }
        if (this.anrs != null) {
            String[] arr$2 = this.anrs;
            for (String anr : arr$2) {
                output.append("anrs = [" + anr + "],");
            }
        } else {
            output.append("anrs is null,");
        }
        if (this.snes != null) {
            String[] arr$3 = this.snes;
            for (String sne : arr$3) {
                output.append("snes = [" + sne + "],");
            }
        } else {
            output.append("snes is null,");
        }
        if (this.grps != null) {
            byte[] arr$4 = this.grps;
            for (byte grp : arr$4) {
                output.append("grps = [" + ((int) grp) + "],");
            }
        } else {
            output.append("grps is null,");
        }
        return output.toString();
    }

    public boolean isStringArrayEmpty(String[] strs) {
        if (strs == null || strs.length == 0) {
            return true;
        }
        for (String s : strs) {
            if (s != null && !s.equals("")) {
                return false;
            }
        }
        return true;
    }

    public boolean isbyteArrayEmpty(byte[] bytes) {
        return bytes == null || bytes.length == 0;
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(this.mAlphaTag) && TextUtils.isEmpty(this.mNumber) && isStringArrayEmpty(this.mEmails) && isStringArrayEmpty(this.anrs) && isStringArrayEmpty(this.snes);
    }

    public boolean hasExtendedRecord() {
        return (this.mExtRecord == 0 || this.mExtRecord == 255) ? false : true;
    }

    public boolean hasExtendedRecordAnr0() {
        return (this.extRecordAnr0 == 0 || this.extRecordAnr0 == 255) ? false : true;
    }

    private static boolean stringCompareNullEqualsEmpty(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null) {
            s1 = "";
        }
        if (s2 == null) {
            s2 = "";
        }
        return s1.trim().equals(s2.trim());
    }

    private boolean stringArrayCompareNullEqualsEmpty(String[] ss1, String[] ss2) {
        int index2;
        int index1;
        if ((isStringArrayEmpty(ss1) && !isStringArrayEmpty(ss2)) || (!isStringArrayEmpty(ss1) && isStringArrayEmpty(ss2))) {
            return false;
        }
        if (isStringArrayEmpty(ss1) && isStringArrayEmpty(ss2)) {
            return true;
        }
        String[] ssc1 = new String[ss1.length];
        String[] ssc2 = new String[ss2.length];
        int len$ = ss1.length;
        int i$ = 0;
        int index12 = 0;
        while (i$ < len$) {
            String s = ss1[i$];
            if (s.isEmpty()) {
                index1 = index12;
            } else {
                index1 = index12 + 1;
                ssc1[index12] = s;
            }
            i$++;
            index12 = index1;
        }
        int len$2 = ss2.length;
        int i$2 = 0;
        int index22 = 0;
        while (i$2 < len$2) {
            String s2 = ss2[i$2];
            if (s2.isEmpty()) {
                index2 = index22;
            } else {
                index2 = index22 + 1;
                ssc2[index22] = s2;
            }
            i$2++;
            index22 = index2;
        }
        if (index12 != index22) {
            return false;
        }
        for (int i = 0; i < index12; i++) {
            if (!stringCompareNullEqualsEmpty(ssc1[i], ssc2[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean byteCompareNullEqualsEmpty(byte b1, byte b2) {
        return b1 == b2;
    }

    private boolean byteArrayCompareNullEqualsEmpty(byte[] bytes1, byte[] bytes2) {
        if ((isbyteArrayEmpty(bytes1) && !isbyteArrayEmpty(bytes2)) || (!isbyteArrayEmpty(bytes1) && isbyteArrayEmpty(bytes2))) {
            return false;
        }
        if (isbyteArrayEmpty(bytes1) && isbyteArrayEmpty(bytes2)) {
            return true;
        }
        if (bytes1.length != bytes2.length) {
            return false;
        }
        byte[] bytes12 = bubblesort(bytes1);
        byte[] bytes22 = bubblesort(bytes2);
        for (int i = 0; i < bytes12.length; i++) {
            if (!byteCompareNullEqualsEmpty(bytes12[i], bytes22[i])) {
                return false;
            }
        }
        return true;
    }

    static byte[] bubblesort(byte[] a) {
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < (a.length - i) - 1; j++) {
                if (a[j] > a[j + 1]) {
                    byte temp = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = temp;
                }
            }
        }
        return a;
    }

    public boolean isEqual(AdnRecord adn) {
        return stringCompareNullEqualsEmpty(this.mAlphaTag, adn.mAlphaTag) && stringCompareNullEqualsEmpty(this.mNumber, adn.mNumber) && stringArrayCompareNullEqualsEmpty(this.mEmails, adn.mEmails) && stringArrayCompareNullEqualsEmpty(this.anrs, adn.anrs);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mEfid);
        dest.writeInt(this.mRecordNumber);
        dest.writeString(this.mAlphaTag);
        dest.writeString(this.mNumber);
        dest.writeStringArray(this.mEmails);
        dest.writeIntArray(this.emailEfids);
        dest.writeIntArray(this.emailIndexes);
        dest.writeIntArray(this.emailFileTypes);
        dest.writeStringArray(this.anrs);
        dest.writeIntArray(this.anrEfids);
        dest.writeIntArray(this.anrIndexes);
        dest.writeIntArray(this.anrFileTypes);
        dest.writeStringArray(this.snes);
        dest.writeIntArray(this.sneEfids);
        dest.writeIntArray(this.sneIndexes);
        dest.writeIntArray(this.sneFileTypes);
        dest.writeByteArray(this.grps);
        dest.writeInt(this.grpEfid);
    }

    public byte[] buildAdnString(int recordSize, int adnExt1Id) {
        byte[] adnString = buildAdnString(recordSize);
        int footerOffset = recordSize - 14;
        adnString[footerOffset + 13] = (byte) adnExt1Id;
        return adnString;
    }

    public byte[] buildAnrString(int recordSize, String anr, int anrExt1Id, int adnIndex, int extRecordAnr0) {
        byte[] anrString = buildAnrString(recordSize, anr, anrExt1Id, adnIndex);
        anrString[14] = (byte) (extRecordAnr0 & 255);
        return anrString;
    }

    public byte[] buildAnrString(int recordSize, String anr, int anrExt1Id, int adnIndex) {
        byte[] anrString = new byte[recordSize];
        for (int i = 0; i < recordSize; i++) {
            anrString[i] = -1;
        }
        if (!anr.isEmpty()) {
            byte[] bcdAnr = PhoneNumberUtils.numberToCalledPartyBCD(anr);
            System.arraycopy(bcdAnr, 0, anrString, 2, bcdAnr.length);
            anrString[0] = 0;
            anrString[1] = (byte) bcdAnr.length;
            if (recordSize >= 17) {
                anrString[16] = (byte) adnIndex;
            }
        }
        return anrString;
    }

    public byte[] buildAdnString(int recordSize) {
        byte[] bcdNumber;
        int footerOffset = recordSize - 14;
        byte[] adnString = new byte[recordSize];
        for (int i = 0; i < recordSize; i++) {
            adnString[i] = -1;
        }
        if (this.mNumber != null && this.mNumber.length() > 20) {
            Rlog.w(LOG_TAG, "[buildAdnString] Max length of dialing number is 20");
            return null;
        }
        if (this.mAlphaTag != null && this.mAlphaTag.length() > footerOffset) {
            Rlog.w(LOG_TAG, "[buildAdnString] Max length of tag is " + footerOffset);
            return null;
        }
        if (TextUtils.isEmpty(this.mNumber)) {
            Rlog.w(LOG_TAG, "[buildAdnString] Empty dialing number");
            bcdNumber = new byte[]{-1};
            adnString[footerOffset + 0] = -1;
        } else {
            bcdNumber = PhoneNumberUtils.numberToCalledPartyBCD(this.mNumber);
            adnString[footerOffset + 0] = (byte) bcdNumber.length;
        }
        System.arraycopy(bcdNumber, 0, adnString, footerOffset + 1, bcdNumber.length);
        adnString[footerOffset + 12] = -1;
        adnString[footerOffset + 13] = -1;
        if (!TextUtils.isEmpty(this.mAlphaTag)) {
            if (this.mAlphaTag.getBytes().length != this.mAlphaTag.length()) {
                byte[] byteTag = gbEncoding(this.mAlphaTag);
                if (byteTag.length > footerOffset) {
                    if (footerOffset % 2 != 1) {
                        footerOffset--;
                    }
                    System.arraycopy(byteTag, 0, adnString, 0, footerOffset);
                    return adnString;
                }
                System.arraycopy(byteTag, 0, adnString, 0, byteTag.length);
                return adnString;
            }
            byte[] byteTag2 = GsmAlphabet.stringToGsm8BitPacked(this.mAlphaTag);
            System.arraycopy(byteTag2, 0, adnString, 0, byteTag2.length);
            return adnString;
        }
        return adnString;
    }

    public byte[] buildSneOrGsdString(int recordSize, String str) {
        byte[] retString = new byte[recordSize];
        for (int i = 0; i < recordSize; i++) {
            retString[i] = -1;
        }
        if (!TextUtils.isEmpty(str)) {
            if (str.getBytes().length != str.length()) {
                byte[] byteStr = gbEncoding(str);
                if (byteStr.length > recordSize) {
                    if (recordSize % 2 != 1) {
                        recordSize--;
                    }
                    System.arraycopy(byteStr, 0, retString, 0, recordSize);
                } else {
                    System.arraycopy(byteStr, 0, retString, 0, byteStr.length);
                }
            } else {
                byte[] byteStr2 = GsmAlphabet.stringToGsm8BitPacked(this.mAlphaTag);
                System.arraycopy(byteStr2, 0, retString, 0, byteStr2.length);
            }
        }
        return retString;
    }

    private byte[] gbEncoding(String gbString) {
        byte[] result = null;
        try {
            byte[] byteTag = gbString.getBytes("utf-16BE");
            result = new byte[byteTag.length + 1];
            result[0] = -128;
            System.arraycopy(byteTag, 0, result, 1, byteTag.length);
            return result;
        } catch (UnsupportedEncodingException e) {
            Rlog.e(LOG_TAG, "alphaTag convert byte exception");
            return result;
        }
    }

    public void appendExtRecord(byte[] extRecord) {
        if (extRecord != null) {
            try {
                if (extRecord.length == 13 && (extRecord[0] & 3) == 2 && (extRecord[1] & 255) <= 10) {
                    this.mNumber += PhoneNumberUtils.calledPartyBCDFragmentToString(extRecord, 2, extRecord[1] & 255);
                }
            } catch (RuntimeException ex) {
                Rlog.w(LOG_TAG, "Error parsing AdnRecord ext record", ex);
            }
        }
    }

    private void parseRecord(byte[] record) {
        try {
            this.mAlphaTag = IccUtils.adnStringFieldToString(record, 0, record.length - 14);
            int footerOffset = record.length - 14;
            int numberLength = record[footerOffset] & 255;
            if (numberLength > 11) {
                this.mNumber = "";
            } else {
                this.mNumber = PhoneNumberUtils.calledPartyBCDToString(record, footerOffset + 1, numberLength);
                this.mExtRecord = record[record.length - 1] & 255;
                this.mEmails = null;
                this.emailEfids = null;
                this.emailIndexes = null;
                this.emailFileTypes = null;
                this.anrs = null;
                this.anrEfids = null;
                this.anrIndexes = null;
                this.anrFileTypes = null;
                this.snes = null;
                this.sneEfids = null;
                this.sneIndexes = null;
                this.sneFileTypes = null;
            }
        } catch (RuntimeException ex) {
            Rlog.w(LOG_TAG, "Error parsing AdnRecord", ex);
            this.mNumber = "";
            this.mAlphaTag = "";
            this.mEmails = null;
            this.emailEfids = null;
            this.emailIndexes = null;
            this.emailFileTypes = null;
            this.anrs = null;
            this.anrEfids = null;
            this.anrIndexes = null;
            this.anrFileTypes = null;
            this.snes = null;
            this.sneEfids = null;
            this.sneIndexes = null;
            this.sneFileTypes = null;
        }
    }
}
