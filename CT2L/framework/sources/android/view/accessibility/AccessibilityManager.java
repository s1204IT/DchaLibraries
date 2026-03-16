package android.view.accessibility;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.view.IWindow;
import android.view.accessibility.IAccessibilityManager;
import android.view.accessibility.IAccessibilityManagerClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AccessibilityManager {
    public static final int DALTONIZER_CORRECT_DEUTERANOMALY = 12;
    public static final int DALTONIZER_DISABLED = -1;
    public static final int DALTONIZER_SIMULATE_MONOCHROMACY = 0;
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "AccessibilityManager";
    public static final int STATE_FLAG_ACCESSIBILITY_ENABLED = 1;
    public static final int STATE_FLAG_HIGH_TEXT_CONTRAST_ENABLED = 4;
    public static final int STATE_FLAG_TOUCH_EXPLORATION_ENABLED = 2;
    private static AccessibilityManager sInstance;
    static final Object sInstanceSync = new Object();
    final Handler mHandler;
    boolean mIsEnabled;
    boolean mIsHighTextContrastEnabled;
    boolean mIsTouchExplorationEnabled;
    private IAccessibilityManager mService;
    final int mUserId;
    private final Object mLock = new Object();
    private final CopyOnWriteArrayList<AccessibilityStateChangeListener> mAccessibilityStateChangeListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<TouchExplorationStateChangeListener> mTouchExplorationStateChangeListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<HighTextContrastChangeListener> mHighTextContrastStateChangeListeners = new CopyOnWriteArrayList<>();
    private final IAccessibilityManagerClient.Stub mClient = new IAccessibilityManagerClient.Stub() {
        @Override
        public void setState(int state) {
            AccessibilityManager.this.mHandler.obtainMessage(4, state, 0).sendToTarget();
        }
    };

    public interface AccessibilityStateChangeListener {
        void onAccessibilityStateChanged(boolean z);
    }

    public interface HighTextContrastChangeListener {
        void onHighTextContrastStateChanged(boolean z);
    }

    public interface TouchExplorationStateChangeListener {
        void onTouchExplorationStateChanged(boolean z);
    }

    public static AccessibilityManager getInstance(Context context) {
        int userId;
        synchronized (sInstanceSync) {
            if (sInstance == null) {
                if (Binder.getCallingUid() == 1000 || context.checkCallingOrSelfPermission(Manifest.permission.INTERACT_ACROSS_USERS) == 0 || context.checkCallingOrSelfPermission(Manifest.permission.INTERACT_ACROSS_USERS_FULL) == 0) {
                    userId = -2;
                } else {
                    userId = UserHandle.myUserId();
                }
                IBinder iBinder = ServiceManager.getService(Context.ACCESSIBILITY_SERVICE);
                IAccessibilityManager service = iBinder == null ? null : IAccessibilityManager.Stub.asInterface(iBinder);
                sInstance = new AccessibilityManager(context, service, userId);
            }
        }
        return sInstance;
    }

    public AccessibilityManager(Context context, IAccessibilityManager service, int userId) {
        this.mHandler = new MyHandler(context.getMainLooper());
        this.mService = service;
        this.mUserId = userId;
        synchronized (this.mLock) {
            tryConnectToServiceLocked();
        }
    }

    public IAccessibilityManagerClient getClient() {
        return this.mClient;
    }

    public boolean isEnabled() {
        boolean z;
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            z = service == null ? false : this.mIsEnabled;
        }
        return z;
    }

    public boolean isTouchExplorationEnabled() {
        boolean z;
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            z = service == null ? false : this.mIsTouchExplorationEnabled;
        }
        return z;
    }

    public boolean isHighTextContrastEnabled() {
        boolean z;
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            z = service == null ? false : this.mIsHighTextContrastEnabled;
        }
        return z;
    }

    public void sendAccessibilityEvent(AccessibilityEvent event) {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service != null) {
                if (!this.mIsEnabled) {
                    if (Process.myUid() == 1000) {
                        Log.e(LOG_TAG, "Ignore Accessibility is off pid=" + Process.myPid(), new Exception("system uid sendAccessibilityEvent when accessibility is off"));
                        return;
                    }
                    throw new IllegalStateException("Accessibility off. Did you forget to check that?");
                }
                int userId = this.mUserId;
                boolean doRecycle = false;
                try {
                    try {
                        event.setEventTime(SystemClock.uptimeMillis());
                        long identityToken = Binder.clearCallingIdentity();
                        doRecycle = service.sendAccessibilityEvent(event, userId);
                        Binder.restoreCallingIdentity(identityToken);
                        if (doRecycle) {
                            event.recycle();
                        }
                    } catch (RemoteException re) {
                        Log.e(LOG_TAG, "Error during sending " + event + " ", re);
                        if (doRecycle) {
                            event.recycle();
                        }
                    }
                } catch (Throwable th) {
                    if (doRecycle) {
                        event.recycle();
                    }
                    throw th;
                }
            }
        }
    }

    public void interrupt() {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service != null) {
                if (!this.mIsEnabled) {
                    throw new IllegalStateException("Accessibility off. Did you forget to check that?");
                }
                int userId = this.mUserId;
                try {
                    service.interrupt(userId);
                } catch (RemoteException re) {
                    Log.e(LOG_TAG, "Error while requesting interrupt from all services. ", re);
                }
            }
        }
    }

    @Deprecated
    public List<ServiceInfo> getAccessibilityServiceList() {
        List<AccessibilityServiceInfo> infos = getInstalledAccessibilityServiceList();
        List<ServiceInfo> services = new ArrayList<>();
        int infoCount = infos.size();
        for (int i = 0; i < infoCount; i++) {
            AccessibilityServiceInfo info = infos.get(i);
            services.add(info.getResolveInfo().serviceInfo);
        }
        return Collections.unmodifiableList(services);
    }

    public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList() {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
                return Collections.emptyList();
            }
            int userId = this.mUserId;
            List<AccessibilityServiceInfo> services = null;
            try {
                services = service.getInstalledAccessibilityServiceList(userId);
            } catch (RemoteException re) {
                Log.e(LOG_TAG, "Error while obtaining the installed AccessibilityServices. ", re);
            }
            if (services != null) {
                return Collections.unmodifiableList(services);
            }
            return Collections.emptyList();
        }
    }

    public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int feedbackTypeFlags) {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
                return Collections.emptyList();
            }
            int userId = this.mUserId;
            List<AccessibilityServiceInfo> services = null;
            try {
                services = service.getEnabledAccessibilityServiceList(feedbackTypeFlags, userId);
            } catch (RemoteException re) {
                Log.e(LOG_TAG, "Error while obtaining the installed AccessibilityServices. ", re);
            }
            if (services != null) {
                return Collections.unmodifiableList(services);
            }
            return Collections.emptyList();
        }
    }

    public boolean addAccessibilityStateChangeListener(AccessibilityStateChangeListener listener) {
        return this.mAccessibilityStateChangeListeners.add(listener);
    }

    public boolean removeAccessibilityStateChangeListener(AccessibilityStateChangeListener listener) {
        return this.mAccessibilityStateChangeListeners.remove(listener);
    }

    public boolean addTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener) {
        return this.mTouchExplorationStateChangeListeners.add(listener);
    }

    public boolean removeTouchExplorationStateChangeListener(TouchExplorationStateChangeListener listener) {
        return this.mTouchExplorationStateChangeListeners.remove(listener);
    }

    public boolean addHighTextContrastStateChangeListener(HighTextContrastChangeListener listener) {
        return this.mHighTextContrastStateChangeListeners.add(listener);
    }

    public boolean removeHighTextContrastStateChangeListener(HighTextContrastChangeListener listener) {
        return this.mHighTextContrastStateChangeListeners.remove(listener);
    }

    private void setStateLocked(int stateFlags) {
        boolean enabled = (stateFlags & 1) != 0;
        boolean touchExplorationEnabled = (stateFlags & 2) != 0;
        boolean highTextContrastEnabled = (stateFlags & 4) != 0;
        boolean wasEnabled = this.mIsEnabled;
        boolean wasTouchExplorationEnabled = this.mIsTouchExplorationEnabled;
        boolean wasHighTextContrastEnabled = this.mIsHighTextContrastEnabled;
        this.mIsEnabled = enabled;
        this.mIsTouchExplorationEnabled = touchExplorationEnabled;
        this.mIsHighTextContrastEnabled = highTextContrastEnabled;
        if (wasEnabled != enabled) {
            this.mHandler.sendEmptyMessage(1);
        }
        if (wasTouchExplorationEnabled != touchExplorationEnabled) {
            this.mHandler.sendEmptyMessage(2);
        }
        if (wasHighTextContrastEnabled != highTextContrastEnabled) {
            this.mHandler.sendEmptyMessage(3);
        }
    }

    public int addAccessibilityInteractionConnection(IWindow windowToken, IAccessibilityInteractionConnection connection) {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service == null) {
                return -1;
            }
            int userId = this.mUserId;
            try {
                return service.addAccessibilityInteractionConnection(windowToken, connection, userId);
            } catch (RemoteException re) {
                Log.e(LOG_TAG, "Error while adding an accessibility interaction connection. ", re);
                return -1;
            }
        }
    }

    public void removeAccessibilityInteractionConnection(IWindow windowToken) {
        synchronized (this.mLock) {
            IAccessibilityManager service = getServiceLocked();
            if (service != null) {
                try {
                    service.removeAccessibilityInteractionConnection(windowToken);
                } catch (RemoteException re) {
                    Log.e(LOG_TAG, "Error while removing an accessibility interaction connection. ", re);
                }
            }
        }
    }

    private IAccessibilityManager getServiceLocked() {
        if (this.mService == null) {
            tryConnectToServiceLocked();
        }
        return this.mService;
    }

    private void tryConnectToServiceLocked() {
        IBinder iBinder = ServiceManager.getService(Context.ACCESSIBILITY_SERVICE);
        if (iBinder != null) {
            IAccessibilityManager service = IAccessibilityManager.Stub.asInterface(iBinder);
            try {
                int stateFlags = service.addClient(this.mClient, this.mUserId);
                setStateLocked(stateFlags);
                this.mService = service;
            } catch (RemoteException re) {
                Log.e(LOG_TAG, "AccessibilityManagerService is dead", re);
            }
        }
    }

    private void handleNotifyAccessibilityStateChanged() {
        boolean isEnabled;
        synchronized (this.mLock) {
            isEnabled = this.mIsEnabled;
        }
        for (AccessibilityStateChangeListener listener : this.mAccessibilityStateChangeListeners) {
            listener.onAccessibilityStateChanged(isEnabled);
        }
    }

    private void handleNotifyTouchExplorationStateChanged() {
        boolean isTouchExplorationEnabled;
        synchronized (this.mLock) {
            isTouchExplorationEnabled = this.mIsTouchExplorationEnabled;
        }
        for (TouchExplorationStateChangeListener listener : this.mTouchExplorationStateChangeListeners) {
            listener.onTouchExplorationStateChanged(isTouchExplorationEnabled);
        }
    }

    private void handleNotifyHighTextContrastStateChanged() {
        boolean isHighTextContrastEnabled;
        synchronized (this.mLock) {
            isHighTextContrastEnabled = this.mIsHighTextContrastEnabled;
        }
        for (HighTextContrastChangeListener listener : this.mHighTextContrastStateChangeListeners) {
            listener.onHighTextContrastStateChanged(isHighTextContrastEnabled);
        }
    }

    private final class MyHandler extends Handler {
        public static final int MSG_NOTIFY_ACCESSIBILITY_STATE_CHANGED = 1;
        public static final int MSG_NOTIFY_EXPLORATION_STATE_CHANGED = 2;
        public static final int MSG_NOTIFY_HIGH_TEXT_CONTRAST_STATE_CHANGED = 3;
        public static final int MSG_SET_STATE = 4;

        public MyHandler(Looper looper) {
            super(looper, null, false);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    AccessibilityManager.this.handleNotifyAccessibilityStateChanged();
                    return;
                case 2:
                    AccessibilityManager.this.handleNotifyTouchExplorationStateChanged();
                    return;
                case 3:
                    AccessibilityManager.this.handleNotifyHighTextContrastStateChanged();
                    return;
                case 4:
                    int state = message.arg1;
                    synchronized (AccessibilityManager.this.mLock) {
                        AccessibilityManager.this.setStateLocked(state);
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
    }
}
