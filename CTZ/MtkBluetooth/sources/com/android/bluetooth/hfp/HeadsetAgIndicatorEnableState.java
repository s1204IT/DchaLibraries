package com.android.bluetooth.hfp;

public class HeadsetAgIndicatorEnableState extends HeadsetMessageObject {
    public boolean battery;
    public boolean roam;
    public boolean service;
    public boolean signal;

    HeadsetAgIndicatorEnableState(boolean z, boolean z2, boolean z3, boolean z4) {
        this.service = z;
        this.roam = z2;
        this.signal = z3;
        this.battery = z4;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof HeadsetAgIndicatorEnableState)) {
            return false;
        }
        HeadsetAgIndicatorEnableState headsetAgIndicatorEnableState = (HeadsetAgIndicatorEnableState) obj;
        return this.service == headsetAgIndicatorEnableState.service && this.roam == headsetAgIndicatorEnableState.roam && this.signal == headsetAgIndicatorEnableState.signal && this.battery == headsetAgIndicatorEnableState.battery;
    }

    public int hashCode() {
        int i = this.service ? 1 : 0;
        if (this.roam) {
            i += 2;
        }
        if (this.signal) {
            i += 4;
        }
        return this.battery ? i + 8 : i;
    }

    @Override
    public void buildString(StringBuilder sb) {
        if (sb == null) {
            return;
        }
        sb.append(getClass().getSimpleName());
        sb.append("[service=");
        sb.append(this.service);
        sb.append(", roam=");
        sb.append(this.roam);
        sb.append(", signal=");
        sb.append(this.signal);
        sb.append(", battery=");
        sb.append(this.battery);
        sb.append("]");
    }
}
