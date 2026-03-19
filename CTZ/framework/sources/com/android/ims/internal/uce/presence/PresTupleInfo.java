package com.android.ims.internal.uce.presence;

import android.os.Parcel;
import android.os.Parcelable;

public class PresTupleInfo implements Parcelable {
    public static final Parcelable.Creator<PresTupleInfo> CREATOR = new Parcelable.Creator<PresTupleInfo>() {
        @Override
        public PresTupleInfo createFromParcel(Parcel parcel) {
            return new PresTupleInfo(parcel);
        }

        @Override
        public PresTupleInfo[] newArray(int i) {
            return new PresTupleInfo[i];
        }
    };
    private String mContactUri;
    private String mFeatureTag;
    private String mTimestamp;

    public String getFeatureTag() {
        return this.mFeatureTag;
    }

    public void setFeatureTag(String str) {
        this.mFeatureTag = str;
    }

    public String getContactUri() {
        return this.mContactUri;
    }

    public void setContactUri(String str) {
        this.mContactUri = str;
    }

    public String getTimestamp() {
        return this.mTimestamp;
    }

    public void setTimestamp(String str) {
        this.mTimestamp = str;
    }

    public PresTupleInfo() {
        this.mFeatureTag = "";
        this.mContactUri = "";
        this.mTimestamp = "";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mFeatureTag);
        parcel.writeString(this.mContactUri);
        parcel.writeString(this.mTimestamp);
    }

    private PresTupleInfo(Parcel parcel) {
        this.mFeatureTag = "";
        this.mContactUri = "";
        this.mTimestamp = "";
        readFromParcel(parcel);
    }

    public void readFromParcel(Parcel parcel) {
        this.mFeatureTag = parcel.readString();
        this.mContactUri = parcel.readString();
        this.mTimestamp = parcel.readString();
    }
}
