package android.bluetooth.mesh;

import android.os.Parcel;
import android.os.Parcelable;

public class MeshInitParams implements Parcelable {
    public static final Parcelable.Creator<MeshInitParams> CREATOR = new Parcelable.Creator<MeshInitParams>() {
        @Override
        public MeshInitParams createFromParcel(Parcel parcel) {
            return new MeshInitParams(parcel);
        }

        @Override
        public MeshInitParams[] newArray(int i) {
            return new MeshInitParams[i];
        }
    };
    private int[] mCustomizeParams;
    private int mDefaultTtl;
    private int[] mDeviceUuid;
    private long mFeatureMask;
    private int[] mFriendInitParams;
    private int mOobInfo;
    private int[] mProvisioneeParams;
    private int mRole;
    private byte[] mUri;

    public MeshInitParams() {
    }

    public MeshInitParams(Parcel parcel) {
        this.mRole = parcel.readInt();
        this.mProvisioneeParams = parcel.createIntArray();
        this.mDeviceUuid = parcel.createIntArray();
        this.mOobInfo = parcel.readInt();
        this.mDefaultTtl = parcel.readInt();
        this.mUri = parcel.createByteArray();
        this.mFeatureMask = parcel.readLong();
        this.mFriendInitParams = parcel.createIntArray();
        this.mCustomizeParams = parcel.createIntArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mRole);
        parcel.writeIntArray(this.mProvisioneeParams);
        parcel.writeIntArray(this.mDeviceUuid);
        parcel.writeInt(this.mOobInfo);
        parcel.writeInt(this.mDefaultTtl);
        parcel.writeByteArray(this.mUri);
        parcel.writeLong(this.mFeatureMask);
        parcel.writeIntArray(this.mFriendInitParams);
        parcel.writeIntArray(this.mCustomizeParams);
    }

    public class ProvisioneeParams {
        private int mAlgorithms;
        private int mInputOobAction;
        private int mInputOobSize;
        private int mNumberOfElements;
        private int mOutputOobAction;
        private int mOutputOobSize;
        private int mPublicKeyType;
        private int mStaticOobType;

        public ProvisioneeParams(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            this.mNumberOfElements = i;
            this.mAlgorithms = i2;
            this.mPublicKeyType = i3;
            this.mStaticOobType = i4;
            this.mOutputOobSize = i5;
            this.mOutputOobAction = i6;
            this.mInputOobSize = i7;
            this.mInputOobAction = i8;
        }

        public int getNumberOfElements() {
            return this.mNumberOfElements;
        }

        public int getAlgorithms() {
            return this.mAlgorithms;
        }

        public int getPublicKeyType() {
            return this.mPublicKeyType;
        }

        public int getStaticOobType() {
            return this.mStaticOobType;
        }

        public int getOutputOobSize() {
            return this.mOutputOobSize;
        }

        public int getOutputOobAction() {
            return this.mOutputOobAction;
        }

        public int getInputOobSize() {
            return this.mInputOobSize;
        }

        public int getInputOobAction() {
            return this.mInputOobAction;
        }
    }

    public class FriendInitParams {
        private int mLpnNumber;
        private int mQueueSize;
        private int mSubscriptionListSize;

        public FriendInitParams(int i, int i2, int i3) {
            this.mLpnNumber = i;
            this.mQueueSize = i2;
            this.mSubscriptionListSize = i3;
        }

        public int getLpnNumber() {
            return this.mLpnNumber;
        }

        public int getQueueSize() {
            return this.mQueueSize;
        }

        public int getSubscriptionListSize() {
            return this.mSubscriptionListSize;
        }
    }

    public class CustomizeParams {
        private int mMaxRemoteNodeCnt;
        private int mSave2flash;

        public CustomizeParams(int i, int i2) {
            this.mMaxRemoteNodeCnt = i;
            this.mSave2flash = i2;
        }

        public int getMaxRemoteNodeCnt() {
            return this.mMaxRemoteNodeCnt;
        }

        public int getSave2flash() {
            return this.mSave2flash;
        }
    }

    public void setRole(int i) {
        this.mRole = i;
    }

    public void setProvisioneeParams(ProvisioneeParams provisioneeParams) {
        this.mProvisioneeParams = new int[]{provisioneeParams.getNumberOfElements(), provisioneeParams.getAlgorithms(), provisioneeParams.getPublicKeyType(), provisioneeParams.getStaticOobType(), provisioneeParams.getOutputOobSize(), provisioneeParams.getOutputOobAction(), provisioneeParams.getInputOobSize(), provisioneeParams.getInputOobAction()};
    }

    public void setDeviceUuid(int[] iArr) {
        this.mDeviceUuid = iArr;
    }

    public void setOobInfo(int i) {
        this.mOobInfo = i;
    }

    public void setDefaultTtl(int i) {
        this.mDefaultTtl = i;
    }

    public void setUri(byte[] bArr) {
        this.mUri = bArr;
    }

    public void setFeatureMask(long j) {
        this.mFeatureMask = j;
    }

    public void setFriendInitParams(FriendInitParams friendInitParams) {
        this.mFriendInitParams = new int[]{friendInitParams.getLpnNumber(), friendInitParams.getQueueSize(), friendInitParams.getSubscriptionListSize()};
    }

    public void setCustomizeParams(CustomizeParams customizeParams) {
        this.mCustomizeParams = new int[]{customizeParams.getMaxRemoteNodeCnt(), customizeParams.getSave2flash()};
    }

    public int getRole() {
        return this.mRole;
    }

    public int[] getProvisioneeParams() {
        return this.mProvisioneeParams;
    }

    public int[] getDeviceUuid() {
        return this.mDeviceUuid;
    }

    public int getOobInfo() {
        return this.mOobInfo;
    }

    public int getDefaultTtl() {
        return this.mDefaultTtl;
    }

    public byte[] getUri() {
        return this.mUri;
    }

    public long getFeatureMask() {
        return this.mFeatureMask;
    }

    public int[] getFriendInitParams() {
        return this.mFriendInitParams;
    }

    public int[] getCustomizeParams() {
        return this.mCustomizeParams;
    }
}
