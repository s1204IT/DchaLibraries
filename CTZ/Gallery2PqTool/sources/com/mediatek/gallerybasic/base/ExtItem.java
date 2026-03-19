package com.mediatek.gallerybasic.base;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.ParcelFileDescriptor;
import com.mediatek.gallerybasic.util.DecodeSpecLimitor;
import com.mediatek.gallerybasic.util.DecodeUtils;
import com.mediatek.gallerybasic.util.Log;
import com.mediatek.gallerybasic.util.Utils;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class ExtItem {
    private static final String TAG = "MtkGallery2/ExtItem";
    protected Context mContext;
    protected int mHeight;
    protected MediaData mMediaData;
    protected int mWidth;

    public interface DataChangeListener {
        void onExtItemDataChange(ArrayList<String> arrayList);
    }

    public enum SupportOperation {
        DELETE,
        ROTATE,
        SHARE,
        CROP,
        SHOW_ON_MAP,
        SETAS,
        FULL_IMAGE,
        PLAY,
        CACHE,
        EDIT,
        INFO,
        TRIM,
        UNLOCK,
        BACK,
        ACTION,
        CAMERA_SHORTCUT,
        MUTE,
        PRINT
    }

    public class Thumbnail {
        public Bitmap mBitmap;
        public boolean mNeedClearCache;
        public boolean mStillNeedDecode;

        public Thumbnail(Bitmap bitmap, boolean z) {
            this.mBitmap = bitmap;
            this.mStillNeedDecode = z;
        }

        public Thumbnail(Bitmap bitmap, boolean z, boolean z2) {
            this.mBitmap = bitmap;
            this.mStillNeedDecode = z;
            this.mNeedClearCache = z2;
        }
    }

    public ExtItem(Context context, MediaData mediaData) {
        this.mContext = context;
        this.mMediaData = mediaData;
    }

    public ExtItem(MediaData mediaData) {
        this.mMediaData = mediaData;
    }

    public synchronized void updateMediaData(MediaData mediaData) {
        this.mMediaData = mediaData;
    }

    public Thumbnail getThumbnail(ThumbType thumbType) {
        return null;
    }

    public Bitmap decodeBitmap(BitmapFactory.Options options) {
        if (this.mMediaData.isVideo) {
            return DecodeUtils.decodeVideoThumbnail(this.mMediaData.filePath, options);
        }
        if (this.mMediaData.filePath != null) {
            return DecodeUtils.decodeBitmap(this.mMediaData.filePath, options);
        }
        if (this.mMediaData.uri != null) {
            return DecodeUtils.decodeBitmap(this.mContext, this.mMediaData.uri, options);
        }
        return null;
    }

    public ArrayList<SupportOperation> getSupportedOperations() {
        return null;
    }

    public ArrayList<SupportOperation> getNotSupportedOperations() {
        return null;
    }

    public boolean supportHighQuality() {
        return true;
    }

    public void delete() {
    }

    public boolean isNeedToCacheThumb(ThumbType thumbType) {
        return true;
    }

    public boolean isNeedToGetThumbFromCache(ThumbType thumbType) {
        return true;
    }

    public String[] getDetails() {
        return null;
    }

    public int getWidth() {
        return this.mWidth > 0 ? this.mWidth : this.mMediaData.width;
    }

    public int getHeight() {
        return this.mHeight > 0 ? this.mHeight : this.mMediaData.height;
    }

    public void decodeBounds() throws Throwable {
        ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor;
        if (this.mContext == null || this.mMediaData.isVideo || this.mMediaData.uri == null || DecodeSpecLimitor.isOutOfSpecLimit(this.mMediaData.fileSize, this.mMediaData.width, this.mMediaData.height, this.mMediaData.mimeType)) {
            return;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        ParcelFileDescriptor parcelFileDescriptor = 0;
        ParcelFileDescriptor parcelFileDescriptor2 = null;
        try {
            try {
                parcelFileDescriptorOpenFileDescriptor = this.mContext.getContentResolver().openFileDescriptor(this.mMediaData.uri, "r");
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
            e = e;
        }
        try {
            FileDescriptor fileDescriptor = parcelFileDescriptorOpenFileDescriptor.getFileDescriptor();
            if (fileDescriptor != null) {
                BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
            }
            Utils.closeSilently(parcelFileDescriptorOpenFileDescriptor);
        } catch (FileNotFoundException e2) {
            e = e2;
            parcelFileDescriptor2 = parcelFileDescriptorOpenFileDescriptor;
            e.printStackTrace();
            Utils.closeSilently(parcelFileDescriptor2);
        } catch (Throwable th2) {
            th = th2;
            parcelFileDescriptor = parcelFileDescriptorOpenFileDescriptor;
            Utils.closeSilently(parcelFileDescriptor);
            throw th;
        }
        this.mWidth = options.outWidth;
        this.mHeight = options.outHeight;
        parcelFileDescriptor = "<decodeBounds> mWidth = " + this.mWidth + " mHeight = " + this.mHeight;
        Log.d(TAG, parcelFileDescriptor);
    }

    public void registerListener(DataChangeListener dataChangeListener) {
    }

    public void unRegisterListener(DataChangeListener dataChangeListener) {
    }
}
