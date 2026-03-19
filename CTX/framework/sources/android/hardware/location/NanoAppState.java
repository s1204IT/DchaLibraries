package android.hardware.location;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

@SystemApi
public final class NanoAppState implements Parcelable {
    public static final Parcelable.Creator<NanoAppState> CREATOR = new Parcelable.Creator<NanoAppState>() {
        @Override
        public NanoAppState createFromParcel(Parcel parcel) {
            return new NanoAppState(parcel);
        }

        @Override
        public NanoAppState[] newArray(int i) {
            return new NanoAppState[i];
        }
    };
    private boolean mIsEnabled;
    private long mNanoAppId;
    private int mNanoAppVersion;

    public NanoAppState(long j, int i, boolean z) {
        this.mNanoAppId = j;
        this.mNanoAppVersion = i;
        this.mIsEnabled = z;
    }

    public long getNanoAppId() {
        return this.mNanoAppId;
    }

    public long getNanoAppVersion() {
        return this.mNanoAppVersion;
    }

    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    private NanoAppState(Parcel parcel) {
        this.mNanoAppId = parcel.readLong();
        this.mNanoAppVersion = parcel.readInt();
        this.mIsEnabled = parcel.readInt() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mNanoAppId);
        parcel.writeInt(this.mNanoAppVersion);
        parcel.writeInt(this.mIsEnabled ? 1 : 0);
    }
}
