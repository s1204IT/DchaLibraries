package android.app;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Rational;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public final class PictureInPictureArgs implements Parcelable {
    public static final Parcelable.Creator<PictureInPictureArgs> CREATOR = new Parcelable.Creator<PictureInPictureArgs>() {
        @Override
        public PictureInPictureArgs createFromParcel(Parcel parcel) {
            return new PictureInPictureArgs(parcel);
        }

        @Override
        public PictureInPictureArgs[] newArray(int i) {
            return new PictureInPictureArgs[i];
        }
    };
    private Rational mAspectRatio;
    private Rect mSourceRectHint;
    private Rect mSourceRectHintInsets;
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

        public PictureInPictureArgs build() {
            return new PictureInPictureArgs(this.mAspectRatio, this.mUserActions, this.mSourceRectHint);
        }
    }

    @Deprecated
    public PictureInPictureArgs() {
    }

    @Deprecated
    public PictureInPictureArgs(float f, List<RemoteAction> list) {
        setAspectRatio(f);
        setActions(list);
    }

    private PictureInPictureArgs(Parcel parcel) {
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

    private PictureInPictureArgs(Rational rational, List<RemoteAction> list, Rect rect) {
        this.mAspectRatio = rational;
        this.mUserActions = list;
        this.mSourceRectHint = rect;
    }

    @Deprecated
    public void setAspectRatio(float f) {
        this.mAspectRatio = new Rational((int) (f * 1.0E9f), 1000000000);
    }

    @Deprecated
    public void setActions(List<RemoteAction> list) {
        if (this.mUserActions != null) {
            this.mUserActions = null;
        }
        if (list != null) {
            this.mUserActions = new ArrayList(list);
        }
    }

    @Deprecated
    public void setSourceRectHint(Rect rect) {
        if (rect == null) {
            this.mSourceRectHint = null;
        } else {
            this.mSourceRectHint = new Rect(rect);
        }
    }

    public void copyOnlySet(PictureInPictureArgs pictureInPictureArgs) {
        if (pictureInPictureArgs.hasSetAspectRatio()) {
            this.mAspectRatio = pictureInPictureArgs.mAspectRatio;
        }
        if (pictureInPictureArgs.hasSetActions()) {
            this.mUserActions = pictureInPictureArgs.mUserActions;
        }
        if (pictureInPictureArgs.hasSourceBoundsHint()) {
            this.mSourceRectHint = new Rect(pictureInPictureArgs.getSourceRectHint());
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

    @Deprecated
    public void setSourceRectHintInsets(Rect rect) {
        if (rect == null) {
            this.mSourceRectHintInsets = null;
        } else {
            this.mSourceRectHintInsets = new Rect(rect);
        }
    }

    public Rect getSourceRectHint() {
        return this.mSourceRectHint;
    }

    public Rect getSourceRectHintInsets() {
        return this.mSourceRectHintInsets;
    }

    public boolean hasSourceBoundsHint() {
        return (this.mSourceRectHint == null || this.mSourceRectHint.isEmpty()) ? false : true;
    }

    public boolean hasSourceBoundsHintInsets() {
        return this.mSourceRectHintInsets != null;
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

    public static PictureInPictureArgs convert(PictureInPictureParams pictureInPictureParams) {
        return new PictureInPictureArgs(pictureInPictureParams.getAspectRatioRational(), pictureInPictureParams.getActions(), pictureInPictureParams.getSourceRectHint());
    }

    public static PictureInPictureParams convert(PictureInPictureArgs pictureInPictureArgs) {
        return new PictureInPictureParams(pictureInPictureArgs.getAspectRatioRational(), pictureInPictureArgs.getActions(), pictureInPictureArgs.getSourceRectHint());
    }
}
