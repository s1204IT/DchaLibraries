package com.android.server.display;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplaySessionInfo;
import android.hardware.display.WifiDisplayStatus;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.WifiDisplayController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import libcore.util.Objects;

final class WifiDisplayAdapter extends DisplayAdapter {
    private static final String ACTION_DISCONNECT = "android.server.display.wfd.DISCONNECT";
    private static final boolean DEBUG = true;
    private static final String DISPLAY_NAME_PREFIX = "wifi:";
    private static final int MSG_SEND_STATUS_CHANGE_BROADCAST = 1;
    private static final String TAG = "WifiDisplayAdapter";
    private WifiDisplay mActiveDisplay;
    private int mActiveDisplayState;
    private WifiDisplay[] mAvailableDisplays;
    private final BroadcastReceiver mBroadcastReceiver;
    private WifiDisplayStatus mCurrentStatus;
    private WifiDisplayController mDisplayController;
    private WifiDisplayDevice mDisplayDevice;
    private WifiDisplay[] mDisplays;
    private int mFeatureState;
    private final WifiDisplayHandler mHandler;
    private boolean mInDisconnectingThread;
    private boolean mPendingStatusChangeBroadcast;
    private final PersistentDataStore mPersistentDataStore;
    private WifiDisplay[] mRememberedDisplays;
    private int mScanState;
    private WifiDisplaySessionInfo mSessionInfo;
    private boolean mSinkConnectRequest;
    private boolean mSinkEnabled;
    private final boolean mSupportsProtectedBuffers;
    private final WifiDisplayController.Listener mWifiDisplayListener;

    enum SinkEvent {
        SINK_EVENT_CONNECTING,
        SINK_EVENT_CONNECTION_FAILED,
        SINK_EVENT_CONNECTED,
        SINK_EVENT_DISCONNECTED;

        public static SinkEvent[] valuesCustom() {
            return values();
        }
    }

