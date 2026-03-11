package com.android.settings.search;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

public class ResultPayload implements Parcelable {
    public static final Parcelable.Creator<ResultPayload> CREATOR = new Parcelable.Creator<ResultPayload>() {
        @Override
        public ResultPayload createFromParcel(Parcel parcel) {
            return new ResultPayload(parcel);
        }

        @Override
        public ResultPayload[] newArray(int i) {
            return new ResultPayload[i];
        }
    };
    protected final Intent mIntent;

    private ResultPayload(Parcel parcel) {
        this.mIntent = (Intent) parcel.readParcelable(ResultPayload.class.getClassLoader());
    }

    public ResultPayload(Intent intent) {
        this.mIntent = intent;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mIntent, i);
    }
}
