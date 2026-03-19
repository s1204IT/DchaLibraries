package android.bluetooth.mesh;

import android.bluetooth.BluetoothMesh;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Spanned;
import android.util.Log;
import java.util.Arrays;

public class MeshModel implements Parcelable {
    public static final Parcelable.Creator<MeshModel> CREATOR = new Parcelable.Creator<MeshModel>() {
        @Override
        public MeshModel createFromParcel(Parcel parcel) {
            return new MeshModel(parcel);
        }

        @Override
        public MeshModel[] newArray(int i) {
            return new MeshModel[i];
        }
    };
    private static final boolean DBG = true;
    private static final String TAG = "BluetoothMesh_MeshModel";
    private static final boolean VDBG = true;
    protected int mCompanyID;
    protected ModelConfigMessage mConfigMsg;
    protected int mElementIndex;
    protected BluetoothMesh mMesh;
    protected int mModelHandle;
    protected long mModelID;
    protected int mModelOpcode;
    protected int mOpcodeCount;
    protected ModelTxMessage mTxMsg;
    protected int[] mVendorMsgOpcodes;

    public MeshModel(BluetoothMesh bluetoothMesh) {
        this.mMesh = bluetoothMesh;
    }

    public MeshModel(BluetoothMesh bluetoothMesh, int i) {
        this.mMesh = bluetoothMesh;
        this.mModelOpcode = i;
    }

    public MeshModel(BluetoothMesh bluetoothMesh, int i, int i2) {
        this.mMesh = bluetoothMesh;
        this.mModelOpcode = i;
        this.mElementIndex = i2;
    }

