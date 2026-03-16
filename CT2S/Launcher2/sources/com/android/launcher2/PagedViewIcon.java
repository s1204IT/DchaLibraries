package com.android.launcher2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.TextView;

public class PagedViewIcon extends TextView {
    private Bitmap mIcon;
    private boolean mLockDrawableState;
    private PressedCallback mPressedCallback;

    public interface PressedCallback {
        void iconPressed(PagedViewIcon pagedViewIcon);
    }

    public PagedViewIcon(Context context) {
        this(context, null);
    }

    public PagedViewIcon(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedViewIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mLockDrawableState = false;
    }

    public void applyFromApplicationInfo(ApplicationInfo info, boolean scaleUp, PressedCallback cb) {
        this.mIcon = info.iconBitmap;
        this.mPressedCallback = cb;
        setCompoundDrawablesWithIntrinsicBounds((Drawable) null, new FastBitmapDrawable(this.mIcon), (Drawable) null, (Drawable) null);
        setText(info.title);
        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }
        setTag(info);
    }

    public void lockDrawableState() {
        this.mLockDrawableState = true;
    }

    public void resetDrawableState() {
        this.mLockDrawableState = false;
        post(new Runnable() {
            @Override
            public void run() {
                PagedViewIcon.this.refreshDrawableState();
            }
        });
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (isPressed()) {
            setAlpha(0.4f);
            if (this.mPressedCallback != null) {
                this.mPressedCallback.iconPressed(this);
                return;
            }
            return;
        }
        if (!this.mLockDrawableState) {
            setAlpha(1.0f);
        }
    }
}
