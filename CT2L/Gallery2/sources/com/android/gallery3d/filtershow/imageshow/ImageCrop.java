package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.crop.CropDrawingUtils;
import com.android.gallery3d.filtershow.crop.CropMath;
import com.android.gallery3d.filtershow.crop.CropObject;
import com.android.gallery3d.filtershow.editors.EditorCrop;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;

public class ImageCrop extends ImageShow {
    private static final String TAG = ImageCrop.class.getSimpleName();
    private Drawable mCropIndicator;
    private CropObject mCropObj;
    private Matrix mDisplayCropMatrix;
    private Matrix mDisplayMatrix;
    private Matrix mDisplayMatrixInverse;
    EditorCrop mEditorCrop;
    private GeometryMathUtils.GeometryHolder mGeometry;
    private RectF mImageBounds;
    private int mIndicatorSize;
    FilterCropRepresentation mLocalRep;
    private int mMinSideSize;
    private boolean mMovingBlock;
    private Paint mPaint;
    private float mPrevX;
    private float mPrevY;
    private RectF mScreenCropBounds;
    private Mode mState;
    private int mTouchTolerance;
    private GeometryMathUtils.GeometryHolder mUpdateHolder;
    private boolean mValidDraw;

    private enum Mode {
        NONE,
        MOVE
    }

    public ImageCrop(Context context) {
        super(context);
        this.mImageBounds = new RectF();
        this.mScreenCropBounds = new RectF();
        this.mPaint = new Paint();
        this.mCropObj = null;
        this.mGeometry = new GeometryMathUtils.GeometryHolder();
        this.mUpdateHolder = new GeometryMathUtils.GeometryHolder();
        this.mMovingBlock = false;
        this.mDisplayMatrix = null;
        this.mDisplayCropMatrix = null;
        this.mDisplayMatrixInverse = null;
        this.mPrevX = 0.0f;
        this.mPrevY = 0.0f;
        this.mMinSideSize = 90;
        this.mTouchTolerance = 40;
        this.mState = Mode.NONE;
        this.mValidDraw = false;
        this.mLocalRep = new FilterCropRepresentation();
        setup(context);
    }

    private void setup(Context context) {
        Resources rsc = context.getResources();
        this.mCropIndicator = rsc.getDrawable(R.drawable.camera_crop);
        this.mIndicatorSize = (int) rsc.getDimension(R.dimen.crop_indicator_size);
        this.mMinSideSize = (int) rsc.getDimension(R.dimen.crop_min_side);
        this.mTouchTolerance = (int) rsc.getDimension(R.dimen.crop_touch_tolerance);
    }

    public void setFilterCropRepresentation(FilterCropRepresentation crop) {
        if (crop == null) {
            crop = new FilterCropRepresentation();
        }
        this.mLocalRep = crop;
        GeometryMathUtils.initializeHolder(this.mUpdateHolder, this.mLocalRep);
        this.mValidDraw = true;
    }

    public FilterCropRepresentation getFinalRepresentation() {
        return this.mLocalRep;
    }

