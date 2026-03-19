package android.hardware.radio.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class CdmaSmsMessage {
    public boolean isServicePresent;
    public int serviceCategory;
    public int teleserviceId;
    public final CdmaSmsAddress address = new CdmaSmsAddress();
    public final CdmaSmsSubaddress subAddress = new CdmaSmsSubaddress();
    public final ArrayList<Byte> bearerData = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != CdmaSmsMessage.class) {
            return false;
        }
        CdmaSmsMessage cdmaSmsMessage = (CdmaSmsMessage) obj;
        if (this.teleserviceId == cdmaSmsMessage.teleserviceId && this.isServicePresent == cdmaSmsMessage.isServicePresent && this.serviceCategory == cdmaSmsMessage.serviceCategory && HidlSupport.deepEquals(this.address, cdmaSmsMessage.address) && HidlSupport.deepEquals(this.subAddress, cdmaSmsMessage.subAddress) && HidlSupport.deepEquals(this.bearerData, cdmaSmsMessage.bearerData)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.teleserviceId))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.isServicePresent))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.serviceCategory))), Integer.valueOf(HidlSupport.deepHashCode(this.address)), Integer.valueOf(HidlSupport.deepHashCode(this.subAddress)), Integer.valueOf(HidlSupport.deepHashCode(this.bearerData)));
    }

    public final String toString() {
        return "{.teleserviceId = " + this.teleserviceId + ", .isServicePresent = " + this.isServicePresent + ", .serviceCategory = " + this.serviceCategory + ", .address = " + this.address + ", .subAddress = " + this.subAddress + ", .bearerData = " + this.bearerData + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(88L), 0L);
    }

    public static final ArrayList<CdmaSmsMessage> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<CdmaSmsMessage> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 88, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            CdmaSmsMessage cdmaSmsMessage = new CdmaSmsMessage();
            cdmaSmsMessage.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 88);
            arrayList.add(cdmaSmsMessage);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.teleserviceId = hwBlob.getInt32(j + 0);
        this.isServicePresent = hwBlob.getBool(j + 4);
        this.serviceCategory = hwBlob.getInt32(j + 8);
        this.address.readEmbeddedFromParcel(hwParcel, hwBlob, j + 16);
        this.subAddress.readEmbeddedFromParcel(hwParcel, hwBlob, j + 48);
        long j2 = j + 72;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
        this.bearerData.clear();
        for (int i = 0; i < int32; i++) {
            this.bearerData.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(88);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<CdmaSmsMessage> arrayList) {
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
        hwBlob.putInt32(j + 0, this.teleserviceId);
        hwBlob.putBool(4 + j, this.isServicePresent);
        hwBlob.putInt32(j + 8, this.serviceCategory);
        this.address.writeEmbeddedToBlob(hwBlob, 16 + j);
        this.subAddress.writeEmbeddedToBlob(hwBlob, 48 + j);
        int size = this.bearerData.size();
        long j2 = j + 72;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 1);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8(i * 1, this.bearerData.get(i).byteValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
