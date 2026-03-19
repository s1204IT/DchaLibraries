package com.android.internal.telephony.cat;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class TextMessage implements Parcelable {
    public static final Parcelable.Creator<TextMessage> CREATOR = new Parcelable.Creator<TextMessage>() {
        @Override
        public TextMessage createFromParcel(Parcel parcel) {
            return new TextMessage(parcel);
        }

        @Override
        public TextMessage[] newArray(int i) {
            return new TextMessage[i];
        }
    };
    public Duration duration;
    public Bitmap icon;
    public boolean iconSelfExplanatory;
    public boolean isHighPriority;
    public boolean responseNeeded;
    public String text;
    public String title;
    public boolean userClear;

    public TextMessage() {
        this.title = "";
        this.text = null;
        this.icon = null;
        this.iconSelfExplanatory = false;
        this.isHighPriority = false;
        this.responseNeeded = true;
        this.userClear = false;
        this.duration = null;
    }

    protected TextMessage(Parcel parcel) {
        boolean z;
        boolean z2;
        boolean z3;
        this.title = "";
        this.text = null;
        this.icon = null;
        this.iconSelfExplanatory = false;
        this.isHighPriority = false;
        this.responseNeeded = true;
        this.userClear = false;
        this.duration = null;
        this.title = parcel.readString();
        this.text = parcel.readString();
        this.icon = (Bitmap) parcel.readParcelable(null);
        if (parcel.readInt() != 1) {
            z = false;
        } else {
            z = true;
        }
        this.iconSelfExplanatory = z;
        if (parcel.readInt() != 1) {
            z2 = false;
        } else {
            z2 = true;
        }
        this.isHighPriority = z2;
        if (parcel.readInt() != 1) {
            z3 = false;
        } else {
            z3 = true;
        }
        this.responseNeeded = z3;
        this.userClear = parcel.readInt() == 1;
        this.duration = (Duration) parcel.readParcelable(null);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.title);
        parcel.writeString(this.text);
        parcel.writeParcelable(this.icon, 0);
        parcel.writeInt(this.iconSelfExplanatory ? 1 : 0);
        parcel.writeInt(this.isHighPriority ? 1 : 0);
        parcel.writeInt(this.responseNeeded ? 1 : 0);
        parcel.writeInt(this.userClear ? 1 : 0);
        parcel.writeParcelable(this.duration, 0);
    }

    public String toString() {
        return "title=" + this.title + " text=" + this.text + " icon=" + this.icon + " iconSelfExplanatory=" + this.iconSelfExplanatory + " isHighPriority=" + this.isHighPriority + " responseNeeded=" + this.responseNeeded + " userClear=" + this.userClear + " duration=" + this.duration;
    }
}
