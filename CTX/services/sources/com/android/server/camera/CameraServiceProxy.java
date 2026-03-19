package com.android.server.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.ICameraService;
import android.hardware.ICameraServiceProxy;
import android.metrics.LogMaker;
import android.nfc.IAppCallback;
import android.nfc.INfcAdapter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import com.android.server.ServiceThread;
import com.android.server.SystemService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CameraServiceProxy extends SystemService implements Handler.Callback, IBinder.DeathRecipient {
    private static final String CAMERA_SERVICE_BINDER_NAME = "media.camera";
    public static final String CAMERA_SERVICE_PROXY_BINDER_NAME = "media.camera.proxy";
    private static final boolean DEBUG = false;
    public static final int DISABLE_POLLING_FLAGS = 4096;
    public static final int ENABLE_POLLING_FLAGS = 0;
    private static final int MAX_USAGE_HISTORY = 100;
    private static final int MSG_SWITCH_USER = 1;
    private static final String NFC_NOTIFICATION_PROP = "ro.camera.notify_nfc";
    private static final String NFC_SERVICE_BINDER_NAME = "nfc";
    private static final int RETRY_DELAY_TIME = 20;
    private static final String TAG = "CameraService_proxy";
    private static final IBinder nfcInterfaceToken = new Binder();
    private final ArrayMap<String, CameraUsageEvent> mActiveCameraUsage;
    private final ICameraServiceProxy.Stub mCameraServiceProxy;
    private ICameraService mCameraServiceRaw;
    private final List<CameraUsageEvent> mCameraUsageHistory;
    private final Context mContext;
    private Set<Integer> mEnabledCameraUsers;
    private final Handler mHandler;
    private final ServiceThread mHandlerThread;
    private final BroadcastReceiver mIntentReceiver;
    private int mLastUser;
    private final Object mLock;
    private final MetricsLogger mLogger;
    private final boolean mNotifyNfc;
    private UserManager mUserManager;

    private static class CameraUsageEvent {
        public final int mAPILevel;
        public final int mCameraFacing;
        public final String mClientName;
        private long mDurationOrStartTimeMs = SystemClock.elapsedRealtime();
        private boolean mCompleted = false;

        public CameraUsageEvent(int i, String str, int i2) {
            this.mCameraFacing = i;
            this.mClientName = str;
            this.mAPILevel = i2;
        }

        public void markCompleted() {
            if (this.mCompleted) {
                return;
            }
            this.mCompleted = true;
            this.mDurationOrStartTimeMs = SystemClock.elapsedRealtime() - this.mDurationOrStartTimeMs;
        }

        public long getDuration() {
            if (this.mCompleted) {
                return this.mDurationOrStartTimeMs;
            }
            return 0L;
        }
    }

    public CameraServiceProxy(Context context) {
        super(context);
        this.mLock = new Object();
        this.mActiveCameraUsage = new ArrayMap<>();
        this.mCameraUsageHistory = new ArrayList();
        this.mLogger = new MetricsLogger();
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                switch (action) {
                    case "android.intent.action.USER_ADDED":
                    case "android.intent.action.USER_REMOVED":
                    case "android.intent.action.USER_INFO_CHANGED":
                    case "android.intent.action.MANAGED_PROFILE_ADDED":
                    case "android.intent.action.MANAGED_PROFILE_REMOVED":
                        synchronized (CameraServiceProxy.this.mLock) {
                            if (CameraServiceProxy.this.mEnabledCameraUsers == null) {
                                return;
                            }
                            CameraServiceProxy.this.switchUserLocked(CameraServiceProxy.this.mLastUser);
                            return;
                        }
                    default:
                        return;
                }
            }
        };
        this.mCameraServiceProxy = new ICameraServiceProxy.Stub() {
            public void pingForUserUpdate() {
                if (Binder.getCallingUid() == 1047) {
                    CameraServiceProxy.this.notifySwitchWithRetries(30);
                    return;
                }
                Slog.e(CameraServiceProxy.TAG, "Calling UID: " + Binder.getCallingUid() + " doesn't match expected  camera service UID!");
            }

            public void notifyCameraState(String str, int i, int i2, String str2, int i3) {
                if (Binder.getCallingUid() == 1047) {
                    CameraServiceProxy.cameraStateToString(i);
                    CameraServiceProxy.cameraFacingToString(i2);
                    CameraServiceProxy.this.updateActivityCount(str, i, i2, str2, i3);
                } else {
                    Slog.e(CameraServiceProxy.TAG, "Calling UID: " + Binder.getCallingUid() + " doesn't match expected  camera service UID!");
                }
            }
        };
        this.mContext = context;
        this.mHandlerThread = new ServiceThread(TAG, -4, false);
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper(), this);
        this.mNotifyNfc = SystemProperties.getInt(NFC_NOTIFICATION_PROP, 0) > 0;
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message.what == 1) {
            notifySwitchWithRetries(message.arg1);
        } else {
            Slog.e(TAG, "CameraServiceProxy error, invalid message: " + message.what);
        }
        return true;
    }

    @Override
    public void onStart() {
        this.mUserManager = UserManager.get(this.mContext);
        if (this.mUserManager == null) {
            throw new IllegalStateException("UserManagerService must start before CameraServiceProxy!");
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_INFO_CHANGED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        this.mContext.registerReceiver(this.mIntentReceiver, intentFilter);
        publishBinderService(CAMERA_SERVICE_PROXY_BINDER_NAME, this.mCameraServiceProxy);
        publishLocalService(CameraServiceProxy.class, this);
        CameraStatsJobService.schedule(this.mContext);
    }

    @Override
    public void onStartUser(int i) {
        synchronized (this.mLock) {
            if (this.mEnabledCameraUsers == null) {
                switchUserLocked(i);
            }
        }
    }

    @Override
    public void onSwitchUser(int i) {
        synchronized (this.mLock) {
            switchUserLocked(i);
        }
    }

    @Override
    public void binderDied() {
        synchronized (this.mLock) {
            this.mCameraServiceRaw = null;
            boolean zIsEmpty = this.mActiveCameraUsage.isEmpty();
            this.mActiveCameraUsage.clear();
            if (this.mNotifyNfc && !zIsEmpty) {
                notifyNfcService(true);
            }
        }
    }

    void dumpUsageEvents() {
        int i;
        synchronized (this.mLock) {
            Collections.shuffle(this.mCameraUsageHistory);
            for (CameraUsageEvent cameraUsageEvent : this.mCameraUsageHistory) {
                switch (cameraUsageEvent.mCameraFacing) {
                    case 0:
                        i = 0;
                        break;
                    case 1:
                        i = 1;
                        break;
                    case 2:
                        i = 2;
                        break;
                    default:
                        continue;
                }
                this.mLogger.write(new LogMaker(1032).setType(4).setSubtype(i).setLatency(cameraUsageEvent.getDuration()).addTaggedData(1322, Integer.valueOf(cameraUsageEvent.mAPILevel)).setPackageName(cameraUsageEvent.mClientName));
            }
            this.mCameraUsageHistory.clear();
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            CameraStatsJobService.schedule(this.mContext);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void switchUserLocked(int i) {
        Set<Integer> enabledUserHandles = getEnabledUserHandles(i);
        this.mLastUser = i;
        if (this.mEnabledCameraUsers == null || !this.mEnabledCameraUsers.equals(enabledUserHandles)) {
            this.mEnabledCameraUsers = enabledUserHandles;
            notifyMediaserverLocked(1, enabledUserHandles);
        }
    }

    private Set<Integer> getEnabledUserHandles(int i) {
        int[] enabledProfileIds = this.mUserManager.getEnabledProfileIds(i);
        ArraySet arraySet = new ArraySet(enabledProfileIds.length);
        for (int i2 : enabledProfileIds) {
            arraySet.add(Integer.valueOf(i2));
        }
        return arraySet;
    }

    private void notifySwitchWithRetries(int i) {
        synchronized (this.mLock) {
            if (this.mEnabledCameraUsers == null) {
                return;
            }
            if (notifyMediaserverLocked(1, this.mEnabledCameraUsers)) {
                i = 0;
            }
            if (i <= 0) {
                return;
            }
            Slog.i(TAG, "Could not notify camera service of user switch, retrying...");
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, i - 1, 0, null), 20L);
        }
    }

    private boolean notifyMediaserverLocked(int i, Set<Integer> set) {
        if (this.mCameraServiceRaw == null) {
            IBinder binderService = getBinderService(CAMERA_SERVICE_BINDER_NAME);
            if (binderService == null) {
                Slog.w(TAG, "Could not notify mediaserver, camera service not available.");
                return false;
            }
            try {
                binderService.linkToDeath(this, 0);
                this.mCameraServiceRaw = ICameraService.Stub.asInterface(binderService);
            } catch (RemoteException e) {
                Slog.w(TAG, "Could not link to death of native camera service");
                return false;
            }
        }
        try {
            this.mCameraServiceRaw.notifySystemEvent(i, toArray(set));
            return true;
        } catch (RemoteException e2) {
            Slog.w(TAG, "Could not notify mediaserver, remote exception: " + e2);
            return false;
        }
    }

    private void updateActivityCount(String str, int i, int i2, String str2, int i3) {
        synchronized (this.mLock) {
            boolean zIsEmpty = this.mActiveCameraUsage.isEmpty();
            switch (i) {
                case 1:
                    CameraUsageEvent cameraUsageEventPut = this.mActiveCameraUsage.put(str, new CameraUsageEvent(i2, str2, i3));
                    if (cameraUsageEventPut != null) {
                        Slog.w(TAG, "Camera " + str + " was already marked as active");
                        cameraUsageEventPut.markCompleted();
                        this.mCameraUsageHistory.add(cameraUsageEventPut);
                    }
                    break;
                case 2:
                case 3:
                    CameraUsageEvent cameraUsageEventRemove = this.mActiveCameraUsage.remove(str);
                    if (cameraUsageEventRemove != null) {
                        cameraUsageEventRemove.markCompleted();
                        this.mCameraUsageHistory.add(cameraUsageEventRemove);
                        if (this.mCameraUsageHistory.size() > 100) {
                            dumpUsageEvents();
                        }
                    }
                    break;
            }
            boolean zIsEmpty2 = this.mActiveCameraUsage.isEmpty();
            if (this.mNotifyNfc && zIsEmpty != zIsEmpty2) {
                notifyNfcService(zIsEmpty2);
            }
        }
    }

    private void notifyNfcService(boolean z) {
        IBinder binderService = getBinderService(NFC_SERVICE_BINDER_NAME);
        if (binderService == null) {
            Slog.w(TAG, "Could not connect to NFC service to notify it of camera state");
            return;
        }
        try {
            INfcAdapter.Stub.asInterface(binderService).setReaderMode(nfcInterfaceToken, (IAppCallback) null, z ? 0 : 4096, (Bundle) null);
        } catch (RemoteException e) {
            Slog.w(TAG, "Could not notify NFC service, remote exception: " + e);
        }
    }

    private static int[] toArray(Collection<Integer> collection) {
        int[] iArr = new int[collection.size()];
        Iterator<Integer> it = collection.iterator();
        int i = 0;
        while (it.hasNext()) {
            iArr[i] = it.next().intValue();
            i++;
        }
        return iArr;
    }

    private static String cameraStateToString(int i) {
        switch (i) {
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

    private static String cameraFacingToString(int i) {
        switch (i) {
            case 0:
                return "CAMERA_FACING_BACK";
            case 1:
                return "CAMERA_FACING_FRONT";
            case 2:
                return "CAMERA_FACING_EXTERNAL";
            default:
                return "CAMERA_FACING_UNKNOWN";
        }
    }
}
