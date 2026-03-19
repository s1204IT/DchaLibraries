package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import java.util.UUID;

public final class BluetoothHeadsetClientCall implements Parcelable {
    public static final int CALL_STATE_ACTIVE = 0;
    public static final int CALL_STATE_ALERTING = 3;
    public static final int CALL_STATE_DIALING = 2;
    public static final int CALL_STATE_HELD = 1;
    public static final int CALL_STATE_HELD_BY_RESPONSE_AND_HOLD = 6;
    public static final int CALL_STATE_INCOMING = 4;
    public static final int CALL_STATE_TERMINATED = 7;
    public static final int CALL_STATE_WAITING = 5;
    public static final Parcelable.Creator<BluetoothHeadsetClientCall> CREATOR = new Parcelable.Creator<BluetoothHeadsetClientCall>() {
        @Override
        public BluetoothHeadsetClientCall createFromParcel(Parcel parcel) {
            return new BluetoothHeadsetClientCall((BluetoothDevice) parcel.readParcelable(null), parcel.readInt(), UUID.fromString(parcel.readString()), parcel.readInt(), parcel.readString(), parcel.readInt() == 1, parcel.readInt() == 1, parcel.readInt() == 1);
        }

        @Override
        public BluetoothHeadsetClientCall[] newArray(int i) {
            return new BluetoothHeadsetClientCall[i];
        }
    };
    private final long mCreationElapsedMilli;
    private final BluetoothDevice mDevice;
    private final int mId;
    private final boolean mInBandRing;
    private boolean mMultiParty;
    private String mNumber;
    private final boolean mOutgoing;
    private int mState;
    private final UUID mUUID;

    public BluetoothHeadsetClientCall(BluetoothDevice bluetoothDevice, int i, int i2, String str, boolean z, boolean z2, boolean z3) {
        this(bluetoothDevice, i, UUID.randomUUID(), i2, str, z, z2, z3);
    }

    public BluetoothHeadsetClientCall(BluetoothDevice bluetoothDevice, int i, UUID uuid, int i2, String str, boolean z, boolean z2, boolean z3) {
        this.mDevice = bluetoothDevice;
        this.mId = i;
        this.mUUID = uuid;
        this.mState = i2;
        this.mNumber = str == null ? "" : str;
        this.mMultiParty = z;
        this.mOutgoing = z2;
        this.mInBandRing = z3;
        this.mCreationElapsedMilli = SystemClock.elapsedRealtime();
    }

    public void setState(int i) {
        this.mState = i;
    }

    public void setNumber(String str) {
        this.mNumber = str;
    }

    public void setMultiParty(boolean z) {
        this.mMultiParty = z;
    }

    public BluetoothDevice getDevice() {
        return this.mDevice;
    }

    public int getId() {
        return this.mId;
    }

    public UUID getUUID() {
        return this.mUUID;
    }

    public int getState() {
        return this.mState;
    }

    public String getNumber() {
        return this.mNumber;
    }

    public long getCreationElapsedMilli() {
        return this.mCreationElapsedMilli;
    }

    public boolean isMultiParty() {
        return this.mMultiParty;
    }

    public boolean isOutgoing() {
        return this.mOutgoing;
    }

    public boolean isInBandRing() {
        return this.mInBandRing;
    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean z) {
        StringBuilder sb = new StringBuilder("BluetoothHeadsetClientCall{mDevice: ");
        sb.append(z ? this.mDevice : Integer.valueOf(this.mDevice.hashCode()));
        sb.append(", mId: ");
        sb.append(this.mId);
        sb.append(", mUUID: ");
        sb.append(this.mUUID);
        sb.append(", mState: ");
        switch (this.mState) {
            case 0:
                sb.append("ACTIVE");
                break;
            case 1:
                sb.append("HELD");
                break;
            case 2:
                sb.append("DIALING");
                break;
            case 3:
                sb.append("ALERTING");
                break;
            case 4:
                sb.append("INCOMING");
                break;
            case 5:
                sb.append("WAITING");
                break;
            case 6:
                sb.append("HELD_BY_RESPONSE_AND_HOLD");
                break;
            case 7:
                sb.append("TERMINATED");
                break;
            default:
                sb.append(this.mState);
                break;
        }
        sb.append(", mNumber: ");
        sb.append(z ? this.mNumber : Integer.valueOf(this.mNumber.hashCode()));
        sb.append(", mMultiParty: ");
        sb.append(this.mMultiParty);
        sb.append(", mOutgoing: ");
        sb.append(this.mOutgoing);
        sb.append(", mInBandRing: ");
        sb.append(this.mInBandRing);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mDevice, 0);
        parcel.writeInt(this.mId);
        parcel.writeString(this.mUUID.toString());
        parcel.writeInt(this.mState);
        parcel.writeString(this.mNumber);
        parcel.writeInt(this.mMultiParty ? 1 : 0);
        parcel.writeInt(this.mOutgoing ? 1 : 0);
        parcel.writeInt(this.mInBandRing ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
