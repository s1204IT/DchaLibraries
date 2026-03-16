package android.accessibilityservice;

import android.accessibilityservice.IAccessibilityServiceClient;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityInteractionClient;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.SomeArgs;
import java.util.List;

public abstract class AccessibilityService extends Service {
    public static final int GESTURE_SWIPE_DOWN = 2;
    public static final int GESTURE_SWIPE_DOWN_AND_LEFT = 15;
    public static final int GESTURE_SWIPE_DOWN_AND_RIGHT = 16;
    public static final int GESTURE_SWIPE_DOWN_AND_UP = 8;
    public static final int GESTURE_SWIPE_LEFT = 3;
    public static final int GESTURE_SWIPE_LEFT_AND_DOWN = 10;
    public static final int GESTURE_SWIPE_LEFT_AND_RIGHT = 5;
    public static final int GESTURE_SWIPE_LEFT_AND_UP = 9;
    public static final int GESTURE_SWIPE_RIGHT = 4;
    public static final int GESTURE_SWIPE_RIGHT_AND_DOWN = 12;
    public static final int GESTURE_SWIPE_RIGHT_AND_LEFT = 6;
    public static final int GESTURE_SWIPE_RIGHT_AND_UP = 11;
    public static final int GESTURE_SWIPE_UP = 1;
    public static final int GESTURE_SWIPE_UP_AND_DOWN = 7;
    public static final int GESTURE_SWIPE_UP_AND_LEFT = 13;
    public static final int GESTURE_SWIPE_UP_AND_RIGHT = 14;
    public static final int GLOBAL_ACTION_BACK = 1;
    public static final int GLOBAL_ACTION_HOME = 2;
    public static final int GLOBAL_ACTION_NOTIFICATIONS = 4;
    public static final int GLOBAL_ACTION_POWER_DIALOG = 6;
    public static final int GLOBAL_ACTION_QUICK_SETTINGS = 5;
    public static final int GLOBAL_ACTION_RECENTS = 3;
    private static final String LOG_TAG = "AccessibilityService";
    public static final String SERVICE_INTERFACE = "android.accessibilityservice.AccessibilityService";
    public static final String SERVICE_META_DATA = "android.accessibilityservice";
    private int mConnectionId;
    private AccessibilityServiceInfo mInfo;
    private WindowManager mWindowManager;
    private IBinder mWindowToken;

    public interface Callbacks {
        void init(int i, IBinder iBinder);

        void onAccessibilityEvent(AccessibilityEvent accessibilityEvent);

        boolean onGesture(int i);

        void onInterrupt();

        boolean onKeyEvent(KeyEvent keyEvent);

        void onServiceConnected();
    }

    public abstract void onAccessibilityEvent(AccessibilityEvent accessibilityEvent);

    public abstract void onInterrupt();

    protected void onServiceConnected() {
    }

    protected boolean onGesture(int gestureId) {
        return false;
    }

    protected boolean onKeyEvent(KeyEvent event) {
        return false;
    }

    public List<AccessibilityWindowInfo> getWindows() {
        return AccessibilityInteractionClient.getInstance().getWindows(this.mConnectionId);
    }

    public AccessibilityNodeInfo getRootInActiveWindow() {
        return AccessibilityInteractionClient.getInstance().getRootInActiveWindow(this.mConnectionId);
    }

