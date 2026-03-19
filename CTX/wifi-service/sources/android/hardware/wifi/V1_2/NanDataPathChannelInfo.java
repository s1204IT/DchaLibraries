package android.hardware.wifi.V1_2;

import android.hardware.wifi.V1_0.WifiChannelWidthInMhz;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class NanDataPathChannelInfo {
    public int channelBandwidth;
    public int channelFreq;
    public int numSpatialStreams;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanDataPathChannelInfo.class) {
            return false;
        }
        NanDataPathChannelInfo nanDataPathChannelInfo = (NanDataPathChannelInfo) obj;
        if (this.channelFreq == nanDataPathChannelInfo.channelFreq && this.channelBandwidth == nanDataPathChannelInfo.channelBandwidth && this.numSpatialStreams == nanDataPathChannelInfo.numSpatialStreams) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.channelFreq))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.channelBandwidth))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.numSpatialStreams))));
    }

    public final String toString() {
        return "{.channelFreq = " + this.channelFreq + ", .channelBandwidth = " + WifiChannelWidthInMhz.toString(this.channelBandwidth) + ", .numSpatialStreams = " + this.numSpatialStreams + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(12L), 0L);
    }

    public static final ArrayList<NanDataPathChannelInfo> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanDataPathChannelInfo> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 12, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanDataPathChannelInfo nanDataPathChannelInfo = new NanDataPathChannelInfo();
            nanDataPathChannelInfo.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 12);
            arrayList.add(nanDataPathChannelInfo);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.channelFreq = hwBlob.getInt32(0 + j);
        this.channelBandwidth = hwBlob.getInt32(4 + j);
        this.numSpatialStreams = hwBlob.getInt32(j + 8);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(12);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanDataPathChannelInfo> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 12);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 12);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.channelFreq);
        hwBlob.putInt32(4 + j, this.channelBandwidth);
        hwBlob.putInt32(j + 8, this.numSpatialStreams);
    }
}
