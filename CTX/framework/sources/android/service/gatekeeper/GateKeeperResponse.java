package android.service.gatekeeper;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.annotations.VisibleForTesting;

public final class GateKeeperResponse implements Parcelable {
    public static final Parcelable.Creator<GateKeeperResponse> CREATOR = new Parcelable.Creator<GateKeeperResponse>() {
        @Override
        public GateKeeperResponse createFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            if (i == 1) {
                return GateKeeperResponse.createRetryResponse(parcel.readInt());
            }
            if (i == 0) {
                boolean z = parcel.readInt() == 1;
                byte[] bArr = null;
                int i2 = parcel.readInt();
                if (i2 > 0) {
                    bArr = new byte[i2];
                    parcel.readByteArray(bArr);
                }
                return GateKeeperResponse.createOkResponse(bArr, z);
            }
            return GateKeeperResponse.createGenericResponse(i);
        }

        @Override
        public GateKeeperResponse[] newArray(int i) {
            return new GateKeeperResponse[i];
        }
    };
    public static final int RESPONSE_ERROR = -1;
    public static final int RESPONSE_OK = 0;
    public static final int RESPONSE_RETRY = 1;
    private byte[] mPayload;
    private final int mResponseCode;
    private boolean mShouldReEnroll;
    private int mTimeout;

    private GateKeeperResponse(int i) {
        this.mResponseCode = i;
    }

    @VisibleForTesting
    public static GateKeeperResponse createGenericResponse(int i) {
        return new GateKeeperResponse(i);
    }

    private static GateKeeperResponse createRetryResponse(int i) {
        GateKeeperResponse gateKeeperResponse = new GateKeeperResponse(1);
        gateKeeperResponse.mTimeout = i;
        return gateKeeperResponse;
    }

    @VisibleForTesting
    public static GateKeeperResponse createOkResponse(byte[] bArr, boolean z) {
        GateKeeperResponse gateKeeperResponse = new GateKeeperResponse(0);
        gateKeeperResponse.mPayload = bArr;
        gateKeeperResponse.mShouldReEnroll = z;
        return gateKeeperResponse;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mResponseCode);
        if (this.mResponseCode == 1) {
            parcel.writeInt(this.mTimeout);
            return;
        }
        if (this.mResponseCode == 0) {
            parcel.writeInt(this.mShouldReEnroll ? 1 : 0);
            if (this.mPayload != null) {
                parcel.writeInt(this.mPayload.length);
                parcel.writeByteArray(this.mPayload);
            } else {
                parcel.writeInt(0);
            }
        }
    }

    public byte[] getPayload() {
        return this.mPayload;
    }

    public int getTimeout() {
        return this.mTimeout;
    }

    public boolean getShouldReEnroll() {
        return this.mShouldReEnroll;
    }

    public int getResponseCode() {
        return this.mResponseCode;
    }
}
