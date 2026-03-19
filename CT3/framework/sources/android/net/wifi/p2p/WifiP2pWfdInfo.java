package android.net.wifi.p2p;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Locale;

public class WifiP2pWfdInfo implements Parcelable {
    private static final int CONTENT_PROTECTION_SUPPORT = 256;
    private static final int COUPLED_SINK_SUPPORT_AT_SINK = 8;
    private static final int COUPLED_SINK_SUPPORT_AT_SOURCE = 4;
    public static final Parcelable.Creator<WifiP2pWfdInfo> CREATOR = new Parcelable.Creator<WifiP2pWfdInfo>() {
        @Override
        public WifiP2pWfdInfo createFromParcel(Parcel in) {
            WifiP2pWfdInfo device = new WifiP2pWfdInfo();
            device.readFromParcel(in);
            return device;
        }

        @Override
        public WifiP2pWfdInfo[] newArray(int size) {
            return new WifiP2pWfdInfo[size];
        }
    };
    private static final int DEVICE_TYPE = 3;
    private static final int I2C_READ_WRITE_SUPPORT = 2;
    private static final int PREFERRED_DISPLAY_SUPPORT = 4;
    public static final int PRIMARY_SINK = 1;
    public static final int SECONDARY_SINK = 2;
    private static final int SESSION_AVAILABLE = 48;
    private static final int SESSION_AVAILABLE_BIT1 = 16;
    private static final int SESSION_AVAILABLE_BIT2 = 32;
    public static final int SOURCE_OR_PRIMARY_SINK = 3;
    private static final int STANDBY_RESUME_CONTROL_SUPPORT = 8;
    private static final String TAG = "WifiP2pWfdInfo";
    private static final int UIBC_SUPPORT = 1;
    public static final int WFD_SOURCE = 0;
    public boolean mCrossmountLoaned;
    private int mCtrlPort;
    private int mDeviceInfo;
    private int mExtCapa;
    private int mMaxThroughput;
    private boolean mWfdEnabled;

    public WifiP2pWfdInfo() {
    }

    public WifiP2pWfdInfo(int devInfo, int ctrlPort, int maxTput) {
        this.mWfdEnabled = true;
        this.mDeviceInfo = devInfo;
        this.mCtrlPort = ctrlPort;
        this.mMaxThroughput = maxTput;
    }

    public WifiP2pWfdInfo(int devInfo, int ctrlPort, int maxTput, int extCapa) {
        this.mWfdEnabled = true;
        this.mDeviceInfo = devInfo;
        this.mCtrlPort = ctrlPort;
        this.mMaxThroughput = maxTput;
        this.mExtCapa = extCapa;
    }

    public boolean isWfdEnabled() {
        return this.mWfdEnabled;
    }

    public void setWfdEnabled(boolean enabled) {
        this.mWfdEnabled = enabled;
    }

    public int getDeviceType() {
        return this.mDeviceInfo & 3;
    }

    public boolean setDeviceType(int deviceType) {
        if (deviceType < 0 || deviceType > 3) {
            return false;
        }
        this.mDeviceInfo &= -4;
        this.mDeviceInfo |= deviceType;
        return true;
    }

    public boolean isCoupledSinkSupportedAtSource() {
        return (this.mDeviceInfo & 8) != 0;
    }

    public void setCoupledSinkSupportAtSource(boolean enabled) {
        if (enabled) {
            this.mDeviceInfo |= 8;
        } else {
            this.mDeviceInfo &= -9;
        }
    }

    public boolean isCoupledSinkSupportedAtSink() {
        return (this.mDeviceInfo & 8) != 0;
    }

    public void setCoupledSinkSupportAtSink(boolean enabled) {
        if (enabled) {
            this.mDeviceInfo |= 8;
        } else {
            this.mDeviceInfo &= -9;
        }
    }

    public boolean isSessionAvailable() {
        return (this.mDeviceInfo & 48) != 0;
    }

    public void setSessionAvailable(boolean enabled) {
        if (enabled) {
            this.mDeviceInfo |= 16;
            this.mDeviceInfo &= -33;
        } else {
            this.mDeviceInfo &= -49;
        }
    }

