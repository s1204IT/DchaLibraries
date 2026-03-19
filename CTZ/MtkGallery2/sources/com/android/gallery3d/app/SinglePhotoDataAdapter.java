package com.android.gallery3d.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.UriImage;
import com.android.gallery3d.ui.BitmapScreenNail;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.ui.TileImageViewAdapter;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.ThreadPool;
import com.mediatek.gallery3d.adapter.FeatureHelper;

public class SinglePhotoDataAdapter extends TileImageViewAdapter implements PhotoPage.Model {
    private BitmapScreenNail mBitmapScreenNail;
    private Handler mHandler;
    private boolean mHasFullImage;
    private MediaItem mItem;
    private PhotoView mPhotoView;
    private Future<?> mTask;
    private ThreadPool mThreadPool;
    private int mLoadingState = 0;
    private FutureListener<BitmapRegionDecoder> mLargeListener = new FutureListener<BitmapRegionDecoder>() {
        @Override
        public void onFutureDone(Future<BitmapRegionDecoder> future) {
            BitmapRegionDecoder bitmapRegionDecoder = future.get();
            if (bitmapRegionDecoder == null) {
                com.android.gallery3d.util.Log.d("Gallery2/SinglePhotoDataAdapter", "<mLargeListener.onFutureDone> get RegionDecoder fail, uri = " + SinglePhotoDataAdapter.this.mItem.getContentUri() + ", try to decode thumb");
                SinglePhotoDataAdapter.this.mHasFullImage = false;
                SinglePhotoDataAdapter.this.mTask = SinglePhotoDataAdapter.this.mThreadPool.submit(SinglePhotoDataAdapter.this.mItem.requestImage(1), SinglePhotoDataAdapter.this.mThumbListener);
                return;
            }
            int width = bitmapRegionDecoder.getWidth();
            int height = bitmapRegionDecoder.getHeight();
            if (SinglePhotoDataAdapter.this.mLoadingState == 2 && FeatureHelper.isJpegOutOfLimit(SinglePhotoDataAdapter.this.mItem.getMimeType(), width, height)) {
                com.android.gallery3d.util.Log.d("Gallery2/SinglePhotoDataAdapter", String.format("out of limitation: %s [mime type: %s, width: %d, height: %d]", SinglePhotoDataAdapter.this.mItem.getPath().toString(), SinglePhotoDataAdapter.this.mItem.getMimeType(), Integer.valueOf(width), Integer.valueOf(height)));
                bitmapRegionDecoder.recycle();
            } else {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = BitmapUtils.computeSampleSize(1024.0f / Math.max(width, height));
                SinglePhotoDataAdapter.this.mHandler.sendMessage(SinglePhotoDataAdapter.this.mHandler.obtainMessage(1, new ImageBundle(bitmapRegionDecoder, com.mediatek.gallerybasic.util.BitmapUtils.replaceBackgroundColor(bitmapRegionDecoder.decodeRegion(new Rect(0, 0, width, height), options), true))));
            }
        }
    };
    private FutureListener<Bitmap> mThumbListener = new FutureListener<Bitmap>() {
        @Override
        public void onFutureDone(Future<Bitmap> future) {
            SinglePhotoDataAdapter.this.mHandler.sendMessage(SinglePhotoDataAdapter.this.mHandler.obtainMessage(1, future));
        }
    };

