package android.hardware.wifi.V1_2;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class NanDataPathScheduleUpdateInd {
    public final byte[] peerDiscoveryAddress = new byte[6];
    public final ArrayList<NanDataPathChannelInfo> channelInfo = new ArrayList<>();
    public final ArrayList<Integer> ndpInstanceIds = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanDataPathScheduleUpdateInd.class) {
            return false;
        }
        NanDataPathScheduleUpdateInd nanDataPathScheduleUpdateInd = (NanDataPathScheduleUpdateInd) obj;
        if (HidlSupport.deepEquals(this.peerDiscoveryAddress, nanDataPathScheduleUpdateInd.peerDiscoveryAddress) && HidlSupport.deepEquals(this.channelInfo, nanDataPathScheduleUpdateInd.channelInfo) && HidlSupport.deepEquals(this.ndpInstanceIds, nanDataPathScheduleUpdateInd.ndpInstanceIds)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.peerDiscoveryAddress)), Integer.valueOf(HidlSupport.deepHashCode(this.channelInfo)), Integer.valueOf(HidlSupport.deepHashCode(this.ndpInstanceIds)));
    }

    public final String toString() {
        return "{.peerDiscoveryAddress = " + Arrays.toString(this.peerDiscoveryAddress) + ", .channelInfo = " + this.channelInfo + ", .ndpInstanceIds = " + this.ndpInstanceIds + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(40L), 0L);
    }

    public static final ArrayList<NanDataPathScheduleUpdateInd> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanDataPathScheduleUpdateInd> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 40, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanDataPathScheduleUpdateInd nanDataPathScheduleUpdateInd = new NanDataPathScheduleUpdateInd();
            nanDataPathScheduleUpdateInd.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 40);
            arrayList.add(nanDataPathScheduleUpdateInd);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        hwBlob.copyToInt8Array(j + 0, this.peerDiscoveryAddress, 6);
        long j2 = j + 8;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 12, hwBlob.handle(), j2 + 0, true);
        this.channelInfo.clear();
        for (int i = 0; i < int32; i++) {
            NanDataPathChannelInfo nanDataPathChannelInfo = new NanDataPathChannelInfo();
            nanDataPathChannelInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 12);
            this.channelInfo.add(nanDataPathChannelInfo);
        }
        long j3 = j + 24;
        int int322 = hwBlob.getInt32(8 + j3);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 4, hwBlob.handle(), j3 + 0, true);
        this.ndpInstanceIds.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            this.ndpInstanceIds.add(Integer.valueOf(embeddedBuffer2.getInt32(i2 * 4)));
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(40);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanDataPathScheduleUpdateInd> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 40);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 40);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt8Array(j + 0, this.peerDiscoveryAddress);
        int size = this.channelInfo.size();
        long j2 = j + 8;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 12);
        for (int i = 0; i < size; i++) {
            this.channelInfo.get(i).writeEmbeddedToBlob(hwBlob2, i * 12);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        int size2 = this.ndpInstanceIds.size();
        long j3 = j + 24;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 4);
        for (int i2 = 0; i2 < size2; i2++) {
            hwBlob3.putInt32(i2 * 4, this.ndpInstanceIds.get(i2).intValue());
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
    }
}
