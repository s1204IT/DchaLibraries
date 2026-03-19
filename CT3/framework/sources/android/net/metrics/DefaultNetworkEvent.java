package android.net.metrics;

import android.net.NetworkCapabilities;
import android.os.Parcel;
import android.os.Parcelable;
import android.security.keystore.KeyProperties;

public final class DefaultNetworkEvent extends IpConnectivityEvent implements Parcelable {
    public static final Parcelable.Creator<DefaultNetworkEvent> CREATOR = new Parcelable.Creator<DefaultNetworkEvent>() {
        @Override
        public DefaultNetworkEvent createFromParcel(Parcel in) {
            return new DefaultNetworkEvent(in, null);
        }

        @Override
        public DefaultNetworkEvent[] newArray(int size) {
            return new DefaultNetworkEvent[size];
        }
    };
    public final int netId;
    public final boolean prevIPv4;
    public final boolean prevIPv6;
    public final int prevNetId;
    public final int[] transportTypes;

    DefaultNetworkEvent(Parcel in, DefaultNetworkEvent defaultNetworkEvent) {
        this(in);
    }

    private DefaultNetworkEvent(int netId, int[] transportTypes, int prevNetId, boolean prevIPv4, boolean prevIPv6) {
        this.netId = netId;
        this.transportTypes = transportTypes;
        this.prevNetId = prevNetId;
        this.prevIPv4 = prevIPv4;
        this.prevIPv6 = prevIPv6;
    }

    private DefaultNetworkEvent(Parcel in) {
        this.netId = in.readInt();
        this.transportTypes = in.createIntArray();
        this.prevNetId = in.readInt();
        this.prevIPv4 = in.readByte() > 0;
        this.prevIPv6 = in.readByte() > 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.netId);
        out.writeIntArray(this.transportTypes);
        out.writeInt(this.prevNetId);
        out.writeByte(this.prevIPv4 ? (byte) 1 : (byte) 0);
        out.writeByte(this.prevIPv6 ? (byte) 1 : (byte) 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        String prevNetwork = String.valueOf(this.prevNetId);
        String newNetwork = String.valueOf(this.netId);
        if (this.prevNetId != 0) {
            prevNetwork = prevNetwork + ":" + ipSupport();
        }
        if (this.netId != 0) {
            newNetwork = newNetwork + ":" + NetworkCapabilities.transportNamesOf(this.transportTypes);
        }
        return String.format("DefaultNetworkEvent(%s -> %s)", prevNetwork, newNetwork);
    }

    private String ipSupport() {
        if (this.prevIPv4 && this.prevIPv6) {
            return "DUAL";
        }
        if (this.prevIPv6) {
            return "IPv6";
        }
        if (this.prevIPv4) {
            return "IPv4";
        }
        return KeyProperties.DIGEST_NONE;
    }

    public static void logEvent(int netId, int[] transports, int prevNetId, boolean hadIPv4, boolean hadIPv6) {
        logEvent(new DefaultNetworkEvent(netId, transports, prevNetId, hadIPv4, hadIPv6));
    }
}
