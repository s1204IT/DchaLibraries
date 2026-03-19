package android.hardware.broadcastradio.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class ProgramInfo {
    public int infoFlags;
    public int signalQuality;
    public final ProgramSelector selector = new ProgramSelector();
    public final ProgramIdentifier logicallyTunedTo = new ProgramIdentifier();
    public final ProgramIdentifier physicallyTunedTo = new ProgramIdentifier();
    public final ArrayList<ProgramIdentifier> relatedContent = new ArrayList<>();
    public final ArrayList<Metadata> metadata = new ArrayList<>();
    public final ArrayList<VendorKeyValue> vendorInfo = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != ProgramInfo.class) {
            return false;
        }
        ProgramInfo programInfo = (ProgramInfo) obj;
        if (HidlSupport.deepEquals(this.selector, programInfo.selector) && HidlSupport.deepEquals(this.logicallyTunedTo, programInfo.logicallyTunedTo) && HidlSupport.deepEquals(this.physicallyTunedTo, programInfo.physicallyTunedTo) && HidlSupport.deepEquals(this.relatedContent, programInfo.relatedContent) && HidlSupport.deepEquals(Integer.valueOf(this.infoFlags), Integer.valueOf(programInfo.infoFlags)) && this.signalQuality == programInfo.signalQuality && HidlSupport.deepEquals(this.metadata, programInfo.metadata) && HidlSupport.deepEquals(this.vendorInfo, programInfo.vendorInfo)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.selector)), Integer.valueOf(HidlSupport.deepHashCode(this.logicallyTunedTo)), Integer.valueOf(HidlSupport.deepHashCode(this.physicallyTunedTo)), Integer.valueOf(HidlSupport.deepHashCode(this.relatedContent)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.infoFlags))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.signalQuality))), Integer.valueOf(HidlSupport.deepHashCode(this.metadata)), Integer.valueOf(HidlSupport.deepHashCode(this.vendorInfo)));
    }

    public final String toString() {
        return "{.selector = " + this.selector + ", .logicallyTunedTo = " + this.logicallyTunedTo + ", .physicallyTunedTo = " + this.physicallyTunedTo + ", .relatedContent = " + this.relatedContent + ", .infoFlags = " + ProgramInfoFlags.dumpBitfield(this.infoFlags) + ", .signalQuality = " + this.signalQuality + ", .metadata = " + this.metadata + ", .vendorInfo = " + this.vendorInfo + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(120L), 0L);
    }

    public static final ArrayList<ProgramInfo> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<ProgramInfo> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 120, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            ProgramInfo programInfo = new ProgramInfo();
            programInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 120);
            arrayList.add(programInfo);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.selector.readEmbeddedFromParcel(hwParcel, hwBlob, j + 0);
        this.logicallyTunedTo.readEmbeddedFromParcel(hwParcel, hwBlob, j + 32);
        this.physicallyTunedTo.readEmbeddedFromParcel(hwParcel, hwBlob, j + 48);
        long j2 = j + 64;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 16, hwBlob.handle(), j2 + 0, true);
        this.relatedContent.clear();
        for (int i = 0; i < int32; i++) {
            ProgramIdentifier programIdentifier = new ProgramIdentifier();
            programIdentifier.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 16);
            this.relatedContent.add(programIdentifier);
        }
        this.infoFlags = hwBlob.getInt32(j + 80);
        this.signalQuality = hwBlob.getInt32(j + 84);
        long j3 = j + 88;
        int int322 = hwBlob.getInt32(j3 + 8);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 32, hwBlob.handle(), j3 + 0, true);
        this.metadata.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            Metadata metadata = new Metadata();
            metadata.readEmbeddedFromParcel(hwParcel, embeddedBuffer2, i2 * 32);
            this.metadata.add(metadata);
        }
        long j4 = j + 104;
        int int323 = hwBlob.getInt32(j4 + 8);
        HwBlob embeddedBuffer3 = hwParcel.readEmbeddedBuffer(int323 * 32, hwBlob.handle(), 0 + j4, true);
        this.vendorInfo.clear();
        for (int i3 = 0; i3 < int323; i3++) {
            VendorKeyValue vendorKeyValue = new VendorKeyValue();
            vendorKeyValue.readEmbeddedFromParcel(hwParcel, embeddedBuffer3, i3 * 32);
            this.vendorInfo.add(vendorKeyValue);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(120);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<ProgramInfo> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 120);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 120);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        this.selector.writeEmbeddedToBlob(hwBlob, j + 0);
        this.logicallyTunedTo.writeEmbeddedToBlob(hwBlob, j + 32);
        this.physicallyTunedTo.writeEmbeddedToBlob(hwBlob, j + 48);
        int size = this.relatedContent.size();
        long j2 = j + 64;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 16);
        for (int i = 0; i < size; i++) {
            this.relatedContent.get(i).writeEmbeddedToBlob(hwBlob2, i * 16);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        hwBlob.putInt32(j + 80, this.infoFlags);
        hwBlob.putInt32(j + 84, this.signalQuality);
        int size2 = this.metadata.size();
        long j3 = j + 88;
        hwBlob.putInt32(j3 + 8, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 32);
        for (int i2 = 0; i2 < size2; i2++) {
            this.metadata.get(i2).writeEmbeddedToBlob(hwBlob3, i2 * 32);
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
        int size3 = this.vendorInfo.size();
        long j4 = j + 104;
        hwBlob.putInt32(8 + j4, size3);
        hwBlob.putBool(j4 + 12, false);
        HwBlob hwBlob4 = new HwBlob(size3 * 32);
        for (int i3 = 0; i3 < size3; i3++) {
            this.vendorInfo.get(i3).writeEmbeddedToBlob(hwBlob4, i3 * 32);
        }
        hwBlob.putBlob(j4 + 0, hwBlob4);
    }
}
