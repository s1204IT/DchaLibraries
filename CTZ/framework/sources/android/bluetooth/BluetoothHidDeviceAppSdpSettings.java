package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.EventLog;

public final class BluetoothHidDeviceAppSdpSettings implements Parcelable {
    public static final Parcelable.Creator<BluetoothHidDeviceAppSdpSettings> CREATOR = new Parcelable.Creator<BluetoothHidDeviceAppSdpSettings>() {
        @Override
        public BluetoothHidDeviceAppSdpSettings createFromParcel(Parcel parcel) {
            return new BluetoothHidDeviceAppSdpSettings(parcel.readString(), parcel.readString(), parcel.readString(), parcel.readByte(), parcel.createByteArray());
        }

        @Override
        public BluetoothHidDeviceAppSdpSettings[] newArray(int i) {
            return new BluetoothHidDeviceAppSdpSettings[i];
        }
    };
    private static final int MAX_DESCRIPTOR_SIZE = 2048;
    private final String mDescription;
    private final byte[] mDescriptors;
    private final String mName;
    private final String mProvider;
    private final byte mSubclass;

    public BluetoothHidDeviceAppSdpSettings(String str, String str2, String str3, byte b, byte[] bArr) {
        this.mName = str;
        this.mDescription = str2;
        this.mProvider = str3;
        this.mSubclass = b;
        if (bArr == null || bArr.length > 2048) {
            EventLog.writeEvent(1397638484, "119819889", -1, "");
            throw new IllegalArgumentException("descriptors must be not null and shorter than 2048");
        }
        this.mDescriptors = (byte[]) bArr.clone();
    }

    public String getName() {
        return this.mName;
    }

    public String getDescription() {
        return this.mDescription;
    }

    public String getProvider() {
        return this.mProvider;
    }

    public byte getSubclass() {
        return this.mSubclass;
    }

    public byte[] getDescriptors() {
        return this.mDescriptors;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mName);
        parcel.writeString(this.mDescription);
        parcel.writeString(this.mProvider);
        parcel.writeByte(this.mSubclass);
        parcel.writeByteArray(this.mDescriptors);
    }
}
