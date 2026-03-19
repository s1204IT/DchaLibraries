package android.hardware.contexthub.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class NanoAppBinary {
    public long appId;
    public int appVersion;
    public final ArrayList<Byte> customBinary = new ArrayList<>();
    public int flags;
    public byte targetChreApiMajorVersion;
    public byte targetChreApiMinorVersion;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanoAppBinary.class) {
            return false;
        }
        NanoAppBinary nanoAppBinary = (NanoAppBinary) obj;
        if (this.appId == nanoAppBinary.appId && this.appVersion == nanoAppBinary.appVersion && HidlSupport.deepEquals(Integer.valueOf(this.flags), Integer.valueOf(nanoAppBinary.flags)) && this.targetChreApiMajorVersion == nanoAppBinary.targetChreApiMajorVersion && this.targetChreApiMinorVersion == nanoAppBinary.targetChreApiMinorVersion && HidlSupport.deepEquals(this.customBinary, nanoAppBinary.customBinary)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.appId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.appVersion))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.flags))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.targetChreApiMajorVersion))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.targetChreApiMinorVersion))), Integer.valueOf(HidlSupport.deepHashCode(this.customBinary)));
    }

    public final String toString() {
        return "{.appId = " + this.appId + ", .appVersion = " + this.appVersion + ", .flags = " + NanoAppFlags.dumpBitfield(this.flags) + ", .targetChreApiMajorVersion = " + ((int) this.targetChreApiMajorVersion) + ", .targetChreApiMinorVersion = " + ((int) this.targetChreApiMinorVersion) + ", .customBinary = " + this.customBinary + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(40L), 0L);
    }

    public static final ArrayList<NanoAppBinary> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanoAppBinary> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 40, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanoAppBinary nanoAppBinary = new NanoAppBinary();
            nanoAppBinary.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 40);
            arrayList.add(nanoAppBinary);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.appId = hwBlob.getInt64(j + 0);
        this.appVersion = hwBlob.getInt32(j + 8);
        this.flags = hwBlob.getInt32(j + 12);
        this.targetChreApiMajorVersion = hwBlob.getInt8(j + 16);
        this.targetChreApiMinorVersion = hwBlob.getInt8(j + 17);
        long j2 = j + 24;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
        this.customBinary.clear();
        for (int i = 0; i < int32; i++) {
            this.customBinary.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(40);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanoAppBinary> arrayList) {
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
        hwBlob.putInt64(j + 0, this.appId);
        hwBlob.putInt32(j + 8, this.appVersion);
        hwBlob.putInt32(j + 12, this.flags);
        hwBlob.putInt8(16 + j, this.targetChreApiMajorVersion);
        hwBlob.putInt8(17 + j, this.targetChreApiMinorVersion);
        int size = this.customBinary.size();
        long j2 = j + 24;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 1);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8(i * 1, this.customBinary.get(i).byteValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
