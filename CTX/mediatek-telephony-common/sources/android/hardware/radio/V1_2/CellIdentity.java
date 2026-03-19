package android.hardware.radio.V1_2;

import android.hardware.radio.V1_0.CellInfoType;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class CellIdentity {
    public int cellInfoType;
    public final ArrayList<CellIdentityGsm> cellIdentityGsm = new ArrayList<>();
    public final ArrayList<CellIdentityWcdma> cellIdentityWcdma = new ArrayList<>();
    public final ArrayList<CellIdentityCdma> cellIdentityCdma = new ArrayList<>();
    public final ArrayList<CellIdentityLte> cellIdentityLte = new ArrayList<>();
    public final ArrayList<CellIdentityTdscdma> cellIdentityTdscdma = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != CellIdentity.class) {
            return false;
        }
        CellIdentity cellIdentity = (CellIdentity) obj;
        if (this.cellInfoType == cellIdentity.cellInfoType && HidlSupport.deepEquals(this.cellIdentityGsm, cellIdentity.cellIdentityGsm) && HidlSupport.deepEquals(this.cellIdentityWcdma, cellIdentity.cellIdentityWcdma) && HidlSupport.deepEquals(this.cellIdentityCdma, cellIdentity.cellIdentityCdma) && HidlSupport.deepEquals(this.cellIdentityLte, cellIdentity.cellIdentityLte) && HidlSupport.deepEquals(this.cellIdentityTdscdma, cellIdentity.cellIdentityTdscdma)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cellInfoType))), Integer.valueOf(HidlSupport.deepHashCode(this.cellIdentityGsm)), Integer.valueOf(HidlSupport.deepHashCode(this.cellIdentityWcdma)), Integer.valueOf(HidlSupport.deepHashCode(this.cellIdentityCdma)), Integer.valueOf(HidlSupport.deepHashCode(this.cellIdentityLte)), Integer.valueOf(HidlSupport.deepHashCode(this.cellIdentityTdscdma)));
    }

    public final String toString() {
        return "{.cellInfoType = " + CellInfoType.toString(this.cellInfoType) + ", .cellIdentityGsm = " + this.cellIdentityGsm + ", .cellIdentityWcdma = " + this.cellIdentityWcdma + ", .cellIdentityCdma = " + this.cellIdentityCdma + ", .cellIdentityLte = " + this.cellIdentityLte + ", .cellIdentityTdscdma = " + this.cellIdentityTdscdma + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(88L), 0L);
    }

    public static final ArrayList<CellIdentity> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<CellIdentity> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 88, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            CellIdentity cellIdentity = new CellIdentity();
            cellIdentity.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 88);
            arrayList.add(cellIdentity);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.cellInfoType = hwBlob.getInt32(j + 0);
        long j2 = j + 8;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 80, hwBlob.handle(), j2 + 0, true);
        this.cellIdentityGsm.clear();
        for (int i = 0; i < int32; i++) {
            CellIdentityGsm cellIdentityGsm = new CellIdentityGsm();
            cellIdentityGsm.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 80);
            this.cellIdentityGsm.add(cellIdentityGsm);
        }
        long j3 = j + 24;
        int int322 = hwBlob.getInt32(j3 + 8);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 80, hwBlob.handle(), j3 + 0, true);
        this.cellIdentityWcdma.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            CellIdentityWcdma cellIdentityWcdma = new CellIdentityWcdma();
            cellIdentityWcdma.readEmbeddedFromParcel(hwParcel, embeddedBuffer2, i2 * 80);
            this.cellIdentityWcdma.add(cellIdentityWcdma);
        }
        long j4 = j + 40;
        int int323 = hwBlob.getInt32(j4 + 8);
        HwBlob embeddedBuffer3 = hwParcel.readEmbeddedBuffer(int323 * 56, hwBlob.handle(), j4 + 0, true);
        this.cellIdentityCdma.clear();
        for (int i3 = 0; i3 < int323; i3++) {
            CellIdentityCdma cellIdentityCdma = new CellIdentityCdma();
            cellIdentityCdma.readEmbeddedFromParcel(hwParcel, embeddedBuffer3, i3 * 56);
            this.cellIdentityCdma.add(cellIdentityCdma);
        }
        long j5 = j + 56;
        int int324 = hwBlob.getInt32(j5 + 8);
        HwBlob embeddedBuffer4 = hwParcel.readEmbeddedBuffer(int324 * 88, hwBlob.handle(), j5 + 0, true);
        this.cellIdentityLte.clear();
        for (int i4 = 0; i4 < int324; i4++) {
            CellIdentityLte cellIdentityLte = new CellIdentityLte();
            cellIdentityLte.readEmbeddedFromParcel(hwParcel, embeddedBuffer4, i4 * 88);
            this.cellIdentityLte.add(cellIdentityLte);
        }
        long j6 = j + 72;
        int int325 = hwBlob.getInt32(j6 + 8);
        HwBlob embeddedBuffer5 = hwParcel.readEmbeddedBuffer(int325 * 88, hwBlob.handle(), 0 + j6, true);
        this.cellIdentityTdscdma.clear();
        for (int i5 = 0; i5 < int325; i5++) {
            CellIdentityTdscdma cellIdentityTdscdma = new CellIdentityTdscdma();
            cellIdentityTdscdma.readEmbeddedFromParcel(hwParcel, embeddedBuffer5, i5 * 88);
            this.cellIdentityTdscdma.add(cellIdentityTdscdma);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(88);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<CellIdentity> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 88);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 88);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.cellInfoType);
        int size = this.cellIdentityGsm.size();
        long j2 = j + 8;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 80);
        for (int i = 0; i < size; i++) {
            this.cellIdentityGsm.get(i).writeEmbeddedToBlob(hwBlob2, i * 80);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        int size2 = this.cellIdentityWcdma.size();
        long j3 = j + 24;
        hwBlob.putInt32(j3 + 8, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 80);
        for (int i2 = 0; i2 < size2; i2++) {
            this.cellIdentityWcdma.get(i2).writeEmbeddedToBlob(hwBlob3, i2 * 80);
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
        int size3 = this.cellIdentityCdma.size();
        long j4 = j + 40;
        hwBlob.putInt32(j4 + 8, size3);
        hwBlob.putBool(j4 + 12, false);
        HwBlob hwBlob4 = new HwBlob(size3 * 56);
        for (int i3 = 0; i3 < size3; i3++) {
            this.cellIdentityCdma.get(i3).writeEmbeddedToBlob(hwBlob4, i3 * 56);
        }
        hwBlob.putBlob(j4 + 0, hwBlob4);
        int size4 = this.cellIdentityLte.size();
        long j5 = j + 56;
        hwBlob.putInt32(j5 + 8, size4);
        hwBlob.putBool(j5 + 12, false);
        HwBlob hwBlob5 = new HwBlob(size4 * 88);
        for (int i4 = 0; i4 < size4; i4++) {
            this.cellIdentityLte.get(i4).writeEmbeddedToBlob(hwBlob5, i4 * 88);
        }
        hwBlob.putBlob(j5 + 0, hwBlob5);
        int size5 = this.cellIdentityTdscdma.size();
        long j6 = j + 72;
        hwBlob.putInt32(8 + j6, size5);
        hwBlob.putBool(j6 + 12, false);
        HwBlob hwBlob6 = new HwBlob(size5 * 88);
        for (int i5 = 0; i5 < size5; i5++) {
            this.cellIdentityTdscdma.get(i5).writeEmbeddedToBlob(hwBlob6, i5 * 88);
        }
        hwBlob.putBlob(j6 + 0, hwBlob6);
    }
}
