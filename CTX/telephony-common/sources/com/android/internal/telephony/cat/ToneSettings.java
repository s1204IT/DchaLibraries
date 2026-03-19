package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class ToneSettings implements Parcelable {
    public static final Parcelable.Creator<ToneSettings> CREATOR = new Parcelable.Creator<ToneSettings>() {
        @Override
        public ToneSettings createFromParcel(Parcel parcel) {
            return new ToneSettings(parcel);
        }

        @Override
        public ToneSettings[] newArray(int i) {
            return new ToneSettings[i];
        }
    };
    public Duration duration;
    public Tone tone;
    public boolean vibrate;

    public ToneSettings(Duration duration, Tone tone, boolean z) {
        this.duration = duration;
        this.tone = tone;
        this.vibrate = z;
    }

    private ToneSettings(Parcel parcel) {
        this.duration = (Duration) parcel.readParcelable(null);
        this.tone = (Tone) parcel.readParcelable(null);
        this.vibrate = parcel.readInt() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.duration, 0);
        parcel.writeParcelable(this.tone, 0);
        parcel.writeInt(this.vibrate ? 1 : 0);
    }
}
