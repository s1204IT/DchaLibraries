package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class AbsSavedState implements Parcelable {
    private final Parcelable mSuperState;
    public static final AbsSavedState EMPTY_STATE = new AbsSavedState() {
    };
    public static final Parcelable.Creator<AbsSavedState> CREATOR = new Parcelable.Creator<AbsSavedState>() {
        @Override
        public AbsSavedState createFromParcel(Parcel in) {
            Parcelable superState = in.readParcelable(null);
            if (superState != null) {
                throw new IllegalStateException("superState must be null");
            }
            return AbsSavedState.EMPTY_STATE;
        }

        @Override
        public AbsSavedState[] newArray(int size) {
            return new AbsSavedState[size];
        }
    };

    private AbsSavedState() {
        this.mSuperState = null;
    }

    protected AbsSavedState(Parcelable superState) {
        if (superState == null) {
            throw new IllegalArgumentException("superState must not be null");
        }
        this.mSuperState = superState == EMPTY_STATE ? null : superState;
    }

    protected AbsSavedState(Parcel source) {
        Parcelable superState = source.readParcelable(null);
        this.mSuperState = superState == null ? EMPTY_STATE : superState;
    }

    public final Parcelable getSuperState() {
        return this.mSuperState;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mSuperState, flags);
    }
}
