package com.android.systemui.stackdivider;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import com.android.systemui.R;

/* loaded from: classes.dex */
public class MinimizedDockShadow extends View {
    private int mDockSide;
    private final Paint mShadowPaint;

    public MinimizedDockShadow(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mShadowPaint = new Paint();
        this.mDockSide = -1;
    }

    public void setDockSide(int i) throws Resources.NotFoundException {
        if (i != this.mDockSide) {
            this.mDockSide = i;
            updatePaint(getLeft(), getTop(), getRight(), getBottom());
            invalidate();
        }
    }

    private void updatePaint(int i, int i2, int i3, int i4) throws Resources.NotFoundException {
        int color = this.mContext.getResources().getColor(R.color.minimize_dock_shadow_start, null);
        int color2 = this.mContext.getResources().getColor(R.color.minimize_dock_shadow_end, null);
        int iArgb = Color.argb((Color.alpha(color) + Color.alpha(color2)) / 2, 0, 0, 0);
        int iArgb2 = Color.argb((int) ((Color.alpha(color) * 0.25f) + (Color.alpha(color2) * 0.75f)), 0, 0, 0);
        if (this.mDockSide == 2) {
            this.mShadowPaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, i4 - i2, new int[]{color, iArgb, iArgb2, color2}, new float[]{0.0f, 0.35f, 0.6f, 1.0f}, Shader.TileMode.CLAMP));
        } else if (this.mDockSide == 1) {
            this.mShadowPaint.setShader(new LinearGradient(0.0f, 0.0f, i3 - i, 0.0f, new int[]{color, iArgb, iArgb2, color2}, new float[]{0.0f, 0.35f, 0.6f, 1.0f}, Shader.TileMode.CLAMP));
        } else if (this.mDockSide == 3) {
            this.mShadowPaint.setShader(new LinearGradient(i3 - i, 0.0f, 0.0f, 0.0f, new int[]{color, iArgb, iArgb2, color2}, new float[]{0.0f, 0.35f, 0.6f, 1.0f}, Shader.TileMode.CLAMP));
        }
    }

    @Override // android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) throws Resources.NotFoundException {
        super.onLayout(z, i, i2, i3, i4);
        if (z) {
            updatePaint(i, i2, i3, i4);
            invalidate();
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), this.mShadowPaint);
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }
}
