package android.support.design.widget;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import com.android.contacts.ContactPhotoManager;

class CircularBorderDrawable extends Drawable {
    private ColorStateList borderTint;
    float borderWidth;
    private int bottomInnerStrokeColor;
    private int bottomOuterStrokeColor;
    private int currentBorderTintColor;
    private boolean invalidateShader;
    final Paint paint;
    final Rect rect;
    final RectF rectF;
    private float rotation;
    private int topInnerStrokeColor;
    private int topOuterStrokeColor;

    @Override
    public void draw(Canvas canvas) {
        if (this.invalidateShader) {
            this.paint.setShader(createGradientShader());
            this.invalidateShader = false;
        }
        float halfBorderWidth = this.paint.getStrokeWidth() / 2.0f;
        RectF rectF = this.rectF;
        copyBounds(this.rect);
        rectF.set(this.rect);
        rectF.left += halfBorderWidth;
        rectF.top += halfBorderWidth;
        rectF.right -= halfBorderWidth;
        rectF.bottom -= halfBorderWidth;
        canvas.save();
        canvas.rotate(this.rotation, rectF.centerX(), rectF.centerY());
        canvas.drawOval(rectF, this.paint);
        canvas.restore();
    }

    @Override
    public boolean getPadding(Rect padding) {
        int borderWidth = Math.round(this.borderWidth);
        padding.set(borderWidth, borderWidth, borderWidth, borderWidth);
        return true;
    }

    @Override
    public void setAlpha(int alpha) {
        this.paint.setAlpha(alpha);
        invalidateSelf();
    }

    void setBorderTint(ColorStateList tint) {
        if (tint != null) {
            this.currentBorderTintColor = tint.getColorForState(getState(), this.currentBorderTintColor);
        }
        this.borderTint = tint;
        this.invalidateShader = true;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return this.borderWidth > ContactPhotoManager.OFFSET_DEFAULT ? -3 : -2;
    }

    final void setRotation(float rotation) {
        if (rotation != this.rotation) {
            this.rotation = rotation;
            invalidateSelf();
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        this.invalidateShader = true;
    }

    @Override
    public boolean isStateful() {
        return (this.borderTint != null && this.borderTint.isStateful()) || super.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        int newColor;
        if (this.borderTint != null && (newColor = this.borderTint.getColorForState(state, this.currentBorderTintColor)) != this.currentBorderTintColor) {
            this.invalidateShader = true;
            this.currentBorderTintColor = newColor;
        }
        if (this.invalidateShader) {
            invalidateSelf();
        }
        return this.invalidateShader;
    }

    private Shader createGradientShader() {
        Rect rect = this.rect;
        copyBounds(rect);
        float borderRatio = this.borderWidth / rect.height();
        int[] colors = {ColorUtils.compositeColors(this.topOuterStrokeColor, this.currentBorderTintColor), ColorUtils.compositeColors(this.topInnerStrokeColor, this.currentBorderTintColor), ColorUtils.compositeColors(ColorUtils.setAlphaComponent(this.topInnerStrokeColor, 0), this.currentBorderTintColor), ColorUtils.compositeColors(ColorUtils.setAlphaComponent(this.bottomInnerStrokeColor, 0), this.currentBorderTintColor), ColorUtils.compositeColors(this.bottomInnerStrokeColor, this.currentBorderTintColor), ColorUtils.compositeColors(this.bottomOuterStrokeColor, this.currentBorderTintColor)};
        float[] positions = {ContactPhotoManager.OFFSET_DEFAULT, borderRatio, 0.5f, 0.5f, 1.0f - borderRatio, 1.0f};
        return new LinearGradient(ContactPhotoManager.OFFSET_DEFAULT, rect.top, ContactPhotoManager.OFFSET_DEFAULT, rect.bottom, colors, positions, Shader.TileMode.CLAMP);
    }
}
