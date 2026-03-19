package android.hardware.hdmi;

import android.annotation.SystemApi;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;

@SystemApi
public class HdmiDeviceInfo implements Parcelable {
    public static final int ADDR_INTERNAL = 0;
    public static final int DEVICE_AUDIO_SYSTEM = 5;
    public static final int DEVICE_INACTIVE = -1;
    public static final int DEVICE_PLAYBACK = 4;
    public static final int DEVICE_PURE_CEC_SWITCH = 6;
    public static final int DEVICE_RECORDER = 1;
    public static final int DEVICE_RESERVED = 2;
    public static final int DEVICE_TUNER = 3;
    public static final int DEVICE_TV = 0;
    public static final int DEVICE_VIDEO_PROCESSOR = 7;
    private static final int HDMI_DEVICE_TYPE_CEC = 0;
    private static final int HDMI_DEVICE_TYPE_HARDWARE = 2;
    private static final int HDMI_DEVICE_TYPE_INACTIVE = 100;
    private static final int HDMI_DEVICE_TYPE_MHL = 1;
    public static final int ID_INVALID = 65535;
    private static final int ID_OFFSET_CEC = 0;
    private static final int ID_OFFSET_HARDWARE = 192;
    private static final int ID_OFFSET_MHL = 128;
    public static final int PATH_INTERNAL = 0;
    public static final int PATH_INVALID = 65535;
    public static final int PORT_INVALID = -1;
    private final int mAdopterId;
    private final int mDeviceId;
    private final int mDevicePowerStatus;
    private final int mDeviceType;
    private final String mDisplayName;
    private final int mHdmiDeviceType;
    private final int mId;
    private final int mLogicalAddress;
    private final int mPhysicalAddress;
    private final int mPortId;
    private final int mVendorId;
    public static final HdmiDeviceInfo INACTIVE_DEVICE = new HdmiDeviceInfo();
    public static final Parcelable.Creator<HdmiDeviceInfo> CREATOR = new Parcelable.Creator<HdmiDeviceInfo>() {
        @Override
        public HdmiDeviceInfo createFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            int i2 = parcel.readInt();
            int i3 = parcel.readInt();
            if (i != 100) {
                switch (i) {
                    case 0:
                        return new HdmiDeviceInfo(parcel.readInt(), i2, i3, parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readInt());
                    case 1:
                        return new HdmiDeviceInfo(i2, i3, parcel.readInt(), parcel.readInt());
                    case 2:
                        return new HdmiDeviceInfo(i2, i3);
                    default:
                        return null;
                }
            }
            return HdmiDeviceInfo.INACTIVE_DEVICE;
        }

