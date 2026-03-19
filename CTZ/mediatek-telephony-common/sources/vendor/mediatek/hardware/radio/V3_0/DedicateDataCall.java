package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class DedicateDataCall {
    public int active;
    public int bearerId;
    public int cid;
    public int ddcId;
    public int failCause;
    public int hasPcscf;
    public int hasQos;
    public int hasTft;
    public int interfaceId;
    public int primaryCid;
    public int signalingFlag;
    public final Qos qos = new Qos();
    public final Tft tft = new Tft();
    public String pcscf = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != DedicateDataCall.class) {
            return false;
        }
        DedicateDataCall dedicateDataCall = (DedicateDataCall) obj;
        if (this.ddcId == dedicateDataCall.ddcId && this.interfaceId == dedicateDataCall.interfaceId && this.primaryCid == dedicateDataCall.primaryCid && this.cid == dedicateDataCall.cid && this.active == dedicateDataCall.active && this.signalingFlag == dedicateDataCall.signalingFlag && this.bearerId == dedicateDataCall.bearerId && this.failCause == dedicateDataCall.failCause && this.hasQos == dedicateDataCall.hasQos && HidlSupport.deepEquals(this.qos, dedicateDataCall.qos) && this.hasTft == dedicateDataCall.hasTft && HidlSupport.deepEquals(this.tft, dedicateDataCall.tft) && this.hasPcscf == dedicateDataCall.hasPcscf && HidlSupport.deepEquals(this.pcscf, dedicateDataCall.pcscf)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.ddcId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.interfaceId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.primaryCid))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cid))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.active))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.signalingFlag))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.bearerId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.failCause))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.hasQos))), Integer.valueOf(HidlSupport.deepHashCode(this.qos)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.hasTft))), Integer.valueOf(HidlSupport.deepHashCode(this.tft)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.hasPcscf))), Integer.valueOf(HidlSupport.deepHashCode(this.pcscf)));
    }

    public final String toString() {
        return "{.ddcId = " + this.ddcId + ", .interfaceId = " + this.interfaceId + ", .primaryCid = " + this.primaryCid + ", .cid = " + this.cid + ", .active = " + this.active + ", .signalingFlag = " + this.signalingFlag + ", .bearerId = " + this.bearerId + ", .failCause = " + this.failCause + ", .hasQos = " + this.hasQos + ", .qos = " + this.qos + ", .hasTft = " + this.hasTft + ", .tft = " + this.tft + ", .hasPcscf = " + this.hasPcscf + ", .pcscf = " + this.pcscf + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(136L), 0L);
    }

    public static final ArrayList<DedicateDataCall> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<DedicateDataCall> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 136, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            DedicateDataCall dedicateDataCall = new DedicateDataCall();
            dedicateDataCall.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 136);
            arrayList.add(dedicateDataCall);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.ddcId = hwBlob.getInt32(j + 0);
        this.interfaceId = hwBlob.getInt32(4 + j);
        this.primaryCid = hwBlob.getInt32(8 + j);
        this.cid = hwBlob.getInt32(12 + j);
        this.active = hwBlob.getInt32(16 + j);
        this.signalingFlag = hwBlob.getInt32(20 + j);
        this.bearerId = hwBlob.getInt32(24 + j);
        this.failCause = hwBlob.getInt32(28 + j);
        this.hasQos = hwBlob.getInt32(32 + j);
        this.qos.readEmbeddedFromParcel(hwParcel, hwBlob, 36 + j);
        this.hasTft = hwBlob.getInt32(56 + j);
        this.tft.readEmbeddedFromParcel(hwParcel, hwBlob, 64 + j);
        this.hasPcscf = hwBlob.getInt32(112 + j);
        long j2 = j + 120;
        this.pcscf = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.pcscf.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(136);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<DedicateDataCall> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 136);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 136);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.ddcId);
        hwBlob.putInt32(4 + j, this.interfaceId);
        hwBlob.putInt32(8 + j, this.primaryCid);
        hwBlob.putInt32(12 + j, this.cid);
        hwBlob.putInt32(16 + j, this.active);
        hwBlob.putInt32(20 + j, this.signalingFlag);
        hwBlob.putInt32(24 + j, this.bearerId);
        hwBlob.putInt32(28 + j, this.failCause);
        hwBlob.putInt32(32 + j, this.hasQos);
        this.qos.writeEmbeddedToBlob(hwBlob, 36 + j);
        hwBlob.putInt32(56 + j, this.hasTft);
        this.tft.writeEmbeddedToBlob(hwBlob, 64 + j);
        hwBlob.putInt32(112 + j, this.hasPcscf);
        hwBlob.putString(j + 120, this.pcscf);
    }
}
