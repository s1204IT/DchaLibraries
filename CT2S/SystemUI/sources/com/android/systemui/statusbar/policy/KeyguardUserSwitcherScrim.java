package com.android.systemui.statusbar.policy;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.android.systemui.R;

public class KeyguardUserSwitcherScrim extends Drawable implements View.OnLayoutChangeListener {
    private int mDarkColor;
    private int mLayoutWidth;
    private int mTop;
    private int mAlpha = 255;
    private Paint mRadialGradientPaint = new Paint();

    public KeyguardUserSwitcherScrim(View host) {
        host.addOnLayoutChangeListener(this);
        this.mDarkColor = host.getResources().getColor(R.color.keyguard_user_switcher_background_gradient_color);
    }

    @Override
    public void draw(Canvas canvas) {
        boolean isLtr = getLayoutDirection() == 0;
        Rect bounds = getBounds();
        float width = bounds.width() * 2.5f;
        float height = (this.mTop + bounds.height()) * 2.5f;
        canvas.translate(0.0f, -this.mTop);
        canvas.scale(1.0f, height / width);
        canvas.drawRect(isLtr ? bounds.right - width : 0.0f, 0.0f, isLtr ? bounds.right : bounds.left + width, width, this.mRadialGradientPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        this.mAlpha = alpha;
        updatePaint();
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return this.mAlpha;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            this.mLayoutWidth = right - left;
            this.mTop = top;
            updatePaint();
        }
    }

    private void updatePaint() {
        if (this.mLayoutWidth != 0) {
            float radius = this.mLayoutWidth * 2.5f;
            boolean isLtr = getLayoutDirection() == 0;
            this.mRadialGradientPaint.setShader(new RadialGradient(isLtr ? this.mLayoutWidth : 0.0f, 0.0f, radius, new int[]{Color.argb((int) ((Color.alpha(this.mDarkColor) * this.mAlpha) / 255.0f), 0, 0, 0), 0}, new float[]{Math.max(0.0f, (this.mLayoutWidth * 0.75f) / radius), 1.0f}, Shader.TileMode.CLAMP));
        }
    }
}
