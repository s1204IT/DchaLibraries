package com.android.contacts.detail;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TransformableImageView extends ImageView {
    public TransformableImageView(Context context) {
        super(context);
    }

    public TransformableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TransformableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float scale;
        if (getDrawable() != null) {
            int saveCount = canvas.getSaveCount();
            canvas.save();
            canvas.translate(this.mPaddingLeft, this.mPaddingTop);
            Matrix drawMatrix = new Matrix();
            int dwidth = getDrawable().getIntrinsicWidth();
            int dheight = getDrawable().getIntrinsicHeight();
            int vwidth = (getWidth() - this.mPaddingLeft) - this.mPaddingRight;
            int vheight = (getHeight() - this.mPaddingTop) - this.mPaddingBottom;
            float dx = 0.0f;
            float dy = 0.0f;
            if (dwidth * vheight > vwidth * dheight) {
                scale = vheight / dheight;
                dx = (vwidth - (dwidth * scale)) * 0.5f;
            } else {
                scale = vwidth / dwidth;
                dy = (vheight - (dheight * scale)) * 0.5f;
            }
            drawMatrix.setScale(scale, scale);
            drawMatrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
            canvas.concat(drawMatrix);
            getDrawable().draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }
}
