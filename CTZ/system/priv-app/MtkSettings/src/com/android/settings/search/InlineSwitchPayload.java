package com.android.settings.search;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class InlineSwitchPayload extends InlinePayload {
    public static final Parcelable.Creator<InlineSwitchPayload> CREATOR = new Parcelable.Creator<InlineSwitchPayload>() { // from class: com.android.settings.search.InlineSwitchPayload.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public InlineSwitchPayload createFromParcel(Parcel parcel) {
            return new InlineSwitchPayload(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
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

    @Override // com.android.settings.search.InlinePayload, com.android.settings.search.ResultPayload, android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(this.mIsStandard ? 1 : 0);
    }
}
