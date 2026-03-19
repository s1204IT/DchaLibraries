package android.hardware.display;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class WifiDisplay implements Parcelable {
    private final boolean mCanConnect;
    private final String mDeviceAddress;
    private final String mDeviceAlias;
    private final String mDeviceName;
    private final boolean mIsAvailable;
    private final boolean mIsRemembered;
    public static final WifiDisplay[] EMPTY_ARRAY = new WifiDisplay[0];
    public static final Parcelable.Creator<WifiDisplay> CREATOR = new Parcelable.Creator<WifiDisplay>() {
        @Override
        public WifiDisplay createFromParcel(Parcel parcel) {
            return new WifiDisplay(parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt() != 0, parcel.readInt() != 0, parcel.readInt() != 0);
        }

        @Override
        public WifiDisplay[] newArray(int i) {
            return i == 0 ? WifiDisplay.EMPTY_ARRAY : new WifiDisplay[i];
        }
    };

    public WifiDisplay(String str, String str2, String str3, boolean z, boolean z2, boolean z3) {
        if (str == null) {
            throw new IllegalArgumentException("deviceAddress must not be null");
        }
        if (str2 == null) {
            throw new IllegalArgumentException("deviceName must not be null");
        }
        this.mDeviceAddress = str;
        this.mDeviceName = str2;
        this.mDeviceAlias = str3;
        this.mIsAvailable = z;
        this.mCanConnect = z2;
        this.mIsRemembered = z3;
    }

    public String getDeviceAddress() {
        return this.mDeviceAddress;
    }

    public String getDeviceName() {
        return this.mDeviceName;
    }

    public String getDeviceAlias() {
        return this.mDeviceAlias;
    }

    public boolean isAvailable() {
        return this.mIsAvailable;
    }

    public boolean canConnect() {
        return this.mCanConnect;
    }

    public boolean isRemembered() {
        return this.mIsRemembered;
    }

    public String getFriendlyDisplayName() {
        return this.mDeviceAlias != null ? this.mDeviceAlias : this.mDeviceName;
    }

    public boolean equals(Object obj) {
        return (obj instanceof WifiDisplay) && equals((WifiDisplay) obj);
    }

    public boolean equals(WifiDisplay wifiDisplay) {
        return wifiDisplay != null && this.mDeviceAddress.equals(wifiDisplay.mDeviceAddress) && this.mDeviceName.equals(wifiDisplay.mDeviceName) && Objects.equals(this.mDeviceAlias, wifiDisplay.mDeviceAlias);
    }

    public boolean hasSameAddress(WifiDisplay wifiDisplay) {
        return wifiDisplay != null && this.mDeviceAddress.equals(wifiDisplay.mDeviceAddress);
    }

    public int hashCode() {
        return this.mDeviceAddress.hashCode();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mDeviceAddress);
        parcel.writeString(this.mDeviceName);
        parcel.writeString(this.mDeviceAlias);
        parcel.writeInt(this.mIsAvailable ? 1 : 0);
        parcel.writeInt(this.mCanConnect ? 1 : 0);
        parcel.writeInt(this.mIsRemembered ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        String str = this.mDeviceName + " (" + this.mDeviceAddress + ")";
        if (this.mDeviceAlias != null) {
            str = str + ", alias " + this.mDeviceAlias;
        }
        return str + ", isAvailable " + this.mIsAvailable + ", canConnect " + this.mCanConnect + ", isRemembered " + this.mIsRemembered;
    }
}
