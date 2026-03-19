package android.hardware.display;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.IDisplayManager;
import android.hardware.display.IDisplayManagerCallback;
import android.hardware.display.IVirtualDisplayCallback;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.DisplayInfo;
import android.view.Surface;
import java.util.ArrayList;

public final class DisplayManagerGlobal {
    private static final boolean DEBUG = false;
    public static final int EVENT_DISPLAY_ADDED = 1;
    public static final int EVENT_DISPLAY_CHANGED = 2;
    public static final int EVENT_DISPLAY_REMOVED = 3;
    private static final String TAG = "DisplayManager";
    private static final boolean USE_CACHE = false;
    private static DisplayManagerGlobal sInstance;
    private DisplayManagerCallback mCallback;
    private int[] mDisplayIdCache;
    private final IDisplayManager mDm;
    private int mWifiDisplayScanNestCount;
    private final Object mLock = new Object();
    private final ArrayList<DisplayListenerDelegate> mDisplayListeners = new ArrayList<>();
    private final SparseArray<DisplayInfo> mDisplayInfoCache = new SparseArray<>();

    private DisplayManagerGlobal(IDisplayManager dm) {
        this.mDm = dm;
    }

    public static DisplayManagerGlobal getInstance() {
        DisplayManagerGlobal displayManagerGlobal;
        IBinder b;
        synchronized (DisplayManagerGlobal.class) {
            if (sInstance == null && (b = ServiceManager.getService(Context.DISPLAY_SERVICE)) != null) {
                sInstance = new DisplayManagerGlobal(IDisplayManager.Stub.asInterface(b));
            }
            displayManagerGlobal = sInstance;
        }
        return displayManagerGlobal;
    }

