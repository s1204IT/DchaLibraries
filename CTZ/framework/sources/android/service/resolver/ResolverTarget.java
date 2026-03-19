package android.service.resolver;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

@SystemApi
public final class ResolverTarget implements Parcelable {
    public static final Parcelable.Creator<ResolverTarget> CREATOR = new Parcelable.Creator<ResolverTarget>() {
        @Override
        public ResolverTarget createFromParcel(Parcel parcel) {
            return new ResolverTarget(parcel);
        }

        @Override
        public ResolverTarget[] newArray(int i) {
            return new ResolverTarget[i];
        }
    };
    private static final String TAG = "ResolverTarget";
    private float mChooserScore;
    private float mLaunchScore;
    private float mRecencyScore;
    private float mSelectProbability;
    private float mTimeSpentScore;

    public ResolverTarget() {
    }

    ResolverTarget(Parcel parcel) {
        this.mRecencyScore = parcel.readFloat();
        this.mTimeSpentScore = parcel.readFloat();
        this.mLaunchScore = parcel.readFloat();
        this.mChooserScore = parcel.readFloat();
        this.mSelectProbability = parcel.readFloat();
    }

    public float getRecencyScore() {
        return this.mRecencyScore;
    }

    public void setRecencyScore(float f) {
        this.mRecencyScore = f;
    }

    public float getTimeSpentScore() {
        return this.mTimeSpentScore;
    }

    public void setTimeSpentScore(float f) {
        this.mTimeSpentScore = f;
    }

    public float getLaunchScore() {
        return this.mLaunchScore;
    }

    public void setLaunchScore(float f) {
        this.mLaunchScore = f;
    }

    public float getChooserScore() {
        return this.mChooserScore;
    }

    public void setChooserScore(float f) {
        this.mChooserScore = f;
    }

    public float getSelectProbability() {
        return this.mSelectProbability;
    }

    public void setSelectProbability(float f) {
        this.mSelectProbability = f;
    }

    public String toString() {
        return "ResolverTarget{" + this.mRecencyScore + ", " + this.mTimeSpentScore + ", " + this.mLaunchScore + ", " + this.mChooserScore + ", " + this.mSelectProbability + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloat(this.mRecencyScore);
        parcel.writeFloat(this.mTimeSpentScore);
        parcel.writeFloat(this.mLaunchScore);
        parcel.writeFloat(this.mChooserScore);
        parcel.writeFloat(this.mSelectProbability);
    }
}
