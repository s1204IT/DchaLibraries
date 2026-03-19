package com.mediatek.internal.telephony.phb;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class AlphaTag implements Parcelable {
    public static final Parcelable.Creator<AlphaTag> CREATOR = new Parcelable.Creator<AlphaTag>() {
        @Override
        public AlphaTag createFromParcel(Parcel parcel) {
            return new AlphaTag(parcel.readInt(), parcel.readString(), parcel.readInt());
        }

        @Override
        public AlphaTag[] newArray(int i) {
            return new AlphaTag[i];
        }
    };
    static final String LOG_TAG = "AlphaTag";
    String mAlphaTag;
    int mPbrIndex;
    int mRecordNumber;

    public AlphaTag(int i, String str, int i2) {
        this.mAlphaTag = null;
        this.mRecordNumber = i;
        this.mAlphaTag = str;
        this.mPbrIndex = i2;
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

    public void setRecordIndex(int i) {
        this.mRecordNumber = i;
    }

    public void setAlphaTag(String str) {
        this.mAlphaTag = str;
    }

    public void setPbrIndex(int i) {
        this.mPbrIndex = i;
    }

    public String toString() {
        return "AlphaTag: '" + this.mRecordNumber + "' '" + this.mAlphaTag + "' '" + this.mPbrIndex + "'";
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(this.mAlphaTag);
    }

    private static boolean stringCompareNullEqualsEmpty(String str, String str2) {
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

    public boolean isEqual(AlphaTag alphaTag) {
        return stringCompareNullEqualsEmpty(this.mAlphaTag, alphaTag.mAlphaTag);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mRecordNumber);
        parcel.writeString(this.mAlphaTag);
        parcel.writeInt(this.mPbrIndex);
    }
}
