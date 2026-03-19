package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class AbsSavedState implements Parcelable {
    private final Parcelable mSuperState;
    public static final AbsSavedState EMPTY_STATE = new AbsSavedState() {
    };
    public static final Parcelable.Creator<AbsSavedState> CREATOR = new Parcelable.ClassLoaderCreator<AbsSavedState>() {
        @Override
        public AbsSavedState createFromParcel(Parcel parcel) {
            return createFromParcel(parcel, (ClassLoader) null);
        }

        @Override
        public AbsSavedState createFromParcel(Parcel parcel, ClassLoader classLoader) {
            if (parcel.readParcelable(classLoader) != null) {
                throw new IllegalStateException("superState must be null");
            }
            return AbsSavedState.EMPTY_STATE;
        }

        @Override
        public AbsSavedState[] newArray(int i) {
            return new AbsSavedState[i];
        }
    };

    private AbsSavedState() {
        this.mSuperState = null;
    }

    protected AbsSavedState(Parcelable parcelable) {
        if (parcelable == null) {
            throw new IllegalArgumentException("superState must not be null");
        }
        this.mSuperState = parcelable == EMPTY_STATE ? null : parcelable;
    }

    protected AbsSavedState(Parcel parcel) {
        this(parcel, null);
    }

    protected AbsSavedState(Parcel parcel, ClassLoader classLoader) {
        Parcelable parcelable = parcel.readParcelable(classLoader);
        this.mSuperState = parcelable == null ? EMPTY_STATE : parcelable;
    }

    public final Parcelable getSuperState() {
        return this.mSuperState;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mSuperState, i);
    }
}