    public void setContentProtected(boolean enabled) {
        if (enabled) {
            this.mDeviceInfo |= 256;
        } else {
            this.mDeviceInfo &= -257;
        }
    }

    public boolean isContentProtected() {
        return (this.mDeviceInfo & 256) != 0;
    }

    public int getExtendedCapability() {
        return this.mExtCapa;
    }

    public void setUibcSupported(boolean enabled) {
        if (enabled) {
            this.mExtCapa |= 1;
        } else {
            this.mExtCapa &= -2;
        }
    }

    public boolean isUibcSupported() {
        return (this.mExtCapa & 1) != 0;
    }

    public void setI2cRWSupported(boolean enabled) {
        if (enabled) {
            this.mExtCapa |= 2;
        } else {
            this.mExtCapa &= -3;
        }
    }

    public boolean isI2cRWSupported() {
        return (this.mExtCapa & 2) != 0;
    }

    public void setPreferredDisplaySupported(boolean enabled) {
        if (enabled) {
            this.mExtCapa |= 4;
        } else {
            this.mExtCapa &= -5;
        }
    }

    public boolean isPreferredDisplaySupported() {
        return (this.mExtCapa & 4) != 0;
    }

    public void setStandbyResumeCtrlSupported(boolean enabled) {
        if (enabled) {
            this.mExtCapa |= 8;
        } else {
            this.mExtCapa &= -9;
        }
    }

    public boolean isStandbyResumeCtrlSupported() {
        return (this.mExtCapa & 8) != 0;
    }

    public int getControlPort() {
        return this.mCtrlPort;
    }

    public void setControlPort(int port) {
        this.mCtrlPort = port;
    }

    public void setMaxThroughput(int maxThroughput) {
        this.mMaxThroughput = maxThroughput;
    }

    public int getMaxThroughput() {
        return this.mMaxThroughput;
    }

    public String getDeviceInfoHex() {
        return String.format(Locale.US, "%04x%04x%04x%04x", 6, Integer.valueOf(this.mDeviceInfo), Integer.valueOf(this.mCtrlPort), Integer.valueOf(this.mMaxThroughput));
    }

    public String getExtCapaHex() {
        return String.format(Locale.US, "%04x%04x", 2, Integer.valueOf(this.mExtCapa));
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("WFD enabled: ").append(this.mWfdEnabled);
        sbuf.append("\n WFD DeviceInfo: ").append(this.mDeviceInfo);
        sbuf.append("\n WFD CtrlPort: ").append(this.mCtrlPort);
        sbuf.append("\n WFD MaxThroughput: ").append(this.mMaxThroughput);
        sbuf.append("\n WFD Extended Capability: ").append(this.mExtCapa);
        sbuf.append("\n WFD info. loan to Crossmount? ").append(this.mCrossmountLoaned);
        return sbuf.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public WifiP2pWfdInfo(WifiP2pWfdInfo source) {
        if (source == null) {
            return;
        }
        this.mWfdEnabled = source.mWfdEnabled;
        this.mDeviceInfo = source.mDeviceInfo;
        this.mCtrlPort = source.mCtrlPort;
        this.mMaxThroughput = source.mMaxThroughput;
        this.mExtCapa = source.mExtCapa;
        this.mCrossmountLoaned = source.mCrossmountLoaned;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mWfdEnabled ? 1 : 0);
        dest.writeInt(this.mDeviceInfo);
        dest.writeInt(this.mCtrlPort);
        dest.writeInt(this.mMaxThroughput);
        dest.writeInt(this.mExtCapa);
        dest.writeInt(this.mCrossmountLoaned ? 1 : 0);
    }

    public void readFromParcel(Parcel in) {
        this.mWfdEnabled = in.readInt() == 1;
        this.mDeviceInfo = in.readInt();
        this.mCtrlPort = in.readInt();
        this.mMaxThroughput = in.readInt();
        this.mExtCapa = in.readInt();
        this.mCrossmountLoaned = in.readInt() == 1;
    }
}
