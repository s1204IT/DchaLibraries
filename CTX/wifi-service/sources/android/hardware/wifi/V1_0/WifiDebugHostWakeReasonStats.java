package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class WifiDebugHostWakeReasonStats {
    public int totalCmdEventWakeCnt;
    public int totalDriverFwLocalWakeCnt;
    public int totalRxPacketWakeCnt;
    public final ArrayList<Integer> cmdEventWakeCntPerType = new ArrayList<>();
    public final ArrayList<Integer> driverFwLocalWakeCntPerType = new ArrayList<>();
    public final WifiDebugHostWakeReasonRxPacketDetails rxPktWakeDetails = new WifiDebugHostWakeReasonRxPacketDetails();
    public final WifiDebugHostWakeReasonRxMulticastPacketDetails rxMulticastPkWakeDetails = new WifiDebugHostWakeReasonRxMulticastPacketDetails();
    public final WifiDebugHostWakeReasonRxIcmpPacketDetails rxIcmpPkWakeDetails = new WifiDebugHostWakeReasonRxIcmpPacketDetails();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != WifiDebugHostWakeReasonStats.class) {
            return false;
        }
        WifiDebugHostWakeReasonStats wifiDebugHostWakeReasonStats = (WifiDebugHostWakeReasonStats) obj;
        if (this.totalCmdEventWakeCnt == wifiDebugHostWakeReasonStats.totalCmdEventWakeCnt && HidlSupport.deepEquals(this.cmdEventWakeCntPerType, wifiDebugHostWakeReasonStats.cmdEventWakeCntPerType) && this.totalDriverFwLocalWakeCnt == wifiDebugHostWakeReasonStats.totalDriverFwLocalWakeCnt && HidlSupport.deepEquals(this.driverFwLocalWakeCntPerType, wifiDebugHostWakeReasonStats.driverFwLocalWakeCntPerType) && this.totalRxPacketWakeCnt == wifiDebugHostWakeReasonStats.totalRxPacketWakeCnt && HidlSupport.deepEquals(this.rxPktWakeDetails, wifiDebugHostWakeReasonStats.rxPktWakeDetails) && HidlSupport.deepEquals(this.rxMulticastPkWakeDetails, wifiDebugHostWakeReasonStats.rxMulticastPkWakeDetails) && HidlSupport.deepEquals(this.rxIcmpPkWakeDetails, wifiDebugHostWakeReasonStats.rxIcmpPkWakeDetails)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.totalCmdEventWakeCnt))), Integer.valueOf(HidlSupport.deepHashCode(this.cmdEventWakeCntPerType)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.totalDriverFwLocalWakeCnt))), Integer.valueOf(HidlSupport.deepHashCode(this.driverFwLocalWakeCntPerType)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.totalRxPacketWakeCnt))), Integer.valueOf(HidlSupport.deepHashCode(this.rxPktWakeDetails)), Integer.valueOf(HidlSupport.deepHashCode(this.rxMulticastPkWakeDetails)), Integer.valueOf(HidlSupport.deepHashCode(this.rxIcmpPkWakeDetails)));
    }

    public final String toString() {
        return "{.totalCmdEventWakeCnt = " + this.totalCmdEventWakeCnt + ", .cmdEventWakeCntPerType = " + this.cmdEventWakeCntPerType + ", .totalDriverFwLocalWakeCnt = " + this.totalDriverFwLocalWakeCnt + ", .driverFwLocalWakeCntPerType = " + this.driverFwLocalWakeCntPerType + ", .totalRxPacketWakeCnt = " + this.totalRxPacketWakeCnt + ", .rxPktWakeDetails = " + this.rxPktWakeDetails + ", .rxMulticastPkWakeDetails = " + this.rxMulticastPkWakeDetails + ", .rxIcmpPkWakeDetails = " + this.rxIcmpPkWakeDetails + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(96L), 0L);
    }

    public static final ArrayList<WifiDebugHostWakeReasonStats> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<WifiDebugHostWakeReasonStats> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 96, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            WifiDebugHostWakeReasonStats wifiDebugHostWakeReasonStats = new WifiDebugHostWakeReasonStats();
            wifiDebugHostWakeReasonStats.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 96);
            arrayList.add(wifiDebugHostWakeReasonStats);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.totalCmdEventWakeCnt = hwBlob.getInt32(j + 0);
        long j2 = j + 8;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 4, hwBlob.handle(), j2 + 0, true);
        this.cmdEventWakeCntPerType.clear();
        for (int i = 0; i < int32; i++) {
            this.cmdEventWakeCntPerType.add(Integer.valueOf(embeddedBuffer.getInt32(i * 4)));
        }
        this.totalDriverFwLocalWakeCnt = hwBlob.getInt32(j + 24);
        long j3 = j + 32;
        int int322 = hwBlob.getInt32(j3 + 8);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 4, hwBlob.handle(), 0 + j3, true);
        this.driverFwLocalWakeCntPerType.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            this.driverFwLocalWakeCntPerType.add(Integer.valueOf(embeddedBuffer2.getInt32(i2 * 4)));
        }
        this.totalRxPacketWakeCnt = hwBlob.getInt32(j + 48);
        this.rxPktWakeDetails.readEmbeddedFromParcel(hwParcel, hwBlob, j + 52);
        this.rxMulticastPkWakeDetails.readEmbeddedFromParcel(hwParcel, hwBlob, j + 64);
        this.rxIcmpPkWakeDetails.readEmbeddedFromParcel(hwParcel, hwBlob, j + 76);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(96);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<WifiDebugHostWakeReasonStats> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 96);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 96);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.totalCmdEventWakeCnt);
        int size = this.cmdEventWakeCntPerType.size();
        long j2 = j + 8;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 4);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt32(i * 4, this.cmdEventWakeCntPerType.get(i).intValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        hwBlob.putInt32(j + 24, this.totalDriverFwLocalWakeCnt);
        int size2 = this.driverFwLocalWakeCntPerType.size();
        long j3 = j + 32;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 4);
        for (int i2 = 0; i2 < size2; i2++) {
            hwBlob3.putInt32(i2 * 4, this.driverFwLocalWakeCntPerType.get(i2).intValue());
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
        hwBlob.putInt32(j + 48, this.totalRxPacketWakeCnt);
        this.rxPktWakeDetails.writeEmbeddedToBlob(hwBlob, j + 52);
        this.rxMulticastPkWakeDetails.writeEmbeddedToBlob(hwBlob, j + 64);
        this.rxIcmpPkWakeDetails.writeEmbeddedToBlob(hwBlob, j + 76);
    }
}
