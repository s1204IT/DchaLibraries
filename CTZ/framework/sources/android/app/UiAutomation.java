package android.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.accessibilityservice.IAccessibilityServiceConnection;
import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;
import android.view.Display;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.WindowAnimationFrameStats;
import android.view.WindowContentFrameStats;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityInteractionClient;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import com.android.internal.util.function.pooled.PooledLambda;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import libcore.io.IoUtils;

public final class UiAutomation {
    private static final int CONNECTION_ID_UNDEFINED = -1;
    private static final long CONNECT_TIMEOUT_MILLIS = 5000;
    private static final boolean DEBUG = false;
    public static final int FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES = 1;
    private static final String LOG_TAG = UiAutomation.class.getSimpleName();
    public static final int ROTATION_FREEZE_0 = 0;
    public static final int ROTATION_FREEZE_180 = 2;
    public static final int ROTATION_FREEZE_270 = 3;
    public static final int ROTATION_FREEZE_90 = 1;
    public static final int ROTATION_FREEZE_CURRENT = -1;
    public static final int ROTATION_UNFREEZE = -2;
    private IAccessibilityServiceClient mClient;
    private int mFlags;
    private boolean mIsConnecting;
    private boolean mIsDestroyed;
    private long mLastEventTimeMillis;
    private final Handler mLocalCallbackHandler;
    private OnAccessibilityEventListener mOnAccessibilityEventListener;
    private HandlerThread mRemoteCallbackThread;
    private final IUiAutomationConnection mUiAutomationConnection;
    private boolean mWaitingForEventDelivery;
    private final Object mLock = new Object();
    private final ArrayList<AccessibilityEvent> mEventQueue = new ArrayList<>();
    private int mConnectionId = -1;

    public interface AccessibilityEventFilter {
        boolean accept(AccessibilityEvent accessibilityEvent);
    }

    public interface OnAccessibilityEventListener {
        void onAccessibilityEvent(AccessibilityEvent accessibilityEvent);
    }

    public UiAutomation(Looper looper, IUiAutomationConnection iUiAutomationConnection) {
        if (looper == null) {
            throw new IllegalArgumentException("Looper cannot be null!");
        }
        if (iUiAutomationConnection == null) {
            throw new IllegalArgumentException("Connection cannot be null!");
        }
        this.mLocalCallbackHandler = new Handler(looper);
        this.mUiAutomationConnection = iUiAutomationConnection;
    }

    public void connect() {
        connect(0);
    }

    public void connect(int i) {
        synchronized (this.mLock) {
            throwIfConnectedLocked();
            if (this.mIsConnecting) {
                return;
            }
            this.mIsConnecting = true;
            this.mRemoteCallbackThread = new HandlerThread("UiAutomation");
            this.mRemoteCallbackThread.start();
            this.mClient = new IAccessibilityServiceClientImpl(this.mRemoteCallbackThread.getLooper());
            try {
                this.mUiAutomationConnection.connect(this.mClient, i);
                this.mFlags = i;
                synchronized (this.mLock) {
                    long jUptimeMillis = SystemClock.uptimeMillis();
                    while (true) {
                        try {
                            if (!isConnectedLocked()) {
                                long jUptimeMillis2 = 5000 - (SystemClock.uptimeMillis() - jUptimeMillis);
                                if (jUptimeMillis2 <= 0) {
                                    throw new RuntimeException("Error while connecting UiAutomation");
                                }
                                try {
                                    this.mLock.wait(jUptimeMillis2);
                                } catch (InterruptedException e) {
                                }
                            }
                        } finally {
                            this.mIsConnecting = false;
                        }
                    }
                }
            } catch (RemoteException e2) {
                throw new RuntimeException("Error while connecting UiAutomation", e2);
            }
        }
    }

    public int getFlags() {
        return this.mFlags;
    }

    public void disconnect() {
        synchronized (this.mLock) {
            if (this.mIsConnecting) {
                throw new IllegalStateException("Cannot call disconnect() while connecting!");
            }
            throwIfNotConnectedLocked();
            this.mConnectionId = -1;
        }
        try {
            try {
                this.mUiAutomationConnection.disconnect();
            } catch (RemoteException e) {
                throw new RuntimeException("Error while disconnecting UiAutomation", e);
            }
        } finally {
            this.mRemoteCallbackThread.quit();
            this.mRemoteCallbackThread = null;
        }
    }

