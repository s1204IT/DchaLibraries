package android.telephony.ims;

import android.annotation.SystemApi;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.telecom.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SystemApi
public final class ImsConferenceState implements Parcelable {
    public static final Parcelable.Creator<ImsConferenceState> CREATOR = new Parcelable.Creator<ImsConferenceState>() {
        @Override
        public ImsConferenceState createFromParcel(Parcel parcel) {
            return new ImsConferenceState(parcel);
        }

        @Override
        public ImsConferenceState[] newArray(int i) {
            return new ImsConferenceState[i];
        }
    };
    public static final String DISPLAY_TEXT = "display-text";
    public static final String ENDPOINT = "endpoint";
    public static final String SIP_STATUS_CODE = "sipstatuscode";
    public static final String STATUS = "status";
    public static final String STATUS_ALERTING = "alerting";
    public static final String STATUS_CONNECTED = "connected";
    public static final String STATUS_CONNECT_FAIL = "connect-fail";
    public static final String STATUS_DIALING_IN = "dialing-in";
    public static final String STATUS_DIALING_OUT = "dialing-out";
    public static final String STATUS_DISCONNECTED = "disconnected";
    public static final String STATUS_DISCONNECTING = "disconnecting";
    public static final String STATUS_MUTED_VIA_FOCUS = "muted-via-focus";
    public static final String STATUS_ON_HOLD = "on-hold";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SEND_ONLY = "sendonly";
    public static final String STATUS_SEND_RECV = "sendrecv";
    public static final String USER = "user";
    public final HashMap<String, Bundle> mParticipants;

    public ImsConferenceState() {
        this.mParticipants = new HashMap<>();
    }

    private ImsConferenceState(Parcel parcel) {
        this.mParticipants = new HashMap<>();
        readFromParcel(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        Set<Map.Entry<String, Bundle>> setEntrySet;
        parcel.writeInt(this.mParticipants.size());
        if (this.mParticipants.size() > 0 && (setEntrySet = this.mParticipants.entrySet()) != null) {
            for (Map.Entry<String, Bundle> entry : setEntrySet) {
                parcel.writeString(entry.getKey());
                parcel.writeParcelable(entry.getValue(), 0);
            }
        }
    }

    private void readFromParcel(Parcel parcel) {
        int i = parcel.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            this.mParticipants.put(parcel.readString(), (Bundle) parcel.readParcelable(null));
        }
    }

    public static int getConnectionStateForStatus(String str) {
        if (str.equals(STATUS_PENDING)) {
            return 0;
        }
        if (str.equals(STATUS_DIALING_IN)) {
            return 2;
        }
        if (str.equals(STATUS_ALERTING) || str.equals(STATUS_DIALING_OUT)) {
            return 3;
        }
        if (str.equals(STATUS_ON_HOLD) || str.equals(STATUS_SEND_ONLY)) {
            return 5;
        }
        return (str.equals("connected") || str.equals(STATUS_MUTED_VIA_FOCUS) || str.equals(STATUS_DISCONNECTING) || str.equals(STATUS_SEND_RECV) || !str.equals(STATUS_DISCONNECTED)) ? 4 : 6;
    }

    public String toString() {
        Set<Map.Entry<String, Bundle>> setEntrySet;
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(ImsConferenceState.class.getSimpleName());
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        if (this.mParticipants.size() > 0 && (setEntrySet = this.mParticipants.entrySet()) != null) {
            sb.append("<");
            for (Map.Entry<String, Bundle> entry : setEntrySet) {
                sb.append(entry.getKey());
                sb.append(": ");
                Bundle value = entry.getValue();
                for (String str : value.keySet()) {
                    sb.append(str);
                    sb.append("=");
                    if (ENDPOINT.equals(str) || "user".equals(str)) {
                        sb.append(Log.pii(value.get(str)));
                    } else {
                        sb.append(value.get(str));
                    }
                    sb.append(", ");
                }
            }
            sb.append(">");
        }
        sb.append("]");
        return sb.toString();
    }
}
