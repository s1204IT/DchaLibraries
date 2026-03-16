package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Build;
import android.os.Message;
import android.support.v4.app.FragmentManagerImpl;
import android.support.v4.app.NotificationCompat;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.animation.AccelerateInterpolator;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.GLCanvas;
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

public class PhotoView extends GLView {
    private static float TRANSITION_SCALE_FACTOR = 0.74f;
    private boolean mCancelExtraScalingPending;
    private Context mContext;
    private EdgeView mEdgeView;
    private boolean mFullScreenCamera;
    private final MyGestureListener mGestureListener;
    private final GestureRecognizer mGestureRecognizer;
    private SynchronizedHandler mHandler;
    private int mHolding;
    private Listener mListener;
    private Model mModel;
    private int mNextBound;
    private StringTexture mNoThumbnailText;
    private final int mPlaceholderColor;
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
    private int mTouchBoxIndex = Integer.MAX_VALUE;
    private int mUndoIndexHint = Integer.MAX_VALUE;

    public interface Listener {
        void onActionBarAllowed(boolean z);

        void onActionBarWanted();

        void onCommitDeleteImage();

        void onCurrentImageUpdated();

        void onDeleteImage(Path path, int i);

        void onFilmModeChanged(boolean z);

        void onFullScreenChanged(boolean z);

        void onPictureCenter(boolean z);

        void onSingleTapUp(int i, int i2);

        void onUndoBarVisibilityChanged(boolean z);

        void onUndoDeleteImage();
    }

    public interface Model extends TileImageView.TileSource {
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

    static int access$372(PhotoView x0, int x1) {
        int i = x0.mHolding & x1;
        x0.mHolding = i;
        return i;
    }

    static int access$376(PhotoView x0, int x1) {
        int i = x0.mHolding | x1;
        x0.mHolding = i;
        return i;
    }

    public PhotoView(AbstractGalleryActivity activity) {
        this.mTileView = new TileImageView(activity);
        addComponent(this.mTileView);
        this.mContext = activity.getAndroidContext();
        this.mPlaceholderColor = this.mContext.getResources().getColor(R.color.photo_placeholder);
        this.mEdgeView = new EdgeView(this.mContext);
        addComponent(this.mEdgeView);
        this.mUndoBar = new UndoBarView(this.mContext);
        addComponent(this.mUndoBar);
        this.mUndoBar.setVisibility(1);
        this.mUndoBar.setOnClickListener(new GLView.OnClickListener() {
            @Override
            public void onClick(GLView v) {
                PhotoView.this.mListener.onUndoDeleteImage();
                PhotoView.this.hideUndoBar();
            }
        });
        this.mNoThumbnailText = StringTexture.newInstance(this.mContext.getString(R.string.no_thumbnail), 20.0f, -1);
        this.mHandler = new MyHandler(activity.getGLRoot());
        this.mGestureListener = new MyGestureListener();
        this.mGestureRecognizer = new GestureRecognizer(this.mContext, this.mGestureListener);
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
            public void onPull(int offset, int direction) {
                PhotoView.this.mEdgeView.onPull(offset, direction);
            }

            @Override
            public void onAbsorb(int velocity, int direction) {
                PhotoView.this.mEdgeView.onAbsorb(velocity, direction);
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
    }

    public void stopScrolling() {
        this.mPositionController.stopScrolling();
    }

    public void setModel(Model model) {
        this.mModel = model;
        this.mTileView.setModel(this.mModel);
    }

    class MyHandler extends SynchronizedHandler {
        public MyHandler(GLRoot root) {
            super(root);
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
                    PhotoView.this.mHandler.removeMessages(6);
                    Message m = PhotoView.this.mHandler.obtainMessage(6);
                    PhotoView.this.mHandler.sendMessageDelayed(m, 2000L);
                    int numberOfPictures = (PhotoView.this.mNextBound - PhotoView.this.mPrevBound) + 1;
                    if (numberOfPictures == 2 && (PhotoView.this.mModel.isCamera(PhotoView.this.mNextBound) || PhotoView.this.mModel.isCamera(PhotoView.this.mPrevBound))) {
                        numberOfPictures--;
                    }
                    PhotoView.this.showUndoBar(numberOfPictures <= 1);
                    return;
                case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                    if (!PhotoView.this.mHandler.hasMessages(5)) {
                        PhotoView.access$372(PhotoView.this, -5);
                        PhotoView.this.snapback();
                        return;
                    }
                    return;
                case 7:
                    PhotoView.this.checkHideUndoBar(2);
                    return;
                case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                    PhotoView.this.checkHideUndoBar(8);
                    return;
                default:
                    throw new AssertionError(message.what);
            }
        }
    }

    public void setWantPictureCenterCallbacks(boolean wanted) {
        this.mWantPictureCenterCallbacks = wanted;
    }

