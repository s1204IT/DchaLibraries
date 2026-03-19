package android.hardware.camera2.impl;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.impl.CameraDeviceImpl;
import android.hardware.camera2.params.OutputConfiguration;
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
import java.util.concurrent.Executor;

public class CameraConstrainedHighSpeedCaptureSessionImpl extends CameraConstrainedHighSpeedCaptureSession implements CameraCaptureSessionCore {
    private final CameraCharacteristics mCharacteristics;
    private final CameraCaptureSessionImpl mSessionImpl;

    CameraConstrainedHighSpeedCaptureSessionImpl(int i, CameraCaptureSession.StateCallback stateCallback, Executor executor, CameraDeviceImpl cameraDeviceImpl, Executor executor2, boolean z, CameraCharacteristics cameraCharacteristics) {
        this.mCharacteristics = cameraCharacteristics;
        this.mSessionImpl = new CameraCaptureSessionImpl(i, null, new WrapperCallback(stateCallback), executor, cameraDeviceImpl, executor2, z);
    }

    @Override
    public List<CaptureRequest> createHighSpeedRequestList(CaptureRequest captureRequest) throws CameraAccessException {
        CaptureRequest.Builder builder;
        if (captureRequest == null) {
            throw new IllegalArgumentException("Input capture request must not be null");
        }
        Collection<Surface> targets = captureRequest.getTargets();
        Range range = (Range) captureRequest.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
        SurfaceUtils.checkConstrainedHighSpeedSurfaces(targets, range, (StreamConfigurationMap) this.mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP));
        int iIntValue = ((Integer) range.getUpper()).intValue() / 30;
        ArrayList arrayList = new ArrayList();
        CaptureRequest.Builder builder2 = new CaptureRequest.Builder(new CameraMetadataNative(captureRequest.getNativeCopy()), false, -1, captureRequest.getLogicalCameraId(), null);
        builder2.setTag(captureRequest.getTag());
        Iterator<Surface> it = targets.iterator();
        Surface next = it.next();
        if (targets.size() == 1 && SurfaceUtils.isSurfaceForHwVideoEncoder(next)) {
            builder2.set(CaptureRequest.CONTROL_CAPTURE_INTENT, 1);
        } else {
            builder2.set(CaptureRequest.CONTROL_CAPTURE_INTENT, 3);
        }
        builder2.setPartOfCHSRequestList(true);
        if (targets.size() == 2) {
            builder = new CaptureRequest.Builder(new CameraMetadataNative(captureRequest.getNativeCopy()), false, -1, captureRequest.getLogicalCameraId(), null);
            builder.setTag(captureRequest.getTag());
            builder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, 3);
            builder.addTarget(next);
            Surface next2 = it.next();
            builder.addTarget(next2);
            builder.setPartOfCHSRequestList(true);
            if (SurfaceUtils.isSurfaceForHwVideoEncoder(next)) {
                next2 = next;
            }
            builder2.addTarget(next2);
        } else {
            builder2.addTarget(next);
            builder = null;
        }
        for (int i = 0; i < iIntValue; i++) {
            if (i == 0 && builder != null) {
                arrayList.add(builder.build());
            } else {
                arrayList.add(builder2.build());
            }
        }
        return Collections.unmodifiableList(arrayList);
    }

    private boolean isConstrainedHighSpeedRequestList(List<CaptureRequest> list) {
        Preconditions.checkCollectionNotEmpty(list, "High speed request list");
        Iterator<CaptureRequest> it = list.iterator();
        while (it.hasNext()) {
            if (!it.next().isPartOfCRequestList()) {
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
    public void prepare(int i, Surface surface) throws CameraAccessException {
        this.mSessionImpl.prepare(i, surface);
    }

    @Override
    public void tearDown(Surface surface) throws CameraAccessException {
        this.mSessionImpl.tearDown(surface);
    }

    @Override
    public int capture(CaptureRequest captureRequest, CameraCaptureSession.CaptureCallback captureCallback, Handler handler) throws CameraAccessException {
        throw new UnsupportedOperationException("Constrained high speed session doesn't support this method");
    }

    @Override
    public int captureSingleRequest(CaptureRequest captureRequest, Executor executor, CameraCaptureSession.CaptureCallback captureCallback) throws CameraAccessException {
        throw new UnsupportedOperationException("Constrained high speed session doesn't support this method");
    }

    @Override
    public int captureBurst(List<CaptureRequest> list, CameraCaptureSession.CaptureCallback captureCallback, Handler handler) throws CameraAccessException {
        if (!isConstrainedHighSpeedRequestList(list)) {
            throw new IllegalArgumentException("Only request lists created by createHighSpeedRequestList() can be submitted to a constrained high speed capture session");
        }
        return this.mSessionImpl.captureBurst(list, captureCallback, handler);
    }

    @Override
    public int captureBurstRequests(List<CaptureRequest> list, Executor executor, CameraCaptureSession.CaptureCallback captureCallback) throws CameraAccessException {
        if (!isConstrainedHighSpeedRequestList(list)) {
            throw new IllegalArgumentException("Only request lists created by createHighSpeedRequestList() can be submitted to a constrained high speed capture session");
        }
        return this.mSessionImpl.captureBurstRequests(list, executor, captureCallback);
    }

    @Override
    public int setRepeatingRequest(CaptureRequest captureRequest, CameraCaptureSession.CaptureCallback captureCallback, Handler handler) throws CameraAccessException {
        throw new UnsupportedOperationException("Constrained high speed session doesn't support this method");
    }

    @Override
    public int setSingleRepeatingRequest(CaptureRequest captureRequest, Executor executor, CameraCaptureSession.CaptureCallback captureCallback) throws CameraAccessException {
        throw new UnsupportedOperationException("Constrained high speed session doesn't support this method");
    }

    @Override
    public int setRepeatingBurst(List<CaptureRequest> list, CameraCaptureSession.CaptureCallback captureCallback, Handler handler) throws CameraAccessException {
        if (!isConstrainedHighSpeedRequestList(list)) {
            throw new IllegalArgumentException("Only request lists created by createHighSpeedRequestList() can be submitted to a constrained high speed capture session");
        }
        return this.mSessionImpl.setRepeatingBurst(list, captureCallback, handler);
    }

    @Override
    public int setRepeatingBurstRequests(List<CaptureRequest> list, Executor executor, CameraCaptureSession.CaptureCallback captureCallback) throws CameraAccessException {
        if (!isConstrainedHighSpeedRequestList(list)) {
            throw new IllegalArgumentException("Only request lists created by createHighSpeedRequestList() can be submitted to a constrained high speed capture session");
        }
        return this.mSessionImpl.setRepeatingBurstRequests(list, executor, captureCallback);
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
    public void updateOutputConfiguration(OutputConfiguration outputConfiguration) throws CameraAccessException {
        throw new UnsupportedOperationException("Constrained high speed session doesn't support this method");
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

    @Override
    public void finalizeOutputConfigurations(List<OutputConfiguration> list) throws CameraAccessException {
        this.mSessionImpl.finalizeOutputConfigurations(list);
    }

    private class WrapperCallback extends CameraCaptureSession.StateCallback {
        private final CameraCaptureSession.StateCallback mCallback;

        public WrapperCallback(CameraCaptureSession.StateCallback stateCallback) {
            this.mCallback = stateCallback;
        }

        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            this.mCallback.onConfigured(CameraConstrainedHighSpeedCaptureSessionImpl.this);
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            this.mCallback.onConfigureFailed(CameraConstrainedHighSpeedCaptureSessionImpl.this);
        }

        @Override
        public void onReady(CameraCaptureSession cameraCaptureSession) {
            this.mCallback.onReady(CameraConstrainedHighSpeedCaptureSessionImpl.this);
        }

        @Override
        public void onActive(CameraCaptureSession cameraCaptureSession) {
            this.mCallback.onActive(CameraConstrainedHighSpeedCaptureSessionImpl.this);
        }

        @Override
        public void onCaptureQueueEmpty(CameraCaptureSession cameraCaptureSession) {
            this.mCallback.onCaptureQueueEmpty(CameraConstrainedHighSpeedCaptureSessionImpl.this);
        }

        @Override
        public void onClosed(CameraCaptureSession cameraCaptureSession) {
            this.mCallback.onClosed(CameraConstrainedHighSpeedCaptureSessionImpl.this);
        }

        @Override
        public void onSurfacePrepared(CameraCaptureSession cameraCaptureSession, Surface surface) {
            this.mCallback.onSurfacePrepared(CameraConstrainedHighSpeedCaptureSessionImpl.this, surface);
        }
    }
}
