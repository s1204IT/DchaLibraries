package android.hardware.radio.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class StkCcUnsolSsResult {
    public int requestType;
    public int result;
    public int serviceClass;
    public int serviceType;
    public int teleserviceType;
    public final ArrayList<SsInfoData> ssInfo = new ArrayList<>();
    public final ArrayList<CfData> cfData = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != StkCcUnsolSsResult.class) {
            return false;
        }
        StkCcUnsolSsResult stkCcUnsolSsResult = (StkCcUnsolSsResult) obj;
        if (this.serviceType == stkCcUnsolSsResult.serviceType && this.requestType == stkCcUnsolSsResult.requestType && this.teleserviceType == stkCcUnsolSsResult.teleserviceType && HidlSupport.deepEquals(Integer.valueOf(this.serviceClass), Integer.valueOf(stkCcUnsolSsResult.serviceClass)) && this.result == stkCcUnsolSsResult.result && HidlSupport.deepEquals(this.ssInfo, stkCcUnsolSsResult.ssInfo) && HidlSupport.deepEquals(this.cfData, stkCcUnsolSsResult.cfData)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.serviceType))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.requestType))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.teleserviceType))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.serviceClass))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.result))), Integer.valueOf(HidlSupport.deepHashCode(this.ssInfo)), Integer.valueOf(HidlSupport.deepHashCode(this.cfData)));
    }

    public final String toString() {
        return "{.serviceType = " + SsServiceType.toString(this.serviceType) + ", .requestType = " + SsRequestType.toString(this.requestType) + ", .teleserviceType = " + SsTeleserviceType.toString(this.teleserviceType) + ", .serviceClass = " + SuppServiceClass.dumpBitfield(this.serviceClass) + ", .result = " + RadioError.toString(this.result) + ", .ssInfo = " + this.ssInfo + ", .cfData = " + this.cfData + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(56L), 0L);
    }

    public static final ArrayList<StkCcUnsolSsResult> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<StkCcUnsolSsResult> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 56, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            StkCcUnsolSsResult stkCcUnsolSsResult = new StkCcUnsolSsResult();
            stkCcUnsolSsResult.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 56);
            arrayList.add(stkCcUnsolSsResult);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.serviceType = hwBlob.getInt32(j + 0);
        this.requestType = hwBlob.getInt32(j + 4);
        this.teleserviceType = hwBlob.getInt32(j + 8);
        this.serviceClass = hwBlob.getInt32(j + 12);
        this.result = hwBlob.getInt32(j + 16);
        long j2 = j + 24;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 16, hwBlob.handle(), j2 + 0, true);
        this.ssInfo.clear();
        for (int i = 0; i < int32; i++) {
            SsInfoData ssInfoData = new SsInfoData();
            ssInfoData.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 16);
            this.ssInfo.add(ssInfoData);
        }
        long j3 = j + 40;
        int int322 = hwBlob.getInt32(j3 + 8);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 16, hwBlob.handle(), 0 + j3, true);
        this.cfData.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            CfData cfData = new CfData();
            cfData.readEmbeddedFromParcel(hwParcel, embeddedBuffer2, i2 * 16);
            this.cfData.add(cfData);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(56);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<StkCcUnsolSsResult> arrayList) {
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
        hwBlob.putInt32(j + 0, this.serviceType);
        hwBlob.putInt32(j + 4, this.requestType);
        hwBlob.putInt32(j + 8, this.teleserviceType);
        hwBlob.putInt32(j + 12, this.serviceClass);
        hwBlob.putInt32(j + 16, this.result);
        int size = this.ssInfo.size();
        long j2 = j + 24;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 16);
        for (int i = 0; i < size; i++) {
            this.ssInfo.get(i).writeEmbeddedToBlob(hwBlob2, i * 16);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        int size2 = this.cfData.size();
        long j3 = j + 40;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(12 + j3, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 16);
        for (int i2 = 0; i2 < size2; i2++) {
            this.cfData.get(i2).writeEmbeddedToBlob(hwBlob3, i2 * 16);
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
    }
}
