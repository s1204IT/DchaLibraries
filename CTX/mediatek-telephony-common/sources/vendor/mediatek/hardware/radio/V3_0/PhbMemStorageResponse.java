package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class PhbMemStorageResponse {
    public String storage = new String();
    public int total;
    public int used;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != PhbMemStorageResponse.class) {
            return false;
        }
        PhbMemStorageResponse phbMemStorageResponse = (PhbMemStorageResponse) obj;
        if (HidlSupport.deepEquals(this.storage, phbMemStorageResponse.storage) && this.used == phbMemStorageResponse.used && this.total == phbMemStorageResponse.total) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.storage)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.used))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.total))));
    }

    public final String toString() {
        return "{.storage = " + this.storage + ", .used = " + this.used + ", .total = " + this.total + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
    }

    public static final ArrayList<PhbMemStorageResponse> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<PhbMemStorageResponse> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            PhbMemStorageResponse phbMemStorageResponse = new PhbMemStorageResponse();
            phbMemStorageResponse.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
            arrayList.add(phbMemStorageResponse);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        long j2 = j + 0;
        this.storage = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.storage.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.used = hwBlob.getInt32(j + 16);
        this.total = hwBlob.getInt32(j + 20);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(24);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<PhbMemStorageResponse> arrayList) {
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
        hwBlob.putString(0 + j, this.storage);
        hwBlob.putInt32(16 + j, this.used);
        hwBlob.putInt32(j + 20, this.total);
    }
}
