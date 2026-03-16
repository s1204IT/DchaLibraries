package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

public final class BluetoothActivityEnergyInfo implements Parcelable {
    public static final int BT_STACK_STATE_INVALID = 0;
    public static final int BT_STACK_STATE_STATE_ACTIVE = 1;
    public static final int BT_STACK_STATE_STATE_IDLE = 3;
    public static final int BT_STACK_STATE_STATE_SCANNING = 2;
    public static final Parcelable.Creator<BluetoothActivityEnergyInfo> CREATOR = new Parcelable.Creator<BluetoothActivityEnergyInfo>() {
        @Override
        public BluetoothActivityEnergyInfo createFromParcel(Parcel in) {
            int stackState = in.readInt();
            int txTime = in.readInt();
            int rxTime = in.readInt();
            int idleTime = in.readInt();
            int energyUsed = in.readInt();
            return new BluetoothActivityEnergyInfo(stackState, txTime, rxTime, idleTime, energyUsed);
        }

        @Override
        public BluetoothActivityEnergyInfo[] newArray(int size) {
            return new BluetoothActivityEnergyInfo[size];
        }
    };
    private final int mBluetoothStackState;
    private final int mControllerEnergyUsed;
    private final int mControllerIdleTimeMs;
    private final int mControllerRxTimeMs;
    private final int mControllerTxTimeMs;
    private final long timestamp = System.currentTimeMillis();

    public BluetoothActivityEnergyInfo(int stackState, int txTime, int rxTime, int idleTime, int energyUsed) {
        this.mBluetoothStackState = stackState;
        this.mControllerTxTimeMs = txTime;
        this.mControllerRxTimeMs = rxTime;
        this.mControllerIdleTimeMs = idleTime;
        this.mControllerEnergyUsed = energyUsed;
    }

    public String toString() {
        return "BluetoothActivityEnergyInfo{ timestamp=" + this.timestamp + " mBluetoothStackState=" + this.mBluetoothStackState + " mControllerTxTimeMs=" + this.mControllerTxTimeMs + " mControllerRxTimeMs=" + this.mControllerRxTimeMs + " mControllerIdleTimeMs=" + this.mControllerIdleTimeMs + " mControllerEnergyUsed=" + this.mControllerEnergyUsed + " }";
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mBluetoothStackState);
        out.writeInt(this.mControllerTxTimeMs);
        out.writeInt(this.mControllerRxTimeMs);
        out.writeInt(this.mControllerIdleTimeMs);
        out.writeInt(this.mControllerEnergyUsed);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getBluetoothStackState() {
        return this.mBluetoothStackState;
    }

    public int getControllerTxTimeMillis() {
        return this.mControllerTxTimeMs;
    }

    public int getControllerRxTimeMillis() {
        return this.mControllerRxTimeMs;
    }

    public int getControllerIdleTimeMillis() {
        return this.mControllerIdleTimeMs;
    }

    public int getControllerEnergyUsed() {
        return this.mControllerEnergyUsed;
    }

    public long getTimeStamp() {
        return this.timestamp;
    }

    public boolean isValid() {
        return (getControllerTxTimeMillis() == 0 && getControllerRxTimeMillis() == 0 && getControllerIdleTimeMillis() == 0) ? false : true;
    }
}
