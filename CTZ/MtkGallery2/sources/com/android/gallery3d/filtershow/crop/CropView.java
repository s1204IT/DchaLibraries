package com.android.gallery3d.filtershow.crop;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.R;
import com.mediatek.gallery3d.util.Log;
import mf.org.apache.xerces.dom3.as.ASContentModel;

public class CropView extends View {
    private Bitmap mBitmap;
    private Drawable mCropIndicator;
    private CropObject mCropObj;
    private float mDashOffLength;
    private float mDashOnLength;
    private boolean mDirty;
    private Matrix mDisplayMatrix;
    private Matrix mDisplayMatrixInverse;
    private boolean mDoSpot;
    private boolean mEnableTouchMotion;
    private RectF mImageBounds;
    private int mIndicatorSize;
    private int mMargin;
    private int mMinSideSize;
    private boolean mMovingBlock;
    private int mOverlayShadowColor;
    private int mOverlayWPShadowColor;
    private Paint mPaint;
    private float mPrevX;
    private float mPrevY;
    private int mRotation;
    private RectF mScreenBounds;
    private RectF mScreenCropBounds;
    private RectF mScreenImageBounds;
    private NinePatchDrawable mShadow;
    private Rect mShadowBounds;
    private int mShadowMargin;
    private float mSpotX;
    private float mSpotY;
    private Mode mState;
    private int mTouchTolerance;
    private int mWPMarkerColor;

    private enum Mode {
        NONE,
        MOVE,
        MODIFY_TOUCH_POINTER
    }

    public CropView(Context context) {
        super(context);
        this.mImageBounds = new RectF();
        this.mScreenBounds = new RectF();
        this.mScreenImageBounds = new RectF();
        this.mScreenCropBounds = new RectF();
        this.mShadowBounds = new Rect();
        this.mPaint = new Paint();
        this.mCropObj = null;
        this.mRotation = 0;
        this.mMovingBlock = false;
        this.mDisplayMatrix = null;
        this.mDisplayMatrixInverse = null;
        this.mDirty = false;
        this.mPrevX = 0.0f;
        this.mPrevY = 0.0f;
        this.mSpotX = 0.0f;
        this.mSpotY = 0.0f;
        this.mDoSpot = false;
        this.mShadowMargin = 15;
        this.mMargin = 32;
        this.mOverlayShadowColor = -822083584;
        this.mOverlayWPShadowColor = 1593835520;
        this.mWPMarkerColor = ASContentModel.AS_UNBOUNDED;
        this.mMinSideSize = 90;
        this.mTouchTolerance = 40;
        this.mDashOnLength = 20.0f;
        this.mDashOffLength = 10.0f;
        this.mState = Mode.NONE;
        this.mEnableTouchMotion = true;
        setup(context);
    }

