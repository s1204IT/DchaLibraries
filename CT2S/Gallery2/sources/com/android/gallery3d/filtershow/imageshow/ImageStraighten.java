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
    private FilterStraightenRepresentation mLocalRep;
    private int mOnStartAnimDelay;
    private final Paint mPaint;
    private RectF mPriorCropAtUp;
    private MODES mState;
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
    }

    @Override
    public void attach() {
        super.attach();
        this.mGridAlpha = 1.0f;
        hidesGrid(this.mOnStartAnimDelay);
    }

    private void hidesGrid(int delay) {
        this.mAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        this.mAnimator.setStartDelay(delay);
        this.mAnimator.setDuration(this.mAnimDelay);
        this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ImageStraighten.this.mGridAlpha = ((Float) animation.getAnimatedValue()).floatValue();
                ImageStraighten.this.invalidate();
            }
        });
        this.mAnimator.start();
    }

    public void setFilterStraightenRepresentation(FilterStraightenRepresentation rep) {
        if (rep == null) {
            rep = new FilterStraightenRepresentation();
        }
        this.mLocalRep = rep;
        float straighten = this.mLocalRep.getStraighten();
        this.mAngle = straighten;
        this.mBaseAngle = straighten;
        this.mInitialAngle = straighten;
    }

    public Collection<FilterRepresentation> getFinalRepresentation() {
        ArrayList<FilterRepresentation> reps = new ArrayList<>(2);
        reps.add(this.mLocalRep);
        if (this.mInitialAngle != this.mLocalRep.getStraighten()) {
            reps.add(new FilterCropRepresentation(this.mCrop));
        }
        return reps;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
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

    private static float angleFor(float dx, float dy) {
        return (float) ((Math.atan2(dx, dy) * 180.0d) / 3.141592653589793d);
    }

    private float getCurrentTouchAngle() {
        float centerX = getWidth() / 2.0f;
        float centerY = getHeight() / 2.0f;
        if (this.mCurrentX == this.mTouchCenterX && this.mCurrentY == this.mTouchCenterY) {
            return 0.0f;
        }
        float dX1 = this.mTouchCenterX - centerX;
        float dY1 = this.mTouchCenterY - centerY;
        float dX2 = this.mCurrentX - centerX;
        float dY2 = this.mCurrentY - centerY;
        float angleA = angleFor(dX1, dY1);
        float angleB = angleFor(dX2, dY2);
        return (angleB - angleA) % 360.0f;
    }

    private void computeValue() {
        float angle = getCurrentTouchAngle();
        this.mAngle = (this.mBaseAngle - angle) % 360.0f;
        this.mAngle = Math.max(-45.0f, this.mAngle);
        this.mAngle = Math.min(45.0f, this.mAngle);
    }

    public static void getUntranslatedStraightenCropBounds(RectF outRect, float straightenAngle) {
        float deg = straightenAngle;
        if (deg < 0.0f) {
            deg = -deg;
        }
        double a = Math.toRadians(deg);
        double sina = Math.sin(a);
        double cosa = Math.cos(a);
        double rw = outRect.width();
        double rh = outRect.height();
        double h1 = (rh * rh) / ((rw * sina) + (rh * cosa));
        double h2 = (rh * rw) / ((rw * cosa) + (rh * sina));
        double hh = Math.min(h1, h2);
        double ww = (hh * rw) / rh;
        float left = (float) ((rw - ww) * 0.5d);
        float top = (float) ((rh - hh) * 0.5d);
        float right = (float) (((double) left) + ww);
        float bottom = (float) (((double) top) + hh);
        outRect.set(left, top, right, bottom);
    }

    private void updateCurrentCrop(Matrix m, GeometryMathUtils.GeometryHolder h, RectF tmp, int imageWidth, int imageHeight, int viewWidth, int viewHeight) {
        int iw;
        int ih;
        tmp.set(0.0f, 0.0f, imageHeight, imageWidth);
        m.mapRect(tmp);
        float f = tmp.top;
        float f2 = tmp.bottom;
        float f3 = tmp.left;
        float f4 = tmp.right;
        m.mapRect(tmp);
        if (GeometryMathUtils.needsDimensionSwap(h.rotation)) {
            tmp.set(0.0f, 0.0f, imageHeight, imageWidth);
            iw = imageHeight;
            ih = imageWidth;
        } else {
            tmp.set(0.0f, 0.0f, imageWidth, imageHeight);
            iw = imageWidth;
            ih = imageHeight;
        }
        float scale = GeometryMathUtils.scale(iw, ih, viewWidth, viewHeight);
        GeometryMathUtils.scaleRect(tmp, scale * 0.9f);
        getUntranslatedStraightenCropBounds(tmp, this.mAngle);
        tmp.offset((viewWidth / 2.0f) - tmp.centerX(), (viewHeight / 2.0f) - tmp.centerY());
        h.straighten = 0.0f;
        Matrix m1 = GeometryMathUtils.getFullGeometryToScreenMatrix(h, imageWidth, imageHeight, viewWidth, viewHeight);
        m.reset();
        m1.invert(m);
        this.mCrop.set(tmp);
        m.mapRect(this.mCrop);
        FilterCropRepresentation.findNormalizedCrop(this.mCrop, imageWidth, imageHeight);
    }

    @Override
    public void onDraw(Canvas canvas) {
        MasterImage master = MasterImage.getImage();
        Bitmap image = master.getFiltersOnlyImage();
        if (image == null) {
            MasterImage.getImage().invalidateFiltersOnly();
            return;
        }
        GeometryMathUtils.initializeHolder(this.mDrawHolder, this.mLocalRep);
        this.mDrawHolder.straighten = this.mAngle;
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int viewWidth = canvas.getWidth();
        int viewHeight = canvas.getHeight();
        Matrix m = GeometryMathUtils.getFullGeometryToScreenMatrix(this.mDrawHolder, imageWidth, imageHeight, viewWidth, viewHeight);
        this.mPaint.reset();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setFilterBitmap(true);
        canvas.drawBitmap(image, m, this.mPaint);
        this.mPaint.setFilterBitmap(false);
        this.mPaint.setColor(-1);
        this.mPaint.setStrokeWidth(2.0f);
        this.mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        updateCurrentCrop(m, this.mDrawHolder, this.mDrawRect, imageWidth, imageHeight, viewWidth, viewHeight);
        if (this.mFirstDrawSinceUp) {
            this.mPriorCropAtUp.set(this.mCrop);
            this.mLocalRep.setStraighten(this.mAngle);
            this.mFirstDrawSinceUp = false;
        }
        CropDrawingUtils.drawShade(canvas, this.mDrawRect);
        if (this.mState == MODES.MOVE || this.mGridAlpha > 0.0f) {
            canvas.save();
            canvas.clipRect(this.mDrawRect);
            float step = Math.max(viewWidth, viewHeight) / 16;
            for (int i = 1; i < 16; i++) {
                float p = i * step;
                int alpha = (int) (this.mDefaultGridAlpha * this.mGridAlpha);
                if (alpha == 0 && this.mState == MODES.MOVE) {
                    alpha = this.mDefaultGridAlpha;
                }
                this.mPaint.setAlpha(alpha);
                canvas.drawLine(p, 0.0f, p, viewHeight, this.mPaint);
                canvas.drawLine(0.0f, p, viewWidth, p, this.mPaint);
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
