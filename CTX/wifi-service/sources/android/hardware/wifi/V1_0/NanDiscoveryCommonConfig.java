package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class NanDiscoveryCommonConfig {
    public int configRangingIndications;
    public boolean disableDiscoveryTerminationIndication;
    public boolean disableFollowupReceivedIndication;
    public boolean disableMatchExpirationIndication;
    public byte discoveryCount;
    public int discoveryMatchIndicator;
    public short discoveryWindowPeriod;
    public short distanceEgressCm;
    public short distanceIngressCm;
    public int rangingIntervalMsec;
    public boolean rangingRequired;
    public byte sessionId;
    public short ttlSec;
    public boolean useRssiThreshold;
    public final ArrayList<Byte> serviceName = new ArrayList<>();
    public final ArrayList<Byte> serviceSpecificInfo = new ArrayList<>();
    public final ArrayList<Byte> extendedServiceSpecificInfo = new ArrayList<>();
    public final ArrayList<Byte> rxMatchFilter = new ArrayList<>();
    public final ArrayList<Byte> txMatchFilter = new ArrayList<>();
    public final NanDataPathSecurityConfig securityConfig = new NanDataPathSecurityConfig();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanDiscoveryCommonConfig.class) {
            return false;
        }
        NanDiscoveryCommonConfig nanDiscoveryCommonConfig = (NanDiscoveryCommonConfig) obj;
        if (this.sessionId == nanDiscoveryCommonConfig.sessionId && this.ttlSec == nanDiscoveryCommonConfig.ttlSec && this.discoveryWindowPeriod == nanDiscoveryCommonConfig.discoveryWindowPeriod && this.discoveryCount == nanDiscoveryCommonConfig.discoveryCount && HidlSupport.deepEquals(this.serviceName, nanDiscoveryCommonConfig.serviceName) && this.discoveryMatchIndicator == nanDiscoveryCommonConfig.discoveryMatchIndicator && HidlSupport.deepEquals(this.serviceSpecificInfo, nanDiscoveryCommonConfig.serviceSpecificInfo) && HidlSupport.deepEquals(this.extendedServiceSpecificInfo, nanDiscoveryCommonConfig.extendedServiceSpecificInfo) && HidlSupport.deepEquals(this.rxMatchFilter, nanDiscoveryCommonConfig.rxMatchFilter) && HidlSupport.deepEquals(this.txMatchFilter, nanDiscoveryCommonConfig.txMatchFilter) && this.useRssiThreshold == nanDiscoveryCommonConfig.useRssiThreshold && this.disableDiscoveryTerminationIndication == nanDiscoveryCommonConfig.disableDiscoveryTerminationIndication && this.disableMatchExpirationIndication == nanDiscoveryCommonConfig.disableMatchExpirationIndication && this.disableFollowupReceivedIndication == nanDiscoveryCommonConfig.disableFollowupReceivedIndication && HidlSupport.deepEquals(this.securityConfig, nanDiscoveryCommonConfig.securityConfig) && this.rangingRequired == nanDiscoveryCommonConfig.rangingRequired && this.rangingIntervalMsec == nanDiscoveryCommonConfig.rangingIntervalMsec && HidlSupport.deepEquals(Integer.valueOf(this.configRangingIndications), Integer.valueOf(nanDiscoveryCommonConfig.configRangingIndications)) && this.distanceIngressCm == nanDiscoveryCommonConfig.distanceIngressCm && this.distanceEgressCm == nanDiscoveryCommonConfig.distanceEgressCm) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.sessionId))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.ttlSec))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.discoveryWindowPeriod))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.discoveryCount))), Integer.valueOf(HidlSupport.deepHashCode(this.serviceName)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.discoveryMatchIndicator))), Integer.valueOf(HidlSupport.deepHashCode(this.serviceSpecificInfo)), Integer.valueOf(HidlSupport.deepHashCode(this.extendedServiceSpecificInfo)), Integer.valueOf(HidlSupport.deepHashCode(this.rxMatchFilter)), Integer.valueOf(HidlSupport.deepHashCode(this.txMatchFilter)), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.useRssiThreshold))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.disableDiscoveryTerminationIndication))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.disableMatchExpirationIndication))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.disableFollowupReceivedIndication))), Integer.valueOf(HidlSupport.deepHashCode(this.securityConfig)), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.rangingRequired))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.rangingIntervalMsec))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.configRangingIndications))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.distanceIngressCm))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.distanceEgressCm))));
    }

    public final String toString() {
        return "{.sessionId = " + ((int) this.sessionId) + ", .ttlSec = " + ((int) this.ttlSec) + ", .discoveryWindowPeriod = " + ((int) this.discoveryWindowPeriod) + ", .discoveryCount = " + ((int) this.discoveryCount) + ", .serviceName = " + this.serviceName + ", .discoveryMatchIndicator = " + NanMatchAlg.toString(this.discoveryMatchIndicator) + ", .serviceSpecificInfo = " + this.serviceSpecificInfo + ", .extendedServiceSpecificInfo = " + this.extendedServiceSpecificInfo + ", .rxMatchFilter = " + this.rxMatchFilter + ", .txMatchFilter = " + this.txMatchFilter + ", .useRssiThreshold = " + this.useRssiThreshold + ", .disableDiscoveryTerminationIndication = " + this.disableDiscoveryTerminationIndication + ", .disableMatchExpirationIndication = " + this.disableMatchExpirationIndication + ", .disableFollowupReceivedIndication = " + this.disableFollowupReceivedIndication + ", .securityConfig = " + this.securityConfig + ", .rangingRequired = " + this.rangingRequired + ", .rangingIntervalMsec = " + this.rangingIntervalMsec + ", .configRangingIndications = " + NanRangingIndication.dumpBitfield(this.configRangingIndications) + ", .distanceIngressCm = " + ((int) this.distanceIngressCm) + ", .distanceEgressCm = " + ((int) this.distanceEgressCm) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(176L), 0L);
    }

    public static final ArrayList<NanDiscoveryCommonConfig> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanDiscoveryCommonConfig> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 176, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanDiscoveryCommonConfig nanDiscoveryCommonConfig = new NanDiscoveryCommonConfig();
            nanDiscoveryCommonConfig.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 176);
            arrayList.add(nanDiscoveryCommonConfig);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.sessionId = hwBlob.getInt8(j + 0);
        this.ttlSec = hwBlob.getInt16(j + 2);
        this.discoveryWindowPeriod = hwBlob.getInt16(j + 4);
        this.discoveryCount = hwBlob.getInt8(j + 6);
        long j2 = j + 8;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
        this.serviceName.clear();
        for (int i = 0; i < int32; i++) {
            this.serviceName.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
        this.discoveryMatchIndicator = hwBlob.getInt32(j + 24);
        long j3 = j + 32;
        int int322 = hwBlob.getInt32(j3 + 8);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 1, hwBlob.handle(), j3 + 0, true);
        this.serviceSpecificInfo.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            this.serviceSpecificInfo.add(Byte.valueOf(embeddedBuffer2.getInt8(i2 * 1)));
        }
        long j4 = j + 48;
        int int323 = hwBlob.getInt32(j4 + 8);
        HwBlob embeddedBuffer3 = hwParcel.readEmbeddedBuffer(int323 * 1, hwBlob.handle(), j4 + 0, true);
        this.extendedServiceSpecificInfo.clear();
        for (int i3 = 0; i3 < int323; i3++) {
            this.extendedServiceSpecificInfo.add(Byte.valueOf(embeddedBuffer3.getInt8(i3 * 1)));
        }
        long j5 = j + 64;
        int int324 = hwBlob.getInt32(j5 + 8);
        HwBlob embeddedBuffer4 = hwParcel.readEmbeddedBuffer(int324 * 1, hwBlob.handle(), j5 + 0, true);
        this.rxMatchFilter.clear();
        for (int i4 = 0; i4 < int324; i4++) {
            this.rxMatchFilter.add(Byte.valueOf(embeddedBuffer4.getInt8(i4 * 1)));
        }
        long j6 = j + 80;
        int int325 = hwBlob.getInt32(8 + j6);
        HwBlob embeddedBuffer5 = hwParcel.readEmbeddedBuffer(int325 * 1, hwBlob.handle(), j6 + 0, true);
        this.txMatchFilter.clear();
        for (int i5 = 0; i5 < int325; i5++) {
            this.txMatchFilter.add(Byte.valueOf(embeddedBuffer5.getInt8(i5 * 1)));
        }
        this.useRssiThreshold = hwBlob.getBool(j + 96);
        this.disableDiscoveryTerminationIndication = hwBlob.getBool(j + 97);
        this.disableMatchExpirationIndication = hwBlob.getBool(j + 98);
        this.disableFollowupReceivedIndication = hwBlob.getBool(j + 99);
        this.securityConfig.readEmbeddedFromParcel(hwParcel, hwBlob, j + 104);
        this.rangingRequired = hwBlob.getBool(j + 160);
        this.rangingIntervalMsec = hwBlob.getInt32(j + 164);
        this.configRangingIndications = hwBlob.getInt32(j + 168);
        this.distanceIngressCm = hwBlob.getInt16(j + 172);
        this.distanceEgressCm = hwBlob.getInt16(j + 174);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(176);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanDiscoveryCommonConfig> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 176);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 176);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt8(j + 0, this.sessionId);
        hwBlob.putInt16(j + 2, this.ttlSec);
        hwBlob.putInt16(j + 4, this.discoveryWindowPeriod);
        hwBlob.putInt8(j + 6, this.discoveryCount);
        int size = this.serviceName.size();
        long j2 = j + 8;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 1);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8(i * 1, this.serviceName.get(i).byteValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        hwBlob.putInt32(j + 24, this.discoveryMatchIndicator);
        int size2 = this.serviceSpecificInfo.size();
        long j3 = j + 32;
        hwBlob.putInt32(j3 + 8, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 1);
        for (int i2 = 0; i2 < size2; i2++) {
            hwBlob3.putInt8(i2 * 1, this.serviceSpecificInfo.get(i2).byteValue());
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
        int size3 = this.extendedServiceSpecificInfo.size();
        long j4 = j + 48;
        hwBlob.putInt32(j4 + 8, size3);
        hwBlob.putBool(j4 + 12, false);
        HwBlob hwBlob4 = new HwBlob(size3 * 1);
        for (int i3 = 0; i3 < size3; i3++) {
            hwBlob4.putInt8(i3 * 1, this.extendedServiceSpecificInfo.get(i3).byteValue());
        }
        hwBlob.putBlob(j4 + 0, hwBlob4);
        int size4 = this.rxMatchFilter.size();
        long j5 = j + 64;
        hwBlob.putInt32(j5 + 8, size4);
        hwBlob.putBool(j5 + 12, false);
        HwBlob hwBlob5 = new HwBlob(size4 * 1);
        for (int i4 = 0; i4 < size4; i4++) {
            hwBlob5.putInt8(i4 * 1, this.rxMatchFilter.get(i4).byteValue());
        }
        hwBlob.putBlob(j5 + 0, hwBlob5);
        int size5 = this.txMatchFilter.size();
        long j6 = j + 80;
        hwBlob.putInt32(8 + j6, size5);
        hwBlob.putBool(j6 + 12, false);
        HwBlob hwBlob6 = new HwBlob(size5 * 1);
        for (int i5 = 0; i5 < size5; i5++) {
            hwBlob6.putInt8(i5 * 1, this.txMatchFilter.get(i5).byteValue());
        }
        hwBlob.putBlob(j6 + 0, hwBlob6);
        hwBlob.putBool(j + 96, this.useRssiThreshold);
        hwBlob.putBool(j + 97, this.disableDiscoveryTerminationIndication);
        hwBlob.putBool(j + 98, this.disableMatchExpirationIndication);
        hwBlob.putBool(j + 99, this.disableFollowupReceivedIndication);
        this.securityConfig.writeEmbeddedToBlob(hwBlob, j + 104);
        hwBlob.putBool(j + 160, this.rangingRequired);
        hwBlob.putInt32(j + 164, this.rangingIntervalMsec);
        hwBlob.putInt32(j + 168, this.configRangingIndications);
        hwBlob.putInt16(j + 172, this.distanceIngressCm);
        hwBlob.putInt16(j + 174, this.distanceEgressCm);
    }
}
