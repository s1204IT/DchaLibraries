package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Rect;
import com.android.gallery3d.R;
import com.android.gallery3d.glrenderer.FadeOutTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.NinePatchTexture;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.ui.SlotView;
import com.mediatek.gallery3d.layout.FancyHelper;

public abstract class AbstractSlotRenderer implements SlotView.SlotRenderer {
    private final NinePatchTexture mFramePressed;
    private FadeOutTexture mFramePressedUp;
    private final NinePatchTexture mFrameSelected;
    private final ResourceTexture mPanoramaIcon;
    private final ResourceTexture mVideoOverlay;
    private final ResourceTexture mVideoPlayIcon;

    protected AbstractSlotRenderer(Context context) {
        this.mVideoOverlay = new ResourceTexture(context, R.drawable.ic_video_thumb);
        this.mVideoPlayIcon = new ResourceTexture(context, R.drawable.ic_gallery_play);
        this.mPanoramaIcon = new ResourceTexture(context, R.drawable.ic_360pano_holo_light);
        this.mFramePressed = new NinePatchTexture(context, R.drawable.grid_pressed);
        this.mFrameSelected = new NinePatchTexture(context, R.drawable.grid_selected);
    }

    protected void drawContent(GLCanvas gLCanvas, Texture texture, int i, int i2, int i3) {
        gLCanvas.save(2);
        int iMin = Math.min(i, i2);
        if (i3 != 0) {
            float f = iMin / 2;
            gLCanvas.translate(f, f);
            gLCanvas.rotate(i3, 0.0f, 0.0f, 1.0f);
            float f2 = (-iMin) / 2;
            gLCanvas.translate(f2, f2);
        }
        float f3 = iMin;
        float fMin = Math.min(f3 / texture.getWidth(), f3 / texture.getHeight());
        gLCanvas.scale(fMin, fMin, 1.0f);
        texture.draw(gLCanvas, 0, 0);
        gLCanvas.restore();
    }

    protected void drawVideoOverlay(GLCanvas gLCanvas, int i, int i2) {
        if (FancyHelper.isFancyLayoutSupported()) {
            gLCanvas.save(2);
            int i3 = i2 / ((int) 47.0f);
            for (int i4 = 0; i4 < i3; i4++) {
                gLCanvas.fillRect(10.0f, 27.0f + (i4 * 47.0f), 20.0f, 20.0f, -13487566);
            }
            gLCanvas.restore();
            return;
        }
        float height = i2 / r2.getHeight();
        this.mVideoOverlay.draw(gLCanvas, 0, 0, Math.round(r2.getWidth() * height), Math.round(height * r2.getHeight()));
    }

    protected void drawPanoramaIcon(GLCanvas gLCanvas, int i, int i2) {
        int iMin = Math.min(i, i2) / 6;
        this.mPanoramaIcon.draw(gLCanvas, (i - iMin) / 2, (i2 - iMin) / 2, iMin, iMin);
    }

    protected boolean isPressedUpFrameFinished() {
        if (this.mFramePressedUp != null) {
            if (this.mFramePressedUp.isAnimating()) {
                return false;
            }
            this.mFramePressedUp = null;
            return true;
        }
        return true;
    }

    protected void drawPressedUpFrame(GLCanvas gLCanvas, int i, int i2) {
        if (this.mFramePressedUp == null) {
            this.mFramePressedUp = new FadeOutTexture(this.mFramePressed);
        }
        drawFrame(gLCanvas, this.mFramePressed.getPaddings(), this.mFramePressedUp, 0, 0, i, i2);
    }

    protected void drawPressedFrame(GLCanvas gLCanvas, int i, int i2) {
        drawFrame(gLCanvas, this.mFramePressed.getPaddings(), this.mFramePressed, 0, 0, i, i2);
    }

    protected void drawSelectedFrame(GLCanvas gLCanvas, int i, int i2) {
        drawFrame(gLCanvas, this.mFrameSelected.getPaddings(), this.mFrameSelected, 0, 0, i, i2);
    }

    protected static void drawFrame(GLCanvas gLCanvas, Rect rect, Texture texture, int i, int i2, int i3, int i4) {
        texture.draw(gLCanvas, i - rect.left, i2 - rect.top, i3 + rect.left + rect.right, i4 + rect.top + rect.bottom);
    }
}
