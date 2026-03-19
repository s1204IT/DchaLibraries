package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.text.TextUtils;
import android.util.proto.ProtoOutputStream;
import java.util.Objects;
import java.util.Set;

public class NetworkRequest implements Parcelable {
    public static final Parcelable.Creator<NetworkRequest> CREATOR = new Parcelable.Creator<NetworkRequest>() {
        @Override
        public NetworkRequest createFromParcel(Parcel parcel) {
            return new NetworkRequest(NetworkCapabilities.CREATOR.createFromParcel(parcel), parcel.readInt(), parcel.readInt(), Type.valueOf(parcel.readString()));
        }

        @Override
        public NetworkRequest[] newArray(int i) {
            return new NetworkRequest[i];
        }
    };
    public final int legacyType;
    public final NetworkCapabilities networkCapabilities;
    public final int requestId;
    public final Type type;

    public enum Type {
        NONE,
        LISTEN,
        TRACK_DEFAULT,
        REQUEST,
        BACKGROUND_REQUEST
    }

    public NetworkRequest(NetworkCapabilities networkCapabilities, int i, int i2, Type type) {
        if (networkCapabilities == null) {
            throw new NullPointerException();
        }
        this.requestId = i2;
        this.networkCapabilities = networkCapabilities;
        this.legacyType = i;
        this.type = type;
    }

    public NetworkRequest(NetworkRequest networkRequest) {
        this.networkCapabilities = new NetworkCapabilities(networkRequest.networkCapabilities);
        this.requestId = networkRequest.requestId;
        this.legacyType = networkRequest.legacyType;
        this.type = networkRequest.type;
    }

    public static class Builder {
        private final NetworkCapabilities mNetworkCapabilities = new NetworkCapabilities();

        public Builder() {
            this.mNetworkCapabilities.setSingleUid(Process.myUid());
        }

        public NetworkRequest build() {
            NetworkCapabilities networkCapabilities = new NetworkCapabilities(this.mNetworkCapabilities);
            networkCapabilities.maybeMarkCapabilitiesRestricted();
            return new NetworkRequest(networkCapabilities, -1, 0, Type.NONE);
        }

        public Builder addCapability(int i) {
            this.mNetworkCapabilities.addCapability(i);
            return this;
        }

        public Builder removeCapability(int i) {
            this.mNetworkCapabilities.removeCapability(i);
            return this;
        }

        public Builder setCapabilities(NetworkCapabilities networkCapabilities) {
            this.mNetworkCapabilities.set(networkCapabilities);
            return this;
        }

        public Builder setUids(Set<UidRange> set) {
            this.mNetworkCapabilities.setUids(set);
            return this;
        }

        public Builder addUnwantedCapability(int i) {
            this.mNetworkCapabilities.addUnwantedCapability(i);
            return this;
        }

        public Builder clearCapabilities() {
            this.mNetworkCapabilities.clearAll();
            return this;
        }

        public Builder addTransportType(int i) {
            this.mNetworkCapabilities.addTransportType(i);
            return this;
        }

        public Builder removeTransportType(int i) {
            this.mNetworkCapabilities.removeTransportType(i);
            return this;
        }

        public Builder setLinkUpstreamBandwidthKbps(int i) {
            this.mNetworkCapabilities.setLinkUpstreamBandwidthKbps(i);
            return this;
        }

        public Builder setLinkDownstreamBandwidthKbps(int i) {
            this.mNetworkCapabilities.setLinkDownstreamBandwidthKbps(i);
            return this;
        }

        public Builder setNetworkSpecifier(String str) {
            return setNetworkSpecifier(TextUtils.isEmpty(str) ? null : new StringNetworkSpecifier(str));
        }

        public Builder setNetworkSpecifier(NetworkSpecifier networkSpecifier) {
            MatchAllNetworkSpecifier.checkNotMatchAllNetworkSpecifier(networkSpecifier);
            this.mNetworkCapabilities.setNetworkSpecifier(networkSpecifier);
            return this;
        }

        public Builder setSignalStrength(int i) {
            this.mNetworkCapabilities.setSignalStrength(i);
            return this;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.networkCapabilities.writeToParcel(parcel, i);
        parcel.writeInt(this.legacyType);
        parcel.writeInt(this.requestId);
        parcel.writeString(this.type.name());
    }

    public boolean isListen() {
        return this.type == Type.LISTEN;
    }

    public boolean isRequest() {
        return isForegroundRequest() || isBackgroundRequest();
    }

    public boolean isForegroundRequest() {
        return this.type == Type.TRACK_DEFAULT || this.type == Type.REQUEST;
    }

    public boolean isBackgroundRequest() {
        return this.type == Type.BACKGROUND_REQUEST;
    }

    public boolean hasCapability(int i) {
        return this.networkCapabilities.hasCapability(i);
    }

    public boolean hasUnwantedCapability(int i) {
        return this.networkCapabilities.hasUnwantedCapability(i);
    }

    public boolean hasTransport(int i) {
        return this.networkCapabilities.hasTransport(i);
    }

    public String toString() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append("NetworkRequest [ ");
        sb.append(this.type);
        sb.append(" id=");
        sb.append(this.requestId);
        if (this.legacyType != -1) {
            str = ", legacyType=" + this.legacyType;
        } else {
            str = "";
        }
        sb.append(str);
        sb.append(", ");
        sb.append(this.networkCapabilities.toString());
        sb.append(" ]");
        return sb.toString();
    }

    private int typeToProtoEnum(Type type) {
        switch (type) {
            case NONE:
                return 1;
            case LISTEN:
                return 2;
            case TRACK_DEFAULT:
                return 3;
            case REQUEST:
                return 4;
            case BACKGROUND_REQUEST:
                return 5;
            default:
                return 0;
        }
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1159641169921L, typeToProtoEnum(this.type));
        protoOutputStream.write(1120986464258L, this.requestId);
        protoOutputStream.write(1120986464259L, this.legacyType);
        this.networkCapabilities.writeToProto(protoOutputStream, 1146756268036L);
        protoOutputStream.end(jStart);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof NetworkRequest)) {
            return false;
        }
        NetworkRequest networkRequest = (NetworkRequest) obj;
        return networkRequest.legacyType == this.legacyType && networkRequest.requestId == this.requestId && networkRequest.type == this.type && Objects.equals(networkRequest.networkCapabilities, this.networkCapabilities);
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.requestId), Integer.valueOf(this.legacyType), this.networkCapabilities, this.type);
    }
}
