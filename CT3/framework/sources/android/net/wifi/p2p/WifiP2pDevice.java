package android.net.wifi.p2p;

import android.net.ProxyInfo;
import android.os.Parcel;
import android.os.Parcelable;
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
        public WifiP2pDevice createFromParcel(Parcel in) {
            WifiP2pDevice device = new WifiP2pDevice();
            device.deviceName = in.readString();
            device.deviceAddress = in.readString();
            device.interfaceAddress = in.readString();
            device.primaryDeviceType = in.readString();
            device.secondaryDeviceType = in.readString();
            device.wpsConfigMethodsSupported = in.readInt();
            device.deviceCapability = in.readInt();
            device.groupCapability = in.readInt();
            device.status = in.readInt();
            if (in.readInt() == 1) {
                device.wfdInfo = WifiP2pWfdInfo.CREATOR.createFromParcel(in);
            }
            device.deviceIP = in.readString();
            return device;
        }

        @Override
        public WifiP2pDevice[] newArray(int size) {
            return new WifiP2pDevice[size];
        }
    };

    public WifiP2pDevice() {
        this.deviceName = ProxyInfo.LOCAL_EXCL_LIST;
        this.deviceAddress = ProxyInfo.LOCAL_EXCL_LIST;
        this.interfaceAddress = "00:00:00:00:00:00";
        this.status = 4;
    }

    public WifiP2pDevice(String string) throws IllegalArgumentException {
        this.deviceName = ProxyInfo.LOCAL_EXCL_LIST;
        this.deviceAddress = ProxyInfo.LOCAL_EXCL_LIST;
        this.interfaceAddress = "00:00:00:00:00:00";
        this.status = 4;
        String[] tokens = string.split("[ \n]");
        if (tokens.length < 1) {
            throw new IllegalArgumentException("Malformed supplicant event");
        }
        switch (tokens.length) {
            case 1:
                this.deviceAddress = string;
                return;
            case 2:
                Matcher match = twoTokenPattern.matcher(string);
                if (!match.find()) {
                    throw new IllegalArgumentException("Malformed supplicant event");
                }
                this.deviceAddress = match.group(2);
                return;
            case 3:
                Matcher match2 = threeTokenPattern.matcher(string);
                if (!match2.find()) {
                    throw new IllegalArgumentException("Malformed supplicant event");
                }
                this.interfaceAddress = match2.group(1);
                this.deviceAddress = match2.group(2);
                return;
            default:
                String modifiedString = string.replaceAll("\n", "_");
                Matcher match3 = detailedDevicePattern.matcher(modifiedString);
                if (!match3.find()) {
                    throw new IllegalArgumentException("Malformed supplicant event");
                }
                this.interfaceAddress = match3.group(1);
                this.deviceAddress = match3.group(3);
                this.primaryDeviceType = match3.group(4);
                this.deviceName = match3.group(5);
                this.wpsConfigMethodsSupported = parseHex(match3.group(6));
                this.deviceCapability = parseHex(match3.group(7));
                this.groupCapability = parseHex(match3.group(8));
                if (match3.group(9) != null) {
                    String str = match3.group(10);
                    this.wfdInfo = new WifiP2pWfdInfo(parseHex(str.substring(0, 4)), parseHex(str.substring(4, 8)), parseHex(str.substring(8, 12)));
                }
                if (tokens[0].startsWith("P2P-DEVICE-FOUND")) {
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

    public void update(WifiP2pDevice device) {
        updateSupplicantDetails(device);
        this.status = device.status;
    }

    public void updateSupplicantDetails(WifiP2pDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("device is null");
        }
        if (device.deviceAddress == null) {
            throw new IllegalArgumentException("deviceAddress is null");
        }
        if (!this.deviceAddress.equals(device.deviceAddress)) {
            throw new IllegalArgumentException("deviceAddress does not match");
        }
        this.deviceName = device.deviceName;
        this.interfaceAddress = device.interfaceAddress;
        this.primaryDeviceType = device.primaryDeviceType;
        this.secondaryDeviceType = device.secondaryDeviceType;
        this.wpsConfigMethodsSupported = device.wpsConfigMethodsSupported;
        this.deviceCapability = device.deviceCapability;
        this.groupCapability = device.groupCapability;
        this.wfdInfo = device.wfdInfo;
        this.deviceIP = device.deviceIP;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WifiP2pDevice)) {
            return false;
        }
        WifiP2pDevice other = (WifiP2pDevice) obj;
        if (other == null || other.deviceAddress == null) {
            return this.deviceAddress == null;
        }
        return other.deviceAddress.equals(this.deviceAddress);
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("Device: ").append(this.deviceName);
        sbuf.append("\n deviceAddress: ").append(this.deviceAddress);
        sbuf.append("\n interfaceAddress: ").append(this.interfaceAddress);
        sbuf.append("\n primary type: ").append(this.primaryDeviceType);
        sbuf.append("\n secondary type: ").append(this.secondaryDeviceType);
        sbuf.append("\n wps: ").append(this.wpsConfigMethodsSupported);
        sbuf.append("\n grpcapab: ").append(this.groupCapability);
        sbuf.append("\n devcapab: ").append(this.deviceCapability);
        sbuf.append("\n status: ").append(this.status);
        sbuf.append("\n wfdInfo: ").append(this.wfdInfo);
        sbuf.append("\n deviceIP: ").append(this.deviceIP);
        return sbuf.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public WifiP2pDevice(WifiP2pDevice source) {
        this.deviceName = ProxyInfo.LOCAL_EXCL_LIST;
        this.deviceAddress = ProxyInfo.LOCAL_EXCL_LIST;
        this.interfaceAddress = "00:00:00:00:00:00";
        this.status = 4;
        if (source == null) {
            return;
        }
        this.deviceName = source.deviceName;
        this.deviceAddress = source.deviceAddress;
        this.interfaceAddress = source.interfaceAddress;
        this.primaryDeviceType = source.primaryDeviceType;
        this.secondaryDeviceType = source.secondaryDeviceType;
        this.wpsConfigMethodsSupported = source.wpsConfigMethodsSupported;
        this.deviceCapability = source.deviceCapability;
        this.groupCapability = source.groupCapability;
        this.status = source.status;
        this.wfdInfo = new WifiP2pWfdInfo(source.wfdInfo);
        this.deviceIP = source.deviceIP;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.deviceName);
        dest.writeString(this.deviceAddress);
        dest.writeString(this.interfaceAddress);
        dest.writeString(this.primaryDeviceType);
        dest.writeString(this.secondaryDeviceType);
        dest.writeInt(this.wpsConfigMethodsSupported);
        dest.writeInt(this.deviceCapability);
        dest.writeInt(this.groupCapability);
        dest.writeInt(this.status);
        if (this.wfdInfo != null) {
            dest.writeInt(1);
            this.wfdInfo.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeString(this.deviceIP);
    }

    public static int parseHex(String hexString) {
        if (hexString.startsWith("0x") || hexString.startsWith("0X")) {
            hexString = hexString.substring(2);
        }
        try {
            int num = Integer.parseInt(hexString, 16);
            return num;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse hex string " + hexString);
            return 0;
        }
    }
}
