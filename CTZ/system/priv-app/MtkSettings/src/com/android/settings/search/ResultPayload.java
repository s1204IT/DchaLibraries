package com.android.settings.search;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class ResultPayload implements Parcelable {
    public static final Parcelable.Creator<ResultPayload> CREATOR = new Parcelable.Creator<ResultPayload>() { // from class: com.android.settings.search.ResultPayload.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ResultPayload createFromParcel(Parcel parcel) {
            return new ResultPayload(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
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

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mIntent, i);
    }
}
