package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class PhbEntryStructure {
    public int index;
    public int ton;
    public int type;
    public String number = new String();
    public String alphaId = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != PhbEntryStructure.class) {
            return false;
        }
        PhbEntryStructure phbEntryStructure = (PhbEntryStructure) obj;
        if (this.type == phbEntryStructure.type && this.index == phbEntryStructure.index && HidlSupport.deepEquals(this.number, phbEntryStructure.number) && this.ton == phbEntryStructure.ton && HidlSupport.deepEquals(this.alphaId, phbEntryStructure.alphaId)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.index))), Integer.valueOf(HidlSupport.deepHashCode(this.number)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.ton))), Integer.valueOf(HidlSupport.deepHashCode(this.alphaId)));
    }

    public final String toString() {
        return "{.type = " + this.type + ", .index = " + this.index + ", .number = " + this.number + ", .ton = " + this.ton + ", .alphaId = " + this.alphaId + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(48L), 0L);
    }

    public static final ArrayList<PhbEntryStructure> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<PhbEntryStructure> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 48, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            PhbEntryStructure phbEntryStructure = new PhbEntryStructure();
            phbEntryStructure.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 48);
            arrayList.add(phbEntryStructure);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.type = hwBlob.getInt32(j + 0);
        this.index = hwBlob.getInt32(j + 4);
        long j2 = j + 8;
        this.number = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.number.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.ton = hwBlob.getInt32(j + 24);
        long j3 = j + 32;
        this.alphaId = hwBlob.getString(j3);
        hwParcel.readEmbeddedBuffer(this.alphaId.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(48);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<PhbEntryStructure> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 48);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 48);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.type);
        hwBlob.putInt32(4 + j, this.index);
        hwBlob.putString(8 + j, this.number);
        hwBlob.putInt32(24 + j, this.ton);
        hwBlob.putString(j + 32, this.alphaId);
    }
}
