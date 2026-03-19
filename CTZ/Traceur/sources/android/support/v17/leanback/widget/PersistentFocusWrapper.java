package android.support.v17.leanback.widget;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.util.ArrayList;

class PersistentFocusWrapper extends FrameLayout {
    private boolean mPersistFocusVertical;
    private int mSelectedPosition;

    int getGrandChildCount() {
        ViewGroup wrapper = (ViewGroup) getChildAt(0);
        if (wrapper == null) {
            return 0;
        }
        return wrapper.getChildCount();
    }

    private boolean shouldPersistFocusFromDirection(int direction) {
        return (this.mPersistFocusVertical && (direction == 33 || direction == 130)) || (!this.mPersistFocusVertical && (direction == 17 || direction == 66));
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (hasFocus() || getGrandChildCount() == 0 || !shouldPersistFocusFromDirection(direction)) {
            super.addFocusables(views, direction, focusableMode);
        } else {
            views.add(this);
        }
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        View view = focused;
        while (view != null && view.getParent() != child) {
            view = (View) view.getParent();
        }
        this.mSelectedPosition = view == null ? -1 : ((ViewGroup) child).indexOfChild(view);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        ViewGroup wrapper = (ViewGroup) getChildAt(0);
        if (wrapper != null && this.mSelectedPosition >= 0 && this.mSelectedPosition < getGrandChildCount() && wrapper.getChildAt(this.mSelectedPosition).requestFocus(direction, previouslyFocusedRect)) {
            return true;
        }
        return super.requestFocus(direction, previouslyFocusedRect);
    }

    static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int mSelectedPosition;

        SavedState(Parcel in) {
            super(in);
            this.mSelectedPosition = in.readInt();
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(this.mSelectedPosition);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mSelectedPosition = this.mSelectedPosition;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (!(parcelable instanceof SavedState)) {
            super.onRestoreInstanceState(parcelable);
        } else {
            this.mSelectedPosition = parcelable.mSelectedPosition;
            super.onRestoreInstanceState(parcelable.getSuperState());
        }
    }
}
