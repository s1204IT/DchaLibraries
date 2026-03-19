package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class PktFilter {
    public int bitmap;
    public int direction;
    public int flowLabel;
    public int id;
    public int localPortHigh;
    public int localPortLow;
    public int networkPfIdentifier;
    public int precedence;
    public int protocolNextHeader;
    public int remotePortHigh;
    public int remotePortLow;
    public int spi;
    public int tos;
    public int tosMask;
    public String address = new String();
    public String mask = new String();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != PktFilter.class) {
            return false;
        }
        PktFilter pktFilter = (PktFilter) obj;
        if (this.id == pktFilter.id && this.precedence == pktFilter.precedence && this.direction == pktFilter.direction && this.networkPfIdentifier == pktFilter.networkPfIdentifier && this.bitmap == pktFilter.bitmap && HidlSupport.deepEquals(this.address, pktFilter.address) && HidlSupport.deepEquals(this.mask, pktFilter.mask) && this.protocolNextHeader == pktFilter.protocolNextHeader && this.localPortLow == pktFilter.localPortLow && this.localPortHigh == pktFilter.localPortHigh && this.remotePortLow == pktFilter.remotePortLow && this.remotePortHigh == pktFilter.remotePortHigh && this.spi == pktFilter.spi && this.tos == pktFilter.tos && this.tosMask == pktFilter.tosMask && this.flowLabel == pktFilter.flowLabel) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.id))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.precedence))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.direction))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.networkPfIdentifier))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.bitmap))), Integer.valueOf(HidlSupport.deepHashCode(this.address)), Integer.valueOf(HidlSupport.deepHashCode(this.mask)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.protocolNextHeader))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.localPortLow))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.localPortHigh))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.remotePortLow))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.remotePortHigh))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.spi))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.tos))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.tosMask))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.flowLabel))));
    }

    public final String toString() {
        return "{.id = " + this.id + ", .precedence = " + this.precedence + ", .direction = " + this.direction + ", .networkPfIdentifier = " + this.networkPfIdentifier + ", .bitmap = " + this.bitmap + ", .address = " + this.address + ", .mask = " + this.mask + ", .protocolNextHeader = " + this.protocolNextHeader + ", .localPortLow = " + this.localPortLow + ", .localPortHigh = " + this.localPortHigh + ", .remotePortLow = " + this.remotePortLow + ", .remotePortHigh = " + this.remotePortHigh + ", .spi = " + this.spi + ", .tos = " + this.tos + ", .tosMask = " + this.tosMask + ", .flowLabel = " + this.flowLabel + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(96L), 0L);
    }

    public static final ArrayList<PktFilter> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<PktFilter> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 96, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            PktFilter pktFilter = new PktFilter();
            pktFilter.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 96);
            arrayList.add(pktFilter);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.id = hwBlob.getInt32(j + 0);
        this.precedence = hwBlob.getInt32(j + 4);
        this.direction = hwBlob.getInt32(j + 8);
        this.networkPfIdentifier = hwBlob.getInt32(j + 12);
        this.bitmap = hwBlob.getInt32(j + 16);
        long j2 = j + 24;
        this.address = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.address.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        long j3 = j + 40;
        this.mask = hwBlob.getString(j3);
        hwParcel.readEmbeddedBuffer(this.mask.getBytes().length + 1, hwBlob.handle(), j3 + 0, false);
        this.protocolNextHeader = hwBlob.getInt32(j + 56);
        this.localPortLow = hwBlob.getInt32(j + 60);
        this.localPortHigh = hwBlob.getInt32(j + 64);
        this.remotePortLow = hwBlob.getInt32(j + 68);
        this.remotePortHigh = hwBlob.getInt32(j + 72);
        this.spi = hwBlob.getInt32(j + 76);
        this.tos = hwBlob.getInt32(j + 80);
        this.tosMask = hwBlob.getInt32(j + 84);
        this.flowLabel = hwBlob.getInt32(j + 88);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(96);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<PktFilter> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 96);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 96);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.id);
        hwBlob.putInt32(4 + j, this.precedence);
        hwBlob.putInt32(8 + j, this.direction);
        hwBlob.putInt32(12 + j, this.networkPfIdentifier);
        hwBlob.putInt32(16 + j, this.bitmap);
        hwBlob.putString(24 + j, this.address);
        hwBlob.putString(40 + j, this.mask);
        hwBlob.putInt32(56 + j, this.protocolNextHeader);
        hwBlob.putInt32(60 + j, this.localPortLow);
        hwBlob.putInt32(64 + j, this.localPortHigh);
        hwBlob.putInt32(68 + j, this.remotePortLow);
        hwBlob.putInt32(72 + j, this.remotePortHigh);
        hwBlob.putInt32(76 + j, this.spi);
        hwBlob.putInt32(80 + j, this.tos);
        hwBlob.putInt32(84 + j, this.tosMask);
        hwBlob.putInt32(j + 88, this.flowLabel);
    }
}
