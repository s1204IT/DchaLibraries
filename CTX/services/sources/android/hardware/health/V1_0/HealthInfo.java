package android.hardware.health.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class HealthInfo {
    public int batteryChargeCounter;
    public int batteryCurrent;
    public int batteryCycleCount;
    public int batteryFullCharge;
    public int batteryHealth;
    public int batteryLevel;
    public boolean batteryPresent;
    public int batteryStatus;
    public String batteryTechnology = new String();
    public int batteryTemperature;
    public int batteryVoltage;
    public boolean chargerAcOnline;
    public boolean chargerUsbOnline;
    public boolean chargerWirelessOnline;
    public int maxChargingCurrent;
    public int maxChargingVoltage;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != HealthInfo.class) {
            return false;
        }
        HealthInfo healthInfo = (HealthInfo) obj;
        if (this.chargerAcOnline == healthInfo.chargerAcOnline && this.chargerUsbOnline == healthInfo.chargerUsbOnline && this.chargerWirelessOnline == healthInfo.chargerWirelessOnline && this.maxChargingCurrent == healthInfo.maxChargingCurrent && this.maxChargingVoltage == healthInfo.maxChargingVoltage && this.batteryStatus == healthInfo.batteryStatus && this.batteryHealth == healthInfo.batteryHealth && this.batteryPresent == healthInfo.batteryPresent && this.batteryLevel == healthInfo.batteryLevel && this.batteryVoltage == healthInfo.batteryVoltage && this.batteryTemperature == healthInfo.batteryTemperature && this.batteryCurrent == healthInfo.batteryCurrent && this.batteryCycleCount == healthInfo.batteryCycleCount && this.batteryFullCharge == healthInfo.batteryFullCharge && this.batteryChargeCounter == healthInfo.batteryChargeCounter && HidlSupport.deepEquals(this.batteryTechnology, healthInfo.batteryTechnology)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.chargerAcOnline))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.chargerUsbOnline))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.chargerWirelessOnline))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxChargingCurrent))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxChargingVoltage))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.batteryStatus))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.batteryHealth))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.batteryPresent))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.batteryLevel))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.batteryVoltage))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.batteryTemperature))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.batteryCurrent))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.batteryCycleCount))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.batteryFullCharge))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.batteryChargeCounter))), Integer.valueOf(HidlSupport.deepHashCode(this.batteryTechnology)));
    }

    public final String toString() {
        return "{.chargerAcOnline = " + this.chargerAcOnline + ", .chargerUsbOnline = " + this.chargerUsbOnline + ", .chargerWirelessOnline = " + this.chargerWirelessOnline + ", .maxChargingCurrent = " + this.maxChargingCurrent + ", .maxChargingVoltage = " + this.maxChargingVoltage + ", .batteryStatus = " + BatteryStatus.toString(this.batteryStatus) + ", .batteryHealth = " + BatteryHealth.toString(this.batteryHealth) + ", .batteryPresent = " + this.batteryPresent + ", .batteryLevel = " + this.batteryLevel + ", .batteryVoltage = " + this.batteryVoltage + ", .batteryTemperature = " + this.batteryTemperature + ", .batteryCurrent = " + this.batteryCurrent + ", .batteryCycleCount = " + this.batteryCycleCount + ", .batteryFullCharge = " + this.batteryFullCharge + ", .batteryChargeCounter = " + this.batteryChargeCounter + ", .batteryTechnology = " + this.batteryTechnology + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(72L), 0L);
    }

    public static final ArrayList<HealthInfo> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<HealthInfo> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 72, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            HealthInfo healthInfo = new HealthInfo();
            healthInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 72);
            arrayList.add(healthInfo);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.chargerAcOnline = hwBlob.getBool(j + 0);
        this.chargerUsbOnline = hwBlob.getBool(1 + j);
        this.chargerWirelessOnline = hwBlob.getBool(2 + j);
        this.maxChargingCurrent = hwBlob.getInt32(4 + j);
        this.maxChargingVoltage = hwBlob.getInt32(8 + j);
        this.batteryStatus = hwBlob.getInt32(12 + j);
        this.batteryHealth = hwBlob.getInt32(16 + j);
        this.batteryPresent = hwBlob.getBool(20 + j);
        this.batteryLevel = hwBlob.getInt32(24 + j);
        this.batteryVoltage = hwBlob.getInt32(28 + j);
        this.batteryTemperature = hwBlob.getInt32(32 + j);
        this.batteryCurrent = hwBlob.getInt32(36 + j);
        this.batteryCycleCount = hwBlob.getInt32(40 + j);
        this.batteryFullCharge = hwBlob.getInt32(44 + j);
        this.batteryChargeCounter = hwBlob.getInt32(48 + j);
        long j2 = j + 56;
        this.batteryTechnology = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.batteryTechnology.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(72);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<HealthInfo> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 72);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 72);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putBool(0 + j, this.chargerAcOnline);
        hwBlob.putBool(1 + j, this.chargerUsbOnline);
        hwBlob.putBool(2 + j, this.chargerWirelessOnline);
        hwBlob.putInt32(4 + j, this.maxChargingCurrent);
        hwBlob.putInt32(8 + j, this.maxChargingVoltage);
        hwBlob.putInt32(12 + j, this.batteryStatus);
        hwBlob.putInt32(16 + j, this.batteryHealth);
        hwBlob.putBool(20 + j, this.batteryPresent);
        hwBlob.putInt32(24 + j, this.batteryLevel);
        hwBlob.putInt32(28 + j, this.batteryVoltage);
        hwBlob.putInt32(32 + j, this.batteryTemperature);
        hwBlob.putInt32(36 + j, this.batteryCurrent);
        hwBlob.putInt32(40 + j, this.batteryCycleCount);
        hwBlob.putInt32(44 + j, this.batteryFullCharge);
        hwBlob.putInt32(48 + j, this.batteryChargeCounter);
        hwBlob.putString(j + 56, this.batteryTechnology);
    }
}
