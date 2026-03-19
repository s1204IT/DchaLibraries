package com.android.server.wifi.wificond;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.util.ArrayList;
import java.util.Objects;

public class SingleScanSettings implements Parcelable {
    public static final Parcelable.Creator<SingleScanSettings> CREATOR = new Parcelable.Creator<SingleScanSettings>() {
        @Override
        public SingleScanSettings createFromParcel(Parcel parcel) {
            SingleScanSettings singleScanSettings = new SingleScanSettings();
            singleScanSettings.scanType = parcel.readInt();
            if (!SingleScanSettings.isValidScanType(singleScanSettings.scanType)) {
                Log.wtf(SingleScanSettings.TAG, "Invalid scan type " + singleScanSettings.scanType);
            }
            singleScanSettings.channelSettings = new ArrayList<>();
            parcel.readTypedList(singleScanSettings.channelSettings, ChannelSettings.CREATOR);
            singleScanSettings.hiddenNetworks = new ArrayList<>();
            parcel.readTypedList(singleScanSettings.hiddenNetworks, HiddenNetwork.CREATOR);
            if (parcel.dataAvail() != 0) {
                Log.e(SingleScanSettings.TAG, "Found trailing data after parcel parsing.");
            }
            return singleScanSettings;
        }

        @Override
        public SingleScanSettings[] newArray(int i) {
            return new SingleScanSettings[i];
        }
    };
    private static final String TAG = "SingleScanSettings";
    public ArrayList<ChannelSettings> channelSettings;
    public ArrayList<HiddenNetwork> hiddenNetworks;
    public int scanType;

    public boolean equals(Object obj) {
        SingleScanSettings singleScanSettings;
        if (this == obj) {
            return true;
        }
        if ((obj instanceof SingleScanSettings) && (singleScanSettings = (SingleScanSettings) obj) != null) {
            return this.scanType == singleScanSettings.scanType && this.channelSettings.equals(singleScanSettings.channelSettings) && this.hiddenNetworks.equals(singleScanSettings.hiddenNetworks);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.scanType), this.channelSettings, this.hiddenNetworks);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private static boolean isValidScanType(int i) {
        return i == 0 || i == 1 || i == 2;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (!isValidScanType(this.scanType)) {
            Log.wtf(TAG, "Invalid scan type " + this.scanType);
        }
        parcel.writeInt(this.scanType);
        parcel.writeTypedList(this.channelSettings);
        parcel.writeTypedList(this.hiddenNetworks);
    }
}
