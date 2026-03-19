package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class StaRoamingConfig {
    public final ArrayList<byte[]> bssidBlacklist = new ArrayList<>();
    public final ArrayList<byte[]> ssidWhitelist = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != StaRoamingConfig.class) {
            return false;
        }
        StaRoamingConfig staRoamingConfig = (StaRoamingConfig) obj;
        if (HidlSupport.deepEquals(this.bssidBlacklist, staRoamingConfig.bssidBlacklist) && HidlSupport.deepEquals(this.ssidWhitelist, staRoamingConfig.ssidWhitelist)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.bssidBlacklist)), Integer.valueOf(HidlSupport.deepHashCode(this.ssidWhitelist)));
    }

    public final String toString() {
        return "{.bssidBlacklist = " + this.bssidBlacklist + ", .ssidWhitelist = " + this.ssidWhitelist + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(32L), 0L);
    }

    public static final ArrayList<StaRoamingConfig> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<StaRoamingConfig> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 32, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            StaRoamingConfig staRoamingConfig = new StaRoamingConfig();
            staRoamingConfig.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 32);
            arrayList.add(staRoamingConfig);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        long j2 = j + 0;
        int int32 = hwBlob.getInt32(j2 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 6, hwBlob.handle(), j2 + 0, true);
        this.bssidBlacklist.clear();
        for (int i = 0; i < int32; i++) {
            byte[] bArr = new byte[6];
            embeddedBuffer.copyToInt8Array(i * 6, bArr, 6);
            this.bssidBlacklist.add(bArr);
        }
        long j3 = j + 16;
        int int322 = hwBlob.getInt32(8 + j3);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 32, hwBlob.handle(), j3 + 0, true);
        this.ssidWhitelist.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            byte[] bArr2 = new byte[32];
            embeddedBuffer2.copyToInt8Array(i2 * 32, bArr2, 32);
            this.ssidWhitelist.add(bArr2);
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(32);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<StaRoamingConfig> arrayList) {
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
        int size = this.bssidBlacklist.size();
        long j2 = j + 0;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 6);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8Array(i * 6, this.bssidBlacklist.get(i));
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        int size2 = this.ssidWhitelist.size();
        long j3 = j + 16;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 32);
        for (int i2 = 0; i2 < size2; i2++) {
            hwBlob3.putInt8Array(i2 * 32, this.ssidWhitelist.get(i2));
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
    }
}
