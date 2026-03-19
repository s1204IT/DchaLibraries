package android.hardware.tetheroffload.control.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class IPv4AddrPortPair {
    public String addr = new String();
    public short port;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != IPv4AddrPortPair.class) {
            return false;
        }
        IPv4AddrPortPair iPv4AddrPortPair = (IPv4AddrPortPair) obj;
        if (HidlSupport.deepEquals(this.addr, iPv4AddrPortPair.addr) && this.port == iPv4AddrPortPair.port) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.addr)), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.port))));
    }

    public final String toString() {
        return "{.addr = " + this.addr + ", .port = " + ((int) this.port) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
    }

    public static final ArrayList<IPv4AddrPortPair> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<IPv4AddrPortPair> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            IPv4AddrPortPair iPv4AddrPortPair = new IPv4AddrPortPair();
            iPv4AddrPortPair.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
            arrayList.add(iPv4AddrPortPair);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        long j2 = j + 0;
        this.addr = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.addr.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.port = hwBlob.getInt16(j + 16);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(24);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<IPv4AddrPortPair> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 24);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 24);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putString(0 + j, this.addr);
        hwBlob.putInt16(j + 16, this.port);
    }
}
