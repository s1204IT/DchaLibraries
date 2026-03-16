package com.android.server.display;

import android.R;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayEventReceiver;
import android.view.SurfaceControl;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import java.io.PrintWriter;
import java.util.Arrays;

final class LocalDisplayAdapter extends DisplayAdapter {
    private static final int[] BUILT_IN_DISPLAY_IDS_TO_SCAN = {0, 1};
    private static final String TAG = "LocalDisplayAdapter";
    private static final String UNIQUE_ID_PREFIX = "local:";
    private final SparseArray<LocalDisplayDevice> mDevices;
    private HotplugDisplayEventReceiver mHotplugReceiver;

    public LocalDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener) {
        super(syncRoot, context, handler, listener, TAG);
        this.mDevices = new SparseArray<>();
    }

    @Override
    public void registerLocked() {
        super.registerLocked();
        this.mHotplugReceiver = new HotplugDisplayEventReceiver(getHandler().getLooper());
        int[] arr$ = BUILT_IN_DISPLAY_IDS_TO_SCAN;
        for (int builtInDisplayId : arr$) {
            tryConnectDisplayLocked(builtInDisplayId);
        }
    }

    private void tryConnectDisplayLocked(int builtInDisplayId) {
        IBinder displayToken = SurfaceControl.getBuiltInDisplay(builtInDisplayId);
        if (displayToken != null) {
            SurfaceControl.PhysicalDisplayInfo[] configs = SurfaceControl.getDisplayConfigs(displayToken);
            if (configs == null) {
                Slog.w(TAG, "No valid configs found for display device " + builtInDisplayId);
                return;
            }
            int activeConfig = SurfaceControl.getActiveConfig(displayToken);
            if (activeConfig < 0) {
                Slog.w(TAG, "No active config found for display device " + builtInDisplayId);
                return;
            }
            LocalDisplayDevice device = this.mDevices.get(builtInDisplayId);
            if (device == null) {
                LocalDisplayDevice device2 = new LocalDisplayDevice(displayToken, builtInDisplayId, configs, activeConfig);
                this.mDevices.put(builtInDisplayId, device2);
                sendDisplayDeviceEventLocked(device2, 1);
            } else if (device.updatePhysicalDisplayInfoLocked(configs, activeConfig)) {
                sendDisplayDeviceEventLocked(device, 2);
            }
        }
    }

    private void tryDisconnectDisplayLocked(int builtInDisplayId) {
        LocalDisplayDevice device = this.mDevices.get(builtInDisplayId);
        if (device != null) {
            this.mDevices.remove(builtInDisplayId);
            sendDisplayDeviceEventLocked(device, 3);
        }
    }

    static int getPowerModeForState(int state) {
        switch (state) {
            case 1:
                return 0;
            case 2:
            default:
                return 2;
            case 3:
                return 1;
            case 4:
                return 3;
        }
    }

    private final class LocalDisplayDevice extends DisplayDevice {
        private final int mBuiltInDisplayId;
        private final int mDefaultPhysicalDisplayInfo;
        private boolean mHavePendingChanges;
        private DisplayDeviceInfo mInfo;
        private float mLastRequestedRefreshRate;
        private final SurfaceControl.PhysicalDisplayInfo mPhys;
        private int[] mRefreshRateConfigIndices;
        private int mState;
        private float[] mSupportedRefreshRates;

        public LocalDisplayDevice(IBinder displayToken, int builtInDisplayId, SurfaceControl.PhysicalDisplayInfo[] physicalDisplayInfos, int activeDisplayInfo) {
            super(LocalDisplayAdapter.this, displayToken, LocalDisplayAdapter.UNIQUE_ID_PREFIX + builtInDisplayId);
            this.mState = 0;
            this.mBuiltInDisplayId = builtInDisplayId;
            this.mPhys = new SurfaceControl.PhysicalDisplayInfo(physicalDisplayInfos[activeDisplayInfo]);
            this.mDefaultPhysicalDisplayInfo = activeDisplayInfo;
            updateSupportedRefreshRatesLocked(physicalDisplayInfos, this.mPhys);
        }

        public boolean updatePhysicalDisplayInfoLocked(SurfaceControl.PhysicalDisplayInfo[] physicalDisplayInfos, int activeDisplayInfo) {
            SurfaceControl.PhysicalDisplayInfo newPhys = physicalDisplayInfos[activeDisplayInfo];
            if (this.mPhys.equals(newPhys)) {
                return false;
            }
            this.mPhys.copyFrom(newPhys);
            updateSupportedRefreshRatesLocked(physicalDisplayInfos, this.mPhys);
            this.mHavePendingChanges = true;
            return true;
        }

        @Override
        public void applyPendingDisplayDeviceInfoChangesLocked() {
            if (this.mHavePendingChanges) {
                this.mInfo = null;
                this.mHavePendingChanges = false;
            }
        }

        @Override
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                this.mInfo = new DisplayDeviceInfo();
                this.mInfo.width = this.mPhys.width;
                this.mInfo.height = this.mPhys.height;
                this.mInfo.refreshRate = this.mPhys.refreshRate;
                this.mInfo.supportedRefreshRates = this.mSupportedRefreshRates;
                this.mInfo.appVsyncOffsetNanos = this.mPhys.appVsyncOffsetNanos;
                this.mInfo.presentationDeadlineNanos = this.mPhys.presentationDeadlineNanos;
                this.mInfo.state = this.mState;
                this.mInfo.uniqueId = getUniqueId();
                if (this.mPhys.secure) {
                    this.mInfo.flags = 12;
                }
                if (this.mBuiltInDisplayId == 0) {
                    this.mInfo.name = LocalDisplayAdapter.this.getContext().getResources().getString(R.string.mediasize_chinese_om_dai_pa_kai);
                    this.mInfo.flags |= 3;
                    this.mInfo.type = 1;
                    this.mInfo.densityDpi = (int) ((this.mPhys.density * 160.0f) + 0.5f);
                    this.mInfo.xDpi = this.mPhys.xDpi;
                    this.mInfo.yDpi = this.mPhys.yDpi;
                    this.mInfo.touch = 1;
                } else {
                    this.mInfo.type = 2;
                    this.mInfo.flags |= 64;
                    this.mInfo.name = LocalDisplayAdapter.this.getContext().getResources().getString(R.string.mediasize_chinese_om_jurro_ku_kai);
                    this.mInfo.touch = 2;
                    this.mInfo.setAssumedDensityForExternalDisplay(this.mPhys.width, this.mPhys.height);
                    if ("portrait".equals(SystemProperties.get("persist.demo.hdmirotation"))) {
                        this.mInfo.rotation = 3;
                    }
                    if (SystemProperties.getBoolean("persist.demo.hdmirotates", false)) {
                        this.mInfo.flags |= 2;
                    }
                }
            }
            return this.mInfo;
        }

        @Override
        public Runnable requestDisplayStateLocked(final int state) {
            if (this.mState == state) {
                return null;
            }
            final int displayId = this.mBuiltInDisplayId;
            final IBinder token = getDisplayTokenLocked();
            final int mode = LocalDisplayAdapter.getPowerModeForState(state);
            this.mState = state;
            updateDeviceInfoLocked();
            return new Runnable() {
                @Override
                public void run() {
                    Trace.traceBegin(131072L, "requestDisplayState(" + Display.stateToString(state) + ", id=" + displayId + ")");
                    try {
                        SurfaceControl.setDisplayPowerMode(token, mode);
                    } finally {
                        Trace.traceEnd(131072L);
                    }
                }
            };
        }

        @Override
        public void requestRefreshRateLocked(float refreshRate) {
            if (this.mLastRequestedRefreshRate != refreshRate) {
                this.mLastRequestedRefreshRate = refreshRate;
                if (refreshRate != 0.0f) {
                    int N = this.mSupportedRefreshRates.length;
                    for (int i = 0; i < N; i++) {
                        if (refreshRate == this.mSupportedRefreshRates[i]) {
                            int configIndex = this.mRefreshRateConfigIndices[i];
                            SurfaceControl.setActiveConfig(getDisplayTokenLocked(), configIndex);
                            return;
                        }
                    }
                    Slog.w(LocalDisplayAdapter.TAG, "Requested refresh rate " + refreshRate + " is unsupported.");
                }
                SurfaceControl.setActiveConfig(getDisplayTokenLocked(), this.mDefaultPhysicalDisplayInfo);
            }
        }

        @Override
        public void dumpLocked(PrintWriter pw) {
            super.dumpLocked(pw);
            pw.println("mBuiltInDisplayId=" + this.mBuiltInDisplayId);
            pw.println("mPhys=" + this.mPhys);
            pw.println("mState=" + Display.stateToString(this.mState));
        }

        private void updateDeviceInfoLocked() {
            this.mInfo = null;
            LocalDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
        }

        private void updateSupportedRefreshRatesLocked(SurfaceControl.PhysicalDisplayInfo[] physicalDisplayInfos, SurfaceControl.PhysicalDisplayInfo activePhys) {
            int idx;
            int N = physicalDisplayInfos.length;
            this.mSupportedRefreshRates = new float[N];
            this.mRefreshRateConfigIndices = new int[N];
            int i = 0;
            int idx2 = 0;
            while (i < N) {
                SurfaceControl.PhysicalDisplayInfo phys = physicalDisplayInfos[i];
                if (activePhys.width == phys.width && activePhys.height == phys.height && activePhys.density == phys.density && activePhys.xDpi == phys.xDpi && activePhys.yDpi == phys.yDpi) {
                    this.mSupportedRefreshRates[idx2] = phys.refreshRate;
                    idx = idx2 + 1;
                    this.mRefreshRateConfigIndices[idx2] = i;
                } else {
                    idx = idx2;
                }
                i++;
                idx2 = idx;
            }
            if (idx2 != N) {
                this.mSupportedRefreshRates = Arrays.copyOfRange(this.mSupportedRefreshRates, 0, idx2);
                this.mRefreshRateConfigIndices = Arrays.copyOfRange(this.mRefreshRateConfigIndices, 0, idx2);
            }
        }
    }

    private final class HotplugDisplayEventReceiver extends DisplayEventReceiver {
        public HotplugDisplayEventReceiver(Looper looper) {
            super(looper);
        }

        public void onHotplug(long timestampNanos, int builtInDisplayId, boolean connected) {
            synchronized (LocalDisplayAdapter.this.getSyncRoot()) {
                if (connected) {
                    LocalDisplayAdapter.this.tryConnectDisplayLocked(builtInDisplayId);
                } else {
                    LocalDisplayAdapter.this.tryDisconnectDisplayLocked(builtInDisplayId);
                }
            }
        }
    }
}
