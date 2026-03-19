package android.hardware.radio.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class PcoDataInfo {
    public int cid;
    public int pcoId;
    public String bearerProto = new String();
    public final ArrayList<Byte> contents = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != PcoDataInfo.class) {
            return false;
        }
        PcoDataInfo pcoDataInfo = (PcoDataInfo) obj;
        if (this.cid == pcoDataInfo.cid && HidlSupport.deepEquals(this.bearerProto, pcoDataInfo.bearerProto) && this.pcoId == pcoDataInfo.pcoId && HidlSupport.deepEquals(this.contents, pcoDataInfo.contents)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cid))), Integer.valueOf(HidlSupport.deepHashCode(this.bearerProto)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.pcoId))), Integer.valueOf(HidlSupport.deepHashCode(this.contents)));
    }

    public final String toString() {
        return "{.cid = " + this.cid + ", .bearerProto = " + this.bearerProto + ", .pcoId = " + this.pcoId + ", .contents = " + this.contents + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(48L), 0L);
    }

    public static final ArrayList<PcoDataInfo> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<PcoDataInfo> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 48, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            PcoDataInfo pcoDataInfo = new PcoDataInfo();
            pcoDataInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 48);
            arrayList.add(pcoDataInfo);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.cid = hwBlob.getInt32(j + 0);
        long j2 = j + 8;
        this.bearerProto = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.bearerProto.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.pcoId = hwBlob.getInt32(j + 24);
        long j3 = j + 32;
        int int32 = hwBlob.getInt32(8 + j3);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j3 + 0, true);
        this.contents.clear();
        for (int i = 0; i < int32; i++) {
            this.contents.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(48);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<PcoDataInfo> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 48);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 48);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.cid);
        hwBlob.putString(j + 8, this.bearerProto);
        hwBlob.putInt32(24 + j, this.pcoId);
        int size = this.contents.size();
        long j2 = j + 32;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 1);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8(i * 1, this.contents.get(i).byteValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
