package com.mediatek.galleryraw;

import android.content.Context;
import android.content.res.Resources;
import com.mediatek.gallerybasic.base.IAlbumSlotRenderer;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.gl.MGLCanvas;
import com.mediatek.gallerybasic.gl.MResourceTexture;
import com.mediatek.gallerybasic.util.RenderUtils;

public class RawAlbumSlotRenderer implements IAlbumSlotRenderer {
    private static MResourceTexture sRawOverlay = null;
    private Context mContext;
    private Resources mResources;

    public RawAlbumSlotRenderer(Context context, Resources resources) {
        this.mContext = context;
        this.mResources = resources;
    }

    @Override
    public boolean renderContent(MGLCanvas mGLCanvas, int i, int i2, MediaData mediaData) {
        return false;
    }

    @Override
    public boolean renderCover(MGLCanvas mGLCanvas, int i, int i2, MediaData mediaData) {
        if (mediaData.mediaType.getMainType() != RawMember.sType) {
            return false;
        }
        if (sRawOverlay == null) {
            sRawOverlay = new MResourceTexture(this.mResources, R.drawable.ic_raw);
        }
        RenderUtils.renderOverlayOnSlot(mGLCanvas, sRawOverlay, i, i2);
        return true;
    }
}
