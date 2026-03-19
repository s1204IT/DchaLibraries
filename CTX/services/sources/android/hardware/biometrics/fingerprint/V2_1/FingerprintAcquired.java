package android.hardware.biometrics.fingerprint.V2_1;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class FingerprintAcquired {
    public int acquiredInfo;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && obj.getClass() == FingerprintAcquired.class && this.acquiredInfo == ((FingerprintAcquired) obj).acquiredInfo) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.acquiredInfo))));
    }

    public final String toString() {
        return "{.acquiredInfo = " + FingerprintAcquiredInfo.toString(this.acquiredInfo) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(4L), 0L);
    }

    public static final ArrayList<FingerprintAcquired> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<FingerprintAcquired> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 4, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            FingerprintAcquired fingerprintAcquired = new FingerprintAcquired();
            fingerprintAcquired.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 4);
            arrayList.add(fingerprintAcquired);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.acquiredInfo = hwBlob.getInt32(j + 0);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(4);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<FingerprintAcquired> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 4);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 4);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.acquiredInfo);
    }
}
