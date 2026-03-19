package android.hardware.radio.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class HardwareConfig {
    public int state;
    public int type;
    public String uuid = new String();
    public final ArrayList<HardwareConfigModem> modem = new ArrayList<>();
    public final ArrayList<HardwareConfigSim> sim = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != HardwareConfig.class) {
            return false;
        }
        HardwareConfig hardwareConfig = (HardwareConfig) obj;
        if (this.type == hardwareConfig.type && HidlSupport.deepEquals(this.uuid, hardwareConfig.uuid) && this.state == hardwareConfig.state && HidlSupport.deepEquals(this.modem, hardwareConfig.modem) && HidlSupport.deepEquals(this.sim, hardwareConfig.sim)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(this.uuid)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.state))), Integer.valueOf(HidlSupport.deepHashCode(this.modem)), Integer.valueOf(HidlSupport.deepHashCode(this.sim)));
    }

    public final String toString() {
        return "{.type = " + HardwareConfigType.toString(this.type) + ", .uuid = " + this.uuid + ", .state = " + HardwareConfigState.toString(this.state) + ", .modem = " + this.modem + ", .sim = " + this.sim + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(64L), 0L);
    }

    public static final ArrayList<HardwareConfig> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<HardwareConfig> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 64, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            HardwareConfig hardwareConfig = new HardwareConfig();
            hardwareConfig.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 64);
            arrayList.add(hardwareConfig);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.type = hwBlob.getInt32(j + 0);
        long j2 = j + 8;
        this.uuid = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.uuid.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.state = hwBlob.getInt32(j + 24);
        long j3 = j + 32;
        int int32 = hwBlob.getInt32(j3 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 20, hwBlob.handle(), j3 + 0, true);
        this.modem.clear();
        for (int i = 0; i < int32; i++) {
            HardwareConfigModem hardwareConfigModem = new HardwareConfigModem();
            hardwareConfigModem.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 20);
            this.modem.add(hardwareConfigModem);
        }
        long j4 = j + 48;
        int int322 = hwBlob.getInt32(j4 + 8);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 16, hwBlob.handle(), 0 + j4, true);
        this.sim.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            HardwareConfigSim hardwareConfigSim = new HardwareConfigSim();
            hardwareConfigSim.readEmbeddedFromParcel(hwParcel, embeddedBuffer2, i2 * 16);
            this.sim.add(hardwareConfigSim);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(64);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<HardwareConfig> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 64);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 64);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.type);
        hwBlob.putString(j + 8, this.uuid);
        hwBlob.putInt32(j + 24, this.state);
        int size = this.modem.size();
        long j2 = j + 32;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 20);
        for (int i = 0; i < size; i++) {
            this.modem.get(i).writeEmbeddedToBlob(hwBlob2, i * 20);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        int size2 = this.sim.size();
        long j3 = j + 48;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 16);
        for (int i2 = 0; i2 < size2; i2++) {
            this.sim.get(i2).writeEmbeddedToBlob(hwBlob3, i2 * 16);
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
    }
}