    private void internallyUpdateLocalRep(RectF crop, RectF image) {
        FilterCropRepresentation.findNormalizedCrop(crop, (int) image.width(), (int) image.height());
        this.mGeometry.crop.set(crop);
        this.mUpdateHolder.set(this.mGeometry);
        this.mLocalRep.setCrop(crop);
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
                        internallyUpdateLocalRep(this.mCropObj.getInnerBounds(), this.mCropObj.getOuterBounds());
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

    private void clearDisplay() {
        this.mDisplayMatrix = null;
        this.mDisplayMatrixInverse = null;
        invalidate();
    }

    public void applyFreeAspect() {
        this.mCropObj.unsetAspectRatio();
        invalidate();
    }

    public void applyOriginalAspect() {
        RectF outer = this.mCropObj.getOuterBounds();
        float w = outer.width();
        float h = outer.height();
        if (w > 0.0f && h > 0.0f) {
            applyAspect(w, h);
            this.mCropObj.resetBoundsTo(outer, outer);
            internallyUpdateLocalRep(this.mCropObj.getInnerBounds(), this.mCropObj.getOuterBounds());
        } else {
            Log.w(TAG, "failed to set aspect ratio original");
        }
        invalidate();
    }

    public void applyAspect(float x, float y) {
        if (x <= 0.0f || y <= 0.0f) {
            throw new IllegalArgumentException("Bad arguments to applyAspect");
        }
        if (GeometryMathUtils.needsDimensionSwap(this.mGeometry.rotation)) {
            x = y;
            y = x;
        }
        if (!this.mCropObj.setInnerAspectRatio(x, y)) {
            Log.w(TAG, "failed to set aspect ratio");
        }
        internallyUpdateLocalRep(this.mCropObj.getInnerBounds(), this.mCropObj.getOuterBounds());
        invalidate();
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

    private void forceStateConsistency() {
        MasterImage master = MasterImage.getImage();
        Bitmap image = master.getFiltersOnlyImage();
        int width = image.getWidth();
        int height = image.getHeight();
        if (this.mCropObj == null || !this.mUpdateHolder.equals(this.mGeometry) || this.mImageBounds.width() != width || this.mImageBounds.height() != height || !this.mLocalRep.getCrop().equals(this.mUpdateHolder.crop)) {
            this.mImageBounds.set(0.0f, 0.0f, width, height);
            this.mGeometry.set(this.mUpdateHolder);
            this.mLocalRep.setCrop(this.mUpdateHolder.crop);
            RectF scaledCrop = new RectF(this.mUpdateHolder.crop);
            FilterCropRepresentation.findScaledCrop(scaledCrop, width, height);
            this.mCropObj = new CropObject(this.mImageBounds, scaledCrop, (int) this.mUpdateHolder.straighten);
            this.mState = Mode.NONE;
            clearDisplay();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        clearDisplay();
    }

    @Override
    public void onDraw(Canvas canvas) {
        Bitmap bitmap = MasterImage.getImage().getFiltersOnlyImage();
        if (bitmap == null) {
            MasterImage.getImage().invalidateFiltersOnly();
        }
        if (this.mValidDraw && bitmap != null) {
            forceStateConsistency();
            this.mImageBounds.set(0.0f, 0.0f, bitmap.getWidth(), bitmap.getHeight());
            if (this.mDisplayCropMatrix == null || this.mDisplayMatrix == null || this.mDisplayMatrixInverse == null) {
                this.mCropObj.unsetAspectRatio();
                this.mDisplayMatrix = GeometryMathUtils.getFullGeometryToScreenMatrix(this.mGeometry, bitmap.getWidth(), bitmap.getHeight(), canvas.getWidth(), canvas.getHeight());
                float straighten = this.mGeometry.straighten;
                this.mGeometry.straighten = 0.0f;
                this.mDisplayCropMatrix = GeometryMathUtils.getFullGeometryToScreenMatrix(this.mGeometry, bitmap.getWidth(), bitmap.getHeight(), canvas.getWidth(), canvas.getHeight());
                this.mGeometry.straighten = straighten;
                this.mDisplayMatrixInverse = new Matrix();
                this.mDisplayMatrixInverse.reset();
                if (!this.mDisplayCropMatrix.invert(this.mDisplayMatrixInverse)) {
                    Log.w(TAG, "could not invert display matrix");
                    this.mDisplayMatrixInverse = null;
                    return;
                }
                this.mCropObj.setMinInnerSideSize(this.mDisplayMatrixInverse.mapRadius(this.mMinSideSize));
                this.mCropObj.setTouchTolerance(this.mDisplayMatrixInverse.mapRadius(this.mTouchTolerance));
                int[] sides = {2, 8, 1, 4};
                int delta = Math.min(canvas.getWidth(), canvas.getHeight()) / 4;
                int[] dy = {delta, -delta, 0, 0};
                int[] dx = {0, 0, delta, -delta};
                for (int i = 0; i < sides.length; i++) {
                    this.mCropObj.selectEdge(sides[i]);
                    this.mCropObj.moveCurrentSelection(dx[i], dy[i]);
                    this.mCropObj.moveCurrentSelection(-dx[i], -dy[i]);
                }
                this.mCropObj.selectEdge(0);
            }
            this.mPaint.reset();
            this.mPaint.setAntiAlias(true);
            this.mPaint.setFilterBitmap(true);
            canvas.drawBitmap(bitmap, this.mDisplayMatrix, this.mPaint);
            this.mCropObj.getInnerBounds(this.mScreenCropBounds);
            RectF outer = this.mCropObj.getOuterBounds();
            FilterCropRepresentation.findNormalizedCrop(this.mScreenCropBounds, (int) outer.width(), (int) outer.height());
            FilterCropRepresentation.findScaledCrop(this.mScreenCropBounds, bitmap.getWidth(), bitmap.getHeight());
            if (this.mDisplayCropMatrix.mapRect(this.mScreenCropBounds)) {
                CropDrawingUtils.drawCropRect(canvas, this.mScreenCropBounds);
                CropDrawingUtils.drawShade(canvas, this.mScreenCropBounds);
                CropDrawingUtils.drawRuleOfThird(canvas, this.mScreenCropBounds);
                CropDrawingUtils.drawIndicators(canvas, this.mCropIndicator, this.mIndicatorSize, this.mScreenCropBounds, this.mCropObj.isFixedAspect(), decode(this.mCropObj.getSelectState(), this.mGeometry.rotation.value()));
            }
        }
    }

    public void setEditor(EditorCrop editorCrop) {
        this.mEditorCrop = editorCrop;
    }
}
