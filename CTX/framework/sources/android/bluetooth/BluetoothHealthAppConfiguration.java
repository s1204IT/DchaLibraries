package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.logging.nano.MetricsProto;

public final class BluetoothHealthAppConfiguration implements Parcelable {
    public static final Parcelable.Creator<BluetoothHealthAppConfiguration> CREATOR = new Parcelable.Creator<BluetoothHealthAppConfiguration>() {
        @Override
        public BluetoothHealthAppConfiguration createFromParcel(Parcel parcel) {
            return new BluetoothHealthAppConfiguration(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt());
        }

        @Override
        public BluetoothHealthAppConfiguration[] newArray(int i) {
            return new BluetoothHealthAppConfiguration[i];
        }
    };
    private final int mChannelType;
    private final int mDataType;
    private final String mName;
    private final int mRole;

    BluetoothHealthAppConfiguration(String str, int i) {
        this.mName = str;
        this.mDataType = i;
        this.mRole = 2;
        this.mChannelType = 12;
    }

    BluetoothHealthAppConfiguration(String str, int i, int i2, int i3) {
        this.mName = str;
        this.mDataType = i;
        this.mRole = i2;
        this.mChannelType = i3;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BluetoothHealthAppConfiguration)) {
            return false;
        }
        BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration = (BluetoothHealthAppConfiguration) obj;
        return this.mName != null && this.mName.equals(bluetoothHealthAppConfiguration.getName()) && this.mDataType == bluetoothHealthAppConfiguration.getDataType() && this.mRole == bluetoothHealthAppConfiguration.getRole() && this.mChannelType == bluetoothHealthAppConfiguration.getChannelType();
    }

    public int hashCode() {
        return (31 * (((((MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + (this.mName != null ? this.mName.hashCode() : 0)) * 31) + this.mDataType) * 31) + this.mRole)) + this.mChannelType;
    }

    public String toString() {
        return "BluetoothHealthAppConfiguration [mName = " + this.mName + ",mDataType = " + this.mDataType + ", mRole = " + this.mRole + ",mChannelType = " + this.mChannelType + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getDataType() {
        return this.mDataType;
    }

    public String getName() {
        return this.mName;
    }

    public int getRole() {
        return this.mRole;
    }

    public int getChannelType() {
        return this.mChannelType;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mName);
        parcel.writeInt(this.mDataType);
        parcel.writeInt(this.mRole);
        parcel.writeInt(this.mChannelType);
    }
}
