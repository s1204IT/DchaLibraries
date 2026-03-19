package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class NanSubscribeRequest {
    public final NanDiscoveryCommonConfig baseConfigs = new NanDiscoveryCommonConfig();
    public final ArrayList<byte[]> intfAddr = new ArrayList<>();
    public boolean isSsiRequiredForMatch;
    public boolean shouldUseSrf;
    public boolean srfRespondIfInAddressSet;
    public int srfType;
    public int subscribeType;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanSubscribeRequest.class) {
            return false;
        }
        NanSubscribeRequest nanSubscribeRequest = (NanSubscribeRequest) obj;
        if (HidlSupport.deepEquals(this.baseConfigs, nanSubscribeRequest.baseConfigs) && this.subscribeType == nanSubscribeRequest.subscribeType && this.srfType == nanSubscribeRequest.srfType && this.srfRespondIfInAddressSet == nanSubscribeRequest.srfRespondIfInAddressSet && this.shouldUseSrf == nanSubscribeRequest.shouldUseSrf && this.isSsiRequiredForMatch == nanSubscribeRequest.isSsiRequiredForMatch && HidlSupport.deepEquals(this.intfAddr, nanSubscribeRequest.intfAddr)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.baseConfigs)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.subscribeType))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.srfType))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.srfRespondIfInAddressSet))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.shouldUseSrf))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.isSsiRequiredForMatch))), Integer.valueOf(HidlSupport.deepHashCode(this.intfAddr)));
    }

    public final String toString() {
        return "{.baseConfigs = " + this.baseConfigs + ", .subscribeType = " + NanSubscribeType.toString(this.subscribeType) + ", .srfType = " + NanSrfType.toString(this.srfType) + ", .srfRespondIfInAddressSet = " + this.srfRespondIfInAddressSet + ", .shouldUseSrf = " + this.shouldUseSrf + ", .isSsiRequiredForMatch = " + this.isSsiRequiredForMatch + ", .intfAddr = " + this.intfAddr + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(208L), 0L);
    }

    public static final ArrayList<NanSubscribeRequest> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanSubscribeRequest> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 208, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanSubscribeRequest nanSubscribeRequest = new NanSubscribeRequest();
            nanSubscribeRequest.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 208);
            arrayList.add(nanSubscribeRequest);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.baseConfigs.readEmbeddedFromParcel(hwParcel, hwBlob, j + 0);
        this.subscribeType = hwBlob.getInt32(j + 176);
        this.srfType = hwBlob.getInt32(j + 180);
        this.srfRespondIfInAddressSet = hwBlob.getBool(j + 184);
        this.shouldUseSrf = hwBlob.getBool(j + 185);
        this.isSsiRequiredForMatch = hwBlob.getBool(j + 186);
        long j2 = j + 192;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 6, hwBlob.handle(), j2 + 0, true);
        this.intfAddr.clear();
        for (int i = 0; i < int32; i++) {
            byte[] bArr = new byte[6];
            embeddedBuffer.copyToInt8Array(i * 6, bArr, 6);
            this.intfAddr.add(bArr);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(208);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanSubscribeRequest> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 208);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 208);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        this.baseConfigs.writeEmbeddedToBlob(hwBlob, j + 0);
        hwBlob.putInt32(176 + j, this.subscribeType);
        hwBlob.putInt32(180 + j, this.srfType);
        hwBlob.putBool(184 + j, this.srfRespondIfInAddressSet);
        hwBlob.putBool(185 + j, this.shouldUseSrf);
        hwBlob.putBool(186 + j, this.isSsiRequiredForMatch);
        int size = this.intfAddr.size();
        long j2 = j + 192;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 6);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8Array(i * 6, this.intfAddr.get(i));
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
