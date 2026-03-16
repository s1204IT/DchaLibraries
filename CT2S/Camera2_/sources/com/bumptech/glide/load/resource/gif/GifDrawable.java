package com.bumptech.glide.load.resource.gif;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.resource.gif.GifFrameManager;

public class GifDrawable extends Drawable implements Animatable, GifFrameManager.FrameCallback {
    private Bitmap currentFrame;
    private GifDecoder decoder;
    private final GifFrameManager frameManager;
    private boolean isRecycled;
    private boolean isRunning;
    private int width = -1;
    private int height = -1;
    private final Paint paint = new Paint();

    public GifDrawable(GifDecoder decoder, GifFrameManager frameManager) {
        this.decoder = decoder;
        this.frameManager = frameManager;
    }

    @Override
    public void start() {
        if (!this.isRunning) {
            this.isRunning = true;
            this.frameManager.getNextFrame(this);
            invalidateSelf();
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        if (!visible) {
            stop();
        } else {
            start();
        }
        return super.setVisible(visible, restart);
    }

    @Override
    public int getIntrinsicWidth() {
        return this.width;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.height;
    }

    @Override
    public void stop() {
        this.isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.currentFrame != null) {
            canvas.drawBitmap(this.currentFrame, 0.0f, 0.0f, this.paint);
        }
    }

    @Override
    public void setAlpha(int i) {
        this.paint.setAlpha(i);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return this.decoder.isTransparent() ? -2 : -1;
    }

    @Override
    @TargetApi(11)
    public void onFrameRead(Bitmap frame) {
        if (Build.VERSION.SDK_INT >= 11 && getCallback() == null) {
            stop();
            return;
        }
        if (this.isRunning) {
            if (this.width == -1) {
                this.width = frame.getWidth();
            }
            if (this.height == -1) {
                this.height = frame.getHeight();
            }
            if (frame != null) {
                this.currentFrame = frame;
                invalidateSelf();
            }
            this.frameManager.getNextFrame(this);
        }
    }

    public void recycle() {
        this.isRecycled = true;
        this.frameManager.clear();
    }

    public boolean isRecycled() {
        return this.isRecycled;
    }
}
