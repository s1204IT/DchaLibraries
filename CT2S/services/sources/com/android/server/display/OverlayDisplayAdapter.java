package com.android.server.display;

import android.R;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Slog;
import android.view.Surface;
import android.view.SurfaceControl;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.display.DisplayAdapter;
import com.android.server.display.DisplayManagerService;
import com.android.server.display.OverlayDisplayWindow;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OverlayDisplayAdapter extends DisplayAdapter {
    static final boolean DEBUG = false;
    private static final int MAX_HEIGHT = 4096;
    private static final int MAX_WIDTH = 4096;
    private static final int MIN_HEIGHT = 100;
    private static final int MIN_WIDTH = 100;
    private static final Pattern SETTING_PATTERN = Pattern.compile("(\\d+)x(\\d+)/(\\d+)(,[a-z]+)*");
    static final String TAG = "OverlayDisplayAdapter";
    private static final String UNIQUE_ID_PREFIX = "overlay:";
    private String mCurrentOverlaySetting;
    private final ArrayList<OverlayDisplayHandle> mOverlays;
    private final Handler mUiHandler;

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
        int width;
        int height;
        int densityDpi;
        String flagString;
        String value = Settings.Global.getString(getContext().getContentResolver(), "overlay_display_devices");
        if (value == null) {
            value = "";
        }
        if (!value.equals(this.mCurrentOverlaySetting)) {
            this.mCurrentOverlaySetting = value;
            if (!this.mOverlays.isEmpty()) {
                Slog.i(TAG, "Dismissing all overlay display devices.");
                for (OverlayDisplayHandle overlay : this.mOverlays) {
                    overlay.dismissLocked();
                }
                this.mOverlays.clear();
            }
            int count = 0;
            String[] arr$ = value.split(";");
            for (String part : arr$) {
                Matcher matcher = SETTING_PATTERN.matcher(part);
                if (matcher.matches()) {
                    if (count >= 4) {
                        Slog.w(TAG, "Too many overlay display devices specified: " + value);
                        return;
                    }
                    try {
                        width = Integer.parseInt(matcher.group(1), 10);
                        height = Integer.parseInt(matcher.group(2), 10);
                        densityDpi = Integer.parseInt(matcher.group(3), 10);
                        flagString = matcher.group(4);
                    } catch (NumberFormatException e) {
                    }
                    if (width >= 100 && width <= 4096 && height >= 100 && height <= 4096 && densityDpi >= 120 && densityDpi <= 480) {
                        count++;
                        String name = getContext().getResources().getString(R.string.mediasize_chinese_om_pa_kai, Integer.valueOf(count));
                        int gravity = chooseOverlayGravity(count);
                        boolean secure = (flagString == null || !flagString.contains(",secure")) ? DEBUG : true;
                        Slog.i(TAG, "Showing overlay display device #" + count + ": name=" + name + ", width=" + width + ", height=" + height + ", densityDpi=" + densityDpi + ", secure=" + secure);
                        this.mOverlays.add(new OverlayDisplayHandle(name, width, height, densityDpi, gravity, secure, count));
                    } else {
                        Slog.w(TAG, "Malformed overlay display devices setting: " + value);
                    }
                } else if (!part.isEmpty()) {
                }
            }
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

    private final class OverlayDisplayDevice extends DisplayDevice {
        private final int mDensityDpi;
        private final long mDisplayPresentationDeadlineNanos;
        private final int mHeight;
        private DisplayDeviceInfo mInfo;
        private final String mName;
        private final float mRefreshRate;
        private final boolean mSecure;
        private int mState;
        private Surface mSurface;
        private SurfaceTexture mSurfaceTexture;
        private final int mWidth;

        public OverlayDisplayDevice(IBinder displayToken, String name, int width, int height, float refreshRate, long presentationDeadlineNanos, int densityDpi, boolean secure, int state, SurfaceTexture surfaceTexture, int number) {
            super(OverlayDisplayAdapter.this, displayToken, OverlayDisplayAdapter.UNIQUE_ID_PREFIX + number);
            this.mName = name;
            this.mWidth = width;
            this.mHeight = height;
            this.mRefreshRate = refreshRate;
            this.mDisplayPresentationDeadlineNanos = presentationDeadlineNanos;
            this.mDensityDpi = densityDpi;
            this.mSecure = secure;
            this.mState = state;
            this.mSurfaceTexture = surfaceTexture;
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
            if (this.mSurfaceTexture != null) {
                if (this.mSurface == null) {
                    this.mSurface = new Surface(this.mSurfaceTexture);
                }
                setSurfaceInTransactionLocked(this.mSurface);
            }
        }

        public void setStateLocked(int state) {
            this.mState = state;
            this.mInfo = null;
        }

        @Override
        public DisplayDeviceInfo getDisplayDeviceInfoLocked() {
            if (this.mInfo == null) {
                this.mInfo = new DisplayDeviceInfo();
                this.mInfo.name = this.mName;
                this.mInfo.uniqueId = getUniqueId();
                this.mInfo.width = this.mWidth;
                this.mInfo.height = this.mHeight;
                this.mInfo.refreshRate = this.mRefreshRate;
                this.mInfo.supportedRefreshRates = new float[]{this.mRefreshRate};
                this.mInfo.densityDpi = this.mDensityDpi;
                this.mInfo.xDpi = this.mDensityDpi;
                this.mInfo.yDpi = this.mDensityDpi;
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
    }

    private final class OverlayDisplayHandle implements OverlayDisplayWindow.Listener {
        private final int mDensityDpi;
        private OverlayDisplayDevice mDevice;
        private final int mGravity;
        private final int mHeight;
        private final String mName;
        private final int mNumber;
        private final boolean mSecure;
        private final int mWidth;
        private OverlayDisplayWindow mWindow;
        private final Runnable mShowRunnable = new Runnable() {
            @Override
            public void run() {
                OverlayDisplayWindow window = new OverlayDisplayWindow(OverlayDisplayAdapter.this.getContext(), OverlayDisplayHandle.this.mName, OverlayDisplayHandle.this.mWidth, OverlayDisplayHandle.this.mHeight, OverlayDisplayHandle.this.mDensityDpi, OverlayDisplayHandle.this.mGravity, OverlayDisplayHandle.this.mSecure, OverlayDisplayHandle.this);
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
                if (window != null) {
                    window.dismiss();
                }
            }
        };

        public OverlayDisplayHandle(String name, int width, int height, int densityDpi, int gravity, boolean secure, int number) {
            this.mName = name;
            this.mWidth = width;
            this.mHeight = height;
            this.mDensityDpi = densityDpi;
            this.mGravity = gravity;
            this.mSecure = secure;
            this.mNumber = number;
            OverlayDisplayAdapter.this.mUiHandler.post(this.mShowRunnable);
        }

        public void dismissLocked() {
            OverlayDisplayAdapter.this.mUiHandler.removeCallbacks(this.mShowRunnable);
            OverlayDisplayAdapter.this.mUiHandler.post(this.mDismissRunnable);
        }

        @Override
        public void onWindowCreated(SurfaceTexture surfaceTexture, float refreshRate, long presentationDeadlineNanos, int state) {
            synchronized (OverlayDisplayAdapter.this.getSyncRoot()) {
                IBinder displayToken = SurfaceControl.createDisplay(this.mName, this.mSecure);
                this.mDevice = OverlayDisplayAdapter.this.new OverlayDisplayDevice(displayToken, this.mName, this.mWidth, this.mHeight, refreshRate, presentationDeadlineNanos, this.mDensityDpi, this.mSecure, state, surfaceTexture, this.mNumber);
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
            pw.println("    mWidth=" + this.mWidth);
            pw.println("    mHeight=" + this.mHeight);
            pw.println("    mDensityDpi=" + this.mDensityDpi);
            pw.println("    mGravity=" + this.mGravity);
            pw.println("    mSecure=" + this.mSecure);
            pw.println("    mNumber=" + this.mNumber);
            if (this.mWindow != null) {
                IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "    ");
                ipw.increaseIndent();
                DumpUtils.dumpAsync(OverlayDisplayAdapter.this.mUiHandler, this.mWindow, ipw, 200L);
            }
        }
    }
}
