package android.net.wifi.aware;

import android.net.NetworkSpecifier;
import android.os.Parcel;
import android.os.Parcelable;
import android.security.keystore.KeyProperties;
import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringJoiner;
import libcore.util.HexEncoding;

public class WifiAwareAgentNetworkSpecifier extends NetworkSpecifier implements Parcelable {
    public static final Parcelable.Creator<WifiAwareAgentNetworkSpecifier> CREATOR = new Parcelable.Creator<WifiAwareAgentNetworkSpecifier>() {
        @Override
        public WifiAwareAgentNetworkSpecifier createFromParcel(Parcel parcel) {
            WifiAwareAgentNetworkSpecifier wifiAwareAgentNetworkSpecifier = new WifiAwareAgentNetworkSpecifier();
            for (Object obj : parcel.readArray(null)) {
                wifiAwareAgentNetworkSpecifier.mNetworkSpecifiers.add((ByteArrayWrapper) obj);
            }
            return wifiAwareAgentNetworkSpecifier;
        }

        @Override
        public WifiAwareAgentNetworkSpecifier[] newArray(int i) {
            return new WifiAwareAgentNetworkSpecifier[i];
        }
    };
    private static final String TAG = "WifiAwareAgentNs";
    private static final boolean VDBG = false;
    private MessageDigest mDigester;
    private Set<ByteArrayWrapper> mNetworkSpecifiers = new HashSet();

    public WifiAwareAgentNetworkSpecifier() {
    }

    public WifiAwareAgentNetworkSpecifier(WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier) {
        initialize();
        this.mNetworkSpecifiers.add(convert(wifiAwareNetworkSpecifier));
    }

    public WifiAwareAgentNetworkSpecifier(WifiAwareNetworkSpecifier[] wifiAwareNetworkSpecifierArr) {
        initialize();
        for (WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier : wifiAwareNetworkSpecifierArr) {
            this.mNetworkSpecifiers.add(convert(wifiAwareNetworkSpecifier));
        }
    }

    public boolean isEmpty() {
        return this.mNetworkSpecifiers.isEmpty();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeArray(this.mNetworkSpecifiers.toArray());
    }

    public int hashCode() {
        return this.mNetworkSpecifiers.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof WifiAwareAgentNetworkSpecifier)) {
            return false;
        }
        return this.mNetworkSpecifiers.equals(((WifiAwareAgentNetworkSpecifier) obj).mNetworkSpecifiers);
    }

    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(",");
        Iterator<ByteArrayWrapper> it = this.mNetworkSpecifiers.iterator();
        while (it.hasNext()) {
            stringJoiner.add(it.next().toString());
        }
        return stringJoiner.toString();
    }

    @Override
    public boolean satisfiedBy(NetworkSpecifier networkSpecifier) {
        if (!(networkSpecifier instanceof WifiAwareAgentNetworkSpecifier)) {
            return false;
        }
        WifiAwareAgentNetworkSpecifier wifiAwareAgentNetworkSpecifier = (WifiAwareAgentNetworkSpecifier) networkSpecifier;
        Iterator<ByteArrayWrapper> it = this.mNetworkSpecifiers.iterator();
        while (it.hasNext()) {
            if (!wifiAwareAgentNetworkSpecifier.mNetworkSpecifiers.contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    public boolean satisfiesAwareNetworkSpecifier(WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier) {
        return this.mNetworkSpecifiers.contains(convert(wifiAwareNetworkSpecifier));
    }

    @Override
    public void assertValidFromUid(int i) {
        throw new SecurityException("WifiAwareAgentNetworkSpecifier should not be used in network requests");
    }

    @Override
    public NetworkSpecifier redact() {
        return null;
    }

    private void initialize() {
        try {
            this.mDigester = MessageDigest.getInstance(KeyProperties.DIGEST_SHA256);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Can not instantiate a SHA-256 digester!? Will match nothing.");
        }
    }

    private ByteArrayWrapper convert(WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier) {
        if (this.mDigester == null) {
            return null;
        }
        Parcel parcelObtain = Parcel.obtain();
        wifiAwareNetworkSpecifier.writeToParcel(parcelObtain, 0);
        byte[] bArrMarshall = parcelObtain.marshall();
        this.mDigester.reset();
        this.mDigester.update(bArrMarshall);
        return new ByteArrayWrapper(this.mDigester.digest());
    }

    private static class ByteArrayWrapper implements Parcelable {
        public static final Parcelable.Creator<ByteArrayWrapper> CREATOR = new Parcelable.Creator<ByteArrayWrapper>() {
            @Override
            public ByteArrayWrapper createFromParcel(Parcel parcel) {
                return new ByteArrayWrapper(parcel.readBlob());
            }

            @Override
            public ByteArrayWrapper[] newArray(int i) {
                return new ByteArrayWrapper[i];
            }
        };
        private byte[] mData;

        ByteArrayWrapper(byte[] bArr) {
            this.mData = bArr;
        }

        public int hashCode() {
            return Arrays.hashCode(this.mData);
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof ByteArrayWrapper)) {
                return false;
            }
            return Arrays.equals(((ByteArrayWrapper) obj).mData, this.mData);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeBlob(this.mData);
        }

        public String toString() {
            return new String(HexEncoding.encode(this.mData));
        }
    }
}
