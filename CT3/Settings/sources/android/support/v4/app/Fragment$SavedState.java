package android.support.v4.app;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class Fragment$SavedState implements Parcelable {
    public static final Parcelable.Creator<Fragment$SavedState> CREATOR = new Parcelable.Creator<Fragment$SavedState>() {
        @Override
        public Fragment$SavedState createFromParcel(Parcel in) {
            return new Fragment$SavedState(in, null);
        }

        @Override
        public Fragment$SavedState[] newArray(int size) {
            return new Fragment$SavedState[size];
        }
    };
    final Bundle mState;

    Fragment$SavedState(Parcel in, ClassLoader loader) {
        this.mState = in.readBundle();
        if (loader == null || this.mState == null) {
            return;
        }
        this.mState.setClassLoader(loader);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(this.mState);
    }
}
