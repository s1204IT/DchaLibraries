package com.android.server.wifi.wificond;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Objects;

public class PnoSettings implements Parcelable {
    public static final Parcelable.Creator<PnoSettings> CREATOR = new Parcelable.Creator<PnoSettings>() {
        @Override
        public PnoSettings createFromParcel(Parcel parcel) {
            PnoSettings pnoSettings = new PnoSettings();
            pnoSettings.intervalMs = parcel.readInt();
            pnoSettings.min2gRssi = parcel.readInt();
            pnoSettings.min5gRssi = parcel.readInt();
            pnoSettings.pnoNetworks = new ArrayList<>();
            parcel.readTypedList(pnoSettings.pnoNetworks, PnoNetwork.CREATOR);
            return pnoSettings;
        }

        @Override
        public PnoSettings[] newArray(int i) {
            return new PnoSettings[i];
        }
    };
    public int intervalMs;
    public int min2gRssi;
    public int min5gRssi;
    public ArrayList<PnoNetwork> pnoNetworks;

    public boolean equals(Object obj) {
        PnoSettings pnoSettings;
        if (this == obj) {
            return true;
        }
        if ((obj instanceof PnoSettings) && (pnoSettings = (PnoSettings) obj) != null) {
            return this.intervalMs == pnoSettings.intervalMs && this.min2gRssi == pnoSettings.min2gRssi && this.min5gRssi == pnoSettings.min5gRssi && this.pnoNetworks.equals(pnoSettings.pnoNetworks);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.intervalMs), Integer.valueOf(this.min2gRssi), Integer.valueOf(this.min5gRssi), this.pnoNetworks);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.intervalMs);
        parcel.writeInt(this.min2gRssi);
        parcel.writeInt(this.min5gRssi);
        parcel.writeTypedList(this.pnoNetworks);
    }
}
