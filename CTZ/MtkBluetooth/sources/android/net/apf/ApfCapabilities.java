package android.net.apf;

public class ApfCapabilities {
    public final int apfPacketFormat;
    public final int apfVersionSupported;
    public final int maximumApfProgramSize;

    public ApfCapabilities(int i, int i2, int i3) {
        this.apfVersionSupported = i;
        this.maximumApfProgramSize = i2;
        this.apfPacketFormat = i3;
    }

    public String toString() {
        return String.format("%s{version: %d, maxSize: %d, format: %d}", getClass().getSimpleName(), Integer.valueOf(this.apfVersionSupported), Integer.valueOf(this.maximumApfProgramSize), Integer.valueOf(this.apfPacketFormat));
    }

    public boolean hasDataAccess() {
        return this.apfVersionSupported >= 4;
    }
}
