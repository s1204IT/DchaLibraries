package com.android.setupwizardlib.template;

import android.os.Handler;
import android.os.Looper;
import com.android.setupwizardlib.TemplateLayout;

public class RequireScrollMixin implements Mixin {
    private ScrollHandlingDelegate mDelegate;
    private OnRequireScrollStateChangedListener mListener;
    private final TemplateLayout mTemplateLayout;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mRequiringScrollToBottom = false;
    private boolean mEverScrolledToBottom = false;

    public interface OnRequireScrollStateChangedListener {
        void onRequireScrollStateChanged(boolean z);
    }

    public interface ScrollHandlingDelegate {
    }

    public RequireScrollMixin(TemplateLayout templateLayout) {
        this.mTemplateLayout = templateLayout;
    }

    public void setScrollHandlingDelegate(ScrollHandlingDelegate scrollHandlingDelegate) {
        this.mDelegate = scrollHandlingDelegate;
    }

    void notifyScrollabilityChange(boolean z) {
        if (z == this.mRequiringScrollToBottom) {
            return;
        }
        if (z) {
            if (!this.mEverScrolledToBottom) {
                postScrollStateChange(true);
                this.mRequiringScrollToBottom = true;
                return;
            }
            return;
        }
        postScrollStateChange(false);
        this.mRequiringScrollToBottom = false;
        this.mEverScrolledToBottom = true;
    }

    private void postScrollStateChange(final boolean z) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (RequireScrollMixin.this.mListener != null) {
                    RequireScrollMixin.this.mListener.onRequireScrollStateChanged(z);
                }
            }
        });
    }
}
