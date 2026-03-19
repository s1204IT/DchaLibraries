package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class WifiChannelInfo {
    public int centerFreq;
    public int centerFreq0;
    public int centerFreq1;
    public int width;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != WifiChannelInfo.class) {
            return false;
        }
        WifiChannelInfo wifiChannelInfo = (WifiChannelInfo) obj;
        if (this.width == wifiChannelInfo.width && this.centerFreq == wifiChannelInfo.centerFreq && this.centerFreq0 == wifiChannelInfo.centerFreq0 && this.centerFreq1 == wifiChannelInfo.centerFreq1) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.width))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.centerFreq))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.centerFreq0))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.centerFreq1))));
    }

    public final String toString() {
        return "{.width = " + WifiChannelWidthInMhz.toString(this.width) + ", .centerFreq = " + this.centerFreq + ", .centerFreq0 = " + this.centerFreq0 + ", .centerFreq1 = " + this.centerFreq1 + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(16L), 0L);
    }

    public static final ArrayList<WifiChannelInfo> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<WifiChannelInfo> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 16, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            WifiChannelInfo wifiChannelInfo = new WifiChannelInfo();
            wifiChannelInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 16);
            arrayList.add(wifiChannelInfo);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.width = hwBlob.getInt32(0 + j);
        this.centerFreq = hwBlob.getInt32(4 + j);
        this.centerFreq0 = hwBlob.getInt32(8 + j);
        this.centerFreq1 = hwBlob.getInt32(j + 12);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(16);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<WifiChannelInfo> arrayList) {
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
        hwBlob.putInt32(0 + j, this.width);
        hwBlob.putInt32(4 + j, this.centerFreq);
        hwBlob.putInt32(8 + j, this.centerFreq0);
        hwBlob.putInt32(j + 12, this.centerFreq1);
    }
}
