package android.hardware.location;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import libcore.util.EmptyArray;

@SystemApi
@Deprecated
public class NanoAppInstanceInfo implements Parcelable {
    public static final Parcelable.Creator<NanoAppInstanceInfo> CREATOR = new Parcelable.Creator<NanoAppInstanceInfo>() {
        @Override
        public NanoAppInstanceInfo createFromParcel(Parcel parcel) {
            return new NanoAppInstanceInfo(parcel);
        }

        @Override
        public NanoAppInstanceInfo[] newArray(int i) {
            return new NanoAppInstanceInfo[i];
        }
    };
    private long mAppId;
    private int mAppVersion;
    private int mContexthubId;
    private int mHandle;
    private String mName;
    private int mNeededExecMemBytes;
    private int mNeededReadMemBytes;
    private int[] mNeededSensors;
    private int mNeededWriteMemBytes;
    private int[] mOutputEvents;
    private String mPublisher;

    public NanoAppInstanceInfo() {
        this.mPublisher = "Unknown";
        this.mName = "Unknown";
        this.mNeededReadMemBytes = 0;
        this.mNeededWriteMemBytes = 0;
        this.mNeededExecMemBytes = 0;
        this.mNeededSensors = EmptyArray.INT;
        this.mOutputEvents = EmptyArray.INT;
    }

    public NanoAppInstanceInfo(int i, long j, int i2, int i3) {
        this.mPublisher = "Unknown";
        this.mName = "Unknown";
        this.mNeededReadMemBytes = 0;
        this.mNeededWriteMemBytes = 0;
        this.mNeededExecMemBytes = 0;
        this.mNeededSensors = EmptyArray.INT;
        this.mOutputEvents = EmptyArray.INT;
        this.mHandle = i;
        this.mAppId = j;
        this.mAppVersion = i2;
        this.mContexthubId = i3;
    }

    public String getPublisher() {
        return this.mPublisher;
    }

    public String getName() {
        return this.mName;
    }

    public long getAppId() {
        return this.mAppId;
    }

    public int getAppVersion() {
        return this.mAppVersion;
    }

    public int getNeededReadMemBytes() {
        return this.mNeededReadMemBytes;
    }

    public int getNeededWriteMemBytes() {
        return this.mNeededWriteMemBytes;
    }

    public int getNeededExecMemBytes() {
        return this.mNeededExecMemBytes;
    }

    public int[] getNeededSensors() {
        return this.mNeededSensors;
    }

    public int[] getOutputEvents() {
        return this.mOutputEvents;
    }

    public int getContexthubId() {
        return this.mContexthubId;
    }

    public int getHandle() {
        return this.mHandle;
    }

    private NanoAppInstanceInfo(Parcel parcel) {
        this.mPublisher = "Unknown";
        this.mName = "Unknown";
        this.mNeededReadMemBytes = 0;
        this.mNeededWriteMemBytes = 0;
        this.mNeededExecMemBytes = 0;
        this.mNeededSensors = EmptyArray.INT;
        this.mOutputEvents = EmptyArray.INT;
        this.mPublisher = parcel.readString();
        this.mName = parcel.readString();
        this.mHandle = parcel.readInt();
        this.mAppId = parcel.readLong();
        this.mAppVersion = parcel.readInt();
        this.mContexthubId = parcel.readInt();
        this.mNeededReadMemBytes = parcel.readInt();
        this.mNeededWriteMemBytes = parcel.readInt();
        this.mNeededExecMemBytes = parcel.readInt();
        this.mNeededSensors = new int[parcel.readInt()];
        parcel.readIntArray(this.mNeededSensors);
        this.mOutputEvents = new int[parcel.readInt()];
        parcel.readIntArray(this.mOutputEvents);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mPublisher);
        parcel.writeString(this.mName);
        parcel.writeInt(this.mHandle);
        parcel.writeLong(this.mAppId);
        parcel.writeInt(this.mAppVersion);
        parcel.writeInt(this.mContexthubId);
        parcel.writeInt(this.mNeededReadMemBytes);
        parcel.writeInt(this.mNeededWriteMemBytes);
        parcel.writeInt(this.mNeededExecMemBytes);
        parcel.writeInt(this.mNeededSensors.length);
        parcel.writeIntArray(this.mNeededSensors);
        parcel.writeInt(this.mOutputEvents.length);
        parcel.writeIntArray(this.mOutputEvents);
    }

    public String toString() {
        return (("handle : " + this.mHandle) + ", Id : 0x" + Long.toHexString(this.mAppId)) + ", Version : 0x" + Integer.toHexString(this.mAppVersion);
    }
}
