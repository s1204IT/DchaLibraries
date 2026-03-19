package com.android.server.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.ICameraService;
import android.hardware.ICameraServiceProxy;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.nfc.IAppCallback;
import android.nfc.INfcAdapter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import java.util.Collection;
import java.util.Set;

public class CameraService extends SystemService implements Handler.Callback, IBinder.DeathRecipient {
    private static final String ACTION_SHUTDOWN_IPO = "android.intent.action.ACTION_SHUTDOWN_IPO";
    private static final String CAMERA_SERVICE_BINDER_NAME = "media.camera";
    public static final String CAMERA_SERVICE_PROXY_BINDER_NAME = "media.camera.proxy";
    public static final int CAMERA_STATE_ACTIVE = 1;
    public static final int CAMERA_STATE_CLOSED = 3;
    public static final int CAMERA_STATE_IDLE = 2;
    public static final int CAMERA_STATE_OPEN = 0;
    private static final boolean DEBUG = false;
    public static final int DISABLE_POLLING_FLAGS = 4096;
    public static final int ENABLE_POLLING_FLAGS = 0;
    private static final int MSG_SWITCH_USER = 1;
    private static final String NFC_NOTIFICATION_PROP = "ro.camera.notify_nfc";
    private static final String NFC_SERVICE_BINDER_NAME = "nfc";
    private static final int RETRY_DELAY_TIME = 20;
    private static final String TAG = "CameraService_proxy";
    private static final IBinder nfcInterfaceToken = new Binder();
    private int mActiveCameraCount;
    private final ArraySet<String> mActiveCameraIds;
    private final ICameraServiceProxy.Stub mCameraServiceProxy;
    private ICameraService mCameraServiceRaw;
    private final Context mContext;
    private Set<Integer> mEnabledCameraUsers;
    private final Handler mHandler;
    private final ServiceThread mHandlerThread;
    private final BroadcastReceiver mIntentReceiver;
    private int mLastUser;
    private final Object mLock;
    private final boolean mNotifyNfc;
    private UserManager mUserManager;

