package android.app;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Rational;
import java.util.ArrayList;
import java.util.List;

public final class PictureInPictureParams implements Parcelable {
    public static final Parcelable.Creator<PictureInPictureParams> CREATOR = new Parcelable.Creator<PictureInPictureParams>() {
        @Override
        public PictureInPictureParams createFromParcel(Parcel parcel) {
            return new PictureInPictureParams(parcel);
        }

        @Override
        public PictureInPictureParams[] newArray(int i) {
            return new PictureInPictureParams[i];
        }
    };
    private Rational mAspectRatio;
    private Rect mSourceRectHint;
    private List<RemoteAction> mUserActions;

    public static class Builder {
        private Rational mAspectRatio;
        private Rect mSourceRectHint;
        private List<RemoteAction> mUserActions;

        public Builder setAspectRatio(Rational rational) {
            this.mAspectRatio = rational;
            return this;
        }

        public Builder setActions(List<RemoteAction> list) {
            if (this.mUserActions != null) {
                this.mUserActions = null;
            }
            if (list != null) {
                this.mUserActions = new ArrayList(list);
            }
            return this;
        }

        public Builder setSourceRectHint(Rect rect) {
            if (rect == null) {
                this.mSourceRectHint = null;
            } else {
                this.mSourceRectHint = new Rect(rect);
            }
            return this;
        }

        public PictureInPictureParams build() {
            return new PictureInPictureParams(this.mAspectRatio, this.mUserActions, this.mSourceRectHint);
        }
    }

    PictureInPictureParams() {
    }

    PictureInPictureParams(Parcel parcel) {
        if (parcel.readInt() != 0) {
            this.mAspectRatio = new Rational(parcel.readInt(), parcel.readInt());
        }
        if (parcel.readInt() != 0) {
            this.mUserActions = new ArrayList();
            parcel.readParcelableList(this.mUserActions, RemoteAction.class.getClassLoader());
        }
        if (parcel.readInt() != 0) {
            this.mSourceRectHint = Rect.CREATOR.createFromParcel(parcel);
        }
    }

    PictureInPictureParams(Rational rational, List<RemoteAction> list, Rect rect) {
        this.mAspectRatio = rational;
        this.mUserActions = list;
        this.mSourceRectHint = rect;
    }

    public void copyOnlySet(PictureInPictureParams pictureInPictureParams) {
        if (pictureInPictureParams.hasSetAspectRatio()) {
            this.mAspectRatio = pictureInPictureParams.mAspectRatio;
        }
        if (pictureInPictureParams.hasSetActions()) {
            this.mUserActions = pictureInPictureParams.mUserActions;
        }
        if (pictureInPictureParams.hasSourceBoundsHint()) {
            this.mSourceRectHint = new Rect(pictureInPictureParams.getSourceRectHint());
        }
    }

    public float getAspectRatio() {
        if (this.mAspectRatio != null) {
            return this.mAspectRatio.floatValue();
        }
        return 0.0f;
    }

    public Rational getAspectRatioRational() {
        return this.mAspectRatio;
    }

    public boolean hasSetAspectRatio() {
        return this.mAspectRatio != null;
    }

    public List<RemoteAction> getActions() {
        return this.mUserActions;
    }

    public boolean hasSetActions() {
        return this.mUserActions != null;
    }

    public void truncateActions(int i) {
        if (hasSetActions()) {
            this.mUserActions = this.mUserActions.subList(0, Math.min(this.mUserActions.size(), i));
        }
    }

    public Rect getSourceRectHint() {
        return this.mSourceRectHint;
    }

    public boolean hasSourceBoundsHint() {
        return (this.mSourceRectHint == null || this.mSourceRectHint.isEmpty()) ? false : true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (this.mAspectRatio != null) {
            parcel.writeInt(1);
            parcel.writeInt(this.mAspectRatio.getNumerator());
            parcel.writeInt(this.mAspectRatio.getDenominator());
        } else {
            parcel.writeInt(0);
        }
        if (this.mUserActions != null) {
            parcel.writeInt(1);
            parcel.writeParcelableList(this.mUserActions, 0);
        } else {
            parcel.writeInt(0);
        }
        if (this.mSourceRectHint != null) {
            parcel.writeInt(1);
            this.mSourceRectHint.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
    }
}
