package android.hardware.wifi.V1_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class RttLciInformation {
    public int altitude;
    public byte altitudeUnc;
    public int floor;
    public int heightAboveFloor;
    public int heightUnc;
    public long latitude;
    public byte latitudeUnc;
    public long longitude;
    public byte longitudeUnc;
    public int motionPattern;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != RttLciInformation.class) {
            return false;
        }
        RttLciInformation rttLciInformation = (RttLciInformation) obj;
        if (this.latitude == rttLciInformation.latitude && this.longitude == rttLciInformation.longitude && this.altitude == rttLciInformation.altitude && this.latitudeUnc == rttLciInformation.latitudeUnc && this.longitudeUnc == rttLciInformation.longitudeUnc && this.altitudeUnc == rttLciInformation.altitudeUnc && this.motionPattern == rttLciInformation.motionPattern && this.floor == rttLciInformation.floor && this.heightAboveFloor == rttLciInformation.heightAboveFloor && this.heightUnc == rttLciInformation.heightUnc) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.latitude))), Integer.valueOf(HidlSupport.deepHashCode(Long.valueOf(this.longitude))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.altitude))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.latitudeUnc))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.longitudeUnc))), Integer.valueOf(HidlSupport.deepHashCode(Byte.valueOf(this.altitudeUnc))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.motionPattern))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.floor))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.heightAboveFloor))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.heightUnc))));
    }

    public final String toString() {
        return "{.latitude = " + this.latitude + ", .longitude = " + this.longitude + ", .altitude = " + this.altitude + ", .latitudeUnc = " + ((int) this.latitudeUnc) + ", .longitudeUnc = " + ((int) this.longitudeUnc) + ", .altitudeUnc = " + ((int) this.altitudeUnc) + ", .motionPattern = " + RttMotionPattern.toString(this.motionPattern) + ", .floor = " + this.floor + ", .heightAboveFloor = " + this.heightAboveFloor + ", .heightUnc = " + this.heightUnc + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(40L), 0L);
    }

    public static final ArrayList<RttLciInformation> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<RttLciInformation> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 40, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            RttLciInformation rttLciInformation = new RttLciInformation();
            rttLciInformation.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 40);
            arrayList.add(rttLciInformation);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.latitude = hwBlob.getInt64(0 + j);
        this.longitude = hwBlob.getInt64(8 + j);
        this.altitude = hwBlob.getInt32(16 + j);
        this.latitudeUnc = hwBlob.getInt8(20 + j);
        this.longitudeUnc = hwBlob.getInt8(21 + j);
        this.altitudeUnc = hwBlob.getInt8(22 + j);
        this.motionPattern = hwBlob.getInt32(24 + j);
        this.floor = hwBlob.getInt32(28 + j);
        this.heightAboveFloor = hwBlob.getInt32(32 + j);
        this.heightUnc = hwBlob.getInt32(j + 36);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(40);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<RttLciInformation> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 40);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 40);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt64(0 + j, this.latitude);
        hwBlob.putInt64(8 + j, this.longitude);
        hwBlob.putInt32(16 + j, this.altitude);
        hwBlob.putInt8(20 + j, this.latitudeUnc);
        hwBlob.putInt8(21 + j, this.longitudeUnc);
        hwBlob.putInt8(22 + j, this.altitudeUnc);
        hwBlob.putInt32(24 + j, this.motionPattern);
        hwBlob.putInt32(28 + j, this.floor);
        hwBlob.putInt32(32 + j, this.heightAboveFloor);
        hwBlob.putInt32(j + 36, this.heightUnc);
    }
}
