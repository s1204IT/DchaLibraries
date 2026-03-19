package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class NanDebugConfig {
    public short clusterIdBottomRangeVal;
    public short clusterIdTopRangeVal;
    public byte hopCountForceVal;
    public int ouiVal;
    public byte randomFactorForceVal;
    public boolean validClusterIdVals;
    public boolean validDiscoveryChannelVal;
    public boolean validHopCountForceVal;
    public boolean validIntfAddrVal;
    public boolean validOuiVal;
    public boolean validRandomFactorForceVal;
    public boolean validUseBeaconsInBandVal;
    public boolean validUseSdfInBandVal;
    public final byte[] intfAddrVal = new byte[6];
    public final int[] discoveryChannelMhzVal = new int[2];
    public final boolean[] useBeaconsInBandVal = new boolean[2];
    public final boolean[] useSdfInBandVal = new boolean[2];

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanDebugConfig.class) {
            return false;
        }
        NanDebugConfig nanDebugConfig = (NanDebugConfig) obj;
        if (this.validClusterIdVals == nanDebugConfig.validClusterIdVals && this.clusterIdBottomRangeVal == nanDebugConfig.clusterIdBottomRangeVal && this.clusterIdTopRangeVal == nanDebugConfig.clusterIdTopRangeVal && this.validIntfAddrVal == nanDebugConfig.validIntfAddrVal && HidlSupport.deepEquals(this.intfAddrVal, nanDebugConfig.intfAddrVal) && this.validOuiVal == nanDebugConfig.validOuiVal && this.ouiVal == nanDebugConfig.ouiVal && this.validRandomFactorForceVal == nanDebugConfig.validRandomFactorForceVal && this.randomFactorForceVal == nanDebugConfig.randomFactorForceVal && this.validHopCountForceVal == nanDebugConfig.validHopCountForceVal && this.hopCountForceVal == nanDebugConfig.hopCountForceVal && this.validDiscoveryChannelVal == nanDebugConfig.validDiscoveryChannelVal && HidlSupport.deepEquals(this.discoveryChannelMhzVal, nanDebugConfig.discoveryChannelMhzVal) && this.validUseBeaconsInBandVal == nanDebugConfig.validUseBeaconsInBandVal && HidlSupport.deepEquals(this.useBeaconsInBandVal, nanDebugConfig.useBeaconsInBandVal) && this.validUseSdfInBandVal == nanDebugConfig.validUseSdfInBandVal && HidlSupport.deepEquals(this.useSdfInBandVal, nanDebugConfig.useSdfInBandVal)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.validClusterIdVals))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.clusterIdBottomRangeVal))), Integer.valueOf(HidlSupport.deepHashCode(Short.valueOf(this.clusterIdTopRangeVal))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.validIntfAddrVal))), Integer.valueOf(HidlSupport.deepHashCode(this.intfAddrVal)), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.validOuiVal))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.ouiVal))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.validRandomFactorForceVal))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.randomFactorForceVal))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.validHopCountForceVal))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.hopCountForceVal))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.validDiscoveryChannelVal))), Integer.valueOf(HidlSupport.deepHashCode(this.discoveryChannelMhzVal)), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.validUseBeaconsInBandVal))), Integer.valueOf(HidlSupport.deepHashCode(this.useBeaconsInBandVal)), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.validUseSdfInBandVal))), Integer.valueOf(HidlSupport.deepHashCode(this.useSdfInBandVal)));
    }

    public final String toString() {
        return "{.validClusterIdVals = " + this.validClusterIdVals + ", .clusterIdBottomRangeVal = " + ((int) this.clusterIdBottomRangeVal) + ", .clusterIdTopRangeVal = " + ((int) this.clusterIdTopRangeVal) + ", .validIntfAddrVal = " + this.validIntfAddrVal + ", .intfAddrVal = " + Arrays.toString(this.intfAddrVal) + ", .validOuiVal = " + this.validOuiVal + ", .ouiVal = " + this.ouiVal + ", .validRandomFactorForceVal = " + this.validRandomFactorForceVal + ", .randomFactorForceVal = " + ((int) this.randomFactorForceVal) + ", .validHopCountForceVal = " + this.validHopCountForceVal + ", .hopCountForceVal = " + ((int) this.hopCountForceVal) + ", .validDiscoveryChannelVal = " + this.validDiscoveryChannelVal + ", .discoveryChannelMhzVal = " + Arrays.toString(this.discoveryChannelMhzVal) + ", .validUseBeaconsInBandVal = " + this.validUseBeaconsInBandVal + ", .useBeaconsInBandVal = " + Arrays.toString(this.useBeaconsInBandVal) + ", .validUseSdfInBandVal = " + this.validUseSdfInBandVal + ", .useSdfInBandVal = " + Arrays.toString(this.useSdfInBandVal) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(44L), 0L);
    }

    public static final ArrayList<NanDebugConfig> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanDebugConfig> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 44, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanDebugConfig nanDebugConfig = new NanDebugConfig();
            nanDebugConfig.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 44);
            arrayList.add(nanDebugConfig);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.validClusterIdVals = hwBlob.getBool(0 + j);
        this.clusterIdBottomRangeVal = hwBlob.getInt16(2 + j);
        this.clusterIdTopRangeVal = hwBlob.getInt16(4 + j);
        this.validIntfAddrVal = hwBlob.getBool(6 + j);
        hwBlob.copyToInt8Array(7 + j, this.intfAddrVal, 6);
        this.validOuiVal = hwBlob.getBool(13 + j);
        this.ouiVal = hwBlob.getInt32(16 + j);
        this.validRandomFactorForceVal = hwBlob.getBool(20 + j);
        this.randomFactorForceVal = hwBlob.getInt8(21 + j);
        this.validHopCountForceVal = hwBlob.getBool(22 + j);
        this.hopCountForceVal = hwBlob.getInt8(23 + j);
        this.validDiscoveryChannelVal = hwBlob.getBool(24 + j);
        hwBlob.copyToInt32Array(28 + j, this.discoveryChannelMhzVal, 2);
        this.validUseBeaconsInBandVal = hwBlob.getBool(36 + j);
        hwBlob.copyToBoolArray(37 + j, this.useBeaconsInBandVal, 2);
        this.validUseSdfInBandVal = hwBlob.getBool(39 + j);
        hwBlob.copyToBoolArray(j + 40, this.useSdfInBandVal, 2);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(44);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanDebugConfig> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 44);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 44);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putBool(0 + j, this.validClusterIdVals);
        hwBlob.putInt16(2 + j, this.clusterIdBottomRangeVal);
        hwBlob.putInt16(4 + j, this.clusterIdTopRangeVal);
        hwBlob.putBool(6 + j, this.validIntfAddrVal);
        hwBlob.putInt8Array(7 + j, this.intfAddrVal);
        hwBlob.putBool(13 + j, this.validOuiVal);
        hwBlob.putInt32(16 + j, this.ouiVal);
        hwBlob.putBool(20 + j, this.validRandomFactorForceVal);
        hwBlob.putInt8(21 + j, this.randomFactorForceVal);
        hwBlob.putBool(22 + j, this.validHopCountForceVal);
        hwBlob.putInt8(23 + j, this.hopCountForceVal);
        hwBlob.putBool(24 + j, this.validDiscoveryChannelVal);
        hwBlob.putInt32Array(28 + j, this.discoveryChannelMhzVal);
        hwBlob.putBool(36 + j, this.validUseBeaconsInBandVal);
        hwBlob.putBoolArray(37 + j, this.useBeaconsInBandVal);
        hwBlob.putBool(39 + j, this.validUseSdfInBandVal);
        hwBlob.putBoolArray(j + 40, this.useSdfInBandVal);
    }
}
