package android.hardware.camera2;

import android.content.Context;
import android.hardware.CameraInfo;
import android.hardware.ICameraService;
import android.hardware.ICameraServiceListener;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.impl.CameraDeviceImpl;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.legacy.CameraDeviceUserShim;
import android.hardware.camera2.legacy.LegacyMetadataMapper;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Binder;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;
import java.util.ArrayList;

public final class CameraManager {
    private static final int API_VERSION_1 = 1;
    private static final int API_VERSION_2 = 2;
    private static final int CAMERA_TYPE_ALL = 1;
    private static final int CAMERA_TYPE_BACKWARD_COMPATIBLE = 0;
    private static final String TAG = "CameraManager";
    private static final int USE_CALLING_UID = -1;
    private final Context mContext;
    private ArrayList<String> mDeviceIdList;
    private final boolean DEBUG = false;
    private final Object mLock = new Object();

    public CameraManager(Context context) {
        synchronized (this.mLock) {
            this.mContext = context;
        }
    }

    public String[] getCameraIdList() throws CameraAccessException {
        String[] strArr;
        synchronized (this.mLock) {
            strArr = (String[]) getOrCreateDeviceIdListLocked().toArray(new String[0]);
        }
        return strArr;
    }

    public void registerAvailabilityCallback(AvailabilityCallback callback, Handler handler) {
        if (handler == null) {
            Looper looper = Looper.myLooper();
            if (looper == null) {
                throw new IllegalArgumentException("No handler given, and current thread has no looper!");
            }
            handler = new Handler(looper);
        }
        CameraManagerGlobal.get().registerAvailabilityCallback(callback, handler);
    }

    public void unregisterAvailabilityCallback(AvailabilityCallback callback) {
        CameraManagerGlobal.get().unregisterAvailabilityCallback(callback);
    }

    public void registerTorchCallback(TorchCallback callback, Handler handler) {
        if (handler == null) {
            Looper looper = Looper.myLooper();
            if (looper == null) {
                throw new IllegalArgumentException("No handler given, and current thread has no looper!");
            }
            handler = new Handler(looper);
        }
        CameraManagerGlobal.get().registerTorchCallback(callback, handler);
    }

    public void unregisterTorchCallback(TorchCallback callback) {
        CameraManagerGlobal.get().unregisterTorchCallback(callback);
    }

    public CameraCharacteristics getCameraCharacteristics(String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics = null;
        synchronized (this.mLock) {
            if (!getOrCreateDeviceIdListLocked().contains(cameraId)) {
                throw new IllegalArgumentException(String.format("Camera id %s does not match any currently connected camera device", cameraId));
            }
            int id = Integer.parseInt(cameraId);
            ICameraService cameraService = CameraManagerGlobal.get().getCameraService();
            if (cameraService == null) {
                throw new CameraAccessException(2, "Camera service is currently unavailable");
            }
            try {
                if (!supportsCamera2ApiLocked(cameraId)) {
                    String parameters = cameraService.getLegacyParameters(id);
                    CameraInfo info = cameraService.getCameraInfo(id);
                    characteristics = LegacyMetadataMapper.createCharacteristics(parameters, info);
                } else {
                    CameraMetadataNative info2 = cameraService.getCameraCharacteristics(id);
                    CameraCharacteristics characteristics2 = new CameraCharacteristics(info2);
                    characteristics = characteristics2;
                }
            } catch (RemoteException e) {
                throw new CameraAccessException(2, "Camera service is currently unavailable", e);
            } catch (ServiceSpecificException e2) {
                throwAsPublicException(e2);
            }
        }
        return characteristics;
    }

