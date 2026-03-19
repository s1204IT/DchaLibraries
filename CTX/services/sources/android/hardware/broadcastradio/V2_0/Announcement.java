package android.hardware.broadcastradio.V2_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class Announcement {
    public byte type;
    public final ProgramSelector selector = new ProgramSelector();
    public final ArrayList<VendorKeyValue> vendorInfo = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != Announcement.class) {
            return false;
        }
        Announcement announcement = (Announcement) obj;
        if (HidlSupport.deepEquals(this.selector, announcement.selector) && this.type == announcement.type && HidlSupport.deepEquals(this.vendorInfo, announcement.vendorInfo)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.selector)), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(this.vendorInfo)));
    }

    public final String toString() {
        return "{.selector = " + this.selector + ", .type = " + AnnouncementType.toString(this.type) + ", .vendorInfo = " + this.vendorInfo + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(56L), 0L);
    }

    public static final ArrayList<Announcement> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<Announcement> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 56, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            Announcement announcement = new Announcement();
            announcement.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 56);
            arrayList.add(announcement);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.selector.readEmbeddedFromParcel(hwParcel, hwBlob, j + 0);
        this.type = hwBlob.getInt8(j + 32);
        long j2 = j + 40;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, hwBlob.handle(), j2 + 0, true);
        this.vendorInfo.clear();
        for (int i = 0; i < int32; i++) {
            VendorKeyValue vendorKeyValue = new VendorKeyValue();
            vendorKeyValue.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
            this.vendorInfo.add(vendorKeyValue);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(56);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<Announcement> arrayList) {
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
        this.selector.writeEmbeddedToBlob(hwBlob, j + 0);
        hwBlob.putInt8(32 + j, this.type);
        int size = this.vendorInfo.size();
        long j2 = j + 40;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 32);
        for (int i = 0; i < size; i++) {
            this.vendorInfo.get(i).writeEmbeddedToBlob(hwBlob2, i * 32);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
