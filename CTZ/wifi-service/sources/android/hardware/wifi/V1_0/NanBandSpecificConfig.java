package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class NanBandSpecificConfig {
    public byte discoveryWindowIntervalVal;
    public byte dwellTimeMs;
    public byte rssiClose;
    public byte rssiCloseProximity;
    public byte rssiMiddle;
    public short scanPeriodSec;
    public boolean validDiscoveryWindowIntervalVal;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanBandSpecificConfig.class) {
            return false;
        }
        NanBandSpecificConfig nanBandSpecificConfig = (NanBandSpecificConfig) obj;
        if (this.rssiClose == nanBandSpecificConfig.rssiClose && this.rssiMiddle == nanBandSpecificConfig.rssiMiddle && this.rssiCloseProximity == nanBandSpecificConfig.rssiCloseProximity && this.dwellTimeMs == nanBandSpecificConfig.dwellTimeMs && this.scanPeriodSec == nanBandSpecificConfig.scanPeriodSec && this.validDiscoveryWindowIntervalVal == nanBandSpecificConfig.validDiscoveryWindowIntervalVal && this.discoveryWindowIntervalVal == nanBandSpecificConfig.discoveryWindowIntervalVal) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.rssiClose))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.rssiMiddle))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.rssiCloseProximity))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.dwellTimeMs))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.scanPeriodSec))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.validDiscoveryWindowIntervalVal))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.discoveryWindowIntervalVal))));
    }

    public final String toString() {
        return "{.rssiClose = " + ((int) this.rssiClose) + ", .rssiMiddle = " + ((int) this.rssiMiddle) + ", .rssiCloseProximity = " + ((int) this.rssiCloseProximity) + ", .dwellTimeMs = " + ((int) this.dwellTimeMs) + ", .scanPeriodSec = " + ((int) this.scanPeriodSec) + ", .validDiscoveryWindowIntervalVal = " + this.validDiscoveryWindowIntervalVal + ", .discoveryWindowIntervalVal = " + ((int) this.discoveryWindowIntervalVal) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(8L), 0L);
    }

    public static final ArrayList<NanBandSpecificConfig> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanBandSpecificConfig> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 8, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanBandSpecificConfig nanBandSpecificConfig = new NanBandSpecificConfig();
            nanBandSpecificConfig.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 8);
            arrayList.add(nanBandSpecificConfig);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.rssiClose = hwBlob.getInt8(0 + j);
        this.rssiMiddle = hwBlob.getInt8(1 + j);
        this.rssiCloseProximity = hwBlob.getInt8(2 + j);
        this.dwellTimeMs = hwBlob.getInt8(3 + j);
        this.scanPeriodSec = hwBlob.getInt16(4 + j);
        this.validDiscoveryWindowIntervalVal = hwBlob.getBool(6 + j);
        this.discoveryWindowIntervalVal = hwBlob.getInt8(j + 7);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(8);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanBandSpecificConfig> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 8);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 8);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt8(0 + j, this.rssiClose);
        hwBlob.putInt8(1 + j, this.rssiMiddle);
        hwBlob.putInt8(2 + j, this.rssiCloseProximity);
        hwBlob.putInt8(3 + j, this.dwellTimeMs);
        hwBlob.putInt16(4 + j, this.scanPeriodSec);
        hwBlob.putBool(6 + j, this.validDiscoveryWindowIntervalVal);
        hwBlob.putInt8(j + 7, this.discoveryWindowIntervalVal);
    }
}
