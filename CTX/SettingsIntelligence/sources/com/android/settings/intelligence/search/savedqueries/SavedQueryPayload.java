package com.android.settings.intelligence.search.savedqueries;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.settings.intelligence.search.ResultPayload;

public class SavedQueryPayload extends ResultPayload {
    public static final Parcelable.Creator<SavedQueryPayload> CREATOR = new Parcelable.Creator<SavedQueryPayload>() {
        @Override
        public SavedQueryPayload createFromParcel(Parcel parcel) {
            return new SavedQueryPayload(parcel);
        }

        @Override
        public SavedQueryPayload[] newArray(int i) {
            return new SavedQueryPayload[i];
        }
    };
    public final String query;

    public SavedQueryPayload(String str) {
        super((Intent) null);
        this.query = str;
    }

    SavedQueryPayload(Parcel parcel) {
        super((Intent) null);
        this.query = parcel.readString();
    }

    @Override
    public int getType() {
        return 4;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.query);
    }
}
