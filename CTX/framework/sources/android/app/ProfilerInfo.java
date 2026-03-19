package android.app;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.logging.nano.MetricsProto;
import java.io.IOException;
import java.util.Objects;

public class ProfilerInfo implements Parcelable {
    public static final Parcelable.Creator<ProfilerInfo> CREATOR = new Parcelable.Creator<ProfilerInfo>() {
        @Override
        public ProfilerInfo createFromParcel(Parcel parcel) {
            return new ProfilerInfo(parcel);
        }

        @Override
        public ProfilerInfo[] newArray(int i) {
            return new ProfilerInfo[i];
        }
    };
    private static final String TAG = "ProfilerInfo";
    public final String agent;
    public final boolean attachAgentDuringBind;
    public final boolean autoStopProfiler;
    public ParcelFileDescriptor profileFd;
    public final String profileFile;
    public final int samplingInterval;
    public final boolean streamingOutput;

    public ProfilerInfo(String str, ParcelFileDescriptor parcelFileDescriptor, int i, boolean z, boolean z2, String str2, boolean z3) {
        this.profileFile = str;
        this.profileFd = parcelFileDescriptor;
        this.samplingInterval = i;
        this.autoStopProfiler = z;
        this.streamingOutput = z2;
        this.agent = str2;
        this.attachAgentDuringBind = z3;
    }

    public ProfilerInfo(ProfilerInfo profilerInfo) {
        this.profileFile = profilerInfo.profileFile;
        this.profileFd = profilerInfo.profileFd;
        this.samplingInterval = profilerInfo.samplingInterval;
        this.autoStopProfiler = profilerInfo.autoStopProfiler;
        this.streamingOutput = profilerInfo.streamingOutput;
        this.agent = profilerInfo.agent;
        this.attachAgentDuringBind = profilerInfo.attachAgentDuringBind;
    }

    public ProfilerInfo setAgent(String str, boolean z) {
        return new ProfilerInfo(this.profileFile, this.profileFd, this.samplingInterval, this.autoStopProfiler, this.streamingOutput, str, z);
    }

    public void closeFd() {
        if (this.profileFd != null) {
            try {
                this.profileFd.close();
            } catch (IOException e) {
                Slog.w(TAG, "Failure closing profile fd", e);
            }
            this.profileFd = null;
        }
    }

    @Override
    public int describeContents() {
        if (this.profileFd != null) {
            return this.profileFd.describeContents();
        }
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.profileFile);
        if (this.profileFd != null) {
            parcel.writeInt(1);
            this.profileFd.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.samplingInterval);
        parcel.writeInt(this.autoStopProfiler ? 1 : 0);
        parcel.writeInt(this.streamingOutput ? 1 : 0);
        parcel.writeString(this.agent);
        parcel.writeBoolean(this.attachAgentDuringBind);
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1138166333441L, this.profileFile);
        if (this.profileFd != null) {
            protoOutputStream.write(1120986464258L, this.profileFd.getFd());
        }
        protoOutputStream.write(1120986464259L, this.samplingInterval);
        protoOutputStream.write(1133871366148L, this.autoStopProfiler);
        protoOutputStream.write(1133871366149L, this.streamingOutput);
        protoOutputStream.write(1138166333446L, this.agent);
        protoOutputStream.end(jStart);
    }

    private ProfilerInfo(Parcel parcel) {
        this.profileFile = parcel.readString();
        this.profileFd = parcel.readInt() != 0 ? ParcelFileDescriptor.CREATOR.createFromParcel(parcel) : null;
        this.samplingInterval = parcel.readInt();
        this.autoStopProfiler = parcel.readInt() != 0;
        this.streamingOutput = parcel.readInt() != 0;
        this.agent = parcel.readString();
        this.attachAgentDuringBind = parcel.readBoolean();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ProfilerInfo profilerInfo = (ProfilerInfo) obj;
        if (Objects.equals(this.profileFile, profilerInfo.profileFile) && this.autoStopProfiler == profilerInfo.autoStopProfiler && this.samplingInterval == profilerInfo.samplingInterval && this.streamingOutput == profilerInfo.streamingOutput && Objects.equals(this.agent, profilerInfo.agent)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (((((((MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + Objects.hashCode(this.profileFile)) * 31) + this.samplingInterval) * 31) + (this.autoStopProfiler ? 1 : 0)) * 31) + (this.streamingOutput ? 1 : 0))) + Objects.hashCode(this.agent);
    }
}