    public WifiDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener, PersistentDataStore persistentDataStore) {
        super(syncRoot, context, handler, listener, TAG);
        this.mFeatureState = 2;
        this.mDisplays = WifiDisplay.EMPTY_ARRAY;
        this.mAvailableDisplays = WifiDisplay.EMPTY_ARRAY;
        this.mRememberedDisplays = WifiDisplay.EMPTY_ARRAY;
        this.mSinkEnabled = false;
        this.mSinkConnectRequest = false;
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!intent.getAction().equals(WifiDisplayAdapter.ACTION_DISCONNECT)) {
                    return;
                }
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplayAdapter.this.requestDisconnectLocked();
                }
            }
        };
        this.mWifiDisplayListener = new WifiDisplayController.Listener() {
            @Override
            public void onFeatureStateChanged(int featureState) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mFeatureState != featureState) {
                        WifiDisplayAdapter.this.mFeatureState = featureState;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override
            public void onScanStarted() {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mScanState != 1) {
                        WifiDisplayAdapter.this.mScanState = 1;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override
            public void onScanResults(WifiDisplay[] availableDisplays) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplay[] availableDisplays2 = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAliases(availableDisplays);
                    boolean changed = !Arrays.equals(WifiDisplayAdapter.this.mAvailableDisplays, availableDisplays2);
                    for (int i = 0; !changed && i < availableDisplays2.length; i++) {
                        changed = availableDisplays2[i].canConnect() != WifiDisplayAdapter.this.mAvailableDisplays[i].canConnect();
                    }
                    if (changed) {
                        WifiDisplayAdapter.this.mAvailableDisplays = availableDisplays2;
                        WifiDisplayAdapter.this.fixRememberedDisplayNamesFromAvailableDisplaysLocked();
                        WifiDisplayAdapter.this.updateDisplaysLocked();
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override
            public void onScanFinished() {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mScanState != 0) {
                        WifiDisplayAdapter.this.mScanState = 0;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override
            public void onDisplayConnecting(WifiDisplay display) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mSinkEnabled) {
                        WifiDisplayAdapter.this.handleSinkEvent(display, SinkEvent.SINK_EVENT_CONNECTING);
                        return;
                    }
                    WifiDisplay display2 = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAlias(display);
                    if (WifiDisplayAdapter.this.mActiveDisplayState != 1 || WifiDisplayAdapter.this.mActiveDisplay == null || !WifiDisplayAdapter.this.mActiveDisplay.equals(display2)) {
                        WifiDisplayAdapter.this.mActiveDisplayState = 1;
                        WifiDisplayAdapter.this.mActiveDisplay = display2;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override
            public void onDisplayConnectionFailed() {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mSinkEnabled) {
                        WifiDisplayAdapter.this.handleSinkEvent(null, SinkEvent.SINK_EVENT_CONNECTION_FAILED);
                        return;
                    }
                    if (WifiDisplayAdapter.this.mActiveDisplayState != 0 || WifiDisplayAdapter.this.mActiveDisplay != null) {
                        WifiDisplayAdapter.this.mActiveDisplayState = 0;
                        WifiDisplayAdapter.this.mActiveDisplay = null;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override
            public void onDisplayConnected(WifiDisplay display, Surface surface, int width, int height, int flags) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mSinkEnabled) {
                        WifiDisplayAdapter.this.handleSinkEvent(display, SinkEvent.SINK_EVENT_CONNECTED);
                        return;
                    }
                    WifiDisplay display2 = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAlias(display);
                    WifiDisplayAdapter.this.addDisplayDeviceLocked(display2, surface, width, height, flags);
                    if (WifiDisplayAdapter.this.mActiveDisplayState != 2 || WifiDisplayAdapter.this.mActiveDisplay == null || !WifiDisplayAdapter.this.mActiveDisplay.equals(display2)) {
                        WifiDisplayAdapter.this.mActiveDisplayState = 2;
                        WifiDisplayAdapter.this.mActiveDisplay = display2;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override
            public void onDisplaySessionInfo(WifiDisplaySessionInfo sessionInfo) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplayAdapter.this.mSessionInfo = sessionInfo;
                    WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                }
            }

            @Override
            public void onDisplayChanged(WifiDisplay display) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplay display2 = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAlias(display);
                    if (WifiDisplayAdapter.this.mActiveDisplay != null && WifiDisplayAdapter.this.mActiveDisplay.hasSameAddress(display2) && !WifiDisplayAdapter.this.mActiveDisplay.equals(display2)) {
                        WifiDisplayAdapter.this.mActiveDisplay = display2;
                        WifiDisplayAdapter.this.renameDisplayDeviceLocked(display2.getFriendlyDisplayName());
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override
            public void onDisplayDisconnected() {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mSinkEnabled) {
                        WifiDisplayAdapter.this.handleSinkEvent(null, SinkEvent.SINK_EVENT_DISCONNECTED);
                        return;
                    }
                    WifiDisplayAdapter.this.removeDisplayDeviceLocked();
                    if (WifiDisplayAdapter.this.mActiveDisplayState != 0 || WifiDisplayAdapter.this.mActiveDisplay != null) {
                        WifiDisplayAdapter.this.mActiveDisplayState = 0;
                        WifiDisplayAdapter.this.mActiveDisplay = null;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override
            public void onDisplayDisconnecting() {
                if (!WifiDisplayAdapter.this.mInDisconnectingThread) {
                    return;
                }
                Slog.e(WifiDisplayAdapter.TAG, "still in WfdDisConnThread!");
            }
        };
        this.mHandler = new WifiDisplayHandler(handler.getLooper());
        this.mPersistentDataStore = persistentDataStore;
        this.mSupportsProtectedBuffers = context.getResources().getBoolean(R.^attr-private.layout_alwaysShow);
    }

    @Override
    public void dumpLocked(PrintWriter pw) {
        super.dumpLocked(pw);
        pw.println("mCurrentStatus=" + getWifiDisplayStatusLocked());
        pw.println("mFeatureState=" + this.mFeatureState);
        pw.println("mScanState=" + this.mScanState);
        pw.println("mActiveDisplayState=" + this.mActiveDisplayState);
        pw.println("mActiveDisplay=" + this.mActiveDisplay);
        pw.println("mDisplays=" + Arrays.toString(this.mDisplays));
        pw.println("mAvailableDisplays=" + Arrays.toString(this.mAvailableDisplays));
        pw.println("mRememberedDisplays=" + Arrays.toString(this.mRememberedDisplays));
        pw.println("mPendingStatusChangeBroadcast=" + this.mPendingStatusChangeBroadcast);
        pw.println("mSupportsProtectedBuffers=" + this.mSupportsProtectedBuffers);
        if (this.mDisplayController == null) {
            pw.println("mDisplayController=null");
            return;
        }
        pw.println("mDisplayController:");
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        ipw.increaseIndent();
        DumpUtils.dumpAsync(getHandler(), this.mDisplayController, ipw, "", 200L);
    }

    @Override
    public void registerLocked() {
        super.registerLocked();
        updateRememberedDisplaysLocked();
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                WifiDisplayAdapter.this.mDisplayController = new WifiDisplayController(WifiDisplayAdapter.this.getContext(), WifiDisplayAdapter.this.getHandler(), WifiDisplayAdapter.this.mWifiDisplayListener);
                WifiDisplayAdapter.this.getContext().registerReceiverAsUser(WifiDisplayAdapter.this.mBroadcastReceiver, UserHandle.ALL, new IntentFilter(WifiDisplayAdapter.ACTION_DISCONNECT), null, WifiDisplayAdapter.this.mHandler);
            }
        });
    }

    public void requestStartScanLocked() {
        Slog.d(TAG, "requestStartScanLocked");
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController == null) {
                    return;
                }
                WifiDisplayAdapter.this.mDisplayController.requestStartScan();
            }
        });
    }

    public void requestStopScanLocked() {
        Slog.d(TAG, "requestStopScanLocked");
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController == null) {
                    return;
                }
                WifiDisplayAdapter.this.mDisplayController.requestStopScan();
            }
        });
    }

    public void requestConnectLocked(final String address) {
        Slog.d(TAG, "requestConnectLocked: address=" + address);
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController == null) {
                    return;
                }
                WifiDisplayAdapter.this.mDisplayController.requestConnect(address);
            }
        });
    }

    public void requestPauseLocked() {
        Slog.d(TAG, "requestPauseLocked");
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController == null) {
                    return;
                }
                WifiDisplayAdapter.this.mDisplayController.requestPause();
            }
        });
    }

    public void requestResumeLocked() {
        Slog.d(TAG, "requestResumeLocked");
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController == null) {
                    return;
                }
                WifiDisplayAdapter.this.mDisplayController.requestResume();
            }
        });
    }

    public void requestDisconnectLocked() {
        Slog.d(TAG, "requestDisconnectedLocked");
        if (SystemProperties.get("ro.mtk_wfd_sink_support").equals("1") && this.mSinkEnabled) {
            this.mSinkConnectRequest = false;
            if (this.mDisplayController != null) {
                this.mDisplayController.requestDisconnect();
            }
        } else {
            Slog.d(TAG, "Call removeDisplayDeviceLocked()");
            removeDisplayDeviceLocked();
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (WifiDisplayAdapter.this.mDisplayController == null) {
                        return;
                    }
                    WifiDisplayAdapter.this.mDisplayController.requestDisconnect();
                }
            });
        }
        Slog.d(TAG, "requestDisconnectedLocked return");
    }

    public void requestRenameLocked(String address, String alias) {
        Slog.d(TAG, "requestRenameLocked: address=" + address + ", alias=" + alias);
        if (alias != null) {
            alias = alias.trim();
            if (alias.isEmpty() || alias.equals(address)) {
                alias = null;
            }
        }
        WifiDisplay display = this.mPersistentDataStore.getRememberedWifiDisplay(address);
        if (display != null && !Objects.equal(display.getDeviceAlias(), alias)) {
            if (this.mPersistentDataStore.rememberWifiDisplay(new WifiDisplay(address, display.getDeviceName(), alias, false, false, false))) {
                this.mPersistentDataStore.saveIfNeeded();
                updateRememberedDisplaysLocked();
                scheduleStatusChangedBroadcastLocked();
            }
        }
        if (this.mActiveDisplay == null || !this.mActiveDisplay.getDeviceAddress().equals(address)) {
            return;
        }
        renameDisplayDeviceLocked(this.mActiveDisplay.getFriendlyDisplayName());
    }

    public void requestForgetLocked(String address) {
        Slog.d(TAG, "requestForgetLocked: address=" + address);
        if (this.mPersistentDataStore.forgetWifiDisplay(address)) {
            this.mPersistentDataStore.saveIfNeeded();
            updateRememberedDisplaysLocked();
            scheduleStatusChangedBroadcastLocked();
        }
        if (this.mActiveDisplay != null && this.mActiveDisplay.getDeviceAddress().equals(address)) {
            requestDisconnectLocked();
        } else if (this.mActiveDisplay != null) {
            Slog.e(TAG, "mActiveDisplay = " + this.mActiveDisplay);
        } else {
            Slog.e(TAG, "mActiveDisplay = null");
        }
    }

    public WifiDisplayStatus getWifiDisplayStatusLocked() {
        if (this.mCurrentStatus == null) {
            this.mCurrentStatus = new WifiDisplayStatus(this.mFeatureState, this.mScanState, this.mActiveDisplayState, this.mActiveDisplay, this.mDisplays, this.mSessionInfo);
        }
        Slog.d(TAG, "getWifiDisplayStatusLocked: result=" + this.mCurrentStatus);
        return this.mCurrentStatus;
    }

    public boolean getIfSinkEnabledLocked() {
        Slog.d(TAG, "getIfSinkEnabledLocked");
        if (this.mDisplayController != null) {
            return this.mDisplayController.getIfSinkEnabled();
        }
        return false;
    }

    public void requestEnableSinkLocked(final boolean enable) {
        Slog.d(TAG, "requestEnableSinkLocked: enable=" + enable);
        this.mSinkEnabled = enable;
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController == null) {
                    return;
                }
                WifiDisplayAdapter.this.mDisplayController.requestEnableSink(enable);
            }
        });
    }

    public void requestWaitConnectionLocked(final Surface surface) {
        Slog.d(TAG, "requestWaitConnectionLocked");
        this.mSinkConnectRequest = true;
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (!WifiDisplayAdapter.this.mSinkConnectRequest || WifiDisplayAdapter.this.mDisplayController == null) {
                    return;
                }
                WifiDisplayAdapter.this.mDisplayController.requestWaitConnection(surface);
            }
        });
    }

    public void requestSuspendDisplayLocked(final boolean suspend, final Surface surface) {
        Slog.d(TAG, "requestSuspendSinkDisplayLocked: suspend=" + suspend);
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController == null) {
                    return;
                }
                WifiDisplayAdapter.this.mDisplayController.requestSuspendDisplay(suspend, surface);
            }
        });
    }

    public void sendUibcInputEventLocked(String input) {
        Slog.d(TAG, "sendUibcInputEvent: input=" + input);
        this.mDisplayController.sendUibcInputEvent(input);
    }

    private void updateDisplaysLocked() {
        List<WifiDisplay> displays = new ArrayList<>(this.mAvailableDisplays.length + this.mRememberedDisplays.length);
        boolean[] remembered = new boolean[this.mAvailableDisplays.length];
        for (WifiDisplay d : this.mRememberedDisplays) {
            boolean available = false;
            int i = 0;
            while (true) {
                if (i >= this.mAvailableDisplays.length) {
                    break;
                }
                if (!d.equals(this.mAvailableDisplays[i])) {
                    i++;
                } else {
                    available = true;
                    remembered[i] = true;
                    break;
                }
            }
            if (!available) {
                displays.add(new WifiDisplay(d.getDeviceAddress(), d.getDeviceName(), d.getDeviceAlias(), false, false, true));
            }
        }
        for (int i2 = 0; i2 < this.mAvailableDisplays.length; i2++) {
            WifiDisplay d2 = this.mAvailableDisplays[i2];
            displays.add(new WifiDisplay(d2.getDeviceAddress(), d2.getDeviceName(), d2.getDeviceAlias(), true, d2.canConnect(), remembered[i2]));
        }
        this.mDisplays = (WifiDisplay[]) displays.toArray(WifiDisplay.EMPTY_ARRAY);
    }

    private void updateRememberedDisplaysLocked() {
        this.mRememberedDisplays = this.mPersistentDataStore.getRememberedWifiDisplays();
        this.mActiveDisplay = this.mPersistentDataStore.applyWifiDisplayAlias(this.mActiveDisplay);
        this.mAvailableDisplays = this.mPersistentDataStore.applyWifiDisplayAliases(this.mAvailableDisplays);
        updateDisplaysLocked();
    }

    private void fixRememberedDisplayNamesFromAvailableDisplaysLocked() {
        boolean changed = false;
        for (int i = 0; i < this.mRememberedDisplays.length; i++) {
            WifiDisplay rememberedDisplay = this.mRememberedDisplays[i];
            WifiDisplay availableDisplay = findAvailableDisplayLocked(rememberedDisplay.getDeviceAddress());
            if (availableDisplay != null && !rememberedDisplay.equals(availableDisplay)) {
                Slog.d(TAG, "fixRememberedDisplayNamesFromAvailableDisplaysLocked: updating remembered display to " + availableDisplay);
                this.mRememberedDisplays[i] = availableDisplay;
                changed |= this.mPersistentDataStore.rememberWifiDisplay(availableDisplay);
            }
        }
        if (!changed) {
            return;
        }
        this.mPersistentDataStore.saveIfNeeded();
    }

    private WifiDisplay findAvailableDisplayLocked(String address) {
        for (WifiDisplay display : this.mAvailableDisplays) {
            if (display.getDeviceAddress().equals(address)) {
                return display;
            }
        }
        return null;
    }

    private void addDisplayDeviceLocked(WifiDisplay display, Surface surface, int width, int height, int flags) {
        removeDisplayDeviceLocked();
        if (this.mPersistentDataStore.rememberWifiDisplay(display)) {
            this.mPersistentDataStore.saveIfNeeded();
            updateRememberedDisplaysLocked();
            scheduleStatusChangedBroadcastLocked();
        }
        boolean secure = (flags & 1) != 0;
        int deviceFlags = 64;
        if (secure) {
            deviceFlags = 68;
            if (this.mSupportsProtectedBuffers) {
                deviceFlags = 68 | 8;
            }
        }
        if (width < height) {
            deviceFlags |= 2;
        }
        String name = display.getFriendlyDisplayName();
        String address = display.getDeviceAddress();
        IBinder displayToken = SurfaceControl.createDisplay(name, secure);
        this.mDisplayDevice = new WifiDisplayDevice(displayToken, name, width, height, 60.0f, deviceFlags, address, surface);
        sendDisplayDeviceEventLocked(this.mDisplayDevice, 1);
    }

    private void removeDisplayDeviceLocked() {
        if (this.mDisplayDevice == null) {
            return;
        }
        this.mDisplayDevice.destroyLocked();
        sendDisplayDeviceEventLocked(this.mDisplayDevice, 3);
        this.mDisplayDevice = null;
    }

    private void renameDisplayDeviceLocked(String name) {
        if (this.mDisplayDevice == null || this.mDisplayDevice.getNameLocked().equals(name)) {
            return;
        }
        this.mDisplayDevice.setNameLocked(name);
        sendDisplayDeviceEventLocked(this.mDisplayDevice, 2);
    }

    private void scheduleStatusChangedBroadcastLocked() {
        this.mCurrentStatus = null;
        if (this.mPendingStatusChangeBroadcast) {
            return;
        }
        this.mPendingStatusChangeBroadcast = true;
        this.mHandler.sendEmptyMessage(1);
    }

    private void handleSendStatusChangeBroadcast() {
        synchronized (getSyncRoot()) {
            if (!this.mPendingStatusChangeBroadcast) {
                return;
            }
            this.mPendingStatusChangeBroadcast = false;
            Intent intent = new Intent("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
            intent.addFlags(1073741824);
            intent.putExtra("android.hardware.display.extra.WIFI_DISPLAY_STATUS", (Parcelable) getWifiDisplayStatusLocked());
            getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private final class WifiDisplayDevice extends DisplayDevice {
        private final String mAddress;
        private final int mFlags;
        private final int mHeight;
        private DisplayDeviceInfo mInfo;
        private final Display.Mode mMode;
        private String mName;
        private final float mRefreshRate;
        private Surface mSurface;
        private final int mWidth;

        public WifiDisplayDevice(IBinder displayToken, String name, int width, int height, float refreshRate, int flags, String address, Surface surface) {
            super(WifiDisplayAdapter.this, displayToken, WifiDisplayAdapter.DISPLAY_NAME_PREFIX + address);
            this.mName = name;
            this.mWidth = width;
            this.mHeight = height;
            this.mRefreshRate = refreshRate;
            this.mFlags = flags;
            this.mAddress = address;
            this.mSurface = surface;
            this.mMode = WifiDisplayAdapter.createMode(width, height, refreshRate);
        }

        public void destroyLocked() {
            if (this.mSurface != null) {
                this.mSurface.release();
                this.mSurface = null;
            }
            SurfaceControl.destroyDisplay(getDisplayTokenLocked());
        }

        public void setNameLocked(String name) {
            this.mName = name;
            this.mInfo = null;
        }

        @Override
        public void performTraversalInTransactionLocked() {
            if (this.mSurface == null) {
                return;
            }
            setSurfaceInTransactionLocked(this.mSurface);
        }

        @Override
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                this.mInfo = new DisplayDeviceInfo();
                this.mInfo.name = this.mName;
                this.mInfo.uniqueId = getUniqueId();
                this.mInfo.width = this.mWidth;
                this.mInfo.height = this.mHeight;
                this.mInfo.modeId = this.mMode.getModeId();
                this.mInfo.defaultModeId = this.mMode.getModeId();
                this.mInfo.supportedModes = new Display.Mode[]{this.mMode};
                this.mInfo.presentationDeadlineNanos = 1000000000 / ((long) ((int) this.mRefreshRate));
                this.mInfo.flags = this.mFlags;
                this.mInfo.type = 3;
                this.mInfo.address = this.mAddress;
                this.mInfo.touch = 2;
                this.mInfo.setAssumedDensityForExternalDisplay(this.mWidth, this.mHeight);
            }
            return this.mInfo;
        }
    }

    private final class WifiDisplayHandler extends Handler {
        public WifiDisplayHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    WifiDisplayAdapter.this.handleSendStatusChangeBroadcast();
                    break;
            }
        }
    }

    private void handleSinkEvent(WifiDisplay display, SinkEvent event) {
        Slog.d(TAG, "handleSinkEvent(), event:" + event + ", DisplayState:" + this.mActiveDisplayState);
        this.mActiveDisplay = display;
        if (event == SinkEvent.SINK_EVENT_CONNECTING) {
            if (this.mActiveDisplayState != 1) {
                this.mActiveDisplayState = 1;
            }
        } else if (event == SinkEvent.SINK_EVENT_CONNECTION_FAILED) {
            if (this.mActiveDisplayState != 0) {
                this.mActiveDisplayState = 0;
            }
        } else if (event == SinkEvent.SINK_EVENT_CONNECTED) {
            if (this.mActiveDisplayState != 2) {
                this.mActiveDisplayState = 2;
            }
        } else if (event == SinkEvent.SINK_EVENT_DISCONNECTED && this.mActiveDisplayState != 0) {
            this.mActiveDisplayState = 0;
        }
        scheduleStatusChangedBroadcastLocked();
    }
}
