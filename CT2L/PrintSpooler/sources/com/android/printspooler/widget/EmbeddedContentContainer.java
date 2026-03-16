package com.android.printspooler.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class EmbeddedContentContainer extends FrameLayout {
    private OnSizeChangeListener mSizeChangeListener;

    public interface OnSizeChangeListener {
        void onSizeChanged(int i, int i2);
    }

    public EmbeddedContentContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOnSizeChangeListener(OnSizeChangeListener listener) {
        this.mSizeChangeListener = listener;
    }

    @Override
    protected void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
        if (this.mSizeChangeListener != null) {
            this.mSizeChangeListener.onSizeChanged(newWidth, newHeight);
        }
    }
}
