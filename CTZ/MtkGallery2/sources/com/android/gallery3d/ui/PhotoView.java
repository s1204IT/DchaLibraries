package com.android.gallery3d.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.PhotoDataAdapter;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.RawTexture;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.StringTexture;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.GestureRecognizer;
import com.android.gallery3d.ui.PositionController;
import com.android.gallery3d.ui.TileImageView;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.RangeArray;
import com.android.gallery3d.util.UsageStatistics;
import com.mediatek.gallery3d.adapter.FeatureManager;
import com.mediatek.gallery3d.adapter.MGLRootView;
import com.mediatek.gallery3d.adapter.PhotoPlayFacade;
import com.mediatek.gallerybasic.base.IPhotoRenderer;
import com.mediatek.gallerybasic.base.LayerManager;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.PlayEngine;
import com.mediatek.gallerybasic.util.DebugUtils;
import mf.org.apache.xerces.dom3.as.ASContentModel;

public class PhotoView extends GLView implements PlayEngine.OnFrameAvailableListener {
    private static final boolean CARD_EFFECT = true;
    private static final float DEFAULT_TEXT_SIZE = 20.0f;
    private static final int HOLD_CAPTURE_ANIMATION = 2;
    private static final int HOLD_DELETE = 4;
    private static final int HOLD_TOUCH_DOWN = 1;
    private static final int ICON_RATIO = 6;
    public static final long INVALID_DATA_VERSION = -1;
    public static final int INVALID_SIZE = -1;
    private static final int MAX_DISMISS_VELOCITY = 2500;
    private static final int MSG_CANCEL_EXTRA_SCALING = 2;
    private static final int MSG_CAPTURE_ANIMATION_DONE = 4;
    private static final int MSG_DELETE_ANIMATION_DONE = 5;
    private static final int MSG_DELETE_DONE = 6;
    private static final int MSG_SWITCH_FOCUS = 3;
    private static final int MSG_UNDO_BAR_FULL_CAMERA = 8;
    private static final int MSG_UNDO_BAR_TIMEOUT = 7;
    private static final boolean OFFSET_EFFECT = true;
    public static final int SCREEN_NAIL_MAX = 3;
    private static final int SWIPE_ESCAPE_DISTANCE = 150;
    private static final int SWIPE_ESCAPE_VELOCITY = 500;
    private static final float SWIPE_THRESHOLD = 300.0f;
    private static final String TAG = "Gallery2/PhotoView";
    private static float TRANSITION_SCALE_FACTOR = 0.74f;
    private static final int UNDO_BAR_DELETE_LAST = 16;
    private static final int UNDO_BAR_FULL_CAMERA = 8;
    private static final int UNDO_BAR_SHOW = 1;
    private static final int UNDO_BAR_TIMEOUT = 2;
    private static final int UNDO_BAR_TOUCHED = 4;
    public static long sSetScreenNailTime;
    private int mActiveCount;
    private Activity mActivity;
    private boolean mCancelExtraScalingPending;
    private Context mContext;
    private EdgeView mEdgeView;
    private boolean mFullScreenCamera;
    private final MyGestureListener mGestureListener;
    private final GestureRecognizer mGestureRecognizer;
    private SynchronizedHandler mHandler;
    private int mHolding;
    private LayerManager mLayerManager;
    private Listener mListener;
    private Model mModel;
    private int mNextBound;
    private StringTexture mNoThumbnailText;
    private IPhotoRenderer[] mPhotoRenderExts;
    private final int mPlaceholderColor;
    private int mPlayCount;
    private PlayEngine mPlayEngine;
    private final PositionController mPositionController;
    private int mPrevBound;
    private TileImageView mTileView;
    private boolean mTouchBoxDeletable;
    private UndoBarView mUndoBar;
    private int mUndoBarState;
    private Texture mVideoPlayIcon;
    private ZInterpolator mScaleInterpolator = new ZInterpolator(0.5f);
    private AccelerateInterpolator mAlphaInterpolator = new AccelerateInterpolator(0.9f);
    private final RangeArray<Picture> mPictures = new RangeArray<>(-3, 3);
    private Size[] mSizes = new Size[7];
    private boolean mFilmMode = false;
    private boolean mWantPictureCenterCallbacks = false;
    private int mDisplayRotation = 0;
    private int mCompensation = 0;
    private Rect mCameraRelativeFrame = new Rect();
    private Rect mCameraRect = new Rect();
    private boolean mFirst = true;
    private int mTouchBoxIndex = ASContentModel.AS_UNBOUNDED;
    private int mUndoIndexHint = ASContentModel.AS_UNBOUNDED;
    private boolean mIsUpdateEngDataEnable = true;
    private boolean mIsClsuter = false;
    private boolean mIsStoragePathChanged = false;

    public interface Listener {
        void onActionBarAllowed(boolean z);

        void onActionBarWanted();

        void onCommitDeleteImage();

        void onCurrentImageUpdated();

        void onDeleteImage(Path path, int i);

        void onFilmModeChanged(boolean z);

        void onFullScreenChanged(boolean z);

        void onPictureCenter(boolean z);

        void onSingleTapConfirmed(int i, int i2);

        void onSingleTapUp(int i, int i2);

        void onUndoBarVisibilityChanged(boolean z);

        void onUndoDeleteImage();
    }

    public interface Model extends TileImageView.TileSource {
        public static final int FOCUS_HINT_NEXT = 0;
        public static final int FOCUS_HINT_PREVIOUS = 1;
        public static final int LOADING_COMPLETE = 1;
        public static final int LOADING_FAIL = 2;
        public static final int LOADING_INIT = 0;

        int getCurrentIndex();

        int getImageRotation(int i);

        void getImageSize(int i, Size size);

        int getLoadingState(int i);

        MediaItem getMediaItem(int i);

        ScreenNail getScreenNail(int i);

        boolean isCamera(int i);

        boolean isDeletable(int i);

        boolean isPanorama(int i);

        boolean isStaticCamera(int i);

        boolean isVideo(int i);

        void moveTo(int i);

        void setFocusHintDirection(int i);

        void setFocusHintPath(Path path);

        void setNeedFullImage(boolean z);
    }

    private interface Picture {
        void draw(GLCanvas gLCanvas, Rect rect);

        void forceSize();

        int getRotation();

        Size getSize();

        boolean isCamera();

        boolean isDeletable();

        void reload();

        void setScreenNail(ScreenNail screenNail);
    }

    public static class Size {
        public int height;
        public int width;
    }

    static int access$372(PhotoView photoView, int i) {
        int i2 = i & photoView.mHolding;
        photoView.mHolding = i2;
        return i2;
    }

    static int access$376(PhotoView photoView, int i) {
        int i2 = i | photoView.mHolding;
        photoView.mHolding = i2;
        return i2;
    }

    public PhotoView(AbstractGalleryActivity abstractGalleryActivity) {
        this.mTileView = new TileImageView(abstractGalleryActivity);
        addComponent(this.mTileView);
        this.mContext = abstractGalleryActivity.getAndroidContext();
        this.mPlaceholderColor = this.mContext.getResources().getColor(R.color.photo_placeholder);
        this.mEdgeView = new EdgeView(this.mContext);
        addComponent(this.mEdgeView);
        this.mUndoBar = new UndoBarView(this.mContext);
        addComponent(this.mUndoBar);
        this.mUndoBar.setVisibility(1);
        this.mUndoBar.setOnClickListener(new GLView.OnClickListener() {
            @Override
            public void onClick(GLView gLView) {
                PhotoView.this.mListener.onUndoDeleteImage();
                PhotoView.this.hideUndoBar();
            }
        });
        this.mNoThumbnailText = StringTexture.newInstance(this.mContext.getString(R.string.no_thumbnail), this.mContext.getResources().getDimensionPixelSize(R.dimen.albumset_title_font_size), -1);
        this.mHandler = new MyHandler(abstractGalleryActivity.getGLRoot());
        this.mGestureListener = new MyGestureListener();
        this.mGestureRecognizer = new GestureRecognizer(this.mContext, this.mGestureListener, abstractGalleryActivity.getGLRoot());
        this.mPositionController = new PositionController(this.mContext, new PositionController.Listener() {
            @Override
            public void invalidate() {
                PhotoView.this.invalidate();
            }

            @Override
            public boolean isHoldingDown() {
                return (PhotoView.this.mHolding & 1) != 0;
            }

            @Override
            public boolean isHoldingDelete() {
                return (PhotoView.this.mHolding & 4) != 0;
            }

            @Override
            public void onPull(int i, int i2) {
                PhotoView.this.mEdgeView.onPull(i, i2);
            }

            @Override
            public void onRelease() {
                PhotoView.this.mEdgeView.onRelease();
            }

            @Override
            public void onAbsorb(int i, int i2) {
                PhotoView.this.mEdgeView.onAbsorb(i, i2);
            }
        });
        this.mVideoPlayIcon = new ResourceTexture(this.mContext, R.drawable.ic_control_play);
        for (int i = -3; i <= 3; i++) {
            if (i == 0) {
                this.mPictures.put(i, new FullPicture());
            } else {
                this.mPictures.put(i, new ScreenNailPicture(i));
            }
        }
        extConstruct(abstractGalleryActivity);
    }

