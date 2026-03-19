package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class NanDataPathSecurityConfig {
    public int cipherType;
    public int securityType;
    public final byte[] pmk = new byte[32];
    public final ArrayList<Byte> passphrase = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanDataPathSecurityConfig.class) {
            return false;
        }
        NanDataPathSecurityConfig nanDataPathSecurityConfig = (NanDataPathSecurityConfig) obj;
        if (this.securityType == nanDataPathSecurityConfig.securityType && this.cipherType == nanDataPathSecurityConfig.cipherType && HidlSupport.deepEquals(this.pmk, nanDataPathSecurityConfig.pmk) && HidlSupport.deepEquals(this.passphrase, nanDataPathSecurityConfig.passphrase)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.securityType))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cipherType))), Integer.valueOf(HidlSupport.deepHashCode(this.pmk)), Integer.valueOf(HidlSupport.deepHashCode(this.passphrase)));
    }

    public final String toString() {
        return "{.securityType = " + NanDataPathSecurityType.toString(this.securityType) + ", .cipherType = " + NanCipherSuiteType.toString(this.cipherType) + ", .pmk = " + Arrays.toString(this.pmk) + ", .passphrase = " + this.passphrase + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(56L), 0L);
    }

    public static final ArrayList<NanDataPathSecurityConfig> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanDataPathSecurityConfig> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 56, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanDataPathSecurityConfig nanDataPathSecurityConfig = new NanDataPathSecurityConfig();
            nanDataPathSecurityConfig.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 56);
            arrayList.add(nanDataPathSecurityConfig);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.securityType = hwBlob.getInt32(j + 0);
        this.cipherType = hwBlob.getInt32(j + 4);
        hwBlob.copyToInt8Array(j + 8, this.pmk, 32);
        long j2 = j + 40;
        int int32 = hwBlob.getInt32(8 + j2);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j2 + 0, true);
        this.passphrase.clear();
        for (int i = 0; i < int32; i++) {
            this.passphrase.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(56);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanDataPathSecurityConfig> arrayList) {
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
        hwBlob.putInt32(j + 0, this.securityType);
        hwBlob.putInt32(4 + j, this.cipherType);
        hwBlob.putInt8Array(j + 8, this.pmk);
        int size = this.passphrase.size();
        long j2 = j + 40;
        hwBlob.putInt32(8 + j2, size);
        hwBlob.putBool(12 + j2, false);
        HwBlob hwBlob2 = new HwBlob(size * 1);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8(i * 1, this.passphrase.get(i).byteValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
    }
}
