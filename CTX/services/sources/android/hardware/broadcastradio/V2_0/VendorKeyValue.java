package android.hardware.broadcastradio.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class VendorKeyValue {
    public String key = new String();
    public String value = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != VendorKeyValue.class) {
            return false;
        }
        VendorKeyValue vendorKeyValue = (VendorKeyValue) obj;
        if (HidlSupport.deepEquals(this.key, vendorKeyValue.key) && HidlSupport.deepEquals(this.value, vendorKeyValue.value)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.key)), Integer.valueOf(HidlSupport.deepHashCode(this.value)));
    }

    public final String toString() {
        return "{.key = " + this.key + ", .value = " + this.value + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(32L), 0L);
    }

    public static final ArrayList<VendorKeyValue> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<VendorKeyValue> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            VendorKeyValue vendorKeyValue = new VendorKeyValue();
            vendorKeyValue.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
            arrayList.add(vendorKeyValue);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        long j2 = j + 0;
        this.key = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.key.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        long j3 = j + 16;
        this.value = hwBlob.getString(j3);
        hwParcel.readEmbeddedBuffer(this.value.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(32);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<VendorKeyValue> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 32);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 32);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putString(0 + j, this.key);
        hwBlob.putString(j + 16, this.value);
    }
}
