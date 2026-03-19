package com.mediatek.camera.feature.setting.matrixdisplay;

import android.graphics.ImageFormat;
import android.graphics.Point;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;
import com.mediatek.camera.common.debug.CameraSysTrace;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.feature.setting.matrixdisplay.MatrixDisplayViewManager;
import com.mediatek.camera.matrixdisplay.ext.MatrixDisplayExt;
import java.util.ArrayList;

public class MatrixDisplayHandler implements IPreviewFrameCallback, MatrixDisplayViewManager.EffectUpdateListener, MatrixDisplayViewManager.SurfaceAvailableListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MatrixDisplayHandler.class.getSimpleName());
    private EffectAvailableListener mEffectAvailableListener;
    private Handler mHandler;
    private long mInputStartTime;
    private MatrixDisplayExt mMatrixDisplayExt;
    private int mMaxSurfaceBufferHeight;
    private int mMaxSurfaceBufferWidth;
    private int[] mEffectIndexs = new int[12];
    private byte[][] mEffectsBuffers = new byte[36][];
    private ArrayList<byte[]> mCacheBuffer = new ArrayList<>();
    private ConditionVariable mReleaseCondition = new ConditionVariable();
    private volatile boolean mIsReleased = false;
    private volatile boolean mHasRegisterBuffer = false;
    private volatile int mNumOfCurrentProcess = 0;
    private volatile int mNumOfDropFrame = 6;
    private volatile int mCacheIndex = 0;
    private int mInputFrames = 0;
    private Object mHandlerLock = new Object();
    private MatrixDisplayExt.EffectAvailableCallback mEffectsCallback = new MatrixDisplayExt.EffectAvailableCallback() {
    };

    public interface EffectAvailableListener {
    }

    static int access$510(MatrixDisplayHandler matrixDisplayHandler) {
        int i = matrixDisplayHandler.mNumOfCurrentProcess;
        matrixDisplayHandler.mNumOfCurrentProcess = i - 1;
        return i;
    }

    @Override
    public void onPreviewFrameAvailable(byte[] bArr) {
        LogHelper.d(TAG, "[onPreviewFrameAvailable] pv callback data:" + bArr.length + ", mNumOfCurrentProcess:" + this.mNumOfCurrentProcess);
        if (this.mInputFrames == 0) {
            this.mInputStartTime = System.currentTimeMillis();
        }
        this.mInputFrames++;
        if (this.mInputFrames % 20 == 0) {
            long jCurrentTimeMillis = System.currentTimeMillis() - this.mInputStartTime;
            LogHelper.d(TAG, "[onPreviewFrameAvailable] pv callback fps:" + (20000 / jCurrentTimeMillis));
            this.mInputStartTime = System.currentTimeMillis();
        }
        if (this.mNumOfCurrentProcess == 2) {
            synchronized (this.mHandlerLock) {
                if (this.mHandler != null) {
                    this.mHandler.removeMessages(103);
                    this.mNumOfCurrentProcess = 1;
                }
            }
        }
        if (this.mNumOfCurrentProcess < 2 && this.mNumOfDropFrame >= 6) {
            if (this.mHasRegisterBuffer) {
                processEffect(bArr);
            }
        } else if (this.mNumOfDropFrame < 6) {
            this.mNumOfDropFrame++;
        }
    }

    @Override
    public void onEffectUpdated(int i, int i2) {
        this.mEffectIndexs[i] = i2;
    }

    @Override
    public void onSurfaceAvailable(Surface surface, int i, int i2, int i3) {
        if (!this.mHasRegisterBuffer) {
            Point pointUpdateSurfaceSize = updateSurfaceSize(i, i2);
            LogHelper.d(TAG, "[onSurfaceAvailable] register buffer size, bufferWidth:" + pointUpdateSurfaceSize.x + ",bufferHeight:" + pointUpdateSurfaceSize.y);
            synchronized (this.mHandlerLock) {
                if (this.mHandler != null) {
                    this.mHandler.obtainMessage(101, pointUpdateSurfaceSize.x, pointUpdateSurfaceSize.y).sendToTarget();
                    this.mHasRegisterBuffer = true;
                }
            }
        }
        synchronized (this.mHandlerLock) {
            if (this.mHandler != null) {
                this.mHandler.obtainMessage(102, i3, 0, surface).sendToTarget();
            }
        }
    }

    public synchronized void initialize(int i, int i2, int i3, int i4, int i5) {
        LogHelper.d(TAG, "[initialize]previewWidth:" + i + ", previewHeight" + i2 + ",imageFormat:" + i3);
        this.mMaxSurfaceBufferWidth = Math.max(i4, i5) / 4;
        this.mMaxSurfaceBufferHeight = Math.min(i4, i5) / 4;
        this.mMatrixDisplayExt = MatrixDisplayExt.getInstance();
        synchronized (this.mHandlerLock) {
            if (this.mHandler == null) {
                HandlerThread handlerThread = new HandlerThread("draw buffer handler thread", -4);
                handlerThread.start();
                this.mHandler = new EffectHandler(handlerThread.getLooper());
            }
        }
        for (int i6 = 0; i6 < this.mEffectIndexs.length; i6++) {
            this.mEffectIndexs[i6] = -1;
        }
        int bitsPerPixel = ((i * i2) * ImageFormat.getBitsPerPixel(i3)) / 8;
        synchronized (this.mCacheBuffer) {
            if (this.mCacheBuffer.size() == 0) {
                for (int i7 = 0; i7 < 3; i7++) {
                    this.mCacheBuffer.add(new byte[bitsPerPixel]);
                }
            }
        }
        this.mCacheIndex = 0;
        this.mNumOfDropFrame = 0;
        this.mNumOfCurrentProcess = 0;
        this.mMatrixDisplayExt.setCallback(this.mEffectsCallback);
        synchronized (this.mHandlerLock) {
            if (this.mHandler != null) {
                this.mHandler.obtainMessage(100, i, i2).sendToTarget();
            }
        }
    }

    public synchronized void release() {
        if (this.mIsReleased) {
            return;
        }
        if (this.mMatrixDisplayExt != null) {
            this.mMatrixDisplayExt.setCallback(null);
        }
        LogHelper.d(TAG, "[release] mHandler:" + this.mHandler);
        synchronized (this.mHandlerLock) {
            if (this.mHandler != null) {
                this.mHandler.sendEmptyMessage(104);
                this.mReleaseCondition.block();
                this.mReleaseCondition.close();
                this.mHandler.getLooper().quit();
                this.mHandler = null;
            }
        }
        this.mMatrixDisplayExt = null;
        LogHelper.d(TAG, "[release] end");
    }

    public void setEffectAvailableListener(EffectAvailableListener effectAvailableListener) {
        this.mEffectAvailableListener = effectAvailableListener;
    }

    private Point updateSurfaceSize(int i, int i2) {
        LogHelper.d(TAG, "[updateSurfaceSize] input size, width = " + i + ", height = " + i2 + ", mMaxSurfaceBufferWidth = " + this.mMaxSurfaceBufferWidth + ", mMaxSurfaceBufferHeight = " + this.mMaxSurfaceBufferHeight);
        int i3 = i2;
        float f = 1.1f;
        int i4 = i;
        while (true) {
            if (i4 > this.mMaxSurfaceBufferWidth || i3 > this.mMaxSurfaceBufferHeight) {
                i4 = (int) (i / f);
                i3 = (int) (i2 / f);
                f += 0.1f;
            } else {
                int i5 = (i4 / 32) * 32;
                int i6 = (i3 / 16) * 16;
                Point point = new Point(i5, i6);
                LogHelper.d(TAG, "[updateSurfaceSize] output size,newWidth:" + i5 + ",newHeight:" + i6);
                return point;
            }
        }
    }

    private void processEffect(byte[] bArr) {
        byte[] bArr2;
        if (bArr == null) {
            LogHelper.w(TAG, "[processEffect] data is null, return");
            return;
        }
        synchronized (this.mCacheBuffer) {
            bArr2 = this.mCacheBuffer.get(this.mCacheIndex);
        }
        if (bArr.length != bArr2.length) {
            LogHelper.d(TAG, "[processEffect]preview buffer size is larger,return!");
            return;
        }
        System.arraycopy(bArr, 0, bArr2, 0, bArr.length);
        this.mCacheIndex = (this.mCacheIndex + 1) % 3;
        this.mNumOfCurrentProcess++;
        synchronized (this.mHandlerLock) {
            if (this.mHandler != null) {
                this.mHandler.sendMessage(this.mHandler.obtainMessage(103, this.mCacheIndex, 0, bArr2));
            }
        }
    }

    private void releaseMatrixDisplay() {
        LogHelper.d(TAG, "<releaseMatrixDisplay>");
        if (!this.mIsReleased) {
            this.mIsReleased = true;
            this.mHasRegisterBuffer = false;
            this.mMatrixDisplayExt.release();
            for (int i = 0; i < this.mEffectsBuffers.length; i++) {
                this.mEffectsBuffers[i] = null;
            }
            synchronized (this.mCacheBuffer) {
                this.mCacheBuffer.clear();
            }
        }
        this.mReleaseCondition.open();
    }

    private class EffectHandler extends Handler {
        public EffectHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogHelper.d(MatrixDisplayHandler.TAG, "<handleMessage> msg.what:" + message.what + ",mIsReleased:" + MatrixDisplayHandler.this.mIsReleased);
            switch (message.what) {
                case 100:
                    LogHelper.d(MatrixDisplayHandler.TAG, "<handleMessage> previewWidth:" + message.arg1 + ",previewHeight:" + message.arg2);
                    MatrixDisplayHandler.this.mMatrixDisplayExt.initialize(message.arg1, message.arg2, 12, 11);
                    MatrixDisplayHandler.this.mIsReleased = false;
                    break;
                case 101:
                    if (!MatrixDisplayHandler.this.mIsReleased) {
                        int i = ((message.arg1 * message.arg2) * 3) / 2;
                        for (int i2 = 0; i2 < 36; i2++) {
                            if (MatrixDisplayHandler.this.mEffectsBuffers[i2] == null) {
                                MatrixDisplayHandler.this.mEffectsBuffers[i2] = new byte[i];
                            }
                        }
                        MatrixDisplayHandler.this.mMatrixDisplayExt.setBuffers(message.arg1, message.arg2, MatrixDisplayHandler.this.mEffectsBuffers);
                        break;
                    }
                    break;
                case 102:
                    if (!MatrixDisplayHandler.this.mIsReleased) {
                        MatrixDisplayHandler.this.mMatrixDisplayExt.setSurface((Surface) message.obj, message.arg1);
                        break;
                    }
                    break;
                case 103:
                    if (!MatrixDisplayHandler.this.mIsReleased) {
                        long jCurrentTimeMillis = System.currentTimeMillis();
                        CameraSysTrace.onEventSystrace("process frame", true);
                        MatrixDisplayHandler.this.mMatrixDisplayExt.process((byte[]) message.obj, MatrixDisplayHandler.this.mEffectIndexs);
                        MatrixDisplayHandler.access$510(MatrixDisplayHandler.this);
                        CameraSysTrace.onEventSystrace("process frame", false);
                        long jCurrentTimeMillis2 = System.currentTimeMillis();
                        LogHelper.d(MatrixDisplayHandler.TAG, "process time:" + (jCurrentTimeMillis2 - jCurrentTimeMillis));
                        break;
                    }
                    break;
                case 104:
                    MatrixDisplayHandler.this.releaseMatrixDisplay();
                    break;
                default:
                    LogHelper.d(MatrixDisplayHandler.TAG, "<handleMessage>unrecognized message!");
                    break;
            }
        }
    }
}
