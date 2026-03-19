package com.android.gallery3d.filtershow.imageshow;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.crop.CropDrawingUtils;
import com.android.gallery3d.filtershow.editors.EditorStraighten;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterStraightenRepresentation;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import java.util.ArrayList;
import java.util.Collection;

public class ImageStraighten extends ImageShow {
    private static final String TAG = ImageStraighten.class.getSimpleName();
    private float mAngle;
    private int mAnimDelay;
    private ValueAnimator mAnimator;
    private float mBaseAngle;
    private RectF mCrop;
    private String mCropRepName;
    private float mCurrentX;
    private float mCurrentY;
    private int mDefaultGridAlpha;
    private GeometryMathUtils.GeometryHolder mDrawHolder;
    private Path mDrawPath;
    private RectF mDrawRect;
    private EditorStraighten mEditorStraighten;
    private boolean mFirstDrawSinceUp;
    private float mGridAlpha;
    private float mInitialAngle;
    volatile boolean mIsRepInit;
    private FilterStraightenRepresentation mLocalRep;
    private int mOnStartAnimDelay;
    private final Paint mPaint;
    private RectF mPriorCropAtUp;
    private MODES mState;
    private String mStraightenRepName;
    private float mTouchCenterX;
    private float mTouchCenterY;

    private enum MODES {
        NONE,
        MOVE
    }

    public ImageStraighten(Context context) {
        super(context);
        this.mBaseAngle = 0.0f;
        this.mAngle = 0.0f;
        this.mInitialAngle = 0.0f;
        this.mFirstDrawSinceUp = false;
        this.mLocalRep = new FilterStraightenRepresentation();
        this.mPriorCropAtUp = new RectF();
        this.mDrawRect = new RectF();
        this.mDrawPath = new Path();
        this.mDrawHolder = new GeometryMathUtils.GeometryHolder();
        this.mState = MODES.NONE;
        this.mAnimator = null;
        this.mDefaultGridAlpha = 60;
        this.mGridAlpha = 1.0f;
        this.mOnStartAnimDelay = 1000;
        this.mAnimDelay = 500;
        this.mCrop = new RectF();
        this.mPaint = new Paint();
        this.mIsRepInit = false;
        this.mStraightenRepName = context.getString(R.string.straighten);
        this.mCropRepName = context.getString(R.string.crop);
    }

    @Override
    public void attach() {
        super.attach();
        this.mGridAlpha = 1.0f;
        hidesGrid(this.mOnStartAnimDelay);
    }

