package com.android.systemui.statusbar.notification;

import android.view.View;
import android.view.ViewGroup;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.statusbar.ExpandableNotificationRow;

public class AboveShelfObserver implements AboveShelfChangedListener {
    private boolean mHasViewsAboveShelf = false;
    private final ViewGroup mHostLayout;
    private HasViewAboveShelfChangedListener mListener;

    public interface HasViewAboveShelfChangedListener {
        void onHasViewsAboveShelfChanged(boolean z);
    }

    public AboveShelfObserver(ViewGroup viewGroup) {
        this.mHostLayout = viewGroup;
    }

    public void setListener(HasViewAboveShelfChangedListener hasViewAboveShelfChangedListener) {
        this.mListener = hasViewAboveShelfChangedListener;
    }

    @Override
    public void onAboveShelfStateChanged(boolean z) {
        if (!z && this.mHostLayout != null) {
            int childCount = this.mHostLayout.getChildCount();
            int i = 0;
            while (true) {
                if (i >= childCount) {
                    break;
                }
                View childAt = this.mHostLayout.getChildAt(i);
                if (!(childAt instanceof ExpandableNotificationRow) || !((ExpandableNotificationRow) childAt).isAboveShelf()) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            }
        }
        if (this.mHasViewsAboveShelf != z) {
            this.mHasViewsAboveShelf = z;
            if (this.mListener != null) {
                this.mListener.onHasViewsAboveShelfChanged(z);
            }
        }
    }

    @VisibleForTesting
    boolean hasViewsAboveShelf() {
        return this.mHasViewsAboveShelf;
    }
}
