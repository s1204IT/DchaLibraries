package com.android.gallery3d.filtershow.imageshow;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.cache.BitmapCache;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRotateRepresentation;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import com.android.gallery3d.filtershow.history.HistoryItem;
import com.android.gallery3d.filtershow.history.HistoryManager;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.pipeline.Buffer;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.pipeline.RenderingRequest;
import com.android.gallery3d.filtershow.pipeline.RenderingRequestCaller;
import com.android.gallery3d.filtershow.pipeline.SharedBuffer;
import com.android.gallery3d.filtershow.pipeline.SharedPreset;
import com.android.gallery3d.filtershow.state.StateAdapter;
import java.util.List;
import java.util.Vector;

public class MasterImage implements RenderingRequestCaller {
    private static MasterImage sMasterImage = null;
    private FilterRepresentation mCurrentFilterRepresentation;
    private List<ExifTag> mEXIF;
    private int mOrientation;
    private Rect mOriginalBounds;
    private boolean mShowsOriginal;
    private boolean DEBUG = false;
    private boolean mSupportsHighRes = false;
    private ImageFilter mCurrentFilter = null;
    private ImagePreset mPreset = null;
    private ImagePreset mLoadedPreset = null;
    private ImagePreset mGeometryOnlyPreset = null;
    private ImagePreset mFiltersOnlyPreset = null;
    private SharedBuffer mPreviewBuffer = new SharedBuffer();
    private SharedPreset mPreviewPreset = new SharedPreset();
    private Bitmap mOriginalBitmapSmall = null;
    private Bitmap mOriginalBitmapLarge = null;
    private Bitmap mOriginalBitmapHighres = null;
    private Bitmap mTemporaryThumbnail = null;
    private final Vector<ImageShow> mLoadListeners = new Vector<>();
    private Uri mUri = null;
    private int mZoomOrientation = 1;
    private Bitmap mGeometryOnlyBitmap = null;
    private Bitmap mFiltersOnlyBitmap = null;
    private Bitmap mPartialBitmap = null;
    private Bitmap mHighresBitmap = null;
    private Bitmap mPreviousImage = null;
    private int mShadowMargin = 15;
    private Rect mPartialBounds = new Rect();
    private ValueAnimator mAnimator = null;
    private float mMaskScale = 1.0f;
    private boolean mOnGoingNewLookAnimation = false;
    private float mAnimRotationValue = 0.0f;
    private float mCurrentAnimRotationStartValue = 0.0f;
    private float mAnimFraction = 0.0f;
    private int mCurrentLookAnimation = 0;
    private HistoryManager mHistory = null;
    private StateAdapter mState = null;
    private FilterShowActivity mActivity = null;
    private Vector<ImageShow> mObservers = new Vector<>();
    private float mScaleFactor = 1.0f;
    private float mMaxScaleFactor = 3.0f;
    private Point mTranslation = new Point();
    private Point mOriginalTranslation = new Point();
    private Point mImageShowSize = new Point();
    private BitmapCache mBitmapCache = new BitmapCache();
    private Runnable mWarnListenersRunnable = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < MasterImage.this.mLoadListeners.size(); i++) {
                ImageShow imageShow = (ImageShow) MasterImage.this.mLoadListeners.elementAt(i);
                imageShow.imageLoaded();
            }
            MasterImage.this.invalidatePreview();
        }
    };

    private MasterImage() {
    }

    public static void setMaster(MasterImage master) {
        sMasterImage = master;
    }

    public static MasterImage getImage() {
        if (sMasterImage == null) {
            sMasterImage = new MasterImage();
        }
        return sMasterImage;
    }

    public Bitmap getOriginalBitmapSmall() {
        return this.mOriginalBitmapSmall;
    }

    public Bitmap getOriginalBitmapLarge() {
        return this.mOriginalBitmapLarge;
    }

    public Bitmap getOriginalBitmapHighres() {
        return this.mOriginalBitmapHighres == null ? this.mOriginalBitmapLarge : this.mOriginalBitmapHighres;
    }

    public void setOriginalBitmapHighres(Bitmap mOriginalBitmapHighres) {
        this.mOriginalBitmapHighres = mOriginalBitmapHighres;
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public Rect getOriginalBounds() {
        return this.mOriginalBounds;
    }

    public void setOriginalBounds(Rect r) {
        this.mOriginalBounds = r;
    }

    public Uri getUri() {
        return this.mUri;
    }

    public void setUri(Uri uri) {
        this.mUri = uri;
    }

    public int getZoomOrientation() {
        return this.mZoomOrientation;
    }

    public void addListener(ImageShow imageShow) {
        if (!this.mLoadListeners.contains(imageShow)) {
            this.mLoadListeners.add(imageShow);
        }
    }

    public void warnListeners() {
        this.mActivity.runOnUiThread(this.mWarnListenersRunnable);
    }

    public boolean loadBitmap(Uri uri, int size) {
        setUri(uri);
        this.mEXIF = ImageLoader.getExif(getActivity(), uri);
        this.mOrientation = ImageLoader.getMetadataOrientation(this.mActivity, uri);
        Rect originalBounds = new Rect();
        this.mOriginalBitmapLarge = ImageLoader.loadOrientedConstrainedBitmap(uri, this.mActivity, Math.min(900, size), this.mOrientation, originalBounds);
        setOriginalBounds(originalBounds);
        if (this.mOriginalBitmapLarge == null) {
            return false;
        }
        int sh = (int) ((160 * this.mOriginalBitmapLarge.getHeight()) / this.mOriginalBitmapLarge.getWidth());
        this.mOriginalBitmapSmall = Bitmap.createScaledBitmap(this.mOriginalBitmapLarge, 160, sh, true);
        this.mZoomOrientation = this.mOrientation;
        warnListeners();
        return true;
    }

    public void setSupportsHighRes(boolean value) {
        this.mSupportsHighRes = value;
    }

    public void addObserver(ImageShow observer) {
        if (!this.mObservers.contains(observer)) {
            this.mObservers.add(observer);
        }
    }

    public void removeObserver(ImageShow observer) {
        this.mObservers.remove(observer);
    }

    public void setActivity(FilterShowActivity activity) {
        this.mActivity = activity;
    }

    public FilterShowActivity getActivity() {
        return this.mActivity;
    }

    public synchronized ImagePreset getPreset() {
        return this.mPreset;
    }

    public synchronized void setPreset(ImagePreset preset, FilterRepresentation change, boolean addToHistory) {
        if (this.DEBUG) {
            preset.showFilters();
        }
        this.mPreset = preset;
        this.mPreset.fillImageStateAdapter(this.mState);
        if (addToHistory) {
            HistoryItem historyItem = new HistoryItem(this.mPreset, change);
            this.mHistory.addHistoryItem(historyItem);
        }
        updatePresets(true);
        resetGeometryImages(false);
        this.mActivity.updateCategories();
    }

    public void onHistoryItemClick(int position) {
        HistoryItem historyItem = this.mHistory.getItem(position);
        ImagePreset newPreset = new ImagePreset(historyItem.getImagePreset());
        setPreset(newPreset, historyItem.getFilterRepresentation(), false);
        this.mHistory.setCurrentPreset(position);
    }

    public HistoryManager getHistory() {
        return this.mHistory;
    }

    public StateAdapter getState() {
        return this.mState;
    }

    public void setHistoryManager(HistoryManager adapter) {
        this.mHistory = adapter;
    }

    public void setStateAdapter(StateAdapter adapter) {
        this.mState = adapter;
    }

    public void setCurrentFilter(ImageFilter filter) {
        this.mCurrentFilter = filter;
    }

    public ImageFilter getCurrentFilter() {
        return this.mCurrentFilter;
    }

    public synchronized boolean hasModifications() {
        boolean zHasModifications = false;
        synchronized (this) {
            ImagePreset loadedPreset = getLoadedPreset();
            if (this.mPreset == null) {
                if (loadedPreset != null) {
                    zHasModifications = loadedPreset.hasModifications();
                }
            } else if (loadedPreset == null) {
                zHasModifications = this.mPreset.hasModifications();
            } else if (!this.mPreset.equals(loadedPreset)) {
                zHasModifications = true;
            }
        }
        return zHasModifications;
    }

    public SharedBuffer getPreviewBuffer() {
        return this.mPreviewBuffer;
    }

    public SharedPreset getPreviewPreset() {
        return this.mPreviewPreset;
    }

    public Bitmap getFilteredImage() {
        this.mPreviewBuffer.swapConsumerIfNeeded();
        Buffer consumer = this.mPreviewBuffer.getConsumer();
        if (consumer != null) {
            return consumer.getBitmap();
        }
        return null;
    }

    public Bitmap getFiltersOnlyImage() {
        return this.mFiltersOnlyBitmap;
    }

    public Bitmap getGeometryOnlyImage() {
        return this.mGeometryOnlyBitmap;
    }

    public Bitmap getPartialImage() {
        return this.mPartialBitmap;
    }

    public Rect getPartialBounds() {
        return this.mPartialBounds;
    }

    public Bitmap getHighresImage() {
        return this.mHighresBitmap == null ? getFilteredImage() : this.mHighresBitmap;
    }

    public Bitmap getPreviousImage() {
        return this.mPreviousImage;
    }

    public ImagePreset getCurrentPreset() {
        return getPreviewBuffer().getConsumer().getPreset();
    }

    public float getMaskScale() {
        return this.mMaskScale;
    }

    public void setMaskScale(float scale) {
        this.mMaskScale = scale;
        notifyObservers();
    }

    public float getAnimRotationValue() {
        return this.mAnimRotationValue;
    }

    public void setAnimRotation(float rotation) {
        this.mAnimRotationValue = this.mCurrentAnimRotationStartValue + rotation;
        notifyObservers();
    }

    public void setAnimFraction(float fraction) {
        this.mAnimFraction = fraction;
    }

    public float getAnimFraction() {
        return this.mAnimFraction;
    }

    public boolean onGoingNewLookAnimation() {
        return this.mOnGoingNewLookAnimation;
    }

    public int getCurrentLookAnimation() {
        return this.mCurrentLookAnimation;
    }

    public void resetAnimBitmap() {
        this.mBitmapCache.cache(this.mPreviousImage);
        this.mPreviousImage = null;
    }

    public void onNewLook(FilterRepresentation newRepresentation) {
        if (getFilteredImage() != null) {
            if (this.mAnimator != null) {
                this.mAnimator.cancel();
                if (this.mCurrentLookAnimation == 2) {
                    this.mCurrentAnimRotationStartValue += 90.0f;
                }
            } else {
                resetAnimBitmap();
                this.mPreviousImage = this.mBitmapCache.getBitmapCopy(getFilteredImage(), 2);
            }
            if (newRepresentation instanceof FilterUserPresetRepresentation) {
                this.mCurrentLookAnimation = 1;
                this.mAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
                this.mAnimator.setDuration(650L);
            }
            if (newRepresentation instanceof FilterRotateRepresentation) {
                this.mCurrentLookAnimation = 2;
                this.mAnimator = ValueAnimator.ofFloat(0.0f, 90.0f);
                this.mAnimator.setDuration(500L);
            }
            if (newRepresentation instanceof FilterMirrorRepresentation) {
                this.mCurrentLookAnimation = 3;
                this.mAnimator = ValueAnimator.ofFloat(1.0f, 0.0f, -1.0f);
                this.mAnimator.setDuration(500L);
            }
            this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (MasterImage.this.mCurrentLookAnimation != 1) {
                        if (MasterImage.this.mCurrentLookAnimation == 2 || MasterImage.this.mCurrentLookAnimation == 3) {
                            MasterImage.this.setAnimRotation(((Float) animation.getAnimatedValue()).floatValue());
                            MasterImage.this.setAnimFraction(animation.getAnimatedFraction());
                            return;
                        }
                        return;
                    }
                    MasterImage.this.setMaskScale(((Float) animation.getAnimatedValue()).floatValue());
                }
            });
            this.mAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    MasterImage.this.mOnGoingNewLookAnimation = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    MasterImage.this.mOnGoingNewLookAnimation = false;
                    MasterImage.this.mCurrentAnimRotationStartValue = 0.0f;
                    MasterImage.this.mAnimator = null;
                    MasterImage.this.notifyObservers();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            this.mAnimator.start();
            notifyObservers();
        }
    }

    public void notifyObservers() {
        for (ImageShow observer : this.mObservers) {
            observer.invalidate();
        }
    }

    public void resetGeometryImages(boolean force) {
        if (this.mPreset != null) {
            ImagePreset newPresetGeometryOnly = new ImagePreset(this.mPreset);
            newPresetGeometryOnly.setDoApplyFilters(false);
            newPresetGeometryOnly.setDoApplyGeometry(true);
            if (force || this.mGeometryOnlyPreset == null || !newPresetGeometryOnly.equals(this.mGeometryOnlyPreset)) {
                this.mGeometryOnlyPreset = newPresetGeometryOnly;
                RenderingRequest.post(this.mActivity, null, this.mGeometryOnlyPreset, 2, this);
            }
            ImagePreset newPresetFiltersOnly = new ImagePreset(this.mPreset);
            newPresetFiltersOnly.setDoApplyFilters(true);
            newPresetFiltersOnly.setDoApplyGeometry(false);
            if (force || this.mFiltersOnlyPreset == null || !newPresetFiltersOnly.same(this.mFiltersOnlyPreset)) {
                this.mFiltersOnlyPreset = newPresetFiltersOnly;
                RenderingRequest.post(this.mActivity, null, this.mFiltersOnlyPreset, 1, this);
            }
        }
    }

    public void updatePresets(boolean force) {
        invalidatePreview();
    }

    public FilterRepresentation getCurrentFilterRepresentation() {
        return this.mCurrentFilterRepresentation;
    }

    public void setCurrentFilterRepresentation(FilterRepresentation currentFilterRepresentation) {
        this.mCurrentFilterRepresentation = currentFilterRepresentation;
    }

    public void invalidateFiltersOnly() {
        this.mFiltersOnlyPreset = null;
        invalidatePreview();
    }

    public void invalidatePartialPreview() {
        if (this.mPartialBitmap != null) {
            this.mBitmapCache.cache(this.mPartialBitmap);
            this.mPartialBitmap = null;
            notifyObservers();
        }
    }

    public void invalidateHighresPreview() {
        if (this.mHighresBitmap != null) {
            this.mBitmapCache.cache(this.mHighresBitmap);
            this.mHighresBitmap = null;
            notifyObservers();
        }
    }

    public void invalidatePreview() {
        if (this.mPreset != null) {
            this.mPreviewPreset.enqueuePreset(this.mPreset);
            this.mPreviewBuffer.invalidate();
            invalidatePartialPreview();
            invalidateHighresPreview();
            needsUpdatePartialPreview();
            needsUpdateHighResPreview();
            this.mActivity.getProcessingService().updatePreviewBuffer();
        }
    }

    public void setImageShowSize(int w, int h) {
        if (this.mImageShowSize.x != w || this.mImageShowSize.y != h) {
            this.mImageShowSize.set(w, h);
            float maxWidth = this.mOriginalBounds.width() / w;
            float maxHeight = this.mOriginalBounds.height() / h;
            this.mMaxScaleFactor = Math.max(3.0f, Math.max(maxWidth, maxHeight));
            needsUpdatePartialPreview();
            needsUpdateHighResPreview();
        }
    }

    public Matrix originalImageToScreen() {
        return computeImageToScreen(null, 0.0f, true);
    }

    public Matrix computeImageToScreen(Bitmap bitmapToDraw, float rotate, boolean applyGeometry) {
        Matrix m;
        if (getOriginalBounds() == null || this.mImageShowSize.x == 0 || this.mImageShowSize.y == 0) {
            return null;
        }
        float scale = 1.0f;
        float translateX = 0.0f;
        float translateY = 0.0f;
        if (applyGeometry) {
            GeometryMathUtils.GeometryHolder holder = GeometryMathUtils.unpackGeometry(this.mPreset.getGeometryFilters());
            m = GeometryMathUtils.getCropSelectionToScreenMatrix(null, holder, getOriginalBounds().width(), getOriginalBounds().height(), this.mImageShowSize.x, this.mImageShowSize.y);
        } else if (bitmapToDraw != null) {
            m = new Matrix();
            RectF size = new RectF(0.0f, 0.0f, bitmapToDraw.getWidth(), bitmapToDraw.getHeight());
            scale = this.mImageShowSize.x / size.width();
            if (size.width() < size.height()) {
                scale = this.mImageShowSize.y / size.height();
            }
            translateX = (this.mImageShowSize.x - (size.width() * scale)) / 2.0f;
            translateY = (this.mImageShowSize.y - (size.height() * scale)) / 2.0f;
        } else {
            return null;
        }
        Point translation = getTranslation();
        m.postScale(scale, scale);
        m.postRotate(rotate, this.mImageShowSize.x / 2.0f, this.mImageShowSize.y / 2.0f);
        m.postTranslate(translateX, translateY);
        m.postTranslate(this.mShadowMargin, this.mShadowMargin);
        m.postScale(getScaleFactor(), getScaleFactor(), this.mImageShowSize.x / 2.0f, this.mImageShowSize.y / 2.0f);
        m.postTranslate(translation.x * getScaleFactor(), translation.y * getScaleFactor());
        return m;
    }

    public void needsUpdateHighResPreview() {
        if (this.mSupportsHighRes && this.mActivity.getProcessingService() != null && this.mPreset != null) {
            this.mActivity.getProcessingService().postHighresRenderingRequest(this.mPreset, getScaleFactor(), this);
            invalidateHighresPreview();
        }
    }

    public void needsUpdatePartialPreview() {
        if (this.mPreset != null) {
            if (!this.mPreset.canDoPartialRendering()) {
                invalidatePartialPreview();
                return;
            }
            Matrix originalToScreen = getImage().originalImageToScreen();
            if (originalToScreen != null) {
                Matrix screenToOriginal = new Matrix();
                originalToScreen.invert(screenToOriginal);
                RectF bounds = new RectF(0.0f, 0.0f, this.mImageShowSize.x + (this.mShadowMargin * 2), this.mImageShowSize.y + (this.mShadowMargin * 2));
                screenToOriginal.mapRect(bounds);
                Rect rBounds = new Rect();
                bounds.roundOut(rBounds);
                this.mActivity.getProcessingService().postFullresRenderingRequest(this.mPreset, getScaleFactor(), rBounds, new Rect(0, 0, this.mImageShowSize.x, this.mImageShowSize.y), this);
                invalidatePartialPreview();
            }
        }
    }

    @Override
    public void available(RenderingRequest request) {
        if (request.getBitmap() != null) {
            boolean needsCheckModification = false;
            if (request.getType() == 2) {
                this.mBitmapCache.cache(this.mGeometryOnlyBitmap);
                this.mGeometryOnlyBitmap = request.getBitmap();
                needsCheckModification = true;
            }
            if (request.getType() == 1) {
                this.mBitmapCache.cache(this.mFiltersOnlyBitmap);
                this.mFiltersOnlyBitmap = request.getBitmap();
                notifyObservers();
                needsCheckModification = true;
            }
            if (request.getType() == 4 && request.getScaleFactor() == getScaleFactor()) {
                this.mBitmapCache.cache(this.mPartialBitmap);
                this.mPartialBitmap = request.getBitmap();
                this.mPartialBounds.set(request.getBounds());
                notifyObservers();
                needsCheckModification = true;
            }
            if (request.getType() == 5) {
                this.mBitmapCache.cache(this.mHighresBitmap);
                this.mHighresBitmap = request.getBitmap();
                notifyObservers();
                needsCheckModification = true;
            }
            if (needsCheckModification) {
                this.mActivity.enableSave(hasModifications());
            }
        }
    }

    public static void reset() {
        sMasterImage = null;
    }

    public float getScaleFactor() {
        return this.mScaleFactor;
    }

    public void setScaleFactor(float scaleFactor) {
        if (scaleFactor != this.mScaleFactor) {
            this.mScaleFactor = scaleFactor;
            invalidatePartialPreview();
        }
    }

    public Point getTranslation() {
        return this.mTranslation;
    }

    public void setTranslation(Point translation) {
        this.mTranslation.x = translation.x;
        this.mTranslation.y = translation.y;
        needsUpdatePartialPreview();
    }

    public Point getOriginalTranslation() {
        return this.mOriginalTranslation;
    }

    public void setOriginalTranslation(Point originalTranslation) {
        this.mOriginalTranslation.x = originalTranslation.x;
        this.mOriginalTranslation.y = originalTranslation.y;
    }

    public void resetTranslation() {
        this.mTranslation.x = 0;
        this.mTranslation.y = 0;
        needsUpdatePartialPreview();
    }

    public Bitmap getTemporaryThumbnailBitmap() {
        if (this.mTemporaryThumbnail == null && getOriginalBitmapSmall() != null) {
            this.mTemporaryThumbnail = getOriginalBitmapSmall().copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(this.mTemporaryThumbnail);
            canvas.drawARGB(200, 80, 80, 80);
        }
        return this.mTemporaryThumbnail;
    }

    public Bitmap getThumbnailBitmap() {
        return getOriginalBitmapSmall();
    }

    public Bitmap getLargeThumbnailBitmap() {
        return getOriginalBitmapLarge();
    }

    public float getMaxScaleFactor() {
        return this.mMaxScaleFactor;
    }

    public boolean supportsHighRes() {
        return this.mSupportsHighRes;
    }

    public void setShowsOriginal(boolean value) {
        this.mShowsOriginal = value;
        notifyObservers();
    }

    public boolean showsOriginal() {
        return this.mShowsOriginal;
    }

    public void setLoadedPreset(ImagePreset preset) {
        this.mLoadedPreset = preset;
    }

    public ImagePreset getLoadedPreset() {
        return this.mLoadedPreset;
    }

    public List<ExifTag> getEXIF() {
        return this.mEXIF;
    }

    public BitmapCache getBitmapCache() {
        return this.mBitmapCache;
    }

    public boolean hasTinyPlanet() {
        return this.mPreset.contains((byte) 6);
    }
}
