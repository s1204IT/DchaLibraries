package android.bluetooth.mesh;

import android.os.Parcel;
import android.os.Parcelable;

public class OtaOperationParams implements Parcelable {
    public static final Parcelable.Creator<OtaOperationParams> CREATOR = new Parcelable.Creator<OtaOperationParams>() {
        @Override
        public OtaOperationParams createFromParcel(Parcel parcel) {
            return new OtaOperationParams(parcel);
        }

        @Override
        public OtaOperationParams[] newArray(int i) {
            return new OtaOperationParams[i];
        }
    };
    private static final boolean DBG = true;
    private static final String TAG = "BluetoothMesh_OtaOperationParams";
    private int mAppkeyIndex;
    private int mDistributorAddr;
    private long mFwId;
    private int mGroupAddr;
    private boolean mManualApply;
    private int mNodeAddr;
    private byte[] mObjFile;
    private int[] mObjId;
    private int mObjSize;
    private int mOpcode;
    private int[] mUpdaters;
    private int mUpdatersNum;

    public OtaOperationParams() {
    }

    public OtaOperationParams(Parcel parcel) {
        this.mOpcode = parcel.readInt();
        this.mNodeAddr = parcel.readInt();
        this.mObjFile = parcel.createByteArray();
        this.mObjSize = parcel.readInt();
        this.mObjId = parcel.createIntArray();
        this.mFwId = parcel.readLong();
        this.mAppkeyIndex = parcel.readInt();
        this.mDistributorAddr = parcel.readInt();
        this.mGroupAddr = parcel.readInt();
        this.mUpdatersNum = parcel.readInt();
        this.mUpdaters = parcel.createIntArray();
        this.mManualApply = parcel.readInt() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mOpcode);
        parcel.writeInt(this.mNodeAddr);
        parcel.writeByteArray(this.mObjFile);
        parcel.writeInt(this.mObjSize);
        parcel.writeIntArray(this.mObjId);
        parcel.writeLong(this.mFwId);
        parcel.writeInt(this.mAppkeyIndex);
        parcel.writeInt(this.mDistributorAddr);
        parcel.writeInt(this.mGroupAddr);
        parcel.writeInt(this.mUpdatersNum);
        parcel.writeIntArray(this.mUpdaters);
        parcel.writeInt(this.mManualApply ? 1 : 0);
    }

    public void setOtaInitiatorMsgHandler(int i) {
        this.mOpcode = 0;
        this.mAppkeyIndex = i;
    }

    public void setOtaInitiatorFwInfoGet(int i) {
        this.mOpcode = 1;
        this.mNodeAddr = i;
    }

    public void setOtaInitiatorStopParams(long j, int i) {
        this.mOpcode = 3;
        this.mFwId = j;
        this.mDistributorAddr = i;
    }

    public void setOtaInitiatorStartParams(byte[] bArr, int i, int[] iArr, long j, int i2, int i3, int i4, int i5, int[] iArr2, boolean z) {
        this.mOpcode = 2;
        this.mObjFile = bArr;
        this.mObjSize = i;
        this.mObjId = iArr;
        this.mFwId = j;
        this.mAppkeyIndex = i2;
        this.mDistributorAddr = i3;
        this.mGroupAddr = i4;
        this.mUpdatersNum = i5;
        this.mUpdaters = iArr2;
        this.mManualApply = z;
    }

    public void setOtaInitiatorApplyDistribution() {
        this.mOpcode = 4;
    }

    public int getOpcode() {
        return this.mOpcode;
    }

    public int getNodeAddr() {
        return this.mNodeAddr;
    }

    public byte[] getObjFile() {
        return this.mObjFile;
    }

    public int getObjSize() {
        return this.mObjSize;
    }

    public int[] getObjId() {
        return this.mObjId;
    }

    public long getFwId() {
        return this.mFwId;
    }

    public int getAppkeyIndex() {
        return this.mAppkeyIndex;
    }

    public int getDistributorAddr() {
        return this.mDistributorAddr;
    }

    public int getGroupAddr() {
        return this.mGroupAddr;
    }

    public int getUpdatersNum() {
        return this.mUpdatersNum;
    }

    public int[] getUpdaters() {
        return this.mUpdaters;
    }

    public boolean getManualApply() {
        return this.mManualApply;
    }
}
