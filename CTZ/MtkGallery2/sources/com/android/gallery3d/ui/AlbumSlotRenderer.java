package com.android.gallery3d.ui;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AlbumDataLoader;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.ColorTexture;
import com.android.gallery3d.glrenderer.FadeInTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.ui.AlbumSlidingWindow;
import com.mediatek.gallery3d.adapter.FeatureManager;
import com.mediatek.gallery3d.util.FeatureConfig;
import com.mediatek.gallerybasic.base.IAlbumSlotRenderer;

public class AlbumSlotRenderer extends AbstractSlotRenderer {
    private static final int CACHE_SIZE;
    private static final String TAG = "Gallery2/AlbumView";
    public static boolean sPerformanceCaseRunning;
    public static long sWaitFinishedTime;
    private final AbstractGalleryActivity mActivity;
    private boolean mAnimatePressedUp;
    private AlbumSlidingWindow mDataWindow;
    private IAlbumSlotRenderer[] mExtRenders;
    private boolean mHasShowLog;
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

    static {
        CACHE_SIZE = FeatureConfig.sIsLowRamDevice ? 32 : 96;
        sPerformanceCaseRunning = false;
        sWaitFinishedTime = 0L;
    }

    public AlbumSlotRenderer(AbstractGalleryActivity abstractGalleryActivity, SlotView slotView, SelectionManager selectionManager, int i) {
        super(abstractGalleryActivity);
        this.mPressedIndex = -1;
        this.mHighlightItemPath = null;
        this.mHasShowLog = false;
        this.mActivity = abstractGalleryActivity;
        this.mSlotView = slotView;
        this.mSelectionManager = selectionManager;
        this.mPlaceholderColor = i;
        this.mWaitLoadingTexture = new ColorTexture(this.mPlaceholderColor);
        this.mWaitLoadingTexture.setSize(1, 1);
        this.mExtRenders = (IAlbumSlotRenderer[]) FeatureManager.getInstance().getImplement(IAlbumSlotRenderer.class, this.mActivity, this.mActivity.getResources());
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

    public void setModel(AlbumDataLoader albumDataLoader) {
        if (this.mDataWindow != null) {
            this.mDataWindow.setListener(null);
            this.mSlotView.setSlotCount(0);
            this.mDataWindow = null;
        }
        if (albumDataLoader != null) {
            this.mDataWindow = new AlbumSlidingWindow(this.mActivity, albumDataLoader, CACHE_SIZE);
            this.mDataWindow.setListener(new MyDataModelListener());
            this.mSlotView.setSlotCount(albumDataLoader.size());
        }
    }

    private static Texture checkTexture(Texture texture) {
        if (!(texture instanceof TiledTexture) || ((TiledTexture) texture).isReady()) {
            return texture;
        }
        return null;
    }

    @Override
    public int renderSlot(GLCanvas gLCanvas, int i, int i2, int i3, int i4) {
        boolean zRenderContent;
        int i5 = 0;
        if (this.mSlotFilter != null && !this.mSlotFilter.acceptSlot(i)) {
            return 0;
        }
        AlbumSlidingWindow.AlbumEntry albumEntry = this.mDataWindow.get(i);
        Texture textureCheckTexture = checkTexture(albumEntry.content);
        if (textureCheckTexture != null) {
            this.mDataWindow.recycle(albumEntry);
        }
        if (textureCheckTexture == null) {
            textureCheckTexture = this.mWaitLoadingTexture;
            albumEntry.isWaitDisplayed = true;
        } else if (albumEntry.isWaitDisplayed) {
            albumEntry.isWaitDisplayed = false;
            textureCheckTexture = albumEntry.bitmapTexture;
            albumEntry.content = textureCheckTexture;
            sWaitFinishedTime = System.currentTimeMillis();
        }
        Texture texture = textureCheckTexture;
        if (albumEntry.item != null) {
            zRenderContent = false;
            for (IAlbumSlotRenderer iAlbumSlotRenderer : this.mExtRenders) {
                zRenderContent = iAlbumSlotRenderer.renderContent(gLCanvas.getMGLCanvas(), i3, i4, albumEntry.item.getMediaData());
            }
        } else {
            zRenderContent = false;
        }
        if (!zRenderContent) {
            drawContent(gLCanvas, texture, i3, i4, albumEntry.rotation);
        }
        int i6 = ((texture instanceof FadeInTexture) && ((FadeInTexture) texture).isAnimating()) ? 2 : 0;
        if (albumEntry.item != null) {
            IAlbumSlotRenderer[] iAlbumSlotRendererArr = this.mExtRenders;
            int length = iAlbumSlotRendererArr.length;
            int i7 = 0;
            while (i5 < length) {
                i7 |= iAlbumSlotRendererArr[i5].renderCover(gLCanvas.getMGLCanvas(), i3, i4, albumEntry.item.getMediaData()) ? 1 : 0;
                i5++;
            }
            i5 = i7;
        }
        if (i5 == 0) {
            if (albumEntry.mediaType == 4) {
                drawVideoOverlay(gLCanvas, i3, i4);
            }
            if (albumEntry.isPanorama) {
                drawPanoramaIcon(gLCanvas, i3, i4);
            }
        }
        return renderOverlay(gLCanvas, i, albumEntry, i3, i4) | i6;
    }

    private int renderOverlay(GLCanvas gLCanvas, int i, AlbumSlidingWindow.AlbumEntry albumEntry, int i2, int i3) {
        if (this.mPressedIndex == i) {
            if (this.mAnimatePressedUp) {
                if (!this.mHasShowLog && sPerformanceCaseRunning) {
                    Log.d(TAG, "[CMCC Performance test][Gallery2][Gallery] load 1M image time start [" + System.currentTimeMillis() + "]");
                    this.mHasShowLog = true;
                }
                drawPressedUpFrame(gLCanvas, i2, i3);
                if (!isPressedUpFrameFinished()) {
                    return 2;
                }
                this.mAnimatePressedUp = false;
                this.mPressedIndex = -1;
                return 2;
            }
            drawPressedFrame(gLCanvas, i2, i3);
        } else if (albumEntry.path != null && this.mHighlightItemPath == albumEntry.path) {
            drawSelectedFrame(gLCanvas, i2, i3);
        } else if (this.mInSelectionMode && this.mSelectionManager.isItemSelected(albumEntry.path)) {
            drawSelectedFrame(gLCanvas, i2, i3);
        }
        return 0;
    }

    private class MyDataModelListener implements AlbumSlidingWindow.Listener {
        private MyDataModelListener() {
        }

        @Override
        public void onContentChanged() {
            AlbumSlotRenderer.this.mSlotView.invalidate();
        }

        @Override
        public void onSizeChanged(int i) {
            AlbumSlotRenderer.this.mSlotView.setSlotCount(i);
            AlbumSlotRenderer.this.mSlotView.invalidate();
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
    public void onVisibleRangeChanged(int i, int i2) {
        if (this.mDataWindow != null) {
            this.mDataWindow.setActiveWindow(i, i2);
        }
    }

    @Override
    public void onSlotSizeChanged(int i, int i2) {
    }

    public void setSlotFilter(SlotFilter slotFilter) {
        this.mSlotFilter = slotFilter;
    }
}
