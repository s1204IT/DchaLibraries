package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class NanCapabilities {
    public int maxAppInfoLen;
    public int maxConcurrentClusters;
    public int maxExtendedServiceSpecificInfoLen;
    public int maxMatchFilterLen;
    public int maxNdiInterfaces;
    public int maxNdpSessions;
    public int maxPublishes;
    public int maxQueuedTransmitFollowupMsgs;
    public int maxServiceNameLen;
    public int maxServiceSpecificInfoLen;
    public int maxSubscribeInterfaceAddresses;
    public int maxSubscribes;
    public int maxTotalMatchFilterLen;
    public int supportedCipherSuites;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanCapabilities.class) {
            return false;
        }
        NanCapabilities nanCapabilities = (NanCapabilities) obj;
        if (this.maxConcurrentClusters == nanCapabilities.maxConcurrentClusters && this.maxPublishes == nanCapabilities.maxPublishes && this.maxSubscribes == nanCapabilities.maxSubscribes && this.maxServiceNameLen == nanCapabilities.maxServiceNameLen && this.maxMatchFilterLen == nanCapabilities.maxMatchFilterLen && this.maxTotalMatchFilterLen == nanCapabilities.maxTotalMatchFilterLen && this.maxServiceSpecificInfoLen == nanCapabilities.maxServiceSpecificInfoLen && this.maxExtendedServiceSpecificInfoLen == nanCapabilities.maxExtendedServiceSpecificInfoLen && this.maxNdiInterfaces == nanCapabilities.maxNdiInterfaces && this.maxNdpSessions == nanCapabilities.maxNdpSessions && this.maxAppInfoLen == nanCapabilities.maxAppInfoLen && this.maxQueuedTransmitFollowupMsgs == nanCapabilities.maxQueuedTransmitFollowupMsgs && this.maxSubscribeInterfaceAddresses == nanCapabilities.maxSubscribeInterfaceAddresses && HidlSupport.deepEquals(Integer.valueOf(this.supportedCipherSuites), Integer.valueOf(nanCapabilities.supportedCipherSuites))) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxConcurrentClusters))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxPublishes))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxSubscribes))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxServiceNameLen))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxMatchFilterLen))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxTotalMatchFilterLen))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxServiceSpecificInfoLen))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxExtendedServiceSpecificInfoLen))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxNdiInterfaces))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxNdpSessions))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxAppInfoLen))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxQueuedTransmitFollowupMsgs))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.maxSubscribeInterfaceAddresses))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.supportedCipherSuites))));
    }

    public final String toString() {
        return "{.maxConcurrentClusters = " + this.maxConcurrentClusters + ", .maxPublishes = " + this.maxPublishes + ", .maxSubscribes = " + this.maxSubscribes + ", .maxServiceNameLen = " + this.maxServiceNameLen + ", .maxMatchFilterLen = " + this.maxMatchFilterLen + ", .maxTotalMatchFilterLen = " + this.maxTotalMatchFilterLen + ", .maxServiceSpecificInfoLen = " + this.maxServiceSpecificInfoLen + ", .maxExtendedServiceSpecificInfoLen = " + this.maxExtendedServiceSpecificInfoLen + ", .maxNdiInterfaces = " + this.maxNdiInterfaces + ", .maxNdpSessions = " + this.maxNdpSessions + ", .maxAppInfoLen = " + this.maxAppInfoLen + ", .maxQueuedTransmitFollowupMsgs = " + this.maxQueuedTransmitFollowupMsgs + ", .maxSubscribeInterfaceAddresses = " + this.maxSubscribeInterfaceAddresses + ", .supportedCipherSuites = " + NanCipherSuiteType.dumpBitfield(this.supportedCipherSuites) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(56L), 0L);
    }

    public static final ArrayList<NanCapabilities> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanCapabilities> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 56, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanCapabilities nanCapabilities = new NanCapabilities();
            nanCapabilities.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 56);
            arrayList.add(nanCapabilities);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.maxConcurrentClusters = hwBlob.getInt32(0 + j);
        this.maxPublishes = hwBlob.getInt32(4 + j);
        this.maxSubscribes = hwBlob.getInt32(8 + j);
        this.maxServiceNameLen = hwBlob.getInt32(12 + j);
        this.maxMatchFilterLen = hwBlob.getInt32(16 + j);
        this.maxTotalMatchFilterLen = hwBlob.getInt32(20 + j);
        this.maxServiceSpecificInfoLen = hwBlob.getInt32(24 + j);
        this.maxExtendedServiceSpecificInfoLen = hwBlob.getInt32(28 + j);
        this.maxNdiInterfaces = hwBlob.getInt32(32 + j);
        this.maxNdpSessions = hwBlob.getInt32(36 + j);
        this.maxAppInfoLen = hwBlob.getInt32(40 + j);
        this.maxQueuedTransmitFollowupMsgs = hwBlob.getInt32(44 + j);
        this.maxSubscribeInterfaceAddresses = hwBlob.getInt32(48 + j);
        this.supportedCipherSuites = hwBlob.getInt32(j + 52);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(56);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanCapabilities> arrayList) {
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
        hwBlob.putInt32(0 + j, this.maxConcurrentClusters);
        hwBlob.putInt32(4 + j, this.maxPublishes);
        hwBlob.putInt32(8 + j, this.maxSubscribes);
        hwBlob.putInt32(12 + j, this.maxServiceNameLen);
        hwBlob.putInt32(16 + j, this.maxMatchFilterLen);
        hwBlob.putInt32(20 + j, this.maxTotalMatchFilterLen);
        hwBlob.putInt32(24 + j, this.maxServiceSpecificInfoLen);
        hwBlob.putInt32(28 + j, this.maxExtendedServiceSpecificInfoLen);
        hwBlob.putInt32(32 + j, this.maxNdiInterfaces);
        hwBlob.putInt32(36 + j, this.maxNdpSessions);
        hwBlob.putInt32(40 + j, this.maxAppInfoLen);
        hwBlob.putInt32(44 + j, this.maxQueuedTransmitFollowupMsgs);
        hwBlob.putInt32(48 + j, this.maxSubscribeInterfaceAddresses);
        hwBlob.putInt32(j + 52, this.supportedCipherSuites);
    }
}
