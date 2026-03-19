package android.net.wifi.aware;

import android.net.NetworkSpecifier;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.logging.nano.MetricsProto;
import java.util.Arrays;
import java.util.Objects;

public final class WifiAwareNetworkSpecifier extends NetworkSpecifier implements Parcelable {
    public static final Parcelable.Creator<WifiAwareNetworkSpecifier> CREATOR = new Parcelable.Creator<WifiAwareNetworkSpecifier>() {
        @Override
        public WifiAwareNetworkSpecifier createFromParcel(Parcel parcel) {
            return new WifiAwareNetworkSpecifier(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.createByteArray(), parcel.createByteArray(), parcel.readString(), parcel.readInt());
        }

        @Override
        public WifiAwareNetworkSpecifier[] newArray(int i) {
            return new WifiAwareNetworkSpecifier[i];
        }
    };
    public static final int NETWORK_SPECIFIER_TYPE_IB = 0;
    public static final int NETWORK_SPECIFIER_TYPE_IB_ANY_PEER = 1;
    public static final int NETWORK_SPECIFIER_TYPE_MAX_VALID = 3;
    public static final int NETWORK_SPECIFIER_TYPE_OOB = 2;
    public static final int NETWORK_SPECIFIER_TYPE_OOB_ANY_PEER = 3;
    public final int clientId;
    public final String passphrase;
    public final int peerId;
    public final byte[] peerMac;
    public final byte[] pmk;
    public final int requestorUid;
    public final int role;
    public final int sessionId;
    public final int type;

    public WifiAwareNetworkSpecifier(int i, int i2, int i3, int i4, int i5, byte[] bArr, byte[] bArr2, String str, int i6) {
        this.type = i;
        this.role = i2;
        this.clientId = i3;
        this.sessionId = i4;
        this.peerId = i5;
        this.peerMac = bArr;
        this.pmk = bArr2;
        this.passphrase = str;
        this.requestorUid = i6;
    }

    public boolean isOutOfBand() {
        return this.type == 2 || this.type == 3;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.type);
        parcel.writeInt(this.role);
        parcel.writeInt(this.clientId);
        parcel.writeInt(this.sessionId);
        parcel.writeInt(this.peerId);
        parcel.writeByteArray(this.peerMac);
        parcel.writeByteArray(this.pmk);
        parcel.writeString(this.passphrase);
        parcel.writeInt(this.requestorUid);
    }

    @Override
    public boolean satisfiedBy(NetworkSpecifier networkSpecifier) {
        if (networkSpecifier instanceof WifiAwareAgentNetworkSpecifier) {
            return ((WifiAwareAgentNetworkSpecifier) networkSpecifier).satisfiesAwareNetworkSpecifier(this);
        }
        return equals(networkSpecifier);
    }

    public int hashCode() {
        return (31 * (((((((((((((((MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + this.type) * 31) + this.role) * 31) + this.clientId) * 31) + this.sessionId) * 31) + this.peerId) * 31) + Arrays.hashCode(this.peerMac)) * 31) + Arrays.hashCode(this.pmk)) * 31) + Objects.hashCode(this.passphrase))) + this.requestorUid;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WifiAwareNetworkSpecifier)) {
            return false;
        }
        WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier = (WifiAwareNetworkSpecifier) obj;
        return this.type == wifiAwareNetworkSpecifier.type && this.role == wifiAwareNetworkSpecifier.role && this.clientId == wifiAwareNetworkSpecifier.clientId && this.sessionId == wifiAwareNetworkSpecifier.sessionId && this.peerId == wifiAwareNetworkSpecifier.peerId && Arrays.equals(this.peerMac, wifiAwareNetworkSpecifier.peerMac) && Arrays.equals(this.pmk, wifiAwareNetworkSpecifier.pmk) && Objects.equals(this.passphrase, wifiAwareNetworkSpecifier.passphrase) && this.requestorUid == wifiAwareNetworkSpecifier.requestorUid;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("WifiAwareNetworkSpecifier [");
        sb.append("type=");
        sb.append(this.type);
        sb.append(", role=");
        sb.append(this.role);
        sb.append(", clientId=");
        sb.append(this.clientId);
        sb.append(", sessionId=");
        sb.append(this.sessionId);
        sb.append(", peerId=");
        sb.append(this.peerId);
        sb.append(", peerMac=");
        sb.append(this.peerMac == null ? "<null>" : "<non-null>");
        sb.append(", pmk=");
        sb.append(this.pmk == null ? "<null>" : "<non-null>");
        sb.append(", passphrase=");
        sb.append(this.passphrase == null ? "<null>" : "<non-null>");
        sb.append(", requestorUid=");
        sb.append(this.requestorUid);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public void assertValidFromUid(int i) {
        if (this.requestorUid != i) {
            throw new SecurityException("mismatched UIDs");
        }
    }
}
