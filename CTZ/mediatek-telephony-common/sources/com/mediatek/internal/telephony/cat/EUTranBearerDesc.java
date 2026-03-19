package com.mediatek.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class EUTranBearerDesc extends BearerDesc {
    public static final Parcelable.Creator<EUTranBearerDesc> CREATOR = new Parcelable.Creator<EUTranBearerDesc>() {
        @Override
        public EUTranBearerDesc createFromParcel(Parcel parcel) {
            return new EUTranBearerDesc(parcel);
        }

        @Override
        public EUTranBearerDesc[] newArray(int i) {
            return new EUTranBearerDesc[i];
        }
    };
    public int QCI;
    public int guarBitRateD;
    public int guarBitRateDEx;
    public int guarBitRateU;
    public int guarBitRateUEx;
    public int maxBitRateD;
    public int maxBitRateDEx;
    public int maxBitRateU;
    public int maxBitRateUEx;
    public int pdnType;

    public EUTranBearerDesc() {
        this.QCI = 0;
        this.maxBitRateU = 0;
        this.maxBitRateD = 0;
        this.guarBitRateU = 0;
        this.guarBitRateD = 0;
        this.maxBitRateUEx = 0;
        this.maxBitRateDEx = 0;
        this.guarBitRateUEx = 0;
        this.guarBitRateDEx = 0;
        this.pdnType = 0;
        this.bearerType = 11;
    }

    private EUTranBearerDesc(Parcel parcel) {
        this.QCI = 0;
        this.maxBitRateU = 0;
        this.maxBitRateD = 0;
        this.guarBitRateU = 0;
        this.guarBitRateD = 0;
        this.maxBitRateUEx = 0;
        this.maxBitRateDEx = 0;
        this.guarBitRateUEx = 0;
        this.guarBitRateDEx = 0;
        this.pdnType = 0;
        this.bearerType = parcel.readInt();
        this.QCI = parcel.readInt();
        this.maxBitRateU = parcel.readInt();
        this.maxBitRateD = parcel.readInt();
        this.guarBitRateU = parcel.readInt();
        this.guarBitRateD = parcel.readInt();
        this.maxBitRateUEx = parcel.readInt();
        this.maxBitRateDEx = parcel.readInt();
        this.guarBitRateUEx = parcel.readInt();
        this.guarBitRateDEx = parcel.readInt();
        this.pdnType = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.bearerType);
        parcel.writeInt(this.QCI);
        parcel.writeInt(this.maxBitRateU);
        parcel.writeInt(this.maxBitRateD);
        parcel.writeInt(this.guarBitRateU);
        parcel.writeInt(this.guarBitRateD);
        parcel.writeInt(this.maxBitRateUEx);
        parcel.writeInt(this.maxBitRateDEx);
        parcel.writeInt(this.guarBitRateUEx);
        parcel.writeInt(this.guarBitRateDEx);
        parcel.writeInt(this.pdnType);
    }
}
