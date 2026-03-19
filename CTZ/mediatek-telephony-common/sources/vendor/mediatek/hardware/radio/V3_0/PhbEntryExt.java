package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class PhbEntryExt {
    public int adtype;
    public int hidden;
    public int index;
    public int type;
    public String number = new String();
    public String text = new String();
    public String group = new String();
    public String adnumber = new String();
    public String secondtext = new String();
    public String email = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != PhbEntryExt.class) {
            return false;
        }
        PhbEntryExt phbEntryExt = (PhbEntryExt) obj;
        if (this.index == phbEntryExt.index && HidlSupport.deepEquals(this.number, phbEntryExt.number) && this.type == phbEntryExt.type && HidlSupport.deepEquals(this.text, phbEntryExt.text) && this.hidden == phbEntryExt.hidden && HidlSupport.deepEquals(this.group, phbEntryExt.group) && HidlSupport.deepEquals(this.adnumber, phbEntryExt.adnumber) && this.adtype == phbEntryExt.adtype && HidlSupport.deepEquals(this.secondtext, phbEntryExt.secondtext) && HidlSupport.deepEquals(this.email, phbEntryExt.email)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.index))), Integer.valueOf(HidlSupport.deepHashCode(this.number)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(this.text)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.hidden))), Integer.valueOf(HidlSupport.deepHashCode(this.group)), Integer.valueOf(HidlSupport.deepHashCode(this.adnumber)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.adtype))), Integer.valueOf(HidlSupport.deepHashCode(this.secondtext)), Integer.valueOf(HidlSupport.deepHashCode(this.email)));
    }

    public final String toString() {
        return "{.index = " + this.index + ", .number = " + this.number + ", .type = " + this.type + ", .text = " + this.text + ", .hidden = " + this.hidden + ", .group = " + this.group + ", .adnumber = " + this.adnumber + ", .adtype = " + this.adtype + ", .secondtext = " + this.secondtext + ", .email = " + this.email + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(128L), 0L);
    }

    public static final ArrayList<PhbEntryExt> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<PhbEntryExt> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 128, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            PhbEntryExt phbEntryExt = new PhbEntryExt();
            phbEntryExt.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 128);
            arrayList.add(phbEntryExt);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.index = hwBlob.getInt32(j + 0);
        long j2 = j + 8;
        this.number = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.number.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.type = hwBlob.getInt32(j + 24);
        long j3 = j + 32;
        this.text = hwBlob.getString(j3);
        hwParcel.readEmbeddedBuffer(this.text.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
        this.hidden = hwBlob.getInt32(j + 48);
        long j4 = j + 56;
        this.group = hwBlob.getString(j4);
        hwParcel.readEmbeddedBuffer(this.group.getBytes().length + 1, hwBlob.handle(), j4 + 0, false);
        long j5 = j + 72;
        this.adnumber = hwBlob.getString(j5);
        hwParcel.readEmbeddedBuffer(this.adnumber.getBytes().length + 1, hwBlob.handle(), j5 + 0, false);
        this.adtype = hwBlob.getInt32(j + 88);
        long j6 = j + 96;
        this.secondtext = hwBlob.getString(j6);
        hwParcel.readEmbeddedBuffer(this.secondtext.getBytes().length + 1, hwBlob.handle(), j6 + 0, false);
        long j7 = j + 112;
        this.email = hwBlob.getString(j7);
        hwParcel.readEmbeddedBuffer(this.email.getBytes().length + 1, hwBlob.handle(), j7 + 0, false);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(128);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<PhbEntryExt> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 128);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 128);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.index);
        hwBlob.putString(8 + j, this.number);
        hwBlob.putInt32(24 + j, this.type);
        hwBlob.putString(32 + j, this.text);
        hwBlob.putInt32(48 + j, this.hidden);
        hwBlob.putString(56 + j, this.group);
        hwBlob.putString(72 + j, this.adnumber);
        hwBlob.putInt32(88 + j, this.adtype);
        hwBlob.putString(96 + j, this.secondtext);
        hwBlob.putString(j + 112, this.email);
    }
}
