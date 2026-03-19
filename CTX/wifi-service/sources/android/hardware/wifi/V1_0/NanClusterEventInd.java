package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class NanClusterEventInd {
    public final byte[] addr = new byte[6];
    public int eventType;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanClusterEventInd.class) {
            return false;
        }
        NanClusterEventInd nanClusterEventInd = (NanClusterEventInd) obj;
        if (this.eventType == nanClusterEventInd.eventType && HidlSupport.deepEquals(this.addr, nanClusterEventInd.addr)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.eventType))), Integer.valueOf(HidlSupport.deepHashCode(this.addr)));
    }

    public final String toString() {
        return "{.eventType = " + NanClusterEventType.toString(this.eventType) + ", .addr = " + Arrays.toString(this.addr) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(12L), 0L);
    }

    public static final ArrayList<NanClusterEventInd> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanClusterEventInd> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 12, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanClusterEventInd nanClusterEventInd = new NanClusterEventInd();
            nanClusterEventInd.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 12);
            arrayList.add(nanClusterEventInd);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.eventType = hwBlob.getInt32(0 + j);
        hwBlob.copyToInt8Array(j + 4, this.addr, 6);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(12);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanClusterEventInd> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 12);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 12);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.eventType);
        hwBlob.putInt8Array(j + 4, this.addr);
    }
}
