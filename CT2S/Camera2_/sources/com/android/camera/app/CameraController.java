package com.android.camera.app;

import android.content.Context;
import android.os.Handler;
import com.android.camera.CameraDisabledException;
import com.android.camera.debug.Log;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.CameraExceptionHandler;

public class CameraController implements CameraAgent.CameraOpenCallback, CameraProvider {
    private static final int EMPTY_REQUEST = -1;
    private static final Log.Tag TAG = new Log.Tag("CameraController");
    private final Handler mCallbackHandler;
    private CameraAgent.CameraOpenCallback mCallbackReceiver;
    private final CameraAgent mCameraAgent;
    private final CameraAgent mCameraAgentNg;
    private CameraAgent.CameraProxy mCameraProxy;
    private final Context mContext;
    private CameraDeviceInfo mInfo;
    private int mRequestingCameraId = -1;
    private boolean mUsingNewApi = false;

    public CameraController(Context context, CameraAgent.CameraOpenCallback callbackReceiver, Handler handler, CameraAgent cameraManager, CameraAgent cameraManagerNg) {
        this.mContext = context;
        this.mCallbackReceiver = callbackReceiver;
        this.mCallbackHandler = handler;
        this.mCameraAgent = cameraManager;
        this.mCameraAgentNg = cameraManagerNg == cameraManager ? null : cameraManagerNg;
        this.mInfo = this.mCameraAgent.getCameraDeviceInfo();
        if (this.mInfo == null && this.mCallbackReceiver != null) {
            this.mCallbackReceiver.onDeviceOpenFailure(-1, "GETTING_CAMERA_INFO");
        }
    }

    @Override
    public void setCameraExceptionHandler(CameraExceptionHandler exceptionHandler) {
        this.mCameraAgent.setCameraExceptionHandler(exceptionHandler);
        if (this.mCameraAgentNg != null) {
            this.mCameraAgentNg.setCameraExceptionHandler(exceptionHandler);
        }
    }

    @Override
    public CameraDeviceInfo.Characteristics getCharacteristics(int cameraId) {
        if (this.mInfo == null) {
            return null;
        }
        return this.mInfo.getCharacteristics(cameraId);
    }

    @Override
    public int getCurrentCameraId() {
        if (this.mCameraProxy != null) {
            return this.mCameraProxy.getCameraId();
        }
        Log.v(TAG, "getCurrentCameraId without an open camera... returning requested id");
        return this.mRequestingCameraId;
    }

    @Override
    public int getNumberOfCameras() {
        if (this.mInfo == null) {
            return 0;
        }
        return this.mInfo.getNumberOfCameras();
    }

    @Override
    public int getFirstBackCameraId() {
        if (this.mInfo == null) {
            return -1;
        }
        return this.mInfo.getFirstBackCameraId();
    }

    @Override
    public int getFirstFrontCameraId() {
        if (this.mInfo == null) {
            return -1;
        }
        return this.mInfo.getFirstFrontCameraId();
    }

    @Override
    public boolean isFrontFacingCamera(int id) {
        if (this.mInfo == null) {
            return false;
        }
        if (id >= this.mInfo.getNumberOfCameras() || this.mInfo.getCharacteristics(id) == null) {
            Log.e(TAG, "Camera info not available:" + id);
            return false;
        }
        return this.mInfo.getCharacteristics(id).isFacingFront();
    }

    @Override
    public boolean isBackFacingCamera(int id) {
        if (this.mInfo == null) {
            return false;
        }
        if (id >= this.mInfo.getNumberOfCameras() || this.mInfo.getCharacteristics(id) == null) {
            Log.e(TAG, "Camera info not available:" + id);
            return false;
        }
        return this.mInfo.getCharacteristics(id).isFacingBack();
    }

    @Override
    public void onCameraOpened(CameraAgent.CameraProxy camera) {
        Log.v(TAG, "onCameraOpened");
        if (this.mRequestingCameraId == camera.getCameraId()) {
            this.mCameraProxy = camera;
            this.mRequestingCameraId = -1;
            if (this.mCallbackReceiver != null) {
                this.mCallbackReceiver.onCameraOpened(camera);
            }
        }
    }

