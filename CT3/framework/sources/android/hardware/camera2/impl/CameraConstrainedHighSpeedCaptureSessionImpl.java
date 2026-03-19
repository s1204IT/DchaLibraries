package android.hardware.camera2.impl;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.impl.CameraDeviceImpl;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.utils.SurfaceUtils;
import android.os.Handler;
import android.util.Range;
import android.view.Surface;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CameraConstrainedHighSpeedCaptureSessionImpl extends CameraConstrainedHighSpeedCaptureSession implements CameraCaptureSessionCore {
    private final CameraCharacteristics mCharacteristics;
    private final CameraCaptureSessionImpl mSessionImpl;

    CameraConstrainedHighSpeedCaptureSessionImpl(int id, List<Surface> outputs, CameraCaptureSession.StateCallback callback, Handler stateHandler, CameraDeviceImpl deviceImpl, Handler deviceStateHandler, boolean configureSuccess, CameraCharacteristics characteristics) {
        this.mCharacteristics = characteristics;
        CameraCaptureSession.StateCallback wrapperCallback = new WrapperCallback(callback);
        this.mSessionImpl = new CameraCaptureSessionImpl(id, null, outputs, wrapperCallback, stateHandler, deviceImpl, deviceStateHandler, configureSuccess);
    }

    @Override
    public List<CaptureRequest> createHighSpeedRequestList(CaptureRequest request) throws CameraAccessException {
        if (request == null) {
            throw new IllegalArgumentException("Input capture request must not be null");
        }
        Collection<Surface> outputSurfaces = request.getTargets();
        Range<Integer> fpsRange = (Range) request.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
        StreamConfigurationMap config = (StreamConfigurationMap) this.mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        SurfaceUtils.checkConstrainedHighSpeedSurfaces(outputSurfaces, fpsRange, config);
        int requestListSize = ((Integer) fpsRange.getUpper()).intValue() / 30;
        List<CaptureRequest> requestList = new ArrayList<>();
        CameraMetadataNative requestMetadata = new CameraMetadataNative(request.getNativeCopy());
        CaptureRequest.Builder singleTargetRequestBuilder = new CaptureRequest.Builder(requestMetadata, false, -1);
        Iterator<Surface> iterator = outputSurfaces.iterator();
        Surface firstSurface = iterator.next();
        if (outputSurfaces.size() == 1 && SurfaceUtils.isSurfaceForHwVideoEncoder(firstSurface)) {
            singleTargetRequestBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, 1);
        } else {
            singleTargetRequestBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, 3);
        }
        singleTargetRequestBuilder.setPartOfCHSRequestList(true);
        CaptureRequest.Builder doubleTargetRequestBuilder = null;
        if (outputSurfaces.size() == 2) {
            CameraMetadataNative requestMetadata2 = new CameraMetadataNative(request.getNativeCopy());
            doubleTargetRequestBuilder = new CaptureRequest.Builder(requestMetadata2, false, -1);
            doubleTargetRequestBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, 3);
            doubleTargetRequestBuilder.addTarget(firstSurface);
            Surface secondSurface = iterator.next();
            doubleTargetRequestBuilder.addTarget(secondSurface);
            doubleTargetRequestBuilder.setPartOfCHSRequestList(true);
            Surface recordingSurface = firstSurface;
            if (!SurfaceUtils.isSurfaceForHwVideoEncoder(firstSurface)) {
                recordingSurface = secondSurface;
            }
            singleTargetRequestBuilder.addTarget(recordingSurface);
        } else {
            singleTargetRequestBuilder.addTarget(firstSurface);
        }
        for (int i = 0; i < requestListSize; i++) {
            if (i == 0 && doubleTargetRequestBuilder != null) {
                requestList.add(doubleTargetRequestBuilder.build());
            } else {
                requestList.add(singleTargetRequestBuilder.build());
            }
        }
        return Collections.unmodifiableList(requestList);
    }

    private boolean isConstrainedHighSpeedRequestList(List<CaptureRequest> requestList) {
        Preconditions.checkCollectionNotEmpty(requestList, "High speed request list");
        for (CaptureRequest request : requestList) {
            if (!request.isPartOfCRequestList()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public CameraDevice getDevice() {
        return this.mSessionImpl.getDevice();
    }

    @Override
    public void prepare(Surface surface) throws CameraAccessException {
        this.mSessionImpl.prepare(surface);
    }

    @Override
    public void prepare(int maxCount, Surface surface) throws CameraAccessException {
        this.mSessionImpl.prepare(maxCount, surface);
    }

    @Override
    public void tearDown(Surface surface) throws CameraAccessException {
        this.mSessionImpl.tearDown(surface);
    }

    @Override
    public int capture(CaptureRequest request, CameraCaptureSession.CaptureCallback listener, Handler handler) throws CameraAccessException {
        throw new UnsupportedOperationException("Constrained high speed session doesn't support this method");
    }

    @Override
    public int captureBurst(List<CaptureRequest> requests, CameraCaptureSession.CaptureCallback listener, Handler handler) throws CameraAccessException {
        if (!isConstrainedHighSpeedRequestList(requests)) {
            throw new IllegalArgumentException("Only request lists created by createHighSpeedRequestList() can be submitted to a constrained high speed capture session");
        }
        return this.mSessionImpl.captureBurst(requests, listener, handler);
    }

    @Override
    public int setRepeatingRequest(CaptureRequest request, CameraCaptureSession.CaptureCallback listener, Handler handler) throws CameraAccessException {
        throw new UnsupportedOperationException("Constrained high speed session doesn't support this method");
    }

    @Override
    public int setRepeatingBurst(List<CaptureRequest> requests, CameraCaptureSession.CaptureCallback listener, Handler handler) throws CameraAccessException {
        if (!isConstrainedHighSpeedRequestList(requests)) {
            throw new IllegalArgumentException("Only request lists created by createHighSpeedRequestList() can be submitted to a constrained high speed capture session");
        }
        return this.mSessionImpl.setRepeatingBurst(requests, listener, handler);
    }

    @Override
    public void stopRepeating() throws CameraAccessException {
        this.mSessionImpl.stopRepeating();
    }

    @Override
    public void abortCaptures() throws CameraAccessException {
        this.mSessionImpl.abortCaptures();
    }

    @Override
    public Surface getInputSurface() {
        return null;
    }

    @Override
    public void close() {
        this.mSessionImpl.close();
    }

    @Override
    public boolean isReprocessable() {
        return false;
    }

    @Override
    public void replaceSessionClose() {
        this.mSessionImpl.replaceSessionClose();
    }

    @Override
    public CameraDeviceImpl.StateCallbackKK getDeviceStateCallback() {
        return this.mSessionImpl.getDeviceStateCallback();
    }

    @Override
    public boolean isAborting() {
        return this.mSessionImpl.isAborting();
    }

    private class WrapperCallback extends CameraCaptureSession.StateCallback {
        private final CameraCaptureSession.StateCallback mCallback;

        public WrapperCallback(CameraCaptureSession.StateCallback callback) {
            this.mCallback = callback;
        }

        @Override
        public void onConfigured(CameraCaptureSession session) {
            this.mCallback.onConfigured(CameraConstrainedHighSpeedCaptureSessionImpl.this);
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            this.mCallback.onConfigureFailed(CameraConstrainedHighSpeedCaptureSessionImpl.this);
        }

        @Override
        public void onReady(CameraCaptureSession session) {
            this.mCallback.onReady(CameraConstrainedHighSpeedCaptureSessionImpl.this);
        }

        @Override
        public void onActive(CameraCaptureSession session) {
            this.mCallback.onActive(CameraConstrainedHighSpeedCaptureSessionImpl.this);
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            this.mCallback.onClosed(CameraConstrainedHighSpeedCaptureSessionImpl.this);
        }

        @Override
        public void onSurfacePrepared(CameraCaptureSession session, Surface surface) {
            this.mCallback.onSurfacePrepared(CameraConstrainedHighSpeedCaptureSessionImpl.this, surface);
        }
    }
}
