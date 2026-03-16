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
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.tools.SaveImage;
import java.io.File;
import java.util.ArrayList;

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

    private static Bitmap convertToAlphaMask(Bitmap b) {
        Bitmap a = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ALPHA_8);
        Canvas c = new Canvas(a);
        c.drawBitmap(b, 0.0f, 0.0f, (Paint) null);
        return a;
    }

    private static Shader createShader(Bitmap b) {
        return new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    }

    public FilterShowActivity getActivity() {
        return this.mActivity;
    }

    public boolean hasModifications() {
        return MasterImage.getImage().hasModifications();
    }

    public void resetParameter() {
    }

    public ImageShow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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

    public ImageShow(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        Resources res = context.getResources();
        this.mTextSize = res.getDimensionPixelSize(R.dimen.photoeditor_text_size);
        this.mTextPadding = res.getDimensionPixelSize(R.dimen.photoeditor_text_padding);
        this.mOriginalTextMargin = res.getDimensionPixelSize(R.dimen.photoeditor_original_text_margin);
        this.mOriginalTextSize = res.getDimensionPixelSize(R.dimen.photoeditor_original_text_size);
        this.mBackgroundColor = res.getColor(R.color.background_screen);
        this.mOriginalText = res.getString(R.string.original_picture_text);
        this.mShadow = (NinePatchDrawable) res.getDrawable(R.drawable.geometry_shadow);
        setupGestureDetector(context);
        this.mActivity = (FilterShowActivity) context;
        if (sMask == null) {
            Bitmap mask = BitmapFactory.decodeResource(res, R.drawable.spot_mask);
            sMask = convertToAlphaMask(mask);
        }
        this.mEdgeEffect = new EdgeEffectCompat(context);
        this.mEdgeSize = res.getDimensionPixelSize(R.dimen.edge_glow_size);
    }

    public void attach() {
        MasterImage.getImage().addObserver(this);
        bindAsImageLoadListener();
        MasterImage.getImage().resetGeometryImages(false);
    }

    public void detach() {
        MasterImage.getImage().removeObserver(this);
        this.mMaskPaint.reset();
    }

    public void setupGestureDetector(Context context) {
        this.mGestureDetector = new GestureDetector(context, this);
        this.mScaleGestureDetector = new ScaleGestureDetector(context, this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(parentWidth, parentHeight);
    }

    public ImageFilter getCurrentFilter() {
        return MasterImage.getImage().getCurrentFilter();
    }

    protected Matrix getImageToScreenMatrix(boolean reflectRotation) {
        MasterImage master = MasterImage.getImage();
        if (master.getOriginalBounds() == null) {
            return new Matrix();
        }
        Matrix m = GeometryMathUtils.getImageToScreenMatrix(master.getPreset().getGeometryFilters(), reflectRotation, master.getOriginalBounds(), getWidth(), getHeight());
        Point translate = master.getTranslation();
        float scaleFactor = master.getScaleFactor();
        m.postTranslate(translate.x, translate.y);
        m.postScale(scaleFactor, scaleFactor, getWidth() / 2.0f, getHeight() / 2.0f);
        return m;
    }

    protected Matrix getScreenToImageMatrix(boolean reflectRotation) {
        Matrix m = getImageToScreenMatrix(reflectRotation);
        Matrix invert = new Matrix();
        m.invert(invert);
        return invert;
    }

    public ImagePreset getImagePreset() {
        return MasterImage.getImage().getPreset();
    }

    @Override
    public void onDraw(Canvas canvas) {
        this.mPaint.reset();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setFilterBitmap(true);
        MasterImage.getImage().setImageShowSize(getWidth() - (this.mShadowMargin * 2), getHeight() - (this.mShadowMargin * 2));
        MasterImage img = MasterImage.getImage();
        if (this.mActivity.isLoadingVisible() && getFilteredImage() != null) {
            if (img.getLoadedPreset() == null || (img.getLoadedPreset() != null && img.getLoadedPreset().equals(img.getCurrentPreset()))) {
                this.mActivity.stopLoadingIndicator();
            } else if (img.getLoadedPreset() != null) {
                return;
            }
            this.mActivity.stopLoadingIndicator();
        }
        canvas.save();
        this.mShadowDrawn = false;
        Bitmap highresPreview = MasterImage.getImage().getHighresImage();
        Bitmap fullHighres = MasterImage.getImage().getPartialImage();
        boolean isDoingNewLookAnimation = MasterImage.getImage().onGoingNewLookAnimation();
        if (highresPreview == null || isDoingNewLookAnimation) {
            drawImageAndAnimate(canvas, getFilteredImage());
        } else {
            drawImageAndAnimate(canvas, highresPreview);
        }
        drawHighresImage(canvas, fullHighres);
        drawCompareImage(canvas, getGeometryOnlyImage());
        canvas.restore();
        if (!this.mEdgeEffect.isFinished()) {
            canvas.save();
            float dx = (getHeight() - getWidth()) / 2.0f;
            if (getWidth() > getHeight()) {
                dx = (-(getWidth() - getHeight())) / 2.0f;
            }
            if (this.mCurrentEdgeEffect == 4) {
                canvas.rotate(180.0f, getWidth() / 2, getHeight() / 2);
            } else if (this.mCurrentEdgeEffect == 3) {
                canvas.rotate(90.0f, getWidth() / 2, getHeight() / 2);
                canvas.translate(0.0f, dx);
            } else if (this.mCurrentEdgeEffect == 1) {
                canvas.rotate(270.0f, getWidth() / 2, getHeight() / 2);
                canvas.translate(0.0f, dx);
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

    private void drawHighresImage(Canvas canvas, Bitmap fullHighres) {
        Matrix originalToScreen = MasterImage.getImage().originalImageToScreen();
        if (fullHighres != null && originalToScreen != null) {
            Matrix screenToOriginal = new Matrix();
            originalToScreen.invert(screenToOriginal);
            Rect rBounds = new Rect();
            rBounds.set(MasterImage.getImage().getPartialBounds());
            if (fullHighres != null) {
                originalToScreen.preTranslate(rBounds.left, rBounds.top);
                canvas.clipRect(this.mImageBounds);
                canvas.drawBitmap(fullHighres, originalToScreen, this.mPaint);
            }
        }
    }

    public void resetImageCaches(ImageShow caller) {
        MasterImage.getImage().invalidatePreview();
    }

    public Bitmap getGeometryOnlyImage() {
        return MasterImage.getImage().getGeometryOnlyImage();
    }

    public Bitmap getFilteredImage() {
        return MasterImage.getImage().getFilteredImage();
    }

    public void drawImageAndAnimate(Canvas canvas, Bitmap image) {
        if (image != null) {
            MasterImage master = MasterImage.getImage();
            Matrix m = master.computeImageToScreen(image, 0.0f, false);
            if (m != null) {
                canvas.save();
                RectF d = new RectF(0.0f, 0.0f, image.getWidth(), image.getHeight());
                m.mapRect(d);
                d.roundOut(this.mImageBounds);
                boolean showAnimatedImage = master.onGoingNewLookAnimation();
                if (!showAnimatedImage && this.mDidStartAnimation) {
                    if (master.getPreset().equals(master.getCurrentPreset())) {
                        this.mDidStartAnimation = false;
                        MasterImage.getImage().resetAnimBitmap();
                    } else {
                        showAnimatedImage = true;
                    }
                } else if (showAnimatedImage) {
                    this.mDidStartAnimation = true;
                }
                if (showAnimatedImage) {
                    canvas.save();
                    Bitmap previousImage = master.getPreviousImage();
                    Matrix mp = master.computeImageToScreen(previousImage, 0.0f, false);
                    RectF dp = new RectF(0.0f, 0.0f, previousImage.getWidth(), previousImage.getHeight());
                    mp.mapRect(dp);
                    Rect previousBounds = new Rect();
                    dp.roundOut(previousBounds);
                    float centerX = dp.centerX();
                    float centerY = dp.centerY();
                    boolean needsToDrawImage = true;
                    if (master.getCurrentLookAnimation() == 1) {
                        float maskScale = MasterImage.getImage().getMaskScale();
                        if (maskScale >= 0.0f) {
                            float maskW = sMask.getWidth() / 2.0f;
                            float maskH = sMask.getHeight() / 2.0f;
                            Point point = this.mActivity.hintTouchPoint(this);
                            float maxMaskScale = (Math.max(getWidth(), getHeight()) * 2) / Math.min(maskW, maskH);
                            float maskScale2 = maskScale * maxMaskScale;
                            float x = point.x - (maskW * maskScale2);
                            float y = point.y - (maskH * maskScale2);
                            this.mShaderMatrix.reset();
                            this.mShaderMatrix.setScale(1.0f / maskScale2, 1.0f / maskScale2);
                            this.mShaderMatrix.preTranslate((-x) + this.mImageBounds.left, (-y) + this.mImageBounds.top);
                            float scaleImageX = this.mImageBounds.width() / image.getWidth();
                            float scaleImageY = this.mImageBounds.height() / image.getHeight();
                            this.mShaderMatrix.preScale(scaleImageX, scaleImageY);
                            this.mMaskPaint.reset();
                            Shader maskShader = createShader(image);
                            maskShader.setLocalMatrix(this.mShaderMatrix);
                            this.mMaskPaint.setShader(maskShader);
                            drawShadow(canvas, this.mImageBounds);
                            canvas.drawBitmap(previousImage, m, this.mPaint);
                            canvas.clipRect(this.mImageBounds);
                            canvas.translate(x, y);
                            canvas.scale(maskScale2, maskScale2);
                            canvas.drawBitmap(sMask, 0.0f, 0.0f, this.mMaskPaint);
                            needsToDrawImage = false;
                        }
                    } else if (master.getCurrentLookAnimation() == 2) {
                        Rect d1 = computeImageBounds(master.getPreviousImage().getHeight(), master.getPreviousImage().getWidth());
                        Rect d2 = computeImageBounds(master.getPreviousImage().getWidth(), master.getPreviousImage().getHeight());
                        float finalScale = (1.0f * (1.0f - master.getAnimFraction())) + (master.getAnimFraction() * (d1.width() / d2.height()));
                        canvas.rotate(master.getAnimRotationValue(), centerX, centerY);
                        canvas.scale(finalScale, finalScale, centerX, centerY);
                    } else if (master.getCurrentLookAnimation() == 3 && (master.getCurrentFilterRepresentation() instanceof FilterMirrorRepresentation)) {
                        FilterMirrorRepresentation rep = (FilterMirrorRepresentation) master.getCurrentFilterRepresentation();
                        ImagePreset preset = master.getPreset();
                        ArrayList<FilterRepresentation> geometry = (ArrayList) preset.getGeometryFilters();
                        GeometryMathUtils.GeometryHolder holder = GeometryMathUtils.unpackGeometry(geometry);
                        if (holder.rotation.value() == 90 || holder.rotation.value() == 270) {
                            if (rep.isHorizontal() && !rep.isVertical()) {
                                canvas.scale(1.0f, master.getAnimRotationValue(), centerX, centerY);
                            } else if (rep.isVertical() && !rep.isHorizontal()) {
                                canvas.scale(1.0f, master.getAnimRotationValue(), centerX, centerY);
                            } else if (!rep.isHorizontal() || rep.isVertical()) {
                                canvas.scale(master.getAnimRotationValue(), 1.0f, centerX, centerY);
                            } else {
                                canvas.scale(master.getAnimRotationValue(), 1.0f, centerX, centerY);
                            }
                        } else if (rep.isHorizontal() && !rep.isVertical()) {
                            canvas.scale(master.getAnimRotationValue(), 1.0f, centerX, centerY);
                        } else if (rep.isVertical() && !rep.isHorizontal()) {
                            canvas.scale(master.getAnimRotationValue(), 1.0f, centerX, centerY);
                        } else if (!rep.isHorizontal() || rep.isVertical()) {
                            canvas.scale(1.0f, master.getAnimRotationValue(), centerX, centerY);
                        } else {
                            canvas.scale(1.0f, master.getAnimRotationValue(), centerX, centerY);
                        }
                    }
                    if (needsToDrawImage) {
                        drawShadow(canvas, previousBounds);
                        canvas.drawBitmap(previousImage, mp, this.mPaint);
                    }
                    canvas.restore();
                } else {
                    drawShadow(canvas, this.mImageBounds);
                    canvas.drawBitmap(image, m, this.mPaint);
                }
                canvas.restore();
            }
        }
    }

    private Rect computeImageBounds(int imageWidth, int imageHeight) {
        float scale = GeometryMathUtils.scale(imageWidth, imageHeight, getWidth(), getHeight());
        float w = imageWidth * scale;
        float h = imageHeight * scale;
        float ty = (getHeight() - h) / 2.0f;
        float tx = (getWidth() - w) / 2.0f;
        return new Rect(((int) tx) + this.mShadowMargin, ((int) ty) + this.mShadowMargin, ((int) (w + tx)) - this.mShadowMargin, ((int) (h + ty)) - this.mShadowMargin);
    }

    private void drawShadow(Canvas canvas, Rect d) {
        if (!this.mShadowDrawn) {
            this.mShadowBounds.set(d.left - this.mShadowMargin, d.top - this.mShadowMargin, d.right + this.mShadowMargin, d.bottom + this.mShadowMargin);
            this.mShadow.setBounds(this.mShadowBounds);
            this.mShadow.draw(canvas);
            this.mShadowDrawn = true;
        }
    }

    public void drawCompareImage(Canvas canvas, Bitmap image) {
        int px;
        int py;
        MasterImage master = MasterImage.getImage();
        boolean showsOriginal = master.showsOriginal();
        if (showsOriginal || this.mTouchShowOriginal) {
            canvas.save();
            if (image != null) {
                if (this.mShowOriginalDirection == 0) {
                    if (Math.abs(this.mTouch.y - this.mTouchDown.y) > Math.abs(this.mTouch.x - this.mTouchDown.x)) {
                        this.mShowOriginalDirection = UNVEIL_VERTICAL;
                    } else {
                        this.mShowOriginalDirection = UNVEIL_HORIZONTAL;
                    }
                }
                if (this.mShowOriginalDirection == UNVEIL_VERTICAL) {
                    px = this.mImageBounds.width();
                    py = this.mTouch.y - this.mImageBounds.top;
                } else {
                    px = this.mTouch.x - this.mImageBounds.left;
                    py = this.mImageBounds.height();
                    if (showsOriginal) {
                        px = this.mImageBounds.width();
                    }
                }
                Rect d = new Rect(this.mImageBounds.left, this.mImageBounds.top, this.mImageBounds.left + px, this.mImageBounds.top + py);
                if (this.mShowOriginalDirection == UNVEIL_HORIZONTAL) {
                    if (this.mTouchDown.x - this.mTouch.x > 0) {
                        d.set(this.mImageBounds.left + px, this.mImageBounds.top, this.mImageBounds.right, this.mImageBounds.top + py);
                    }
                } else if (this.mTouchDown.y - this.mTouch.y > 0) {
                    d.set(this.mImageBounds.left, this.mImageBounds.top + py, this.mImageBounds.left + px, this.mImageBounds.bottom);
                }
                canvas.clipRect(d);
                Matrix m = master.computeImageToScreen(image, 0.0f, false);
                canvas.drawBitmap(image, m, this.mPaint);
                Paint paint = new Paint();
                paint.setColor(-16777216);
                paint.setStrokeWidth(3.0f);
                if (this.mShowOriginalDirection == UNVEIL_VERTICAL) {
                    canvas.drawLine(this.mImageBounds.left, this.mTouch.y, this.mImageBounds.right, this.mTouch.y, paint);
                } else {
                    canvas.drawLine(this.mTouch.x, this.mImageBounds.top, this.mTouch.x, this.mImageBounds.bottom, paint);
                }
                Rect bounds = new Rect();
                paint.setAntiAlias(true);
                paint.setTextSize(this.mOriginalTextSize);
                paint.getTextBounds(this.mOriginalText, 0, this.mOriginalText.length(), bounds);
                paint.setColor(-16777216);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3.0f);
                canvas.drawText(this.mOriginalText, this.mImageBounds.left + this.mOriginalTextMargin, this.mImageBounds.top + bounds.height() + this.mOriginalTextMargin, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setStrokeWidth(1.0f);
                paint.setColor(-1);
                canvas.drawText(this.mOriginalText, this.mImageBounds.left + this.mOriginalTextMargin, this.mImageBounds.top + bounds.height() + this.mOriginalTextMargin, paint);
            }
            canvas.restore();
        }
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
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        int action = event.getAction() & 255;
        this.mGestureDetector.onTouchEvent(event);
        boolean scaleInProgress = scaleInProgress();
        this.mScaleGestureDetector.onTouchEvent(event);
        if (this.mInteractionMode == InteractionMode.SCALE) {
            return true;
        }
        if (!scaleInProgress() && scaleInProgress) {
            this.mFinishedScalingOperation = true;
        }
        int ex = (int) event.getX();
        int ey = (int) event.getY();
        if (action == 0) {
            this.mInteractionMode = InteractionMode.MOVE;
            this.mTouchDown.x = ex;
            this.mTouchDown.y = ey;
            this.mTouchShowOriginalDate = System.currentTimeMillis();
            this.mShowOriginalDirection = 0;
            MasterImage.getImage().setOriginalTranslation(MasterImage.getImage().getTranslation());
        }
        if (action == 2 && this.mInteractionMode == InteractionMode.MOVE) {
            this.mTouch.x = ex;
            this.mTouch.y = ey;
            float scaleFactor = MasterImage.getImage().getScaleFactor();
            if (scaleFactor > 1.0f) {
                float translateX = (this.mTouch.x - this.mTouchDown.x) / scaleFactor;
                float translateY = (this.mTouch.y - this.mTouchDown.y) / scaleFactor;
                Point originalTranslation = MasterImage.getImage().getOriginalTranslation();
                Point translation = MasterImage.getImage().getTranslation();
                translation.x = (int) (originalTranslation.x + translateX);
                translation.y = (int) (originalTranslation.y + translateY);
                MasterImage.getImage().setTranslation(translation);
                this.mTouchShowOriginal = false;
            } else if (enableComparison() && !this.mOriginalDisabled && System.currentTimeMillis() - this.mTouchShowOriginalDate > 200 && event.getPointerCount() == 1) {
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
        float scaleFactor2 = MasterImage.getImage().getScaleFactor();
        Point translation2 = MasterImage.getImage().getTranslation();
        constrainTranslation(translation2, scaleFactor2);
        MasterImage.getImage().setTranslation(translation2);
        invalidate();
        return true;
    }

    private void startAnimTranslation(int fromX, int toX, int fromY, int toY, int delay) {
        if (fromX != toX || fromY != toY) {
            if (this.mAnimatorTranslateX != null) {
                this.mAnimatorTranslateX.cancel();
            }
            if (this.mAnimatorTranslateY != null) {
                this.mAnimatorTranslateY.cancel();
            }
            this.mAnimatorTranslateX = ValueAnimator.ofInt(fromX, toX);
            this.mAnimatorTranslateY = ValueAnimator.ofInt(fromY, toY);
            this.mAnimatorTranslateX.setDuration(delay);
            this.mAnimatorTranslateY.setDuration(delay);
            this.mAnimatorTranslateX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    Point translation = MasterImage.getImage().getTranslation();
                    translation.x = ((Integer) animation.getAnimatedValue()).intValue();
                    MasterImage.getImage().setTranslation(translation);
                    ImageShow.this.invalidate();
                }
            });
            this.mAnimatorTranslateY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    Point translation = MasterImage.getImage().getTranslation();
                    translation.y = ((Integer) animation.getAnimatedValue()).intValue();
                    MasterImage.getImage().setTranslation(translation);
                    ImageShow.this.invalidate();
                }
            });
            this.mAnimatorTranslateX.start();
            this.mAnimatorTranslateY.start();
        }
    }

    private void applyTranslationConstraints() {
        float scaleFactor = MasterImage.getImage().getScaleFactor();
        Point translation = MasterImage.getImage().getTranslation();
        int x = translation.x;
        int y = translation.y;
        constrainTranslation(translation, scaleFactor);
        if (x != translation.x || y != translation.y) {
            startAnimTranslation(x, translation.x, y, translation.y, 200);
        }
    }

    protected boolean enableComparison() {
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent arg0) {
        this.mZoomIn = !this.mZoomIn;
        float scale = 1.0f;
        float x = arg0.getX();
        float y = arg0.getY();
        if (this.mZoomIn) {
            scale = MasterImage.getImage().getMaxScaleFactor();
        }
        if (scale != MasterImage.getImage().getScaleFactor()) {
            if (this.mAnimatorScale != null) {
                this.mAnimatorScale.cancel();
            }
            this.mAnimatorScale = ValueAnimator.ofFloat(MasterImage.getImage().getScaleFactor(), scale);
            float translateX = (getWidth() / 2) - x;
            float translateY = (getHeight() / 2) - y;
            Point translation = MasterImage.getImage().getTranslation();
            int startTranslateX = translation.x;
            int startTranslateY = translation.y;
            if (scale != 1.0f) {
                translation.x = (int) (this.mOriginalTranslation.x + translateX);
                translation.y = (int) (this.mOriginalTranslation.y + translateY);
            } else {
                translation.x = 0;
                translation.y = 0;
            }
            constrainTranslation(translation, scale);
            startAnimTranslation(startTranslateX, translation.x, startTranslateY, translation.y, 400);
            this.mAnimatorScale.setDuration(400L);
            this.mAnimatorScale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    MasterImage.getImage().setScaleFactor(((Float) animation.getAnimatedValue()).floatValue());
                    ImageShow.this.invalidate();
                }
            });
            this.mAnimatorScale.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    ImageShow.this.applyTranslationConstraints();
                    MasterImage.getImage().needsUpdatePartialPreview();
                    ImageShow.this.invalidate();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            this.mAnimatorScale.start();
        }
        return true;
    }

    private void constrainTranslation(Point translation, float scale) {
        int currentEdgeEffect = 0;
        if (scale <= 1.0f) {
            this.mCurrentEdgeEffect = 0;
            this.mEdgeEffect.finish();
            return;
        }
        Matrix originalToScreen = MasterImage.getImage().originalImageToScreen();
        Rect originalBounds = MasterImage.getImage().getOriginalBounds();
        RectF screenPos = new RectF(originalBounds);
        originalToScreen.mapRect(screenPos);
        boolean rightConstraint = screenPos.right < ((float) (getWidth() - this.mShadowMargin));
        boolean leftConstraint = screenPos.left > ((float) this.mShadowMargin);
        boolean topConstraint = screenPos.top > ((float) this.mShadowMargin);
        boolean bottomConstraint = screenPos.bottom < ((float) (getHeight() - this.mShadowMargin));
        if (screenPos.width() > getWidth()) {
            if (rightConstraint && !leftConstraint) {
                float tx = screenPos.right - (translation.x * scale);
                translation.x = (int) (((getWidth() - this.mShadowMargin) - tx) / scale);
                currentEdgeEffect = 3;
            } else if (leftConstraint && !rightConstraint) {
                float tx2 = screenPos.left - (translation.x * scale);
                translation.x = (int) ((this.mShadowMargin - tx2) / scale);
                currentEdgeEffect = 1;
            }
        } else {
            float tx3 = screenPos.right - (translation.x * scale);
            float dx = ((getWidth() - (this.mShadowMargin * 2)) - screenPos.width()) / 2.0f;
            translation.x = (int) ((((getWidth() - this.mShadowMargin) - tx3) - dx) / scale);
        }
        if (screenPos.height() > getHeight()) {
            if (bottomConstraint && !topConstraint) {
                float ty = screenPos.bottom - (translation.y * scale);
                translation.y = (int) (((getHeight() - this.mShadowMargin) - ty) / scale);
                currentEdgeEffect = 4;
            } else if (topConstraint && !bottomConstraint) {
                float ty2 = screenPos.top - (translation.y * scale);
                translation.y = (int) ((this.mShadowMargin - ty2) / scale);
                currentEdgeEffect = 2;
            }
        } else {
            float ty3 = screenPos.bottom - (translation.y * scale);
            float dy = ((getHeight() - (this.mShadowMargin * 2)) - screenPos.height()) / 2.0f;
            translation.y = (int) ((((getHeight() - this.mShadowMargin) - ty3) - dy) / scale);
        }
        if (this.mCurrentEdgeEffect != currentEdgeEffect) {
            if (this.mCurrentEdgeEffect == 0 || currentEdgeEffect != 0) {
                this.mCurrentEdgeEffect = currentEdgeEffect;
                this.mEdgeEffect.finish();
            }
            this.mEdgeEffect.setSize(getWidth(), this.mEdgeSize);
        }
        if (currentEdgeEffect != 0) {
            this.mEdgeEffect.onPull(this.mEdgeSize);
        }
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent startEvent, MotionEvent endEvent, float arg2, float arg3) {
        return (this.mActivity == null || endEvent.getPointerCount() == 2) ? false : true;
    }

    @Override
    public void onLongPress(MotionEvent arg0) {
    }

    @Override
    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent arg0) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent arg0) {
        return false;
    }

    public void openUtilityPanel(LinearLayout accessoryViewList) {
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        MasterImage img = MasterImage.getImage();
        float scaleFactor = img.getScaleFactor() * detector.getScaleFactor();
        if (scaleFactor > MasterImage.getImage().getMaxScaleFactor()) {
            scaleFactor = MasterImage.getImage().getMaxScaleFactor();
        }
        if (scaleFactor < 1.0f) {
            scaleFactor = 1.0f;
        }
        MasterImage.getImage().setScaleFactor(scaleFactor);
        float scaleFactor2 = img.getScaleFactor();
        float focusx = detector.getFocusX();
        float focusy = detector.getFocusY();
        float translateX = (focusx - this.mStartFocusX) / scaleFactor2;
        float translateY = (focusy - this.mStartFocusY) / scaleFactor2;
        Point translation = MasterImage.getImage().getTranslation();
        translation.x = (int) (this.mOriginalTranslation.x + translateX);
        translation.y = (int) (this.mOriginalTranslation.y + translateY);
        MasterImage.getImage().setTranslation(translation);
        invalidate();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        Point pos = MasterImage.getImage().getTranslation();
        this.mOriginalTranslation.x = pos.x;
        this.mOriginalTranslation.y = pos.y;
        this.mOriginalScale = MasterImage.getImage().getScaleFactor();
        this.mStartFocusX = detector.getFocusX();
        this.mStartFocusY = detector.getFocusY();
        this.mInteractionMode = InteractionMode.SCALE;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
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
