package com.android.systemui.recents.views;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import com.android.systemui.statusbar.AlphaOptimizedImageView;

/* loaded from: classes.dex */
public class FixedSizeImageView extends AlphaOptimizedImageView {
    private boolean mAllowInvalidate;
    private boolean mAllowRelayout;

    public FixedSizeImageView(Context context) {
        this(context, null);
    }

    public FixedSizeImageView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public FixedSizeImageView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public FixedSizeImageView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mAllowRelayout = true;
        this.mAllowInvalidate = true;
    }

    @Override // android.view.View
    public void requestLayout() {
        if (this.mAllowRelayout) {
            super.requestLayout();
        }
    }

    @Override // android.view.View
    public void invalidate() {
        if (this.mAllowInvalidate) {
            super.invalidate();
        }
    }

    @Override // android.widget.ImageView
    public void setImageDrawable(Drawable drawable) {
        boolean z = (drawable instanceof BitmapDrawable) && ((BitmapDrawable) drawable).getBitmap() == null;
        if (drawable == null || z) {
            this.mAllowRelayout = false;
            this.mAllowInvalidate = false;
        }
        super.setImageDrawable(drawable);
        this.mAllowRelayout = true;
        this.mAllowInvalidate = true;
    }
}