    @Override
    public void onCameraDisabled(int cameraId) {
        if (this.mCallbackReceiver != null) {
            this.mCallbackReceiver.onCameraDisabled(cameraId);
        }
    }

    @Override
    public void onDeviceOpenFailure(int cameraId, String info) {
        if (this.mCallbackReceiver != null) {
            this.mCallbackReceiver.onDeviceOpenFailure(cameraId, info);
        }
    }

    @Override
    public void onDeviceOpenedAlready(int cameraId, String info) {
        if (this.mCallbackReceiver != null) {
            this.mCallbackReceiver.onDeviceOpenedAlready(cameraId, info);
        }
    }

    @Override
    public void onReconnectionFailure(CameraAgent mgr, String info) {
        if (this.mCallbackReceiver != null) {
            this.mCallbackReceiver.onReconnectionFailure(mgr, info);
        }
    }

    @Override
    public void requestCamera(int id) {
        requestCamera(id, false);
    }

    @Override
    public void requestCamera(int id, boolean useNewApi) {
        Log.v(TAG, "requestCamera");
        if (this.mRequestingCameraId == -1 && this.mRequestingCameraId != id && this.mInfo != null) {
            this.mRequestingCameraId = id;
            boolean useNewApi2 = this.mCameraAgentNg != null && useNewApi;
            CameraAgent cameraManager = useNewApi2 ? this.mCameraAgentNg : this.mCameraAgent;
            if (this.mCameraProxy == null) {
                checkAndOpenCamera(this.mContext, cameraManager, id, this.mCallbackHandler, this);
            } else if (this.mCameraProxy.getCameraId() != id || this.mUsingNewApi != useNewApi2) {
                boolean syncClose = GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mContext);
                Log.v(TAG, "different camera already opened, closing then reopening");
                if (this.mUsingNewApi) {
                    this.mCameraAgentNg.closeCamera(this.mCameraProxy, true);
                } else {
                    this.mCameraAgent.closeCamera(this.mCameraProxy, syncClose);
                }
                checkAndOpenCamera(this.mContext, cameraManager, id, this.mCallbackHandler, this);
            } else {
                Log.v(TAG, "reconnecting to use the existing camera");
                this.mCameraProxy.reconnect(this.mCallbackHandler, this);
                this.mCameraProxy = null;
            }
            this.mUsingNewApi = useNewApi2;
            this.mInfo = cameraManager.getCameraDeviceInfo();
        }
    }

    @Override
    public boolean waitingForCamera() {
        return this.mRequestingCameraId != -1;
    }

    @Override
    public void releaseCamera(int id) {
        if (this.mCameraProxy == null) {
            if (this.mRequestingCameraId == -1) {
                Log.w(TAG, "Trying to release the camera before requesting");
            }
            this.mRequestingCameraId = -1;
        } else {
            if (this.mCameraProxy.getCameraId() != id) {
                throw new IllegalStateException("Trying to release an unopened camera.");
            }
            this.mRequestingCameraId = -1;
        }
    }

    public void removeCallbackReceiver() {
        this.mCallbackReceiver = null;
    }

    public void closeCamera(boolean synced) {
        Log.v(TAG, "Closing camera");
        this.mCameraProxy = null;
        if (this.mUsingNewApi) {
            this.mCameraAgentNg.closeCamera(this.mCameraProxy, synced);
        } else {
            this.mCameraAgent.closeCamera(this.mCameraProxy, synced);
        }
        this.mRequestingCameraId = -1;
        this.mUsingNewApi = false;
    }

    private static void checkAndOpenCamera(Context context, CameraAgent cameraManager, final int cameraId, Handler handler, final CameraAgent.CameraOpenCallback cb) {
        Log.v(TAG, "checkAndOpenCamera");
        try {
            CameraUtil.throwIfCameraDisabled(context);
            cameraManager.openCamera(handler, cameraId, cb);
        } catch (CameraDisabledException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    cb.onCameraDisabled(cameraId);
                }
            });
        }
    }

    public void setOneShotPreviewCallback(Handler handler, CameraAgent.CameraPreviewDataCallback cb) {
        this.mCameraProxy.setOneShotPreviewCallback(handler, cb);
    }
}
