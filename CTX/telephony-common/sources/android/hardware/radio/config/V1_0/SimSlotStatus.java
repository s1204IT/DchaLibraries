package android.hardware.radio.config.V1_0;

import android.hardware.radio.V1_0.CardState;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class SimSlotStatus {
    public int cardState;
    public int logicalSlotId;
    public int slotState;
    public String atr = new String();
    public String iccid = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != SimSlotStatus.class) {
            return false;
        }
        SimSlotStatus simSlotStatus = (SimSlotStatus) obj;
        if (this.cardState == simSlotStatus.cardState && this.slotState == simSlotStatus.slotState && HidlSupport.deepEquals(this.atr, simSlotStatus.atr) && this.logicalSlotId == simSlotStatus.logicalSlotId && HidlSupport.deepEquals(this.iccid, simSlotStatus.iccid)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cardState))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.slotState))), Integer.valueOf(HidlSupport.deepHashCode(this.atr)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.logicalSlotId))), Integer.valueOf(HidlSupport.deepHashCode(this.iccid)));
    }

    public final String toString() {
        return "{.cardState = " + CardState.toString(this.cardState) + ", .slotState = " + SlotState.toString(this.slotState) + ", .atr = " + this.atr + ", .logicalSlotId = " + this.logicalSlotId + ", .iccid = " + this.iccid + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(48L), 0L);
    }

    public static final ArrayList<SimSlotStatus> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<SimSlotStatus> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 48, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            SimSlotStatus simSlotStatus = new SimSlotStatus();
            simSlotStatus.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 48);
            arrayList.add(simSlotStatus);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.cardState = hwBlob.getInt32(j + 0);
        this.slotState = hwBlob.getInt32(j + 4);
        long j2 = j + 8;
        this.atr = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.atr.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.logicalSlotId = hwBlob.getInt32(j + 24);
        long j3 = j + 32;
        this.iccid = hwBlob.getString(j3);
        hwParcel.readEmbeddedBuffer(this.iccid.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(48);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<SimSlotStatus> arrayList) {
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
        hwBlob.putInt32(0 + j, this.cardState);
        hwBlob.putInt32(4 + j, this.slotState);
        hwBlob.putString(8 + j, this.atr);
        hwBlob.putInt32(24 + j, this.logicalSlotId);
        hwBlob.putString(j + 32, this.iccid);
    }
}
