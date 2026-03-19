package com.mediatek.gallerybasic.base;

import com.mediatek.gallerybasic.gl.MGLCanvas;

public interface IAlbumSlotRenderer {
    boolean renderContent(MGLCanvas mGLCanvas, int i, int i2, MediaData mediaData);

    boolean renderCover(MGLCanvas mGLCanvas, int i, int i2, MediaData mediaData);
}
