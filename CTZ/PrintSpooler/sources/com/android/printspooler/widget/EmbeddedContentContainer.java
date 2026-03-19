package com.android.printspooler.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class EmbeddedContentContainer extends FrameLayout {
    private OnSizeChangeListener mSizeChangeListener;

    public interface OnSizeChangeListener {
        void onSizeChanged(int i, int i2);
    }

    public EmbeddedContentContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void setOnSizeChangeListener(OnSizeChangeListener onSizeChangeListener) {
        this.mSizeChangeListener = onSizeChangeListener;
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (this.mSizeChangeListener != null) {
            this.mSizeChangeListener.onSizeChanged(i, i2);
        }
    }
}
