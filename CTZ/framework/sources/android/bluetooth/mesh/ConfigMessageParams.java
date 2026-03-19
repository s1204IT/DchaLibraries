package android.bluetooth.mesh;

import android.os.Parcel;
import android.os.Parcelable;

public class ConfigMessageParams implements Parcelable {
    public static final Parcelable.Creator<ConfigMessageParams> CREATOR = new Parcelable.Creator<ConfigMessageParams>() {
        @Override
        public ConfigMessageParams createFromParcel(Parcel parcel) {
            return new ConfigMessageParams(parcel);
        }

        @Override
        public ConfigMessageParams[] newArray(int i) {
            return new ConfigMessageParams[i];
        }
    };
    private int TTL;
    private int addressType;
    private int addressValue;
    private int[] appkey;
    private int appkeyIndex;
    private int beacon;
    private int count;
    private int countLog;
    private int destination;
    private int elementAddress;
    private int features;
    private boolean friendshipCredentialFlag;
    private int gattProxy;
    private int identity;
    private int intervalSteps;
    private int meshFriend;
    private long modelId;
    private int[] netkey;
    private int netkeyIndex;
    private int page;
    private int periodLog;
    private int publishPeriod;
    private int publishTTL;
    private int relay;
    private int retransmitCount;
    private int retransmitIntervalSteps;
    private int source;
    private int transition;
    private int[] virtualUUID;

    public ConfigMessageParams() {
    }

