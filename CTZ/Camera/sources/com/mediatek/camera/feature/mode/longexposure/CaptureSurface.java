package com.mediatek.camera.feature.mode.longexposure;

import android.annotation.TargetApi;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.nio.ByteBuffer;

@TargetApi(19)
class CaptureSurface {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(CaptureSurface.class.getSimpleName());
    private final Handler mCaptureHandler;
    private ImageReader mCaptureImageReader;
    private ImageCallback mImageCallback;
    private int mPictureHeight;
    private int mPictureWidth;
    private int mFormat = 256;
    private int mMaxImages = 2;
    private final Object mImageReaderSync = new Object();
    private final ImageReader.OnImageAvailableListener mCaptureImageListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            LogHelper.i(CaptureSurface.TAG, "[onImageAvailable]");
            if (CaptureSurface.this.mImageCallback != null) {
                CaptureSurface.this.mImageCallback.onPictureCallback(CaptureSurface.this.getJpeg(imageReader.acquireLatestImage()));
            }
        }
    };

    public interface ImageCallback {
        void onPictureCallback(byte[] bArr);
    }

    public void setCaptureCallback(ImageCallback imageCallback) {
        this.mImageCallback = imageCallback;
    }

    public CaptureSurface() {
        LogHelper.d(TAG, "[CaptureSurface] Construct");
        HandlerThread handlerThread = new HandlerThread("cap_surface");
        handlerThread.start();
        this.mCaptureHandler = new Handler(handlerThread.getLooper());
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

    public void release() {
        if (this.mCaptureHandler != null) {
            if (Build.VERSION.SDK_INT >= 18) {
                this.mCaptureHandler.getLooper().quitSafely();
            } else {
                this.mCaptureHandler.getLooper().quit();
            }
        }
    }

    private byte[] getJpeg(Image image) {
        byte[] bArr;
        synchronized (this.mImageReaderSync) {
            if (256 == image.getFormat()) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                bArr = new byte[buffer.remaining()];
                buffer.get(bArr);
                buffer.rewind();
                image.close();
            } else {
                image.close();
                throw new RuntimeException("[getJpeg] image format not supported.");
            }
        }
        return bArr;
    }
}
