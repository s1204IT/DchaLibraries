package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class WifiRateInfo {
    public int bitRateInKbps;
    public int bw;
    public int nss;
    public int preamble;
    public byte rateMcsIdx;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != WifiRateInfo.class) {
            return false;
        }
        WifiRateInfo wifiRateInfo = (WifiRateInfo) obj;
        if (this.preamble == wifiRateInfo.preamble && this.nss == wifiRateInfo.nss && this.bw == wifiRateInfo.bw && this.rateMcsIdx == wifiRateInfo.rateMcsIdx && this.bitRateInKbps == wifiRateInfo.bitRateInKbps) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.preamble))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.nss))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.bw))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.rateMcsIdx))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.bitRateInKbps))));
    }

    public final String toString() {
        return "{.preamble = " + WifiRatePreamble.toString(this.preamble) + ", .nss = " + WifiRateNss.toString(this.nss) + ", .bw = " + WifiChannelWidthInMhz.toString(this.bw) + ", .rateMcsIdx = " + ((int) this.rateMcsIdx) + ", .bitRateInKbps = " + this.bitRateInKbps + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(20L), 0L);
    }

    public static final ArrayList<WifiRateInfo> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<WifiRateInfo> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 20, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            WifiRateInfo wifiRateInfo = new WifiRateInfo();
            wifiRateInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 20);
            arrayList.add(wifiRateInfo);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.preamble = hwBlob.getInt32(0 + j);
        this.nss = hwBlob.getInt32(4 + j);
        this.bw = hwBlob.getInt32(8 + j);
        this.rateMcsIdx = hwBlob.getInt8(12 + j);
        this.bitRateInKbps = hwBlob.getInt32(j + 16);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(20);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<WifiRateInfo> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 20);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 20);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.preamble);
        hwBlob.putInt32(4 + j, this.nss);
        hwBlob.putInt32(8 + j, this.bw);
        hwBlob.putInt8(12 + j, this.rateMcsIdx);
        hwBlob.putInt32(j + 16, this.bitRateInKbps);
    }
}
