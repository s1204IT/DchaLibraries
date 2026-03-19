package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.util.ThreadPool;

public class ActionImage extends MediaItem {
    private GalleryApp mApplication;
    private int mResourceId;

    public ActionImage(Path path, GalleryApp galleryApp, int i) {
        super(path, nextVersionNumber());
        this.mApplication = (GalleryApp) Utils.checkNotNull(galleryApp);
        this.mResourceId = i;
    }

    @Override
    public ThreadPool.Job<Bitmap> requestImage(int i) {
        return new BitmapJob(i);
    }

    @Override
    public ThreadPool.Job<BitmapRegionDecoder> requestLargeImage() {
        return null;
    }

    private class BitmapJob implements ThreadPool.Job<Bitmap> {
        private int mType;

        protected BitmapJob(int i) {
            this.mType = i;
        }

        @Override
        public Bitmap run(ThreadPool.JobContext jobContext) {
            int targetSize = MediaItem.getTargetSize(this.mType);
            Bitmap bitmapDecodeResource = BitmapFactory.decodeResource(ActionImage.this.mApplication.getResources(), ActionImage.this.mResourceId);
            if (this.mType == 2) {
                return BitmapUtils.resizeAndCropCenter(bitmapDecodeResource, targetSize, true);
            }
            return BitmapUtils.resizeDownBySideLength(bitmapDecodeResource, targetSize, true);
        }
    }

    @Override
    public int getSupportedOperations() {
        return 16384;
    }

    @Override
    public int getMediaType() {
        return 1;
    }

    @Override
    public Uri getContentUri() {
        return null;
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
}
