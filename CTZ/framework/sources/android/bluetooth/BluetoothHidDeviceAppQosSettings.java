package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

public final class BluetoothHidDeviceAppQosSettings implements Parcelable {
    public static final Parcelable.Creator<BluetoothHidDeviceAppQosSettings> CREATOR = new Parcelable.Creator<BluetoothHidDeviceAppQosSettings>() {
        @Override
        public BluetoothHidDeviceAppQosSettings createFromParcel(Parcel parcel) {
            return new BluetoothHidDeviceAppQosSettings(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
        }

        @Override
        public BluetoothHidDeviceAppQosSettings[] newArray(int i) {
            return new BluetoothHidDeviceAppQosSettings[i];
        }
    };
    public static final int MAX = -1;
    public static final int SERVICE_BEST_EFFORT = 1;
    public static final int SERVICE_GUARANTEED = 2;
    public static final int SERVICE_NO_TRAFFIC = 0;
    private final int mDelayVariation;
    private final int mLatency;
    private final int mPeakBandwidth;
    private final int mServiceType;
    private final int mTokenBucketSize;
    private final int mTokenRate;

    public BluetoothHidDeviceAppQosSettings(int i, int i2, int i3, int i4, int i5, int i6) {
        this.mServiceType = i;
        this.mTokenRate = i2;
        this.mTokenBucketSize = i3;
        this.mPeakBandwidth = i4;
        this.mLatency = i5;
        this.mDelayVariation = i6;
    }

    public int getServiceType() {
        return this.mServiceType;
    }

    public int getTokenRate() {
        return this.mTokenRate;
    }

    public int getTokenBucketSize() {
        return this.mTokenBucketSize;
    }

    public int getPeakBandwidth() {
        return this.mPeakBandwidth;
    }

    public int getLatency() {
        return this.mLatency;
    }

    public int getDelayVariation() {
        return this.mDelayVariation;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mServiceType);
        parcel.writeInt(this.mTokenRate);
        parcel.writeInt(this.mTokenBucketSize);
        parcel.writeInt(this.mPeakBandwidth);
        parcel.writeInt(this.mLatency);
        parcel.writeInt(this.mDelayVariation);
    }
}
