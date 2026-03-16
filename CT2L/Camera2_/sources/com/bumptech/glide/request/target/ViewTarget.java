package com.bumptech.glide.request.target;

import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.Target;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public abstract class ViewTarget<T extends View, Z> implements Target<Z> {
    private static final String TAG = "ViewTarget";
    private final SizeDeterminer sizeDeterminer;
    private final T view;

    public ViewTarget(T view) {
        this.view = view;
        this.sizeDeterminer = new SizeDeterminer(view);
    }

    public T getView() {
        return this.view;
    }

    @Override
    public void getSize(Target.SizeReadyCallback cb) {
        this.sizeDeterminer.getSize(cb);
    }

    @Override
    public void setRequest(Request request) {
        this.view.setTag(request);
    }

    @Override
    public Request getRequest() {
        Object tag = this.view.getTag();
        if (tag == null) {
            return null;
        }
        if (tag instanceof Request) {
            Request request = (Request) tag;
            return request;
        }
        throw new IllegalArgumentException("You must not call setTag() on a view Glide is targeting");
    }

    public String toString() {
        return "Target for: " + this.view;
    }

    private static class SizeDeterminer {
        private Set<Target.SizeReadyCallback> cbs = new HashSet();
        private SizeDeterminerLayoutListener layoutListener;
        private final View view;

        public SizeDeterminer(View view) {
            this.view = view;
        }

        private void notifyCbs(int width, int height) {
            for (Target.SizeReadyCallback cb : this.cbs) {
                cb.onSizeReady(width, height);
            }
            this.cbs.clear();
        }

        private void checkCurrentDimens() {
            if (!this.cbs.isEmpty()) {
                boolean calledCallback = true;
                ViewGroup.LayoutParams layoutParams = this.view.getLayoutParams();
                if (isViewSizeValid()) {
                    notifyCbs(this.view.getWidth(), this.view.getHeight());
                } else if (isLayoutParamsSizeValid()) {
                    notifyCbs(layoutParams.width, layoutParams.height);
                } else {
                    calledCallback = false;
                }
                if (calledCallback) {
                    ViewTreeObserver observer = this.view.getViewTreeObserver();
                    if (observer.isAlive()) {
                        observer.removeOnPreDrawListener(this.layoutListener);
                    }
                }
            }
        }

        public void getSize(Target.SizeReadyCallback cb) {
            ViewGroup.LayoutParams layoutParams = this.view.getLayoutParams();
            if (isViewSizeValid()) {
                cb.onSizeReady(this.view.getWidth(), this.view.getHeight());
                return;
            }
            if (isLayoutParamsSizeValid()) {
                cb.onSizeReady(layoutParams.width, layoutParams.height);
                return;
            }
            if (isUsingWrapContent()) {
                WindowManager windowManager = (WindowManager) this.view.getContext().getSystemService("window");
                Display display = windowManager.getDefaultDisplay();
                int width = display.getWidth();
                int height = display.getHeight();
                if (Log.isLoggable(ViewTarget.TAG, 5)) {
                    Log.w(ViewTarget.TAG, "Trying to load image into ImageView using WRAP_CONTENT, defaulting to screen dimensions: [" + width + "x" + height + "]. Give the view an actual width and height  for better performance.");
                }
                cb.onSizeReady(display.getWidth(), display.getHeight());
                return;
            }
            this.cbs.add(cb);
            ViewTreeObserver observer = this.view.getViewTreeObserver();
            this.layoutListener = new SizeDeterminerLayoutListener(this);
            observer.addOnPreDrawListener(this.layoutListener);
        }

        private boolean isViewSizeValid() {
            return this.view.getWidth() > 0 && this.view.getHeight() > 0;
        }

        private boolean isUsingWrapContent() {
            ViewGroup.LayoutParams layoutParams = this.view.getLayoutParams();
            return layoutParams != null && (layoutParams.width == -2 || layoutParams.height == -2);
        }

        private boolean isLayoutParamsSizeValid() {
            ViewGroup.LayoutParams layoutParams = this.view.getLayoutParams();
            return layoutParams != null && layoutParams.width > 0 && layoutParams.height > 0;
        }

        private static class SizeDeterminerLayoutListener implements ViewTreeObserver.OnPreDrawListener {
            private final WeakReference<SizeDeterminer> sizeDeterminerRef;

            public SizeDeterminerLayoutListener(SizeDeterminer sizeDeterminer) {
                this.sizeDeterminerRef = new WeakReference<>(sizeDeterminer);
            }

            @Override
            public boolean onPreDraw() {
                if (Log.isLoggable(ViewTarget.TAG, 2)) {
                    Log.v(ViewTarget.TAG, "OnGlobalLayoutListener called listener=" + this);
                }
                SizeDeterminer sizeDeterminer = this.sizeDeterminerRef.get();
                if (sizeDeterminer != null) {
                    sizeDeterminer.checkCurrentDimens();
                    return true;
                }
                return true;
            }
        }
    }
}
