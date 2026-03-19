package android.hardware.wifi.V1_2;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class NanConfigRequestSupplemental {
    public int discoveryBeaconIntervalMs;
    public boolean enableDiscoveryWindowEarlyTermination;
    public boolean enableRanging;
    public int numberOfSpatialStreamsInDiscovery;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanConfigRequestSupplemental.class) {
            return false;
        }
        NanConfigRequestSupplemental nanConfigRequestSupplemental = (NanConfigRequestSupplemental) obj;
        if (this.discoveryBeaconIntervalMs == nanConfigRequestSupplemental.discoveryBeaconIntervalMs && this.numberOfSpatialStreamsInDiscovery == nanConfigRequestSupplemental.numberOfSpatialStreamsInDiscovery && this.enableDiscoveryWindowEarlyTermination == nanConfigRequestSupplemental.enableDiscoveryWindowEarlyTermination && this.enableRanging == nanConfigRequestSupplemental.enableRanging) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.discoveryBeaconIntervalMs))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.numberOfSpatialStreamsInDiscovery))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.enableDiscoveryWindowEarlyTermination))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.enableRanging))));
    }

    public final String toString() {
        return "{.discoveryBeaconIntervalMs = " + this.discoveryBeaconIntervalMs + ", .numberOfSpatialStreamsInDiscovery = " + this.numberOfSpatialStreamsInDiscovery + ", .enableDiscoveryWindowEarlyTermination = " + this.enableDiscoveryWindowEarlyTermination + ", .enableRanging = " + this.enableRanging + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(12L), 0L);
    }

    public static final ArrayList<NanConfigRequestSupplemental> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanConfigRequestSupplemental> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 12, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanConfigRequestSupplemental nanConfigRequestSupplemental = new NanConfigRequestSupplemental();
            nanConfigRequestSupplemental.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 12);
            arrayList.add(nanConfigRequestSupplemental);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.discoveryBeaconIntervalMs = hwBlob.getInt32(0 + j);
        this.numberOfSpatialStreamsInDiscovery = hwBlob.getInt32(4 + j);
        this.enableDiscoveryWindowEarlyTermination = hwBlob.getBool(8 + j);
        this.enableRanging = hwBlob.getBool(j + 9);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(12);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanConfigRequestSupplemental> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 12);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 12);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.discoveryBeaconIntervalMs);
        hwBlob.putInt32(4 + j, this.numberOfSpatialStreamsInDiscovery);
        hwBlob.putBool(8 + j, this.enableDiscoveryWindowEarlyTermination);
        hwBlob.putBool(j + 9, this.enableRanging);
    }
}
