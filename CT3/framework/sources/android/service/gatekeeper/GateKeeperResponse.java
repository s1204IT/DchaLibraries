package android.service.gatekeeper;

import android.os.Parcel;
import android.os.Parcelable;

public final class GateKeeperResponse implements Parcelable {
    public static final Parcelable.Creator<GateKeeperResponse> CREATOR = new Parcelable.Creator<GateKeeperResponse>() {
        @Override
        public GateKeeperResponse createFromParcel(Parcel source) {
            int responseCode = source.readInt();
            GateKeeperResponse response = new GateKeeperResponse(responseCode, (GateKeeperResponse) null);
            if (responseCode == 1) {
                response.setTimeout(source.readInt());
            } else if (responseCode == 0) {
                response.setShouldReEnroll(source.readInt() == 1);
                int size = source.readInt();
                if (size > 0) {
                    byte[] payload = new byte[size];
                    source.readByteArray(payload);
                    response.setPayload(payload);
                }
            }
            return response;
        }

        @Override
        public GateKeeperResponse[] newArray(int size) {
            return new GateKeeperResponse[size];
        }
    };
    public static final int RESPONSE_ERROR = -1;
    public static final int RESPONSE_OK = 0;
    public static final int RESPONSE_RETRY = 1;
    private byte[] mPayload;
    private final int mResponseCode;
    private boolean mShouldReEnroll;
    private int mTimeout;

    GateKeeperResponse(int responseCode, GateKeeperResponse gateKeeperResponse) {
        this(responseCode);
    }

    private GateKeeperResponse(int responseCode) {
        this.mResponseCode = responseCode;
    }

    private GateKeeperResponse(int responseCode, int timeout) {
        this.mResponseCode = responseCode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mResponseCode);
        if (this.mResponseCode == 1) {
            dest.writeInt(this.mTimeout);
            return;
        }
        if (this.mResponseCode != 0) {
            return;
        }
        dest.writeInt(this.mShouldReEnroll ? 1 : 0);
        if (this.mPayload != null) {
            dest.writeInt(this.mPayload.length);
            dest.writeByteArray(this.mPayload);
        } else {
            dest.writeInt(0);
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

    private void setTimeout(int timeout) {
        this.mTimeout = timeout;
    }

    private void setShouldReEnroll(boolean shouldReEnroll) {
        this.mShouldReEnroll = shouldReEnroll;
    }

    private void setPayload(byte[] payload) {
        this.mPayload = payload;
    }
}
