package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class WifiDebugPacketFateFrameInfo {
    public long driverTimestampUsec;
    public long firmwareTimestampUsec;
    public final ArrayList<Byte> frameContent = new ArrayList<>();
    public long frameLen;
    public int frameType;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != WifiDebugPacketFateFrameInfo.class) {
            return false;
        }
        WifiDebugPacketFateFrameInfo wifiDebugPacketFateFrameInfo = (WifiDebugPacketFateFrameInfo) obj;
        if (this.frameType == wifiDebugPacketFateFrameInfo.frameType && this.frameLen == wifiDebugPacketFateFrameInfo.frameLen && this.driverTimestampUsec == wifiDebugPacketFateFrameInfo.driverTimestampUsec && this.firmwareTimestampUsec == wifiDebugPacketFateFrameInfo.firmwareTimestampUsec && HidlSupport.deepEquals(this.frameContent, wifiDebugPacketFateFrameInfo.frameContent)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.frameType))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.frameLen))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.driverTimestampUsec))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.firmwareTimestampUsec))), Integer.valueOf(HidlSupport.deepHashCode(this.frameContent)));
    }

    public final String toString() {
        return "{.frameType = " + WifiDebugPacketFateFrameType.toString(this.frameType) + ", .frameLen = " + this.frameLen + ", .driverTimestampUsec = " + this.driverTimestampUsec + ", .firmwareTimestampUsec = " + this.firmwareTimestampUsec + ", .frameContent = " + this.frameContent + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(48L), 0L);
    }

    public static final ArrayList<WifiDebugPacketFateFrameInfo> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<WifiDebugPacketFateFrameInfo> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 48, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            WifiDebugPacketFateFrameInfo wifiDebugPacketFateFrameInfo = new WifiDebugPacketFateFrameInfo();
            wifiDebugPacketFateFrameInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 48);
            arrayList.add(wifiDebugPacketFateFrameInfo);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.frameType = hwBlob.getInt32(j + 0);
        this.frameLen = hwBlob.getInt64(j + 8);
        this.driverTimestampUsec = hwBlob.getInt64(j + 16);
        this.firmwareTimestampUsec = hwBlob.getInt64(j + 24);
        long j2 = j + 32;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
        this.frameContent.clear();
        for (int i = 0; i < int32; i++) {
            this.frameContent.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(48);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<WifiDebugPacketFateFrameInfo> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 48);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 48);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.frameType);
        hwBlob.putInt64(j + 8, this.frameLen);
        hwBlob.putInt64(16 + j, this.driverTimestampUsec);
        hwBlob.putInt64(24 + j, this.firmwareTimestampUsec);
        int size = this.frameContent.size();
        long j2 = j + 32;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 1);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8(i * 1, this.frameContent.get(i).byteValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
