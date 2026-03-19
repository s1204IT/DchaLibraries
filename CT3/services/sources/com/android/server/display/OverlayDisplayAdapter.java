package com.android.server.display;

import android.R;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Slog;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.OverlayDisplayWindow;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OverlayDisplayAdapter extends DisplayAdapter {
    static final boolean DEBUG = false;
    private static final int MAX_HEIGHT = 4096;
    private static final int MAX_WIDTH = 4096;
    private static final int MIN_HEIGHT = 100;
    private static final int MIN_WIDTH = 100;
    static final String TAG = "OverlayDisplayAdapter";
    private static final String UNIQUE_ID_PREFIX = "overlay:";
    private String mCurrentOverlaySetting;
    private final ArrayList<OverlayDisplayHandle> mOverlays;
    private final Handler mUiHandler;
    private static final Pattern DISPLAY_PATTERN = Pattern.compile("([^,]+)(,[a-z]+)*");
    private static final Pattern MODE_PATTERN = Pattern.compile("(\\d+)x(\\d+)/(\\d+)");

    public OverlayDisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, DisplayAdapter.Listener listener, Handler uiHandler) {
        super(syncRoot, context, handler, listener, TAG);
        this.mOverlays = new ArrayList<>();
        this.mCurrentOverlaySetting = "";
        this.mUiHandler = uiHandler;
    }

    @Override
    public void dumpLocked(PrintWriter pw) {
        super.dumpLocked(pw);
        pw.println("mCurrentOverlaySetting=" + this.mCurrentOverlaySetting);
        pw.println("mOverlays: size=" + this.mOverlays.size());
        for (OverlayDisplayHandle overlay : this.mOverlays) {
            overlay.dumpLocked(pw);
        }
    }

    @Override
    public void registerLocked() {
        super.registerLocked();
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                OverlayDisplayAdapter.this.getContext().getContentResolver().registerContentObserver(Settings.Global.getUriFor("overlay_display_devices"), true, new ContentObserver(OverlayDisplayAdapter.this.getHandler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        OverlayDisplayAdapter.this.updateOverlayDisplayDevices();
                    }
                });
                OverlayDisplayAdapter.this.updateOverlayDisplayDevices();
            }
        });
    }

    private void updateOverlayDisplayDevices() {
        synchronized (getSyncRoot()) {
            updateOverlayDisplayDevicesLocked();
        }
    }

    private void updateOverlayDisplayDevicesLocked() {
        String value = Settings.Global.getString(getContext().getContentResolver(), "overlay_display_devices");
        if (value == null) {
            value = "";
        }
        if (value.equals(this.mCurrentOverlaySetting)) {
            return;
        }
        this.mCurrentOverlaySetting = value;
        if (!this.mOverlays.isEmpty()) {
            Slog.i(TAG, "Dismissing all overlay display devices.");
            for (OverlayDisplayHandle overlay : this.mOverlays) {
                overlay.dismissLocked();
            }
            this.mOverlays.clear();
        }
        int count = 0;
        String[] strArrSplit = value.split(";");
        int i = 0;
        int length = strArrSplit.length;
        while (true) {
            int i2 = i;
            if (i2 >= length) {
                return;
            }
            String part = strArrSplit[i2];
            Matcher displayMatcher = DISPLAY_PATTERN.matcher(part);
            if (!displayMatcher.matches()) {
                Slog.w(TAG, "Malformed overlay display devices setting: " + value);
            } else {
                if (count >= 4) {
                    Slog.w(TAG, "Too many overlay display devices specified: " + value);
                    return;
                }
                String modeString = displayMatcher.group(1);
                String flagString = displayMatcher.group(2);
                ArrayList<OverlayMode> modes = new ArrayList<>();
                for (String mode : modeString.split("\\|")) {
                    Matcher modeMatcher = MODE_PATTERN.matcher(mode);
                    if (modeMatcher.matches()) {
                        try {
                            int width = Integer.parseInt(modeMatcher.group(1), 10);
                            int height = Integer.parseInt(modeMatcher.group(2), 10);
                            int densityDpi = Integer.parseInt(modeMatcher.group(3), 10);
                            if (width < 100 || width > 4096 || height < 100 || height > 4096 || densityDpi < 120 || densityDpi > 640) {
                                Slog.w(TAG, "Ignoring out-of-range overlay display mode: " + mode);
                            } else {
                                modes.add(new OverlayMode(width, height, densityDpi));
                            }
                        } catch (NumberFormatException e) {
                        }
                    } else if (mode.isEmpty()) {
                    }
                }
                if (!modes.isEmpty()) {
                    count++;
                    String name = getContext().getResources().getString(R.string.indeterminate_progress_34, Integer.valueOf(count));
                    int gravity = chooseOverlayGravity(count);
                    boolean zContains = flagString != null ? flagString.contains(",secure") : false;
                    Slog.i(TAG, "Showing overlay display device #" + count + ": name=" + name + ", modes=" + Arrays.toString(modes.toArray()));
                    this.mOverlays.add(new OverlayDisplayHandle(name, modes, gravity, zContains, count));
                }
            }
            i = i2 + 1;
        }
    }

    private static int chooseOverlayGravity(int overlayNumber) {
        switch (overlayNumber) {
            case 1:
                return 51;
            case 2:
                return 85;
            case 3:
                return 53;
            default:
                return 83;
        }
    }

    private abstract class OverlayDisplayDevice extends DisplayDevice {
        private int mActiveMode;
        private final int mDefaultMode;
        private final long mDisplayPresentationDeadlineNanos;
        private DisplayDeviceInfo mInfo;
        private final Display.Mode[] mModes;
        private final String mName;
        private final List<OverlayMode> mRawModes;
        private final float mRefreshRate;
        private final boolean mSecure;
        private int mState;
        private Surface mSurface;
        private SurfaceTexture mSurfaceTexture;

        public abstract void onModeChangedLocked(int i);

        public OverlayDisplayDevice(IBinder displayToken, String name, List<OverlayMode> modes, int activeMode, int defaultMode, float refreshRate, long presentationDeadlineNanos, boolean secure, int state, SurfaceTexture surfaceTexture, int number) {
            super(OverlayDisplayAdapter.this, displayToken, OverlayDisplayAdapter.UNIQUE_ID_PREFIX + number);
            this.mName = name;
            this.mRefreshRate = refreshRate;
            this.mDisplayPresentationDeadlineNanos = presentationDeadlineNanos;
            this.mSecure = secure;
            this.mState = state;
            this.mSurfaceTexture = surfaceTexture;
            this.mRawModes = modes;
            this.mModes = new Display.Mode[modes.size()];
            for (int i = 0; i < modes.size(); i++) {
                OverlayMode mode = modes.get(i);
                this.mModes[i] = OverlayDisplayAdapter.createMode(mode.mWidth, mode.mHeight, refreshRate);
            }
            this.mActiveMode = activeMode;
            this.mDefaultMode = defaultMode;
        }

        public void destroyLocked() {
            this.mSurfaceTexture = null;
            if (this.mSurface != null) {
                this.mSurface.release();
                this.mSurface = null;
            }
            SurfaceControl.destroyDisplay(getDisplayTokenLocked());
        }

        @Override
        public void performTraversalInTransactionLocked() {
            if (this.mSurfaceTexture == null) {
                return;
            }
            if (this.mSurface == null) {
                this.mSurface = new Surface(this.mSurfaceTexture);
            }
            setSurfaceInTransactionLocked(this.mSurface);
        }

        public void setStateLocked(int state) {
            this.mState = state;
            this.mInfo = null;
        }

        @Override
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                Display.Mode mode = this.mModes[this.mActiveMode];
                OverlayMode rawMode = this.mRawModes.get(this.mActiveMode);
                this.mInfo = new DisplayDeviceInfo();
                this.mInfo.name = this.mName;
                this.mInfo.uniqueId = getUniqueId();
                this.mInfo.width = mode.getPhysicalWidth();
                this.mInfo.height = mode.getPhysicalHeight();
                this.mInfo.modeId = mode.getModeId();
                this.mInfo.defaultModeId = this.mModes[0].getModeId();
                this.mInfo.supportedModes = this.mModes;
                this.mInfo.densityDpi = rawMode.mDensityDpi;
                this.mInfo.xDpi = rawMode.mDensityDpi;
                this.mInfo.yDpi = rawMode.mDensityDpi;
                this.mInfo.presentationDeadlineNanos = this.mDisplayPresentationDeadlineNanos + (1000000000 / ((long) ((int) this.mRefreshRate)));
                this.mInfo.flags = 64;
                if (this.mSecure) {
                    this.mInfo.flags |= 4;
                }
                this.mInfo.type = 4;
                this.mInfo.touch = 0;
                this.mInfo.state = this.mState;
            }
            return this.mInfo;
        }

        @Override
        public void requestColorTransformAndModeInTransactionLocked(int color, int id) {
            int index = -1;
            if (id == 0) {
                index = 0;
            } else {
                int i = 0;
                while (true) {
                    if (i >= this.mModes.length) {
                        break;
                    }
                    if (this.mModes[i].getModeId() != id) {
                        i++;
                    } else {
                        index = i;
                        break;
                    }
                }
            }
            if (index == -1) {
                Slog.w(OverlayDisplayAdapter.TAG, "Unable to locate mode " + id + ", reverting to default.");
                index = this.mDefaultMode;
            }
            if (this.mActiveMode == index) {
                return;
            }
            this.mActiveMode = index;
            this.mInfo = null;
            OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this, 2);
            onModeChangedLocked(index);
        }
    }

    private final class OverlayDisplayHandle implements OverlayDisplayWindow.Listener {
        private static final int DEFAULT_MODE_INDEX = 0;
        private OverlayDisplayDevice mDevice;
        private final int mGravity;
        private final List<OverlayMode> mModes;
        private final String mName;
        private final int mNumber;
        private final boolean mSecure;
        private OverlayDisplayWindow mWindow;
        private final Runnable mShowRunnable = new Runnable() {
            @Override
            public void run() {
                OverlayMode mode = (OverlayMode) OverlayDisplayHandle.this.mModes.get(OverlayDisplayHandle.this.mActiveMode);
                OverlayDisplayWindow window = new OverlayDisplayWindow(OverlayDisplayAdapter.this.getContext(), OverlayDisplayHandle.this.mName, mode.mWidth, mode.mHeight, mode.mDensityDpi, OverlayDisplayHandle.this.mGravity, OverlayDisplayHandle.this.mSecure, OverlayDisplayHandle.this);
                window.show();
                synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                    OverlayDisplayHandle.this.mWindow = window;
                }
            }
        };
        private final Runnable mDismissRunnable = new Runnable() {
            @Override
            public void run() {
                OverlayDisplayWindow window;
                synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                    window = OverlayDisplayHandle.this.mWindow;
                    OverlayDisplayHandle.this.mWindow = null;
                }
                if (window == null) {
                    return;
                }
                window.dismiss();
            }
        };
        private final Runnable mResizeRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                    if (OverlayDisplayHandle.this.mWindow == null) {
                        return;
                    }
                    OverlayMode mode = (OverlayMode) OverlayDisplayHandle.this.mModes.get(OverlayDisplayHandle.this.mActiveMode);
                    OverlayDisplayWindow window = OverlayDisplayHandle.this.mWindow;
                    window.resize(mode.mWidth, mode.mHeight, mode.mDensityDpi);
                }
            }
        };
        private int mActiveMode = 0;

        public OverlayDisplayHandle(String name, List<OverlayMode> modes, int gravity, boolean secure, int number) {
            this.mName = name;
            this.mModes = modes;
            this.mGravity = gravity;
            this.mSecure = secure;
            this.mNumber = number;
            showLocked();
        }

        private void showLocked() {
            OverlayDisplayAdapter.this.mUiHandler.post(this.mShowRunnable);
        }

        public void dismissLocked() {
            OverlayDisplayAdapter.this.mUiHandler.removeCallbacks(this.mShowRunnable);
            OverlayDisplayAdapter.this.mUiHandler.post(this.mDismissRunnable);
        }

        private void onActiveModeChangedLocked(int index) {
            OverlayDisplayAdapter.this.mUiHandler.removeCallbacks(this.mResizeRunnable);
            this.mActiveMode = index;
            if (this.mWindow == null) {
                return;
            }
            OverlayDisplayAdapter.this.mUiHandler.post(this.mResizeRunnable);
        }

        @Override
        public void onWindowCreated(SurfaceTexture surfaceTexture, float refreshRate, long presentationDeadlineNanos, int state) {
            synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                IBinder displayToken = SurfaceControl.createDisplay(this.mName, this.mSecure);
                this.mDevice = new OverlayDisplayDevice(OverlayDisplayAdapter.this, displayToken, this.mName, this.mModes, this.mActiveMode, 0, refreshRate, presentationDeadlineNanos, this.mSecure, state, surfaceTexture, this.mNumber) {
                    @Override
                    public void onModeChangedLocked(int index) {
                        OverlayDisplayHandle.this.onActiveModeChangedLocked(index);
                    }
                };
                OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 1);
            }
        }

        @Override
        public void onWindowDestroyed() {
            synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                if (this.mDevice != null) {
                    this.mDevice.destroyLocked();
                    OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 3);
                }
            }
        }

        @Override
        public void onStateChanged(int state) {
            synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                if (this.mDevice != null) {
                    this.mDevice.setStateLocked(state);
                    OverlayDisplayAdapter.this.sendDisplayDeviceEventLocked(this.mDevice, 2);
                }
            }
        }

        public void dumpLocked(PrintWriter pw) {
            pw.println("  " + this.mName + ":");
            pw.println("    mModes=" + Arrays.toString(this.mModes.toArray()));
            pw.println("    mActiveMode=" + this.mActiveMode);
            pw.println("    mGravity=" + this.mGravity);
            pw.println("    mSecure=" + this.mSecure);
            pw.println("    mNumber=" + this.mNumber);
            if (this.mWindow == null) {
                return;
            }
            IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "    ");
            ipw.increaseIndent();
            DumpUtils.dumpAsync(OverlayDisplayAdapter.this.mUiHandler, this.mWindow, ipw, "", 200L);
        }
    }

    private static final class OverlayMode {
        final int mDensityDpi;
        final int mHeight;
        final int mWidth;

        OverlayMode(int width, int height, int densityDpi) {
            this.mWidth = width;
            this.mHeight = height;
            this.mDensityDpi = densityDpi;
        }

        public String toString() {
            return "{width=" + this.mWidth + ", height=" + this.mHeight + ", densityDpi=" + this.mDensityDpi + "}";
        }
    }
}
