package android.bluetooth.mesh;

import android.os.Parcel;
import android.os.Parcelable;

public final class BluetoothMeshAccessTxMessage implements Parcelable {
    public static final Parcelable.Creator<BluetoothMeshAccessTxMessage> CREATOR = new Parcelable.Creator<BluetoothMeshAccessTxMessage>() {
        @Override
        public BluetoothMeshAccessTxMessage createFromParcel(Parcel parcel) {
            return new BluetoothMeshAccessTxMessage(parcel);
        }

        @Override
        public BluetoothMeshAccessTxMessage[] newArray(int i) {
            return new BluetoothMeshAccessTxMessage[i];
        }
    };
    private static int[] mBuffer;
    private static int mBufferLen;
    private static int mCompanyId;
    private static int mOpCode;

    public BluetoothMeshAccessTxMessage() {
    }

    public BluetoothMeshAccessTxMessage(int i, int i2, int[] iArr) {
        mOpCode = i;
        mCompanyId = i2;
        mBuffer = iArr;
        mBufferLen = iArr.length;
    }

    public BluetoothMeshAccessTxMessage(int i, int i2, int[] iArr, int i3) {
        mOpCode = i;
        mCompanyId = i2;
        mBuffer = iArr;
        mBufferLen = i3;
    }

    public BluetoothMeshAccessTxMessage(Parcel parcel) {
        mOpCode = parcel.readInt();
        mCompanyId = parcel.readInt();
        mBuffer = parcel.createIntArray();
        mBufferLen = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mOpCode);
        parcel.writeInt(mCompanyId);
        parcel.writeIntArray(mBuffer);
        parcel.writeInt(mBufferLen);
    }

    public void setAccessOpCode(int i, int i2) {
        mOpCode = i;
        mCompanyId = i2;
    }

    public void setBuffer(int[] iArr, int i) {
        mBuffer = iArr;
        mBufferLen = i;
    }

    public int getOpCode() {
        return mOpCode;
    }

    public int getCompanyId() {
        return mCompanyId;
    }

    public int[] getBuffer() {
        return mBuffer;
    }
}
