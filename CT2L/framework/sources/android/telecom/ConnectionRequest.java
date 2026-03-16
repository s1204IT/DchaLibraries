package android.telecom;

import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public final class ConnectionRequest implements Parcelable {
    public static final Parcelable.Creator<ConnectionRequest> CREATOR = new Parcelable.Creator<ConnectionRequest>() {
        @Override
        public ConnectionRequest createFromParcel(Parcel source) {
            return new ConnectionRequest(source);
        }

        @Override
        public ConnectionRequest[] newArray(int size) {
            return new ConnectionRequest[size];
        }
    };
    private final PhoneAccountHandle mAccountHandle;
    private final Uri mAddress;
    private final Bundle mExtras;
    private final int mVideoState;

    public ConnectionRequest(PhoneAccountHandle accountHandle, Uri handle, Bundle extras) {
        this(accountHandle, handle, extras, 0);
    }

    public ConnectionRequest(PhoneAccountHandle accountHandle, Uri handle, Bundle extras, int videoState) {
        this.mAccountHandle = accountHandle;
        this.mAddress = handle;
        this.mExtras = extras;
        this.mVideoState = videoState;
    }

    private ConnectionRequest(Parcel in) {
        this.mAccountHandle = (PhoneAccountHandle) in.readParcelable(getClass().getClassLoader());
        this.mAddress = (Uri) in.readParcelable(getClass().getClassLoader());
        this.mExtras = (Bundle) in.readParcelable(getClass().getClassLoader());
        this.mVideoState = in.readInt();
    }

    public PhoneAccountHandle getAccountHandle() {
        return this.mAccountHandle;
    }

    public Uri getAddress() {
        return this.mAddress;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    public int getVideoState() {
        return this.mVideoState;
    }

    public String toString() {
        Object[] objArr = new Object[2];
        objArr[0] = this.mAddress == null ? Uri.EMPTY : Connection.toLogSafePhoneNumber(this.mAddress.toString());
        objArr[1] = this.mExtras == null ? ProxyInfo.LOCAL_EXCL_LIST : this.mExtras;
        return String.format("ConnectionRequest %s %s", objArr);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel destination, int flags) {
        destination.writeParcelable(this.mAccountHandle, 0);
        destination.writeParcelable(this.mAddress, 0);
        destination.writeParcelable(this.mExtras, 0);
        destination.writeInt(this.mVideoState);
    }
}
