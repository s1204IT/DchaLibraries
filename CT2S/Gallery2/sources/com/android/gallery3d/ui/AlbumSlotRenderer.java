package com.android.gallery3d.ui;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AlbumDataLoader;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.ColorTexture;
import com.android.gallery3d.glrenderer.FadeInTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.ui.AlbumSlidingWindow;

public class AlbumSlotRenderer extends AbstractSlotRenderer {
    private final AbstractGalleryActivity mActivity;
    private boolean mAnimatePressedUp;
    private AlbumSlidingWindow mDataWindow;
    private Path mHighlightItemPath;
    private boolean mInSelectionMode;
    private final int mPlaceholderColor;
    private int mPressedIndex;
    private final SelectionManager mSelectionManager;
    private SlotFilter mSlotFilter;
    private final SlotView mSlotView;
    private final ColorTexture mWaitLoadingTexture;

    public interface SlotFilter {
        boolean acceptSlot(int i);
    }

    public AlbumSlotRenderer(AbstractGalleryActivity activity, SlotView slotView, SelectionManager selectionManager, int placeholderColor) {
        super(activity);
        this.mPressedIndex = -1;
        this.mHighlightItemPath = null;
        this.mActivity = activity;
        this.mSlotView = slotView;
        this.mSelectionManager = selectionManager;
        this.mPlaceholderColor = placeholderColor;
        this.mWaitLoadingTexture = new ColorTexture(this.mPlaceholderColor);
        this.mWaitLoadingTexture.setSize(1, 1);
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

    public void setModel(AlbumDataLoader model) {
        if (this.mDataWindow != null) {
            this.mDataWindow.setListener(null);
            this.mSlotView.setSlotCount(0);
            this.mDataWindow = null;
        }
        if (model != null) {
            this.mDataWindow = new AlbumSlidingWindow(this.mActivity, model, 96);
            this.mDataWindow.setListener(new MyDataModelListener());
            this.mSlotView.setSlotCount(model.size());
        }
    }

    private static Texture checkTexture(Texture texture) {
        if (!(texture instanceof TiledTexture) || ((TiledTexture) texture).isReady()) {
            return texture;
        }
        return null;
    }

    @Override
    public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height) {
        if (this.mSlotFilter != null && !this.mSlotFilter.acceptSlot(index)) {
            return 0;
        }
        AlbumSlidingWindow.AlbumEntry entry = this.mDataWindow.get(index);
        int renderRequestFlags = 0;
        Texture content = checkTexture(entry.content);
        if (content == null) {
            content = this.mWaitLoadingTexture;
            entry.isWaitDisplayed = true;
        } else if (entry.isWaitDisplayed) {
            entry.isWaitDisplayed = false;
            content = new FadeInTexture(this.mPlaceholderColor, entry.bitmapTexture);
            entry.content = content;
        }
        drawContent(canvas, content, width, height, entry.rotation);
        if ((content instanceof FadeInTexture) && ((FadeInTexture) content).isAnimating()) {
            renderRequestFlags = 0 | 2;
        }
        if (entry.mediaType == 4) {
            drawVideoOverlay(canvas, width, height);
        } else if (entry.item != null && BitmapUtils.isGifPicture(entry.item.getMimeType())) {
            drawVideoWithoutOverlay(canvas, width, height);
        }
        if (entry.isPanorama) {
            drawPanoramaIcon(canvas, width, height);
        }
        return renderRequestFlags | renderOverlay(canvas, index, entry, width, height);
    }

    private int renderOverlay(GLCanvas canvas, int index, AlbumSlidingWindow.AlbumEntry entry, int width, int height) {
        int renderRequestFlags = 0;
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
        } else if (entry.path != null && this.mHighlightItemPath == entry.path) {
            drawSelectedFrame(canvas, width, height);
        } else if (this.mInSelectionMode && this.mSelectionManager.isItemSelected(entry.path)) {
            drawSelectedFrame(canvas, width, height);
        }
        return renderRequestFlags;
    }

    private class MyDataModelListener implements AlbumSlidingWindow.Listener {
        private MyDataModelListener() {
        }

        @Override
        public void onContentChanged() {
            AlbumSlotRenderer.this.mSlotView.invalidate();
        }

        @Override
        public void onSizeChanged(int size) {
            AlbumSlotRenderer.this.mSlotView.setSlotCount(size);
        }
    }

    public void resume() {
        this.mDataWindow.resume();
    }

    public void pause() {
        this.mDataWindow.pause();
    }

    @Override
    public void prepareDrawing() {
        this.mInSelectionMode = this.mSelectionManager.inSelectionMode();
    }

    @Override
    public void onVisibleRangeChanged(int visibleStart, int visibleEnd) {
        if (this.mDataWindow != null) {
            this.mDataWindow.setActiveWindow(visibleStart, visibleEnd);
        }
    }

    @Override
    public void onSlotSizeChanged(int width, int height) {
    }

    public void setSlotFilter(SlotFilter slotFilter) {
        this.mSlotFilter = slotFilter;
    }
}
