package android.telephony;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;

public final class CellInfoLte extends CellInfo implements Parcelable {
    public static final Parcelable.Creator<CellInfoLte> CREATOR = new Parcelable.Creator<CellInfoLte>() {
        @Override
        public CellInfoLte createFromParcel(Parcel parcel) {
            parcel.readInt();
            return CellInfoLte.createFromParcelBody(parcel);
        }

        @Override
        public CellInfoLte[] newArray(int i) {
            return new CellInfoLte[i];
        }
    };
    private static final boolean DBG = false;
    private static final String LOG_TAG = "CellInfoLte";
    private CellIdentityLte mCellIdentityLte;
    private CellSignalStrengthLte mCellSignalStrengthLte;

    public CellInfoLte() {
        this.mCellIdentityLte = new CellIdentityLte();
        this.mCellSignalStrengthLte = new CellSignalStrengthLte();
    }

    public CellInfoLte(CellInfoLte cellInfoLte) {
        super(cellInfoLte);
        this.mCellIdentityLte = cellInfoLte.mCellIdentityLte.copy();
        this.mCellSignalStrengthLte = cellInfoLte.mCellSignalStrengthLte.copy();
    }

    public CellIdentityLte getCellIdentity() {
        return this.mCellIdentityLte;
    }

    public void setCellIdentity(CellIdentityLte cellIdentityLte) {
        this.mCellIdentityLte = cellIdentityLte;
    }

    public CellSignalStrengthLte getCellSignalStrength() {
        return this.mCellSignalStrengthLte;
    }

    public void setCellSignalStrength(CellSignalStrengthLte cellSignalStrengthLte) {
        this.mCellSignalStrengthLte = cellSignalStrengthLte;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + this.mCellIdentityLte.hashCode() + this.mCellSignalStrengthLte.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        try {
            CellInfoLte cellInfoLte = (CellInfoLte) obj;
            if (this.mCellIdentityLte.equals(cellInfoLte.mCellIdentityLte)) {
                return this.mCellSignalStrengthLte.equals(cellInfoLte.mCellSignalStrengthLte);
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("CellInfoLte:{");
        stringBuffer.append(super.toString());
        stringBuffer.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        stringBuffer.append(this.mCellIdentityLte);
        stringBuffer.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        stringBuffer.append(this.mCellSignalStrengthLte);
        stringBuffer.append("}");
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i, 3);
        this.mCellIdentityLte.writeToParcel(parcel, i);
        this.mCellSignalStrengthLte.writeToParcel(parcel, i);
    }

    private CellInfoLte(Parcel parcel) {
        super(parcel);
        this.mCellIdentityLte = CellIdentityLte.CREATOR.createFromParcel(parcel);
        this.mCellSignalStrengthLte = CellSignalStrengthLte.CREATOR.createFromParcel(parcel);
    }

    protected static CellInfoLte createFromParcelBody(Parcel parcel) {
        return new CellInfoLte(parcel);
    }

    private static void log(String str) {
        Rlog.w(LOG_TAG, str);
    }
}
