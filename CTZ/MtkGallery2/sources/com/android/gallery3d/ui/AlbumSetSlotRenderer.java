package com.android.gallery3d.ui;

import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AlbumSetDataLoader;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.ColorTexture;
import com.android.gallery3d.glrenderer.FadeInTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.glrenderer.UploadedTexture;
import com.android.gallery3d.ui.AlbumSetSlidingWindow;
import com.mediatek.gallery3d.layout.FancyHelper;
import com.mediatek.gallery3d.util.FeatureConfig;

public class AlbumSetSlotRenderer extends AbstractSlotRenderer {
    public static final int CACHE_SIZE;
    private static final String TAG = "Gallery2/AlbumSetView";
    public static long mWaitFinishedTime;
    private final AbstractGalleryActivity mActivity;
    private boolean mAnimatePressedUp;
    private final ResourceTexture mCameraOverlay;
    protected AlbumSetSlidingWindow mDataWindow;
    private Path mHighlightItemPath;
    private boolean mInSelectionMode;
    protected final LabelSpec mLabelSpec;
    private final int mPlaceholderColor;
    private int mPressedIndex;
    private final SelectionManager mSelectionManager;
    private SlotView mSlotView;
    private final ColorTexture mWaitLoadingTexture;

    public static class LabelSpec {
        public int backgroundColor;
        public int borderSize;
        public int countColor;
        public int countFontSize;
        public int countOffset;
        public int iconSize;
        public int labelBackgroundHeight;
        public int leftMargin;
        public int titleColor;
        public int titleFontSize;
        public int titleOffset;
        public int titleRightMargin;
    }

    static {
        CACHE_SIZE = FeatureConfig.sIsLowRamDevice ? 32 : 96;
        mWaitFinishedTime = 0L;
    }

    public AlbumSetSlotRenderer(AbstractGalleryActivity abstractGalleryActivity, SelectionManager selectionManager, SlotView slotView, LabelSpec labelSpec, int i) {
        super(abstractGalleryActivity);
        this.mPressedIndex = -1;
        this.mHighlightItemPath = null;
        this.mActivity = abstractGalleryActivity;
        this.mSelectionManager = selectionManager;
        this.mSlotView = slotView;
        this.mLabelSpec = labelSpec;
        this.mPlaceholderColor = i;
        this.mWaitLoadingTexture = new ColorTexture(this.mPlaceholderColor);
        this.mWaitLoadingTexture.setSize(1, 1);
        this.mCameraOverlay = new ResourceTexture(abstractGalleryActivity, R.drawable.ic_cameraalbum_overlay);
    }

    public void setPressedIndex(int i) {
        if (this.mPressedIndex == i) {
            return;
        }
        this.mPressedIndex = i;
        this.mSlotView.invalidate();
    }

    public void setPressedUp() {
        if (this.mPressedIndex == -1) {
            return;
        }
        this.mAnimatePressedUp = true;
        this.mSlotView.invalidate();
    }

    public void setHighlightItemPath(Path path) {
        if (this.mHighlightItemPath == path) {
            return;
        }
        this.mHighlightItemPath = path;
        this.mSlotView.invalidate();
    }

    public void setModel(AlbumSetDataLoader albumSetDataLoader) {
        if (this.mDataWindow != null) {
            this.mDataWindow.setListener(null);
            this.mDataWindow = null;
            this.mSlotView.setSlotCount(0);
        }
        if (albumSetDataLoader != null) {
            this.mDataWindow = new AlbumSetSlidingWindow(this.mActivity, albumSetDataLoader, this.mLabelSpec, CACHE_SIZE);
            this.mDataWindow.setListener(new MyCacheListener());
            this.mSlotView.setSlotCount(this.mDataWindow.size());
        }
    }

    private static Texture checkLabelTexture(Texture texture) {
        if (!(texture instanceof UploadedTexture) || !texture.isUploading()) {
            return texture;
        }
        return null;
    }

    private static Texture checkContentTexture(Texture texture) {
        if (!(texture instanceof TiledTexture) || ((TiledTexture) texture).isReady()) {
            return texture;
        }
        return null;
    }

