package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class SmsParams {
    public int dcs;
    public int format;
    public int pid;
    public int vp;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != SmsParams.class) {
            return false;
        }
        SmsParams smsParams = (SmsParams) obj;
        if (this.format == smsParams.format && this.vp == smsParams.vp && this.pid == smsParams.pid && this.dcs == smsParams.dcs) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.format))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.vp))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.pid))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.dcs))));
    }

    public final String toString() {
        return "{.format = " + this.format + ", .vp = " + this.vp + ", .pid = " + this.pid + ", .dcs = " + this.dcs + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(16L), 0L);
    }

    public static final ArrayList<SmsParams> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<SmsParams> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 16, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            SmsParams smsParams = new SmsParams();
            smsParams.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 16);
            arrayList.add(smsParams);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.format = hwBlob.getInt32(0 + j);
        this.vp = hwBlob.getInt32(4 + j);
        this.pid = hwBlob.getInt32(8 + j);
        this.dcs = hwBlob.getInt32(j + 12);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(16);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<SmsParams> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 16);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 16);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.format);
        hwBlob.putInt32(4 + j, this.vp);
        hwBlob.putInt32(8 + j, this.pid);
        hwBlob.putInt32(j + 12, this.dcs);
    }
}
