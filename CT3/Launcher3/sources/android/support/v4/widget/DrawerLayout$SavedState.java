package android.support.v4.widget;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.AbsSavedState;

class DrawerLayout$SavedState extends AbsSavedState {
    public static final Parcelable.Creator<DrawerLayout$SavedState> CREATOR = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<DrawerLayout$SavedState>() {
        @Override
        public DrawerLayout$SavedState createFromParcel(Parcel in, ClassLoader loader) {
            return new DrawerLayout$SavedState(in, loader);
        }

        @Override
        public DrawerLayout$SavedState[] newArray(int size) {
            return new DrawerLayout$SavedState[size];
        }
    });
    int lockModeEnd;
    int lockModeLeft;
    int lockModeRight;
    int lockModeStart;
    int openDrawerGravity;

    public DrawerLayout$SavedState(Parcel in, ClassLoader loader) {
        super(in, loader);
        this.openDrawerGravity = 0;
        this.openDrawerGravity = in.readInt();
        this.lockModeLeft = in.readInt();
        this.lockModeRight = in.readInt();
        this.lockModeStart = in.readInt();
        this.lockModeEnd = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.openDrawerGravity);
        dest.writeInt(this.lockModeLeft);
        dest.writeInt(this.lockModeRight);
        dest.writeInt(this.lockModeStart);
        dest.writeInt(this.lockModeEnd);
    }
}
