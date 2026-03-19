package com.mediatek.camera.feature.mode.vsdof.photo.device;

import android.annotation.TargetApi;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.photo.ThumbnailHelper;
import java.nio.ByteBuffer;

@TargetApi(19)
public class CaptureSurface {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(CaptureSurface.class.getSimpleName());
    private final Handler mCaptureHandler;
    private ImageReader mCaptureImageReader;
    private ImageCallback mImageCallback;
    private int mPictureHeight;
    private int mPictureWidth;
    private int mFormat = 256;
    private int mMaxImages = 12;
    private final Object mImageReaderSync = new Object();
    private String mFormatTag = new String("jpeg");
    private final ImageReader.OnImageAvailableListener mCaptureImageListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            LogHelper.i(CaptureSurface.TAG, "[onImageAvailable]");
            if (CaptureSurface.this.mImageCallback != null) {
                Image imageAcquireNextImage = imageReader.acquireNextImage();
                if (imageAcquireNextImage == null) {
                    LogHelper.w(CaptureSurface.TAG, "[onImageAvailable] image buffer is null");
                    return;
                }
                CaptureSurface.this.mImageCallback.onPictureCallback(CaptureSurface.this.getBuffer(imageAcquireNextImage), imageAcquireNextImage.getFormat(), CaptureSurface.this.mFormatTag, imageAcquireNextImage.getWidth(), imageAcquireNextImage.getHeight());
            }
        }
    };

    public interface ImageCallback {
        void onPictureCallback(byte[] bArr, int i, String str, int i2, int i3);
    }

    public CaptureSurface() {
        LogHelper.d(TAG, "[CaptureSurface]Construct");
        this.mCaptureHandler = new Handler(Looper.myLooper());
    }

    public boolean updatePictureInfo(int i, int i2, int i3, int i4) {
        LogHelper.i(TAG, "[updatePictureInfo] width = " + i + ",height = " + i2 + ",format = " + i3 + ",maxImage = " + i4 + ",mCaptureImageReader = " + this.mCaptureImageReader);
        if (this.mCaptureImageReader != null && this.mPictureWidth == i && this.mPictureHeight == i2 && i3 == this.mFormat && i4 == this.mMaxImages) {
            LogHelper.d(TAG, "[updatePictureInfo],the info : " + this.mPictureWidth + " x " + this.mPictureHeight + ",format = " + i3 + ",maxImage = " + i4 + " is same as before");
            return false;
        }
        this.mPictureWidth = i;
        this.mPictureHeight = i2;
        this.mFormat = i3;
        this.mMaxImages = i4;
        synchronized (this.mImageReaderSync) {
            this.mCaptureImageReader = ImageReader.newInstance(this.mPictureWidth, this.mPictureHeight, this.mFormat, this.mMaxImages);
            this.mCaptureImageReader.setOnImageAvailableListener(this.mCaptureImageListener, this.mCaptureHandler);
        }
        return true;
    }

    public Surface getSurface() {
        synchronized (this.mImageReaderSync) {
            if (this.mCaptureImageReader != null) {
                return this.mCaptureImageReader.getSurface();
            }
            return null;
        }
    }

    public void releaseCaptureSurface() {
        LogHelper.d(TAG, "[releaseCaptureSurface], mCaptureImageReader = " + this.mCaptureImageReader);
        synchronized (this.mImageReaderSync) {
            if (this.mCaptureImageReader != null) {
                this.mCaptureImageReader.close();
                this.mCaptureImageReader = null;
            }
        }
    }

    public void setCaptureCallback(ImageCallback imageCallback) {
        this.mImageCallback = imageCallback;
    }

    public void setFormat(String str) {
        if (str == null) {
            LogHelper.d(TAG, "[setFormat], null pointer! mFormatTag: " + str);
            return;
        }
        this.mFormatTag = str;
        LogHelper.d(TAG, "[setFormat], mFormatTag: " + this.mFormatTag);
    }

    private byte[] getBuffer(Image image) {
        byte[] yUVBuffer;
        synchronized (this.mImageReaderSync) {
            LogHelper.i(TAG, "[getBuffer] image.getFormat = " + image.getFormat() + " image.getWidth = " + image.getWidth() + " image.getHeight = " + image.getHeight() + " format = " + image.getFormat() + " formatTag = " + this.mFormatTag);
            if ("jpeg".equalsIgnoreCase(this.mFormatTag)) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                yUVBuffer = new byte[buffer.remaining()];
                buffer.get(yUVBuffer);
                buffer.rewind();
            } else if ("thumbnail".equalsIgnoreCase(this.mFormatTag)) {
                yUVBuffer = ThumbnailHelper.getYUVBuffer(image);
            } else {
                throw new RuntimeException("Unsupported image format.");
            }
            image.close();
        }
        return yUVBuffer;
    }
}