    public MeshModel(Parcel parcel) {
        this.mModelOpcode = parcel.readInt();
        this.mModelHandle = parcel.readInt();
        this.mElementIndex = parcel.readInt();
        this.mModelID = parcel.readLong();
        this.mVendorMsgOpcodes = parcel.createIntArray();
        this.mCompanyID = parcel.readInt();
        this.mOpcodeCount = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mModelOpcode);
        parcel.writeInt(this.mModelHandle);
        parcel.writeInt(this.mElementIndex);
        parcel.writeLong(this.mModelID);
        parcel.writeIntArray(this.mVendorMsgOpcodes);
        parcel.writeInt(this.mCompanyID);
        parcel.writeInt(this.mOpcodeCount);
    }

    public class ModelTxMessage {
        protected int mAppKeyIndex;
        protected int mDst;
        protected int mDstAddrType;
        protected int mMsgOpCode;
        protected int mNetKeyIndex;
        protected int mSrc;
        protected int mTtl;
        protected int[] mVirtualUUID;

        public ModelTxMessage() {
        }
    }

    public class ModelConfigMessage {
        protected int mDst;
        protected int mMsgOpCode;
        protected int mNetKeyIndex;
        protected int mSrc;
        protected int mTtl;

        public ModelConfigMessage() {
        }
    }

    public void setTxMessageHeader(int i, int i2, int[] iArr, int i3, int i4, int i5, int i6, int i7) {
        if (this.mTxMsg == null) {
            this.mTxMsg = new ModelTxMessage();
        }
        this.mTxMsg.mDstAddrType = i;
        this.mTxMsg.mDst = i2;
        this.mTxMsg.mVirtualUUID = iArr;
        this.mTxMsg.mSrc = i3;
        this.mTxMsg.mTtl = i4;
        this.mTxMsg.mNetKeyIndex = i5;
        this.mTxMsg.mAppKeyIndex = i6;
        this.mTxMsg.mMsgOpCode = i7;
    }

    public void setConfigMessageHeader(int i, int i2, int i3, int i4, int i5) {
        if (this.mConfigMsg == null) {
            this.mConfigMsg = new ModelConfigMessage();
        }
        this.mConfigMsg.mSrc = i;
        this.mConfigMsg.mDst = i2;
        this.mConfigMsg.mTtl = i3;
        this.mConfigMsg.mNetKeyIndex = i4;
        this.mConfigMsg.mMsgOpCode = i5;
    }

    protected void modelSendPacket() {
        modelSendPacket((int[]) null);
    }

    protected void modelSendPacket(int i) {
        modelSendPacket(new int[]{i});
    }

    protected void modelSendPacket(int i, int i2) {
        modelSendPacket(new int[]{i, i2});
    }

    protected void modelSendPacket(int i, int i2, int i3) {
        modelSendPacket(new int[]{i, i2, i3});
    }

    protected void modelSendPacket(int i, int i2, int i3, int i4) {
        modelSendPacket(new int[]{i, i2, i3, i4});
    }

    protected void modelSendPacket(int i, int i2, int[] iArr) {
        int length;
        if (iArr != null) {
            length = iArr.length;
        } else {
            length = 0;
        }
        int[] iArr2 = new int[2 + length];
        iArr2[0] = i;
        iArr2[1] = i2;
        if (iArr != null) {
            System.arraycopy(iArr, 0, iArr2, 2, length);
        }
        modelSendPacket(iArr2);
    }

    protected void modelSendPacket(int[] iArr) {
        int[] iArr2;
        int i;
        int[] iArr3;
        Log.d(TAG, "modelSendPacket: params=" + Arrays.toString(iArr));
        if (this.mTxMsg == null) {
            Log.e(TAG, "TxMsg is null, should create header first");
            return;
        }
        if (this.mMesh == null) {
            Log.e(TAG, "BluetoothMesh is null, cannot send");
            return;
        }
        int length = iArr == null ? 0 : iArr.length;
        if (this.mTxMsg.mMsgOpCode < 127) {
            int[] iArr4 = new int[length + 1];
            iArr4[0] = this.mTxMsg.mMsgOpCode & 255;
            Log.d(TAG, "modelSendPacket  1-octet opcode = " + this.mTxMsg.mMsgOpCode);
            iArr3 = iArr4;
            i = 1;
        } else if (this.mTxMsg.mMsgOpCode <= 127 || this.mTxMsg.mMsgOpCode >= 49152) {
            if (this.mTxMsg.mMsgOpCode >= 49152) {
                i = 3;
                iArr2 = new int[length + 3];
                iArr2[0] = (this.mTxMsg.mMsgOpCode & Spanned.SPAN_PRIORITY) >> 16;
                iArr2[1] = this.mCompanyID & 255;
                iArr2[2] = (this.mCompanyID & 65280) >> 8;
                Log.d(TAG, "modelSendPacket  3-octet opcode = " + this.mTxMsg.mMsgOpCode);
            } else {
                iArr2 = null;
                Log.d(TAG, "modelSendPacket  should never here!! ");
                i = 0;
            }
            iArr3 = iArr2;
        } else {
            int[] iArr5 = new int[length + 2];
            iArr5[0] = (this.mTxMsg.mMsgOpCode & 65280) >> 8;
            iArr5[1] = this.mTxMsg.mMsgOpCode & 255;
            Log.d(TAG, "modelSendPacket  2-octet opcode = " + this.mTxMsg.mMsgOpCode);
            iArr3 = iArr5;
            i = 2;
        }
        if (iArr != null && iArr3 != null) {
            System.arraycopy(iArr, 0, iArr3, i, iArr.length);
        }
        this.mMesh.sendPacket(this.mTxMsg.mDst, this.mTxMsg.mSrc, this.mTxMsg.mTtl, this.mTxMsg.mNetKeyIndex, this.mTxMsg.mAppKeyIndex, iArr3);
    }

    protected void modelSendConfigMessage() {
        if (this.mConfigMsg == null) {
            Log.e(TAG, "TxMsg is null, should create header first");
        } else if (this.mMesh == null) {
            Log.e(TAG, "BluetoothMesh is null, cannot send");
        } else {
            this.mMesh.sendConfigMessage(this.mConfigMsg.mDst, this.mConfigMsg.mSrc, this.mConfigMsg.mTtl, this.mConfigMsg.mNetKeyIndex, this.mConfigMsg.mMsgOpCode, null);
        }
    }

    protected void modelSendConfigMessage(ConfigMessageParams configMessageParams) {
        if (this.mConfigMsg == null) {
            Log.e(TAG, "TxMsg is null, should create header first");
        } else if (this.mMesh == null) {
            Log.e(TAG, "BluetoothMesh is null, cannot send");
        } else {
            this.mMesh.sendConfigMessage(this.mConfigMsg.mDst, this.mConfigMsg.mSrc, this.mConfigMsg.mTtl, this.mConfigMsg.mNetKeyIndex, this.mConfigMsg.mMsgOpCode, configMessageParams);
        }
    }

    protected int[] TwoOctetsToArray(int i) {
        if (i > 65535 || i < 0) {
            Log.w(TAG, "Param should be 0x0000~0xFFFF. Wrong param 0x" + Integer.toHexString(i) + ", will keep the last 2 bytes 0x" + Integer.toHexString(65535 & i));
        }
        return new int[]{i & 255, (i & 65280) >> 8};
    }

    public void setModelOpcode(int i) {
        this.mModelOpcode = i;
    }

    public void setElementIndex(int i) {
        this.mElementIndex = i;
    }

    public void setModelHandle(int i) {
        this.mModelHandle = i;
    }

    public void setModelID(long j) {
        this.mModelID = j;
    }

    public void setVendorMsgOpcodes(int[] iArr) {
        this.mVendorMsgOpcodes = iArr;
    }

    public void setCompanyID(int i) {
        this.mCompanyID = i;
    }

    public void setOpcodeCount(int i) {
        this.mOpcodeCount = i;
    }

    public int getModelOpcode() {
        return this.mModelOpcode;
    }

    public int getElementIndex() {
        return this.mElementIndex;
    }

    public int getModelHandle() {
        return this.mModelHandle;
    }

    public long getModelID() {
        return this.mModelID;
    }

    public int[] getVendorMsgOpcodes() {
        return this.mVendorMsgOpcodes;
    }

    public int getCompanyID() {
        return this.mCompanyID;
    }

    public int getOpcodeCount() {
        return this.mOpcodeCount;
    }

    public void onMsgHandler(int i, BluetoothMeshAccessRxMessage bluetoothMeshAccessRxMessage) {
    }

    public void onPublishTimeoutCallback(int i) {
    }
}
