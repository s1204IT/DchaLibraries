package android.hardware.radio.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class CarrierRestrictions {
    public final ArrayList<Carrier> allowedCarriers = new ArrayList<>();
    public final ArrayList<Carrier> excludedCarriers = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != CarrierRestrictions.class) {
            return false;
        }
        CarrierRestrictions carrierRestrictions = (CarrierRestrictions) obj;
        if (HidlSupport.deepEquals(this.allowedCarriers, carrierRestrictions.allowedCarriers) && HidlSupport.deepEquals(this.excludedCarriers, carrierRestrictions.excludedCarriers)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.allowedCarriers)), Integer.valueOf(HidlSupport.deepHashCode(this.excludedCarriers)));
    }

    public final String toString() {
        return "{.allowedCarriers = " + this.allowedCarriers + ", .excludedCarriers = " + this.excludedCarriers + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(32L), 0L);
    }

    public static final ArrayList<CarrierRestrictions> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<CarrierRestrictions> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            CarrierRestrictions carrierRestrictions = new CarrierRestrictions();
            carrierRestrictions.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
            arrayList.add(carrierRestrictions);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        long j2 = j + 0;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 56, hwBlob.handle(), j2 + 0, true);
        this.allowedCarriers.clear();
        for (int i = 0; i < int32; i++) {
            Carrier carrier = new Carrier();
            carrier.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 56);
            this.allowedCarriers.add(carrier);
        }
        long j3 = j + 16;
        int int322 = hwBlob.getInt32(j3 + 8);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 56, hwBlob.handle(), 0 + j3, true);
        this.excludedCarriers.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            Carrier carrier2 = new Carrier();
            carrier2.readEmbeddedFromParcel(hwParcel, embeddedBuffer2, i2 * 56);
            this.excludedCarriers.add(carrier2);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(32);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<CarrierRestrictions> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 32);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 32);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        int size = this.allowedCarriers.size();
        long j2 = j + 0;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 56);
        for (int i = 0; i < size; i++) {
            this.allowedCarriers.get(i).writeEmbeddedToBlob(hwBlob2, i * 56);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        int size2 = this.excludedCarriers.size();
        long j3 = j + 16;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 56);
        for (int i2 = 0; i2 < size2; i2++) {
            this.excludedCarriers.get(i2).writeEmbeddedToBlob(hwBlob3, i2 * 56);
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
    }
}
