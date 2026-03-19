package com.android.gallery3d.ingest.ui;

import android.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.mtp.MtpDevice;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.gallery3d.ingest.data.BitmapWithMetadata;
import com.android.gallery3d.ingest.data.IngestObjectInfo;
import com.android.gallery3d.ingest.data.MtpBitmapFetch;
import com.android.gallery3d.ingest.data.MtpDeviceIndex;
import java.lang.ref.WeakReference;

public class MtpImageView extends ImageView {
    private Matrix mDrawMatrix;
    private MtpDevice mFetchDevice;
    private Object mFetchLock;
    private IngestObjectInfo mFetchObjectInfo;
    private boolean mFetchPending;
    private Object mFetchResult;
    private int mGeneration;
    private float mLastBitmapHeight;
    private float mLastBitmapWidth;
    private int mLastRotationDegrees;
    private int mObjectHandle;
    private Drawable mOverlayIcon;
    private boolean mShowOverlayIcon;
    private WeakReference<MtpImageView> mWeakReference;
    private static final FetchImageHandler sFetchHandler = FetchImageHandler.createOnNewThread();
    private static final ShowImageHandler sFetchCompleteHandler = new ShowImageHandler();

    private void init() {
        showPlaceholder();
    }

    public MtpImageView(Context context) {
        super(context);
        this.mWeakReference = new WeakReference<>(this);
        this.mFetchLock = new Object();
        this.mFetchPending = false;
        this.mDrawMatrix = new Matrix();
        init();
    }

