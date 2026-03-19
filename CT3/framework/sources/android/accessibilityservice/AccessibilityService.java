package android.accessibilityservice;

import android.accessibilityservice.GestureDescription;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.graphics.Region;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
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
    public static final int GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN = 7;
    private static final boolean IS_ENG_BUILD = "eng".equals(Build.TYPE);
    private static final String LOG_TAG = "AccessibilityService";
    public static final String SERVICE_INTERFACE = "android.accessibilityservice.AccessibilityService";
    public static final String SERVICE_META_DATA = "android.accessibilityservice";
    public static final int SHOW_MODE_AUTO = 0;
    public static final int SHOW_MODE_HIDDEN = 1;
    private int mConnectionId;
    private SparseArray<GestureResultCallbackInfo> mGestureStatusCallbackInfos;
    private int mGestureStatusCallbackSequence;
    private AccessibilityServiceInfo mInfo;
    private final Object mLock = new Object();
    private MagnificationController mMagnificationController;
    private SoftKeyboardController mSoftKeyboardController;
    private WindowManager mWindowManager;
    private IBinder mWindowToken;

    public interface Callbacks {
        void init(int i, IBinder iBinder);

        void onAccessibilityEvent(AccessibilityEvent accessibilityEvent);

        boolean onGesture(int i);

        void onInterrupt();

        boolean onKeyEvent(KeyEvent keyEvent);

        void onMagnificationChanged(Region region, float f, float f2, float f3);

        void onPerformGestureResult(int i, boolean z);

        void onServiceConnected();

        void onSoftKeyboardShowModeChanged(int i);
    }

    public abstract void onAccessibilityEvent(AccessibilityEvent accessibilityEvent);

    public abstract void onInterrupt();

    private void dispatchServiceConnected() {
        if (this.mMagnificationController != null) {
            this.mMagnificationController.onServiceConnected();
        }
        onServiceConnected();
    }

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

    public final void disableSelf() {
        IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mConnectionId);
        if (connection == null) {
            return;
        }
        try {
            connection.disableSelf();
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        }
    }

    public final MagnificationController getMagnificationController() {
        MagnificationController magnificationController;
        synchronized (this.mLock) {
            if (this.mMagnificationController == null) {
                this.mMagnificationController = new MagnificationController(this, this.mLock);
            }
            magnificationController = this.mMagnificationController;
        }
        return magnificationController;
    }

    public final boolean dispatchGesture(GestureDescription gesture, GestureResultCallback callback, Handler handler) {
        IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mConnectionId);
        if (connection == null) {
            return false;
        }
        List<GestureDescription.GestureStep> steps = GestureDescription.MotionEventGenerator.getGestureStepsFromGestureDescription(gesture, 100);
        try {
            synchronized (this.mLock) {
                this.mGestureStatusCallbackSequence++;
                if (callback != null) {
                    if (this.mGestureStatusCallbackInfos == null) {
                        this.mGestureStatusCallbackInfos = new SparseArray<>();
                    }
                    GestureResultCallbackInfo callbackInfo = new GestureResultCallbackInfo(gesture, callback, handler);
                    this.mGestureStatusCallbackInfos.put(this.mGestureStatusCallbackSequence, callbackInfo);
                }
                connection.sendGesture(this.mGestureStatusCallbackSequence, new ParceledListSlice(steps));
            }
            return true;
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        }
    }

    void onPerformGestureResult(int sequence, final boolean completedSuccessfully) {
        final GestureResultCallbackInfo callbackInfo;
        if (this.mGestureStatusCallbackInfos == null) {
            return;
        }
        synchronized (this.mLock) {
            callbackInfo = this.mGestureStatusCallbackInfos.get(sequence);
        }
        if (callbackInfo == null || callbackInfo.gestureDescription == null || callbackInfo.callback == null) {
            return;
        }
        if (callbackInfo.handler != null) {
            callbackInfo.handler.post(new Runnable() {
                @Override
                public void run() {
                    if (completedSuccessfully) {
                        callbackInfo.callback.onCompleted(callbackInfo.gestureDescription);
                    } else {
                        callbackInfo.callback.onCancelled(callbackInfo.gestureDescription);
                    }
                }
            });
        } else if (completedSuccessfully) {
            callbackInfo.callback.onCompleted(callbackInfo.gestureDescription);
        } else {
            callbackInfo.callback.onCancelled(callbackInfo.gestureDescription);
        }
    }

    private void onMagnificationChanged(Region region, float scale, float centerX, float centerY) {
        if (this.mMagnificationController == null) {
            return;
        }
        this.mMagnificationController.dispatchMagnificationChanged(region, scale, centerX, centerY);
    }

    public static final class MagnificationController {
        private ArrayMap<OnMagnificationChangedListener, Handler> mListeners;
        private final Object mLock;
        private final AccessibilityService mService;

        public interface OnMagnificationChangedListener {
            void onMagnificationChanged(MagnificationController magnificationController, Region region, float f, float f2, float f3);
        }

        MagnificationController(AccessibilityService service, Object lock) {
            this.mService = service;
            this.mLock = lock;
        }

        void onServiceConnected() {
            synchronized (this.mLock) {
                if (this.mListeners != null && !this.mListeners.isEmpty()) {
                    setMagnificationCallbackEnabled(true);
                }
            }
        }

        public void addListener(OnMagnificationChangedListener listener) {
            addListener(listener, null);
        }

        public void addListener(OnMagnificationChangedListener listener, Handler handler) {
            synchronized (this.mLock) {
                if (this.mListeners == null) {
                    this.mListeners = new ArrayMap<>();
                }
                boolean shouldEnableCallback = this.mListeners.isEmpty();
                this.mListeners.put(listener, handler);
                if (shouldEnableCallback) {
                    setMagnificationCallbackEnabled(true);
                }
            }
        }

        public boolean removeListener(OnMagnificationChangedListener listener) {
            boolean hasKey;
            if (this.mListeners == null) {
                return false;
            }
            synchronized (this.mLock) {
                int keyIndex = this.mListeners.indexOfKey(listener);
                hasKey = keyIndex >= 0;
                if (hasKey) {
                    this.mListeners.removeAt(keyIndex);
                }
                if (hasKey && this.mListeners.isEmpty()) {
                    setMagnificationCallbackEnabled(false);
                }
            }
            return hasKey;
        }

        private void setMagnificationCallbackEnabled(boolean enabled) {
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mService.mConnectionId);
            if (connection == null) {
                return;
            }
            try {
                connection.setMagnificationCallbackEnabled(enabled);
            } catch (RemoteException re) {
                throw new RuntimeException(re);
            }
        }

        void dispatchMagnificationChanged(final Region region, final float scale, final float centerX, final float centerY) {
            synchronized (this.mLock) {
                if (this.mListeners == null || this.mListeners.isEmpty()) {
                    Slog.d(AccessibilityService.LOG_TAG, "Received magnification changed callback with no listeners registered!");
                    setMagnificationCallbackEnabled(false);
                    return;
                }
                ArrayMap<OnMagnificationChangedListener, Handler> entries = new ArrayMap<>(this.mListeners);
                int count = entries.size();
                for (int i = 0; i < count; i++) {
                    final OnMagnificationChangedListener listener = entries.keyAt(i);
                    Handler handler = entries.valueAt(i);
                    if (handler != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onMagnificationChanged(MagnificationController.this, region, scale, centerX, centerY);
                            }
                        });
                    } else {
                        listener.onMagnificationChanged(this, region, scale, centerX, centerY);
                    }
                }
            }
        }

        public float getScale() {
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getMagnificationScale();
                } catch (RemoteException re) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to obtain scale", re);
                    re.rethrowFromSystemServer();
                    return 1.0f;
                }
            }
            return 1.0f;
        }

        public float getCenterX() {
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getMagnificationCenterX();
                } catch (RemoteException re) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to obtain center X", re);
                    re.rethrowFromSystemServer();
                    return 0.0f;
                }
            }
            return 0.0f;
        }

        public float getCenterY() {
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getMagnificationCenterY();
                } catch (RemoteException re) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to obtain center Y", re);
                    re.rethrowFromSystemServer();
                    return 0.0f;
                }
            }
            return 0.0f;
        }

        public Region getMagnificationRegion() {
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getMagnificationRegion();
                } catch (RemoteException re) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to obtain magnified region", re);
                    re.rethrowFromSystemServer();
                }
            }
            return Region.obtain();
        }

        public boolean reset(boolean animate) {
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.resetMagnification(animate);
                } catch (RemoteException re) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to reset", re);
                    re.rethrowFromSystemServer();
                    return false;
                }
            }
            return false;
        }

        public boolean setScale(float scale, boolean animate) {
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.setMagnificationScaleAndCenter(scale, Float.NaN, Float.NaN, animate);
                } catch (RemoteException re) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to set scale", re);
                    re.rethrowFromSystemServer();
                    return false;
                }
            }
            return false;
        }

        public boolean setCenter(float centerX, float centerY, boolean animate) {
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.setMagnificationScaleAndCenter(Float.NaN, centerX, centerY, animate);
                } catch (RemoteException re) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to set center", re);
                    re.rethrowFromSystemServer();
                    return false;
                }
            }
            return false;
        }
    }

    public final SoftKeyboardController getSoftKeyboardController() {
        SoftKeyboardController softKeyboardController;
        synchronized (this.mLock) {
            if (this.mSoftKeyboardController == null) {
                this.mSoftKeyboardController = new SoftKeyboardController(this, this.mLock);
            }
            softKeyboardController = this.mSoftKeyboardController;
        }
        return softKeyboardController;
    }

    private void onSoftKeyboardShowModeChanged(int showMode) {
        if (this.mSoftKeyboardController == null) {
            return;
        }
        this.mSoftKeyboardController.dispatchSoftKeyboardShowModeChanged(showMode);
    }

    public static final class SoftKeyboardController {
        private ArrayMap<OnShowModeChangedListener, Handler> mListeners;
        private final Object mLock;
        private final AccessibilityService mService;

        public interface OnShowModeChangedListener {
            void onShowModeChanged(SoftKeyboardController softKeyboardController, int i);
        }

        SoftKeyboardController(AccessibilityService service, Object lock) {
            this.mService = service;
            this.mLock = lock;
        }

        void onServiceConnected() {
            synchronized (this.mLock) {
                if (this.mListeners != null && !this.mListeners.isEmpty()) {
                    setSoftKeyboardCallbackEnabled(true);
                }
            }
        }

        public void addOnShowModeChangedListener(OnShowModeChangedListener listener) {
            addOnShowModeChangedListener(listener, null);
        }

        public void addOnShowModeChangedListener(OnShowModeChangedListener listener, Handler handler) {
            synchronized (this.mLock) {
                if (this.mListeners == null) {
                    this.mListeners = new ArrayMap<>();
                }
                boolean shouldEnableCallback = this.mListeners.isEmpty();
                this.mListeners.put(listener, handler);
                if (shouldEnableCallback) {
                    setSoftKeyboardCallbackEnabled(true);
                }
            }
        }

        public boolean removeOnShowModeChangedListener(OnShowModeChangedListener listener) {
            boolean hasKey;
            if (this.mListeners == null) {
                return false;
            }
            synchronized (this.mLock) {
                int keyIndex = this.mListeners.indexOfKey(listener);
                hasKey = keyIndex >= 0;
                if (hasKey) {
                    this.mListeners.removeAt(keyIndex);
                }
                if (hasKey && this.mListeners.isEmpty()) {
                    setSoftKeyboardCallbackEnabled(false);
                }
            }
            return hasKey;
        }

        private void setSoftKeyboardCallbackEnabled(boolean enabled) {
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mService.mConnectionId);
            if (connection == null) {
                return;
            }
            try {
                connection.setSoftKeyboardCallbackEnabled(enabled);
            } catch (RemoteException re) {
                throw new RuntimeException(re);
            }
        }

        void dispatchSoftKeyboardShowModeChanged(final int showMode) {
            synchronized (this.mLock) {
                if (this.mListeners == null || this.mListeners.isEmpty()) {
                    Slog.d(AccessibilityService.LOG_TAG, "Received soft keyboard show mode changed callback with no listeners registered!");
                    setSoftKeyboardCallbackEnabled(false);
                    return;
                }
                ArrayMap<OnShowModeChangedListener, Handler> entries = new ArrayMap<>(this.mListeners);
                int count = entries.size();
                for (int i = 0; i < count; i++) {
                    final OnShowModeChangedListener listener = entries.keyAt(i);
                    Handler handler = entries.valueAt(i);
                    if (handler != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onShowModeChanged(SoftKeyboardController.this, showMode);
                            }
                        });
                    } else {
                        listener.onShowModeChanged(this, showMode);
                    }
                }
            }
        }

        public int getShowMode() {
            try {
                return Settings.Secure.getInt(this.mService.getContentResolver(), Settings.Secure.ACCESSIBILITY_SOFT_KEYBOARD_MODE);
            } catch (Settings.SettingNotFoundException e) {
                Log.v(AccessibilityService.LOG_TAG, "Failed to obtain the soft keyboard mode", e);
                return 0;
            }
        }

        public boolean setShowMode(int showMode) {
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.setSoftKeyboardShowMode(showMode);
                } catch (RemoteException re) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to set soft keyboard behavior", re);
                    re.rethrowFromSystemServer();
                    return false;
                }
            }
            return false;
        }
    }

    public final boolean performGlobalAction(int action) {
        IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getInstance().getConnection(this.mConnectionId);
        if (connection != null) {
            try {
                return connection.performGlobalAction(action);
            } catch (RemoteException re) {
                Log.w(LOG_TAG, "Error while calling performGlobalAction", re);
                re.rethrowFromSystemServer();
                return false;
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
                re.rethrowFromSystemServer();
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
        if (this.mInfo == null || connection == null) {
            return;
        }
        try {
            connection.setServiceInfo(this.mInfo);
            this.mInfo = null;
            AccessibilityInteractionClient.getInstance().clearCache();
        } catch (RemoteException re) {
            Log.w(LOG_TAG, "Error while setting AccessibilityServiceInfo", re);
            re.rethrowFromSystemServer();
        }
    }

    @Override
    public Object getSystemService(String name) {
        if (getBaseContext() == null) {
            throw new IllegalStateException("System services not available to Activities before onCreate()");
        }
        if (Context.WINDOW_SERVICE.equals(name)) {
            if (this.mWindowManager == null) {
                this.mWindowManager = (WindowManager) getBaseContext().getSystemService(name);
            }
            return this.mWindowManager;
        }
        return super.getSystemService(name);
    }

    @Override
    public final IBinder onBind(Intent intent) {
        return new IAccessibilityServiceClientWrapper(this, getMainLooper(), new Callbacks() {
            @Override
            public void onServiceConnected() {
                AccessibilityService.this.dispatchServiceConnected();
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

            @Override
            public void onMagnificationChanged(Region region, float scale, float centerX, float centerY) {
                AccessibilityService.this.onMagnificationChanged(region, scale, centerX, centerY);
            }

            @Override
            public void onSoftKeyboardShowModeChanged(int showMode) {
                AccessibilityService.this.onSoftKeyboardShowModeChanged(showMode);
            }

            @Override
            public void onPerformGestureResult(int sequence, boolean completedSuccessfully) {
                AccessibilityService.this.onPerformGestureResult(sequence, completedSuccessfully);
            }
        });
    }

    public static class IAccessibilityServiceClientWrapper extends IAccessibilityServiceClient.Stub implements HandlerCaller.Callback {
        private static final int DO_CLEAR_ACCESSIBILITY_CACHE = 5;
        private static final int DO_GESTURE_COMPLETE = 9;
        private static final int DO_INIT = 1;
        private static final int DO_ON_ACCESSIBILITY_EVENT = 3;
        private static final int DO_ON_GESTURE = 4;
        private static final int DO_ON_INTERRUPT = 2;
        private static final int DO_ON_KEY_EVENT = 6;
        private static final int DO_ON_MAGNIFICATION_CHANGED = 7;
        private static final int DO_ON_SOFT_KEYBOARD_SHOW_MODE_CHANGED = 8;
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
        public void onMagnificationChanged(Region region, float scale, float centerX, float centerY) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = region;
            args.arg2 = Float.valueOf(scale);
            args.arg3 = Float.valueOf(centerX);
            args.arg4 = Float.valueOf(centerY);
            Message message = this.mCaller.obtainMessageO(7, args);
            this.mCaller.sendMessage(message);
        }

        @Override
        public void onSoftKeyboardShowModeChanged(int showMode) {
            Message message = this.mCaller.obtainMessageI(8, showMode);
            this.mCaller.sendMessage(message);
        }

        @Override
        public void onPerformGestureResult(int sequence, boolean successfully) {
            Message message = this.mCaller.obtainMessageII(9, sequence, successfully ? 1 : 0);
            this.mCaller.sendMessage(message);
        }

        public void executeMessage(Message message) {
            switch (message.what) {
                case 1:
                    this.mConnectionId = message.arg1;
                    SomeArgs args = (SomeArgs) message.obj;
                    IAccessibilityServiceConnection connection = (IAccessibilityServiceConnection) args.arg1;
                    IBinder windowToken = (IBinder) args.arg2;
                    args.recycle();
                    if (AccessibilityService.IS_ENG_BUILD) {
                        Log.d(AccessibilityService.LOG_TAG, "DO_INIT");
                    }
                    if (connection != null) {
                        AccessibilityInteractionClient.getInstance().addConnection(this.mConnectionId, connection);
                        this.mCallback.init(this.mConnectionId, windowToken);
                        this.mCallback.onServiceConnected();
                        if (AccessibilityService.IS_ENG_BUILD) {
                            Log.d(AccessibilityService.LOG_TAG, "DO_INIT: mConnectionId=" + this.mConnectionId);
                            return;
                        }
                        return;
                    }
                    AccessibilityInteractionClient.getInstance().removeConnection(this.mConnectionId);
                    this.mConnectionId = -1;
                    AccessibilityInteractionClient.getInstance().clearCache();
                    this.mCallback.init(-1, null);
                    return;
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
                            return;
                        } catch (IllegalStateException e3) {
                            return;
                        }
                    } finally {
                        try {
                            event2.recycle();
                            break;
                        } catch (IllegalStateException e4) {
                        }
                    }
                case 7:
                    SomeArgs args2 = (SomeArgs) message.obj;
                    Region region = (Region) args2.arg1;
                    float scale = ((Float) args2.arg2).floatValue();
                    float centerX = ((Float) args2.arg3).floatValue();
                    float centerY = ((Float) args2.arg4).floatValue();
                    this.mCallback.onMagnificationChanged(region, scale, centerX, centerY);
                    return;
                case 8:
                    int showMode = message.arg1;
                    this.mCallback.onSoftKeyboardShowModeChanged(showMode);
                    return;
                case 9:
                    boolean successfully = message.arg2 == 1;
                    this.mCallback.onPerformGestureResult(message.arg1, successfully);
                    return;
                default:
                    Log.w(AccessibilityService.LOG_TAG, "Unknown message type " + message.what);
                    return;
            }
        }
    }

    public static abstract class GestureResultCallback {
        public void onCompleted(GestureDescription gestureDescription) {
        }

        public void onCancelled(GestureDescription gestureDescription) {
        }
    }

    private static class GestureResultCallbackInfo {
        GestureResultCallback callback;
        GestureDescription gestureDescription;
        Handler handler;

        GestureResultCallbackInfo(GestureDescription gestureDescription, GestureResultCallback callback, Handler handler) {
            this.gestureDescription = gestureDescription;
            this.callback = callback;
            this.handler = handler;
        }
    }
}
