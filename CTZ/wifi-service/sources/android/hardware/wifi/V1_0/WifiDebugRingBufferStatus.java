package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class WifiDebugRingBufferStatus {
    public int flags;
    public int freeSizeInBytes;
    public int ringId;
    public String ringName = new String();
    public int sizeInBytes;
    public int verboseLevel;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != WifiDebugRingBufferStatus.class) {
            return false;
        }
        WifiDebugRingBufferStatus wifiDebugRingBufferStatus = (WifiDebugRingBufferStatus) obj;
        if (HidlSupport.deepEquals(this.ringName, wifiDebugRingBufferStatus.ringName) && this.flags == wifiDebugRingBufferStatus.flags && this.ringId == wifiDebugRingBufferStatus.ringId && this.sizeInBytes == wifiDebugRingBufferStatus.sizeInBytes && this.freeSizeInBytes == wifiDebugRingBufferStatus.freeSizeInBytes && this.verboseLevel == wifiDebugRingBufferStatus.verboseLevel) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.ringName)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.flags))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.ringId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.sizeInBytes))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.freeSizeInBytes))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.verboseLevel))));
    }

    public final String toString() {
        return "{.ringName = " + this.ringName + ", .flags = " + this.flags + ", .ringId = " + this.ringId + ", .sizeInBytes = " + this.sizeInBytes + ", .freeSizeInBytes = " + this.freeSizeInBytes + ", .verboseLevel = " + this.verboseLevel + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(40L), 0L);
    }

    public static final ArrayList<WifiDebugRingBufferStatus> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<WifiDebugRingBufferStatus> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 40, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            WifiDebugRingBufferStatus wifiDebugRingBufferStatus = new WifiDebugRingBufferStatus();
            wifiDebugRingBufferStatus.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 40);
            arrayList.add(wifiDebugRingBufferStatus);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        long j2 = j + 0;
        this.ringName = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.ringName.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.flags = hwBlob.getInt32(j + 16);
        this.ringId = hwBlob.getInt32(j + 20);
        this.sizeInBytes = hwBlob.getInt32(j + 24);
        this.freeSizeInBytes = hwBlob.getInt32(j + 28);
        this.verboseLevel = hwBlob.getInt32(j + 32);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(40);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<WifiDebugRingBufferStatus> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 40);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 40);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putString(0 + j, this.ringName);
        hwBlob.putInt32(16 + j, this.flags);
        hwBlob.putInt32(20 + j, this.ringId);
        hwBlob.putInt32(24 + j, this.sizeInBytes);
        hwBlob.putInt32(28 + j, this.freeSizeInBytes);
        hwBlob.putInt32(j + 32, this.verboseLevel);
    }
}
