package com.mediatek.internal.telephony.uicc;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class UsimGroup implements Parcelable {
    public static final Parcelable.Creator<UsimGroup> CREATOR = new Parcelable.Creator<UsimGroup>() {
        @Override
        public UsimGroup createFromParcel(Parcel source) {
            int recordNumber = source.readInt();
            String alphaTag = source.readString();
            return new UsimGroup(recordNumber, alphaTag);
        }

        @Override
        public UsimGroup[] newArray(int size) {
            return new UsimGroup[size];
        }
    };
    static final String LOG_TAG = "UsimGroup";
    String mAlphaTag;
    int mRecordNumber;

    public UsimGroup(int recordNumber, String alphaTag) {
        this.mAlphaTag = null;
        this.mRecordNumber = recordNumber;
        this.mAlphaTag = alphaTag;
    }

    public int getRecordIndex() {
        return this.mRecordNumber;
    }

    public String getAlphaTag() {
        return this.mAlphaTag;
    }

    public void setRecordIndex(int nIndex) {
        this.mRecordNumber = nIndex;
    }

    public void setAlphaTag(String alphaString) {
        this.mAlphaTag = alphaString;
    }

    public String toString() {
        return "UsimGroup '" + this.mRecordNumber + "' '" + this.mAlphaTag + "' ";
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

    public boolean isEqual(UsimGroup uGas) {
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
    }
}
