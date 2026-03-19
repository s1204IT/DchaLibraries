package com.mediatek.internal.telephony.phb;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class UsimGroup implements Parcelable {
    public static final Parcelable.Creator<UsimGroup> CREATOR = new Parcelable.Creator<UsimGroup>() {
        @Override
        public UsimGroup createFromParcel(Parcel parcel) {
            return new UsimGroup(parcel.readInt(), parcel.readString());
        }

        @Override
        public UsimGroup[] newArray(int i) {
            return new UsimGroup[i];
        }
    };
    static final String LOG_TAG = "UsimGroup";
    String mAlphaTag;
    int mRecordNumber;

    public UsimGroup(int i, String str) {
        this.mAlphaTag = null;
        this.mRecordNumber = i;
        this.mAlphaTag = str;
    }

    public int getRecordIndex() {
        return this.mRecordNumber;
    }

    public String getAlphaTag() {
        return this.mAlphaTag;
    }

    public void setRecordIndex(int i) {
        this.mRecordNumber = i;
    }

    public void setAlphaTag(String str) {
        this.mAlphaTag = str;
    }

    public String toString() {
        return "UsimGroup '" + this.mRecordNumber + "' '" + this.mAlphaTag + "' ";
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

    public boolean isEqual(UsimGroup usimGroup) {
        return stringCompareNullEqualsEmpty(this.mAlphaTag, usimGroup.mAlphaTag);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mRecordNumber);
        parcel.writeString(this.mAlphaTag);
    }
}
