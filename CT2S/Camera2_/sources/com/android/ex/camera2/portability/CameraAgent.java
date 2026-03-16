package com.android.ex.camera2.portability;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.debug.Log;

public abstract class CameraAgent {
    public static final long CAMERA_OPERATION_TIMEOUT_MS = 5000;
    private static final Log.Tag TAG = new Log.Tag("CamAgnt");
    private static int mCameraOpened = 0;
    private static int monCameraOpened = 0;
    private static int mCameraClosed = 0;

    public interface CameraAFCallback {
        void onAutoFocus(boolean z, CameraProxy cameraProxy);
    }

    public interface CameraAFMoveCallback {
        void onAutoFocusMoving(boolean z, CameraProxy cameraProxy);
    }

    public interface CameraErrorCallback {
        void onError(int i, CameraProxy cameraProxy);
    }

    public interface CameraFaceDetectionCallback {
        void onFaceDetection(Camera.Face[] faceArr, CameraProxy cameraProxy);
    }

    public interface CameraIspInfoCallback {
        void onIspInfo(Camera.IspInfo ispInfo, CameraProxy cameraProxy);
    }

    public interface CameraOpenCallback {
        void onCameraDisabled(int i);

        void onCameraOpened(CameraProxy cameraProxy);

        void onDeviceOpenFailure(int i, String str);

        void onDeviceOpenedAlready(int i, String str);

        void onReconnectionFailure(CameraAgent cameraAgent, String str);
    }

    public interface CameraPictureCallback {
        void onPictureTaken(byte[] bArr, CameraProxy cameraProxy);
    }

    public interface CameraPreviewDataCallback {
        void onPreviewFrame(byte[] bArr, CameraProxy cameraProxy);
    }

    public interface CameraSceneDetectionCallback {
        void onSceneDetection(String str, CameraProxy cameraProxy);
    }

    public interface CameraShutterCallback {
        void onShutter(CameraProxy cameraProxy);
    }

    public interface CameraStartPreviewCallback {
        void onPreviewStarted();
    }

    public abstract CameraDeviceInfo getCameraDeviceInfo();

    protected abstract CameraExceptionHandler getCameraExceptionHandler();

    protected abstract Handler getCameraHandler();

    protected abstract CameraStateHolder getCameraState();

    protected abstract DispatchThread getDispatchThread();

    public abstract void recycle();

    public abstract void setCameraExceptionHandler(CameraExceptionHandler cameraExceptionHandler);

    static int access$110() {
        int i = mCameraClosed;
        mCameraClosed = i - 1;
        return i;
    }

    static int access$210() {
        int i = mCameraOpened;
        mCameraOpened = i - 1;
        return i;
    }

    static int access$308() {
        int i = monCameraOpened;
        monCameraOpened = i + 1;
        return i;
    }

    public static class CameraStartPreviewCallbackForward implements CameraStartPreviewCallback {
        private final CameraStartPreviewCallback mCallback;
        private final Handler mHandler;

        public static CameraStartPreviewCallbackForward getNewInstance(Handler handler, CameraStartPreviewCallback cb) {
            if (handler == null || cb == null) {
                return null;
            }
            return new CameraStartPreviewCallbackForward(handler, cb);
        }

        private CameraStartPreviewCallbackForward(Handler h, CameraStartPreviewCallback cb) {
            this.mHandler = h;
            this.mCallback = cb;
        }

