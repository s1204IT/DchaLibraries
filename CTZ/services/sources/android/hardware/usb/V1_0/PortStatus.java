package android.hardware.usb.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class PortStatus {
    public boolean canChangeDataRole;
    public boolean canChangeMode;
    public boolean canChangePowerRole;
    public int currentDataRole;
    public int currentMode;
    public int currentPowerRole;
    public String portName = new String();
    public int supportedModes;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != PortStatus.class) {
            return false;
        }
        PortStatus portStatus = (PortStatus) obj;
        if (HidlSupport.deepEquals(this.portName, portStatus.portName) && this.currentDataRole == portStatus.currentDataRole && this.currentPowerRole == portStatus.currentPowerRole && this.currentMode == portStatus.currentMode && this.canChangeMode == portStatus.canChangeMode && this.canChangeDataRole == portStatus.canChangeDataRole && this.canChangePowerRole == portStatus.canChangePowerRole && this.supportedModes == portStatus.supportedModes) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.portName)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.currentDataRole))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.currentPowerRole))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.currentMode))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.canChangeMode))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.canChangeDataRole))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.canChangePowerRole))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.supportedModes))));
    }

    public final String toString() {
        return "{.portName = " + this.portName + ", .currentDataRole = " + PortDataRole.toString(this.currentDataRole) + ", .currentPowerRole = " + PortPowerRole.toString(this.currentPowerRole) + ", .currentMode = " + PortMode.toString(this.currentMode) + ", .canChangeMode = " + this.canChangeMode + ", .canChangeDataRole = " + this.canChangeDataRole + ", .canChangePowerRole = " + this.canChangePowerRole + ", .supportedModes = " + PortMode.toString(this.supportedModes) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(40L), 0L);
    }

    public static final ArrayList<PortStatus> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<PortStatus> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 40, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            PortStatus portStatus = new PortStatus();
            portStatus.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 40);
            arrayList.add(portStatus);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        long j2 = j + 0;
        this.portName = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.portName.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.currentDataRole = hwBlob.getInt32(j + 16);
        this.currentPowerRole = hwBlob.getInt32(j + 20);
        this.currentMode = hwBlob.getInt32(j + 24);
        this.canChangeMode = hwBlob.getBool(j + 28);
        this.canChangeDataRole = hwBlob.getBool(j + 29);
        this.canChangePowerRole = hwBlob.getBool(j + 30);
        this.supportedModes = hwBlob.getInt32(j + 32);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(40);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<PortStatus> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 40);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 40);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putString(0 + j, this.portName);
        hwBlob.putInt32(16 + j, this.currentDataRole);
        hwBlob.putInt32(20 + j, this.currentPowerRole);
        hwBlob.putInt32(24 + j, this.currentMode);
        hwBlob.putBool(28 + j, this.canChangeMode);
        hwBlob.putBool(29 + j, this.canChangeDataRole);
        hwBlob.putBool(30 + j, this.canChangePowerRole);
        hwBlob.putInt32(j + 32, this.supportedModes);
    }
}
