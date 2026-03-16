package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class FlashlightController {
    private static final boolean DEBUG = Log.isLoggable("FlashlightController", 3);
    private boolean mCameraAvailable;
    private CameraDevice mCameraDevice;
    private String mCameraId;
    private final CameraManager mCameraManager;
    private boolean mFlashlightEnabled;
    private CaptureRequest mFlashlightRequest;
    private Handler mHandler;
    private CameraCaptureSession mSession;
    private Surface mSurface;
    private SurfaceTexture mSurfaceTexture;
    private final ArrayList<WeakReference<FlashlightListener>> mListeners = new ArrayList<>(1);
    private final CameraDevice.StateListener mCameraListener = new CameraDevice.StateListener() {
        public void onOpened(CameraDevice camera) {
            FlashlightController.this.mCameraDevice = camera;
            FlashlightController.this.postUpdateFlashlight();
        }

        public void onDisconnected(CameraDevice camera) {
            if (FlashlightController.this.mCameraDevice == camera) {
                FlashlightController.this.dispatchOff();
                FlashlightController.this.teardown();
            }
        }

        public void onError(CameraDevice camera, int error) {
            Log.e("FlashlightController", "Camera error: camera=" + camera + " error=" + error);
            if (camera == FlashlightController.this.mCameraDevice || FlashlightController.this.mCameraDevice == null) {
                FlashlightController.this.handleError();
            }
        }
    };
    private final CameraCaptureSession.StateListener mSessionListener = new CameraCaptureSession.StateListener() {
        public void onConfigured(CameraCaptureSession session) {
            if (session.getDevice() == FlashlightController.this.mCameraDevice) {
                FlashlightController.this.mSession = session;
            } else {
                session.close();
            }
            FlashlightController.this.postUpdateFlashlight();
        }

        public void onConfigureFailed(CameraCaptureSession session) {
            Log.e("FlashlightController", "Configure failed.");
            if (FlashlightController.this.mSession == null || FlashlightController.this.mSession == session) {
                FlashlightController.this.handleError();
            }
        }
    };
    private final Runnable mUpdateFlashlightRunnable = new Runnable() {
        @Override
        public void run() {
            FlashlightController.this.updateFlashlight(false);
        }
    };
    private final Runnable mKillFlashlightRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
                FlashlightController.this.mFlashlightEnabled = false;
            }
            FlashlightController.this.updateFlashlight(true);
            FlashlightController.this.dispatchOff();
        }
    };
    private final CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(String cameraId) {
            if (FlashlightController.DEBUG) {
                Log.d("FlashlightController", "onCameraAvailable(" + cameraId + ")");
            }
            if (cameraId.equals(FlashlightController.this.mCameraId)) {
                setCameraAvailable(true);
            }
        }

        @Override
        public void onCameraUnavailable(String cameraId) {
            if (FlashlightController.DEBUG) {
                Log.d("FlashlightController", "onCameraUnavailable(" + cameraId + ")");
            }
            if (cameraId.equals(FlashlightController.this.mCameraId)) {
                setCameraAvailable(false);
            }
        }

        private void setCameraAvailable(boolean available) {
            boolean changed;
            synchronized (FlashlightController.this) {
                changed = FlashlightController.this.mCameraAvailable != available;
                FlashlightController.this.mCameraAvailable = available;
            }
            if (changed) {
                if (FlashlightController.DEBUG) {
                    Log.d("FlashlightController", "dispatchAvailabilityChanged(" + available + ")");
                }
                FlashlightController.this.dispatchAvailabilityChanged(available);
            }
        }
    };

    public interface FlashlightListener {
        void onFlashlightAvailabilityChanged(boolean z);

        void onFlashlightError();

        void onFlashlightOff();
    }

    public FlashlightController(Context mContext) {
        this.mCameraManager = (CameraManager) mContext.getSystemService("camera");
        initialize();
    }

    public void initialize() {
        try {
            this.mCameraId = getCameraId();
            if (this.mCameraId != null) {
                ensureHandler();
                this.mCameraManager.registerAvailabilityCallback(this.mAvailabilityCallback, this.mHandler);
            }
        } catch (Throwable e) {
            Log.e("FlashlightController", "Couldn't initialize.", e);
        }
    }

    public synchronized void setFlashlight(boolean enabled) {
        if (this.mFlashlightEnabled != enabled) {
            this.mFlashlightEnabled = enabled;
            postUpdateFlashlight();
        }
    }

    public void killFlashlight() {
        boolean enabled;
        synchronized (this) {
            enabled = this.mFlashlightEnabled;
        }
        if (enabled) {
            this.mHandler.post(this.mKillFlashlightRunnable);
        }
    }

    public synchronized boolean isAvailable() {
        return this.mCameraAvailable;
    }

    public void addListener(FlashlightListener l) {
        synchronized (this.mListeners) {
            cleanUpListenersLocked(l);
            this.mListeners.add(new WeakReference<>(l));
        }
    }

    public void removeListener(FlashlightListener l) {
        synchronized (this.mListeners) {
            cleanUpListenersLocked(l);
        }
    }

    private synchronized void ensureHandler() {
        if (this.mHandler == null) {
            HandlerThread thread = new HandlerThread("FlashlightController", 10);
            thread.start();
            this.mHandler = new Handler(thread.getLooper());
        }
    }

    private void startDevice() throws CameraAccessException {
        this.mCameraManager.openCamera(getCameraId(), (CameraDevice.StateCallback) this.mCameraListener, this.mHandler);
    }

    private void startSession() throws CameraAccessException {
        this.mSurfaceTexture = new SurfaceTexture(false);
        Size size = getSmallestSize(this.mCameraDevice.getId());
        this.mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
        this.mSurface = new Surface(this.mSurfaceTexture);
        ArrayList<Surface> outputs = new ArrayList<>(1);
        outputs.add(this.mSurface);
        this.mCameraDevice.createCaptureSession(outputs, this.mSessionListener, this.mHandler);
    }

    private Size getSmallestSize(String cameraId) throws CameraAccessException {
        Size[] outputSizes = ((StreamConfigurationMap) this.mCameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(SurfaceTexture.class);
        if (outputSizes == null || outputSizes.length == 0) {
            throw new IllegalStateException("Camera " + cameraId + "doesn't support any outputSize.");
        }
        Size chosen = outputSizes[0];
        for (Size s : outputSizes) {
            if (chosen.getWidth() >= s.getWidth() && chosen.getHeight() >= s.getHeight()) {
                chosen = s;
            }
        }
        return chosen;
    }

    private void postUpdateFlashlight() {
        ensureHandler();
        this.mHandler.post(this.mUpdateFlashlightRunnable);
    }

    private String getCameraId() throws CameraAccessException {
        String[] ids = this.mCameraManager.getCameraIdList();
        for (String id : ids) {
            CameraCharacteristics c = this.mCameraManager.getCameraCharacteristics(id);
            Boolean flashAvailable = (Boolean) c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            Integer lensFacing = (Integer) c.get(CameraCharacteristics.LENS_FACING);
            if (flashAvailable != null && flashAvailable.booleanValue() && lensFacing != null && lensFacing.intValue() == 1) {
                return id;
            }
        }
        return null;
    }

    private void updateFlashlight(boolean forceDisable) {
        boolean enabled;
        try {
            synchronized (this) {
                enabled = this.mFlashlightEnabled && !forceDisable;
            }
            if (enabled) {
                if (this.mCameraDevice == null) {
                    startDevice();
                    return;
                }
                if (this.mSession == null) {
                    startSession();
                    return;
                }
                if (this.mFlashlightRequest == null) {
                    CaptureRequest.Builder builder = this.mCameraDevice.createCaptureRequest(1);
                    builder.set(CaptureRequest.FLASH_MODE, 2);
                    builder.addTarget(this.mSurface);
                    CaptureRequest request = builder.build();
                    this.mSession.capture(request, null, this.mHandler);
                    this.mFlashlightRequest = request;
                    return;
                }
                return;
            }
            if (this.mCameraDevice != null) {
                this.mCameraDevice.close();
                teardown();
            }
        } catch (CameraAccessException e) {
            e = e;
            Log.e("FlashlightController", "Error in updateFlashlight", e);
            handleError();
        } catch (IllegalStateException e2) {
            e = e2;
            Log.e("FlashlightController", "Error in updateFlashlight", e);
            handleError();
        } catch (UnsupportedOperationException e3) {
            e = e3;
            Log.e("FlashlightController", "Error in updateFlashlight", e);
            handleError();
        }
    }

    private void teardown() {
        this.mCameraDevice = null;
        this.mSession = null;
        this.mFlashlightRequest = null;
        if (this.mSurface != null) {
            this.mSurface.release();
            this.mSurfaceTexture.release();
        }
        this.mSurface = null;
        this.mSurfaceTexture = null;
    }

    private void handleError() {
        synchronized (this) {
            this.mFlashlightEnabled = false;
        }
        dispatchError();
        dispatchOff();
        updateFlashlight(true);
    }

    private void dispatchOff() {
        dispatchListeners(1, false);
    }

    private void dispatchError() {
        dispatchListeners(0, false);
    }

    private void dispatchAvailabilityChanged(boolean available) {
        dispatchListeners(2, available);
    }

    private void dispatchListeners(int message, boolean argument) {
        synchronized (this.mListeners) {
            int N = this.mListeners.size();
            boolean cleanup = false;
            for (int i = 0; i < N; i++) {
                FlashlightListener l = this.mListeners.get(i).get();
                if (l != null) {
                    if (message == 0) {
                        l.onFlashlightError();
                    } else if (message == 1) {
                        l.onFlashlightOff();
                    } else if (message == 2) {
                        l.onFlashlightAvailabilityChanged(argument);
                    }
                } else {
                    cleanup = true;
                }
            }
            if (cleanup) {
                cleanUpListenersLocked(null);
            }
        }
    }

    private void cleanUpListenersLocked(FlashlightListener listener) {
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            FlashlightListener found = this.mListeners.get(i).get();
            if (found == null || found == listener) {
                this.mListeners.remove(i);
            }
        }
    }
}
