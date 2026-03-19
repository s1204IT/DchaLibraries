package android.bluetooth;

import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothGattCharacteristic implements Parcelable {
    public static final Parcelable.Creator<BluetoothGattCharacteristic> CREATOR = new Parcelable.Creator<BluetoothGattCharacteristic>() {
        @Override
        public BluetoothGattCharacteristic createFromParcel(Parcel parcel) {
            return new BluetoothGattCharacteristic(parcel);
        }

        @Override
        public BluetoothGattCharacteristic[] newArray(int i) {
            return new BluetoothGattCharacteristic[i];
        }
    };
    public static final int FORMAT_FLOAT = 52;
    public static final int FORMAT_SFLOAT = 50;
    public static final int FORMAT_SINT16 = 34;
    public static final int FORMAT_SINT32 = 36;
    public static final int FORMAT_SINT8 = 33;
    public static final int FORMAT_UINT16 = 18;
    public static final int FORMAT_UINT32 = 20;
    public static final int FORMAT_UINT8 = 17;
    public static final int PERMISSION_READ = 1;
    public static final int PERMISSION_READ_ENCRYPTED = 2;
    public static final int PERMISSION_READ_ENCRYPTED_MITM = 4;
    public static final int PERMISSION_WRITE = 16;
    public static final int PERMISSION_WRITE_ENCRYPTED = 32;
    public static final int PERMISSION_WRITE_ENCRYPTED_MITM = 64;
    public static final int PERMISSION_WRITE_SIGNED = 128;
    public static final int PERMISSION_WRITE_SIGNED_MITM = 256;
    public static final int PROPERTY_BROADCAST = 1;
    public static final int PROPERTY_EXTENDED_PROPS = 128;
    public static final int PROPERTY_INDICATE = 32;
    public static final int PROPERTY_NOTIFY = 16;
    public static final int PROPERTY_READ = 2;
    public static final int PROPERTY_SIGNED_WRITE = 64;
    public static final int PROPERTY_WRITE = 8;
    public static final int PROPERTY_WRITE_NO_RESPONSE = 4;
    public static final int WRITE_TYPE_DEFAULT = 2;
    public static final int WRITE_TYPE_NO_RESPONSE = 1;
    public static final int WRITE_TYPE_SIGNED = 4;
    protected List<BluetoothGattDescriptor> mDescriptors;
    protected int mInstance;
    protected int mKeySize;
    protected int mPermissions;
    protected int mProperties;
    protected BluetoothGattService mService;
    protected UUID mUuid;
    protected byte[] mValue;
    protected int mWriteType;

    public BluetoothGattCharacteristic(UUID uuid, int i, int i2) {
        this.mKeySize = 16;
        initCharacteristic(null, uuid, 0, i, i2);
    }

    BluetoothGattCharacteristic(BluetoothGattService bluetoothGattService, UUID uuid, int i, int i2, int i3) {
        this.mKeySize = 16;
        initCharacteristic(bluetoothGattService, uuid, i, i2, i3);
    }

    public BluetoothGattCharacteristic(UUID uuid, int i, int i2, int i3) {
        this.mKeySize = 16;
        initCharacteristic(null, uuid, i, i2, i3);
    }

    private void initCharacteristic(BluetoothGattService bluetoothGattService, UUID uuid, int i, int i2, int i3) {
        this.mUuid = uuid;
        this.mInstance = i;
        this.mProperties = i2;
        this.mPermissions = i3;
        this.mService = bluetoothGattService;
        this.mValue = null;
        this.mDescriptors = new ArrayList();
        if ((this.mProperties & 4) != 0) {
            this.mWriteType = 1;
        } else {
            this.mWriteType = 2;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(new ParcelUuid(this.mUuid), 0);
        parcel.writeInt(this.mInstance);
        parcel.writeInt(this.mProperties);
        parcel.writeInt(this.mPermissions);
        parcel.writeInt(this.mKeySize);
        parcel.writeInt(this.mWriteType);
        parcel.writeTypedList(this.mDescriptors);
    }

    private BluetoothGattCharacteristic(Parcel parcel) {
        this.mKeySize = 16;
        this.mUuid = ((ParcelUuid) parcel.readParcelable(null)).getUuid();
        this.mInstance = parcel.readInt();
        this.mProperties = parcel.readInt();
        this.mPermissions = parcel.readInt();
        this.mKeySize = parcel.readInt();
        this.mWriteType = parcel.readInt();
        this.mDescriptors = new ArrayList();
        ArrayList<BluetoothGattDescriptor> arrayListCreateTypedArrayList = parcel.createTypedArrayList(BluetoothGattDescriptor.CREATOR);
        if (arrayListCreateTypedArrayList != null) {
            for (BluetoothGattDescriptor bluetoothGattDescriptor : arrayListCreateTypedArrayList) {
                bluetoothGattDescriptor.setCharacteristic(this);
                this.mDescriptors.add(bluetoothGattDescriptor);
            }
        }
    }

    public int getKeySize() {
        return this.mKeySize;
    }

    public boolean addDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor) {
        this.mDescriptors.add(bluetoothGattDescriptor);
        bluetoothGattDescriptor.setCharacteristic(this);
        return true;
    }

    BluetoothGattDescriptor getDescriptor(UUID uuid, int i) {
        for (BluetoothGattDescriptor bluetoothGattDescriptor : this.mDescriptors) {
            if (bluetoothGattDescriptor.getUuid().equals(uuid) && bluetoothGattDescriptor.getInstanceId() == i) {
                return bluetoothGattDescriptor;
            }
        }
        return null;
    }

    public BluetoothGattService getService() {
        return this.mService;
    }

    void setService(BluetoothGattService bluetoothGattService) {
        this.mService = bluetoothGattService;
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

    public int getProperties() {
        return this.mProperties;
    }

    public int getPermissions() {
        return this.mPermissions;
    }

    public int getWriteType() {
        return this.mWriteType;
    }

    public void setWriteType(int i) {
        this.mWriteType = i;
    }

    public void setKeySize(int i) {
        this.mKeySize = i;
    }

    public List<BluetoothGattDescriptor> getDescriptors() {
        return this.mDescriptors;
    }

    public BluetoothGattDescriptor getDescriptor(UUID uuid) {
        for (BluetoothGattDescriptor bluetoothGattDescriptor : this.mDescriptors) {
            if (bluetoothGattDescriptor.getUuid().equals(uuid)) {
                return bluetoothGattDescriptor;
            }
        }
        return null;
    }

    public byte[] getValue() {
        return this.mValue;
    }

    public Integer getIntValue(int i, int i2) {
        if (getTypeLen(i) + i2 > this.mValue.length) {
            return null;
        }
        switch (i) {
            case 17:
                break;
            case 18:
                break;
            case 20:
                break;
            case 33:
                break;
            case 34:
                break;
            case 36:
                break;
        }
        return null;
    }

    public Float getFloatValue(int i, int i2) {
        if (getTypeLen(i) + i2 > this.mValue.length) {
            return null;
        }
        if (i == 50) {
            return Float.valueOf(bytesToFloat(this.mValue[i2], this.mValue[i2 + 1]));
        }
        if (i != 52) {
            return null;
        }
        return Float.valueOf(bytesToFloat(this.mValue[i2], this.mValue[i2 + 1], this.mValue[i2 + 2], this.mValue[i2 + 3]));
    }

    public String getStringValue(int i) {
        if (this.mValue == null || i > this.mValue.length) {
            return null;
        }
        byte[] bArr = new byte[this.mValue.length - i];
        for (int i2 = 0; i2 != this.mValue.length - i; i2++) {
            bArr[i2] = this.mValue[i + i2];
        }
        return new String(bArr);
    }

    public boolean setValue(byte[] bArr) {
        this.mValue = bArr;
        return true;
    }

    public boolean setValue(int i, int i2, int i3) {
        int typeLen = getTypeLen(i2) + i3;
        if (this.mValue == null) {
            this.mValue = new byte[typeLen];
        }
        if (typeLen > this.mValue.length) {
            return false;
        }
        switch (i2) {
            case 17:
                this.mValue[i3] = (byte) (i & 255);
                break;
            case 18:
                this.mValue[i3] = (byte) (i & 255);
                this.mValue[i3 + 1] = (byte) ((i >> 8) & 255);
                break;
            case 20:
                int i4 = i3 + 1;
                this.mValue[i3] = (byte) (i & 255);
                int i5 = i4 + 1;
                this.mValue[i4] = (byte) ((i >> 8) & 255);
                this.mValue[i5] = (byte) ((i >> 16) & 255);
                this.mValue[i5 + 1] = (byte) ((i >> 24) & 255);
                break;
            case 33:
                i = intToSignedBits(i, 8);
                this.mValue[i3] = (byte) (i & 255);
                break;
            case 34:
                i = intToSignedBits(i, 16);
                this.mValue[i3] = (byte) (i & 255);
                this.mValue[i3 + 1] = (byte) ((i >> 8) & 255);
                break;
            case 36:
                i = intToSignedBits(i, 32);
                int i42 = i3 + 1;
                this.mValue[i3] = (byte) (i & 255);
                int i52 = i42 + 1;
                this.mValue[i42] = (byte) ((i >> 8) & 255);
                this.mValue[i52] = (byte) ((i >> 16) & 255);
                this.mValue[i52 + 1] = (byte) ((i >> 24) & 255);
                break;
        }
        return false;
    }

    public boolean setValue(int i, int i2, int i3, int i4) {
        int typeLen = getTypeLen(i3) + i4;
        if (this.mValue == null) {
            this.mValue = new byte[typeLen];
        }
        if (typeLen > this.mValue.length) {
            return false;
        }
        if (i3 == 50) {
            int iIntToSignedBits = intToSignedBits(i, 12);
            int iIntToSignedBits2 = intToSignedBits(i2, 4);
            int i5 = i4 + 1;
            this.mValue[i4] = (byte) (iIntToSignedBits & 255);
            this.mValue[i5] = (byte) ((iIntToSignedBits >> 8) & 15);
            byte[] bArr = this.mValue;
            bArr[i5] = (byte) (bArr[i5] + ((byte) ((iIntToSignedBits2 & 15) << 4)));
            return true;
        }
        if (i3 != 52) {
            return false;
        }
        int iIntToSignedBits3 = intToSignedBits(i, 24);
        int iIntToSignedBits4 = intToSignedBits(i2, 8);
        int i6 = i4 + 1;
        this.mValue[i4] = (byte) (iIntToSignedBits3 & 255);
        int i7 = i6 + 1;
        this.mValue[i6] = (byte) ((iIntToSignedBits3 >> 8) & 255);
        int i8 = i7 + 1;
        this.mValue[i7] = (byte) ((iIntToSignedBits3 >> 16) & 255);
        byte[] bArr2 = this.mValue;
        bArr2[i8] = (byte) (bArr2[i8] + ((byte) (iIntToSignedBits4 & 255)));
        return true;
    }

    public boolean setValue(String str) {
        this.mValue = str.getBytes();
        return true;
    }

    private int getTypeLen(int i) {
        return i & 15;
    }

    private int unsignedByteToInt(byte b) {
        return b & 255;
    }

    private int unsignedBytesToInt(byte b, byte b2) {
        return unsignedByteToInt(b) + (unsignedByteToInt(b2) << 8);
    }

    private int unsignedBytesToInt(byte b, byte b2, byte b3, byte b4) {
        return unsignedByteToInt(b) + (unsignedByteToInt(b2) << 8) + (unsignedByteToInt(b3) << 16) + (unsignedByteToInt(b4) << 24);
    }

    private float bytesToFloat(byte b, byte b2) {
        return (float) (((double) unsignedToSigned(unsignedByteToInt(b) + ((unsignedByteToInt(b2) & 15) << 8), 12)) * Math.pow(10.0d, unsignedToSigned(unsignedByteToInt(b2) >> 4, 4)));
    }

    private float bytesToFloat(byte b, byte b2, byte b3, byte b4) {
        return (float) (((double) unsignedToSigned(unsignedByteToInt(b) + (unsignedByteToInt(b2) << 8) + (unsignedByteToInt(b3) << 16), 24)) * Math.pow(10.0d, b4));
    }

    private int unsignedToSigned(int i, int i2) {
        int i3 = 1 << (i2 - 1);
        if ((i & i3) != 0) {
            return (-1) * (i3 - (i & (i3 - 1)));
        }
        return i;
    }

    private int intToSignedBits(int i, int i2) {
        if (i < 0) {
            int i3 = 1 << (i2 - 1);
            return (i & (i3 - 1)) + i3;
        }
        return i;
    }
}
