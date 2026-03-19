package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class EUTranBearerDesc extends BearerDesc {
    public static final Parcelable.Creator<EUTranBearerDesc> CREATOR = new Parcelable.Creator<EUTranBearerDesc>() {
        @Override
        public EUTranBearerDesc createFromParcel(Parcel in) {
            return new EUTranBearerDesc(in, null);
        }

        @Override
        public EUTranBearerDesc[] newArray(int size) {
            return new EUTranBearerDesc[size];
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

    EUTranBearerDesc(Parcel in, EUTranBearerDesc eUTranBearerDesc) {
        this(in);
    }

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

    private EUTranBearerDesc(Parcel in) {
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
        this.bearerType = in.readInt();
        this.QCI = in.readInt();
        this.maxBitRateU = in.readInt();
        this.maxBitRateD = in.readInt();
        this.guarBitRateU = in.readInt();
        this.guarBitRateD = in.readInt();
        this.maxBitRateUEx = in.readInt();
        this.maxBitRateDEx = in.readInt();
        this.guarBitRateUEx = in.readInt();
        this.guarBitRateDEx = in.readInt();
        this.pdnType = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.bearerType);
        dest.writeInt(this.QCI);
        dest.writeInt(this.maxBitRateU);
        dest.writeInt(this.maxBitRateD);
        dest.writeInt(this.guarBitRateU);
        dest.writeInt(this.guarBitRateD);
        dest.writeInt(this.maxBitRateUEx);
        dest.writeInt(this.maxBitRateDEx);
        dest.writeInt(this.guarBitRateUEx);
        dest.writeInt(this.guarBitRateDEx);
        dest.writeInt(this.pdnType);
    }
}
