package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class SimAuthStructure {
    public int mode;
    public String param1 = new String();
    public String param2 = new String();
    public int sessionId;
    public int tag;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != SimAuthStructure.class) {
            return false;
        }
        SimAuthStructure simAuthStructure = (SimAuthStructure) obj;
        if (this.sessionId == simAuthStructure.sessionId && this.mode == simAuthStructure.mode && HidlSupport.deepEquals(this.param1, simAuthStructure.param1) && HidlSupport.deepEquals(this.param2, simAuthStructure.param2) && this.tag == simAuthStructure.tag) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.sessionId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.mode))), Integer.valueOf(HidlSupport.deepHashCode(this.param1)), Integer.valueOf(HidlSupport.deepHashCode(this.param2)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.tag))));
    }

    public final String toString() {
        return "{.sessionId = " + this.sessionId + ", .mode = " + this.mode + ", .param1 = " + this.param1 + ", .param2 = " + this.param2 + ", .tag = " + this.tag + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(48L), 0L);
    }

    public static final ArrayList<SimAuthStructure> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<SimAuthStructure> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 48, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            SimAuthStructure simAuthStructure = new SimAuthStructure();
            simAuthStructure.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 48);
            arrayList.add(simAuthStructure);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.sessionId = hwBlob.getInt32(j + 0);
        this.mode = hwBlob.getInt32(j + 4);
        long j2 = j + 8;
        this.param1 = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.param1.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        long j3 = j + 24;
        this.param2 = hwBlob.getString(j3);
        hwParcel.readEmbeddedBuffer(this.param2.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
        this.tag = hwBlob.getInt32(j + 40);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(48);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<SimAuthStructure> arrayList) {
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
        hwBlob.putInt32(0 + j, this.sessionId);
        hwBlob.putInt32(4 + j, this.mode);
        hwBlob.putString(8 + j, this.param1);
        hwBlob.putString(24 + j, this.param2);
        hwBlob.putInt32(j + 40, this.tag);
    }
}
