package com.mediatek.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class GPRSBearerDesc extends BearerDesc {
    public static final Parcelable.Creator<GPRSBearerDesc> CREATOR = new Parcelable.Creator<GPRSBearerDesc>() {
        @Override
        public GPRSBearerDesc createFromParcel(Parcel parcel) {
            return new GPRSBearerDesc(parcel);
        }

        @Override
        public GPRSBearerDesc[] newArray(int i) {
            return new GPRSBearerDesc[i];
        }
    };
    public int bearerService;
    public int connectionElement;
    public int dataCompression;
    public int dataRate;
    public int delay;
    public int headerCompression;
    public int mean;
    public int pdpType;
    public int peak;
    public int precedence;
    public int reliability;

    public GPRSBearerDesc() {
        this.precedence = 0;
        this.delay = 0;
        this.reliability = 0;
        this.peak = 0;
        this.mean = 0;
        this.pdpType = 0;
        this.dataCompression = 0;
        this.headerCompression = 0;
        this.dataRate = 0;
        this.bearerService = 0;
        this.connectionElement = 0;
        this.bearerType = 2;
    }

    private GPRSBearerDesc(Parcel parcel) {
        this.precedence = 0;
        this.delay = 0;
        this.reliability = 0;
        this.peak = 0;
        this.mean = 0;
        this.pdpType = 0;
        this.dataCompression = 0;
        this.headerCompression = 0;
        this.dataRate = 0;
        this.bearerService = 0;
        this.connectionElement = 0;
        this.bearerType = parcel.readInt();
        this.precedence = parcel.readInt();
        this.delay = parcel.readInt();
        this.reliability = parcel.readInt();
        this.peak = parcel.readInt();
        this.mean = parcel.readInt();
        this.pdpType = parcel.readInt();
        this.dataCompression = parcel.readInt();
        this.headerCompression = parcel.readInt();
        this.dataRate = parcel.readInt();
        this.bearerService = parcel.readInt();
        this.connectionElement = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.bearerType);
        parcel.writeInt(this.precedence);
        parcel.writeInt(this.delay);
        parcel.writeInt(this.reliability);
        parcel.writeInt(this.peak);
        parcel.writeInt(this.mean);
        parcel.writeInt(this.pdpType);
        parcel.writeInt(this.dataCompression);
        parcel.writeInt(this.headerCompression);
        parcel.writeInt(this.dataRate);
        parcel.writeInt(this.bearerService);
        parcel.writeInt(this.connectionElement);
    }
}