    public DisplayInfo getDisplayInfo(int displayId) {
        try {
            synchronized (this.mLock) {
                DisplayInfo info = this.mDm.getDisplayInfo(displayId);
                if (info == null) {
                    return null;
                }
                registerCallbackIfNeededLocked();
                return info;
            }
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public int[] getDisplayIds() {
        int[] displayIds;
        try {
            synchronized (this.mLock) {
                displayIds = this.mDm.getDisplayIds();
                registerCallbackIfNeededLocked();
            }
            return displayIds;
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public Display getCompatibleDisplay(int displayId, DisplayAdjustments daj) {
        DisplayInfo displayInfo = getDisplayInfo(displayId);
        if (displayInfo == null) {
            return null;
        }
        return new Display(this, displayId, displayInfo, daj);
    }

    public Display getRealDisplay(int displayId) {
        return getCompatibleDisplay(displayId, DisplayAdjustments.DEFAULT_DISPLAY_ADJUSTMENTS);
    }

    public void registerDisplayListener(DisplayManager.DisplayListener listener, Handler handler) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mLock) {
            int index = findDisplayListenerLocked(listener);
            if (index < 0) {
                this.mDisplayListeners.add(new DisplayListenerDelegate(listener, handler));
                registerCallbackIfNeededLocked();
            }
        }
    }

    public void unregisterDisplayListener(DisplayManager.DisplayListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mLock) {
            int index = findDisplayListenerLocked(listener);
            if (index >= 0) {
                DisplayListenerDelegate d = this.mDisplayListeners.get(index);
                d.clearEvents();
                this.mDisplayListeners.remove(index);
            }
        }
    }

    private int findDisplayListenerLocked(DisplayManager.DisplayListener listener) {
        int numListeners = this.mDisplayListeners.size();
        for (int i = 0; i < numListeners; i++) {
            if (this.mDisplayListeners.get(i).mListener == listener) {
                return i;
            }
        }
        return -1;
    }

    private void registerCallbackIfNeededLocked() {
        DisplayManagerCallback displayManagerCallback = null;
        if (this.mCallback != null) {
            return;
        }
        this.mCallback = new DisplayManagerCallback(this, displayManagerCallback);
        try {
            this.mDm.registerCallback(this.mCallback);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    private void handleDisplayEvent(int displayId, int event) {
        synchronized (this.mLock) {
            int numListeners = this.mDisplayListeners.size();
            for (int i = 0; i < numListeners; i++) {
                this.mDisplayListeners.get(i).sendDisplayEvent(displayId, event);
            }
        }
    }

    public void startWifiDisplayScan() {
        synchronized (this.mLock) {
            int i = this.mWifiDisplayScanNestCount;
            this.mWifiDisplayScanNestCount = i + 1;
            if (i == 0) {
                registerCallbackIfNeededLocked();
                try {
                    this.mDm.startWifiDisplayScan();
                } catch (RemoteException ex) {
                    throw ex.rethrowFromSystemServer();
                }
            }
        }
    }

    public void stopWifiDisplayScan() {
        synchronized (this.mLock) {
            int i = this.mWifiDisplayScanNestCount - 1;
            this.mWifiDisplayScanNestCount = i;
            if (i == 0) {
                try {
                    this.mDm.stopWifiDisplayScan();
                } catch (RemoteException ex) {
                    throw ex.rethrowFromSystemServer();
                }
            } else if (this.mWifiDisplayScanNestCount < 0) {
                Log.wtf(TAG, "Wifi display scan nest count became negative: " + this.mWifiDisplayScanNestCount);
                this.mWifiDisplayScanNestCount = 0;
            }
        }
    }

    public void connectWifiDisplay(String deviceAddress) {
        if (deviceAddress == null) {
            throw new IllegalArgumentException("deviceAddress must not be null");
        }
        try {
            this.mDm.connectWifiDisplay(deviceAddress);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void pauseWifiDisplay() {
        try {
            this.mDm.pauseWifiDisplay();
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void resumeWifiDisplay() {
        try {
            this.mDm.resumeWifiDisplay();
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void disconnectWifiDisplay() {
        try {
            this.mDm.disconnectWifiDisplay();
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void renameWifiDisplay(String deviceAddress, String alias) {
        if (deviceAddress == null) {
            throw new IllegalArgumentException("deviceAddress must not be null");
        }
        try {
            this.mDm.renameWifiDisplay(deviceAddress, alias);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void forgetWifiDisplay(String deviceAddress) {
        if (deviceAddress == null) {
            throw new IllegalArgumentException("deviceAddress must not be null");
        }
        try {
            this.mDm.forgetWifiDisplay(deviceAddress);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public WifiDisplayStatus getWifiDisplayStatus() {
        try {
            return this.mDm.getWifiDisplayStatus();
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void requestColorTransform(int displayId, int colorTransformId) {
        try {
            this.mDm.requestColorTransform(displayId, colorTransformId);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public VirtualDisplay createVirtualDisplay(Context context, MediaProjection projection, String name, int width, int height, int densityDpi, Surface surface, int flags, VirtualDisplay.Callback callback, Handler handler) {
        if (TextUtils.isEmpty(name)) {
            throw new IllegalArgumentException("name must be non-null and non-empty");
        }
        if (width <= 0 || height <= 0 || densityDpi <= 0) {
            throw new IllegalArgumentException("width, height, and densityDpi must be greater than 0");
        }
        VirtualDisplayCallback callbackWrapper = new VirtualDisplayCallback(callback, handler);
        try {
            int displayId = this.mDm.createVirtualDisplay(callbackWrapper, projection != null ? projection.getProjection() : null, context.getPackageName(), name, width, height, densityDpi, surface, flags);
            if (displayId < 0) {
                Log.e(TAG, "Could not create virtual display: " + name);
                return null;
            }
            Display display = getRealDisplay(displayId);
            if (display == null) {
                Log.wtf(TAG, "Could not obtain display info for newly created virtual display: " + name);
                try {
                    this.mDm.releaseVirtualDisplay(callbackWrapper);
                    return null;
                } catch (RemoteException ex) {
                    throw ex.rethrowFromSystemServer();
                }
            }
            return new VirtualDisplay(this, display, callbackWrapper, surface);
        } catch (RemoteException ex2) {
            throw ex2.rethrowFromSystemServer();
        }
    }

    public void setVirtualDisplaySurface(IVirtualDisplayCallback token, Surface surface) {
        try {
            this.mDm.setVirtualDisplaySurface(token, surface);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void resizeVirtualDisplay(IVirtualDisplayCallback token, int width, int height, int densityDpi) {
        try {
            this.mDm.resizeVirtualDisplay(token, width, height, densityDpi);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public void releaseVirtualDisplay(IVirtualDisplayCallback token) {
        try {
            this.mDm.releaseVirtualDisplay(token);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    public boolean isSinkEnabled() {
        try {
            boolean enabled = this.mDm.isSinkEnabled();
            return enabled;
        } catch (RemoteException ex) {
            Log.w(TAG, "Failed to get sink status.", ex);
            return false;
        }
    }

    public void enableSink(boolean enable) {
        try {
            this.mDm.enableSink(enable);
        } catch (RemoteException ex) {
            Log.w(TAG, "Failed to request sink", ex);
        }
    }

    public void waitWifiDisplayConnection(Surface surface) {
        try {
            this.mDm.waitWifiDisplayConnection(surface);
        } catch (RemoteException ex) {
            Log.w(TAG, "Failed to request wait connection", ex);
        }
    }

    public void suspendWifiDisplay(boolean suspend, Surface surface) {
        try {
            this.mDm.suspendWifiDisplay(suspend, surface);
        } catch (RemoteException ex) {
            Log.w(TAG, "Failed to request suspend display", ex);
        }
    }

    public void sendUibcInputEvent(String input) {
        try {
            this.mDm.sendUibcInputEvent(input);
        } catch (RemoteException ex) {
            Log.w(TAG, "Failed to send uibc input event", ex);
        }
    }

    private final class DisplayManagerCallback extends IDisplayManagerCallback.Stub {
        DisplayManagerCallback(DisplayManagerGlobal this$0, DisplayManagerCallback displayManagerCallback) {
            this();
        }

        private DisplayManagerCallback() {
        }

        @Override
        public void onDisplayEvent(int displayId, int event) {
            DisplayManagerGlobal.this.handleDisplayEvent(displayId, event);
        }
    }

    private static final class DisplayListenerDelegate extends Handler {
        public final DisplayManager.DisplayListener mListener;

        public DisplayListenerDelegate(DisplayManager.DisplayListener listener, Handler handler) {
            super(handler != null ? handler.getLooper() : Looper.myLooper(), null, true);
            this.mListener = listener;
        }

        public void sendDisplayEvent(int displayId, int event) {
            Message msg = obtainMessage(event, displayId, 0);
            sendMessage(msg);
        }

        public void clearEvents() {
            removeCallbacksAndMessages(null);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    this.mListener.onDisplayAdded(msg.arg1);
                    break;
                case 2:
                    this.mListener.onDisplayChanged(msg.arg1);
                    break;
                case 3:
                    this.mListener.onDisplayRemoved(msg.arg1);
                    break;
            }
        }
    }

    private static final class VirtualDisplayCallback extends IVirtualDisplayCallback.Stub {
        private VirtualDisplayCallbackDelegate mDelegate;

        public VirtualDisplayCallback(VirtualDisplay.Callback callback, Handler handler) {
            if (callback == null) {
                return;
            }
            this.mDelegate = new VirtualDisplayCallbackDelegate(callback, handler);
        }

        @Override
        public void onPaused() {
            if (this.mDelegate == null) {
                return;
            }
            this.mDelegate.sendEmptyMessage(0);
        }

        @Override
        public void onResumed() {
            if (this.mDelegate == null) {
                return;
            }
            this.mDelegate.sendEmptyMessage(1);
        }

        @Override
        public void onStopped() {
            if (this.mDelegate == null) {
                return;
            }
            this.mDelegate.sendEmptyMessage(2);
        }
    }

    private static final class VirtualDisplayCallbackDelegate extends Handler {
        public static final int MSG_DISPLAY_PAUSED = 0;
        public static final int MSG_DISPLAY_RESUMED = 1;
        public static final int MSG_DISPLAY_STOPPED = 2;
        private final VirtualDisplay.Callback mCallback;

        public VirtualDisplayCallbackDelegate(VirtualDisplay.Callback callback, Handler handler) {
            super(handler != null ? handler.getLooper() : Looper.myLooper(), null, true);
            this.mCallback = callback;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    this.mCallback.onPaused();
                    break;
                case 1:
                    this.mCallback.onResumed();
                    break;
                case 2:
                    this.mCallback.onStopped();
                    break;
            }
        }
    }
}
