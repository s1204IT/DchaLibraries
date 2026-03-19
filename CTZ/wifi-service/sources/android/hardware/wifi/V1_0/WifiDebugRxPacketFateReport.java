package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class WifiDebugRxPacketFateReport {
    public int fate;
    public final WifiDebugPacketFateFrameInfo frameInfo = new WifiDebugPacketFateFrameInfo();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != WifiDebugRxPacketFateReport.class) {
            return false;
        }
        WifiDebugRxPacketFateReport wifiDebugRxPacketFateReport = (WifiDebugRxPacketFateReport) obj;
        if (this.fate == wifiDebugRxPacketFateReport.fate && HidlSupport.deepEquals(this.frameInfo, wifiDebugRxPacketFateReport.frameInfo)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.fate))), Integer.valueOf(HidlSupport.deepHashCode(this.frameInfo)));
    }

    public final String toString() {
        return "{.fate = " + WifiDebugRxPacketFate.toString(this.fate) + ", .frameInfo = " + this.frameInfo + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(56L), 0L);
    }

    public static final ArrayList<WifiDebugRxPacketFateReport> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<WifiDebugRxPacketFateReport> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 56, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            WifiDebugRxPacketFateReport wifiDebugRxPacketFateReport = new WifiDebugRxPacketFateReport();
            wifiDebugRxPacketFateReport.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 56);
            arrayList.add(wifiDebugRxPacketFateReport);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.fate = hwBlob.getInt32(0 + j);
        this.frameInfo.readEmbeddedFromParcel(hwParcel, hwBlob, j + 8);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(56);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<WifiDebugRxPacketFateReport> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 56);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 56);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.fate);
        this.frameInfo.writeEmbeddedToBlob(hwBlob, j + 8);
    }
}