    public void notifyDataChange(int[] fromIndex, int prevBound, int nextBound) {
        this.mPrevBound = prevBound;
        this.mNextBound = nextBound;
        if (this.mTouchBoxIndex != Integer.MAX_VALUE) {
            int k = this.mTouchBoxIndex;
            this.mTouchBoxIndex = Integer.MAX_VALUE;
            int i = 0;
            while (true) {
                if (i >= 7) {
                    break;
                }
                if (fromIndex[i] != k) {
                    i++;
                } else {
                    this.mTouchBoxIndex = i - 3;
                    break;
                }
            }
        }
        if (this.mUndoIndexHint != Integer.MAX_VALUE && Math.abs(this.mUndoIndexHint - this.mModel.getCurrentIndex()) >= 3) {
            hideUndoBar();
        }
        for (int i2 = -3; i2 <= 3; i2++) {
            Picture p = this.mPictures.get(i2);
            p.reload();
            this.mSizes[i2 + 3] = p.getSize();
        }
        boolean wasDeleting = this.mPositionController.hasDeletingBox();
        this.mPositionController.moveBox(fromIndex, this.mPrevBound < 0, this.mNextBound > 0, this.mModel.isCamera(0), this.mSizes);
        for (int i3 = -3; i3 <= 3; i3++) {
            setPictureSize(i3);
        }
        boolean isDeleting = this.mPositionController.hasDeletingBox();
        if (wasDeleting && !isDeleting) {
            this.mHandler.removeMessages(6);
            Message m = this.mHandler.obtainMessage(6);
            this.mHandler.sendMessageDelayed(m, 600L);
        }
        invalidate();
    }

    public void notifyImageChange(int index) {
        if (index == 0) {
            this.mListener.onCurrentImageUpdated();
        }
        this.mPictures.get(index).reload();
        setPictureSize(index);
        invalidate();
    }

    private void setPictureSize(int index) {
        Picture p = this.mPictures.get(index);
        this.mPositionController.setImageSize(index, p.getSize(), (index == 0 && p.isCamera()) ? this.mCameraRect : null);
    }

    @Override
    protected void onLayout(boolean changeSize, int left, int top, int right, int bottom) {
        int w = right - left;
        int h = bottom - top;
        this.mTileView.layout(0, 0, w, h);
        this.mEdgeView.layout(0, 0, w, h);
        this.mUndoBar.measure(0, 0);
        this.mUndoBar.layout(0, h - this.mUndoBar.getMeasuredHeight(), w, h);
        GLRoot root = getGLRoot();
        int displayRotation = root.getDisplayRotation();
        int compensation = root.getCompensation();
        if (this.mDisplayRotation != displayRotation || this.mCompensation != compensation) {
            this.mDisplayRotation = displayRotation;
            this.mCompensation = compensation;
            for (int i = -3; i <= 3; i++) {
                Picture p = this.mPictures.get(i);
                if (p.isCamera()) {
                    p.forceSize();
                }
            }
        }
        updateCameraRect();
        this.mPositionController.setConstrainedFrame(this.mCameraRect);
        if (changeSize) {
            this.mPositionController.setViewSize(getWidth(), getHeight());
        }
    }

    private void updateCameraRect() {
        int w = getWidth();
        int h = getHeight();
        if (this.mCompensation % 180 != 0) {
            w = h;
            h = w;
        }
        int l = this.mCameraRelativeFrame.left;
        int t = this.mCameraRelativeFrame.top;
        int r = this.mCameraRelativeFrame.right;
        int b = this.mCameraRelativeFrame.bottom;
        switch (this.mCompensation) {
            case 0:
                this.mCameraRect.set(l, t, r, b);
                break;
            case 90:
                this.mCameraRect.set(h - b, l, h - t, r);
                break;
            case 180:
                this.mCameraRect.set(w - r, h - b, w - l, h - t);
                break;
            case 270:
                this.mCameraRect.set(t, w - r, b, w - l);
                break;
        }
        Log.d("PhotoView", "compensation = " + this.mCompensation + ", CameraRelativeFrame = " + this.mCameraRelativeFrame + ", mCameraRect = " + this.mCameraRect);
    }

    private int getCameraRotation() {
        return ((this.mCompensation - this.mDisplayRotation) + 360) % 360;
    }

    private int getPanoramaRotation() {
        int orientation = this.mContext.getResources().getConfiguration().orientation;
        boolean invertPortrait = orientation == 1 && (this.mDisplayRotation == 90 || this.mDisplayRotation == 270);
        boolean invert = this.mDisplayRotation >= 180;
        if (invert != invertPortrait) {
            return (this.mCompensation + 180) % 360;
        }
        return this.mCompensation;
    }

    class FullPicture implements Picture {
        private boolean mIsCamera;
        private boolean mIsDeletable;
        private boolean mIsGif;
        private boolean mIsPanorama;
        private boolean mIsStaticCamera;
        private boolean mIsVideo;
        private int mRotation;
        private int mLoadingState = 0;
        private Size mSize = new Size();

        FullPicture() {
        }

