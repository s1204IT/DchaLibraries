package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class RttResponder {
    public final WifiChannelInfo channel = new WifiChannelInfo();
    public int preamble;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != RttResponder.class) {
            return false;
        }
        RttResponder rttResponder = (RttResponder) obj;
        if (HidlSupport.deepEquals(this.channel, rttResponder.channel) && this.preamble == rttResponder.preamble) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.channel)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.preamble))));
    }

    public final String toString() {
        return "{.channel = " + this.channel + ", .preamble = " + RttPreamble.toString(this.preamble) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(20L), 0L);
    }

    public static final ArrayList<RttResponder> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<RttResponder> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 20, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            RttResponder rttResponder = new RttResponder();
            rttResponder.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 20);
            arrayList.add(rttResponder);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.channel.readEmbeddedFromParcel(hwParcel, hwBlob, 0 + j);
        this.preamble = hwBlob.getInt32(j + 16);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(20);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<RttResponder> arrayList) {
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
        this.channel.writeEmbeddedToBlob(hwBlob, 0 + j);
        hwBlob.putInt32(j + 16, this.preamble);
    }
}
