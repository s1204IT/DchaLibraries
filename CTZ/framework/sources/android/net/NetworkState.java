package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class NetworkState implements Parcelable {
    private static final boolean SANITY_CHECK_ROAMING = false;
    public final LinkProperties linkProperties;
    public final Network network;
    public final NetworkCapabilities networkCapabilities;
    public final String networkId;
    public final NetworkInfo networkInfo;
    public final String subscriberId;
    public static final NetworkState EMPTY = new NetworkState(null, null, null, null, null, null);
    public static final Parcelable.Creator<NetworkState> CREATOR = new Parcelable.Creator<NetworkState>() {
        @Override
        public NetworkState createFromParcel(Parcel parcel) {
            return new NetworkState(parcel);
        }

        @Override
        public NetworkState[] newArray(int i) {
            return new NetworkState[i];
        }
    };

    public NetworkState(NetworkInfo networkInfo, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, Network network, String str, String str2) {
        this.networkInfo = networkInfo;
        this.linkProperties = linkProperties;
        this.networkCapabilities = networkCapabilities;
        this.network = network;
        this.subscriberId = str;
        this.networkId = str2;
    }

    public NetworkState(Parcel parcel) {
        this.networkInfo = (NetworkInfo) parcel.readParcelable(null);
        this.linkProperties = (LinkProperties) parcel.readParcelable(null);
        this.networkCapabilities = (NetworkCapabilities) parcel.readParcelable(null);
        this.network = (Network) parcel.readParcelable(null);
        this.subscriberId = parcel.readString();
        this.networkId = parcel.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.networkInfo, i);
        parcel.writeParcelable(this.linkProperties, i);
        parcel.writeParcelable(this.networkCapabilities, i);
        parcel.writeParcelable(this.network, i);
        parcel.writeString(this.subscriberId);
        parcel.writeString(this.networkId);
    }
}
