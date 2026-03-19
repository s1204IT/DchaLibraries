package com.android.launcher3.graphics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.util.Property;
import android.util.SparseArray;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.ItemInfoWithIcon;
import com.android.launcher3.anim.Interpolators;
import java.lang.ref.WeakReference;

public class PreloadIconDrawable extends FastBitmapDrawable {
    private static final int COLOR_SHADOW = 1426063360;
    private static final int COLOR_TRACK = 2012147438;
    private static final float COMPLETE_ANIM_FRACTION = 0.3f;
    private static final long DURATION_SCALE = 500;
    private static final int MAX_PAINT_ALPHA = 255;
    public static final int PATH_SIZE = 100;
    private static final float PROGRESS_GAP = 2.0f;
    private static final float PROGRESS_WIDTH = 7.0f;
    private static final float SMALL_SCALE = 0.6f;
    private final Context mContext;
    private ObjectAnimator mCurrentAnim;
    private float mIconScale;
    private final int mIndicatorColor;
    private float mInternalStateProgress;
    private final PathMeasure mPathMeasure;
    private final Paint mProgressPaint;
    private final Path mProgressPath;
    private boolean mRanFinishAnimation;
    private final Path mScaledProgressPath;
    private final Path mScaledTrackPath;
    private Bitmap mShadowBitmap;
    private final Matrix mTmpMatrix;
    private int mTrackAlpha;
    private float mTrackLength;
    private static final Property<PreloadIconDrawable, Float> INTERNAL_STATE = new Property<PreloadIconDrawable, Float>(Float.TYPE, "internalStateProgress") {
        @Override
        public Float get(PreloadIconDrawable preloadIconDrawable) {
            return Float.valueOf(preloadIconDrawable.mInternalStateProgress);
        }

        @Override
        public void set(PreloadIconDrawable preloadIconDrawable, Float f) {
            preloadIconDrawable.setInternalProgress(f.floatValue());
        }
    };
    private static final SparseArray<WeakReference<Bitmap>> sShadowCache = new SparseArray<>();

    public PreloadIconDrawable(ItemInfoWithIcon itemInfoWithIcon, Path path, Context context) {
        super(itemInfoWithIcon);
        this.mTmpMatrix = new Matrix();
        this.mPathMeasure = new PathMeasure();
        this.mContext = context;
        this.mProgressPath = path;
        this.mScaledTrackPath = new Path();
        this.mScaledProgressPath = new Path();
        this.mProgressPaint = new Paint(3);
        this.mProgressPaint.setStyle(Paint.Style.STROKE);
        this.mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mIndicatorColor = IconPalette.getPreloadProgressColor(context, this.mIconColor);
        setInternalProgress(0.0f);
    }

