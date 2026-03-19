package com.android.server.wifi.wificond;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.Objects;

public class HiddenNetwork implements Parcelable {
    public static final Parcelable.Creator<HiddenNetwork> CREATOR = new Parcelable.Creator<HiddenNetwork>() {
        @Override
        public HiddenNetwork createFromParcel(Parcel parcel) {
            HiddenNetwork hiddenNetwork = new HiddenNetwork();
            hiddenNetwork.ssid = parcel.createByteArray();
            return hiddenNetwork;
        }

        @Override
        public HiddenNetwork[] newArray(int i) {
            return new HiddenNetwork[i];
        }
    };
    private static final String TAG = "HiddenNetwork";
    public byte[] ssid;

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HiddenNetwork)) {
            return false;
        }
        return Arrays.equals(this.ssid, ((HiddenNetwork) obj).ssid);
    }

    public int hashCode() {
        return Objects.hash(this.ssid);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByteArray(this.ssid);
    }
}
