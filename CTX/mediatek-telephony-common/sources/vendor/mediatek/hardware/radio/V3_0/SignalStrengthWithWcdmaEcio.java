package vendor.mediatek.hardware.radio.V3_0;

import android.os.HidlSupport;
import android.os.HwBlob;
import android.os.HwParcel;
import java.util.ArrayList;
import java.util.Objects;

public final class SignalStrengthWithWcdmaEcio {
    public int cdma_dbm;
    public int cdma_ecio;
    public int evdo_dbm;
    public int evdo_ecio;
    public int evdo_signalNoiseRatio;
    public int gsm_bitErrorRate;
    public int gsm_signalStrength;
    public int lte_cqi;
    public int lte_rsrp;
    public int lte_rsrq;
    public int lte_rssnr;
    public int lte_signalStrength;
    public int tdscdma_rscp;
    public int wcdma_ecio;
    public int wcdma_rscp;

    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != SignalStrengthWithWcdmaEcio.class) {
            return false;
        }
        SignalStrengthWithWcdmaEcio signalStrengthWithWcdmaEcio = (SignalStrengthWithWcdmaEcio) obj;
        if (this.gsm_signalStrength == signalStrengthWithWcdmaEcio.gsm_signalStrength && this.gsm_bitErrorRate == signalStrengthWithWcdmaEcio.gsm_bitErrorRate && this.wcdma_rscp == signalStrengthWithWcdmaEcio.wcdma_rscp && this.wcdma_ecio == signalStrengthWithWcdmaEcio.wcdma_ecio && this.cdma_dbm == signalStrengthWithWcdmaEcio.cdma_dbm && this.cdma_ecio == signalStrengthWithWcdmaEcio.cdma_ecio && this.evdo_dbm == signalStrengthWithWcdmaEcio.evdo_dbm && this.evdo_ecio == signalStrengthWithWcdmaEcio.evdo_ecio && this.evdo_signalNoiseRatio == signalStrengthWithWcdmaEcio.evdo_signalNoiseRatio && this.lte_signalStrength == signalStrengthWithWcdmaEcio.lte_signalStrength && this.lte_rsrp == signalStrengthWithWcdmaEcio.lte_rsrp && this.lte_rsrq == signalStrengthWithWcdmaEcio.lte_rsrq && this.lte_rssnr == signalStrengthWithWcdmaEcio.lte_rssnr && this.lte_cqi == signalStrengthWithWcdmaEcio.lte_cqi && this.tdscdma_rscp == signalStrengthWithWcdmaEcio.tdscdma_rscp) {
            return true;
        }
        return false;
    }

    public final int hashCode() {
        return Objects.hash(Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.gsm_signalStrength))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.gsm_bitErrorRate))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.wcdma_rscp))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.wcdma_ecio))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cdma_dbm))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.cdma_ecio))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.evdo_dbm))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.evdo_ecio))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.evdo_signalNoiseRatio))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.lte_signalStrength))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.lte_rsrp))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.lte_rsrq))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.lte_rssnr))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.lte_cqi))), Integer.valueOf(HidlSupport.deepHashCode(Integer.valueOf(this.tdscdma_rscp))));
    }

    public final String toString() {
        return "{.gsm_signalStrength = " + this.gsm_signalStrength + ", .gsm_bitErrorRate = " + this.gsm_bitErrorRate + ", .wcdma_rscp = " + this.wcdma_rscp + ", .wcdma_ecio = " + this.wcdma_ecio + ", .cdma_dbm = " + this.cdma_dbm + ", .cdma_ecio = " + this.cdma_ecio + ", .evdo_dbm = " + this.evdo_dbm + ", .evdo_ecio = " + this.evdo_ecio + ", .evdo_signalNoiseRatio = " + this.evdo_signalNoiseRatio + ", .lte_signalStrength = " + this.lte_signalStrength + ", .lte_rsrp = " + this.lte_rsrp + ", .lte_rsrq = " + this.lte_rsrq + ", .lte_rssnr = " + this.lte_rssnr + ", .lte_cqi = " + this.lte_cqi + ", .tdscdma_rscp = " + this.tdscdma_rscp + "}";
    }

    public final void readFromParcel(HwParcel hwParcel) {
        readEmbeddedFromParcel(hwParcel, hwParcel.readBuffer(60L), 0L);
    }

    public static final ArrayList<SignalStrengthWithWcdmaEcio> readVectorFromParcel(HwParcel hwParcel) {
        ArrayList<SignalStrengthWithWcdmaEcio> arrayList = new ArrayList<>();
        HwBlob buffer = hwParcel.readBuffer(16L);
        int int32 = buffer.getInt32(8L);
        HwBlob embeddedBuffer = hwParcel.readEmbeddedBuffer(int32 * 60, buffer.handle(), 0L, true);
        arrayList.clear();
        for (int i = 0; i < int32; i++) {
            SignalStrengthWithWcdmaEcio signalStrengthWithWcdmaEcio = new SignalStrengthWithWcdmaEcio();
            signalStrengthWithWcdmaEcio.readEmbeddedFromParcel(hwParcel, embeddedBuffer, i * 60);
            arrayList.add(signalStrengthWithWcdmaEcio);
        }
        return arrayList;
    }

    public final void readEmbeddedFromParcel(HwParcel hwParcel, HwBlob hwBlob, long j) {
        this.gsm_signalStrength = hwBlob.getInt32(0 + j);
        this.gsm_bitErrorRate = hwBlob.getInt32(4 + j);
        this.wcdma_rscp = hwBlob.getInt32(8 + j);
        this.wcdma_ecio = hwBlob.getInt32(12 + j);
        this.cdma_dbm = hwBlob.getInt32(16 + j);
        this.cdma_ecio = hwBlob.getInt32(20 + j);
        this.evdo_dbm = hwBlob.getInt32(24 + j);
        this.evdo_ecio = hwBlob.getInt32(28 + j);
        this.evdo_signalNoiseRatio = hwBlob.getInt32(32 + j);
        this.lte_signalStrength = hwBlob.getInt32(36 + j);
        this.lte_rsrp = hwBlob.getInt32(40 + j);
        this.lte_rsrq = hwBlob.getInt32(44 + j);
        this.lte_rssnr = hwBlob.getInt32(48 + j);
        this.lte_cqi = hwBlob.getInt32(52 + j);
        this.tdscdma_rscp = hwBlob.getInt32(j + 56);
    }

    public final void writeToParcel(HwParcel hwParcel) {
        HwBlob hwBlob = new HwBlob(60);
        writeEmbeddedToBlob(hwBlob, 0L);
        hwParcel.writeBuffer(hwBlob);
    }

    public static final void writeVectorToParcel(HwParcel hwParcel, ArrayList<SignalStrengthWithWcdmaEcio> arrayList) {
        HwBlob hwBlob = new HwBlob(16);
        int size = arrayList.size();
        hwBlob.putInt32(8L, size);
        hwBlob.putBool(12L, false);
        HwBlob hwBlob2 = new HwBlob(size * 60);
        for (int i = 0; i < size; i++) {
            arrayList.get(i).writeEmbeddedToBlob(hwBlob2, i * 60);
        }
        hwBlob.putBlob(0L, hwBlob2);
        hwParcel.writeBuffer(hwBlob);
    }

    public final void writeEmbeddedToBlob(HwBlob hwBlob, long j) {
        hwBlob.putInt32(0 + j, this.gsm_signalStrength);
        hwBlob.putInt32(4 + j, this.gsm_bitErrorRate);
        hwBlob.putInt32(8 + j, this.wcdma_rscp);
        hwBlob.putInt32(12 + j, this.wcdma_ecio);
        hwBlob.putInt32(16 + j, this.cdma_dbm);
        hwBlob.putInt32(20 + j, this.cdma_ecio);
        hwBlob.putInt32(24 + j, this.evdo_dbm);
        hwBlob.putInt32(28 + j, this.evdo_ecio);
        hwBlob.putInt32(32 + j, this.evdo_signalNoiseRatio);
        hwBlob.putInt32(36 + j, this.lte_signalStrength);
        hwBlob.putInt32(40 + j, this.lte_rsrp);
        hwBlob.putInt32(44 + j, this.lte_rsrq);
        hwBlob.putInt32(48 + j, this.lte_rssnr);
        hwBlob.putInt32(52 + j, this.lte_cqi);
        hwBlob.putInt32(j + 56, this.tdscdma_rscp);
    }
}
