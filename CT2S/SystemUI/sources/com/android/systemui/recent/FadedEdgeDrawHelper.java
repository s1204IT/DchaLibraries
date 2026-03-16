package com.android.systemui.recent;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;
import com.android.systemui.R;
import com.android.systemui.recent.RecentsPanelView;

public class FadedEdgeDrawHelper {
    private Paint mBlackPaint;
    private LinearGradient mFade;
    private Matrix mFadeMatrix;
    private Paint mFadePaint;
    private int mFadingEdgeLength;
    private boolean mIsVertical;
    private View mScrollView;
    private boolean mSoftwareRendered = false;

    public static FadedEdgeDrawHelper create(Context context, AttributeSet attrs, View scrollView, boolean isVertical) {
        boolean isTablet = context.getResources().getBoolean(R.bool.config_recents_interface_for_tablets);
        if (isTablet) {
            return null;
        }
        return new FadedEdgeDrawHelper(context, attrs, scrollView, isVertical);
    }

    public FadedEdgeDrawHelper(Context context, AttributeSet attrs, View scrollView, boolean isVertical) {
        this.mScrollView = scrollView;
        TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.View);
        this.mFadingEdgeLength = a.getDimensionPixelSize(25, ViewConfiguration.get(context).getScaledFadingEdgeLength());
        this.mIsVertical = isVertical;
    }

    public void onAttachedToWindowCallback(LinearLayout layout, boolean hardwareAccelerated) {
        this.mSoftwareRendered = !hardwareAccelerated;
        if (this.mSoftwareRendered) {
        }
        this.mScrollView.setVerticalFadingEdgeEnabled(false);
        this.mScrollView.setHorizontalFadingEdgeEnabled(false);
    }

    public void addViewCallback(View newLinearLayoutChild) {
        if (this.mSoftwareRendered) {
            RecentsPanelView.ViewHolder holder = (RecentsPanelView.ViewHolder) newLinearLayoutChild.getTag();
            holder.labelView.setDrawingCacheEnabled(true);
            holder.labelView.buildDrawingCache();
        }
    }

    public void drawCallback(Canvas canvas, int left, int right, int top, int bottom, int scrollX, int scrollY, float topFadingEdgeStrength, float bottomFadingEdgeStrength, float leftFadingEdgeStrength, float rightFadingEdgeStrength, int mPaddingTop) {
        if (this.mSoftwareRendered) {
        }
        if (this.mFadePaint == null) {
            this.mFadePaint = new Paint();
            this.mFadeMatrix = new Matrix();
            this.mFade = new LinearGradient(0.0f, 0.0f, 0.0f, 1.0f, -872415232, 0, Shader.TileMode.CLAMP);
            this.mFadePaint.setShader(this.mFade);
        }
        boolean drawTop = false;
        boolean drawBottom = false;
        boolean drawLeft = false;
        boolean drawRight = false;
        float topFadeStrength = 0.0f;
        float bottomFadeStrength = 0.0f;
        float leftFadeStrength = 0.0f;
        float rightFadeStrength = 0.0f;
        float fadeHeight = this.mFadingEdgeLength;
        int length = (int) fadeHeight;
        if (this.mIsVertical && top + length > bottom - length) {
            length = (bottom - top) / 2;
        }
        if (!this.mIsVertical && left + length > right - length) {
            length = (right - left) / 2;
        }
        if (this.mIsVertical) {
            topFadeStrength = Math.max(0.0f, Math.min(1.0f, topFadingEdgeStrength));
            drawTop = topFadeStrength * fadeHeight > 1.0f;
            bottomFadeStrength = Math.max(0.0f, Math.min(1.0f, bottomFadingEdgeStrength));
            drawBottom = bottomFadeStrength * fadeHeight > 1.0f;
        }
        if (!this.mIsVertical) {
            leftFadeStrength = Math.max(0.0f, Math.min(1.0f, leftFadingEdgeStrength));
            drawLeft = leftFadeStrength * fadeHeight > 1.0f;
            rightFadeStrength = Math.max(0.0f, Math.min(1.0f, rightFadingEdgeStrength));
            drawRight = rightFadeStrength * fadeHeight > 1.0f;
        }
        if (drawTop) {
            this.mFadeMatrix.setScale(1.0f, fadeHeight * topFadeStrength);
            this.mFadeMatrix.postTranslate(left, top);
            this.mFade.setLocalMatrix(this.mFadeMatrix);
            this.mFadePaint.setShader(this.mFade);
            canvas.drawRect(left, top, right, top + length, this.mFadePaint);
            if (this.mBlackPaint == null) {
                this.mBlackPaint = new Paint();
                this.mBlackPaint.setColor(-16777216);
            }
            canvas.drawRect(left, top - mPaddingTop, right, top, this.mBlackPaint);
        }
        if (drawBottom) {
            this.mFadeMatrix.setScale(1.0f, fadeHeight * bottomFadeStrength);
            this.mFadeMatrix.postRotate(180.0f);
            this.mFadeMatrix.postTranslate(left, bottom);
            this.mFade.setLocalMatrix(this.mFadeMatrix);
            this.mFadePaint.setShader(this.mFade);
            canvas.drawRect(left, bottom - length, right, bottom, this.mFadePaint);
        }
        if (drawLeft) {
            this.mFadeMatrix.setScale(1.0f, fadeHeight * leftFadeStrength);
            this.mFadeMatrix.postRotate(-90.0f);
            this.mFadeMatrix.postTranslate(left, top);
            this.mFade.setLocalMatrix(this.mFadeMatrix);
            this.mFadePaint.setShader(this.mFade);
            canvas.drawRect(left, top, left + length, bottom, this.mFadePaint);
        }
        if (drawRight) {
            this.mFadeMatrix.setScale(1.0f, fadeHeight * rightFadeStrength);
            this.mFadeMatrix.postRotate(90.0f);
            this.mFadeMatrix.postTranslate(right, top);
            this.mFade.setLocalMatrix(this.mFadeMatrix);
            this.mFadePaint.setShader(this.mFade);
            canvas.drawRect(right - length, top, right, bottom, this.mFadePaint);
        }
    }

    public int getVerticalFadingEdgeLength() {
        return this.mFadingEdgeLength;
    }

    public int getHorizontalFadingEdgeLength() {
        return this.mFadingEdgeLength;
    }
}
