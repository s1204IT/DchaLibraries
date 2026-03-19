package com.android.server.twilight;

import java.text.DateFormat;

public class TwilightState {
    private final float mAmount;
    private final boolean mIsNight;

    TwilightState(boolean isNight, float amount) {
        this.mIsNight = isNight;
        this.mAmount = amount;
    }

    public boolean isNight() {
        return this.mIsNight;
    }

    public float getAmount() {
        return this.mAmount;
    }

    public boolean equals(Object o) {
        if (o instanceof TwilightState) {
            return equals((TwilightState) o);
        }
        return false;
    }

    public boolean equals(TwilightState other) {
        return other != null && this.mIsNight == other.mIsNight && this.mAmount == other.mAmount;
    }

    public int hashCode() {
        return 0;
    }

    public String toString() {
        DateFormat.getDateTimeInstance();
        return "{TwilightState: isNight=" + this.mIsNight + ", mAmount=" + this.mAmount + "}";
    }
}
