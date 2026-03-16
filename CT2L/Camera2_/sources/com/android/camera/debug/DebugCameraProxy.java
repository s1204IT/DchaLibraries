package com.android.camera.debug;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.view.SurfaceHolder;
import com.android.camera.debug.Log;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.CameraStateHolder;
import com.android.ex.camera2.portability.DispatchThread;

public class DebugCameraProxy extends CameraAgent.CameraProxy {
    private final CameraAgent.CameraProxy mProxy;
    private final Log.Tag mTag;

    public DebugCameraProxy(Log.Tag tag, CameraAgent.CameraProxy proxy) {
        this.mTag = tag;
        this.mProxy = proxy;
    }

    @Override
    public Camera getCamera() {
        log("getCamera");
        return this.mProxy.getCamera();
    }

    @Override
    public int getCameraId() {
        log("getCameraId: " + this.mProxy.getCameraId());
        return this.mProxy.getCameraId();
    }

    @Override
    public CameraDeviceInfo.Characteristics getCharacteristics() {
        log("getCharacteristics");
        return this.mProxy.getCharacteristics();
    }

    @Override
    public CameraAgent getAgent() {
        log("getAgent");
        return this.mProxy.getAgent();
    }

    @Override
    public CameraCapabilities getCapabilities() {
        log("getCapabilities");
        return this.mProxy.getCapabilities();
    }

    @Override
    public CameraCapabilities updateCapabilities() {
        log("updateCapabilities");
        return this.mProxy.updateCapabilities();
    }

    @Override
    public void reconnect(Handler handler, CameraAgent.CameraOpenCallback cb) {
        log("reconnect");
        this.mProxy.reconnect(handler, cb);
    }

    @Override
    public void unlock() {
        log("unlock");
        this.mProxy.unlock();
    }

    @Override
    public void lock() {
        log("lock");
        this.mProxy.lock();
    }

    @Override
    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        log("setPreviewTexture");
        this.mProxy.setPreviewTexture(surfaceTexture);
    }

    @Override
    public void setPreviewTextureSync(SurfaceTexture surfaceTexture) {
        log("setPreviewTextureSync");
        this.mProxy.setPreviewTextureSync(surfaceTexture);
    }

    @Override
    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        log("setPreviewDisplay");
        this.mProxy.setPreviewDisplay(surfaceHolder);
    }

    @Override
    public void startPreview() {
        log("startPreview");
        this.mProxy.startPreview();
    }

    @Override
    public void startPreviewWithCallback(Handler h, CameraAgent.CameraStartPreviewCallback cb) {
        log("startPreviewWithCallback");
        this.mProxy.startPreviewWithCallback(h, cb);
    }

    @Override
    public void stopPreview() {
        log("stopPreview");
        this.mProxy.stopPreview();
    }

    @Override
    public void setPreviewDataCallback(Handler handler, CameraAgent.CameraPreviewDataCallback cb) {
        log("setPreviewDataCallback");
        this.mProxy.setPreviewDataCallback(handler, cb);
    }

    @Override
    public void setOneShotPreviewCallback(Handler handler, CameraAgent.CameraPreviewDataCallback cb) {
        log("setOneShotPreviewCallback");
        this.mProxy.setOneShotPreviewCallback(handler, cb);
    }

    @Override
    public void setPreviewDataCallbackWithBuffer(Handler handler, CameraAgent.CameraPreviewDataCallback cb) {
        log("setPreviewDataCallbackWithBuffer");
        this.mProxy.setPreviewDataCallbackWithBuffer(handler, cb);
    }

    @Override
    public void addCallbackBuffer(byte[] callbackBuffer) {
        log("addCallbackBuffer");
        this.mProxy.addCallbackBuffer(callbackBuffer);
    }

    @Override
    public void autoFocus(Handler handler, CameraAgent.CameraAFCallback cb) {
        log("autoFocus");
        this.mProxy.autoFocus(handler, cb);
    }

    @Override
    public void cancelAutoFocus() {
        log("cancelAutoFocus");
        this.mProxy.cancelAutoFocus();
    }

    @Override
    public void setAutoFocusMoveCallback(Handler handler, CameraAgent.CameraAFMoveCallback cb) {
        log("setAutoFocusMoveCallback");
        this.mProxy.setAutoFocusMoveCallback(handler, cb);
    }

    @Override
    public void takePicture(Handler handler, CameraAgent.CameraShutterCallback shutter, CameraAgent.CameraPictureCallback raw, CameraAgent.CameraPictureCallback postview, CameraAgent.CameraPictureCallback jpeg) {
        log("takePicture");
        this.mProxy.takePicture(handler, shutter, raw, postview, jpeg);
    }

    @Override
    public void setDisplayOrientation(int degrees) {
        log("setDisplayOrientation:" + degrees);
        this.mProxy.setDisplayOrientation(degrees);
    }

    @Override
    public void setZoomChangeListener(Camera.OnZoomChangeListener listener) {
        log("setZoomChangeListener");
        this.mProxy.setZoomChangeListener(listener);
    }

    @Override
    public void setFaceDetectionCallback(Handler handler, CameraAgent.CameraFaceDetectionCallback callback) {
        log("setFaceDetectionCallback");
        this.mProxy.setFaceDetectionCallback(handler, callback);
    }

    @Override
    public void startFaceDetection() {
        log("startFaceDetection");
        this.mProxy.startFaceDetection();
    }

    @Override
    public void stopFaceDetection() {
        log("stopFaceDetection");
        this.mProxy.stopFaceDetection();
    }

    @Override
    public void setParameters(Camera.Parameters params) {
        log("setParameters");
        this.mProxy.setParameters(params);
    }

    @Override
    public Camera.Parameters getParameters() {
        log("getParameters");
        return this.mProxy.getParameters();
    }

    @Override
    public CameraSettings getSettings() {
        log("getSettings");
        return this.mProxy.getSettings();
    }

    @Override
    public boolean applySettings(CameraSettings settings) {
        log("applySettings");
        return this.mProxy.applySettings(settings);
    }

    @Override
    public void refreshSettings() {
        log("refreshParameters");
        this.mProxy.refreshSettings();
    }

    @Override
    public void enableShutterSound(boolean enable) {
        log("enableShutterSound:" + enable);
        this.mProxy.enableShutterSound(enable);
    }

    @Override
    public String dumpDeviceSettings() {
        log("dumpDeviceSettings");
        return this.mProxy.dumpDeviceSettings();
    }

    @Override
    public Handler getCameraHandler() {
        return this.mProxy.getCameraHandler();
    }

    @Override
    public DispatchThread getDispatchThread() {
        return this.mProxy.getDispatchThread();
    }

    @Override
    public CameraStateHolder getCameraState() {
        return this.mProxy.getCameraState();
    }

    private void log(String msg) {
        Log.v(this.mTag, msg);
    }
}
