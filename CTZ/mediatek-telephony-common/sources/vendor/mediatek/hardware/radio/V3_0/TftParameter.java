package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class TftParameter {
    public final ArrayList<Integer> linkedPfList = new ArrayList<>();
    public int linkedPfNumber;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != TftParameter.class) {
            return false;
        }
        TftParameter tftParameter = (TftParameter) obj;
        if (this.linkedPfNumber == tftParameter.linkedPfNumber && HidlSupport.deepEquals(this.linkedPfList, tftParameter.linkedPfList)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.linkedPfNumber))), Integer.valueOf(HidlSupport.deepHashCode(this.linkedPfList)));
    }

    public final String toString() {
        return "{.linkedPfNumber = " + this.linkedPfNumber + ", .linkedPfList = " + this.linkedPfList + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
    }

    public static final ArrayList<TftParameter> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<TftParameter> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            TftParameter tftParameter = new TftParameter();
            tftParameter.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
            arrayList.add(tftParameter);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.linkedPfNumber = hwBlob.getInt32(j + 0);
        long j2 = j + 8;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 4, hwBlob.handle(), j2 + 0, true);
        this.linkedPfList.clear();
        for (int i = 0; i < int32; i++) {
            this.linkedPfList.add(Integer.valueOf(embeddedBuffer.getInt32(i * 4)));
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(24);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<TftParameter> arrayList) {
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
        hwBlob.putInt32(j + 0, this.linkedPfNumber);
        int size = this.linkedPfList.size();
        long j2 = j + 8;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 4);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt32(i * 4, this.linkedPfList.get(i).intValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
