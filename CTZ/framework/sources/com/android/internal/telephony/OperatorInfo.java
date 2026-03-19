package com.android.internal.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Telephony;

public class OperatorInfo implements Parcelable {
    public static final Parcelable.Creator<OperatorInfo> CREATOR = new Parcelable.Creator<OperatorInfo>() {
        @Override
        public OperatorInfo createFromParcel(Parcel parcel) {
            return new OperatorInfo(parcel.readString(), parcel.readString(), parcel.readString(), (State) parcel.readSerializable());
        }

        @Override
        public OperatorInfo[] newArray(int i) {
            return new OperatorInfo[i];
        }
    };
    private String mOperatorAlphaLong;
    private String mOperatorAlphaShort;
    private String mOperatorNumeric;
    private State mState;

    public enum State {
        UNKNOWN,
        AVAILABLE,
        CURRENT,
        FORBIDDEN
    }

    public String getOperatorAlphaLong() {
        return this.mOperatorAlphaLong;
    }

    public String getOperatorAlphaShort() {
        return this.mOperatorAlphaShort;
    }

    public String getOperatorNumeric() {
        return this.mOperatorNumeric;
    }

    public State getState() {
        return this.mState;
    }

    OperatorInfo(String str, String str2, String str3, State state) {
        this.mState = State.UNKNOWN;
        this.mOperatorAlphaLong = str;
        this.mOperatorAlphaShort = str2;
        this.mOperatorNumeric = str3;
        this.mState = state;
    }

    public OperatorInfo(String str, String str2, String str3, String str4) {
        this(str, str2, str3, rilStateToState(str4));
    }

    public OperatorInfo(String str, String str2, String str3) {
        this(str, str2, str3, State.UNKNOWN);
    }

    private static State rilStateToState(String str) {
        if (str.equals("unknown")) {
            return State.UNKNOWN;
        }
        if (str.equals("available")) {
            return State.AVAILABLE;
        }
        if (str.equals(Telephony.Carriers.CURRENT)) {
            return State.CURRENT;
        }
        if (str.equals("forbidden")) {
            return State.FORBIDDEN;
        }
        throw new RuntimeException("RIL impl error: Invalid network state '" + str + "'");
    }

    public String toString() {
        return "OperatorInfo " + this.mOperatorAlphaLong + "/" + this.mOperatorAlphaShort + "/" + this.mOperatorNumeric + "/" + this.mState;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mOperatorAlphaLong);
        parcel.writeString(this.mOperatorAlphaShort);
        parcel.writeString(this.mOperatorNumeric);
        parcel.writeSerializable(this.mState);
    }
}
