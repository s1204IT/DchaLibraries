package android.hardware.health.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class HealthInfo {
    public int batteryCurrentAverage;
    public final android.hardware.health.V1_0.HealthInfo legacy = new android.hardware.health.V1_0.HealthInfo();
    public final ArrayList<DiskStats> diskStats = new ArrayList<>();
    public final ArrayList<StorageInfo> storageInfos = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != HealthInfo.class) {
            return false;
        }
        HealthInfo healthInfo = (HealthInfo) obj;
        if (HidlSupport.deepEquals(this.legacy, healthInfo.legacy) && this.batteryCurrentAverage == healthInfo.batteryCurrentAverage && HidlSupport.deepEquals(this.diskStats, healthInfo.diskStats) && HidlSupport.deepEquals(this.storageInfos, healthInfo.storageInfos)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.legacy)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.batteryCurrentAverage))), Integer.valueOf(HidlSupport.deepHashCode(this.diskStats)), Integer.valueOf(HidlSupport.deepHashCode(this.storageInfos)));
    }

    public final String toString() {
        return "{.legacy = " + this.legacy + ", .batteryCurrentAverage = " + this.batteryCurrentAverage + ", .diskStats = " + this.diskStats + ", .storageInfos = " + this.storageInfos + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(112L), 0L);
    }

    public static final ArrayList<HealthInfo> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<HealthInfo> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 112, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            HealthInfo healthInfo = new HealthInfo();
            healthInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 112);
            arrayList.add(healthInfo);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.legacy.readEmbeddedFromParcel(hwParcel, hwBlob, j + 0);
        this.batteryCurrentAverage = hwBlob.getInt32(j + 72);
        long j2 = j + 80;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 112, hwBlob.handle(), j2 + 0, true);
        this.diskStats.clear();
        for (int i = 0; i < int32; i++) {
            DiskStats diskStats = new DiskStats();
            diskStats.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 112);
            this.diskStats.add(diskStats);
        }
        long j3 = j + 96;
        int int322 = hwBlob.getInt32(j3 + 8);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 48, hwBlob.handle(), 0 + j3, true);
        this.storageInfos.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            StorageInfo storageInfo = new StorageInfo();
            storageInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer2, i2 * 48);
            this.storageInfos.add(storageInfo);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(112);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<HealthInfo> arrayList) {
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
        this.legacy.writeEmbeddedToBlob(hwBlob, j + 0);
        hwBlob.putInt32(j + 72, this.batteryCurrentAverage);
        int size = this.diskStats.size();
        long j2 = j + 80;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 112);
        for (int i = 0; i < size; i++) {
            this.diskStats.get(i).writeEmbeddedToBlob(hwBlob2, i * 112);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        int size2 = this.storageInfos.size();
        long j3 = j + 96;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 48);
        for (int i2 = 0; i2 < size2; i2++) {
            this.storageInfos.get(i2).writeEmbeddedToBlob(hwBlob3, i2 * 48);
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
    }
}
