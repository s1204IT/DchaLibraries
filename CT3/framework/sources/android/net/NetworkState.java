package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class NetworkState implements Parcelable {
    public final LinkProperties linkProperties;
    public final Network network;
    public final NetworkCapabilities networkCapabilities;
    public final String networkId;
    public final NetworkInfo networkInfo;
    public final String subscriberId;
    public static final NetworkState EMPTY = new NetworkState(null, null, null, null, null, null);
    public static final Parcelable.Creator<NetworkState> CREATOR = new Parcelable.Creator<NetworkState>() {
        @Override
        public NetworkState createFromParcel(Parcel in) {
            return new NetworkState(in);
        }

        @Override
        public NetworkState[] newArray(int size) {
            return new NetworkState[size];
        }
    };

    public NetworkState(NetworkInfo networkInfo, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, Network network, String subscriberId, String networkId) {
        this.networkInfo = networkInfo;
        this.linkProperties = linkProperties;
        this.networkCapabilities = networkCapabilities;
        this.network = network;
        this.subscriberId = subscriberId;
        this.networkId = networkId;
    }

    public NetworkState(Parcel in) {
        this.networkInfo = (NetworkInfo) in.readParcelable(null);
        this.linkProperties = (LinkProperties) in.readParcelable(null);
        this.networkCapabilities = (NetworkCapabilities) in.readParcelable(null);
        this.network = (Network) in.readParcelable(null);
        this.subscriberId = in.readString();
        this.networkId = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(this.networkInfo, flags);
        out.writeParcelable(this.linkProperties, flags);
        out.writeParcelable(this.networkCapabilities, flags);
        out.writeParcelable(this.network, flags);
        out.writeString(this.subscriberId);
        out.writeString(this.networkId);
    }
}
