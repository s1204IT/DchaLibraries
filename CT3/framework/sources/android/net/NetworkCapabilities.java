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
            netCap.mSignalStrength = in.readInt();
            return netCap;
        }

        @Override
        public NetworkCapabilities[] newArray(int size) {
            return new NetworkCapabilities[size];
        }
    };
    private static final long DEFAULT_CAPABILITIES = 57344;
    public static final String MATCH_ALL_REQUESTS_NETWORK_SPECIFIER = "*";
    private static final int MAX_NET_CAPABILITY = 27;
    private static final int MAX_TRANSPORT = 5;
    private static final int MIN_NET_CAPABILITY = 0;
    private static final int MIN_TRANSPORT = 0;
    private static final long MUTABLE_CAPABILITIES = 212992;
    public static final int NET_CAPABILITY_BIP = 27;
    public static final int NET_CAPABILITY_CAPTIVE_PORTAL = 17;
    public static final int NET_CAPABILITY_CBS = 5;
    public static final int NET_CAPABILITY_CMMAIL = 23;
    public static final int NET_CAPABILITY_DM = 20;
    public static final int NET_CAPABILITY_DUN = 2;
    public static final int NET_CAPABILITY_EIMS = 10;
    public static final int NET_CAPABILITY_FOTA = 3;
    public static final int NET_CAPABILITY_IA = 7;
    public static final int NET_CAPABILITY_IMS = 4;
    public static final int NET_CAPABILITY_INTERNET = 12;
    public static final int NET_CAPABILITY_MMS = 0;
    public static final int NET_CAPABILITY_NET = 22;
    public static final int NET_CAPABILITY_NOT_METERED = 11;
    public static final int NET_CAPABILITY_NOT_RESTRICTED = 13;
    public static final int NET_CAPABILITY_NOT_VPN = 15;
    public static final int NET_CAPABILITY_RCS = 8;
    public static final int NET_CAPABILITY_RCSE = 25;
    public static final int NET_CAPABILITY_SUPL = 1;
    public static final int NET_CAPABILITY_TETHERING = 24;
    public static final int NET_CAPABILITY_TRUSTED = 14;
    public static final int NET_CAPABILITY_VALIDATED = 16;
    public static final int NET_CAPABILITY_VSIM = 26;
    public static final int NET_CAPABILITY_WAP = 21;
    public static final int NET_CAPABILITY_WIFI_P2P = 6;
    public static final int NET_CAPABILITY_XCAP = 9;
    private static final long NON_REQUESTABLE_CAPABILITIES = 196608;
    private static final long RESTRICTED_CAPABILITIES = 1980;
    public static final int SIGNAL_STRENGTH_UNSPECIFIED = Integer.MIN_VALUE;
    public static final int TRANSPORT_BLUETOOTH = 2;
    public static final int TRANSPORT_CELLULAR = 0;
    public static final int TRANSPORT_ETHERNET = 3;
    public static final int TRANSPORT_TEDONGLE = 5;
    public static final int TRANSPORT_VPN = 4;
    public static final int TRANSPORT_WIFI = 1;
    private int mLinkDownBandwidthKbps;
    private int mLinkUpBandwidthKbps;
    private long mNetworkCapabilities;
    private String mNetworkSpecifier;
    private int mSignalStrength;
    private long mTransportTypes;

    public NetworkCapabilities() {
        clearAll();
        this.mNetworkCapabilities = DEFAULT_CAPABILITIES;
    }

    public NetworkCapabilities(NetworkCapabilities nc) {
        if (nc == null) {
            return;
        }
        this.mNetworkCapabilities = nc.mNetworkCapabilities;
        this.mTransportTypes = nc.mTransportTypes;
        this.mLinkUpBandwidthKbps = nc.mLinkUpBandwidthKbps;
        this.mLinkDownBandwidthKbps = nc.mLinkDownBandwidthKbps;
        this.mNetworkSpecifier = nc.mNetworkSpecifier;
        this.mSignalStrength = nc.mSignalStrength;
    }

    public void clearAll() {
        this.mTransportTypes = 0L;
        this.mNetworkCapabilities = 0L;
        this.mLinkDownBandwidthKbps = 0;
        this.mLinkUpBandwidthKbps = 0;
        this.mNetworkSpecifier = null;
        this.mSignalStrength = Integer.MIN_VALUE;
    }

    public NetworkCapabilities addCapability(int capability) {
        if (capability < 0 || capability > 27) {
            throw new IllegalArgumentException("NetworkCapability out of range");
        }
        this.mNetworkCapabilities |= (long) (1 << capability);
        return this;
    }

    public NetworkCapabilities removeCapability(int capability) {
        if (capability < 0 || capability > 27) {
            throw new IllegalArgumentException("NetworkCapability out of range");
        }
        this.mNetworkCapabilities &= (long) (~(1 << capability));
        return this;
    }

    public int[] getCapabilities() {
        return enumerateBits(this.mNetworkCapabilities);
    }

    public boolean hasCapability(int capability) {
        return capability >= 0 && capability <= 27 && (this.mNetworkCapabilities & ((long) (1 << capability))) != 0;
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

    public String describeFirstNonRequestableCapability() {
        if (hasCapability(16)) {
            return "NET_CAPABILITY_VALIDATED";
        }
        if (hasCapability(17)) {
            return "NET_CAPABILITY_CAPTIVE_PORTAL";
        }
        if ((this.mNetworkCapabilities & NON_REQUESTABLE_CAPABILITIES) != 0) {
            return "unknown non-requestable capabilities " + Long.toHexString(this.mNetworkCapabilities);
        }
        if (this.mLinkUpBandwidthKbps != 0 || this.mLinkDownBandwidthKbps != 0) {
            return "link bandwidth";
        }
        if (hasSignalStrength()) {
            return "signalStrength";
        }
        return null;
    }

    private boolean satisfiedByNetCapabilities(NetworkCapabilities nc, boolean onlyImmutable) {
        long networkCapabilities = this.mNetworkCapabilities;
        if (onlyImmutable) {
            networkCapabilities &= -212993;
        }
        return (nc.mNetworkCapabilities & networkCapabilities) == networkCapabilities;
    }

    public boolean equalsNetCapabilities(NetworkCapabilities nc) {
        return nc.mNetworkCapabilities == this.mNetworkCapabilities;
    }

    private boolean equalsNetCapabilitiesImmutable(NetworkCapabilities that) {
        return (this.mNetworkCapabilities & (-212993)) == (that.mNetworkCapabilities & (-212993));
    }

    public void maybeMarkCapabilitiesRestricted() {
        if ((this.mNetworkCapabilities & (-59325)) != 0 || (this.mNetworkCapabilities & RESTRICTED_CAPABILITIES) == 0) {
            return;
        }
        removeCapability(13);
    }

    public NetworkCapabilities addTransportType(int transportType) {
        if (transportType < 0 || transportType > 5) {
            throw new IllegalArgumentException("TransportType out of range");
        }
        this.mTransportTypes |= (long) (1 << transportType);
        setNetworkSpecifier(this.mNetworkSpecifier);
        return this;
    }

    public NetworkCapabilities removeTransportType(int transportType) {
        if (transportType < 0 || transportType > 5) {
            throw new IllegalArgumentException("TransportType out of range");
        }
        this.mTransportTypes &= (long) (~(1 << transportType));
        setNetworkSpecifier(this.mNetworkSpecifier);
        return this;
    }

    public int[] getTransportTypes() {
        return enumerateBits(this.mTransportTypes);
    }

    public boolean hasTransport(int transportType) {
        return transportType >= 0 && transportType <= 5 && (this.mTransportTypes & ((long) (1 << transportType))) != 0;
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

    public NetworkCapabilities setNetworkSpecifier(String networkSpecifier) {
        if (!TextUtils.isEmpty(networkSpecifier) && Long.bitCount(this.mTransportTypes) != 1) {
            throw new IllegalStateException("Must have a single transport specified to use setNetworkSpecifier");
        }
        this.mNetworkSpecifier = networkSpecifier;
        return this;
    }

    public String getNetworkSpecifier() {
        return this.mNetworkSpecifier;
    }

    private void combineSpecifiers(NetworkCapabilities nc) {
        String otherSpecifier = nc.getNetworkSpecifier();
        if (TextUtils.isEmpty(otherSpecifier)) {
            return;
        }
        if (!TextUtils.isEmpty(this.mNetworkSpecifier)) {
            throw new IllegalStateException("Can't combine two networkSpecifiers");
        }
        setNetworkSpecifier(otherSpecifier);
    }

    private boolean satisfiedBySpecifier(NetworkCapabilities nc) {
        if (TextUtils.isEmpty(this.mNetworkSpecifier) || this.mNetworkSpecifier.equals(nc.mNetworkSpecifier)) {
            return true;
        }
        return MATCH_ALL_REQUESTS_NETWORK_SPECIFIER.equals(nc.mNetworkSpecifier);
    }

    private boolean equalsSpecifier(NetworkCapabilities nc) {
        if (TextUtils.isEmpty(this.mNetworkSpecifier)) {
            return TextUtils.isEmpty(nc.mNetworkSpecifier);
        }
        return this.mNetworkSpecifier.equals(nc.mNetworkSpecifier);
    }

    public void setSignalStrength(int signalStrength) {
        this.mSignalStrength = signalStrength;
    }

    public boolean hasSignalStrength() {
        return this.mSignalStrength > Integer.MIN_VALUE;
    }

    public int getSignalStrength() {
        return this.mSignalStrength;
    }

    private void combineSignalStrength(NetworkCapabilities nc) {
        this.mSignalStrength = Math.max(this.mSignalStrength, nc.mSignalStrength);
    }

    private boolean satisfiedBySignalStrength(NetworkCapabilities nc) {
        return this.mSignalStrength <= nc.mSignalStrength;
    }

    private boolean equalsSignalStrength(NetworkCapabilities nc) {
        return this.mSignalStrength == nc.mSignalStrength;
    }

    public void combineCapabilities(NetworkCapabilities nc) {
        combineNetCapabilities(nc);
        combineTransportTypes(nc);
        combineLinkBandwidths(nc);
        combineSpecifiers(nc);
        combineSignalStrength(nc);
    }

    private boolean satisfiedByNetworkCapabilities(NetworkCapabilities nc, boolean onlyImmutable) {
        if (nc == null || !satisfiedByNetCapabilities(nc, onlyImmutable) || !satisfiedByTransportTypes(nc) || ((!onlyImmutable && !satisfiedByLinkBandwidths(nc)) || !satisfiedBySpecifier(nc))) {
            return false;
        }
        if (onlyImmutable) {
            return true;
        }
        return satisfiedBySignalStrength(nc);
    }

    public boolean satisfiedByNetworkCapabilities(NetworkCapabilities nc) {
        return satisfiedByNetworkCapabilities(nc, false);
    }

    public boolean satisfiedByImmutableNetworkCapabilities(NetworkCapabilities nc) {
        return satisfiedByNetworkCapabilities(nc, true);
    }

    public boolean equalImmutableCapabilities(NetworkCapabilities nc) {
        if (nc != null && equalsNetCapabilitiesImmutable(nc) && equalsTransportTypes(nc)) {
            return equalsSpecifier(nc);
        }
        return false;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof NetworkCapabilities)) {
            return false;
        }
        NetworkCapabilities that = (NetworkCapabilities) obj;
        if (equalsNetCapabilities(that) && equalsTransportTypes(that) && equalsLinkBandwidths(that) && equalsSignalStrength(that)) {
            return equalsSpecifier(that);
        }
        return false;
    }

    public int hashCode() {
        return (TextUtils.isEmpty(this.mNetworkSpecifier) ? 0 : this.mNetworkSpecifier.hashCode() * 17) + (this.mLinkDownBandwidthKbps * 13) + ((int) (this.mNetworkCapabilities & (-1))) + (((int) (this.mNetworkCapabilities >> 32)) * 3) + (((int) (this.mTransportTypes & (-1))) * 5) + (((int) (this.mTransportTypes >> 32)) * 7) + (this.mLinkUpBandwidthKbps * 11) + (this.mSignalStrength * 19);
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
        dest.writeInt(this.mSignalStrength);
    }

    public String toString() {
        String upBand;
        String dnBand;
        int[] types = getTransportTypes();
        String transports = types.length > 0 ? " Transports: " + transportNamesOf(types) : ProxyInfo.LOCAL_EXCL_LIST;
        int[] types2 = getCapabilities();
        String capabilities = types2.length > 0 ? " Capabilities: " : ProxyInfo.LOCAL_EXCL_LIST;
        int i = 0;
        while (i < types2.length) {
            switch (types2[i]) {
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
                case 16:
                    capabilities = capabilities + "VALIDATED";
                    break;
                case 17:
                    capabilities = capabilities + "CAPTIVE_PORTAL";
                    break;
                case 20:
                    capabilities = capabilities + "DM";
                    break;
                case 21:
                    capabilities = capabilities + "WAP";
                    break;
                case 22:
                    capabilities = capabilities + "NET";
                    break;
                case 23:
                    capabilities = capabilities + "CMMAIL";
                    break;
                case 24:
                    capabilities = capabilities + "TETHERING";
                    break;
                case 25:
                    capabilities = capabilities + "RCSE";
                    break;
                case 26:
                    capabilities = capabilities + "VSIM";
                    break;
                case 27:
                    capabilities = capabilities + "BIP";
                    break;
            }
            i++;
            if (i < types2.length) {
                capabilities = capabilities + "&";
            }
        }
        if (this.mLinkUpBandwidthKbps > 0) {
            upBand = " LinkUpBandwidth>=" + this.mLinkUpBandwidthKbps + "Kbps";
        } else {
            upBand = ProxyInfo.LOCAL_EXCL_LIST;
        }
        if (this.mLinkDownBandwidthKbps > 0) {
            dnBand = " LinkDnBandwidth>=" + this.mLinkDownBandwidthKbps + "Kbps";
        } else {
            dnBand = ProxyInfo.LOCAL_EXCL_LIST;
        }
        String specifier = this.mNetworkSpecifier == null ? ProxyInfo.LOCAL_EXCL_LIST : " Specifier: <" + this.mNetworkSpecifier + ">";
        String signalStrength = hasSignalStrength() ? " SignalStrength: " + this.mSignalStrength : ProxyInfo.LOCAL_EXCL_LIST;
        return "[" + transports + capabilities + upBand + dnBand + specifier + signalStrength + "]";
    }

    public static String transportNamesOf(int[] types) {
        String transports = ProxyInfo.LOCAL_EXCL_LIST;
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
        return transports;
    }
}
