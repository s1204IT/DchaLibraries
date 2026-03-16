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
import com.android.gallery3d.ui.BitmapScreenNail;
import com.android.gallery3d.ui.PhotoView;
import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.ui.TileImageViewAdapter;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.ThreadPool;

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
            BitmapRegionDecoder decoder = future.get();
            if (decoder != null) {
                int width = decoder.getWidth();
                int height = decoder.getHeight();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = BitmapUtils.computeSampleSize(1024.0f / Math.max(width, height));
                Bitmap bitmap = decoder.decodeRegion(new Rect(0, 0, width, height), options);
                SinglePhotoDataAdapter.this.mHandler.sendMessage(SinglePhotoDataAdapter.this.mHandler.obtainMessage(1, new ImageBundle(decoder, bitmap)));
            }
        }
    };
    private FutureListener<Bitmap> mThumbListener = new FutureListener<Bitmap>() {
        @Override
        public void onFutureDone(Future<Bitmap> future) {
            SinglePhotoDataAdapter.this.mHandler.sendMessage(SinglePhotoDataAdapter.this.mHandler.obtainMessage(1, future));
        }
    };

    public SinglePhotoDataAdapter(AbstractGalleryActivity activity, PhotoView view, MediaItem item) {
        this.mItem = (MediaItem) Utils.checkNotNull(item);
        this.mHasFullImage = (item.getSupportedOperations() & 64) != 0;
        this.mPhotoView = (PhotoView) Utils.checkNotNull(view);
        this.mHandler = new SynchronizedHandler(activity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                Utils.assertTrue(message.what == 1);
                if (SinglePhotoDataAdapter.this.mHasFullImage) {
                    SinglePhotoDataAdapter.this.onDecodeLargeComplete((ImageBundle) message.obj);
                } else {
                    SinglePhotoDataAdapter.this.onDecodeThumbComplete((Future) message.obj);
                }
            }
        };
        this.mThreadPool = activity.getThreadPool();
    }

    private static class ImageBundle {
        public final Bitmap backupImage;
        public final BitmapRegionDecoder decoder;

        public ImageBundle(BitmapRegionDecoder decoder, Bitmap backupImage) {
            this.decoder = decoder;
            this.backupImage = backupImage;
        }
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    private void setScreenNail(Bitmap bitmap, int width, int height) {
        this.mBitmapScreenNail = new BitmapScreenNail(bitmap);
        setScreenNail(this.mBitmapScreenNail, width, height);
    }

    private void onDecodeLargeComplete(ImageBundle bundle) {
        try {
            setScreenNail(bundle.backupImage, bundle.decoder.getWidth(), bundle.decoder.getHeight());
            setRegionDecoder(bundle.decoder);
            this.mPhotoView.notifyImageChange(0);
        } catch (Throwable t) {
            Log.w("SinglePhotoDataAdapter", "fail to decode large", t);
        }
    }

    private void onDecodeThumbComplete(Future<Bitmap> future) {
        try {
            Bitmap backup = future.get();
            if (backup == null) {
                this.mLoadingState = 2;
            } else {
                this.mLoadingState = 1;
                setScreenNail(backup, backup.getWidth(), backup.getHeight());
                this.mPhotoView.notifyImageChange(0);
            }
        } catch (Throwable t) {
            Log.w("SinglePhotoDataAdapter", "fail to decode thumb", t);
        }
    }

    @Override
    public void resume() {
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
        Future<?> task = this.mTask;
        task.cancel();
        task.waitDone();
        if (task.get() == null) {
            this.mTask = null;
        }
        if (this.mBitmapScreenNail != null) {
            this.mBitmapScreenNail.recycle();
            this.mBitmapScreenNail = null;
        }
    }

    @Override
    public void moveTo(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getImageSize(int offset, PhotoView.Size size) {
        if (offset == 0) {
            size.width = this.mItem.getWidth();
            size.height = this.mItem.getHeight();
        } else {
            size.width = 0;
            size.height = 0;
        }
    }

    @Override
    public int getImageRotation(int offset) {
        if (offset == 0) {
            return this.mItem.getFullImageRotation();
        }
        return 0;
    }

    @Override
    public ScreenNail getScreenNail(int offset) {
        if (offset == 0) {
            return getScreenNail();
        }
        return null;
    }

    @Override
    public void setNeedFullImage(boolean enabled) {
    }

    @Override
    public boolean isCamera(int offset) {
        return false;
    }

    @Override
    public boolean isPanorama(int offset) {
        return false;
    }

    @Override
    public boolean isStaticCamera(int offset) {
        return false;
    }

    @Override
    public boolean isVideo(int offset) {
        return this.mItem.getMediaType() == 4;
    }

    @Override
    public boolean isDeletable(int offset) {
        return (this.mItem.getSupportedOperations() & 1) != 0;
    }

    @Override
    public MediaItem getMediaItem(int offset) {
        if (offset == 0) {
            return this.mItem;
        }
        return null;
    }

    @Override
    public int getCurrentIndex() {
        return 0;
    }

    @Override
    public void setCurrentPhoto(Path path, int indexHint) {
    }

    @Override
    public void setFocusHintDirection(int direction) {
    }

    @Override
    public void setFocusHintPath(Path path) {
    }

    @Override
    public int getLoadingState(int offset) {
        return this.mLoadingState;
    }
}
