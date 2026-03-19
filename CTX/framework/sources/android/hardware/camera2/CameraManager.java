package android.hardware.camera2;

import android.content.Context;
import android.hardware.CameraStatus;
import android.hardware.ICameraService;
import android.hardware.ICameraServiceListener;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.impl.CameraDeviceImpl;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.legacy.CameraDeviceUserShim;
import android.hardware.camera2.legacy.LegacyMetadataMapper;
import android.os.Binder;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        return CameraManagerGlobal.get().getCameraIdList();
    }

    public void registerAvailabilityCallback(AvailabilityCallback availabilityCallback, Handler handler) {
        CameraManagerGlobal.get().registerAvailabilityCallback(availabilityCallback, CameraDeviceImpl.checkAndWrapHandler(handler));
    }

    public void registerAvailabilityCallback(Executor executor, AvailabilityCallback availabilityCallback) {
        if (executor == null) {
            throw new IllegalArgumentException("executor was null");
        }
        CameraManagerGlobal.get().registerAvailabilityCallback(availabilityCallback, executor);
    }

    public void unregisterAvailabilityCallback(AvailabilityCallback availabilityCallback) {
        CameraManagerGlobal.get().unregisterAvailabilityCallback(availabilityCallback);
    }

    public void registerTorchCallback(TorchCallback torchCallback, Handler handler) {
        CameraManagerGlobal.get().registerTorchCallback(torchCallback, CameraDeviceImpl.checkAndWrapHandler(handler));
    }

    public void registerTorchCallback(Executor executor, TorchCallback torchCallback) {
        if (executor == null) {
            throw new IllegalArgumentException("executor was null");
        }
        CameraManagerGlobal.get().registerTorchCallback(torchCallback, executor);
    }

    public void unregisterTorchCallback(TorchCallback torchCallback) {
        CameraManagerGlobal.get().unregisterTorchCallback(torchCallback);
    }

    public CameraCharacteristics getCameraCharacteristics(String str) throws CameraAccessException {
        CameraCharacteristics cameraCharacteristics;
        if (CameraManagerGlobal.sCameraServiceDisabled) {
            throw new IllegalArgumentException("No cameras available on device");
        }
        synchronized (this.mLock) {
            ICameraService cameraService = CameraManagerGlobal.get().getCameraService();
            if (cameraService == null) {
                throw new CameraAccessException(2, "Camera service is currently unavailable");
            }
            try {
                if (!supportsCamera2ApiLocked(str)) {
                    int i = Integer.parseInt(str);
                    cameraCharacteristics = LegacyMetadataMapper.createCharacteristics(cameraService.getLegacyParameters(i), cameraService.getCameraInfo(i));
                } else {
                    cameraCharacteristics = new CameraCharacteristics(cameraService.getCameraCharacteristics(str));
                }
            } catch (RemoteException e) {
                throw new CameraAccessException(2, "Camera service is currently unavailable", e);
            } catch (ServiceSpecificException e2) {
                throwAsPublicException(e2);
                cameraCharacteristics = null;
            }
        }
        return cameraCharacteristics;
    }

    private CameraDevice openCameraDeviceUserAsync(String str, CameraDevice.StateCallback stateCallback, Executor executor, int i) throws CameraAccessException {
        CameraDeviceImpl cameraDeviceImpl;
        ICameraDeviceUser iCameraDeviceUserConnectBinderShim;
        CameraCharacteristics cameraCharacteristics = getCameraCharacteristics(str);
        synchronized (this.mLock) {
            ICameraDeviceUser iCameraDeviceUser = null;
            cameraDeviceImpl = new CameraDeviceImpl(str, stateCallback, executor, cameraCharacteristics, this.mContext.getApplicationInfo().targetSdkVersion);
            CameraDeviceImpl.CameraDeviceCallbacks callbacks = cameraDeviceImpl.getCallbacks();
            try {
                try {
                    if (supportsCamera2ApiLocked(str)) {
                        ICameraService cameraService = CameraManagerGlobal.get().getCameraService();
                        if (cameraService == null) {
                            throw new ServiceSpecificException(4, "Camera service is currently unavailable");
                        }
                        iCameraDeviceUserConnectBinderShim = cameraService.connectDevice(callbacks, str, this.mContext.getOpPackageName(), i);
                    } else {
                        try {
                            int i2 = Integer.parseInt(str);
                            Log.i(TAG, "Using legacy camera HAL.");
                            iCameraDeviceUserConnectBinderShim = CameraDeviceUserShim.connectBinderShim(callbacks, i2);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Expected cameraId to be numeric, but it was: " + str);
                        }
                    }
                    iCameraDeviceUser = iCameraDeviceUserConnectBinderShim;
                } catch (RemoteException e2) {
                    ServiceSpecificException serviceSpecificException = new ServiceSpecificException(4, "Camera service is currently unavailable");
                    cameraDeviceImpl.setRemoteFailure(serviceSpecificException);
                    throwAsPublicException(serviceSpecificException);
                }
            } catch (ServiceSpecificException e3) {
                if (e3.errorCode == 9) {
                    throw new AssertionError("Should've gone down the shim path");
                }
                if (e3.errorCode == 7 || e3.errorCode == 8 || e3.errorCode == 6 || e3.errorCode == 4 || e3.errorCode == 10) {
                    cameraDeviceImpl.setRemoteFailure(e3);
                    if (e3.errorCode == 6 || e3.errorCode == 4 || e3.errorCode == 7) {
                        throwAsPublicException(e3);
                    }
                } else {
                    throwAsPublicException(e3);
                }
            }
            cameraDeviceImpl.setRemoteDevice(iCameraDeviceUser);
        }
        return cameraDeviceImpl;
    }

    public void openCamera(String str, CameraDevice.StateCallback stateCallback, Handler handler) throws CameraAccessException {
        openCameraForUid(str, stateCallback, CameraDeviceImpl.checkAndWrapHandler(handler), -1);
    }

    public void openCamera(String str, Executor executor, CameraDevice.StateCallback stateCallback) throws CameraAccessException {
        if (executor == null) {
            throw new IllegalArgumentException("executor was null");
        }
        openCameraForUid(str, stateCallback, executor, -1);
    }

    public void openCameraForUid(String str, CameraDevice.StateCallback stateCallback, Executor executor, int i) throws CameraAccessException {
        if (str == null) {
            throw new IllegalArgumentException("cameraId was null");
        }
        if (stateCallback == null) {
            throw new IllegalArgumentException("callback was null");
        }
        if (CameraManagerGlobal.sCameraServiceDisabled) {
            throw new IllegalArgumentException("No cameras available on device");
        }
        openCameraDeviceUserAsync(str, stateCallback, executor, i);
    }

    public void setTorchMode(String str, boolean z) throws CameraAccessException {
        if (CameraManagerGlobal.sCameraServiceDisabled) {
            throw new IllegalArgumentException("No cameras available on device");
        }
        CameraManagerGlobal.get().setTorchMode(str, z);
    }

    public static abstract class AvailabilityCallback {
        public void onCameraAvailable(String str) {
        }

        public void onCameraUnavailable(String str) {
        }
    }

    public static abstract class TorchCallback {
        public void onTorchModeUnavailable(String str) {
        }

        public void onTorchModeChanged(String str, boolean z) {
        }
    }

    public static void throwAsPublicException(Throwable th) throws CameraAccessException {
        int i = 2;
        if (th instanceof ServiceSpecificException) {
            ServiceSpecificException serviceSpecificException = (ServiceSpecificException) th;
            switch (serviceSpecificException.errorCode) {
                case 1:
                    throw new SecurityException(serviceSpecificException.getMessage(), serviceSpecificException);
                case 2:
                case 3:
                    throw new IllegalArgumentException(serviceSpecificException.getMessage(), serviceSpecificException);
                case 4:
                    break;
                case 5:
                default:
                    i = 3;
                    break;
                case 6:
                    i = 1;
                    break;
                case 7:
                    i = 4;
                    break;
                case 8:
                    i = 5;
                    break;
                case 9:
                    i = 1000;
                    break;
            }
            throw new CameraAccessException(i, serviceSpecificException.getMessage(), serviceSpecificException);
        }
        if (th instanceof DeadObjectException) {
            throw new CameraAccessException(2, "Camera service has died unexpectedly", th);
        }
        if (th instanceof RemoteException) {
            throw new UnsupportedOperationException("An unknown RemoteException was thrown which should never happen.", th);
        }
        if (th instanceof RuntimeException) {
            throw ((RuntimeException) th);
        }
    }

    private boolean supportsCamera2ApiLocked(String str) {
        return supportsCameraApiLocked(str, 2);
    }

    private boolean supportsCameraApiLocked(String str, int i) {
        try {
            ICameraService cameraService = CameraManagerGlobal.get().getCameraService();
            if (cameraService == null) {
                return false;
            }
            return cameraService.supportsCameraApi(str, i);
        } catch (RemoteException e) {
            return false;
        }
    }

    private static final class CameraManagerGlobal extends ICameraServiceListener.Stub implements IBinder.DeathRecipient {
        private static final String CAMERA_SERVICE_BINDER_NAME = "media.camera";
        private static final String TAG = "CameraManagerGlobal";
        private static final CameraManagerGlobal gCameraManager = new CameraManagerGlobal();
        public static final boolean sCameraServiceDisabled = SystemProperties.getBoolean("config.disable_cameraservice", false);
        private ICameraService mCameraService;
        private final boolean DEBUG = false;
        private final int CAMERA_SERVICE_RECONNECT_DELAY_MS = 1000;
        private final ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(1);
        private final ArrayMap<String, Integer> mDeviceStatus = new ArrayMap<>();
        private final ArrayMap<AvailabilityCallback, Executor> mCallbackMap = new ArrayMap<>();
        private Binder mTorchClientBinder = new Binder();
        private final ArrayMap<String, Integer> mTorchStatus = new ArrayMap<>();
        private final ArrayMap<TorchCallback, Executor> mTorchCallbackMap = new ArrayMap<>();
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
                if (this.mCameraService == null && !sCameraServiceDisabled) {
                    Log.e(TAG, "Camera service is unavailable");
                }
                iCameraService = this.mCameraService;
            }
            return iCameraService;
        }

        private void connectCameraServiceLocked() {
            if (this.mCameraService != null || sCameraServiceDisabled) {
                return;
            }
            Log.i(TAG, "Connecting to camera service");
            IBinder service = ServiceManager.getService(CAMERA_SERVICE_BINDER_NAME);
            if (service == null) {
                return;
            }
            try {
                service.linkToDeath(this, 0);
                ICameraService iCameraServiceAsInterface = ICameraService.Stub.asInterface(service);
                try {
                    CameraMetadataNative.setupGlobalVendorTagDescriptor();
                } catch (ServiceSpecificException e) {
                    handleRecoverableSetupErrors(e);
                }
                try {
                    for (CameraStatus cameraStatus : iCameraServiceAsInterface.addListener(this)) {
                        onStatusChangedLocked(cameraStatus.status, cameraStatus.cameraId);
                    }
                    this.mCameraService = iCameraServiceAsInterface;
                } catch (RemoteException e2) {
                } catch (ServiceSpecificException e3) {
                    throw new IllegalStateException("Failed to register a camera service listener", e3);
                }
            } catch (RemoteException e4) {
            }
        }

        public String[] getCameraIdList() {
            String[] strArr;
            synchronized (this.mLock) {
                connectCameraServiceLocked();
                int i = 0;
                for (int i2 = 0; i2 < this.mDeviceStatus.size(); i2++) {
                    int iIntValue = this.mDeviceStatus.valueAt(i2).intValue();
                    if (iIntValue != 0 && iIntValue != 2) {
                        i++;
                    }
                }
                strArr = new String[i];
                int i3 = 0;
                for (int i4 = 0; i4 < this.mDeviceStatus.size(); i4++) {
                    int iIntValue2 = this.mDeviceStatus.valueAt(i4).intValue();
                    if (iIntValue2 != 0 && iIntValue2 != 2) {
                        strArr[i3] = this.mDeviceStatus.keyAt(i4);
                        i3++;
                    }
                }
            }
            Arrays.sort(strArr, new Comparator<String>() {
                @Override
                public int compare(String str, String str2) {
                    int i5;
                    int i6;
                    try {
                        i5 = Integer.parseInt(str);
                    } catch (NumberFormatException e) {
                        i5 = -1;
                    }
                    try {
                        i6 = Integer.parseInt(str2);
                    } catch (NumberFormatException e2) {
                        i6 = -1;
                    }
                    if (i5 >= 0 && i6 >= 0) {
                        return i5 - i6;
                    }
                    if (i5 >= 0) {
                        return -1;
                    }
                    if (i6 >= 0) {
                        return 1;
                    }
                    return str.compareTo(str2);
                }
            });
            return strArr;
        }

        public void setTorchMode(String str, boolean z) throws CameraAccessException {
            synchronized (this.mLock) {
                if (str == null) {
                    throw new IllegalArgumentException("cameraId was null");
                }
                ICameraService cameraService = getCameraService();
                if (cameraService == null) {
                    throw new CameraAccessException(2, "Camera service is currently unavailable");
                }
                try {
                    try {
                        cameraService.setTorchMode(str, z, this.mTorchClientBinder);
                    } catch (RemoteException e) {
                        throw new CameraAccessException(2, "Camera service is currently unavailable");
                    }
                } catch (ServiceSpecificException e2) {
                    CameraManager.throwAsPublicException(e2);
                }
            }
        }

        private void handleRecoverableSetupErrors(ServiceSpecificException serviceSpecificException) {
            if (serviceSpecificException.errorCode == 4) {
                Log.w(TAG, serviceSpecificException.getMessage());
                return;
            }
            throw new IllegalStateException(serviceSpecificException);
        }

        private boolean isAvailable(int i) {
            if (i == 1) {
                return true;
            }
            return false;
        }

        private boolean validStatus(int i) {
            if (i != -2) {
                switch (i) {
                    case 0:
                    case 1:
                    case 2:
                        return true;
                    default:
                        return false;
                }
            }
            return true;
        }

        private boolean validTorchStatus(int i) {
            switch (i) {
                case 0:
                case 1:
                case 2:
                    return true;
                default:
                    return false;
            }
        }

        private void postSingleUpdate(final AvailabilityCallback availabilityCallback, Executor executor, final String str, int i) {
            long jClearCallingIdentity;
            if (isAvailable(i)) {
                jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            availabilityCallback.onCameraAvailable(str);
                        }
                    });
                } finally {
                }
            } else {
                jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            availabilityCallback.onCameraUnavailable(str);
                        }
                    });
                } finally {
                }
            }
        }

        private void postSingleTorchUpdate(final TorchCallback torchCallback, Executor executor, final String str, final int i) {
            long jClearCallingIdentity;
            switch (i) {
                case 1:
                case 2:
                    jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        executor.execute(new Runnable() {
                            @Override
                            public final void run() {
                                torchCallback.onTorchModeChanged(str, i == 2);
                            }
                        });
                        return;
                    } finally {
                    }
                default:
                    jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        executor.execute(new Runnable() {
                            @Override
                            public final void run() {
                                torchCallback.onTorchModeUnavailable(str);
                            }
                        });
                        return;
                    } finally {
                    }
            }
        }

        private void updateCallbackLocked(AvailabilityCallback availabilityCallback, Executor executor) {
            for (int i = 0; i < this.mDeviceStatus.size(); i++) {
                postSingleUpdate(availabilityCallback, executor, this.mDeviceStatus.keyAt(i), this.mDeviceStatus.valueAt(i).intValue());
            }
        }

        private void onStatusChangedLocked(int i, String str) {
            Integer numPut;
            if (!validStatus(i)) {
                Log.e(TAG, String.format("Ignoring invalid device %s status 0x%x", str, Integer.valueOf(i)));
                return;
            }
            if (i == 0) {
                numPut = this.mDeviceStatus.remove(str);
            } else {
                numPut = this.mDeviceStatus.put(str, Integer.valueOf(i));
            }
            if (numPut != null && numPut.intValue() == i) {
                return;
            }
            if (numPut != null && isAvailable(i) == isAvailable(numPut.intValue())) {
                return;
            }
            int size = this.mCallbackMap.size();
            for (int i2 = 0; i2 < size; i2++) {
                postSingleUpdate(this.mCallbackMap.keyAt(i2), this.mCallbackMap.valueAt(i2), str, i);
            }
        }

        private void updateTorchCallbackLocked(TorchCallback torchCallback, Executor executor) {
            for (int i = 0; i < this.mTorchStatus.size(); i++) {
                postSingleTorchUpdate(torchCallback, executor, this.mTorchStatus.keyAt(i), this.mTorchStatus.valueAt(i).intValue());
            }
        }

        private void onTorchStatusChangedLocked(int i, String str) {
            if (!validTorchStatus(i)) {
                Log.e(TAG, String.format("Ignoring invalid device %s torch status 0x%x", str, Integer.valueOf(i)));
                return;
            }
            Integer numPut = this.mTorchStatus.put(str, Integer.valueOf(i));
            if (numPut != null && numPut.intValue() == i) {
                return;
            }
            int size = this.mTorchCallbackMap.size();
            for (int i2 = 0; i2 < size; i2++) {
                postSingleTorchUpdate(this.mTorchCallbackMap.keyAt(i2), this.mTorchCallbackMap.valueAt(i2), str, i);
            }
        }

        public void registerAvailabilityCallback(AvailabilityCallback availabilityCallback, Executor executor) {
            synchronized (this.mLock) {
                connectCameraServiceLocked();
                if (this.mCallbackMap.put(availabilityCallback, executor) == null) {
                    updateCallbackLocked(availabilityCallback, executor);
                }
                if (this.mCameraService == null) {
                    scheduleCameraServiceReconnectionLocked();
                }
            }
        }

        public void unregisterAvailabilityCallback(AvailabilityCallback availabilityCallback) {
            synchronized (this.mLock) {
                this.mCallbackMap.remove(availabilityCallback);
            }
        }

        public void registerTorchCallback(TorchCallback torchCallback, Executor executor) {
            synchronized (this.mLock) {
                connectCameraServiceLocked();
                if (this.mTorchCallbackMap.put(torchCallback, executor) == null) {
                    updateTorchCallbackLocked(torchCallback, executor);
                }
                if (this.mCameraService == null) {
                    scheduleCameraServiceReconnectionLocked();
                }
            }
        }

        public void unregisterTorchCallback(TorchCallback torchCallback) {
            synchronized (this.mLock) {
                this.mTorchCallbackMap.remove(torchCallback);
            }
        }

        @Override
        public void onStatusChanged(int i, String str) throws RemoteException {
            synchronized (this.mLock) {
                onStatusChangedLocked(i, str);
            }
        }

        @Override
        public void onTorchStatusChanged(int i, String str) throws RemoteException {
            synchronized (this.mLock) {
                onTorchStatusChangedLocked(i, str);
            }
        }

        private void scheduleCameraServiceReconnectionLocked() {
            if (this.mCallbackMap.isEmpty() && this.mTorchCallbackMap.isEmpty()) {
                return;
            }
            try {
                this.mScheduler.schedule(new Runnable() {
                    @Override
                    public final void run() {
                        CameraManager.CameraManagerGlobal.lambda$scheduleCameraServiceReconnectionLocked$2(this.f$0);
                    }
                }, 1000L, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException e) {
                Log.e(TAG, "Failed to schedule camera service re-connect: " + e);
            }
        }

        public static void lambda$scheduleCameraServiceReconnectionLocked$2(CameraManagerGlobal cameraManagerGlobal) {
            if (cameraManagerGlobal.getCameraService() == null) {
                synchronized (cameraManagerGlobal.mLock) {
                    cameraManagerGlobal.scheduleCameraServiceReconnectionLocked();
                }
            }
        }

        @Override
        public void binderDied() {
            synchronized (this.mLock) {
                if (this.mCameraService == null) {
                    return;
                }
                this.mCameraService = null;
                for (int i = 0; i < this.mDeviceStatus.size(); i++) {
                    onStatusChangedLocked(0, this.mDeviceStatus.keyAt(i));
                }
                for (int i2 = 0; i2 < this.mTorchStatus.size(); i2++) {
                    onTorchStatusChangedLocked(0, this.mTorchStatus.keyAt(i2));
                }
                scheduleCameraServiceReconnectionLocked();
            }
        }
    }
}
