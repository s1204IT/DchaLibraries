package vendor.mediatek.hardware.radio.V3_0;

import android.hardware.radio.V1_0.OperatorInfo;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class OperatorInfoWithAct {
    public final OperatorInfo base = new OperatorInfo();
    public String lac = new String();
    public String act = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != OperatorInfoWithAct.class) {
            return false;
        }
        OperatorInfoWithAct operatorInfoWithAct = (OperatorInfoWithAct) obj;
        if (HidlSupport.deepEquals(this.base, operatorInfoWithAct.base) && HidlSupport.deepEquals(this.lac, operatorInfoWithAct.lac) && HidlSupport.deepEquals(this.act, operatorInfoWithAct.act)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.base)), Integer.valueOf(HidlSupport.deepHashCode(this.lac)), Integer.valueOf(HidlSupport.deepHashCode(this.act)));
    }

    public final String toString() {
        return "{.base = " + this.base + ", .lac = " + this.lac + ", .act = " + this.act + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(88L), 0L);
    }

    public static final ArrayList<OperatorInfoWithAct> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<OperatorInfoWithAct> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 88, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            OperatorInfoWithAct operatorInfoWithAct = new OperatorInfoWithAct();
            operatorInfoWithAct.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 88);
            arrayList.add(operatorInfoWithAct);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.base.readEmbeddedFromParcel(hwParcel, hwBlob, j + 0);
        long j2 = j + 56;
        this.lac = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.lac.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        long j3 = j + 72;
        this.act = hwBlob.getString(j3);
        hwParcel.readEmbeddedBuffer(this.act.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(88);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<OperatorInfoWithAct> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 88);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 88);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        this.base.writeEmbeddedToBlob(hwBlob, 0 + j);
        hwBlob.putString(56 + j, this.lac);
        hwBlob.putString(j + 72, this.act);
    }
}
