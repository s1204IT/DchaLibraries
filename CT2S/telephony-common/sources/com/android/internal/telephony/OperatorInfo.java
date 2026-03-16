package com.android.internal.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Telephony;

public class OperatorInfo implements Parcelable {
    public static final Parcelable.Creator<OperatorInfo> CREATOR = new Parcelable.Creator<OperatorInfo>() {
        @Override
        public OperatorInfo createFromParcel(Parcel in) {
            OperatorInfo opInfo = new OperatorInfo(in.readString(), in.readString(), in.readString(), (State) in.readSerializable(), (RadioAccessTechnology) in.readSerializable());
            return opInfo;
        }

        @Override
        public OperatorInfo[] newArray(int size) {
            return new OperatorInfo[size];
        }
    };
    private String mOperatorAlphaLong;
    private String mOperatorAlphaShort;
    private String mOperatorNumeric;
    private RadioAccessTechnology mRat;
    private State mState;

    public enum RadioAccessTechnology {
        GSM,
        GSM_COMPACT,
        UTRAN,
        GSM_wEGPRS,
        UTRAN_wHSDPA,
        UTRAN_wHSUPA,
        UTRAN_wHSDPA_HSUPA,
        E_UTRAN
    }

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

    public RadioAccessTechnology getRat() {
        return this.mRat;
    }

    OperatorInfo(String operatorAlphaLong, String operatorAlphaShort, String operatorNumeric, State state, RadioAccessTechnology rat) {
        this.mState = State.UNKNOWN;
        this.mRat = RadioAccessTechnology.GSM;
        this.mOperatorAlphaLong = operatorAlphaLong;
        this.mOperatorAlphaShort = operatorAlphaShort;
        this.mOperatorNumeric = operatorNumeric;
        this.mState = state;
        this.mRat = rat;
    }

    public OperatorInfo(String operatorAlphaLong, String operatorAlphaShort, String operatorNumeric, String stateString, String ratString) {
        this(operatorAlphaLong, operatorAlphaShort, operatorNumeric, rilStateToState(stateString), rilRatToRat(ratString));
    }

    private static State rilStateToState(String s) {
        if (s.equals("unknown")) {
            return State.UNKNOWN;
        }
        if (s.equals("available")) {
            return State.AVAILABLE;
        }
        if (s.equals(Telephony.Carriers.CURRENT)) {
            return State.CURRENT;
        }
        if (s.equals("forbidden")) {
            return State.FORBIDDEN;
        }
        throw new RuntimeException("RIL impl error: Invalid network state '" + s + "'");
    }

    private static RadioAccessTechnology rilRatToRat(String s) {
        if (s.equals("GSM")) {
            return RadioAccessTechnology.GSM;
        }
        if (s.equals("GSM_COMPACT")) {
            return RadioAccessTechnology.GSM_COMPACT;
        }
        if (s.equals("UTRAN")) {
            return RadioAccessTechnology.UTRAN;
        }
        if (s.equals("GSM_wEGPRS")) {
            return RadioAccessTechnology.GSM_wEGPRS;
        }
        if (s.equals("UTRAN_wHSDPA")) {
            return RadioAccessTechnology.UTRAN_wHSDPA;
        }
        if (s.equals("UTRAN_wHSUPA")) {
            return RadioAccessTechnology.UTRAN_wHSUPA;
        }
        if (s.equals("UTRAN_wHSDPA_HSUPA")) {
            return RadioAccessTechnology.UTRAN_wHSDPA_HSUPA;
        }
        if (s.equals("E_UTRAN")) {
            return RadioAccessTechnology.E_UTRAN;
        }
        throw new RuntimeException("RIL impl error: Invalid radio access technology '" + s + "'");
    }

    public String toString() {
        return "OperatorInfo " + this.mOperatorAlphaLong + "/" + this.mOperatorAlphaShort + "/" + this.mOperatorNumeric + "/" + this.mState + "/" + this.mRat;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mOperatorAlphaLong);
        dest.writeString(this.mOperatorAlphaShort);
        dest.writeString(this.mOperatorNumeric);
        dest.writeSerializable(this.mState);
        dest.writeSerializable(this.mRat);
    }
}
