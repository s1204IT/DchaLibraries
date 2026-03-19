package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class RttLcrInformation {
    public final byte[] countryCode = new byte[2];
    public String civicInfo = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != RttLcrInformation.class) {
            return false;
        }
        RttLcrInformation rttLcrInformation = (RttLcrInformation) obj;
        if (HidlSupport.deepEquals(this.countryCode, rttLcrInformation.countryCode) && HidlSupport.deepEquals(this.civicInfo, rttLcrInformation.civicInfo)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.countryCode)), Integer.valueOf(HidlSupport.deepHashCode(this.civicInfo)));
    }

    public final String toString() {
        return "{.countryCode = " + Arrays.toString(this.countryCode) + ", .civicInfo = " + this.civicInfo + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
    }

    public static final ArrayList<RttLcrInformation> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<RttLcrInformation> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            RttLcrInformation rttLcrInformation = new RttLcrInformation();
            rttLcrInformation.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
            arrayList.add(rttLcrInformation);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        hwBlob.copyToInt8Array(j + 0, this.countryCode, 2);
        long j2 = j + 8;
        this.civicInfo = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.civicInfo.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(24);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<RttLcrInformation> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 24);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 24);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt8Array(0 + j, this.countryCode);
        hwBlob.putString(j + 8, this.civicInfo);
    }
}
