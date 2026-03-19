package com.mediatek.camera.feature.setting.matrixdisplay;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MatrixDisplayRequestConfig implements ICameraSetting.ICaptureRequestConfigure, IMatrixDisplayConfig {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MatrixDisplayRequestConfig.class.getSimpleName());
    private IPreviewFrameCallback mCallback;
    private String mCurrentEffect;
    private ISettingManager.SettingDevice2Requester mDevice2Requester;
    private Handler mHandler;
    private ImageReader mImageReader;
    private int mPreviewHeight;
    private int mPreviewWidth;
    private ValueInitializedListener mValueInitializedListener;
    private boolean mDisplayOpened = false;
    private byte[][] mPreviewBuffers = new byte[3][];
    private int mCursor = 0;
    private ImageReader.OnImageAvailableListener mImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            int i;
            int i2;
            int i3;
            int i4;
            int i5;
            synchronized (MatrixDisplayRequestConfig.this) {
                Image imageAcquireNextImage = imageReader.acquireNextImage();
                if (imageAcquireNextImage == null) {
                    LogHelper.d(MatrixDisplayRequestConfig.TAG, "[onImageAvailable] acquireNextImage return false, return");
                    return;
                }
                Image.Plane[] planes = imageAcquireNextImage.getPlanes();
                int width = imageAcquireNextImage.getWidth();
                int height = imageAcquireNextImage.getHeight();
                int format = imageAcquireNextImage.getFormat();
                if (MatrixDisplayRequestConfig.this.mPreviewBuffers[MatrixDisplayRequestConfig.this.mCursor] == null) {
                    LogHelper.d(MatrixDisplayRequestConfig.TAG, "[onImageAvailable] mPreviewBuffers[mCursor] is null, return");
                    imageAcquireNextImage.close();
                    return;
                }
                byte[] bArr = MatrixDisplayRequestConfig.this.mPreviewBuffers[MatrixDisplayRequestConfig.this.mCursor];
                int rowStride = planes[0].getRowStride();
                for (int i6 = 0; i6 < planes.length; i6++) {
                    if (rowStride < planes[i6].getRowStride()) {
                        rowStride = planes[i6].getRowStride();
                    }
                }
                byte[] bArr2 = new byte[rowStride];
                int i7 = 0;
                int i8 = 0;
                while (i7 < planes.length) {
                    ByteBuffer buffer = planes[i7].getBuffer();
                    int rowStride2 = planes[i7].getRowStride();
                    int pixelStride = planes[i7].getPixelStride();
                    if (i7 != 0) {
                        i = width / 2;
                    } else {
                        i = width;
                    }
                    if (i7 != 0) {
                        i2 = height / 2;
                    } else {
                        i2 = height;
                    }
                    Image.Plane[] planeArr = planes;
                    int bitsPerPixel = ImageFormat.getBitsPerPixel(format) / 8;
                    int i9 = width;
                    if (format == 35) {
                        int i10 = i * i2;
                        if (i7 == 0) {
                            buffer.get(bArr, i8, i10);
                        } else if (i7 == 1) {
                            buffer.get(bArr, i8 + i10, i10);
                        } else if (i7 == 2) {
                            buffer.get(bArr, i8 - i10, i10);
                        }
                        i8 += i10;
                    } else if (format == 842094169) {
                        int i11 = 0;
                        while (i11 < i2) {
                            if (pixelStride == bitsPerPixel) {
                                i3 = height;
                                i4 = i * bitsPerPixel;
                                buffer.get(bArr, i8, i4);
                                i8 += i4;
                                i5 = bitsPerPixel;
                            } else {
                                i3 = height;
                                i4 = ((i - 1) * pixelStride) + bitsPerPixel;
                                i5 = bitsPerPixel;
                                buffer.get(bArr2, 0, i4);
                                int i12 = i8;
                                int i13 = 0;
                                while (i13 < i) {
                                    bArr[i12] = bArr2[i13 * pixelStride];
                                    i13++;
                                    i12++;
                                }
                                i8 = i12;
                            }
                            if (i11 < i2 - 1) {
                                buffer.position((buffer.position() + rowStride2) - i4);
                            }
                            i11++;
                            height = i3;
                            bitsPerPixel = i5;
                        }
                    }
                    buffer.rewind();
                    i7++;
                    planes = planeArr;
                    width = i9;
                    height = height;
                }
                imageAcquireNextImage.close();
                if (MatrixDisplayRequestConfig.this.mCallback != null) {
                    MatrixDisplayRequestConfig.this.mCallback.onPreviewFrameAvailable(bArr);
                }
                MatrixDisplayRequestConfig.this.mCursor = MatrixDisplayRequestConfig.access$208(MatrixDisplayRequestConfig.this) % 3;
            }
        }
    };

    interface ValueInitializedListener {
        void onValueInitialized(List<String> list, String str, List<String> list2);
    }

    static int access$208(MatrixDisplayRequestConfig matrixDisplayRequestConfig) {
        int i = matrixDisplayRequestConfig.mCursor;
        matrixDisplayRequestConfig.mCursor = i + 1;
        return i;
    }

    enum ModeEnum {
        NONE(0),
        MONO(1),
        NEGATIVE(2),
        SOLARIZE(3),
        SEPIA(4),
        POSTERIZE(5),
        WHITEBOARD(6),
        BLACKBOARD(7),
        AQUA(8),
        NASHVILLE(11),
        HEFE(12),
        VALENCIA(13),
        XPROLL(14),
        LOFI(15),
        SIERRA(16),
        WALDEN(18);

        private int mValue;

        ModeEnum(int i) {
            this.mValue = 0;
            this.mValue = i;
        }

        public int getValue() {
            return this.mValue;
        }

        public String getName() {
            return toString();
        }
    }

    public MatrixDisplayRequestConfig(String str, ISettingManager.SettingDevice2Requester settingDevice2Requester, ValueInitializedListener valueInitializedListener) {
        this.mDevice2Requester = settingDevice2Requester;
        this.mValueInitializedListener = valueInitializedListener;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        List<String> listConvertEnumToString = convertEnumToString((int[]) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS));
        String lowerCase = ModeEnum.NONE.getName().toLowerCase(Locale.ENGLISH);
        Size[] outputSizes = ((StreamConfigurationMap) cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(SurfaceHolder.class);
        ArrayList arrayList = new ArrayList(outputSizes.length);
        for (Size size : outputSizes) {
            arrayList.add(size);
        }
        sortSizeInDescending(arrayList);
        this.mValueInitializedListener.onValueInitialized(listConvertEnumToString, lowerCase, sizeToStr(arrayList));
    }

    @Override
    public synchronized void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        LogHelper.d(TAG, "[configCaptureRequest], mCurrentEffect:" + this.mCurrentEffect + ", mDisplayOpened:" + this.mDisplayOpened);
        builder.set(CaptureRequest.CONTROL_EFFECT_MODE, Integer.valueOf(convertStringToEnum(this.mCurrentEffect)));
        if (this.mDisplayOpened) {
            builder.addTarget(this.mImageReader.getSurface());
        }
    }

    @Override
    public synchronized void configSessionSurface(List<Surface> list) {
        LogHelper.d(TAG, "[configSessionSurface], mDisplayOpened:" + this.mDisplayOpened);
        if (this.mDisplayOpened) {
            list.add(this.mImageReader.getSurface());
        }
    }

    @Override
    public CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback() {
        return null;
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDevice2Requester.requestRestartSession();
    }

    @Override
    public Surface configRawSurface() {
        return null;
    }

    public void setPreviewFrameCallback(IPreviewFrameCallback iPreviewFrameCallback) {
        this.mCallback = iPreviewFrameCallback;
    }

    @Override
    public void setPreviewSize(int i, int i2) {
        this.mPreviewWidth = i;
        this.mPreviewHeight = i2;
    }

    @Override
    public synchronized void setDisplayStatus(boolean z) {
        this.mDisplayOpened = z;
        int i = 0;
        if (!z) {
            while (i < 3) {
                this.mPreviewBuffers[i] = null;
                i++;
            }
            this.mImageReader.setOnImageAvailableListener(null, null);
            this.mImageReader.close();
            this.mImageReader = null;
            this.mHandler.getLooper().quitSafely();
        } else {
            boolean z2 = true;
            if (this.mImageReader != null) {
                if (this.mImageReader.getWidth() != this.mPreviewWidth || this.mImageReader.getHeight() != this.mPreviewHeight) {
                    this.mImageReader.close();
                    this.mHandler.getLooper().quitSafely();
                } else {
                    z2 = false;
                }
            }
            if (z2) {
                this.mImageReader = ImageReader.newInstance(this.mPreviewWidth, this.mPreviewHeight, 842094169, 3);
                HandlerThread handlerThread = new HandlerThread("MatrixDisplay-ImageReader Handler Thread");
                handlerThread.start();
                this.mHandler = new Handler(handlerThread.getLooper());
                this.mImageReader.setOnImageAvailableListener(this.mImageAvailableListener, this.mHandler);
                while (i < 3) {
                    this.mPreviewBuffers[i] = new byte[((this.mPreviewWidth * this.mPreviewHeight) * ImageFormat.getBitsPerPixel(842094169)) / 8];
                    i++;
                }
            }
        }
    }

    @Override
    public void setSelectedEffect(String str) {
        this.mCurrentEffect = str;
    }

    private List<String> convertEnumToString(int[] iArr) {
        ModeEnum[] modeEnumArrValues = ModeEnum.values();
        ArrayList arrayList = new ArrayList(iArr.length);
        for (int i : iArr) {
            int length = modeEnumArrValues.length;
            int i2 = 0;
            while (true) {
                if (i2 < length) {
                    ModeEnum modeEnum = modeEnumArrValues[i2];
                    if (modeEnum.getValue() != i) {
                        i2++;
                    } else {
                        arrayList.add(modeEnum.getName().replace('_', '-').toLowerCase(Locale.ENGLISH));
                        break;
                    }
                }
            }
        }
        return arrayList;
    }

    private int convertStringToEnum(String str) {
        for (ModeEnum modeEnum : ModeEnum.values()) {
            if (modeEnum.getName().replace('_', '-').toLowerCase(Locale.ENGLISH).equalsIgnoreCase(str)) {
                return modeEnum.getValue();
            }
        }
        return 0;
    }

    private List<String> sizeToStr(List<Size> list) {
        ArrayList arrayList = new ArrayList(list.size());
        for (Size size : list) {
            arrayList.add(size.getWidth() + "x" + size.getHeight());
        }
        return arrayList;
    }

    private void sortSizeInDescending(List<Size> list) {
        int i = 0;
        while (i < list.size()) {
            int i2 = i + 1;
            int i3 = i;
            Size size = list.get(i);
            for (int i4 = i2; i4 < list.size(); i4++) {
                Size size2 = list.get(i4);
                if (size2.getWidth() * size2.getHeight() > size.getWidth() * size.getHeight()) {
                    i3 = i4;
                    size = size2;
                }
            }
            Size size3 = list.get(i);
            list.set(i, size);
            list.set(i3, size3);
            i = i2;
        }
    }
}
