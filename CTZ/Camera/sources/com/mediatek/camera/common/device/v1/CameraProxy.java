package com.mediatek.camera.common.device.v1;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Message;
import android.view.SurfaceHolder;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.CameraProxyBase;
import com.mediatek.camera.common.device.CameraStateCallback;
import com.mediatek.camera.common.utils.AtomAccessor;
import java.io.IOException;

public class CameraProxy extends CameraProxyBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(CameraProxy.class.getSimpleName());
    private Camera mCamera;
    private String mCameraId;
    private CameraHandler mRequestHandler;
    private Object mWaitDoneObject = new Object();
    private AtomAccessor mAtomAccessor = new AtomAccessor();
    final Runnable mWaitLockRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraProxy.this.mWaitDoneObject) {
                try {
                    LogHelper.d(CameraProxy.TAG, "[waitLockRunnable] wait +");
                    CameraProxy.this.mWaitDoneObject.wait();
                    LogHelper.d(CameraProxy.TAG, "[waitLockRunnable] wait -");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    final Runnable mResumeLockRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraProxy.this.mWaitDoneObject) {
                CameraProxy.this.mWaitDoneObject.notifyAll();
                LogHelper.d(CameraProxy.TAG, "[resumeLockRunnable] notifyAll ");
            }
        }
    };

    public static abstract class StateCallback extends CameraStateCallback {
        public abstract void onClosed(CameraProxy cameraProxy);

        public abstract void onError(CameraProxy cameraProxy, int i);

        public abstract void onOpened(CameraProxy cameraProxy);
    }

    public interface VendorDataCallback {
        void onDataCallback(int i, byte[] bArr, int i2, int i3);

        void onDataTaken(Message message);
    }

    public CameraProxy(String str, CameraHandler cameraHandler, Camera camera) {
        this.mCameraId = null;
        this.mRequestHandler = null;
        this.mCamera = null;
        this.mCameraId = str;
        this.mRequestHandler = cameraHandler;
        this.mCamera = camera;
    }

    @Override
    public String getId() {
        return this.mCameraId;
    }

    @Override
    public CameraDeviceManagerFactory.CameraApi getApiType() {
        return CameraDeviceManagerFactory.CameraApi.API1;
    }

    public Camera getCamera() {
        return this.mCamera;
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder) throws IOException {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(106, surfaceHolder));
    }

    public void setPreviewTexture(SurfaceTexture surfaceTexture) throws IOException {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(101, surfaceTexture));
    }

    public void startPreview() {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(102));
    }

    public void startPreviewAsync() {
        this.mAtomAccessor.sendAtomMessage(this.mRequestHandler, this.mRequestHandler.obtainMessage(102));
    }

    public void stopPreview() {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(103));
    }

    public void stopPreviewAsync() {
        this.mAtomAccessor.sendAtomMessage(this.mRequestHandler, this.mRequestHandler.obtainMessage(103));
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(107, previewCallback));
    }

    public void setOneShotPreviewCallback(Camera.PreviewCallback previewCallback) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(108, previewCallback));
    }

    public void setPreviewCallbackWithBuffer(Camera.PreviewCallback previewCallback) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(104, previewCallback));
    }

    public void addCallbackBuffer(byte[] bArr) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(105, bArr));
    }

    public void autoFocus(Camera.AutoFocusCallback autoFocusCallback) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(301, autoFocusCallback));
    }

    public void cancelAutoFocus() {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(302));
    }

    public void setAutoFocusMoveCallback(Camera.AutoFocusMoveCallback autoFocusMoveCallback) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(303, autoFocusMoveCallback));
    }

    public void takePicture(Camera.ShutterCallback shutterCallback, Camera.PictureCallback pictureCallback, Camera.PictureCallback pictureCallback2, Camera.PictureCallback pictureCallback3) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(601, new CaptureCallbackGroup(shutterCallback, pictureCallback, pictureCallback2, pictureCallback3)));
    }

    public void takePictureAsync(Camera.ShutterCallback shutterCallback, Camera.PictureCallback pictureCallback, Camera.PictureCallback pictureCallback2, Camera.PictureCallback pictureCallback3) {
        this.mAtomAccessor.sendAtomMessage(this.mRequestHandler, this.mRequestHandler.obtainMessage(601, new CaptureCallbackGroup(shutterCallback, pictureCallback, pictureCallback2, pictureCallback3)));
    }

    public void startSmoothZoom(int i) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(305, Integer.valueOf(i)));
    }

    public void stopSmoothZoom() {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(306));
    }

    public void setDisplayOrientation(int i) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(502, Integer.valueOf(i)));
    }

    public boolean enableShutterSound(boolean z) {
        boolean[] zArr = new boolean[1];
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(501, zArr));
        return zArr[0];
    }

    public void setZoomChangeListener(Camera.OnZoomChangeListener onZoomChangeListener) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(304, onZoomChangeListener));
    }

    public void setFaceDetectionListener(Camera.FaceDetectionListener faceDetectionListener) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(461, faceDetectionListener));
    }

    public void startFaceDetection() {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(462));
    }

    public void stopFaceDetection() {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(463));
    }

    public void setParameters(Camera.Parameters parameters) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(201, parameters));
    }

    public Camera.Parameters getOriginalParameters(boolean z) {
        if (z) {
            Camera.Parameters[] parametersArr = new Camera.Parameters[1];
            if (this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(203, z ? 1 : 0, 0, parametersArr)) && parametersArr[0] != null) {
                return parametersArr[0];
            }
            return this.mRequestHandler.getOriginalParameters();
        }
        return this.mRequestHandler.getOriginalParameters();
    }

    public Camera.Parameters getParameters() {
        Camera.Parameters[] parametersArr = new Camera.Parameters[1];
        if (this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(202, parametersArr)) && parametersArr[0] != null) {
            return parametersArr[0];
        }
        return this.mRequestHandler.getOriginalParameters();
    }

    public void sendCommand(int i, int i2, int i3) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(701, new CommandInfo(i, i2, i3)));
    }

    public void setVendorDataCallback(int i, VendorDataCallback vendorDataCallback) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(702, new VendCallbackInfo(i, vendorDataCallback)));
    }

    public void lock(boolean z) {
        if (z) {
            this.mAtomAccessor.sendAtomMessageAtFrontOfQueue(this.mRequestHandler, this.mRequestHandler.obtainMessage(4));
        }
        this.mResumeLockRunnable.run();
    }

    public void unlock() {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(3), this.mWaitLockRunnable);
    }

    public void reconnect() throws IOException {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(2));
    }

    public void close() {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(1));
    }

    public void closeAsync() {
        this.mAtomAccessor.sendAtomMessage(this.mRequestHandler, this.mRequestHandler.obtainMessage(1));
    }

    final class CaptureCallbackGroup {
        final Camera.PictureCallback mJpegCallback;
        final Camera.PictureCallback mPostViewCallback;
        final Camera.PictureCallback mRawCallback;
        final Camera.ShutterCallback mShutterCallback;

        CaptureCallbackGroup(Camera.ShutterCallback shutterCallback, Camera.PictureCallback pictureCallback, Camera.PictureCallback pictureCallback2, Camera.PictureCallback pictureCallback3) {
            this.mShutterCallback = shutterCallback;
            this.mRawCallback = pictureCallback;
            this.mPostViewCallback = pictureCallback2;
            this.mJpegCallback = pictureCallback3;
        }
    }

    final class CommandInfo {
        final int mArg1;
        final int mArg2;
        final int mCommand;

        CommandInfo(int i, int i2, int i3) {
            this.mCommand = i;
            this.mArg1 = i2;
            this.mArg2 = i3;
        }
    }

    final class VendCallbackInfo {
        final VendorDataCallback mArg1;
        final int mMsgId;

        VendCallbackInfo(int i, VendorDataCallback vendorDataCallback) {
            this.mMsgId = i;
            this.mArg1 = vendorDataCallback;
        }
    }
}
