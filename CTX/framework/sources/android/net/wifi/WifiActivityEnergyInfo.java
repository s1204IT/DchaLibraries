package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public final class WifiActivityEnergyInfo implements Parcelable {
    public static final Parcelable.Creator<WifiActivityEnergyInfo> CREATOR = new Parcelable.Creator<WifiActivityEnergyInfo>() {
        @Override
        public WifiActivityEnergyInfo createFromParcel(Parcel parcel) {
            return new WifiActivityEnergyInfo(parcel.readLong(), parcel.readInt(), parcel.readLong(), parcel.createLongArray(), parcel.readLong(), parcel.readLong(), parcel.readLong(), parcel.readLong());
        }

        @Override
        public WifiActivityEnergyInfo[] newArray(int i) {
            return new WifiActivityEnergyInfo[i];
        }
    };
    public static final int STACK_STATE_INVALID = 0;
    public static final int STACK_STATE_STATE_ACTIVE = 1;
    public static final int STACK_STATE_STATE_IDLE = 3;
    public static final int STACK_STATE_STATE_SCANNING = 2;
    public long mControllerEnergyUsed;
    public long mControllerIdleTimeMs;
    public long mControllerRxTimeMs;
    public long mControllerScanTimeMs;
    public long mControllerTxTimeMs;
    public long[] mControllerTxTimePerLevelMs;
    public int mStackState;
    public long mTimestamp;

    public WifiActivityEnergyInfo(long j, int i, long j2, long[] jArr, long j3, long j4, long j5, long j6) {
        this.mTimestamp = j;
        this.mStackState = i;
        this.mControllerTxTimeMs = j2;
        this.mControllerTxTimePerLevelMs = jArr;
        this.mControllerRxTimeMs = j3;
        this.mControllerScanTimeMs = j4;
        this.mControllerIdleTimeMs = j5;
        this.mControllerEnergyUsed = j6;
    }

    public String toString() {
        return "WifiActivityEnergyInfo{ timestamp=" + this.mTimestamp + " mStackState=" + this.mStackState + " mControllerTxTimeMs=" + this.mControllerTxTimeMs + " mControllerTxTimePerLevelMs=" + Arrays.toString(this.mControllerTxTimePerLevelMs) + " mControllerRxTimeMs=" + this.mControllerRxTimeMs + " mControllerScanTimeMs=" + this.mControllerScanTimeMs + " mControllerIdleTimeMs=" + this.mControllerIdleTimeMs + " mControllerEnergyUsed=" + this.mControllerEnergyUsed + " }";
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mTimestamp);
        parcel.writeInt(this.mStackState);
        parcel.writeLong(this.mControllerTxTimeMs);
        parcel.writeLongArray(this.mControllerTxTimePerLevelMs);
        parcel.writeLong(this.mControllerRxTimeMs);
        parcel.writeLong(this.mControllerScanTimeMs);
        parcel.writeLong(this.mControllerIdleTimeMs);
        parcel.writeLong(this.mControllerEnergyUsed);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getStackState() {
        return this.mStackState;
    }

    public long getControllerTxTimeMillis() {
        return this.mControllerTxTimeMs;
    }

    public long getControllerTxTimeMillisAtLevel(int i) {
        if (i < this.mControllerTxTimePerLevelMs.length) {
            return this.mControllerTxTimePerLevelMs[i];
        }
        return 0L;
    }

    public long getControllerRxTimeMillis() {
        return this.mControllerRxTimeMs;
    }

    public long getControllerScanTimeMillis() {
        return this.mControllerScanTimeMs;
    }

    public long getControllerIdleTimeMillis() {
        return this.mControllerIdleTimeMs;
    }

    public long getControllerEnergyUsed() {
        return this.mControllerEnergyUsed;
    }

    public long getTimeStamp() {
        return this.mTimestamp;
    }

    public boolean isValid() {
        return this.mControllerTxTimeMs >= 0 && this.mControllerRxTimeMs >= 0 && this.mControllerScanTimeMs >= 0 && this.mControllerIdleTimeMs >= 0;
    }
}
