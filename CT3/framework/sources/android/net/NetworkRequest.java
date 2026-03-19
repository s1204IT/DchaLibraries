package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;

public class NetworkRequest implements Parcelable {
    public static final Parcelable.Creator<NetworkRequest> CREATOR = new Parcelable.Creator<NetworkRequest>() {
        @Override
        public NetworkRequest createFromParcel(Parcel in) {
            NetworkCapabilities nc = (NetworkCapabilities) in.readParcelable(null);
            int legacyType = in.readInt();
            int requestId = in.readInt();
            NetworkRequest result = new NetworkRequest(nc, legacyType, requestId);
            return result;
        }

        @Override
        public NetworkRequest[] newArray(int size) {
            return new NetworkRequest[size];
        }
    };
    public final int legacyType;
    public final NetworkCapabilities networkCapabilities;
    public final int requestId;

    public NetworkRequest(NetworkCapabilities nc, int legacyType, int rId) {
        if (nc == null) {
            throw new NullPointerException();
        }
        this.requestId = rId;
        this.networkCapabilities = nc;
        this.legacyType = legacyType;
    }

    public NetworkRequest(NetworkRequest that) {
        this.networkCapabilities = new NetworkCapabilities(that.networkCapabilities);
        this.requestId = that.requestId;
        this.legacyType = that.legacyType;
    }

    public static class Builder {
        private final NetworkCapabilities mNetworkCapabilities = new NetworkCapabilities();

        public NetworkRequest build() {
            NetworkCapabilities nc = new NetworkCapabilities(this.mNetworkCapabilities);
            nc.maybeMarkCapabilitiesRestricted();
            return new NetworkRequest(nc, -1, 0);
        }

        public Builder addCapability(int capability) {
            this.mNetworkCapabilities.addCapability(capability);
            return this;
        }

        public Builder removeCapability(int capability) {
            this.mNetworkCapabilities.removeCapability(capability);
            return this;
        }

        public Builder clearCapabilities() {
            this.mNetworkCapabilities.clearAll();
            return this;
        }

        public Builder addTransportType(int transportType) {
            this.mNetworkCapabilities.addTransportType(transportType);
            return this;
        }

        public Builder removeTransportType(int transportType) {
            this.mNetworkCapabilities.removeTransportType(transportType);
            return this;
        }

        public Builder setLinkUpstreamBandwidthKbps(int upKbps) {
            this.mNetworkCapabilities.setLinkUpstreamBandwidthKbps(upKbps);
            return this;
        }

        public Builder setLinkDownstreamBandwidthKbps(int downKbps) {
            this.mNetworkCapabilities.setLinkDownstreamBandwidthKbps(downKbps);
            return this;
        }

        public Builder setNetworkSpecifier(String networkSpecifier) {
            if (NetworkCapabilities.MATCH_ALL_REQUESTS_NETWORK_SPECIFIER.equals(networkSpecifier)) {
                throw new IllegalArgumentException("Invalid network specifier - must not be '*'");
            }
            this.mNetworkCapabilities.setNetworkSpecifier(networkSpecifier);
            return this;
        }

        public Builder setSignalStrength(int signalStrength) {
            this.mNetworkCapabilities.setSignalStrength(signalStrength);
            return this;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.networkCapabilities, flags);
        dest.writeInt(this.legacyType);
        dest.writeInt(this.requestId);
    }

    public String toString() {
        return "NetworkRequest [ id=" + this.requestId + ", legacyType=" + this.legacyType + ", " + this.networkCapabilities.toString() + " ]";
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkRequest)) {
            return false;
        }
        NetworkRequest that = (NetworkRequest) obj;
        if (that.legacyType != this.legacyType || that.requestId != this.requestId) {
            return false;
        }
        if (that.networkCapabilities == null && this.networkCapabilities == null) {
            return true;
        }
        if (that.networkCapabilities != null) {
            return that.networkCapabilities.equals(this.networkCapabilities);
        }
        return false;
    }

    public int hashCode() {
        return this.requestId + (this.legacyType * Process.MEDIA_UID) + (this.networkCapabilities.hashCode() * 1051);
    }
}