    public CropView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mImageBounds = new RectF();
        this.mScreenBounds = new RectF();
        this.mScreenImageBounds = new RectF();
        this.mScreenCropBounds = new RectF();
        this.mShadowBounds = new Rect();
        this.mPaint = new Paint();
        this.mCropObj = null;
        this.mRotation = 0;
        this.mMovingBlock = false;
        this.mDisplayMatrix = null;
        this.mDisplayMatrixInverse = null;
        this.mDirty = false;
        this.mPrevX = 0.0f;
        this.mPrevY = 0.0f;
        this.mSpotX = 0.0f;
        this.mSpotY = 0.0f;
        this.mDoSpot = false;
        this.mShadowMargin = 15;
        this.mMargin = 32;
        this.mOverlayShadowColor = -822083584;
        this.mOverlayWPShadowColor = 1593835520;
        this.mWPMarkerColor = ASContentModel.AS_UNBOUNDED;
        this.mMinSideSize = 90;
        this.mTouchTolerance = 40;
        this.mDashOnLength = 20.0f;
        this.mDashOffLength = 10.0f;
        this.mState = Mode.NONE;
        this.mEnableTouchMotion = true;
        setup(context);
    }

    public CropView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mImageBounds = new RectF();
        this.mScreenBounds = new RectF();
        this.mScreenImageBounds = new RectF();
        this.mScreenCropBounds = new RectF();
        this.mShadowBounds = new Rect();
        this.mPaint = new Paint();
        this.mCropObj = null;
        this.mRotation = 0;
        this.mMovingBlock = false;
        this.mDisplayMatrix = null;
        this.mDisplayMatrixInverse = null;
        this.mDirty = false;
        this.mPrevX = 0.0f;
        this.mPrevY = 0.0f;
        this.mSpotX = 0.0f;
        this.mSpotY = 0.0f;
        this.mDoSpot = false;
        this.mShadowMargin = 15;
        this.mMargin = 32;
        this.mOverlayShadowColor = -822083584;
        this.mOverlayWPShadowColor = 1593835520;
        this.mWPMarkerColor = ASContentModel.AS_UNBOUNDED;
        this.mMinSideSize = 90;
        this.mTouchTolerance = 40;
        this.mDashOnLength = 20.0f;
        this.mDashOffLength = 10.0f;
        this.mState = Mode.NONE;
        this.mEnableTouchMotion = true;
        setup(context);
    }

    private void setup(Context context) {
        Resources resources = context.getResources();
        this.mShadow = (NinePatchDrawable) resources.getDrawable(R.drawable.geometry_shadow);
        this.mCropIndicator = resources.getDrawable(R.drawable.camera_crop);
        this.mIndicatorSize = (int) resources.getDimension(R.dimen.crop_indicator_size);
        this.mShadowMargin = (int) resources.getDimension(R.dimen.shadow_margin);
        this.mMargin = (int) resources.getDimension(R.dimen.preview_margin);
        this.mMinSideSize = (int) resources.getDimension(R.dimen.crop_min_side);
        this.mTouchTolerance = (int) resources.getDimension(R.dimen.crop_touch_tolerance);
        this.mOverlayShadowColor = resources.getColor(R.color.crop_shadow_color);
        this.mOverlayWPShadowColor = resources.getColor(R.color.crop_shadow_wp_color);
        this.mWPMarkerColor = resources.getColor(R.color.crop_wp_markers);
        this.mDashOnLength = resources.getDimension(R.dimen.wp_selector_dash_length);
        this.mDashOffLength = resources.getDimension(R.dimen.wp_selector_off_length);
    }

    public void initialize(Bitmap bitmap, RectF rectF, RectF rectF2, int i) {
        this.mBitmap = bitmap;
        if (this.mCropObj != null) {
            RectF innerBounds = this.mCropObj.getInnerBounds();
            RectF outerBounds = this.mCropObj.getOuterBounds();
            if (innerBounds != rectF || outerBounds != rectF2 || this.mRotation != i) {
                this.mRotation = i;
                this.mCropObj.resetBoundsTo(rectF, rectF2);
                clearDisplay();
                return;
            }
            return;
        }
        this.mRotation = i;
        this.mCropObj = new CropObject(rectF2, rectF, 0);
        clearDisplay();
    }

    public RectF getCrop() {
        return this.mCropObj.getInnerBounds();
    }

    public RectF getPhoto() {
        return this.mCropObj.getOuterBounds();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!this.mEnableTouchMotion) {
            return true;
        }
        float[] point = getPoint(motionEvent);
        float f = point[0];
        float f2 = point[1];
        if (this.mDisplayMatrix == null || this.mDisplayMatrixInverse == null) {
            return true;
        }
        float[] fArr = {f, f2};
        this.mDisplayMatrixInverse.mapPoints(fArr);
        float f3 = fArr[0];
        float f4 = fArr[1];
        switch (motionEvent.getActionMasked()) {
            case 0:
                if (this.mState == Mode.NONE) {
                    if (!this.mCropObj.selectEdge(f3, f4)) {
                        this.mMovingBlock = this.mCropObj.selectEdge(16);
                    }
                    this.mPrevX = f3;
                    this.mPrevY = f4;
                    this.mState = Mode.MOVE;
                }
                break;
            case 1:
                if (this.mState == Mode.MOVE) {
                    this.mCropObj.selectEdge(0);
                    this.mMovingBlock = false;
                    this.mPrevX = f3;
                    this.mPrevY = f4;
                    this.mState = Mode.NONE;
                }
                break;
            case 2:
                if (this.mState == Mode.MOVE) {
                    this.mCropObj.moveCurrentSelection(f3 - this.mPrevX, f4 - this.mPrevY);
                    this.mPrevX = f3;
                    this.mPrevY = f4;
                } else if (this.mState == Mode.MODIFY_TOUCH_POINTER) {
                    this.mPrevX = f3;
                    this.mPrevY = f4;
                    this.mState = Mode.MOVE;
                }
                break;
            case 5:
            case 6:
                this.mState = Mode.MODIFY_TOUCH_POINTER;
                break;
        }
        invalidate();
        return true;
    }

    private void reset() {
        Log.w("CropView", "crop reset called");
        this.mState = Mode.NONE;
        this.mCropObj = null;
        this.mRotation = 0;
        this.mMovingBlock = false;
        clearDisplay();
    }

    private void clearDisplay() {
        this.mDisplayMatrix = null;
        this.mDisplayMatrixInverse = null;
        invalidate();
    }

    protected void configChanged() {
        this.mDirty = true;
    }

    public void applyAspect(float f, float f2) {
        if (f <= 0.0f || f2 <= 0.0f) {
            throw new IllegalArgumentException("Bad arguments to applyAspect");
        }
        if ((this.mRotation < 0 ? -this.mRotation : this.mRotation) % 180 == 90) {
            f2 = f;
            f = f2;
        }
        if (!this.mCropObj.setInnerAspectRatio(f, f2)) {
            Log.w("CropView", "failed to set aspect ratio");
        }
        invalidate();
    }

    public void setWallpaperSpotlight(float f, float f2) {
        this.mSpotX = f;
        this.mSpotY = f2;
        if (this.mSpotX > 0.0f && this.mSpotY > 0.0f) {
            this.mDoSpot = true;
        }
    }

    private int bitCycleLeft(int i, int i2, int i3) {
        int i4 = (1 << i3) - 1;
        int i5 = i & i4;
        int i6 = i2 % i3;
        return (i & (~i4)) | ((i5 << i6) & i4) | (i5 >> (i3 - i6));
    }

    private int decode(int i, float f) {
        int iConstrainedRotation = CropMath.constrainedRotation(f);
        if (iConstrainedRotation == 90) {
            return bitCycleLeft(i, 1, 4);
        }
        if (iConstrainedRotation == 180) {
            return bitCycleLeft(i, 2, 4);
        }
        if (iConstrainedRotation == 270) {
            return bitCycleLeft(i, 3, 4);
        }
        return i;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.mBitmap == null) {
            return;
        }
        if (this.mDirty) {
            this.mDirty = false;
            clearDisplay();
        }
        this.mImageBounds = new RectF(0.0f, 0.0f, this.mBitmap.getWidth(), this.mBitmap.getHeight());
        this.mScreenBounds = new RectF(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight());
        this.mScreenBounds.inset(this.mMargin, this.mMargin);
        if (this.mCropObj == null) {
            reset();
            this.mCropObj = new CropObject(this.mImageBounds, this.mImageBounds, 0);
        }
        if (this.mDisplayMatrix == null || this.mDisplayMatrixInverse == null) {
            this.mDisplayMatrix = new Matrix();
            this.mDisplayMatrix.reset();
            if (!CropDrawingUtils.setImageToScreenMatrix(this.mDisplayMatrix, this.mImageBounds, this.mScreenBounds, this.mRotation)) {
                Log.w("CropView", "failed to get screen matrix");
                this.mDisplayMatrix = null;
                return;
            }
            this.mDisplayMatrixInverse = new Matrix();
            this.mDisplayMatrixInverse.reset();
            if (!this.mDisplayMatrix.invert(this.mDisplayMatrixInverse)) {
                Log.w("CropView", "could not invert display matrix");
                this.mDisplayMatrixInverse = null;
                return;
            } else {
                this.mMinSideSize = (int) Math.min(Math.min(this.mImageBounds.width(), this.mImageBounds.height()), this.mMinSideSize);
                this.mCropObj.setMinInnerSideSize(this.mDisplayMatrixInverse.mapRadius(this.mMinSideSize));
                this.mCropObj.setTouchTolerance(this.mDisplayMatrixInverse.mapRadius(this.mTouchTolerance));
                invalidate();
                return;
            }
        }
        this.mScreenImageBounds.set(this.mImageBounds);
        this.mDisplayMatrix.mapRect(this.mScreenImageBounds);
        int iMapRadius = (int) this.mDisplayMatrix.mapRadius(this.mShadowMargin);
        this.mScreenImageBounds.roundOut(this.mShadowBounds);
        this.mShadowBounds.set(this.mShadowBounds.left - iMapRadius, this.mShadowBounds.top - iMapRadius, this.mShadowBounds.right + iMapRadius, this.mShadowBounds.bottom + iMapRadius);
        this.mShadow.setBounds(this.mShadowBounds);
        this.mShadow.draw(canvas);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setFilterBitmap(true);
        canvas.drawBitmap(this.mBitmap, this.mDisplayMatrix, this.mPaint);
        this.mCropObj.getInnerBounds(this.mScreenCropBounds);
        this.mDisplayMatrix.mapRect(this.mScreenCropBounds);
        Paint paint = new Paint();
        paint.setColor(this.mOverlayShadowColor);
        paint.setStyle(Paint.Style.FILL);
        CropDrawingUtils.drawShadows(canvas, paint, this.mScreenCropBounds, new RectF((float) Math.floor(this.mScreenImageBounds.left), (float) Math.floor(this.mScreenImageBounds.top), (float) Math.ceil(this.mScreenImageBounds.right), (float) Math.ceil(this.mScreenImageBounds.bottom)));
        CropDrawingUtils.drawCropRect(canvas, this.mScreenCropBounds);
        if (!this.mDoSpot) {
            CropDrawingUtils.drawRuleOfThird(canvas, this.mScreenCropBounds);
        } else {
            Paint paint2 = new Paint();
            paint2.setColor(this.mWPMarkerColor);
            paint2.setStrokeWidth(3.0f);
            paint2.setStyle(Paint.Style.STROKE);
            paint2.setPathEffect(new DashPathEffect(new float[]{this.mDashOnLength, this.mDashOnLength + this.mDashOffLength}, 0.0f));
            paint.setColor(this.mOverlayWPShadowColor);
            CropDrawingUtils.drawWallpaperSelectionFrame(canvas, this.mScreenCropBounds, this.mSpotX, this.mSpotY, paint2, paint);
        }
        CropDrawingUtils.drawIndicators(canvas, this.mCropIndicator, this.mIndicatorSize, this.mScreenCropBounds, this.mCropObj.isFixedAspect(), decode(this.mCropObj.getSelectState(), this.mRotation));
    }

    private float[] getPoint(MotionEvent motionEvent) {
        if (motionEvent.getPointerCount() == 1) {
            return new float[]{motionEvent.getX(), motionEvent.getY()};
        }
        return new float[]{(motionEvent.getX(0) + motionEvent.getX(1)) / 2.0f, (motionEvent.getY(0) + motionEvent.getY(1)) / 2.0f};
    }

    public void enableTouchMotion(boolean z) {
        this.mEnableTouchMotion = z;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (z) {
            configChanged();
        }
    }
}
