package com.android.camera.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.camera2.R;

public abstract class AbstractTutorialOverlay {
    protected final CloseListener mCloseListener;
    private final int mLayoutResId;
    private ViewGroup mPlaceholderWrapper;

    public interface CloseListener {
        void onTutorialClosed();
    }

    protected abstract void onInflated(View view);

    public AbstractTutorialOverlay(int layoutResId, CloseListener closeListener) {
        this.mLayoutResId = layoutResId;
        this.mCloseListener = closeListener;
    }

    public final void show(ViewGroup placeHolderWrapper, LayoutInflater inflater) {
        this.mPlaceholderWrapper = placeHolderWrapper;
        if (this.mPlaceholderWrapper != null) {
            this.mPlaceholderWrapper.removeAllViews();
        }
        this.mPlaceholderWrapper.setVisibility(0);
        ViewGroup placeHolder = (ViewGroup) inflater.inflate(R.layout.tutorials_placeholder, this.mPlaceholderWrapper).findViewById(R.id.tutorials_placeholder);
        onInflated(inflater.inflate(this.mLayoutResId, placeHolder));
    }

    public void removeOverlayAndHideWrapper() {
        if (this.mPlaceholderWrapper != null) {
            this.mPlaceholderWrapper.removeAllViews();
        }
        this.mPlaceholderWrapper.setVisibility(8);
    }

    public void close() {
        removeOverlayAndHideWrapper();
        if (this.mCloseListener != null) {
            this.mCloseListener.onTutorialClosed();
        }
    }
}
