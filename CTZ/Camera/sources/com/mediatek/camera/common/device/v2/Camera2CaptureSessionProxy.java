package com.mediatek.camera.common.device.v2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.os.Handler;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.utils.AtomAccessor;
import java.util.ArrayList;
import java.util.List;

public class Camera2CaptureSessionProxy implements AutoCloseable {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(Camera2CaptureSessionProxy.class.getSimpleName());
    private AtomAccessor mAtomAccessor = new AtomAccessor();
    private Camera2Proxy mCamera2Proxy;
    private final Handler mRequestHandler;

    public Camera2CaptureSessionProxy(Handler handler, Camera2Proxy camera2Proxy) {
        this.mRequestHandler = handler;
        this.mCamera2Proxy = camera2Proxy;
    }

    public Camera2Proxy getDevice() {
        return this.mCamera2Proxy;
    }

    public void prepare(Surface surface) throws CameraAccessException {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(201, surface));
    }

    public int capture(CaptureRequest captureRequest, CameraCaptureSession.CaptureCallback captureCallback, Handler handler) throws CameraAccessException {
        int[] iArr = new int[1];
        ArrayList arrayList = new ArrayList();
        arrayList.add(captureRequest);
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(202, new SessionOperatorInfo(arrayList, captureCallback, handler, iArr)));
        return iArr[0];
    }

    public int captureBurst(List<CaptureRequest> list, CameraCaptureSession.CaptureCallback captureCallback, Handler handler) throws CameraAccessException {
        int[] iArr = new int[1];
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(203, new SessionOperatorInfo(list, captureCallback, handler, iArr)));
        return iArr[0];
    }

    public int setRepeatingRequest(CaptureRequest captureRequest, CameraCaptureSession.CaptureCallback captureCallback, Handler handler) throws CameraAccessException {
        int[] iArr = new int[1];
        ArrayList arrayList = new ArrayList();
        arrayList.add(captureRequest);
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(204, new SessionOperatorInfo(arrayList, captureCallback, handler, iArr)));
        return iArr[0];
    }

    public int setRepeatingBurst(List<CaptureRequest> list, CameraCaptureSession.CaptureCallback captureCallback, Handler handler) throws CameraAccessException {
        int[] iArr = new int[1];
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(205, new SessionOperatorInfo(list, captureCallback, handler, iArr)));
        return iArr[0];
    }

    public List<CaptureRequest> createHighSpeedRequestList(CaptureRequest captureRequest) throws CameraAccessException {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        arrayList2.add(captureRequest);
        SessionOperatorInfo sessionOperatorInfo = new SessionOperatorInfo(arrayList2, (CameraCaptureSession.CaptureCallback) null, (Handler) null, arrayList);
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(211, sessionOperatorInfo));
        arrayList.addAll(sessionOperatorInfo.mResultRequest);
        return arrayList;
    }

    public void stopRepeating() throws CameraAccessException {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(206));
    }

    public void abortCaptures() throws CameraAccessException {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(207));
    }

    public boolean isReprocessable() {
        boolean[] zArr = new boolean[1];
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(210, zArr));
        return zArr[0];
    }

    public Surface getInputSurface() {
        Surface[] surfaceArr = new Surface[1];
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(208, surfaceArr));
        return surfaceArr[0];
    }

    public void finalizeOutputConfigurations(List<OutputConfiguration> list) {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(212, list));
    }

    @Override
    public void close() {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(209));
    }

    public static abstract class StateCallback {
        public abstract void onConfigureFailed(Camera2CaptureSessionProxy camera2CaptureSessionProxy);

        public abstract void onConfigured(Camera2CaptureSessionProxy camera2CaptureSessionProxy);

        public void onReady(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
        }

        public void onActive(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
        }

        public void onClosed(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
        }

        public void onSurfacePrepared(Camera2CaptureSessionProxy camera2CaptureSessionProxy, Surface surface) {
        }
    }

    class SessionOperatorInfo {
        CameraCaptureSession.CaptureCallback mCaptureCallback;
        List<CaptureRequest> mCaptureRequest;
        Handler mHandler;
        volatile List<CaptureRequest> mResultRequest;
        int[] mSessionNum;

        SessionOperatorInfo(List<CaptureRequest> list, CameraCaptureSession.CaptureCallback captureCallback, Handler handler, int[] iArr) {
            this.mCaptureRequest = list;
            this.mCaptureCallback = captureCallback;
            this.mHandler = handler;
            this.mSessionNum = iArr;
        }

        SessionOperatorInfo(List<CaptureRequest> list, CameraCaptureSession.CaptureCallback captureCallback, Handler handler, List<CaptureRequest> list2) {
            this.mCaptureRequest = list;
            this.mCaptureCallback = captureCallback;
            this.mHandler = handler;
            this.mResultRequest = list2;
        }
    }
}
