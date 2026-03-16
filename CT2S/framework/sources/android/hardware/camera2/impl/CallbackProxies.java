package android.hardware.camera2.impl;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.dispatch.Dispatchable;
import android.hardware.camera2.dispatch.MethodNameInvoker;
import android.hardware.camera2.impl.CameraDeviceImpl;
import com.android.internal.util.Preconditions;

public class CallbackProxies {

    public static class DeviceStateCallbackProxy extends CameraDeviceImpl.StateCallbackKK {
        private final MethodNameInvoker<CameraDeviceImpl.StateCallbackKK> mProxy;

        public DeviceStateCallbackProxy(Dispatchable<CameraDeviceImpl.StateCallbackKK> dispatchTarget) {
            this.mProxy = new MethodNameInvoker<>((Dispatchable) Preconditions.checkNotNull(dispatchTarget, "dispatchTarget must not be null"), CameraDeviceImpl.StateCallbackKK.class);
        }

        @Override
        public void onOpened(CameraDevice camera) throws Throwable {
            this.mProxy.invoke("onOpened", camera);
        }

        @Override
        public void onDisconnected(CameraDevice camera) throws Throwable {
            this.mProxy.invoke("onDisconnected", camera);
        }

        @Override
        public void onError(CameraDevice camera, int error) throws Throwable {
            this.mProxy.invoke("onError", camera, Integer.valueOf(error));
        }

        @Override
        public void onUnconfigured(CameraDevice camera) throws Throwable {
            this.mProxy.invoke("onUnconfigured", camera);
        }

        @Override
        public void onActive(CameraDevice camera) throws Throwable {
            this.mProxy.invoke("onActive", camera);
        }

        @Override
        public void onBusy(CameraDevice camera) throws Throwable {
            this.mProxy.invoke("onBusy", camera);
        }

        @Override
        public void onClosed(CameraDevice camera) throws Throwable {
            this.mProxy.invoke("onClosed", camera);
        }

        @Override
        public void onIdle(CameraDevice camera) throws Throwable {
            this.mProxy.invoke("onIdle", camera);
        }
    }

    public static class DeviceCaptureCallbackProxy extends CameraDeviceImpl.CaptureCallback {
        private final MethodNameInvoker<CameraDeviceImpl.CaptureCallback> mProxy;

        public DeviceCaptureCallbackProxy(Dispatchable<CameraDeviceImpl.CaptureCallback> dispatchTarget) {
            this.mProxy = new MethodNameInvoker<>((Dispatchable) Preconditions.checkNotNull(dispatchTarget, "dispatchTarget must not be null"), CameraDeviceImpl.CaptureCallback.class);
        }

        @Override
        public void onCaptureStarted(CameraDevice camera, CaptureRequest request, long timestamp, long frameNumber) throws Throwable {
            this.mProxy.invoke("onCaptureStarted", camera, request, Long.valueOf(timestamp), Long.valueOf(frameNumber));
        }

        @Override
        public void onCapturePartial(CameraDevice camera, CaptureRequest request, CaptureResult result) throws Throwable {
            this.mProxy.invoke("onCapturePartial", camera, request, result);
        }

        @Override
        public void onCaptureProgressed(CameraDevice camera, CaptureRequest request, CaptureResult partialResult) throws Throwable {
            this.mProxy.invoke("onCaptureProgressed", camera, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraDevice camera, CaptureRequest request, TotalCaptureResult result) throws Throwable {
            this.mProxy.invoke("onCaptureCompleted", camera, request, result);
        }

        @Override
        public void onCaptureFailed(CameraDevice camera, CaptureRequest request, CaptureFailure failure) throws Throwable {
            this.mProxy.invoke("onCaptureFailed", camera, request, failure);
        }

        @Override
        public void onCaptureSequenceCompleted(CameraDevice camera, int sequenceId, long frameNumber) throws Throwable {
            this.mProxy.invoke("onCaptureSequenceCompleted", camera, Integer.valueOf(sequenceId), Long.valueOf(frameNumber));
        }

        @Override
        public void onCaptureSequenceAborted(CameraDevice camera, int sequenceId) throws Throwable {
            this.mProxy.invoke("onCaptureSequenceAborted", camera, Integer.valueOf(sequenceId));
        }
    }

    public static class SessionStateCallbackProxy extends CameraCaptureSession.StateCallback {
        private final MethodNameInvoker<CameraCaptureSession.StateCallback> mProxy;

        public SessionStateCallbackProxy(Dispatchable<CameraCaptureSession.StateCallback> dispatchTarget) {
            this.mProxy = new MethodNameInvoker<>((Dispatchable) Preconditions.checkNotNull(dispatchTarget, "dispatchTarget must not be null"), CameraCaptureSession.StateCallback.class);
        }

        @Override
        public void onConfigured(CameraCaptureSession session) throws Throwable {
            this.mProxy.invoke("onConfigured", session);
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) throws Throwable {
            this.mProxy.invoke("onConfigureFailed", session);
        }

        @Override
        public void onReady(CameraCaptureSession session) throws Throwable {
            this.mProxy.invoke("onReady", session);
        }

        @Override
        public void onActive(CameraCaptureSession session) throws Throwable {
            this.mProxy.invoke("onActive", session);
        }

        @Override
        public void onClosed(CameraCaptureSession session) throws Throwable {
            this.mProxy.invoke("onClosed", session);
        }
    }

    private CallbackProxies() {
        throw new AssertionError();
    }
}
