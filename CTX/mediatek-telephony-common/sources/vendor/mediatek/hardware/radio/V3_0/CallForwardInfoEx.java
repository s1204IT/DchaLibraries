package vendor.mediatek.hardware.radio.V3_0;

import android.hardware.radio.V1_0.CallForwardInfoStatus;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class CallForwardInfoEx {
    public int reason;
    public int serviceClass;
    public int status;
    public int timeSeconds;
    public int toa;
    public String number = new String();
    public String timeSlotBegin = new String();
    public String timeSlotEnd = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != CallForwardInfoEx.class) {
            return false;
        }
        CallForwardInfoEx callForwardInfoEx = (CallForwardInfoEx) obj;
        if (this.status == callForwardInfoEx.status && this.reason == callForwardInfoEx.reason && this.serviceClass == callForwardInfoEx.serviceClass && this.toa == callForwardInfoEx.toa && HidlSupport.deepEquals(this.number, callForwardInfoEx.number) && this.timeSeconds == callForwardInfoEx.timeSeconds && HidlSupport.deepEquals(this.timeSlotBegin, callForwardInfoEx.timeSlotBegin) && HidlSupport.deepEquals(this.timeSlotEnd, callForwardInfoEx.timeSlotEnd)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.status))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.reason))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.serviceClass))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.toa))), Integer.valueOf(HidlSupport.deepHashCode(this.number)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.timeSeconds))), Integer.valueOf(HidlSupport.deepHashCode(this.timeSlotBegin)), Integer.valueOf(HidlSupport.deepHashCode(this.timeSlotEnd)));
    }

    public final String toString() {
        return "{.status = " + CallForwardInfoStatus.toString(this.status) + ", .reason = " + this.reason + ", .serviceClass = " + this.serviceClass + ", .toa = " + this.toa + ", .number = " + this.number + ", .timeSeconds = " + this.timeSeconds + ", .timeSlotBegin = " + this.timeSlotBegin + ", .timeSlotEnd = " + this.timeSlotEnd + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(72L), 0L);
    }

    public static final ArrayList<CallForwardInfoEx> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<CallForwardInfoEx> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 72, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            CallForwardInfoEx callForwardInfoEx = new CallForwardInfoEx();
            callForwardInfoEx.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 72);
            arrayList.add(callForwardInfoEx);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.status = hwBlob.getInt32(j + 0);
        this.reason = hwBlob.getInt32(j + 4);
        this.serviceClass = hwBlob.getInt32(j + 8);
        this.toa = hwBlob.getInt32(j + 12);
        long j2 = j + 16;
        this.number = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.number.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.timeSeconds = hwBlob.getInt32(j + 32);
        long j3 = j + 40;
        this.timeSlotBegin = hwBlob.getString(j3);
        hwParcel.readEmbeddedBuffer(this.timeSlotBegin.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
        long j4 = j + 56;
        this.timeSlotEnd = hwBlob.getString(j4);
        hwParcel.readEmbeddedBuffer(this.timeSlotEnd.getBytes().length + 1, hwBlob.handle(), j4 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(72);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<CallForwardInfoEx> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 72);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 72);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.status);
        hwBlob.putInt32(4 + j, this.reason);
        hwBlob.putInt32(8 + j, this.serviceClass);
        hwBlob.putInt32(12 + j, this.toa);
        hwBlob.putString(16 + j, this.number);
        hwBlob.putInt32(32 + j, this.timeSeconds);
        hwBlob.putString(40 + j, this.timeSlotBegin);
        hwBlob.putString(j + 56, this.timeSlotEnd);
    }
}