        @Override
        public void reload() {
            PhotoView.this.mTileView.notifyModelInvalidated();
            this.mIsCamera = PhotoView.this.mModel.isCamera(0);
            this.mIsPanorama = PhotoView.this.mModel.isPanorama(0);
            this.mIsStaticCamera = PhotoView.this.mModel.isStaticCamera(0);
            this.mIsVideo = PhotoView.this.mModel.isVideo(0);
            this.mIsGif = PhotoView.this.mModel.getMediaItem(0) == null ? false : BitmapUtils.isGifPicture(PhotoView.this.mModel.getMediaItem(0).getMimeType());
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
            int w = PhotoView.this.mTileView.mImageWidth;
            int h = PhotoView.this.mTileView.mImageHeight;
            this.mSize.width = PhotoView.getRotated(this.mRotation, w, h);
            this.mSize.height = PhotoView.getRotated(this.mRotation, h, w);
        }

        @Override
        public void draw(GLCanvas canvas, Rect r) {
            drawTileView(canvas, r);
            if ((PhotoView.this.mHolding & (-2)) == 0 && PhotoView.this.mWantPictureCenterCallbacks && PhotoView.this.mPositionController.isCenter()) {
                PhotoView.this.mListener.onPictureCenter(this.mIsCamera);
            }
        }

        @Override
        public void setScreenNail(ScreenNail s) {
            PhotoView.this.mTileView.setScreenNail(s);
        }

        @Override
        public boolean isCamera() {
            return this.mIsCamera;
        }

        @Override
        public boolean isDeletable() {
            return this.mIsDeletable;
        }

        private void drawTileView(GLCanvas canvas, Rect r) {
            float cxPage;
            float imageScale = PhotoView.this.mPositionController.getImageScale();
            int viewW = PhotoView.this.getWidth();
            int viewH = PhotoView.this.getHeight();
            float cx = r.exactCenterX();
            float cy = r.exactCenterY();
            float scale = 1.0f;
            canvas.save(3);
            float filmRatio = PhotoView.this.mPositionController.getFilmRatio();
            boolean wantsCardEffect = (this.mIsCamera || filmRatio == 1.0f || ((Picture) PhotoView.this.mPictures.get(-1)).isCamera() || PhotoView.this.mPositionController.inOpeningAnimation()) ? false : true;
            boolean wantsOffsetEffect = this.mIsDeletable && filmRatio == 1.0f && r.centerY() != viewH / 2;
            if (wantsCardEffect) {
                int left = r.left;
                int right = r.right;
                float progress = Utils.clamp(PhotoView.calculateMoveOutProgress(left, right, viewW), -1.0f, 1.0f);
                if (progress < 0.0f) {
                    float scale2 = PhotoView.this.getScrollScale(progress);
                    float alpha = PhotoView.this.getScrollAlpha(progress);
                    scale = PhotoView.interpolate(filmRatio, scale2, 1.0f);
                    imageScale *= scale;
                    canvas.multiplyAlpha(PhotoView.interpolate(filmRatio, alpha, 1.0f));
                    if (right - left <= viewW) {
                        cxPage = viewW / 2.0f;
                    } else {
                        cxPage = ((right - left) * scale) / 2.0f;
                    }
                    cx = PhotoView.interpolate(filmRatio, cxPage, cx);
                }
            } else if (wantsOffsetEffect) {
                float offset = (r.centerY() - (viewH / 2)) / viewH;
                float alpha2 = PhotoView.this.getOffsetAlpha(offset);
                canvas.multiplyAlpha(alpha2);
            }
            setTileViewPosition(cx, cy, viewW, viewH, imageScale);
            PhotoView.this.renderChild(canvas, PhotoView.this.mTileView);
            canvas.translate((int) (0.5f + cx), (int) (0.5f + cy));
            int s = (int) ((Math.min(r.width(), r.height()) * scale) + 0.5f);
            if (this.mIsVideo || this.mIsGif) {
                PhotoView.this.drawVideoPlayIcon(canvas, s);
            }
            if (this.mLoadingState == 2) {
                PhotoView.this.drawLoadingFailMessage(canvas);
            }
            canvas.restore();
        }

