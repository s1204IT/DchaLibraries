package android.hardware.health.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class DiskStats {
    public final StorageAttribute attr = new StorageAttribute();
    public long ioInFlight;
    public long ioInQueue;
    public long ioTicks;
    public long readMerges;
    public long readSectors;
    public long readTicks;
    public long reads;
    public long writeMerges;
    public long writeSectors;
    public long writeTicks;
    public long writes;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != DiskStats.class) {
            return false;
        }
        DiskStats diskStats = (DiskStats) obj;
        if (this.reads == diskStats.reads && this.readMerges == diskStats.readMerges && this.readSectors == diskStats.readSectors && this.readTicks == diskStats.readTicks && this.writes == diskStats.writes && this.writeMerges == diskStats.writeMerges && this.writeSectors == diskStats.writeSectors && this.writeTicks == diskStats.writeTicks && this.ioInFlight == diskStats.ioInFlight && this.ioTicks == diskStats.ioTicks && this.ioInQueue == diskStats.ioInQueue && HidlSupport.deepEquals(this.attr, diskStats.attr)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.reads))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.readMerges))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.readSectors))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.readTicks))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.writes))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.writeMerges))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.writeSectors))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.writeTicks))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.ioInFlight))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.ioTicks))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.ioInQueue))), Integer.valueOf(HidlSupport.deepHashCode(this.attr)));
    }

    public final String toString() {
        return "{.reads = " + this.reads + ", .readMerges = " + this.readMerges + ", .readSectors = " + this.readSectors + ", .readTicks = " + this.readTicks + ", .writes = " + this.writes + ", .writeMerges = " + this.writeMerges + ", .writeSectors = " + this.writeSectors + ", .writeTicks = " + this.writeTicks + ", .ioInFlight = " + this.ioInFlight + ", .ioTicks = " + this.ioTicks + ", .ioInQueue = " + this.ioInQueue + ", .attr = " + this.attr + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(112L), 0L);
    }

    public static final ArrayList<DiskStats> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<DiskStats> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 112, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            DiskStats diskStats = new DiskStats();
            diskStats.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 112);
            arrayList.add(diskStats);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.reads = hwBlob.getInt64(0 + j);
        this.readMerges = hwBlob.getInt64(8 + j);
        this.readSectors = hwBlob.getInt64(16 + j);
        this.readTicks = hwBlob.getInt64(24 + j);
        this.writes = hwBlob.getInt64(32 + j);
        this.writeMerges = hwBlob.getInt64(40 + j);
        this.writeSectors = hwBlob.getInt64(48 + j);
        this.writeTicks = hwBlob.getInt64(56 + j);
        this.ioInFlight = hwBlob.getInt64(64 + j);
        this.ioTicks = hwBlob.getInt64(72 + j);
        this.ioInQueue = hwBlob.getInt64(80 + j);
        this.attr.readEmbeddedFromParcel(hwParcel, hwBlob, j + 88);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(112);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<DiskStats> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 112);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 112);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt64(0 + j, this.reads);
        hwBlob.putInt64(8 + j, this.readMerges);
        hwBlob.putInt64(16 + j, this.readSectors);
        hwBlob.putInt64(24 + j, this.readTicks);
        hwBlob.putInt64(32 + j, this.writes);
        hwBlob.putInt64(40 + j, this.writeMerges);
        hwBlob.putInt64(48 + j, this.writeSectors);
        hwBlob.putInt64(56 + j, this.writeTicks);
        hwBlob.putInt64(64 + j, this.ioInFlight);
        hwBlob.putInt64(72 + j, this.ioTicks);
        hwBlob.putInt64(80 + j, this.ioInQueue);
        this.attr.writeEmbeddedToBlob(hwBlob, j + 88);
    }
}
