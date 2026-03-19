package android.hardware.radio.V1_1;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class KeepaliveRequest {
    public int cid;
    public int destinationPort;
    public int maxKeepaliveIntervalMillis;
    public int sourcePort;
    public int type;
    public final ArrayList<Byte> sourceAddress = new ArrayList<>();
    public final ArrayList<Byte> destinationAddress = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != KeepaliveRequest.class) {
            return false;
        }
        KeepaliveRequest keepaliveRequest = (KeepaliveRequest) obj;
        if (this.type == keepaliveRequest.type && HidlSupport.deepEquals(this.sourceAddress, keepaliveRequest.sourceAddress) && this.sourcePort == keepaliveRequest.sourcePort && HidlSupport.deepEquals(this.destinationAddress, keepaliveRequest.destinationAddress) && this.destinationPort == keepaliveRequest.destinationPort && this.maxKeepaliveIntervalMillis == keepaliveRequest.maxKeepaliveIntervalMillis && this.cid == keepaliveRequest.cid) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(this.sourceAddress)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.sourcePort))), Integer.valueOf(HidlSupport.deepHashCode(this.destinationAddress)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.destinationPort))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxKeepaliveIntervalMillis))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cid))));
    }

    public final String toString() {
        return "{.type = " + KeepaliveType.toString(this.type) + ", .sourceAddress = " + this.sourceAddress + ", .sourcePort = " + this.sourcePort + ", .destinationAddress = " + this.destinationAddress + ", .destinationPort = " + this.destinationPort + ", .maxKeepaliveIntervalMillis = " + this.maxKeepaliveIntervalMillis + ", .cid = " + this.cid + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(64L), 0L);
    }

    public static final ArrayList<KeepaliveRequest> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<KeepaliveRequest> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 64, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            KeepaliveRequest keepaliveRequest = new KeepaliveRequest();
            keepaliveRequest.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 64);
            arrayList.add(keepaliveRequest);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.type = hwBlob.getInt32(j + 0);
        long j2 = j + 8;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
        this.sourceAddress.clear();
        for (int i = 0; i < int32; i++) {
            this.sourceAddress.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
        this.sourcePort = hwBlob.getInt32(j + 24);
        long j3 = j + 32;
        int int322 = hwBlob.getInt32(8 + j3);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 1, hwBlob.handle(), j3 + 0, true);
        this.destinationAddress.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            this.destinationAddress.add(Byte.valueOf(embeddedBuffer2.getInt8(i2 * 1)));
        }
        this.destinationPort = hwBlob.getInt32(j + 48);
        this.maxKeepaliveIntervalMillis = hwBlob.getInt32(j + 52);
        this.cid = hwBlob.getInt32(j + 56);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(64);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<KeepaliveRequest> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 64);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 64);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.type);
        int size = this.sourceAddress.size();
        long j2 = j + 8;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 1);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8(i * 1, this.sourceAddress.get(i).byteValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        hwBlob.putInt32(j + 24, this.sourcePort);
        int size2 = this.destinationAddress.size();
        long j3 = j + 32;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 1);
        for (int i2 = 0; i2 < size2; i2++) {
            hwBlob3.putInt8(i2 * 1, this.destinationAddress.get(i2).byteValue());
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
        hwBlob.putInt32(j + 48, this.destinationPort);
        hwBlob.putInt32(j + 52, this.maxKeepaliveIntervalMillis);
        hwBlob.putInt32(j + 56, this.cid);
    }
}
