package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class RttCapabilities {
    public int bwSupport;
    public boolean lciSupported;
    public boolean lcrSupported;
    public byte mcVersion;
    public int preambleSupport;
    public boolean responderSupported;
    public boolean rttFtmSupported;
    public boolean rttOneSidedSupported;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != RttCapabilities.class) {
            return false;
        }
        RttCapabilities rttCapabilities = (RttCapabilities) obj;
        if (this.rttOneSidedSupported == rttCapabilities.rttOneSidedSupported && this.rttFtmSupported == rttCapabilities.rttFtmSupported && this.lciSupported == rttCapabilities.lciSupported && this.lcrSupported == rttCapabilities.lcrSupported && this.responderSupported == rttCapabilities.responderSupported && HidlSupport.deepEquals(Integer.valueOf(this.preambleSupport), Integer.valueOf(rttCapabilities.preambleSupport)) && HidlSupport.deepEquals(Integer.valueOf(this.bwSupport), Integer.valueOf(rttCapabilities.bwSupport)) && this.mcVersion == rttCapabilities.mcVersion) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.rttOneSidedSupported))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.rttFtmSupported))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.lciSupported))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.lcrSupported))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.responderSupported))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.preambleSupport))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.bwSupport))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.mcVersion))));
    }

    public final String toString() {
        return "{.rttOneSidedSupported = " + this.rttOneSidedSupported + ", .rttFtmSupported = " + this.rttFtmSupported + ", .lciSupported = " + this.lciSupported + ", .lcrSupported = " + this.lcrSupported + ", .responderSupported = " + this.responderSupported + ", .preambleSupport = " + RttPreamble.dumpBitfield(this.preambleSupport) + ", .bwSupport = " + RttBw.dumpBitfield(this.bwSupport) + ", .mcVersion = " + ((int) this.mcVersion) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(20L), 0L);
    }

    public static final ArrayList<RttCapabilities> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<RttCapabilities> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 20, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            RttCapabilities rttCapabilities = new RttCapabilities();
            rttCapabilities.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 20);
            arrayList.add(rttCapabilities);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.rttOneSidedSupported = hwBlob.getBool(0 + j);
        this.rttFtmSupported = hwBlob.getBool(1 + j);
        this.lciSupported = hwBlob.getBool(2 + j);
        this.lcrSupported = hwBlob.getBool(3 + j);
        this.responderSupported = hwBlob.getBool(4 + j);
        this.preambleSupport = hwBlob.getInt32(8 + j);
        this.bwSupport = hwBlob.getInt32(12 + j);
        this.mcVersion = hwBlob.getInt8(j + 16);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(20);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<RttCapabilities> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 20);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 20);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putBool(0 + j, this.rttOneSidedSupported);
        hwBlob.putBool(1 + j, this.rttFtmSupported);
        hwBlob.putBool(2 + j, this.lciSupported);
        hwBlob.putBool(3 + j, this.lcrSupported);
        hwBlob.putBool(4 + j, this.responderSupported);
        hwBlob.putInt32(8 + j, this.preambleSupport);
        hwBlob.putInt32(12 + j, this.bwSupport);
        hwBlob.putInt8(j + 16, this.mcVersion);
    }
}
