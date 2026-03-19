package android.hardware.radio.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class CellInfo {
    public int cellInfoType;
    public boolean registered;
    public long timeStamp;
    public int timeStampType;
    public final ArrayList<CellInfoGsm> gsm = new ArrayList<>();
    public final ArrayList<CellInfoCdma> cdma = new ArrayList<>();
    public final ArrayList<CellInfoLte> lte = new ArrayList<>();
    public final ArrayList<CellInfoWcdma> wcdma = new ArrayList<>();
    public final ArrayList<CellInfoTdscdma> tdscdma = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != CellInfo.class) {
            return false;
        }
        CellInfo cellInfo = (CellInfo) obj;
        if (this.cellInfoType == cellInfo.cellInfoType && this.registered == cellInfo.registered && this.timeStampType == cellInfo.timeStampType && this.timeStamp == cellInfo.timeStamp && HidlSupport.deepEquals(this.gsm, cellInfo.gsm) && HidlSupport.deepEquals(this.cdma, cellInfo.cdma) && HidlSupport.deepEquals(this.lte, cellInfo.lte) && HidlSupport.deepEquals(this.wcdma, cellInfo.wcdma) && HidlSupport.deepEquals(this.tdscdma, cellInfo.tdscdma)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cellInfoType))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.registered))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.timeStampType))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.timeStamp))), Integer.valueOf(HidlSupport.deepHashCode(this.gsm)), Integer.valueOf(HidlSupport.deepHashCode(this.cdma)), Integer.valueOf(HidlSupport.deepHashCode(this.lte)), Integer.valueOf(HidlSupport.deepHashCode(this.wcdma)), Integer.valueOf(HidlSupport.deepHashCode(this.tdscdma)));
    }

    public final String toString() {
        return "{.cellInfoType = " + CellInfoType.toString(this.cellInfoType) + ", .registered = " + this.registered + ", .timeStampType = " + TimeStampType.toString(this.timeStampType) + ", .timeStamp = " + this.timeStamp + ", .gsm = " + this.gsm + ", .cdma = " + this.cdma + ", .lte = " + this.lte + ", .wcdma = " + this.wcdma + ", .tdscdma = " + this.tdscdma + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(104L), 0L);
    }

    public static final ArrayList<CellInfo> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<CellInfo> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 104, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            CellInfo cellInfo = new CellInfo();
            cellInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 104);
            arrayList.add(cellInfo);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.cellInfoType = hwBlob.getInt32(j + 0);
        this.registered = hwBlob.getBool(j + 4);
        this.timeStampType = hwBlob.getInt32(j + 8);
        this.timeStamp = hwBlob.getInt64(j + 16);
        long j2 = j + 24;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 64, hwBlob.handle(), j2 + 0, true);
        this.gsm.clear();
        for (int i = 0; i < int32; i++) {
            CellInfoGsm cellInfoGsm = new CellInfoGsm();
            cellInfoGsm.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 64);
            this.gsm.add(cellInfoGsm);
        }
        long j3 = j + 40;
        int int322 = hwBlob.getInt32(j3 + 8);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 40, hwBlob.handle(), j3 + 0, true);
        this.cdma.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            CellInfoCdma cellInfoCdma = new CellInfoCdma();
            cellInfoCdma.readEmbeddedFromParcel(hwParcel, embeddedBuffer2, i2 * 40);
            this.cdma.add(cellInfoCdma);
        }
        long j4 = j + 56;
        int int323 = hwBlob.getInt32(j4 + 8);
        HwBlob embeddedBuffer3 = hwParcel.readEmbeddedBuffer(int323 * 72, hwBlob.handle(), j4 + 0, true);
        this.lte.clear();
        for (int i3 = 0; i3 < int323; i3++) {
            CellInfoLte cellInfoLte = new CellInfoLte();
            cellInfoLte.readEmbeddedFromParcel(hwParcel, embeddedBuffer3, i3 * 72);
            this.lte.add(cellInfoLte);
        }
        long j5 = j + 72;
        int int324 = hwBlob.getInt32(j5 + 8);
        HwBlob embeddedBuffer4 = hwParcel.readEmbeddedBuffer(int324 * 56, hwBlob.handle(), j5 + 0, true);
        this.wcdma.clear();
        for (int i4 = 0; i4 < int324; i4++) {
            CellInfoWcdma cellInfoWcdma = new CellInfoWcdma();
            cellInfoWcdma.readEmbeddedFromParcel(hwParcel, embeddedBuffer4, i4 * 56);
            this.wcdma.add(cellInfoWcdma);
        }
        long j6 = j + 88;
        int int325 = hwBlob.getInt32(j6 + 8);
        HwBlob embeddedBuffer5 = hwParcel.readEmbeddedBuffer(int325 * 56, hwBlob.handle(), 0 + j6, true);
        this.tdscdma.clear();
        for (int i5 = 0; i5 < int325; i5++) {
            CellInfoTdscdma cellInfoTdscdma = new CellInfoTdscdma();
            cellInfoTdscdma.readEmbeddedFromParcel(hwParcel, embeddedBuffer5, i5 * 56);
            this.tdscdma.add(cellInfoTdscdma);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(104);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<CellInfo> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 104);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 104);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.cellInfoType);
        hwBlob.putBool(j + 4, this.registered);
        hwBlob.putInt32(j + 8, this.timeStampType);
        hwBlob.putInt64(j + 16, this.timeStamp);
        int size = this.gsm.size();
        long j2 = j + 24;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 64);
        for (int i = 0; i < size; i++) {
            this.gsm.get(i).writeEmbeddedToBlob(hwBlob2, i * 64);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        int size2 = this.cdma.size();
        long j3 = j + 40;
        hwBlob.putInt32(j3 + 8, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 40);
        for (int i2 = 0; i2 < size2; i2++) {
            this.cdma.get(i2).writeEmbeddedToBlob(hwBlob3, i2 * 40);
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
        int size3 = this.lte.size();
        long j4 = j + 56;
        hwBlob.putInt32(j4 + 8, size3);
        hwBlob.putBool(j4 + 12, false);
        HwBlob hwBlob4 = new HwBlob(size3 * 72);
        for (int i3 = 0; i3 < size3; i3++) {
            this.lte.get(i3).writeEmbeddedToBlob(hwBlob4, i3 * 72);
        }
        hwBlob.putBlob(j4 + 0, hwBlob4);
        int size4 = this.wcdma.size();
        long j5 = j + 72;
        hwBlob.putInt32(j5 + 8, size4);
        hwBlob.putBool(j5 + 12, false);
        HwBlob hwBlob5 = new HwBlob(size4 * 56);
        for (int i4 = 0; i4 < size4; i4++) {
            this.wcdma.get(i4).writeEmbeddedToBlob(hwBlob5, i4 * 56);
        }
        hwBlob.putBlob(j5 + 0, hwBlob5);
        int size5 = this.tdscdma.size();
        long j6 = j + 88;
        hwBlob.putInt32(8 + j6, size5);
        hwBlob.putBool(j6 + 12, false);
        HwBlob hwBlob6 = new HwBlob(size5 * 56);
        for (int i5 = 0; i5 < size5; i5++) {
            this.tdscdma.get(i5).writeEmbeddedToBlob(hwBlob6, i5 * 56);
        }
        hwBlob.putBlob(j6 + 0, hwBlob6);
    }
}
