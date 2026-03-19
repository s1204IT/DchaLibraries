package android.hardware.broadcastradio.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class ProgramFilter {
    public boolean excludeModifications;
    public final ArrayList<Integer> identifierTypes = new ArrayList<>();
    public final ArrayList<ProgramIdentifier> identifiers = new ArrayList<>();
    public boolean includeCategories;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != ProgramFilter.class) {
            return false;
        }
        ProgramFilter programFilter = (ProgramFilter) obj;
        if (HidlSupport.deepEquals(this.identifierTypes, programFilter.identifierTypes) && HidlSupport.deepEquals(this.identifiers, programFilter.identifiers) && this.includeCategories == programFilter.includeCategories && this.excludeModifications == programFilter.excludeModifications) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.identifierTypes)), Integer.valueOf(HidlSupport.deepHashCode(this.identifiers)), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.includeCategories))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.excludeModifications))));
    }

    public final String toString() {
        return "{.identifierTypes = " + this.identifierTypes + ", .identifiers = " + this.identifiers + ", .includeCategories = " + this.includeCategories + ", .excludeModifications = " + this.excludeModifications + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(40L), 0L);
    }

    public static final ArrayList<ProgramFilter> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<ProgramFilter> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 40, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            ProgramFilter programFilter = new ProgramFilter();
            programFilter.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 40);
            arrayList.add(programFilter);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        long j2 = j + 0;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 4, hwBlob.handle(), j2 + 0, true);
        this.identifierTypes.clear();
        for (int i = 0; i < int32; i++) {
            this.identifierTypes.add(Integer.valueOf(embeddedBuffer.getInt32(i * 4)));
        }
        long j3 = j + 16;
        int int322 = hwBlob.getInt32(8 + j3);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 16, hwBlob.handle(), j3 + 0, true);
        this.identifiers.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            ProgramIdentifier programIdentifier = new ProgramIdentifier();
            programIdentifier.readEmbeddedFromParcel(hwParcel, embeddedBuffer2, i2 * 16);
            this.identifiers.add(programIdentifier);
        }
        this.includeCategories = hwBlob.getBool(j + 32);
        this.excludeModifications = hwBlob.getBool(j + 33);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(40);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<ProgramFilter> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 40);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 40);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        int size = this.identifierTypes.size();
        long j2 = j + 0;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 4);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt32(i * 4, this.identifierTypes.get(i).intValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        int size2 = this.identifiers.size();
        long j3 = j + 16;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 16);
        for (int i2 = 0; i2 < size2; i2++) {
            this.identifiers.get(i2).writeEmbeddedToBlob(hwBlob3, i2 * 16);
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
        hwBlob.putBool(j + 32, this.includeCategories);
        hwBlob.putBool(j + 33, this.excludeModifications);
    }
}
