package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class IccOpenLogicalChannelResponse implements Parcelable {
    public static final Parcelable.Creator<IccOpenLogicalChannelResponse> CREATOR = new Parcelable.Creator<IccOpenLogicalChannelResponse>() {
        @Override
        public IccOpenLogicalChannelResponse createFromParcel(Parcel parcel) {
            return new IccOpenLogicalChannelResponse(parcel);
        }

        @Override
        public IccOpenLogicalChannelResponse[] newArray(int i) {
            return new IccOpenLogicalChannelResponse[i];
        }
    };
    public static final int INVALID_CHANNEL = -1;
    public static final int STATUS_MISSING_RESOURCE = 2;
    public static final int STATUS_NO_ERROR = 1;
    public static final int STATUS_NO_SUCH_ELEMENT = 3;
    public static final int STATUS_UNKNOWN_ERROR = 4;
    private final int mChannel;
    private final byte[] mSelectResponse;
    private final int mStatus;

    public IccOpenLogicalChannelResponse(int i, int i2, byte[] bArr) {
        this.mChannel = i;
        this.mStatus = i2;
        this.mSelectResponse = bArr;
    }

    private IccOpenLogicalChannelResponse(Parcel parcel) {
        this.mChannel = parcel.readInt();
        this.mStatus = parcel.readInt();
        int i = parcel.readInt();
        if (i > 0) {
            this.mSelectResponse = new byte[i];
            parcel.readByteArray(this.mSelectResponse);
        } else {
            this.mSelectResponse = null;
        }
    }

    public int getChannel() {
        return this.mChannel;
    }

    public int getStatus() {
        return this.mStatus;
    }

    public byte[] getSelectResponse() {
        return this.mSelectResponse;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mChannel);
        parcel.writeInt(this.mStatus);
        if (this.mSelectResponse != null && this.mSelectResponse.length > 0) {
            parcel.writeInt(this.mSelectResponse.length);
            parcel.writeByteArray(this.mSelectResponse);
        } else {
            parcel.writeInt(0);
        }
    }

    public String toString() {
        return "Channel: " + this.mChannel + " Status: " + this.mStatus;
    }
}