    public SinglePhotoDataAdapter(AbstractGalleryActivity abstractGalleryActivity, PhotoView photoView, MediaItem mediaItem) {
        this.mItem = (MediaItem) Utils.checkNotNull(mediaItem);
        this.mHasFullImage = (mediaItem.getSupportedOperations() & 64) != 0;
        com.android.gallery3d.util.Log.d("Gallery2/SinglePhotoDataAdapter", "<SinglePhotoDataAdapter> hasFullImage " + this.mHasFullImage);
        this.mPhotoView = (PhotoView) Utils.checkNotNull(photoView);
        this.mHandler = new SynchronizedHandler(abstractGalleryActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.assertTrue(message.what == 1);
                if (message.obj != null) {
                    if (message.obj instanceof ImageBundle) {
                        SinglePhotoDataAdapter.this.onDecodeLargeComplete((ImageBundle) message.obj);
                    } else {
                        SinglePhotoDataAdapter.this.onDecodeThumbComplete((Future) message.obj);
                    }
                }
            }
        };
        this.mThreadPool = abstractGalleryActivity.getThreadPool();
    }

    private static class ImageBundle {
        public final Bitmap backupImage;
        public final BitmapRegionDecoder decoder;

        public ImageBundle(BitmapRegionDecoder bitmapRegionDecoder, Bitmap bitmap) {
            this.decoder = bitmapRegionDecoder;
            this.backupImage = bitmap;
        }
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    private void setScreenNail(Bitmap bitmap, int i, int i2) {
        this.mBitmapScreenNail = new BitmapScreenNail(bitmap, this.mItem);
        setScreenNail(this.mBitmapScreenNail, i, i2);
    }

    private void onDecodeLargeComplete(ImageBundle imageBundle) {
        try {
            setScreenNail(imageBundle.backupImage, imageBundle.decoder.getWidth(), imageBundle.decoder.getHeight());
            setRegionDecoder(imageBundle.decoder);
            this.mPhotoView.notifyImageChange(0);
        } catch (Throwable th) {
            com.android.gallery3d.util.Log.w("Gallery2/SinglePhotoDataAdapter", "fail to decode large", th);
        }
    }

    private void onDecodeThumbComplete(Future<Bitmap> future) {
        try {
            Bitmap bitmap = future.get();
            if (bitmap == null) {
                this.mLoadingState = 2;
                this.mPhotoView.notifyImageChange(0);
            } else {
                this.mLoadingState = 1;
                setScreenNail(bitmap, bitmap.getWidth(), bitmap.getHeight());
                this.mPhotoView.notifyImageChange(0);
            }
        } catch (Throwable th) {
            com.android.gallery3d.util.Log.w("Gallery2/SinglePhotoDataAdapter", "fail to decode thumb", th);
        }
    }

    @Override
    public void resume() {
        if (this.mItem != null && (this.mItem instanceof UriImage)) {
            ((UriImage) this.mItem).updateMediaData();
        }
        if (this.mTask == null) {
            if (this.mHasFullImage) {
                this.mTask = this.mThreadPool.submit(this.mItem.requestLargeImage(), this.mLargeListener);
            } else {
                this.mTask = this.mThreadPool.submit(this.mItem.requestImage(1), this.mThumbListener);
            }
        }
    }

    @Override
    public void pause() {
        Future<?> future = this.mTask;
        future.cancel();
        future.waitDone();
        this.mTask = null;
        if (this.mBitmapScreenNail != null) {
            this.mBitmapScreenNail.recycle();
            this.mBitmapScreenNail = null;
        }
        this.mLoadingState = 0;
        clearAndRecycle();
    }

    @Override
    public void moveTo(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getImageSize(int i, PhotoView.Size size) {
        if (i == 0) {
            size.width = this.mItem.getWidth();
            size.height = this.mItem.getHeight();
        } else {
            size.width = 0;
            size.height = 0;
        }
    }

    @Override
    public int getImageRotation(int i) {
        if (i == 0) {
            return this.mItem.getFullImageRotation();
        }
        return 0;
    }

    @Override
    public ScreenNail getScreenNail(int i) {
        if (i == 0) {
            return getScreenNail();
        }
        return null;
    }

    @Override
    public void setNeedFullImage(boolean z) {
    }

    @Override
    public boolean isCamera(int i) {
        return false;
    }

    @Override
    public boolean isPanorama(int i) {
        return false;
    }

    @Override
    public boolean isStaticCamera(int i) {
        return false;
    }

    @Override
    public boolean isVideo(int i) {
        return this.mItem.getMediaType() == 4;
    }

    @Override
    public boolean isDeletable(int i) {
        return false;
    }

    @Override
    public MediaItem getMediaItem(int i) {
        if (i == 0) {
            return this.mItem;
        }
        return null;
    }

    @Override
    public int getCurrentIndex() {
        return 0;
    }

    @Override
    public void setCurrentPhoto(Path path, int i) {
    }

    @Override
    public void setFocusHintDirection(int i) {
    }

    @Override
    public void setFocusHintPath(Path path) {
    }

    @Override
    public int getLoadingState(int i) {
        return this.mLoadingState;
    }

    @Override
    public int getImageHeight() {
        return this.mItem.getHeight();
    }

    @Override
    public int getImageWidth() {
        return this.mItem.getWidth();
    }
}
