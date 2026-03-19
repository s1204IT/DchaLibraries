package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class StaLinkLayerIfacePacketStats {
    public long lostMpdu;
    public long retries;
    public long rxMpdu;
    public long txMpdu;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != StaLinkLayerIfacePacketStats.class) {
            return false;
        }
        StaLinkLayerIfacePacketStats staLinkLayerIfacePacketStats = (StaLinkLayerIfacePacketStats) obj;
        if (this.rxMpdu == staLinkLayerIfacePacketStats.rxMpdu && this.txMpdu == staLinkLayerIfacePacketStats.txMpdu && this.lostMpdu == staLinkLayerIfacePacketStats.lostMpdu && this.retries == staLinkLayerIfacePacketStats.retries) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.rxMpdu))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.txMpdu))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.lostMpdu))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.retries))));
    }

    public final String toString() {
        return "{.rxMpdu = " + this.rxMpdu + ", .txMpdu = " + this.txMpdu + ", .lostMpdu = " + this.lostMpdu + ", .retries = " + this.retries + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(32L), 0L);
    }

    public static final ArrayList<StaLinkLayerIfacePacketStats> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<StaLinkLayerIfacePacketStats> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            StaLinkLayerIfacePacketStats staLinkLayerIfacePacketStats = new StaLinkLayerIfacePacketStats();
            staLinkLayerIfacePacketStats.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
            arrayList.add(staLinkLayerIfacePacketStats);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.rxMpdu = hwBlob.getInt64(0 + j);
        this.txMpdu = hwBlob.getInt64(8 + j);
        this.lostMpdu = hwBlob.getInt64(16 + j);
        this.retries = hwBlob.getInt64(j + 24);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(32);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<StaLinkLayerIfacePacketStats> arrayList) {
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
        hwBlob.putInt64(0 + j, this.rxMpdu);
        hwBlob.putInt64(8 + j, this.txMpdu);
        hwBlob.putInt64(16 + j, this.lostMpdu);
        hwBlob.putInt64(j + 24, this.retries);
    }
}
