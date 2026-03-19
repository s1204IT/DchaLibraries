package com.mediatek.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class DefaultBearerDesc extends BearerDesc {
    public static final Parcelable.Creator<DefaultBearerDesc> CREATOR = new Parcelable.Creator<DefaultBearerDesc>() {
        @Override
        public DefaultBearerDesc createFromParcel(Parcel parcel) {
            return new DefaultBearerDesc(parcel);
        }

        @Override
        public DefaultBearerDesc[] newArray(int i) {
            return new DefaultBearerDesc[i];
        }
    };

    public DefaultBearerDesc() {
        this.bearerType = 3;
    }

    private DefaultBearerDesc(Parcel parcel) {
        this.bearerType = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.bearerType);
    }
}
