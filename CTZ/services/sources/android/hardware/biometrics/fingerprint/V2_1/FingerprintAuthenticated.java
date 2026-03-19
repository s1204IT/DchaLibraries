package android.hardware.biometrics.fingerprint.V2_1;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class FingerprintAuthenticated {
    public final FingerprintFingerId finger = new FingerprintFingerId();
    public final byte[] hat = new byte[69];

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != FingerprintAuthenticated.class) {
            return false;
        }
        FingerprintAuthenticated fingerprintAuthenticated = (FingerprintAuthenticated) obj;
        if (HidlSupport.deepEquals(this.finger, fingerprintAuthenticated.finger) && HidlSupport.deepEquals(this.hat, fingerprintAuthenticated.hat)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.finger)), Integer.valueOf(HidlSupport.deepHashCode(this.hat)));
    }

    public final String toString() {
        return "{.finger = " + this.finger + ", .hat = " + Arrays.toString(this.hat) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(80L), 0L);
    }

    public static final ArrayList<FingerprintAuthenticated> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<FingerprintAuthenticated> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 80, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            FingerprintAuthenticated fingerprintAuthenticated = new FingerprintAuthenticated();
            fingerprintAuthenticated.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 80);
            arrayList.add(fingerprintAuthenticated);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.finger.readEmbeddedFromParcel(hwParcel, hwBlob, 0 + j);
        hwBlob.copyToInt8Array(j + 8, this.hat, 69);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(80);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<FingerprintAuthenticated> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 80);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 80);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        this.finger.writeEmbeddedToBlob(hwBlob, 0 + j);
        hwBlob.putInt8Array(j + 8, this.hat);
    }
}
