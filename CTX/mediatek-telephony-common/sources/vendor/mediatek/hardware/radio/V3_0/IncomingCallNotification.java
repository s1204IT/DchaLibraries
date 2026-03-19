package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class IncomingCallNotification {
    public String callId = new String();
    public String number = new String();
    public String type = new String();
    public String callMode = new String();
    public String seqNo = new String();
    public String redirectNumber = new String();
    public String toNumber = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != IncomingCallNotification.class) {
            return false;
        }
        IncomingCallNotification incomingCallNotification = (IncomingCallNotification) obj;
        if (HidlSupport.deepEquals(this.callId, incomingCallNotification.callId) && HidlSupport.deepEquals(this.number, incomingCallNotification.number) && HidlSupport.deepEquals(this.type, incomingCallNotification.type) && HidlSupport.deepEquals(this.callMode, incomingCallNotification.callMode) && HidlSupport.deepEquals(this.seqNo, incomingCallNotification.seqNo) && HidlSupport.deepEquals(this.redirectNumber, incomingCallNotification.redirectNumber) && HidlSupport.deepEquals(this.toNumber, incomingCallNotification.toNumber)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.callId)), Integer.valueOf(HidlSupport.deepHashCode(this.number)), Integer.valueOf(HidlSupport.deepHashCode(this.type)), Integer.valueOf(HidlSupport.deepHashCode(this.callMode)), Integer.valueOf(HidlSupport.deepHashCode(this.seqNo)), Integer.valueOf(HidlSupport.deepHashCode(this.redirectNumber)), Integer.valueOf(HidlSupport.deepHashCode(this.toNumber)));
    }

    public final String toString() {
        return "{.callId = " + this.callId + ", .number = " + this.number + ", .type = " + this.type + ", .callMode = " + this.callMode + ", .seqNo = " + this.seqNo + ", .redirectNumber = " + this.redirectNumber + ", .toNumber = " + this.toNumber + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(112L), 0L);
    }

    public static final ArrayList<IncomingCallNotification> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<IncomingCallNotification> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 112, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            IncomingCallNotification incomingCallNotification = new IncomingCallNotification();
            incomingCallNotification.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 112);
            arrayList.add(incomingCallNotification);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        long j2 = j + 0;
        this.callId = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.callId.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        long j3 = j + 16;
        this.number = hwBlob.getString(j3);
        hwParcel.readEmbeddedBuffer(this.number.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
        long j4 = j + 32;
        this.type = hwBlob.getString(j4);
        hwParcel.readEmbeddedBuffer(this.type.getBytes().length + 1, hwBlob.handle(), j4 + 0, false);
        long j5 = j + 48;
        this.callMode = hwBlob.getString(j5);
        hwParcel.readEmbeddedBuffer(this.callMode.getBytes().length + 1, hwBlob.handle(), j5 + 0, false);
        long j6 = j + 64;
        this.seqNo = hwBlob.getString(j6);
        hwParcel.readEmbeddedBuffer(this.seqNo.getBytes().length + 1, hwBlob.handle(), j6 + 0, false);
        long j7 = j + 80;
        this.redirectNumber = hwBlob.getString(j7);
        hwParcel.readEmbeddedBuffer(this.redirectNumber.getBytes().length + 1, hwBlob.handle(), j7 + 0, false);
        long j8 = j + 96;
        this.toNumber = hwBlob.getString(j8);
        hwParcel.readEmbeddedBuffer(this.toNumber.getBytes().length + 1, hwBlob.handle(), j8 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(112);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<IncomingCallNotification> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 112);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 112);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putString(0 + j, this.callId);
        hwBlob.putString(16 + j, this.number);
        hwBlob.putString(32 + j, this.type);
        hwBlob.putString(48 + j, this.callMode);
        hwBlob.putString(64 + j, this.seqNo);
        hwBlob.putString(80 + j, this.redirectNumber);
        hwBlob.putString(j + 96, this.toNumber);
    }
}
