package android.bluetooth;

import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import java.util.UUID;

public class BluetoothGattIncludedService implements Parcelable {
    public static final Parcelable.Creator<BluetoothGattIncludedService> CREATOR = new Parcelable.Creator<BluetoothGattIncludedService>() {
        @Override
        public BluetoothGattIncludedService createFromParcel(Parcel parcel) {
            return new BluetoothGattIncludedService(parcel);
        }

        @Override
        public BluetoothGattIncludedService[] newArray(int i) {
            return new BluetoothGattIncludedService[i];
        }
    };
    protected int mInstanceId;
    protected int mServiceType;
    protected UUID mUuid;

    public BluetoothGattIncludedService(UUID uuid, int i, int i2) {
        this.mUuid = uuid;
        this.mInstanceId = i;
        this.mServiceType = i2;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(new ParcelUuid(this.mUuid), 0);
        parcel.writeInt(this.mInstanceId);
        parcel.writeInt(this.mServiceType);
    }

    private BluetoothGattIncludedService(Parcel parcel) {
        this.mUuid = ((ParcelUuid) parcel.readParcelable(null)).getUuid();
        this.mInstanceId = parcel.readInt();
        this.mServiceType = parcel.readInt();
    }

    public UUID getUuid() {
        return this.mUuid;
    }

    public int getInstanceId() {
        return this.mInstanceId;
    }

    public int getType() {
        return this.mServiceType;
    }
}
