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

    protected void drawContent(GLCanvas canvas, Texture content, int width, int height, int rotation) {
        canvas.save(2);
        int height2 = Math.min(width, height);
        if (rotation != 0) {
            canvas.translate(height2 / 2, height2 / 2);
            canvas.rotate(rotation, 0.0f, 0.0f, 1.0f);
            canvas.translate((-height2) / 2, (-height2) / 2);
        }
        float scale = Math.min(height2 / content.getWidth(), height2 / content.getHeight());
        canvas.scale(scale, scale, 1.0f);
        content.draw(canvas, 0, 0);
        canvas.restore();
    }

    protected void drawVideoOverlay(GLCanvas canvas, int width, int height) {
        ResourceTexture v = this.mVideoOverlay;
        float scale = height / v.getHeight();
        int w = Math.round(v.getWidth() * scale);
        int h = Math.round(v.getHeight() * scale);
        v.draw(canvas, 0, 0, w, h);
        int s = Math.min(width, height) / 6;
        this.mVideoPlayIcon.draw(canvas, (width - s) / 2, (height - s) / 2, s, s);
    }

    protected void drawVideoWithoutOverlay(GLCanvas canvas, int width, int height) {
        int s = Math.min(width, height) / 6;
        this.mVideoPlayIcon.draw(canvas, (width - s) / 2, (height - s) / 2, s, s);
    }

    protected void drawPanoramaIcon(GLCanvas canvas, int width, int height) {
        int iconSize = Math.min(width, height) / 6;
        this.mPanoramaIcon.draw(canvas, (width - iconSize) / 2, (height - iconSize) / 2, iconSize, iconSize);
    }

    protected boolean isPressedUpFrameFinished() {
        if (this.mFramePressedUp != null) {
            if (this.mFramePressedUp.isAnimating()) {
                return false;
            }
            this.mFramePressedUp = null;
        }
        return true;
    }

    protected void drawPressedUpFrame(GLCanvas canvas, int width, int height) {
        if (this.mFramePressedUp == null) {
            this.mFramePressedUp = new FadeOutTexture(this.mFramePressed);
        }
        drawFrame(canvas, this.mFramePressed.getPaddings(), this.mFramePressedUp, 0, 0, width, height);
    }

    protected void drawPressedFrame(GLCanvas canvas, int width, int height) {
        drawFrame(canvas, this.mFramePressed.getPaddings(), this.mFramePressed, 0, 0, width, height);
    }

    protected void drawSelectedFrame(GLCanvas canvas, int width, int height) {
        drawFrame(canvas, this.mFrameSelected.getPaddings(), this.mFrameSelected, 0, 0, width, height);
    }

    protected static void drawFrame(GLCanvas canvas, Rect padding, Texture frame, int x, int y, int width, int height) {
        frame.draw(canvas, x - padding.left, y - padding.top, padding.left + width + padding.right, padding.top + height + padding.bottom);
    }
}
