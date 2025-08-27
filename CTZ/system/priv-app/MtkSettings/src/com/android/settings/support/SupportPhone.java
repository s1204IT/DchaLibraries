package com.android.settings.support;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public final class SupportPhone implements Parcelable {
    public static final Parcelable.Creator<SupportPhone> CREATOR = new Parcelable.Creator<SupportPhone>() { // from class: com.android.settings.support.SupportPhone.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SupportPhone createFromParcel(Parcel parcel) {
            return new SupportPhone(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public SupportPhone[] newArray(int i) {
            return new SupportPhone[i];
        }
    };
    public final boolean isTollFree;
    public final String language;
    public final String number;

    protected SupportPhone(Parcel parcel) {
        this.language = parcel.readString();
        this.number = parcel.readString();
        this.isTollFree = parcel.readInt() != 0;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.language);
        parcel.writeString(this.number);
        parcel.writeInt(this.isTollFree ? 1 : 0);
    }
}
