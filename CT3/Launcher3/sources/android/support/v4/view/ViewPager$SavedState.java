package android.support.v4.view;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;

public class ViewPager$SavedState extends AbsSavedState {
    public static final Parcelable.Creator<ViewPager$SavedState> CREATOR = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<ViewPager$SavedState>() {
        @Override
        public ViewPager$SavedState createFromParcel(Parcel in, ClassLoader loader) {
            return new ViewPager$SavedState(in, loader);
        }

        @Override
        public ViewPager$SavedState[] newArray(int size) {
            return new ViewPager$SavedState[size];
        }
    });
    Parcelable adapterState;
    ClassLoader loader;
    int position;

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(this.position);
        out.writeParcelable(this.adapterState, flags);
    }

    public String toString() {
        return "FragmentPager.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " position=" + this.position + "}";
    }

    ViewPager$SavedState(Parcel in, ClassLoader loader) {
        super(in, loader);
        loader = loader == null ? getClass().getClassLoader() : loader;
        this.position = in.readInt();
        this.adapterState = in.readParcelable(loader);
        this.loader = loader;
    }
}