    private CameraDevice openCameraDeviceUserAsync(String cameraId, CameraDevice.StateCallback callback, Handler handler) throws CameraAccessException {
        CameraDeviceImpl deviceImpl;
        CameraCharacteristics characteristics = getCameraCharacteristics(cameraId);
        synchronized (this.mLock) {
            ICameraDeviceUser cameraUser = null;
            deviceImpl = new CameraDeviceImpl(cameraId, callback, handler, characteristics);
            ICameraDeviceCallbacks callbacks = deviceImpl.getCallbacks();
            try {
                int id = Integer.parseInt(cameraId);
                try {
                    try {
                        if (supportsCamera2ApiLocked(cameraId)) {
                            ICameraService cameraService = CameraManagerGlobal.get().getCameraService();
                            if (cameraService == null) {
                                throw new ServiceSpecificException(4, "Camera service is currently unavailable");
                            }
                            cameraUser = cameraService.connectDevice(callbacks, id, this.mContext.getOpPackageName(), -1);
                        } else {
                            Log.i(TAG, "Using legacy camera HAL.");
                            cameraUser = CameraDeviceUserShim.connectBinderShim(callbacks, id);
                        }
                    } catch (ServiceSpecificException e) {
                        if (e.errorCode == 9) {
                            throw new AssertionError("Should've gone down the shim path");
                        }
                        if (e.errorCode == 7 || e.errorCode == 8 || e.errorCode == 6 || e.errorCode == 4 || e.errorCode == 10) {
                            deviceImpl.setRemoteFailure(e);
                            if (e.errorCode == 6 || e.errorCode == 4 || e.errorCode == 7) {
                                throwAsPublicException(e);
                            }
                        } else {
                            throwAsPublicException(e);
                        }
                    }
                } catch (RemoteException e2) {
                    ServiceSpecificException sse = new ServiceSpecificException(4, "Camera service is currently unavailable");
                    deviceImpl.setRemoteFailure(sse);
                    throwAsPublicException(sse);
                }
                deviceImpl.setRemoteDevice(cameraUser);
            } catch (NumberFormatException e3) {
                throw new IllegalArgumentException("Expected cameraId to be numeric, but it was: " + cameraId);
            }
        }
        return deviceImpl;
    }

    public void openCamera(String cameraId, CameraDevice.StateCallback callback, Handler handler) throws CameraAccessException {
        if (cameraId == null) {
            throw new IllegalArgumentException("cameraId was null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback was null");
        }
        if (handler == null) {
            if (Looper.myLooper() != null) {
                handler = new Handler();
            } else {
                throw new IllegalArgumentException("Handler argument is null, but no looper exists in the calling thread");
            }
        }
        openCameraDeviceUserAsync(cameraId, callback, handler);
    }

    public void setTorchMode(String cameraId, boolean enabled) throws CameraAccessException {
        CameraManagerGlobal.get().setTorchMode(cameraId, enabled);
    }

    public static abstract class AvailabilityCallback {
        public void onCameraAvailable(String cameraId) {
        }

        public void onCameraUnavailable(String cameraId) {
        }
    }

    public static abstract class TorchCallback {
        public void onTorchModeUnavailable(String cameraId) {
        }

        public void onTorchModeChanged(String cameraId, boolean enabled) {
        }
    }

    public static void throwAsPublicException(Throwable t) throws CameraAccessException {
        int reason;
        if (t instanceof ServiceSpecificException) {
            ServiceSpecificException e = (ServiceSpecificException) t;
            switch (e.errorCode) {
                case 1:
                    throw new SecurityException(e.getMessage(), e);
                case 2:
                case 3:
                    throw new IllegalArgumentException(e.getMessage(), e);
                case 4:
                    reason = 2;
                    break;
                case 5:
                default:
                    reason = 3;
                    break;
                case 6:
                    reason = 1;
                    break;
                case 7:
                    reason = 4;
                    break;
                case 8:
                    reason = 5;
                    break;
                case 9:
                    reason = 1000;
                    break;
            }
            throw new CameraAccessException(reason, e.getMessage(), e);
        }
        if (t instanceof DeadObjectException) {
            throw new CameraAccessException(2, "Camera service has died unexpectedly", t);
        }
        if (t instanceof RemoteException) {
            throw new UnsupportedOperationException("An unknown RemoteException was thrown which should never happen.", t);
        }
        if (!(t instanceof RuntimeException)) {
        } else {
            throw ((RuntimeException) t);
        }
    }

