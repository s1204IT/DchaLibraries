package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class StaBackgroundScanBucketParameters {
    public int band;
    public int bucketIdx;
    public int eventReportScheme;
    public int exponentialBase;
    public int exponentialMaxPeriodInMs;
    public int exponentialStepCount;
    public final ArrayList<Integer> frequencies = new ArrayList<>();
    public int periodInMs;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != StaBackgroundScanBucketParameters.class) {
            return false;
        }
        StaBackgroundScanBucketParameters staBackgroundScanBucketParameters = (StaBackgroundScanBucketParameters) obj;
        if (this.bucketIdx == staBackgroundScanBucketParameters.bucketIdx && this.band == staBackgroundScanBucketParameters.band && HidlSupport.deepEquals(this.frequencies, staBackgroundScanBucketParameters.frequencies) && this.periodInMs == staBackgroundScanBucketParameters.periodInMs && HidlSupport.deepEquals(Integer.valueOf(this.eventReportScheme), Integer.valueOf(staBackgroundScanBucketParameters.eventReportScheme)) && this.exponentialMaxPeriodInMs == staBackgroundScanBucketParameters.exponentialMaxPeriodInMs && this.exponentialBase == staBackgroundScanBucketParameters.exponentialBase && this.exponentialStepCount == staBackgroundScanBucketParameters.exponentialStepCount) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.bucketIdx))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.band))), Integer.valueOf(HidlSupport.deepHashCode(this.frequencies)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.periodInMs))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.eventReportScheme))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.exponentialMaxPeriodInMs))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.exponentialBase))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.exponentialStepCount))));
    }

    public final String toString() {
        return "{.bucketIdx = " + this.bucketIdx + ", .band = " + WifiBand.toString(this.band) + ", .frequencies = " + this.frequencies + ", .periodInMs = " + this.periodInMs + ", .eventReportScheme = " + StaBackgroundScanBucketEventReportSchemeMask.dumpBitfield(this.eventReportScheme) + ", .exponentialMaxPeriodInMs = " + this.exponentialMaxPeriodInMs + ", .exponentialBase = " + this.exponentialBase + ", .exponentialStepCount = " + this.exponentialStepCount + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(48L), 0L);
    }

    public static final ArrayList<StaBackgroundScanBucketParameters> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<StaBackgroundScanBucketParameters> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 48, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            StaBackgroundScanBucketParameters staBackgroundScanBucketParameters = new StaBackgroundScanBucketParameters();
            staBackgroundScanBucketParameters.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 48);
            arrayList.add(staBackgroundScanBucketParameters);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.bucketIdx = hwBlob.getInt32(j + 0);
        this.band = hwBlob.getInt32(j + 4);
        long j2 = j + 8;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 4, hwBlob.handle(), j2 + 0, true);
        this.frequencies.clear();
        for (int i = 0; i < int32; i++) {
            this.frequencies.add(Integer.valueOf(embeddedBuffer.getInt32(i * 4)));
        }
        this.periodInMs = hwBlob.getInt32(j + 24);
        this.eventReportScheme = hwBlob.getInt32(j + 28);
        this.exponentialMaxPeriodInMs = hwBlob.getInt32(j + 32);
        this.exponentialBase = hwBlob.getInt32(j + 36);
        this.exponentialStepCount = hwBlob.getInt32(j + 40);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(48);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<StaBackgroundScanBucketParameters> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 48);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 48);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.bucketIdx);
        hwBlob.putInt32(4 + j, this.band);
        int size = this.frequencies.size();
        long j2 = j + 8;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 4);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt32(i * 4, this.frequencies.get(i).intValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        hwBlob.putInt32(24 + j, this.periodInMs);
        hwBlob.putInt32(28 + j, this.eventReportScheme);
        hwBlob.putInt32(32 + j, this.exponentialMaxPeriodInMs);
        hwBlob.putInt32(36 + j, this.exponentialBase);
        hwBlob.putInt32(j + 40, this.exponentialStepCount);
    }
}
