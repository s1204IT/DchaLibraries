package com.mediatek.gallerybasic.util;

import com.mediatek.gallerybasic.gl.MGLCanvas;
import com.mediatek.gallerybasic.gl.MTexture;

public class RenderUtils {
    public static void renderOverlayOnSlot(MGLCanvas mGLCanvas, MTexture mTexture, int i, int i2) {
        int iMin = Math.min(i, i2) / 5;
        mTexture.draw(mGLCanvas, iMin / 4, i2 - ((iMin * 5) / 4), iMin, iMin);
    }
}
