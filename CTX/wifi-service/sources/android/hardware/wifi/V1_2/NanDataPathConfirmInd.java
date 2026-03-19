package android.hardware.wifi.V1_2;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class NanDataPathConfirmInd {
    public final android.hardware.wifi.V1_0.NanDataPathConfirmInd V1_0 = new android.hardware.wifi.V1_0.NanDataPathConfirmInd();
    public final ArrayList<NanDataPathChannelInfo> channelInfo = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanDataPathConfirmInd.class) {
            return false;
        }
        NanDataPathConfirmInd nanDataPathConfirmInd = (NanDataPathConfirmInd) obj;
        if (HidlSupport.deepEquals(this.V1_0, nanDataPathConfirmInd.V1_0) && HidlSupport.deepEquals(this.channelInfo, nanDataPathConfirmInd.channelInfo)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.V1_0)), Integer.valueOf(HidlSupport.deepHashCode(this.channelInfo)));
    }

    public final String toString() {
        return "{.V1_0 = " + this.V1_0 + ", .channelInfo = " + this.channelInfo + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(72L), 0L);
    }

    public static final ArrayList<NanDataPathConfirmInd> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanDataPathConfirmInd> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 72, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanDataPathConfirmInd nanDataPathConfirmInd = new NanDataPathConfirmInd();
            nanDataPathConfirmInd.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 72);
            arrayList.add(nanDataPathConfirmInd);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.V1_0.readEmbeddedFromParcel(hwParcel, hwBlob, j + 0);
        long j2 = j + 56;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 12, hwBlob.handle(), j2 + 0, true);
        this.channelInfo.clear();
        for (int i = 0; i < int32; i++) {
            NanDataPathChannelInfo nanDataPathChannelInfo = new NanDataPathChannelInfo();
            nanDataPathChannelInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 12);
            this.channelInfo.add(nanDataPathChannelInfo);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(72);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanDataPathConfirmInd> arrayList) {
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
        this.V1_0.writeEmbeddedToBlob(hwBlob, j + 0);
        int size = this.channelInfo.size();
        long j2 = j + 56;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 12);
        for (int i = 0; i < size; i++) {
            this.channelInfo.get(i).writeEmbeddedToBlob(hwBlob2, i * 12);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
