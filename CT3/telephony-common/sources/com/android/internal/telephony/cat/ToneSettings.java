package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class ToneSettings implements Parcelable {
    public static final Parcelable.Creator<ToneSettings> CREATOR = new Parcelable.Creator<ToneSettings>() {
        @Override
        public ToneSettings createFromParcel(Parcel in) {
            return new ToneSettings(in, null);
        }

        @Override
        public ToneSettings[] newArray(int size) {
            return new ToneSettings[size];
        }
    };
    public Duration duration;
    public Tone tone;
    public boolean vibrate;

    ToneSettings(Parcel in, ToneSettings toneSettings) {
        this(in);
    }

    public ToneSettings(Duration duration, Tone tone, boolean vibrate) {
        this.duration = duration;
        this.tone = tone;
        this.vibrate = vibrate;
    }

    private ToneSettings(Parcel in) {
        this.duration = (Duration) in.readParcelable(null);
        this.tone = (Tone) in.readParcelable(null);
        this.vibrate = in.readInt() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.duration, 0);
        dest.writeParcelable(this.tone, 0);
        dest.writeInt(this.vibrate ? 1 : 0);
    }
}
