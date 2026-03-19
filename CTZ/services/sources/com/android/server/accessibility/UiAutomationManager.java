package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import android.view.accessibility.AccessibilityEvent;
import com.android.internal.util.DumpUtils;
import com.android.server.accessibility.AbstractAccessibilityServiceConnection;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.accessibility.UiAutomationManager;
import com.android.server.wm.WindowManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;

class UiAutomationManager {
    private static final ComponentName COMPONENT_NAME = new ComponentName("com.android.server.accessibility", "UiAutomation");
    private static final String LOG_TAG = "UiAutomationManager";
    private AbstractAccessibilityServiceConnection.SystemSupport mSystemSupport;
    private int mUiAutomationFlags;
    private UiAutomationService mUiAutomationService;
    private AccessibilityServiceInfo mUiAutomationServiceInfo;
    private IBinder mUiAutomationServiceOwner;
    private final IBinder.DeathRecipient mUiAutomationServiceOwnerDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            UiAutomationManager.this.mUiAutomationServiceOwner.unlinkToDeath(this, 0);
            UiAutomationManager.this.mUiAutomationServiceOwner = null;
            if (UiAutomationManager.this.mUiAutomationService != null) {
                UiAutomationManager.this.destroyUiAutomationService();
            }
        }
    };

    UiAutomationManager() {
    }

    void registerUiTestAutomationServiceLocked(IBinder iBinder, IAccessibilityServiceClient iAccessibilityServiceClient, Context context, AccessibilityServiceInfo accessibilityServiceInfo, int i, Handler handler, Object obj, AccessibilityManagerService.SecurityPolicy securityPolicy, AbstractAccessibilityServiceConnection.SystemSupport systemSupport, WindowManagerInternal windowManagerInternal, GlobalActionPerformer globalActionPerformer, int i2) {
        accessibilityServiceInfo.setComponentName(COMPONENT_NAME);
        if (this.mUiAutomationService != null) {
            throw new IllegalStateException("UiAutomationService " + iAccessibilityServiceClient + "already registered!");
        }
        try {
            iBinder.linkToDeath(this.mUiAutomationServiceOwnerDeathRecipient, 0);
            this.mSystemSupport = systemSupport;
            this.mUiAutomationService = new UiAutomationService(context, accessibilityServiceInfo, i, handler, obj, securityPolicy, systemSupport, windowManagerInternal, globalActionPerformer);
            this.mUiAutomationServiceOwner = iBinder;
            this.mUiAutomationFlags = i2;
            this.mUiAutomationServiceInfo = accessibilityServiceInfo;
            this.mUiAutomationService.mServiceInterface = iAccessibilityServiceClient;
            this.mUiAutomationService.onAdded();
            try {
                this.mUiAutomationService.mServiceInterface.asBinder().linkToDeath(this.mUiAutomationService, 0);
                this.mUiAutomationService.connectServiceUnknownThread();
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Failed registering death link: " + e);
                destroyUiAutomationService();
            }
        } catch (RemoteException e2) {
            Slog.e(LOG_TAG, "Couldn't register for the death of a UiTestAutomationService!", e2);
        }
    }

    void unregisterUiTestAutomationServiceLocked(IAccessibilityServiceClient iAccessibilityServiceClient) {
        if (this.mUiAutomationService == null || iAccessibilityServiceClient == null || this.mUiAutomationService.mServiceInterface == null || iAccessibilityServiceClient.asBinder() != this.mUiAutomationService.mServiceInterface.asBinder()) {
            throw new IllegalStateException("UiAutomationService " + iAccessibilityServiceClient + " not registered!");
        }
        destroyUiAutomationService();
    }

    void sendAccessibilityEventLocked(AccessibilityEvent accessibilityEvent) {
        if (this.mUiAutomationService != null) {
            this.mUiAutomationService.notifyAccessibilityEvent(accessibilityEvent);
        }
    }

    boolean isUiAutomationRunningLocked() {
        return this.mUiAutomationService != null;
    }

    boolean suppressingAccessibilityServicesLocked() {
        return this.mUiAutomationService != null && (this.mUiAutomationFlags & 1) == 0;
    }

    boolean isTouchExplorationEnabledLocked() {
        return this.mUiAutomationService != null && this.mUiAutomationService.mRequestTouchExplorationMode;
    }

    boolean canRetrieveInteractiveWindowsLocked() {
        return this.mUiAutomationService != null && this.mUiAutomationService.mRetrieveInteractiveWindows;
    }

    int getRequestedEventMaskLocked() {
        if (this.mUiAutomationService == null) {
            return 0;
        }
        return this.mUiAutomationService.mEventTypes;
    }

    int getRelevantEventTypes() {
        if (this.mUiAutomationService == null) {
            return 0;
        }
        return this.mUiAutomationService.getRelevantEventTypes();
    }

    AccessibilityServiceInfo getServiceInfo() {
        if (this.mUiAutomationService == null) {
            return null;
        }
        return this.mUiAutomationService.getServiceInfo();
    }

    void dumpUiAutomationService(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mUiAutomationService != null) {
            this.mUiAutomationService.dump(fileDescriptor, printWriter, strArr);
        }
    }

    private void destroyUiAutomationService() {
        this.mUiAutomationService.mServiceInterface.asBinder().unlinkToDeath(this.mUiAutomationService, 0);
        this.mUiAutomationService.onRemoved();
        this.mUiAutomationService.resetLocked();
        this.mUiAutomationService = null;
        this.mUiAutomationFlags = 0;
        if (this.mUiAutomationServiceOwner != null) {
            this.mUiAutomationServiceOwner.unlinkToDeath(this.mUiAutomationServiceOwnerDeathRecipient, 0);
            this.mUiAutomationServiceOwner = null;
        }
        this.mSystemSupport.onClientChange(false);
    }

    private class UiAutomationService extends AbstractAccessibilityServiceConnection {
        private final Handler mMainHandler;

        UiAutomationService(Context context, AccessibilityServiceInfo accessibilityServiceInfo, int i, Handler handler, Object obj, AccessibilityManagerService.SecurityPolicy securityPolicy, AbstractAccessibilityServiceConnection.SystemSupport systemSupport, WindowManagerInternal windowManagerInternal, GlobalActionPerformer globalActionPerformer) {
            super(context, UiAutomationManager.COMPONENT_NAME, accessibilityServiceInfo, i, handler, obj, securityPolicy, systemSupport, windowManagerInternal, globalActionPerformer);
            this.mMainHandler = handler;
        }

        void connectServiceUnknownThread() {
            this.mMainHandler.post(new Runnable() {
                @Override
                public final void run() {
                    UiAutomationManager.UiAutomationService.lambda$connectServiceUnknownThread$0(this.f$0);
                }
            });
        }

        public static void lambda$connectServiceUnknownThread$0(UiAutomationService uiAutomationService) {
            IAccessibilityServiceClient iAccessibilityServiceClient;
            IBinder iBinder;
            try {
                synchronized (uiAutomationService.mLock) {
                    iAccessibilityServiceClient = uiAutomationService.mServiceInterface;
                    uiAutomationService.mService = iAccessibilityServiceClient == null ? null : uiAutomationService.mServiceInterface.asBinder();
                    iBinder = uiAutomationService.mService;
                }
                if (iAccessibilityServiceClient != null) {
                    iBinder.linkToDeath(uiAutomationService, 0);
                    iAccessibilityServiceClient.init(uiAutomationService, uiAutomationService.mId, uiAutomationService.mOverlayWindowToken);
                }
            } catch (RemoteException e) {
                Slog.w(UiAutomationManager.LOG_TAG, "Error initialized connection", e);
                UiAutomationManager.this.destroyUiAutomationService();
            }
        }

        @Override
        public void binderDied() {
            UiAutomationManager.this.destroyUiAutomationService();
        }

        @Override
        protected boolean isCalledForCurrentUserLocked() {
            return true;
        }

        @Override
        protected boolean supportsFlagForNotImportantViews(AccessibilityServiceInfo accessibilityServiceInfo) {
            return true;
        }

        @Override
        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(this.mContext, UiAutomationManager.LOG_TAG, printWriter)) {
                synchronized (this.mLock) {
                    printWriter.append((CharSequence) ("Ui Automation[eventTypes=" + AccessibilityEvent.eventTypeToString(this.mEventTypes)));
                    printWriter.append((CharSequence) (", notificationTimeout=" + this.mNotificationTimeout));
                    printWriter.append("]");
                }
            }
        }

        public boolean setSoftKeyboardShowMode(int i) {
            return false;
        }

        public boolean isAccessibilityButtonAvailable() {
            return false;
        }

        public void disableSelf() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }

        @Override
        public boolean isCapturingFingerprintGestures() {
            return false;
        }

        @Override
        public void onFingerprintGestureDetectionActiveChanged(boolean z) {
        }

        @Override
        public void onFingerprintGesture(int i) {
        }
    }
}
