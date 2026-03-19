package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class EtwsNotification {
    public int messageId;
    public String plmnId = new String();
    public String securityInfo = new String();
    public int serialNumber;
    public int warningType;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != EtwsNotification.class) {
            return false;
        }
        EtwsNotification etwsNotification = (EtwsNotification) obj;
        if (this.warningType == etwsNotification.warningType && this.messageId == etwsNotification.messageId && this.serialNumber == etwsNotification.serialNumber && HidlSupport.deepEquals(this.plmnId, etwsNotification.plmnId) && HidlSupport.deepEquals(this.securityInfo, etwsNotification.securityInfo)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.warningType))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.messageId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.serialNumber))), Integer.valueOf(HidlSupport.deepHashCode(this.plmnId)), Integer.valueOf(HidlSupport.deepHashCode(this.securityInfo)));
    }

    public final String toString() {
        return "{.warningType = " + this.warningType + ", .messageId = " + this.messageId + ", .serialNumber = " + this.serialNumber + ", .plmnId = " + this.plmnId + ", .securityInfo = " + this.securityInfo + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(48L), 0L);
    }

    public static final ArrayList<EtwsNotification> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<EtwsNotification> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 48, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            EtwsNotification etwsNotification = new EtwsNotification();
            etwsNotification.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 48);
            arrayList.add(etwsNotification);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.warningType = hwBlob.getInt32(j + 0);
        this.messageId = hwBlob.getInt32(j + 4);
        this.serialNumber = hwBlob.getInt32(j + 8);
        long j2 = j + 16;
        this.plmnId = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.plmnId.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        long j3 = j + 32;
        this.securityInfo = hwBlob.getString(j3);
        hwParcel.readEmbeddedBuffer(this.securityInfo.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(48);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<EtwsNotification> arrayList) {
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
        hwBlob.putInt32(0 + j, this.warningType);
        hwBlob.putInt32(4 + j, this.messageId);
        hwBlob.putInt32(8 + j, this.serialNumber);
        hwBlob.putString(16 + j, this.plmnId);
        hwBlob.putString(j + 32, this.securityInfo);
    }
}
