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
import com.android.server.backup.BackupManagerConstants;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.mediatek.server.display.MtkWifiDisplayController;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

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
    private MtkWifiDisplayController mDisplayController;
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
    private final MtkWifiDisplayController.Listener mWifiDisplayListener;

    enum SinkEvent {
        SINK_EVENT_CONNECTING,
        SINK_EVENT_CONNECTION_FAILED,
        SINK_EVENT_CONNECTED,
        SINK_EVENT_DISCONNECTED
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
                if (intent.getAction().equals(WifiDisplayAdapter.ACTION_DISCONNECT)) {
                    synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                        WifiDisplayAdapter.this.requestDisconnectLocked();
                    }
                }
            }
        };
        this.mWifiDisplayListener = new MtkWifiDisplayController.Listener() {
            @Override
            public void onFeatureStateChanged(int i) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mFeatureState != i) {
                        WifiDisplayAdapter.this.mFeatureState = i;
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
            public void onScanResults(WifiDisplay[] wifiDisplayArr) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplay[] wifiDisplayArrApplyWifiDisplayAliases = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAliases(wifiDisplayArr);
                    boolean z = !Arrays.equals(WifiDisplayAdapter.this.mAvailableDisplays, wifiDisplayArrApplyWifiDisplayAliases);
                    for (int i = 0; !z && i < wifiDisplayArrApplyWifiDisplayAliases.length; i++) {
                        z = wifiDisplayArrApplyWifiDisplayAliases[i].canConnect() != WifiDisplayAdapter.this.mAvailableDisplays[i].canConnect();
                    }
                    if (z) {
                        WifiDisplayAdapter.this.mAvailableDisplays = wifiDisplayArrApplyWifiDisplayAliases;
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
            public void onDisplayConnecting(WifiDisplay wifiDisplay) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mSinkEnabled) {
                        WifiDisplayAdapter.this.handleSinkEvent(wifiDisplay, SinkEvent.SINK_EVENT_CONNECTING);
                        return;
                    }
                    WifiDisplay wifiDisplayApplyWifiDisplayAlias = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAlias(wifiDisplay);
                    if (WifiDisplayAdapter.this.mActiveDisplayState != 1 || WifiDisplayAdapter.this.mActiveDisplay == null || !WifiDisplayAdapter.this.mActiveDisplay.equals(wifiDisplayApplyWifiDisplayAlias)) {
                        WifiDisplayAdapter.this.mActiveDisplayState = 1;
                        WifiDisplayAdapter.this.mActiveDisplay = wifiDisplayApplyWifiDisplayAlias;
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
            public void onDisplayConnected(WifiDisplay wifiDisplay, Surface surface, int i, int i2, int i3) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    if (WifiDisplayAdapter.this.mSinkEnabled) {
                        WifiDisplayAdapter.this.handleSinkEvent(wifiDisplay, SinkEvent.SINK_EVENT_CONNECTED);
                        return;
                    }
                    WifiDisplay wifiDisplayApplyWifiDisplayAlias = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAlias(wifiDisplay);
                    WifiDisplayAdapter.this.addDisplayDeviceLocked(wifiDisplayApplyWifiDisplayAlias, surface, i, i2, i3);
                    if (WifiDisplayAdapter.this.mActiveDisplayState != 2 || WifiDisplayAdapter.this.mActiveDisplay == null || !WifiDisplayAdapter.this.mActiveDisplay.equals(wifiDisplayApplyWifiDisplayAlias)) {
                        WifiDisplayAdapter.this.mActiveDisplayState = 2;
                        WifiDisplayAdapter.this.mActiveDisplay = wifiDisplayApplyWifiDisplayAlias;
                        WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                    }
                }
            }

            @Override
            public void onDisplaySessionInfo(WifiDisplaySessionInfo wifiDisplaySessionInfo) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplayAdapter.this.mSessionInfo = wifiDisplaySessionInfo;
                    WifiDisplayAdapter.this.scheduleStatusChangedBroadcastLocked();
                }
            }

            @Override
            public void onDisplayChanged(WifiDisplay wifiDisplay) {
                synchronized (WifiDisplayAdapter.this.getSyncRoot()) {
                    WifiDisplay wifiDisplayApplyWifiDisplayAlias = WifiDisplayAdapter.this.mPersistentDataStore.applyWifiDisplayAlias(wifiDisplay);
                    if (WifiDisplayAdapter.this.mActiveDisplay != null && WifiDisplayAdapter.this.mActiveDisplay.hasSameAddress(wifiDisplayApplyWifiDisplayAlias) && !WifiDisplayAdapter.this.mActiveDisplay.equals(wifiDisplayApplyWifiDisplayAlias)) {
                        WifiDisplayAdapter.this.mActiveDisplay = wifiDisplayApplyWifiDisplayAlias;
                        WifiDisplayAdapter.this.renameDisplayDeviceLocked(wifiDisplayApplyWifiDisplayAlias.getFriendlyDisplayName());
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
                if (true == WifiDisplayAdapter.this.mInDisconnectingThread) {
                    Slog.e(WifiDisplayAdapter.TAG, "still in WfdDisConnThread!");
                }
            }
        };
        this.mHandler = new WifiDisplayHandler(handler.getLooper());
        this.mPersistentDataStore = persistentDataStore;
        this.mSupportsProtectedBuffers = context.getResources().getBoolean(R.^attr-private.preferenceActivityStyle);
    }

    @Override
    public void dumpLocked(PrintWriter printWriter) {
        super.dumpLocked(printWriter);
        printWriter.println("mCurrentStatus=" + getWifiDisplayStatusLocked());
        printWriter.println("mFeatureState=" + this.mFeatureState);
        printWriter.println("mScanState=" + this.mScanState);
        printWriter.println("mActiveDisplayState=" + this.mActiveDisplayState);
        printWriter.println("mActiveDisplay=" + this.mActiveDisplay);
        printWriter.println("mDisplays=" + Arrays.toString(this.mDisplays));
        printWriter.println("mAvailableDisplays=" + Arrays.toString(this.mAvailableDisplays));
        printWriter.println("mRememberedDisplays=" + Arrays.toString(this.mRememberedDisplays));
        printWriter.println("mPendingStatusChangeBroadcast=" + this.mPendingStatusChangeBroadcast);
        printWriter.println("mSupportsProtectedBuffers=" + this.mSupportsProtectedBuffers);
        if (this.mDisplayController == null) {
            printWriter.println("mDisplayController=null");
            return;
        }
        printWriter.println("mDisplayController:");
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.increaseIndent();
        DumpUtils.dumpAsync(getHandler(), this.mDisplayController, indentingPrintWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 200L);
    }

    @Override
    public void registerLocked() {
        super.registerLocked();
        updateRememberedDisplaysLocked();
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                WifiDisplayAdapter.this.mDisplayController = new MtkWifiDisplayController(WifiDisplayAdapter.this.getContext(), WifiDisplayAdapter.this.getHandler(), WifiDisplayAdapter.this.mWifiDisplayListener);
                WifiDisplayAdapter.this.getContext().registerReceiverAsUser(WifiDisplayAdapter.this.mBroadcastReceiver, UserHandle.ALL, new IntentFilter(WifiDisplayAdapter.ACTION_DISCONNECT), null, WifiDisplayAdapter.this.mHandler);
            }
        });
    }

    public void requestStartScanLocked() {
        Slog.d(TAG, "requestStartScanLocked");
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestStartScan();
                }
            }
        });
    }

    public void requestStopScanLocked() {
        Slog.d(TAG, "requestStopScanLocked");
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestStopScan();
                }
            }
        });
    }

    public void requestConnectLocked(final String str) {
        Slog.d(TAG, "requestConnectLocked: address=" + str);
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestConnect(str);
                }
            }
        });
    }

    public void requestPauseLocked() {
        Slog.d(TAG, "requestPauseLocked");
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestPause();
                }
            }
        });
    }

    public void requestResumeLocked() {
        Slog.d(TAG, "requestResumeLocked");
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestResume();
                }
            }
        });
    }

    public void requestDisconnectLocked() {
        Slog.d(TAG, "requestDisconnectedLocked");
        if (SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1") && this.mSinkEnabled) {
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
                    if (WifiDisplayAdapter.this.mDisplayController != null) {
                        WifiDisplayAdapter.this.mDisplayController.requestDisconnect();
                    }
                }
            });
        }
        Slog.d(TAG, "requestDisconnectedLocked return");
    }

    public void requestRenameLocked(String str, String str2) {
        Slog.d(TAG, "requestRenameLocked: address=" + str + ", alias=" + str2);
        if (str2 != null) {
            str2 = str2.trim();
            if (str2.isEmpty() || str2.equals(str)) {
                str2 = null;
            }
        }
        String str3 = str2;
        WifiDisplay rememberedWifiDisplay = this.mPersistentDataStore.getRememberedWifiDisplay(str);
        if (rememberedWifiDisplay != null && !Objects.equals(rememberedWifiDisplay.getDeviceAlias(), str3)) {
            if (this.mPersistentDataStore.rememberWifiDisplay(new WifiDisplay(str, rememberedWifiDisplay.getDeviceName(), str3, false, false, false))) {
                this.mPersistentDataStore.saveIfNeeded();
                updateRememberedDisplaysLocked();
                scheduleStatusChangedBroadcastLocked();
            }
        }
        if (this.mActiveDisplay != null && this.mActiveDisplay.getDeviceAddress().equals(str)) {
            renameDisplayDeviceLocked(this.mActiveDisplay.getFriendlyDisplayName());
        }
    }

    public void requestForgetLocked(String str) {
        Slog.d(TAG, "requestForgetLocked: address=" + str);
        if (this.mPersistentDataStore.forgetWifiDisplay(str)) {
            this.mPersistentDataStore.saveIfNeeded();
            updateRememberedDisplaysLocked();
            scheduleStatusChangedBroadcastLocked();
        }
        if (this.mActiveDisplay != null && this.mActiveDisplay.getDeviceAddress().equals(str)) {
            requestDisconnectLocked();
            return;
        }
        if (this.mActiveDisplay != null) {
            Slog.e(TAG, "mActiveDisplay = " + this.mActiveDisplay);
            return;
        }
        Slog.e(TAG, "mActiveDisplay = null");
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

    public void requestEnableSinkLocked(final boolean z) {
        Slog.d(TAG, "requestEnableSinkLocked: enable=" + z);
        this.mSinkEnabled = z;
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestEnableSink(z);
                }
            }
        });
    }

    public void requestWaitConnectionLocked(final Surface surface) {
        Slog.d(TAG, "requestWaitConnectionLocked");
        this.mSinkConnectRequest = true;
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mSinkConnectRequest && WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestWaitConnection(surface);
                }
            }
        });
    }

    public void requestSuspendDisplayLocked(final boolean z, final Surface surface) {
        Slog.d(TAG, "requestSuspendSinkDisplayLocked: suspend=" + z);
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (WifiDisplayAdapter.this.mDisplayController != null) {
                    WifiDisplayAdapter.this.mDisplayController.requestSuspendDisplay(z, surface);
                }
            }
        });
    }

    public void sendUibcInputEventLocked(String str) {
        Slog.d(TAG, "sendUibcInputEvent: input=" + str);
        this.mDisplayController.sendUibcInputEvent(str);
    }

    private void updateDisplaysLocked() {
        boolean z;
        ArrayList arrayList = new ArrayList(this.mAvailableDisplays.length + this.mRememberedDisplays.length);
        boolean[] zArr = new boolean[this.mAvailableDisplays.length];
        for (WifiDisplay wifiDisplay : this.mRememberedDisplays) {
            int i = 0;
            while (true) {
                z = true;
                if (i < this.mAvailableDisplays.length) {
                    if (!wifiDisplay.equals(this.mAvailableDisplays[i])) {
                        i++;
                    } else {
                        zArr[i] = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                arrayList.add(new WifiDisplay(wifiDisplay.getDeviceAddress(), wifiDisplay.getDeviceName(), wifiDisplay.getDeviceAlias(), false, false, true));
            }
        }
        for (int i2 = 0; i2 < this.mAvailableDisplays.length; i2++) {
            WifiDisplay wifiDisplay2 = this.mAvailableDisplays[i2];
            arrayList.add(new WifiDisplay(wifiDisplay2.getDeviceAddress(), wifiDisplay2.getDeviceName(), wifiDisplay2.getDeviceAlias(), true, wifiDisplay2.canConnect(), zArr[i2]));
        }
        this.mDisplays = (WifiDisplay[]) arrayList.toArray(WifiDisplay.EMPTY_ARRAY);
    }

    private void updateRememberedDisplaysLocked() {
        this.mRememberedDisplays = this.mPersistentDataStore.getRememberedWifiDisplays();
        this.mActiveDisplay = this.mPersistentDataStore.applyWifiDisplayAlias(this.mActiveDisplay);
        this.mAvailableDisplays = this.mPersistentDataStore.applyWifiDisplayAliases(this.mAvailableDisplays);
        updateDisplaysLocked();
    }

    private void fixRememberedDisplayNamesFromAvailableDisplaysLocked() {
        boolean zRememberWifiDisplay = false;
        for (int i = 0; i < this.mRememberedDisplays.length; i++) {
            WifiDisplay wifiDisplay = this.mRememberedDisplays[i];
            WifiDisplay wifiDisplayFindAvailableDisplayLocked = findAvailableDisplayLocked(wifiDisplay.getDeviceAddress());
            if (wifiDisplayFindAvailableDisplayLocked != null && !wifiDisplay.equals(wifiDisplayFindAvailableDisplayLocked)) {
                Slog.d(TAG, "fixRememberedDisplayNamesFromAvailableDisplaysLocked: updating remembered display to " + wifiDisplayFindAvailableDisplayLocked);
                this.mRememberedDisplays[i] = wifiDisplayFindAvailableDisplayLocked;
                zRememberWifiDisplay |= this.mPersistentDataStore.rememberWifiDisplay(wifiDisplayFindAvailableDisplayLocked);
            }
        }
        if (zRememberWifiDisplay) {
            this.mPersistentDataStore.saveIfNeeded();
        }
    }

    private WifiDisplay findAvailableDisplayLocked(String str) {
        for (WifiDisplay wifiDisplay : this.mAvailableDisplays) {
            if (wifiDisplay.getDeviceAddress().equals(str)) {
                return wifiDisplay;
            }
        }
        return null;
    }

    private void addDisplayDeviceLocked(WifiDisplay wifiDisplay, Surface surface, int i, int i2, int i3) {
        boolean z;
        removeDisplayDeviceLocked();
        if (this.mPersistentDataStore.rememberWifiDisplay(wifiDisplay)) {
            this.mPersistentDataStore.saveIfNeeded();
            updateRememberedDisplaysLocked();
            scheduleStatusChangedBroadcastLocked();
        }
        if ((i3 & 1) == 0) {
            z = false;
        } else {
            z = true;
        }
        int i4 = 64;
        if (z) {
            i4 = 68;
            if (this.mSupportsProtectedBuffers) {
                i4 = 76;
            }
        }
        if (i < i2) {
            i4 |= 2;
        }
        String friendlyDisplayName = wifiDisplay.getFriendlyDisplayName();
        this.mDisplayDevice = new WifiDisplayDevice(SurfaceControl.createDisplay(friendlyDisplayName, z), friendlyDisplayName, i, i2, 60.0f, i4, wifiDisplay.getDeviceAddress(), surface);
        sendDisplayDeviceEventLocked(this.mDisplayDevice, 1);
    }

    private void removeDisplayDeviceLocked() {
        if (this.mDisplayDevice != null) {
            this.mDisplayDevice.destroyLocked();
            sendDisplayDeviceEventLocked(this.mDisplayDevice, 3);
            this.mDisplayDevice = null;
        }
    }

    private void renameDisplayDeviceLocked(String str) {
        if (this.mDisplayDevice != null && !this.mDisplayDevice.getNameLocked().equals(str)) {
            this.mDisplayDevice.setNameLocked(str);
            sendDisplayDeviceEventLocked(this.mDisplayDevice, 2);
        }
    }

    private void scheduleStatusChangedBroadcastLocked() {
        this.mCurrentStatus = null;
        if (!this.mPendingStatusChangeBroadcast) {
            this.mPendingStatusChangeBroadcast = true;
            this.mHandler.sendEmptyMessage(1);
        }
    }

    private void handleSendStatusChangeBroadcast() {
        synchronized (getSyncRoot()) {
            if (this.mPendingStatusChangeBroadcast) {
                this.mPendingStatusChangeBroadcast = false;
                Intent intent = new Intent("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
                intent.addFlags(1073741824);
                intent.putExtra("android.hardware.display.extra.WIFI_DISPLAY_STATUS", (Parcelable) getWifiDisplayStatusLocked());
                getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
            }
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

        public WifiDisplayDevice(IBinder iBinder, String str, int i, int i2, float f, int i3, String str2, Surface surface) {
            super(WifiDisplayAdapter.this, iBinder, WifiDisplayAdapter.DISPLAY_NAME_PREFIX + str2);
            this.mName = str;
            this.mWidth = i;
            this.mHeight = i2;
            this.mRefreshRate = f;
            this.mFlags = i3;
            this.mAddress = str2;
            this.mSurface = surface;
            this.mMode = DisplayAdapter.createMode(i, i2, f);
        }

        @Override
        public boolean hasStableUniqueId() {
            return true;
        }

        public void destroyLocked() {
            if (this.mSurface != null) {
                this.mSurface.release();
                this.mSurface = null;
            }
            SurfaceControl.destroyDisplay(getDisplayTokenLocked());
        }

        public void setNameLocked(String str) {
            this.mName = str;
            this.mInfo = null;
        }

        @Override
        public void performTraversalLocked(SurfaceControl.Transaction transaction) {
            if (this.mSurface != null) {
                setSurfaceLocked(transaction, this.mSurface);
            }
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
        public void handleMessage(Message message) {
            if (message.what == 1) {
                WifiDisplayAdapter.this.handleSendStatusChangeBroadcast();
            }
        }
    }

    private void handleSinkEvent(WifiDisplay wifiDisplay, SinkEvent sinkEvent) {
        Slog.d(TAG, "handleSinkEvent(), event:" + sinkEvent + ", DisplayState:" + this.mActiveDisplayState);
        this.mActiveDisplay = wifiDisplay;
        if (sinkEvent == SinkEvent.SINK_EVENT_CONNECTING) {
            if (this.mActiveDisplayState != 1) {
                this.mActiveDisplayState = 1;
            }
        } else if (sinkEvent == SinkEvent.SINK_EVENT_CONNECTION_FAILED) {
            if (this.mActiveDisplayState != 0) {
                this.mActiveDisplayState = 0;
            }
        } else if (sinkEvent == SinkEvent.SINK_EVENT_CONNECTED) {
            if (this.mActiveDisplayState != 2) {
                this.mActiveDisplayState = 2;
            }
        } else if (sinkEvent == SinkEvent.SINK_EVENT_DISCONNECTED && this.mActiveDisplayState != 0) {
            this.mActiveDisplayState = 0;
        }
        scheduleStatusChangedBroadcastLocked();
    }
}
