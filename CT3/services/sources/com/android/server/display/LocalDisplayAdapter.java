package com.android.server.display;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
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
import com.android.server.LocalServices;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

final class LocalDisplayAdapter extends DisplayAdapter {
    private static final int[] BUILT_IN_DISPLAY_IDS_TO_SCAN = {0, 1};
    private static final boolean DEBUG = true;
    private static final String PROPERTY_EMULATOR_CIRCULAR = "ro.emulator.circular";
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
        for (int builtInDisplayId : BUILT_IN_DISPLAY_IDS_TO_SCAN) {
            tryConnectDisplayLocked(builtInDisplayId);
        }
    }

    private void tryConnectDisplayLocked(int builtInDisplayId) {
        IBinder displayToken = SurfaceControl.getBuiltInDisplay(builtInDisplayId);
        if (displayToken == null) {
            return;
        }
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
        } else {
            if (!device.updatePhysicalDisplayInfoLocked(configs, activeConfig)) {
                return;
            }
            sendDisplayDeviceEventLocked(device, 2);
        }
    }

    private void tryDisconnectDisplayLocked(int builtInDisplayId) {
        LocalDisplayDevice device = this.mDevices.get(builtInDisplayId);
        if (device == null) {
            return;
        }
        this.mDevices.remove(builtInDisplayId);
        sendDisplayDeviceEventLocked(device, 3);
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

        static final boolean f8assertionsDisabled;
        final boolean $assertionsDisabled;
        private int mActiveColorTransformId;
        private boolean mActiveColorTransformInvalid;
        private int mActiveModeId;
        private boolean mActiveModeInvalid;
        private int mActivePhysIndex;
        private final Light mBacklight;
        private int mBrightness;
        private final int mBuiltInDisplayId;
        private int mDefaultColorTransformId;
        private int mDefaultModeId;
        private SurfaceControl.PhysicalDisplayInfo[] mDisplayInfos;
        private boolean mHavePendingChanges;
        private Display.HdrCapabilities mHdrCapabilities;
        private DisplayDeviceInfo mInfo;
        private int mState;
        private final SparseArray<Display.ColorTransform> mSupportedColorTransforms;
        private final SparseArray<DisplayModeRecord> mSupportedModes;

        static {
            f8assertionsDisabled = !LocalDisplayDevice.class.desiredAssertionStatus();
        }

        public LocalDisplayDevice(IBinder displayToken, int builtInDisplayId, SurfaceControl.PhysicalDisplayInfo[] physicalDisplayInfos, int activeDisplayInfo) {
            super(LocalDisplayAdapter.this, displayToken, LocalDisplayAdapter.UNIQUE_ID_PREFIX + builtInDisplayId);
            this.mSupportedModes = new SparseArray<>();
            this.mSupportedColorTransforms = new SparseArray<>();
            this.mState = 0;
            this.mBrightness = -1;
            this.mBuiltInDisplayId = builtInDisplayId;
            updatePhysicalDisplayInfoLocked(physicalDisplayInfos, activeDisplayInfo);
            if (this.mBuiltInDisplayId == 0) {
                LightsManager lights = (LightsManager) LocalServices.getService(LightsManager.class);
                this.mBacklight = lights.getLight(0);
            } else {
                this.mBacklight = null;
            }
            this.mHdrCapabilities = SurfaceControl.getHdrCapabilities(displayToken);
        }

        public boolean updatePhysicalDisplayInfoLocked(SurfaceControl.PhysicalDisplayInfo[] physicalDisplayInfos, int activeDisplayInfo) {
            boolean z;
            this.mDisplayInfos = (SurfaceControl.PhysicalDisplayInfo[]) Arrays.copyOf(physicalDisplayInfos, physicalDisplayInfos.length);
            this.mActivePhysIndex = activeDisplayInfo;
            ArrayList<Display.ColorTransform> colorTransforms = new ArrayList<>();
            boolean colorTransformsAdded = false;
            Display.ColorTransform activeColorTransform = null;
            for (int i = 0; i < physicalDisplayInfos.length; i++) {
                SurfaceControl.PhysicalDisplayInfo info = physicalDisplayInfos[i];
                boolean existingMode = false;
                int j = 0;
                while (true) {
                    if (j >= colorTransforms.size()) {
                        break;
                    }
                    if (colorTransforms.get(j).getColorTransform() != info.colorTransform) {
                        j++;
                    } else {
                        existingMode = true;
                        break;
                    }
                }
                if (!existingMode) {
                    Display.ColorTransform colorTransform = findColorTransform(info);
                    if (colorTransform == null) {
                        colorTransform = LocalDisplayAdapter.createColorTransform(info.colorTransform);
                        colorTransformsAdded = true;
                    }
                    colorTransforms.add(colorTransform);
                    if (i == activeDisplayInfo) {
                        activeColorTransform = colorTransform;
                    }
                }
            }
            ArrayList<DisplayModeRecord> records = new ArrayList<>();
            boolean modesAdded = false;
            for (SurfaceControl.PhysicalDisplayInfo info2 : physicalDisplayInfos) {
                boolean existingMode2 = false;
                int j2 = 0;
                while (true) {
                    if (j2 >= records.size()) {
                        break;
                    }
                    if (!records.get(j2).hasMatchingMode(info2)) {
                        j2++;
                    } else {
                        existingMode2 = true;
                        break;
                    }
                }
                if (!existingMode2) {
                    DisplayModeRecord record = findDisplayModeRecord(info2);
                    if (record == null) {
                        record = new DisplayModeRecord(info2);
                        modesAdded = true;
                    }
                    records.add(record);
                }
            }
            DisplayModeRecord activeRecord = null;
            int i2 = 0;
            while (true) {
                if (i2 >= records.size()) {
                    break;
                }
                DisplayModeRecord record2 = records.get(i2);
                if (!record2.hasMatchingMode(physicalDisplayInfos[activeDisplayInfo])) {
                    i2++;
                } else {
                    activeRecord = record2;
                    break;
                }
            }
            if (this.mActiveModeId != 0 && this.mActiveModeId != activeRecord.mMode.getModeId()) {
                this.mActiveModeInvalid = true;
                LocalDisplayAdapter.this.sendTraversalRequestLocked();
            }
            if (this.mActiveColorTransformId != 0 && this.mActiveColorTransformId != activeColorTransform.getId()) {
                this.mActiveColorTransformInvalid = true;
                LocalDisplayAdapter.this.sendTraversalRequestLocked();
            }
            if (colorTransforms.size() != this.mSupportedColorTransforms.size()) {
                z = true;
            } else {
                z = colorTransformsAdded;
            }
            boolean recordsChanged = records.size() == this.mSupportedModes.size() ? modesAdded : true;
            if (!recordsChanged && !z) {
                return false;
            }
            this.mHavePendingChanges = true;
            this.mSupportedModes.clear();
            for (DisplayModeRecord record3 : records) {
                this.mSupportedModes.put(record3.mMode.getModeId(), record3);
            }
            this.mSupportedColorTransforms.clear();
            for (Display.ColorTransform colorTransform2 : colorTransforms) {
                this.mSupportedColorTransforms.put(colorTransform2.getId(), colorTransform2);
            }
            if (findDisplayInfoIndexLocked(this.mDefaultColorTransformId, this.mDefaultModeId) < 0) {
                if (this.mDefaultModeId != 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Default display mode no longer available, using currently active mode as default.");
                }
                this.mDefaultModeId = activeRecord.mMode.getModeId();
                if (this.mDefaultColorTransformId != 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Default color transform no longer available, using currently active color transform as default");
                }
                this.mDefaultColorTransformId = activeColorTransform.getId();
            }
            if (this.mSupportedModes.indexOfKey(this.mActiveModeId) < 0) {
                if (this.mActiveModeId != 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Active display mode no longer available, reverting to default mode.");
                }
                this.mActiveModeId = this.mDefaultModeId;
                this.mActiveModeInvalid = true;
            }
            if (this.mSupportedColorTransforms.indexOfKey(this.mActiveColorTransformId) < 0) {
                if (this.mActiveColorTransformId != 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Active color transform no longer available, reverting to default transform.");
                }
                this.mActiveColorTransformId = this.mDefaultColorTransformId;
                this.mActiveColorTransformInvalid = true;
            }
            LocalDisplayAdapter.this.sendTraversalRequestLocked();
            return true;
        }

        private DisplayModeRecord findDisplayModeRecord(SurfaceControl.PhysicalDisplayInfo info) {
            for (int i = 0; i < this.mSupportedModes.size(); i++) {
                DisplayModeRecord record = this.mSupportedModes.valueAt(i);
                if (record.hasMatchingMode(info)) {
                    return record;
                }
            }
            return null;
        }

        private Display.ColorTransform findColorTransform(SurfaceControl.PhysicalDisplayInfo info) {
            for (int i = 0; i < this.mSupportedColorTransforms.size(); i++) {
                Display.ColorTransform transform = this.mSupportedColorTransforms.valueAt(i);
                if (transform.getColorTransform() == info.colorTransform) {
                    return transform;
                }
            }
            return null;
        }

        @Override
        public void applyPendingDisplayDeviceInfoChangesLocked() {
            if (!this.mHavePendingChanges) {
                return;
            }
            this.mInfo = null;
            this.mHavePendingChanges = false;
        }

        @Override
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                SurfaceControl.PhysicalDisplayInfo phys = this.mDisplayInfos[this.mActivePhysIndex];
                this.mInfo = new DisplayDeviceInfo();
                this.mInfo.width = phys.width;
                this.mInfo.height = phys.height;
                this.mInfo.modeId = this.mActiveModeId;
                this.mInfo.defaultModeId = this.mDefaultModeId;
                this.mInfo.supportedModes = new Display.Mode[this.mSupportedModes.size()];
                for (int i = 0; i < this.mSupportedModes.size(); i++) {
                    DisplayModeRecord record = this.mSupportedModes.valueAt(i);
                    this.mInfo.supportedModes[i] = record.mMode;
                }
                this.mInfo.colorTransformId = this.mActiveColorTransformId;
                this.mInfo.defaultColorTransformId = this.mDefaultColorTransformId;
                this.mInfo.supportedColorTransforms = new Display.ColorTransform[this.mSupportedColorTransforms.size()];
                for (int i2 = 0; i2 < this.mSupportedColorTransforms.size(); i2++) {
                    this.mInfo.supportedColorTransforms[i2] = this.mSupportedColorTransforms.valueAt(i2);
                }
                this.mInfo.hdrCapabilities = this.mHdrCapabilities;
                this.mInfo.appVsyncOffsetNanos = phys.appVsyncOffsetNanos;
                this.mInfo.presentationDeadlineNanos = phys.presentationDeadlineNanos;
                this.mInfo.state = this.mState;
                this.mInfo.uniqueId = getUniqueId();
                if (phys.secure) {
                    this.mInfo.flags = 12;
                }
                Resources res = LocalDisplayAdapter.this.getContext().getResources();
                if (this.mBuiltInDisplayId == 0) {
                    this.mInfo.name = res.getString(R.string.indeterminate_progress_32);
                    this.mInfo.flags |= 3;
                    if (res.getBoolean(R.^attr-private.notificationHeaderStyle) || (Build.IS_EMULATOR && SystemProperties.getBoolean(LocalDisplayAdapter.PROPERTY_EMULATOR_CIRCULAR, false))) {
                        this.mInfo.flags |= 256;
                    }
                    this.mInfo.type = 1;
                    this.mInfo.densityDpi = (int) ((phys.density * 160.0f) + 0.5f);
                    this.mInfo.xDpi = phys.xDpi;
                    this.mInfo.yDpi = phys.yDpi;
                    this.mInfo.touch = 1;
                } else {
                    this.mInfo.type = 2;
                    this.mInfo.flags |= 64;
                    this.mInfo.name = LocalDisplayAdapter.this.getContext().getResources().getString(R.string.indeterminate_progress_33);
                    this.mInfo.touch = 2;
                    this.mInfo.setAssumedDensityForExternalDisplay(phys.width, phys.height);
                    if ("portrait".equals(SystemProperties.get("persist.demo.hdmirotation"))) {
                        this.mInfo.rotation = 3;
                    }
                    if (SystemProperties.getBoolean("persist.demo.hdmirotates", false)) {
                        this.mInfo.flags |= 2;
                    }
                    if (!res.getBoolean(R.^attr-private.keepDotActivated)) {
                        this.mInfo.flags |= 128;
                    }
                }
            }
            return this.mInfo;
        }

        @Override
        public Runnable requestDisplayStateLocked(final int state, final int brightness) {
            boolean z = true;
            if (!f8assertionsDisabled) {
                if (state == 1 && brightness != 0) {
                    z = false;
                }
                if (!z) {
                    throw new AssertionError();
                }
            }
            boolean stateChanged = this.mState != state;
            final boolean brightnessChanged = (this.mBrightness == brightness || this.mBacklight == null) ? false : true;
            if (!stateChanged && !brightnessChanged) {
                return null;
            }
            final int displayId = this.mBuiltInDisplayId;
            final IBinder token = getDisplayTokenLocked();
            final int oldState = this.mState;
            if (stateChanged) {
                this.mState = state;
                updateDeviceInfoLocked();
            }
            if (brightnessChanged) {
                this.mBrightness = brightness;
            }
            return new Runnable() {
                @Override
                public void run() {
                    int currentState = oldState;
                    if (Display.isSuspendedState(oldState) || oldState == 0) {
                        if (!Display.isSuspendedState(state)) {
                            setDisplayState(state);
                            currentState = state;
                        } else if (state == 4 || oldState == 4) {
                            setDisplayState(3);
                            currentState = 3;
                        } else {
                            return;
                        }
                    }
                    if (brightnessChanged) {
                        setDisplayBrightness(brightness);
                    }
                    if (state == currentState) {
                        return;
                    }
                    setDisplayState(state);
                }

                private void setDisplayState(int state2) {
                    Slog.d(LocalDisplayAdapter.TAG, "setDisplayState(id=" + displayId + ", state=" + Display.stateToString(state2) + ")");
                    Trace.traceBegin(524288L, "setDisplayState(id=" + displayId + ", state=" + Display.stateToString(state2) + ")");
                    try {
                        int mode = LocalDisplayAdapter.getPowerModeForState(state2);
                        SurfaceControl.setDisplayPowerMode(token, mode);
                    } finally {
                        Trace.traceEnd(524288L);
                    }
                }

                private void setDisplayBrightness(int brightness2) {
                    Slog.d(LocalDisplayAdapter.TAG, "setDisplayBrightness(id=" + displayId + ", brightness=" + brightness2 + ")");
                    Trace.traceBegin(524288L, "setDisplayBrightness(id=" + displayId + ", brightness=" + brightness2 + ")");
                    try {
                        LocalDisplayDevice.this.mBacklight.setBrightness(brightness2);
                    } finally {
                        Trace.traceEnd(524288L);
                    }
                }
            };
        }

        @Override
        public void requestColorTransformAndModeInTransactionLocked(int colorTransformId, int modeId) {
            if (modeId == 0) {
                modeId = this.mDefaultModeId;
            } else if (this.mSupportedModes.indexOfKey(modeId) < 0) {
                Slog.w(LocalDisplayAdapter.TAG, "Requested mode " + modeId + " is not supported by this display, reverting to default display mode.");
                modeId = this.mDefaultModeId;
            }
            if (colorTransformId == 0) {
                colorTransformId = this.mDefaultColorTransformId;
            } else if (this.mSupportedColorTransforms.indexOfKey(colorTransformId) < 0) {
                Slog.w(LocalDisplayAdapter.TAG, "Requested color transform " + colorTransformId + " is not supported by this display, reverting to the default color transform");
                colorTransformId = this.mDefaultColorTransformId;
            }
            int physIndex = findDisplayInfoIndexLocked(colorTransformId, modeId);
            if (physIndex < 0) {
                Slog.w(LocalDisplayAdapter.TAG, "Requested color transform, mode ID pair (" + colorTransformId + ", " + modeId + ") not available, trying color transform with default mode ID");
                modeId = this.mDefaultModeId;
                physIndex = findDisplayInfoIndexLocked(colorTransformId, modeId);
                if (physIndex < 0) {
                    Slog.w(LocalDisplayAdapter.TAG, "Requested color transform with default mode ID still not available, falling back to default color transform with default mode.");
                    colorTransformId = this.mDefaultColorTransformId;
                    physIndex = findDisplayInfoIndexLocked(colorTransformId, modeId);
                }
            }
            if (this.mActivePhysIndex == physIndex) {
                return;
            }
            SurfaceControl.setActiveConfig(getDisplayTokenLocked(), physIndex);
            this.mActivePhysIndex = physIndex;
            this.mActiveModeId = modeId;
            this.mActiveModeInvalid = false;
            this.mActiveColorTransformId = colorTransformId;
            this.mActiveColorTransformInvalid = false;
            updateDeviceInfoLocked();
        }

        @Override
        public void dumpLocked(PrintWriter pw) {
            super.dumpLocked(pw);
            pw.println("mBuiltInDisplayId=" + this.mBuiltInDisplayId);
            pw.println("mActivePhysIndex=" + this.mActivePhysIndex);
            pw.println("mActiveModeId=" + this.mActiveModeId);
            pw.println("mActiveColorTransformId=" + this.mActiveColorTransformId);
            pw.println("mState=" + Display.stateToString(this.mState));
            pw.println("mBrightness=" + this.mBrightness);
            pw.println("mBacklight=" + this.mBacklight);
            pw.println("mDisplayInfos=");
            for (int i = 0; i < this.mDisplayInfos.length; i++) {
                pw.println("  " + this.mDisplayInfos[i]);
            }
            pw.println("mSupportedModes=");
            for (int i2 = 0; i2 < this.mSupportedModes.size(); i2++) {
                pw.println("  " + this.mSupportedModes.valueAt(i2));
            }
            pw.println("mSupportedColorTransforms=[");
            for (int i3 = 0; i3 < this.mSupportedColorTransforms.size(); i3++) {
                if (i3 != 0) {
                    pw.print(", ");
                }
                pw.print(this.mSupportedColorTransforms.valueAt(i3));
            }
            pw.println("]");
        }

        private int findDisplayInfoIndexLocked(int colorTransformId, int modeId) {
            DisplayModeRecord record = this.mSupportedModes.get(modeId);
            Display.ColorTransform transform = this.mSupportedColorTransforms.get(colorTransformId);
            if (record != null && transform != null) {
                for (int i = 0; i < this.mDisplayInfos.length; i++) {
                    SurfaceControl.PhysicalDisplayInfo info = this.mDisplayInfos[i];
                    if (info.colorTransform == transform.getColorTransform() && record.hasMatchingMode(info)) {
                        return i;
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

    private static final class DisplayModeRecord {
        public final Display.Mode mMode;

        public DisplayModeRecord(SurfaceControl.PhysicalDisplayInfo phys) {
            this.mMode = LocalDisplayAdapter.createMode(phys.width, phys.height, phys.refreshRate);
        }

        public boolean hasMatchingMode(SurfaceControl.PhysicalDisplayInfo info) {
            int modeRefreshRate = Float.floatToIntBits(this.mMode.getRefreshRate());
            int displayInfoRefreshRate = Float.floatToIntBits(info.refreshRate);
            return this.mMode.getPhysicalWidth() == info.width && this.mMode.getPhysicalHeight() == info.height && modeRefreshRate == displayInfoRefreshRate;
        }

        public String toString() {
            return "DisplayModeRecord{mMode=" + this.mMode + "}";
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
