package android.hardware.configstore.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class OptionalBool {
    public boolean specified;
    public boolean value;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != OptionalBool.class) {
            return false;
        }
        OptionalBool optionalBool = (OptionalBool) obj;
        if (this.specified == optionalBool.specified && this.value == optionalBool.value) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.specified))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.value))));
    }

    public final String toString() {
        return "{.specified = " + this.specified + ", .value = " + this.value + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(2L), 0L);
    }

    public static final ArrayList<OptionalBool> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<OptionalBool> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 2, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            OptionalBool optionalBool = new OptionalBool();
            optionalBool.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 2);
            arrayList.add(optionalBool);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.specified = hwBlob.getBool(0 + j);
        this.value = hwBlob.getBool(j + 1);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(2);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<OptionalBool> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 2);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 2);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putBool(0 + j, this.specified);
        hwBlob.putBool(j + 1, this.value);
    }
}
