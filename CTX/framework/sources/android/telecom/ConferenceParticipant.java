package android.telecom;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class ConferenceParticipant implements Parcelable {
    public static final Parcelable.Creator<ConferenceParticipant> CREATOR = new Parcelable.Creator<ConferenceParticipant>() {
        @Override
        public ConferenceParticipant createFromParcel(Parcel parcel) {
            ClassLoader classLoader = ParcelableCall.class.getClassLoader();
            return new ConferenceParticipant((Uri) parcel.readParcelable(classLoader), parcel.readString(), (Uri) parcel.readParcelable(classLoader), parcel.readInt());
        }

        @Override
        public ConferenceParticipant[] newArray(int i) {
            return new ConferenceParticipant[i];
        }
    };
    private final String mDisplayName;
    private final Uri mEndpoint;
    private final Uri mHandle;
    private final int mState;

    public ConferenceParticipant(Uri uri, String str, Uri uri2, int i) {
        this.mHandle = uri;
        this.mDisplayName = str;
        this.mEndpoint = uri2;
        this.mState = i;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mHandle, 0);
        parcel.writeString(this.mDisplayName);
        parcel.writeParcelable(this.mEndpoint, 0);
        parcel.writeInt(this.mState);
    }

    public String toString() {
        return "[ConferenceParticipant Handle: " + Log.pii(this.mHandle) + " DisplayName: " + Log.pii(this.mDisplayName) + " Endpoint: " + Log.pii(this.mEndpoint) + " State: " + Connection.stateToString(this.mState) + "]";
    }

    public Uri getHandle() {
        return this.mHandle;
    }

    public String getDisplayName() {
        return this.mDisplayName;
    }

    public Uri getEndpoint() {
        return this.mEndpoint;
    }

    public int getState() {
        return this.mState;
    }
}
