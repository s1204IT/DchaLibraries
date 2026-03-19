package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class NanConfigRequest {
    public final NanBandSpecificConfig[] bandSpecificConfig = new NanBandSpecificConfig[2];
    public boolean disableDiscoveryAddressChangeIndication;
    public boolean disableJoinedClusterIndication;
    public boolean disableStartedClusterIndication;
    public boolean includePublishServiceIdsInBeacon;
    public boolean includeSubscribeServiceIdsInBeacon;
    public int macAddressRandomizationIntervalSec;
    public byte masterPref;
    public byte numberOfPublishServiceIdsInBeacon;
    public byte numberOfSubscribeServiceIdsInBeacon;
    public short rssiWindowSize;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanConfigRequest.class) {
            return false;
        }
        NanConfigRequest nanConfigRequest = (NanConfigRequest) obj;
        if (this.masterPref == nanConfigRequest.masterPref && this.disableDiscoveryAddressChangeIndication == nanConfigRequest.disableDiscoveryAddressChangeIndication && this.disableStartedClusterIndication == nanConfigRequest.disableStartedClusterIndication && this.disableJoinedClusterIndication == nanConfigRequest.disableJoinedClusterIndication && this.includePublishServiceIdsInBeacon == nanConfigRequest.includePublishServiceIdsInBeacon && this.numberOfPublishServiceIdsInBeacon == nanConfigRequest.numberOfPublishServiceIdsInBeacon && this.includeSubscribeServiceIdsInBeacon == nanConfigRequest.includeSubscribeServiceIdsInBeacon && this.numberOfSubscribeServiceIdsInBeacon == nanConfigRequest.numberOfSubscribeServiceIdsInBeacon && this.rssiWindowSize == nanConfigRequest.rssiWindowSize && this.macAddressRandomizationIntervalSec == nanConfigRequest.macAddressRandomizationIntervalSec && HidlSupport.deepEquals(this.bandSpecificConfig, nanConfigRequest.bandSpecificConfig)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.masterPref))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.disableDiscoveryAddressChangeIndication))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.disableStartedClusterIndication))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.disableJoinedClusterIndication))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.includePublishServiceIdsInBeacon))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.numberOfPublishServiceIdsInBeacon))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.includeSubscribeServiceIdsInBeacon))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.numberOfSubscribeServiceIdsInBeacon))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.rssiWindowSize))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.macAddressRandomizationIntervalSec))), Integer.valueOf(HidlSupport.deepHashCode(this.bandSpecificConfig)));
    }

    public final String toString() {
        return "{.masterPref = " + ((int) this.masterPref) + ", .disableDiscoveryAddressChangeIndication = " + this.disableDiscoveryAddressChangeIndication + ", .disableStartedClusterIndication = " + this.disableStartedClusterIndication + ", .disableJoinedClusterIndication = " + this.disableJoinedClusterIndication + ", .includePublishServiceIdsInBeacon = " + this.includePublishServiceIdsInBeacon + ", .numberOfPublishServiceIdsInBeacon = " + ((int) this.numberOfPublishServiceIdsInBeacon) + ", .includeSubscribeServiceIdsInBeacon = " + this.includeSubscribeServiceIdsInBeacon + ", .numberOfSubscribeServiceIdsInBeacon = " + ((int) this.numberOfSubscribeServiceIdsInBeacon) + ", .rssiWindowSize = " + ((int) this.rssiWindowSize) + ", .macAddressRandomizationIntervalSec = " + this.macAddressRandomizationIntervalSec + ", .bandSpecificConfig = " + Arrays.toString(this.bandSpecificConfig) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(32L), 0L);
    }

    public static final ArrayList<NanConfigRequest> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanConfigRequest> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanConfigRequest nanConfigRequest = new NanConfigRequest();
            nanConfigRequest.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
            arrayList.add(nanConfigRequest);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.masterPref = hwBlob.getInt8(0 + j);
        this.disableDiscoveryAddressChangeIndication = hwBlob.getBool(1 + j);
        this.disableStartedClusterIndication = hwBlob.getBool(2 + j);
        this.disableJoinedClusterIndication = hwBlob.getBool(3 + j);
        this.includePublishServiceIdsInBeacon = hwBlob.getBool(4 + j);
        this.numberOfPublishServiceIdsInBeacon = hwBlob.getInt8(5 + j);
        this.includeSubscribeServiceIdsInBeacon = hwBlob.getBool(6 + j);
        this.numberOfSubscribeServiceIdsInBeacon = hwBlob.getInt8(7 + j);
        this.rssiWindowSize = hwBlob.getInt16(j + 8);
        this.macAddressRandomizationIntervalSec = hwBlob.getInt32(12 + j);
        long j2 = j + 16;
        for (int i = 0; i < 2; i++) {
            this.bandSpecificConfig[i] = new NanBandSpecificConfig();
            this.bandSpecificConfig[i].readEmbeddedFromParcel(hwParcel, hwBlob, j2);
            j2 += 8;
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(32);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanConfigRequest> arrayList) {
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
        hwBlob.putInt8(0 + j, this.masterPref);
        hwBlob.putBool(1 + j, this.disableDiscoveryAddressChangeIndication);
        hwBlob.putBool(2 + j, this.disableStartedClusterIndication);
        hwBlob.putBool(3 + j, this.disableJoinedClusterIndication);
        hwBlob.putBool(4 + j, this.includePublishServiceIdsInBeacon);
        hwBlob.putInt8(5 + j, this.numberOfPublishServiceIdsInBeacon);
        hwBlob.putBool(6 + j, this.includeSubscribeServiceIdsInBeacon);
        hwBlob.putInt8(7 + j, this.numberOfSubscribeServiceIdsInBeacon);
        hwBlob.putInt16(j + 8, this.rssiWindowSize);
        hwBlob.putInt32(12 + j, this.macAddressRandomizationIntervalSec);
        long j2 = j + 16;
        for (int i = 0; i < 2; i++) {
            this.bandSpecificConfig[i].writeEmbeddedToBlob(hwBlob, j2);
            j2 += 8;
        }
    }
}