    public int getConnectionId() {
        int i;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            i = this.mConnectionId;
        }
        return i;
    }

    public boolean isDestroyed() {
        return this.mIsDestroyed;
    }

    public void setOnAccessibilityEventListener(OnAccessibilityEventListener onAccessibilityEventListener) {
        synchronized (this.mLock) {
            this.mOnAccessibilityEventListener = onAccessibilityEventListener;
        }
    }

    public void destroy() {
        disconnect();
        this.mIsDestroyed = true;
    }

    public final boolean performGlobalAction(int i) {
        IAccessibilityServiceConnection connection;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            AccessibilityInteractionClient.getInstance();
            connection = AccessibilityInteractionClient.getConnection(this.mConnectionId);
        }
        if (connection != null) {
            try {
                return connection.performGlobalAction(i);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "Error while calling performGlobalAction", e);
                return false;
            }
        }
        return false;
    }

    public AccessibilityNodeInfo findFocus(int i) {
        return AccessibilityInteractionClient.getInstance().findFocus(this.mConnectionId, -2, AccessibilityNodeInfo.ROOT_NODE_ID, i);
    }

    public final AccessibilityServiceInfo getServiceInfo() {
        IAccessibilityServiceConnection connection;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            AccessibilityInteractionClient.getInstance();
            connection = AccessibilityInteractionClient.getConnection(this.mConnectionId);
        }
        if (connection != null) {
            try {
                return connection.getServiceInfo();
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "Error while getting AccessibilityServiceInfo", e);
                return null;
            }
        }
        return null;
    }

    public final void setServiceInfo(AccessibilityServiceInfo accessibilityServiceInfo) {
        IAccessibilityServiceConnection connection;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            AccessibilityInteractionClient.getInstance().clearCache();
            AccessibilityInteractionClient.getInstance();
            connection = AccessibilityInteractionClient.getConnection(this.mConnectionId);
        }
        if (connection != null) {
            try {
                connection.setServiceInfo(accessibilityServiceInfo);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "Error while setting AccessibilityServiceInfo", e);
            }
        }
    }

    public List<AccessibilityWindowInfo> getWindows() {
        int i;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            i = this.mConnectionId;
        }
        return AccessibilityInteractionClient.getInstance().getWindows(i);
    }

    public AccessibilityNodeInfo getRootInActiveWindow() {
        int i;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            i = this.mConnectionId;
        }
        return AccessibilityInteractionClient.getInstance().getRootInActiveWindow(i);
    }

    public boolean injectInputEvent(InputEvent inputEvent, boolean z) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            return this.mUiAutomationConnection.injectInputEvent(inputEvent, z);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error while injecting input event!", e);
            return false;
        }
    }

    public boolean setRotation(int i) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        switch (i) {
            case -2:
            case -1:
            case 0:
            case 1:
            case 2:
            case 3:
                try {
                    this.mUiAutomationConnection.setRotation(i);
                    return true;
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "Error while setting rotation!", e);
                    return false;
                }
            default:
                throw new IllegalArgumentException("Invalid rotation.");
        }
    }

    public AccessibilityEvent executeAndWaitForEvent(Runnable runnable, AccessibilityEventFilter accessibilityEventFilter, long j) throws TimeoutException {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            this.mEventQueue.clear();
            this.mWaitingForEventDelivery = true;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        runnable.run();
        ArrayList arrayList = new ArrayList();
        try {
            long jUptimeMillis2 = SystemClock.uptimeMillis();
            while (true) {
                ArrayList arrayList2 = new ArrayList();
                synchronized (this.mLock) {
                    arrayList2.addAll(this.mEventQueue);
                    this.mEventQueue.clear();
                }
                while (!arrayList2.isEmpty()) {
                    AccessibilityEvent accessibilityEvent = (AccessibilityEvent) arrayList2.remove(0);
                    if (accessibilityEvent.getEventTime() >= jUptimeMillis) {
                        if (accessibilityEventFilter.accept(accessibilityEvent)) {
                            int size = arrayList.size();
                            for (int i = 0; i < size; i++) {
                                ((AccessibilityEvent) arrayList.get(i)).recycle();
                            }
                            synchronized (this.mLock) {
                                this.mWaitingForEventDelivery = false;
                                this.mEventQueue.clear();
                                this.mLock.notifyAll();
                            }
                            return accessibilityEvent;
                        }
                        arrayList.add(accessibilityEvent);
                    }
                }
                long jUptimeMillis3 = j - (SystemClock.uptimeMillis() - jUptimeMillis2);
                if (jUptimeMillis3 <= 0) {
                    throw new TimeoutException("Expected event not received within: " + j + " ms among: " + arrayList);
                }
                synchronized (this.mLock) {
                    if (this.mEventQueue.isEmpty()) {
                        try {
                            this.mLock.wait(jUptimeMillis3);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        } catch (Throwable th) {
            int size2 = arrayList.size();
            for (int i2 = 0; i2 < size2; i2++) {
                ((AccessibilityEvent) arrayList.get(i2)).recycle();
            }
            synchronized (this.mLock) {
                this.mWaitingForEventDelivery = false;
                this.mEventQueue.clear();
                this.mLock.notifyAll();
                throw th;
            }
        }
    }

    public void waitForIdle(long j, long j2) throws TimeoutException {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (this.mLastEventTimeMillis <= 0) {
                this.mLastEventTimeMillis = jUptimeMillis;
            }
            while (true) {
                long jUptimeMillis2 = SystemClock.uptimeMillis();
                if (j2 - (jUptimeMillis2 - jUptimeMillis) <= 0) {
                    throw new TimeoutException("No idle state with idle timeout: " + j + " within global timeout: " + j2);
                }
                long j3 = j - (jUptimeMillis2 - this.mLastEventTimeMillis);
                if (j3 > 0) {
                    try {
                        this.mLock.wait(j3);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    public Bitmap takeScreenshot() {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        Display realDisplay = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        Point point = new Point();
        realDisplay.getRealSize(point);
        try {
            Bitmap bitmapTakeScreenshot = this.mUiAutomationConnection.takeScreenshot(new Rect(0, 0, point.x, point.y), realDisplay.getRotation());
            if (bitmapTakeScreenshot == null) {
                return null;
            }
            bitmapTakeScreenshot.setHasAlpha(false);
            return bitmapTakeScreenshot;
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error while taking screnshot!", e);
            return null;
        }
    }

    public void setRunAsMonkey(boolean z) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            ActivityManager.getService().setUserIsMonkey(z);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error while setting run as monkey!", e);
        }
    }

    public boolean clearWindowContentFrameStats(int i) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            return this.mUiAutomationConnection.clearWindowContentFrameStats(i);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error clearing window content frame stats!", e);
            return false;
        }
    }

    public WindowContentFrameStats getWindowContentFrameStats(int i) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            return this.mUiAutomationConnection.getWindowContentFrameStats(i);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error getting window content frame stats!", e);
            return null;
        }
    }

    public void clearWindowAnimationFrameStats() {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            this.mUiAutomationConnection.clearWindowAnimationFrameStats();
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error clearing window animation frame stats!", e);
        }
    }

    public WindowAnimationFrameStats getWindowAnimationFrameStats() {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            return this.mUiAutomationConnection.getWindowAnimationFrameStats();
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error getting window animation frame stats!", e);
            return null;
        }
    }

    public void grantRuntimePermission(String str, String str2) {
        grantRuntimePermissionAsUser(str, str2, Process.myUserHandle());
    }

    @Deprecated
    public boolean grantRuntimePermission(String str, String str2, UserHandle userHandle) {
        grantRuntimePermissionAsUser(str, str2, userHandle);
        return true;
    }

    public void grantRuntimePermissionAsUser(String str, String str2, UserHandle userHandle) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            this.mUiAutomationConnection.grantRuntimePermission(str, str2, userHandle.getIdentifier());
        } catch (Exception e) {
            throw new SecurityException("Error granting runtime permission", e);
        }
    }

    public void revokeRuntimePermission(String str, String str2) {
        revokeRuntimePermissionAsUser(str, str2, Process.myUserHandle());
    }

    @Deprecated
    public boolean revokeRuntimePermission(String str, String str2, UserHandle userHandle) {
        revokeRuntimePermissionAsUser(str, str2, userHandle);
        return true;
    }

    public void revokeRuntimePermissionAsUser(String str, String str2, UserHandle userHandle) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            this.mUiAutomationConnection.revokeRuntimePermission(str, str2, userHandle.getIdentifier());
        } catch (Exception e) {
            throw new SecurityException("Error granting runtime permission", e);
        }
    }

    public ParcelFileDescriptor executeShellCommand(String str) throws Throwable {
        ParcelFileDescriptor parcelFileDescriptor;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        warnIfBetterCommand(str);
        ParcelFileDescriptor parcelFileDescriptor2 = null;
        try {
            try {
                ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
                parcelFileDescriptor = parcelFileDescriptorArrCreatePipe[0];
                try {
                    ParcelFileDescriptor parcelFileDescriptor3 = parcelFileDescriptorArrCreatePipe[1];
                    try {
                        this.mUiAutomationConnection.executeShellCommand(str, parcelFileDescriptor3, null);
                        IoUtils.closeQuietly(parcelFileDescriptor3);
                    } catch (RemoteException e) {
                        e = e;
                        parcelFileDescriptor2 = parcelFileDescriptor3;
                        Log.e(LOG_TAG, "Error executing shell command!", e);
                        IoUtils.closeQuietly(parcelFileDescriptor2);
                    } catch (IOException e2) {
                        e = e2;
                        parcelFileDescriptor2 = parcelFileDescriptor3;
                        Log.e(LOG_TAG, "Error executing shell command!", e);
                        IoUtils.closeQuietly(parcelFileDescriptor2);
                    } catch (Throwable th) {
                        th = th;
                        parcelFileDescriptor2 = parcelFileDescriptor3;
                        IoUtils.closeQuietly(parcelFileDescriptor2);
                        throw th;
                    }
                } catch (RemoteException e3) {
                    e = e3;
                } catch (IOException e4) {
                    e = e4;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (RemoteException e5) {
            e = e5;
            parcelFileDescriptor = null;
        } catch (IOException e6) {
            e = e6;
            parcelFileDescriptor = null;
        }
        return parcelFileDescriptor;
    }

    public ParcelFileDescriptor[] executeShellCommandRw(String str) throws Throwable {
        ParcelFileDescriptor parcelFileDescriptor;
        ParcelFileDescriptor parcelFileDescriptor2;
        ParcelFileDescriptor parcelFileDescriptor3;
        ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        warnIfBetterCommand(str);
        ParcelFileDescriptor parcelFileDescriptor4 = null;
        try {
            try {
                try {
                    parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
                    parcelFileDescriptor2 = parcelFileDescriptorArrCreatePipe[0];
                } catch (Throwable th) {
                    th = th;
                    parcelFileDescriptor = null;
                }
            } catch (RemoteException e) {
                e = e;
                parcelFileDescriptor2 = null;
                parcelFileDescriptor3 = null;
            } catch (IOException e2) {
                e = e2;
                parcelFileDescriptor2 = null;
                parcelFileDescriptor3 = null;
            }
            try {
                ParcelFileDescriptor parcelFileDescriptor5 = parcelFileDescriptorArrCreatePipe[1];
                try {
                    ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe2 = ParcelFileDescriptor.createPipe();
                    parcelFileDescriptor = parcelFileDescriptorArrCreatePipe2[0];
                    try {
                        try {
                            parcelFileDescriptor3 = parcelFileDescriptorArrCreatePipe2[1];
                        } catch (Throwable th2) {
                            th = th2;
                            parcelFileDescriptor4 = parcelFileDescriptor5;
                            IoUtils.closeQuietly(parcelFileDescriptor4);
                            IoUtils.closeQuietly(parcelFileDescriptor);
                            throw th;
                        }
                    } catch (RemoteException e3) {
                        e = e3;
                        parcelFileDescriptor3 = null;
                    } catch (IOException e4) {
                        e = e4;
                        parcelFileDescriptor3 = null;
                    }
                    try {
                        this.mUiAutomationConnection.executeShellCommand(str, parcelFileDescriptor5, parcelFileDescriptor);
                        IoUtils.closeQuietly(parcelFileDescriptor5);
                    } catch (RemoteException e5) {
                        e = e5;
                        parcelFileDescriptor4 = parcelFileDescriptor5;
                        Log.e(LOG_TAG, "Error executing shell command!", e);
                        IoUtils.closeQuietly(parcelFileDescriptor4);
                    } catch (IOException e6) {
                        e = e6;
                        parcelFileDescriptor4 = parcelFileDescriptor5;
                        Log.e(LOG_TAG, "Error executing shell command!", e);
                        IoUtils.closeQuietly(parcelFileDescriptor4);
                    }
                } catch (RemoteException e7) {
                    e = e7;
                    parcelFileDescriptor3 = null;
                    parcelFileDescriptor = null;
                } catch (IOException e8) {
                    e = e8;
                    parcelFileDescriptor3 = null;
                    parcelFileDescriptor = null;
                } catch (Throwable th3) {
                    th = th3;
                    parcelFileDescriptor = null;
                }
            } catch (RemoteException e9) {
                e = e9;
                parcelFileDescriptor3 = null;
                parcelFileDescriptor = parcelFileDescriptor3;
                Log.e(LOG_TAG, "Error executing shell command!", e);
                IoUtils.closeQuietly(parcelFileDescriptor4);
                IoUtils.closeQuietly(parcelFileDescriptor);
                return new ParcelFileDescriptor[]{parcelFileDescriptor2, parcelFileDescriptor3};
            } catch (IOException e10) {
                e = e10;
                parcelFileDescriptor3 = null;
                parcelFileDescriptor = parcelFileDescriptor3;
                Log.e(LOG_TAG, "Error executing shell command!", e);
                IoUtils.closeQuietly(parcelFileDescriptor4);
                IoUtils.closeQuietly(parcelFileDescriptor);
                return new ParcelFileDescriptor[]{parcelFileDescriptor2, parcelFileDescriptor3};
            }
            IoUtils.closeQuietly(parcelFileDescriptor);
            return new ParcelFileDescriptor[]{parcelFileDescriptor2, parcelFileDescriptor3};
        } catch (Throwable th4) {
            th = th4;
        }
    }

    private static float getDegreesForRotation(int i) {
        switch (i) {
            case 1:
                return 270.0f;
            case 2:
                return 180.0f;
            case 3:
                return 90.0f;
            default:
                return 0.0f;
        }
    }

    private boolean isConnectedLocked() {
        return this.mConnectionId != -1;
    }

    private void throwIfConnectedLocked() {
        if (this.mConnectionId != -1) {
            throw new IllegalStateException("UiAutomation not connected!");
        }
    }

    private void throwIfNotConnectedLocked() {
        if (!isConnectedLocked()) {
            throw new IllegalStateException("UiAutomation not connected!");
        }
    }

    private void warnIfBetterCommand(String str) {
        if (str.startsWith("pm grant ")) {
            Log.w(LOG_TAG, "UiAutomation.grantRuntimePermission() is more robust and should be used instead of 'pm grant'");
        } else if (str.startsWith("pm revoke ")) {
            Log.w(LOG_TAG, "UiAutomation.revokeRuntimePermission() is more robust and should be used instead of 'pm revoke'");
        }
    }

    private class IAccessibilityServiceClientImpl extends AccessibilityService.IAccessibilityServiceClientWrapper {
        public IAccessibilityServiceClientImpl(Looper looper) {
            super(null, looper, new AccessibilityService.Callbacks() {
                @Override
                public void init(int i, IBinder iBinder) {
                    synchronized (uiAutomation.mLock) {
                        uiAutomation.mConnectionId = i;
                        uiAutomation.mLock.notifyAll();
                    }
                }

                @Override
                public void onServiceConnected() {
                }

                @Override
                public void onInterrupt() {
                }

                @Override
                public boolean onGesture(int i) {
                    return false;
                }

                @Override
                public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
                    OnAccessibilityEventListener onAccessibilityEventListener;
                    synchronized (uiAutomation.mLock) {
                        uiAutomation.mLastEventTimeMillis = accessibilityEvent.getEventTime();
                        if (uiAutomation.mWaitingForEventDelivery) {
                            uiAutomation.mEventQueue.add(AccessibilityEvent.obtain(accessibilityEvent));
                        }
                        uiAutomation.mLock.notifyAll();
                        onAccessibilityEventListener = uiAutomation.mOnAccessibilityEventListener;
                    }
                    if (onAccessibilityEventListener != null) {
                        uiAutomation.mLocalCallbackHandler.post(PooledLambda.obtainRunnable(new BiConsumer() {
                            @Override
                            public final void accept(Object obj, Object obj2) {
                                ((UiAutomation.OnAccessibilityEventListener) obj).onAccessibilityEvent((AccessibilityEvent) obj2);
                            }
                        }, onAccessibilityEventListener, AccessibilityEvent.obtain(accessibilityEvent)).recycleOnUse());
                    }
                }

                @Override
                public boolean onKeyEvent(KeyEvent keyEvent) {
                    return false;
                }

                @Override
                public void onMagnificationChanged(Region region, float f, float f2, float f3) {
                }

                @Override
                public void onSoftKeyboardShowModeChanged(int i) {
                }

                @Override
                public void onPerformGestureResult(int i, boolean z) {
                }

                @Override
                public void onFingerprintCapturingGesturesChanged(boolean z) {
                }

                @Override
                public void onFingerprintGesture(int i) {
                }

                @Override
                public void onAccessibilityButtonClicked() {
                }

                @Override
                public void onAccessibilityButtonAvailabilityChanged(boolean z) {
                }
            });
        }
    }
}
