package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.util.ThreadPool;

public class SnailItem extends MediaItem {
    private ScreenNail mScreenNail;

    public SnailItem(Path path) {
        super(path, nextVersionNumber());
    }

    @Override
    public ThreadPool.Job<Bitmap> requestImage(int type) {
        return new ThreadPool.Job<Bitmap>() {
            @Override
            public Bitmap run(ThreadPool.JobContext jc) {
                return null;
            }
        };
    }

    @Override
    public ThreadPool.Job<BitmapRegionDecoder> requestLargeImage() {
        return new ThreadPool.Job<BitmapRegionDecoder>() {
            @Override
            public BitmapRegionDecoder run(ThreadPool.JobContext jc) {
                return null;
            }
        };
    }

    @Override
    public ScreenNail getScreenNail() {
        return this.mScreenNail;
    }

    @Override
    public String getMimeType() {
        return "";
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    public void setScreenNail(ScreenNail screenNail) {
        this.mScreenNail = screenNail;
    }

    public void updateVersion() {
        this.mDataVersion = nextVersionNumber();
    }
}
