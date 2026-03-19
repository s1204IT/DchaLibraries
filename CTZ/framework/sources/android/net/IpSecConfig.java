package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.annotations.VisibleForTesting;

public final class IpSecConfig implements Parcelable {
    public static final Parcelable.Creator<IpSecConfig> CREATOR = new Parcelable.Creator<IpSecConfig>() {
        @Override
        public IpSecConfig createFromParcel(Parcel parcel) {
            return new IpSecConfig(parcel);
        }

        @Override
        public IpSecConfig[] newArray(int i) {
            return new IpSecConfig[i];
        }
    };
    private static final String TAG = "IpSecConfig";
    private IpSecAlgorithm mAuthenticatedEncryption;
    private IpSecAlgorithm mAuthentication;
    private String mDestinationAddress;
    private int mEncapRemotePort;
    private int mEncapSocketResourceId;
    private int mEncapType;
    private IpSecAlgorithm mEncryption;
    private int mMarkMask;
    private int mMarkValue;
    private int mMode;
    private int mNattKeepaliveInterval;
    private Network mNetwork;
    private String mSourceAddress;
    private int mSpiResourceId;

    public void setMode(int i) {
        this.mMode = i;
    }

    public void setSourceAddress(String str) {
        this.mSourceAddress = str;
    }

    public void setDestinationAddress(String str) {
        this.mDestinationAddress = str;
    }

    public void setSpiResourceId(int i) {
        this.mSpiResourceId = i;
    }

    public void setEncryption(IpSecAlgorithm ipSecAlgorithm) {
        this.mEncryption = ipSecAlgorithm;
    }

    public void setAuthentication(IpSecAlgorithm ipSecAlgorithm) {
        this.mAuthentication = ipSecAlgorithm;
    }

    public void setAuthenticatedEncryption(IpSecAlgorithm ipSecAlgorithm) {
        this.mAuthenticatedEncryption = ipSecAlgorithm;
    }

    public void setNetwork(Network network) {
        this.mNetwork = network;
    }

    public void setEncapType(int i) {
        this.mEncapType = i;
    }

    public void setEncapSocketResourceId(int i) {
        this.mEncapSocketResourceId = i;
    }

    public void setEncapRemotePort(int i) {
        this.mEncapRemotePort = i;
    }

    public void setNattKeepaliveInterval(int i) {
        this.mNattKeepaliveInterval = i;
    }

    public void setMarkValue(int i) {
        this.mMarkValue = i;
    }

    public void setMarkMask(int i) {
        this.mMarkMask = i;
    }

    public int getMode() {
        return this.mMode;
    }

    public String getSourceAddress() {
        return this.mSourceAddress;
    }

    public int getSpiResourceId() {
        return this.mSpiResourceId;
    }

    public String getDestinationAddress() {
        return this.mDestinationAddress;
    }

    public IpSecAlgorithm getEncryption() {
        return this.mEncryption;
    }

    public IpSecAlgorithm getAuthentication() {
        return this.mAuthentication;
    }

    public IpSecAlgorithm getAuthenticatedEncryption() {
        return this.mAuthenticatedEncryption;
    }

    public Network getNetwork() {
        return this.mNetwork;
    }

    public int getEncapType() {
        return this.mEncapType;
    }

    public int getEncapSocketResourceId() {
        return this.mEncapSocketResourceId;
    }

    public int getEncapRemotePort() {
        return this.mEncapRemotePort;
    }

    public int getNattKeepaliveInterval() {
        return this.mNattKeepaliveInterval;
    }

    public int getMarkValue() {
        return this.mMarkValue;
    }

