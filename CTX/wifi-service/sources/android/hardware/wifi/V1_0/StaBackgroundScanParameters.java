package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class StaBackgroundScanParameters {
    public int basePeriodInMs;
    public final ArrayList<StaBackgroundScanBucketParameters> buckets = new ArrayList<>();
    public int maxApPerScan;
    public int reportThresholdNumScans;
    public int reportThresholdPercent;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != StaBackgroundScanParameters.class) {
            return false;
        }
        StaBackgroundScanParameters staBackgroundScanParameters = (StaBackgroundScanParameters) obj;
        if (this.basePeriodInMs == staBackgroundScanParameters.basePeriodInMs && this.maxApPerScan == staBackgroundScanParameters.maxApPerScan && this.reportThresholdPercent == staBackgroundScanParameters.reportThresholdPercent && this.reportThresholdNumScans == staBackgroundScanParameters.reportThresholdNumScans && HidlSupport.deepEquals(this.buckets, staBackgroundScanParameters.buckets)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.basePeriodInMs))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxApPerScan))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.reportThresholdPercent))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.reportThresholdNumScans))), Integer.valueOf(HidlSupport.deepHashCode(this.buckets)));
    }

    public final String toString() {
        return "{.basePeriodInMs = " + this.basePeriodInMs + ", .maxApPerScan = " + this.maxApPerScan + ", .reportThresholdPercent = " + this.reportThresholdPercent + ", .reportThresholdNumScans = " + this.reportThresholdNumScans + ", .buckets = " + this.buckets + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(32L), 0L);
    }

    public static final ArrayList<StaBackgroundScanParameters> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<StaBackgroundScanParameters> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            StaBackgroundScanParameters staBackgroundScanParameters = new StaBackgroundScanParameters();
            staBackgroundScanParameters.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
            arrayList.add(staBackgroundScanParameters);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.basePeriodInMs = hwBlob.getInt32(j + 0);
        this.maxApPerScan = hwBlob.getInt32(j + 4);
        this.reportThresholdPercent = hwBlob.getInt32(j + 8);
        this.reportThresholdNumScans = hwBlob.getInt32(j + 12);
        long j2 = j + 16;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 48, hwBlob.handle(), j2 + 0, true);
        this.buckets.clear();
        for (int i = 0; i < int32; i++) {
            StaBackgroundScanBucketParameters staBackgroundScanBucketParameters = new StaBackgroundScanBucketParameters();
            staBackgroundScanBucketParameters.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 48);
            this.buckets.add(staBackgroundScanBucketParameters);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(32);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<StaBackgroundScanParameters> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 32);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 32);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.basePeriodInMs);
        hwBlob.putInt32(4 + j, this.maxApPerScan);
        hwBlob.putInt32(j + 8, this.reportThresholdPercent);
        hwBlob.putInt32(j + 12, this.reportThresholdNumScans);
        int size = this.buckets.size();
        long j2 = j + 16;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 48);
        for (int i = 0; i < size; i++) {
            this.buckets.get(i).writeEmbeddedToBlob(hwBlob2, i * 48);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
