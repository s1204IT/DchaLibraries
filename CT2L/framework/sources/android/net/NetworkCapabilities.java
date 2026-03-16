package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public final class NetworkCapabilities implements Parcelable {
    public static final Parcelable.Creator<NetworkCapabilities> CREATOR = new Parcelable.Creator<NetworkCapabilities>() {
        @Override
        public NetworkCapabilities createFromParcel(Parcel in) {
            NetworkCapabilities netCap = new NetworkCapabilities();
            netCap.mNetworkCapabilities = in.readLong();
            netCap.mTransportTypes = in.readLong();
            netCap.mLinkUpBandwidthKbps = in.readInt();
            netCap.mLinkDownBandwidthKbps = in.readInt();
            netCap.mNetworkSpecifier = in.readString();
            return netCap;
        }

        @Override
        public NetworkCapabilities[] newArray(int size) {
            return new NetworkCapabilities[size];
        }
    };
    private static final int MAX_NET_CAPABILITY = 16;
    private static final int MAX_TRANSPORT = 4;
    private static final int MIN_NET_CAPABILITY = 0;
    private static final int MIN_TRANSPORT = 0;
    public static final int NET_CAPABILITY_CBS = 5;
    public static final int NET_CAPABILITY_DUN = 2;
    public static final int NET_CAPABILITY_EIMS = 10;
    public static final int NET_CAPABILITY_FOTA = 3;
    public static final int NET_CAPABILITY_IA = 7;
    public static final int NET_CAPABILITY_IMS = 4;
    public static final int NET_CAPABILITY_INTERNET = 12;
    public static final int NET_CAPABILITY_MMS = 0;
    public static final int NET_CAPABILITY_NOT_METERED = 11;
    public static final int NET_CAPABILITY_NOT_RESTRICTED = 13;
    public static final int NET_CAPABILITY_NOT_VPN = 15;
    public static final int NET_CAPABILITY_RCS = 8;
    public static final int NET_CAPABILITY_SUPL = 1;
    public static final int NET_CAPABILITY_TRUSTED = 14;
    public static final int NET_CAPABILITY_VALIDATED = 16;
    public static final int NET_CAPABILITY_WIFI_P2P = 6;
    public static final int NET_CAPABILITY_XCAP = 9;
    public static final int TRANSPORT_BLUETOOTH = 2;
    public static final int TRANSPORT_CELLULAR = 0;
    public static final int TRANSPORT_ETHERNET = 3;
    public static final int TRANSPORT_VPN = 4;
    public static final int TRANSPORT_WIFI = 1;
    private int mLinkDownBandwidthKbps;
    private int mLinkUpBandwidthKbps;
    private long mNetworkCapabilities;
    private String mNetworkSpecifier;
    private long mTransportTypes;

    public NetworkCapabilities() {
        this.mNetworkCapabilities = 57344L;
    }

    public NetworkCapabilities(NetworkCapabilities nc) {
        this.mNetworkCapabilities = 57344L;
        if (nc != null) {
            this.mNetworkCapabilities = nc.mNetworkCapabilities;
            this.mTransportTypes = nc.mTransportTypes;
            this.mLinkUpBandwidthKbps = nc.mLinkUpBandwidthKbps;
            this.mLinkDownBandwidthKbps = nc.mLinkDownBandwidthKbps;
            this.mNetworkSpecifier = nc.mNetworkSpecifier;
        }
    }

    public NetworkCapabilities addCapability(int capability) {
        if (capability < 0 || capability > 16) {
            throw new IllegalArgumentException("NetworkCapability out of range");
        }
        this.mNetworkCapabilities |= (long) (1 << capability);
        return this;
    }

    public NetworkCapabilities removeCapability(int capability) {
        if (capability < 0 || capability > 16) {
            throw new IllegalArgumentException("NetworkCapability out of range");
        }
        this.mNetworkCapabilities &= (long) ((1 << capability) ^ (-1));
        return this;
    }

    public int[] getCapabilities() {
        return enumerateBits(this.mNetworkCapabilities);
    }

    public boolean hasCapability(int capability) {
        if (capability < 0 || capability > 16) {
            return false;
        }
        return (this.mNetworkCapabilities & ((long) (1 << capability))) != 0;
    }

    private int[] enumerateBits(long val) {
        int size = Long.bitCount(val);
        int[] result = new int[size];
        int index = 0;
        int resource = 0;
        while (true) {
            int index2 = index;
            if (val > 0) {
                if ((val & 1) == 1) {
                    index = index2 + 1;
                    result[index2] = resource;
                } else {
                    index = index2;
                }
                val >>= 1;
                resource++;
            } else {
                return result;
            }
        }
    }

    private void combineNetCapabilities(NetworkCapabilities nc) {
        this.mNetworkCapabilities |= nc.mNetworkCapabilities;
    }

    private boolean satisfiedByNetCapabilities(NetworkCapabilities nc) {
        return (nc.mNetworkCapabilities & this.mNetworkCapabilities) == this.mNetworkCapabilities;
    }

    public boolean equalsNetCapabilities(NetworkCapabilities nc) {
        return nc.mNetworkCapabilities == this.mNetworkCapabilities;
    }

    public NetworkCapabilities addTransportType(int transportType) {
        if (transportType < 0 || transportType > 4) {
            throw new IllegalArgumentException("TransportType out of range");
        }
        this.mTransportTypes |= (long) (1 << transportType);
        setNetworkSpecifier(this.mNetworkSpecifier);
        return this;
    }

    public NetworkCapabilities removeTransportType(int transportType) {
        if (transportType < 0 || transportType > 4) {
            throw new IllegalArgumentException("TransportType out of range");
        }
        this.mTransportTypes &= (long) ((1 << transportType) ^ (-1));
        setNetworkSpecifier(this.mNetworkSpecifier);
        return this;
    }

    public int[] getTransportTypes() {
        return enumerateBits(this.mTransportTypes);
    }

    public boolean hasTransport(int transportType) {
        if (transportType < 0 || transportType > 4) {
            return false;
        }
        return (this.mTransportTypes & ((long) (1 << transportType))) != 0;
    }

    private void combineTransportTypes(NetworkCapabilities nc) {
        this.mTransportTypes |= nc.mTransportTypes;
    }

    private boolean satisfiedByTransportTypes(NetworkCapabilities nc) {
        return this.mTransportTypes == 0 || (this.mTransportTypes & nc.mTransportTypes) != 0;
    }

    public boolean equalsTransportTypes(NetworkCapabilities nc) {
        return nc.mTransportTypes == this.mTransportTypes;
    }

    public void setLinkUpstreamBandwidthKbps(int upKbps) {
        this.mLinkUpBandwidthKbps = upKbps;
    }

    public int getLinkUpstreamBandwidthKbps() {
        return this.mLinkUpBandwidthKbps;
    }

    public void setLinkDownstreamBandwidthKbps(int downKbps) {
        this.mLinkDownBandwidthKbps = downKbps;
    }

    public int getLinkDownstreamBandwidthKbps() {
        return this.mLinkDownBandwidthKbps;
    }

    private void combineLinkBandwidths(NetworkCapabilities nc) {
        this.mLinkUpBandwidthKbps = Math.max(this.mLinkUpBandwidthKbps, nc.mLinkUpBandwidthKbps);
        this.mLinkDownBandwidthKbps = Math.max(this.mLinkDownBandwidthKbps, nc.mLinkDownBandwidthKbps);
    }

    private boolean satisfiedByLinkBandwidths(NetworkCapabilities nc) {
        return this.mLinkUpBandwidthKbps <= nc.mLinkUpBandwidthKbps && this.mLinkDownBandwidthKbps <= nc.mLinkDownBandwidthKbps;
    }

    private boolean equalsLinkBandwidths(NetworkCapabilities nc) {
        return this.mLinkUpBandwidthKbps == nc.mLinkUpBandwidthKbps && this.mLinkDownBandwidthKbps == nc.mLinkDownBandwidthKbps;
    }

    public void setNetworkSpecifier(String networkSpecifier) {
        if (!TextUtils.isEmpty(networkSpecifier) && Long.bitCount(this.mTransportTypes) != 1) {
            throw new IllegalStateException("Must have a single transport specified to use setNetworkSpecifier");
        }
        this.mNetworkSpecifier = networkSpecifier;
    }

    public String getNetworkSpecifier() {
        return this.mNetworkSpecifier;
    }

    private void combineSpecifiers(NetworkCapabilities nc) {
        String otherSpecifier = nc.getNetworkSpecifier();
        if (!TextUtils.isEmpty(otherSpecifier)) {
            if (!TextUtils.isEmpty(this.mNetworkSpecifier)) {
                throw new IllegalStateException("Can't combine two networkSpecifiers");
            }
            setNetworkSpecifier(otherSpecifier);
        }
    }

    private boolean satisfiedBySpecifier(NetworkCapabilities nc) {
        return TextUtils.isEmpty(this.mNetworkSpecifier) || this.mNetworkSpecifier.equals(nc.mNetworkSpecifier);
    }

    private boolean equalsSpecifier(NetworkCapabilities nc) {
        return TextUtils.isEmpty(this.mNetworkSpecifier) ? TextUtils.isEmpty(nc.mNetworkSpecifier) : this.mNetworkSpecifier.equals(nc.mNetworkSpecifier);
    }

    public void combineCapabilities(NetworkCapabilities nc) {
        combineNetCapabilities(nc);
        combineTransportTypes(nc);
        combineLinkBandwidths(nc);
        combineSpecifiers(nc);
    }

    public boolean satisfiedByNetworkCapabilities(NetworkCapabilities nc) {
        return nc != null && satisfiedByNetCapabilities(nc) && satisfiedByTransportTypes(nc) && satisfiedByLinkBandwidths(nc) && satisfiedBySpecifier(nc);
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof NetworkCapabilities)) {
            return false;
        }
        NetworkCapabilities that = (NetworkCapabilities) obj;
        return equalsNetCapabilities(that) && equalsTransportTypes(that) && equalsLinkBandwidths(that) && equalsSpecifier(that);
    }

    public int hashCode() {
        return (TextUtils.isEmpty(this.mNetworkSpecifier) ? 0 : this.mNetworkSpecifier.hashCode() * 17) + (this.mLinkDownBandwidthKbps * 13) + ((int) (this.mNetworkCapabilities & (-1))) + (((int) (this.mNetworkCapabilities >> 32)) * 3) + (((int) (this.mTransportTypes & (-1))) * 5) + (((int) (this.mTransportTypes >> 32)) * 7) + (this.mLinkUpBandwidthKbps * 11);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mNetworkCapabilities);
        dest.writeLong(this.mTransportTypes);
        dest.writeInt(this.mLinkUpBandwidthKbps);
        dest.writeInt(this.mLinkDownBandwidthKbps);
        dest.writeString(this.mNetworkSpecifier);
    }

    public String toString() {
        int[] types = getTransportTypes();
        String transports = types.length > 0 ? " Transports: " : ProxyInfo.LOCAL_EXCL_LIST;
        int i = 0;
        while (i < types.length) {
            switch (types[i]) {
                case 0:
                    transports = transports + "CELLULAR";
                    break;
                case 1:
                    transports = transports + "WIFI";
                    break;
                case 2:
                    transports = transports + "BLUETOOTH";
                    break;
                case 3:
                    transports = transports + "ETHERNET";
                    break;
                case 4:
                    transports = transports + "VPN";
                    break;
            }
            i++;
            if (i < types.length) {
                transports = transports + "|";
            }
        }
        int[] types2 = getCapabilities();
        String capabilities = types2.length > 0 ? " Capabilities: " : ProxyInfo.LOCAL_EXCL_LIST;
        int i2 = 0;
        while (i2 < types2.length) {
            switch (types2[i2]) {
                case 0:
                    capabilities = capabilities + "MMS";
                    break;
                case 1:
                    capabilities = capabilities + "SUPL";
                    break;
                case 2:
                    capabilities = capabilities + "DUN";
                    break;
                case 3:
                    capabilities = capabilities + "FOTA";
                    break;
                case 4:
                    capabilities = capabilities + "IMS";
                    break;
                case 5:
                    capabilities = capabilities + "CBS";
                    break;
                case 6:
                    capabilities = capabilities + "WIFI_P2P";
                    break;
                case 7:
                    capabilities = capabilities + "IA";
                    break;
                case 8:
                    capabilities = capabilities + "RCS";
                    break;
                case 9:
                    capabilities = capabilities + "XCAP";
                    break;
                case 10:
                    capabilities = capabilities + "EIMS";
                    break;
                case 11:
                    capabilities = capabilities + "NOT_METERED";
                    break;
                case 12:
                    capabilities = capabilities + "INTERNET";
                    break;
                case 13:
                    capabilities = capabilities + "NOT_RESTRICTED";
                    break;
                case 14:
                    capabilities = capabilities + "TRUSTED";
                    break;
                case 15:
                    capabilities = capabilities + "NOT_VPN";
                    break;
            }
            i2++;
            if (i2 < types2.length) {
                capabilities = capabilities + "&";
            }
        }
        String upBand = this.mLinkUpBandwidthKbps > 0 ? " LinkUpBandwidth>=" + this.mLinkUpBandwidthKbps + "Kbps" : ProxyInfo.LOCAL_EXCL_LIST;
        String dnBand = this.mLinkDownBandwidthKbps > 0 ? " LinkDnBandwidth>=" + this.mLinkDownBandwidthKbps + "Kbps" : ProxyInfo.LOCAL_EXCL_LIST;
        String specifier = this.mNetworkSpecifier == null ? ProxyInfo.LOCAL_EXCL_LIST : " Specifier: <" + this.mNetworkSpecifier + ">";
        return "[" + transports + capabilities + upBand + dnBand + specifier + "]";
    }
}
