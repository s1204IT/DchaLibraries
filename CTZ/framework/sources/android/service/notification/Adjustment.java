package android.service.notification;

import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

@SystemApi
public final class Adjustment implements Parcelable {
    public static final Parcelable.Creator<Adjustment> CREATOR = new Parcelable.Creator<Adjustment>() {
        @Override
        public Adjustment createFromParcel(Parcel parcel) {
            return new Adjustment(parcel);
        }

        @Override
        public Adjustment[] newArray(int i) {
            return new Adjustment[i];
        }
    };
    public static final String KEY_GROUP_KEY = "key_group_key";
    public static final String KEY_PEOPLE = "key_people";
    public static final String KEY_SNOOZE_CRITERIA = "key_snooze_criteria";
    public static final String KEY_USER_SENTIMENT = "key_user_sentiment";
    private final CharSequence mExplanation;
    private final String mKey;
    private final String mPackage;
    private final Bundle mSignals;
    private final int mUser;

    public Adjustment(String str, String str2, Bundle bundle, CharSequence charSequence, int i) {
        this.mPackage = str;
        this.mKey = str2;
        this.mSignals = bundle;
        this.mExplanation = charSequence;
        this.mUser = i;
    }

    protected Adjustment(Parcel parcel) {
        if (parcel.readInt() == 1) {
            this.mPackage = parcel.readString();
        } else {
            this.mPackage = null;
        }
        if (parcel.readInt() == 1) {
            this.mKey = parcel.readString();
        } else {
            this.mKey = null;
        }
        if (parcel.readInt() == 1) {
            this.mExplanation = parcel.readCharSequence();
        } else {
            this.mExplanation = null;
        }
        this.mSignals = parcel.readBundle();
        this.mUser = parcel.readInt();
    }

    public String getPackage() {
        return this.mPackage;
    }

    public String getKey() {
        return this.mKey;
    }

    public CharSequence getExplanation() {
        return this.mExplanation;
    }

    public Bundle getSignals() {
        return this.mSignals;
    }

    public int getUser() {
        return this.mUser;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (this.mPackage != null) {
            parcel.writeInt(1);
            parcel.writeString(this.mPackage);
        } else {
            parcel.writeInt(0);
        }
        if (this.mKey != null) {
            parcel.writeInt(1);
            parcel.writeString(this.mKey);
        } else {
            parcel.writeInt(0);
        }
        if (this.mExplanation != null) {
            parcel.writeInt(1);
            parcel.writeCharSequence(this.mExplanation);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeBundle(this.mSignals);
        parcel.writeInt(this.mUser);
    }

    public String toString() {
        return "Adjustment{mSignals=" + this.mSignals + '}';
    }
}
