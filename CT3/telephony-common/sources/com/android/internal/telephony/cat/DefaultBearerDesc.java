package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class DefaultBearerDesc extends BearerDesc {
    public static final Parcelable.Creator<DefaultBearerDesc> CREATOR = new Parcelable.Creator<DefaultBearerDesc>() {
        @Override
        public DefaultBearerDesc createFromParcel(Parcel in) {
            return new DefaultBearerDesc(in, null);
        }

        @Override
        public DefaultBearerDesc[] newArray(int size) {
            return new DefaultBearerDesc[size];
        }
    };

    DefaultBearerDesc(Parcel in, DefaultBearerDesc defaultBearerDesc) {
        this(in);
    }

    public DefaultBearerDesc() {
        this.bearerType = 3;
    }

    private DefaultBearerDesc(Parcel in) {
        this.bearerType = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.bearerType);
    }
}
