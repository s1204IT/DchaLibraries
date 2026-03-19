package android.telecom;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

@SystemApi
public final class TelecomAnalytics implements Parcelable {
    public static final Parcelable.Creator<TelecomAnalytics> CREATOR = new Parcelable.Creator<TelecomAnalytics>() {
        @Override
        public TelecomAnalytics createFromParcel(Parcel parcel) {
            return new TelecomAnalytics(parcel);
        }

        @Override
        public TelecomAnalytics[] newArray(int i) {
            return new TelecomAnalytics[i];
        }
    };
    private List<ParcelableCallAnalytics> mCallAnalytics;
    private List<SessionTiming> mSessionTimings;

    public static final class SessionTiming extends TimedEvent<Integer> implements Parcelable {
        public static final Parcelable.Creator<SessionTiming> CREATOR = new Parcelable.Creator<SessionTiming>() {
            @Override
            public SessionTiming createFromParcel(Parcel parcel) {
                return new SessionTiming(parcel);
            }

            @Override
            public SessionTiming[] newArray(int i) {
                return new SessionTiming[i];
            }
        };
        public static final int CSW_ADD_CONFERENCE_CALL = 108;
        public static final int CSW_HANDLE_CREATE_CONNECTION_COMPLETE = 100;
        public static final int CSW_REMOVE_CALL = 106;
        public static final int CSW_SET_ACTIVE = 101;
        public static final int CSW_SET_DIALING = 103;
        public static final int CSW_SET_DISCONNECTED = 104;
        public static final int CSW_SET_IS_CONFERENCED = 107;
        public static final int CSW_SET_ON_HOLD = 105;
        public static final int CSW_SET_RINGING = 102;
        public static final int ICA_ANSWER_CALL = 1;
        public static final int ICA_CONFERENCE = 8;
        public static final int ICA_DISCONNECT_CALL = 3;
        public static final int ICA_HOLD_CALL = 4;
        public static final int ICA_MUTE = 6;
        public static final int ICA_REJECT_CALL = 2;
        public static final int ICA_SET_AUDIO_ROUTE = 7;
        public static final int ICA_UNHOLD_CALL = 5;
        private int mId;
        private long mTime;

        public SessionTiming(int i, long j) {
            this.mId = i;
            this.mTime = j;
        }

        private SessionTiming(Parcel parcel) {
            this.mId = parcel.readInt();
            this.mTime = parcel.readLong();
        }

        @Override
        public Integer getKey() {
            return Integer.valueOf(this.mId);
        }

        @Override
        public long getTime() {
            return this.mTime;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.mId);
            parcel.writeLong(this.mTime);
        }
    }

    public TelecomAnalytics(List<SessionTiming> list, List<ParcelableCallAnalytics> list2) {
        this.mSessionTimings = list;
        this.mCallAnalytics = list2;
    }

    private TelecomAnalytics(Parcel parcel) {
        this.mSessionTimings = new ArrayList();
        parcel.readTypedList(this.mSessionTimings, SessionTiming.CREATOR);
        this.mCallAnalytics = new ArrayList();
        parcel.readTypedList(this.mCallAnalytics, ParcelableCallAnalytics.CREATOR);
    }

    public List<SessionTiming> getSessionTimings() {
        return this.mSessionTimings;
    }

    public List<ParcelableCallAnalytics> getCallAnalytics() {
        return this.mCallAnalytics;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(this.mSessionTimings);
        parcel.writeTypedList(this.mCallAnalytics);
    }
}
