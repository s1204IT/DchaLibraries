package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public class WifiChannel implements Parcelable {
    public static final Parcelable.Creator<WifiChannel> CREATOR = new Parcelable.Creator<WifiChannel>() {
        @Override
        public WifiChannel createFromParcel(Parcel in) {
            WifiChannel channel = new WifiChannel();
            channel.freqMHz = in.readInt();
            channel.channelNum = in.readInt();
            channel.isDFS = in.readInt() != 0;
            return channel;
        }

        @Override
        public WifiChannel[] newArray(int size) {
            return new WifiChannel[size];
        }
    };
    private static final int MAX_CHANNEL_NUM = 196;
    private static final int MAX_FREQ_MHZ = 5825;
    private static final int MIN_CHANNEL_NUM = 1;
    private static final int MIN_FREQ_MHZ = 2412;
    public int channelNum;
    public int freqMHz;
    public boolean isDFS;

    public boolean isValid() {
        if (this.freqMHz < MIN_FREQ_MHZ || this.freqMHz > MAX_FREQ_MHZ) {
            return false;
        }
        return this.channelNum >= 1 && this.channelNum <= 196;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.freqMHz);
        out.writeInt(this.channelNum);
        out.writeInt(this.isDFS ? 1 : 0);
    }
}
