package android.net.wifi.p2p;

import android.os.Parcel;
import android.os.Parcelable;
import android.telecom.Logging.Session;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WifiP2pDevice implements Parcelable {
    public static final int AVAILABLE = 3;
    public static final int CONNECTED = 0;
    private static final int DEVICE_CAPAB_CLIENT_DISCOVERABILITY = 2;
    private static final int DEVICE_CAPAB_CONCURRENT_OPER = 4;
    private static final int DEVICE_CAPAB_DEVICE_LIMIT = 16;
    private static final int DEVICE_CAPAB_INFRA_MANAGED = 8;
    private static final int DEVICE_CAPAB_INVITATION_PROCEDURE = 32;
    private static final int DEVICE_CAPAB_SERVICE_DISCOVERY = 1;
    public static final int FAILED = 2;
    private static final int GROUP_CAPAB_CROSS_CONN = 16;
    private static final int GROUP_CAPAB_GROUP_FORMATION = 64;
    private static final int GROUP_CAPAB_GROUP_LIMIT = 4;
    private static final int GROUP_CAPAB_GROUP_OWNER = 1;
    private static final int GROUP_CAPAB_INTRA_BSS_DIST = 8;
    private static final int GROUP_CAPAB_PERSISTENT_GROUP = 2;
    private static final int GROUP_CAPAB_PERSISTENT_RECONN = 32;
    public static final int INVITED = 1;
    private static final String TAG = "WifiP2pDevice";
    public static final int UNAVAILABLE = 4;
    private static final int WPS_CONFIG_DISPLAY = 8;
    private static final int WPS_CONFIG_KEYPAD = 256;
    private static final int WPS_CONFIG_PUSHBUTTON = 128;
    public String deviceAddress;
    public int deviceCapability;
    public String deviceIP;
    public String deviceName;
    public int groupCapability;
    public String interfaceAddress;
    public String primaryDeviceType;
    public String secondaryDeviceType;
    public int status;
    public WifiP2pWfdInfo wfdInfo;
    public int wpsConfigMethodsSupported;
    private static final Pattern detailedDevicePattern = Pattern.compile("((?:[0-9a-f]{2}:){5}[0-9a-f]{2}) (\\d+ )?p2p_dev_addr=((?:[0-9a-f]{2}:){5}[0-9a-f]{2}) pri_dev_type=(\\d+-[0-9a-fA-F]+-\\d+) name='(.*)' config_methods=(0x[0-9a-fA-F]+) dev_capab=(0x[0-9a-fA-F]+) group_capab=(0x[0-9a-fA-F]+)( wfd_dev_info=0x([0-9a-fA-F]{12}))?");
    private static final Pattern twoTokenPattern = Pattern.compile("(p2p_dev_addr=)?((?:[0-9a-f]{2}:){5}[0-9a-f]{2})");
    private static final Pattern threeTokenPattern = Pattern.compile("((?:[0-9a-f]{2}:){5}[0-9a-f]{2}) p2p_dev_addr=((?:[0-9a-f]{2}:){5}[0-9a-f]{2})");
    public static final Parcelable.Creator<WifiP2pDevice> CREATOR = new Parcelable.Creator<WifiP2pDevice>() {
        @Override
        public WifiP2pDevice createFromParcel(Parcel parcel) {
            WifiP2pDevice wifiP2pDevice = new WifiP2pDevice();
            wifiP2pDevice.deviceName = parcel.readString();
            wifiP2pDevice.deviceAddress = parcel.readString();
            wifiP2pDevice.interfaceAddress = parcel.readString();
            wifiP2pDevice.primaryDeviceType = parcel.readString();
            wifiP2pDevice.secondaryDeviceType = parcel.readString();
            wifiP2pDevice.wpsConfigMethodsSupported = parcel.readInt();
            wifiP2pDevice.deviceCapability = parcel.readInt();
            wifiP2pDevice.groupCapability = parcel.readInt();
            wifiP2pDevice.status = parcel.readInt();
            if (parcel.readInt() == 1) {
                wifiP2pDevice.wfdInfo = WifiP2pWfdInfo.CREATOR.createFromParcel(parcel);
            }
            wifiP2pDevice.deviceIP = parcel.readString();
            return wifiP2pDevice;
        }

        @Override
        public WifiP2pDevice[] newArray(int i) {
            return new WifiP2pDevice[i];
        }
    };

    public WifiP2pDevice() {
        this.deviceName = "";
        this.deviceAddress = "";
        this.interfaceAddress = "00:00:00:00:00:00";
        this.status = 4;
    }

    public WifiP2pDevice(String str) throws IllegalArgumentException {
        this.deviceName = "";
        this.deviceAddress = "";
        this.interfaceAddress = "00:00:00:00:00:00";
        this.status = 4;
        String[] strArrSplit = str.split("[ \n]");
        if (strArrSplit.length < 1) {
            throw new IllegalArgumentException("Malformed supplicant event");
        }
        switch (strArrSplit.length) {
            case 1:
                this.deviceAddress = str;
                return;
            case 2:
                Matcher matcher = twoTokenPattern.matcher(str);
                if (!matcher.find()) {
                    throw new IllegalArgumentException("Malformed supplicant event");
                }
                this.deviceAddress = matcher.group(2);
                return;
            case 3:
                Matcher matcher2 = threeTokenPattern.matcher(str);
                if (!matcher2.find()) {
                    throw new IllegalArgumentException("Malformed supplicant event");
                }
                this.interfaceAddress = matcher2.group(1);
                this.deviceAddress = matcher2.group(2);
                return;
            default:
                Matcher matcher3 = detailedDevicePattern.matcher(str.replaceAll("\n", Session.SESSION_SEPARATION_CHAR_CHILD));
                if (!matcher3.find()) {
                    throw new IllegalArgumentException("Malformed supplicant event");
                }
                this.interfaceAddress = matcher3.group(1);
                this.deviceAddress = matcher3.group(3);
                this.primaryDeviceType = matcher3.group(4);
                this.deviceName = matcher3.group(5);
                this.wpsConfigMethodsSupported = parseHex(matcher3.group(6));
                this.deviceCapability = parseHex(matcher3.group(7));
                this.groupCapability = parseHex(matcher3.group(8));
                if (matcher3.group(9) != null) {
                    String strGroup = matcher3.group(10);
                    this.wfdInfo = new WifiP2pWfdInfo(parseHex(strGroup.substring(0, 4)), parseHex(strGroup.substring(4, 8)), parseHex(strGroup.substring(8, 12)));
                }
                if (strArrSplit[0].startsWith("P2P-DEVICE-FOUND")) {
                    this.status = 3;
                }
                this.deviceIP = null;
                return;
        }
    }

    public boolean wpsPbcSupported() {
        return (this.wpsConfigMethodsSupported & 128) != 0;
    }

    public boolean wpsKeypadSupported() {
        return (this.wpsConfigMethodsSupported & 256) != 0;
    }

    public boolean wpsDisplaySupported() {
        return (this.wpsConfigMethodsSupported & 8) != 0;
    }

    public boolean isServiceDiscoveryCapable() {
        return (this.deviceCapability & 1) != 0;
    }

    public boolean isInvitationCapable() {
        return (this.deviceCapability & 32) != 0;
    }

    public boolean isDeviceLimit() {
        return (this.deviceCapability & 16) != 0;
    }

    public boolean isGroupOwner() {
        return (this.groupCapability & 1) != 0;
    }

    public boolean isGroupLimit() {
        return (this.groupCapability & 4) != 0;
    }

    public void update(WifiP2pDevice wifiP2pDevice) {
        updateSupplicantDetails(wifiP2pDevice);
        this.status = wifiP2pDevice.status;
    }

    public void updateSupplicantDetails(WifiP2pDevice wifiP2pDevice) {
        if (wifiP2pDevice == null) {
            throw new IllegalArgumentException("device is null");
        }
        if (wifiP2pDevice.deviceAddress == null) {
            throw new IllegalArgumentException("deviceAddress is null");
        }
        if (!this.deviceAddress.equals(wifiP2pDevice.deviceAddress)) {
            throw new IllegalArgumentException("deviceAddress does not match");
        }
        this.deviceName = wifiP2pDevice.deviceName;
        this.interfaceAddress = wifiP2pDevice.interfaceAddress;
        this.primaryDeviceType = wifiP2pDevice.primaryDeviceType;
        this.secondaryDeviceType = wifiP2pDevice.secondaryDeviceType;
        this.wpsConfigMethodsSupported = wifiP2pDevice.wpsConfigMethodsSupported;
        this.deviceCapability = wifiP2pDevice.deviceCapability;
        this.groupCapability = wifiP2pDevice.groupCapability;
        this.wfdInfo = wifiP2pDevice.wfdInfo;
        this.deviceIP = wifiP2pDevice.deviceIP;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WifiP2pDevice)) {
            return false;
        }
        WifiP2pDevice wifiP2pDevice = (WifiP2pDevice) obj;
        if (wifiP2pDevice == null || wifiP2pDevice.deviceAddress == null) {
            return this.deviceAddress == null;
        }
        return wifiP2pDevice.deviceAddress.equals(this.deviceAddress);
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Device: ");
        stringBuffer.append(this.deviceName);
        stringBuffer.append("\n deviceAddress: ");
        stringBuffer.append(this.deviceAddress);
        stringBuffer.append("\n interfaceAddress: ");
        stringBuffer.append(this.interfaceAddress);
        stringBuffer.append("\n primary type: ");
        stringBuffer.append(this.primaryDeviceType);
        stringBuffer.append("\n secondary type: ");
        stringBuffer.append(this.secondaryDeviceType);
        stringBuffer.append("\n wps: ");
        stringBuffer.append(this.wpsConfigMethodsSupported);
        stringBuffer.append("\n grpcapab: ");
        stringBuffer.append(this.groupCapability);
        stringBuffer.append("\n devcapab: ");
        stringBuffer.append(this.deviceCapability);
        stringBuffer.append("\n status: ");
        stringBuffer.append(this.status);
        stringBuffer.append("\n wfdInfo: ");
        stringBuffer.append(this.wfdInfo);
        stringBuffer.append("\n deviceIP: ");
        stringBuffer.append(this.deviceIP);
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public WifiP2pDevice(WifiP2pDevice wifiP2pDevice) {
        this.deviceName = "";
        this.deviceAddress = "";
        this.interfaceAddress = "00:00:00:00:00:00";
        this.status = 4;
        if (wifiP2pDevice != null) {
            this.deviceName = wifiP2pDevice.deviceName;
            this.deviceAddress = wifiP2pDevice.deviceAddress;
            this.interfaceAddress = wifiP2pDevice.interfaceAddress;
            this.primaryDeviceType = wifiP2pDevice.primaryDeviceType;
            this.secondaryDeviceType = wifiP2pDevice.secondaryDeviceType;
            this.wpsConfigMethodsSupported = wifiP2pDevice.wpsConfigMethodsSupported;
            this.deviceCapability = wifiP2pDevice.deviceCapability;
            this.groupCapability = wifiP2pDevice.groupCapability;
            this.status = wifiP2pDevice.status;
            this.wfdInfo = new WifiP2pWfdInfo(wifiP2pDevice.wfdInfo);
            this.deviceIP = wifiP2pDevice.deviceIP;
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.deviceName);
        parcel.writeString(this.deviceAddress);
        parcel.writeString(this.interfaceAddress);
        parcel.writeString(this.primaryDeviceType);
        parcel.writeString(this.secondaryDeviceType);
        parcel.writeInt(this.wpsConfigMethodsSupported);
        parcel.writeInt(this.deviceCapability);
        parcel.writeInt(this.groupCapability);
        parcel.writeInt(this.status);
        if (this.wfdInfo != null) {
            parcel.writeInt(1);
            this.wfdInfo.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeString(this.deviceIP);
    }

    private int parseHex(String str) {
        if (str.startsWith("0x") || str.startsWith("0X")) {
            str = str.substring(2);
        }
        try {
            return Integer.parseInt(str, 16);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse hex string " + str);
            return 0;
        }
    }
}
