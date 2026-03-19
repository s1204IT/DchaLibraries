package android.hardware.biometrics.fingerprint.V2_1;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class FingerprintFingerId {
    public int fid;
    public int gid;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != FingerprintFingerId.class) {
            return false;
        }
        FingerprintFingerId fingerprintFingerId = (FingerprintFingerId) obj;
        if (this.gid == fingerprintFingerId.gid && this.fid == fingerprintFingerId.fid) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.gid))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.fid))));
    }

    public final String toString() {
        return "{.gid = " + this.gid + ", .fid = " + this.fid + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(8L), 0L);
    }

    public static final ArrayList<FingerprintFingerId> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<FingerprintFingerId> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 8, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            FingerprintFingerId fingerprintFingerId = new FingerprintFingerId();
            fingerprintFingerId.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 8);
            arrayList.add(fingerprintFingerId);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.gid = hwBlob.getInt32(0 + j);
        this.fid = hwBlob.getInt32(j + 4);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(8);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<FingerprintFingerId> arrayList) {
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
        hwBlob.putInt32(0 + j, this.gid);
        hwBlob.putInt32(j + 4, this.fid);
    }
}
