package com.android.ims.internal.uce.options;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.ims.internal.uce.common.CapInfo;

public class OptionsCapInfo implements Parcelable {
    public static final Parcelable.Creator<OptionsCapInfo> CREATOR = new Parcelable.Creator<OptionsCapInfo>() {
        @Override
        public OptionsCapInfo createFromParcel(Parcel parcel) {
            return new OptionsCapInfo(parcel);
        }

        @Override
        public OptionsCapInfo[] newArray(int i) {
            return new OptionsCapInfo[i];
        }
    };
    private CapInfo mCapInfo;
    private String mSdp;

    public static OptionsCapInfo getOptionsCapInfoInstance() {
        return new OptionsCapInfo();
    }

    public String getSdp() {
        return this.mSdp;
    }

    public void setSdp(String str) {
        this.mSdp = str;
    }

    public OptionsCapInfo() {
        this.mSdp = "";
        this.mCapInfo = new CapInfo();
    }

    public CapInfo getCapInfo() {
        return this.mCapInfo;
    }

    public void setCapInfo(CapInfo capInfo) {
        this.mCapInfo = capInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mSdp);
        parcel.writeParcelable(this.mCapInfo, i);
    }

    private OptionsCapInfo(Parcel parcel) {
        this.mSdp = "";
        readFromParcel(parcel);
    }

    public void readFromParcel(Parcel parcel) {
        this.mSdp = parcel.readString();
        this.mCapInfo = (CapInfo) parcel.readParcelable(CapInfo.class.getClassLoader());
    }
}
