package android.hardware.broadcastradio.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class AmFmBandRange {
    public int lowerBound;
    public int scanSpacing;
    public int spacing;
    public int upperBound;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != AmFmBandRange.class) {
            return false;
        }
        AmFmBandRange amFmBandRange = (AmFmBandRange) obj;
        if (this.lowerBound == amFmBandRange.lowerBound && this.upperBound == amFmBandRange.upperBound && this.spacing == amFmBandRange.spacing && this.scanSpacing == amFmBandRange.scanSpacing) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.lowerBound))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.upperBound))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.spacing))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.scanSpacing))));
    }

    public final String toString() {
        return "{.lowerBound = " + this.lowerBound + ", .upperBound = " + this.upperBound + ", .spacing = " + this.spacing + ", .scanSpacing = " + this.scanSpacing + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(16L), 0L);
    }

    public static final ArrayList<AmFmBandRange> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<AmFmBandRange> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 16, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            AmFmBandRange amFmBandRange = new AmFmBandRange();
            amFmBandRange.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 16);
            arrayList.add(amFmBandRange);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.lowerBound = hwBlob.getInt32(0 + j);
        this.upperBound = hwBlob.getInt32(4 + j);
        this.spacing = hwBlob.getInt32(8 + j);
        this.scanSpacing = hwBlob.getInt32(j + 12);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(16);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<AmFmBandRange> arrayList) {
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
        hwBlob.putInt32(0 + j, this.lowerBound);
        hwBlob.putInt32(4 + j, this.upperBound);
        hwBlob.putInt32(8 + j, this.spacing);
        hwBlob.putInt32(j + 12, this.scanSpacing);
    }
}
