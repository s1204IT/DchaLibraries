package android.hardware.weaver.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class WeaverConfig {
    public int keySize;
    public int slots;
    public int valueSize;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != WeaverConfig.class) {
            return false;
        }
        WeaverConfig weaverConfig = (WeaverConfig) obj;
        if (this.slots == weaverConfig.slots && this.keySize == weaverConfig.keySize && this.valueSize == weaverConfig.valueSize) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.slots))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.keySize))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.valueSize))));
    }

    public final String toString() {
        return "{.slots = " + this.slots + ", .keySize = " + this.keySize + ", .valueSize = " + this.valueSize + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(12L), 0L);
    }

    public static final ArrayList<WeaverConfig> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<WeaverConfig> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 12, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            WeaverConfig weaverConfig = new WeaverConfig();
            weaverConfig.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 12);
            arrayList.add(weaverConfig);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.slots = hwBlob.getInt32(0 + j);
        this.keySize = hwBlob.getInt32(4 + j);
        this.valueSize = hwBlob.getInt32(j + 8);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(12);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<WeaverConfig> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 12);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 12);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.slots);
        hwBlob.putInt32(4 + j, this.keySize);
        hwBlob.putInt32(j + 8, this.valueSize);
    }
}