    @Override
    protected void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
        this.mTmpMatrix.setScale(((rect.width() - 14.0f) - 4.0f) / 100.0f, ((rect.height() - 14.0f) - 4.0f) / 100.0f);
        this.mTmpMatrix.postTranslate(rect.left + PROGRESS_WIDTH + PROGRESS_GAP, rect.top + PROGRESS_WIDTH + PROGRESS_GAP);
        this.mProgressPath.transform(this.mTmpMatrix, this.mScaledTrackPath);
        float fWidth = rect.width() / 100;
        this.mProgressPaint.setStrokeWidth(PROGRESS_WIDTH * fWidth);
        this.mShadowBitmap = getShadowBitmap(rect.width(), rect.height(), PROGRESS_GAP * fWidth);
        this.mPathMeasure.setPath(this.mScaledTrackPath, true);
        this.mTrackLength = this.mPathMeasure.getLength();
        setInternalProgress(this.mInternalStateProgress);
    }

    private Bitmap getShadowBitmap(int i, int i2, float f) {
        int i3 = (i << 16) | i2;
        WeakReference<Bitmap> weakReference = sShadowCache.get(i3);
        Bitmap bitmap = weakReference != null ? weakReference.get() : null;
        if (bitmap != null) {
            return bitmap;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        this.mProgressPaint.setShadowLayer(f, 0.0f, 0.0f, COLOR_SHADOW);
        this.mProgressPaint.setColor(COLOR_TRACK);
        this.mProgressPaint.setAlpha(255);
        canvas.drawPath(this.mScaledTrackPath, this.mProgressPaint);
        this.mProgressPaint.clearShadowLayer();
        canvas.setBitmap(null);
        sShadowCache.put(i3, new WeakReference<>(bitmapCreateBitmap));
        return bitmapCreateBitmap;
    }

    @Override
    public void drawInternal(Canvas canvas, Rect rect) {
        if (this.mRanFinishAnimation) {
            super.drawInternal(canvas, rect);
            return;
        }
        this.mProgressPaint.setColor(this.mIndicatorColor);
        this.mProgressPaint.setAlpha(this.mTrackAlpha);
        if (this.mShadowBitmap != null) {
            canvas.drawBitmap(this.mShadowBitmap, rect.left, rect.top, this.mProgressPaint);
        }
        canvas.drawPath(this.mScaledProgressPath, this.mProgressPaint);
        int iSave = canvas.save();
        canvas.scale(this.mIconScale, this.mIconScale, rect.exactCenterX(), rect.exactCenterY());
        super.drawInternal(canvas, rect);
        canvas.restoreToCount(iSave);
    }

    @Override
    protected boolean onLevelChange(int i) {
        updateInternalState(i * 0.01f, getBounds().width() > 0, false);
        return true;
    }

    public void maybePerformFinishedAnimation() {
        if (this.mInternalStateProgress == 0.0f) {
            this.mInternalStateProgress = 1.0f;
        }
        updateInternalState(1.3f, true, true);
    }

    public boolean hasNotCompleted() {
        return !this.mRanFinishAnimation;
    }

    private void updateInternalState(float f, boolean z, boolean z2) {
        if (this.mCurrentAnim != null) {
            this.mCurrentAnim.cancel();
            this.mCurrentAnim = null;
        }
        if (Float.compare(f, this.mInternalStateProgress) == 0) {
            return;
        }
        if (f < this.mInternalStateProgress) {
            z = false;
        }
        if (!z || this.mRanFinishAnimation) {
            setInternalProgress(f);
            return;
        }
        this.mCurrentAnim = ObjectAnimator.ofFloat(this, INTERNAL_STATE, f);
        this.mCurrentAnim.setDuration((long) ((f - this.mInternalStateProgress) * 500.0f));
        this.mCurrentAnim.setInterpolator(Interpolators.LINEAR);
        if (z2) {
            this.mCurrentAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    PreloadIconDrawable.this.mRanFinishAnimation = true;
                }
            });
        }
        this.mCurrentAnim.start();
    }

    private void setInternalProgress(float f) {
        this.mInternalStateProgress = f;
        if (f <= 0.0f) {
            this.mIconScale = SMALL_SCALE;
            this.mScaledTrackPath.reset();
            this.mTrackAlpha = 255;
            setIsDisabled(true);
        }
        if (f < 1.0f && f > 0.0f) {
            this.mPathMeasure.getSegment(0.0f, f * this.mTrackLength, this.mScaledProgressPath, true);
            this.mIconScale = SMALL_SCALE;
            this.mTrackAlpha = 255;
            setIsDisabled(true);
        } else if (f >= 1.0f) {
            setIsDisabled(false);
            this.mScaledTrackPath.set(this.mScaledProgressPath);
            float f2 = (f - 1.0f) / COMPLETE_ANIM_FRACTION;
            if (f2 >= 1.0f) {
                this.mIconScale = 1.0f;
                this.mTrackAlpha = 0;
            } else {
                this.mTrackAlpha = Math.round((1.0f - f2) * 255.0f);
                this.mIconScale = SMALL_SCALE + (0.39999998f * f2);
            }
        }
        invalidateSelf();
    }
}
