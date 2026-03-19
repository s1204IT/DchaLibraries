package android.hardware.radio.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class Call {
    public byte als;
    public int index;
    public boolean isMT;
    public boolean isMpty;
    public boolean isVoice;
    public boolean isVoicePrivacy;
    public int namePresentation;
    public int numberPresentation;
    public int state;
    public int toa;
    public String number = new String();
    public String name = new String();
    public final ArrayList<UusInfo> uusInfo = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != Call.class) {
            return false;
        }
        Call call = (Call) obj;
        if (this.state == call.state && this.index == call.index && this.toa == call.toa && this.isMpty == call.isMpty && this.isMT == call.isMT && this.als == call.als && this.isVoice == call.isVoice && this.isVoicePrivacy == call.isVoicePrivacy && HidlSupport.deepEquals(this.number, call.number) && this.numberPresentation == call.numberPresentation && HidlSupport.deepEquals(this.name, call.name) && this.namePresentation == call.namePresentation && HidlSupport.deepEquals(this.uusInfo, call.uusInfo)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.state))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.index))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.toa))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.isMpty))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.isMT))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.als))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.isVoice))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.isVoicePrivacy))), Integer.valueOf(HidlSupport.deepHashCode(this.number)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.numberPresentation))), Integer.valueOf(HidlSupport.deepHashCode(this.name)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.namePresentation))), Integer.valueOf(HidlSupport.deepHashCode(this.uusInfo)));
    }

    public final String toString() {
        return "{.state = " + CallState.toString(this.state) + ", .index = " + this.index + ", .toa = " + this.toa + ", .isMpty = " + this.isMpty + ", .isMT = " + this.isMT + ", .als = " + ((int) this.als) + ", .isVoice = " + this.isVoice + ", .isVoicePrivacy = " + this.isVoicePrivacy + ", .number = " + this.number + ", .numberPresentation = " + CallPresentation.toString(this.numberPresentation) + ", .name = " + this.name + ", .namePresentation = " + CallPresentation.toString(this.namePresentation) + ", .uusInfo = " + this.uusInfo + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(88L), 0L);
    }

    public static final ArrayList<Call> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<Call> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 88, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            Call call = new Call();
            call.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 88);
            arrayList.add(call);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.state = hwBlob.getInt32(j + 0);
        this.index = hwBlob.getInt32(j + 4);
        this.toa = hwBlob.getInt32(j + 8);
        this.isMpty = hwBlob.getBool(j + 12);
        this.isMT = hwBlob.getBool(j + 13);
        this.als = hwBlob.getInt8(j + 14);
        this.isVoice = hwBlob.getBool(j + 15);
        this.isVoicePrivacy = hwBlob.getBool(j + 16);
        long j2 = j + 24;
        this.number = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.number.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.numberPresentation = hwBlob.getInt32(j + 40);
        long j3 = j + 48;
        this.name = hwBlob.getString(j3);
        hwParcel.readEmbeddedBuffer(this.name.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
        this.namePresentation = hwBlob.getInt32(j + 64);
        long j4 = j + 72;
        int int32 = hwBlob.getInt32(8 + j4);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 24, hwBlob.handle(), j4 + 0, true);
        this.uusInfo.clear();
        for (int i = 0; i < int32; i++) {
            UusInfo uusInfo = new UusInfo();
            uusInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 24);
            this.uusInfo.add(uusInfo);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(88);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<Call> arrayList) {
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
        hwBlob.putInt32(j + 0, this.state);
        hwBlob.putInt32(4 + j, this.index);
        hwBlob.putInt32(j + 8, this.toa);
        hwBlob.putBool(j + 12, this.isMpty);
        hwBlob.putBool(13 + j, this.isMT);
        hwBlob.putInt8(14 + j, this.als);
        hwBlob.putBool(15 + j, this.isVoice);
        hwBlob.putBool(16 + j, this.isVoicePrivacy);
        hwBlob.putString(24 + j, this.number);
        hwBlob.putInt32(40 + j, this.numberPresentation);
        hwBlob.putString(48 + j, this.name);
        hwBlob.putInt32(64 + j, this.namePresentation);
        int size = this.uusInfo.size();
        long j2 = j + 72;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 24);
        for (int i = 0; i < size; i++) {
            this.uusInfo.get(i).writeEmbeddedToBlob(hwBlob2, i * 24);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
