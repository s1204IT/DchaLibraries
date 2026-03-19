package com.android.internal.telephony.uicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.GsmAlphabet;
import java.util.Arrays;

public class AdnRecord implements Parcelable {
    protected static final int ADN_BCD_NUMBER_LENGTH = 0;
    protected static final int ADN_CAPABILITY_ID = 12;
    protected static final int ADN_DIALING_NUMBER_END = 11;
    protected static final int ADN_DIALING_NUMBER_START = 2;
    protected static final int ADN_EXTENSION_ID = 13;
    protected static final int ADN_TON_AND_NPI = 1;
    public static final Parcelable.Creator<AdnRecord> CREATOR = new Parcelable.Creator<AdnRecord>() {
        @Override
        public AdnRecord createFromParcel(Parcel parcel) {
            return new AdnRecord(parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readStringArray());
        }

        @Override
        public AdnRecord[] newArray(int i) {
            return new AdnRecord[i];
        }
    };
    protected static final int EXT_RECORD_LENGTH_BYTES = 13;
    protected static final int EXT_RECORD_TYPE_ADDITIONAL_DATA = 2;
    protected static final int EXT_RECORD_TYPE_MASK = 3;
    protected static final int FOOTER_SIZE_BYTES = 14;
    static final String LOG_TAG = "AdnRecord";
    protected static final int MAX_EXT_CALLED_PARTY_LENGTH = 10;
    protected static final int MAX_NUMBER_SIZE_BYTES = 11;
    public String mAlphaTag;
    public int mEfid;
    public String[] mEmails;
    public int mExtRecord;
    public String mNumber;
    public int mRecordNumber;

    public AdnRecord(byte[] bArr) {
        this(0, 0, bArr);
    }

    public AdnRecord(int i, int i2, byte[] bArr) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.mExtRecord = 255;
        this.mEfid = i;
        this.mRecordNumber = i2;
        parseRecord(bArr);
    }

    public AdnRecord(String str, String str2) {
        this(0, 0, str, str2);
    }

    public AdnRecord(String str, String str2, String[] strArr) {
        this(0, 0, str, str2, strArr);
    }

    public AdnRecord(int i, int i2, String str, String str2, String[] strArr) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.mExtRecord = 255;
        this.mEfid = i;
        this.mRecordNumber = i2;
        this.mAlphaTag = str;
        this.mNumber = str2;
        this.mEmails = strArr;
    }

    public AdnRecord(int i, int i2, String str, String str2) {
        this.mAlphaTag = null;
        this.mNumber = null;
        this.mExtRecord = 255;
        this.mEfid = i;
        this.mRecordNumber = i2;
        this.mAlphaTag = str;
        this.mNumber = str2;
        this.mEmails = null;
    }

    public String getAlphaTag() {
        return this.mAlphaTag;
    }

    public int getEfid() {
        return this.mEfid;
    }

    public int getRecId() {
        return this.mRecordNumber;
    }

    public String getNumber() {
        return this.mNumber;
    }

    public void setNumber(String str) {
        this.mNumber = str;
    }

    public String[] getEmails() {
        return this.mEmails;
    }

    public void setEmails(String[] strArr) {
        this.mEmails = strArr;
    }

    public String toString() {
        return "ADN Record '" + this.mAlphaTag + "' '" + Rlog.pii(LOG_TAG, this.mNumber) + " " + Rlog.pii(LOG_TAG, this.mEmails) + "'";
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(this.mAlphaTag) && TextUtils.isEmpty(this.mNumber) && this.mEmails == null;
    }

    public boolean hasExtendedRecord() {
        return (this.mExtRecord == 0 || this.mExtRecord == 255) ? false : true;
    }

    protected static boolean stringCompareNullEqualsEmpty(String str, String str2) {
        if (str == str2) {
            return true;
        }
        if (str == null) {
            str = "";
        }
        if (str2 == null) {
            str2 = "";
        }
        return str.equals(str2);
    }

    public boolean isEqual(AdnRecord adnRecord) {
        return stringCompareNullEqualsEmpty(this.mAlphaTag, adnRecord.mAlphaTag) && stringCompareNullEqualsEmpty(this.mNumber, adnRecord.mNumber) && Arrays.equals(this.mEmails, adnRecord.mEmails);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mEfid);
        parcel.writeInt(this.mRecordNumber);
        parcel.writeString(this.mAlphaTag);
        parcel.writeString(this.mNumber);
        parcel.writeStringArray(this.mEmails);
    }

    public byte[] buildAdnString(int i) {
        int i2 = i - 14;
        byte[] bArr = new byte[i];
        for (int i3 = 0; i3 < i; i3++) {
            bArr[i3] = -1;
        }
        if (TextUtils.isEmpty(this.mNumber)) {
            Rlog.w(LOG_TAG, "[buildAdnString] Empty dialing number");
            return bArr;
        }
        if (this.mNumber.length() > 20) {
            Rlog.w(LOG_TAG, "[buildAdnString] Max length of dialing number is 20");
            return null;
        }
        byte[] bArrStringToGsm8BitPacked = !TextUtils.isEmpty(this.mAlphaTag) ? GsmAlphabet.stringToGsm8BitPacked(this.mAlphaTag) : new byte[0];
        if (bArrStringToGsm8BitPacked.length > i2) {
            Rlog.w(LOG_TAG, "[buildAdnString] Max length of tag is " + i2);
            return null;
        }
        byte[] bArrNumberToCalledPartyBCD = PhoneNumberUtils.numberToCalledPartyBCD(this.mNumber, 1);
        System.arraycopy(bArrNumberToCalledPartyBCD, 0, bArr, i2 + 1, bArrNumberToCalledPartyBCD.length);
        bArr[i2 + 0] = (byte) bArrNumberToCalledPartyBCD.length;
        bArr[i2 + 12] = -1;
        bArr[i2 + 13] = -1;
        if (bArrStringToGsm8BitPacked.length > 0) {
            System.arraycopy(bArrStringToGsm8BitPacked, 0, bArr, 0, bArrStringToGsm8BitPacked.length);
        }
        return bArr;
    }

    public void appendExtRecord(byte[] bArr) {
        try {
            if (bArr.length != 13 || (bArr[0] & 3) != 2 || (bArr[1] & 255) > 10) {
                return;
            }
            this.mNumber += PhoneNumberUtils.calledPartyBCDFragmentToString(bArr, 2, 255 & bArr[1], 1);
        } catch (RuntimeException e) {
            Rlog.w(LOG_TAG, "Error parsing AdnRecord ext record", e);
        }
    }

    private void parseRecord(byte[] bArr) {
        try {
            this.mAlphaTag = IccUtils.adnStringFieldToString(bArr, 0, bArr.length - 14);
            int length = bArr.length - 14;
            int i = bArr[length] & 255;
            if (i > 11) {
                this.mNumber = "";
                return;
            }
            this.mNumber = PhoneNumberUtils.calledPartyBCDToString(bArr, length + 1, i, 1);
            this.mExtRecord = bArr[bArr.length - 1] & 255;
            this.mEmails = null;
        } catch (RuntimeException e) {
            Rlog.w(LOG_TAG, "Error parsing AdnRecord", e);
            this.mNumber = "";
            this.mAlphaTag = "";
            this.mEmails = null;
        }
    }
}
