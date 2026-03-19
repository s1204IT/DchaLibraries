package android.net;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import java.io.FileDescriptor;
import java.io.IOException;

public final class IpSecUdpEncapResponse implements Parcelable {
    public static final Parcelable.Creator<IpSecUdpEncapResponse> CREATOR = new Parcelable.Creator<IpSecUdpEncapResponse>() {
        @Override
        public IpSecUdpEncapResponse createFromParcel(Parcel parcel) {
            return new IpSecUdpEncapResponse(parcel);
        }

        @Override
        public IpSecUdpEncapResponse[] newArray(int i) {
            return new IpSecUdpEncapResponse[i];
        }
    };
    private static final String TAG = "IpSecUdpEncapResponse";
    public final ParcelFileDescriptor fileDescriptor;
    public final int port;
    public final int resourceId;
    public final int status;

    @Override
    public int describeContents() {
        return this.fileDescriptor != null ? 1 : 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.status);
        parcel.writeInt(this.resourceId);
        parcel.writeInt(this.port);
        parcel.writeParcelable(this.fileDescriptor, 1);
    }

    public IpSecUdpEncapResponse(int i) {
        if (i == 0) {
            throw new IllegalArgumentException("Valid status implies other args must be provided");
        }
        this.status = i;
        this.resourceId = -1;
        this.port = -1;
        this.fileDescriptor = null;
    }

    public IpSecUdpEncapResponse(int i, int i2, int i3, FileDescriptor fileDescriptor) throws IOException {
        if (i == 0 && fileDescriptor == null) {
            throw new IllegalArgumentException("Valid status implies FD must be non-null");
        }
        this.status = i;
        this.resourceId = i2;
        this.port = i3;
        this.fileDescriptor = this.status == 0 ? ParcelFileDescriptor.dup(fileDescriptor) : null;
    }

    private IpSecUdpEncapResponse(Parcel parcel) {
        this.status = parcel.readInt();
        this.resourceId = parcel.readInt();
        this.port = parcel.readInt();
        this.fileDescriptor = (ParcelFileDescriptor) parcel.readParcelable(ParcelFileDescriptor.class.getClassLoader());
    }
}
