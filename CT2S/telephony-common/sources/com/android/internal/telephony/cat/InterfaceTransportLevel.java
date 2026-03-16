package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class InterfaceTransportLevel implements Parcelable {
    public static final Parcelable.Creator<InterfaceTransportLevel> CREATOR = new Parcelable.Creator<InterfaceTransportLevel>() {
        @Override
        public InterfaceTransportLevel createFromParcel(Parcel in) {
            return new InterfaceTransportLevel(in);
        }

        @Override
        public InterfaceTransportLevel[] newArray(int size) {
            return new InterfaceTransportLevel[size];
        }
    };
    public int port;
    public TransportProtocol protocol;

    public enum TransportProtocol {
        RESERVED(0),
        UDP_CLIENT_REMOTE(1),
        TCP_CLIENT_REMOTE(2),
        TCP_SERVER(3),
        UDP_CLIENT_LOCAL(4),
        TCP_CLIENT_LOCAL(5);

        private int mValue;

        TransportProtocol(int value) {
            this.mValue = value;
        }

        public int value() {
            return this.mValue;
        }
    }

    public InterfaceTransportLevel(int port, TransportProtocol protocol) {
        this.port = port;
        this.protocol = protocol;
    }

    private InterfaceTransportLevel(Parcel in) {
        this.port = in.readInt();
        this.protocol = TransportProtocol.values()[in.readInt()];
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.port);
        dest.writeInt(this.protocol.ordinal());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
