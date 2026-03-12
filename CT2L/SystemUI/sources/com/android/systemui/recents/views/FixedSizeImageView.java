package com.android.systemui.recents.views;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FixedSizeImageView extends ImageView {
    boolean mAllowInvalidate;
    boolean mAllowRelayout;

    public FixedSizeImageView(Context context) {
        this(context, null);
    }

    public FixedSizeImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FixedSizeImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FixedSizeImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mAllowRelayout = true;
        this.mAllowInvalidate = true;
    }

    @Override
    public void requestLayout() {
        if (this.mAllowRelayout) {
            super.requestLayout();
        }
    }

    @Override
    public void invalidate() {
        if (this.mAllowInvalidate) {
            super.invalidate();
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        boolean isNullBitmapDrawable = (drawable instanceof BitmapDrawable) && ((BitmapDrawable) drawable).getBitmap() == null;
        if (drawable == null || isNullBitmapDrawable) {
            this.mAllowRelayout = false;
            this.mAllowInvalidate = false;
        }
        super.setImageDrawable(drawable);
        this.mAllowRelayout = true;
        this.mAllowInvalidate = true;
    }
}
