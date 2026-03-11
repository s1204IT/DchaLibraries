package android.support.v4.widget;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.AbsSavedState;

class SlidingPaneLayout$SavedState extends AbsSavedState {
    public static final Parcelable.Creator<SlidingPaneLayout$SavedState> CREATOR = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SlidingPaneLayout$SavedState>() {
        @Override
        public SlidingPaneLayout$SavedState createFromParcel(Parcel in, ClassLoader loader) {
            return new SlidingPaneLayout$SavedState(in, loader, null);
        }

        @Override
        public SlidingPaneLayout$SavedState[] newArray(int size) {
            return new SlidingPaneLayout$SavedState[size];
        }
    });
    boolean isOpen;

    SlidingPaneLayout$SavedState(Parcel in, ClassLoader loader, SlidingPaneLayout$SavedState slidingPaneLayout$SavedState) {
        this(in, loader);
    }

    private SlidingPaneLayout$SavedState(Parcel in, ClassLoader loader) {
        super(in, loader);
        this.isOpen = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(this.isOpen ? 1 : 0);
    }
}
