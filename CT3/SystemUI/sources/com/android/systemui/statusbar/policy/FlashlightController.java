package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class FlashlightController {
    private static final boolean DEBUG = Log.isLoggable("FlashlightController", 3);
    private final String mCameraId;
    private final CameraManager mCameraManager;
    private boolean mFlashlightEnabled;
    private Handler mHandler;
    private boolean mTorchAvailable;
    private final ArrayList<WeakReference<FlashlightListener>> mListeners = new ArrayList<>(1);
    private final CameraManager.TorchCallback mTorchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeUnavailable(String cameraId) {
            if (!TextUtils.equals(cameraId, FlashlightController.this.mCameraId)) {
                return;
            }
            setCameraAvailable(false);
        }

        @Override
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            if (!TextUtils.equals(cameraId, FlashlightController.this.mCameraId)) {
                return;
            }
            setCameraAvailable(true);
            setTorchMode(enabled);
        }

        private void setCameraAvailable(boolean available) {
            boolean changed;
            synchronized (FlashlightController.this) {
                changed = FlashlightController.this.mTorchAvailable != available;
                FlashlightController.this.mTorchAvailable = available;
            }
            if (!changed) {
                return;
            }
            if (FlashlightController.DEBUG) {
                Log.d("FlashlightController", "dispatchAvailabilityChanged(" + available + ")");
            }
            FlashlightController.this.dispatchAvailabilityChanged(available);
        }

        private void setTorchMode(boolean enabled) {
            boolean changed;
            synchronized (FlashlightController.this) {
                changed = FlashlightController.this.mFlashlightEnabled != enabled;
                FlashlightController.this.mFlashlightEnabled = enabled;
            }
            if (!changed) {
                return;
            }
            if (FlashlightController.DEBUG) {
                Log.d("FlashlightController", "dispatchModeChanged(" + enabled + ")");
            }
            FlashlightController.this.dispatchModeChanged(enabled);
        }
    };

    public interface FlashlightListener {
        void onFlashlightAvailabilityChanged(boolean z);

        void onFlashlightChanged(boolean z);

        void onFlashlightError();
    }

    public FlashlightController(Context mContext) {
        this.mCameraManager = (CameraManager) mContext.getSystemService("camera");
        try {
            try {
                String cameraId = getCameraId();
                this.mCameraId = cameraId;
                if (this.mCameraId == null) {
                    return;
                }
                ensureHandler();
                this.mCameraManager.registerTorchCallback(this.mTorchCallback, this.mHandler);
            } catch (Throwable e) {
                Log.e("FlashlightController", "Couldn't initialize.", e);
                this.mCameraId = null;
            }
        } catch (Throwable th) {
            this.mCameraId = null;
            throw th;
        }
    }

    public void setFlashlight(boolean enabled) {
        boolean pendingError = false;
        synchronized (this) {
            if (this.mFlashlightEnabled != enabled) {
                this.mFlashlightEnabled = enabled;
                try {
                    this.mCameraManager.setTorchMode(this.mCameraId, enabled);
                } catch (CameraAccessException e) {
                    Log.e("FlashlightController", "Couldn't set torch mode", e);
                    this.mFlashlightEnabled = false;
                    pendingError = true;
                }
            }
        }
        dispatchModeChanged(this.mFlashlightEnabled);
        if (!pendingError) {
            return;
        }
        dispatchError();
    }

    public boolean hasFlashlight() {
        return this.mCameraId != null;
    }

    public synchronized boolean isEnabled() {
        return this.mFlashlightEnabled;
    }

    public synchronized boolean isAvailable() {
        return this.mTorchAvailable;
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

    public void dispatchModeChanged(boolean enabled) {
        dispatchListeners(1, enabled);
    }

    private void dispatchError() {
        dispatchListeners(1, false);
    }

    public void dispatchAvailabilityChanged(boolean available) {
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
                        l.onFlashlightChanged(argument);
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