    public void stopScrolling() {
        this.mPositionController.stopScrolling();
    }

    public void setModel(Model model) {
        this.mModel = model;
        this.mTileView.setModel(this.mModel);
    }

    class MyHandler extends SynchronizedHandler {
        public MyHandler(GLRoot gLRoot) {
            super(gLRoot);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 2:
                    PhotoView.this.mGestureRecognizer.cancelScale();
                    PhotoView.this.mPositionController.setExtraScalingRange(false);
                    PhotoView.this.mCancelExtraScalingPending = false;
                    return;
                case 3:
                    PhotoView.this.switchFocus();
                    return;
                case 4:
                    PhotoView.this.captureAnimationDone(message.arg1);
                    return;
                case 5:
                    PhotoView.this.mListener.onDeleteImage((Path) message.obj, message.arg1);
                    if (!PhotoView.this.mIsClsuter) {
                        PhotoView.this.mHandler.removeMessages(6);
                        PhotoView.this.mHandler.sendMessageDelayed(PhotoView.this.mHandler.obtainMessage(6), 2000L);
                    }
                    int i = (PhotoView.this.mNextBound - PhotoView.this.mPrevBound) + 1;
                    if (i == 2 && (PhotoView.this.mModel.isCamera(PhotoView.this.mNextBound) || PhotoView.this.mModel.isCamera(PhotoView.this.mPrevBound))) {
                        i--;
                    }
                    PhotoView.this.showUndoBar(i <= 1);
                    return;
                case 6:
                    if (!PhotoView.this.mHandler.hasMessages(5)) {
                        PhotoView.access$372(PhotoView.this, -5);
                        PhotoView.this.snapback();
                        return;
                    }
                    return;
                case 7:
                    PhotoView.this.checkHideUndoBar(2);
                    return;
                case 8:
                    PhotoView.this.checkHideUndoBar(8);
                    return;
                default:
                    throw new AssertionError(message.what);
            }
        }
    }

    public void setWantPictureCenterCallbacks(boolean z) {
        this.mWantPictureCenterCallbacks = z;
    }

    public void notifyDataChange(int[] iArr, int i, int i2) {
        this.mPrevBound = i;
        this.mNextBound = i2;
        if (this.mTouchBoxIndex != Integer.MAX_VALUE) {
            int i3 = this.mTouchBoxIndex;
            this.mTouchBoxIndex = ASContentModel.AS_UNBOUNDED;
            int i4 = 0;
            while (true) {
                if (i4 >= 7) {
                    break;
                }
                if (iArr[i4] != i3) {
                    i4++;
                } else {
                    this.mTouchBoxIndex = i4 - 3;
                    break;
                }
            }
        }
        if (this.mUndoIndexHint != Integer.MAX_VALUE && Math.abs(this.mUndoIndexHint - this.mModel.getCurrentIndex()) >= 3) {
            hideUndoBar();
        }
        for (int i5 = -3; i5 <= 3; i5++) {
            Picture picture = this.mPictures.get(i5);
            picture.reload();
            this.mSizes[i5 + 3] = picture.getSize();
        }
        boolean zHasDeletingBox = this.mPositionController.hasDeletingBox();
        this.mPositionController.moveBox(iArr, this.mPrevBound < 0, this.mNextBound > 0, this.mModel.isCamera(0), this.mSizes);
        for (int i6 = -3; i6 <= 3; i6++) {
            setPictureSize(i6, false);
        }
        boolean zHasDeletingBox2 = this.mPositionController.hasDeletingBox();
        if (zHasDeletingBox && !zHasDeletingBox2) {
            this.mHandler.removeMessages(6);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(6), 600L);
        }
        updateEngineData();
        invalidate();
    }

    public boolean isDeleting() {
        return (this.mHolding & 4) != 0 && this.mPositionController.hasDeletingBox();
    }

    public void notifyImageChange(int i) {
        if (i == 0) {
            this.mListener.onCurrentImageUpdated();
        }
        this.mPictures.get(i).reload();
        setPictureSize(i, false);
        updateEngineData();
        invalidate();
    }

    private void setPictureSize(int i, boolean z) {
        Picture picture = this.mPictures.get(i);
        this.mPositionController.setImageSize(i, picture.getSize(), (i == 0 && picture.isCamera()) ? this.mCameraRect : null, z);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5 = i3 - i;
        int i6 = i4 - i2;
        this.mTileView.layout(0, 0, i5, i6);
        this.mEdgeView.layout(0, 0, i5, i6);
        this.mUndoBar.measure(0, 0);
        this.mUndoBar.layout(0, i6 - this.mUndoBar.getMeasuredHeight(), i5, i6);
        GLRoot gLRoot = getGLRoot();
        int displayRotation = gLRoot.getDisplayRotation();
        int compensation = gLRoot.getCompensation();
        if (this.mDisplayRotation != displayRotation || this.mCompensation != compensation) {
            this.mDisplayRotation = displayRotation;
            this.mCompensation = compensation;
            for (int i7 = -3; i7 <= 3; i7++) {
                Picture picture = this.mPictures.get(i7);
                if (picture.isCamera()) {
                    picture.forceSize();
                }
            }
        }
        updateCameraRect();
        this.mPositionController.setConstrainedFrame(this.mCameraRect);
        if (z) {
            this.mPositionController.setViewSize(getWidth(), getHeight());
        }
        this.mLayerManager.onLayout(z, i, i2, i3, i4);
    }

    private void updateCameraRect() {
        int width = getWidth();
        int height = getHeight();
        if (this.mCompensation % 180 == 0) {
            height = width;
            width = height;
        }
        int i = this.mCameraRelativeFrame.left;
        int i2 = this.mCameraRelativeFrame.top;
        int i3 = this.mCameraRelativeFrame.right;
        int i4 = this.mCameraRelativeFrame.bottom;
        int i5 = this.mCompensation;
        if (i5 == 0) {
            this.mCameraRect.set(i, i2, i3, i4);
        } else if (i5 == 90) {
            this.mCameraRect.set(width - i4, i, width - i2, i3);
        } else if (i5 == 180) {
            this.mCameraRect.set(height - i3, width - i4, height - i, width - i2);
        } else if (i5 == 270) {
            this.mCameraRect.set(i2, height - i3, i4, height - i);
        }
        Log.d(TAG, "compensation = " + this.mCompensation + ", CameraRelativeFrame = " + this.mCameraRelativeFrame + ", mCameraRect = " + this.mCameraRect);
    }

    public void setCameraRelativeFrame(Rect rect) {
        this.mCameraRelativeFrame.set(rect);
        updateCameraRect();
    }

    private int getCameraRotation() {
        return ((this.mCompensation - this.mDisplayRotation) + 360) % 360;
    }

    private int getPanoramaRotation() {
        boolean z = false;
        boolean z2 = this.mContext.getResources().getConfiguration().orientation == 1 && (this.mDisplayRotation == 90 || this.mDisplayRotation == 270);
        if (this.mDisplayRotation >= 180) {
            z = true;
        }
        if (z != z2) {
            return (this.mCompensation + 180) % 360;
        }
        return this.mCompensation;
    }

    class FullPicture implements Picture {
        private boolean mIsCamera;
        private boolean mIsDeletable;
        private boolean mIsPanorama;
        private boolean mIsStaticCamera;
        private boolean mIsVideo;
        private int mRotation;
        private ScreenNail mScreenNail;
        private int mLoadingState = 0;
        private Size mSize = new Size();

        FullPicture() {
        }

        @Override
        public int getRotation() {
            updateSize();
            return this.mRotation;
        }

        @Override
        public void reload() {
            PhotoView.this.mTileView.notifyModelInvalidated();
            this.mIsCamera = PhotoView.this.mModel.isCamera(0);
            this.mIsPanorama = PhotoView.this.mModel.isPanorama(0);
            this.mIsStaticCamera = PhotoView.this.mModel.isStaticCamera(0);
            this.mIsVideo = PhotoView.this.mModel.isVideo(0);
            this.mIsDeletable = PhotoView.this.mModel.isDeletable(0);
            this.mLoadingState = PhotoView.this.mModel.getLoadingState(0);
            setScreenNail(PhotoView.this.mModel.getScreenNail(0));
            updateSize();
        }

        @Override
        public Size getSize() {
            return this.mSize;
        }

        @Override
        public void forceSize() {
            updateSize();
            PhotoView.this.mPositionController.forceImageSize(0, this.mSize);
        }

        private void updateSize() {
            if (this.mIsPanorama) {
                this.mRotation = PhotoView.this.getPanoramaRotation();
            } else if (!this.mIsCamera || this.mIsStaticCamera) {
                this.mRotation = PhotoView.this.mModel.getImageRotation(0);
            } else {
                this.mRotation = PhotoView.this.getCameraRotation();
            }
            int i = PhotoView.this.mTileView.mImageWidth;
            int i2 = PhotoView.this.mTileView.mImageHeight;
            this.mSize.width = PhotoView.getRotated(this.mRotation, i, i2);
            this.mSize.height = PhotoView.getRotated(this.mRotation, i2, i);
        }

        @Override
        public void draw(GLCanvas gLCanvas, Rect rect) {
            drawTileView(gLCanvas, rect);
            if ((PhotoView.this.mHolding & (-2)) == 0 && PhotoView.this.mWantPictureCenterCallbacks && PhotoView.this.mPositionController.isCenter()) {
                PhotoView.this.mListener.onPictureCenter(this.mIsCamera);
            }
        }

        @Override
        public void setScreenNail(ScreenNail screenNail) {
            PhotoView.this.mTileView.setScreenNail(screenNail);
            this.mScreenNail = screenNail;
            PhotoView.sSetScreenNailTime = System.currentTimeMillis();
        }

        @Override
        public boolean isCamera() {
            return this.mIsCamera;
        }

        @Override
        public boolean isDeletable() {
            return this.mIsDeletable;
        }

        private void drawTileView(GLCanvas gLCanvas, Rect rect) {
            float f;
            float f2;
            Rect rect2;
            int i;
            float fInterpolate;
            float f3;
            float imageScale = PhotoView.this.mPositionController.getImageScale();
            int width = PhotoView.this.getWidth();
            int height = PhotoView.this.getHeight();
            float fExactCenterX = rect.exactCenterX();
            float fExactCenterY = rect.exactCenterY();
            gLCanvas.save(3);
            float filmRatio = PhotoView.this.mPositionController.getFilmRatio();
            boolean z = (this.mIsCamera || filmRatio == 1.0f || ((Picture) PhotoView.this.mPictures.get(-1)).isCamera() || PhotoView.this.mPositionController.inOpeningAnimation()) ? false : true;
            boolean z2 = this.mIsDeletable && filmRatio == 1.0f && rect.centerY() != height / 2;
            if (z) {
                int i2 = rect.left;
                int i3 = rect.right;
                float fClamp = Utils.clamp(PhotoView.calculateMoveOutProgress(i2, i3, width), -1.0f, 1.0f);
                if (fClamp < 0.0f) {
                    float scrollScale = PhotoView.this.getScrollScale(fClamp);
                    float scrollAlpha = PhotoView.this.getScrollAlpha(fClamp);
                    fInterpolate = PhotoView.interpolate(filmRatio, scrollScale, 1.0f);
                    imageScale *= fInterpolate;
                    gLCanvas.multiplyAlpha(PhotoView.interpolate(filmRatio, scrollAlpha, 1.0f));
                    int i4 = i3 - i2;
                    if (i4 <= width) {
                        f3 = width / 2.0f;
                    } else {
                        f3 = (i4 * fInterpolate) / 2.0f;
                    }
                    fExactCenterX = PhotoView.interpolate(filmRatio, f3, fExactCenterX);
                } else {
                    fInterpolate = 1.0f;
                }
                f = imageScale;
                f2 = fInterpolate;
            } else {
                if (z2) {
                    gLCanvas.multiplyAlpha(PhotoView.this.getOffsetAlpha((rect.centerY() - (height / 2)) / height));
                }
                f = imageScale;
                f2 = 1.0f;
            }
            int i5 = (int) (fExactCenterX + 0.5f);
            int i6 = (int) (0.5f + fExactCenterY);
            boolean zDrawCurrentDynamic = PhotoView.this.drawCurrentDynamic(PhotoView.this.mModel.getMediaItem(0), gLCanvas, rect, i5, i6, this.mRotation, f2);
            if (this.mLoadingState == 2 && zDrawCurrentDynamic) {
                this.mLoadingState = 1;
            }
            if (!zDrawCurrentDynamic) {
                i = i6;
                rect2 = rect;
                setTileViewPosition(fExactCenterX, fExactCenterY, width, height, f);
                PhotoView.this.renderChild(gLCanvas, PhotoView.this.mTileView);
                if (this.mLoadingState == 2 && !this.mIsCamera && (this.mScreenNail == null || ((this.mScreenNail instanceof TiledScreenNail) && !((TiledScreenNail) this.mScreenNail).isPlaceHolderDrawingEnabled()))) {
                    PhotoView.this.drawPlaceHolder(gLCanvas, rect2);
                }
            } else {
                rect2 = rect;
                i = i6;
            }
            PhotoView.this.drawLayer(gLCanvas, rect2, i5, i, 0, f2);
            gLCanvas.translate(i5, i);
            Math.min(rect.width(), rect.height());
            if (this.mIsVideo) {
                PhotoView.this.drawVideoPlayIcon(gLCanvas, PhotoView.this.getIconSize(1.0f));
            }
            for (IPhotoRenderer iPhotoRenderer : PhotoView.this.mPhotoRenderExts) {
                if (PhotoView.this.mModel.getMediaItem(0) != null) {
                    iPhotoRenderer.renderOverlay(gLCanvas.getMGLCanvas(), rect.width(), rect.height(), PhotoView.this.mModel.getMediaItem(0).getMediaData());
                }
            }
            if (this.mLoadingState == 2) {
                PhotoView.this.drawLoadingFailMessage(gLCanvas);
            }
            gLCanvas.restore();
        }

        private void setTileViewPosition(float f, float f2, int i, int i2, float f3) {
            int imageWidth = PhotoView.this.mPositionController.getImageWidth();
            int imageHeight = PhotoView.this.mPositionController.getImageHeight();
            int i3 = (int) ((imageWidth / 2.0f) + (((i / 2.0f) - f) / f3) + 0.5f);
            int i4 = (int) ((imageHeight / 2.0f) + (((i2 / 2.0f) - f2) / f3) + 0.5f);
            int i5 = imageWidth - i3;
            int i6 = imageHeight - i4;
            int i7 = this.mRotation;
            if (i7 == 0) {
                i5 = i4;
                i4 = i3;
            } else if (i7 != 90) {
                if (i7 == 180) {
                    i4 = i5;
                    i5 = i6;
                } else {
                    if (i7 != 270) {
                        throw new RuntimeException(String.valueOf(this.mRotation));
                    }
                    i5 = i3;
                    i4 = i6;
                }
            }
            PhotoView.this.mTileView.setPosition(i4, i5, f3, this.mRotation);
        }
    }

    private class ScreenNailPicture implements Picture {
        private int mIndex;
        private boolean mIsCamera;
        private boolean mIsDeletable;
        private boolean mIsPanorama;
        private boolean mIsStaticCamera;
        private boolean mIsVideo;
        private int mRotation;
        private ScreenNail mScreenNail;
        private int mLoadingState = 0;
        private Size mSize = new Size();

        public ScreenNailPicture(int i) {
            this.mIndex = i;
        }

        @Override
        public int getRotation() {
            return this.mRotation;
        }

        @Override
        public void reload() {
            this.mIsCamera = PhotoView.this.mModel.isCamera(this.mIndex);
            this.mIsPanorama = PhotoView.this.mModel.isPanorama(this.mIndex);
            this.mIsStaticCamera = PhotoView.this.mModel.isStaticCamera(this.mIndex);
            this.mIsVideo = PhotoView.this.mModel.isVideo(this.mIndex);
            this.mIsDeletable = PhotoView.this.mModel.isDeletable(this.mIndex);
            this.mLoadingState = PhotoView.this.mModel.getLoadingState(this.mIndex);
            setScreenNail(PhotoView.this.mModel.getScreenNail(this.mIndex));
            updateSize();
        }

        @Override
        public Size getSize() {
            return this.mSize;
        }

        @Override
        public void draw(GLCanvas gLCanvas, Rect rect) {
            int iCenterX;
            int i;
            if (this.mScreenNail == null) {
                if (this.mIndex >= PhotoView.this.mPrevBound && this.mIndex <= PhotoView.this.mNextBound) {
                    PhotoView.this.drawPlaceHolder(gLCanvas, rect);
                    return;
                }
                return;
            }
            int width = PhotoView.this.getWidth();
            int height = PhotoView.this.getHeight();
            if (rect.left < width && rect.right > 0 && rect.top < height && rect.bottom > 0) {
                float filmRatio = PhotoView.this.mPositionController.getFilmRatio();
                boolean z = (this.mIndex <= 0 || filmRatio == 1.0f || ((Picture) PhotoView.this.mPictures.get(0)).isCamera()) ? false : true;
                boolean z2 = this.mIsDeletable && filmRatio == 1.0f && rect.centerY() != height / 2;
                if (z) {
                    iCenterX = (int) (PhotoView.interpolate(filmRatio, width / 2, rect.centerX()) + 0.5f);
                } else {
                    iCenterX = rect.centerX();
                }
                int iCenterY = rect.centerY();
                gLCanvas.save(3);
                gLCanvas.translate(iCenterX, iCenterY);
                if (z) {
                    float fClamp = Utils.clamp(((width / 2) - rect.centerX()) / width, -1.0f, 1.0f);
                    float scrollAlpha = PhotoView.this.getScrollAlpha(fClamp);
                    float scrollScale = PhotoView.this.getScrollScale(fClamp);
                    float fInterpolate = PhotoView.interpolate(filmRatio, scrollAlpha, 1.0f);
                    float fInterpolate2 = PhotoView.interpolate(filmRatio, scrollScale, 1.0f);
                    gLCanvas.multiplyAlpha(fInterpolate);
                    gLCanvas.scale(fInterpolate2, fInterpolate2, 1.0f);
                } else if (z2) {
                    gLCanvas.multiplyAlpha(PhotoView.this.getOffsetAlpha((rect.centerY() - (height / 2)) / height));
                }
                if (this.mRotation != 0) {
                    gLCanvas.rotate(this.mRotation, 0.0f, 0.0f, 1.0f);
                }
                int rotated = PhotoView.getRotated(this.mRotation, rect.width(), rect.height());
                int rotated2 = PhotoView.getRotated(this.mRotation, rect.height(), rect.width());
                boolean zDrawOtherDynamic = PhotoView.this.drawOtherDynamic(PhotoView.this.mModel.getMediaItem(this.mIndex), this.mIndex, gLCanvas, rotated, rotated2);
                if (!zDrawOtherDynamic) {
                    i = 2;
                    this.mScreenNail.draw(gLCanvas, (-rotated) / 2, (-rotated2) / 2, rotated, rotated2);
                } else {
                    i = 2;
                }
                if (this.mLoadingState == i && zDrawOtherDynamic) {
                    this.mLoadingState = 1;
                }
                if (isScreenNailAnimating()) {
                    PhotoView.this.invalidate();
                }
                if (this.mRotation != 0) {
                    gLCanvas.rotate(-this.mRotation, 0.0f, 0.0f, 1.0f);
                }
                int iconSize = PhotoView.this.getIconSize(1.0f);
                if (this.mIsVideo) {
                    PhotoView.this.drawVideoPlayIcon(gLCanvas, iconSize);
                }
                for (IPhotoRenderer iPhotoRenderer : PhotoView.this.mPhotoRenderExts) {
                    if (PhotoView.this.mModel.getMediaItem(this.mIndex) != null) {
                        iPhotoRenderer.renderOverlay(gLCanvas.getMGLCanvas(), rect.width(), rect.height(), PhotoView.this.mModel.getMediaItem(this.mIndex).getMediaData());
                    }
                }
                if (this.mLoadingState == i) {
                    PhotoView.this.drawLoadingFailMessage(gLCanvas);
                }
                gLCanvas.restore();
                return;
            }
            this.mScreenNail.noDraw();
        }

        private boolean isScreenNailAnimating() {
            return this.mScreenNail.isAnimating();
        }

        @Override
        public void setScreenNail(ScreenNail screenNail) {
            this.mScreenNail = screenNail;
        }

        @Override
        public void forceSize() {
            updateSize();
            PhotoView.this.mPositionController.forceImageSize(this.mIndex, this.mSize);
        }

        private void updateSize() {
            if (this.mIsPanorama) {
                this.mRotation = PhotoView.this.getPanoramaRotation();
            } else if (!this.mIsCamera || this.mIsStaticCamera) {
                this.mRotation = PhotoView.this.mModel.getImageRotation(this.mIndex);
            } else {
                this.mRotation = PhotoView.this.getCameraRotation();
            }
            if (this.mScreenNail == null) {
                PhotoView.this.mModel.getImageSize(this.mIndex, this.mSize);
            } else {
                this.mSize.width = this.mScreenNail.getWidth();
                this.mSize.height = this.mScreenNail.getHeight();
            }
            int i = this.mSize.width;
            int i2 = this.mSize.height;
            this.mSize.width = PhotoView.getRotated(this.mRotation, i, i2);
            this.mSize.height = PhotoView.getRotated(this.mRotation, i2, i);
        }

        @Override
        public boolean isCamera() {
            return this.mIsCamera;
        }

        @Override
        public boolean isDeletable() {
            return this.mIsDeletable;
        }
    }

    private void drawPlaceHolder(GLCanvas gLCanvas, Rect rect) {
        gLCanvas.fillRect(rect.left, rect.top, rect.width(), rect.height(), this.mPlaceholderColor);
    }

    private void drawVideoPlayIcon(GLCanvas gLCanvas, int i) {
        int i2 = i / 6;
        int i3 = (-i2) / 2;
        this.mVideoPlayIcon.draw(gLCanvas, i3, i3, i2, i2);
    }

    private void drawLoadingFailMessage(GLCanvas gLCanvas) {
        StringTexture stringTexture = this.mNoThumbnailText;
        stringTexture.draw(gLCanvas, (-stringTexture.getWidth()) / 2, (-stringTexture.getHeight()) / 2);
    }

    private static int getRotated(int i, int i2, int i3) {
        return i % 180 == 0 ? i2 : i3;
    }

    @Override
    protected boolean onTouch(MotionEvent motionEvent) {
        this.mGestureRecognizer.setAvaliable(!this.mLayerManager.onTouch(motionEvent));
        this.mGestureRecognizer.onTouchEvent(motionEvent);
        return true;
    }

    private class MyGestureListener implements GestureRecognizer.Listener {
        private float mAccScale;
        private boolean mCanChangeMode;
        private int mDeltaY;
        private boolean mDownInScrolling;
        private boolean mFirstScrollX;
        private boolean mHadFling;
        private boolean mIgnoreScalingGesture;
        private boolean mIgnoreSwipingGesture;
        private boolean mIgnoreUpEvent;
        private boolean mModeChanged;
        private boolean mScrolledAfterDown;

        private MyGestureListener() {
            this.mIgnoreUpEvent = false;
        }

        @Override
        public boolean onSingleTapConfirmed(float f, float f2) {
            GLRoot gLRoot;
            if (Build.VERSION.SDK_INT < 14 && (PhotoView.this.mHolding & 1) == 0) {
                return true;
            }
            PhotoView.access$372(PhotoView.this, -2);
            if (PhotoView.this.mFilmMode && !this.mDownInScrolling) {
                PhotoView.this.switchToHitPicture((int) (f + 0.5f), (int) (f2 + 0.5f));
                MediaItem mediaItem = PhotoView.this.mModel.getMediaItem(0);
                if (((mediaItem != null ? mediaItem.getSupportedOperations() : 0) & 16384) == 0) {
                    PhotoView.this.setFilmMode(false);
                    return true;
                }
            }
            if (PhotoView.this.mListener == null || (gLRoot = PhotoView.this.getGLRoot()) == null) {
                return true;
            }
            Matrix compensationMatrix = gLRoot.getCompensationMatrix();
            Matrix matrix = new Matrix();
            compensationMatrix.invert(matrix);
            float[] fArr = {f, f2};
            matrix.mapPoints(fArr);
            PhotoView.this.mListener.onSingleTapConfirmed((int) (fArr[0] + 0.5f), (int) (fArr[1] + 0.5f));
            return true;
        }

        @Override
        public boolean onSingleTapUp(float f, float f2) {
            if (PhotoView.this.mListener == null || PhotoView.this.getGLRoot() == null) {
                return true;
            }
            Matrix compensationMatrix = PhotoView.this.getGLRoot().getCompensationMatrix();
            Matrix matrix = new Matrix();
            compensationMatrix.invert(matrix);
            float[] fArr = {f, f2};
            matrix.mapPoints(fArr);
            PhotoView.this.mListener.onSingleTapUp((int) (fArr[0] + 0.5f), (int) (fArr[1] + 0.5f));
            return true;
        }

        @Override
        public boolean onDoubleTap(float f, float f2) {
            if (this.mIgnoreScalingGesture || this.mIgnoreSwipingGesture) {
                return true;
            }
            if (((Picture) PhotoView.this.mPictures.get(0)).isCamera()) {
                return false;
            }
            PositionController positionController = PhotoView.this.mPositionController;
            float imageScale = positionController.getImageScale();
            this.mIgnoreUpEvent = true;
            if (imageScale <= 0.75f || positionController.isAtMinimalScale()) {
                positionController.zoomIn(f, f2, Math.max(1.5f, Math.min(imageScale * 1.5f, 4.0f)));
            } else {
                positionController.resetToFullView();
            }
            return true;
        }

        @Override
        public boolean onScroll(float f, float f2, float f3, float f4) {
            int iCalculateDeltaY;
            int iCalculateDeltaY2;
            if (this.mIgnoreSwipingGesture) {
                return true;
            }
            if (!this.mScrolledAfterDown) {
                this.mScrolledAfterDown = true;
                this.mFirstScrollX = Math.abs(f) > Math.abs(f2);
            }
            int i = (int) ((-f) + 0.5f);
            int i2 = (int) ((-f2) + 0.5f);
            if (!PhotoView.this.mFilmMode) {
                PhotoView.this.mPositionController.scrollPage(i, i2);
            } else if (this.mFirstScrollX) {
                PhotoView.this.mPositionController.scrollFilmX(i);
            } else if (PhotoView.this.mTouchBoxIndex != Integer.MAX_VALUE && (iCalculateDeltaY2 = (iCalculateDeltaY = calculateDeltaY(f4)) - this.mDeltaY) != 0) {
                PhotoView.this.mPositionController.scrollFilmY(PhotoView.this.mTouchBoxIndex, iCalculateDeltaY2);
                this.mDeltaY = iCalculateDeltaY;
            }
            return true;
        }

        private int calculateDeltaY(float f) {
            if (PhotoView.this.mTouchBoxDeletable) {
                return (int) (f + 0.5f);
            }
            float height = PhotoView.this.getHeight();
            float fSin = 0.15f * height;
            if (Math.abs(f) < height) {
                fSin *= (float) Math.sin(((double) (f / height)) * 1.5707963267948966d);
            } else if (f <= 0.0f) {
                fSin = -fSin;
            }
            return (int) (fSin + 0.5f);
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            if (this.mIgnoreSwipingGesture || this.mModeChanged) {
                return true;
            }
            if (PhotoView.this.swipeImages(f, f2)) {
                this.mIgnoreUpEvent = true;
            } else {
                flingImages(f, f2, Math.abs(motionEvent2.getY() - motionEvent.getY()));
            }
            this.mHadFling = true;
            return true;
        }

        private boolean flingImages(float f, float f2, float f3) {
            boolean z;
            int iMin;
            int iFlingFilmY;
            int i = (int) (f + 0.5f);
            int i2 = (int) (0.5f + f2);
            if (!PhotoView.this.mFilmMode) {
                return PhotoView.this.mPositionController.flingPage(i, i2);
            }
            if (Math.abs(f) > Math.abs(f2)) {
                return PhotoView.this.mPositionController.flingFilmX(i);
            }
            if (!PhotoView.this.mFilmMode || PhotoView.this.mTouchBoxIndex == Integer.MAX_VALUE || !PhotoView.this.mTouchBoxDeletable) {
                return false;
            }
            int iDpToPixel = GalleryUtils.dpToPixel(PhotoView.MAX_DISMISS_VELOCITY);
            int iDpToPixel2 = GalleryUtils.dpToPixel(PhotoView.SWIPE_ESCAPE_VELOCITY);
            int iDpToPixel3 = GalleryUtils.dpToPixel(PhotoView.SWIPE_ESCAPE_DISTANCE);
            int iCenterY = PhotoView.this.mPositionController.getPosition(PhotoView.this.mTouchBoxIndex).centerY();
            if (Math.abs(i2) > iDpToPixel2 && Math.abs(i2) > Math.abs(i)) {
                if ((i2 > 0) == (iCenterY > PhotoView.this.getHeight() / 2) && f3 >= iDpToPixel3) {
                    z = true;
                }
            } else {
                z = false;
            }
            if (!z || (iFlingFilmY = PhotoView.this.mPositionController.flingFilmY(PhotoView.this.mTouchBoxIndex, (iMin = Math.min(i2, iDpToPixel)))) < 0) {
                return false;
            }
            PhotoView.this.mPositionController.setPopFromTop(iMin < 0);
            deleteAfterAnimation(iFlingFilmY);
            PhotoView.this.mTouchBoxIndex = ASContentModel.AS_UNBOUNDED;
            return true;
        }

        private void deleteAfterAnimation(int i) {
            MediaItem mediaItem = PhotoView.this.mModel.getMediaItem(PhotoView.this.mTouchBoxIndex);
            if (mediaItem == null) {
                return;
            }
            PhotoView.this.mListener.onCommitDeleteImage();
            PhotoView.this.mUndoIndexHint = PhotoView.this.mModel.getCurrentIndex() + PhotoView.this.mTouchBoxIndex;
            PhotoView.access$376(PhotoView.this, 4);
            Message messageObtainMessage = PhotoView.this.mHandler.obtainMessage(5);
            messageObtainMessage.obj = mediaItem.getPath();
            messageObtainMessage.arg1 = PhotoView.this.mTouchBoxIndex;
            PhotoView.this.mHandler.sendMessageDelayed(messageObtainMessage, i);
        }

        @Override
        public boolean onScaleBegin(float f, float f2) {
            if (this.mIgnoreSwipingGesture) {
                return true;
            }
            this.mIgnoreScalingGesture = this.mIgnoreScalingGesture || ((Picture) PhotoView.this.mPictures.get(0)).isCamera();
            if (this.mIgnoreScalingGesture) {
                return true;
            }
            PhotoView.this.mPositionController.beginScale(f, f2);
            this.mCanChangeMode = PhotoView.this.mFilmMode || PhotoView.this.mPositionController.isAtMinimalScale();
            this.mAccScale = 1.0f;
            return true;
        }

        @Override
        public boolean onScale(float f, float f2, float f3) {
            if (this.mIgnoreSwipingGesture || this.mIgnoreScalingGesture || this.mModeChanged) {
                return true;
            }
            if (Float.isNaN(f3) || Float.isInfinite(f3)) {
                return false;
            }
            int iScaleBy = PhotoView.this.mPositionController.scaleBy(f3, f, f2);
            this.mAccScale *= f3;
            boolean z = this.mAccScale < 0.97f || this.mAccScale > 1.03f;
            if (this.mCanChangeMode && z && ((iScaleBy < 0 && !PhotoView.this.mFilmMode) || (iScaleBy > 0 && PhotoView.this.mFilmMode))) {
                stopExtraScalingIfNeeded();
                PhotoView.access$372(PhotoView.this, -2);
                if (PhotoView.this.mFilmMode) {
                    UsageStatistics.setPendingTransitionCause("PinchOut");
                } else {
                    UsageStatistics.setPendingTransitionCause("PinchIn");
                }
                PhotoView.this.setFilmMode(!PhotoView.this.mFilmMode);
                onScaleEnd();
                this.mModeChanged = true;
                return true;
            }
            if (iScaleBy != 0) {
                startExtraScalingIfNeeded();
            } else {
                stopExtraScalingIfNeeded();
            }
            return true;
        }

        @Override
        public void onScaleEnd() {
            if (this.mIgnoreSwipingGesture || this.mIgnoreScalingGesture) {
                return;
            }
            PhotoView.this.mPositionController.endScale();
        }

        private void startExtraScalingIfNeeded() {
            if (!PhotoView.this.mCancelExtraScalingPending) {
                PhotoView.this.mHandler.sendEmptyMessageDelayed(2, 700L);
                PhotoView.this.mPositionController.setExtraScalingRange(true);
                PhotoView.this.mCancelExtraScalingPending = true;
            }
        }

        private void stopExtraScalingIfNeeded() {
            if (PhotoView.this.mCancelExtraScalingPending) {
                PhotoView.this.mHandler.removeMessages(2);
                PhotoView.this.mPositionController.setExtraScalingRange(false);
                PhotoView.this.mCancelExtraScalingPending = false;
            }
        }

        @Override
        public void onDown(float f, float f2) {
            PhotoView.this.checkHideUndoBar(4);
            this.mDeltaY = 0;
            this.mModeChanged = false;
            if (this.mIgnoreSwipingGesture) {
                return;
            }
            PhotoView.access$376(PhotoView.this, 1);
            if (PhotoView.this.mFilmMode && PhotoView.this.mPositionController.isScrolling()) {
                this.mDownInScrolling = true;
                PhotoView.this.mPositionController.stopScrolling();
            } else {
                this.mDownInScrolling = false;
            }
            this.mHadFling = false;
            this.mScrolledAfterDown = false;
            if (!PhotoView.this.mFilmMode) {
                PhotoView.this.mTouchBoxIndex = ASContentModel.AS_UNBOUNDED;
                return;
            }
            PhotoView.this.mTouchBoxIndex = PhotoView.this.mPositionController.hitTest((int) (f + 0.5f), PhotoView.this.getHeight() / 2);
            if (PhotoView.this.mTouchBoxIndex < PhotoView.this.mPrevBound || PhotoView.this.mTouchBoxIndex > PhotoView.this.mNextBound) {
                PhotoView.this.mTouchBoxIndex = ASContentModel.AS_UNBOUNDED;
            } else {
                PhotoView.this.mTouchBoxDeletable = ((Picture) PhotoView.this.mPictures.get(PhotoView.this.mTouchBoxIndex)).isDeletable();
            }
        }

        @Override
        public void onUp() {
            int iFlingFilmY;
            if (this.mIgnoreSwipingGesture) {
                return;
            }
            PhotoView.access$372(PhotoView.this, -2);
            PhotoView.this.mEdgeView.onRelease();
            if (PhotoView.this.mFilmMode && this.mScrolledAfterDown && !this.mFirstScrollX && PhotoView.this.mTouchBoxIndex != Integer.MAX_VALUE) {
                Rect position = PhotoView.this.mPositionController.getPosition(PhotoView.this.mTouchBoxIndex);
                float height = PhotoView.this.getHeight();
                float f = 0.5f * height;
                if (Math.abs(position.centerY() - f) > 0.4f * height && (iFlingFilmY = PhotoView.this.mPositionController.flingFilmY(PhotoView.this.mTouchBoxIndex, 0)) >= 0) {
                    PhotoView.this.mPositionController.setPopFromTop(((float) position.centerY()) < f);
                    deleteAfterAnimation(iFlingFilmY);
                }
            }
            if (!this.mIgnoreUpEvent) {
                if (!PhotoView.this.mFilmMode || this.mHadFling || !this.mFirstScrollX || !PhotoView.this.snapToNeighborImage()) {
                    PhotoView.this.snapback();
                    return;
                }
                return;
            }
            this.mIgnoreUpEvent = false;
        }

        public void setSwipingEnabled(boolean z) {
            this.mIgnoreSwipingGesture = !z;
        }

        public void setScalingEnabled(boolean z) {
            this.mIgnoreScalingGesture = !z;
        }
    }

    public void setSwipingEnabled(boolean z) {
        this.mGestureListener.setSwipingEnabled(z);
    }

    public void setScalingEnabled(boolean z) {
        this.mGestureListener.setScalingEnabled(z);
    }

    private void updateActionBar() {
        if (this.mModel.isCamera(0) && !this.mFilmMode) {
            this.mListener.onActionBarAllowed(false);
            return;
        }
        this.mListener.onActionBarAllowed(true);
        if (this.mFilmMode) {
            this.mListener.onActionBarWanted();
        }
    }

    public void setFilmMode(boolean z) {
        if (this.mFilmMode == z) {
            return;
        }
        this.mFilmMode = z;
        this.mPositionController.setFilmMode(this.mFilmMode);
        this.mModel.setNeedFullImage(!z);
        this.mModel.setFocusHintDirection(this.mFilmMode ? 1 : 0);
        updateActionBar();
        this.mListener.onFilmModeChanged(z);
        this.mLayerManager.onFilmModeChange(this.mFilmMode);
    }

    public boolean getFilmMode() {
        return this.mFilmMode;
    }

    public void pause() {
        if (!this.mFilmMode) {
            this.mPositionController.resetToFullView();
        }
        this.mPositionController.skipAnimation();
        this.mTileView.freeTextures();
        for (int i = -3; i <= 3; i++) {
            this.mPictures.get(i).setScreenNail(null);
        }
        hideUndoBar();
        this.mLayerManager.pause();
        this.mPlayEngine.pause();
        stopUpdateEngineData();
    }

    public void resume() {
        this.mTileView.prepareTextures();
        this.mPositionController.skipToFinalPosition();
        this.mLayerManager.resume();
        this.mPlayEngine.resume();
        startUpdateEngineData();
        updateEngineData();
    }

    public void resetToFirstPicture() {
        this.mModel.moveTo(0);
        setFilmMode(false);
    }

    private void showUndoBar(boolean z) {
        this.mHandler.removeMessages(7);
        this.mUndoBarState = 1;
        if (z) {
            this.mUndoBarState |= 16;
        }
        this.mUndoBar.animateVisibility(0);
        this.mHandler.sendEmptyMessageDelayed(7, 3000L);
        if (this.mListener != null) {
            this.mListener.onUndoBarVisibilityChanged(true);
        }
    }

    private void hideUndoBar() {
        this.mHandler.removeMessages(7);
        this.mListener.onCommitDeleteImage();
        this.mUndoBar.animateVisibility(1);
        this.mUndoBarState = 0;
        this.mUndoIndexHint = ASContentModel.AS_UNBOUNDED;
        this.mListener.onUndoBarVisibilityChanged(false);
        Log.d(TAG, "<hideUndoBar> do closeOptionsMenu operation");
        this.mActivity.closeOptionsMenu();
    }

    private void checkHideUndoBar(int i) {
        this.mUndoBarState = i | this.mUndoBarState;
        if ((this.mUndoBarState & 1) == 0) {
            return;
        }
        boolean z = (this.mUndoBarState & 2) != 0;
        boolean z2 = (this.mUndoBarState & 4) != 0;
        boolean z3 = (this.mUndoBarState & 8) != 0;
        boolean z4 = (this.mUndoBarState & 16) != 0;
        if ((z && z4) || z3 || z2) {
            hideUndoBar();
        }
    }

    public boolean canUndo() {
        return (this.mUndoBarState & 1) != 0;
    }

    @Override
    protected void render(GLCanvas gLCanvas) {
        updateAllPhotoPlaySize();
        if (this.mFirst) {
            this.mPictures.get(0).reload();
        }
        boolean z = !this.mFilmMode && this.mPictures.get(0).isCamera() && this.mPositionController.isCenter() && this.mPositionController.isAtMinimalScale();
        if (this.mFirst || z != this.mFullScreenCamera) {
            this.mFullScreenCamera = z;
            this.mFirst = false;
            this.mListener.onFullScreenChanged(z);
            if (z) {
                this.mHandler.sendEmptyMessage(8);
            }
        }
        if (!this.mFullScreenCamera) {
            boolean z2 = this.mPositionController.getFilmRatio() == 0.0f;
            i = (this.mHolding & 2) != 0 ? 1 : 0;
            if (!z2 || i != 0) {
                i = 3;
            } else {
                i = 1;
            }
        }
        for (int i = i; i >= (-i); i--) {
            this.mPictures.get(i).draw(gLCanvas, this.mPositionController.getPosition(i));
        }
        renderChild(gLCanvas, this.mEdgeView);
        renderChild(gLCanvas, this.mUndoBar);
        if (DebugUtils.DEBUG_POSITION_CONTROLLER) {
            renderPositionController(gLCanvas);
        }
        this.mPositionController.advanceAnimation();
        checkFocusSwitching();
    }

    private void checkFocusSwitching() {
        if (this.mFilmMode && !this.mHandler.hasMessages(3) && switchPosition() != 0) {
            this.mHandler.sendEmptyMessage(3);
        }
    }

    private void switchFocus() {
        if (this.mHolding != 0) {
            return;
        }
        int iSwitchPosition = switchPosition();
        if (iSwitchPosition == -1) {
            switchToPrevImage();
        } else if (iSwitchPosition == 1) {
            switchToNextImage();
        }
    }

    private int switchPosition() {
        Rect position = this.mPositionController.getPosition(0);
        int width = getWidth() / 2;
        if (position.left > width && this.mPrevBound < 0) {
            Rect position2 = this.mPositionController.getPosition(-1);
            if (width - position2.right < position.left - width) {
                return -1;
            }
        } else if (position.right < width && this.mNextBound > 0) {
            Rect position3 = this.mPositionController.getPosition(1);
            if (position3.left - width < width - position.right) {
                return 1;
            }
        }
        return 0;
    }

    private void switchToHitPicture(int i, int i2) {
        int totalCount;
        int iHitTestIgnoreVertical = this.mPositionController.hitTestIgnoreVertical(i, i2);
        Log.d(TAG, "<switchToHitPicture> x=" + i + ", y=" + i2 + ", hit test result index=" + iHitTestIgnoreVertical);
        if (iHitTestIgnoreVertical == Integer.MAX_VALUE) {
            return;
        }
        int currentIndex = this.mModel.getCurrentIndex();
        if ((iHitTestIgnoreVertical < 0 && this.mPrevBound < 0) | (iHitTestIgnoreVertical > 0 && this.mNextBound > 0)) {
            int i3 = currentIndex + iHitTestIgnoreVertical;
            if (i3 < 0) {
                this.mModel.moveTo(0);
                Log.d(TAG, "<switchToHitPicture> curIndex + hitIndex < 0, move to 0");
                return;
            }
            if ((this.mModel instanceof PhotoDataAdapter) && (totalCount = ((PhotoDataAdapter) this.mModel).getTotalCount()) > 0 && i3 >= totalCount) {
                StringBuilder sb = new StringBuilder();
                sb.append("<switchToHitPicture> adjust targetIndex from ");
                sb.append(i3);
                sb.append(" to ");
                i3 = totalCount - 1;
                sb.append(i3);
                Log.d(TAG, sb.toString());
            }
            this.mModel.moveTo(i3);
            Log.d(TAG, "<switchToHitPicture> move to " + i3);
        }
    }

    private boolean swipeImages(float f, float f2) {
        if (this.mFilmMode) {
            return false;
        }
        PositionController positionController = this.mPositionController;
        boolean zIsAtMinimalScale = positionController.isAtMinimalScale();
        int imageAtEdges = positionController.getImageAtEdges();
        if (!zIsAtMinimalScale && Math.abs(f2) > Math.abs(f) && ((imageAtEdges & 4) == 0 || (imageAtEdges & 8) == 0)) {
            return false;
        }
        if (f < -300.0f && (zIsAtMinimalScale || (imageAtEdges & 2) != 0)) {
            return slideToNextPicture();
        }
        if (f <= SWIPE_THRESHOLD || (!zIsAtMinimalScale && (imageAtEdges & 1) == 0)) {
            return false;
        }
        return slideToPrevPicture();
    }

    private void snapback() {
        if ((this.mHolding & (-5)) != 0) {
            return;
        }
        if (this.mFilmMode || !snapToNeighborImage()) {
            this.mPositionController.snapback();
        }
    }

    private boolean snapToNeighborImage() {
        Rect position = this.mPositionController.getPosition(0);
        int width = getWidth();
        int iGapToSide = (width / 5) + gapToSide(position.width(), width);
        if (width - position.right > iGapToSide) {
            return slideToNextPicture();
        }
        if (position.left > iGapToSide) {
            return slideToPrevPicture();
        }
        return false;
    }

    private boolean slideToNextPicture() {
        if (this.mNextBound <= 0) {
            return false;
        }
        switchToNextImage();
        this.mPositionController.startHorizontalSlide();
        return true;
    }

    private boolean slideToPrevPicture() {
        if (this.mPrevBound >= 0) {
            return false;
        }
        switchToPrevImage();
        this.mPositionController.startHorizontalSlide();
        return true;
    }

    private static int gapToSide(int i, int i2) {
        return Math.max(0, (i2 - i) / 2);
    }

    public void switchToImage(int i) {
        this.mModel.moveTo(i);
    }

    private void switchToNextImage() {
        this.mModel.moveTo(this.mModel.getCurrentIndex() + 1);
    }

    private void switchToPrevImage() {
        this.mModel.moveTo(this.mModel.getCurrentIndex() - 1);
    }

    private void switchToFirstImage() {
        this.mModel.moveTo(0);
    }

    public void setOpenAnimationRect(Rect rect) {
        this.mPositionController.setOpenAnimationRect(rect);
    }

    public boolean switchWithCaptureAnimation(int i) {
        GLRoot gLRoot = getGLRoot();
        if (gLRoot == null) {
            return false;
        }
        gLRoot.lockRenderThread();
        try {
            return switchWithCaptureAnimationLocked(i);
        } finally {
            gLRoot.unlockRenderThread();
        }
    }

    private boolean switchWithCaptureAnimationLocked(int i) {
        if (this.mHolding != 0) {
            return true;
        }
        if (i == 1) {
            if (this.mNextBound <= 0) {
                return false;
            }
            if (!this.mFilmMode) {
                this.mListener.onActionBarAllowed(false);
            }
            switchToNextImage();
            this.mPositionController.startCaptureAnimationSlide(-1);
        } else {
            if (i != -1 || this.mPrevBound >= 0) {
                return false;
            }
            if (this.mFilmMode) {
                setFilmMode(false);
            }
            if (this.mModel.getCurrentIndex() > 3) {
                switchToFirstImage();
                this.mPositionController.skipToFinalPosition();
                return true;
            }
            switchToFirstImage();
            this.mPositionController.startCaptureAnimationSlide(1);
        }
        this.mHolding |= 2;
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4, i, 0), 700L);
        return true;
    }

    private void captureAnimationDone(int i) {
        this.mHolding &= -3;
        if (i == 1 && !this.mFilmMode) {
            this.mListener.onActionBarAllowed(true);
            this.mListener.onActionBarWanted();
        }
        snapback();
    }

    private static float calculateMoveOutProgress(int i, int i2, int i3) {
        int i4 = i2 - i;
        if (i4 < i3) {
            if (i > (i3 / 2) - (i4 / 2)) {
                return (-(i - r3)) / (i3 - r3);
            }
            return (i - r3) / ((-i4) - r3);
        }
        if (i > 0) {
            return (-i) / i3;
        }
        if (i2 < i3) {
            return (i3 - i2) / i3;
        }
        return 0.0f;
    }

    private float getScrollAlpha(float f) {
        if (f < 0.0f) {
            return this.mAlphaInterpolator.getInterpolation(1.0f - Math.abs(f));
        }
        return 1.0f;
    }

    private float getScrollScale(float f) {
        float interpolation = this.mScaleInterpolator.getInterpolation(Math.abs(f));
        return (1.0f - interpolation) + (interpolation * TRANSITION_SCALE_FACTOR);
    }

    private static class ZInterpolator {
        private float focalLength;

        public ZInterpolator(float f) {
            this.focalLength = f;
        }

        public float getInterpolation(float f) {
            return (1.0f - (this.focalLength / (this.focalLength + f))) / (1.0f - (this.focalLength / (this.focalLength + 1.0f)));
        }
    }

    private static float interpolate(float f, float f2, float f3) {
        return f2 + ((f3 - f2) * f * f);
    }

    private float getOffsetAlpha(float f) {
        float f2 = f / 0.5f;
        return Utils.clamp(f2 > 0.0f ? 1.0f - f2 : f2 + 1.0f, 0.03f, 1.0f);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public Rect getPhotoRect(int i) {
        return this.mPositionController.getPosition(i);
    }

    public PhotoFallbackEffect buildFallbackEffect(GLView gLView, GLCanvas gLCanvas) {
        RawTexture rawTexture;
        Rect rect = new Rect();
        Utils.assertTrue(gLView.getBoundsOf(this, rect));
        Rect rectBounds = bounds();
        PhotoFallbackEffect photoFallbackEffect = new PhotoFallbackEffect();
        for (int i = -3; i <= 3; i++) {
            MediaItem mediaItem = this.mModel.getMediaItem(i);
            if (mediaItem != null) {
                ?? screenNail = this.mModel.getScreenNail(i);
                if ((screenNail instanceof TiledScreenNail) && !screenNail.isShowingPlaceholder()) {
                    Rect rect2 = new Rect(getPhotoRect(i));
                    if (Rect.intersects(rectBounds, rect2)) {
                        rect2.offset(rect.left, rect.top);
                        int width = screenNail.getWidth();
                        int height = screenNail.getHeight();
                        int imageRotation = this.mModel.getImageRotation(i);
                        if (imageRotation % 180 == 0) {
                            rawTexture = new RawTexture(width, height, true);
                            gLCanvas.beginRenderTarget(rawTexture);
                            gLCanvas.translate(width / 2.0f, height / 2.0f);
                        } else {
                            rawTexture = new RawTexture(height, width, true);
                            gLCanvas.beginRenderTarget(rawTexture);
                            gLCanvas.translate(height / 2.0f, width / 2.0f);
                        }
                        RawTexture rawTexture2 = rawTexture;
                        gLCanvas.rotate(imageRotation, 0.0f, 0.0f, 1.0f);
                        gLCanvas.translate((-width) / 2.0f, (-height) / 2.0f);
                        screenNail.draw(gLCanvas, 0, 0, width, height);
                        gLCanvas.endRenderTarget();
                        photoFallbackEffect.addEntry(mediaItem.getPath(), rect2, rawTexture2);
                    }
                }
            }
        }
        return photoFallbackEffect;
    }

    public synchronized void startUpdateEngineData() {
        Log.d(TAG, "<startUpdateEngineData>");
        this.mIsUpdateEngDataEnable = true;
    }

    public synchronized void stopUpdateEngineData() {
        Log.d(TAG, "<stopUpdateEngineData>");
        this.mIsUpdateEngDataEnable = false;
    }

    public void destroy() {
        this.mLayerManager.destroy();
    }

    @Override
    public void onFrameAvailable(int i) {
        invalidate();
    }

    private void extConstruct(Activity activity) {
        this.mActivity = activity;
        this.mLayerManager = PhotoPlayFacade.createLayerMananger(activity);
        this.mLayerManager.init(getRootView(), new MGLRootView(this));
        this.mPlayCount = PhotoPlayFacade.getFullScreenPlayCount();
        this.mActiveCount = PhotoPlayFacade.getFullScreenTotalCount();
        this.mPlayEngine = PhotoPlayFacade.createPlayEngineForFullScreen();
        this.mPlayEngine.setOnFrameAvailableListener(this);
        this.mPlayEngine.setLayerManager(this.mLayerManager);
        this.mPhotoRenderExts = (IPhotoRenderer[]) FeatureManager.getInstance().getImplement(IPhotoRenderer.class, this.mContext, this.mContext.getResources());
    }

    private boolean drawCurrentDynamic(MediaItem mediaItem, GLCanvas gLCanvas, Rect rect, int i, int i2, int i3, float f) {
        if (mediaItem == null) {
            return false;
        }
        gLCanvas.save(2);
        gLCanvas.translate(i, i2);
        if (i3 != 0) {
            gLCanvas.rotate(i3, 0.0f, 0.0f, 1.0f);
        }
        gLCanvas.scale(f, f, 1.0f);
        int rotated = getRotated(i3, rect.width(), rect.height());
        int rotated2 = getRotated(i3, rect.height(), rect.width());
        gLCanvas.translate((-rotated) * 0.5f, (-rotated2) * 0.5f);
        boolean zDraw = this.mPlayEngine.draw(mediaItem.getMediaData(), covertToEngineIndex(0), gLCanvas.getMGLCanvas(), rotated, rotated2);
        gLCanvas.restore();
        return zDraw;
    }

    private void drawLayer(GLCanvas gLCanvas, Rect rect, int i, int i2, int i3, float f) {
        if (this.mFilmMode || !this.mPositionController.isAtMinimalScale()) {
            return;
        }
        gLCanvas.save(2);
        int rotated = getRotated(i3, rect.width(), rect.height());
        int rotated2 = getRotated(i3, rect.height(), rect.width());
        gLCanvas.translate(i - (rotated * 0.5f), 0.0f);
        this.mLayerManager.drawLayer(gLCanvas.getMGLCanvas(), rotated, rotated2);
        gLCanvas.restore();
    }

    private boolean drawOtherDynamic(MediaItem mediaItem, int i, GLCanvas gLCanvas, int i2, int i3) {
        int iCovertToEngineIndex;
        if (mediaItem == null || (iCovertToEngineIndex = covertToEngineIndex(i)) < 0 || iCovertToEngineIndex >= this.mActiveCount) {
            return false;
        }
        gLCanvas.save(2);
        gLCanvas.translate((-i2) / 2, (-i3) / 2);
        boolean zDraw = this.mPlayEngine.draw(mediaItem.getMediaData(), iCovertToEngineIndex, gLCanvas.getMGLCanvas(), i2, i3);
        gLCanvas.restore();
        return zDraw;
    }

    private void updateEngineData() {
        synchronized (this) {
            if (!this.mIsUpdateEngDataEnable) {
                Log.d(TAG, "<updateEngineData> mIsUpdateEngDataEnable = false, return");
                return;
            }
            MediaData[] mediaDataArr = new MediaData[this.mActiveCount];
            for (int i = (-this.mActiveCount) / 2; i <= this.mActiveCount / 2; i++) {
                MediaItem mediaItem = this.mModel.getMediaItem(i);
                if (mediaItem != null) {
                    mediaDataArr[(this.mActiveCount / 2) + i] = mediaItem.getMediaData();
                } else {
                    mediaDataArr[(this.mActiveCount / 2) + i] = null;
                }
            }
            this.mPlayEngine.updateData(mediaDataArr);
        }
    }

    private ViewGroup getRootView() {
        ViewGroup viewGroup = (ViewGroup) this.mActivity.findViewById(R.id.gallery_root);
        if (viewGroup == null) {
            Log.d(TAG, "<getRootView> galleryRoot = null, return null");
        }
        return viewGroup;
    }

    private void updateAllPhotoPlaySize() {
        for (int i = (-this.mActiveCount) / 2; i <= this.mActiveCount / 2; i++) {
            updatePhotoPlaySize(i);
        }
    }

    private void updatePhotoPlaySize(int i) {
        MediaItem mediaItem = this.mModel.getMediaItem(i);
        if (i < (-this.mActiveCount) / 2 || i > this.mActiveCount / 2 || this.mPictures.get(i) == null || mediaItem == null) {
            return;
        }
        int iCovertToEngineIndex = covertToEngineIndex(i);
        int playWidth = this.mPlayEngine.getPlayWidth(iCovertToEngineIndex, mediaItem.getMediaData());
        int playHeight = this.mPlayEngine.getPlayHeight(iCovertToEngineIndex, mediaItem.getMediaData());
        int rotation = this.mPictures.get(i).getRotation();
        setSize(i, getRotated(rotation, playWidth, playHeight), getRotated(rotation, playHeight, playWidth), this.mPlayEngine.isSkipAnimationWhenUpdateSize(iCovertToEngineIndex));
    }

    private void setSize(int i, int i2, int i3, boolean z) {
        if (i2 == 0 || i3 == 0) {
            setPictureSize(i, z);
            return;
        }
        Size size = new Size();
        size.width = i2;
        size.height = i3;
        this.mPositionController.setImageSize(i, size, null, z);
    }

    private int covertToEngineIndex(int i) {
        return i + (this.mActiveCount / 2);
    }

    private int covertFromEngineIndex(int i) {
        return i - (this.mActiveCount / 2);
    }

    public void setIsCluster(boolean z) {
        this.mIsClsuter = z;
    }

    private int getIconSize(float f) {
        int iMin = (int) ((f * Math.min(getWidth(), getHeight())) + 0.5f);
        if (this.mFilmMode) {
            return iMin / 2;
        }
        return iMin;
    }

    public boolean onBackPressed() {
        return this.mLayerManager.onBackPressed();
    }

    public boolean onUpPressed() {
        return this.mLayerManager.onUpPressed();
    }

    @Override
    protected void onKeyEvent(KeyEvent keyEvent) {
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return this.mLayerManager.onCreateOptionsMenu(menu);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return this.mLayerManager.onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        return this.mLayerManager.onOptionsItemSelected(menuItem);
    }

    public boolean onActionBarVisibilityChange(boolean z) {
        return this.mLayerManager.onActionBarVisibilityChange(z);
    }

    public boolean freshLayers(boolean z) {
        return this.mLayerManager.freshLayers(z);
    }

    private void renderPositionController(GLCanvas gLCanvas) {
        for (int i = -3; i <= 3; i++) {
            Rect position = this.mPositionController.getPosition(i);
            if (i == 0) {
                gLCanvas.fillRect(position.left, position.top, position.width(), position.height(), -2013265784);
            } else {
                gLCanvas.fillRect(position.left, position.top, position.width(), position.height(), -2013230968);
            }
        }
        gLCanvas.fillRect((getWidth() / 2) - 1, 0.0f, 2.0f, getHeight(), -65536);
        int iSwitchPosition = switchPosition();
        if (iSwitchPosition == -1) {
            gLCanvas.fillRect(0.0f, 100.0f, 50.0f, 50.0f, -256);
        } else if (iSwitchPosition == 1) {
            gLCanvas.fillRect(getWidth() - 50, 100.0f, 50.0f, 50.0f, -256);
        }
    }

    public void setStoragePathChanged(boolean z) {
        this.mIsStoragePathChanged = z;
    }
}
