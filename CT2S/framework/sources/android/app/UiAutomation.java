package android.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.accessibilityservice.IAccessibilityServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.display.DisplayManagerGlobal;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import libcore.io.IoUtils;

public final class UiAutomation {
    private static final int CONNECTION_ID_UNDEFINED = -1;
    private static final long CONNECT_TIMEOUT_MILLIS = 5000;
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = UiAutomation.class.getSimpleName();
    public static final int ROTATION_FREEZE_0 = 0;
    public static final int ROTATION_FREEZE_180 = 2;
    public static final int ROTATION_FREEZE_270 = 3;
    public static final int ROTATION_FREEZE_90 = 1;
    public static final int ROTATION_FREEZE_CURRENT = -1;
    public static final int ROTATION_UNFREEZE = -2;
    private final IAccessibilityServiceClient mClient;
    private boolean mIsConnecting;
    private long mLastEventTimeMillis;
    private OnAccessibilityEventListener mOnAccessibilityEventListener;
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

    public UiAutomation(Looper looper, IUiAutomationConnection connection) {
        if (looper == null) {
            throw new IllegalArgumentException("Looper cannot be null!");
        }
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null!");
        }
        this.mUiAutomationConnection = connection;
        this.mClient = new IAccessibilityServiceClientImpl(looper);
    }

    public void connect() {
        synchronized (this.mLock) {
            throwIfConnectedLocked();
            if (!this.mIsConnecting) {
                this.mIsConnecting = true;
                try {
                    this.mUiAutomationConnection.connect(this.mClient);
                    synchronized (this.mLock) {
                        long startTimeMillis = SystemClock.uptimeMillis();
                        while (!isConnectedLocked()) {
                            try {
                                long elapsedTimeMillis = SystemClock.uptimeMillis() - startTimeMillis;
                                long remainingTimeMillis = 5000 - elapsedTimeMillis;
                                if (remainingTimeMillis <= 0) {
                                    throw new RuntimeException("Error while connecting UiAutomation");
                                }
                                try {
                                    this.mLock.wait(remainingTimeMillis);
                                } catch (InterruptedException e) {
                                }
                            } finally {
                                this.mIsConnecting = false;
                            }
                        }
                    }
                } catch (RemoteException re) {
                    throw new RuntimeException("Error while connecting UiAutomation", re);
                }
            }
        }
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
            this.mUiAutomationConnection.disconnect();
        } catch (RemoteException re) {
            throw new RuntimeException("Error while disconnecting UiAutomation", re);
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

    public void setOnAccessibilityEventListener(OnAccessibilityEventListener listener) {
        synchronized (this.mLock) {
            this.mOnAccessibilityEventListener = listener;
        }
    }

    public final boolean performGlobalAction(int action) {
        IAccessibilityServiceConnection connection;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            connection = AccessibilityInteractionClient.getInstance().getConnection(this.mConnectionId);
        }
        if (connection != null) {
            try {
                return connection.performGlobalAction(action);
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while calling performGlobalAction", re);
            }
        }
        return false;
    }

    public AccessibilityNodeInfo findFocus(int focus) {
        return AccessibilityInteractionClient.getInstance().findFocus(this.mConnectionId, -2, AccessibilityNodeInfo.ROOT_NODE_ID, focus);
    }

    public final AccessibilityServiceInfo getServiceInfo() {
        IAccessibilityServiceConnection connection;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            connection = AccessibilityInteractionClient.getInstance().getConnection(this.mConnectionId);
        }
        if (connection != null) {
            try {
                return connection.getServiceInfo();
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while getting AccessibilityServiceInfo", re);
            }
        }
        return null;
    }

    public final void setServiceInfo(AccessibilityServiceInfo info) {
        IAccessibilityServiceConnection connection;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            AccessibilityInteractionClient.getInstance().clearCache();
            connection = AccessibilityInteractionClient.getInstance().getConnection(this.mConnectionId);
        }
        if (connection != null) {
            try {
                connection.setServiceInfo(info);
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while setting AccessibilityServiceInfo", re);
            }
        }
    }

    public List<AccessibilityWindowInfo> getWindows() {
        int connectionId;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            connectionId = this.mConnectionId;
        }
        return AccessibilityInteractionClient.getInstance().getWindows(connectionId);
    }

    public AccessibilityNodeInfo getRootInActiveWindow() {
        int connectionId;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            connectionId = this.mConnectionId;
        }
        return AccessibilityInteractionClient.getInstance().getRootInActiveWindow(connectionId);
    }

    public boolean injectInputEvent(InputEvent event, boolean sync) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            return this.mUiAutomationConnection.injectInputEvent(event, sync);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error while injecting input event!", re);
            return false;
        }
    }

    public boolean setRotation(int rotation) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        switch (rotation) {
            case -2:
            case -1:
            case 0:
            case 1:
            case 2:
            case 3:
                try {
                    this.mUiAutomationConnection.setRotation(rotation);
                    return true;
                } catch (RemoteException re) {
                    Log.e(LOG_TAG, "Error while setting rotation!", re);
                    return false;
                }
            default:
                throw new IllegalArgumentException("Invalid rotation.");
        }
    }

    public AccessibilityEvent executeAndWaitForEvent(Runnable command, AccessibilityEventFilter filter, long timeoutMillis) throws TimeoutException {
        AccessibilityEvent event;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            this.mEventQueue.clear();
            this.mWaitingForEventDelivery = true;
        }
        long executionStartTimeMillis = SystemClock.uptimeMillis();
        command.run();
        synchronized (this.mLock) {
            try {
                long startTimeMillis = SystemClock.uptimeMillis();
                while (true) {
                    if (!this.mEventQueue.isEmpty()) {
                        event = this.mEventQueue.remove(0);
                        if (event.getEventTime() < executionStartTimeMillis) {
                            continue;
                        } else if (!filter.accept(event)) {
                            event.recycle();
                        }
                    } else {
                        long elapsedTimeMillis = SystemClock.uptimeMillis() - startTimeMillis;
                        long remainingTimeMillis = timeoutMillis - elapsedTimeMillis;
                        if (remainingTimeMillis <= 0) {
                            throw new TimeoutException("Expected event not received within: " + timeoutMillis + " ms.");
                        }
                        try {
                            this.mLock.wait(remainingTimeMillis);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            } finally {
                this.mWaitingForEventDelivery = false;
                this.mEventQueue.clear();
                this.mLock.notifyAll();
            }
        }
        return event;
    }

    public void waitForIdle(long idleTimeoutMillis, long globalTimeoutMillis) throws TimeoutException {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
            long startTimeMillis = SystemClock.uptimeMillis();
            if (this.mLastEventTimeMillis <= 0) {
                this.mLastEventTimeMillis = startTimeMillis;
            }
            while (true) {
                long currentTimeMillis = SystemClock.uptimeMillis();
                long elapsedGlobalTimeMillis = currentTimeMillis - startTimeMillis;
                long remainingGlobalTimeMillis = globalTimeoutMillis - elapsedGlobalTimeMillis;
                if (remainingGlobalTimeMillis <= 0) {
                    throw new TimeoutException("No idle state with idle timeout: " + idleTimeoutMillis + " within global timeout: " + globalTimeoutMillis);
                }
                long elapsedIdleTimeMillis = currentTimeMillis - this.mLastEventTimeMillis;
                long remainingIdleTimeMillis = idleTimeoutMillis - elapsedIdleTimeMillis;
                if (remainingIdleTimeMillis > 0) {
                    try {
                        this.mLock.wait(remainingIdleTimeMillis);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    public Bitmap takeScreenshot() {
        float screenshotWidth;
        float screenshotHeight;
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        Point displaySize = new Point();
        display.getRealSize(displaySize);
        int displayWidth = displaySize.x;
        int displayHeight = displaySize.y;
        int rotation = display.getRotation();
        switch (rotation) {
            case 0:
                screenshotWidth = displayWidth;
                screenshotHeight = displayHeight;
                break;
            case 1:
                screenshotWidth = displayHeight;
                screenshotHeight = displayWidth;
                break;
            case 2:
                screenshotWidth = displayWidth;
                screenshotHeight = displayHeight;
                break;
            case 3:
                screenshotWidth = displayHeight;
                screenshotHeight = displayWidth;
                break;
            default:
                throw new IllegalArgumentException("Invalid rotation: " + rotation);
        }
        try {
            Bitmap screenShot = this.mUiAutomationConnection.takeScreenshot((int) screenshotWidth, (int) screenshotHeight);
            if (screenShot == null) {
                return null;
            }
            if (rotation != 0) {
                Bitmap unrotatedScreenShot = Bitmap.createBitmap(displayWidth, displayHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(unrotatedScreenShot);
                canvas.translate(unrotatedScreenShot.getWidth() / 2, unrotatedScreenShot.getHeight() / 2);
                canvas.rotate(getDegreesForRotation(rotation));
                canvas.translate((-screenshotWidth) / 2.0f, (-screenshotHeight) / 2.0f);
                canvas.drawBitmap(screenShot, 0.0f, 0.0f, (Paint) null);
                canvas.setBitmap(null);
                screenShot.recycle();
                screenShot = unrotatedScreenShot;
            }
            screenShot.setHasAlpha(false);
            return screenShot;
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error while taking screnshot!", re);
            return null;
        }
    }

    public void setRunAsMonkey(boolean enable) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            ActivityManagerNative.getDefault().setUserIsMonkey(enable);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error while setting run as monkey!", re);
        }
    }

    public boolean clearWindowContentFrameStats(int windowId) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            return this.mUiAutomationConnection.clearWindowContentFrameStats(windowId);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error clearing window content frame stats!", re);
            return false;
        }
    }

    public WindowContentFrameStats getWindowContentFrameStats(int windowId) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            return this.mUiAutomationConnection.getWindowContentFrameStats(windowId);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error getting window content frame stats!", re);
            return null;
        }
    }

    public void clearWindowAnimationFrameStats() {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            this.mUiAutomationConnection.clearWindowAnimationFrameStats();
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error clearing window animation frame stats!", re);
        }
    }

    public WindowAnimationFrameStats getWindowAnimationFrameStats() {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        try {
            return this.mUiAutomationConnection.getWindowAnimationFrameStats();
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error getting window animation frame stats!", re);
            return null;
        }
    }

    public ParcelFileDescriptor executeShellCommand(String command) {
        synchronized (this.mLock) {
            throwIfNotConnectedLocked();
        }
        ParcelFileDescriptor source = null;
        ParcelFileDescriptor sink = null;
        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            source = pipe[0];
            sink = pipe[1];
            this.mUiAutomationConnection.executeShellCommand(command, sink);
        } catch (RemoteException re) {
            Log.e(LOG_TAG, "Error executing shell command!", re);
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Error executing shell command!", ioe);
        } finally {
            IoUtils.closeQuietly(sink);
        }
        return source;
    }

    private static float getDegreesForRotation(int value) {
        switch (value) {
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

    private class IAccessibilityServiceClientImpl extends AccessibilityService.IAccessibilityServiceClientWrapper {
        public IAccessibilityServiceClientImpl(Looper looper) {
            super(null, looper, new AccessibilityService.Callbacks() {
                @Override
                public void init(int connectionId, IBinder windowToken) {
                    synchronized (uiAutomation.mLock) {
                        uiAutomation.mConnectionId = connectionId;
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
                public boolean onGesture(int gestureId) {
                    return false;
                }

                @Override
                public void onAccessibilityEvent(AccessibilityEvent event) {
                    synchronized (uiAutomation.mLock) {
                        uiAutomation.mLastEventTimeMillis = event.getEventTime();
                        if (uiAutomation.mWaitingForEventDelivery) {
                            uiAutomation.mEventQueue.add(AccessibilityEvent.obtain(event));
                        }
                        uiAutomation.mLock.notifyAll();
                    }
                    OnAccessibilityEventListener listener = uiAutomation.mOnAccessibilityEventListener;
                    if (listener != null) {
                        listener.onAccessibilityEvent(AccessibilityEvent.obtain(event));
                    }
                }

                @Override
                public boolean onKeyEvent(KeyEvent event) {
                    return false;
                }
            });
        }
    }
}