    private ArrayList<String> getOrCreateDeviceIdListLocked() throws CameraAccessException {
        CameraMetadataNative info;
        if (this.mDeviceIdList == null || SystemProperties.get("ro.mtk_crossmount_support").equals(WifiEnterpriseConfig.ENGINE_ENABLE)) {
            int numCameras = 0;
            ICameraService cameraService = CameraManagerGlobal.get().getCameraService();
            ArrayList<String> deviceIdList = new ArrayList<>();
            if (cameraService == null) {
                return deviceIdList;
            }
            try {
                numCameras = cameraService.getNumberOfCameras(1);
                if (numCameras > 2) {
                    numCameras = 2;
                }
            } catch (RemoteException e) {
                return deviceIdList;
            } catch (ServiceSpecificException e2) {
                throwAsPublicException(e2);
            }
            for (int i = 0; i < numCameras; i++) {
                boolean isDeviceSupported = false;
                try {
                    info = cameraService.getCameraCharacteristics(i);
                } catch (RemoteException e3) {
                    deviceIdList.clear();
                    return deviceIdList;
                } catch (ServiceSpecificException e4) {
                    if (e4.errorCode != 4 || e4.errorCode != 3) {
                        throwAsPublicException(e4);
                    }
                }
                if (!info.isEmpty()) {
                    isDeviceSupported = true;
                    if (isDeviceSupported) {
                        deviceIdList.add(String.valueOf(i));
                    } else {
                        Log.w(TAG, "Error querying camera device " + i + " for listing.");
                    }
                } else {
                    throw new AssertionError("Expected to get non-empty characteristics");
                }
            }
            this.mDeviceIdList = deviceIdList;
        }
        return this.mDeviceIdList;
    }

    private boolean supportsCamera2ApiLocked(String cameraId) {
        return supportsCameraApiLocked(cameraId, 2);
    }

    private boolean supportsCameraApiLocked(String cameraId, int apiVersion) {
        int id = Integer.parseInt(cameraId);
        try {
            ICameraService cameraService = CameraManagerGlobal.get().getCameraService();
            if (cameraService == null) {
                return false;
            }
            return cameraService.supportsCameraApi(id, apiVersion);
        } catch (RemoteException e) {
            return false;
        }
    }

    private static final class CameraManagerGlobal extends ICameraServiceListener.Stub implements IBinder.DeathRecipient {
        private static final String CAMERA_SERVICE_BINDER_NAME = "media.camera";
        private static final String TAG = "CameraManagerGlobal";
        private static final CameraManagerGlobal gCameraManager = new CameraManagerGlobal();
        private ICameraService mCameraService;
        private final boolean DEBUG = false;
        private final int CAMERA_SERVICE_RECONNECT_DELAY_MS = 1000;
        private final ArrayMap<String, Integer> mDeviceStatus = new ArrayMap<>();
        private final ArrayMap<AvailabilityCallback, Handler> mCallbackMap = new ArrayMap<>();
        private Binder mTorchClientBinder = new Binder();
        private final ArrayMap<String, Integer> mTorchStatus = new ArrayMap<>();
        private final ArrayMap<TorchCallback, Handler> mTorchCallbackMap = new ArrayMap<>();
        private final Object mLock = new Object();

        private CameraManagerGlobal() {
        }

