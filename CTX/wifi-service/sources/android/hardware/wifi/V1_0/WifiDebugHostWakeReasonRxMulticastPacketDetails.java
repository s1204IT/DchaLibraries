package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class WifiDebugHostWakeReasonRxMulticastPacketDetails {
    public int ipv4RxMulticastAddrCnt;
    public int ipv6RxMulticastAddrCnt;
    public int otherRxMulticastAddrCnt;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != WifiDebugHostWakeReasonRxMulticastPacketDetails.class) {
            return false;
        }
        WifiDebugHostWakeReasonRxMulticastPacketDetails wifiDebugHostWakeReasonRxMulticastPacketDetails = (WifiDebugHostWakeReasonRxMulticastPacketDetails) obj;
        if (this.ipv4RxMulticastAddrCnt == wifiDebugHostWakeReasonRxMulticastPacketDetails.ipv4RxMulticastAddrCnt && this.ipv6RxMulticastAddrCnt == wifiDebugHostWakeReasonRxMulticastPacketDetails.ipv6RxMulticastAddrCnt && this.otherRxMulticastAddrCnt == wifiDebugHostWakeReasonRxMulticastPacketDetails.otherRxMulticastAddrCnt) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.ipv4RxMulticastAddrCnt))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.ipv6RxMulticastAddrCnt))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.otherRxMulticastAddrCnt))));
    }

    public final String toString() {
        return "{.ipv4RxMulticastAddrCnt = " + this.ipv4RxMulticastAddrCnt + ", .ipv6RxMulticastAddrCnt = " + this.ipv6RxMulticastAddrCnt + ", .otherRxMulticastAddrCnt = " + this.otherRxMulticastAddrCnt + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(12L), 0L);
    }

    public static final ArrayList<WifiDebugHostWakeReasonRxMulticastPacketDetails> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<WifiDebugHostWakeReasonRxMulticastPacketDetails> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 12, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            WifiDebugHostWakeReasonRxMulticastPacketDetails wifiDebugHostWakeReasonRxMulticastPacketDetails = new WifiDebugHostWakeReasonRxMulticastPacketDetails();
            wifiDebugHostWakeReasonRxMulticastPacketDetails.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 12);
            arrayList.add(wifiDebugHostWakeReasonRxMulticastPacketDetails);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.ipv4RxMulticastAddrCnt = hwBlob.getInt32(0 + j);
        this.ipv6RxMulticastAddrCnt = hwBlob.getInt32(4 + j);
        this.otherRxMulticastAddrCnt = hwBlob.getInt32(j + 8);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(12);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<WifiDebugHostWakeReasonRxMulticastPacketDetails> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 12);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 12);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.ipv4RxMulticastAddrCnt);
        hwBlob.putInt32(4 + j, this.ipv6RxMulticastAddrCnt);
        hwBlob.putInt32(j + 8, this.otherRxMulticastAddrCnt);
    }
}
