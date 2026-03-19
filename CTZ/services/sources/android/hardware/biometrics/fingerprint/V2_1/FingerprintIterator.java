package android.hardware.biometrics.fingerprint.V2_1;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class FingerprintIterator {
    public final FingerprintFingerId finger = new FingerprintFingerId();
    public int remainingTemplates;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != FingerprintIterator.class) {
            return false;
        }
        FingerprintIterator fingerprintIterator = (FingerprintIterator) obj;
        if (HidlSupport.deepEquals(this.finger, fingerprintIterator.finger) && this.remainingTemplates == fingerprintIterator.remainingTemplates) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.finger)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.remainingTemplates))));
    }

    public final String toString() {
        return "{.finger = " + this.finger + ", .remainingTemplates = " + this.remainingTemplates + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(12L), 0L);
    }

    public static final ArrayList<FingerprintIterator> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<FingerprintIterator> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 12, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            FingerprintIterator fingerprintIterator = new FingerprintIterator();
            fingerprintIterator.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 12);
            arrayList.add(fingerprintIterator);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.finger.readEmbeddedFromParcel(hwParcel, hwBlob, 0 + j);
        this.remainingTemplates = hwBlob.getInt32(j + 8);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(12);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<FingerprintIterator> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 12);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 12);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        this.finger.writeEmbeddedToBlob(hwBlob, 0 + j);
        hwBlob.putInt32(j + 8, this.remainingTemplates);
    }
}
