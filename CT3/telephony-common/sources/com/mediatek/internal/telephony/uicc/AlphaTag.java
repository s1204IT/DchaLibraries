package com.mediatek.internal.telephony.uicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class AlphaTag implements Parcelable {
    public static final Parcelable.Creator<AlphaTag> CREATOR = new Parcelable.Creator<AlphaTag>() {
        @Override
        public AlphaTag createFromParcel(Parcel source) {
            int recordNumber = source.readInt();
            String alphaTag = source.readString();
            int pbrIndex = source.readInt();
            return new AlphaTag(recordNumber, alphaTag, pbrIndex);
        }

        @Override
        public AlphaTag[] newArray(int size) {
            return new AlphaTag[size];
        }
    };
    static final String LOG_TAG = "AlphaTag";
    String mAlphaTag;
    int mPbrIndex;
    int mRecordNumber;

    public AlphaTag(int recordNumber, String alphaTag, int pbr) {
        this.mAlphaTag = null;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
        this.mPbrIndex = pbr;
    }

    public int getRecordIndex() {
        return this.mRecordNumber;
    }

    public String getAlphaTag() {
        return this.mAlphaTag;
    }

    public int getPbrIndex() {
        return this.mPbrIndex;
    }

    public void setRecordIndex(int nIndex) {
        this.mRecordNumber = nIndex;
    }

    public void setAlphaTag(String alphaString) {
        this.mAlphaTag = alphaString;
    }

    public void setPbrIndex(int pbr) {
        this.mPbrIndex = pbr;
    }

    public String toString() {
        return "AlphaTag: '" + this.mRecordNumber + "' '" + this.mAlphaTag + "' '" + this.mPbrIndex + "'";
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(this.mAlphaTag);
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

    public boolean isEqual(AlphaTag uGas) {
        return stringCompareNullEqualsEmpty(this.mAlphaTag, uGas.mAlphaTag);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mRecordNumber);
        dest.writeString(this.mAlphaTag);
        dest.writeInt(this.mPbrIndex);
    }
}
