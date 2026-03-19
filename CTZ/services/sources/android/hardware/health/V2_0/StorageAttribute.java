package android.hardware.health.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class StorageAttribute {
    public boolean isBootDevice;
    public boolean isInternal;
    public String name = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != StorageAttribute.class) {
            return false;
        }
        StorageAttribute storageAttribute = (StorageAttribute) obj;
        if (this.isInternal == storageAttribute.isInternal && this.isBootDevice == storageAttribute.isBootDevice && HidlSupport.deepEquals(this.name, storageAttribute.name)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.isInternal))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.isBootDevice))), Integer.valueOf(HidlSupport.deepHashCode(this.name)));
    }

    public final String toString() {
        return "{.isInternal = " + this.isInternal + ", .isBootDevice = " + this.isBootDevice + ", .name = " + this.name + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
    }

    public static final ArrayList<StorageAttribute> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<StorageAttribute> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            StorageAttribute storageAttribute = new StorageAttribute();
            storageAttribute.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
            arrayList.add(storageAttribute);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.isInternal = hwBlob.getBool(j + 0);
        this.isBootDevice = hwBlob.getBool(1 + j);
        long j2 = j + 8;
        this.name = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.name.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(24);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<StorageAttribute> arrayList) {
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
        hwBlob.putBool(0 + j, this.isInternal);
        hwBlob.putBool(1 + j, this.isBootDevice);
        hwBlob.putString(j + 8, this.name);
    }
}
