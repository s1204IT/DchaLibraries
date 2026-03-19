package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class DataConnectionRealTimeInfo implements Parcelable {
    public static final Parcelable.Creator<DataConnectionRealTimeInfo> CREATOR = new Parcelable.Creator<DataConnectionRealTimeInfo>() {
        @Override
        public DataConnectionRealTimeInfo createFromParcel(Parcel parcel) {
            return new DataConnectionRealTimeInfo(parcel);
        }

        @Override
        public DataConnectionRealTimeInfo[] newArray(int i) {
            return new DataConnectionRealTimeInfo[i];
        }
    };
    public static final int DC_POWER_STATE_HIGH = 3;
    public static final int DC_POWER_STATE_LOW = 1;
    public static final int DC_POWER_STATE_MEDIUM = 2;
    public static final int DC_POWER_STATE_UNKNOWN = Integer.MAX_VALUE;
    private int mDcPowerState;
    private long mTime;

    public DataConnectionRealTimeInfo(long j, int i) {
        this.mTime = j;
        this.mDcPowerState = i;
    }

    public DataConnectionRealTimeInfo() {
        this.mTime = Long.MAX_VALUE;
        this.mDcPowerState = Integer.MAX_VALUE;
    }

    private DataConnectionRealTimeInfo(Parcel parcel) {
        this.mTime = parcel.readLong();
        this.mDcPowerState = parcel.readInt();
    }

    public long getTime() {
        return this.mTime;
    }

    public int getDcPowerState() {
        return this.mDcPowerState;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mTime);
        parcel.writeInt(this.mDcPowerState);
    }

    public int hashCode() {
        long j = this.mTime + 17;
        return (int) (j + (17 * j) + ((long) this.mDcPowerState));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DataConnectionRealTimeInfo dataConnectionRealTimeInfo = (DataConnectionRealTimeInfo) obj;
        if (this.mTime == dataConnectionRealTimeInfo.mTime && this.mDcPowerState == dataConnectionRealTimeInfo.mDcPowerState) {
            return true;
        }
        return false;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("mTime=");
        stringBuffer.append(this.mTime);
        stringBuffer.append(" mDcPowerState=");
        stringBuffer.append(this.mDcPowerState);
        return stringBuffer.toString();
    }
}