        @Override
        public void onPreviewStarted() {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    CameraStartPreviewCallbackForward.this.mCallback.onPreviewStarted();
                }
            });
        }
    }

    public static class CameraOpenCallbackForward implements CameraOpenCallback {
        private final CameraOpenCallback mCallback;
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        public static CameraOpenCallbackForward getNewInstance(Handler handler, CameraOpenCallback cb) {
            if (handler == null || cb == null) {
                return null;
            }
            return new CameraOpenCallbackForward(handler, cb);
        }

        private CameraOpenCallbackForward(Handler h, CameraOpenCallback cb) {
            this.mCallback = cb;
        }

        @Override
        public void onCameraOpened(final CameraProxy camera) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (CameraAgent.mCameraClosed > 0) {
                        if (CameraAgent.mCameraOpened > 0) {
                            CameraAgent.access$210();
                            CameraAgent.access$110();
                            return;
                        }
                        return;
                    }
                    if (CameraAgent.mCameraOpened > CameraAgent.monCameraOpened) {
                        CameraAgent.access$308();
                    }
                    CameraOpenCallbackForward.this.mCallback.onCameraOpened(camera);
                }
            });
        }

        @Override
        public void onCameraDisabled(final int cameraId) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    CameraOpenCallbackForward.this.mCallback.onCameraDisabled(cameraId);
                }
            });
        }

        @Override
        public void onDeviceOpenFailure(final int cameraId, final String info) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    CameraOpenCallbackForward.this.mCallback.onDeviceOpenFailure(cameraId, info);
                }
            });
        }

        @Override
        public void onDeviceOpenedAlready(final int cameraId, final String info) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    CameraOpenCallbackForward.this.mCallback.onDeviceOpenedAlready(cameraId, info);
                }
            });
        }

        @Override
        public void onReconnectionFailure(final CameraAgent mgr, final String info) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    CameraOpenCallbackForward.this.mCallback.onReconnectionFailure(mgr, info);
                }
            });
        }
    }

    private static class IspInfoCallbackForward implements Camera.IspInfoListener {
        private final CameraIspInfoCallback mCallback;
        private final CameraProxy mCamera;
        private final Handler mHandler;

        public static IspInfoCallbackForward getNewInstance(Handler handler, CameraProxy camera, CameraIspInfoCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new IspInfoCallbackForward(handler, camera, cb);
        }

        private IspInfoCallbackForward(Handler h, CameraProxy camera, CameraIspInfoCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        public void onIspInfo(final Camera.IspInfo ispInfo, Camera camera) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    IspInfoCallbackForward.this.mCallback.onIspInfo(ispInfo, IspInfoCallbackForward.this.mCamera);
                }
            });
        }
    }

    public void openCamera(final Handler handler, final int cameraId, final CameraOpenCallback callback) {
        mCameraOpened++;
        try {
            getDispatchThread().runJob(new Runnable() {
                @Override
                public void run() {
                    CameraAgent.this.getCameraHandler().obtainMessage(1, cameraId, 0, CameraOpenCallbackForward.getNewInstance(handler, callback)).sendToTarget();
                }
            });
        } catch (RuntimeException ex) {
            getCameraExceptionHandler().onDispatchThreadException(ex);
        }
    }

    public void closeCamera(CameraProxy camera, boolean synced) {
        if (mCameraOpened > 0) {
            if (mCameraOpened == monCameraOpened) {
                mCameraOpened = 0;
                monCameraOpened = 0;
                mCameraClosed = 0;
            } else if (mCameraOpened > mCameraClosed) {
                mCameraClosed++;
            }
        }
        try {
            if (synced) {
                if (!getCameraState().isInvalid()) {
                    final WaitDoneBundle bundle = new WaitDoneBundle();
                    getDispatchThread().runJobSync(new Runnable() {
                        @Override
                        public void run() {
                            CameraAgent.this.getCameraHandler().obtainMessage(2).sendToTarget();
                            CameraAgent.this.getCameraHandler().post(bundle.mUnlockRunnable);
                        }
                    }, bundle.mWaitLock, CAMERA_OPERATION_TIMEOUT_MS, "camera release");
                }
            } else {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraAgent.this.getCameraHandler().removeCallbacksAndMessages(null);
                        CameraAgent.this.getCameraHandler().obtainMessage(2).sendToTarget();
                    }
                });
            }
        } catch (RuntimeException ex) {
            getCameraExceptionHandler().onDispatchThreadException(ex);
        }
    }

    public static abstract class CameraProxy {
        public abstract boolean applySettings(CameraSettings cameraSettings);

        public abstract void autoFocus(Handler handler, CameraAFCallback cameraAFCallback);

        public abstract String dumpDeviceSettings();

        public abstract CameraAgent getAgent();

        @Deprecated
        public abstract Camera getCamera();

        public abstract Handler getCameraHandler();

        public abstract int getCameraId();

        public abstract CameraStateHolder getCameraState();

        public abstract CameraCapabilities getCapabilities();

        public abstract CameraDeviceInfo.Characteristics getCharacteristics();

        public abstract DispatchThread getDispatchThread();

        @Deprecated
        public abstract Camera.Parameters getParameters();

        public abstract CameraSettings getSettings();

        @TargetApi(16)
        public abstract void setAutoFocusMoveCallback(Handler handler, CameraAFMoveCallback cameraAFMoveCallback);

        public abstract void setFaceDetectionCallback(Handler handler, CameraFaceDetectionCallback cameraFaceDetectionCallback);

        public abstract void setOneShotPreviewCallback(Handler handler, CameraPreviewDataCallback cameraPreviewDataCallback);

        @Deprecated
        public abstract void setParameters(Camera.Parameters parameters);

        public abstract void setPreviewDataCallback(Handler handler, CameraPreviewDataCallback cameraPreviewDataCallback);

        public abstract void setPreviewDataCallbackWithBuffer(Handler handler, CameraPreviewDataCallback cameraPreviewDataCallback);

        public abstract void setZoomChangeListener(Camera.OnZoomChangeListener onZoomChangeListener);

        public abstract void takePicture(Handler handler, CameraShutterCallback cameraShutterCallback, CameraPictureCallback cameraPictureCallback, CameraPictureCallback cameraPictureCallback2, CameraPictureCallback cameraPictureCallback3);

        public abstract CameraCapabilities updateCapabilities();

        public void reconnect(final Handler handler, final CameraOpenCallback cb) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(3, CameraProxy.this.getCameraId(), 0, CameraOpenCallbackForward.getNewInstance(handler, cb)).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void unlock() {
            if (!getCameraState().isInvalid()) {
                final WaitDoneBundle bundle = new WaitDoneBundle();
                try {
                    getDispatchThread().runJobSync(new Runnable() {
                        @Override
                        public void run() {
                            CameraProxy.this.getCameraHandler().sendEmptyMessage(4);
                            CameraProxy.this.getCameraHandler().post(bundle.mUnlockRunnable);
                        }
                    }, bundle.mWaitLock, CameraAgent.CAMERA_OPERATION_TIMEOUT_MS, "camera unlock");
                } catch (RuntimeException ex) {
                    getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
                }
            }
        }

        public void lock() {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().sendEmptyMessage(5);
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void setPreviewTexture(final SurfaceTexture surfaceTexture) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(101, surfaceTexture).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void setPreviewTextureSync(final SurfaceTexture surfaceTexture) {
            if (!getCameraState().isInvalid()) {
                final WaitDoneBundle bundle = new WaitDoneBundle();
                try {
                    getDispatchThread().runJobSync(new Runnable() {
                        @Override
                        public void run() {
                            CameraProxy.this.getCameraHandler().obtainMessage(101, surfaceTexture).sendToTarget();
                            CameraProxy.this.getCameraHandler().post(bundle.mUnlockRunnable);
                        }
                    }, bundle.mWaitLock, CameraAgent.CAMERA_OPERATION_TIMEOUT_MS, "set preview texture");
                } catch (RuntimeException ex) {
                    getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
                }
            }
        }

        public void setPreviewDisplay(final SurfaceHolder surfaceHolder) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(106, surfaceHolder).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void startPreview() {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(102, null).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void startPreviewWithCallback(final Handler h, final CameraStartPreviewCallback cb) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(102, CameraStartPreviewCallbackForward.getNewInstance(h, cb)).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void stopPreview() {
            if (!getCameraState().isInvalid()) {
                final WaitDoneBundle bundle = new WaitDoneBundle();
                try {
                    getDispatchThread().runJobSync(new Runnable() {
                        @Override
                        public void run() {
                            CameraProxy.this.getCameraHandler().obtainMessage(103, bundle).sendToTarget();
                        }
                    }, bundle.mWaitLock, CameraAgent.CAMERA_OPERATION_TIMEOUT_MS, "stop preview");
                } catch (RuntimeException ex) {
                    getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
                }
            }
        }

        public void addCallbackBuffer(final byte[] callbackBuffer) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(105, callbackBuffer).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void cancelAutoFocus() {
            getCameraHandler().sendMessageAtFrontOfQueue(getCameraHandler().obtainMessage(CameraActions.CANCEL_AUTO_FOCUS));
            getCameraHandler().sendEmptyMessage(CameraActions.CANCEL_AUTO_FOCUS_FINISH);
        }

        public void setDisplayOrientation(int degrees) {
            setDisplayOrientation(degrees, true);
        }

        public void setDisplayOrientation(final int degrees, final boolean capture) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(CameraActions.SET_DISPLAY_ORIENTATION, degrees, capture ? 1 : 0).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void setJpegOrientation(final int degrees) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(CameraActions.SET_JPEG_ORIENTATION, degrees, 0).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void setSceneDetectionCallback(Handler handler, CameraSceneDetectionCallback callback) {
        }

        public void startFaceDetection() {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(CameraActions.START_FACE_DETECTION, 2).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void startFaceDetection(final int type) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(CameraActions.START_FACE_DETECTION, Integer.valueOf(type)).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void stopFaceDetection() {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().sendEmptyMessage(CameraActions.STOP_FACE_DETECTION);
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        protected boolean applySettingsHelper(CameraSettings settings, final int statesToAwait) {
            if (settings == null) {
                Log.v(CameraAgent.TAG, "null argument in applySettings()");
                return false;
            }
            if (!getCapabilities().supports(settings)) {
                Log.w(CameraAgent.TAG, "Unsupported settings in applySettings()");
                return false;
            }
            final CameraSettings copyOfSettings = settings.copy();
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraStateHolder cameraState = CameraProxy.this.getCameraState();
                        if (!cameraState.isInvalid()) {
                            cameraState.waitForStates(statesToAwait);
                            CameraProxy.this.getCameraHandler().obtainMessage(204, copyOfSettings).sendToTarget();
                        }
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
            return true;
        }

        public void refreshSettings() {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().sendEmptyMessage(203);
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void enableShutterSound(final boolean enable) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    @Override
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(CameraActions.ENABLE_SHUTTER_SOUND, enable ? 1 : 0, 0).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public int setRegister(int address, int value) {
            return -1;
        }

        public int getRegister(int address) {
            return -1;
        }

        public void setIspInfoCallback(Handler handler, CameraIspInfoCallback callback) {
        }
    }

    public static class WaitDoneBundle {
        public final Object mWaitLock = new Object();
        public final Runnable mUnlockRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (WaitDoneBundle.this.mWaitLock) {
                    WaitDoneBundle.this.mWaitLock.notifyAll();
                }
            }
        };

        WaitDoneBundle() {
        }

        static void unblockSyncWaiters(Message msg) {
            if (msg != null && (msg.obj instanceof WaitDoneBundle)) {
                WaitDoneBundle bundle = (WaitDoneBundle) msg.obj;
                bundle.mUnlockRunnable.run();
            }
        }
    }
}
