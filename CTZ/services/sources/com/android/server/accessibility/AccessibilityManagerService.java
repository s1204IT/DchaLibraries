package com.android.server.accessibility;

import android.R;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.app.ActivityManagerInternal;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManager;
import android.hardware.fingerprint.IFingerprintService;
import android.media.AudioManagerInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.provider.Settings;
import android.provider.SettingsStringUtil;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.IInputFilter;
import android.view.IWindow;
import android.view.KeyEvent;
import android.view.MagnificationSpec;
import android.view.View;
import android.view.WindowInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityInteractionClient;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.accessibility.IAccessibilityInteractionConnection;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import android.view.accessibility.IAccessibilityManager;
import android.view.accessibility.IAccessibilityManagerClient;
import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FunctionalUtils;
import com.android.internal.util.IntPair;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalServices;
import com.android.server.accessibility.AbstractAccessibilityServiceConnection;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.pm.DumpState;
import com.android.server.wm.WindowManagerInternal;
import com.mediatek.server.powerhal.PowerHalManager;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParserException;

public class AccessibilityManagerService extends IAccessibilityManager.Stub implements AbstractAccessibilityServiceConnection.SystemSupport {
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final boolean DEBUG = false;
    private static final String FUNCTION_DUMP = "dump";
    private static final String FUNCTION_REGISTER_UI_TEST_AUTOMATION_SERVICE = "registerUiTestAutomationService";
    private static final String GET_WINDOW_TOKEN = "getWindowToken";
    private static final String LOG_TAG = "AccessibilityManagerService";
    public static final int MAGNIFICATION_GESTURE_HANDLER_ID = 0;
    private static final String SET_PIP_ACTION_REPLACEMENT = "setPictureInPictureActionReplacingConnection";
    private static final String TEMPORARY_ENABLE_ACCESSIBILITY_UNTIL_KEYGUARD_REMOVED = "temporaryEnableAccessibilityStateUntilKeyguardRemoved";
    private static final int WAIT_FOR_USER_STATE_FULLY_INITIALIZED_MILLIS = 3000;
    private static final int WAIT_MOTION_INJECTOR_TIMEOUT_MILLIS = 1000;
    private static final int WAIT_WINDOWS_TIMEOUT_MILLIS = 5000;
    private static int sNextWindowId;
    private final AppOpsManager mAppOpsManager;
    private AppWidgetManagerInternal mAppWidgetService;
    private final Context mContext;
    private AlertDialog mEnableTouchExplorationDialog;
    private FingerprintGestureDispatcher mFingerprintGestureDispatcher;
    private final GlobalActionPerformer mGlobalActionPerformer;
    private boolean mHasInputFilter;
    private boolean mInitialized;
    private AccessibilityInputFilter mInputFilter;
    private InteractionBridge mInteractionBridge;
    private boolean mIsAccessibilityButtonShown;
    private KeyEventDispatcher mKeyEventDispatcher;
    private MagnificationController mMagnificationController;
    private final MainHandler mMainHandler;
    private MotionEventInjector mMotionEventInjector;
    private final PackageManager mPackageManager;
    private RemoteAccessibilityConnection mPictureInPictureActionReplacingConnection;
    private final PowerManager mPowerManager;
    private final UserManager mUserManager;
    private WindowsForAccessibilityCallback mWindowsForAccessibilityCallback;
    private static final int OWN_PROCESS_ID = Process.myPid();
    private static int sIdCounter = 1;
    private final Object mLock = new Object();
    private final TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);
    private final Rect mTempRect = new Rect();
    private final Rect mTempRect1 = new Rect();
    private final Point mTempPoint = new Point();
    private final Set<ComponentName> mTempComponentNameSet = new HashSet();
    private final List<AccessibilityServiceInfo> mTempAccessibilityServiceInfoList = new ArrayList();
    private final IntArray mTempIntArray = new IntArray(0);
    private final RemoteCallbackList<IAccessibilityManagerClient> mGlobalClients = new RemoteCallbackList<>();
    private final SparseArray<RemoteAccessibilityConnection> mGlobalInteractionConnections = new SparseArray<>();
    private final SparseArray<IBinder> mGlobalWindowTokens = new SparseArray<>();
    private final SparseArray<UserState> mUserStates = new SparseArray<>();
    private final UiAutomationManager mUiAutomationManager = new UiAutomationManager();
    private int mCurrentUserId = 0;
    private final WindowManagerInternal mWindowManagerService = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
    private final SecurityPolicy mSecurityPolicy = new SecurityPolicy();

    static int access$2508() {
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
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mMainHandler = new MainHandler(this.mContext.getMainLooper());
        this.mGlobalActionPerformer = new GlobalActionPerformer(this.mContext, this.mWindowManagerService);
        registerBroadcastReceivers();
        new AccessibilityContentObserver(this.mMainHandler).register(context.getContentResolver());
    }

    @Override
    public int getCurrentUserIdLocked() {
        return this.mCurrentUserId;
    }

    @Override
    public boolean isAccessibilityButtonShown() {
        return this.mIsAccessibilityButtonShown;
    }

    @Override
    public FingerprintGestureDispatcher getFingerprintGestureDispatcher() {
        return this.mFingerprintGestureDispatcher;
    }

    private UserState getUserState(int i) {
        UserState userStateLocked;
        synchronized (this.mLock) {
            userStateLocked = getUserStateLocked(i);
        }
        return userStateLocked;
    }

    private UserState getUserStateLocked(int i) {
        UserState userState = this.mUserStates.get(i);
        if (userState == null) {
            UserState userState2 = new UserState(i);
            this.mUserStates.put(i, userState2);
            return userState2;
        }
        return userState;
    }

    boolean getBindInstantServiceAllowed(int i) {
        return this.mSecurityPolicy.getBindInstantServiceAllowed(i);
    }

    void setBindInstantServiceAllowed(int i, boolean z) {
        this.mSecurityPolicy.setBindInstantServiceAllowed(i, z);
    }

    private void registerBroadcastReceivers() {
        new PackageMonitor() {
            public void onSomePackagesChanged() {
                synchronized (AccessibilityManagerService.this.mLock) {
                    if (getChangingUserId() != AccessibilityManagerService.this.mCurrentUserId) {
                        return;
                    }
                    UserState currentUserStateLocked = AccessibilityManagerService.this.getCurrentUserStateLocked();
                    currentUserStateLocked.mInstalledServices.clear();
                    if (AccessibilityManagerService.this.readConfigurationForUserStateLocked(currentUserStateLocked)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(currentUserStateLocked);
                    }
                }
            }

            public void onPackageUpdateFinished(String str, int i) {
                synchronized (AccessibilityManagerService.this.mLock) {
                    int changingUserId = getChangingUserId();
                    if (changingUserId != AccessibilityManagerService.this.mCurrentUserId) {
                        return;
                    }
                    UserState userStateLocked = AccessibilityManagerService.this.getUserStateLocked(changingUserId);
                    boolean z = false;
                    for (int size = userStateLocked.mBoundServices.size() - 1; size >= 0; size--) {
                        AccessibilityServiceConnection accessibilityServiceConnection = userStateLocked.mBoundServices.get(size);
                        if (accessibilityServiceConnection.mComponentName.getPackageName().equals(str)) {
                            accessibilityServiceConnection.unbindLocked();
                            z = true;
                        }
                    }
                    if (z) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(userStateLocked);
                    }
                }
            }

            public void onPackageRemoved(String str, int i) {
                synchronized (AccessibilityManagerService.this.mLock) {
                    int changingUserId = getChangingUserId();
                    if (changingUserId != AccessibilityManagerService.this.mCurrentUserId) {
                        return;
                    }
                    UserState userStateLocked = AccessibilityManagerService.this.getUserStateLocked(changingUserId);
                    Iterator<ComponentName> it = userStateLocked.mEnabledServices.iterator();
                    while (it.hasNext()) {
                        ComponentName next = it.next();
                        if (next.getPackageName().equals(str)) {
                            it.remove();
                            AccessibilityManagerService.this.persistComponentNamesToSettingLocked("enabled_accessibility_services", userStateLocked.mEnabledServices, changingUserId);
                            userStateLocked.mTouchExplorationGrantedServices.remove(next);
                            AccessibilityManagerService.this.persistComponentNamesToSettingLocked("touch_exploration_granted_accessibility_services", userStateLocked.mTouchExplorationGrantedServices, changingUserId);
                            AccessibilityManagerService.this.onUserStateChangedLocked(userStateLocked);
                            return;
                        }
                    }
                }
            }

            public boolean onHandleForceStop(Intent intent, String[] strArr, int i, boolean z) {
                synchronized (AccessibilityManagerService.this.mLock) {
                    int changingUserId = getChangingUserId();
                    if (changingUserId != AccessibilityManagerService.this.mCurrentUserId) {
                        return false;
                    }
                    UserState userStateLocked = AccessibilityManagerService.this.getUserStateLocked(changingUserId);
                    Iterator<ComponentName> it = userStateLocked.mEnabledServices.iterator();
                    while (it.hasNext()) {
                        String packageName = it.next().getPackageName();
                        for (String str : strArr) {
                            if (packageName.equals(str)) {
                                if (!z) {
                                    return true;
                                }
                                it.remove();
                                AccessibilityManagerService.this.persistComponentNamesToSettingLocked("enabled_accessibility_services", userStateLocked.mEnabledServices, changingUserId);
                                AccessibilityManagerService.this.onUserStateChangedLocked(userStateLocked);
                            }
                        }
                    }
                    return false;
                }
            }
        }.register(this.mContext, (Looper) null, UserHandle.ALL, true);
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
                    synchronized (AccessibilityManagerService.this.mLock) {
                        UserState currentUserStateLocked = AccessibilityManagerService.this.getCurrentUserStateLocked();
                        if (AccessibilityManagerService.this.readConfigurationForUserStateLocked(currentUserStateLocked)) {
                            AccessibilityManagerService.this.onUserStateChangedLocked(currentUserStateLocked);
                        }
                    }
                    return;
                }
                if ("android.os.action.SETTING_RESTORED".equals(action) && "enabled_accessibility_services".equals(intent.getStringExtra("setting_name"))) {
                    synchronized (AccessibilityManagerService.this.mLock) {
                        AccessibilityManagerService.this.restoreEnabledAccessibilityServicesLocked(intent.getStringExtra("previous_value"), intent.getStringExtra("new_value"));
                    }
                }
            }
        }, UserHandle.ALL, intentFilter, null, null);
    }

    public long addClient(IAccessibilityManagerClient iAccessibilityManagerClient, int i) {
        synchronized (this.mLock) {
            int iResolveCallingUserIdEnforcingPermissionsLocked = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(i);
            UserState userStateLocked = getUserStateLocked(iResolveCallingUserIdEnforcingPermissionsLocked);
            Client client = new Client(iAccessibilityManagerClient, Binder.getCallingUid(), userStateLocked);
            if (this.mSecurityPolicy.isCallerInteractingAcrossUsers(i)) {
                this.mGlobalClients.register(iAccessibilityManagerClient, client);
                return IntPair.of(userStateLocked.getClientState(), client.mLastSentRelevantEventTypes);
            }
            userStateLocked.mUserClients.register(iAccessibilityManagerClient, client);
            return IntPair.of(iResolveCallingUserIdEnforcingPermissionsLocked == this.mCurrentUserId ? userStateLocked.getClientState() : 0, client.mLastSentRelevantEventTypes);
        }
    }

    public void sendAccessibilityEvent(AccessibilityEvent accessibilityEvent, int i) {
        boolean z;
        AccessibilityWindowInfo pictureInPictureWindow;
        synchronized (this.mLock) {
            if (accessibilityEvent.getWindowId() == -3 && (pictureInPictureWindow = this.mSecurityPolicy.getPictureInPictureWindow()) != null) {
                accessibilityEvent.setWindowId(pictureInPictureWindow.getId());
            }
            int iResolveCallingUserIdEnforcingPermissionsLocked = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(i);
            accessibilityEvent.setPackageName(this.mSecurityPolicy.resolveValidReportedPackageLocked(accessibilityEvent.getPackageName(), UserHandle.getCallingAppId(), iResolveCallingUserIdEnforcingPermissionsLocked));
            if (iResolveCallingUserIdEnforcingPermissionsLocked != this.mCurrentUserId) {
                z = false;
            } else {
                if (this.mSecurityPolicy.canDispatchAccessibilityEventLocked(accessibilityEvent)) {
                    this.mSecurityPolicy.updateActiveAndAccessibilityFocusedWindowLocked(accessibilityEvent.getWindowId(), accessibilityEvent.getSourceNodeId(), accessibilityEvent.getEventType(), accessibilityEvent.getAction());
                    this.mSecurityPolicy.updateEventSourceLocked(accessibilityEvent);
                    z = true;
                } else {
                    z = false;
                }
                if (this.mHasInputFilter && this.mInputFilter != null) {
                    this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                        @Override
                        public final void accept(Object obj, Object obj2) {
                            ((AccessibilityManagerService) obj).sendAccessibilityEventToInputFilter((AccessibilityEvent) obj2);
                        }
                    }, this, AccessibilityEvent.obtain(accessibilityEvent)));
                }
            }
        }
        if (z) {
            if (accessibilityEvent.getEventType() == 32 && this.mWindowsForAccessibilityCallback != null) {
                ((WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class)).computeWindowsForAccessibility();
            }
            synchronized (this.mLock) {
                notifyAccessibilityServicesDelayedLocked(accessibilityEvent, false);
                notifyAccessibilityServicesDelayedLocked(accessibilityEvent, true);
                this.mUiAutomationManager.sendAccessibilityEventLocked(accessibilityEvent);
            }
        }
        if (OWN_PROCESS_ID != Binder.getCallingPid()) {
            accessibilityEvent.recycle();
        }
    }

    private void sendAccessibilityEventToInputFilter(AccessibilityEvent accessibilityEvent) {
        synchronized (this.mLock) {
            if (this.mHasInputFilter && this.mInputFilter != null) {
                this.mInputFilter.notifyAccessibilityEvent(accessibilityEvent);
            }
        }
        accessibilityEvent.recycle();
    }

    public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList(int i) {
        List<AccessibilityServiceInfo> list;
        synchronized (this.mLock) {
            list = getUserStateLocked(this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(i)).mInstalledServices;
        }
        return list;
    }

    public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int i, int i2) {
        synchronized (this.mLock) {
            UserState userStateLocked = getUserStateLocked(this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(i2));
            if (this.mUiAutomationManager.suppressingAccessibilityServicesLocked()) {
                return Collections.emptyList();
            }
            ArrayList<AccessibilityServiceConnection> arrayList = userStateLocked.mBoundServices;
            int size = arrayList.size();
            ArrayList arrayList2 = new ArrayList(size);
            for (int i3 = 0; i3 < size; i3++) {
                AccessibilityServiceConnection accessibilityServiceConnection = arrayList.get(i3);
                if ((accessibilityServiceConnection.mFeedbackType & i) != 0) {
                    arrayList2.add(accessibilityServiceConnection.getServiceInfo());
                }
            }
            return arrayList2;
        }
    }

    public void interrupt(int i) {
        synchronized (this.mLock) {
            int iResolveCallingUserIdEnforcingPermissionsLocked = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(i);
            if (iResolveCallingUserIdEnforcingPermissionsLocked != this.mCurrentUserId) {
                return;
            }
            ArrayList<AccessibilityServiceConnection> arrayList = getUserStateLocked(iResolveCallingUserIdEnforcingPermissionsLocked).mBoundServices;
            int size = arrayList.size();
            ArrayList arrayList2 = new ArrayList(size);
            for (int i2 = 0; i2 < size; i2++) {
                AccessibilityServiceConnection accessibilityServiceConnection = arrayList.get(i2);
                IBinder iBinder = accessibilityServiceConnection.mService;
                IAccessibilityServiceClient iAccessibilityServiceClient = accessibilityServiceConnection.mServiceInterface;
                if (iBinder != null && iAccessibilityServiceClient != null) {
                    arrayList2.add(iAccessibilityServiceClient);
                }
            }
            int size2 = arrayList2.size();
            for (int i3 = 0; i3 < size2; i3++) {
                try {
                    ((IAccessibilityServiceClient) arrayList2.get(i3)).onInterrupt();
                } catch (RemoteException e) {
                    Slog.e(LOG_TAG, "Error sending interrupt request to " + arrayList2.get(i3), e);
                }
            }
        }
    }

    public int addAccessibilityInteractionConnection(IWindow iWindow, IAccessibilityInteractionConnection iAccessibilityInteractionConnection, String str, int i) throws RemoteException {
        int i2;
        synchronized (this.mLock) {
            int iResolveCallingUserIdEnforcingPermissionsLocked = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(i);
            int uid = UserHandle.getUid(iResolveCallingUserIdEnforcingPermissionsLocked, UserHandle.getCallingAppId());
            String strResolveValidReportedPackageLocked = this.mSecurityPolicy.resolveValidReportedPackageLocked(str, UserHandle.getCallingAppId(), iResolveCallingUserIdEnforcingPermissionsLocked);
            i2 = sNextWindowId;
            sNextWindowId = i2 + 1;
            if (this.mSecurityPolicy.isCallerInteractingAcrossUsers(i)) {
                RemoteAccessibilityConnection remoteAccessibilityConnection = new RemoteAccessibilityConnection(i2, iAccessibilityInteractionConnection, strResolveValidReportedPackageLocked, uid, -1);
                remoteAccessibilityConnection.linkToDeath();
                this.mGlobalInteractionConnections.put(i2, remoteAccessibilityConnection);
                this.mGlobalWindowTokens.put(i2, iWindow.asBinder());
            } else {
                RemoteAccessibilityConnection remoteAccessibilityConnection2 = new RemoteAccessibilityConnection(i2, iAccessibilityInteractionConnection, strResolveValidReportedPackageLocked, uid, iResolveCallingUserIdEnforcingPermissionsLocked);
                remoteAccessibilityConnection2.linkToDeath();
                UserState userStateLocked = getUserStateLocked(iResolveCallingUserIdEnforcingPermissionsLocked);
                userStateLocked.mInteractionConnections.put(i2, remoteAccessibilityConnection2);
                userStateLocked.mWindowTokens.put(i2, iWindow.asBinder());
            }
        }
        return i2;
    }

    public void removeAccessibilityInteractionConnection(IWindow iWindow) {
        synchronized (this.mLock) {
            this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(UserHandle.getCallingUserId());
            IBinder iBinderAsBinder = iWindow.asBinder();
            if (removeAccessibilityInteractionConnectionInternalLocked(iBinderAsBinder, this.mGlobalWindowTokens, this.mGlobalInteractionConnections) >= 0) {
                return;
            }
            int size = this.mUserStates.size();
            for (int i = 0; i < size; i++) {
                UserState userStateValueAt = this.mUserStates.valueAt(i);
                if (removeAccessibilityInteractionConnectionInternalLocked(iBinderAsBinder, userStateValueAt.mWindowTokens, userStateValueAt.mInteractionConnections) >= 0) {
                    return;
                }
            }
        }
    }

    private int removeAccessibilityInteractionConnectionInternalLocked(IBinder iBinder, SparseArray<IBinder> sparseArray, SparseArray<RemoteAccessibilityConnection> sparseArray2) {
        int size = sparseArray.size();
        for (int i = 0; i < size; i++) {
            if (sparseArray.valueAt(i) == iBinder) {
                int iKeyAt = sparseArray.keyAt(i);
                sparseArray.removeAt(i);
                sparseArray2.get(iKeyAt).unlinkToDeath();
                sparseArray2.remove(iKeyAt);
                return iKeyAt;
            }
        }
        return -1;
    }

    public void setPictureInPictureActionReplacingConnection(IAccessibilityInteractionConnection iAccessibilityInteractionConnection) throws RemoteException {
        this.mSecurityPolicy.enforceCallingPermission("android.permission.MODIFY_ACCESSIBILITY_DATA", SET_PIP_ACTION_REPLACEMENT);
        synchronized (this.mLock) {
            if (this.mPictureInPictureActionReplacingConnection != null) {
                this.mPictureInPictureActionReplacingConnection.unlinkToDeath();
                this.mPictureInPictureActionReplacingConnection = null;
            }
            if (iAccessibilityInteractionConnection != null) {
                RemoteAccessibilityConnection remoteAccessibilityConnection = new RemoteAccessibilityConnection(-3, iAccessibilityInteractionConnection, "foo.bar.baz", 1000, -1);
                this.mPictureInPictureActionReplacingConnection = remoteAccessibilityConnection;
                remoteAccessibilityConnection.linkToDeath();
            }
        }
    }

    public void registerUiTestAutomationService(IBinder iBinder, IAccessibilityServiceClient iAccessibilityServiceClient, AccessibilityServiceInfo accessibilityServiceInfo, int i) {
        this.mSecurityPolicy.enforceCallingPermission("android.permission.RETRIEVE_WINDOW_CONTENT", FUNCTION_REGISTER_UI_TEST_AUTOMATION_SERVICE);
        synchronized (this.mLock) {
            UiAutomationManager uiAutomationManager = this.mUiAutomationManager;
            Context context = this.mContext;
            int i2 = sIdCounter;
            sIdCounter = i2 + 1;
            uiAutomationManager.registerUiTestAutomationServiceLocked(iBinder, iAccessibilityServiceClient, context, accessibilityServiceInfo, i2, this.mMainHandler, this.mLock, this.mSecurityPolicy, this, this.mWindowManagerService, this.mGlobalActionPerformer, i);
            onUserStateChangedLocked(getCurrentUserStateLocked());
        }
    }

    public void unregisterUiTestAutomationService(IAccessibilityServiceClient iAccessibilityServiceClient) {
        synchronized (this.mLock) {
            this.mUiAutomationManager.unregisterUiTestAutomationServiceLocked(iAccessibilityServiceClient);
        }
    }

    public void temporaryEnableAccessibilityStateUntilKeyguardRemoved(ComponentName componentName, boolean z) {
        this.mSecurityPolicy.enforceCallingPermission("android.permission.TEMPORARY_ENABLE_ACCESSIBILITY", TEMPORARY_ENABLE_ACCESSIBILITY_UNTIL_KEYGUARD_REMOVED);
        if (!this.mWindowManagerService.isKeyguardLocked()) {
            return;
        }
        synchronized (this.mLock) {
            UserState currentUserStateLocked = getCurrentUserStateLocked();
            currentUserStateLocked.mIsTouchExplorationEnabled = z;
            currentUserStateLocked.mIsDisplayMagnificationEnabled = false;
            currentUserStateLocked.mIsNavBarMagnificationEnabled = false;
            currentUserStateLocked.mIsAutoclickEnabled = false;
            currentUserStateLocked.mEnabledServices.clear();
            currentUserStateLocked.mEnabledServices.add(componentName);
            currentUserStateLocked.mBindingServices.clear();
            currentUserStateLocked.mTouchExplorationGrantedServices.clear();
            currentUserStateLocked.mTouchExplorationGrantedServices.add(componentName);
            onUserStateChangedLocked(currentUserStateLocked);
        }
    }

    public IBinder getWindowToken(int i, int i2) {
        this.mSecurityPolicy.enforceCallingPermission("android.permission.RETRIEVE_WINDOW_TOKEN", GET_WINDOW_TOKEN);
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(i2) != this.mCurrentUserId) {
                return null;
            }
            if (this.mSecurityPolicy.findA11yWindowInfoById(i) == null) {
                return null;
            }
            return findWindowTokenLocked(i);
        }
    }

    public void notifyAccessibilityButtonClicked() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE") != 0) {
            throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR_SERVICE");
        }
        synchronized (this.mLock) {
            notifyAccessibilityButtonClickedLocked();
        }
    }

    public void notifyAccessibilityButtonVisibilityChanged(boolean z) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.STATUS_BAR_SERVICE") != 0) {
            throw new SecurityException("Caller does not hold permission android.permission.STATUS_BAR_SERVICE");
        }
        synchronized (this.mLock) {
            notifyAccessibilityButtonVisibilityChangedLocked(z);
        }
    }

    boolean onGesture(int i) {
        boolean zNotifyGestureLocked;
        synchronized (this.mLock) {
            zNotifyGestureLocked = notifyGestureLocked(i, false);
            if (!zNotifyGestureLocked) {
                zNotifyGestureLocked = notifyGestureLocked(i, true);
            }
        }
        return zNotifyGestureLocked;
    }

    @VisibleForTesting
    public boolean notifyKeyEvent(KeyEvent keyEvent, int i) {
        synchronized (this.mLock) {
            ArrayList<AccessibilityServiceConnection> arrayList = getCurrentUserStateLocked().mBoundServices;
            if (arrayList.isEmpty()) {
                return false;
            }
            return getKeyEventDispatcher().notifyKeyEventLocked(keyEvent, i, arrayList);
        }
    }

    public void notifyMagnificationChanged(Region region, float f, float f2, float f3) {
        synchronized (this.mLock) {
            notifyClearAccessibilityCacheLocked();
            notifyMagnificationChangedLocked(region, f, f2, f3);
        }
    }

    void setMotionEventInjector(MotionEventInjector motionEventInjector) {
        synchronized (this.mLock) {
            this.mMotionEventInjector = motionEventInjector;
            this.mLock.notifyAll();
        }
    }

    @Override
    public MotionEventInjector getMotionEventInjectorLocked() {
        long jUptimeMillis = SystemClock.uptimeMillis() + 1000;
        while (this.mMotionEventInjector == null && SystemClock.uptimeMillis() < jUptimeMillis) {
            try {
                this.mLock.wait(jUptimeMillis - SystemClock.uptimeMillis());
            } catch (InterruptedException e) {
            }
        }
        if (this.mMotionEventInjector == null) {
            Slog.e(LOG_TAG, "MotionEventInjector installation timed out");
        }
        return this.mMotionEventInjector;
    }

    boolean getAccessibilityFocusClickPointInScreen(Point point) {
        return getInteractionBridge().getAccessibilityFocusClickPointInScreenNotLocked(point);
    }

    public boolean performActionOnAccessibilityFocusedItem(AccessibilityNodeInfo.AccessibilityAction accessibilityAction) {
        return getInteractionBridge().performActionOnAccessibilityFocusedItemNotLocked(accessibilityAction);
    }

    boolean getWindowBounds(int i, Rect rect) {
        IBinder iBinder;
        synchronized (this.mLock) {
            iBinder = this.mGlobalWindowTokens.get(i);
            if (iBinder == null) {
                iBinder = getCurrentUserStateLocked().mWindowTokens.get(i);
            }
        }
        this.mWindowManagerService.getWindowFrame(iBinder, rect);
        if (!rect.isEmpty()) {
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

    private void switchUser(int i) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId == i && this.mInitialized) {
                return;
            }
            UserState currentUserStateLocked = getCurrentUserStateLocked();
            currentUserStateLocked.onSwitchToAnotherUserLocked();
            if (currentUserStateLocked.mUserClients.getRegisteredCallbackCount() > 0) {
                this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() {
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((AccessibilityManagerService) obj).sendStateToClients(((Integer) obj2).intValue(), ((Integer) obj3).intValue());
                    }
                }, this, 0, Integer.valueOf(currentUserStateLocked.mUserId)));
            }
            boolean z = true;
            if (((UserManager) this.mContext.getSystemService("user")).getUsers().size() <= 1) {
                z = false;
            }
            this.mCurrentUserId = i;
            UserState currentUserStateLocked2 = getCurrentUserStateLocked();
            readConfigurationForUserStateLocked(currentUserStateLocked2);
            onUserStateChangedLocked(currentUserStateLocked2);
            if (z) {
                this.mMainHandler.sendMessageDelayed(PooledLambda.obtainMessage(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((AccessibilityManagerService) obj).announceNewUserIfNeeded();
                    }
                }, this), 3000L);
            }
        }
    }

    private void announceNewUserIfNeeded() {
        synchronized (this.mLock) {
            if (getCurrentUserStateLocked().isHandlingAccessibilityEvents()) {
                String string = this.mContext.getString(R.string.mmcc_illegal_ms, ((UserManager) this.mContext.getSystemService("user")).getUserInfo(this.mCurrentUserId).name);
                AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(16384);
                accessibilityEventObtain.getText().add(string);
                sendAccessibilityEventLocked(accessibilityEventObtain, this.mCurrentUserId);
            }
        }
    }

    private void unlockUser(int i) {
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.resolveProfileParentLocked(i) == this.mCurrentUserId) {
                onUserStateChangedLocked(getUserStateLocked(this.mCurrentUserId));
            }
        }
    }

    private void removeUser(int i) {
        synchronized (this.mLock) {
            this.mUserStates.remove(i);
        }
    }

    void restoreEnabledAccessibilityServicesLocked(String str, String str2) {
        readComponentNamesFromStringLocked(str, this.mTempComponentNameSet, false);
        readComponentNamesFromStringLocked(str2, this.mTempComponentNameSet, true);
        UserState userStateLocked = getUserStateLocked(0);
        userStateLocked.mEnabledServices.clear();
        userStateLocked.mEnabledServices.addAll(this.mTempComponentNameSet);
        persistComponentNamesToSettingLocked("enabled_accessibility_services", userStateLocked.mEnabledServices, 0);
        onUserStateChangedLocked(userStateLocked);
    }

    private InteractionBridge getInteractionBridge() {
        InteractionBridge interactionBridge;
        synchronized (this.mLock) {
            if (this.mInteractionBridge == null) {
                this.mInteractionBridge = new InteractionBridge();
            }
            interactionBridge = this.mInteractionBridge;
        }
        return interactionBridge;
    }

    private boolean notifyGestureLocked(int i, boolean z) {
        UserState currentUserStateLocked = getCurrentUserStateLocked();
        for (int size = currentUserStateLocked.mBoundServices.size() - 1; size >= 0; size--) {
            AccessibilityServiceConnection accessibilityServiceConnection = currentUserStateLocked.mBoundServices.get(size);
            if (accessibilityServiceConnection.mRequestTouchExplorationMode && accessibilityServiceConnection.mIsDefault == z) {
                accessibilityServiceConnection.notifyGesture(i);
                return true;
            }
        }
        return false;
    }

    private void notifyClearAccessibilityCacheLocked() {
        UserState currentUserStateLocked = getCurrentUserStateLocked();
        for (int size = currentUserStateLocked.mBoundServices.size() - 1; size >= 0; size--) {
            currentUserStateLocked.mBoundServices.get(size).notifyClearAccessibilityNodeInfoCache();
        }
    }

    private void notifyMagnificationChangedLocked(Region region, float f, float f2, float f3) {
        UserState currentUserStateLocked = getCurrentUserStateLocked();
        for (int size = currentUserStateLocked.mBoundServices.size() - 1; size >= 0; size--) {
            currentUserStateLocked.mBoundServices.get(size).notifyMagnificationChangedLocked(region, f, f2, f3);
        }
    }

    private void notifySoftKeyboardShowModeChangedLocked(int i) {
        UserState currentUserStateLocked = getCurrentUserStateLocked();
        for (int size = currentUserStateLocked.mBoundServices.size() - 1; size >= 0; size--) {
            currentUserStateLocked.mBoundServices.get(size).notifySoftKeyboardShowModeChangedLocked(i);
        }
    }

    private void notifyAccessibilityButtonClickedLocked() {
        UserState currentUserStateLocked = getCurrentUserStateLocked();
        boolean z = currentUserStateLocked.mIsNavBarMagnificationEnabled;
        int size = currentUserStateLocked.mBoundServices.size() - 1;
        int i = z;
        while (size >= 0) {
            if (currentUserStateLocked.mBoundServices.get(size).mRequestAccessibilityButton) {
                i++;
            }
            size--;
            i = i;
        }
        if (i == 0) {
            return;
        }
        if (i == 1) {
            if (currentUserStateLocked.mIsNavBarMagnificationEnabled) {
                this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((AccessibilityManagerService) obj).sendAccessibilityButtonToInputFilter();
                    }
                }, this));
                return;
            }
            for (int size2 = currentUserStateLocked.mBoundServices.size() - 1; size2 >= 0; size2--) {
                AccessibilityServiceConnection accessibilityServiceConnection = currentUserStateLocked.mBoundServices.get(size2);
                if (accessibilityServiceConnection.mRequestAccessibilityButton) {
                    accessibilityServiceConnection.notifyAccessibilityButtonClickedLocked();
                    return;
                }
            }
            return;
        }
        if (currentUserStateLocked.mServiceAssignedToAccessibilityButton == null && !currentUserStateLocked.mIsNavBarMagnificationAssignedToAccessibilityButton) {
            this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ((AccessibilityManagerService) obj).showAccessibilityButtonTargetSelection();
                }
            }, this));
        } else {
            if (currentUserStateLocked.mIsNavBarMagnificationEnabled && currentUserStateLocked.mIsNavBarMagnificationAssignedToAccessibilityButton) {
                this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        ((AccessibilityManagerService) obj).sendAccessibilityButtonToInputFilter();
                    }
                }, this));
                return;
            }
            for (int size3 = currentUserStateLocked.mBoundServices.size() - 1; size3 >= 0; size3--) {
                AccessibilityServiceConnection accessibilityServiceConnection2 = currentUserStateLocked.mBoundServices.get(size3);
                if (accessibilityServiceConnection2.mRequestAccessibilityButton && accessibilityServiceConnection2.mComponentName.equals(currentUserStateLocked.mServiceAssignedToAccessibilityButton)) {
                    accessibilityServiceConnection2.notifyAccessibilityButtonClickedLocked();
                    return;
                }
            }
        }
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((AccessibilityManagerService) obj).showAccessibilityButtonTargetSelection();
            }
        }, this));
    }

    private void sendAccessibilityButtonToInputFilter() {
        synchronized (this.mLock) {
            if (this.mHasInputFilter && this.mInputFilter != null) {
                this.mInputFilter.notifyAccessibilityButtonClicked();
            }
        }
    }

    private void showAccessibilityButtonTargetSelection() {
        Intent intent = new Intent("com.android.internal.intent.action.CHOOSE_ACCESSIBILITY_BUTTON");
        intent.addFlags(268468224);
        this.mContext.startActivityAsUser(intent, UserHandle.of(this.mCurrentUserId));
    }

    private void notifyAccessibilityButtonVisibilityChangedLocked(boolean z) {
        UserState currentUserStateLocked = getCurrentUserStateLocked();
        this.mIsAccessibilityButtonShown = z;
        for (int size = currentUserStateLocked.mBoundServices.size() - 1; size >= 0; size--) {
            AccessibilityServiceConnection accessibilityServiceConnection = currentUserStateLocked.mBoundServices.get(size);
            if (accessibilityServiceConnection.mRequestAccessibilityButton) {
                accessibilityServiceConnection.notifyAccessibilityButtonAvailabilityChangedLocked(accessibilityServiceConnection.isAccessibilityButtonAvailableLocked(currentUserStateLocked));
            }
        }
    }

    private void removeAccessibilityInteractionConnectionLocked(int i, int i2) {
        if (i2 == -1) {
            this.mGlobalWindowTokens.remove(i);
            this.mGlobalInteractionConnections.remove(i);
        } else {
            UserState currentUserStateLocked = getCurrentUserStateLocked();
            currentUserStateLocked.mWindowTokens.remove(i);
            currentUserStateLocked.mInteractionConnections.remove(i);
        }
    }

    private boolean readInstalledAccessibilityServiceLocked(UserState userState) {
        int i;
        this.mTempAccessibilityServiceInfoList.clear();
        if (userState.mBindInstantServiceAllowed) {
            i = 9207940;
        } else {
            i = 819332;
        }
        List listQueryIntentServicesAsUser = this.mPackageManager.queryIntentServicesAsUser(new Intent("android.accessibilityservice.AccessibilityService"), i, this.mCurrentUserId);
        int size = listQueryIntentServicesAsUser.size();
        for (int i2 = 0; i2 < size; i2++) {
            ResolveInfo resolveInfo = (ResolveInfo) listQueryIntentServicesAsUser.get(i2);
            if (canRegisterService(resolveInfo.serviceInfo)) {
                try {
                    this.mTempAccessibilityServiceInfoList.add(new AccessibilityServiceInfo(resolveInfo, this.mContext));
                } catch (IOException | XmlPullParserException e) {
                    Slog.e(LOG_TAG, "Error while initializing AccessibilityServiceInfo", e);
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
        return false;
    }

    private boolean canRegisterService(ServiceInfo serviceInfo) {
        if (!"android.permission.BIND_ACCESSIBILITY_SERVICE".equals(serviceInfo.permission)) {
            Slog.w(LOG_TAG, "Skipping accessibility service " + new ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToShortString() + ": it does not require the permission android.permission.BIND_ACCESSIBILITY_SERVICE");
            return false;
        }
        if (this.mAppOpsManager.noteOpNoThrow("android:bind_accessibility_service", serviceInfo.applicationInfo.uid, serviceInfo.packageName) != 0) {
            Slog.w(LOG_TAG, "Skipping accessibility service " + new ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToShortString() + ": disallowed by AppOps");
            return false;
        }
        return true;
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

    private void notifyAccessibilityServicesDelayedLocked(AccessibilityEvent accessibilityEvent, boolean z) {
        try {
            UserState currentUserStateLocked = getCurrentUserStateLocked();
            int size = currentUserStateLocked.mBoundServices.size();
            for (int i = 0; i < size; i++) {
                AccessibilityServiceConnection accessibilityServiceConnection = currentUserStateLocked.mBoundServices.get(i);
                if (accessibilityServiceConnection.mIsDefault == z) {
                    accessibilityServiceConnection.notifyAccessibilityEvent(accessibilityEvent);
                }
            }
        } catch (IndexOutOfBoundsException e) {
        }
    }

    private void updateRelevantEventsLocked(final UserState userState) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public final void run() {
                AccessibilityManagerService accessibilityManagerService = this.f$0;
                AccessibilityManagerService.UserState userState2 = userState;
                accessibilityManagerService.broadcastToClients(userState2, FunctionalUtils.ignoreRemoteException(new FunctionalUtils.RemoteExceptionIgnoringConsumer() {
                    public final void acceptOrThrow(Object obj) throws RemoteException {
                        AccessibilityManagerService.lambda$updateRelevantEventsLocked$0(this.f$0, userState2, (AccessibilityManagerService.Client) obj);
                    }
                }));
            }
        });
    }

    public static void lambda$updateRelevantEventsLocked$0(AccessibilityManagerService accessibilityManagerService, UserState userState, Client client) throws RemoteException {
        int iComputeRelevantEventTypesLocked;
        boolean z;
        synchronized (accessibilityManagerService.mLock) {
            iComputeRelevantEventTypesLocked = accessibilityManagerService.computeRelevantEventTypesLocked(userState, client);
            if (client.mLastSentRelevantEventTypes != iComputeRelevantEventTypesLocked) {
                client.mLastSentRelevantEventTypes = iComputeRelevantEventTypesLocked;
                z = true;
            } else {
                z = false;
            }
        }
        if (z) {
            client.mCallback.setRelevantEventTypes(iComputeRelevantEventTypesLocked);
        }
    }

    private int computeRelevantEventTypesLocked(UserState userState, Client client) {
        int relevantEventTypes;
        int size = userState.mBoundServices.size();
        int relevantEventTypes2 = 0;
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            AccessibilityServiceConnection accessibilityServiceConnection = userState.mBoundServices.get(i2);
            if (isClientInPackageWhitelist(accessibilityServiceConnection.getServiceInfo(), client)) {
                relevantEventTypes = accessibilityServiceConnection.getRelevantEventTypes();
            } else {
                relevantEventTypes = 0;
            }
            i |= relevantEventTypes;
        }
        if (isClientInPackageWhitelist(this.mUiAutomationManager.getServiceInfo(), client)) {
            relevantEventTypes2 = this.mUiAutomationManager.getRelevantEventTypes();
        }
        return i | relevantEventTypes2;
    }

    private static boolean isClientInPackageWhitelist(AccessibilityServiceInfo accessibilityServiceInfo, Client client) {
        if (accessibilityServiceInfo == null) {
            return false;
        }
        String[] strArr = client.mPackageNames;
        boolean zIsEmpty = ArrayUtils.isEmpty(accessibilityServiceInfo.packageNames);
        if (!zIsEmpty && strArr != null) {
            for (String str : strArr) {
                if (ArrayUtils.contains(accessibilityServiceInfo.packageNames, str)) {
                    return true;
                }
            }
            return zIsEmpty;
        }
        return zIsEmpty;
    }

    private void broadcastToClients(UserState userState, Consumer<Client> consumer) {
        this.mGlobalClients.broadcastForEachCookie(consumer);
        userState.mUserClients.broadcastForEachCookie(consumer);
    }

    private void unbindAllServicesLocked(UserState userState) {
        ArrayList<AccessibilityServiceConnection> arrayList = userState.mBoundServices;
        for (int size = arrayList.size(); size > 0; size--) {
            arrayList.get(0).unbindLocked();
        }
    }

    private void readComponentNamesFromSettingLocked(String str, int i, Set<ComponentName> set) {
        readComponentNamesFromStringLocked(Settings.Secure.getStringForUser(this.mContext.getContentResolver(), str, i), set, false);
    }

    private void readComponentNamesFromStringLocked(String str, Set<ComponentName> set, boolean z) {
        ComponentName componentNameUnflattenFromString;
        if (!z) {
            set.clear();
        }
        if (str != null) {
            TextUtils.SimpleStringSplitter simpleStringSplitter = this.mStringColonSplitter;
            simpleStringSplitter.setString(str);
            while (simpleStringSplitter.hasNext()) {
                String next = simpleStringSplitter.next();
                if (next != null && next.length() > 0 && (componentNameUnflattenFromString = ComponentName.unflattenFromString(next)) != null) {
                    set.add(componentNameUnflattenFromString);
                }
            }
        }
    }

    @Override
    public void persistComponentNamesToSettingLocked(String str, Set<ComponentName> set, int i) {
        StringBuilder sb = new StringBuilder();
        for (ComponentName componentName : set) {
            if (sb.length() > 0) {
                sb.append(COMPONENT_NAME_SEPARATOR);
            }
            sb.append(componentName.flattenToShortString());
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putStringForUser(this.mContext.getContentResolver(), str, sb.toString(), i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void updateServicesLocked(UserState userState) {
        int i;
        int i2;
        Map<ComponentName, AccessibilityServiceConnection> map = userState.mComponentNameToServiceMap;
        boolean zIsUserUnlockingOrUnlocked = ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).isUserUnlockingOrUnlocked(userState.mUserId);
        int size = userState.mInstalledServices.size();
        int i3 = 0;
        while (i3 < size) {
            AccessibilityServiceInfo accessibilityServiceInfo = userState.mInstalledServices.get(i3);
            ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(accessibilityServiceInfo.getId());
            AccessibilityServiceConnection accessibilityServiceConnection = map.get(componentNameUnflattenFromString);
            if (!zIsUserUnlockingOrUnlocked && !accessibilityServiceInfo.isDirectBootAware()) {
                Slog.d(LOG_TAG, "Ignoring non-encryption-aware service " + componentNameUnflattenFromString);
            } else {
                if (!userState.mBindingServices.contains(componentNameUnflattenFromString)) {
                    if (userState.mEnabledServices.contains(componentNameUnflattenFromString) && !this.mUiAutomationManager.suppressingAccessibilityServicesLocked()) {
                        if (accessibilityServiceConnection == null) {
                            Context context = this.mContext;
                            int i4 = sIdCounter;
                            sIdCounter = i4 + 1;
                            i = i3;
                            i2 = size;
                            accessibilityServiceConnection = new AccessibilityServiceConnection(userState, context, componentNameUnflattenFromString, accessibilityServiceInfo, i4, this.mMainHandler, this.mLock, this.mSecurityPolicy, this, this.mWindowManagerService, this.mGlobalActionPerformer);
                        } else {
                            i = i3;
                            i2 = size;
                            if (userState.mBoundServices.contains(accessibilityServiceConnection)) {
                            }
                        }
                        accessibilityServiceConnection.bindLocked();
                    } else {
                        i = i3;
                        i2 = size;
                        if (accessibilityServiceConnection != null) {
                            accessibilityServiceConnection.unbindLocked();
                        }
                    }
                }
                i3 = i + 1;
                size = i2;
            }
            i = i3;
            i2 = size;
            i3 = i + 1;
            size = i2;
        }
        int size2 = userState.mBoundServices.size();
        this.mTempIntArray.clear();
        for (int i5 = 0; i5 < size2; i5++) {
            ResolveInfo resolveInfo = userState.mBoundServices.get(i5).mAccessibilityServiceInfo.getResolveInfo();
            if (resolveInfo != null) {
                this.mTempIntArray.add(resolveInfo.serviceInfo.applicationInfo.uid);
            }
        }
        AudioManagerInternal audioManagerInternal = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
        if (audioManagerInternal != null) {
            audioManagerInternal.setAccessibilityServiceUids(this.mTempIntArray);
        }
        updateAccessibilityEnabledSetting(userState);
    }

    private void scheduleUpdateClientsIfNeededLocked(UserState userState) {
        int clientState = userState.getClientState();
        if (userState.mLastSentClientState != clientState) {
            if (this.mGlobalClients.getRegisteredCallbackCount() > 0 || userState.mUserClients.getRegisteredCallbackCount() > 0) {
                userState.mLastSentClientState = clientState;
                this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() {
                    public final void accept(Object obj, Object obj2, Object obj3) {
                        ((AccessibilityManagerService) obj).sendStateToAllClients(((Integer) obj2).intValue(), ((Integer) obj3).intValue());
                    }
                }, this, Integer.valueOf(clientState), Integer.valueOf(userState.mUserId)));
            }
        }
    }

    private void sendStateToAllClients(int i, int i2) {
        sendStateToClients(i, this.mGlobalClients);
        sendStateToClients(i, i2);
    }

    private void sendStateToClients(int i, int i2) {
        sendStateToClients(i, getUserState(i2).mUserClients);
    }

    private void sendStateToClients(final int i, RemoteCallbackList<IAccessibilityManagerClient> remoteCallbackList) {
        remoteCallbackList.broadcast(FunctionalUtils.ignoreRemoteException(new FunctionalUtils.RemoteExceptionIgnoringConsumer() {
            public final void acceptOrThrow(Object obj) {
                ((IAccessibilityManagerClient) obj).setState(i);
            }
        }));
    }

    private void scheduleNotifyClientsOfServicesStateChange(UserState userState) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).sendServicesStateChanged((RemoteCallbackList) obj2);
            }
        }, this, userState.mUserClients));
    }

    private void sendServicesStateChanged(RemoteCallbackList<IAccessibilityManagerClient> remoteCallbackList) {
        notifyClientsOfServicesStateChange(this.mGlobalClients);
        notifyClientsOfServicesStateChange(remoteCallbackList);
    }

    private void notifyClientsOfServicesStateChange(RemoteCallbackList<IAccessibilityManagerClient> remoteCallbackList) {
        remoteCallbackList.broadcast(FunctionalUtils.ignoreRemoteException(new FunctionalUtils.RemoteExceptionIgnoringConsumer() {
            public final void acceptOrThrow(Object obj) {
                ((IAccessibilityManagerClient) obj).notifyServicesStateChanged();
            }
        }));
    }

    private void scheduleUpdateInputFilter(UserState userState) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).updateInputFilter((AccessibilityManagerService.UserState) obj2);
            }
        }, this, userState));
    }

    private void scheduleUpdateFingerprintGestureHandling(UserState userState) {
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                ((AccessibilityManagerService) obj).updateFingerprintGestureHandling((AccessibilityManagerService.UserState) obj2);
            }
        }, this, userState));
    }

    private void updateInputFilter(UserState userState) {
        boolean z;
        IInputFilter iInputFilter;
        if (this.mUiAutomationManager.suppressingAccessibilityServicesLocked()) {
            return;
        }
        synchronized (this.mLock) {
            z = true;
            int i = userState.mIsDisplayMagnificationEnabled ? 1 : 0;
            if (userState.mIsNavBarMagnificationEnabled) {
                i |= 64;
            }
            if (userHasMagnificationServicesLocked(userState)) {
                i |= 32;
            }
            if (userState.isHandlingAccessibilityEvents() && userState.mIsTouchExplorationEnabled) {
                i |= 2;
            }
            if (userState.mIsFilterKeyEventsEnabled) {
                i |= 4;
            }
            if (userState.mIsAutoclickEnabled) {
                i |= 8;
            }
            if (userState.mIsPerformGesturesEnabled) {
                i |= 16;
            }
            iInputFilter = null;
            if (i != 0) {
                if (this.mHasInputFilter) {
                    z = false;
                } else {
                    this.mHasInputFilter = true;
                    if (this.mInputFilter == null) {
                        this.mInputFilter = new AccessibilityInputFilter(this.mContext, this);
                    }
                    iInputFilter = this.mInputFilter;
                }
                this.mInputFilter.setUserAndEnabledFeatures(userState.mUserId, i);
            } else if (this.mHasInputFilter) {
                this.mHasInputFilter = false;
                this.mInputFilter.setUserAndEnabledFeatures(userState.mUserId, 0);
            } else {
                z = false;
            }
        }
        if (z) {
            this.mWindowManagerService.setInputFilter(iInputFilter);
        }
    }

    private void showEnableTouchExplorationDialog(final AccessibilityServiceConnection accessibilityServiceConnection) {
        synchronized (this.mLock) {
            String string = accessibilityServiceConnection.getServiceInfo().getResolveInfo().loadLabel(this.mContext.getPackageManager()).toString();
            final UserState currentUserStateLocked = getCurrentUserStateLocked();
            if (currentUserStateLocked.mIsTouchExplorationEnabled) {
                return;
            }
            if (this.mEnableTouchExplorationDialog == null || !this.mEnableTouchExplorationDialog.isShowing()) {
                this.mEnableTouchExplorationDialog = new AlertDialog.Builder(this.mContext).setIconAttribute(R.attr.alertDialogIcon).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        currentUserStateLocked.mTouchExplorationGrantedServices.add(accessibilityServiceConnection.mComponentName);
                        AccessibilityManagerService.this.persistComponentNamesToSettingLocked("touch_exploration_granted_accessibility_services", currentUserStateLocked.mTouchExplorationGrantedServices, currentUserStateLocked.mUserId);
                        currentUserStateLocked.mIsTouchExplorationEnabled = true;
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            Settings.Secure.putIntForUser(AccessibilityManagerService.this.mContext.getContentResolver(), "touch_exploration_enabled", 1, currentUserStateLocked.mUserId);
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            AccessibilityManagerService.this.onUserStateChangedLocked(currentUserStateLocked);
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            throw th;
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).setTitle(R.string.capability_title_canCaptureFingerprintGestures).setMessage(this.mContext.getString(R.string.capability_desc_canTakeScreenshot, string)).create();
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
        updateAccessibilityShortcutLocked(userState);
        updateWindowsForAccessibilityCallbackLocked(userState);
        updateAccessibilityFocusBehaviorLocked(userState);
        updateFilterKeyEventsLocked(userState);
        updateTouchExplorationLocked(userState);
        updatePerformGesturesLocked(userState);
        updateDisplayDaltonizerLocked(userState);
        updateDisplayInversionLocked(userState);
        updateMagnificationLocked(userState);
        updateSoftKeyboardShowModeLocked(userState);
        scheduleUpdateFingerprintGestureHandling(userState);
        scheduleUpdateInputFilter(userState);
        scheduleUpdateClientsIfNeededLocked(userState);
        updateRelevantEventsLocked(userState);
        updateAccessibilityButtonTargetsLocked(userState);
    }

    private void updateAccessibilityFocusBehaviorLocked(UserState userState) {
        ArrayList<AccessibilityServiceConnection> arrayList = userState.mBoundServices;
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            if (arrayList.get(i).canRetrieveInteractiveWindowsLocked()) {
                userState.mAccessibilityFocusOnlyInActiveWindow = false;
                return;
            }
        }
        userState.mAccessibilityFocusOnlyInActiveWindow = true;
    }

    private void updateWindowsForAccessibilityCallbackLocked(UserState userState) {
        boolean zCanRetrieveInteractiveWindowsLocked = this.mUiAutomationManager.canRetrieveInteractiveWindowsLocked();
        ArrayList<AccessibilityServiceConnection> arrayList = userState.mBoundServices;
        int size = arrayList.size();
        for (int i = 0; !zCanRetrieveInteractiveWindowsLocked && i < size; i++) {
            if (arrayList.get(i).canRetrieveInteractiveWindowsLocked()) {
                zCanRetrieveInteractiveWindowsLocked = true;
            }
        }
        if (zCanRetrieveInteractiveWindowsLocked) {
            if (this.mWindowsForAccessibilityCallback == null) {
                this.mWindowsForAccessibilityCallback = new WindowsForAccessibilityCallback();
                this.mWindowManagerService.setWindowsForAccessibilityCallback(this.mWindowsForAccessibilityCallback);
                return;
            }
            return;
        }
        if (this.mWindowsForAccessibilityCallback != null) {
            this.mWindowsForAccessibilityCallback = null;
            this.mWindowManagerService.setWindowsForAccessibilityCallback(null);
            this.mSecurityPolicy.clearWindowsLocked();
        }
    }

    private void updateLegacyCapabilitiesLocked(UserState userState) {
        int size = userState.mInstalledServices.size();
        for (int i = 0; i < size; i++) {
            AccessibilityServiceInfo accessibilityServiceInfo = userState.mInstalledServices.get(i);
            ResolveInfo resolveInfo = accessibilityServiceInfo.getResolveInfo();
            if ((accessibilityServiceInfo.getCapabilities() & 2) == 0 && resolveInfo.serviceInfo.applicationInfo.targetSdkVersion <= 17) {
                if (userState.mTouchExplorationGrantedServices.contains(new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name))) {
                    accessibilityServiceInfo.setCapabilities(accessibilityServiceInfo.getCapabilities() | 2);
                }
            }
        }
    }

    private void updatePerformGesturesLocked(UserState userState) {
        int size = userState.mBoundServices.size();
        for (int i = 0; i < size; i++) {
            if ((userState.mBoundServices.get(i).getCapabilities() & 32) != 0) {
                userState.mIsPerformGesturesEnabled = true;
                return;
            }
        }
        userState.mIsPerformGesturesEnabled = false;
    }

    private void updateFilterKeyEventsLocked(UserState userState) {
        int size = userState.mBoundServices.size();
        for (int i = 0; i < size; i++) {
            AccessibilityServiceConnection accessibilityServiceConnection = userState.mBoundServices.get(i);
            if (accessibilityServiceConnection.mRequestFilterKeyEvents && (accessibilityServiceConnection.getCapabilities() & 8) != 0) {
                userState.mIsFilterKeyEventsEnabled = true;
                return;
            }
        }
        userState.mIsFilterKeyEventsEnabled = false;
    }

    private boolean readConfigurationForUserStateLocked(UserState userState) {
        return readAccessibilityButtonSettingsLocked(userState) | readInstalledAccessibilityServiceLocked(userState) | readEnabledAccessibilityServicesLocked(userState) | readTouchExplorationGrantedAccessibilityServicesLocked(userState) | readTouchExplorationEnabledSettingLocked(userState) | readHighTextContrastEnabledSettingLocked(userState) | readMagnificationEnabledSettingsLocked(userState) | readAutoclickEnabledSettingLocked(userState) | readAccessibilityShortcutSettingLocked(userState);
    }

    private void updateAccessibilityEnabledSetting(UserState userState) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "accessibility_enabled", (this.mUiAutomationManager.isUiAutomationRunningLocked() || userState.isHandlingAccessibilityEvents()) ? 1 : 0, userState.mUserId);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean readTouchExplorationEnabledSettingLocked(UserState userState) {
        boolean z = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "touch_exploration_enabled", 0, userState.mUserId) == 1;
        if (z == userState.mIsTouchExplorationEnabled) {
            return false;
        }
        userState.mIsTouchExplorationEnabled = z;
        return true;
    }

    private boolean readMagnificationEnabledSettingsLocked(UserState userState) {
        boolean z = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_magnification_enabled", 0, userState.mUserId) == 1;
        boolean z2 = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_magnification_navbar_enabled", 0, userState.mUserId) == 1;
        if (z == userState.mIsDisplayMagnificationEnabled && z2 == userState.mIsNavBarMagnificationEnabled) {
            return false;
        }
        userState.mIsDisplayMagnificationEnabled = z;
        userState.mIsNavBarMagnificationEnabled = z2;
        return true;
    }

    private boolean readAutoclickEnabledSettingLocked(UserState userState) {
        boolean z = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_autoclick_enabled", 0, userState.mUserId) == 1;
        if (z == userState.mIsAutoclickEnabled) {
            return false;
        }
        userState.mIsAutoclickEnabled = z;
        return true;
    }

    private boolean readHighTextContrastEnabledSettingLocked(UserState userState) {
        boolean z = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "high_text_contrast_enabled", 0, userState.mUserId) == 1;
        if (z == userState.mIsTextHighContrastEnabled) {
            return false;
        }
        userState.mIsTextHighContrastEnabled = z;
        return true;
    }

    private boolean readSoftKeyboardShowModeChangedLocked(UserState userState) {
        int intForUser = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_soft_keyboard_mode", 0, userState.mUserId);
        if (intForUser == userState.mSoftKeyboardShowMode) {
            return false;
        }
        userState.mSoftKeyboardShowMode = intForUser;
        return true;
    }

    private void updateTouchExplorationLocked(UserState userState) {
        boolean zIsTouchExplorationEnabledLocked = this.mUiAutomationManager.isTouchExplorationEnabledLocked();
        int size = userState.mBoundServices.size();
        int i = 0;
        while (true) {
            if (i >= size) {
                break;
            }
            if (!canRequestAndRequestsTouchExplorationLocked(userState.mBoundServices.get(i), userState)) {
                i++;
            } else {
                zIsTouchExplorationEnabledLocked = true;
                break;
            }
        }
        if (zIsTouchExplorationEnabledLocked != userState.mIsTouchExplorationEnabled) {
            userState.mIsTouchExplorationEnabled = zIsTouchExplorationEnabledLocked;
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "touch_exploration_enabled", zIsTouchExplorationEnabledLocked ? 1 : 0, userState.mUserId);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private boolean readAccessibilityShortcutSettingLocked(UserState userState) {
        String targetServiceComponentNameString = AccessibilityShortcutController.getTargetServiceComponentNameString(this.mContext, userState.mUserId);
        if (targetServiceComponentNameString == null || targetServiceComponentNameString.isEmpty()) {
            if (userState.mServiceToEnableWithShortcut == null) {
                return false;
            }
            userState.mServiceToEnableWithShortcut = null;
            return true;
        }
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(targetServiceComponentNameString);
        if (componentNameUnflattenFromString != null && componentNameUnflattenFromString.equals(userState.mServiceToEnableWithShortcut)) {
            return false;
        }
        userState.mServiceToEnableWithShortcut = componentNameUnflattenFromString;
        return true;
    }

    private boolean readAccessibilityButtonSettingsLocked(UserState userState) {
        String stringForUser = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "accessibility_button_target_component", userState.mUserId);
        if (TextUtils.isEmpty(stringForUser)) {
            if (userState.mServiceAssignedToAccessibilityButton == null && !userState.mIsNavBarMagnificationAssignedToAccessibilityButton) {
                return false;
            }
            userState.mServiceAssignedToAccessibilityButton = null;
            userState.mIsNavBarMagnificationAssignedToAccessibilityButton = false;
            return true;
        }
        if (stringForUser.equals(MagnificationController.class.getName())) {
            if (userState.mIsNavBarMagnificationAssignedToAccessibilityButton) {
                return false;
            }
            userState.mServiceAssignedToAccessibilityButton = null;
            userState.mIsNavBarMagnificationAssignedToAccessibilityButton = true;
            return true;
        }
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(stringForUser);
        if (Objects.equals(componentNameUnflattenFromString, userState.mServiceAssignedToAccessibilityButton)) {
            return false;
        }
        userState.mServiceAssignedToAccessibilityButton = componentNameUnflattenFromString;
        userState.mIsNavBarMagnificationAssignedToAccessibilityButton = false;
        return true;
    }

    private void updateAccessibilityShortcutLocked(UserState userState) {
        if (userState.mServiceToEnableWithShortcut == null) {
            return;
        }
        boolean zContainsKey = AccessibilityShortcutController.getFrameworkShortcutFeaturesMap().containsKey(userState.mServiceToEnableWithShortcut);
        for (int i = 0; !zContainsKey && i < userState.mInstalledServices.size(); i++) {
            if (userState.mInstalledServices.get(i).getComponentName().equals(userState.mServiceToEnableWithShortcut)) {
                zContainsKey = true;
            }
        }
        if (!zContainsKey) {
            userState.mServiceToEnableWithShortcut = null;
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "accessibility_shortcut_target_service", null, userState.mUserId);
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "accessibility_shortcut_enabled", 0, userState.mUserId);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private boolean canRequestAndRequestsTouchExplorationLocked(AccessibilityServiceConnection accessibilityServiceConnection, UserState userState) {
        if (!accessibilityServiceConnection.canReceiveEventsLocked() || !accessibilityServiceConnection.mRequestTouchExplorationMode) {
            return false;
        }
        if (accessibilityServiceConnection.getServiceInfo().getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion <= 17) {
            if (userState.mTouchExplorationGrantedServices.contains(accessibilityServiceConnection.mComponentName)) {
                return true;
            }
            if (this.mEnableTouchExplorationDialog == null || !this.mEnableTouchExplorationDialog.isShowing()) {
                this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new BiConsumer() {
                    @Override
                    public final void accept(Object obj, Object obj2) {
                        ((AccessibilityManagerService) obj).showEnableTouchExplorationDialog((AccessibilityServiceConnection) obj2);
                    }
                }, this, accessibilityServiceConnection));
            }
        } else if ((accessibilityServiceConnection.getCapabilities() & 2) != 0) {
            return true;
        }
        return false;
    }

    private void updateDisplayDaltonizerLocked(UserState userState) {
        DisplayAdjustmentUtils.applyDaltonizerSetting(this.mContext, userState.mUserId);
    }

    private void updateDisplayInversionLocked(UserState userState) {
        DisplayAdjustmentUtils.applyInversionSetting(this.mContext, userState.mUserId);
    }

    private void updateMagnificationLocked(UserState userState) {
        if (userState.mUserId != this.mCurrentUserId) {
            return;
        }
        if (!this.mUiAutomationManager.suppressingAccessibilityServicesLocked() && (userState.mIsDisplayMagnificationEnabled || userState.mIsNavBarMagnificationEnabled || userHasListeningMagnificationServicesLocked(userState))) {
            getMagnificationController();
            this.mMagnificationController.register();
        } else if (this.mMagnificationController != null) {
            this.mMagnificationController.unregister();
        }
    }

    private boolean userHasMagnificationServicesLocked(UserState userState) {
        ArrayList<AccessibilityServiceConnection> arrayList = userState.mBoundServices;
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            if (this.mSecurityPolicy.canControlMagnification(arrayList.get(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean userHasListeningMagnificationServicesLocked(UserState userState) {
        ArrayList<AccessibilityServiceConnection> arrayList = userState.mBoundServices;
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            AccessibilityServiceConnection accessibilityServiceConnection = arrayList.get(i);
            if (this.mSecurityPolicy.canControlMagnification(accessibilityServiceConnection) && accessibilityServiceConnection.isMagnificationCallbackEnabled()) {
                return true;
            }
        }
        return false;
    }

    private void updateSoftKeyboardShowModeLocked(UserState userState) {
        if (userState.mUserId == this.mCurrentUserId && userState.mSoftKeyboardShowMode != 0 && !userState.mEnabledServices.contains(userState.mServiceChangingSoftKeyboardMode)) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "accessibility_soft_keyboard_mode", 0, userState.mUserId);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                userState.mSoftKeyboardShowMode = 0;
                userState.mServiceChangingSoftKeyboardMode = null;
                notifySoftKeyboardShowModeChangedLocked(userState.mSoftKeyboardShowMode);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }
    }

    private void updateFingerprintGestureHandling(UserState userState) {
        ArrayList<AccessibilityServiceConnection> arrayList;
        synchronized (this.mLock) {
            arrayList = userState.mBoundServices;
            if (this.mFingerprintGestureDispatcher == null && this.mPackageManager.hasSystemFeature("android.hardware.fingerprint")) {
                int size = arrayList.size();
                int i = 0;
                while (true) {
                    if (i >= size) {
                        break;
                    }
                    if (arrayList.get(i).isCapturingFingerprintGestures()) {
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            IFingerprintService iFingerprintServiceAsInterface = IFingerprintService.Stub.asInterface(ServiceManager.getService("fingerprint"));
                            if (iFingerprintServiceAsInterface != null) {
                                this.mFingerprintGestureDispatcher = new FingerprintGestureDispatcher(iFingerprintServiceAsInterface, this.mContext.getResources(), this.mLock);
                                break;
                            }
                        } finally {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        }
                    }
                    i++;
                }
            }
        }
        if (this.mFingerprintGestureDispatcher != null) {
            this.mFingerprintGestureDispatcher.updateClientList(arrayList);
        }
    }

    private void updateAccessibilityButtonTargetsLocked(UserState userState) {
        for (int size = userState.mBoundServices.size() - 1; size >= 0; size--) {
            AccessibilityServiceConnection accessibilityServiceConnection = userState.mBoundServices.get(size);
            if (accessibilityServiceConnection.mRequestAccessibilityButton) {
                accessibilityServiceConnection.notifyAccessibilityButtonAvailabilityChangedLocked(accessibilityServiceConnection.isAccessibilityButtonAvailableLocked(userState));
            }
        }
    }

    @Override
    @GuardedBy("mLock")
    public MagnificationSpec getCompatibleMagnificationSpecLocked(int i) {
        IBinder iBinder = this.mGlobalWindowTokens.get(i);
        if (iBinder == null) {
            iBinder = getCurrentUserStateLocked().mWindowTokens.get(i);
        }
        if (iBinder != null) {
            return this.mWindowManagerService.getCompatibleMagnificationSpecForWindow(iBinder);
        }
        return null;
    }

    @Override
    public KeyEventDispatcher getKeyEventDispatcher() {
        if (this.mKeyEventDispatcher == null) {
            this.mKeyEventDispatcher = new KeyEventDispatcher(this.mMainHandler, 8, this.mLock, this.mPowerManager);
        }
        return this.mKeyEventDispatcher;
    }

    @Override
    public PendingIntent getPendingIntentActivity(Context context, int i, Intent intent, int i2) {
        return PendingIntent.getActivity(context, i, intent, i2);
    }

    public void performAccessibilityShortcut() {
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000 && this.mContext.checkCallingPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException("performAccessibilityShortcut requires the WRITE_SECURE_SETTINGS permission");
        }
        Map frameworkShortcutFeaturesMap = AccessibilityShortcutController.getFrameworkShortcutFeaturesMap();
        synchronized (this.mLock) {
            UserState userStateLocked = getUserStateLocked(this.mCurrentUserId);
            ComponentName componentName = userStateLocked.mServiceToEnableWithShortcut;
            if (componentName == null) {
                return;
            }
            if (frameworkShortcutFeaturesMap.containsKey(componentName)) {
                AccessibilityShortcutController.ToggleableFrameworkFeatureInfo toggleableFrameworkFeatureInfo = (AccessibilityShortcutController.ToggleableFrameworkFeatureInfo) frameworkShortcutFeaturesMap.get(componentName);
                SettingsStringUtil.SettingStringHelper settingStringHelper = new SettingsStringUtil.SettingStringHelper(this.mContext.getContentResolver(), toggleableFrameworkFeatureInfo.getSettingKey(), this.mCurrentUserId);
                if (!TextUtils.equals(toggleableFrameworkFeatureInfo.getSettingOnValue(), settingStringHelper.read())) {
                    settingStringHelper.write(toggleableFrameworkFeatureInfo.getSettingOnValue());
                } else {
                    settingStringHelper.write(toggleableFrameworkFeatureInfo.getSettingOffValue());
                }
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (userStateLocked.mComponentNameToServiceMap.get(componentName) == null) {
                    enableAccessibilityServiceLocked(componentName, this.mCurrentUserId);
                } else {
                    disableAccessibilityServiceLocked(componentName, this.mCurrentUserId);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private void enableAccessibilityServiceLocked(ComponentName componentName, int i) {
        SettingsStringUtil.SettingStringHelper settingStringHelper = new SettingsStringUtil.SettingStringHelper(this.mContext.getContentResolver(), "enabled_accessibility_services", i);
        settingStringHelper.write(SettingsStringUtil.ComponentNameSet.add(settingStringHelper.read(), componentName));
        UserState userStateLocked = getUserStateLocked(i);
        if (userStateLocked.mEnabledServices.add(componentName)) {
            onUserStateChangedLocked(userStateLocked);
        }
    }

    private void disableAccessibilityServiceLocked(ComponentName componentName, int i) {
        SettingsStringUtil.SettingStringHelper settingStringHelper = new SettingsStringUtil.SettingStringHelper(this.mContext.getContentResolver(), "enabled_accessibility_services", i);
        settingStringHelper.write(SettingsStringUtil.ComponentNameSet.remove(settingStringHelper.read(), componentName));
        UserState userStateLocked = getUserStateLocked(i);
        if (userStateLocked.mEnabledServices.remove(componentName)) {
            onUserStateChangedLocked(userStateLocked);
        }
    }

    private void sendAccessibilityEventLocked(AccessibilityEvent accessibilityEvent, int i) {
        accessibilityEvent.setEventTime(SystemClock.uptimeMillis());
        this.mMainHandler.sendMessage(PooledLambda.obtainMessage(new TriConsumer() {
            public final void accept(Object obj, Object obj2, Object obj3) {
                ((AccessibilityManagerService) obj).sendAccessibilityEvent((AccessibilityEvent) obj2, ((Integer) obj3).intValue());
            }
        }, this, accessibilityEvent, Integer.valueOf(i)));
    }

    public boolean sendFingerprintGesture(int i) {
        synchronized (this.mLock) {
            if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
                throw new SecurityException("Only SYSTEM can call sendFingerprintGesture");
            }
        }
        if (this.mFingerprintGestureDispatcher == null) {
            return false;
        }
        return this.mFingerprintGestureDispatcher.onFingerprintGesture(i);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, LOG_TAG, printWriter)) {
            synchronized (this.mLock) {
                printWriter.println("ACCESSIBILITY MANAGER (dumpsys accessibility)");
                printWriter.println();
                int size = this.mUserStates.size();
                for (int i = 0; i < size; i++) {
                    UserState userStateValueAt = this.mUserStates.valueAt(i);
                    printWriter.append((CharSequence) ("User state[attributes:{id=" + userStateValueAt.mUserId));
                    StringBuilder sb = new StringBuilder();
                    sb.append(", currentUser=");
                    sb.append(userStateValueAt.mUserId == this.mCurrentUserId);
                    printWriter.append((CharSequence) sb.toString());
                    printWriter.append((CharSequence) (", touchExplorationEnabled=" + userStateValueAt.mIsTouchExplorationEnabled));
                    printWriter.append((CharSequence) (", displayMagnificationEnabled=" + userStateValueAt.mIsDisplayMagnificationEnabled));
                    printWriter.append((CharSequence) (", navBarMagnificationEnabled=" + userStateValueAt.mIsNavBarMagnificationEnabled));
                    printWriter.append((CharSequence) (", autoclickEnabled=" + userStateValueAt.mIsAutoclickEnabled));
                    if (this.mUiAutomationManager.isUiAutomationRunningLocked()) {
                        printWriter.append(", ");
                        this.mUiAutomationManager.dumpUiAutomationService(fileDescriptor, printWriter, strArr);
                        printWriter.println();
                    }
                    printWriter.append("}");
                    printWriter.println();
                    printWriter.append("           services:{");
                    int size2 = userStateValueAt.mBoundServices.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        if (i2 > 0) {
                            printWriter.append(", ");
                            printWriter.println();
                            printWriter.append("                     ");
                        }
                        userStateValueAt.mBoundServices.get(i2).dump(fileDescriptor, printWriter, strArr);
                    }
                    printWriter.println("}]");
                    printWriter.println();
                }
                if (this.mSecurityPolicy.mWindows != null) {
                    int size3 = this.mSecurityPolicy.mWindows.size();
                    for (int i3 = 0; i3 < size3; i3++) {
                        if (i3 > 0) {
                            printWriter.append(',');
                            printWriter.println();
                        }
                        printWriter.append("Window[");
                        printWriter.append((CharSequence) this.mSecurityPolicy.mWindows.get(i3).toString());
                        printWriter.append(']');
                    }
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

        RemoteAccessibilityConnection(int i, IAccessibilityInteractionConnection iAccessibilityInteractionConnection, String str, int i2, int i3) {
            this.mWindowId = i;
            this.mPackageName = str;
            this.mUid = i2;
            this.mUserId = i3;
            this.mConnection = iAccessibilityInteractionConnection;
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

    final class MainHandler extends Handler {
        public static final int MSG_SEND_KEY_EVENT_TO_INPUT_FILTER = 8;

        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 8) {
                KeyEvent keyEvent = (KeyEvent) message.obj;
                int i = message.arg1;
                synchronized (AccessibilityManagerService.this.mLock) {
                    if (AccessibilityManagerService.this.mHasInputFilter && AccessibilityManagerService.this.mInputFilter != null) {
                        AccessibilityManagerService.this.mInputFilter.sendInputEvent(keyEvent, i);
                    }
                }
                keyEvent.recycle();
            }
        }
    }

    void clearAccessibilityFocus(IntSupplier intSupplier) {
        clearAccessibilityFocus(intSupplier.getAsInt());
    }

    void clearAccessibilityFocus(int i) {
        getInteractionBridge().clearAccessibilityFocusNotLocked(i);
    }

    private IBinder findWindowTokenLocked(int i) {
        IBinder iBinder = this.mGlobalWindowTokens.get(i);
        if (iBinder != null) {
            return iBinder;
        }
        return getCurrentUserStateLocked().mWindowTokens.get(i);
    }

    private int findWindowIdLocked(IBinder iBinder) {
        int iIndexOfValue = this.mGlobalWindowTokens.indexOfValue(iBinder);
        if (iIndexOfValue >= 0) {
            return this.mGlobalWindowTokens.keyAt(iIndexOfValue);
        }
        UserState currentUserStateLocked = getCurrentUserStateLocked();
        int iIndexOfValue2 = currentUserStateLocked.mWindowTokens.indexOfValue(iBinder);
        if (iIndexOfValue2 >= 0) {
            return currentUserStateLocked.mWindowTokens.keyAt(iIndexOfValue2);
        }
        return -1;
    }

    @Override
    public void ensureWindowsAvailableTimed() {
        synchronized (this.mLock) {
            if (this.mSecurityPolicy.mWindows != null) {
                return;
            }
            if (this.mWindowsForAccessibilityCallback == null) {
                onUserStateChangedLocked(getCurrentUserStateLocked());
            }
            if (this.mWindowsForAccessibilityCallback == null) {
                return;
            }
            long jUptimeMillis = SystemClock.uptimeMillis();
            while (this.mSecurityPolicy.mWindows == null) {
                long jUptimeMillis2 = 5000 - (SystemClock.uptimeMillis() - jUptimeMillis);
                if (jUptimeMillis2 <= 0) {
                    return;
                } else {
                    try {
                        this.mLock.wait(jUptimeMillis2);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    @Override
    public MagnificationController getMagnificationController() {
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

    @Override
    public boolean performAccessibilityAction(int i, long j, int i2, Bundle bundle, int i3, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i4, long j2) {
        WindowInfo windowInfoFindWindowInfoById;
        synchronized (this.mLock) {
            RemoteAccessibilityConnection connectionLocked = getConnectionLocked(i);
            if (connectionLocked == null) {
                return false;
            }
            boolean z = i2 == 64 || i2 == 128;
            AccessibilityWindowInfo accessibilityWindowInfoFindA11yWindowInfoById = this.mSecurityPolicy.findA11yWindowInfoById(i);
            IBinder iBinder = (z || (windowInfoFindWindowInfoById = this.mSecurityPolicy.findWindowInfoById(i)) == null) ? null : windowInfoFindWindowInfoById.activityToken;
            if (accessibilityWindowInfoFindA11yWindowInfoById != null && accessibilityWindowInfoFindA11yWindowInfoById.isInPictureInPictureMode() && this.mPictureInPictureActionReplacingConnection != null && !z) {
                connectionLocked = this.mPictureInPictureActionReplacingConnection;
            }
            int callingPid = Binder.getCallingPid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 3, 0);
                if (iBinder != null) {
                    ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).setFocusedActivity(iBinder);
                }
                connectionLocked.mConnection.performAccessibilityAction(j, i2, bundle, i3, iAccessibilityInteractionConnectionCallback, i4, callingPid, j2);
                return true;
            } catch (RemoteException e) {
                return false;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    @Override
    public RemoteAccessibilityConnection getConnectionLocked(int i) {
        RemoteAccessibilityConnection remoteAccessibilityConnection = this.mGlobalInteractionConnections.get(i);
        if (remoteAccessibilityConnection == null) {
            remoteAccessibilityConnection = getCurrentUserStateLocked().mInteractionConnections.get(i);
        }
        if (remoteAccessibilityConnection != null && remoteAccessibilityConnection.mConnection != null) {
            return remoteAccessibilityConnection;
        }
        return null;
    }

    @Override
    public IAccessibilityInteractionConnectionCallback replaceCallbackIfNeeded(IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i, int i2, int i3, long j) {
        AccessibilityWindowInfo accessibilityWindowInfoFindA11yWindowInfoById = this.mSecurityPolicy.findA11yWindowInfoById(i);
        if (accessibilityWindowInfoFindA11yWindowInfoById == null || !accessibilityWindowInfoFindA11yWindowInfoById.isInPictureInPictureMode() || this.mPictureInPictureActionReplacingConnection == null) {
            return iAccessibilityInteractionConnectionCallback;
        }
        return new ActionReplacingCallback(iAccessibilityInteractionConnectionCallback, this.mPictureInPictureActionReplacingConnection.mConnection, i2, i3, j);
    }

    @Override
    public void onClientChange(boolean z) {
        UserState userStateLocked = getUserStateLocked(this.mCurrentUserId);
        onUserStateChangedLocked(userStateLocked);
        if (z) {
            scheduleNotifyClientsOfServicesStateChange(userStateLocked);
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

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new AccessibilityShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    final class WindowsForAccessibilityCallback implements WindowManagerInternal.WindowsForAccessibilityCallback {
        WindowsForAccessibilityCallback() {
        }

        @Override
        public void onWindowsForAccessibilityChanged(List<WindowInfo> list) {
            synchronized (AccessibilityManagerService.this.mLock) {
                AccessibilityManagerService.this.mSecurityPolicy.updateWindowsLocked(list);
                AccessibilityManagerService.this.mLock.notifyAll();
            }
        }

        private AccessibilityWindowInfo populateReportedWindowLocked(WindowInfo windowInfo) {
            int iFindWindowIdLocked = AccessibilityManagerService.this.findWindowIdLocked(windowInfo.token);
            if (iFindWindowIdLocked < 0) {
                return null;
            }
            AccessibilityWindowInfo accessibilityWindowInfoObtain = AccessibilityWindowInfo.obtain();
            accessibilityWindowInfoObtain.setId(iFindWindowIdLocked);
            accessibilityWindowInfoObtain.setType(getTypeForWindowManagerWindowType(windowInfo.type));
            accessibilityWindowInfoObtain.setLayer(windowInfo.layer);
            accessibilityWindowInfoObtain.setFocused(windowInfo.focused);
            accessibilityWindowInfoObtain.setBoundsInScreen(windowInfo.boundsInScreen);
            accessibilityWindowInfoObtain.setTitle(windowInfo.title);
            accessibilityWindowInfoObtain.setAnchorId(windowInfo.accessibilityIdOfAnchor);
            accessibilityWindowInfoObtain.setPictureInPicture(windowInfo.inPictureInPicture);
            int iFindWindowIdLocked2 = AccessibilityManagerService.this.findWindowIdLocked(windowInfo.parentToken);
            if (iFindWindowIdLocked2 >= 0) {
                accessibilityWindowInfoObtain.setParentId(iFindWindowIdLocked2);
            }
            if (windowInfo.childTokens != null) {
                int size = windowInfo.childTokens.size();
                for (int i = 0; i < size; i++) {
                    int iFindWindowIdLocked3 = AccessibilityManagerService.this.findWindowIdLocked((IBinder) windowInfo.childTokens.get(i));
                    if (iFindWindowIdLocked3 >= 0) {
                        accessibilityWindowInfoObtain.addChild(iFindWindowIdLocked3);
                    }
                }
            }
            return accessibilityWindowInfoObtain;
        }

        private int getTypeForWindowManagerWindowType(int i) {
            switch (i) {
                case 1:
                case 2:
                case 3:
                case 4:
                    return 1;
                default:
                    switch (i) {
                        case 1000:
                        case NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE:
                        case 1002:
                        case 1003:
                            return 1;
                        default:
                            switch (i) {
                                case PowerHalManager.ROTATE_BOOST_TIME:
                                case 2001:
                                case 2003:
                                    return 3;
                                case 2002:
                                    return 1;
                                default:
                                    switch (i) {
                                        case 2005:
                                        case 2007:
                                            return 1;
                                        case 2006:
                                        case 2008:
                                        case 2009:
                                        case 2010:
                                            return 3;
                                        case 2011:
                                        case 2012:
                                            return 2;
                                        default:
                                            switch (i) {
                                                case 2019:
                                                case 2020:
                                                    return 3;
                                                default:
                                                    switch (i) {
                                                        case 1005:
                                                            return 1;
                                                        case 2014:
                                                        case 2017:
                                                        case 2024:
                                                        case 2036:
                                                        case 2038:
                                                            return 3;
                                                        case 2032:
                                                            return 4;
                                                        case 2034:
                                                            return 5;
                                                        default:
                                                            return -1;
                                                    }
                                            }
                                    }
                            }
                    }
            }
        }
    }

    private final class InteractionBridge {
        private final ComponentName COMPONENT_NAME = new ComponentName("com.android.server.accessibility", "InteractionBridge");
        private final AccessibilityInteractionClient mClient;
        private final int mConnectionId;
        private final Display mDefaultDisplay;

        public InteractionBridge() {
            UserState currentUserStateLocked;
            AccessibilityServiceInfo accessibilityServiceInfo = new AccessibilityServiceInfo();
            accessibilityServiceInfo.setCapabilities(1);
            accessibilityServiceInfo.flags |= 64;
            accessibilityServiceInfo.flags |= 2;
            synchronized (AccessibilityManagerService.this.mLock) {
                currentUserStateLocked = AccessibilityManagerService.this.getCurrentUserStateLocked();
            }
            AccessibilityServiceConnection accessibilityServiceConnection = new AccessibilityServiceConnection(currentUserStateLocked, AccessibilityManagerService.this.mContext, this.COMPONENT_NAME, accessibilityServiceInfo, AccessibilityManagerService.access$2508(), AccessibilityManagerService.this.mMainHandler, AccessibilityManagerService.this.mLock, AccessibilityManagerService.this.mSecurityPolicy, AccessibilityManagerService.this, AccessibilityManagerService.this.mWindowManagerService, AccessibilityManagerService.this.mGlobalActionPerformer) {
                @Override
                public boolean supportsFlagForNotImportantViews(AccessibilityServiceInfo accessibilityServiceInfo2) {
                    return true;
                }
            };
            this.mConnectionId = accessibilityServiceConnection.mId;
            this.mClient = AccessibilityInteractionClient.getInstance();
            AccessibilityInteractionClient accessibilityInteractionClient = this.mClient;
            AccessibilityInteractionClient.addConnection(this.mConnectionId, accessibilityServiceConnection);
            this.mDefaultDisplay = ((DisplayManager) AccessibilityManagerService.this.mContext.getSystemService("display")).getDisplay(0);
        }

        public void clearAccessibilityFocusNotLocked(int i) {
            AccessibilityNodeInfo accessibilityFocusNotLocked = getAccessibilityFocusNotLocked(i);
            if (accessibilityFocusNotLocked != null) {
                accessibilityFocusNotLocked.performAction(128);
            }
        }

        public boolean performActionOnAccessibilityFocusedItemNotLocked(AccessibilityNodeInfo.AccessibilityAction accessibilityAction) {
            AccessibilityNodeInfo accessibilityFocusNotLocked = getAccessibilityFocusNotLocked();
            if (accessibilityFocusNotLocked == null || !accessibilityFocusNotLocked.getActionList().contains(accessibilityAction)) {
                return false;
            }
            return accessibilityFocusNotLocked.performAction(accessibilityAction.getId());
        }

        public boolean getAccessibilityFocusClickPointInScreenNotLocked(Point point) {
            AccessibilityNodeInfo accessibilityFocusNotLocked = getAccessibilityFocusNotLocked();
            if (accessibilityFocusNotLocked != null) {
                synchronized (AccessibilityManagerService.this.mLock) {
                    Rect rect = AccessibilityManagerService.this.mTempRect;
                    accessibilityFocusNotLocked.getBoundsInScreen(rect);
                    MagnificationSpec compatibleMagnificationSpecLocked = AccessibilityManagerService.this.getCompatibleMagnificationSpecLocked(accessibilityFocusNotLocked.getWindowId());
                    if (compatibleMagnificationSpecLocked != null && !compatibleMagnificationSpecLocked.isNop()) {
                        rect.offset((int) (-compatibleMagnificationSpecLocked.offsetX), (int) (-compatibleMagnificationSpecLocked.offsetY));
                        rect.scale(1.0f / compatibleMagnificationSpecLocked.scale);
                    }
                    Rect rect2 = AccessibilityManagerService.this.mTempRect1;
                    AccessibilityManagerService.this.getWindowBounds(accessibilityFocusNotLocked.getWindowId(), rect2);
                    if (rect.intersect(rect2)) {
                        Point point2 = AccessibilityManagerService.this.mTempPoint;
                        this.mDefaultDisplay.getRealSize(point2);
                        if (!rect.intersect(0, 0, point2.x, point2.y)) {
                            return false;
                        }
                        point.set(rect.centerX(), rect.centerY());
                        return true;
                    }
                    return false;
                }
            }
            return false;
        }

        private AccessibilityNodeInfo getAccessibilityFocusNotLocked() {
            synchronized (AccessibilityManagerService.this.mLock) {
                int i = AccessibilityManagerService.this.mSecurityPolicy.mAccessibilityFocusedWindowId;
                if (i == -1) {
                    return null;
                }
                return getAccessibilityFocusNotLocked(i);
            }
        }

        private AccessibilityNodeInfo getAccessibilityFocusNotLocked(int i) {
            return this.mClient.findFocus(this.mConnectionId, i, AccessibilityNodeInfo.ROOT_NODE_ID, 2);
        }
    }

    public class SecurityPolicy {
        public static final int INVALID_WINDOW_ID = -1;
        private static final int KEEP_SOURCE_EVENT_TYPES = 4438463;
        private boolean mTouchInteractionInProgress;
        public List<AccessibilityWindowInfo> mWindows;
        public SparseArray<AccessibilityWindowInfo> mA11yWindowInfoById = new SparseArray<>();
        public SparseArray<WindowInfo> mWindowInfoById = new SparseArray<>();
        public int mActiveWindowId = -1;
        public int mFocusedWindowId = -1;
        public int mAccessibilityFocusedWindowId = -1;
        public long mAccessibilityFocusNodeId = 2147483647L;

        public SecurityPolicy() {
        }

        private boolean canDispatchAccessibilityEventLocked(AccessibilityEvent accessibilityEvent) {
            switch (accessibilityEvent.getEventType()) {
                case 32:
                case 64:
                case 128:
                case 256:
                case 512:
                case 1024:
                case 16384:
                case DumpState.DUMP_DOMAIN_PREFERRED:
                case DumpState.DUMP_FROZEN:
                case DumpState.DUMP_DEXOPT:
                case DumpState.DUMP_COMPILER_STATS:
                case DumpState.DUMP_CHANGES:
                case DumpState.DUMP_SERVICE_PERMISSIONS:
                    return true;
                default:
                    return isRetrievalAllowingWindowLocked(accessibilityEvent.getWindowId());
            }
        }

        private boolean isValidPackageForUid(String str, int i) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return i == AccessibilityManagerService.this.mPackageManager.getPackageUidAsUser(str, UserHandle.getUserId(i));
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        String resolveValidReportedPackageLocked(CharSequence charSequence, int i, int i2) {
            if (charSequence == null) {
                return null;
            }
            if (i == 1000) {
                return charSequence.toString();
            }
            String string = charSequence.toString();
            int uid = UserHandle.getUid(i2, i);
            if (!isValidPackageForUid(string, uid)) {
                AppWidgetManagerInternal appWidgetManager = AccessibilityManagerService.this.getAppWidgetManager();
                if (appWidgetManager == null || !ArrayUtils.contains(appWidgetManager.getHostedWidgetPackages(uid), string)) {
                    String[] packagesForUid = AccessibilityManagerService.this.mPackageManager.getPackagesForUid(uid);
                    if (ArrayUtils.isEmpty(packagesForUid)) {
                        return null;
                    }
                    return packagesForUid[0];
                }
                return charSequence.toString();
            }
            return charSequence.toString();
        }

        String[] computeValidReportedPackages(int i, String str, int i2) {
            ArraySet hostedWidgetPackages;
            if (UserHandle.getAppId(i) == 1000) {
                return EmptyArray.STRING;
            }
            String[] strArr = {str};
            AppWidgetManagerInternal appWidgetManager = AccessibilityManagerService.this.getAppWidgetManager();
            if (appWidgetManager != null && (hostedWidgetPackages = appWidgetManager.getHostedWidgetPackages(i2)) != null && !hostedWidgetPackages.isEmpty()) {
                String[] strArr2 = new String[strArr.length + hostedWidgetPackages.size()];
                System.arraycopy(strArr, 0, strArr2, 0, strArr.length);
                int size = hostedWidgetPackages.size();
                for (int i3 = 0; i3 < size; i3++) {
                    strArr2[strArr.length + i3] = (String) hostedWidgetPackages.valueAt(i3);
                }
                return strArr2;
            }
            return strArr;
        }

        private boolean getBindInstantServiceAllowed(int i) {
            AccessibilityManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_BIND_INSTANT_SERVICE", "getBindInstantServiceAllowed");
            UserState userState = (UserState) AccessibilityManagerService.this.mUserStates.get(i);
            return userState != null && userState.mBindInstantServiceAllowed;
        }

        private void setBindInstantServiceAllowed(int i, boolean z) {
            AccessibilityManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_BIND_INSTANT_SERVICE", "setBindInstantServiceAllowed");
            UserState userState = (UserState) AccessibilityManagerService.this.mUserStates.get(i);
            if (userState == null) {
                if (!z) {
                    return;
                }
                userState = AccessibilityManagerService.this.new UserState(i);
                AccessibilityManagerService.this.mUserStates.put(i, userState);
            }
            if (userState.mBindInstantServiceAllowed != z) {
                userState.mBindInstantServiceAllowed = z;
                AccessibilityManagerService.this.onUserStateChangedLocked(userState);
            }
        }

        public void clearWindowsLocked() {
            List<WindowInfo> listEmptyList = Collections.emptyList();
            int i = this.mActiveWindowId;
            updateWindowsLocked(listEmptyList);
            this.mActiveWindowId = i;
            this.mWindows = null;
        }

        public void updateWindowsLocked(List<WindowInfo> list) {
            AccessibilityWindowInfo accessibilityWindowInfoPopulateReportedWindowLocked;
            if (this.mWindows == null) {
                this.mWindows = new ArrayList();
            }
            ArrayList arrayList = new ArrayList(this.mWindows);
            SparseArray<AccessibilityWindowInfo> sparseArrayClone = this.mA11yWindowInfoById.clone();
            this.mWindows.clear();
            this.mA11yWindowInfoById.clear();
            for (int i = 0; i < this.mWindowInfoById.size(); i++) {
                this.mWindowInfoById.valueAt(i).recycle();
            }
            this.mWindowInfoById.clear();
            this.mFocusedWindowId = -1;
            if (!this.mTouchInteractionInProgress) {
                this.mActiveWindowId = -1;
            }
            int size = list.size();
            boolean z = this.mAccessibilityFocusedWindowId != -1;
            if (size > 0) {
                boolean z2 = true;
                for (int i2 = 0; i2 < size; i2++) {
                    WindowInfo windowInfo = list.get(i2);
                    if (AccessibilityManagerService.this.mWindowsForAccessibilityCallback != null) {
                        accessibilityWindowInfoPopulateReportedWindowLocked = AccessibilityManagerService.this.mWindowsForAccessibilityCallback.populateReportedWindowLocked(windowInfo);
                    } else {
                        accessibilityWindowInfoPopulateReportedWindowLocked = null;
                    }
                    if (accessibilityWindowInfoPopulateReportedWindowLocked != null) {
                        accessibilityWindowInfoPopulateReportedWindowLocked.setLayer((size - 1) - accessibilityWindowInfoPopulateReportedWindowLocked.getLayer());
                        int id = accessibilityWindowInfoPopulateReportedWindowLocked.getId();
                        if (accessibilityWindowInfoPopulateReportedWindowLocked.isFocused()) {
                            this.mFocusedWindowId = id;
                            if (!this.mTouchInteractionInProgress) {
                                this.mActiveWindowId = id;
                                accessibilityWindowInfoPopulateReportedWindowLocked.setActive(true);
                            } else if (id == this.mActiveWindowId) {
                                z2 = false;
                            }
                        }
                        this.mWindows.add(accessibilityWindowInfoPopulateReportedWindowLocked);
                        this.mA11yWindowInfoById.put(id, accessibilityWindowInfoPopulateReportedWindowLocked);
                        this.mWindowInfoById.put(id, WindowInfo.obtain(windowInfo));
                    }
                }
                if (this.mTouchInteractionInProgress && z2) {
                    this.mActiveWindowId = this.mFocusedWindowId;
                }
                int size2 = this.mWindows.size();
                boolean z3 = z;
                for (int i3 = 0; i3 < size2; i3++) {
                    AccessibilityWindowInfo accessibilityWindowInfo = this.mWindows.get(i3);
                    if (accessibilityWindowInfo.getId() == this.mActiveWindowId) {
                        accessibilityWindowInfo.setActive(true);
                    }
                    if (accessibilityWindowInfo.getId() == this.mAccessibilityFocusedWindowId) {
                        accessibilityWindowInfo.setAccessibilityFocused(true);
                        z3 = false;
                    }
                }
                z = z3;
            }
            sendEventsForChangedWindowsLocked(arrayList, sparseArrayClone);
            for (int size3 = arrayList.size() - 1; size3 >= 0; size3--) {
                arrayList.remove(size3).recycle();
            }
            if (z) {
                AccessibilityManagerService.this.mMainHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$Xd4PICw0vnPU2BuBjOCbMMfcgU.INSTANCE, AccessibilityManagerService.this, box(this.mAccessibilityFocusedWindowId)));
            }
        }

        private void sendEventsForChangedWindowsLocked(List<AccessibilityWindowInfo> list, SparseArray<AccessibilityWindowInfo> sparseArray) {
            ArrayList arrayList = new ArrayList();
            int size = list.size();
            for (int i = 0; i < size; i++) {
                AccessibilityWindowInfo accessibilityWindowInfo = list.get(i);
                if (this.mA11yWindowInfoById.get(accessibilityWindowInfo.getId()) == null) {
                    arrayList.add(AccessibilityEvent.obtainWindowsChangedEvent(accessibilityWindowInfo.getId(), 2));
                }
            }
            int size2 = this.mWindows.size();
            for (int i2 = 0; i2 < size2; i2++) {
                AccessibilityWindowInfo accessibilityWindowInfo2 = this.mWindows.get(i2);
                AccessibilityWindowInfo accessibilityWindowInfo3 = sparseArray.get(accessibilityWindowInfo2.getId());
                if (accessibilityWindowInfo3 == null) {
                    arrayList.add(AccessibilityEvent.obtainWindowsChangedEvent(accessibilityWindowInfo2.getId(), 1));
                } else {
                    int iDifferenceFrom = accessibilityWindowInfo2.differenceFrom(accessibilityWindowInfo3);
                    if (iDifferenceFrom != 0) {
                        arrayList.add(AccessibilityEvent.obtainWindowsChangedEvent(accessibilityWindowInfo2.getId(), iDifferenceFrom));
                    }
                }
            }
            int size3 = arrayList.size();
            for (int i3 = 0; i3 < size3; i3++) {
                AccessibilityManagerService.this.sendAccessibilityEventLocked((AccessibilityEvent) arrayList.get(i3), AccessibilityManagerService.this.mCurrentUserId);
            }
        }

        public boolean computePartialInteractiveRegionForWindowLocked(int i, Region region) {
            boolean z = false;
            if (this.mWindows == null) {
                return false;
            }
            Region region2 = null;
            for (int size = this.mWindows.size() - 1; size >= 0; size--) {
                AccessibilityWindowInfo accessibilityWindowInfo = this.mWindows.get(size);
                if (region2 == null) {
                    if (accessibilityWindowInfo.getId() == i) {
                        Rect rect = AccessibilityManagerService.this.mTempRect;
                        accessibilityWindowInfo.getBoundsInScreen(rect);
                        region.set(rect);
                        region2 = region;
                    }
                } else if (accessibilityWindowInfo.getType() != 4) {
                    Rect rect2 = AccessibilityManagerService.this.mTempRect;
                    accessibilityWindowInfo.getBoundsInScreen(rect2);
                    if (region2.op(rect2, Region.Op.DIFFERENCE)) {
                        z = true;
                    }
                }
            }
            return z;
        }

        public void updateEventSourceLocked(AccessibilityEvent accessibilityEvent) {
            if ((accessibilityEvent.getEventType() & KEEP_SOURCE_EVENT_TYPES) == 0) {
                accessibilityEvent.setSource((View) null);
            }
        }

        public void updateActiveAndAccessibilityFocusedWindowLocked(int i, long j, int i2, int i3) {
            if (i2 == 32) {
                synchronized (AccessibilityManagerService.this.mLock) {
                    if (AccessibilityManagerService.this.mWindowsForAccessibilityCallback == null) {
                        this.mFocusedWindowId = getFocusedWindowId();
                        if (i == this.mFocusedWindowId) {
                            this.mActiveWindowId = i;
                        }
                    }
                }
                return;
            }
            if (i2 == 128) {
                synchronized (AccessibilityManagerService.this.mLock) {
                    if (this.mTouchInteractionInProgress && this.mActiveWindowId != i) {
                        setActiveWindowLocked(i);
                    }
                }
                return;
            }
            if (i2 == 32768) {
                synchronized (AccessibilityManagerService.this.mLock) {
                    if (this.mAccessibilityFocusedWindowId != i) {
                        AccessibilityManagerService.this.mMainHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$Xd4PICw0vnPU2BuBjOCbMMfcgU.INSTANCE, AccessibilityManagerService.this, box(this.mAccessibilityFocusedWindowId)));
                        AccessibilityManagerService.this.mSecurityPolicy.setAccessibilityFocusedWindowLocked(i);
                        this.mAccessibilityFocusNodeId = j;
                    }
                }
                return;
            }
            if (i2 == 65536) {
                synchronized (AccessibilityManagerService.this.mLock) {
                    if (this.mAccessibilityFocusNodeId == j) {
                        this.mAccessibilityFocusNodeId = 2147483647L;
                    }
                    if (this.mAccessibilityFocusNodeId == 2147483647L && this.mAccessibilityFocusedWindowId == i && i3 != 64) {
                        this.mAccessibilityFocusedWindowId = -1;
                    }
                }
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
                int i = AccessibilityManagerService.this.mSecurityPolicy.mActiveWindowId;
                setActiveWindowLocked(this.mFocusedWindowId);
                if (i != AccessibilityManagerService.this.mSecurityPolicy.mActiveWindowId && this.mAccessibilityFocusedWindowId == i && AccessibilityManagerService.this.getCurrentUserStateLocked().mAccessibilityFocusOnlyInActiveWindow) {
                    AccessibilityManagerService.this.mMainHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$Xd4PICw0vnPU2BuBjOCbMMfcgU.INSTANCE, AccessibilityManagerService.this, box(i)));
                }
            }
        }

        private IntSupplier box(int i) {
            return PooledLambda.obtainSupplier(i).recycleOnUse();
        }

        public int getActiveWindowId() {
            if (this.mActiveWindowId == -1 && !this.mTouchInteractionInProgress) {
                this.mActiveWindowId = getFocusedWindowId();
            }
            return this.mActiveWindowId;
        }

        private void setActiveWindowLocked(int i) {
            if (this.mActiveWindowId != i) {
                AccessibilityManagerService.this.sendAccessibilityEventLocked(AccessibilityEvent.obtainWindowsChangedEvent(this.mActiveWindowId, 32), AccessibilityManagerService.this.mCurrentUserId);
                this.mActiveWindowId = i;
                if (this.mWindows != null) {
                    int size = this.mWindows.size();
                    for (int i2 = 0; i2 < size; i2++) {
                        AccessibilityWindowInfo accessibilityWindowInfo = this.mWindows.get(i2);
                        if (accessibilityWindowInfo.getId() == i) {
                            accessibilityWindowInfo.setActive(true);
                            AccessibilityManagerService.this.sendAccessibilityEventLocked(AccessibilityEvent.obtainWindowsChangedEvent(i, 32), AccessibilityManagerService.this.mCurrentUserId);
                        } else {
                            accessibilityWindowInfo.setActive(false);
                        }
                    }
                }
            }
        }

        private void setAccessibilityFocusedWindowLocked(int i) {
            if (this.mAccessibilityFocusedWindowId != i) {
                AccessibilityManagerService.this.sendAccessibilityEventLocked(AccessibilityEvent.obtainWindowsChangedEvent(this.mAccessibilityFocusedWindowId, 128), AccessibilityManagerService.this.mCurrentUserId);
                this.mAccessibilityFocusedWindowId = i;
                if (this.mWindows != null) {
                    int size = this.mWindows.size();
                    for (int i2 = 0; i2 < size; i2++) {
                        AccessibilityWindowInfo accessibilityWindowInfo = this.mWindows.get(i2);
                        if (accessibilityWindowInfo.getId() == i) {
                            accessibilityWindowInfo.setAccessibilityFocused(true);
                            AccessibilityManagerService.this.sendAccessibilityEventLocked(AccessibilityEvent.obtainWindowsChangedEvent(i, 128), AccessibilityManagerService.this.mCurrentUserId);
                        } else {
                            accessibilityWindowInfo.setAccessibilityFocused(false);
                        }
                    }
                }
            }
        }

        public boolean canGetAccessibilityNodeInfoLocked(AbstractAccessibilityServiceConnection abstractAccessibilityServiceConnection, int i) {
            return canRetrieveWindowContentLocked(abstractAccessibilityServiceConnection) && isRetrievalAllowingWindowLocked(i);
        }

        public boolean canRetrieveWindowsLocked(AbstractAccessibilityServiceConnection abstractAccessibilityServiceConnection) {
            return canRetrieveWindowContentLocked(abstractAccessibilityServiceConnection) && abstractAccessibilityServiceConnection.mRetrieveInteractiveWindows;
        }

        public boolean canRetrieveWindowContentLocked(AbstractAccessibilityServiceConnection abstractAccessibilityServiceConnection) {
            return (abstractAccessibilityServiceConnection.getCapabilities() & 1) != 0;
        }

        public boolean canControlMagnification(AbstractAccessibilityServiceConnection abstractAccessibilityServiceConnection) {
            return (abstractAccessibilityServiceConnection.getCapabilities() & 16) != 0;
        }

        public boolean canPerformGestures(AccessibilityServiceConnection accessibilityServiceConnection) {
            return (accessibilityServiceConnection.getCapabilities() & 32) != 0;
        }

        public boolean canCaptureFingerprintGestures(AccessibilityServiceConnection accessibilityServiceConnection) {
            return (accessibilityServiceConnection.getCapabilities() & 64) != 0;
        }

        private int resolveProfileParentLocked(int i) {
            if (i != AccessibilityManagerService.this.mCurrentUserId) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    UserInfo profileParent = AccessibilityManagerService.this.mUserManager.getProfileParent(i);
                    if (profileParent != null) {
                        return profileParent.getUserHandle().getIdentifier();
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return i;
        }

        public int resolveCallingUserIdEnforcingPermissionsLocked(int i) {
            int callingUid = Binder.getCallingUid();
            if (callingUid == 0 || callingUid == 1000 || callingUid == 2000) {
                if (i == -2 || i == -3) {
                    return AccessibilityManagerService.this.mCurrentUserId;
                }
                return resolveProfileParentLocked(i);
            }
            int userId = UserHandle.getUserId(callingUid);
            if (userId != i) {
                if (resolveProfileParentLocked(userId) == AccessibilityManagerService.this.mCurrentUserId && (i == -2 || i == -3)) {
                    return AccessibilityManagerService.this.mCurrentUserId;
                }
                if (!hasPermission("android.permission.INTERACT_ACROSS_USERS") && !hasPermission("android.permission.INTERACT_ACROSS_USERS_FULL")) {
                    throw new SecurityException("Call from user " + userId + " as user " + i + " without permission INTERACT_ACROSS_USERS or INTERACT_ACROSS_USERS_FULL not allowed.");
                }
                if (i == -2 || i == -3) {
                    return AccessibilityManagerService.this.mCurrentUserId;
                }
                throw new IllegalArgumentException("Calling user can be changed to only UserHandle.USER_CURRENT or UserHandle.USER_CURRENT_OR_SELF.");
            }
            return resolveProfileParentLocked(i);
        }

        public boolean isCallerInteractingAcrossUsers(int i) {
            return Binder.getCallingPid() == Process.myPid() || Binder.getCallingUid() == 2000 || i == -2 || i == -3;
        }

        private boolean isRetrievalAllowingWindowLocked(int i) {
            if (Binder.getCallingUid() == 1000) {
                return true;
            }
            if (Binder.getCallingUid() != 2000 || isShellAllowedToRetrieveWindowLocked(i)) {
                return i == this.mActiveWindowId || findA11yWindowInfoById(i) != null;
            }
            return false;
        }

        private boolean isShellAllowedToRetrieveWindowLocked(int i) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                IBinder iBinderFindWindowTokenLocked = AccessibilityManagerService.this.findWindowTokenLocked(i);
                if (iBinderFindWindowTokenLocked == null) {
                    return false;
                }
                if (AccessibilityManagerService.this.mWindowManagerService.getWindowOwnerUserId(iBinderFindWindowTokenLocked) == -10000) {
                    return false;
                }
                return !AccessibilityManagerService.this.mUserManager.hasUserRestriction("no_debugging_features", UserHandle.of(r5));
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public AccessibilityWindowInfo findA11yWindowInfoById(int i) {
            return this.mA11yWindowInfoById.get(i);
        }

        private WindowInfo findWindowInfoById(int i) {
            return this.mWindowInfoById.get(i);
        }

        private AccessibilityWindowInfo getPictureInPictureWindow() {
            if (this.mWindows != null) {
                int size = this.mWindows.size();
                for (int i = 0; i < size; i++) {
                    AccessibilityWindowInfo accessibilityWindowInfo = this.mWindows.get(i);
                    if (accessibilityWindowInfo.isInPictureInPictureMode()) {
                        return accessibilityWindowInfo;
                    }
                }
                return null;
            }
            return null;
        }

        private void enforceCallingPermission(String str, String str2) {
            if (AccessibilityManagerService.OWN_PROCESS_ID != Binder.getCallingPid() && !hasPermission(str)) {
                throw new SecurityException("You do not have " + str + " required to call " + str2 + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            }
        }

        private boolean hasPermission(String str) {
            return AccessibilityManagerService.this.mContext.checkCallingPermission(str) == 0;
        }

        private int getFocusedWindowId() {
            int iFindWindowIdLocked;
            IBinder focusedWindowToken = AccessibilityManagerService.this.mWindowManagerService.getFocusedWindowToken();
            synchronized (AccessibilityManagerService.this.mLock) {
                iFindWindowIdLocked = AccessibilityManagerService.this.findWindowIdLocked(focusedWindowToken);
            }
            return iFindWindowIdLocked;
        }
    }

    class Client {
        final IAccessibilityManagerClient mCallback;
        int mLastSentRelevantEventTypes;
        final String[] mPackageNames;

        private Client(IAccessibilityManagerClient iAccessibilityManagerClient, int i, UserState userState) {
            this.mCallback = iAccessibilityManagerClient;
            this.mPackageNames = AccessibilityManagerService.this.mPackageManager.getPackagesForUid(i);
            synchronized (AccessibilityManagerService.this.mLock) {
                this.mLastSentRelevantEventTypes = AccessibilityManagerService.this.computeRelevantEventTypesLocked(userState, this);
            }
        }
    }

    public class UserState {
        public boolean mAccessibilityFocusOnlyInActiveWindow;
        public boolean mBindInstantServiceAllowed;
        public boolean mIsAutoclickEnabled;
        public boolean mIsDisplayMagnificationEnabled;
        public boolean mIsFilterKeyEventsEnabled;
        public boolean mIsNavBarMagnificationAssignedToAccessibilityButton;
        public boolean mIsNavBarMagnificationEnabled;
        public boolean mIsPerformGesturesEnabled;
        public boolean mIsTextHighContrastEnabled;
        public boolean mIsTouchExplorationEnabled;
        public ComponentName mServiceAssignedToAccessibilityButton;
        public ComponentName mServiceChangingSoftKeyboardMode;
        public ComponentName mServiceToEnableWithShortcut;
        public final int mUserId;
        public final RemoteCallbackList<IAccessibilityManagerClient> mUserClients = new RemoteCallbackList<>();
        public final SparseArray<RemoteAccessibilityConnection> mInteractionConnections = new SparseArray<>();
        public final SparseArray<IBinder> mWindowTokens = new SparseArray<>();
        public final ArrayList<AccessibilityServiceConnection> mBoundServices = new ArrayList<>();
        public final Map<ComponentName, AccessibilityServiceConnection> mComponentNameToServiceMap = new HashMap();
        public final List<AccessibilityServiceInfo> mInstalledServices = new ArrayList();
        private final Set<ComponentName> mBindingServices = new HashSet();
        public final Set<ComponentName> mEnabledServices = new HashSet();
        public final Set<ComponentName> mTouchExplorationGrantedServices = new HashSet();
        public int mLastSentClientState = -1;
        public int mSoftKeyboardShowMode = 0;

        public UserState(int i) {
            this.mUserId = i;
        }

        public int getClientState() {
            boolean z = AccessibilityManagerService.this.mUiAutomationManager.isUiAutomationRunningLocked() || isHandlingAccessibilityEvents();
            int i = z ? 1 : 0;
            if (z && this.mIsTouchExplorationEnabled) {
                i |= 2;
            }
            if (this.mIsTextHighContrastEnabled) {
                return i | 4;
            }
            return i;
        }

        public boolean isHandlingAccessibilityEvents() {
            return (this.mBoundServices.isEmpty() && this.mBindingServices.isEmpty()) ? false : true;
        }

        public void onSwitchToAnotherUserLocked() {
            AccessibilityManagerService.this.unbindAllServicesLocked(this);
            this.mBoundServices.clear();
            this.mBindingServices.clear();
            this.mLastSentClientState = -1;
            this.mEnabledServices.clear();
            this.mTouchExplorationGrantedServices.clear();
            this.mIsTouchExplorationEnabled = false;
            this.mIsDisplayMagnificationEnabled = false;
            this.mIsNavBarMagnificationEnabled = false;
            this.mServiceAssignedToAccessibilityButton = null;
            this.mIsNavBarMagnificationAssignedToAccessibilityButton = false;
            this.mIsAutoclickEnabled = false;
            this.mSoftKeyboardShowMode = 0;
        }

        public void addServiceLocked(AccessibilityServiceConnection accessibilityServiceConnection) {
            if (!this.mBoundServices.contains(accessibilityServiceConnection)) {
                accessibilityServiceConnection.onAdded();
                this.mBoundServices.add(accessibilityServiceConnection);
                this.mComponentNameToServiceMap.put(accessibilityServiceConnection.mComponentName, accessibilityServiceConnection);
                AccessibilityManagerService.this.scheduleNotifyClientsOfServicesStateChange(this);
            }
        }

        public void removeServiceLocked(AccessibilityServiceConnection accessibilityServiceConnection) {
            this.mBoundServices.remove(accessibilityServiceConnection);
            accessibilityServiceConnection.onRemoved();
            this.mComponentNameToServiceMap.clear();
            for (int i = 0; i < this.mBoundServices.size(); i++) {
                AccessibilityServiceConnection accessibilityServiceConnection2 = this.mBoundServices.get(i);
                this.mComponentNameToServiceMap.put(accessibilityServiceConnection2.mComponentName, accessibilityServiceConnection2);
            }
            AccessibilityManagerService.this.scheduleNotifyClientsOfServicesStateChange(this);
        }

        public Set<ComponentName> getBindingServicesLocked() {
            return this.mBindingServices;
        }
    }

    private final class AccessibilityContentObserver extends ContentObserver {
        private final Uri mAccessibilityButtonComponentIdUri;
        private final Uri mAccessibilityShortcutServiceIdUri;
        private final Uri mAccessibilitySoftKeyboardModeUri;
        private final Uri mAutoclickEnabledUri;
        private final Uri mDisplayDaltonizerEnabledUri;
        private final Uri mDisplayDaltonizerUri;
        private final Uri mDisplayInversionEnabledUri;
        private final Uri mDisplayMagnificationEnabledUri;
        private final Uri mEnabledAccessibilityServicesUri;
        private final Uri mHighTextContrastUri;
        private final Uri mNavBarMagnificationEnabledUri;
        private final Uri mTouchExplorationEnabledUri;
        private final Uri mTouchExplorationGrantedAccessibilityServicesUri;

        public AccessibilityContentObserver(Handler handler) {
            super(handler);
            this.mTouchExplorationEnabledUri = Settings.Secure.getUriFor("touch_exploration_enabled");
            this.mDisplayMagnificationEnabledUri = Settings.Secure.getUriFor("accessibility_display_magnification_enabled");
            this.mNavBarMagnificationEnabledUri = Settings.Secure.getUriFor("accessibility_display_magnification_navbar_enabled");
            this.mAutoclickEnabledUri = Settings.Secure.getUriFor("accessibility_autoclick_enabled");
            this.mEnabledAccessibilityServicesUri = Settings.Secure.getUriFor("enabled_accessibility_services");
            this.mTouchExplorationGrantedAccessibilityServicesUri = Settings.Secure.getUriFor("touch_exploration_granted_accessibility_services");
            this.mDisplayInversionEnabledUri = Settings.Secure.getUriFor("accessibility_display_inversion_enabled");
            this.mDisplayDaltonizerEnabledUri = Settings.Secure.getUriFor("accessibility_display_daltonizer_enabled");
            this.mDisplayDaltonizerUri = Settings.Secure.getUriFor("accessibility_display_daltonizer");
            this.mHighTextContrastUri = Settings.Secure.getUriFor("high_text_contrast_enabled");
            this.mAccessibilitySoftKeyboardModeUri = Settings.Secure.getUriFor("accessibility_soft_keyboard_mode");
            this.mAccessibilityShortcutServiceIdUri = Settings.Secure.getUriFor("accessibility_shortcut_target_service");
            this.mAccessibilityButtonComponentIdUri = Settings.Secure.getUriFor("accessibility_button_target_component");
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(this.mTouchExplorationEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mDisplayMagnificationEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mNavBarMagnificationEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mAutoclickEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mEnabledAccessibilityServicesUri, false, this, -1);
            contentResolver.registerContentObserver(this.mTouchExplorationGrantedAccessibilityServicesUri, false, this, -1);
            contentResolver.registerContentObserver(this.mDisplayInversionEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mDisplayDaltonizerEnabledUri, false, this, -1);
            contentResolver.registerContentObserver(this.mDisplayDaltonizerUri, false, this, -1);
            contentResolver.registerContentObserver(this.mHighTextContrastUri, false, this, -1);
            contentResolver.registerContentObserver(this.mAccessibilitySoftKeyboardModeUri, false, this, -1);
            contentResolver.registerContentObserver(this.mAccessibilityShortcutServiceIdUri, false, this, -1);
            contentResolver.registerContentObserver(this.mAccessibilityButtonComponentIdUri, false, this, -1);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            synchronized (AccessibilityManagerService.this.mLock) {
                UserState currentUserStateLocked = AccessibilityManagerService.this.getCurrentUserStateLocked();
                if (this.mTouchExplorationEnabledUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readTouchExplorationEnabledSettingLocked(currentUserStateLocked)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(currentUserStateLocked);
                    }
                } else if (this.mDisplayMagnificationEnabledUri.equals(uri) || this.mNavBarMagnificationEnabledUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readMagnificationEnabledSettingsLocked(currentUserStateLocked)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(currentUserStateLocked);
                    }
                } else if (this.mAutoclickEnabledUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readAutoclickEnabledSettingLocked(currentUserStateLocked)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(currentUserStateLocked);
                    }
                } else if (this.mEnabledAccessibilityServicesUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readEnabledAccessibilityServicesLocked(currentUserStateLocked)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(currentUserStateLocked);
                    }
                } else if (this.mTouchExplorationGrantedAccessibilityServicesUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readTouchExplorationGrantedAccessibilityServicesLocked(currentUserStateLocked)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(currentUserStateLocked);
                    }
                } else if (this.mDisplayDaltonizerEnabledUri.equals(uri) || this.mDisplayDaltonizerUri.equals(uri)) {
                    AccessibilityManagerService.this.updateDisplayDaltonizerLocked(currentUserStateLocked);
                } else if (this.mDisplayInversionEnabledUri.equals(uri)) {
                    AccessibilityManagerService.this.updateDisplayInversionLocked(currentUserStateLocked);
                } else if (this.mHighTextContrastUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readHighTextContrastEnabledSettingLocked(currentUserStateLocked)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(currentUserStateLocked);
                    }
                } else if (this.mAccessibilitySoftKeyboardModeUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readSoftKeyboardShowModeChangedLocked(currentUserStateLocked)) {
                        AccessibilityManagerService.this.notifySoftKeyboardShowModeChangedLocked(currentUserStateLocked.mSoftKeyboardShowMode);
                        AccessibilityManagerService.this.onUserStateChangedLocked(currentUserStateLocked);
                    }
                } else if (this.mAccessibilityShortcutServiceIdUri.equals(uri)) {
                    if (AccessibilityManagerService.this.readAccessibilityShortcutSettingLocked(currentUserStateLocked)) {
                        AccessibilityManagerService.this.onUserStateChangedLocked(currentUserStateLocked);
                    }
                } else if (this.mAccessibilityButtonComponentIdUri.equals(uri) && AccessibilityManagerService.this.readAccessibilityButtonSettingsLocked(currentUserStateLocked)) {
                    AccessibilityManagerService.this.onUserStateChangedLocked(currentUserStateLocked);
                }
            }
        }
    }
}
