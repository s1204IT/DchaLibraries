package com.android.ims.internal.uce.presence;

import android.os.Parcel;
import android.os.Parcelable;

public class PresServiceInfo implements Parcelable {
    public static final Parcelable.Creator<PresServiceInfo> CREATOR = new Parcelable.Creator<PresServiceInfo>() {
        @Override
        public PresServiceInfo createFromParcel(Parcel parcel) {
            return new PresServiceInfo(parcel);
        }

        @Override
        public PresServiceInfo[] newArray(int i) {
            return new PresServiceInfo[i];
        }
    };
    public static final int UCE_PRES_MEDIA_CAP_FULL_AUDIO_AND_VIDEO = 2;
    public static final int UCE_PRES_MEDIA_CAP_FULL_AUDIO_ONLY = 1;
    public static final int UCE_PRES_MEDIA_CAP_NONE = 0;
    public static final int UCE_PRES_MEDIA_CAP_UNKNOWN = 3;
    private int mMediaCap;
    private String mServiceDesc;
    private String mServiceID;
    private String mServiceVer;

    public int getMediaType() {
        return this.mMediaCap;
    }

    public void setMediaType(int i) {
        this.mMediaCap = i;
    }

    public String getServiceId() {
        return this.mServiceID;
    }

    public void setServiceId(String str) {
        this.mServiceID = str;
    }

    public String getServiceDesc() {
        return this.mServiceDesc;
    }

    public void setServiceDesc(String str) {
        this.mServiceDesc = str;
    }

    public String getServiceVer() {
        return this.mServiceVer;
    }

    public void setServiceVer(String str) {
        this.mServiceVer = str;
    }

    public PresServiceInfo() {
        this.mMediaCap = 0;
        this.mServiceID = "";
        this.mServiceDesc = "";
        this.mServiceVer = "";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mServiceID);
        parcel.writeString(this.mServiceDesc);
        parcel.writeString(this.mServiceVer);
        parcel.writeInt(this.mMediaCap);
    }

    private PresServiceInfo(Parcel parcel) {
        this.mMediaCap = 0;
        this.mServiceID = "";
        this.mServiceDesc = "";
        this.mServiceVer = "";
        readFromParcel(parcel);
    }

    public void readFromParcel(Parcel parcel) {
        this.mServiceID = parcel.readString();
        this.mServiceDesc = parcel.readString();
        this.mServiceVer = parcel.readString();
        this.mMediaCap = parcel.readInt();
    }
}
