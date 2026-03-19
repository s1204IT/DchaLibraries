package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class StaScanResult {
    public short beaconPeriodInMs;
    public short capability;
    public int frequency;
    public int rssi;
    public long timeStampInUs;
    public final ArrayList<Byte> ssid = new ArrayList<>();
    public final byte[] bssid = new byte[6];
    public final ArrayList<WifiInformationElement> informationElements = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != StaScanResult.class) {
            return false;
        }
        StaScanResult staScanResult = (StaScanResult) obj;
        if (this.timeStampInUs == staScanResult.timeStampInUs && HidlSupport.deepEquals(this.ssid, staScanResult.ssid) && HidlSupport.deepEquals(this.bssid, staScanResult.bssid) && this.rssi == staScanResult.rssi && this.frequency == staScanResult.frequency && this.beaconPeriodInMs == staScanResult.beaconPeriodInMs && this.capability == staScanResult.capability && HidlSupport.deepEquals(this.informationElements, staScanResult.informationElements)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.timeStampInUs))), Integer.valueOf(HidlSupport.deepHashCode(this.ssid)), Integer.valueOf(HidlSupport.deepHashCode(this.bssid)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.rssi))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.frequency))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.beaconPeriodInMs))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.capability))), Integer.valueOf(HidlSupport.deepHashCode(this.informationElements)));
    }

    public final String toString() {
        return "{.timeStampInUs = " + this.timeStampInUs + ", .ssid = " + this.ssid + ", .bssid = " + Arrays.toString(this.bssid) + ", .rssi = " + this.rssi + ", .frequency = " + this.frequency + ", .beaconPeriodInMs = " + ((int) this.beaconPeriodInMs) + ", .capability = " + ((int) this.capability) + ", .informationElements = " + this.informationElements + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(64L), 0L);
    }

    public static final ArrayList<StaScanResult> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<StaScanResult> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 64, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            StaScanResult staScanResult = new StaScanResult();
            staScanResult.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 64);
            arrayList.add(staScanResult);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.timeStampInUs = hwBlob.getInt64(j + 0);
        long j2 = j + 8;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
        this.ssid.clear();
        for (int i = 0; i < int32; i++) {
            this.ssid.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
        hwBlob.copyToInt8Array(j + 24, this.bssid, 6);
        this.rssi = hwBlob.getInt32(j + 32);
        this.frequency = hwBlob.getInt32(j + 36);
        this.beaconPeriodInMs = hwBlob.getInt16(j + 40);
        this.capability = hwBlob.getInt16(j + 42);
        long j3 = j + 48;
        int int322 = hwBlob.getInt32(8 + j3);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 24, hwBlob.handle(), j3 + 0, true);
        this.informationElements.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            WifiInformationElement wifiInformationElement = new WifiInformationElement();
            wifiInformationElement.readEmbeddedFromParcel(hwParcel, embeddedBuffer2, i2 * 24);
            this.informationElements.add(wifiInformationElement);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(64);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<StaScanResult> arrayList) {
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
        hwBlob.putInt64(j + 0, this.timeStampInUs);
        int size = this.ssid.size();
        long j2 = j + 8;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 1);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8(i * 1, this.ssid.get(i).byteValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        hwBlob.putInt8Array(j + 24, this.bssid);
        hwBlob.putInt32(j + 32, this.rssi);
        hwBlob.putInt32(j + 36, this.frequency);
        hwBlob.putInt16(j + 40, this.beaconPeriodInMs);
        hwBlob.putInt16(j + 42, this.capability);
        int size2 = this.informationElements.size();
        long j3 = j + 48;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 24);
        for (int i2 = 0; i2 < size2; i2++) {
            this.informationElements.get(i2).writeEmbeddedToBlob(hwBlob3, i2 * 24);
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
    }
}
