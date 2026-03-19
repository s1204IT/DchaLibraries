package com.android.server.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.accessibilityservice.IAccessibilityServiceConnection;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ParceledListSlice;
import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Slog;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MagnificationSpec;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.DumpUtils;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.accessibility.FingerprintGestureDispatcher;
import com.android.server.accessibility.KeyEventDispatcher;
import com.android.server.wm.WindowManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract class AbstractAccessibilityServiceConnection extends IAccessibilityServiceConnection.Stub implements ServiceConnection, IBinder.DeathRecipient, KeyEventDispatcher.KeyEventFilter, FingerprintGestureDispatcher.FingerprintGestureClient {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "AbstractAccessibilityServiceConnection";
    protected final AccessibilityServiceInfo mAccessibilityServiceInfo;
    boolean mCaptureFingerprintGestures;
    final ComponentName mComponentName;
    protected final Context mContext;
    public Handler mEventDispatchHandler;
    int mEventTypes;
    int mFeedbackType;
    int mFetchFlags;
    private final GlobalActionPerformer mGlobalActionPerformer;
    final int mId;
    public final InvocationHandler mInvocationHandler;
    boolean mIsDefault;
    boolean mLastAccessibilityButtonCallbackState;
    protected final Object mLock;
    long mNotificationTimeout;
    boolean mReceivedAccessibilityButtonCallbackSinceBind;
    boolean mRequestAccessibilityButton;
    boolean mRequestFilterKeyEvents;
    boolean mRequestTouchExplorationMode;
    boolean mRetrieveInteractiveWindows;
    protected final AccessibilityManagerService.SecurityPolicy mSecurityPolicy;
    IBinder mService;
    IAccessibilityServiceClient mServiceInterface;
    protected final SystemSupport mSystemSupport;
    private final WindowManagerInternal mWindowManagerService;
    Set<String> mPackageNames = new HashSet();
    final SparseArray<AccessibilityEvent> mPendingEvents = new SparseArray<>();
    boolean mUsesAccessibilityCache = false;
    final IBinder mOverlayWindowToken = new Binder();

    public interface SystemSupport {
        void ensureWindowsAvailableTimed();

        MagnificationSpec getCompatibleMagnificationSpecLocked(int i);

        AccessibilityManagerService.RemoteAccessibilityConnection getConnectionLocked(int i);

        int getCurrentUserIdLocked();

        FingerprintGestureDispatcher getFingerprintGestureDispatcher();

        KeyEventDispatcher getKeyEventDispatcher();

        MagnificationController getMagnificationController();

        MotionEventInjector getMotionEventInjectorLocked();

        PendingIntent getPendingIntentActivity(Context context, int i, Intent intent, int i2);

        boolean isAccessibilityButtonShown();

        void onClientChange(boolean z);

        boolean performAccessibilityAction(int i, long j, int i2, Bundle bundle, int i3, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i4, long j2);

        void persistComponentNamesToSettingLocked(String str, Set<ComponentName> set, int i);

        IAccessibilityInteractionConnectionCallback replaceCallbackIfNeeded(IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i, int i2, int i3, long j);
    }

    protected abstract boolean isCalledForCurrentUserLocked();

    public AbstractAccessibilityServiceConnection(Context context, ComponentName componentName, AccessibilityServiceInfo accessibilityServiceInfo, int i, Handler handler, Object obj, AccessibilityManagerService.SecurityPolicy securityPolicy, SystemSupport systemSupport, WindowManagerInternal windowManagerInternal, GlobalActionPerformer globalActionPerformer) {
        this.mContext = context;
        this.mWindowManagerService = windowManagerInternal;
        this.mId = i;
        this.mComponentName = componentName;
        this.mAccessibilityServiceInfo = accessibilityServiceInfo;
        this.mLock = obj;
        this.mSecurityPolicy = securityPolicy;
        this.mGlobalActionPerformer = globalActionPerformer;
        this.mSystemSupport = systemSupport;
        this.mInvocationHandler = new InvocationHandler(handler.getLooper());
        this.mEventDispatchHandler = new Handler(handler.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                AbstractAccessibilityServiceConnection.this.notifyAccessibilityEventInternal(message.what, (AccessibilityEvent) message.obj, message.arg1 != 0);
            }
        };
        setDynamicallyConfigurableProperties(accessibilityServiceInfo);
    }

    @Override
    public boolean onKeyEvent(KeyEvent keyEvent, int i) {
        if (!this.mRequestFilterKeyEvents || this.mServiceInterface == null || (this.mAccessibilityServiceInfo.getCapabilities() & 8) == 0) {
            return false;
        }
        try {
            this.mServiceInterface.onKeyEvent(keyEvent, i);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setDynamicallyConfigurableProperties(AccessibilityServiceInfo accessibilityServiceInfo) {
        this.mEventTypes = accessibilityServiceInfo.eventTypes;
        this.mFeedbackType = accessibilityServiceInfo.feedbackType;
        String[] strArr = accessibilityServiceInfo.packageNames;
        if (strArr != null) {
            this.mPackageNames.addAll(Arrays.asList(strArr));
        }
        this.mNotificationTimeout = accessibilityServiceInfo.notificationTimeout;
        this.mIsDefault = (accessibilityServiceInfo.flags & 1) != 0;
        if (supportsFlagForNotImportantViews(accessibilityServiceInfo)) {
            if ((accessibilityServiceInfo.flags & 2) != 0) {
                this.mFetchFlags |= 8;
            } else {
                this.mFetchFlags &= -9;
            }
        }
        if ((accessibilityServiceInfo.flags & 16) != 0) {
            this.mFetchFlags |= 16;
        } else {
            this.mFetchFlags &= -17;
        }
        this.mRequestTouchExplorationMode = (accessibilityServiceInfo.flags & 4) != 0;
        this.mRequestFilterKeyEvents = (accessibilityServiceInfo.flags & 32) != 0;
        this.mRetrieveInteractiveWindows = (accessibilityServiceInfo.flags & 64) != 0;
        this.mCaptureFingerprintGestures = (accessibilityServiceInfo.flags & 512) != 0;
        this.mRequestAccessibilityButton = (accessibilityServiceInfo.flags & 256) != 0;
    }

    protected boolean supportsFlagForNotImportantViews(AccessibilityServiceInfo accessibilityServiceInfo) {
        return accessibilityServiceInfo.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion >= 16;
    }

    public boolean canReceiveEventsLocked() {
        return (this.mEventTypes == 0 || this.mFeedbackType == 0 || this.mService == null) ? false : true;
    }

    public void setOnKeyEventResult(boolean z, int i) {
        this.mSystemSupport.getKeyEventDispatcher().setOnKeyEventResult(this, z, i);
    }

    public AccessibilityServiceInfo getServiceInfo() {
        AccessibilityServiceInfo accessibilityServiceInfo;
        synchronized (this.mLock) {
            accessibilityServiceInfo = this.mAccessibilityServiceInfo;
        }
        return accessibilityServiceInfo;
    }

    public int getCapabilities() {
        return this.mAccessibilityServiceInfo.getCapabilities();
    }

    int getRelevantEventTypes() {
        return (this.mUsesAccessibilityCache ? 4307005 : 0) | this.mEventTypes;
    }

    public void setServiceInfo(AccessibilityServiceInfo accessibilityServiceInfo) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                AccessibilityServiceInfo accessibilityServiceInfo2 = this.mAccessibilityServiceInfo;
                if (accessibilityServiceInfo2 != null) {
                    accessibilityServiceInfo2.updateDynamicallyConfigurableProperties(accessibilityServiceInfo);
                    setDynamicallyConfigurableProperties(accessibilityServiceInfo2);
                } else {
                    setDynamicallyConfigurableProperties(accessibilityServiceInfo);
                }
                this.mSystemSupport.onClientChange(true);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public List<AccessibilityWindowInfo> getWindows() {
        this.mSystemSupport.ensureWindowsAvailableTimed();
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return null;
            }
            if (!this.mSecurityPolicy.canRetrieveWindowsLocked(this)) {
                return null;
            }
            if (this.mSecurityPolicy.mWindows == null) {
                return null;
            }
            ArrayList arrayList = new ArrayList();
            int size = this.mSecurityPolicy.mWindows.size();
            for (int i = 0; i < size; i++) {
                AccessibilityWindowInfo accessibilityWindowInfoObtain = AccessibilityWindowInfo.obtain(this.mSecurityPolicy.mWindows.get(i));
                accessibilityWindowInfoObtain.setConnectionId(this.mId);
                arrayList.add(accessibilityWindowInfoObtain);
            }
            return arrayList;
        }
    }

    public AccessibilityWindowInfo getWindow(int i) {
        this.mSystemSupport.ensureWindowsAvailableTimed();
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return null;
            }
            if (!this.mSecurityPolicy.canRetrieveWindowsLocked(this)) {
                return null;
            }
            AccessibilityWindowInfo accessibilityWindowInfoFindA11yWindowInfoById = this.mSecurityPolicy.findA11yWindowInfoById(i);
            if (accessibilityWindowInfoFindA11yWindowInfoById == null) {
                return null;
            }
            AccessibilityWindowInfo accessibilityWindowInfoObtain = AccessibilityWindowInfo.obtain(accessibilityWindowInfoFindA11yWindowInfoById);
            accessibilityWindowInfoObtain.setConnectionId(this.mId);
            return accessibilityWindowInfoObtain;
        }
    }

    public String[] findAccessibilityNodeInfosByViewId(int i, long j, String str, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, long j2) throws RemoteException {
        Region regionObtain = Region.obtain();
        synchronized (this.mLock) {
            this.mUsesAccessibilityCache = true;
            if (!isCalledForCurrentUserLocked()) {
                return null;
            }
            int iResolveAccessibilityWindowIdLocked = resolveAccessibilityWindowIdLocked(i);
            if (!this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, iResolveAccessibilityWindowIdLocked)) {
                return null;
            }
            AccessibilityManagerService.RemoteAccessibilityConnection connectionLocked = this.mSystemSupport.getConnectionLocked(iResolveAccessibilityWindowIdLocked);
            if (connectionLocked == null) {
                return null;
            }
            if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(iResolveAccessibilityWindowIdLocked, regionObtain)) {
                regionObtain.recycle();
                regionObtain = null;
            }
            MagnificationSpec compatibleMagnificationSpecLocked = this.mSystemSupport.getCompatibleMagnificationSpecLocked(iResolveAccessibilityWindowIdLocked);
            int callingPid = Binder.getCallingPid();
            IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallbackReplaceCallbackIfNeeded = this.mSystemSupport.replaceCallbackIfNeeded(iAccessibilityInteractionConnectionCallback, iResolveAccessibilityWindowIdLocked, i2, callingPid, j2);
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                connectionLocked.getRemote().findAccessibilityNodeInfosByViewId(j, str, regionObtain, i2, iAccessibilityInteractionConnectionCallbackReplaceCallbackIfNeeded, this.mFetchFlags, callingPid, j2, compatibleMagnificationSpecLocked);
                String[] strArrComputeValidReportedPackages = this.mSecurityPolicy.computeValidReportedPackages(callingUid, connectionLocked.getPackageName(), connectionLocked.getUid());
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                return strArrComputeValidReportedPackages;
            } catch (RemoteException e) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                return null;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                throw th;
            }
        }
    }

    public String[] findAccessibilityNodeInfosByText(int i, long j, String str, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, long j2) throws RemoteException {
        Region regionObtain = Region.obtain();
        synchronized (this.mLock) {
            this.mUsesAccessibilityCache = true;
            if (!isCalledForCurrentUserLocked()) {
                return null;
            }
            int iResolveAccessibilityWindowIdLocked = resolveAccessibilityWindowIdLocked(i);
            if (!this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, iResolveAccessibilityWindowIdLocked)) {
                return null;
            }
            AccessibilityManagerService.RemoteAccessibilityConnection connectionLocked = this.mSystemSupport.getConnectionLocked(iResolveAccessibilityWindowIdLocked);
            if (connectionLocked == null) {
                return null;
            }
            if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(iResolveAccessibilityWindowIdLocked, regionObtain)) {
                regionObtain.recycle();
                regionObtain = null;
            }
            MagnificationSpec compatibleMagnificationSpecLocked = this.mSystemSupport.getCompatibleMagnificationSpecLocked(iResolveAccessibilityWindowIdLocked);
            int callingPid = Binder.getCallingPid();
            IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallbackReplaceCallbackIfNeeded = this.mSystemSupport.replaceCallbackIfNeeded(iAccessibilityInteractionConnectionCallback, iResolveAccessibilityWindowIdLocked, i2, callingPid, j2);
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                connectionLocked.getRemote().findAccessibilityNodeInfosByText(j, str, regionObtain, i2, iAccessibilityInteractionConnectionCallbackReplaceCallbackIfNeeded, this.mFetchFlags, callingPid, j2, compatibleMagnificationSpecLocked);
                String[] strArrComputeValidReportedPackages = this.mSecurityPolicy.computeValidReportedPackages(callingUid, connectionLocked.getPackageName(), connectionLocked.getUid());
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                return strArrComputeValidReportedPackages;
            } catch (RemoteException e) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                return null;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                throw th;
            }
        }
    }

    public String[] findAccessibilityNodeInfoByAccessibilityId(int i, long j, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i3, long j2, Bundle bundle) throws RemoteException {
        Region regionObtain = Region.obtain();
        synchronized (this.mLock) {
            this.mUsesAccessibilityCache = true;
            if (!isCalledForCurrentUserLocked()) {
                return null;
            }
            int iResolveAccessibilityWindowIdLocked = resolveAccessibilityWindowIdLocked(i);
            if (!this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, iResolveAccessibilityWindowIdLocked)) {
                return null;
            }
            AccessibilityManagerService.RemoteAccessibilityConnection connectionLocked = this.mSystemSupport.getConnectionLocked(iResolveAccessibilityWindowIdLocked);
            if (connectionLocked == null) {
                return null;
            }
            if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(iResolveAccessibilityWindowIdLocked, regionObtain)) {
                regionObtain.recycle();
                regionObtain = null;
            }
            MagnificationSpec compatibleMagnificationSpecLocked = this.mSystemSupport.getCompatibleMagnificationSpecLocked(iResolveAccessibilityWindowIdLocked);
            int callingPid = Binder.getCallingPid();
            IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallbackReplaceCallbackIfNeeded = this.mSystemSupport.replaceCallbackIfNeeded(iAccessibilityInteractionConnectionCallback, iResolveAccessibilityWindowIdLocked, i2, callingPid, j2);
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                connectionLocked.getRemote().findAccessibilityNodeInfoByAccessibilityId(j, regionObtain, i2, iAccessibilityInteractionConnectionCallbackReplaceCallbackIfNeeded, this.mFetchFlags | i3, callingPid, j2, compatibleMagnificationSpecLocked, bundle);
                String[] strArrComputeValidReportedPackages = this.mSecurityPolicy.computeValidReportedPackages(callingUid, connectionLocked.getPackageName(), connectionLocked.getUid());
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                return strArrComputeValidReportedPackages;
            } catch (RemoteException e) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                return null;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                throw th;
            }
        }
    }

    public String[] findFocus(int i, long j, int i2, int i3, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, long j2) throws Throwable {
        long j3;
        long j4;
        Region regionObtain = Region.obtain();
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return null;
            }
            int iResolveAccessibilityWindowIdForFindFocusLocked = resolveAccessibilityWindowIdForFindFocusLocked(i, i2);
            if (!this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, iResolveAccessibilityWindowIdForFindFocusLocked)) {
                return null;
            }
            AccessibilityManagerService.RemoteAccessibilityConnection connectionLocked = this.mSystemSupport.getConnectionLocked(iResolveAccessibilityWindowIdForFindFocusLocked);
            if (connectionLocked == null) {
                return null;
            }
            if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(iResolveAccessibilityWindowIdForFindFocusLocked, regionObtain)) {
                regionObtain.recycle();
                regionObtain = null;
            }
            MagnificationSpec compatibleMagnificationSpecLocked = this.mSystemSupport.getCompatibleMagnificationSpecLocked(iResolveAccessibilityWindowIdForFindFocusLocked);
            int callingPid = Binder.getCallingPid();
            IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallbackReplaceCallbackIfNeeded = this.mSystemSupport.replaceCallbackIfNeeded(iAccessibilityInteractionConnectionCallback, iResolveAccessibilityWindowIdForFindFocusLocked, i3, callingPid, j2);
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
            } catch (RemoteException e) {
                j4 = jClearCallingIdentity;
            } catch (Throwable th) {
                th = th;
                j3 = jClearCallingIdentity;
            }
            try {
                connectionLocked.getRemote().findFocus(j, i2, regionObtain, i3, iAccessibilityInteractionConnectionCallbackReplaceCallbackIfNeeded, this.mFetchFlags, callingPid, j2, compatibleMagnificationSpecLocked);
                String[] strArrComputeValidReportedPackages = this.mSecurityPolicy.computeValidReportedPackages(callingUid, connectionLocked.getPackageName(), connectionLocked.getUid());
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                return strArrComputeValidReportedPackages;
            } catch (RemoteException e2) {
                j4 = jClearCallingIdentity;
                Binder.restoreCallingIdentity(j4);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                return null;
            } catch (Throwable th2) {
                th = th2;
                j3 = jClearCallingIdentity;
                Binder.restoreCallingIdentity(j3);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                throw th;
            }
        }
    }

    public String[] focusSearch(int i, long j, int i2, int i3, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, long j2) throws RemoteException {
        Region regionObtain = Region.obtain();
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return null;
            }
            int iResolveAccessibilityWindowIdLocked = resolveAccessibilityWindowIdLocked(i);
            if (!this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, iResolveAccessibilityWindowIdLocked)) {
                return null;
            }
            AccessibilityManagerService.RemoteAccessibilityConnection connectionLocked = this.mSystemSupport.getConnectionLocked(iResolveAccessibilityWindowIdLocked);
            if (connectionLocked == null) {
                return null;
            }
            if (!this.mSecurityPolicy.computePartialInteractiveRegionForWindowLocked(iResolveAccessibilityWindowIdLocked, regionObtain)) {
                regionObtain.recycle();
                regionObtain = null;
            }
            MagnificationSpec compatibleMagnificationSpecLocked = this.mSystemSupport.getCompatibleMagnificationSpecLocked(iResolveAccessibilityWindowIdLocked);
            int callingPid = Binder.getCallingPid();
            IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallbackReplaceCallbackIfNeeded = this.mSystemSupport.replaceCallbackIfNeeded(iAccessibilityInteractionConnectionCallback, iResolveAccessibilityWindowIdLocked, i3, callingPid, j2);
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                connectionLocked.getRemote().focusSearch(j, i2, regionObtain, i3, iAccessibilityInteractionConnectionCallbackReplaceCallbackIfNeeded, this.mFetchFlags, callingPid, j2, compatibleMagnificationSpecLocked);
                String[] strArrComputeValidReportedPackages = this.mSecurityPolicy.computeValidReportedPackages(callingUid, connectionLocked.getPackageName(), connectionLocked.getUid());
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                return strArrComputeValidReportedPackages;
            } catch (RemoteException e) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                return null;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (regionObtain != null && Binder.isProxy(connectionLocked.getRemote())) {
                    regionObtain.recycle();
                }
                throw th;
            }
        }
    }

    public void sendGesture(int i, ParceledListSlice parceledListSlice) {
    }

    public boolean performAccessibilityAction(int i, long j, int i2, Bundle bundle, int i3, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, long j2) throws RemoteException {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return false;
            }
            int iResolveAccessibilityWindowIdLocked = resolveAccessibilityWindowIdLocked(i);
            if (this.mSecurityPolicy.canGetAccessibilityNodeInfoLocked(this, iResolveAccessibilityWindowIdLocked)) {
                return this.mSystemSupport.performAccessibilityAction(iResolveAccessibilityWindowIdLocked, j, i2, bundle, i3, iAccessibilityInteractionConnectionCallback, this.mFetchFlags, j2);
            }
            return false;
        }
    }

    public boolean performGlobalAction(int i) {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return false;
            }
            return this.mGlobalActionPerformer.performGlobalAction(i);
        }
    }

    public boolean isFingerprintGestureDetectionAvailable() {
        FingerprintGestureDispatcher fingerprintGestureDispatcher;
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint") && isCapturingFingerprintGestures() && (fingerprintGestureDispatcher = this.mSystemSupport.getFingerprintGestureDispatcher()) != null && fingerprintGestureDispatcher.isFingerprintGestureDetectionAvailable();
    }

    public float getMagnificationScale() {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return 1.0f;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return this.mSystemSupport.getMagnificationController().getScale();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public Region getMagnificationRegion() {
        synchronized (this.mLock) {
            Region regionObtain = Region.obtain();
            if (!isCalledForCurrentUserLocked()) {
                return regionObtain;
            }
            MagnificationController magnificationController = this.mSystemSupport.getMagnificationController();
            boolean zRegisterMagnificationIfNeeded = registerMagnificationIfNeeded(magnificationController);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                magnificationController.getMagnificationRegion(regionObtain);
                return regionObtain;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (zRegisterMagnificationIfNeeded) {
                    magnificationController.unregister();
                }
            }
        }
    }

    public float getMagnificationCenterX() {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return 0.0f;
            }
            MagnificationController magnificationController = this.mSystemSupport.getMagnificationController();
            boolean zRegisterMagnificationIfNeeded = registerMagnificationIfNeeded(magnificationController);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return magnificationController.getCenterX();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (zRegisterMagnificationIfNeeded) {
                    magnificationController.unregister();
                }
            }
        }
    }

    public float getMagnificationCenterY() {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return 0.0f;
            }
            MagnificationController magnificationController = this.mSystemSupport.getMagnificationController();
            boolean zRegisterMagnificationIfNeeded = registerMagnificationIfNeeded(magnificationController);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return magnificationController.getCenterY();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (zRegisterMagnificationIfNeeded) {
                    magnificationController.unregister();
                }
            }
        }
    }

    private boolean registerMagnificationIfNeeded(MagnificationController magnificationController) {
        if (!magnificationController.isRegisteredLocked() && this.mSecurityPolicy.canControlMagnification(this)) {
            magnificationController.register();
            return true;
        }
        return false;
    }

    public boolean resetMagnification(boolean z) {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return false;
            }
            if (!this.mSecurityPolicy.canControlMagnification(this)) {
                return false;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return this.mSystemSupport.getMagnificationController().reset(z);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public boolean setMagnificationScaleAndCenter(float f, float f2, float f3, boolean z) {
        synchronized (this.mLock) {
            if (!isCalledForCurrentUserLocked()) {
                return false;
            }
            if (!this.mSecurityPolicy.canControlMagnification(this)) {
                return false;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                MagnificationController magnificationController = this.mSystemSupport.getMagnificationController();
                if (!magnificationController.isRegisteredLocked()) {
                    magnificationController.register();
                }
                return magnificationController.setScaleAndCenter(f, f2, f3, z, this.mId);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public void setMagnificationCallbackEnabled(boolean z) {
        this.mInvocationHandler.setMagnificationCallbackEnabled(z);
    }

    public boolean isMagnificationCallbackEnabled() {
        return this.mInvocationHandler.mIsMagnificationCallbackEnabled;
    }

    public void setSoftKeyboardCallbackEnabled(boolean z) {
        this.mInvocationHandler.setSoftKeyboardCallbackEnabled(z);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, LOG_TAG, printWriter)) {
            synchronized (this.mLock) {
                printWriter.append((CharSequence) ("Service[label=" + ((Object) this.mAccessibilityServiceInfo.getResolveInfo().loadLabel(this.mContext.getPackageManager()))));
                printWriter.append((CharSequence) (", feedbackType" + AccessibilityServiceInfo.feedbackTypeToString(this.mFeedbackType)));
                printWriter.append((CharSequence) (", capabilities=" + this.mAccessibilityServiceInfo.getCapabilities()));
                printWriter.append((CharSequence) (", eventTypes=" + AccessibilityEvent.eventTypeToString(this.mEventTypes)));
                printWriter.append((CharSequence) (", notificationTimeout=" + this.mNotificationTimeout));
                printWriter.append("]");
            }
        }
    }

    public void onAdded() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mWindowManagerService.addWindowToken(this.mOverlayWindowToken, 2032, 0);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void onRemoved() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mWindowManagerService.removeWindowToken(this.mOverlayWindowToken, true, 0);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void resetLocked() {
        this.mSystemSupport.getKeyEventDispatcher().flush(this);
        try {
            if (this.mServiceInterface != null) {
                this.mServiceInterface.init((IAccessibilityServiceConnection) null, this.mId, (IBinder) null);
            }
        } catch (RemoteException e) {
        }
        if (this.mService != null) {
            this.mService.unlinkToDeath(this, 0);
            this.mService = null;
        }
        this.mServiceInterface = null;
        this.mReceivedAccessibilityButtonCallbackSinceBind = false;
    }

    public boolean isConnectedLocked() {
        return this.mService != null;
    }

    public void notifyAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        Message messageObtainMessage;
        synchronized (this.mLock) {
            int eventType = accessibilityEvent.getEventType();
            boolean zWantsEventLocked = wantsEventLocked(accessibilityEvent);
            boolean z = this.mUsesAccessibilityCache && (4307005 & eventType) != 0;
            if (zWantsEventLocked || z) {
                AccessibilityEvent accessibilityEventObtain = AccessibilityEvent.obtain(accessibilityEvent);
                if (this.mNotificationTimeout > 0 && eventType != 2048) {
                    AccessibilityEvent accessibilityEvent2 = this.mPendingEvents.get(eventType);
                    this.mPendingEvents.put(eventType, accessibilityEventObtain);
                    if (accessibilityEvent2 != null) {
                        this.mEventDispatchHandler.removeMessages(eventType);
                        accessibilityEvent2.recycle();
                    }
                    messageObtainMessage = this.mEventDispatchHandler.obtainMessage(eventType);
                } else {
                    messageObtainMessage = this.mEventDispatchHandler.obtainMessage(eventType, accessibilityEventObtain);
                }
                messageObtainMessage.arg1 = zWantsEventLocked ? 1 : 0;
                this.mEventDispatchHandler.sendMessageDelayed(messageObtainMessage, this.mNotificationTimeout);
            }
        }
    }

    private boolean wantsEventLocked(AccessibilityEvent accessibilityEvent) {
        if (!canReceiveEventsLocked()) {
            return false;
        }
        if (accessibilityEvent.getWindowId() != -1 && !accessibilityEvent.isImportantForAccessibility() && (this.mFetchFlags & 8) == 0) {
            return false;
        }
        int eventType = accessibilityEvent.getEventType();
        if ((this.mEventTypes & eventType) != eventType) {
            return false;
        }
        Set<String> set = this.mPackageNames;
        return set.isEmpty() || set.contains(accessibilityEvent.getPackageName() != null ? accessibilityEvent.getPackageName().toString() : null);
    }

    private void notifyAccessibilityEventInternal(int i, AccessibilityEvent accessibilityEvent, boolean z) {
        synchronized (this.mLock) {
            IAccessibilityServiceClient iAccessibilityServiceClient = this.mServiceInterface;
            if (iAccessibilityServiceClient == null) {
                return;
            }
            if (accessibilityEvent == null) {
                accessibilityEvent = this.mPendingEvents.get(i);
                if (accessibilityEvent == null) {
                    return;
                } else {
                    this.mPendingEvents.remove(i);
                }
            }
            if (this.mSecurityPolicy.canRetrieveWindowContentLocked(this)) {
                accessibilityEvent.setConnectionId(this.mId);
            } else {
                accessibilityEvent.setSource((View) null);
            }
            accessibilityEvent.setSealed(true);
            try {
                try {
                    iAccessibilityServiceClient.onAccessibilityEvent(accessibilityEvent, z);
                } catch (RemoteException e) {
                    Slog.e(LOG_TAG, "Error during sending " + accessibilityEvent + " to " + iAccessibilityServiceClient, e);
                }
            } finally {
                accessibilityEvent.recycle();
            }
        }
    }

    public void notifyGesture(int i) {
        this.mInvocationHandler.obtainMessage(1, i, 0).sendToTarget();
    }

    public void notifyClearAccessibilityNodeInfoCache() {
        this.mInvocationHandler.sendEmptyMessage(2);
    }

    public void notifyMagnificationChangedLocked(Region region, float f, float f2, float f3) {
        this.mInvocationHandler.notifyMagnificationChangedLocked(region, f, f2, f3);
    }

    public void notifySoftKeyboardShowModeChangedLocked(int i) {
        this.mInvocationHandler.notifySoftKeyboardShowModeChangedLocked(i);
    }

    public void notifyAccessibilityButtonClickedLocked() {
        this.mInvocationHandler.notifyAccessibilityButtonClickedLocked();
    }

    public void notifyAccessibilityButtonAvailabilityChangedLocked(boolean z) {
        this.mInvocationHandler.notifyAccessibilityButtonAvailabilityChangedLocked(z);
    }

    private void notifyMagnificationChangedInternal(Region region, float f, float f2, float f3) {
        IAccessibilityServiceClient serviceInterfaceSafely = getServiceInterfaceSafely();
        if (serviceInterfaceSafely != null) {
            try {
                serviceInterfaceSafely.onMagnificationChanged(region, f, f2, f3);
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Error sending magnification changes to " + this.mService, e);
            }
        }
    }

    private void notifySoftKeyboardShowModeChangedInternal(int i) {
        IAccessibilityServiceClient serviceInterfaceSafely = getServiceInterfaceSafely();
        if (serviceInterfaceSafely != null) {
            try {
                serviceInterfaceSafely.onSoftKeyboardShowModeChanged(i);
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Error sending soft keyboard show mode changes to " + this.mService, e);
            }
        }
    }

    private void notifyAccessibilityButtonClickedInternal() {
        IAccessibilityServiceClient serviceInterfaceSafely = getServiceInterfaceSafely();
        if (serviceInterfaceSafely != null) {
            try {
                serviceInterfaceSafely.onAccessibilityButtonClicked();
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Error sending accessibility button click to " + this.mService, e);
            }
        }
    }

    private void notifyAccessibilityButtonAvailabilityChangedInternal(boolean z) {
        if (this.mReceivedAccessibilityButtonCallbackSinceBind && this.mLastAccessibilityButtonCallbackState == z) {
            return;
        }
        this.mReceivedAccessibilityButtonCallbackSinceBind = true;
        this.mLastAccessibilityButtonCallbackState = z;
        IAccessibilityServiceClient serviceInterfaceSafely = getServiceInterfaceSafely();
        if (serviceInterfaceSafely != null) {
            try {
                serviceInterfaceSafely.onAccessibilityButtonAvailabilityChanged(z);
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Error sending accessibility button availability change to " + this.mService, e);
            }
        }
    }

    private void notifyGestureInternal(int i) {
        IAccessibilityServiceClient serviceInterfaceSafely = getServiceInterfaceSafely();
        if (serviceInterfaceSafely != null) {
            try {
                serviceInterfaceSafely.onGesture(i);
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Error during sending gesture " + i + " to " + this.mService, e);
            }
        }
    }

    private void notifyClearAccessibilityCacheInternal() {
        IAccessibilityServiceClient serviceInterfaceSafely = getServiceInterfaceSafely();
        if (serviceInterfaceSafely != null) {
            try {
                serviceInterfaceSafely.clearAccessibilityCache();
            } catch (RemoteException e) {
                Slog.e(LOG_TAG, "Error during requesting accessibility info cache to be cleared.", e);
            }
        }
    }

    private IAccessibilityServiceClient getServiceInterfaceSafely() {
        IAccessibilityServiceClient iAccessibilityServiceClient;
        synchronized (this.mLock) {
            iAccessibilityServiceClient = this.mServiceInterface;
        }
        return iAccessibilityServiceClient;
    }

    private int resolveAccessibilityWindowIdLocked(int i) {
        if (i == Integer.MAX_VALUE) {
            return this.mSecurityPolicy.getActiveWindowId();
        }
        return i;
    }

    private int resolveAccessibilityWindowIdForFindFocusLocked(int i, int i2) {
        if (i == Integer.MAX_VALUE) {
            return this.mSecurityPolicy.mActiveWindowId;
        }
        if (i == -2) {
            if (i2 == 1) {
                return this.mSecurityPolicy.mFocusedWindowId;
            }
            if (i2 == 2) {
                return this.mSecurityPolicy.mAccessibilityFocusedWindowId;
            }
        }
        return i;
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    private final class InvocationHandler extends Handler {
        public static final int MSG_CLEAR_ACCESSIBILITY_CACHE = 2;
        private static final int MSG_ON_ACCESSIBILITY_BUTTON_AVAILABILITY_CHANGED = 8;
        private static final int MSG_ON_ACCESSIBILITY_BUTTON_CLICKED = 7;
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
            int i = message.what;
            switch (i) {
                case 1:
                    AbstractAccessibilityServiceConnection.this.notifyGestureInternal(message.arg1);
                    return;
                case 2:
                    AbstractAccessibilityServiceConnection.this.notifyClearAccessibilityCacheInternal();
                    return;
                case 3:
                case 4:
                default:
                    throw new IllegalArgumentException("Unknown message: " + i);
                case 5:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    AbstractAccessibilityServiceConnection.this.notifyMagnificationChangedInternal((Region) someArgs.arg1, ((Float) someArgs.arg2).floatValue(), ((Float) someArgs.arg3).floatValue(), ((Float) someArgs.arg4).floatValue());
                    someArgs.recycle();
                    return;
                case 6:
                    AbstractAccessibilityServiceConnection.this.notifySoftKeyboardShowModeChangedInternal(message.arg1);
                    return;
                case 7:
                    AbstractAccessibilityServiceConnection.this.notifyAccessibilityButtonClickedInternal();
                    return;
                case 8:
                    AbstractAccessibilityServiceConnection.this.notifyAccessibilityButtonAvailabilityChangedInternal(message.arg1 != 0);
                    return;
            }
        }

        public void notifyMagnificationChangedLocked(Region region, float f, float f2, float f3) {
            if (!this.mIsMagnificationCallbackEnabled) {
                return;
            }
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = region;
            someArgsObtain.arg2 = Float.valueOf(f);
            someArgsObtain.arg3 = Float.valueOf(f2);
            someArgsObtain.arg4 = Float.valueOf(f3);
            obtainMessage(5, someArgsObtain).sendToTarget();
        }

        public void setMagnificationCallbackEnabled(boolean z) {
            this.mIsMagnificationCallbackEnabled = z;
        }

        public void notifySoftKeyboardShowModeChangedLocked(int i) {
            if (!this.mIsSoftKeyboardCallbackEnabled) {
                return;
            }
            obtainMessage(6, i, 0).sendToTarget();
        }

        public void setSoftKeyboardCallbackEnabled(boolean z) {
            this.mIsSoftKeyboardCallbackEnabled = z;
        }

        public void notifyAccessibilityButtonClickedLocked() {
            obtainMessage(7).sendToTarget();
        }

        public void notifyAccessibilityButtonAvailabilityChangedLocked(boolean z) {
            obtainMessage(8, z ? 1 : 0, 0).sendToTarget();
        }
    }
}