    public int getMarkMask() {
        return this.mMarkMask;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mMode);
        parcel.writeString(this.mSourceAddress);
        parcel.writeString(this.mDestinationAddress);
        parcel.writeParcelable(this.mNetwork, i);
        parcel.writeInt(this.mSpiResourceId);
        parcel.writeParcelable(this.mEncryption, i);
        parcel.writeParcelable(this.mAuthentication, i);
        parcel.writeParcelable(this.mAuthenticatedEncryption, i);
        parcel.writeInt(this.mEncapType);
        parcel.writeInt(this.mEncapSocketResourceId);
        parcel.writeInt(this.mEncapRemotePort);
        parcel.writeInt(this.mNattKeepaliveInterval);
        parcel.writeInt(this.mMarkValue);
        parcel.writeInt(this.mMarkMask);
    }

    @VisibleForTesting
    public IpSecConfig() {
        this.mMode = 0;
        this.mSourceAddress = "";
        this.mDestinationAddress = "";
        this.mSpiResourceId = -1;
        this.mEncapType = 0;
        this.mEncapSocketResourceId = -1;
    }

    @VisibleForTesting
    public IpSecConfig(IpSecConfig ipSecConfig) {
        this.mMode = 0;
        this.mSourceAddress = "";
        this.mDestinationAddress = "";
        this.mSpiResourceId = -1;
        this.mEncapType = 0;
        this.mEncapSocketResourceId = -1;
        this.mMode = ipSecConfig.mMode;
        this.mSourceAddress = ipSecConfig.mSourceAddress;
        this.mDestinationAddress = ipSecConfig.mDestinationAddress;
        this.mNetwork = ipSecConfig.mNetwork;
        this.mSpiResourceId = ipSecConfig.mSpiResourceId;
        this.mEncryption = ipSecConfig.mEncryption;
        this.mAuthentication = ipSecConfig.mAuthentication;
        this.mAuthenticatedEncryption = ipSecConfig.mAuthenticatedEncryption;
        this.mEncapType = ipSecConfig.mEncapType;
        this.mEncapSocketResourceId = ipSecConfig.mEncapSocketResourceId;
        this.mEncapRemotePort = ipSecConfig.mEncapRemotePort;
        this.mNattKeepaliveInterval = ipSecConfig.mNattKeepaliveInterval;
        this.mMarkValue = ipSecConfig.mMarkValue;
        this.mMarkMask = ipSecConfig.mMarkMask;
    }

    private IpSecConfig(Parcel parcel) {
        this.mMode = 0;
        this.mSourceAddress = "";
        this.mDestinationAddress = "";
        this.mSpiResourceId = -1;
        this.mEncapType = 0;
        this.mEncapSocketResourceId = -1;
        this.mMode = parcel.readInt();
        this.mSourceAddress = parcel.readString();
        this.mDestinationAddress = parcel.readString();
        this.mNetwork = (Network) parcel.readParcelable(Network.class.getClassLoader());
        this.mSpiResourceId = parcel.readInt();
        this.mEncryption = (IpSecAlgorithm) parcel.readParcelable(IpSecAlgorithm.class.getClassLoader());
        this.mAuthentication = (IpSecAlgorithm) parcel.readParcelable(IpSecAlgorithm.class.getClassLoader());
        this.mAuthenticatedEncryption = (IpSecAlgorithm) parcel.readParcelable(IpSecAlgorithm.class.getClassLoader());
        this.mEncapType = parcel.readInt();
        this.mEncapSocketResourceId = parcel.readInt();
        this.mEncapRemotePort = parcel.readInt();
        this.mNattKeepaliveInterval = parcel.readInt();
        this.mMarkValue = parcel.readInt();
        this.mMarkMask = parcel.readInt();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{mMode=");
        sb.append(this.mMode == 1 ? "TUNNEL" : "TRANSPORT");
        sb.append(", mSourceAddress=");
        sb.append(this.mSourceAddress);
        sb.append(", mDestinationAddress=");
        sb.append(this.mDestinationAddress);
        sb.append(", mNetwork=");
        sb.append(this.mNetwork);
        sb.append(", mEncapType=");
        sb.append(this.mEncapType);
        sb.append(", mEncapSocketResourceId=");
        sb.append(this.mEncapSocketResourceId);
        sb.append(", mEncapRemotePort=");
        sb.append(this.mEncapRemotePort);
        sb.append(", mNattKeepaliveInterval=");
        sb.append(this.mNattKeepaliveInterval);
        sb.append("{mSpiResourceId=");
        sb.append(this.mSpiResourceId);
        sb.append(", mEncryption=");
        sb.append(this.mEncryption);
        sb.append(", mAuthentication=");
        sb.append(this.mAuthentication);
        sb.append(", mAuthenticatedEncryption=");
        sb.append(this.mAuthenticatedEncryption);
        sb.append(", mMarkValue=");
        sb.append(this.mMarkValue);
        sb.append(", mMarkMask=");
        sb.append(this.mMarkMask);
        sb.append("}");
        return sb.toString();
    }

    @VisibleForTesting
    public static boolean equals(IpSecConfig ipSecConfig, IpSecConfig ipSecConfig2) {
        if (ipSecConfig == null || ipSecConfig2 == null) {
            return ipSecConfig == ipSecConfig2;
        }
        if (ipSecConfig.mMode == ipSecConfig2.mMode && ipSecConfig.mSourceAddress.equals(ipSecConfig2.mSourceAddress) && ipSecConfig.mDestinationAddress.equals(ipSecConfig2.mDestinationAddress)) {
            return ((ipSecConfig.mNetwork != null && ipSecConfig.mNetwork.equals(ipSecConfig2.mNetwork)) || ipSecConfig.mNetwork == ipSecConfig2.mNetwork) && ipSecConfig.mEncapType == ipSecConfig2.mEncapType && ipSecConfig.mEncapSocketResourceId == ipSecConfig2.mEncapSocketResourceId && ipSecConfig.mEncapRemotePort == ipSecConfig2.mEncapRemotePort && ipSecConfig.mNattKeepaliveInterval == ipSecConfig2.mNattKeepaliveInterval && ipSecConfig.mSpiResourceId == ipSecConfig2.mSpiResourceId && IpSecAlgorithm.equals(ipSecConfig.mEncryption, ipSecConfig2.mEncryption) && IpSecAlgorithm.equals(ipSecConfig.mAuthenticatedEncryption, ipSecConfig2.mAuthenticatedEncryption) && IpSecAlgorithm.equals(ipSecConfig.mAuthentication, ipSecConfig2.mAuthentication) && ipSecConfig.mMarkValue == ipSecConfig2.mMarkValue && ipSecConfig.mMarkMask == ipSecConfig2.mMarkMask;
        }
        return false;
    }
}
