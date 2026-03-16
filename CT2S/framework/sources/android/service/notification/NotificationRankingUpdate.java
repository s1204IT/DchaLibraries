package android.service.notification;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class NotificationRankingUpdate implements Parcelable {
    public static final Parcelable.Creator<NotificationRankingUpdate> CREATOR = new Parcelable.Creator<NotificationRankingUpdate>() {
        @Override
        public NotificationRankingUpdate createFromParcel(Parcel parcel) {
            return new NotificationRankingUpdate(parcel);
        }

        @Override
        public NotificationRankingUpdate[] newArray(int size) {
            return new NotificationRankingUpdate[size];
        }
    };
    private final int mFirstAmbientIndex;
    private final String[] mInterceptedKeys;
    private final String[] mKeys;
    private final Bundle mVisibilityOverrides;

    public NotificationRankingUpdate(String[] keys, String[] interceptedKeys, Bundle visibilityOverrides, int firstAmbientIndex) {
        this.mKeys = keys;
        this.mFirstAmbientIndex = firstAmbientIndex;
        this.mInterceptedKeys = interceptedKeys;
        this.mVisibilityOverrides = visibilityOverrides;
    }

    public NotificationRankingUpdate(Parcel in) {
        this.mKeys = in.readStringArray();
        this.mFirstAmbientIndex = in.readInt();
        this.mInterceptedKeys = in.readStringArray();
        this.mVisibilityOverrides = in.readBundle();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeStringArray(this.mKeys);
        out.writeInt(this.mFirstAmbientIndex);
        out.writeStringArray(this.mInterceptedKeys);
        out.writeBundle(this.mVisibilityOverrides);
    }

    public String[] getOrderedKeys() {
        return this.mKeys;
    }

    public int getFirstAmbientIndex() {
        return this.mFirstAmbientIndex;
    }

    public String[] getInterceptedKeys() {
        return this.mInterceptedKeys;
    }

    public Bundle getVisibilityOverrides() {
        return this.mVisibilityOverrides;
    }
}
