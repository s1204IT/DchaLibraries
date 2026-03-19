package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class StaScanData {
    public int bucketsScanned;
    public int flags;
    public final ArrayList<StaScanResult> results = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != StaScanData.class) {
            return false;
        }
        StaScanData staScanData = (StaScanData) obj;
        if (HidlSupport.deepEquals(Integer.valueOf(this.flags), Integer.valueOf(staScanData.flags)) && this.bucketsScanned == staScanData.bucketsScanned && HidlSupport.deepEquals(this.results, staScanData.results)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.flags))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.bucketsScanned))), Integer.valueOf(HidlSupport.deepHashCode(this.results)));
    }

    public final String toString() {
        return "{.flags = " + StaScanDataFlagMask.dumpBitfield(this.flags) + ", .bucketsScanned = " + this.bucketsScanned + ", .results = " + this.results + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
    }

    public static final ArrayList<StaScanData> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<StaScanData> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            StaScanData staScanData = new StaScanData();
            staScanData.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
            arrayList.add(staScanData);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.flags = hwBlob.getInt32(j + 0);
        this.bucketsScanned = hwBlob.getInt32(j + 4);
        long j2 = j + 8;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 64, hwBlob.handle(), j2 + 0, true);
        this.results.clear();
        for (int i = 0; i < int32; i++) {
            StaScanResult staScanResult = new StaScanResult();
            staScanResult.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 64);
            this.results.add(staScanResult);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(24);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<StaScanData> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 24);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 24);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.flags);
        hwBlob.putInt32(4 + j, this.bucketsScanned);
        int size = this.results.size();
        long j2 = j + 8;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 64);
        for (int i = 0; i < size; i++) {
            this.results.get(i).writeEmbeddedToBlob(hwBlob2, i * 64);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
