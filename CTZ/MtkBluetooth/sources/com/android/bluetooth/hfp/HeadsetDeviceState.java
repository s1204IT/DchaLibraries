package com.android.bluetooth.hfp;

class HeadsetDeviceState extends HeadsetMessageObject {
    int mBatteryCharge;
    int mRoam;
    int mService;
    int mSignal;

    HeadsetDeviceState(int i, int i2, int i3, int i4) {
        this.mService = i;
        this.mRoam = i2;
        this.mSignal = i3;
        this.mBatteryCharge = i4;
    }

    @Override
    public void buildString(StringBuilder sb) {
        if (sb == null) {
            return;
        }
        sb.append(getClass().getSimpleName());
        sb.append("[hasCellularService=");
        sb.append(this.mService);
        sb.append(", isRoaming=");
        sb.append(this.mRoam);
        sb.append(", signalStrength");
        sb.append(this.mSignal);
        sb.append(", batteryCharge=");
        sb.append(this.mBatteryCharge);
        sb.append("]");
    }
}