    public CameraService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mActiveCameraIds = new ArraySet<>();
        this.mActiveCameraCount = 0;
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                CameraService.this.processCustomBroadcastActions(action);
                if (!action.equals("android.intent.action.USER_ADDED") && !action.equals("android.intent.action.USER_REMOVED") && !action.equals("android.intent.action.USER_INFO_CHANGED") && !action.equals("android.intent.action.MANAGED_PROFILE_ADDED") && !action.equals("android.intent.action.MANAGED_PROFILE_REMOVED")) {
                    return;
                }
                synchronized (CameraService.this.mLock) {
                    if (CameraService.this.mEnabledCameraUsers == null) {
                        return;
                    }
                    CameraService.this.switchUserLocked(CameraService.this.mLastUser);
                }
            }
        };
        this.mCameraServiceProxy = new ICameraServiceProxy.Stub() {
            public void pingForUserUpdate() {
                CameraService.this.notifySwitchWithRetries(30);
            }

            public void notifyCameraState(String cameraId, int newCameraState) {
                CameraService.cameraStateToString(newCameraState);
                CameraService.this.updateActivityCount(cameraId, newCameraState);
            }
        };
        this.mContext = context;
        this.mHandlerThread = new ServiceThread(TAG, -4, false);
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper(), this);
        this.mNotifyNfc = SystemProperties.getInt(NFC_NOTIFICATION_PROP, 0) > 0;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                notifySwitchWithRetries(msg.arg1);
                break;
            default:
                Slog.e(TAG, "CameraService error, invalid message: " + msg.what);
                break;
        }
        return true;
    }

    @Override
    public void onStart() {
        this.mUserManager = UserManager.get(this.mContext);
        if (this.mUserManager == null) {
            throw new IllegalStateException("UserManagerService must start before CameraService!");
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_INFO_CHANGED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        addCustomActionsToFilter(filter);
        this.mContext.registerReceiver(this.mIntentReceiver, filter);
        publishBinderService(CAMERA_SERVICE_PROXY_BINDER_NAME, this.mCameraServiceProxy);
    }

    @Override
    public void onStartUser(int userHandle) {
        synchronized (this.mLock) {
            if (this.mEnabledCameraUsers == null) {
                switchUserLocked(userHandle);
            }
        }
    }

    @Override
    public void onSwitchUser(int userHandle) {
        synchronized (this.mLock) {
            switchUserLocked(userHandle);
        }
    }

    @Override
    public void binderDied() {
        synchronized (this.mLock) {
            this.mCameraServiceRaw = null;
            boolean wasEmpty = this.mActiveCameraIds.isEmpty();
            this.mActiveCameraIds.clear();
            if (this.mNotifyNfc && !wasEmpty) {
                notifyNfcService(true);
            }
        }
    }

    private void switchUserLocked(int userHandle) {
        Set<Integer> currentUserHandles = getEnabledUserHandles(userHandle);
        this.mLastUser = userHandle;
        if (this.mEnabledCameraUsers != null && this.mEnabledCameraUsers.equals(currentUserHandles)) {
            return;
        }
        this.mEnabledCameraUsers = currentUserHandles;
        notifyMediaserverLocked(1, currentUserHandles);
    }

    private Set<Integer> getEnabledUserHandles(int currentUserHandle) {
        int[] userProfiles = this.mUserManager.getEnabledProfileIds(currentUserHandle);
        Set<Integer> handles = new ArraySet<>(userProfiles.length);
        for (int id : userProfiles) {
            handles.add(Integer.valueOf(id));
        }
        return handles;
    }

    private void notifySwitchWithRetries(int retries) {
        synchronized (this.mLock) {
            if (this.mEnabledCameraUsers == null) {
                return;
            }
            if (notifyMediaserverLocked(1, this.mEnabledCameraUsers)) {
                retries = 0;
            }
            if (retries <= 0) {
                return;
            }
            Slog.i(TAG, "Could not notify camera service of user switch, retrying...");
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, retries - 1, 0, null), 20L);
        }
    }

    private boolean notifyMediaserverLocked(int eventType, Set<Integer> updatedUserHandles) {
        if (this.mCameraServiceRaw == null) {
            IBinder cameraServiceBinder = getBinderService(CAMERA_SERVICE_BINDER_NAME);
            if (cameraServiceBinder == null) {
                Slog.w(TAG, "Could not notify mediaserver, camera service not available.");
                return false;
            }
            try {
                cameraServiceBinder.linkToDeath(this, 0);
                this.mCameraServiceRaw = ICameraService.Stub.asInterface(cameraServiceBinder);
            } catch (RemoteException e) {
                Slog.w(TAG, "Could not link to death of native camera service");
                return false;
            }
        }
        try {
            this.mCameraServiceRaw.notifySystemEvent(eventType, toArray(updatedUserHandles));
            return true;
        } catch (RemoteException e2) {
            Slog.w(TAG, "Could not notify mediaserver, remote exception: " + e2);
            return false;
        }
    }

    private void updateActivityCount(String cameraId, int newCameraState) {
        synchronized (this.mLock) {
            boolean wasEmpty = this.mActiveCameraIds.isEmpty();
            switch (newCameraState) {
                case 1:
                    this.mActiveCameraIds.add(cameraId);
                    break;
                case 2:
                case 3:
                    this.mActiveCameraIds.remove(cameraId);
                    break;
            }
            boolean isEmpty = this.mActiveCameraIds.isEmpty();
            if (this.mNotifyNfc && wasEmpty != isEmpty) {
                notifyNfcService(isEmpty);
            }
        }
    }

    private void notifyNfcService(boolean enablePolling) {
        IBinder nfcServiceBinder = getBinderService(NFC_SERVICE_BINDER_NAME);
        if (nfcServiceBinder == null) {
            Slog.w(TAG, "Could not connect to NFC service to notify it of camera state");
            return;
        }
        INfcAdapter nfcAdapterRaw = INfcAdapter.Stub.asInterface(nfcServiceBinder);
        int flags = enablePolling ? 0 : 4096;
        try {
            nfcAdapterRaw.setReaderMode(nfcInterfaceToken, (IAppCallback) null, flags, (Bundle) null);
        } catch (RemoteException e) {
            Slog.w(TAG, "Could not notify NFC service, remote exception: " + e);
        }
    }

    private static int[] toArray(Collection<Integer> c) {
        int len = c.size();
        int[] ret = new int[len];
        int idx = 0;
        for (Integer i : c) {
            ret[idx] = i.intValue();
            idx++;
        }
        return ret;
    }

    private static String cameraStateToString(int newCameraState) {
        switch (newCameraState) {
            case 0:
                return "CAMERA_STATE_OPEN";
            case 1:
                return "CAMERA_STATE_ACTIVE";
            case 2:
                return "CAMERA_STATE_IDLE";
            case 3:
                return "CAMERA_STATE_CLOSED";
            default:
                return "CAMERA_STATE_UNKNOWN";
        }
    }

    private void addCustomActionsToFilter(IntentFilter filter) {
        filter.addAction("android.intent.action.ACTION_SHUTDOWN");
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
    }

    private void processCustomBroadcastActions(String action) {
        if (!action.equals("android.intent.action.ACTION_SHUTDOWN") && !action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
            return;
        }
        synchronized (this.mLock) {
            closeAllFlash();
        }
    }

    private void closeAllFlash() {
        try {
            CameraManager cameraManager = (CameraManager) this.mContext.getSystemService("camera");
            String[] cameraIdList = cameraManager.getCameraIdList();
            for (String cameraId : cameraIdList) {
                cameraManager.setTorchMode(cameraId, false);
            }
        } catch (CameraAccessException e) {
            Slog.w(TAG, "Could not close flash, camera access exception: " + e);
        } catch (IllegalArgumentException e2) {
            Slog.w(TAG, "Could not close flash, IllegalArgument exception: " + e2);
        }
    }
}