    public MtpImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mWeakReference = new WeakReference<>(this);
        this.mFetchLock = new Object();
        this.mFetchPending = false;
        this.mDrawMatrix = new Matrix();
        init();
    }

    public MtpImageView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mWeakReference = new WeakReference<>(this);
        this.mFetchLock = new Object();
        this.mFetchPending = false;
        this.mDrawMatrix = new Matrix();
        init();
    }

    private void showPlaceholder() {
        setImageResource(R.color.transparent);
    }

    public void setMtpDeviceAndObjectInfo(MtpDevice mtpDevice, IngestObjectInfo ingestObjectInfo, int i) {
        int objectHandle = ingestObjectInfo.getObjectHandle();
        if (objectHandle == this.mObjectHandle && i == this.mGeneration) {
            return;
        }
        cancelLoadingAndClear();
        showPlaceholder();
        this.mGeneration = i;
        this.mObjectHandle = objectHandle;
        this.mShowOverlayIcon = MtpDeviceIndex.SUPPORTED_VIDEO_FORMATS.contains(Integer.valueOf(ingestObjectInfo.getFormat()));
        if (this.mShowOverlayIcon && this.mOverlayIcon == null) {
            this.mOverlayIcon = getResources().getDrawable(com.android.gallery3d.R.drawable.ic_control_play);
            updateOverlayIconBounds();
        }
        synchronized (this.mFetchLock) {
            this.mFetchObjectInfo = ingestObjectInfo;
            this.mFetchDevice = mtpDevice;
            if (this.mFetchPending) {
                return;
            }
            this.mFetchPending = true;
            sFetchHandler.sendMessage(sFetchHandler.obtainMessage(0, this.mWeakReference));
        }
    }

    protected Object fetchMtpImageDataFromDevice(MtpDevice mtpDevice, IngestObjectInfo ingestObjectInfo) {
        if (ingestObjectInfo.getCompressedSize() <= 8388608 && MtpDeviceIndex.SUPPORTED_IMAGE_FORMATS.contains(Integer.valueOf(ingestObjectInfo.getFormat()))) {
            return MtpBitmapFetch.getFullsize(mtpDevice, ingestObjectInfo);
        }
        return new BitmapWithMetadata(MtpBitmapFetch.getThumbnail(mtpDevice, ingestObjectInfo), 0);
    }

    private void updateDrawMatrix() {
        float f;
        float f2;
        float fMin;
        this.mDrawMatrix.reset();
        float height = getHeight();
        float width = getWidth();
        boolean z = this.mLastRotationDegrees % 180 != 0;
        if (z) {
            f = this.mLastBitmapHeight;
            f2 = this.mLastBitmapWidth;
        } else {
            f = this.mLastBitmapWidth;
            f2 = this.mLastBitmapHeight;
        }
        if (f <= width && f2 <= height) {
            fMin = 1.0f;
        } else {
            fMin = Math.min(width / f, height / f2);
        }
        this.mDrawMatrix.setScale(fMin, fMin);
        if (z) {
            this.mDrawMatrix.postTranslate((-f2) * fMin * 0.5f, (-f) * fMin * 0.5f);
            this.mDrawMatrix.postRotate(this.mLastRotationDegrees);
            this.mDrawMatrix.postTranslate(f * fMin * 0.5f, f2 * fMin * 0.5f);
        }
        this.mDrawMatrix.postTranslate((width - (f * fMin)) * 0.5f, (height - (f2 * fMin)) * 0.5f);
        if (!z && this.mLastRotationDegrees > 0) {
            this.mDrawMatrix.postRotate(this.mLastRotationDegrees, width / 2.0f, height / 2.0f);
        }
        setImageMatrix(this.mDrawMatrix);
    }

    private void updateOverlayIconBounds() {
        int intrinsicHeight = this.mOverlayIcon.getIntrinsicHeight();
        int intrinsicWidth = this.mOverlayIcon.getIntrinsicWidth();
        int height = getHeight();
        int width = getWidth();
        float f = height;
        float f2 = f / (intrinsicHeight * 4);
        float f3 = width;
        float f4 = f3 / (intrinsicWidth * 4);
        if (f2 >= 1.0f && f4 >= 1.0f) {
            this.mOverlayIcon.setBounds((width - intrinsicWidth) / 2, (height - intrinsicHeight) / 2, (width + intrinsicWidth) / 2, (height + intrinsicHeight) / 2);
            return;
        }
        float fMin = Math.min(f2, f4);
        float f5 = intrinsicWidth * fMin;
        float f6 = fMin * intrinsicHeight;
        this.mOverlayIcon.setBounds(((int) (f3 - f5)) / 2, ((int) (f - f6)) / 2, ((int) (f3 + f5)) / 2, ((int) (f + f6)) / 2);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (z && getScaleType() == ImageView.ScaleType.MATRIX) {
            updateDrawMatrix();
        }
        if (this.mShowOverlayIcon && z && this.mOverlayIcon != null) {
            updateOverlayIconBounds();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mShowOverlayIcon && this.mOverlayIcon != null) {
            this.mOverlayIcon.draw(canvas);
        }
    }

    protected void onMtpImageDataFetchedFromDevice(Object obj) {
        BitmapWithMetadata bitmapWithMetadata = (BitmapWithMetadata) obj;
        if (getScaleType() == ImageView.ScaleType.MATRIX) {
            this.mLastBitmapHeight = bitmapWithMetadata.bitmap.getHeight();
            this.mLastBitmapWidth = bitmapWithMetadata.bitmap.getWidth();
            this.mLastRotationDegrees = bitmapWithMetadata.rotationDegrees;
            updateDrawMatrix();
        } else {
            setRotation(bitmapWithMetadata.rotationDegrees);
        }
        setAlpha(0.0f);
        setImageBitmap(bitmapWithMetadata.bitmap);
        animate().alpha(1.0f);
    }

    protected void cancelLoadingAndClear() {
        synchronized (this.mFetchLock) {
            this.mFetchDevice = null;
            this.mFetchObjectInfo = null;
            this.mFetchResult = null;
        }
        animate().cancel();
        setImageResource(R.color.transparent);
    }

    @Override
    public void onDetachedFromWindow() {
        cancelLoadingAndClear();
        super.onDetachedFromWindow();
    }

    private static class FetchImageHandler extends Handler {
        public FetchImageHandler(Looper looper) {
            super(looper);
        }

        public static FetchImageHandler createOnNewThread() {
            HandlerThread handlerThread = new HandlerThread("MtpImageView Fetch");
            handlerThread.start();
            return new FetchImageHandler(handlerThread.getLooper());
        }

        @Override
        public void handleMessage(Message message) {
            MtpDevice mtpDevice;
            IngestObjectInfo ingestObjectInfo;
            Object objFetchMtpImageDataFromDevice;
            MtpImageView mtpImageView = (MtpImageView) ((WeakReference) message.obj).get();
            if (mtpImageView != null) {
                synchronized (mtpImageView.mFetchLock) {
                    mtpImageView.mFetchPending = false;
                    mtpDevice = mtpImageView.mFetchDevice;
                    ingestObjectInfo = mtpImageView.mFetchObjectInfo;
                }
                if (mtpDevice != null && (objFetchMtpImageDataFromDevice = mtpImageView.fetchMtpImageDataFromDevice(mtpDevice, ingestObjectInfo)) != null) {
                    synchronized (mtpImageView.mFetchLock) {
                        if (mtpImageView.mFetchObjectInfo != ingestObjectInfo) {
                            return;
                        }
                        mtpImageView.mFetchResult = objFetchMtpImageDataFromDevice;
                        mtpImageView.mFetchDevice = null;
                        mtpImageView.mFetchObjectInfo = null;
                        MtpImageView.sFetchCompleteHandler.sendMessage(MtpImageView.sFetchCompleteHandler.obtainMessage(0, mtpImageView.mWeakReference));
                    }
                }
            }
        }
    }

    private static class ShowImageHandler extends Handler {
        private ShowImageHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            Object obj;
            MtpImageView mtpImageView = (MtpImageView) ((WeakReference) message.obj).get();
            if (mtpImageView != null) {
                synchronized (mtpImageView.mFetchLock) {
                    obj = mtpImageView.mFetchResult;
                }
                if (obj == null) {
                    return;
                }
                mtpImageView.onMtpImageDataFetchedFromDevice(obj);
            }
        }
    }
}
