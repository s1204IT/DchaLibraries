package com.android.ims.internal.uce.presence;

import android.os.Parcel;
import android.os.Parcelable;

public class PresSubscriptionState implements Parcelable {
    public static final Parcelable.Creator<PresSubscriptionState> CREATOR = new Parcelable.Creator<PresSubscriptionState>() {
        @Override
        public PresSubscriptionState createFromParcel(Parcel parcel) {
            return new PresSubscriptionState(parcel);
        }

        @Override
        public PresSubscriptionState[] newArray(int i) {
            return new PresSubscriptionState[i];
        }
    };
    public static final int UCE_PRES_SUBSCRIPTION_STATE_ACTIVE = 0;
    public static final int UCE_PRES_SUBSCRIPTION_STATE_PENDING = 1;
    public static final int UCE_PRES_SUBSCRIPTION_STATE_TERMINATED = 2;
    public static final int UCE_PRES_SUBSCRIPTION_STATE_UNKNOWN = 3;
    private int mPresSubscriptionState;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mPresSubscriptionState);
    }

    private PresSubscriptionState(Parcel parcel) {
        this.mPresSubscriptionState = 3;
        readFromParcel(parcel);
    }

    public void readFromParcel(Parcel parcel) {
        this.mPresSubscriptionState = parcel.readInt();
    }

    public PresSubscriptionState() {
        this.mPresSubscriptionState = 3;
    }

    public int getPresSubscriptionStateValue() {
        return this.mPresSubscriptionState;
    }

    public void setPresSubscriptionState(int i) {
        this.mPresSubscriptionState = i;
    }
}
