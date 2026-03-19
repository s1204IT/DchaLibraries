package android.telephony;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;

public final class CellInfoWcdma extends CellInfo implements Parcelable {
    public static final Parcelable.Creator<CellInfoWcdma> CREATOR = new Parcelable.Creator<CellInfoWcdma>() {
        @Override
        public CellInfoWcdma createFromParcel(Parcel parcel) {
            parcel.readInt();
            return CellInfoWcdma.createFromParcelBody(parcel);
        }

        @Override
        public CellInfoWcdma[] newArray(int i) {
            return new CellInfoWcdma[i];
        }
    };
    private static final boolean DBG = false;
    private static final String LOG_TAG = "CellInfoWcdma";
    private CellIdentityWcdma mCellIdentityWcdma;
    private CellSignalStrengthWcdma mCellSignalStrengthWcdma;

    public CellInfoWcdma() {
        this.mCellIdentityWcdma = new CellIdentityWcdma();
        this.mCellSignalStrengthWcdma = new CellSignalStrengthWcdma();
    }

    public CellInfoWcdma(CellInfoWcdma cellInfoWcdma) {
        super(cellInfoWcdma);
        this.mCellIdentityWcdma = cellInfoWcdma.mCellIdentityWcdma.copy();
        this.mCellSignalStrengthWcdma = cellInfoWcdma.mCellSignalStrengthWcdma.copy();
    }

    public CellIdentityWcdma getCellIdentity() {
        return this.mCellIdentityWcdma;
    }

    public void setCellIdentity(CellIdentityWcdma cellIdentityWcdma) {
        this.mCellIdentityWcdma = cellIdentityWcdma;
    }

    public CellSignalStrengthWcdma getCellSignalStrength() {
        return this.mCellSignalStrengthWcdma;
    }

    public void setCellSignalStrength(CellSignalStrengthWcdma cellSignalStrengthWcdma) {
        this.mCellSignalStrengthWcdma = cellSignalStrengthWcdma;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + this.mCellIdentityWcdma.hashCode() + this.mCellSignalStrengthWcdma.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        try {
            CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) obj;
            if (this.mCellIdentityWcdma.equals(cellInfoWcdma.mCellIdentityWcdma)) {
                return this.mCellSignalStrengthWcdma.equals(cellInfoWcdma.mCellSignalStrengthWcdma);
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("CellInfoWcdma:{");
        stringBuffer.append(super.toString());
        stringBuffer.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        stringBuffer.append(this.mCellIdentityWcdma);
        stringBuffer.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        stringBuffer.append(this.mCellSignalStrengthWcdma);
        stringBuffer.append("}");
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i, 4);
        this.mCellIdentityWcdma.writeToParcel(parcel, i);
        this.mCellSignalStrengthWcdma.writeToParcel(parcel, i);
    }

    private CellInfoWcdma(Parcel parcel) {
        super(parcel);
        this.mCellIdentityWcdma = CellIdentityWcdma.CREATOR.createFromParcel(parcel);
        this.mCellSignalStrengthWcdma = CellSignalStrengthWcdma.CREATOR.createFromParcel(parcel);
    }

    protected static CellInfoWcdma createFromParcelBody(Parcel parcel) {
        return new CellInfoWcdma(parcel);
    }

    private static void log(String str) {
        Rlog.w(LOG_TAG, str);
    }
}
