package android.bluetooth;

import android.os.BatteryStats;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothGattCharacteristic {
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
    protected int mKeySize = 16;
    protected int mPermissions;
    protected int mProperties;
    protected BluetoothGattService mService;
    protected UUID mUuid;
    protected byte[] mValue;
    protected int mWriteType;

    public BluetoothGattCharacteristic(UUID uuid, int properties, int permissions) {
        initCharacteristic(null, uuid, 0, properties, permissions);
    }

    BluetoothGattCharacteristic(BluetoothGattService service, UUID uuid, int instanceId, int properties, int permissions) {
        initCharacteristic(service, uuid, instanceId, properties, permissions);
    }

    private void initCharacteristic(BluetoothGattService service, UUID uuid, int instanceId, int properties, int permissions) {
        this.mUuid = uuid;
        this.mInstance = instanceId;
        this.mProperties = properties;
        this.mPermissions = permissions;
        this.mService = service;
        this.mValue = null;
        this.mDescriptors = new ArrayList();
        if ((this.mProperties & 4) != 0) {
            this.mWriteType = 1;
        } else {
            this.mWriteType = 2;
        }
    }

    int getKeySize() {
        return this.mKeySize;
    }

    public boolean addDescriptor(BluetoothGattDescriptor descriptor) {
        this.mDescriptors.add(descriptor);
        descriptor.setCharacteristic(this);
        return true;
    }

    BluetoothGattDescriptor getDescriptor(UUID uuid, int instanceId) {
        for (BluetoothGattDescriptor descriptor : this.mDescriptors) {
            if (descriptor.getUuid().equals(uuid) && descriptor.getInstanceId() == instanceId) {
                return descriptor;
            }
        }
        return null;
    }

    public BluetoothGattService getService() {
        return this.mService;
    }

    void setService(BluetoothGattService service) {
        this.mService = service;
    }

    public UUID getUuid() {
        return this.mUuid;
    }

    public int getInstanceId() {
        return this.mInstance;
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

    public void setWriteType(int writeType) {
        this.mWriteType = writeType;
    }

    public void setKeySize(int keySize) {
        this.mKeySize = keySize;
    }

    public List<BluetoothGattDescriptor> getDescriptors() {
        return this.mDescriptors;
    }

    public BluetoothGattDescriptor getDescriptor(UUID uuid) {
        for (BluetoothGattDescriptor descriptor : this.mDescriptors) {
            if (descriptor.getUuid().equals(uuid)) {
                return descriptor;
            }
        }
        return null;
    }

    public byte[] getValue() {
        return this.mValue;
    }

    public Integer getIntValue(int formatType, int offset) {
        if (getTypeLen(formatType) + offset > this.mValue.length) {
            return null;
        }
        switch (formatType) {
            case 17:
                return Integer.valueOf(unsignedByteToInt(this.mValue[offset]));
            case 18:
                return Integer.valueOf(unsignedBytesToInt(this.mValue[offset], this.mValue[offset + 1]));
            case 20:
                return Integer.valueOf(unsignedBytesToInt(this.mValue[offset], this.mValue[offset + 1], this.mValue[offset + 2], this.mValue[offset + 3]));
            case 33:
                return Integer.valueOf(unsignedToSigned(unsignedByteToInt(this.mValue[offset]), 8));
            case 34:
                return Integer.valueOf(unsignedToSigned(unsignedBytesToInt(this.mValue[offset], this.mValue[offset + 1]), 16));
            case 36:
                return Integer.valueOf(unsignedToSigned(unsignedBytesToInt(this.mValue[offset], this.mValue[offset + 1], this.mValue[offset + 2], this.mValue[offset + 3]), 32));
            default:
                return null;
        }
    }

    public Float getFloatValue(int formatType, int offset) {
        if (getTypeLen(formatType) + offset > this.mValue.length) {
            return null;
        }
        switch (formatType) {
            case 50:
                return Float.valueOf(bytesToFloat(this.mValue[offset], this.mValue[offset + 1]));
            case 51:
            default:
                return null;
            case 52:
                return Float.valueOf(bytesToFloat(this.mValue[offset], this.mValue[offset + 1], this.mValue[offset + 2], this.mValue[offset + 3]));
        }
    }

    public String getStringValue(int offset) {
        if (this.mValue == null || offset > this.mValue.length) {
            return null;
        }
        byte[] strBytes = new byte[this.mValue.length - offset];
        for (int i = 0; i != this.mValue.length - offset; i++) {
            strBytes[i] = this.mValue[offset + i];
        }
        return new String(strBytes);
    }

    public boolean setValue(byte[] value) {
        this.mValue = value;
        return true;
    }

    public boolean setValue(int value, int formatType, int offset) {
        int len = offset + getTypeLen(formatType);
        if (this.mValue == null) {
            this.mValue = new byte[len];
        }
        if (len > this.mValue.length) {
            return false;
        }
        switch (formatType) {
            case 17:
                this.mValue[offset] = (byte) (value & 255);
                break;
            case 18:
                this.mValue[offset] = (byte) (value & 255);
                this.mValue[offset + 1] = (byte) ((value >> 8) & 255);
                break;
            case 20:
                int offset2 = offset + 1;
                this.mValue[offset] = (byte) (value & 255);
                int offset3 = offset2 + 1;
                this.mValue[offset2] = (byte) ((value >> 8) & 255);
                this.mValue[offset3] = (byte) ((value >> 16) & 255);
                this.mValue[offset3 + 1] = (byte) ((value >> 24) & 255);
                break;
            case 33:
                value = intToSignedBits(value, 8);
                this.mValue[offset] = (byte) (value & 255);
                break;
            case 34:
                value = intToSignedBits(value, 16);
                this.mValue[offset] = (byte) (value & 255);
                this.mValue[offset + 1] = (byte) ((value >> 8) & 255);
                break;
            case 36:
                value = intToSignedBits(value, 32);
                int offset22 = offset + 1;
                this.mValue[offset] = (byte) (value & 255);
                int offset32 = offset22 + 1;
                this.mValue[offset22] = (byte) ((value >> 8) & 255);
                this.mValue[offset32] = (byte) ((value >> 16) & 255);
                this.mValue[offset32 + 1] = (byte) ((value >> 24) & 255);
                break;
        }
        return false;
    }

    public boolean setValue(int mantissa, int exponent, int formatType, int offset) {
        int len = offset + getTypeLen(formatType);
        if (this.mValue == null) {
            this.mValue = new byte[len];
        }
        if (len > this.mValue.length) {
            return false;
        }
        switch (formatType) {
            case 50:
                int mantissa2 = intToSignedBits(mantissa, 12);
                int exponent2 = intToSignedBits(exponent, 4);
                int offset2 = offset + 1;
                this.mValue[offset] = (byte) (mantissa2 & 255);
                this.mValue[offset2] = (byte) ((mantissa2 >> 8) & 15);
                byte[] bArr = this.mValue;
                bArr[offset2] = (byte) (bArr[offset2] + ((byte) ((exponent2 & 15) << 4)));
                return true;
            case 51:
            default:
                return false;
            case 52:
                int mantissa3 = intToSignedBits(mantissa, 24);
                int exponent3 = intToSignedBits(exponent, 8);
                int offset3 = offset + 1;
                this.mValue[offset] = (byte) (mantissa3 & 255);
                int offset4 = offset3 + 1;
                this.mValue[offset3] = (byte) ((mantissa3 >> 8) & 255);
                int offset5 = offset4 + 1;
                this.mValue[offset4] = (byte) ((mantissa3 >> 16) & 255);
                byte[] bArr2 = this.mValue;
                bArr2[offset5] = (byte) (bArr2[offset5] + ((byte) (exponent3 & 255)));
                return true;
        }
    }

    public boolean setValue(String value) {
        this.mValue = value.getBytes();
        return true;
    }

    private int getTypeLen(int formatType) {
        return formatType & 15;
    }

    private int unsignedByteToInt(byte b) {
        return b & BatteryStats.HistoryItem.CMD_NULL;
    }

    private int unsignedBytesToInt(byte b0, byte b1) {
        return unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8);
    }

    private int unsignedBytesToInt(byte b0, byte b1, byte b2, byte b3) {
        return unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8) + (unsignedByteToInt(b2) << 16) + (unsignedByteToInt(b3) << 24);
    }

    private float bytesToFloat(byte b0, byte b1) {
        int mantissa = unsignedToSigned(unsignedByteToInt(b0) + ((unsignedByteToInt(b1) & 15) << 8), 12);
        int exponent = unsignedToSigned(unsignedByteToInt(b1) >> 4, 4);
        return (float) (((double) mantissa) * Math.pow(10.0d, exponent));
    }

    private float bytesToFloat(byte b0, byte b1, byte b2, byte b3) {
        int mantissa = unsignedToSigned(unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8) + (unsignedByteToInt(b2) << 16), 24);
        return (float) (((double) mantissa) * Math.pow(10.0d, b3));
    }

    private int unsignedToSigned(int unsigned, int size) {
        if (((1 << (size - 1)) & unsigned) != 0) {
            return ((1 << (size - 1)) - (((1 << (size - 1)) - 1) & unsigned)) * (-1);
        }
        return unsigned;
    }

    private int intToSignedBits(int i, int size) {
        if (i < 0) {
            return (1 << (size - 1)) + (((1 << (size - 1)) - 1) & i);
        }
        return i;
    }
}
