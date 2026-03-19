package com.mediatek.gallerybasic.base;

import com.mediatek.gallerybasic.gl.MGLCanvas;

public interface ISlideshowRenderer {
    boolean renderCurrentContent(MGLCanvas mGLCanvas, int i, int i2, MediaData mediaData);

    boolean renderCurrentCover(MGLCanvas mGLCanvas, int i, int i2, int i3, int i4, MediaData mediaData, float f);

    boolean renderPrevContent(MGLCanvas mGLCanvas, int i, int i2, MediaData mediaData);

    boolean renderPrevCover(MGLCanvas mGLCanvas, int i, int i2, int i3, int i4, MediaData mediaData, float f);
}
