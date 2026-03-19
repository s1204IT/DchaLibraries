package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class VsimMessage {
    public int length;
    public int messageId;
    public int slotId;
    public int transactionId;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != VsimMessage.class) {
            return false;
        }
        VsimMessage vsimMessage = (VsimMessage) obj;
        if (this.transactionId == vsimMessage.transactionId && this.messageId == vsimMessage.messageId && this.slotId == vsimMessage.slotId && this.length == vsimMessage.length) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.transactionId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.messageId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.slotId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.length))));
    }

    public final String toString() {
        return "{.transactionId = " + this.transactionId + ", .messageId = " + this.messageId + ", .slotId = " + this.slotId + ", .length = " + this.length + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(16L), 0L);
    }

    public static final ArrayList<VsimMessage> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<VsimMessage> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 16, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            VsimMessage vsimMessage = new VsimMessage();
            vsimMessage.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 16);
            arrayList.add(vsimMessage);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.transactionId = hwBlob.getInt32(0 + j);
        this.messageId = hwBlob.getInt32(4 + j);
        this.slotId = hwBlob.getInt32(8 + j);
        this.length = hwBlob.getInt32(j + 12);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(16);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<VsimMessage> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 16);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 16);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.transactionId);
        hwBlob.putInt32(4 + j, this.messageId);
        hwBlob.putInt32(8 + j, this.slotId);
        hwBlob.putInt32(j + 12, this.length);
    }
}
