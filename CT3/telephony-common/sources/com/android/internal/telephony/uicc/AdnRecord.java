package com.android.internal.telephony.uicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.cat.BipUtils;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

public class AdnRecord implements Parcelable {
    static final int ADN_BCD_NUMBER_LENGTH = 0;
    static final int ADN_CAPABILITY_ID = 12;
    static final int ADN_DIALING_NUMBER_END = 11;
    static final int ADN_DIALING_NUMBER_START = 2;
    static final int ADN_EXTENSION_ID = 13;
    static final int ADN_TON_AND_NPI = 1;
    public static final Parcelable.Creator<AdnRecord> CREATOR = new Parcelable.Creator<AdnRecord>() {
        @Override
        public AdnRecord createFromParcel(Parcel source) {
            int efid = source.readInt();
            int recordNumber = source.readInt();
            String alphaTag = source.readString();
            String number = source.readString();
            String[] emails = source.readStringArray();
            String anr = source.readString();
            String anr2 = source.readString();
            String anr3 = source.readString();
            String grpIds = source.readString();
            int aas = source.readInt();
            String sne = source.readString();
            AdnRecord adn = new AdnRecord(efid, recordNumber, alphaTag, number, anr, anr2, anr3, emails, grpIds);
            adn.setAasIndex(aas);
            adn.setSne(sne);
            return adn;
        }

        @Override
        public AdnRecord[] newArray(int size) {
            return new AdnRecord[size];
        }
    };
    static final int EXT_RECORD_LENGTH_BYTES = 13;
    static final int EXT_RECORD_TYPE_ADDITIONAL_DATA = 2;
    static final int EXT_RECORD_TYPE_MASK = 3;
    static final int FOOTER_SIZE_BYTES = 14;
    static final String LOG_TAG = "AdnRecord";
    static final int MAX_EXT_CALLED_PARTY_LENGTH = 10;
    static final int MAX_NUMBER_SIZE_BYTES = 11;
    private static final String SIM_NUM_PATTERN = "[+]?[[0-9][*#pw,;]]+[[0-9][*#pw,;]]*";
    int aas;
    String additionalNumber;
    String additionalNumber2;
    String additionalNumber3;
    String grpIds;
    String mAlphaTag;
    int mEfid;
    String[] mEmails;
    int mExtRecord;
    String mNumber;
    int mRecordNumber;
    int mResult;
    String sne;

    public AdnRecord(byte[] record) {
        this(0, 0, record);
    }

