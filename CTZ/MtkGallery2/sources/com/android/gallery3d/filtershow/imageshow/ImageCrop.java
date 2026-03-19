package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.crop.CropDrawingUtils;
import com.android.gallery3d.filtershow.crop.CropMath;
import com.android.gallery3d.filtershow.crop.CropObject;
import com.android.gallery3d.filtershow.editors.EditorCrop;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.mediatek.gallery3d.util.Log;

public class ImageCrop extends ImageShow {
    private static final String TAG = ImageCrop.class.getSimpleName();
    private Drawable mCropIndicator;
    private CropObject mCropObj;
    private String mCropRepName;
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
        this.mCropRepName = context.getString(R.string.crop);
    }

    private void setup(Context context) {
        Resources resources = context.getResources();
        this.mCropIndicator = resources.getDrawable(R.drawable.camera_crop);
        this.mIndicatorSize = (int) resources.getDimension(R.dimen.crop_indicator_size);
        this.mMinSideSize = (int) resources.getDimension(R.dimen.crop_min_side);
        this.mTouchTolerance = (int) resources.getDimension(R.dimen.crop_touch_tolerance);
    }

    public void setFilterCropRepresentation(FilterCropRepresentation filterCropRepresentation) {
        if (filterCropRepresentation == null) {
            filterCropRepresentation = new FilterCropRepresentation();
        }
        this.mLocalRep = filterCropRepresentation;
        this.mLocalRep.setName(this.mCropRepName);
        GeometryMathUtils.initializeHolder(this.mUpdateHolder, this.mLocalRep);
        this.mValidDraw = true;
    }

    public FilterCropRepresentation getFinalRepresentation() {
        return this.mLocalRep;
    }

    private void internallyUpdateLocalRep(RectF rectF, RectF rectF2) {
        FilterCropRepresentation.findNormalizedCrop(rectF, (int) rectF2.width(), (int) rectF2.height());
        this.mGeometry.crop.set(rectF);
        this.mUpdateHolder.set(this.mGeometry);
        this.mLocalRep.setCrop(rectF);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        if (this.mDisplayMatrix == null || this.mDisplayMatrixInverse == null) {
            return true;
        }
        float[] fArr = {x, y};
        this.mDisplayMatrixInverse.mapPoints(fArr);
        float f = fArr[0];
        float f2 = fArr[1];
        switch (motionEvent.getActionMasked()) {
            case 0:
                if (this.mState == Mode.NONE) {
                    if (!this.mCropObj.selectEdge(f, f2)) {
                        this.mMovingBlock = this.mCropObj.selectEdge(16);
                    }
                    this.mPrevX = f;
                    this.mPrevY = f2;
                    this.mState = Mode.MOVE;
                }
                break;
            case 1:
                if (this.mState == Mode.MOVE) {
                    this.mCropObj.selectEdge(0);
                    this.mMovingBlock = false;
                    this.mPrevX = f;
                    this.mPrevY = f2;
                    this.mState = Mode.NONE;
                    internallyUpdateLocalRep(this.mCropObj.getInnerBounds(), this.mCropObj.getOuterBounds());
                }
                break;
            case 2:
                if (this.mState == Mode.MOVE) {
                    this.mCropObj.moveCurrentSelection(f - this.mPrevX, f2 - this.mPrevY);
                    this.mPrevX = f;
                    this.mPrevY = f2;
                }
                break;
        }
        invalidate();
        return true;
    }

    private void clearDisplay() {
        this.mDisplayMatrix = null;
        this.mDisplayMatrixInverse = null;
        invalidate();
    }

    public void applyFreeAspect() {
        if (this.mCropObj != null) {
            this.mCropObj.unsetAspectRatio();
            this.mCropObj.resetInnerRect();
        }
        invalidate();
    }

    public void applyOriginalAspect() {
        this.mCropObj.setAspectRatio();
        this.mCropObj.resetInnerRect();
        invalidate();
    }

    public void applyAspect(float f, float f2) {
        if (this.mCropObj == null) {
            return;
        }
        if (f <= 0.0f || f2 <= 0.0f) {
            throw new IllegalArgumentException("Bad arguments to applyAspect");
        }
        if (GeometryMathUtils.needsDimensionSwap(this.mGeometry.rotation)) {
            f2 = f;
            f = f2;
        }
        if (!this.mCropObj.setInnerAspectRatio(f, f2)) {
            Log.w(TAG, "failed to set aspect ratio");
        }
        internallyUpdateLocalRep(this.mCropObj.getInnerBounds(), this.mCropObj.getOuterBounds());
        invalidate();
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

    private void forceStateConsistency() {
        Bitmap filtersOnlyImage = MasterImage.getImage().getFiltersOnlyImage();
        int width = filtersOnlyImage.getWidth();
        int height = filtersOnlyImage.getHeight();
        if (this.mCropObj == null || !this.mUpdateHolder.equals(this.mGeometry) || this.mImageBounds.width() != width || this.mImageBounds.height() != height || !this.mLocalRep.getCrop().equals(this.mUpdateHolder.crop)) {
            this.mImageBounds.set(0.0f, 0.0f, width, height);
            this.mGeometry.set(this.mUpdateHolder);
            this.mLocalRep.setCrop(this.mUpdateHolder.crop);
            RectF rectF = new RectF(this.mUpdateHolder.crop);
            FilterCropRepresentation.findScaledCrop(rectF, width, height);
            ImageStraighten.getUntranslatedStraightenCropBounds(rectF, this.mUpdateHolder.straighten);
            this.mCropObj = new CropObject(this.mImageBounds, rectF, (int) this.mUpdateHolder.straighten);
            this.mState = Mode.NONE;
            clearDisplay();
        }
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        clearDisplay();
    }

    @Override
    public void onDraw(Canvas canvas) {
        Bitmap filtersOnlyImage = MasterImage.getImage().getFiltersOnlyImage();
        if (filtersOnlyImage == null) {
            MasterImage.getImage().invalidateFiltersOnly();
        }
        if (!this.mValidDraw || filtersOnlyImage == null) {
            return;
        }
        forceStateConsistency();
        this.mImageBounds.set(0.0f, 0.0f, filtersOnlyImage.getWidth(), filtersOnlyImage.getHeight());
        if (this.mDisplayCropMatrix == null || this.mDisplayMatrix == null || this.mDisplayMatrixInverse == null) {
            this.mCropObj.unsetAspectRatio();
            this.mDisplayMatrix = GeometryMathUtils.getFullGeometryToScreenMatrix(this.mGeometry, filtersOnlyImage.getWidth(), filtersOnlyImage.getHeight(), canvas.getWidth(), canvas.getHeight());
            float f = this.mGeometry.straighten;
            this.mGeometry.straighten = 0.0f;
            this.mDisplayCropMatrix = GeometryMathUtils.getFullGeometryToScreenMatrix(this.mGeometry, filtersOnlyImage.getWidth(), filtersOnlyImage.getHeight(), canvas.getWidth(), canvas.getHeight());
            this.mGeometry.straighten = f;
            this.mDisplayMatrixInverse = new Matrix();
            this.mDisplayMatrixInverse.reset();
            if (!this.mDisplayCropMatrix.invert(this.mDisplayMatrixInverse)) {
                Log.w(TAG, "could not invert display matrix");
                this.mDisplayMatrixInverse = null;
                return;
            }
            this.mMinSideSize = (int) Math.min(Math.min(this.mImageBounds.width(), this.mImageBounds.height()), this.mMinSideSize);
            this.mCropObj.setMinInnerSideSize(this.mDisplayMatrixInverse.mapRadius(this.mMinSideSize));
            this.mCropObj.setTouchTolerance(this.mDisplayMatrixInverse.mapRadius(this.mTouchTolerance));
            int[] iArr = {2, 8, 1, 4};
            RectF innerBounds = this.mCropObj.getInnerBounds();
            float fMin = Math.min(Math.min(innerBounds.width(), innerBounds.height()) - this.mCropObj.getMinSideSize(), Math.min(canvas.getWidth(), canvas.getHeight()) / 4);
            float f2 = -fMin;
            float[] fArr = {fMin, f2, 0.0f, 0.0f};
            float[] fArr2 = {0.0f, 0.0f, fMin, f2};
            for (int i = 0; i < iArr.length; i++) {
                this.mCropObj.selectEdge(iArr[i]);
                this.mCropObj.moveCurrentSelection(fArr2[i], fArr[i]);
                this.mCropObj.moveCurrentSelection(-fArr2[i], -fArr[i]);
            }
            this.mCropObj.setOriginalInnerRect(this.mCropObj.getInnerBounds());
            this.mCropObj.selectEdge(0);
        }
        this.mPaint.reset();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setFilterBitmap(true);
        canvas.drawBitmap(filtersOnlyImage, this.mDisplayMatrix, this.mPaint);
        this.mCropObj.getInnerBounds(this.mScreenCropBounds);
        RectF outerBounds = this.mCropObj.getOuterBounds();
        FilterCropRepresentation.findNormalizedCrop(this.mScreenCropBounds, (int) outerBounds.width(), (int) outerBounds.height());
        FilterCropRepresentation.findScaledCrop(this.mScreenCropBounds, filtersOnlyImage.getWidth(), filtersOnlyImage.getHeight());
        this.mDisplayCropMatrix.mapRect(this.mScreenCropBounds);
        CropDrawingUtils.drawCropRect(canvas, this.mScreenCropBounds);
        CropDrawingUtils.drawShade(canvas, this.mScreenCropBounds);
        CropDrawingUtils.drawRuleOfThird(canvas, this.mScreenCropBounds);
        CropDrawingUtils.drawIndicators(canvas, this.mCropIndicator, this.mIndicatorSize, this.mScreenCropBounds, this.mCropObj.isFixedAspect(), decode(this.mCropObj.getSelectState(), this.mGeometry.rotation.value()));
    }

    public void setEditor(EditorCrop editorCrop) {
        this.mEditorCrop = editorCrop;
    }
}
