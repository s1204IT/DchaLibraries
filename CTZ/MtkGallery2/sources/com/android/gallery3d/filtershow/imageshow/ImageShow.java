package com.android.gallery3d.filtershow.imageshow;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.NinePatchDrawable;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.tools.SaveImage;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.galleryportable.SystemPropertyUtils;
import java.io.File;
import java.util.ArrayList;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class ImageShow extends View implements GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener {
    private static int UNVEIL_HORIZONTAL = 1;
    private static int UNVEIL_VERTICAL = 2;
    private static Bitmap sMask;
    private FilterShowActivity mActivity;
    private ValueAnimator mAnimatorScale;
    private ValueAnimator mAnimatorTranslateX;
    private ValueAnimator mAnimatorTranslateY;
    protected int mBackgroundColor;
    private int mCurrentEdgeEffect;
    private boolean mDidStartAnimation;
    private EdgeEffectCompat mEdgeEffect;
    private int mEdgeSize;
    private boolean mFinishedScalingOperation;
    private GestureDetector mGestureDetector;
    protected Rect mImageBounds;
    InteractionMode mInteractionMode;
    private Paint mMaskPaint;
    private boolean mOriginalDisabled;
    float mOriginalScale;
    private String mOriginalText;
    private int mOriginalTextMargin;
    private int mOriginalTextSize;
    Point mOriginalTranslation;
    protected Paint mPaint;
    private ScaleGestureDetector mScaleGestureDetector;
    private Matrix mShaderMatrix;
    private NinePatchDrawable mShadow;
    private Rect mShadowBounds;
    private boolean mShadowDrawn;
    private int mShadowMargin;
    private int mShowOriginalDirection;
    float mStartFocusX;
    float mStartFocusY;
    protected int mTextPadding;
    protected int mTextSize;
    private Point mTouch;
    private Point mTouchDown;
    private boolean mTouchShowOriginal;
    private long mTouchShowOriginalDate;
    private final long mTouchShowOriginalDelayMin;
    private boolean mZoomIn;

    private enum InteractionMode {
        NONE,
        SCALE,
        MOVE
    }

    private static Bitmap convertToAlphaMask(Bitmap bitmap) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ALPHA_8);
        new Canvas(bitmapCreateBitmap).drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
        return bitmapCreateBitmap;
    }

    private static Shader createShader(Bitmap bitmap) {
        return new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    }

    public FilterShowActivity getActivity() {
        return this.mActivity;
    }

    public boolean hasModifications() {
        return MasterImage.getImage().hasModifications();
    }

    public void resetParameter() {
    }

    public ImageShow(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mPaint = new Paint();
        this.mGestureDetector = null;
        this.mScaleGestureDetector = null;
        this.mImageBounds = new Rect();
        this.mOriginalDisabled = false;
        this.mTouchShowOriginal = false;
        this.mTouchShowOriginalDate = 0L;
        this.mTouchShowOriginalDelayMin = 200L;
        this.mShowOriginalDirection = 0;
        this.mShadow = null;
        this.mShadowBounds = new Rect();
        this.mShadowMargin = 15;
        this.mShadowDrawn = false;
        this.mTouchDown = new Point();
        this.mTouch = new Point();
        this.mFinishedScalingOperation = false;
        this.mZoomIn = false;
        this.mOriginalTranslation = new Point();
        this.mEdgeEffect = null;
        this.mCurrentEdgeEffect = 0;
        this.mEdgeSize = 100;
        this.mAnimatorScale = null;
        this.mAnimatorTranslateX = null;
        this.mAnimatorTranslateY = null;
        this.mInteractionMode = InteractionMode.NONE;
        this.mMaskPaint = new Paint();
        this.mShaderMatrix = new Matrix();
        this.mDidStartAnimation = false;
        this.mActivity = null;
        setupImageShow(context);
    }

    public ImageShow(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPaint = new Paint();
        this.mGestureDetector = null;
        this.mScaleGestureDetector = null;
        this.mImageBounds = new Rect();
        this.mOriginalDisabled = false;
        this.mTouchShowOriginal = false;
        this.mTouchShowOriginalDate = 0L;
        this.mTouchShowOriginalDelayMin = 200L;
        this.mShowOriginalDirection = 0;
        this.mShadow = null;
        this.mShadowBounds = new Rect();
        this.mShadowMargin = 15;
        this.mShadowDrawn = false;
        this.mTouchDown = new Point();
        this.mTouch = new Point();
        this.mFinishedScalingOperation = false;
        this.mZoomIn = false;
        this.mOriginalTranslation = new Point();
        this.mEdgeEffect = null;
        this.mCurrentEdgeEffect = 0;
        this.mEdgeSize = 100;
        this.mAnimatorScale = null;
        this.mAnimatorTranslateX = null;
        this.mAnimatorTranslateY = null;
        this.mInteractionMode = InteractionMode.NONE;
        this.mMaskPaint = new Paint();
        this.mShaderMatrix = new Matrix();
        this.mDidStartAnimation = false;
        this.mActivity = null;
        setupImageShow(context);
    }

    public ImageShow(Context context) {
        super(context);
        this.mPaint = new Paint();
        this.mGestureDetector = null;
        this.mScaleGestureDetector = null;
        this.mImageBounds = new Rect();
        this.mOriginalDisabled = false;
        this.mTouchShowOriginal = false;
        this.mTouchShowOriginalDate = 0L;
        this.mTouchShowOriginalDelayMin = 200L;
        this.mShowOriginalDirection = 0;
        this.mShadow = null;
        this.mShadowBounds = new Rect();
        this.mShadowMargin = 15;
        this.mShadowDrawn = false;
        this.mTouchDown = new Point();
        this.mTouch = new Point();
        this.mFinishedScalingOperation = false;
        this.mZoomIn = false;
        this.mOriginalTranslation = new Point();
        this.mEdgeEffect = null;
        this.mCurrentEdgeEffect = 0;
        this.mEdgeSize = 100;
        this.mAnimatorScale = null;
        this.mAnimatorTranslateX = null;
        this.mAnimatorTranslateY = null;
        this.mInteractionMode = InteractionMode.NONE;
        this.mMaskPaint = new Paint();
        this.mShaderMatrix = new Matrix();
        this.mDidStartAnimation = false;
        this.mActivity = null;
        setupImageShow(context);
    }

    private void setupImageShow(Context context) {
        Resources resources = context.getResources();
        this.mTextSize = resources.getDimensionPixelSize(R.dimen.photoeditor_text_size);
        this.mTextPadding = resources.getDimensionPixelSize(R.dimen.photoeditor_text_padding);
        this.mOriginalTextMargin = resources.getDimensionPixelSize(R.dimen.photoeditor_original_text_margin);
        this.mOriginalTextSize = resources.getDimensionPixelSize(R.dimen.photoeditor_original_text_size);
        this.mBackgroundColor = resources.getColor(R.color.background_screen);
        this.mOriginalText = resources.getString(R.string.original_picture_text);
        this.mShadow = (NinePatchDrawable) resources.getDrawable(R.drawable.geometry_shadow);
        setupGestureDetector(context);
        this.mActivity = (FilterShowActivity) context;
        if (sMask == null) {
            sMask = convertToAlphaMask(BitmapFactory.decodeResource(resources, R.drawable.spot_mask));
        }
        this.mEdgeEffect = new EdgeEffectCompat(context);
        this.mEdgeSize = resources.getDimensionPixelSize(R.dimen.edge_glow_size);
    }

    public void attach() {
        MasterImage.getImage().addObserver(this);
        bindAsImageLoadListener();
        MasterImage.getImage().resetGeometryImages(false);
    }

    public void detach() {
        MasterImage.getImage().removeObserver(this);
        MasterImage.getImage().removeListener(this);
        this.mMaskPaint.reset();
    }

    public void setupGestureDetector(Context context) {
        this.mGestureDetector = new GestureDetector(context, this);
        this.mScaleGestureDetector = new ScaleGestureDetector(context, this);
    }

    @Override
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(View.MeasureSpec.getSize(i), View.MeasureSpec.getSize(i2));
    }

    public ImageFilter getCurrentFilter() {
        return MasterImage.getImage().getCurrentFilter();
    }

    protected Matrix getImageToScreenMatrix(boolean z) {
        MasterImage image = MasterImage.getImage();
        if (image.getOriginalBounds() == null) {
            return new Matrix();
        }
        float width = getWidth() - (this.mShadowMargin * 2);
        float height = getHeight() - (2 * this.mShadowMargin);
        Matrix imageToScreenMatrix = GeometryMathUtils.getImageToScreenMatrix(image.getPreset().getGeometryFilters(), z, image.getOriginalBounds(), width, height);
        Point translation = image.getTranslation();
        float scaleFactor = image.getScaleFactor();
        imageToScreenMatrix.postTranslate(this.mShadowMargin, this.mShadowMargin);
        imageToScreenMatrix.postTranslate(translation.x, translation.y);
        imageToScreenMatrix.postScale(scaleFactor, scaleFactor, width / 2.0f, height / 2.0f);
        return imageToScreenMatrix;
    }

    protected Matrix getScreenToImageMatrix(boolean z) {
        Matrix imageToScreenMatrix = getImageToScreenMatrix(z);
        Matrix matrix = new Matrix();
        imageToScreenMatrix.invert(matrix);
        return matrix;
    }

    public ImagePreset getImagePreset() {
        return MasterImage.getImage().getPreset();
    }

    @Override
    public void onDraw(Canvas canvas) {
        this.mPaint.reset();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setFilterBitmap(true);
        if (MasterImage.getImage().getOriginalBounds() == null) {
            Log.v("ImageShow", "bitmap not ready, skip this onDraw pass");
            return;
        }
        MasterImage.getImage().setImageShowSize(getWidth() - (this.mShadowMargin * 2), getHeight() - (this.mShadowMargin * 2));
        MasterImage image = MasterImage.getImage();
        if (this.mActivity.isLoadingVisible() && getFilteredImage() != null) {
            if (image.getLoadedPreset() == null || (image.getLoadedPreset() != null && image.getLoadedPreset().equals(image.getCurrentPreset()))) {
                this.mActivity.stopLoadingIndicator();
            } else if (image.getLoadedPreset() != null) {
                return;
            }
            this.mActivity.stopLoadingIndicator();
        }
        canvas.save();
        this.mShadowDrawn = false;
        Bitmap highresImage = MasterImage.getImage().getHighresImage();
        MasterImage.getImage().getPartialImage();
        boolean zOnGoingNewLookAnimation = MasterImage.getImage().onGoingNewLookAnimation();
        if (highresImage == null || zOnGoingNewLookAnimation) {
            drawImageAndAnimate(canvas, getFilteredImage());
        } else {
            drawImageAndAnimate(canvas, highresImage);
        }
        drawCompareImage(canvas, getGeometryOnlyImage());
        canvas.restore();
        if (!this.mEdgeEffect.isFinished()) {
            canvas.save();
            float height = (getHeight() - getWidth()) / 2.0f;
            if (getWidth() > getHeight()) {
                height = (-(getWidth() - getHeight())) / 2.0f;
            }
            if (this.mCurrentEdgeEffect == 4) {
                canvas.rotate(180.0f, getWidth() / 2, getHeight() / 2);
            } else if (this.mCurrentEdgeEffect == 3) {
                canvas.rotate(90.0f, getWidth() / 2, getHeight() / 2);
                canvas.translate(0.0f, height);
            } else if (this.mCurrentEdgeEffect == 1) {
                canvas.rotate(270.0f, getWidth() / 2, getHeight() / 2);
                canvas.translate(0.0f, height);
            }
            if (this.mCurrentEdgeEffect != 0) {
                this.mEdgeEffect.draw(canvas);
            }
            canvas.restore();
            invalidate();
            return;
        }
        this.mCurrentEdgeEffect = 0;
    }

    public void resetImageCaches(ImageShow imageShow) {
        MasterImage.getImage().invalidatePreview();
    }

    public Bitmap getGeometryOnlyImage() {
        return MasterImage.getImage().getGeometryOnlyImage();
    }

    public Bitmap getFilteredImage() {
        return MasterImage.getImage().getFilteredImage();
    }

    public void drawImageAndAnimate(Canvas canvas, Bitmap bitmap) {
        MasterImage image;
        Matrix matrixComputeImageToScreen;
        if (bitmap == null || (matrixComputeImageToScreen = (image = MasterImage.getImage()).computeImageToScreen(bitmap, 0.0f, false)) == null) {
            return;
        }
        canvas.save();
        RectF rectF = new RectF(0.0f, 0.0f, bitmap.getWidth(), bitmap.getHeight());
        matrixComputeImageToScreen.mapRect(rectF);
        rectF.roundOut(this.mImageBounds);
        boolean zOnGoingNewLookAnimation = image.onGoingNewLookAnimation();
        boolean z = true;
        if (!zOnGoingNewLookAnimation && this.mDidStartAnimation) {
            if (image.getPreset().equals(image.getCurrentPreset())) {
                this.mDidStartAnimation = false;
                MasterImage.getImage().resetAnimBitmap();
            } else {
                zOnGoingNewLookAnimation = true;
            }
        } else if (zOnGoingNewLookAnimation) {
            this.mDidStartAnimation = true;
        }
        if (zOnGoingNewLookAnimation) {
            canvas.save();
            Bitmap previousImage = image.getPreviousImage();
            Matrix matrixComputeImageToScreen2 = image.computeImageToScreen(previousImage, 0.0f, false);
            RectF rectF2 = new RectF(0.0f, 0.0f, previousImage.getWidth(), previousImage.getHeight());
            matrixComputeImageToScreen2.mapRect(rectF2);
            Rect rect = new Rect();
            rectF2.roundOut(rect);
            float fCenterX = rectF2.centerX();
            float fCenterY = rectF2.centerY();
            if (image.getCurrentLookAnimation() == 1) {
                float maskScale = MasterImage.getImage().getMaskScale();
                if (maskScale >= 0.0f) {
                    float width = sMask.getWidth() / 2.0f;
                    float height = sMask.getHeight() / 2.0f;
                    Point pointHintTouchPoint = this.mActivity.hintTouchPoint(this);
                    float fMax = maskScale * ((2 * Math.max(getWidth(), getHeight())) / Math.min(width, height));
                    float f = pointHintTouchPoint.x - (width * fMax);
                    float f2 = pointHintTouchPoint.y - (height * fMax);
                    this.mShaderMatrix.reset();
                    float f3 = 1.0f / fMax;
                    this.mShaderMatrix.setScale(f3, f3);
                    this.mShaderMatrix.preTranslate((-f) + this.mImageBounds.left, (-f2) + this.mImageBounds.top);
                    this.mShaderMatrix.preScale(this.mImageBounds.width() / bitmap.getWidth(), this.mImageBounds.height() / bitmap.getHeight());
                    this.mMaskPaint.reset();
                    this.mMaskPaint.setShader(createShader(bitmap));
                    this.mMaskPaint.getShader().setLocalMatrix(this.mShaderMatrix);
                    canvas.drawBitmap(previousImage, matrixComputeImageToScreen2, this.mPaint);
                    canvas.clipRect(this.mImageBounds);
                    canvas.translate(f, f2);
                    canvas.scale(fMax, fMax);
                    canvas.drawBitmap(sMask, 0.0f, 0.0f, this.mMaskPaint);
                    z = false;
                }
            } else if (image.getCurrentLookAnimation() == 2) {
                float animFraction = (1.0f * (1.0f - image.getAnimFraction())) + ((computeImageBounds(image.getPreviousImage().getHeight(), image.getPreviousImage().getWidth()).width() / computeImageBounds(image.getPreviousImage().getWidth(), image.getPreviousImage().getHeight()).height()) * image.getAnimFraction());
                canvas.rotate(image.getAnimRotationValue(), fCenterX, fCenterY);
                canvas.scale(animFraction, animFraction, fCenterX, fCenterY);
            } else if (image.getCurrentLookAnimation() == 3 && (image.getCurrentFilterRepresentation() instanceof FilterMirrorRepresentation)) {
                FilterMirrorRepresentation filterMirrorRepresentation = (FilterMirrorRepresentation) image.getCurrentFilterRepresentation();
                GeometryMathUtils.GeometryHolder geometryHolderUnpackGeometry = GeometryMathUtils.unpackGeometry((ArrayList) image.getPreset().getGeometryFilters());
                if (geometryHolderUnpackGeometry.rotation.value() == 90 || geometryHolderUnpackGeometry.rotation.value() == 270) {
                    if (filterMirrorRepresentation.isHorizontal() && !filterMirrorRepresentation.isVertical()) {
                        canvas.scale(1.0f, image.getAnimRotationValue(), fCenterX, fCenterY);
                    } else if (filterMirrorRepresentation.isVertical() && !filterMirrorRepresentation.isHorizontal()) {
                        canvas.scale(1.0f, image.getAnimRotationValue(), fCenterX, fCenterY);
                    } else if (!filterMirrorRepresentation.isHorizontal() || filterMirrorRepresentation.isVertical()) {
                        canvas.scale(image.getAnimRotationValue(), 1.0f, fCenterX, fCenterY);
                    } else {
                        canvas.scale(image.getAnimRotationValue(), 1.0f, fCenterX, fCenterY);
                    }
                } else if (filterMirrorRepresentation.isHorizontal() && !filterMirrorRepresentation.isVertical()) {
                    canvas.scale(image.getAnimRotationValue(), 1.0f, fCenterX, fCenterY);
                } else if (filterMirrorRepresentation.isVertical() && !filterMirrorRepresentation.isHorizontal()) {
                    canvas.scale(image.getAnimRotationValue(), 1.0f, fCenterX, fCenterY);
                } else if (!filterMirrorRepresentation.isHorizontal() || filterMirrorRepresentation.isVertical()) {
                    canvas.scale(1.0f, image.getAnimRotationValue(), fCenterX, fCenterY);
                } else {
                    canvas.scale(1.0f, image.getAnimRotationValue(), fCenterX, fCenterY);
                }
            }
            if (z) {
                drawShadow(canvas, rect);
                canvas.drawBitmap(previousImage, matrixComputeImageToScreen2, this.mPaint);
            }
            canvas.restore();
        } else {
            drawShadow(canvas, this.mImageBounds);
            canvas.drawBitmap(bitmap, matrixComputeImageToScreen, this.mPaint);
        }
        canvas.restore();
    }

    private Rect computeImageBounds(int i, int i2) {
        float f = i;
        float f2 = i2;
        float fScale = GeometryMathUtils.scale(f, f2, getWidth(), getHeight());
        float f3 = f * fScale;
        float f4 = f2 * fScale;
        float height = (getHeight() - f4) / 2.0f;
        float width = (getWidth() - f3) / 2.0f;
        return new Rect(((int) width) + this.mShadowMargin, ((int) height) + this.mShadowMargin, ((int) (f3 + width)) - this.mShadowMargin, ((int) (f4 + height)) - this.mShadowMargin);
    }

    private void drawShadow(Canvas canvas, Rect rect) {
        if (!SystemPropertyUtils.get("prop.filtershow.imagetest").equals(SchemaSymbols.ATTVAL_TRUE_1) && !this.mShadowDrawn) {
            this.mShadowBounds.set(rect.left - this.mShadowMargin, rect.top - this.mShadowMargin, rect.right + this.mShadowMargin, rect.bottom + this.mShadowMargin);
            this.mShadow.setBounds(this.mShadowBounds);
            this.mShadow.draw(canvas);
            this.mShadowDrawn = true;
        }
    }

    public void drawCompareImage(Canvas canvas, Bitmap bitmap) {
        int iHeight;
        int iWidth;
        MasterImage image = MasterImage.getImage();
        boolean zShowsOriginal = image.showsOriginal();
        if (!zShowsOriginal && !this.mTouchShowOriginal) {
            return;
        }
        canvas.save();
        if (bitmap != null) {
            if (this.mShowOriginalDirection == 0) {
                if (Math.abs(this.mTouch.y - this.mTouchDown.y) > Math.abs(this.mTouch.x - this.mTouchDown.x)) {
                    this.mShowOriginalDirection = UNVEIL_VERTICAL;
                } else {
                    this.mShowOriginalDirection = UNVEIL_HORIZONTAL;
                }
            }
            if (this.mShowOriginalDirection == UNVEIL_VERTICAL) {
                iWidth = this.mImageBounds.width();
                iHeight = this.mTouch.y - this.mImageBounds.top;
            } else {
                int i = this.mTouch.x - this.mImageBounds.left;
                iHeight = this.mImageBounds.height();
                if (zShowsOriginal) {
                    iWidth = this.mImageBounds.width();
                } else {
                    iWidth = i;
                }
            }
            Rect rect = new Rect(this.mImageBounds.left, this.mImageBounds.top, this.mImageBounds.left + iWidth, this.mImageBounds.top + iHeight);
            if (this.mShowOriginalDirection == UNVEIL_HORIZONTAL) {
                if (this.mTouchDown.x - this.mTouch.x > 0) {
                    rect.set(this.mImageBounds.left + iWidth, this.mImageBounds.top, this.mImageBounds.right, this.mImageBounds.top + iHeight);
                }
            } else if (this.mTouchDown.y - this.mTouch.y > 0) {
                rect.set(this.mImageBounds.left, this.mImageBounds.top + iHeight, this.mImageBounds.left + iWidth, this.mImageBounds.bottom);
            }
            canvas.clipRect(rect);
            canvas.drawBitmap(bitmap, image.computeImageToScreen(bitmap, 0.0f, false), this.mPaint);
            Paint paint = new Paint();
            paint.setColor(-16777216);
            paint.setStrokeWidth(3.0f);
            if (this.mShowOriginalDirection == UNVEIL_VERTICAL) {
                canvas.drawLine(this.mImageBounds.left, this.mTouch.y, this.mImageBounds.right, this.mTouch.y, paint);
            } else {
                canvas.drawLine(this.mTouch.x, this.mImageBounds.top, this.mTouch.x, this.mImageBounds.bottom, paint);
            }
            Rect rect2 = new Rect();
            paint.setAntiAlias(true);
            paint.setTextSize(this.mOriginalTextSize);
            paint.getTextBounds(this.mOriginalText, 0, this.mOriginalText.length(), rect2);
            paint.setColor(-16777216);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3.0f);
            canvas.drawText(this.mOriginalText, this.mImageBounds.left + this.mOriginalTextMargin, this.mImageBounds.top + rect2.height() + this.mOriginalTextMargin, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(1.0f);
            paint.setColor(-1);
            canvas.drawText(this.mOriginalText, this.mImageBounds.left + this.mOriginalTextMargin, this.mImageBounds.top + rect2.height() + this.mOriginalTextMargin, paint);
        }
        canvas.restore();
    }

    public void bindAsImageLoadListener() {
        MasterImage.getImage().addListener(this);
    }

    public void updateImage() {
        invalidate();
    }

    public void imageLoaded() {
        updateImage();
    }

    public void saveImage(FilterShowActivity filterShowActivity, File file) {
        SaveImage.saveImage(getImagePreset(), filterShowActivity, file);
    }

    public boolean scaleInProgress() {
        return this.mScaleGestureDetector.isInProgress();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
        int action = motionEvent.getAction() & 255;
        this.mGestureDetector.onTouchEvent(motionEvent);
        boolean zScaleInProgress = scaleInProgress();
        this.mScaleGestureDetector.onTouchEvent(motionEvent);
        if (this.mInteractionMode == InteractionMode.SCALE) {
            return true;
        }
        if (!scaleInProgress() && zScaleInProgress) {
            this.mFinishedScalingOperation = true;
        }
        int x = (int) motionEvent.getX();
        int y = (int) motionEvent.getY();
        if (action == 0) {
            this.mInteractionMode = InteractionMode.MOVE;
            this.mTouchDown.x = x;
            this.mTouchDown.y = y;
            this.mTouchShowOriginalDate = System.currentTimeMillis();
            this.mShowOriginalDirection = 0;
            MasterImage.getImage().setOriginalTranslation(MasterImage.getImage().getTranslation());
        }
        if (action == 2 && this.mInteractionMode == InteractionMode.MOVE) {
            this.mTouch.x = x;
            this.mTouch.y = y;
            float scaleFactor = MasterImage.getImage().getScaleFactor();
            if (scaleFactor > 1.0f) {
                float f = (this.mTouch.x - this.mTouchDown.x) / scaleFactor;
                float f2 = (this.mTouch.y - this.mTouchDown.y) / scaleFactor;
                Point originalTranslation = MasterImage.getImage().getOriginalTranslation();
                Point translation = MasterImage.getImage().getTranslation();
                translation.x = (int) (originalTranslation.x + f);
                translation.y = (int) (originalTranslation.y + f2);
                MasterImage.getImage().setTranslation(translation);
                this.mTouchShowOriginal = false;
            } else if (enableComparison() && !this.mOriginalDisabled && System.currentTimeMillis() - this.mTouchShowOriginalDate > 200 && motionEvent.getPointerCount() == 1) {
                this.mTouchShowOriginal = true;
            }
        }
        if (action == 1 || action == 3 || action == 4) {
            this.mInteractionMode = InteractionMode.NONE;
            this.mTouchShowOriginal = false;
            this.mTouchDown.x = 0;
            this.mTouchDown.y = 0;
            this.mTouch.x = 0;
            this.mTouch.y = 0;
            if (MasterImage.getImage().getScaleFactor() <= 1.0f) {
                MasterImage.getImage().setScaleFactor(1.0f);
                MasterImage.getImage().resetTranslation();
            }
        }
        if (this.mActivity.getProcessingService().isRenderingTaskBusy()) {
            Log.d("ImageShow", "onTouchEvent, renderingTask is busy, so disable TouchShowOriginal");
            this.mTouchShowOriginal = false;
        }
        float scaleFactor2 = MasterImage.getImage().getScaleFactor();
        Point translation2 = MasterImage.getImage().getTranslation();
        constrainTranslation(translation2, scaleFactor2);
        MasterImage.getImage().setTranslation(translation2);
        invalidate();
        return true;
    }

    private void startAnimTranslation(int i, int i2, int i3, int i4, int i5) {
        if (i == i2 && i3 == i4) {
            return;
        }
        if (this.mAnimatorTranslateX != null) {
            this.mAnimatorTranslateX.cancel();
        }
        if (this.mAnimatorTranslateY != null) {
            this.mAnimatorTranslateY.cancel();
        }
        this.mAnimatorTranslateX = ValueAnimator.ofInt(i, i2);
        this.mAnimatorTranslateY = ValueAnimator.ofInt(i3, i4);
        long j = i5;
        this.mAnimatorTranslateX.setDuration(j);
        this.mAnimatorTranslateY.setDuration(j);
        this.mAnimatorTranslateX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Point translation = MasterImage.getImage().getTranslation();
                translation.x = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                MasterImage.getImage().setTranslation(translation);
                ImageShow.this.invalidate();
            }
        });
        this.mAnimatorTranslateY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Point translation = MasterImage.getImage().getTranslation();
                translation.y = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                MasterImage.getImage().setTranslation(translation);
                ImageShow.this.invalidate();
            }
        });
        this.mAnimatorTranslateX.start();
        this.mAnimatorTranslateY.start();
    }

    private void applyTranslationConstraints() {
        float scaleFactor = MasterImage.getImage().getScaleFactor();
        Point translation = MasterImage.getImage().getTranslation();
        int i = translation.x;
        int i2 = translation.y;
        constrainTranslation(translation, scaleFactor);
        if (i != translation.x || i2 != translation.y) {
            startAnimTranslation(i, translation.x, i2, translation.y, 200);
        }
    }

    protected boolean enableComparison() {
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        float maxScaleFactor;
        this.mZoomIn = !this.mZoomIn;
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        if (this.mZoomIn) {
            maxScaleFactor = MasterImage.getImage().getMaxScaleFactor();
        } else {
            maxScaleFactor = 1.0f;
        }
        if (maxScaleFactor != MasterImage.getImage().getScaleFactor()) {
            if (this.mAnimatorScale != null) {
                this.mAnimatorScale.cancel();
            }
            this.mAnimatorScale = ValueAnimator.ofFloat(MasterImage.getImage().getScaleFactor(), maxScaleFactor);
            float width = (getWidth() / 2) - x;
            float height = (getHeight() / 2) - y;
            Point translation = MasterImage.getImage().getTranslation();
            int i = translation.x;
            int i2 = translation.y;
            if (maxScaleFactor != 1.0f) {
                translation.x = (int) (this.mOriginalTranslation.x + width);
                translation.y = (int) (this.mOriginalTranslation.y + height);
            } else {
                translation.x = 0;
                translation.y = 0;
            }
            constrainTranslation(translation, maxScaleFactor);
            startAnimTranslation(i, translation.x, i2, translation.y, 400);
            this.mAnimatorScale.setDuration(400L);
            this.mAnimatorScale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    MasterImage.getImage().setScaleFactor(((Float) valueAnimator.getAnimatedValue()).floatValue());
                    ImageShow.this.invalidate();
                }
            });
            this.mAnimatorScale.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    ImageShow.this.applyTranslationConstraints();
                    MasterImage.getImage().needsUpdatePartialPreview();
                    ImageShow.this.invalidate();
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });
            this.mAnimatorScale.start();
        }
        return true;
    }

    private void constrainTranslation(Point point, float f) {
        int i = 0;
        if (f <= 1.0f) {
            this.mCurrentEdgeEffect = 0;
            this.mEdgeEffect.finish();
            return;
        }
        Matrix matrixOriginalImageToScreenWithRotation = MasterImage.getImage().originalImageToScreenWithRotation();
        if (matrixOriginalImageToScreenWithRotation == null) {
            this.mCurrentEdgeEffect = 0;
            this.mEdgeEffect.finish();
            return;
        }
        RectF rectF = new RectF(MasterImage.getImage().getOriginalBounds());
        matrixOriginalImageToScreenWithRotation.mapRect(rectF);
        boolean z = rectF.right < ((float) (getWidth() - this.mShadowMargin));
        boolean z2 = rectF.left > ((float) this.mShadowMargin);
        boolean z3 = rectF.top > ((float) this.mShadowMargin);
        boolean z4 = rectF.bottom < ((float) (getHeight() - this.mShadowMargin));
        int i2 = 2;
        if (rectF.width() > getWidth()) {
            if (z && !z2) {
                point.x = (int) (((getWidth() - this.mShadowMargin) - (rectF.right - (point.x * f))) / f);
                i = 3;
            } else if (z2 && !z) {
                point.x = (int) ((this.mShadowMargin - (rectF.left - (point.x * f))) / f);
                i = 1;
            }
        } else {
            point.x = (int) ((((getWidth() - this.mShadowMargin) - (rectF.right - (point.x * f))) - (((getWidth() - (this.mShadowMargin * 2)) - rectF.width()) / 2.0f)) / f);
        }
        if (rectF.height() > getHeight()) {
            if (z4 && !z3) {
                point.y = (int) (((getHeight() - this.mShadowMargin) - (rectF.bottom - (point.y * f))) / f);
                i2 = 4;
            } else if (z3 && !z4) {
                point.y = (int) ((this.mShadowMargin - (rectF.top - (point.y * f))) / f);
            }
            if (this.mCurrentEdgeEffect != i2) {
                if (this.mCurrentEdgeEffect == 0 || i2 != 0) {
                    this.mCurrentEdgeEffect = i2;
                    this.mEdgeEffect.finish();
                }
                this.mEdgeEffect.setSize(getWidth(), this.mEdgeSize);
            }
            if (i2 == 0) {
                this.mEdgeEffect.onPull(this.mEdgeSize);
                return;
            }
            return;
        }
        point.y = (int) ((((getHeight() - this.mShadowMargin) - (rectF.bottom - (point.y * f))) - (((getHeight() - (2 * this.mShadowMargin)) - rectF.height()) / 2.0f)) / f);
        i2 = i;
        if (this.mCurrentEdgeEffect != i2) {
        }
        if (i2 == 0) {
        }
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return (this.mActivity == null || motionEvent2.getPointerCount() == 2) ? false : true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    public void openUtilityPanel(LinearLayout linearLayout) {
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        MasterImage image = MasterImage.getImage();
        float scaleFactor = image.getScaleFactor() * scaleGestureDetector.getScaleFactor();
        if (scaleFactor > MasterImage.getImage().getMaxScaleFactor()) {
            scaleFactor = MasterImage.getImage().getMaxScaleFactor();
        }
        if (scaleFactor < 1.0f) {
            scaleFactor = 1.0f;
        }
        MasterImage.getImage().setScaleFactor(scaleFactor);
        float scaleFactor2 = image.getScaleFactor();
        float focusX = scaleGestureDetector.getFocusX();
        float focusY = scaleGestureDetector.getFocusY();
        float f = (focusX - this.mStartFocusX) / scaleFactor2;
        float f2 = (focusY - this.mStartFocusY) / scaleFactor2;
        Point translation = MasterImage.getImage().getTranslation();
        translation.x = (int) (this.mOriginalTranslation.x + f);
        translation.y = (int) (this.mOriginalTranslation.y + f2);
        MasterImage.getImage().setTranslation(translation);
        invalidate();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        Point translation = MasterImage.getImage().getTranslation();
        this.mOriginalTranslation.x = translation.x;
        this.mOriginalTranslation.y = translation.y;
        this.mOriginalScale = MasterImage.getImage().getScaleFactor();
        this.mStartFocusX = scaleGestureDetector.getFocusX();
        this.mStartFocusY = scaleGestureDetector.getFocusY();
        this.mInteractionMode = InteractionMode.SCALE;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        this.mInteractionMode = InteractionMode.NONE;
        if (MasterImage.getImage().getScaleFactor() < 1.0f) {
            MasterImage.getImage().setScaleFactor(1.0f);
            invalidate();
        }
    }

    public boolean didFinishScalingOperation() {
        if (!this.mFinishedScalingOperation) {
            return false;
        }
        this.mFinishedScalingOperation = false;
        return true;
    }
}
