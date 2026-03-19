package android.hardware.radio.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class CdmaNumberInfoRecord {
    public String number = new String();
    public byte numberPlan;
    public byte numberType;
    public byte pi;
    public byte si;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != CdmaNumberInfoRecord.class) {
            return false;
        }
        CdmaNumberInfoRecord cdmaNumberInfoRecord = (CdmaNumberInfoRecord) obj;
        if (HidlSupport.deepEquals(this.number, cdmaNumberInfoRecord.number) && this.numberType == cdmaNumberInfoRecord.numberType && this.numberPlan == cdmaNumberInfoRecord.numberPlan && this.pi == cdmaNumberInfoRecord.pi && this.si == cdmaNumberInfoRecord.si) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.number)), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.numberType))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.numberPlan))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.pi))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.si))));
    }

    public final String toString() {
        return "{.number = " + this.number + ", .numberType = " + ((int) this.numberType) + ", .numberPlan = " + ((int) this.numberPlan) + ", .pi = " + ((int) this.pi) + ", .si = " + ((int) this.si) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
    }

    public static final ArrayList<CdmaNumberInfoRecord> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<CdmaNumberInfoRecord> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            CdmaNumberInfoRecord cdmaNumberInfoRecord = new CdmaNumberInfoRecord();
            cdmaNumberInfoRecord.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
            arrayList.add(cdmaNumberInfoRecord);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        long j2 = j + 0;
        this.number = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.number.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.numberType = hwBlob.getInt8(j + 16);
        this.numberPlan = hwBlob.getInt8(j + 17);
        this.pi = hwBlob.getInt8(j + 18);
        this.si = hwBlob.getInt8(j + 19);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(24);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<CdmaNumberInfoRecord> arrayList) {
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
        hwBlob.putString(0 + j, this.number);
        hwBlob.putInt8(16 + j, this.numberType);
        hwBlob.putInt8(17 + j, this.numberPlan);
        hwBlob.putInt8(18 + j, this.pi);
        hwBlob.putInt8(j + 19, this.si);
    }
}
