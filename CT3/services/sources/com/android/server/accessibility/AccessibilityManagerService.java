package com.android.server.accessibility;

import android.R;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.accessibilityservice.IAccessibilityServiceConnection;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.StatusBarManager;
import android.appwidget.AppWidgetManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.IWindow;
import android.view.KeyEvent;
import android.view.MagnificationSpec;
import android.view.MotionEvent;
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
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.pm.PackageManagerService;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.mediatek.anrmanager.ANRManager;
import com.mediatek.datashaping.DataShapingUtils;
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
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParserException;

public class AccessibilityManagerService extends IAccessibilityManager.Stub {
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final boolean DEBUG = false;
    private static final String FUNCTION_DUMP = "dump";
    private static final String FUNCTION_REGISTER_UI_TEST_AUTOMATION_SERVICE = "registerUiTestAutomationService";
    private static final String GET_WINDOW_TOKEN = "getWindowToken";
    private static final String LOG_TAG = "AccessibilityManagerService";
    public static final int MAGNIFICATION_GESTURE_HANDLER_ID = 0;
    private static final String TEMPORARY_ENABLE_ACCESSIBILITY_UNTIL_KEYGUARD_REMOVED = "temporaryEnableAccessibilityStateUntilKeyguardRemoved";
    private static final int WAIT_FOR_USER_STATE_FULLY_INITIALIZED_MILLIS = 3000;
    private static final int WAIT_MOTION_INJECTOR_TIMEOUT_MILLIS = 1000;
    private static final int WAIT_WINDOWS_TIMEOUT_MILLIS = 5000;
    private static final int WINDOW_ID_UNKNOWN = -1;
    private static int sNextWindowId;
    private AppWidgetManagerInternal mAppWidgetService;
    private final Context mContext;
    private AlertDialog mEnableTouchExplorationDialog;
    private boolean mHasInputFilter;
    private boolean mInitialized;
    private AccessibilityInputFilter mInputFilter;
    private InteractionBridge mInteractionBridge;
    private KeyEventDispatcher mKeyEventDispatcher;
    private MagnificationController mMagnificationController;
    private final MainHandler mMainHandler;
    private MotionEventInjector mMotionEventInjector;
    private final PackageManager mPackageManager;
    private final PowerManager mPowerManager;
    private final UserManager mUserManager;
    private WindowsForAccessibilityCallback mWindowsForAccessibilityCallback;
    private static final boolean IS_ENG_BUILD = "eng".equals(Build.TYPE);
    private static final ComponentName sFakeAccessibilityServiceComponentName = new ComponentName("foo.bar", "FakeService");
    private static final int OWN_PROCESS_ID = Process.myPid();
    private static int sIdCounter = 1;
    private final Object mLock = new Object();
    private final TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);
    private final List<AccessibilityServiceInfo> mEnabledServicesForFeedbackTempList = new ArrayList();
    private final Rect mTempRect = new Rect();
    private final Rect mTempRect1 = new Rect();
    private final Point mTempPoint = new Point();
    private final Set<ComponentName> mTempComponentNameSet = new HashSet();
    private final List<AccessibilityServiceInfo> mTempAccessibilityServiceInfoList = new ArrayList();
    private final RemoteCallbackList<IAccessibilityManagerClient> mGlobalClients = new RemoteCallbackList<>();
    private final SparseArray<RemoteAccessibilityConnection> mGlobalInteractionConnections = new SparseArray<>();
    private final SparseArray<IBinder> mGlobalWindowTokens = new SparseArray<>();
    private final SparseArray<UserState> mUserStates = new SparseArray<>();
    private int mCurrentUserId = 0;
    private final WindowManagerInternal mWindowManagerService = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
    private final SecurityPolicy mSecurityPolicy = new SecurityPolicy();

    private UserState getCurrentUserStateLocked() {
        return getUserStateLocked(this.mCurrentUserId);
    }

    public AccessibilityManagerService(Context context) {
        this.mContext = context;
        this.mPackageManager = this.mContext.getPackageManager();
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mMainHandler = new MainHandler(this.mContext.getMainLooper());
        registerBroadcastReceivers();
        new AccessibilityContentObserver(this.mMainHandler).register(context.getContentResolver());
        if (!SystemProperties.get("ro.mtk_ipo_support").equals("1")) {
            return;
        }
        registerIPOReceiver(context);
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
                    if (getChangingUserId() != AccessibilityManagerService.this.mCurrentUserId) {
                        return;
                    }
                    UserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                    userState.mInstalledServices.clear();
                    if (!userState.isUiAutomationSuppressingOtherServices() && AccessibilityManagerService.this.readConfigurationForUserStateLocked(userState)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                }
            }

            public void onPackageRemoved(String packageName, int uid) {
                synchronized (AccessibilityManagerService.this.mLock) {
                    int userId = getChangingUserId();
                    if (userId != AccessibilityManagerService.this.mCurrentUserId) {
                        return;
                    }
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
                            if (!userState.isUiAutomationSuppressingOtherServices()) {
                                AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                            }
                            return;
                        }
                    }
                }
            }

            public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
                synchronized (AccessibilityManagerService.this.mLock) {
                    int userId = getChangingUserId();
                    if (userId != AccessibilityManagerService.this.mCurrentUserId) {
                        return false;
                    }
                    UserState userState = AccessibilityManagerService.this.getUserStateLocked(userId);
                    Iterator<ComponentName> it = userState.mEnabledServices.iterator();
                    while (it.hasNext()) {
                        ComponentName comp = it.next();
                        String compPkg = comp.getPackageName();
                        for (String pkg : packages) {
                            if (compPkg.equals(pkg)) {
                                if (!doit) {
                                    return true;
                                }
                                it.remove();
                                AccessibilityManagerService.this.persistComponentNamesToSettingLocked("enabled_accessibility_services", userState.mEnabledServices, userId);
                                if (!userState.isUiAutomationSuppressingOtherServices()) {
                                    AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                                }
                            }
                        }
                    }
                    return false;
                }
            }
        };
        monitor.register(this.mContext, (Looper) null, UserHandle.ALL, true);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.addAction("android.os.action.SETTING_RESTORED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    AccessibilityManagerService.this.switchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                    return;
                }
                if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                    AccessibilityManagerService.this.unlockUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                    return;
                }
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    AccessibilityManagerService.this.removeUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                    return;
                }
                if ("android.intent.action.USER_PRESENT".equals(action)) {
                    UserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                    if (userState.isUiAutomationSuppressingOtherServices() || !AccessibilityManagerService.this.readConfigurationForUserStateLocked(userState)) {
                        return;
                    }
                    AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    return;
                }
                if (!"android.os.action.SETTING_RESTORED".equals(action)) {
                    return;
                }
                String which = intent.getStringExtra("setting_name");
                if (!"enabled_accessibility_services".equals(which)) {
                    return;
                }
                synchronized (AccessibilityManagerService.this.mLock) {
                    AccessibilityManagerService.this.restoreEnabledAccessibilityServicesLocked(intent.getStringExtra("previous_value"), intent.getStringExtra("new_value"));
                }
            }
        }, UserHandle.ALL, intentFilter, null, null);
    }

    public int addClient(IAccessibilityManagerClient client, int userId) {
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            UserState userState = getUserStateLocked(resolvedUserId);
            if (this.mSecurityPolicy.isCallerInteractingAcrossUsers(userId)) {
                this.mGlobalClients.register(client);
                return userState.getClientState();
            }
            userState.mClients.register(client);
            return resolvedUserId == this.mCurrentUserId ? userState.getClientState() : 0;
        }
    }

    public boolean sendAccessibilityEvent(AccessibilityEvent event, int userId) {
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            event.setPackageName(this.mSecurityPolicy.resolveValidReportedPackageLocked(event.getPackageName(), UserHandle.getCallingAppId(), resolvedUserId));
            if (resolvedUserId != this.mCurrentUserId) {
                return true;
            }
            if (this.mSecurityPolicy.canDispatchAccessibilityEventLocked(event)) {
                this.mSecurityPolicy.updateActiveAndAccessibilityFocusedWindowLocked(event.getWindowId(), event.getSourceNodeId(), event.getEventType(), event.getAction());
                this.mSecurityPolicy.updateEventSourceLocked(event);
                notifyAccessibilityServicesDelayedLocked(event, false);
                notifyAccessibilityServicesDelayedLocked(event, true);
            }
            if (this.mHasInputFilter && this.mInputFilter != null) {
                this.mMainHandler.obtainMessage(1, AccessibilityEvent.obtain(event)).sendToTarget();
            }
            event.recycle();
            return OWN_PROCESS_ID != Binder.getCallingPid();
        }
    }

    public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(int userId) {
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            UserState userState = getUserStateLocked(resolvedUserId);
            if (userState.mUiAutomationService != null) {
                List<AccessibilityServiceInfo> installedServices = new ArrayList<>();
                installedServices.addAll(userState.mInstalledServices);
                installedServices.remove(userState.mUiAutomationService.mAccessibilityServiceInfo);
                return installedServices;
            }
            return userState.mInstalledServices;
        }
    }

    public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int feedbackType, int userId) {
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            UserState userState = getUserStateLocked(resolvedUserId);
            if (userState.isUiAutomationSuppressingOtherServices()) {
                return Collections.emptyList();
            }
            List<AccessibilityServiceInfo> result = this.mEnabledServicesForFeedbackTempList;
            result.clear();
            List<Service> services = userState.mBoundServices;
            while (feedbackType != 0) {
                int feedbackTypeBit = 1 << Integer.numberOfTrailingZeros(feedbackType);
                feedbackType &= ~feedbackTypeBit;
                int serviceCount = services.size();
                for (int i = 0; i < serviceCount; i++) {
                    Service service = services.get(i);
                    if (!sFakeAccessibilityServiceComponentName.equals(service.mComponentName) && (service.mFeedbackType & feedbackTypeBit) != 0) {
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
            if (resolvedUserId != this.mCurrentUserId) {
                return;
            }
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

    public int addAccessibilityInteractionConnection(IWindow windowToken, IAccessibilityInteractionConnection connection, String packageName, int userId) throws RemoteException {
        int windowId;
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
            int resolvedUid = UserHandle.getUid(resolvedUserId, UserHandle.getCallingAppId());
            String packageName2 = this.mSecurityPolicy.resolveValidReportedPackageLocked(packageName, UserHandle.getCallingAppId(), resolvedUserId);
            windowId = sNextWindowId;
            sNextWindowId = windowId + 1;
            if (this.mSecurityPolicy.isCallerInteractingAcrossUsers(userId)) {
                RemoteAccessibilityConnection wrapper = new RemoteAccessibilityConnection(windowId, connection, packageName2, resolvedUid, -1);
                wrapper.linkToDeath();
                this.mGlobalInteractionConnections.put(windowId, wrapper);
                this.mGlobalWindowTokens.put(windowId, windowToken.asBinder());
            } else {
                RemoteAccessibilityConnection wrapper2 = new RemoteAccessibilityConnection(windowId, connection, packageName2, resolvedUid, resolvedUserId);
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
            if (removedWindowId >= 0) {
                return;
            }
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

    private int removeAccessibilityInteractionConnectionInternalLocked(IBinder windowToken, SparseArray<IBinder> windowTokens, SparseArray<RemoteAccessibilityConnection> interactionConnections) {
        int count = windowTokens.size();
        for (int i = 0; i < count; i++) {
            if (windowTokens.valueAt(i) == windowToken) {
                int windowId = windowTokens.keyAt(i);
                windowTokens.removeAt(i);
                RemoteAccessibilityConnection wrapper = interactionConnections.get(windowId);
                wrapper.unlinkToDeath();
                interactionConnections.remove(windowId);
                return windowId;
            }
        }
        return -1;
    }

    public void registerUiTestAutomationService(IBinder owner, IAccessibilityServiceClient serviceClient, AccessibilityServiceInfo accessibilityServiceInfo, int flags) {
        if (IS_ENG_BUILD) {
            Slog.d(LOG_TAG, "registerUiTestAutomationService begins");
        }
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
                userState.mUiAutomationFlags = flags;
                userState.mInstalledServices.add(accessibilityServiceInfo);
                if ((flags & 1) == 0) {
                    userState.mIsTouchExplorationEnabled = false;
                    userState.mIsEnhancedWebAccessibilityEnabled = false;
                    userState.mIsDisplayMagnificationEnabled = false;
                    userState.mIsAutoclickEnabled = false;
                    userState.mEnabledServices.clear();
                }
                userState.mEnabledServices.add(sFakeAccessibilityServiceComponentName);
                userState.mTouchExplorationGrantedServices.add(sFakeAccessibilityServiceComponentName);
                onUserStateChangedLocked(userState);
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Couldn't register for the death of a UiTestAutomationService!", re);
                return;
            }
        }
        if (!IS_ENG_BUILD) {
            return;
        }
        Slog.d(LOG_TAG, "registerUiTestAutomationService ends");
    }

    public void unregisterUiTestAutomationService(IAccessibilityServiceClient serviceClient) {
        synchronized (this.mLock) {
            UserState userState = getCurrentUserStateLocked();
            if (userState.mUiAutomationService != null && serviceClient != null && userState.mUiAutomationService.mServiceInterface != null && userState.mUiAutomationService.mServiceInterface.asBinder() == serviceClient.asBinder()) {
                userState.mUiAutomationService.binderDied();
            } else {
                throw new IllegalStateException("UiAutomationService " + serviceClient + " not registered!");
            }
        }
    }

    public void temporaryEnableAccessibilityStateUntilKeyguardRemoved(ComponentName service, boolean touchExplorationEnabled) {
        this.mSecurityPolicy.enforceCallingPermission("android.permission.TEMPORARY_ENABLE_ACCESSIBILITY", TEMPORARY_ENABLE_ACCESSIBILITY_UNTIL_KEYGUARD_REMOVED);
        if (!this.mWindowManagerService.isKeyguardLocked()) {
            return;
        }
        synchronized (this.mLock) {
            UserState userState = getCurrentUserStateLocked();
            if (userState.isUiAutomationSuppressingOtherServices()) {
                return;
            }
            userState.mIsTouchExplorationEnabled = touchExplorationEnabled;
            userState.mIsEnhancedWebAccessibilityEnabled = false;
            userState.mIsDisplayMagnificationEnabled = false;
            userState.mIsAutoclickEnabled = false;
            userState.mEnabledServices.clear();
            userState.mEnabledServices.add(service);
            userState.mBindingServices.clear();
            userState.mTouchExplorationGrantedServices.clear();
            userState.mTouchExplorationGrantedServices.add(service);
            onUserStateChangedLocked(userState);
        }
    }

    public IBinder getWindowToken(int windowId, int userId) {
        this.mSecurityPolicy.enforceCallingPermission("android.permission.RETRIEVE_WINDOW_TOKEN", GET_WINDOW_TOKEN);
        synchronized (this.mLock) {
            int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
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
            handled = notifyGestureLocked(gestureId, false);
            if (!handled) {
                handled = notifyGestureLocked(gestureId, true);
            }
        }
        return handled;
    }

    boolean notifyKeyEvent(KeyEvent event, int policyFlags) {
        synchronized (this.mLock) {
            List<Service> boundServices = getCurrentUserStateLocked().mBoundServices;
            if (boundServices.isEmpty()) {
                return false;
            }
            return getKeyEventDispatcher().notifyKeyEventLocked(event, policyFlags, boundServices);
        }
    }

    void notifyMagnificationChanged(Region region, float scale, float centerX, float centerY) {
        synchronized (this.mLock) {
            notifyMagnificationChangedLocked(region, scale, centerX, centerY);
        }
    }

    void setMotionEventInjector(MotionEventInjector motionEventInjector) {
        synchronized (this.mLock) {
            this.mMotionEventInjector = motionEventInjector;
            this.mLock.notifyAll();
        }
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
        return false;
    }

    boolean accessibilityFocusOnlyInActiveWindow() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mWindowsForAccessibilityCallback == null;
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
            if (this.mCurrentUserId == userId && this.mInitialized) {
                return;
            }
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

    private void unlockUser(int userId) {
        synchronized (this.mLock) {
            int parentUserId = this.mSecurityPolicy.resolveProfileParentLocked(userId);
            if (parentUserId == this.mCurrentUserId) {
                UserState userState = getUserStateLocked(this.mCurrentUserId);
                onUserStateChangedLocked(userState);
            }
        }
    }

    private void removeUser(int userId) {
        synchronized (this.mLock) {
            this.mUserStates.remove(userId);
        }
    }

    void restoreEnabledAccessibilityServicesLocked(String oldSetting, String newSetting) {
        readComponentNamesFromStringLocked(oldSetting, this.mTempComponentNameSet, false);
        readComponentNamesFromStringLocked(newSetting, this.mTempComponentNameSet, true);
        UserState userState = getUserStateLocked(0);
        userState.mEnabledServices.clear();
        userState.mEnabledServices.addAll(this.mTempComponentNameSet);
        persistComponentNamesToSettingLocked("enabled_accessibility_services", userState.mEnabledServices, 0);
        onUserStateChangedLocked(userState);
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
        return false;
    }

    private void notifyClearAccessibilityCacheLocked() {
        UserState state = getCurrentUserStateLocked();
        for (int i = state.mBoundServices.size() - 1; i >= 0; i--) {
            Service service = state.mBoundServices.get(i);
            service.notifyClearAccessibilityNodeInfoCache();
        }
    }

    private void notifyMagnificationChangedLocked(Region region, float scale, float centerX, float centerY) {
        UserState state = getCurrentUserStateLocked();
        for (int i = state.mBoundServices.size() - 1; i >= 0; i--) {
            Service service = state.mBoundServices.get(i);
            service.notifyMagnificationChangedLocked(region, scale, centerX, centerY);
        }
    }

    private void notifySoftKeyboardShowModeChangedLocked(int showMode) {
        UserState state = getCurrentUserStateLocked();
        for (int i = state.mBoundServices.size() - 1; i >= 0; i--) {
            Service service = state.mBoundServices.get(i);
            service.notifySoftKeyboardShowModeChangedLocked(showMode);
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
        List<ResolveInfo> installedServices = this.mPackageManager.queryIntentServicesAsUser(new Intent("android.accessibilityservice.AccessibilityService"), 819332, this.mCurrentUserId);
        int count = installedServices.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo resolveInfo = installedServices.get(i);
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if ("android.permission.BIND_ACCESSIBILITY_SERVICE".equals(serviceInfo.permission)) {
                try {
                    AccessibilityServiceInfo accessibilityServiceInfo = new AccessibilityServiceInfo(resolveInfo, this.mContext);
                    this.mTempAccessibilityServiceInfoList.add(accessibilityServiceInfo);
                } catch (IOException | XmlPullParserException xppe) {
                    Slog.e(LOG_TAG, "Error while initializing AccessibilityServiceInfo", xppe);
                }
            } else {
                Slog.w(LOG_TAG, "Skipping accessibilty service " + new ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToShortString() + ": it does not require the permission android.permission.BIND_ACCESSIBILITY_SERVICE");
            }
        }
        if (this.mTempAccessibilityServiceInfoList.equals(userState.mInstalledServices)) {
            this.mTempAccessibilityServiceInfoList.clear();
            return false;
        }
        userState.mInstalledServices.clear();
        userState.mInstalledServices.addAll(this.mTempAccessibilityServiceInfoList);
        this.mTempAccessibilityServiceInfoList.clear();
        return true;
    }

    private boolean readEnabledAccessibilityServicesLocked(UserState userState) {
        this.mTempComponentNameSet.clear();
        readComponentNamesFromSettingLocked("enabled_accessibility_services", userState.mUserId, this.mTempComponentNameSet);
        if (!this.mTempComponentNameSet.equals(userState.mEnabledServices)) {
            userState.mEnabledServices.clear();
            userState.mEnabledServices.addAll(this.mTempComponentNameSet);
            if (userState.mUiAutomationService != null) {
                userState.mEnabledServices.add(sFakeAccessibilityServiceComponentName);
            }
            this.mTempComponentNameSet.clear();
            return true;
        }
        this.mTempComponentNameSet.clear();
        return false;
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
        return false;
    }

    private void notifyAccessibilityServicesDelayedLocked(AccessibilityEvent event, boolean isDefault) {
        try {
            UserState state = getCurrentUserStateLocked();
            int count = state.mBoundServices.size();
            for (int i = 0; i < count; i++) {
                Service service = state.mBoundServices.get(i);
                if (service.mIsDefault == isDefault && canDispatchEventToServiceLocked(service, event)) {
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

    private boolean canDispatchEventToServiceLocked(Service service, AccessibilityEvent event) {
        if (!service.canReceiveEventsLocked()) {
            return false;
        }
        if (event.getWindowId() != -1 && !event.isImportantForAccessibility() && (service.mFetchFlags & 8) == 0) {
            return false;
        }
        int eventType = event.getEventType();
        if ((service.mEventTypes & eventType) != eventType) {
            return false;
        }
        Set<String> packageNames = service.mPackageNames;
        String string = event.getPackageName() != null ? event.getPackageName().toString() : null;
        if (packageNames.isEmpty()) {
            return true;
        }
        return packageNames.contains(string);
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
        String settingValue = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), settingName, userId);
        readComponentNamesFromStringLocked(settingValue, outComponentNames, false);
    }

    private void readComponentNamesFromStringLocked(String names, Set<ComponentName> outComponentNames, boolean doMerge) {
        ComponentName enabledService;
        if (!doMerge) {
            outComponentNames.clear();
        }
        if (names == null) {
            return;
        }
        TextUtils.SimpleStringSplitter splitter = this.mStringColonSplitter;
        splitter.setString(names);
        while (splitter.hasNext()) {
            String str = splitter.next();
            if (str != null && str.length() > 0 && (enabledService = ComponentName.unflattenFromString(str)) != null) {
                outComponentNames.add(enabledService);
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
        long identity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), settingName, builder.toString(), userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void updateServicesLocked(UserState userState) {
        Map<ComponentName, Service> componentNameToServiceMap = userState.mComponentNameToServiceMap;
        boolean isUnlockingOrUnlocked = ((UserManager) this.mContext.getSystemService(UserManager.class)).isUserUnlockingOrUnlocked(userState.mUserId);
        int count = userState.mInstalledServices.size();
        for (int i = 0; i < count; i++) {
            AccessibilityServiceInfo installedService = userState.mInstalledServices.get(i);
            ComponentName componentName = ComponentName.unflattenFromString(installedService.getId());
            Service service = componentNameToServiceMap.get(componentName);
            if (!isUnlockingOrUnlocked && !installedService.isDirectBootAware()) {
                Slog.d(LOG_TAG, "Ignoring non-encryption-aware service " + componentName);
            } else if (!userState.mBindingServices.contains(componentName)) {
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
        }
        updateAccessibilityEnabledSetting(userState);
    }

    private void scheduleUpdateClientsIfNeededLocked(UserState userState) {
        int clientState = userState.getClientState();
        if (userState.mLastSentClientState == clientState) {
            return;
        }
        if (this.mGlobalClients.getRegisteredCallbackCount() <= 0 && userState.mClients.getRegisteredCallbackCount() <= 0) {
            return;
        }
        userState.mLastSentClientState = clientState;
        this.mMainHandler.obtainMessage(2, clientState, userState.mUserId).sendToTarget();
    }

    private void scheduleUpdateInputFilter(UserState userState) {
        this.mMainHandler.obtainMessage(6, userState).sendToTarget();
    }

    private void updateInputFilter(UserState userState) {
        boolean setInputFilter = false;
        AccessibilityInputFilter inputFilter = null;
        synchronized (this.mLock) {
            int flags = 0;
            if (userState.mIsDisplayMagnificationEnabled) {
                flags = 1;
            }
            if (userHasMagnificationServicesLocked(userState)) {
                flags |= 32;
            }
            if (userState.isHandlingAccessibilityEvents() && userState.mIsTouchExplorationEnabled) {
                flags |= 2;
            }
            if (userState.mIsFilterKeyEventsEnabled) {
                flags |= 4;
            }
            if (userState.mIsAutoclickEnabled) {
                flags |= 8;
            }
            if (userState.mIsPerformGesturesEnabled) {
                flags |= 16;
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
                this.mInputFilter.setUserAndEnabledFeatures(userState.mUserId, flags);
            } else if (this.mHasInputFilter) {
                this.mHasInputFilter = false;
                this.mInputFilter.setUserAndEnabledFeatures(userState.mUserId, 0);
                inputFilter = null;
                setInputFilter = true;
            }
        }
        if (!setInputFilter) {
            return;
        }
        this.mWindowManagerService.setInputFilter(inputFilter);
    }

    private void showEnableTouchExplorationDialog(final Service service) {
        synchronized (this.mLock) {
            String label = service.mResolveInfo.loadLabel(this.mContext.getPackageManager()).toString();
            final UserState state = getCurrentUserStateLocked();
            if (state.mIsTouchExplorationEnabled) {
                return;
            }
            if (this.mEnableTouchExplorationDialog == null || !this.mEnableTouchExplorationDialog.isShowing()) {
                this.mEnableTouchExplorationDialog = new AlertDialog.Builder(this.mContext).setIconAttribute(R.attr.alertDialogIcon).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        state.mTouchExplorationGrantedServices.add(service.mComponentName);
                        AccessibilityManagerService.this.persistComponentNamesToSettingLocked("touch_exploration_granted_accessibility_services", state.mTouchExplorationGrantedServices, state.mUserId);
                        UserState userState = AccessibilityManagerService.this.getUserStateLocked(service.mUserId);
                        userState.mIsTouchExplorationEnabled = true;
                        long identity = Binder.clearCallingIdentity();
                        try {
                            Settings.Secure.putIntForUser(AccessibilityManagerService.this.mContext.getContentResolver(), "touch_exploration_enabled", 1, service.mUserId);
                            Binder.restoreCallingIdentity(identity);
                            AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(identity);
                            throw th;
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setTitle(R.string.decline).setMessage(this.mContext.getString(R.string.decline_remote_bugreport_action, label)).create();
                this.mEnableTouchExplorationDialog.getWindow().setType(2003);
                this.mEnableTouchExplorationDialog.getWindow().getAttributes().privateFlags |= 16;
                this.mEnableTouchExplorationDialog.setCanceledOnTouchOutside(true);
                this.mEnableTouchExplorationDialog.show();
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
        updatePerformGesturesLocked(userState);
        updateEnhancedWebAccessibilityLocked(userState);
        updateDisplayColorAdjustmentSettingsLocked(userState);
        updateMagnificationLocked(userState);
        updateSoftKeyboardShowModeLocked(userState);
        scheduleUpdateInputFilter(userState);
        scheduleUpdateClientsIfNeededLocked(userState);
    }

    private void updateAccessibilityFocusBehaviorLocked(UserState userState) {
        List<Service> boundServices = userState.mBoundServices;
        int boundServiceCount = boundServices.size();
        for (int i = 0; i < boundServiceCount; i++) {
            Service boundService = boundServices.get(i);
            if (boundService.canRetrieveInteractiveWindowsLocked()) {
                userState.mAccessibilityFocusOnlyInActiveWindow = false;
                return;
            }
        }
        userState.mAccessibilityFocusOnlyInActiveWindow = true;
    }

    private void updateWindowsForAccessibilityCallbackLocked(UserState userState) {
        List<Service> boundServices = userState.mBoundServices;
        int boundServiceCount = boundServices.size();
        for (int i = 0; i < boundServiceCount; i++) {
            Service boundService = boundServices.get(i);
            if (boundService.canRetrieveInteractiveWindowsLocked()) {
                if (this.mWindowsForAccessibilityCallback == null) {
                    this.mWindowsForAccessibilityCallback = new WindowsForAccessibilityCallback();
                    this.mWindowManagerService.setWindowsForAccessibilityCallback(this.mWindowsForAccessibilityCallback);
                    return;
                }
                return;
            }
        }
        if (this.mWindowsForAccessibilityCallback == null) {
            return;
        }
        this.mWindowsForAccessibilityCallback = null;
        this.mWindowManagerService.setWindowsForAccessibilityCallback((WindowManagerInternal.WindowsForAccessibilityCallback) null);
        this.mSecurityPolicy.clearWindowsLocked();
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

    private void updatePerformGesturesLocked(UserState userState) {
        int serviceCount = userState.mBoundServices.size();
        for (int i = 0; i < serviceCount; i++) {
            Service service = userState.mBoundServices.get(i);
            if ((service.mAccessibilityServiceInfo.getCapabilities() & 32) != 0) {
                userState.mIsPerformGesturesEnabled = true;
                return;
            }
        }
        userState.mIsPerformGesturesEnabled = false;
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
        userState.mIsFilterKeyEventsEnabled = false;
    }

    private boolean readConfigurationForUserStateLocked(UserState userState) {
        boolean somethingChanged = readInstalledAccessibilityServiceLocked(userState);
        return somethingChanged | readEnabledAccessibilityServicesLocked(userState) | readTouchExplorationGrantedAccessibilityServicesLocked(userState) | readTouchExplorationEnabledSettingLocked(userState) | readHighTextContrastEnabledSettingLocked(userState) | readEnhancedWebAccessibilityEnabledChangedLocked(userState) | readDisplayMagnificationEnabledSettingLocked(userState) | readAutoclickEnabledSettingLocked(userState) | readDisplayColorAdjustmentSettingsLocked(userState);
    }

    private void updateAccessibilityEnabledSetting(UserState userState) {
        long identity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "accessibility_enabled", userState.isHandlingAccessibilityEvents() ? 1 : 0, userState.mUserId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean readTouchExplorationEnabledSettingLocked(UserState userState) {
        boolean touchExplorationEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "touch_exploration_enabled", 0, userState.mUserId) == 1;
        if (touchExplorationEnabled == userState.mIsTouchExplorationEnabled) {
            return false;
        }
        userState.mIsTouchExplorationEnabled = touchExplorationEnabled;
        return true;
    }

    private boolean readDisplayMagnificationEnabledSettingLocked(UserState userState) {
        boolean displayMagnificationEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_magnification_enabled", 0, userState.mUserId) == 1;
        if (displayMagnificationEnabled == userState.mIsDisplayMagnificationEnabled) {
            return false;
        }
        userState.mIsDisplayMagnificationEnabled = displayMagnificationEnabled;
        return true;
    }

    private boolean readAutoclickEnabledSettingLocked(UserState userState) {
        boolean autoclickEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_autoclick_enabled", 0, userState.mUserId) == 1;
        if (autoclickEnabled == userState.mIsAutoclickEnabled) {
            return false;
        }
        userState.mIsAutoclickEnabled = autoclickEnabled;
        return true;
    }

    private boolean readEnhancedWebAccessibilityEnabledChangedLocked(UserState userState) {
        boolean enhancedWeAccessibilityEnabled = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_script_injection", 0, userState.mUserId) == 1;
        if (enhancedWeAccessibilityEnabled == userState.mIsEnhancedWebAccessibilityEnabled) {
            return false;
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
            return false;
        }
        userState.mIsTextHighContrastEnabled = highTextContrastEnabled;
        return true;
    }

    private boolean readSoftKeyboardShowModeChangedLocked(UserState userState) {
        int softKeyboardShowMode = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_soft_keyboard_mode", 0, userState.mUserId);
        if (softKeyboardShowMode == userState.mSoftKeyboardShowMode) {
            return false;
        }
        userState.mSoftKeyboardShowMode = softKeyboardShowMode;
        return true;
    }

    private void updateTouchExplorationLocked(UserState userState) {
        boolean enabled = false;
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
        if (enabled == userState.mIsTouchExplorationEnabled) {
            return;
        }
        userState.mIsTouchExplorationEnabled = enabled;
        long identity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "touch_exploration_enabled", enabled ? 1 : 0, userState.mUserId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean canRequestAndRequestsTouchExplorationLocked(Service service) {
        if (!service.canReceiveEventsLocked() || !service.mRequestTouchExplorationMode) {
            return false;
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
        return false;
    }

    private void updateEnhancedWebAccessibilityLocked(UserState userState) {
        boolean enabled = false;
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
        if (enabled == userState.mIsEnhancedWebAccessibilityEnabled) {
            return;
        }
        userState.mIsEnhancedWebAccessibilityEnabled = enabled;
        long identity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "accessibility_script_injection", enabled ? 1 : 0, userState.mUserId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean canRequestAndRequestsEnhancedWebAccessibilityLocked(Service service) {
        if (service.canReceiveEventsLocked() && service.mRequestEnhancedWebAccessibility) {
            return service.mIsAutomation || (service.mAccessibilityServiceInfo.getCapabilities() & 4) != 0;
        }
        return false;
    }

    private void updateDisplayColorAdjustmentSettingsLocked(UserState userState) {
        DisplayAdjustmentUtils.applyAdjustments(this.mContext, userState.mUserId);
    }

    private void updateMagnificationLocked(UserState userState) {
        if (userState.mUserId != this.mCurrentUserId) {
            return;
        }
        if (userState.mIsDisplayMagnificationEnabled || userHasListeningMagnificationServicesLocked(userState)) {
            getMagnificationController();
            this.mMagnificationController.register();
        } else {
            if (this.mMagnificationController == null) {
                return;
            }
            this.mMagnificationController.unregister();
        }
    }

    private boolean userHasMagnificationServicesLocked(UserState userState) {
        List<Service> services = userState.mBoundServices;
        int count = services.size();
        for (int i = 0; i < count; i++) {
            Service service = services.get(i);
            if (this.mSecurityPolicy.canControlMagnification(service)) {
                return true;
            }
        }
        return false;
    }

    private boolean userHasListeningMagnificationServicesLocked(UserState userState) {
        List<Service> services = userState.mBoundServices;
        int count = services.size();
        for (int i = 0; i < count; i++) {
            Service service = services.get(i);
            if (this.mSecurityPolicy.canControlMagnification(service) && service.mInvocationHandler.mIsMagnificationCallbackEnabled) {
                return true;
            }
        }
        return false;
    }

    private void updateSoftKeyboardShowModeLocked(UserState userState) {
        int userId = userState.mUserId;
        if (userId != this.mCurrentUserId || userState.mSoftKeyboardShowMode == 0) {
            return;
        }
        boolean serviceChangingSoftKeyboardModeIsEnabled = userState.mEnabledServices.contains(userState.mServiceChangingSoftKeyboardMode);
        if (serviceChangingSoftKeyboardModeIsEnabled) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "accessibility_soft_keyboard_mode", 0, userState.mUserId);
            Binder.restoreCallingIdentity(identity);
            userState.mSoftKeyboardShowMode = 0;
            userState.mServiceChangingSoftKeyboardMode = null;
            notifySoftKeyboardShowModeChangedLocked(userState.mSoftKeyboardShowMode);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
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

    private KeyEventDispatcher getKeyEventDispatcher() {
        if (this.mKeyEventDispatcher == null) {
            this.mKeyEventDispatcher = new KeyEventDispatcher(this.mMainHandler, 8, this.mLock, this.mPowerManager);
        }
        return this.mKeyEventDispatcher;
    }

    public void enableAccessibilityService(ComponentName componentName, int userId) {
        synchronized (this.mLock) {
            if (Binder.getCallingUid() != 1000) {
                throw new SecurityException("only SYSTEM can call enableAccessibilityService.");
            }
            SettingsStringHelper settingsHelper = new SettingsStringHelper("enabled_accessibility_services", userId);
            settingsHelper.addService(componentName);
            settingsHelper.writeToSettings();
            UserState userState = getUserStateLocked(userId);
            if (userState.mEnabledServices.add(componentName)) {
                onUserStateChangedLocked(userState);
            }
        }
    }

    public void disableAccessibilityService(ComponentName componentName, int userId) {
        synchronized (this.mLock) {
            if (Binder.getCallingUid() != 1000) {
                throw new SecurityException("only SYSTEM can call disableAccessibility");
            }
            SettingsStringHelper settingsHelper = new SettingsStringHelper("enabled_accessibility_services", userId);
            settingsHelper.deleteService(componentName);
            settingsHelper.writeToSettings();
            UserState userState = getUserStateLocked(userId);
            if (userState.mEnabledServices.remove(componentName)) {
                onUserStateChangedLocked(userState);
            }
        }
    }

    private class SettingsStringHelper {
        private static final String SETTINGS_DELIMITER = ":";
        private ContentResolver mContentResolver;
        private Set<String> mServices;
        private final String mSettingsName;
        private final int mUserId;

        public SettingsStringHelper(String name, int userId) {
            this.mUserId = userId;
            this.mSettingsName = name;
            this.mContentResolver = AccessibilityManagerService.this.mContext.getContentResolver();
            String servicesString = Settings.Secure.getStringForUser(this.mContentResolver, this.mSettingsName, userId);
            this.mServices = new HashSet();
            if (TextUtils.isEmpty(servicesString)) {
                return;
            }
            TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(SETTINGS_DELIMITER.charAt(0));
            colonSplitter.setString(servicesString);
            while (colonSplitter.hasNext()) {
                String serviceName = colonSplitter.next();
                this.mServices.add(serviceName);
            }
        }

        public void addService(ComponentName component) {
            this.mServices.add(component.flattenToString());
        }

        public void deleteService(ComponentName component) {
            this.mServices.remove(component.flattenToString());
        }

        public void writeToSettings() {
            Settings.Secure.putStringForUser(this.mContentResolver, this.mSettingsName, TextUtils.join(SETTINGS_DELIMITER, this.mServices), this.mUserId);
        }
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
                pw.append((CharSequence) (", currentUser=" + (userState.mUserId == this.mCurrentUserId)));
                pw.append((CharSequence) (", touchExplorationEnabled=" + userState.mIsTouchExplorationEnabled));
                pw.append((CharSequence) (", displayMagnificationEnabled=" + userState.mIsDisplayMagnificationEnabled));
                pw.append((CharSequence) (", autoclickEnabled=" + userState.mIsAutoclickEnabled));
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

    class RemoteAccessibilityConnection implements IBinder.DeathRecipient {
        private final IAccessibilityInteractionConnection mConnection;
        private final String mPackageName;
        private final int mUid;
        private final int mUserId;
        private final int mWindowId;

        RemoteAccessibilityConnection(int windowId, IAccessibilityInteractionConnection connection, String packageName, int uid, int userId) {
            this.mWindowId = windowId;
            this.mPackageName = packageName;
            this.mUid = uid;
            this.mUserId = userId;
            this.mConnection = connection;
        }

        public int getUid() {
            return this.mUid;
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public IAccessibilityInteractionConnection getRemote() {
            return this.mConnection;
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
                    }
                    bridge.clearAccessibilityFocusNotLocked(windowId);
                    return;
            }
        }

        private void announceNewUserIfNeeded() {
            synchronized (AccessibilityManagerService.this.mLock) {
                UserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                if (userState.isHandlingAccessibilityEvents()) {
                    UserManager userManager = (UserManager) AccessibilityManagerService.this.mContext.getSystemService("user");
                    String message = AccessibilityManagerService.this.mContext.getString(R.string.issued_by, userManager.getUserInfo(AccessibilityManagerService.this.mCurrentUserId).name);
                    AccessibilityEvent event = AccessibilityEvent.obtain(PackageManagerService.DumpState.DUMP_KEYSETS);
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
            if (this.mSecurityPolicy.mWindows != null) {
                return;
            }
            if (this.mWindowsForAccessibilityCallback == null) {
                UserState userState = getCurrentUserStateLocked();
                onUserStateChangedLocked(userState);
            }
            if (this.mWindowsForAccessibilityCallback == null) {
                return;
            }
            long startMillis = SystemClock.uptimeMillis();
            while (this.mSecurityPolicy.mWindows == null) {
                long elapsedMillis = SystemClock.uptimeMillis() - startMillis;
                long remainMillis = DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC - elapsedMillis;
                if (remainMillis <= 0) {
                    return;
                } else {
                    try {
                        this.mLock.wait(remainMillis);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    MagnificationController getMagnificationController() {
        MagnificationController magnificationController;
        synchronized (this.mLock) {
            if (this.mMagnificationController == null) {
                this.mMagnificationController = new MagnificationController(this.mContext, this, this.mLock);
                this.mMagnificationController.setUserId(this.mCurrentUserId);
            }
            magnificationController = this.mMagnificationController;
        }
        return magnificationController;
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
        public final InvocationHandler mInvocationHandler;
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

        public Service(int userId, ComponentName componentName, AccessibilityServiceInfo accessibilityServiceInfo) {
            this.mId = 0;
            this.mEventDispatchHandler = new Handler(AccessibilityManagerService.this.mMainHandler.getLooper()) {
                @Override
                public void handleMessage(Message message) {
                    int eventType = message.what;
                    AccessibilityEvent event = (AccessibilityEvent) message.obj;
                    Service.this.notifyAccessibilityEventInternal(eventType, event);
                }
            };
            this.mInvocationHandler = new InvocationHandler(AccessibilityManagerService.this.mMainHandler.getLooper());
            this.mUserId = userId;
            this.mResolveInfo = accessibilityServiceInfo.getResolveInfo();
            int i = AccessibilityManagerService.sIdCounter;
            int unused = AccessibilityManagerService.sIdCounter = i + 1;
            this.mId = i;
            this.mComponentName = componentName;
            this.mAccessibilityServiceInfo = accessibilityServiceInfo;
            this.mIsAutomation = AccessibilityManagerService.sFakeAccessibilityServiceComponentName.equals(componentName);
            if (!this.mIsAutomation) {
                this.mIntent = new Intent().setComponent(this.mComponentName);
                this.mIntent.putExtra("android.intent.extra.client_label", R.string.foreground_service_apps_in_background);
                long idendtity = Binder.clearCallingIdentity();
                try {
                    if (BenesseExtension.getDchaState() == 0) {
                        this.mIntent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivity(AccessibilityManagerService.this.mContext, 0, new Intent("android.settings.ACCESSIBILITY_SETTINGS"), 0));
                    }
                } finally {
                    Binder.restoreCallingIdentity(idendtity);
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
                long identity = Binder.clearCallingIdentity();
                try {
                    if (this.mService == null && AccessibilityManagerService.this.mContext.bindServiceAsUser(this.mIntent, this, 33554433, new UserHandle(this.mUserId))) {
                        userState.mBindingServices.add(this.mComponentName);
                    }
                    return false;
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
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
            return false;
        }

        public boolean unbindLocked() {
            if (this.mService == null) {
                return false;
            }
            UserState userState = AccessibilityManagerService.this.getUserStateLocked(this.mUserId);
            AccessibilityManagerService.this.getKeyEventDispatcher().flush(this);
            if (!this.mIsAutomation) {
                AccessibilityManagerService.this.mContext.unbindService(this);
            } else {
                userState.destroyUiAutomationService();
            }
            AccessibilityManagerService.this.removeServiceLocked(this, userState);
            resetLocked();
            return true;
        }

        public void disableSelf() {
            synchronized (AccessibilityManagerService.this.mLock) {
                UserState userState = AccessibilityManagerService.this.getUserStateLocked(this.mUserId);
                if (userState.mEnabledServices.remove(this.mComponentName)) {
                    long identity = Binder.clearCallingIdentity();
                    try {
                        AccessibilityManagerService.this.persistComponentNamesToSettingLocked("enabled_accessibility_services", userState.mEnabledServices, this.mUserId);
                        Binder.restoreCallingIdentity(identity);
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(identity);
                        throw th;
                    }
                }
            }
        }

        public boolean canReceiveEventsLocked() {
            return (this.mEventTypes == 0 || this.mFeedbackType == 0 || this.mService == null) ? false : true;
        }

        public void setOnKeyEventResult(boolean handled, int sequence) {
            AccessibilityManagerService.this.getKeyEventDispatcher().setOnKeyEventResult(this, handled, sequence);
        }

        public AccessibilityServiceInfo getServiceInfo() {
            AccessibilityServiceInfo accessibilityServiceInfo;
            synchronized (AccessibilityManagerService.this.mLock) {
                accessibilityServiceInfo = this.mAccessibilityServiceInfo;
            }
            return accessibilityServiceInfo;
        }

        public boolean canRetrieveInteractiveWindowsLocked() {
            if (AccessibilityManagerService.this.mSecurityPolicy.canRetrieveWindowContentLocked(this)) {
                return this.mRetrieveInteractiveWindows;
            }
            return false;
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
                    this.mWasConnectedAndDied = false;
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

        private boolean isCalledForCurrentUserLocked() {
            int resolvedUserId = AccessibilityManagerService.this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(-2);
            return resolvedUserId == AccessibilityManagerService.this.mCurrentUserId;
        }

        public List<AccessibilityWindowInfo> getWindows() {
            AccessibilityManagerService.this.ensureWindowsAvailableTimed();
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return null;
                }
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canRetrieveWindowsLocked(this);
                if (!permissionGranted) {
                    return null;
                }
                if (AccessibilityManagerService.this.mSecurityPolicy.mWindows == null) {
                    return null;
                }
                List<AccessibilityWindowInfo> windows = new ArrayList<>();
                int windowCount = AccessibilityManagerService.this.mSecurityPolicy.mWindows.size();
                for (int i = 0; i < windowCount; i++) {
                    AccessibilityWindowInfo window = AccessibilityManagerService.this.mSecurityPolicy.mWindows.get(i);
                    AccessibilityWindowInfo windowClone = AccessibilityWindowInfo.obtain(window);
                    windowClone.setConnectionId(this.mId);
                    windows.add(windowClone);
                }
                return windows;
            }
        }

        public AccessibilityWindowInfo getWindow(int windowId) {
            AccessibilityManagerService.this.ensureWindowsAvailableTimed();
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return null;
                }
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canRetrieveWindowsLocked(this);
                if (!permissionGranted) {
                    return null;
                }
                AccessibilityWindowInfo window = AccessibilityManagerService.this.mSecurityPolicy.findWindowById(windowId);
                if (window == null) {
                    return null;
                }
                AccessibilityWindowInfo windowClone = AccessibilityWindowInfo.obtain(window);
                windowClone.setConnectionId(this.mId);
                return windowClone;
            }
        }

        public String[] findAccessibilityNodeInfosByViewId(int accessibilityWindowId, long accessibilityNodeId, String viewIdResName, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
            Region partialInteractiveRegion = Region.obtain();
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return null;
                }
                int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId);
                if (!permissionGranted) {
                    return null;
                }
                RemoteAccessibilityConnection connection = getConnectionLocked(resolvedWindowId);
                if (connection == null) {
                    return null;
                }
                if (!AccessibilityManagerService.this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                    partialInteractiveRegion.recycle();
                    partialInteractiveRegion = null;
                }
                int interrogatingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long identityToken = Binder.clearCallingIdentity();
                MagnificationSpec spec = AccessibilityManagerService.this.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                try {
                    connection.getRemote().findAccessibilityNodeInfosByViewId(accessibilityNodeId, viewIdResName, partialInteractiveRegion, interactionId, callback, this.mFetchFlags, interrogatingPid, interrogatingTid, spec);
                    String[] strArrComputeValidReportedPackages = AccessibilityManagerService.this.mSecurityPolicy.computeValidReportedPackages(callingUid, connection.getPackageName(), connection.getUid());
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                    }
                    return strArrComputeValidReportedPackages;
                } catch (RemoteException e) {
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                        return null;
                    }
                    return null;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                    }
                    throw th;
                }
            }
        }

        public String[] findAccessibilityNodeInfosByText(int accessibilityWindowId, long accessibilityNodeId, String text, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
            Region partialInteractiveRegion = Region.obtain();
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return null;
                }
                int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId);
                if (!permissionGranted) {
                    return null;
                }
                RemoteAccessibilityConnection connection = getConnectionLocked(resolvedWindowId);
                if (connection == null) {
                    return null;
                }
                if (!AccessibilityManagerService.this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                    partialInteractiveRegion.recycle();
                    partialInteractiveRegion = null;
                }
                int interrogatingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long identityToken = Binder.clearCallingIdentity();
                MagnificationSpec spec = AccessibilityManagerService.this.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                try {
                    connection.getRemote().findAccessibilityNodeInfosByText(accessibilityNodeId, text, partialInteractiveRegion, interactionId, callback, this.mFetchFlags, interrogatingPid, interrogatingTid, spec);
                    String[] strArrComputeValidReportedPackages = AccessibilityManagerService.this.mSecurityPolicy.computeValidReportedPackages(callingUid, connection.getPackageName(), connection.getUid());
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                    }
                    return strArrComputeValidReportedPackages;
                } catch (RemoteException e) {
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                        return null;
                    }
                    return null;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                    }
                    throw th;
                }
            }
        }

        public String[] findAccessibilityNodeInfoByAccessibilityId(int accessibilityWindowId, long accessibilityNodeId, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, long interrogatingTid) throws RemoteException {
            Region partialInteractiveRegion = Region.obtain();
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return null;
                }
                int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId);
                if (!permissionGranted) {
                    return null;
                }
                RemoteAccessibilityConnection connection = getConnectionLocked(resolvedWindowId);
                if (connection == null) {
                    return null;
                }
                if (!AccessibilityManagerService.this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                    partialInteractiveRegion.recycle();
                    partialInteractiveRegion = null;
                }
                int interrogatingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long identityToken = Binder.clearCallingIdentity();
                MagnificationSpec spec = AccessibilityManagerService.this.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                try {
                    connection.getRemote().findAccessibilityNodeInfoByAccessibilityId(accessibilityNodeId, partialInteractiveRegion, interactionId, callback, this.mFetchFlags | flags, interrogatingPid, interrogatingTid, spec);
                    String[] strArrComputeValidReportedPackages = AccessibilityManagerService.this.mSecurityPolicy.computeValidReportedPackages(callingUid, connection.getPackageName(), connection.getUid());
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                    }
                    return strArrComputeValidReportedPackages;
                } catch (RemoteException e) {
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                        return null;
                    }
                    return null;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                    }
                    throw th;
                }
            }
        }

        public String[] findFocus(int accessibilityWindowId, long accessibilityNodeId, int focusType, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
            Region partialInteractiveRegion = Region.obtain();
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return null;
                }
                int resolvedWindowId = resolveAccessibilityWindowIdForFindFocusLocked(accessibilityWindowId, focusType);
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId);
                if (!permissionGranted) {
                    return null;
                }
                RemoteAccessibilityConnection connection = getConnectionLocked(resolvedWindowId);
                if (connection == null) {
                    return null;
                }
                if (!AccessibilityManagerService.this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                    partialInteractiveRegion.recycle();
                    partialInteractiveRegion = null;
                }
                int interrogatingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long identityToken = Binder.clearCallingIdentity();
                MagnificationSpec spec = AccessibilityManagerService.this.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                try {
                    connection.getRemote().findFocus(accessibilityNodeId, focusType, partialInteractiveRegion, interactionId, callback, this.mFetchFlags, interrogatingPid, interrogatingTid, spec);
                    String[] strArrComputeValidReportedPackages = AccessibilityManagerService.this.mSecurityPolicy.computeValidReportedPackages(callingUid, connection.getPackageName(), connection.getUid());
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                    }
                    return strArrComputeValidReportedPackages;
                } catch (RemoteException e) {
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                        return null;
                    }
                    return null;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                    }
                    throw th;
                }
            }
        }

        public String[] focusSearch(int accessibilityWindowId, long accessibilityNodeId, int direction, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
            Region partialInteractiveRegion = Region.obtain();
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return null;
                }
                int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId);
                if (!permissionGranted) {
                    return null;
                }
                RemoteAccessibilityConnection connection = getConnectionLocked(resolvedWindowId);
                if (connection == null) {
                    return null;
                }
                if (!AccessibilityManagerService.this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(resolvedWindowId, partialInteractiveRegion)) {
                    partialInteractiveRegion.recycle();
                    partialInteractiveRegion = null;
                }
                int interrogatingPid = Binder.getCallingPid();
                int callingUid = Binder.getCallingUid();
                long identityToken = Binder.clearCallingIdentity();
                MagnificationSpec spec = AccessibilityManagerService.this.getCompatibleMagnificationSpecLocked(resolvedWindowId);
                try {
                    connection.getRemote().focusSearch(accessibilityNodeId, direction, partialInteractiveRegion, interactionId, callback, this.mFetchFlags, interrogatingPid, interrogatingTid, spec);
                    String[] strArrComputeValidReportedPackages = AccessibilityManagerService.this.mSecurityPolicy.computeValidReportedPackages(callingUid, connection.getPackageName(), connection.getUid());
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                    }
                    return strArrComputeValidReportedPackages;
                } catch (RemoteException e) {
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                        return null;
                    }
                    return null;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identityToken);
                    if (partialInteractiveRegion != null && Binder.isProxy(connection.getRemote())) {
                        partialInteractiveRegion.recycle();
                    }
                    throw th;
                }
            }
        }

        public void sendGesture(int sequence, ParceledListSlice gestureSteps) {
            synchronized (AccessibilityManagerService.this.mLock) {
                if (AccessibilityManagerService.this.mSecurityPolicy.canPerformGestures(this)) {
                    long endMillis = SystemClock.uptimeMillis() + 1000;
                    while (AccessibilityManagerService.this.mMotionEventInjector == null && SystemClock.uptimeMillis() < endMillis) {
                        try {
                            AccessibilityManagerService.this.mLock.wait(endMillis - SystemClock.uptimeMillis());
                        } catch (InterruptedException e) {
                        }
                    }
                    if (AccessibilityManagerService.this.mMotionEventInjector != null) {
                        List<GestureDescription.GestureStep> steps = gestureSteps.getList();
                        List<MotionEvent> events = GestureDescription.MotionEventGenerator.getMotionEventsFromGestureSteps(steps);
                        if (events.get(events.size() - 1).getAction() == 1) {
                            AccessibilityManagerService.this.mMotionEventInjector.injectEvents(events, this.mServiceInterface, sequence);
                            return;
                        }
                        Slog.e(AccessibilityManagerService.LOG_TAG, "Gesture is not well-formed");
                    } else {
                        Slog.e(AccessibilityManagerService.LOG_TAG, "MotionEventInjector installation timed out");
                    }
                }
                try {
                    this.mServiceInterface.onPerformGestureResult(sequence, false);
                } catch (RemoteException re) {
                    Slog.e(AccessibilityManagerService.LOG_TAG, "Error sending motion event injection failure to " + this.mServiceInterface, re);
                }
            }
        }

        public boolean performAccessibilityAction(int accessibilityWindowId, long accessibilityNodeId, int action, Bundle arguments, int interactionId, IAccessibilityInteractionConnectionCallback callback, long interrogatingTid) throws RemoteException {
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return false;
                }
                int resolvedWindowId = resolveAccessibilityWindowIdLocked(accessibilityWindowId);
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, resolvedWindowId);
                if (!permissionGranted) {
                    return false;
                }
                RemoteAccessibilityConnection connection = getConnectionLocked(resolvedWindowId);
                if (connection == null) {
                    return false;
                }
                int interrogatingPid = Binder.getCallingPid();
                long identityToken = Binder.clearCallingIdentity();
                try {
                    AccessibilityManagerService.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 3, 0);
                    connection.mConnection.performAccessibilityAction(accessibilityNodeId, action, arguments, interactionId, callback, this.mFetchFlags, interrogatingPid, interrogatingTid);
                    return true;
                } catch (RemoteException e) {
                    return true;
                } finally {
                    Binder.restoreCallingIdentity(identityToken);
                }
            }
        }

        public boolean performGlobalAction(int action) {
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return false;
                }
                long identity = Binder.clearCallingIdentity();
                try {
                    AccessibilityManagerService.this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 3, 0);
                    switch (action) {
                        case 1:
                            sendDownAndUpKeyEvents(4);
                            return true;
                        case 2:
                            sendDownAndUpKeyEvents(3);
                            return true;
                        case 3:
                            openRecents();
                            return true;
                        case 4:
                            expandNotifications();
                            return true;
                        case 5:
                            expandQuickSettings();
                            return true;
                        case 6:
                            showGlobalActions();
                            return true;
                        case 7:
                            toggleSplitScreen();
                            return true;
                        default:
                            return false;
                    }
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public float getMagnificationScale() {
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return 1.0f;
                }
                long identity = Binder.clearCallingIdentity();
                try {
                    return AccessibilityManagerService.this.getMagnificationController().getScale();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public Region getMagnificationRegion() {
            synchronized (AccessibilityManagerService.this.mLock) {
                Region region = Region.obtain();
                if (!isCalledForCurrentUserLocked()) {
                    return region;
                }
                MagnificationController magnificationController = AccessibilityManagerService.this.getMagnificationController();
                boolean forceRegistration = AccessibilityManagerService.this.mSecurityPolicy.canControlMagnification(this);
                boolean initiallyRegistered = magnificationController.isRegisteredLocked();
                if (!initiallyRegistered && forceRegistration) {
                    magnificationController.register();
                }
                long identity = Binder.clearCallingIdentity();
                try {
                    magnificationController.getMagnificationRegion(region);
                    return region;
                } finally {
                    Binder.restoreCallingIdentity(identity);
                    if (!initiallyRegistered && forceRegistration) {
                        magnificationController.unregister();
                    }
                }
            }
        }

        public float getMagnificationCenterX() {
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return 0.0f;
                }
                long identity = Binder.clearCallingIdentity();
                try {
                    return AccessibilityManagerService.this.getMagnificationController().getCenterX();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public float getMagnificationCenterY() {
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return 0.0f;
                }
                long identity = Binder.clearCallingIdentity();
                try {
                    return AccessibilityManagerService.this.getMagnificationController().getCenterY();
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public boolean resetMagnification(boolean animate) {
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return false;
                }
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canControlMagnification(this);
                if (!permissionGranted) {
                    return false;
                }
                long identity = Binder.clearCallingIdentity();
                try {
                    return AccessibilityManagerService.this.getMagnificationController().reset(animate);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public boolean setMagnificationScaleAndCenter(float scale, float centerX, float centerY, boolean animate) {
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return false;
                }
                boolean permissionGranted = AccessibilityManagerService.this.mSecurityPolicy.canControlMagnification(this);
                if (!permissionGranted) {
                    return false;
                }
                long identity = Binder.clearCallingIdentity();
                try {
                    MagnificationController magnificationController = AccessibilityManagerService.this.getMagnificationController();
                    if (!magnificationController.isRegisteredLocked()) {
                        magnificationController.register();
                    }
                    return magnificationController.setScaleAndCenter(scale, centerX, centerY, animate, this.mId);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void setMagnificationCallbackEnabled(boolean enabled) {
            this.mInvocationHandler.setMagnificationCallbackEnabled(enabled);
        }

        public boolean setSoftKeyboardShowMode(int showMode) {
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isCalledForCurrentUserLocked()) {
                    return false;
                }
                UserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                long identity = Binder.clearCallingIdentity();
                try {
                    if (showMode == 0) {
                        userState.mServiceChangingSoftKeyboardMode = null;
                    } else {
                        userState.mServiceChangingSoftKeyboardMode = this.mComponentName;
                    }
                    Settings.Secure.putIntForUser(AccessibilityManagerService.this.mContext.getContentResolver(), "accessibility_soft_keyboard_mode", showMode, userState.mUserId);
                    Binder.restoreCallingIdentity(identity);
                    return true;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(identity);
                    throw th;
                }
            }
        }

        public void setSoftKeyboardCallbackEnabled(boolean enabled) {
            this.mInvocationHandler.setSoftKeyboardCallbackEnabled(enabled);
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            AccessibilityManagerService.this.mSecurityPolicy.enforceCallingPermission("android.permission.DUMP", AccessibilityManagerService.FUNCTION_DUMP);
            synchronized (AccessibilityManagerService.this.mLock) {
                pw.append((CharSequence) ("Service[label=" + this.mAccessibilityServiceInfo.getResolveInfo().loadLabel(AccessibilityManagerService.this.mContext.getPackageManager())));
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
            if (AccessibilityManagerService.IS_ENG_BUILD) {
                Slog.d(AccessibilityManagerService.LOG_TAG, "resetLocked()", new Throwable("resetLocked()"));
            }
            try {
                this.mServiceInterface.init((IAccessibilityServiceConnection) null, this.mId, (IBinder) null);
            } catch (RemoteException e) {
            }
            this.mService = null;
            this.mServiceInterface = null;
        }

        public boolean isConnectedLocked() {
            return this.mService != null;
        }

        @Override
        public void binderDied() {
            synchronized (AccessibilityManagerService.this.mLock) {
                if (!isConnectedLocked()) {
                    return;
                }
                this.mWasConnectedAndDied = true;
                AccessibilityManagerService.this.getKeyEventDispatcher().flush(this);
                UserState userState = AccessibilityManagerService.this.getUserStateLocked(this.mUserId);
                AccessibilityManagerService.this.removeServiceLocked(this, userState);
                resetLocked();
                if (this.mIsAutomation) {
                    userState.mInstalledServices.remove(this.mAccessibilityServiceInfo);
                    userState.mEnabledServices.remove(this.mComponentName);
                    userState.destroyUiAutomationService();
                    AccessibilityManagerService.this.readConfigurationForUserStateLocked(userState);
                }
                if (this.mId == AccessibilityManagerService.this.getMagnificationController().getIdOfLastServiceToMagnify()) {
                    AccessibilityManagerService.this.getMagnificationController().resetIfNeeded(true);
                }
                AccessibilityManagerService.this.onUserStateChangedLocked(userState);
            }
        }

        public void notifyAccessibilityEvent(AccessibilityEvent event) {
            Message message;
            synchronized (AccessibilityManagerService.this.mLock) {
                int eventType = event.getEventType();
                AccessibilityEvent newEvent = AccessibilityEvent.obtain(event);
                if (this.mNotificationTimeout > 0 && eventType != 2048) {
                    AccessibilityEvent oldEvent = this.mPendingEvents.get(eventType);
                    this.mPendingEvents.put(eventType, newEvent);
                    if (oldEvent != null) {
                        this.mEventDispatchHandler.removeMessages(eventType);
                        oldEvent.recycle();
                    }
                    message = this.mEventDispatchHandler.obtainMessage(eventType);
                } else {
                    message = this.mEventDispatchHandler.obtainMessage(eventType, newEvent);
                }
                this.mEventDispatchHandler.sendMessageDelayed(message, this.mNotificationTimeout);
            }
        }

        private void notifyAccessibilityEventInternal(int eventType, AccessibilityEvent event) {
            synchronized (AccessibilityManagerService.this.mLock) {
                IAccessibilityServiceClient listener = this.mServiceInterface;
                if (listener == null) {
                    return;
                }
                if (event == null) {
                    event = this.mPendingEvents.get(eventType);
                    if (event == null) {
                        return;
                    } else {
                        this.mPendingEvents.remove(eventType);
                    }
                }
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

        public void notifyGesture(int gestureId) {
            this.mInvocationHandler.obtainMessage(1, gestureId, 0).sendToTarget();
        }

        public void notifyClearAccessibilityNodeInfoCache() {
            this.mInvocationHandler.sendEmptyMessage(2);
        }

        public void notifyMagnificationChangedLocked(Region region, float scale, float centerX, float centerY) {
            this.mInvocationHandler.notifyMagnificationChangedLocked(region, scale, centerX, centerY);
        }

        public void notifySoftKeyboardShowModeChangedLocked(int showState) {
            this.mInvocationHandler.notifySoftKeyboardShowModeChangedLocked(showState);
        }

        private void notifyMagnificationChangedInternal(Region region, float scale, float centerX, float centerY) {
            IAccessibilityServiceClient listener;
            synchronized (AccessibilityManagerService.this.mLock) {
                listener = this.mServiceInterface;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onMagnificationChanged(region, scale, centerX, centerY);
            } catch (RemoteException re) {
                Slog.e(AccessibilityManagerService.LOG_TAG, "Error sending magnification changes to " + this.mService, re);
            }
        }

        private void notifySoftKeyboardShowModeChangedInternal(int showState) {
            IAccessibilityServiceClient listener;
            synchronized (AccessibilityManagerService.this.mLock) {
                listener = this.mServiceInterface;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onSoftKeyboardShowModeChanged(showState);
            } catch (RemoteException re) {
                Slog.e(AccessibilityManagerService.LOG_TAG, "Error sending soft keyboard show mode changes to " + this.mService, re);
            }
        }

        private void notifyGestureInternal(int gestureId) {
            IAccessibilityServiceClient listener;
            synchronized (AccessibilityManagerService.this.mLock) {
                listener = this.mServiceInterface;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onGesture(gestureId);
            } catch (RemoteException re) {
                Slog.e(AccessibilityManagerService.LOG_TAG, "Error during sending gesture " + gestureId + " to " + this.mService, re);
            }
        }

        private void notifyClearAccessibilityCacheInternal() {
            IAccessibilityServiceClient listener;
            synchronized (AccessibilityManagerService.this.mLock) {
                listener = this.mServiceInterface;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.clearAccessibilityCache();
            } catch (RemoteException re) {
                Slog.e(AccessibilityManagerService.LOG_TAG, "Error during requesting accessibility info cache to be cleared.", re);
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
            StatusBarManagerInternal statusBarService = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            statusBarService.toggleRecentApps();
            Binder.restoreCallingIdentity(token);
        }

        private void showGlobalActions() {
            AccessibilityManagerService.this.mWindowManagerService.showGlobalActions();
        }

        private void toggleSplitScreen() {
            ((StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class)).toggleSplitScreen();
        }

        private RemoteAccessibilityConnection getConnectionLocked(int windowId) {
            RemoteAccessibilityConnection wrapper = (RemoteAccessibilityConnection) AccessibilityManagerService.this.mGlobalInteractionConnections.get(windowId);
            if (wrapper == null) {
                wrapper = AccessibilityManagerService.this.getCurrentUserStateLocked().mInteractionConnections.get(windowId);
            }
            if (wrapper == null || wrapper.mConnection == null) {
                return null;
            }
            return wrapper;
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
            }
            return windowId;
        }

        private final class InvocationHandler extends Handler {
            public static final int MSG_CLEAR_ACCESSIBILITY_CACHE = 2;
            public static final int MSG_ON_GESTURE = 1;
            private static final int MSG_ON_MAGNIFICATION_CHANGED = 5;
            private static final int MSG_ON_SOFT_KEYBOARD_STATE_CHANGED = 6;
            private boolean mIsMagnificationCallbackEnabled;
            private boolean mIsSoftKeyboardCallbackEnabled;

            public InvocationHandler(Looper looper) {
                super(looper, null, true);
                this.mIsMagnificationCallbackEnabled = false;
                this.mIsSoftKeyboardCallbackEnabled = false;
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
                        Service.this.notifyClearAccessibilityCacheInternal();
                        return;
                    case 3:
                    case 4:
                    default:
                        throw new IllegalArgumentException("Unknown message: " + type);
                    case 5:
                        SomeArgs args = (SomeArgs) message.obj;
                        Region region = (Region) args.arg1;
                        float scale = ((Float) args.arg2).floatValue();
                        float centerX = ((Float) args.arg3).floatValue();
                        float centerY = ((Float) args.arg4).floatValue();
                        Service.this.notifyMagnificationChangedInternal(region, scale, centerX, centerY);
                        return;
                    case 6:
                        int showState = message.arg1;
                        Service.this.notifySoftKeyboardShowModeChangedInternal(showState);
                        return;
                }
            }

            public void notifyMagnificationChangedLocked(Region region, float scale, float centerX, float centerY) {
                if (!this.mIsMagnificationCallbackEnabled) {
                    return;
                }
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = region;
                args.arg2 = Float.valueOf(scale);
                args.arg3 = Float.valueOf(centerX);
                args.arg4 = Float.valueOf(centerY);
                Message msg = obtainMessage(5, args);
                msg.sendToTarget();
            }

            public void setMagnificationCallbackEnabled(boolean enabled) {
                this.mIsMagnificationCallbackEnabled = enabled;
            }

            public void notifySoftKeyboardShowModeChangedLocked(int showState) {
                if (!this.mIsSoftKeyboardCallbackEnabled) {
                    return;
                }
                Message msg = obtainMessage(6, showState, 0);
                msg.sendToTarget();
            }

            public void setSoftKeyboardCallbackEnabled(boolean enabled) {
                this.mIsSoftKeyboardCallbackEnabled = enabled;
            }
        }
    }

    private AppWidgetManagerInternal getAppWidgetManager() {
        AppWidgetManagerInternal appWidgetManagerInternal;
        synchronized (this.mLock) {
            if (this.mAppWidgetService == null && this.mPackageManager.hasSystemFeature("android.software.app_widgets")) {
                this.mAppWidgetService = (AppWidgetManagerInternal) LocalServices.getService(AppWidgetManagerInternal.class);
            }
            appWidgetManagerInternal = this.mAppWidgetService;
        }
        return appWidgetManagerInternal;
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
            reportedWindow.setTitle(window.title);
            reportedWindow.setAnchorId(window.accessibilityIdOfAnchor);
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
            }
            return reportedWindow;
        }

        private int getTypeForWindowManagerWindowType(int windowType) {
            switch (windowType) {
                case 1:
                case 2:
                case 3:
                case 1000:
                case ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG:
                case ANRManager.START_MONITOR_SERVICE_TIMEOUT_MSG:
                case 1003:
                case 1005:
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
                case 2036:
                    return 3;
                case 2011:
                case 2012:
                    return 2;
                case 2032:
                    return 4;
                case 2034:
                    return 5;
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
            if (focus == null) {
                return;
            }
            focus.performAction(128);
        }

        public boolean getAccessibilityFocusClickPointInScreenNotLocked(Point outPoint) {
            AccessibilityNodeInfo focus = getAccessibilityFocusNotLocked();
            if (focus == null) {
                return false;
            }
            synchronized (AccessibilityManagerService.this.mLock) {
                Rect boundsInScreen = AccessibilityManagerService.this.mTempRect;
                focus.getBoundsInScreen(boundsInScreen);
                Rect windowBounds = AccessibilityManagerService.this.mTempRect1;
                AccessibilityManagerService.this.getWindowBounds(focus.getWindowId(), windowBounds);
                if (!boundsInScreen.intersect(windowBounds)) {
                    return false;
                }
                MagnificationSpec spec = AccessibilityManagerService.this.getCompatibleMagnificationSpecLocked(focus.getWindowId());
                if (spec != null && !spec.isNop()) {
                    boundsInScreen.offset((int) (-spec.offsetX), (int) (-spec.offsetY));
                    boundsInScreen.scale(1.0f / spec.scale);
                }
                Point screenSize = AccessibilityManagerService.this.mTempPoint;
                this.mDefaultDisplay.getRealSize(screenSize);
                if (!boundsInScreen.intersect(0, 0, screenSize.x, screenSize.y)) {
                    return false;
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
                case 256:
                case 512:
                case 1024:
                case PackageManagerService.DumpState.DUMP_KEYSETS:
                case PackageManagerService.DumpState.DUMP_DOMAIN_PREFERRED:
                case PackageManagerService.DumpState.DUMP_FROZEN:
                case PackageManagerService.DumpState.DUMP_DEXOPT:
                case 2097152:
                case 4194304:
                case 16777216:
                    return true;
                default:
                    return isRetrievalAllowingWindow(event.getWindowId());
            }
        }

        private boolean isValidPackageForUid(String packageName, int uid) {
            long token = Binder.clearCallingIdentity();
            try {
                return uid == AccessibilityManagerService.this.mPackageManager.getPackageUid(packageName, UserHandle.getUserId(uid));
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        String resolveValidReportedPackageLocked(CharSequence packageName, int appId, int userId) {
            if (packageName == null) {
                return null;
            }
            if (appId == 1000) {
                return packageName.toString();
            }
            String packageNameStr = packageName.toString();
            int resolvedUid = UserHandle.getUid(userId, appId);
            if (isValidPackageForUid(packageNameStr, resolvedUid)) {
                return packageName.toString();
            }
            AppWidgetManagerInternal appWidgetManager = AccessibilityManagerService.this.getAppWidgetManager();
            if (appWidgetManager != null && ArrayUtils.contains(appWidgetManager.getHostedWidgetPackages(resolvedUid), packageNameStr)) {
                return packageName.toString();
            }
            String[] packageNames = AccessibilityManagerService.this.mPackageManager.getPackagesForUid(resolvedUid);
            if (ArrayUtils.isEmpty(packageNames)) {
                return null;
            }
            return packageNames[0];
        }

        String[] computeValidReportedPackages(int callingUid, String targetPackage, int targetUid) {
            ArraySet<String> widgetPackages;
            if (UserHandle.getAppId(callingUid) == 1000) {
                return EmptyArray.STRING;
            }
            String[] uidPackages = {targetPackage};
            AppWidgetManagerInternal appWidgetManager = AccessibilityManagerService.this.getAppWidgetManager();
            if (appWidgetManager != null && (widgetPackages = appWidgetManager.getHostedWidgetPackages(targetUid)) != null && !widgetPackages.isEmpty()) {
                String[] validPackages = new String[uidPackages.length + widgetPackages.size()];
                System.arraycopy(uidPackages, 0, validPackages, 0, uidPackages.length);
                int widgetPackageCount = widgetPackages.size();
                for (int i = 0; i < widgetPackageCount; i++) {
                    validPackages[uidPackages.length + i] = widgetPackages.valueAt(i);
                }
                return validPackages;
            }
            return uidPackages;
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
                            activeWindowGone = false;
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
                return false;
            }
            Region windowInteractiveRegion = null;
            boolean windowInteractiveRegionChanged = false;
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
            if ((event.getEventType() & RETRIEVAL_ALLOWING_EVENT_TYPES) != 0) {
                return;
            }
            event.setSource(null);
        }

        public void updateActiveAndAccessibilityFocusedWindowLocked(int windowId, long nodeId, int eventType, int eventAction) {
            Object obj;
            switch (eventType) {
                case 32:
                    obj = AccessibilityManagerService.this.mLock;
                    synchronized (obj) {
                        if (AccessibilityManagerService.this.mWindowsForAccessibilityCallback == null) {
                            this.mFocusedWindowId = getFocusedWindowId();
                            if (windowId == this.mFocusedWindowId) {
                                this.mActiveWindowId = windowId;
                            }
                        }
                        break;
                    }
                    break;
                case 128:
                    obj = AccessibilityManagerService.this.mLock;
                    synchronized (obj) {
                        if (this.mTouchInteractionInProgress && this.mActiveWindowId != windowId) {
                            setActiveWindowLocked(windowId);
                        }
                        break;
                    }
                    break;
                case PackageManagerService.DumpState.DUMP_VERSION:
                    obj = AccessibilityManagerService.this.mLock;
                    synchronized (obj) {
                        if (this.mAccessibilityFocusedWindowId != windowId) {
                            AccessibilityManagerService.this.mMainHandler.obtainMessage(9, this.mAccessibilityFocusedWindowId, 0).sendToTarget();
                            AccessibilityManagerService.this.mSecurityPolicy.setAccessibilityFocusedWindowLocked(windowId);
                            this.mAccessibilityFocusNodeId = nodeId;
                        }
                        break;
                    }
                    break;
                case PackageManagerService.DumpState.DUMP_INSTALLS:
                    obj = AccessibilityManagerService.this.mLock;
                    synchronized (obj) {
                        if (this.mAccessibilityFocusNodeId == nodeId) {
                            this.mAccessibilityFocusNodeId = 2147483647L;
                        }
                        if (this.mAccessibilityFocusNodeId == 2147483647L && this.mAccessibilityFocusedWindowId == windowId && eventAction != 64) {
                            this.mAccessibilityFocusedWindowId = -1;
                        }
                        break;
                    }
                    break;
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
                this.mTouchInteractionInProgress = false;
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
            if (this.mActiveWindowId == windowId) {
                return;
            }
            this.mActiveWindowId = windowId;
            if (this.mWindows != null) {
                int windowCount = this.mWindows.size();
                for (int i = 0; i < windowCount; i++) {
                    AccessibilityWindowInfo window = this.mWindows.get(i);
                    window.setActive(window.getId() == windowId);
                }
            }
            notifyWindowsChanged();
        }

        private void setAccessibilityFocusedWindowLocked(int windowId) {
            if (this.mAccessibilityFocusedWindowId == windowId) {
                return;
            }
            this.mAccessibilityFocusedWindowId = windowId;
            if (this.mWindows != null) {
                int windowCount = this.mWindows.size();
                for (int i = 0; i < windowCount; i++) {
                    AccessibilityWindowInfo window = this.mWindows.get(i);
                    window.setAccessibilityFocused(window.getId() == windowId);
                }
            }
            notifyWindowsChanged();
        }

        private void notifyWindowsChanged() {
            if (AccessibilityManagerService.this.mWindowsForAccessibilityCallback == null) {
                return;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                AccessibilityEvent event = AccessibilityEvent.obtain(4194304);
                event.setEventTime(SystemClock.uptimeMillis());
                AccessibilityManagerService.this.sendAccessibilityEvent(event, AccessibilityManagerService.this.mCurrentUserId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean canGetAccessibilityNodeInfoLocked(Service service, int windowId) {
            if (canRetrieveWindowContentLocked(service)) {
                return isRetrievalAllowingWindow(windowId);
            }
            return false;
        }

        public boolean canRetrieveWindowsLocked(Service service) {
            if (canRetrieveWindowContentLocked(service)) {
                return service.mRetrieveInteractiveWindows;
            }
            return false;
        }

        public boolean canRetrieveWindowContentLocked(Service service) {
            return (service.mAccessibilityServiceInfo.getCapabilities() & 1) != 0;
        }

        public boolean canControlMagnification(Service service) {
            return (service.mAccessibilityServiceInfo.getCapabilities() & 16) != 0;
        }

        public boolean canPerformGestures(Service service) {
            return (service.mAccessibilityServiceInfo.getCapabilities() & 32) != 0;
        }

        private int resolveProfileParentLocked(int userId) {
            if (userId != AccessibilityManagerService.this.mCurrentUserId) {
                long identity = Binder.clearCallingIdentity();
                try {
                    UserInfo parent = AccessibilityManagerService.this.mUserManager.getProfileParent(userId);
                    if (parent != null) {
                        return parent.getUserHandle().getIdentifier();
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
            return Binder.getCallingPid() == Process.myPid() || callingUid == 2000 || userId == -2 || userId == -3;
        }

        private boolean isRetrievalAllowingWindow(int windowId) {
            return Binder.getCallingUid() == 1000 || windowId == this.mActiveWindowId || findWindowById(windowId) != null;
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
            return AccessibilityManagerService.this.mContext.checkCallingPermission(permission) == 0;
        }

        private int getFocusedWindowId() {
            int iFindWindowIdLocked;
            IBinder token = AccessibilityManagerService.this.mWindowManagerService.getFocusedWindowToken();
            synchronized (AccessibilityManagerService.this.mLock) {
                iFindWindowIdLocked = AccessibilityManagerService.this.findWindowIdLocked(token);
            }
            return iFindWindowIdLocked;
        }

        public void updateActiveWindowLocked() {
            IBinder token = AccessibilityManagerService.this.mWindowManagerService.getFocusedWindowToken();
            int windowId = AccessibilityManagerService.this.findWindowIdLocked(token);
            Slog.i(AccessibilityManagerService.LOG_TAG, "updateActiveWindow, windowId = " + windowId + ", mActiveWindowId = " + this.mActiveWindowId);
            if (windowId != this.mActiveWindowId) {
                this.mActiveWindowId = windowId;
            } else {
                this.mActiveWindowId = -1;
            }
        }
    }

    private class UserState {
        public boolean mAccessibilityFocusOnlyInActiveWindow;
        public boolean mHasDisplayColorAdjustment;
        public boolean mIsAutoclickEnabled;
        public boolean mIsDisplayMagnificationEnabled;
        public boolean mIsEnhancedWebAccessibilityEnabled;
        public boolean mIsFilterKeyEventsEnabled;
        public boolean mIsPerformGesturesEnabled;
        public boolean mIsTextHighContrastEnabled;
        public boolean mIsTouchExplorationEnabled;
        public ComponentName mServiceChangingSoftKeyboardMode;
        private int mUiAutomationFlags;
        private Service mUiAutomationService;
        private IAccessibilityServiceClient mUiAutomationServiceClient;
        private IBinder mUiAutomationServiceOwner;
        public final int mUserId;
        public final RemoteCallbackList<IAccessibilityManagerClient> mClients = new RemoteCallbackList<>();
        public final SparseArray<RemoteAccessibilityConnection> mInteractionConnections = new SparseArray<>();
        public final SparseArray<IBinder> mWindowTokens = new SparseArray<>();
        public final CopyOnWriteArrayList<Service> mBoundServices = new CopyOnWriteArrayList<>();
        public final Map<ComponentName, Service> mComponentNameToServiceMap = new HashMap();
        public final List<AccessibilityServiceInfo> mInstalledServices = new ArrayList();
        public final Set<ComponentName> mBindingServices = new HashSet();
        public final Set<ComponentName> mEnabledServices = new HashSet();
        public final Set<ComponentName> mTouchExplorationGrantedServices = new HashSet();
        public int mLastSentClientState = -1;
        public int mSoftKeyboardShowMode = 0;
        private final IBinder.DeathRecipient mUiAutomationSerivceOnwerDeathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                UserState.this.mUiAutomationServiceOwner.unlinkToDeath(UserState.this.mUiAutomationSerivceOnwerDeathRecipient, 0);
                UserState.this.mUiAutomationServiceOwner = null;
                if (UserState.this.mUiAutomationService == null) {
                    return;
                }
                UserState.this.mUiAutomationService.binderDied();
            }
        };

        public UserState(int userId) {
            this.mUserId = userId;
        }

        public int getClientState() {
            int clientState = 0;
            if (isHandlingAccessibilityEvents()) {
                clientState = 1;
            }
            if (isHandlingAccessibilityEvents() && this.mIsTouchExplorationEnabled) {
                clientState |= 2;
            }
            if (this.mIsTextHighContrastEnabled) {
                return clientState | 4;
            }
            return clientState;
        }

        public boolean isHandlingAccessibilityEvents() {
            return (this.mBoundServices.isEmpty() && this.mBindingServices.isEmpty()) ? false : true;
        }

        public void onSwitchToAnotherUser() {
            if (this.mUiAutomationService != null) {
                this.mUiAutomationService.binderDied();
            }
            AccessibilityManagerService.this.unbindAllServicesLocked(this);
            this.mBoundServices.clear();
            this.mBindingServices.clear();
            this.mLastSentClientState = -1;
            this.mEnabledServices.clear();
            this.mTouchExplorationGrantedServices.clear();
            this.mIsTouchExplorationEnabled = false;
            this.mIsEnhancedWebAccessibilityEnabled = false;
            this.mIsDisplayMagnificationEnabled = false;
            this.mIsAutoclickEnabled = false;
            this.mSoftKeyboardShowMode = 0;
        }

        public void destroyUiAutomationService() {
            this.mUiAutomationService = null;
            this.mUiAutomationFlags = 0;
            this.mUiAutomationServiceClient = null;
            if (this.mUiAutomationServiceOwner == null) {
                return;
            }
            this.mUiAutomationServiceOwner.unlinkToDeath(this.mUiAutomationSerivceOnwerDeathRecipient, 0);
            this.mUiAutomationServiceOwner = null;
        }

        boolean isUiAutomationSuppressingOtherServices() {
            return this.mUiAutomationService != null && (this.mUiAutomationFlags & 1) == 0;
        }
    }

    private final class AccessibilityContentObserver extends ContentObserver {
        private final Uri mAccessibilitySoftKeyboardModeUri;
        private final Uri mAutoclickEnabledUri;
        private final Uri mDisplayColorMatrixUri;
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
            this.mTouchExplorationEnabledUri = Settings.Secure.getUriFor("touch_exploration_enabled");
            this.mDisplayMagnificationEnabledUri = Settings.Secure.getUriFor("accessibility_display_magnification_enabled");
            this.mAutoclickEnabledUri = Settings.Secure.getUriFor("accessibility_autoclick_enabled");
            this.mEnabledAccessibilityServicesUri = Settings.Secure.getUriFor("enabled_accessibility_services");
            this.mTouchExplorationGrantedAccessibilityServicesUri = Settings.Secure.getUriFor("touch_exploration_granted_accessibility_services");
            this.mEnhancedWebAccessibilityUri = Settings.Secure.getUriFor("accessibility_script_injection");
            this.mDisplayInversionEnabledUri = Settings.Secure.getUriFor("accessibility_display_inversion_enabled");
            this.mDisplayDaltonizerEnabledUri = Settings.Secure.getUriFor("accessibility_display_daltonizer_enabled");
            this.mDisplayDaltonizerUri = Settings.Secure.getUriFor("accessibility_display_daltonizer");
            this.mDisplayColorMatrixUri = Settings.Secure.getUriFor("accessibility_display_color_matrix");
            this.mHighTextContrastUri = Settings.Secure.getUriFor("high_text_contrast_enabled");
            this.mAccessibilitySoftKeyboardModeUri = Settings.Secure.getUriFor("accessibility_soft_keyboard_mode");
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(this.mTouchExplorationEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mDisplayMagnificationEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mAutoclickEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mEnabledAccessibilityServicesUri, false, this, -1);
            contentResolver.registerContentObserver(this.mTouchExplorationGrantedAccessibilityServicesUri, false, this, -1);
            contentResolver.registerContentObserver(this.mEnhancedWebAccessibilityUri, false, this, -1);
            contentResolver.registerContentObserver(this.mDisplayInversionEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mDisplayDaltonizerEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mDisplayDaltonizerUri, false, this, -1);
            contentResolver.registerContentObserver(this.mDisplayColorMatrixUri, false, this, -1);
            contentResolver.registerContentObserver(this.mHighTextContrastUri, false, this, -1);
            contentResolver.registerContentObserver(this.mAccessibilitySoftKeyboardModeUri, false, this, -1);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (AccessibilityManagerService.this.mLock) {
                UserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                if (userState.isUiAutomationSuppressingOtherServices()) {
                    return;
                }
                if (this.mTouchExplorationEnabledUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readTouchExplorationEnabledSettingLocked(userState)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                } else if (this.mDisplayMagnificationEnabledUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readDisplayMagnificationEnabledSettingLocked(userState)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                } else if (this.mAutoclickEnabledUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readAutoclickEnabledSettingLocked(userState)) {
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
                } else if (this.mDisplayColorMatrixUri.equals(uri)) {
                    AccessibilityManagerService.this.updateDisplayColorAdjustmentSettingsLocked(userState);
                } else if (this.mHighTextContrastUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readHighTextContrastEnabledSettingLocked(userState)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                    }
                } else if (this.mAccessibilitySoftKeyboardModeUri.equals(uri) && AccessibilityManagerService.this.readSoftKeyboardShowModeChangedLocked(userState)) {
                    AccessibilityManagerService.this.notifySoftKeyboardShowModeChangedLocked(userState.mSoftKeyboardShowMode);
                    AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                }
            }
        }
    }

    private void manageAccessibilityServices() {
        UserState userState = getCurrentUserStateLocked();
        synchronized (this.mLock) {
            unbindAllServicesLocked(userState);
            scheduleUpdateClientsIfNeededLocked(userState);
        }
    }

    private void registerIPOReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_BOOT_IPO");
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                UserState userState = AccessibilityManagerService.this.getCurrentUserStateLocked();
                if ("android.intent.action.ACTION_BOOT_IPO".equals(intent.getAction())) {
                    AccessibilityManagerService.this.readConfigurationForUserStateLocked(userState);
                    AccessibilityManagerService.this.onUserStateChangedLocked(userState);
                } else {
                    if (!"android.intent.action.ACTION_SHUTDOWN_IPO".equals(intent.getAction())) {
                        return;
                    }
                    AccessibilityManagerService.this.manageAccessibilityServices();
                }
            }
        }, filter);
    }
}
