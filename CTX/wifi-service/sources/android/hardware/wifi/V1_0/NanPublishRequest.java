package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import com.android.server.wifi.WifiConfigManager;
import java.util.ArrayList;
import java.util.Objects;

public final class NanPublishRequest {
    public boolean autoAcceptDataPathRequests;
    public final NanDiscoveryCommonConfig baseConfigs = new NanDiscoveryCommonConfig();
    public int publishType;
    public int txType;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanPublishRequest.class) {
            return false;
        }
        NanPublishRequest nanPublishRequest = (NanPublishRequest) obj;
        if (HidlSupport.deepEquals(this.baseConfigs, nanPublishRequest.baseConfigs) && this.publishType == nanPublishRequest.publishType && this.txType == nanPublishRequest.txType && this.autoAcceptDataPathRequests == nanPublishRequest.autoAcceptDataPathRequests) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.baseConfigs)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.publishType))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.txType))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.autoAcceptDataPathRequests))));
    }

    public final String toString() {
        return "{.baseConfigs = " + this.baseConfigs + ", .publishType = " + NanPublishType.toString(this.publishType) + ", .txType = " + NanTxType.toString(this.txType) + ", .autoAcceptDataPathRequests = " + this.autoAcceptDataPathRequests + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(192L), 0L);
    }

    public static final ArrayList<NanPublishRequest> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanPublishRequest> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * WifiConfigManager.SCAN_CACHE_ENTRIES_MAX_SIZE, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanPublishRequest nanPublishRequest = new NanPublishRequest();
            nanPublishRequest.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * WifiConfigManager.SCAN_CACHE_ENTRIES_MAX_SIZE);
            arrayList.add(nanPublishRequest);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.baseConfigs.readEmbeddedFromParcel(hwParcel, hwBlob, 0 + j);
        this.publishType = hwBlob.getInt32(176 + j);
        this.txType = hwBlob.getInt32(180 + j);
        this.autoAcceptDataPathRequests = hwBlob.getBool(j + 184);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(WifiConfigManager.SCAN_CACHE_ENTRIES_MAX_SIZE);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanPublishRequest> arrayList) {
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
        this.baseConfigs.writeEmbeddedToBlob(hwBlob, 0 + j);
        hwBlob.putInt32(176 + j, this.publishType);
        hwBlob.putInt32(180 + j, this.txType);
        hwBlob.putBool(j + 184, this.autoAcceptDataPathRequests);
    }
}
