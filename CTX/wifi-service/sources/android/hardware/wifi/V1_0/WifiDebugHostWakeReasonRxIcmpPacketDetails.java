package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class WifiDebugHostWakeReasonRxIcmpPacketDetails {
    public int icmp6Na;
    public int icmp6Ns;
    public int icmp6Pkt;
    public int icmp6Ra;
    public int icmpPkt;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != WifiDebugHostWakeReasonRxIcmpPacketDetails.class) {
            return false;
        }
        WifiDebugHostWakeReasonRxIcmpPacketDetails wifiDebugHostWakeReasonRxIcmpPacketDetails = (WifiDebugHostWakeReasonRxIcmpPacketDetails) obj;
        if (this.icmpPkt == wifiDebugHostWakeReasonRxIcmpPacketDetails.icmpPkt && this.icmp6Pkt == wifiDebugHostWakeReasonRxIcmpPacketDetails.icmp6Pkt && this.icmp6Ra == wifiDebugHostWakeReasonRxIcmpPacketDetails.icmp6Ra && this.icmp6Na == wifiDebugHostWakeReasonRxIcmpPacketDetails.icmp6Na && this.icmp6Ns == wifiDebugHostWakeReasonRxIcmpPacketDetails.icmp6Ns) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.icmpPkt))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.icmp6Pkt))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.icmp6Ra))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.icmp6Na))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.icmp6Ns))));
    }

    public final String toString() {
        return "{.icmpPkt = " + this.icmpPkt + ", .icmp6Pkt = " + this.icmp6Pkt + ", .icmp6Ra = " + this.icmp6Ra + ", .icmp6Na = " + this.icmp6Na + ", .icmp6Ns = " + this.icmp6Ns + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(20L), 0L);
    }

    public static final ArrayList<WifiDebugHostWakeReasonRxIcmpPacketDetails> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<WifiDebugHostWakeReasonRxIcmpPacketDetails> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 20, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            WifiDebugHostWakeReasonRxIcmpPacketDetails wifiDebugHostWakeReasonRxIcmpPacketDetails = new WifiDebugHostWakeReasonRxIcmpPacketDetails();
            wifiDebugHostWakeReasonRxIcmpPacketDetails.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 20);
            arrayList.add(wifiDebugHostWakeReasonRxIcmpPacketDetails);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.icmpPkt = hwBlob.getInt32(0 + j);
        this.icmp6Pkt = hwBlob.getInt32(4 + j);
        this.icmp6Ra = hwBlob.getInt32(8 + j);
        this.icmp6Na = hwBlob.getInt32(12 + j);
        this.icmp6Ns = hwBlob.getInt32(j + 16);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(20);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<WifiDebugHostWakeReasonRxIcmpPacketDetails> arrayList) {
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
        hwBlob.putInt32(0 + j, this.icmpPkt);
        hwBlob.putInt32(4 + j, this.icmp6Pkt);
        hwBlob.putInt32(8 + j, this.icmp6Ra);
        hwBlob.putInt32(12 + j, this.icmp6Na);
        hwBlob.putInt32(j + 16, this.icmp6Ns);
    }
}
