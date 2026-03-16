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

    public MtpImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mWeakReference = new WeakReference<>(this);
        this.mFetchLock = new Object();
        this.mFetchPending = false;
        this.mDrawMatrix = new Matrix();
        init();
    }

    public MtpImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mWeakReference = new WeakReference<>(this);
        this.mFetchLock = new Object();
        this.mFetchPending = false;
        this.mDrawMatrix = new Matrix();
        init();
    }

    private void showPlaceholder() {
        setImageResource(R.color.transparent);
    }

    public void setMtpDeviceAndObjectInfo(MtpDevice device, IngestObjectInfo object, int gen) {
        int handle = object.getObjectHandle();
        if (handle != this.mObjectHandle || gen != this.mGeneration) {
            cancelLoadingAndClear();
            showPlaceholder();
            this.mGeneration = gen;
            this.mObjectHandle = handle;
            this.mShowOverlayIcon = MtpDeviceIndex.SUPPORTED_VIDEO_FORMATS.contains(Integer.valueOf(object.getFormat()));
            if (this.mShowOverlayIcon && this.mOverlayIcon == null) {
                this.mOverlayIcon = getResources().getDrawable(com.android.gallery3d.R.drawable.ic_control_play);
                updateOverlayIconBounds();
            }
            synchronized (this.mFetchLock) {
                this.mFetchObjectInfo = object;
                this.mFetchDevice = device;
                if (!this.mFetchPending) {
                    this.mFetchPending = true;
                    sFetchHandler.sendMessage(sFetchHandler.obtainMessage(0, this.mWeakReference));
                }
            }
        }
    }

    protected Object fetchMtpImageDataFromDevice(MtpDevice device, IngestObjectInfo info) {
        return (info.getCompressedSize() > 8388608 || !MtpDeviceIndex.SUPPORTED_IMAGE_FORMATS.contains(Integer.valueOf(info.getFormat()))) ? new BitmapWithMetadata(MtpBitmapFetch.getThumbnail(device, info), 0) : MtpBitmapFetch.getFullsize(device, info);
    }

    private void updateDrawMatrix() {
        float dwidth;
        float dheight;
        float scale;
        this.mDrawMatrix.reset();
        float vheight = getHeight();
        float vwidth = getWidth();
        boolean rotated90 = this.mLastRotationDegrees % 180 != 0;
        if (rotated90) {
            dwidth = this.mLastBitmapHeight;
            dheight = this.mLastBitmapWidth;
        } else {
            dwidth = this.mLastBitmapWidth;
            dheight = this.mLastBitmapHeight;
        }
        if (dwidth <= vwidth && dheight <= vheight) {
            scale = 1.0f;
        } else {
            scale = Math.min(vwidth / dwidth, vheight / dheight);
        }
        this.mDrawMatrix.setScale(scale, scale);
        if (rotated90) {
            this.mDrawMatrix.postTranslate((-dheight) * scale * 0.5f, (-dwidth) * scale * 0.5f);
            this.mDrawMatrix.postRotate(this.mLastRotationDegrees);
            this.mDrawMatrix.postTranslate(dwidth * scale * 0.5f, dheight * scale * 0.5f);
        }
        this.mDrawMatrix.postTranslate((vwidth - (dwidth * scale)) * 0.5f, (vheight - (dheight * scale)) * 0.5f);
        if (!rotated90 && this.mLastRotationDegrees > 0) {
            this.mDrawMatrix.postRotate(this.mLastRotationDegrees, vwidth / 2.0f, vheight / 2.0f);
        }
        setImageMatrix(this.mDrawMatrix);
    }

    private void updateOverlayIconBounds() {
        int iheight = this.mOverlayIcon.getIntrinsicHeight();
        int iwidth = this.mOverlayIcon.getIntrinsicWidth();
        int vheight = getHeight();
        int vwidth = getWidth();
        float scaleHeight = vheight / (iheight * 4);
        float scaleWidth = vwidth / (iwidth * 4);
        if (scaleHeight >= 1.0f && scaleWidth >= 1.0f) {
            this.mOverlayIcon.setBounds((vwidth - iwidth) / 2, (vheight - iheight) / 2, (vwidth + iwidth) / 2, (vheight + iheight) / 2);
        } else {
            float scale = Math.min(scaleHeight, scaleWidth);
            this.mOverlayIcon.setBounds(((int) (vwidth - (iwidth * scale))) / 2, ((int) (vheight - (iheight * scale))) / 2, ((int) (vwidth + (iwidth * scale))) / 2, ((int) (vheight + (iheight * scale))) / 2);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && getScaleType() == ImageView.ScaleType.MATRIX) {
            updateDrawMatrix();
        }
        if (this.mShowOverlayIcon && changed && this.mOverlayIcon != null) {
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

    protected void onMtpImageDataFetchedFromDevice(Object result) {
        BitmapWithMetadata bitmapWithMetadata = (BitmapWithMetadata) result;
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
        public FetchImageHandler(Looper l) {
            super(l);
        }

        public static FetchImageHandler createOnNewThread() {
            HandlerThread t = new HandlerThread("MtpImageView Fetch");
            t.start();
            return new FetchImageHandler(t.getLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            MtpDevice device;
            IngestObjectInfo objectInfo;
            Object result;
            MtpImageView parent = (MtpImageView) ((WeakReference) msg.obj).get();
            if (parent != null) {
                synchronized (parent.mFetchLock) {
                    parent.mFetchPending = false;
                    device = parent.mFetchDevice;
                    objectInfo = parent.mFetchObjectInfo;
                }
                if (device != null && (result = parent.fetchMtpImageDataFromDevice(device, objectInfo)) != null) {
                    synchronized (parent.mFetchLock) {
                        if (parent.mFetchObjectInfo == objectInfo) {
                            parent.mFetchResult = result;
                            parent.mFetchDevice = null;
                            parent.mFetchObjectInfo = null;
                            MtpImageView.sFetchCompleteHandler.sendMessage(MtpImageView.sFetchCompleteHandler.obtainMessage(0, parent.mWeakReference));
                        }
                    }
                }
            }
        }
    }

    private static class ShowImageHandler extends Handler {
        private ShowImageHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            Object result;
            MtpImageView parent = (MtpImageView) ((WeakReference) msg.obj).get();
            if (parent != null) {
                synchronized (parent.mFetchLock) {
                    result = parent.mFetchResult;
                }
                if (result != null) {
                    parent.onMtpImageDataFetchedFromDevice(result);
                }
            }
        }
    }
}
