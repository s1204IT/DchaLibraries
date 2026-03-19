package android.net;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.util.BitUtils;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public final class NetworkCapabilities implements Parcelable {
    private static final long DEFAULT_CAPABILITIES = 57344;
    private static final long FORCE_RESTRICTED_CAPABILITIES = 4194304;
    private static final int INVALID_UID = -1;
    public static final int LINK_BANDWIDTH_UNSPECIFIED = 0;
    private static final int MAX_NET_CAPABILITY = 28;
    public static final int MAX_TRANSPORT = 6;
    private static final int MIN_NET_CAPABILITY = 0;
    public static final int MIN_TRANSPORT = 0;
    private static final long MUTABLE_CAPABILITIES = 4145152;
    public static final int NET_CAPABILITY_BIP = 27;
    public static final int NET_CAPABILITY_CAPTIVE_PORTAL = 17;
    public static final int NET_CAPABILITY_CBS = 5;
    public static final int NET_CAPABILITY_DUN = 2;
    public static final int NET_CAPABILITY_EIMS = 10;
    public static final int NET_CAPABILITY_FOREGROUND = 19;
    public static final int NET_CAPABILITY_FOTA = 3;
    public static final int NET_CAPABILITY_IA = 7;
    public static final int NET_CAPABILITY_IMS = 4;
    public static final int NET_CAPABILITY_INTERNET = 12;
    public static final int NET_CAPABILITY_MMS = 0;
    public static final int NET_CAPABILITY_NOT_CONGESTED = 20;
    public static final int NET_CAPABILITY_NOT_METERED = 11;
    public static final int NET_CAPABILITY_NOT_RESTRICTED = 13;
    public static final int NET_CAPABILITY_NOT_ROAMING = 18;
    public static final int NET_CAPABILITY_NOT_SUSPENDED = 21;
    public static final int NET_CAPABILITY_NOT_VPN = 15;

    @SystemApi
    public static final int NET_CAPABILITY_OEM_PAID = 22;
    public static final int NET_CAPABILITY_PREEMPT = 28;
    public static final int NET_CAPABILITY_RCS = 8;
    public static final int NET_CAPABILITY_SUPL = 1;
    public static final int NET_CAPABILITY_TRUSTED = 14;
    public static final int NET_CAPABILITY_VALIDATED = 16;
    public static final int NET_CAPABILITY_VSIM = 26;
    public static final int NET_CAPABILITY_WAP = 25;
    public static final int NET_CAPABILITY_WIFI_P2P = 6;
    public static final int NET_CAPABILITY_XCAP = 9;
    private static final long NON_REQUESTABLE_CAPABILITIES = 4128768;

    @VisibleForTesting
    static final long RESTRICTED_CAPABILITIES = 1980;
    public static final int SIGNAL_STRENGTH_UNSPECIFIED = Integer.MIN_VALUE;
    private static final String TAG = "NetworkCapabilities";
    public static final int TRANSPORT_BLUETOOTH = 2;
    public static final int TRANSPORT_CELLULAR = 0;
    public static final int TRANSPORT_ETHERNET = 3;
    public static final int TRANSPORT_LOWPAN = 6;
    public static final int TRANSPORT_VPN = 4;
    public static final int TRANSPORT_WIFI = 1;
    public static final int TRANSPORT_WIFI_AWARE = 5;

    @VisibleForTesting
    static final long UNRESTRICTED_CAPABILITIES = 4163;
    private long mNetworkCapabilities;
    private String mSSID;
    private long mTransportTypes;
    private long mUnwantedNetworkCapabilities;
    private static final String[] TRANSPORT_NAMES = {"CELLULAR", "WIFI", "BLUETOOTH", "ETHERNET", "VPN", "WIFI_AWARE", "LOWPAN"};
    public static final Parcelable.Creator<NetworkCapabilities> CREATOR = new Parcelable.Creator<NetworkCapabilities>() {
        @Override
        public NetworkCapabilities createFromParcel(Parcel parcel) {
            NetworkCapabilities networkCapabilities = new NetworkCapabilities();
            networkCapabilities.mNetworkCapabilities = parcel.readLong();
            networkCapabilities.mUnwantedNetworkCapabilities = parcel.readLong();
            networkCapabilities.mTransportTypes = parcel.readLong();
            networkCapabilities.mLinkUpBandwidthKbps = parcel.readInt();
            networkCapabilities.mLinkDownBandwidthKbps = parcel.readInt();
            networkCapabilities.mNetworkSpecifier = (NetworkSpecifier) parcel.readParcelable(null);
            networkCapabilities.mSignalStrength = parcel.readInt();
            networkCapabilities.mUids = parcel.readArraySet(null);
            networkCapabilities.mSSID = parcel.readString();
            return networkCapabilities;
        }

        @Override
        public NetworkCapabilities[] newArray(int i) {
            return new NetworkCapabilities[i];
        }
    };
    private int mEstablishingVpnAppUid = -1;
    private int mLinkUpBandwidthKbps = 0;
    private int mLinkDownBandwidthKbps = 0;
    private NetworkSpecifier mNetworkSpecifier = null;
    private int mSignalStrength = Integer.MIN_VALUE;
    private ArraySet<UidRange> mUids = null;

    private interface NameOf {
        String nameOf(int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface NetCapability {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Transport {
    }

    public NetworkCapabilities() {
        clearAll();
        this.mNetworkCapabilities = DEFAULT_CAPABILITIES;
    }

    public NetworkCapabilities(NetworkCapabilities networkCapabilities) {
        if (networkCapabilities != null) {
            set(networkCapabilities);
        }
    }

    public void clearAll() {
        this.mUnwantedNetworkCapabilities = 0L;
        this.mTransportTypes = 0L;
        this.mNetworkCapabilities = 0L;
        this.mLinkDownBandwidthKbps = 0;
        this.mLinkUpBandwidthKbps = 0;
        this.mNetworkSpecifier = null;
        this.mSignalStrength = Integer.MIN_VALUE;
        this.mUids = null;
        this.mEstablishingVpnAppUid = -1;
        this.mSSID = null;
    }

    public void set(NetworkCapabilities networkCapabilities) {
        this.mNetworkCapabilities = networkCapabilities.mNetworkCapabilities;
        this.mTransportTypes = networkCapabilities.mTransportTypes;
        this.mLinkUpBandwidthKbps = networkCapabilities.mLinkUpBandwidthKbps;
        this.mLinkDownBandwidthKbps = networkCapabilities.mLinkDownBandwidthKbps;
        this.mNetworkSpecifier = networkCapabilities.mNetworkSpecifier;
        this.mSignalStrength = networkCapabilities.mSignalStrength;
        setUids(networkCapabilities.mUids);
        this.mEstablishingVpnAppUid = networkCapabilities.mEstablishingVpnAppUid;
        this.mUnwantedNetworkCapabilities = networkCapabilities.mUnwantedNetworkCapabilities;
        this.mSSID = networkCapabilities.mSSID;
    }

    public NetworkCapabilities addCapability(int i) {
        checkValidCapability(i);
        int i2 = 1 << i;
        this.mNetworkCapabilities |= (long) i2;
        this.mUnwantedNetworkCapabilities &= (long) (~i2);
        return this;
    }

    public void addUnwantedCapability(int i) {
        checkValidCapability(i);
        int i2 = 1 << i;
        this.mUnwantedNetworkCapabilities |= (long) i2;
        this.mNetworkCapabilities &= (long) (~i2);
    }

    public NetworkCapabilities removeCapability(int i) {
        checkValidCapability(i);
        long j = ~(1 << i);
        this.mNetworkCapabilities &= j;
        this.mUnwantedNetworkCapabilities = j & this.mUnwantedNetworkCapabilities;
        return this;
    }

    public NetworkCapabilities setCapability(int i, boolean z) {
        if (z) {
            addCapability(i);
        } else {
            removeCapability(i);
        }
        return this;
    }

    public int[] getCapabilities() {
        return BitUtils.unpackBits(this.mNetworkCapabilities);
    }

    public int[] getUnwantedCapabilities() {
        return BitUtils.unpackBits(this.mUnwantedNetworkCapabilities);
    }

    public void setCapabilities(int[] iArr, int[] iArr2) {
        this.mNetworkCapabilities = BitUtils.packBits(iArr);
        this.mUnwantedNetworkCapabilities = BitUtils.packBits(iArr2);
    }

    @Deprecated
    public void setCapabilities(int[] iArr) {
        setCapabilities(iArr, new int[0]);
    }

    public boolean hasCapability(int i) {
        return isValidCapability(i) && (this.mNetworkCapabilities & ((long) (1 << i))) != 0;
    }

    public boolean hasUnwantedCapability(int i) {
        return isValidCapability(i) && (this.mUnwantedNetworkCapabilities & ((long) (1 << i))) != 0;
    }

    private void combineNetCapabilities(NetworkCapabilities networkCapabilities) {
        this.mNetworkCapabilities |= networkCapabilities.mNetworkCapabilities;
        this.mUnwantedNetworkCapabilities |= networkCapabilities.mUnwantedNetworkCapabilities;
    }

    public String describeFirstNonRequestableCapability() {
        long j = (this.mNetworkCapabilities | this.mUnwantedNetworkCapabilities) & NON_REQUESTABLE_CAPABILITIES;
        if (j != 0) {
            return capabilityNameOf(BitUtils.unpackBits(j)[0]);
        }
        if (this.mLinkUpBandwidthKbps != 0 || this.mLinkDownBandwidthKbps != 0) {
            return "link bandwidth";
        }
        if (hasSignalStrength()) {
            return "signalStrength";
        }
        return null;
    }

    private boolean satisfiedByNetCapabilities(NetworkCapabilities networkCapabilities, boolean z) {
        long j = this.mNetworkCapabilities;
        long j2 = this.mUnwantedNetworkCapabilities;
        long j3 = networkCapabilities.mNetworkCapabilities;
        if (z) {
            j &= -4145153;
            j2 &= -4145153;
        }
        return (j3 & j) == j && (j2 & j3) == 0;
    }

    public boolean equalsNetCapabilities(NetworkCapabilities networkCapabilities) {
        return networkCapabilities.mNetworkCapabilities == this.mNetworkCapabilities && networkCapabilities.mUnwantedNetworkCapabilities == this.mUnwantedNetworkCapabilities;
    }

    private boolean equalsNetCapabilitiesRequestable(NetworkCapabilities networkCapabilities) {
        return (this.mNetworkCapabilities & (-4128769)) == (networkCapabilities.mNetworkCapabilities & (-4128769)) && (this.mUnwantedNetworkCapabilities & (-4128769)) == ((-4128769) & networkCapabilities.mUnwantedNetworkCapabilities);
    }

    public void maybeMarkCapabilitiesRestricted() {
        boolean z = (this.mNetworkCapabilities & 4194304) != 0;
        boolean z2 = (this.mNetworkCapabilities & UNRESTRICTED_CAPABILITIES) != 0;
        boolean z3 = (this.mNetworkCapabilities & RESTRICTED_CAPABILITIES) != 0;
        if (z || (z3 && !z2)) {
            removeCapability(13);
        }
    }

    public static boolean isValidTransport(int i) {
        return i >= 0 && i <= 6;
    }

    public NetworkCapabilities addTransportType(int i) {
        checkValidTransportType(i);
        this.mTransportTypes |= (long) (1 << i);
        setNetworkSpecifier(this.mNetworkSpecifier);
        return this;
    }

    public NetworkCapabilities removeTransportType(int i) {
        checkValidTransportType(i);
        this.mTransportTypes &= (long) (~(1 << i));
        setNetworkSpecifier(this.mNetworkSpecifier);
        return this;
    }

    public NetworkCapabilities setTransportType(int i, boolean z) {
        if (z) {
            addTransportType(i);
        } else {
            removeTransportType(i);
        }
        return this;
    }

    public int[] getTransportTypes() {
        return BitUtils.unpackBits(this.mTransportTypes);
    }

    public void setTransportTypes(int[] iArr) {
        this.mTransportTypes = BitUtils.packBits(iArr);
    }

    public boolean hasTransport(int i) {
        return isValidTransport(i) && (this.mTransportTypes & ((long) (1 << i))) != 0;
    }

    private void combineTransportTypes(NetworkCapabilities networkCapabilities) {
        this.mTransportTypes |= networkCapabilities.mTransportTypes;
    }

    private boolean satisfiedByTransportTypes(NetworkCapabilities networkCapabilities) {
        return this.mTransportTypes == 0 || (this.mTransportTypes & networkCapabilities.mTransportTypes) != 0;
    }

    public boolean equalsTransportTypes(NetworkCapabilities networkCapabilities) {
        return networkCapabilities.mTransportTypes == this.mTransportTypes;
    }

    public void setEstablishingVpnAppUid(int i) {
        this.mEstablishingVpnAppUid = i;
    }

    public NetworkCapabilities setLinkUpstreamBandwidthKbps(int i) {
        this.mLinkUpBandwidthKbps = i;
        return this;
    }

    public int getLinkUpstreamBandwidthKbps() {
        return this.mLinkUpBandwidthKbps;
    }

    public NetworkCapabilities setLinkDownstreamBandwidthKbps(int i) {
        this.mLinkDownBandwidthKbps = i;
        return this;
    }

    public int getLinkDownstreamBandwidthKbps() {
        return this.mLinkDownBandwidthKbps;
    }

    private void combineLinkBandwidths(NetworkCapabilities networkCapabilities) {
        this.mLinkUpBandwidthKbps = Math.max(this.mLinkUpBandwidthKbps, networkCapabilities.mLinkUpBandwidthKbps);
        this.mLinkDownBandwidthKbps = Math.max(this.mLinkDownBandwidthKbps, networkCapabilities.mLinkDownBandwidthKbps);
    }

    private boolean satisfiedByLinkBandwidths(NetworkCapabilities networkCapabilities) {
        return this.mLinkUpBandwidthKbps <= networkCapabilities.mLinkUpBandwidthKbps && this.mLinkDownBandwidthKbps <= networkCapabilities.mLinkDownBandwidthKbps;
    }

    private boolean equalsLinkBandwidths(NetworkCapabilities networkCapabilities) {
        return this.mLinkUpBandwidthKbps == networkCapabilities.mLinkUpBandwidthKbps && this.mLinkDownBandwidthKbps == networkCapabilities.mLinkDownBandwidthKbps;
    }

    public static int minBandwidth(int i, int i2) {
        if (i == 0) {
            return i2;
        }
        if (i2 == 0) {
            return i;
        }
        return Math.min(i, i2);
    }

    public static int maxBandwidth(int i, int i2) {
        return Math.max(i, i2);
    }

    public NetworkCapabilities setNetworkSpecifier(NetworkSpecifier networkSpecifier) {
        if (networkSpecifier != null && Long.bitCount(this.mTransportTypes) != 1) {
            throw new IllegalStateException("Must have a single transport specified to use setNetworkSpecifier");
        }
        this.mNetworkSpecifier = networkSpecifier;
        return this;
    }

    public NetworkSpecifier getNetworkSpecifier() {
        return this.mNetworkSpecifier;
    }

    private void combineSpecifiers(NetworkCapabilities networkCapabilities) {
        if (this.mNetworkSpecifier != null && !this.mNetworkSpecifier.equals(networkCapabilities.mNetworkSpecifier)) {
            throw new IllegalStateException("Can't combine two networkSpecifiers");
        }
        setNetworkSpecifier(networkCapabilities.mNetworkSpecifier);
    }

    private boolean satisfiedBySpecifier(NetworkCapabilities networkCapabilities) {
        return this.mNetworkSpecifier == null || this.mNetworkSpecifier.satisfiedBy(networkCapabilities.mNetworkSpecifier) || (networkCapabilities.mNetworkSpecifier instanceof MatchAllNetworkSpecifier);
    }

    private boolean equalsSpecifier(NetworkCapabilities networkCapabilities) {
        return Objects.equals(this.mNetworkSpecifier, networkCapabilities.mNetworkSpecifier);
    }

    public NetworkCapabilities setSignalStrength(int i) {
        this.mSignalStrength = i;
        return this;
    }

    public boolean hasSignalStrength() {
        return this.mSignalStrength > Integer.MIN_VALUE;
    }

    public int getSignalStrength() {
        return this.mSignalStrength;
    }

    private void combineSignalStrength(NetworkCapabilities networkCapabilities) {
        this.mSignalStrength = Math.max(this.mSignalStrength, networkCapabilities.mSignalStrength);
    }

    private boolean satisfiedBySignalStrength(NetworkCapabilities networkCapabilities) {
        return this.mSignalStrength <= networkCapabilities.mSignalStrength;
    }

    private boolean equalsSignalStrength(NetworkCapabilities networkCapabilities) {
        return this.mSignalStrength == networkCapabilities.mSignalStrength;
    }

    public NetworkCapabilities setSingleUid(int i) {
        ArraySet arraySet = new ArraySet(1);
        arraySet.add(new UidRange(i, i));
        setUids(arraySet);
        return this;
    }

    public NetworkCapabilities setUids(Set<UidRange> set) {
        if (set == null) {
            this.mUids = null;
        } else {
            this.mUids = new ArraySet<>(set);
        }
        return this;
    }

    public Set<UidRange> getUids() {
        if (this.mUids == null) {
            return null;
        }
        return new ArraySet((ArraySet) this.mUids);
    }

    public boolean appliesToUid(int i) {
        if (this.mUids == null) {
            return true;
        }
        Iterator<UidRange> it = this.mUids.iterator();
        while (it.hasNext()) {
            if (it.next().contains(i)) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    public boolean equalsUids(NetworkCapabilities networkCapabilities) {
        ArraySet<UidRange> arraySet = networkCapabilities.mUids;
        if (arraySet == null) {
            return this.mUids == null;
        }
        if (this.mUids == null) {
            return false;
        }
        ArraySet arraySet2 = new ArraySet((ArraySet) this.mUids);
        for (UidRange uidRange : arraySet) {
            if (!arraySet2.contains(uidRange)) {
                return false;
            }
            arraySet2.remove(uidRange);
        }
        return arraySet2.isEmpty();
    }

    public boolean satisfiedByUids(NetworkCapabilities networkCapabilities) {
        if (networkCapabilities.mUids == null || this.mUids == null) {
            return true;
        }
        for (UidRange uidRange : this.mUids) {
            if (uidRange.contains(networkCapabilities.mEstablishingVpnAppUid)) {
                return true;
            }
            if (!networkCapabilities.appliesToUidRange(uidRange)) {
                return false;
            }
        }
        return true;
    }

    @VisibleForTesting
    public boolean appliesToUidRange(UidRange uidRange) {
        if (this.mUids == null) {
            return true;
        }
        Iterator<UidRange> it = this.mUids.iterator();
        while (it.hasNext()) {
            if (it.next().containsRange(uidRange)) {
                return true;
            }
        }
        return false;
    }

    private void combineUids(NetworkCapabilities networkCapabilities) {
        if (networkCapabilities.mUids == null || this.mUids == null) {
            this.mUids = null;
        } else {
            this.mUids.addAll((ArraySet<? extends UidRange>) networkCapabilities.mUids);
        }
    }

    public NetworkCapabilities setSSID(String str) {
        this.mSSID = str;
        return this;
    }

    public String getSSID() {
        return this.mSSID;
    }

    public boolean equalsSSID(NetworkCapabilities networkCapabilities) {
        return Objects.equals(this.mSSID, networkCapabilities.mSSID);
    }

    public boolean satisfiedBySSID(NetworkCapabilities networkCapabilities) {
        return this.mSSID == null || this.mSSID.equals(networkCapabilities.mSSID);
    }

    private void combineSSIDs(NetworkCapabilities networkCapabilities) {
        if (this.mSSID != null && !this.mSSID.equals(networkCapabilities.mSSID)) {
            throw new IllegalStateException("Can't combine two SSIDs");
        }
        setSSID(networkCapabilities.mSSID);
    }

    public void combineCapabilities(NetworkCapabilities networkCapabilities) {
        combineNetCapabilities(networkCapabilities);
        combineTransportTypes(networkCapabilities);
        combineLinkBandwidths(networkCapabilities);
        combineSpecifiers(networkCapabilities);
        combineSignalStrength(networkCapabilities);
        combineUids(networkCapabilities);
        combineSSIDs(networkCapabilities);
    }

    private boolean satisfiedByNetworkCapabilities(NetworkCapabilities networkCapabilities, boolean z) {
        return networkCapabilities != null && satisfiedByNetCapabilities(networkCapabilities, z) && satisfiedByTransportTypes(networkCapabilities) && (z || satisfiedByLinkBandwidths(networkCapabilities)) && satisfiedBySpecifier(networkCapabilities) && ((z || satisfiedBySignalStrength(networkCapabilities)) && ((z || satisfiedByUids(networkCapabilities)) && (z || satisfiedBySSID(networkCapabilities))));
    }

    public boolean satisfiedByNetworkCapabilities(NetworkCapabilities networkCapabilities) {
        return satisfiedByNetworkCapabilities(networkCapabilities, false);
    }

    public boolean satisfiedByImmutableNetworkCapabilities(NetworkCapabilities networkCapabilities) {
        return satisfiedByNetworkCapabilities(networkCapabilities, true);
    }

    public String describeImmutableDifferences(NetworkCapabilities networkCapabilities) {
        if (networkCapabilities == null) {
            return "other NetworkCapabilities was null";
        }
        StringJoiner stringJoiner = new StringJoiner(", ");
        long j = this.mNetworkCapabilities & (-4147201);
        long j2 = (-4147201) & networkCapabilities.mNetworkCapabilities;
        if (j != j2) {
            stringJoiner.add(String.format("immutable capabilities changed: %s -> %s", capabilityNamesOf(BitUtils.unpackBits(j)), capabilityNamesOf(BitUtils.unpackBits(j2))));
        }
        if (!equalsSpecifier(networkCapabilities)) {
            stringJoiner.add(String.format("specifier changed: %s -> %s", getNetworkSpecifier(), networkCapabilities.getNetworkSpecifier()));
        }
        if (!equalsTransportTypes(networkCapabilities)) {
            stringJoiner.add(String.format("transports changed: %s -> %s", transportNamesOf(getTransportTypes()), transportNamesOf(networkCapabilities.getTransportTypes())));
        }
        return stringJoiner.toString();
    }

    public boolean equalRequestableCapabilities(NetworkCapabilities networkCapabilities) {
        return networkCapabilities != null && equalsNetCapabilitiesRequestable(networkCapabilities) && equalsTransportTypes(networkCapabilities) && equalsSpecifier(networkCapabilities);
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof NetworkCapabilities)) {
            return false;
        }
        NetworkCapabilities networkCapabilities = (NetworkCapabilities) obj;
        return equalsNetCapabilities(networkCapabilities) && equalsTransportTypes(networkCapabilities) && equalsLinkBandwidths(networkCapabilities) && equalsSignalStrength(networkCapabilities) && equalsSpecifier(networkCapabilities) && equalsUids(networkCapabilities) && equalsSSID(networkCapabilities);
    }

    public int hashCode() {
        return ((int) (this.mNetworkCapabilities & (-1))) + (((int) (this.mNetworkCapabilities >> 32)) * 3) + (((int) (this.mUnwantedNetworkCapabilities & (-1))) * 5) + (((int) (this.mUnwantedNetworkCapabilities >> 32)) * 7) + (((int) ((-1) & this.mTransportTypes)) * 11) + (((int) (this.mTransportTypes >> 32)) * 13) + (this.mLinkUpBandwidthKbps * 17) + (this.mLinkDownBandwidthKbps * 19) + (Objects.hashCode(this.mNetworkSpecifier) * 23) + (this.mSignalStrength * 29) + (Objects.hashCode(this.mUids) * 31) + (Objects.hashCode(this.mSSID) * 37);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mNetworkCapabilities);
        parcel.writeLong(this.mUnwantedNetworkCapabilities);
        parcel.writeLong(this.mTransportTypes);
        parcel.writeInt(this.mLinkUpBandwidthKbps);
        parcel.writeInt(this.mLinkDownBandwidthKbps);
        parcel.writeParcelable((Parcelable) this.mNetworkSpecifier, i);
        parcel.writeInt(this.mSignalStrength);
        parcel.writeArraySet(this.mUids);
        parcel.writeString(this.mSSID);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        if (0 != this.mTransportTypes) {
            sb.append(" Transports: ");
            appendStringRepresentationOfBitMaskToStringBuilder(sb, this.mTransportTypes, new NameOf() {
                @Override
                public final String nameOf(int i) {
                    return NetworkCapabilities.transportNameOf(i);
                }
            }, "|");
        }
        if (0 != this.mNetworkCapabilities) {
            sb.append(" Capabilities: ");
            appendStringRepresentationOfBitMaskToStringBuilder(sb, this.mNetworkCapabilities, new NameOf() {
                @Override
                public final String nameOf(int i) {
                    return NetworkCapabilities.capabilityNameOf(i);
                }
            }, "&");
        }
        if (0 != this.mNetworkCapabilities) {
            sb.append(" Unwanted: ");
            appendStringRepresentationOfBitMaskToStringBuilder(sb, this.mUnwantedNetworkCapabilities, new NameOf() {
                @Override
                public final String nameOf(int i) {
                    return NetworkCapabilities.capabilityNameOf(i);
                }
            }, "&");
        }
        if (this.mLinkUpBandwidthKbps > 0) {
            sb.append(" LinkUpBandwidth>=");
            sb.append(this.mLinkUpBandwidthKbps);
            sb.append("Kbps");
        }
        if (this.mLinkDownBandwidthKbps > 0) {
            sb.append(" LinkDnBandwidth>=");
            sb.append(this.mLinkDownBandwidthKbps);
            sb.append("Kbps");
        }
        if (this.mNetworkSpecifier != null) {
            sb.append(" Specifier: <");
            sb.append(this.mNetworkSpecifier);
            sb.append(">");
        }
        if (hasSignalStrength()) {
            sb.append(" SignalStrength: ");
            sb.append(this.mSignalStrength);
        }
        if (this.mUids != null) {
            if (1 == this.mUids.size() && this.mUids.valueAt(0).count() == 1) {
                sb.append(" Uid: ");
                sb.append(this.mUids.valueAt(0).start);
            } else {
                sb.append(" Uids: <");
                sb.append(this.mUids);
                sb.append(">");
            }
        }
        if (this.mEstablishingVpnAppUid != -1) {
            sb.append(" EstablishingAppUid: ");
            sb.append(this.mEstablishingVpnAppUid);
        }
        if (this.mSSID != null) {
            sb.append(" SSID: ");
            sb.append(this.mSSID);
        }
        sb.append("]");
        return sb.toString();
    }

    public static void appendStringRepresentationOfBitMaskToStringBuilder(StringBuilder sb, long j, NameOf nameOf, String str) {
        boolean z = false;
        int i = 0;
        while (j != 0) {
            if ((1 & j) != 0) {
                if (z) {
                    sb.append(str);
                } else {
                    z = true;
                }
                sb.append(nameOf.nameOf(i));
            }
            j >>= 1;
            i++;
        }
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        for (int i : getTransportTypes()) {
            protoOutputStream.write(2259152797697L, i);
        }
        for (int i2 : getCapabilities()) {
            protoOutputStream.write(2259152797698L, i2);
        }
        protoOutputStream.write(1120986464259L, this.mLinkUpBandwidthKbps);
        protoOutputStream.write(1120986464260L, this.mLinkDownBandwidthKbps);
        if (this.mNetworkSpecifier != null) {
            protoOutputStream.write(1138166333445L, this.mNetworkSpecifier.toString());
        }
        protoOutputStream.write(1133871366150L, hasSignalStrength());
        protoOutputStream.write(NetworkCapabilitiesProto.SIGNAL_STRENGTH, this.mSignalStrength);
        protoOutputStream.end(jStart);
    }

    public static String capabilityNamesOf(int[] iArr) {
        StringJoiner stringJoiner = new StringJoiner("|");
        if (iArr != null) {
            for (int i : iArr) {
                stringJoiner.add(capabilityNameOf(i));
            }
        }
        return stringJoiner.toString();
    }

    public static String capabilityNameOf(int i) {
        switch (i) {
            case 0:
                return "MMS";
            case 1:
                return "SUPL";
            case 2:
                return "DUN";
            case 3:
                return "FOTA";
            case 4:
                return "IMS";
            case 5:
                return "CBS";
            case 6:
                return "WIFI_P2P";
            case 7:
                return "IA";
            case 8:
                return "RCS";
            case 9:
                return "XCAP";
            case 10:
                return "EIMS";
            case 11:
                return "NOT_METERED";
            case 12:
                return "INTERNET";
            case 13:
                return "NOT_RESTRICTED";
            case 14:
                return "TRUSTED";
            case 15:
                return "NOT_VPN";
            case 16:
                return "VALIDATED";
            case 17:
                return "CAPTIVE_PORTAL";
            case 18:
                return "NOT_ROAMING";
            case 19:
                return "FOREGROUND";
            case 20:
                return "NOT_CONGESTED";
            case 21:
                return "NOT_SUSPENDED";
            case 22:
                return "OEM_PAID";
            case 23:
            case 24:
            default:
                return Integer.toString(i);
            case 25:
                return "WAP";
            case 26:
                return "VSIM";
            case 27:
                return "BIP";
            case 28:
                return "PREEMPTIVE";
        }
    }

    public static String transportNamesOf(int[] iArr) {
        StringJoiner stringJoiner = new StringJoiner("|");
        if (iArr != null) {
            for (int i : iArr) {
                stringJoiner.add(transportNameOf(i));
            }
        }
        return stringJoiner.toString();
    }

    public static String transportNameOf(int i) {
        if (!isValidTransport(i)) {
            return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
        }
        return TRANSPORT_NAMES[i];
    }

    private static void checkValidTransportType(int i) {
        Preconditions.checkArgument(isValidTransport(i), "Invalid TransportType " + i);
    }

    private static boolean isValidCapability(int i) {
        return i >= 0 && i <= 28;
    }

    private static void checkValidCapability(int i) {
        Preconditions.checkArgument(isValidCapability(i), "NetworkCapability " + i + "out of range");
    }
}
