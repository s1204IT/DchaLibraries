package vendor.mediatek.hardware.radio.V3_0;

import android.hardware.radio.V1_0.SetupDataCallResult;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class MtkSetupDataCallResult {
    public final SetupDataCallResult dcr = new SetupDataCallResult();
    public int rat;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != MtkSetupDataCallResult.class) {
            return false;
        }
        MtkSetupDataCallResult mtkSetupDataCallResult = (MtkSetupDataCallResult) obj;
        if (HidlSupport.deepEquals(this.dcr, mtkSetupDataCallResult.dcr) && this.rat == mtkSetupDataCallResult.rat) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.dcr)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.rat))));
    }

    public final String toString() {
        return "{.dcr = " + this.dcr + ", .rat = " + this.rat + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(128L), 0L);
    }

    public static final ArrayList<MtkSetupDataCallResult> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<MtkSetupDataCallResult> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 128, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            MtkSetupDataCallResult mtkSetupDataCallResult = new MtkSetupDataCallResult();
            mtkSetupDataCallResult.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 128);
            arrayList.add(mtkSetupDataCallResult);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.dcr.readEmbeddedFromParcel(hwParcel, hwBlob, 0 + j);
        this.rat = hwBlob.getInt32(j + 120);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(128);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<MtkSetupDataCallResult> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 128);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 128);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        this.dcr.writeEmbeddedToBlob(hwBlob, 0 + j);
        hwBlob.putInt32(j + 120, this.rat);
    }
}