        @Override
        public HdmiDeviceInfo[] newArray(int i) {
            return new HdmiDeviceInfo[i];
        }
    };

    public HdmiDeviceInfo(int i, int i2, int i3, int i4, int i5, String str, int i6) {
        this.mHdmiDeviceType = 0;
        this.mPhysicalAddress = i2;
        this.mPortId = i3;
        this.mId = idForCecDevice(i);
        this.mLogicalAddress = i;
        this.mDeviceType = i4;
        this.mVendorId = i5;
        this.mDevicePowerStatus = i6;
        this.mDisplayName = str;
        this.mDeviceId = -1;
        this.mAdopterId = -1;
    }

    public HdmiDeviceInfo(int i, int i2, int i3, int i4, int i5, String str) {
        this(i, i2, i3, i4, i5, str, -1);
    }

    public HdmiDeviceInfo(int i, int i2) {
        this.mHdmiDeviceType = 2;
        this.mPhysicalAddress = i;
        this.mPortId = i2;
        this.mId = idForHardware(i2);
        this.mLogicalAddress = -1;
        this.mDeviceType = 2;
        this.mVendorId = 0;
        this.mDevicePowerStatus = -1;
        this.mDisplayName = "HDMI" + i2;
        this.mDeviceId = -1;
        this.mAdopterId = -1;
    }

    public HdmiDeviceInfo(int i, int i2, int i3, int i4) {
        this.mHdmiDeviceType = 1;
        this.mPhysicalAddress = i;
        this.mPortId = i2;
        this.mId = idForMhlDevice(i2);
        this.mLogicalAddress = -1;
        this.mDeviceType = 2;
        this.mVendorId = 0;
        this.mDevicePowerStatus = -1;
        this.mDisplayName = "Mobile";
        this.mDeviceId = i3;
        this.mAdopterId = i4;
    }

    public HdmiDeviceInfo() {
        this.mHdmiDeviceType = 100;
        this.mPhysicalAddress = 65535;
        this.mId = 65535;
        this.mLogicalAddress = -1;
        this.mDeviceType = -1;
        this.mPortId = -1;
        this.mDevicePowerStatus = -1;
        this.mDisplayName = "Inactive";
        this.mVendorId = 0;
        this.mDeviceId = -1;
        this.mAdopterId = -1;
    }

    public int getId() {
        return this.mId;
    }

    public static int idForCecDevice(int i) {
        return 0 + i;
    }

    public static int idForMhlDevice(int i) {
        return 128 + i;
    }

    public static int idForHardware(int i) {
        return 192 + i;
    }

    public int getLogicalAddress() {
        return this.mLogicalAddress;
    }

    public int getPhysicalAddress() {
        return this.mPhysicalAddress;
    }

    public int getPortId() {
        return this.mPortId;
    }

    public int getDeviceType() {
        return this.mDeviceType;
    }

    public int getDevicePowerStatus() {
        return this.mDevicePowerStatus;
    }

    public int getDeviceId() {
        return this.mDeviceId;
    }

    public int getAdopterId() {
        return this.mAdopterId;
    }

    public boolean isSourceType() {
        return isCecDevice() ? this.mDeviceType == 4 || this.mDeviceType == 1 || this.mDeviceType == 3 : isMhlDevice();
    }

    public boolean isCecDevice() {
        return this.mHdmiDeviceType == 0;
    }

    public boolean isMhlDevice() {
        return this.mHdmiDeviceType == 1;
    }

    public boolean isInactivated() {
        return this.mHdmiDeviceType == 100;
    }

    public String getDisplayName() {
        return this.mDisplayName;
    }

    public int getVendorId() {
        return this.mVendorId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mHdmiDeviceType);
        parcel.writeInt(this.mPhysicalAddress);
        parcel.writeInt(this.mPortId);
        switch (this.mHdmiDeviceType) {
            case 0:
                parcel.writeInt(this.mLogicalAddress);
                parcel.writeInt(this.mDeviceType);
                parcel.writeInt(this.mVendorId);
                parcel.writeInt(this.mDevicePowerStatus);
                parcel.writeString(this.mDisplayName);
                break;
            case 1:
                parcel.writeInt(this.mDeviceId);
                parcel.writeInt(this.mAdopterId);
                break;
        }
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        int i = this.mHdmiDeviceType;
        if (i != 100) {
            switch (i) {
                case 0:
                    stringBuffer.append("CEC: ");
                    stringBuffer.append("logical_address: ");
                    stringBuffer.append(String.format("0x%02X", Integer.valueOf(this.mLogicalAddress)));
                    stringBuffer.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    stringBuffer.append("device_type: ");
                    stringBuffer.append(this.mDeviceType);
                    stringBuffer.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    stringBuffer.append("vendor_id: ");
                    stringBuffer.append(this.mVendorId);
                    stringBuffer.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    stringBuffer.append("display_name: ");
                    stringBuffer.append(this.mDisplayName);
                    stringBuffer.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    stringBuffer.append("power_status: ");
                    stringBuffer.append(this.mDevicePowerStatus);
                    stringBuffer.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    break;
                case 1:
                    stringBuffer.append("MHL: ");
                    stringBuffer.append("device_id: ");
                    stringBuffer.append(String.format("0x%04X", Integer.valueOf(this.mDeviceId)));
                    stringBuffer.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    stringBuffer.append("adopter_id: ");
                    stringBuffer.append(String.format("0x%04X", Integer.valueOf(this.mAdopterId)));
                    stringBuffer.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    break;
                case 2:
                    stringBuffer.append("Hardware: ");
                    break;
                default:
                    return "";
            }
        } else {
            stringBuffer.append("Inactivated: ");
        }
        stringBuffer.append("physical_address: ");
        stringBuffer.append(String.format("0x%04X", Integer.valueOf(this.mPhysicalAddress)));
        stringBuffer.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        stringBuffer.append("port_id: ");
        stringBuffer.append(this.mPortId);
        return stringBuffer.toString();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof HdmiDeviceInfo)) {
            return false;
        }
        HdmiDeviceInfo hdmiDeviceInfo = (HdmiDeviceInfo) obj;
        return this.mHdmiDeviceType == hdmiDeviceInfo.mHdmiDeviceType && this.mPhysicalAddress == hdmiDeviceInfo.mPhysicalAddress && this.mPortId == hdmiDeviceInfo.mPortId && this.mLogicalAddress == hdmiDeviceInfo.mLogicalAddress && this.mDeviceType == hdmiDeviceInfo.mDeviceType && this.mVendorId == hdmiDeviceInfo.mVendorId && this.mDevicePowerStatus == hdmiDeviceInfo.mDevicePowerStatus && this.mDisplayName.equals(hdmiDeviceInfo.mDisplayName) && this.mDeviceId == hdmiDeviceInfo.mDeviceId && this.mAdopterId == hdmiDeviceInfo.mAdopterId;
    }
}
