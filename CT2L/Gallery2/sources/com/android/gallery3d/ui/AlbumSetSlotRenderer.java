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

public class AlbumSetSlotRenderer extends AbstractSlotRenderer {
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

    public AlbumSetSlotRenderer(AbstractGalleryActivity activity, SelectionManager selectionManager, SlotView slotView, LabelSpec labelSpec, int placeholderColor) {
        super(activity);
        this.mPressedIndex = -1;
        this.mHighlightItemPath = null;
        this.mActivity = activity;
        this.mSelectionManager = selectionManager;
        this.mSlotView = slotView;
        this.mLabelSpec = labelSpec;
        this.mPlaceholderColor = placeholderColor;
        this.mWaitLoadingTexture = new ColorTexture(this.mPlaceholderColor);
        this.mWaitLoadingTexture.setSize(1, 1);
        this.mCameraOverlay = new ResourceTexture(activity, R.drawable.ic_cameraalbum_overlay);
    }

    public void setPressedIndex(int index) {
        if (this.mPressedIndex != index) {
            this.mPressedIndex = index;
            this.mSlotView.invalidate();
        }
    }

    public void setPressedUp() {
        if (this.mPressedIndex != -1) {
            this.mAnimatePressedUp = true;
            this.mSlotView.invalidate();
        }
    }

    public void setHighlightItemPath(Path path) {
        if (this.mHighlightItemPath != path) {
            this.mHighlightItemPath = path;
            this.mSlotView.invalidate();
        }
    }

    public void setModel(AlbumSetDataLoader model) {
        if (this.mDataWindow != null) {
            this.mDataWindow.setListener(null);
            this.mDataWindow = null;
            this.mSlotView.setSlotCount(0);
        }
        if (model != null) {
            this.mDataWindow = new AlbumSetSlidingWindow(this.mActivity, model, this.mLabelSpec, 96);
            this.mDataWindow.setListener(new MyCacheListener());
            this.mSlotView.setSlotCount(this.mDataWindow.size());
        }
    }

    private static Texture checkLabelTexture(Texture texture) {
        if ((texture instanceof UploadedTexture) && ((UploadedTexture) texture).isUploading()) {
            return null;
        }
        return texture;
    }

    private static Texture checkContentTexture(Texture texture) {
        if (!(texture instanceof TiledTexture) || ((TiledTexture) texture).isReady()) {
            return texture;
        }
        return null;
    }

    @Override
    public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height) {
        AlbumSetSlidingWindow.AlbumSetEntry entry = this.mDataWindow.get(index);
        int renderRequestFlags = 0 | renderContent(canvas, entry, width, height);
        return renderRequestFlags | renderLabel(canvas, entry, width, height) | renderOverlay(canvas, index, entry, width, height);
    }

    protected int renderOverlay(GLCanvas canvas, int index, AlbumSetSlidingWindow.AlbumSetEntry entry, int width, int height) {
        int renderRequestFlags = 0;
        if (entry.album != null && entry.album.isCameraRoll()) {
            int uncoveredHeight = height - this.mLabelSpec.labelBackgroundHeight;
            int dim = uncoveredHeight / 2;
            this.mCameraOverlay.draw(canvas, (width - dim) / 2, (uncoveredHeight - dim) / 2, dim, dim);
        }
        if (this.mPressedIndex == index) {
            if (this.mAnimatePressedUp) {
                drawPressedUpFrame(canvas, width, height);
                renderRequestFlags = 0 | 2;
                if (isPressedUpFrameFinished()) {
                    this.mAnimatePressedUp = false;
                    this.mPressedIndex = -1;
                }
            } else {
                drawPressedFrame(canvas, width, height);
            }
        } else if (this.mHighlightItemPath != null && this.mHighlightItemPath == entry.setPath) {
            drawSelectedFrame(canvas, width, height);
        } else if (this.mInSelectionMode && this.mSelectionManager.isItemSelected(entry.setPath)) {
            drawSelectedFrame(canvas, width, height);
        }
        return renderRequestFlags;
    }

    protected int renderContent(GLCanvas canvas, AlbumSetSlidingWindow.AlbumSetEntry entry, int width, int height) {
        Texture content = checkContentTexture(entry.content);
        if (content == null) {
            content = this.mWaitLoadingTexture;
            entry.isWaitLoadingDisplayed = true;
        } else if (entry.isWaitLoadingDisplayed) {
            entry.isWaitLoadingDisplayed = false;
            content = new FadeInTexture(this.mPlaceholderColor, entry.bitmapTexture);
            entry.content = content;
        }
        drawContent(canvas, content, width, height, entry.rotation);
        if (!(content instanceof FadeInTexture) || !((FadeInTexture) content).isAnimating()) {
            return 0;
        }
        int renderRequestFlags = 0 | 2;
        return renderRequestFlags;
    }

    protected int renderLabel(GLCanvas canvas, AlbumSetSlidingWindow.AlbumSetEntry entry, int width, int height) {
        Texture content = checkLabelTexture(entry.labelTexture);
        if (content == null) {
            content = this.mWaitLoadingTexture;
        }
        int b = AlbumLabelMaker.getBorderSize();
        int h = this.mLabelSpec.labelBackgroundHeight;
        content.draw(canvas, -b, (height - h) + b, width + b + b, h);
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
        public void onSizeChanged(int size) {
            AlbumSetSlotRenderer.this.mSlotView.setSlotCount(size);
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
    public void onVisibleRangeChanged(int visibleStart, int visibleEnd) {
        if (this.mDataWindow != null) {
            this.mDataWindow.setActiveWindow(visibleStart, visibleEnd);
        }
    }

    @Override
    public void onSlotSizeChanged(int width, int height) {
        if (this.mDataWindow != null) {
            this.mDataWindow.onSlotSizeChanged(width, height);
        }
    }
}
