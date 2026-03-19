package android.net.lowpan;

public class LowpanEnergyScanResult {
    public static final int UNKNOWN = Integer.MAX_VALUE;
    private int mChannel = Integer.MAX_VALUE;
    private int mMaxRssi = Integer.MAX_VALUE;

    LowpanEnergyScanResult() {
    }

    public int getChannel() {
        return this.mChannel;
    }

    public int getMaxRssi() {
        return this.mMaxRssi;
    }

    void setChannel(int i) {
        this.mChannel = i;
    }

    void setMaxRssi(int i) {
        this.mMaxRssi = i;
    }

    public String toString() {
        return "LowpanEnergyScanResult(channel: " + this.mChannel + ", maxRssi:" + this.mMaxRssi + ")";
    }
}
