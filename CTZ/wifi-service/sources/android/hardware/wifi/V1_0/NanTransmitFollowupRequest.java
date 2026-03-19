package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class NanTransmitFollowupRequest {
    public boolean disableFollowupResultIndication;
    public byte discoverySessionId;
    public boolean isHighPriority;
    public int peerId;
    public boolean shouldUseDiscoveryWindow;
    public final byte[] addr = new byte[6];
    public final ArrayList<Byte> serviceSpecificInfo = new ArrayList<>();
    public final ArrayList<Byte> extendedServiceSpecificInfo = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanTransmitFollowupRequest.class) {
            return false;
        }
        NanTransmitFollowupRequest nanTransmitFollowupRequest = (NanTransmitFollowupRequest) obj;
        if (this.discoverySessionId == nanTransmitFollowupRequest.discoverySessionId && this.peerId == nanTransmitFollowupRequest.peerId && HidlSupport.deepEquals(this.addr, nanTransmitFollowupRequest.addr) && this.isHighPriority == nanTransmitFollowupRequest.isHighPriority && this.shouldUseDiscoveryWindow == nanTransmitFollowupRequest.shouldUseDiscoveryWindow && HidlSupport.deepEquals(this.serviceSpecificInfo, nanTransmitFollowupRequest.serviceSpecificInfo) && HidlSupport.deepEquals(this.extendedServiceSpecificInfo, nanTransmitFollowupRequest.extendedServiceSpecificInfo) && this.disableFollowupResultIndication == nanTransmitFollowupRequest.disableFollowupResultIndication) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.discoverySessionId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.peerId))), Integer.valueOf(HidlSupport.deepHashCode(this.addr)), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.isHighPriority))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.shouldUseDiscoveryWindow))), Integer.valueOf(HidlSupport.deepHashCode(this.serviceSpecificInfo)), Integer.valueOf(HidlSupport.deepHashCode(this.extendedServiceSpecificInfo)), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.disableFollowupResultIndication))));
    }

    public final String toString() {
        return "{.discoverySessionId = " + ((int) this.discoverySessionId) + ", .peerId = " + this.peerId + ", .addr = " + Arrays.toString(this.addr) + ", .isHighPriority = " + this.isHighPriority + ", .shouldUseDiscoveryWindow = " + this.shouldUseDiscoveryWindow + ", .serviceSpecificInfo = " + this.serviceSpecificInfo + ", .extendedServiceSpecificInfo = " + this.extendedServiceSpecificInfo + ", .disableFollowupResultIndication = " + this.disableFollowupResultIndication + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(56L), 0L);
    }

    public static final ArrayList<NanTransmitFollowupRequest> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanTransmitFollowupRequest> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 56, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanTransmitFollowupRequest nanTransmitFollowupRequest = new NanTransmitFollowupRequest();
            nanTransmitFollowupRequest.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 56);
            arrayList.add(nanTransmitFollowupRequest);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.discoverySessionId = hwBlob.getInt8(j + 0);
        this.peerId = hwBlob.getInt32(j + 4);
        hwBlob.copyToInt8Array(j + 8, this.addr, 6);
        this.isHighPriority = hwBlob.getBool(j + 14);
        this.shouldUseDiscoveryWindow = hwBlob.getBool(j + 15);
        long j2 = j + 16;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
        this.serviceSpecificInfo.clear();
        for (int i = 0; i < int32; i++) {
            this.serviceSpecificInfo.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
        long j3 = j + 32;
        int int322 = hwBlob.getInt32(8 + j3);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 1, hwBlob.handle(), j3 + 0, true);
        this.extendedServiceSpecificInfo.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            this.extendedServiceSpecificInfo.add(Byte.valueOf(embeddedBuffer2.getInt8(i2 * 1)));
        }
        this.disableFollowupResultIndication = hwBlob.getBool(j + 48);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(56);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanTransmitFollowupRequest> arrayList) {
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
        hwBlob.putInt8(j + 0, this.discoverySessionId);
        hwBlob.putInt32(j + 4, this.peerId);
        hwBlob.putInt8Array(j + 8, this.addr);
        hwBlob.putBool(j + 14, this.isHighPriority);
        hwBlob.putBool(j + 15, this.shouldUseDiscoveryWindow);
        int size = this.serviceSpecificInfo.size();
        long j2 = j + 16;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 1);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8(i * 1, this.serviceSpecificInfo.get(i).byteValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        int size2 = this.extendedServiceSpecificInfo.size();
        long j3 = j + 32;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 1);
        for (int i2 = 0; i2 < size2; i2++) {
            hwBlob3.putInt8(i2 * 1, this.extendedServiceSpecificInfo.get(i2).byteValue());
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
        hwBlob.putBool(j + 48, this.disableFollowupResultIndication);
    }
}