    @Override
    public int renderSlot(GLCanvas gLCanvas, int i, int i2, int i3, int i4) {
        AlbumSetSlidingWindow.AlbumSetEntry albumSetEntry;
        try {
            albumSetEntry = this.mDataWindow.get(i);
        } catch (AssertionError e) {
            Log.d(TAG, "<renderSlot> AssertionError when mDataWindow.get, " + e.getMessage());
            albumSetEntry = null;
        }
        if (albumSetEntry == null) {
            Log.d(TAG, "<renderSlot> entry is null, so return");
            return 0;
        }
        AlbumSetSlidingWindow.AlbumSetEntry albumSetEntry2 = albumSetEntry;
        return renderOverlay(gLCanvas, i, albumSetEntry2, i3, i4, FancyHelper.isFancyLayoutSupported()) | renderContent(gLCanvas, i, albumSetEntry2, i3, i4, FancyHelper.isFancyLayoutSupported()) | 0 | renderLabel(gLCanvas, albumSetEntry, i3, i4);
    }

    protected int renderOverlay(GLCanvas gLCanvas, int i, AlbumSetSlidingWindow.AlbumSetEntry albumSetEntry, int i2, int i3) {
        if (albumSetEntry.album != null && albumSetEntry.album.isCameraRoll()) {
            int i4 = i3 - this.mLabelSpec.labelBackgroundHeight;
            int i5 = i4 / 2;
            this.mCameraOverlay.draw(gLCanvas, (i2 - i5) / 2, (i4 - i5) / 2, i5, i5);
        }
        if (this.mPressedIndex == i) {
            if (this.mAnimatePressedUp) {
                drawPressedUpFrame(gLCanvas, i2, i3);
                if (!isPressedUpFrameFinished()) {
                    return 2;
                }
                this.mAnimatePressedUp = false;
                this.mPressedIndex = -1;
                return 2;
            }
            drawPressedFrame(gLCanvas, i2, i3);
        } else if (this.mHighlightItemPath != null && this.mHighlightItemPath == albumSetEntry.setPath) {
            drawSelectedFrame(gLCanvas, i2, i3);
        } else if (this.mInSelectionMode && this.mSelectionManager.isItemSelected(albumSetEntry.setPath)) {
            drawSelectedFrame(gLCanvas, i2, i3);
        }
        return 0;
    }

    protected int renderContent(GLCanvas gLCanvas, AlbumSetSlidingWindow.AlbumSetEntry albumSetEntry, int i, int i2) {
        Texture textureCheckContentTexture = checkContentTexture(albumSetEntry.content);
        if (textureCheckContentTexture != null) {
            this.mDataWindow.recycle(albumSetEntry);
        }
        if (textureCheckContentTexture == null) {
            textureCheckContentTexture = this.mWaitLoadingTexture;
            albumSetEntry.isWaitLoadingDisplayed = true;
        } else if (albumSetEntry.isWaitLoadingDisplayed) {
            albumSetEntry.isWaitLoadingDisplayed = false;
            textureCheckContentTexture = albumSetEntry.bitmapTexture;
            albumSetEntry.content = textureCheckContentTexture;
            mWaitFinishedTime = System.currentTimeMillis();
        }
        drawContent(gLCanvas, textureCheckContentTexture, i, i2, albumSetEntry.rotation);
        if (!(textureCheckContentTexture instanceof FadeInTexture) || !((FadeInTexture) textureCheckContentTexture).isAnimating()) {
            return 0;
        }
        return 2;
    }

    protected int renderLabel(GLCanvas gLCanvas, AlbumSetSlidingWindow.AlbumSetEntry albumSetEntry, int i, int i2) {
        Texture textureCheckLabelTexture = checkLabelTexture(albumSetEntry.labelTexture);
        if (textureCheckLabelTexture == null) {
            textureCheckLabelTexture = this.mWaitLoadingTexture;
        }
        Texture texture = textureCheckLabelTexture;
        int borderSize = AlbumLabelMaker.getBorderSize();
        int i3 = this.mLabelSpec.labelBackgroundHeight;
        texture.draw(gLCanvas, -borderSize, (i2 - i3) + borderSize, i + borderSize + borderSize, i3);
        return 0;
    }

    @Override
    public void prepareDrawing() {
        this.mInSelectionMode = this.mSelectionManager.inSelectionMode();
    }

    private class MyCacheListener implements AlbumSetSlidingWindow.Listener {
        private MyCacheListener() {
        }

        @Override
        public void onSizeChanged(int i) {
            AlbumSetSlotRenderer.this.mSlotView.setSlotCount(i);
        }

        @Override
        public void onContentChanged() {
            AlbumSetSlotRenderer.this.mSlotView.invalidate();
        }
    }

    public void pause() {
        this.mDataWindow.pause();
    }

    public void resume() {
        this.mDataWindow.resume();
    }

    @Override
    public void onVisibleRangeChanged(int i, int i2) {
        if (this.mDataWindow != null) {
            this.mDataWindow.setActiveWindow(i, i2);
        }
    }

