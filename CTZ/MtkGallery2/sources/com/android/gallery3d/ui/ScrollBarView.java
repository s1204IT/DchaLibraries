package com.android.gallery3d.ui;

import android.R;
import android.content.Context;
import android.util.TypedValue;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.NinePatchTexture;

public class ScrollBarView extends GLView {
    private static final String TAG = "Gallery2/ScrollBarView";
    private int mBarHeight;
    private int mContentPosition;
    private int mContentTotal;
    private int mGivenGripWidth;
    private int mGripHeight;
    private int mGripPosition;
    private int mGripWidth;
    private NinePatchTexture mScrollBarTexture;

    public ScrollBarView(Context context, int i, int i2) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.scrollbarThumbHorizontal, typedValue, true);
        this.mScrollBarTexture = new NinePatchTexture(context, typedValue.resourceId);
        this.mGripPosition = 0;
        this.mGripWidth = 0;
        this.mGivenGripWidth = i2;
        this.mGripHeight = i;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (z) {
            this.mBarHeight = i4 - i2;
        }
    }

    public void setContentPosition(int i, int i2) {
        if (i == this.mContentPosition && i2 == this.mContentTotal) {
            return;
        }
        invalidate();
        this.mContentPosition = i;
        this.mContentTotal = i2;
        if (this.mContentTotal <= 0) {
            this.mGripPosition = 0;
            this.mGripWidth = 0;
        } else {
            this.mGripWidth = this.mGivenGripWidth;
            this.mGripPosition = Math.round(((getWidth() - this.mGripWidth) / this.mContentTotal) * this.mContentPosition);
        }
    }

    @Override
    protected void render(GLCanvas gLCanvas) {
        super.render(gLCanvas);
        if (this.mGripWidth == 0) {
            return;
        }
        bounds();
        this.mScrollBarTexture.draw(gLCanvas, this.mGripPosition, (this.mBarHeight - this.mGripHeight) / 2, this.mGripWidth, this.mGripHeight);
    }
}
