package android.support.v4.widget;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

class NestedScrollView$SavedState extends View.BaseSavedState {
    public static final Parcelable.Creator<NestedScrollView$SavedState> CREATOR = new Parcelable.Creator<NestedScrollView$SavedState>() {
        @Override
        public NestedScrollView$SavedState createFromParcel(Parcel in) {
            return new NestedScrollView$SavedState(in);
        }

        @Override
        public NestedScrollView$SavedState[] newArray(int size) {
            return new NestedScrollView$SavedState[size];
        }
    };
    public int scrollPosition;

    public NestedScrollView$SavedState(Parcel source) {
        super(source);
        this.scrollPosition = source.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.scrollPosition);
    }

    public String toString() {
        return "HorizontalScrollView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " scrollPosition=" + this.scrollPosition + "}";
    }
}
