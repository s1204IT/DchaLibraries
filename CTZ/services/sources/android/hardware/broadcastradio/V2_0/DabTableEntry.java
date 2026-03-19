package android.hardware.broadcastradio.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class DabTableEntry {
    public int frequency;
    public String label = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != DabTableEntry.class) {
            return false;
        }
        DabTableEntry dabTableEntry = (DabTableEntry) obj;
        if (HidlSupport.deepEquals(this.label, dabTableEntry.label) && this.frequency == dabTableEntry.frequency) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.label)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.frequency))));
    }

    public final String toString() {
        return "{.label = " + this.label + ", .frequency = " + this.frequency + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
    }

    public static final ArrayList<DabTableEntry> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<DabTableEntry> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            DabTableEntry dabTableEntry = new DabTableEntry();
            dabTableEntry.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
            arrayList.add(dabTableEntry);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        long j2 = j + 0;
        this.label = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.label.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.frequency = hwBlob.getInt32(j + 16);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(24);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<DabTableEntry> arrayList) {
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
        hwBlob.putString(0 + j, this.label);
        hwBlob.putInt32(j + 16, this.frequency);
    }
}
