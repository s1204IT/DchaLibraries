package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public final class IpSecTunnelInterfaceResponse implements Parcelable {
    public static final Parcelable.Creator<IpSecTunnelInterfaceResponse> CREATOR = new Parcelable.Creator<IpSecTunnelInterfaceResponse>() {
        @Override
        public IpSecTunnelInterfaceResponse createFromParcel(Parcel parcel) {
            return new IpSecTunnelInterfaceResponse(parcel);
        }

        @Override
        public IpSecTunnelInterfaceResponse[] newArray(int i) {
            return new IpSecTunnelInterfaceResponse[i];
        }
    };
    private static final String TAG = "IpSecTunnelInterfaceResponse";
    public final String interfaceName;
    public final int resourceId;
    public final int status;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.status);
        parcel.writeInt(this.resourceId);
        parcel.writeString(this.interfaceName);
    }

    public IpSecTunnelInterfaceResponse(int i) {
        if (i == 0) {
            throw new IllegalArgumentException("Valid status implies other args must be provided");
        }
        this.status = i;
        this.resourceId = -1;
        this.interfaceName = "";
    }

    public IpSecTunnelInterfaceResponse(int i, int i2, String str) {
        this.status = i;
        this.resourceId = i2;
        this.interfaceName = str;
    }

    private IpSecTunnelInterfaceResponse(Parcel parcel) {
        this.status = parcel.readInt();
        this.resourceId = parcel.readInt();
        this.interfaceName = parcel.readString();
    }
}