    @Override
    public void onSlotSizeChanged(int i, int i2) {
        if (this.mDataWindow != null) {
            this.mDataWindow.onSlotSizeChanged(i, i2);
        }
    }

    public void onEyePositionChanged(int i) {
        this.mDataWindow.onEyePositionChanged(i);
    }

    @Override
    protected void drawContent(GLCanvas gLCanvas, Texture texture, int i, int i2, int i3) {
        gLCanvas.save(2);
        if (i3 == 90) {
            float f = i2 / 2;
            gLCanvas.translate(f, f);
            gLCanvas.rotate(i3, 0.0f, 0.0f, 1.0f);
            gLCanvas.translate((-i2) / 2, r3 - (i - i2));
        } else if (i3 == 270) {
            float f2 = i2 / 2;
            gLCanvas.translate(f2, f2);
            gLCanvas.rotate(i3, 0.0f, 0.0f, 1.0f);
            float f3 = (-i2) / 2;
            gLCanvas.translate(f3, f3);
        } else if (i3 == 180) {
            gLCanvas.translate(i / 2, i2 / 2);
            gLCanvas.rotate(i3, 0.0f, 0.0f, 1.0f);
            gLCanvas.translate((-i) / 2, (-i2) / 2);
        }
        if (i3 == 90 || i3 == 270) {
            texture.draw(gLCanvas, 0, 0, i2, i);
        } else {
            texture.draw(gLCanvas, 0, 0, i, i2);
        }
        gLCanvas.restore();
    }

    protected int renderOverlay(GLCanvas gLCanvas, int i, AlbumSetSlidingWindow.AlbumSetEntry albumSetEntry, int i2, int i3, boolean z) {
        if (albumSetEntry.album != null && albumSetEntry.album.isCameraRoll()) {
            int i4 = i3 - this.mLabelSpec.labelBackgroundHeight;
            if (z) {
                int iMax = (int) (0.11f * Math.max(FancyHelper.getHeightPixels(), FancyHelper.getWidthPixels()));
                this.mCameraOverlay.draw(gLCanvas, (i2 - iMax) / 2, (i3 - iMax) / 2, iMax, iMax);
            } else {
                int i5 = i4 / 2;
                this.mCameraOverlay.draw(gLCanvas, (i2 - i5) / 2, (i4 - i5) / 2, i5, i5);
            }
        }
        if (this.mPressedIndex == i) {
            if (this.mAnimatePressedUp) {
                drawPressedUpFrame(gLCanvas, i2, i3);
                if (!isPressedUpFrameFinished()) {
                    return 2;
                }
                this.mAnimatePressedUp = false;
                this.mPressedIndex = -1;
                return 2;
            }
            drawPressedFrame(gLCanvas, i2, i3);
        } else if (this.mHighlightItemPath != null && this.mHighlightItemPath == albumSetEntry.setPath) {
            drawSelectedFrame(gLCanvas, i2, i3);
        } else if (this.mInSelectionMode && this.mSelectionManager.isItemSelected(albumSetEntry.setPath)) {
            drawSelectedFrame(gLCanvas, i2, i3);
        }
        return 0;
    }

    protected int renderContent(GLCanvas gLCanvas, int i, AlbumSetSlidingWindow.AlbumSetEntry albumSetEntry, int i2, int i3, boolean z) {
        int i4;
        Texture textureCheckContentTexture = checkContentTexture(albumSetEntry.content);
        if (textureCheckContentTexture == null) {
            textureCheckContentTexture = this.mWaitLoadingTexture;
            if (z) {
                this.mWaitLoadingTexture.setSize(i2, i3);
            }
            albumSetEntry.isWaitLoadingDisplayed = true;
        } else if (albumSetEntry.isWaitLoadingDisplayed) {
            albumSetEntry.isWaitLoadingDisplayed = false;
            textureCheckContentTexture = albumSetEntry.bitmapTexture;
            albumSetEntry.content = textureCheckContentTexture;
        }
        if (z) {
            if (!albumSetEntry.isWaitLoadingDisplayed) {
                i4 = albumSetEntry.rotation;
            } else {
                i4 = 0;
            }
            drawContent(gLCanvas, textureCheckContentTexture, i2, i3, i4);
        } else {
            super.drawContent(gLCanvas, textureCheckContentTexture, i2, i3, albumSetEntry.rotation);
        }
        if (!(textureCheckContentTexture instanceof FadeInTexture) || !((FadeInTexture) textureCheckContentTexture).isAnimating()) {
            return 0;
        }
        return 2;
    }
}