    public ConfigMessageParams(Parcel parcel) {
        this.beacon = parcel.readInt();
        this.page = parcel.readInt();
        this.TTL = parcel.readInt();
        this.gattProxy = parcel.readInt();
        this.meshFriend = parcel.readInt();
        this.relay = parcel.readInt();
        this.retransmitCount = parcel.readInt();
        this.retransmitIntervalSteps = parcel.readInt();
        this.elementAddress = parcel.readInt();
        this.modelId = parcel.readLong();
        this.friendshipCredentialFlag = parcel.readInt() != 0;
        this.publishTTL = parcel.readInt();
        this.publishPeriod = parcel.readInt();
        this.addressType = parcel.readInt();
        this.addressValue = parcel.readInt();
        this.virtualUUID = parcel.createIntArray();
        this.appkeyIndex = parcel.readInt();
        this.netkeyIndex = parcel.readInt();
        this.appkey = parcel.createIntArray();
        this.netkey = parcel.createIntArray();
        this.identity = parcel.readInt();
        this.transition = parcel.readInt();
        this.destination = parcel.readInt();
        this.countLog = parcel.readInt();
        this.periodLog = parcel.readInt();
        this.features = parcel.readInt();
        this.source = parcel.readInt();
        this.count = parcel.readInt();
        this.intervalSteps = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.beacon);
        parcel.writeInt(this.page);
        parcel.writeInt(this.TTL);
        parcel.writeInt(this.gattProxy);
        parcel.writeInt(this.meshFriend);
        parcel.writeInt(this.relay);
        parcel.writeInt(this.retransmitCount);
        parcel.writeInt(this.retransmitIntervalSteps);
        parcel.writeInt(this.elementAddress);
        parcel.writeLong(this.modelId);
        parcel.writeInt(this.friendshipCredentialFlag ? 1 : 0);
        parcel.writeInt(this.publishTTL);
        parcel.writeInt(this.publishPeriod);
        parcel.writeInt(this.addressType);
        parcel.writeInt(this.addressValue);
        parcel.writeIntArray(this.virtualUUID);
        parcel.writeInt(this.appkeyIndex);
        parcel.writeInt(this.netkeyIndex);
        parcel.writeIntArray(this.appkey);
        parcel.writeIntArray(this.netkey);
        parcel.writeInt(this.identity);
        parcel.writeInt(this.transition);
        parcel.writeInt(this.destination);
        parcel.writeInt(this.countLog);
        parcel.writeInt(this.periodLog);
        parcel.writeInt(this.features);
        parcel.writeInt(this.source);
        parcel.writeInt(this.count);
        parcel.writeInt(this.intervalSteps);
    }

    public void setConfigBeaconGetParam() {
    }

    public void setConfigBeaconSetParam(int i) {
        this.beacon = i;
    }

    public void setConfigCompositionDataGetParam(int i) {
        this.page = i;
    }

    public void setConfigDefaultTTLGetParam() {
    }

    public void setConfigDefaultTTLSetParam(int i) {
        this.TTL = i;
    }

    public void setConfigGattProxyGetParam() {
    }

    public void setConfigGattProxySetParam(int i) {
        this.gattProxy = i;
    }

    public void setConfigFriendGetParam() {
    }

    public void setConfigFriendSetParam(int i) {
        this.meshFriend = i;
    }

    public void setConfigRelayGetParam() {
    }

    public void setConfigRelaySetParam(int i, int i2, int i3) {
        this.relay = i;
        this.retransmitCount = i2;
        this.retransmitIntervalSteps = i3;
    }

    public void setConfigModelPubGetParam(int i, long j) {
        this.elementAddress = i;
        this.modelId = j;
    }

    public void setConfigModelPubSetParam(int i, int i2, int i3, int[] iArr, int i4, boolean z, int i5, int i6, int i7, int i8, long j) {
        this.elementAddress = i;
        this.addressType = i2;
        this.addressValue = i3;
        this.virtualUUID = iArr;
        this.appkeyIndex = i4;
        this.friendshipCredentialFlag = z;
        this.publishTTL = i5;
        this.publishPeriod = i6;
        this.retransmitCount = i7;
        this.retransmitIntervalSteps = i8;
        this.modelId = j;
    }

    public void setConfigModelSubAddParam(int i, int i2, int i3, int[] iArr, long j) {
        this.elementAddress = i;
        this.addressType = i2;
        this.addressValue = i3;
        this.virtualUUID = iArr;
        this.modelId = j;
    }

    public void setConfigModelSubDelParam(int i, int i2, int i3, int[] iArr, long j) {
        this.elementAddress = i;
        this.addressType = i2;
        this.addressValue = i3;
        this.virtualUUID = iArr;
        this.modelId = j;
    }

    public void setConfigModelSubOwParam(int i, int i2, int i3, int[] iArr, long j) {
        this.elementAddress = i;
        this.addressType = i2;
        this.addressValue = i3;
        this.virtualUUID = iArr;
        this.modelId = j;
    }

    public void setConfigModelSubDelAllParam(int i, long j) {
        this.elementAddress = i;
        this.modelId = j;
    }

    public void setConfigSigModelSubGetParam(int i, long j) {
        this.elementAddress = i;
        this.modelId = j;
    }

    public void setConfigVendorModelSubGetParam(int i, long j) {
        this.elementAddress = i;
        this.modelId = j;
    }

    public void setConfigNetkeyAddParam(int i, int[] iArr) {
        this.netkeyIndex = i;
        this.netkey = iArr;
    }

    public void setConfigNetkeyUpdateParam(int i, int[] iArr) {
        this.netkeyIndex = i;
        this.netkey = iArr;
    }

    public void setConfigNetkeyDelParam(int i) {
        this.netkeyIndex = i;
    }

    public void setConfigNetkeyGetParam() {
    }

    public void setConfigAppkeyAddParam(int i, int i2, int[] iArr) {
        this.netkeyIndex = i;
        this.appkeyIndex = i2;
        this.appkey = iArr;
    }

    public void setConfigAppkeyUpdateParam(int i, int i2, int[] iArr) {
        this.netkeyIndex = i;
        this.appkeyIndex = i2;
        this.appkey = iArr;
    }

    public void setConfigAppkeyDelParam(int i, int i2) {
        this.netkeyIndex = i;
        this.appkeyIndex = i2;
    }

    public void setConfigAppkeyGetParam(int i) {
        this.netkeyIndex = i;
    }

    public void setConfigModelAppBindParam(int i, int i2, long j) {
        this.elementAddress = i;
        this.appkeyIndex = i2;
        this.modelId = j;
    }

    public void setConfigModelAppUnbindParam(int i, int i2, long j) {
        this.elementAddress = i;
        this.appkeyIndex = i2;
        this.modelId = j;
    }

    public void setConfigSigModelAppGetParam(int i, long j) {
        this.elementAddress = i;
        this.modelId = j;
    }

    public void setConfigVendorModelAppGetParam(int i, long j) {
        this.elementAddress = i;
        this.modelId = j;
    }

    public void setConfigNodeIdentityGetParam(int i) {
        this.netkeyIndex = i;
    }

    public void setConfigNodeIdentitySetParam(int i, int i2) {
        this.netkeyIndex = i;
        this.identity = i2;
    }

    public void setConfigNodeResetParam() {
    }

    public void setConfigKeyRefreshPhaseGetParam(int i) {
        this.netkeyIndex = i;
    }

    public void setConfigKeyRefreshPhaseSetParam(int i, int i2) {
        this.netkeyIndex = i;
        this.transition = i2;
    }

    public void setConfigHbPubGetParam() {
    }

    public void setConfigHbPubSetParam(int i, int i2, int i3, int i4, int i5, int i6) {
        this.destination = i;
        this.countLog = i2;
        this.periodLog = i3;
        this.TTL = i4;
        this.features = i5;
        this.netkeyIndex = i6;
    }

    public void setConfigHbSubGetParam() {
    }

    public void setConfigHbSubSetParam(int i, int i2, int i3) {
        this.source = i;
        this.destination = i2;
        this.periodLog = i3;
    }

    public void setConfigNetworkTransmitGetParam() {
    }

    public void setConfigNetworkTransmitSetParam(int i, int i2) {
        this.count = i;
        this.intervalSteps = i2;
    }

    public int getBeacon() {
        return this.beacon;
    }

    public int getPage() {
        return this.page;
    }

    public int getTTL() {
        return this.TTL;
    }

    public int getGattProxy() {
        return this.gattProxy;
    }

    public int getMeshFriend() {
        return this.meshFriend;
    }

    public int getRelay() {
        return this.relay;
    }

    public int getRetransmitCount() {
        return this.retransmitCount;
    }

    public int getRetransmitIntervalSteps() {
        return this.retransmitIntervalSteps;
    }

    public int getElementAddress() {
        return this.elementAddress;
    }

    public long getModelId() {
        return this.modelId;
    }

    public boolean getFriendshipCredentialFlag() {
        return this.friendshipCredentialFlag;
    }

    public int getPublishTTL() {
        return this.publishTTL;
    }

    public int getPublishPeriod() {
        return this.publishPeriod;
    }

    public int getAddressType() {
        return this.addressType;
    }

    public int getAddressValue() {
        return this.addressValue;
    }

    public int[] getVirtualUUID() {
        return this.virtualUUID;
    }

    public int getAppkeyIndex() {
        return this.appkeyIndex;
    }

    public int getNetkeyIndex() {
        return this.netkeyIndex;
    }

    public int[] getAppkey() {
        return this.appkey;
    }

    public int[] getNetkey() {
        return this.netkey;
    }

    public int getIdentity() {
        return this.identity;
    }

    public int getTransition() {
        return this.transition;
    }

    public int getDestination() {
        return this.destination;
    }

    public int getCountLog() {
        return this.countLog;
    }

    public int getPeriodLog() {
        return this.periodLog;
    }

    public int getFeatures() {
        return this.features;
    }

    public int getSource() {
        return this.source;
    }

    public int getCount() {
        return this.count;
    }

    public int getIntervalSteps() {
        return this.intervalSteps;
    }
}
