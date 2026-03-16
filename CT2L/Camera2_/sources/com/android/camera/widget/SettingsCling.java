package com.android.camera.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.android.camera2.R;

public class SettingsCling extends FrameLayout {
    private final Paint mClingPaint;
    private final int mClingTriangleHeight;
    private final int mClingTriangleWidth;
    private final Path mTrianglePath;

    public SettingsCling(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTrianglePath = new Path();
        this.mClingPaint = new Paint();
        setWillNotDraw(false);
        this.mClingTriangleHeight = getResources().getDimensionPixelSize(R.dimen.settings_cling_triangle_height);
        this.mClingTriangleWidth = getResources().getDimensionPixelSize(R.dimen.settings_cling_triangle_width);
        this.mClingPaint.setColor(getResources().getColor(R.color.settings_cling_color));
        this.mClingPaint.setStyle(Paint.Style.FILL);
    }

    public void updatePosition(View referenceView) {
        if (referenceView != null) {
            float referenceRight = referenceView.getX() + referenceView.getMeasuredWidth();
            setTranslationX(referenceRight - getMeasuredWidth());
            float referenceTop = referenceView.getY();
            if (referenceTop < getMeasuredHeight()) {
                setTranslationY(referenceView.getMeasuredHeight() + referenceTop);
                float triangleStartX = getMeasuredWidth() - (referenceView.getMeasuredWidth() / 2);
                this.mTrianglePath.reset();
                this.mTrianglePath.moveTo(triangleStartX, 0.0f);
                this.mTrianglePath.lineTo(triangleStartX - (this.mClingTriangleWidth / 2), this.mClingTriangleHeight + 0.0f);
                this.mTrianglePath.lineTo((this.mClingTriangleWidth / 2) + triangleStartX, this.mClingTriangleHeight + 0.0f);
                this.mTrianglePath.lineTo(triangleStartX, 0.0f);
            } else {
                setTranslationY(referenceTop - getMeasuredHeight());
                float triangleStartX2 = getMeasuredWidth() - (referenceView.getMeasuredWidth() / 2);
                float triangleStartY = getMeasuredHeight();
                this.mTrianglePath.reset();
                this.mTrianglePath.moveTo(triangleStartX2, triangleStartY);
                this.mTrianglePath.lineTo(triangleStartX2 - (this.mClingTriangleWidth / 2), triangleStartY - this.mClingTriangleHeight);
                this.mTrianglePath.lineTo((this.mClingTriangleWidth / 2) + triangleStartX2, triangleStartY - this.mClingTriangleHeight);
                this.mTrianglePath.lineTo(triangleStartX2, triangleStartY);
            }
            invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(this.mTrianglePath, this.mClingPaint);
    }
}
