package com.android.settings.search;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class InlineListPayload extends InlinePayload {
    public static final Parcelable.Creator<InlineListPayload> CREATOR = new Parcelable.Creator<InlineListPayload>() { // from class: com.android.settings.search.InlineListPayload.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InlineListPayload createFromParcel(Parcel parcel) {
            return new InlineListPayload(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InlineListPayload[] newArray(int i) {
            return new InlineListPayload[i];
        }
    };
    private int mNumOptions;

    private InlineListPayload(Parcel parcel) {
        super(parcel);
        this.mNumOptions = parcel.readInt();
    }

    @Override // com.android.settings.search.InlinePayload, com.android.settings.search.ResultPayload, android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(this.mNumOptions);
    }
}
