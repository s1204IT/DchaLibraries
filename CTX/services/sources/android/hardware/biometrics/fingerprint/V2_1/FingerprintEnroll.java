package android.hardware.biometrics.fingerprint.V2_1;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class FingerprintEnroll {
    public final FingerprintFingerId finger = new FingerprintFingerId();
    public long msg;
    public int samplesRemaining;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != FingerprintEnroll.class) {
            return false;
        }
        FingerprintEnroll fingerprintEnroll = (FingerprintEnroll) obj;
        if (HidlSupport.deepEquals(this.finger, fingerprintEnroll.finger) && this.samplesRemaining == fingerprintEnroll.samplesRemaining && this.msg == fingerprintEnroll.msg) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.finger)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.samplesRemaining))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.msg))));
    }

    public final String toString() {
        return "{.finger = " + this.finger + ", .samplesRemaining = " + this.samplesRemaining + ", .msg = " + this.msg + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
    }

    public static final ArrayList<FingerprintEnroll> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<FingerprintEnroll> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            FingerprintEnroll fingerprintEnroll = new FingerprintEnroll();
            fingerprintEnroll.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
            arrayList.add(fingerprintEnroll);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.finger.readEmbeddedFromParcel(hwParcel, hwBlob, 0 + j);
        this.samplesRemaining = hwBlob.getInt32(8 + j);
        this.msg = hwBlob.getInt64(j + 16);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(24);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<FingerprintEnroll> arrayList) {
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
        this.finger.writeEmbeddedToBlob(hwBlob, 0 + j);
        hwBlob.putInt32(8 + j, this.samplesRemaining);
        hwBlob.putInt64(j + 16, this.msg);
    }
}
