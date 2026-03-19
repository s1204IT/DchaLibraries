package android.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Picture;
import android.graphics.Rect;

public class PictureDrawable extends Drawable {
    private Picture mPicture;

    public PictureDrawable(Picture picture) {
        this.mPicture = picture;
    }

    public Picture getPicture() {
        return this.mPicture;
    }

    public void setPicture(Picture picture) {
        this.mPicture = picture;
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.mPicture == null) {
            return;
        }
        Rect bounds = getBounds();
        canvas.save();
        canvas.clipRect(bounds);
        canvas.translate(bounds.left, bounds.top);
        canvas.drawPicture(this.mPicture);
        canvas.restore();
    }

    @Override
    public int getIntrinsicWidth() {
        if (this.mPicture != null) {
            return this.mPicture.getWidth();
        }
        return -1;
    }

    @Override
    public int getIntrinsicHeight() {
        if (this.mPicture != null) {
            return this.mPicture.getHeight();
        }
        return -1;
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public void setAlpha(int alpha) {
    }
}
