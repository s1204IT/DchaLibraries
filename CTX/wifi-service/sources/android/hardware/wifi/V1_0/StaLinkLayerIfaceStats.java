package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class StaLinkLayerIfaceStats {
    public int avgRssiMgmt;
    public int beaconRx;
    public final StaLinkLayerIfacePacketStats wmeBePktStats = new StaLinkLayerIfacePacketStats();
    public final StaLinkLayerIfacePacketStats wmeBkPktStats = new StaLinkLayerIfacePacketStats();
    public final StaLinkLayerIfacePacketStats wmeViPktStats = new StaLinkLayerIfacePacketStats();
    public final StaLinkLayerIfacePacketStats wmeVoPktStats = new StaLinkLayerIfacePacketStats();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != StaLinkLayerIfaceStats.class) {
            return false;
        }
        StaLinkLayerIfaceStats staLinkLayerIfaceStats = (StaLinkLayerIfaceStats) obj;
        if (this.beaconRx == staLinkLayerIfaceStats.beaconRx && this.avgRssiMgmt == staLinkLayerIfaceStats.avgRssiMgmt && HidlSupport.deepEquals(this.wmeBePktStats, staLinkLayerIfaceStats.wmeBePktStats) && HidlSupport.deepEquals(this.wmeBkPktStats, staLinkLayerIfaceStats.wmeBkPktStats) && HidlSupport.deepEquals(this.wmeViPktStats, staLinkLayerIfaceStats.wmeViPktStats) && HidlSupport.deepEquals(this.wmeVoPktStats, staLinkLayerIfaceStats.wmeVoPktStats)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.beaconRx))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.avgRssiMgmt))), Integer.valueOf(HidlSupport.deepHashCode(this.wmeBePktStats)), Integer.valueOf(HidlSupport.deepHashCode(this.wmeBkPktStats)), Integer.valueOf(HidlSupport.deepHashCode(this.wmeViPktStats)), Integer.valueOf(HidlSupport.deepHashCode(this.wmeVoPktStats)));
    }

    public final String toString() {
        return "{.beaconRx = " + this.beaconRx + ", .avgRssiMgmt = " + this.avgRssiMgmt + ", .wmeBePktStats = " + this.wmeBePktStats + ", .wmeBkPktStats = " + this.wmeBkPktStats + ", .wmeViPktStats = " + this.wmeViPktStats + ", .wmeVoPktStats = " + this.wmeVoPktStats + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(136L), 0L);
    }

    public static final ArrayList<StaLinkLayerIfaceStats> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<StaLinkLayerIfaceStats> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 136, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            StaLinkLayerIfaceStats staLinkLayerIfaceStats = new StaLinkLayerIfaceStats();
            staLinkLayerIfaceStats.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 136);
            arrayList.add(staLinkLayerIfaceStats);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.beaconRx = hwBlob.getInt32(0 + j);
        this.avgRssiMgmt = hwBlob.getInt32(4 + j);
        this.wmeBePktStats.readEmbeddedFromParcel(hwParcel, hwBlob, 8 + j);
        this.wmeBkPktStats.readEmbeddedFromParcel(hwParcel, hwBlob, 40 + j);
        this.wmeViPktStats.readEmbeddedFromParcel(hwParcel, hwBlob, 72 + j);
        this.wmeVoPktStats.readEmbeddedFromParcel(hwParcel, hwBlob, j + 104);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(136);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<StaLinkLayerIfaceStats> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 136);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 136);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.beaconRx);
        hwBlob.putInt32(4 + j, this.avgRssiMgmt);
        this.wmeBePktStats.writeEmbeddedToBlob(hwBlob, 8 + j);
        this.wmeBkPktStats.writeEmbeddedToBlob(hwBlob, 40 + j);
        this.wmeViPktStats.writeEmbeddedToBlob(hwBlob, 72 + j);
        this.wmeVoPktStats.writeEmbeddedToBlob(hwBlob, j + 104);
    }
}
