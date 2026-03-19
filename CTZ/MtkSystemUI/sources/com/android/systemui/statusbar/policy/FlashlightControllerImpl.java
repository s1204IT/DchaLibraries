package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.statusbar.policy.FlashlightController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class FlashlightControllerImpl implements FlashlightController {
    private static final boolean DEBUG = Log.isLoggable("FlashlightController", 3);
    private String mCameraId;
    private final CameraManager mCameraManager;
    private final Context mContext;
    private boolean mFlashlightEnabled;
    private Handler mHandler;
    private boolean mTorchAvailable;
    private final ArrayList<WeakReference<FlashlightController.FlashlightListener>> mListeners = new ArrayList<>(1);
    private final CameraManager.TorchCallback mTorchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeUnavailable(String str) {
            if (TextUtils.equals(str, FlashlightControllerImpl.this.mCameraId)) {
                setCameraAvailable(false);
            }
        }

        @Override
        public void onTorchModeChanged(String str, boolean z) {
            if (TextUtils.equals(str, FlashlightControllerImpl.this.mCameraId)) {
                setCameraAvailable(true);
                setTorchMode(z);
            }
        }

        private void setCameraAvailable(boolean z) {
            boolean z2;
            synchronized (FlashlightControllerImpl.this) {
                z2 = FlashlightControllerImpl.this.mTorchAvailable != z;
                FlashlightControllerImpl.this.mTorchAvailable = z;
            }
            if (z2) {
                if (FlashlightControllerImpl.DEBUG) {
                    Log.d("FlashlightController", "dispatchAvailabilityChanged(" + z + ")");
                }
                FlashlightControllerImpl.this.dispatchAvailabilityChanged(z);
            }
        }

        private void setTorchMode(boolean z) {
            boolean z2;
            synchronized (FlashlightControllerImpl.this) {
                z2 = FlashlightControllerImpl.this.mFlashlightEnabled != z;
                FlashlightControllerImpl.this.mFlashlightEnabled = z;
            }
            if (z2) {
                if (FlashlightControllerImpl.DEBUG) {
                    Log.d("FlashlightController", "dispatchModeChanged(" + z + ")");
                }
                FlashlightControllerImpl.this.dispatchModeChanged(z);
            }
        }
    };

    public FlashlightControllerImpl(Context context) {
        this.mContext = context;
        this.mCameraManager = (CameraManager) this.mContext.getSystemService("camera");
        tryInitCamera();
    }

    private void tryInitCamera() {
        try {
            this.mCameraId = getCameraId();
            if (this.mCameraId != null) {
                ensureHandler();
                this.mCameraManager.registerTorchCallback(this.mTorchCallback, this.mHandler);
            }
        } catch (Throwable th) {
            Log.e("FlashlightController", "Couldn't initialize.", th);
        }
    }

    @Override
    public void setFlashlight(boolean z) {
        synchronized (this) {
            if (this.mCameraId == null) {
                return;
            }
            boolean z2 = false;
            if (this.mFlashlightEnabled != z) {
                this.mFlashlightEnabled = z;
                try {
                    this.mCameraManager.setTorchMode(this.mCameraId, z);
                } catch (CameraAccessException e) {
                    Log.e("FlashlightController", "Couldn't set torch mode", e);
                    this.mFlashlightEnabled = false;
                    z2 = true;
                }
            }
            dispatchModeChanged(this.mFlashlightEnabled);
            if (z2) {
                dispatchError();
            }
        }
    }

    @Override
    public boolean hasFlashlight() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.camera.flash");
    }

    @Override
    public synchronized boolean isEnabled() {
        return this.mFlashlightEnabled;
    }

    @Override
    public synchronized boolean isAvailable() {
        return this.mTorchAvailable;
    }

    @Override
    public void addCallback(FlashlightController.FlashlightListener flashlightListener) {
        synchronized (this.mListeners) {
            if (this.mCameraId == null) {
                tryInitCamera();
            }
            cleanUpListenersLocked(flashlightListener);
            this.mListeners.add(new WeakReference<>(flashlightListener));
            flashlightListener.onFlashlightAvailabilityChanged(this.mTorchAvailable);
            flashlightListener.onFlashlightChanged(this.mFlashlightEnabled);
        }
    }

    @Override
    public void removeCallback(FlashlightController.FlashlightListener flashlightListener) {
        synchronized (this.mListeners) {
            cleanUpListenersLocked(flashlightListener);
        }
    }

    private synchronized void ensureHandler() {
        if (this.mHandler == null) {
            HandlerThread handlerThread = new HandlerThread("FlashlightController", 10);
            handlerThread.start();
            this.mHandler = new Handler(handlerThread.getLooper());
        }
    }

    private String getCameraId() throws CameraAccessException {
        for (String str : this.mCameraManager.getCameraIdList()) {
            CameraCharacteristics cameraCharacteristics = this.mCameraManager.getCameraCharacteristics(str);
            Boolean bool = (Boolean) cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            Integer num = (Integer) cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (bool != null && bool.booleanValue() && num != null && num.intValue() == 1) {
                return str;
            }
        }
        return null;
    }

    private void dispatchModeChanged(boolean z) {
        dispatchListeners(1, z);
    }

    private void dispatchError() {
        dispatchListeners(1, false);
    }

    private void dispatchAvailabilityChanged(boolean z) {
        dispatchListeners(2, z);
    }

    private void dispatchListeners(int i, boolean z) {
        synchronized (this.mListeners) {
            int size = this.mListeners.size();
            boolean z2 = false;
            for (int i2 = 0; i2 < size; i2++) {
                FlashlightController.FlashlightListener flashlightListener = this.mListeners.get(i2).get();
                if (flashlightListener == null) {
                    z2 = true;
                } else if (i == 0) {
                    flashlightListener.onFlashlightError();
                } else if (i == 1) {
                    flashlightListener.onFlashlightChanged(z);
                } else if (i == 2) {
                    flashlightListener.onFlashlightAvailabilityChanged(z);
                }
            }
            if (z2) {
                cleanUpListenersLocked(null);
            }
        }
    }

    private void cleanUpListenersLocked(FlashlightController.FlashlightListener flashlightListener) {
        for (int size = this.mListeners.size() - 1; size >= 0; size--) {
            FlashlightController.FlashlightListener flashlightListener2 = this.mListeners.get(size).get();
            if (flashlightListener2 == null || flashlightListener2 == flashlightListener) {
                this.mListeners.remove(size);
            }
        }
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("FlashlightController state:");
        printWriter.print("  mCameraId=");
        printWriter.println(this.mCameraId);
        printWriter.print("  mFlashlightEnabled=");
        printWriter.println(this.mFlashlightEnabled);
        printWriter.print("  mTorchAvailable=");
        printWriter.println(this.mTorchAvailable);
    }
}
