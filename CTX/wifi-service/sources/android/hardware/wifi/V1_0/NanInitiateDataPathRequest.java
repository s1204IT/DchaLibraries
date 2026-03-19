package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class NanInitiateDataPathRequest {
    public int channel;
    public int channelRequestType;
    public int peerId;
    public final byte[] peerDiscMacAddr = new byte[6];
    public String ifaceName = new String();
    public final NanDataPathSecurityConfig securityConfig = new NanDataPathSecurityConfig();
    public final ArrayList<Byte> appInfo = new ArrayList<>();
    public final ArrayList<Byte> serviceNameOutOfBand = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanInitiateDataPathRequest.class) {
            return false;
        }
        NanInitiateDataPathRequest nanInitiateDataPathRequest = (NanInitiateDataPathRequest) obj;
        if (this.peerId == nanInitiateDataPathRequest.peerId && HidlSupport.deepEquals(this.peerDiscMacAddr, nanInitiateDataPathRequest.peerDiscMacAddr) && this.channelRequestType == nanInitiateDataPathRequest.channelRequestType && this.channel == nanInitiateDataPathRequest.channel && HidlSupport.deepEquals(this.ifaceName, nanInitiateDataPathRequest.ifaceName) && HidlSupport.deepEquals(this.securityConfig, nanInitiateDataPathRequest.securityConfig) && HidlSupport.deepEquals(this.appInfo, nanInitiateDataPathRequest.appInfo) && HidlSupport.deepEquals(this.serviceNameOutOfBand, nanInitiateDataPathRequest.serviceNameOutOfBand)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.peerId))), Integer.valueOf(HidlSupport.deepHashCode(this.peerDiscMacAddr)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.channelRequestType))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.channel))), Integer.valueOf(HidlSupport.deepHashCode(this.ifaceName)), Integer.valueOf(HidlSupport.deepHashCode(this.securityConfig)), Integer.valueOf(HidlSupport.deepHashCode(this.appInfo)), Integer.valueOf(HidlSupport.deepHashCode(this.serviceNameOutOfBand)));
    }

    public final String toString() {
        return "{.peerId = " + this.peerId + ", .peerDiscMacAddr = " + Arrays.toString(this.peerDiscMacAddr) + ", .channelRequestType = " + NanDataPathChannelCfg.toString(this.channelRequestType) + ", .channel = " + this.channel + ", .ifaceName = " + this.ifaceName + ", .securityConfig = " + this.securityConfig + ", .appInfo = " + this.appInfo + ", .serviceNameOutOfBand = " + this.serviceNameOutOfBand + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(128L), 0L);
    }

    public static final ArrayList<NanInitiateDataPathRequest> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanInitiateDataPathRequest> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 128, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanInitiateDataPathRequest nanInitiateDataPathRequest = new NanInitiateDataPathRequest();
            nanInitiateDataPathRequest.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 128);
            arrayList.add(nanInitiateDataPathRequest);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.peerId = hwBlob.getInt32(j + 0);
        hwBlob.copyToInt8Array(j + 4, this.peerDiscMacAddr, 6);
        this.channelRequestType = hwBlob.getInt32(j + 12);
        this.channel = hwBlob.getInt32(j + 16);
        long j2 = j + 24;
        this.ifaceName = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.ifaceName.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.securityConfig.readEmbeddedFromParcel(hwParcel, hwBlob, j + 40);
        long j3 = j + 96;
        int int32 = hwBlob.getInt32(j3 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j3 + 0, true);
        this.appInfo.clear();
        for (int i = 0; i < int32; i++) {
            this.appInfo.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
        long j4 = j + 112;
        int int322 = hwBlob.getInt32(j4 + 8);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 1, hwBlob.handle(), j4 + 0, true);
        this.serviceNameOutOfBand.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            this.serviceNameOutOfBand.add(Byte.valueOf(embeddedBuffer2.getInt8(i2 * 1)));
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(128);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanInitiateDataPathRequest> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 128);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 128);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(j + 0, this.peerId);
        hwBlob.putInt8Array(j + 4, this.peerDiscMacAddr);
        hwBlob.putInt32(j + 12, this.channelRequestType);
        hwBlob.putInt32(j + 16, this.channel);
        hwBlob.putString(j + 24, this.ifaceName);
        this.securityConfig.writeEmbeddedToBlob(hwBlob, j + 40);
        int size = this.appInfo.size();
        long j2 = j + 96;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 1);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8(i * 1, this.appInfo.get(i).byteValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        int size2 = this.serviceNameOutOfBand.size();
        long j3 = j + 112;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(12 + j3, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 1);
        for (int i2 = 0; i2 < size2; i2++) {
            hwBlob3.putInt8(i2 * 1, this.serviceNameOutOfBand.get(i2).byteValue());
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
    }
}
