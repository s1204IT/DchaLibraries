package android.bluetooth;

import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import java.util.UUID;

public class BluetoothGattDescriptor implements Parcelable {
    public static final int PERMISSION_READ = 1;
    public static final int PERMISSION_READ_ENCRYPTED = 2;
    public static final int PERMISSION_READ_ENCRYPTED_MITM = 4;
    public static final int PERMISSION_WRITE = 16;
    public static final int PERMISSION_WRITE_ENCRYPTED = 32;
    public static final int PERMISSION_WRITE_ENCRYPTED_MITM = 64;
    public static final int PERMISSION_WRITE_SIGNED = 128;
    public static final int PERMISSION_WRITE_SIGNED_MITM = 256;
    protected BluetoothGattCharacteristic mCharacteristic;
    protected int mInstance;
    protected int mPermissions;
    protected UUID mUuid;
    protected byte[] mValue;
    public static final byte[] ENABLE_NOTIFICATION_VALUE = {1, 0};
    public static final byte[] ENABLE_INDICATION_VALUE = {2, 0};
    public static final byte[] DISABLE_NOTIFICATION_VALUE = {0, 0};
    public static final Parcelable.Creator<BluetoothGattDescriptor> CREATOR = new Parcelable.Creator<BluetoothGattDescriptor>() {
        @Override
        public BluetoothGattDescriptor createFromParcel(Parcel parcel) {
            return new BluetoothGattDescriptor(parcel);
        }

        @Override
        public BluetoothGattDescriptor[] newArray(int i) {
            return new BluetoothGattDescriptor[i];
        }
    };

    public BluetoothGattDescriptor(UUID uuid, int i) {
        initDescriptor(null, uuid, 0, i);
    }

    BluetoothGattDescriptor(BluetoothGattCharacteristic bluetoothGattCharacteristic, UUID uuid, int i, int i2) {
        initDescriptor(bluetoothGattCharacteristic, uuid, i, i2);
    }

    public BluetoothGattDescriptor(UUID uuid, int i, int i2) {
        initDescriptor(null, uuid, i, i2);
    }

    private void initDescriptor(BluetoothGattCharacteristic bluetoothGattCharacteristic, UUID uuid, int i, int i2) {
        this.mCharacteristic = bluetoothGattCharacteristic;
        this.mUuid = uuid;
        this.mInstance = i;
        this.mPermissions = i2;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(new ParcelUuid(this.mUuid), 0);
        parcel.writeInt(this.mInstance);
        parcel.writeInt(this.mPermissions);
    }

    private BluetoothGattDescriptor(Parcel parcel) {
        this.mUuid = ((ParcelUuid) parcel.readParcelable(null)).getUuid();
        this.mInstance = parcel.readInt();
        this.mPermissions = parcel.readInt();
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return this.mCharacteristic;
    }

    void setCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        this.mCharacteristic = bluetoothGattCharacteristic;
    }

    public UUID getUuid() {
        return this.mUuid;
    }

    public int getInstanceId() {
        return this.mInstance;
    }

    public void setInstanceId(int i) {
        this.mInstance = i;
    }

    public int getPermissions() {
        return this.mPermissions;
    }

    public byte[] getValue() {
        return this.mValue;
    }

    public boolean setValue(byte[] bArr) {
        this.mValue = bArr;
        return true;
    }
}
