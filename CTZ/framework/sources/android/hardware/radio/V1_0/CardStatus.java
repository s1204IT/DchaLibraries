package android.hardware.radio.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class CardStatus {
    public final ArrayList<AppStatus> applications = new ArrayList<>();
    public int cardState;
    public int cdmaSubscriptionAppIndex;
    public int gsmUmtsSubscriptionAppIndex;
    public int imsSubscriptionAppIndex;
    public int universalPinState;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != CardStatus.class) {
            return false;
        }
        CardStatus cardStatus = (CardStatus) obj;
        if (this.cardState == cardStatus.cardState && this.universalPinState == cardStatus.universalPinState && this.gsmUmtsSubscriptionAppIndex == cardStatus.gsmUmtsSubscriptionAppIndex && this.cdmaSubscriptionAppIndex == cardStatus.cdmaSubscriptionAppIndex && this.imsSubscriptionAppIndex == cardStatus.imsSubscriptionAppIndex && HidlSupport.deepEquals(this.applications, cardStatus.applications)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cardState))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.universalPinState))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.gsmUmtsSubscriptionAppIndex))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cdmaSubscriptionAppIndex))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.imsSubscriptionAppIndex))), Integer.valueOf(HidlSupport.deepHashCode(this.applications)));
    }

    public final String toString() {
        return "{.cardState = " + CardState.toString(this.cardState) + ", .universalPinState = " + PinState.toString(this.universalPinState) + ", .gsmUmtsSubscriptionAppIndex = " + this.gsmUmtsSubscriptionAppIndex + ", .cdmaSubscriptionAppIndex = " + this.cdmaSubscriptionAppIndex + ", .imsSubscriptionAppIndex = " + this.imsSubscriptionAppIndex + ", .applications = " + this.applications + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(40L), 0L);
    }

    public static final ArrayList<CardStatus> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<CardStatus> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 40, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            CardStatus cardStatus = new CardStatus();
            cardStatus.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 40);
            arrayList.add(cardStatus);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.cardState = hwBlob.getInt32(j + 0);
        this.universalPinState = hwBlob.getInt32(j + 4);
        this.gsmUmtsSubscriptionAppIndex = hwBlob.getInt32(j + 8);
        this.cdmaSubscriptionAppIndex = hwBlob.getInt32(j + 12);
        this.imsSubscriptionAppIndex = hwBlob.getInt32(j + 16);
        long j2 = j + 24;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 64, hwBlob.handle(), j2 + 0, true);
        this.applications.clear();
        for (int i = 0; i < int32; i++) {
            AppStatus appStatus = new AppStatus();
            appStatus.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 64);
            this.applications.add(appStatus);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(40);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<CardStatus> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 40);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 40);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.cardState);
        hwBlob.putInt32(4 + j, this.universalPinState);
        hwBlob.putInt32(j + 8, this.gsmUmtsSubscriptionAppIndex);
        hwBlob.putInt32(j + 12, this.cdmaSubscriptionAppIndex);
        hwBlob.putInt32(16 + j, this.imsSubscriptionAppIndex);
        int size = this.applications.size();
        long j2 = j + 24;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 64);
        for (int i = 0; i < size; i++) {
            this.applications.get(i).writeEmbeddedToBlob(hwBlob2, i * 64);
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