        public static CameraManagerGlobal get() {
            return gCameraManager;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        public ICameraService getCameraService() {
            ICameraService iCameraService;
            synchronized (this.mLock) {
                connectCameraServiceLocked();
                if (this.mCameraService == null) {
                    Log.e(TAG, "Camera service is unavailable");
                }
                iCameraService = this.mCameraService;
            }
            return iCameraService;
        }

        private void connectCameraServiceLocked() {
            if (this.mCameraService != null) {
                return;
            }
            Log.i(TAG, "Connecting to camera service");
            IBinder cameraServiceBinder = ServiceManager.getService(CAMERA_SERVICE_BINDER_NAME);
            if (cameraServiceBinder == null) {
                return;
            }
            try {
                cameraServiceBinder.linkToDeath(this, 0);
                ICameraService cameraService = ICameraService.Stub.asInterface(cameraServiceBinder);
                try {
                    CameraMetadataNative.setupGlobalVendorTagDescriptor();
                } catch (ServiceSpecificException e) {
                    handleRecoverableSetupErrors(e);
                }
                try {
                    cameraService.addListener(this);
                    this.mCameraService = cameraService;
                } catch (RemoteException e2) {
                } catch (ServiceSpecificException e3) {
                    throw new IllegalStateException("Failed to register a camera service listener", e3);
                }
            } catch (RemoteException e4) {
            }
        }

        public void setTorchMode(String cameraId, boolean enabled) throws CameraAccessException {
            synchronized (this.mLock) {
                if (cameraId == null) {
                    throw new IllegalArgumentException("cameraId was null");
                }
                ICameraService cameraService = getCameraService();
                if (cameraService == null) {
                    throw new CameraAccessException(2, "Camera service is currently unavailable");
                }
                try {
                    cameraService.setTorchMode(cameraId, enabled, this.mTorchClientBinder);
                } catch (RemoteException e) {
                    throw new CameraAccessException(2, "Camera service is currently unavailable");
                } catch (ServiceSpecificException e2) {
                    CameraManager.throwAsPublicException(e2);
                }
            }
        }

        private void handleRecoverableSetupErrors(ServiceSpecificException e) {
            switch (e.errorCode) {
                case 4:
                    Log.w(TAG, e.getMessage());
                    return;
                default:
                    throw new IllegalStateException(e);
            }
        }

        private boolean isAvailable(int status) {
            switch (status) {
                case 1:
                    return true;
                default:
                    return false;
            }
        }

        private boolean validStatus(int status) {
            switch (status) {
                case -2:
                case 0:
                case 1:
                case 2:
                    return true;
                case -1:
                default:
                    return false;
            }
        }

        private boolean validTorchStatus(int status) {
            switch (status) {
                case 0:
                case 1:
                case 2:
                    return true;
                default:
                    return false;
            }
        }

        private void postSingleUpdate(final AvailabilityCallback callback, Handler handler, final String id, int status) {
            if (isAvailable(status)) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (Integer.parseInt(id) < 2) {
                            callback.onCameraAvailable(id);
                        }
                    }
                });
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (Integer.parseInt(id) < 2) {
                            callback.onCameraUnavailable(id);
                        }
                    }
                });
            }
        }

        private void postSingleTorchUpdate(final TorchCallback callback, Handler handler, final String id, final int status) {
            switch (status) {
                case 1:
                case 2:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onTorchModeChanged(id, status == 2);
                        }
                    });
                    break;
                default:
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onTorchModeUnavailable(id);
                        }
                    });
                    break;
            }
        }

        private void updateCallbackLocked(AvailabilityCallback callback, Handler handler) {
            for (int i = 0; i < this.mDeviceStatus.size(); i++) {
                String id = this.mDeviceStatus.keyAt(i);
                Integer status = this.mDeviceStatus.valueAt(i);
                postSingleUpdate(callback, handler, id, status.intValue());
            }
        }

        private void onStatusChangedLocked(int status, String id) {
            if (!validStatus(status)) {
                Log.e(TAG, String.format("Ignoring invalid device %s status 0x%x", id, Integer.valueOf(status)));
                return;
            }
            Integer oldStatus = this.mDeviceStatus.put(id, Integer.valueOf(status));
            if (oldStatus != null && oldStatus.intValue() == status) {
                return;
            }
            if (oldStatus != null && isAvailable(status) == isAvailable(oldStatus.intValue())) {
                return;
            }
            int callbackCount = this.mCallbackMap.size();
            for (int i = 0; i < callbackCount; i++) {
                Handler handler = this.mCallbackMap.valueAt(i);
                AvailabilityCallback callback = this.mCallbackMap.keyAt(i);
                postSingleUpdate(callback, handler, id, status);
            }
        }

        private void updateTorchCallbackLocked(TorchCallback callback, Handler handler) {
            for (int i = 0; i < this.mTorchStatus.size(); i++) {
                String id = this.mTorchStatus.keyAt(i);
                Integer status = this.mTorchStatus.valueAt(i);
                postSingleTorchUpdate(callback, handler, id, status.intValue());
            }
        }

        private void onTorchStatusChangedLocked(int status, String id) {
            if (!validTorchStatus(status)) {
                Log.e(TAG, String.format("Ignoring invalid device %s torch status 0x%x", id, Integer.valueOf(status)));
                return;
            }
            Integer oldStatus = this.mTorchStatus.put(id, Integer.valueOf(status));
            if (oldStatus != null && oldStatus.intValue() == status) {
                return;
            }
            int callbackCount = this.mTorchCallbackMap.size();
            for (int i = 0; i < callbackCount; i++) {
                Handler handler = this.mTorchCallbackMap.valueAt(i);
                TorchCallback callback = this.mTorchCallbackMap.keyAt(i);
                postSingleTorchUpdate(callback, handler, id, status);
            }
        }

        public void registerAvailabilityCallback(AvailabilityCallback callback, Handler handler) {
            synchronized (this.mLock) {
                connectCameraServiceLocked();
                Handler oldHandler = this.mCallbackMap.put(callback, handler);
                if (oldHandler == null) {
                    updateCallbackLocked(callback, handler);
                }
                if (this.mCameraService == null) {
                    scheduleCameraServiceReconnectionLocked();
                }
            }
        }

        public void unregisterAvailabilityCallback(AvailabilityCallback callback) {
            synchronized (this.mLock) {
                this.mCallbackMap.remove(callback);
            }
        }

        public void registerTorchCallback(TorchCallback callback, Handler handler) {
            synchronized (this.mLock) {
                connectCameraServiceLocked();
                Handler oldHandler = this.mTorchCallbackMap.put(callback, handler);
                if (oldHandler == null) {
                    updateTorchCallbackLocked(callback, handler);
                }
                if (this.mCameraService == null) {
                    scheduleCameraServiceReconnectionLocked();
                }
            }
        }

        public void unregisterTorchCallback(TorchCallback callback) {
            synchronized (this.mLock) {
                this.mTorchCallbackMap.remove(callback);
            }
        }

        @Override
        public void onStatusChanged(int status, int cameraId) throws RemoteException {
            synchronized (this.mLock) {
                onStatusChangedLocked(status, String.valueOf(cameraId));
            }
        }

        @Override
        public void onTorchStatusChanged(int status, String cameraId) throws RemoteException {
            synchronized (this.mLock) {
                onTorchStatusChangedLocked(status, cameraId);
            }
        }

        private void scheduleCameraServiceReconnectionLocked() {
            Handler handler;
            if (this.mCallbackMap.size() > 0) {
                handler = this.mCallbackMap.valueAt(0);
            } else if (this.mTorchCallbackMap.size() > 0) {
                handler = this.mTorchCallbackMap.valueAt(0);
            } else {
                return;
            }
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ICameraService cameraService = CameraManagerGlobal.this.getCameraService();
                    if (cameraService != null) {
                        return;
                    }
                    synchronized (CameraManagerGlobal.this.mLock) {
                        CameraManagerGlobal.this.scheduleCameraServiceReconnectionLocked();
                    }
                }
            }, 1000L);
        }

        @Override
        public void binderDied() {
            synchronized (this.mLock) {
                if (this.mCameraService == null) {
                    return;
                }
                this.mCameraService = null;
                for (int i = 0; i < this.mDeviceStatus.size(); i++) {
                    String cameraId = this.mDeviceStatus.keyAt(i);
                    onStatusChangedLocked(0, cameraId);
                }
                for (int i2 = 0; i2 < this.mTorchStatus.size(); i2++) {
                    String cameraId2 = this.mTorchStatus.keyAt(i2);
                    onTorchStatusChangedLocked(0, cameraId2);
                }
                scheduleCameraServiceReconnectionLocked();
            }
        }
    }
}
