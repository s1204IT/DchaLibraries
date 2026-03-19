package android.app;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

public class ProfilerInfo implements Parcelable {
    public static final Parcelable.Creator<ProfilerInfo> CREATOR = new Parcelable.Creator<ProfilerInfo>() {
        @Override
        public ProfilerInfo createFromParcel(Parcel in) {
            return new ProfilerInfo(in, null);
        }

        @Override
        public ProfilerInfo[] newArray(int size) {
            return new ProfilerInfo[size];
        }
    };
    public final boolean autoStopProfiler;
    public ParcelFileDescriptor profileFd;
    public final String profileFile;
    public final int samplingInterval;

    ProfilerInfo(Parcel in, ProfilerInfo profilerInfo) {
        this(in);
    }

    public ProfilerInfo(String filename, ParcelFileDescriptor fd, int interval, boolean autoStop) {
        this.profileFile = filename;
        this.profileFd = fd;
        this.samplingInterval = interval;
        this.autoStopProfiler = autoStop;
    }

    @Override
    public int describeContents() {
        if (this.profileFd != null) {
            return this.profileFd.describeContents();
        }
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.profileFile);
        if (this.profileFd != null) {
            out.writeInt(1);
            this.profileFd.writeToParcel(out, flags);
        } else {
            out.writeInt(0);
        }
        out.writeInt(this.samplingInterval);
        out.writeInt(this.autoStopProfiler ? 1 : 0);
    }

    private ProfilerInfo(Parcel in) {
        this.profileFile = in.readString();
        this.profileFd = in.readInt() != 0 ? ParcelFileDescriptor.CREATOR.createFromParcel(in) : null;
        this.samplingInterval = in.readInt();
        this.autoStopProfiler = in.readInt() != 0;
    }
}
