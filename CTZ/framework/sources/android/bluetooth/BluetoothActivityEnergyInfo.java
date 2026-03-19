package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public final class BluetoothActivityEnergyInfo implements Parcelable {
    public static final int BT_STACK_STATE_INVALID = 0;
    public static final int BT_STACK_STATE_STATE_ACTIVE = 1;
    public static final int BT_STACK_STATE_STATE_IDLE = 3;
    public static final int BT_STACK_STATE_STATE_SCANNING = 2;
    public static final Parcelable.Creator<BluetoothActivityEnergyInfo> CREATOR = new Parcelable.Creator<BluetoothActivityEnergyInfo>() {
        @Override
        public BluetoothActivityEnergyInfo createFromParcel(Parcel parcel) {
            return new BluetoothActivityEnergyInfo(parcel);
        }

        @Override
        public BluetoothActivityEnergyInfo[] newArray(int i) {
            return new BluetoothActivityEnergyInfo[i];
        }
    };
    private int mBluetoothStackState;
    private long mControllerEnergyUsed;
    private long mControllerIdleTimeMs;
    private long mControllerRxTimeMs;
    private long mControllerTxTimeMs;
    private final long mTimestamp;
    private UidTraffic[] mUidTraffic;

    public BluetoothActivityEnergyInfo(long j, int i, long j2, long j3, long j4, long j5) {
        this.mTimestamp = j;
        this.mBluetoothStackState = i;
        this.mControllerTxTimeMs = j2;
        this.mControllerRxTimeMs = j3;
        this.mControllerIdleTimeMs = j4;
        this.mControllerEnergyUsed = j5;
    }

    BluetoothActivityEnergyInfo(Parcel parcel) {
        this.mTimestamp = parcel.readLong();
        this.mBluetoothStackState = parcel.readInt();
        this.mControllerTxTimeMs = parcel.readLong();
        this.mControllerRxTimeMs = parcel.readLong();
        this.mControllerIdleTimeMs = parcel.readLong();
        this.mControllerEnergyUsed = parcel.readLong();
        this.mUidTraffic = (UidTraffic[]) parcel.createTypedArray(UidTraffic.CREATOR);
    }

    public String toString() {
        return "BluetoothActivityEnergyInfo{ mTimestamp=" + this.mTimestamp + " mBluetoothStackState=" + this.mBluetoothStackState + " mControllerTxTimeMs=" + this.mControllerTxTimeMs + " mControllerRxTimeMs=" + this.mControllerRxTimeMs + " mControllerIdleTimeMs=" + this.mControllerIdleTimeMs + " mControllerEnergyUsed=" + this.mControllerEnergyUsed + " mUidTraffic=" + Arrays.toString(this.mUidTraffic) + " }";
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mTimestamp);
        parcel.writeInt(this.mBluetoothStackState);
        parcel.writeLong(this.mControllerTxTimeMs);
        parcel.writeLong(this.mControllerRxTimeMs);
        parcel.writeLong(this.mControllerIdleTimeMs);
        parcel.writeLong(this.mControllerEnergyUsed);
        parcel.writeTypedArray(this.mUidTraffic, i);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getBluetoothStackState() {
        return this.mBluetoothStackState;
    }

    public long getControllerTxTimeMillis() {
        return this.mControllerTxTimeMs;
    }

    public long getControllerRxTimeMillis() {
        return this.mControllerRxTimeMs;
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

    public UidTraffic[] getUidTraffic() {
        return this.mUidTraffic;
    }

    public void setUidTraffic(UidTraffic[] uidTrafficArr) {
        this.mUidTraffic = uidTrafficArr;
    }

    public boolean isValid() {
        return this.mControllerTxTimeMs >= 0 && this.mControllerRxTimeMs >= 0 && this.mControllerIdleTimeMs >= 0;
    }
}
