package com.mediatek.camera.common.device.v2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.OutputConfiguration;
import android.os.Handler;
import android.view.Surface;
import com.google.common.base.Preconditions;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.CameraProxyBase;
import com.mediatek.camera.common.device.CameraStateCallback;
import com.mediatek.camera.common.device.v2.Camera2CaptureSessionProxy;
import com.mediatek.camera.common.utils.AtomAccessor;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Camera2Proxy extends CameraProxyBase {
    public static final int TEMPLATE_MANUAL = 6;
    public static final int TEMPLATE_PREVIEW = 1;
    public static final int TEMPLATE_RECORD = 3;
    public static final int TEMPLATE_STILL_CAPTURE = 2;
    public static final int TEMPLATE_VIDEO_SNAPSHOT = 4;
    public static final int TEMPLATE_ZERO_SHUTTER_LAG = 5;
    private final CameraDevice mCameraDevice;
    private final String mCameraId;
    private CaptureRequest.Builder mCaptureBuilder;
    private CaptureRequest.Builder mReprocessBuilder;
    private final Camera2Handler mRequestHandler;
    private final Handler mRespondHandler;
    private final AtomicBoolean mClosing = new AtomicBoolean();
    private final Object mInterfaceLock = new Object();
    private AtomAccessor mAtomAccessor = new AtomAccessor();

    public Camera2Proxy(String str, CameraDevice cameraDevice, Camera2Handler camera2Handler, Handler handler) {
        this.mCameraDevice = cameraDevice;
        this.mCameraId = str;
        this.mRequestHandler = camera2Handler;
        this.mRespondHandler = handler;
    }

    @Override
    public String getId() {
        return this.mCameraId;
    }

    @Override
    public CameraDeviceManagerFactory.CameraApi getApiType() {
        return CameraDeviceManagerFactory.CameraApi.API2;
    }

    public Handler getRespondHandler() {
        return this.mRespondHandler;
    }

    public CameraDevice getCameraDevice() {
        return this.mCameraDevice;
    }

    public void createCaptureSession(List<Surface> list, Camera2CaptureSessionProxy.StateCallback stateCallback, Handler handler) throws CameraAccessException {
        this.mAtomAccessor.sendAtomMessage(this.mRequestHandler, this.mRequestHandler.obtainMessage(101, new SessionCreatorInfo(list, stateCallback, handler, (InputConfiguration) null)));
    }

    public void createCaptureSession(List<Surface> list, Camera2CaptureSessionProxy.StateCallback stateCallback, Handler handler, CaptureRequest.Builder builder) throws CameraAccessException {
        this.mAtomAccessor.sendAtomMessage(this.mRequestHandler, this.mRequestHandler.obtainMessage(101, new SessionCreatorInfo(list, stateCallback, handler, null, builder)));
    }

    public void createCaptureSession(Camera2CaptureSessionProxy.StateCallback stateCallback, Handler handler, CaptureRequest.Builder builder, List<OutputConfiguration> list) throws CameraAccessException {
        this.mAtomAccessor.sendAtomMessage(this.mRequestHandler, this.mRequestHandler.obtainMessage(101, new SessionCreatorInfo(list, stateCallback, handler, builder)));
    }

    public void createReprocessableCaptureSession(InputConfiguration inputConfiguration, List<Surface> list, Camera2CaptureSessionProxy.StateCallback stateCallback, Handler handler) throws CameraAccessException {
        Preconditions.checkNotNull(inputConfiguration, "createReprocessableCaptureSession,the inputConfig nust not null");
        Preconditions.checkNotNull(list, "createReprocessableCaptureSession,the surface must not null");
        Preconditions.checkNotNull(stateCallback, "createReprocessableCaptureSession, the state callback must not null");
        this.mAtomAccessor.sendAtomMessage(this.mRequestHandler, this.mRequestHandler.obtainMessage(102, new SessionCreatorInfo(list, stateCallback, handler, inputConfiguration)));
    }

    public void createConstrainedHighSpeedCaptureSession(List<Surface> list, Camera2CaptureSessionProxy.StateCallback stateCallback, Handler handler) throws CameraAccessException {
        Preconditions.checkNotNull(list, "createConstrainedHighSpeedCaptureSession,the surface must not null");
        Preconditions.checkNotNull(stateCallback, "createConstrainedHighSpeedCaptureSession, the callback must not null");
        this.mAtomAccessor.sendAtomMessage(this.mRequestHandler, this.mRequestHandler.obtainMessage(103, new SessionCreatorInfo(list, stateCallback, handler, (InputConfiguration) null)));
    }

    public CaptureRequest.Builder createCaptureRequest(int i) throws CameraAccessException {
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(104, i, 0, new RequestCreatorInfo(null)));
        return this.mCaptureBuilder;
    }

    public CaptureRequest.Builder createReprocessCaptureRequest(TotalCaptureResult totalCaptureResult) throws CameraAccessException {
        Preconditions.checkNotNull(totalCaptureResult, "createReprocessCaptureRequest,the total capture result must not null");
        this.mAtomAccessor.sendAtomMessageAndWait(this.mRequestHandler, this.mRequestHandler.obtainMessage(105, new RequestCreatorInfo(totalCaptureResult)));
        return this.mReprocessBuilder;
    }

    public static abstract class StateCallback extends CameraStateCallback {
        public abstract void onDisconnected(Camera2Proxy camera2Proxy);

        public abstract void onError(Camera2Proxy camera2Proxy, int i);

        public abstract void onOpened(Camera2Proxy camera2Proxy);

        public void onClosed(Camera2Proxy camera2Proxy) {
        }
    }

    final class SessionCreatorInfo {
        final CaptureRequest.Builder mBuilder;
        final Camera2CaptureSessionProxy.StateCallback mCallback;
        final Handler mHandler;
        final InputConfiguration mInputConfiguration;
        final List<OutputConfiguration> mOutputConfigs;
        final List<Surface> mSurfaces;

        SessionCreatorInfo(List<Surface> list, Camera2CaptureSessionProxy.StateCallback stateCallback, Handler handler, InputConfiguration inputConfiguration) {
            this.mSurfaces = list;
            this.mCallback = stateCallback;
            this.mHandler = handler;
            this.mInputConfiguration = inputConfiguration;
            this.mBuilder = null;
            this.mOutputConfigs = null;
        }

        SessionCreatorInfo(List<Surface> list, Camera2CaptureSessionProxy.StateCallback stateCallback, Handler handler, InputConfiguration inputConfiguration, CaptureRequest.Builder builder) {
            this.mSurfaces = list;
            this.mCallback = stateCallback;
            this.mHandler = handler;
            this.mInputConfiguration = inputConfiguration;
            this.mBuilder = builder;
            this.mOutputConfigs = null;
        }

        SessionCreatorInfo(List<OutputConfiguration> list, Camera2CaptureSessionProxy.StateCallback stateCallback, Handler handler, CaptureRequest.Builder builder) {
            this.mSurfaces = null;
            this.mCallback = stateCallback;
            this.mHandler = handler;
            this.mInputConfiguration = null;
            this.mBuilder = builder;
            this.mOutputConfigs = list;
        }
    }

    final class RequestCreatorInfo {
        TotalCaptureResult mResult;

        RequestCreatorInfo(TotalCaptureResult totalCaptureResult) {
            this.mResult = totalCaptureResult;
        }

        void setCaptureRequestBuilder(CaptureRequest.Builder builder) {
            Camera2Proxy.this.mCaptureBuilder = builder;
        }

        void setReprocessRequestBuilder(CaptureRequest.Builder builder) {
            Camera2Proxy.this.mReprocessBuilder = builder;
        }
    }

    private boolean isClosed() {
        return this.mClosing.get();
    }
}
