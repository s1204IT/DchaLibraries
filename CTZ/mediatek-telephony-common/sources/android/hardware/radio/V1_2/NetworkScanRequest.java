package android.hardware.radio.V1_2;

import android.hardware.radio.V1_1.RadioAccessSpecifier;
import android.hardware.radio.V1_1.ScanType;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class NetworkScanRequest {
    public boolean incrementalResults;
    public int incrementalResultsPeriodicity;
    public int interval;
    public int maxSearchTime;
    public int type;
    public final ArrayList<RadioAccessSpecifier> specifiers = new ArrayList<>();
    public final ArrayList<String> mccMncs = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NetworkScanRequest.class) {
            return false;
        }
        NetworkScanRequest networkScanRequest = (NetworkScanRequest) obj;
        if (this.type == networkScanRequest.type && this.interval == networkScanRequest.interval && HidlSupport.deepEquals(this.specifiers, networkScanRequest.specifiers) && this.maxSearchTime == networkScanRequest.maxSearchTime && this.incrementalResults == networkScanRequest.incrementalResults && this.incrementalResultsPeriodicity == networkScanRequest.incrementalResultsPeriodicity && HidlSupport.deepEquals(this.mccMncs, networkScanRequest.mccMncs)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.interval))), Integer.valueOf(HidlSupport.deepHashCode(this.specifiers)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxSearchTime))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.incrementalResults))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.incrementalResultsPeriodicity))), Integer.valueOf(HidlSupport.deepHashCode(this.mccMncs)));
    }

    public final String toString() {
        return "{.type = " + ScanType.toString(this.type) + ", .interval = " + this.interval + ", .specifiers = " + this.specifiers + ", .maxSearchTime = " + this.maxSearchTime + ", .incrementalResults = " + this.incrementalResults + ", .incrementalResultsPeriodicity = " + this.incrementalResultsPeriodicity + ", .mccMncs = " + this.mccMncs + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(56L), 0L);
    }

    public static final ArrayList<NetworkScanRequest> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NetworkScanRequest> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 56, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NetworkScanRequest networkScanRequest = new NetworkScanRequest();
            networkScanRequest.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 56);
            arrayList.add(networkScanRequest);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.type = hwBlob.getInt32(j + 0);
        this.interval = hwBlob.getInt32(j + 4);
        long j2 = j + 8;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 72, hwBlob.handle(), j2 + 0, true);
        this.specifiers.clear();
        for (int i = 0; i < int32; i++) {
            RadioAccessSpecifier radioAccessSpecifier = new RadioAccessSpecifier();
            radioAccessSpecifier.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 72);
            this.specifiers.add(radioAccessSpecifier);
        }
        this.maxSearchTime = hwBlob.getInt32(j + 24);
        this.incrementalResults = hwBlob.getBool(j + 28);
        this.incrementalResultsPeriodicity = hwBlob.getInt32(j + 32);
        long j3 = j + 40;
        int int322 = hwBlob.getInt32(8 + j3);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 16, hwBlob.handle(), j3 + 0, true);
        this.mccMncs.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            new String();
            String string = embeddedBuffer2.getString(i2 * 16);
            hwParcel.readEmbeddedBuffer(string.getBytes().length + 1, embeddedBuffer2.handle(), r3 + 0, false);
            this.mccMncs.add(string);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(56);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NetworkScanRequest> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 56);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 56);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.type);
        hwBlob.putInt32(j + 4, this.interval);
        int size = this.specifiers.size();
        long j2 = j + 8;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 72);
        for (int i = 0; i < size; i++) {
            this.specifiers.get(i).writeEmbeddedToBlob(hwBlob2, i * 72);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        hwBlob.putInt32(j + 24, this.maxSearchTime);
        hwBlob.putBool(j + 28, this.incrementalResults);
        hwBlob.putInt32(j + 32, this.incrementalResultsPeriodicity);
        int size2 = this.mccMncs.size();
        long j3 = j + 40;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 16);
        for (int i2 = 0; i2 < size2; i2++) {
            hwBlob3.putString(i2 * 16, this.mccMncs.get(i2));
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
    }
}
