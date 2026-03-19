package android.net.wifi.rtt;

import android.annotation.SystemApi;
import android.net.MacAddress;
import android.net.wifi.aware.PeerHandle;
import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Objects;

public final class RangingResult implements Parcelable {
    public static final int STATUS_FAIL = 1;
    public static final int STATUS_RESPONDER_DOES_NOT_SUPPORT_IEEE80211MC = 2;
    public static final int STATUS_SUCCESS = 0;
    private static final String TAG = "RangingResult";
    private final int mDistanceMm;
    private final int mDistanceStdDevMm;
    private final byte[] mLci;
    private final byte[] mLcr;
    private final MacAddress mMac;
    private final int mNumAttemptedMeasurements;
    private final int mNumSuccessfulMeasurements;
    private final PeerHandle mPeerHandle;
    private final int mRssi;
    private final int mStatus;
    private final long mTimestamp;
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    public static final Parcelable.Creator<RangingResult> CREATOR = new Parcelable.Creator<RangingResult>() {
        @Override
        public RangingResult[] newArray(int i) {
            return new RangingResult[i];
        }

        @Override
        public RangingResult createFromParcel(Parcel parcel) {
            MacAddress macAddressCreateFromParcel;
            int i = parcel.readInt();
            PeerHandle peerHandle = null;
            if (!parcel.readBoolean()) {
                macAddressCreateFromParcel = null;
            } else {
                macAddressCreateFromParcel = MacAddress.CREATOR.createFromParcel(parcel);
            }
            boolean z = parcel.readBoolean();
            if (z) {
                peerHandle = new PeerHandle(parcel.readInt());
            }
            int i2 = parcel.readInt();
            int i3 = parcel.readInt();
            int i4 = parcel.readInt();
            int i5 = parcel.readInt();
            int i6 = parcel.readInt();
            byte[] bArrCreateByteArray = parcel.createByteArray();
            byte[] bArrCreateByteArray2 = parcel.createByteArray();
            long j = parcel.readLong();
            if (z) {
                return new RangingResult(i, peerHandle, i2, i3, i4, i5, i6, bArrCreateByteArray, bArrCreateByteArray2, j);
            }
            return new RangingResult(i, macAddressCreateFromParcel, i2, i3, i4, i5, i6, bArrCreateByteArray, bArrCreateByteArray2, j);
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface RangeResultStatus {
    }

    public RangingResult(int i, MacAddress macAddress, int i2, int i3, int i4, int i5, int i6, byte[] bArr, byte[] bArr2, long j) {
        this.mStatus = i;
        this.mMac = macAddress;
        this.mPeerHandle = null;
        this.mDistanceMm = i2;
        this.mDistanceStdDevMm = i3;
        this.mRssi = i4;
        this.mNumAttemptedMeasurements = i5;
        this.mNumSuccessfulMeasurements = i6;
        this.mLci = bArr == null ? EMPTY_BYTE_ARRAY : bArr;
        this.mLcr = bArr2 == null ? EMPTY_BYTE_ARRAY : bArr2;
        this.mTimestamp = j;
    }

    public RangingResult(int i, PeerHandle peerHandle, int i2, int i3, int i4, int i5, int i6, byte[] bArr, byte[] bArr2, long j) {
        this.mStatus = i;
        this.mMac = null;
        this.mPeerHandle = peerHandle;
        this.mDistanceMm = i2;
        this.mDistanceStdDevMm = i3;
        this.mRssi = i4;
        this.mNumAttemptedMeasurements = i5;
        this.mNumSuccessfulMeasurements = i6;
        this.mLci = bArr == null ? EMPTY_BYTE_ARRAY : bArr;
        this.mLcr = bArr2 == null ? EMPTY_BYTE_ARRAY : bArr2;
        this.mTimestamp = j;
    }

    public int getStatus() {
        return this.mStatus;
    }

    public MacAddress getMacAddress() {
        return this.mMac;
    }

    public PeerHandle getPeerHandle() {
        return this.mPeerHandle;
    }

    public int getDistanceMm() {
        if (this.mStatus != 0) {
            throw new IllegalStateException("getDistanceMm(): invoked on an invalid result: getStatus()=" + this.mStatus);
        }
        return this.mDistanceMm;
    }

    public int getDistanceStdDevMm() {
        if (this.mStatus != 0) {
            throw new IllegalStateException("getDistanceStdDevMm(): invoked on an invalid result: getStatus()=" + this.mStatus);
        }
        return this.mDistanceStdDevMm;
    }

    public int getRssi() {
        if (this.mStatus != 0) {
            throw new IllegalStateException("getRssi(): invoked on an invalid result: getStatus()=" + this.mStatus);
        }
        return this.mRssi;
    }

    public int getNumAttemptedMeasurements() {
        if (this.mStatus != 0) {
            throw new IllegalStateException("getNumAttemptedMeasurements(): invoked on an invalid result: getStatus()=" + this.mStatus);
        }
        return this.mNumAttemptedMeasurements;
    }

    public int getNumSuccessfulMeasurements() {
        if (this.mStatus != 0) {
            throw new IllegalStateException("getNumSuccessfulMeasurements(): invoked on an invalid result: getStatus()=" + this.mStatus);
        }
        return this.mNumSuccessfulMeasurements;
    }

    @SystemApi
    public byte[] getLci() {
        if (this.mStatus != 0) {
            throw new IllegalStateException("getLci(): invoked on an invalid result: getStatus()=" + this.mStatus);
        }
        return this.mLci;
    }

    @SystemApi
    public byte[] getLcr() {
        if (this.mStatus != 0) {
            throw new IllegalStateException("getReportedLocationCivic(): invoked on an invalid result: getStatus()=" + this.mStatus);
        }
        return this.mLcr;
    }

    public long getRangingTimestampMillis() {
        if (this.mStatus != 0) {
            throw new IllegalStateException("getRangingTimestampMillis(): invoked on an invalid result: getStatus()=" + this.mStatus);
        }
        return this.mTimestamp;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mStatus);
        if (this.mMac == null) {
            parcel.writeBoolean(false);
        } else {
            parcel.writeBoolean(true);
            this.mMac.writeToParcel(parcel, i);
        }
        if (this.mPeerHandle == null) {
            parcel.writeBoolean(false);
        } else {
            parcel.writeBoolean(true);
            parcel.writeInt(this.mPeerHandle.peerId);
        }
        parcel.writeInt(this.mDistanceMm);
        parcel.writeInt(this.mDistanceStdDevMm);
        parcel.writeInt(this.mRssi);
        parcel.writeInt(this.mNumAttemptedMeasurements);
        parcel.writeInt(this.mNumSuccessfulMeasurements);
        parcel.writeByteArray(this.mLci);
        parcel.writeByteArray(this.mLcr);
        parcel.writeLong(this.mTimestamp);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RangingResult: [status=");
        sb.append(this.mStatus);
        sb.append(", mac=");
        sb.append(this.mMac);
        sb.append(", peerHandle=");
        sb.append(this.mPeerHandle == null ? "<null>" : Integer.valueOf(this.mPeerHandle.peerId));
        sb.append(", distanceMm=");
        sb.append(this.mDistanceMm);
        sb.append(", distanceStdDevMm=");
        sb.append(this.mDistanceStdDevMm);
        sb.append(", rssi=");
        sb.append(this.mRssi);
        sb.append(", numAttemptedMeasurements=");
        sb.append(this.mNumAttemptedMeasurements);
        sb.append(", numSuccessfulMeasurements=");
        sb.append(this.mNumSuccessfulMeasurements);
        sb.append(", lci=");
        sb.append(this.mLci);
        sb.append(", lcr=");
        sb.append(this.mLcr);
        sb.append(", timestamp=");
        sb.append(this.mTimestamp);
        sb.append("]");
        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RangingResult)) {
            return false;
        }
        RangingResult rangingResult = (RangingResult) obj;
        return this.mStatus == rangingResult.mStatus && Objects.equals(this.mMac, rangingResult.mMac) && Objects.equals(this.mPeerHandle, rangingResult.mPeerHandle) && this.mDistanceMm == rangingResult.mDistanceMm && this.mDistanceStdDevMm == rangingResult.mDistanceStdDevMm && this.mRssi == rangingResult.mRssi && this.mNumAttemptedMeasurements == rangingResult.mNumAttemptedMeasurements && this.mNumSuccessfulMeasurements == rangingResult.mNumSuccessfulMeasurements && Arrays.equals(this.mLci, rangingResult.mLci) && Arrays.equals(this.mLcr, rangingResult.mLcr) && this.mTimestamp == rangingResult.mTimestamp;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mStatus), this.mMac, this.mPeerHandle, Integer.valueOf(this.mDistanceMm), Integer.valueOf(this.mDistanceStdDevMm), Integer.valueOf(this.mRssi), Integer.valueOf(this.mNumAttemptedMeasurements), Integer.valueOf(this.mNumSuccessfulMeasurements), this.mLci, this.mLcr, Long.valueOf(this.mTimestamp));
    }
}