    private void hidesGrid(int i) {
        this.mAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        this.mAnimator.setStartDelay(i);
        this.mAnimator.setDuration(this.mAnimDelay);
        this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ImageStraighten.this.mGridAlpha = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                ImageStraighten.this.invalidate();
            }
        });
        this.mAnimator.start();
    }

    public void setFilterStraightenRepresentation(FilterStraightenRepresentation filterStraightenRepresentation) {
        if (filterStraightenRepresentation == null) {
            filterStraightenRepresentation = new FilterStraightenRepresentation();
        }
        this.mLocalRep = filterStraightenRepresentation;
        this.mLocalRep.setName(this.mStraightenRepName);
        float straighten = this.mLocalRep.getStraighten();
        this.mAngle = straighten;
        this.mBaseAngle = straighten;
        this.mInitialAngle = straighten;
        this.mIsRepInit = true;
    }

    public Collection<FilterRepresentation> getFinalRepresentation() {
        ArrayList arrayList = new ArrayList(2);
        arrayList.add(this.mLocalRep);
        if (this.mInitialAngle != this.mLocalRep.getStraighten()) {
            FilterCropRepresentation filterCropRepresentation = new FilterCropRepresentation(this.mCrop);
            filterCropRepresentation.setName(this.mCropRepName);
            arrayList.add(filterCropRepresentation);
        }
        this.mIsRepInit = false;
        return arrayList;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        switch (motionEvent.getActionMasked()) {
            case 0:
                if (this.mState == MODES.NONE) {
                    this.mTouchCenterX = x;
                    this.mTouchCenterY = y;
                    this.mCurrentX = x;
                    this.mCurrentY = y;
                    this.mState = MODES.MOVE;
                    this.mBaseAngle = this.mAngle;
                }
                break;
            case 1:
                if (this.mState == MODES.MOVE) {
                    this.mState = MODES.NONE;
                    this.mCurrentX = x;
                    this.mCurrentY = y;
                    computeValue();
                    this.mFirstDrawSinceUp = true;
                    hidesGrid(0);
                }
                break;
            case 2:
                if (this.mState == MODES.MOVE) {
                    this.mCurrentX = x;
                    this.mCurrentY = y;
                    computeValue();
                }
                break;
        }
        invalidate();
        return true;
    }

    private static float angleFor(float f, float f2) {
        return (float) ((Math.atan2(f, f2) * 180.0d) / 3.141592653589793d);
    }

    private float getCurrentTouchAngle() {
        float width = getWidth() / 2.0f;
        float height = getHeight() / 2.0f;
        if (this.mCurrentX == this.mTouchCenterX && this.mCurrentY == this.mTouchCenterY) {
            return 0.0f;
        }
        float f = this.mTouchCenterX - width;
        float f2 = this.mTouchCenterY - height;
        return (angleFor(this.mCurrentX - width, this.mCurrentY - height) - angleFor(f, f2)) % 360.0f;
    }

    private void computeValue() {
        this.mAngle = (this.mBaseAngle - getCurrentTouchAngle()) % 360.0f;
        this.mAngle = Math.max(-45.0f, this.mAngle);
        this.mAngle = Math.min(45.0f, this.mAngle);
    }

    public static void getUntranslatedStraightenCropBounds(RectF rectF, float f) {
        if (f < 0.0f) {
            f = -f;
        }
        double radians = Math.toRadians(f);
        double dSin = Math.sin(radians);
        double dCos = Math.cos(radians);
        double dWidth = rectF.width();
        double dHeight = rectF.height();
        double dMin = Math.min((dHeight * dHeight) / ((dWidth * dSin) + (dHeight * dCos)), (dHeight * dWidth) / ((dCos * dWidth) + (dSin * dHeight)));
        double d = (dMin * dWidth) / dHeight;
        float f2 = (float) ((dWidth - d) * 0.5d);
        float f3 = (float) ((dHeight - dMin) * 0.5d);
        rectF.set(f2, f3, (float) (((double) f2) + d), (float) (((double) f3) + dMin));
    }

    private void updateCurrentCrop(Matrix matrix, GeometryMathUtils.GeometryHolder geometryHolder, RectF rectF, int i, int i2, int i3, int i4) {
        int i5;
        int i6;
        float f = i2;
        float f2 = i;
        rectF.set(0.0f, 0.0f, f, f2);
        matrix.mapRect(rectF);
        float f3 = rectF.top;
        float f4 = rectF.bottom;
        float f5 = rectF.left;
        float f6 = rectF.right;
        matrix.mapRect(rectF);
        if (GeometryMathUtils.needsDimensionSwap(geometryHolder.rotation)) {
            rectF.set(0.0f, 0.0f, f, f2);
            i6 = i;
            i5 = i2;
        } else {
            rectF.set(0.0f, 0.0f, f2, f);
            i5 = i;
            i6 = i2;
        }
        float f7 = i3;
        float f8 = i4;
        GeometryMathUtils.scaleRect(rectF, GeometryMathUtils.scale(i5, i6, f7, f8) * 0.9f);
        getUntranslatedStraightenCropBounds(rectF, this.mAngle);
        rectF.offset((f7 / 2.0f) - rectF.centerX(), (f8 / 2.0f) - rectF.centerY());
        geometryHolder.straighten = 0.0f;
        Matrix fullGeometryToScreenMatrix = GeometryMathUtils.getFullGeometryToScreenMatrix(geometryHolder, i, i2, i3, i4);
        matrix.reset();
        fullGeometryToScreenMatrix.invert(matrix);
        this.mCrop.set(rectF);
        matrix.mapRect(this.mCrop);
        FilterCropRepresentation.findNormalizedCrop(this.mCrop, i, i2);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (!this.mIsRepInit) {
            return;
        }
        Bitmap filtersOnlyImage = MasterImage.getImage().getFiltersOnlyImage();
        if (filtersOnlyImage == null) {
            MasterImage.getImage().invalidateFiltersOnly();
            return;
        }
        GeometryMathUtils.initializeHolder(this.mDrawHolder, this.mLocalRep);
        this.mDrawHolder.straighten = this.mAngle;
        int width = filtersOnlyImage.getWidth();
        int height = filtersOnlyImage.getHeight();
        int width2 = canvas.getWidth();
        int height2 = canvas.getHeight();
        Matrix fullGeometryToScreenMatrix = GeometryMathUtils.getFullGeometryToScreenMatrix(this.mDrawHolder, width, height, width2, height2);
        this.mPaint.reset();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setFilterBitmap(true);
        canvas.drawBitmap(filtersOnlyImage, fullGeometryToScreenMatrix, this.mPaint);
        this.mPaint.setFilterBitmap(false);
        this.mPaint.setColor(-1);
        this.mPaint.setStrokeWidth(2.0f);
        this.mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        updateCurrentCrop(fullGeometryToScreenMatrix, this.mDrawHolder, this.mDrawRect, width, height, width2, height2);
        if (this.mFirstDrawSinceUp) {
            this.mPriorCropAtUp.set(this.mCrop);
            this.mLocalRep.setStraighten(this.mAngle);
            this.mFirstDrawSinceUp = false;
        }
        CropDrawingUtils.drawShade(canvas, this.mDrawRect);
        if (this.mState == MODES.MOVE || this.mGridAlpha > 0.0f) {
            canvas.save();
            canvas.clipRect(this.mDrawRect);
            float fMax = Math.max(width2, height2) / 16;
            for (int i = 1; i < 16; i++) {
                float f = i * fMax;
                int i2 = (int) (this.mDefaultGridAlpha * this.mGridAlpha);
                if (i2 == 0 && this.mState == MODES.MOVE) {
                    i2 = this.mDefaultGridAlpha;
                }
                this.mPaint.setAlpha(i2);
                canvas.drawLine(f, 0.0f, f, height2, this.mPaint);
                canvas.drawLine(0.0f, f, width2, f, this.mPaint);
            }
            canvas.restore();
        }
        this.mPaint.reset();
        this.mPaint.setColor(-1);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeWidth(3.0f);
        this.mDrawPath.reset();
        this.mDrawPath.addRect(this.mDrawRect, Path.Direction.CW);
        canvas.drawPath(this.mDrawPath, this.mPaint);
    }

    public void setEditor(EditorStraighten editorStraighten) {
        this.mEditorStraighten = editorStraighten;
    }
}
