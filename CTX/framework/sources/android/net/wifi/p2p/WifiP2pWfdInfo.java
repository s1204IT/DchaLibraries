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
        public WifiP2pWfdInfo createFromParcel(Parcel parcel) {
            WifiP2pWfdInfo wifiP2pWfdInfo = new WifiP2pWfdInfo();
            wifiP2pWfdInfo.readFromParcel(parcel);
            return wifiP2pWfdInfo;
        }

        @Override
        public WifiP2pWfdInfo[] newArray(int i) {
            return new WifiP2pWfdInfo[i];
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
    private int mCtrlPort;
    private int mDeviceInfo;
    private int mExtCapa;
    private int mMaxThroughput;
    private boolean mWfdEnabled;

    public WifiP2pWfdInfo() {
    }

    public WifiP2pWfdInfo(int i, int i2, int i3) {
        this.mWfdEnabled = true;
        this.mDeviceInfo = i;
        this.mCtrlPort = i2;
        this.mMaxThroughput = i3;
    }

    public WifiP2pWfdInfo(int i, int i2, int i3, int i4) {
        this.mWfdEnabled = true;
        this.mDeviceInfo = i;
        this.mCtrlPort = i2;
        this.mMaxThroughput = i3;
        this.mExtCapa = i4;
    }

    public boolean isWfdEnabled() {
        return this.mWfdEnabled;
    }

    public void setWfdEnabled(boolean z) {
        this.mWfdEnabled = z;
    }

    public int getDeviceType() {
        return this.mDeviceInfo & 3;
    }

    public boolean setDeviceType(int i) {
        if (i >= 0 && i <= 3) {
            this.mDeviceInfo &= -4;
            this.mDeviceInfo = i | this.mDeviceInfo;
            return true;
        }
        return false;
    }

    public boolean isCoupledSinkSupportedAtSource() {
        return (this.mDeviceInfo & 8) != 0;
    }

    public void setCoupledSinkSupportAtSource(boolean z) {
        if (z) {
            this.mDeviceInfo |= 8;
        } else {
            this.mDeviceInfo &= -9;
        }
    }

    public boolean isCoupledSinkSupportedAtSink() {
        return (this.mDeviceInfo & 8) != 0;
    }

    public void setCoupledSinkSupportAtSink(boolean z) {
        if (z) {
            this.mDeviceInfo |= 8;
        } else {
            this.mDeviceInfo &= -9;
        }
    }

    public boolean isSessionAvailable() {
        return (this.mDeviceInfo & 48) != 0;
    }

    public void setSessionAvailable(boolean z) {
        if (z) {
            this.mDeviceInfo |= 16;
            this.mDeviceInfo &= -33;
        } else {
            this.mDeviceInfo &= -49;
        }
    }

    public void setContentProtected(boolean z) {
        if (z) {
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

    public void setUibcSupported(boolean z) {
        if (z) {
            this.mExtCapa |= 1;
        } else {
            this.mExtCapa &= -2;
        }
    }

    public boolean isUibcSupported() {
        return (this.mExtCapa & 1) != 0;
    }

    public void setI2cRWSupported(boolean z) {
        if (z) {
            this.mExtCapa |= 2;
        } else {
            this.mExtCapa &= -3;
        }
    }

    public boolean isI2cRWSupported() {
        return (this.mExtCapa & 2) != 0;
    }

    public void setPreferredDisplaySupported(boolean z) {
        if (z) {
            this.mExtCapa |= 4;
        } else {
            this.mExtCapa &= -5;
        }
    }

    public boolean isPreferredDisplaySupported() {
        return (this.mExtCapa & 4) != 0;
    }

    public void setStandbyResumeCtrlSupported(boolean z) {
        if (z) {
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

    public void setControlPort(int i) {
        this.mCtrlPort = i;
    }

    public void setMaxThroughput(int i) {
        this.mMaxThroughput = i;
    }

    public int getMaxThroughput() {
        return this.mMaxThroughput;
    }

    public String getDeviceInfoHex() {
        return String.format(Locale.US, "%04x%04x%04x", Integer.valueOf(this.mDeviceInfo), Integer.valueOf(this.mCtrlPort), Integer.valueOf(this.mMaxThroughput));
    }

    public String getExtCapaHex() {
        return String.format(Locale.US, "%04x%04x", 2, Integer.valueOf(this.mExtCapa));
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("WFD enabled: ");
        stringBuffer.append(this.mWfdEnabled);
        stringBuffer.append("\n WFD DeviceInfo: ");
        stringBuffer.append(this.mDeviceInfo);
        stringBuffer.append("\n WFD CtrlPort: ");
        stringBuffer.append(this.mCtrlPort);
        stringBuffer.append("\n WFD MaxThroughput: ");
        stringBuffer.append(this.mMaxThroughput);
        stringBuffer.append("\n WFD Extended Capability: ");
        stringBuffer.append(this.mExtCapa);
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public WifiP2pWfdInfo(WifiP2pWfdInfo wifiP2pWfdInfo) {
        if (wifiP2pWfdInfo != null) {
            this.mWfdEnabled = wifiP2pWfdInfo.mWfdEnabled;
            this.mDeviceInfo = wifiP2pWfdInfo.mDeviceInfo;
            this.mCtrlPort = wifiP2pWfdInfo.mCtrlPort;
            this.mMaxThroughput = wifiP2pWfdInfo.mMaxThroughput;
            this.mExtCapa = wifiP2pWfdInfo.mExtCapa;
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mWfdEnabled ? 1 : 0);
        parcel.writeInt(this.mDeviceInfo);
        parcel.writeInt(this.mCtrlPort);
        parcel.writeInt(this.mMaxThroughput);
        parcel.writeInt(this.mExtCapa);
    }

    public void readFromParcel(Parcel parcel) {
        this.mWfdEnabled = parcel.readInt() == 1;
        this.mDeviceInfo = parcel.readInt();
        this.mCtrlPort = parcel.readInt();
        this.mMaxThroughput = parcel.readInt();
        this.mExtCapa = parcel.readInt();
    }
}
