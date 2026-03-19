package android.hardware.display;

import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.graphics.Point;
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
import android.util.Pair;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.DisplayInfo;
import android.view.Surface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private DisplayManagerGlobal(IDisplayManager iDisplayManager) {
        this.mDm = iDisplayManager;
    }

    public static DisplayManagerGlobal getInstance() {
        DisplayManagerGlobal displayManagerGlobal;
        IBinder service;
        synchronized (DisplayManagerGlobal.class) {
            if (sInstance == null && (service = ServiceManager.getService(Context.DISPLAY_SERVICE)) != null) {
                sInstance = new DisplayManagerGlobal(IDisplayManager.Stub.asInterface(service));
            }
            displayManagerGlobal = sInstance;
        }
        return displayManagerGlobal;
    }

    public DisplayInfo getDisplayInfo(int i) {
        try {
            synchronized (this.mLock) {
                DisplayInfo displayInfo = this.mDm.getDisplayInfo(i);
                if (displayInfo == null) {
                    return null;
                }
                registerCallbackIfNeededLocked();
                return displayInfo;
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
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
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Display getCompatibleDisplay(int i, DisplayAdjustments displayAdjustments) {
        DisplayInfo displayInfo = getDisplayInfo(i);
        if (displayInfo == null) {
            return null;
        }
        return new Display(this, i, displayInfo, displayAdjustments);
    }

    public Display getCompatibleDisplay(int i, Resources resources) {
        DisplayInfo displayInfo = getDisplayInfo(i);
        if (displayInfo == null) {
            return null;
        }
        return new Display(this, i, displayInfo, resources);
    }

    public Display getRealDisplay(int i) {
        return getCompatibleDisplay(i, DisplayAdjustments.DEFAULT_DISPLAY_ADJUSTMENTS);
    }

    public void registerDisplayListener(DisplayManager.DisplayListener displayListener, Handler handler) {
        if (displayListener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mLock) {
            if (findDisplayListenerLocked(displayListener) < 0) {
                this.mDisplayListeners.add(new DisplayListenerDelegate(displayListener, handler));
                registerCallbackIfNeededLocked();
            }
        }
    }

    public void unregisterDisplayListener(DisplayManager.DisplayListener displayListener) {
        if (displayListener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mLock) {
            int iFindDisplayListenerLocked = findDisplayListenerLocked(displayListener);
            if (iFindDisplayListenerLocked >= 0) {
                this.mDisplayListeners.get(iFindDisplayListenerLocked).clearEvents();
                this.mDisplayListeners.remove(iFindDisplayListenerLocked);
            }
        }
    }

    private int findDisplayListenerLocked(DisplayManager.DisplayListener displayListener) {
        int size = this.mDisplayListeners.size();
        for (int i = 0; i < size; i++) {
            if (this.mDisplayListeners.get(i).mListener == displayListener) {
                return i;
            }
        }
        return -1;
    }

    private void registerCallbackIfNeededLocked() {
        if (this.mCallback == null) {
            this.mCallback = new DisplayManagerCallback();
            try {
                this.mDm.registerCallback(this.mCallback);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    private void handleDisplayEvent(int i, int i2) {
        synchronized (this.mLock) {
            int size = this.mDisplayListeners.size();
            for (int i3 = 0; i3 < size; i3++) {
                this.mDisplayListeners.get(i3).sendDisplayEvent(i, i2);
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
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
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
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            } else if (this.mWifiDisplayScanNestCount < 0) {
                Log.wtf(TAG, "Wifi display scan nest count became negative: " + this.mWifiDisplayScanNestCount);
                this.mWifiDisplayScanNestCount = 0;
            }
        }
    }

    public void connectWifiDisplay(String str) {
        if (str == null) {
            throw new IllegalArgumentException("deviceAddress must not be null");
        }
        try {
            this.mDm.connectWifiDisplay(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void pauseWifiDisplay() {
        try {
            this.mDm.pauseWifiDisplay();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void resumeWifiDisplay() {
        try {
            this.mDm.resumeWifiDisplay();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void disconnectWifiDisplay() {
        try {
            this.mDm.disconnectWifiDisplay();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void renameWifiDisplay(String str, String str2) {
        if (str == null) {
            throw new IllegalArgumentException("deviceAddress must not be null");
        }
        try {
            this.mDm.renameWifiDisplay(str, str2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void forgetWifiDisplay(String str) {
        if (str == null) {
            throw new IllegalArgumentException("deviceAddress must not be null");
        }
        try {
            this.mDm.forgetWifiDisplay(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public WifiDisplayStatus getWifiDisplayStatus() {
        try {
            return this.mDm.getWifiDisplayStatus();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestColorMode(int i, int i2) {
        try {
            this.mDm.requestColorMode(i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setSaturationLevel(float f) {
        try {
            this.mDm.setSaturationLevel(f);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public VirtualDisplay createVirtualDisplay(Context context, MediaProjection mediaProjection, String str, int i, int i2, int i3, Surface surface, int i4, VirtualDisplay.Callback callback, Handler handler, String str2) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("name must be non-null and non-empty");
        }
        if (i <= 0 || i2 <= 0 || i3 <= 0) {
            throw new IllegalArgumentException("width, height, and densityDpi must be greater than 0");
        }
        VirtualDisplayCallback virtualDisplayCallback = new VirtualDisplayCallback(callback, handler);
        try {
            int iCreateVirtualDisplay = this.mDm.createVirtualDisplay(virtualDisplayCallback, mediaProjection != null ? mediaProjection.getProjection() : null, context.getPackageName(), str, i, i2, i3, surface, i4, str2);
            if (iCreateVirtualDisplay < 0) {
                Log.e(TAG, "Could not create virtual display: " + str);
                return null;
            }
            Display realDisplay = getRealDisplay(iCreateVirtualDisplay);
            if (realDisplay == null) {
                Log.wtf(TAG, "Could not obtain display info for newly created virtual display: " + str);
                try {
                    this.mDm.releaseVirtualDisplay(virtualDisplayCallback);
                    return null;
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            return new VirtualDisplay(this, realDisplay, virtualDisplayCallback, surface);
        } catch (RemoteException e2) {
            throw e2.rethrowFromSystemServer();
        }
    }

    public void setVirtualDisplaySurface(IVirtualDisplayCallback iVirtualDisplayCallback, Surface surface) {
        try {
            this.mDm.setVirtualDisplaySurface(iVirtualDisplayCallback, surface);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void resizeVirtualDisplay(IVirtualDisplayCallback iVirtualDisplayCallback, int i, int i2, int i3) {
        try {
            this.mDm.resizeVirtualDisplay(iVirtualDisplayCallback, i, i2, i3);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void releaseVirtualDisplay(IVirtualDisplayCallback iVirtualDisplayCallback) {
        try {
            this.mDm.releaseVirtualDisplay(iVirtualDisplayCallback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Point getStableDisplaySize() {
        try {
            return this.mDm.getStableDisplaySize();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<BrightnessChangeEvent> getBrightnessEvents(String str) {
        try {
            ParceledListSlice brightnessEvents = this.mDm.getBrightnessEvents(str);
            if (brightnessEvents == null) {
                return Collections.emptyList();
            }
            return brightnessEvents.getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setBrightnessConfigurationForUser(BrightnessConfiguration brightnessConfiguration, int i, String str) {
        try {
            this.mDm.setBrightnessConfigurationForUser(brightnessConfiguration, i, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public BrightnessConfiguration getBrightnessConfigurationForUser(int i) {
        try {
            return this.mDm.getBrightnessConfigurationForUser(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public BrightnessConfiguration getDefaultBrightnessConfiguration() {
        try {
            return this.mDm.getDefaultBrightnessConfiguration();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTemporaryBrightness(int i) {
        try {
            this.mDm.setTemporaryBrightness(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTemporaryAutoBrightnessAdjustment(float f) {
        try {
            this.mDm.setTemporaryAutoBrightnessAdjustment(f);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Pair<float[], float[]> getMinimumBrightnessCurve() {
        try {
            Curve minimumBrightnessCurve = this.mDm.getMinimumBrightnessCurve();
            return Pair.create(minimumBrightnessCurve.getX(), minimumBrightnessCurve.getY());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<AmbientBrightnessDayStats> getAmbientBrightnessStats() {
        try {
            ParceledListSlice ambientBrightnessStats = this.mDm.getAmbientBrightnessStats();
            if (ambientBrightnessStats == null) {
                return Collections.emptyList();
            }
            return ambientBrightnessStats.getList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isSinkEnabled() {
        try {
            return this.mDm.isSinkEnabled();
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to get sink status.", e);
            return false;
        }
    }

    public void enableSink(boolean z) {
        try {
            this.mDm.enableSink(z);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to request sink", e);
        }
    }

    public void waitWifiDisplayConnection(Surface surface) {
        try {
            this.mDm.waitWifiDisplayConnection(surface);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to request wait connection", e);
        }
    }

    public void suspendWifiDisplay(boolean z, Surface surface) {
        try {
            this.mDm.suspendWifiDisplay(z, surface);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to request suspend display", e);
        }
    }

    public void sendUibcInputEvent(String str) {
        try {
            this.mDm.sendUibcInputEvent(str);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to send uibc input event", e);
        }
    }

    private final class DisplayManagerCallback extends IDisplayManagerCallback.Stub {
        private DisplayManagerCallback() {
        }

        @Override
        public void onDisplayEvent(int i, int i2) {
            DisplayManagerGlobal.this.handleDisplayEvent(i, i2);
        }
    }

    private static final class DisplayListenerDelegate extends Handler {
        public final DisplayManager.DisplayListener mListener;

        public DisplayListenerDelegate(DisplayManager.DisplayListener displayListener, Handler handler) {
            super(handler != null ? handler.getLooper() : Looper.myLooper(), null, true);
            this.mListener = displayListener;
        }

        public void sendDisplayEvent(int i, int i2) {
            sendMessage(obtainMessage(i2, i, 0));
        }

        public void clearEvents() {
            removeCallbacksAndMessages(null);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    this.mListener.onDisplayAdded(message.arg1);
                    break;
                case 2:
                    this.mListener.onDisplayChanged(message.arg1);
                    break;
                case 3:
                    this.mListener.onDisplayRemoved(message.arg1);
                    break;
            }
        }
    }

    private static final class VirtualDisplayCallback extends IVirtualDisplayCallback.Stub {
        private VirtualDisplayCallbackDelegate mDelegate;

        public VirtualDisplayCallback(VirtualDisplay.Callback callback, Handler handler) {
            if (callback != null) {
                this.mDelegate = new VirtualDisplayCallbackDelegate(callback, handler);
            }
        }

        @Override
        public void onPaused() {
            if (this.mDelegate != null) {
                this.mDelegate.sendEmptyMessage(0);
            }
        }

        @Override
        public void onResumed() {
            if (this.mDelegate != null) {
                this.mDelegate.sendEmptyMessage(1);
            }
        }

        @Override
        public void onStopped() {
            if (this.mDelegate != null) {
                this.mDelegate.sendEmptyMessage(2);
            }
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
        public void handleMessage(Message message) {
            switch (message.what) {
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
