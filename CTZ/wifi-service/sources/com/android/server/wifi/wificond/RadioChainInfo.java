package com.android.server.wifi.wificond;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public class RadioChainInfo implements Parcelable {
    public static final Parcelable.Creator<RadioChainInfo> CREATOR = new Parcelable.Creator<RadioChainInfo>() {
        @Override
        public RadioChainInfo createFromParcel(Parcel parcel) {
            RadioChainInfo radioChainInfo = new RadioChainInfo();
            radioChainInfo.chainId = parcel.readInt();
            radioChainInfo.level = parcel.readInt();
            return radioChainInfo;
        }

        @Override
        public RadioChainInfo[] newArray(int i) {
            return new RadioChainInfo[i];
        }
    };
    private static final String TAG = "RadioChainInfo";
    public int chainId;
    public int level;

    public RadioChainInfo() {
    }

    public RadioChainInfo(int i, int i2) {
        this.chainId = i;
        this.level = i2;
    }

    public boolean equals(Object obj) {
        RadioChainInfo radioChainInfo;
        if (this == obj) {
            return true;
        }
        if ((obj instanceof RadioChainInfo) && (radioChainInfo = (RadioChainInfo) obj) != null) {
            return this.chainId == radioChainInfo.chainId && this.level == radioChainInfo.level;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.chainId), Integer.valueOf(this.level));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.chainId);
        parcel.writeInt(this.level);
    }
}
