package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class StaApfPacketFilterCapabilities {
    public int maxLength;
    public int version;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != StaApfPacketFilterCapabilities.class) {
            return false;
        }
        StaApfPacketFilterCapabilities staApfPacketFilterCapabilities = (StaApfPacketFilterCapabilities) obj;
        if (this.version == staApfPacketFilterCapabilities.version && this.maxLength == staApfPacketFilterCapabilities.maxLength) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.version))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxLength))));
    }

    public final String toString() {
        return "{.version = " + this.version + ", .maxLength = " + this.maxLength + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(8L), 0L);
    }

    public static final ArrayList<StaApfPacketFilterCapabilities> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<StaApfPacketFilterCapabilities> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 8, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            StaApfPacketFilterCapabilities staApfPacketFilterCapabilities = new StaApfPacketFilterCapabilities();
            staApfPacketFilterCapabilities.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 8);
            arrayList.add(staApfPacketFilterCapabilities);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.version = hwBlob.getInt32(0 + j);
        this.maxLength = hwBlob.getInt32(j + 4);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(8);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<StaApfPacketFilterCapabilities> arrayList) {
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
        hwBlob.putInt32(0 + j, this.version);
        hwBlob.putInt32(j + 4, this.maxLength);
    }
}
