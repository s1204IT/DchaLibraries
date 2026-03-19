package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class StaBackgroundScanCapabilities {
    public int maxApCachePerScan;
    public int maxBuckets;
    public int maxCacheSize;
    public int maxReportingThreshold;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != StaBackgroundScanCapabilities.class) {
            return false;
        }
        StaBackgroundScanCapabilities staBackgroundScanCapabilities = (StaBackgroundScanCapabilities) obj;
        if (this.maxCacheSize == staBackgroundScanCapabilities.maxCacheSize && this.maxBuckets == staBackgroundScanCapabilities.maxBuckets && this.maxApCachePerScan == staBackgroundScanCapabilities.maxApCachePerScan && this.maxReportingThreshold == staBackgroundScanCapabilities.maxReportingThreshold) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxCacheSize))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxBuckets))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxApCachePerScan))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxReportingThreshold))));
    }

    public final String toString() {
        return "{.maxCacheSize = " + this.maxCacheSize + ", .maxBuckets = " + this.maxBuckets + ", .maxApCachePerScan = " + this.maxApCachePerScan + ", .maxReportingThreshold = " + this.maxReportingThreshold + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(16L), 0L);
    }

    public static final ArrayList<StaBackgroundScanCapabilities> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<StaBackgroundScanCapabilities> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 16, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            StaBackgroundScanCapabilities staBackgroundScanCapabilities = new StaBackgroundScanCapabilities();
            staBackgroundScanCapabilities.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 16);
            arrayList.add(staBackgroundScanCapabilities);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.maxCacheSize = hwBlob.getInt32(0 + j);
        this.maxBuckets = hwBlob.getInt32(4 + j);
        this.maxApCachePerScan = hwBlob.getInt32(8 + j);
        this.maxReportingThreshold = hwBlob.getInt32(j + 12);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(16);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<StaBackgroundScanCapabilities> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 16);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 16);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.maxCacheSize);
        hwBlob.putInt32(4 + j, this.maxBuckets);
        hwBlob.putInt32(8 + j, this.maxApCachePerScan);
        hwBlob.putInt32(j + 12, this.maxReportingThreshold);
    }
}
