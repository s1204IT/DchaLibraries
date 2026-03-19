package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public class PPPOEInfo implements Parcelable {
    public static final Parcelable.Creator<PPPOEInfo> CREATOR = new Parcelable.Creator<PPPOEInfo>() {
        @Override
        public PPPOEInfo createFromParcel(Parcel in) {
            PPPOEInfo result = new PPPOEInfo();
            result.status = Status.valueOf(in.readString());
            result.online_time = in.readLong();
            return result;
        }

        @Override
        public PPPOEInfo[] newArray(int size) {
            return new PPPOEInfo[size];
        }
    };
    public long online_time;
    public Status status;

    public enum Status {
        OFFLINE,
        CONNECTING,
        ONLINE;

        public static Status[] valuesCustom() {
            return values();
        }
    }

    public PPPOEInfo() {
        this.status = Status.OFFLINE;
    }

    public PPPOEInfo(Status s) {
        this.status = s;
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(" status: ").append(this.status.toString());
        sbuf.append('\n');
        sbuf.append(" online_time: ").append(this.online_time);
        sbuf.append("\n");
        return sbuf.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public PPPOEInfo(PPPOEInfo source) {
        if (source == null) {
            return;
        }
        this.status = source.status;
        this.online_time = source.online_time;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.status.name());
        dest.writeLong(this.online_time);
    }
}
