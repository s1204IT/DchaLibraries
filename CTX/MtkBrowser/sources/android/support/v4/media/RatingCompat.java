package android.support.v4.media;

import android.os.Parcel;
import android.os.Parcelable;

public final class RatingCompat implements Parcelable {
    public static final Parcelable.Creator<RatingCompat> CREATOR = new Parcelable.Creator<RatingCompat>() {
        @Override
        public RatingCompat createFromParcel(Parcel parcel) {
            return new RatingCompat(parcel.readInt(), parcel.readFloat());
        }

        @Override
        public RatingCompat[] newArray(int i) {
            return new RatingCompat[i];
        }
    };
    private final int mRatingStyle;
    private final float mRatingValue;

    RatingCompat(int i, float f) {
        this.mRatingStyle = i;
        this.mRatingValue = f;
    }

    @Override
    public int describeContents() {
        return this.mRatingStyle;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Rating:style=");
        sb.append(this.mRatingStyle);
        sb.append(" rating=");
        sb.append(this.mRatingValue < 0.0f ? "unrated" : String.valueOf(this.mRatingValue));
        return sb.toString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mRatingStyle);
        parcel.writeFloat(this.mRatingValue);
    }
}
