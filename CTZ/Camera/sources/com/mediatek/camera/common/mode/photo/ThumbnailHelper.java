package com.mediatek.camera.common.mode.photo;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.util.Size;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.mode.CameraApiHelper;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ThumbnailHelper {
    private static CameraCharacteristics mCameraCharacteristics;
    private static int mHeight;
    private static CaptureRequest.Key<int[]> mKeyRequestSize;
    private static int mOriginalWidth;
    private static ArrayList<Size> mThumbnailSizes;
    private static int mViewWidth;
    private static int mWidth;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ThumbnailHelper.class.getSimpleName());
    private static volatile boolean mIsSupport = false;
    private static volatile boolean mOverrideSupportValue = true;
    private static boolean mFindSize = false;
    private static boolean VERBOSE = false;

    public static void setApp(IApp iApp) {
        mWidth = iApp.getAppUi().getThumbnailViewWidth();
        mViewWidth = mWidth;
        mOriginalWidth = mWidth;
    }

    public static void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics, Context context, int i) {
        mCameraCharacteristics = cameraCharacteristics;
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(context).getDeviceDescriptionMap().get(String.valueOf(i));
        if (deviceDescription != null) {
            mIsSupport = deviceDescription.isThumbnailPostViewSupport().booleanValue();
        }
        LogHelper.d(TAG, "[setCameraCharacteristics], mIsSupport = " + mIsSupport);
        if (mIsSupport && deviceDescription != null) {
            mKeyRequestSize = deviceDescription.getKeyPostViewRequestSizeMode();
            LogHelper.d(TAG, "[setCameraCharacteristics], mKeyRequestSize = " + mKeyRequestSize);
            mThumbnailSizes = deviceDescription.getAvailableThumbnailSizes();
        }
    }

    public static int getThumbnailWidth() {
        return mWidth;
    }

    public static int getThumbnailHeight() {
        return mHeight;
    }

    public static byte[] getYUVBuffer(Image image) {
        int i;
        if (image.getFormat() != 35) {
            throw new IllegalArgumentException("Format not support!");
        }
        Rect cropRect = image.getCropRect();
        int format = image.getFormat();
        int iWidth = cropRect.width();
        int iHeight = cropRect.height();
        Image.Plane[] planes = image.getPlanes();
        int i2 = iWidth * iHeight;
        byte[] bArr = new byte[(ImageFormat.getBitsPerPixel(format) * i2) / 8];
        int i3 = 0;
        byte[] bArr2 = new byte[planes[0].getRowStride()];
        if (VERBOSE) {
            LogHelper.v(TAG, "[getYUVBuffer] get data from " + planes.length + " planes");
        }
        int i4 = 0;
        int i5 = 0;
        int i6 = 1;
        while (i4 < planes.length) {
            switch (i4) {
                case 0:
                    i5 = i3;
                    i6 = 1;
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    i5 = i2 + 1;
                    i6 = 2;
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    i5 = i2;
                    i6 = 2;
                    break;
            }
            ByteBuffer buffer = planes[i4].getBuffer();
            int rowStride = planes[i4].getRowStride();
            int pixelStride = planes[i4].getPixelStride();
            if (VERBOSE) {
                LogHelper.v(TAG, "[getYUVBuffer] pixelStride " + pixelStride);
                LogHelper.v(TAG, "[getYUVBuffer] rowStride " + rowStride);
                LogHelper.v(TAG, "[getYUVBuffer] width " + iWidth);
                LogHelper.v(TAG, "[getYUVBuffer] height " + iHeight);
                LogHelper.v(TAG, "[getYUVBuffer] buffer size " + buffer.remaining());
            }
            int i7 = i4 == 0 ? 0 : 1;
            int i8 = iWidth >> i7;
            int i9 = iHeight >> i7;
            int i10 = iWidth;
            int i11 = iHeight;
            buffer.position(((cropRect.top >> i7) * rowStride) + ((cropRect.left >> i7) * pixelStride));
            for (int i12 = 0; i12 < i9; i12++) {
                if (pixelStride == 1 && i6 == 1) {
                    buffer.get(bArr, i5, i8);
                    i5 += i8;
                    i = i8;
                } else {
                    i = ((i8 - 1) * pixelStride) + 1;
                    buffer.get(bArr2, 0, i);
                    int i13 = i5;
                    for (int i14 = 0; i14 < i8; i14++) {
                        bArr[i13] = bArr2[i14 * pixelStride];
                        i13 += i6;
                    }
                    i5 = i13;
                }
                if (i12 < i9 - 1) {
                    buffer.position((buffer.position() + rowStride) - i);
                }
            }
            if (VERBOSE) {
                LogHelper.v(TAG, "[getYUVBuffer] Finished reading data from plane " + i4);
            }
            i4++;
            iWidth = i10;
            iHeight = i11;
            i3 = 0;
        }
        return bArr;
    }

    public static boolean isPostViewSupported() {
        return mIsSupport;
    }

    public static boolean isPostViewOverrideSupported() {
        return mOverrideSupportValue && mIsSupport;
    }

    public static void overrideSupportedValue(String str, boolean z) {
        LogHelper.d(TAG, "[setPostViewSupportedValue] key: " + str + ", value: " + z);
        mOverrideSupportValue = z;
    }

    public static void setDefaultJpegThumbnailSize(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.JPEG_THUMBNAIL_SIZE, new Size(mWidth, mHeight));
    }

    public static void configPostViewRequest(CaptureRequest.Builder builder) {
        if (!mIsSupport || builder == null) {
            return;
        }
        builder.set(mKeyRequestSize, new int[]{mWidth, mHeight});
    }

    public static void updateThumbnailSize(double d) {
        int i = mOriginalWidth * mOriginalWidth;
        int i2 = mOriginalWidth;
        mHeight = i2;
        mWidth = i2;
        LogHelper.d(TAG, "[updateThumbnailSize], original, mWidth = " + mOriginalWidth);
        Size[] outputSizes = ((StreamConfigurationMap) mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(34);
        if (outputSizes == null) {
            return;
        }
        int i3 = Integer.MAX_VALUE;
        for (Size size : outputSizes) {
            int width = (size.getWidth() * size.getHeight()) - i;
            if (width > 0 && width <= i3) {
                mWidth = size.getWidth();
                mHeight = size.getHeight();
                i3 = width;
            }
        }
        LogHelper.d(TAG, "[updateThumbnailSize], mWidth = " + mWidth + ", mHeight = " + mHeight);
    }
}
