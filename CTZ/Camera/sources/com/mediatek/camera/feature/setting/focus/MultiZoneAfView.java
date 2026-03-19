package com.mediatek.camera.feature.setting.focus;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.portability.SystemProperties;

public class MultiZoneAfView extends View {
    private Drawable mAfIndicator;
    private Drawable[] mAfStatusIndicators;
    private float mAnimatorRatio;
    private int mDisplayOrientation;
    private Matrix mMatrix;
    private boolean mMirror;
    private int mOrientation;
    private int mPreviewHeight;
    private int mPreviewWidth;
    private RectF mRect;
    private float mScaleRatio;
    private ValueAnimator mValueAnimator;
    private MultiWindow[] mWindows;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MultiZoneAfView.class.getSimpleName());
    private static final int[] MZAF_ICON = {R.drawable.ic_multi_zone_focus_focusing, R.drawable.ic_multi_zone_focus_focused};

    public MultiZoneAfView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mScaleRatio = 0.0f;
        this.mAnimatorRatio = 1.0f;
        this.mMatrix = new Matrix();
        this.mRect = new RectF();
        this.mValueAnimator = new ValueAnimator();
        this.mAfStatusIndicators = new Drawable[MZAF_ICON.length];
        getViewDrawable();
        this.mAfIndicator = this.mAfStatusIndicators[0];
        this.mScaleRatio = Float.parseFloat(SystemProperties.getString("vendor.multizone.af.window.ratio", "0.4"));
    }

    public void updateFocusWindows(MultiWindow[] multiWindowArr) {
        this.mWindows = multiWindowArr;
    }

    public void showWindows(boolean z) {
        this.mValueAnimator.cancel();
        if (z) {
            this.mValueAnimator = ValueAnimator.ofFloat(1.0f, 1.2f).setDuration(1000L);
        } else {
            this.mValueAnimator = ValueAnimator.ofFloat(this.mAnimatorRatio, 1.0f).setDuration(200L);
        }
        this.mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                MultiZoneAfView.this.mAnimatorRatio = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                if (MultiZoneAfView.this.mAnimatorRatio * MultiZoneAfView.this.mScaleRatio <= 1.0f) {
                    MultiZoneAfView.this.invalidate();
                }
            }
        });
        this.mValueAnimator.start();
    }

    public void clear() {
        this.mWindows = null;
        invalidate();
    }

    public void setPreviewSize(int i, int i2) {
        this.mPreviewWidth = i;
        this.mPreviewHeight = i2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mWindows != null && this.mWindows.length > 0) {
            LogHelper.d(TAG, "onDraw length " + this.mWindows.length + " ,mDisplayOrientation = " + this.mDisplayOrientation + " ,mOrientation= " + this.mOrientation + ",mMirror =" + this.mMirror);
            int i = this.mPreviewWidth;
            int i2 = this.mPreviewHeight;
            if ((i2 > i && (this.mDisplayOrientation == 0 || this.mDisplayOrientation == 180)) || (i2 < i && (this.mDisplayOrientation == 90 || this.mDisplayOrientation == 270))) {
                i2 = i;
                i = i2;
            }
            CameraUtil.prepareMatrix(this.mMatrix, this.mMirror, this.mDisplayOrientation, i, i2);
            Matrix matrix = new Matrix();
            float[] fArr = {(getWidth() - i) / 2.0f, (getHeight() - i2) / 2.0f};
            canvas.save();
            this.mMatrix.postRotate(this.mOrientation);
            canvas.rotate(-this.mOrientation);
            matrix.mapPoints(fArr);
            for (int i3 = 0; i3 < this.mWindows.length; i3++) {
                this.mRect.set(this.mWindows[i3].mBounds);
                dumpRect(this.mRect, "Original rect");
                this.mMatrix.mapRect(this.mRect);
                dumpRect(this.mRect, "Transformed rect");
                LogHelper.d(TAG, "window[ " + i3 + " ] result " + this.mWindows[i3].mResult);
                if (this.mWindows[i3].mResult > 0) {
                    this.mAfIndicator = this.mAfStatusIndicators[1];
                } else {
                    this.mAfIndicator = this.mAfStatusIndicators[0];
                }
                this.mRect.offset(fArr[0], fArr[1]);
                this.mAfIndicator.setBounds(scale());
                this.mAfIndicator.draw(canvas);
            }
            canvas.restore();
        }
        super.onDraw(canvas);
    }

    private Drawable[] getViewDrawable() {
        int length = this.mAfStatusIndicators.length;
        for (int i = 0; i < length; i++) {
            this.mAfStatusIndicators[i] = getResources().getDrawable(MZAF_ICON[i]);
        }
        return this.mAfStatusIndicators;
    }

    private Rect scale() {
        Rect rect = new Rect();
        float fCenterX = this.mRect.centerX();
        float fCenterY = this.mRect.centerY();
        float fMin = Math.min(this.mRect.width(), this.mRect.height());
        rect.set((int) (fCenterX - (((this.mAnimatorRatio * fMin) * this.mScaleRatio) / 2.0f)), (int) (fCenterY - (((this.mAnimatorRatio * fMin) * this.mScaleRatio) / 2.0f)), (int) (fCenterX + (((this.mAnimatorRatio * fMin) * this.mScaleRatio) / 2.0f)), (int) (fCenterY + (((fMin * this.mAnimatorRatio) * this.mScaleRatio) / 2.0f)));
        return rect;
    }

    public static final class MultiWindow {
        public Rect mBounds;
        public int mResult;

        public MultiWindow(Rect rect, int i) {
            this.mBounds = rect;
            this.mResult = i;
        }

        public String toString() {
            return String.format("{ bounds: %s, result: %s}", this.mBounds, Integer.valueOf(this.mResult));
        }
    }

    private void dumpRect(RectF rectF, String str) {
        LogHelper.d(TAG, str + "=(" + rectF.left + "," + rectF.top + "," + rectF.right + "," + rectF.bottom + ")");
    }
}
