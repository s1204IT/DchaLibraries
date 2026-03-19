package com.android.server.display;

import android.R;
import android.app.ActivityThread;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.sidekick.SidekickInternal;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.DisplayEventReceiver;
import android.view.SurfaceControl;
import com.android.server.LocalServices;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

final class LocalDisplayAdapter extends DisplayAdapter {
    private static final boolean DEBUG = false;
    private static final String PROPERTY_EMULATOR_CIRCULAR = "ro.emulator.circular";
    private static final String TAG = "LocalDisplayAdapter";
    private static final String UNIQUE_ID_PREFIX = "local:";
    private final SparseArray<LocalDisplayDevice> mDevices;
    private HotplugDisplayEventReceiver mHotplugReceiver;
    private static final boolean MTK_DEBUG = "eng".equals(Build.TYPE);
    private static final int[] BUILT_IN_DISPLAY_IDS_TO_SCAN = {0, 1};

    public LocalDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener) {
        super(syncRoot, context, handler, listener, TAG);
        this.mDevices = new SparseArray<>();
    }

    @Override
    public void registerLocked() {
        super.registerLocked();
        this.mHotplugReceiver = new HotplugDisplayEventReceiver(getHandler().getLooper());
        for (int i : BUILT_IN_DISPLAY_IDS_TO_SCAN) {
            tryConnectDisplayLocked(i);
        }
    }

    private void tryConnectDisplayLocked(int i) {
        IBinder builtInDisplay = SurfaceControl.getBuiltInDisplay(i);
        if (builtInDisplay != null) {
            SurfaceControl.PhysicalDisplayInfo[] displayConfigs = SurfaceControl.getDisplayConfigs(builtInDisplay);
            if (displayConfigs == null) {
                Slog.w(TAG, "No valid configs found for display device " + i);
                return;
            }
            int activeConfig = SurfaceControl.getActiveConfig(builtInDisplay);
            if (activeConfig < 0) {
                Slog.w(TAG, "No active config found for display device " + i);
                return;
            }
            int activeColorMode = SurfaceControl.getActiveColorMode(builtInDisplay);
            if (activeColorMode < 0) {
                Slog.w(TAG, "Unable to get active color mode for display device " + i);
                activeColorMode = -1;
            }
            int i2 = activeColorMode;
            int[] displayColorModes = SurfaceControl.getDisplayColorModes(builtInDisplay);
            LocalDisplayDevice localDisplayDevice = this.mDevices.get(i);
            if (localDisplayDevice == null) {
                LocalDisplayDevice localDisplayDevice2 = new LocalDisplayDevice(builtInDisplay, i, displayConfigs, activeConfig, displayColorModes, i2);
                this.mDevices.put(i, localDisplayDevice2);
                sendDisplayDeviceEventLocked(localDisplayDevice2, 1);
            } else if (localDisplayDevice.updatePhysicalDisplayInfoLocked(displayConfigs, activeConfig, displayColorModes, i2)) {
                sendDisplayDeviceEventLocked(localDisplayDevice, 2);
            }
        }
    }

    private void tryDisconnectDisplayLocked(int i) {
        LocalDisplayDevice localDisplayDevice = this.mDevices.get(i);
        if (localDisplayDevice != null) {
            this.mDevices.remove(i);
            sendDisplayDeviceEventLocked(localDisplayDevice, 3);
        }
    }

    static int getPowerModeForState(int i) {
        if (i == 1) {
            return 0;
        }
        if (i != 6) {
            switch (i) {
                case 3:
                    return 1;
                case 4:
                    return 3;
                default:
                    return 2;
            }
        }
        return 4;
    }

    private final class LocalDisplayDevice extends DisplayDevice {
        static final boolean $assertionsDisabled = false;
        private int mActiveColorMode;
        private boolean mActiveColorModeInvalid;
        private int mActiveModeId;
        private boolean mActiveModeInvalid;
        private int mActivePhysIndex;
        private final Light mBacklight;
        private int mBrightness;
        private final int mBuiltInDisplayId;
        private int mDefaultModeId;
        private SurfaceControl.PhysicalDisplayInfo[] mDisplayInfos;
        private boolean mHavePendingChanges;
        private Display.HdrCapabilities mHdrCapabilities;
        private DisplayDeviceInfo mInfo;
        private boolean mSidekickActive;
        private SidekickInternal mSidekickInternal;
        private int mState;
        private final ArrayList<Integer> mSupportedColorModes;
        private final SparseArray<DisplayModeRecord> mSupportedModes;

        public LocalDisplayDevice(IBinder iBinder, int i, SurfaceControl.PhysicalDisplayInfo[] physicalDisplayInfoArr, int i2, int[] iArr, int i3) {
            super(LocalDisplayAdapter.this, iBinder, LocalDisplayAdapter.UNIQUE_ID_PREFIX + i);
            this.mSupportedModes = new SparseArray<>();
            this.mSupportedColorModes = new ArrayList<>();
            this.mState = 0;
            this.mBrightness = -1;
            this.mBuiltInDisplayId = i;
            updatePhysicalDisplayInfoLocked(physicalDisplayInfoArr, i2, iArr, i3);
            updateColorModesLocked(iArr, i3);
            this.mSidekickInternal = (SidekickInternal) LocalServices.getService(SidekickInternal.class);
            if (this.mBuiltInDisplayId == 0) {
                this.mBacklight = ((LightsManager) LocalServices.getService(LightsManager.class)).getLight(0);
            } else {
                this.mBacklight = null;
            }
            this.mHdrCapabilities = SurfaceControl.getHdrCapabilities(iBinder);
        }

        @Override
        public boolean hasStableUniqueId() {
            return true;
        }

        public boolean updatePhysicalDisplayInfoLocked(SurfaceControl.PhysicalDisplayInfo[] physicalDisplayInfoArr, int i, int[] iArr, int i2) {
            boolean z;
            this.mDisplayInfos = (SurfaceControl.PhysicalDisplayInfo[]) Arrays.copyOf(physicalDisplayInfoArr, physicalDisplayInfoArr.length);
            this.mActivePhysIndex = i;
            ArrayList<DisplayModeRecord> arrayList = new ArrayList();
            boolean z2 = false;
            for (SurfaceControl.PhysicalDisplayInfo physicalDisplayInfo : physicalDisplayInfoArr) {
                int i3 = 0;
                while (true) {
                    if (i3 < arrayList.size()) {
                        if (!((DisplayModeRecord) arrayList.get(i3)).hasMatchingMode(physicalDisplayInfo)) {
                            i3++;
                        } else {
                            z = true;
                            break;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
                if (!z) {
                    DisplayModeRecord displayModeRecordFindDisplayModeRecord = findDisplayModeRecord(physicalDisplayInfo);
                    if (displayModeRecordFindDisplayModeRecord == null) {
                        displayModeRecordFindDisplayModeRecord = new DisplayModeRecord(physicalDisplayInfo);
                        z2 = true;
                    }
                    arrayList.add(displayModeRecordFindDisplayModeRecord);
                }
            }
            DisplayModeRecord displayModeRecord = null;
            int i4 = 0;
            while (true) {
                if (i4 >= arrayList.size()) {
                    break;
                }
                DisplayModeRecord displayModeRecord2 = (DisplayModeRecord) arrayList.get(i4);
                if (!displayModeRecord2.hasMatchingMode(physicalDisplayInfoArr[i])) {
                    i4++;
                } else {
                    displayModeRecord = displayModeRecord2;
                    break;
                }
            }
            if (this.mActiveModeId != 0 && this.mActiveModeId != displayModeRecord.mMode.getModeId()) {
                this.mActiveModeInvalid = true;
                LocalDisplayAdapter.this.sendTraversalRequestLocked();
            }
            if (!(arrayList.size() != this.mSupportedModes.size() || z2)) {
                return false;
            }
            this.mHavePendingChanges = true;
            this.mSupportedModes.clear();
            for (DisplayModeRecord displayModeRecord3 : arrayList) {
                this.mSupportedModes.put(displayModeRecord3.mMode.getModeId(), displayModeRecord3);
            }
            if (findDisplayInfoIndexLocked(this.mDefaultModeId) < 0) {
                if (this.mDefaultModeId != 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Default display mode no longer available, using currently active mode as default.");
                }
                this.mDefaultModeId = displayModeRecord.mMode.getModeId();
            }
            if (this.mSupportedModes.indexOfKey(this.mActiveModeId) < 0) {
                if (this.mActiveModeId != 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Active display mode no longer available, reverting to default mode.");
                }
                this.mActiveModeId = this.mDefaultModeId;
                this.mActiveModeInvalid = true;
            }
            LocalDisplayAdapter.this.sendTraversalRequestLocked();
            return true;
        }

        private boolean updateColorModesLocked(int[] iArr, int i) {
            ArrayList arrayList = new ArrayList();
            if (iArr == null) {
                return false;
            }
            boolean z = false;
            for (int i2 : iArr) {
                if (!this.mSupportedColorModes.contains(Integer.valueOf(i2))) {
                    z = true;
                }
                arrayList.add(Integer.valueOf(i2));
            }
            if (!(arrayList.size() != this.mSupportedColorModes.size() || z)) {
                return false;
            }
            this.mHavePendingChanges = true;
            this.mSupportedColorModes.clear();
            this.mSupportedColorModes.addAll(arrayList);
            Collections.sort(this.mSupportedColorModes);
            if (!this.mSupportedColorModes.contains(Integer.valueOf(this.mActiveColorMode))) {
                if (this.mActiveColorMode != 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Active color mode no longer available, reverting to default mode.");
                    this.mActiveColorMode = 0;
                    this.mActiveColorModeInvalid = true;
                } else if (!this.mSupportedColorModes.isEmpty()) {
                    Slog.e(LocalDisplayAdapter.TAG, "Default and active color mode is no longer available! Reverting to first available mode.");
                    this.mActiveColorMode = this.mSupportedColorModes.get(0).intValue();
                    this.mActiveColorModeInvalid = true;
                } else {
                    Slog.e(LocalDisplayAdapter.TAG, "No color modes available!");
                }
            }
            return true;
        }

        private DisplayModeRecord findDisplayModeRecord(SurfaceControl.PhysicalDisplayInfo physicalDisplayInfo) {
            for (int i = 0; i < this.mSupportedModes.size(); i++) {
                DisplayModeRecord displayModeRecordValueAt = this.mSupportedModes.valueAt(i);
                if (displayModeRecordValueAt.hasMatchingMode(physicalDisplayInfo)) {
                    return displayModeRecordValueAt;
                }
            }
            return null;
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
                SurfaceControl.PhysicalDisplayInfo physicalDisplayInfo = this.mDisplayInfos[this.mActivePhysIndex];
                this.mInfo = new DisplayDeviceInfo();
                this.mInfo.width = physicalDisplayInfo.width;
                this.mInfo.height = physicalDisplayInfo.height;
                this.mInfo.modeId = this.mActiveModeId;
                this.mInfo.defaultModeId = this.mDefaultModeId;
                this.mInfo.supportedModes = new Display.Mode[this.mSupportedModes.size()];
                for (int i = 0; i < this.mSupportedModes.size(); i++) {
                    this.mInfo.supportedModes[i] = this.mSupportedModes.valueAt(i).mMode;
                }
                this.mInfo.colorMode = this.mActiveColorMode;
                this.mInfo.supportedColorModes = new int[this.mSupportedColorModes.size()];
                for (int i2 = 0; i2 < this.mSupportedColorModes.size(); i2++) {
                    this.mInfo.supportedColorModes[i2] = this.mSupportedColorModes.get(i2).intValue();
                }
                this.mInfo.hdrCapabilities = this.mHdrCapabilities;
                this.mInfo.appVsyncOffsetNanos = physicalDisplayInfo.appVsyncOffsetNanos;
                this.mInfo.presentationDeadlineNanos = physicalDisplayInfo.presentationDeadlineNanos;
                this.mInfo.state = this.mState;
                this.mInfo.uniqueId = getUniqueId();
                if (physicalDisplayInfo.secure) {
                    this.mInfo.flags = 12;
                }
                Resources resources = LocalDisplayAdapter.this.getOverlayContext().getResources();
                if (this.mBuiltInDisplayId == 0) {
                    this.mInfo.name = resources.getString(R.string.bugreport_countdown);
                    DisplayDeviceInfo displayDeviceInfo = this.mInfo;
                    displayDeviceInfo.flags = 3 | displayDeviceInfo.flags;
                    if (resources.getBoolean(R.^attr-private.leftToRight) || (Build.IS_EMULATOR && SystemProperties.getBoolean(LocalDisplayAdapter.PROPERTY_EMULATOR_CIRCULAR, false))) {
                        this.mInfo.flags |= 256;
                    }
                    this.mInfo.displayCutout = DisplayCutout.fromResources(resources, this.mInfo.width, this.mInfo.height);
                    this.mInfo.type = 1;
                    this.mInfo.densityDpi = (int) ((physicalDisplayInfo.density * 160.0f) + 0.5f);
                    this.mInfo.xDpi = physicalDisplayInfo.xDpi;
                    this.mInfo.yDpi = physicalDisplayInfo.yDpi;
                    this.mInfo.touch = 1;
                } else {
                    this.mInfo.displayCutout = null;
                    this.mInfo.type = 2;
                    this.mInfo.flags |= 64;
                    this.mInfo.name = LocalDisplayAdapter.this.getContext().getResources().getString(R.string.bugreport_message);
                    this.mInfo.touch = 2;
                    this.mInfo.setAssumedDensityForExternalDisplay(physicalDisplayInfo.width, physicalDisplayInfo.height);
                    if ("portrait".equals(SystemProperties.get("persist.demo.hdmirotation"))) {
                        this.mInfo.rotation = 3;
                    }
                    if (SystemProperties.getBoolean("persist.demo.hdmirotates", false)) {
                        this.mInfo.flags |= 2;
                    }
                    if (!resources.getBoolean(R.^attr-private.layout_hasNestedScrollIndicator)) {
                        this.mInfo.flags |= 128;
                    }
                    if (resources.getBoolean(R.^attr-private.layout_ignoreOffset)) {
                        this.mInfo.flags |= 16;
                    }
                }
            }
            return this.mInfo;
        }

        @Override
        public Runnable requestDisplayStateLocked(final int i, final int i2) {
            boolean z = this.mState != i;
            final boolean z2 = (this.mBrightness == i2 || this.mBacklight == null) ? false : true;
            if (z || z2) {
                final int i3 = this.mBuiltInDisplayId;
                final IBinder displayTokenLocked = getDisplayTokenLocked();
                final int i4 = this.mState;
                if (z) {
                    this.mState = i;
                    updateDeviceInfoLocked();
                }
                if (z2) {
                    this.mBrightness = i2;
                }
                return new Runnable() {
                    @Override
                    public void run() {
                        int i5 = i4;
                        if (Display.isSuspendedState(i4) || i4 == 0) {
                            if (!Display.isSuspendedState(i)) {
                                setDisplayState(i);
                                i5 = i;
                            } else if (i == 4 || i4 == 4) {
                                setDisplayState(3);
                                i5 = 3;
                            } else if (i == 6 || i4 == 6) {
                                setDisplayState(2);
                                i5 = 2;
                            } else {
                                return;
                            }
                        }
                        boolean z3 = true;
                        if ((i == 5 || i5 == 5) && i5 != i) {
                            setVrMode(i == 5);
                        } else {
                            z3 = false;
                        }
                        if (z2 || z3) {
                            setDisplayBrightness(i2);
                        }
                        if (i != i5) {
                            setDisplayState(i);
                        }
                    }

                    private void setVrMode(boolean z3) {
                        LocalDisplayDevice.this.mBacklight.setVrMode(z3);
                    }

                    private void setDisplayState(int i5) {
                        if (LocalDisplayAdapter.MTK_DEBUG) {
                            Slog.d(LocalDisplayAdapter.TAG, "setDisplayState(id=" + i3 + ", state=" + Display.stateToString(i5) + ")");
                        }
                        if (LocalDisplayDevice.this.mSidekickActive) {
                            Trace.traceBegin(131072L, "SidekickInternal#endDisplayControl");
                            try {
                                LocalDisplayDevice.this.mSidekickInternal.endDisplayControl();
                                Trace.traceEnd(131072L);
                                LocalDisplayDevice.this.mSidekickActive = false;
                            } finally {
                            }
                        }
                        int powerModeForState = LocalDisplayAdapter.getPowerModeForState(i5);
                        Trace.traceBegin(131072L, "setDisplayState(id=" + i3 + ", state=" + Display.stateToString(i5) + ")");
                        try {
                            SurfaceControl.setDisplayPowerMode(displayTokenLocked, powerModeForState);
                            Trace.traceCounter(131072L, "DisplayPowerMode", powerModeForState);
                            Trace.traceEnd(131072L);
                            if (Display.isSuspendedState(i5) && i5 != 1 && LocalDisplayDevice.this.mSidekickInternal != null && !LocalDisplayDevice.this.mSidekickActive) {
                                Trace.traceBegin(131072L, "SidekickInternal#startDisplayControl");
                                try {
                                    LocalDisplayDevice.this.mSidekickActive = LocalDisplayDevice.this.mSidekickInternal.startDisplayControl(i5);
                                } finally {
                                }
                            }
                        } finally {
                        }
                    }

                    private void setDisplayBrightness(int i5) {
                        if (LocalDisplayAdapter.MTK_DEBUG) {
                            Slog.d(LocalDisplayAdapter.TAG, "setDisplayBrightness(id=" + i3 + ", brightness=" + i5 + ")");
                        }
                        Trace.traceBegin(131072L, "setDisplayBrightness(id=" + i3 + ", brightness=" + i5 + ")");
                        try {
                            LocalDisplayDevice.this.mBacklight.setBrightness(i5);
                            Trace.traceCounter(131072L, "ScreenBrightness", i5);
                        } finally {
                            Trace.traceEnd(131072L);
                        }
                    }
                };
            }
            return null;
        }

        @Override
        public void requestDisplayModesLocked(int i, int i2) {
            if (requestModeLocked(i2) || requestColorModeLocked(i)) {
                updateDeviceInfoLocked();
            }
        }

        @Override
        public void onOverlayChangedLocked() {
            updateDeviceInfoLocked();
        }

        public boolean requestModeLocked(int i) {
            if (i == 0) {
                i = this.mDefaultModeId;
            } else if (this.mSupportedModes.indexOfKey(i) < 0) {
                Slog.w(LocalDisplayAdapter.TAG, "Requested mode " + i + " is not supported by this display, reverting to default display mode.");
                i = this.mDefaultModeId;
            }
            int iFindDisplayInfoIndexLocked = findDisplayInfoIndexLocked(i);
            if (iFindDisplayInfoIndexLocked < 0) {
                Slog.w(LocalDisplayAdapter.TAG, "Requested mode ID " + i + " not available, trying with default mode ID");
                i = this.mDefaultModeId;
                iFindDisplayInfoIndexLocked = findDisplayInfoIndexLocked(i);
            }
            if (this.mActivePhysIndex == iFindDisplayInfoIndexLocked) {
                return false;
            }
            SurfaceControl.setActiveConfig(getDisplayTokenLocked(), iFindDisplayInfoIndexLocked);
            this.mActivePhysIndex = iFindDisplayInfoIndexLocked;
            this.mActiveModeId = i;
            this.mActiveModeInvalid = false;
            return true;
        }

        public boolean requestColorModeLocked(int i) {
            if (this.mActiveColorMode == i) {
                return false;
            }
            if (!this.mSupportedColorModes.contains(Integer.valueOf(i))) {
                Slog.w(LocalDisplayAdapter.TAG, "Unable to find color mode " + i + ", ignoring request.");
                return false;
            }
            SurfaceControl.setActiveColorMode(getDisplayTokenLocked(), i);
            this.mActiveColorMode = i;
            this.mActiveColorModeInvalid = false;
            return true;
        }

        @Override
        public void dumpLocked(PrintWriter printWriter) {
            super.dumpLocked(printWriter);
            printWriter.println("mBuiltInDisplayId=" + this.mBuiltInDisplayId);
            printWriter.println("mActivePhysIndex=" + this.mActivePhysIndex);
            printWriter.println("mActiveModeId=" + this.mActiveModeId);
            printWriter.println("mActiveColorMode=" + this.mActiveColorMode);
            printWriter.println("mState=" + Display.stateToString(this.mState));
            printWriter.println("mBrightness=" + this.mBrightness);
            printWriter.println("mBacklight=" + this.mBacklight);
            printWriter.println("mDisplayInfos=");
            for (int i = 0; i < this.mDisplayInfos.length; i++) {
                printWriter.println("  " + this.mDisplayInfos[i]);
            }
            printWriter.println("mSupportedModes=");
            for (int i2 = 0; i2 < this.mSupportedModes.size(); i2++) {
                printWriter.println("  " + this.mSupportedModes.valueAt(i2));
            }
            printWriter.print("mSupportedColorModes=[");
            for (int i3 = 0; i3 < this.mSupportedColorModes.size(); i3++) {
                if (i3 != 0) {
                    printWriter.print(", ");
                }
                printWriter.print(this.mSupportedColorModes.get(i3));
            }
            printWriter.println("]");
        }

        private int findDisplayInfoIndexLocked(int i) {
            DisplayModeRecord displayModeRecord = this.mSupportedModes.get(i);
            if (displayModeRecord != null) {
                for (int i2 = 0; i2 < this.mDisplayInfos.length; i2++) {
                    if (displayModeRecord.hasMatchingMode(this.mDisplayInfos[i2])) {
                        return i2;
                    }
                }
                return -1;
            }
            return -1;
        }

        private void updateDeviceInfoLocked() {
            this.mInfo = null;
            LocalDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
        }
    }

    Context getOverlayContext() {
        return ActivityThread.currentActivityThread().getSystemUiContext();
    }

    private static final class DisplayModeRecord {
        public final Display.Mode mMode;

        public DisplayModeRecord(SurfaceControl.PhysicalDisplayInfo physicalDisplayInfo) {
            this.mMode = DisplayAdapter.createMode(physicalDisplayInfo.width, physicalDisplayInfo.height, physicalDisplayInfo.refreshRate);
        }

        public boolean hasMatchingMode(SurfaceControl.PhysicalDisplayInfo physicalDisplayInfo) {
            return this.mMode.getPhysicalWidth() == physicalDisplayInfo.width && this.mMode.getPhysicalHeight() == physicalDisplayInfo.height && Float.floatToIntBits(this.mMode.getRefreshRate()) == Float.floatToIntBits(physicalDisplayInfo.refreshRate);
        }

        public String toString() {
            return "DisplayModeRecord{mMode=" + this.mMode + "}";
        }
    }

    private final class HotplugDisplayEventReceiver extends DisplayEventReceiver {
        public HotplugDisplayEventReceiver(Looper looper) {
            super(looper, 0);
        }

        public void onHotplug(long j, int i, boolean z) {
            synchronized (LocalDisplayAdapter.this.getSyncRoot()) {
                try {
                    if (z) {
                        LocalDisplayAdapter.this.tryConnectDisplayLocked(i);
                    } else {
                        LocalDisplayAdapter.this.tryDisconnectDisplayLocked(i);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }
}
