package com.android.server.wifi.wificond;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.Objects;

public class PnoNetwork implements Parcelable {
    public static final Parcelable.Creator<PnoNetwork> CREATOR = new Parcelable.Creator<PnoNetwork>() {
        @Override
        public PnoNetwork createFromParcel(Parcel parcel) {
            PnoNetwork pnoNetwork = new PnoNetwork();
            pnoNetwork.isHidden = parcel.readInt() != 0;
            pnoNetwork.ssid = parcel.createByteArray();
            return pnoNetwork;
        }

        @Override
        public PnoNetwork[] newArray(int i) {
            return new PnoNetwork[i];
        }
    };
    public boolean isHidden;
    public byte[] ssid;

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PnoNetwork)) {
            return false;
        }
        PnoNetwork pnoNetwork = (PnoNetwork) obj;
        return Arrays.equals(this.ssid, pnoNetwork.ssid) && this.isHidden == pnoNetwork.isHidden;
    }

    public int hashCode() {
        return Objects.hash(Boolean.valueOf(this.isHidden), this.ssid);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.isHidden ? 1 : 0);
        parcel.writeByteArray(this.ssid);
    }
}
