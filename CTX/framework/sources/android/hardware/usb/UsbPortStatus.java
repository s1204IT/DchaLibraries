package android.hardware.usb;

import android.os.Parcel;
import android.os.Parcelable;

public final class UsbPortStatus implements Parcelable {
    public static final Parcelable.Creator<UsbPortStatus> CREATOR = new Parcelable.Creator<UsbPortStatus>() {
        @Override
        public UsbPortStatus createFromParcel(Parcel parcel) {
            return new UsbPortStatus(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
        }

        @Override
        public UsbPortStatus[] newArray(int i) {
            return new UsbPortStatus[i];
        }
    };
    private final int mCurrentDataRole;
    private final int mCurrentMode;
    private final int mCurrentPowerRole;
    private final int mSupportedRoleCombinations;

    public UsbPortStatus(int i, int i2, int i3, int i4) {
        this.mCurrentMode = i;
        this.mCurrentPowerRole = i2;
        this.mCurrentDataRole = i3;
        this.mSupportedRoleCombinations = i4;
    }

    public boolean isConnected() {
        return this.mCurrentMode != 0;
    }

    public int getCurrentMode() {
        return this.mCurrentMode;
    }

    public int getCurrentPowerRole() {
        return this.mCurrentPowerRole;
    }

    public int getCurrentDataRole() {
        return this.mCurrentDataRole;
    }

    public boolean isRoleCombinationSupported(int i, int i2) {
        return (UsbPort.combineRolesAsBit(i, i2) & this.mSupportedRoleCombinations) != 0;
    }

    public int getSupportedRoleCombinations() {
        return this.mSupportedRoleCombinations;
    }

    public String toString() {
        return "UsbPortStatus{connected=" + isConnected() + ", currentMode=" + UsbPort.modeToString(this.mCurrentMode) + ", currentPowerRole=" + UsbPort.powerRoleToString(this.mCurrentPowerRole) + ", currentDataRole=" + UsbPort.dataRoleToString(this.mCurrentDataRole) + ", supportedRoleCombinations=" + UsbPort.roleCombinationsToString(this.mSupportedRoleCombinations) + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mCurrentMode);
        parcel.writeInt(this.mCurrentPowerRole);
        parcel.writeInt(this.mCurrentDataRole);
        parcel.writeInt(this.mSupportedRoleCombinations);
    }
}
