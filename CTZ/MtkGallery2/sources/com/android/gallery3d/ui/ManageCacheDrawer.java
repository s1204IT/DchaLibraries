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

    public ManageCacheDrawer(AbstractGalleryActivity abstractGalleryActivity, SelectionManager selectionManager, SlotView slotView, AlbumSetSlotRenderer.LabelSpec labelSpec, int i, int i2) {
        super(abstractGalleryActivity, selectionManager, slotView, labelSpec, abstractGalleryActivity.getResources().getColor(R.color.cache_placeholder));
        this.mCheckedItem = new ResourceTexture(abstractGalleryActivity, R.drawable.btn_make_offline_normal_on_holo_dark);
        this.mUnCheckedItem = new ResourceTexture(abstractGalleryActivity, R.drawable.btn_make_offline_normal_off_holo_dark);
        this.mLocalAlbumIcon = new ResourceTexture(abstractGalleryActivity, R.drawable.btn_make_offline_disabled_on_holo_dark);
        this.mCachingText = StringTexture.newInstance(abstractGalleryActivity.getString(R.string.caching_label), 12.0f, -1);
        this.mSelectionManager = selectionManager;
        this.mCachePinSize = i;
        this.mCachePinMargin = i2;
    }

    private static boolean isLocal(int i) {
        return i != 2;
    }

    @Override
    public int renderSlot(GLCanvas gLCanvas, int i, int i2, int i3, int i4) {
        AlbumSetSlidingWindow.AlbumSetEntry albumSetEntry = this.mDataWindow.get(i);
        boolean z = albumSetEntry.cacheFlag == 2;
        boolean z2 = z && albumSetEntry.cacheStatus != 3;
        boolean zIsItemSelected = z ^ this.mSelectionManager.isItemSelected(albumSetEntry.setPath);
        boolean z3 = isLocal(albumSetEntry.sourceType) || zIsItemSelected;
        if (!z3) {
            gLCanvas.save(1);
            gLCanvas.multiplyAlpha(0.6f);
        }
        int iRenderContent = 0 | renderContent(gLCanvas, albumSetEntry, i3, i4);
        if (!z3) {
            gLCanvas.restore();
        }
        int iRenderLabel = iRenderContent | renderLabel(gLCanvas, albumSetEntry, i3, i4);
        drawCachingPin(gLCanvas, albumSetEntry.setPath, albumSetEntry.sourceType, z2, zIsItemSelected, i3, i4);
        return renderOverlay(gLCanvas, i, albumSetEntry, i3, i4) | iRenderLabel;
    }

    private void drawCachingPin(GLCanvas gLCanvas, Path path, int i, boolean z, boolean z2, int i2, int i3) {
        ResourceTexture resourceTexture;
        if (isLocal(i)) {
            resourceTexture = this.mLocalAlbumIcon;
        } else if (z2) {
            resourceTexture = this.mCheckedItem;
        } else {
            resourceTexture = this.mUnCheckedItem;
        }
        ResourceTexture resourceTexture2 = resourceTexture;
        int i4 = this.mCachePinSize;
        resourceTexture2.draw(gLCanvas, (i2 - this.mCachePinMargin) - i4, i3 - i4, i4, i4);
        if (z) {
            this.mCachingText.draw(gLCanvas, (i2 - this.mCachingText.getWidth()) / 2, i3 - this.mCachingText.getHeight());
        }
    }
}
