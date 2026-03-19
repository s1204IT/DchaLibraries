package android.hardware.broadcastradio.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class ProgramListChunk {
    public boolean complete;
    public boolean purge;
    public final ArrayList<ProgramInfo> modified = new ArrayList<>();
    public final ArrayList<ProgramIdentifier> removed = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != ProgramListChunk.class) {
            return false;
        }
        ProgramListChunk programListChunk = (ProgramListChunk) obj;
        if (this.purge == programListChunk.purge && this.complete == programListChunk.complete && HidlSupport.deepEquals(this.modified, programListChunk.modified) && HidlSupport.deepEquals(this.removed, programListChunk.removed)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.purge))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.complete))), Integer.valueOf(HidlSupport.deepHashCode(this.modified)), Integer.valueOf(HidlSupport.deepHashCode(this.removed)));
    }

    public final String toString() {
        return "{.purge = " + this.purge + ", .complete = " + this.complete + ", .modified = " + this.modified + ", .removed = " + this.removed + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(40L), 0L);
    }

    public static final ArrayList<ProgramListChunk> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<ProgramListChunk> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 40, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            ProgramListChunk programListChunk = new ProgramListChunk();
            programListChunk.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 40);
            arrayList.add(programListChunk);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.purge = hwBlob.getBool(j + 0);
        this.complete = hwBlob.getBool(j + 1);
        long j2 = j + 8;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 120, hwBlob.handle(), j2 + 0, true);
        this.modified.clear();
        for (int i = 0; i < int32; i++) {
            ProgramInfo programInfo = new ProgramInfo();
            programInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 120);
            this.modified.add(programInfo);
        }
        long j3 = j + 24;
        int int322 = hwBlob.getInt32(j3 + 8);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 16, hwBlob.handle(), 0 + j3, true);
        this.removed.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            ProgramIdentifier programIdentifier = new ProgramIdentifier();
            programIdentifier.readEmbeddedFromParcel(hwParcel, embeddedBuffer2, i2 * 16);
            this.removed.add(programIdentifier);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(40);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<ProgramListChunk> arrayList) {
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
        hwBlob.putBool(j + 0, this.purge);
        hwBlob.putBool(j + 1, this.complete);
        int size = this.modified.size();
        long j2 = j + 8;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 120);
        for (int i = 0; i < size; i++) {
            this.modified.get(i).writeEmbeddedToBlob(hwBlob2, i * 120);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        int size2 = this.removed.size();
        long j3 = j + 24;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 16);
        for (int i2 = 0; i2 < size2; i2++) {
            this.removed.get(i2).writeEmbeddedToBlob(hwBlob3, i2 * 16);
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
    }
}
