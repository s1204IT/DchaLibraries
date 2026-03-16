package com.android.gallery3d.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.PanoramaMetadataSupport;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DownloadCache;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.util.ThreadPool;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

public class UriImage extends MediaItem {
    private GalleryApp mApplication;
    private DownloadCache.Entry mCacheEntry;
    private final String mContentType;
    private ParcelFileDescriptor mFileDescriptor;
    private int mHeight;
    private PanoramaMetadataSupport mPanoramaMetadata;
    private int mRotation;
    private int mState;
    private final Uri mUri;
    private int mWidth;

    public UriImage(GalleryApp application, Path path, Uri uri, String contentType) {
        super(path, nextVersionNumber());
        this.mState = 0;
        this.mPanoramaMetadata = new PanoramaMetadataSupport(this);
        this.mUri = uri;
        this.mApplication = (GalleryApp) Utils.checkNotNull(application);
        this.mContentType = contentType;
        Log.i("UriImage", "!!! request image: UriImage: " + this.mUri + " Type: " + this.mContentType);
    }

    @Override
    public ThreadPool.Job<Bitmap> requestImage(int type) {
        return new BitmapJob(type);
    }

    @Override
    public ThreadPool.Job<BitmapRegionDecoder> requestLargeImage() {
        return new RegionDecoderJob();
    }

    private void openFileOrDownloadTempFile(ThreadPool.JobContext jc) {
        int state = openOrDownloadInner(jc);
        synchronized (this) {
            this.mState = state;
            if (this.mState != 2 && this.mFileDescriptor != null) {
                Utils.closeSilently(this.mFileDescriptor);
                this.mFileDescriptor = null;
            }
            notifyAll();
        }
    }

    private int openOrDownloadInner(ThreadPool.JobContext jc) {
        String scheme = this.mUri.getScheme();
        if ("content".equals(scheme) || "android.resource".equals(scheme) || "file".equals(scheme)) {
            try {
                if ("image/jpeg".equalsIgnoreCase(this.mContentType)) {
                    InputStream is = this.mApplication.getContentResolver().openInputStream(this.mUri);
                    this.mRotation = Exif.getOrientation(is);
                    Utils.closeSilently(is);
                }
                this.mFileDescriptor = this.mApplication.getContentResolver().openFileDescriptor(this.mUri, "r");
                return jc.isCancelled() ? 0 : 2;
            } catch (FileNotFoundException e) {
                Log.w("UriImage", "fail to open: " + this.mUri, e);
                return -1;
            }
        }
        try {
            URL url = new URI(this.mUri.toString()).toURL();
            this.mCacheEntry = this.mApplication.getDownloadCache().download(jc, url);
            if (jc.isCancelled()) {
                return 0;
            }
            if (this.mCacheEntry == null) {
                Log.w("UriImage", "download failed " + url);
                return -1;
            }
            if ("image/jpeg".equalsIgnoreCase(this.mContentType)) {
                InputStream is2 = new FileInputStream(this.mCacheEntry.cacheFile);
                this.mRotation = Exif.getOrientation(is2);
                Utils.closeSilently(is2);
            }
            this.mFileDescriptor = ParcelFileDescriptor.open(this.mCacheEntry.cacheFile, 268435456);
            return 2;
        } catch (Throwable t) {
            Log.w("UriImage", "download error", t);
            return -1;
        }
    }

    private boolean prepareInputFile(ThreadPool.JobContext jc) {
        jc.setCancelListener(new ThreadPool.CancelListener() {
            @Override
            public void onCancel() {
                synchronized (this) {
                    notifyAll();
                }
            }
        });
        while (true) {
            synchronized (this) {
                if (jc.isCancelled()) {
                    return false;
                }
                if (this.mState == 0) {
                    this.mState = 1;
                } else {
                    if (this.mState == -1) {
                        return false;
                    }
                    if (this.mState == 2) {
                        return true;
                    }
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            openFileOrDownloadTempFile(jc);
        }
    }

    private class RegionDecoderJob implements ThreadPool.Job<BitmapRegionDecoder> {
        private RegionDecoderJob() {
        }

        @Override
        public BitmapRegionDecoder run(ThreadPool.JobContext jc) {
            if (!UriImage.this.prepareInputFile(jc)) {
                return null;
            }
            BitmapRegionDecoder decoder = DecodeUtils.createBitmapRegionDecoder(jc, UriImage.this.mFileDescriptor.getFileDescriptor(), false);
            UriImage.this.mWidth = decoder.getWidth();
            UriImage.this.mHeight = decoder.getHeight();
            return decoder;
        }
    }

    private class BitmapJob implements ThreadPool.Job<Bitmap> {
        private int mType;

        protected BitmapJob(int type) {
            this.mType = type;
        }

        @Override
        public Bitmap run(ThreadPool.JobContext jc) {
            if (!UriImage.this.prepareInputFile(jc)) {
                return null;
            }
            int targetSize = MediaItem.getTargetSize(this.mType);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = DecodeUtils.decodeThumbnail(jc, UriImage.this.mFileDescriptor.getFileDescriptor(), options, targetSize, this.mType);
            if (jc.isCancelled() || bitmap == null) {
                return null;
            }
            if (this.mType == 2) {
                return BitmapUtils.resizeAndCropCenter(bitmap, targetSize, true);
            }
            return BitmapUtils.resizeDownBySideLength(bitmap, targetSize, true);
        }
    }

    @Override
    public int getSupportedOperations() {
        int supported = isSharable() ? 131104 | 4 : 131104;
        if (BitmapUtils.isSupportedByRegionDecoder(this.mContentType)) {
            return supported | 576;
        }
        return supported;
    }

    @Override
    public void getPanoramaSupport(MediaObject.PanoramaSupportCallback callback) {
        this.mPanoramaMetadata.getPanoramaSupport(this.mApplication, callback);
    }

    private boolean isSharable() {
        return "file".equals(this.mUri.getScheme());
    }

    @Override
    public int getMediaType() {
        return 2;
    }

    @Override
    public Uri getContentUri() {
        return this.mUri;
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        if (this.mWidth != 0 && this.mHeight != 0) {
            details.addDetail(5, Integer.valueOf(this.mWidth));
            details.addDetail(6, Integer.valueOf(this.mHeight));
        }
        if (this.mContentType != null) {
            details.addDetail(9, this.mContentType);
        }
        if ("file".equals(this.mUri.getScheme())) {
            String filePath = this.mUri.getPath();
            details.addDetail(200, filePath);
            MediaDetails.extractExifInfo(details, filePath);
        }
        return details;
    }

    @Override
    public String getMimeType() {
        return this.mContentType;
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mFileDescriptor != null) {
                Utils.closeSilently(this.mFileDescriptor);
            }
        } finally {
            super.finalize();
        }
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getRotation() {
        return this.mRotation;
    }
}
