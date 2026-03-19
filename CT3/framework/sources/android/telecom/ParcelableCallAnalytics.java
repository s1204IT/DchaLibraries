package android.telecom;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcelableCallAnalytics implements Parcelable {
    public static final int CALLTYPE_INCOMING = 1;
    public static final int CALLTYPE_OUTGOING = 2;
    public static final int CALLTYPE_UNKNOWN = 0;
    public static final int CDMA_PHONE = 1;
    public static final Parcelable.Creator<ParcelableCallAnalytics> CREATOR = new Parcelable.Creator<ParcelableCallAnalytics>() {
        @Override
        public ParcelableCallAnalytics createFromParcel(Parcel in) {
            return new ParcelableCallAnalytics(in);
        }

        @Override
        public ParcelableCallAnalytics[] newArray(int size) {
            return new ParcelableCallAnalytics[size];
        }
    };
    public static final int GSM_PHONE = 2;
    public static final int IMS_PHONE = 4;
    public static final long MILLIS_IN_1_SECOND = 1000;
    public static final long MILLIS_IN_5_MINUTES = 300000;
    public static final int SIP_PHONE = 8;
    public static final int STILL_CONNECTED = -1;
    public static final int THIRD_PARTY_PHONE = 16;
    private final long callDurationMillis;
    private final int callTechnologies;
    private final int callTerminationCode;
    private final int callType;
    private final String connectionService;
    private final boolean isAdditionalCall;
    private final boolean isCreatedFromExistingConnection;
    private final boolean isEmergencyCall;
    private final boolean isInterrupted;
    private final long startTimeMillis;

    public ParcelableCallAnalytics(long startTimeMillis, long callDurationMillis, int callType, boolean isAdditionalCall, boolean isInterrupted, int callTechnologies, int callTerminationCode, boolean isEmergencyCall, String connectionService, boolean isCreatedFromExistingConnection) {
        this.startTimeMillis = startTimeMillis;
        this.callDurationMillis = callDurationMillis;
        this.callType = callType;
        this.isAdditionalCall = isAdditionalCall;
        this.isInterrupted = isInterrupted;
        this.callTechnologies = callTechnologies;
        this.callTerminationCode = callTerminationCode;
        this.isEmergencyCall = isEmergencyCall;
        this.connectionService = connectionService;
        this.isCreatedFromExistingConnection = isCreatedFromExistingConnection;
    }

    public ParcelableCallAnalytics(Parcel in) {
        this.startTimeMillis = in.readLong();
        this.callDurationMillis = in.readLong();
        this.callType = in.readInt();
        this.isAdditionalCall = readByteAsBoolean(in);
        this.isInterrupted = readByteAsBoolean(in);
        this.callTechnologies = in.readInt();
        this.callTerminationCode = in.readInt();
        this.isEmergencyCall = readByteAsBoolean(in);
        this.connectionService = in.readString();
        this.isCreatedFromExistingConnection = readByteAsBoolean(in);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(this.startTimeMillis);
        out.writeLong(this.callDurationMillis);
        out.writeInt(this.callType);
        writeBooleanAsByte(out, this.isAdditionalCall);
        writeBooleanAsByte(out, this.isInterrupted);
        out.writeInt(this.callTechnologies);
        out.writeInt(this.callTerminationCode);
        writeBooleanAsByte(out, this.isEmergencyCall);
        out.writeString(this.connectionService);
        writeBooleanAsByte(out, this.isCreatedFromExistingConnection);
    }

    public long getStartTimeMillis() {
        return this.startTimeMillis;
    }

    public long getCallDurationMillis() {
        return this.callDurationMillis;
    }

    public int getCallType() {
        return this.callType;
    }

    public boolean isAdditionalCall() {
        return this.isAdditionalCall;
    }

    public boolean isInterrupted() {
        return this.isInterrupted;
    }

    public int getCallTechnologies() {
        return this.callTechnologies;
    }

    public int getCallTerminationCode() {
        return this.callTerminationCode;
    }

    public boolean isEmergencyCall() {
        return this.isEmergencyCall;
    }

    public String getConnectionService() {
        return this.connectionService;
    }

    public boolean isCreatedFromExistingConnection() {
        return this.isCreatedFromExistingConnection;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private static void writeBooleanAsByte(Parcel out, boolean b) {
        out.writeByte((byte) (b ? 1 : 0));
    }

    private static boolean readByteAsBoolean(Parcel in) {
        return in.readByte() == 1;
    }
}
