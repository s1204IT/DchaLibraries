package android.hardware.broadcastradio.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class Metadata {
    public long intValue;
    public int key;
    public String stringValue = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != Metadata.class) {
            return false;
        }
        Metadata metadata = (Metadata) obj;
        if (this.key == metadata.key && this.intValue == metadata.intValue && HidlSupport.deepEquals(this.stringValue, metadata.stringValue)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.key))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.intValue))), Integer.valueOf(HidlSupport.deepHashCode(this.stringValue)));
    }

    public final String toString() {
        return "{.key = " + this.key + ", .intValue = " + this.intValue + ", .stringValue = " + this.stringValue + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(32L), 0L);
    }

    public static final ArrayList<Metadata> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<Metadata> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            Metadata metadata = new Metadata();
            metadata.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
            arrayList.add(metadata);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.key = hwBlob.getInt32(j + 0);
        this.intValue = hwBlob.getInt64(8 + j);
        long j2 = j + 16;
        this.stringValue = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.stringValue.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(32);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<Metadata> arrayList) {
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
        hwBlob.putInt32(0 + j, this.key);
        hwBlob.putInt64(8 + j, this.intValue);
        hwBlob.putString(j + 16, this.stringValue);
    }
}
