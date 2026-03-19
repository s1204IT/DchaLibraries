package android.bluetooth.mesh;

import android.os.Parcel;
import android.os.Parcelable;

public final class BluetoothMeshAccessRxMessage implements Parcelable {
    public static final Parcelable.Creator<BluetoothMeshAccessRxMessage> CREATOR = new Parcelable.Creator<BluetoothMeshAccessRxMessage>() {
        @Override
        public BluetoothMeshAccessRxMessage createFromParcel(Parcel parcel) {
            return new BluetoothMeshAccessRxMessage(parcel);
        }

        @Override
        public BluetoothMeshAccessRxMessage[] newArray(int i) {
            return new BluetoothMeshAccessRxMessage[i];
        }
    };
    private static int mAppKeyIndex;
    private static int[] mBuffer;
    private static int mBufferLen;
    private static int mCompanyId;
    private static int mDstAddr;
    private static int mNetKeyIndex;
    private static int mOpCode;
    private static int mRssi;
    private static int mSrcAddr;
    private static int mTtl;

    public BluetoothMeshAccessRxMessage() {
    }

    public BluetoothMeshAccessRxMessage(int i, int i2, int[] iArr, int i3, int i4, int i5, int i6, int i7, int i8, int i9) {
        mOpCode = i;
        mCompanyId = i2;
        mBuffer = iArr;
        mBufferLen = i3;
        mSrcAddr = i4;
        mDstAddr = i5;
        mAppKeyIndex = i6;
        mNetKeyIndex = i7;
        mRssi = i8;
        mTtl = i9;
    }

    public BluetoothMeshAccessRxMessage(Parcel parcel) {
        mOpCode = parcel.readInt();
        mCompanyId = parcel.readInt();
        mBuffer = parcel.createIntArray();
        mBufferLen = parcel.readInt();
        mSrcAddr = parcel.readInt();
        mDstAddr = parcel.readInt();
        mAppKeyIndex = parcel.readInt();
        mNetKeyIndex = parcel.readInt();
        mRssi = parcel.readInt();
        mTtl = parcel.readInt();
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
        parcel.writeInt(mSrcAddr);
        parcel.writeInt(mDstAddr);
        parcel.writeInt(mAppKeyIndex);
        parcel.writeInt(mNetKeyIndex);
        parcel.writeInt(mRssi);
        parcel.writeInt(mTtl);
    }

    public void setAccessOpCode(int i, int i2) {
        mOpCode = i;
        mCompanyId = i2;
    }

    public void setMetaData(int i, int i2, int i3, int i4, int i5, int i6) {
        mSrcAddr = i;
        mDstAddr = i2;
        mAppKeyIndex = i3;
        mNetKeyIndex = i4;
        mRssi = i5;
        mTtl = i6;
    }

    public void setBuffer(int[] iArr) {
        mBuffer = iArr;
        mBufferLen = iArr.length;
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

    public int getSrcAddr() {
        return mSrcAddr;
    }

    public int getDstAddr() {
        return mDstAddr;
    }

    public int getAppKeyIndex() {
        return mAppKeyIndex;
    }

    public int getNetKeyIndex() {
        return mNetKeyIndex;
    }

    public int getRssi() {
        return mRssi;
    }

    public int getTtl() {
        return mTtl;
    }
}
