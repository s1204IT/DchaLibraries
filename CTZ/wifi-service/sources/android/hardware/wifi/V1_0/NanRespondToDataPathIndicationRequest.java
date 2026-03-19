package android.hardware.wifi.V1_0;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class NanRespondToDataPathIndicationRequest {
    public boolean acceptRequest;
    public int ndpInstanceId;
    public String ifaceName = new String();
    public final NanDataPathSecurityConfig securityConfig = new NanDataPathSecurityConfig();
    public final ArrayList<Byte> appInfo = new ArrayList<>();
    public final ArrayList<Byte> serviceNameOutOfBand = new ArrayList<>();

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != NanRespondToDataPathIndicationRequest.class) {
            return false;
        }
        NanRespondToDataPathIndicationRequest nanRespondToDataPathIndicationRequest = (NanRespondToDataPathIndicationRequest) obj;
        if (this.acceptRequest == nanRespondToDataPathIndicationRequest.acceptRequest && this.ndpInstanceId == nanRespondToDataPathIndicationRequest.ndpInstanceId && HidlSupport.deepEquals(this.ifaceName, nanRespondToDataPathIndicationRequest.ifaceName) && HidlSupport.deepEquals(this.securityConfig, nanRespondToDataPathIndicationRequest.securityConfig) && HidlSupport.deepEquals(this.appInfo, nanRespondToDataPathIndicationRequest.appInfo) && HidlSupport.deepEquals(this.serviceNameOutOfBand, nanRespondToDataPathIndicationRequest.serviceNameOutOfBand)) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Boolean.valueOf(this.acceptRequest))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.ndpInstanceId))), Integer.valueOf(HidlSupport.deepHashCode(this.ifaceName)), Integer.valueOf(HidlSupport.deepHashCode(this.securityConfig)), Integer.valueOf(HidlSupport.deepHashCode(this.appInfo)), Integer.valueOf(HidlSupport.deepHashCode(this.serviceNameOutOfBand)));
    }

    public final String toString() {
        return "{.acceptRequest = " + this.acceptRequest + ", .ndpInstanceId = " + this.ndpInstanceId + ", .ifaceName = " + this.ifaceName + ", .securityConfig = " + this.securityConfig + ", .appInfo = " + this.appInfo + ", .serviceNameOutOfBand = " + this.serviceNameOutOfBand + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(112L), 0L);
    }

    public static final ArrayList<NanRespondToDataPathIndicationRequest> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<NanRespondToDataPathIndicationRequest> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * ISupplicantStaIfaceCallback.StatusCode.FILS_AUTHENTICATION_FAILURE, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            NanRespondToDataPathIndicationRequest nanRespondToDataPathIndicationRequest = new NanRespondToDataPathIndicationRequest();
            nanRespondToDataPathIndicationRequest.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * ISupplicantStaIfaceCallback.StatusCode.FILS_AUTHENTICATION_FAILURE);
            arrayList.add(nanRespondToDataPathIndicationRequest);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.acceptRequest = hwBlob.getBool(j + 0);
        this.ndpInstanceId = hwBlob.getInt32(j + 4);
        long j2 = j + 8;
        this.ifaceName = hwBlob.getString(j2);
        hwParcel.readEmbeddedBuffer(this.ifaceName.getBytes().length + 1, hwBlob.handle(), j2 + 0, false);
        this.securityConfig.readEmbeddedFromParcel(hwParcel, hwBlob, j + 24);
        long j3 = j + 80;
        int int32 = hwBlob.getInt32(j3 + 8);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 1, hwBlob.handle(), j3 + 0, true);
        this.appInfo.clear();
        for (int i = 0; i < int32; i++) {
            this.appInfo.add(Byte.valueOf(embeddedBuffer.getInt8(i * 1)));
        }
        long j4 = j + 96;
        int int322 = hwBlob.getInt32(8 + j4);
        HwBlob embeddedBuffer2 = hwParcel.readEmbeddedBuffer(int322 * 1, hwBlob.handle(), j4 + 0, true);
        this.serviceNameOutOfBand.clear();
        for (int i2 = 0; i2 < int322; i2++) {
            this.serviceNameOutOfBand.add(Byte.valueOf(embeddedBuffer2.getInt8(i2 * 1)));
        }
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(ISupplicantStaIfaceCallback.StatusCode.FILS_AUTHENTICATION_FAILURE);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<NanRespondToDataPathIndicationRequest> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * ISupplicantStaIfaceCallback.StatusCode.FILS_AUTHENTICATION_FAILURE);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * ISupplicantStaIfaceCallback.StatusCode.FILS_AUTHENTICATION_FAILURE);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putBool(j + 0, this.acceptRequest);
        hwBlob.putInt32(j + 4, this.ndpInstanceId);
        hwBlob.putString(j + 8, this.ifaceName);
        this.securityConfig.writeEmbeddedToBlob(hwBlob, j + 24);
        int size = this.appInfo.size();
        long j2 = j + 80;
        hwBlob.putInt32(j2 + 8, size);
        hwBlob.putBool(j2 + 12, false);
        HwBlob hwBlob2 = new HwBlob(size * 1);
        for (int i = 0; i < size; i++) {
            hwBlob2.putInt8(i * 1, this.appInfo.get(i).byteValue());
        }
        hwBlob.putBlob(j2 + 0, hwBlob2);
        int size2 = this.serviceNameOutOfBand.size();
        long j3 = j + 96;
        hwBlob.putInt32(8 + j3, size2);
        hwBlob.putBool(j3 + 12, false);
        HwBlob hwBlob3 = new HwBlob(size2 * 1);
        for (int i2 = 0; i2 < size2; i2++) {
            hwBlob3.putInt8(i2 * 1, this.serviceNameOutOfBand.get(i2).byteValue());
        }
        hwBlob.putBlob(j3 + 0, hwBlob3);
    }
}
