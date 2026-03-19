package android.accessibilityservice;

import android.accessibilityservice.GestureDescription;
import android.accessibilityservice.IAccessibilityServiceClient;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.graphics.Region;
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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
    public static final int GLOBAL_ACTION_LOCK_SCREEN = 8;
    public static final int GLOBAL_ACTION_NOTIFICATIONS = 4;
    public static final int GLOBAL_ACTION_POWER_DIALOG = 6;
    public static final int GLOBAL_ACTION_QUICK_SETTINGS = 5;
    public static final int GLOBAL_ACTION_RECENTS = 3;
    public static final int GLOBAL_ACTION_TAKE_SCREENSHOT = 9;
    public static final int GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN = 7;
    private static final String LOG_TAG = "AccessibilityService";
    public static final String SERVICE_INTERFACE = "android.accessibilityservice.AccessibilityService";
    public static final String SERVICE_META_DATA = "android.accessibilityservice";
    public static final int SHOW_MODE_AUTO = 0;
    public static final int SHOW_MODE_HIDDEN = 1;
    private AccessibilityButtonController mAccessibilityButtonController;
    private FingerprintGestureController mFingerprintGestureController;
    private SparseArray<GestureResultCallbackInfo> mGestureStatusCallbackInfos;
    private int mGestureStatusCallbackSequence;
    private AccessibilityServiceInfo mInfo;
    private MagnificationController mMagnificationController;
    private SoftKeyboardController mSoftKeyboardController;
    private WindowManager mWindowManager;
    private IBinder mWindowToken;
    private int mConnectionId = -1;
    private final Object mLock = new Object();

    public interface Callbacks {
        void init(int i, IBinder iBinder);

        void onAccessibilityButtonAvailabilityChanged(boolean z);

        void onAccessibilityButtonClicked();

        void onAccessibilityEvent(AccessibilityEvent accessibilityEvent);

        void onFingerprintCapturingGesturesChanged(boolean z);

        void onFingerprintGesture(int i);

        boolean onGesture(int i);

        void onInterrupt();

        boolean onKeyEvent(KeyEvent keyEvent);

        void onMagnificationChanged(Region region, float f, float f2, float f3);

        void onPerformGestureResult(int i, boolean z);

        void onServiceConnected();

        void onSoftKeyboardShowModeChanged(int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SoftKeyboardShowMode {
    }

    public abstract void onAccessibilityEvent(AccessibilityEvent accessibilityEvent);

    public abstract void onInterrupt();

    private void dispatchServiceConnected() {
        if (this.mMagnificationController != null) {
            this.mMagnificationController.onServiceConnected();
        }
        if (this.mSoftKeyboardController != null) {
            this.mSoftKeyboardController.onServiceConnected();
        }
        onServiceConnected();
    }

    protected void onServiceConnected() {
    }

    protected boolean onGesture(int i) {
        return false;
    }

    protected boolean onKeyEvent(KeyEvent keyEvent) {
        return false;
    }

    public List<AccessibilityWindowInfo> getWindows() {
        return AccessibilityInteractionClient.getInstance().getWindows(this.mConnectionId);
    }

    public AccessibilityNodeInfo getRootInActiveWindow() {
        return AccessibilityInteractionClient.getInstance().getRootInActiveWindow(this.mConnectionId);
    }

    public final void disableSelf() {
        AccessibilityInteractionClient.getInstance();
        IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mConnectionId);
        if (connection != null) {
            try {
                connection.disableSelf();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
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

    public final FingerprintGestureController getFingerprintGestureController() {
        if (this.mFingerprintGestureController == null) {
            AccessibilityInteractionClient.getInstance();
            this.mFingerprintGestureController = new FingerprintGestureController(AccessibilityInteractionClient.getConnection(this.mConnectionId));
        }
        return this.mFingerprintGestureController;
    }

    public final boolean dispatchGesture(GestureDescription gestureDescription, GestureResultCallback gestureResultCallback, Handler handler) {
        AccessibilityInteractionClient.getInstance();
        IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mConnectionId);
        if (connection == null) {
            return false;
        }
        List<GestureDescription.GestureStep> gestureStepsFromGestureDescription = GestureDescription.MotionEventGenerator.getGestureStepsFromGestureDescription(gestureDescription, 100);
        try {
            synchronized (this.mLock) {
                this.mGestureStatusCallbackSequence++;
                if (gestureResultCallback != null) {
                    if (this.mGestureStatusCallbackInfos == null) {
                        this.mGestureStatusCallbackInfos = new SparseArray<>();
                    }
                    this.mGestureStatusCallbackInfos.put(this.mGestureStatusCallbackSequence, new GestureResultCallbackInfo(gestureDescription, gestureResultCallback, handler));
                }
                connection.sendGesture(this.mGestureStatusCallbackSequence, new ParceledListSlice(gestureStepsFromGestureDescription));
            }
            return true;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    void onPerformGestureResult(int i, final boolean z) {
        final GestureResultCallbackInfo gestureResultCallbackInfo;
        if (this.mGestureStatusCallbackInfos == null) {
            return;
        }
        synchronized (this.mLock) {
            gestureResultCallbackInfo = this.mGestureStatusCallbackInfos.get(i);
        }
        if (gestureResultCallbackInfo != null && gestureResultCallbackInfo.gestureDescription != null && gestureResultCallbackInfo.callback != null) {
            if (gestureResultCallbackInfo.handler != null) {
                gestureResultCallbackInfo.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (z) {
                            gestureResultCallbackInfo.callback.onCompleted(gestureResultCallbackInfo.gestureDescription);
                        } else {
                            gestureResultCallbackInfo.callback.onCancelled(gestureResultCallbackInfo.gestureDescription);
                        }
                    }
                });
            } else if (z) {
                gestureResultCallbackInfo.callback.onCompleted(gestureResultCallbackInfo.gestureDescription);
            } else {
                gestureResultCallbackInfo.callback.onCancelled(gestureResultCallbackInfo.gestureDescription);
            }
        }
    }

    private void onMagnificationChanged(Region region, float f, float f2, float f3) {
        if (this.mMagnificationController != null) {
            this.mMagnificationController.dispatchMagnificationChanged(region, f, f2, f3);
        }
    }

    private void onFingerprintCapturingGesturesChanged(boolean z) {
        getFingerprintGestureController().onGestureDetectionActiveChanged(z);
    }

    private void onFingerprintGesture(int i) {
        getFingerprintGestureController().onGesture(i);
    }

    public static final class MagnificationController {
        private ArrayMap<OnMagnificationChangedListener, Handler> mListeners;
        private final Object mLock;
        private final AccessibilityService mService;

        public interface OnMagnificationChangedListener {
            void onMagnificationChanged(MagnificationController magnificationController, Region region, float f, float f2, float f3);
        }

        MagnificationController(AccessibilityService accessibilityService, Object obj) {
            this.mService = accessibilityService;
            this.mLock = obj;
        }

        void onServiceConnected() {
            synchronized (this.mLock) {
                if (this.mListeners != null && !this.mListeners.isEmpty()) {
                    setMagnificationCallbackEnabled(true);
                }
            }
        }

        public void addListener(OnMagnificationChangedListener onMagnificationChangedListener) {
            addListener(onMagnificationChangedListener, null);
        }

        public void addListener(OnMagnificationChangedListener onMagnificationChangedListener, Handler handler) {
            synchronized (this.mLock) {
                if (this.mListeners == null) {
                    this.mListeners = new ArrayMap<>();
                }
                boolean zIsEmpty = this.mListeners.isEmpty();
                this.mListeners.put(onMagnificationChangedListener, handler);
                if (zIsEmpty) {
                    setMagnificationCallbackEnabled(true);
                }
            }
        }

        public boolean removeListener(OnMagnificationChangedListener onMagnificationChangedListener) {
            boolean z;
            if (this.mListeners == null) {
                return false;
            }
            synchronized (this.mLock) {
                int iIndexOfKey = this.mListeners.indexOfKey(onMagnificationChangedListener);
                z = iIndexOfKey >= 0;
                if (z) {
                    this.mListeners.removeAt(iIndexOfKey);
                }
                if (z && this.mListeners.isEmpty()) {
                    setMagnificationCallbackEnabled(false);
                }
            }
            return z;
        }

        private void setMagnificationCallbackEnabled(boolean z) {
            AccessibilityInteractionClient.getInstance();
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    connection.setMagnificationCallbackEnabled(z);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        void dispatchMagnificationChanged(final Region region, final float f, final float f2, final float f3) {
            synchronized (this.mLock) {
                if (this.mListeners != null && !this.mListeners.isEmpty()) {
                    ArrayMap arrayMap = new ArrayMap(this.mListeners);
                    int size = arrayMap.size();
                    for (int i = 0; i < size; i++) {
                        final OnMagnificationChangedListener onMagnificationChangedListener = (OnMagnificationChangedListener) arrayMap.keyAt(i);
                        Handler handler = (Handler) arrayMap.valueAt(i);
                        if (handler != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    onMagnificationChangedListener.onMagnificationChanged(MagnificationController.this, region, f, f2, f3);
                                }
                            });
                        } else {
                            onMagnificationChangedListener.onMagnificationChanged(this, region, f, f2, f3);
                        }
                    }
                    return;
                }
                Slog.d(AccessibilityService.LOG_TAG, "Received magnification changed callback with no listeners registered!");
                setMagnificationCallbackEnabled(false);
            }
        }

        public float getScale() {
            AccessibilityInteractionClient.getInstance();
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getMagnificationScale();
                } catch (RemoteException e) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to obtain scale", e);
                    e.rethrowFromSystemServer();
                    return 1.0f;
                }
            }
            return 1.0f;
        }

        public float getCenterX() {
            AccessibilityInteractionClient.getInstance();
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getMagnificationCenterX();
                } catch (RemoteException e) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to obtain center X", e);
                    e.rethrowFromSystemServer();
                    return 0.0f;
                }
            }
            return 0.0f;
        }

        public float getCenterY() {
            AccessibilityInteractionClient.getInstance();
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getMagnificationCenterY();
                } catch (RemoteException e) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to obtain center Y", e);
                    e.rethrowFromSystemServer();
                    return 0.0f;
                }
            }
            return 0.0f;
        }

        public Region getMagnificationRegion() {
            AccessibilityInteractionClient.getInstance();
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.getMagnificationRegion();
                } catch (RemoteException e) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to obtain magnified region", e);
                    e.rethrowFromSystemServer();
                }
            }
            return Region.obtain();
        }

        public boolean reset(boolean z) {
            AccessibilityInteractionClient.getInstance();
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.resetMagnification(z);
                } catch (RemoteException e) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to reset", e);
                    e.rethrowFromSystemServer();
                    return false;
                }
            }
            return false;
        }

        public boolean setScale(float f, boolean z) {
            AccessibilityInteractionClient.getInstance();
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.setMagnificationScaleAndCenter(f, Float.NaN, Float.NaN, z);
                } catch (RemoteException e) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to set scale", e);
                    e.rethrowFromSystemServer();
                    return false;
                }
            }
            return false;
        }

        public boolean setCenter(float f, float f2, boolean z) {
            AccessibilityInteractionClient.getInstance();
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.setMagnificationScaleAndCenter(Float.NaN, f, f2, z);
                } catch (RemoteException e) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to set center", e);
                    e.rethrowFromSystemServer();
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

    private void onSoftKeyboardShowModeChanged(int i) {
        if (this.mSoftKeyboardController != null) {
            this.mSoftKeyboardController.dispatchSoftKeyboardShowModeChanged(i);
        }
    }

    public static final class SoftKeyboardController {
        private ArrayMap<OnShowModeChangedListener, Handler> mListeners;
        private final Object mLock;
        private final AccessibilityService mService;

        public interface OnShowModeChangedListener {
            void onShowModeChanged(SoftKeyboardController softKeyboardController, int i);
        }

        SoftKeyboardController(AccessibilityService accessibilityService, Object obj) {
            this.mService = accessibilityService;
            this.mLock = obj;
        }

        void onServiceConnected() {
            synchronized (this.mLock) {
                if (this.mListeners != null && !this.mListeners.isEmpty()) {
                    setSoftKeyboardCallbackEnabled(true);
                }
            }
        }

        public void addOnShowModeChangedListener(OnShowModeChangedListener onShowModeChangedListener) {
            addOnShowModeChangedListener(onShowModeChangedListener, null);
        }

        public void addOnShowModeChangedListener(OnShowModeChangedListener onShowModeChangedListener, Handler handler) {
            synchronized (this.mLock) {
                if (this.mListeners == null) {
                    this.mListeners = new ArrayMap<>();
                }
                boolean zIsEmpty = this.mListeners.isEmpty();
                this.mListeners.put(onShowModeChangedListener, handler);
                if (zIsEmpty) {
                    setSoftKeyboardCallbackEnabled(true);
                }
            }
        }

        public boolean removeOnShowModeChangedListener(OnShowModeChangedListener onShowModeChangedListener) {
            boolean z;
            if (this.mListeners == null) {
                return false;
            }
            synchronized (this.mLock) {
                int iIndexOfKey = this.mListeners.indexOfKey(onShowModeChangedListener);
                z = iIndexOfKey >= 0;
                if (z) {
                    this.mListeners.removeAt(iIndexOfKey);
                }
                if (z && this.mListeners.isEmpty()) {
                    setSoftKeyboardCallbackEnabled(false);
                }
            }
            return z;
        }

        private void setSoftKeyboardCallbackEnabled(boolean z) {
            AccessibilityInteractionClient.getInstance();
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    connection.setSoftKeyboardCallbackEnabled(z);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        void dispatchSoftKeyboardShowModeChanged(final int i) {
            synchronized (this.mLock) {
                if (this.mListeners != null && !this.mListeners.isEmpty()) {
                    ArrayMap arrayMap = new ArrayMap(this.mListeners);
                    int size = arrayMap.size();
                    for (int i2 = 0; i2 < size; i2++) {
                        final OnShowModeChangedListener onShowModeChangedListener = (OnShowModeChangedListener) arrayMap.keyAt(i2);
                        Handler handler = (Handler) arrayMap.valueAt(i2);
                        if (handler != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    onShowModeChangedListener.onShowModeChanged(SoftKeyboardController.this, i);
                                }
                            });
                        } else {
                            onShowModeChangedListener.onShowModeChanged(this, i);
                        }
                    }
                    return;
                }
                Slog.w(AccessibilityService.LOG_TAG, "Received soft keyboard show mode changed callback with no listeners registered!");
                setSoftKeyboardCallbackEnabled(false);
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

        public boolean setShowMode(int i) {
            AccessibilityInteractionClient.getInstance();
            IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mService.mConnectionId);
            if (connection != null) {
                try {
                    return connection.setSoftKeyboardShowMode(i);
                } catch (RemoteException e) {
                    Log.w(AccessibilityService.LOG_TAG, "Failed to set soft keyboard behavior", e);
                    e.rethrowFromSystemServer();
                    return false;
                }
            }
            return false;
        }
    }

    public final AccessibilityButtonController getAccessibilityButtonController() {
        AccessibilityButtonController accessibilityButtonController;
        synchronized (this.mLock) {
            if (this.mAccessibilityButtonController == null) {
                AccessibilityInteractionClient.getInstance();
                this.mAccessibilityButtonController = new AccessibilityButtonController(AccessibilityInteractionClient.getConnection(this.mConnectionId));
            }
            accessibilityButtonController = this.mAccessibilityButtonController;
        }
        return accessibilityButtonController;
    }

    private void onAccessibilityButtonClicked() {
        getAccessibilityButtonController().dispatchAccessibilityButtonClicked();
    }

    private void onAccessibilityButtonAvailabilityChanged(boolean z) {
        getAccessibilityButtonController().dispatchAccessibilityButtonAvailabilityChanged(z);
    }

    public final boolean performGlobalAction(int i) {
        AccessibilityInteractionClient.getInstance();
        IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mConnectionId);
        if (connection != null) {
            try {
                return connection.performGlobalAction(i);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "Error while calling performGlobalAction", e);
                e.rethrowFromSystemServer();
                return false;
            }
        }
        return false;
    }

    public AccessibilityNodeInfo findFocus(int i) {
        return AccessibilityInteractionClient.getInstance().findFocus(this.mConnectionId, -2, AccessibilityNodeInfo.ROOT_NODE_ID, i);
    }

    public final AccessibilityServiceInfo getServiceInfo() {
        AccessibilityInteractionClient.getInstance();
        IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mConnectionId);
        if (connection != null) {
            try {
                return connection.getServiceInfo();
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "Error while getting AccessibilityServiceInfo", e);
                e.rethrowFromSystemServer();
                return null;
            }
        }
        return null;
    }

    public final void setServiceInfo(AccessibilityServiceInfo accessibilityServiceInfo) {
        this.mInfo = accessibilityServiceInfo;
        sendServiceInfo();
    }

    private void sendServiceInfo() {
        AccessibilityInteractionClient.getInstance();
        IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mConnectionId);
        if (this.mInfo != null && connection != null) {
            try {
                connection.setServiceInfo(this.mInfo);
                this.mInfo = null;
                AccessibilityInteractionClient.getInstance().clearCache();
            } catch (RemoteException e) {
                Log.w(LOG_TAG, "Error while setting AccessibilityServiceInfo", e);
                e.rethrowFromSystemServer();
            }
        }
    }

    @Override
    public Object getSystemService(String str) {
        if (getBaseContext() == null) {
            throw new IllegalStateException("System services not available to Activities before onCreate()");
        }
        if (Context.WINDOW_SERVICE.equals(str)) {
            if (this.mWindowManager == null) {
                this.mWindowManager = (WindowManager) getBaseContext().getSystemService(str);
            }
            return this.mWindowManager;
        }
        return super.getSystemService(str);
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
            public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
                AccessibilityService.this.onAccessibilityEvent(accessibilityEvent);
            }

            @Override
            public void init(int i, IBinder iBinder) {
                AccessibilityService.this.mConnectionId = i;
                AccessibilityService.this.mWindowToken = iBinder;
                ((WindowManagerImpl) AccessibilityService.this.getSystemService(Context.WINDOW_SERVICE)).setDefaultToken(iBinder);
            }

            @Override
            public boolean onGesture(int i) {
                return AccessibilityService.this.onGesture(i);
            }

            @Override
            public boolean onKeyEvent(KeyEvent keyEvent) {
                return AccessibilityService.this.onKeyEvent(keyEvent);
            }

            @Override
            public void onMagnificationChanged(Region region, float f, float f2, float f3) {
                AccessibilityService.this.onMagnificationChanged(region, f, f2, f3);
            }

            @Override
            public void onSoftKeyboardShowModeChanged(int i) {
                AccessibilityService.this.onSoftKeyboardShowModeChanged(i);
            }

            @Override
            public void onPerformGestureResult(int i, boolean z) {
                AccessibilityService.this.onPerformGestureResult(i, z);
            }

            @Override
            public void onFingerprintCapturingGesturesChanged(boolean z) {
                AccessibilityService.this.onFingerprintCapturingGesturesChanged(z);
            }

            @Override
            public void onFingerprintGesture(int i) {
                AccessibilityService.this.onFingerprintGesture(i);
            }

            @Override
            public void onAccessibilityButtonClicked() {
                AccessibilityService.this.onAccessibilityButtonClicked();
            }

            @Override
            public void onAccessibilityButtonAvailabilityChanged(boolean z) {
                AccessibilityService.this.onAccessibilityButtonAvailabilityChanged(z);
            }
        });
    }

    public static class IAccessibilityServiceClientWrapper extends IAccessibilityServiceClient.Stub implements HandlerCaller.Callback {
        private static final int DO_ACCESSIBILITY_BUTTON_AVAILABILITY_CHANGED = 13;
        private static final int DO_ACCESSIBILITY_BUTTON_CLICKED = 12;
        private static final int DO_CLEAR_ACCESSIBILITY_CACHE = 5;
        private static final int DO_GESTURE_COMPLETE = 9;
        private static final int DO_INIT = 1;
        private static final int DO_ON_ACCESSIBILITY_EVENT = 3;
        private static final int DO_ON_FINGERPRINT_ACTIVE_CHANGED = 10;
        private static final int DO_ON_FINGERPRINT_GESTURE = 11;
        private static final int DO_ON_GESTURE = 4;
        private static final int DO_ON_INTERRUPT = 2;
        private static final int DO_ON_KEY_EVENT = 6;
        private static final int DO_ON_MAGNIFICATION_CHANGED = 7;
        private static final int DO_ON_SOFT_KEYBOARD_SHOW_MODE_CHANGED = 8;
        private final Callbacks mCallback;
        private final HandlerCaller mCaller;
        private int mConnectionId = -1;

        public IAccessibilityServiceClientWrapper(Context context, Looper looper, Callbacks callbacks) {
            this.mCallback = callbacks;
            this.mCaller = new HandlerCaller(context, looper, this, true);
        }

        @Override
        public void init(IAccessibilityServiceConnection iAccessibilityServiceConnection, int i, IBinder iBinder) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageIOO(1, i, iAccessibilityServiceConnection, iBinder));
        }

        @Override
        public void onInterrupt() {
            this.mCaller.sendMessage(this.mCaller.obtainMessage(2));
        }

        @Override
        public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent, boolean z) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageBO(3, z, accessibilityEvent));
        }

        @Override
        public void onGesture(int i) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageI(4, i));
        }

        @Override
        public void clearAccessibilityCache() {
            this.mCaller.sendMessage(this.mCaller.obtainMessage(5));
        }

        @Override
        public void onKeyEvent(KeyEvent keyEvent, int i) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageIO(6, i, keyEvent));
        }

        @Override
        public void onMagnificationChanged(Region region, float f, float f2, float f3) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = region;
            someArgsObtain.arg2 = Float.valueOf(f);
            someArgsObtain.arg3 = Float.valueOf(f2);
            someArgsObtain.arg4 = Float.valueOf(f3);
            this.mCaller.sendMessage(this.mCaller.obtainMessageO(7, someArgsObtain));
        }

        @Override
        public void onSoftKeyboardShowModeChanged(int i) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageI(8, i));
        }

        @Override
        public void onPerformGestureResult(int i, boolean z) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageII(9, i, z ? 1 : 0));
        }

        @Override
        public void onFingerprintCapturingGesturesChanged(boolean z) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageI(10, z ? 1 : 0));
        }

        @Override
        public void onFingerprintGesture(int i) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageI(11, i));
        }

        @Override
        public void onAccessibilityButtonClicked() {
            this.mCaller.sendMessage(this.mCaller.obtainMessage(12));
        }

        @Override
        public void onAccessibilityButtonAvailabilityChanged(boolean z) {
            this.mCaller.sendMessage(this.mCaller.obtainMessageI(13, z ? 1 : 0));
        }

        @Override
        public void executeMessage(Message message) {
            switch (message.what) {
                case 1:
                    this.mConnectionId = message.arg1;
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    IAccessibilityServiceConnection iAccessibilityServiceConnection = (IAccessibilityServiceConnection) someArgs.arg1;
                    IBinder iBinder = (IBinder) someArgs.arg2;
                    someArgs.recycle();
                    if (iAccessibilityServiceConnection != null) {
                        AccessibilityInteractionClient.getInstance();
                        AccessibilityInteractionClient.addConnection(this.mConnectionId, iAccessibilityServiceConnection);
                        this.mCallback.init(this.mConnectionId, iBinder);
                        this.mCallback.onServiceConnected();
                        return;
                    }
                    AccessibilityInteractionClient.getInstance();
                    AccessibilityInteractionClient.removeConnection(this.mConnectionId);
                    this.mConnectionId = -1;
                    AccessibilityInteractionClient.getInstance().clearCache();
                    this.mCallback.init(-1, null);
                    return;
                case 2:
                    if (this.mConnectionId != -1) {
                        this.mCallback.onInterrupt();
                        return;
                    }
                    return;
                case 3:
                    AccessibilityEvent accessibilityEvent = (AccessibilityEvent) message.obj;
                    boolean z = message.arg1 != 0;
                    if (accessibilityEvent != null) {
                        AccessibilityInteractionClient.getInstance().onAccessibilityEvent(accessibilityEvent);
                        if (z && this.mConnectionId != -1) {
                            this.mCallback.onAccessibilityEvent(accessibilityEvent);
                        }
                        try {
                            accessibilityEvent.recycle();
                            return;
                        } catch (IllegalStateException e) {
                            return;
                        }
                    }
                    return;
                case 4:
                    if (this.mConnectionId != -1) {
                        this.mCallback.onGesture(message.arg1);
                        return;
                    }
                    return;
                case 5:
                    AccessibilityInteractionClient.getInstance().clearCache();
                    return;
                case 6:
                    KeyEvent keyEvent = (KeyEvent) message.obj;
                    try {
                        AccessibilityInteractionClient.getInstance();
                        IAccessibilityServiceConnection connection = AccessibilityInteractionClient.getConnection(this.mConnectionId);
                        if (connection != null) {
                            try {
                                connection.setOnKeyEventResult(this.mCallback.onKeyEvent(keyEvent), message.arg1);
                                break;
                            } catch (RemoteException e2) {
                            }
                        }
                        try {
                            keyEvent.recycle();
                            return;
                        } catch (IllegalStateException e3) {
                            return;
                        }
                    } catch (Throwable th) {
                        try {
                            keyEvent.recycle();
                            break;
                        } catch (IllegalStateException e4) {
                        }
                        throw th;
                    }
                case 7:
                    if (this.mConnectionId != -1) {
                        SomeArgs someArgs2 = (SomeArgs) message.obj;
                        this.mCallback.onMagnificationChanged((Region) someArgs2.arg1, ((Float) someArgs2.arg2).floatValue(), ((Float) someArgs2.arg3).floatValue(), ((Float) someArgs2.arg4).floatValue());
                        return;
                    }
                    return;
                case 8:
                    if (this.mConnectionId != -1) {
                        this.mCallback.onSoftKeyboardShowModeChanged(message.arg1);
                        return;
                    }
                    return;
                case 9:
                    if (this.mConnectionId != -1) {
                        this.mCallback.onPerformGestureResult(message.arg1, message.arg2 == 1);
                        return;
                    }
                    return;
                case 10:
                    if (this.mConnectionId != -1) {
                        this.mCallback.onFingerprintCapturingGesturesChanged(message.arg1 == 1);
                        return;
                    }
                    return;
                case 11:
                    if (this.mConnectionId != -1) {
                        this.mCallback.onFingerprintGesture(message.arg1);
                        return;
                    }
                    return;
                case 12:
                    if (this.mConnectionId != -1) {
                        this.mCallback.onAccessibilityButtonClicked();
                        return;
                    }
                    return;
                case 13:
                    if (this.mConnectionId != -1) {
                        this.mCallback.onAccessibilityButtonAvailabilityChanged(message.arg1 != 0);
                        return;
                    }
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

        GestureResultCallbackInfo(GestureDescription gestureDescription, GestureResultCallback gestureResultCallback, Handler handler) {
            this.gestureDescription = gestureDescription;
            this.callback = gestureResultCallback;
            this.handler = handler;
        }
    }
}
