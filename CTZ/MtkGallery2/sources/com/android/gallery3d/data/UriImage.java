package com.android.gallery3d.data;

import android.database.Cursor;
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
import com.mediatek.gallery3d.adapter.FeatureHelper;
import com.mediatek.gallery3d.adapter.MediaDataParser;
import com.mediatek.gallery3d.adapter.PhotoPlayFacade;
import com.mediatek.gallery3d.util.FeatureConfig;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import com.mediatek.gallerybasic.base.ExtItem;
import com.mediatek.gallerybasic.util.DecodeSpecLimitor;
import java.io.File;
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
    private boolean mIsUrlImage;
    private PanoramaMetadataSupport mPanoramaMetadata;
    private int mRotation;
    private int mState;
    private final Uri mUri;
    private int mWidth;

    public UriImage(GalleryApp galleryApp, Path path, Uri uri, String str) {
        super(path, nextVersionNumber());
        this.mState = 0;
        this.mPanoramaMetadata = new PanoramaMetadataSupport(this);
        this.mIsUrlImage = false;
        this.mUri = uri;
        this.mApplication = (GalleryApp) Utils.checkNotNull(galleryApp);
        this.mContentType = str;
        updateMediaData();
        if (this.mWidth <= 0 || this.mHeight <= 0) {
            int width = this.mExtItem.getWidth();
            int height = this.mExtItem.getHeight();
            if (width > 0 && height > 0) {
                this.mWidth = width;
                this.mHeight = height;
                updateMediaData();
            }
        }
        com.mediatek.gallery3d.util.Log.d("Gallery2/UriImage", "<UriImage> mUri " + this.mUri + " mContentType " + this.mContentType + " mWidth = " + this.mWidth + " mHeight = " + this.mHeight);
    }

    @Override
    public ThreadPool.Job<Bitmap> requestImage(int i) {
        return new BitmapJob(i);
    }

    @Override
    public ThreadPool.Job<BitmapRegionDecoder> requestLargeImage() {
        return new RegionDecoderJob();
    }

    private void openFileOrDownloadTempFile(ThreadPool.JobContext jobContext) {
        int iOpenOrDownloadInner = openOrDownloadInner(jobContext);
        synchronized (this) {
            this.mState = iOpenOrDownloadInner;
            if (this.mState != 2 && this.mFileDescriptor != null) {
                Utils.closeSilently(this.mFileDescriptor);
                this.mFileDescriptor = null;
            }
            notifyAll();
        }
    }

    private int openOrDownloadInner(ThreadPool.JobContext jobContext) {
        String scheme = this.mUri.getScheme();
        if ("content".equals(scheme) || "android.resource".equals(scheme) || "file".equals(scheme)) {
            if (this.mMediaData.filePath != null && this.mMediaData.filePath.toLowerCase().endsWith(".mudp")) {
                com.mediatek.gallery3d.util.Log.d("Gallery2/UriImage", "<openOrDownloadInner> return STATE_DOWNLOADED");
                return 2;
            }
            try {
                if ("image/jpeg".equalsIgnoreCase(this.mContentType)) {
                    InputStream inputStreamOpenInputStream = this.mApplication.getContentResolver().openInputStream(this.mUri);
                    this.mRotation = Exif.getOrientation(inputStreamOpenInputStream);
                    Utils.closeSilently(inputStreamOpenInputStream);
                } else if ("image/x-adobe-dng".equalsIgnoreCase(this.mContentType)) {
                    InputStream inputStreamOpenInputStream2 = this.mApplication.getContentResolver().openInputStream(this.mUri);
                    this.mRotation = FeatureHelper.getOrientationFromExif(this.mMediaData.filePath, inputStreamOpenInputStream2);
                    Utils.closeSilently(inputStreamOpenInputStream2);
                }
                this.mFileDescriptor = this.mApplication.getContentResolver().openFileDescriptor(this.mUri, "r");
                return jobContext.isCancelled() ? 0 : 2;
            } catch (FileNotFoundException e) {
                com.mediatek.gallery3d.util.Log.w("Gallery2/UriImage", "fail to open: " + this.mUri, e);
                return -1;
            }
        }
        try {
            this.mIsUrlImage = true;
            URL url = new URI(this.mUri.toString()).toURL();
            DownloadCache downloadCache = this.mApplication.getDownloadCache();
            if (downloadCache == null) {
                com.mediatek.gallery3d.util.Log.w("Gallery2/UriImage", "<openOrDownloadInner> failed to get DownloadCache");
                return -1;
            }
            this.mCacheEntry = downloadCache.download(jobContext, url);
            if (jobContext.isCancelled()) {
                return 0;
            }
            if (this.mCacheEntry == null) {
                com.mediatek.gallery3d.util.Log.w("Gallery2/UriImage", "download failed " + url);
                return -1;
            }
            if ("image/jpeg".equalsIgnoreCase(this.mContentType)) {
                FileInputStream fileInputStream = new FileInputStream(this.mCacheEntry.cacheFile);
                this.mRotation = Exif.getOrientation(fileInputStream);
                Utils.closeSilently(fileInputStream);
            }
            this.mFileDescriptor = ParcelFileDescriptor.open(this.mCacheEntry.cacheFile, 268435456);
            return 2;
        } catch (Throwable th) {
            com.mediatek.gallery3d.util.Log.w("Gallery2/UriImage", "download error", th);
            return -1;
        }
    }

    private boolean prepareInputFile(ThreadPool.JobContext jobContext) {
        jobContext.setCancelListener(new ThreadPool.CancelListener() {
            @Override
            public void onCancel() {
                synchronized (this) {
                    notifyAll();
                }
            }
        });
        while (true) {
            synchronized (this) {
                if (jobContext.isCancelled()) {
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
            openFileOrDownloadTempFile(jobContext);
        }
    }

    private class RegionDecoderJob implements ThreadPool.Job<BitmapRegionDecoder> {
        private RegionDecoderJob() {
        }

        @Override
        public BitmapRegionDecoder run(ThreadPool.JobContext jobContext) {
            if (!UriImage.this.prepareInputFile(jobContext)) {
                return null;
            }
            if (UriImage.this.mMediaData != null && DecodeSpecLimitor.isOutOfSpecLimit(UriImage.this.getUriImageFileSize(UriImage.this.mUri), UriImage.this.mMediaData.width, UriImage.this.mMediaData.height, UriImage.this.mContentType)) {
                UriImage.this.releaseInputFile();
                com.mediatek.gallery3d.util.Log.d("Gallery2/UriImage", "<RegionDecoderJob.run> out of spec limit, abort decoding!");
                return null;
            }
            BitmapRegionDecoder bitmapRegionDecoderCreateBitmapRegionDecoder = DecodeUtils.createBitmapRegionDecoder(jobContext, UriImage.this.mFileDescriptor.getFileDescriptor(), false);
            if (bitmapRegionDecoderCreateBitmapRegionDecoder == null) {
                UriImage.this.releaseInputFile();
                return null;
            }
            int width = bitmapRegionDecoderCreateBitmapRegionDecoder.getWidth();
            int height = bitmapRegionDecoderCreateBitmapRegionDecoder.getHeight();
            if (width > 0 && height > 0 && width != UriImage.this.mWidth && height != UriImage.this.mHeight) {
                UriImage.this.mWidth = width;
                UriImage.this.mHeight = height;
                UriImage.this.updateMediaData();
            }
            UriImage.this.releaseInputFile();
            return bitmapRegionDecoderCreateBitmapRegionDecoder;
        }
    }

    private class BitmapJob implements ThreadPool.Job<Bitmap> {
        private int mType;

        protected BitmapJob(int i) {
            this.mType = i;
        }

        @Override
        public Bitmap run(ThreadPool.JobContext jobContext) {
            int width;
            int height;
            Bitmap bitmapResizeDownBySideLength;
            if (!UriImage.this.prepareInputFile(jobContext)) {
                return null;
            }
            if (UriImage.this.mMediaData != null && DecodeSpecLimitor.isOutOfSpecLimit(UriImage.this.getUriImageFileSize(UriImage.this.mUri), UriImage.this.mMediaData.width, UriImage.this.mMediaData.height, UriImage.this.mContentType)) {
                com.mediatek.gallery3d.util.Log.d("Gallery2/UriImage", "<BitmapJob.run> out of spec limit, abort decoding!");
                UriImage.this.releaseInputFile();
                return null;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            if (UriImage.this.mFileDescriptor != null) {
                DecodeUtils.decodeBounds(jobContext, UriImage.this.mFileDescriptor.getFileDescriptor(), options);
                width = options.outWidth;
                height = options.outHeight;
            } else {
                width = UriImage.this.mExtItem.getWidth();
                height = UriImage.this.mExtItem.getHeight();
            }
            if (width > 0 && height > 0 && width != UriImage.this.mWidth && height != UriImage.this.mHeight) {
                UriImage.this.mWidth = width;
                UriImage.this.mHeight = height;
                UriImage.this.updateMediaData();
            }
            ExtItem.Thumbnail thumbnail = UriImage.this.mExtItem.getThumbnail(FeatureHelper.convertToThumbType(this.mType));
            if (thumbnail != null && thumbnail.mBitmap != null) {
                UriImage.this.releaseInputFile();
                return thumbnail.mBitmap;
            }
            if (thumbnail != null && thumbnail.mBitmap == null && !thumbnail.mStillNeedDecode) {
                UriImage.this.releaseInputFile();
                return null;
            }
            int targetSize = MediaItem.getTargetSize(this.mType);
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmapDecodeThumbnail = DecodeUtils.decodeThumbnail(jobContext, UriImage.this.mFileDescriptor.getFileDescriptor(), options2, targetSize, this.mType);
            if (jobContext.isCancelled() || bitmapDecodeThumbnail == null) {
                UriImage.this.releaseInputFile();
                return null;
            }
            if (this.mType == 2) {
                bitmapResizeDownBySideLength = BitmapUtils.resizeAndCropCenter(bitmapDecodeThumbnail, targetSize, true);
            } else {
                bitmapResizeDownBySideLength = BitmapUtils.resizeDownBySideLength(bitmapDecodeThumbnail, targetSize, true);
            }
            Bitmap bitmapClearAlphaValueIfPng = com.mediatek.gallerybasic.util.BitmapUtils.clearAlphaValueIfPng(bitmapResizeDownBySideLength, UriImage.this.mMediaData.mimeType, true);
            UriImage.this.releaseInputFile();
            return bitmapClearAlphaValueIfPng;
        }
    }

    @Override
    public int getSupportedOperations() {
        int i;
        if (!this.mIsUrlImage) {
            i = 131104;
        } else {
            i = 131072;
        }
        if (isSharable()) {
            i |= 4;
        }
        if (BitmapUtils.isSupportedByRegionDecoder(this.mContentType)) {
            i |= 576;
            float fMax = sThumbnailTargetSize / Math.max(this.mWidth, this.mHeight);
            int i2 = (int) (this.mWidth * fMax);
            if (Math.max(0, Utils.ceilLog2(this.mWidth / i2)) == 0 || (FeatureConfig.sIsLowRamDevice && this.mWidth * this.mHeight > 12582912)) {
                com.mediatek.gallery3d.util.Log.d("Gallery2/UriImage", "<getSupportedOperations> item thumbWidth " + i2 + " scale " + fMax);
                com.mediatek.gallery3d.util.Log.d("Gallery2/UriImage", "<getSupportedOperations> item not support full image, mWidth " + this.mWidth + " sthumbnailsize " + sThumbnailTargetSize);
                com.mediatek.gallery3d.util.Log.d("Gallery2/UriImage", "<getSupportedOperations> sIsLowRamDevice " + FeatureConfig.sIsLowRamDevice + ", mWidth * mHeight is " + (this.mWidth * this.mHeight));
                i &= -65;
            }
        }
        int iMergeSupportOperations = FeatureHelper.mergeSupportOperations(i, this.mExtItem.getSupportedOperations(), this.mExtItem.getNotSupportedOperations());
        if ("file".equals(this.mUri.getScheme())) {
            return iMergeSupportOperations & (-131073) & (-33) & (-5);
        }
        return iMergeSupportOperations;
    }

    @Override
    public void getPanoramaSupport(MediaObject.PanoramaSupportCallback panoramaSupportCallback) {
        this.mPanoramaMetadata.getPanoramaSupport(this.mApplication, panoramaSupportCallback);
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
            String path = this.mUri.getPath();
            details.addDetail(200, path);
            MediaDetails.extractExifInfo(details, path);
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
        return this.mWidth;
    }

    @Override
    public int getHeight() {
        return this.mHeight;
    }

    @Override
    public int getRotation() {
        return this.mRotation;
    }

    public void updateMediaData() {
        synchronized (this.mMediaDataLock) {
            this.mMediaData = MediaDataParser.parseUriImageMediaData(this);
            this.mExtItem = PhotoPlayFacade.getMediaCenter().getItem(this.mMediaData);
        }
    }

    private long getUriImageFileSize(Uri uri) throws Throwable {
        Throwable th;
        Cursor cursorQuery;
        if (!FeatureHelper.isLocalUri(uri)) {
            return 0L;
        }
        String str = null;
        try {
            cursorQuery = this.mApplication.getContentResolver().query(uri, new String[]{BookmarkEnhance.COLUMN_DATA}, null, null, null);
        } catch (IllegalArgumentException e) {
            e = e;
            cursorQuery = null;
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
            if (cursorQuery != null) {
            }
            throw th;
        }
        if (cursorQuery == null) {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return 0L;
        }
        try {
            try {
                int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow(BookmarkEnhance.COLUMN_DATA);
                cursorQuery.moveToFirst();
                String string = cursorQuery.getString(columnIndexOrThrow);
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                str = string;
            } catch (IllegalArgumentException e2) {
                e = e2;
                com.mediatek.gallery3d.util.Log.e("Gallery2/UriImage", "<getUriImageFileSize> Exception", e);
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            }
            if (str == null) {
                return 0L;
            }
            File file = new File(str);
            if (file.exists()) {
                return file.length();
            }
            return 0L;
        } catch (Throwable th3) {
            th = th3;
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            throw th;
        }
    }

    private synchronized void releaseInputFile() {
        if (this.mFileDescriptor != null) {
            Utils.closeSilently(this.mFileDescriptor);
            this.mFileDescriptor = null;
        }
        this.mState = 0;
    }
}
