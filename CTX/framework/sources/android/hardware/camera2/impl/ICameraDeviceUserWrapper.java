package android.hardware.camera2.impl;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.ICameraDeviceUser;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.utils.SubmitInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Surface;

public class ICameraDeviceUserWrapper {
    private final ICameraDeviceUser mRemoteDevice;

    public ICameraDeviceUserWrapper(ICameraDeviceUser iCameraDeviceUser) {
        if (iCameraDeviceUser == null) {
            throw new NullPointerException("Remote device may not be null");
        }
        this.mRemoteDevice = iCameraDeviceUser;
    }

    public void unlinkToDeath(IBinder.DeathRecipient deathRecipient, int i) {
        if (this.mRemoteDevice.asBinder() != null) {
            this.mRemoteDevice.asBinder().unlinkToDeath(deathRecipient, i);
        }
    }

    public void disconnect() {
        try {
            this.mRemoteDevice.disconnect();
        } catch (RemoteException e) {
        }
    }

    public SubmitInfo submitRequest(CaptureRequest captureRequest, boolean z) throws CameraAccessException {
        try {
            return this.mRemoteDevice.submitRequest(captureRequest, z);
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public SubmitInfo submitRequestList(CaptureRequest[] captureRequestArr, boolean z) throws CameraAccessException {
        try {
            return this.mRemoteDevice.submitRequestList(captureRequestArr, z);
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public long cancelRequest(int i) throws CameraAccessException {
        try {
            return this.mRemoteDevice.cancelRequest(i);
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public void beginConfigure() throws CameraAccessException {
        try {
            this.mRemoteDevice.beginConfigure();
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public void endConfigure(int i, CameraMetadataNative cameraMetadataNative) throws CameraAccessException {
        try {
            ICameraDeviceUser iCameraDeviceUser = this.mRemoteDevice;
            if (cameraMetadataNative == null) {
                cameraMetadataNative = new CameraMetadataNative();
            }
            iCameraDeviceUser.endConfigure(i, cameraMetadataNative);
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public void deleteStream(int i) throws CameraAccessException {
        try {
            this.mRemoteDevice.deleteStream(i);
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public int createStream(OutputConfiguration outputConfiguration) throws CameraAccessException {
        try {
            return this.mRemoteDevice.createStream(outputConfiguration);
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public int createInputStream(int i, int i2, int i3) throws CameraAccessException {
        try {
            return this.mRemoteDevice.createInputStream(i, i2, i3);
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public Surface getInputSurface() throws CameraAccessException {
        try {
            return this.mRemoteDevice.getInputSurface();
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public CameraMetadataNative createDefaultRequest(int i) throws CameraAccessException {
        try {
            return this.mRemoteDevice.createDefaultRequest(i);
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public CameraMetadataNative getCameraInfo() throws CameraAccessException {
        try {
            return this.mRemoteDevice.getCameraInfo();
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public void waitUntilIdle() throws CameraAccessException {
        try {
            this.mRemoteDevice.waitUntilIdle();
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public long flush() throws CameraAccessException {
        try {
            return this.mRemoteDevice.flush();
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public void prepare(int i) throws CameraAccessException {
        try {
            this.mRemoteDevice.prepare(i);
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public void tearDown(int i) throws CameraAccessException {
        try {
            this.mRemoteDevice.tearDown(i);
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public void prepare2(int i, int i2) throws CameraAccessException {
        try {
            this.mRemoteDevice.prepare2(i, i2);
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public void updateOutputConfiguration(int i, OutputConfiguration outputConfiguration) throws CameraAccessException {
        try {
            this.mRemoteDevice.updateOutputConfiguration(i, outputConfiguration);
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }

    public void finalizeOutputConfigurations(int i, OutputConfiguration outputConfiguration) throws CameraAccessException {
        try {
            this.mRemoteDevice.finalizeOutputConfigurations(i, outputConfiguration);
        } catch (Throwable th) {
            CameraManager.throwAsPublicException(th);
            throw new UnsupportedOperationException("Unexpected exception", th);
        }
    }
}
