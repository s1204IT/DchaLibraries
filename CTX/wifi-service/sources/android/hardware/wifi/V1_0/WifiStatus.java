package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class WifiStatus {
    public int code;
    public String description = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != WifiStatus.class) {
            return false;
        }
        WifiStatus wifiStatus = (WifiStatus) obj;
        if (this.code == wifiStatus.code && HidlSupport.deepEquals(this.description, wifiStatus.description)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.code))), Integer.valueOf(HidlSupport.deepHashCode(this.description)));
    }

    public final String toString() {
        return "{.code = " + WifiStatusCode.toString(this.code) + ", .description = " + this.description + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
    }

    public static final ArrayList<WifiStatus> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<WifiStatus> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            WifiStatus wifiStatus = new WifiStatus();
            wifiStatus.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
            arrayList.add(wifiStatus);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.code = hwBlob.getInt32(j + 0);
        long j2 = j + 8;
        this.description = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.description.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(24);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<WifiStatus> arrayList) {
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
        hwBlob.putInt32(0 + j, this.code);
        hwBlob.putString(j + 8, this.description);
    }
}
