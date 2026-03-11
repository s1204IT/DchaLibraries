package com.android.systemui.stackdivider;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import com.android.systemui.R;

public class MinimizedDockShadow extends View {
    private int mDockSide;
    private final Paint mShadowPaint;

    public MinimizedDockShadow(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mShadowPaint = new Paint();
        this.mDockSide = -1;
    }

    public void setDockSide(int dockSide) {
        if (dockSide == this.mDockSide) {
            return;
        }
        this.mDockSide = dockSide;
        updatePaint(getLeft(), getTop(), getRight(), getBottom());
        invalidate();
    }

    private void updatePaint(int left, int top, int right, int bottom) {
        int startColor = this.mContext.getResources().getColor(R.color.minimize_dock_shadow_start, null);
        int endColor = this.mContext.getResources().getColor(R.color.minimize_dock_shadow_end, null);
        int middleColor = Color.argb((Color.alpha(startColor) + Color.alpha(endColor)) / 2, 0, 0, 0);
        int quarter = Color.argb((int) ((Color.alpha(startColor) * 0.25f) + (Color.alpha(endColor) * 0.75f)), 0, 0, 0);
        if (this.mDockSide == 2) {
            this.mShadowPaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, bottom - top, new int[]{startColor, middleColor, quarter, endColor}, new float[]{0.0f, 0.35f, 0.6f, 1.0f}, Shader.TileMode.CLAMP));
        } else if (this.mDockSide == 1) {
            this.mShadowPaint.setShader(new LinearGradient(0.0f, 0.0f, right - left, 0.0f, new int[]{startColor, middleColor, quarter, endColor}, new float[]{0.0f, 0.35f, 0.6f, 1.0f}, Shader.TileMode.CLAMP));
        } else {
            if (this.mDockSide != 3) {
                return;
            }
            this.mShadowPaint.setShader(new LinearGradient(right - left, 0.0f, 0.0f, 0.0f, new int[]{startColor, middleColor, quarter, endColor}, new float[]{0.0f, 0.35f, 0.6f, 1.0f}, Shader.TileMode.CLAMP));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!changed) {
            return;
        }
        updatePaint(left, top, right, bottom);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), this.mShadowPaint);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
