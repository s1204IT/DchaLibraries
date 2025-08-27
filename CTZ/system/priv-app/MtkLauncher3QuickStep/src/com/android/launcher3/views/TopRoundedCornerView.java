package com.android.launcher3.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import com.android.launcher3.R;
import com.android.launcher3.util.Themes;

/* loaded from: classes.dex */
public class TopRoundedCornerView extends SpringRelativeLayout {
    private final Path mClipPath;
    private int mNavBarScrimHeight;
    private final Paint mNavBarScrimPaint;
    private float[] mRadii;
    private final RectF mRect;

    public TopRoundedCornerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mRect = new RectF();
        this.mClipPath = new Path();
        this.mNavBarScrimHeight = 0;
        float dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.bg_round_rect_radius);
        this.mRadii = new float[]{dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, 0.0f, 0.0f, 0.0f, 0.0f};
        this.mNavBarScrimPaint = new Paint();
        this.mNavBarScrimPaint.setColor(Themes.getAttrColor(context, R.attr.allAppsNavBarScrimColor));
    }

    public TopRoundedCornerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public void setNavBarScrimHeight(int i) {
        if (this.mNavBarScrimHeight != i) {
            this.mNavBarScrimHeight = i;
            invalidate();
        }
    }

    @Override // android.view.View
    public void draw(Canvas canvas) {
        canvas.save();
        canvas.clipPath(this.mClipPath);
        super.draw(canvas);
        canvas.restore();
        if (this.mNavBarScrimHeight > 0) {
            canvas.drawRect(0.0f, getHeight() - this.mNavBarScrimHeight, getWidth(), getHeight(), this.mNavBarScrimPaint);
        }
    }

    @Override // android.widget.RelativeLayout, android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        this.mRect.set(0.0f, 0.0f, getMeasuredWidth(), getMeasuredHeight());
        this.mClipPath.reset();
        this.mClipPath.addRoundRect(this.mRect, this.mRadii, Path.Direction.CW);
    }
}