        private void setTileViewPosition(float cx, float cy, int viewW, int viewH, float scale) {
            int x;
            int y;
            int imageW = PhotoView.this.mPositionController.getImageWidth();
            int imageH = PhotoView.this.mPositionController.getImageHeight();
            int centerX = (int) ((imageW / 2.0f) + (((viewW / 2.0f) - cx) / scale) + 0.5f);
            int centerY = (int) ((imageH / 2.0f) + (((viewH / 2.0f) - cy) / scale) + 0.5f);
            int inverseX = imageW - centerX;
            int inverseY = imageH - centerY;
            switch (this.mRotation) {
                case 0:
                    x = centerX;
                    y = centerY;
                    break;
                case 90:
                    x = centerY;
                    y = inverseX;
                    break;
                case 180:
                    x = inverseX;
                    y = inverseY;
                    break;
                case 270:
                    x = inverseY;
                    y = centerX;
                    break;
                default:
                    throw new RuntimeException(String.valueOf(this.mRotation));
            }
            PhotoView.this.mTileView.setPosition(x, y, scale, this.mRotation);
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

        public ScreenNailPicture(int index) {
            this.mIndex = index;
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
        public void draw(GLCanvas canvas, Rect r) {
            int cx;
            if (this.mScreenNail == null) {
                if (this.mIndex >= PhotoView.this.mPrevBound && this.mIndex <= PhotoView.this.mNextBound) {
                    PhotoView.this.drawPlaceHolder(canvas, r);
                    return;
                }
                return;
            }
            int w = PhotoView.this.getWidth();
            int h = PhotoView.this.getHeight();
            if (r.left < w && r.right > 0 && r.top < h && r.bottom > 0) {
                float filmRatio = PhotoView.this.mPositionController.getFilmRatio();
                boolean wantsCardEffect = (this.mIndex <= 0 || filmRatio == 1.0f || ((Picture) PhotoView.this.mPictures.get(0)).isCamera()) ? false : true;
                boolean wantsOffsetEffect = this.mIsDeletable && filmRatio == 1.0f && r.centerY() != h / 2;
                if (wantsCardEffect) {
                    cx = (int) (PhotoView.interpolate(filmRatio, w / 2, r.centerX()) + 0.5f);
                } else {
                    cx = r.centerX();
                }
                int cy = r.centerY();
                canvas.save(3);
                canvas.translate(cx, cy);
                if (wantsCardEffect) {
                    float progress = Utils.clamp(((w / 2) - r.centerX()) / w, -1.0f, 1.0f);
                    float alpha = PhotoView.this.getScrollAlpha(progress);
                    float scale = PhotoView.this.getScrollScale(progress);
                    float alpha2 = PhotoView.interpolate(filmRatio, alpha, 1.0f);
                    float scale2 = PhotoView.interpolate(filmRatio, scale, 1.0f);
                    canvas.multiplyAlpha(alpha2);
                    canvas.scale(scale2, scale2, 1.0f);
                } else if (wantsOffsetEffect) {
                    float offset = (r.centerY() - (h / 2)) / h;
                    float alpha3 = PhotoView.this.getOffsetAlpha(offset);
                    canvas.multiplyAlpha(alpha3);
                }
                if (this.mRotation != 0) {
                    canvas.rotate(this.mRotation, 0.0f, 0.0f, 1.0f);
                }
                int drawW = PhotoView.getRotated(this.mRotation, r.width(), r.height());
                int drawH = PhotoView.getRotated(this.mRotation, r.height(), r.width());
                this.mScreenNail.draw(canvas, (-drawW) / 2, (-drawH) / 2, drawW, drawH);
                if (isScreenNailAnimating()) {
                    PhotoView.this.invalidate();
                }
                int s = Math.min(drawW, drawH);
                if (this.mIsVideo) {
                    PhotoView.this.drawVideoPlayIcon(canvas, s);
                }
                if (this.mLoadingState == 2) {
                    PhotoView.this.drawLoadingFailMessage(canvas);
                }
                canvas.restore();
                return;
            }
            this.mScreenNail.noDraw();
        }

        private boolean isScreenNailAnimating() {
            return (this.mScreenNail instanceof TiledScreenNail) && ((TiledScreenNail) this.mScreenNail).isAnimating();
        }

        @Override
        public void setScreenNail(ScreenNail s) {
            this.mScreenNail = s;
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
            int w = this.mSize.width;
            int h = this.mSize.height;
            this.mSize.width = PhotoView.getRotated(this.mRotation, w, h);
            this.mSize.height = PhotoView.getRotated(this.mRotation, h, w);
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

    private void drawPlaceHolder(GLCanvas canvas, Rect r) {
        canvas.fillRect(r.left, r.top, r.width(), r.height(), this.mPlaceholderColor);
    }

    private void drawVideoPlayIcon(GLCanvas canvas, int side) {
        int s = side / 6;
        this.mVideoPlayIcon.draw(canvas, (-s) / 2, (-s) / 2, s, s);
    }

    private void drawLoadingFailMessage(GLCanvas canvas) {
        StringTexture m = this.mNoThumbnailText;
        m.draw(canvas, (-m.getWidth()) / 2, (-m.getHeight()) / 2);
    }

    private static int getRotated(int degree, int original, int theother) {
        return degree % 180 == 0 ? original : theother;
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
        this.mGestureRecognizer.onTouchEvent(event);
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
        public boolean onSingleTapUp(float x, float y) {
            if (Build.VERSION.SDK_INT >= 14 || (PhotoView.this.mHolding & 1) != 0) {
                PhotoView.access$372(PhotoView.this, -2);
                if (!PhotoView.this.mFilmMode || this.mDownInScrolling) {
                    if (PhotoView.this.mListener != null) {
                        Matrix m = PhotoView.this.getGLRoot().getCompensationMatrix();
                        Matrix inv = new Matrix();
                        m.invert(inv);
                        float[] pts = {x, y};
                        inv.mapPoints(pts);
                        PhotoView.this.mListener.onSingleTapUp((int) (pts[0] + 0.5f), (int) (pts[1] + 0.5f));
                    }
                } else {
                    PhotoView.this.switchToHitPicture((int) (x + 0.5f), (int) (y + 0.5f));
                    MediaItem item = PhotoView.this.mModel.getMediaItem(0);
                    int supported = item != null ? item.getSupportedOperations() : 0;
                    if ((supported & 16384) == 0) {
                        PhotoView.this.setFilmMode(false);
                        this.mIgnoreUpEvent = true;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(float x, float y) {
            if (this.mIgnoreSwipingGesture) {
                return true;
            }
            if (((Picture) PhotoView.this.mPictures.get(0)).isCamera()) {
                return false;
            }
            PositionController controller = PhotoView.this.mPositionController;
            float scale = controller.getImageScale();
            this.mIgnoreUpEvent = true;
            if (scale <= 0.75f || controller.isAtMinimalScale()) {
                controller.zoomIn(x, y, Math.max(1.0f, 1.5f * scale));
            } else {
                controller.resetToFullView();
            }
            return true;
        }

        @Override
        public boolean onScroll(float dx, float dy, float totalX, float totalY) {
            int newDeltaY;
            int d;
            if (!this.mIgnoreSwipingGesture) {
                if (!this.mScrolledAfterDown) {
                    this.mScrolledAfterDown = true;
                    this.mFirstScrollX = Math.abs(dx) > Math.abs(dy);
                }
                int dxi = (int) ((-dx) + 0.5f);
                int dyi = (int) ((-dy) + 0.5f);
                if (!PhotoView.this.mFilmMode) {
                    PhotoView.this.mPositionController.scrollPage(dxi, dyi);
                } else if (this.mFirstScrollX) {
                    PhotoView.this.mPositionController.scrollFilmX(dxi);
                } else if (PhotoView.this.mTouchBoxIndex != Integer.MAX_VALUE && (d = (newDeltaY = calculateDeltaY(totalY)) - this.mDeltaY) != 0) {
                    PhotoView.this.mPositionController.scrollFilmY(PhotoView.this.mTouchBoxIndex, d);
                    this.mDeltaY = newDeltaY;
                }
            }
            return true;
        }

        private int calculateDeltaY(float delta) {
            float delta2;
            if (PhotoView.this.mTouchBoxDeletable) {
                return (int) (delta + 0.5f);
            }
            int size = PhotoView.this.getHeight();
            float maxScrollDistance = 0.15f * size;
            if (Math.abs(delta) >= size) {
                delta2 = delta > 0.0f ? maxScrollDistance : -maxScrollDistance;
            } else {
                delta2 = maxScrollDistance * FloatMath.sin((delta / size) * 1.5707964f);
            }
            return (int) (delta2 + 0.5f);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!this.mIgnoreSwipingGesture && !this.mModeChanged) {
                if (PhotoView.this.swipeImages(velocityX, velocityY)) {
                    this.mIgnoreUpEvent = true;
                } else {
                    flingImages(velocityX, velocityY, Math.abs(e2.getY() - e1.getY()));
                }
                this.mHadFling = true;
            }
            return true;
        }

        private boolean flingImages(float velocityX, float velocityY, float dY) {
            boolean fastEnough;
            int vy;
            int duration;
            int vx = (int) (0.5f + velocityX);
            int vy2 = (int) (0.5f + velocityY);
            if (!PhotoView.this.mFilmMode) {
                return PhotoView.this.mPositionController.flingPage(vx, vy2);
            }
            if (Math.abs(velocityX) > Math.abs(velocityY)) {
                return PhotoView.this.mPositionController.flingFilmX(vx);
            }
            if (!PhotoView.this.mFilmMode || PhotoView.this.mTouchBoxIndex == Integer.MAX_VALUE || !PhotoView.this.mTouchBoxDeletable) {
                return false;
            }
            int maxVelocity = GalleryUtils.dpToPixel(2500);
            int escapeVelocity = GalleryUtils.dpToPixel(500);
            int escapeDistance = GalleryUtils.dpToPixel(150);
            int centerY = PhotoView.this.mPositionController.getPosition(PhotoView.this.mTouchBoxIndex).centerY();
            if (Math.abs(vy2) <= escapeVelocity || Math.abs(vy2) <= Math.abs(vx)) {
                fastEnough = false;
            } else {
                if ((vy2 > 0) == (centerY > PhotoView.this.getHeight() / 2) && dY >= escapeDistance) {
                    fastEnough = true;
                }
            }
            if (fastEnough && (duration = PhotoView.this.mPositionController.flingFilmY(PhotoView.this.mTouchBoxIndex, (vy = Math.min(vy2, maxVelocity)))) >= 0) {
                PhotoView.this.mPositionController.setPopFromTop(vy < 0);
                deleteAfterAnimation(duration);
                PhotoView.this.mTouchBoxIndex = Integer.MAX_VALUE;
                return true;
            }
            return false;
        }

        private void deleteAfterAnimation(int duration) {
            MediaItem item = PhotoView.this.mModel.getMediaItem(PhotoView.this.mTouchBoxIndex);
            if (item != null) {
                PhotoView.this.mListener.onCommitDeleteImage();
                PhotoView.this.mUndoIndexHint = PhotoView.this.mModel.getCurrentIndex() + PhotoView.this.mTouchBoxIndex;
                PhotoView.access$376(PhotoView.this, 4);
                Message m = PhotoView.this.mHandler.obtainMessage(5);
                m.obj = item.getPath();
                m.arg1 = PhotoView.this.mTouchBoxIndex;
                PhotoView.this.mHandler.sendMessageDelayed(m, duration);
            }
        }

        @Override
        public boolean onScaleBegin(float focusX, float focusY) {
            if (!this.mIgnoreSwipingGesture) {
                this.mIgnoreScalingGesture = ((Picture) PhotoView.this.mPictures.get(0)).isCamera();
                if (!this.mIgnoreScalingGesture) {
                    PhotoView.this.mPositionController.beginScale(focusX, focusY);
                    this.mCanChangeMode = PhotoView.this.mFilmMode || PhotoView.this.mPositionController.isAtMinimalScale();
                    this.mAccScale = 1.0f;
                }
            }
            return true;
        }

        @Override
        public boolean onScale(float focusX, float focusY, float scale) {
            if (this.mIgnoreSwipingGesture || this.mIgnoreScalingGesture || this.mModeChanged) {
                return true;
            }
            if (Float.isNaN(scale) || Float.isInfinite(scale)) {
                return false;
            }
            int outOfRange = PhotoView.this.mPositionController.scaleBy(scale, focusX, focusY);
            this.mAccScale *= scale;
            boolean largeEnough = this.mAccScale < 0.97f || this.mAccScale > 1.03f;
            if (this.mCanChangeMode && largeEnough && ((outOfRange < 0 && !PhotoView.this.mFilmMode) || (outOfRange > 0 && PhotoView.this.mFilmMode))) {
                stopExtraScalingIfNeeded();
                PhotoView.access$372(PhotoView.this, -2);
                if (PhotoView.this.mFilmMode) {
                    UsageStatistics.setPendingTransitionCause("PinchOut");
                } else {
                    UsageStatistics.setPendingTransitionCause("PinchIn");
                }
                PhotoView.this.setFilmMode(PhotoView.this.mFilmMode ? false : true);
                onScaleEnd();
                this.mModeChanged = true;
                return true;
            }
            if (outOfRange != 0) {
                startExtraScalingIfNeeded();
                return true;
            }
            stopExtraScalingIfNeeded();
            return true;
        }

        @Override
        public void onScaleEnd() {
            if (!this.mIgnoreSwipingGesture && !this.mIgnoreScalingGesture && !this.mModeChanged) {
                PhotoView.this.mPositionController.endScale();
            }
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
        public void onDown(float x, float y) {
            PhotoView.this.checkHideUndoBar(4);
            this.mDeltaY = 0;
            this.mModeChanged = false;
            if (!this.mIgnoreSwipingGesture) {
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
                    PhotoView.this.mTouchBoxIndex = Integer.MAX_VALUE;
                    return;
                }
                int xi = (int) (x + 0.5f);
                PhotoView.this.mTouchBoxIndex = PhotoView.this.mPositionController.hitTest(xi, PhotoView.this.getHeight() / 2);
                if (PhotoView.this.mTouchBoxIndex < PhotoView.this.mPrevBound || PhotoView.this.mTouchBoxIndex > PhotoView.this.mNextBound) {
                    PhotoView.this.mTouchBoxIndex = Integer.MAX_VALUE;
                } else {
                    PhotoView.this.mTouchBoxDeletable = ((Picture) PhotoView.this.mPictures.get(PhotoView.this.mTouchBoxIndex)).isDeletable();
                }
            }
        }

        @Override
        public void onUp() {
            int duration;
            if (!this.mIgnoreSwipingGesture) {
                PhotoView.access$372(PhotoView.this, -2);
                PhotoView.this.mEdgeView.onRelease();
                if (PhotoView.this.mFilmMode && this.mScrolledAfterDown && !this.mFirstScrollX && PhotoView.this.mTouchBoxIndex != Integer.MAX_VALUE) {
                    Rect r = PhotoView.this.mPositionController.getPosition(PhotoView.this.mTouchBoxIndex);
                    int h = PhotoView.this.getHeight();
                    if (Math.abs(r.centerY() - (h * 0.5f)) > 0.4f * h && (duration = PhotoView.this.mPositionController.flingFilmY(PhotoView.this.mTouchBoxIndex, 0)) >= 0) {
                        PhotoView.this.mPositionController.setPopFromTop(((float) r.centerY()) < ((float) h) * 0.5f);
                        deleteAfterAnimation(duration);
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
        }
    }

    private void updateActionBar() {
        boolean isCamera = this.mPictures.get(0).isCamera();
        if (isCamera && !this.mFilmMode) {
            this.mListener.onActionBarAllowed(false);
            return;
        }
        this.mListener.onActionBarAllowed(true);
        if (this.mFilmMode) {
            this.mListener.onActionBarWanted();
        }
    }

    public void setFilmMode(boolean enabled) {
        if (this.mFilmMode != enabled) {
            this.mFilmMode = enabled;
            this.mPositionController.setFilmMode(this.mFilmMode);
            this.mModel.setNeedFullImage(!enabled);
            this.mModel.setFocusHintDirection(this.mFilmMode ? 1 : 0);
            updateActionBar();
            this.mListener.onFilmModeChanged(enabled);
        }
    }

    public boolean getFilmMode() {
        return this.mFilmMode;
    }

    public void pause() {
        this.mPositionController.skipAnimation();
        this.mTileView.freeTextures();
        for (int i = -3; i <= 3; i++) {
            this.mPictures.get(i).setScreenNail(null);
        }
        hideUndoBar();
    }

    public void resume() {
        this.mTileView.prepareTextures();
        this.mPositionController.skipToFinalPosition();
    }

    public void resetToFirstPicture() {
        this.mModel.moveTo(0);
        setFilmMode(false);
    }

    private void showUndoBar(boolean deleteLast) {
        this.mHandler.removeMessages(7);
        this.mUndoBarState = 1;
        if (deleteLast) {
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
        this.mUndoIndexHint = Integer.MAX_VALUE;
        this.mListener.onUndoBarVisibilityChanged(false);
    }

    private void checkHideUndoBar(int addition) {
        this.mUndoBarState |= addition;
        if ((this.mUndoBarState & 1) != 0) {
            boolean timeout = (this.mUndoBarState & 2) != 0;
            boolean touched = (this.mUndoBarState & 4) != 0;
            boolean fullCamera = (this.mUndoBarState & 8) != 0;
            boolean deleteLast = (this.mUndoBarState & 16) != 0;
            if ((timeout && deleteLast) || fullCamera || touched) {
                hideUndoBar();
            }
        }
    }

    public boolean canUndo() {
        return (this.mUndoBarState & 1) != 0;
    }

    @Override
    protected void render(GLCanvas canvas) {
        int neighbors;
        if (this.mFirst) {
            this.mPictures.get(0).reload();
        }
        boolean full = !this.mFilmMode && this.mPictures.get(0).isCamera() && this.mPositionController.isCenter() && this.mPositionController.isAtMinimalScale();
        if (this.mFirst || full != this.mFullScreenCamera) {
            this.mFullScreenCamera = full;
            this.mFirst = false;
            this.mListener.onFullScreenChanged(full);
            if (full) {
                this.mHandler.sendEmptyMessage(8);
            }
        }
        if (this.mFullScreenCamera) {
            neighbors = 0;
        } else {
            boolean inPageMode = this.mPositionController.getFilmRatio() == 0.0f;
            boolean inCaptureAnimation = (this.mHolding & 2) != 0;
            if (inPageMode && !inCaptureAnimation) {
                neighbors = 1;
            } else {
                neighbors = 3;
            }
        }
        for (int i = neighbors; i >= (-neighbors); i--) {
            Rect r = this.mPositionController.getPosition(i);
            this.mPictures.get(i).draw(canvas, r);
        }
        renderChild(canvas, this.mEdgeView);
        renderChild(canvas, this.mUndoBar);
        this.mPositionController.advanceAnimation();
        checkFocusSwitching();
    }

    private void checkFocusSwitching() {
        if (this.mFilmMode && !this.mHandler.hasMessages(3) && switchPosition() != 0) {
            this.mHandler.sendEmptyMessage(3);
        }
    }

    private void switchFocus() {
        if (this.mHolding == 0) {
            switch (switchPosition()) {
                case -1:
                    switchToPrevImage();
                    break;
                case 1:
                    switchToNextImage();
                    break;
            }
        }
    }

    private int switchPosition() {
        Rect curr = this.mPositionController.getPosition(0);
        int center = getWidth() / 2;
        if (curr.left > center && this.mPrevBound < 0) {
            Rect prev = this.mPositionController.getPosition(-1);
            int currDist = curr.left - center;
            int prevDist = center - prev.right;
            if (prevDist < currDist) {
                return -1;
            }
        } else if (curr.right < center && this.mNextBound > 0) {
            Rect next = this.mPositionController.getPosition(1);
            int currDist2 = center - curr.right;
            int nextDist = next.left - center;
            if (nextDist < currDist2) {
                return 1;
            }
        }
        return 0;
    }

    private void switchToHitPicture(int x, int y) {
        if (this.mPrevBound < 0) {
            Rect r = this.mPositionController.getPosition(-1);
            if (r.right >= x) {
                slideToPrevPicture();
                return;
            }
        }
        if (this.mNextBound > 0) {
            Rect r2 = this.mPositionController.getPosition(1);
            if (r2.left <= x) {
                slideToNextPicture();
            }
        }
    }

    private boolean swipeImages(float velocityX, float velocityY) {
        if (this.mFilmMode) {
            return false;
        }
        PositionController controller = this.mPositionController;
        boolean isMinimal = controller.isAtMinimalScale();
        int edges = controller.getImageAtEdges();
        if (!isMinimal && Math.abs(velocityY) > Math.abs(velocityX) && ((edges & 4) == 0 || (edges & 8) == 0)) {
            return false;
        }
        if (velocityX < -300.0f && (isMinimal || (edges & 2) != 0)) {
            return slideToNextPicture();
        }
        if (velocityX <= 300.0f) {
            return false;
        }
        if (isMinimal || (edges & 1) != 0) {
            return slideToPrevPicture();
        }
        return false;
    }

    private void snapback() {
        if ((this.mHolding & (-5)) == 0) {
            if (this.mFilmMode || !snapToNeighborImage()) {
                this.mPositionController.snapback();
            }
        }
    }

    private boolean snapToNeighborImage() {
        Rect r = this.mPositionController.getPosition(0);
        int viewW = getWidth();
        int moveThreshold = viewW / 5;
        int threshold = moveThreshold + gapToSide(r.width(), viewW);
        if (viewW - r.right > threshold) {
            return slideToNextPicture();
        }
        if (r.left > threshold) {
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

    private static int gapToSide(int imageWidth, int viewWidth) {
        return Math.max(0, (viewWidth - imageWidth) / 2);
    }

    public void switchToImage(int index) {
        this.mModel.moveTo(index);
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

    public boolean switchWithCaptureAnimation(int offset) {
        GLRoot root = getGLRoot();
        if (root == null) {
            return false;
        }
        root.lockRenderThread();
        try {
            return switchWithCaptureAnimationLocked(offset);
        } finally {
            root.unlockRenderThread();
        }
    }

    private boolean switchWithCaptureAnimationLocked(int offset) {
        if (this.mHolding != 0) {
            return true;
        }
        if (offset == 1) {
            if (this.mNextBound <= 0) {
                return false;
            }
            if (!this.mFilmMode) {
                this.mListener.onActionBarAllowed(false);
            }
            switchToNextImage();
            this.mPositionController.startCaptureAnimationSlide(-1);
        } else {
            if (offset == -1 && this.mPrevBound < 0) {
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
            return false;
        }
        this.mHolding |= 2;
        Message m = this.mHandler.obtainMessage(4, offset, 0);
        this.mHandler.sendMessageDelayed(m, 700L);
        return true;
    }

    private void captureAnimationDone(int offset) {
        this.mHolding &= -3;
        if (offset == 1 && !this.mFilmMode) {
            this.mListener.onActionBarAllowed(true);
            this.mListener.onActionBarWanted();
        }
        snapback();
    }

    private static float calculateMoveOutProgress(int left, int right, int viewWidth) {
        int w = right - left;
        if (w < viewWidth) {
            int zx = (viewWidth / 2) - (w / 2);
            if (left > zx) {
                return (-(left - zx)) / (viewWidth - zx);
            }
            return (left - zx) / ((-w) - zx);
        }
        if (left > 0) {
            return (-left) / viewWidth;
        }
        if (right < viewWidth) {
            return (viewWidth - right) / viewWidth;
        }
        return 0.0f;
    }

    private float getScrollAlpha(float scrollProgress) {
        if (scrollProgress < 0.0f) {
            return this.mAlphaInterpolator.getInterpolation(1.0f - Math.abs(scrollProgress));
        }
        return 1.0f;
    }

    private float getScrollScale(float scrollProgress) {
        float interpolatedProgress = this.mScaleInterpolator.getInterpolation(Math.abs(scrollProgress));
        float scale = (1.0f - interpolatedProgress) + (TRANSITION_SCALE_FACTOR * interpolatedProgress);
        return scale;
    }

    private static class ZInterpolator {
        private float focalLength;

        public ZInterpolator(float foc) {
            this.focalLength = foc;
        }

        public float getInterpolation(float input) {
            return (1.0f - (this.focalLength / (this.focalLength + input))) / (1.0f - (this.focalLength / (this.focalLength + 1.0f)));
        }
    }

    private static float interpolate(float ratio, float from, float to) {
        return ((to - from) * ratio * ratio) + from;
    }

    private float getOffsetAlpha(float offset) {
        float offset2 = offset / 0.5f;
        float alpha = offset2 > 0.0f ? 1.0f - offset2 : 1.0f + offset2;
        return Utils.clamp(alpha, 0.03f, 1.0f);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }
}
