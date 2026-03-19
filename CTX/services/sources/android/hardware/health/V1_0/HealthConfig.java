package android.hardware.health.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class HealthConfig {
    public int periodicChoresIntervalFast;
    public int periodicChoresIntervalSlow;
    public String batteryStatusPath = new String();
    public String batteryHealthPath = new String();
    public String batteryPresentPath = new String();
    public String batteryCapacityPath = new String();
    public String batteryVoltagePath = new String();
    public String batteryTemperaturePath = new String();
    public String batteryTechnologyPath = new String();
    public String batteryCurrentNowPath = new String();
    public String batteryCurrentAvgPath = new String();
    public String batteryChargeCounterPath = new String();
    public String batteryFullChargePath = new String();
    public String batteryCycleCountPath = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != HealthConfig.class) {
            return false;
        }
        HealthConfig healthConfig = (HealthConfig) obj;
        if (this.periodicChoresIntervalFast == healthConfig.periodicChoresIntervalFast && this.periodicChoresIntervalSlow == healthConfig.periodicChoresIntervalSlow && HidlSupport.deepEquals(this.batteryStatusPath, healthConfig.batteryStatusPath) && HidlSupport.deepEquals(this.batteryHealthPath, healthConfig.batteryHealthPath) && HidlSupport.deepEquals(this.batteryPresentPath, healthConfig.batteryPresentPath) && HidlSupport.deepEquals(this.batteryCapacityPath, healthConfig.batteryCapacityPath) && HidlSupport.deepEquals(this.batteryVoltagePath, healthConfig.batteryVoltagePath) && HidlSupport.deepEquals(this.batteryTemperaturePath, healthConfig.batteryTemperaturePath) && HidlSupport.deepEquals(this.batteryTechnologyPath, healthConfig.batteryTechnologyPath) && HidlSupport.deepEquals(this.batteryCurrentNowPath, healthConfig.batteryCurrentNowPath) && HidlSupport.deepEquals(this.batteryCurrentAvgPath, healthConfig.batteryCurrentAvgPath) && HidlSupport.deepEquals(this.batteryChargeCounterPath, healthConfig.batteryChargeCounterPath) && HidlSupport.deepEquals(this.batteryFullChargePath, healthConfig.batteryFullChargePath) && HidlSupport.deepEquals(this.batteryCycleCountPath, healthConfig.batteryCycleCountPath)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.periodicChoresIntervalFast))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.periodicChoresIntervalSlow))), Integer.valueOf(HidlSupport.deepHashCode(this.batteryStatusPath)), Integer.valueOf(HidlSupport.deepHashCode(this.batteryHealthPath)), Integer.valueOf(HidlSupport.deepHashCode(this.batteryPresentPath)), Integer.valueOf(HidlSupport.deepHashCode(this.batteryCapacityPath)), Integer.valueOf(HidlSupport.deepHashCode(this.batteryVoltagePath)), Integer.valueOf(HidlSupport.deepHashCode(this.batteryTemperaturePath)), Integer.valueOf(HidlSupport.deepHashCode(this.batteryTechnologyPath)), Integer.valueOf(HidlSupport.deepHashCode(this.batteryCurrentNowPath)), Integer.valueOf(HidlSupport.deepHashCode(this.batteryCurrentAvgPath)), Integer.valueOf(HidlSupport.deepHashCode(this.batteryChargeCounterPath)), Integer.valueOf(HidlSupport.deepHashCode(this.batteryFullChargePath)), Integer.valueOf(HidlSupport.deepHashCode(this.batteryCycleCountPath)));
    }

    public final String toString() {
        return "{.periodicChoresIntervalFast = " + this.periodicChoresIntervalFast + ", .periodicChoresIntervalSlow = " + this.periodicChoresIntervalSlow + ", .batteryStatusPath = " + this.batteryStatusPath + ", .batteryHealthPath = " + this.batteryHealthPath + ", .batteryPresentPath = " + this.batteryPresentPath + ", .batteryCapacityPath = " + this.batteryCapacityPath + ", .batteryVoltagePath = " + this.batteryVoltagePath + ", .batteryTemperaturePath = " + this.batteryTemperaturePath + ", .batteryTechnologyPath = " + this.batteryTechnologyPath + ", .batteryCurrentNowPath = " + this.batteryCurrentNowPath + ", .batteryCurrentAvgPath = " + this.batteryCurrentAvgPath + ", .batteryChargeCounterPath = " + this.batteryChargeCounterPath + ", .batteryFullChargePath = " + this.batteryFullChargePath + ", .batteryCycleCountPath = " + this.batteryCycleCountPath + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(200L), 0L);
    }

    public static final ArrayList<HealthConfig> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<HealthConfig> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 200, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            HealthConfig healthConfig = new HealthConfig();
            healthConfig.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 200);
            arrayList.add(healthConfig);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.periodicChoresIntervalFast = hwBlob.getInt32(j + 0);
        this.periodicChoresIntervalSlow = hwBlob.getInt32(j + 4);
        long j2 = j + 8;
        this.batteryStatusPath = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.batteryStatusPath.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        long j3 = j + 24;
        this.batteryHealthPath = hwBlob.getString(j3);
        hwParcel.readEmbeddedBuffer(this.batteryHealthPath.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
        long j4 = j + 40;
        this.batteryPresentPath = hwBlob.getString(j4);
        hwParcel.readEmbeddedBuffer(this.batteryPresentPath.getBytes().length + 1, hwBlob.handle(), j4 + 0, false);
        long j5 = j + 56;
        this.batteryCapacityPath = hwBlob.getString(j5);
        hwParcel.readEmbeddedBuffer(this.batteryCapacityPath.getBytes().length + 1, hwBlob.handle(), j5 + 0, false);
        long j6 = j + 72;
        this.batteryVoltagePath = hwBlob.getString(j6);
        hwParcel.readEmbeddedBuffer(this.batteryVoltagePath.getBytes().length + 1, hwBlob.handle(), j6 + 0, false);
        long j7 = j + 88;
        this.batteryTemperaturePath = hwBlob.getString(j7);
        hwParcel.readEmbeddedBuffer(this.batteryTemperaturePath.getBytes().length + 1, hwBlob.handle(), j7 + 0, false);
        long j8 = j + 104;
        this.batteryTechnologyPath = hwBlob.getString(j8);
        hwParcel.readEmbeddedBuffer(this.batteryTechnologyPath.getBytes().length + 1, hwBlob.handle(), j8 + 0, false);
        long j9 = j + 120;
        this.batteryCurrentNowPath = hwBlob.getString(j9);
        hwParcel.readEmbeddedBuffer(this.batteryCurrentNowPath.getBytes().length + 1, hwBlob.handle(), j9 + 0, false);
        long j10 = j + 136;
        this.batteryCurrentAvgPath = hwBlob.getString(j10);
        hwParcel.readEmbeddedBuffer(this.batteryCurrentAvgPath.getBytes().length + 1, hwBlob.handle(), j10 + 0, false);
        long j11 = j + 152;
        this.batteryChargeCounterPath = hwBlob.getString(j11);
        hwParcel.readEmbeddedBuffer(this.batteryChargeCounterPath.getBytes().length + 1, hwBlob.handle(), j11 + 0, false);
        long j12 = j + 168;
        this.batteryFullChargePath = hwBlob.getString(j12);
        hwParcel.readEmbeddedBuffer(this.batteryFullChargePath.getBytes().length + 1, hwBlob.handle(), j12 + 0, false);
        long j13 = j + 184;
        this.batteryCycleCountPath = hwBlob.getString(j13);
        hwParcel.readEmbeddedBuffer(this.batteryCycleCountPath.getBytes().length + 1, hwBlob.handle(), j13 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(200);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<HealthConfig> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 200);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 200);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.periodicChoresIntervalFast);
        hwBlob.putInt32(4 + j, this.periodicChoresIntervalSlow);
        hwBlob.putString(8 + j, this.batteryStatusPath);
        hwBlob.putString(24 + j, this.batteryHealthPath);
        hwBlob.putString(40 + j, this.batteryPresentPath);
        hwBlob.putString(56 + j, this.batteryCapacityPath);
        hwBlob.putString(72 + j, this.batteryVoltagePath);
        hwBlob.putString(88 + j, this.batteryTemperaturePath);
        hwBlob.putString(104 + j, this.batteryTechnologyPath);
        hwBlob.putString(120 + j, this.batteryCurrentNowPath);
        hwBlob.putString(136 + j, this.batteryCurrentAvgPath);
        hwBlob.putString(152 + j, this.batteryChargeCounterPath);
        hwBlob.putString(168 + j, this.batteryFullChargePath);
        hwBlob.putString(j + 184, this.batteryCycleCountPath);
    }
}
