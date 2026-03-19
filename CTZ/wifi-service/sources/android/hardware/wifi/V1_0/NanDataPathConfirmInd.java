package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class NanDataPathConfirmInd {
    public boolean dataPathSetupSuccess;
    public int ndpInstanceId;
    public final byte[] peerNdiMacAddr = new byte[6];
    public final ArrayList<Byte> appInfo = new ArrayList<>();
    public final WifiNanStatus status = new WifiNanStatus();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanDataPathConfirmInd.class) {
            return false;
        }
        NanDataPathConfirmInd nanDataPathConfirmInd = (NanDataPathConfirmInd) obj;
        if (this.ndpInstanceId == nanDataPathConfirmInd.ndpInstanceId && this.dataPathSetupSuccess == nanDataPathConfirmInd.dataPathSetupSuccess && HidlSupport.deepEquals(this.peerNdiMacAddr, nanDataPathConfirmInd.peerNdiMacAddr) && HidlSupport.deepEquals(this.appInfo, nanDataPathConfirmInd.appInfo) && HidlSupport.deepEquals(this.status, nanDataPathConfirmInd.status)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.ndpInstanceId))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.dataPathSetupSuccess))), Integer.valueOf(HidlSupport.deepHashCode(this.peerNdiMacAddr)), Integer.valueOf(HidlSupport.deepHashCode(this.appInfo)), Integer.valueOf(HidlSupport.deepHashCode(this.status)));
    }

    public final String toString() {
        return "{.ndpInstanceId = " + this.ndpInstanceId + ", .dataPathSetupSuccess = " + this.dataPathSetupSuccess + ", .peerNdiMacAddr = " + Arrays.toString(this.peerNdiMacAddr) + ", .appInfo = " + this.appInfo + ", .status = " + this.status + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(56L), 0L);
    }

    public static final ArrayList<NanDataPathConfirmInd> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanDataPathConfirmInd> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 56, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanDataPathConfirmInd nanDataPathConfirmInd = new NanDataPathConfirmInd();
            nanDataPathConfirmInd.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 56);
            arrayList.add(nanDataPathConfirmInd);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.ndpInstanceId = hwBlob.getInt32(j + 0);
        this.dataPathSetupSuccess = hwBlob.getBool(j + 4);
        hwBlob.copyToInt8Array(j + 5, this.peerNdiMacAddr, 6);
        long j2 = j + 16;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
        this.appInfo.clear();
        for (int i = 0; i < int32; i++) {
            this.appInfo.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
        this.status.readEmbeddedFromParcel(hwParcel, hwBlob, j + 32);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(56);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanDataPathConfirmInd> arrayList) {
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
        hwBlob.putInt32(j + 0, this.ndpInstanceId);
        hwBlob.putBool(4 + j, this.dataPathSetupSuccess);
        hwBlob.putInt8Array(5 + j, this.peerNdiMacAddr);
        int size = this.appInfo.size();
        long j2 = 16 + j;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 1);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8(i * 1, this.appInfo.get(i).byteValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        this.status.writeEmbeddedToBlob(hwBlob, j + 32);
    }
}