    public final boolean performGlobalAction(int action) {
        IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mConnectionId);
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
        IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mConnectionId);
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
        this.mInfo = info;
        sendServiceInfo();
    }

    private void sendServiceInfo() {
        IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mConnectionId);
        if (this.mInfo != null && connection != null) {
            try {
                connection.setServiceInfo(this.mInfo);
                this.mInfo = null;
                AccessibilityInteractionClient.getInstance().clearCache();
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while setting AccessibilityServiceInfo", re);
            }
        }
    }

    @Override
    public Object getSystemService(String name) {
        if (getBaseContext() == null) {
            throw new IllegalStateException("System services not available to Activities before onCreate()");
        }
        if (!Context.WINDOW_SERVICE.equals(name)) {
            return super.getSystemService(name);
        }
        if (this.mWindowManager == null) {
            this.mWindowManager = (WindowManager) getBaseContext().getSystemService(name);
        }
        return this.mWindowManager;
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return new IAccessibilityServiceClientWrapper(this, getMainLooper(), new Callbacks() {
            @Override
            public void onServiceConnected() {
                AccessibilityService.this.onServiceConnected();
            }

            @Override
            public void onInterrupt() {
                AccessibilityService.this.onInterrupt();
            }

            @Override
            public void onAccessibilityEvent(AccessibilityEvent event) {
                AccessibilityService.this.onAccessibilityEvent(event);
            }

            @Override
            public void init(int connectionId, IBinder windowToken) {
                AccessibilityService.this.mConnectionId = connectionId;
                AccessibilityService.this.mWindowToken = windowToken;
                WindowManagerImpl wm = (WindowManagerImpl) AccessibilityService.this.getSystemService(Context.WINDOW_SERVICE);
                wm.setDefaultToken(windowToken);
            }

            @Override
            public boolean onGesture(int gestureId) {
                return AccessibilityService.this.onGesture(gestureId);
            }

            @Override
            public boolean onKeyEvent(KeyEvent event) {
                return AccessibilityService.this.onKeyEvent(event);
            }
        });
    }

    public static class IAccessibilityServiceClientWrapper extends IAccessibilityServiceClient.Stub implements HandlerCaller.Callback {
        private static final int DO_CLEAR_ACCESSIBILITY_CACHE = 5;
        private static final int DO_INIT = 1;
        private static final int DO_ON_ACCESSIBILITY_EVENT = 3;
        private static final int DO_ON_GESTURE = 4;
        private static final int DO_ON_INTERRUPT = 2;
        private static final int DO_ON_KEY_EVENT = 6;
        private final Callbacks mCallback;
        private final HandlerCaller mCaller;
        private int mConnectionId;

        public IAccessibilityServiceClientWrapper(Context context, Looper looper, Callbacks callback) {
            this.mCallback = callback;
            this.mCaller = new HandlerCaller(context, looper, this, true);
        }

        @Override
        public void init(IAccessibilityServiceConnection connection, int connectionId, IBinder windowToken) {
            Message message = this.mCaller.obtainMessageIOO(1, connectionId, connection, windowToken);
            this.mCaller.sendMessage(message);
        }

        @Override
        public void onInterrupt() {
            Message message = this.mCaller.obtainMessage(2);
            this.mCaller.sendMessage(message);
        }

        @Override
        public void onAccessibilityEvent(AccessibilityEvent event) {
            Message message = this.mCaller.obtainMessageO(3, event);
            this.mCaller.sendMessage(message);
        }

        @Override
        public void onGesture(int gestureId) {
            Message message = this.mCaller.obtainMessageI(4, gestureId);
            this.mCaller.sendMessage(message);
        }

        @Override
        public void clearAccessibilityCache() {
            Message message = this.mCaller.obtainMessage(5);
            this.mCaller.sendMessage(message);
        }

        @Override
        public void onKeyEvent(KeyEvent event, int sequence) {
            Message message = this.mCaller.obtainMessageIO(6, sequence, event);
            this.mCaller.sendMessage(message);
        }

        @Override
        public void executeMessage(Message message) {
            switch (message.what) {
                case 1:
                    this.mConnectionId = message.arg1;
                    SomeArgs args = (SomeArgs) message.obj;
                    IAccessibilityServiceConnection connection = (IAccessibilityServiceConnection) args.arg1;
                    IBinder windowToken = (IBinder) args.arg2;
                    args.recycle();
                    if (connection != null) {
                        AccessibilityInteractionClient.getInstance().addConnection(this.mConnectionId, connection);
                        this.mCallback.init(this.mConnectionId, windowToken);
                        this.mCallback.onServiceConnected();
                        return;
                    } else {
                        AccessibilityInteractionClient.getInstance().removeConnection(this.mConnectionId);
                        this.mConnectionId = -1;
                        AccessibilityInteractionClient.getInstance().clearCache();
                        this.mCallback.init(-1, null);
                        return;
                    }
                case 2:
                    this.mCallback.onInterrupt();
                    return;
                case 3:
                    AccessibilityEvent event = (AccessibilityEvent) message.obj;
                    if (event != null) {
                        AccessibilityInteractionClient.getInstance().onAccessibilityEvent(event);
                        this.mCallback.onAccessibilityEvent(event);
                        try {
                            event.recycle();
                            return;
                        } catch (IllegalStateException e) {
                            return;
                        }
                    }
                    return;
                case 4:
                    int gestureId = message.arg1;
                    this.mCallback.onGesture(gestureId);
                    return;
                case 5:
                    AccessibilityInteractionClient.getInstance().clearCache();
                    return;
                case 6:
                    KeyEvent event2 = (KeyEvent) message.obj;
                    try {
                        IAccessibilityServiceConnection connection2 = AccessibilityInteractionClient.getInstance().getConnection(this.mConnectionId);
                        if (connection2 != null) {
                            boolean result = this.mCallback.onKeyEvent(event2);
                            int sequence = message.arg1;
                            try {
                                connection2.setOnKeyEventResult(result, sequence);
                                break;
                            } catch (RemoteException e2) {
                            }
                        }
                        try {
                            event2.recycle();
                            return;
                        } catch (IllegalStateException e3) {
                            return;
                        }
                    } catch (Throwable th) {
                        try {
                            event2.recycle();
                            break;
                        } catch (IllegalStateException e4) {
                        }
                        throw th;
                    }
                default:
                    Log.w(AccessibilityService.LOG_TAG, "Unknown message type " + message.what);
                    return;
            }
        }
    }
}
