package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class StaLinkLayerStats {
    public final StaLinkLayerIfaceStats iface = new StaLinkLayerIfaceStats();
    public final ArrayList<StaLinkLayerRadioStats> radios = new ArrayList<>();
    public long timeStampInMs;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != StaLinkLayerStats.class) {
            return false;
        }
        StaLinkLayerStats staLinkLayerStats = (StaLinkLayerStats) obj;
        if (HidlSupport.deepEquals(this.iface, staLinkLayerStats.iface) && HidlSupport.deepEquals(this.radios, staLinkLayerStats.radios) && this.timeStampInMs == staLinkLayerStats.timeStampInMs) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.iface)), Integer.valueOf(HidlSupport.deepHashCode(this.radios)), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.timeStampInMs))));
    }

    public final String toString() {
        return "{.iface = " + this.iface + ", .radios = " + this.radios + ", .timeStampInMs = " + this.timeStampInMs + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(160L), 0L);
    }

    public static final ArrayList<StaLinkLayerStats> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<StaLinkLayerStats> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 160, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            StaLinkLayerStats staLinkLayerStats = new StaLinkLayerStats();
            staLinkLayerStats.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 160);
            arrayList.add(staLinkLayerStats);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.iface.readEmbeddedFromParcel(hwParcel, hwBlob, j + 0);
        long j2 = j + 136;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, hwBlob.handle(), j2 + 0, true);
        this.radios.clear();
        for (int i = 0; i < int32; i++) {
            StaLinkLayerRadioStats staLinkLayerRadioStats = new StaLinkLayerRadioStats();
            staLinkLayerRadioStats.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
            this.radios.add(staLinkLayerRadioStats);
        }
        this.timeStampInMs = hwBlob.getInt64(j + 152);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(160);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<StaLinkLayerStats> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 160);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 160);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        this.iface.writeEmbeddedToBlob(hwBlob, j + 0);
        int size = this.radios.size();
        long j2 = 136 + j;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 32);
        for (int i = 0; i < size; i++) {
            this.radios.get(i).writeEmbeddedToBlob(hwBlob2, i * 32);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        hwBlob.putInt64(j + 152, this.timeStampInMs);
    }
}
