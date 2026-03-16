package com.android.ims;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ImsConferenceState implements Parcelable {
    public static final Parcelable.Creator<ImsConferenceState> CREATOR = new Parcelable.Creator<ImsConferenceState>() {
        @Override
        public ImsConferenceState createFromParcel(Parcel in) {
            return new ImsConferenceState(in);
        }

        @Override
        public ImsConferenceState[] newArray(int size) {
            return new ImsConferenceState[size];
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
    public static final String USER = "user";
    public HashMap<String, Bundle> mParticipants = new HashMap<>();

    public ImsConferenceState() {
    }

    public ImsConferenceState(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        Set<Map.Entry<String, Bundle>> entries;
        out.writeInt(this.mParticipants.size());
        if (this.mParticipants.size() > 0 && (entries = this.mParticipants.entrySet()) != null) {
            for (Map.Entry<String, Bundle> entry : entries) {
                out.writeString(entry.getKey());
                out.writeParcelable(entry.getValue(), 0);
            }
        }
    }

    private void readFromParcel(Parcel in) {
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            String user = in.readString();
            Bundle state = (Bundle) in.readParcelable(null);
            this.mParticipants.put(user, state);
        }
    }

    public static int getConnectionStateForStatus(String status) {
        if (status.equals(STATUS_PENDING)) {
            return 0;
        }
        if (status.equals(STATUS_DIALING_IN)) {
            return 2;
        }
        if (status.equals(STATUS_ALERTING) || status.equals(STATUS_DIALING_OUT)) {
            return 3;
        }
        if (status.equals(STATUS_ON_HOLD)) {
            return 5;
        }
        return (status.equals("connected") || status.equals(STATUS_MUTED_VIA_FOCUS) || status.equals(STATUS_DISCONNECTING) || !status.equals(STATUS_DISCONNECTED)) ? 4 : 6;
    }
}
