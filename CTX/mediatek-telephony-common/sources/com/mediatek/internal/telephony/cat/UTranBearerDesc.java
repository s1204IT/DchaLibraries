package com.mediatek.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class UTranBearerDesc extends BearerDesc {
    public static final Parcelable.Creator<UTranBearerDesc> CREATOR = new Parcelable.Creator<UTranBearerDesc>() {
        @Override
        public UTranBearerDesc createFromParcel(Parcel parcel) {
            return new UTranBearerDesc(parcel);
        }

        @Override
        public UTranBearerDesc[] newArray(int i) {
            return new UTranBearerDesc[i];
        }
    };
    public int deliveryOfErroneousSdus;
    public int deliveryOrder;
    public int guarBitRateDL_High;
    public int guarBitRateDL_Low;
    public int guarBitRateUL_High;
    public int guarBitRateUL_Low;
    public int maxBitRateDL_High;
    public int maxBitRateDL_Low;
    public int maxBitRateUL_High;
    public int maxBitRateUL_Low;
    public int maxSduSize;
    public int pdpType;
    public int residualBitErrorRadio;
    public int sduErrorRatio;
    public int trafficClass;
    public int trafficHandlingPriority;
    public int transferDelay;

    public UTranBearerDesc() {
        this.trafficClass = 0;
        this.maxBitRateUL_High = 0;
        this.maxBitRateUL_Low = 0;
        this.maxBitRateDL_High = 0;
        this.maxBitRateDL_Low = 0;
        this.guarBitRateUL_High = 0;
        this.guarBitRateUL_Low = 0;
        this.guarBitRateDL_High = 0;
        this.guarBitRateDL_Low = 0;
        this.deliveryOrder = 0;
        this.maxSduSize = 0;
        this.sduErrorRatio = 0;
        this.residualBitErrorRadio = 0;
        this.deliveryOfErroneousSdus = 0;
        this.transferDelay = 0;
        this.trafficHandlingPriority = 0;
        this.pdpType = 0;
        this.bearerType = 9;
    }

    private UTranBearerDesc(Parcel parcel) {
        this.trafficClass = 0;
        this.maxBitRateUL_High = 0;
        this.maxBitRateUL_Low = 0;
        this.maxBitRateDL_High = 0;
        this.maxBitRateDL_Low = 0;
        this.guarBitRateUL_High = 0;
        this.guarBitRateUL_Low = 0;
        this.guarBitRateDL_High = 0;
        this.guarBitRateDL_Low = 0;
        this.deliveryOrder = 0;
        this.maxSduSize = 0;
        this.sduErrorRatio = 0;
        this.residualBitErrorRadio = 0;
        this.deliveryOfErroneousSdus = 0;
        this.transferDelay = 0;
        this.trafficHandlingPriority = 0;
        this.pdpType = 0;
        this.bearerType = parcel.readInt();
        this.trafficClass = parcel.readInt();
        this.maxBitRateUL_High = parcel.readInt();
        this.maxBitRateUL_Low = parcel.readInt();
        this.maxBitRateDL_High = parcel.readInt();
        this.maxBitRateDL_Low = parcel.readInt();
        this.guarBitRateUL_High = parcel.readInt();
        this.guarBitRateUL_Low = parcel.readInt();
        this.guarBitRateDL_High = parcel.readInt();
        this.guarBitRateDL_Low = parcel.readInt();
        this.deliveryOrder = parcel.readInt();
        this.maxSduSize = parcel.readInt();
        this.sduErrorRatio = parcel.readInt();
        this.residualBitErrorRadio = parcel.readInt();
        this.deliveryOfErroneousSdus = parcel.readInt();
        this.transferDelay = parcel.readInt();
        this.trafficHandlingPriority = parcel.readInt();
        this.pdpType = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.bearerType);
        parcel.writeInt(this.trafficClass);
        parcel.writeInt(this.maxBitRateUL_High);
        parcel.writeInt(this.maxBitRateUL_Low);
        parcel.writeInt(this.maxBitRateDL_High);
        parcel.writeInt(this.maxBitRateDL_Low);
        parcel.writeInt(this.guarBitRateUL_High);
        parcel.writeInt(this.guarBitRateUL_Low);
        parcel.writeInt(this.guarBitRateDL_High);
        parcel.writeInt(this.guarBitRateDL_Low);
        parcel.writeInt(this.deliveryOrder);
        parcel.writeInt(this.maxSduSize);
        parcel.writeInt(this.sduErrorRatio);
        parcel.writeInt(this.residualBitErrorRadio);
        parcel.writeInt(this.deliveryOfErroneousSdus);
        parcel.writeInt(this.transferDelay);
        parcel.writeInt(this.trafficHandlingPriority);
        parcel.writeInt(this.pdpType);
    }
}
