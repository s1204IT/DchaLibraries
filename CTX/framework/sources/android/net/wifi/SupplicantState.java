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

    public static final Parcelable.Creator<SupplicantState> CREATOR = new Parcelable.Creator<SupplicantState>() {
        @Override
        public SupplicantState createFromParcel(Parcel parcel) {
            return SupplicantState.valueOf(parcel.readString());
        }

        @Override
        public SupplicantState[] newArray(int i) {
            return new SupplicantState[i];
        }
    };

    public static boolean isValidState(SupplicantState supplicantState) {
        return (supplicantState == UNINITIALIZED || supplicantState == INVALID) ? false : true;
    }

    public static boolean isHandshakeState(SupplicantState supplicantState) {
        switch (supplicantState) {
            case AUTHENTICATING:
            case ASSOCIATING:
            case ASSOCIATED:
            case FOUR_WAY_HANDSHAKE:
            case GROUP_HANDSHAKE:
                return true;
            case COMPLETED:
            case DISCONNECTED:
            case INTERFACE_DISABLED:
            case INACTIVE:
            case SCANNING:
            case DORMANT:
            case UNINITIALIZED:
            case INVALID:
                return false;
            default:
                throw new IllegalArgumentException("Unknown supplicant state");
        }
    }

    public static boolean isConnecting(SupplicantState supplicantState) {
        switch (supplicantState) {
            case AUTHENTICATING:
            case ASSOCIATING:
            case ASSOCIATED:
            case FOUR_WAY_HANDSHAKE:
            case GROUP_HANDSHAKE:
            case COMPLETED:
                return true;
            case DISCONNECTED:
            case INTERFACE_DISABLED:
            case INACTIVE:
            case SCANNING:
            case DORMANT:
            case UNINITIALIZED:
            case INVALID:
                return false;
            default:
                throw new IllegalArgumentException("Unknown supplicant state");
        }
    }

    public static boolean isDriverActive(SupplicantState supplicantState) {
        switch (supplicantState) {
            case AUTHENTICATING:
            case ASSOCIATING:
            case ASSOCIATED:
            case FOUR_WAY_HANDSHAKE:
            case GROUP_HANDSHAKE:
            case COMPLETED:
            case DISCONNECTED:
            case INACTIVE:
            case SCANNING:
            case DORMANT:
                return true;
            case INTERFACE_DISABLED:
            case UNINITIALIZED:
            case INVALID:
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
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name());
    }
}
