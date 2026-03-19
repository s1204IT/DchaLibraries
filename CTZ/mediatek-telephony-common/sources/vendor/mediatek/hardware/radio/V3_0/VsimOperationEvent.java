package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class VsimOperationEvent {
    public String data = new String();
    public int dataLength;
    public int eventId;
    public int result;
    public int transactionId;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != VsimOperationEvent.class) {
            return false;
        }
        VsimOperationEvent vsimOperationEvent = (VsimOperationEvent) obj;
        if (this.transactionId == vsimOperationEvent.transactionId && this.eventId == vsimOperationEvent.eventId && this.result == vsimOperationEvent.result && this.dataLength == vsimOperationEvent.dataLength && HidlSupport.deepEquals(this.data, vsimOperationEvent.data)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.transactionId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.eventId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.result))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.dataLength))), Integer.valueOf(HidlSupport.deepHashCode(this.data)));
    }

    public final String toString() {
        return "{.transactionId = " + this.transactionId + ", .eventId = " + this.eventId + ", .result = " + this.result + ", .dataLength = " + this.dataLength + ", .data = " + this.data + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(32L), 0L);
    }

    public static final ArrayList<VsimOperationEvent> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<VsimOperationEvent> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            VsimOperationEvent vsimOperationEvent = new VsimOperationEvent();
            vsimOperationEvent.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
            arrayList.add(vsimOperationEvent);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.transactionId = hwBlob.getInt32(j + 0);
        this.eventId = hwBlob.getInt32(4 + j);
        this.result = hwBlob.getInt32(8 + j);
        this.dataLength = hwBlob.getInt32(12 + j);
        long j2 = j + 16;
        this.data = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.data.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(32);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<VsimOperationEvent> arrayList) {
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
        hwBlob.putInt32(0 + j, this.transactionId);
        hwBlob.putInt32(4 + j, this.eventId);
        hwBlob.putInt32(8 + j, this.result);
        hwBlob.putInt32(12 + j, this.dataLength);
        hwBlob.putString(j + 16, this.data);
    }
}
