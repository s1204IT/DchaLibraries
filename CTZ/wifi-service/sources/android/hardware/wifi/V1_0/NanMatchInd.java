package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class NanMatchInd {
    public byte discoverySessionId;
    public boolean matchOccuredInBeaconFlag;
    public boolean outOfResourceFlag;
    public int peerCipherType;
    public int peerId;
    public boolean peerRequiresRanging;
    public boolean peerRequiresSecurityEnabledInNdp;
    public int rangingIndicationType;
    public int rangingMeasurementInCm;
    public byte rssiValue;
    public final byte[] addr = new byte[6];
    public final ArrayList<Byte> serviceSpecificInfo = new ArrayList<>();
    public final ArrayList<Byte> extendedServiceSpecificInfo = new ArrayList<>();
    public final ArrayList<Byte> matchFilter = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanMatchInd.class) {
            return false;
        }
        NanMatchInd nanMatchInd = (NanMatchInd) obj;
        if (this.discoverySessionId == nanMatchInd.discoverySessionId && this.peerId == nanMatchInd.peerId && HidlSupport.deepEquals(this.addr, nanMatchInd.addr) && HidlSupport.deepEquals(this.serviceSpecificInfo, nanMatchInd.serviceSpecificInfo) && HidlSupport.deepEquals(this.extendedServiceSpecificInfo, nanMatchInd.extendedServiceSpecificInfo) && HidlSupport.deepEquals(this.matchFilter, nanMatchInd.matchFilter) && this.matchOccuredInBeaconFlag == nanMatchInd.matchOccuredInBeaconFlag && this.outOfResourceFlag == nanMatchInd.outOfResourceFlag && this.rssiValue == nanMatchInd.rssiValue && this.peerCipherType == nanMatchInd.peerCipherType && this.peerRequiresSecurityEnabledInNdp == nanMatchInd.peerRequiresSecurityEnabledInNdp && this.peerRequiresRanging == nanMatchInd.peerRequiresRanging && this.rangingMeasurementInCm == nanMatchInd.rangingMeasurementInCm && HidlSupport.deepEquals(Integer.valueOf(this.rangingIndicationType), Integer.valueOf(nanMatchInd.rangingIndicationType))) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.discoverySessionId))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.peerId))), Integer.valueOf(HidlSupport.deepHashCode(this.addr)), Integer.valueOf(HidlSupport.deepHashCode(this.serviceSpecificInfo)), Integer.valueOf(HidlSupport.deepHashCode(this.extendedServiceSpecificInfo)), Integer.valueOf(HidlSupport.deepHashCode(this.matchFilter)), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.matchOccuredInBeaconFlag))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.outOfResourceFlag))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.rssiValue))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.peerCipherType))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.peerRequiresSecurityEnabledInNdp))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.peerRequiresRanging))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.rangingMeasurementInCm))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.rangingIndicationType))));
    }

    public final String toString() {
        return "{.discoverySessionId = " + ((int) this.discoverySessionId) + ", .peerId = " + this.peerId + ", .addr = " + Arrays.toString(this.addr) + ", .serviceSpecificInfo = " + this.serviceSpecificInfo + ", .extendedServiceSpecificInfo = " + this.extendedServiceSpecificInfo + ", .matchFilter = " + this.matchFilter + ", .matchOccuredInBeaconFlag = " + this.matchOccuredInBeaconFlag + ", .outOfResourceFlag = " + this.outOfResourceFlag + ", .rssiValue = " + ((int) this.rssiValue) + ", .peerCipherType = " + NanCipherSuiteType.toString(this.peerCipherType) + ", .peerRequiresSecurityEnabledInNdp = " + this.peerRequiresSecurityEnabledInNdp + ", .peerRequiresRanging = " + this.peerRequiresRanging + ", .rangingMeasurementInCm = " + this.rangingMeasurementInCm + ", .rangingIndicationType = " + NanRangingIndication.dumpBitfield(this.rangingIndicationType) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(88L), 0L);
    }

    public static final ArrayList<NanMatchInd> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanMatchInd> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 88, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanMatchInd nanMatchInd = new NanMatchInd();
            nanMatchInd.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 88);
            arrayList.add(nanMatchInd);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.discoverySessionId = hwBlob.getInt8(j + 0);
        this.peerId = hwBlob.getInt32(j + 4);
        hwBlob.copyToInt8Array(j + 8, this.addr, 6);
        long j2 = j + 16;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
        this.serviceSpecificInfo.clear();
        for (int i = 0; i < int32; i++) {
            this.serviceSpecificInfo.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
        long j3 = j + 32;
        int int322 = hwBlob.getInt32(j3 + 8);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 1, hwBlob.handle(), j3 + 0, true);
        this.extendedServiceSpecificInfo.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            this.extendedServiceSpecificInfo.add(Byte.valueOf(embeddedBuffer2.getInt8(i2 * 1)));
        }
        long j4 = j + 48;
        int int323 = hwBlob.getInt32(8 + j4);
        HwBlob embeddedBuffer3 = hwParcel.readEmbeddedBuffer(int323 * 1, hwBlob.handle(), j4 + 0, true);
        this.matchFilter.clear();
        for (int i3 = 0; i3 < int323; i3++) {
            this.matchFilter.add(Byte.valueOf(embeddedBuffer3.getInt8(i3 * 1)));
        }
        this.matchOccuredInBeaconFlag = hwBlob.getBool(j + 64);
        this.outOfResourceFlag = hwBlob.getBool(j + 65);
        this.rssiValue = hwBlob.getInt8(j + 66);
        this.peerCipherType = hwBlob.getInt32(j + 68);
        this.peerRequiresSecurityEnabledInNdp = hwBlob.getBool(j + 72);
        this.peerRequiresRanging = hwBlob.getBool(j + 73);
        this.rangingMeasurementInCm = hwBlob.getInt32(j + 76);
        this.rangingIndicationType = hwBlob.getInt32(j + 80);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(88);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanMatchInd> arrayList) {
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
        hwBlob.putInt8(j + 0, this.discoverySessionId);
        hwBlob.putInt32(j + 4, this.peerId);
        hwBlob.putInt8Array(j + 8, this.addr);
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
        hwBlob.putInt32(j3 + 8, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 1);
        for (int i2 = 0; i2 < size2; i2++) {
            hwBlob3.putInt8(i2 * 1, this.extendedServiceSpecificInfo.get(i2).byteValue());
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
        int size3 = this.matchFilter.size();
        long j4 = j + 48;
        hwBlob.putInt32(8 + j4, size3);
        hwBlob.putBool(j4 + 12, false);
        HwBlob hwBlob4 = new HwBlob(size3 * 1);
        for (int i3 = 0; i3 < size3; i3++) {
            hwBlob4.putInt8(i3 * 1, this.matchFilter.get(i3).byteValue());
        }
        hwBlob.putBlob(j4 + 0, hwBlob4);
        hwBlob.putBool(j + 64, this.matchOccuredInBeaconFlag);
        hwBlob.putBool(j + 65, this.outOfResourceFlag);
        hwBlob.putInt8(j + 66, this.rssiValue);
        hwBlob.putInt32(j + 68, this.peerCipherType);
        hwBlob.putBool(j + 72, this.peerRequiresSecurityEnabledInNdp);
        hwBlob.putBool(j + 73, this.peerRequiresRanging);
        hwBlob.putInt32(j + 76, this.rangingMeasurementInCm);
        hwBlob.putInt32(j + 80, this.rangingIndicationType);
    }
}
