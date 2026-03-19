package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class Tft {
    public int operation;
    public int pfNumber;
    public final ArrayList<PktFilter> pfList = new ArrayList<>();
    public final TftParameter tftParameter = new TftParameter();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != Tft.class) {
            return false;
        }
        Tft tft = (Tft) obj;
        if (this.operation == tft.operation && this.pfNumber == tft.pfNumber && HidlSupport.deepEquals(this.pfList, tft.pfList) && HidlSupport.deepEquals(this.tftParameter, tft.tftParameter)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.operation))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.pfNumber))), Integer.valueOf(HidlSupport.deepHashCode(this.pfList)), Integer.valueOf(HidlSupport.deepHashCode(this.tftParameter)));
    }

    public final String toString() {
        return "{.operation = " + this.operation + ", .pfNumber = " + this.pfNumber + ", .pfList = " + this.pfList + ", .tftParameter = " + this.tftParameter + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(48L), 0L);
    }

    public static final ArrayList<Tft> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<Tft> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 48, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            Tft tft = new Tft();
            tft.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 48);
            arrayList.add(tft);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.operation = hwBlob.getInt32(j + 0);
        this.pfNumber = hwBlob.getInt32(j + 4);
        long j2 = j + 8;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 96, hwBlob.handle(), j2 + 0, true);
        this.pfList.clear();
        for (int i = 0; i < int32; i++) {
            PktFilter pktFilter = new PktFilter();
            pktFilter.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 96);
            this.pfList.add(pktFilter);
        }
        this.tftParameter.readEmbeddedFromParcel(hwParcel, hwBlob, j + 24);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(48);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<Tft> arrayList) {
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
        hwBlob.putInt32(j + 0, this.operation);
        hwBlob.putInt32(4 + j, this.pfNumber);
        int size = this.pfList.size();
        long j2 = j + 8;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 96);
        for (int i = 0; i < size; i++) {
            this.pfList.get(i).writeEmbeddedToBlob(hwBlob2, i * 96);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        this.tftParameter.writeEmbeddedToBlob(hwBlob, j + 24);
    }
}
