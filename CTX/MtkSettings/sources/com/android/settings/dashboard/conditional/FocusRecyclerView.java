package com.android.settings.dashboard.conditional;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

public class FocusRecyclerView extends RecyclerView {
    private DetachListener mDetachListener;
    private FocusListener mListener;

    public interface DetachListener {
        void onDetachedFromWindow();
    }

    public interface FocusListener {
        void onWindowFocusChanged(boolean z);
    }

    public FocusRecyclerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (this.mListener != null) {
            this.mListener.onWindowFocusChanged(z);
        }
    }

    public void setListener(FocusListener focusListener) {
        this.mListener = focusListener;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mDetachListener != null) {
            this.mDetachListener.onDetachedFromWindow();
        }
    }

    public void setDetachListener(DetachListener detachListener) {
        this.mDetachListener = detachListener;
    }
}
