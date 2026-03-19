package android.hardware.radio.V1_1;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class NetworkScanRequest {
    public int interval;
    public final ArrayList<RadioAccessSpecifier> specifiers = new ArrayList<>();
    public int type;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NetworkScanRequest.class) {
            return false;
        }
        NetworkScanRequest networkScanRequest = (NetworkScanRequest) obj;
        if (this.type == networkScanRequest.type && this.interval == networkScanRequest.interval && HidlSupport.deepEquals(this.specifiers, networkScanRequest.specifiers)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.interval))), Integer.valueOf(HidlSupport.deepHashCode(this.specifiers)));
    }

    public final String toString() {
        return "{.type = " + ScanType.toString(this.type) + ", .interval = " + this.interval + ", .specifiers = " + this.specifiers + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(24L), 0L);
    }

    public static final ArrayList<NetworkScanRequest> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NetworkScanRequest> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NetworkScanRequest networkScanRequest = new NetworkScanRequest();
            networkScanRequest.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
            arrayList.add(networkScanRequest);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.type = hwBlob.getInt32(j + 0);
        this.interval = hwBlob.getInt32(j + 4);
        long j2 = j + 8;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 72, hwBlob.handle(), j2 + 0, true);
        this.specifiers.clear();
        for (int i = 0; i < int32; i++) {
            RadioAccessSpecifier radioAccessSpecifier = new RadioAccessSpecifier();
            radioAccessSpecifier.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 72);
            this.specifiers.add(radioAccessSpecifier);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(24);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NetworkScanRequest> arrayList) {
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
        hwBlob.putInt32(j + 0, this.type);
        hwBlob.putInt32(4 + j, this.interval);
        int size = this.specifiers.size();
        long j2 = j + 8;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 72);
        for (int i = 0; i < size; i++) {
            this.specifiers.get(i).writeEmbeddedToBlob(hwBlob2, i * 72);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
