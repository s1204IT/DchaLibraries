package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class BearerDescription implements Parcelable {
    public static final Parcelable.Creator<BearerDescription> CREATOR = new Parcelable.Creator<BearerDescription>() {
        @Override
        public BearerDescription createFromParcel(Parcel in) {
            return new BearerDescription(in);
        }

        @Override
        public BearerDescription[] newArray(int size) {
            return new BearerDescription[size];
        }
    };
    public byte[] parameters;
    public BearerType type;

    public enum BearerType {
        MOBILE_CSD(1),
        MOBILE_PS(2),
        DEFAULT_BEARER(3),
        LOCAL_LINK(4),
        BLUETOOTH(5),
        IRDA(6),
        RS232(7),
        MOBILE_PS_EXTENDED_QOS(9),
        I_WLAN(10),
        USB(16),
        E_UTRAN(11);

        private int mValue;

        BearerType(int value) {
            this.mValue = value;
        }

        public int value() {
            return this.mValue;
        }
    }

    public BearerDescription(BearerType type, byte[] parameters) {
        this.parameters = new byte[0];
        this.type = type;
        this.parameters = parameters;
    }

    private BearerDescription(Parcel in) {
        this.parameters = new byte[0];
        this.type = BearerType.values()[in.readInt()];
        int len = in.readInt();
        if (len > 0) {
            this.parameters = new byte[len];
            in.readByteArray(this.parameters);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.type.ordinal());
        dest.writeInt(this.parameters.length);
        if (this.parameters.length > 0) {
            dest.writeByteArray(this.parameters);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
