package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public final class IpPrefix implements Parcelable {
    public static final Parcelable.Creator<IpPrefix> CREATOR = new Parcelable.Creator<IpPrefix>() {
        @Override
        public IpPrefix createFromParcel(Parcel in) {
            byte[] address = in.createByteArray();
            int prefixLength = in.readInt();
            return new IpPrefix(address, prefixLength);
        }

        @Override
        public IpPrefix[] newArray(int size) {
            return new IpPrefix[size];
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

    public IpPrefix(byte[] address, int prefixLength) {
        this.address = (byte[]) address.clone();
        this.prefixLength = prefixLength;
        checkAndMaskAddressAndPrefixLength();
    }

    public IpPrefix(InetAddress address, int prefixLength) {
        this.address = address.getAddress();
        this.prefixLength = prefixLength;
        checkAndMaskAddressAndPrefixLength();
    }

    public IpPrefix(String prefix) {
        Pair<InetAddress, Integer> ipAndMask = NetworkUtils.parseIpAndMask(prefix);
        this.address = ipAndMask.first.getAddress();
        this.prefixLength = ipAndMask.second.intValue();
        checkAndMaskAddressAndPrefixLength();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof IpPrefix)) {
            return false;
        }
        IpPrefix that = (IpPrefix) obj;
        return Arrays.equals(this.address, that.address) && this.prefixLength == that.prefixLength;
    }

    public int hashCode() {
        return Arrays.hashCode(this.address) + (this.prefixLength * 11);
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
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(this.address);
        dest.writeInt(this.prefixLength);
    }
}
