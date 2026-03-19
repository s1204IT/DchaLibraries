package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;

public final class IpPrefix implements Parcelable {
    public static final Parcelable.Creator<IpPrefix> CREATOR = new Parcelable.Creator<IpPrefix>() {
        @Override
        public IpPrefix createFromParcel(Parcel parcel) {
            return new IpPrefix(parcel.createByteArray(), parcel.readInt());
        }

        @Override
        public IpPrefix[] newArray(int i) {
            return new IpPrefix[i];
        }
    };
    private final byte[] address;
    private final int prefixLength;

    private void checkAndMaskAddressAndPrefixLength() {
        if (this.address.length != 4 && this.address.length != 16) {
            throw new IllegalArgumentException("IpPrefix has " + this.address.length + " bytes which is neither 4 nor 16");
        }
        NetworkUtils.maskRawAddress(this.address, this.prefixLength);
    }

    public IpPrefix(byte[] bArr, int i) {
        this.address = (byte[]) bArr.clone();
        this.prefixLength = i;
        checkAndMaskAddressAndPrefixLength();
    }

    public IpPrefix(InetAddress inetAddress, int i) {
        this.address = inetAddress.getAddress();
        this.prefixLength = i;
        checkAndMaskAddressAndPrefixLength();
    }

    public IpPrefix(String str) {
        Pair<InetAddress, Integer> ipAndMask = NetworkUtils.parseIpAndMask(str);
        this.address = ipAndMask.first.getAddress();
        this.prefixLength = ipAndMask.second.intValue();
        checkAndMaskAddressAndPrefixLength();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof IpPrefix)) {
            return false;
        }
        IpPrefix ipPrefix = (IpPrefix) obj;
        return Arrays.equals(this.address, ipPrefix.address) && this.prefixLength == ipPrefix.prefixLength;
    }

    public int hashCode() {
        return Arrays.hashCode(this.address) + (11 * this.prefixLength);
    }

    public InetAddress getAddress() {
        try {
            return InetAddress.getByAddress(this.address);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public byte[] getRawAddress() {
        return (byte[]) this.address.clone();
    }

    public int getPrefixLength() {
        return this.prefixLength;
    }

    public boolean contains(InetAddress inetAddress) {
        byte[] address = inetAddress == null ? null : inetAddress.getAddress();
        if (address == null || address.length != this.address.length) {
            return false;
        }
        NetworkUtils.maskRawAddress(address, this.prefixLength);
        return Arrays.equals(this.address, address);
    }

    public boolean containsPrefix(IpPrefix ipPrefix) {
        if (ipPrefix.getPrefixLength() < this.prefixLength) {
            return false;
        }
        byte[] rawAddress = ipPrefix.getRawAddress();
        NetworkUtils.maskRawAddress(rawAddress, this.prefixLength);
        return Arrays.equals(rawAddress, this.address);
    }

    public boolean isIPv6() {
        return getAddress() instanceof Inet6Address;
    }

    public boolean isIPv4() {
        return getAddress() instanceof Inet4Address;
    }

    public String toString() {
        try {
            return InetAddress.getByAddress(this.address).getHostAddress() + "/" + this.prefixLength;
        } catch (UnknownHostException e) {
            throw new IllegalStateException("IpPrefix with invalid address! Shouldn't happen.", e);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByteArray(this.address);
        parcel.writeInt(this.prefixLength);
    }

    public static Comparator<IpPrefix> lengthComparator() {
        return new Comparator<IpPrefix>() {
            @Override
            public int compare(IpPrefix ipPrefix, IpPrefix ipPrefix2) {
                if (ipPrefix.isIPv4()) {
                    if (ipPrefix2.isIPv6()) {
                        return -1;
                    }
                } else if (ipPrefix2.isIPv4()) {
                    return 1;
                }
                int prefixLength = ipPrefix.getPrefixLength();
                int prefixLength2 = ipPrefix2.getPrefixLength();
                if (prefixLength < prefixLength2) {
                    return -1;
                }
                if (prefixLength2 < prefixLength) {
                    return 1;
                }
                byte[] bArr = ipPrefix.address;
                byte[] bArr2 = ipPrefix2.address;
                int length = bArr.length < bArr2.length ? bArr.length : bArr2.length;
                for (int i = 0; i < length; i++) {
                    if (bArr[i] < bArr2[i]) {
                        return -1;
                    }
                    if (bArr[i] > bArr2[i]) {
                        return 1;
                    }
                }
                if (bArr2.length < length) {
                    return 1;
                }
                return bArr.length < length ? -1 : 0;
            }
        };
    }
}
