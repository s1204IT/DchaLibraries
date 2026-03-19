package com.android.server.wifi.wificond;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.util.Objects;

public class ChannelSettings implements Parcelable {
    public static final Parcelable.Creator<ChannelSettings> CREATOR = new Parcelable.Creator<ChannelSettings>() {
        @Override
        public ChannelSettings createFromParcel(Parcel parcel) {
            ChannelSettings channelSettings = new ChannelSettings();
            channelSettings.frequency = parcel.readInt();
            if (parcel.dataAvail() != 0) {
                Log.e(ChannelSettings.TAG, "Found trailing data after parcel parsing.");
            }
            return channelSettings;
        }

        @Override
        public ChannelSettings[] newArray(int i) {
            return new ChannelSettings[i];
        }
    };
    private static final String TAG = "ChannelSettings";
    public int frequency;

    public boolean equals(Object obj) {
        ChannelSettings channelSettings;
        if (this == obj) {
            return true;
        }
        return (obj instanceof ChannelSettings) && (channelSettings = (ChannelSettings) obj) != null && this.frequency == channelSettings.frequency;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.frequency));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.frequency);
    }
}
