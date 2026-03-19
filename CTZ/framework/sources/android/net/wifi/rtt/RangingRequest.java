package android.net.wifi.rtt;

import android.annotation.SystemApi;
import android.net.MacAddress;
import android.net.wifi.ScanResult;
import android.net.wifi.aware.PeerHandle;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

public final class RangingRequest implements Parcelable {
    public static final Parcelable.Creator<RangingRequest> CREATOR = new Parcelable.Creator<RangingRequest>() {
        @Override
        public RangingRequest[] newArray(int i) {
            return new RangingRequest[i];
        }

        @Override
        public RangingRequest createFromParcel(Parcel parcel) {
            return new RangingRequest(parcel.readArrayList(null));
        }
    };
    private static final int MAX_PEERS = 10;
    public final List<ResponderConfig> mRttPeers;

    public static int getMaxPeers() {
        return 10;
    }

    private RangingRequest(List<ResponderConfig> list) {
        this.mRttPeers = list;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeList(this.mRttPeers);
    }

    public String toString() {
        StringJoiner stringJoiner = new StringJoiner(", ", "RangingRequest: mRttPeers=[", "]");
        Iterator<ResponderConfig> it = this.mRttPeers.iterator();
        while (it.hasNext()) {
            stringJoiner.add(it.next().toString());
        }
        return stringJoiner.toString();
    }

    public void enforceValidity(boolean z) {
        if (this.mRttPeers.size() > 10) {
            throw new IllegalArgumentException("Ranging to too many peers requested. Use getMaxPeers() API to get limit.");
        }
        Iterator<ResponderConfig> it = this.mRttPeers.iterator();
        while (it.hasNext()) {
            if (!it.next().isValid(z)) {
                throw new IllegalArgumentException("Invalid Responder specification");
            }
        }
    }

    public static final class Builder {
        private List<ResponderConfig> mRttPeers = new ArrayList();

        public Builder addAccessPoint(ScanResult scanResult) {
            if (scanResult == null) {
                throw new IllegalArgumentException("Null ScanResult!");
            }
            return addResponder(ResponderConfig.fromScanResult(scanResult));
        }

        public Builder addAccessPoints(List<ScanResult> list) {
            if (list == null) {
                throw new IllegalArgumentException("Null list of ScanResults!");
            }
            Iterator<ScanResult> it = list.iterator();
            while (it.hasNext()) {
                addAccessPoint(it.next());
            }
            return this;
        }

        public Builder addWifiAwarePeer(MacAddress macAddress) {
            if (macAddress == null) {
                throw new IllegalArgumentException("Null peer MAC address");
            }
            return addResponder(ResponderConfig.fromWifiAwarePeerMacAddressWithDefaults(macAddress));
        }

        public Builder addWifiAwarePeer(PeerHandle peerHandle) {
            if (peerHandle == null) {
                throw new IllegalArgumentException("Null peer handler (identifier)");
            }
            return addResponder(ResponderConfig.fromWifiAwarePeerHandleWithDefaults(peerHandle));
        }

        @SystemApi
        public Builder addResponder(ResponderConfig responderConfig) {
            if (responderConfig == null) {
                throw new IllegalArgumentException("Null Responder!");
            }
            this.mRttPeers.add(responderConfig);
            return this;
        }

        public RangingRequest build() {
            return new RangingRequest(this.mRttPeers);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RangingRequest)) {
            return false;
        }
        RangingRequest rangingRequest = (RangingRequest) obj;
        return this.mRttPeers.size() == rangingRequest.mRttPeers.size() && this.mRttPeers.containsAll(rangingRequest.mRttPeers);
    }

    public int hashCode() {
        return this.mRttPeers.hashCode();
    }
}