    public AdnRecord(int efid, int recordNumber, byte[] record) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.additionalNumber = null;
        this.additionalNumber2 = null;
        this.additionalNumber3 = null;
        this.mExtRecord = 255;
        this.aas = 0;
        this.sne = null;
        this.mResult = 1;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        parseRecord(record);
    }

    public AdnRecord(String alphaTag, String number) {
        this(0, 0, alphaTag, number);
    }

    public AdnRecord(String alphaTag, String number, String anr) {
        this(0, 0, alphaTag, number, anr);
    }

    public AdnRecord(String alphaTag, String number, String[] emails) {
        this(0, 0, alphaTag, number, emails);
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String[] emails) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.additionalNumber = null;
        this.additionalNumber2 = null;
        this.additionalNumber3 = null;
        this.mExtRecord = 255;
        this.aas = 0;
        this.sne = null;
        this.mResult = 1;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
        this.additionalNumber = UsimPBMemInfo.STRING_NOT_SET;
        this.additionalNumber2 = UsimPBMemInfo.STRING_NOT_SET;
        this.additionalNumber3 = UsimPBMemInfo.STRING_NOT_SET;
        this.grpIds = null;
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.additionalNumber = null;
        this.additionalNumber2 = null;
        this.additionalNumber3 = null;
        this.mExtRecord = 255;
        this.aas = 0;
        this.sne = null;
        this.mResult = 1;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = null;
        this.additionalNumber = UsimPBMemInfo.STRING_NOT_SET;
        this.additionalNumber2 = UsimPBMemInfo.STRING_NOT_SET;
        this.additionalNumber3 = UsimPBMemInfo.STRING_NOT_SET;
        this.grpIds = null;
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String anr) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.additionalNumber = null;
        this.additionalNumber2 = null;
        this.additionalNumber3 = null;
        this.mExtRecord = 255;
        this.aas = 0;
        this.sne = null;
        this.mResult = 1;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = null;
        this.additionalNumber = anr;
        this.additionalNumber2 = UsimPBMemInfo.STRING_NOT_SET;
        this.additionalNumber3 = UsimPBMemInfo.STRING_NOT_SET;
        this.grpIds = null;
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String anr, String[] emails, String grps) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.additionalNumber = null;
        this.additionalNumber2 = null;
        this.additionalNumber3 = null;
        this.mExtRecord = 255;
        this.aas = 0;
        this.sne = null;
        this.mResult = 1;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
        this.additionalNumber = anr;
        this.additionalNumber2 = UsimPBMemInfo.STRING_NOT_SET;
        this.additionalNumber3 = UsimPBMemInfo.STRING_NOT_SET;
        this.grpIds = grps;
    }

    public AdnRecord(int efid, int recordNumber, String alphaTag, String number, String anr, String anr2, String anr3, String[] emails, String grps) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.additionalNumber = null;
        this.additionalNumber2 = null;
        this.additionalNumber3 = null;
        this.mExtRecord = 255;
        this.aas = 0;
        this.sne = null;
        this.mResult = 1;
        this.mEfid = efid;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mNumber = number;
        this.mEmails = emails;
        this.additionalNumber = anr;
        this.additionalNumber2 = anr2;
        this.additionalNumber3 = anr3;
        this.grpIds = grps;
    }

    public int getRecId() {
        return this.mRecordNumber;
    }

    public String getAlphaTag() {
        return this.mAlphaTag;
    }

    public int getEfid() {
        return this.mEfid;
    }

    public String getNumber() {
        return this.mNumber;
    }

    public String getAdditionalNumber() {
        return this.additionalNumber;
    }

    public String getAdditionalNumber(int index) {
        if (index == 0) {
            String number = this.additionalNumber;
            return number;
        }
        if (index == 1) {
            String number2 = this.additionalNumber2;
            return number2;
        }
        if (index == 2) {
            String number3 = this.additionalNumber3;
            return number3;
        }
        Rlog.e(LOG_TAG, "getAdditionalNumber Error:" + index);
        return null;
    }

    public int getAasIndex() {
        return this.aas;
    }

    public String getSne() {
        return this.sne;
    }

    public String[] getEmails() {
        return this.mEmails;
    }

    public String getGrpIds() {
        return this.grpIds;
    }

    public void setNumber(String number) {
        this.mNumber = number;
    }

    public void setAnr(String anr) {
        this.additionalNumber = anr;
    }

    public void setAnr(String anr, int index) {
        if (index == 0) {
            this.additionalNumber = anr;
            return;
        }
        if (index == 1) {
            this.additionalNumber2 = anr;
        } else if (index == 2) {
            this.additionalNumber3 = anr;
        } else {
            Rlog.e(LOG_TAG, "setAnr Error:" + index);
        }
    }

    public void setAasIndex(int aas) {
        this.aas = aas;
    }

    public void setSne(String sne) {
        this.sne = sne;
    }

    public void setGrpIds(String grps) {
        this.grpIds = grps;
    }

    public void setEmails(String[] emails) {
        this.mEmails = emails;
    }

    public void setRecordIndex(int nIndex) {
        this.mRecordNumber = nIndex;
    }

    public String toString() {
        return "ADN Record:" + this.mRecordNumber + ",alphaTag:" + this.mAlphaTag + ",number:" + this.mNumber + ",anr:" + this.additionalNumber + ",anr2:" + this.additionalNumber2 + ",anr3:" + this.additionalNumber3 + ",aas:" + this.aas + ",emails:" + this.mEmails + ",grpIds:" + this.grpIds + ",sne:" + this.sne;
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(this.mAlphaTag) && TextUtils.isEmpty(this.mNumber) && TextUtils.isEmpty(this.additionalNumber) && this.mEmails == null;
    }

    public boolean hasExtendedRecord() {
        return (this.mExtRecord == 0 || this.mExtRecord == 255) ? false : true;
    }

    private static boolean stringCompareNullEqualsEmpty(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null) {
            s1 = UsimPBMemInfo.STRING_NOT_SET;
        }
        if (s2 == null) {
            s2 = UsimPBMemInfo.STRING_NOT_SET;
        }
        return s1.equals(s2);
    }

    public boolean isEqual(AdnRecord adn) {
        if (stringCompareNullEqualsEmpty(this.mAlphaTag, adn.mAlphaTag)) {
            return stringCompareNullEqualsEmpty(this.mNumber, adn.mNumber);
        }
        return false;
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
        dest.writeString(this.additionalNumber);
        dest.writeString(this.additionalNumber2);
        dest.writeString(this.additionalNumber3);
        dest.writeString(this.grpIds);
        dest.writeInt(this.aas);
        dest.writeString(this.sne);
    }

    public byte[] buildAdnString(int recordSize) {
        Rlog.i(LOG_TAG, "in BuildAdnString");
        int footerOffset = recordSize - 14;
        int alphaIdLength = 0;
        byte[] adnString = new byte[recordSize];
        for (int i = 0; i < recordSize; i++) {
            adnString[i] = -1;
        }
        if (isPhoneNumberInvaild(this.mNumber)) {
            Rlog.w(LOG_TAG, "[buildAdnString] invaild number");
            this.mResult = -15;
            return null;
        }
        if (TextUtils.isEmpty(this.mNumber)) {
            Rlog.w(LOG_TAG, "[buildAdnString] Empty dialing number");
            this.mResult = 1;
        } else {
            if (this.mNumber.length() > 20) {
                this.mResult = -1;
                Rlog.w(LOG_TAG, "[buildAdnString] Max length of dialing number is 20");
                return null;
            }
            if (this.mAlphaTag != null && this.mAlphaTag.length() > footerOffset) {
                this.mResult = -2;
                Rlog.w(LOG_TAG, "[buildAdnString] Max length of tag is " + footerOffset);
                return null;
            }
            this.mResult = 1;
            try {
                byte[] bcdNumber = PhoneNumberUtils.numberToCalledPartyBCD(this.mNumber);
                System.arraycopy(bcdNumber, 0, adnString, footerOffset + 1, bcdNumber.length);
                adnString[footerOffset + 0] = (byte) bcdNumber.length;
                adnString[footerOffset + 12] = -1;
                adnString[footerOffset + 13] = -1;
            } catch (RuntimeException e) {
                CommandException cmdEx = new CommandException(CommandException.Error.INVALID_PARAMETER);
                throw new RuntimeException("invalid number for BCD ", cmdEx);
            }
        }
        if (!TextUtils.isEmpty(this.mAlphaTag)) {
            if (isContainChineseChar(this.mAlphaTag)) {
                Rlog.i(LOG_TAG, "[buildAdnString] getBytes,alphaTag:" + this.mAlphaTag);
                try {
                    Rlog.i(LOG_TAG, "call getBytes");
                    byte[] byteTag = this.mAlphaTag.getBytes("utf-16be");
                    Rlog.i(LOG_TAG, "byteTag," + IccUtils.bytesToHexString(byteTag));
                    byte[] header = {BipUtils.TCP_STATUS_ESTABLISHED};
                    System.arraycopy(header, 0, adnString, 0, 1);
                    if (byteTag.length > adnString.length - 1) {
                        this.mResult = -2;
                        Rlog.w(LOG_TAG, "[buildAdnString] after getBytes byteTag.length:" + byteTag.length + " adnString.length:" + adnString.length);
                        return null;
                    }
                    System.arraycopy(byteTag, 0, adnString, 1, byteTag.length);
                    alphaIdLength = byteTag.length + 1;
                    Rlog.i(LOG_TAG, "arrarString" + IccUtils.bytesToHexString(adnString));
                } catch (UnsupportedEncodingException e2) {
                    Rlog.w(LOG_TAG, "[buildAdnString] getBytes exception");
                    return null;
                }
            } else {
                Rlog.i(LOG_TAG, "[buildAdnString] stringToGsm8BitPacked");
                byte[] byteTag2 = GsmAlphabet.stringToGsm8BitPacked(this.mAlphaTag);
                alphaIdLength = byteTag2.length;
                if (alphaIdLength > adnString.length) {
                    this.mResult = -2;
                    Rlog.w(LOG_TAG, "[buildAdnString] after stringToGsm8BitPacked byteTag.length:" + byteTag2.length + " adnString.length:" + adnString.length);
                    return null;
                }
                System.arraycopy(byteTag2, 0, adnString, 0, byteTag2.length);
            }
        }
        if (this.mAlphaTag == null || alphaIdLength <= footerOffset) {
            return adnString;
        }
        this.mResult = -2;
        Rlog.w(LOG_TAG, "[buildAdnString] Max length of tag is " + footerOffset + ",alphaIdLength:" + alphaIdLength);
        return null;
    }

    public int getErrorNumber() {
        return this.mResult;
    }

    public void appendExtRecord(byte[] extRecord) {
        try {
            if (extRecord.length != 13 || (extRecord[0] & 3) != 2 || (extRecord[1] & PplMessageManager.Type.INVALID) > 10) {
                return;
            }
            this.mNumber += PhoneNumberUtils.calledPartyBCDFragmentToString(extRecord, 2, extRecord[1] & PplMessageManager.Type.INVALID);
        } catch (RuntimeException ex) {
            Rlog.w(LOG_TAG, "Error parsing AdnRecord ext record", ex);
        }
    }

    private void parseRecord(byte[] record) {
        try {
            this.mAlphaTag = IccUtils.adnStringFieldToString(record, 0, record.length - 14);
            int footerOffset = record.length - 14;
            int numberLength = record[footerOffset] & PplMessageManager.Type.INVALID;
            if (numberLength > 11) {
                this.mNumber = UsimPBMemInfo.STRING_NOT_SET;
                return;
            }
            this.mNumber = PhoneNumberUtils.calledPartyBCDToString(record, footerOffset + 1, numberLength);
            this.mExtRecord = record[record.length - 1] & PplMessageManager.Type.INVALID;
            this.mEmails = null;
            this.additionalNumber = UsimPBMemInfo.STRING_NOT_SET;
            this.additionalNumber2 = UsimPBMemInfo.STRING_NOT_SET;
            this.additionalNumber3 = UsimPBMemInfo.STRING_NOT_SET;
            this.grpIds = null;
        } catch (RuntimeException ex) {
            Rlog.w(LOG_TAG, "Error parsing AdnRecord", ex);
            this.mNumber = UsimPBMemInfo.STRING_NOT_SET;
            this.mAlphaTag = UsimPBMemInfo.STRING_NOT_SET;
            this.mEmails = null;
            this.additionalNumber = UsimPBMemInfo.STRING_NOT_SET;
            this.additionalNumber2 = UsimPBMemInfo.STRING_NOT_SET;
            this.additionalNumber3 = UsimPBMemInfo.STRING_NOT_SET;
            this.grpIds = null;
        }
    }

    private boolean isContainChineseChar(String alphTag) {
        int length = alphTag.length();
        for (int i = 0; i < length; i++) {
            if (Pattern.matches("[一-龥]", alphTag.substring(i, i + 1))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPhoneNumberInvaild(String phoneNumber) {
        if (!TextUtils.isEmpty(phoneNumber)) {
            String tempPhoneNumber = PhoneNumberUtils.stripSeparators(phoneNumber);
            if (!Pattern.matches(SIM_NUM_PATTERN, PhoneNumberUtils.extractCLIRPortion(tempPhoneNumber))) {
                return true;
            }
            return false;
        }
        return false;
    }
}
