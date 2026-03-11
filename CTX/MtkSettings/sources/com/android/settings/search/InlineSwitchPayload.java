package com.android.settings.search;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

public class InlineSwitchPayload extends InlinePayload {
    public static final Parcelable.Creator<InlineSwitchPayload> CREATOR = new Parcelable.Creator<InlineSwitchPayload>() {
        @Override
        public InlineSwitchPayload createFromParcel(Parcel parcel) {
            return new InlineSwitchPayload(parcel);
        }

        @Override
        public InlineSwitchPayload[] newArray(int i) {
            return new InlineSwitchPayload[i];
        }
    };
    private boolean mIsStandard;

    public InlineSwitchPayload(String str, int i, int i2, Intent intent, boolean z, int i3) {
        super(str, i, intent, z, i3);
        this.mIsStandard = i2 == 1;
    }

    private InlineSwitchPayload(Parcel parcel) {
        super(parcel);
        this.mIsStandard = parcel.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(this.mIsStandard ? 1 : 0);
    }
}
