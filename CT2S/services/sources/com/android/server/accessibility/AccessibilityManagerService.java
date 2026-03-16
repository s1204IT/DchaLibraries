package com.android.server.accessibility;

import android.R;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.accessibilityservice.IAccessibilityServiceConnection;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Pools;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.IWindow;
import android.view.InputEventConsistencyVerifier;
import android.view.KeyEvent;
import android.view.MagnificationSpec;
import android.view.WindowInfo;
import android.view.WindowManagerInternal;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityInteractionClient;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.accessibility.IAccessibilityInteractionConnection;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import android.view.accessibility.IAccessibilityManager;
import android.view.accessibility.IAccessibilityManagerClient;
import com.android.internal.content.PackageMonitor;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.pm.PackageManagerService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.xmlpull.v1.XmlPullParserException;

public class AccessibilityManagerService extends IAccessibilityManager.Stub {
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final boolean DEBUG = false;
    private static final String FUNCTION_DUMP = "dump";
    private static final String FUNCTION_REGISTER_UI_TEST_AUTOMATION_SERVICE = "registerUiTestAutomationService";
    private static final String GET_WINDOW_TOKEN = "getWindowToken";
    private static final String LOG_TAG = "AccessibilityManagerService";
    private static final int MAX_POOL_SIZE = 10;
    private static final String TEMPORARY_ENABLE_ACCESSIBILITY_UNTIL_KEYGUARD_REMOVED = "temporaryEnableAccessibilityStateUntilKeyguardRemoved";
    private static final int WAIT_FOR_USER_STATE_FULLY_INITIALIZED_MILLIS = 3000;
    private static final int WAIT_WINDOWS_TIMEOUT_MILLIS = 5000;
    private static final int WINDOW_ID_UNKNOWN = -1;
    private static int sNextWindowId;
    private final Context mContext;
    private AlertDialog mEnableTouchExplorationDialog;
    private boolean mHasInputFilter;
    private boolean mInitialized;
    private AccessibilityInputFilter mInputFilter;
    private InteractionBridge mInteractionBridge;
    private final LockPatternUtils mLockPatternUtils;
    private final MainHandler mMainHandler;
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;
    private WindowsForAccessibilityCallback mWindowsForAccessibilityCallback;
    private static final ComponentName sFakeAccessibilityServiceComponentName = new ComponentName("foo.bar", "FakeService");
    private static final int OWN_PROCESS_ID = Process.myPid();
    private static int sIdCounter = 0;
    private final Object mLock = new Object();
    private final Pools.Pool<PendingEvent> mPendingEventPool = new Pools.SimplePool(10);
    private final TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);
    private final List<AccessibilityServiceInfo> mEnabledServicesForFeedbackTempList = new ArrayList();
    private final Region mTempRegion = new Region();
    private final Rect mTempRect = new Rect();
    private final Rect mTempRect1 = new Rect();
    private final Point mTempPoint = new Point();
    private final Set<ComponentName> mTempComponentNameSet = new HashSet();
    private final List<AccessibilityServiceInfo> mTempAccessibilityServiceInfoList = new ArrayList();
    private final RemoteCallbackList<IAccessibilityManagerClient> mGlobalClients = new RemoteCallbackList<>();
    private final SparseArray<AccessibilityConnectionWrapper> mGlobalInteractionConnections = new SparseArray<>();
    private final SparseArray<IBinder> mGlobalWindowTokens = new SparseArray<>();
    private final SparseArray<UserState> mUserStates = new SparseArray<>();
    private int mCurrentUserId = 0;
    private final WindowManagerInternal mWindowManagerService = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
    private final SecurityPolicy mSecurityPolicy = new SecurityPolicy();

    static int access$2808() {
        int i = sIdCounter;
        sIdCounter = i + 1;
        return i;
    }

    private UserState getCurrentUserStateLocked() {
        return getUserStateLocked(this.mCurrentUserId);
    }

    public AccessibilityManagerService(Context context) {
        this.mContext = context;
        this.mPackageManager = this.mContext.getPackageManager();
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mMainHandler = new MainHandler(this.mContext.getMainLooper());
        this.mLockPatternUtils = new LockPatternUtils(context);
        registerBroadcastReceivers();
        new AccessibilityContentObserver(this.mMainHandler).register(context.getContentResolver());
    }

    private UserState getUserStateLocked(int userId) {
        UserState state = this.mUserStates.get(userId);
        if (state == null) {
            UserState state2 = new UserState(userId);
            this.mUserStates.put(userId, state2);
            return state2;
        }
        return state;
    }

    private void registerBroadcastReceivers() {
        PackageMonitor monitor = new PackageMonitor() {
            public void onSomePackagesChanged() {
                synchronized (AccessibilityManagerService.this.mLock) {
                    if (getChangingUserId() == AccessibilityManagerService.this.mCurrentUserId) {
                        UserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                        userState.mInstalledServices.clear();
                        if (userState.mUiAutomationService == null && AccessibilityManagerService.this.readConfigurationForUserStateLocked(userState)) {
                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                        }
                    }
                }
            }

            public void onPackageRemoved(String packageName, int uid) {
                synchronized (AccessibilityManagerService.this.mLock) {
                    int userId = getChangingUserId();
                    if (userId == AccessibilityManagerService.this.mCurrentUserId) {
                        UserState userState = AccessibilityManagerService.this.getUserStateLocked(userId);
                        Iterator<ComponentName> it = userState.mEnabledServices.iterator();
                        while (it.hasNext()) {
                            ComponentName comp = it.next();
                            String compPkg = comp.getPackageName();
                            if (compPkg.equals(packageName)) {
                                it.remove();
                                AccessibilityManagerService.this.persistComponentNamesToSettingLocked("enabled_accessibility_services", userState.mEnabledServices, userId);
                                userState.mTouchExplorationGrantedServices.remove(comp);
                                AccessibilityManagerService.this.persistComponentNamesToSettingLocked("touch_exploration_granted_accessibility_services", userState.mTouchExplorationGrantedServices, userId);
                                if (userState.mUiAutomationService == null) {
                                    AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                                }
                                return;
                            }
                        }
                    }
                }
            }

            public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
                boolean z;
                synchronized (AccessibilityManagerService.this.mLock) {
                    int userId = getChangingUserId();
                    if (userId == AccessibilityManagerService.this.mCurrentUserId) {
                        UserState userState = AccessibilityManagerService.this.getUserStateLocked(userId);
                        Iterator<ComponentName> it = userState.mEnabledServices.iterator();
                        loop0: while (true) {
                            if (it.hasNext()) {
                                ComponentName comp = it.next();
                                String compPkg = comp.getPackageName();
                                for (String pkg : packages) {
                                    if (compPkg.equals(pkg)) {
                                        if (!doit) {
                                            z = true;
                                            break loop0;
                                        }
                                        it.remove();
                                        AccessibilityManagerService.this.persistComponentNamesToSettingLocked("enabled_accessibility_services", userState.mEnabledServices, userId);
                                        if (userState.mUiAutomationService == null) {
                                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                                        }
                                    }
                                }
                            } else {
                                z = AccessibilityManagerService.DEBUG;
                                break;
                            }
                        }
                    } else {
                        z = AccessibilityManagerService.DEBUG;
                    }
                }
                return z;
            }
        };
        monitor.register(this.mContext, (Looper) null, UserHandle.ALL, true);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    AccessibilityManagerService.this.switchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                    return;
                }
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    AccessibilityManagerService.this.removeUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                    return;
                }
                if ("android.intent.action.USER_PRESENT".equals(action)) {
                    UserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                    if (userState.mUiAutomationService == null && AccessibilityManagerService.this.readConfigurationForUserStateLocked(userState)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                }
            }
        }, UserHandle.ALL, intentFilter, null, null);
    }

    public int addClient(IAccessibilityManagerClient client, int userId) {
        int clientState;
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            UserState userState = getUserStateLocked(resolvedUserId);
            if (this.mSecurityPolicy.isCallerInteractingAcrossUsers(userId)) {
                this.mGlobalClients.register(client);
                clientState = userState.getClientState();
            } else {
                userState.mClients.register(client);
                clientState = resolvedUserId == this.mCurrentUserId ? userState.getClientState() : 0;
            }
        }
        return clientState;
    }

    public boolean sendAccessibilityEvent(AccessibilityEvent event, int userId) {
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            if (resolvedUserId != this.mCurrentUserId) {
                return true;
            }
            if (this.mSecurityPolicy.canDispatchAccessibilityEventLocked(event)) {
                this.mSecurityPolicy.updateActiveAndAccessibilityFocusedWindowLocked(event.getWindowId(), event.getSourceNodeId(), event.getEventType());
                this.mSecurityPolicy.updateEventSourceLocked(event);
                notifyAccessibilityServicesDelayedLocked(event, DEBUG);
                notifyAccessibilityServicesDelayedLocked(event, true);
            }
            if (this.mHasInputFilter && this.mInputFilter != null) {
                this.mMainHandler.obtainMessage(1, AccessibilityEvent.obtain(event)).sendToTarget();
            }
            event.recycle();
            getUserStateLocked(resolvedUserId).mHandledFeedbackTypes = 0;
            if (OWN_PROCESS_ID == Binder.getCallingPid()) {
                return DEBUG;
            }
            return true;
        }
    }

    public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(int userId) {
        List<AccessibilityServiceInfo> installedServices;
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            UserState userState = getUserStateLocked(resolvedUserId);
            if (userState.mUiAutomationService != null) {
                installedServices = new ArrayList<>();
                installedServices.addAll(userState.mInstalledServices);
                installedServices.remove(userState.mUiAutomationService.mAccessibilityServiceInfo);
            } else {
                installedServices = userState.mInstalledServices;
            }
        }
        return installedServices;
    }

    public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int feedbackType, int userId) {
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            UserState userState = getUserStateLocked(resolvedUserId);
            if (userState.mUiAutomationService != null) {
                return Collections.emptyList();
            }
            List<AccessibilityServiceInfo> result = this.mEnabledServicesForFeedbackTempList;
            result.clear();
            List<Service> services = userState.mBoundServices;
            while (feedbackType != 0) {
                int feedbackTypeBit = 1 << Integer.numberOfTrailingZeros(feedbackType);
                feedbackType &= feedbackTypeBit ^ (-1);
                int serviceCount = services.size();
                for (int i = 0; i < serviceCount; i++) {
                    Service service = services.get(i);
                    if ((service.mFeedbackType & feedbackTypeBit) != 0) {
                        result.add(service.mAccessibilityServiceInfo);
                    }
                }
            }
            return result;
        }
    }

    public void interrupt(int userId) {
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            if (resolvedUserId == this.mCurrentUserId) {
                CopyOnWriteArrayList<Service> services = getUserStateLocked(resolvedUserId).mBoundServices;
                int count = services.size();
                for (int i = 0; i < count; i++) {
                    Service service = services.get(i);
                    try {
                        service.mServiceInterface.onInterrupt();
                    } catch (RemoteException re) {
                        Slog.e(LOG_TAG, "Error during sending interrupt request to " + service.mService, re);
                    }
                }
            }
        }
    }

    public int addAccessibilityInteractionConnection(IWindow windowToken, IAccessibilityInteractionConnection connection, int userId) throws RemoteException {
        int windowId;
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            windowId = sNextWindowId;
            sNextWindowId = windowId + 1;
            if (this.mSecurityPolicy.isCallerInteractingAcrossUsers(userId)) {
                AccessibilityConnectionWrapper wrapper = new AccessibilityConnectionWrapper(windowId, connection, -1);
                wrapper.linkToDeath();
                this.mGlobalInteractionConnections.put(windowId, wrapper);
                this.mGlobalWindowTokens.put(windowId, windowToken.asBinder());
            } else {
                AccessibilityConnectionWrapper wrapper2 = new AccessibilityConnectionWrapper(windowId, connection, resolvedUserId);
                wrapper2.linkToDeath();
                UserState userState = getUserStateLocked(resolvedUserId);
                userState.mInteractionConnections.put(windowId, wrapper2);
                userState.mWindowTokens.put(windowId, windowToken.asBinder());
            }
        }
        return windowId;
    }

    public void removeAccessibilityInteractionConnection(IWindow window) {
        synchronized (this.mLock) {
            this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(UserHandle.getCallingUserId());
            IBinder token = window.asBinder();
            int removedWindowId = removeAccessibilityInteractionConnectionInternalLocked(token, this.mGlobalWindowTokens, this.mGlobalInteractionConnections);
            if (removedWindowId < 0) {
                int userCount = this.mUserStates.size();
                for (int i = 0; i < userCount; i++) {
                    UserState userState = this.mUserStates.valueAt(i);
                    int removedWindowIdForUser = removeAccessibilityInteractionConnectionInternalLocked(token, userState.mWindowTokens, userState.mInteractionConnections);
                    if (removedWindowIdForUser >= 0) {
                        return;
                    }
                }
            }
        }
    }

    private int removeAccessibilityInteractionConnectionInternalLocked(IBinder windowToken, SparseArray<IBinder> windowTokens, SparseArray<AccessibilityConnectionWrapper> interactionConnections) {
        int count = windowTokens.size();
        for (int i = 0; i < count; i++) {
            if (windowTokens.valueAt(i) == windowToken) {
                int windowId = windowTokens.keyAt(i);
                windowTokens.removeAt(i);
                AccessibilityConnectionWrapper wrapper = interactionConnections.get(windowId);
                wrapper.unlinkToDeath();
                interactionConnections.remove(windowId);
                return windowId;
            }
        }
        return -1;
    }

    public void registerUiTestAutomationService(IBinder owner, IAccessibilityServiceClient serviceClient, AccessibilityServiceInfo accessibilityServiceInfo) {
        this.mSecurityPolicy.enforceCallingPermission("android.permission.RETRIEVE_WINDOW_CONTENT", FUNCTION_REGISTER_UI_TEST_AUTOMATION_SERVICE);
        accessibilityServiceInfo.setComponentName(sFakeAccessibilityServiceComponentName);
        synchronized (this.mLock) {
            UserState userState = getCurrentUserStateLocked();
            if (userState.mUiAutomationService != null) {
                throw new IllegalStateException("UiAutomationService " + serviceClient + "already registered!");
            }
            try {
                owner.linkToDeath(userState.mUiAutomationSerivceOnwerDeathRecipient, 0);
                userState.mUiAutomationServiceOwner = owner;
                userState.mUiAutomationServiceClient = serviceClient;
                userState.mIsAccessibilityEnabled = true;
                userState.mIsTouchExplorationEnabled = DEBUG;
                userState.mIsEnhancedWebAccessibilityEnabled = DEBUG;
                userState.mIsDisplayMagnificationEnabled = DEBUG;
                userState.mInstalledServices.add(accessibilityServiceInfo);
                userState.mEnabledServices.clear();
                userState.mEnabledServices.add(sFakeAccessibilityServiceComponentName);
                userState.mTouchExplorationGrantedServices.add(sFakeAccessibilityServiceComponentName);
                onUserStateChangedLocked(userState);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Couldn't register for the death of a UiTestAutomationService!", re);
            }
        }
    }

    public void unregisterUiTestAutomationService(IAccessibilityServiceClient serviceClient) {
        synchronized (this.mLock) {
            UserState userState = getCurrentUserStateLocked();
            if (userState.mUiAutomationService == null || serviceClient == null || userState.mUiAutomationService.mServiceInterface == null || userState.mUiAutomationService.mServiceInterface.asBinder() != serviceClient.asBinder()) {
                throw new IllegalStateException("UiAutomationService " + serviceClient + " not registered!");
            }
            userState.mUiAutomationService.binderDied();
        }
    }

    public void temporaryEnableAccessibilityStateUntilKeyguardRemoved(ComponentName service, boolean touchExplorationEnabled) {
        this.mSecurityPolicy.enforceCallingPermission("android.permission.TEMPORARY_ENABLE_ACCESSIBILITY", TEMPORARY_ENABLE_ACCESSIBILITY_UNTIL_KEYGUARD_REMOVED);
        if (this.mWindowManagerService.isKeyguardLocked()) {
            synchronized (this.mLock) {
                UserState userState = getCurrentUserStateLocked();
                if (userState.mUiAutomationService == null) {
                    userState.mIsAccessibilityEnabled = true;
                    userState.mIsTouchExplorationEnabled = touchExplorationEnabled;
                    userState.mIsEnhancedWebAccessibilityEnabled = DEBUG;
                    userState.mIsDisplayMagnificationEnabled = DEBUG;
                    userState.mEnabledServices.clear();
                    userState.mEnabledServices.add(service);
                    userState.mBindingServices.clear();
                    userState.mTouchExplorationGrantedServices.clear();
                    userState.mTouchExplorationGrantedServices.add(service);
                    onUserStateChangedLocked(userState);
                }
            }
        }
    }

    public IBinder getWindowToken(int windowId) {
        this.mSecurityPolicy.enforceCallingPermission("android.permission.RETRIEVE_WINDOW_TOKEN", GET_WINDOW_TOKEN);
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(UserHandle.getCallingUserId());
            if (resolvedUserId != this.mCurrentUserId) {
                return null;
            }
            if (this.mSecurityPolicy.findWindowById(windowId) == null) {
                return null;
            }
            IBinder token = this.mGlobalWindowTokens.get(windowId);
            if (token != null) {
                return token;
            }
            return getCurrentUserStateLocked().mWindowTokens.get(windowId);
        }
    }

    boolean onGesture(int gestureId) {
        boolean handled;
        synchronized (this.mLock) {
            handled = notifyGestureLocked(gestureId, DEBUG);
            if (!handled) {
                handled = notifyGestureLocked(gestureId, true);
            }
        }
        return handled;
    }

    boolean notifyKeyEvent(KeyEvent event, int policyFlags) {
        boolean handled;
        synchronized (this.mLock) {
            KeyEvent localClone = KeyEvent.obtain(event);
            handled = notifyKeyEventLocked(localClone, policyFlags, DEBUG);
            if (!handled) {
                handled = notifyKeyEventLocked(localClone, policyFlags, true);
            }
        }
        return handled;
    }

    boolean getAccessibilityFocusClickPointInScreen(Point outPoint) {
        return getInteractionBridgeLocked().getAccessibilityFocusClickPointInScreenNotLocked(outPoint);
    }

    boolean getWindowBounds(int windowId, Rect outBounds) {
        IBinder token;
        synchronized (this.mLock) {
            token = this.mGlobalWindowTokens.get(windowId);
            if (token == null) {
                token = getCurrentUserStateLocked().mWindowTokens.get(windowId);
            }
        }
        this.mWindowManagerService.getWindowFrame(token, outBounds);
        if (!outBounds.isEmpty()) {
            return true;
        }
        return DEBUG;
    }

    boolean accessibilityFocusOnlyInActiveWindow() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mWindowsForAccessibilityCallback == null ? true : DEBUG;
        }
        return z;
    }

    int getActiveWindowId() {
        return this.mSecurityPolicy.getActiveWindowId();
    }

    void onTouchInteractionStart() {
        this.mSecurityPolicy.onTouchInteractionStart();
    }

    void onTouchInteractionEnd() {
        this.mSecurityPolicy.onTouchInteractionEnd();
    }

    void onMagnificationStateChanged() {
        notifyClearAccessibilityCacheLocked();
    }

    private void switchUser(int userId) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId != userId || !this.mInitialized) {
                UserState oldUserState = getCurrentUserStateLocked();
                oldUserState.onSwitchToAnotherUser();
                if (oldUserState.mClients.getRegisteredCallbackCount() > 0) {
                    this.mMainHandler.obtainMessage(3, oldUserState.mUserId, 0).sendToTarget();
                }
                UserManager userManager = (UserManager) this.mContext.getSystemService("user");
                boolean announceNewUser = userManager.getUsers().size() > 1;
                this.mCurrentUserId = userId;
                UserState userState = getCurrentUserStateLocked();
                if (userState.mUiAutomationService != null) {
                    userState.mUiAutomationService.binderDied();
                }
                readConfigurationForUserStateLocked(userState);
                onUserStateChangedLocked(userState);
                if (announceNewUser) {
                    this.mMainHandler.sendEmptyMessageDelayed(5, 3000L);
                }
            }
        }
    }

    private void removeUser(int userId) {
        synchronized (this.mLock) {
            this.mUserStates.remove(userId);
        }
    }

    private InteractionBridge getInteractionBridgeLocked() {
        if (this.mInteractionBridge == null) {
            this.mInteractionBridge = new InteractionBridge();
        }
        return this.mInteractionBridge;
    }

    private boolean notifyGestureLocked(int gestureId, boolean isDefault) {
        UserState state = getCurrentUserStateLocked();
        for (int i = state.mBoundServices.size() - 1; i >= 0; i--) {
            Service service = state.mBoundServices.get(i);
            if (service.mRequestTouchExplorationMode && service.mIsDefault == isDefault) {
                service.notifyGesture(gestureId);
                return true;
            }
        }
        return DEBUG;
    }

    private boolean notifyKeyEventLocked(KeyEvent event, int policyFlags, boolean isDefault) {
        UserState state = getCurrentUserStateLocked();
        for (int i = state.mBoundServices.size() - 1; i >= 0; i--) {
            Service service = state.mBoundServices.get(i);
            if (service.mRequestFilterKeyEvents && (service.mAccessibilityServiceInfo.getCapabilities() & 8) != 0 && service.mIsDefault == isDefault) {
                service.notifyKeyEvent(event, policyFlags);
                return true;
            }
        }
        return DEBUG;
    }

    private void notifyClearAccessibilityCacheLocked() {
        UserState state = getCurrentUserStateLocked();
        for (int i = state.mBoundServices.size() - 1; i >= 0; i--) {
            Service service = state.mBoundServices.get(i);
            service.notifyClearAccessibilityNodeInfoCache();
        }
    }

    private void removeAccessibilityInteractionConnectionLocked(int windowId, int userId) {
        if (userId == -1) {
            this.mGlobalWindowTokens.remove(windowId);
            this.mGlobalInteractionConnections.remove(windowId);
        } else {
            UserState userState = getCurrentUserStateLocked();
            userState.mWindowTokens.remove(windowId);
            userState.mInteractionConnections.remove(windowId);
        }
    }

    private boolean readInstalledAccessibilityServiceLocked(UserState userState) {
        this.mTempAccessibilityServiceInfoList.clear();
        List<ResolveInfo> installedServices = this.mPackageManager.queryIntentServicesAsUser(new Intent("android.accessibilityservice.AccessibilityService"), 132, this.mCurrentUserId);
        int count = installedServices.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo resolveInfo = installedServices.get(i);
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (!"android.permission.BIND_ACCESSIBILITY_SERVICE".equals(serviceInfo.permission)) {
                Slog.w(LOG_TAG, "Skipping accessibilty service " + new ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToShortString() + ": it does not require the permission android.permission.BIND_ACCESSIBILITY_SERVICE");
            } else {
                try {
                    AccessibilityServiceInfo accessibilityServiceInfo = new AccessibilityServiceInfo(resolveInfo, this.mContext);
                    this.mTempAccessibilityServiceInfoList.add(accessibilityServiceInfo);
                } catch (IOException | XmlPullParserException xppe) {
                    Slog.e(LOG_TAG, "Error while initializing AccessibilityServiceInfo", xppe);
                }
            }
        }
        if (!this.mTempAccessibilityServiceInfoList.equals(userState.mInstalledServices)) {
            userState.mInstalledServices.clear();
            userState.mInstalledServices.addAll(this.mTempAccessibilityServiceInfoList);
            this.mTempAccessibilityServiceInfoList.clear();
            return true;
        }
        this.mTempAccessibilityServiceInfoList.clear();
        return DEBUG;
    }

    private boolean readEnabledAccessibilityServicesLocked(UserState userState) {
        this.mTempComponentNameSet.clear();
        readComponentNamesFromSettingLocked("enabled_accessibility_services", userState.mUserId, this.mTempComponentNameSet);
        if (!this.mTempComponentNameSet.equals(userState.mEnabledServices)) {
            userState.mEnabledServices.clear();
            userState.mEnabledServices.addAll(this.mTempComponentNameSet);
            this.mTempComponentNameSet.clear();
            return true;
        }
        this.mTempComponentNameSet.clear();
        return DEBUG;
    }

    private boolean readTouchExplorationGrantedAccessibilityServicesLocked(UserState userState) {
        this.mTempComponentNameSet.clear();
        readComponentNamesFromSettingLocked("touch_exploration_granted_accessibility_services", userState.mUserId, this.mTempComponentNameSet);
        if (!this.mTempComponentNameSet.equals(userState.mTouchExplorationGrantedServices)) {
            userState.mTouchExplorationGrantedServices.clear();
            userState.mTouchExplorationGrantedServices.addAll(this.mTempComponentNameSet);
            this.mTempComponentNameSet.clear();
            return true;
        }
        this.mTempComponentNameSet.clear();
        return DEBUG;
    }

    private void notifyAccessibilityServicesDelayedLocked(AccessibilityEvent event, boolean isDefault) {
        try {
            UserState state = getCurrentUserStateLocked();
            int count = state.mBoundServices.size();
            for (int i = 0; i < count; i++) {
                Service service = state.mBoundServices.get(i);
                if (service.mIsDefault == isDefault && canDispatchEventToServiceLocked(service, event, state.mHandledFeedbackTypes)) {
                    state.mHandledFeedbackTypes |= service.mFeedbackType;
                    service.notifyAccessibilityEvent(event);
                }
            }
        } catch (IndexOutOfBoundsException e) {
        }
    }

    private void addServiceLocked(Service service, UserState userState) {
        try {
            service.onAdded();
            userState.mBoundServices.add(service);
            userState.mComponentNameToServiceMap.put(service.mComponentName, service);
        } catch (RemoteException e) {
        }
    }

    private void removeServiceLocked(Service service, UserState userState) {
        userState.mBoundServices.remove(service);
        userState.mComponentNameToServiceMap.remove(service.mComponentName);
        service.onRemoved();
    }

    private boolean canDispatchEventToServiceLocked(Service service, AccessibilityEvent event, int handledFeedbackTypes) {
        if (!service.canReceiveEventsLocked()) {
            return DEBUG;
        }
        if (event.getWindowId() != -1 && !event.isImportantForAccessibility() && (service.mFetchFlags & 8) == 0) {
            return DEBUG;
        }
        int eventType = event.getEventType();
        if ((service.mEventTypes & eventType) != eventType) {
            return DEBUG;
        }
        Set<String> packageNames = service.mPackageNames;
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : null;
        if (!packageNames.isEmpty() && !packageNames.contains(packageName)) {
            return DEBUG;
        }
        int feedbackType = service.mFeedbackType;
        if ((handledFeedbackTypes & feedbackType) != feedbackType || feedbackType == 16) {
            return true;
        }
        return DEBUG;
    }

    private void unbindAllServicesLocked(UserState userState) {
        List<Service> services = userState.mBoundServices;
        int i = 0;
        int count = services.size();
        while (i < count) {
            Service service = services.get(i);
            if (service.unbindLocked()) {
                i--;
                count--;
            }
            i++;
        }
    }

    private void readComponentNamesFromSettingLocked(String settingName, int userId, Set<ComponentName> outComponentNames) {
        ComponentName enabledService;
        String settingValue = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), settingName, userId);
        outComponentNames.clear();
        if (settingValue != null) {
            TextUtils.SimpleStringSplitter splitter = this.mStringColonSplitter;
            splitter.setString(settingValue);
            while (splitter.hasNext()) {
                String str = splitter.next();
                if (str != null && str.length() > 0 && (enabledService = ComponentName.unflattenFromString(str)) != null) {
                    outComponentNames.add(enabledService);
                }
            }
        }
    }

    private void persistComponentNamesToSettingLocked(String settingName, Set<ComponentName> componentNames, int userId) {
        StringBuilder builder = new StringBuilder();
        for (ComponentName componentName : componentNames) {
            if (builder.length() > 0) {
                builder.append(COMPONENT_NAME_SEPARATOR);
            }
            builder.append(componentName.flattenToShortString());
        }
        Settings.Secure.putStringForUser(this.mContext.getContentResolver(), settingName, builder.toString(), userId);
    }

    private void manageServicesLocked(UserState userState) {
        Map<ComponentName, Service> componentNameToServiceMap = userState.mComponentNameToServiceMap;
        boolean isEnabled = userState.mIsAccessibilityEnabled;
        int count = userState.mInstalledServices.size();
        for (int i = 0; i < count; i++) {
            AccessibilityServiceInfo installedService = userState.mInstalledServices.get(i);
            ComponentName componentName = ComponentName.unflattenFromString(installedService.getId());
            Service service = componentNameToServiceMap.get(componentName);
            if (isEnabled) {
                if (!userState.mBindingServices.contains(componentName)) {
                    if (userState.mEnabledServices.contains(componentName)) {
                        if (service == null) {
                            service = new Service(userState.mUserId, componentName, installedService);
                        } else if (userState.mBoundServices.contains(service)) {
                        }
                        service.bindLocked();
                    } else if (service != null) {
                        service.unbindLocked();
                    }
                }
            } else if (service != null) {
                service.unbindLocked();
            } else {
                userState.mBindingServices.remove(componentName);
            }
        }
        if (isEnabled && userState.mBoundServices.isEmpty() && userState.mBindingServices.isEmpty()) {
            userState.mIsAccessibilityEnabled = DEBUG;
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "accessibility_enabled", 0, userState.mUserId);
        }
    }

    private void scheduleUpdateClientsIfNeededLocked(UserState userState) {
        int clientState = userState.getClientState();
        if (userState.mLastSentClientState != clientState) {
            if (this.mGlobalClients.getRegisteredCallbackCount() > 0 || userState.mClients.getRegisteredCallbackCount() > 0) {
                userState.mLastSentClientState = clientState;
                this.mMainHandler.obtainMessage(2, clientState, userState.mUserId).sendToTarget();
            }
        }
    }

    private void scheduleUpdateInputFilter(UserState userState) {
        this.mMainHandler.obtainMessage(6, userState).sendToTarget();
    }

    private void updateInputFilter(UserState userState) {
        boolean setInputFilter = DEBUG;
        AccessibilityInputFilter inputFilter = null;
        synchronized (this.mLock) {
            int flags = 0;
            if (userState.mIsDisplayMagnificationEnabled) {
                flags = 0 | 1;
            }
            if (userState.mIsAccessibilityEnabled && userState.mIsTouchExplorationEnabled) {
                flags |= 2;
            }
            if (userState.mIsFilterKeyEventsEnabled) {
                flags |= 4;
            }
            if (flags != 0) {
                if (!this.mHasInputFilter) {
                    this.mHasInputFilter = true;
                    if (this.mInputFilter == null) {
                        this.mInputFilter = new AccessibilityInputFilter(this.mContext, this);
                    }
                    inputFilter = this.mInputFilter;
                    setInputFilter = true;
                }
                this.mInputFilter.setEnabledFeatures(flags);
            } else if (this.mHasInputFilter) {
                this.mHasInputFilter = DEBUG;
                this.mInputFilter.disableFeatures();
                inputFilter = null;
                setInputFilter = true;
            }
        }
        if (setInputFilter) {
            this.mWindowManagerService.setInputFilter(inputFilter);
        }
    }

    private void showEnableTouchExplorationDialog(final Service service) {
        synchronized (this.mLock) {
            String label = service.mResolveInfo.loadLabel(this.mContext.getPackageManager()).toString();
            final UserState state = getCurrentUserStateLocked();
            if (!state.mIsTouchExplorationEnabled) {
                if (this.mEnableTouchExplorationDialog == null || !this.mEnableTouchExplorationDialog.isShowing()) {
                    this.mEnableTouchExplorationDialog = new AlertDialog.Builder(this.mContext).setIconAttribute(R.attr.alertDialogIcon).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            state.mTouchExplorationGrantedServices.add(service.mComponentName);
                            AccessibilityManagerService.this.persistComponentNamesToSettingLocked("touch_exploration_granted_accessibility_services", state.mTouchExplorationGrantedServices, state.mUserId);
                            UserState userState = AccessibilityManagerService.this.getUserStateLocked(service.mUserId);
                            userState.mIsTouchExplorationEnabled = true;
                            Settings.Secure.putIntForUser(AccessibilityManagerService.this.mContext.getContentResolver(), "touch_exploration_enabled", 1, service.mUserId);
                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).setTitle(R.string.fingerprint_or_screen_lock_dialog_default_subtitle).setMessage(this.mContext.getString(R.string.fingerprint_recalibrate_notification_content, label)).create();
                    this.mEnableTouchExplorationDialog.getWindow().setType(2003);
                    this.mEnableTouchExplorationDialog.getWindow().getAttributes().privateFlags |= 16;
                    this.mEnableTouchExplorationDialog.setCanceledOnTouchOutside(true);
                    this.mEnableTouchExplorationDialog.show();
                }
            }
        }
    }

    private void onUserStateChangedLocked(UserState userState) {
        this.mInitialized = true;
        updateLegacyCapabilitiesLocked(userState);
        updateServicesLocked(userState);
        updateWindowsForAccessibilityCallbackLocked(userState);
        updateAccessibilityFocusBehaviorLocked(userState);
        updateFilterKeyEventsLocked(userState);
        updateTouchExplorationLocked(userState);
        updateEnhancedWebAccessibilityLocked(userState);
        updateDisplayColorAdjustmentSettingsLocked(userState);
        scheduleUpdateInputFilter(userState);
        scheduleUpdateClientsIfNeededLocked(userState);
    }

    private void updateAccessibilityFocusBehaviorLocked(UserState userState) {
        List<Service> boundServices = userState.mBoundServices;
        int boundServiceCount = boundServices.size();
        for (int i = 0; i < boundServiceCount; i++) {
            Service boundService = boundServices.get(i);
            if (boundService.canRetrieveInteractiveWindowsLocked()) {
                userState.mAccessibilityFocusOnlyInActiveWindow = DEBUG;
                return;
            }
        }
        userState.mAccessibilityFocusOnlyInActiveWindow = true;
    }

    private void updateWindowsForAccessibilityCallbackLocked(UserState userState) {
        if (userState.mIsAccessibilityEnabled) {
            boolean boundServiceCanRetrieveInteractiveWindows = DEBUG;
            List<Service> boundServices = userState.mBoundServices;
            int boundServiceCount = boundServices.size();
            int i = 0;
            while (true) {
                if (i >= boundServiceCount) {
                    break;
                }
                Service boundService = boundServices.get(i);
                if (!boundService.canRetrieveInteractiveWindowsLocked()) {
                    i++;
                } else {
                    boundServiceCanRetrieveInteractiveWindows = true;
                    break;
                }
            }
            if (boundServiceCanRetrieveInteractiveWindows) {
                if (this.mWindowsForAccessibilityCallback == null) {
                    this.mWindowsForAccessibilityCallback = new WindowsForAccessibilityCallback();
                    this.mWindowManagerService.setWindowsForAccessibilityCallback(this.mWindowsForAccessibilityCallback);
                    return;
                }
                return;
            }
        }
        if (this.mWindowsForAccessibilityCallback != null) {
            this.mWindowsForAccessibilityCallback = null;
            this.mWindowManagerService.setWindowsForAccessibilityCallback((WindowManagerInternal.WindowsForAccessibilityCallback) null);
            this.mSecurityPolicy.clearWindowsLocked();
        }
    }

    private void updateLegacyCapabilitiesLocked(UserState userState) {
        int installedServiceCount = userState.mInstalledServices.size();
        for (int i = 0; i < installedServiceCount; i++) {
            AccessibilityServiceInfo serviceInfo = userState.mInstalledServices.get(i);
            ResolveInfo resolveInfo = serviceInfo.getResolveInfo();
            if ((serviceInfo.getCapabilities() & 2) == 0 && resolveInfo.serviceInfo.applicationInfo.targetSdkVersion <= 17) {
                ComponentName componentName = new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
                if (userState.mTouchExplorationGrantedServices.contains(componentName)) {
                    serviceInfo.setCapabilities(serviceInfo.getCapabilities() | 2);
                }
            }
        }
    }

    private void updateFilterKeyEventsLocked(UserState userState) {
        int serviceCount = userState.mBoundServices.size();
        for (int i = 0; i < serviceCount; i++) {
            Service service = userState.mBoundServices.get(i);
            if (service.mRequestFilterKeyEvents && (service.mAccessibilityServiceInfo.getCapabilities() & 8) != 0) {
                userState.mIsFilterKeyEventsEnabled = true;
                return;
            }
        }
        userState.mIsFilterKeyEventsEnabled = DEBUG;
    }

    private void updateServicesLocked(UserState userState) {
        if (userState.mIsAccessibilityEnabled) {
            manageServicesLocked(userState);
        } else {
            unbindAllServicesLocked(userState);
        }
    }

    private boolean readConfigurationForUserStateLocked(UserState userState) {
        boolean somthingChanged = readAccessibilityEnabledSettingLocked(userState);
        return somthingChanged | readInstalledAccessibilityServiceLocked(userState) | readEnabledAccessibilityServicesLocked(userState) | readTouchExplorationGrantedAccessibilityServicesLocked(userState) | readTouchExplorationEnabledSettingLocked(userState) | readHighTextContrastEnabledSettingLocked(userState) | readEnhancedWebAccessibilityEnabledChangedLocked(userState) | readDisplayMagnificationEnabledSettingLocked(userState) | readDisplayColorAdjustmentSettingsLocked(userState);
    }

    private boolean readAccessibilityEnabledSettingLocked(UserState userState) {
        boolean accessibilityEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_enabled", 0, userState.mUserId) == 1;
        if (accessibilityEnabled == userState.mIsAccessibilityEnabled) {
            return DEBUG;
        }
        userState.mIsAccessibilityEnabled = accessibilityEnabled;
        return true;
    }

    private boolean readTouchExplorationEnabledSettingLocked(UserState userState) {
        boolean touchExplorationEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "touch_exploration_enabled", 0, userState.mUserId) == 1;
        if (touchExplorationEnabled == userState.mIsTouchExplorationEnabled) {
            return DEBUG;
        }
        userState.mIsTouchExplorationEnabled = touchExplorationEnabled;
        return true;
    }

    private boolean readDisplayMagnificationEnabledSettingLocked(UserState userState) {
        boolean displayMagnificationEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_magnification_enabled", 0, userState.mUserId) == 1;
        if (displayMagnificationEnabled == userState.mIsDisplayMagnificationEnabled) {
            return DEBUG;
        }
        userState.mIsDisplayMagnificationEnabled = displayMagnificationEnabled;
        return true;
    }

    private boolean readEnhancedWebAccessibilityEnabledChangedLocked(UserState userState) {
        boolean enhancedWeAccessibilityEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_script_injection", 0, userState.mUserId) == 1;
        if (enhancedWeAccessibilityEnabled == userState.mIsEnhancedWebAccessibilityEnabled) {
            return DEBUG;
        }
        userState.mIsEnhancedWebAccessibilityEnabled = enhancedWeAccessibilityEnabled;
        return true;
    }

    private boolean readDisplayColorAdjustmentSettingsLocked(UserState userState) {
        boolean displayAdjustmentsEnabled = DisplayAdjustmentUtils.hasAdjustments(this.mContext, userState.mUserId);
        if (displayAdjustmentsEnabled != userState.mHasDisplayColorAdjustment) {
            userState.mHasDisplayColorAdjustment = displayAdjustmentsEnabled;
            return true;
        }
        return displayAdjustmentsEnabled;
    }

    private boolean readHighTextContrastEnabledSettingLocked(UserState userState) {
        boolean highTextContrastEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "high_text_contrast_enabled", 0, userState.mUserId) == 1;
        if (highTextContrastEnabled == userState.mIsTextHighContrastEnabled) {
            return DEBUG;
        }
        userState.mIsTextHighContrastEnabled = highTextContrastEnabled;
        return true;
    }

    private void updateTouchExplorationLocked(UserState userState) {
        boolean enabled = DEBUG;
        int serviceCount = userState.mBoundServices.size();
        int i = 0;
        while (true) {
            if (i >= serviceCount) {
                break;
            }
            Service service = userState.mBoundServices.get(i);
            if (!canRequestAndRequestsTouchExplorationLocked(service)) {
                i++;
            } else {
                enabled = true;
                break;
            }
        }
        if (enabled != userState.mIsTouchExplorationEnabled) {
            userState.mIsTouchExplorationEnabled = enabled;
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "touch_exploration_enabled", enabled ? 1 : 0, userState.mUserId);
        }
    }

    private boolean canRequestAndRequestsTouchExplorationLocked(Service service) {
        if (!service.canReceiveEventsLocked() || !service.mRequestTouchExplorationMode) {
            return DEBUG;
        }
        if (service.mIsAutomation) {
            return true;
        }
        if (service.mResolveInfo.serviceInfo.applicationInfo.targetSdkVersion <= 17) {
            UserState userState = getUserStateLocked(service.mUserId);
            if (userState.mTouchExplorationGrantedServices.contains(service.mComponentName)) {
                return true;
            }
            if (this.mEnableTouchExplorationDialog == null || !this.mEnableTouchExplorationDialog.isShowing()) {
                this.mMainHandler.obtainMessage(7, service).sendToTarget();
            }
        } else if ((service.mAccessibilityServiceInfo.getCapabilities() & 2) != 0) {
            return true;
        }
        return DEBUG;
    }

    private void updateEnhancedWebAccessibilityLocked(UserState userState) {
        boolean enabled = DEBUG;
        int serviceCount = userState.mBoundServices.size();
        int i = 0;
        while (true) {
            if (i >= serviceCount) {
                break;
            }
            Service service = userState.mBoundServices.get(i);
            if (!canRequestAndRequestsEnhancedWebAccessibilityLocked(service)) {
                i++;
            } else {
                enabled = true;
                break;
            }
        }
        if (enabled != userState.mIsEnhancedWebAccessibilityEnabled) {
            userState.mIsEnhancedWebAccessibilityEnabled = enabled;
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "accessibility_script_injection", enabled ? 1 : 0, userState.mUserId);
        }
    }

    private boolean canRequestAndRequestsEnhancedWebAccessibilityLocked(Service service) {
        if (!service.canReceiveEventsLocked() || !service.mRequestEnhancedWebAccessibility) {
            return DEBUG;
        }
        if (service.mIsAutomation || (service.mAccessibilityServiceInfo.getCapabilities() & 4) != 0) {
            return true;
        }
        return DEBUG;
    }

    private void updateDisplayColorAdjustmentSettingsLocked(UserState userState) {
        DisplayAdjustmentUtils.applyAdjustments(this.mContext, userState.mUserId);
    }

    private boolean hasRunningServicesLocked(UserState userState) {
        if (userState.mBoundServices.isEmpty() && userState.mBindingServices.isEmpty()) {
            return DEBUG;
        }
        return true;
    }

    private MagnificationSpec getCompatibleMagnificationSpecLocked(int windowId) {
        IBinder windowToken = this.mGlobalWindowTokens.get(windowId);
        if (windowToken == null) {
            windowToken = getCurrentUserStateLocked().mWindowTokens.get(windowId);
        }
        if (windowToken != null) {
            return this.mWindowManagerService.getCompatibleMagnificationSpecForWindow(windowToken);
        }
        return null;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mSecurityPolicy.enforceCallingPermission("android.permission.DUMP", FUNCTION_DUMP);
        synchronized (this.mLock) {
            pw.println("ACCESSIBILITY MANAGER (dumpsys accessibility)");
            pw.println();
            int userCount = this.mUserStates.size();
            for (int i = 0; i < userCount; i++) {
                UserState userState = this.mUserStates.valueAt(i);
                pw.append((CharSequence) ("User state[attributes:{id=" + userState.mUserId));
                pw.append((CharSequence) (", currentUser=" + (userState.mUserId == this.mCurrentUserId ? true : DEBUG)));
                pw.append((CharSequence) (", accessibilityEnabled=" + userState.mIsAccessibilityEnabled));
                pw.append((CharSequence) (", touchExplorationEnabled=" + userState.mIsTouchExplorationEnabled));
                pw.append((CharSequence) (", displayMagnificationEnabled=" + userState.mIsDisplayMagnificationEnabled));
                if (userState.mUiAutomationService != null) {
                    pw.append(", ");
                    userState.mUiAutomationService.dump(fd, pw, args);
                    pw.println();
                }
                pw.append("}");
                pw.println();
                pw.append("           services:{");
                int serviceCount = userState.mBoundServices.size();
                for (int j = 0; j < serviceCount; j++) {
                    if (j > 0) {
                        pw.append(", ");
                        pw.println();
                        pw.append("                     ");
                    }
                    Service service = userState.mBoundServices.get(j);
                    service.dump(fd, pw, args);
                }
                pw.println("}]");
                pw.println();
            }
            if (this.mSecurityPolicy.mWindows != null) {
                int windowCount = this.mSecurityPolicy.mWindows.size();
                for (int j2 = 0; j2 < windowCount; j2++) {
                    if (j2 > 0) {
                        pw.append(',');
                        pw.println();
                    }
                    pw.append("Window[");
                    AccessibilityWindowInfo window = this.mSecurityPolicy.mWindows.get(j2);
                    pw.append((CharSequence) window.toString());
                    pw.append(']');
                }
            }
        }
    }

    private class AccessibilityConnectionWrapper implements IBinder.DeathRecipient {
        private final IAccessibilityInteractionConnection mConnection;
        private final int mUserId;
        private final int mWindowId;

        public AccessibilityConnectionWrapper(int windowId, IAccessibilityInteractionConnection connection, int userId) {
            this.mWindowId = windowId;
            this.mUserId = userId;
            this.mConnection = connection;
        }

        public void linkToDeath() throws RemoteException {
            this.mConnection.asBinder().linkToDeath(this, 0);
        }

        public void unlinkToDeath() {
            this.mConnection.asBinder().unlinkToDeath(this, 0);
        }

        @Override
        public void binderDied() {
            unlinkToDeath();
            synchronized (AccessibilityManagerService.this.mLock) {
                AccessibilityManagerService.this.removeAccessibilityInteractionConnectionLocked(this.mWindowId, this.mUserId);
            }
        }
    }

    private final class MainHandler extends Handler {
        public static final int MSG_ANNOUNCE_NEW_USER_IF_NEEDED = 5;
        public static final int MSG_CLEAR_ACCESSIBILITY_FOCUS = 9;
        public static final int MSG_SEND_ACCESSIBILITY_EVENT_TO_INPUT_FILTER = 1;
        public static final int MSG_SEND_CLEARED_STATE_TO_CLIENTS_FOR_USER = 3;
        public static final int MSG_SEND_KEY_EVENT_TO_INPUT_FILTER = 8;
        public static final int MSG_SEND_STATE_TO_CLIENTS = 2;
        public static final int MSG_SHOW_ENABLED_TOUCH_EXPLORATION_DIALOG = 7;
        public static final int MSG_UPDATE_INPUT_FILTER = 6;

        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            InteractionBridge bridge;
            int type = msg.what;
            switch (type) {
                case 1:
                    AccessibilityEvent event = (AccessibilityEvent) msg.obj;
                    synchronized (AccessibilityManagerService.this.mLock) {
                        if (AccessibilityManagerService.this.mHasInputFilter && AccessibilityManagerService.this.mInputFilter != null) {
                            AccessibilityManagerService.this.mInputFilter.notifyAccessibilityEvent(event);
                        }
                        break;
                    }
                    event.recycle();
                    return;
                case 2:
                    int clientState = msg.arg1;
                    int userId = msg.arg2;
                    sendStateToClients(clientState, AccessibilityManagerService.this.mGlobalClients);
                    sendStateToClientsForUser(clientState, userId);
                    return;
                case 3:
                    int userId2 = msg.arg1;
                    sendStateToClientsForUser(0, userId2);
                    return;
                case 4:
                default:
                    return;
                case 5:
                    announceNewUserIfNeeded();
                    return;
                case 6:
                    UserState userState = (UserState) msg.obj;
                    AccessibilityManagerService.this.updateInputFilter(userState);
                    return;
                case 7:
                    Service service = (Service) msg.obj;
                    AccessibilityManagerService.this.showEnableTouchExplorationDialog(service);
                    return;
                case 8:
                    KeyEvent event2 = (KeyEvent) msg.obj;
                    int policyFlags = msg.arg1;
                    synchronized (AccessibilityManagerService.this.mLock) {
                        if (AccessibilityManagerService.this.mHasInputFilter && AccessibilityManagerService.this.mInputFilter != null) {
                            AccessibilityManagerService.this.mInputFilter.sendInputEvent(event2, policyFlags);
                        }
                        break;
                    }
                    event2.recycle();
                    return;
                case 9:
                    int windowId = msg.arg1;
                    synchronized (AccessibilityManagerService.this.mLock) {
                        bridge = AccessibilityManagerService.this.getInteractionBridgeLocked();
                        break;
                    }
                    bridge.clearAccessibilityFocusNotLocked(windowId);
                    return;
            }
        }

        private void announceNewUserIfNeeded() {
            synchronized (AccessibilityManagerService.this.mLock) {
                UserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                if (userState.mIsAccessibilityEnabled) {
                    UserManager userManager = (UserManager) AccessibilityManagerService.this.mContext.getSystemService("user");
                    String message = AccessibilityManagerService.this.mContext.getString(R.string.mediasize_iso_c5, userManager.getUserInfo(AccessibilityManagerService.this.mCurrentUserId).name);
                    AccessibilityEvent event = AccessibilityEvent.obtain(16384);
                    event.getText().add(message);
                    AccessibilityManagerService.this.sendAccessibilityEvent(event, AccessibilityManagerService.this.mCurrentUserId);
                }
            }
        }

        private void sendStateToClientsForUser(int clientState, int userId) {
            UserState userState;
            synchronized (AccessibilityManagerService.this.mLock) {
                userState = AccessibilityManagerService.this.getUserStateLocked(userId);
            }
            sendStateToClients(clientState, userState.mClients);
        }

        private void sendStateToClients(int clientState, RemoteCallbackList<IAccessibilityManagerClient> clients) {
            try {
                int userClientCount = clients.beginBroadcast();
                for (int i = 0; i < userClientCount; i++) {
                    IAccessibilityManagerClient client = clients.getBroadcastItem(i);
                    try {
                        client.setState(clientState);
                    } catch (RemoteException e) {
                    }
                }
            } finally {
                clients.finishBroadcast();
            }
        }
    }

    private PendingEvent obtainPendingEventLocked(KeyEvent event, int policyFlags, int sequence) {
        PendingEvent pendingEvent = (PendingEvent) this.mPendingEventPool.acquire();
        if (pendingEvent == null) {
            pendingEvent = new PendingEvent();
        }
        pendingEvent.event = event;
        pendingEvent.policyFlags = policyFlags;
        pendingEvent.sequence = sequence;
        return pendingEvent;
    }

    private void recyclePendingEventLocked(PendingEvent pendingEvent) {
        pendingEvent.clear();
        this.mPendingEventPool.release(pendingEvent);
    }

    private int findWindowIdLocked(IBinder token) {
        int globalIndex = this.mGlobalWindowTokens.indexOfValue(token);
        if (globalIndex >= 0) {
            return this.mGlobalWindowTokens.keyAt(globalIndex);
        }
        UserState userState = getCurrentUserStateLocked();
        int userIndex = userState.mWindowTokens.indexOfValue(token);
        if (userIndex >= 0) {
            return userState.mWindowTokens.keyAt(userIndex);
        }
        return -1;
    }

    private void ensureWindowsAvailableTimed() {
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.mWindows == null) {
                if (this.mWindowsForAccessibilityCallback == null) {
                    UserState userState = getCurrentUserStateLocked();
                    onUserStateChangedLocked(userState);
                }
                if (this.mWindowsForAccessibilityCallback != null) {
                    long startMillis = SystemClock.uptimeMillis();
                    while (this.mSecurityPolicy.mWindows == null) {
                        long elapsedMillis = SystemClock.uptimeMillis() - startMillis;
                        long remainMillis = 5000 - elapsedMillis;
                        if (remainMillis > 0) {
                            try {
                                this.mLock.wait(remainMillis);
                            } catch (InterruptedException e) {
                            }
                        } else {
                            return;
                        }
                    }
                }
            }
        }
    }

    class Service extends IAccessibilityServiceConnection.Stub implements ServiceConnection, IBinder.DeathRecipient {
        AccessibilityServiceInfo mAccessibilityServiceInfo;
        ComponentName mComponentName;
        public Handler mEventDispatchHandler;
        int mEventTypes;
        int mFeedbackType;
        int mFetchFlags;
        int mId;
        Intent mIntent;
        public InvocationHandler mInvocationHandler;
        boolean mIsAutomation;
        boolean mIsDefault;
        long mNotificationTimeout;
        boolean mRequestEnhancedWebAccessibility;
        boolean mRequestFilterKeyEvents;
        boolean mRequestTouchExplorationMode;
        final ResolveInfo mResolveInfo;
        boolean mRetrieveInteractiveWindows;
        IBinder mService;
        IAccessibilityServiceClient mServiceInterface;
        final int mUserId;
        boolean mWasConnectedAndDied;
        Set<String> mPackageNames = new HashSet();
        final IBinder mOverlayWindowToken = new Binder();
        final SparseArray<AccessibilityEvent> mPendingEvents = new SparseArray<>();
        final KeyEventDispatcher mKeyEventDispatcher = new KeyEventDispatcher();

        public Service(int userId, ComponentName componentName, AccessibilityServiceInfo accessibilityServiceInfo) {
            this.mId = 0;
            this.mEventDispatchHandler = new Handler(AccessibilityManagerService.this.mMainHandler.getLooper()) {
                @Override
                public void handleMessage(Message message) {
                    int eventType = message.what;
                    Service.this.notifyAccessibilityEventInternal(eventType);
                }
            };
            this.mInvocationHandler = new InvocationHandler(AccessibilityManagerService.this.mMainHandler.getLooper());
            this.mUserId = userId;
            this.mResolveInfo = accessibilityServiceInfo.getResolveInfo();
            this.mId = AccessibilityManagerService.access$2808();
            this.mComponentName = componentName;
            this.mAccessibilityServiceInfo = accessibilityServiceInfo;
            this.mIsAutomation = AccessibilityManagerService.sFakeAccessibilityServiceComponentName.equals(componentName);
            if (!this.mIsAutomation) {
                this.mIntent = new Intent().setComponent(this.mComponentName);
                this.mIntent.putExtra("android.intent.extra.client_label", R.string.keyguard_accessibility_unlock_area_expanded);
                if (BenesseExtension.getDchaState() == 0) {
                    this.mIntent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivity(AccessibilityManagerService.this.mContext, 0, new Intent("android.settings.ACCESSIBILITY_SETTINGS"), 0));
                }
            }
            setDynamicallyConfigurableProperties(accessibilityServiceInfo);
        }

        public void setDynamicallyConfigurableProperties(AccessibilityServiceInfo info) {
            this.mEventTypes = info.eventTypes;
            this.mFeedbackType = info.feedbackType;
            String[] packageNames = info.packageNames;
            if (packageNames != null) {
                this.mPackageNames.addAll(Arrays.asList(packageNames));
            }
            this.mNotificationTimeout = info.notificationTimeout;
            this.mIsDefault = (info.flags & 1) != 0;
            if (this.mIsAutomation || info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion >= 16) {
                if ((info.flags & 2) != 0) {
                    this.mFetchFlags |= 8;
                } else {
                    this.mFetchFlags &= -9;
                }
            }
            if ((info.flags & 16) != 0) {
                this.mFetchFlags |= 16;
            } else {
                this.mFetchFlags &= -17;
            }
            this.mRequestTouchExplorationMode = (info.flags & 4) != 0;
            this.mRequestEnhancedWebAccessibility = (info.flags & 8) != 0;
            this.mRequestFilterKeyEvents = (info.flags & 32) != 0;
            this.mRetrieveInteractiveWindows = (info.flags & 64) != 0;
        }

        public boolean bindLocked() {
            UserState userState = AccessibilityManagerService.this.getUserStateLocked(this.mUserId);
            if (!this.mIsAutomation) {
                if (this.mService == null && AccessibilityManagerService.this.mContext.bindServiceAsUser(this.mIntent, this, 1, new UserHandle(this.mUserId))) {
                    userState.mBindingServices.add(this.mComponentName);
                    return AccessibilityManagerService.DEBUG;
                }
                return AccessibilityManagerService.DEBUG;
            }
            userState.mBindingServices.add(this.mComponentName);
            this.mService = userState.mUiAutomationServiceClient.asBinder();
            AccessibilityManagerService.this.mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Service.this.onServiceConnected(Service.this.mComponentName, Service.this.mService);
                }
            });
            userState.mUiAutomationService = this;
            return AccessibilityManagerService.DEBUG;
        }

        public boolean unbindLocked() {
            if (this.mService != null) {
                UserState userState = AccessibilityManagerService.this.getUserStateLocked(this.mUserId);
                this.mKeyEventDispatcher.flush();
                if (!this.mIsAutomation) {
                    AccessibilityManagerService.this.mContext.unbindService(this);
                } else {
                    userState.destroyUiAutomationService();
                }
                AccessibilityManagerService.this.removeServiceLocked(this, userState);
                resetLocked();
                return true;
            }
            return AccessibilityManagerService.DEBUG;
        }

        public boolean canReceiveEventsLocked() {
            if (this.mEventTypes == 0 || this.mFeedbackType == 0 || this.mService == null) {
                return AccessibilityManagerService.DEBUG;
            }
            return true;
        }

        public void setOnKeyEventResult(boolean handled, int sequence) {
            this.mKeyEventDispatcher.setOnKeyEventResult(handled, sequence);
        }

        public AccessibilityServiceInfo getServiceInfo() {
            AccessibilityServiceInfo accessibilityServiceInfo;
            synchronized (AccessibilityManagerService.this.mLock) {
                accessibilityServiceInfo = this.mAccessibilityServiceInfo;
            }
            return accessibilityServiceInfo;
        }

        public boolean canRetrieveInteractiveWindowsLocked() {
            if (AccessibilityManagerService.this.mSecurityPolicy.canRetrieveWindowContentLocked(this) && this.mRetrieveInteractiveWindows) {
                return true;
            }
            return AccessibilityManagerService.DEBUG;
        }

        public void setServiceInfo(AccessibilityServiceInfo info) {
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (AccessibilityManagerService.this.mLock) {
                    AccessibilityServiceInfo oldInfo = this.mAccessibilityServiceInfo;
                    if (oldInfo != null) {
                        oldInfo.updateDynamicallyConfigurableProperties(info);
                        setDynamicallyConfigurableProperties(oldInfo);
                    } else {
                        setDynamicallyConfigurableProperties(info);
                    }
                    UserState userState = AccessibilityManagerService.this.getUserStateLocked(this.mUserId);
                    AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            synchronized (AccessibilityManagerService.this.mLock) {
                this.mService = service;
                this.mServiceInterface = IAccessibilityServiceClient.Stub.asInterface(service);
                UserState userState = AccessibilityManagerService.this.getUserStateLocked(this.mUserId);
                AccessibilityManagerService.this.addServiceLocked(this, userState);
                if (userState.mBindingServices.contains(this.mComponentName) || this.mWasConnectedAndDied) {
                    userState.mBindingServices.remove(this.mComponentName);
                    this.mWasConnectedAndDied = AccessibilityManagerService.DEBUG;
                    try {
                        this.mServiceInterface.init(this, this.mId, this.mOverlayWindowToken);
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    } catch (RemoteException re) {
                        Slog.w(AccessibilityManagerService.LOG_TAG, "Error while setting connection for service: " + service, re);
                        binderDied();
                    }
                } else {
                    binderDied();
                }
            }
        }

        public List<AccessibilityWindowInfo> getWindows() {
            List<AccessibilityWindowInfo> windows = null;
            AccessibilityManagerService.this.ensureWindowsAvailableTimed();
            synchronized (AccessibilityManagerService.this.mLock) {
                int resolvedUserId = AccessibilityManagerService.this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(-2);
                if (resolvedUserId == AccessibilityManagerService.this.mCurrentUserId) {
                    boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canRetrieveWindowsLocked(this);
                    if (permissionGranted) {
                        if (AccessibilityManagerService.this.mSecurityPolicy.mWindows != null) {
                            windows = new ArrayList<>();
                            int windowCount = AccessibilityManagerService.this.mSecurityPolicy.mWindows.size();
                            for (int i = 0; i < windowCount; i++) {
                                AccessibilityWindowInfo window = AccessibilityManagerService.this.mSecurityPolicy.mWindows.get(i);
                                AccessibilityWindowInfo windowClone = AccessibilityWindowInfo.obtain(window);
                                windowClone.setConnectionId(this.mId);
                                windows.add(windowClone);
                            }
                        }
                    }
                }
            }
            return windows;
        }

        public AccessibilityWindowInfo getWindow(int windowId) {
            AccessibilityWindowInfo windowClone = null;
            AccessibilityManagerService.this.ensureWindowsAvailableTimed();
            synchronized (AccessibilityManagerService.this.mLock) {
                int resolvedUserId = AccessibilityManagerService.this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(-2);
                if (resolvedUserId == AccessibilityManagerService.this.mCurrentUserId) {
                    boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canRetrieveWindowsLocked(this);
                    if (permissionGranted) {
                        AccessibilityWindowInfo window = AccessibilityManagerService.this.mSecurityPolicy.findWindowById(windowId);
                        if (window != null) {
                            windowClone = AccessibilityWindowInfo.obtain(window);
                            windowClone.setConnectionId(this.mId);
                        }
                    }
                }
            }
            return windowClone;
        }

        public boolean findAccessibilityNodeInfosByViewId(int accessibilityWindowId, long accessibilityNodeId, String viewIdResName, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
            Region partialInteractiveRegion = AccessibilityManagerService.this.mTempRegion;
            synchronized (AccessibilityManagerService.this.mLock) {
                int resolvedUserId = AccessibilityManagerService.this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(-2);
                if (resolvedUserId != AccessibilityManagerService.this.mCurrentUserId) {
                    return AccessibilityManagerService.DEBUG;
                }
                int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId);
                if (!permissionGranted) {
                    return AccessibilityManagerService.DEBUG;
                }
                IAccessibilityInteractionConnection connection = getConnectionLocked(resolvedWindowId);
                if (connection != null) {
                    if (!AccessibilityManagerService.this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                        partialInteractiveRegion = null;
                    }
                    int interrogatingPid = Binder.getCallingPid();
                    long identityToken = Binder.clearCallingIdentity();
                    MagnificationSpec spec = AccessibilityManagerService.this.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                    try {
                        connection.findAccessibilityNodeInfosByViewId(accessibilityNodeId, viewIdResName, partialInteractiveRegion, interactionId, callback, this.mFetchFlags, interrogatingPid, interrogatingTid, spec);
                        Binder.restoreCallingIdentity(identityToken);
                        return true;
                    } catch (RemoteException e) {
                        Binder.restoreCallingIdentity(identityToken);
                        return AccessibilityManagerService.DEBUG;
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(identityToken);
                        throw th;
                    }
                }
                return AccessibilityManagerService.DEBUG;
            }
        }

        public boolean findAccessibilityNodeInfosByText(int accessibilityWindowId, long accessibilityNodeId, String text, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
            Region partialInteractiveRegion = AccessibilityManagerService.this.mTempRegion;
            synchronized (AccessibilityManagerService.this.mLock) {
                int resolvedUserId = AccessibilityManagerService.this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(-2);
                if (resolvedUserId != AccessibilityManagerService.this.mCurrentUserId) {
                    return AccessibilityManagerService.DEBUG;
                }
                int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId);
                if (!permissionGranted) {
                    return AccessibilityManagerService.DEBUG;
                }
                IAccessibilityInteractionConnection connection = getConnectionLocked(resolvedWindowId);
                if (connection != null) {
                    if (!AccessibilityManagerService.this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                        partialInteractiveRegion = null;
                    }
                    int interrogatingPid = Binder.getCallingPid();
                    long identityToken = Binder.clearCallingIdentity();
                    MagnificationSpec spec = AccessibilityManagerService.this.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                    try {
                        connection.findAccessibilityNodeInfosByText(accessibilityNodeId, text, partialInteractiveRegion, interactionId, callback, this.mFetchFlags, interrogatingPid, interrogatingTid, spec);
                        Binder.restoreCallingIdentity(identityToken);
                        return true;
                    } catch (RemoteException e) {
                        Binder.restoreCallingIdentity(identityToken);
                        return AccessibilityManagerService.DEBUG;
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(identityToken);
                        throw th;
                    }
                }
                return AccessibilityManagerService.DEBUG;
            }
        }

        public boolean findAccessibilityNodeInfoByAccessibilityId(int accessibilityWindowId, long accessibilityNodeId, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, long interrogatingTid) throws RemoteException {
            Region partialInteractiveRegion = AccessibilityManagerService.this.mTempRegion;
            synchronized (AccessibilityManagerService.this.mLock) {
                int resolvedUserId = AccessibilityManagerService.this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(-2);
                if (resolvedUserId != AccessibilityManagerService.this.mCurrentUserId) {
                    return AccessibilityManagerService.DEBUG;
                }
                int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId);
                if (!permissionGranted) {
                    return AccessibilityManagerService.DEBUG;
                }
                IAccessibilityInteractionConnection connection = getConnectionLocked(resolvedWindowId);
                if (connection != null) {
                    if (!AccessibilityManagerService.this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                        partialInteractiveRegion = null;
                    }
                    int interrogatingPid = Binder.getCallingPid();
                    long identityToken = Binder.clearCallingIdentity();
                    MagnificationSpec spec = AccessibilityManagerService.this.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                    try {
                        connection.findAccessibilityNodeInfoByAccessibilityId(accessibilityNodeId, partialInteractiveRegion, interactionId, callback, this.mFetchFlags | flags, interrogatingPid, interrogatingTid, spec);
                        Binder.restoreCallingIdentity(identityToken);
                        return true;
                    } catch (RemoteException e) {
                        Binder.restoreCallingIdentity(identityToken);
                        return AccessibilityManagerService.DEBUG;
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(identityToken);
                        throw th;
                    }
                }
                return AccessibilityManagerService.DEBUG;
            }
        }

        public boolean findFocus(int accessibilityWindowId, long accessibilityNodeId, int focusType, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
            Region partialInteractiveRegion = AccessibilityManagerService.this.mTempRegion;
            synchronized (AccessibilityManagerService.this.mLock) {
                int resolvedUserId = AccessibilityManagerService.this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(-2);
                if (resolvedUserId != AccessibilityManagerService.this.mCurrentUserId) {
                    return AccessibilityManagerService.DEBUG;
                }
                int resolvedWindowId = resolveAccessibilityWindowIdForFindFocusLocked(accessibilityWindowId, focusType);
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId);
                if (!permissionGranted) {
                    return AccessibilityManagerService.DEBUG;
                }
                IAccessibilityInteractionConnection connection = getConnectionLocked(resolvedWindowId);
                if (connection != null) {
                    if (!AccessibilityManagerService.this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                        partialInteractiveRegion = null;
                    }
                    int interrogatingPid = Binder.getCallingPid();
                    long identityToken = Binder.clearCallingIdentity();
                    MagnificationSpec spec = AccessibilityManagerService.this.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                    try {
                        connection.findFocus(accessibilityNodeId, focusType, partialInteractiveRegion, interactionId, callback, this.mFetchFlags, interrogatingPid, interrogatingTid, spec);
                        Binder.restoreCallingIdentity(identityToken);
                        return true;
                    } catch (RemoteException e) {
                        Binder.restoreCallingIdentity(identityToken);
                        return AccessibilityManagerService.DEBUG;
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(identityToken);
                        throw th;
                    }
                }
                return AccessibilityManagerService.DEBUG;
            }
        }

        public boolean focusSearch(int accessibilityWindowId, long accessibilityNodeId, int direction, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
            Region partialInteractiveRegion = AccessibilityManagerService.this.mTempRegion;
            synchronized (AccessibilityManagerService.this.mLock) {
                int resolvedUserId = AccessibilityManagerService.this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(-2);
                if (resolvedUserId != AccessibilityManagerService.this.mCurrentUserId) {
                    return AccessibilityManagerService.DEBUG;
                }
                int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId);
                if (!permissionGranted) {
                    return AccessibilityManagerService.DEBUG;
                }
                IAccessibilityInteractionConnection connection = getConnectionLocked(resolvedWindowId);
                if (connection != null) {
                    if (!AccessibilityManagerService.this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                        partialInteractiveRegion = null;
                    }
                    int interrogatingPid = Binder.getCallingPid();
                    long identityToken = Binder.clearCallingIdentity();
                    MagnificationSpec spec = AccessibilityManagerService.this.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                    try {
                        connection.focusSearch(accessibilityNodeId, direction, partialInteractiveRegion, interactionId, callback, this.mFetchFlags, interrogatingPid, interrogatingTid, spec);
                        Binder.restoreCallingIdentity(identityToken);
                        return true;
                    } catch (RemoteException e) {
                        Binder.restoreCallingIdentity(identityToken);
                        return AccessibilityManagerService.DEBUG;
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(identityToken);
                        throw th;
                    }
                }
                return AccessibilityManagerService.DEBUG;
            }
        }

        public boolean performAccessibilityAction(int accessibilityWindowId, long accessibilityNodeId, int action, Bundle arguments, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
            synchronized (AccessibilityManagerService.this.mLock) {
                int resolvedUserId = AccessibilityManagerService.this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(-2);
                if (resolvedUserId != AccessibilityManagerService.this.mCurrentUserId) {
                    return AccessibilityManagerService.DEBUG;
                }
                int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId);
                if (!permissionGranted) {
                    return AccessibilityManagerService.DEBUG;
                }
                IAccessibilityInteractionConnection connection = getConnectionLocked(resolvedWindowId);
                if (connection == null) {
                    return AccessibilityManagerService.DEBUG;
                }
                int interrogatingPid = Binder.getCallingPid();
                long identityToken = Binder.clearCallingIdentity();
                try {
                    connection.performAccessibilityAction(accessibilityNodeId, action, arguments, interactionId, callback, this.mFetchFlags, interrogatingPid, interrogatingTid);
                } catch (RemoteException e) {
                } finally {
                    Binder.restoreCallingIdentity(identityToken);
                }
                return true;
            }
        }

        public boolean performGlobalAction(int action) {
            boolean z = AccessibilityManagerService.DEBUG;
            synchronized (AccessibilityManagerService.this.mLock) {
                int resolvedUserId = AccessibilityManagerService.this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(-2);
                if (resolvedUserId == AccessibilityManagerService.this.mCurrentUserId) {
                    long identity = Binder.clearCallingIdentity();
                    try {
                        switch (action) {
                            case 1:
                                sendDownAndUpKeyEvents(4);
                                Binder.restoreCallingIdentity(identity);
                                z = true;
                                break;
                            case 2:
                                sendDownAndUpKeyEvents(3);
                                Binder.restoreCallingIdentity(identity);
                                z = true;
                                break;
                            case 3:
                                openRecents();
                                Binder.restoreCallingIdentity(identity);
                                z = true;
                                break;
                            case 4:
                                expandNotifications();
                                Binder.restoreCallingIdentity(identity);
                                z = true;
                                break;
                            case 5:
                                expandQuickSettings();
                                Binder.restoreCallingIdentity(identity);
                                z = true;
                                break;
                            case 6:
                                showGlobalActions();
                                Binder.restoreCallingIdentity(identity);
                                z = true;
                                break;
                        }
                    } finally {
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            }
            return z;
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            AccessibilityManagerService.this.mSecurityPolicy.enforceCallingPermission("android.permission.DUMP", AccessibilityManagerService.FUNCTION_DUMP);
            synchronized (AccessibilityManagerService.this.mLock) {
                pw.append((CharSequence) ("Service[label=" + ((Object) this.mAccessibilityServiceInfo.getResolveInfo().loadLabel(AccessibilityManagerService.this.mContext.getPackageManager()))));
                pw.append((CharSequence) (", feedbackType" + AccessibilityServiceInfo.feedbackTypeToString(this.mFeedbackType)));
                pw.append((CharSequence) (", capabilities=" + this.mAccessibilityServiceInfo.getCapabilities()));
                pw.append((CharSequence) (", eventTypes=" + AccessibilityEvent.eventTypeToString(this.mEventTypes)));
                pw.append((CharSequence) (", notificationTimeout=" + this.mNotificationTimeout));
                pw.append("]");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }

        public void onAdded() throws RemoteException {
            linkToOwnDeathLocked();
            long identity = Binder.clearCallingIdentity();
            try {
                AccessibilityManagerService.this.mWindowManagerService.addWindowToken(this.mOverlayWindowToken, 2032);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void onRemoved() {
            long identity = Binder.clearCallingIdentity();
            try {
                AccessibilityManagerService.this.mWindowManagerService.removeWindowToken(this.mOverlayWindowToken, true);
                Binder.restoreCallingIdentity(identity);
                unlinkToOwnDeathLocked();
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
        }

        public void linkToOwnDeathLocked() throws RemoteException {
            this.mService.linkToDeath(this, 0);
        }

        public void unlinkToOwnDeathLocked() {
            this.mService.unlinkToDeath(this, 0);
        }

        public void resetLocked() {
            try {
                this.mServiceInterface.init((IAccessibilityServiceConnection) null, this.mId, (IBinder) null);
            } catch (RemoteException e) {
            }
            this.mService = null;
            this.mServiceInterface = null;
        }

        public boolean isConnectedLocked() {
            if (this.mService != null) {
                return true;
            }
            return AccessibilityManagerService.DEBUG;
        }

        @Override
        public void binderDied() {
            synchronized (AccessibilityManagerService.this.mLock) {
                if (isConnectedLocked()) {
                    this.mWasConnectedAndDied = true;
                    this.mKeyEventDispatcher.flush();
                    UserState userState = AccessibilityManagerService.this.getUserStateLocked(this.mUserId);
                    AccessibilityManagerService.this.removeServiceLocked(this, userState);
                    resetLocked();
                    if (this.mIsAutomation) {
                        userState.mInstalledServices.remove(this.mAccessibilityServiceInfo);
                        userState.mEnabledServices.remove(this.mComponentName);
                        userState.destroyUiAutomationService();
                        if (AccessibilityManagerService.this.readConfigurationForUserStateLocked(userState)) {
                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                        }
                    }
                }
            }
        }

        public void notifyAccessibilityEvent(AccessibilityEvent event) {
            synchronized (AccessibilityManagerService.this.mLock) {
                int eventType = event.getEventType();
                AccessibilityEvent newEvent = AccessibilityEvent.obtain(event);
                AccessibilityEvent oldEvent = this.mPendingEvents.get(eventType);
                this.mPendingEvents.put(eventType, newEvent);
                if (oldEvent != null) {
                    this.mEventDispatchHandler.removeMessages(eventType);
                    oldEvent.recycle();
                }
                Message message = this.mEventDispatchHandler.obtainMessage(eventType);
                this.mEventDispatchHandler.sendMessageDelayed(message, this.mNotificationTimeout);
            }
        }

        private void notifyAccessibilityEventInternal(int eventType) {
            synchronized (AccessibilityManagerService.this.mLock) {
                IAccessibilityServiceClient listener = this.mServiceInterface;
                if (listener != null) {
                    AccessibilityEvent event = this.mPendingEvents.get(eventType);
                    if (event != null) {
                        this.mPendingEvents.remove(eventType);
                        if (AccessibilityManagerService.this.mSecurityPolicy.canRetrieveWindowContentLocked(this)) {
                            event.setConnectionId(this.mId);
                        } else {
                            event.setSource(null);
                        }
                        event.setSealed(true);
                        try {
                            listener.onAccessibilityEvent(event);
                        } catch (RemoteException re) {
                            Slog.e(AccessibilityManagerService.LOG_TAG, "Error during sending " + event + " to " + listener, re);
                        } finally {
                            event.recycle();
                        }
                    }
                }
            }
        }

        public void notifyGesture(int gestureId) {
            this.mInvocationHandler.obtainMessage(1, gestureId, 0).sendToTarget();
        }

        public void notifyKeyEvent(KeyEvent event, int policyFlags) {
            this.mInvocationHandler.obtainMessage(2, policyFlags, 0, event).sendToTarget();
        }

        public void notifyClearAccessibilityNodeInfoCache() {
            this.mInvocationHandler.sendEmptyMessage(3);
        }

        private void notifyGestureInternal(int gestureId) {
            IAccessibilityServiceClient listener;
            synchronized (AccessibilityManagerService.this.mLock) {
                listener = this.mServiceInterface;
            }
            if (listener != null) {
                try {
                    listener.onGesture(gestureId);
                } catch (RemoteException re) {
                    Slog.e(AccessibilityManagerService.LOG_TAG, "Error during sending gesture " + gestureId + " to " + this.mService, re);
                }
            }
        }

        private void notifyKeyEventInternal(KeyEvent event, int policyFlags) {
            this.mKeyEventDispatcher.notifyKeyEvent(event, policyFlags);
        }

        private void notifyClearAccessibilityCacheInternal() {
            IAccessibilityServiceClient listener;
            synchronized (AccessibilityManagerService.this.mLock) {
                listener = this.mServiceInterface;
            }
            if (listener != null) {
                try {
                    listener.clearAccessibilityCache();
                } catch (RemoteException re) {
                    Slog.e(AccessibilityManagerService.LOG_TAG, "Error during requesting accessibility info cache to be cleared.", re);
                }
            }
        }

        private void sendDownAndUpKeyEvents(int keyCode) {
            long token = Binder.clearCallingIdentity();
            long downTime = SystemClock.uptimeMillis();
            KeyEvent down = KeyEvent.obtain(downTime, downTime, 0, keyCode, 0, 0, -1, 0, 8, 257, null);
            InputManager.getInstance().injectInputEvent(down, 0);
            down.recycle();
            long upTime = SystemClock.uptimeMillis();
            KeyEvent up = KeyEvent.obtain(downTime, upTime, 1, keyCode, 0, 0, -1, 0, 8, 257, null);
            InputManager.getInstance().injectInputEvent(up, 0);
            up.recycle();
            Binder.restoreCallingIdentity(token);
        }

        private void expandNotifications() {
            long token = Binder.clearCallingIdentity();
            StatusBarManager statusBarManager = (StatusBarManager) AccessibilityManagerService.this.mContext.getSystemService("statusbar");
            statusBarManager.expandNotificationsPanel();
            Binder.restoreCallingIdentity(token);
        }

        private void expandQuickSettings() {
            long token = Binder.clearCallingIdentity();
            StatusBarManager statusBarManager = (StatusBarManager) AccessibilityManagerService.this.mContext.getSystemService("statusbar");
            statusBarManager.expandSettingsPanel();
            Binder.restoreCallingIdentity(token);
        }

        private void openRecents() {
            long token = Binder.clearCallingIdentity();
            IStatusBarService statusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
            try {
                statusBarService.toggleRecentApps();
            } catch (RemoteException e) {
                Slog.e(AccessibilityManagerService.LOG_TAG, "Error toggling recent apps.");
            }
            Binder.restoreCallingIdentity(token);
        }

        private void showGlobalActions() {
            AccessibilityManagerService.this.mWindowManagerService.showGlobalActions();
        }

        private IAccessibilityInteractionConnection getConnectionLocked(int windowId) {
            AccessibilityConnectionWrapper wrapper = (AccessibilityConnectionWrapper) AccessibilityManagerService.this.mGlobalInteractionConnections.get(windowId);
            if (wrapper == null) {
                wrapper = AccessibilityManagerService.this.getCurrentUserStateLocked().mInteractionConnections.get(windowId);
            }
            if (wrapper == null || wrapper.mConnection == null) {
                return null;
            }
            return wrapper.mConnection;
        }

        private int resolveAccessibilityWindowIdLocked(int accessibilityWindowId) {
            if (accessibilityWindowId == Integer.MAX_VALUE) {
                return AccessibilityManagerService.this.mSecurityPolicy.getActiveWindowId();
            }
            return accessibilityWindowId;
        }

        private int resolveAccessibilityWindowIdForFindFocusLocked(int windowId, int focusType) {
            if (windowId == Integer.MAX_VALUE) {
                return AccessibilityManagerService.this.mSecurityPolicy.mActiveWindowId;
            }
            if (windowId == -2) {
                if (focusType == 1) {
                    return AccessibilityManagerService.this.mSecurityPolicy.mFocusedWindowId;
                }
                if (focusType == 2) {
                    return AccessibilityManagerService.this.mSecurityPolicy.mAccessibilityFocusedWindowId;
                }
                return windowId;
            }
            return windowId;
        }

        private final class InvocationHandler extends Handler {
            public static final int MSG_CLEAR_ACCESSIBILITY_CACHE = 3;
            public static final int MSG_ON_GESTURE = 1;
            public static final int MSG_ON_KEY_EVENT = 2;
            public static final int MSG_ON_KEY_EVENT_TIMEOUT = 4;

            public InvocationHandler(Looper looper) {
                super(looper, null, true);
            }

            @Override
            public void handleMessage(Message message) {
                int type = message.what;
                switch (type) {
                    case 1:
                        int gestureId = message.arg1;
                        Service.this.notifyGestureInternal(gestureId);
                        return;
                    case 2:
                        KeyEvent event = (KeyEvent) message.obj;
                        int policyFlags = message.arg1;
                        Service.this.notifyKeyEventInternal(event, policyFlags);
                        return;
                    case 3:
                        Service.this.notifyClearAccessibilityCacheInternal();
                        return;
                    case 4:
                        PendingEvent eventState = (PendingEvent) message.obj;
                        Service.this.setOnKeyEventResult(AccessibilityManagerService.DEBUG, eventState.sequence);
                        return;
                    default:
                        throw new IllegalArgumentException("Unknown message: " + type);
                }
            }
        }

        private final class KeyEventDispatcher {
            private static final long ON_KEY_EVENT_TIMEOUT_MILLIS = 500;
            private PendingEvent mPendingEvents;
            private final InputEventConsistencyVerifier mSentEventsVerifier;

            private KeyEventDispatcher() {
                this.mSentEventsVerifier = InputEventConsistencyVerifier.isInstrumentationEnabled() ? new InputEventConsistencyVerifier(this, 0, KeyEventDispatcher.class.getSimpleName()) : null;
            }

            public void notifyKeyEvent(KeyEvent event, int policyFlags) {
                PendingEvent pendingEvent;
                synchronized (AccessibilityManagerService.this.mLock) {
                    pendingEvent = addPendingEventLocked(event, policyFlags);
                }
                Message message = Service.this.mInvocationHandler.obtainMessage(4, pendingEvent);
                Service.this.mInvocationHandler.sendMessageDelayed(message, ON_KEY_EVENT_TIMEOUT_MILLIS);
                try {
                    Service.this.mServiceInterface.onKeyEvent(pendingEvent.event, pendingEvent.sequence);
                } catch (RemoteException e) {
                    setOnKeyEventResult(AccessibilityManagerService.DEBUG, pendingEvent.sequence);
                }
            }

            public void setOnKeyEventResult(boolean handled, int sequence) {
                synchronized (AccessibilityManagerService.this.mLock) {
                    PendingEvent pendingEvent = removePendingEventLocked(sequence);
                    if (pendingEvent != null) {
                        Service.this.mInvocationHandler.removeMessages(4, pendingEvent);
                        pendingEvent.handled = handled;
                        finishPendingEventLocked(pendingEvent);
                    }
                }
            }

            public void flush() {
                synchronized (AccessibilityManagerService.this.mLock) {
                    cancelAllPendingEventsLocked();
                    if (this.mSentEventsVerifier != null) {
                        this.mSentEventsVerifier.reset();
                    }
                }
            }

            private PendingEvent addPendingEventLocked(KeyEvent event, int policyFlags) {
                int sequence = event.getSequenceNumber();
                PendingEvent pendingEvent = AccessibilityManagerService.this.obtainPendingEventLocked(event, policyFlags, sequence);
                pendingEvent.next = this.mPendingEvents;
                this.mPendingEvents = pendingEvent;
                return pendingEvent;
            }

            private PendingEvent removePendingEventLocked(int sequence) {
                PendingEvent previous = null;
                for (PendingEvent current = this.mPendingEvents; current != null; current = current.next) {
                    if (current.sequence == sequence) {
                        if (previous != null) {
                            previous.next = current.next;
                        } else {
                            this.mPendingEvents = current.next;
                        }
                        current.next = null;
                        return current;
                    }
                    previous = current;
                }
                return null;
            }

            private void finishPendingEventLocked(PendingEvent pendingEvent) {
                if (!pendingEvent.handled) {
                    sendKeyEventToInputFilter(pendingEvent.event, pendingEvent.policyFlags);
                }
                pendingEvent.event = null;
                AccessibilityManagerService.this.recyclePendingEventLocked(pendingEvent);
            }

            private void sendKeyEventToInputFilter(KeyEvent event, int policyFlags) {
                if (this.mSentEventsVerifier != null) {
                    this.mSentEventsVerifier.onKeyEvent(event, 0);
                }
                AccessibilityManagerService.this.mMainHandler.obtainMessage(8, policyFlags | 1073741824, 0, event).sendToTarget();
            }

            private void cancelAllPendingEventsLocked() {
                while (this.mPendingEvents != null) {
                    PendingEvent pendingEvent = removePendingEventLocked(this.mPendingEvents.sequence);
                    pendingEvent.handled = AccessibilityManagerService.DEBUG;
                    Service.this.mInvocationHandler.removeMessages(4, pendingEvent);
                    finishPendingEventLocked(pendingEvent);
                }
            }
        }
    }

    private static final class PendingEvent {
        KeyEvent event;
        boolean handled;
        PendingEvent next;
        int policyFlags;
        int sequence;

        private PendingEvent() {
        }

        public void clear() {
            if (this.event != null) {
                this.event.recycle();
                this.event = null;
            }
            this.next = null;
            this.policyFlags = 0;
            this.sequence = 0;
            this.handled = AccessibilityManagerService.DEBUG;
        }
    }

    final class WindowsForAccessibilityCallback implements WindowManagerInternal.WindowsForAccessibilityCallback {
        WindowsForAccessibilityCallback() {
        }

        public void onWindowsForAccessibilityChanged(List<WindowInfo> windows) {
            synchronized (AccessibilityManagerService.this.mLock) {
                List<AccessibilityWindowInfo> reportedWindows = new ArrayList<>();
                int receivedWindowCount = windows.size();
                for (int i = 0; i < receivedWindowCount; i++) {
                    WindowInfo receivedWindow = windows.get(i);
                    AccessibilityWindowInfo reportedWindow = populateReportedWindow(receivedWindow);
                    if (reportedWindow != null) {
                        reportedWindows.add(reportedWindow);
                    }
                }
                AccessibilityManagerService.this.mSecurityPolicy.updateWindowsLocked(reportedWindows);
                AccessibilityManagerService.this.mLock.notifyAll();
            }
        }

        private AccessibilityWindowInfo populateReportedWindow(WindowInfo window) {
            int windowId = AccessibilityManagerService.this.findWindowIdLocked(window.token);
            if (windowId < 0) {
                return null;
            }
            AccessibilityWindowInfo reportedWindow = AccessibilityWindowInfo.obtain();
            reportedWindow.setId(windowId);
            reportedWindow.setType(getTypeForWindowManagerWindowType(window.type));
            reportedWindow.setLayer(window.layer);
            reportedWindow.setFocused(window.focused);
            reportedWindow.setBoundsInScreen(window.boundsInScreen);
            int parentId = AccessibilityManagerService.this.findWindowIdLocked(window.parentToken);
            if (parentId >= 0) {
                reportedWindow.setParentId(parentId);
            }
            if (window.childTokens != null) {
                int childCount = window.childTokens.size();
                for (int i = 0; i < childCount; i++) {
                    IBinder childToken = (IBinder) window.childTokens.get(i);
                    int childId = AccessibilityManagerService.this.findWindowIdLocked(childToken);
                    if (childId >= 0) {
                        reportedWindow.addChild(childId);
                    }
                }
                return reportedWindow;
            }
            return reportedWindow;
        }

        private int getTypeForWindowManagerWindowType(int windowType) {
            switch (windowType) {
                case 1:
                case 2:
                case 3:
                case 1000:
                case 1001:
                case 1002:
                case 1003:
                case 2002:
                case 2005:
                case 2007:
                    return 1;
                case 2000:
                case 2001:
                case 2003:
                case 2006:
                case 2008:
                case 2009:
                case 2010:
                case 2014:
                case 2017:
                case 2019:
                case 2020:
                case 2024:
                    return 3;
                case 2011:
                case 2012:
                    return 2;
                case 2032:
                    return 4;
                default:
                    return -1;
            }
        }
    }

    private final class InteractionBridge {
        private final AccessibilityInteractionClient mClient;
        private final int mConnectionId;
        private final Display mDefaultDisplay;

        public InteractionBridge() {
            AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            info.setCapabilities(1);
            info.flags |= 64;
            info.flags |= 2;
            Service service = AccessibilityManagerService.this.new Service(-10000, AccessibilityManagerService.sFakeAccessibilityServiceComponentName, info);
            this.mConnectionId = service.mId;
            this.mClient = AccessibilityInteractionClient.getInstance();
            this.mClient.addConnection(this.mConnectionId, service);
            DisplayManager displayManager = (DisplayManager) AccessibilityManagerService.this.mContext.getSystemService("display");
            this.mDefaultDisplay = displayManager.getDisplay(0);
        }

        public void clearAccessibilityFocusNotLocked(int windowId) {
            AccessibilityNodeInfo focus = getAccessibilityFocusNotLocked(windowId);
            if (focus != null) {
                focus.performAction(128);
            }
        }

        public boolean getAccessibilityFocusClickPointInScreenNotLocked(Point outPoint) {
            AccessibilityNodeInfo focus = getAccessibilityFocusNotLocked();
            if (focus == null) {
                return AccessibilityManagerService.DEBUG;
            }
            synchronized (AccessibilityManagerService.this.mLock) {
                Rect boundsInScreen = AccessibilityManagerService.this.mTempRect;
                focus.getBoundsInScreen(boundsInScreen);
                Rect windowBounds = AccessibilityManagerService.this.mTempRect1;
                AccessibilityManagerService.this.getWindowBounds(focus.getWindowId(), windowBounds);
                boundsInScreen.intersect(windowBounds);
                if (boundsInScreen.isEmpty()) {
                    return AccessibilityManagerService.DEBUG;
                }
                MagnificationSpec spec = AccessibilityManagerService.this.getCompatibleMagnificationSpecLocked(focus.getWindowId());
                if (spec != null && !spec.isNop()) {
                    boundsInScreen.offset((int) (-spec.offsetX), (int) (-spec.offsetY));
                    boundsInScreen.scale(1.0f / spec.scale);
                }
                Point screenSize = AccessibilityManagerService.this.mTempPoint;
                this.mDefaultDisplay.getRealSize(screenSize);
                boundsInScreen.intersect(0, 0, screenSize.x, screenSize.y);
                if (boundsInScreen.isEmpty()) {
                    return AccessibilityManagerService.DEBUG;
                }
                outPoint.set(boundsInScreen.centerX(), boundsInScreen.centerY());
                return true;
            }
        }

        private AccessibilityNodeInfo getAccessibilityFocusNotLocked() {
            synchronized (AccessibilityManagerService.this.mLock) {
                int focusedWindowId = AccessibilityManagerService.this.mSecurityPolicy.mAccessibilityFocusedWindowId;
                if (focusedWindowId == -1) {
                    return null;
                }
                return getAccessibilityFocusNotLocked(focusedWindowId);
            }
        }

        private AccessibilityNodeInfo getAccessibilityFocusNotLocked(int windowId) {
            return this.mClient.findFocus(this.mConnectionId, windowId, AccessibilityNodeInfo.ROOT_NODE_ID, 2);
        }
    }

    final class SecurityPolicy {
        public static final int INVALID_WINDOW_ID = -1;
        private static final int RETRIEVAL_ALLOWING_EVENT_TYPES = 244159;
        private boolean mTouchInteractionInProgress;
        public List<AccessibilityWindowInfo> mWindows;
        public int mActiveWindowId = -1;
        public int mFocusedWindowId = -1;
        public int mAccessibilityFocusedWindowId = -1;
        public long mAccessibilityFocusNodeId = 2147483647L;

        SecurityPolicy() {
        }

        private boolean canDispatchAccessibilityEventLocked(AccessibilityEvent event) {
            int eventType = event.getEventType();
            switch (eventType) {
                case 32:
                case 64:
                case 128:
                case PackageManagerService.DumpState.DUMP_VERIFIERS:
                case 512:
                case 1024:
                case 16384:
                case 262144:
                case 524288:
                case 1048576:
                case 2097152:
                case 4194304:
                    return true;
                default:
                    return isRetrievalAllowingWindow(event.getWindowId());
            }
        }

        public void clearWindowsLocked() {
            List<AccessibilityWindowInfo> windows = Collections.emptyList();
            int activeWindowId = this.mActiveWindowId;
            updateWindowsLocked(windows);
            this.mActiveWindowId = activeWindowId;
            this.mWindows = null;
        }

        public void updateWindowsLocked(List<AccessibilityWindowInfo> windows) {
            if (this.mWindows == null) {
                this.mWindows = new ArrayList();
            }
            int oldWindowCount = this.mWindows.size();
            for (int i = oldWindowCount - 1; i >= 0; i--) {
                this.mWindows.remove(i).recycle();
            }
            this.mFocusedWindowId = -1;
            if (!this.mTouchInteractionInProgress) {
                this.mActiveWindowId = -1;
            }
            boolean activeWindowGone = true;
            int windowCount = windows.size();
            if (windowCount > 0) {
                for (int i2 = 0; i2 < windowCount; i2++) {
                    AccessibilityWindowInfo window = windows.get(i2);
                    int windowId = window.getId();
                    if (window.isFocused()) {
                        this.mFocusedWindowId = windowId;
                        if (!this.mTouchInteractionInProgress) {
                            this.mActiveWindowId = windowId;
                            window.setActive(true);
                        } else if (windowId == this.mActiveWindowId) {
                            activeWindowGone = AccessibilityManagerService.DEBUG;
                        }
                    }
                    this.mWindows.add(window);
                }
                if (this.mTouchInteractionInProgress && activeWindowGone) {
                    this.mActiveWindowId = this.mFocusedWindowId;
                }
                for (int i3 = 0; i3 < windowCount; i3++) {
                    AccessibilityWindowInfo window2 = this.mWindows.get(i3);
                    if (window2.getId() == this.mActiveWindowId) {
                        window2.setActive(true);
                    }
                    if (window2.getId() == this.mAccessibilityFocusedWindowId) {
                        window2.setAccessibilityFocused(true);
                    }
                }
            }
            notifyWindowsChanged();
        }

        public boolean computePartialInteractiveRegionForWindowLocked(int windowId, Region outRegion) {
            if (this.mWindows == null) {
                return AccessibilityManagerService.DEBUG;
            }
            Region windowInteractiveRegion = null;
            boolean windowInteractiveRegionChanged = AccessibilityManagerService.DEBUG;
            int windowCount = this.mWindows.size();
            for (int i = windowCount - 1; i >= 0; i--) {
                AccessibilityWindowInfo currentWindow = this.mWindows.get(i);
                if (windowInteractiveRegion == null) {
                    if (currentWindow.getId() == windowId) {
                        Rect currentWindowBounds = AccessibilityManagerService.this.mTempRect;
                        currentWindow.getBoundsInScreen(currentWindowBounds);
                        outRegion.set(currentWindowBounds);
                        windowInteractiveRegion = outRegion;
                    }
                } else if (currentWindow.getType() != 4) {
                    Rect currentWindowBounds2 = AccessibilityManagerService.this.mTempRect;
                    currentWindow.getBoundsInScreen(currentWindowBounds2);
                    if (windowInteractiveRegion.op(currentWindowBounds2, Region.Op.DIFFERENCE)) {
                        windowInteractiveRegionChanged = true;
                    }
                }
            }
            return windowInteractiveRegionChanged;
        }

        public void updateEventSourceLocked(AccessibilityEvent event) {
            if ((event.getEventType() & RETRIEVAL_ALLOWING_EVENT_TYPES) == 0) {
                event.setSource(null);
            }
        }

        public void updateActiveAndAccessibilityFocusedWindowLocked(int windowId, long nodeId, int eventType) {
            switch (eventType) {
                case 32:
                    synchronized (AccessibilityManagerService.this.mLock) {
                        if (AccessibilityManagerService.this.mWindowsForAccessibilityCallback == null) {
                            this.mFocusedWindowId = getFocusedWindowId();
                            if (windowId == this.mFocusedWindowId) {
                                this.mActiveWindowId = windowId;
                            }
                        }
                        break;
                    }
                    return;
                case 128:
                    synchronized (AccessibilityManagerService.this.mLock) {
                        if (this.mTouchInteractionInProgress && this.mActiveWindowId != windowId) {
                            setActiveWindowLocked(windowId);
                        }
                        break;
                    }
                    return;
                case 32768:
                    synchronized (AccessibilityManagerService.this.mLock) {
                        if (this.mAccessibilityFocusedWindowId != windowId) {
                            AccessibilityManagerService.this.mMainHandler.obtainMessage(9, this.mAccessibilityFocusedWindowId, 0).sendToTarget();
                            AccessibilityManagerService.this.mSecurityPolicy.setAccessibilityFocusedWindowLocked(windowId);
                            this.mAccessibilityFocusNodeId = nodeId;
                        }
                        break;
                    }
                    return;
                case 65536:
                    synchronized (AccessibilityManagerService.this.mLock) {
                        if (this.mAccessibilityFocusNodeId == nodeId) {
                            this.mAccessibilityFocusNodeId = 2147483647L;
                        }
                        if (this.mAccessibilityFocusNodeId == 2147483647L && this.mAccessibilityFocusedWindowId == windowId) {
                            this.mAccessibilityFocusedWindowId = -1;
                        }
                        break;
                    }
                    return;
                default:
                    return;
            }
        }

        public void onTouchInteractionStart() {
            synchronized (AccessibilityManagerService.this.mLock) {
                this.mTouchInteractionInProgress = true;
            }
        }

        public void onTouchInteractionEnd() {
            synchronized (AccessibilityManagerService.this.mLock) {
                this.mTouchInteractionInProgress = AccessibilityManagerService.DEBUG;
                int oldActiveWindow = AccessibilityManagerService.this.mSecurityPolicy.mActiveWindowId;
                setActiveWindowLocked(this.mFocusedWindowId);
                if (oldActiveWindow != AccessibilityManagerService.this.mSecurityPolicy.mActiveWindowId && this.mAccessibilityFocusedWindowId == oldActiveWindow && AccessibilityManagerService.this.getCurrentUserStateLocked().mAccessibilityFocusOnlyInActiveWindow) {
                    AccessibilityManagerService.this.mMainHandler.obtainMessage(9, oldActiveWindow, 0).sendToTarget();
                }
            }
        }

        public int getActiveWindowId() {
            if (this.mActiveWindowId == -1 && !this.mTouchInteractionInProgress) {
                this.mActiveWindowId = getFocusedWindowId();
            }
            return this.mActiveWindowId;
        }

        private void setActiveWindowLocked(int windowId) {
            if (this.mActiveWindowId != windowId) {
                this.mActiveWindowId = windowId;
                if (this.mWindows != null) {
                    int windowCount = this.mWindows.size();
                    for (int i = 0; i < windowCount; i++) {
                        AccessibilityWindowInfo window = this.mWindows.get(i);
                        window.setActive(window.getId() == windowId ? true : AccessibilityManagerService.DEBUG);
                    }
                }
                notifyWindowsChanged();
            }
        }

        private void setAccessibilityFocusedWindowLocked(int windowId) {
            if (this.mAccessibilityFocusedWindowId != windowId) {
                this.mAccessibilityFocusedWindowId = windowId;
                if (this.mWindows != null) {
                    int windowCount = this.mWindows.size();
                    for (int i = 0; i < windowCount; i++) {
                        AccessibilityWindowInfo window = this.mWindows.get(i);
                        window.setAccessibilityFocused(window.getId() == windowId ? true : AccessibilityManagerService.DEBUG);
                    }
                }
                notifyWindowsChanged();
            }
        }

        private void notifyWindowsChanged() {
            if (AccessibilityManagerService.this.mWindowsForAccessibilityCallback != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    AccessibilityEvent event = AccessibilityEvent.obtain(4194304);
                    event.setEventTime(SystemClock.uptimeMillis());
                    AccessibilityManagerService.this.sendAccessibilityEvent(event, AccessibilityManagerService.this.mCurrentUserId);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public boolean canGetAccessibilityNodeInfoLocked(Service service, int windowId) {
            if (canRetrieveWindowContentLocked(service) && isRetrievalAllowingWindow(windowId)) {
                return true;
            }
            return AccessibilityManagerService.DEBUG;
        }

        public boolean canRetrieveWindowsLocked(Service service) {
            if (canRetrieveWindowContentLocked(service) && service.mRetrieveInteractiveWindows) {
                return true;
            }
            return AccessibilityManagerService.DEBUG;
        }

        public boolean canRetrieveWindowContentLocked(Service service) {
            if ((service.mAccessibilityServiceInfo.getCapabilities() & 1) != 0) {
                return true;
            }
            return AccessibilityManagerService.DEBUG;
        }

        private int resolveProfileParentLocked(int userId) {
            if (userId != AccessibilityManagerService.this.mCurrentUserId) {
                long identity = Binder.clearCallingIdentity();
                try {
                    UserInfo parent = AccessibilityManagerService.this.mUserManager.getProfileParent(userId);
                    if (parent != null) {
                        userId = parent.getUserHandle().getIdentifier();
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
            return userId;
        }

        public int resolveCallingUserIdEnforcingPermissionsLocked(int userId) {
            int callingUid = Binder.getCallingUid();
            if (callingUid == 0 || callingUid == 1000 || callingUid == 2000) {
                if (userId == -2 || userId == -3) {
                    return AccessibilityManagerService.this.mCurrentUserId;
                }
                return resolveProfileParentLocked(userId);
            }
            int callingUserId = UserHandle.getUserId(callingUid);
            if (callingUserId == userId) {
                return resolveProfileParentLocked(userId);
            }
            int callingUserParentId = resolveProfileParentLocked(callingUserId);
            if (callingUserParentId == AccessibilityManagerService.this.mCurrentUserId && (userId == -2 || userId == -3)) {
                return AccessibilityManagerService.this.mCurrentUserId;
            }
            if (!hasPermission("android.permission.INTERACT_ACROSS_USERS") && !hasPermission("android.permission.INTERACT_ACROSS_USERS_FULL")) {
                throw new SecurityException("Call from user " + callingUserId + " as user " + userId + " without permission INTERACT_ACROSS_USERS or INTERACT_ACROSS_USERS_FULL not allowed.");
            }
            if (userId == -2 || userId == -3) {
                return AccessibilityManagerService.this.mCurrentUserId;
            }
            throw new IllegalArgumentException("Calling user can be changed to only UserHandle.USER_CURRENT or UserHandle.USER_CURRENT_OR_SELF.");
        }

        public boolean isCallerInteractingAcrossUsers(int userId) {
            int callingUid = Binder.getCallingUid();
            if (Binder.getCallingPid() == Process.myPid() || callingUid == 2000 || userId == -2 || userId == -3) {
                return true;
            }
            return AccessibilityManagerService.DEBUG;
        }

        private boolean isRetrievalAllowingWindow(int windowId) {
            if (Binder.getCallingUid() == 1000 || windowId == this.mActiveWindowId || findWindowById(windowId) != null) {
                return true;
            }
            return AccessibilityManagerService.DEBUG;
        }

        private AccessibilityWindowInfo findWindowById(int windowId) {
            if (this.mWindows != null) {
                int windowCount = this.mWindows.size();
                for (int i = 0; i < windowCount; i++) {
                    AccessibilityWindowInfo window = this.mWindows.get(i);
                    if (window.getId() == windowId) {
                        return window;
                    }
                }
            }
            return null;
        }

        private void enforceCallingPermission(String permission, String function) {
            if (AccessibilityManagerService.OWN_PROCESS_ID != Binder.getCallingPid() && !hasPermission(permission)) {
                throw new SecurityException("You do not have " + permission + " required to call " + function + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            }
        }

        private boolean hasPermission(String permission) {
            if (AccessibilityManagerService.this.mContext.checkCallingPermission(permission) == 0) {
                return true;
            }
            return AccessibilityManagerService.DEBUG;
        }

        private int getFocusedWindowId() {
            int iFindWindowIdLocked;
            IBinder token = AccessibilityManagerService.this.mWindowManagerService.getFocusedWindowToken();
            synchronized (AccessibilityManagerService.this.mLock) {
                iFindWindowIdLocked = AccessibilityManagerService.this.findWindowIdLocked(token);
            }
            return iFindWindowIdLocked;
        }
    }

    private class UserState {
        public boolean mAccessibilityFocusOnlyInActiveWindow;
        public boolean mHasDisplayColorAdjustment;
        public boolean mIsAccessibilityEnabled;
        public boolean mIsDisplayMagnificationEnabled;
        public boolean mIsEnhancedWebAccessibilityEnabled;
        public boolean mIsFilterKeyEventsEnabled;
        public boolean mIsTextHighContrastEnabled;
        public boolean mIsTouchExplorationEnabled;
        private Service mUiAutomationService;
        private IAccessibilityServiceClient mUiAutomationServiceClient;
        private IBinder mUiAutomationServiceOwner;
        public final int mUserId;
        public final RemoteCallbackList<IAccessibilityManagerClient> mClients = new RemoteCallbackList<>();
        public final SparseArray<AccessibilityConnectionWrapper> mInteractionConnections = new SparseArray<>();
        public final SparseArray<IBinder> mWindowTokens = new SparseArray<>();
        public final CopyOnWriteArrayList<Service> mBoundServices = new CopyOnWriteArrayList<>();
        public final Map<ComponentName, Service> mComponentNameToServiceMap = new HashMap();
        public final List<AccessibilityServiceInfo> mInstalledServices = new ArrayList();
        public final Set<ComponentName> mBindingServices = new HashSet();
        public final Set<ComponentName> mEnabledServices = new HashSet();
        public final Set<ComponentName> mTouchExplorationGrantedServices = new HashSet();
        public int mHandledFeedbackTypes = 0;
        public int mLastSentClientState = -1;
        private final IBinder.DeathRecipient mUiAutomationSerivceOnwerDeathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                UserState.this.mUiAutomationServiceOwner.unlinkToDeath(UserState.this.mUiAutomationSerivceOnwerDeathRecipient, 0);
                UserState.this.mUiAutomationServiceOwner = null;
                if (UserState.this.mUiAutomationService != null) {
                    UserState.this.mUiAutomationService.binderDied();
                }
            }
        };

        public UserState(int userId) {
            this.mUserId = userId;
        }

        public int getClientState() {
            int clientState = 0;
            if (this.mIsAccessibilityEnabled) {
                clientState = 0 | 1;
            }
            if (this.mIsAccessibilityEnabled && this.mIsTouchExplorationEnabled) {
                clientState |= 2;
            }
            if (this.mIsTextHighContrastEnabled) {
                return clientState | 4;
            }
            return clientState;
        }

        public void onSwitchToAnotherUser() {
            if (this.mUiAutomationService != null) {
                this.mUiAutomationService.binderDied();
            }
            AccessibilityManagerService.this.unbindAllServicesLocked(this);
            this.mBoundServices.clear();
            this.mBindingServices.clear();
            this.mHandledFeedbackTypes = 0;
            this.mLastSentClientState = -1;
            this.mEnabledServices.clear();
            this.mTouchExplorationGrantedServices.clear();
            this.mIsAccessibilityEnabled = AccessibilityManagerService.DEBUG;
            this.mIsTouchExplorationEnabled = AccessibilityManagerService.DEBUG;
            this.mIsEnhancedWebAccessibilityEnabled = AccessibilityManagerService.DEBUG;
            this.mIsDisplayMagnificationEnabled = AccessibilityManagerService.DEBUG;
        }

        public void destroyUiAutomationService() {
            this.mUiAutomationService = null;
            this.mUiAutomationServiceClient = null;
            if (this.mUiAutomationServiceOwner != null) {
                this.mUiAutomationServiceOwner.unlinkToDeath(this.mUiAutomationSerivceOnwerDeathRecipient, 0);
                this.mUiAutomationServiceOwner = null;
            }
        }
    }

    private final class AccessibilityContentObserver extends ContentObserver {
        private final Uri mAccessibilityEnabledUri;
        private final Uri mDisplayDaltonizerEnabledUri;
        private final Uri mDisplayDaltonizerUri;
        private final Uri mDisplayInversionEnabledUri;
        private final Uri mDisplayMagnificationEnabledUri;
        private final Uri mEnabledAccessibilityServicesUri;
        private final Uri mEnhancedWebAccessibilityUri;
        private final Uri mHighTextContrastUri;
        private final Uri mTouchExplorationEnabledUri;
        private final Uri mTouchExplorationGrantedAccessibilityServicesUri;

        public AccessibilityContentObserver(Handler handler) {
            super(handler);
            this.mAccessibilityEnabledUri = Settings.Secure.getUriFor("accessibility_enabled");
            this.mTouchExplorationEnabledUri = Settings.Secure.getUriFor("touch_exploration_enabled");
            this.mDisplayMagnificationEnabledUri = Settings.Secure.getUriFor("accessibility_display_magnification_enabled");
            this.mEnabledAccessibilityServicesUri = Settings.Secure.getUriFor("enabled_accessibility_services");
            this.mTouchExplorationGrantedAccessibilityServicesUri = Settings.Secure.getUriFor("touch_exploration_granted_accessibility_services");
            this.mEnhancedWebAccessibilityUri = Settings.Secure.getUriFor("accessibility_script_injection");
            this.mDisplayInversionEnabledUri = Settings.Secure.getUriFor("accessibility_display_inversion_enabled");
            this.mDisplayDaltonizerEnabledUri = Settings.Secure.getUriFor("accessibility_display_daltonizer_enabled");
            this.mDisplayDaltonizerUri = Settings.Secure.getUriFor("accessibility_display_daltonizer");
            this.mHighTextContrastUri = Settings.Secure.getUriFor("high_text_contrast_enabled");
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(this.mAccessibilityEnabledUri, AccessibilityManagerService.DEBUG, this, -1);
            contentResolver.registerContentObserver(this.mTouchExplorationEnabledUri, AccessibilityManagerService.DEBUG, this, -1);
            contentResolver.registerContentObserver(this.mDisplayMagnificationEnabledUri, AccessibilityManagerService.DEBUG, this, -1);
            contentResolver.registerContentObserver(this.mEnabledAccessibilityServicesUri, AccessibilityManagerService.DEBUG, this, -1);
            contentResolver.registerContentObserver(this.mTouchExplorationGrantedAccessibilityServicesUri, AccessibilityManagerService.DEBUG, this, -1);
            contentResolver.registerContentObserver(this.mEnhancedWebAccessibilityUri, AccessibilityManagerService.DEBUG, this, -1);
            contentResolver.registerContentObserver(this.mDisplayInversionEnabledUri, AccessibilityManagerService.DEBUG, this, -1);
            contentResolver.registerContentObserver(this.mDisplayDaltonizerEnabledUri, AccessibilityManagerService.DEBUG, this, -1);
            contentResolver.registerContentObserver(this.mDisplayDaltonizerUri, AccessibilityManagerService.DEBUG, this, -1);
            contentResolver.registerContentObserver(this.mHighTextContrastUri, AccessibilityManagerService.DEBUG, this, -1);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (AccessibilityManagerService.this.mLock) {
                UserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                if (userState.mUiAutomationService == null) {
                    if (this.mAccessibilityEnabledUri.equals(uri)) {
                        if (AccessibilityManagerService.this.readAccessibilityEnabledSettingLocked(userState)) {
                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                        }
                    } else if (this.mTouchExplorationEnabledUri.equals(uri)) {
                        if (AccessibilityManagerService.this.readTouchExplorationEnabledSettingLocked(userState)) {
                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                        }
                    } else if (this.mDisplayMagnificationEnabledUri.equals(uri)) {
                        if (AccessibilityManagerService.this.readDisplayMagnificationEnabledSettingLocked(userState)) {
                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                        }
                    } else if (this.mEnabledAccessibilityServicesUri.equals(uri)) {
                        if (AccessibilityManagerService.this.readEnabledAccessibilityServicesLocked(userState)) {
                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                        }
                    } else if (this.mTouchExplorationGrantedAccessibilityServicesUri.equals(uri)) {
                        if (AccessibilityManagerService.this.readTouchExplorationGrantedAccessibilityServicesLocked(userState)) {
                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                        }
                    } else if (this.mEnhancedWebAccessibilityUri.equals(uri)) {
                        if (AccessibilityManagerService.this.readEnhancedWebAccessibilityEnabledChangedLocked(userState)) {
                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                        }
                    } else if (this.mDisplayInversionEnabledUri.equals(uri) || this.mDisplayDaltonizerEnabledUri.equals(uri) || this.mDisplayDaltonizerUri.equals(uri)) {
                        if (AccessibilityManagerService.this.readDisplayColorAdjustmentSettingsLocked(userState)) {
                            AccessibilityManagerService.this.updateDisplayColorAdjustmentSettingsLocked(userState);
                        }
                    } else if (this.mHighTextContrastUri.equals(uri) && AccessibilityManagerService.this.readHighTextContrastEnabledSettingLocked(userState)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                }
            }
        }
    }
}
