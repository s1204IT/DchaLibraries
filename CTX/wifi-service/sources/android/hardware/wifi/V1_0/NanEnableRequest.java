package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class NanEnableRequest {
    public byte hopCountMax;
    public final boolean[] operateInBand = new boolean[2];
    public final NanConfigRequest configParams = new NanConfigRequest();
    public final NanDebugConfig debugConfigs = new NanDebugConfig();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanEnableRequest.class) {
            return false;
        }
        NanEnableRequest nanEnableRequest = (NanEnableRequest) obj;
        if (HidlSupport.deepEquals(this.operateInBand, nanEnableRequest.operateInBand) && this.hopCountMax == nanEnableRequest.hopCountMax && HidlSupport.deepEquals(this.configParams, nanEnableRequest.configParams) && HidlSupport.deepEquals(this.debugConfigs, nanEnableRequest.debugConfigs)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.operateInBand)), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.hopCountMax))), Integer.valueOf(HidlSupport.deepHashCode(this.configParams)), Integer.valueOf(HidlSupport.deepHashCode(this.debugConfigs)));
    }

    public final String toString() {
        return "{.operateInBand = " + Arrays.toString(this.operateInBand) + ", .hopCountMax = " + ((int) this.hopCountMax) + ", .configParams = " + this.configParams + ", .debugConfigs = " + this.debugConfigs + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(80L), 0L);
    }

    public static final ArrayList<NanEnableRequest> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanEnableRequest> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 80, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanEnableRequest nanEnableRequest = new NanEnableRequest();
            nanEnableRequest.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 80);
            arrayList.add(nanEnableRequest);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        hwBlob.copyToBoolArray(0 + j, this.operateInBand, 2);
        this.hopCountMax = hwBlob.getInt8(2 + j);
        this.configParams.readEmbeddedFromParcel(hwParcel, hwBlob, 4 + j);
        this.debugConfigs.readEmbeddedFromParcel(hwParcel, hwBlob, j + 36);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(80);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanEnableRequest> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 80);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 80);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putBoolArray(0 + j, this.operateInBand);
        hwBlob.putInt8(2 + j, this.hopCountMax);
        this.configParams.writeEmbeddedToBlob(hwBlob, 4 + j);
        this.debugConfigs.writeEmbeddedToBlob(hwBlob, j + 36);
    }
}
