package com.android.gallery3d.ui;

import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.StringTexture;
import com.android.gallery3d.ui.AlbumSetSlidingWindow;
import com.android.gallery3d.ui.AlbumSetSlotRenderer;

public class ManageCacheDrawer extends AlbumSetSlotRenderer {
    private final int mCachePinMargin;
    private final int mCachePinSize;
    private final StringTexture mCachingText;
    private final ResourceTexture mCheckedItem;
    private final ResourceTexture mLocalAlbumIcon;
    private final SelectionManager mSelectionManager;
    private final ResourceTexture mUnCheckedItem;

    public ManageCacheDrawer(AbstractGalleryActivity activity, SelectionManager selectionManager, SlotView slotView, AlbumSetSlotRenderer.LabelSpec labelSpec, int cachePinSize, int cachePinMargin) {
        super(activity, selectionManager, slotView, labelSpec, activity.getResources().getColor(R.color.cache_placeholder));
        this.mCheckedItem = new ResourceTexture(activity, R.drawable.btn_make_offline_normal_on_holo_dark);
        this.mUnCheckedItem = new ResourceTexture(activity, R.drawable.btn_make_offline_normal_off_holo_dark);
        this.mLocalAlbumIcon = new ResourceTexture(activity, R.drawable.btn_make_offline_disabled_on_holo_dark);
        String cachingLabel = activity.getString(R.string.caching_label);
        this.mCachingText = StringTexture.newInstance(cachingLabel, 12.0f, -1);
        this.mSelectionManager = selectionManager;
        this.mCachePinSize = cachePinSize;
        this.mCachePinMargin = cachePinMargin;
    }

    private static boolean isLocal(int dataSourceType) {
        return dataSourceType != 2;
    }

    @Override
    public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height) {
        AlbumSetSlidingWindow.AlbumSetEntry entry = this.mDataWindow.get(index);
        boolean wantCache = entry.cacheFlag == 2;
        boolean isCaching = wantCache && entry.cacheStatus != 3;
        boolean selected = this.mSelectionManager.isItemSelected(entry.setPath);
        boolean chooseToCache = wantCache ^ selected;
        boolean available = isLocal(entry.sourceType) || chooseToCache;
        if (!available) {
            canvas.save(1);
            canvas.multiplyAlpha(0.6f);
        }
        int renderRequestFlags = 0 | renderContent(canvas, entry, width, height);
        if (!available) {
            canvas.restore();
        }
        int renderRequestFlags2 = renderRequestFlags | renderLabel(canvas, entry, width, height);
        drawCachingPin(canvas, entry.setPath, entry.sourceType, isCaching, chooseToCache, width, height);
        return renderRequestFlags2 | renderOverlay(canvas, index, entry, width, height);
    }

    private void drawCachingPin(GLCanvas canvas, Path path, int dataSourceType, boolean isCaching, boolean chooseToCache, int width, int height) {
        ResourceTexture icon;
        if (isLocal(dataSourceType)) {
            icon = this.mLocalAlbumIcon;
        } else if (chooseToCache) {
            icon = this.mCheckedItem;
        } else {
            icon = this.mUnCheckedItem;
        }
        int s = this.mCachePinSize;
        int m = this.mCachePinMargin;
        icon.draw(canvas, (width - m) - s, height - s, s, s);
        if (isCaching) {
            int w = this.mCachingText.getWidth();
            int h = this.mCachingText.getHeight();
            this.mCachingText.draw(canvas, (width - w) / 2, height - h);
        }
    }
}
