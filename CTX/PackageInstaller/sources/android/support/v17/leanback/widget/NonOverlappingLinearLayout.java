package android.support.v17.leanback.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import java.util.ArrayList;

public class NonOverlappingLinearLayout extends LinearLayout {
    boolean mDeferFocusableViewAvailableInLayout;
    boolean mFocusableViewAvailableFixEnabled;
    final ArrayList<ArrayList<View>> mSortedAvailableViews;

    public NonOverlappingLinearLayout(Context context) {
        this(context, null);
    }

    public NonOverlappingLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NonOverlappingLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mFocusableViewAvailableFixEnabled = false;
        this.mSortedAvailableViews = new ArrayList<>();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setFocusableViewAvailableFixEnabled(boolean enabled) {
        this.mFocusableViewAvailableFixEnabled = enabled;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        boolean z2;
        int size;
        ?? r0 = 0;
        int i5 = 0;
        try {
            this.mDeferFocusableViewAvailableInLayout = this.mFocusableViewAvailableFixEnabled && getOrientation() == 0 && getLayoutDirection() == 1;
            if (this.mDeferFocusableViewAvailableInLayout) {
                while (this.mSortedAvailableViews.size() > getChildCount()) {
                    this.mSortedAvailableViews.remove(this.mSortedAvailableViews.size() - 1);
                }
                while (this.mSortedAvailableViews.size() < getChildCount()) {
                    this.mSortedAvailableViews.add(new ArrayList<>());
                }
            }
            super.onLayout(z, i, i2, i3, i4);
            if (this.mDeferFocusableViewAvailableInLayout) {
                for (int i6 = 0; i6 < this.mSortedAvailableViews.size(); i6++) {
                    for (int i7 = 0; i7 < this.mSortedAvailableViews.get(i6).size(); i7++) {
                        super.focusableViewAvailable(this.mSortedAvailableViews.get(i6).get(i7));
                    }
                }
            }
            if (z2) {
                while (true) {
                    if (i5 >= size) {
                        return;
                    }
                }
            }
        } finally {
            if (this.mDeferFocusableViewAvailableInLayout) {
                this.mDeferFocusableViewAvailableInLayout = false;
                while (r0 < this.mSortedAvailableViews.size()) {
                    this.mSortedAvailableViews.get(r0).clear();
                    r0++;
                }
            }
        }
    }

    @Override
    public void focusableViewAvailable(View v) {
        if (this.mDeferFocusableViewAvailableInLayout) {
            View i = v;
            int index = -1;
            while (true) {
                if (i == this || i == null) {
                    break;
                }
                if (i.getParent() == this) {
                    index = indexOfChild(i);
                    break;
                }
                i = (View) i.getParent();
            }
            if (index != -1) {
                this.mSortedAvailableViews.get(index).add(v);
                return;
            }
            return;
        }
        super.focusableViewAvailable(v);
    }
}
