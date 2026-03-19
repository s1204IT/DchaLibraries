package android.hardware.usb.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class PortRole {
    public int role;
    public int type;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != PortRole.class) {
            return false;
        }
        PortRole portRole = (PortRole) obj;
        if (this.type == portRole.type && this.role == portRole.role) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.role))));
    }

    public final String toString() {
        return "{.type = " + PortRoleType.toString(this.type) + ", .role = " + this.role + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(8L), 0L);
    }

    public static final ArrayList<PortRole> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<PortRole> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 8, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            PortRole portRole = new PortRole();
            portRole.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 8);
            arrayList.add(portRole);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.type = hwBlob.getInt32(0 + j);
        this.role = hwBlob.getInt32(j + 4);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(8);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<PortRole> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 8);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 8);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.type);
        hwBlob.putInt32(j + 4, this.role);
    }
}
