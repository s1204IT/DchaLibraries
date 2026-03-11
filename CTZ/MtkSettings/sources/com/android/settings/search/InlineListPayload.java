package com.android.settings.search;

import android.os.Parcel;
import android.os.Parcelable;

public class InlineListPayload extends InlinePayload {
    public static final Parcelable.Creator<InlineListPayload> CREATOR = new Parcelable.Creator<InlineListPayload>() {
        @Override
        public InlineListPayload createFromParcel(Parcel parcel) {
            return new InlineListPayload(parcel);
        }

        @Override
        public InlineListPayload[] newArray(int i) {
            return new InlineListPayload[i];
        }
    };
    private int mNumOptions;

    private InlineListPayload(Parcel parcel) {
        super(parcel);
        this.mNumOptions = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(this.mNumOptions);
    }
}
