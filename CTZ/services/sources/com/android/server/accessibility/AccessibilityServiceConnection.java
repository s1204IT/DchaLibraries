package com.android.server.accessibility;

import android.R;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.accessibility.AbstractAccessibilityServiceConnection;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.wm.WindowManagerInternal;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.function.Consumer;

class AccessibilityServiceConnection extends AbstractAccessibilityServiceConnection {
    private static final String LOG_TAG = "AccessibilityServiceConnection";
    final Intent mIntent;
    private final Handler mMainHandler;
    final WeakReference<AccessibilityManagerService.UserState> mUserStateWeakReference;
    private boolean mWasConnectedAndDied;

    public AccessibilityServiceConnection(AccessibilityManagerService.UserState userState, Context context, ComponentName componentName, AccessibilityServiceInfo accessibilityServiceInfo, int i, Handler handler, Object obj, AccessibilityManagerService.SecurityPolicy securityPolicy, AbstractAccessibilityServiceConnection.SystemSupport systemSupport, WindowManagerInternal windowManagerInternal, GlobalActionPerformer globalActionPerformer) {
        super(context, componentName, accessibilityServiceInfo, i, handler, obj, securityPolicy, systemSupport, windowManagerInternal, globalActionPerformer);
        this.mUserStateWeakReference = new WeakReference<>(userState);
        this.mIntent = new Intent().setComponent(this.mComponentName);
        this.mMainHandler = handler;
        this.mIntent.putExtra("android.intent.extra.client_label", R.string.config_devicePolicyManagement);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (BenesseExtension.getDchaState() == 0) {
                this.mIntent.putExtra("android.intent.extra.client_intent", this.mSystemSupport.getPendingIntentActivity(this.mContext, 0, new Intent("android.settings.ACCESSIBILITY_SETTINGS"), 0));
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void bindLocked() {
        AccessibilityManagerService.UserState userState = this.mUserStateWeakReference.get();
        if (userState == null) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        int i = 33554433;
        try {
            if (userState.mBindInstantServiceAllowed) {
                i = 37748737;
            }
            if (this.mService == null && this.mContext.bindServiceAsUser(this.mIntent, this, i, new UserHandle(userState.mUserId))) {
                userState.getBindingServicesLocked().add(this.mComponentName);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void unbindLocked() {
        this.mContext.unbindService(this);
        AccessibilityManagerService.UserState userState = this.mUserStateWeakReference.get();
        if (userState == null) {
            return;
        }
        userState.removeServiceLocked(this);
        resetLocked();
    }

    public boolean canRetrieveInteractiveWindowsLocked() {
        return this.mSecurityPolicy.canRetrieveWindowContentLocked(this) && this.mRetrieveInteractiveWindows;
    }

    public void disableSelf() {
        synchronized (this.mLock) {
            AccessibilityManagerService.UserState userState = this.mUserStateWeakReference.get();
            if (userState == null) {
                return;
            }
            if (userState.mEnabledServices.remove(this.mComponentName)) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mSystemSupport.persistComponentNamesToSettingLocked("enabled_accessibility_services", userState.mEnabledServices, userState.mUserId);
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    this.mSystemSupport.onClientChange(false);
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        synchronized (this.mLock) {
            if (this.mService != iBinder) {
                if (this.mService != null) {
                    this.mService.unlinkToDeath(this, 0);
                }
                this.mService = iBinder;
                try {
                    this.mService.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    Slog.e(LOG_TAG, "Failed registering death link");
                    binderDied();
                    return;
                }
            }
            this.mServiceInterface = IAccessibilityServiceClient.Stub.asInterface(iBinder);
            AccessibilityManagerService.UserState userState = this.mUserStateWeakReference.get();
            if (userState == null) {
                return;
            }
            userState.addServiceLocked(this);
            this.mSystemSupport.onClientChange(false);
            this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((AccessibilityServiceConnection) obj).initializeService();
                }
            }, this));
        }
    }

    @Override
    public AccessibilityServiceInfo getServiceInfo() {
        this.mAccessibilityServiceInfo.crashed = this.mWasConnectedAndDied;
        return this.mAccessibilityServiceInfo;
    }

    private void initializeService() {
        IAccessibilityServiceClient iAccessibilityServiceClient;
        synchronized (this.mLock) {
            AccessibilityManagerService.UserState userState = this.mUserStateWeakReference.get();
            if (userState == null) {
                return;
            }
            Set<ComponentName> bindingServicesLocked = userState.getBindingServicesLocked();
            if (bindingServicesLocked.contains(this.mComponentName) || this.mWasConnectedAndDied) {
                bindingServicesLocked.remove(this.mComponentName);
                this.mWasConnectedAndDied = false;
                iAccessibilityServiceClient = this.mServiceInterface;
            } else {
                iAccessibilityServiceClient = null;
            }
            if (iAccessibilityServiceClient == null) {
                binderDied();
                return;
            }
            try {
                iAccessibilityServiceClient.init(this, this.mId, this.mOverlayWindowToken);
            } catch (RemoteException e) {
                Slog.w(LOG_TAG, "Error while setting connection for service: " + iAccessibilityServiceClient, e);
                binderDied();
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        binderDied();
    }

    @Override
    protected boolean isCalledForCurrentUserLocked() {
        return this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(-2) == this.mSystemSupport.getCurrentUserIdLocked();
    }

    public boolean setSoftKeyboardShowMode(int i) {
        ComponentName componentName;
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return false;
            }
            AccessibilityManagerService.UserState userState = this.mUserStateWeakReference.get();
            if (userState == null) {
                return false;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            if (i == 0) {
                componentName = null;
            } else {
                try {
                    componentName = this.mComponentName;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
            userState.mServiceChangingSoftKeyboardMode = componentName;
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "accessibility_soft_keyboard_mode", i, userState.mUserId);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return true;
        }
    }

    public boolean isAccessibilityButtonAvailable() {
        synchronized (this.mLock) {
            boolean z = false;
            if (!isCalledForCurrentUserLocked()) {
                return false;
            }
            AccessibilityManagerService.UserState userState = this.mUserStateWeakReference.get();
            if (userState != null && isAccessibilityButtonAvailableLocked(userState)) {
                z = true;
            }
            return z;
        }
    }

    @Override
    public void binderDied() {
        synchronized (this.mLock) {
            if (isConnectedLocked()) {
                this.mWasConnectedAndDied = true;
                resetLocked();
                if (this.mId == this.mSystemSupport.getMagnificationController().getIdOfLastServiceToMagnify()) {
                    this.mSystemSupport.getMagnificationController().resetIfNeeded(true);
                }
                this.mSystemSupport.onClientChange(false);
            }
        }
    }

    public boolean isAccessibilityButtonAvailableLocked(AccessibilityManagerService.UserState userState) {
        int i = 0;
        if (!this.mRequestAccessibilityButton || !this.mSystemSupport.isAccessibilityButtonShown()) {
            return false;
        }
        if (userState.mIsNavBarMagnificationEnabled && userState.mIsNavBarMagnificationAssignedToAccessibilityButton) {
            return false;
        }
        for (int size = userState.mBoundServices.size() - 1; size >= 0; size--) {
            if (userState.mBoundServices.get(size).mRequestAccessibilityButton) {
                i++;
            }
        }
        if (i == 1 || userState.mServiceAssignedToAccessibilityButton == null) {
            return true;
        }
        return this.mComponentName.equals(userState.mServiceAssignedToAccessibilityButton);
    }

    @Override
    public boolean isCapturingFingerprintGestures() {
        return this.mServiceInterface != null && this.mSecurityPolicy.canCaptureFingerprintGestures(this) && this.mCaptureFingerprintGestures;
    }

    @Override
    public void onFingerprintGestureDetectionActiveChanged(boolean z) {
        IAccessibilityServiceClient iAccessibilityServiceClient;
        if (!isCapturingFingerprintGestures()) {
            return;
        }
        synchronized (this.mLock) {
            iAccessibilityServiceClient = this.mServiceInterface;
        }
        if (iAccessibilityServiceClient != null) {
            try {
                this.mServiceInterface.onFingerprintCapturingGesturesChanged(z);
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public void onFingerprintGesture(int i) {
        IAccessibilityServiceClient iAccessibilityServiceClient;
        if (!isCapturingFingerprintGestures()) {
            return;
        }
        synchronized (this.mLock) {
            iAccessibilityServiceClient = this.mServiceInterface;
        }
        if (iAccessibilityServiceClient != null) {
            try {
                this.mServiceInterface.onFingerprintGesture(i);
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public void sendGesture(int i, ParceledListSlice parceledListSlice) {
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.canPerformGestures(this)) {
                MotionEventInjector motionEventInjectorLocked = this.mSystemSupport.getMotionEventInjectorLocked();
                if (motionEventInjectorLocked != null) {
                    motionEventInjectorLocked.injectEvents(parceledListSlice.getList(), this.mServiceInterface, i);
                } else {
                    try {
                        this.mServiceInterface.onPerformGestureResult(i, false);
                    } catch (RemoteException e) {
                        Slog.e(LOG_TAG, "Error sending motion event injection failure to " + this.mServiceInterface, e);
                    }
                }
            }
        }
    }
}
