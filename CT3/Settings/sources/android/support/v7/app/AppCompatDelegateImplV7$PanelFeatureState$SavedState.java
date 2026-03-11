package android.support.v7.app;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;

class AppCompatDelegateImplV7$PanelFeatureState$SavedState implements Parcelable {
    public static final Parcelable.Creator<AppCompatDelegateImplV7$PanelFeatureState$SavedState> CREATOR = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<AppCompatDelegateImplV7$PanelFeatureState$SavedState>() {
        @Override
        public AppCompatDelegateImplV7$PanelFeatureState$SavedState createFromParcel(Parcel in, ClassLoader loader) {
            return AppCompatDelegateImplV7$PanelFeatureState$SavedState.readFromParcel(in, loader);
        }

        @Override
        public AppCompatDelegateImplV7$PanelFeatureState$SavedState[] newArray(int size) {
            return new AppCompatDelegateImplV7$PanelFeatureState$SavedState[size];
        }
    });
    int featureId;
    boolean isOpen;
    Bundle menuState;

    private AppCompatDelegateImplV7$PanelFeatureState$SavedState() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.featureId);
        dest.writeInt(this.isOpen ? 1 : 0);
        if (!this.isOpen) {
            return;
        }
        dest.writeBundle(this.menuState);
    }

    public static AppCompatDelegateImplV7$PanelFeatureState$SavedState readFromParcel(Parcel source, ClassLoader loader) {
        AppCompatDelegateImplV7$PanelFeatureState$SavedState savedState = new AppCompatDelegateImplV7$PanelFeatureState$SavedState();
        savedState.featureId = source.readInt();
        savedState.isOpen = source.readInt() == 1;
        if (savedState.isOpen) {
            savedState.menuState = source.readBundle(loader);
        }
        return savedState;
    }
}
