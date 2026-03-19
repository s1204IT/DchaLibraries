package com.android.ims.internal.uce.presence;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.ims.internal.uce.common.CapInfo;

public class PresCapInfo implements Parcelable {
    public static final Parcelable.Creator<PresCapInfo> CREATOR = new Parcelable.Creator<PresCapInfo>() {
        @Override
        public PresCapInfo createFromParcel(Parcel parcel) {
            return new PresCapInfo(parcel);
        }

        @Override
        public PresCapInfo[] newArray(int i) {
            return new PresCapInfo[i];
        }
    };
    private CapInfo mCapInfo;
    private String mContactUri;

    public CapInfo getCapInfo() {
        return this.mCapInfo;
    }

    public void setCapInfo(CapInfo capInfo) {
        this.mCapInfo = capInfo;
    }

    public String getContactUri() {
        return this.mContactUri;
    }

    public void setContactUri(String str) {
        this.mContactUri = str;
    }

    public PresCapInfo() {
        this.mContactUri = "";
        this.mCapInfo = new CapInfo();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mContactUri);
        parcel.writeParcelable(this.mCapInfo, i);
    }

    private PresCapInfo(Parcel parcel) {
        this.mContactUri = "";
        readFromParcel(parcel);
    }

    public void readFromParcel(Parcel parcel) {
        this.mContactUri = parcel.readString();
        this.mCapInfo = (CapInfo) parcel.readParcelable(CapInfo.class.getClassLoader());
    }
}
