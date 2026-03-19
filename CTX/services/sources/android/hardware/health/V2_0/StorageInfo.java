package android.hardware.health.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class StorageInfo {
    public short eol;
    public short lifetimeA;
    public short lifetimeB;
    public final StorageAttribute attr = new StorageAttribute();
    public String version = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != StorageInfo.class) {
            return false;
        }
        StorageInfo storageInfo = (StorageInfo) obj;
        if (HidlSupport.deepEquals(this.attr, storageInfo.attr) && this.eol == storageInfo.eol && this.lifetimeA == storageInfo.lifetimeA && this.lifetimeB == storageInfo.lifetimeB && HidlSupport.deepEquals(this.version, storageInfo.version)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.attr)), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.eol))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.lifetimeA))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.lifetimeB))), Integer.valueOf(HidlSupport.deepHashCode(this.version)));
    }

    public final String toString() {
        return "{.attr = " + this.attr + ", .eol = " + ((int) this.eol) + ", .lifetimeA = " + ((int) this.lifetimeA) + ", .lifetimeB = " + ((int) this.lifetimeB) + ", .version = " + this.version + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(48L), 0L);
    }

    public static final ArrayList<StorageInfo> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<StorageInfo> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 48, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            StorageInfo storageInfo = new StorageInfo();
            storageInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 48);
            arrayList.add(storageInfo);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.attr.readEmbeddedFromParcel(hwParcel, hwBlob, j + 0);
        this.eol = hwBlob.getInt16(24 + j);
        this.lifetimeA = hwBlob.getInt16(26 + j);
        this.lifetimeB = hwBlob.getInt16(28 + j);
        long j2 = j + 32;
        this.version = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.version.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(48);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<StorageInfo> arrayList) {
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
        this.attr.writeEmbeddedToBlob(hwBlob, 0 + j);
        hwBlob.putInt16(24 + j, this.eol);
        hwBlob.putInt16(26 + j, this.lifetimeA);
        hwBlob.putInt16(28 + j, this.lifetimeB);
        hwBlob.putString(j + 32, this.version);
    }
}
