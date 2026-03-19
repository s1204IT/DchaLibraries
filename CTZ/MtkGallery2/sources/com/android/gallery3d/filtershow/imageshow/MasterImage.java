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
import com.android.gallery3d.filtershow.filters.ImageFilterDraw;
import com.android.gallery3d.filtershow.history.HistoryItem;
import com.android.gallery3d.filtershow.history.HistoryManager;
import com.android.gallery3d.filtershow.pipeline.Buffer;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.pipeline.RenderingRequest;
import com.android.gallery3d.filtershow.pipeline.RenderingRequestCaller;
import com.android.gallery3d.filtershow.pipeline.SharedBuffer;
import com.android.gallery3d.filtershow.pipeline.SharedPreset;
import com.android.gallery3d.filtershow.state.StateAdapter;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class MasterImage implements RenderingRequestCaller {
    private FilterRepresentation mCurrentFilterRepresentation;
    private List<ExifTag> mEXIF;
    private int mOrientation;
    private Rect mOriginalBounds;
    private boolean mShowsOriginal;
    private static MasterImage sMasterImage = null;
    public static Rect sBoundsBackup = new Rect();
    public static volatile boolean sIsRenderFilters = false;
    public static final Object sLock = new Object();
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
                ((ImageShow) MasterImage.this.mLoadListeners.elementAt(i)).imageLoaded();
            }
            MasterImage.this.invalidatePreview();
        }
    };

    private MasterImage() {
    }

    public static void setMaster(MasterImage masterImage) {
        synchronized (sLock) {
            sMasterImage = masterImage;
        }
    }

    public static MasterImage getImage() {
        MasterImage masterImage;
        synchronized (sLock) {
            if (sMasterImage == null) {
                sMasterImage = new MasterImage();
            }
            masterImage = sMasterImage;
        }
        return masterImage;
    }

    public Bitmap getOriginalBitmapSmall() {
        return this.mOriginalBitmapSmall;
    }

    public Bitmap getOriginalBitmapLarge() {
        return this.mOriginalBitmapLarge;
    }

    public Bitmap getOriginalBitmapHighres() {
        if (this.mOriginalBitmapHighres == null) {
            return this.mOriginalBitmapLarge;
        }
        return this.mOriginalBitmapHighres;
    }

    public void setOriginalBitmapHighres(Bitmap bitmap) {
        this.mOriginalBitmapHighres = bitmap;
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public Rect getOriginalBounds() {
        return this.mOriginalBounds;
    }

    public void setOriginalBounds(Rect rect) {
        this.mOriginalBounds = rect;
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

    public boolean loadBitmap(Uri uri, int i) {
        setUri(uri);
        this.mEXIF = ImageLoader.getExif(getActivity(), uri);
        this.mOrientation = ImageLoader.getMetadataOrientation(this.mActivity, uri);
        Rect rect = new Rect();
        this.mOriginalBitmapLarge = ImageLoader.loadOrientedConstrainedBitmap(uri, this.mActivity, Math.min(900, i), this.mOrientation, rect);
        setOriginalBounds(rect);
        if (this.mOriginalBitmapLarge == null) {
            return false;
        }
        int height = (int) ((160 * this.mOriginalBitmapLarge.getHeight()) / this.mOriginalBitmapLarge.getWidth());
        if (height == 0) {
            height = 1;
        }
        this.mOriginalBitmapSmall = Bitmap.createScaledBitmap(this.mOriginalBitmapLarge, 160, height, true);
        this.mZoomOrientation = this.mOrientation;
        warnListeners();
        return true;
    }

    public void setSupportsHighRes(boolean z) {
        this.mSupportsHighRes = z;
    }

    public void addObserver(ImageShow imageShow) {
        if (this.mObservers.contains(imageShow)) {
            return;
        }
        this.mObservers.add(imageShow);
    }

    public void removeObserver(ImageShow imageShow) {
        this.mObservers.remove(imageShow);
    }

    public void setActivity(FilterShowActivity filterShowActivity) {
        this.mActivity = filterShowActivity;
    }

    public FilterShowActivity getActivity() {
        return this.mActivity;
    }

    public synchronized ImagePreset getPreset() {
        return this.mPreset;
    }

    public synchronized void setPreset(ImagePreset imagePreset, FilterRepresentation filterRepresentation, boolean z) {
        if (this.DEBUG) {
            imagePreset.showFilters();
        }
        this.mPreset = imagePreset;
        this.mPreset.fillImageStateAdapter(this.mState);
        if (z) {
            this.mHistory.addHistoryItem(new HistoryItem(this.mPreset, filterRepresentation));
        }
        updatePresets(true);
        resetGeometryImages(false);
        this.mActivity.updateCategories();
    }

    public void onHistoryItemClick(int i) {
        HistoryItem item = this.mHistory.getItem(i);
        if (item == null) {
            return;
        }
        setPreset(new ImagePreset(item.getImagePreset()), item.getFilterRepresentation(), false);
        this.mHistory.setCurrentPreset(i);
    }

    public HistoryManager getHistory() {
        return this.mHistory;
    }

    public StateAdapter getState() {
        return this.mState;
    }

    public void setHistoryManager(HistoryManager historyManager) {
        this.mHistory = historyManager;
    }

    public void setStateAdapter(StateAdapter stateAdapter) {
        this.mState = stateAdapter;
    }

    public void setCurrentFilter(ImageFilter imageFilter) {
        this.mCurrentFilter = imageFilter;
    }

    public ImageFilter getCurrentFilter() {
        return this.mCurrentFilter;
    }

    public synchronized boolean hasModifications() {
        ImagePreset loadedPreset = getLoadedPreset();
        if (this.mPreset == null) {
            if (loadedPreset == null) {
                return false;
            }
            return loadedPreset.hasModifications();
        }
        if (loadedPreset == null) {
            return this.mPreset.hasModifications();
        }
        return !this.mPreset.equals(loadedPreset);
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

    public Bitmap getHighresImage() {
        if (this.mHighresBitmap == null) {
            return getFilteredImage();
        }
        return this.mHighresBitmap;
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

    public void setMaskScale(float f) {
        this.mMaskScale = f;
        notifyObservers();
    }

    public float getAnimRotationValue() {
        return this.mAnimRotationValue;
    }

    public void setAnimRotation(float f) {
        this.mAnimRotationValue = this.mCurrentAnimRotationStartValue + f;
        notifyObservers();
    }

    public void setAnimFraction(float f) {
        this.mAnimFraction = f;
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

    public void onNewLook(FilterRepresentation filterRepresentation) {
        if (getFilteredImage() == null) {
            return;
        }
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
            if (this.mCurrentLookAnimation == 2 && (filterRepresentation instanceof FilterRotateRepresentation)) {
                this.mCurrentAnimRotationStartValue += 90.0f;
            }
        } else {
            resetAnimBitmap();
            this.mPreviousImage = this.mBitmapCache.getBitmapCopy(getFilteredImage(), 2);
        }
        if (filterRepresentation instanceof FilterUserPresetRepresentation) {
            this.mCurrentLookAnimation = 1;
            this.mAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
            this.mAnimator.setDuration(650L);
        }
        if (filterRepresentation instanceof FilterRotateRepresentation) {
            this.mCurrentLookAnimation = 2;
            this.mAnimator = ValueAnimator.ofFloat(0.0f, 90.0f);
            this.mAnimator.setDuration(500L);
        }
        if (filterRepresentation instanceof FilterMirrorRepresentation) {
            this.mCurrentLookAnimation = 3;
            this.mAnimator = ValueAnimator.ofFloat(1.0f, 0.0f, -1.0f);
            this.mAnimator.setDuration(500L);
        }
        this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (MasterImage.this.mCurrentLookAnimation != 1) {
                    if (MasterImage.this.mCurrentLookAnimation == 2 || MasterImage.this.mCurrentLookAnimation == 3) {
                        MasterImage.this.setAnimRotation(((Float) valueAnimator.getAnimatedValue()).floatValue());
                        MasterImage.this.setAnimFraction(valueAnimator.getAnimatedFraction());
                        return;
                    }
                    return;
                }
                MasterImage.this.setMaskScale(((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        });
        this.mAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                MasterImage.this.mOnGoingNewLookAnimation = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                MasterImage.this.mOnGoingNewLookAnimation = false;
                MasterImage.this.mCurrentAnimRotationStartValue = 0.0f;
                MasterImage.this.mAnimator = null;
                MasterImage.this.notifyObservers();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mAnimator.start();
        notifyObservers();
    }

    public void notifyObservers() {
        Iterator<ImageShow> it = this.mObservers.iterator();
        while (it.hasNext()) {
            it.next().invalidate();
        }
    }

    public void resetGeometryImages(boolean z) {
        if (this.mPreset == null) {
            return;
        }
        ImagePreset imagePreset = new ImagePreset(this.mPreset);
        imagePreset.setDoApplyFilters(false);
        imagePreset.setDoApplyGeometry(true);
        if (z || this.mGeometryOnlyPreset == null || !imagePreset.equals(this.mGeometryOnlyPreset)) {
            this.mGeometryOnlyPreset = imagePreset;
            RenderingRequest.post(this.mActivity, null, this.mGeometryOnlyPreset, 2, this);
        }
        ImagePreset imagePreset2 = new ImagePreset(this.mPreset);
        imagePreset2.setDoApplyFilters(true);
        imagePreset2.setDoApplyGeometry(false);
        if (z || this.mFiltersOnlyPreset == null || !imagePreset2.same(this.mFiltersOnlyPreset)) {
            this.mFiltersOnlyPreset = imagePreset2;
            RenderingRequest.post(this.mActivity, null, this.mFiltersOnlyPreset, 1, this);
        }
    }

    public void updatePresets(boolean z) {
        invalidatePreview();
    }

    public FilterRepresentation getCurrentFilterRepresentation() {
        return this.mCurrentFilterRepresentation;
    }

    public void setCurrentFilterRepresentation(FilterRepresentation filterRepresentation) {
        this.mCurrentFilterRepresentation = filterRepresentation;
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
        if (this.mPreset == null) {
            return;
        }
        this.mPreviewPreset.enqueuePreset(this.mPreset);
        this.mPreviewBuffer.invalidate();
        invalidatePartialPreview();
        invalidateHighresPreview();
        needsUpdatePartialPreview();
        needsUpdateHighResPreview();
        this.mActivity.getProcessingService().updatePreviewBuffer();
    }

    public void setImageShowSize(int i, int i2) {
        if (this.mImageShowSize.x != i || this.mImageShowSize.y != i2) {
            this.mImageShowSize.set(i, i2);
            this.mMaxScaleFactor = Math.max(3.0f, Math.max(this.mOriginalBounds.width() / i, this.mOriginalBounds.height() / i2));
            needsUpdatePartialPreview();
            needsUpdateHighResPreview();
        }
    }

    public Matrix originalImageToScreen() {
        return computeImageToScreen(null, 0.0f, true);
    }

    public Matrix computeImageToScreen(Bitmap bitmap, float f, boolean z) {
        Matrix cropSelectionToScreenMatrix;
        float f2;
        if (getOriginalBounds() == null || this.mImageShowSize.x == 0 || this.mImageShowSize.y == 0) {
            return null;
        }
        float f3 = 1.0f;
        float fWidth = 0.0f;
        if (z) {
            if (this.mPreset == null) {
                return null;
            }
            cropSelectionToScreenMatrix = GeometryMathUtils.getCropSelectionToScreenMatrix(null, GeometryMathUtils.unpackGeometry(this.mPreset.getGeometryFilters()), getOriginalBounds().width(), getOriginalBounds().height(), this.mImageShowSize.x, this.mImageShowSize.y);
            f2 = 0.0f;
        } else {
            if (bitmap == null) {
                return null;
            }
            Matrix matrix = new Matrix();
            RectF rectF = new RectF(0.0f, 0.0f, bitmap.getWidth(), bitmap.getHeight());
            float fWidth2 = this.mImageShowSize.x / rectF.width();
            if (rectF.width() < rectF.height()) {
                fWidth2 = this.mImageShowSize.y / rectF.height();
            }
            fWidth = (this.mImageShowSize.x - (rectF.width() * fWidth2)) / 2.0f;
            float fHeight = (this.mImageShowSize.y - (rectF.height() * fWidth2)) / 2.0f;
            f3 = fWidth2;
            cropSelectionToScreenMatrix = matrix;
            f2 = fHeight;
        }
        Point translation = getTranslation();
        cropSelectionToScreenMatrix.postScale(f3, f3);
        cropSelectionToScreenMatrix.postRotate(f, this.mImageShowSize.x / 2.0f, this.mImageShowSize.y / 2.0f);
        cropSelectionToScreenMatrix.postTranslate(fWidth, f2);
        cropSelectionToScreenMatrix.postTranslate(this.mShadowMargin, this.mShadowMargin);
        cropSelectionToScreenMatrix.postScale(getScaleFactor(), getScaleFactor(), this.mImageShowSize.x / 2.0f, this.mImageShowSize.y / 2.0f);
        cropSelectionToScreenMatrix.postTranslate(translation.x * getScaleFactor(), translation.y * getScaleFactor());
        return cropSelectionToScreenMatrix;
    }

    public void needsUpdateHighResPreview() {
        if (!this.mSupportsHighRes) {
            this.mActivity.enableSave(hasModifications());
        } else {
            if (this.mActivity.getProcessingService() == null || this.mPreset == null) {
                return;
            }
            this.mActivity.getProcessingService().postHighresRenderingRequest(this.mPreset, getScaleFactor(), this);
            invalidateHighresPreview();
        }
    }

    public void needsUpdatePartialPreview() {
        if (this.mPreset == null) {
            return;
        }
        if (!this.mPreset.canDoPartialRendering()) {
            invalidatePartialPreview();
            return;
        }
        Matrix matrixOriginalImageToScreen = getImage().originalImageToScreen();
        if (matrixOriginalImageToScreen == null) {
            return;
        }
        Matrix matrix = new Matrix();
        matrixOriginalImageToScreen.invert(matrix);
        RectF rectF = new RectF(0.0f, 0.0f, this.mImageShowSize.x + (this.mShadowMargin * 2), this.mImageShowSize.y + (2 * this.mShadowMargin));
        matrix.mapRect(rectF);
        Rect rect = new Rect();
        rectF.roundOut(rect);
        this.mActivity.getProcessingService().postFullresRenderingRequest(this.mPreset, getScaleFactor(), rect, new Rect(0, 0, this.mImageShowSize.x, this.mImageShowSize.y), this);
        invalidatePartialPreview();
    }

    @Override
    public void available(RenderingRequest renderingRequest) {
        boolean z;
        if (renderingRequest.getBitmap() == null) {
            return;
        }
        if (renderingRequest.getType() == 2) {
            this.mBitmapCache.cache(this.mGeometryOnlyBitmap);
            this.mGeometryOnlyBitmap = renderingRequest.getBitmap();
            z = true;
        } else {
            z = false;
        }
        if (renderingRequest.getType() == 1) {
            this.mBitmapCache.cache(this.mFiltersOnlyBitmap);
            this.mFiltersOnlyBitmap = renderingRequest.getBitmap();
            notifyObservers();
            z = true;
        }
        if (renderingRequest.getType() == 4 && renderingRequest.getScaleFactor() == getScaleFactor()) {
            this.mBitmapCache.cache(this.mPartialBitmap);
            this.mPartialBitmap = renderingRequest.getBitmap();
            this.mPartialBounds.set(renderingRequest.getBounds());
            notifyObservers();
            z = true;
        }
        if (renderingRequest.getType() == 5) {
            ImageFilterDraw.disableCache(false);
            this.mBitmapCache.cache(this.mHighresBitmap);
            this.mHighresBitmap = renderingRequest.getBitmap();
            notifyObservers();
            z = true;
        }
        if (z) {
            this.mActivity.enableSave(hasModifications());
        }
    }

    public static void reset() {
        sMasterImage = null;
    }

    public float getScaleFactor() {
        return this.mScaleFactor;
    }

    public void setScaleFactor(float f) {
        if (f == this.mScaleFactor) {
            return;
        }
        this.mScaleFactor = f;
        invalidatePartialPreview();
    }

    public Point getTranslation() {
        return this.mTranslation;
    }

    public void setTranslation(Point point) {
        this.mTranslation.x = point.x;
        this.mTranslation.y = point.y;
        needsUpdatePartialPreview();
    }

    public Point getOriginalTranslation() {
        return this.mOriginalTranslation;
    }

    public void setOriginalTranslation(Point point) {
        this.mOriginalTranslation.x = point.x;
        this.mOriginalTranslation.y = point.y;
    }

    public void resetTranslation() {
        this.mTranslation.x = 0;
        this.mTranslation.y = 0;
        needsUpdatePartialPreview();
    }

    public Bitmap getTemporaryThumbnailBitmap() {
        if (this.mTemporaryThumbnail == null && getOriginalBitmapSmall() != null) {
            this.mTemporaryThumbnail = getOriginalBitmapSmall().copy(Bitmap.Config.ARGB_8888, true);
            new Canvas(this.mTemporaryThumbnail).drawARGB(200, 80, 80, 80);
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

    public void setShowsOriginal(boolean z) {
        this.mShowsOriginal = z;
        notifyObservers();
    }

    public boolean showsOriginal() {
        return this.mShowsOriginal;
    }

    public void setLoadedPreset(ImagePreset imagePreset) {
        this.mLoadedPreset = imagePreset;
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

    public void removeListener(ImageShow imageShow) {
        this.mLoadListeners.remove(imageShow);
    }

    public void backupBounds() {
        sBoundsBackup = this.mOriginalBounds;
    }

    public Matrix originalImageToScreenWithRotation() {
        if (this.mOrientation == 6) {
            return computeImageToScreen(null, 90.0f, true);
        }
        if (this.mOrientation == 8) {
            return computeImageToScreen(null, 270.0f, true);
        }
        return computeImageToScreen(null, 0.0f, true);
    }
}
