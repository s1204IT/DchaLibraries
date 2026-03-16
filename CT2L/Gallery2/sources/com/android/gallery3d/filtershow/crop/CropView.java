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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.android.gallery3d.R;

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
        MOVE
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
        this.mWPMarkerColor = Integer.MAX_VALUE;
        this.mMinSideSize = 90;
        this.mTouchTolerance = 40;
        this.mDashOnLength = 20.0f;
        this.mDashOffLength = 10.0f;
        this.mState = Mode.NONE;
        setup(context);
    }

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        this.mWPMarkerColor = Integer.MAX_VALUE;
        this.mMinSideSize = 90;
        this.mTouchTolerance = 40;
        this.mDashOnLength = 20.0f;
        this.mDashOffLength = 10.0f;
        this.mState = Mode.NONE;
        setup(context);
    }

    public CropView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
        this.mWPMarkerColor = Integer.MAX_VALUE;
        this.mMinSideSize = 90;
        this.mTouchTolerance = 40;
        this.mDashOnLength = 20.0f;
        this.mDashOffLength = 10.0f;
        this.mState = Mode.NONE;
        setup(context);
    }

    private void setup(Context context) {
        Resources rsc = context.getResources();
        this.mShadow = (NinePatchDrawable) rsc.getDrawable(R.drawable.geometry_shadow);
        this.mCropIndicator = rsc.getDrawable(R.drawable.camera_crop);
        this.mIndicatorSize = (int) rsc.getDimension(R.dimen.crop_indicator_size);
        this.mShadowMargin = (int) rsc.getDimension(R.dimen.shadow_margin);
        this.mMargin = (int) rsc.getDimension(R.dimen.preview_margin);
        this.mMinSideSize = (int) rsc.getDimension(R.dimen.crop_min_side);
        this.mTouchTolerance = (int) rsc.getDimension(R.dimen.crop_touch_tolerance);
        this.mOverlayShadowColor = rsc.getColor(R.color.crop_shadow_color);
        this.mOverlayWPShadowColor = rsc.getColor(R.color.crop_shadow_wp_color);
        this.mWPMarkerColor = rsc.getColor(R.color.crop_wp_markers);
        this.mDashOnLength = rsc.getDimension(R.dimen.wp_selector_dash_length);
        this.mDashOffLength = rsc.getDimension(R.dimen.wp_selector_off_length);
    }

    public void initialize(Bitmap image, RectF newCropBounds, RectF newPhotoBounds, int rotation) {
        this.mBitmap = image;
        if (this.mCropObj != null) {
            RectF crop = this.mCropObj.getInnerBounds();
            RectF containing = this.mCropObj.getOuterBounds();
            if (crop != newCropBounds || containing != newPhotoBounds || this.mRotation != rotation) {
                this.mRotation = rotation;
                this.mCropObj.resetBoundsTo(newCropBounds, newPhotoBounds);
                clearDisplay();
                return;
            }
            return;
        }
        this.mRotation = rotation;
        this.mCropObj = new CropObject(newPhotoBounds, newCropBounds, 0);
        clearDisplay();
    }

    public RectF getCrop() {
        return this.mCropObj.getInnerBounds();
    }

    public RectF getPhoto() {
        return this.mCropObj.getOuterBounds();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (this.mDisplayMatrix != null && this.mDisplayMatrixInverse != null) {
            float[] touchPoint = {x, y};
            this.mDisplayMatrixInverse.mapPoints(touchPoint);
            float x2 = touchPoint[0];
            float y2 = touchPoint[1];
            switch (event.getActionMasked()) {
                case 0:
                    if (this.mState == Mode.NONE) {
                        if (!this.mCropObj.selectEdge(x2, y2)) {
                            this.mMovingBlock = this.mCropObj.selectEdge(16);
                        }
                        this.mPrevX = x2;
                        this.mPrevY = y2;
                        this.mState = Mode.MOVE;
                    }
                    break;
                case 1:
                    if (this.mState == Mode.MOVE) {
                        this.mCropObj.selectEdge(0);
                        this.mMovingBlock = false;
                        this.mPrevX = x2;
                        this.mPrevY = y2;
                        this.mState = Mode.NONE;
                    }
                    break;
                case 2:
                    if (this.mState == Mode.MOVE) {
                        float dx = x2 - this.mPrevX;
                        float dy = y2 - this.mPrevY;
                        this.mCropObj.moveCurrentSelection(dx, dy);
                        this.mPrevX = x2;
                        this.mPrevY = y2;
                    }
                    break;
            }
            invalidate();
        }
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

    public void applyAspect(float x, float y) {
        if (x <= 0.0f || y <= 0.0f) {
            throw new IllegalArgumentException("Bad arguments to applyAspect");
        }
        if ((this.mRotation < 0 ? -this.mRotation : this.mRotation) % 180 == 90) {
            x = y;
            y = x;
        }
        if (!this.mCropObj.setInnerAspectRatio(x, y)) {
            Log.w("CropView", "failed to set aspect ratio");
        }
        invalidate();
    }

    public void setWallpaperSpotlight(float spotlightX, float spotlightY) {
        this.mSpotX = spotlightX;
        this.mSpotY = spotlightY;
        if (this.mSpotX > 0.0f && this.mSpotY > 0.0f) {
            this.mDoSpot = true;
        }
    }

    private int bitCycleLeft(int x, int times, int d) {
        int mask = (1 << d) - 1;
        int mout = x & mask;
        int times2 = times % d;
        int hi = mout >> (d - times2);
        int low = (mout << times2) & mask;
        int ret = x & (mask ^ (-1));
        return ret | low | hi;
    }

    private int decode(int movingEdges, float rotation) {
        int rot = CropMath.constrainedRotation(rotation);
        switch (rot) {
            case 90:
                return bitCycleLeft(movingEdges, 1, 4);
            case 180:
                return bitCycleLeft(movingEdges, 2, 4);
            case 270:
                return bitCycleLeft(movingEdges, 3, 4);
            default:
                return movingEdges;
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.mBitmap != null) {
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
                    this.mCropObj.setMinInnerSideSize(this.mDisplayMatrixInverse.mapRadius(this.mMinSideSize));
                    this.mCropObj.setTouchTolerance(this.mDisplayMatrixInverse.mapRadius(this.mTouchTolerance));
                }
            }
            this.mScreenImageBounds.set(this.mImageBounds);
            if (this.mDisplayMatrix.mapRect(this.mScreenImageBounds)) {
                int margin = (int) this.mDisplayMatrix.mapRadius(this.mShadowMargin);
                this.mScreenImageBounds.roundOut(this.mShadowBounds);
                this.mShadowBounds.set(this.mShadowBounds.left - margin, this.mShadowBounds.top - margin, this.mShadowBounds.right + margin, this.mShadowBounds.bottom + margin);
                this.mShadow.setBounds(this.mShadowBounds);
                this.mShadow.draw(canvas);
            }
            this.mPaint.setAntiAlias(true);
            this.mPaint.setFilterBitmap(true);
            canvas.drawBitmap(this.mBitmap, this.mDisplayMatrix, this.mPaint);
            this.mCropObj.getInnerBounds(this.mScreenCropBounds);
            if (this.mDisplayMatrix.mapRect(this.mScreenCropBounds)) {
                Paint p = new Paint();
                p.setColor(this.mOverlayShadowColor);
                p.setStyle(Paint.Style.FILL);
                CropDrawingUtils.drawShadows(canvas, p, this.mScreenCropBounds, this.mScreenImageBounds);
                CropDrawingUtils.drawCropRect(canvas, this.mScreenCropBounds);
                if (!this.mDoSpot) {
                    CropDrawingUtils.drawRuleOfThird(canvas, this.mScreenCropBounds);
                } else {
                    Paint wpPaint = new Paint();
                    wpPaint.setColor(this.mWPMarkerColor);
                    wpPaint.setStrokeWidth(3.0f);
                    wpPaint.setStyle(Paint.Style.STROKE);
                    wpPaint.setPathEffect(new DashPathEffect(new float[]{this.mDashOnLength, this.mDashOnLength + this.mDashOffLength}, 0.0f));
                    p.setColor(this.mOverlayWPShadowColor);
                    CropDrawingUtils.drawWallpaperSelectionFrame(canvas, this.mScreenCropBounds, this.mSpotX, this.mSpotY, wpPaint, p);
                }
                CropDrawingUtils.drawIndicators(canvas, this.mCropIndicator, this.mIndicatorSize, this.mScreenCropBounds, this.mCropObj.isFixedAspect(), decode(this.mCropObj.getSelectState(), this.mRotation));
            }
        }
    }
}
