package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class RttConfig {
    public int burstDuration;
    public int burstPeriod;
    public int bw;
    public boolean mustRequestLci;
    public boolean mustRequestLcr;
    public int numBurst;
    public int numFramesPerBurst;
    public int numRetriesPerFtmr;
    public int numRetriesPerRttFrame;
    public int peer;
    public int preamble;
    public int type;
    public final byte[] addr = new byte[6];
    public final WifiChannelInfo channel = new WifiChannelInfo();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != RttConfig.class) {
            return false;
        }
        RttConfig rttConfig = (RttConfig) obj;
        if (HidlSupport.deepEquals(this.addr, rttConfig.addr) && this.type == rttConfig.type && this.peer == rttConfig.peer && HidlSupport.deepEquals(this.channel, rttConfig.channel) && this.burstPeriod == rttConfig.burstPeriod && this.numBurst == rttConfig.numBurst && this.numFramesPerBurst == rttConfig.numFramesPerBurst && this.numRetriesPerRttFrame == rttConfig.numRetriesPerRttFrame && this.numRetriesPerFtmr == rttConfig.numRetriesPerFtmr && this.mustRequestLci == rttConfig.mustRequestLci && this.mustRequestLcr == rttConfig.mustRequestLcr && this.burstDuration == rttConfig.burstDuration && this.preamble == rttConfig.preamble && this.bw == rttConfig.bw) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(this.addr)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.type))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.peer))), Integer.valueOf(HidlSupport.deepHashCode(this.channel)), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.burstPeriod))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.numBurst))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.numFramesPerBurst))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.numRetriesPerRttFrame))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.numRetriesPerFtmr))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.mustRequestLci))), Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.mustRequestLcr))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.burstDuration))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.preamble))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.bw))));
    }

    public final String toString() {
        return "{.addr = " + Arrays.toString(this.addr) + ", .type = " + RttType.toString(this.type) + ", .peer = " + RttPeerType.toString(this.peer) + ", .channel = " + this.channel + ", .burstPeriod = " + this.burstPeriod + ", .numBurst = " + this.numBurst + ", .numFramesPerBurst = " + this.numFramesPerBurst + ", .numRetriesPerRttFrame = " + this.numRetriesPerRttFrame + ", .numRetriesPerFtmr = " + this.numRetriesPerFtmr + ", .mustRequestLci = " + this.mustRequestLci + ", .mustRequestLcr = " + this.mustRequestLcr + ", .burstDuration = " + this.burstDuration + ", .preamble = " + RttPreamble.toString(this.preamble) + ", .bw = " + RttBw.toString(this.bw) + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(68L), 0L);
    }

    public static final ArrayList<RttConfig> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<RttConfig> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 68, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            RttConfig rttConfig = new RttConfig();
            rttConfig.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 68);
            arrayList.add(rttConfig);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        hwBlob.copyToInt8Array(0 + j, this.addr, 6);
        this.type = hwBlob.getInt32(8 + j);
        this.peer = hwBlob.getInt32(12 + j);
        this.channel.readEmbeddedFromParcel(hwParcel, hwBlob, 16 + j);
        this.burstPeriod = hwBlob.getInt32(32 + j);
        this.numBurst = hwBlob.getInt32(36 + j);
        this.numFramesPerBurst = hwBlob.getInt32(40 + j);
        this.numRetriesPerRttFrame = hwBlob.getInt32(44 + j);
        this.numRetriesPerFtmr = hwBlob.getInt32(48 + j);
        this.mustRequestLci = hwBlob.getBool(52 + j);
        this.mustRequestLcr = hwBlob.getBool(53 + j);
        this.burstDuration = hwBlob.getInt32(56 + j);
        this.preamble = hwBlob.getInt32(60 + j);
        this.bw = hwBlob.getInt32(j + 64);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(68);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<RttConfig> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 68);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 68);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt8Array(0 + j, this.addr);
        hwBlob.putInt32(8 + j, this.type);
        hwBlob.putInt32(12 + j, this.peer);
        this.channel.writeEmbeddedToBlob(hwBlob, 16 + j);
        hwBlob.putInt32(32 + j, this.burstPeriod);
        hwBlob.putInt32(36 + j, this.numBurst);
        hwBlob.putInt32(40 + j, this.numFramesPerBurst);
        hwBlob.putInt32(44 + j, this.numRetriesPerRttFrame);
        hwBlob.putInt32(48 + j, this.numRetriesPerFtmr);
        hwBlob.putBool(52 + j, this.mustRequestLci);
        hwBlob.putBool(53 + j, this.mustRequestLcr);
        hwBlob.putInt32(56 + j, this.burstDuration);
        hwBlob.putInt32(60 + j, this.preamble);
        hwBlob.putInt32(j + 64, this.bw);
    }
}
