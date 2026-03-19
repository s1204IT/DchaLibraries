package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public class RssiPacketCountInfo implements Parcelable {
    public static final Parcelable.Creator<RssiPacketCountInfo> CREATOR = new Parcelable.Creator<RssiPacketCountInfo>() {
        @Override
        public RssiPacketCountInfo createFromParcel(Parcel in) {
            return new RssiPacketCountInfo(in, null);
        }

        @Override
        public RssiPacketCountInfo[] newArray(int size) {
            return new RssiPacketCountInfo[size];
        }
    };
    public int mLinkspeed;
    public long rACKFailureCount;
    public long rFCSErrorCount;
    public long rFailedCount;
    public long rMultipleRetryCount;
    public long rRetryCount;
    public int rssi;
    public int rxgood;
    public int txbad;
    public int txgood;

    RssiPacketCountInfo(Parcel in, RssiPacketCountInfo rssiPacketCountInfo) {
        this(in);
    }

    public RssiPacketCountInfo() {
        this.rxgood = 0;
        this.txbad = 0;
        this.txgood = 0;
        this.rssi = 0;
        this.mLinkspeed = 0;
        this.rFCSErrorCount = 0L;
        this.rACKFailureCount = 0L;
        this.rMultipleRetryCount = 0L;
        this.rRetryCount = 0L;
        this.rFailedCount = 0L;
    }

    private RssiPacketCountInfo(Parcel in) {
        this.rssi = in.readInt();
        this.txgood = in.readInt();
        this.txbad = in.readInt();
        this.rxgood = in.readInt();
        this.rFailedCount = in.readLong();
        this.rRetryCount = in.readLong();
        this.rMultipleRetryCount = in.readLong();
        this.rACKFailureCount = in.readLong();
        this.rFCSErrorCount = in.readLong();
        this.mLinkspeed = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.rssi);
        out.writeInt(this.txgood);
        out.writeInt(this.txbad);
        out.writeInt(this.rxgood);
        out.writeLong(this.rFailedCount);
        out.writeLong(this.rRetryCount);
        out.writeLong(this.rMultipleRetryCount);
        out.writeLong(this.rACKFailureCount);
        out.writeLong(this.rFCSErrorCount);
        out.writeInt(this.mLinkspeed);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
