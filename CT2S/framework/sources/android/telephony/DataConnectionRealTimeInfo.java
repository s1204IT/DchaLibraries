package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class DataConnectionRealTimeInfo implements Parcelable {
    private int mDcPowerState;
    private long mTime;
    public static int DC_POWER_STATE_LOW = 1;
    public static int DC_POWER_STATE_MEDIUM = 2;
    public static int DC_POWER_STATE_HIGH = 3;
    public static int DC_POWER_STATE_UNKNOWN = Integer.MAX_VALUE;
    public static final Parcelable.Creator<DataConnectionRealTimeInfo> CREATOR = new Parcelable.Creator<DataConnectionRealTimeInfo>() {
        @Override
        public DataConnectionRealTimeInfo createFromParcel(Parcel in) {
            return new DataConnectionRealTimeInfo(in);
        }

        @Override
        public DataConnectionRealTimeInfo[] newArray(int size) {
            return new DataConnectionRealTimeInfo[size];
        }
    };

    public DataConnectionRealTimeInfo(long time, int dcPowerState) {
        this.mTime = time;
        this.mDcPowerState = dcPowerState;
    }

    public DataConnectionRealTimeInfo() {
        this.mTime = Long.MAX_VALUE;
        this.mDcPowerState = DC_POWER_STATE_UNKNOWN;
    }

    private DataConnectionRealTimeInfo(Parcel in) {
        this.mTime = in.readLong();
        this.mDcPowerState = in.readInt();
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
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(this.mTime);
        out.writeInt(this.mDcPowerState);
    }

    public int hashCode() {
        long result = (17 * 1) + this.mTime;
        return (int) (result + (17 * result) + ((long) this.mDcPowerState));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            DataConnectionRealTimeInfo other = (DataConnectionRealTimeInfo) obj;
            return this.mTime == other.mTime && this.mDcPowerState == other.mDcPowerState;
        }
        return false;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("mTime=").append(this.mTime);
        sb.append(" mDcPowerState=").append(this.mDcPowerState);
        return sb.toString();
    }
}
