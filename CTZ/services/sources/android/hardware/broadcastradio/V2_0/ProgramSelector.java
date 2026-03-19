package android.hardware.broadcastradio.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class ProgramSelector {
    public final ProgramIdentifier primaryId = new ProgramIdentifier();
    public final ArrayList<ProgramIdentifier> secondaryIds = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != ProgramSelector.class) {
            return false;
        }
        ProgramSelector programSelector = (ProgramSelector) obj;
        if (HidlSupport.deepEquals(this.primaryId, programSelector.primaryId) && HidlSupport.deepEquals(this.secondaryIds, programSelector.secondaryIds)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.primaryId)), Integer.valueOf(HidlSupport.deepHashCode(this.secondaryIds)));
    }

    public final String toString() {
        return "{.primaryId = " + this.primaryId + ", .secondaryIds = " + this.secondaryIds + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(32L), 0L);
    }

    public static final ArrayList<ProgramSelector> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<ProgramSelector> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            ProgramSelector programSelector = new ProgramSelector();
            programSelector.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
            arrayList.add(programSelector);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.primaryId.readEmbeddedFromParcel(hwParcel, hwBlob, j + 0);
        long j2 = j + 16;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 16, hwBlob.handle(), j2 + 0, true);
        this.secondaryIds.clear();
        for (int i = 0; i < int32; i++) {
            ProgramIdentifier programIdentifier = new ProgramIdentifier();
            programIdentifier.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 16);
            this.secondaryIds.add(programIdentifier);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(32);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<ProgramSelector> arrayList) {
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
        this.primaryId.writeEmbeddedToBlob(hwBlob, j + 0);
        int size = this.secondaryIds.size();
        long j2 = j + 16;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 16);
        for (int i = 0; i < size; i++) {
            this.secondaryIds.get(i).writeEmbeddedToBlob(hwBlob2, i * 16);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
