package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class NanDataPathRequestInd {
    public byte discoverySessionId;
    public int ndpInstanceId;
    public boolean securityRequired;
    public final byte[] peerDiscMacAddr = new byte[6];
    public final ArrayList<Byte> appInfo = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanDataPathRequestInd.class) {
            return false;
        }
        NanDataPathRequestInd nanDataPathRequestInd = (NanDataPathRequestInd) obj;
        if (this.discoverySessionId == nanDataPathRequestInd.discoverySessionId && HidlSupport.deepEquals(this.peerDiscMacAddr, nanDataPathRequestInd.peerDiscMacAddr) && this.ndpInstanceId == nanDataPathRequestInd.ndpInstanceId && this.securityRequired == nanDataPathRequestInd.securityRequired && HidlSupport.deepEquals(this.appInfo, nanDataPathRequestInd.appInfo)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.discoverySessionId))), Integer.valueOf(HidlSupport.deepHashCode(this.peerDiscMacAddr)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.ndpInstanceId))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.securityRequired))), Integer.valueOf(HidlSupport.deepHashCode(this.appInfo)));
    }

    public final String toString() {
        return "{.discoverySessionId = " + ((int) this.discoverySessionId) + ", .peerDiscMacAddr = " + Arrays.toString(this.peerDiscMacAddr) + ", .ndpInstanceId = " + this.ndpInstanceId + ", .securityRequired = " + this.securityRequired + ", .appInfo = " + this.appInfo + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(32L), 0L);
    }

    public static final ArrayList<NanDataPathRequestInd> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanDataPathRequestInd> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanDataPathRequestInd nanDataPathRequestInd = new NanDataPathRequestInd();
            nanDataPathRequestInd.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
            arrayList.add(nanDataPathRequestInd);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.discoverySessionId = hwBlob.getInt8(j + 0);
        hwBlob.copyToInt8Array(j + 1, this.peerDiscMacAddr, 6);
        this.ndpInstanceId = hwBlob.getInt32(j + 8);
        this.securityRequired = hwBlob.getBool(j + 12);
        long j2 = j + 16;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
        this.appInfo.clear();
        for (int i = 0; i < int32; i++) {
            this.appInfo.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(32);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanDataPathRequestInd> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 32);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 32);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt8(j + 0, this.discoverySessionId);
        hwBlob.putInt8Array(1 + j, this.peerDiscMacAddr);
        hwBlob.putInt32(j + 8, this.ndpInstanceId);
        hwBlob.putBool(j + 12, this.securityRequired);
        int size = this.appInfo.size();
        long j2 = j + 16;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 1);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8(i * 1, this.appInfo.get(i).byteValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
