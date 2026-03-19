package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class VsimEvent {
    public int eventId;
    public int simType;
    public int transactionId;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != VsimEvent.class) {
            return false;
        }
        VsimEvent vsimEvent = (VsimEvent) obj;
        if (this.transactionId == vsimEvent.transactionId && this.eventId == vsimEvent.eventId && this.simType == vsimEvent.simType) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.transactionId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.eventId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.simType))));
    }

    public final String toString() {
        return "{.transactionId = " + this.transactionId + ", .eventId = " + this.eventId + ", .simType = " + this.simType + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(12L), 0L);
    }

    public static final ArrayList<VsimEvent> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<VsimEvent> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 12, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            VsimEvent vsimEvent = new VsimEvent();
            vsimEvent.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 12);
            arrayList.add(vsimEvent);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.transactionId = hwBlob.getInt32(0 + j);
        this.eventId = hwBlob.getInt32(4 + j);
        this.simType = hwBlob.getInt32(j + 8);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(12);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<VsimEvent> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 12);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 12);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.transactionId);
        hwBlob.putInt32(4 + j, this.eventId);
        hwBlob.putInt32(j + 8, this.simType);
    }
}
