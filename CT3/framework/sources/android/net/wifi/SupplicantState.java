package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public enum SupplicantState implements Parcelable {
    DISCONNECTED,
    INTERFACE_DISABLED,
    INACTIVE,
    SCANNING,
    AUTHENTICATING,
    ASSOCIATING,
    ASSOCIATED,
    FOUR_WAY_HANDSHAKE,
    GROUP_HANDSHAKE,
    COMPLETED,
    DORMANT,
    UNINITIALIZED,
    INVALID;


    private static final int[] f21androidnetwifiSupplicantStateSwitchesValues = null;
    public static final Parcelable.Creator<SupplicantState> CREATOR = new Parcelable.Creator<SupplicantState>() {
        @Override
        public SupplicantState createFromParcel(Parcel in) {
            return SupplicantState.valueOf(in.readString());
        }

        @Override
        public SupplicantState[] newArray(int size) {
            return new SupplicantState[size];
        }
    };

    private static int[] m1684getandroidnetwifiSupplicantStateSwitchesValues() {
        if (f21androidnetwifiSupplicantStateSwitchesValues != null) {
            return f21androidnetwifiSupplicantStateSwitchesValues;
        }
        int[] iArr = new int[valuesCustom().length];
        try {
            iArr[ASSOCIATED.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[ASSOCIATING.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[AUTHENTICATING.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[COMPLETED.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[DISCONNECTED.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[DORMANT.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[FOUR_WAY_HANDSHAKE.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[GROUP_HANDSHAKE.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[INACTIVE.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[INTERFACE_DISABLED.ordinal()] = 10;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[INVALID.ordinal()] = 11;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[SCANNING.ordinal()] = 12;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[UNINITIALIZED.ordinal()] = 13;
        } catch (NoSuchFieldError e13) {
        }
        f21androidnetwifiSupplicantStateSwitchesValues = iArr;
        return iArr;
    }

    public static SupplicantState[] valuesCustom() {
        return values();
    }

    public static boolean isValidState(SupplicantState state) {
        return (state == UNINITIALIZED || state == INVALID) ? false : true;
    }

    public static boolean isHandshakeState(SupplicantState state) {
        switch (m1684getandroidnetwifiSupplicantStateSwitchesValues()[state.ordinal()]) {
            case 1:
            case 2:
            case 3:
            case 7:
            case 8:
                return true;
            case 4:
            case 5:
            case 6:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
                return false;
            default:
                throw new IllegalArgumentException("Unknown supplicant state");
        }
    }

    public static boolean isConnecting(SupplicantState state) {
        switch (m1684getandroidnetwifiSupplicantStateSwitchesValues()[state.ordinal()]) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 7:
            case 8:
                return true;
            case 5:
            case 6:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
                return false;
            default:
                throw new IllegalArgumentException("Unknown supplicant state");
        }
    }

    public static boolean isDriverActive(SupplicantState state) {
        switch (m1684getandroidnetwifiSupplicantStateSwitchesValues()[state.ordinal()]) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 12:
                return true;
            case 10:
            case 11:
            case 13:
                return false;
            default:
                throw new IllegalArgumentException("Unknown supplicant state");
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name());
    }
}
