package android.hardware.usb.V1_1;

import android.hardware.usb.V1_0.PortStatus;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class PortStatus_1_1 {
    public int currentMode;
    public final PortStatus status = new PortStatus();
    public int supportedModes;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != PortStatus_1_1.class) {
            return false;
        }
        PortStatus_1_1 portStatus_1_1 = (PortStatus_1_1) obj;
        if (HidlSupport.deepEquals(this.status, portStatus_1_1.status) && HidlSupport.deepEquals(Integer.valueOf(this.supportedModes), Integer.valueOf(portStatus_1_1.supportedModes)) && this.currentMode == portStatus_1_1.currentMode) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.status)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.supportedModes))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.currentMode))));
    }

    public final String toString() {
        return "{.status = " + this.status + ", .supportedModes = " + PortMode_1_1.dumpBitfield(this.supportedModes) + ", .currentMode = " + PortMode_1_1.toString(this.currentMode) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(48L), 0L);
    }

    public static final ArrayList<PortStatus_1_1> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<PortStatus_1_1> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 48, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            PortStatus_1_1 portStatus_1_1 = new PortStatus_1_1();
            portStatus_1_1.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 48);
            arrayList.add(portStatus_1_1);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.status.readEmbeddedFromParcel(hwParcel, hwBlob, 0 + j);
        this.supportedModes = hwBlob.getInt32(40 + j);
        this.currentMode = hwBlob.getInt32(j + 44);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(48);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<PortStatus_1_1> arrayList) {
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
        this.status.writeEmbeddedToBlob(hwBlob, 0 + j);
        hwBlob.putInt32(40 + j, this.supportedModes);
        hwBlob.putInt32(j + 44, this.currentMode);
    }
}
