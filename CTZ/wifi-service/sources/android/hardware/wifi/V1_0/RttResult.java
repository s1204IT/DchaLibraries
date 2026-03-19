package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import com.android.server.wifi.WifiConfigManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class RttResult {
    public int burstDurationInMs;
    public int burstNum;
    public int distanceInMm;
    public int distanceSdInMm;
    public int distanceSpreadInMm;
    public int measurementNumber;
    public int negotiatedBurstNum;
    public byte numberPerBurstPeer;
    public byte retryAfterDuration;
    public int rssi;
    public int rssiSpread;
    public long rtt;
    public long rttSd;
    public long rttSpread;
    public int status;
    public int successNumber;
    public long timeStampInUs;
    public int type;
    public final byte[] addr = new byte[6];
    public final WifiRateInfo txRate = new WifiRateInfo();
    public final WifiRateInfo rxRate = new WifiRateInfo();
    public final WifiInformationElement lci = new WifiInformationElement();
    public final WifiInformationElement lcr = new WifiInformationElement();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != RttResult.class) {
            return false;
        }
        RttResult rttResult = (RttResult) obj;
        if (HidlSupport.deepEquals(this.addr, rttResult.addr) && this.burstNum == rttResult.burstNum && this.measurementNumber == rttResult.measurementNumber && this.successNumber == rttResult.successNumber && this.numberPerBurstPeer == rttResult.numberPerBurstPeer && this.status == rttResult.status && this.retryAfterDuration == rttResult.retryAfterDuration && this.type == rttResult.type && this.rssi == rttResult.rssi && this.rssiSpread == rttResult.rssiSpread && HidlSupport.deepEquals(this.txRate, rttResult.txRate) && HidlSupport.deepEquals(this.rxRate, rttResult.rxRate) && this.rtt == rttResult.rtt && this.rttSd == rttResult.rttSd && this.rttSpread == rttResult.rttSpread && this.distanceInMm == rttResult.distanceInMm && this.distanceSdInMm == rttResult.distanceSdInMm && this.distanceSpreadInMm == rttResult.distanceSpreadInMm && this.timeStampInUs == rttResult.timeStampInUs && this.burstDurationInMs == rttResult.burstDurationInMs && this.negotiatedBurstNum == rttResult.negotiatedBurstNum && HidlSupport.deepEquals(this.lci, rttResult.lci) && HidlSupport.deepEquals(this.lcr, rttResult.lcr)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.addr)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.burstNum))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.measurementNumber))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.successNumber))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.numberPerBurstPeer))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.status))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.retryAfterDuration))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.rssi))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.rssiSpread))), Integer.valueOf(HidlSupport.deepHashCode(this.txRate)), Integer.valueOf(HidlSupport.deepHashCode(this.rxRate)), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.rtt))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.rttSd))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.rttSpread))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.distanceInMm))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.distanceSdInMm))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.distanceSpreadInMm))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.timeStampInUs))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.burstDurationInMs))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.negotiatedBurstNum))), Integer.valueOf(HidlSupport.deepHashCode(this.lci)), Integer.valueOf(HidlSupport.deepHashCode(this.lcr)));
    }

    public final String toString() {
        return "{.addr = " + Arrays.toString(this.addr) + ", .burstNum = " + this.burstNum + ", .measurementNumber = " + this.measurementNumber + ", .successNumber = " + this.successNumber + ", .numberPerBurstPeer = " + ((int) this.numberPerBurstPeer) + ", .status = " + RttStatus.toString(this.status) + ", .retryAfterDuration = " + ((int) this.retryAfterDuration) + ", .type = " + RttType.toString(this.type) + ", .rssi = " + this.rssi + ", .rssiSpread = " + this.rssiSpread + ", .txRate = " + this.txRate + ", .rxRate = " + this.rxRate + ", .rtt = " + this.rtt + ", .rttSd = " + this.rttSd + ", .rttSpread = " + this.rttSpread + ", .distanceInMm = " + this.distanceInMm + ", .distanceSdInMm = " + this.distanceSdInMm + ", .distanceSpreadInMm = " + this.distanceSpreadInMm + ", .timeStampInUs = " + this.timeStampInUs + ", .burstDurationInMs = " + this.burstDurationInMs + ", .negotiatedBurstNum = " + this.negotiatedBurstNum + ", .lci = " + this.lci + ", .lcr = " + this.lcr + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(192L), 0L);
    }

    public static final ArrayList<RttResult> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<RttResult> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * WifiConfigManager.SCAN_CACHE_ENTRIES_MAX_SIZE, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            RttResult rttResult = new RttResult();
            rttResult.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * WifiConfigManager.SCAN_CACHE_ENTRIES_MAX_SIZE);
            arrayList.add(rttResult);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        hwBlob.copyToInt8Array(0 + j, this.addr, 6);
        this.burstNum = hwBlob.getInt32(8 + j);
        this.measurementNumber = hwBlob.getInt32(12 + j);
        this.successNumber = hwBlob.getInt32(16 + j);
        this.numberPerBurstPeer = hwBlob.getInt8(20 + j);
        this.status = hwBlob.getInt32(24 + j);
        this.retryAfterDuration = hwBlob.getInt8(28 + j);
        this.type = hwBlob.getInt32(32 + j);
        this.rssi = hwBlob.getInt32(36 + j);
        this.rssiSpread = hwBlob.getInt32(40 + j);
        this.txRate.readEmbeddedFromParcel(hwParcel, hwBlob, 44 + j);
        this.rxRate.readEmbeddedFromParcel(hwParcel, hwBlob, 64 + j);
        this.rtt = hwBlob.getInt64(88 + j);
        this.rttSd = hwBlob.getInt64(96 + j);
        this.rttSpread = hwBlob.getInt64(104 + j);
        this.distanceInMm = hwBlob.getInt32(112 + j);
        this.distanceSdInMm = hwBlob.getInt32(116 + j);
        this.distanceSpreadInMm = hwBlob.getInt32(120 + j);
        this.timeStampInUs = hwBlob.getInt64(128 + j);
        this.burstDurationInMs = hwBlob.getInt32(136 + j);
        this.negotiatedBurstNum = hwBlob.getInt32(140 + j);
        this.lci.readEmbeddedFromParcel(hwParcel, hwBlob, 144 + j);
        this.lcr.readEmbeddedFromParcel(hwParcel, hwBlob, j + 168);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(WifiConfigManager.SCAN_CACHE_ENTRIES_MAX_SIZE);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<RttResult> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * WifiConfigManager.SCAN_CACHE_ENTRIES_MAX_SIZE);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * WifiConfigManager.SCAN_CACHE_ENTRIES_MAX_SIZE);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt8Array(0 + j, this.addr);
        hwBlob.putInt32(8 + j, this.burstNum);
        hwBlob.putInt32(12 + j, this.measurementNumber);
        hwBlob.putInt32(16 + j, this.successNumber);
        hwBlob.putInt8(20 + j, this.numberPerBurstPeer);
        hwBlob.putInt32(24 + j, this.status);
        hwBlob.putInt8(28 + j, this.retryAfterDuration);
        hwBlob.putInt32(32 + j, this.type);
        hwBlob.putInt32(36 + j, this.rssi);
        hwBlob.putInt32(40 + j, this.rssiSpread);
        this.txRate.writeEmbeddedToBlob(hwBlob, 44 + j);
        this.rxRate.writeEmbeddedToBlob(hwBlob, 64 + j);
        hwBlob.putInt64(88 + j, this.rtt);
        hwBlob.putInt64(96 + j, this.rttSd);
        hwBlob.putInt64(104 + j, this.rttSpread);
        hwBlob.putInt32(112 + j, this.distanceInMm);
        hwBlob.putInt32(116 + j, this.distanceSdInMm);
        hwBlob.putInt32(120 + j, this.distanceSpreadInMm);
        hwBlob.putInt64(128 + j, this.timeStampInUs);
        hwBlob.putInt32(136 + j, this.burstDurationInMs);
        hwBlob.putInt32(140 + j, this.negotiatedBurstNum);
        this.lci.writeEmbeddedToBlob(hwBlob, 144 + j);
        this.lcr.writeEmbeddedToBlob(hwBlob, j + 168);
    }
}
